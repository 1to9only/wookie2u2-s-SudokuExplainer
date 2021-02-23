/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.hinters.align.CellSet;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


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

	public static String none = "";
	public static String comma = ", ";
	public static String and = " and ";
	public static String or = " or ";
	public static String commaOnly = ",";
	public static String space = " ";
	private static final StringBuilder MY_SB = new StringBuilder(64);

	// ============================ IFormatter ==============================

	public static <E> String csv(Collection<? extends E> c, IFormatter<E> ts) {
		return Frmt.frmt(c, ts, comma, comma);
	}
	public static <E> String frmt(Collection<? extends E> c, IFormatter<E> ts, String sep, String lastSep) {
		if(c==null) return "";
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
		Frmt.csv(out, c, filter, formatter, comma, comma);
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

	public static String csvs(Collection<String> ss) { return Frmt.frmts(ss, comma, comma); }
	public static String ands(Collection<String> ss) { return Frmt.frmts(ss, comma, and); }
	public static String ors(Collection<String> ss) { return Frmt.frmts(ss, comma, or); }
	public static String frmts(Collection<String> ss, String sep, String lastSep) {
		if(ss==null) return "";
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
	public static String csv(int... vs) { return Frmt.frmt(vs, comma, comma); }
	/** ssv: 3, {1,2,4,0} => "1 2 4" */
	public static String ssv(int n, int... vs) { return Frmt.frmt(vs, n, space, space); }
	/** and: {1,2,4,0} => "1, 2, 4 and 0" */
	public static String and(int... vs) { return Frmt.frmt(vs, comma, and); }
	/** or: {1,2,4,0} => "1, 2, 4 or 0" */
	public static String or(int... vs) { return Frmt.frmt(vs, comma, or); }
	/** frmt: {1,2,4,0}, ", ", " or "  =>  "1, 2, 4 or 0" */
	public static String frmt(int[] vs, String sep, String lastSep) {
		return frmt(vs, vs.length, sep, lastSep);
	}
	/** frmt: {1,2,4,0}, 3, ", ", " or "  =>  "1, 2 or 4" */
	public static String frmt(int[] vs, int n, String sep, String lastSep) {
		if(vs==null || vs.length==0 || n==0) return "";
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
//		if(vs==null || vs.length==0) return "";
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
	public static String plain(boolean... bs) { return Frmt.frmt(bs, none, none); }
	public static String csv(boolean... bs) { return Frmt.frmt(bs, comma, comma); }
	public static String and(boolean... bs) { return Frmt.frmt(bs, comma, and); }
	public static String or(boolean... bs) { return Frmt.frmt(bs, comma, or); }
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

	// ============================ Values ==============================

	public static String csv(Values vs) { return Frmt.frmt(vs, comma, comma); }
	public static String and(Values vs) { return Frmt.frmt(vs, comma, and); }
	public static String or(Values vs) { return Frmt.frmt(vs, comma, or); }
	public static String frmt(Values vs, String sep, String lastSep) {
		if(vs==null) return "";
		MY_SB.setLength(0);
		return appendTo(MY_SB, vs.bits, vs.size, sep, lastSep).toString();
	}
	/** Appends the string representation of bits (a Values bitset) to the
	 * given StringBuilder, separating each value with 'sep', except the
	 * last which is preceeded by 'lastSep'.
	 * @param sb to append to.
	 * @param bits int to format.
	 * @param size int the number of set bits (1s) in bits.
	 * @param sep values separator.
	 * @param lastSep the last values separator.
	 * @return the given 'sb' so that you can chain method calls. */
	public static StringBuilder appendTo(StringBuilder sb, int bits, int size
			, String sep, String lastSep) {
		int i = 0;
		for ( int v : VALUESES[bits] ) {
			if ( ++i > 1 ) //nb: i is now effectively 1-based, so i<size is correct
				sb.append(i<size ? sep : lastSep);
			sb.append(v);
		}
		return sb;
	}

//not used 2020-10-23 but retain for a while. Delete if you like
//	public static String asValues(int... bitss) {
//		final int n = bitss.length;
//		Values[] vss = new Values[n];
//		for ( int i=0; i<n; ++i )
//			vss[i] = new Values(bitss[i], false);
//		return Frmt.frmt(vss, comma, comma);
//	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	public static String csv(Values... vss) { return Frmt.frmt(vss, comma, comma); }
//	public static String and(Values... vss) { return Frmt.frmt(vss, comma, and); }
//	public static String or(Values... vss) { return Frmt.frmt(vss, comma, or); }
//	public static String frmt(Values[] vss, final String sep, final String lastSep) {
//		MY_SB.setLength(0);
//		return append(MY_SB, vss, sep, lastSep).toString();
//	}
//	public static StringBuilder append(StringBuilder sb, Values[] vss, String sep, String lastSep) {
//		if(vss==null) return sb;
//		for ( int i=0, n=vss.length, m=n-1; i<n; ++i ) {
//			if ( vss[i] == null )
//				break; // it's a null terminated array
//			if ( i > 0 )
//				sb.append(i<m ? sep : lastSep);
//			vss[i].appendTo(sb); // delegate actual formatting back to Values
//		}
//		return sb;
//	}

	// ============================ Indexes ==============================

//not used 2020-10-23 but I'm loath to delete any of this crap
//	public static String csv(Indexes is) { return Frmt.frmt(is, comma, comma); }
//	public static String and(Indexes is) { return Frmt.frmt(is, comma, and); }
//	public static String or(Indexes is) { return Frmt.frmt(is, comma, or); }
//	public static String frmt(Indexes is, String sep, String lastSep) {
//		if(is==null) return "";
//		MY_SB.setLength(0);
//		return is.appendTo(MY_SB, sep, lastSep).toString();
//	}

	// ============================ Cell ==============================

	/**
	 * Currently only used for debug logging.
	 * @param sep the separator between cells-strings: " " works, ", " is standard
	 * @param numCells the number of cells to format
	 * @param cells arguements array of Cell to be formatted.
	 * @return a CSV list of the cell.toFullString() of each cell in cells
	 * (which may be a null-terminated array).
	 */
	public static String toFullString(String sep, int numCells, Cell... cells) {
		if(cells==null) return "null";
		MY_SB.setLength(0);
		final int n = Math.min(numCells, cells.length);
		for ( int i=0; i<n; ++i ) {
			if ( cells[i] == null )
				break; // null terminated list
			if ( i > 0 )
				MY_SB.append(sep);
			MY_SB.append(cells[i].toFullString());
		}
		return MY_SB.toString();
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	/**
//	 * Currently only ever used to format watches in the debugger, where it's
//	 * very useful, so keep it, even though it's not used, because it is. See?
//	 * @param numCells the number of cells to format
//	 * @param cells arguements array of Cell to be formatted.
//	 * @return a CSV list of the cell.toFullString() of each cell in cells
//	 * (which may be a null-terminated array).
//	 */
//	public static String toFullString(int numCells, Cell... cells) {
//		return toFullString(", ", numCells, cells);
//	}
//
//	/**
//	 * Currently only ever used to format watches in the debugger, where it's
//	 * very useful, so keep it, even though it's not used, because it is. See?
//	 * @param cells arguements array Cells to format (may be a null-terminated
//	 * array)
//	 * @return a CSV list of the cell.toFullString() of each cell in cells
//	 * (which may be a null-terminated array).
//	 */
//	public static String toFullString(Cell... cells) {
//		return toFullString(", ", cells.length, cells);
//	}

	/**
	 * Currently only ever used to format watches in the debugger, where it's
	 * very useful, so keep it, even though it's not used, because it is. See?
	 * @param cells {@code Iterable<Cell>} to be formatted.
	 * @param sep cell separator String. The default is ", "
	 * @return a CSV list of the cell.toFullString() of each cell in cells
	 * (which may be a null-terminated array).
	 */
	public static String toFullString(String sep, Iterable<Cell> cells) {
		if(cells==null) return "null";
		MY_SB.setLength(0);
		int i = 0;
		// Iterator used to handle null-terminated lists, which Nutbeans
		// is sure can't possibly exist. F___ing Nutbeans!
		Cell cell;
		for ( Iterator<Cell> it = cells.iterator(); it.hasNext(); ) {
			if ( (cell=it.next()) == null )
				break; // null terminated list
			if ( ++i > 1 )
				MY_SB.append(sep);
			MY_SB.append(cell.toFullString());
		}
		return MY_SB.toString();
	}

	public static String toFullString(Iterable<Cell> cells) {
		return toFullString(", ", cells);
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	// this method currently only used in Debug watch statements in A*E.
//	public static String csv(boolean dummy, int... bitss) {
//		if(bitss==null) return "null";
//		final int n = bitss.length;
//		if(n==0) return "";
//		StringBuilder sb = MY_SB;
//		sb.setLength(0);
//		sb.append(Values.toString(bitss[0]));
//		for ( int i=1; i<n; ++i )
//			sb.append(", ").append(Values.toString(bitss[i]));
//		return sb.toString();
//	}
//
//	// this method currently only used in Debug watch statements in A*E.
//	public static String csv(Cell[] cells, int n) {
//		if(cells==null) return "null";
//		if(n==0) return "";
//		StringBuilder sb = MY_SB;
//		sb.setLength(0);
//		sb.append(cells[0].toFullString());
//		for ( int i=1; i<n; ++i )
//			sb.append(", ").append(cells[i].toFullString());
//		return sb.toString();
//	}

	public static String csv(Cell... cells) { return Frmt.frmt(cells, cells.length, comma, comma); }
	public static String csv(int n, Cell... cells) { return Frmt.frmt(cells, n, comma, comma); }
	public static String ssv(Cell... cells) { return Frmt.frmt(cells, cells.length, space, space); }
	public static String ssv(int n, Cell... cells) { return Frmt.frmt(cells, n, space, space); }
	public static String and(Cell... cells) { return Frmt.frmt(cells, cells.length, comma, and); }
	public static String or(Cell... cells) { return Frmt.frmt(cells, cells.length, comma, or); }
	public static String frmt(Cell[] cells, int n, final String sep, final String lastSep) {
		MY_SB.setLength(0);
		return append(MY_SB, cells, n, sep, lastSep).toString();
	}
	public static StringBuilder append(StringBuilder sb, Cell[] cells, int n, String sep, String lastSep) {
		if(cells==null) return sb;
		for ( int i=0,m=n-1; i<n; ++i ) {
			if ( cells[i] == null )
				break; // it's a null terminated array
			if ( i > 0 )
				sb.append(i<m ? sep : lastSep);
			sb.append(cells[i].id);
		}
		return sb;
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	public static String csv(Iterable<Cell> cells) { return Frmt.frmt(cells, comma, comma); }
//	public static String and(Iterable<Cell> cells) { return Frmt.frmt(cells, comma, and); }
//	public static String or(Iterable<Cell> cells) { return Frmt.frmt(cells, comma, or); }
//	public static String frmt(Iterable<Cell> cells, final String sep, final String lastSep) {
//		MY_SB.setLength(0);
//		return append(MY_SB, cells, sep, lastSep).toString();
//	}
//	public static StringBuilder append(StringBuilder sb, Iterable<Cell> cells, final String sep, final String lastSep) {
//		if(cells==null) return sb;
//		final int n = count(cells);
//		int i = 0;
//		for ( Cell c : cells ) {
//			if ( ++i > 1 )
//				sb.append(i<n ? sep : lastSep);
//			sb.append(c.id);
//		}
//		return sb;
//	}

	public static String csv(Collection<Cell> cells) { return Frmt.frmt(cells, comma, comma); }
	public static String ssv(Collection<Cell> cells) { return Frmt.frmt(cells, space, space); }
	public static String and(Collection<Cell> cells) { return Frmt.frmt(cells, comma, and); }
	public static String or(Collection<Cell> cells) { return Frmt.frmt(cells, comma, or); }
	public static String frmt(Collection<Cell> cells, final String sep, final String lastSep) {
		MY_SB.setLength(0);
		return append(MY_SB, cells, sep, lastSep).toString();
	}
	public static StringBuilder append(StringBuilder sb, Collection<Cell> cells, final String sep, final String lastSep) {
		if(cells==null) return sb;
		int n = cells.size();
		int i = 0;
		for ( Cell c : cells ) {
			if ( c==null)
				break; // null terminated list!
			if ( ++i > 1 )
				// nb: i<n only works coz i is 1-based and n is 0-based;
				//     but, if it works then don't ____ with it.
				sb.append(i<n ? sep : lastSep);
			sb.append(c.id);
		}
		return sb;
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	// Currently only used by Frmt.map which is only called by Debug.diff.
//	public static String csvFS(Collection<Cell> cells) { return Frmt.frmtFS(cells, commaOnly, commaOnly); }
//	public static String andFS(Collection<Cell> cells) { return Frmt.frmtFS(cells, comma, and); }
//	public static String orFS(Collection<Cell> cells) { return Frmt.frmtFS(cells, comma, or); }
//	public static String frmtFS(Collection<Cell> cells, final String sep, final String lastSep) {
//		MY_SB.setLength(0);
//		return appendFS(MY_SB, cells, sep, lastSep).toString();
//	}

	public static StringBuilder appendFS(StringBuilder sb, Collection<Cell> cells, final String sep, final String lastSep) {
		if(cells==null) return sb;
		int m = cells.size() - 1; // m is n-1
		int i = 0;
		for ( Cell c : cells ) {
			if ( ++i > 1 )
				sb.append(i<m ? sep : lastSep);
			sb.append(c.toFullString());
		}
		return sb;
	}

	// ============================ Object ==============================

	public static String csv(Object[] a) { return Frmt.frmtObj(comma, comma, a); }
	public static String and(Object[] a) { return Frmt.frmtObj(comma, and, a); }
	public static String or(Object[] a) { return Frmt.frmtObj(comma, or, a); }
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

	public static String csvIt(Iterable<?> any) { return formatIt(any, comma, comma); }
	public static String andIt(Iterable<?> any) { return formatIt(any, comma, and); }
	public static String orIt(Iterable<?> any) { return formatIt(any, comma, or); }
	public static String formatIt(Iterable<?> any, final String sep, final String lastSep) {
		if ( any == null )
			return "";
		final int n = countIt(any); // count them to recognise the last one
		if ( n == 0 )
			return "";
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

	// ========================== regions ============================

	// nb: List binds before Collection to avert type-erasure problem
	public static String and(List<ARegion> list) {
		return and((Object[])list.toArray(new ARegion[list.size()]));
	}

	public static String and(List<ARegion> bases, List<ARegion> covers) {
		StringBuilder sb = getSB(7 * (bases.size()+covers.size()));
		boolean first = true;
		Iterator<ARegion> b = bases.iterator();
		Iterator<ARegion> c = covers.iterator();
		// append a base and then a cover
		while ( b.hasNext() && c.hasNext() ) {
			ARegion base = b.next();
			ARegion cover = c.next();
			if(first) first=false; else sb.append(", ");
			sb.append(base.id).append(", ").append(cover.id);
		}
		// append any trailers
		while ( b.hasNext() ) {
			ARegion base = b.next();
			if(first) first=false; else sb.append(", ");
			sb.append(base.id);
		}
		// append any trailers
		while ( c.hasNext() ) {
			ARegion cover = c.next();
			if(first) first=false; else sb.append(", ");
			sb.append(cover.id);
		}
		return sb.toString();
	}

	// nb: List binds before Collection to avert type-erasure problem
	public static String csv(List<ARegion> regions) {
		if ( regions == null )
			return "null";
		StringBuilder sb = getSB(7 * regions.size());
		boolean first = true;
		for ( ARegion r : regions ) {
			if(first) first=false; else sb.append(", ");
			sb.append(r.id);
		}
		return sb.toString();
	}

	// nb: Set binds before Collection to avert type-erasure problem
	public static String csv(Set<ARegion> regions) {
		if ( regions == null )
			return "null";
		StringBuilder sb = getSB(4 * regions.size());
		boolean first = true;
		for ( ARegion r : regions ) {
			if ( first )
				first = false;
			else
				sb.append(", ");
			sb.append(r.id);
		}
		return sb.toString();
	}

	public static StringBuilder basesAndCovers(StringBuilder sb, List<ARegion> bases, List<ARegion> covers) {
		return sb.append(csv(bases)).append(" and ").append(csv(covers));
	}

//not used 2020-10-23 but I'm loath to delete any of this crap
//	/**
//	 * Return "$basesCSV and $coversCSV" from $basesUsed and $coversUsed.
//	 * @param basesUsed 27 booleans (consequent with Grid.regions): true when
//	 *  this region is used as a base, else false.
//	 * @param coversUsed 27 booleans (consequent with Grid.regions): true when
//	 *  this region is used as a cover, else false.
//	 * @return used "$basesCSV and $coversCSV"
//	 */
//	public static String basesAndCovers(boolean[] basesUsed, boolean[] coversUsed) {
//		StringBuilder sb = getSB(128);
//		csv(sb, basesUsed);
//		sb.append(" and ");
//		csv(sb, coversUsed);
//		return sb.toString();
//	}
//
//	/**
//	 * Append the id's of the used regions to sb, and return sb.
//	 * @param used regions to this
//	 * @param sb
//	 * @return sb
//	 */
//	public static final StringBuilder csv(StringBuilder sb, boolean[] used) {
//		boolean first = true;
//		for ( int i=0; i<27; ++i )
//			if ( used[i] ) {
//				if ( first )
//					first = false;
//				else
//					sb.append(", ");
//				sb.append(Grid.REGION_IDS[i]);
//			}
//		return sb;
//	}
//
//	public static String basesAndCovers(ARegion[] regions) {
//		final int n = regions.length; // nb: must be even
//		StringBuilder sb = getSB(4 * n);
//		// bases are evens
//		csv(sb, 0, n, regions);
//		sb.append(" and ");
//		// covers are odds
//		csv(sb, 1, n, regions);
//		return sb.toString();
//	}
//	private static void csv(StringBuilder sb, int firstI, int n, ARegion[] regions) {
//		sb.append(regions[firstI].id);
//		for ( int i=firstI+2; i<n; i+=2 )
//			if ( regions[i] != null ) // skip any nulls
//				sb.append(", ").append(regions[i].id);
//	}
//
//	// append 2 => id's, else if full => id's, else => just types
//	public static void frmt(StringBuilder sb, ARegion[] regions) {
//		frmt(sb, regions, false);
//	}
//	public static void frmt(StringBuilder sb, ARegion[] regions, boolean full) {
//		final int len = regions.length;
//		if (len == 2) {
//			sb.append(regions[0].id)		// bases (evens)
//			  .append(" and ").append(regions[1].id);	// covers (odds)
//		} else if (full) {
//			sb.append(regions[0].id);
//			for ( int i=2; i<regions.length; i+=2 )
//				sb.append(", ").append(regions[i].id);
//			sb.append(" and ").append(regions[1].id);
//			for ( int i=3; i<regions.length; i+=2 )
//				sb.append(", ").append(regions[i].id);
//		} else {
////			assert len>=4 && len%2==0; // 4 or 6
//			final int n = (len - (len%2)) / 2;
//			plural(sb, n, regions[0].typeName).append(" and ");
//			plural(sb, n, regions[1].typeName);
//		}
//	}
//
//	public static StringBuilder plural(StringBuilder sb, int n, String thing) {
//		return sb.append(n).append(' ').append(thing).append(n==1?"":"s");
//	}

	// ========================== funky stuff ============================

	public static DecimalFormat DOUBLE = new DecimalFormat("#0.0");
	public static String dbl(double d) { return DOUBLE.format(d); }

	// 132 spaces should be enough, I guess
	private static final String SPACES = repeat(" ", 132);

	public static String repeat(String chars, int howManyTimes) {
		StringBuilder sb = new StringBuilder(chars.length()*howManyTimes);
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

	public static String enspace(String name) {
		char[] src = name.toCharArray();
		char[] dst = new char[src.length*2];
		// copy subsequent characters, inserting a space before each capital.
		int count = 0;
		char prev = '_';
		for ( char ch : src ) {
			if ( prev!='_' && prev!='-' && !Character.isUpperCase(prev)
			  && Character.isUpperCase(ch) )
				dst[count++] = ' ';
			dst[count++] = ch;
			prev = ch;
		}
		return new String(dst, 0, count);
	}

	public static String plural(int n, String thing) {
		return thing+(n==1?"":"s");
	}

	public static String values(Values vs) {
		return Frmt.plural(vs.size, "value")+" "+Frmt.and(vs);
	}

	// Currently only used by Debug.diff to format CellSet's out of the exclusion map.
	static String excludersMap(HashMap<Cell, CellSet> map) {
		StringBuilder sb = getSB();
		sb.setLength(0);
		sb.append(map.size()).append(": [").append(NL).append("\t  ");
		boolean isSubsequent = false;
		for ( Cell cell : map.keySet() ) {
			CellSet set = map.get(cell);
			if (isSubsequent) sb.append(NL).append("\t, "); else isSubsequent = true;
			sb.append(rpad(cell.toString(),12)).append("=>").append(set.size()).append(":[");
			appendFS(sb, set, ",", ","); // only use of this method currently
			sb.append("]");
		}
		sb.append(" ]");
		return sb.toString();
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

	public static String interleave(List<ARegion> bases, List<ARegion> covers) {
		StringBuilder sb = getSB();
		final int n = 2 * Math.max(bases.size(), covers.size());
		Iterator<ARegion> b = bases.iterator();
		Iterator<ARegion> c = covers.iterator();
		boolean first = true;
		for ( int i=0; i<n; ++i ) {
			if(first) first=false; else sb.append(", ");
			try {
				sb.append(i%2==0 ? b.next() : c.next());
			} catch (NoSuchElementException ex) {
				// do nothing, just leave it blank
			}
		}
		// trim off any trailing ", "
		if ( sb.charAt(sb.length()-2)==',' )
			sb.setLength(sb.length()-2);
		return sb.toString();
	}

	private Frmt() {} // make this class non-instantiable
}
