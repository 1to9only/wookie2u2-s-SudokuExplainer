/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Log;

/**
 * The LogicalSolverFactory is the only way to create a LogicalSolver (outside
 * of this package). I'm trying to ensure that there is ONE LogicalSolver in
 * existence at any one time, recreating it as required.
 * <p>
 * Why? well LogicalSolver.solve relies upon "stateful" static fields, so if
 * two of them run simultaneously there is mahem, so we ensure that there is
 * only ever one LogicalSolver in existence at any one time.
 * <p>
 * If I ever figure-out how to get rid of those stateful static fields (like
 * the Naked/Hidden singles queues in AHint.apply, for instance) then we can
 * get rid of this Factory too, I think.
 * <p>
 * Everything in this implementation is static.
 *
 * @author Keith Corlett 2020 Apr 18
 */
public final class LogicalSolverFactory {

	private static LogicalSolver solver;

	/**
	 * Get the current LogicalSolver, else construct a new LogicalSolver and
	 * return it; so get is an MRU cache of size 1.
	 *
	 * @return the current cached LogicalSolver, which may be existing or a
	 *  new one, depending on if the Earth fundamentally moved for you.
	 *  Butt one simply loves tearing one a new one. Doesn't one? Sigh.
	 */
	public static LogicalSolver get() {
		if ( solver == null )
			solver = newLogicalSolver();
		return solver;
	}

	/**
	 * Recreate is used in the GUI when the Settings have probably changed.
	 * <p>
	 * LogicalSolver.configureHinters() reads wantedHinters from Settings, and
	 * only way to run configureHinters is via the LogicalSolver constructor;
	 * so we need a "recreate" method in the factory. It would be a better
	 * design if this method was not necessary, but it is. Sigh.
	 *
	 * @return a new LogicalSolver if unset or the Settings have changed, else
	 *  the existing one.
	 */
	public static LogicalSolver recreate() {
//		Log.teeln("LogicalSolverFactory is recreating the LogicalSolver...");
		solver = newLogicalSolver();
		Log.teeln(solver.getWanteds());
		Log.teeln(solver.getUnwanteds());
		return solver;
	}

	// This method exists to ALWAYS update solversSettings after creating a new
	// LogicalSolver, so that solversSettings is logically "pinned" to solver.
	private static LogicalSolver newLogicalSolver() {
		LogicalSolver solver;
		try {
			solver = new LogicalSolverBuilder().build();
		} catch (Exception ex) {
			StdErr.exit("WTF: new LogicalSolver() exception", ex);
			solver = null; // you can't get here!
		}
		return solver;
	}

	// never used
	private LogicalSolverFactory() { }

}
