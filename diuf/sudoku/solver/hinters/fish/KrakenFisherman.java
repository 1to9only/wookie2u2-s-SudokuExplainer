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
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.Ass;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.LinkedMatrixAssSet;
import diuf.sudoku.utils.IntHashSet;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * KrakenFisherman is the second version of KrakenFisherman. It runs in about
 * half the time of the original, to find the same hints, using KrakenTables.
 * <p>
 * The KrakenTables partially does chaining. It builds a table of all possible
 * ONs and OFFs and then wires them together into chains. KrakenFisherman then
 * searches the existing chains, instead of each findHints call building it's
 * own chains from scratch, so it's faster.
 *
 * @author Keith Corlett 2020-10-11
 */
public class KrakenFisherman extends AHinter
//		implements diuf.sudoku.solver.IReporter
		implements diuf.sudoku.solver.IPreparer
{

	/** switch on/off logging in findHints. */
	public static boolean FIND_HINTS_IS_NOISY = false;

	// Fish Settings

	// maximum number of fins (extra candidates in bases)
	private static final int MAX_FINS = 5; // kraken was 2; complex is 5

	// maximum number of endo-fins (candidates in two overlapping bases)
	private static final int MAX_ENDO_FINS = 0; // buggers up Krakens

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

	// the circular buffer used in both kt1Search and kt2Search.
	private static final int QSIZE = 32;
	private static final int QMASK = QSIZE - 1;
	private static final Eff[] queue = new Eff[QSIZE]; // Pronounce Properly!

	// ~~~~~~~~~~~~~~~~~~~~~~~~ krakenTables ---~~~~~~~~~~~~~~~~~~~~~~~~

	/** Returns THE single instance of kraken tables, which is shared among
	 * the three instances of KrakenFisherman.
	 * @return THE single instance of kraken tables */
	private static KrakenTables krakenTables() {
		if ( krakenTables == null )
			krakenTables = new KrakenTables();
		return krakenTables;
	}
	private static KrakenTables krakenTables; // NOTE static, so I persist between instances.

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
	private Idx candidates;
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
	/** maximum number of fins (extra cells which maybe v in a base). */
	private final int maxFins; // = Math.min(degree + 1, MAX_FINS)
	/** The KrakenTables are shared by all three KrakenFisherman's. */
	final KrakenTables tables;

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
	private boolean wantOneHint;

	/**
	 * The Constructor.
	 * @param tech Tech.KrakenSwampfish, Tech.KrakenSwordfish, or
	 *  Tech.KrakenJellyfish
	 */
	public KrakenFisherman(Tech tech) {
		super(tech);
		KT1_VISITOR.tables = this.tables = krakenTables();
		// Kraken only
		assert tech.nom.startsWith("Kraken");
		// 2=Swampfish, 3=Swordfish, 4=Jellyfish
		assert degree>=2 && degree<=4;
		maxFins = Math.min(degree + 2, MAX_FINS); // currently 5
		// initialise the two recursion-stacks
		for ( int i=0; i<baseStack.length; ++i )
			baseStack[i] = new BaseStackEntry();
		for ( int i=0; i<coverStack.length; ++i )
			coverStack[i] = new CoverStackEntry();
	}

	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		// I am just trying to ensure that it initialises the table for each
		// puzzle; apparently there's a problem with Franken Swordfish and
		// Franken Jellyfish not re-initialising the kt2Cache.
		tables.myHintNumber = -1;
		tables.myPuzzleID = -1L;
	}

	/**
	 * foreach candidate 1..9 {@link #searchBases(boolean)} for Fish.
	 * Search rows first, then cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// if there's hints in the accu return, coz Kraken search takes ages
		if ( accu.size() > 0 )
			return true;
		// find new hints
		final long start = System.nanoTime();
		this.grid = grid;
		this.idxs = grid.getIdxs();
		this.accu = accu;
		this.wantOneHint = accu.isSingle();
		int pre = accu.size();

		try {
			// initialise exits-early if it's already done for this grid.
			// nb: change it to true to force initialise when debugging
			// ALWAYS re-initialise and clear the cache for Kraken Swordfish(3)
			// and Jellyfish(4) else we produce invalid hints, I know not why
			// exactly (@stretch). This slower but works.
			if ( tables.initialise(grid, false) ) // @check false
				kt2Cache.clear();
			if ( wantOneHint ) { // short-circuit
				// v is a field! v is short for the Fish candidate value
				for ( candidate=1; candidate<10; ++candidate ) {
					candidates = idxs[candidate];
					// find fish in rows, else find fish in cols
					if ( searchBases(ROW) || searchBases(COL) )
						break;
				}
			} else { // no short-circuit
				// v is a field! v is short for the Fish candidate value
				for ( candidate=1; candidate<10; ++candidate ) {
					candidates = idxs[candidate];
					// always find fish in rows and cols
					if ( searchBases(ROW) | searchBases(COL) )
						break;
				}
			}
			if ( FIND_HINTS_IS_NOISY )
				Log.format(tech.nom+" in %,d ns\n", System.nanoTime()-start);
		} finally {
			this.grid = null;
			this.idxs = null;
			this.accu = null;
		}
		return accu.size() > pre;
	}

	/**
	 * search all possible combinations of {@link #degree} base regions.
	 * @param baseType ROW searches rows, or COL searches cols.
	 * @return were hint/s found
	 */
	private boolean searchBases(int baseType) {

		// presume that no hints will be found
		searchBasesResult = false;
		// find eligible bases and allCovers; the actual covers are calculated
		// in searchCovers because eligibility depends on the current baseSet.
		if ( !findEligibleBasesAndAllCovers(baseType==ROW) )
			return false;

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
			if ( (baseIndex=cB.prevIndex) != -1 )
				basesUsed[baseIndex] = false;
			// get the previous levels BaseStackEntry, containing the existing
			// candidates and endoFins sets (the union of ALL bases so-far).
			pB = baseStack[baseLevel - 1];
			// get the next base (must exist or we would have fallen back)
			b = cB.index++;
			// use bases[b] at-this-level
			baseIndex = cB.prevIndex = bases[b]; // remember prev to unuse
			basesUsed[baseIndex] = true;

			// current vs = previous vs + vs in this base (v is candidate value)
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
			} else {
				// add existing endos to my endos (to size them all)
				efM0|=pB.efM0; efM1|=pB.efM1; efM2|=pB.efM2;
				if ( Idx.sizeLTE(efM0,efM1,efM2, MAX_ENDO_FINS) ) {
					// current endos = existing + my endos
					cB.efM0=efM0; cB.efM1=efM1; cB.efM2=efM2;
				} else {
					continue; // MAX_ENDO_FINS exceeded
				}
			}
			// if any current-endos check not-too-many && any buds-in-common
			// (pays-off because one whole searchCovers may be skipped)
			if ( (cB.efM0|cB.efM1|cB.efM2) == 0 ) {
				doSearch = true; // there's no endos to check, so it passed.
			} else if ( Idx.sizeLTE(cB.efM0,cB.efM1,cB.efM2, maxFins) ) {
				// buds = cells which see all the endo-fins
				Grid.cmnBuds(endoFins.set(cB.efM0,cB.efM1,cB.efM2), buds);
				// doSearch = does any candidate see all the endo-fins?
				doSearch = ( (buds.a0 & candidates.a0)
						   | (buds.a1 & candidates.a1)
						   | (buds.a2 & candidates.a2) ) != 0;
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
		} // for ever
	}
	// these fields are (logically) searchBases variables
	private boolean searchBasesResult;
	// the previous and current BaseStackEntry
	private BaseStackEntry pB, cB;
	private int b; // current bases-array index (and parallel arrays baseVs)
	private int baseIndex; // index in grid.regions of current base (bases[b])
	// endo-fins: fish candidate in the-existing-bases AND the-new-base
	private int efM0, efM1, efM2;
	// baseLevel is our depth in the baseStack, ie number of bases in baseSet.
	private int baseLevel;
	// doSearch: should searchBases call searchCovers?
	private boolean doSearch;

