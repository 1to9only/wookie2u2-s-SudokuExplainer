/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.Values.BITS9;

/**
 * TooFewValues implements the {@link Tech#TooFewValues} Sudoku puzzle
 * validator. A valid Sudoku can omit only one value. If more than one value
 * does not appear in the given puzzle then produce a warning hint.
 */
public final class TooFewValues extends AWarningHinter {

	public TooFewValues() {
		super(Tech.TooFewValues);
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		if ( grid.enoughValues )
			return false;
		int cands = 0;
		for ( Cell cell : grid.cells )
			cands |= VSHFT[cell.value]; // cell.value 0 is a no-op
		int count = Integer.bitCount(BITS9 & ~cands);
		if ( count > 1 ) {
			final StringBuilder sb = SB((REGION_SIZE-count)<<2);
			int cnt = 0;
			for ( int v : VALUESES[BITS9 & ~cands] ) {
				if ( ++cnt > 1 )
					sb.append(CSP);
				sb.append(v);
			}
			accu.add(new WarningHint(this, "More than one value is missing", "TooFewValues.html", sb.toString()));
			return true;
		} else {
			grid.enoughValues = true;
			return false;
		}
	}

}
