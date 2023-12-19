/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.utils.IFilter;
import java.util.Set;

/**
 * IdxL: A Lockable Idx extends Idx to add a lock() and unlock() method so that
 * accidental mutation of an Idx throws a RuntimeException, ie when any mutator
 * is called on an IdxL that is lock()ed.
 * <p>
 * BEWARE: IdxL MUST override ALL of Idxs mutators to call checkLock(). If you
 * miss one it renders the whole of IdxL basically useless! The tool
 * {@link diuf.sudoku.tools.CheckIdxLMutators } helps with this.
 * <p>
 * Back-story: IdxL separates the locking feature out of Idx, because locking
 * slows all operations on all Idxs, but locking is used on few Idxs (buddies),
 * so I subtyped-out the locking feature. Swapping-in IdxL is pain in the ass,
 * but worth the trouble for speed. IdxL saves 3 or 4 seconds per top1465; and
 * if you can find 3-or-4 seconds for less work you deserve a Gold Star!
 *
 * @author Keith Corlett 2020-10-17
 */
public class IdxL extends Idx {

	private static final long serialVersionUID = 113343L;

	public static IdxL empty() {
		return new IdxL(); // empty by default
	}

	public static IdxL full() {
		return new IdxL(true); // full by design
	}

	public static IdxL of(final Idx idx) { // overrides Idx of(Idx)
		return new IdxL(idx);
	}

