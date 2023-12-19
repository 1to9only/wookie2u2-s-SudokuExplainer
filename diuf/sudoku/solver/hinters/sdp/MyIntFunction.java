/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

/**
 * MyIntFunction is the inverse of java.util.function.IntFunction.
 * I take T and return int; he takes int and returns T.
 *
 * @author Keith Corlett 2023-07-01
 */
interface MyIntFunction<T> {
	public int get(T t);
}
