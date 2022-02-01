/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * A MutableInt exposes an int field that may be mutated inside a lambda
 * expression, which requires all arguments to be final, or effectively so.
 *
 * @author Keith Corlett 2021-11-21
 */
public class MutableInt {
	public int value;
	public MutableInt() { value = 0; }
	public MutableInt(int value) { this.value = value; }
}
