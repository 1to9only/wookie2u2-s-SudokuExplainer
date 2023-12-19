/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * MissingMaybes implements the {@link Tech#MissingMaybes} Sudoku grid
 * validator. I check that every empty cell has atleast one maybe remaining.
 * <p>
 * {@link Grid#hasMissingMaybes} implements this check, which I present as an
 * IHinter.
 */
public final class MissingMaybes extends AWarningHinter {

	public MissingMaybes() {
		super(Tech.MissingMaybes);
	}

	/**
	 * Produce a WarningHint when any cell in grid has no potential values
	 * remaining.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return were any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		if ( grid.hasMissingMaybes() ) {
			accu.add(new WarningHint(this, grid.invalidity, "MissingMaybes.html"));
			return true;
		}
		return false;
	}

}
