/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.ArrayList;


/**
 * I'm just a bit annoyed at the wastefulness of java.lang.Integer's parsing.
 * @author Keith Corlett 2020 Jan
 */
public final class MyInteger {

	/**
	 * Returns the int value of the substring of line starting at start and
	 * ending at the character before end. Try it, it does make sense.
	 * Equivalent to <pre>{@code
	 * Integer.parse(line.substring(start, end).trim().replaceAll(COMMA,EMPTY_STRING))
	 * }</pre>
	 * except without any temporary bloody Strings.
	 *
	 * <p>
	 * <b>WARNING: Rant Ahead. Language warning.</b>
	 *
	 * Why the hell does the Java API do everything the least bloody efficient
	 * way possible? Benchmark PrintStream.format against GNU's printf one day
	 * and then tell me who's being outrun by his granny? De/serialisation is
	 * a MAJOR component of 90+% apps and this piece of s__t couldn't
	 * outserialise it's grand-mother, literally. Sigh.
	 *
	 * That's it, the next-time Netbeans spazza's I'm gunna ____ing SHOOT IT!
	 * Except Eclipse is even worse! C# anyone? Going once! Going twice! Sold
	 * to the mongtard who's utterly beholden to the Sith for the rest of his
	 * miserable ____ing life. Sorry emperor. Yes emperor. Oh, No workie on
	 * Death Star 10! Have you considered upgrading? Did I mention the X-Wing
	 * package deal? And would you like to be fried with that?
	 *
	 * I just wish they would make the core Java API <b>efficient</b>. 1.8's
	 * HashMap is slower than 1.7's, so why publish it? Pure incompetence!
	 *
	 * @param line the String to parse
	 * @param begin 0-based index of the start of the string to parse out of
	 * line, inclusive
	 * @param end 0 based index of the end of the string to parse out of line,
	 *  <b>EXCLUSIVE</b>
	 * @return the int value of the substring.
	 */
	public static int parse(String line, int begin, int end) {
		boolean isNegative = false;
		char ch;
		int value = 0;
		for ( int i=begin; i<end; ++i )
			if ( (ch=line.charAt(i)) == '-' )
				isNegative = true;
			else if ( ch>='0' && ch<='9' ) // skips any spaces and commas
				value = value*10 + ch-'0';
		return isNegative ? -value : value; // let it autobox
	}
	public static int parse(String s) {
		return parse(s, 0, s.length());
	}
	public static int parse(String s, int i) {
		return parse(s, i, s.length());
	}

	public static ArrayList<Integer> list(int... ints) {
		ArrayList<Integer> list = new ArrayList<>(ints.length);
		for ( int i : ints )
			list.add(i);
		return list;
	}

	private MyInteger() {}
}
