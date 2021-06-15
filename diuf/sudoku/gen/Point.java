/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

/**
 * A Point is a location (a row and col) in the Grid, in a Symmetry.
 *
 * @author Keith Corlett
 */
public final class Point {

	public final int x;
	public final int y;
	public final String string;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
		// The String constructor which doesn't take a copy of the given chars
		// array is package private, which I find really bloody annoying. The
		// JDK developers must trust app-developers in order to empower them.
		this.string = new String(new char[]{(char)('0'+x), ',', (char)('0'+y)});
	}

	@Override
	public String toString() {
		return this.string;
	}
}