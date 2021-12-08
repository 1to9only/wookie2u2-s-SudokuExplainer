/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A hint produced by {NoDoubleValues} when there are two occurrences of one
 * value in a region, to tell the user that they stuffed-up.
 *
 * @author Keith Corlett 2021-07-09 Exhumed from an anonymous implementation in
 *  the  NoDoubleValues class, to make it implements IPretendHint. sigh.
 */
final class NoDoubleValuesHint extends WarningHint implements IPretendHint {

	private final ARegion invalidRegion;
	private final int doubledValue;

	/**
	 * Constructor.
	 * @param hinter this
	 * @param args [String grid.invalidity, ARegion invalidRegion, int doubledValue]
	 */
	NoDoubleValuesHint(AHinter hinter, Object... args) {
		super(hinter, (String)args[0], "NoDoubleValuesHint.html", args);
		this.invalidRegion = (ARegion)args[1];
		this.doubledValue = (int)args[2];
	}

	// override getRedCells to show the GUI user the naughty cells
	@Override
	public Set<Cell> getRedCells() {
		final Set<Cell> result = new LinkedHashSet<>(8, 1F);
		for ( Cell c : invalidRegion.cells )
			if ( c.value == doubledValue )
				result.add(c);
		return result;
	}

	// override getBases to show the GUI user the naughty region
	@Override
	public ARegion[] getBases() {
		return Regions.array(invalidRegion);
	}

	// everything-else is handled by WarningHint, especially toStringImpl.
}
