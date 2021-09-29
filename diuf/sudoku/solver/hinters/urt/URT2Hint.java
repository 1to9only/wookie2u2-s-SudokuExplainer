/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.List;


public final class URT2Hint extends AURTHint {

	private final Cell[] cellsWithExtraValues;
	private final int valueToRemove;

	public URT2Hint(UniqueRectangle hinter, List<Cell> loop, int v1, int v2
			, Pots redPots, Cell[] cellsWithExtraValues, int valueToRemove) {
		super(2, hinter, loop, v1, v2, redPots);
		this.cellsWithExtraValues = cellsWithExtraValues;
		this.valueToRemove = valueToRemove;
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots(cellsWithExtraValues.length, 1F);
			for ( Cell c : cellsWithExtraValues )
				pots.put(c, new Values(valueToRemove)); // orange
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
			, Frmu.csv(loop)		// 3
			, Frmu.or(cellsWithExtraValues)	// 4
			, Frmu.and(cellsWithExtraValues)	// 5
			, valueToRemove			// 6
			, redPots.toString()	// 7
		);
	}
}