/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Visit something with two parameters, of two generic types. In my case that
 * is Grid.Cell and Integer count. Shame theres no generic int. Sigh.
 *
 * @author Keith Corlett
 * @param <A> generics param type one
 * @param <B> generics param type two
 */
public interface IVisitor2<A, B> {
	public void visit(A a, B b);
}
