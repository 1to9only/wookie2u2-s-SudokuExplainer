/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Frmu;
import java.util.Collections;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;

/**
 * Direct Hidden Set hint. A "direct" hint sets a cells value, which sometimes
 * happens with a HiddenSet and is now even sometimes detected (but only
 * sometimes, not all the time... Sigh).
 */
public final class HiddenSetDirectHint extends AHint  {

	private final Cell[] cells;
	private final int[] hdnSetValuesArray;
	private final ARegion region;

	/** Used by Locking. */
	public final int hdnSetValues;
	/** Used by Locking. */
	public final Idx hdnSetIdx;

	public HiddenSetDirectHint(AHinter hinter, Cell[] cells, int hdnSetValues
			, Pots orangePots, Pots redPots, ARegion region, int valueToSet
			, Cell cellToSet) {
		super(hinter, AHint.INDIRECT, cellToSet, valueToSet, redPots, null
				, orangePots, null, Regions.list(region), null);
		this.cells = cells;
		this.hdnSetValues = hdnSetValues;
		this.region = region;
		/** Used by Locking. */
		this.hdnSetValuesArray = Values.toArrayNew(hdnSetValues);
		this.hdnSetIdx = Idx.of(cells);
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
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(cells))
		  .append(COLON_SP).append(Frmt.csv(hdnSetValuesArray))
		  .append(IN).append(region.id)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "HiddenSetDirectHint.html"
				, NUMBER_NAMES[hdnSetValuesArray.length-2]	// {0}
				, Frmu.csv(cells)				//  1
				, Frmt.and(hdnSetValuesArray)	//  2
				, region.id						//  3
				, getHintTypeName()				//  4
				, cell.id						//  5
				, Integer.toString(value)		//  6
				, redPots.toString()			//  7
		);
	}

}
