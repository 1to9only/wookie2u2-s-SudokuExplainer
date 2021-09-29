/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;

/**
 * Cheeter or Cheat not, there is no Cheat!
 *
 * @author Keith Corlett 2010-06-26
 */
interface Cheeter {
	Pots cheat(final Grid grid, final Tech tech, final LogicalSolver.Chaet colour, final boolean chaets);
}
