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
 * NoHomelessValues checks that no unplaced value has zero remaining places in
 * any region; ie foreach region, foreach unplaced value: check this value has
 * at-least one possible position in this region.
 * <p>
 * NB: grid now implements the exam, so all I do is present it as an IHinter.
 */
public final class NoHomelessValues extends AWarningHinter {

	public NoHomelessValues() {
		super(Tech.HomelessValues);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( grid.hasHomelessValues()) {
			accu.add(new WarningHint(this, grid.invalidity
					, "NoHomelessValues.html", grid.invalidity));
			return true;
		}
		return false;
	}

}
