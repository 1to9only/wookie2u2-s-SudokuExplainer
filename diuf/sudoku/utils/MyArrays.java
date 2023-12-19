/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.SB;
import java.util.Objects;

/**
 * Static helper methods for Object arrays.
 * <p>
 * Note that each primitive type: int, long, boolean, String has its own arrays
 * class: ${type}Arrays. Strings can use these methods too, but they are a bit
 * special, hence require there own class. Primitive-wrapper arrays are thrown
 * in with the primitives.
 *
 * @author Keith Corlett 2013
 */
public final class MyArrays {

	public static void clear(final Object[] array) {
		clear(array, array.length);
	}

	public static void clear(final Object[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = null;
	}

	public static void clear(final Object[][] array) {
		for ( int i=0,n=array.length; i<n; ++i )
			clear(array[i], array[i].length);
	}

	public static boolean notNull(Object[] objects, int[] indexes) {
		for ( int index : indexes )
			if ( objects[index] == null )
				return false;
		return true;
	}

	/**
	 * The in method does a simple O(n) linear search of array for target,
	 * using reference-equals (identity), so the target must be the same
	 * instance that is in the array, not another instance that equals it.
	 * <p>
	 * This method exists because java.util.Arrays.binarySearch assumes array
	 * is sorted ascending, and if it is not then it is probably faster to just
	 * search it linearly.
	 * <p>
	 * This method is O(n), so the larger the array the longer I take to not
	 * find the target. Do NOT use this method in anger on big (say more than
	 * 256 elements) arrays.
	 *
	 * @param array to look in
	 * @param target to look for
	 * @return true if target is in array, else false
	 */
	public static boolean in(Object[] array, Object target) {
		for ( Object i : array )
			if ( i == target )
				return true;
		return false;
	}

	/**
	 * Does the master array containsAll of the targets. <br>
	 * If targets is empty then ALWAYS return true; <br>
	 * If master is empty then ALWAYS returns false; <br>
	 * which are both correct, I guess, but look out for them anyway.
	 *
	 * @param master the master list to look in
	 * @param masterSize
	 * @param targets the target list to look for
	 * @param targetsSize
	 * @return
	 */
	public static boolean containsAll(final Object[] master, final int masterSize, final Object[] targets, final int targetsSize) {
		if ( targetsSize > masterSize )
			return false;
		OUTER: for ( int ti=0; ti<targetsSize; ++ti ) {
			for ( int mi=0; mi<masterSize; ++mi )
				if ( Objects.equals(master[mi], targets[ti]) )
					continue OUTER;
			return false;
		}
		return true;
	}

	public static String toString(final String sep, final Object[] array, int n) {
		if ( array == null )
			return Frmt.NULL_STRING;
		if ( array.length == 0 || n < 1 )
			return Frmt.EMPTY_STRING; // not EMPTY_ARRAY. Sigh.
		// we need no StringBuilder for 1.
		if ( array.length == 1 || n == 1 )
			return String.valueOf(array[0]);
		final StringBuilder sb = SB(n<<4); // *16
		sb.append(String.valueOf(array[0])); // first sans sep
		for ( int i=1; i<n; ++i ) // foreach subsequent, with sep
			sb.append(sep).append(String.valueOf(array[i]));
		return sb.toString();
	}

	/**
	 * Formats Asses in ChainerMulti.GWH (GridWatchHint)
	 * but can ?SV any array of Object.
	 *
	 * @param a array of objects to format
	 * @param sep separator printed between elements
	 * @param nullStr printed when an element is null
	 * @return a ?SV string
	 */
	public static String toString(final Object[] a, final String sep, final String nullStr) {
		if ( a==null || sep==null || nullStr==null )
			return "";
		final StringBuilder sb = SB(a.length<<2);
		boolean first = true;
		for ( Object o : a ) {
			if ( first )
				first = false;
			else
				sb.append(sep);
			sb.append(o==null ? nullStr : o);
		}
		return sb.toString();
	}

}
