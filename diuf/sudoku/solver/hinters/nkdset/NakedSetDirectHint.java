/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public final class NakedSetDirectHint extends AHint implements IActualHint {

	private final List<Cell> nkdSetCellsList;
	private final Values nkdSetValues;
	private final Pots orangePots;
	private final ARegion region;

	public NakedSetDirectHint(
			  AHinter hinter
			, Cell cellToSet, int valueToSet
			, List<Cell> nkdSetCellsList // the naked-set cells
			, Values nkdSetValues // the naked-set values
			, Pots orangePots // the naked-set cells => all of each cells values
			, Pots redPots // cell=>values to be removed, ie all other cells in
						   // in the region which maybe the nkSetValues (!empty)
			, ARegion region
	) {
		// this "Direct" hint is rendered INDIRECT so it's grouped with the
		// other indirect hint types in the GUI's HintsTree; and it has redPots.
		super(hinter, AHint.INDIRECT, cellToSet, valueToSet, redPots);
		this.nkdSetCellsList = nkdSetCellsList;
		this.nkdSetValues = nkdSetValues;
		this.orangePots = orangePots;
		this.region = region;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return Collections.singleton(cell);
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null )
			greenPots = new Pots(cell, new Values(value));
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getOranges(int viewNum) {
		return orangePots;
	}

	@Override
	public Pots getReds(int viewNum) {
		return redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if (isBig) {
			String regionID = getFirstRegionId();
			if(regionID==null) regionID="regnoto";
			s+=" in <b1>"+regionID+"</b1>";
		}
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName()).append(": ")
		  .append(Frmt.csv(nkdSetCellsList)).append(": ")
		  .append(Frmt.csv(nkdSetValues)).append(" in ").append(region.id);
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetDirectHint.html"
			, NUMBER_NAMES[degree-2]	// {0}
			, Frmt.csv(nkdSetCellsList)	//  1
			, Frmt.csv(nkdSetValues)	//  2
			, region.id					//  3
			, getHintTypeName()			//  4
			, cell.id					//  5
			, Integer.toString(value)	//  6
		);
	}
}
