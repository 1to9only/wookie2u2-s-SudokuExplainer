/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.NL;
import java.util.Formatter;
import java.util.Locale;
import static diuf.sudoku.utils.Frmt.SP;


/**
 * Some static String helper methods.
 * @author Keith Corlett 2019 OCT
 */
public final class MyStrings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static String[] splitLines(String lines) {
		if ( lines==null || lines.isEmpty() )
			return new String[0];
		if ( lines.indexOf('\r')==-1
		  && lines.indexOf('\n')==-1  )
			return new String[]{lines};
		return lines.split("\\r\\n|\\r|\\n");
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static final StringBuilder STRING_BUILDER = new StringBuilder(256); // 256 is just a guess, may grow.
	private static final Locale LOCALE = Locale.getDefault(); // or you could select manually.
	private static final Formatter FORMATTER = new Formatter(STRING_BUILDER, LOCALE);
	/**
	 * Returns a formatted String a bit faster than {@code String.format(...)}.
	 * <p>
	 * Prefer MyStrings.format(...) to String.format(...) especially in a loop.
	 * I can't believe Java doesn't have a built-in sprintf, but it doesn't.
	 * What it has is String.format(...) which creates a Formatter each call,
	 * which means getting the Locale, and it's all a bit overheadish.
	 * <p>
	 * Synchronized internally to avert boot colocation. Costs eff-all in a
	 * single threaded environment. I dunno why Java API programmers didn't
	 * synchronize internally, but they didn't, so I did.
	 * @param frmt the format string. See {@link java.util.Formatter}.
	 * @param args the arguments array. See {@link java.util.Formatter}.
	 * @return the formatted String.
	 */
	public static String format(String frmt, Object... args) {
		synchronized ( STRING_BUILDER ) {
			STRING_BUILDER.setLength(0);
			// nb: FORMATTER constructor locks it onto STRING_BUILDER
			// nb: explicitly passing the Locale is a tad faster again.
			//     LOCALE (currently) is Locale.getDefault() asat first hit.
			//     If you want a different locale then copy/paste this method
			//     to ${Locale}Format(...) and share SB & FORMATTER;
			//     or maybe copy/paste this whole class to My${Locale} and
			//     have it implement a MyLocale interface, so you can swap
			//     them in/out/around as required.
			FORMATTER.format(LOCALE, frmt, args);
			return STRING_BUILDER.toString();
		}
	}

	// ------------------------------ replaceAll ------------------------------

	// Welcome.html is the largest .html file at just under 2K (1,995 bytes)
	/** The Big Buffer Size: 2048. */
	public static final int BIG_BFR_SIZE = 2 * 1024;

	/**
	 * get the cached StringBuilder of atleast BUFFER_SIZE.
	 * @return
	 */
	public static StringBuilder bigSB() {
		if ( sb == null )
			sb = new StringBuilder(BIG_BFR_SIZE); // growable!
		else
			sb.setLength(0);
		return sb;
	}
	private static StringBuilder sb = null;

