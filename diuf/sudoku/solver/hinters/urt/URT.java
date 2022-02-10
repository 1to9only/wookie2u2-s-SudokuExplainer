/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.*;
import diuf.sudoku.Cells.CALease;
import static diuf.sudoku.Cells.caLease;
import diuf.sudoku.Grid.*;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Indexes.*;
import diuf.sudoku.IntArrays.IALease;
import static diuf.sudoku.IntArrays.iaLease;
import static diuf.sudoku.Values.*;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.Permutations;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * URT UniqueRecTangle implements the Unique Rectangles and loops Sudoku
 * solving techniques. Supports types 1 to 4 and Hidden. Skewed non-orthogonal
 * loops (fairly rare) are also detected. A rectangle is a loop of 4 cells.
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
//	private long[] COUNTS = new long[8];

	// maximum number of cells in a workLoop
	private static final int LOOP_SIZE = 16; // guess
	// maximum number of loops in the loops array
	private static final int MAX_LOOPS = 16; // observed 12

	/**
	 * Combine all of the maybes in the maybes array into one aggregate value
	 * (they're bitsets, so just {@code aggregate |= maybe}), and if there's
	 * exactly $size of them return them, else return 0. I also return 0 if
	 * {@code VSIZE[maybe] < 2}, to suppress any naked singles.
	 *
	 * @param maybes
	 * @param size
	 * @return
	 */
	private static int aggregate(final int[] maybes, final int size) {
		int aggregate = 0;
		for ( int maybe : maybes ) {
			if ( VSIZE[maybe] < 2 )
				return 0;
			aggregate |= maybe;
		}
		if ( VSIZE[aggregate] != size )
			return 0;
		return aggregate;
	}

	/**
	 * Does this permutation of indexes contain i1 but NOT i2?
	 * These indexes are indexes in region.cells
	 *
	 * @param perm
	 * @param i1
	 * @param i2
	 * @return
	 */
	private static boolean has1ButNot2(final int[] perm, final int i1, final int i2) {
		assert i1 != i2;
		boolean found1 = false;
		for ( int i=0,n=perm.length; i<n; ++i )
			if ( perm[i] == i1 )
				found1 = true;
			else if ( perm[i] == i2 )
				return false;
		return found1;
	}

	// evens and odds are created ONCE, rather than in each isValid call.
	// Note that this is (currently) the only use of the RegionSet class
	private static final RegionSet EVENS = new RegionSet();
	private static final RegionSet ODDS = new RegionSet();
	/**
	 * Is workLoop actually a loop, ie is each region of each cell visited by
	 * exactly two cells of opposing parity.
	 * <pre>
	 * isLoop builds two parity-sets: evens and odds.
	 * The first cell is even, the second is odd, and so on around the loop.
	 * If this parity has already seen this region then it's NOT a loop.
	 * Then we check that the two parity-sets contain the same regions.
	 * </pre>
	 *
	 * @param workLoop the cells in the purported loop
	 * @param n the number of cells in workLoop
	 * @return is workLoop actually a loop
	 */
	private static boolean isLoop(final Cell[] workLoop, final int n) {
		boolean result = true;
		try {
			for ( int i=0; i<n; ++i )
				if ( !(i%2==0 ? EVENS : ODDS).addAll(workLoop[i].regions) ) {
					// nb: this because I "doubt" return in a finally block. I
					// know that's silly coz the Java spec says it's fine, but
					// I don't trust the bastards implementing the spec. There
					// is all sorts of numpty going-on under the hood. I hear
					// they've even hired GUI programmers. I think it best to
					// minimise ones exposure. Sunscreen.
					result = false;
					break;
				}
			if ( result )
				// each region has been visited once in each parity (or never)
				// so return do both parities have all the same regions?
				result = EVENS.equals(ODDS);
		} finally {
			EVENS.clear(); ODDS.clear(); // for next time
		}
		return result;
	}

	// ============================ INSTANCE STUFF ============================

	// a LinkedHashSet whose add ignores null or pre-existing hint
	private final Set<AURTHint> hintsSet = new LinkedHashSet<AURTHint>(16, 1F) {
		private static final long serialVersionUID = 20145039L;
		// HashSet.contains is definitely fast enough, for any number of hints.
		@Override
		public boolean add(AURTHint h) {
			return h!=null && !super.contains(h) && super.add(h);
		}
		// ignore the null collection
		@Override
		public boolean addAll(Collection<? extends AURTHint> c) {
			return c!=null && super.addAll(c);
		}
	};

	// startCell must be a field for correctness, not just for prevention of
	// garbage creation; alternatively you could check that a loop-is-a-loop
	// by retrieving the startCell from the loop (it's index 0)
	private Cell startCell = null;
	// v1andV2 is a bitset containing v1 and v2, the two maybes of the
	// startCell. It's a field rather than parameters because it doesn't
	// change during recursion, so there's no point in passing it (or v1
	// and v2) down the stack.
	private int cands;
	// the number of cells in the workLoop
	private int workLoopSize;
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
	private final Cell[] extraCells = new Cell[LOOP_SIZE]; // 4 isn't enough

	// the current grid
	private Grid grid;
	// the indices of each potential value 1..9 in the current grid
	private Idx[] idxs;

	private int extraCands, numExtraCands;

	public URT() {
		super(Tech.UniqueRectangle);
		for ( int i=0; i<MAX_LOOPS; ++i )
			loopsIdxs[i] = new Idx();
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
		if ( workLoopSize > 0 ) {
			workLoopSize = 0;
			Arrays.fill(workLoop, null);
			Arrays.fill(isInWorkLoop, false);
		}
	}

	// Retaining a single Grid/Cell/ARegion holds the whole Grid!
	// nb: called "finally" at the end of getHints.
	private void clean() {
		grid = null;
		idxs = null;
		startCell = null;
		hintsSet.clear();
		clearExtraCells();
		clearWorkLoop();
		clearLoops();
	}

	// runs after puzzle loaded into grid, and before the first findHints.
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		setIsEnabled(true); // just incase I disable myself in carp.
	}

	@Override // IHinter via AHintNumberActivatableHinter
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		assert grid != null;
		assert accu != null;
		assert hintsSet.isEmpty(); // cleared in clean, even upon exception
		final boolean result;
		try {
			this.grid = grid;
			this.idxs = grid.idxs;
			final int n = search(); // returns number of hints found
			if ( result = n > 0 ) {
				if ( n == 1 ) {
					accu.add(hintsSet.iterator().next());
				} else {
					final AURTHint[] hints = hintsSet.toArray(new AURTHint[n]);
					Arrays.sort(hints, AURTHint.BY_ORDER);
					for ( AURTHint hint : hints )
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
	private int search() {
		Cell[] loop;
		Cell c1, c2;
		int n, v1, v2, i, j;
		for ( Cell cell : grid.cells ) {
			if ( cell.size == 2 ) {
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
				// nb: startCell is a field, so doesn't change upon recursion.
				// param cell = the current cell in this loop. Always the same
				//       as startCell, so we add a loop when length > 3 and we
				//       've worked our way back to the startCell, recursively.
				// param prevRTI = -1 is invalid, meaning none: there is no
				//       previous region type for the first call.
				// param extraCands = 0 means none. Each recursion has its own
				//       extraCands variable, localising it, so when we pop-up
				//       out of each recursion extraCands returns to what it
				//       was when we were last at this level in the callstack,
				//       which is devilishly clever. I like it.
				// param x = 2 means atmost two cells with extra values. It's a
				//       parameter because it's decremented in the descent, but
				//       returns to its previous value in each ascent
				if ( recurse(cell, -1, 0, 2) ) {
					// look for hints in the loops
					for ( i=0; i<numLoops; ++i ) {
						loop = loops[i];
						n = loopSizes[i];
						// get cells in loop with extra (not v1 or v2) values.
						// It's a field so no recreate, and no pass around.
						// nb: all cells in loop maybe v1 and v2
						clearExtraCells();
						for ( j=0; j<n; ++j )
							if ( loop[j].size > 2 )
								extraCells[numExtraCells++] = loop[j];
						// type of URT possible mostly depends on numExtraCells
						if ( numExtraCells == 1 )
							// FOUND Type 1
							hintsSet.add(new URT1Hint(this, loop, n, v1, v2
									, new Pots(extraCells[0], cands, DUMMY)));
						else if ( numExtraCells == 2 ) {
							// Type 2, 3 or 4 is possible
							c1 = extraCells[0];
							c2 = extraCells[1];
							// get extra (not v1 or v2) candidates, and count
							extraCands = (c1.maybes | c2.maybes) & ~cands;
							numExtraCands = VSIZE[extraCands];
							if ( numExtraCands == 1 ) // try Type 2
								hintsSet.add(lookForType2OrHidden(v1, v2, loop, n));
							else if ( numExtraCands >= 2 ) // try Type 3
								hintsSet.addAll(lookForType3(v1, v2, c1, c2, loop, n));
							// try Type 4
							hintsSet.add(lookForType4(v1, v2, c1, c2, loop, n));
						} else if ( numExtraCells > 2 )
							// only Type 2 or Hidden Unique Rectangle possible
							hintsSet.add(lookForType2OrHidden(v1, v2, loop, n));
						else
							return carp(v1, v2, cell, loop, n);
					}
				}
			}
		}
		// 95+% of calls fail, so do NOT create a new empty ArrayList. Putz!
		return hintsSet.size();
	}

	// Bad number (presumably 0) of extra cells! The Sudoku has 2 solutions so
	// is not my problem, so just log it. I think this CANNOT happen coz grid
	// has 1 solution to get here, so chase it down.
	// BUT this went-off in generate, and I'll BFIIK, so I ignore it!
	private int carp(final int v1, final int v2, final Cell cell, final Cell[] loop, final int n) {
		if ( Run.type != Run.Type.Generator ) {
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
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
		// UniqueRectange is ____ed, so any hints are dodgy, so:
		// 1. disable this hinter for this puzzle
		setIsEnabled(false);
		// 2. ignore any/all existing hints.
		return 0;
	}

	/**
	 * recurseLoops recursively finds all loops from the given start cell which
	 * maybe both v1 and v2. Note that the top-call is for each startCell that
	 * maybe EXACTLY v1 and v2, which defines v1 and v2. At most two cells in a
	 * loop may have extra (not v1 or v2) values, or 2-or-more-cells may share
	 * a-single-common-extra-value.
	 * <p>
	 * Each recurseLoops-call searches the regions of "the current cell" for
	 * the next current-cell, and so-on, until we find ourselves back at the
	 * startCell, forming a loop, which we add to the loops array. So the
	 * workLoop is only a POTENTIAL loop, until we get back to the startCell.
	 * <p>
	 * Don't panic! This is pretty complex, so be patient with yourself. You
	 * WILL understand it eventually. It's just a bit tricky, that's all.
	 *
	 * @param cc current cell: the current "corner" of the potential loop to be
	 *  extended. Each cell in the grid which has exactly 2 potential values
	 *  becomes a startCell. We extend the potential loop by finding the next
	 *  corner (this variable); and then I recursively call myself to find the
	 *  next "corner", and so-on, until we get back to the startCell. Note that
	 *  the startCell is a field, so it does not change during recursion.
	 * @param prevRti previous region type index: the type of the region shared
	 *  by the current cell and the cell in the previous recursion, which is
	 *  skipped in the search for the next cell, to stop infinite recursion
	 *  caused by repeatedly searching back-and-forth in a single region.
	 *  prevRti starts as -1 meaning "none".
	 * @param extraCands extra values in the current loop as a bitset. Always
	 *  starts as 0 (meaning empty) because each startCell has two maybes: v1
	 *  and v2, then each corner that maybe v1 and v2 which also has extra
	 *  (3-or-more) maybes donates to extraCands. There can be at most 2 cells
	 *  in a loop with extra values OR there is only one extra value, ie ALL
	 *  (maybe more than 2) cells with extraCands share a single extra value.
	 *  There are always cells having only v1 and v2, the startCell being one.
	 *  NB: extraCands is a parameter (a local) which reverts as we ascend back
	 *  up the recursive callstack, automatically going back-to the callers
	 *  variable with it's existing value (unchanged by recursion). This
	 *  behaviour is required for algorithmic correctness (which I found more
	 *  than a little confusing, until it came clear, overnight, sigh).
	 * @param x numExtraValueCellsRemaining: the remaining number of cells with
	 *  extraCands allowed in the loop. nXs starts at 2, and is decremented as
	 *  we extend the loop, while it stays above 0 (or VSIZE[extraVs]==1).
	 */
	private boolean recurse(final Cell cc, final int prevRti, int extraCands
			, final int x) {
		// ANSI-C style vars, so recurse has ONE stackframe, for speed.
		// Note that recurse is ____ing hammered, so be uberspeedy here.
		ARegion r; // the cc.region we explore for the next cell to add to loop
		Cell c; // the next cell we examine to add to loop
		int rti, ci; // regionTypeIndex, cellIndex
		workLoop[workLoopSize++] = cc;
		isInWorkLoop[cc.i] = true;
		// presume that no loops will be found
		boolean result = false;
		for ( rti=0; rti<NUM_REGION_TYPES; ++rti ) { // BOX, ROW, COL
			// skip if the current rti equals the previous region type
			if ( (r=cc.regions[rti]).typeIndex != prevRti ) {
				for ( ci=0; ci<REGION_SIZE; ++ci ) {
					if ( (c=r.cells[ci])==startCell && workLoopSize>3 ) {
						// loop closed: store if valid and not already exists.
						if ( isLoop(workLoop, workLoopSize)
						  && !exists(workLoopIdx.set(workLoop, workLoopSize)) ) {
							loopsIdxs[numLoops].set(workLoopIdx);
							loopSizes[numLoops] = workLoopSize;
							loops[numLoops++] = Arrays.copyOf(workLoop, workLoopSize);
							result = true;
						}
					// else if cell may be both v1 and v2
					//      && cell is NOT already in the workLoop
					} else if ( (c.maybes & cands)==cands && !isInWorkLoop[c.i] ) {
						// add c.maybes to my extraCands, to build-up as we go
						extraCands |= c.maybes & ~cands;
						// continue this search:
						// if cell has two maybes: v1 and v2 (no extras);
						// or cell has extra values and the maximum number of
						//    cells with extra values (2) is not exceeded;
						// or cell has 1 extra value, the same as all previous
						//    cells with an extra value (for type 2 only)
						// or the bastard is a Hidden Rectangle
						if ( VSIZE[c.maybes]==2 || x>0 || VSIZE[extraCands]==1
						  // * Hidden Rectangles are 4 cells (no loops) having
						  //   atleast one extraCands (and we do da rest later)
						  || (workLoopSize<4 && VSIZE[extraCands]>0) ) {
							// add c to loop, and then examine it's regions
							// looking for the next cell to add to the loop,
							// and so on.
							result |= recurse(c, r.typeIndex, extraCands
									, VSIZE[c.maybes]>2 ? x-1 : x);
						}
					}
				}
			}
		}
		// rollback
		isInWorkLoop[cc.i] = false;
		--workLoopSize;
		return result;
	}

	private boolean exists(final Idx workLoopIdx) {
		for ( int i=0; i<numLoops; ++i )
			if ( workLoopIdx.allIn(loopsIdxs[i]) )
				return true;
		return false;
	}

	// I handle Type 2, and flick-pass HiddenURT
	private AURTHint lookForType2OrHidden(final int v1, final int v2
			, final Cell[] loop, final int n) {
		int xCands = 0;
		for ( int i=0; i<numExtraCells; ++i )
			xCands |= extraCells[i].maybes;
		xCands &= ~cands;
		assert VSIZE[xCands] > 0;
		// Type 2 has ONE extra value, but more could be a Hidden Rectangle
		if ( VSIZE[xCands] == 1 ) {
			final int xValue = VFIRST[xCands];
			final Idx xBuds = Cells.commonBuddiesNew(extraCells, numExtraCells)
								   .and(idxs[xValue]);
			if ( xBuds.any() )
				return new URT2Hint(this, loop, n, v1, v2
						, new Pots(xBuds, grid, xValue)
						, Arrays.copyOf(extraCells, numExtraCells)
						, xValue);
		} else if ( n == 4 )
			return lookForHiddenRectangle(v1, v2, loop, n);
		return null;
	}

	// This code relies on the rather odd A,B,D,C order! loop contains A,B,D,C
	// and that's how it'll always be, coz we start da search from A (a bivalue
	// cell), then find B in the same box, then D in the same row-or-col, and
	// finally C in the same col-or-row.
	// NB: There's 468 HiddenURT's in top1465, so it's well worth the work.
	private AURTHint lookForHiddenRectangle(final int v1, final int v2
			, final Cell[] loop, final int n) {
//		final Cell A = loop[0]; // the start cell
		final Cell B = loop[1];
		final Cell D = loop[2]; // NOTE: D and C out of order (above).
		final Cell C = loop[3];
		// A is a or b only, B and C require 1-or-more extra values;
		// and D has no constraint (it's allowed 0-or-more extra values).
		if ( B.size>2 && C.size>2 ) {
			// Are B and D the only places for 'l' (v1) in a common region;
			//         OR (the only places for 'l' (v2) in a common region
			//             and if so we swap v1 and v2, so l=v2 and e=v1)
			// we dont know, yet, which is v1 and v2, so use variable variables
			int l = 0  // the locked-in value
			  , e = 0; // the eliminatable value
			ARegion r, base = null;
			for ( int rti=0; rti<NUM_REGION_TYPES; ++rti )
				if ( (r=B.regions[rti]) == D.regions[rti] ) {
					// we know that both B and D maybe v1,
					// so check that r has ONLY these two places for v1.
					if ( r.ridx[v1].size == 2 ) {
						base = r;
						l = v1;
						e = v2;
						break;
					}
					// we know that both B and D maybe v2,
					// so check that r has ONLY two places for v2.
					if ( r.ridx[v2].size == 2 ) {
						base = r;
						l = v2;
						e = v1;
						break;
					}
				}
			if ( base != null )
				// are C and D the only places for 'l' in a common region
				for ( int rti=0; rti<NUM_REGION_TYPES; ++rti )
					if ( (r=C.regions[rti]) == D.regions[rti]
					  // we know that both C and D maybe 'l', so just check
					  // that r has ONLY these two places for 'l'.
					  && r.ridx[l].size == 2 )
						return new URTHiddenHint(this, loop, n, l, e
								, new Pots(D, e), base, r);
		}
		return null;
	}

	// all naked/hidden sets of more than 4 cells are degenerate: they are the
	// combination of simpler hints, so SE never finds them, so no look for em
	private static final int MAX_SET_SIZE = 5;

	// unusually, Type 3 returns multiple hints in a new List, which may be
	// null but never empty; hence hintSet.addAll handles nulls (just for me).
	private List<AURTHint> lookForType3(final int v1, final int v2
			, final Cell c1, final Cell c2, final Cell[] loop, final int n) {
		ARegion r;
		// Get the extra (not v1 or v2) values from c1 and c2.
		final int numRmvCands = 7 - numExtraCands; //number of removable values
		// we seek Naked and Hidden Set hints (collection created on demand).
		HintsList hints = null;
		// foreach region that contains both c1 and c2
		for ( int rti=0; rti<NUM_REGION_TYPES; ++rti )
			// if c1 and c2 share a region of this type
			if ( (r=c1.regions[rti]) == c2.regions[rti] )
				// foreach size: the degree that walks from 2..5
				for ( int size=numExtraCands; size<MAX_SET_SIZE; ++size ) {
					// look for Naked Sets
					if ( size*2 <= r.emptyCellCount )
						hints = lookForNakedSets(size, c1, c2, r, loop, n, v1
								, v2, hints);
					// look for Hidden Sets
					if ( size*2<r.emptyCellCount && size-3<numRmvCands )
						hints = lookForHiddenSets(size, numRmvCands, c1, c2, r
								, loop, n, v1, v2, hints);
				}
		return hints;
	}

	/**
	 * Look for Naked Sets of size in this URT at Cells c1 and c2.
	 * <p>
	 * Examine each combo of size cells in region including c1 but not c2.
	 * <p>
	 * Yes, this s__t IS confusing. Don't panic (yet).
	 *
	 * @param size the number of cells in the Naked Set we seek
	 * @param c1 is the first cell with extra values in this URT
	 * @param c2 is the second cell with extra values in this URT
	 * @param r region to search, which is common to c1 and c2
	 * @param loop the Cells in this URT
	 * @param n number of cells in this URT
	 * @param v1 the first value common to all cells in this URT
	 * @param v2 the second value common to all cells in this URT
	 * @param hints the {@code HintsList} to which I add hints.
	 */
	private HintsList lookForNakedSets(final int size, final Cell c1
			, final Cell c2, final ARegion r, final Cell[] loop, final int n
			, final int v1, final int v2, HintsList hints) {
		Cell cell;
		int permCands, i, cnt, nsCands;
		final int i1=r.indexOf(c1), i2=r.indexOf(c2);
		final int baseCands = extraCands & c1.maybes & c2.maybes;
		try ( IALease paLease = iaLease(size);
			  IALease mLease = iaLease(size);
			  CALease cLease = caLease(size - 1) ) {
			final Cell[] otherCells = cLease.array;
			final int[] maybes = mLease.array;
			for ( int[] perm : new Permutations(REGION_SIZE, paLease.array) )
				if ( has1ButNot2(perm, i1, i2) ) {
					permCands = baseCands;
					for ( i=0,cnt=0; i<size; ++i )
						if ( perm[i] == i1 ) {
							maybes[i] = extraCands;
						} else {
							otherCells[cnt++] = cell = r.cells[perm[i]];
							permCands |= (maybes[i]=cell.maybes);
						}
					assert cnt == otherCells.length;
					if ( VSIZE[permCands] == size
					  && (nsCands=aggregate(maybes, size)) != 0 )
						hints = addType3NakedSetHint(loop, n, v1, v2, r, c1, c2
								, otherCells, nsCands, hints);
				}
		}
		return hints;
	}

	private HintsList addType3NakedSetHint(final Cell[] loop, final int n
			, final int v1, final int v2, final ARegion r, final Cell c1
			, final Cell c2, final Cell[] otherCells, final int nsCands
			, HintsList hints) {
		int mine;
		final boolean[] excluded = new boolean[GRID_SIZE];
		excluded[c1.i] = excluded[c2.i] = true;
		for ( Cell c : otherCells )
			excluded[c.i] = true;
		final PotHolder reds = new PotHolder();
		for ( Cell c : r.cells )
			if ( (mine=c.maybes & nsCands)!=0 && !excluded[c.i] )
				reds.put(c, mine);
		if ( reds.any ) {
			if ( hints == null )
				hints = new HintsList();
			hints.add(new URT3NakedSetHint(this, loop, n, v1, v2, reds.copyAndClear()
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
	 * $4.95/min. You can't afford a loop. Davo! "Bring out the gimp!"
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
	 * @return the HintsList, which may be created on the fly
	 */
	private HintsList lookForHiddenSets(final int N, final int numRmvCands
			, final Cell c1, final Cell c2, final ARegion r, final Cell[] loop
			, final int n, final int v1, final int v2, HintsList hints) {
		int hsCands, i, hsIndexes, indexes;
		boolean ok;
		final Indexes[] regionIdx = r.ridx;
		final int i2 = ISHFT[r.indexOf(c2)];
		final int[] rmvCands = VSHIFTED[VALL & ~extraCands & ~cands];
		final int L = N - 2;
		try ( IALease lLease = iaLease(L) ) {
			for ( int[] perm : new Permutations(numRmvCands, lLease.array) ) {
				hsCands = cands;
				for ( i=0; i<L; ++i )
					hsCands |= rmvCands[perm[i]];
				ok = true;
				hsIndexes = 0;
				for ( int v : VALUESES[hsCands] ) {
					if ( (indexes=regionIdx[v].bits & ~i2) == 0 ) {
						ok = false;
						break;
					}
					hsIndexes |= indexes;
				}
				if ( ok && ISIZE[hsIndexes]==N )
					// Hidden set found, but does it remove any maybes?
					hints = addType3HiddenSetHint(loop, n, v1, v2, hsCands
							, r, c1, c2, hsIndexes, hints);
			}
		}
		return hints;
	}

	private HintsList addType3HiddenSetHint(final Cell[] loop, final int n
			, final int v1, final int v2, final int hsCands, final ARegion r
			, final Cell c1, final Cell c2, int hsIndexes, HintsList hints) {
		Cell c;
		int mine;
		hsIndexes &= ~ISHFT[r.indexOf(c1)];
		hsIndexes &= ~ISHFT[r.indexOf(c2)];
		final PotHolder reds = new PotHolder();
		for ( int i : INDEXES[hsIndexes] )
			if ( (mine=(c=r.cells[i]).maybes & ~hsCands) != 0 )
				reds.put(c, mine);
		if ( reds.any ) {
			if ( hints == null )
				hints = new HintsList();
			hints.add(new URT3HiddenSetHint(this, loop, n, v1, v2, reds.copyAndClear()
					, c1, c2, extraCands, hsCands, r, hsIndexes));
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
	private AURTHint lookForType4(final int v1, final int v2, final Cell c1
			, final Cell c2, final Cell[] loop, final int n) {
		ARegion r, r1=null, r2=null;
		int cnt = 0;
		tmp1.set(extraCells, 2); // extraCells is where we got c1 and c2 from
		for ( int rti=0; rti<NUM_REGION_TYPES; ++rti ) // BOX,ROW,COL
			if ( (r=c1.regions[rti]) == c2.regions[rti] ) {
				if(!tmp2.setAndNotAny(r.idxs[v1], tmp1)) { r1=r; ++cnt; }
				if(!tmp2.setAndNotAny(r.idxs[v2], tmp1)) { r2=r; ++cnt; }
			}
		if ( cnt != 1 )
			return null; // nothing to see here. Move along!
		// ONE of r1/r2 is set, and terniaries are fast enough here.
		final ARegion region = r1!=null ? r1 : r2;
		final int lockValue = r1!=null ? v1 : v2;
		final int vToRemove = r1!=null ? v2 : v1;
		final Pots reds = new Pots(c1, c2, vToRemove);
		return new URT4Hint(this, loop, n, lockValue, vToRemove, reds, c1, c2
				, region);
	}
	private final Idx tmp1 = new Idx();
	private final Idx tmp2 = new Idx();

	// used only within lookForType3, rather than create a results-list when
	// none is required because no hint is found 99.93% of the time.
	final static class HintsList extends LinkedList<AURTHint> {
		private static final long serialVersionUID = 10948321L;
		/** Note that add MUST ignore nulls! */
		@Override
		public boolean add(AURTHint h) {
			return h!=null && super.add(h);
		}
	}

}
