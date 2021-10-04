/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * Implementation of the Naked Single Sudoku solving technique. A "naked single"
 * has just one potential value (maybe) remaining. The Cell is set to that value
 * when the hint is applied. The apply method is in the hints superclass:
 * ADirectHint.
 * <p>
 * This is the first {@code AHinter} used in LogicalSolver, so it's the first
 * "tech" a newbie programmer is most likely to look at, so it's the best place
 * to explain one simple thing: This project is about solving a Sudoku puzzle
 * quickly, it is not about "nice looking code". Things will look "ugly" to you,
 * so you will want to "fix" them. Don't, at least until you know enough
 * about... oh, never mind!
 * <p>
 * Just Don't. But when you do just because you really, really want to
 * <b><i>(Adams D.)</i></b> then please performance test your changes.
 * Do a top1465 run in {@code LogicalSolverTester} <b>before</b> you make your
 * code changes and keep the .log file; then make your code changes; then run
 * top1465 again, and compare the performance.
 * <p>
 * If it's slower then revert. Yes really, it's that simple. But take heart:
 * about half of my attempts to speed it up have turned out to slow it down.
 * This is how we learn. We're all idiots. A smart idiot retains his pearls,
 * throws away his crap; and makes a <b>reasonable</b> effort to differentiate
 * between the two.
 * <p>
 * Following each code change kill (Ctrl-c) the first LogicalSolverTester run 5
 * puzzles in, and restart it. Look at the two series of solve times, the second
 * run should be a second or two faster for the first two or three puzzles.
 * That's Javas Just In Time (JIT) compiler, which does a lot less work when
 * it's not compiling, and there's not a damn thing you can do about it, except
 * kill it when you're 5 puzzles (or so) in, and restart it. Clear?
 */
public final class NakedSingle extends AHinter {

	public NakedSingle() {
		super(Tech.NakedSingle);
	}

	// I wish they were all this simple.
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		boolean result = false;
		int value;
		for ( Cell cell : grid.cells )
			if ( cell.size == 1 )
				if ( (value=VFIRST[cell.maybes]) > 0 ) {
					result = true;
					if ( accu.add(new NakedSingleHint(this, cell, value)) )
						return result;
				} else // invalid
					cell.clearMaybes();
		return result;
	}

}
