/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.ON_BIT;
import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.solver.hinters.FunkyAssSet;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.IMyPollSet;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * LinkedMatrixAssSet (an IAssSet) extends {@code AbstractSet<Ass>} to provide
 * high-performance contains and remove methods, at the cost of high RAM usage
 * which causes slow construction (we create a 9k array) so construct me as
 * little as possible: clear-and-reuse me instead.
 * <p>
 * NOTE: LinkedMatrixAssSet is "funky" which means its add method is addOnly;
 * that is add does NOT update existing Asses, it just returns false as per the
 * {@link FunkyAssSet} specification (demurring from Collections).
 * <p>
 * The getAss method returns the Ass with parents, which is and always will be
 * the first one added. Any attempt to replace that Ass with another (which
 * almost certainly has different heritage) does nothing, and returns false.
 * <p>
 * This differs from java.util.Set implementations which replace the existing
 * entry with the offered one, and return false, which is, IMHO, wrong! To me
 * false means the collection has not changed, so returning false when the
 * collection has actually been modified is a lie. But there are other ways to
 * look at it. The Collections developers valued number of elements changed
 * over collection modified, hence the update operation of the add method is
 * ignored, because the number of elements in the collection has not changed,
 * despite the fact the collection HAS been modified. Sigh.
 * <p>
 * Features are:<ul>
 * <li>contains and remove methods are fast O(1) as apposed to the slow O(1) of
 *  a {@code HashSet<Ass>} such as {@link FunkyAssSet}.</li>
 * <li>a fast iterator (for an iterator, which are all slow compared to Javas
 *  array-iterator, mainly because of two methods per element).</li>
 * <li>clear is O(size) as apposed to arrays O(capacity).</li>
 * <li>WARN: Construction is pretty slow thanks to the 9,270 byte matrix, so
 *  do NOT create me OFTEN, or MANY. Never in a tight loop!</li>
 * </ul>
 * KRC 2020-10-23 moved LinkedMatrixAssSet from diuf.sudoku.utils coz nothing
 * with project-types (except Debug) should be in utils (to keep utils portable
 * between projects); I am now used in both chain and fish packages; so not
 * sure where I belong, so I am in the hinters package, above both of them.
 */
public final class LinkedMatrixAssSet extends AbstractSet<Ass> implements IAssSet, IMyPollSet<Ass> {

	public boolean isOn;

	int size;
	Node head, foot;

	// nodes indexes: [indice 0..80][value 1..9]
	// package visible for Chainer* DIY funky test
	Node[][] nodes;

	public LinkedMatrixAssSet() {
		nodes = new Node[GRID_SIZE][VALUE_CEILING];
	}

	@Override
	public boolean isEmpty() {
		return head == null;
	}

