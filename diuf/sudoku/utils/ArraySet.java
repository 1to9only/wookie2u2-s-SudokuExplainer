/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * ArraySet implements {@code Set<T>} using an array of Object. Unlike other
 * implementations of java.util.Set, this one does NOT enforce uniqueness, so
 * it's only useful if you already have distinct elements to add to it. On the
 * bright side the {@link #array} field is public, as is the {@link #size} so
 * you can iterate the array quickly using a fast array-iterator, instead of an
 * O(2n) Iterator. Basically I'm just a Set interface for an array, so you can
 * populate me as a Set (with above caveat), and then iterate me as an array.
 * <p>
 * Oh, and I do <b>NOT</b> allow null elements!
 *
 * @author Keith Corlett 2021-09-28
 */
public class ArraySet<T> implements Set<T> {

	public static final int NOT_FOUND = -1;

	public static final int MAX_CAPACITY = 1024;

	public final int maxCapacity;
	public T[] array;
	private int lastIndex;
	public int capacity;
	public int size;

	public ArraySet(final int initialCapacity) {
		this(initialCapacity, MAX_CAPACITY);
	}

	@SuppressWarnings({"unchecked"})
	private T[] newArray(final int size) {
		return (T[])new Object[size];
	}

	public ArraySet(final int initialCapacity, final int maxCapacity) {
		if ( maxCapacity > MAX_CAPACITY ) {
			throw new IllegalArgumentException("maxCapacity exceeds MAX_CAPACITY="+MAX_CAPACITY+" so use another Set, because my indexOf (et al) is an O(n) operation, so large ArraySets are too slow.");
		}
		this.maxCapacity = maxCapacity;
		if ( initialCapacity > maxCapacity ) {
			throw new IllegalArgumentException("initialCapacity exceeds maxCapacity="+maxCapacity);
		}
		final int size;
		if ( initialCapacity < 8 ) {
			size = 8;
		} else {
			size = initialCapacity;
		}
		array = newArray(capacity=size);
		lastIndex = capacity - 1;
	}

	public void grow(final int newCapacity) {
		if ( newCapacity > maxCapacity ) {
			throw new ArrayIndexOutOfBoundsException("growth exceeded maxCapacity="+maxCapacity);
		}
		T[] newArray = newArray(newCapacity);
		System.arraycopy(array, 0, newArray, 0, size);
		array = newArray;
		capacity = newCapacity;
		lastIndex = capacity - 1;
	}

	@Override
	public boolean add(final T e) {
		if ( e == null ) {
			throw new NullPointerException(Log.me()+" precludes null");
		}
		if ( size >= capacity ) {
			grow(capacity<<1);
		}
		array[size] = e;
		++size; // nb: size++ above is WRONGLY incremented before AIOOBE
		return true;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Cast Object to T. This method exists to limit the scope for
	 * @SuppressWarnings({"unchecked"}).
	 *
	 * @throws ClassCastException if unconvertible.
	 */
	@SuppressWarnings({"unchecked"})
	private T cast(Object o) {
		return (T)o;
	}

	public int indexOf(Object o) {
		if ( o != null ) {
			final T t = cast(o);
			for ( int i=0; i<size; ++i ) {
				if ( t.equals(array[i]) ) {
					return i;
				}
			}
		}
		return NOT_FOUND; // -1
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) > NOT_FOUND;
	}

	@Override
	public Iterator<T> iterator() {
		return new MyIterator();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		System.arraycopy(array, 0, a, 0, size);
		return a;
	}

	@Override
	public Object[] toArray() {
		return toArray(newArray(size));
	}

	public boolean remove(final int i) {
		if ( i > lastIndex ) {
			throw new ArrayIndexOutOfBoundsException(""+i+" > "+lastIndex);
		} else if ( i == lastIndex ) {
			array[lastIndex] = null;
		} else {
			// copy existing trailer over the last element.
			for ( int j=i; j<size; ++j ) {
				array[j] = array[j+1];
			}
		}
		--size;
		return true;
	}

	@Override
	public boolean remove(final Object o) {
		return remove(indexOf(o));
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for ( Object e : c ) {
			if ( !contains(e) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(final Collection<? extends T> c) {
		boolean result = false;
		for ( T e : c ) {
			result |= add(e);
		}
		return result;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		boolean result = false;
		for ( final Iterator<T> it = iterator(); it.hasNext(); ) {
			if ( !c.contains(it.next()) ) {
				it.remove();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		boolean result = false;
		for ( Object e : c ) {
			result |= remove(e);
		}
		return result;
	}

	@Override
	public void clear() {
		Arrays.fill(array, null);
		size = 0;
	}

	private class MyIterator implements Iterator<T> {
		private int i; // index
		@Override
		public boolean hasNext() {
			return i < size;
		}
		@Override
		public T next() {
			return array[i++];
		}
		@Override
		public void remove() {
			ArraySet.this.remove(i);
		}
	}

}
