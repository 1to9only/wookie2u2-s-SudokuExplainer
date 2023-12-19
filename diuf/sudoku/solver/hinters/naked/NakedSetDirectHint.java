/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.naked;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;

public final class NakedSetDirectHint extends AHint  {

	private final Idx nkdSetIdx;
	private final int nkdSetValues;
	private final ARegion region;

	public NakedSetDirectHint(
			  Grid grid
			, IHinter hinter // pass me you, to pin myself back to
			, Cell cellToSet // cell to set
			, int valueToSet // value to set cell to
			, Idx nkdSetIdx // an Idx of the NakedSet cells (cloned)
			, int nkdSetValues // a bitset of the NakedSet values
			, Pots oranges // the NakedSet cells => all of each cells values
			, Pots reds // cell=>values to be removed, ie all other cells in
						// the region which maybe the nkdSetValues (!empty),
						// where "other" means not the nkdSetCells. sigh.
			, ARegion region // the region which contains the NakedSet
	) {
		// this "Direct" hint has eliminations, so is rendered INDIRECT and
		// grouped with other indirect hint types in the GUIs HintsTree.
		// No, it is not sane, but it does work, and that is what matters.
		super(grid, hinter, AHint.INDIRECT, cellToSet, valueToSet, reds, null
				, oranges, null, Regions.array(region), null);
		this.nkdSetIdx = new Idx(nkdSetIdx);
		this.nkdSetValues = nkdSetValues;
		this.region = region;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return Idx.of(cell.indice);
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		if ( greenPots == null )
			greenPots = new Pots(cell.indice, value);
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
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		  .append(nkdSetIdx.csv()).append(COLON_SP)
		  .append(Values.csv(nkdSetValues)).append(IN).append(region.label);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetDirectHint.html"
			, NUMBER_NAMES[degree-2]	// {0}
			, nkdSetIdx.csv()			//  1
			, Values.csv(nkdSetValues)	//  2
			, region.label					//  3
			, getHintTypeName()			//  4
			, cell.id					//  5
			, Integer.toString(value)	//  6
			, reds.toString()			//  7
		);
	}

}
