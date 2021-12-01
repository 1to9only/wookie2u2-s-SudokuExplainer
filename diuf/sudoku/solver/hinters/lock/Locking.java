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
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.Arrays;

/**
 * Locking implements the Locking (Pointing and Claiming) Sudoku solving
 * technique.
 * <p>
 * Locking occurs when a candidate is "locked into" a region; ie when these are
 * the only cells in a region that maybe this value then one of them must be
 * that value (because every value must appear in each region), therefore we
 * can eliminate the value from other cells in other region/s that are common
 * to all of these cells.
 * <p>
 * Note that in "standard" Sudoku with region types: box, row, and col there
 * is only one other possible type-of-region common to two-or-more cells. For
 * a box it's either a row or a col; and for a row-or-col it's a box; hence I
 * have decomposed Locking into Pointing and Claiming; because I found the term
 * Locking nonsensical when I started my Sudoku journey (I get it now), so here
 * I define my preferred "simple" terminology: <ul>
 * <li><b>Pointing</b> is when all candidates in a box also share a row-or-col,
 *  removing all other candidates from the row-or-col.
 * <li>eg: both cells that maybe 5 in box1 are both in colC, <br>
 *  hence one of those two cells must be 5, <br>
 *  therefore no other cell in colC may be 5.
 * <li><b>Claiming</b> is when all candidates in a row-or-col also share a box,
 *  removing all other candidates from the box.
 * <li>eg: the three cells that maybe 9 in row1 are all in box2, <br>
 *  hence one of those three cells must be 9, <br>
 *  therefore no other cell in box2 may be 9.
 * </ul>
 * <p>
 * Locking is extended by LockingSpeedMode to use a HintsApplicumulator to
 * apply each Locking hint as soon as it is found, so that all hints are
 * applied in one pass through the grid, which is a bit faster.
 * <p>
 * Locking is extended by SiameseLocking to find HiddenPairs/Triples maximising
 * the eliminations displayed in one hint in the GUI. Don't bother with siamese
 * in the batch coz it's slower, and we care not about presentation.
 * NB: there's a switch in batch to turn-on siamese, to find test-cases. sigh.
 * <p>
 * lock2.LockingGen is an alternate implementation of the Locking technique,
 * but it's a bit slower (despite my best efforts), and does not differentiate
 * between Pointing and Claiming, which means the P/C differentiation is
 * unnecessary, and everything should be as simple as possible, so Locking is
 * based on "poor thinking", but it IS faster. sigh. If you ever want to add
 * additional region types then chuck Locking and it's subtypes, and just use
 * LockingGen, it's fast enough, just not quite as fast as this mess.
 */
public class Locking extends AHinter {

	// constants to determine if a boxes ridx[v] are all in a row.
	// * You can calculate these values on the fly, but I think hard-coding
	//   them is a bit faster, and I think it shows the logic better.
	// * I'm using binary to show the logic, but it presents a left-to-right
	//   view of a right-to-left reality, ergo they're upside bloody down!
	protected static final int ROW1 = Integer.parseInt("000"
												     + "000"
												     + "111", 2); // 7=1+2+4
	protected static final int ROW2 = Integer.parseInt("000"
												     + "111"
												     + "000", 2); // 56=7<<3
	protected static final int ROW3 = Integer.parseInt("111"
												     + "000"
												     + "000", 2); // 448=7<<6

	// constants to determine if a boxes ridx[v] are all in a col.
	protected static final int COL1 = Integer.parseInt("001"
												     + "001"
												     + "001", 2); // 73=1+8+64
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
	protected static enum LockType {
		  Pointing // Box eliminate from Row/Col
		, Claiming // Row/Col eliminate from Box
		, SiamesePointing // LockingSpeedMode: pointing on multiple values
		, SiameseClaiming // LockingSpeedMode: claiming on multiple values
	}

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
	 * and then pass-back the total elims in a SummaryHint to getHints-caller
	 * via the "normal" HintsAccumulator passed into getHints.
	 * <p>
	 * It's all bit complicated, so writing an explanation seems harder than
	 * writing the code, and I expect the explanation is probably harder to
	 * understand than the code. I just suck at explaining stuff. sigh.
	 * <p>
	 * Note that the HintsApplicumulator has a "debug mode" where it populates
	 * a StringBuilder with the-full-string of each hint applied to the grid.
	 *
	 * @param grid
	 * @param accu
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
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

	/**
	 * Overridden by SiameseLocking to find hints per region.
	 *
	 * @param r the region we're searching in
	 * @return should we stop now because we've found the first hint, ie
	 *  accu.isSingle, which is nonsensical when seeking Siamese hints, hence
	 *  my return value is now simply ignored, and I am, officially, a putz!
	 */
	protected boolean endRegion(ARegion r) { return false; }

