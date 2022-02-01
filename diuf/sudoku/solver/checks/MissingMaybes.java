/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * Check that each empty cell has atleast one potential value remaining.
 * <p>
 * {@link Grid#hasMissingMaybes} implements the examination, which I present as
 * an IHinter.
 */
public final class MissingMaybes extends AWarningHinter {

	public MissingMaybes() {
		super(Tech.MissingMaybes);
	}

	/**
	 * Produce a WarningHint when any cell in grid has no potential values
	 * remaining.
	 *
	 * @param grid
	 * @param accu
	 * @return 
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( grid.hasMissingMaybes() ) {
			accu.add(new WarningHint(this, grid.invalidity
					, "MissingMaybes.html"));
			return true;
		}
		return false;
	}

}
