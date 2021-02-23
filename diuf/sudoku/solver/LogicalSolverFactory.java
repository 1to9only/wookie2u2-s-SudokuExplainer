/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Settings;
import diuf.sudoku.solver.LogicalSolver.Mode;
import diuf.sudoku.gen.IInterruptMonitor;
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
	 * Shorthand for get(LogicalSolver.Mode.ACCURACY) which is the default mode.
	 * 
	 * @return 
	 */
	public static LogicalSolver get() {
		return get(LogicalSolver.Mode.ACCURACY);
	}
	/**
	 * Get the current LogicalSolver if it has the given Mode and
	 * IInterruptMonitor, else construct a new LogicalSolver with those
	 * settings, cache it, and return it. Get is an MRU-cache of size 1.
	 * <p>
	 * The {@link LogicalSolver.Mode} is either ACCURACY (the default), or
	 * SPEED for {@link diuf.sudoku.test.LogicalSolverTester} -SPEED switch.
	 * I could probably do-away with the -SPEED switch and the Mode enum now,
	 * because ACCURACY is fast-enough as it is, without throwing ALL my toys
	 * out of my pram. I've only retained it because it's just nice to see how
	 * quickly it CAN be done, to offset any feelings of self-worth gained by
	 * writing the latest slow-assed bloody hinter. Everything that's slower
	 * per-elim than the Chainers is too bloody slow, and that's most of it.
	 * <p>
	 * Generating a new IDKFA puzzle takes an indeterminate period, ie ken ages.
	 * The longest I've seen was seven minutes, but that was really unlucky.
	 * And I noticed that all IDKFA's generated used no mirroring to strip the
	 * grid, so now only NONE is used with IDKFA, so if it ever takes seven
	 * minutes again you've been really really unlucky. Most IDKFA generates
	 * take a couple of minutes. Patience is a virtue.
	 * <p>
	 * But I am unvirtuous, so I cache generated puzzles. My hope is that Mr
	 * User will solve each puzzle generated, giving adequate time to generate
	 * a replacement in the background. Mr Thick will sit there and press the
	 * generate button repeatedly, killing and restarting the generate process.
	 * Intelligence is a virtue too, in theory. It's only a problem in practice.
	 * And this is a Sudoku puzzle solver, which isn't the least bit practical,
	 * so I feel safe, despite my all too apparent lack of intelligence.
	 *
	 * @param mode The LogicalSolver.Mode of this LogicalSolver.
	 * @return the current cached LogicalSolver, which may be existing or a
	 *  new one, depending on if the Earth fundamentally moved for you.
	 *  Butt one simply loves tearing one a new one. Doesn't one? Sigh.
	 */
	public static LogicalSolver get(Mode mode) {
		// mode is required, monitor is nullable
		assert mode != null;
		// create new LS if unset, or mode has changed.
		if ( solver==null || mode!=solver.mode )
			solver = newLogicalSolver(mode);
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
		if ( solver == null )
			solver = newLogicalSolver(Mode.ACCURACY);
		// recreate the LogicalSolver if the underlying Settings have changed.
		else if ( !Settings.THE.equals(solversSettings) )
			solver = newLogicalSolver(solver.mode);
		return solver;
	}

	// This method exists to ALWAYS update solversSettings after creating a new
	// LogicalSolver, so that solversSettings is logically "pinned" to solver.
	private static LogicalSolver newLogicalSolver(LogicalSolver.Mode mode) {
		LogicalSolver solver = new LogicalSolver(mode);
		updateSolversSettings();
		return solver;
	}

	// update the solversSettings for equals(Settings.THE) by next recreate().
	private static void updateSolversSettings() {
		try {
			solversSettings = (Settings)Settings.THE.clone();
		} catch (Exception ex) {
			StdErr.exit("failed to Settings.THE.clone()", ex);
		}
	}

	// never used
	private LogicalSolverFactory() { }

}
