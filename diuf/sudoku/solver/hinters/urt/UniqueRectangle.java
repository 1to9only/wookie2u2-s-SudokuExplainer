/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.Permutations;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * UniqueRectangle implements the Unique Rectangles and Unique Loops Sudoku
 * solving techniques. Supports types 1 to 4 and Hidden. Skewed non-orthogonal
 * loops (fairly rare) are also detected.
 * <p>
 * NOTE that SE includes URT Types 5 and 6 in lesser hint types, but the Sudoku
 * community has "reserved" 5 and 6, so I use Type 7 to mean a Hidden URT.
 * The distinction of Types 5 and 6 from lesser hint types is unnecessary, so
 * they are dropped from this minimum rule set implementation.
 * <p>
 * Q: Why do people make things more complicated than they need to be?<br>
 * A: Because they did not realise the complications were unnecessary.
 */
public final class UniqueRectangle extends AHinter
		implements diuf.sudoku.solver.hinters.ICleanUp
//				 , diuf.sudoku.solver.IReporter
{
	// Int ArrayS: save creating array in each call (no garbage)
	private static final int[][] IAS1 = new int[REGION_SIZE][];
	private static final int[][] IAS2 = new int[REGION_SIZE][];
	private static final int[][] IAS3 = new int[REGION_SIZE][];
	static {
		for ( int i=0; i<REGION_SIZE; ++i ) {
			IAS1[i] = new int[i];
			IAS2[i] = new int[i];
			IAS3[i] = new int[i];
		}
	}

	// a LinkedHashSet whose add ignores null or pre-existing hint
	private final Set<AURTHint> hintsSet = new LinkedHashSet<AURTHint>(16, 1F) {
		private static final long serialVersionUID = 20145039L;
		@Override
		public boolean add(AURTHint h) {
			// contains fast enough coz it's a low-volume problem.
			// there's only 361 hints in top1465.d5.mt.
			return h!=null && !super.contains(h) && super.add(h);
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
	// working storage for current loop.
	private static final int LOOP_SIZE = 16; // guess 16
	private final Cell[] workLoop = new Cell[LOOP_SIZE];
	private int workLoopSize;
	// fast workLoop.contains(cell)
	private final boolean[] isInWorkLoop = new boolean[GRID_SIZE];
	// the collection of loops found to date.
	private static final int MAX_LOOPS = 16; // guess 16 // observed 12
	private final Cell[][] loops = new Cell[MAX_LOOPS][];
	private final int[] loopSizes = new int[MAX_LOOPS];
	// the number of loops currently in the loops array
	private int numLoops;
	// the cells in this loop with extra (not v1 or v2) values.
	private final Cell[] extraCells = new Cell[4];
	private int numExtraCells;
	// the indices of each potential value 1..9 in the current grid
	private Idx[] idxs;
	// An Idx of the workLoop
	private final Idx workLoopIdx = new Idx();
	// Idx's of the loops
	private final Idx[] loopsIdxs = new Idx[MAX_LOOPS];
	// THE common regions array (re-used rather than create junk collections)
	private final ARegion[] cmnRgns = new ARegion[2];

	public UniqueRectangle() {
		super(Tech.URT);
		for ( int i=0; i<MAX_LOOPS; ++i )
			loopsIdxs[i] = new Idx();
	}

//	@Override // IReporter
//	public void report() {
//		Log.teef("%s\n", this.getClass().getSimpleName());
//		Log.teef("type4Cnt = %,d", type4Cnt);
//	}
//	private int type4Cnt = 0;

	/**
	 * Forget all my s__t and call my supers cleanUp to do likewise there.
	 */
	@Override // ICleanUp
	public void cleanUp() {
		clean();
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

	// nb: called "manually" at the end of getHints.
	private void clean() {
		startCell = null;
		idxs = null;
		hintsSet.clear();
		clearExtraCells();
		clearWorkLoop();
		clearLoops();
		Arrays.fill(cmnRgns, null);
		Cells.cleanCasA();
	}

	@Override // IHinter via AHintNumberActivatableHinter
	public boolean findHints(Grid grid, IAccumulator accu) {
		// get the indices of each potential value 1..9 in this grid ONCE, and
		// stash it in a field for reference later.
		this.idxs = grid.idxs;
		boolean result = false;
		try {
			final ArrayList<AURTHint> list = search(grid);
			if ( list != null ) {
				final int n = list.size();
				if ( result=(n > 0) ) {
					if ( n > 1 ) {
						list.sort(AURTHint.BY_ORDER);
					}
					for ( AURTHint hint : list ) {
						if ( accu.add(hint) ) {
							break;
						}
					}
				}
			}
		} finally {
			// Retaining a single Cell/ARegion holds the whole Grid!
			clean();
		}
		// return
		return result;
	}

	private ArrayList<AURTHint> search(Grid grid) {
		Cell[] loop;
		Cell c1, c2;
		int loopSize, extraVs, numExtraVs;
		this.hintsSet.clear();
		int v1, v2, i, j;
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
				// starting at startCell, find all loops of cells that maybe v1
				// and v2 in grid, which have atmost 2 extra (not v1 or v2) vs.
				// recurseLoops returns "Are there any loops?"
				// nb: startCell is a field, so it doesn't change when
				//     recurseLoops recurses.
				// param cell = the current cell in this loop. Always the same
				//       as startCell, so add a loop when length > 3 and we get
				//       back to the startCell.
				// param prevRTI = -1 is invalid, meaning none: there is
				//       no previous region type for the first call.
				// param extraVs = 0 meaning none, yet. Each descent has it's
				//       own extraVals variable, localising it; so that when we
				//       pop-up out of each recursion extraVals returns to what
				//       it was when we were last at this level in callstack.
				//       This really is devilishly clever. I like it.
				// param x = 2 is essential for algorithmic correctness, it's a
				//       parameter because it's decremented in the descent, but
				//       returns to its previous value in each recursive ascent
				if ( recurseLoops(cell, -1, 0, 2) ) {
					// look for hints in the loops
					for ( i=0; i<numLoops; ++i ) {
						loop = loops[i];
						loopSize = loopSizes[i];
						// get cells in loop with extra (not v1 or v2) values.
						// It's a field so no recreate, and no pass around.
						// nb: all cells in loop maybe v1 and v2
						clearExtraCells();
						for ( j=0; j<loopSize; ++j )
							if ( loop[j].size > 2 )
								extraCells[numExtraCells++] = loop[j];
						// type of URT possible mostly depends on numExtraCells
						if ( numExtraCells == 1 )
							// Try a type-1 hint
							hintsSet.add(createType1Hint(v1, v2, loop, loopSize));
						else if ( numExtraCells == 2 ) {
							// Type 2 or 3 or 4 is possible
							c1 = extraCells[0];
							c2 = extraCells[1];
							// a bitset of the extra values
							extraVs = (c1.maybes|c2.maybes) & ~VSHFT[v1]
									& ~VSHFT[v2];
							// the number of extra values
							numExtraVs = VSIZE[extraVs];
							if ( numExtraVs == 1 ) { // Try type 2 hint
								hintsSet.add(createType2OrHiddenHint(grid
										, v1, v2, loop, loopSize));
							} else if ( numExtraVs >= 2 ) { // Try type 3 hint
								hintsSet.addAll(createMultipleType3Hints(grid
										, v1, v2, c1, c2, loop, loopSize));
							}
							// Try type 4 hint
							hintsSet.add(createType4Hint(loop, loopSize
									, c1, c2, v1, v2));
						} else if ( numExtraCells > 2 ) {
							// only Type 2 or Hidden Unique Rectangle possible
							hintsSet.add(createType2OrHiddenHint(grid, v1, v2
									, loop, loopSize));
						} else {
							badRescue(loop, loopSize, cell, v1, v2, grid);
							return null;
						}
					} // next loop
				}
			}
		} // next cell
		return new ArrayList<>(hintsSet);
	}

	private void badRescue(final Cell[] loop, final int loopSize
			, final Cell cell, final int v1, final int v2, final Grid grid) {
		// Bad number (presumably 0) of rescue cells! This Sudoku has two
		// solutions, so not my problem, so just log it. I think this CAN'T
		// happen, coz grid has 1 solution to get here, so chase these down.
		// This goes off in generate! BFIIK!
		if ( Run.type != Run.Type.Generator ) {
			Log.teeln();
			Log.teef("%s: Bad number of rescue cells!\n", getClass().getSimpleName());
			Log.teeln("numExtraCells = "+numExtraCells);
			Log.teeln("extraCells = "+Frmu.toFullString(", ", numExtraCells, extraCells));
			Log.teeln("loop = "+Frmu.toFullString(", ", loopSize, loop));
			Log.teeln("cell = "+cell.toFullString());
			Log.teeln("v1 = "+v1);
			Log.teeln("v2 = "+v2);
			Log.teeln();
			Log.teeln(grid);
			Log.teeln();
			Log.teeTrace(new Throwable());
			Log.teeln();
			// for LogicalSolverTester
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
	}

	/**
	 * recurseLoops recursively finds all loops from the given start cell which
	 * maybe both v1 and v2. Note that the top-call is for each startCell that
	 * maybe EXACTLY v1 and v2, which defines v1 and v2 (the v1AndV2 bitset).
	 * At most two cells in a loop may have extra (not v1 or v2) values,
	 * or 2-or-more-cells may share a-single-common-extra-value.
	 *
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
	 * @param extraVs extra values in the current loop as a bitset. extraVs
	 *  starts as 0 (meaning empty) because each startCell has two maybes: v1
	 *  and v2, then each corner that maybe v1 and v2 which also has extra
	 *  (3-or-more) maybes donates the extras to extraVs. There can be at most
	 *  2 cells in a loop with extra values OR there is only one extra value
	 *  (ie all (could be > 2) cells with extraVs share a single extra value).
	 *  There may also be cells having only v1 and v2.
	 *  NB: extraVs is a parameter (a local) which reverts as we ascend back
	 *  up the recursive callstack, automatically going back-to the callers
	 *  variable with it's existing value (unchanged by recursion). This
	 *  behaviour is required for algorithmic correctness (which I found more
	 *  than a little confusing, until it came clear, overnight).
	 * @param x numExtraValueCellsRemaining: the remaining number of cells with
	 *  extraVs allowed in the loop. nXs starts at 2, and is decremented as we
	 *  extend the loop, while it stays above 0 (or VSIZE[extraVs]==1).
	 */
	private boolean recurseLoops(final Cell cc, final int prevRti, int extraVs
			, final int x) {
//++numFindLoops; // observed 14,521 calls in 1 analyse of 1 puzzle
		workLoop[workLoopSize++] = cc;
		isInWorkLoop[cc.i] = true;
		ARegion r; // region
		Cell c; // cell
		int maybes // c.maybes
		  , rti // regionTypeIndex
		  , ci // cellIndex
		  , i; // the ubiqitious index
		boolean ok; // are we good?
		// presume that no loops will be found
		boolean result = false;
		for ( rti=0; rti<Grid.NUM_REGION_TYPES; ++rti ) { // BOX, ROW, COL
			// skip if the current rti equals the previous region type
			if ( (r=cc.regions[rti]).typeIndex != prevRti ) {
				for ( ci=0; ci<REGION_SIZE; ++ci ) { // 2*9=18 * 14,521=261,378
					if ( (c=r.cells[ci])==startCell && workLoopSize>3 ) {
						// loop closed: store if valid and not already exists.
						if ( isValid(workLoop, workLoopSize) ) {
							workLoopIdx.set(workLoop, workLoopSize);
							for ( ok=true,i=0; i<numLoops; ++i )
								if ( loopsIdxs[i].hasAll(workLoopIdx) ) {
									ok = false;
									break;
								}
							if ( ok ) {
								loopsIdxs[numLoops].set(workLoopIdx);
								loopSizes[numLoops] = workLoopSize;
								loops[numLoops++] = Arrays.copyOf(workLoop, workLoopSize);
								result = true;
							}
						}
					// skip if cell may NOT be both v1 and v2
					//      or is already in workLoop
					} else if ( (c.maybes & cands)==cands && !isInWorkLoop[c.i] ) {
						// mutate my local extraVs, to build-it-up as we go
						extraVs |= (maybes=c.maybes) & ~cands;
						// continue this search:
						// if cell has two maybes: v1 and v2 (no extras);
						// or cell has extra values and the maximum number of
						//    cells with extra values (2) is not exceeded;
						// or cell has 1 extra value, the same as all previous
						//    cells with an extra value (for type 2 only)
						// or the bastard is a Hidden Unique Rectangle
						if ( VSIZE[maybes]==2 || x>0 || VSIZE[extraVs]==1
						  // * Hidden Unique Rectangles are 4 cells (no loops)
						  //   having atleast one extraVs (do the rest later)
						  || (workLoopSize<4 && VSIZE[extraVs]>0) ) {
							result |= recurseLoops(c, r.typeIndex, extraVs
									, VSIZE[maybes]>2 ? x-1 : x);
						}
					}
				}
			} // next cell in region
		} // next region of currCell
		// rollback
		isInWorkLoop[cc.i] = false;
		--workLoopSize;
		return result;
	}

	/**
	 * Check if the given loop (a list of cells) is a candidate for a Unique
	 * Loop by seeing if each region of each cell is visited by exactly 2
	 * cells of opposing parity; ie wax-on==wax-off.
	 * <p>
	 * Note that the cells are already known to all have the same two maybes
	 * (with at most two having extraVs), so I don't check it.
	 *
	 * @param grid the grid
	 * @param workLoop the cells of the loop
	 * @param workLoopSize the number of cells in the loop
	 * @return whether the given loop is a candidate for a unique loop
	 */
	private boolean isValid(final Cell[] workLoop, final int workLoopSize) {
		// we build two sets: odds and evens. The first cell is an even, the
		// second is an odd, and so on around the loop. Then we check that
		// the two sets contain exactly the same regions.
		boolean result;
		try {
			Set<ARegion> set;
			boolean isEven = true;
			for ( int i=0; i<workLoopSize; ++i ) {
				set = isEven ? evens : odds;
				for ( ARegion r : workLoop[i].regions )
					if ( !set.add(r) ) // add only (no update).
						return false; // "I told him we've already got one"
				isEven = !isEven;
			}
			// Each region has been visited once with each parity (or never)
			result = odds.equals(evens);
		} finally {
			// clean-up for next time.
			evens.clear();
			odds.clear();
		}
		return result;
	}
	// evens and odds are created ONCE, rather than in each isValid call.
	// Note that this is (currently) the only use of the RegionSet class
	private final Set<ARegion> evens = new RegionSet();
	private final Set<ARegion> odds = new RegionSet();

	private AURTHint createType1Hint(int v1, int v2, Cell[] loop, int loopSize) {
		Cell rescueCell = extraCells[0];
		Pots redPots = new Pots(rescueCell, VSHFT[v1]|VSHFT[v2], false);
		return new URT1Hint(this, loop, loopSize, v1, v2, redPots, rescueCell);
	}

	private AURTHint createType2OrHiddenHint(Grid grid, int v1, int v2
			, Cell[] loop, int loopSize) {
		// cellsWithExtraValuesArray := extraCells list
		final Cell[] xCells = Arrays.copyOf(extraCells, numExtraCells);
		// get the extra values: loop-cells maybes except v1 and v2
		int extraVs = 0;
		for ( Cell c : xCells ) {
			extraVs |= c.maybes;
		}
		extraVs &= ~VSHFT[v1];
		extraVs &= ~VSHFT[v2];
		// there must be extra values (or we're completely rooted)
		assert VSIZE[extraVs] != 0;
		// Type 2 has 1 extra value; more could be a Hidden Unique Rectangle
		if ( VSIZE[extraVs] == 1 ) {
			// Type 2 Unique Rectangle
			// get the first (and only) extra value
			final int extraV = VFIRST[extraVs];
			// get extraBuds := buds common to all extraCs, except extraCs
			// themselves, which maybe extraV.
			final Idx extraBuds = Cells.cmnBudsNew(xCells);
			extraBuds.and(idxs[extraV]);
			if ( extraBuds.none() ) {
				return null; // nothing to see here
			}
			// build the removable (red) potentials
			final Pots reds = new Pots(extraV, extraBuds.cellsA(grid));
			// build and return the hint
			return new URT2Hint(this, loop, loopSize, v1, v2, reds, xCells, extraV);
		} else {
			// Hidden Unique Rectangle
			// This code relies on the rather odd A,B,D,C order! loop contains
			// A,B,D,C and that's how it'll always be, coz we start the search
			// from A (a bivalue cell), then find B in the same box, then D in
			// the same row-or-col, and finally C in the same col-or-row.
			// NB: 468 Hidden URT's in top1465, so it's worth the hastle.
//			final Cell A = loop[0]; // the start cell
			final Cell B = loop[1];
			final Cell D = loop[2]; // NOTE: D and C out of order (above).
			final Cell C = loop[3];
			// A is a or b only, B and C require 1-or-more extra values;
			// and D has no constraint (it's allowed 0-or-more extra values).
			if ( B.size>2 && C.size>2 ) {
				// Are B and D the only places for 'a' (v1) in a common region;
				//         OR (the only places for 'b' (v2) in a common region
				//             and if so we swap v1 and v2, to make v1 'a')
				ARegion[] bases = null;
				int l=0, e=0; // locked-in value, eliminatable value
				int n = Regions.common(B, D, cmnRgns);
				for ( int i=0; i<n; ++i ) {
					// we know that both B and D maybe v1, so just check that
					// r has ONLY these two places for v1.
					if ( cmnRgns[i].ridx[v1].size == 2 ) {
						bases = Regions.array(cmnRgns[i]);
						l = v1;
						e = v2;
						break;
					}
					// we know that both B and D maybe v2, so check that r has
					// ONLY two places for v2. Note that l=v2 and e=v1.
					if ( cmnRgns[i].ridx[v2].size == 2 ) {
						bases = Regions.array(cmnRgns[i]);
						l = v2;
						e = v1;
						break;
					}
				}
				if ( bases != null ) {
					// are C and D the only places for 'l' in a common region
					ARegion[] covers = null;
					n = Regions.common(C, D, cmnRgns);
					for ( int i=0; i<n; ++i ) {
						// we know that both C and D maybe 'l', so just check
						// that r has ONLY these two places for 'l'.
						if ( cmnRgns[i].ridx[l].size == 2 ) {
							covers = Regions.array(cmnRgns[i]);
							break;
						}
					}
					if ( covers != null ) {
						// FOUND a Hidden Unique Rectangle
						// get the removable (red) potentials
						final Pots reds = new Pots(D, e);
						// build and return the hint: bases BD, and covers CD
						return new URTHiddenHint(this, loop, loopSize, l, e
								, reds, bases, covers);
					}
				}
			}
		}
		return null;
	}

	private List<AURTHint> createMultipleType3Hints(Grid grid, int v1, int v2
			, Cell c1, Cell c2, Cell[] loop, int loopSize) {
		final List<AURTHint> hints = new LinkedList<AURTHint>() {
			private static final long serialVersionUID = 10948320L;
			/** Note that add ignores nulls! */
			@Override
			public boolean add(AURTHint h) {
				return h!=null && super.add(h);
			}
		};
		// Get the extra (not v1 or v2) values from c1 and c2.
		final int extraVs = (c1.maybes | c2.maybes) & ~VSHFT[v1] & ~VSHFT[v2];
		//assert extraVs.equals(new Values(c1.maybes).or(c2.maybes).clear(v1).clear(v2));
		final int numExtraVs = VSIZE[extraVs];
		final int numRmvVs = 7 - numExtraVs; // number of removable values
		// c1 and c2 could occupy the same box and the same (row or col), in
		// which case we search both common regions for Naked and Hidden Sets
		// NOTE: this code is executed rarely enough for us to get away with
		// creating a new Collection on each invocation, but it's not ideal.
		final List<ARegion> commonRegions = commonRegions(grid, c1, c2);
		if ( commonRegions.isEmpty() ) {
			return hints; // an empty list
		}
		// look for Naked and Hidden sets.
		for ( int n=numExtraVs; n<8; ++n ) { // setSize (a walking degree)
			// create here with the setSize, which is OK coz we're rare.
			int[] maybes = new int[n];
			// foreach region that contains both c1 and c2
			for ( ARegion r : commonRegions ) { // 1 or 2 of them
				// look for Naked Sets
				if ( n*2 <= r.emptyCellCount ) {
					lookForNakedSets(n, extraVs, c1, c2, maybes, r, loop
							, loopSize, v1, v2, hints);
				}
				// look for Hidden Sets
				if ( n*2<r.emptyCellCount && n-2<=numRmvVs ) {
					lookForHiddenSets(n, numRmvVs, r.indexOf(c2), extraVs
							, c1, c2, r, loop, loopSize, v1, v2, hints);
				}
			} // next common region
		} // for degree
		return hints;
	}

	/**
	 * Returns a new {@code ArrayList<ARegion>} containing the 0, 1, or 2
	 * regions which are common to these two cells.
	 * @param grid
	 * @param c1
	 * @param c2
	 * @return a new {@code ArrayList<ARegion>}
	 */
	private ArrayList<ARegion> commonRegions(Grid grid, Cell c1, Cell c2) {
		final ArrayList<ARegion> commonRegions = new ArrayList<>(2);
		for ( int rti=0; rti<3; ++rti ) // box, row, col
			if ( c1.regions[rti] == c2.regions[rti] ) // reference equals OK
				commonRegions.add(c1.regions[rti]);
		return commonRegions;
	}

	/**
	 * Look for Naked Sets of size n in this URT at Cells c1 and c2.
	 * <p>
	 * Yes, this s__t IS confusing. Don't panic (yet).
	 *
	 * @param n is the setSize (number of cells in the Naked Set we seek)
	 * @param extraVals is the maybes of the cells in this URT except v1 and v2
	 * @param c1 is the first cell with extra values in this URT
	 * @param c2 is the second cell with extra values in this URT (there can
	 *  only be 2)
	 * @param maybes is an empty array of n Values[], passed in rather than
	 *  repeatedly create the array of the same size.
	 * @param region is the region we are searching in, which is common to
	 *  c1 and c2 (the two cells with extra values in this URT)
	 * @param loop the Cells in this URT
	 * @param int v1 the first value common to all cells in this URT
	 * @param int v2 the second value common to all cells in this URT
	 * @param hints the {@code Collection<AURTHint>} to which we add hints.
	 */
	private void lookForNakedSets(int n, final int extraVals
			, Cell c1, Cell c2, int[] maybes, ARegion region
			, Cell[] loop, int loopSize, int v1, int v2
			, final Collection<AURTHint> hints) {
		// We look at each combination of $degree cells in this region which
		// includes c1 but not c2
		// get the indexes of c1 and c2 in this regions cells array
		final int idxOfC1=region.indexOf(c1), idxOfC2=region.indexOf(c2);
		// ensure extraVals+c1+c2==fullSet or not a NakedSet with c1 and c2
		final int baseValues = extraVals & c1.maybes & c2.maybes;

		// WARNING: We can only get away with using the cas so long as the
		// array is NEVER retained! otherCells contents could be modified
		// at any arbitrary time in the future. All this to reduce GC.
		final Cell[] otherCells = Cells.arrayA(n - 1);

		int cmnMaybes;  Cell cell;
		int i, cnt, nkdSetValuesBits;
		// foreach possible combination of n cells amongst the 9 in the region
		for ( int[] perm : new Permutations(REGION_SIZE, IAS1[n]) ) {
			// which includes c1 but does not include c2
			if ( !contains1ButNot2(perm, idxOfC1, idxOfC2) )
				continue;
			// ensure extraVals+c1+c2==fullSet or not a NakedSet with c1 and c2
			nkdSetValuesBits = baseValues;
			for ( i=0,cnt=0; i<n; ++i )
				if ( perm[i] == idxOfC1 ) { // index of cell c1
					// so use the extraValues; this is NOT an otherCell so it
					// does NOT contribute to nkdSetValues
					maybes[i] = extraVals;
				} else {
					// otherwise use cells maybes; this is an otherCell which
					// contributes to nkdSetValues
					otherCells[cnt++] = cell = region.cells[perm[i]];
					nkdSetValuesBits |= (maybes[i]=cell.maybes);
				}
			// ALL elements of otherCells are over-written every time
			assert cnt == otherCells.length;
			// look for $degree common potential values
			// ensure extraVals+c1+c2==fullSet or not a NakedSet with c1 and c2
			if ( VSIZE[nkdSetValuesBits] == n
			  && (cmnMaybes=Values.common(maybes, n)) != 0 )
				// naked set found, but does it eliminate anything?
				// note that hints.add ignores nulls
				hints.add(createType3NakedSetHint(loop, loopSize, v1, v2
						, extraVals, region, c1, c2, otherCells, cmnMaybes));
		} // next permutation
	}

	// Does perm (this permutation of indexes) contain idxOfC1 but NOT idxOfC2?
	private boolean contains1ButNot2(int[] perm, int idxOfC1, int idxOfC2) {
		assert idxOfC1 != idxOfC2;
		boolean foundIdxOfC1 = false;
		for ( int i=0,n=perm.length; i<n; ++i )
			if ( perm[i] == idxOfC1 )
				foundIdxOfC1 = true;
			else if ( perm[i] == idxOfC2 )
				return false;
		return foundIdxOfC1;
	}

	private AURTHint createType3NakedSetHint(Cell[] loop, int loopSize, int v1
			, int v2, int extraVals, ARegion region, Cell c1, Cell c2
			, Cell[] otherCells, int nkdSetVals) {
		// Build removable potentials (as efficiently as possible)

		// get an Idx for it's fast O(1) contains method.
		// nb: excluded is ONLY my filter, so I can ignore c1 and c2 by adding
		// them ONCE; instead of ignoring them explicitly, individually and
		// bloody repeatedly in the below bloody loop. Jeez I'm thick!
		final Idx excluded = Idx.of(otherCells);
		excluded.add(c1.i);
		excluded.add(c2.i);

		Pots reds=null;  int bits;
		for ( Cell c : region.cells ) //other cell
			if ( (bits=c.maybes & nkdSetVals) != 0
			  && !excluded.has(c.i) ) {
				if(reds==null) reds=new Pots();
				reds.put(c, bits);
			}
		if ( reds == null )
			return null; // no eliminations! 90+% return here
		return new URT3NakedSetHint(this, loop, loopSize, v1, v2, reds, c1, c2
				, extraVals, region, otherCells, nkdSetVals);
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
	 * @param numRmvVals is only used as Permutations nBits parameter: the
	 *  size of the master list
	 * @param idxOfC2 the index of c2 in the regions cells array
	 * @param extraVals the extra values that are hanging-out with v1 and v2
	 *  in the corners of our rectangle/loop
	 * @param c1 the first corner with extra (more than 2) values
	 * @param c2 the second corner with extra (more than 2) values
	 * @param r ARegion to examine which contains both c1 and c2
	 * @param loop the corners of the rectangle/loop
	 * @param v1 the first value in all 4-or-whatever corners
	 * @param v2 the second value in all 4-or-whatever corners
	 * @param hints {@code Collection<AURTHint>} to which we add hints
	 * @return Gimp
	 */
	private void lookForHiddenSets(int N, int numRmvVals, int idxOfC2
			, final int extraVals, Cell c1, Cell c2, ARegion r
			, Cell[] loop, int loopSize, int v1, int v2
			, final Collection<AURTHint> hints
	) {
		// constants
		final int shftIdxOfC2 = ISHFT[idxOfC2];
		// get the removable values array := {1..9} - extraVals - v1 - v2
		final int[] rmvVals = VALUESES[VALL & ~extraVals & ~VSHFT[v1] & ~VSHFT[v2]];
		// the values of the hidden set
		final int[] hdnSetVals = IAS2[N];
		// set sizes
		final int M = N - 1; // get in!
		final int L = N - 2; // get it?
		// variables
		int i, bits, b;
		// foreach possible combination of the removable values
		P_LOOP: for ( int[] perm : new Permutations(numRmvVals, IAS3[L]) ) {
			// Look for a hidden set with this combo of removable values.
			// We seek $N locations for $N values of each potential hidden set.
			// get the hidden set values := this combo + v1 + v2.
			for ( i=0; i<L; ++i )
				hdnSetVals[i] = rmvVals[perm[i]];
			hdnSetVals[L] = v1; // penultimate
			hdnSetVals[M] = v2; // last
			// get the locations of each hdnSetVal in the region minus c2
			// (Don't ask me why - c2. I don't know. Yes, I'm stupid!)
			bits = 0;
			for ( int v : hdnSetVals ) {
				// bits |= myBits = the indexes of the hidden set - c2
				bits |= b = r.ridx[v].bits & ~shftIdxOfC2;
				// if b is empty then skip (to the store?)
				if(b==0) continue P_LOOP;
			}
			// look for $N possible locations for these $N values
			if ( VSIZE[bits] == N )
				// Hidden set found, but does it remove any maybes?
				// nb: hints.add ignores nulls (it'll just return false)
				hints.add(createType3HiddenSetHint(loop, loopSize, v1, v2
						, extraVals, Values.bitset(hdnSetVals), r, c1, c2
						, new Indexes(bits)));
		} // next permutation
	}

	private AURTHint createType3HiddenSetHint(final Cell[] loop
			, final int loopSize, final int v1, final int v2
			, final int extraVals, final int hdnSetValues, final ARegion r
			, final Cell c1, final Cell c2, final Indexes hdnSetIdxs) {
		// remove c1 and c2 from hdnSetIdxs
		// nb: hdnSetIdxs is MINE (it was created in the call to me).
		hdnSetIdxs.remove(r.indexOf(c1));
		hdnSetIdxs.remove(r.indexOf(c2));
		// Build the red (removeable) potential values
		Pots reds = null;
		Cell cell;
		int pink;
		for ( int i : INDEXES[hdnSetIdxs.bits] )
			if ( (pink=(cell=r.cells[i]).maybes & ~hdnSetValues) != 0 ) {
				if(reds == null) reds=new Pots();
				reds.put(cell, pink);
			}
		if ( reds == null )
			return null;
		return new URT3HiddenSetHint(this, loop, loopSize, v1, v2, reds, c1, c2
				, extraVals, hdnSetValues, r, hdnSetIdxs);
	}

/*
The first URT Type 4
13#top1465.d5.mt	.4.3..9...7...5.2.8........6.....352..5....9...4...6.....9....8....42.7..9.16....
.4.3..9...7...5.2.8........6.....352..5....9...4...6.....9...68....42.79.9.16....
125,,126,,278,1678,,18,567,139,,1369,46,189,,48,,146,,1256,1269,2467,1279,14679,457,34,34567,,18,1789,478,1789,14789,,,,127,1238,,2678,1238,1368,478,,147,1279,1238,,2578,2359,13789,,18,17,1247,125,127,,357,37,1245,,,135,568,1368,58,,,15,,,2457,,278,,,78,245,34,345
24   	        481,100	25	 182	  2	Unique Rectangle              	Unique Rectangle type 4: H3, I3, I9, H9 on 3 and 4 (I3-4, I9-4)
*/
	// Note that SE's Type 4 includes Uniqueness Test 6.
	private AURTHint createType4Hint(Cell[] loop, int loopSize, Cell c1, Cell c2
			, int v1, int v2) {
		// Look for v1 or v2 locked in the region common to c1 and c2. The
		// value that's locked into a region can be removed from cells with
		// extra (not v1 or v2) values.
		//
		// meaning find me a region r1-or-r2
		//   which is common to both c1 and c2
		//     and contains NO cells (except c1 or c2) which may be v1 or v2
		//
		// ergo: find me a region in which c1 and c2 are the only possible
		//       locations for v1 and v2
		//
		// It's perfectly simple Simonds: If you aren't having lunch, but your
		// little brother is having his hair cut, then move your coat down to
		// the lower peg before going to the pictures, but after slamming your
		// cock repeatedly in the fifth floor prefects bathroom window withOUT
		// whimpering like some spineless aardvarkian wolf nippled mommas-boy.
		//
		ARegion r1=null, r2=null;
		final int sv1 = VSHFT[v1];
		final int sv2 = VSHFT[v2];
		for ( ARegion r : c1.regions ) // c1's Box, Row, Col
			// if r is common to both c1 and c2 then
			if ( r == c2.regions[r.typeIndex] ) {
				// remember r if it has no v1 or v2 outside of c1 or c2.
				// Now the oral sex: there's a mess of special code underneath
				// these two lines; and all for a polished alabaster artichoke
				// heart with bi-wolf-nipples!
				// Ergo: It's pretty stupid and I should fix it; but I've done
				// it now and it works, and if it works don't ____ with it even
				// if it relies on a mess of "special" code. sigh.
//++type4Cnt; // 382,984 for top1465.d5.mt
				if(r.maybe(sv1, cs).remove(c1, c2).isEmpty()) r1=r;
				if(r.maybe(sv2, cs).remove(c1, c2).isEmpty()) r2=r;
			}
		// one of the two r's must be null.
		if ( (r1==null && r2==null) || (r1!=null && r2!=null) )
			return null; // nothing to see here. Move along!
		assert r1==null || r2==null;
		// either r1==null or r2==null, but not both
		// nb: Executed rarely, so terniaries are fast enough.
		ARegion region = r1!=null ? r1 : r2;
		int lockValue = r1!=null ? v1 : v2;
		int vToRemove = r1!=null ? v2 : v1;
		Pots redPots = new Pots();
		redPots.put(c1, VSHFT[vToRemove]);
		redPots.put(c2, VSHFT[vToRemove]);
		return new URT4Hint(this, loop, loopSize, lockValue, vToRemove
				, redPots, c1, c2, region);
	}
	private final UrtCellSet cs = new UrtCellSet();

	/**
	 * Used only by UniqueRectangle, but the CellSet reference type is used by
	 * {@link Grid.ARegion#maybe(int bits, IUrtCellSet results)} to fetch the
	 * cells in this region which maybe 'bits'.
	 */
	public static interface IUrtCellSet extends Set<Cell> {
		/**
		 * Removes the given cells from this CellSet.
		 * @param a first Cell to remove
		 * @param b second Cell to remove
		 * @return this IUrtCellSet, for method chaining.
		 */
		public IUrtCellSet remove(Cell a, Cell b);
	}

	/**
	 * Used only by UniqueRectangle, once. There's nothing specific to URT in
	 * this class, but it's only used here, hence the "odd" name.
	 */
	private static class UrtCellSet extends LinkedHashSet<Cell> implements IUrtCellSet {

		private static final long serialVersionUID = 609192873L;

		/**
		 * {@InheritDoc}
		 */
		public UrtCellSet(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		/**
		 * {@InheritDoc}
		 */
		public UrtCellSet(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Constructs a LinkedHashCellSet of 16 capacity with 0.75F loadFactor
		 */
		public UrtCellSet() {
			super(16, 0.75F);
		}

		/**
		 * The Copy Constructor.
		 * @param src a LinkedHashCellSet to copy
		 */
		public UrtCellSet(UrtCellSet src) {
			super(src);
		}

		/**
		 * {@InheritDoc}
		 */
		@Override
		public IUrtCellSet remove(Cell a, Cell b) {
			super.remove(a);
			super.remove(b);
			return this;
		}

	}

}
