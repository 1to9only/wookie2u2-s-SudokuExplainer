/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.hinters.align.CellSet;
import static diuf.sudoku.utils.Frmt.NL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.NULL_ST;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.COMMA;
import static diuf.sudoku.utils.Frmt.and;
import static diuf.sudoku.utils.Frmt.OR;
import static diuf.sudoku.utils.Frmt.or;
import static diuf.sudoku.utils.Frmt.COMMA_SP;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.SPACE;

/**
 * Frmu (that's the letter following t) is Frmt for Sudoku Explainer types, so
 * Frmt (like everything else in utils) remains portable between projects. Frmu
 * and Frmt are conjoined twins, so are in the same package. Remove Frmu after
 * copying utils to another project, and strip Debug of all app-specific types.
 *
 * @author Keith Corlett 2010-06-20
 */
public class Frmu {

	// SB is "loaned" publicly via getSB()
	// It's up to you to not use the same SB twice. BIG Sigh.
	private static final StringBuilder SB = new StringBuilder(64);
	// MYSB is all mine, gotten with getMySB(). It's up to me to not use the
	// same SB twice. MYSB helps a lot. Presume that SB is on loan and use MYSB
	// EVERYwhere internally. If you want another SB then re-jig the problem,
	// or create a new SB on-the-fly. We can only go so far caching SB's before
	// the speed increases gained but not creating and growing buffers OTF all
	// the time gets head-onned by depleated RAM coz some dopey bastard holds
	// a big buffer he won't let go of, despite the fact that it's used rarely.
	// You cannot have it both ways. So two re-used buffers make sense, but a
	// third stinks like cock. So create him on-the-fly. Clear.
	private static final StringBuilder MYSB = new StringBuilder(64);

	// ========================== ARegion ============================

	// nb: List binds before Collection to avert type-erasure problem
	public static String and(List<ARegion> list) {
		return Frmt.and((Object[])list.toArray(new ARegion[list.size()]));
	}
	// nb: List binds before Collection to avert type-erasure problem
	public static String csv(List<ARegion> regions) {
		if ( regions == null )
			return NULL_ST;
		StringBuilder sb = getMySB(7 * regions.size());
		boolean first = true;
		for ( ARegion r : regions ) {
			if(first) first=false; else sb.append(COMMA_SP);
			sb.append(r.id);
		}
		return sb.toString();
	}

	// nb: Set binds before Collection to avert type-erasure problem
	public static String csv(Set<ARegion> regions) {
		return csv(getMySB(regions.size()<<3), regions).toString();
	}

	/** Append comma separated regions to sb. */
	public static StringBuilder csv(StringBuilder sb, Iterable<ARegion> regions) {
		if ( regions == null )
			return sb.append(NULL_ST);
		boolean first = true;
		for ( ARegion r : regions ) {
			if ( first )
				first = false;
			else
				sb.append(COMMA_SP);
			sb.append(r.id);
		}
		return sb;
	}

	public static StringBuilder basesAndCovers(List<ARegion> bases, List<ARegion> covers) {
		return basesAndCovers(getMySB((bases.size()+covers.size())<<3), bases, covers);
	}
	public static StringBuilder basesAndCovers(StringBuilder sb, List<ARegion> bases, List<ARegion> covers) {
		return csv(csv(sb, bases).append(AND), covers);
	}

