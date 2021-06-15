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
import diuf.sudoku.solver.checks.WarningHint;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * A LogicalAnalyser is a wrapper that turns the LogicalSolver into an IHinter.
 * It solves a Sudoku puzzle (a Grid) logically, to work-out how difficult that
 * is, and produce a summary of the solving techniques that were applied in
 * order to solve the puzzle.
 * <p>
 * I'm only wafer-thin: I just present the LogicalSolver in IHinter's clothes.
 * LogicalAnalyser and LogicalSolver are codependant, so they MUST be in the
 * same package. The analyser calls back the LogicalSolver passed to it's
 * constructor, so it's best if analysers are always automatic variables (not
 * fields) so that: he comes, he's used, he goes; rather than make the GC deal
 * with a cut-off closed-loop of references.
 * @see diuf.sudoku.solver.checks.AnalysisHint
 * 
 * @author Keith Corlett
 */
public final class LogicalAnalyser extends AWarningHinter
		implements IPreparer
{
	/**
	 * The LogicalSolver that created me (ie was passed to my constructor).
	 */
	private final LogicalSolver solver;
	
	/**
	 * When true the {@link LogicalAnalyser#findHints} method prints out the
	 * hints when each hint is applied to grid.
	 * <p>
	 * Normally isNoisy is false. I'm a public field because the findHints
	 * method is defined by IHinter, and therefore cannot accept additional
	 * parameters, which is a shame. If you set me true then play nice with
	 * the other kiddies and <b>finally</b> set me false again. The single
	 * instance of LogicalAnalyser is shared.
	 * <p>
	 * If you have trouble try creating your own LogicalAnalyser and set it's
	 * isNoisy to true, use it, then loose it. You REALLY don't want isNoisy
	 * true when a puzzle is validated coz the recursive analyser is verbose,
	 * filling-up your hard-disk, especially if it's a small SDD like mine.
	 * <p>
	 * I've tried loads of workarounds for this problem, and am yet to find a
	 * solution that I'm really happy with.
	 */
	public boolean isNoisy = false;

	/**
	 * Note that the only constructor is package visible, and is only called by
	 * LogicalSolver (except in the JUnit test-case, which is a bit trixie).
	 *
	 * @param solver My master LogicalSolver.
	 */
	LogicalAnalyser(LogicalSolver solver) {
		// LogicalAnalysis isn't a real solving technique, it's an agglomerate
		// of all (wanted) techniques, so we just use Tech.Solution, giving us
		// degree 0, and difficulty 0.0, which is NOT representative. Sigh.
		super(Tech.Solution);
		this.solver = solver;
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
	public boolean findHints(Grid grid, IAccumulator accu) {
		try {
			// run the puzzleValidators and the gridValidators seperately
			// here because differentiating a WarningHint is complicated.
			AHint warning = solver.validatePuzzleAndGrid(grid, false);
			if ( warning != null )
				return accu.add(warning);
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
			boolean isSolved;
			synchronized ( GrabBag.ANALYSE_LOCK ) {
				// call-back the LogicalSolver that created me.
				isSolved = solver.solve(grid, usageMap, false, isNoisy, false);
			}
			if ( isSolved )
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
