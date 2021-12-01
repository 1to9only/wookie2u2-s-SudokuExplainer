/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;

/**
 * EmptyRectangleHint encapsulates the data required to display an Empty
 * Rectangle hint.
 * @author Keith Corlett Mar 28
 */
public class EmptyRectangleHint extends AHint  {

	// nb: package visible so that EmptyRectangle can compare hints.
	final int redValue;
	final boolean isDual;
	final String debugMessage;

	public EmptyRectangleHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots greens, Pots blues
			, Pots redPots, String debugMessage) {
		super(hinter, redPots, greens, null, blues, bases, covers);
		this.redValue = value;
		this.isDual = false;
		this.debugMessage = debugMessage;
	}

	@Override
	public Set<Grid.Cell> getAquaCells(int unusedViewNum) {
		return greens.keySet();
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += ON+redValue;
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(greens.keySet()))
		  .append(ON).append(redValue)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		Cell redCell = reds.firstCell();
		final String id1, id2;
		{ // this block just localises 'it'
			Iterator<Cell> it = greens.keySet().iterator();
			id1 = it.next().id;
			id2 = it.next().id;
		}
		return Html.produce(this, "EmptyRectangleHint.html"
				, Integer.toString(redValue) //{0}
				, bases.get(0).id			 // 1 erBox
				, redCell.id				 // 2
				, covers.get(0).id			 // 3 erRow
				, covers.get(0).typeName	 // 4
				, covers.get(1).id			 // 5 assisting 1
				, covers.get(2).typeName	 // 6
				, covers.get(2).id			 // 7 assisting 2
				, id1						 // 8 assisting cell1
				, id2						 // 9 assisting cell2
				, debugMessage				 //10 identifies which method was used to find this hint: "A:ROW", or "B:COL"
				, reds.toString()		 //11
		);
	}

}
