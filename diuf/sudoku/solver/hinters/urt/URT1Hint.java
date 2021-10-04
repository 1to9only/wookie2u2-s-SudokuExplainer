/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.Frmu;
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
		return new Pots(target, VSHFT[v1]|VSHFT[v2], false);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT1Hint.html"
			, getTypeName()		//{0}
			, v1				// 1
			, v2				// 2
			, Frmu.csv(loop)	// 3
			, target.id			// 4
			, redPots.toString()// 5
		);
	}
}