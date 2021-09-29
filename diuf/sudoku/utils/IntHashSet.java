/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.SPACE;
import java.util.Arrays;


/**
 * A HashSet of int, based on the java.util.Set interface, but not implementing
 * it, or extending any java.util crap, just simple and clean. Implements only
 * add (which does NOT update existing, ie this Set is "funky"), and clear;
 * which is all I need. I use this set to determine only "is this integer
 * distinct from all of those that have come before it".
 * <p>
 * KRC 2020-11-26 I just tried late-creating the table, in add, but it's nearly
 * two minutes slower: was 08:41 now 10:57 because add is called billions of
 * times, so just if(table==null) takes distant monarch ages.
 * Then I tried an addFirst late-creates table and an addSubsequent, but it was
 * still slower at 09:25, so reverted to create final table in constructor.
 * Then I tried without the insert-sort but it was slower 4+ second first 2.
 * Now try minus insert-sort (again) on final table: is slower.
 * so revert to insert-sort in add-only on a final table: is fastest.
 *
 * @author Keith Corlett 2020-11-13
 */
public class IntHashSet {

	/**
	 * An Entry in the linked-list of each table element.
	 */
	protected static class Entry {
		int value;
		Entry next; // a forward-only linked list
		public Entry(int value, Entry next) {
			this.value = value;
			this.next = next;
		}
		@Override
		public String toString() {
			return ""+value+SPACE+next; // no loops please
		}
	}

	/** The array of forward-only linked-lists of Entry. */
	protected final Entry[] table;

	/** table.length - 1 */
	protected final int mask;

	/**
	 * Construct a new IntHashSet with a table length of capacity.
	 * @param capacity
	 */
	public IntHashSet(int capacity) {
		this.mask = capacity - 1;
		table = new Entry[capacity];
	}

	/**
	 * Return a hash of the given key.
	 * <p>
	 * This method can (I think) be overridden, despite being static.
	 * <p>
	 * PERFORMANCE: This static method is faster than doing it inline, which
	 * is counter-intuitive: I will never understand how that works. And it's
	 * a method to ensure consistency and it's overridable, so win win win.
	 *
	 * @param key the key is already an int (so no key.hashCode() here mate)
	 * @return {@code return hc ^ (hc >>> 12);} to leave the last 4 bits alone
	 */
	protected static int hash(int key) {
		return key ^ (key >>> 12); // leave the last 4 bits alone
	}

	/**
	 * Add i to this Set.
	 * <p>
	 * Note that IntHashSet does NOT grow automatically like a
	 * java.util.HashSet.
	 *
	 * @param i
	 * @return was i added? false means "already exists"
	 */
	public boolean add(int i) {
		final int index = hash(i) & mask;
		Entry e = table[index];
		if ( e == null ) { // no entry in table yet
			table[index] = new Entry(i, null);
			return true;
		} else {
			// seek i in list: stop at first entry thats larger, keeping track
			// of the previous entry (the last entry that's smaller), which we
			// insert after, ie we're doing an insert-sort here. It's O(n) but
			// still faster than a tree, and I do not understand why: suspect
			// the number of multi-element lists is low, and like 95% of'em
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
