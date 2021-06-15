/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;


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

	/** Adds a WarningHint to the HintsAccumulator
	 * if a value appears more than once in any Region.
	 * @return any hints found. */
	@Override
	public boolean findHints(final Grid grid, IAccumulator accu) {

		final int doubledValue = grid.firstDoubledValue();
		if ( doubledValue == 0 )
			return false; // it's all good

		// Crap: more than one cell in $invalidRegion is set to $doubledValue.
		accu.add(new WarningHint(this, grid.invalidity
				, "NoDoubleValues.html", grid.invalidity) {
			// override getRedCells to show the GUI user the naughty cells
			@Override
			public Set<Cell> getRedCells() {
				Set<Cell> result = new LinkedHashSet<>(8, 1F);
				for ( Cell c : grid.invalidRegion.cells )
					if ( c.value == doubledValue )
						result.add(c);
				return result;
			}
			// override getBases to show the GUI user the naughty region
			@Override
			public List<ARegion> getBases() {
				return Regions.list(grid.invalidRegion);
			}
			// all else is handled by WarningHint, especially toStringImpl.
		});
		return true;
	}
}