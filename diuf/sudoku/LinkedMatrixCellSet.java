//KRC 2023-10-14 LinkedMatrixCellSet replaced with an Idx.
//That is SudokuExplainer LinkedMatrixCellSet filteredCells becomes
//                        Idx filteredIndices
//Because this was plain stupid. I should have seen it in the first place!
//
//So, this code is usable and parts of it are well tested. You can pretty much
//count on whatever operations are being done filteredIndices where formerly
//performed on LinkedMatrixCellSet, which are: add, contains/has and clear.
//But I can't say that the iterator, for instance, was ever used, and therefore
//may not work at all.
//
//LinkedMatrixCellSet does not have a test-case, which is a shame. I like test
//cases for utility classes; saves all sorts of horrors down the line, when
//components fail to operate as expected. The fact that this one didn't have a
//test-case made it much more likely to get the chop. Hackers (myself included)
//be warned. If it's worth doing, it's worth doing right, which means PROOF,
//not tinned artichoke hearts on immitation eel flavoured fairy bread. PROOF!
//Hovercraft cost extra. Prometheus nicked my chitlins. Eckkky-thump! Har-jya!
///*
// * Project: Sudoku Explainer
// * Copyright (C) 2006-2007 Nicolas Juillerat
// * Copyright (C) 2013-2023 Keith Corlett
// * Available under the terms of the Lesser General Public License (LGPL)
// */
//package diuf.sudoku;
//
//import diuf.sudoku.Grid.Cell;
//import diuf.sudoku.utils.IMyPollSet;
//import java.util.AbstractSet;
//import java.util.Iterator;
//import static diuf.sudoku.utils.Frmt.ST_NULL;
//
///**
// * A LinkedMatrixCellSet is an {@code IMyPollSet<Cell>}
// * which extends {@code java.util.Set<Cell>}.
// * <p>
// * NOTE: This Set is funky: add is addOnly, it does NOT update existing.
// * <p>
// * My distinguished features are:
// * <ul>
// * <li>Fast O(1) contains and remove methods, as apposed to ArrayLists O(n/2)
// * or HashMaps O(8)ish depending on bucket-list-length, ie the collision rate
// * produced by the elements hashCode() method and the local hash method, which
// * is pseudorandom (ergo mad as a hatter).
// * <li>O(n) clear method, as apposed to HashMaps O(capacity).
// * <li>A "fast" iterator (all iterators are too slow, IMHO) with remove, which
// * does not throw ConcurrentModificationExceptions. <b>NOTE</b> that unlike
// * java.util.* my Iterator does <b>not</b> ever throw
// * ConcurrentModificationException so the caller <b>must not</b> modify the set
// * (except through the iterator) whilst iteration is underway. If tempted to do
// * that you should probably use this set as a queue (ie poll()) instead.
// * Mischief managed, and my Iterator is a bit faster to boot.
// * <li>Accessing {@code poll()} requires your reference-type be either
// * {@code IMyPollSet<Cell>} or the LinkedMatrixCellSet implementation. Almost
// * any Set can be extended to provide a simple poll() method. I really cannot
// * think why it is not in the JAPI. We gain so much usefulness for so little
// * effort. I guess that most programmers are using a-list-as-queue where they
// * actually want a-set-as-queue, simply because thats all the JAPI provides. I
// * guess that most of the programmers who do this do not realise it, therefore
// * software is buggier because multiple instances of a thing may be enqued and
// * processed, producing kaotic results.
// * <li>NB: All my fields are public: Node, head, foot, matrix, and size.
// * I operate on the premise of look, but do not touch, unless you understand it
// * (A. Einstein), or you just really really want to (D. Adams). But if you use
// * my public fields then you are bound to LinkedMatrixCellSet as your reference
// * type, which is limiting. You never get something for nothing. Speed or
// * flexibility. Your choice.
// * <li>My downside is I use a doubly-subscripted-array, which uses rather a lot
// * of memory, and suffers suspiciously slow construction. So use me where you
// * need fast contains/remove and you can get away with clearing instead of OFT
// * construction. Basically, I am at my best as a field, which you clear, fill,
// * and then read; repeatedly.
// * <li>NOTE: there is no point using Grid size constants here, if the grid-size
// * ever changes then the <u>design</u> of Idx (et al) must be adjusted to suit,
// * I imagine I would use inheritance to cater for different grid-sizes.
// * </ul>
// * <p>
// * This class was exhumed from the 'align' package. It started life as an inner
// * class of AlignedTripleExclusion, which is now just AlignedExclusion. Its now
// * also used in SudokuExplainer, and god-knows-what-else. It is NOT a 'utils'
// * class because its locked onto Grid.Cell which is an SE-only class. My utils
// * are supposed to be portable between apps. Im not real sure where it should
// * live (which package) so its up here in the "globals" for now. I guess I want
// * a new 'utilsa' (utilities application) package thats allowed app-types.
// */
//public class LinkedMatrixCellSet extends AbstractSet<Cell> implements IMyPollSet<Cell> {
//
//	// a node in a doubly-linked list of Cells
//	public static final class Node {
//		public Cell cell;
//		public Node prev;
//		public Node next;
//		Node(Node prev, Cell cell, Node next) {
//			this.prev = prev;
//			this.cell = cell;
//			this.next = next;
//		}
//		@Override
//		public String toString() {
//			if ( cell == null )
//				return ST_NULL;
//			return cell.toString()+"=>"+next;
//		}
//	}
//
//	// Takes a wee while to create, something like 144 ns each in testing, but
//	// it seems to depend on how you hold your tongue. Dunno why arrays are so
//	// slow in Java. Slow to create, slow to read, slower to write. Argh! Meh!
//	public final Node[][] matrix = new Node[9][9];
//	public Node head = null;
//	public Node foot = null;
//	public int size = 0;
//
//	/**
//	 * Constructs a new empty LinkedMatrixCellSet.
//	 */
//	public LinkedMatrixCellSet() {
//	}
//
//	/**
//	 * Allows this Set to be used as a Queue.
//	 * @return the first Cell removed from this Set, else null.
//	 */
//	@Override
//	public Cell poll() {
//		if ( head == null )
//			return null;
//		return remove(head.cell);
//	}
//
//	@Override
//	public void pollAll(final IMyPollSet<Cell> src) {
//		Cell c;
//		while ( (c=src.poll()) != null )
//			add(c);
//	}
//
//	@Override
//	public boolean contains(final Object o) {
//		if ( !(o instanceof Cell) )
//			return false;
//		final Cell cell = (Cell)o;
//		return matrix[cell.y][cell.x] != null;
//	}
//	public boolean contains(final Cell cell) {
//		return matrix[cell.y][cell.x] != null;
//	}
//
//	@Override
//	public boolean add(final Cell cell) {
//		if ( matrix[cell.y][cell.x] != null )
//			return false; // I told him we have allready got one.
//		final Node node = new Node(foot, cell, null);
//		if ( head == null )
//			head = foot = node; // NB: foot was null when node was constructed
//		else
//			foot = foot.next = node;
//		matrix[cell.y][cell.x] = node;
//		++size;
//		return true;
//	}
//
//	@Override
//	public boolean remove(final Object o) {
//		return (o instanceof Cell) && remove((Cell)o)!=null;
//	}
//	public Cell remove(final Cell cell) {
//		final Node node = matrix[cell.y][cell.x];
//		if ( node == null )
//			return null; // I told him we allready do not got one. Hmmm.
//		matrix[cell.y][cell.x] = null;
//		--size;
//		if ( node == head )
//			if ( foot == head )
//				head = foot = null; // ie an empty list
//			else
//				head = head.next;
//		else {
//			Node p=node.prev, n=node.next;
//			p.next = n;
//			if ( n != null )
//				n.prev = p;
//		}
//		return cell;
//	}
//
//	@Override
//	public void clear() {
//		// this concept was stolen from 1.8s java.util.LinkedList:
//		// Clearing all of the links between nodes is "unnecessary", but:
//		// - helps a generational GC if the discarded nodes inhabit more
//		//   than one generation
//		// - is sure to free memory even if there is a reachable Iterator
//		switch (size) {
//		case 0:
//			return;
//		case 1:
//			matrix[head.cell.y][head.cell.x] = null;
//			head.cell = null;
//			// prev and next are already null
//			break;
//		default:
//			for ( Node n=head,next; n!=null; n=next ) {
//				next = n.next;
//				matrix[n.cell.y][n.cell.x] = null;
//				n.cell = null;
//				n.next = null;
//				n.prev = null;
//			}
//		}
//		size = 0;
//		head = foot = null;
//	}
//
//	// overridden trying to make it faster.
//	// just check the damn size yourself, directly, that is why it is public.
//	@Override
//	public int size() {
//		return size;
//	}
//
//	// overridden trying to make it faster.
//	// just check the damn size yourself, directly, that is why it is public.
//	@Override
//	public boolean isEmpty() {
//		return size == 0;
//	}
//
//	/**
//	 * Iterators are slow! O(2n) operations is slow. To access each element,
//	 * you invoke two methods: hasNext and next, and a method invocation is
//	 * slow, when compared to iterating an array which has zero method calls
//	 * per element. Use an iterator, just not where performance matters, so
//	 * mainly when you need the remove method.
//	 *
//	 * @return a new iterator over cells in this Set
//	 */
//	@Override
//	public Iterator<Cell> iterator() {
//		return new MyIterator();
//	}
//
//	private class MyIterator implements Iterator<Cell> {
//		private Node prev = null;
//		private Node curr = head;
//		@Override
//		public boolean hasNext(){
//			return curr != null;
//		}
//		@Override
//		public Cell next(){
//			Cell result = curr.cell;
//			prev = curr;
//			curr = curr.next;
//			return result;
//		}
//		@Override
//		public void remove(){
//			// NPE if prev==null; remove called before next.
//			LinkedMatrixCellSet.this.remove(prev.cell);
//		}
//	};
//
//}
