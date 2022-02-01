/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.GRID_SIZE;
import java.io.Closeable;
import java.util.ArrayList;

/**
 * IntArrays leases-out an int[], whose close returns it. This allows us to
 * re-use int-arrays to avoid the expense of creating a new array on the fly.
 * The underlying problem is Javas array creation is slow, especially when you
 * include subsequent Garbage Collection. Leases are a tad more efficient. The
 * downside is re-used arrays are re-used, so may contain old-junk, which is no
 * problem if (as is normal) you repopulate a right-sized array completely.
 * <p>
 * Note that IntArrays is an SE class (not utils) only coz it uses GRID_SIZE to
 * determine the maximum size of a cachable int-array. An AIOOBE means your
 * array is too big for me, so just create it yourself. Sigh.
 * <pre>
 * KRC 2021-12-27 IALeaseList extends {@link diuf.sudoku.utils.MyArrayList}
 * which is a hack of java.util.ArrayList that expose it's elementData array,
 * coz a native Java array-iterator is just a bit faster than Iterator with two
 * invocations per element. Downside is it's a raw null-terminated Object[].
 *
 * KRC 2021-12-27 after performance testing I deleted MyArrayList. No matter
 * what you do, you can't beat java.util, coz it's totally optimised. It's
 * cleaner without it.
 * </pre>
 *
 *
 * @author Keith Corlett 2021-12-26
 */
public final class IntArrays {

	/**
	 * A Lease over an int[], whose close returns it to the pool.
	 */
	public final static class IALease implements Closeable {
		private boolean free;
		public final int[] array;
		public IALease(int n) {
			this.array = new int[n];
		}
		public IALease take() {
			this.free = false;
			return this;
		}
		@Override
		public void close() {
			free = true;
		}
		@Override
		public String toString() {
			return free ? "free" : "busy";
		}
	}

	// this class exists just to avoid the generic array problem.
	private static class IALeaseList extends ArrayList<IALease> {
		private static final long serialVersionUID = 131412121413L;
	}

	// I don't know how big this needs to be, but an unused LeaseList remains
	// null, I think, until it's created, so we're not wasting too much RAM.
	// NB: GRID_SIZE+1 to create the int[81] for all cells.
	private static final IALeaseList[] IA_LEASES = new IALeaseList[GRID_SIZE+1];

	/**
	 * Returns a lease over an int[], which is returned to the pool by close.
	 * This is a way of "borrowing" an existing array, instead of profligate
	 * array creation, which unfortunately is a bit slow in Java, especially
	 * when you include the additional work in the garbage collector. Simply,
	 * less garbage means less garbage collection. Most commonly a right sized
	 * array is completely overwritten every time it's used, so GC needlessly
	 * clears this RAM before making it available again. Leasing arrays avoids
	 * this overhead, just beware that reused arrays are "dirty" (still contain
	 * "previous" values).
	 *
	 * @param size the size of the required int-array
	 * @return a Lease over an int[], which is returned to the pool by close.
	 */
	public static IALease iaLease(final int size) {
		if ( IA_LEASES[size] == null )
			IA_LEASES[size] = new IALeaseList();
		else
			for ( IALease lease : IA_LEASES[size] ) // slow Iterator
				if ( lease.free )
					return lease.take();
		final IALease lease = new IALease(size);
		IA_LEASES[size].add(lease);
		return lease;
	}

	private IntArrays() { } // Never used

}
