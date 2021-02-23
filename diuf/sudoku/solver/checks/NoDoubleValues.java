/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
 * This class checks that no value appears more than once in each region.
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

		// Poo: more than one cell in $invalidRegion is set to $doubledValue.
		accu.add(new WarningHint(this, grid.invalidity
				, "NoDoubleValues.html", grid.invalidity) {
			@Override
			public Set<Cell> getRedCells() {
				Set<Cell> result = new LinkedHashSet<>(8, 1F);
				for ( Cell c : grid.invalidRegion.cells )
					if ( c.value == doubledValue )
						result.add(c);
				return result;
			}
			@Override
			public List<ARegion> getBases() {
				return Regions.list(grid.invalidRegion);
			}
		});
		return true;
	}
}