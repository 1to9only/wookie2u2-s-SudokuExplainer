/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.SB;
import java.io.IOException;
import java.io.OutputStream;

/**
 * I want a PrintStream that prints to String.
 *
 * @author Keith Corlett 2023-05-16
 */
class StringOutputStream extends OutputStream {
	private final StringBuilder sb = SB(256);
	@Override
	public void write(int b) throws IOException {
		sb.append((char)b);
	}
	@Override
	public String toString() {
		return sb.toString();
	}
}
