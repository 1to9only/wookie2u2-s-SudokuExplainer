/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.utils.StringPrintStream;
import java.io.PrintStream;

/**
 * Static helper methods for StackTrace.
 *
 * @author Keith Corlett 2023-09-10
 */
class StackTrace {

	public static String of(final Exception ex) {
		final PrintStream bfr = new StringPrintStream();
		ex.printStackTrace(bfr);
		return bfr.toString();
	}
}
