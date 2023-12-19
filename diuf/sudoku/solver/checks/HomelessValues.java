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
 * HomelessValues implements the {@link Tech#HomelessValues} Sudoku grid
 * validator. I check that no unplaced value has no remaining places in any
 * region; ie foreach region, foreach unplaced value: check this value has
 * at-least one possible place in this region, ie there is a cell in this
 * region which maybe v.
 * <p>
 * {@link Grid#hasHomelessValues() } implements the check. All I do is present
 * its results as an IHinter.
 */
public final class HomelessValues extends AWarningHinter {

	public HomelessValues() {
		super(Tech.HomelessValues);
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		if ( grid.hasHomelessValues()) {
			accu.add(new WarningHint(this, grid.invalidity
					, "HomelessValues.html", grid.invalidity));
			return true;
		}
		return false;
	}

}