	/**
	 * Pointing: Box => Row/Col.
	 * <pre>
	 * Example: All the cells in box 2 which maybe 9 are in column D,
	 * hence one of those 2-or-3 cells must be a 9, therefore no other
	 * cell in column D can be 9.
	 * </pre>
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	protected final boolean pointing(final Grid grid, final IAccumulator accu) {
		Box box; // the current Box
		Indexes[] bio; // indexes in box.cells which maybe each value 1..9
		ARegion line; // the region we eliminate from, either a Row or a Col
		Cell[] cells; // the cells to go in the hint
		int i // region Index
		  , v // value
		  , card // cardinality: the number of set (1) bits in a bitset
		  , b // box.idxsOf[v].bits mainly coz it needs a really short name
		  , offset; // the 0-based offset of this row/col within this box.
		// presume that no hints will be found
		boolean result = false;
		// for each of the 9 boxes in the grid
		for ( i=0; i<REGION_SIZE; ++i ) {
			// we need atleast 3 empty cells to form this pattern
			if ( (box=grid.boxs[i]).emptyCellCount > 2 ) {
				startRegion(box); // SiameseLocking
				bio = box.ridx;
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// 0 means v is already placed in this box;
					// 1 is a HiddenSingle (not my problem);
					// 2 or 3 cells in a box could all be in a row or col; but
					// 4 or more can't be.
					if ( (card=bio[v].size)>1 && card<4 ) {
						// if all v's in this box are all in a line then set
						// the offset in order to fetch that line.
					    // offset expressions are tautologies: they're in the
					    // if-statement to smash it all into one expression
						// without the expense of invoking a method
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
						// and if that line has other v's then it's a Pointing
						if ( line!=null && line.ridx[v].size > card
						  // FOUND Pointing!
						  // get the cells in this box which maybe v.
						  // maybe should always return card. Never say never.
						  // nb: new Cell[] coz the CAS goes bad, recursively.
						  && box.maybe(VSHFT[v], cells=new Cell[card]) == card
						) {
							final AHint hint = createHint(LockType.Pointing
									, box, line, cells, card, v, grid);
							if ( hint != null ) {
								result = true;
								accu.add(hint);
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
	protected final boolean claiming(final Grid grid, final IAccumulator accu) {
		ARegion line; // a row (9..17) or a column (18..26)
		Indexes[] rio; // bases idxsOf array
		Box[] crossingBoxes; // the 3 boxes which intersect this row/col
		Cell[] cells; // the hints in each box
		int i // region Index
		  , v // value
		  , card // cardinality: the number of elements in a bitset
		  , b // rio[v].bits
		  , offset; // the offset of the matching box in the grid.boxs array
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
					// 2 or 3 cells in line can all be in box; 4+ can't.
					// 1 is hidden single, 0 means v is set, so not my problem.
					if ( (card=rio[v].size)>1 && card<4
					  // if all v's in base are in the same box.
					  // nb: ROW* values work on cols too (despite there name)
					  // because they're applied to the lines cells array it
					  // makes no difference which direction that line points.
					  && ( (((b=rio[v].bits) & ROW1)==b && (offset=0)==0) // bases 2or3 possible positions of v are all in the first 3 cells, so the offset is 0
						|| ((b & ROW2)==b && (offset=1)==1) // all in the second 3 cells => 1
						|| ((b & ROW3)==b && (offset=2)==2) ) // all in the third 3 cells => 2
					  // and there are some extra v's in the box to be removed
					  && crossingBoxes[offset].ridx[v].size > card
					  // FOUND Claiming!
					  // get the cells in this line which maybe v.
					  // maybe should always return card. Never say never.
					  // nb: new Cell[] coz the CAS goes bad, recursively.
					  && line.maybe(VSHFT[v], cells=new Cell[card]) == card
					) {
						// create the hint and add it to the accumulator
						final AHint hint = createHint(LockType.Claiming
								, line, crossingBoxes[offset], cells, card
								, v, grid);
						if ( hint != null ) {
							result = true; // never say never!
							accu.add(hint);
						}
					}
				} // next value
				endRegion(line); // SiameseLocking
			} // fi
		} // next region1
		return result;
	}

	/**
	 * Create a LockingHint for both pointing and claiming.
	 *
	 * @param type tells you who called me, in error-messages for debugging
	 * @param base the base region. <br>
	 *  For Pointing that's the box. <br>
	 *  For Claiming that's the row/col (aka the line).
	 * @param cover the other region. <br>
	 *  For Pointing that's the row/col (aka the line). <br>
	 *  For Claiming that's the box.
	 * @param cells the cells in the Locked set.
	 * @param n the number of cells in the cells array
	 * @param v the bloody value to be removed from cells
	 * @param grid currently only used for error messages
	 * @return a new LockingHint, else null (nothing to see here (run away))
	 */
	protected final LockingHint createHint(final LockType type
			, final ARegion base, final ARegion cover, final Cell[] cells
			, final int n, final int v, final Grid grid) {
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
			// IGNORE this if BruteForce is in the call-stack (BFIIK)
			assert Debug.isClassNameInTheCallStack(10, "BruteForce")
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
