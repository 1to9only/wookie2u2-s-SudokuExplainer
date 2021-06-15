/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;


/**
 * This simple forward-only-linked-list implementation of the Queue interface
 * does NOT throw ConcurrentModificationException when you interlace offering
 * new items at my foot with repeatedly polling off my head, which greatly
 * expedites my use as a queue.
 * <p>This class only exists for performance sake, if s__t goes wonky then try
 * using a java.util.LinkedList as a Deque instead, and work-around the CMEs
 * by creating a new list for each pass. See Juillerats Chainer as an example.
 * @author Keith Corlett 2017 Dec
 * @param <E> the Element type
 */
public final class MyLinkedFifoQueue<E> extends AbstractQueue<E> implements Queue<E> {

	public static final class Node<E> {
		public E item;
		public Node<E> next; // NB: forward linked only (there is no prev)
		public Node(E e) { this.item = e; }
		// the head has the-one-and-only null item
		@Override
		public String toString() { return String.valueOf(item); }
	}

	public Node<E> head, foot;
	public int size;

	public MyLinkedFifoQueue() {
		super();
		foot = head = new Node<>(null);
		size = 0;
	}

	public MyLinkedFifoQueue(E e) {
		super();
		add(e);
	}

	/**
	 * Add all 'c' to this Queue. Note that Collection has no alternative to an
	 * iterator, which is a bit slow Redge.
	 *
	 * @param c
	 */
	public MyLinkedFifoQueue(Collection<E> c) {
		super();
		if ( c.size() > 0 )
			for (E e : c) {
				// do add in line because it's faster
				Node<E> n = new Node<>(e);
				foot.next = n;
				foot = n;
				++size;
			}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0; // NB: this is fast but dodgy coz size() can be overridden
	}

	/** Add 'e' to the foot (tail, back-end) of this queue.
	 * @return true. */
	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public boolean add(E e) {
		Node<E> n = new Node<>(e);
		foot.next = n;
		foot = n;
		++size;
		return true;
	}

	/**
	 * Add all of 'c' to this Queue.  Note that Collection has no alternative
	 * to an iterator, which is a bit slow Redge.
	 * @param c
	 * @return
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if ( c==null || c.isEmpty() )
			return false;
		for (E e : c) { // there is no alternative to an iterator
			// do add in line because it's faster
			Node<E> n = new Node<>(e);
			foot.next = n;
			foot = n;
			++size;
		}
		return true;
	}

	// This method get's hammered (a bit) coz MyLinkedFifoQueue is used as the
	// to-do-list in AChainingHint.getParents, which addsAll(a.parents), which
	// is a MyLinkedList of almost allways 1 Ass, so we can avoid the normal
	// iterator. I hope this is a bit faster, coz it's damn ugly!
	public boolean addAll(MyLinkedList<E> c) {
		if ( c == null )
			return false;
		switch ( c.size ) {
			case 0:
				return false;
			// Most Ass's have 1 parent, so no need for a slow iterator.
			case 1: { // this block localises variables
				Node<E> node = new Node<>(c.first.item);
				foot.next = node;
				foot = node;
				++size;
				return true;
			}
			// Some Ass's have multiple parents. Still no slow iterator coz I'm
			// a MyLinkedList, which exposes it's internal data-structures.
			default: { // this block localises variables
				Node<E> node;
				for ( MyLinkedList.Node<E> cn=c.first; cn!=null; cn=cn.next ) {
					node = new Node<>(cn.item);
					foot.next = node;
					foot = node;
					++size;
				}
				return true;
			}
		}
	}

	/** @return the head (front, top) of this queue,
	 * or <tt>null</tt> if this queue is empty. */
	@Override
	public E poll() {
		if ( size == 0 )
			return null;
		E result = head.next.item;
		head.next.item = null; // make it easy for GC
		head = head.next;
		if ( --size == 0 ) // ergo foot==head
			foot = head = new Node<>(null);
		return result;
	}

    /**
     * Removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
	@Override
    public void clear() {
		size = 0;
		foot = head = new Node<>(null);
    }

	@Override
	public E peek() {
		return head.item;
	}


	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			Node<E> curr = head;
			@Override
			public boolean hasNext() {
				return curr.item!=null;
			}
			@Override
			public E next() {
				E result = curr.item;
				curr = curr.next;
				return result;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException("remove is not supported.");
			}
		};
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size]);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T[] toArray(T[] a) {
		int i=0;
		for ( Node c=head.next; c!=null && i<size; c=c.next )
			a[i++] = (T)c.item;
		return a;
	}

	@Override
	public String toString() {
		StringBuilder mySB = getSB();
		int cnt = 0;
		for ( Node<E> c=head.next; c!=null; c=c.next ) { // c for Current
			if ( ++cnt > 1 )
				mySB.append(", ");
			mySB.append(String.valueOf(c.item));
		}
		return mySB.toString();
	}
	private static StringBuilder getSB() {
		if ( sb == null )
			sb = new StringBuilder(64); // let it grow as required
		else
			sb.setLength(0);
		return sb;
	}
	private static StringBuilder sb = null;
}