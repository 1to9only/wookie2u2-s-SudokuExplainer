/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.COL;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.WORDS;
import static diuf.sudoku.Idx.WORD_MASK;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import static diuf.sudoku.Regions.BOX_MASK;
import static diuf.sudoku.Regions.COL_MASK;
import static diuf.sudoku.Regions.REGION_TYPE;
import static diuf.sudoku.Regions.ROW_COL_MASK;
import static diuf.sudoku.Regions.ROW_MASK;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.Validator;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.MINUS;
import diuf.sudoku.utils.Log;
import static java.lang.Integer.bitCount;
import java.util.Arrays;
import java.util.List;

/**
 * ComplexFisherman implements Basic Fish (commented-out), Franken Fish, and
 * Mutant Fish Sudoku solving techniques, but not Kraken Fish. Basic Fish code
 * is commented-out because BasicFisherman is faster.
 * <p>
 * This started as a copy-paste of HoDoKu's FishSolver, modified to use Sudoku
 * Explainers model, chainers, and hints; and import all HoDoKu dependencies.
 * I ripped-out Kraken Fish coz I'm too stupid to work-out how to implement
 * them using my chainers. Kudos to hobiwan. Mistakes are mine. ~KRC
 * <p>
 * Fish have there own language. Here's a brief precis, defining terminology.
 * Other folks have other perspectives. There's more than one way to do it.
 * <p>
 * Basic Fish: $degree base regions (rows/cols) share $degree positions for a
 * value, and every region has exactly one instance of each value, hence value
 * is "locked into" these base regions, therefore value can be eliminated from
 * all other places in the cross-regions, called the covers.
 * <p>
 * Degree is the fish size, ie the number of bases, and num covers:<ul>
 * <li>The fish size is stored in super.degree, from the passed Tech.
 * <li>2 regions are Swampfish, not X-Wing. Recall Yoda raising Luke's X-Wing
 * from the swamp. I demur because fish should have fish-names, not wing-names,
 * especially in software that already has wings. Mispatterned nominatives
 * matter when all you have to go-on are names. You name your kid Mr Ed and are
 * surprised when blind folks think he's a horse. We're all born blind.
 * <li>3 regions are called Swordfish.
 * <li>4 regions are called Jellyfish.
 * <li>Larger fish {5=Squirmbag, 6=Whale, 7=Leviathan} are all degenerate
 * (combinations of simpler patterns) so I do not (though I can) seek them.
 * </ul>
 * <p>
 * So, the regions we look in are called "bases". The crossing regions that we
 * actually eliminate from are called the "covers". So we: <ul>
 * <li>first search the rows (bases), and the cols are our covers;
 * <li>then bases are cols, and covers are rows (they swap roles).
 * <li>Mutant Fish (below) need only one search (includes all bases).
 * </ul>
 * <p>
 * All the "complex" fish types have fins (below) to expand the definitions of
 * basic fish to find more hints, but with reduced eliminations, so we're doing
 * more work for less pay, ergo s__t gets harder.
 * <p>
 * A Finned Fish is a basic fish just with one-or-more fins. A fin is an extra
 * position for the fish value in a base region. Fins expand the number of
 * matches but also reduce the number of eliminations; because each elimination
 * must see all fins.
 * <p>
 * Sashimi Fish rely on a fin (else they're degenerate) to allow us to drop a
 * corner from the normal Fish pattern: replacing a corner with a fin. Sashimi
 * Fish are now found in the "normal" Finned Fish search.
 * <p>
 * Franken Fish adds a box to the bases OR the covers.
 * <p>
 * Mutant Fish adds boxes to the bases AND the covers.
 * <p>
 * KrakenFisherman (not me) is fish + chaining: If each fish configuration
 * and each fin has an XY-forcing-chain to the same elimination, then that
 * elimination cannot be false, so it can be applied.
 * <p>
 * 2020-09-28 after some hard-thinking I've dropped Kraken for now in order to
 * get the rest of it working first. The plan is to come back and nut-out how
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
 * </pre>
 *
 * @author Keith Corlett 2020-09-24
 */
