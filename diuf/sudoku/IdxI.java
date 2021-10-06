/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * IdxI is an IdxL that is immutable post construction. Any attempt to mutate
 * an IdxI throws an ImmutableException, so just don't, final; but please note
 * that I am NOT final, so that you can take the Adams D but only if you really
 * really want to.
 * <p>
 * The problem with people who think they know everything is that they think
 * that they know everything. It's that thinking that is the problem, not the
 * people. Solve the problem. Retain the people. Solving the people gets hard
 * fast. Our government is currently undertaking a major effort to solve the
 * people. It will not work. Advertising can't cure corona, or global warming.
 * But I'm a leader. ____off ya twit!
 *
 * @author Keith Corlett 2020-07-13
 */
public class IdxI extends IdxL implements Cloneable {

	private static final long serialVersionUID = 4598934L;

	public static IdxI of(int[] indices) {
		int a0=0, a1=0, a2=0;
		for ( int i : indices )
			if ( i < BITS_PER_ELEMENT )
				a0 |= SHFT[i];
			else if ( i < BITS_TWO_ELEMENTS )
				a1 |= SHFT[i%BITS_PER_ELEMENT];
			else
				a2 |= SHFT[i%BITS_PER_ELEMENT];
		return new IdxI(a0, a1, a2);
	}

	IdxI() {
		super();
	}

	IdxI(boolean isFull) {
		super(isFull);
	}

	IdxI(Idx src) {
		super(src);
	}

	IdxI(int a0, int a1, int a2) {
		super(a0, a1, a2);
	}

	@Override
	public IdxI clone() {
		IdxI copy = (IdxI)super.clone();
		copy.isLocked = false; // you clone an Idx BECAUSE it's locked!
		return copy;
	}

	/**
	 * Make IdxL ALLWAYS throw a wobbly.
	 */
	@Override
	protected void checkLock() {
		throw new ImmutableException();
	}

	@Override
	public String toString() {
		return "IdxI: "+super.toString();
	}

	private static class ImmutableException extends RuntimeException {
		private static final long serialVersionUID = 2524545L;
		public ImmutableException() {
			super("IdxI[mmutable] is immutable!");
		}
	}

}
