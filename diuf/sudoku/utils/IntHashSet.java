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
 * A HashSet of int, based on the java.util.Set interface, but not implementing
 * it, or extending any java.util crap, just simple and clean. Implements only
 * add (which does NOT update existing, ie this Set is "funky"), and the clear
 * method; which is all I need. I use IntHashSet to determine "is this integer
 * distinct from all of those that have come before it".
 * <p>
 * NB: IntHashSet does NOT grow automatically like java.util.HashSet.
 * <p>
 * KRC 2020-11-26 I just tried late-creating the table, in add, but it is nearly
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

//	// DEBUG
//	public void dump(final String label) {
//		System.out.println();
//		System.out.println(label);
//		for ( int i=0,n=table.length; i<n; ++i )
//			System.out.println(""+i+": "+table[i]);
//	}

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
			return ""+value+SP+next; // no loops please
		}
	}

	/** The array of forward-only linked-lists of Entry. */
	protected final Entry[] table;

	/** table.length - 1 */
	protected final int mask;

	public static boolean isPowerOf2(int x) {
		// a 0-fixed-size IntHashSet is nonsensical, as is 2, 4, and 8. Sigh.
		// I stop at 1<<31, which is the max java thinks is reasonable.
		// That is 2,147,483,648 so I see there point. 2 BILLION elements in a
		// table would require more RAM than you find in many PC's. I bet the
		// cosmologists bloody hate it though. FORTRAN! OOTRAN? groovtran?
		for ( int i=1; i<32; ++i )
			if ( x == 1<<i )
				return true;
		return false;
	}

	/**
	 * Construct a new IntHashSet with a table length of capacity.
	 * <p>
	 * NB: IntHashSet does NOT grow automatically like java.util.HashSet so the
	 * capacity you give me to start with is all I have. If that is too small
	 * then you will see many lists (using dump) longer than about 9.
	 *
	 * @param capacity must be a power of 2.
	 */
	public IntHashSet(int capacity) {
		if ( !isPowerOf2(capacity) )
			throw new IllegalArgumentException("capacity must be a power of 2");
		this.mask = capacity - 1;
		table = new Entry[capacity];
	}

	/**
	 * Return a hash of the given key.
	 * <p>
	 * This method can (I think) be overridden, despite being static.
	 * <p>
	 * PERFORMANCE: This static method is faster than doing it inline, which
	 * is counter-intuitive: I will never understand how that works. And it is
	 * a method to ensure consistency and it is overridable, so win win win.
	 *
	 * @param key the key is already an int (so no key.hashCode() here mate)
	 * @return {@code return hc ^ (hc >>> 12);} to leave the last 4 bits alone
	 */
	protected static int hash(final int key) {
		return key ^ (key >>> 12); // leave the last 4 bits alone
	}

	/**
	 * Add i to this Set. This add is "funky" by which I mean that it is an
	 * addOnly in java.util.Collections parlance. If you attempt to add an
	 * element that is already in the Set then add JUST returns false. This
	 * is in contrast to the add methods in java.util.Collections, which
	 * replace the existing element with the given value and return false
	 * in the same circumstance, which is WRONG in my humble opinion. If I
	 * wanted an update method then I would implement an update method. Add
	 * means add. Just add. Plain and simple.
	 * <p>
	 * NB: IntHashSet does NOT grow automatically like java.util.HashSet.
	 *
	 * @param i
	 * @return was i added? false means "already exists"
	 */
	public boolean add(final int i) {
		final int index = hash(i) & mask;
		final Entry[] t = table;
		Entry e = t[index];
		if ( e == null ) { // no entry in table yet
			t[index] = new Entry(i, null);
			return true;
		} else {
			// seek i in list: stop at first entry thats larger, keeping track
			// of the previous entry (the last entry that is smaller), which we
			// insert after, ie we are doing an insert-sort here. It is O(n) but
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
			if ( p == t[index] )
				// insert i before the first entry in the linked-list
				t[index] = new Entry(i, p);
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
