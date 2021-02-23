/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Set;


/**
 * Bug1Hint holds the data for a "BUG type 1:" hint.
 * <p>
 * There are 29 "BUG type 1:"s and just 1 "BUG type 2:"s in top1465 if you
 * turn-off all the HoDoKu hint types. If you turn on all the HoDoKu hint-types
 * (especially Coloring) then no BUG's are found so you may as well deselect me.
 * 
 * @author Nicolas Juillerat
 */
public final class Bug1Hint extends ABugHint implements IActualHint {

	protected final Cell bugCell;
	protected final Values bugValues;

	public Bug1Hint(AHinter hinter, Pots redPots, Cell bugCell
			, Values bugValues) {
		super(hinter, redPots);
		this.bugCell = bugCell;
		this.bugValues = bugValues;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(bugCell);
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( this.greenPots == null )
			greenPots = new Pots(bugCell, bugValues);
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getReds(int viewNum) {
		return redPots;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "BUG type 1";
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+bugCell.id;
	}

	@Override
	public String toHtmlImpl() {
		Values redVals = bugCell.maybes.minus(bugValues);
		return Html.produce(this, "Bug1Hint.html"
				, Frmt.and(bugValues)	// {0}
				, bugCell.id			//  1
				, Frmt.or(bugValues)	//  2
				, Frmt.values(redVals)	//  3
		);
	}
}