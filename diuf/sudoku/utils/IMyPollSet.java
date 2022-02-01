/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * An {@code IMyPollSet<T>} is just a {@code java.util.Set<T>} with the 
 * {@code poll()} method borrowed from the {@code java.util.Queue} interface
 * to avoid {@code ConcurrentModificationException}s.
 * <p>
 * A {@code java.util.Set} enforces uniqueness of elements and
 * {@code java.util.LinkedSet} maintains a Set in "natural order" so that my
 * Iterator produces elements in the order in which they where added.
 * 
 * @author Keith Corlett 2018 Jan
 * @param <T> the type of stuff in this Set.
 */
public interface IMyPollSet<T> extends java.util.Set<T> {
    /**
     * Removes and returns the head of this set,
     * or returns {@code null} if this set is empty.
	 * <p>
	 * @see java.util.Queue#poll
     *
	 * @return the first (oldest) element which is removed from this Set.
	 */
	public T poll();
}
