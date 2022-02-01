/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import static diuf.sudoku.Grid.BY9;

/**
 * A Point is an x, y location (a row and col) in the Grid, in a Symmetry.
 *
 * @author Keith Corlett
 */
public final class Point {

	public final int x;
	public final int y;
	public final int i;

	public Point(final int x, final int y) {
		this.x = x;
		this.y = y;
		this.i = BY9[y] + x;
	}

	// NOTE that Point.toString is only used in debug
	@Override
	public String toString() {
		// The String constructor which doesn't take a copy of the given chars
		// array is package private, which I find really bloody annoying. The
		// JDK developers should trust app-developers in order to empower them,
		// but not trust the morons; so how can a JDK developer differentiate
		// an app-developer from a moron? Probably by how much we abuse JDK
		// developers, for not trusting us! But how to measure that in code?
		if ( ts == null )
			ts = new String(new char[]{(char)('0'+x), ',', (char)('0'+y)});
		return ts;
	}
	private String ts;

}
