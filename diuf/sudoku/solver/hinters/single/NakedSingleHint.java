/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.solver.ADirectHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.Frmt.COLON_SP;

/**
 * A NakedSingleHint is the data-structure representing a naked single hint.
 * A "naked single" is a Cell which has just one potential value (maybe)
 * remaining. A Cell which has one potential value is set to that value when
 * this hint is applied. The apply method is in my superclass: ADirectHint.
 *
 * @author Keith Corlett
 */
public final class NakedSingleHint extends ADirectHint {

	public NakedSingleHint(Grid grid, IHinter hinter, Cell cell, int value) {
		super(grid, hinter, null, cell, value);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " at <b>" + cell.id + "</b>";
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP).append(super.toStringImpl());
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSingleHint.html"
				, Integer.toString(value)	// {0}
				, cell.id					//  1
		);
	}

}
