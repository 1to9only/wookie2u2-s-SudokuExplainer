/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.solver.hinters.Slots.*;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
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
	 * NOTE WELL: Generate bug with grid field. Just Don't!
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	protected final boolean pointing(final Grid grid, final IAccumulator accu) {
		Box box; // the current Box
		Indexes[] bio; // indexes in box.cells which maybe each value 1..9
		ARegion line; // the region we eliminate from, either a Row or a Col
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
						// if all v's in this box are all in one "slot" then
						// set the offset which defines the correct line.
					    // offset expressions are tautologies: they're in the
					    // if-statement to smash it all into one expression
						// without the expense of invoking a method
						line = null;
						b = bio[v].bits;
						if ( ((b & SLOT1)==b && ((offset=0)==0))
						  || ((b & SLOT2)==b && ((offset=1)==1))
						  || ((b & SLOT3)==b && ((offset=2)==2)) )
							line = grid.rows[box.top + offset];
						else if ( ((b & SLOT4)==b && ((offset=0)==0))
							   || ((b & SLOT5)==b && ((offset=1)==1))
							   || ((b & SLOT6)==b && ((offset=2)==2)) )
							line = grid.cols[box.left + offset];
						// and if that line has other v's then it's a Pointing
						if ( line!=null && line.ridx[v].size>card ) {
						    // FOUND Pointing!
							final AHint hint = createHint(box, line, card, v);
							if ( hint != null ) {
								result = true;
								accu.add(hint);
							}
						}
					}
				}
				endRegion(box); // SiameseLocking
			}
		}
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
		Box[] coxs; // the three crossingBoxs that intersect this row/col
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
				coxs = line.crossingBoxs;
				startRegion(line); // SiameseLocking
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// 2 or 3 cells in line can all be in box; 4+ can't.
					// 1 is hidden single, 0 means v is set, so not my problem.
					if ( (card=rio[v].size)>1 && card<4
					  // if all v's in base are in the same "slot"
					  && ( (((b=rio[v].bits) & SLOT1)==b && (offset=0)==0)
						|| ((b & SLOT2)==b && (offset=1)==1)
						|| ((b & SLOT3)==b && (offset=2)==2) )
					  // and there are some extra v's in the box to be removed
					  && coxs[offset].ridx[v].size > card
					) {
					    // FOUND Claiming!
						final AHint hint = createHint(line, coxs[offset], card, v);
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
	 * @param base the base region. <br>
	 *  For Pointing that's the box. <br>
	 *  For Claiming that's the row/col (aka the line).
	 * @param cover the other region. <br>
	 *  For Pointing that's the row/col (aka the line). <br>
	 *  For Claiming that's the box.
	 * @param card the number of cells in base which maybe v
	 * @param v the bloody value to be removed from cells
	 * @return a new LockingHint, else null (nothing to see here (run away))
	 */
	protected final LockingHint createHint(final ARegion base, final ARegion cover
			, final int card, final int v) {
		final int sv = VSHFT[v]; // shiftedValueToRemove
		// get cells that are Locked into base
		// nb: done manually coz grid.idxs are rooted in generate.
		final Cell[] cells = new Cell[card];
		if ( base.maybe(sv, cells) == card ) {
			// build removable (red) potentials: v's in cover butNot base.
			// nb: done manually coz grid.idxs are rooted in generate.
			final Pots reds = new Pots();
			for ( Cell c : cover.cells )
				if ( (c.maybes & sv)!=0 && !base.contains(c) )
					reds.put(c, sv);
			if ( !reds.isEmpty() ) { // NEVER happens. Never say never.
				eliminationsFound(reds); // call-back LockingSpeedMode
				return new LockingHint(this, v, new Pots(cells, card, v), reds
						, base, cover);
			}
		}
		// Should NEVER happen coz we check that cover has extra idxs before we
		// createHint. Developers investigate. Users no see.
		// BFIIK: Ignore in BruteForce
		assert Debug.isClassNameInTheCallStack(10, "BruteForce")
			: "No elims at "+base+AND+cover+ON+v+IN+Arrays.toString(cells)+NL
				+base.getGrid().toString();
		return null;
	}

}
