/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.MyStrings.SB;
import java.util.Locale;

/**
 * Static helper methods for long[].
 *
 * @author Keith Corlett 2023-08-05
 */
public class LongArrays {

	public static boolean isEmpty(long[] a) {
		for ( int i=0,n=a.length; i<n; ++i )
			if ( a[i] != 0L )
				return false;
		return true;
	}

	public static String format(final long[] a, final String sep, final String format) {
		return format(a, sep, sep, format);
	}

	public static String format(final long[] a, final String sep, final String lastSep, final String format) {
		if ( a == null )
			return "null";
		if ( a.length == 0 )
			return "empty";
		final StringBuilder sb = SB(a.length<<4); // * 16 is just guess
		final java.util.Formatter formatter = new java.util.Formatter(sb);
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

	public static String toString(final long[] array) {
		if ( array == null )
			return Frmt.NULL_STRING; // "null"
		if ( array.length == 0 )
			return Frmt.EMPTY_ARRAY; // "[]"
		final StringBuilder sb = SB(array.length<<5); // *32
		final java.util.Formatter comma = new java.util.Formatter(sb, Locale.ENGLISH);
		// first element
		sb.append("[");
		comma.format("%,d", array[0]);
		for ( int i=1; i<array.length; ++i ) { // subsequent elements
			sb.append(", ");
			comma.format("%,d", array[i]);
		}
		return sb.append("]").toString();
	}

	public static void main(final String[] args) {
		final long[] array = new long[32];
		final java.util.Random random = new java.util.Random();
		for ( int i=0; i<array.length; ++i )
			array[i] = random.nextLong();
		final String formatted = toString(array);
		System.out.println("array="+formatted);
		System.out.println("a.length="+formatted.length()+" sb.length="+(array.length<<5));
	}
/*
array=[8,772,063,408,684,274,742, 3,249,209,831,615,331,758, 8,756,497,156,101,811,900, 8,402,640,030,266,837,921, 4,959,305,134,071,446,448, -2,101,677,788,286,598,632, -5,757,905,310,217,928,163, -3,425,089,287,561,494,091, -6,339,847,764,215,346,973, -8,251,603,520,700,196,541, -5,510,933,722,870,925,143, -1,926,790,641,925,260,606, -7,780,716,822,208,370,415, -3,470,202,033,203,778,109, -3,674,699,330,186,179,282, -4,158,615,201,547,119,882, -2,774,641,119,515,708,622, 9,174,197,368,159,096,065, 4,310,005,163,751,951,211, -7,722,307,365,113,159,022, 2,471,514,910,245,628,528, 1,460,813,354,524,143,031, -7,582,102,559,168,857,449, 115,173,681,105,218,668, -7,015,412,689,287,065,431, -4,534,324,936,380,004,747, 2,505,860,055,585,413,080, 8,463,149,918,888,254,574, 4,137,281,564,667,971,825, 1,192,731,580,695,966,752, -7,731,428,022,055,165,747, -1,572,118,801,647,397,355]
a.length=880 sb.length=1024
*/

	private LongArrays() { } // Never used

}
