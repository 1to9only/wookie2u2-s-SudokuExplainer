/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * Based on HoDoKu's FishSolver, so here's hobiwan's licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.List;


/**
 * ComplexFisherman implements Basic Fish, Franken Fish, and Mutant Fish Sudoku
 * solving techniques, but doesn't implement Kraken Fish! This class is based
 * on HoDoKu's FishSolver.
 * <p>
 * Fish come with a whole language of there own. Here's a very brief precis.
 * NOTE: other Sudokuists have other perspectives: There's >1 way to do it.
 * <p>
 * A "basic fish" is a pattern where 2/3/4 bases (rows-or-cols) share 2/3/4
 * possible positions (covers: cols-or-rows) for a potential value; therefore
 * one of the intersecting cells (a corner) must contain the value, so we can
 * remove any extra positions for value from the covers (cols-or-rows).
 * <p>
 * Fish size:<ul>
 * <li>2 regions are Swampfish, common name X-Wing. I dissent because it's a
 * bloody Fish, not a Wing! Recall Yoda raising Luke's X-Wing from the swamp.
 * <li>3 regions are called Swordfish
 * <li>4 regions are called Jellyfish
 * <li>Larger fish are all degenerate (combinations of simpler patterns) so I
 * do not (though I can) seek them: 5=Squirmbag, 6=Whale, 7=Leviathan
 * <li>The size of the fish is stored in super.degree (set by Tech selection).
 * </ul>
 * <p>
 * The regions we look in are called "bases". The crossing regions that we
 * actually eliminate from are called the "covers". So for "basic fish" we:<ul>
 * <li>first search the rows (bases), and the cols are our covers;
 * <li>then bases are cols, and covers are rows (they swap roles).
 * <li>Mutant Fish (below) need only one search (includes all bases).
 * </ul>
 * <p>
 * A "basic fish" does not have fins. All the "complex fish" types expand the
 * search to find more hints with reduced eliminations. We do more work for
 * less pay as s__t gets harder.
 * <p>
 * A "finned fish" is a basic fish just with one-or-more fins. A fin is an
 * additional position for the fish value in a base region. Fins expand the
 * number of matches but also reduce the number of eliminations; because each
 * elimination must see all fins (so IIRC all fins must see each other).
 * <p>
 * Sashimi fish rely on a fin (else they're degenerate) to allow us to drop
 * a corner from the normal Fish pattern: replacing a corner with a fin.
 * <p>
 * Franken fish adds a box to the bases OR the covers.
 * <p>
 * Mutant fish adds boxes to the bases AND the covers.
 * <p>
 * NOT_IMPLEMENTED: Kraken fish adds chaining to our equations: If all possible
 * configurations of the Fish AND all the fins chain to the same elimination,
 * then the elimination must be true, so it is applied.
 * <p>
 * This class is a copy-paste of HoDoKu's FishSolver, modified to use Sudoku
 * Explainers model, chainers, and hints; and import all HoDoKu dependencies.
 * I ripped-out Kraken Fish coz I'm too stupid to work-out how to implement
 * them using my chainers.
 * <p>
 * Kudos to hobiwan. Mistakes are mine. ~KRC
 * <p>
 * 2020-09-28 after some hard-thinking I've dropped Kraken for now in order to
 * get the rest of it working first. The plan is to come back and knut-out how
 * to find kraken-chains more efficiently. Hobiwans design relies on doing the
 * tabling-search for all chains ONCE, and re-using that and the ALS-search
 * results in the Kraken search; I need to not re-use results, so I need to
 * only search each corner and each fin. By "each corner" I mean each corner
 * in the first base with > 1 corners, ie all possible Fish configurations.
 * I have no-idea how to handle ALSes, yet.
 * <p>
 * 2020-09-29 Bit the bullet and ripped-out Kraken Fish completely, the plan
 * now is to implement Kraken independently in KrakenFisherman, which will do
 * it's own chaining (ie have no dependency on either HoDoKu or my Chainers).
 * <p>
 * 2020-10-04 Progress update: Finned and Franken work pretty well, but Mutant
 * still finds just one hint (a Franken Jellyfish) where HoDoKu finds hundreds
 * of hints, and I can't see (yet) where I've gone wrong.
 * <p>
 * <pre>
 * 2020-11-29 Progress update: Mutants are FAR too slow, esp Jellyfish
 *    10,916,885,082 4433   2,462,640  0               0 Mutant Swampfish    10 seconds
 *   215,801,070,442 4433  48,680,593  2 107,900,535,221 Mutant Swordfish  3.58 minutes
 * 2,091,340,950,760 4431 471,979,451 19 110,070,576,355 Mutant Jellyfish 34.85 minutes
 *
 * I cannot see any opportunities to speed-up the actual code, so I'm left with
 * less savoury options. I could enspeedonate it for top1465 only, as per A*E,
 * but that stinks. Or I could just have another look at speeding-up mutants.
 *
 * 13:31 with the row/col AND box filter reinstated and logic fixed
 *   137,405,847,323 4409  31,164,855  2 68,702,923,661 Mutant Swordfish  2.28 minutes
 * 1,608,436,992,524 4407 364,973,222 18 89,357,610,695 Mutant Jellyfish 26.80 minutes
 *
 * 14:53 it can't be done any faster. sigh.
 *   140,582,626,255 4409  31,885,376  2 70,291,313,127 Mutant Swordfish
 * 1,624,673,985,496 4407 368,657,586 18 90,259,665,860 Mutant Jellyfish
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 2,154,956,584,000 (35:54)     1,470,960,125
 *
 * 2020-12-02 09:53 reinstated mutants, without the filter
 *    11,373,373,900 4409   2,579,581  0               0 Mutant Swampfish
 *   221,869,645,200 4409  50,321,988  2 110,934,822,600 Mutant Swordfish
 * 2,148,729,373,100 4407 487,571,902 19 113,091,019,636 Mutant Jellyfish
 *
 * 2020-12-03 07:06 modified cover filter is SLOWER
 *    12,584,486,700 4409   2,854,272  0               0 Mutant Swampfish
 *   254,482,656,400 4409  57,718,905  2 127,241,328,200 Mutant Swordfish
 * 2,479,199,708,600 4407 562,559,498 19 130,484,195,189 Mutant Jellyfish
 *
 * 2020-12-03 07:?? HACK base filter and modified cover filter
 *     6,145,725,700 4415   1,392,010  0              0 Mutant Swampfish
 *    75,165,147,100 4415  17,024,948  1 75,165,147,100 Mutant Swordfish
 *   493,146,482,300 4414 111,723,262 11 44,831,498,390 Mutant Jellyfish
 *
 * 2020-12-03 08:06 HACK base filter only (remove cover filter coz it slower)
 *     4,990,931,400 4415  1,130,448  0              0 Mutant Swampfish
 *    60,627,458,000 4415 13,732,153  1 60,627,458,000 Mutant Swordfish
 *   394,362,063,900 4414 89,343,467 11 35,851,096,718 Mutant Jellyfish
 *
 * 2020-12-03 08:?? comment-out usedCoverTypes beneath the dead cover filter
 * </pre>
 *
 * @author Keith Corlett 2020 Sept 24
 */
