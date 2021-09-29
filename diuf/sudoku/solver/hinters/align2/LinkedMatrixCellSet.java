/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.utils.Frmt.NULL_ST;
import diuf.sudoku.utils.IMyPollSet;
import java.util.AbstractSet;
import java.util.Iterator;


/**
 * A LinkedMatrixCellSet is an {@code IMyPollSet<Cell>} which extends
 * {@code java.util.Set<Cell>} with a few funky features:
 * <ul>
 * <li>Fast O(1) contains and remove methods, as apposed to ArrayLists O(n/2) or
 * HashMaps O(8ish depending on chain length, ie the collision rate produced by
 * the elements hashCode() method and the local hash method, which is all out to
 * sea).
 * <li>O(n) clear method, as apposed to HashMaps O(capacity).
 * <li>A "fast" iterator (all iterators are too slow, IMHO) with remove, which
 * does not throw ConcurrentModificationException's. <b>NOTE</b> that unlike
 * java.util.* my Iterator does <b>not</b> ever throw
 * ConcurrentModificationException, so the caller <b>must not</b> modify the set
 * (except through the iterator) whilst iteration is underway. If you're tempted
 * to do that you should probably use this set as a queue (ie the poll() method)
 * instead: mischief managed, and my Iterator is a bit faster to boot.
 * <li>Accessing {@code poll()} currently requires your reference-type be either
 * {@code IMyPollSet<Cell>} or the LinkedMatrixCellSet implementation. Almost
 * any Set can be extended to provide a simple poll() method. I really can't
 * think why it's not in the JAPI. We gain so much usefulness for so little
 * effort. I guess that most programmers are using list-as-queue where they
 * actually want set-as-queue, simply because that's all the JAPI provides. I'd
 * also guess that most of the programmers who do this don't realise it, and
 * therefore software is buggier because multiple instances of a thing may be
 * enqued and processed, with demi-kaotic results.
 * <li>NB: Everything in this class is public. The Node, the head, the foot, the
 * matrix, and the size. I operate on the premise of look, but don't touch,
 * unless you understand it (A. Einstein), or you just really really want to
 * (D. Adams). But, if you want to access my public bits then you're innately
 * bound to LinkedMatrixCellSet as your reference type, which is limiting. You
 * never get something for nothing. Speed or flexibility. Your choice.
 * <li>A {@code int[] idx()} method to get an idx (index) of the cells in this
 * Set as an array of 3 times 27-bit bitsets = 81 cells. Also the idx1 and idx2
 * methods get the intersection of this Set and another, returning does the
 * result contain less than 1 (or 2) cells: being a fast O(1) "&=" bit-twiddle
 * retainAll, instead of hammering the ____ing ____ out of my nice fast O(1)
 * contains method. Sigh.
 * <li>The downside is high memory usage and therefore a slow constructor. So
 * use me where you need idx's or a fast contains/remove implementation and
 * you can get away with clearing instead of reconstructing. Basically, I'm at
 * my best as a field, which you clear, fill, and then read; repeatedly.
 * </ul>
 * <p>
 * This used-to-be an inner class in AlignedTripleExclusion which mysteriously
 * ran 20 seconds slower, and IIRC inner classes are optimised differently, so
 * I "promoted it" to an outer-class; that's only (currently) used in
 * Aligned*Exclusion. The mysterious slow-down went away, in fact it was
 * 113.45 seconds faster than HashSet for top1465. You could still extend a
 * {@code HashSet<Cell>} instead of me (if you think I have bugs); you'd just
 * have to work-out how to do the poll and idx/1/2 methods.
 * <p>
 * Aligned*Exclusion is NOT used in LogicalSolver.SPEED mode, BUT asat May 2018
 * I've sped-up everything to the point that I'm (mostly) using ACCURACY mode in
 * LogicalSolverTester coz it's "fast enough" and it picks-up all the heavies
 * and chains; except that Aligned*Exclusion is/are still too slow, so I mostly
 * deselect them in the Options Solving Techniques dialog.<br>
 * See: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku}
 * <p>
 * So this is a part of the push to the ultimate fast accurate analysis of any
 * Sudoku puzzle. It's a challenge.
 * <p>
 * This is free, open source software: use, re-use, enhance, adapt, steal, or
 * just steal some ideas. The only rule is "It's NOT my ____ing problem!".
 */
