/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Hint that allows the user to directly view the solution of a sudoku.
 */
public final class SolutionHint extends AWarningHint {

	private final Grid grid;
	private final Grid solution;
	// F8 MENU~Tools~Solve: true displays solution as green potential values
	private final boolean wantSolutionHint;

	public SolutionHint(AHinter hinter, Grid grid, Grid solution
			, boolean wantSolutionHint) {
		super(hinter);
		this.grid = grid;
		this.solution = solution;
		this.wantSolutionHint = wantSolutionHint;
	}

	@Override
	public Pots getGreens(int viewNum) {
		Pots result = new Pots(81, 1F);
		for (int i=0; i<81; ++i)
			result.put(grid.cells[i], new Values(solution.cells[i].value));
		return result;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		Set<Cell> set = new LinkedHashSet<>(64, 1F); // 81-17=64 is a powerOf2
		for ( Cell c : grid.cells )
			if ( c.maybes.size == 1 )
				set.add(c); // hit rate to low to cache
		return set;
	}

	@Override
	public String toStringImpl() {
		return "Solution";
	}

	@Override
	public String toHtmlImpl() {
		final String filename = wantSolutionHint
				? "SolutionHint.html" // (F8) describes green potential values
				: "SudokuSolved.html"; // for "solution by natural causes"
		return Html.load(this, filename);
	}

	@Override
	public int apply(boolean notUsed) {
		int result = (81 - grid.countFilledCells()) * 10;
		solution.copyTo(grid);
		return result;
	}

	public Grid getSolution() {
		return solution;
	}

}
