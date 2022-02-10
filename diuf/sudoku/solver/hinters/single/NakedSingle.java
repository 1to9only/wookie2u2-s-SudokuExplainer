/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.single;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * NakedSingle implements the NakedSingle Sudoku solving technique.
 * <p>
 * A "naked single" has just one maybe remaining. The Cell is set to that value
 * when the hint is applied. FYI, apply is in the superclass: ADirectHint.
 * <p>
 * This is the first {@code AHinter} used in LogicalSolver, so it's the first
 * "tech" a newbie programmer is most likely to look at, so here I explain one
 * simple thing: This project is about solving a Sudoku puzzle quickly, it's
 * not about nice looking code. S__t is ugly, so you'll really want to fix it.
 * Just Don't, at least until you understand enough about... oh, never mind!
 * Just let it be for a while, will ya.
 * <p>
 * Just Don't. But when you do just because you really, really, really want to
 * <b><i>(Adams D.)</i></b> then ALWAYS performance test your changes. I run
 * the batch in the release process to ensure new-slowness is detected early.
 * Do a top1465 run in {@code LogicalSolverTester} <b>before</b> you make your
 * code changes and keep the .log file; then make your code changes; then run
 * top1465 again, and compare the performance. You may need to restart the
 * batch several times, because the JIT compiler (see below) is a complete
 * non-deterministic pain-in-the-ass, with cheese, pickles, and a flood of
 * dotaise-bloody-sauce.
 * <p>
 * If it's slower then revert. Yes really, it's that simple. But take heart:
 * maybe half of my attempts to speed it up have turned out to slow it down.
 * This is how we learn. We're all idiots. A smart idiot retains his pearls,
 * chucks his mistakes; and makes a <u>reasonable</u> effort to differentiate
 * between the two.
 * <p>
 * JITTERS: Following each code-change kill (Ctrl-C) the first "about-four"
 * LogicalSolverTester runs about 12..14 puzzles in, and restart it. Look at
 * solve times, a subsequent run will be substantially faster for the first two
 * or three puzzles. That's Java's Just In Time (JIT) compiler that takes less
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
		// dereference ONCE coz it's (mildly) hammered.
		// A local stack reference is faster to resolve than a heap reference.
		// As a rule of thumb if you reference it 6+ times and you can create a
		// local stack reference to it then go ahead and do so. It'll probably
		// be faster. 4 and 5 are a bit iffy. Below that forget it.
		final int[] size = grid.size;
		// presume that no hint will be found
		boolean result = false;
		// foreach cell in the grid
		// fastard: for-indice loop like this is faster than array-iteratoring
		// the grid.cells array, especially when we use the index to look-up in
		// "parallel arrays" instead of repeatedly dereferencing from the cell
		// because an array lookup is MUCH faster than a struct-dereference.
		for ( int i=0; i<GRID_SIZE; ++i )
			// if this cell has ONE potential value
			// fastard: the grid.size array exists, and there is a local stack
			// reference to it, for speed
			if ( size[i] == 1 ) {
				// then raise a hint to set this cell to that value.
				// set the result for return either now or when I'm finished.
				result = true;
				// accu.add only returns true in the batch, which passes in an
				// instance of SingleHintsAccumulator, that wants oneOnly hint.
				// The VFIRST[grid.maybes[i]] deserves some explanation.
				// We know that this cell has just ONE maybe (size[i]==1), so
				// we can set this cell to it's first (and only) maybe, which
				// we can get from the VFIRST array that we pre-made in Values
				// for precisely this purpose (among others).
				// fastard: the grid.size array exists, and there's a stack
				// reference to it, for speed; but there is no such reference
				// to grid.maybes coz I hint much less often than I run, so it
				// takes more time to create the reference than it saves, hence
				// it is OK to reference the "raw" grid again when we hint.
				if ( accu.add(new NakedSingleHint(this, grid.cells[i]
											, VFIRST[grid.maybes[i]])) )
					return result;
			}
		return result;
	}

}
