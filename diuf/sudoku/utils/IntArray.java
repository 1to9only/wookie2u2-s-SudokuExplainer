/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * A static helper class because int[] has no innate forEach.
 *
 * @author Keith Corlett 2021-11-08
 */
public class IntArray {
	
	public static void each(final int[] a, final IntVisitor v) {
		for ( int i : a )
			v.visit(i);
	}
	
	public static boolean doWhile(final int[] a, final IntFilter f) {
		for ( int i : a )
			if ( !f.accept(i) )
				return false;
		return true;
	}
	
	private IntArray() { } // Never used

}
