/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.Col;
import static diuf.sudoku.Grid.FIRST_ROW;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Grid.Row;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import static diuf.sudoku.solver.hinters.Slots.*;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import java.util.Arrays;

/**
 * Locking implements the {@link Tech#Locking} Sudoku solving technique, but my
 * hints are called Pointing and Claiming (not Locking).
 * <p>
 * I call this solving technique "Locking", which I prefer to "Basic Locking"
 * implemented in lock2.LockingGen, because this one is just a bit faster.
 * This matters quite a lot to BruteForce, which hammers me, HARD! BruteForce
 * ALLWAYS uses LockingSpeedMode which extends Locking (me), even when the user
 * selects the "Locking Basic" tech in the GUI. I am also extended by
 * SiameseLocking, but less said about cats in The Centre For Small Mammal
 * Extinction, ie Australia, the better. Sigh.
 * <p>
 * Locking occurs when a candidate is "locked into" a region; ie when these are
 * the only cells in a region that maybe this value then one of them must be
 * that value (because every value must appear in each region), therefore we
 * can eliminate the value from other cells in other region/s that are common
 * to all of these cells.
 * <p>
 * Note that in "standard" Sudoku with region types: box, row, and col there is
 * only one other possible type-of-region common to two-or-more cells. For a
 * box it is either a row or a col; and for a row-or-col it is a box; hence I
 * have decomposed Locking into Pointing and Claiming; because I found the term
 * Locking nonsensical when I started my Sudoku journey (I get it now), so... <br>
 * here I define my preferred "simple" terminology: <ul>
 * <li><b>v</b> for value is the current value that we search for. <br>
 *  The term v is used through-out SudokuExplainer (SE), mostly sans comment.
 * <li><b>Pointing</b> is when all vs in a box also share a row-or-col, <br>
 *  removing all other vs from the row-or-col.
 * <li>Eg: both cells that maybe 5 in box1 are both in colC, <br>
 *  hence one of those two cells must be 5, <br>
 *  therefore no other cell in colC may be 5.
 * <li><b>Claiming</b> is when all vs in a row-or-col also share a box, <br>
 *  removing all other vs from the box.
 * <li>Eg: the three cells that maybe 9 in row1 are all in box2, <br>
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
 * in the batch coz it's slow, and cares not for presentation. <br>
 * NB: The batch has a Siamese switch, to find test-cases. Sigh.
 * <p>
 * LockingGen is an alternate implementation of the Locking technique, but it's
 * a tad slower (despite my best efforts), and doesn't differentiate between
 * Pointing and Claiming, but this differentiation is unnecessary, and stuff
 * should be as simple as possible, so Locking is based on "poor thinking", but
 * it IS faster. Sigh.
 * <p>
 * If you ever want to add additional region types then bin Locking and it's
 * subtypes, and just use LockingGen, it is fast enough, just not quite as fast
 * as this ugly bastard.
 * <p>
 * Note that Locking is swapped on-the-fly, hence nothing in the lock package
 * can be a prepper!
 */
public class Locking extends AHinter implements IFoxyHinter {

	public Locking() {
		super(Tech.Locking);
	}

	/**
	 * Searches the given grid for Pointing (box on row/col) and Claiming
	 * (row/col on box) Hints, which are added to the given HintsAccumulator.
	 * <p>
	 * S__t goes weird in "speed mode" when a Hints<b>Applic</b>umulator was
	 * passed to my Constructor, in which case we apply each hint now (to
	 * autosolve the grid) and pass-back the total elims in a SummaryHint to
	 * getHints-caller via the "normal" HintsAccumulator passed into getHints.
	 * <p>
	 * It is all a bit complicated, so writing an explanation seems harder than
	 * writing the code, and I expect the explanation is probably harder to
	 * understand than the code. Maybe I just suck at explaining stuff. Maybe
	 * my code sucks too. Sigh.
	 * <p>
	 * Note that the HintsApplicumulator has a "debug mode" where it populates
	 * a StringBuilder with the-full-string of each hint applied to the grid.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return was a hint/s found.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// normal mode: batch, test-cases, GUI (wantLess)
		if ( accu.isSingle() ) {
			// batch wants 1 hint so prefer pointing to claiming, using
			// the usual short-circuiting boolean-or operator.
			return pointing(grid, accu) || claiming(grid, accu);
		}
		// GUI wants multiple hints so collect both pointing and claiming with
		// the unusual bitwise-or operator, so they are both always executed.
		return pointing(grid, accu) | claiming(grid, accu);
	}

	/**
	 * Overridden by LockingSpeedMode to handle new eliminations.
	 *
	 * @param gridCells grid.cells of the grid that we are searching
	 * @param reds eliminations
	 */
	protected void eliminationsFound(final Cell[] gridCells, final Pots reds) {
		// Do nothing
	}

