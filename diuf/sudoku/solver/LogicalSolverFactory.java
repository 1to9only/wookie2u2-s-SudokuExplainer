/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.Log;

/**
 * The LogicalSolverFactory is the only way to create a LogicalSolver (outside
 * of this package). I am trying to ensure that there is ONE LogicalSolver in
 * existence at any one time, recreating it as required.
 * <p>
 * Why? well LogicalSolver.solve relies upon "stateful" static fields, so if
 * two of them run simultaneously there will be mahem, so we ensure that there
 * is only ever one LogicalSolver in existence at any one time.
 * <p>
 * If I ever figure-out how to get rid of those stateful static fields (like
 * the Naked/Hidden singles queues in AHint.apply, for instance) then we can
 * get rid of this Factory too, I think. This class was incepted to solve
 * static-referencing issues. Also, we can get THE LogicalSolver anywhere
 * without needing a specific type reference, which supports later abstraction.
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
	 * <p>
	 * We get/create/build one LogicalSolver at a time, so I am synchronized.
	 *
	 * @return the current cached LogicalSolver, which may be existing or a
	 *  new one, depending on if the Earth fundamentally moved for you.
	 *  Butt one simply loves tearing one a new one. Does not one? Sigh.
	 */
	public synchronized static LogicalSolver get() {
		if ( solver == null )
			solver = newLogicalSolver();
		return solver;
	}

	/**
	 * Recreate is used in the GUI when the Config have probably changed.
	 * <p>
	 * LogicalSolver.configureHinters() reads wantedHinters from Config, and
	 * only way to run configureHinters is via the LogicalSolver constructor;
	 * so we need a "recreate" method in the factory. It would be a better
	 * design if this method was not necessary, but it is. Sigh.
	 * <p>
	 * We get/create/build one LogicalSolver at a time, so I am synchronized.
	 *
	 * @return a new LogicalSolver if unset or the Config have changed, else
	 *  the existing one.
	 */
	public synchronized static LogicalSolver recreate() {
		Log.teeln("LogicalSolverFactory is recreating the LogicalSolver...");
		solver = newLogicalSolver();
		solver.reportConfiguration();
		return solver;
	}

	/**
	 * As above but without the gannet.
	 *
	 * @return a new LogicalSolver
	 */
	public synchronized static LogicalSolver recreateQiuetly() {
		return solver = newLogicalSolver();
	}

	// This method exists to ALWAYS update solvers config by creating a new
	// LogicalSolver, so that config is logically "pinned" to a solver, and
	// and simply cannot change during a solve-run.
	// We get/create/build one LogicalSolver at a time, so I am synchronized.
	private synchronized static LogicalSolver newLogicalSolver() {
		LogicalSolver newSolver;
		try {
			newSolver = new LogicalSolverBuilder().build();
		} catch (Exception ex) {
			StdErr.exit("FAIL: Building a new LogicalSolver threw an exception", ex);
			newSolver = null; // you cannot get here!
		}
		return newSolver;
	}

	// never used
	private LogicalSolverFactory() { }

}
