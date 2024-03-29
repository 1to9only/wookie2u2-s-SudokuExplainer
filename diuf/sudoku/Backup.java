/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.GRID_SIZE;

/**
 * A Backup is a copy of a puzzle, from which we can QUICKLY restore a Grids
 * values and maybes to there previous state, then rebuild (reindex) the grid.
 * <p>
 * Backup takes RecursiveSolverTester under 1 second, without AUTOSOLVE!
 *
 * @author Keith Corlett 2021-07-23
 */
public class Backup {

	public final int[] values = new int[GRID_SIZE];
	public final int[] maybes = new int[GRID_SIZE];

	public Backup(Grid grid) {
		grid.getValues(values);
		grid.getMaybes(maybes);
	}

}
