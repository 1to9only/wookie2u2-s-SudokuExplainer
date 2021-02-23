/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;

/**
 * IdxL(ockable) extends Idx to add a lock() and unlock() method which prevent
 * accidental mutation of the Idx's contents by throwing a RuntimeException
 * when a mutator is called on an IdxL that has been lock()ed.
 * <p>
 * BEWARE: IdxL MUST override ALL of Idx's mutators to call checkLock().
 * If you miss one IdxL achieves nothing!
 * <p>
 * Back-story: IdxL separates the locking feature out of Idx, because
 * locking slows all operations on all Idx's, but locking is only ever used on
 * a few Idx's (all the buddies), so I subtyped-out the locking feature.
 * <p>
 * Swapping-in IdxL and Idx is a bit of a pain in the arse, but it's
 * worth the trouble for the speed improvement (3 or 4 seconds per top1465);
 * and if you can find 3-or-4 seconds cheaper I'll send you a Gold Star.
 *
 * @author Keith Corlett 2020-10-17
 */
public class IdxL extends Idx implements Cloneable {

	private static final long serialVersionUID = 113343L;

	protected boolean isLocked = false;

	IdxL() {
		super();
	}

	IdxL(Idx src) {
		super(src);
	}

	@Override
	public IdxL clone() {
		IdxL copy = (IdxL)super.clone();
		copy.isLocked = false; // you clone an Idx BECAUSE it's locked!
		return copy;
	}

	/**
	 * Prevent this Idx being modified accidentally. Throws a RuntimeException
	 * when you subsequently call any method that modifies my array contents.
	 * <p>
	 * Currently the regions indexes (ARegion.idx) are the only ones that're
	 * ever locked, but you can lock others, making them read-only. You should
	 * probably document them here also.
	 * <p>
	 * lock() is intended as a way to find the line of code which modifies the
	 * array contents, which causes problems down-stream. It wasn't intended
	 * for "normal use", though I see no reason why you shouldn't "leave it in"
	 * for any/all Idx-sets which have experienced this problem. If it happened
	 * once then it'll probably happen again, eventually. Problems are like
	 * that. It's a damn shame that solutions are not.
	 * <p>
	 * WARN: It's incumbent on the programmer to call checkLock() at the start
	 * of each and every method that can modify the arrays contents!
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

	private static class LockedException extends RuntimeException {
		private static final long serialVersionUID = 5194434282L;
		public LockedException() {
			super("This IdxL is locked!");
		}
	}

	/**
	 * If this Idx is lock()ed then throw a RuntimeException("Idx is locked!")
	 * so that the stack-trace tells you exactly where you stuffed up, by
	 * attempting to (possibly) modify an Idx which is still locked.
	 * <p>
	 * WARNING: for locks to work, EVERY method which mutates my state (except
	 * the constructors) must call checkLock(), so that the FIRST attempt to
	 * modify a locked Idx throws a RuntimeException.
	 */
	protected void checkLock() {
		if ( isLocked )
			throw new LockedException();
	}

	@Override
	public Idx set(Idx src) {
		checkLock();
		return super.set(src);
	}

	@Override
	public Idx set(int a0, int a1, int a2) {
		checkLock();
		return super.set(a0, a1, a2);
	}

	@Override
	public Idx set(Grid.ARegion[] regions) {
		checkLock();
		return super.set(regions);
	}

	@Override
	public Idx setAnd(Idx aa, Idx bb) {
		checkLock();
		return super.setAnd(aa, bb);
	}

	@Override
	public boolean setAndAny(Idx aa, Idx bb) {
		checkLock();
		return super.setAndAny(aa, bb);
	}

	@Override
	public Idx setOr(Idx aa, Idx bb) {
		checkLock();
		return super.setOr(aa, bb);
	}

	@Override
	public boolean setAny(Idx aa) {
		checkLock();
		return super.setAny(aa);
	}

	@Override
	public Idx setAndNot(Idx aa, Idx bb) {
		checkLock();
		return super.setAndNot(aa, bb);
	}
	
	@Override
	public Idx setExcept(Idx aa, Idx bb, Idx cc) {
		checkLock();
		return super.setExcept(aa, bb, cc);
	}

	@Override
	public Idx setAllExcept(Idx s2) {
		checkLock();
		return super.setAllExcept(s2);
	}

	@Override
	public void orNot(Idx s2) {
		checkLock();
		super.orNot(s2);
	}

	@Override
	public boolean orAndAny(Idx s1, Idx s2) {
		checkLock();
		return super.orAndAny(s1, s2);
	}

	@Override
	public void orAnd(Idx s1, Idx s2, Idx s3) {
		checkLock();
		super.orAnd(s1, s2, s3);
	}

	@Override
	public void not() {
		checkLock();
		super.not();
	}

	@Override
	public Idx clear() {
		checkLock();
		return super.clear();
	}

	@Override
	public Idx fill() {
		checkLock();
		return super.fill();
	}

	@Override
	public void add(int i) {
		checkLock();
		super.add(i);
	}

	/**
	 * Add the indice of each of the given cells to this Idx.
	 * <p>
	 * addAll checkLock()'s FIRST, even if cells is null or empty.
	 *
	 * @param cells
	 * @return this Idx, for method chaining.
	 */
	@Override
	public IdxL addAll(Cell[] cells) {
		checkLock();
		return (IdxL)super.addAll(cells);
	}

	@Override
	public void remove(int i) {
		checkLock();
		super.remove(i);
	}

	@Override
	public Idx and(Idx other) {
		checkLock();
		return super.and(other);
	}

	@Override
	public Idx andNot(Idx other) {
		checkLock();
		return super.andNot(other);
	}

	@Override
	public boolean andNotAny(Idx other) {
		checkLock();
		return super.andNotAny(other);
	}

	@Override
	public Idx or(Idx other) {
		checkLock();
		return super.or(other);
	}

	@Override
	public String toString() {
		if ( true ) // @check true
			return super.toString();
		else // debug only
			return (isLocked?"SHUT ":"OPEN ") + super.toString(); //To change body of generated methods, choose Tools | Templates.
	}

}
