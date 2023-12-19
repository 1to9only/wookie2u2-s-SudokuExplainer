/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.Grid.MAX_EMPTY_CELLS;

/**
 * A hint that shows two different solutions of an invalid Sudoku.
 * @see diuf.sudoku.solver.checks.BruteForce
 */
public final class MultipleSolutionsHint extends AWarningHint implements IPretendHint {

	private final Grid solution1;
	private final Grid solution2;
	private int lastViewNum = 0;

	public MultipleSolutionsHint(IHinter hinter, Grid source, Grid solution1
			, Grid solution2) {
		super(null, hinter);
		this.solution1 = solution1;
		this.solution2 = solution2;
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		if (lastViewNum == 0)
			grid.copyFrom(solution1);
		else
			grid.copyFrom(solution2);
		// Clear all potentials
		for ( Cell c : grid.cells )
			c.clearMaybes();
		for (int i=0; i<NUM_REGIONS; ++i)
			for (int v=1; v<VALUE_CEILING; ++v)
				grid.regions[i].clearPlaces(v);
		return MAX_EMPTY_CELLS * 10;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		Pots result = new Pots(GRID_SIZE, 1F);
		final Grid sol; if(viewNum==0) sol=solution1; else sol=solution2;
		for ( Cell c : sol.cells )
			result.put(c.indice, VSHFT[sol.cells[c.indice].value]);
		lastViewNum = viewNum;
		return result;
	}

	@Override
	public int getViewCount() {
		return 2;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(32).append("Multiple Solutions");
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
