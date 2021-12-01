/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.utils.Frmt.NULL_ST;
import diuf.sudoku.utils.IMyPollSet;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * LinkedMatrixCellSet exists for speed.
 * <p>
 * I implement {@link ILinkedCellSet},
 * which adds {idx, idx1, and idx2} to {@link IMyPollSet<Cell>},
 * which adds {poll} {@code java.util.Set<Cell>}.
 * <p>
 * Features:<ul>
 * <li>Fast O(1) contains and remove methods, as apposed to ArrayLists hopeless
 * O(n/2) or HashMaps better-but-still-not-great O(8'ish).
 * <li>O(n) clear method, as apposed to HashMaps O(capacity).
 * <li>Fast iterator (all iterators are too slow, IMHO) with remove, which does
 * not (unlike java.util.*) throw ConcurrentModificationException, so my caller
 * <b>MUST NOT</b> modify the set (except through the iterator) while iteration
 * is underway. If you're tempted then use poll instead, that's what it's for.
 * <li>Using {@code poll()} currently requires your reference-type be either
 * {@code IMyPollSet<Cell>} or this LinkedMatrixCellSet implementation. <br>
 * Almost any Set can be extended to provide a simple poll() method. I can't
 * think why it's not in the JAPI. We gain much utility for little trouble. I
 * guess that most programmers are using list-as-queue where they actually want
 * set-as-queue because that's all the JAPI provides. I also guess most hackers
 * who do this don't realise they've done it, hence software is buggier because
 * multiple instances are vended, yielding chaotic results.
 * <li>All my fields are public {node, head, foot, matrix, and size} on the
 * premise of look, but don't touch, unless you understand it (Einstein A.), or
 * you just really want to (Adams D.). But, accessing fields requires you bind
 * to LinkedMatrixCellSet. Still, ya kana have aught for nix. So, you can have
 * speed or implementation-independence. Your choice.
 * <li>The {@code int[] idx()} method gets an idx (index) of the cells in this
 * Set as three by 27-bit bitsets, for 81 cells. Also the idx1 and idx2 methods
 * get the intersection of this Set and another, returning does result contain
 * less than 1 (or 2) cells: yielding a fast O(1) "&=" bit-twiddle retainAll,
 * instead of hammering the ____ing s__t out of my fast O(1) contains method,
 * which is inherently O(n). sigh.
 * <li>The downside is high memory usage and therefore a slow constructor. So
 * use me where you need idx's or a fast contains/remove implementation and you
 * can get away with clearing instead of reconstructing. Basically, I'm at my
 * best as a field, which you clear, fill, and then read; repeatedly.
 * </ul>
 * I was an inner class of Aligned3Exclusion which mysteriously ran 20 seconds
 * slower, and IIRC inner classes are optimised differently, so I was promoted
 * to an outer-class; that's only (currently) used in A*E. Slowness solved. In
 * fact I'm now 113.45 secs faster than HashSet over top1465.
 * <p>
 * If you suspect I have bugs then drop-in LinkedHashCellSet instead.
 * If results differ there's a good chance I have a bug.
 */
public class LinkedMatrixCellSet
		extends AbstractSet<Cell>
		implements ILinkedCellSet
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
			return cell==null ? NULL_ST : cell.toString()+"=>"+next;
		}
	}

	// The 3 bitsets of the Idx we build. It's a bit faster to re-use a single
	// array than it is to repeatedly create on the fly. Array creation in Java
	// seems VERY slow, and I know not why. I build several billion Idx's, so
	// any efficiency is time-significant, and this one's simple to boot.
	private static final int[] ARRAY = new int[3];

	private Idx idx;

	// NB: This array is a bit slow to create.
	public final Node[][] matrix = new Node[REGION_SIZE][REGION_SIZE];
	public Node head = null;
	public Node foot = null;
	public int size = 0;

	/**
	 * Constructs a new empty LinkedMatrixCellSet.
	 */
	public LinkedMatrixCellSet() {
	}

