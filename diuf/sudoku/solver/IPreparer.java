/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;

/**
 * IPrepared is implemented by all the Aligned*Exclusion hinters, and by the
 * ALS hinters. It facilitates preparing a hinter to solve each puzzle.
 *
 * @author Keith Corlett 2020-03-07
 */
public interface IPreparer {
	/**
	 * Some hinters need to prepare for each puzzle, which happens after the
	 * LogicalSolver is created/configured, but before the first getHints, to
	 * prepare each hinter to solve each puzzle.
	 * <p>
	 * So to become a prepper one just implements IPreparer, and LogicalSolver
	 * calls your prepare method before it starts solving each puzzle. Simples.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	public void prepare(Grid grid, LogicalSolver logicalSolver);
}
