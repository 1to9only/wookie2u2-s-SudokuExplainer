/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hidden;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Frmu;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;

/**
 * DTO (Data Transfer Object) for a Direct Hidden Set hint.
 */
public final class HiddenSetDirectHint extends AHint  {

	private final Cell[] cells; // HiddenSet cells
	private final int[] values; // HiddenSet values
	private final ARegion region; // that contains the HiddenSet

	public HiddenSetDirectHint(final IHinter hinter, final Cell[] cells
			, final int values, final ARegion region, final int valueToSet
			, final Cell cellToSet, final Pots reds, final Pots greens
			, final Pots oranges
	) {
		super(region.getGrid(), hinter, AHint.INDIRECT, cellToSet, valueToSet
				, reds, greens, oranges, null, Regions.array(region), null);
		this.cells = cells;
		this.values = VALUESES[values];
		this.region = region;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNumberNotUsed) {
		return Idx.of(cell.indice);
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(Frmu.csv(cells)).append(COLON_SP).append(Frmt.csv(values))
		.append(IN).append(region.label);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "HiddenSetDirectHint.html"
			, NUMBER_NAMES[values.length-2]	// {0}
			, Frmu.csv(cells)				//  1
			, Frmt.and(values)				//  2
			, region.label					//  3
			, getHintTypeName()				//  4
			, cell.id						//  5
			, Integer.toString(value)		//  6
			, reds.toString()				//  7
		);
	}

}
