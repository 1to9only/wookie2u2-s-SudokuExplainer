/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import java.util.Collection;

/**
 * Cells exposes static helper methods for Grid.Cell; mainly dealing with
 * arrays and collections, for convenience, and to keep them all together.
 * <p>
 * They are many, warranting there own class. Grid is too fat with concerns,
 * hence I lighten his load with Cells and Regions.
 *
 * @author Keith Corlett 2021-05-06
 */
public class Cells {

	public static boolean arraysEquals(final Cell[] as, final Cell[] bs) {
		if ( as.length != bs.length )
			return false;
		final int n = as.length;
		for ( int i=0; i<n; ++i )
			if ( !as[i].equals(bs[i]) )
				return false;
		return true;
	}

	public static String[] ids(final Collection<Integer> indices) {
		if ( indices==null || indices.isEmpty() )
			return new String[]{""};
		final String[] result = new String[indices.size()];
		int i = 0;
		for ( int indice : indices )
			result[i++] = CELL_IDS[indice];
		return result;
	}

	/**
	 * Returns a new Idx of buddies common to all cells.
	 *
	 * @param cells to get the common buds of, must be full of cells, meaning
	 *  atleast 2; if theres only one cell you get-back all of its buds; but
	 *  if cell[0] is null I throw an NPE
	 * @param n number of cells
	 * @return a new Idx of the common buds
	 */
	public static Idx commonBuddiesNew(final Cell[] cells, final int n) {
		final Idx result = new Idx(cells[0].buds);
		for ( int i=1; i<n; ++i )
			result.and(cells[i].buds);
		return result;
	}

	private Cells() { } // Never used

}
