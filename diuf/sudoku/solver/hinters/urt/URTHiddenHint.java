/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;

/**
 * The DTO for a Hidden Rectangle only (no loops) Hint; ie loop.size==4.
 *
 * @author Keith Corlett 2021-02-25
 */
public final class URTHiddenHint extends AURTHint {

	private final List<ARegion> myBases; // the B -> D edge of the rectangle
	private final List<ARegion> myCovers; // the C -> D edge of the rectangle
	public URTHiddenHint(UniqueRectangle hinter, List<Cell> loop
			, int v1, int v2, Pots redPots
			, List<ARegion> bases, List<ARegion> covers) {
		// Type 7 is a Hidden Unique Rectangle (aka Unique Rectangle Hidden)
		super(7, hinter, loop, v1, v2, redPots);
		myBases = bases;
		myCovers = covers;
	}

	@Override
	public List<ARegion> getBases() {
		return myBases;
	}

	@Override
	public List<ARegion> getCovers() {
		return myCovers;
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots(4, 1F);
			for ( Cell c : loop )
				pots.put(c, new Values(v1)); // v1 (a) is locked into rectangle
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public Pots getBlues(Grid grid, int viewNum) {
		if ( bluePots == null ) {
			Pots pots = new Pots(4, 1F);
			for ( Cell c : loop )
				pots.put(c, new Values(v2)); // v2 (b) is value to remove
			// remove the removable (red) pot so that it appears red
			pots.removeAll(getReds(0));
			bluePots = pots;
		}
		return bluePots;
	}
	private Pots bluePots;

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URTHiddenHint.html"
			, getTypeName()			//{0}
			, v1					// 1
			, v2					// 2
			, Frmt.csv(loop)		// 3
			, redPots.toString()	// 4
			, loop.get(0).id		// 5 A
			, loop.get(1).id		// 6 B
			, loop.get(2).id		// 7 D (out of order!)
			, loop.get(3).id		// 8 C (out of order!)
		);
	}
}