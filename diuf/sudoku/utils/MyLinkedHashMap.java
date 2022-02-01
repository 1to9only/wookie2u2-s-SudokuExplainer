/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/*
 * The original java.util.LinkedHashMap pro-forma licence statement:
 *
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Hashtable; // for documentation (inherited) only
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;


/**
 * KRC: Hacked out of the 1.7 open-source code-base (a couple of years ago)
 * in order to extend a HashMap into a MyFunkyLinkedHashMap, which doesn't
 * replace existing values when isFunky is on (as it is by default). I notice
 * that 1.8 has this feature, but I can't figure out how to turn it on. Sigh.
 *
 * <p>This class differs from java.util,LinkedHashMap only in that it wraps
 * diuf.sudoku.utils.MyHashMap, which has been hacked (as little as possible).
 *
 * <p><b>HACKS</b>:<ul>
 * <li>addEntry commented out optionally removing the oldest entry. I never use
 * this feature and it slows me down.
 * <li>clear() is also hacked for a tiny efficiency gain. O(n) not O(capacity).
 * <li>added the poll() for use by MyHashSet.poll(). NB: this method returns the
 * first KEY, not the value associated with it, as one might reasonably expect.
 * This is because MyHashSet's values are all 1 object, to save on RAM and GC.
 * </ul>
 *
 * <p>Hash table and linked list implementation of the <tt>Map</tt> interface,
 * with predictable iteration order.  This implementation differs from
 * <tt>MyHashMap</tt> in that it maintains a doubly-linked list of its entries,
 * which defines the iteration order as the order in which keys were inserted
 * into the map (<i>insertion-order</i>). Insertion order is unaffected if a
 * key is <i>re-inserted</i> into the map. A key <tt>k</tt> is said to be
 * "reinserted" into a map <tt>m</tt> if <tt>m.put(k, v)</tt> is invoked when
 * <tt>m.containsKey(k)</tt> would return <tt>true</tt> immediately prior to
 * the invocation.
 *
 * <p>This implementation spares its clients from the unspecified, generally
 * chaotic ordering provided by {@link MyHashMap} (and {@link Hashtable}),
 * without incurring the increased cost associated with {@link TreeMap}.  It
 * can be used to produce a copy of a map that has the same order as the
 * original, regardless of the original map's implementation:
 * <pre>
 *     void foo(Map m) {
 *         Map copy = new MyLinkedHashMap(m);
 *         ...
 *     }
 * </pre>
 * This technique is particularly useful if a module takes a map on input,
 * copies it, and later returns results whose order is determined by that of
 * the copy.  (Clients generally appreciate having things returned in the same
 * order they were presented.)
 *
 * <p>A special {@link #MyLinkedHashMap(int,float,boolean) constructor} is
 * provided to create a linked hash map whose order of iteration is the order
 * in which its entries were last accessed, from least-recently accessed to
 * most-recently (<i>access-order</i>).  This kind of map is well-suited to
 * building LRU caches.  Invoking the <tt>put</tt> or <tt>get</tt> method
 * results in an access to the corresponding entry (assuming it exists after
 * the invocation completes).  The <tt>putAll</tt> method generates one entry
 * access for each mapping in the specified map, in the order that key-value
 * mappings are provided by the specified map's entry set iterator.  <i>No
 * other methods generate entry accesses.</i> In particular, operations on
 * collection-views do <i>not</i> affect the order of iteration of the backing
 * map.
 *
 * <p>The {@link #removeEldestEntry(Map.Entry)} method may be overridden to
 * impose a policy for removing stale mappings automatically when new mappings
 * are added to the map.
 *
 * <p>This class provides all of the optional <tt>Map</tt> operations, and
 * permits null elements.  Like <tt>MyHashMap</tt>, it provides constant-time
 * performance for the basic operations (<tt>add</tt>, <tt>contains</tt> and
 * <tt>remove</tt>), assuming the hash function disperses elements
 * properly among the buckets.  Performance is likely to be just slightly
 * below that of <tt>MyHashMap</tt>, due to the added expense of maintaining the
 * linked list, with one exception: Iteration over the collection-views
 * of a <tt>MyLinkedHashMap</tt> requires time proportional to the <i>size</i>
 * of the map, regardless of its capacity.  Iteration over a <tt>MyHashMap</tt>
 * is likely to be more expensive, requiring time proportional to its
 * <i>capacity</i>.
 *
 * <p>A linked hash map has two parameters that affect its performance:
 * <i>initial capacity</i> and <i>load factor</i>.  They are defined precisely
 * as for <tt>MyHashMap</tt>.  Note, however, that the penalty for choosing an
 * excessively high value for initial capacity is less severe for this class
 * than for <tt>MyHashMap</tt>, as iteration times for this class are unaffected
 * by capacity.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked hash map concurrently, and at least
 * one of the threads modifies the map structurally, it <em>must</em> be
 * synchronized externally.  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new MyLinkedHashMap(...));</pre>
 *
 * A structural modification is any operation that adds or deletes one or more
 * mappings or, in the case of access-ordered linked hash maps, affects
 * iteration order.  In insertion-ordered linked hash maps, merely changing
 * the value associated with a key that is already contained in the map is not
 * a structural modification.  <strong>In access-ordered linked hash maps,
 * merely querying the map with <tt>get</tt> is a structural
 * modification.</strong>)
 *
 * <p>The iterators returned by the <tt>iterator</tt> method of the collections
 * returned by all of this class's collection view methods are
 * <em>fail-fast</em>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     MyHashMap
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.4
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MyLinkedHashMap<K,V>
	extends MyHashMap<K,V>
	implements Map<K,V>
{
	private static final long serialVersionUID = 3801124242820219131L;

	/**
	 * The head of the doubly linked list, in "natural order", meaning this
	 * linked list iterates in the same order that entries where added.
	 * (NB: Natural order is lost in an unlinked HashMap, it's pseudo-random.)
	 */
	protected transient AnEntry<K,V> header;

	/**
	 * Constructs an empty insertion-ordered <tt>MyLinkedHashMap</tt> instance
	 * with the specified initial capacity and load factor.
	 *
	 * @param  initialCapacity the initial capacity
	 * @param  loadFactor      the load factor
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *         or the load factor is nonpositive
	 */
	public MyLinkedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Constructs an empty insertion-ordered <tt>MyLinkedHashMap</tt> instance
	 * with the specified initial capacity and a default load factor (0.75).
	 *
	 * @param  initialCapacity the initial capacity
	 * @throws IllegalArgumentException if the initial capacity is negative
	 */
	public MyLinkedHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs an empty insertion-ordered <tt>MyLinkedHashMap</tt> instance
	 * with the default initial capacity (16) and load factor (0.75).
	 */
	public MyLinkedHashMap() {
		super();
	}

	/**
	 * Constructs an insertion-ordered <tt>MyLinkedHashMap</tt> instance with
	 * the same mappings as the specified map.  The <tt>MyLinkedHashMap</tt>
	 * instance is created with a default load factor (0.75) and an initial
	 * capacity sufficient to hold the mappings in the specified map.
	 *
	 * @param  m the map whose mappings are to be placed in this map
	 * @throws NullPointerException if the specified map is null
	 */
	public MyLinkedHashMap(Map<? extends K, ? extends V> m) {
		super(m);
	}

	/**
	 * Called by superclass constructors and pseudoconstructors (clone,
	 * readObject) before any entries are inserted into the map.  Initializes
	 * the linked-list.
	 */
	@Override
	void initialise() {
		header = new AnEntry<>(-1, null, null, null);
		header.before = header.after = header;
	}

	/**
	 * Called by MyHashSet.poll to remove and return the first KEY (not the
	 * value associated with it, as you might reasonably expect) in this map.
	 * "First" is the first entry after the head of this map, ie the first
	 * entry that was added to this map, ie "natural order".
	 *
	 * @return remove and return the first KEY in this map; or <tt>null</tt>
	 * meaning that "this map is empty".
	 */
	public MyHashMap.Entry<K,V> poll() {
		K key = header.after.key;
		if(key==null) return null;
		return removeEntryForNotNullKey(key);
	}

	/**
	 * Transfers all entries to new table array.  This method is called
	 * by superclass resize.  It is overridden for performance, as it is
	 * faster to iterate using our linked list.
	 */
	@Override
	MyHashMap.Entry[] transfer(MyHashMap.Entry[] newTable) {
		// recall length is a power of 2, so length-1 is 1111111, or whatever.
		final int capacityMask = newTable.length - 1;
		int i;
		for ( AnEntry<K,V> e=header.after; e!=header; e=e.after ) {
			i = e.hash & capacityMask;
			e.next = newTable[i];
			newTable[i] = e;
		}
		return newTable;
	}


	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the
	 * specified value.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map maps one or more keys to the
	 *         specified value
	 */
	@Override
	public boolean containsValue(Object value) {
		// Overridden to take advantage of faster iterator
		if (value==null) {
			for (AnEntry e = header.after; e != header; e = e.after)
				if (e.value==null)
					return true;
		} else {
			for (AnEntry e = header.after; e != header; e = e.after)
				if (value.equals(e.value))
					return true;
		}
		return false;
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise
	 * it returns {@code null}.  (There can be at most one such mapping.)
	 *
	 * <p>A return value of {@code null} does not <i>necessarily</i>
	 * indicate that the map contains no mapping for the key; it's also
	 * possible that the map explicitly maps the key to {@code null}.
	 * The {@link #containsKey containsKey} operation may be used to
	 * distinguish these two cases.
	 */
	@Override
	public V get(Object key) {
		AnEntry<K,V> e = (AnEntry<K,V>)getEntry(key);
		return e==null ? null : e.value;
	}

	/**
	 * KRC: Returns the first key in this Map.
	 * <p>
	 * nb: I was caching this (hence the method), but it went bad as soon as
	 * I stepped into the wild, so then I tried using my linked-list but it
	 * doesn't appear to be maintained any longer (WTF!), so I gave-up and
	 * just went back to how this was done in the past, except now atleast
	 * it's only done badly/slowly ONCE. Sigh.
	 * <p>
	 * NOTE: now used in the wild, as well as by test-cases, which is what
	 * I was written for, but I turned out to be pretty handy elsewhere as
	 * well, which is always nice.
	 *
	 * @return the first key in this map.
	 */
	public K firstKey() {
		return keySet().iterator().next();
	}

	/**
	 * KRC: Returns the value of the firstCell() in this Map.
	 * <p>
	 * NOTE: currently only used by test-cases, which rely on the value of the
	 * first key, not just the first value (Putz!) which was always the case
	 * in my tests only because they all have only one elimination. Sigh.
	 *
	 * @return the first value in this map.
	 */
	public V firstValue() {
		return get(firstKey());
	}

	/**
	 * Removes all of the mappings from this map.
	 * The map will be empty after this call returns.
	 */
	@Override
	public void clear() {
		if ( size == 0 )
			return;
		++modCount; // invalidate existing iterators
		if ( size < 10 ) { // this breaks even at about a dozen
			for ( K key=header.after.key; key!=null; key=header.after.key )
				removeEntryForNotNullKey(key);
		} else {
			final MyHashMap.Entry<K,V>[] t = table;
			final int n = t.length;
			for ( int i=0; i<n; ++i )
				t[i] = null;
		}
		header = new AnEntry<>(-1, null, null, null);
		header.before = header.after = header;
		size = 0;
	}

	/**
	 * MyLinkedHashMap entry.
	 */
	protected static final class AnEntry<K,V> extends MyHashMap.Entry<K,V> {
		// These fields comprise the doubly linked list used for iteration.
		AnEntry<K,V> before, after;
		AnEntry(int hash, K key, V value, MyHashMap.Entry<K,V> next) {
			super(hash, key, value, next);
		}
		/** Inserts this entry into the list before the given existingEntry. */
		private void addBefore(AnEntry<K,V> existingEntry) {
			after  = existingEntry;
			before = existingEntry.before;
			before.after = this;
			after.before = this;
		}
		/** Removes this entry from the linked list. */
		@Override
		void recordRemoval(MyHashMap<K,V> m) {
			// the entry before me's next entry := my next entry
			before.after = after;
			// the entry after me's previous entry := my previous entry
			after.before = before;
			// so I am snipped out of the doubly-linked-list.
		}
	}

	private abstract class LinkedHashIterator<T> implements Iterator<T> {
		AnEntry<K,V> nextEntry    = header.after;
		AnEntry<K,V> lastReturned = null;

		/**
		 * The modCount value that the iterator believes that the backing
		 * List should have.  If this expectation is violated, the iterator
		 * has detected concurrent modification.
		 */
		int expectedModCount = modCount;

		@Override
		public boolean hasNext() {
			return nextEntry != header;
		}

		@Override
		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();

			MyLinkedHashMap.this.remove(lastReturned.key);
			lastReturned = null;
			expectedModCount = modCount;
		}

		AnEntry<K,V> nextEntry() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextEntry == header)
				throw new NoSuchElementException();

			AnEntry<K,V> e = lastReturned = nextEntry;
			nextEntry = e.after;
			return e;
		}
	}

	private final class KeyIterator extends LinkedHashIterator<K> {
		@Override
		public K next() { return nextEntry().key; }
	}

	private final class ValueIterator extends LinkedHashIterator<V> {
		@Override
		public V next() { return nextEntry().value; }
	}

	private final class EntryIterator
			extends LinkedHashIterator<Map.Entry<K,V>> {
		@Override
		public Map.Entry<K,V> next() { return nextEntry(); }
	}

	// These Overrides alter the behavior of superclass view iterator() methods
	@Override
	Iterator<K> newKeyIterator() { return new KeyIterator(); }
	@Override
	Iterator<V> newValueIterator() { return new ValueIterator(); }
	@Override
	Iterator<Map.Entry<K,V>> newEntryIterator() { return new EntryIterator(); }

	/**
	 * This override alters behavior of superclass put method. It causes newly
	 * allocated entry to get inserted at the end of the linked list and
	 * removes the eldest entry if appropriate.
	 */
	@Override
	void addEntry(int hash, K key, V value, int bucketIndex) {
		createEntry(hash, key, value, bucketIndex);
//KRC: commented out because I never use this feature and it's slowing me down.
//		// Remove eldest entry if instructed, else grow capacity if appropriate
//		AnEntry<K,V> eldest = header.after;
//		if (removeEldestEntry(eldest)) {
//			removeEntryForKey(eldest.key);
//		} else {
			if ( size > threshold ) // was >= which upsizes all my 8s to 16s
				resize(2 * table.length);
//		}
	}

	/**
	 * This override differs from addEntry in that it doesn't resize the
	 * table or remove the eldest entry.
	 */
	@Override
	void createEntry(int hash, K key, V value, int bucketIndex) {
		MyHashMap.Entry<K,V> old = table[bucketIndex];
		AnEntry<K,V> e = new AnEntry<>(hash, key, value, old);
		table[bucketIndex] = e;
		e.addBefore(header);
		++size;
	}

	/**
	 * Returns <tt>true</tt> if this map should remove its eldest entry.
	 * This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
	 * inserting a new entry into the map.  It provides the implementor
	 * with the opportunity to remove the eldest entry each time a new one
	 * is added.  This is useful if the map represents a cache: it allows
	 * the map to reduce memory consumption by deleting stale entries.
	 *
	 * <p>Sample use: this override will allow the map to grow up to 100
	 * entries and then delete the eldest entry each time a new entry is
	 * added, maintaining a steady state of 100 entries.
	 * <pre>
	 *     private static final int MAX_ENTRIES = 100;
	 *
	 *     protected boolean removeEldestEntry(Map.Entry eldest) {
	 *        return size() > MAX_ENTRIES;
	 *     }
	 * </pre>
	 *
	 * <p>This method typically does not modify the map in any way,
	 * instead allowing the map to modify itself as directed by its
	 * return value.  It <i>is</i> permitted for this method to modify
	 * the map directly, but if it does so, it <i>must</i> return
	 * <tt>false</tt> (indicating that the map should not attempt any
	 * further modification).  The effects of returning <tt>true</tt>
	 * after modifying the map from within this method are unspecified.
	 *
	 * <p>This implementation merely returns <tt>false</tt> (so that this
	 * map acts like a normal map - the eldest element is never removed).
	 *
	 * @param    eldest The least recently inserted entry in the map, or if
	 *           this is an access-ordered map, the least recently accessed
	 *           entry.  This is the entry that will be removed it this
	 *           method returns <tt>true</tt>.  If the map was empty prior
	 *           to the <tt>put</tt> or <tt>putAll</tt> invocation resulting
	 *           in this invocation, this will be the entry that was just
	 *           inserted; in other words, if the map contains a single
	 *           entry, the eldest entry is also the newest.
	 * @return   <tt>true</tt> if the eldest entry should be removed
	 *           from the map; <tt>false</tt> if it should be retained.
	 */
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		return false;
	}

}
