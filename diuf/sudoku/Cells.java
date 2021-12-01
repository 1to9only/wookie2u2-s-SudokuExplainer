/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	public static HashSet<Cell> set(final Cell cell) {
		final HashSet<Cell> result = new HashSet<>();
		result.add(cell);
		return result;
	}

	/**
	 * Create a new HashSet containing the parameters array.
	 *
	 * @param cells
	 * @return a new {@code HashSet<Cell>}
	 */
	public static HashSet<Cell> set(final Cell... cells) {
		final HashSet<Cell> result = new HashSet<>(cells.length, 1.0F);
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
	public static ArrayList<Cell> list(final Cell[] cells, final int n) {
		final ArrayList<Cell> result = new ArrayList<>(n);
		for ( int i=0; i<n; ++i )
			result.add(cells[i]);
		return result;
	}

	/**
	 * Create a new array of cells from any sort of Collection of cells.
	 *
	 * @param cells
	 * @return
	 */
	public static Cell[] array(final Collection<Cell> cells) {
		final Cell[] result = new Cell[cells.size()];
		int i = 0;
		for ( Cell c : cells )
			result[i++] = c;
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
	private static final Cell[][] CASA = new Cell[GRID_SIZE+1][];
	private static final Cell[][] CASB = new Cell[GRID_SIZE+1][];
	static {
		// A cell has 20 siblings. The larger arrays are late-populated upon
		// use, except the last of 81 cells, which I presume is always used.
		for ( int i=0; i<21; ++i ) {
			CASA[i] = new Cell[i];
			CASB[i] = new Cell[i];
		}
		// CASA only has the 81-cell-array premade
		CASA[GRID_SIZE] = new Cell[GRID_SIZE];
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
	public static Cell[] arrayA(final int size) {
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
	public static Cell[] arrayB(final int size) {
		Cell[] cells = CASB[size];
		// late populate CellsArrayS 21..80
		if ( cells == null )
			cells = CASB[size] = new Cell[size];
		return cells;
	}

	public static void cleanCasA() {
		for ( int i=0; i<GRID_SIZE; ++i )
			if ( CASA[i] != null )
				Arrays.fill(CASA[i], null);
	}

	public static void cleanCasA(final int size) {
		if ( CASA[size] != null )
			Arrays.fill(CASA[size], null);
	}

	public static void cleanCasB() {
		for ( int i=0; i<GRID_SIZE; ++i )
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
	public static Cell[] copy(final Cell[] src, final int n) {
		final Cell[] dest = new Cell[n];
		System.arraycopy(src, 0, dest, 0, n);
		return dest;
	}

	/**
	 * Clear the given cells array.
	 *
	 * @param cells
	 */
	public static void clear(final Cell[] cells) {
		for ( int i=0,n=cells.length; i<n; ++i )
			cells[i] = null;
	}

	/**
	 * Read the {@link Cell#maybes} from 'cells' into 'result', ONCE for
	 * speed, rather than repeatedly dereferencing the cell repeatedly.
	 *
	 * @param cells to read: length determines how many
	 * @param result to repopulate
	 * @return pass-through the given result array
	 */
	public static int[] maybes(final Cell[] cells, final int[] result) {
		return maybes(cells, cells.length, result);
	}

	/**
	 * Read the {@link Cell#maybes} from 'cells' into 'result', ONCE for
	 * speed, rather than repeatedly dereferencing the cell repeatedly.
	 *
	 * @param cells to read
	 * @param n how many cells to read
	 * @param result to repopulate
	 * @return pass-through the given result array
	 */
	public static int[] maybes(final Cell[] cells, final int n, final int[] result) {
		for ( int i=0; i<n; ++i )
			result[i] = cells[i].maybes;
		return result;
	}

	/**
	 * Read the {@link Cell#i indices} of 'cells' into 'result', ONCE for
	 * speed, rather than repeatedly dereferencing the cell repeatedly.
	 *
	 * @param cells to read
	 * @param n number of cells
	 * @param result to repopulate
	 * @return how many
	 */
	public static int indices(final Cell[] cells, final int n, final int[] result) {
		for ( int i=0; i<n; ++i ) {
			result[i] = cells[i].i;
		}
		return n;
	}

	/**
	 * Repopulates 'result' with 'n' 'src' cells having size &lt; 'ceiling'.
	 *
	 * @param ceiling the maximum cell size plus one
	 * @param src source cells
	 * @param n number of source cells
	 * @param result destination cells array, to repopulate
	 * @return the number of cells in result
	 */
	public static int sized(final int ceiling, final Cell[] src, final int n, final Cell[] result) {
		int cnt = 0;
		for ( int i=0; i<n; ++i ) {
			if ( src[i].size < ceiling ) {
				result[cnt++] = src[i];
			}
		}
		return cnt;
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
	public static Idx cmnBudsNew(final Cell[] cells) {
		// it throws an NPE if cells is empty... let it!
		final Idx result = new Idx(cells[0].buds);
		for ( int i=1,e=cells.length-1; ; ) {
			result.and(cells[i].buds);
			if(++i>e) break;
		}
		return result;
	}

	private Cells() { } // Never used

}
