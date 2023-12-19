/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * IntQueue is {@code Queue<int>} which cannot exist, so I rolled my own.
 *
 * @author Keith Corlett 2023-11-11
 */
public interface IntQueue {

	/**
	 * QEMPTY is returned by poll when this queue is empty.
	 */
	public static final int QEMPTY = -1;

	/**
	 * Returns the next element or -1 meaning none
	 * @return next element of -1 meaning none (empty)
	 */
	public int poll();

}
