/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * IntFor {@code <the generic type T> produces an int given an instance T.
 *
 * @author Keith Corlett 2023-07-01
 */
public interface IntFor<T> {
	int of(T c);
}