public class LinkedMatrixCellSet
		extends AbstractSet<Cell>
		implements IMyPollSet<Cell>
{
	// a node in a doubly-linked list of Cells
	public static final class Node {
		public Cell cell;
		public Node prev;
		public Node next;
		Node(Node prev, Cell cell, Node next) {
			this.prev = prev;
			this.cell = cell;
			this.next = next;
		}
		@Override
		public String toString() {
			if ( cell == null )
				return NULL_ST;
			return cell.toString()+"=>"+next;
		}
	}

	// Takes a wee while to create, something like 144 ns each in testing, but
	// it seems to depend on how you hold your tongue. Dunno why arrays are so
	// slow in Java. Slow to create, slow to read, slower to write. Argh! Meh!
	public final Node[][] matrix = new Node[9][9];
	public Node head = null;
	public Node foot = null;
	public int size = 0;

	/**
	 * Constructs a new empty LinkedMatrixCellSet.
	 */
	public LinkedMatrixCellSet() {
	}

// It's a shame that this isn't used any longer, but Scoobie's happier.
// I'm retaining it just for the foot assignment, which is "mad clever".
//	/**
//	 * Constructs a new LinkedMatrixCellSet of the first 'n' Cells in the given
//	 * 'cells' array, for the fast contains method; rather than hammer a slower
//	 * O(n/2) array contains method.
//	 * <p>Currently not used but retained anyways. It'll be useful one day.
//	 * @param n the number of cells in the cells array to be added. Note that
//	 * this may be less than cells.length, ie a re-usable array.
//	 * @param cells to be added to this set.
//	 */
//	public LinkedMatrixCellSet(final int n, Cell[] cells) {
//		super();
//		// pass out if s__t looks ____ed. Always good advice.
//		if ( cells==null || cells.length==0 || n<1 )
//			return;
//		// create the first node
//		Cell cell = cells[0];
//		matrix[cell.y][cell.x] = head = foot = new Node(null, cell, null);
//		// create any subsequent nodes
//		for ( int i=1; i<n; ++i ) { // NOTE that 1
//			// Work that ____ out Scoobie: It runs right to left, mostly, and
//			// still makes my brain HURT! Assignments are NOT transitive!
//			matrix[(cell=cells[i]).y][cell.x] = foot = foot.next
//					= new Node(foot, cell, null); // the existing foot
//			++size;
//		}
//	}

	/**
	 * Allows this Set to be used as a Queue.
	 * @return the first Cell removed from this Set, else null.
	 */
	@Override
	public Cell poll() {
		if ( head == null )
			return null;
		return remove(head.cell);
	}

	@Override
	public boolean contains(Object o) {
		if ( !(o instanceof Cell) )
			return false;
		Cell cell = (Cell)o;
		return matrix[cell.y][cell.x] != null;
	}
	public boolean contains(Cell cell) {
		return matrix[cell.y][cell.x] != null;
	}

	@Override
	public boolean add(Cell cell) {
		if ( matrix[cell.y][cell.x] != null )
			return false; // I told him we've allready got one.
		Node node = new Node(foot, cell, null);
		if ( head == null )
			head = foot = node; // NB: foot was null when node was constructed
		else
			foot = foot.next = node;
		matrix[cell.y][cell.x] = node;
		++size;
		idx = null;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return (o instanceof Cell) && remove((Cell)o)!=null;
	}
	public Cell remove(Cell cell) {
		final Node node = matrix[cell.y][cell.x];
		if ( node == null )
			return null; // I told him we allready don't got one. Hmmm.
		matrix[cell.y][cell.x] = null;
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
		idx = null;
		return cell;
	}

	@Override
	public void clear() {
		// this concept was stolen from 1.8's java.util.LinkedList:
		// Clearing all of the links between nodes is "unnecessary", but:
		// - helps a generational GC if the discarded nodes inhabit more
		//   than one generation
		// - is sure to free memory even if there is a reachable Iterator
		switch (size) {
		case 0:
			return;
		case 1:
			matrix[head.cell.y][head.cell.x] = null;
			head.cell = null;
			// prev and next are already null
			break;
		default:
			for ( Node n=head,next; n!=null; n=next ) {
				next = n.next;
				matrix[n.cell.y][n.cell.x] = null;
				n.cell = null;
				n.next = null;
				n.prev = null;
			}
		}
		size = 0;
		head = foot = null;
		idx = null;
	}

	// overridden trying to make it faster.
	// just check the damn size yourself, directly, that's why it's public.
	@Override
	public int size() {
		return size;
	}

	// overridden trying to make it faster.
	// just check the damn size yourself, directly, that's why it's public.
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Iterators are slow, because they call two methods: hasNext and next
	 * for each element.
	 * @return
	 */
	@Override
	public Iterator<Cell> iterator() {
		return new Iterator<Cell>() {
			private Node curr = null; // curr is before the head, for remove!
			private Node next = head; // so the next is the head
			@Override
			public boolean hasNext(){
				return next != null;
			}
			@Override // r for result
			public Cell next(){
				Cell result = next.cell;
				curr = next;
				next = next.next; // Yep, Scoobie can lick his own balls!
				return result;
			}
			@Override
			public void remove(){
				// let it NPE if curr == null; remove called before next.
				LinkedMatrixCellSet.this.remove(curr.cell);
			}
		};
	}

	// ------------------------------- indexland -------------------------------

	/**
	 * Get a bitset index of the cells in this CellSet. An Idx is an array of
	 * three 27-bit bitsets (3 * 27 = 81) containing Grid.cells indices. The
	 * position (from the right) of each set (1) bit is a Grid.cells array
	 * indice (Cell.i) of a cell that is present in this CellSet.
	 * <p>
	 * Idx's exist to do retainAll <b>quickly</b> in Aligned*Exclusion using
	 * the bitwise-and binary operator: {@code 1010 & 1101 == 1000}. An Idx
	 * turns retainAll which was O(n) calls of contains(Cell) into ONE
	 * bitwise-and operation, which is MUCH faster.
	 * <p>
	 * <b>Villainious Laughenating Wobblybonks Batman!</b>
	 * <p>
	 * The {@link Idx#cells(Grid grid, Cell[] cells)} method populates an array
	 * of Cells from an idx, and returns it. Idx also has an iterator, but it's
	 * slower than iterating an array, so especially if you iterate repeatedly
	 * use a re-usable array, not the iterator!
	 * <p>
	 * idx() is O(n) even for a cache-miss, not O(81). The idx is cached. The
	 * cache is cleared when you add or remove an item or clear this Set. The
	 * cache is shared with the other idx*(...) methods, so its created ONCE
	 * no matter which you call first.
	 * <p>
	 * idx() is O(1) for a cache hit. We rely on cache-hits to reduce runtime,
	 * which happens in A*E, especially the larger ones, where each cell in the
	 * set visits each excluder-set, so larger sets means a higher cache hit
	 * rate, and it's the larger A*E's that are REALLY slow.
	 *
	 * @return cached Idx. Do NOT ____ with its contents! You WILL be shot!
	 */
	Idx idx() {
		if ( idx == null ) { // created once then cached (until you change set)
			a[0]=a[1]=a[2]=0; // zero THE static array
			for ( Node n=head; n!=null; n=n.next ) // O(size) faster than O(81)
				a[n.cell.idxdex] |= n.cell.idxshft;  // add n.cell.i to idx
			idx = new Idx(a);
		}
		return idx;
	}
	private Idx idx;
	private static final int[] a = new int[3];

	/**
	 * This is shorthand for Aligned*Exclusion to call {@link #idx()} and set
	 * the result to the intersection of my index and given other index, ie:
	 * <pre>{@code
	 *	result[0] = mine[0] & other[0];
	 *	result[1] = mine[1] & other[1];
	 *	result[2] = mine[2] & other[2];
	 * }</pre>
	 * and return is the result empty, ie contains zero set (1) bits.
	 * <p>
	 * It's implemented this way rather than repeat the intersection code many
	 * times in the various A*E implementation classes. It's also faster.
	 * <p>
	 * Trixily I share the cache with the other idx*(...) methods, so it is
	 * read ONCE (unless you modify this Set).
	 *
	 * @param result Idx the result index to be set
	 * @param other Idx the other index to bitwise-and (&amp;) with mine
	 * @return is the result empty, ie contains zero set (1) bits. This odd
	 *  return value is exactly what we want in A*E. Implementing it here ONCE
	 *  saves us repeating a pretty tricky line of code hundreds of times.
	 */
	boolean idx1(final Idx result, final Idx other) {
		final Idx me = idx();
		// return result isEmpty
		return (result.a0 = me.a0 & other.a0) == 0
			 & (result.a1 = me.a1 & other.a1) == 0
			 & (result.a2 = me.a2 & other.a2) == 0;
	}

	/**
	 * Another shorthand for Aligned*Exclusion which calls {@code idx()} sets
	 * the result to {@code idx &amp; other} and returns: Does result contain
	 * LESS THAN two indices?
	 * @param result int[3] the result index to set
	 * @param other int[3] the other index to "and" with this one
	 * @return does the result index contain LESS THAN two values? This odd
	 * return value is exactly what's required in A*E. Implementing it here
	 * ONCE saves repeating some pretty tricky code hundreds of times.
	 */
	boolean idx2(final Idx result, final Idx other) {
		final Idx me = idx();
		int cnt=0, only=0; // the only element that's non-zero
		if ( (result.a0=me.a0 & other.a0)>0 && ++cnt==1 )
			only = result.a0;
		if ( (result.a1=me.a1 & other.a1)>0 && ++cnt==1 )
			only = result.a1;
		if ( (result.a2=me.a2 & other.a2)>0 && ++cnt==1 )
			only = result.a2;
		// return does the result index contain LESS THAN 2 set (1) bits?
		return cnt==0
			|| ( cnt==1 && VSIZE[ only       & 511]
				         + VSIZE[(only>>>9)  & 511]
				         + VSIZE[(only>>>18) & 511] == 1 );
	}

	public static interface CellVisitor {
		public boolean visit(Cell c);
	}

	// nb: myForEach to avert collision with Iterable forEach
	public boolean myForEach(CellVisitor v) {
		for ( Node n=head; n!=null; n=n.next )
			if ( v.visit(n.cell) )
				return true;
		return false;
	}
}
