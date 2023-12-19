/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.utils.Frmt.*;
import static diuf.sudoku.utils.MyStrings.SB;
import java.util.Collection;

/**
 * I am named Frmu because u follows t.
 * <p>
 * Frmu is Frmt for app-specific types, so that utils (including Frmt) remains
 * portable between projects. Frmu and Frmt are conjoined twins, hence must be
 * in the same package, but Frmu does NOT really belong in utils, because it
 * contains application-types. When setting-up a new project gut Frmu, and
 * start again from torts.
 *
 * @author Keith Corlett 2010-06-20
 */
public class Frmu {

	/**
	 * Returns Space Separated Values of cell.id of $indices.
	 *
	 * @param indices to stringify
	 * @return SSV of cell.id of indices
	 */
	public static String ids(Collection<Integer> indices) {
		return Frmt.frmt(indices, (i)->CELL_IDS[i], SP, SP);
	}

	// ========================== ARegion ============================

	/**
	 * Returns CSV (Comma Separated Values) formatted $regions.
	 *
	 * @param regions to format
	 * @return CSV formatted $regions
	 */
	public static StringBuilder csv(ARegion[] regions) {
		if ( regions==null )
			return SB(16).append(NULL_STRING); // WARN: in dumps of MANY test-cases!
		return csv(SB(regions.length<<3), regions);
	}

	/**
	 * Append $regions to $sb in CSV (Comma Separated Values) format.
	 *
	 * @param sb to append to
	 * @param regions to append
	 * @return sb with regions appended
	 */
	public static StringBuilder csv(final StringBuilder sb, final ARegion[] regions) {
		if ( regions == null )
			return sb.append(NULL_STRING);
		boolean first = true;
		for ( ARegion r : regions ) {
			if(first) first=false; else sb.append(CSP);
			sb.append(r.label);
		}
		return sb;
	}

	public static StringBuilder basesAndCovers(final ARegion[] bases, final ARegion[] covers) {
		final int capacity;
		if ( bases!=null && covers!=null ) // avert NPE
			capacity = (bases.length+covers.length)<<3; // * 8
		else
			capacity = 128;
		return basesAndCovers(SB(capacity), bases, covers);
	}

	public static StringBuilder basesAndCovers(final StringBuilder sb, final ARegion[] bases, final ARegion[] covers) {
		return csv(csv(sb, bases).append(AND), covers);
	}

	// ============================ Cell ==============================

	/**
	 * Currently only used for debug logging. This handles a null-terminated
	 * array for when you have a fixed-size array you are re-using and do not
	 * know how many are currently present. It is easy to null next upon add,
	 * presuming that your array is one larger than is ever added.
	 *
	 * @param sep the separator between cells
	 * @param numCells the number of cells in cells
	 * @param cells to be formatted
	 * @return a CSV list of the cell.toFullString() of each cell in cells
	 * (which may be a null-terminated array)
	 */
	public static String toFullString(final String sep, final int numCells, final Cell[] cells) {
		if ( cells == null )
			return NULL_STRING;
		final StringBuilder sb = SB(numCells<<5); // * 32
		sb.setLength(0);
		final int n = Math.min(numCells, cells.length);
		for ( int i=0; i<n; ++i ) {
			if ( cells[i] == null )
				break; // null terminated list
			if ( i > 0 )
				sb.append(sep);
			sb.append(cells[i].toFullString());
		}
		return sb.toString();
	}

	/**
	 * retain for debugging
	 *
	 * @param cells {@code Iterable<Cell>} to be formatted.
	 * @param sep cell separator String. The default is ", "
	 * @return a CSV list of the cell.toFullString() of each cell in cells
	 * (which may be a null-terminated array).
	 */
	public static String toFullString(final String sep, final Iterable<Cell> cells) {
		if ( cells == null )
			return NULL_STRING;
		final StringBuilder sb = SB(128);
		int i = 0;
		for ( Cell cell : cells ) {
			if ( cell == null )
				break; // null terminated list
			if ( ++i > 1 )
				sb.append(sep);
			sb.append(cell.toFullString());
		}
		return sb.toString();
	}
	public static String toFullString(Iterable<Cell> cells) {
		return toFullString(CSP, cells);
	}

