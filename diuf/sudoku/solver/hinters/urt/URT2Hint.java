/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;

public final class URT2Hint extends AURTHint {

	private final Cell[] cellsWithExtraValues;
	private final int valueToRemove;

	public URT2Hint(AHinter hinter, Cell[] loop, int loopSize, int v1, int v2
			, Pots redPots, Cell[] cellsWithExtraValues, int valueToRemove) {
		super(2, hinter, loop, loopSize, v1, v2, redPots);
		this.cellsWithExtraValues = cellsWithExtraValues;
		this.valueToRemove = valueToRemove;
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots(cellsWithExtraValues.length, 1F);
			for ( Cell c : cellsWithExtraValues )
				pots.put(c, VSHFT[valueToRemove]); // orange
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT2Hint.html"
			, getTypeName()			//{0}
			, v1					// 1
			, v2					// 2
			, Frmu.csv(loopSize, loop)		// 3
			, Frmu.or(cellsWithExtraValues)	// 4
			, Frmu.and(cellsWithExtraValues) // 5
			, valueToRemove			// 6
			, reds.toString()	// 7
		);
	}
}