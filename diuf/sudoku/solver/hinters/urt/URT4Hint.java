/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;


public final class URT4Hint extends AURTHint {

	private final Cell c1;
	private final Cell c2;
	private final int lockVal;
	private final int valueToRemove;
	private final Grid.ARegion region;

	public URT4Hint(Grid grid, IHinter hinter, Cell[] loop, int loopSize, int lockVal
			, int valueToRemove, Pots redPots, Cell c1, Cell c2
			, ARegion region) {
		super(grid, 4, hinter, loop, loopSize, lockVal, valueToRemove, redPots);
		this.c1 = c1;
		this.c2 = c2;
		this.lockVal = lockVal;
		this.valueToRemove = valueToRemove;
		this.region = region;
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(region);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT4Hint.html"
			, getTypeName()				//{0}
			, lockVal					// 1
			, valueToRemove				// 2
			, Frmu.csv(loopSize, loop)	// 3
			, c1.id						// 4
			, c2.id						// 5
			, region.label					// 6
			, reds.toString()			// 7
		);
	}
}
