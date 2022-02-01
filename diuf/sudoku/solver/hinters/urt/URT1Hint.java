/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;

public final class URT1Hint extends AURTHint {

	public URT1Hint(AHinter hinter, Cell[] loop, int loopSize
			, int v1, int v2, Pots reds) {
		super(1, hinter, loop, loopSize, v1, v2, reds);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT1Hint.html"
			, getTypeName()				//{0}
			, v1						// 1
			, v2						// 2
			, Frmu.csv(loopSize, loop)	// 3
			, reds.firstKey().id		// 4
			, reds.toString()			// 5
		);
	}
}