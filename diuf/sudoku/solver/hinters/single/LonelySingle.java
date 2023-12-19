/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BOX;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * LonelySingle implements the {@link Tech#LonelySingle} Sudoku solving
 * technique, ie naked singles that are the only unset Cell in a Box.
 * <p>
 * LonelySingles are a wank! Using LonelySingles finds no more hints, it just
 * effects the order in which hints are found.
 * <p>
 * NOTE: I tried seeking LonelySingles in all regions (box, row, and col) but
 * found the results pretty bloody confusing, so restricted it to Boxes only,
 * so it jumps around less, so it is more predictable.
 */
public final class LonelySingle extends AHinter {

	public LonelySingle() {
		super(Tech.LonelySingle);
	}

	/**
	 * Find all LonelySingle in this grid.
	 *
	 * @param grid to search
	 * @param accu to which I add hints
	 * @return any found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// presume that no hints will be found.
		boolean result = false;

		// It is faster to de-activate me instead of relying on this check.
		// LonelySingle is designed for the GUI, ie not SingleHintsAccumulator.
		// Else rely on NakedSingle, which finds all LonelySingles, it just
		// does so in a different order. I find the topest-leftest (by box)
		// LONELY NakedSingle first, that is all. Ergo: I am a waste of time
		// unless you care about the order in which NakedSingles are found,
		// which means only in the GUI, as determined by the accu type.
		if ( accu instanceof SingleHintsAccumulator )
			return result;

		ARegion box;
		Cell cell;
		int value;
		// Note that loneliness is only considered in Boxs, not rows/cols;
		// and not for the last box, which is left unfilled so that isFull
		// has less cells to look-at, and is therefore a bit faster.
		//
		// If we search rows and cols as well the resulting solve order
		// confuses me, and I presume it will be confusing to users; or more
		// likely most of them will simply accept that it is how it is,
		// despite it being mad-ass confusing, and never complain.
		for ( int i=0; i<8; ++i  ) { // 8 is not the last box!
			// hint if there is ONE empty cell remaining in this region
			if ( (box=grid.boxs[i]).emptyCellCount == 1 ) {
				// FOUND a LonelyNakedHiddenSingle! So ____ it!

				// the cell is the first (and only) empty cell in region
				// the value is it is first (and only) potential value
				value = VFIRST[(cell=box.firstEmptyCell()).maybes];

				// late-validate the code (under Cell.set) updating maybes.
				// I have one potential value, ergo I am naked
				assert cell.size == 1;
				// region has one position for value, ergo I am hidden
				assert box.numPlaces[value] == 1;
				// my maybes ARE my value (sigh)
				assert cell.maybes == VSHFT[value];
				// I am the regions position for value
				assert box.places[value] == cell.placeIn[BOX];

				// We found one-or-more hints
				result = true;

				// construct a new LonelySingleHint and add it to accu.
				// nb: LonelySingle only in GUI, so no "batch" exit-early.
				accu.add(new LonelySingleHint(grid, this, cell, value));
			}
		}
		return result;
	}

}
