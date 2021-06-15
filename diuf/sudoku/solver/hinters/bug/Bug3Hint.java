/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bug3Hint holds the data for a "BUG type 3:" hint.
 * <p>
 * Bug3Hint is untested as at 2020-08-26 because I don't have a Sudoku puzzle
 * that contains one.
 * <p>
 * There are 29 "BUG type 1:"s and just 1 "BUG type 2:"s in top1465 if you
 * turn-off all the HoDoKu hint types. If you turn on all the HoDoKu hint-types
 * (especially Coloring) then no BUG's are found so you may as well deselect me.
 * 
 * @author Nicolas Juillerat
 */
public final class Bug3Hint extends ABugHint implements IActualHint {

	private final Set<Cell> bugCells;
	private final Cell[] nkdSetCells;
	private final Map<Cell, Values> extraValues;
	private final Values allExtraValues;
	private final Values nkdSetVals;
	private final ARegion region;

	public Bug3Hint(AHinter hinter, Pots redPots
			, Cell[] nakedCells, Map<Cell, Values> extraValues
			, Values allExtraValues, Values nakedSetValues, ARegion region) {
		super(hinter, redPots);
		this.bugCells = extraValues.keySet();
		this.extraValues = extraValues;
		this.allExtraValues = allExtraValues;
		this.nkdSetCells = nakedCells;
		this.nkdSetVals = nakedSetValues;
		assert super.degree == nakedSetValues.size;
		this.region = region;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return bugCells;
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null ) {
			Pots pots = new Pots(bugCells.size()+nkdSetCells.length, 1F);
			for ( Cell c : bugCells )
				pots.put(c, nkdSetVals.intersect(extraValues.get(c))); // green
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getReds(int viewNum) {
		if ( myRedPots == null ) {
			Pots pots = new Pots(super.redPots.size()+nkdSetCells.length, 1F);
			pots.putAll2(redPots);
			for ( Cell c : nkdSetCells )
				pots.put(c, new Values(nkdSetVals)); // orange
		}
		return myRedPots;
	}
	private Pots myRedPots;

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots(bugCells.size()+nkdSetCells.length, 1F);
			for ( Cell c : nkdSetCells )
				pots.put(c, new Values(nkdSetVals)); // orange
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public double getDifficulty() {
		// hinter.tech.difficulty + 0.1 for each cell in the naked/hidden set
		return super.getDifficulty() + degree*0.1;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "BUG type 3";
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.csv(bugCells)+" on "+Frmt.csv(nkdSetVals);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "Bug3Hint.html"
				, Frmt.and(allExtraValues)	// {0}
				, Frmt.and(bugCells)		//  1
				, Frmt.or(bugCells)			//  2
				, Frmt.or(allExtraValues)	//  3
				, GROUP_NAMES[degree-2]		//  4
				, Frmt.and(nkdSetCells)		//  5
				, Frmt.and(nkdSetVals)		//  6
				, region.id					//  7
		);
	}
}