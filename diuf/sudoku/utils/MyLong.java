/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 *
 * @author Keith Corlett 2020 Feb
 */
public final class MyLong {

	/**
	 * Returns the long value of the substring of line starting at start and
	 * ending at the character before end, ignoring any commas (or indeed any
	 * non-digit characters) so that "%,d" formatted strings do NOT throw a
	 * NumberFormatException.
	 * <p>
	 * This method is (roughly ~ I am naive) equivalent to
	 * {@code Long.parseLong(line.substring(start, end).replaceAll(",",""))}
	 * but without all the temporary Strings.
	 * <p>
	 * Also note that no warranty is made of the performance of this method
	 * verses Long.parseLong, which may actually be quicker, despite all those
	 * temporary bloody Strings.
	 * 
	 * @param line the String to parse
	 * @param begin the inclusive 0-based index of the start of the string to
	 * parse from line; normally 0
	 * @param end the <b>EXCLUSIVE</b> 0 based index of the end of the string
	 * to parse from line; normally line.length()
	 * @return the long value of the substring.
	 */
	public static long parse(String line, int begin, int end) {
		char ch;
		long value = 0;
		boolean isNegative = false;
		for ( int i=begin; i<end; ++i )
			if ( (ch=line.charAt(i)) == '-' )
				isNegative = true;
			else if ( ch>='0' && ch<='9' ) // skips any spaces and commas
				value = value*10 + ch-'0';
		return isNegative ? -value : value; // let it autobox
	}
	/**
	 * Returns the long value of the String s.
	 * @param s the String to parse into a long value
	 * @return {@code parse(s, 0, s.length())}
	 */
	public static long parse(String s) {
		return parse(s, 0, s.length());
	}
	private MyLong() {}
}
