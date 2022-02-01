/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.solver.LogicalSolver;

/**
 * An IPrepare is (logically) a subtype of IHinter (which it does NOT extend).
 * It facilitates preparing a hinter to solve this puzzle.
 * <p>
 * The LogicalSolver prepares wantedHinters which implement IPrepare
 * before solving that grid (a puzzle). Some of the test-cases run hinters
 * which require preparation, and that gets interesting because the instance
 * of the hinter being tested is typically NOT in the logical solver, so it
 * must be prepared separately. This still acceptable BECAUSE test-cases are
 * the only places where hinters exist out-side of a LogicalSolver, everwhere
 * else in Sudoku Explainer an IHinter implementation exists ONLY in the
 * context of it's "parent" LogicalSolver; so that when the LogicalSolver
 * is replaced with a new one (because Settings have changed) all of it's
 * hinters are recreated also, so that a Logical Solver and all of it's
 * hinters are logically "locked onto" there specific Settings.
 * <p>
 * <code>
 * New Settings -> new LogicalSolver -> new Hinters. <b>Simples!</b>
 * </code>
 *
 * @author Keith Corlett 2020-03-07
 */
public interface IPrepare {
	/**
	 * Some hinters need to prepare for each puzzle, which happens after the
	 * LogicalSolver is created/configured, but before the first getHints, to
	 * prepare each hinter to solve each puzzle.
	 * <p>
	 * So to become a prepper one just implements IPrepare, and LogicalSolver
	 * calls your prepare method before it starts solving each puzzle. Simples.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	public void prepare(Grid grid, LogicalSolver logicalSolver);
}
