/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A StringPrintWriter just wraps a PrintWriter (which it extends) around a
 * StringWriter, whose to toString is my toString, so that you can print to
 * a PrintWriter. Remember to flush me, just to be sure, and then toString me
 * to get back what ever you printed as a String. Simples.
 *
 * @author Keith Corlett 2021-12-15
 */
public class StringPrintWriter extends PrintWriter {

	// the most common usage for the StringPrintWriter trick.
	public static String stackTraceOf(Throwable ex) {
		final StringPrintWriter writer = new StringPrintWriter();
		ex.printStackTrace(writer);
		writer.flush();
		return writer.toString();
	}

	public StringPrintWriter() {
		super(new StringWriter());
	}

	@Override
	public String toString() {
		// out is the StringWriter passed to super constructor
		return out.toString();
	}

}
