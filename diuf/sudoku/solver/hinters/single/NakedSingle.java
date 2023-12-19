/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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
 * NakedSingle implements the {@link Tech#NakedSingle} Sudoku solving
 * technique.
 * <p>
 * A "naked single" has just one maybe remaining. The Cell is set to that value
 * when the hint is applied. FYI, apply is in my hints superclass: ADirectHint.
 * <p>
 * This is the first {@code AHinter} used in LogicalSolver, so its the first
 * Tech a newbie programmer is most likely to look at, so here I explain one
 * simple thing: SudokuExplainer is about solving a Sudoku puzzle quickly, its
 * not about nice looking code. S__t is ugly, so you will want to fix it. Just
 * let it be, until you understand enough about... oh, never mind! Just let it
 * be for a while, will ya.
 * <p>
 * Just do not. But when you do, because you really want to (<i>Adams D.</i>),
 * then performance test your changes. Run the batch. New-slowness fails fast.
 * Run {@link diuf.sudoku.test.LogicalSolverTester} <b>before</b> you make your
 * code changes and keep the .log file; then make your code changes; then run
 * the batch again. Compare performances. The batch has priming solves because
 * non-deterministic JIT compilation obfuscates the relative efficiency of the
 * many possible implementations, which I hate, except I am too lazy to hate
 * stuff, so I complain persistently. There is a batch summary per release in
 * {@link diuf.sudoku.BuildTimings_02} because running a pre-hack batch is NOT
 * me. I keep forgetting.
 * <p>
 * If its slower then revert. Yes really. Its that simple. Be brave: maybe half
 * of my attempts to speed it up have slowed it down. Thats how we learn. We
 * are all idiots. A smart idiot retains pearls, throws mistakes, and makes a
 * <u>reasonable</u> effort to differentiate between the two.
 * <p>
 * NakedSingle is used in BruteForce, so performance really matters here.
 */
public final class NakedSingle extends AHinter
{

	public NakedSingle() {
		super(Tech.NakedSingle);
	}

	/**
	 * Find all Naked Single hints in the $grid and add them to $accu.
	 * <p>
	 * NOOBS: The return value of {@link IAccumulator#add} is interesting.
	 * This is the first hinter NOOBS are likely to look at, hence it's well
	 * commented. Comment quality declines as you descend in {@link Tech};
	 * and then goes-off like a frog-in-a-sock in the Chainers, which are
	 * incomprehensible without comments, and are still challenging even
	 * with "heavy" commenting. S__t gets harder as you go down the list.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// Dereference ONCE, coz NakedSingle is hammered. Three uses: normal
		// LogicalSolver, with all the other hinters, BruteForce, and Chainers.
		// A local stack reference is faster to resolve than a heap reference.
		// As a rule of thumb if you reference it 6+ times and then a local
		// stack reference will probably be faster. 4 or 5 are a bit iffy.
		// This is referenced 81 times, so it's a no-brainer.
		final int[] sizes = grid.sizes;
		// presume that no hint will be found
		boolean result = false;
		// foreach cell in the grid
		// fastard: for-indice loop like this is faster than array-iteratoring
		// the Grid.cells array, especially when we use the index to look-up in
		// "parallel arrays" instead of repeatedly dereferencing from the cell
		// because an array lookup is MUCH faster than a struct-dereference.
		int i = 0;
		do
			// if this cell has ONE potential value
			// fastard: the grid.size array exists, and there is a local stack
			// reference to it, for speed
			if ( sizes[i] == 1 ) {
				// FOUND a NakedSingle!
				// set result for return either now, or once grid complete.
				result = true;
				// Construct a hint an add it to the give IAccumulator.
				// accu.add returns true in batch, and GUI's findFirstHint.
				// The determinant is they pass-in a SingleHintsAccumulator.
				//
				// The VFIRST[grid.maybes[i]] also deserves some explanation.
				// We know that this cell has just ONE maybe (sizes[i]==1), so
				// we can set this cell to it is first (and only) maybe, which
				// we can get from the VFIRST array that we pre-made in Values
				// for precisely this purpose.
				//
				// FASTARD: the grid.sizes array exists, and I create a stack
				// reference to it, for speed; but there is no such shortcut to
				// grid.maybes coz we hint MUCH less often than we search, so
				// it takes more time to create the reference than it saves,
				// so we reference the "raw" grid again when we hint. Clear?
				// This s__t is why SE is now "a bit" faster than Juillerat.
				if ( accu.add(new NakedSingleHint(grid, this, grid.cells[i]
											, VFIRST[grid.maybes[i]])) )
					return result;
			}
		while (++i < GRID_SIZE);
		return result;
	}

}
