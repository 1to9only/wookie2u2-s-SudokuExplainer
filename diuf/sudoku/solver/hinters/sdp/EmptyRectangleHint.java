/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.REGION_TYPE_NAMES;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import java.util.Iterator;
import java.util.Set;

/**
 * EmptyRectangleHint encapsulates the data required to display an Empty
 * Rectangle hint.
 * @author Keith Corlett Mar 28
 */
public class EmptyRectangleHint extends AHint  {

	// nb: package visible so that EmptyRectangle can compare hints.
	final int redValue;
	final boolean isDual;

	public EmptyRectangleHint(Grid grid, IHinter hinter, int value
			, ARegion[] bases, ARegion[] covers, Pots greens, Pots blues
			, Pots reds) {
		super(grid, hinter, reds, greens, null, blues, bases, covers);
		this.redValue = value;
		this.isDual = false;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int unusedViewNum) {
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
	public StringBuilder toStringImpl() {
		return Frmt.ids(SB(64).append(getHintTypeName()).append(COLON_SP)
		, greens.keySet(), CSP, CSP).append(ON).append(redValue);
	}

	@Override
	public String toHtmlImpl() {
		int indice = reds.firstKey();
		final String id1, id2;
		{ // this block just localises the idioterator
			// this funkyness is to allow for a rooted hint!
			Iterator<Integer> it = greens.keySet().iterator();
			if ( it.hasNext() ) {
				id1 = CELL_IDS[it.next()];
				id2 = it.hasNext() ? CELL_IDS[it.next()] : "#ERR";
			} else id1 = id2 = "#ERR";
		}
		return Html.produce(this, "EmptyRectangleHint.html"
		, Integer.toString(redValue)		//{0}
		, bases[0].label					// 1 erBox
		, CELL_IDS[indice]					// 2
		, covers[0].label					// 3 erRow
		, REGION_TYPE_NAMES[covers[0].rti]	// 4
		, covers[1].label					// 5 assisting 1
		, REGION_TYPE_NAMES[covers[1].rti]	// 6
		, covers[2].label					// 7 assisting 2
		, id1								// 8 assisting cell1
		, id2								// 9 assisting cell2
		, debugMessage						//10
		, reds.toString()					//11
		);
	}

}
