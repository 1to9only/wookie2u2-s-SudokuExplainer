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

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;


/**
 * This class implements the <tt>Set</tt> interface, backed by a hash table
 * (actually a <tt>MyHashMap</tt> instance).  It makes no guarantees as to the
 * iteration order of the set; in particular, it does not guarantee that the
 * order will remain constant over time.  This class permits the <tt>null</tt>
 * element.
 *
 * <p>This class offers constant time performance for the basic operations
 * (<tt>add</tt>, <tt>remove</tt>, <tt>contains</tt> and <tt>size</tt>),
 * assuming the hash function disperses the elements properly among the
 * buckets.  Iterating over this set requires time proportional to the sum of
 * the <tt>MyHashSet</tt> instance's size (the number of elements) plus the
 * "capacity" of the backing <tt>MyHashMap</tt> instance (the number of
 * buckets).  Thus, it is very important not to set the initial capacity too
 * high (or the load factor too low) if iteration performance is important.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash set concurrently, and at least one of
 * the threads modifies the set, it <i>must</i> be synchronized externally.
 * This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the set.
 *
 * If no such object exists, the set should be "wrapped" using the
 * {@link Collections#synchronizedSet Collections.synchronizedSet}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the set:<pre>
 *   Set s = Collections.synchronizedSet(new MyHashSet(...));</pre>
 *
 * <p>The iterators returned by this class's <tt>iterator</tt> method are
 * <i>fail-fast</i>: if the set is modified at any time after the iterator is
 * created, in any way except through the iterator's own <tt>remove</tt>
 * method, the Iterator throws a {@link ConcurrentModificationException}.
 * Thus, in the face of concurrent modification, the iterator fails quickly
 * and cleanly, rather than risking arbitrary, non-deterministic behavior at
 * an undetermined time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Collection
 * @see     Set
 * @see     TreeSet
 * @see     MyHashMap
 * @since   1.2
 */

