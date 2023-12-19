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
import diuf.sudoku.solver.LogicalSolver;
import static diuf.sudoku.solver.hinters.Validator.reportInvalidHint;
import static diuf.sudoku.solver.hinters.Validator.validOffs;
import static diuf.sudoku.solver.hinters.Validator.validOns;

/**
 * For the sake of argument lets say that AlignedExclusion isAVeryNaughtyBoy()!
 * The {@link LogicalSolver} wraps the usual {@link IAccumulator} in a
 * ValidatingHintsAccumulator (an instance of this class) to
 * {@link diuf.sudoku.solver.hinters.Validator} each AlignedExclusionHint.
 * <p>
 * When done you restore isAVeryNaughtyBoy() to return false. There is testcase
 * that fails whenever any hinter is Very/VeryNaughty, so that Naughtyness cant
 * make it into a release-build (we ALWAYS run all the testcases as part of a
 * build).
 * <p>
 * All other hinters switched-over to the ValidatingHintsAccumulator, reducing
 * overall Validator code and eradicating "${HINTER}_USES_VALIDATOR" constants
 * from Validator. I rolled my sleeves up and did the lot.
 * <p>
 * Note that AHint now implements getGrid() to return the grid upon which this
 * hint was based (even if it means copying that grid). There is no memory leak
 * (despite my fears) because the Grid outlives Hints upon it; no GC problems.
 *
 * @author Keith Corlett 2023-05-25
 */
public class ValidatingHintsAccumulator extends AAccumulatorWrapper {

	public ValidatingHintsAccumulator(IAccumulator accu) {
		super(accu);
		assert accu instanceof HintsAccumulator
			|| accu instanceof SingleHintsAccumulator
				: "Unknown accu: "+accu;
	}

	@Override
	public boolean add(final AHint hint) {
		// do not check Warnings
		if ( hint!=null && !(hint instanceof AWarningHint) ) {
			final Grid grid = hint.getGrid();
			if ( grid != null ) {
				// first the setPots (set cells), if any
				final Pots sets = hint.getSetPots();
				if ( sets!=null && !validOns(grid, sets) ) {
					hint.invalidate(); // set BEFORE toFullString
					reportInvalidHint(hint.hinter.toString(), grid, hint.toFullString());
				}
				// then the reds (eliminations, not getReds(0) ya dummy) if any
				final Pots reds = hint.reds;
				if ( reds!=null && !validOffs(grid, reds) ) {
					hint.invalidate(); // set BEFORE toFullString
					reportInvalidHint(hint.hinter.toString(), grid, hint.toFullString());
				}
			}
		}
		// add any invalid hints, that are now marked as invalid!
		return accu.add(hint);
	}

}
