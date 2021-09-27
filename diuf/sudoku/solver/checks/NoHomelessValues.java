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
 * NoHomelessValues validates that no unplaced value has no remaining possible
 * positions in any region, ie foreach region, foreach unplaced value: check
 * that this value has at-least one possible position in this region.
 * <p>
 * Note: grid now implements the actual examination in the hasHomelessValues
 * method, so all this class need do is present it's results as an IHinter.
 */
public final class NoHomelessValues extends AWarningHinter {

	public NoHomelessValues() {
		super(Tech.NoHomelessValues);
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
