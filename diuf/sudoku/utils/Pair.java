/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.EQUALS;


/**
 * A pair of generically-typed objects.
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public class Pair<A, B> {

	public A a;
	public B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	private boolean eq(Object o1, Object o2) {
		return o1==null ? o2==null : o1.equals(o2);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair<?,?> other = (Pair<?,?>)o;
			return eq(this.a, other.a)
				&& eq(this.b, other.b);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		if (a != null)
			result = a.hashCode();
		if (b != null)
			result ^= b.hashCode();
		return result;
	}

	// not used in anger
	@Override
	public String toString() {
		return ""+a+EQUALS+b;
	}

}
