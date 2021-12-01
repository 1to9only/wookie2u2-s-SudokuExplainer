/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.MIN_CLUES;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * A valid Sudoku has atleast 17 clues. If the grid contains less than 17
 * cells with a non-zero value then produce a warning hint.
 */
public final class InsufficientClues extends AWarningHinter {

	public InsufficientClues() {
		super(Tech.TooFewClues);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( grid.enoughClues )
			return false;
		final int n;
		if ( (n=grid.numSet) >= MIN_CLUES ) {
			grid.enoughClues = true;
			return false;
		}
		accu.add(new WarningHint(this
				, "Number of clues "+n+" is less than "+MIN_CLUES
				, "TooFewClues.html", n));
		return true;
	}

}