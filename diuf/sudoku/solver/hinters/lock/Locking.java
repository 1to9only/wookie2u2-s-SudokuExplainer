/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.Arrays;


/**
 * Implementation of Locking (Pointing and Claiming) solving techniques.
 * <p>
 * Locking is when a candidate is "locked into" a region; ie when these are
 * the only cells in a region that maybe this value then one of them MUST
 * be this value (each value must appear in each region), eliminating the
 * value from other cells in other region/s common to all cells in the set.
 * <p>
 * Note that in "standard" Sudoku with region types: box, row, and col there
 * is only one other possible type-of-region common to two-or-more cells. For
 * a box it's either a row or a col; and for a row-or-col it's a box; hence I
 * have decomposed Locking into Pointing and Claiming; because I found the term
 * Locking nonsensical when I started my Sudoku journey (I get it now), so here
 * are my definitions of my preferred "simpler" terms:<ul>
 * <li><b>Pointing</b> is when all candidates in a box are in one row-or-col,
 *  therefore we can remove all other candidates from the row-or-col.
 * <li>for example: both cells that maybe 5 in box1 are both in colC,
 *  <br>so one of those two cells must be 5,
 *  <br>therefore no other cell in colC may be 5.
 * <li><b>Claiming</b> is when all candidates in a row-or-col are in one box,
 *  therefore we can remove all other candidates from the box.
 * <li>for example: the three cells that maybe 9 in row1 are all in box2,
 *  <br>so one of those three cells must be 9,
 *  <br>therefore no other cell in box2 may be 9.
 * </ul>
 * <p>
 * Locking may use a HintsApplicumulator to apply each Pointing and Claiming
 * hint as soon as it is found, so that "all" hints are applied in one pass
 * through the grid. Then I add a AppliedHintsSummaryHint to the "normal"
 * Single/Default/Chains IAccumulator to pass nElims back to {@code apply()}
 * to keep track of "the score".
 * <p>
 * NB: If you implement Resetable you'll need to put a null apcu check in the
 * existing reset method, coz LogicalSolver calls reset when there's nothing
 * to reset; ie outside of RecursiveAnaylser; ie "normal" mode.
 * <p>
 * KRC 2020-02-29 Added mergeSiameseHints and eliminateSubsets to this already
 * too-complex class. Used only in the GUI when !wantMore && !isFilter.
 * A "Siamese" hint is when multiple pointing hints are generated for multiple
 * values in the same region, whose redPots are all in a common region.
 * Example: {@code C:/Users/User/Documents/SodukuPuzzles/Test/ClaimingFromABoxWith3EmptyCells_001.txt}
 * eliminateSubsets occurred to me after implementing siamese to remove any/all
 * hints which have been superceeded by an "upgraded" HiddenSetHint (but beware
 * it works with ALL hints in the accu), so that the best (highest score) hint
 * is selected, so that I can just press enter again.
 */
public class Locking extends AHinter {

	// constants to determine if a boxes ridx[v] are all in a row.
	// The best way to display the logic is in binary.
	// NOTE: left-to-right view of right-to-left bits -> upside down!
	protected static final int ROW1 = Integer.parseInt("000"
												     + "000"
												     + "111", 2); // 7
	protected static final int ROW2 = Integer.parseInt("000"
												     + "111"
												     + "000", 2); // 56=7<<3
	protected static final int ROW3 = Integer.parseInt("111"
												     + "000"
												     + "000", 2); // 448=56<<3

	// constants to determine if a boxes ridx[v] are all in a column.
	protected static final int COL1 = Integer.parseInt("001"
												     + "001"
												     + "001", 2); // 73
	protected static final int COL2 = Integer.parseInt("010"
												     + "010"
												     + "010", 2); // 146=73<<1
	protected static final int COL3 = Integer.parseInt("100"
													 + "100"
												     + "100", 2); // 292=73<<2

	/**
	 * Used in error messages, to tell the programmer which method
	 * called createHint, to make errors more traceable.
	 */
	protected static enum LockingType { Pointing, Claiming
									  , SiamesePointing, SiameseClaiming }

	/**
	 * Constructor for "normal mode".
	 */
	public Locking() {
		super(Tech.Locking);
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and Claiming
	 * (row/col on box) Hints, which are added to the given HintsAccumulator.
	 * <p>
	 * S__t goes weird in "speed mode" when a HintsApplicumulator was passed to
	 * my Constructor, in which case we apply each hint now (to autosolve grid)
	 * and then pass-back the total elims in a AppliedHintsSummaryHint to
	 * getHints-caller via the "normal" HintsAccumulator passed into getHints.
	 * <p>
	 * It's all bit complicated, so writing an explanation seems harder than
	 * writing the code, and I expect the explanation is probably harder to
	 * understand than the code. I just suck at explaining stuff. sigh.
	 * <p>
	 * Note that the HintsApplicumulator has a "debug mode" where it populates
	 * a StringBuilder with the-full-string of each hint applied to the grid.
	 *
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// normal mode: GUI, test-cases, batch
		if ( accu.isSingle() )
			// user wants 1 hint so prefer pointing to claiming.
			// nb: "normal" short-circuiting boolean-or operator.
			return pointing(grid, accu) || claiming(grid, accu);
		// GUI wants multiple hints so collect both pointing & claiming
		// nb: "unusual" bitwise-or operator, so they're both executed.
		return pointing(grid, accu) | claiming(grid, accu);
	}

