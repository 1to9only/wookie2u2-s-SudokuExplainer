/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * IntFilter defines the accept(int) method, which is typically implemented
 * with a lambda expression in the call to doWhile.
 *
 * @author User
 */
public interface IntFilter {
	boolean accept(int i);
}
