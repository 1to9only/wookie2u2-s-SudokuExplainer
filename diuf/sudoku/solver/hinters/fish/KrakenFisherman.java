/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * Based on HoDoKu's FishSolver, by Bernhard Hobiger. KrakenFisherman has been
 * modified extensively from hobiwans original; especially funky KrakenTables
 * caching all possible chains in a Grid. Kudos to hobiwan. Mistakes are mine.
 *
 * Here's hobiwans standard licence statement:
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
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.Ass;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Regions;
import static diuf.sudoku.Regions.BOX_MASK;
import static diuf.sudoku.Regions.COL_MASK;
import static diuf.sudoku.Regions.ROW_COL_MASK;
import static diuf.sudoku.Regions.ROW_MASK;
import diuf.sudoku.Run;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.LinkedMatrixAssSet;
import static diuf.sudoku.solver.hinters.Validator.reportRedPots;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_KRAKEN_FISHERMAN;
import static diuf.sudoku.solver.hinters.Validator.validOffs;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import diuf.sudoku.utils.IntFilter;
import diuf.sudoku.utils.IntHashSet;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;


/**
 * KrakenFisherman is my second version of KrakenFisherman. It runs in about
 * half the time of the original, to find the same hints, using KrakenTables.
 * <p>
 * KrakenTables partially do the chaining. We build a table of all possible ONs
 * and OFFs, and wire them together into chains. KrakenFisherman then searches
 * the existing chains, instead of each findHints building it's own chains from
 * scratch, so it's faster.
 * <p>
 * Please note that this code is evidently "not quite right", the details of
 * which have thus-far alluded me. Krakens are suppressed in Generate because
 * it "goes mental", so generate does not, at this time, include Krakens!
 * <p>
 * KrakenSwampfish are still A BIT SLOW. KrakenSwordfish are still SLOW.
 * But please note well that KrakenJellyfish is still WAY TOO SLOW, despite
 * (or possibly because of) my best efforts to enspeedonate it using every
 * trick in the book, and quite a few that aren't, probably for good reason,
 * so please just don't use it! A real systems engineer could possibly do it
 * a bit faster, but what's actually required is a genius to imagineer a new
 * approach. I lack the motivation, because mixing chaining with Fish, and
 * pretending it's simpler than chaining is just plain wrong, IMHO.
 * <p>
 * I suspect that the Kraken grew out of manual Sudoku solving, when somebody
 * noticed that, having looked for fish, it wasn't that much extra work to also
 * then look for chains, which then have a higher hit-rate than "random" chains
 * so overall it took less effort; but on a computer, unless you do the Kraken
 * chaining IN the Fish search, Krakens take longer, which is a now-obvious
 * down-side of my decision to separate-out the Kraken, but I still refuse to
 * put Krakens back into ComplexFisherman, but an opportunity exists to put the
 * Kraken searches back into ComplexFisherman, to do it all in one. I fear that
 * doing so is beyond me; I struggled to get this code into it's current state,
 * which is "pretty fast" compared to the original, but still there's a way to
 * do it faster available only to a seriously smart-assed smart-ass, ergo not
 * me. I've had a gutful of Krakens when I don't, reasonably, believe in them.
 * <p>
 * This code is a mess. I tried every-bloody-thing to make it faster, and the
 * result is a poorly conceived mess. KF can be done smarter, just not by me.
 *
 * <pre>
 * KRC 2021-11-01 10:54 a cache-miss creates new instances so that a cache-hit
 * can just return those instances, rather than copying cache to constants, coz
 * it spent about half it's time just copying cached data!
 * 529,131,437,000 1467 360,689,459 35 15,118,041,057 KrakenJellyfish
 * 323,188,864,200 1467 220,305,974 35  9,233,967,548 faster caching technique
 * 309,076,138,200 1467 210,685,847 35  8,830,746,805 KT2_CACHE[][] was a Map
 * </pre>
 *
 * @author Keith Corlett 2020-10-11
 */
