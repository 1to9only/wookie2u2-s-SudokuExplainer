/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.io.PrintStream;
import java.util.LinkedHashSet;

/**
 * My convenient Formatter for collections of stuff
 */
public final class Frmt {

	// NL is \r\n on Winblows; \n on *nix; and \r on appleOS
	// Do NOT get me started on file-path seperator characters. I mean which
	// ____ing inbred chose THE escape character as a file-path-seperator?
	public static final String NL = System.lineSeparator();

	private static final StringBuilder SB = new StringBuilder(64);
	public static StringBuilder getSB() {
		synchronized( SB ) {
			SB.setLength(0);
			return SB;
		}
	}
	public static StringBuilder getSB(int initialCapacity) {
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
	public static final String[] LOWERCASE_LETTERS = new String[] {
		"a", "b", "c", "d", "e", "f", "g", "h", "i"
	};

	public static final String COLON_SP = ": ";
	public static final String COLON_ONLY = ":";
	public static final String IN = " in ";
	public static final String ON = " on ";
	public static final String SO = " so ";
	public static final String NULL_ST = "null";

	public static final String EMPTY_STRING = "";
	public static final String COMMA_SP = ", ";
	public static final String COMMA = ",";
	public static final String AND = " and ";
	public static final String OR = " or ";
	public static final String SPACE = " ";
	public static final String TAB = "\t";
	public static final String TWO_SPACES = "  ";
	public static final String PLUS = "+";
	public static final String MINUS = "-";
	public static final String PERIOD = ".";
	public static final String EQUALS = "=";
	public static final String NOT_EQUALS = "!=";

	private static final StringBuilder MY_SB = new StringBuilder(64);


	// ======================== Object array IFormatter =======================

	// just cast it to Whatever in your Formatter and IFilter impl's.

	public static String csv(String prefix, Object[] a, IFormatter<Object> ts) {
		if(a==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		MY_SB.append(prefix);
		return appendTo(MY_SB, a, ts, COMMA_SP, COMMA_SP).toString();
	}
	public static String csv(Object[] a, IFormatter<Object> ts) {
		return Frmt.frmt(a, ts, COMMA_SP, COMMA_SP);
	}
	public static String frmt(Object[] a, IFormatter<Object> ts, String sep, String lastSep) {
		if(a==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendTo(MY_SB, a, ts, sep, lastSep).toString();
	}
	public static StringBuilder appendTo(StringBuilder sb, Object[] a, IFormatter<Object> ts, String sep, String lastSep) {
		final int m = a.length - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( Object e : a ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(ts.format(e));
		}
		return sb;
	}

	public static void csv(PrintStream out, Object[] a, IFilter<Object> filter
			, IFormatter<Object> formatter) {
		Frmt.csv(out, a, filter, formatter, COMMA_SP, COMMA_SP);
	}
	public static void csv(PrintStream out, Object[] a, IFilter<Object> filter
			, IFormatter<Object> formatter, String sep, String lastSep) {
		final int m = a.length - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( Object e : a )
			if ( filter.accept(e) ) {
				if ( ++i > 1 )
					out.append(i<m ? sep : lastSep);
				out.append(formatter.format(e));
			}
	}

	// ======================== Collection IFormatter =========================

	public static <E> String csv(Collection<? extends E> c, IFormatter<E> ts) {
		return Frmt.frmt(c, ts, COMMA_SP, COMMA_SP);
	}
	public static <E> String frmt(Collection<? extends E> c, IFormatter<E> ts, String sep, String lastSep) {
		if(c==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendTo(MY_SB, c, ts, sep, lastSep).toString();
	}
	/** Appends the given String to the given StringBuilder, separating each
	 * value with 'sep', except the last which is preceeded by 'lastSep'.
	 * @param <E> The Element type of the collection
	 * @param sb StringBuilder to append to
	 * @param c {@code Collection<E>} to be appended
	 * @param ts {@code ToStringer<E>} whose {@Code String toString(E e)} method
	 * will be invoked to convert each element of the collection into a String.
	 * @param sep values separator
	 * @param lastSep the last values separator
	 * @return the given 'sb' so that you can chain method calls */
	public static <E> StringBuilder appendTo(StringBuilder sb
			, Collection<? extends E> c, IFormatter<E> ts
			, String sep, String lastSep) {
		final int m = c.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( E e : c ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(ts.format(e));
		}
		return sb;
	}

	/**
	 * Appends to 'out'
	 * those elements of 'c'
	 * which are 'accept'ed by 'filter'
	 * as a CSV list 'format'ed by the 'formatter'
	 * and separated by ", " and ", "
	 * @param <E>
	 * @param out PrintStream to print to
	 * @param c {@code Collection<? extends E>} to print
	 * @param filter {@code Filter<E>} only those elements which are 'accept'ed
	 * by the 'filter' are printed.
	 * @param formatter
	 */
	public static <E> void csv(PrintStream out, Collection<? extends E> c
			, IFilter<E> filter, IFormatter<E> formatter) {
		Frmt.csv(out, c, filter, formatter, COMMA_SP, COMMA_SP);
	}

	/**
	 * Appends to 'out'
	 * those elements of 'c'
	 * which are 'accept'ed by 'filter'
	 * as a CSV list 'toString'ed by the 'formatter'
	 * and (separated by 'sep' and 'lastSep',
	 *                ie ", ", ", "
	 *                or ", ", "and ")
	 * @param <E>
	 * @param out PrintStream to print to
	 * @param c {@code Collection<? extends E>} to print
	 * @param filter {@code Filter<E>} only those elements which are 'accept'ed
	 * by the 'filter' are printed.
	 * @param formatter
	 * @param sep
	 * @param lastSep
	 */
	public static <E> void csv(PrintStream out, Collection<? extends E> c
			, IFilter<E> filter, IFormatter<E> formatter
			, String sep, String lastSep) {
		final int m = c.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( E e : c )
			if ( filter.accept(e) ) {
				if ( ++i > 1 )
					out.append(i<m ? sep : lastSep);
				out.append(formatter.format(e));
			}
	}

	// ============================ String ==============================

	public static String csvs(Collection<String> ss) { return Frmt.frmts(ss, COMMA_SP, COMMA_SP); }
	public static String ands(Collection<String> ss) { return Frmt.frmts(ss, COMMA_SP, AND); }
	public static String ors(Collection<String> ss) { return Frmt.frmts(ss, COMMA_SP, OR); }
	public static String frmts(Collection<String> ss, String sep, String lastSep) {
		if(ss==null) return EMPTY_STRING;
		MY_SB.setLength(0);
		return appendTos(MY_SB, ss, sep, lastSep).toString();
	}
	/** Appends the given String to the given StringBuilder, separating each
	 * value with 'sep', except the last which is preceeded by 'lastSep'.
	 * @param sb to append to
	 * @param ss {@code Collection<String>} to format as a list (or whatever)
	 * @param sep values separator
	 * @param lastSep the last values separator
	 * @return the given 'sb' so that you can chain method calls */
	public static StringBuilder appendTos(StringBuilder sb
			, Collection<String> ss, String sep, String lastSep) {
		final int m = ss.size() - 1; // m is n-1; n is the population count.
		int i = 0;
		for ( String s : ss ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(s);
		}
		return sb;
	}

	/**
	 * Parse a $sep Separated Values line into a {@code LinkedHashSet<String>}.
	 * @param line to parse.
	 * @param sep the field separator, typically ", " for CSV.
	 * @return {@code LinkedHashSet<String>} because iteration order is
	 * significant to testing it, even if it isn't in using it, which it
	 * almost certainly will be in some future instance, so it's Linked.
	 */
	public static LinkedHashSet<String> csvToSetOfStrings(String line, String sep) {
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
		// and they're the ones that matter. If short strings are slower then
		// it's still a bounded problem, but if long strings are slower then
		// it's a Microsoft product. Insert new programmer and press any key
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
				 // which would've been bloody fast enough anyways, for
				 // small tables of short lists; which is what've I got.
			for ( int i=line.indexOf(sep, st); i>-1; i=line.indexOf(sep, st) ) {
				result.add(new String(chars, st, i-st));
				st = i + sepLen;
			}
		}
		// and don't forget the last one, also handles !line.contains(sep).
		result.add(new String(chars, st, line.length()-st));
		return result;
	}

	/**
	 * This is roughly equivalent to: {@code
	 * line.substring(begin, end).indexOf(c);
	 * }
	 * except without the temporary String.
	 * @param line to look in
	 * @param c to look for
	 * @param begin the 0-based starting index of the search (inclusive)
	 * @param end the 0-based end index of the search (exclusive)
	 * @return the 0-based index of c in line, if that falls at or after begin
	 *  and before end
	 * @throws StringIndexOutOfBoundsException wrap me in a safeIndexOf if you
	 *  can't workout begin and end reliably. I'm too efficient for that s__t.
	 */
	public static int indexOf(String line, char c, int begin, int end) {
		for ( int i=begin; i<end; ++i )
			if ( line.charAt(i) == c )
				return i;
		return -1;
	}

	// =============================== int... =================================

	/** csv: {1,2,4,0} => "1, 2, 4, 0" */
	public static String csv(int... vs) { return Frmt.frmt(vs, COMMA_SP, COMMA_SP); }
	/** ssv: 3, {1,2,4,0} => "1 2 4" */
	public static String ssv(int n, int... vs) { return Frmt.frmt(vs, n, SPACE, SPACE); }
	/** and: {1,2,4,0} => "1, 2, 4 and 0" */
	public static String and(int... vs) { return Frmt.frmt(vs, COMMA_SP, AND); }
	/** or: {1,2,4,0} => "1, 2, 4 or 0" */
	public static String or(int... vs) { return Frmt.frmt(vs, COMMA_SP, OR); }
	/** frmt: {1,2,4,0}, ", ", " or "  =>  "1, 2, 4 or 0" */
	public static String frmt(int[] vs, String sep, String lastSep) {
		return frmt(vs, vs.length, sep, lastSep);
	}
	/** frmt: {1,2,4,0}, 3, ", ", " or "  =>  "1, 2 or 4" */
	public static String frmt(int[] vs, int n, String sep, String lastSep) {
		if(vs==null || vs.length==0 || n==0) return EMPTY_STRING;
		MY_SB.setLength(0);
		return append(MY_SB, vs, n, sep, lastSep).toString();
	}
	// Append the first n values to sb, separated by sep and lastSep.
	// This method is private coz it comes with preconditions:
	// 1. sb is not null (you probably need to clear it before you call me)
	// 2. vs is not null or empty
	// 3. n is presumed to be greater than 0, not that it really matters
	// 4. sep and lastSep are not null
	// @param sb StringBuilder to append to
	// @param vs int[] to print
	// @param n number of 'vs' elements to print
	// @param sep the separator string (except for before the last element)
	// @param lastSep the separator string for before the last element
	// @return the index of the first 0 in the given int array.
// If you wish to make this method public again then go ahead, and don't forget
// to turn this comment back into javadoc.
	private static StringBuilder append(StringBuilder sb, int[] vs, final int n
			, String sep, String lastSep) {
		for ( int i=0,m=n-1; i<n; ++i ) {
			if ( i > 0 )
				sb.append(i<m ? sep : lastSep);
			sb.append(vs[i]);
		}
		return sb;
	}

//not used, but retained just coz it's a bit clever
//	/** csv-Null-Terminated-List: {1,2,4,0,0} => "1, 2, 4" */
//	public static String csvNTL(int... vs) { return Frmt.frmtNTL(vs, comma, comma); }
//	/** and-Null-Terminated-List: {1,2,4,0,0} => "1, 2 and 4" */
//	public static String andNTL(int... vs) { return Frmt.frmtNTL(vs, comma, and); }
//	/** or-Null-Terminated-List: {1,2,4,0,0} => "1, 2 or 4" */
//	public static String orNTL(int... vs) { return Frmt.frmtNTL(vs, comma, or); }
//	/** frmt-Null-Terminated-List: {1,2,4,0,0}, ", ", " or "  =>  "1, 2 or 4" */
//	public static String frmtNTL(int[] vs, String sep, String lastSep) {
//		if(vs==null || vs.length==0) return EMPTY_STRING;
//		MY_SB.setLength(0);
//		return append(MY_SB, vs, eoNTL(vs), sep, lastSep).toString();
//	}
//	/** endOfNullTerminatedList returns the index of the first 0 in 'vs'.
//	 * @param vs int[] to examine
//	 * @return the index of the first 0 in the given int array. */
//	public static int eoNTL(int[] vs) {
//		final int n = vs.length;
//		for ( int i=0; i<n; ++i )
//			if ( vs[i] == 0 )
//				return i;
//		return n;
//	}

	public static int[] toIntArray(String[] strings) {
		if ( strings==null || strings.length==0 )
			return null;
		int[] ints = new int[strings.length];
		int cnt = 0;
		for ( String s : strings )
			ints[cnt++] = Integer.parseInt(s);
		return ints;
	}

//not used 2020-10-23 but retained coz it's handy for watch expressions
	public static String toBinaryString(int i, int len) {
		String s = Integer.toBinaryString(i);
		int sl = s.length();
		// that's 32 0's
		return "00000000000000000000000000000000".substring(0, len-sl) + s;
	}

	// ============================ Integer ==============================

	/**
	 * Converts the given set of Strings into a new set of Integers by calling
	 * Integer.valueOf(s) on each.
	 * @param strings {@code Set<String>}
	 * @return {@code Set<Integer>}
	 * @throws NumberFormatException which is all your problem.
	 */
	public static LinkedHashSet<Integer> toIntegersSet(LinkedHashSet<String> strings) {
		LinkedHashSet<Integer> result = new LinkedHashSet<>(strings.size());
		for ( String s : strings )
			result.add(Integer.valueOf(s));
		return result;
	}

	/**
	 * Reads the given line into a {@code LinkedHashSet<Integer>}.
	 * @param line to read
	 * @param sep the list separator, typically ", ".
	 * @return {@code LinkedHashSet<Integer>}
	 * @throws NumberFormatException which is all your problem.
	 */
	public static LinkedHashSet<Integer> integerSetFromCsv(String line, String sep) {
		return toIntegersSet(csvToSetOfStrings(line, sep));
	}

	public static String frmtIntegers(Collection<Integer> vs, String sep, String lastSep) {
		MY_SB.setLength(0);
		int[] array = toIntArray(vs);
		return append(MY_SB, array, array.length, sep, lastSep).toString();
	}

	/**
	 * Unboxes the given collection of Integers into an int-array.
	 * @param vs {@code Collection<Integer>} to unbox.
	 * @return {@code int[]}
	 */
	public static int[] toIntArray(Collection<Integer> vs) {
		int[] result = new int[vs.size()];
		int cnt = 0;
		for ( Integer v : vs )
			result[cnt++] = v; // let it auto-unbox
		return result;
	}

	// ============================ boolean... ==============================

//not used 2020-11-23 but retained just coz I think I should have it
	public static String plain(boolean... bs) { return Frmt.frmt(bs, EMPTY_STRING, EMPTY_STRING); }
	public static String csv(boolean... bs) { return Frmt.frmt(bs, COMMA_SP, COMMA_SP); }
	public static String and(boolean... bs) { return Frmt.frmt(bs, COMMA_SP, AND); }
	public static String or(boolean... bs) { return Frmt.frmt(bs, COMMA_SP, OR); }
	public static String frmt(boolean[] bs, String sep, String lastSep) {
		MY_SB.setLength(0);
		return append(MY_SB, bs, sep, lastSep).toString();
	}
	public static StringBuilder append(StringBuilder sb, boolean[] bs, String sep, String lastSep) {
		if(bs==null) return sb;
		for ( int i=0, n=bs.length, m=n-1; i<n; ++i ) {
			if (i > 0)
				sb.append(i<m ? sep : lastSep);
			sb.append(bs[i]?'T':'F');
		}
		return sb;
	}

	// ============================ Object ==============================

	public static String csv(Object[] a) { return Frmt.frmtObj(COMMA_SP, COMMA_SP, a); }
	public static String and(Object[] a) { return Frmt.frmtObj(COMMA_SP, AND, a); }
	public static String or(Object[] a) { return Frmt.frmtObj(COMMA_SP, OR, a); }
	public static String frmtObj(final String sep, final String lastSep, Object[] a) {
		MY_SB.setLength(0);
		return appendObj(MY_SB, a, sep, lastSep).toString();
	}
	public static StringBuilder appendObj(StringBuilder sb, Object[] a
			, final String sep, final String lastSep) {
		if(a==null) return sb;
		return appendObj(sb, a, a.length, sep, lastSep);
	}
	public static StringBuilder appendObj(StringBuilder sb, Object[] a, int n
			, final String sep, final String lastSep) {
		// find the last non-null entry
		while ( n>0 && a[n-1]==null )
			--n;
		// foreach element
		for ( int i=0,m=n-1; i<n; ++i) {
			if ( i > 0 )
				sb.append(i<m ? sep : lastSep);
			sb.append(String.valueOf(a[i]));
		}
		return sb;
	}

	public static String csvIt(Iterable<?> any) { return formatIt(any, COMMA_SP, COMMA_SP); }
	public static String andIt(Iterable<?> any) { return formatIt(any, COMMA_SP, AND); }
	public static String orIt(Iterable<?> any) { return formatIt(any, COMMA_SP, OR); }
	public static String formatIt(Iterable<?> any, final String sep, final String lastSep) {
		if ( any == null )
			return EMPTY_STRING;
		final int n = countIt(any); // count them to recognise the last one
		if ( n == 0 )
			return EMPTY_STRING;
		StringBuilder sb = getSB();
		Iterator<?> it = any.iterator();
		// foreach element
		sb.append(String.valueOf(it.next()));
		for ( int i=1,m=n-1; i<n; ++i )
			sb.append(i<m ? sep : lastSep)
			  .append(String.valueOf(it.next()));
		return sb.toString();
	}

	public static int countIt(Iterable<?> things) {
		int count = 0;
		for ( Object e : things )
			++count;
		return count;
	}

	// ========================== funky stuff ============================

	public static DecimalFormat DOUBLE_FORMAT = new DecimalFormat("#0.00");
	public static String frmtDbl(double d) { return DOUBLE_FORMAT.format(d); }

	// 132 spaces should be enough, I guess
	public static final String SPACES = repeat(SPACE, 132);

	public static String repeat(String chars, int howManyTimes) {
		final StringBuilder sb = new StringBuilder(chars.length()*howManyTimes);
		for ( int i=0; i<howManyTimes; ++i )
			sb.append(chars);
		return sb.toString();
	}

	public static String rpad(String s, int len) {
		return s + SPACES.substring(0, Math.max(len-s.length(),0));
	}
	public static String lpad(String s, int len) {
		return SPACES.substring(0, Math.max(len-s.length(),0)) + s;
	}
	public static String spaces(int n) {
		return SPACES.substring(0, n);
	}

	public static String plural(int n, String thing) {
		return thing+(n==1?EMPTY_STRING:"s");
	}

	/**
	 * Return a string of i, left padded with spaces to atleast size length.
	 * @param i
	 * @param size
	 * @return enspace(1, 2) -&gt; " 1"
	 */
	public static String enspace(int i, int size) {
		if ( i>-1 && i<(10*(size-1)) )
			return spaces(size-1)+Integer.toString(i);
		return Integer.toString(i);
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	public static String ifNull(String s, String defualt) {
//		return s!=null ? s : defualt;
//	}
//
//	// dump the Grid.containsValue array as "....5.7.."
//	public static String indices(boolean[] containsValue) {
//		StringBuilder sb = getSB();
//		for ( int v=1; v<10; ++v )
//			sb.append(DIGITS[containsValue[v] ? v : 0]);
//		return sb.toString();
//	}
//	private static final char[] DIGITS = {'.', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	private Frmt() {} // make this class non-instantiable
}
