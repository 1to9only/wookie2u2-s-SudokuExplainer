/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Visit!
 * 
 * @author Keith Corlett 2021-07-08
 */
public interface IVisitor<E> {
	void visit(E e);
}
