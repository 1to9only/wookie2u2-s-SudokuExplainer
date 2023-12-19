/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.Frmt.SP;
import static diuf.sudoku.utils.MyMath.powerOf2;
import java.util.Formatter;
import java.util.Locale;


/**
 * Some static String helper methods.
 * @author Keith Corlett 2019 OCT
 */
public final class MyStrings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static String[] splitLines(String lines) {
		if ( lines==null || lines.isEmpty() )
			return new String[0];
		if ( lines.indexOf('\r')<0 && lines.indexOf('\n')<0 )
			return new String[]{lines};
		// matches any and every NL, in order (Dildows, Mac, *nix)!
		return lines.split("\\r\\n|\\r|\\n");
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static final StringBuilder STRING_BUILDER = SB(256); // 256 is just a guess, may grow.
	private static final Locale LOCALE = Locale.getDefault(); // or you could select manually.
	private static final Formatter FORMATTER = new Formatter(STRING_BUILDER, LOCALE);
	/**
	 * Returns a formatted String a bit faster than {@code String.format(...)}.
	 * <p>
	 * Prefer MyStrings.format(...) to String.format(...) especially in a loop.
	 * I cannot believe Java does not have a built-in sprintf, but it does not.
	 * What it has is String.format(...) which creates a Formatter each call,
	 * which means getting the Locale, and it is all a bit overheadish.
	 * <p>
	 * Synchronized internally to avert boot colocation. Costs eff-all in a
	 * single threaded environment. I dunno why Java API programmers did not
	 * synchronize internally, but they did not, so I did.
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

	// ------------------------ ____ing StringBuilder -------------------------

	/**
	 * Returns a new StringBuilder of the given capacity.
	 *
	 * @param initialCapacity the starting size. It can grow from there. Given
	 *  initialCapacity is increased to next power-of-2 to be kind to the GC,
	 *  especially when they are ALL so, to decrease memory fragging. It does
	 *  not matter if a buffer is twice as large as it needs to be. Typically
	 *  its stringified and released. Millions of tiny fragments between used
	 *  blocks gives malloc a headache. 16 is StringBuilder minimum size in SE.
	 *  16 is the default size of a StringBuilder.
	 * <p>
	 * @return a new StringBuilder(initialCapacity)
	 */
	public static StringBuilder SB(final int initialCapacity) {
		return new StringBuilder(powerOf2(Math.max(16, initialCapacity)));
	}

	// ------------------------------ replaceAll ------------------------------

	/**
	 * The Big Buffer Size: 16K. This aims to cover 90% of html files upon
	 * which we call replaceAll, and we do that in
	 * {@link Html#format(java.lang.String, java.lang.Object...) and
	 * {@link Html#colorIn(java.lang.String). It matters not if the buffer
	 * grows to cover the monsters. It matters quite a lot when it's not bog
	 * enough to cover about 90% of use-cases. A little slowness is faster!
	 * Bigger is better up to a point, when it crashes like a mo-fo.
	 * <p>
	 * As an aside: 9 billion is 300 times larger than 300 million, which is
	 * scienctists best guess at the sustainable population level of humans on
	 * planet Earth, given our current (2023) technology set. The global
	 * population cannot mathematically be made to reach ten billion. It just
	 * won't work. Things start going wrong everywhere, leading to the colapse
	 * of life sustaining ecological systems. But I only build mathelogical
	 * models of s__t for a living, so what the hell would I know. Just keep
	 * electing morons like Trump. She'll be right. Sigh.
	 */
	public static final int BIG_BFR_SIZE = 16 * 1024; // must be a power-of-2

	/**
	 * get the cached StringBuilder of atleast BUFFER_SIZE.
	 * @return
	 */
	private static StringBuilder bigSB() {
		if ( bigSb == null )
			bigSb = SB(BIG_BFR_SIZE); // growable!
		else
			bigSb.setLength(0);
		return bigSb;
	}
	private static StringBuilder bigSb;

