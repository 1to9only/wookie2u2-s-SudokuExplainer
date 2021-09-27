/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Grid;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Frmt;
import java.util.Collection;
import java.util.List;


/**
 * An Applicumulator is by "apply" out of "accumulator"; a striped lion.
 * <p>
 * A HintsApplicumulator is used by
 * {@link diuf.sudoku.solver.hinters.lock.Locking} to apply each Pointing and
 * Claiming hint as soon as it is found, so that all hints are applied in one
 * pass through the grid (it's all about efficiency). Then Locking adds a
 * single {@link AppliedHintsSummaryHint} to the "normal" IAccumulator to pass
 * the total number of potential values eliminated back to the calling
 * {@link AHint#applyImpl} method, in order to keep score.
 * <p>
 * Note that there's nothing specific to Locking here, it's just that Locking
 * is currently the only place I'm used, because Locking is were I'm needed
 * most, coz Locking tends to cause Locking; far more than other hint types.
 * <p>
 * {@link diuf.sudoku.solver.checks.RecursiveAnalyser#solveLogically} builds-up
 * a {@link AppliedHintsSummaryHint} with the agglomerate of all hints found
 * and applied (through me) in a run. I track the number of hints found, the
 * number of eliminations, and a StringBuilder of the line-separated
 * toFullString's of all the hints I apply, to pass on to the summary hint.
 * <p>
 * Note that for speed, the public Grid grid should be set post-construction,
 * rather than have every single bloody hint lookup it's own grid.
 * @author Keith Corlett 2016
 */
public final class HintsApplicumulator implements IAccumulator {

	public int numHints = 0;
	public int numElims = 0;
	// we append each hint.toFullString() to this StringBuilder when != null
	public final StringBuilder SB;
	// true to Autosolve, false to stop Cell.set finding subsequent singles.
	public final boolean isAutosolving;
	// set me to avoid getting the grid for every single bloody hint
	public Grid grid;

	 /**
	  * Constructor.
	  * <p>
	  * <b>Note</b> for speed, the grid field should be set post-construction,
	  * rather than have every single bloody hint lookup it's own grid.
	  * Clear it in a finally block when you're finished with that grid or this
	  * HintsApplicumulator. Most HintsApplicumulator are short-lived.
	  *
	  * @param isStringy
	  * @param isAutosolving 
	  */
	public HintsApplicumulator(boolean isStringy, boolean isAutosolving) {
		if ( isStringy )
			this.SB = new StringBuilder(256); // 256 just a guess
		else
			this.SB = null;
		this.isAutosolving = isAutosolving;
	}

	@Override
	public boolean isSingle() { return false; }

	/**
	 * Runs at the start of each solveLogically, to clear my buffers.
	 */
	@Override
	public void reset() {
		numHints = 0;
		numElims = 0;
		if ( SB != null )
			SB.setLength(0);
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
		if ( SB != null ) { // ie constructor @param isStringy = true
			if ( SB.length() > 0 )
				SB.append(Frmt.NL);
			SB.append(hint.toFullString());
		}
		// make Cell.set append subsequent singles (if sb!=null)
		hint.SB = SB;
		// nb: Do NOT eat apply exceptions, they're handled by my caller!
		Grid myGrid = this.grid; // this.grid should be set and finally cleared
		if ( myGrid != null ) // so pass it to apply, for speed
			numElims += hint.apply(isAutosolving, false, myGrid);
		else // else let the hint look-up it's own grid
			numElims += hint.apply(isAutosolving, false);
		// keep count
		++numHints;
		// always return false so that the hinter always keeps searching
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends AHint> hints) {
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
