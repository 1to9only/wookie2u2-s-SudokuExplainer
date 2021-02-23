/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.solver.AHint;
import java.util.Collection;
import java.util.List;


/**
 * A SingleHintsAccumulator just holds the first added hint and interrupts
 * processing by returning true. The HintsAccumulator always returns false.
 * A return value of true means "The Accumulator wants you to abandon the
 * search, so please exit immediately."
 */
public final class SingleHintsAccumulator implements IAccumulator {

	private AHint theHint = null;

	@Override
	public boolean isSingle() { return true; }

	@Override
	public boolean add(AHint hint) {
		if ( hint==null || hint==theHint )
			return false; // you can't get here if it were true
		theHint = hint;
		return true; // meaning the caller should exit now, rather than continue looking for more hints.
	}

	@Override
	public void reset() {
		theHint = null;
	}

	@Override
	public boolean addAll(Collection<AHint> hints) {
		if ( hints.size() > 0 ) {
			theHint = hints.iterator().next();
			return true;
		}
		return false;
//		throw new UnsupportedOperationException("It's a SINGLEHintsAccumulator!");
	}

	/** @return the only accumulated hint, or <tt>null</tt> if none. */
	@Override
	public AHint getHint() {
		AHint result = theHint;
		theHint = null; // making this instance multi-use
		return result;
	}

	@Override
	public AHint peek() {
		return theHint; // may be null
	}

	@Override
	public AHint peekLast() {
		return theHint; // may be null
	}

	@Override
	public boolean hasAny() {
		return theHint != null;
	}

	@Override
	public int size() {
		return theHint==null ? 0 : 1;
	}

	@Override
	public void sort() {
		// a no-op
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		// a no-op
	}

}
