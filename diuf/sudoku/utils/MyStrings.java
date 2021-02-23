/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Formatter;
import java.util.Locale;


/**
 * Some static String helper methods.
 * @author Keith Corlett 2019 OCT
 */
public final class MyStrings {

	public static final String NL = diuf.sudoku.utils.Frmt.NL;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static String[] splitLines(String lines) {
		if ( lines==null || lines.isEmpty() )
			return new String[0];
		if ( lines.indexOf('\r')==-1
		  && lines.indexOf('\n')==-1  )
			return new String[]{lines};
		return lines.split("\\r\\n|\\r|\\n");
	}

//not used 2020-10-23 but I'm loath to remove any of this crap
//	public static String combine(String[] lines, int first, int n) {
//		StringBuilder sb = new StringBuilder(1024); // big enough, but not too big
//		for ( int i=first; i<n; ++i )
//			sb.append(lines[i]);
//		return sb.toString();
//	}

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
	private static final int BUFFER_SIZE = 2 * 1024;

	// get the "clean" StringBuilder field
	public static StringBuilder getSB() {
		if ( sb == null )
			sb = new StringBuilder(BUFFER_SIZE); // growable!
		else
			sb.setLength(0);
		return sb;
	}
	private static StringBuilder sb = null;

	/**
	 * Replace all occurrences of the String 'target' with 'replacement' in 's'.
	 * I'm just a less-fat bastard, so re-implement me if you're really keen.
	 * This method exists because String.replaceAll uses patterns and I need
	 * plain-text. String.replaceAll creates a garbage Pattern and Matcher for
	 * each invocation, whereas I don't create anything, even re-using a single
	 * StringBuilder field, which costs less overall. String.substring is still
	 * expensive because he creates a garbage String. If you're an efficiency nut
	 * (like me) you'd s.toCharArray() and do it manually with System.arrayCopy;
	 * but I'm a sane nutter so I judge that it's just not worth the hassle. The
	 * GC is fast-enough at cleaning-up first-gen Strings, which is no accident.
	 * Ignore me, I just bloody hate Java protecting me from myself by hiding
	 * the char array which underlies a String; and the alternative is to roll
	 * my own whole JAPI and be all non-standard... not bloody likely! This is
	 * quite non-standard enough. If it's too slow then I'll go the whole-hog.
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
			; s = getSB().append(s.substring(0, i))				// prefix
						 .append(replacement)					// replace
						 .append(s.substring(i+len)).toString()	// suffix
			)
				++count; // Used for debugging. It's this or a null statement.
// there are valid cases where 4-args are passed to HTML which doesn't contain
// a {4}, because we pass the same args-list to several different HTML's.
// FYI: I thought about a Log.FYI mode, but quickly gave it up as a bad lot.
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

	/**
	 * Squeeze a multi-line string into one line, reducing all consecutive
	 * spaces to a single space, and trim it.
	 */
	public static String squeeze(String s) {
		return s.replaceAll(NL, " ").replaceAll("  +", " ").trim();
	}

	/**
	 * Return the word (ending with a space) starting at i from line.
	 */
	public static String word(String line, int i) {
		return line.substring(i, line.indexOf(' ', i));
	}

	private MyStrings() {}

}
