/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.checks.AWarningHinter;
import diuf.sudoku.solver.checks.AnalysisHint;
import diuf.sudoku.solver.checks.WarningHint;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * A LogicalAnalyser solves a Sudoku puzzle (a Grid) logically, to work-out
 * how difficult that is, and produce a summary of the solving techniques that
 * were applied in order to solve the puzzle.
 * <p>
 * Well, actually I'm only wafer-thin: just a wrapper to turn a LogicalSolver
 * into an IHinter. LogicalAnalyser and LogicalSolver are co-dependant, so they
 * MUST be in the same package. The analyser calls back the LogicalSolver passed
 * to it's constructor, so it's best if analysers are always automatic variables
 * (not fields) so that: he comes, he's used, he goes (an interesting sex life);
 * rather than make the GC deal with yet another billabong: a cut-off closed
 * loop of references.
 * @see diuf.sudoku.solver.checks.AnalysisHint
 * 
 * @author Keith Corlett
 */
public final class LogicalAnalyser extends AWarningHinter
		implements IPreparer
{

	private final LogicalSolver logicalSolver;

	/**
	 * Note that the only constructor is package visible, and is only called
	 * by LogicalSolver (except in the JUnit test-case, which is a bit trixie).
	 * @param solver My master LogicalSolver.
	 */
	LogicalAnalyser(LogicalSolver solver) {
		// LogicalAnalysis isn't a real solving technique, it's an agglomerate
		// of all (wanted) techniques, so we just use Tech.Solution, giving us
		// degree 0, and difficulty 0.0, which is NOT representative. Sigh.
		super(Tech.Solution);
		this.logicalSolver = solver;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation produces a single AnalysisHint (a subtype of
	 * WarningHint) containing the difficulty rating of the Sudoku puzzle,
	 * and a list of the hints that were applied to solve it.
	 * <p>If the Sudoku is not valid then a "raw" WarningHint is produced,
	 * but the puzzle is pre-validated by the caller (LogicalSolver.analyse)
	 * so the puzzle is valid so this should never happen. Never say never.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		try {
			// run the puzzleValidators and the gridValidators seperately
			// here because differentiating a WarningHint is complicated.
			AHint hint = logicalSolver.validatePuzzleAndGrid(grid, false);
			if ( hint != null )
				return accu.add(hint);
			// NB: synchronized so that the generator thread waits for the
			// analyse (ie solve) to complete before generating a puzzle to
			// replenish its cache. We can't run two solves concurrently coz
			// they use stateful static variables internally. Sigh.
			// KRC 2019 OCT @strech I really should clean-up those statics now
			// that I multithread, but I don't know what-else instead of static
			// variables which reference the stateful objects.
			// KRC 2020 JUN @strech I'm just looking at the code to clean-up
			// unused s__t and see what else I should do. I should create a
			// RunContext instance which knows if we're in the GUI or not, and
			// holds all my stateful objects in non-static fields. I will try
			// this recipe but not right now. I'll need a backup, too much has
			// changed right now for me to do a high risk Ignoto change.
			UsageMap usageMap = new UsageMap();
			boolean result;
			synchronized ( GrabBag.ANALYSE_LOCK ) {
				// call-back the logicalSolver which created me.
				// solve(grid, usage, validate, isNoisy, logHints)
				result = logicalSolver.solve(grid, usageMap, false, false, false);
			}
			if ( result )
				accu.add(new AnalysisHint(this, usageMap));
			else
				accu.add(new WarningHint(this, "Unsolvable", "NoSolution.html"));
		} catch (Exception ex) {
			StdErr.whinge(ex);
			accu.add(new WarningHint(this, ex.toString(), "NoSolution.html"));
		}
		return true;
	}

	@Override
	public String toString() {
		return "Logical Analyser";
	}

	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		// A no-op required for the bloody test case: LogicalAnalyserTester.
	}

}
