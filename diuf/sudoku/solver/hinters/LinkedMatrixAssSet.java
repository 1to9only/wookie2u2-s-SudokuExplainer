/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMyPollSet;
import diuf.sudoku.utils.IMySet;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * LinkedMatrixAssSet (an IAssSet) extends {@code AbstractSet<Ass>} to provide
 * high-performance contains and remove methods, at the cost of high RAM usage
 * which causes slow construction (we create a 9k array) so construct me as
 * little as possible: clear-and-reuse me instead.
 * <p>
 * Note that LinkedMatrixAssSet is always "Funky", ie add does NOT update
 * existing Ass's, it returns false; consistent with {@link FunkyAssSet}.
 * This is a pre-requisite of the getAss method which returns the Ass with
 * parents, ie the first one added. Any attempt to add a duplicate does
 * nothing, and returns false. This differs from java.util.Set implementations
 * which all replace an existing with the proffered new, and return false.
 * Ergo: It wasn't me boss! Mental!
 * <p>
 * Features are:<ul>
 * <li>contains and remove methods are fast O(1) as apposed to the slow O(1)
 *  of a {@code HashSet<Ass>} such as {@link FunkyAssSet}.</li>
 * <li>a fast iterator (for an iterator, which are all slow compared to Javas
 *  array-iterator, mainly because of two methods per element).</li>
 * <li>clear is O(size) as apposed to arrays O(capacity).</li>
 * <li>WARNING: Construction is pretty slow thanks to the 9,270 byte matrix, so
 *  do NOT create me OFTEN, or MANY. Never in a loop!</li>
 * </ul>
 * KRC 2020-10-23 moved LinkedMatrixAssSet from diuf.sudoku.utils coz nothing
 * with project-types (except Debug) should be in utils (to keep utils portable
 * between projects); It is now used in both chain and fish packages; so I am
 * not really sure where it belongs; so here it is, above both of them.
 */
