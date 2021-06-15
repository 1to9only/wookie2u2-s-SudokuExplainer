/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Shared implementation for all Warning Hinters: a hinter which produces
 * WarningHint's. Extended by all "pseudo-hinters" which validate the Sudoku
 * puzzle and/or the current grid.
 */
public abstract class AWarningHinter extends AHinter {
	public AWarningHinter(Tech tech) {
		super(tech);
	}
}