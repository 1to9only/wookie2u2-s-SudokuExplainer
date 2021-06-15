/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Set;


/** A java.util.Set plus poll(), remove(), and funky retains.
 * @param <E> */
public interface IMySet<E> extends Set<E> {
	
	/**
	 * This method is a bit odd: it gets the element from this Set which equals
	 * the given pKey object. This is useful when the Set contains "the real
	 * objects" and the pKey object is an "identity-only" key object.
	 * @param pKey
	 * @return
	 */
	public E get(E pKey);
	
	/**
	 * We've borrowed the poll method from the java.util.Queue interface.
	 * @return E
	 */
	public E poll();
	
	/** 
	 * We've also borrowed the remove method from the Queue interface; a poll
	 * which throws NoSuchElementException when the queue/set is empty.
	 * @return E.
	 */
	public E remove();
	
	/**
	 * do retainAll(c) and return isEmpty() instead of isModified.
	 * @param c {@code IMySet<E>}
	 * @return isEmpty() instead of isModified.
	 */
	public boolean retainAllIsEmpty(IMySet<E> c);
	
	/**
	 * do retainAll(c) and then c.clear();
	 * @param c {@code IMySet<E>}
	 */
	public void retainAllClear(IMySet<E> c);
}