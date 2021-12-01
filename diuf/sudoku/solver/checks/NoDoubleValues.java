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
 * A valid solved Sudoku has exactly one instance of each value in each region,
 * so if a region contains two instances of the same value then raise a warning
 * hint.
 */
public final class NoDoubleValues extends AWarningHinter {

	public NoDoubleValues() {
		super(Tech.DoubleValues);
	}

	/**
	 * Hint when any value appears more than once in any Region.
	 *
	 * @return any hints found.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		final int v = grid.firstDoubledValue();
		final boolean result = v != 0;
		if ( result )
			accu.add(new NoDoubleValuesHint(this, grid.invalidity
					, grid.invalidRegion, v));
		return result;
	}

}
