/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.utils.IntQueue.QEMPTY;
import java.util.Iterator;
import java.util.Set;

/**
 * IdxI is an immutable Idx, ie an IdxL that is immutable post construction.
 * Any attempt to mutate an IdxI throws an ImmutableException, so just dont.
 * Note IdxI is final, so you cant AdamsD me, but only if you really want to.
 * <p>
 * The problem with people who think they know everything is that they think
 * that they know everything. Its that thinking that is the problem, not the
 * people. Solve the problem, retain the people. Solving the people gets ugly
 * fast. Our government is currently undertaking a major effort to solve the
 * people. It will not work. Advertising wont cure Corona, or global warming.
 * But then, I am not a psychopath, or a psychophant, so I respect governance.
 * I dont like it, but I do respect it. Our government however can go and get
 * ____ed. They have no respect for anything, let alone governance.
 * <p>
 * Intelligent people can and will always make-up there own minds, about
 * <u>every</u>thing, quite insightfully, employing high-quality extrapolation
 * of quality information. Trying to con the bastards is fundamentally useless,
 * and just makes you look like a ___ing Dick! Take it or leave it. So no, I am
 * most certainly NOT finally final, coz my missus cant say I have to be. Sigh.
 *
 * @author Keith Corlett 2020-07-13
 */
public class IdxI extends IdxL {

	private static final long serialVersionUID = 4598934L;

	/** THE empty immutable Idx. */
	public static final IdxI EMPTY = new IdxI() {
		private static final long serialVersionUID = 2222222598713497292L;
		@Override
		public boolean isEmpty() { return true; }
		@Override
		public boolean any() { return false; }
		@Override
		public int size() { return 0; }
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				@Override
				public boolean hasNext() { return false; }
				@Override
				public Integer next() { return QEMPTY; }
			};
		}
		@Override
		public IntQueue indices() {
			return new IntQueue() {
				@Override
				public int poll() { return QEMPTY; }
			};
		}
	};

	// SPEED: switch tested substantially faster than array
	public static IdxI of(final int[] indices) {
		long m0=0L; int m1=0;
		for ( int i : indices )
			if ( i < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxI(m0, m1, indices.length);
	}

	public static IdxI of(final Cell[] cells) {
		return of(cells, cells.length);
	}

	public static IdxI of(final Cell[] cells, final int n) {
		long m0=0L; int i, m1=0;
		for ( int j=0; j<n; ++j )
			if ( (i=cells[j].indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxI(m0, m1, n);
	}

	public static IdxI of(final Cell[] cells, final int[] indexes) {
		long m0=0L; int i, m1=0;
		for ( int j : indexes )
			if ( (i=cells[j].indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxI(m0, m1, indexes.length);
	}

	public static IdxI of(final Cell[] cells, final int[] indexes, final int n) {
		long m0=0L; int i, m1=0;
		for ( int j=0; j<n; ++j )
			if ( (i=cells[indexes[j]].indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxI(m0, m1, n);
	}

	public static IdxI of(final int[] indices, final int[] indexes, final int n) {
		long m0=0L; int i, m1=0;
		for ( int j=0; j<n; ++j )
			if ( (i=indices[indexes[j]]) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxI(m0, m1, n);
	}

	// immutable: cache everything!
	private int size = -1;
	private boolean anySet;
	private boolean any;
	private String ts;

	public IdxI() {
		super();
	}

	public IdxI(final boolean isFull) {
		super(isFull);
	}

	public IdxI(final Idx src) {
		super(src);
	}

	public IdxI(final long m0, final int m1) {
		super(m0, m1);
	}

	public IdxI(final long m0, final int m1, final int n) {
		super(m0, m1);
		size = n;
	}

	public IdxI(final Set<Integer> indices) {
		super(indices);
	}

	// IdxI is immutable: cache everything!
	@Override
	public int size() {
		if ( size < 0 )
			size = super.size();
		return size;
	}

	// IdxI is immutable: cache everything!
	@Override
	public boolean any() {
		if ( !anySet ) {
			anySet = true;
			any = size() > 0;
		}
		return any;
	}

	/**
	 * Throws an ImmutableException, always. An IdxI is permanently locked
	 * because my lock mechanism (this method) ignores the isLocked switch.
	 */
	@Override
	protected void checkLock() {
		throw new ImmutableException();
	}

	/**
	 * Throws an ImmutableException, always. This is an IdxI. You should NOT be
	 * attempting to unlock me. You have probably muddled-up your Idxs.
	 *
	 * @return never: always throws
	 */
	@Override
	public IdxI unlock() {
		throw new ImmutableException();
	}

	@Override
	public String toString() {
		if ( ts == null ) {
			ts = super.toString();
		}
		return ts;
	}

	private static class ImmutableException extends RuntimeException {
		private static final long serialVersionUID = 2524546L;
		public ImmutableException() { super("IdxI is immutable!"); }
	}

}
