/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.List;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;

/**
 * SkyscraperHint contains the hint data for display in the GUI, and also
 * produces the HTML and the String representation of a Skyscraper hint.
 * @author Keith Corlett 2020 Mar 26
 */
public class SkyscraperHint extends AHint  {

	private final int valueToRemove;
	public SkyscraperHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots redPots, Pots green) {
		super(hinter, redPots, green, null, null, bases, covers);
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
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(greens.keySet()))
		  .append(ON).append(Integer.toString(valueToRemove))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "SkyscraperHint.html"
				, Integer.toString(valueToRemove)	// {0}
				, bases.get(0).id			//  1
				, bases.get(1).id			//  2
				, covers.get(0).typeName	//  3
				, reds.toString()		//  4
		);
	}

}
