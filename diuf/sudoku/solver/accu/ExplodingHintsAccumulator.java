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
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.Validator;

/**
 * For development only: Report an invalid hint, and throw UnsolvableException.
 * A hint is invalid if it removes a maybe that is the value of this cell in
 * the solution; or if it sets a cell to other than the solution value. Pretty
 * obviously this requires us to know the solution of the puzzle WHILE we are
 * calculating the solution to the puzzle. Thanks to Donald Knuth thats not a
 * problem, theres a REALLY fast way to calculate the solution to any Sudoku
 * called BruteForce.
 *
 * @see diuf.sudoku.solver.checks.BruteForce.
 *
 * @author Keith Corlett 2023-06-01
 */
public class ExplodingHintsAccumulator extends AAccumulatorWrapper {

	public ExplodingHintsAccumulator(IAccumulator accu) {
		super(accu);
	}

	@Override
	public boolean add(final AHint hint) {
		// do not check Warnings
		if ( !(hint instanceof AWarningHint) ) {
			final Grid grid = hint.getGrid();
			if ( grid != null ) {
				// first the setPots (set cells), if any
				final Pots setPots = hint.getSetPots();
				if ( setPots!=null && !Validator.validOns(grid, setPots) ) {
					hint.setIsInvalid(true); // MUST set before toFullString
					Validator.reportInvalidHint(hint.hinter.toString(), grid, hint.toFullString());
					throw new UnsolvableException("Invalid hint");
				}
				// then the redPots (eliminations), if any
				final Pots redPots = hint.getRedPots(0);
				if ( redPots!=null && !Validator.validOffs(grid, redPots) ) {
					hint.setIsInvalid(true); // MUST set before toFullString
					Validator.reportInvalidHint(hint.hinter.toString(), grid, hint.toFullString());
					throw new UnsolvableException("Invalid SetPots hint");
				}
			}
		}
		// add any invalid hints, that are now marked as invalid!
		return accu.add(hint);
	}

}
