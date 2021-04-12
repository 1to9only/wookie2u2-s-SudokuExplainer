/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;

/**
 *
 * @author User
 */
public class TwoStringKiteHint extends AHint implements IActualHint {

	private final int redValue;
	private final Cell[] rowPair;
	private final Cell[] colPair;
	public TwoStringKiteHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots orangePots, Pots bluePots, Pots redPots
			, Cell[] rowPair, Cell[] colPair) {
		// nb: what are normally greens are oranges here
		super(hinter, redPots, null, orangePots, bluePots, bases, covers);
		this.redValue = value;
		this.rowPair = rowPair;
		this.colPair = colPair;
	}

	@Override
	public Set<Grid.Cell> getAquaCells(int unusedViewNum) {
		// nb: what are normally greens are oranges here
		return oranges.keySet();
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on "+Frmt.and(new Values(oranges.values()));
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName())
		  .append(": ").append(Frmt.csv(oranges.keySet()))
		  .append(" on ").append(Integer.toString(redValue));
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		Cell redCell = redPots.firstKey();
		return Html.produce(this, "TwoStringKiteHint.html"
				, Integer.toString(redValue)	//{0}
				, covers.get(0).id				// 1
				, covers.get(1).id				// 2
				, bases.get(0).id				// 3
				, redCell.id					// 4
				, rowPair[0].id					// 5
				, rowPair[1].id					// 6
				, colPair[0].id					// 7
				, colPair[1].id					// 8
				, redPots.toString()			// 9
		);
	}

}
