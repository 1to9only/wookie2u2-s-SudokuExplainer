/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Arrays;
import static diuf.sudoku.utils.Frmt.SP;

/**
 * A HashSet of 512 long, based on java.util.Set but not implementing it or
 * extending any java.util crap. Simple and clean. Implements only add (which
 * does NOT update existing, ie this Set is "funky") and clear, which is all I
 * want. I use LongHashSet to determine "is this long distinct from all of
 * those that have come before it".
 * <p>
 * This Set can contain more than 512 entries, but my table (unlike java.util)
 * never grows, the linked lists just keep growing so too many collisions cause
 * LongHashSet to be LESS performant than java.util.HashSet. It is horses for
 * courses. Autoboxing and growth vs neither for speed at low altitudes.
 * <p>
 * Use the debugger to see your built table/s, and adjust hash accordingly.
 * There is no real problem with copy-pasting an implementation per use. Sigh.
 *
 * @author Keith Corlett 2021-11-30 a copy-paste of IntHashSet
 */
public class LongHashSet {

	/**
	 * An Entry in the linked-list of each table element.
	 */
	protected static class Entry {
		long value;
		Entry next; // a forward-only linked list
		public Entry(long value, Entry next) {
			this.value = value;
			this.next = next;
		}
		@Override
		public String toString() {
			return ""+value+SP+next; // no loops please
		}
	}

	/** The array of forward-only linked-lists of Entry. */
	protected final Entry[] table;

	/** table.length - 1 */
	protected final int mask;

	/**
	 * Construct a new LongHashSet with a table length of 512.
	 */
	public LongHashSet() {
		this.mask = 511;
		table = new Entry[512];
	}

	/**
	 * Return an 8 bit hash of the given long key, for hard-coded capacity 512.
	 *
	 * @param key
	 * @return key folded into a byte
	 */
	protected static int hash(final long key) {
		final int i = (int)(key ^ (key>>>32));
		final char c = (char)(i ^ (i >>> 16));
		return c ^ (c >>> 8); // a byte, as an int, with hangover bits.
	}

	/**
	 * Is hashCode in this Set?
	 *
	 * @param hashCode to examine
	 * @return is hashCode in this Set?
	 */
	public boolean has(final long hashCode) {
		final int index = hash(hashCode) & mask;
		Entry e = table[index];
		if ( e == null ) // no entry in table yet
			return false;
		do
			if ( e.value == hashCode )
				return true;
			else if ( e.value > hashCode )
				return false;
		while ( (e=e.next) != null );
		return false;
	}

	/**
	 * Add i to this Set.
	 * <p>
	 * Note that LongHashSet does NOT grow automatically like a
	 * java.util.HashSet.
	 *
	 * @param i
	 * @return was i added? false means "already exists"
	 */
	public boolean add(final long i) {
		final int index = hash(i) & mask;
		Entry e = table[index];
		if ( e == null ) { // no entry in table yet
			table[index] = new Entry(i, null);
			return true;
		} else {
			// seek i in list: stop at first entry thats larger, keeping track
			// of the previous entry (the last entry that is smaller), which we
			// insert after, ie we are doing an insert-sort here. It is O(n) but
			// still faster than a tree, and I do not understand why: suspect
			// the number of multi-element lists is low, and like 95% of them
			// have "few" entries (less than 6) so the extra complexity of a
			// tree ends up costing us time.
			Entry p = e; // previous
			do {
				if ( e.value == i )
					return false; // do not update the existing entry
				else if ( e.value > i )
					break;
				p = e;
				e = e.next;
			} while ( e != null );
			if ( p == table[index] )
				// insert i before the first entry in the linked-list
				table[index] = new Entry(i, p);
			else
				// insert i after previous (the last entry thats smaller)
				p.next = new Entry(i, e);
			return true;
		}
	}

	public void clear() {
		Arrays.fill(table, null);
	}

}
