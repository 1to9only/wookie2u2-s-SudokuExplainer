/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;


/**
 * <p><tt>java.util.</tt>{@link java.util.LinkedHashSet} copy-con doubles the
 * {@code initialCapacity} and then {@code HashMap} constructor rounds that up
 * to the next power of 2, which is a bit of a waste in the case where we know
 * exactly how big the table needs to be, so I created <b>MyLinkedHashSet</b>
 * whose copy-con doesn't double-up.
 * <p>Also, I've always wanted to debug {@link java.util.LinkedHashMap}, coz
 *  it's like troat fooking a feral cat: slightly amusing, but mostly painful.
 * <p><tt>HashTable</tt> and <tt>LinkedList</tt> implementation of the
 *  <tt>Set</tt> interface, with predictable iteration order. This
 *  implementation differs from <tt>HashSet</tt> in that it maintains a doubly-
 *  linked list running through all of its entries, which defines the iteration
 *  ordering: ie: the <i>insertion-order</i> of the set.
 * <p>Note that insertion order is <i>not</i> affected if an element is
 *  <i>re-inserted</i> into the set. (An element <tt>e</tt> is reinserted into
 *  a set <tt>s</tt> if <tt>s.add(e)</tt> is invoked when <tt>s.contains(e)</tt>
 *  would return <tt>true</tt> immediately prior to the invocation.)
 * <p>This implementation spares its clients from the unspecified, generally
 *  chaotic ordering provided by {@link HashSet}, without incurring the
 *  increased cost associated with {@link TreeSet}.  It can be used to
 *  produce a copy of a set that has the same order as the original, regardless
 *  of the original set's implementation:
 * <pre>
 *     void foo(Set s) {
 *         Set copy = new MyLinkedHashSet(s);
 *         ...
 *     }
 * </pre>
 * <p>This technique is particularly useful if a module takes a set on input,
 *  copies it, and later returns results whose order is determined by that of
 *  the copy. (I generally appreciate having things returned in the same order
 *  I presented them. Do you?)
 *
 * <p>This class provides all of the optional <tt>Set</tt> operations, and
 * permits null elements.  Like <tt>HashSet</tt>, it provides constant-time
 * performance for the basic operations (<tt>add</tt>, <tt>contains</tt> and
 * <tt>remove</tt>), assuming the hash function disperses elements
 * properly among the buckets.  Performance is likely to be just slightly
 * below that of <tt>HashSet</tt>, due to the added expense of maintaining the
 * linked list, with one exception: Iteration over a <tt>MyLinkedHashSet</tt>
 * requires time proportional to the <i>size</i> of the set, regardless of
 * its capacity.  Iteration over a <tt>HashSet</tt> is likely to be more
 * expensive, requiring time proportional to its <i>capacity</i>.
 *
 * <p>A linked hash set has two parameters that affect its performance:
 * <i>initial capacity</i> and <i>load factor</i>.  They are defined precisely
 * as for <tt>HashSet</tt>.  Note, however, that the penalty for choosing an
 * excessively high value for initial capacity is less severe for this class
 * than for <tt>HashSet</tt>, as iteration times for this class are unaffected
 * by capacity.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked hash set concurrently, and at least
 * one of the threads modifies the set, it <em>must</em> be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the set.
 *
 * If no such object exists, the set should be "wrapped" using the
 * {@link Collections#synchronizedSet Collections.synchronizedSet}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the set: <pre>
 *   Set s = Collections.synchronizedSet(new MyLinkedHashSet(...));</pre>
 *
 * <p>The iterators returned by this class's <tt>iterator</tt> method are
 * <em>fail-fast</em>: if the set is modified at any time after the iterator
 * is created, in any way except through the iterator's own <tt>remove</tt>
 * method, the iterator will throw a {@link ConcurrentModificationException}.
 * Thus, in the face of concurrent modification, the iterator fails quickly
 * and cleanly, rather than risking arbitrary, non-deterministic behavior at
 * an undetermined time in the future.
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
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Set
 * @see     HashSet
 * @see     TreeSet
 * @see     Hashtable
 * @since   1.4
 */
public class MyLinkedHashSet<E>
	extends MyHashSet<E>
	implements IMyPollSet<E>, Cloneable, java.io.Serializable {

	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD_FACTOR = 0.75F;

	private static final long serialVersionUID = -2851667679971038690L;

	/**
	 * Constructs a new, empty linked hash set with the specified initial
	 * capacity and load factor.
	 *
	 * @param      initialCapacity the initial capacity of the linked hash set
	 * @param      loadFactor      the load factor of the linked hash set
	 * @throws     IllegalArgumentException  if the initial capacity is less
	 *               than zero, or if the load factor is nonpositive
	 */
	public MyLinkedHashSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor, true);
	}

	/**
	 * Constructs a new, empty linked hash set with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param   initialCapacity   the initial capacity of the MyLinkedHashSet
	 * @throws  IllegalArgumentException if the initial capacity is less
	 *              than zero
	 */
	public MyLinkedHashSet(int initialCapacity) {
		super(initialCapacity, DEFAULT_LOAD_FACTOR, true);
	}

	/**
	 * Constructs a new, empty linked hash set with the default initial
	 * capacity (16) and load factor (0.75).
	 */
	public MyLinkedHashSet() {
		super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, true);
	}

	/**
	 * Constructs a new linked hash set with the same elements as the given
	 * collection. The linked hash set is created with an initialCapacity
	 * big-enough to hold only the elements in the given collection and the
	 * NON-default load factor of 1.0.
	 *
	 * @param c  the collection whose elements are to be placed into
	 *           this set
	 * @throws NullPointerException if the specified collection is null
	 */
	public MyLinkedHashSet(Collection<? extends E> c) {
		super(Math.max(c.size(), DEFAULT_CAPACITY), DEFAULT_LOAD_FACTOR, true);
		//addAll(c);
        for (E e : c)
            add(e);
		assert size() == c.size();
	}

	/**
	 * Constructs a new linked hash set with the same elements as the
	 * specified arguments array. The linked hash set is created with an
	 * initial-capacity sufficient to hold the elements in the given array
	 * and the default load factor of 0.75.
	 *
	 * @param a  the array whose elements are to be placed into this set
	 * @throws NullPointerException if the specified array is null
	 */
	@SuppressWarnings("unchecked")
	public MyLinkedHashSet(E[] a) {
		super(Math.max(a.length, DEFAULT_CAPACITY), DEFAULT_LOAD_FACTOR, true);
		for ( E e : a )
			add(e);
	}

	/**
	 * Constructs a new linked hash set containing 'e' with an initial
	 * capacity of 8 and the NON-default load factor of 1.0.
	 *
	 * @param e  the element to be placed into this set
	 */
	public MyLinkedHashSet(E e) {
		super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, true);
		add(e);
	}

	/**
	 * Retrieves and removes the head of this MyLinkedHashSet,
	 * or returns <tt>null</tt> if this MyLinkedHashSet is empty.
	 *
	 * @return the head of this MyLinkedHashSet, or <tt>null</tt>
	 * if this MyLinkedHashSet is empty
	 */
	@Override
	public E poll() { // NB: override required to expose method publicly.
		return super.poll(); // protected in parent coz only usable when Linked
	}

	/** The Set is empty when this method returns.
	 * Performance is O(n), better than MyHashMap.clears O(capacity). */
	@Override
	public void clear() {
		while ( super.poll() != null ); // null statement is intentional
	}

}