//// 4#top1465.d5.mt
////WARN: KF2 invalidity D9-5 in #Kraken type 2: Finned Franken Swordfish: row 1, row 3, row 6 and box 3, col A, col C on 5 (D9-5)
//private static final boolean[] DEBUG_BASES = Regions.parseUsed("row 1, row 3, row 6");
//private static final boolean[] DEBUG_COVERS = Regions.parseUsed("box 3, col A, col C");

	/**
	 * Search each combo of eligible covers for the current baseSet (bC).
	 * "Eligible" means the cover is not already a base, and has atleast one
	 * candidate in common with the current baseSet.
	 *
	 * @param cB the current BaseStackEntry contains the candidates (vs's)
	 *  and endo-fins (ef's) of ALL our $degree bases (the current baseSet).
	 * @return were any hint/s found?
	 */
	private boolean searchCovers(BaseStackEntry cB) {

		// presume that no hints will be found
		searchCoversResult = false;

		// get eligible covers from allCovers; done here because eligibility
		// depends on which bases are used.
		numCovers = 0;
		for ( int i=0; i<numAllCovers; ++i ) {
			// skip if this cover is a base already
			if ( basesUsed[allCovers[i]] )
				continue;
			// skip if this cover has no candidates in common with the baseSet
			if ( (cB.vsM0 & allCoverVsM0[i]) == 0
			  && (cB.vsM1 & allCoverVsM1[i]) == 0
			  && (cB.vsM2 & allCoverVsM2[i]) == 0 )
				continue;
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
		// coverLevel: the current depth in the coverStack
		coverLevel = 1; // start at level 1 (0 is just a stopper)
		// initialise previous CoverStackEntry
		pC = coverStack[0];
		pC.vsM0=pC.vsM1=pC.vsM2 = pC.skM0=pC.skM1=pC.skM2 = 0; // empty the prevCover candidates and sharks
		// initialise current CoverStackEntry
		cC = coverStack[1];
		cC.index = 0;
		cC.prevIndex = -1;
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
			// cP "cover Previous" is the previous coverStack entry
			pC = coverStack[coverLevel - 1];
			// unuse the previous cover
			// hijack coverIndex coz we don't really need another variable
			if ( (coverIndex=cC.prevIndex) != -1 )
				coversUsed[coverIndex] = false;
			// get next cover set (must exist or we would have fallen back)
			c = cC.index++;
			// use covers[c] as the current cover
			coverIndex = cC.prevIndex = covers[c]; // remember prev to unuse
			coversUsed[coverIndex] = true;

//if ( "row 4, row 5 and col G, col H".equals(Frmt.basesAndCovers(basesUsed, coversUsed)) )
//	Debug.breakpoint();

			// sharks: if the new cover has candidates common with the existing
			// covers then those candidates become possible eliminations, which
			// I call sharks because it's shorter than cannabilistic.
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
				// fins = current endo-fins + candidates in bases except covers
				finsM0 = cB.efM0 | (cB.vsM0 & ~cC.vsM0);
				finsM1 = cB.efM1 | (cB.vsM1 & ~cC.vsM1);
				finsM2 = cB.efM2 | (cB.vsM2 & ~cC.vsM2);
				// complex fish needs fins, but not too many of them
				if ( (finsM0|finsM1|finsM2) != 0
				  && Idx.sizeLTE(finsM0,finsM1,finsM2, maxFins)
				  // finBuds: cells which see (same box/row/col) all fins
				  // if any finBuds then we try to eliminate
				  && Grid.cmnBuds(fins.set(finsM0,finsM1,finsM2), buds).any()
				) {
					// nb: fins are already set in the above if statement

					// candidate is deletable if in covers andNot bases;
					// or belongs to more than one cover set (endo-fins).
					// deletes=((covers & ~bases) or endos) seeing all fins
					deletes.set(((cC.vsM0 & ~cB.vsM0) | cB.efM0) & buds.a0
							  , ((cC.vsM1 & ~cB.vsM1) | cB.efM1) & buds.a1
							  , ((cC.vsM2 & ~cB.vsM2) | cB.efM2) & buds.a2);
					// sharks = current sharks which see all fins
					sharks.set(cC.skM0 & buds.a0
							 , cC.skM1 & buds.a1
							 , cC.skM2 & buds.a2);
					// Go Phil... hmm...
//if ( AHint.hintNumber==10 && degree==3
//  && Arrays.equals(basesUsed, DEBUG_BASES) // row 1, row 3, row 6
//  && Arrays.equals(coversUsed, DEBUG_COVERS) ) { // box 3, col A, col C
//	Debug.breakpoint();
//}
					if ( searchForKraken() ) {
						searchCoversResult = true;
						if ( wantOneHint )
							return searchCoversResult;
					}
				}
			} // fi level == degree
			// move onto the next level
			if (coverLevel < degree) {
				cC = coverStack[++coverLevel];
				cC.index = c + 1;
				cC.prevIndex = -1;
			}
		} // for ever
	}
	// these fields are (logically) searchCovers variables
	private CoverStackEntry pC, cC; // the previous and current CoverStackEntry
	private boolean searchCoversResult;
	private int c; // the current cover region index
	private int coverIndex; // index in grid.regions of the current cover
	private int coverLevel; // current coversStack level, ie coversSet size
	private int numCovers; // the number of covers in the covers array
	private int ground; // partial-calculation of the maximum cover index
	// fins: indices of exo-fins and endo-fins
	private int finsM0, finsM1, finsM2;
