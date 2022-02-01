/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.GRID_SIZE;

/**
 * A Backup is a copy of the Grid's essential data from which we can QUICKLY
 * restore the Grid to it's previous state: ie the values and maybes of each
 * cell in a grid. I was using a toString/parse for this, but they're slow,
 * so now I use a Backup to store the raw int values instead.
 * <p>
 * Backup takes RecursiveSolverTester under 1 second, without AUTOSOLVE!
 *
 * @author Keith Corlett 2021-07-23
 */
public class Backup {

	public final int[] values = new int[GRID_SIZE];
	public final int[] maybes = new int[GRID_SIZE];

	public Backup(Grid grid) {
		grid.values(values);
		grid.maybes(maybes);
	}
	
}
