/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.NL;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.Objects;

/**
 * An Arrays helper utility class instead of repeating ourselves.
 * @author Keith Corlett 2013
 */
public final class MyArrays {

	public static int[] clear(final int[] array) {
		clear(array, array.length);
		return array;
	}
	public static void clear(final int[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = 0;
	}

	public static void clear(final Object[] array) {
		clear(array, array.length);
	}
	public static void clear(final Object[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = null;
	}

	public static void clear(final boolean[] array) {
		for ( int i=0,n=array.length; i<n; ++i )
			array[i] = false;
	}

	public static boolean contains(final Object[] cells, final int n, final Object target) {
		for ( int i=0; i<n; ++i )
			if ( cells[i] == target ) // NB: reference equals is OK
				return true;
		return false;
	}

	public static boolean contains(final int[] array, final int n, final int target) {
		for ( int i=0; i<n; ++i )
			if ( array[i] == target ) // NB: reference equals is OK
				return true;
		return false;
	}

	public static String[] leftShift(String[] args) {
		String[] newArgs = new String[args.length-1];
		System.arraycopy(args, 1, newArgs, 0, args.length-1);
		return newArgs;
	}

	public static int[] clone(int[] src, int n) {
		int[] copy = new int[n];
		for ( int i=0; i<n; ++i )
			copy[i] = src[i];
		return copy;
	}

	/**
	 * The in method does a simple O(n) linear search of array for target,
	 * using reference-equals (identity), so the target must be the same
	 * instance that is in the array, not another instance that equals it.
	 * <p>
	 * This method exists because java.util.Arrays.binarySearch assumes array
	 * is sorted ascending, and if it isn't then it's probably faster to just
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
	static boolean in(Object[] array, Object target) {
		for ( Object i : array )
			if ( i == target )
				return true;
		return false;
	}

	/**
	 * Returns is any element in 'a' true?
	 *
	 * @param a to examine
	 * @return Is any element in 'a' true?
	 */
	public static boolean any(final boolean[] a) {
		for ( boolean b : a )
			if ( b )
				return true;
		return false;
	}

	public static void hitRate(PrintStream out, int[][] hit, int[][] cnt) {
		for ( int i=0,I=hit.length; i<I; ++i ) {
			out.format("%3d: ", i);
			final int J = hit[i].length;
			if ( J > 0 ) {
				out.format("%7.3f", ((double)hit[i][0]/cnt[i][0])*100);
				for ( int j=1; j<J; ++j ) {
					out.format(", %7.3f", ((double)hit[i][j]/cnt[i][j])*100);
				}
			}
			out.append(NL);
		}
	}

	public static String format(final long[] a, final String sep, final String format) {
		return format(a, sep, sep, format);
	}

	public static String format(final long[] a, final String sep, final String lastSep, final String format) {
		if ( a == null )
			return "null";
		if ( a.length == 0 )
			return "empty";
		final StringBuilder sb = new StringBuilder(a.length * 12); // just guess
		final Formatter formatter = new Formatter(sb);
		for ( int i=1,n=a.length,m=n-1; i<n; ++i ) {
			if ( i > 0 )
				if ( i < m )
					sb.append(sep);
				else
					sb.append(lastSep);
			formatter.format(format, a[i]);
		}
		return sb.toString();
	}

	/**
	 * Does the master array containsAll of the targets. <br>
	 * If targets is empty then ALWAYS return true; <br>
	 * If master is empty then ALWAYS returns false; <br>
	 * which are both correct, I guess, but look out for them anyway.
	 *
	 * @param master
	 * @param masterSize
	 * @param targets
	 * @param targetsSize
	 * @return
	 */
	public static boolean containsAll(final Object[] master, final int masterSize, final Object[] targets, final int targetsSize) {
		OUTER: for ( int ti=0; ti<targetsSize; ++ti ) {
			for ( int mi=0; mi<masterSize; ++mi ) {
				if ( Objects.equals(master[mi], targets[ti]) ) {
					continue OUTER;
				}
			}
			return false;
		}
		return true;
	}

	private static StringBuilder sbFor(final int[][] matrix) {
		try {
			return new StringBuilder(12*matrix.length*matrix[0].length);
		} catch (Exception eaten) {
			return new StringBuilder(1024);
		}
	}
	public static String toString(final int[][] matrix) {
		final StringBuilder sb = sbFor(matrix);
		boolean first = true;
		for ( int[] line : matrix ) {
			if(first) first=false; else sb.append(NL);
			sb.append(java.util.Arrays.toString(line));
		}
		return sb.toString();
	}

}
