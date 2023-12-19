/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.*;
import static diuf.sudoku.Constants.beep;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.Permuter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * URT UniqueRecTangle and loops implements the {@link Tech#URT} Sudoku solving
 * technique. This implementation supports types 1 to 4 and Hidden. Any skewed
 * non-orthogonal loops (rare) are also detected. A rectangle is just a loop of
 * 4 cells.
 * <p>
 * NOTE that SE includes URT Types 5 and 6 in lesser hint types, but the Sudoku
 * community has "reserved" 5 and 6, so I use Type 7 to mean a Hidden URT.
 * The distinction of Types 5 and 6 from lesser hint types is unnecessary, so
 * they are dropped from this minimum rule set implementation.
 * <p>
 * Q: Why do people make things more complicated than they need to be?<br>
 * A: Because they did not know that the complications were unnecessary.
 */
public final class URT extends AHinter
		implements diuf.sudoku.solver.hinters.IPrepare
//				 , diuf.sudoku.solver.hinters.IReporter
{
//	@Override // IReporter
//	public void report() {
//		Log.teeln(tech.name()+": COUNTS="+Arrays.toString(COUNTS));
//	}
//	private long[] COUNTS = new long[15];

	// maximum number of cells in a workLoop
	private static final int LOOP_SIZE = 16; // guess
	// maximum number of loops in the loops array
	private static final int MAX_LOOPS = 16; // observed 12

	// all naked/hidden sets of more than 4 cells are degenerate: they are the
	// combination of simpler hints, so SE never finds them, so no look for em
	private static final int MAX_SET_SIZE = 5;

	/** The value when a Map is used as a Set. */
	private static final Object PRESENT = new Object();

	/**
	 * Combine all of the maybes in the maybes array into one aggregate value
	 * (they are bitsets, so just {@code aggregate |= maybe}), and if there is
	 * exactly $size of them return them, else return 0. I also return 0 if
	 * {@code VSIZE[maybe] < 2}, to suppress any naked singles.
	 *
	 * @param maybeses
	 * @param size
	 * @return
	 */
	private static int aggregate(final int[] maybeses, final int size) {
		int aggregate = 0;
		for ( int maybes : maybeses ) {
			if ( VSIZE[maybes] < 2 )
				return 0;
			aggregate |= maybes;
		}
		if ( VSIZE[aggregate] != size )
			return 0;
		return aggregate;
	}

	/**
	 * Does this permutation of indexes contain i1 but NOT i2?
	 * These are indexes in region.cells.
	 *
	 * @param perm
	 * @param i1
	 * @param i2
	 * @return
	 */
	private static boolean has1ButNot2(final int[] perm, final int i1, final int i2) {
		assert i1 != i2;
		boolean found1 = false;
		int n = perm.length;
		int i = 0;
		do
			if ( perm[i] == i1 )
				found1 = true;
			else if ( perm[i] == i2 )
				return false;
		while (++i < n);
		return found1;
	}

	/**
	 * Is this loop actually a loop, ie is each region visited by exactly two
	 * cells of opposing parity.
	 * <p>
	 * isLoop builds two parity-sets: even and odds. The first cell is even,
	 * and the second is odd, and so on around the loop. If this parity has
	 * already seen this region then this is NOT a loop. Then we check that
	 * the two parity-sets contain the same regions.
	 * <p>
	 * <b>BUG</b>: my bitwise implementation misses non-orthogonal-loops, with
	 * two cells in the same box, but not in the same row/col. But I care NOT!
	 * It's SO much faster. Non-orthogonal means "not 90 degrees"; ie "does not
	 * line up". Ideally, a bitwise impl recognising same box. But YOU fix it!
	 *
	 * @param loop the cells in the purported loop
	 * @param n the number of cells in workLoop
	 * @return is workLoop actually a loop
	 */
	private boolean isLoop(final Cell[] loop, final int n) {
		assert n > 3; // a loop is atleast 4 cells
		int r, odds, i = 2
		  , even = loop[0].ribs;  // first cell is even,
		do { // as is every second one thereafter
			if((even & (r=loop[i].ribs)) > 0) return false;
			even |= r;
		} while ( (i+=2) < n );
		odds = loop[1].ribs; // second cell is odd,
		i = 3;
		do { // as is every second one thereafter
			if((odds & (r=loop[i].ribs)) > 0) return false;
			odds |= r;
		} while ( (i+=2) < n );
//		if ( true ) {
//			System.out.println(Frmu.frmt(loop, n, CSP, CSP));
//			System.out.println("even: "+Regions.usedString(even));
//			System.out.println("odds: "+Regions.usedString(odds));
//		}
		return even == odds;
	}

//RETAIN despite not being used, in case correctness is preferred to speed.
	/**
	 * Is this loop actually a loop? This slow implementation uses 2 HashSets.
	 * The regions of the even cells go in 'even', and the regions of the odd
	 * cells go in 'odds'. If these two sets are equal then it's a loop; ergo
	 * each region has been visited ONCE in each parity, or not at all.
	 * <p>
	 * This implementation is slower than isLoopFast, but is (I think) more
	 * accurate, as it (I think) accounts correctly for cells that are in the
	 * same box, but are not in the same row/col; whereas isLoopFast gets that
	 * part wrong, and I know not how to fix it. I doubt it CAN be rectified
	 * efficiently, but I am just a humble programmer. I get stuff wrong too.
	 *
	 * @param loop the loop of cells to test: always 4 or more cells
	 * @param n the number of cells in the loop
	 * @return is this loop actually a loop?
	 */
	private boolean isLoopSlow(final Cell[] loop, final int n) {
		// I use Map as Set to access putIfPresent
		HashMap<ARegion, Object> even = new HashMap<>();
		HashMap<ARegion, Object> odds = new HashMap<>();
		HashMap<ARegion, Object> parity; // even or odds
		for ( int i=0; i<n; ++i ) {
			if(i%2==0) parity=even; else parity=odds;
			for ( ARegion region : loop[i].regions )
				if ( parity.putIfAbsent(region, PRESENT) != null )
					return false;
		}
		// All regions must have been visited once with each parity (or never)
		return even.keySet().equals(odds.keySet());
	}

	// ============================ INSTANCE STUFF ============================

	// a LinkedHashSet whose add ignores null or pre-existing hint
	private final Set<AURTHint> list = new LinkedHashSet<AURTHint>(16, 1F) {
		private static final long serialVersionUID = 20145039L;
		// HashSet.contains is definitely fast enough, for any number of hints.
		@Override
		public boolean add(final AURTHint h) {
			return h!=null && !super.contains(h) && super.add(h);
		}
		// ignore the null collection
		@Override
		public boolean addAll(final Collection<? extends AURTHint> c) {
			return c!=null && super.addAll(c);
		}
	};

	// startCell must be a field for correctness, not just for prevention of
	// garbage creation; alternatively you could check that a loop-is-a-loop
	// by retrieving the startCell from the loop (it is index 0)
	private Cell startCell = null;
	// v1andV2 is a bitset containing v1 and v2, the two maybes of the
	// startCell. It is a field rather than parameters because it does not
	// change during recursion, so there is no point in passing it (or v1
	// and v2) down the stack.
	private int cands;
	// the number of cells in the workLoop
	private int wlSize;
	// working storage for current loop.
	private final Cell[] workLoop = new Cell[LOOP_SIZE];
	// fast workLoop.contains(cell)
	private final boolean[] isInWorkLoop = new boolean[GRID_SIZE];
	// An Idx of the workLoop
	private final Idx workLoopIdx = new Idx();
	// the number of loops currently in the loops array
	private int numLoops;
	// the number of cells in each loop found to date.
	private final int[] loopSizes = new int[MAX_LOOPS];
	// the loops found to date.
	private final Cell[][] loops = new Cell[MAX_LOOPS][];
	// Idx of each loop found to date.
	private final Idx[] loopsIdxs = new Idx[MAX_LOOPS];
	// the number of extraCells
	private int numExtraCells;
	// the cells in this loop with extra (not v1 or v2) values.
	private final Cell[] extraCells = new Cell[LOOP_SIZE]; // 4 is not enough
	// temporary Idxs
	private final Idx tmp1 = new Idx();
	private final Idx tmp2 = new Idx();

	private int extraCands, numExtraCands;

	public URT() {
		super(Tech.URT);
		for ( int i=0; i<MAX_LOOPS; ++i )
			loopsIdxs[i] = new Idx();
	}

	// runs after puzzle loaded into grid, and before the first findHints.
	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		setIsEnabled(true); // just incase I disable myself in carp.
	}

	@Override
	public void setFields(final Grid grid) {
		this.grid = grid;
		idxs = grid.idxs;
	}

	@Override
	public void clearFields() {
		grid = null;
		idxs = null;
	}

	@Override
	public boolean findHints() {												//  13,803
		assert list.isEmpty(); // cleared in clean, even upon exception
		final boolean result;
		try {
			final int n = search(); // returns number of hints found
			if ( result = n > 0 ) {												//     629
				if ( n == 1 ) {
					accu.add(list.iterator().next());
				} else {
					final AURTHint[] array = list.toArray(new AURTHint[n]);
					Arrays.sort(array, AURTHint.BY_ORDER);
					for ( AURTHint hint : array )
						if ( accu.add(hint) )
							break;
				}
			}
		} finally {
			clean();
		}
		return result;
	}

	// search the grid for UniqueRectangles. If a hint is found then add
	// it to the hintsSet, to filter-out duplicates, and return how many.
	private int search() {														//  13,803
		Cell loop[], c1, c2;
		int n, v1, v2, i, j;
		for ( Cell cell : grid.cells ) {
			if ( cell.size == 2 ) {												// 188,607
				v1 = VFIRST[cell.maybes];
				v2 = VFIRST[cell.maybes & ~VSHFT[v1]];
				assert v1>=1 && v1<=9;
				assert v2>=1 && v2<=9;
				assert v2>v1;
				clearLoops();
				clearWorkLoop();
				this.startCell = cell;
				this.cands = cell.maybes;
				// recurse finds all loops of cells that maybe v1 and v2 in the
				// grid, starting at startCell, having atmost 2 extra (not v1
				// or v2) maybes (more in recurse). It returns "Any loops?"
				// nb: startCell is a field, so does not change upon recursion.
				// param cell = the current cell in this loop. The same as
				//       startCell, so we add a loop when length > 3 and work
				//       our way back to the startCell, recursively.
				// param prevRegion = null means none: there is no previous
				//       region type for the first call.
				// param extraCands = 0 means none. Each recursion has its own
				//       extraCands variable, localising it, so when we pop-up
				//       out of each recursion extraCands returns to what it
				//       was when we were last at this level in the callstack,
				//       which is devilishly clever. I like it.
				// param x = 2 means atmost two cells with extra values. Its a
				//       parameter because its decremented in the descent, but
				//       returns to its previous value in each ascent
				if ( recurse(cell, null, 0, 2) ) {								//  56,038
					// look for hints in the loops
					i = 0;
					do {														//  75,419
						loop = loops[i];
						n = loopSizes[i];
						// get cells in loop with extra (not v1 or v2) values.
						// It is a field so no recreate, and no pass around.
						// nb: all cells in loop maybe v1 and v2
						clearExtraCells();
						j = 0;
						do
							if ( loop[j].size > 2 )
								extraCells[numExtraCells++] = loop[j];
						while (++j < n);
						// type of URT possible mostly depends on numExtraCells
						if ( numExtraCells == 1 ) {								//     291
							// FOUND Type 1
							list.add(new URT1Hint(grid, this, loop, n, v1, v2
							, new Pots(extraCells[0].indice, cands, DUMMY)));
						} else if ( numExtraCells == 2 ) {						//  18,574
							// Type 2, 3 or 4 is possible
							c1 = extraCells[0];
							c2 = extraCells[1];
							// get extra (not v1 or v2) candidates, and count
							extraCands = (c1.maybes | c2.maybes) & ~cands;
							numExtraCands = VSIZE[extraCands];
							if ( numExtraCands == 1 ) // try Type 2
								list.add(seekType2OrHidden(v1, v2, loop, n));
							else if ( numExtraCands >= 2 ) // try Type 3
								list.addAll(seekType3(v1, v2, c1, c2, loop, n));
							// try Type 4
							list.add(seekType4(v1, v2, c1, c2, loop, n));
						} else if ( numExtraCells > 2 ) {						//  56,554
							// only Type 2 or Hidden Unique Rectangle possible
							list.add(seekType2OrHidden(v1, v2, loop, n));
						} else { // 0
							return carp(v1, v2, cell, loop, n);
						}
					} while (++i < numLoops);
				}
			}
		}
		// 95+% of calls fail, so do NOT create a new empty ArrayList. Putz!
		return list.size();
	}

	/**
	 * The recurse method recursively finds all "loops" from the given start
	 * cell which maybe both v1 and v2. Note that the top-call is for each
	 * startCell that maybe EXACTLY v1 and v2, which defines v1 and v2.
	 * At most two cells in a loop may have extra (not v1 or v2) values,
	 * or 2-or-more-cells may share a-single-common-extra-value.
	 * <p>
	 * NOTE WELL: Hidden Rectangles (a Johnny-come-lately) break these rules by
	 * a flick of the wrist, taking three cells with extra values.
	 * <p>
	 * Each recurse-call searches the regions of "the current cell" for the
	 * next current-cell, and so-on, until we find ourselves back at the
	 * startCell, forming a loop, which we add to the loops array. So the
	 * workLoop is only a POTENTIAL loop, until we get back to the startCell.
	 * <p>
	 * Do not panic! This is pretty complex, so be patient with yourself. You
	 * WILL understand it eventually. It is just a bit tricky, thats all.
	 *
	 * @param cc currentCell: the current "corner" of the potential loop to be
	 *  extended. Each cell in the grid which has exactly 2 potential values
	 *  becomes a startCell, from which we extend the potential loop by finding
	 *  the next cell (this variable); and then I recursively call myself to
	 *  find the next "corner", and so-on, until we get back to the startCell.
	 *  Note startCell is a field, so it does not change during recursion.
	 * @param prevRgn previousRegion: the region shared by the current cell and
	 *  the previous cell (cc of the previous recursion), which is skipped in
	 *  the search for the next cell, to avert infinite recursion caused by
	 *  endlessly searching back-and-forth in a single region. <br>
	 *  prevRgn starts as null meaning "none".
	 * @param extraCands extra values in the current loop as a bitset, always
	 *  starts as 0 (meaning empty). The startCells two maybes are v1 and v2,
	 *  and the other maybes (all except v1 and v2) of each subsequent cell are
	 *  "extra", accumulated in extraCands. There can be at most 2 cells in a
	 *  loop with extra values OR there is only one extra value, ie ALL (may be
	 *  more than 2) cells with extraCands share a single extra value. <br>
	 *  NB: extraCands is a parameter (a local) which reverts automatically as
	 *  we ascend back-up the recursive callstack, going back-to the callers
	 *  variable, with its existing value (unchanged by recursion). This
	 *  behaviour is required for algorithmic correctness.
	 * @param x numExtraValueCellsRemaining: the remaining number of cells with
	 *  extraCands allowed in the loop. x starts at 2, and is decremented as we
	 *  extend the loop, while it stays above 0 (or VSIZE[extraVs]==1).
	 * @return were any loops found.
	 */
	private boolean recurse(final Cell cc, final ARegion prevRgn, int extraCands, final int x) {
		// ANSI-C style vars, so recurse has ONE stackframe, for speed.			//  7,449,242
		// Note that recurse is hammered, so be uberspeedy here.
		ARegion r; // cc.region we explore for next cell to add to loop
		Cell rCells[], cell; // to try to add to loop
		int rti, i, m; // regionTypeIndex, cellIndex, cellMaybes
		workLoop[wlSize++] = cc;
		isInWorkLoop[cc.indice] = true;
		// presume that no loops will be found
		rti = 0;
		do { // BOX, ROW, COL
			// skip if the current rti equals the previous region type
			if ( (r=cc.regions[rti]) != prevRgn ) { // intentional reference equals
				i = 0;
				rCells = r.cells;
				do {
					cell = rCells[i];
					if ( cell==startCell && wlSize>3 ) {
						// loop closed: store if valid and not already exists.	//  1,692,955
						if ( isLoop(workLoop, wlSize)
						  && !exists(workLoopIdx.set(workLoop, wlSize))
						) {														//     75,419
							loopsIdxs[numLoops].set(workLoopIdx);
							loopSizes[numLoops] = wlSize;
							loops[numLoops++] = Arrays.copyOf(workLoop, wlSize);
							return true;
						}
					// else if cell may be both v1 and v2
					//      && cell is NOT already in the workLoop
					} else if ( ((m=cell.maybes) & cands) == cands
						   && !isInWorkLoop[cell.indice]
					) {															// 23,501,251
						// add cell.maybes to my extraCands to build-it-up
						extraCands |= m & ~cands;
						// continue this search:
						// if cell.maybes are v1 and v2 (no extras)
						if ( ( VSIZE[m] == 2
							// or cell has extra values and the max number of
							//    cells with extra values (2) is not exceeded
							|| x > 0
							// or cell has 1 extra value, same as all previous
							//    cells with an extra value (for type 2 only)
							|| VSIZE[extraCands] == 1
							// or HiddenRectangle: 4 cells with 1+ extraCands
						    || (wlSize<4 && VSIZE[extraCands]>0) )				//  7,260,635
							// add cell to loop, and examine its three regions
							// seeking next cell to add to loop... and so on.
						  && recurse(cell, r, extraCands, VSIZE[m]>2?x-1:x) )
							return true;
					}
				} while (++i < REGION_SIZE);
			}
		} while (++rti < NUM_REGION_TYPES);
		// rollback
		isInWorkLoop[cc.indice] = false;
		--wlSize;
		return false;
	}

	// distinctness test: is workLoop already in workLoops
	private boolean exists(final Idx workLoopIdx) {
		if ( numLoops > 0 ) {
			Idx x;
			final long wlA = workLoopIdx.m0;
			final int wlB = workLoopIdx.m1;
			int i = 0;
			do
				if ( (wlA & (x=loopsIdxs[i]).m0) == x.m0
				  && (wlB & x.m1) == x.m1 )
					return true;
			while (++i < numLoops);
		}
		return false;
	}

	// I handle Type 2, and flick-pass HiddenURT
	private AURTHint seekType2OrHidden(final int v1, final int v2
			, final Cell[] loop, final int n) {
		assert numExtraCells > 0;
		int xCands = 0;
		int i = 0;
		do
			xCands |= extraCells[i].maybes;
		while (++i < numExtraCells);
		xCands &= ~cands;
		assert VSIZE[xCands] > 0;
		// Type 2 has ONE extra value, but more could be a Hidden Rectangle
		if ( VSIZE[xCands] == 1 ) {
			final int xValue = VFIRST[xCands];
			final Idx xBuds = Cells.commonBuddiesNew(extraCells, numExtraCells)
								   .and(idxs[xValue]);
			if ( xBuds.any() )
				return new URT2Hint(grid, this, loop, n, v1, v2
						, new Pots(xBuds, grid.maybes, xValue)
						, Arrays.copyOf(extraCells, numExtraCells)
						, xValue);
		} else if ( n == 4 )
			return seekHiddenRectangle(v1, v2, loop, n);
		return null;
	}

	// This code relies on the rather odd A,B,D,C order! loop contains A,B,D,C
	// and thats how it will always be, coz we start da search from A (a bivalue
	// cell), then find B in the same box, then D in the same row-or-col, and
	// finally C in the same col-or-row.
	// NB: There is 468 HiddenURTs in top1465, so it is well worth the work.
	private AURTHint seekHiddenRectangle(final int v1, final int v2
			, final Cell[] loop, final int n) {
//		final Cell A = loop[0]; // the start cell
		final Cell B = loop[1];
		final Cell D = loop[2]; // NOTE: D and C out of order (above).
		final Cell C = loop[3];
		// A is a or b only, B and C require 1-or-more extra values;
		// and D has no constraint (it is allowed 0-or-more extra values).
		if ( B.size>2 && C.size>2 ) {
			// Are B and D the only places for 'l' (v1) in a common region;
			//         OR (the only places for 'l' (v2) in a common region
			//             and if so we swap v1 and v2, so l=v2 and e=v1)
			// we dont know, yet, which is v1 and v2, so use variable variables
			int l = 0 // the locked-in value
			  , e = 0 // the eliminatable value
			  , rti = 0;
			ARegion r, base = null;
			do
				if ( (r=B.regions[rti]) == D.regions[rti] ) {
					// we know that both B and D maybe v1,
					// so check that r has ONLY these two places for v1.
					if ( r.numPlaces[v1] == 2 ) {
						base = r;
						l = v1;
						e = v2;
						break;
					}
					// we know that both B and D maybe v2,
					// so check that r has ONLY two places for v2.
					if ( r.numPlaces[v2] == 2 ) {
						base = r;
						l = v2;
						e = v1;
						break;
					}
				}
			while (++rti < NUM_REGION_TYPES);
			if ( base != null ) {
				// are C and D the only places for 'l' in a common region
				rti = 0;
				do
					if ( (r=C.regions[rti]) == D.regions[rti]
					  // we know that both C and D maybe 'l', so just check
					  // that r has ONLY these two places for 'l'.
					  && r.numPlaces[l] == 2 )
						return new URTHiddenHint(grid, this, loop, n, l, e
								, new Pots(D.indice, e), base, r);
				while (++rti < NUM_REGION_TYPES);
			}
		}
		return null;
	}

	// unusually, Type 3 returns multiple hints in a new List, which may be
	// null but never empty; hence hintSet.addAll handles nulls (just for me).
	private Hints seekType3(final int v1, final int v2, final Cell c1
			, final Cell c2, final Cell[] loop, final int n) {
		ARegion r;
		int size;
		// Get the extra (not v1 or v2) values from c1 and c2.
		final int numRmvCands = 7 - numExtraCands; //number of removable values
		// we seek Naked and Hidden Set hints (collection created on demand).
		Hints hints = null;
		// foreach region that contains both c1 and c2
		int rti = 0;
		do
			// if c1 and c2 share a region of this type
			if ( (r=c1.regions[rti]) == c2.regions[rti] ) {
				// foreach size: the degree that walks from 2..5
				size = numExtraCands;
				do {
					// look for Naked Sets
					if ( size*2 <= r.emptyCellCount )
						hints = seekNakedSets(size, c1, c2, r, loop, n, v1
								, v2, hints);
					// look for Hidden Sets
					if ( size*2<r.emptyCellCount && size-3<numRmvCands )
						hints = seekHiddenSets(size, numRmvCands, c1, c2, r
								, loop, n, v1, v2, hints);
				} while (++size < MAX_SET_SIZE);
			}
		while (++rti < NUM_REGION_TYPES);
		return hints;
	}

	/**
	 * Look for Naked Sets of size in this URT at Cells c1 and c2.
	 * <p>
	 * Examine each combo of size cells in region including c1 but not c2.
	 * <p>
	 * Yes, this s__t IS confusing. Do not panic (yet).
	 *
	 * @param size the number of cells in the Naked Set we seek
	 * @param c1 is the first cell with extra values in this URT
	 * @param c2 is the second cell with extra values in this URT
	 * @param r region to search, which is common to c1 and c2
	 * @param loop the Cells in this URT
	 * @param n number of cells in this URT
	 * @param v1 the first value common to all cells in this URT
	 * @param v2 the second value common to all cells in this URT
	 * @param hints the {@code Hints} to which I add hints.
	 */
	private Hints seekNakedSets(final int size, final Cell c1, final Cell c2
			, final ARegion r, final Cell[] loop, final int n, final int v1
			, final int v2, Hints hints) {
		Cell cell;
		int permCands, i, cnt, nsCands;
		final int i1=r.indexOf(c1), i2=r.indexOf(c2);
		final int baseCands = extraCands & c1.maybes & c2.maybes;
		final Cell[] otherCells = new Cell[size - 1];
		final int[] maybes = new int[size];
		for ( int[] p : new Permuter().permute(REGION_SIZE, new int[size]) ) {
			if ( has1ButNot2(p, i1, i2) ) {
				permCands = baseCands;
				i = 0;
				cnt = 0;
				do {
					if ( p[i] == i1 )
						maybes[i] = extraCands;
					else {
						otherCells[cnt++] = cell = r.cells[p[i]];
						permCands |= (maybes[i]=cell.maybes);
					}
				} while (++i < size);
				assert cnt == otherCells.length;
				if ( VSIZE[permCands] == size
				  && (nsCands=aggregate(maybes, size)) > 0 ) // 9bits
					hints = addType3NakedSetHint(loop, n, v1, v2, r, c1, c2
							, otherCells, nsCands, hints);
			}
		}
		return hints;
	}

	private Hints addType3NakedSetHint(final Cell[] loop, final int n
			, final int v1, final int v2, final ARegion r, final Cell c1
			, final Cell c2, final Cell[] otherCells, final int nsCands
			, Hints hints) {
		int mine;
		final boolean[] excluded = new boolean[GRID_SIZE];
		excluded[c1.indice] = excluded[c2.indice] = true;
		for ( Cell c : otherCells )
			excluded[c.indice] = true;
		Pots reds = null;
		for ( Cell c : r.cells )
			if ( (mine=c.maybes & nsCands)!=0 && !excluded[c.indice] ) {
				if(reds==null) reds = new Pots();
				reds.put(c.indice, mine);
			}
		if ( reds != null ) {
			if(hints==null) hints = new Hints();
			hints.add(new URT3NakedSetHint(grid, this, loop, n, v1, v2, reds
					, c1, c2, extraCands, r, otherCells, nsCands));
		}
		return hints;
	}

	/**
	 * The chookForGliddenSluts method searches for hidden sets involving the
	 * {@code extraValues}.
	 * <p>
	 * It also has a lube fetish, and loves to beat Catholic priests "most
	 * roughly" with PVC poultry. Tune in next week when we wewease the Gimp,
	 * and Davo strips-down to a Naked Set. Now THAT is unique! Rectangles just
	 * $4.95/min. You cannot afford a loop. Davo! "Bring out the gimp!"
	 *
	 * @param N is the setSize: the size of the hidden set to look for
	 * @param numRmvCands is only used as Permutations nBits parameter: the
	 *  size of the master list
	 * @param c1 the first corner with extra (more than 2) values
	 * @param c2 the second corner with extra (more than 2) values
	 * @param r ARegion to examine which contains both c1 and c2
	 * @param loop the corners/cells of this rectangle/loop
	 * @param n the number of cells in the loop
	 * @param v1 the first value in all 4-or-whatever corners
	 * @param v2 the second value in all 4-or-whatever corners
	 * @param hints {@code Collection<AURTHint>} to which we add hints
	 * @return the Hints, which may be created on the fly
	 */
	private Hints seekHiddenSets(final int N, final int numRmvCands
			, final Cell c1, final Cell c2, final ARegion r, final Cell[] loop
			, final int n, final int v1, final int v2, Hints hints) {
		int hsCands, i, hsIndexes, indexes;
		boolean ok;
		final int[] rPlaces = r.places;
		final int i2 = ISHFT[r.indexOf(c2)];
		final int[] rmvCands = VSHIFTED[BITS9 & ~extraCands & ~cands];
		final int L = N - 2;
		final int[] pa = new int[L];
		for ( int[] p : new Permuter().permute(numRmvCands, pa) ) {
			hsCands = cands;
			for ( i=0; i<L; ++i ) {
				hsCands |= rmvCands[p[i]];
			}
			ok = true;
			hsIndexes = 0;
			for ( int v : VALUESES[hsCands] ) {
				if ( (indexes=rPlaces[v] & ~i2) == 0 ) {
					ok = false;
					break;
				}
				hsIndexes |= indexes;
			}
			if ( ok && ISIZE[hsIndexes]==N ) {
				// Hidden set found, but does it remove any maybes?
				hints = addType3HiddenSetHint(loop, n, v1, v2, hsCands
						, r, c1, c2, hsIndexes, hints);
			}
		}
		return hints;
	}

	private Hints addType3HiddenSetHint(final Cell[] loop, final int n
			, final int v1, final int v2, final int hsCands, final ARegion r
			, final Cell c1, final Cell c2, int hsIndexes, Hints hints) {
		Cell c;
		int mine;
		hsIndexes &= ~ISHFT[r.indexOf(c1)];
		hsIndexes &= ~ISHFT[r.indexOf(c2)];
		Pots reds = null;
		for ( int i : INDEXES[hsIndexes] )
			if ( (mine=(c=r.cells[i]).maybes & ~hsCands) > 0 ) { // 9bits
				if(reds==null) reds = new Pots();
				reds.put(c.indice, mine);
			}
		if ( reds != null ) {
			if(hints==null) hints = new Hints();
			hints.add(new URT3HiddenSetHint(grid, this, loop, n, v1, v2
				, reds, c1, c2, extraCands, hsCands, r, hsIndexes));
		}
		return hints;
	}

/*
The first URT Type 4
13#top1465.d5.mt	.4.3..9...7...5.2.8........6.....352..5....9...4...6.....9....8....42.7..9.16....
.4.3..9...7...5.2.8........6.....352..5....9...4...6.....9...68....42.79.9.16....
125,,126,,278,1678,,18,567,139,,1369,46,189,,48,,146,,1256,1269,2467,1279,14679,457,34,34567,,18,1789,478,1789,14789,,,,127,1238,,2678,1238,1368,478,,147,1279,1238,,2578,2359,13789,,18,17,1247,125,127,,357,37,1245,,,135,568,1368,58,,,15,,,2457,,278,,,78,245,34,345
24   	        481,100	25	 182	  2	Unique Rectangle              	Unique Rectangle type 4: H3, I3, I9, H9 on 3 and 4 (I3-4, I9-4)
*/
	/**
	 * Find a region in which c1 and c2 are the only places for v1 and v2.
	 * nb: Type 4 includes Uniqueness Test 6.
	 *
	 * @param loop
	 * @param n
	 * @param c1
	 * @param c2
	 * @param v1
	 * @param v2
	 * @return
	 */
	private AURTHint seekType4(final int v1, final int v2, final Cell c1
			, final Cell c2, final Cell[] loop, final int n) {
		ARegion r, r1=null, r2=null;
		int cnt = 0;
		tmp1.set(extraCells, 2); // extraCells is where we got c1 and c2 from
		int rti = 0;
		do // BOX,ROW,COL
			if ( (r=c1.regions[rti]) == c2.regions[rti] ) {
				if(!tmp2.setAndNotAny(r.idxs[v1], tmp1)) { r1=r; ++cnt; }
				if(!tmp2.setAndNotAny(r.idxs[v2], tmp1)) { r2=r; ++cnt; }
			}
		while (++rti < NUM_REGION_TYPES);
		if ( cnt != 1 )
			return null; // nothing to see here. Move along!
		// ONE of r1/r2 is set, and terniaries are fast enough here.
		final ARegion region = r1!=null ? r1 : r2;
		final int lockValue = r1!=null ? v1 : v2;
		final int vToRemove = r1!=null ? v2 : v1;
		final Pots reds = new Pots(vToRemove, c1, c2);
		return new URT4Hint(grid, this, loop, n, lockValue, vToRemove, reds
				, c1, c2, region);
	}

	private void clearLoops() {
		if ( numLoops > 0 ) {
			numLoops = 0;
			Arrays.fill(loops, null);
			Arrays.fill(loopSizes, 0);
		}
	}

	private void clearExtraCells() {
		if ( numExtraCells > 0 ) {
			numExtraCells = 0;
			Arrays.fill(extraCells, null);
		}
	}

	private void clearWorkLoop() {
		if ( wlSize > 0 ) {
			wlSize = 0;
			Arrays.fill(workLoop, null);
			Arrays.fill(isInWorkLoop, false);
		}
	}

	// Bad number (presumably 0) of extra cells! The Sudoku has 2 solutions so
	// is not my problem, so just log it. I think this CANNOT happen coz grid
	// has 1 solution to get here, so chase it down.
	// BUT this went-off in generate, and I will BFIIK, so I ignore it!
	private int carp(final int v1, final int v2, final Cell cell
			, final Cell[] loop, final int n) {
		if ( !Run.isGenerator() ) {
			Log.teeln("\n"+tech.name()+" disabled for this puzzle.");
			Log.teeln("The extra \"rescue\" cells must exist, or the puzzle is invalid.");
			Log.teeln("More likely URT has a bug, which needs fixing. Sigh.");
			Log.teeln("Bad numExtraCells = "+numExtraCells+" (presumably 0)");
			Log.teeln("extraCells = "+Frmu.toFullString(", ", Math.min(numExtraCells,LOOP_SIZE), extraCells));
			Log.teeln("loop = "+Frmu.toFullString(", ", n, loop));
			Log.teeln("cell = "+cell.toFullString());
			Log.teeln("v1 = "+v1);
			Log.teeln("v2 = "+v2);
			Log.teeln("\n"+grid+"\n");
			Log.teeTrace(new Throwable());
			Log.teeln();
			// for LogicalSolverTester
			beep();
		}
		// UniqueRectange is ____ed, so any hints are dodgy, so:
		// 1. disable this hinter for this puzzle
		setIsEnabled(false);
		// 2. ignore any/all existing hints.
		return 0;
	}

	// Retaining a single Grid/Cell/ARegion holds the whole Grid!
	// nb: called "finally" at the end of getHints.
	private void clean() {
		startCell = null;
		list.clear();
		clearExtraCells();
		clearWorkLoop();
		clearLoops();
	}

	// used only within seekType3, rather than create a results-list when
	// none is required because no hint is found 99.93% of the time.
	final static class Hints extends LinkedList<AURTHint> {
		private static final long serialVersionUID = 10948321L;
		/** Note that add MUST ignore nulls! */
		@Override
		public boolean add(AURTHint h) {
			return h!=null && super.add(h);
		}
	}

}