public final class LinkedMatrixAssSet extends AbstractSet<Ass>
		implements IAssSet, IMyPollSet<Ass> {

	// a node in a doubly-linked list of Ass's
	private static final class Node {
		public Node prev;
		public final Ass ass;
		public Node next;
		Node(Node prev, Ass ass, Node next) {
			this.prev = prev;
			this.ass = ass;
			this.next = next;
		}
		@Override
		public String toString() {
			return (prev==null?"#":"<") + ass.toString() + (next==null?"#":">");
		}
	}

	private Node head = null;
	private Node foot = null;
	// nodes indexes: [cell.i][value]
	private final Node[][] nodes = new Node[81][10];
	public int size = 0;

	public LinkedMatrixAssSet() {
	}

	// never used now, but retained anyway
	public LinkedMatrixAssSet(LinkedMatrixAssSet src) {
		copy(src);
	}

	/**
	 * Set this LinkedMatrixAssSet to a copy of the source.
	 * @param source to copy
	 */
	public void copyOf(LinkedMatrixAssSet source) {
		clear();
		copy(source);
	}

	/**
	 * The copy method is protected so that it can be overridden. The copyOf
	 * method clears this LinkedMatrixAssSet and then calls me, or whatever
	 * you override me to be. If you want to add operations then override me
	 * to do your pre-stuff, call me with super.copy(source);, then do your
	 * post-stuff. If you want copyOf to append (no clear) then override copyOf
	 * and just call me with copy(source);.
	 *
	 * @param source to copy
	 */
	protected void copy(LinkedMatrixAssSet source) {
		if ( source.head == null )
			return;
		Ass a;
		Node sn = source.head;
		Node dn = new Node(foot, sn.ass, null);
		head = foot = dn;
		for ( sn=sn.next; sn!=null; sn=sn.next ) {
			a = sn.ass;
			dn = new Node(foot, a, null);
			foot.next = dn;
			foot = dn;
			nodes[a.i][a.value] = dn;
			++size;
		}
	}

	/**
	 * getAss implements IAssSet.getAss.
	 * <p>WARN this implementation relies upon all Ass's in this set having the
	 *  same isOn, which happily is how it is "naturally" in Chainer.
	 *
	 * @param cell Cell from which we take just the x and y coordinates.
	 * @param value the potential value you seek
	 * @return the Ass if it is present, else null
	 */
	@Override
	public Ass getAss(Cell cell, int value) {
		final Node n = nodes[cell.i][value];
		if ( n == null )
			return null;
		return n.ass;
	}

	/**
	 * @return remove and return the first Ass, else null means empty Set.
	 */
	@Override
	public Ass poll() {
		if ( head == null )
			return null;
		// remove and return head.ass
		Ass ass = head.ass;
		assert nodes[ass.i][ass.value] != null;
		nodes[ass.i][ass.value] = null;
		--size;
		if ( foot == head )
			head = foot = null; // ie an empty list
		else
			head = head.next;
		return ass;
	}

	/**
	 * Remove and return the first element of this Set, throwing
	 * a NoSuchElementException if the set is already empty.
	 * Specified by the IMySet interface for consistency with Deque.
	 *
	 * @return the first Ass in this Set, else throws
	 */
	@Override
	public Ass remove() {
		if ( head == null )
			throw new NoSuchElementException();
		return remove(head.ass);
	}

	/**
	 * Returns the full Assumption (ie with ancestors) from this Set.
	 *
	 * @param a the identity-only (ie without ancestors) Assumption to fetch.
	 * @return the full Assumption with ancestors.
	 */
	@Override
	public Ass get(Ass a) {
		final Node n = nodes[a.i][a.value];
		if ( n == null )
			return null;
		return n.ass;
	}

	/**
	 * visit returns "is this the first time we've visited so-and-so?".
	 * Returns true the first time you visit each Ass, and false thereafter.
	 *
	 * @param a the Ass to visit
	 * @return did this Ass NOT already exist? because it does now.
	 */
	@Override
	public boolean visit(Ass a) {
		// I told him we've already got one
		if ( nodes[a.i][a.value] != null )
			return false;
		// Ok, add this Ass and return true (inlined for speed)
		final Node newNode = new Node(foot, a, null);
		if ( head == null )
			head = foot = newNode; // nb: foot was null when newNode was built
		else {
			foot.next = newNode;
			foot = newNode;
		}
		nodes[a.i][a.value] = newNode;
		++size;
		return true; // We've seen this one now
	}

	@Override
	public boolean contains(Object o) {
		return o!=null && o instanceof Ass && contains((Ass)o);
	}

	/**
	 * The contains(Ass) method is specified by IAssSet. Returns true if this
	 * Set contains the given ass, else false. Note that the given Ass must not
	 * be null. A null Ass will cause a NullPointerException.
	 * <p>
	 * I'm trying to make my generic collections (none of which can contain a
	 * null Ass) to use contains(T) instead of contains(Object) and I'm rapidly
	 * discovering why the Java collections framework uses contains(Object).
	 *
	 * @param a The ass to examine
	 * @return true if 'a' is in this Set, else false
	 */
	@Override
	public boolean contains(Ass a) {
		return nodes[a.i][a.value] != null;
	}

	@Override
	public boolean add(Ass a) {
		// The "Funky" test: do NOT replace an existing Ass, just return false.
		if ( nodes[a.i][a.value] != null )
			return false;
		Node newNode = new Node(foot, a, null);
		if ( head == null )
			head = foot = newNode; // nb: foot was null when newNode was built
		else {
			foot.next = newNode;
			foot = newNode;
		}
		nodes[a.i][a.value] = newNode;
		++size;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return (o instanceof Ass) && remove((Ass)o)!=null;
	}

	public Ass remove(Ass ass) {
		final Node node = nodes[ass.i][ass.value];
		if ( node == null )
			return null;
		nodes[ass.i][ass.value] = null;
		--size;
		if ( node == head )
			if ( foot == head )
				head = foot = null; // ie an empty list
			else
				head = head.next;
		else {
			Node p=node.prev, n=node.next;
			p.next = n;
			if ( n != null )
				n.prev = p;
		}
		return node.ass;
	}

	@Override
	public void clear() {
		if ( size == 0 )
			return;
		if ( size < 13 )
			for ( Node n=head; n!=null; n=n.next )
				nodes[n.ass.i][n.ass.value] = null;
		else
			for ( int i=0; i<81; ++i )
				Arrays.fill(nodes[i], null);
		size = 0;
		head = foot = null;
	}

	@Override
	public Iterator<Ass> iterator() {
		return new Iterator<Ass>() {
			private Node p = null; // p for previous
			private Node c = head; // c for current
			@Override
			public boolean hasNext(){
				return c!=null;
			}
			@Override // r for result
			public Ass next(){
				Ass r = c.ass;
				p = c;
				c = c.next;
				return r;
			}
			@Override
			public void remove(){
				LinkedMatrixAssSet.this.remove(p.ass); // let it throw NPE
			}
		};
	}

	@Override
	public int size() {
		return size;
	}

	/** do retainAll and @return isEmpty. */
	@Override
	public boolean retainAllIsEmpty(IMySet<Ass> c) {
		if ( size > 0 )
			for ( Iterator<Ass> it=iterator(); it.hasNext(); )
				if ( !c.contains(it.next()) )
					it.remove();
		return size==0;
	}

	/** do retainAll and then c.clear() */
	@Override
	public void retainAllClear(IMySet<Ass> c) {
		for ( Iterator<Ass> it=iterator(); it.hasNext(); )
			if ( !c.remove(it.next()) )
				it.remove();
		c.clear();
	}

}