	@Override
	public boolean any() {
		return head != null;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * The contains(Ass) method is specified by IAssSet. Returns true if this
	 * Set contains the given ass, else false. Note that the given Ass must not
	 * be null. A null Ass will cause a NullPointerException.
	 * <p>
	 * I am trying to make my generic collections (none of which can contain a
	 * null Ass) to use contains(T) instead of contains(Object) and I am rapidly
	 * discovering why the Java collections framework uses contains(Object).
	 *
	 * @param a the Ass to examine (We're gonna need a bigger probe)
	 * @return true if ass is in this Set, else false
	 */
	@Override
	public boolean contains(final Ass a) {
		return nodes[a.indice][a.value] != null;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public boolean contains(final Object o) {
		return o instanceof Ass && nodes[((Ass)o).indice][((Ass)o).value] != null;
	}

	/**
	 * Get the full Assumption (with parents) from this Set.
	 *
	 * @param a identity-only (sans parents) Ass to fetch.
	 * @return the full Ass with parents.
	 */
	@Override
	public Ass get(final Ass a) {
		return getAss(a.indice, a.value);
	}

	/**
	 * getAss implements IAssSet.getAss.
	 * <p>WARN this implementation relies upon all Asses in this set having the
	 *  same isOn, which happily is how it is "naturally" in Chainer.
	 *
	 * @param indice the index of the cell Grid.cells
	 * @param value the potential value you seek
	 * @return the Ass if it is present, else null
	 */
	@Override
	public Ass getAss(final int indice, final int value) {
		final Node n = nodes[indice][value];
		if ( n != null )
			return n.ass;
		return null;
	}

	/**
	 * getAss implements IAssSet.getAss.
	 * <p>
	 * WARN this implementation relies upon all Asses in this set having the
	 * same isOn, which happily is how it is "naturally" in Chainer.
	 *
	 * @param cell Cell from which we take just the x and y coordinates.
	 * @param value the potential value you seek
	 * @return the Ass if it is present, else null
	 */
	@Override
	public Ass getAss(final Cell cell, final int value) {
		return getAss(cell.indice, value);
	}

	/**
	 * getAss implements IAssSet.getAss.
	 * <p>
	 * WARN this implementation relies upon all Asses in this set having the
	 * same isOn, which happily is how it is "naturally" in Chainer.
	 * <p>
	 * The isOn field must be pre-set to the "On-ness" of this Set in order for
	 * getAss(hashCode) to work. It is false by default. To the best of my
	 * knowledge getAss(hashCode) is NEVER used on a LinkedMatrixAssSet, only
	 * on FunkyAssSet (HashCode-based) where it's a natural.
	 * <p>
	 * getAss(hashCode) is the only way to get an On from a Set of Offs, but in
	 * an LMAS you must set isOn true, possibly temporarily. This situation is
	 * is a result of making both implementations implement a common interface.
	 * It is NOT ideal. It just is what it is, and hopefully it can be made to
	 * work anyway.
	 *
	 * @param hashCode
	 * @return the Ass if it is present, else null
	 */
	@Override
	public Ass getAss(final int hashCode) {
		if ( (hashCode & ON_BIT) > 0 == isOn )
			// FYI this code is untested, hence
			// Ass.indiceOf and Ass.valueOf are untested!
			return getAss(Ass.indiceOf(hashCode), Ass.valueOf(hashCode));
		return null;
	}

	/**
	 * The justAdd method just adds. YOU use the public nodes field to check
	 * that Ass doesnt already exist, which is faster sans invocation, and if
	 * so then you justAdd. DIY funkyness faster coz it reduces invocations.
	 *
	 * @param a the Ass to just add
	 * @return the given ass (a new one) so that my caller can enqueue it.
	 */
	Ass justAdd(final Ass a) {
		if ( head == null ) // empty
			head = foot = nodes[a.indice][a.value] = new Node(null, a, null);
		else
			foot = foot.next = nodes[a.indice][a.value] = new Node(foot, a, null);
		++size;
//		assert count() == size;
		return a;
	}

	/**
	 * Add the given Ass to this Set, but funky (ie addOnly) meaning a request
	 * to add an Ass that already exists in this Set just returns false, unlike
	 * java.util.Set implementations which overwrite the existing Set-element
	 * with the given value and then return false. Not me your honour!
	 *
	 * @param a to add
	 * @return was a added?
	 */
	@Override
	public boolean add(final Ass a) {
		return nodes[a.indice][a.value] == null // funky
			&& justAdd(a) != null;
	}

	/**
	 * addAll(LMAS) overloads {@link java.util.Set#addAll(Collection) } for
	 * speed. LMAS to LMAS can be done faster.
	 *
	 * @param src to add
	 * @return any added
	 */
	boolean addAll(final LinkedMatrixAssSet src) {
		if ( src.head == null ) // empty src
			return false;
		if ( head == null ) { // dst is empty, so no need for funky test
			// you cant set foot.next on first item, so do seperately
			Node s = src.head;
			head = foot = nodes[s.ass.indice][s.ass.value] = new Node(null, s.ass, null);
			size = 1;
			for ( s=s.next; s!=null; s=s.next ) {
				foot = foot.next = nodes[s.ass.indice][s.ass.value] = new Node(foot, s.ass, null);
				++size;
			}
			return true;
		} else {
			// The funky test means we know not which is the first item until
			// we find the first item, hence every single bloody item requires
			// both the funky test AND the empty test, and for reasons I do not
			// really understand, I find the empty test on every item really
			// ____ing annoying. Simply: There MUST be a better way. But it
			// matters not in SE, which always calls addAll on an empty LMAS.
			Ass a; // item
			boolean modified = false;
			for ( Node s=src.head; s!=null; s=s.next ) {
				if ( nodes[(a=s.ass).indice][a.value] == null ) { // funky test
					if ( head == null ) // empty
						head = foot = nodes[a.indice][a.value] = new Node(null, a, null);
					else
						foot = foot.next = nodes[a.indice][a.value] = new Node(foot, a, null);
					++size;
					modified = true;
				}
			}
			return modified;
		}
	}

	/**
	 * remove overloads {@link java.util.Set#remove(Object)}, for speed.
	 *
	 * @param a
	 * @return
	 */
	Ass remove(final Ass a) {
		final Node n = nodes[a.indice][a.value];
		if ( n == null )
			return null;
		nodes[a.indice][a.value] = null;
		if ( n == head ) {
			if ( n == foot ) { // empty
				head = foot = null;
				size = 0;
			} else { // first
				final Node x = n.next;
				if ( x == null )
					foot = null;
				else
					x.prev = null;
				head = x;
				--size;
			}
		} else { // subsequent
			// p is not null coz n is not head
			// x is null if n is the foot
			final Node p=n.prev, x=n.next;
			p.next = x;
			if ( x == null ) // n (the last) is a goner
				foot = p;
			else
				x.prev = p;
			--size;
		}
//		assert count() == size;
		return n.ass;
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
		if(head == null) throw new NoSuchElementException();
		return remove(head.ass);
	}

	@Override
	public boolean remove(final Object o) {
		return o instanceof Ass && remove((Ass)o)!=null;
	}

	/**
	 * @return remove and return the first Ass, else null means empty Set.
	 */
	@Override
	public Ass poll() {
		if ( head != null )
			return remove(head.ass);
		else
			return null;
	}

	@Override
	public void pollAll(final IMyPollSet<Ass> src) {
		Ass a;
		while ( (a=src.poll()) != null )
			add(a);
	}

	/**
	 * pollAll(LMAS) overloads {@link IMyPollSet#pollAll} for speed. LMAS to
	 * LMAS can be done faster.
	 * <p>
	 * When this set is empty upon entry into pollAll, I simply swap nodes with
	 * src. As it happens, pollAll is called ONLY when this Set is empty, which
	 * is fast 0(1); but non-empty is slow O(n) thanks to the funky test to not
	 * update existing elements.
	 * <p>
	 * When pollAll is complete, this Set contains all elements in src, and
	 * src is empty.
	 *
	 * @param src the Asses to move into this Set, which is (by change) always
	 *  empty when pollAll is invoked; so pollAll is fast only on empty-sets
	 */
	public void pollAll(final LinkedMatrixAssSet src) {
		if ( src.head == null ) // empty source
			return;
		if ( head == null ) { // empty, no funky test
			// Swap nodes with src. This O(1) operation is performant, but it
			// requires nodes to be NOT final, and its a public field, so I am
			// choosing to trust my caller, which is me, so not an issue. Dont
			// do this in a real system, with random techies, esp noobs. It'll
			// almost certainly go bad.
			assert foot == null;
			assert size == 0;
			head = src.head;
			foot = src.foot;
			size = src.size;
			final Node[][] tmp = src.nodes;
			src.nodes = nodes;
			nodes = tmp;
		} else { // funky test is required
			// not used: pollAll is called only on empty sets, hence this impl
			// is nonperformant, which is NOT a problem for me.
			for ( Node s=src.head; s!=null; s=s.next ) {
				src.nodes[s.ass.indice][s.ass.value] = null;
				add(s.ass); // funky
			}
		}
		src.head = src.foot = null;
		src.size = 0;
	}

	@Override
	public void clear() {
		if ( head != null ) {
			for ( Node n=head; n!=null; n=n.next )
				nodes[n.ass.indice][n.ass.value] = null;
			head = foot = null;
			size = 0;
//			assert count() == 0;
		}
	}

	/**
	 * retainAll(LMAS.Node[][]) overloads {@code retainAll(Collection<?>)}
	 * for speed.
	 *
	 * @param keeperNodes LMAS.nodes of the LMAS to retain
	 * @return modified (which I think should be isEmpty)
	 */
	public boolean retainAll(final Node[][] keeperNodes) {
		if ( head == null ) // empty
			return false;
		Ass a;
		Node p, x; // prev, next
        boolean modified = false;
		for ( Node n=head; ; ) {
			if ( keeperNodes[(a=n.ass).indice][a.value] == null ) {
				// inlined remove(a)
				nodes[a.indice][a.value] = null;
				if ( n == head ) {
					if ( n == foot ) { // now empty
						head = foot = null;
						size = 0;
						return true;
					} else { // remove first
						x = n.next;
						if ( x == null )
							foot = null;
						else
							x.prev = null;
						head = x;
						--size;
					}
				} else {
					(p=n.prev).next = x = n.next;
					if ( x == null ) // n (the last) is a goner
						foot = p;
					else
						x.prev = p;
					--size;
				}
				modified = true;
			}
			if((n=n.next)==null) break;
		}
//		assert count() == size;
		return modified;
	}

	boolean retainAllAny(final LinkedMatrixAssSet keepers) {
		if ( head == null ) // empty
			return false;
		final Node[][] keeperNodes = keepers.nodes;
		for ( Node n=head; ; ) {
			final Ass a = n.ass;
			if ( keeperNodes[a.indice][a.value] == null ) {
				// inlined this.remove(a)
				nodes[a.indice][a.value] = null;
				if ( n == head ) {
					if ( n == foot ) { // empty
						head = foot = null;
						size = 0;
						return false;
					} else { // first
						final Node x = n.next; // prev, next
						if ( x == null )
							foot = null;
						else
							x.prev = null;
						head = x;
						--size;
					}
				} else {
					final Node p = n.prev, x; // prev, next
					p.next = x = n.next;
					if ( x == null ) // n (the last) is a goner
						foot = p;
					else
						x.prev = p;
					--size;
				}
			}
			if((n=n.next)==null) break;
		}
		return head != null; // any
	}

	/**
	 * For asserts, debug and test-cases (package visible).
	 *
	 * @return the number of nodes that are not null
	 */
	int count() {
		int count = 0;
		for ( int v,i=0; i<GRID_SIZE; ++i )
			for ( v=1; v<VALUE_CEILING; ++v )
				if ( nodes[i][v] != null )
					++count;
		return count;
	}

	/**
	 * For debug and test-cases (package visible).
	 *
	 * @return A String of the nodes that are not null.
	 */
	static String dump(final Node[][] nodes) {
		final StringBuilder sb = SB(nodes.length<<4); // * 16
		for ( int v,i=0; i<GRID_SIZE; ++i )
			for ( v=1; v<VALUE_CEILING; ++v )
				if ( nodes[i][v] != null )
//					sb.append(", ").append(i).append(".").append(v)
//					  .append("=").append(nodes[i][v]);
					sb.append(", ").append(nodes[i][v]);
		// skip leading ", " if any
		return sb.substring(Math.min(2, sb.length()));
	}

	String dump() {
		return dump(this.nodes);
	}

	@Override
	public Iterator<Ass> iterator() {
		return new MyIterator();
	}

	private class MyIterator implements Iterator<Ass> {
		private Node prev = null;
		private Node curr = head;
		@Override
		public boolean hasNext(){
			return curr != null;
		}
		@Override
		public Ass next(){
			final Ass result = curr.ass;
			prev = curr;
			curr = curr.next;
			return result;
		}
		@Override
		public void remove(){
			LinkedMatrixAssSet.this.remove(prev.ass);
		}
	}

	// a node in a doubly-linked list of Asses
	public static final class Node {
		public Node prev;
		public final Ass ass;
		public Node next;
		Node(final Node prev, final Ass ass, final Node next) {
			this.prev = prev;
			this.ass = ass;
			this.next = next;
		}
		@Override
		public String toString() {
			return (prev==null?"#":"<") + ass.toString() + (next==null?"#":">");
		}
	}

}
