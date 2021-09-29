/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.utils.Frmt.COMMA_SP;


/**
 * Valid Sudokus have 8 or 9 distinct clue values. If at least two
 * values do not appear in the grid then produce a warning message.
 */
public final class TooFewValues extends AWarningHinter {

	public TooFewValues() {
		super(Tech.TooFewValues);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( grid.enoughValues )
			return false;
		// Does this puzzle contain atleast 8 distinct values?
		int seen = 0;
		for ( Cell cell : grid.cells )
			seen |= VSHFT[cell.value]; // 0 safe!
		int numValues = Integer.bitCount(seen);
		if ( numValues >= 8 ) {
			grid.enoughValues = true;
			return false; // 0 or 1 missing values is OK
		}
		// Two or more values are missing
		StringBuilder sb = new StringBuilder((9-numValues)*3);
		int cnt = 0;
		for ( int v : VALUESES[VALL & ~seen] ) {
			if ( ++cnt > 1 )
				sb.append(COMMA_SP);
			sb.append(v);
		}
		accu.add(new WarningHint(this
				, "Distinct values "+numValues+" is less than 8"
				, "TooFewValues.html", sb.toString())
		);
		return true;
	}

}
