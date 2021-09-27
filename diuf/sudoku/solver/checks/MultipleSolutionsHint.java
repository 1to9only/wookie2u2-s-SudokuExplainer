/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;


/**
 * A hint that shows two different solutions of an invalid Sudoku.
 * @see diuf.sudoku.solver.checks.RecursiveAnalyser
 */
public final class MultipleSolutionsHint extends AWarningHint implements IPretendHint {

	private final Grid grid;
	private final Grid solution1;
	private final Grid solution2;
	private int lastViewNum = 0;

	public MultipleSolutionsHint(AHinter hinter, Grid source, Grid solution1
			, Grid solution2) {
		super(hinter);
		this.grid = source;
		this.solution1 = solution1;
		this.solution2 = solution2;
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		if (lastViewNum == 0)
			solution1.copyTo(grid);
		else
			solution2.copyTo(grid);
		// Clear all potentials
		for ( Cell cell : grid.cells )
			cell.maybes.clear();
		for (int i=0; i<27; ++i)  for (int v=1; v<10; ++v)
			grid.regions[i].indexesOf[v].clear();
		return (81-17) * 10;
	}

	@Override
	public Pots getGreens(int viewNum) {
		Pots result = new Pots(81, 1F);
		final Grid sol; if(viewNum==0) sol=solution1; else sol=solution2;
		for ( Cell c : grid.cells )
			result.put(c, new Values(sol.cells[c.i].value));
		lastViewNum = viewNum;
		return result;
	}

	@Override
	public int getViewCount() {
		return 2;
	}

	@Override
	public String toStringImpl() {
		return "Multiple Solutions";
	}

	@Override
	public String toHtmlImpl() {
		return Html.load(this, "MultipleSolutionsHint.html");
	}

	// just used for unit tests
	public Grid getSolution1() {
		return solution1;
	}

	// just used for unit tests
	public Grid getSolution2() {
		return solution2;
	}
}