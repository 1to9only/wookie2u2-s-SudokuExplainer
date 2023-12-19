/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.SB;

/**
 * Static helper methods for boolean[].
 *
 * @author Keith Corlett 2023-06-18
 */
public class BooleanArrays {

	public static void clear(final boolean[] array) {
		for ( int i=0,n=array.length; i<n; ++i )
			array[i] = false;
	}

	public static boolean isEmpty(final boolean[] array) {
		for ( boolean b : array )
			if ( b )
				return false;
		return true;
	}

	public static int count(final boolean[] booleans) {
		int count = 0;
		for ( boolean b : booleans )
			if ( b )
				++count;
		return count;
	}

	public static boolean equals(final boolean[] left, final boolean[] right) {
		if ( right.length != left.length )
			return false;
		for ( int i=0; i<left.length; ++i )
			if ( right[i] != left[i] )
				return false;
		return true;
	}

	public static String toString(final boolean[] array) {
		StringBuilder sb = SB(array.length);
		for ( int i=0; i<array.length; ++i )
			sb.append(array[i] ? "Y" : ".");
		return sb.toString();
	}

	/**
	 * Returns is any element in 'a' true?
	 *
	 * @param array to examine
	 * @return Is any element in 'a' true?
	 */
	public static boolean any(final boolean[] array) {
		for ( boolean b : array )
			if ( b )
				return true;
		return false;
	}

	private BooleanArrays() { }
}
