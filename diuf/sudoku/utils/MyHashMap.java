/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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

import diuf.sudoku.tools.AMyMap;
import static diuf.sudoku.utils.Frmt.EQUALS;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

/**
 * MyHashMap is hashtable based implementation of the <tt>MyMap</tt> interface,
 * which extends java.util.Map to add the "visit" method. I also eradicated
 * ternaries, coz they are slower, so they should be avoided in low-level code,
 * such as collections.
 * <p>
 * This implementation is not intended for "general use". If what you need is
 * a standard HashMap then you should use the standard java.util.HashMap. Note
 * that this implementation started as a copy of Java 1.7's java.util.HashMap.
 * I am now using Java 1.8 whose HashMap is faster than this.
 * <p>
 * This implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key. The <tt>HashMap</tt> class
 * is roughly equivalent to <tt>Hashtable</tt>, except that it is unsynchronised
 * and permits nulls. This class does not guarantee the order of the map; in
 * particular, it does not guarantee that the order remains constant over time.
 * <p>
 * This implementation provides constant-time performance for the basic ops
 * (<tt>get</tt> and <tt>put</tt>), assuming the hash function disperses the
 * elements properly among the buckets. Iteration over collection views takes
 * time proportional to the "capacity" of the <tt>HashMap</tt> instance (the
 * number of buckets) plus its size (the number of key-value mappings). Thus,
 * it is very important not to set the initial capacity too high (or the load
 * factor too low) if iteration performance is important.
 * <p>
 * An instance of <tt>HashMap</tt> has two parameters affecting performance:
 * <i>initial capacity</i> and <i>load factor</i>. The <i>capacity</i> is the
 * number of buckets in the hash table, and the initial capacity is simply the
 * capacity at the time the hash table is created. The <i>load factor</i> is a
 * measure of how full the hash table is allowed to get before its capacity is
 * automatically increased. When the number of entries in the hashtable exceeds
 * the product of the load factor and the current capacity, the hashtable is
 * <i>rehashed</i> (that is, internal data structures are rebuilt) so that the
 * hashtable has approximately twice the number of buckets.
 * <p>
 * As a general rule, the default load factor (.75) yields a good compromise
 * between space and time. Higher values decrease the space overhead but
 * increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>).  The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the number
 * of rehash operations. If the initial capacity is greater than the maximum
 * number of entries divided by the load factor, no rehash operations will ever
 * occur.
 * <p>
 * If many mappings are to be stored in a <tt>HashMap</tt> instance, creating
 * it with a sufficiently large capacity will allow the mappings to be stored
 * more efficiently than letting it perform automatic rehashing as needed to
 * grow the table.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of the
 * threads modifies the map structurally, it <i>must</i> be synchronized
 * externally. A structural modification is any operation that adds or deletes
 * one or more mappings; merely changing the value associated with a key that
 * an instance already contains is not a structural modification. This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.
 * <p>
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 * <p>
 * The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 * <p>
 * This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MyHashMap<K,V> extends AMyMap<K,V> implements Cloneable, Serializable {

	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	static final int DEFAULT_CAPACITY = 8;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load factor used when none specified in constructor.
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75F;

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	transient Entry<K,V>[] table;
	transient int mask; // table.length - 1;

	/**
	 * The number of key-value mappings contained in this map.
	 */
	transient int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 * @serial
	 */
	int threshold;

	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	final float loadFactor;

	/**
	 * The number of times this HashMap has been structurally modified
	 * Structural modifications are those that change the number of mappings in
	 * the HashMap or otherwise modify its internal structure (eg,
	 * rehash).  This field is used to make iterators on Collection-views of
	 * the HashMap fail-fast.  (See ConcurrentModificationException).
	 */
	transient int modCount;

	/**
	 * The value stored against the null key. Remains null until a null key
	 * is added to this map, when it is set.
	 */
	transient V nullKeyValue;

	/**
	 * A lambda that when not null is invoked when the table has been resized,
	 * passing {@code int newCapacity}, to enable you to monitor the capacity
	 * of each MyHashMap.
	 */
	public IntVisitor resizeMonitor;

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial
	 * capacity and load factor.
	 *
	 * @param  initialCapacity the desired initial capacity, which is ignored
	 *  if its too small (&lt; DEFAULT_CAPACITY=8),
	 *  or too large (&gt; MAXIMUM_CAPACITY 1&lt;&lt;30)
	 * @param  loadFactor the load factor
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *         or the load factor is nonpositive
	 */
	public MyHashMap(int initialCapacity, float loadFactor) {
		// validate params
//		if (initialCapacity < 0)
//			throw new IllegalArgumentException("Negative initial capacity: "+initialCapacity);
		if (initialCapacity < DEFAULT_CAPACITY)
			initialCapacity = DEFAULT_CAPACITY;
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor < 0.5f || Float.isNaN(loadFactor))
//			throw new IllegalArgumentException("Illegal load factor: "+loadFactor);
			loadFactor = DEFAULT_LOAD_FACTOR;
		// find lowest power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;
		// create and initialise
		this.loadFactor = loadFactor;
		threshold = (int)(capacity * loadFactor);
		table = new Entry[capacity];
		mask = table.length - 1;
		initialise();
	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param  initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public MyHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the default initial capacity
	 * (16) and the default load factor (0.75).
	 */
	public MyHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = (int)(DEFAULT_CAPACITY * DEFAULT_LOAD_FACTOR);
		table = new Entry[DEFAULT_CAPACITY];
		mask = table.length - 1;
		initialise();
	}

	/**
	 * Constructs a new <tt>HashMap</tt> with the same mappings as the
	 * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
	 * default load factor (0.75) and an initial capacity sufficient to
	 * hold the mappings in the specified <tt>Map</tt>.
	 *
	 * @param   m the map whose mappings are to be placed in this map
	 * @throws  NullPointerException if the specified map is null
	 */
	public MyHashMap(Map<? extends K, ? extends V> m) {
		this(Math.max((int)(m.size()/DEFAULT_LOAD_FACTOR)+1, DEFAULT_CAPACITY)
				, DEFAULT_LOAD_FACTOR);
		putAllForCreate(m);
	}

	// internal utilities

	/**
	 * Initialization hook for subclasses. This method is called
	 * in all constructors and pseudo-constructors (clone, readObject)
	 * after HashMap has been initialized but before any entries have
	 * been inserted.  (In the absence of this method, readObject would
	 * require explicit knowledge of subclasses.)
	 */
	void initialise() {
	}

	public int getCapacity() {
		return table.length;
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which
	 * defends against poor quality hash functions.  This is critical
	 * because HashMap uses power-of-two length hash tables, that
	 * otherwise encounter collisions for hashCodes that do not differ
	 * in lower bits. Note: Null keys always map to hash 0, thus index 0.
	 */
	protected final int hash(int h) {
		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	/**
	 * Returns index for hash code h.
	 */
	protected final int indexFor(int h, int length) {
		return h & (length-1);
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	@Override
	public boolean any() {
		return size > 0;
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
	 * indicate that the map contains no mapping for the key; it is also
	 * possible that the map explicitly maps the key to {@code null}.
	 * The {@link #containsKey containsKey} operation may be used to
	 * distinguish these two cases.
	 *
	 * @see #put(Object, Object)
	 */
	@Override
	public V get(Object key) {
		if ( key == null )
			return getForNullKey();
		int hash = hash(key.hashCode());
		K k;
		for ( Entry<K,V> e=table[hash & mask]; e!=null; e=e.next )
			if ( e.hash==hash && ((k=e.key)==key || key.equals(k)) )
				return e.value;
		return null;
	}

	/**
	 * Offloaded version of get() to look up null keys.  Null keys map
	 * to index 0.  This null case is split out into separate methods
	 * for the sake of performance in the two most commonly used
	 * operations (get and put), but incorporated with conditionals in
	 * others.
	 */
	private V getForNullKey() {
//		for ( Entry<K,V> e=table[0]; e!=null; e=e.next )
//			if ( e.key == null )
//				return e.value;
		return nullKeyValue;
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the
	 * specified key.
	 *
	 * @param   key   The key whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map contains a mapping for the specified
	 * key.
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( key == null ) {
			return getForNullKey() != null;
		}
		return getEntry(key) != null;
	}

	/**
	 * Returns the entry associated with the specified key in the
	 * HashMap.  Returns null if the HashMap contains no mapping
	 * for the key.
	 */
	final Entry<K,V> getEntry(Object key) {
		if ( key == null )
			return new Entry(0, null, getForNullKey(), null);
		final int hash = hash(key.hashCode());
		K k;
		for ( Entry<K,V> e=table[hash & mask]; e!=null; e=e.next )
			if ( e.hash==hash && ( (k=e.key)==key || key.equals(k) ) )
				return e;
		return null;
	}

	final Entry<K,V> getEntry(int hashCode) {
		if ( hashCode == 0 ) {
			for ( Entry<K,V> e=table[0]; e!=null; e=e.next )
				if ( e.key==null )
					return e;
			return null;
		}
		final int hash = hash(hashCode);
		for ( Entry<K,V> e=table[hash & mask]; e!=null; e=e.next )
			if ( e.hash==hash && e.key.hashCode()==hashCode )
				return e;
		return null;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map contains an existing entry for this key, then the old
	 * value is replaced.
	 * <p>
	 * NOTE WELL: if you require null keys or values use a java.util.HashMap!
	 *
	 * @param key the key with which the specified value is to be associated,
	 *  which may be null, but I cannot guarantee ANY results from a MyHashMap
	 *  that contains a null key or null values.
	 * @param value value to be associated with the specified key, which may be
	 *  null, but I cannot guarantee ANY results from a MyHashMap that contains
	 *  a null key or null values.
	 * @return the previous value associated with <tt>key</tt> or <tt>null</tt>
	 *  if there was no mapping for <tt>key</tt>.
	 *
	 */
	@Override
	public V put(K key, V value) {
		// first handle null key
		if ( key == null ) {
			++modCount;
			return putForNullKey(value);
		}
		K k;
		V previousValue;
		final int i;
		int h = key.hashCode();
		               h ^= (h >>> 20) ^ (h >>> 12);
		final int hash = h ^ (h >>> 7) ^ (h >>> 4);
		for ( Entry<K,V> e=table[i=hash & mask]; e!=null; e=e.next )
			if ( e.hash==hash && ( (k=e.key)==key || key.equals(k) ) ) {
				previousValue = e.value;
				e.value = value;
				return previousValue;
			}
		++modCount;
		addEntry(hash, key, value, i);
		return null;
	}

	/**
	 * Add the given $key with the given $value. By calling me you are taking
	 * responsibility for assuring that key does not already exist. Note that
	 * $key is not null: I hate null in HashMap. Use another data-structure.
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean addOnly(K key, V value) {
		// first handle null key
		if ( key == null ) {
			++modCount;
			return putForNullKey(value) == null;
		}
		K k;
		final int hash, i;
		{ // this block just localises h
			int h = key.hashCode();
			h ^= (h >>> 20) ^ (h >>> 12);
			hash = h ^ (h >>> 7) ^ (h >>> 4);
		}
		for ( Entry<K,V> e=table[i=hash & mask]; e!=null; e=e.next )
			if ( e.hash==hash && ( (k=e.key)==key || key.equals(k) ) )
				return false;
		++modCount;
		addEntry(hash, key, value, i);
		return true;
	}

	public V update(K key, V value) {
		if ( key == null )
			return putForNullKey(value); // does not change modcount!
		K k;
		V previousValue;
		final int hash, i;
		{ // this block just localises h
			int h = key.hashCode();
			h ^= (h >>> 20) ^ (h >>> 12);
			hash = h ^ (h >>> 7) ^ (h >>> 4);
			i = hash & mask;
		}
		for ( Entry<K,V> e=table[i]; e!=null; e=e.next )
			if ( e.hash==hash && ( (k=e.key)==key || key.equals(k) ) ) {
				previousValue = e.value;
				e.value = value;
				return previousValue;
			}
		return null;
	}

	/**
	 * This method handles finding the existing entry with a null key, and
	 * if one exists it is value is returned, else the proffered value is added
	 * to the null key.
	 *
	 * @param value
	 * @return
	 */
	private V putForNullKey(V value) {
		if ( nullKeyValue != null )
			return nullKeyValue;
		nullKeyValue = value;
		++size;
		return null;
	}

	/**
	 * This method is used instead of put by constructors and
	 * pseudoconstructors (clone, readObject).  It does not resize the table,
	 * check for comodification, etc.  It calls createEntry rather than
	 * addEntry.
	 */
	private void putForCreate(K key, V value) {
		int hash;
		if (key == null)
			hash = 0;
		else
			hash = hash(key.hashCode());
		int i = indexFor(hash, table.length);

		/**
		 * Look for preexisting entry for key.  This will never happen for
		 * clone or deserialize.  It will only happen for construction if the
		 * input Map is a sorted map whose ordering is inconsistent w/ equals.
		 */
		for (Entry<K,V> e = table[i]; e != null; e = e.next) {
			Object k;
			if (e.hash == hash &&
				((k = e.key) == key || (key != null && key.equals(k)))) {
				e.value = value;
				return;
			}
		}

		createEntry(hash, key, value, i);
	}

	private void putAllForCreate(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			putForCreate(e.getKey(), e.getValue());
	}

	/**
	 * Rehashes the contents of this map into a new array with a
	 * larger capacity.  This method is called automatically when the
	 * number of keys in this map reaches its threshold.
	 *
	 * If current capacity is MAXIMUM_CAPACITY, this method does not
	 * resize the map, but sets threshold to Integer.MAX_VALUE.
	 * This has the effect of preventing future calls.
	 *
	 * @param newCapacity the new capacity, MUST be a power of two;
	 *        must be greater than current capacity unless current
	 *        capacity is MAXIMUM_CAPACITY (in which case value
	 *        is irrelevant).
	 */
	void resize(int newCapacity) {
		if ( table.length == MAXIMUM_CAPACITY ) {
			threshold = Integer.MAX_VALUE;
			return;
		}
		table = transfer(new Entry[newCapacity]);
		mask = table.length - 1;
		threshold = (int)(newCapacity * loadFactor);
		if ( resizeMonitor != null ) {
			resizeMonitor.visit(newCapacity);
		}
	}

	/**
	 * Transfers all entries from current table to newTable.
	 */
	Entry[] transfer(Entry[] newTable) {
		Entry<K,V> e, next;
		int i;
		final Entry[] src = table;
		// newTable.length is a power of 2, so length-1 is all 1's.
		final int newMask = newTable.length - 1;
		for ( int j=0,n=src.length; j<n; ++j ) {
			if ( (e=src[j]) != null ) {
				src[j] = null; // make GC easier
				do {
					next = e.next;
					e.next = newTable[i = e.hash & newMask];
					newTable[i] = e;
					e = next;
				} while ( e != null );
			}
		}
		return newTable;
	}

	/**
	 * Copies all of the mappings from the specified map to this map.
	 * These mappings will replace any mappings that this map had for
	 * any of the keys currently in the specified map.
	 *
	 * @param m mappings to be stored in this map
	 * @throws NullPointerException if the specified map is null
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		int numKeysToBeAdded = m.size();
		if (numKeysToBeAdded == 0)
			return;

		/*
		 * Expand the map if the map if the number of mappings to be added
		 * is greater than or equal to threshold.  This is conservative; the
		 * obvious condition is (m.size() + size) >= threshold, but this
		 * condition could result in a map with twice the appropriate capacity,
		 * if the keys to be added overlap with the keys already in this map.
		 * By using the conservative calculation, we subject ourself
		 * to at most one extra resize.
		 */
		if (numKeysToBeAdded > threshold) {
			int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
			if (targetCapacity > MAXIMUM_CAPACITY)
				targetCapacity = MAXIMUM_CAPACITY;
			int newCapacity = table.length;
			while (newCapacity < targetCapacity)
				newCapacity <<= 1;
			if (newCapacity > table.length)
				resize(newCapacity);
		}

		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	/**
	 * Removes the mapping for the specified key from this map if present.
	 *
	 * @param  key key whose mapping is to be removed from the map
	 * @return the previous value associated with <tt>key</tt>, or
	 *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 *         (A <tt>null</tt> return can also indicate that the map
	 *         previously associated <tt>null</tt> with <tt>key</tt>.)
	 */
	@Override
	public V remove(Object key) {
		if ( key == null ) {
			V value = nullKeyValue;
			nullKeyValue = null;
			return value;
		}
		Entry<K,V> e = removeEntryForKey((K)key);
		if ( e == null )
			return null;
		return e.value;
	}

	/**
	 * Removes and returns the entry associated with the specified key
	 * in the HashMap.  Returns null if the HashMap contains no mapping
	 * for this key.
	 */
	final Entry<K,V> removeEntryForKey(K key) {
		if ( key == null ) {
			Entry<K,V> entry = new Entry(0, null, nullKeyValue, null);
			nullKeyValue = null;
			return entry;
		}
		final int hash = hash(key.hashCode());
//		final int i = indexFor(hash, table.length);
		final int i = hash & mask;
		Entry<K,V> prev = table[i];
		Entry<K,V> e = prev;
		Entry<K,V> next;
		K k;
		while (e != null) {
			next = e.next;
			if ( e.hash==hash
			  && ((k=e.key)==key || (key!=null && key.equals(k))) ) {
				++modCount;
				--size;
				if ( prev == e )
					table[i] = next;
				else
					prev.next = next;
				e.recordRemoval(this);
				return e;
			}
			prev = e;
			e = next;
		}
		return e;
	}

	final Entry<K, V> removeEntryForNotNullKey(K key) {
		final int hash = hash(key.hashCode());
		final int i = hash & mask;
		Entry<K,V> prev = table[i];
		Entry<K,V> e = prev;
		Entry<K,V> next;
		K k;
		while (e != null) {
			next = e.next;
			if ( e.hash==hash && ((k=e.key)==key || key.equals(k)) ) {
				++modCount;
				--size;
				if (prev == e) // e is first entry in linked-list at table[i]
					table[i] = next;
				else
					prev.next = next;
				e.recordRemoval(this);
				return e;
			}
			prev = e;
			e = next;
		}
		return e;
	}

	/**
	 * Special version of remove for EntrySet.
	 */
	final Entry<K,V> removeMapping(Object o) {
		if (!(o instanceof Map.Entry))
			return null;
		Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
		Object key = entry.getKey();
		int hash;
		if (key == null)
			hash = 0;
		else
			hash = hash(key.hashCode());
		int i = indexFor(hash, table.length);
		Entry<K,V> prev = table[i];
		Entry<K,V> e = prev;
		while (e != null) {
			Entry<K,V> next = e.next;
			if (e.hash == hash && e.equals(entry)) {
				++modCount;
				--size;
				if (prev == e)
					table[i] = next;
				else
					prev.next = next;
				e.recordRemoval(this);
				return e;
			}
			prev = e;
			e = next;
		}
		return e;
	}

	/**
	 * Removes all of the mappings from this map.
	 * The map will be empty after this call returns.
	 */
	@Override
	public void clear() {
		++modCount;
		Entry[] myTable = table;
		for ( int i=0, n=myTable.length; i<n; ++i )
			myTable[i] = null;
		nullKeyValue = null;
		size = 0;
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
		if ( value == null )
			return containsNullValue();
		if ( value == nullKeyValue )
			return true;
		final Entry[] tab = table;
		Entry e;
		for ( int i=0, n=tab.length; i<n; ++i )
			for ( e=tab[i]; e!=null; e=e.next )
				if ( value.equals(e.value) )
					return true;
		return false;
	}

	/**
	 * Special-case code for containsValue with null argument
	 */
	private boolean containsNullValue() {
		final Entry[] tab = table;
		Entry e;
		for ( int i=0, n=tab.length; i<n; ++i )
			for ( e=tab[i]; e!=null; e=e.next )
				if ( e.value == null )
					return true;
		return false;
	}

	/**
	 * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
	 * values themselves are not cloned.
	 *
	 * @return a shallow copy of this map
	 */
	@Override
	public Object clone() {
		MyHashMap<K,V> result = null;
		try {
			result = (MyHashMap<K,V>)super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		assert result != null;
		result.table = new Entry[table.length];
		result.mask = mask;
		result.entrySet = null;
		result.modCount = 0;
		result.size = 0;
		result.initialise();
		result.putAllForCreate(this);

		return result;
	}

	public static class Entry<K,V> implements Map.Entry<K,V> {
		final int hash;
		final K key;
		V value;
		Entry<K,V> next;
		/** Creates new entry. */
		Entry(int h, K k, V v, Entry<K,V> n) {
			hash = h;
			key = k;
			value = v;
			next = n;
		}
		@Override
		public final K getKey() {
			return key;
		}
		@Override
		public final V getValue() {
			return value;
		}
		@Override
		public final V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}
		@Override
		public final boolean equals(Object o) {
			if ( !(o instanceof Map.Entry) )
				return false;
			Map.Entry e = (Map.Entry)o;
			K k1 = getKey();
			K k2 = (K)e.getKey();
			if ( k1==k2 || (k1!=null && k1.equals(k2)) ) {
				V v1 = getValue();
				V v2 = (V)e.getValue();
				if ( v1==v2 || (v1!=null && v1.equals(v2)) )
					return true;
			}
			return false;
		}
		@Override
		public final int hashCode() {
			if ( key == null ) {
				if ( value == null )
					return 0;
				else
					return value.hashCode();
			} else {
				if ( value == null )
					return key.hashCode();
				else
					return key.hashCode() | value.hashCode();
			}
		}
		@Override
		public final String toString() {
			return getKey() + EQUALS + getValue();
		}
		/** This method is invoked whenever the entry is
		 * removed from the table. */
		void recordRemoval(MyHashMap<K,V> m) {
		}
	}

	/**
	 * Adds a new entry with the given key, value and hash to the given bucket
	 * of my table; and also resizes the table if it is now too small.
	 * <p>
	 * Subclasses override this to alter the behaviour of the put method.
	 */
	void addEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K,V> e = table[bucketIndex];
		table[bucketIndex] = new Entry<>(hash, key, value, e);
		if ( ++size > threshold )
			resize(table.length<<1);
	}

	/**
	 * Like addEntry except that this version is used when creating entries
	 * as part of Map construction or "pseudo-construction" (cloning,
	 * deserialization).  This version needn't worry about resizing the table.
	 *
	 * Subclass overrides this to alter the behavior of HashMap(Map),
	 * clone, and readObject.
	 */
	void createEntry(int hash, K key, V value, int bucketIndex) {
		Entry<K,V> e = table[bucketIndex];
		table[bucketIndex] = new Entry<>(hash, key, value, e);
		++size;
	}

	private abstract class HashIterator<E> implements Iterator<E> {
		Entry<K,V> next;        // next entry to return
		int expectedModCount;   // For fast-fail
		int index;              // current slot
		Entry<K,V> current;     // current entry

		HashIterator() {
			expectedModCount = modCount;
			if (size > 0) { // advance to first entry
				Entry[] t = table;
				while ( index<t.length && (next=t[index++])==null )
					; // intentional null statement
			}
		}

		@Override
		public final boolean hasNext() {
			return next != null;
		}

		final Entry<K,V> nextEntry() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			Entry<K,V> e = next;
			if (e == null)
				throw new NoSuchElementException();

			if ((next = e.next) == null) {
				Entry[] t = table;
				while ( index<t.length && (next=t[index++])==null )
					; // intentional null statement
			}
			current = e;
			return e;
		}

		@Override
		public void remove() {
			if (current == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			K k = current.key;
			current = null;
			MyHashMap.this.removeEntryForKey(k);
			expectedModCount = modCount;
		}

	}

	private final class ValueIterator extends HashIterator<V> {
		@Override
		public V next() { return nextEntry().value; }
	}

	private final class KeyIterator extends HashIterator<K> {
		@Override
		public K next() { return nextEntry().getKey(); }
	}

	private final class EntryIterator extends HashIterator<Map.Entry<K,V>> {
		@Override
		public Map.Entry<K,V> next() { return nextEntry(); }
	}

	// Subclass overrides these to alter behavior of views' iterator() method
	Iterator<K> newKeyIterator() { return new KeyIterator(); }
	Iterator<V> newValueIterator() { return new ValueIterator(); }
	Iterator<Map.Entry<K,V>> newEntryIterator() { return new EntryIterator(); }

	// Views

	private transient Set<Map.Entry<K,V>> entrySet = null;

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own <tt>remove</tt> operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations.
	 */
	@Override
	public Set<K> keySet() {
		final Set<K> ks = keySet;
		if ( ks != null )
			return ks;
		return keySet = new KeySet();
	}

	private final class KeySet extends AbstractSet<K> {
		@Override
		public int size() { return size; }
		@Override
		public boolean contains(Object o) { return containsKey(o); }
		@Override
		public boolean remove(Object o) { return MyHashMap.this.removeEntryForKey((K)o) != null; }
		@Override
		public void clear() { MyHashMap.this.clear(); }
		@Override
		public Iterator<K> iterator() { return newKeyIterator(); }
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is backed by the map, so changes to the map are
	 * reflected in the collection, and vice-versa.  If the map is
	 * modified while an iteration over the collection is in progress
	 * (except through the iterator's own <tt>remove</tt> operation),
	 * the results of the iteration are undefined.  The collection
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
	 * support the <tt>add</tt> or <tt>addAll</tt> operations.
	 */
	@Override
	public Collection<V> values() {
		final Collection<V> vs = values;
		if ( vs != null )
			return vs;
		return values=new Values();
	}

	private final class Values extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() { return newValueIterator(); }
		@Override
		public int size() { return size; }
		@Override
		public boolean contains(Object o) { return containsValue(o); }
		@Override
		public void clear() { MyHashMap.this.clear(); }
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own <tt>remove</tt> operation, or through the
	 * <tt>setValue</tt> operation on a map entry returned by the
	 * iterator) the results of the iteration are undefined.  The set
	 * supports element removal, which removes the corresponding
	 * mapping from the map, via the <tt>Iterator.remove</tt>,
	 * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
	 * <tt>clear</tt> operations.  It does not support the
	 * <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a set view of the mappings contained in this map
	 */
	@Override
	public Set<Map.Entry<K,V>> entrySet() {
		return entrySet0();
	}

	private Set<Map.Entry<K,V>> entrySet0() {
		final Set<Map.Entry<K,V>> es = entrySet;
		if ( es != null )
			return es;
		return entrySet = new EntrySet();
	}

	private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
		@Override
		public Iterator<Map.Entry<K,V>> iterator() {
			return newEntryIterator();
		}
		@Override
		public boolean contains(final Object o) {
			return o instanceof Map.Entry
				&& contains((Map.Entry<K,V>) o);
		}
		public boolean contains(final Map.Entry<K,V> e) {
			final Entry<K,V> candidate = getEntry(e.getKey());
			return candidate!=null && candidate.equals(e);
		}
		@Override
		public boolean remove(final Object o) {
			return removeMapping(o) != null;
		}
		@Override
		public int size() {
			return size;
		}
		@Override
		public void clear() {
			MyHashMap.this.clear();
		}
	}

	/**
	 * Save the state of the <tt>HashMap</tt> instance to a stream (ie,
	 * serialize it).
	 *
	 * @serialData The <i>capacity</i> of the HashMap (the length of the
	 *             bucket array) is emitted (int), followed by the
	 *             <i>size</i> (an int, the number of key-value
	 *             mappings), followed by the key (Object) and value (Object)
	 *             for each key-value mapping.  The key-value mappings are
	 *             emitted in no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
		throws IOException
	{
		final Iterator<Map.Entry<K,V>> it;
		if ( size > 0 )
			it = entrySet0().iterator();
		else
			it = null;

		// Write out the threshold, loadfactor, and any hidden stuff
		s.defaultWriteObject();

		// Write out number of buckets
		s.writeInt(table.length);

		// Write out size (number of Mappings)
		s.writeInt(size);

		// Write out keys and values (alternating)
		if (it != null) {
			while (it.hasNext()) {
				Map.Entry<K,V> e = it.next();
				s.writeObject(e.getKey());
				s.writeObject(e.getValue());
			}
		}
	}

	private static final long serialVersionUID = 362498820763181265L;

	/**
	 * Reconstitute the <tt>HashMap</tt> instance from a stream (ie,
	 * deserialize it).
	 */
	private void readObject(java.io.ObjectInputStream s)
		 throws IOException, ClassNotFoundException {
		// Read in the threshold, loadfactor, and any hidden stuff
		s.defaultReadObject();
		// Read in number of buckets and allocate the bucket array;
		int numBuckets = s.readInt();
		table = new Entry[numBuckets];
		initialise();  // Give subclass a chance to do its thing.
		// Read in size (number of Mappings)
		int size = s.readInt();
		// Read the keys and values, and put the mappings in the HashMap
		for ( int i=0; i<size; ++i ) {
			K key = (K) s.readObject();
			V value = (V) s.readObject();
			putForCreate(key, value);
		}
	}

	// These methods are used when serializing HashSets
	int   capacity()     { return table.length; }
	float loadFactor()   { return loadFactor;   }
}
