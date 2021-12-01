/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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
 * IntIntHashMap is a {@code java.util.HashMap<int, int>} which supports just
 * the operations I actually use: put and get, and clean.
 * <p>
 * IntIntHashMap's only reason for existence is speed!
 *
 * @author Keith Corlett 2020-11-14
 * @author Keith Corlett 2020-11-21 Rewrite: remove interface and pair-down to
 *  bare minimum for what I actually use, now that I know what I actually use.
 */
public class IntIntHashMap {

	public static final int NOT_FOUND = -1;

	protected static class Entry {
		int key;
		int value;
		Entry next;
		public Entry(int key, int value, Entry next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
		@Override
		public String toString() {
			return ""+key+EQUALS+value+SP+next; // No loops!
		}
		@Override
		public boolean equals(Object o) {
			return o instanceof Entry && equals((Entry)o);
		}
		public boolean equals(Entry o) {
			return key==o.key && value==o.value;
		}
		@Override
		public int hashCode() {
			return key ^ value;
		}
	}

	protected final Entry[] table;
	protected final int mask; // table.length - 1;
	// readonly to public or I kill you.
	public int size;

	public IntIntHashMap(int capacity) {
		table = new Entry[capacity];
		mask = capacity - 1;
		size = 0;
	}

	protected static int hash(int key) {
		return key ^ (key >>> 12); // leave the right 4 bits unchanged
	}

	public int get(int key) {
		for ( Entry e=table[hash(key) & mask]; e!=null; e=e.next )
			if ( e.key == key )
				return e.value; // found
		return NOT_FOUND;
	}

	public int put(int key, int value) {
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

//	public void clearCounted() {
//		int cnt = 0;
//		for ( int i=0,n=table.length; i<n; ++i ) {
//			for ( Entry e=table[i]; e!=null; e=e.next )
//				++cnt;
//			table[i] = null;
//		}
//		maxSize = Math.max(maxSize, cnt);
//	}
//	public static int maxSize;

	public int size() {
		return size;
	}

}
