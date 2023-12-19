/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Tech;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * Shared implementation for all Warning Hinters: a hinter that produces
 * AWarningHint's to report a problem. AWarningHinter is extended by all
 * "pseudo-hinters" that validate the Sudoku puzzle and/or grid.
 * <p>
 * Note that I extend AHinter, but am not a "real" hinter, which may be
 * differentiated by instanceof AWarningHinter, and hints by instanceof
 * AWarningHint. Not all AWarningHint's have a "valid" hinter.
 */
public abstract class AWarningHinter extends AHinter {

	public AWarningHinter(Tech tech) {
		super(tech);
	}

}