public class ComplexFisherman extends AHinter
//		implements diuf.sudoku.solver.IReporter
{
//DEBUG
//	@Override
//	public void report() {
//		if ( seekMutant ) {
//			double total = dones + skips;
//			Log.teef(
//				  tech.name()+": done=%,d (%4.2f\\%), skip=%,d (%4.2f\\%)\n"
//				, dones, ((double)dones)/(total)*100D
//				, skips, ((double)skips)/(total)*100D
//			);
//		}
//	}
//	private long skips=0L, dones=0L;

//DEBUG
//	private String debugContainsValue(boolean[] array) {
//		StringBuilder sb = new StringBuilder(REGION_SIZE);
//		for ( int v=1; v<VALUE_CEILING; ++v )
//			if ( array[v] )
//				sb.append(v);
//			else
//				sb.append("_");
//		return sb.toString();
//	}

	// MIA is an acronym for "Missing In Action". Reds are the eliminations.
	// Problem: grid does NOT contain cell-value/s that are being eliminated.
	// Fix: createHint checks for missing deletes/sharks, throwing "No reds".
	// This is a HACK: No non-existant cell-value should ever be eliminated but
	// it has happened, and I know not why, so treat symptoms, not the disease.
	// True logs eliminations that don't exist in the grid, and drops them.
	private static final boolean MIA_REDS = false;

	// maximum number of fins (extra cells which maybe v in a base)
	// NOTE: 3 finds the same number of hints as 4, but a Swamp goes Sword.
	// 5 finds exactly the same hints as 4. This has big performance impact.
	private static final int MAX_FINS = 3;

	// one more than the maximum number of endo-fins (candidates in two
	// overlapping bases), because I use < which coz it's faster than <=.
	private static final int MAX_ENDO_FINS = 3;

	// @returns a new array containing each element in a + x (the offset, which
	//  in practice is the distance of this word/row from start of grid).
	private static int[] plus(final int[] a, final int x) {
		final int n = a.length;
		final int[] result = new int[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = a[i] + x;
		}
		return result;
	}

	// WORDS_* are Idx.WORDS with 9..72 added to them, to precalculate once
	// instead of adding BILLIONS of times in anyCommonBuddies. These extra
	// fields use heaps of RAM, but still save 17 seconds on top1465. They're
	// local so that if ComplexFisherman is not used (as is common) then the
	// class is never referenced, so these statics are not created. That's part
	// of the reasoning behind Tech: a light place-holder for a heavy class.
	// Idx might be faster if it did this, but it'd probably be slower overall
	// coz of all the extra RAM tied-up in these array-of-arrays. So to be
	// clear, these exist only for speed, and pay-back coz they're hammered!
	private static final int[][] WORDS_09 = new int[1<<9][]; // WORDS +  9
	private static final int[][] WORDS_18 = new int[1<<9][]; // WORDS + 18
	private static final int[][] WORDS_27 = new int[1<<9][]; // WORDS + 27
	private static final int[][] WORDS_36 = new int[1<<9][]; // WORDS + 36
	private static final int[][] WORDS_45 = new int[1<<9][]; // WORDS + 45
	private static final int[][] WORDS_54 = new int[1<<9][]; // WORDS + 54
	private static final int[][] WORDS_63 = new int[1<<9][]; // WORDS + 63
	private static final int[][] WORDS_72 = new int[1<<9][]; // WORDS + 72
	static {
		int[] a;
		for ( int i=0; i<WORDS.length; ++i ) {
			a = WORDS[i];
			WORDS_09[i] = plus(a, 9);
			WORDS_18[i] = plus(a, 18);
			WORDS_27[i] = plus(a, 27);
			WORDS_36[i] = plus(a, 36);
			WORDS_45[i] = plus(a, 45);
			WORDS_54[i] = plus(a, 54);
			WORDS_63[i] = plus(a, 63);
			WORDS_72[i] = plus(a, 72);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~ working storage ~~~~~~~~~~~~~~~~~~~~~~~~

	/** A recursion stack entry for each base region in the current baseSet. */
	final class BaseStackEntry {
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
		// debug only, not used in actual code.
		@Override
		public String toString() {
			return ""+index+": "+Idx.toString(vsM0, vsM1, vsM2);
		}
	}

	/** array of all possible base units (indices in grid.regions). */
	private final int[] bases = new int[NUM_REGIONS];
	/** number of eligible bases. */
	private int numBases;
	/** true means base at index is in current search: boxs,rows,cols. */
	private final boolean[] basesUsed = new boolean[NUM_REGIONS];
	/** count the number of rows,cols,boxs used as base in current search. */
	private final int[] usedBaseTypes = new int[3];
	/** recursion stack for the base region search. */
	private final BaseStackEntry[] baseStack = new BaseStackEntry[REGION_SIZE];
	/** indices of grid.cells which maybe v in bases;<br>
	 * concurrent with bases (not grid.regions as you might expect);<br>
	 * nb: v is shorthand for the fish candidate value. */
	private final int[] baseVsM0 = new int[NUM_REGIONS];
	private final int[] baseVsM1 = new int[NUM_REGIONS];
	private final int[] baseVsM2 = new int[NUM_REGIONS];

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
		// debug only, not used in actual code.
		@Override
		public String toString() {
			return ""+index+": "+Idx.toString(vsM0, vsM1, vsM2);
		}
	}

	/** array of all eligible cover regions (indices in Grid.regions). */
	private final int[] allCovers = new int[NUM_REGIONS];
	/** number of allCovers. */
	private int numAllCovers;
	/** indices of grid.cells maybe v (Fish candidate value) in allCovers. */
	private final int[] allCoverVsM0 = new int[NUM_REGIONS];
	private final int[] allCoverVsM1 = new int[NUM_REGIONS];
	private final int[] allCoverVsM2 = new int[NUM_REGIONS];

	/** array of current cover regions (indices in grid.regions). */
	private final int[] covers = new int[NUM_REGIONS];
	/** indices of grid.cells maybe v (Fish candidate value) in allCovers. */
	private final int[] coverVsM0 = new int[NUM_REGIONS];
	private final int[] coverVsM1 = new int[NUM_REGIONS];
	private final int[] coverVsM2 = new int[NUM_REGIONS];
	/** is the cover region at this index in the current search. */
	private final boolean[] coversUsed = new boolean[NUM_REGIONS];
	/** the recursion stack for the cover region search. */
	private final CoverStackEntry[] coverStack = new CoverStackEntry[REGION_SIZE];

	/** the Fish candidate value, called 'v' for short. */
	private int v;
	/** indices of cells which maybe the Fish candidate value, v. */
	private Idx idx;
	/** DECOMPOSED indices of cells which maybe the Fish candidate value, v. */
	private int idx0, idx1, idx2;
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
	private boolean oneOnly;

	// these fields are set by findHints (values rely on parameters)
	/** The Grid to search for Fish hint/s. */
	private Grid grid;
	/** The cells array of the Grid to search for Fish hint/s. */
	private Cell[] cells;
	/** The IAccumulator to which I add hint/s. */
	private IAccumulator accu;

	/**
	 * The Constructor.
	 * @param tech Tech.Finned/Franken/Mutant Swamp/Sword/Jellyfish
	 */
	public ComplexFisherman(final Tech tech) {
		super(tech);
		// This thing can do bigger fish, but they're all degenerate, and slow!
		assert degree>=2 && degree<=4; // 2=Swampfish, 3=Swordfish, 4=Jellyfish
		// these are ALL the complex fish types we seek (not Kraken)
		seekFinned = tech.name().startsWith("Finned");
		seekFranken = tech.name().startsWith("Franken");
		seekMutant = tech.name().startsWith("Mutant");
		// it's basic unless it's one of the complex fish types.
		// NB: ComplexFisherman NOT used for basic fish, because BasicFisherman
		// is faster; but it CAN find basic fish, just uncomment-out the code.
		seekBasic = !seekFinned && !seekFranken && !seekMutant;
		assert !seekBasic : "Basic-fish are found faster by the BasicFisherman, so the basic-fish code is commented-out in the ComplexFisherman!";

		// initialise the two recursion-stacks
		for ( i=0; i<REGION_SIZE; ++i ) {
			baseStack[i] = new BaseStackEntry();
			coverStack[i] = new CoverStackEntry();
		}
	}

	/**
	 * findHints: foreach candidate 1..9: if templates say there's possible
	 * eliminations then {@link #searchBases(boolean)} for Fish.
	 * Search rows first, then cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 * @param grid
	 * @param accu
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		this.grid = grid;
		this.cells = grid.cells;
		this.accu = accu;
		this.oneOnly = accu.isSingle();
		int pre = accu.size();
		try {
			final Idx[] idxs = grid.idxs;
// templates are useless because EVERYthing passes!
//			final Idx[]	deletables = Run.templates.getDeletables(grid);
//			int done=0, skip=0;
			for ( int v=1; v<VALUE_CEILING; ++v ) {
//				// skip if no idxs[v] are deletable
//				if ( deletables[v].andAny(idxs[v]) ) {
//					++done;
//				} else {
//					++skip;
//					continue;
//				}
				this.v = v;
				this.idx = idxs[v];
				this.idx0 = idx.a0;
				this.idx1 = idx.a1;
				this.idx2 = idx.a2;
				// first search rows for fish
				// Mutant searches both ROWs and COLs
				if ( searchBases(ROW) && oneOnly ) {
					break; // exit-early
				}
				// then search cols for fish
				// Mutant needs only the above search
				if ( !seekMutant && searchBases(COL) && oneOnly ) {
					break; // exit-early
				}
			}
		} finally {
			this.grid = null;
			this.cells = null;
			this.accu = null;
			Cells.cleanCasA();
		}
		return accu.size() > pre;
	}

	/**
	 * Search all possible combinations of {@link #degree} base regions.
	 *
	 * @param baseType ROW, or COL
	 * @return were hint/s found
	 */
	private boolean searchBases(final int baseType) {

		// presume that no hints will be found
		searchBasesResult = false;
		// find eligible bases and allCovers; the actual covers are calculated
		// in searchCovers because eligibility depends on the current bases.
		if ( !selectBasesAndAllCovers(baseType) ) {
			return false;
		}

		// clear everything before we start
		// initialise the counts of boxs, rows, and cols used a base region
		pB = baseStack[0];
		// vs: indices of Grid.cells which maybe v (the Fish candidate value).
		pB.vsM0=pB.vsM1=pB.vsM2 = 0;
		// endo-fins: cells in more than one base regions, which may overlap
		// coz we're handling row/cols (and boxs in Franken and Mutant fish).
		pB.efM0=pB.efM1=pB.efM2 = 0;
		bLevel = 1; // baseLevel: start at level 1 (0 is just a stopper)
		cB = baseStack[1];
		cB.index = 0;
		cB.prevIndex = -1;
		// clear in case we exited-early last time
		Arrays.fill(basesUsed, false);
		usedBaseTypes[BOX]=usedBaseTypes[ROW]=usedBaseTypes[COL] = 0;

		// try all combinations of base regions
		for (;;) {
			// fallback a baseLevel if no unit is available at this baseLevel
			while ( (cB=baseStack[bLevel]).index >= numBases ) {
				// fallback one level at a time to maintain basesUsed
				if ( cB.prevIndex != -1 ) {
					basesUsed[cB.prevIndex] = false;
					--usedBaseTypes[REGION_TYPE[cB.prevIndex]];
					cB.prevIndex = -1;
				}
				if ( --bLevel < 1 ) { // ie <= 0 (level 0 is just a stopper!)
					return searchBasesResult; // we've tried all combos!
				}
			}
			// unuse the-previous-base-at-this-level
			// hijack baseIndex coz I don't really need another variable
			if ( cB.prevIndex != -1 ) {
				basesUsed[cB.prevIndex] = false;
				--usedBaseTypes[REGION_TYPE[cB.prevIndex]];
			}
			// get the previous levels BaseStackEntry, containing the existing
			// candidates and endoFins sets (the union of ALL bases so-far).
			pB = baseStack[bLevel - 1];
			// get the next base (must exist or we would have fallen back)
			b = cB.index++;
			// use bases[b] at-this-level
			cB.prevIndex = bases[b]; // remember prev to unuse
			basesUsed[cB.prevIndex] = true;
			++usedBaseTypes[REGION_TYPE[cB.prevIndex]];

			// v is shorthand for the fish candidate value
			// current v's = previous v's + v's in this base
			vsM0 = cB.vsM0 = pB.vsM0 | baseVsM0[b];
			vsM1 = cB.vsM1 = pB.vsM1 | baseVsM1[b];
			vsM2 = cB.vsM2 = pB.vsM2 | baseVsM2[b];

			// endos: current = existing | (v's in bases AND in this base)
			efM0 = cB.efM0 = pB.efM0 | (pB.vsM0 & baseVsM0[b]);
			efM1 = cB.efM1 = pB.efM1 | (pB.vsM1 & baseVsM1[b]);
			efM2 = cB.efM2 = pB.efM2 | (pB.vsM2 & baseVsM2[b]);

			// if we're still collecting bases
			if ( bLevel < degree ) {
				// if there's no endofins
				if ( (efM0|efM1|efM2) == 0
				  // or there's not too many endofins already
				  || bitCount(efM0)+bitCount(efM1)+bitCount(efM2) < MAX_ENDO_FINS ) {
					// onto the next level
					cB = baseStack[++bLevel];
					cB.index = b + 1;
					cB.prevIndex = -1;
				}
			// else we've collected degree covers, so
			// if there's no endofins to check
			} else if ( (efM0|efM1|efM2) == 0
				// or there's not too many endofins
				|| ( bitCount(efM0)+bitCount(efM1)+bitCount(efM2) < MAX_ENDO_FINS
				  // which have common buddy/s that maybe v
				  && endoFins.set(efM0,efM1,efM2).commonBuddies(buds).and(idx).any() )
			) {
				if ( searchCovers() ) {
					// we found a Fish!
					searchBasesResult = true;
					if ( oneOnly ) {
						return searchBasesResult;
					}
				}
			}
			interrupt();
		}
	}
	// these fields are (logically) searchBases variables
	private boolean searchBasesResult;
	// the previous and current BaseStackEntry
	private BaseStackEntry pB, cB;
	// current bases-array index (and parallel arrays baseVs)
	private int b;
	// endo-fins: fish candidate in the-existing-bases AND the-new-base
	private int efM0, efM1, efM2;
	// vsM* is a shortcut to cB.vsM*, to not deref cB repeatedly in searchCovers
	private int vsM0, vsM1, vsM2;
	// baseLevel is our depth in the baseStack, ie number of bases in baseSet.
	private int bLevel;

	/**
	 * Search all combinations of covers that fit the current bases (cB).
	 * <p>
	 * To "fit" a cover must:<ul>
	 * <li>intersect (have a candidate in common with) the current bases; and
	 * <li>not be a current base; and
	 * <li>a Franken fish has a box as base OR a cover, but not both.
	 * </ul>
	 * <p>
	 * SPEED: searchCovers has zero variables, so it's invocation does NOT
	 * create a stackframe, which is faster. It comes with the cost of field
	 * access for EVERYthing, which sucks, but you can't have it both ways.
	 * searchCovers calls anyCommonBuddies which also has no local variables,
	 * and is therefore stackframeless. This is ~15% faster overall than the
	 * normal Idx.commonBuddies, which is still too slow. If I could have any-
	 * thing right-now it'd be a really fast way to calculate commonBuddies.
	 *
	 * @return were hint/s found?
	 */
	private boolean searchCovers() {
		// presume that no hints will be found
		searchCoversResult = false;
		// get eligible covers from allCovers; done here because eligibility
		// depends on which bases are used.
		numCovers = 0;
		boxIsNotBase = usedBaseTypes[BOX]==0;
		for ( ii=0; ii<numAllCovers; ++ii ) {
			// skip if this cover has no cands in common with current bases
			if ( ( (vsM0 & allCoverVsM0[ii])
				 | (vsM1 & allCoverVsM1[ii])
				 | (vsM2 & allCoverVsM2[ii]) ) != 0
			  // skip if this cover is already used as a base
			  && !basesUsed[allCovers[ii]]
			  // Franken: box as base OR cover, but not both (0..8 are boxs)
			  && (!seekFranken || boxIsNotBase || allCovers[ii]>8)
			) {
				covers[numCovers] = allCovers[ii];
				coverVsM0[numCovers] = allCoverVsM0[ii];
				coverVsM1[numCovers] = allCoverVsM1[ii];
				coverVsM2[numCovers++] = allCoverVsM2[ii];
			}
		}
		// need atleast degree covers to form a Fish.
		if ( numCovers < degree ) {
			return false; // this'll only happen rarely.
		}
		// partial-calculation of the last cover index.
		floor = numCovers - degree - 1;
		// coverLevel: the current depth in the coverStack
		// coverLevel: start at level 1 (0 is just a stopper)
		// currentCover: the current CoverStackEntry
		cC = coverStack[cLevel = 1];
		cC.index = 0;
		cC.prevIndex = -1; // meaning none
		// clean-up from last time, in case we returned.
		Arrays.fill(coversUsed, false);
		// foreach each distinct combo of covers
//<HAMMERED comment="BILLIONS: No continue, label, var, terniary, method">
		for (;;) {
			// while coverLevel exhausted fallback a coverLevel
			// exhausted means there's no more covers to be searched
			// calculating the last index at this coverLevel is tricky
			// and AFAIK it's impossible to calculate quickly
			// this is the best I can do
			// nb: cC (current Cover) is also set here
			while ( (cC=coverStack[cLevel]).index > floor + cLevel ) {
				// unuse the previous cover
				if ( cC.prevIndex != -1 ) {
					coversUsed[cC.prevIndex] = false;
					cC.prevIndex = -1;
				}
				// fallback
				if ( --cLevel < 1 ) { // 0 is just a stopper
					return searchCoversResult; // done
				}
			}
			// unuse the previous cover
			if ( cC.prevIndex != -1 ) {
				coversUsed[cC.prevIndex] = false;
			}
			// squished into one line for speed
			// get next covers (exists or we would have fallen back)
			// remember prevIndex to unuse this cover
			// use covers[c] as the current cover
			coversUsed[cC.prevIndex=covers[c=cC.index++]] = true;
			// set the previous Cover (agglomeration of all existing covers)
			pC = coverStack[cLevel - 1];
			// current v's = previous v's | v's in this cover (agglomerate)
			cC.vsM0 = pC.vsM0 | coverVsM0[c];
			cC.vsM1 = pC.vsM1 | coverVsM1[c];
			cC.vsM2 = pC.vsM2 | coverVsM2[c];
			// if this cover has candidates in common with the existing covers
			// then those candidates become possible eliminations, which I call
			// sharks because it's shorter than cannabilistic. The Great White
			// Shark: a non-placental bearthing predatory young. Do the math!
			// sharks: current = previous | (v's in prev covers & this cover)
			cC.skM0 = pC.skM0 | (pC.vsM0 & coverVsM0[c]);
			cC.skM1 = pC.skM1 | (pC.vsM1 & coverVsM1[c]);
			cC.skM2 = pC.skM2 | (pC.vsM2 & coverVsM2[c]);
			// if we're still collecting covers
			if ( cLevel < degree ) {
				// move onto the next level
				// starting at the cell after the current cell at this level
				coverStack[++cLevel].index = c + 1;
			} else {
				// we have enough covers, so search for fish
				// fins = current endo-fins | v's in bases outside covers
				f0 = efM0 | (vsM0 & ~cC.vsM0);
				f1 = efM1 | (vsM1 & ~cC.vsM1);
				f2 = efM2 | (vsM2 & ~cC.vsM2);
//				// basic fish (no fins)
//				if ( seekBasic ) {
//					// no fins
//					if ( (fM0|fM1|fM2)==0 ) {
//						// deletes = v's in covers except bases
//						dlM0 = cC.vsM0 & ~vsM0;
//						dlM1 = cC.vsM1 & ~vsM1;
//						dlM2 = cC.vsM2 & ~vsM2;
//						// any deletes or sharks
//						if ( (dlM0|dlM1|dlM2|cC.skM0|cC.skM1|cC.skM2) != 0 ) {
//							// ***************** BASIC FISCH ******************
//							// Eliminate candidates in covers except bases.
//							deletes.set(dlM0, dlM1, dlM2);
//							// shark: If a base v is in >1 cover its lunch.
//							sharks.set(cC.skM0, cC.skM1, cC.skM2);
//							fins.clear();
//							if ( (hint=createHint()) != null ) {
//								searchCoversResult = true;
//								if ( accu.add(hint) ) // if accu says so
//									return searchCoversResult; // exit-early
//								hint = null;
//							}
//						}
//					}
//				} else
				// complex fish need fins
				if ( (f0|f1|f2) != 0
				  // but not too many of them
				  && bitCount(f0)+bitCount(f1)+bitCount(f2) <= MAX_FINS
				  // with common buddy/s that maybe v (stackframeless = speed)
				  && anyCommonBuddies() // sets cb0,cb1,cb2
				) {
					// ******************* COMPLEX FISCH ******************
					// Candidate is deletable if in covers but not bases,
					// or belongs to more than one cover set (an endo-fin).
					// deletes=((covers & ~bases) | endos) seeing all fins
					dlM0 = ((cC.vsM0 & ~vsM0) | efM0) & cb0;
					dlM1 = ((cC.vsM1 & ~vsM1) | efM1) & cb1;
					dlM2 = ((cC.vsM2 & ~vsM2) | efM2) & cb2;
					// sharks: current sharks which see all fins
					skM0 = cC.skM0 & cb0;
					skM1 = cC.skM1 & cb1;
					skM2 = cC.skM2 & cb2;
					// if any deletes or any sharks
					if ( (dlM0|dlM1|dlM2|skM0|skM1|skM2) != 0 ) {
//</HAMMERED>
						// we found ourselves a complex Fish!
						deletes.set(dlM0, dlM1, dlM2);
						sharks.set(skM0, skM1, skM2);
						fins.set(f0, f1, f2);
						// nb: fins are already set in the above if statement
						final AHint hint = createHint();
						if ( hint != null ) {
							searchCoversResult = true;
							if ( accu.add(hint) ) { // if accu says so
								return searchCoversResult; // exit-early
							}
						}
					}
				}
			}
		}
	}
	// budsFin: buddies of this fin cell (sorry about the short, bad name).
	private Idx bf;
	// these fields are (logically) searchCovers variables
	private CoverStackEntry pC, cC; // the previous and current CoverStackEntry
	private boolean searchCoversResult;
	private int ii; // index for use in searchCovers
	private int c; // the current cover region index
	private int cLevel; // current coversStack level, ie coversSet size
	private int numCovers; // the number of covers in the covers array
	private int floor; // partial-calculation of the last cover index
	// fins: indices of exo-fins and endo-fins
	private int f0, f1, f2;
	// deletes: indices of potential eliminations
	private int dlM0, dlM1, dlM2;
	// sharks: cannabilistic eliminations
	private int skM0, skM1, skM2;
	// get a mask of the region-types used as bases and covers
	private int baseMask, coverMask;
	// !(is a BOX used as a base)?
	private boolean boxIsNotBase;

	//<NO_WRAPPING comment="wrapping makes this harder to read">
	/**
	 * FAST: do the fin/s have any common buddy/s that maybe v? <br>
	 * IE: {@code fins.set(f0,f1,f2).commonBuddies(buds).and(vs).any()} <br>
	 * sets {cb0,cb1,cb2} to buddies of all fins {f0,f1,f2} and returns any? <br>
	 * <p>
	 * For speed, anyCommonBuddies is stackframeless and exploded (verbose).
	 * I'm about 17% faster overall than Idx.commonBuddies, which is quite a
	 * significant improvement, but comes at the cost of inflexibility.
	 * <p>
	 * PLEASE EXPLAIN: cb0,cb1,cb2 is an Idx of commonBuddies, set to indices
	 * in the grid that maybe v, then we boolean-and that set with the buddies
	 * of each fin in f0,f1,f2. Each BUDDIES excludes the cell itself, so we
	 * end-up with cells that see all fin cells, except the fins themselves,
	 * ergo the common buddies of all of the fin cells.
	 * <p>
	 * The aspect that makes a mess is doing it all as fast as possible, which
	 * boils down to minimising the amount of repetitious work, which is where
	 * WORDS_09 (et al) come in: we've already pre-added the distance from the
	 * start of the grid to each-word-we-are-reading, just to save doing these
	 * additions billions of times.
	 * <p>
	 * Programmers find brevity and flexibility convenient, so we must rejig
	 * our brains to seek performance, which can be rather inconvenient. Speed
	 * costs inflexibility. But what is the probability that this will need to
	 * change? Speed vs Flex. Cheese. Cracker. Balance. That's all I'm saying.
	 * <p>
	 * I'm proud of this method, because it's FAST, even though it's ugly.
	 *
	 * @return commonBuddies {fields: cb0,cb1,cb2} is not empty,
	 *  ie do the fin/s have any common buddy/s that maybe v?
	 */
	private boolean anyCommonBuddies() {
		// set the common buddies to indices of cells in grid which maybe v
		cb0=idx0; cb1=idx1; cb2=idx2;
		// if the top third of fins index is notEmpty
		if ( f0 != 0 ) {
			// if the first row (9 bits) of fins index is notEmpty
			if ( (n=(word=WORDS[f0 & WORD_MASK]).length) != 0 )
				// foreach fin in first row (ie set (1) bit in this word)
				for ( i=0; i<n; ++i )
					// bail if (cmnBuds &= buddies-of-this-fin) isEmpty
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			// the second 9 bits (WORDS_09 values pre-moved down to second row)
			if ( (n=(word=WORDS_09[(f0>>9) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			// the third 9 bits (making 27 bits, which is a third of a Grid)
			if ( (n=(word=WORDS_18[(f0>>18) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
		}
		// the middle third of fins index
		if ( f1 != 0 ) {
			if ( (n=(word=WORDS_27[f1 & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			if ( (n=(word=WORDS_36[(f1>>9) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			if ( (n=(word=WORDS_45[(f1>>18) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
		}
		// the bottom third of fins index
		if ( f2 != 0 ) {
			if ( (n=(word=WORDS_54[f2 & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			if ( (n=(word=WORDS_63[(f2>>9) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
			if ( (n=(word=WORDS_72[(f2>>18) & WORD_MASK]).length) != 0 )
				for ( i=0; i<n; ++i )
					if ( ((cb0&=(bf=BUDDIES[word[i]]).a0) | (cb1&=bf.a1) | (cb2&=bf.a2)) == 0 )
						return false;
		}
		return true; // commonBuddies {cb0,cb1,cb2} is not empty
	}
	// common buddies of all fins (an Idx)
	private int cb0,cb1,cb2;
	// index and number thereof
	private int i, n;
	// an array of the indexes of set-bits in a 9-bit word
	private int[] word;
	//</NO_WRAPPING>

	/**
	 * Create a new ComplexFishHint and return it, or null meaning "no hint".
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
	 * @return the new hint, else null meaning "no hint".
	 * @throw IllegalStateException if MIA_REDS and reds are null or empty.
	 */
	private AHint createHint() {
		final Pots reds = new Pots(); // set-of-Cell=>Values to be eliminated
		// problem with non-existent reds, so log issues,
		// throw IllegalStateException if reds come-out empty.
		if ( MIA_REDS ) {
			final int sv = VSHFT[v]; // bitset of the Fish candidate value
			// skip if there's no deletes and no sharks.
			if ( deletes.none() && sharks.none() ) {
				carp("no deletes and no sharks");
				return null; // pre-tested by each call so shouldn't occur
			}
			// check that each delete exists
			if ( deletes.any() ) { // there may occasionally only be sharks
				deletes.forEach(cells, (c) -> {
					if ( (c.maybes & sv) != 0 ) {
						reds.put(c, sv);
					} else { // the "missing" delete was NOT added
						carp("MIA delete: "+c.toFullString()+MINUS+v);
					}
				});
			}
			// check that each shark exists
			if ( sharks.any() ) { // there's usually only deletes
				// foreach shark (cannibalistic) cell in grid.cells
				sharks.forEach(cells, (c) -> {
					if ( (c.maybes & sv) != 0 ) {
						reds.put(c, sv);
					} else { // the "missing" shark was NOT added
						carp("MIA shark: "+c.toFullString()+MINUS+v);
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
				reds.upsertAll(deletes, grid, v);
			if ( sharks.any() )
				reds.upsertAll(sharks, grid, v);
			if ( reds.isEmpty() ) {
				return null; // should never happen. Never say never.
			}
		}

		baseMask = Regions.types(basesUsed);
		coverMask = Regions.types(coversUsed);

		// a basic fish contains row->col or col->row (no boxs)
		final boolean basicFish = (baseMask==ROW_MASK && coverMask==COL_MASK)
							   || (baseMask==COL_MASK && coverMask==ROW_MASK);

		// a sashimi fish has a base with just one non-fin v
		final boolean isSashimi = basicFish && seekFinned && isSashimi();

		// determine the type
		final ComplexFishHint.Type type;
		if ( basicFish ) {
			// Basic: row->col or col->row (ie no boxes), may have fins
			type = isSashimi ? ComplexFishHint.Type.SASHIMI[degree - 2]
				 : !seekBasic ? ComplexFishHint.Type.FINNED[degree - 2]
				 : ComplexFishHint.Type.BASIC[degree - 2];
		} else if ( ((baseMask&BOX_MASK)!=0 && (coverMask&BOX_MASK)!=0)
				 || (baseMask&ROW_COL_MASK) == ROW_COL_MASK
				 || (coverMask&ROW_COL_MASK) == ROW_COL_MASK ) {
			// Mutant: boxes in bases AND covers
			//      or rows AND cols in bases
			//      or rows AND cols in covers
			type = !seekBasic ? ComplexFishHint.Type.FINNED_MUTANT[degree - 2]
				 : ComplexFishHint.Type.MUTANT[degree - 2];
		} else {
			// Franken: boxes in bases XOR covers
			type = !seekBasic ? ComplexFishHint.Type.FINNED_FRANKEN[degree - 2]
				 : ComplexFishHint.Type.FRANKEN[degree - 2];
		}

		// get the regions from the used boolean arrays
		final ARegion[] basesL = Regions.used(basesUsed, grid);
		final ARegion[] coversL = Regions.used(coversUsed, grid);

		// a corner is a candidate in a base and a cover (ie not fins).
		final Idx cornerIdx = new Idx(vsM0 & ~fins.a0
									, vsM1 & ~fins.a1
									, vsM2 & ~fins.a2);
		// the endo-fins are v's that are in more than one base, so its a fin
		final Idx endoFinsIdx = new Idx(efM0, efM1, efM2);
		// the exo-fins are extra v's in bases (normally called just "fins")
		final Idx exoFinsIdx = new Idx(fins.a0 & ~efM0
									 , fins.a1 & ~efM1
									 , fins.a2 & ~efM2);

		// the Fish candidate as a Values
		// corners = green
		final Pots green = new Pots(cornerIdx.cellsA(grid), v);
		// exoFins = blue
		final Pots blue = new Pots(exoFinsIdx.cellsA(grid), v);
		// endoFins = purple
		final Pots purple = new Pots(endoFinsIdx.cellsA(grid), v);
//		// Q: Can endoFins be set?
//		// A: No.
//		// Q: Is that only if the endoFin seesAll the exoFins?
//		// A: No, so I'll be ____ed if I know what they're on about.
//		if ( !purple.isEmpty() ) {
//			final int[] solution = grid.getSolution();
//			if ( solution != null ) {
//				purple.entrySet().forEach((e) -> {
//					final Cell pc = e.getKey();
//					if ( pc.buds.hasAll(exoFinsIdx)
//					  && solution[pc.i] != VFIRST[e.getValue()] ) {
//						throw new AssertionError("Bad endoFin: e="+e.getKey()+"->"+Values.toString(e.getValue())+" != "+solution[e.getKey().i]);
//					}
//				});
//			}
//		}
		// sharks = yellow
		final Pots yellow;
		if ( sharks.none() )
			yellow = null;
		else {
			yellow = new Pots(sharks.cellsA(grid), v);
			// paint all sharks yellow (except eliminations which stay red)!
			if ( !yellow.isEmpty() ) {
				yellow.removeFromAll(green, blue, purple);
			}
		}

		// paint all eliminations red (including sharks)!
		reds.removeFromAll(green, blue, purple, yellow);

		// paint endo-fins purple, not corners (green) or exo-fins (blue).
		purple.removeFromAll(green, blue);

		final AHint myHint = new ComplexFishHint(this, type, isSashimi, v
			, basesL, coversL, reds, green, blue, purple, yellow, "");

		if ( Validator.COMPLEX_FISHERMAN_VALIDATES ) {
			// only needed for Franken and Mutant, but just check all
			if ( !Validator.isValid(grid, myHint.reds) ) {
				myHint.isInvalid = true;
				Validator.report(tech.name(), grid, myHint.toFullString());
				// See in GUI, but they cause deadCat's in the batch.
				if ( Run.type == Run.Type.Batch ) {
					return null;
				}
			}
		}

		return myHint;
	}

	/**
	 * Returns is this a Sashimi fish?
	 *
	 * @return does any used base contain only 1 non-fin v
	 */
	private boolean isSashimi() {
		for ( int j=0; j<numBases; ++j ) {
			if ( basesUsed[bases[j]]
			  && Idx.sizeLTE(baseVsM0[j] & ~fins.a0
						   , baseVsM1[j] & ~fins.a1
						   , baseVsM2[j] & ~fins.a2, 1) ) {
				return true;
			}
		}
		return false;
	}

	private static void carp(String msg) {
		Log.teeln("ComplexFisherman: "+msg);
	}

	/**
	 * Select the bases and allCovers in a Fish of my type and {@link #degree}.
	 * Repopulates the bases array with numBases base regions; and also
	 * repopulates the allCovers array with numAllCovers potential cover
	 * regions. The actual covers array is calculated later in searchCovers
	 * because the covers are dependant on the current bases.
	 *
	 * @param baseType ROW, or COL
	 * @return are there at least degree bases and allCovers: ie should we
	 *  search this Fish.
	 */
	private boolean selectBasesAndAllCovers(final int baseType) {
		numBases = numAllCovers = 0;
		addRegions(grid.rows, baseType==ROW, seekMutant);
		addRegions(grid.cols, baseType==COL, seekMutant);
		// BOXS in Franken: bases AND covers, used in either but not both.
		// BOXS in Mutant: bases AND covers.
		if ( seekFranken || seekMutant ) {
			addRegions(grid.boxs, true, true);
		}
		return numBases>=degree && numAllCovers>=degree;
	}

	private void addRegions(final ARegion[] regions, final boolean isBase, final boolean andConverse) {
		for ( ARegion region : regions ) {
			// ignore regions which already have the Fish candidate set.
			// nb: call containsValue() coz the field no work in generate.
//			boolean[] pre = region.containsValue.clone();
			if ( !region.containsValue[v] ) {
				addRegion(region, isBase, andConverse);
			}
//			if ( !Arrays.equals(pre, region.containsValue) ) {
//				System.out.println("region: "+region);
//				System.out.println("before: "+debugContainsValue(pre));
//				System.out.println("after : "+debugContainsValue(region.containsValue));
//				Debug.breakpoint();
//			}
		}
	}

	private void addRegion(final ARegion region, final boolean isBase, final boolean andConverse) {
		// indices of cells in this region which maybe the fish candidate value
		final Idx idx = region.idxs[v];
		if ( isBase ) { // region is a base
//			// basic fish bases have upto degree candidates, but not more.
//			if ( !seekBasic || region.ridx[v].size<=degree ) {
				bases[numBases] = region.index;
				baseVsM0[numBases] = idx.a0;
				baseVsM1[numBases] = idx.a1;
				baseVsM2[numBases++] = idx.a2;
//			}
		} else { // region is a cover
			allCovers[numAllCovers] = region.index;
			allCoverVsM0[numAllCovers] = idx.a0;
			allCoverVsM1[numAllCovers] = idx.a1;
			allCoverVsM2[numAllCovers++] = idx.a2;
		}
		if ( andConverse ) {
			addRegion(region, !isBase, false);
		}
	}

}