	/**
	 * Overridden by SiameseLocking to find hints per region
	 *
	 * @param region
	 */
	protected void startSiamese(ARegion region) {
		// Do nothing
	}

	/**
	 * Overridden by SiameseLocking to find hints per region.
	 *
	 * @param region the region we search
	 * @return should we stop now because we have found the first hint, ie
	 *  accu.isSingle, which is nonsensical when seeking Siamese hints, hence
	 *  my return value is now simply ignored, and I am, officially, a putz!
	 */
	protected boolean endSiamese(ARegion region) {
		return false;
	}

	/**
	 * Pointing: Box => Row/Col.
	 * <pre>
	 * Example: All the cells in box 2 which maybe 9 are in column D,
	 * hence one of those 2-or-3 cells must be a 9, therefore no other
	 * cell in column D can be 9.
	 * </pre>
	 *
	 * @param grid to search
	 * @param accu to which I add hints
	 * @return any found
	 */
	protected final boolean pointing(final Grid grid, final IAccumulator accu) {
		Box box; // the current Box
		ARegion line; // the region we eliminate from, either a Row or a Col
		int numPlaces[] // number of box.cells which maybe each value 1..9
		, places[] // indexes in box.cells which maybe each value 1..9
		, i // region Index
		, v // value
		, card // cardinality: the number of set (1) bits in a bitset.
		, b // bitset box.places mainly coz he wants a nice short name.
		, offset // the 0-based offset of this row/col within this box.
		;
		final Box[] boxs = grid.boxs;
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		// presume that no hints will be found
		boolean result = false;
		// for each of the 9 boxes in the grid
		i = 0;
		do {
			// we need atleast 3 empty cells to form this pattern
			if ( (box=boxs[i]).emptyCellCount > 2 ) {
				startSiamese(box); // call-back SiameseLocking
				places = box.places;
				numPlaces = box.numPlaces;
				// foreach value in 1..9
				v = 1;
				do {
					// 0 means v is already placed in this box;
					// 1 is a HiddenSingle (not my problem);
					// 2 or 3 cells in a box could all be in a row or col; but
					// 4 or more cannot be.
					if ( (card=numPlaces[v])>1 && card<4 ) {
						// if all vs in this box are all in one "slot" then
						// set the offset which defines the correct line.
					    // offset expressions are tautologies: they are in the
					    // if-statement to smash it all into one expression
						// without the expense of invoking a method
						line = null;
						b = places[v];
						if ( ((b & TOP_ROW)==b && ((offset=0)==0))
						  || ((b & MID_ROW)==b && ((offset=1)==1))
						  || ((b & BOT_ROW)==b && ((offset=2)==2)) )
							line = rows[box.top + offset];
						else if ( ((b & LEF_COL)==b && ((offset=0)==0))
							   || ((b & MID_COL)==b && ((offset=1)==1))
							   || ((b & RIG_COL)==b && ((offset=2)==2)) )
							line = cols[box.left + offset];
						// and if that line has other vs then it is a Pointing
						if ( line!=null && line.numPlaces[v]>card ) {
						    // FOUND Pointing!
							final AHint hint = hint(grid, box, line, card, v);
							if ( hint != null ) {
								result = true;
								accu.add(hint); // ignore the retval
							}
						}
					}
				} while (++v < VALUE_CEILING);
				endSiamese(box); // call-back SiameseLocking
			}
		} while (++i < REGION_SIZE);
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
	 * @param grid to search
	 * @param accu to which I add hints
	 * @return any found
	 */
	protected final boolean claiming(final Grid grid, final IAccumulator accu) {
		ARegion boxs[], line; // line.intersectors, row (9..17) or col (18..26)
		Box box; // the box to probably claim from
		AHint hint; // the hint, if any
		int[] numPlaces // num places in line of each v 1..9
		, places; // places in line of each v 1..9
		int i // region Index
		, card // cardinality: the number of elements in a bitset
		, b // rio[v].bits
		, offset; // the offset of the matching box in the grid.boxs array
		final ARegion[] regions = grid.regions;
		// presume that no hints will be found
		boolean result = false;
		// for each row and col (9..26) in the grid
		i = FIRST_ROW;
		do {
			// we need atleast 3 empty cells to form this pattern
			if ( (line=regions[i]).emptyCellCount > 2 ) {
				startSiamese(line); // SiameseLocking
				numPlaces = line.numPlaces;
				places = line.places;
				boxs = line.intersectors;
				for ( int v : VALUESES[line.unsetCands] ) {
					// if there is 2 or 3 cells in this line that maybe v.
					// * 0 means v is set, so skip it.
					// * 1 is a hidden single, which is not my problem.
					// * 2 or 3 cells in a line can all be in the same box.
					// * 4+ cells do not fit in the 3 cell intersection.
					if ( (card=numPlaces[v])>1 && card<4
					  // and all vs in the line are in the same box
					  && ( (((b=places[v]) & TOP_ROW)==b && (offset=0)==0)
						|| ((b & MID_ROW)==b && (offset=1)==1)
						|| ((b & BOT_ROW)==b && (offset=2)==2) )
					  // and there are some extra vs in the box to be removed
					  && (box=(Box)boxs[offset]).numPlaces[v] > card
					  // FOUND Claiming!
					  && (hint=hint(grid, line, box, card, v)) != null ) {
						result = true;
						accu.add(hint); // ignore the retval
						hint = null;
					}
				}
				endSiamese(line); // SiameseLocking
			}
		} while (++i < NUM_REGIONS);
		return result;
	}

	/**
	 * Create a LockingHint for both pointing and claiming.
	 *
	 * @param grid to search
	 * @param base the base region. <br>
	 *  For Pointing that is the box. <br>
	 *  For Claiming that is the row/col (aka the line).
	 * @param cover the other region. <br>
	 *  For Pointing that is the row/col (aka the line). <br>
	 *  For Claiming that is the box.
	 * @param card (cardinality) the number of cells in base which maybe v
	 * @param v the value to be removed from cells
	 * @return a new LockingHint, else null (nothing to see here (run away))
	 */
	protected final LockingHint hint(final Grid grid, final ARegion base
			, final ARegion cover, final int card, final int v) {
		final int sv = VSHFT[v]; // shiftedValue
		// get cells that are Locked into base
		final Cell[] lockedCells = new Cell[card];
		int cnt = 0;
		for ( Cell c : base.cells )
			if ( (c.maybes & sv) > 0 ) {
				if ( cnt < card )
					lockedCells[cnt] = c;
				++cnt;
			}
		if ( cnt == card ) {
			// build red (removable) potentials: vs in cover & ~base.
			final Pots reds = new Pots();
			for ( Cell c : cover.cells )
				if ( (c.maybes & sv) > 0 && !base.contains(c) )
					reds.put(c.indice, sv);
			if ( reds.any() ) { // NEVER empty. Never say never.
				eliminationsFound(grid.cells, reds); // call-back LockingSpeedMode
				final Pots greens = new Pots(lockedCells, card, VSHFT[v], DUMMY);
				return new LockingHint(grid, this, v, greens, reds, base, cover
						, new Idx(lockedCells));
			}
		}
		// NEVER, coz we check that cover has extra idxs before we createHint.
		// Developers look. Users no see. Ignore in BruteForce. BFIIK!
		// J8.2_BUG: assert params are evaluated even when assert not triggered
		if ( Run.ASSERTS_ENABLED
		  && !Debug.isClassNameInTheCallStack(16, diuf.sudoku.solver.checks.BruteForce.class.getSimpleName()) )
			throw new AssertionError("No elims at "+base+AND+cover+ON+v+IN+Arrays.toString(lockedCells)+NL
				+base.getGrid().toString());
		return null;
	}

}
