/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2018 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;


/**
 * Filter facilitates deciding which filter to use ONCE, and then executing
 * it repeatedly, which is more efficient (I think) than repeatedly deciding
 * which filter to use in-situ.
 * @author Keith Corlett
 */
public interface IFilter<T> {
	boolean accept(T t);
}
