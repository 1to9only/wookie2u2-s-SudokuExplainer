/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 *
 * Sudoku Explainer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Sudoku Explainer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sudoku Explainer. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.EQUALS;
import static diuf.sudoku.utils.Frmt.SP;

/**
 * LongLongHashMap exists specifically to NOT wrap long primitives, for speed!
 * <p>
 * LongLongHashMap is a {@code java.util.HashMap<long, long>} which supports
 * just the operations I actually use: put and get, and clean.
 *
 * @author Keith Corlett 2020-12-07 created based on IntIntHashMap
 */
public class LongLongHashMap {

	public static final int NOT_FOUND = -1;

	protected static class Entry {
		final long key;
		long value;
		Entry next;
		public Entry(final long key, final long value, final Entry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
		@Override
		public String toString() {
			return ""+key+EQUALS+value+SP+next; // No loops!
		}
		@Override
		public boolean equals(final Object o) {
			return o instanceof Entry && equals((Entry)o);
		}
		public boolean equals(final Entry o) {
			return key==o.key && value==o.value;
		}
		@Override
		public int hashCode() {
			return (int) ((key ^ (key >>> 32)) ^ (value ^ (value >>> 32)));
		}
	}

	/** The array of Entry to store keys and there values. */
	protected final Entry[] table;

	/** table.length - 1. */
	protected final int mask; // table.length - 1;

	/** readonly to public or I kill you. */
	public int size;

	/**
	 * Construct a new LongLongHashMap to hold upto capacity entries.
	 * Unlike java.util.HashMap this data-structure does NOT grow, so
	 * getting the capacity right really matters! To do that, run it,
	 * and max the size() before you clear the map in cleanUp after
	 * each puzzle. That tells you ABOUT how big to make it: choose a
	 * power-of-2 around there, it may be a bit more or less; so long
	 * as it is in the vacinity you will be OK.
	 * Then test it for speed; I was surprised to find that under-sized
	 * maps are faster overall. Too many fat-bastards like me I guess.
	 *
	 * @param capacity is always a power of 2 so 16, 32, 64, 128, 256, 512,
	 *  1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288
	 *  beyond that you are on your own; and if it's under 16 just use array
	 */
	public LongLongHashMap(final int capacity) {
		table = new Entry[capacity];
		mask = capacity - 1;
		size = 0;
	}

	// convert a long key into an int, for the table index
	protected static int hash(final long key) {
		return (int)(key ^ (key >> 32));
	}

	public long get(final long key) {
		for ( Entry e=table[(hash(key)) & mask]; e!=null; e=e.next )
			if ( e.key == key )
				return e.value; // found
		return NOT_FOUND;
	}

	public long put(final long key, final long value) {
		// if you add NOT_FOUND as a key bad things happen; but what you can do
		// is change IntIntMap.NOT_FOUND to Integer.MIN_VALUE, or whatever.
		assert key != NOT_FOUND;
		// if you add NOT_FOUND as a value it is impossible to differentiate
		// it when it is returned as the previous value (for instance), so it
		// is best if you do not add NOT_FOUND as a value either. sigh.
		assert value != NOT_FOUND;
		// get just the index of table not table[index], yet.
		final int index = hash(key) & mask;
		// deleting "exists search" is slower, and incorrect, so keep it.
		for ( Entry e=table[index]; e!=null; e=e.next )
			if ( e.key == key ) {
				e.value = value;
				return 0; // just return !NOT_FOUND, coz its always ignored!
			}
		// insert the new entry at the head of linked-list -> most recent entry
		// first -> both put and get are faster than an old insertion-sort impl
		// because its doing LESS comparisons.
		// note that (pre) table[index] may be null, which is OK.
		table[index] = new Entry(key, value, table[index]);
		++size;
		return NOT_FOUND; // meaning success
	}

	public void clear() {
		for ( int i=0,n=table.length; i<n; ++i )
			table[i] = null;
		size = 0;
	}

	public int size() {
		return size;
	}

}
