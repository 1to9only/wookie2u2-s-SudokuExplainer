/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A StringPrintWriter just wraps a PrintWriter (which it extend) around a
 * StringWriter, whose to toString is my toString, so that you can print to
 * a PrintWriter (don't forget to flush, sigh) and then toString me to get
 * back what you printed. Simples.
 *
 * @author Keith Corlett 2021-12-15
 */
public class StringPrintWriter extends PrintWriter {

	private static final StringWriter SW = new StringWriter();

	public StringPrintWriter() {
		super(SW);
	}

	@Override
	public String toString() {
		return SW.toString();
	}

}
