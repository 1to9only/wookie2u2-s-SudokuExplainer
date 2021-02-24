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
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * EmptyRectangleHint encapsulates the data required to display an Empty
 * Rectangle hint.
 * @author Keith Corlett Mar 28
 */
public class EmptyRectangleHint extends AHint implements IActualHint {

	// nb: package visible so that EmptyRectangle can compare hints.
	final int redValue;
	final boolean isDual;
	final String debugMessage;

	public EmptyRectangleHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots orangePots, Pots bluePots
			, Pots redPots, String debugMessage) {
		// nb: what are normally greens are oranges here
		super(hinter, redPots, null, orangePots, bluePots, bases, covers);
		this.redValue = value;
		this.isDual = false;
		this.debugMessage = debugMessage;
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
			s += " on "+redValue;
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName())
		  .append(": ").append(Frmt.csv(oranges.keySet()))
		  .append(" on ").append(redValue);
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		Cell redCell = redPots.firstKey();
		final String id1, id2;
		{ // this block just localises 'it'
			Iterator<Cell> it = oranges.keySet().iterator();
			id1 = it.next().id;
			id2 = it.next().id;
		}
		return Html.produce(this, "EmptyRectangleHint.html"
				, Integer.toString(redValue)// {0}
				, bases.get(0).id			//  1 erBox
				, redCell.id				//  2
				, covers.get(0).id			//  3 erRow
				, covers.get(0).typeName	//  4 "row"
				, covers.get(1).id			//  5 assisting col
				, covers.get(1).typeName	//  6 "col"
				, covers.get(3).id			//  7 assisting row
				, id1						//  8 assisting cell1
				, id2						//  9 assisting cell2
				, debugMessage				// 10 identifies which method was used to find this hint: "A:ROW", or "B:COL"
				, redPots.toString()		// 11
		);
	}

}
