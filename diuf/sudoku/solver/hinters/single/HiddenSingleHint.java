/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.solver.ADirectHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.Frmt.COLON_SP;

/**
 * A HiddenSingleHint represents a hidden single hint. A "hidden single" is the
 * last remaining Cell in a region which may be a given value. The Cell is set
 * to that value when this hint is applied. The apply method is in my
 * superclass: ADirectHint.
 */
public final class HiddenSingleHint extends ADirectHint {

	public HiddenSingleHint(Grid grid, IHinter hinter, ARegion region
			, Cell cell, int value) {
		super(grid, hinter, region, cell, value);
	}

	@Override
	public int getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		int d = super.getDifficulty();
		// plus bonus 0.1 for being in a row or column
		if(!(getRegion() instanceof Box)) d += 1;
		// 1.2 for hidden single in box
		// 1.3 for hidden single in row or column
		return d;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in <b1>" + getRegion().label + "</b1>";
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP).append(super.toStringImpl());
	}

	@Override
	public String toHtmlImpl() {
		String regionID = getFirstRegionId();
		if(regionID==null) regionID="ignato";
		return Html.produce(this, "HiddenSingleHint.html"
				, cell.id					// {0}
				, Integer.toString(value)	//  1
				, regionID					//  2
		);
	}

}