public class ComplexFisherman extends AHinter
//		implements diuf.sudoku.solver.IReporter
{
	/** switch on/off logging in findHints. */
	public static boolean FIND_HINTS_IS_NOISY = false;

	// Problem: Hint contains eliminations for non-potential-values.
	// Fix: createHint check for MIA deletes and sharks; and throws empty reds.
	// MIA means missing in action. MIA_REDS is almost a WMD 404 Error.
	private static final boolean MIA_REDS = false;

	// constants just to make the code a bit more self-documentary
	private static final boolean BASE = true;
	private static final boolean COVER = false;

	// Fish Settings
	// maximum number of fins (extra cells which maybe v in a base)
	private static final int MAX_FINS = 5;

	// maximum number of endo-fins (candidates in two overlapping bases)
	private static final int MAX_ENDO_FINS = 2;

//	// fishType constants
//	private static final int UNDEFINED = -1; // KRAKEN now uses MUTANT, was UNDEFINED
//	private static final int BASIC = 0;
//	private static final int FRANKEN = 1;
//	private static final int MUTANT = 2;
//	private static final int KRAKEN = 3;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ getRegionTypesMask ~~~~~~~~~~~~~~~~~~~~~~~~~~

	// an array of the type of each of the 27 regions to look-up
	// NB: SE is (box, row, col) whereas HoDoKu is (row, col, box)
	private static final int BOX = Grid.BOX;	// 0
	private static final int ROW = Grid.ROW;	// 1
	private static final int COL = Grid.COL;	// 2
	private static final int[] REGION_TYPE = {
		BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX,
		ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW,
		COL, COL, COL, COL, COL, COL, COL, COL, COL
	};

	// a left-shifted mask for each of the 3 types of regions
	private static final int BOX_MASK = 0x1;
	private static final int ROW_MASK = 0x2;
	private static final int COL_MASK = 0x4;
	// both ROW and COL in the one mask
	private static final int ROW_COL_MASK = ROW_MASK | COL_MASK;
	// an array of the mask of each region type to look-up
	private static final int[] REGION_TYPE_MASK = {BOX_MASK, ROW_MASK, COL_MASK};

	// Returns a mask containing the types of the used regions
	// See: BOX_MASK, ROW_MASK, COL_MASK
	private static int getRegionTypesMask(boolean[] used) {
		int mask = 0;
		for ( int i=0,n=used.length; i<n; ++i )
			if ( used[i] )
				mask |= REGION_TYPE_MASK[REGION_TYPE[i]];
		return mask;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ static stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~

	// squeeze html up into a string suitable for printing on one line
	private static String squeeze(String html) {
		return html.replaceAll("</?[a-z]+>", "") // remove all html tags
				   .replaceAll(" \\(.*?\\)", "") // remove " (blues)", " (yellows)" and " (purples)"
				   .replaceAll("\\s+", " ") // replace all sequence-of-whitespaces with a space
				   .trim(); // strip leading and trailing whitespace
	}

	private int baseType;
	private int coverType;

	// ~~~~~~~~~~~~~~~~~~~~~~~~ working storage ~~~~~~~~~~~~~~~~~~~~~~~~

	/** A recursion stack entry for each base region in the current baseSet. */
	private class BaseStackEntry {
		/** The index of the current base region. */
		int index;
		/** The index of the previous region (to revert to). */
		int prevIndex;
		/** base v's: indices of cells in base set/s which maybe the Fish
		 * candidate value (euphemistically just 'v'). */
		int vsM0, vsM1, vsM2;
		/** endo-fins: indices of cells that are in more than one base region,
		 * and therefore must be treated as fins. */
		int efM0, efM1, efM2;
////@check comment out (debug only)
//		@Override
//		public String toString() {
//			return ""+index+": "+Idx.toString(vsM0, vsM1, vsM2);
//		}
	}

	/** array of all possible base units (indices in grid.regions). */
	private final int[] bases = new int[27];
	/** number of eligible bases. */
	private int numBases;
	/** is the base region at this index in the current search. */
	private final boolean[] basesUsed = new boolean[27];
	/** recursion stack for the base region search. */
	private final BaseStackEntry[] baseStack = new BaseStackEntry[9];
	/** indices of grid.cells which maybe v in bases;<br>
	 * concurrent with bases (not grid.regions as you might expect);<br>
	 * nb: v is shorthand for the fish candidate value. */
	private final int[] baseVsM0 = new int[27];
	private final int[] baseVsM1 = new int[27];
	private final int[] baseVsM2 = new int[27];

	/** An entry in the recursion stack of the covers search. */
	private class CoverStackEntry {
		/** The index of the current cover region. */
		int index;
		/** The index of the previous cover region (to revert to). */
		int prevIndex;
		/** an Idx of cells in cover set/s which maybe the fish candidate;<br>
		 * nb: v is shorthand for the fish candidate value. */
		int vsM0, vsM1, vsM2;
		/** sharks (cannibalistic): an Idx of cells in more than one cover set,
		 * so become potential eliminations. */
		int skM0, skM1, skM2;
////@check comment out (debug only)
//		@Override
//		public String toString() {
//			return ""+index+": "+Idx.toString(vsM0, vsM1, vsM2);
//		}
	}

	/** array of all eligible cover regions (indices in Grid.regions). */
	private final int[] allCovers = new int[27];
	/** number of allCovers. */
	private int numAllCovers;
	/** indices of grid.cells maybe v (Fish candidate value) in allCovers. */
	private final int[] allCoverVsM0 = new int[27];
	private final int[] allCoverVsM1 = new int[27];
	private final int[] allCoverVsM2 = new int[27];

	/** array of current cover regions (indices in grid.regions). */
	private final int[] covers = new int[27];
	/** indices of grid.cells maybe v (Fish candidate value) in allCovers. */
	private final int[] coverVsM0 = new int[27];
	private final int[] coverVsM1 = new int[27];
	private final int[] coverVsM2 = new int[27];
	/** is the cover region at this index in the current search. */
	private final boolean[] coversUsed = new boolean[27];
	/** the recursion stack for the cover region search. */
	private final CoverStackEntry[] coverStack = new CoverStackEntry[9];

	/** the Fish candidate value, colloquially known as just 'v', for short. */
	private int candidate;
	/** grid.cells indices of cells which maybe the Fish candidate value. */
	private Idx vs;
	/** Idx of the endo-fins, used as param to grid.cmnBuds. */
	private final Idx endoFins = new Idx();
	/** buds: used a result of grid.cmnBuds. */
	private final Idx buds = new Idx();
	/** deletes (potential eliminations) in the current covers search;
	 * read back by createHint. */
	private final Idx deletes = new Idx();
	/** fins (both exo and endo) in the current covers search;
	 * read back by createHint. */
	private final Idx fins = new Idx();
	/** sharks (cannibalistic) in the current covers search;
	 * read back by createHint. */
	private final Idx sharks = new Idx();

	// ~~~~~~~~~~~~~~~~~~~~~~ specify which Fish we seek ~~~~~~~~~~~~~~~~~~~~~~

	// these fields are all set by the constructor, to specify the type and
	// size of the Fish patterns that findHints seeks in the given Grid.
	/** Does this Fisherman seek Basic Fish (No fins): has degree candidates
	 * in degree bases. */
	private final boolean seekBasic;
	/** Does this Fisherman seek Finned Fish: has upto MAX_FINS additional
	 * positions for candidate in the bases. */
	private final boolean seekFinned;
	/** Does this Fisherman seek Franken Fish: has a box in either bases
	 * OR covers. */
	private final boolean seekFranken;
	/** Does this Fisherman seek Mutant Fish: has multiple boxs in bases
	 * AND covers. */
	private final boolean seekMutant;
	/** Equals accu.isSingle() meaning the IAccumulator wants just the first
	 * hint, so we exit-early when we find a hint and wantOneHint is true. */
	private boolean wantOneHint;

	// these fields are set by findHints (values rely on parameters)
	/** The Grid to search for Fish hint/s. */
	private Grid grid;
	/** The IAccumulator to which I add hint/s. */
	private IAccumulator accu;

	/**
	 * The Constructor.
	 * @param tech Tech.Finned/Franken/Mutant Swamp/Sword/Jellyfish
	 */
	public ComplexFisherman(Tech tech) {
		super(tech);
		// This thing can do bigger fish, but they're all degenerate, and slow!
		assert degree>=2 && degree<=4; // 2=Swampfish, 3=Swordfish, 4=Jellyfish
		// these are ALL the complex fish types we seek (not Kraken)
		seekFinned = tech.nom.startsWith("Finned");
		seekFranken = tech.nom.startsWith("Franken");
		seekMutant = tech.nom.startsWith("Mutant");
//		assert !seekMutant : "Mutants are too slow to be allowed!";
		// it's basic unless it's one of the complex fish types.
		// NB: ComplexFisherman isn't normally used to find basic fish, just
		// because the BasicFisherman is faster; but it CAN find basic fish.
		seekBasic = !seekFinned && !seekFranken && !seekMutant;
//		assert !seekBasic : "BasicFisherman is faster!";

		// initialise the two recursion-stacks
		for ( int i=0,n=baseStack.length; i<n; ++i )
			baseStack[i] = new BaseStackEntry();
		for ( int i=0,n=coverStack.length; i<n; ++i )
			coverStack[i] = new CoverStackEntry();
	}

	/**
	 * findHints: foreach candidate 1..9: if templates say there's possible
	 * eliminations then {@link #searchBases(boolean)} for Fish.
	 * Search rows first, then cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.grid = grid;
		this.accu = accu;
		this.wantOneHint = accu.isSingle();
		int pre = accu.size();
		try {
			long start = System.nanoTime();
			final Idx[] idxs = grid.getIdxs();
// templates are useless because EVERYthing passes!
//			final Idx[]	deletables = Run.templates.getDeletables(grid);
//			int done=0, skip=0;
			for ( int v=1; v<10; ++v ) {
//				// skip if no idxs[v] are deletable
//				if ( deletables[v].andAny(idxs[v]) )
//					++done;
//				else {
//					++skip;
//					continue;
//				}
				candidate = v;
				vs = idxs[v];
				// first search rows for fish
				baseType = ROW;
				if ( searchBases() )
					break;
				// Mutant needs only one search, for ALL bases and covers
				if ( seekMutant )
					continue;
				// then search cols for fish
				baseType = COL;
				if ( searchBases() )
					break;
			}
			if ( FIND_HINTS_IS_NOISY ) {
//				Log.format(tech.nom+" done=%d skip=%d in %,d ns\n", done, skip, System.nanoTime()-start);
				Log.format(tech.nom+" in %,d ns\n", System.nanoTime()-start);
			}
		} finally {
			this.grid = null;
			this.accu = null;
		}
		return accu.size() > pre;
	}

	/**
	 * Search all possible combinations of {@link #degree} base regions.
	 *
	 * @return were hint/s found
	 */
	private boolean searchBases() {

		// coverType field used to workout if covers form a Mutant.
		if ( baseType == ROW )
			coverType = COL;
		else
			coverType = ROW;

		// presume that no hints will be found
		searchBasesResult = false;
		// find eligible bases and allCovers; the actual covers are calculated
		// in searchCovers because eligibility depends on the current baseSet.
		if ( !findEligibleBasesAndCovers() )
			return false;

		// clear everything before we start
		Arrays.fill(basesUsed, false);
		// initialise the counts of boxs, rows, and cols used a base region
		usedBaseTypes[0]=usedBaseTypes[1]=usedBaseTypes[2] = 0;
		pB = baseStack[0];
		// vs: indices of Grid.cells which maybe v (the Fish candidate value).
		pB.vsM0=pB.vsM1=pB.vsM2 = 0;
		// endo-fins: cells in more than one base regions, which may overlap
		// coz we're handling row/cols (and boxs in Franken and Mutant fish).
		pB.efM0=pB.efM1=pB.efM2 = 0;
		cB = baseStack[1];
		cB.index = 0;
		cB.prevIndex = -1;
		baseLevel = 1; // start at level 1 (level 0 is just a stopper)

		// try all combinations of base regions
		for (;;) {
			// fallback a baseLevel if no unit is available at this baseLevel
			while ( (cB=baseStack[baseLevel]).index >= numBases ) {
				// fallback one level at a time to maintain basesUsed
				if ( cB.prevIndex != -1 ) {
					basesUsed[cB.prevIndex] = false;
					cB.prevIndex = -1;
				}
				if ( --baseLevel < 1 ) // ie <= 0 (level 0 is just a stopper!)
					return searchBasesResult; // we've tried all combos!
			}
			// unuse the-previous-base-at-this-level
			// hijack baseIndex coz I don't really need another variable
			if ( cB.prevIndex != -1 ) {
				basesUsed[cB.prevIndex] = false;
				--usedBaseTypes[REGION_TYPE[cB.prevIndex]];
			}
			// get the previous levels BaseStackEntry, containing the existing
			// candidates and endoFins sets (the union of ALL bases so-far).
			pB = baseStack[baseLevel - 1];
			// get the next base (must exist or we would have fallen back)
			b = cB.index++;
			// use bases[b] at-this-level
			cB.prevIndex = bases[b]; // remember prev to unuse
			basesUsed[cB.prevIndex] = true;
			++usedBaseTypes[REGION_TYPE[cB.prevIndex]];

			// v is shorthand for the fish candidate value
			// current v's = previous v's + v's in this base
			cB.vsM0 = pB.vsM0 | baseVsM0[b];
			cB.vsM1 = pB.vsM1 | baseVsM1[b];
			cB.vsM2 = pB.vsM2 | baseVsM2[b];

			// my endos = candidates in the existing bases AND in this base
			efM0 = pB.vsM0 & baseVsM0[b];
			efM1 = pB.vsM1 & baseVsM1[b];
			efM2 = pB.vsM2 & baseVsM2[b];
			if ( (efM0|efM1|efM2) == 0 ) {
				// current endos = existing endos (no new ones)
				cB.efM0=pB.efM0; cB.efM1=pB.efM1; cB.efM2=pB.efM2;
				doSearch = true;
			} else {
				// add existing endos to my endos (to size them all)
				efM0|=pB.efM0; efM1|=pB.efM1; efM2|=pB.efM2;
				if ( Idx.sizeLTE(efM0,efM1,efM2, MAX_ENDO_FINS) ) {
					// current endos = existing + my endos
					cB.efM0=efM0; cB.efM1=efM1; cB.efM2=efM2;
					doSearch = true;
				} else {
					doSearch = false;
				}
			}
			if ( doSearch
			  // HACK: if seekMutant then search bases with a coverType or BOX.
			  // This is NOT correct but finds 12 of the 21 Mutants in top1465
			  // in about a quarter of the time. It is incorrect coz it skips
			  // all bases are baseType and a cover is also baseType, but there
			  // are fewer hints with this pattern in top1465, so I accept the
			  // compromise for the significant speed gain. Comment out to find
			  // all Mutants, but beware that top1465 takes about 52 minutes.
			  // User switch for this HACK is not implemented coz everyone who
			  // cares is intelligent enough to just comment the bastard out.
			  && (!seekMutant || (usedBaseTypes[coverType]>0 || usedBaseTypes[BOX]>0))
			) {
				// see if the current endos have any buds-in-common
				// (pays-off because one whole searchCovers may be skipped)
				if ( (cB.efM0|cB.efM1|cB.efM2) == 0 ) {
					doSearch = true; // there's no endos to check, so it passed.
				} else if ( Idx.sizeLTE(cB.efM0,cB.efM1,cB.efM2, MAX_FINS) ) {
					// buds = cells which see all the endo-fins
					Grid.cmnBuds(endoFins.set(cB.efM0,cB.efM1,cB.efM2), buds);
					// doSearch = does any candidate see all the endo-fins?
					doSearch = ( (buds.a0 & vs.a0)
							   | (buds.a1 & vs.a1)
							   | (buds.a2 & vs.a2) ) != 0;
				} else { // more than 5 endos, so don't search these bases
					doSearch = false;
				}
				// if we've got degree bases now then search the covers
				if ( baseLevel==degree && doSearch && searchCovers(cB) ) {
					searchBasesResult = true;
					if ( wantOneHint )
						return true;
				}
				// and on to the next level
				if ( baseLevel < degree ) {
					cB = baseStack[++baseLevel];
					cB.index = b + 1;
					cB.prevIndex = -1;
				}
			}
		} // for ever
	}
	// these fields are (logically) searchBases variables
	private boolean searchBasesResult;
	// the previous and current BaseStackEntry
	private BaseStackEntry pB, cB;
	// current bases-array index (and parallel arrays baseVs)
	private int b;
	// endo-fins: fish candidate in the-existing-bases AND the-new-base
	private int efM0, efM1, efM2;
	// baseLevel is our depth in the baseStack, ie number of bases in baseSet.
	private int baseLevel;
	// doSearch: should searchBases call searchCovers?
	private boolean doSearch;

	/**
	 * Search each eligible combination of covers for the current bases (cB).
	 * "Eligible" means the cover is not already a base. In Franken fish we
	 * allow a box as base OR a cover, so one box per covers. Each cover must
	 * have at least one candidate in common with the given bases.
	 * @param cB the current BaseStackEntry contains the candidates (vs's) and
	 *  endo-fins (ef's) of ALL our $degree bases (the current set of bases).
	 * @return were hint/s found?
	 */
	private boolean searchCovers(BaseStackEntry cB) {

		// presume that no hints will be found
		searchCoversResult = false;

		// get eligible covers from allCovers; done here because eligibility
		// depends on which bases are used.
		numCovers = 0;
		for ( int i=0; i<numAllCovers; ++i )
			// skip if this cover has no cands in common with current bases
			if ( ( (cB.vsM0 & allCoverVsM0[i])
			     | (cB.vsM1 & allCoverVsM1[i])
			     | (cB.vsM2 & allCoverVsM2[i]) ) != 0
			  // skip if this cover is already used as a base
			  && !basesUsed[allCovers[i]]
			  // Franken: box as base OR cover, but not both (first 9 are boxs)
			  && (!seekFranken || usedBaseTypes[BOX]==0 || allCovers[i]>8)
			) {
				covers[numCovers] = allCovers[i];
				coverVsM0[numCovers] = allCoverVsM0[i];
				coverVsM1[numCovers] = allCoverVsM1[i];
				coverVsM2[numCovers++] = allCoverVsM2[i];
			}
		// need atleast degree covers to form a Fish.
		if ( numCovers < degree )
			return false; // this'll only happen rarely.
		// partial-calculation of the maximum cover index.
		ground = numCovers - degree;

		// try each combo of covers
		Arrays.fill(coversUsed, false);
		// zero the counts of boxs, rows, and cols currently used as covers
//		usedCoverTypes[0]=usedCoverTypes[1]=usedCoverTypes[2] = 0;
		// coverLevel: the current depth in the coverStack
		coverLevel = 1; // start at level 1 (0 is just a stopper)
		// initialise previous CoverStackEntry
		pC = coverStack[0];
		// empty previous cover candidates and sharks
		pC.vsM0=pC.vsM1=pC.vsM2 = pC.skM0=pC.skM1=pC.skM2 = 0;
		// initialise current CoverStackEntry
		cC = coverStack[1];
		cC.index = 0;
		cC.prevIndex = -1; // meaning none
		// foreach each distinct combo of covers
		for (;;) {
			// fallback level/s if there's no more covers in allCovers
			while ( (cC=coverStack[coverLevel]).index >= (ground + coverLevel) ) {
				// unuse the previous cover
				if ( cC.prevIndex != -1 ) {
					coversUsed[cC.prevIndex] = false;
					cC.prevIndex = -1;
				}
				// fallback
				if ( --coverLevel < 1 )
					return searchCoversResult; // all covers have been searched
			}
			// pC = previousCover = the previous coverStack entry
			pC = coverStack[coverLevel - 1];
			// unuse the previous cover
			if ( cC.prevIndex > -1 )
//			{
				coversUsed[cC.prevIndex] = false;
//				--usedCoverTypes[REGION_TYPE[cC.prevIndex]];
//			}
			// get next cover set (must exist or we would have fallen back)
			c = cC.index++;
			// use covers[c] as the current cover
			coverIndex = cC.prevIndex = covers[c]; // remember prev to unuse
			coversUsed[coverIndex] = true;
//			++usedCoverTypes[REGION_TYPE[coverIndex]];
			// if the new cover has candidates common with the existing covers
			// then those candidates become possible eliminations, which I call
			// sharks because it's shorter than cannabilistic. The Great White
			// Shark: a non-placental bearthing live young. Do the math!
			// current sharks = previous-set + sharks in new cover
			cC.skM0 = pC.skM0 | (pC.vsM0 & coverVsM0[c]);
			cC.skM1 = pC.skM1 | (pC.vsM1 & coverVsM1[c]);
			cC.skM2 = pC.skM2 | (pC.vsM2 & coverVsM2[c]);
			// current v's = previous-set + v's in new cover
			cC.vsM0 = pC.vsM0 | coverVsM0[c];
			cC.vsM1 = pC.vsM1 | coverVsM1[c];
			cC.vsM2 = pC.vsM2 | coverVsM2[c];

			// seek Fish if we're at the right coverLevel.
			if ( coverLevel == degree ) {
// NB: this filter is "correct" but it also SLOWER (why I know not. sigh.)
//				// if seekMutant then search only when a coverType is a base
//				// or a baseType is a cover, or a BOX is both base and cover;
//				// everything else already covered by the Franken fisherman.
//				if ( !seekMutant
//				  || usedBaseTypes[coverType] > 0
//			      || usedCoverTypes[baseType] > 0
//				  || (usedBaseTypes[BOX]>0 && usedCoverTypes[BOX]>0) ) {
					// fins = current endo-fins + candidates in bases except covers
					finsM0 = cB.efM0 | (cB.vsM0 & ~cC.vsM0);
					finsM1 = cB.efM1 | (cB.vsM1 & ~cC.vsM1);
					finsM2 = cB.efM2 | (cB.vsM2 & ~cC.vsM2);
					// deletes: candidates in covers except bases
					delsM0 = cC.vsM0 & ~cB.vsM0;
					delsM1 = cC.vsM1 & ~cB.vsM1;
					delsM2 = cC.vsM2 & ~cB.vsM2;
					// sharks: current sharks
					shrkM0=cC.skM0; shrkM1=cC.skM1; shrkM2=cC.skM2;

					// basic fish (rows->cols or cols->row, no fins)
					if ( seekBasic ) {
						// no fins AND any deletes or sharks
						if ( (finsM0|finsM1|finsM2)==0
						  && (delsM0|delsM1|delsM2|shrkM0|shrkM1|shrkM2) != 0 ) {
							// **************** FINNLESS FISCH ****************
							// Eliminate candidates in covers except bases.
							// If a base v is in >1 cover (shark) its lunch.
							fins.clear();
							deletes.set(delsM0, delsM1, delsM2);
							sharks.set(shrkM0, shrkM1, shrkM2);
							hint = createHint(cB);
							searchCoversResult |= hint!=null;
							if ( accu.add(hint) ) // exit-early if accu says so
								return true;
						}
					// complex fish needs fins, but not too many of them
					} else
					if ( (finsM0|finsM1|finsM2) != 0
					  && Idx.sizeLTE(finsM0,finsM1,finsM2, MAX_FINS)
					  // finBuds: cells which see (same box/row/col) all fins
					  // if any finBuds then we try to eliminate
					  && Grid.cmnBuds(fins.set(finsM0,finsM1,finsM2), buds).any()
					) {
						// ***************** FINNED/SASHIMI FISCH *************
						// Candidate is deletable if in covers except bases, or
						// belongs to more than one cover set (an endo-fin).
						// deletes=((covers & ~bases) or endos) seeing all fins
						delsM0 = (delsM0 | cB.efM0) & buds.a0;
						delsM1 = (delsM1 | cB.efM1) & buds.a1;
						delsM2 = (delsM2 | cB.efM2) & buds.a2;
						// sharks: current sharks which see all fins
						shrkM0&=buds.a0; shrkM1&=buds.a1; shrkM2&=buds.a2;
						// if any deletes or any sharks
						if ( (delsM0|delsM1|delsM2|shrkM0|shrkM1|shrkM2) != 0 ) {
							// we found ourselves a complex Fish!
							deletes.set(delsM0, delsM1, delsM2);
							sharks.set(shrkM0, shrkM1, shrkM2);
							// nb: fins are already set in the above if statement
							hint = createHint(cB);
							searchCoversResult |= hint!=null;
							if ( accu.add(hint) ) // exit-early if accu says so
								return true;
						}
					}
				} else if ( coverLevel < degree ) {
					// move onto the next level
					cC = coverStack[++coverLevel];
					cC.index = c + 1;
					cC.prevIndex = -1;
				}
//			} // !seekMutant
		} // for ever
	}
	// these fields are (logically) searchCovers variables
	private ComplexFishHint hint; // the hint created, if any
	private CoverStackEntry pC, cC; // the previous and current CoverStackEntry
	private boolean searchCoversResult;
	private int c; // the current cover region index
	private int coverIndex; // index in grid.regions of the current cover
	private int coverLevel; // current coversStack level, ie coversSet size
	private int numCovers; // the number of covers in the covers array
	private int ground; // partial-calculation of the maximum cover index
	// fins: indices of exo-fins and endo-fins
	private int finsM0, finsM1, finsM2;
	// deletes: indices of potential eliminations
	private int delsM0, delsM1, delsM2;
	// sharks: cannabilistic eliminations
	private int shrkM0, shrkM1, shrkM2;
	// get a mask of the region-types used as bases and covers
	private int baseMask, coverMask;
	// count the number of rows, cols, and boxs used as a base region
	private final int[] usedBaseTypes = new int[3];
// underpins the dead cover filter
	// count the number of rows, cols, and boxs used as a cover region
//	private final int[] usedCoverTypes = new int[3];

//	@Override
//	public void report() {
//		if ( seekMutant ) {
//			double total = dones + skips;
//			Log.teef(
//				  tech.nom+": done=%,d (%4.2f\\%), skip=%,d (%4.2f\\%)\n"
//				, dones, ((double)dones)/(total)*100D
//				, skips, ((double)skips)/(total)*100D
//			);
//		}
//	}
//	private long skips=0L, dones=0L;

	/**
	 * Create a new HdkFishHint and return it, or null meaning "no hint".
	 * <p>
	 * I read these fields, which must be pre-set:<ul>
	 * <li>Constant: MIA_REDS true checks that each delete still exists.
	 * There's a problem with non-existent eliminations (reds). I log issues,
	 * and throw IllegalStateException if reds are null or empty.
	 * <li>withFins are we looking for fish with fins (true); or just basic
	 * fish (false).
	 * <li>candidate the Fish candidate value 1..9
	 * <li>basesUsed boolean[27] concurrent with grid.regions.
	 * true means this region is currently used as a base, else false.
	 * <li>coversUsed boolean[27] concurrent with grid.regions.
	 * true means this region is currently used as a cover, else false.
	 * <li>bases int[27] contains the index of this base in grid.regions
	 * <li>baseVs Idx[27] concurrent with bases: the grid.cells indice of each
	 * cell in each base with maybe the Fish candidate value
	 * <li>fins both exo-fins (candidates in bases andNot covers) and
	 * endo-fins (candidates in two bases: treat as fins and eliminate if
	 * they see ALL the fins, including endo-fins)
	 * <li>deletes are candidates to be deleted
	 * <li>sharks are cannibalistic deletes (endo-fins to be deleted)
	 * </ul>
	 *
	 * @param cB the current BaseStackEntry
	 * @return the new hint, else null meaning "no hint".
	 * @throw IllegalStateException if MIA_REDS and reds are null or empty.
	 */
	private ComplexFishHint createHint(BaseStackEntry cB) {

		// MIA_REDS: problem with non-existent reds, so log issues,
		// and throw IllegalStateException if reds come-out empty.
		final Pots reds = new Pots(); // set-of-Cell=>Values to be eliminated
		if ( MIA_REDS ) {
			final int sc = VSHFT[candidate]; // shiftedCandidate: the Fish candidate value as a left-shifted bitset, as per cell.maybes.bits
			// skip if there's no deletes and no sharks.
			if ( deletes.isEmpty() && sharks.isEmpty() ) {
				carp("no deletes and no sharks");
				return null; // pre-tested by each call so shouldn't occur
			}
			// check that each delete exists
			if ( deletes.any() ) { // there may occasionally only be sharks
				deletes.forEach(grid.cells, (c) -> {
					if ( (c.maybes.bits & sc) != 0 ) {
						reds.put(c, new Values(candidate));
					} else { // the "missing" delete was NOT added
						carp("MIA delete: "+c.toFullString()+"-"+candidate);
					}
				});
			}
			// check that each shark exists
			if ( sharks.any() ) { // there's usually only deletes
				// foreach shark (cannibalistic) cell in grid.cells
				sharks.forEach(grid.cells, (c) -> {
					if ( (c.maybes.bits & sc) != 0 ) {
						reds.put(c, new Values(candidate));
					} else { // the "missing" shark was NOT added
						carp("MIA shark: "+c.toFullString()+"-"+candidate);
					}
				});
			}
			// red (eliminated) Pots cannot be null or empty
			if ( reds.isEmpty() ) {
				throw new IllegalStateException("null/empty reds!"); // should never happen
			}
		} else {
			// normal operation: just add the deletes and sharks to reds,
			// and then return null (no hint) if reds come-out null or empty.
			if ( deletes.any() )
				deletes.forEach(grid.cells, (c)-> reds.put(c, new Values(candidate)));
			if ( sharks.any() )
				sharks.forEach(grid.cells, (c) -> reds.put(c, new Values(candidate)));
			if ( reds.isEmpty() )
				return null;
		}

		baseMask = getRegionTypesMask(basesUsed);
		coverMask = getRegionTypesMask(coversUsed);

		// a basic fish contains row->col or col->row (no boxs)
		final boolean basicFish = (baseMask==ROW_MASK && coverMask==COL_MASK)
							   || (baseMask==COL_MASK && coverMask==ROW_MASK);

		// look for Sashiminess (used to determine the type)
		boolean sashimiFish = false;
		if ( basicFish && seekFinned ) {
			// isSashimi = any base except fins has only one candidate
			for ( int i=0; i<numBases; ++i ) {
				if ( basesUsed[bases[i]]
				  // there never seems to be none, so don't test for it
				  && Idx.sizeLTE(baseVsM0[i] & ~fins.a0
							   , baseVsM1[i] & ~fins.a1
						       , baseVsM2[i] & ~fins.a2, 1) ) {
					sashimiFish = true;
					break;
				}
			}
		}

		// determine the type
		final ComplexFishHint.Type type;
		if ( basicFish ) {
			// Basic: row->col or col->row (ie no boxes), may have fins
			type = sashimiFish ? ComplexFishHint.Type.SASHIMI[degree - 2]
				 : !seekBasic ? ComplexFishHint.Type.FINNED[degree - 2]
				 : ComplexFishHint.Type.BASIC[degree - 2];
		} else if ( ((baseMask&BOX_MASK)!=0 && (coverMask&BOX_MASK)!=0)
				 || (baseMask&ROW_COL_MASK) == ROW_COL_MASK
				 || (coverMask&ROW_COL_MASK) == ROW_COL_MASK ) {
			// Mutant: boxes in bases AND covers
			//      or rows AND cols in bases or covers
			type = !seekBasic ? ComplexFishHint.Type.FINNED_MUTANT[degree - 2]
				 : ComplexFishHint.Type.MUTANT[degree - 2];
		} else {
			// Franken: boxes in bases OR covers
			type = !seekBasic ? ComplexFishHint.Type.FINNED_FRANKEN[degree - 2]
				 : ComplexFishHint.Type.FRANKEN[degree - 2];
		}

		// get the regions from the used boolean arrays
		final List<ARegion> usedBases = Regions.used(basesUsed, grid);
		final List<ARegion> usedCovers = Regions.used(coversUsed, grid);

		// a corner is a candidate in a base and a cover (ie not fins).
		final Idx cornerIdx = new Idx(cB.vsM0 & ~fins.a0
									, cB.vsM1 & ~fins.a1
									, cB.vsM2 & ~fins.a2);
		final Idx endoFinsIdx = new Idx(cB.efM0, cB.efM1, cB.efM2);
		final Idx exoFinsIdx = new Idx(fins.a0 & ~cB.efM0
									 , fins.a1 & ~cB.efM1
									 , fins.a2 & ~cB.efM2);

		// the Fish candidate as a Values
		final Values cv = new Values(candidate);
		// corners = green
		final Pots green = new Pots(cornerIdx.cells(grid), cv);
		// exoFins = blue
		final Pots blue = new Pots(exoFinsIdx.cells(grid), cv);
		// endoFins = purple
		final Pots purple = new Pots(endoFinsIdx.cells(grid), cv);
		// sharks = yellow
		final Pots yellow;
		if ( sharks.any() ) {
			yellow = new Pots(sharks.cells(grid), cv);
			// paint all sharks yellow (except eliminations which stay red)!
			if ( !yellow.isEmpty() )
				yellow.removeFromAll(green, blue, purple);
		} else
			yellow = null;

		// paint all eliminations red (including sharks)!
		reds.removeFromAll(green, blue, purple, yellow);

		// paint endo-fins purple, not corners (green) or exo-fins (blue).
		purple.removeFromAll(green, blue);

		String debugMessage = "ComplexFisherman";

		ComplexFishHint hint = new ComplexFishHint(this, type, sashimiFish
				, candidate, usedBases, usedCovers, reds, green, blue
				, purple, yellow, debugMessage);

		if ( HintValidator.COMPLEX_FISHERMAN_USES ) {
			// only needed for Franken and Mutant, but just check all
			if ( !HintValidator.isValid(grid, hint.redPots) ) {
				hint.isInvalid = true;
				HintValidator.report("ComplexFisherman", grid, squeeze(hint.toHtmlImpl()));
				return null; // Sigh.
			}
		}

		// just return the hint coz my caller may upgrade to HdkKrakenFishHint
		// and then add it to the accu. Sigh.
		return hint;
	}

	private static void carp(String msg) {
		Log.teeln("ComplexFisherman: "+msg);
	}

	/**
	 * find the bases and allCovers for a Fish of my type and {@link #degree}.
	 * The actual covers are calculated in searchCovers because eligibility
	 * depends on the current bases.
	 *
	 * @return are there at least degree bases and allCovers; else we can skip
	 * this Fish search when I return false.
	 */
	private boolean findEligibleBasesAndCovers() {
		numBases = numAllCovers = 0;
		addRegions(grid.rows, baseType==ROW, seekMutant);
		addRegions(grid.cols, baseType==COL, seekMutant);
		// BOXS in Franken: bases AND covers, used in either but not both.
		// BOXS in Mutant: bases AND covers.
		if ( seekFranken || seekMutant )
			addRegions(grid.boxs, BASE, true);
		return numBases>=degree && numAllCovers>=degree;
	}

	private void addRegions(ARegion[] regions, boolean isBase, boolean andConverse) {
		for ( ARegion region : regions )
			// ignore regions which already have the Fish candidate set.
			// nb: containsValue is rebuilt by LogicalSolver and TestHelp.
			if ( !region.containsValue[candidate] )
				addRegion(region, isBase, andConverse);
	}

	private void addRegion(ARegion region, boolean isBase, boolean andConverse) {
		// indices of cells which maybe the fish candidate value in this region
		final Idx vs = region.idxs[candidate];
		if ( isBase ) { // region is a base
			// basic fish bases have upto degree candidates, but not more.
			if ( !seekBasic || region.indexesOf[candidate].size<=degree ) {
				bases[numBases] = region.index;
				baseVsM0[numBases] = vs.a0;
				baseVsM1[numBases] = vs.a1;
				baseVsM2[numBases++] = vs.a2;
			}
		} else { // region is a cover
			allCovers[numAllCovers] = region.index;
			allCoverVsM0[numAllCovers] = vs.a0;
			allCoverVsM1[numAllCovers] = vs.a1;
			allCoverVsM2[numAllCovers++] = vs.a2;
		}
		if ( andConverse )
			addRegion(region, !isBase, false);
	}

}
