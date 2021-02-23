/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;


public final class Point {

	public final int x;
	public final int y;
	public final String s;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
		this.s = "" + x + "," + y;
	}

	@Override
	public String toString () {
		return this.s;
	}
}