/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * Visit an int today.
 *
 * @author Keith Corlett 2021-12-17
 */
public interface IntVisitor {
	public void visit(int i);
}
