/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;


/**
 * A WarningHint is a warning or information message, not a "real" hint,
 * ergo my apply method is a no-op.
 */
public class WarningHint extends AWarningHint implements IPretendHint {

	private final String message;
	private final String htmlFilename;
	private final Object[] args;

	public WarningHint(IHinter hinter, String message
			, String htmlFilename, Object... args) {
		super(null, hinter);
		this.message = message;
		this.htmlFilename = htmlFilename;
		this.args = args;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(message.length()).append(message);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, htmlFilename, args);
	}

}