	public static String csv(final Cell c1) { return c1.id; }
	public static String csv(final Cell c1, final Cell c2) { return c1.id+CSP+c2.id; }
	public static String csv(final Cell c1, final Cell c2, final Cell c3) { return c1.id+CSP+c2.id+CSP+c3.id; }
	public static String csv(final Cell[] cells) { return frmt(cells, cells.length, CSP, CSP); }
	public static String csv(final int n, final Cell[] cells) { return frmt(cells, n, CSP, CSP); }
	public static String ssv(final Cell[] cells) { return frmt(cells, cells.length, SP, SP); }
	public static String ssv(final int n, final Cell[] cells) { return frmt(cells, n, SP, SP); }
	public static String and(final Cell[] cells) { return frmt(cells, cells.length, CSP, AND); }
	public static String and(final Cell c1, final Cell c2) { return c1.id+AND+c2.id; }
	public static String or(final Cell c1, final Cell c2) { return c1.id+OR+c2.id; }
	public static String or(final Cell[] cells) { return frmt(cells, cells.length, CSP, OR); }
	public static String frmt(final Cell[] cells, final int n, final String sep, final String lastSep) {
		return append(SB(128), cells, n, sep, lastSep).toString();
	}
	public static StringBuilder append(final StringBuilder sb, final Cell[] cells, final int n, final String sep, final String lastSep) {
		if ( cells == null )
			return sb;
		for ( int i=0,m=n-1; i<n; ++i ) {
			if ( cells[i] == null )
				break; // it is a null terminated array
			if ( i > 0 )
				// SPEED: NO TERNIARY!
				if ( i < m )
					sb.append(sep);
				else
					sb.append(lastSep);
			sb.append(cells[i].id);
		}
		return sb;
	}

	public static String csv(final Collection<Cell> cells) { return frmt(cells, CSP, CSP); }
	public static String ssv(final Collection<Cell> cells) { return frmt(cells, SP, SP); }
	public static String and(final Collection<Cell> cells) { return frmt(cells, CSP, AND); }
	public static String or(final Collection<Cell> cells) { return frmt(cells, CSP, OR); }
	public static String frmt(final Collection<Cell> cells, final String sep, final String lastSep) {
		final StringBuilder sb = SB(128);
		return append(sb, cells, sep, lastSep).toString();
	}
	public static StringBuilder append(final StringBuilder sb, final Collection<Cell> cells, final String sep, final String lastSep) {
		if ( cells == null )
			return sb;
		final int n = cells.size();
		int i = 0;
		for ( Cell c : cells ) {
			if ( c==null)
				break; // null terminated list!
			if ( ++i > 1 )
				// SPEED: NO TERNIARY!
				if ( i < n )
					sb.append(sep);
				else
					sb.append(lastSep);
			sb.append(c.id);
		}
		return sb;
	}

	public static StringBuilder appendFullString(StringBuilder sb, Collection<Cell> cells, final String sep, final String lastSep) {
		if(cells==null) return sb;
		int m = cells.size() - 1; // m is n-1
		int i = 0;
		for ( Cell c : cells ) {
			if ( ++i > 1 )
				// SPEED: NO TERNIARY!
				if ( i < m )
					sb.append(sep);
				else
					sb.append(lastSep);
			sb.append(c.toFullString());
		}
		return sb;
	}

	// ============================ Values ==============================

	public static String csv(Values vs) { return frmt(vs, CSP, CSP); }
	public static String and(Values vs) { return frmt(vs, CSP, AND); }
	public static String or(Values vs) { return frmt(vs, CSP, OR); }
	public static String frmt(Values vs, String sep, String lastSep) {
		if(vs==null) return EMPTY_STRING;
		final StringBuilder sb = SB(128);
		return appendTo(sb, vs.bits, vs.size, sep, lastSep).toString();
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
				// SPEED: NO TERNIARY!
				if ( i < size )
					sb.append(sep);
				else
					sb.append(lastSep);
			sb.append(v);
		}
		return sb;
	}

	public static String values(Integer vs) {
		return Frmt.plural(Values.VSIZE[vs], "value")+SP+Values.andString(vs);
	}

	private Frmu() { }
}
