/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Frmt;
import java.util.Collection;
import java.util.List;


/**
 * An Applicumulator is by "applier" out of "accumulator". A striped lion!
 * <p>
 * A HintsApplicumulator is used by Locking to apply each Pointing and Claiming
 * hint as soon as it is found, so that all hints are applied in one pass
 * through the grid (ie it's all about efficiency). We then add a single
 * AppliedHintsSummaryHint to the "normal" IAccumulator to pass the total
 * number of potential values eliminated back to the calling {@code apply()}
 * method to keep score.
 *
 * @author Keith Corlett 2016
 */
public final class HintsApplicumulator implements IAccumulator {

	public int numHints = 0;
	public int numElims = 0;
	// we append each hint.toFullString() to this StringBuilder when != null
	public final StringBuilder sb;
	// true to Autosolve, false to stop Cell.set finding subsequent singles.
	public final boolean isAutosolving;

	public HintsApplicumulator(boolean isStringy, boolean isAutosolving) {
		this.sb = isStringy ? new StringBuilder(256): null; // 256 just a guess
		this.isAutosolving = isAutosolving;
	}

	@Override
	public boolean isSingle() { return false; }

	@Override
	public void reset() {
		numHints = 0;
		numElims = 0;
		if ( sb != null )
			sb.setLength(0);
	}

	/**
	 * This is the funky part: Apply the hint to the grid and keep searching,
	 * so the immediate consequences of each hint are included in the search,
	 * so that setting just one cell may solve the whole grid.
	 * 
	 * @param hint
	 * @return 
	 */
	@Override
	public boolean add(AHint hint) {
		if ( hint == null )
			return false;
		if ( sb != null ) { // ie constructor @param isStringy = true
			if ( sb.length() > 0 )
				sb.append(Frmt.NL);
			sb.append(hint.toFullString());
		}
		// make Cell.set append subsequent singles (if sb!=null)
		hint.sb = sb;
		// nb: Do NOT eat apply exceptions, they're handled by my caller!
		numElims += hint.apply(isAutosolving);
		// keep count
		++numHints;
		// always return false so that the hinter always keeps searching
		return false;
	}

	@Override
	public boolean addAll(Collection<AHint> hints) {
		boolean result = false;
		for ( AHint hint : hints )
			result |= add(hint);
		return result;
	}

	@Override
	public AHint getHint() {
		return null;
	}

	@Override
	public AHint peek() {
		return null;
	}

	@Override
	public AHint peekLast() {
		return null;
	}

	@Override
	public boolean hasAny() {
		return false;
	}

	@Override
	public int size() {
		return 0;
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
