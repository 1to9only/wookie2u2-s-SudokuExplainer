/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.MyStrings.SB;

/**
 * This simple forward-only-linked-list implementation of the Queue interface
 * does NOT throw ConcurrentModificationException when you interlace offering
 * new items at my foot with repeatedly polling off my head, to expedite use.
 * <p>
 * This class only exists for performance sake, if s__t goes screwy just use a
 * java.util.LinkedList as a Deque instead. Work-around the CMEs by creating a
 * new list for each pass. See Juillerats Chainer as an example. MLFQ does not
 * CME, hence does NOT require a new queue for every level, which is a bit more
 * efficient, I think.
 *
 * @author Keith Corlett 2017 Dec
 * @param <E> the Element type
 */
public final class MyLinkedFifoQueue<E> extends AbstractQueue<E> implements Queue<E> {

	// FIFO: a forwards only linked list (not the normal double-linked list)
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
	}

	public MyLinkedFifoQueue(final E e) {
		super();
		add(e);
	}

	/**
	 * Add all 'c' to this Queue. Note that Collection has no alternative to an
	 * iterator, which is a bit slow Redge.
	 *
	 * @param c
	 */
	public MyLinkedFifoQueue(final Collection<E> c) {
		super();
		if ( c.size() > 0 )
			for ( E e : c ) {
				// do add in line because it is faster
				final Node<E> n = new Node<>(e);
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
	public boolean offer(final E e) {
		return add(e);
	}

	@Override
	public boolean add(final E e) {
		final Node<E> n = new Node<>(e);
		foot.next = n;
		foot = n;
		++size;
		return true;
	}

	/**
	 * Add all of 'c' to this Queue. Note that Collection has no alternative to
	 * an iterator, which is a bit slow Redge, but MyLinkedList (below) does.
	 *
	 * @param c
	 * @return
	 */
	@Override
	public boolean addAll(final Collection<? extends E> c) {
		if ( c==null || c.isEmpty() )
			return false;
		for ( E e : c ) { // there is no alternative to an iterator
			// do add in line because it is faster
			final Node<E> n = new Node<>(e);
			foot.next = n;
			foot = n;
			++size;
		}
		return true;
	}

	/**
	 * Add all of the 'E' in the given 'c' to me, at warp factor fly be free.
	 * <p>
	 * addAll is hammered coz MyLinkedFifoQueue is used as the "to do" List in
	 * AChainingHint.getParents that addAll(MyLinkedList a.parents) of almost
	 * always 1 Ass, so we can avoid creating an iterator, and I hate Iterator!
	 * <p>
	 * RANT: Calling two methods to process one element is fine until ya put it
	 * in a loop and call it for every instance in another collection, which is
	 * called for every instance in yet another collection, or whatever, so you
	 * call TWO methods 2 billion times, instead of ONE method 2 billion times,
	 * and end-up spending half your bloody life waiting for creation of
	 * stack-frames. While (thing=things.next()!=null) is faster!
	 * But then my rule is to NEVER add null to a Collection, unless there is a
	 * VERY good reason. I cut my teeth on null terminated lists and the like.
	 * The more rigorous rule-set of old is what makes sense to me. This new
	 * fangled slow-as-____-Iterator-thing s__ts me, so I avoid them.
	 * <p>
	 * Basically building in an O(2*n) operation just to get the next element
	 * was a bad call, but we are now welded onto the gloriously non-performant
	 * piece of s__t. As an engineer, I advise you to never let an architect
	 * design your foundations.
	 * <p>
	 * So, I hope I am faster, because I AM ugly!
	 *
	 * @param c s__t to add
	 * @return was any s__t added?
	 */
	public boolean addAll(final MyLinkedList<E> c) {
		if ( c == null )
			return false;
		switch ( c.size ) {
			case 0:
				return false;
			// Most Ass's have 1 parent, so no need for a slow iterator.
			case 1: { // this block localises variables
				final Node<E> n = new Node<>(c.first.item);
				foot.next = n;
				foot = n;
				++size;
				return true;
			}
			// Some Ass's have multiple parents. Still no slow iterator coz c
			// is a MyLinkedList, that exposes it is internal linked-list.
			// @see my rant in the method comment-block. I hate Iterator!
			default: { // this block localises variables.
				Node<E> n;
				for ( MyLinkedList.Node<E> cn=c.first; cn!=null; cn=cn.next ) {
					n = new Node<>(cn.item);
					foot.next = n;
					foot = n;
					++size;
				}
				return true;
			}
		}
	}

	/** @return the head (front, top) of this queue,
	 * or {@code null} if this queue is empty. */
	@Override
	public E poll() {
		if ( size == 0 )
			return null;
		final E result = head.next.item;
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
	public Object[] toArray() {
		return toArray(new Object[size]);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T[] toArray(final T[] a) { // T must be type compatible with E, ergo T is E, or a superType thereof. This "compatible" rule includes interface types. The types are NOT checked at compile time, so may throw type-cast exceptions at runtime, which sux, but Java does not (currently) allow us to do any better.
		int i=0;
		for ( Node c=head.next; c!=null && i<size; c=c.next )
			a[i++] = (T)c.item;
		return a;
	}

	@Override
	public String toString() {
		final StringBuilder sb = SB(size<<4); // * 16 is just a guess
		int cnt = 0;
		for ( Node<E> c=head.next; c!=null; c=c.next ) { // c for Current
			if ( ++cnt > 1 )
				sb.append(CSP);
			sb.append(String.valueOf(c.item));
		}
		return sb.toString();
	}

	@Override
	public Iterator<E> iterator() {
		return new MyIterator();
	}

	private class MyIterator implements Iterator<E> {
		Node<E> curr = head;
		@Override
		public boolean hasNext() {
			return curr.item!=null;
		}
		@Override
		public E next() {
			final E result = curr.item;
			curr = curr.next;
			return result;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove is not supported.");
		}
	}

}
