/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import java.util.BitSet;
import diuf.sudoku.solver.accu.IAccumulator;


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
		BitSet seen = new BitSet(10);
		for ( Cell cell : grid.cells )
			seen.set(cell.value);
		seen.clear(0); // filter out 0, the value of empty cells
		int numValues = seen.cardinality();
		if ( numValues >= 8 )
			return false; // 0 or 1 missing values is OK

		// Two or more values are missing, so build a string of missing values
		StringBuilder sb = new StringBuilder((9-numValues)*3);
		for ( int v=1, cnt=0; v<10; ++v )
			if ( !seen.get(v) ) {
				if ( ++cnt > 1 )
					sb.append(", ");
				sb.append(v);
			}
		accu.add(new WarningHint(this
				, "Distinct values "+numValues+" is less than 8"
				, "TooFewValues.html", sb.toString())
		);
		return true;
	}

}
