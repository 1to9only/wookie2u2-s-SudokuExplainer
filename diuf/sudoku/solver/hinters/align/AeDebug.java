/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.MAYBES_STR;
import diuf.sudoku.solver.hinters.align.AlignedExclusion.CellStackEntry;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.NL;

/**
 * AeDebug contains static methods and constants that assist with
 * debugging {@link AlignedExclusion} in the Netbeans 8.2 debugger.
 * <p>
 * I recommend NOT .* static importing my methods, to avert name clashes.
 * <p>
 * Most of this code is generalisable, but some formats are specific
 * to {@link AlignedExclusion}, like single digit indexes. It can be
 * adapted to just about anything that uses anything, with a little
 * farnarckling.
 *
 * @author Keith Corlett 2023-06-15
 */
class AeDebug {

	static interface IntFormatter { String format(int i); }
	static interface BooleanFormatter { String format(boolean b); }

	static final IntFormatter MAYBES_FORMAT = (int cands)->MAYBES_STR[cands];

	static final IntFormatter CELL_ID_FORMAT = (int i)->CELL_IDS[i];

	// X is 10 in roman numerals. I want 11 indexes, for an overone.
	static final String[] ARRAY_INDEXES = {"0","1","2","3","4","5","6","7","8","9","X"};
	static final IntFormatter ARRAY_INDEX_FORMAT = (int i)->ARRAY_INDEXES[i];

	// for if your array.length > 10
	static final IntFormatter INTEGER_FORMAT = (int i)->Integer.toString(i);

	// nb: 'T' and '_' are the same width in a non-fixed-width font
	static final BooleanFormatter BOOLEAN_FORMAT = (boolean b)->(b ? "T" : "_");

	static final boolean DEBUG = true;
	static void DEBUGLN() {
		if(DEBUG)System.out.println();
	}
	static void DEBUGLN(final String msg) {
		if(DEBUG)System.out.println(msg);
	}
	static void DEBUG(final String frmt, final Object... args) {
		if(DEBUG)System.out.format(frmt, args);
	}

	static String cellStackToString(final CellStackEntry[] cellStack) {
		if ( cellStack == null )
			return "null";
		final StringBuilder sb = SB(32);
		boolean first = true;
		for ( int n=cellStack.length,i=0; i<n; ++i ) {
			if ( first )
				first = false;
			else
				sb.append(NL).append(", ");
			sb.append(cellStack[i]); // use CellStackEntry#toString()
		}
		return sb.toString();
	}

	static String maybesToString(final int[] maybess, final int n) {
		final StringBuilder sb = SB(32);
		sb.append(MAYBES_FORMAT.format(maybess[0]));
		for ( int i=1; i<n; ++i )
			sb.append(", ").append(MAYBES_FORMAT.format(maybess[i]));
		return sb.toString();
	}

	static String maybesToString(final int[] maybess) {
		return maybesToString(maybess, maybess.length);
	}

	static String maybesStrings(final CellStackEntry[] cellStack, final int n) {
		final StringBuilder sb = SB(32);
		sb.append(MAYBES_FORMAT.format(cellStack[0].maybes));
		for ( int i=1; i<n; ++i )
			sb.append(", ").append(MAYBES_FORMAT.format(cellStack[i].maybes));
		return sb.toString();
	}

	static String cellIds(final CellStackEntry[] cellStack, final int n) {
		final StringBuilder sb = SB(32);
		sb.append(CELL_ID_FORMAT.format(cellStack[0].indice));
		for ( int i=1; i<n; ++i )
			sb.append(", ").append(CELL_ID_FORMAT.format(cellStack[i].indice));
		return sb.toString();
	}

	static StringBuilder appendI(final StringBuilder sb, final int[] array) {
		if ( array == null )
			sb.append("null");
		else {
			for ( int n=array.length,i=0; i<n; ++i )
				sb.append(ARRAY_INDEX_FORMAT.format(array[i]));
		}
		return sb;
	}

	static StringBuilder append(final StringBuilder sb, final int[] array) {
		if ( array == null )
			sb.append("null");
		else {
			sb.append('[');
			boolean first = true;
			for ( int n=array.length,i=0; i<n; ++i ) {
				if ( first )
					first = false;
				else
					sb.append(' ');
				// array contains SINGLE digit ints
				sb.append(Integer.toString(array[i]));
			}
			sb.append(']');
		}
		return sb;
	}

	static String toString(final int[] a) {
		return append(SB(32), a).toString();
	}

	static String toString(final int[][] arrays) {
		if ( arrays == null )
			return "null";
		final StringBuilder sb = SB(32);
		boolean first = true;
		for ( int n=arrays.length,i=0; i<n; ++i ) {
			if ( first )
				first = false;
			else
				sb.append(", ");
			append(sb, arrays[i]);
		}
		return sb.toString();
	}

	static String toString(final boolean[][] arrays) {
		if ( arrays == null )
			return "null";
		final StringBuilder sb = SB(32);
		boolean firstArray = true;
		for ( int n=arrays.length,i=0; i<n; ++i ) {
			if ( firstArray )
				firstArray = false;
			else
				sb.append(Frmt.CSP);
			if ( arrays[i] == null )
				sb.append(Frmt.EMPTY_ARRAY);
			else {
//				sb.append('[');
				for ( int J=arrays[i].length,j=0; j<J; ++j )
					sb.append(BOOLEAN_FORMAT.format(arrays[i][j]));
//				sb.append(']');
			}
		}
		return sb.toString();
	}

	static void assertEquals(final int expect, final int actual) {
		if ( DEBUG )
			if ( actual != expect )
				throw new RuntimeException("unequal: actual="+actual+" != expect="+expect);
	}
	static void assertEquals(final String label, final int index, final int expect, final int actual) {
		if ( DEBUG )
			if ( actual != expect )
				throw new RuntimeException(label+" unequal: "+index+": actual="+actual+" != expect="+expect);
	}
	static void assertArrayEquals(final String label, final int[] expect, final int[] actual) {
		if ( DEBUG ) {
			assertEquals(expect.length, actual.length);
			for ( int i=0; i<expect.length; ++i )
				assertEquals(label, i, expect[i], actual[i]);
		}
	}
	static void assertEquals(final int index, final int expect, final int actual) {
		if ( DEBUG )
			if ( actual != expect )
				throw new RuntimeException("unequal: "+index+": actual="+actual+" != expect="+expect);
	}

	static void assertEquals(final int index, final long expect, final long actual) {
		if ( DEBUG )
			if ( actual != expect )
				throw new RuntimeException("unequal");
	}
	static void assertArrayEquals(final long[] expect, final long[] actual) {
		if ( DEBUG ) {
			assertEquals(expect.length, actual.length);
			for ( int i=0; i<expect.length; ++i )
				assertEquals(i, expect[i], actual[i]);
		}
	}

	static void assertEquals(final boolean expect, final boolean actual) {
		if ( DEBUG ) {
			if ( actual != expect )
				throw new RuntimeException("unequal");
		}
	}

	static void assertArrayEquals(final boolean[] expect, final boolean[] actual) {
		if ( DEBUG ) {
			assertEquals(expect.length, actual.length);
			for ( int i=0; i<expect.length; ++i )
				assertEquals(expect[i], actual[i]);
		}
	}

}
