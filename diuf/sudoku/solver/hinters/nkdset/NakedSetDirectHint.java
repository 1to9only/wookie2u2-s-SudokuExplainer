/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Collections;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;

public final class NakedSetDirectHint extends AHint  {

	private final Cell[] nkdSetCells;
	private final int nkdSetValues;
	private final ARegion region;

	public NakedSetDirectHint(
			  AHinter hinter
			, Cell cellToSet, int valueToSet
			, Cell[] nkdSetCells // an array of the naked-set cells (I store a list)
			, int nkdSetValues // the naked-set values
			, Pots orangePots // the naked-set cells => all of each cells values
			, Pots redPots // cell=>values to be removed, ie all other cells in
						   // in the region which maybe the nkSetValues (!empty)
			, ARegion region
	) {
		// this "Direct" hint is rendered INDIRECT so it's grouped with the
		// other indirect hint types in the GUI's HintsTree; and it has redPots.
		super(hinter, AHint.INDIRECT, cellToSet, valueToSet, redPots, null
				, orangePots, null, Regions.array(region), null);
		this.nkdSetCells = nkdSetCells;
		this.nkdSetValues = nkdSetValues;
		this.region = region;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return Collections.singleton(cell);
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null )
			greenPots = new Pots(cell, value);
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig ) {
			String rid = getFirstRegionId();
			if(rid==null) rid="regnoto";
			s += " in <b1>"+rid+"</b1>";
		}
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(nkdSetCells)).append(COLON_SP)
		  .append(Values.csv(nkdSetValues)).append(IN).append(region.id)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetDirectHint.html"
			, NUMBER_NAMES[degree-2]	// {0}
			, Frmu.csv(nkdSetCells)		//  1
			, Values.csv(nkdSetValues)	//  2
			, region.id					//  3
			, getHintTypeName()			//  4
			, cell.id					//  5
			, Integer.toString(value)	//  6
			, reds.toString()			//  7
		);
	}
}
