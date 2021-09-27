/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Settings;
import diuf.sudoku.io.StdErr;

/**
 * The LogicalSolverFactory is an all-static class. It's the only way to create
 * a LogicalSolver (outside of this package). It's a way of ensuring that there
 * is ONE LogicalSolver in existence at any one time, recreating it as required.
 * <p>
 * Why? well LogicalSolver.solve relies upon "stateful" static fields, so if
 * two of them run simultaneously there is mahem, so we ensure that there is
 * only ever one LogicalSolver in existence at any one time.
 * <p>
 * If I ever figure-out how to get rid of those stateful static fields (like
 * the Naked/Hidden singles queues in AHint.apply, for instance) then we can
 * get rid of this Factory too, I think.
 *
 * @author Keith Corlett 2020 Apr 18
 */
public final class LogicalSolverFactory {

	private static LogicalSolver solver;
	private static Settings solversSettings;

	/**
	 * Get the current LogicalSolver if it has the given IInterruptMonitor,
	 * else construct a new LogicalSolver with that IM, cache and return it.
	 * Get is an MRU-cache of size 1.
	 * <p>
	 * Generating a new Diabolical or IDKFA puzzle takes indeterminate time,
	 * ie bloody ages. The longest I've seen was seven minutes, but that was
	 * pretty unlucky. And I noticed that all IDKFA's use no mirroring to strip
	 * the grid, so now only NONE is used with IDKFA, so if it ever takes seven
	 * minutes again you've been unlucky. IDKFA generates take a few minutes.
	 * Patience is a virtue.
	 * <p>
	 * But I am completely unvirtuous, so I cache generated puzzles. My hope is
	 * that Mr User will solve each puzzle generated, giving adequate time to
	 * generate a replacement in the background. Mr Thick will sit there and
	 * press the generate button repeatedly, killing and restarting generate.
	 * Intelligence is a virtue too, in theory, bit it's just another problem
	 * in practice. But this is a Sudoku puzzle solver, which isn't the least
	 * bit practical, so I feel safe, despite my lack of intelligence. sigh.
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
		// create a default LogicalSolver if unset (should never happen).
		// recreate the LogicalSolver if the underlying Settings have changed.
		if ( solver==null || !Settings.THE.equals(solversSettings) )
			solver = newLogicalSolver();
		return solver;
	}

	// This method exists to ALWAYS update solversSettings after creating a new
	// LogicalSolver, so that solversSettings is logically "pinned" to solver.
	private static LogicalSolver newLogicalSolver() {
		LogicalSolver result;
		try {
			result = new LogicalSolver();
			solversSettings = (Settings)Settings.THE.clone();
		} catch (Exception ex) {
			StdErr.exit("newLogicalSolver failed", ex);
			result = null; // you can't get here!
		}
		return result;
	}

	// never used
	private LogicalSolverFactory() { }

}