public class MyHashSet<E>
	extends AMySet<E>
	implements IMyPollSet<E>, Cloneable, java.io.Serializable
{
	static final long serialVersionUID = -5024744406713321676L;

	private transient MyLinkedHashMap<E,Object> map;

	// Dummy value to associate with an Object in the backing Map
	private static final Object PRESENT = new Object();

	/**
	 * Constructs a new, empty set; the backing <tt>MyHashMap</tt> instance has
	 * default initial capacity (16) and load factor (0.75).
	 */
	public MyHashSet() {
		map = new MyLinkedHashMap<>();
	}

	/**
	 * Constructs a new set containing the elements in the specified
	 * collection.  The <tt>MyHashMap</tt> is created with default load factor
	 * (0.75) and an initial capacity sufficient to contain the elements in
	 * the specified collection.
	 *
	 * @param c the collection whose elements are to be placed into this set
	 * @throws NullPointerException if the specified collection is null
	 */
	public MyHashSet(Collection<? extends E> c) {
		map = new MyLinkedHashMap<>(Math.max((int) (c.size()/0.75F) + 1, 16));
		addAll(c);
	}

	/**
	 * Constructs a new, empty set; the backing <tt>MyHashMap</tt> instance has
	 * the specified initial capacity and the specified load factor.
	 *
	 * @param      initialCapacity   the initial capacity of the hash map
	 * @param      loadFactor        the load factor of the hash map
	 * @throws     IllegalArgumentException if the initial capacity is less
	 *             than zero, or if the load factor is nonpositive
	 */
	public MyHashSet(int initialCapacity, float loadFactor) {
		map = new MyLinkedHashMap<>(initialCapacity, loadFactor);
	}

	/**
	 * Constructs a new, empty set; the backing <tt>MyHashMap</tt> instance has
	 * the specified initial capacity and default load factor (0.75).
	 *
	 * @param      initialCapacity   the initial capacity of the hash table
	 * @throws     IllegalArgumentException if the initial capacity is less
	 *             than zero
	 */
	public MyHashSet(int initialCapacity) {
		map = new MyLinkedHashMap<>(initialCapacity);
	}

	/**
	 * Constructs a new, empty linked hash set.  (This package private
	 * constructor is only used by LinkedHashSet.) The backing
	 * MyHashMap instance is a LinkedHashMap with the specified initial
	 * capacity and the specified load factor.
	 *
	 * @param      initialCapacity   the initial capacity of the hash map
	 * @param      loadFactor        the load factor of the hash map
	 * @param      dummy             ignored (distinguishes this
	 *             constructor from other int, float constructor.)
	 * @throws     IllegalArgumentException if the initial capacity is less
	 *             than zero, or if the load factor is nonpositive
	 */
	MyHashSet(int initialCapacity, float loadFactor, boolean dummy) {
		map = new MyLinkedHashMap<>(initialCapacity, loadFactor);
	}

	/**
	 * Returns an iterator over the elements in this set.  The elements
	 * are returned in no particular order.
	 *
	 * @return an Iterator over the elements in this set
	 * @see ConcurrentModificationException
	 */
	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	/**
	 * Returns the number of elements in this set (its cardinality).
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * Does this Set contain any elements? ie !isEmpty
	 *
	 * @return {@code map.size() > 0}
	 */
	@Override
	public boolean any() {
		return map.size() > 0;
	}

	/**
	 * Returns <tt>true</tt> if this set contains no elements.
	 *
	 * @return <tt>true</tt> if this set contains no elements
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns <tt>true</tt> if this set contains the specified element.
	 * More formally, returns <tt>true</tt> if and only if this set
	 * contains an element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o element whose presence in this set is to be tested
	 * @return <tt>true</tt> if this set contains the specified element
	 */
	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	/**
	 * Adds the specified element to this set if it is not already present.
	 * More formally, adds the specified element <tt>e</tt> to this set if
	 * this set contains no element <tt>e2</tt> such that
	 * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
	 * If this set already contains the element, the call leaves the set
	 * unchanged and returns <tt>false</tt>.
	 *
	 * @param e element to be added to this set
	 * @return <tt>true</tt> if this set did not already contain the specified
	 * element
	 */
	@Override
	public boolean add(E e) {
		return map.put(e, PRESENT)==null;
	}

	public boolean addOnly(final E e) {
		return map.addOnly(e, PRESENT);
	}

	/**
	 * Removes the specified element from this set if it is present.
	 * More formally, removes an element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>,
	 * if this set contains such an element.  Returns <tt>true</tt> if
	 * this set contained the element (or equivalently, if this set
	 * changed as a result of the call).  (This set will not contain the
	 * element once the call returns.)
	 *
	 * @param o object to be removed from this set, if present
	 * @return <tt>true</tt> if the set contained the specified element
	 */
	@Override
	public boolean remove(Object o) {
		return map.remove(o)==PRESENT;
	}

	/**
	 * Removes all of the elements from this set.
	 * The set will be empty after this call returns.
	 */
	@Override
	public void clear() {
		map.clear();
	}

	/**
	 * Returns a shallow copy of this <tt>MyHashSet</tt> instance: the elements
	 * themselves are not cloned.
	 *
	 * @return a shallow copy of this set
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			MyHashSet<E> newSet = (MyHashSet<E>)super.clone();
			newSet.map = (MyLinkedHashMap<E, Object>)map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Save the state of this <tt>MyHashSet</tt> instance to a stream (that is,
	 * serialize it).
	 *
	 * @serialData The capacity of the backing <tt>MyHashMap</tt> instance
	 *             (int), and its load factor (float) are emitted, followed by
	 *             the size of the set (the number of elements it contains)
	 *             (int), followed by all of its elements (each an Object) in
	 *             no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
		throws java.io.IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();

		// Write out MyHashMap capacity and load factor
		s.writeInt(map.capacity());
		s.writeFloat(map.loadFactor());

		// Write out size
		s.writeInt(map.size());

		// Write out all elements in the proper order.
		for (E e : map.keySet())
			s.writeObject(e);
	}

	/**
	 * Reconstitute the <tt>MyHashSet</tt> instance from a stream (that is,
	 * deserialize it).
	 */
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s)
		throws java.io.IOException, ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();

		// Read in MyHashMap capacity and load factor and create backing MyHashMap
		int capacity = s.readInt();
		float loadFactor = s.readFloat();
		map = (((MyHashSet)this) instanceof MyLinkedHashSet ?
			   new MyLinkedHashMap<E,Object>(capacity, loadFactor) :
			   new MyLinkedHashMap<E,Object>(capacity, loadFactor));

		// Read in size
		int size = s.readInt();

		// Read in all elements in the proper order.
		for (int i=0; i<size; i++) {
			E e = (E) s.readObject();
			map.put(e, PRESENT);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public E poll() {
		final java.util.Map.Entry<E,?> e = map.poll();
		return e==null ? null : e.getKey();
	}

	@Override
	public void pollAll(final IMyPollSet<E> src) {
		E e;
		while ( (e=src.poll()) != null )
			add(e);
	}

	/**
	 * Compares the specified object with this set for equality.
	 * <p>
	 * If the given object is also a MyHashSet (or subtype MyLinkedHashSet)
	 * then we compare there two maps, so that two equal sets must have all
	 * the same keys WITH ALL THE SAME VALUES.
	 * <p>
	 * Otherwise I defer to the implementation of equals in my superclass
	 * (AMySet), which handles all types of sets, but reduces equality to
	 * just all the same keys.
	 * <p>
	 * I do all this just so that my bloody testcases work properly! sigh.
	 *
	 * @param o object to be compared for equality with this set
	 * @return <tt>true</tt> if the specified object is equal to this set
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o) {
		if ( o == this )
			return true;
		// if both objects are MyHashSet's then compare there maps
		if ( o instanceof MyHashSet )
			return this.map.equals(((MyHashSet)o).map);
		// else defer to AMySet's implementation of equals
		return super.equals(o);
	}

	@Override
	public E get(E pKey) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public E remove() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
