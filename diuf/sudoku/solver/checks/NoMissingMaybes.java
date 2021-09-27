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
 * Check that all empty cells have some potential values remaining.
 * <p>
 * Note that the grid now has the hasMissingMaybes method which implements the
 * actual examination of the grid, so all this class need do is wrap that into
 * an implementation of IHinter.
 */
public final class NoMissingMaybes extends AWarningHinter {

	public NoMissingMaybes() {
		super(Tech.NoMissingMaybes);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( grid.hasMissingMaybes() ) {
			accu.add(new WarningHint(this, grid.invalidity
					, "NoMissingMaybes.html"));
			return true;
		}
		return false;
	}

}
