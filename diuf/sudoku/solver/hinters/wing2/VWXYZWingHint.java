/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 * This class was boosted from Sukaku.
 */
package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;


/**
 * VWXYZWingHint is the data transfer object (DTO) for a VWXYZ-Wing hint.
 * <p>
 * I extend ABigWingHint which implements all the common crap.
 */
public class VWXYZWingHint extends ABigWingHint {

	public VWXYZWingHint(AHinter hinter, Pots redPots, int wingCands, Cell yz
			, XZ yzVs, Cell[] wing, Cell[] all) {
		super(hinter, redPots, wingCands, yz, yzVs, wing, all);
	}

	@Override
	public String toHtmlImpl() {
		final String filename = both
				? "VWXYZWing2Hint.html"
				: "VWXYZWingHint.html";
		return Html.format(Html.load(this, filename)
			, wing[0].id			// {0}
			, wing[1].id			//  1
			, wing[2].id			//  2
			, wing[3].id			//  3
			, yz.id					//  4
			, z						//  5
			, getWingValue(0)		//  6
			, getWingValue(1)		//  7
			, getWingValue(2)		//  8
			, x						//  9
			, ""					// 10 biggestCardinality
			, ""					// 11 wingSize
			, getSuffix()			// 12
			, "box, row, or col"	// 13
		);
	}

}