	public static String interleave(List<ARegion> bases, List<ARegion> covers) {
		final int n = 2 * Math.max(bases.size(), covers.size());
		final StringBuilder sb = getMySB(n<<3);
		final Iterator<ARegion> b = bases.iterator();
		final Iterator<ARegion> c = covers.iterator();
		boolean first = true;
		for ( int i=0; i<n; ++i ) {
			if(first) first=false; else sb.append(COMMA_SP);
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

	// nb: List binds before Collection to avert type-erasure problem
	public static String ssv(List<ARegion> regions) {
		if ( regions == null )
			return NULL_ST;
		StringBuilder sb = getMySB(7 * regions.size());
		boolean first = true;
		for ( ARegion r : regions ) {
			if(first) first=false; else sb.append(SPACE);
			sb.append(r.id);
		}
		return sb.toString();
	}

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
		if(cells==null) return NULL_ST;
		final StringBuilder sb = getMySB(numCells * 20);
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
	 * Currently only ever used to format watches in the debugger, where it's
	 * very useful, so keep it, even though it's not used, because it is. See?
	 * @param cells {@code Iterable<Cell>} to be formatted.
	 * @param sep cell separator String. The default is ", "
	 * @return a CSV list of the cell.toFullString() of each cell in cells
	 * (which may be a null-terminated array).
	 */
	public static String toFullString(String sep, Iterable<Cell> cells) {
		if(cells==null) return NULL_ST;
		final StringBuilder sb = getMySB(128);
		int i = 0;
		// Iterator used to handle null-terminated lists, which Nutbeans
		// is sure can't possibly exist. F___ing Nutbeans!
		Cell cell;
		for ( Iterator<Cell> it = cells.iterator(); it.hasNext(); ) {
			if ( (cell=it.next()) == null )
				break; // null terminated list
			if ( ++i > 1 )
				sb.append(sep);
			sb.append(cell.toFullString());
		}
		return sb.toString();
	}

	public static String toFullString(Iterable<Cell> cells) {
		return toFullString(COMMA_SP, cells);
	}

	public static String csv(Cell... cells) { return frmt(cells, cells.length, COMMA_SP, COMMA_SP); }
	public static String csv(int n, Cell... cells) { return frmt(cells, n, COMMA_SP, COMMA_SP); }
	public static String ssv(Cell... cells) { return frmt(cells, cells.length, SPACE, SPACE); }
	public static String ssv(int n, Cell... cells) { return frmt(cells, n, SPACE, SPACE); }
	public static String and(Cell... cells) { return frmt(cells, cells.length, COMMA_SP, AND); }
	public static String or(Cell... cells) { return frmt(cells, cells.length, COMMA_SP, OR); }
	public static String frmt(Cell[] cells, int n, final String sep, final String lastSep) {
		final StringBuilder sb = getMySB(128);
		return append(sb, cells, n, sep, lastSep).toString();
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

	public static String csv(final Collection<Cell> cells) { return frmt(cells, COMMA_SP, COMMA_SP); }
	public static String ssv(final Collection<Cell> cells) { return frmt(cells, SPACE, SPACE); }
	public static String and(final Collection<Cell> cells) { return frmt(cells, COMMA_SP, AND); }
	public static String or(final Collection<Cell> cells) { return frmt(cells, COMMA_SP, OR); }
	public static String frmt(final Collection<Cell> cells, final String sep, final String lastSep) {
		final StringBuilder sb = getMySB(128);
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
				// note: i<n only works coz i is 1-based and n is 0-based;
				// but it works so don't ____ with it.
				sb.append(i<n ? sep : lastSep);
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
				sb.append(i<m ? sep : lastSep);
			sb.append(c.toFullString());
		}
		return sb;
	}

	// Currently only used by Debug.diff to format CellSet's out of the exclusion map.
	static String excludersMap(HashMap<Cell, CellSet> map) {
		StringBuilder sb = getMySB(map.size()*32);
		sb.setLength(0);
		sb.append(map.size()).append(": [").append(NL).append("\t  ");
		boolean first = true;
		for ( Cell key : map.keySet() ) {
			CellSet set = map.get(key);
			if(first) first=false; else sb.append(NL).append("\t, ");
			sb.append(Frmt.rpad(key.toString(),12)).append("=>").append(set.size()).append(":[");
			appendFullString(sb, set, COMMA_SP, COMMA_SP); // only use of this method currently
			sb.append("]");
		}
		sb.append(" ]");
		return sb.toString();
	}

	// ============================ Values ==============================

	public static String csv(Values vs) { return frmt(vs, COMMA_SP, COMMA_SP); }
	public static String and(Values vs) { return frmt(vs, COMMA_SP, AND); }
	public static String or(Values vs) { return frmt(vs, COMMA_SP, OR); }
	public static String frmt(Values vs, String sep, String lastSep) {
		if(vs==null) return EMPTY_STRING;
		final StringBuilder sb = getMySB(128);
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
				sb.append(i<size ? sep : lastSep);
			sb.append(v);
		}
		return sb;
	}

	public static String values(Values vs) {
		return Frmt.plural(vs.size, "value")+SPACE+Frmu.and(vs);
	}

	public static StringBuilder getSB() {
		SB.setLength(0);
		return SB;
	}
	public static StringBuilder getSB(int initialCapacity) {
		SB.setLength(0);
		SB.ensureCapacity(initialCapacity);
		return SB;
	}

	private static StringBuilder getMySB(int initialCapacity) {
		MYSB.setLength(0);
		MYSB.ensureCapacity(initialCapacity);
		return MYSB;
	}

	private Frmu() { }
}