// It's a shame that this isn't used any longer, but Scoobie's pretty happy.
//	/**
//	 * Constructs a new LinkedMatrixCellSet of 'n' Cells from the given 'cells'
//	 * array, to provide a fast contains method; rather than hammer a slower
//	 * O(n/2) array contains method.
//	 * <p>Currently not used but retained anyways. It'll be useful one day.
//	 * @param n the number of cells in the cells array to be added. Note that
//	 * this may be less than cells.length, ie a null terminated array.
//	 * @param cells to add to this set.
//	 */
//	public LinkedMatrixCellSet(final int n, Cell[] cells) {
//		super();
//		if ( cells==null || cells.length==0 || n<1 )
//			return;
//		// create the first node
//		Cell cell = cells[0];
//		matrix[cell.y][cell.x] = head = foot = new Node(null, cell, null);
//		// create any subsequent nodes
//		for ( int i=1; i<n; ++i ) { // NOTE that 1
//			// Work that ____ out Scoobie! HINT: It runs right to left (mostly).
//			matrix[(cell=cells[i]).y][cell.x] = foot = foot.next
//					= new Node(foot, cell, null); // that's the existing foot
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

	@Override
	public Iterator<Cell> iterator() {
		return new Iterator<Cell>() {
			private Node curr = null; // curr is before the head
			private Node next = head; // so the next is the head
			@Override
			public boolean hasNext(){
				return next != null;
			}
			@Override // r for result
			public Cell next(){
				Cell result = next.cell;
				curr = next;
				next = next.next; // Yep, Scoobie's licking his own balls again!
				return result;
			}
			@Override
			public void remove(){
				LinkedMatrixCellSet.this.remove(curr.cell);
			}
		};
	}

	// ------------------------------- indexland ------------------------------

	/**
	 * Get a cached Idx of this CellSet. Ie get an Idx containing the indice
	 * of each cell in this CellSet.
	 * <p>
	 * The returned Idx is fundamentally just an array of 3 ints, with 27 bits
	 * used in each, to represent the 81 cells in a Grid. The position of each
	 * set (1) bit represents a cell/indice that is present in this Set.
	 * <p>
	 * Idx's exist for A*E to implement a <u>fast</u> {@code retainAll} on
	 * {@code CellSet}s using bitwise-and: {@code 1010 & 1101 == 1000}.
	 * <p>
	 * This method is O(n) even for a cache-miss, not O(81), and it turns
	 * retainAll which was O(n) calls of my fast 0(1) contains(Cell) method
	 * into <u>ONE</u> fast O(1) "&" operation. I'm a bit proud of this.
	 * <p>
	 * NOTE: The idx is cached internally. The cache is cleared when you add or
	 * remove an item, or clear this Set. Trixily I share the idx cache with
	 * the other idx*(...) methods, so its read ONCE no matter which you call.
	 * <p>
	 * NOTE: This method is fast O(1) for a cache hit. I rely on cache-hits per
	 * set to reduce runtime, which happens in A*E, especially the big ones,
	 * where each cell visits each excluder-set, so bigger sets means a higher
	 * cache-hit-rate, and it's the big ones that're slow.
	 *
	 * @return cached int[3] * 27 used bits = 81 cells. Do NOT ____ with the
	 * arrays contents! You WILL be shot!
	 */
	@Override
	public Idx idx() {
		if ( idx == null ) { // cache until this set changes, which nulls idx
			ARRAY[0]=ARRAY[1]=ARRAY[2]=0; // zero THE static array
			for ( Node n=head; n!=null; n=n.next ) // O(size) faster than O(81)
				ARRAY[n.cell.idxdex] |= n.cell.idxshft;  // add n.cell.i to idx
			idx = new Idx(ARRAY);
		}
		return idx;
	}

	/**
	 * This is shorthand for Aligned*Exclusion to call {@link #idx()} and set
	 * the 'result' to the intersection of me and the 'other' index, ie:
	 * <pre>{@code
	 *	result[0] = mine[0] & other[0];
	 *	result[1] = mine[1] & other[1];
	 *	result[2] = mine[2] & other[2];
	 * }</pre>
	 * returning is the result empty, ie contains zero set (1) bits.
	 * <p>
	 * It's implemented this way rather than repeat the intersection code many
	 * times in the various A*E implementation classes. It's also faster.
	 * <p>
	 * Trickily I share the cache with the other idx*(...) methods, so it is
	 * read ONCE (unless you modify this Set).
	 * <p>
	 * Implementing idx1 and idx2 here ONCE saves repeating tricky code many
	 * times.
	 *
	 * @param other Idx the other index to bitwise-and (&amp;) with mine
	 * @param result Idx the result index to be set
	 * @return is the result empty, ie contains zero set (1) bits. This odd
	 *  return value is exactly what we want in A*E. Implementing it here ONCE
	 *  saves us repeating a pretty tricky line of code hundreds of times.
	 */
	@Override
	public boolean idx1(final Idx other, final Idx result) {
		final Idx x = idx(); // get my idx (cached)
		// return result isEmpty
		return (result.a0 = x.a0 & other.a0) == 0
			 & (result.a1 = x.a1 & other.a1) == 0
			 & (result.a2 = x.a2 & other.a2) == 0;
	}

	/**
	 * Shorthand for Aligned*Exclusion which calls {@code idx()} sets result
	 * to {@code idx &amp; other} and returns: Does result contain LESS THAN
	 * two indices?
	 * <p>
	 * Implementing idx1 and idx2 here ONCE saves repeating tricky code many
	 * times.
	 *
	 * @param other int[3] the other index to "and" with this one
	 * @param result int[3] the result index to set
	 * @return does the result index contain LESS THAN two values? This odd
	 * return value is exactly what's required in A*E.
	 */
	@Override
	public boolean idx2(final Idx other, final Idx result) {
		int cnt = 0 // the number of elements that're non-zero
		  , only = 0; // the only element that's non-zero
		final Idx x = idx(); // get my idx (cached)
		if ( (result.a0=x.a0 & other.a0)>0 && ++cnt==1 )
			only = result.a0;
		if ( (result.a1=x.a1 & other.a1)>0 && ++cnt==1 )
			only = result.a1;
		if ( (result.a2=x.a2 & other.a2)>0 && ++cnt==1 )
			only = result.a2;
		// return does result contain < 2 set bits?
		return cnt==0
			|| (cnt==1 && VSIZE[ only       & 511] // 511 is 9 1's
			            + VSIZE[(only>>>9)  & 511]
			            + VSIZE[(only>>>18) & 511] == 1);
	}

}
