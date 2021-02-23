/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;


public final class URT1Hint extends AURTHint {

	private final Cell target;

	public URT1Hint(UniqueRectangle hinter, List<Cell> loop, int v1, int v2
			, Pots redPots, Cell target) {
		super(1, hinter, loop, v1, v2, redPots);
		this.target = target;
	}

	@Override
	public Pots getReds(int viewNum) {
		return new Pots(target, new Values(v1, v2));
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT1Hint.html"
				, getTypeName()		// {0}
				, v1				//  1
				, v2				//  2
				, Frmt.csv(loop)	//  3
				, target.id			//  4
		);
	}
}