	/**
	 * Replace all occurrences of 'target' with 'replacement' in 's'.
	 * <p>
	 * I'm just a less-fat bastard, so re-implement me if you're really keen.
	 * This method exists because String.replaceAll uses patterns and I need
	 * plain-text. String.replaceAll creates a garbage Pattern and Matcher upon
	 * each invocation, whereas I don't create anything, even re-using a single
	 * StringBuilder field, which costs less overall. String.substring is still
	 * expensive coz he creates a garbage String. A real efficiency nutter
	 * would s.toCharArray() and do it manually with System.arrayCopy; but I'm
	 * a sane nutter, so I judge that it's just not worth the hassle. The GC is
	 * fast-enough at cleaning-up first-gen Strings, which is no accident.
	 * So ignore me, I just hate Java protecting me from myself by hiding the
	 * char array which underlies a String; and the alternative is roll-my-own
	 * JAPI (not likely) and be all non-standard. This is non-standard enough.
	 * If it's still too slow then go the whole-hog.
	 *
	 * @param s the "master" string to modify
	 * @param target the substring of s to be replaced
	 * @param replacement the string to replace target with
	 * @return s with all occurrences of target replaced with replacement
	 */
	public static String replaceAll(String s, String target, String replacement) {
		if ( target == null ) {
			assert false : "null target string"; // techies only
			return s;
		}
		final int len = target.length();
		if ( len < 1 ) {
//			throw new IllegalArgumentException("empty target string");
			assert false : "empty target string"; // techies only
			return s;
		}
		int count = 0; // Used for debugging. Ignore the "not used" warning.
		for ( int i = -len
			; (i=s.indexOf(target, i+len)) > -1
			; s = bigSB().append(s.substring(0, i))				// prefix
						 .append(replacement)					// replace
						 .append(s.substring(i+len)).toString()	// suffix	// suffix
			)
				++count; // Used for debugging. It's this or a null statement.
// there are valid cases where 4-args are passed to HTML which doesn't contain
// a {4}, because we pass the same args-list to several different HTML's.
// FYI: I thought about a Log.FYI mode, but quickly gave-it-up as a bad lot.
//		// log a warning when count==0
//		if ( count == 0 )
//			Log.println("FYI: MyStrings.replace: target not found: "+target);
		return s;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * minus returns name minus the ext, if any.
	 * <p>
	 * Think name.minus(ext) which would exist if I wrote the Java API.
	 * @param name
	 * @param ext
	 * @return the prefix of the last ext in name if any, else name
	 */
	public static String minus(String name, String ext) {
		int i = name.lastIndexOf(ext);
		return i>-1 ? name.substring(0, i) : name;
	}

	/**
	 * upto returns the line upto the first c, if any
	 * <p>
	 * Think line.upto(c) which would exist if I wrote the Java API.
	 * @param c
	 * @param line
	 * @return the prefix of the first c in line if any, else line
	 */
	public static String upto(String line, char c) {
		int i = line.indexOf(c);
		return i>-1 ? line.substring(0, i) : line;
	}

//not used 2020-10-23 but I'm loath to remove any of this crap
//	/**
//	 * Returns true if s is null or empty.
//	 * @param s
//	 * @return s==null || s.isEmpty();
//	 */
//	public static boolean isNullOrEmpty(String s) {
//		return s==null || s.isEmpty();
//	}

	public static final String TWO_OR_MORE_SPACES = "  +"; // regex

	/**
	 * Squeeze a multi-line string into one line, reducing all consecutive
	 * spaces to a single space, and trim it.
	 */
	public static String squeeze(String s) {
		return s.replaceAll(NL, SP).replaceAll(TWO_OR_MORE_SPACES, SP).trim();
	}

	/**
	 * Return the word (ending with a space) starting at i from line.
	 */
	public static String word(String line, int i) {
		return line.substring(i, line.indexOf(' ', i));
	}

	/**
	 * Replace the second occurrence of target in s with replacement, else
	 * return s as-is
	 * @param s
	 * @param target
	 * @param replacement
	 * @return
	 */
	public static String replaceSecond(String s, String target, String replacement) {
		int i;
		if ( (i=s.indexOf(target, 0)) > -1
		  && (i=s.indexOf(target, i+1)) > -1 )
			s = s.substring(0, i) + replacement + s.substring(i+target.length());
		return s;
	}

	/**
	 * Wrap the String 's' into lines of maximum 'len' on spaces.
	 *
	 * @param s to be word-wrapped
	 * @param len the length of the line (eg 80)
	 * @return s wrapped into lines of max-length len
	 */
	public static String wordWrap(String s, int len) {
		int n = s.length();
		if ( n < len )
			return s;
		// len-6 is len-12/2, where 12 is arbitraryModalMaxWordLength, ie the
		// length of the longest words that one typically knows how to spell.
		// It's only a StringBuilder starting capacity so don't overthink it.
		final StringBuilder sb = new StringBuilder(n + n/(len-6)*NL.length() + 1);
		// replace the last space before len in s with a NL,
		// and s becomes the remainder of the string,
		// and around we go again.
		for ( int i=len; i>0 && n>len; --i ) {
			if ( s.charAt(i) == ' ' ) {
				sb.append(s.substring(0, i)).append(NL);
				s = s.substring(i+1, n);
				n = s.length();
				i = len + 1;
			}
		}
		sb.append(s); // append the remainder
		return sb.toString();
	}

//	/**
//	 * How many fields are in s if we split it on token?
//	 *
//	 * @param s to read
//	 * @param token to look for
//	 * @return one more than the number of occurrences of token in s.
//	 */
//	public static int numFields(String s, String token) {
//		int count=1, i=-1;
//		while ( (i=s.indexOf(token, i+1)) > 0 ) {
//			++count;
//		}
//		return count;
//	}

	private MyStrings() {}

}
