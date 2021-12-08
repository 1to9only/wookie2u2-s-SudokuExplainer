/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;

/**
 * A WWingHint stores the hint data to pass to the GUI for display, and also
 * constructs the hints string representation, clue html, and hint html.
 */
public final class WWingHint extends AHint  {

	private final Cell cellA, cellB, wCellA, wCellB;
	private final int value0, value1;
	private final ARegion wABCommonRegion;

	public WWingHint(AHinter hinter, int value0, int value1
			, Cell cellA, Cell cellB, Cell wCellA, Cell wCellB
			, Pots greenPots, Pots bluePots, Pots redPots) {
		super(hinter, AHint.INDIRECT, null, 0, redPots, greenPots, null
				, bluePots, null, null);
		this.value0 = value0;
		this.value1 = value1;
		this.cellA = cellA;
		this.cellB = cellB;
		this.wCellA = wCellA;
		this.wCellB = wCellB;
		this.wABCommonRegion = Regions.common(wCellA, wCellB);
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(wABCommonRegion);
	}

	@Override
	public Set<Cell> getAquaCells(int viewNumUnused) {
		return new MyLinkedHashSet<>(cellA, cellB, wCellA, wCellB);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+value0+AND+value1;
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(greens.keySet()))
		  .append(ON).append(value0).append(AND).append(value1)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "WWingHint.html"
			, cellA.id						//{0}
			, cellB.id						// 1
			, wCellA.id						// 2
			, wCellB.id						// 3
			, Integer.toString(value0)		// 4
			, Integer.toString(value1)		// 5
			, wABCommonRegion				// 6
			, Frmu.and(reds.keySet())		// 7
			, reds.toString()				// 8
		);
	}

}
