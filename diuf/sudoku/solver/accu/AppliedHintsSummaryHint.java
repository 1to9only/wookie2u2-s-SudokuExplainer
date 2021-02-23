/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;


/**
 * A {@code AppliedHintsSummaryHint} is produced by
 * {@link diuf.sudoku.solver.hinters.lock.Locking}
 * AFTER we've used the {@code HintsApplicumulator} to apply (not accumulate) 
 * all the LockingHint's, in order to pass the total-numElims back to the
 * {@link diuf.sudoku.solver.AHint#apply}
 * method through the "standard" HintsAccumulator.
 * <p>Note that there's nothing specific to Locking in this class, it's just
 * that Locking is currently the only place I'm used. So if you want to do the
 * same thing with other rules then put me back in the
 * <tt>diuf.sudoku.solver.accu</tt> package, and go for it.
 *
 * @author Keith Corlett 2016
 */
public final class AppliedHintsSummaryHint extends AHint {

	private final int numElims;
	private final String toString;
	
	/**
	 * The AppliedHintsSummaryHint constructor.
	 * @param numElims
	 * @param apcu the HintsApplicumulator used to apply the hints as they were
	 * added by the hinter. We toString from it if it!=null and its sb!=null.
	 * toString = applied hints toFullStrings; BUT RecursiveAnalyser doesn't
	 * hint.toString() so it's HintsApplicumulator doesn't build the buffer,
	 * so apcu.sb==null within RecursiveAnalyser.
	 */
	public AppliedHintsSummaryHint(int numElims, HintsApplicumulator apcu) {
		super(null, AHint.INDIRECT, null, 0, null, null, null, null, null, null);
		this.numElims = numElims;
		this.toString = apcu!=null && apcu.sb!=null && apcu.numHints>0
				? apcu.sb.toString()
				: "";
	}

	@Override
	public int apply(boolean isAutosolvingUnused, boolean isNoisyUnused) {
		return numElims;
	}

	@Override
	public int getNumElims() {
		return numElims;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return null;
	}

	@Override
	public double getDifficulty() {
		return 0.0;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "Summary";
	}

	@Override
	public String toHtmlImpl() {
		return "";
	}

	@Override
	public String toStringImpl() {
		return toString;
	}
}