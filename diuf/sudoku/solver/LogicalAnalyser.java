/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.checks.AWarningHinter;
import diuf.sudoku.solver.checks.AnalysisHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Log;

/**
 * A LogicalAnalyser presents LogicalSolver.solve as an IHinter. It solves a
 * Sudoku puzzle logically, to work-out how difficult that is, and produce a
 * summary of the solving techniques (by type) that were applied in order to
 * solve the puzzle.
 * <p>
 * I'm a waffer-thin <b>single-use</b> IHinter facade for LogicalSolver.solve.
 * LogicalAnalyser and LogicalSolver are codependant: they MUST be in the same
 * package. My constructor takes the logHints and logTimes params that would
 * normally be passed to findHints, if that were possible, which it isn't coz
 * findHints is defined by IHinter and I must be an AWarningHinter (extends
 * AHinter, which implements IHinter) in order to be a validator, and I
 * abso-____ing-lutely <u>must</u> be a validator, it's what I exist for. sigh.
 * <p>
 * LogicalAnalyser calls back the LogicalSolver passed to its constructor,
 * hence I am ALWAYS an automatic variable, not a field, so: <br>
 * I come, I findHints, then I go.
 * <p>
 * So, a LogicalAnalyser is only waffer-thin and it's <b>single-use</b>!
 *
 * @see diuf.sudoku.solver.checks.AnalysisHint
 *
 * @author Keith Corlett
 */
public final class LogicalAnalyser extends AWarningHinter {

	/**
	 * The LogicalSolver that created me (ie was passed to my constructor).
	 */
	private final LogicalSolver solver;
	private final boolean logHints;
	private final boolean logTimes;

	/**
	 * Note that the only constructor is package visible, and is only called by
	 * LogicalSolver (except in the JUnit test-case, which is a bit trixie).
	 * Please read the class comments now, then come-back and nut-out params.
	 *
	 * @param solver My co-dependant LogicalSolver. Not null.
	 * @param logHints if true then hints are printed in the Log. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 * @param logTimes if true then EVERY single hinter execution is logged. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 *  This setting is ONLY respected in the GUI! If you did this in a full
	 *  LogicalSolverTester run the log-file would be enormous, and I mean
	 *  enormous, not merely huge.
	 */
	LogicalAnalyser(LogicalSolver solver, boolean logHints, boolean logTimes) {
		// Tech.Solution isn't a real solving technique, it's the sum of all
		// wanted techniques. Tech.Solution.degree is 0, and it's difficulty
		// is also 0.0, which is NOT representative. Sigh.
		super(Tech.Analysis);
		this.solver = solver;
		this.logHints = logHints | logTimes; // times useless without hints
		this.logTimes = logTimes;
		assert solver != null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * LogicalAnalyser is a wafer-thin wrapper for {@link LogicalSolver#solve}.
	 * <p>
	 * LogicalAnalyser's implementation of findHints produces a single
	 * AnalysisHint (a WarningHint) containing the difficulty rating of the
	 * given puzzle, and a summary of the hints (by type) that were applied
	 * in order to solve it.
	 * <p>
	 * If the puzzle cannot be solved (is invalid) then a "raw" WarningHint is
	 * produced, but it's pre-validated by LogicalSolver.analyse, so it should
	 * never be invalid, so this should never happen. Never say never.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		try {
			// run the puzzleValidators and the gridValidators seperately
			// here because differentiating a WarningHint is complicated.
			final AHint warning = solver.validatePuzzleAndGrid(grid, F);
			if ( warning != null ) {
				return accu.add(warning);
			}
			final UsageMap usage = new UsageMap();
			// I'm synchronized so that the generator thread waits for analyse
			// (ie solve) to finish before generating a puzzle to replenish its
			// cache. Cant run two solves concurrently coz of stateful statics.
			final boolean ok; // did the puzzle solve
			synchronized ( LogicalSolver.ANALYSE_LOCK ) {
				// call-back the LogicalSolver that created me.
				ok = solver.solve(grid, usage, F, F, logHints, logTimes);
			}
			accu.add(ok? new AnalysisHint(this, usage): solver.unsolvableHint);
		} catch (InterruptException ex) {
			return false;
		} catch (Exception ex) {
			StdErr.whinge(Log.me()+" exception", ex);
			accu.add(solver.unsolvableHint);
		}
		return true;
	}

	@Override
	public String toString() {
		return "LogicalAnalyser";
	}

}
