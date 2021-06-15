/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.Set;


/**
 * A hint that is not really a hint for solving a Sudoku, just a message
 * about the Sudoku.
 */
public abstract class AWarningHint extends AHint {

	public AWarningHint(AHinter hinter) {
		super(hinter, AHint.WARNING, null, 0, null, null, null, null, null, null);
	}

	@Override
	public int applyImpl(boolean isAutosolving) {
		return 0;
	}

	@Override
	public int getNumElims() {
		return 0;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return null;
	}

	@Override
	public Pots getReds(int viewNum) {
		return null;
	}

	public Set<Cell> getRedCells() {
		return null;
	}

	@Override
	public String toFullString() {
		return "";
	}

}
