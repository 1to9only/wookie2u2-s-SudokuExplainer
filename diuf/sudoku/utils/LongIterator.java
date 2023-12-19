/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * An iterator over longs.
 *
 * @author Keith Corlett based on Josh Bloch's java.util.Iterator
 * @see Iterator
 */
public interface LongIterator {

	/**
	 * Returns {@code true} if the iteration has more elements.
	 * (In other words, returns {@code true} if {@link #next} would
	 * return an element rather than throwing an exception.)
	 *
	 * @return {@code true} if the iteration has more elements
	 */
	boolean hasNext();

	/**
	 * Returns the next element in the iteration.
	 *
	 * @return the next element in the iteration
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	long next();

	/**
	 * Removes from the underlying collection the last element returned
	 * by this iterator (optional operation).  This method can be called
	 * only once per call to {@link #next}.  The behavior of an iterator
	 * is unspecified if the underlying collection is modified while the
	 * iteration is in progress in any way other than by calling this
	 * method.
	 *
	 * @implSpec
	 * The default implementation throws an instance of
	 * {@link UnsupportedOperationException} and performs no other action.
	 *
	 * @throws UnsupportedOperationException if the {@code remove}
	 *         operation is not supported by this iterator
	 *
	 * @throws IllegalStateException if the {@code next} method has not
	 *         yet been called, or the {@code remove} method has already
	 *         been called after the last call to the {@code next}
	 *         method
	 */
	default void remove() {
		throw new UnsupportedOperationException("remove");
	}

}
