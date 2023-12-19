/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.IHinter;
import java.util.Set;

/**
 * A hint that is not really a hint for solving a Sudoku, just a message
 * about the Sudoku or it is state, typically a validation error.
 */
public abstract class AWarningHint extends AHint {

	public AWarningHint(Grid grid, IHinter hinter) {
		super(grid, hinter, AHint.WARNING, null, 0, null, null, null, null, null, null);
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		return 0;
	}

	@Override
	public int getNumElims() {
		return 0;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		return null;
	}

	@Override
	public Pots getRedPots(int viewNum) {
		return null;
	}

	public Set<Integer> getRedBgIndices() {
		return null;
	}

	@Override
	public String toFullString() {
		return "";
	}

}
