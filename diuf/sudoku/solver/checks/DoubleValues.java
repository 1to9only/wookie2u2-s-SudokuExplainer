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
 * DoubleValues implements the {@link Tech#DoubleValues} Sudoku grid validator.
 * A valid solved Sudoku has exactly one instance of each value in each region,
 * so if a region contains two instances of the same value then raise a warning
 * hint.
 * <p>
 * {@link Grid#firstDoubledValue() } implements this check, I just present its
 * results as an IHinter.
 */
public final class DoubleValues extends AWarningHinter {

	public DoubleValues() {
		super(Tech.DoubleValues);
	}

	/**
	 * Produce a WarningHint when any value appears more than once in any
	 * Region in the grid.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return any hints found.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		final int v = grid.firstDoubledValue();
		if ( v == 0 )
			return false;
		accu.add(new DoubleValuesHint(this, grid.invalidity
				, grid.invalidRegion, v));
		return true;
	}

}
