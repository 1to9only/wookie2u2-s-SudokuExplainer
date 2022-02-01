/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * IdxI is an IdxL that is immutable post construction. Any attempt to mutate
 * an IdxI throws an ImmutableException, so just don't, final; but note that I
 * am NOT final, so you can Adams D. me if you really really want to.
 * <p>
 * The problem with people who think they know everything is that they think
 * that they know everything. It's that thinking that is the problem, not the
 * people. Solve the problem. Retain the people. Solving the people gets hard
 * fast. Our government is currently undertaking a major effort to solve the
 * people. It will not work. Advertising can't cure corona, or global warming.
 * But then, I'm not psychopath or a psychophant, hence I despise governance.
 * Intelligent people can and will always make-up there own minds, about
 * <u>EVERY</u>thing, quite insightfully, employing high-quality extrapolation
 * of quality information. Trying to con the bastards is fundamentally useless,
 * and just makes you look like a Prize ___ing Dick! Take it or leave it.
 *
 * @author Keith Corlett 2020-07-13
 */
public class IdxI extends IdxL implements Cloneable {

	private static final long serialVersionUID = 4598934L;
	
	/** THE Immutable empty Idx. */
	public static final IdxI EMPTY = new IdxI();

	public static IdxI of(int[] indices) {
		int a0=0, a1=0, a2=0;
		for ( int i : indices )
			if ( i < BITS_PER_ELEMENT )
				a0 |= IDX_SHFT[i];
			else if ( i < BITS_TWO_ELEMENTS )
				a1 |= IDX_SHFT[i-BITS_PER_ELEMENT];
			else
				a2 |= IDX_SHFT[i-BITS_TWO_ELEMENTS];
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

	/**
	 * You clone an IdxI BECAUSE it's permanently locked, so my clone method,
	 * rather oddly, returns an IdxL that is unlocked (NOT an IdxI, which is
	 * as pointless as cloning an Integer with the value 1. Sigh).
	 * <p>
	 * I add nothing: behaviour doesn't change if I don't exist. I'm retained
	 * to explicitly document the pernicious bogee flavour of reality.
	 *
	 * @return an IdxL that is unlocked.
	 */
	@Override
	public IdxL clone() {
		return super.clone();
	}

	/**
	 * Throws an ImmutableException, always. An IdxI is permanently locked
	 * because my lock mechanism (this method) ignores the isLocked switch.
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
		private static final long serialVersionUID = 2524546L;
		public ImmutableException() { super("IdxI is immutable!"); }
	}

}
