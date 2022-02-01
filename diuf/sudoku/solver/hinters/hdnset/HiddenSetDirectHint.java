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

	private final Cell[] cells; // cells in this Hidden Set
	private final ARegion region;
	private final int[] hdnSetValuesArray;

//	/** Used by Locking. */
//	public final Idx hdnSetIdx;
//	public final int hdnSetValues;

	public HiddenSetDirectHint(AHinter hinter, Cell[] cells, int hdnSetValues
			, Pots greens, Pots oranges, Pots reds, ARegion region
			, int valueToSet, Cell cellToSet) {
		super(hinter, AHint.INDIRECT, cellToSet, valueToSet, reds, greens
				, oranges, null, Regions.array(region), null);
		this.cells = cells;
		this.region = region;
		this.hdnSetValuesArray = Values.toArrayNew(hdnSetValues);
//		// Used by Locking.
//		this.hdnSetIdx = Idx.of(cells);
//		this.hdnSetValues = hdnSetValues;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return Collections.singleton(cell);
	}

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
			, reds.toString()				//  7
		);
	}

}
