/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Set;


/**
 * A WWingHint stores the hint data to pass to the GUI for display, and also
 * constructs the hints string representation, clue html, and hint html.
 */
public final class WWingHint extends AHint implements IActualHint {

	private final Cell cellA, cellB, wCellA, wCellB;
	private final int value0, value1;

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
	}

	@Override
	public Set<Cell> getAquaCells(int viewNumUnused) {
		return new MyLinkedHashSet<>(cellA, cellB, wCellA, wCellB);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on "+value0+" and "+value1;
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName()).append(": ")
		  .append(Frmt.csv(greens.keySet()))
		  .append(" on ").append(value0)
		  .append(" and ").append(value1);
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "WWingHint.html"
				, cellA.id		// {0}
				, cellB.id		//  1
				, wCellA.id		//  2
				, wCellB.id		//  3
				, Integer.toString(value0)			// 4
				, Integer.toString(value1)			// 5
				, Grid.commonRegion(wCellA, wCellB) // 6
				, Frmt.and(redPots.keySet())		// 7
		);
	}

}
