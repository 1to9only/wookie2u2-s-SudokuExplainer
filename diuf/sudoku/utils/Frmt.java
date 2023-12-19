/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.utils.MyStrings.SB;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * My convenient Formatter for collections of stuff
 */
public final class Frmt {

	// NL is \r\n on Winblows; \n on *nix; and \r on appleOS
	// Do NOT get me started on file-path seperator characters. I mean which
	// ____ing inbred chose THE escape character as a file-path-seperator?
	public static final String NL = System.lineSeparator();

	private static final StringBuilder SB = SB(64);
	public static StringBuilder getSB() {
		synchronized( SB ) {
			SB.setLength(0);
			return SB;
		}
	}
	public static StringBuilder getSB(final int initialCapacity) {
		synchronized(SB) {
			SB.setLength(0);
			SB.ensureCapacity(initialCapacity);
			return SB;
		}
	}

	public static final String[] DIGITS = new String[] {
		".", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	public static final String[] LETTERS = new String[] {
		"A", "B", "C", "D", "E", "F", "G", "H", "I"
	};
	public static final String[] NUMBERS = {
		"1", "2", "3", "4", "5", "6", "7", "8", "9"
	};

	/** COLON_SPACE */
	public static final String COLON_SP = ": ";
	public static final String COLON = ":";
	public static final String IN = " in ";
	public static final String ARE = " are ";
	public static final String ON = " on ";
	public static final String SO = " so ";
	public static final String CAUSES = " causes ";
	public static final String NULL_STRING = "null"; // the String containing "null", as apposed to NULL_ST: the null String.
	public static final String PLUS_OR_MINUS = "+/-";
	public static final String EMPTY_ARRAY = "[]";
	public static final String EMPTY_STRING = "";
	/** COMMA_SPACE */
	public static final String CSP = ", ";
	public static final String COMMA = ",";
	public static final String AND = " and ";
	public static final String OR = " or ";
	public static final String SET = " set ";
	/** SPACE */
	public static final String SP = " ";
	public static final String TAB = "\t";
	/** TWO_SPACES */
	public static final String SP2 = "  ";
	public static final String PLUS = "+";
	public static final String MINUS = "-";
	public static final String PERIOD = ".";
	public static final String EQUALS = "=";
	public static final String NOT_EQUALS = "!=";

	private static final StringBuilder MY_SB = SB(64);


	// ======================== Object array IFormatter =======================

	// cast it to Whatever in your Formatter and IFilter implemantations.

	// Unused 2023-10-16: retained anyway, because its a bit clever
	public static <T> String csv(final String prefix, final T[] a, final IFormatter<T> ts) {
		if(a==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		MY_SB.append(prefix);
		return appendTo(MY_SB, a, ts, CSP, CSP).toString();
	}
	// Unused 2023-10-16: retained anyway, because its a bit clever
	public static <T> String csv(final T[] a, final IFormatter<T> ts) {
		if(a==null) return NULL_STRING;
		return frmt(a, ts, CSP, CSP);
	}
	// Unused (except above) 2023-10-16: retained anyway, coz its clever
	public static <T> String frmt(final T[] a, final IFormatter<T> ts, final String sep, final String lastSep) {
		if(a==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendTo(MY_SB, a, ts, sep, lastSep).toString();
	}
	// Unused (except above) 2023-10-16: retained anyway, coz its clever
	public static <T> StringBuilder appendTo(final StringBuilder sb, final T[] a, final IFormatter<T> ts, final String sep, final String lastSep) {
		final int m = a.length - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( T e : a ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(ts.format(e));
		}
		return sb;
	}

	/**
	 * Append ${filter}ed ${a}s in ${formatter} format to $sb, separated by
	 * $sep and $lastSep, return $sb.
	 *
	 * @see diuf.sudoku.solver.LogicalSolver#getWantedEnabledHinterNames
	 *
	 * @param <T> the type variable which constrains validity of the parameters
	 * such they should all work with each other
	 * @param sb typically an instance of StringBuilder (possibly not-empty),
	 *  but any Appendable will do, including a StringPrintStream or the like
	 * @param a array of T's to format
	 * @param filter whose accept method determines if each T is output
	 * @param formatter whose format method determines the presentation of T
	 * @param sep appears in the output between intermediate T's,
	 *  typically {@link Frmt#CSP} (", ")
	 * @param lastSep appears in the output before the last T,
	 *  typically {@link Frmt#AND} (" and "),
	 *  or {@link Frmt#OR} (" or ")
	 * @return the given Appendable, probably with more crap in it
	 * @throws IOException from the "raw" StringBuilder/Appendable (Sigh)
	 */
	public static <T> Appendable appendTo(final Appendable sb, final T[] a
			, final IFilter<T> filter, final IFormatter<T> formatter
			, final String sep, final String lastSep) throws IOException {
		final int m = a.length - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( T e : a )
			if ( filter.accept(e) ) {
				if ( ++i > 1 )
					sb.append(i<m ? sep : lastSep);
				sb.append(formatter.format(e));
			}
		return sb;
	}

	// ======================== Collection IFormatter =========================

	/**
	 * Appends the given String to the given StringBuilder, separating each
	 * value with $sep, except the last which is preceeded by $lastSep.
	 *
	 * @param <E> The Element type of the collection
	 * @param sb StringBuilder to append to
	 * @param c {@code Collection<E>} to be appended
	 * @param ts (toString) whose {@code String format(E e)} method returns a
	 *  String representing each element in the collection
	 * @param sep values separator
	 * @param lastSep the last values separator
	 * @return the given $sb so that you can chain method calls
	 */
	public static <E> StringBuilder appendTo(final StringBuilder sb, final Collection<? extends E> c, final IFormatter<E> ts, final String sep, final String lastSep) {
		final int m = c.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( E e : c ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(ts.format(e));
		}
		return sb;
	}

	public static <E> String frmt(final Collection<? extends E> c, final IFormatter<E> ts, final String sep, final String lastSep) {
		if(c==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendTo(MY_SB, c, ts, sep, lastSep).toString();
	}

	public static <E> String csv(final Collection<? extends E> c, final IFormatter<E> ts) {
		return frmt(c, ts, CSP, CSP);
	}

	// Unused 2023-10-16: retained anyway, coz its clever
	/**
	 * Appends to $out
	 * those elements of $c
	 * which are ${accept}ed by $filter
	 * as a CSV list {$format}ed by the $formatter
	 * and separated by ", " and ", ".
	 *
	 * @param <E>
	 * @param out PrintStream to print to
	 * @param c {@code Collection<? extends E>} to print
	 * @param filter {@code Filter<E>} print elements that are ${accept}ed
	 * @param formatter
	 */
	public static <E> void csv(final PrintStream out, final Collection<? extends E> c, final IFilter<E> filter, final IFormatter<E> formatter) {
		csv(out, c, filter, formatter, CSP, CSP);
	}

	/**
	 * Appends to $out
	 * those elements of $c
	 * which are ${accept}ed by $filter
	 * as a CSV list ${toString}ed by the $formatter
	 * and (separated by $sep and $lastSep,
	 *                ie ", ", ", "
	 *                or ", ", "and ").
	 *
	 * @param <E>
	 * @param out PrintStream to print to
	 * @param c {@code Collection<? extends E>} to print
	 * @param filter {@code Filter<E>} print elements that are ${accept}ed
	 * @param formatter
	 * @param sep
	 * @param lastSep
	 */
	public static <E> void csv(final PrintStream out, final Collection<? extends E> c, final IFilter<E> filter, final IFormatter<E> formatter, final String sep, final String lastSep) {
		final int m = c.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( E e : c ) {
			if ( filter.accept(e) ) {
				if ( ++i > 1 )
					out.append(i<m ? sep : lastSep);
				out.append(formatter.format(e));
			}
		}
	}

	// ============================ String ==============================

	// 2023-06-16 used only in testcases, to test other stuff
	public static String csvs(final Collection<String> c) { return frmts(c, CSP, CSP); }
	public static String ands(final Collection<String> c) { return frmts(c, CSP, AND); }
	public static String ors(final Collection<String> c) { return frmts(c, CSP, OR); }
	public static String frmts(final Collection<String> c, final String sep, final String lastSep) {
		if(c==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendS(MY_SB, c, sep, lastSep).toString();
	}
	/**
	 * Appends the given String to the given StringBuilder, separating each
	 * value with $sep, except the last which is preceeded by $lastSep.
	 *
	 * @param sb to append to
	 * @param c {@code Collection<String>} to format as a list (or whatever)
	 * @param sep values separator
	 * @param lastSep the last values separator
	 * @return the given $sb so that you can chain method calls
	 */
	public static StringBuilder appendS(final StringBuilder sb, final Collection<String> c, final String sep, final String lastSep) {
		final int m = c.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( String s : c ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(s);
		}
		return sb;
	}

	/**
	 * Parse a $sep Separated Values line into a {@code LinkedHashSet<String>}.
	 *
	 * @param line to parse.
	 * @param sep the field separator, typically ", " for CSV.
	 * @return {@code LinkedHashSet<String>} because iteration order is
	 * significant to testing it, even if it is not in using it, which it
	 * almost certainly will be in some future instance, so it is Linked.
	 */
	public static LinkedHashSet<String> parse(final String line, final String sep) {
		if ( line == null || line.isEmpty() )
			return new LinkedHashSet<>(0, 0.75F);
		if ( sep == null )
			throw new IllegalArgumentException("null sep");
		final int sepLen = sep.length();
		if ( sepLen == 0 )
			throw new IllegalArgumentException("bad sep=\"\"");

		final LinkedHashSet<String> result = new LinkedHashSet<>(64, 0.75F);
		final char sep0 = sep.charAt(0);
		// toCharArray is faster than getChar(i) for longer (>32 chars) strings
		// and they are the ones that matter. If short strings are slower then
		// it is still a bounded problem, but if long strings are slower then
		// it is a Microsoft product. Insert new programmer and press any key
		// to continue...
		final char[] chars = line.toCharArray();
		int st = 0; // start: the start of the current element in line.
		switch ( sepLen ) {
		case 1:
			for ( int i=0,n=line.length(); i<n; ++i )
				if ( chars[i] == sep0 ) {
					result.add(new String(chars, st, i-st));
					st = i + 1;
				}
			break;
		case 2:
			final char sep1 = sep.charAt(1);
			// n is population size, m = n - 1.
			for ( int i=0,m=line.length()-1; i<m; ++i )
				if ( chars[i]==sep0 && chars[i+1]==sep1 ) {
					result.add(new String(chars, st, i-st));
					st = i + 2;
				}
			break;
		default: // sep is 3+ chars, so just do some ____ing string maths,
				 // which would have been bloody fast enough anyways, for
				 // small tables of short lists; which is what have I got.
			for ( int i=line.indexOf(sep, st); i>-1; i=line.indexOf(sep, st) ) {
				result.add(new String(chars, st, i-st));
				st = i + sepLen;
			}
		}
		// and do not forget the last one, also handles !line.contains(sep).
		result.add(new String(chars, st, line.length()-st));
		return result;
	}

	/**
	 * This is roughly equivalent to:
	 * {@code line.substring(begin, end).indexOf(c); }
	 * except without the temporary String.
	 *
	 * @param line to look in
	 * @param c to look for
	 * @param begin the 0-based starting index of the search (inclusive)
	 * @param end the 0-based end index of the search (exclusive)
	 * @return the 0-based index of c in line, if that falls at or after begin
	 *  and before end
	 * @throws StringIndexOutOfBoundsException wrap me in a safeIndexOf if you
	 *  cannot workout begin and end reliably. I am too efficient for that s__t.
	 */
	public static int indexOf(final String line, final char c, final int begin, final int end) {
		for ( int i=begin; i<end; ++i )
			if ( line.charAt(i) == c )
				return i;
		return -1;
	}

	// =============================== int... =================================

	/** csv: {1,2,4,0} => "1, 2, 4, 0" */
	public static String csv(final int... vs) { return frmt(vs, CSP, CSP); }
	/** ssv: 3, {1,2,4,0} => "1 2 4" */
	public static String ssv(final int n, final int... vs) { return frmt(vs, n, SP, SP); }
	/** and: {1,2,4,0} => "1, 2, 4 and 0" */
	public static String and(final int... vs) { return frmt(vs, CSP, AND); }
	/** or: {1,2,4,0} => "1, 2, 4 or 0" */
	public static String or(final int... vs) { return frmt(vs, CSP, OR); }
	/** frmt: {1,2,4,0}, ", ", " or "  =>  "1, 2, 4 or 0" */
	public static String frmt(final int[] vs, final String sep, final String lastSep) {
		return frmt(vs, vs.length, sep, lastSep);
	}
	/** frmt: {1,2,4,0}, 3, ", ", " or "  =>  "1, 2 or 4" */
	public static String frmt(final int[] vs, final int n, final String sep, final String lastSep) {
		if(vs==null || vs.length==0 || n==0) return EMPTY_STRING;
		MY_SB.setLength(0);
		return append(MY_SB, vs, n, sep, lastSep).toString();
	}
	// Append the first n values of array to sb, separated by sep/lastSep.
	// This method is private coz it comes with preconditions:
	// 1. sb is not null (you probably need to clear it before you call me)
	// 2. array is not null or empty
	// 3. n is presumed to be greater than 0, not that it really matters
	// 4. sep and lastSep are not null
	// @param sb StringBuilder to append to
	// @param array of int[] to stringify
	// @param n number of array elements to stringify
	// @param sep separator between intermediate elements
	// @param lastSep separator before the last element
	// @return the index of the first 0 in the given int array.
// If you wish to make this method public again then go ahead, and do not forget
// to turn this comment back into javadoc.
	private static StringBuilder append(final StringBuilder sb, final int[] array, final int n, final String sep, final String lastSep) {
		for ( int i=0,m=n-1; i<n; ++i ) {
			if ( i > 0 )
				sb.append(i<m ? sep : lastSep);
			sb.append(array[i]);
		}
		return sb;
	}

	// no n here: I wrote it by accident. I vote to retain.
	public static StringBuilder append(final StringBuilder sb, final int[] array, final String sep, final String lastSep) {
		if(array==null || array.length==0) return sb;
		sb.append(array[0]);
		for ( int i=1, n=array.length, m=n-1; i<n; ++i )
			sb.append(i<m ? sep : lastSep).append(array[i]);
		return sb;
	}

//not used: retained coz its clever
//	/** csv-Null-Terminated-List: {1,2,4,0,0} => "1, 2, 4" */
//	public static String csvNTL(int... vs) { return frmtNTL(vs, comma, comma); }
//	/** and-Null-Terminated-List: {1,2,4,0,0} => "1, 2 and 4" */
//	public static String andNTL(int... vs) { return frmtNTL(vs, comma, and); }
//	/** or-Null-Terminated-List: {1,2,4,0,0} => "1, 2 or 4" */
//	public static String orNTL(int... vs) { return frmtNTL(vs, comma, or); }
//	/** frmt-Null-Terminated-List: {1,2,4,0,0}, ", ", " or "  =>  "1, 2 or 4" */
//	public static String frmtNTL(int[] vs, String sep, String lastSep) {
//		if(vs==null || vs.length==0) return EMPTY_STRING;
//		MY_SB.setLength(0);
//		return append(MY_SB, vs, eoNTL(vs), sep, lastSep).toString();
//	}
//	/** endOfNullTerminatedList returns the index of the first 0 in $vs.
//	 * @param vs int[] to examine
//	 * @return the index of the first 0 in the given int array. */
//	public static int eoNTL(int[] vs) {
//		final int n = vs.length;
//		for ( int i=0; i<n; ++i )
//			if ( vs[i] == 0 )
//				return i;
//		return n;
//	}

	public static int[] toIntArray(final String[] strings) {
		if ( strings==null || strings.length==0 )
			return null;
		final int[] ints = new int[strings.length];
		int cnt = 0;
		for ( String s : strings )
			ints[cnt++] = Integer.parseInt(s);
		return ints;
	}

//RETAIN 2023-06-16 used for debugging and watch expressions
	public static String toBinaryString(final int i, final int len) {
		String s = Integer.toBinaryString(i);
		int sl = s.length();
		// that is 32 0s
		return "00000000000000000000000000000000".substring(0, len-sl) + s;
	}

//RETAIN 2023-06-16 used for debugging and watch expressions
	public static String toBinString(final int i, final int len) {
		String s = Integer.toBinaryString(i);
		int sl = s.length();
		if ( sl > len )
			s = s.substring(sl-len, sl);
		sl = s.length();
		// that is 32 0s
		return "00000000000000000000000000000000".substring(0, len-sl) + s;
	}

	// ============================ Integer ==============================

	/**
	 * Converts {@code Set<String>} into {@code Set<Integer>},
	 * by calling {@code Integer.valueOf(s)} on each.
	 *
	 * @param strings {@code Set<String>}
	 * @return {@code Set<Integer>}
	 * @throws NumberFormatException which is all your problem.
	 */
	public static LinkedHashSet<Integer> integers(final LinkedHashSet<String> strings) {
		final LinkedHashSet<Integer> result = new LinkedHashSet<>(strings.size());
		for ( String s : strings )
			result.add(Integer.valueOf(s));
		return result;
	}

	// ============================ boolean... ==============================

//RETAIN 2020-10-16 just coz I think it should exist
	public static String plain(final boolean... bs) { return frmt(bs, EMPTY_STRING, EMPTY_STRING); }
	public static String csv(final boolean... bs) { return frmt(bs, CSP, CSP); }
	public static String and(final boolean... bs) { return frmt(bs, CSP, AND); }
	public static String or(final boolean... bs) { return frmt(bs, CSP, OR); }
	public static String frmt(final boolean[] bs, final String sep, final String lastSep) {
		MY_SB.setLength(0);
		return append(MY_SB, bs, sep, lastSep).toString();
	}
	public static StringBuilder append(final StringBuilder sb, final boolean[] bs, final String sep, final String lastSep) {
		if(bs==null) return sb;
		for ( int i=0, n=bs.length, m=n-1; i<n; ++i ) {
			if (i > 0)
				sb.append(i<m ? sep : lastSep);
			sb.append(bs[i]?'T':'F');
		}
		return sb;
	}

	// ============================ Object ==============================

	public static String ssv(final Object[] a) { return frmtObj(SP, SP, a); }
	public static String csv(final Object[] a) { return frmtObj(CSP, CSP, a); }
	public static String and(final Object[] a) { return frmtObj(CSP, AND, a); }
	public static String or(final Object[] a) { return frmtObj(CSP, OR, a); }
	public static String frmtObj(final String sep, final String lastSep, final Object[] a) {
		MY_SB.setLength(0);
		return appendObj(MY_SB, a, sep, lastSep).toString();
	}
	public static StringBuilder appendObj(final StringBuilder sb, final Object[] a, final String sep, final String lastSep) {
		if(a==null) return sb;
		return appendObj(sb, a, a.length, sep, lastSep);
	}
	public static StringBuilder appendObj(final StringBuilder sb, final Object[] a, final int n, final String sep, final String lastSep) {
		int last = n; // index of the last non-null element
		while ( last>0 && a[last-1]==null )
			--last;
		// foreach element
		for ( int i=0,m=last-1; i<last; ++i) {
			if ( i > 0 )
				sb.append(i<m ? sep : lastSep);
			sb.append(String.valueOf(a[i]));
		}
		return sb;
	}

	// ========================== funky stuff ============================

//RETAIN unused 2023-10-16 because I think it should exist
	public static DecimalFormat DOUBLE_FORMAT = new DecimalFormat("#0.00");
	// dbl because it is short, and double is a reserved word.
	public static String dbl(final double d) { return DOUBLE_FORMAT.format(d); }

	public static DecimalFormat LONG_FORMAT = new DecimalFormat("#,##0");
	// lng because it is short, and long is a reserved word.
	public static String lng(final long l) { return LONG_FORMAT.format(l); }

	// 132 spaces should be enough, I guess
	public static final String SPACES = repeat(SP, 132);
	public static final String ZEROS = repeat("0", 32); // 32 is just a guess

	public static String repeat(final String s, final int howManyTimes) {
		final StringBuilder sb = SB(s.length()*howManyTimes);
		for ( int i=0; i<howManyTimes; ++i )
			sb.append(s);
		return sb.toString();
	}

//RETAIN 2023-10-16 unused but I think it should exist
	public static String rpad(final String s, final int len) {
		return s + SPACES.substring(0, Math.max(len-s.length(),0));
	}
//RETAIN 2023-10-16 unused but I think it should exist
	public static String lpad(final String s, final int len) {
		return SPACES.substring(0, Math.max(len-s.length(),0)) + s;
	}
	public static String spaces(final int n) {
		return SPACES.substring(0, n);
	}
	public static String lfill(final int i, final int len) {
		final String s = Integer.toString(i);
		return ZEROS.substring(0, Math.max(len-s.length(),0)) + s;
	}

	public static String plural(final int n, final String thing) {
		if ( n == 1 )
			return thing;
		return thing+"s";
	}

	public static String plural(final int n, final String one, final String many) {
		if ( n == 1 )
			return one;
		return many;
	}

	/**
	 * Return a string of i, left padded with spaces to atleast size length.
	 * @param i
	 * @param size
	 * @return enspace(1, 2) -&gt; " 1"
	 */
	public static String enspace(final int i, final int size) {
		if ( i>-1 && i<(10*(size-1)) )
			return spaces(size-1)+Integer.toString(i);
		return Integer.toString(i);
	}

	public static StringBuilder ids(final StringBuilder sb, final Set<Integer> indices
			, final String sep, final String lastSep) {
		final int m = indices.size() - 1;
		int i = 0;
		for ( int indice : indices ) {
			if(i>0) sb.append(i==m ? lastSep : sep);
			sb.append(CELL_IDS[indice]);
			++i;
		}
		return sb;
	}

//not used 2020-10-23 but I am loath to delete any of this crap
//	public static String ifNull(String s, String defualt) {
//		return s!=null ? s : defualt;
//	}
//
//	// dump the Grid.containsValue array as "....5.7.."
//	public static String indices(boolean[] containsValue) {
//		StringBuilder sb = getSB();
//		for ( int v=1; v<VALUE_CEILING; ++v )
//			sb.append(DIGITS[containsValue[v] ? v : 0]);
//		return sb.toString();
//	}
//	private static final char[] DIGITS = {'.', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	private Frmt() {} // make this class non-instantiable

}
