/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_TYPE_NAMES;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;

/**
 * SkyscraperHint contains the hint data for display in the GUI, and also
 * produces the HTML and the String representation of a Skyscraper hint.
 * @author Keith Corlett 2020 Mar 26
 */
public class SkyscraperHint extends AHint  {

	private final int valueToRemove;
	public SkyscraperHint(Grid grid, IHinter hinter, int value, ARegion[] bases
			, ARegion[] covers, Pots redPots, Pots green) {
		super(grid, hinter, redPots, green, null, null, bases, covers);
		this.valueToRemove = value;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+Integer.toString(valueToRemove);
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return Frmt.ids(SB(64).append(getHintTypeName()).append(COLON_SP)
		, greens.keySet(), CSP, CSP).append(ON).append(valueToRemove);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "SkyscraperHint.html"
		, Integer.toString(valueToRemove)	// {0}
		, bases[0].label					//  1
		, bases[1].label					//  2
		, REGION_TYPE_NAMES[covers[0].rti]	//  3
		, reds.toString()					//  4
		);
	}

}
