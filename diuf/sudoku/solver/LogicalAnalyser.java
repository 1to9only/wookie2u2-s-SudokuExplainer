/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Tech;
import diuf.sudoku.io.StdErr;
import static diuf.sudoku.solver.LogicalSolver.ANALYSE_LOCK;
import diuf.sudoku.solver.checks.AWarningHinter;
import diuf.sudoku.solver.checks.AnalysisHint;
import diuf.sudoku.solver.checks.WarningHint;
import diuf.sudoku.utils.Log;

/**
 * LogicalAnalyser implements {@link Tech#Analysis} Sudoku puzzle validator.
 * Really, LogicalAnalyser just presents
 * {@link LogicalSolver#solve(diuf.sudoku.Grid, diuf.sudoku.solver.UsageMap, boolean, boolean, boolean, boolean) }
 * as an IHinter.
 * LogicalSolver.solve solves a puzzle logically, calculating how difficult it
 * is, producing a summary of the solving techniques required.
 * <p>
 * I am a waffer-thin <b>single-use</b> IHinter facade for LogicalSolver.solve.
 * LogicalAnalyser and LogicalSolver are codependant: they MUST be in the same
 * package. My constructor takes the logHints and logTimes params that would
 * normally be passed to findHints, if that were possible, which it is not coz
 * findHints is defined by IHinter and I must be an AWarningHinter (extends
 * AHinter, which implements IHinter) in order to be a validator, and I
 * <u>must</u> be a validator; that is what I exist for.
 * <p>
 * LogicalAnalyser calls back the LogicalSolver passed to its constructor,
 * passing "the extra parameters" that are passed to my constructor, hence I
 * am ALWAYS held in an automatic variable, never a field, so I am created, I
 * findHints, then I go. I am <b>single-use</b>.
 * <p>
 * So, a LogicalAnalyser is waffer-thin and stateful, hence <b>single-use</b>.
 * Did I mention that I am a <b>single-use</b> class? Its rather important.
 * <p>
 * There's two ways to measure Sudoku difficulty: maximum or total difficulty.
 * I prefer maxDiff, because the puzzle cannot be solved unless one finds its
 * most difficult elimination. But for a computer difficulty means CPU-time,
 * ergo ttlDiff. The batch reports maxD for humans, and ttlDiff for techies.
 * Total difficulty has a MUCH closer correlation with the total time required
 * to solve a puzzle, ergo CPU-time.
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
	private final boolean logHinterTimes;

	/**
	 * Note that the only constructor is package visible, and is only called by
	 * LogicalSolver (except in the JUnit test-case, which is a bit trixie).
	 * Please read the class comments now, then come-back and nut-out params.
	 *
	 * @param solver My co-dependant LogicalSolver. Not null.
	 * @param logHints if true then hints are printed in the Log. <br>
	 *  In the GUI that is HOME/SudokuExplainer.log in my normal setup.
	 * @param logHinterTimes if true then EVERY single hinter execution is logged. <br>
	 *  In the GUI that is HOME/SudokuExplainer.log in my normal setup.
	 *  This setting is ONLY respected in the GUI! If you did this in a full
	 *  LogicalSolverTester run the log-file would be enormous, and I mean
	 *  enormous, not merely huge.
	 */
	public LogicalAnalyser(final LogicalSolver solver, final boolean logHints
			, final boolean logHinterTimes) {
		// Tech.Solution is not a real solving technique, it is the sum of all
		// wanted techniques. Tech.Solution.degree is 0, and it is difficulty
		// is also 0.0, which is NOT representative. Sigh.
		super(Tech.Analysis);
		this.solver = solver;
		// hinter times make no sense without the hints for context
		this.logHints = logHints | logHinterTimes;
		this.logHinterTimes = logHinterTimes;
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
	 * produced, but it is pre-validated by LogicalSolver.analyse, so it should
	 * never be invalid, so this should never happen. Never say never.
	 */
	@Override
	public boolean findHints() {
		try {
			// validate the grid and puzzle before solve coz picking a warning
			// hint from a summary hint (which is a warning hint) is difficult.
			final AHint warning = solver.validatePuzzleAndGrid(grid, F);
			if ( warning != null ) {
				accu.add(warning);
				return true;
			}
			final UsageMap usage = new UsageMap();
			// I am synchronized so that the generator thread waits for analyse
			// (ie solve) to finish before generating a puzzle to replenish its
			// cache. Cant run two solves concurrently coz of stateful statics.
			final boolean ok; // did the puzzle solve
			synchronized ( ANALYSE_LOCK ) {
				// call-back the LogicalSolver that created me.
				ok = solver.solve(grid, usage, F, F, logHints, logHinterTimes);
			}
			final AHint hint = ok ? new AnalysisHint(this, usage)
					: new WarningHint(this, "Unsolvable", "Unsolvable.html");
			accu.add(hint);
		} catch (HinterruptException ex) {
			return false;
		} catch (Exception ex) {
			StdErr.whinge("WARN: "+Log.me()+" exception", ex);
			accu.add(new WarningHint(this, "Unsolvable", "Unsolvable.html"));
		}
		return true;
	}

	@Override
	public String toString() {
		return "LogicalAnalyser";
	}

}
