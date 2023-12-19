/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import diuf.sudoku.utils.Frmu;
import static diuf.sudoku.utils.Frmt.NULL_STRING;

/**
 * A SummaryHint is produced by {@link diuf.sudoku.solver.hinters.lock.Locking}
 * AFTER we have used the {@link HintsApplicumulator} to apply (not accumulate)
 * all the hints, to pass the total-numElims back to {@link AHint#applyImpl}
 * via the "standard" HintsAccumulator.
 * <p>
 * Note that there is nothing specific to Locking here, it is only that Locking
 * is currently the only place I am used, because Locking is were I am needed,
 * because Locking tends to cause Locking, far more than any other hint type.
 *
 * @author Keith Corlett 2016
 */
public final class SummaryHint extends AHint {

	private final int numElims;
	private final StringBuilder toString;

	/**
	 * Constructor.
	 *
	 * @param source the name of the source hinter
	 * @param numElims number of eliminations
	 * @param apcu the HintsApplicumulator used to apply the hints as they were
	 * added by the hinter. We toString from it if it!=null and its sb!=null.
	 * toString = applied hints toFullStrings; BUT BruteForce does not
	 * hint.toString() so it is HintsApplicumulator does not build the buffer,
	 * so apcu.sb==null within BruteForce.
	 */
	public SummaryHint(final String source, final int numElims, final HintsApplicumulator apcu) {
		super(null, null, AHint.INDIRECT, null, 0, null, null, null, null, null, null);
		this.numElims = numElims;
		// build-up the toString string
		final StringBuilder sb = SB(1024);
		sb.append(source).append("->SummaryHint#");
		if ( apcu == null )
			sb.append(NULL_STRING);
		else {
			sb.append(Integer.toString(apcu.numHints));
			if ( apcu.funnySB != null )
				sb.append(NL).append(apcu.funnySB);
		}
		toString = sb;
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		return numElims;
	}

	@Override
	public int getNumElims() {
		return numElims;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		return null;
	}

	@Override
	public int getDifficulty() {
		return 0;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "Summary";
	}

	@Override
	public String toHtmlImpl() {
		return EMPTY_STRING;
	}

	@Override
	public StringBuilder toStringImpl() {
		return toString;
	}

}
