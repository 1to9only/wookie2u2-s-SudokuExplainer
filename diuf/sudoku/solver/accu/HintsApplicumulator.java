/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Grid;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Frmt;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An Applicumulator is by "apply" out of "accumulator"; a striped lion.
 * <p>
 * A HintsApplicumulator is used by
 * {@link diuf.sudoku.solver.hinters.lock.Locking} to apply each Pointing and
 * Claiming hint as soon as it is found, so that all hints are applied in one
 * pass through the grid (for efficiency); then Locking adds a single
 * {@link diuf.sudoku.solver.accu.SummaryHint} to the "normal" IAccumulator to
 * pass the total number of potential values eliminated back to the calling
 * {@link AHint#applyImpl} method, in order to keep score.
 * <p>
 * Note that there's nothing specific to Locking here, it's just that Locking
 * is currently the only place I'm used, because Locking is were I'm needed
 * most, coz Locking tends to cause Locking; far more than other hint types.
 * <p>
 * {@link diuf.sudoku.solver.checks.BruteForce#solveLogically} builds-up
 * a {@link diuf.sudoku.solver.accu.SummaryHint} with the agglomerate of all
 * hints found and applied (through me) in a run. I track the number of hints
 * found, the number of eliminations, and a StringBuilder of the line-separated
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
	// set me to avoid getting the grid for every single bloody hint
	public Grid grid;

	 /**
	  * Constructor.
	  * <p>
	  * <b>Note</b> that, for speed, the grid field is set post-construction,
	  * rather than have every single hint lookup it's own grid. Null it in a
	  * finally block when you're finished with that grid or this apcu.
	  * <p>
	  * By design, most HintsApplicumulator's are single-use instances: by
	  * which I mean that a HintsApplicumulator is created, used to parse a
	  * run through Grid (several hinters) and then forgotten, because it's
	  * light to construct, but heavy to hold onto, coz it has a Grid.
	  *
	  * @param isStringy should I populate the public SB StringBuilder with the
	  *  hint.toString() of each hint that is added to me when I apply it?
	  */
	public HintsApplicumulator(boolean isStringy) {
		if ( isStringy ) // NEVER in anger
			this.SB = new StringBuilder(512); // just a guess
		else
			this.SB = null;
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
		hint.SB = SB; // make Cell.set append subsequent singles (if SB!=null)
		// nb: HintsApplicumulator ALWAYS applies with AUTOSOLVE=true!
		numElims += hint.applyQuitely(true, grid); // don't eat exceptions!
		hint.SB = null;
		++numHints; // keep count
		return false; // false means hinter keeps searching
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
	public boolean any() {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void sort(Comparator<AHint> comparator) {
		// a no-op
	}

	@Override
	public void removeAll(List<AHint> toRemove) {
		// a no-op
	}

	@Override
	public List<? extends AHint> getList() {
		throw new UnsupportedOperationException("Not supported.");
	}

}
