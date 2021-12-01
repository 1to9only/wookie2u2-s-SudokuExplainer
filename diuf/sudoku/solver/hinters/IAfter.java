/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

/**
 * IAfter allows hinters to tidy-up after each puzzle is solved.
 * You should release Grid/Region/Cell references here rather than ICleanUp,
 * which will never be invoked if we never force GC.
 * <p>
 * To be clear, after each puzzle is solved, 
 * {@link diuf.sudoku.solver.LogicalSolver#solve(Grid, UsageMap, boolean, boolean, boolean, boolean)} 
 * invokes the after method of each wantedHinter that implements IAfter.
 *
 * @author Keith Corlett 2021-09-05
 */
public interface IAfter {
	public void after();
}
