/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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

	public EmptyRectangleHint(AHinter hinter, int value, ARegion[] bases
			, ARegion[] covers, Pots greens, Pots blues, Pots reds) {
		super(hinter, reds, greens, null, blues, bases, covers);
		this.redValue = value;
		this.isDual = false;
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
		Cell victim = reds.firstKey();
		final String id1, id2;
		{ // this block just localises 'it'
			// this funkyness is to allow for a rooted hint!
			Iterator<Cell> it = greens.keySet().iterator();
			if ( it.hasNext() ) {
				id1 = it.next().id;
				id2 = it.hasNext() ? it.next().id : "#ERR";
			} else id1 = id2 = "#ERR";
		}
		return Html.produce(this, "EmptyRectangleHint.html"
			, Integer.toString(redValue) //{0}
			, bases[0].id		 // 1 erBox
			, victim.id			 // 2
			, covers[0].id		 // 3 erRow
			, covers[0].typeName // 4
			, covers[1].id		 // 5 assisting 1
			, covers[2].typeName // 6
			, covers[2].id		 // 7 assisting 2
			, id1				 // 8 assisting cell1
			, id2				 // 9 assisting cell2
			, debugMessage		 //10
			, reds.toString()	 //11
		);
	}

}
