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
 * NoDoubleValues validates that no value appears more than once in a region.
 * <p>
 * Note: the grid implements the actual examination in the firstDoubledValue
 * method, so all this class need do is present it's result as an IHinter.
 */
public final class NoDoubleValues extends AWarningHinter {

	public NoDoubleValues() {
		super(Tech.NoDoubleValues);
	}

	/**
	 * Hint when any value appears more than once in any Region.
	 *
	 * @return any hints found.
	 */
	@Override
	public boolean findHints(final Grid grid, IAccumulator accu) {
		final int doubledValue = grid.firstDoubledValue();
		if ( doubledValue == 0 )
			return false; // it's all good
		// more than one cell in $invalidRegion is set to $doubledValue.
		accu.add(new NoDoubleValuesHint(this, grid.invalidity
				, grid.invalidRegion, doubledValue));
		return true;
	}

}
