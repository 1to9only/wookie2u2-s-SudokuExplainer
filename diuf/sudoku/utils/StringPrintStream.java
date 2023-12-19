/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.io.PrintStream;

/**
 * A StringPrintStream just wraps a PrintStream (which it extends) around a
 * StringWriter, whose to toString is my toString, so you can print stdout
 * to a String, in your test-cases, for example. Remember to flush, to be sure,
 * and then toString me to get back whatever you printed as a String. Simples.
 *
 * @author Keith Corlett 2021-12-15
 */
public class StringPrintStream extends PrintStream {

	public StringPrintStream() {
		super(new StringOutputStream());
	}

	@Override
	public String toString() {
		// out is the StringOutputStream passed to super constructor
		return out.toString();
	}

}
