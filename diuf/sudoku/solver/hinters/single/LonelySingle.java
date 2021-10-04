/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.HintsAccumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * LonelySingle implements the Naked Hidden Single Sudoku solving technique,
 * ie naked singles that are the only unset Cell in a Box.
 * <p>
 * NOTE: I tried seeking LonelySingle's in all regions (box, row, and col) but
 * found the results pretty bloody confusing, so restricted it to Boxes only,
 * so it jumps around less, so it's more predictable.
 */
public final class LonelySingle extends AHinter {

	public LonelySingle() {
		super(Tech.LonelySingle);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// It's faster to de-activate me instead of relying on this check!
		//
		// I only work in the GUI! ie when passed a (Default)HintsAccumulator.
		// Other than that we rely on the NakedSingle hinter, which finds all
		// my hints, it just does so in a different order. I find the topest-
		// leftest (by box) LONELY NakedSingle first, that's all.
		// Ergo: I'm complete waste of time if you don't care about the order
		// that NakedSingles are filled-in in. In in... there's an out there.
		if ( !(accu instanceof HintsAccumulator) )
			return false;

		ARegion box;
		Cell cell;
		int value;
		// presume that no hints will be found.
		boolean result = false;
		// Note that loneliness is only considered in Boxs, not rows/cols; and
		// not for the last box, which is left unfilled so that isFull has less
		// cells to look-at, and is therefore a few nanoseconds faster.
		//
		// If we search rows and cols as well the resulting order of solve is
		// confusing to me (as the user), so I presume it'll be confusing to
		// other users; or more likely most of them will just never think about
		// it, ie accept that it is how it is, despite it being confusing, and
		// so never complain. I complain, therefore I fix it. Sigh.
		for ( int i=0; i<8; ++i  )
			// hint if there is 1 empty cell remaining in this region
			if ( (box=grid.boxs[i]).emptyCellCount == 1 ) {
				// the cell is the first (and only) empty cell in this region
				// the value is it's first (and only) potential value
				value = VFIRST[(cell=box.firstEmptyCell()).maybes];

				// late-validate the code (like Cell.set) that updates maybes
				// when the Grid changes. Better late than never. It's REALLY
				// hard to find the line that buggers-up the maybes! If that
				// happens try putting validation code into a method to call
				// after each update (like Grid.iAmInMyRegionsIndexesOf).
				//
				// <I am the Cell> Be the Cell. Humina humina humina.
				// check I have one potential value (ie I'm naked).
				assert cell.size == 1;
				// check that my maybes ARE that value
				assert cell.maybes == VSHFT[value];
				// check region has one position for value (ie I'm hidden).
				assert box.indexesOf[value].size == 1;
				// check I am the regions position for value.
				assert box.indexesOf[value].bits == ISHFT[cell.indexIn[box.typeIndex]];
				// </I am back to being me> Sigh.

				// nb: [Default]HintsAccumulator.add never returns true, and
				// LonelySingle is used only in the GUI, so don't bother with
				// the usual exit-early upon first hint.
				// nb: The hints-type-name is hinter.tech.name() where hinter
				// is NakedSingle or LonelySingle, so only one hint-type is
				// required (NakedSingleHint). It just reports itself as two
				// different types, that's all. Confused yet? Read it again.
				accu.add(new LonelySingleHint(this, cell, value));
				result = true; // We found at least one hint
			}
		return result;
	}

}
