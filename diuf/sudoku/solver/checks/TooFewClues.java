/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * Produce a WarningHint if the Sudoku doesn't have at least 17 clues.
 */
public final class TooFewClues extends AWarningHinter {

	public TooFewClues() {
		super(Tech.TooFewClues);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		final int count = grid.countFilledCells();
		assert count != 81; // don't validate when the grid.isFull() ya dummy
		if ( count < 17 ) {
			accu.add(new WarningHint(this
					, "Number of clues "+count+" is less than 17"
					, "TooFewClues.html", count));
			return true;
		}
		return false;
	}

}
