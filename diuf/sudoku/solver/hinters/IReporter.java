/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

/**
 * Reporter is an which tells the LogicalSolver that this IHinter exposes a
 * report method, so rather than have the LogicalSolverTester manually reporting
 * on all the static variables we just have each A*E (et al) implement IReporter
 * and the LogicalSolver can call the report() method of each. Simples: and many
 * less static variables, whose behaviour is "unpredictable". I really do not
 * know what is going on, but statics that I expect to be set have been left 0,
 * as if classes where loaded by 2 distinct class loaders or something. BFIIK.
 * I am not waiting 4 hours for a run to be rooted by funky statics management.
 * @author Keith Corlett 2020-02-17
 */
public interface IReporter {

	public void report();

}
