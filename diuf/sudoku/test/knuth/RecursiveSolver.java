/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test.knuth;

import static diuf.sudoku.utils.Debug.*;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.single.HiddenSingle;
import diuf.sudoku.solver.hinters.single.NakedSingle;
import diuf.sudoku.solver.hinters.lock.Locking;
import diuf.sudoku.solver.hinters.lock.LockingSpeedMode;
import java.util.EnumMap;


/**
 * diuf.sudoku.test.knuth.RecursiveSolver is Donald Knuth's algorithm.
 * I just added the four Quick Foxes to make it a bit faster.
 * <p>
 * RecursiveSolver is currently <u>only</u> in RecursiveSolverTester. It is not
 * used in the rest of Sudoku Explainer, hence the separate test.knuth package.
 * The SingleSolution is used in the rest of SE. It does basically the same
 * stuff, with cheese. This is the stripped-out racing version: sans cheese,
 * special sauce, pickles, and any other s__t you can think of.
 * SingleSolution is about functionality, but knuth.RecursiveSolver is all
 * about speed. I'm all about speed. Did I mention that I quite like speed. Not
 * that kind ya dumbass. REAL speed. The kind you discover by working at it.
 * <p>
 * I tried adding all sorts of hinters, but they're all SLOWER, so I took them
 * out again. The per run timings tell me what to leave in/out. You need to be
 * careful what you include: some hinters require region.idxs and such, which
 * means we need to grid.rebuildAllRegionsS__t() at the top of solveLogically,
 * and the expenses outweigh the gains. Knuth is/was a bloody genius.
 * <p>
 * It's harder to pick than a broken nose. Suck it and see.
 * <p>
 * You're Knuthing but a bampot!
 *
 * @author Keith Corlett 2013 (IIRC)
 */
public final class RecursiveSolver {

	/**
	 * The TIMINGS. <ul>
	 * <li>Contents: {@code Tech => Timing(calls, time, elims)} <br>
	 *  There is one TIMINGS per Tech used, each with a Timing record of the
	 *  number of times this Tech was invoked, and it's total elapsed time,
	 *  and the number of eliminations it found. Note that setting a cell value
	 *  is counted as equivalent to eliminating 10 maybes (potential values).
	 * <li>Totaling.Overall: TIMINGS are persistent. We skip clearing before
	 *  each puzzle, and there's no need to sum for each puzzle; they're just
	 *  a running total that is there at the end. Simples!
	 * <li>Totaling.Puzzle: TIMINGS are cleared before each puzzle, and summed
	 *  externally at the end of each puzzle, then the sums are summed to get a
	 *  bloody total. Not so simples, but it enables you to process the total
	 *  timing for each Tech for each puzzle separately, which I don't. sigh.
	 * </ul>
	 */
	public static final EnumMap<Tech, Timing> TIMINGS = new EnumMap<>(Tech.class);

	/** the number of guesses required to solve this puzzle. */
	public static int numGuesses = 0;

	// hinters that are fast: basic Sudoku solving logic. */
	private final Timer[] fourQuickFoxes;

	// The apcu is used by locking to apply hints immediately.
	HintsApplicumulator apcu = new HintsApplicumulator(false);

	// Locking must be reset before each solve
	private final LockingSpeedMode locking;

	/**
	 * Constructor.
	 */
	public RecursiveSolver() {
		fourQuickFoxes = new Timer[] {
			  new Timer(new HiddenSingle())
			, new Timer(new NakedSingle())
			, new Timer(new HiddenSet(Tech.HiddenPair))
			, new Timer(locking = new LockingSpeedMode(apcu))
		};
	}

	/**
	 * Solve the given Sudoku puzzle recursively, ie ASAP.
	 *
	 * @param grid Grid to solve.
	 * @param isNoisy is very verbose
	 * @return true if it can be solved, else false.
	 */
	public boolean solve(Grid grid, boolean isNoisy) {
		final boolean result;
		try {
			apcu.grid = grid;
			numGuesses = 0;
			locking.reset();
			result = recursiveSolve(grid, 1, isNoisy);
		} finally {
			apcu.grid = null;
		}
		return result;
	}

