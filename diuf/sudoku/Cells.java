/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * The Cells class contains static helper methods for collections of Grid.Cell.
 * There are so many of them that I though it was worth removing them from the
 * Grid class, which is already too big.
 *
 * @author Keith Corlett 2021-05-06
 */
public class Cells {

	public static final Cell[] EMPTY_CELLS_ARRAY = new Cell[0];

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
	public static HashSet<Cell> set(final Cell[] cells) {
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
	public static Cell[] arrayNew(final Collection<Cell> cells) {
		final Cell[] result = new Cell[cells.size()];
		int i = 0;
		for ( Cell c : cells )
			result[i++] = c;
		return result;
	}

	public static boolean arraysEquals(final Cell[] as, final Cell[] bs) {
		if ( as.length != bs.length )
			return false;
		final int n = as.length;
		for ( int i=0; i<n; ++i )
			if ( !as[i].equals(bs[i]) )
				return false;
		return true;
	}

	// =========================== CELL ARRAY LEASE ===========================

	/**
	 * CellArrayLease is a "lease" over a Cell[], whose close method clears it
	 * and returns it to the pool, making it available for reuse the next time
	 * you want a cell[]. Leasing arrays is my humble attempt at overcoming how
	 * slow Java is at creating new arrays, and less garbage means less garbage
	 * collection.
	 */
	public static final class CALease implements java.io.Closeable {
		private boolean free;
		public final Cell[] array;
		public CALease(final int size) {
			this.array = new Cell[size];
		}
		public CALease take() {
			this.free = false;
			return this;
		}
		@Override
		public void close() {
			Arrays.fill(array, null);
			this.free = true;
		}
	}

	/**
	 * This class exists because arrays of generics are pigs.
	 */
	private static final class CALeaseList extends ArrayList<CALease> {
		private static final long serialVersionUID = 486727450901L;
	}

	/**
	 * An array of 81 {@code List<CLease>}. So a list per array size.
	 */
	private static final CALeaseList[] CA_LEASES = new CALeaseList[81];

	/**
	 * Get a CALease: a Cell[] with auto-return. The close method returns the
	 * array to the pool, so that if you treat me as a resource (a try-block)
	 * the array is automatically cleared and returned to the pool; solving the
	 * competing problems that creating arrays on the fly in Java is slow, but
	 * avoiding creating arrays on the fly in Java is a total pain in the ass.
	 * Leases are still a pain in the ass, just less so.
	 * <p>
	 * NOTE that CALease.close clears the array, because holding just one Cell
	 * reference holds the whole Grid in memory (a classic memory leak).
	 * <p>
	 * Performance is no better than creating arrays on the fly, mainly coz I
	 * use an Iterator over the appropriate CALeaseList, which is slow. I tried
	 * one way to not use an Iterator (MyArrayList exposes elementData), but it
	 * was no faster. Sigh.
	 *
	 * @param size of the required Cell[]
	 * @return a lease over a Cell[], whose close clears it and returns it to
	 *  the pool
	 */
	public static CALease caLease(final int size) {
		if ( CA_LEASES[size] == null )
			CA_LEASES[size] = new CALeaseList();
		else
			for ( CALease lease : CA_LEASES[size] ) // slow Iterator
				if ( lease.free )
					return lease.take();
		final CALease lease = new CALease(size);
		CA_LEASES[size].add(lease);
		return lease;
	}

	// ========================================================================

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
		for ( int i=0; i<n; ++i )
			if ( src[i].size < ceiling )
				result[cnt++] = src[i];
		return cnt;
	}

	/**
	 * Returns a new Idx of buddies common to all 'cells'.
	 *
	 * @param cells to get the common buds of, must be full of cells, meaning
	 *  atleast 2; if there's only one cell you get-back all of it's buds; but
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
