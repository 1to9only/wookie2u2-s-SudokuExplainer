/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Frmt;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An Applicumulator is by "apply" out of "accumulator". A vegan lion.
 * <p>
 * Within {@link diuf.sudoku.solver.checks.BruteForce} a HintsApplicumulator is
 * used by {@link diuf.sudoku.solver.hinters.lock.Locking} to apply each
 * Pointing and Claiming hint as soon as it is found, so that all hints are
 * applied in one pass through the grid (for efficiency); then Locking adds a
 * single {@link diuf.sudoku.solver.accu.SummaryHint} to the real IAccumulator
 * to pass the total number of maybes eliminated back to the calling
 * {@link AHint#applyImpl} method, in order to keep score.
 * <p>
 * Note that there is nothing specific to Locking here, it's just that Locking
 * is currently the only place I am used, because Locking is were I am needed
 * most, coz Locking tends to cause Locking, more than other hint types do.
 * This is observational only. No supporting data. May be wrong.
 * <p>
 * {@link diuf.sudoku.solver.checks.BruteForce#solveLogically} builds-up a
 * {@link diuf.sudoku.solver.accu.SummaryHint} with the agglomerate of all
 * hints found and applied (through me) in a run. I track the number of hints
 * found, the number of eliminations, and a StringBuilder of the line-separated
 * hint.toFullString of all hints applied, to pass on to the summary hint.
 * <p>
 * Note that for speed, the public grid should be set post-construction, rather
 * than have every single bloody hint-type lookup its own grid.
 *
 * @author Keith Corlett 2016
 */
public final class HintsApplicumulator implements IAccumulator {

	public int numHints = 0;
	public int numElims = 0;
	// we append each hint.toFullString() to this StringBuilder when != null
	public final StringBuilder funnySB;
	// set me to avoid getting the grid for every single bloody hint
	public Grid grid;

	private Pots pots;

	 /**
	  * Constructor.
	  * <p>
	  * <b>Note</b> that, for speed, the grid field is set post-construction,
	  * rather than have every single hint lookup it is own grid. Null it in a
	  * finally block when you are finished with that grid or this apcu.
	  * <p>
	  * By design, most HintsApplicumulators are single-use instances: by
	  * which I mean that a HintsApplicumulator is created, used to parse a
	  * run through Grid (several hinters) and then forgotten, because it is
	  * light to construct, but heavy to hold onto, coz it has a Grid.
	  *
	  * @param isStringy should I populate the public SB StringBuilder with the
	  *  hint.toString() of each hint that is added to me when I apply it?
	  */
	public HintsApplicumulator(boolean isStringy) {
		if ( isStringy ) // NEVER in anger
			this.funnySB = SB(512); // just a guess
		else
			this.funnySB = null;
	}

	@Override
	public boolean isSingle() { return false; }

	/**
	 * Runs at the start of each solveLogically, to clear my buffers.
	 */
	@Override
	public void clear() {
		numHints = 0;
		numElims = 0;
		if ( funnySB != null )
			funnySB.setLength(0);
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
	public boolean add(final AHint hint) {
		if ( hint == null )
			return false;
		if ( funnySB != null ) { // ie constructor @param isStringy = true
			if ( funnySB.length() > 0 )
				funnySB.append(Frmt.NL);
			funnySB.append(hint.toFullString());
		}
		hint.funnySB = funnySB; // Cell.set appends subsequent singles
		if ( pots != null )
			pots.put(hint.cell.indice, VSHFT[hint.value]);
		// nb: HintsApplicumulator ALWAYS applies with AUTOSOLVE=true!
		numElims += hint.applyQuitely(true, grid); // do not eat exceptions!
		hint.funnySB = null;
		++numHints; // keep count
		return false; // false means hinter keeps searching
	}

	@Override
	public boolean addAll(final Collection<? extends AHint> hints) {
		boolean any = false;
		if ( hints==null || hints.size()==0 )
			return any;
		for ( AHint h : hints )
			// NOTE use add in case it is overridden!
			any |= add(h);
		return any;
	}

	@Override
	public AHint poll() {
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
	public void sort(final Comparator<AHint> comparator) {
		// a no-op
	}

	@Override
	public void removeAll(final List<AHint> toRemove) {
		// a no-op
	}

	@Override
	public List<AHint> getList() {
		throw new UnsupportedOperationException("Not supported.");
	}

	public void setSetPots(Pots pots) {
		this.pots = pots;
	}

	public Pots getSetPots() {
		return this.pots;
	}

}
