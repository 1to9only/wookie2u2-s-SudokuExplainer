/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Ass;


/**
 * MyFunkyLinkedHashMap extends MyLinkedHashMap to enfunkonate the put method,
 * and get some Ass. Sigh.
 * <p>
 * Funky means that add does NOT replace the value of existing keys. If the
 * given key is already in this Map then it does nothing and returns false.
 *
 * @author Keith Corlett 2017
 * @param <K> Key type
 * @param <V> Value type
 */
public final class MyFunkyLinkedHashMap<K,V> extends MyLinkedHashMap<K,V> {

	private static final long serialVersionUID = 6976230105L;

	public MyFunkyLinkedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * MyFunkyLinkedHashMap#put differs from java.util.LinkedHashMap#put in
	 * its handling of pre-existing keys.
	 * <p>
	 * Funky means "add-or-(do-nothing-and-return-false); where<br>
	 * java.util does "add-or-(update-and-return-false)
	 * <p>
	 * This override does NOT replace the value of existing keys, only adds
	 * new ones, and returns "Was it added"; so that we can (atomically) use
	 * its return value to enqueue a pendingOn/Off to a Dequeue (LinkedList).
	 * <p>
	 * nb: this override necessitates copy-pasting java.util.LinkedHashMap,
	 * HashMap, and AbstractHashMap because stuff that should be protected
	 * like keySet, values, static hash(), and indexFor is package-private,
	 * negating the whole DRY ethos. Trust a programmer: validate UI.
	 *
	 * @param key the identifier used to retrieve the value
	 * @param value the value to associate with key
	 * @return true if added, false if the given key already exists (and the
	 *  associated value is not changed; where java.util updates it)
	*/
	@Override
	public V put(K key, V value) {
		assert key != null;
		final int hash = hash(key.hashCode());
		final int i = hash & (table.length-1); // truncate to table length.
		K k; // I wonder why java.util.LinkedHashMap uses Object k? I guesse
		     // because that's all that's actually REQUIRED. I think this is
			 // faster though, especially if K has an equals(K), but not sure.
		for ( MyHashMap.Entry<K,V> e=table[i]; e!=null; e=e.next )
			if ( e.hash==hash && ((k=e.key)==key || key.equals(k)) )
				return e.value; // Funky: do not update existing values!!!
		modCount++;
		addEntry(hash, key, value, i);
		return null;
	}

	/**
	 * Gets the Ass with the given hashCode from this Map, or null.
	 * <p>
	 * Note that getAss only works on a Map of Ass. Trying to getAss from
	 * a Collection of Sheep is nonsensical, so leave it to the Kiwis.
	 * <pre>
	 * WARN: This method will throw type-cast-exceptions unless the
	 *     generic type K is-or-extends {@link diuf.sudoku.Ass}
	 * NB: This method need only be package-visible coz it's exposed by
	 *     {@link MyFunkyLinkedHashSet#getAss}, which is public.
	 * NB: requires Ass.hashCode field be public rather than wear the cost
	 *     of invoking hashCode() for each entry in this bucket.
	 *     EVERY invocation does you damage.
	 * </pre>
	 *
	 * @param hashCode your ass.hashCode (see {@link Ass#hashCode}.
	 */
	V getAss(final int hashCode) {
		// nb: hash in-line was slower. Why I know not.
		final int hash = hash(hashCode);
		final int i = hash & (table.length-1);
		for ( MyHashMap.Entry<K,V> e=table[i]; e!=null; e=e.next )
			// note that Ass.equals method just equates hashCodes
			if ( e.hash==hash && ((Ass)e.key).hashCode==hashCode )
				return e.value;
		return null;
	}

}
