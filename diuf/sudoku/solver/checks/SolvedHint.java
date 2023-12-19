/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;

/**
 * The hint displayed when the Sudoku is solved.
 *
 * @author Keith Corlett
 */
public class SolvedHint extends AWarningHint {

	public SolvedHint() {
		super(null, new diuf.sudoku.solver.hinters.DummyHinter());
	}

	@Override
	protected String toHtmlImpl() {
		return "<html><body>"
		+ NL + "<h2>Sudoku Solved</h2>"
		+ NL + "<p>The Sudoku has been solved."
		+ NL + "</body></html>";
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(16).append("Sudoku Solved");
	}

}
