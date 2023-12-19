/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * The funkyness in MyFunkyLinkedHashSet is that the add method is an addOnly
 * method, which does not replace existing elements and returns false if the
 * key is already in this Set.
 * <p>
 * This is a reflective set implementation like {@code java.util.LinkedHashSet}
 * but with a weird {@link #get(Object target)} method that returns the element
 * of the set that is equal to 'target'. This is useful when the {@code equals}
 * method compares just the identity (not all) fields, as per Ass(umption), to
 * quickly retrieve the-instance of a given identity.
 * <p>
 * The downside is creating billions of identity-Ass's to get less Ass, so now
 * {@code getAss(int hashCode)} exposed by {@code IAssSet}) does the same thing
 * but without creating an identity-Ass, just his hashCode, an Asses identity.
 * To be clear, the identity of an Ass, in {@code Ass#equals(Object)}, is its
 * hashCode, which uniquely identifies each Ass. This is every unusual. The
 * hashCode field is usually lossy, so much so that any exception to this rule
 * tends to be buggy simply because people expect stuff to work how they expect
 * it to work, so anything that works differently is wrong, hence needs to be
 * fixed, NOW! Thus the Lord provides us with religious ____wits: c__ts who are
 * too stupid to conceptualise there own stupidity. Sigh.
 * <p>
 * All {@code Ass.equals} does is {@code hashCode==o.hashCode} because hashCode
 * contains the Ass's whole identity {@code isOn, cell.y, cell.x, and value}.
 *
 * @param <E>
 */
public class MyFunkyLinkedHashSet<E> extends AbstractSet<E> implements IMySet<E> {

	// This implementation uses the wrapper pattern on this map.
	// WARN: put(K,V) does NOT replace existing values when key exists.
	protected final MyFunkyLinkedHashMap<E,E> map;

	public MyFunkyLinkedHashSet() {
		// isFunky=true makes the Map ignore attempts to replace existing keys
		this.map = new MyFunkyLinkedHashMap<>();
	}

	public MyFunkyLinkedHashSet(int initialCapacity, float loadFactor) {
		// isFunky=true makes the Map ignore attempts to replace existing keys
		this.map = new MyFunkyLinkedHashMap<>(initialCapacity, loadFactor);
	}

	public MyFunkyLinkedHashSet(Collection<? extends E> set) {
		this(set.size(), 0.75F);
		addAll(set);
	}

	@Override
	public boolean add(E e) {
		// java.util.LinkedHashMap.put replaces the existing value with the
		// given 'o' and returns the previous value, else null; whereas
		// MyFunkyLinkedHashMap.put (being funky) does not replace existing
		// values (only adds a new one) returning "Was it added?".
		// Takeaway: MyFunkyLinkedHashMap.put retains existing values!
		return map.put(e, e) == null; // put returns the prev value for key
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean contains(Object o) {
		// ignore IDE warning: Suspicious call
		return map.containsKey(o);
	}

	@Override
	public E get(E e) {
		return map.get(e);
	}

	/**
	 * Get the Ass with the given hashCode from this Set.
	 * <p>
	 * WARN: map.getAss will throw type-cast-exceptions unless the generic
	 * type T is diuf.sudoku.solver.hinters.chain.Ass
	 * @param hashCode
	 * @return
	 */
	public E getAss(int hashCode) {
		return map.getAss(hashCode);
	}

	@Override
	public E poll() {
		MyHashMap.Entry<E,?> e = map.poll();
		return e==null ? null : e.key;
	}

	@Override
	public E remove() {
		if ( map.isEmpty() ) {
			throw new NoSuchElementException();
		}
		return map.poll().key;
	}

	@Override
	public boolean remove(Object o) {
		return (map.remove(o) != null);
	}

	@Override
	public int size() {
		return map.size;
	}

	@Override
	public boolean any() {
		return map.size > 0;
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

}