/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import java.util.ArrayList;
import java.util.HashSet;

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
	 * Create a new List from a re-usable cells array.
	 *
	 * @param cells
	 * @param numCells
	 * @return
	 */
	public static ArrayList<Cell> list(Cell[] cells, int numCells) {
		ArrayList<Cell> result = new ArrayList<>(numCells);
		for ( int i=0; i<numCells; ++i )
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
	 * See {@link #array(int size)}
	 */
	private static final Cell[][] CAS = new Cell[82][];
	static {
		// A cell has 20 siblings. The larger arrays are late-populated upon
		// use, except the last of 81 cells, which I presume is always used.
		for ( int i=0; i<21; ++i )
			CAS[i] = new Cell[i];
		CAS[81] = new Cell[81];
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
	public static Cell[] array(int size) {
		Cell[] cells = CAS[size];
		// late populate CellsArrayS 21..80
		if ( cells == null )
			cells = CAS[size] = new Cell[size];
		return cells;
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
	 * Returns a new Cell[] of the first n cells
	 * in the given re-usable cells array.
	 *
	 * @param n the number of cells to copy
	 * @param cells the re-usable cells array to copy from
	 * @return a new Cell[] containing a copy of the first n cells.
	 */
	public static Cell[] copy(int n, Cell[] cells) {
		Cell[] result = new Cell[n];
		System.arraycopy(cells, 0, result, 0, n);
		return result;
	}

	/**
	 * Extract maybes.bits from 'cells' into 'maybes'. Done for speed, rather
	 * than repeatedly dereferencing the bloody cell repeatedly.
	 *
	 * @param cells to read maybe.bits from
	 * @param maybes array to set
	 * @return maybes so that you can pass me a CAS or a new array, just so
	 *  it's still all in one line.
	 */
	public static int[] maybesBits(final Cell[] cells, final int[] maybes) {
		for ( int i=0,n=cells.length; i<n; ++i )
			maybes[i] = cells[i].maybes.bits;
		return maybes;
	}

	private Cells() { } // Never used

}
