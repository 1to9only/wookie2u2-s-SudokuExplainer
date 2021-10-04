/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * The Cells class contains static helper methods for collections of Grid.Cell.
 * There are so many of them that I though it was worth removing them from the
 * Grid class, which is already too big.
 *
 * @author Keith Corlett 2021-05-06
 */
public class Cells {

	/**
	 * Create a new HashSet containing the given Cell.
	 *
	 * @param cell
	 * @return a new {@code HashSet<Cell>}
	 */
	public static HashSet<Cell> set(Cell cell) {
		HashSet<Cell> result = new HashSet<>();
		result.add(cell);
		return result;
	}

	/**
	 * Create a new HashSet containing the parameters array.
	 *
	 * @param cells
	 * @return a new {@code HashSet<Cell>}
	 */
	public static HashSet<Cell> set(Cell... cells) {
		HashSet<Cell> result = new HashSet<>(cells.length, 1.0F);
		for ( Cell c : cells )
			result.add(c);
		return result;
	}

	/**
	 * Splitting them out by types means no type-erasure contention, so you
	 * can have "plain" list method.
	 *
	 * @param cells
	 * @return 
	 */
	public static ArrayList<Cell> list(Cell[] cells) {
		return list(cells, cells.length);
	}
	
	/**
	 * Create a new List from a re-usable cells array.
	 *
	 * @param cells
	 * @param n
	 * @return
	 */
	public static ArrayList<Cell> list(Cell[] cells, int n) {
		ArrayList<Cell> result = new ArrayList<>(n);
		for ( int i=0; i<n; ++i )
			result.add(cells[i]);
		return result;
	}

	// ============================= Cell arrays ==============================

	/**
	 * CAS (Cell ArrayS) are created ONCE, for re-use, rather than creating
	 * temporary cells arrays on the fly throughout the codebase. It's just a
	 * bit faster to reuse an array verses rolling your own on the fly.
	 * <p>
	 * The intent is that the array is gotten, populated, and read; and then
	 * your reference is forgotten. Be sure to "forget" your array reference
	 * when you have finished with it, because every Cell reference holds the
	 * whole grid in memory, and a Grid is "quite large" so if multiple grids
	 * are retained (for no purpose) it slows-down the whole application.
	 * <p>
	 * Care is required when using the CAS. If you're unsure then just create
	 * your own cells array, for now, but come-back-to-it later and try using
	 * the CAS for speed. Peruse an existing class that uses the CAS before you
	 * use it for the first time.
	 * <p>
	 * The arrays of 0..20 and 81 are created at start-up. The rest of the CAS
	 * is late-populated rather than creating many large arrays that are simply
	 * never used.
	 * <p>
	 * See {@link #arrayA(int)}
	 */
	private static final Cell[][] CASA = new Cell[82][];
	private static final Cell[][] CASB = new Cell[82][];
	static {
		// A cell has 20 siblings. The larger arrays are late-populated upon
		// use, except the last of 81 cells, which I presume is always used.
		for ( int i=0; i<21; ++i ) {
			CASA[i] = new Cell[i];
			CASB[i] = new Cell[i];
		}
		// CASA only has the 81-cell-array premade
		CASA[81] = new Cell[81];
	}

	/**
	 * Returns the {@code Cell[]} of the given size from the CAS (Cell ArrayS
	 * cache), rather than creating new temporary arrays everywhere, which is a
	 * bit slow.
	 * <p>
	 * The intention is that I grant you (my caller) a "lease" on the returned
	 * array: so you populate the array, you loop through it, and then you just
	 * forget it; typically using automatic variables, which simply dissappear
	 * with your stackframe. You cannot use the CAS or call a method which uses
	 * the CAS inside a method that uses the CAS. I'm single use baby! The CAS
	 * is just a speed thing, if you strike any trouble then I recommend you
	 * "Don't use the bloody CAS!".
	 * <p>
	 * <b>WARNING:</b> If you retain the returned array then it's up to you to
	 * clone() it, because the same array may be returned again the next time I
	 * am called, arbitrarily overwriting it's contents. Usually this situation
	 * means your method/class needs it's own re-usable cell array, rather than
	 * taking the convenient CAS route.
	 * <p>
	 * Note that cell-arrays of size 1..20 and 81 are created at start-up, but
	 * all the rest are created on demand, rather than create lots of unused
	 * arrays; yielding good (I hope) balance between memory use and speed.
	 *
	 * @param size the size of the cached array to retrieve
	 * @return the <b>cached</b> {@code Cell[]}. Did I mention it's cached?
	 */
	public static Cell[] arrayA(int size) {
		Cell[] cells = CASA[size];
		// late populate CellsArrayS 21..80
		if ( cells == null )
			cells = CASA[size] = new Cell[size];
		return cells;
	}

	/**
	 * As per arrayA above, except my arrays are in a separate CASB.
	 * @param size the size of the cached array to retrieve
	 * @return the <b>cached</b> {@code Cell[]}. Did I mention it's cached?
	 */
	public static Cell[] arrayB(int size) {
		Cell[] cells = CASB[size];
		// late populate CellsArrayS 21..80
		if ( cells == null )
			cells = CASB[size] = new Cell[size];
		return cells;
	}

	public static void cleanCasA() {
		for ( int i=0; i<81; ++i )
			if ( CASA[i] != null )
				Arrays.fill(CASA[i], null);
	}

	public static void cleanCasA(int i) {
		if ( CASA[i] != null )
			Arrays.fill(CASA[i], null);
	}

	public static void cleanCasB() {
		for ( int i=0; i<81; ++i )
			if ( CASB[i] != null )
				Arrays.fill(CASB[i], null);
	}

	/**
	 * Return a new Cell[n] containing a copy of the first n elements of src.
	 *
	 * @param src
	 * @param n
	 * @return
	 */
	public static Cell[] copy(Cell[] src, int n) {
		Cell[] dest = new Cell[n];
		System.arraycopy(src, 0, dest, 0, n);
		return dest;
	}

	/**
	 * Clear the given cells array.
	 *
	 * @param cells
	 */
	public static void clear(Cell[] cells) {
		for ( int i=0,n=cells.length; i<n; ++i )
			cells[i] = null;
	}

	/**
	 * Extract maybes from 'cells' into 'maybes'. Done for speed, rather
	 * than repeatedly dereferencing the bloody cell repeatedly.
	 *
	 * @param cells to read maybe.bits from
	 * @param maybes array to set
	 * @return maybes so that you can pass me a CAS or a new array, just so
	 *  it's still all in one line. Note that maybes.length is how many to read
	 *  so maybes must be exactly the right size, so that cells can be a fixed
	 *  size array.
	 */
	public static int[] maybesBits(final Cell[] cells, final int[] maybes) {
		for ( int i=0,n=maybes.length; i<n; ++i )
			maybes[i] = cells[i].maybes;
		return maybes;
	}

	/**
	 * commonBuddiesNew: a new Idx of buds common to all given cells.
	 * <p>
	 * Performance: I'm a tad slow because I use an iterator, which is the cost
	 * of taking an Iterable over a List, so it'll work for a Set also, but not
	 * an Cell[] array, which annoys me. Arrays should be Iterable.
	 *
	 * @param cells to get the common buds of
	 * @return a new Idx of the common buds
	 */
	public static Idx cmnBudsNew(Iterable<Cell> cells) {
		Iterator<Cell> it = cells.iterator();
		// it throws an NPE if cells is empty... let it!
		final Idx result = new Idx(it.next().buds);
		while (it.hasNext()) {
			result.and(it.next().buds);
		}
		return result;
	}

	private Cells() { } // Never used

}