	public static IdxL of(final int[] indices) { // overrides Idx of(int[])
		long m0=0; int m1=0;
		for ( int i : indices )
			if ( i < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		return new IdxL(m0, m1);
	}

	public static IdxL of(final Cell[] cells) {
		return of(cells, cells.length);
	}

	public static IdxL of(final Cell[] cells, final int n) {
		final IdxL result = new IdxL();
		for ( int i=0; i<n; ++i )
			result.add(cells[i].indice);
		return result;
	}

	public static IdxL of(final Cell[] cells, final int[] indexes) {
		final IdxL result = new IdxL();
		for ( int i : indexes )
			result.addOK(cells[i].indice);
		return result;
	}

	public static IdxL of(final Cell[] cells, final int[] indexes, final int n) {
		final IdxL result = new IdxL();
		for ( int i=0; i<n; ++i )
			result.add(cells[indexes[i]].indice);
		return result;
	}

	protected boolean isLocked = false;

	public IdxL() {
		super();
	}

	public IdxL(final boolean isFull) {
		super(isFull);
	}

	public IdxL(final Idx src) {
		super(src);
	}

	public IdxL(final long m0, final int m1) {
		super(m0, m1);
	}

	public IdxL(final Set<Integer> indices) {
		super(indices);
	}

	// I am package visible
	void setOK(final IdxL idx) {
		// no checkLock!
		super.set(idx);
	}

	// I am package visible
	IdxL setOK(final long m0, final int m1) {
		// no checkLock!
		return (IdxL)super.set(m0, m1);
	}

	// I am package visible
	IdxL clearOK() {
		// no checkLock!
		return (IdxL)super.clearMe();
	}

	// I am package visible
	void addOK(final int i) {
		// no checkLock!
		super.add(i);
	}

	// I am package visible
	void removeOK(final int i) {
		// NOTE WELL: no checkLock!
		super.remove(i);
	}

	/**
	 * Prevent this Idx being modified accidentally. Throws a RuntimeException
	 * when you subsequently call any method that modifies my array contents.
	 * <p>
	 * Currently the regions indexes (ARegion.idx) are the only ones that are
	 * ever locked, but you can lock others, making them read-only. You should
	 * probably document them here also.
	 * <p>
	 * lock() is intended as a way to find the line of code which modifies the
	 * array contents, which causes problems down-stream. It was not intended
	 * for "normal use", though I see no reason why you should not leave-it-in
	 * for any/all Idx-sets which have experienced this problem. If it happened
	 * once then it will probably happen again, eventually. Problems are like
	 * that. It is a damn shame that solutions are not.
	 * <p>
	 * WARN: It is incumbent on the IdxL programmer to ensure that checkLock()
	 * is called at the start of each and every @Mutator. Spring would help.
	 * The tool {@link diuf.sudoku.tools.CheckIdxLMutators } is my answer.
	 *
	 * @return this Idx for method chaining.
	 */
	public IdxL lock() {
		isLocked = true;
		return this;
	}

	/**
	 * Allow this Idx to be modified again.
	 * @return this Idx for method chaining.
	 */
	public IdxL unlock() {
		isLocked = false;
		return this;
	}

	/**
	 * If this Idx is lock()ed then throw a RuntimeException("Idx is locked!")
	 * so that the stack-trace tells you exactly where you stuffed up, by
	 * attempting to (possibly) modify an Idx which is still locked.
	 * <p>
	 * WARN: for locks to work, EVERY method which mutates my state (except
	 * the constructors) must call checkLock(), so that the FIRST attempt to
	 * modify a locked Idx throws a RuntimeException.
	 */
	protected void checkLock() {
		if ( isLocked )
			throw new LockedException();
	}

	@Override
	public Idx fill() {
		checkLock();
		return (IdxL)super.fill();
	}

	@Override
	public IdxL setIndice(final int indice) {
		checkLock();
		return (IdxL)super.setIndice(indice);
	}

	@Override
	public Idx setIndices(final int indiceA, final int indiceB) {
		checkLock();
		return (IdxL)super.setIndices(indiceA, indiceB);
	}

	@Override
	public Idx setIndices(final int indiceA, final int indiceB, final int indiceC) {
		checkLock();
		return (IdxL)super.setIndices(indiceA, indiceB, indiceC);
	}

	@Override
	public Idx setIndices(final int indiceA, final int indiceB, final int indiceC, final int indiceD) {
		checkLock();
		return (IdxL)super.setIndices(indiceA, indiceB, indiceC, indiceD);
	}

	@Override
	public IdxL set(final Idx src) {
		checkLock();
		return (IdxL)super.set(src);
	}

	@Override
	public IdxL set(final long m0, final int m1) {
		checkLock();
		return (IdxL)super.set(m0, m1);
	}

	@Override
	public boolean setAny(final long m0, final int m1) {
		checkLock();
		return super.setAny(m0, m1);
	}

	@Override
	public IdxL set(final Cell[] cells, final int n) {
		checkLock();
		return (IdxL)super.set(cells, n);
	}

	@Override
	public IdxL setAnd(final Idx aa, final Idx bb) {
		checkLock();
		return (IdxL)super.setAnd(aa, bb);
	}

	@Override
	public IdxL setAnd(final Idx aa, final Idx bb, final Idx cc) {
		checkLock();
		return (IdxL)super.setAnd(aa, bb, cc);
	}

	@Override
	public boolean setAndAny(final Idx aa, final Idx bb) {
		checkLock();
		return super.setAndAny(aa, bb);
	}

	@Override
	public boolean setAndMany(final Idx aa, final Idx bb) {
		checkLock();
		return super.setAndMany(aa, bb);
	}

	@Override
	public IdxL setAndNot(final Idx aa, final Idx bb) {
		checkLock();
		return (IdxL)super.setAndNot(aa, bb);
	}

	@Override
	public IdxL setOr(final Idx aa, final Idx bb) {
		checkLock();
		return (IdxL)super.setOr(aa, bb);
	}

	@Override
	public boolean setAndNotAny(final Idx aa, final Idx bb) {
		checkLock();
		return super.setAndNotAny(aa, bb);
	}

	@Override
	public boolean setAndNotAny(final Idx aa, final Idx bb, final Idx cc) {
		checkLock();
		return super.setAndNotAny(aa, bb, cc);
	}

	@Override
	public boolean setAndAny(final Idx aa, final Idx bb, final Idx cc) {
		checkLock();
		return super.setAndAny(aa, bb, cc);
	}

	@Override
	public int poll() {
		checkLock();
		return super.poll();
	}

	@Override
	public IdxL clearMe() {
		checkLock();
		return (IdxL)super.clearMe();
	}

	@Override
	public void clear() {
		checkLock();
		super.clear();
	}

	@Override
	public void add(final int i) {
		checkLock();
		super.add(i);
	}

	@Override
	public IdxL read(final Cell[] cells, final IFilter<Cell> f) {
		checkLock();
		return (IdxL)super.read(cells, f);
	}

	@Override
	public void remove(final int i) {
		checkLock();
		super.remove(i);
	}

	@Override
	public boolean visit(final int i) {
		checkLock();
		return super.visit(i);
	}

	@Override
	public IdxL and(final Idx other) {
		checkLock();
		return (IdxL)super.and(other);
	}

	@Override
	public IdxL andNot(final Idx idx) {
		checkLock();
		return (IdxL)super.andNot(idx);
	}

	@Override
	public IdxL or(final Idx idx) {
		checkLock();
		return (IdxL)super.or(idx);
	}

	@Override
	public IdxL or(final Idx idxA, final Idx idxB) {
		checkLock();
		return (IdxL)super.or(idxA, idxB);
	}

	@Override
	public String toString() {
		return super.toString();
//		return (isLocked?"LOKD ":"OPEN ") + super.toString();
	}

	private static class LockedException extends RuntimeException {
		private static final long serialVersionUID = 5194434282L;
		public LockedException() {
			super("This IdxL is locked!");
		}
	}

}
