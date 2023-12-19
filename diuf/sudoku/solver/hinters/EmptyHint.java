/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;

/**
 * The Empty Hint.
 *
 * @author Keith Corlett 2023-09-16 exhumed from SudokuFrame
 */
public class EmptyHint extends AWarningHint implements IPretendHint {

	private final String msg;

	public EmptyHint(final String msg) {
		super(null, new DummyHinter());
		this.msg = msg;
	}

	public EmptyHint() {
		this("");
	}

	@Override
	protected String toHtmlImpl() {
		return "<html><body>Coodies! " + msg + "</body></html>";
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(32).append("EmptyHint: is empty");
	}

}
