/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * Implementation of the NakedSingle Sudoku solving technique. A "naked single"
 * has just one potential value (maybe) remaining. The Cell is set to that
 * value when the hint is applied. apply is up in the superclass: ADirectHint.
 * <p>
 * This is the first {@code AHinter} used in LogicalSolver, so it's the first
 * "tech" a newbie programmer is most likely to look at, so it's the best place
 * to explain one simple thing: This project is about solving a Sudoku puzzle
 * quickly, it is not about "nice looking code". Things look "ugly" to you, so
 * you will want to "fix" them. Don't, at least until you know enough about...
 * oh, never mind! Just let it be for a while will ya.
 * <p>
 * So Just Don't. But when you do just because you really, really want to
 * <b><i>(Adams D.)</i></b> then please performance test your changes.
 * Do a top1465 run in {@code LogicalSolverTester} <b>before</b> you make your
 * code changes and keep the .log file; then make your code changes; then run
 * top1465 again, and compare the performance. You may need to run LST several
 * times, because the JIT compiler (see below) is a complete non-deterministic
 * pain-in-the-ass, with cheese, pickles, and a flood of dotaise-bloody-sauce.
 * <p>
 * If it's slower then revert. Yes really, it's that simple. But take heart:
 * maybe half of my attempts to speed it up have turned out to slow it down.
 * This is how we learn. We're all idiots. A smart idiot retains his pearls,
 * chucks his mistakes; and makes a <u>reasonable</u> effort to differentiate
 * between the two.
 * <p>
 * JITTERS: Following each code-change kill (Ctrl-C) the first two-or-three
 * LogicalSolverTester runs about 12..14 puzzles in, and restart it. Look at da
 * solve times, subsequent runs are substantially faster for the first two or
 * three puzzles. That's Java's Just In Time (JIT) compiler, which takes less
 * time when it's not compiling, and there's nothing you can do about it except
 * kill it when you're about a dozen puzzles (or so) in, and restart it. Clear?
 * <p>
 * The JIT compiler uses statistics to determine which methods require actual
 * compilation, and which to run through the interpreter, and it doesn't always
 * get it right, especially when methods that are eventually HAMMERED are not
 * run at all in the first few (I think 5) seconds, which is a pain-in-the-ass
 * to folks like me who are accustomed to writing highly-performant code that
 * is performance-tested in a runtime that runs in comparable time, time after
 * time, without the dotaise-bloody-sauce. The JIT-compiler keeps me guessing
 * about the relative performance of my code, which I really ____ing hate.
 */
public final class NakedSingle extends AHinter {

	public NakedSingle() {
		super(Tech.NakedSingle);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// dereference ONCE coz it's hammered
		final int[] size = grid.size;
		// presume that no hint will be found
		boolean result = false;
		// foreach cell in the grid
		for ( int i=0; i<GRID_SIZE; ++i ) {
			// if this cell has 1 potential value
			if ( size[i] == 1 ) {
				// then raise a hint setting this cell to that value
				result = true;
				if ( accu.add(new NakedSingleHint(this, grid.cells[i], VFIRST[grid.maybes[i]])) ) {
					return result;
				}
			}
		}
		return result;
	}

}
