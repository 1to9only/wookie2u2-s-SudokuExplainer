/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Set;


/**
 * Bug2Hint holds the data for a "BUG type 2:" hint.
 * <p>
 * There are 29 "BUG type 1:"s and just 1 "BUG type 2:"s in top1465 if you
 * turn-off all the HoDoKu hint types. If you turn on all the HoDoKu hint-types
 * (especially Coloring) then no BUG's are found so you may as well deselect me.
 * 
 * @author Nicolas Juillerat
 */
public final class Bug2Hint extends ABugHint implements IActualHint {

	private final Set<Cell> bugCells;
	private final int bugValue;

	public Bug2Hint(AHinter hinter, Pots redPots, Set<Cell> bugCells
			, int bugValue) {
		super(hinter, redPots);
		this.bugCells = bugCells;
		this.bugValue = bugValue;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return bugCells;
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( this.greenPots == null )
			greenPots = new Pots(bugCells, bugValue);
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public double getDifficulty() {
		// hinter.tech.difficulty + 0.1 for Bug 2 (not real sure about this)
		return super.getDifficulty() + 0.1;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "BUG type 2";
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.csv(bugCells)+" on "+bugValue;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "Bug2Hint.html"
				, bugValue				// {0}
				, Frmt.and(bugCells)	//  1
				, Frmt.or(bugCells)		//  2
		);
	}
}