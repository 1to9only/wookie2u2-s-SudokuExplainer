/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;

/**
 * The hint to display when the Sudoku is solved. Note that a SolutionHint is
 * different (shown by the "Solve" (F8) menu item). This one just says "it's
 * already solved numbnuts."
 *
 * @author Keith Corlett
 */
public class SolvedHint extends AWarningHint {

	public SolvedHint() {
		super(new diuf.sudoku.solver.hinters.DummyHinter());
	}

	@Override
	protected String toHtmlImpl() {
		return "<html><body>"
		+ NL + "<h2>Sudoku Solved</h2>"
		+ NL + "<p>The Sudoku has been solved."
		+ NL + "</body></html>";
	}

	@Override
	protected String toStringImpl() {
		return "Sudoku Solved";
	}

}
