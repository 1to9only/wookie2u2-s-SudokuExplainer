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
 * UVWXYZWingHint is the data transfer object (DTO) for a UVWXYZ-Wing hint.
 * <p>
 * I extend ABigWingHint which implements all the common crap.
 */
public class UVWXYZWingHint extends ABigWingHint {

	// this, reds, yz, yzVs, wingCands, wing, all
	public UVWXYZWingHint(AHinter hinter, Pots redPots, Cell yz, XZ yzVs
			, int wingCands, Cell[] wing, Cell[] all) {
		super(hinter, redPots, wingCands, yz, yzVs, wing, all);
	}

	@Override
	public String toHtmlImpl() {
		final String filename = both
				? "UVWXYZWing2Hint.html"
				: "UVWXYZWingHint.html";
		return Html.format(Html.load(this, filename)
			, wing[0].id			//{0}
			, wing[2].id			// 1
			, wing[3].id			// 2
			, wing[4].id			// 3
			, yz.id					// 4
			, z						// 5
			, getWingValue(0)		// 6
			, getWingValue(1)		// 7
			, getWingValue(2)		// 8
			, x						// 9
			, ""					//10 biggestCardinality
			, ""					//11 wingSize
			, getSuffix()			//12
			, "box, row, or col"	//13
			, wing[1].id			//14
		);
	}

}
