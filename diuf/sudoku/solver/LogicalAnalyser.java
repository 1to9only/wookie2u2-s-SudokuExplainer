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
 * A LogicalAnalyser is a wrapper that turns the LogicalSolver into an IHinter.
 * It solves a Sudoku puzzle (a Grid) logically, to work-out how difficult that
 * is, and produce a summary of the solving techniques that were applied in
 * order to solve the puzzle.
 * <p>
 * I'm a wafer-thin single-use class: I just present LogicalSolver.solve in an
 * IHinter's clothing. LogicalAnalyser and LogicalSolver are codependant. They
 * MUST be in the same package. The analyser calls back the LogicalSolver
 * passed to it's constructor, so analysers are ALWAYS automatic variables (not
 * fields) so that: he comes, he's used, he goes. My constructor takes the
 * logHints and logTimes settings. I'm single use. I'm only waffer-thin!
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
	 *
	 * @param solver My master LogicalSolver. Not null.
	 * @param logHints if true then hints are printed in the Log. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 * @param logTimes if true then EVERY single hinter execution is logged. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 *  This setting is ONLY respected in the GUI! If you did this in a large
	 *  LogicalSolverTester run the log-file would be enormous.
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
	 *
	 * <p>
	 * This implementation produces a single AnalysisHint (a WarningHint)
	 * containing the difficulty rating of the Sudoku puzzle and a list of
	 * the hints that were applied in order to solve it.
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
			final AHint warning = solver.validatePuzzleAndGrid(grid, false);
			if ( warning != null )
				return accu.add(warning);
			final UsageMap usage = new UsageMap();
			// nb: synchronized so that the generator thread waits for analyse
			// (ie solve) to finish before generating a puzzle to replenish its
			// cache. Cant run two solves concurrently coz of stateful statics.
			// KRC 2019 OCT @strech I really should clean-up those statics with
			// new RunContext holds all stateful objects in non-static fields.
			final boolean isSolved;
			synchronized ( LogicalSolver.ANALYSE_LOCK ) {
				// call-back the LogicalSolver that created me.
				isSolved = solver.solve(grid, usage, false, false, logHints
						, logTimes);
			}
			if ( isSolved )
				accu.add(new AnalysisHint(this, usage));
			else
				accu.add(solver.UNSOLVABLE_HINT);
		} catch (InterruptException ex) {
			return false;
		} catch (Exception ex) {
			StdErr.whinge(Log.me()+" exception", ex);
			accu.add(solver.UNSOLVABLE_HINT);
		}
		return true;
	}

	@Override
	public String toString() {
		return "LogicalAnalyser";
	}

}