	/**
	 * Overridden by LockingSpeedMode to handle new eliminations.
	 *
	 * @param redPots
	 */
	protected void eliminationsFound(Pots redPots) {}

	/**
	 * Overridden by SiameseLocking to find hints per region
	 *
	 * @param r
	 */
	protected void startRegion(ARegion r) { }

	//
	/**
	 * Overridden by SiameseLocking to find hints per region.
	 *
	 * @param r the region we're searching in
	 * @return should we stop now because we've found the first hint,
	 *  ie accu.isSingle, which makes no sense seeking Siamese hints.
	 *  Hence my return value is now simply ignored, and I'm a putz!
	 */
	protected boolean endRegion(ARegion r) { return false; }

	/**
	 * Pointing: Box => Row/Col.
	 * <pre>
	 * Example: All the cells in box 2 which maybe 9 are in column D,
	 * hence one of those 2-or-3 cells must be a 9, therefore no other
	 * cell in column D can be 9.
	 * Optimisation: inspect only THE row or THE column (r2) which is
	 * common to all cells in the box base which maybe v.
	 * </pre>
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	protected boolean pointing(Grid grid, IAccumulator accu) {
		Box box; // the current Box
		Indexes[] bio; // box.idxsOf array, indexed by value
		ARegion line; // the region we eliminate from, either a Row or a Col
		Cell[] cells; // the cells to go in the hint
		int i // region Index
		  , v // value
		  , card // cardinality: the number of set (1) bits in a bitset
		  , b // box.idxsOf[v].bits mainly coz it needs a really short name
		  , offset // the 0-based offset of this row/col within this box.
		  , cnt; // count: retval of maybes which always equals cand
		// presume that no hints will be found
		boolean result = false;
		// for each of the 9 boxes in the grid
		for ( i=0; i<REGION_SIZE; ++i ) {
			// we need atleast 3 empty cells to form this pattern
			if ( (box=grid.boxs[i]).emptyCellCount > 2 ) {
				startRegion(box); // SiameseLocking
				bio = box.ridx;
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// 0 means that this value is already placed in this box;
					// 1 is a HiddenSingle (not my bloody problem);
					// 2 or 3 cells in a box could all be in a row or col; but
					// 4 or more can't be.
					if ( (card=bio[v].size)>1 && card<4 ) {
						// bit twiddling: skip unless all v's in this box
						// are all in a line, ie one row, or one col.
						line = null;
						b = bio[v].bits;						  // box's 2or3 possible positions of v are:
						if ( ((b & ROW1)==b && ((offset=0)==0))	  // all in the first 3 cells, so offset is 0
						  || ((b & ROW2)==b && ((offset=1)==1))	  // all in the second 3 cells, so offset is 1
						  || ((b & ROW3)==b && ((offset=2)==2)) ) // all in the third 3 cells, so offset is 2
							line = grid.rows[box.top + offset];	  // therefore line is the row at box.top + offset
						else if ( ((b & COL1)==b && ((offset=0)==0))
							   || ((b & COL2)==b && ((offset=1)==1))
							   || ((b & COL3)==b && ((offset=2)==2)) )
							line = grid.cols[box.left + offset];    // therefore line is the col at box.left + offset
						// all v's in the box are also in the line, so if the
						// line also has other locations for v it's a Pointing
						if ( line!=null && line.ridx[v].size > card ) {
							// get the 2-or-3 cells in this line which maybe v
							// nb: when we get here we're ALWAYS hinting, so it
							// is OK to create the cells array, which we need.
							// nb: the bloody CAS goes recursively bad!
							cnt = box.maybe(VSHFT[v], cells=new Cell[card]);
							if ( cnt == card ) {
								final LockingHint hint = createHint(
									  LockingType.Pointing
									, box
									, line
									, cells
									, card
									, v
									, grid
								);
								if ( hint != null ) {
									assert hint.isPointing == true;
									result = true;
									accu.add(hint);
								}
							}
						}
					}
				} // next value
				endRegion(box); // SiameseLocking
			} // fi
		} // next box
		return result;
	}

	/**
	 * Claiming: Row/Col => Box.
	 * <pre>
	 * Example: All the cells which maybe 9 in row 1 are in box 3, hence one of
	 * those 2-or-3-cells has to be a 9, therefore no other cell in box 3 can
	 * be 9. Ie: Row 1 claims 9 from box 3.
	 * </pre>
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	protected boolean claiming(Grid grid, IAccumulator accu) {
		ARegion line; // a row (9..17) or a column (18..26)
		Indexes[] rio; // bases idxsOf array
		Box[] crossingBoxes; // the 3 boxes which intersect this base
		Cell[] cells; // the hints in each box
		int i // region Index
		  , v // value
		  , card // cardinality: the number of elements in a bitset
		  , b // rio[v].bits
		  , offset // the offset of the matching box in the grid.boxs array
		  , cnt; // count: the retval from maybe, which always equals card
		// presume that no hints will be found
		boolean result = false;
		// for each row (9..17) and col (18..26) in the grid
		for ( i=REGION_SIZE; i<NUM_REGIONS; ++i ) {
			// we need atleast 3 empty cells to form this pattern
			if ( (line=grid.regions[i]).emptyCellCount > 2 ) {
				rio = line.ridx;
				crossingBoxes = line.crossingBoxs;
				startRegion(line); // SiameseLocking
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// 2 or 3 cells in base might all be in a box; 4 or more won't.
					// 1 is a hidden single, 0 means v is set, so not my problem.
					if ( (card=rio[v].size)>1 && card<4
					  // if all v's in base are in the same box.
					  // nb: b = bits of v's possible places in this region
					  // nb: ROW1/2/3 values work on cols too (despite there name)
					  // because theyre applied to the regions cells array it makes
					  // no difference which direction the region points.
					  // nb: offset is 0 if first crossingBox, 1 second, 2 third.
					  // The offset expressions are tautologies, whichre only in da
					  // if coz its cleaner to have it all in one expression verses
					  // if card; get b, ROW1, ROW2, ROW3; if crossing
					  && ( (((b=rio[v].bits) & ROW1)==b && (offset=0)==0) // bases 2or3 possible positions of v are all in the first 3 cells, so the offset is 0
						|| ((b & ROW2)==b && (offset=1)==1) // all in the second 3 cells => 1
						|| ((b & ROW3)==b && (offset=2)==2) ) // all in the third 3 cells => 2
					  // and there are some extra v's in the box to be removed
					  && crossingBoxes[offset].ridx[v].size > card ) {
						// Claiming found!
						// NOTE: The bloody CAS goes recursively bad!
						cnt = line.maybe(VSHFT[v], cells=new Cell[card]);
						if ( cnt == card ) {
							// create the hint and add it to the accumulator
							final LockingHint hint = createHint(
								  LockingType.Claiming
								, line
								, crossingBoxes[offset]
								, cells
								, card
								, v
								, grid
							);
							if ( hint != null ) {
								assert hint.isPointing == false;
								result = true; // never say never!
								accu.add(hint);
							}
						}
					}
				} // next value
				endRegion(line); // SiameseLocking
			} // fi
		} // next region1
		return result;
	}

	/**
	 * Create the hint.
	 *
	 * @param type tells you who called me, in error-messages for debugging
	 * @param base the base region. <br>
	 *  For Pointing that's the box. <br>
	 *  For Claiming that's the row/col (aka the line).
	 * @param cover the other region. <br>
	 *  For Pointing that's the row/col (aka the line). <br>
	 *  For Claiming that's the box.
	 * @param cells some cells, I forget which, but You'll work it out.
	 * @param n the number of cells in the cells array
	 * @param v the bloody value to be removed from cells
	 * @param grid currently only used for error messages
	 * @return a new LockingHint, else null (nothing to see here (run away))
	 */
	protected LockingHint createHint(LockingType type, ARegion base
			, ARegion cover, Cell[] cells, int n, int v, Grid grid) {
		// build removable (red) potentials
		final Pots redPots = new Pots();
		// foreach cell which maybe valueToRemove in covers except bases
		final int sv = VSHFT[v]; // shiftedValueToRemove
		for ( Cell cell : cover.cells )
			if ( (cell.maybes & sv)!=0 && !base.contains(cell) )
				redPots.put(cell, sv);
		if ( redPots.isEmpty() ) {
			// this should NEVER happen coz we check that cover has extra idxs
			// before we createHint. Developers investigate. Users no see.
			// IGNORE this if SingleSolution is in the call-stack (BFIIK)
			assert Debug.isClassNameInTheCallStack(10, "SingleSolution")
				: "BAD "+type+": No elims at "+base+AND+cover+ON+v
				  +IN+Arrays.toString(cells)+NL+grid.toString();
			return null;
		}
		// implemented by LockingSpeedMode (my subtype)
		eliminationsFound(redPots); // bulkDisenliberalisationIsTheOnlyAnswer!
		// build highlighted (green) potentials
		final Pots greenPots = new Pots(cells, n, v);
		Arrays.fill(cells, null);
		// build the hint from LockingHint.html
		return new LockingHint(this, v, greenPots, redPots
				, base, cover, EMPTY_STRING);
	}

}
