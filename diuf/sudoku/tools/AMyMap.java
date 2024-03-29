/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.utils.Frmt.EQUALS;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class provides a skeletal implementation of the <tt>Map</tt>
 * interface, to minimize the effort required to implement this interface.
 *
 * <p>Having copy-pasted java.util.HashMap to MyHashMap I find I also need
 * to copy-paste the java.util.AbstractMap because the keySet and values
 * variables are package-visible instead of protected which IMHO they should
 * be. Oh well, let the hack-fest continue!
 *
 * <p>To implement an unmodifiable map, the programmer needs only to extend this
 * class and provide an implementation for the <tt>entrySet</tt> method, which
 * returns a set-view of the maps mappings.  Typically, the returned set
 * will, in turn, be implemented atop <tt>AbstractSet</tt>.  This set should
 * not support the <tt>add</tt> or <tt>remove</tt> methods, and its iterator
 * should not support the <tt>remove</tt> method.
 *
 * <p>To implement a modifiable map, the programmer must additionally override
 * this classs <tt>put</tt> method (which otherwise throws an
 * <tt>UnsupportedOperationException</tt>), and the iterator returned by
 * <tt>entrySet().iterator()</tt> must additionally implement its
 * <tt>remove</tt> method.
 *
 * <p>The programmer should generally provide a void (no argument) and map
 * constructor, as per the recommendation in the <tt>Map</tt> interface
 * specification.
 *
 * <p>The documentation for each non-abstract method in this class describes its
 * implementation in detail.  Each of these methods may be overridden if the
 * map being implemented admits a more efficient implementation.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see Map
 * @see Collection
 * @since 1.2
 */

@SuppressWarnings("unchecked")
public abstract class AMyMap<K,V> implements java.util.Map<K,V> {
	/**
	 * Sole constructor.  (For invocation by subclass constructors, typically
	 * implicit.)
	 */
	protected AMyMap() {
	}

