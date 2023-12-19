/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Visit something with one parameter.
 *
 * @author Keith Corlett 2021-07-08
 * @param <E> generics type of the parameter
 */
public interface IVisitor<E> {
	void visit(E e);
}