	// WARNING: DO NOT RENAME THIS METHOD! IT'S LOOKED FOR IN THE CALLSTACK
	//          TO FILTER OUT SUPERFLOUS ERROR MESSAGES WHEN A CELLS MAYBES
	//          GET ZEROED BECAUSE WE'RE JUST GUESSING AT A CELLS VALUE.
	private boolean recursiveSolve(Grid g, int depth, boolean isNoisy) { // throws UnsolvableException, but only from the top-level, before any cell values have been guessed!
//uncomment the logging code to debug. It's faster without.
//		indentf('>',depth, "%d %s%s", depth, "solveRecursively", NL);
//		String result = "exhuasted"; // used in finally block
//		try {
			if ( g.isInvalidated() ) {
//				result = "unsolvableA: "+g.invalidity;
				return false;
			}
			// This is the only supraknuthian part.
			if ( solveLogically(g) ) { // throws UnsolvableException
//				result = "solved";
				return true;
			}
			if ( g.isInvalidated() ) {
//				result = "unsolvableB: "+g.invalidity;
				return false;
			}
			// 99.9% of cellToGuess's have 2 maybes. 1 could have 3, one day.
			Cell cellToGuess = getCellToGuess(g);
			// nb: we need a new array at each level in recursion
//			int[] valuesToGuess = Values.toArrayNew(cellToGuess.maybes);
			int[] valuesToGuess = VALUESES[cellToGuess.maybes];
			String backup = g.toString();
			for ( int i=0,n=valuesToGuess.length,m=n-1; i<n; ++i ) {
				int valueToGuess = valuesToGuess[i];
				++numGuesses; // static so total since last call to solve(Grid)
				println(); println(g);
//				indentf('=',depth, "%d guess %s = %d%s", depth, cellToGuess, valueToGuess, NL);
				try {
					cellToGuess.set(valueToGuess, 0, true, null); // throws UnsolvableException  // use hardcoded true in case Grid.AUTOSOLVE is currently false
					if ( g.numSet > 80 ) {
//						result = "solverated";
						return true;
					}
					if ( recursiveSolve(g, depth+1, isNoisy) ) { // throws UnsolvableException
//						result = "already solved";
						return true;
					}
				} catch (UnsolvableException ex) {
					// do nothing, just guess again
				}
				if ( i < m ) // no need to restore after the last value
					g.restore(backup);
			}
			return false; // fizzled
//		} finally {
//			indentf('<',depth, "%d %s%s", depth, result, NL);
//		}
	}

	/**
	 * Try to solve the puzzle with the Four Quick Foxes (fast hinters).
	 * There is only enough logic to solve easy Sudoku puzzles. For the
	 * remainder we guess cell values, which is frightfully bloody fast.
	 */
	private boolean solveLogically(Grid grid) { // throws UnsolvableException
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		AHint hint;
		for ( Timer fox : fourQuickFoxes ) {
			if ( fox.findHints(grid, accu)
			  // NB: Locking uses a HintsApplicumulator internally to apply a
			  // hint when it's found so that all pointing/claiming is done
			  // in ONE pass through the grid, coz it's a bit faster that way.
			  // My call to accu.getHint() returns an AppliedHintsSummaryHint,
			  // whose apply just returns the total number of eliminations.
			  // nb: with autosolve on Locking might fill the grid.
			  // nb: Locking chases maybes removed from cells in regions that
			  // have already been searched, by reprocessing dirty regions til
			  // there are no more dirty regions.
			  && (hint=accu.getHint()) != null )
				// apply the hint using Cell.set(Grid.AUTOSOLVE)
				// nb: pass hardcoded true in case Grid.AUTOSOLVE is false
				// nb: DO NOT eat any UnsolvableException from apply! Let it
				// propagate up to recursiveSolve to eat it at the right time.
				// If we haven't yet guessed a cell value then UnsolvableExcep
				// propagates back up to process in RecsursiveSolverTester,
				// which is fine, because the puzzle is broken (or more likely
				// RecursiveSolver is broken).
				fox.timing.elims += hint.applyQuitely(true, grid);
			if ( grid.numSet > 80 )
				return true;
		}
		return false;
	}

	// Look for the cell with the least number of potentials.
	// Seems to give the best results (in term of speed) empirically
	private Cell getCellToGuess(Grid grid) {
		int card;
		Cell leastCell = null;
		int minCard = 10;
		for ( Cell cell : grid.cells )
			if ( cell.value == 0
			  && (card=cell.size) < minCard ) {
				if ( card == 0 )
					throw new UnsolvableException(""+cell.id+" zero maybes");
				minCard = card;
				leastCell = cell;
			}
		// next cell
		return leastCell;
	}

	/** @return "RecursiveSolver" */
	@Override
	public String toString() {
		return "RecursiveSolver";
	}

	/** The timing: count, nanoseconds, eliminated of a hinter. */
	public static final class Timing {
		/** the number of times I've been called. */
		public int calls = 0;
		/** the total time elapsed in target.getHints. */
		public long time;
		/** the total number of maybes eliminated. */
		public int elims;
		public void reset() {
			calls = 0;
			time = 0L;
			elims = 0;
		}
		public void add(Timing t) {
			calls += t.calls;
			time += t.time;
			elims += t.elims;
		}
		// for debugging only
		@Override
		public String toString() {
			return "calls="+calls+" time="+time+" elims="+elims;
		}
	}

	/**
	 * A Timer is just a "delegate wrapper" AHinter to accrue:
	 *    {@code callCount, totalNanoseconds, totalNumEliminations}
	 * of an actual AHinter.
	 */
	private static final class Timer extends AHinter {
		public final AHinter target;
		public final String name;
		public final Timing timing;
		/** Constructor. */
		public Timer(AHinter target) {
			super(target.tech);
			this.target = target;
			this.name = target.tech.name()+" Timer";
			TIMINGS.put(target.tech, this.timing=new Timing());
		}
		/** Get hints delegates to the actual hinter passed to my constructor. */
		@Override
		public boolean findHints(Grid grid, IAccumulator accu) {
			long start = System.nanoTime();
			boolean result = target.findHints(grid, accu);
			timing.time += System.nanoTime() - start;
			++timing.calls;
			return result;
		}
		/** @return target.hinter.name()+" Timer". */
		@Override
		public String toString() {
			return name;
		}
	}
}