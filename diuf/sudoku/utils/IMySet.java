/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * A java.util.Set plus poll(), remove(), and a funky retainAllClear().
 *
 * @param <E>
 */
public interface IMySet<E> extends java.util.Set<E> {

	/**
	 * get is a bit odd: it gets the element from this Set which equals the
	 * given key object; useful when the Set contains "the actual instances"
	 * and the key is "identity-only".
	 *
	 * @param key
	 * @return
	 */
	public E get(E key);

	/**
	 * I pinched the poll method from {@link java.util.Queue#poll}.
	 *
	 * @return E
	 */
	public E poll();

	/**
	 * I pinched the remove method from {@link java.util.Queue#remove}; a poll
	 * that throws NoSuchElementException when the queue/set is empty.
	 *
	 * @return E.
	 */
	public E remove();

	/**
	 * Fuck !isEmpty()
	 *
	 * @return does this Set contain any elements
	 */
	public boolean any();

}