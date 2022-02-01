/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AWarningHint;
import diuf.sudoku.solver.IPretendHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A hint from {@link diuf.sudoku.solver.checks.BruteForce} to view
 * (and optionally apply) the solution of a Sudoku.
 */
public final class SolutionHint extends AWarningHint implements IPretendHint {

	private final Grid grid;
	private final Grid solution;

	public SolutionHint(AHinter hinter, Grid grid, Grid solution) {
		super(hinter);
		this.grid = grid;
		this.solution = solution;
	}
	public SolutionHint(Grid grid, Grid solution) {
		this(new diuf.sudoku.solver.hinters.DummyHinter(), grid, solution);
	}

	@Override
	public Pots getGreens(int viewNum) {
		final Pots result = new Pots(GRID_SIZE, 1F);
		for (int i=0; i<GRID_SIZE; ++i)
			result.put(grid.cells[i], VSHFT[solution.cells[i].value]);
		return result;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		final Set<Cell> set = new LinkedHashSet<>(64, 1F); // 81-17=64 is a powerOf2
		for ( Cell c : grid.cells )
			if ( c.size == 1 )
				set.add(c); // hit rate to low to cache
		return set;
	}

	@Override
	public String toStringImpl() {
		return "Solution";
	}

	@Override
	public String toHtmlImpl() {
		return Html.load(this, "SolutionHint.html");
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		final int score = (GRID_SIZE - grid.numSet) * 10;
		grid.copyFrom(solution);
		return score;
	}

}
