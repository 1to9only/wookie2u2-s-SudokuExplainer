/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.MyStrings.SB;
import static diuf.sudoku.utils.MyStrings.substring;
import java.io.PrintStream;
import java.util.StringTokenizer;


/**
 * A static helper class because int[] has no innate forEach.
 *
 * @author Keith Corlett 2021-11-08
 */
public class IntArray {

	/** THE empty int[0]. */
	public static final int[] THE_EMPTY_INT_ARRAY = new int[0];

	public static void clear(final int[] array) {
		clear(array, array.length);
	}

	public static void clear(final int[] array, final int n) {
		for ( int i=0; i<n; ++i )
			array[i] = 0;
	}

	public static int[] clone(final int[] array, final int n) {
		final int[] copy = new int[n];
		for ( int i=0; i<n; ++i )
			copy[i] = array[i];
		return copy;
	}

	public static Integer[] integerArray(final int[] array) {
		final int n = array.length;
		final Integer[] results = new Integer[n];
		for ( int i=0; i<n; ++i )
			results[i] = array[i];
		return results;
	}

	public static void each(final int[] array, final IntVisitor visitor) {
		for ( int item : array )
			visitor.visit(item);
	}

	public static boolean doWhile(final int[] array, final IntFilter filter) {
		for ( int item : array )
			if ( !filter.accept(item) )
				return false;
		return true;
	}

	/**
	 * Return s sans leading [ and trailing ], if any.
	 *
	 * @param s to strip: NOT null, "null", "", or "[]".
	 *
	 * @return s sans leading [ and trailing ], if any
	 */
	private static String strip(final String s) {
		final boolean prefix = s.startsWith("[");
		final boolean suffix = s.endsWith("]");
		if ( prefix )
			if ( suffix )
				// nb: substring is my wrapper for String.substring
				return substring(s, 1, s.length()-1);
			else
				return substring(s, 1, s.length());
		else if ( suffix )
			return substring(s, 0, s.length()-1);
		return s;
	}

	/**
	 * returns a new int[]{0, 1, 2} from "[0, 1, 2]", <br>
	 * ie the format produced by {@link java.util.Arrays.toString}.
	 *
	 * @param s the String to parse
	 * @throws NumberFormatException, or RuntimeException from StringTokeniser
	 * @return a new int array; else throws
	 */
	public static int[] parse(final String s) {
		if ( s==null || s.equals(Frmt.NULL_STRING) )
			return null;
		if ( s.length()==0 || s.equals(Frmt.EMPTY_ARRAY) )
			return THE_EMPTY_INT_ARRAY;
		final String stripped = strip(s);
		if ( stripped == null )
			return null; // SIOOBE in substring
		final StringTokenizer st = new StringTokenizer(stripped, ",");
		final int[] result = new int[st.countTokens()];
		int i = 0;
		while ( st.hasMoreTokens() )
			result[i++] = Integer.parseInt(st.nextToken().trim());
		return result;
	}

	/**
	 * return a String representing the int-array $values.
	 * <p>
	 * Currently just delegates to {@link java.util.Arrays#toString(int[]) }
	 * so this method just exists to keep it next to {@link #parse(String)}
	 * because they need to support the same format.
	 *
	 * @param array
	 * @return
	 */
	public static String format(final int[] array) {
		return java.util.Arrays.toString(array);
	}

	public static boolean isEmpty(final int[] array) {
		for ( int i : array )
			if ( i != 0 )
				return false;
		return true;
	}

	public static int sum(final int[] array) {
		int sum = 0;
		for ( int i : array )
			sum += i;
		return sum;
	}

	public static boolean equals(final int[] array, final int b[]) {
		if ( array.length != b.length )
			return false;
		for ( int i=0,n=array.length; i<n; ++i )
			if ( array[i] != b[i] )
				return false;
		return true;
	}

	// nb: this method exists to ensure consistency!
	public static int[] copyOf(final int[] array, final int n) {
		return java.util.Arrays.copyOf(array, n);
	}

	// nb: this method exists to ensure consistency!
	public static String toString(final int[] array) {
		return java.util.Arrays.toString(array);
	}

	public static String toString(final int[] array, final int start, final int end) {
		if ( array==null || start<0 || end<0 || end>=array.length || start>end )
			return "";
		final StringBuilder sb = SB((end-start)<<4); // *16
		sb.append(array[start]);
		for ( int i=start+1; i<=end; i++ )
			sb.append(", ").append(array[i]);
		return sb.toString();
	}

	// ----------------- matrix: an array of int arrays -----------------

	public static void clear(final int[][] matrix) {
		final int n = matrix.length;
		for ( int i=0; i<n; ++i )
			clear(matrix[i]);
	}

	public static boolean isEmpty(final int[][] matrix) {
		for ( int i=0; i<matrix.length; ++i )
			// nb: the empty array is also NOT null!
			if ( matrix[i] != null )
				return false;
		return true;
	}

	private static StringBuilder sb(final int[][] matrix) {
		try {
			return SB((matrix.length<<4) * matrix[0].length);
		} catch ( Exception eaten ) {
			return SB(1024);
		}
	}

	public static String toString(final int[][] matrix) {
		if ( matrix == null )
			return Frmt.NULL_STRING;
		if ( matrix.length < 1 )
			return "";
		final StringBuilder sb = sb(matrix);
		for ( int n=matrix.length,i=0; i<n; ++i )
			sb.append(i).append(": ").append(toString(matrix[i])).append(NL);
		return sb.toString();
	}

	public static void hitRate(PrintStream out, int[][] hit, int[][] cnt) {
		for ( int i=0,I=hit.length; i<I; ++i ) {
			out.format("%3d: ", i);
			final int J = hit[i].length;
			if ( J > 0 ) {
				out.format("%7.3f", ((double)hit[i][0]/cnt[i][0])*100);
				for ( int j=1; j<J; ++j )
					out.format(", %7.3f", ((double)hit[i][j]/cnt[i][j])*100);
			}
			out.append(NL);
		}
	}

	private IntArray() { } // Never used

}
