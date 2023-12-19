/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Set;

/**
 * A WWingHint stores the hint data to pass to the GUI for display, and also
 * constructs the hints string representation, clue html, and hint html.
 */
public final class WWingHint extends AHint  {

	private final Cell cellA, cellB, wingA, wingB;
	private final int y, x;
	private final ARegion commonRegion; // the region common to wingA and wingB

	public WWingHint(Grid grid, IHinter hinter, int y, int x
			, Cell cellA, Cell cellB, Cell wingA, Cell wingB
			, ARegion commonRegion, Pots greens, Pots blues, Pots reds) {
		super(grid, hinter, AHint.INDIRECT, null, 0, reds, greens, null
				, blues, null, null);
		this.y = y;
		this.x = x;
		this.cellA = cellA;
		this.cellB = cellB;
		this.wingA = wingA;
		this.wingB = wingB;
		this.commonRegion = commonRegion;
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(commonRegion);
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNumUnused) {
		return MyLinkedHashSet.of(new int[]{cellA.indice, cellB.indice, wingA.indice, wingB.indice});
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+y+AND+x;
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return Frmt.ids(SB(64).append(getHintTypeName()).append(COLON_SP)
		, greens.keySet(), CSP, CSP).append(ON).append(y).append(AND).append(x);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "WWingHint.html"
			, cellA.id						//{0}
			, cellB.id						// 1
			, wingA.id						// 2
			, wingB.id						// 3
			, Integer.toString(y)			// 4
			, Integer.toString(x)			// 5
			, commonRegion					// 6
			, Frmt.and(ids(reds.keySet()))	// 7
			, reds.toString()				// 8
		);
	}

}