	// Query Operations

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation returns <tt>entrySet().size()</tt>.
	 */
	@Override
	public int size() {
		return entrySet().size();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation returns <tt>size() == 0</tt>.
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation returns <tt>size() > 0</tt>.
	 */
	public boolean any() {
		return size() > 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation iterates over <tt>entrySet()</tt> searching
	 * for an entry with the specified value.  If such an entry is found,
	 * <tt>true</tt> is returned.  If the iteration terminates without
	 * finding such an entry, <tt>false</tt> is returned.  Note that this
	 * implementation requires linear time in the size of the map.
	 *
	 * @throws ClassCastException   {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (value==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getValue()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (value.equals(e.getValue()))
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation iterates over <tt>entrySet()</tt> searching
	 * for an entry with the specified key.  If such an entry is found,
	 * <tt>true</tt> is returned.  If the iteration terminates without
	 * finding such an entry, <tt>false</tt> is returned.  Note that this
	 * implementation requires linear time in the size of the map; many
	 * implementations will override this method.
	 *
	 * @throws ClassCastException   {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		Iterator<Map.Entry<K,V>> i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation iterates over <tt>entrySet()</tt> searching
	 * for an entry with the specified key.  If such an entry is found,
	 * the entrys value is returned.  If the iteration terminates without
	 * finding such an entry, <tt>null</tt> is returned.  Note that this
	 * implementation requires linear time in the size of the map; many
	 * implementations will override this method.
	 *
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 */
	@Override
	public V get(Object key) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (key==null) {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					return e.getValue();
			}
		} else {
			while (i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					return e.getValue();
			}
		}
		return null;
	}


	// Modification Operations

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation always throws an
	 * <tt>UnsupportedOperationException</tt>.
	 *
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 * @throws IllegalArgumentException      {@inheritDoc}
	 */
	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation iterates over <tt>entrySet()</tt> searching for an
	 * entry with the specified key.  If such an entry is found, its value is
	 * obtained with its <tt>getValue</tt> operation, the entry is removed
	 * from the collection (and the backing map) with the iterators
	 * <tt>remove</tt> operation, and the saved value is returned.  If the
	 * iteration terminates without finding such an entry, <tt>null</tt> is
	 * returned.  Note that this implementation requires linear time in the
	 * size of the map; many implementations will override this method.
	 *
	 * <p>Note that this implementation throws an
	 * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
	 * iterator does not support the <tt>remove</tt> method and this map
	 * contains a mapping for the specified key.
	 *
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 */
	@Override
	public V remove(Object key) {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		Entry<K,V> correctEntry = null;
		if (key==null) {
			while (correctEntry==null && i.hasNext()) {
				Entry<K,V> e = i.next();
				if (e.getKey()==null)
					correctEntry = e;
			}
		} else {
			while (correctEntry==null && i.hasNext()) {
				Entry<K,V> e = i.next();
				if (key.equals(e.getKey()))
					correctEntry = e;
			}
		}
		V oldValue = null;
		if (correctEntry !=null) {
			oldValue = correctEntry.getValue();
			i.remove();
		}
		return oldValue;
	}


	// Bulk Operations

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation iterates over the specified maps
	 * <tt>entrySet()</tt> collection, and calls this maps <tt>put</tt>
	 * operation once for each entry returned by the iteration.
	 *
	 * <p>Note that this implementation throws an
	 * <tt>UnsupportedOperationException</tt> if this map does not support
	 * the <tt>put</tt> operation and the specified map is nonempty.
	 *
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws ClassCastException            {@inheritDoc}
	 * @throws NullPointerException          {@inheritDoc}
	 * @throws IllegalArgumentException      {@inheritDoc}
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation calls <tt>entrySet().clear()</tt>.
	 *
	 * <p>Note that this implementation throws an
	 * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
	 * does not support the <tt>clear</tt> operation.
	 *
	 * @throws UnsupportedOperationException {@inheritDoc}
	 */
	@Override
	public void clear() {
		entrySet().clear();
	}


	// Views

	/**
	 * Each of these fields are initialized to contain an instance of the
	 * appropriate view the first time this view is requested.  The views are
	 * stateless, so there is no reason to create more than one of each.
	 */
	protected transient volatile Set<K>        keySet = null;
	protected transient volatile Collection<V> values = null;

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation returns a set that subclasses {@link AbstractSet}.
	 * The subclasss iterator method returns a "wrapper object" over this
	 * maps <tt>entrySet()</tt> iterator.  The <tt>size</tt> method
	 * delegates to this maps <tt>size</tt> method and the
	 * <tt>contains</tt> method delegates to this maps
	 * <tt>containsKey</tt> method.
	 *
	 * <p>The set is created the first time this method is called,
	 * and returned in response to all subsequent calls.  No synchronization
	 * is performed, so there is a slight chance that multiple calls to this
	 * method will not all return the same set.
	 */
	@Override
	public Set<K> keySet() {
		if (keySet == null) {
			keySet = new AbstractSet<K>() {
				@Override
				public int size() { return AMyMap.this.size(); }
				@Override
				public boolean isEmpty() { return AMyMap.this.isEmpty(); }
				@Override
				public void clear() { AMyMap.this.clear(); }
				@Override
				public boolean contains(Object k) { return AMyMap.this.containsKey(k); }
				@Override
				public Iterator<K> iterator() {
					return new Iterator<K>() {
						private Iterator<Entry<K,V>> i = entrySet().iterator();
						@Override
						public boolean hasNext() { return i.hasNext(); }
						@Override
						public K next() { return i.next().getKey(); }
						@Override
						public void remove() { i.remove(); }
					};
				}
			};
		}
		return keySet;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation returns a collection that subclasses {@link
	 * AbstractCollection}.  The subclasss iterator method returns a
	 * "wrapper object" over this maps <tt>entrySet()</tt> iterator.
	 * The <tt>size</tt> method delegates to this maps <tt>size</tt>
	 * method and the <tt>contains</tt> method delegates to this maps
	 * <tt>containsValue</tt> method.
	 *
	 * <p>The collection is created the first time this method is called, and
	 * returned in response to all subsequent calls.  No synchronization is
	 * performed, so there is a slight chance that multiple calls to this
	 * method will not all return the same collection.
	 */
	@Override
	public Collection<V> values() {
		if (values == null) {
			values = new AbstractCollection<V>() {
				@Override
				public Iterator<V> iterator() {
					return new Iterator<V>() {
						private Iterator<Entry<K,V>> i = entrySet().iterator();
						@Override
						public boolean hasNext() { return i.hasNext(); }
						@Override
						public V next() { return i.next().getValue(); }
						@Override
						public void remove() { i.remove(); }
					};
				}
				@Override
				public int size() { return AMyMap.this.size(); }
				@Override
				public boolean isEmpty() { return AMyMap.this.isEmpty(); }
				@Override
				public void clear() { AMyMap.this.clear(); }
				@Override
				public boolean contains(Object v) { return AMyMap.this.containsValue(v); }
			};
		}
		return values;
	}

	@Override
	public abstract Set<Entry<K,V>> entrySet();


	// Comparison and hashing

	/**
	 * Compares the specified object with this map for equality.  Returns
	 * <tt>true</tt> if the given object is also a map and the two maps
	 * represent the same mappings.  More formally, two maps <tt>m1</tt> and
	 * <tt>m2</tt> represent the same mappings if
	 * <tt>m1.entrySet().equals(m2.entrySet())</tt>.  This ensures that the
	 * <tt>equals</tt> method works properly across different implementations
	 * of the <tt>Map</tt> interface.
	 *
	 * <p>This implementation first checks if the specified object is this map;
	 * if so it returns <tt>true</tt>.  Then, it checks if the specified
	 * object is a map whose size is identical to the size of this map; if
	 * not, it returns <tt>false</tt>.  If so, it iterates over this maps
	 * <tt>entrySet</tt> collection, and checks that the specified map
	 * contains each mapping that this map contains.  If the specified map
	 * fails to contain such a mapping, <tt>false</tt> is returned.  If the
	 * iteration completes, <tt>true</tt> is returned.
	 *
	 * @param o object to be compared for equality with this map
	 * @return <tt>true</tt> if the specified object is equal to this map
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Map))
			return false;
		Map<K,V> other = (Map<K,V>) o;
		if (other.size() != size())
			return false;
		try {
			Iterator<Entry<K,V>> it = entrySet().iterator();
			while (it.hasNext()) {
				Entry<K,V> e = it.next();
				K key = e.getKey();
				V value = e.getValue();
				if (value == null) {
					if ( !(other.get(key)==null && other.containsKey(key)) )
						return false;
				} else {
					if ( !value.equals(other.get(key)) )
						return false;
				}
			}
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the hash code value for this map.  The hash code of a map is
	 * defined to be the sum of the hash codes of each entry in the maps
	 * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
	 * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
	 * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
	 * {@link Object#hashCode}.
	 *
	 * <p>This implementation iterates over <tt>entrySet()</tt>, calling
	 * {@link Map.Entry#hashCode hashCode()} on each element (entry) in the
	 * set, and adding up the results.
	 *
	 * @return the hash code value for this map
	 * @see Map.Entry#hashCode()
	 * @see Object#equals(Object)
	 * @see Set#equals(Object)
	 */
	@Override
	public int hashCode() {
		int h = 0;
		Iterator<Entry<K,V>> i = entrySet().iterator();
		while (i.hasNext())
			h += i.next().hashCode();
		return h;
	}

	/**
	 * Returns a string representation of this map.  The string representation
	 * consists of a list of key-value mappings in the order returned by the
	 * maps <tt>entrySet</tt> views iterator, enclosed in braces
	 * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
	 * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
	 * the key followed by an equals sign (<tt>"="</tt>) followed by the
	 * associated value.  Keys and values are converted to strings as by
	 * {@link String#valueOf(Object)}.
	 *
	 * @return a string representation of this map
	 */
	@Override
	public String toString() {
		final Set<Entry<K,V>> set = entrySet();
		final Iterator<Entry<K,V>> it = set.iterator();
		if ( !it.hasNext() )
			return "{}";
		final StringBuilder sb = SB(Math.min(set.size()<<3,4096));
		sb.append('{');
		for (;;) {
			final Entry<K,V> e = it.next();
			final K key = e.getKey();
			if ( key == this )
				sb.append("(this Map)");
			else
				sb.append(key);
			sb.append('=');
			final V value = e.getValue();
			if ( value == this )
				sb.append("(this Map)");
			else
				sb.append(value);
			if ( !it.hasNext() )
				return sb.append('}').toString();
			sb.append(',').append(' ');
		}
	}

	/**
	 * Returns a shallow copy of this <tt>MyAbstractMap</tt> instance: the keys
	 * and values themselves are not cloned.
	 *
	 * @return a shallow copy of this map
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		AMyMap<K,V> result = (AMyMap<K,V>)super.clone();
		result.keySet = null;
		result.values = null;
		return result;
	}

	/**
	 * Utility method for SimpleEntry and SimpleImmutableEntry.
	 * Test for equality, checking for nulls.
	 */
	private static boolean eq(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	// Implementation Note: SimpleEntry and SimpleImmutableEntry
	// are distinct unrelated classes, even though they share
	// some code. Since you cannot add or subtract final-ness
	// of a field in a subclass, they cannot share representations,
	// and the amount of duplicated code is too small to warrant
	// exposing a common abstract class.

	/**
	 * An Entry maintaining a key and a value.  The value may be
	 * changed using the <tt>setValue</tt> method.  This class
	 * facilitates the process of building custom map
	 * implementations. For example, it may be convenient to return
	 * arrays of <tt>SimpleEntry</tt> instances in method
	 * <tt>Map.entrySet().toArray</tt>.
	 *
	 * @since 1.6
	 */
	public static final class SimpleEntry<K,V>
		implements Entry<K,V>, java.io.Serializable {

		private static final long serialVersionUID = -8499721149061103585L;

		private final K key;
		private V value;

		/**
		 * Creates an entry representing a mapping from the specified
		 * key to the specified value.
		 *
		 * @param key the key represented by this entry
		 * @param value the value represented by this entry
		 */
		public SimpleEntry(K key, V value) {
			this.key   = key;
			this.value = value;
		}

		/**
		 * Creates an entry representing the same mapping as the
		 * specified entry.
		 *
		 * @param entry the entry to copy
		 */
		public SimpleEntry(Entry<? extends K, ? extends V> entry) {
			this.key   = entry.getKey();
			this.value = entry.getValue();
		}

		/**
		 * Returns the key corresponding to this entry.
		 *
		 * @return the key corresponding to this entry
		 */
		@Override
		public K getKey() {
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.
		 *
		 * @return the value corresponding to this entry
		 */
		@Override
		public V getValue() {
			return value;
		}

		/**
		 * Replaces the value corresponding to this entry with the specified
		 * value.
		 *
		 * @param value new value to be stored in this entry
		 * @return the old value corresponding to the entry
		 */
		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		/**
		 * Compares the specified object with this entry for equality.
		 * Returns {@code true} if the given object is also a map entry and
		 * the two entries represent the same mapping.  More formally, two
		 * entries {@code e1} and {@code e2} represent the same mapping
		 * if<pre>
		 *   (e1.getKey()==null ?
		 *    e2.getKey()==null :
		 *    e1.getKey().equals(e2.getKey()))
		 *   &amp;&amp;
		 *   (e1.getValue()==null ?
		 *    e2.getValue()==null :
		 *    e1.getValue().equals(e2.getValue()))</pre>
		 * This ensures that the {@code equals} method works properly across
		 * different implementations of the {@code Map.Entry} interface.
		 *
		 * @param o object to be compared for equality with this map entry
		 * @return {@code true} if the specified object is equal to this map
		 *         entry
		 * @see    #hashCode
		 */
		@Override
        @SuppressWarnings("rawtypes")
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			return eq(key, e.getKey()) && eq(value, e.getValue());
		}

		/**
		 * Returns the hash code value for this map entry.  The hash code
		 * of a map entry {@code e} is defined to be: <pre>
		 *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
		 *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
		 * This ensures that {@code e1.equals(e2)} implies that
		 * {@code e1.hashCode()==e2.hashCode()} for any two Entries
		 * {@code e1} and {@code e2}, as required by the general
		 * contract of {@link Object#hashCode}.
		 *
		 * @return the hash code value for this map entry
		 * @see    #equals
		 */
		@Override
		public int hashCode() {
			return (key   == null ? 0 :   key.hashCode()) ^
				   (value == null ? 0 : value.hashCode());
		}

		/**
		 * Returns a String representation of this map entry.  This
		 * implementation returns the string representation of this
		 * entrys key followed by the equals character ("<tt>=</tt>")
		 * followed by the string representation of this entrys value.
		 *
		 * @return a String representation of this map entry
		 */
		@Override
		public String toString() {
			return key + EQUALS + value;
		}

	}

	/**
	 * An Entry maintaining an immutable key and value.  This class
	 * does not support method <tt>setValue</tt>.  This class may be
	 * convenient in methods that return thread-safe snapshots of
	 * key-value mappings.
	 *
	 * @since 1.6
	 */
	public static final class SimpleImmutableEntry<K,V>
		implements Entry<K,V>, java.io.Serializable {

		private static final long serialVersionUID = 7138329143949025153L;

		private final K key;
		private final V value;

		/**
		 * Creates an entry representing a mapping from the specified
		 * key to the specified value.
		 *
		 * @param key the key represented by this entry
		 * @param value the value represented by this entry
		 */
		public SimpleImmutableEntry(K key, V value) {
			this.key   = key;
			this.value = value;
		}

		/**
		 * Creates an entry representing the same mapping as the
		 * specified entry.
		 *
		 * @param entry the entry to copy
		 */
		public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
			this.key   = entry.getKey();
			this.value = entry.getValue();
		}

		/**
		 * Returns the key corresponding to this entry.
		 *
		 * @return the key corresponding to this entry
		 */
		@Override
		public K getKey() {
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.
		 *
		 * @return the value corresponding to this entry
		 */
		@Override
		public V getValue() {
			return value;
		}

		/**
		 * Replaces the value corresponding to this entry with the specified
		 * value (optional operation).  This implementation simply throws
		 * <tt>UnsupportedOperationException</tt>, as this class implements
		 * an <i>immutable</i> map entry.
		 *
		 * @param value new value to be stored in this entry
		 * @return (Does not return)
		 * @throws UnsupportedOperationException always
		 */
		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Compares the specified object with this entry for equality.
		 * Returns {@code true} if the given object is also a map entry and
		 * the two entries represent the same mapping.  More formally, two
		 * entries {@code e1} and {@code e2} represent the same mapping
		 * if<pre>
		 *   (e1.getKey()==null ?
		 *    e2.getKey()==null :
		 *    e1.getKey().equals(e2.getKey()))
		 *   &amp;&amp;
		 *   (e1.getValue()==null ?
		 *    e2.getValue()==null :
		 *    e1.getValue().equals(e2.getValue()))</pre>
		 * This ensures that the {@code equals} method works properly across
		 * different implementations of the {@code Map.Entry} interface.
		 *
		 * @param o object to be compared for equality with this map entry
		 * @return {@code true} if the specified object is equal to this map
		 *         entry
		 * @see    #hashCode
		 */
		@Override
        @SuppressWarnings("rawtypes")
		public boolean equals(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry e = (Map.Entry)o;
			return eq(key, e.getKey()) && eq(value, e.getValue());
		}

		/**
		 * Returns the hash code value for this map entry.  The hash code
		 * of a map entry {@code e} is defined to be: <pre>
		 *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
		 *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
		 * This ensures that {@code e1.equals(e2)} implies that
		 * {@code e1.hashCode()==e2.hashCode()} for any two Entries
		 * {@code e1} and {@code e2}, as required by the general
		 * contract of {@link Object#hashCode}.
		 *
		 * @return the hash code value for this map entry
		 * @see    #equals
		 */
		@Override
		public int hashCode() {
			return (key   == null ? 0 :   key.hashCode()) ^
				   (value == null ? 0 : value.hashCode());
		}

		/**
		 * Returns a String representation of this map entry.  This
		 * implementation returns the string representation of this
		 * entrys key followed by the equals character ("<tt>=</tt>")
		 * followed by the string representation of this entrys value.
		 *
		 * @return a String representation of this map entry
		 */
		@Override
		public String toString() {
			return key + EQUALS + value;
		}

	}

}
