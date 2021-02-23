/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;


public final class URT4Hint extends AURTHint {

	private final Cell c1;
	private final Cell c2;
	private final int lockVal;
	private final int rmvVal;
	private final Grid.ARegion region;

	public URT4Hint(UniqueRectangle hinter, List<Cell> loop, int lockVal
			, int rmvVal, Pots redPots, Cell c1, Cell c2, ARegion region) {
		super(4, hinter, loop, lockVal, rmvVal, redPots);
		this.c1 = c1;
		this.c2 = c2;
		this.lockVal = lockVal;
		this.rmvVal = rmvVal;
		this.region = region;
	}

	@Override
	public Pots getReds(int viewNum) {
		return super.redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT4Hint.html"
			, getTypeName()		// {0}
			, lockVal			//  1
			, rmvVal			//  2
			, Frmt.csv(loop)	//  3
			, c1.id				//  4
			, c2.id				//  5
			, region.id			//  6
		);
	}
}