	/**
	 * Replace all occurrences of 'target' with 'replacement' in 's'.
	 * <p>
	 * I am just a less-fat bastard, so re-implement me if you are really keen.
	 * This method exists because String.replaceAll uses patterns and I need
	 * plain-text. String.replaceAll creates a garbage Pattern and Matcher upon
	 * each invocation, whereas I dont create anything, even re-using a single
	 * StringBuilder field, which costs less overall. String.substring is still
	 * expensive coz he creates a garbage String. A real efficiency nutter
	 * would s.toCharArray() and do it manually with System.arrayCopy; but I am
	 * a sane nutter, so I judge that its just not worth the hassle. The GC is
	 * fast-enough at cleaning-up first-gen Strings, which is no accident.
	 * So ignore me, I just hate Java protecting me from myself by hiding the
	 * char array which underlies a String; and the alternative is roll-my-own
	 * JAPI (unlikely, nonstandard, and Im too stupid). This is non-standard
	 * enough. If its still too slow then go the whole-hog.
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
//debug: its nice to know which one you are upto when debugging.
//		int count = 0;
		for ( int i = -len
			; (i=s.indexOf(target, i+len)) > -1
			; s = bigSB().append(s.substring(0, i))				// prefix
						 .append(replacement)					// replace
						 .append(s.substring(i+len)).toString()	// suffix
			)
				; // intentional null-statement
		// release my buffer between calls
		bigSb = null;
//				++count; //debug: its this OR the null statement.
// there are valid cases where 4-args are passed to HTML which does not contain
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

//not used 2020-10-23 but I am loath to remove any of this crap
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
	 * Returns the index of the second occurrence of $target in $source.
	 *
	 * @param source the string to search
	 * @param target sought
	 * @return index of second target, else -1.
	 */
	public static int indexOfSecond(final String source, final String target) {
		final int i = source.indexOf(target);
		final int j = i>-1 ? source.indexOf(target, i+1) : -1;
		return j > i ? j : i;
	}

	/**
	 * Returns the index of the last occurrence of $target in $s.
	 *
	 * @param s the string to search
	 * @param target sought
	 * @return index of second target, else -1.
	 */
	public static int indexOfLast(String s, char target) {
		final int i = s.indexOf(target);
		int j=i, k=-1;
		while ( (j=s.indexOf(target, j+1)) > -1 )
			k = j;
		return k > i ? k : i;
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
	 * Wrap $s into lines of maximum $len on spaces.
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
		// But its only a starting capacity so dont overthink it + 1!
		final StringBuilder sb = SB(n + n/(len-6)*NL.length() + 1);
		// replace the last space before len in s with a NL,
		// and s becomes the remainder of the string,
		// and around we go again.
		// BUG: broken given a word longer than len!
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

	public static String lineOf(String lines, int i) {
		int start;
		char c;
		int end = lines.indexOf(NL, i);
		if ( end < 0 )
			end = lines.length();
		for ( start=end-1; start>-1; --start )
			if ( (c=lines.charAt(start))==13 || c==10 )
				break;
		if ( start < -1 )
			start = -1;
		return lines.substring(start+1, end);
	}

	/**
	 * If your indexes are bad you get null. Simples.
	 *
	 * @param s the string to get a substring from
	 * @param start the startIndex is 0 based
	 * @param end the-one-after the last index you want
	 * @return {@code s.substring(start, end)} else {@code null}
	 */
	public static String substring(String s, int start, int end) {
		try {
			return s.substring(start, end);
		} catch (StringIndexOutOfBoundsException ex) {
			return null;
		}
	}

	/**
	 * If your indexes are bad you get null. Simples.
	 *
	 * @param s the string to get a substring from
	 * @param start the startIndex is 0 based
	 * @return {@code s.substring(start)} else {@code null}
	 */
	public static String substring(String s, int start) {
		try {
			return s.substring(start);
		} catch (StringIndexOutOfBoundsException ex) {
			return null;
		}
	}

	public static boolean any(final String s) {
		return s!=null && !s.isEmpty();
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

	public static String[] leftShift(String[] args) {
		String[] newArgs = new String[args.length-1];
		System.arraycopy(args, 1, newArgs, 0, args.length-1);
		return newArgs;
	}

	/**
	 * Is $target in $options?
	 *
	 * @param target sought
	 * @param options to consider
	 * @return does any option equals target
	 */
	public static boolean in(String target, String[] options) {
		for ( String option : options )
			if ( target.equals(option) )
				return true;
		return false;
	}

	private MyStrings() {}

}