public class KrakenFisherman extends AHinter
		implements diuf.sudoku.solver.hinters.IPrepare
{

	// ~~~~~~~~~~~~~~~~~~~~~~~~ krakenTables ---~~~~~~~~~~~~~~~~~~~~~~~~

	// NOTE: I'm static, so I'm shared among all instances.
	private static KrakenTables krakenTables;

	/**
	 * Returns THE single instance of kraken tables, which is shared among
	 * the upto three possible instances of KrakenFisherman.
	 *
	 * @return THE single instance of kraken tables
	 */
	private static KrakenTables krakenTables() {
		if ( krakenTables == null )
			krakenTables = new KrakenTables();
		return krakenTables;
	}

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
		// debug only, not used in actual code.
		@Override
		public String toString() {
			return ""+index+COLON_SP+Idx.toString(vsM0, vsM1, vsM2);
		}
	}
	/** array of all possible base units (indices in grid.regions). */
	private final int[] bases = new int[NUM_REGIONS];
	/** number of eligible bases. */
	private int numBases;
	/** is the base region at this index in the current search. */
	private final boolean[] basesUsed = new boolean[NUM_REGIONS];
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
			return ""+index+COLON_SP+Idx.toString(vsM0, vsM1, vsM2);
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

	/** the Fish candidate value, colloquially known as 'v'. */
	private int v;
	/** buds: used a result of grid.cmnBuds. */
	private final Idx buds = new Idx();
	/** deletes (potential eliminations) in the current covers search;
	 * read back by createHint. Mask fields used for search speed. */
	private int deletesM0,deletesM1,deletesM2;
	/** deletes (potential eliminations) in the current covers search;
	 * read back by createHint. The Idx used only to createHint. */
	private final Idx deletes = new Idx();
	/** fins (both exo and endo) in the current covers search;
	 * read back by createHint. */
	private final Idx fins = new Idx();
	/** sharks (cannibalistic) in the current covers search;
	 * read back by createHint. The masks used for search speed. */
	private int sharksM0,sharksM1,sharksM2;
	/** sharks (cannibalistic) in the current covers search;
	 * read back by createHint. The idx used only to createHint. */
	private final Idx sharks = new Idx();
	/** The KrakenTables are shared by all three KrakenFisherman's. */
	final KrakenTables tables;
	/** The max number of endo-fins (v's in more than one base). */
	private final int maxEndoFins;
	/** The max number of fins (extra v's in bases). */
	private final int maxFins;
	/** true when NOT a batch run (or a batch that's printing HTML)? */
	private final boolean wantDetails;

	// ~~~~~~~~~~~~~~~~~~~~~~ specify which Fish we seek ~~~~~~~~~~~~~~~~~~~~~~

	// these fields are set by findHints (values rely on parameters)
	/** The Grid to search for Fish hint/s. */
	private Grid grid;
	/** The indices which maybe each potential value in the Grid to search. */
	private Idx[] idxs;
	/** The IAccumulator to which I add hint/s. */
	private IAccumulator accu;
	/** Equals accu.isSingle() meaning the IAccumulator wants just 1 hint,
	 * so I exit-early only when wantOneHint is true. */
	private boolean oneOnly;

	/**
	 * The Constructor.
	 * @param tech Tech.KrakenSwampfish, Tech.KrakenSwordfish, or
	 *  Tech.KrakenJellyfish
	 */
	public KrakenFisherman(final Tech tech) {
		super(tech);
		KT1_VISITOR.tables = tables = krakenTables();
		// Kraken only
		assert tech.name().startsWith("Kraken");
		// 2=Swampfish, 3=Swordfish, 4=Jellyfish
		assert degree>=2 && degree<=4;
		// maximum number of endo-fins (candidates in more than one base).
		// maxEndoFins must be less than or equal to maxFins.
		this.maxEndoFins = degree==2 ? 2 : degree - 1;
		// maximum number of fins (extra candidates in bases) and endo-fins.
		// maxFins must be greater than or equal to maxEndoFins.
		this.maxFins = degree==2 ? 3 : degree;
		// initialise the two recursion-stacks
		for ( int l=0; l<baseStack.length; ++l )
			baseStack[l] = new BaseStackEntry();
		for ( int l=0; l<coverStack.length; ++l )
			coverStack[l] = new CoverStackEntry();
		// no hint-details in batch UNLESS we're printing the HTML
		this.wantDetails = Run.type!=Run.Type.Batch || Run.PRINT_HINT_HTML;
	}

	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		// initialise for each puzzle: problem with Swordfish and Jellyfish
		// not re-initialising the kt2Cache.
		tables.myHN = -1;
		tables.myPid = -1L;
	}

	/**
	 * foreach candidate 1..9 {@link #searchBases(boolean)} for Fish.
	 * Search rows first, then cols.
	 * <p>
	 * If accu.isSingle then exit-early when the first hint is found.
	 *
	 * @param grid
	 * @param accu
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// SKIP Krakens in Generator coz tables full of nulls, coz caches can't
		// handle maybes being added back in.
		if ( Run.type == Run.Type.Generator )
			return false;
		// if there's hints in the accu return, coz Kraken search takes ages
		if ( accu.size() > 0 )
			return true;
		// find new hints
		this.grid = grid;
		this.idxs = grid.idxs;
		this.accu = accu;
		this.oneOnly = accu.isSingle();
		int pre = accu.size();
		try {
			// initialise exits-early if it's already done for this grid.
			if ( tables.initialise(grid, false) ) // @check false
				for ( int i=0; i<GRID_SIZE; ++i )
					for ( int v=1; v<VALUE_CEILING; ++v )
						KT2_CACHE[i][v] = null;
			if ( oneOnly ) { // short-circuit
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// find fish in rows, else find fish in cols
					if ( searchBases(ROW) || searchBases(COL) )
						break;
				}
			} else { // no short-circuit
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// always find fish in both rows and cols
					if ( searchBases(ROW) | searchBases(COL) )
						break;
				}
			}
		} finally {
			this.grid = null;
			this.idxs = null;
			this.accu = null;
//			Cells.cleanCasA();
		}
		return accu.size() > pre;
	}

	/**
	 * search all possible combinations of {@link #degree} base regions.
	 *
	 * @param baseType ROW searches rows, or COL searches cols.
	 * @return were hint/s found
	 */
	private boolean searchBases(int baseType) {

		// presume that no hints will be found
		searchBasesResult = false;
		// find eligible bases and allCovers; the actual covers are calculated
		// in searchCovers because eligibility depends on the current bases.
		if ( !findEligibleBasesAndAllCovers(baseType==ROW) ) {
			return false;
		}

		// clear everything before we start
		Arrays.fill(basesUsed, false);
		pB = baseStack[0];
		// vs: indices of Grid.cells which maybe v (the Fish candidate value).
		pB.vsM0=pB.vsM1=pB.vsM2 = 0;
		// endo-fins: cells in more than one base regions, which may overlap
		// coz we're handling row-or-cols and boxs in Mutant fish.
		pB.efM0=pB.efM1=pB.efM2 = 0;
		cB = baseStack[1];
		cB.index = 0;
		cB.prevIndex = -1;
		bLevel = 1; // baseLevel: start at level 1 (0 is just a stopper)

		// foreach combo of base regions
		for (;;) {
			// fallback a baseLevel if no unit is available at this baseLevel
			while ( (cB=baseStack[bLevel]).index >= numBases ) {
				// fallback one level at a time to maintain basesUsed
				if ( cB.prevIndex > -1 ) {
					basesUsed[cB.prevIndex] = false;
					cB.prevIndex = -1;
				}
				if ( --bLevel < 1 ) { // ie <= 0 (level 0 is just a stopper!)
					return searchBasesResult; // we've tried all combos!
				}
			}
			// get BaseStackEntry at the previous level, contains the existing
			// candidates and endoFins (the union of ALL bases so-far).
			// NOTE that cB is already set in the above while loop!
			pB = baseStack[bLevel - 1];
			// get the next base (must exist or we would have fallen back)
			// and post-increment the cB.index, for next time
			b = cB.index++;
			// unuse the-previous-base-at-this-level
			if ( cB.prevIndex > -1 ) {
				basesUsed[cB.prevIndex] = false;
			}
			// use bases[b] at-this-level
			baseIndex = cB.prevIndex = bases[b]; // remember prev to unuse
			basesUsed[baseIndex] = true;

			// current vs = previous vs | vs in this base
			// NB: v is the Fish candidate value
			// NB: vsM* exist to not deref cB.vsM* repeatedly in searchCovers
			vsM0 = cB.vsM0 = pB.vsM0 | baseVsM0[b];
			vsM1 = cB.vsM1 = pB.vsM1 | baseVsM1[b];
			vsM2 = cB.vsM2 = pB.vsM2 | baseVsM2[b];

			// presume everything is ok. doSearch exists because the continue
			// statement is slow: working around it can improves performance by
			// a factor of 10!
			doSearch = true;
			// my endos: v's in existing bases AND in this base
			efM0 = pB.vsM0 & baseVsM0[b];
			efM1 = pB.vsM1 & baseVsM1[b];
			efM2 = pB.vsM2 & baseVsM2[b];
			if ( (efM0|efM1|efM2) == 0 ) {
				// current endos = existing endos (no new ones)
				cB.efM0=pB.efM0; cB.efM1=pB.efM1; cB.efM2=pB.efM2;
			} else {
				// add existing endos to my endos (to size them all)
				efM0|=pB.efM0; efM1|=pB.efM1; efM2|=pB.efM2;
				if ( Integer.bitCount(efM0)+Integer.bitCount(efM1)+Integer.bitCount(efM2) <= maxEndoFins ) {
					// current endos = existing + my endos
					cB.efM0=efM0; cB.efM1=efM1; cB.efM2=efM2;
				} else {
					doSearch = false; // MAX_ENDO_FINS exceeded
				}
			}
			if ( doSearch ) {
				if ( bLevel < degree ) {
					// move onto the next level
					cB = baseStack[++bLevel];
					cB.index = b + 1;
					cB.prevIndex = -1;
				} else {
					// we've got degree bases now
					assert bLevel == degree;
					// set efM* to cB.efM* to not repeatedly dereference cB
					efM0=cB.efM0; efM1=cB.efM1; efM2=cB.efM2;
					// so search the covers
					if ( searchCovers() ) {
						searchBasesResult = true;
						if ( oneOnly )
							return searchBasesResult;
					}
				}
			}
			// stop if the user has interrupted the Generator
			interrupt();
		} // next combo of base regions
	}
	// these fields are (logically) searchBases variables
	private boolean searchBasesResult;
	// the previous and current BaseStackEntry
	private BaseStackEntry pB, cB;
	private int b; // current bases-array index (and parallel arrays baseVs)
	private int baseIndex; // index in grid.regions of current base (bases[b])
	// endo-fins: fish candidate in the-existing-bases AND the-new-base
	private int efM0, efM1, efM2;
	// vs: equals current base v's (cB.vsM*). These variables exist just to not
	// need to dereference the cB struct repeatedly in the searchCovers method.
	private int vsM0, vsM1, vsM2;
	// baseLevel is our depth in the baseStack, ie number of bases in baseSet.
	private int bLevel;
	// doSearch: should searchBases call searchCovers?
	private boolean doSearch;

	/**
	 * Search each combination of covers that fits the current bases (cB).
	 * "Fits" means the cover is not already a base, and has at least one
	 * candidate in common with the current bases.
	 *
	 * @return were any hint/s found?
	 */
	private boolean searchCovers() {
		// presume that no hints will be found
		searchCoversResult = false;

		// get eligible covers here because they depend on the current bases.
		numCovers = 0;
		for ( int l=0; l<numAllCovers; ++l ) {
			// if this cover is NOT a base
			if ( !basesUsed[allCovers[l]]
			  // and this cover shares a candidate with the current bases
			  && ( (vsM0 & allCoverVsM0[l])
			     | (vsM1 & allCoverVsM1[l])
			     | (vsM2 & allCoverVsM2[l]) ) != 0
			) {
				covers[numCovers] = allCovers[l];
				coverVsM0[numCovers]   = allCoverVsM0[l];
				coverVsM1[numCovers]   = allCoverVsM1[l];
				coverVsM2[numCovers++] = allCoverVsM2[l];
			}
		}
		// need atleast degree covers to form a Fish.
		if ( numCovers < degree ) {
			return false; // this'll only happen rarely.
		}

		// partial-calculation of the maximum cover index.
		ground = numCovers - degree - 1;
		// clear coversUsed
		Arrays.fill(coversUsed, false);
		// previousCover: the stopper (0) CoverStackEntry
		pC = coverStack[0];
		// coverLevel: the current depth in the coverStack
		// coverLevel: start at level 1 (0 is just a stopper)
		// currentCover: the current CoverStackEntry
		cC = coverStack[cLevel = 1];
		cC.index = 0;
		cC.prevIndex = -1;

		// foreach each possible combination of covers
		// HAMMERED: for-loop down-to commonBuddies.
		for (;;) {
			// fallback level/s if there's no more covers in allCovers
			while ( (cC=coverStack[cLevel]).index > ground + cLevel ) {
				// unuse the previous cover
				if ( cC.prevIndex != -1 ) {
					coversUsed[cC.prevIndex] = false;
					cC.prevIndex = -1;
				}
				// fallback
				if ( --cLevel < 1 ) {
					return searchCoversResult; // all covers have been searched
				}
			}
			// unuse the previous cover
			if ( cC.prevIndex != -1 ) {
				coversUsed[cC.prevIndex] = false;
			}
			// get next cover set (must exist or we would have fallen back)
			// use covers[c] as the current cover
			// remember prevIndex to unuse this cover
			// mashed into one line for speed
			coversUsed[cC.prevIndex=covers[c=cC.index++]] = true;
			// set the previous Cover (the combination of all existing covers)
			// nb: cC (current Cover) is already set in above while loop
			pC = coverStack[cLevel - 1];
			// current v's = previous-set + v's in new cover
			cC.vsM0 = pC.vsM0 | coverVsM0[c];
			cC.vsM1 = pC.vsM1 | coverVsM1[c];
			cC.vsM2 = pC.vsM2 | coverVsM2[c];
			// sharks: if the new cover has candidates common with the existing
			// covers then those candidates become possible eliminations, which
			// I call sharks because it's shorter than cannabilistic.
			// current sharks = previous-set + sharks in new cover
			cC.skM0 = pC.skM0 | (pC.vsM0 & coverVsM0[c]);
			cC.skM1 = pC.skM1 | (pC.vsM1 & coverVsM1[c]);
			cC.skM2 = pC.skM2 | (pC.vsM2 & coverVsM2[c]);
			// if we're still collecting covers then
			if ( cLevel < degree ) {
				// move onto the next level
				// starting at the cell after the current cell at this level
				coverStack[++cLevel].index = c + 1;
			} else {
				// we have degree covers, so now we seek a Fish
				// my-fins = current endo-fins | (v's in bases and not covers)
				fM0 = efM0 | (vsM0 & ~cC.vsM0);
				fM1 = efM1 | (vsM1 & ~cC.vsM1);
				fM2 = efM2 | (vsM2 & ~cC.vsM2);
				// complex fish needs fins
				if ( (fM0|fM1|fM2) != 0
				  // but not too many of them
				  && Integer.bitCount(fM0)+Integer.bitCount(fM1)+Integer.bitCount(fM2) <= maxFins
				  // which need some common buddy/s
				  // nb: Do NOT restrict this to v (the Fish candidate value)
				  // because Kraken Type 2 eliminates all values, so we just
				  // need common buddy/s of ANY value, including set cells!
				  // nb: commonBuddies is slow, it's just my fastest so far.
				  && fins.set(fM0,fM1,fM2).commonBuddies(buds).any()
				) {
					// candidate is deletable if in covers but not bases;
					// or belongs to more than one base set (an endo-fin).
					// deletes=((covers & ~bases) or endos) seeing all fins
					deletesM0 = ((cC.vsM0 & ~vsM0) | efM0) & buds.a0;
					deletesM1 = ((cC.vsM1 & ~vsM1) | efM1) & buds.a1;
					deletesM2 = ((cC.vsM2 & ~vsM2) | efM2) & buds.a2;
					// sharks = current sharks which see all fins
					sharksM0 = cC.skM0 & buds.a0;
					sharksM1 = cC.skM1 & buds.a1;
					sharksM2 = cC.skM2 & buds.a2;
					// nb: fins are already set in the above if.
					// look for Kraken Type 1 and Kraken Type 2 in this Fish.
					if ( searchKrakens() ) {
						searchCoversResult = true;
						if ( oneOnly ) {
							return searchCoversResult;
						}
					}
				}
			}
		} // for ever
	}
	// these fields are (logically) searchCovers variables
	private CoverStackEntry pC, cC; // the previous and current CoverStackEntry
	private boolean searchCoversResult;
	private int c; // the current cover region index
	private int cLevel; // current coversStack level, ie coversSet size
	private int numCovers; // the number of covers in the covers array
	private int ground; // partial-calculation of the maximum cover index
	// fins: indices of exo-fins and endo-fins
	private int fM0, fM1, fM2;
	// get a mask of the region-types used as bases and covers
	private int baseMask, coverMask;

	// ========================== SEEK PHIL McKRAKEN ==========================

	/**
	 * Search the current bases and covers for Kraken Fish type 1 and 2.
	 * <p>
	 * The following fields are used:<ul>
	 * <li>cB: the current baseSet
	 * <li>cC: the current coverSet
	 * <li>deletes: All potential eliminations, including shark ones
	 * <li>fins: All fins (including endo fins)
	 * <li>sharks: All potential shark eliminations
	 * <li>endoFins: All endo fins (included in finsM1/finsM2)
	 * </ul>
	 *
	 * @return where any found. Note that if accu isSingle then only one hint
	 *  is found; but if accu is multiple then you'd best apply them all BEFORE
	 *  you call me again, because I'm too bloody slow, even with caching!
	 */
	private boolean searchKrakens() {
		int ci, v2; // coversIndex, value2
		boolean ok;
		// presume Phil will not find any McKrakens;
		boolean result = false;
		try {
			// Kraken Type 1: if each deletes (cover and not base) can be
			// linked to every fin with a forcing-chain then it's a KF1
			// nb: in KF1 slow continue; is acceptable coz they're rare.
			// nb: deletes is all potential eliminations, including sharks
			// WARN: 2020-11-25 top1465 does no KF1 search, deletes are ALWAYS
			// empty (coz I prefind all fish) so this code impossible to test!
			// if there's anything to be deleted then
			final boolean anyFins = fins.any();
			if ( (deletesM0|deletesM1|deletesM2)!=0 && !anyFins ) {
				final boolean anySharks = kSharks.setAny(sharksM0,sharksM1,sharksM2);
				sharks.clear();
				KT1_VISITOR.candidate = v;
				try ( final IALease lease = Idx.toArrayLease(deletesM0,deletesM1,deletesM2) ) {
					for ( int dk : lease.array ) {
						// It's a KF1 if each fin chains to cells[kd]-v.
						if ( !isKrakenTypeOne(dk, kt1Asses) ) {
							continue;
						}
						if ( anySharks ) {
							if ( kSharks.has(dk) ) {
								sharks.clear().add(dk);
							} else {
								sharks.clear();
							}
						} // else sharks just stays empty
						// Kraken Found!!!! create hint and add to accu
						deletes.clear().add(dk);
						// the causal base hint
						final ComplexFishHint cause = createBaseHint();
						assert cause != null;
						// builds eliminations (reds) and chains
						Pots reds = new Pots();
						List<Ass> chains = new LinkedList<>();
						fins.forEach((fin) -> {
							Ass a = kt1Asses[fin];
							reds.put(a.cell, VSHFT[a.value]);
							chains.add(a);
						});
						// the actual kraken hint "wraps" the causal base hint
						final KrakenFishHint kHint = new KrakenFishHint(this, reds
								, cause, KrakenFishHint.Type.ONE, chains);
						if ( VALIDATE_KRAKEN_FISHERMAN ) {
							if ( !validOffs(grid, kHint.reds) ) {
								kHint.setIsInvalid(true);
								reportRedPots("KF1", grid, kHint.toFullString());
								// see in GUI, skip in batch/testcase
								if ( Run.type != Run.Type.GUI ) {
									continue;
								}
							}
						}
						result = true;
						if ( accu.add(kHint) ) // exit-early if accu says so
							return true;
						break; // one kraken per cause
					}
				}
			}

			// Kraken Type 2: For each cover, find a chain from each candidate
			// in the current bases and this cover, and all the fins; to as yet
			// unkown elimination/s. Check only if this cover doesn't contain
			// only base candidates, because no chain is needed for a shark.
//<HAMMERED comment="No continue, no labels, no create var, no terniaries, no method calls.">
			for ( ci=0; ci<numCovers; ++ci ) { // coverIndex
				if ( coversUsed[covers[ci]] ) {
					// kDels: v's in this cover & current bases, except sharks
					kDelM0 = coverVsM0[ci] & cB.vsM0 & ~kSharks.a0;
					kDelM1 = coverVsM1[ci] & cB.vsM1 & ~kSharks.a1;
					kDelM2 = coverVsM2[ci] & cB.vsM2 & ~kSharks.a2;
					if ( coverVsM0[ci] != kDelM0
					  || coverVsM1[ci] != kDelM1
					  || coverVsM2[ci] != kDelM2 ) {
						// add the fins
						kDelM0 |= fins.a0;
						kDelM1 |= fins.a1;
						kDelM2 |= fins.a2;
						// check each candidate
						for ( v2=1; v2<VALUE_CEILING; ++v2 ) {
							// It's a KT2 if a chain exists from ALL kd+v's
							// to any common elimination/s of v2.
							// NOTE: isKrakenTypeTwo sets kFins and kt2sets
							if ( isKrakenTypeTwo(v2) ) {
//</HAMMERED>
								// FOUND a Kraken!
								// create the base hint
								deletes.clear(); // with no eliminations
								sharks.clear(); // and no sharks
								final ComplexFishHint cause = createBaseHint();
								if ( cause != null ) { // never say never
									// kDeletes array to multiply iterate
									try ( final IALease lease = Idx.toArrayLease(kDelM0,kDelM1,kDelM2) ) {
										final int[] kda = lease.array;
										// get eliminations (reds) and chains
										final Pots reds = new Pots();
										final int fv2 = v2; // for lambda. sigh.
										final List<Ass> chains = new LinkedList<>();
										Idx.forEach(kfM0,kfM1,kfM2, (kf) -> {
											Cell cell = grid.cells[kf];
											reds.put(cell, VSHFT[fv2]);
											for ( int dk : kda ) {
												chains.add(kt2sets[dk].getAss(cell, fv2));
											}
										});
										// build the actual hint, which "wraps" the base hint.
										final KrakenFishHint kraken = new KrakenFishHint(this, reds
												, cause, KrakenFishHint.Type.TWO, chains);
										ok = true;
										if ( VALIDATE_KRAKEN_FISHERMAN ) {
											// NB: generator hits invalid Kraken Swordfish+
											if ( !validOffs(grid, kraken.reds) ) {
												kraken.setIsInvalid(true);
												reportRedPots("KF2", grid, kraken.toFullString());
												// see in GUI, skip in batch/testcase
												if ( Run.type != Run.Type.GUI )
													ok = false;
											}
										}
										if ( ok ) {
											// add hint to the IAccumulator and pop if he says to
											result = true;
											if ( accu.add(kraken) ) // exit-early if accu says so
												return true;
										}
									}
								}
							}
						}
					}
				}
			}
		} finally {
			// finally clean-up in case anything threw an exception.
			// Ass's contain cells, which holds the whole Grid in memory,
			Arrays.fill(kt1Asses, null);
		}
		return result;
	}
	// kraken sharks
	private final Idx kSharks = new Idx();
	// kraken deletes
	private int kDelM0,kDelM1,kDelM2;

	// ============================ KRAKEN TYPE 1 =============================

	/**
	 * indexed by initial assumptions cell.i, but the Ass is actually the end
	 * of the chain (the common consequence) so I'm an index of the end of each
	 * beginning (the chain herein leads you back to the initial assumption).
	 */
	private final Ass[] kt1Asses = new Ass[GRID_SIZE];

	private static final class Kt1Visitor implements IntFilter {
		public KrakenTables tables; // crapon tables
		public int candidate; // the Fish candidate value
		public int kd; // krakenDelete
		public Ass[] asses; // cooincident with grid.cells
		@Override
		public boolean accept(int fin) {
			if ( (asses[fin]=kt1Search(tables.ons[fin][candidate], kd, candidate, tables)) == null ) {
				// clean-up for next time
				Arrays.fill(asses, null);
				return false;
			}
			return true;
		}
	}

	// I create ONE instance of a static visitor rather than create a new one
	// for each and every visit because it is more efficient. The tables and
	// candidate fields are set ONCE in searchForKraken, then isKrakenTypeOne
	// sets the kd and asses fields, and then we call fins.untilFalse to visit
	// (below) each fin-cell. kt1Search then searces the implication-tree (ie
	// the KrakenTables) of the given ON (tables.ons[fin][candidate]) for the
	// target "$kd-$candidate" implication.
	//
	// I am in two minds: I suspect it may be faster to use kt2Search approach
	// of finding ALL implications of each ON once, and then look up target in
	// the tree (ie cache WHOLE implication tree of each ON in an OFFs Idx, den
	// test if the Idx contains "$kd-$candidate"), so that kt1 and kt2 both use
	// a single implication cache.
	private static final Kt1Visitor KT1_VISITOR = new Kt1Visitor();

	/**
	 * <b>WARNING</b>: This code is untested because I don't have a single
	 * instance of Kraken Fish Type 1, nor am I bright enough to figure out
	 * how to find one, so the code here-in has never been bloody executed,
	 * which s__ts me, but there you have it.
	 * <p>
	 * It's a KF Type 1 if each fin chains to cells[krakenDelete]-candidate.
	 *
	 * @param kd krakenDelete: indice of target cell, ie we seek a chain from
	 *  each fin+v to the kd-v.
	 * @param asses Ass[81] coincident with grid.cells
	 * @return true if a KF1 exists, false otherwise
	 */
	protected boolean isKrakenTypeOne(int kd, final Ass[] asses) {
		KT1_VISITOR.kd = kd;
		KT1_VISITOR.asses = asses;
		return fins.doWhile(KT1_VISITOR);
	}

	/**
	 * kt1Search: Kraken Type 1 Chaining: returns the Ass that OFFs targetValue
	 * from targetIndice (ie a chain starting at the initialOn and ending with
	 * the elimination of targetValue from grid.cells[targetIndice]).
	 * <p>
	 * Stack: {@link #searchKrakens} -> {@link isKrakenTypeTwo} -> kt2Search
	 * all private in KrakenFisherman; so we can communicate via fields.
	 * <p>
	 * NOTE: when we enqueue an Ass, first we need to check this Ass never has
	 * been in the queue, for which I use {@link Eff#seen}; otherwise we end-up
	 * in an infinite-loop when an Ass causes itself. Large-L Liberal. Sigh.
	 *
	 * @param initialOn the initial assumption (which is always an ON) has
	 *  effects, which cause effects, and so on; forming a consequence tree,
	 *  which we now search for the target index and value.
	 * @param tIndice targetIndex of the Cell I seek to eliminate from
	 * @param tValue targetValue I seek the elimination of
	 * @param tables the current KrakenTables
	 * @return the target Ass, ie the Ass which eliminates targetValue from the
	 *  grid cell at targetIndice, else null meaning "not found"
	 */
	private static Ass kt1Search(final Eff initialOn, final int tIndice
			, final int tValue, final KrakenTables tables) {
		// seen null initialOn in generate, where s__t gets ____ed up. sigh.
		if ( initialOn == null )
			return null;
		assert initialOn.isOn; // the ON must be an ON
		assert initialOn.hasKids; // all ONs have effects
		Eff[] kids; // p.kids of this parent
		Eff p, k; // p for parent, k for kid
		int i, n; // index of kid, and number of kids in this parent
		// Q is a circular buffer. read and write move independently, but write
		// always outruns read, until read catches up at the end. The buffer
		// must be large enough so that write NEVER overtakes read. The only
		// way to work that out is trial by fire. QSIZE must be a power of 2.
		int r=0, w=0;
		// calc the hashCode of the target "tIndice-tValue" Assumption ONCE.
		final int targetHC = Ass.hashCode(tIndice, tValue, false);
		// mark the whole KrakenTable as unseen
		tables.clearSeens();
		// This for-loop walks down the implication-tree of the initialOn; we
		// return the first assumption whose hashCode field equals the target.
		// NOTE: circular buffer is fast, but hard to follow. Basically we just
		// poll (remove and return the head) until the queue is empty. Most of
		// the speed gain comes from doing everything inline. There is no fast
		// implementation that looks pretty. Any Class is slower. Get over it!
		for ( p=initialOn; p!=null; p=Q[r], Q[r]=null, r=(r+1)&QMASK ) {
			// mark "we've seen this Eff" in the KrakenTable
			p.seen = true;
			// its faster to read a field than invoke hashCode()
			if ( p.hashCode == targetHC )
				return p;
			// add each of my kids which has not yet been seen to the queue.
			// This "not yet been seen" test averts chasing implication-loops,
			// ie averts this implication-walker going into an infinite loop.
			if ( (n=p.numKids) > 0 ) {
				kids = p.kids;
				for ( i=0; i<n; ++i )
					if ( !(k=kids[i]).seen ) {
						Q[w] = k;
						w = (w+1) & QMASK;
					}
			}
		}
		return null;
	}

	// ============================ KRAKEN TYPE 2 =============================

	/**
	 * Array Kt2CacheEntry[indice][value] of each possible assumption to it's
	 * effects and elims.
	 * <p>
	 * Note that I'm static: shared among the 3 KrakenFisherman instances.
	 */
	private static final Kt2CacheEntry[][] KT2_CACHE = new Kt2CacheEntry[GRID_SIZE][VALUE_CEILING];

	/**
	 * Eliminations of the previous chain2 call: indexed by value 1 through 9.
	 * eliminations[v] is an Idx of the cells from which 'v' was eliminated
	 * as a consequence of the initialAss passed to chain2.
	 * <p>
	 * The isKrakenTypeTwo method creates and populates this array itself.
	 */
	private Idx[] kt2elims;

	/**
	 * A Set per cell, of all Ass's (OFFs and ONs) that are a consequences of
	 * the initial assumption.
	 */
	private final LinkedMatrixAssSet[] kt2sets = new LinkedMatrixAssSet[GRID_SIZE];

	/**
	 * It's a Kraken Fish Type 2 if setting ALL the kDeletes to targetValue
	 * causes, via a forcing-chain, any common elimination.
	 * <p>
	 * Post (when isKrakenTypeTwo returns true):<ul>
	 * <li>Idx[1..9] kt2elims field is populated with elims of each value 1..9
	 * <li>Idx kFins field contains indices cells with the common elimination;
	 *  an index of kt2sets.
	 * <li>LinkedMatrixAssSet kt2sets[indice] field is populated with all Ass
	 *  (forcing chains) that link each kDelete cell to the common elim/s.
	 * </ul>
	 * <p>
	 * NOTE: kt2Search is JUST a filter coz I can't work-out how to produce the
	 * requisite output; so it just filters then calls kt2Produce, which uses
	 * the "slow old algorithm" to produce kt2elims/kt2sets, which Batch does
	 * not need (toString only) so it doesn't bother to kt2Produce.
	 *
	 * @param targetValue The candidate for which the search is made
	 * @return true if a KF2 exists, false otherwise
	 */
	private boolean isKrakenTypeTwo(int targetValue) {
		// starting kFins is all targetValue's except kDeletes
		kfM0 = idxs[targetValue].a0 & ~kDelM0;
		kfM1 = idxs[targetValue].a1 & ~kDelM1;
		kfM2 = idxs[targetValue].a2 & ~kDelM2;
		try ( final IALease lease = Idx.toArrayLease(kDelM0,kDelM1,kDelM2) ) {
			kdArray = lease.array;
			// retain only those kFins that share OFF/s with all kd's
			for ( i=0,n=kdArray.length; i<n; ++i ) {
				// first check the cache
				if ( (entry=KT2_CACHE[kd=kdArray[i]][v]) != null ) {
					kt2sets[kd] = entry.mySet;
					kt2elims = entry.myElims;
				} else { // miss, so create and cache it
					kt2sets[kd] = new LinkedMatrixAssSet();
					kt2elims = new Idx[VALUE_CEILING];
					for ( v1=1; v1<VALUE_CEILING; ++v1 )
						kt2elims[v1] = new Idx();
					kt2Search(tables.ons[kd][v], kt2sets[kd], kt2elims);
					KT2_CACHE[kd][v] = new Kt2CacheEntry(kt2sets[kd], kt2elims);
				}
				// krakenFins &= kt2elims[targetValue] from setting kd+candidate
				// if kFins is now empty then
				if ( ( (kfM0 &= kt2elims[targetValue].a0)
					 | (kfM1 &= kt2elims[targetValue].a1)
					 | (kfM2 &= kt2elims[targetValue].a2) ) == 0 ) {
					kt2sets[kd] = null;
					kt2elims = null;
					return false;
				}
			}
			// FOUND Kraken Type 2
			if ( wantDetails )
				// run kt2Produce (original "kt2Search") to get hint details.
				for ( i=0,n=kdArray.length; i<n; ++i ) {
					kd = kdArray[i];
					kt2Produce(new Ass(grid.cells[kd], v, ON), kt2sets[kd], kt2elims);
				}
		}
		return true;
	}
	// the kraken delete indice for isKrakenTypeTwo
	private int kd;
	// kraken fins for isKrakenTypeTwo
	private int kfM0,kfM1,kfM2;
	// store kDeletes in an array coz we iterate it twice upon failure,
	// which of course means 99+% of the time.
	private int[] kdArray;
	private int i,n, j;
	// kt2Search vars
	private int v1;

	private static final class Kt2CacheEntry {
		public final LinkedMatrixAssSet mySet; // effects
		public final Idx[] myElims; // elims
		private Kt2CacheEntry(LinkedMatrixAssSet theSet, Idx[] theElims) {
			mySet = theSet;
			myElims = theElims;
		}
	}
	private Kt2CacheEntry entry;

	/**
	 * Kraken Type 2 Search: populates kt2Set and elims with the consequences
	 * of the XY forcing-chain (both OFFs and ONs) consequences of iOn.
	 * <p>
	 * elims is the indice of each cell that's OFF'ed, by value, so we can:
	 * {@code fins &= kt2elims[targetValue]} to see if any fins remain.
	 * <p>
	 * ktSet is populated with all ON and OFF consequences of iOn
	 *
	 * @param iOn the initial ON Eff, from the kraken tables.
	 * @param kt2Set output implications of setting iOn.cell to iOn.value; and
	 *  kt2set is also used internally to suppress queue double-ups
	 * @param elims output indices from which each 'v' is eliminated by iOn
	 */
	private void kt2Search(final Eff iOn, final LinkedMatrixAssSet kt2Set
			, final Idx[] elims) {
		// reset the queue
		r = w = 0;
		// generate validations: seen null initialOn in analyseDifficulty
		if ( iOn==null || !iOn.isOn || !iOn.hasKids )
			return;
		kt2Set.add(iOn);
		// foreach parent in the queue, starting with iOn.
		for ( Eff p=iOn; p!=null; p=Q[r],Q[r]=null,r=(r+1)&QMASK ) { // parent
			if ( p.isOn ) {
				// p is an ON, so its kids are OFFs (about a third have kids)
				for ( kids=p.kids,ii=0,nn=p.numKids; ii<nn; ++ii )
					if ( kt2Set.add(k=kids[ii]) ) {
						elims[k.value].add(k.i);
						if ( k.hasKids ) {
							Q[w] = k;
							w = (w+1) & QMASK;
						}
					}
			} else // p is an OFF, so its kids are ONs (which ALL have kids)
				for ( kids=p.kids,ii=0,nn=p.numKids; ii<nn; ++ii )
					if ( kt2Set.add(k=kids[ii]) ) {
						Q[w] = k;
						w = (w+1) & QMASK;
					}
		}
	}
	// Q is the circular buffer that is used in both kt1Search and kt2Search.
	// NOTE: QSIZE needs to be large enough for write to NEVER overtake read.
	// 16 doesn't work for top1465; 32 works so 32 it is; 64 feels safe to me.
	// I am consciously choosing to live dangerously, in the persute of speed.
	// NOTE: QSIZE must be a power of 2 in order for the & QMASK trick to work!
	private static final int QSIZE = 32;
	// the mask used to zero the read and write indexes when past end-of-queue.
	// NOTE: QSIZE must be a power of 2 in order for the & QMASK trick to work!
	private static final int QMASK = QSIZE - 1;
	// The queue array is a circular buffer: read and write indexes move
	// independently. Write allways outruns read, but never overtakes it.
	// Think of them as doing laps of a race track (the array) which is large
	// enough to achieve the "write never overtakes read" rule.
	// NOTE: When read overtakes write the queue is empty (end of loop).
	// NOTE: Messy implementation is forced by my uber-performance requirement.
	// original fast Queue : 33,906 ms (69.7%)
	// for top1465.F10.mt  : 25,887 ms (64.3%)
	// with split on p.isOn: 20,296 ms (51.4%)
	// NOTE: QSIZE must be a power of 2 in order for the & QMASK trick to work!
	// If you tell ____wits s__t three times they probably actually hear you.
	private static final Eff[] Q = new Eff[QSIZE];
	// an array coz even an ArrayList is too slow here.
	private Eff[] kids;
	// kid
	private Eff k;
	// indexes and number thereof
	private int ii,nn;
	// the QUEUE read index and write index
	private int r, w;

	private void clear(Idx[] elims) {
		for ( int vv=1; vv<VALUE_CEILING; ++vv )
			elims[vv].clear();
	}

	// ----------------- KT2PRODUCE -----------------

	/** Just to make Ass constructor more self-documenting. */
	private static final boolean ON = true;
	/** Just to make Ass constructor more self-documenting. */
	private static final boolean OFF = false;

	/**
	 * kt2Produce: redo the whole kt2Search search, but slower because we roll
	 * our own Ass's that are used in the hint, so kt2Produce runs ONLY when
	 * kt2Search has already found eliminations. Note that kt2Produce takes
	 * about three times as long as kt2Search, which matters not if it's ONLY
	 * run to produce a hint. I belabour this point because kt2Produce IS the
	 * original "kt2Search", so it will still (I believe) serve that purpose.
	 * it just does so more slowly than kt2Search, it's newer fastardisation.
	 *
	 * @param initialOn the initial ON Ass(umption) (which is always an ON)
	 * @param set the LinkedMatrixAssSet to populate with new Ass's which have
	 *  only the parents they acquire during this search (not all possible
	 *  parents like the Eff's in the KrakenTable, leading to infinite loops
	 *  when the KrakenFishHint reads them to produce chains and links).
	 * @param elims to populate with the indice of eliminations per value.
	 */
	private void kt2Produce(Ass initialOn, final LinkedMatrixAssSet set
			, final Idx[] elims) {
		assert initialOn.isOn;
		Ass e; // the effect of Ass a (the next Ass in the consequences chain)
		Cell c; // a.cell
		int[] vs; // valueses array
		int x // a.value; use x coz v is the Fish candidate value field
		  , sx // shiftedX;
		  , i // index
		  , n; // number-of
		final Cell[] cells = grid.cells;
		kt2Queue.clear(); // should already be empty, but clear it anyway.
		set.clear(); // likewise should already be empty (I think).
		set.add(initialOn); // prevent initialOn being re-added (stop looping)
		clear(elims);

		// foreach Ass in the queue, starting with the initialOn, whose direct
		// implications are added to the queue.
		for ( Ass a=initialOn; a!=null; a=kt2Queue.poll() ) {
			// this loop is the only place 'a' changes so we can cache it's
			// attributes just to save repeatedly dereferencing them
			x = a.value;
			c = a.cell;
			if ( a.isOn ) {
				sx = VSHFT[x];
				// 1. add an OFF for each other potential value of a.cell.
				for ( vs=VALUESES[c.maybes & ~sx],i=0,n=vs.length; i<n; ++i ) {
					if ( set.add(e = new Ass(c, vs[i], OFF, a)) ) {
						kt2Queue.add(e);
						elims[vs[i]].add(c.i);
					}
				}
				// 2. add an OFF for each other possible position of a.value
				//    in each of a.cell's three regions.
				for ( Cell sib : c.siblings ) {
					if ( (sib.maybes & sx) != 0
					  && set.add(e=new Ass(sib, x, OFF, a)) ) {
						kt2Queue.add(e);
						elims[x].add(sib.i);
					}
				}
			} else {
				// 1. if a.cell has only two potential values then it must
				//    be the other potential value, so add an ON to the queue.
				if ( c.size == 2
				  && set.add(e=new Ass(c, VFIRST[c.maybes & ~VSHFT[x]], ON, a)) )
					kt2Queue.add(e);
				// 2. foreach of a.cell's 3 regions: if region has 2 places for
				//    a.value then the other cell must be a.value, so add an ON
				//    to the queue.
				for ( i=0; i<3; ++i ) {
					// nb: hit rate too low to cache cell.regions[j], faster to
					// dereference it again in the 10% of cases where there's 2
					// positions for a.value in the region
					if ( c.regions[i].ridx[x].size == 2
					  && set.add(e=new Ass(cells[c.regions[i].idxs[x].otherThan(c.i)], x, ON, a)) )
						kt2Queue.add(e);
				}
			}
		}
	}
	/** kt2Produce's to-do list. */
	private final Deque<Ass> kt2Queue = new LinkedList<>();

	// ============================= OTHER STUFF ==============================

	/**
	 * Create a new ComplexFishHint as the base for a KrakenFishHint and return
	 * it, or null meaning none.
	 * <p>
	 * In Kraken's this hint is the "causal" or "base" hint only; the actual
	 * hint added to accu is KrakenFishHint, which wraps this causal hint to
	 * provide the eliminations, change the name, and the HTML.
	 * <p>
	 * I read these fields, which must be pre-set:<ul>
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
	 */
	private ComplexFishHint createBaseHint() {
		// add the deletes (if any) and sharks (if any) to reds
		final Pots reds = new Pots();
		if ( deletes.any() )
			reds.upsertAll(deletes, grid, v);
		if ( sharks.any() )
			reds.upsertAll(sharks, grid, v);
		baseMask = Regions.types(basesUsed);
		coverMask = Regions.types(coversUsed);
		// basic fish contains row->col or col->row (no boxs)
		final boolean basicFish = (baseMask==ROW_MASK && coverMask==COL_MASK)
							   || (baseMask==COL_MASK && coverMask==ROW_MASK);
		// determine the type
		final ComplexFishHint.Type type;
		// Basic: row->col or col->row (ie no boxes), may have fins
		if ( basicFish ) {
			type = fins.any() ? ComplexFishHint.Type.FINNED[degree - 2]
				 : ComplexFishHint.Type.BASIC[degree - 2];
		// Mutant: boxes in bases AND boxes in covers
		//      OR rows and cols in bases
		//      OR rows and cols in covers
		} else if ( ((baseMask&BOX_MASK)!=0 && (coverMask&BOX_MASK)!=0)
				 || (baseMask&ROW_COL_MASK) == ROW_COL_MASK
				 || (coverMask&ROW_COL_MASK) == ROW_COL_MASK ) {
			type = fins.any() ? ComplexFishHint.Type.FINNED_MUTANT[degree - 2]
				 : ComplexFishHint.Type.MUTANT[degree - 2];
		// Franken: boxes in bases OR covers
		} else {
			type = fins.any() ? ComplexFishHint.Type.FINNED_FRANKEN[degree - 2]
				 : ComplexFishHint.Type.FRANKEN[degree - 2];
		}
		// get the regions from the *Used boolean arrays
		final ARegion[] usedBases = Regions.used(basesUsed, grid);
		final ARegion[] usedCovers = Regions.used(coversUsed, grid);
		// a corner is a candidate in a base and a cover (ie not fins).
		final Idx cornerIdx = new Idx(cB.vsM0 & ~fins.a0
									, cB.vsM1 & ~fins.a1
									, cB.vsM2 & ~fins.a2);
		final Idx endoFinsIdx = new Idx(cB.efM0, cB.efM1, cB.efM2);
		final Idx exoFinsIdx = new Idx(fins.a0 & ~cB.efM0
									 , fins.a1 & ~cB.efM1
									 , fins.a2 & ~cB.efM2);
		// corners = green
		final Pots green = new Pots(cornerIdx, grid, v);
		// exoFins = blue
		final Pots blue = exoFinsIdx.any() ? new Pots(exoFinsIdx, grid, v) : null;
		// endoFins = purple
		final Pots purple = endoFinsIdx.any() ? new Pots(endoFinsIdx, grid, v) : null;
		// sharks = yellow
		final Pots yellow;
		if ( sharks.any() ) {
			yellow = new Pots(sharks, grid, v);
			// paint all sharks yellow (except eliminations which stay red)!
			if ( !yellow.isEmpty() )
				yellow.removeFromAll(green, blue, purple);
		} else {
			yellow = null;
		}
		// paint all eliminations red (including sharks)!
		reds.removeFromAll(green, blue, purple, yellow);
		// paint the endo-fins purple, not corners (green) or exo-fins (blue).
		if ( purple != null )
			purple.removeFromAll(green, blue);
		// create and return the hint
		return new ComplexFishHint(this, type, false, v, usedBases, usedCovers
				, reds, green, blue, purple, yellow);
	}

	/**
	 * find the bases and allCovers for a Fish of my type and {@link #degree}.
	 * The actual covers are calculated in searchCovers because eligibility
	 * depends on which bases are currently used.
	 * <p>
	 * If this Fisherman is NOT {@link #withFins} then bases cannot have more
	 * than {@link #degree} candidates).
	 *
	 * @param isRows true for Rows as bases, false for Cols as bases
	 * @return are there atleast degree bases and allCovers; ergo good to go
	 */
	private boolean findEligibleBasesAndAllCovers(final boolean isRows) {
		numBases = numAllCovers = 0;
		// ROWS and COLS
		if ( isRows ) {
			addBases(grid.rows);
			addCovers(grid.cols);
		} else {
			addBases(grid.cols);
			addCovers(grid.rows);
		}
		// BOXS
		addBases(grid.boxs);
		addCovers(grid.boxs);
		return numBases>=degree && numAllCovers>=degree;
	}

	private void addBases(ARegion[] regions) {
		Idx rvs; // region v's
		for ( ARegion region : regions )
			if ( !region.containsValue[v] ) {
				bases[numBases] = region.index;
				rvs = region.idxs[v];
				baseVsM0[numBases] = rvs.a0;
				baseVsM1[numBases] = rvs.a1;
				baseVsM2[numBases++] = rvs.a2;
			}
	}

	private void addCovers(ARegion[] regions) {
		Idx rvs; // region v's
		for ( ARegion region : regions )
			if ( !region.containsValue[v] ) {
				allCovers[numAllCovers] = region.index;
				rvs = region.idxs[v];
				allCoverVsM0[numAllCovers] = rvs.a0;
				allCoverVsM1[numAllCovers] = rvs.a1;
				allCoverVsM2[numAllCovers++] = rvs.a2;
			}
	}

	/**
	 * Eff(ect) extends Ass(umption) to provide an effects list (called kids)
	 * to turn an Ass into a doubly-linked-net, with both cause (Ass.parents)
	 * and effects (Eff.kids). Gopher Cleetus?
	 * <p>
	 * A kid is a direct effect of this assumption. A parent is a direct cause
	 * of this assumption. Together both sets form a net, navigable in either
	 * direction: from cause to effect (and thus the whole consequence tree of
	 * any assumption), or from any effect all the way back to its root cause.
	 *
	 * @author Keith Corlet 2020-10-10
	 */
	private static final class Eff extends Ass {

		// kidsHCs: kids (effects) Hash Codes.
		// NB: IntHashSet does not grow like a java.util.Set.
		public final IntHashSet kidsHCs = new IntHashSet(32);
		// You can't have more than 22 kids from 20 siblings. Do the math.
		public final Eff[] kids = new Eff[22];
		// The number of elements in the kids array
		public int numKids;

		/** does this Eff have any kids? */
		public boolean hasKids;

		/** has this Eff been processed already. */
		public boolean seen;

		/**
		 * The Constructor.
		 * @param cell
		 * @param value
		 * @param isOn
		 * @param parent nullable, the Ass is created with a parents-list anyway.
		 */
		public Eff(Cell cell, int value, boolean isOn, Eff parent) {
			super(cell, value, isOn, parent);
		}

		/**
		 * Add this kid to my kids (if its NOT already one of my kids),
		 * and (if so) add me to his parents.
		 * <p>
		 * this.kids is a MyFunkyLinkedHashSet whose add method differs from
		 * java.util.HashMap in its handling of pre-existing elements:<ul>
		 * <li>FunkySet does add-or-(do-nothing-and-return-false); where</li>
		 * <li>java.util.HashSet does add-or-(update-and-return-false).</li>
		 * </ul>
		 */
		void addKid(Eff kid) {
			// add the kid unless its already in the list.
			// this relies on Ass.hashCode uniquely identifying each Eff/Ass,
			// which is the case, but is unusual: most HCs are "lossy".
			// We also rely on hashCode being a public final field, rather
			// than incurr the cost of invoking the hashCode() method.
			// This is executed BILLIONS of times so performance is critical;
			// thats why I split kids into kidsHCs, for distinctness; kids is
			// now an array for fast iteration (no list-iterators).
			if ( kidsHCs.add(kid.hashCode) ) {
				hasKids = true;
				kids[numKids++] = kid;
				kid.parents.linkLast(this); // let it NPE if child is null
			}
		}

	}

	/**
	 * KrakenTables holds the ON and OFF tables for all three KrakenFisherman1.
	 *
	 * @author Keith Corlett 2020-10-11
	 */
	private static final class KrakenTables {

		/** Just to make Eff/Ass constructor more self-documenting. */
		private static final boolean ON = true;
		/** Just to make Eff/Ass constructor more self-documenting. */
		private static final boolean OFF = false;

		/** The AHint.hintNumber for which tables are initialised. */
		private int myHN;
		/** The grid.puzzleID for which tables are initialised. */
		private long myPid;

		/**
		 * The offTable is all the OFF Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] offs = new Eff[GRID_SIZE][VALUE_CEILING];

		/**
		 * The onTable is all the ON Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] ons = new Eff[GRID_SIZE][VALUE_CEILING];

		/**
		 * The Constructor.
		 */
		private KrakenTables() {
		}

		/**
		 * Initialise the tables: partial chaining. The idea is to build all
		 * possible chains ONCE, making both repeated Kraken searches faster.
		 * <p>
		 * So here we build all possible ON and OFF effects, and then look-up
		 * the effects of each ON and OFF Ass(umption) in the grid. Then each
		 * kt?Search follows the existing kids links, instead of building its
		 * own chain from scratch.
		 * <p>
		 * When reading the result the tricky part is stopping, so we "hide"
		 * initialOn.parents (else it endlessly-loops around the net) and
		 * restore them afterwards.
		 * <p>
		 * Tables need to be initialised every time the grid changes, which
		 * sucks because the search is slow and the resulting dependency-net
		 * is lots like one we produced last time, but I'll be ____ed if I
		 * can work out HOW to modify a dependency-net, so we just rebuild
		 * the whole net every time.
		 * <p>
		 * Pseudocode for XY-chaining:
		 * <pre>
		 * if a.isOn // premise is an ON
		 *     1. add an OFF for each other potential value of a.cell
		 *     2. add an OFF for each other possible position of a.value
		 *        in each of a.cell's three regions
		 * else // premise is an OFF
		 *     1. if a.cell has only two potential values then the cell must
		 *        be the other potential value, so enque an ON
		 *     2. if any of a.cell's 3 regions has two positions for a.value
		 *        then that other cell must be a.value, so enque an ON
		 * endif
		 * </pre>
		 *
		 * @param grid the current grid we're searching for Kraken Fish
		 * @param forceRefresh DEBUG ONLY always initialise
		 * @return did we actually initialise?
		 */
		boolean initialise(Grid grid, boolean forceRefresh) {
			// the first initialise call foreach version of each grid init's,
			// the rest just return. "I told him we've already got one."
			if ( myHN==grid.hintNumber && myPid==grid.pid && !forceRefresh )
				return false;

			Eff[] myOns, myOffs;
			Eff on, off, kid;
			int i, maybes, sv, o;

			// 1. construct all possible "moves" (skip set cells).
			// a "move" is a direct effect.
			for ( Cell cell : grid.cells )
				if ( cell.maybes != 0 ) {
					myOns = ons[cell.i];
					myOffs = offs[cell.i];
					for ( int v : VALUESES[cell.maybes] ) {
						myOns[v] = new Eff(cell, v, ON, null);
						myOffs[v] = new Eff(cell, v, OFF, null);
					}
				}

			// 2. wire-up the "moves"
			// ie find the direct effects of each assumption, which then knows
			// both it's causes and its effects, forming a net.
			for ( Cell cell : grid.cells ) {
				if ( cell.value != 0 )
					continue; // skip set cell
				myOns = ons[i = cell.i];
				myOffs = offs[i];
				maybes = cell.maybes;
				for ( int v : VALUESES[maybes] ) {
					on = myOns[v];
					off = myOffs[v];
					sv = VSHFT[v];

					// Assuming that cell is set to value
					// 1. add an OFF for each other potential value of this cell
					for ( int v2 : VALUESES[maybes & ~sv] )
						on.addKid(myOffs[v2]);
					// 2. add an OFF for each of my siblings which maybe v
					for ( Cell sib : cell.siblings )
						if ( (sib.maybes & sv) != 0 )
							on.addKid(offs[sib.i][v]);

					// Assuming that value is eliminated from cell
					// 1. if cell has two maybes, add an ON to the other maybe
					if ( cell.size == 2 )
						off.addKid(myOns[VFIRST[maybes & ~sv]]);
					// 2. if any of cells regions has two v's, other posi is ON
					for ( ARegion r : cell.regions )
						if ( r.ridx[v].size == 2
						  // -1 actually happened, and I don't understand how.
						  // I guess r.ridx[v] or otherThan must be dodgy?
						  && (o=r.idxs[v].otherThan(i)) > -1
						  // null kid actually happened (and not for -1)
						  // so r.ridx[v] must be dodgy!
						  && (kid=ons[o][v]) != null )
							off.addKid(kid);
				}
			}
			myHN = grid.hintNumber;
			myPid = grid.pid;
			return true;
		}

		/**
		 * Set the seen flag of each onTable and offTable entry to false.
		 */
		void clearSeens() {
			for ( int i=0; i<GRID_SIZE; ++i )
				for ( int v=1; v<VALUE_CEILING; ++v ) {
					if ( ons[i][v] != null )
						ons[i][v].seen = false;
					if ( offs[i][v] != null )
						offs[i][v].seen = false;
				}
		}

	}

}