//	// deletes: indices of potential eliminations
//	private int delsM0, delsM1, delsM2;
//	// sharks: cannabilistic eliminations
//	private int shrkM0, shrkM1, shrkM2;
	// get a mask of the region-types used as bases and covers
	private int baseMask, coverMask;

	// ========================== SEEK PHIL McKRAKEN ==========================

	/**
	 * Search the current base/cover set combo for Kraken Fish.
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
	 *  is found; but if accu is multiple then it could contain multiple; so
	 *  you had best apply them all BEFORE you call me again, because Im slow!
	 */
	private boolean searchForKraken() {
		ComplexFishHint base; // the causal or "base" hint
		KrakenFishHint kraken; // Kraken "wraps" base hint, to add elims
		int kDelM0,kDelM1,kDelM2;
		int ci, v2; // coversIndex, value2
		// presume Phil will not find any McKrakens;
		boolean result = false;
		try {
			// Type 1: We have something to delete so see if each covers except
			// bases (deletables) can be linked to every fin; one at a time.
			// nb: deletes holds all potential eliminations, including sharks
			// SKIP type 1 search in Generator coz KrakenTables full of nulls!
			if ( deletes.any() && Run.type!=Run.Type.Generator ) {
				// WARN: 2020-11-25 top1465 Kraken Swampfish no do KF1 search,
				// ie deletes are ALWAYS empty, makes this slow to test.
				kDeletes.set(deletes);
				final boolean anySharks = kSharks.setAny(sharks);
				sharks.clear();
				final boolean anyFins = fins.any();
				final Ass[] asses = this.kt1Asses;
				KT1_VISITOR.candidate = candidate;
				for ( int kd : kDeletes.toArrayA() ) {
					// It's a KF1 if each fin chains to cells[kd]-v.
					if ( anyFins && !isKrakenTypeOne(kd, asses) )
						continue;
					if ( anySharks ) {
						if ( kSharks.contains(kd) )
							sharks.clear().add(kd);
						else if ( !anyFins )
							continue;
						else
							sharks.clear();
					} // else sharks just stays empty
					// Kraken Found!!!! create hint and add to accu
					deletes.clear().add(kd);
					if ( (base=createBaseHint()) == null )
						continue; // should never happen
					// builds eliminations (reds) and chains
					Values valsToRemove = new Values();
					Pots reds = new Pots();
					List<Ass> chains = new LinkedList<>();
					fins.forEach1((fin) -> {
						Ass a = asses[fin];
						valsToRemove.add(a.value);
						reds.put(a.cell, new Values(a.value));
						chains.add(a);
					});
					kraken = new KrakenFishHint(this, reds, base, valsToRemove
							, KrakenFishHint.Type.ONE, chains, new Idx(fins));
//if ( HintValidator.KRAKEN_FISHERMAN_USES ) {
//	// swampfish are fine, it's swordfish an up that I'm having trouble with.
//	if ( degree>2 && !HintValidator.isValid(grid, kraken.redPots) ) {
//		kraken.isInvalid = true;
//		HintValidator.report("KF1", grid, kraken.squeeze());
//		continue; // Sigh.
//	}
//}
					result = true;
					if ( accu.add(kraken) ) // exit-early if accu says so
						return true;
					break; // one kraken per cause
				}
			}

			// Type 2: For each cover: find a chain from each candidate in the
			// current bases and this cover, and all the fins; to as yet unkown
			// elimination/s. Check only if this cover doesnt contain only base
			// candidates, because no chain is needed for a shark.
			sharks.clear();
			for ( ci=0; ci<numCovers; ++ci ) { // coverIndex
				if ( !coversUsed[covers[ci]] )
					continue;
				// kDeletes: v's in this cover & current bases, except sharks
				kDelM0 = coverVsM0[ci] & cB.vsM0 & ~kSharks.a0;
				kDelM1 = coverVsM1[ci] & cB.vsM1 & ~kSharks.a1;
				kDelM2 = coverVsM2[ci] & cB.vsM2 & ~kSharks.a2;
				if ( coverVsM0[ci] == kDelM0
				  && coverVsM1[ci] == kDelM1
				  && coverVsM2[ci] == kDelM2 )
					continue; // would be a normal Forcing Chain -> skip it
				// now add the fins and check all candidates
				kDeletes.set(kDelM0,kDelM1,kDelM2).or(fins);
				V2: for ( v2=1; v2<10; ++v2 ) { // endCandidate
					// It's a KT2 if a chain exists from ALL kDeletes+candidate
					// to any common elimination/s of v2.
					// NOTE WELL: isKrakenTypeTwo sets kFins and kt2sets
					if ( !isKrakenTypeTwo(v2) )
						continue;
					// Kraken Found!!!! create hint and add to accu
					// create the base hint
					deletes.clear(); // with no eliminations
					if ( (base = createBaseHint()) == null )
						continue; // should never happen
					// build the eliminations (reds) and the chains
					final Pots reds = new Pots();
					final int fv2 = v2;
					final List<Ass> chains = new LinkedList<>();
					kFins.forEach1((kf) -> {
						Cell kfc = grid.cells[kf]; // krakenFinCell
						reds.put(kfc, new Values(fv2));
						// kt2sets is populated by isKrakenTypeTwo
						kDeletes.forEach1((kd) ->
								chains.add(kt2sets[kd].getAss(kfc, fv2)));
					});
					Values valsToRemove = new Values(v2);
					// build the actual hint, which "wraps" the base hint.
					kraken = new KrakenFishHint(this, reds, base, valsToRemove
							, KrakenFishHint.Type.TWO, chains, new Idx(kFins));
if ( HintValidator.KRAKEN_FISHERMAN_USES ) {
	// the generator trips over invalid Kraken Swordfish (and-up, presumably)
	// but Swampfish are fine: so analyseDifficulty excludes Kraken Swordfish+
	// but the Validator has to stay. sigh.
	if ( degree>2 && !HintValidator.isValid(grid, kraken.redPots) ) {
		kraken.isInvalid = true;
		HintValidator.report("KF2", grid, kraken.squeeze());
		continue; // Sigh.
	}
}
					// clean-up for next time (was a nasty GUI only bug)
					for ( int j=0,J=kt2sets.length; j<J; ++j )
						this.kt2sets[j].clear();
					// add hint to the IAccumulator and pop if he says to
					result = true;
					if ( accu.add(kraken) ) // exit-early if accu says so
						return true;
				}
			}
		} finally {
			// finally clean-up in case anything threw an exception.
			// Ass's contain cells, which holds the whole Grid in memory,
			Arrays.fill(kt1Asses, null);
			for ( int j=0,J=kt2sets.length; j<J; ++j )
				this.kt2sets[j].clear();
		}
		return result;
	}
	private final Idx kSharks = new Idx();
	private final Idx kDeletes = new Idx();
	private final Idx kFins = new Idx();

	// ============================ KRAKEN TYPE 1 =============================

	/**
	 * indexed by initial assumptions cell.i, but the Ass is actually the end
	 * of the chain (the common consequence) so I'm an index of the end of each
	 * beginning (the chain herein leads you back to the initial assumption).
	 */
	private final Ass[] kt1Asses = new Ass[81];

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
		return fins.untilFalse(KT1_VISITOR);
	}
	// I create ONE instance of a static visitor rather than create a new one
	// for each and every visit because it is more efficient. The tables and
	// candidate fields are set ONCE in searchForKraken, then isKrakenTypeOne
	// sets the kd and asses fields, and then we call fins.untilFalse to visit
	// (below) each fin-cell. kt1Search then searces the implication-tree (ie
	// the KrakenTables) of the given ON (tables.ons[fin][candidate]) for the
	// target "$kd-$candidate" implication.
	//
	// I am in two minds about all this. I suspect it may be faster to just use
	// the kt2Search approach of finding ALL implications of each ON once, and
	// then look up this target in the tree (ie cache WHOLE implication tree of
	// each ON in an OFFs Idx, then test if the Idx contains "$kd-$candidate"),
	// so kt1 and kt2 would both use a single implication cache.
	private static final Kt1Visitor KT1_VISITOR = new Kt1Visitor();

	private static final class Kt1Visitor implements Idx.UntilFalseVisitor {
		public KrakenTables tables; // crapon tables
		public int candidate; // the Fish candidate value
		public int kd; // krakenDelete
		public Ass[] asses; // cooincident with grid.cells
		@Override
		public boolean visit(int fin) {
			if ( (asses[fin]=kt1Search(tables.ons[fin][candidate], kd, candidate, tables)) == null ) {
				// clean-up for next time
				Arrays.fill(asses, null);
				return false;
			}
			return true;
		}
	}

	/**
	 * kt1Search: Kraken Type 1 Chaining: returns the Ass that OFFs targetValue
	 * from targetIndice (ie a chain starting at the initialOn and ending with
	 * the elimination of targetValue from grid.cells[targetIndice]).
	 * <p>
	 * Stack: {@link #searchForKraken} -> {@link isKrakenTypeTwo} -> kt2Search
	 * all private in KrakenFisherman; so we can communicate via fields.
	 * <p>
	 * Note: each time we add to the queue: first we need to check this Ass is
	 * not and never has been in the queue, for which I use {@link Eff#seen}.
	 *
	 * @param initialOn the initial assumption (which is always an ON) has
	 *  effects, which cause effects, and so on; forming a consequence tree,
	 *  which we now search for the target index and value.
	 * @param tIndice targetIndex of the Cell we seek to eliminate from
	 * @param tValue targetValue to seek the elimination of
	 * @return the target Ass, ie the Ass which eliminates targetValue from the
	 *  grid cell at targetIndice, else null meaning "not found"
	 */
	private static Ass kt1Search(Eff initialOn, int tIndice, int tValue
			, KrakenTables tables) {
		// seen null initialOn in generate, where s__t gets a bit ____ed up.
		if ( initialOn == null )
			return null;
		assert initialOn.isOn; // the ON must be an ON
		assert initialOn.hasKids; // all ONs have effects
		Eff p, k; // p for parent, k for kid
		// queue is a circular buffer. read and write move independently, but
		// write outruns read, until read catches up at the end. The buffer
		// must be large enough so that write NEVER overtakes read. The only
		// way to work that out is trial by fire.
		Eff[] q = queue;
		int r=0, w=0, i,n;
		// calc the hashCode of the target "tIndice-tValue" Assumption ONCE.
		final int targetHC = Ass.hashCode(tIndice%9, tIndice/9, tValue, false);
		tables.clearSeens(); // set all Eff.seen = false in the KrakenTable
		for ( p=initialOn; p!=null; p=q[r], q[r]=null, r=(r+1)&QMASK ) {
			// mark this Eff as "we have seen this one" in the KrakenTable
			p.seen = true;
			// its faster to read a field than invoke hashCode()
			if ( p.hashCode == targetHC )
				return p;
			for ( i=0,n=p.numKids; i<n; ++i )
				if ( !(k=p.kids[i]).seen ) {
					q[w] = k;
					w = (w+1) & QMASK;
				}
		}
		return null;
	}

	// ============================ KRAKEN TYPE 2 =============================

	/**
	 * The LMAS of effects and Idx[v]'s of each possible assumption.<br>
	 * Contains max 81 - 17 (min clues) = 64 * 9 = 576 entries.
	 * <p>
	 * Note that I'm static: shared among the 3 KrakenFisherman instances.
	 */
	private static final Map<Eff,Kt2CacheEntry> kt2Cache = new HashMap<>(1024, 1.0F);

	/**
	 * A LinkedMatrixAssSet for each cell in the grid.
	 */
	private final LinkedMatrixAssSet[] kt2sets = new LinkedMatrixAssSet[81];
	{
		for ( int i=0; i<81; ++i )
			// use LinkedMatrixAssSet for it's faster Add (despite high RAM)
			kt2sets[i] = new LinkedMatrixAssSet();
	}

	/** Eliminations of the previous chain2 call: indexed by value 1 through 9.
	 * eliminations[v] is an Idx of the cells from which 'v' was eliminated
	 * as a consequence of the initialAss passed to chain2. */
	private final Idx[] kt2elims = new Idx[10];
	{
		for ( int v=1; v<10; ++v )
			kt2elims[v] = new Idx();
	}

	/**
	 * It's a Kraken Fish Type 2 if setting ALL kDeletes cells to v causes
	 * (through a simple XY-forcing-chain) a common elimination.
	 * <p>
	 * Post (when isKrakenTypeTwo returns true):<ul>
	 * <li>field Idx[1..9] kt2elims is populated with elims of each value 1..9
	 * <li>field Idx kFins contains indices cells with the common elimination;
	 *  an index of kt2sets.
	 * <li>field kt2sets[indice] is populated with all the Ass's (forcing
	 *  chains) required to link all kDelete cells to a common elimination
	 * </ul>
	 *
	 * @param targetValue The candidate for which the search is made
	 * @return true if a KF2 exists, false otherwise
	 */
	private boolean isKrakenTypeTwo(int targetValue) {
		final Idx kFins = this.kFins;
		final KrakenTables tables = this.tables;
		final Idx[] elims = this.kt2elims;
		// starting kFins is all instances of targetValue except the kDeletes
		kFins.setAndNot(this.idxs[targetValue], this.kDeletes);
		// store kDeletes in an array coz we'll probably iterate it twice.
		final int[] kdArray = this.kDeletes.toArrayA();
		int kd;
		// each kDelete needs a chain to some unknown common elimination.
		for ( int i=0,n=kdArray.length; i<n; ++i ) {
			kd = kdArray[i];
			// retain only those kFins that share OFF/s with all previous kd's
			// kt2Search: populates this.kt2sets[kd] with consequences of kd+v
			//            and sets kt2elims to eliminations caused by kd+v
			kt2Search(tables.ons[kd][candidate], kd);
			if ( kFins.and(elims[targetValue]).isEmpty() ) {
				// clean-up for next time (the reason for kdArray)
				for ( int j=0; j<=i; ++j )
					this.kt2sets[kdArray[j]].clear();
				return false;
			}
		}
		// Pointless doing this in Batch, which never reads the chains back.
		if ( Run.type == Run.Type.GUI ) {
			// now we know we're producing a hint, populate kt2sets + kt2elims
			// the old (slow) way, producing Ass's instead of looking-up Eff's
			// in the KrakenTables (which have all possible parents).
			for ( int i=0,n=kdArray.length; i<n; ++i ) {
				kd = kdArray[i];
				kt2Produce(new Ass(grid.cells[kd], candidate, ON)
						, this.kt2sets[kd], elims);
			}
		}
		return true;
	}

	private static final class Kt2CacheEntry {
		public final LinkedMatrixAssSet set;
		public final Idx[] elims;
		// make a shallow copy of the given data-structures
		private Kt2CacheEntry(LinkedMatrixAssSet theSet, Idx[] theElims) {
			this.set = new LinkedMatrixAssSet(theSet);
			this.elims = new Idx[10];
			for ( int v=1; v<10; ++v )
				this.elims[v] = new Idx(theElims[v]);
		}
	}

	/**
	 * Kraken Type 2 Chaining: re-populates the given LinkedMatrixAssSet with
	 * ALL of the XY forcing chain consequences (both OFFs and ONs) of the
	 * initialOn assumption. Each chain starts from the target Ass/s, which
	 * is/are-all OFF/s, and leads back-up to initialOn.
	 * <p>
	 * The output is we set theSet to the whole consequence tree of initialOn,
	 * and also set theElims to the consequent OFFs (indices).
	 * <p>
	 * Note that "the whole consequence tree" includes the ONs which are NOT
	 * required, but {@link LinkedMatrixAssSet#getAss} returns only the OFFs,
	 * because we said so in it's constructor, so it works anyways.
	 * <p>
	 * Stack: {@link #searchForKraken} -> {@link isKrakenTypeTwo} -> kt2Search
	 * all private in KrakenFisherman; so we can communicate via fields.
	 * <p>
	 * The profiler says we spend 59.9% of runtime in this method, so the
	 * performance of this method is ____ing crucial!
	 * <p>
	 * I got that down to 51.3% of runtime, but that is as far as I know how.
	 * Major improvement can only come from doing ALL the chaining prior to the
	 * kt?Search methods which then just looks-up the results, ala hobiwan;
	 * but I run-away from "packed" chain-table-arrays, but MUST consider it.
	 * <p>
	 * 2020-11-26 09:10 1466     522,219,907,500       (08:42)       356,220,946
	 * 2020-11-26 09:17 20,058 ms (50.9%)
	 *
	 * @param initialOn the initial ON Eff(ect) (which is always an ON)
	 * @param theSet is re-populated with all of the effects of initialOn
	 * @param elims[] is re-populated with eliminations of each value
	 */
	private void kt2Search(final Eff initialOn, int kd) {
// I've decided to try caching results instead, which is effectively all that
// hobiwan achieves by precalculating all possibilities, except that I do NOT
// have to calculate ALL possibilities, just the ones we actually use. Note
// that the each initialOn appears in multiple kDelete sets, so caching its
// effects is VERY efficient; and caching them accross the three instances of
// KrakenFisherman is more efficient again.
//		// cache results! the cache is cleared when the puzzle has changed.
//		// 2020-11-26 09:10 1465 522,219,907,500 (08:42) 356,220,946
//		// 2020-11-26 13:01 1465 413,611,828,000 (06:53) 282,328,892 with cache
//		// hit rate 512,170,554/512,744,474*100=99.89% very high!!!
//		// 2020-11-26 14:10 1465 270,863,495,300 (04:30) 184,889,757 cache v2,
//		// remove parameters and set the fields instead of copying contents.
//		// 2020-11-26 15:40 1465 250,464,684,500 (04:10) 170,965,654 fixed bug
//		// in caching which left Krakens (mostly) high-and-dry. Release time!
//		// The cache breaks Kraken Swordfish, and presumably up. Why?
//		// A: the kt2Cache was NOT static: each KF has it's own cache, so
//		// initialising THE tables badly determined time-to-clear-cache.
//		// Now both the tables AND cache are shared (static) its all good.
		final Kt2CacheEntry entry = kt2Cache.get(initialOn);
		if ( entry != null ) {
//			if ( false ) { // DEBUG ONLY
//				// re-run the search and validate results match those cached
//				kt2SearchActual(initialOn, this.kt2sets[kd], kt2elims);
//				assert this.kt2sets[kd].equals(entry.set);
//				for ( int v=1; v<10; ++v )
//					assert kt2elims[v].equals(entry.elims[v]);
//			} else {
				// just set the global fields
				this.kt2sets[kd] = entry.set;
				for ( int v=1; v<10; ++v )
					kt2elims[v].set(entry.elims[v]);
//			}
			return;
		}
		kt2SearchActual(initialOn, this.kt2sets[kd], kt2elims);
		// the CacheEntry constructor takes a shallow copy of data-structures
		kt2Cache.put(initialOn, new Kt2CacheEntry(this.kt2sets[kd], kt2elims));
	}

	/**
	 * kt2SearchActual is the kt2Search you have without the cache that is now
	 * in the actual kt2Search. Confused yet? Well strap-it-on people. This can
	 * only get harder... and faster... We are GO for 12 Inch Nails!
	 *
	 * @param initialOn
	 * @param theSet
	 * @param elims
	 */
	private void kt2SearchActual(final Eff initialOn
			, final LinkedMatrixAssSet theSet, final Idx[] elims) {
		Eff[] kids; // an array coz even an ArrayList is too slow here.
		Eff c; // child
		int i,n;
		// The queue array is a circular buffer: read and write indexes move
		// independantly. Write allways outruns read, but never overtakes it.
		// Think of them as doing laps of a race track (the array) which is
		// made large enough to achieve the "never overtakes" rule. 64 seems
		// "safe" to me; 32 works for top1465; 16 does not; so 32 it is.
		// nb: QSIZE must be a power of 2 just for the & QMASK trick.
		// nb: When read catches up with write it is the end of the queue.
		// nb: This mess is forced on us by the uber-performant requirement;
		//     for top1465.F10.mt: 33,906 ms (69.7%) vs 25,887 ms (64.3%)
		//     pre-test, circular buffer, split on p.isOn: 20,296 ms (51.4%)
		final Eff[] queue = KrakenFisherman.queue; // local reference for speed
		int r = 0  // the read index
		  , w = 0; // the write index
		// clear my s__t BEFORE validating input
		theSet.clear();
		clear(elims);
		// validations for generate: seen null initialOn in analyseDifficulty
		if ( initialOn==null || !initialOn.isOn || !initialOn.hasKids )
			return;
		theSet.add(initialOn); // throws if initialOn is null
		for ( Eff p = initialOn; // parent
			  p != null;
			  // p = queue.poll() sets queue[read]=null so that we can tell
			  //                  when we have reached the end of the queue
			  p=queue[r], queue[r]=null, r=(r+1)&QMASK
		) {
			// nb: All Eff's in the queue have kids, as does the initialOn
			if ( p.isOn ) { // all my kids (usually 3+) are OFFs
				for ( kids=p.kids,i=0,n=p.numKids; i<n; ++i ) // child
					// if not seen previously. Note that theSet is an output.
					// nb: theSet is "Funky", ie does NOT replace existing.
					if ( theSet.add(c=kids[i]) ) {
						elims[c.value].add(c.i);
						// if c has no kids then examining it is pointless, so
						// pre-test c.hasKids, instead of hammering the s__t
						// out of the queue and turning it into a constrictor.
						// top1465 4,312,055,146/14,010,624,552*100=30.78% of
						// OFFs cause an ON, and 14-bill is REALLY BIG number!
						// 2020-11-26 09:10 1465 522,219,907,500 (08:42) 356,220,946
						if ( c.hasKids ) {
							queue[w] = c;
							w = (w+1) & QMASK;
						}
					}
			} else // p is an OFF so its 1-or-2 kids are ONs, so we still need
				// to distinctify, but there is no elims.add.
				// Note that the ONs are NOT required in output (the OFFs are).
				// We get away with adding them because getAss uses "isOn" (set
				// in constructor) to determine whether it looks for offs (as
				// these do) or ons, so getAss ignores the ons; which I add coz
				// it is faster than a local boolean array. I do not understand
				// why. I thought a boolean array would be faster, but testing
				// shows otherwise; and I trust measurements, especially when
				// they diverge from my expectations. Humble pie.
				for ( kids=p.kids,i=0,n=p.numKids; i<n; ++i ) // child
					// all ONs have kids, so just add the sucker to the queue:
					// c.kidsHCs.any is always true, so testing it would just
					// be a waste of CPU time.
					// nb: coz we don't really need the ONs in output I tried
					// using a boolean[81] to filter out repeat ONs but it was
					// a second a puzzle slower, contrary to expectations.
					if ( theSet.add(c=kids[i]) ) {
						queue[w] = c;
						w = (w+1) & QMASK;
					}
		}
	}

	private void clear(Idx[] elims) {
		for ( int v=1; v<10; ++v )
			elims[v].clear();
	}

	// ----------------- KT2PRODUCE -----------------

	/** Just to make Ass constructor more self-documenting. */
	private static final boolean ON = true;
	/** Just to make Ass constructor more self-documenting. */
	private static final boolean OFF = false;
	/**
	 * kt2Produce: redoes the kt2Search search, except it is slower coz it
	 * creates it's own Ass's, which are needed for the step, so only run
	 * after kt2Search has already worked-out that we are going to produce
	 * a hint. Produce takes about three times as long as Search, which
	 * which doest matter if it's ONLY used when we're producing a hint.
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
		final Cell[] cells = grid.cells;
		final Deque<Ass> queue = this.kt2assQueue;
		Ass e; // the effect of Ass a (the next Ass in the consequences chain)
		Cell c, sib; // a.cell; sibling of a.cell
		int[] vals;
		int x; // a.value; use x coz v is the Fish candidate value field
		int sx; // shiftedX;
		int j,J; // just an index
		assert initialOn.isOn;
		queue.clear(); // should already be empty, but clear it anyway.
		set.clear(); // likewise should already be empty (I think).
		set.add(initialOn); // prevent initialOn being re-added (stop looping)
		clear(elims);

		for ( Ass a=initialOn; a!=null; a=queue.poll() ) {
			// this loop is the only place 'a' changes so we can cache it's
			// attributes just to save repeatedly dereferencing them
			x = a.value;
			c = a.cell;
			if ( a.isOn ) {
				sx = VSHFT[x];
				// 1. add an OFF for each other potential value of a.cell.
				vals = VALUESES[c.maybes.bits & ~sx];
				for ( j=0,J=vals.length; j<J; ++j ) {
					if ( set.add(e = new Ass(c, vals[j], OFF, a)) ) {
						queue.add(e);
						elims[vals[j]].add(c.i);
					}
				}
				// 2. add an OFF for each other possible position of a.value
				//    in each of a.cell's three regions.
				for ( j=0; j<20; ++j  ) {
					if ( ((sib=c.siblings[j]).maybes.bits & sx) != 0
					  && set.add(e=new Ass(sib, x, OFF, a)) ) {
						queue.add(e);
						elims[x].add(sib.i);
					}
				}
			} else {
				// 1. if a.cell has only two potential values then it must
				//    be the other potential value, so add an ON to the queue.
				if ( c.maybes.size == 2
				  && set.add(e=new Ass(c, VALUESES[c.maybes.bits & ~VSHFT[x]][0], ON, a)) )
					queue.add(e);
				// 2. foreach of a.cell's 3 regions: if region has 2 places for
				//    a.value then the other cell must be a.value, so add an ON
				//    to the queue.
				for ( j=0; j<3; ++j  ) {
					// nb: hit rate too low to cache c.regions[j], it's faster to dereference it again in the maybe 10% of cases where there's 2 positions for a.value in the region
					if ( c.regions[j].indexesOf[x].size == 2
					  && set.add(e=new Ass(cells[c.regions[j].idxs[x].otherThan(c.i)], x, ON, a)) )
						queue.add(e);
				}
			}
		}
	}
	/** kt2Produce's to-do list. */
	private final Deque<Ass> kt2assQueue = new LinkedList<>();

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
			deletes.forEach1((i) -> reds.put(grid.cells[i], new Values(candidate)));
		if ( sharks.any() )
			sharks.forEach1((i) -> reds.put(grid.cells[i], new Values(candidate)));

		baseMask = getRegionTypesMask(basesUsed);
		coverMask = getRegionTypesMask(coversUsed);

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
		final Pots yellow = sharks.any() ? new Pots(sharks.cells(grid), cv) : null;

		// paint all sharks yellow (except eliminations which stay red)!
		if ( yellow!=null && !yellow.isEmpty() ) {
			yellow.removeFromAll(green, blue, purple);
		}

		// paint all eliminations red (including sharks)!
		reds.removeFromAll(green, blue, purple, yellow);

		// paint the endo-fins purple, not corners (green) or exo-fins (blue).
		purple.removeFromAll(green, blue);

		String debugMessage = "KrakenFisherman1";

		ComplexFishHint hint = new ComplexFishHint(this, type, false, candidate
				, usedBases, usedCovers, reds, green, blue, purple, yellow
				, debugMessage);

		if ( HintValidator.KRAKEN_FISHERMAN_USES ) {
			// swampfish are fine, it's swordfish an up that I'm having trouble with.
			if ( degree>2 && !HintValidator.isValid(grid, hint.redPots) ) {
				hint.isInvalid = true;
				HintValidator.report("KrakenFisherman1_Base", grid, hint.squeeze());
				return null; // Sigh.
			}
		}

		// just return the hint coz my caller may upgrade to HdkKrakenFishHint
		// and then add it to the accu. Sigh.
		return hint;
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
	 * @return are there atleast degree bases and allCovers; so we skip the
	 * Fish search when I return false.
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
		Idx vs;
		for ( ARegion region : regions )
			if ( !region.containsValue[candidate] ) {
				bases[numBases] = region.index;
				vs = region.idxs[candidate];
				baseVsM0[numBases] = vs.a0;
				baseVsM1[numBases] = vs.a1;
				baseVsM2[numBases++] = vs.a2;
			}
	}

	private void addCovers(ARegion[] regions) {
		Idx vs;
		for ( ARegion region : regions )
			if ( !region.containsValue[candidate] ) {
				allCovers[numAllCovers] = region.index;
				vs = region.idxs[candidate];
				allCoverVsM0[numAllCovers] = vs.a0;
				allCoverVsM1[numAllCovers] = vs.a1;
				allCoverVsM2[numAllCovers++] = vs.a2;
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

		// NOTE that IntHashSet does not grow like a java.util.Set.
		// KrakenFisherman.maxKids = 22 is NOT big enough for full Ass.hashCode
		// so we let the largest ones overfill, so there are more collisions,
		// but that's OK, it's just a bit slower. Set size a compromise between
		// construction time and get/add time, which usually wins.
		// Ideally we'd delay creating the table until we add to it, coz only
		// 30% of OFFs cause an ON; so I try that only to find its SLOWER; so
		// ignore me, I'm barking, evidently. This is WHY we performance test.
		public final IntHashSet kidsHCs = new IntHashSet(32);
		// NOTE: kids was an ArrayList<Eff> now its an array, for speed.
		// If you get an AIOOBE then your code has gone wrong somewhere.
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
//				kid.addParent(this); // let it NPE when child is null
				kid.parents.linkLast(this);

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
		private int myHintNumber;
		/** The grid.puzzleID for which tables are initialised. */
		private long myPuzzleID;

		/**
		 * The offTable is all the OFF Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] offs = new Eff[81][10];

		/**
		 * The onTable is all the ON Eff(ects).
		 * The first index is the cell indice (concurrent with grid.cells).
		 * The second index is the potential value 1..9.
		 */
		public final Eff[][] ons = new Eff[81][10];

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
			if ( myHintNumber==AHint.hintNumber && myPuzzleID==grid.puzzleID
			  && !forceRefresh )
				return false;

//			final long start = System.nanoTime();
			Eff[] myOns, myOffs;
			Eff on, off, kid;
			int i, bits, sv, o;

			// 1. construct all possible "moves" (skip set cells).
			// a "move" is a direct effect.
			for ( Cell cell : grid.cells )
				if ( cell.maybes.bits != 0 ) {
					myOns = ons[cell.i];
					myOffs = offs[cell.i];
					for ( int v : VALUESES[cell.maybes.bits] ) {
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
				bits = cell.maybes.bits;
				for ( int v : VALUESES[bits] ) {
					on = myOns[v];
					off = myOffs[v];
					sv = VSHFT[v];

					// Assuming that cell is set to value
					// 1. add an OFF for each other potential value of this cell
					for ( int v2 : VALUESES[bits & ~sv] )
						on.addKid(myOffs[v2]);
					// 2. add an OFF for each of my siblings which maybe v
					for ( Cell sib : cell.siblings )
						if ( (sib.maybes.bits & sv) != 0 )
							on.addKid(offs[sib.i][v]);

					// Assuming that value is eliminated from cell
					// 1. if cell has two maybes, add an ON to the other maybe
					if ( cell.maybes.size == 2 )
						off.addKid(myOns[VALUESES[bits & ~sv][0]]);
					// 2. if any of my regions has two v's, other posi is ON
					for ( ARegion r : cell.regions )
						if ( r.indexesOf[v].size == 2
						  // -1 actually happened, and I don't understand how.
						  // I guess r.indexesOf[v] must be dodgy?
						  && (o=r.idxs[v].otherThan(i)) > -1
						  // null kid actually happened (and not for -1)
						  // so r.indexesOf[v] must be dodgy!
						  && (kid=ons[o][v]) != null )
							off.addKid(kid);
				}
			}
			myHintNumber = AHint.hintNumber;
			myPuzzleID = grid.puzzleID;
//			System.out.format("KT init %,d ns.\n", System.nanoTime()-start);
			return true;
		}

		/**
		 * Set the seen flag of each onTable and offTable entry to false.
		 */
		void clearSeens() {
			for ( int i=0; i<81; ++i )
				for ( int v=1; v<10; ++v ) {
					if ( ons[i][v] != null )
						ons[i][v].seen = false;
					if ( offs[i][v] != null )
						offs[i][v].seen = false;
				}
		}

	}

}
