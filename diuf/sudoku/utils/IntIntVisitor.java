/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Visit two ints today, or your hair will fall-out, honest. Ask Tom.
 *
 * @author Keith Corlett 2021-12-17
 */
public interface IntIntVisitor {
	public void visit(int count, int indice);
}
