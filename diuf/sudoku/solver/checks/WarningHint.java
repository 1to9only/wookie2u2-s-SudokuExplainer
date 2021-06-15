/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;

/**
 * A WarningHint is an arbitrary warning or information message.
 */
public class WarningHint extends AWarningHint {

	private final String message;
	private final String htmlFilename;
	private final Object[] args;

	public WarningHint(AHinter hinter, String message
			, String htmlFilename, Object... args) {
		super(hinter);
		this.message = message;
		this.htmlFilename = htmlFilename;
		this.args = args;
	}

	@Override
	public String toStringImpl() {
		return message;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, htmlFilename, args);
	}
}