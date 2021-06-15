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
	 * Cell ArrayS: late populated coz larger arrays were never used. These
	 * arrays are created once, rather than creating cells arrays on the fly
	 * throughout the application. It is faster to reuse an existing array.
	 * The intent is that the array is gotten, populated, and read; and then
	 * it's contents are forgotten. Be sure to clear the array when you have
	 * finished with it, because each Cell reference holds the whole grid in
	 * memory, and a Grid is "quite large" so if multiple grids are retained
	 * for no purpose it slows-down the whole application. Care is required.
	 * If you're unsure then just create your own cells array, for now, but
	 * come-back-to-it later and consider using the CAS for speed. Peruse any
	 * of the classes which call the array method before you start.
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
	 * cas (Cell ArrayS) returns the {@code Cell[]} of the given size from the
	 * Grids cell array cache, rather than create temporary arrays (slow).
	 * <p>
	 * If you stash the array then it's up to you to clone() it; because the
	 * array I return might be returned again the next time I'm called, so it's
	 * contents may be overwritten arbitrarily. <b>You have been warned!</b>
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
	 * Returns a new Cell[] of the first n cells.
	 *
	 * @param n the number of cells to copy
	 * @param cells the cells array to copy from
	 * @return a new Cell[] containing a copy of the first n cells.
	 */
	public static Cell[] copy(int n, Cell[] cells) {
		Cell[] result = new Cell[n];
		System.arraycopy(cells, 0, result, 0, n);
		return result;
	}

	private Cells() { } // Never used

}
