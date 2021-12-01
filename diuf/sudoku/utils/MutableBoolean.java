/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/**
 * MutableBoolean is for use in lambda expressions, where an effectively final
 * parameter is required, but you need to set a boolean value.
 */
public class MutableBoolean {

	public boolean value;

	public MutableBoolean() {
		// value is initialised to false by default
	}

	public MutableBoolean(boolean value) {
		this.value = value;
	}

	private boolean eq(Object a, Object b) {
		return a==null ? b==null : a.equals(b);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof MutableBoolean
			&& eq(this.value, ((MutableBoolean)o).value);
	}

	@Override
	public int hashCode() {
		return value ? 1 : 0;
	}

	// debug only: not used in anger
	@Override
	public String toString() {
		return value ? "T" : "F";
	}

}
