/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * ArraySet presents an array of Object as a {@code Set<T>}.
 * <p>
 * NOTE WELL: ArraySet does <b>NOT</b> allow null elements!
 * <p>
 * NOTE WELL: that unlike other implementations of java.util.Set, ArraySet does
 * not enforce uniqueness, breaking the java.util contract. Hence, ArraySet is
 * only useful if you already have distinct elements to add to it.
 * <p>
 * The up-side is the {@link #array} field is public, as is the {@link #size}
 * so you can iterate the array using an array-iterator, which is much faster
 * an Iterator, which requires O(2n) method calls.
 * <p>
 * Basically I am just a Set interface for an array, so you populate me as a
 * Set, and iterate the actual array.
 *
 * @author Keith Corlett 2021-09-28
 * @param <T>
 */
public class ArraySet<T> implements Set<T> {

	public static final int NOT_FOUND = -1;

	public static final int MIN_CAPACITY = 8;
	public static final int MAX_CAPACITY = 15*1024;

	public int maxCapacity;
	public T[] array;
	public int capacity;
	public int size; // number of elements currently in this set

	public ArraySet(final int initialCapacity) {
		this(initialCapacity, MAX_CAPACITY);
	}

	public ArraySet() {
		this(MIN_CAPACITY, MAX_CAPACITY);
	}

	public ArraySet(T[] a) {
		this(a.length, MAX_CAPACITY);
		final int len = a.length;
		// nb: arraycopy is slower for tiny arrays, but does not slow down as
		// quickly as the array length increases, so arraycopy is faster for
		// large arrays, and about "a method call" slower for small ones.
		// src, srcPos, dest, destPos, length
		System.arraycopy(a, 0, array, 0, len);
		size = len;
	}

	@SuppressWarnings({"unchecked"})
	private T[] newArray(final int size) {
		return (T[])new Object[size];
	}

	public ArraySet(final int initialCapacity, final int maxCapacity) {
		if ( maxCapacity > MAX_CAPACITY )
			throw new IllegalArgumentException("maxCapacity="+maxCapacity+" > MAX_CAPACITY="+MAX_CAPACITY+", so use another type of Set.");
		this.maxCapacity = maxCapacity;
		if ( initialCapacity > maxCapacity )
			throw new IllegalArgumentException("initialCapacity="+initialCapacity+" > maxCapacity="+maxCapacity);
		array = newArray(capacity=(initialCapacity < MIN_CAPACITY ? MIN_CAPACITY : initialCapacity));
	}

	public void grow(int newCapacity) {
		if ( newCapacity > maxCapacity )
			if ( maxCapacity < MAX_CAPACITY )
				newCapacity = maxCapacity = MAX_CAPACITY;
			else
				throw new ArrayIndexOutOfBoundsException("newCapacity exceeds MAX_CAPACITY="+MAX_CAPACITY);
		T[] newArray = newArray(newCapacity);
		System.arraycopy(array, 0, newArray, 0, size);
		array = newArray;
		capacity = newCapacity;
	}

	@Override
	public boolean add(final T e) {
		if ( e == null )
			throw new NullPointerException(Log.me()+" e is null");
		if ( size >= capacity )
			grow(capacity<<1);
		array[size] = e;
		++size; // nb: size++ above is incremented before AIOOBE
		return true;
	}

	/**
	 * A faster add, available when reference type is the specific ArraySet.
	 * Note that justAdd does not grow array, and does not check for null e.
	 *
	 * @param e to be added to this Set, must NOT be null!
	 */
	public void justAdd(final T e) {
		array[size] = e;
		++size; // nb: size++ above is incremented before AIOOBE
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
	 * Cast Object to the generic type T. This method exists to minimise the
	 * scope of {@code @SuppressWarnings({"unchecked"})} to just the cast.
	 *
	 * @throws ClassCastException if unconvertible.
	 */
	@SuppressWarnings({"unchecked"})
	private T cast(Object o) {
		return (T)o;
	}

	public int indexOf(Object o) {
		if ( o == null ) {
			// ALWAYS search forwards for null!
			for ( int i=0; i<size; ++i )
				if ( array[i] == null )
					return i;
		} else {
			// search backways coz last-added is most common use of indexOf.
			final T target = cast(o);
			for ( int i=size-1; i>-1; --i )
				if ( target.equals(array[i]) )
					return i;
		}
		return NOT_FOUND; // -1
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) > NOT_FOUND;
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

	/**
	 * <b>NOTE WELL that the remove(int) method is, as yet, untested.</b>
	 *
	 * @param i the index of the element to remove
	 * @return was the element removed; always true, else an exception is
	 * thrown.
	 */
	public boolean remove(final int i) {
		if ( i < 0 )
			throw new ArrayIndexOutOfBoundsException("i="+i+" is less than zero");
		if ( i > size-1 )
			return false;
		// copy trailing elements left one place.
		for ( int j=i; j<size; ++j )
			array[j] = array[j+1];
		array[size-1] = null; // for the GC
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
	public boolean retainAll(final Collection<?> keep) {
		boolean result = false;
		for ( final Iterator<T> it = iterator(); it.hasNext(); ) {
			if ( !keep.contains(it.next()) ) {
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

	@Override
	public Iterator<T> iterator() {
		return new MyIterator();
	}

	/**
	 * MyIterator implements an {@code Iterator<T>} over {@code ArraySet<T>}.
	 * <p>
	 * NOTE: MyIterator had a bug with index incrementation that remove()
	 * tripped over. Index was post-incremented by next, so remove(i) killed
	 * the element FOLLOWING the current one, instead of the current one.
	 * To fix this we start with the index BEFORE the first element and
	 * pre-increment it in next(), so that the following call to remove()
	 * kills the current element. Sigh.
	 */
	private class MyIterator implements Iterator<T> {
		private int i = -1; // index is before first
		private final int m = ArraySet.this.size - 1; // m is one before n
		@Override
		public boolean hasNext() {
			return i < m;
		}
		@Override
		public T next() {
			return array[++i];
		}
		@Override
		public void remove() {
			ArraySet.this.remove(i);
		}
	}

}
