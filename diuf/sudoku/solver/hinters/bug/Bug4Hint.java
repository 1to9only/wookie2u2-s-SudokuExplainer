/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Bug4Hint holds the data for a "BUG type 4:" hint.
 * <p>
 * Bug4Hint is untested as at 2020-08-26 because I don't have a Sudoku puzzle
 * that contains one.
 * <p>
 * There are 29 "BUG type 1:"s and just 1 "BUG type 2:"s in top1465 if you
 * turn-off all the HoDoKu hint types. If you turn on all the HoDoKu hint-types
 * (especially Coloring) then no BUG's are found so you may as well deselect me.
 * 
 * @author Nicolas Juillerat
 */
public final class Bug4Hint extends ABugHint implements IActualHint {

	private final Cell bugCell1;
	private final Cell bugCell2;
//	private final Map<Cell, Values> extraValues;
	private final Values allExtraValues;
	private final int valueToRemove; // removable value appearing on both cells
	private final ARegion region;

	public Bug4Hint(AHinter hinter, Pots redPots, Cell bugCell1
			, Cell bugCell2, Map<Cell, Values> extraValues
			, Values allExtraValues, int valueToRemove, ARegion region) {
		super(hinter, redPots);
		this.bugCell1 = bugCell1;
		this.bugCell2 = bugCell2;
//		this.extraValues = extraValues;
		this.allExtraValues = allExtraValues;
		this.valueToRemove = valueToRemove;
		this.region = region;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(bugCell1, bugCell2);
	}

	@Override
	public Pots getOranges(int viewNum) {
        Pots result = new Pots();
        result.put(bugCell1, new Values(valueToRemove)); // orange
        result.put(bugCell2, new Values(valueToRemove)); // orange
        return result;
	}

	@Override
	public Pots getReds(int viewNum) {
		return super.redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public String getHintTypeNameImpl() {
		return "BUG type 4";
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+bugCell1.id+", "+bugCell2.id+" on "+valueToRemove;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "Bug4Hint.html"
				, Frmt.and(allExtraValues)					// {0}
				, Frmt.and(bugCell1, bugCell2)				//  1
				, Frmt.or(bugCell1, bugCell2)				//  2
				, Frmt.or(allExtraValues)					//  3
				, Integer.toString(valueToRemove)			//  4
				, region.id									//  5
				, Frmt.and(new Values(redPots.values()))	//  6
		);
	}
}