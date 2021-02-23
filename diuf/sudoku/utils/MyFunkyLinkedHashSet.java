/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
 * but without creating an identity-Ass, just his hashCode, which IS now
 * actually an Ass's identity anyways. Donkeys be warned.
 * <p>
 * All {@code Ass.equals} does is {@code hashCode==o.hashCode} because hashCode
 * contains the Ass's whole identity {@code isOn, cell.y, cell.x, and value}.
 * @param <T>
 */
public class MyFunkyLinkedHashSet<T> extends AbstractSet<T> implements IMySet<T> {

	// This implementation uses the wrapper pattern on this map.
	// WARN: put(K,V) does NOT replace existing values when key exists.
	private final MyFunkyLinkedHashMap<T,T> map;

	public MyFunkyLinkedHashSet(int initialCapacity, float loadFactor) {
		// isFunky=true makes the Map ignore attempts to replace existing keys
		this.map = new MyFunkyLinkedHashMap<>(initialCapacity, loadFactor);
	}

	public MyFunkyLinkedHashSet(Collection<? extends T> set) {
		this(set.size(), 0.75F);
		addAll(set);
	}

	@Override
	public boolean add(T o) {
		// java.util.LinkedHashMap.put replaces the existing value with the
		// given 'o' and returns the previous value, else null; whereas
		// MyFunkyLinkedHashMap.put (being funky) does not replace existing
		// values (only adds a new one) returning "Was it added?".
		// Takeaway: MyFunkyLinkedHashMap.put retains existing values!
		return map.put(o, o) == null; // put returns the prev value for key
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public T get(T o) {
		return map.get(o);
	}

	/**
	 * Get the Ass with the given hashCode from this Set.
	 * <p>
	 * WARN: map.getAss will throw type-cast-exceptions unless the generic
	 * type T is diuf.sudoku.solver.hinters.chain.Ass
	 * @param hashCode
	 * @return
	 */
	public T getAss(int hashCode) {
		return map.getAss(hashCode);
	}

	@Override
	public T poll() {
		MyHashMap.Entry<T,?> e = map.poll();
		return e==null ? null : e.key;
	}

	@Override
	public T remove() {
		if ( map.isEmpty() )
			throw new NoSuchElementException();
		return map.poll().key;
	}

	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		return (map.remove(o) != null);
	}

	@Override
	public int size() {
		return map.size;
	}

	/** do this.retainAll(c) then @return this.isEmpty(). */
	@Override
	public boolean retainAllIsEmpty(IMySet<T> c) {
		if ( map.size > 0 )
			for ( Iterator<T> it=map.keySet().iterator(); it.hasNext(); )
				if ( !c.contains(it.next()) )
					it.remove();
		return map.size==0;
	}

	/** do this.retainAll(c) and then c.clear(). */
	@Override
	public void retainAllClear(IMySet<T> c) {
		if ( map.size > 0 )
			for ( Iterator<T> it=map.keySet().iterator(); it.hasNext(); )
				if ( !c.remove(it.next()) )
					it.remove();
		c.clear(); // clean-up any left-overs... to be sure to be sure.
	}
}