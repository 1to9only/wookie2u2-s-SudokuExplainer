/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import static diuf.sudoku.utils.Debug.*;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.single.HiddenSingle;
import diuf.sudoku.solver.hinters.single.NakedSingle;
import diuf.sudoku.solver.hinters.lock.Locking;
import java.util.LinkedHashMap;


/**
 * RecursiveSolver is Donald Knuth's algorithm, IIRC. I know it's not mine, I
 * just added the four Quick Foxes to make him a bit quicker again.
 * <p>RecursiveSolver is currently only in RecursiveSolverTester. It's not
 * used in the rest of the project; that's the RecursiveAnalyser which does
 * basically the same stuff, with cheese. This is the stripped-out racing
 * version: sans cheese, special sauce, pickles, and any other s__t you can
 * think of. He's about functionality and speed. I'm all about speed.
 * <p>I tried adding Naked and Hidden Triples but even my bestest efforts make
 * solve SLOWER, so I've taken them out again. The per run timings tell me what
 * to leave in/out. It's harder to pick than a broken nose. Suck it an see.
 *
 * @author Keith Corlett 2013 (IIRC)
 */
public final class RecursiveSolver {

	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	/** hinterName => numTimesCalled, totalElapsedNanos, totalNumsEliminated */
	public static final LinkedHashMap<String, Timing> TIMINGS
			= new LinkedHashMap<>(32, 0.75F); // needs to be big enough to not constantly collide, regardless of how many we actually put in there

	/** the number of guesses required to solve this puzzle. */
	public static int numGuesses = 0;

	// fourQuickFoxes: the hinters which solve basic Sudoku puzzles using basic
	// logic, because that's faster than just guessing cell values. */
	private final AHinter[] fourQuickFoxes;
	// we need a reference to Locking to reset him before each solve.
	private final Locking locking;

	/** Constructor - populates the hinters array.
	 * <pre>
  calls    ns/call    nanoseconds  ns/elim   elims Tech
  38228      1,607     61,448,047      100  611010 HiddenSingle
  21350        985     21,049,507      551   38160 NakedSingle
  20582      5,112    105,233,211    4,960   21213 HiddenPair
  14868     12,190    181,243,287    7,083   25588 Locking
  10153      9,907    100,589,969   22,190    4533 NakedPair
   8849     11,511    101,868,134   46,324    2199 NakedTriple
   8354      5,347     44,671,282   45,910     973 Swampfish
   1465    420,548    616,103,437   43,857   14048 Total
   1465    809,754  1,186,290,126   84,445   14048 Other
   1465  1,230,302  1,802,393,563  128,302   14048 Took
 </pre>
	 */
	public RecursiveSolver() {
		// We need a locking var to reset him before each solve.
		// HintsApplicumulator immediately applies hints to the grid.
		// nb: Locking now searches exhaustively when it's accu is a
		// HintsApplicumulator rather than miss hints from removed maybes
		// of a cell in a region that's already been processed.
		// @param isStringy = false so it doesn't waste time stringifying hints.
		// @param isAutosolving = true so Cell.set(isAutosolving=true), which
		//   now finds and fills any subsequent naked/hidden singles, so any
		//   call to set could conceivably fill the grid.
		locking = new Locking(new HintsApplicumulator(false, true));
//  calls	      ns/call	    nanoseconds	      ns/elim	  elims	Tech
//  line below it is the last hinter which was in used. See fastest with
//  everything upto HiddenPair, but not the rest. WTF on it speeds up again.
		fourQuickFoxes = new AHinter[] {
//   1465	    2,899,668	  4,248,014,062	       20,268	 209588	Took
		  new Timer(new HiddenSingle())
//   1465	    1,577,102	  2,310,455,440	       30,373	  76068	Took
		, new Timer(new NakedSingle())
//   1465	    1,711,919	  2,507,962,792	       33,003	  75990	Took
		, new Timer(new HiddenSet(Tech.HiddenPair))
//   1465	    1,037,277	  1,519,611,487	       53,088	  28624	Took FREAK!
//   1465	    1,138,272	  1,667,568,778	      103,710	  16079	Took
//   1465	    1,360,934	  1,993,768,556	      123,998	  16079	Took
//   1465	    1,139,755	  1,669,742,308	      103,846	  16079	Took
//In CMD: cd C:\Users\User\Documents\NetBeansProjects\DiufSudoku
//java -Xms100m -Xmx1000m -jar dist\DiufSudoku.jar top1465.d5.mt >RecursiveSolverTester.stdout.log
//  calls	      ns/call	    nanoseconds	      ns/elim	  elims	Tech
//   1465	    8,935,273	 13,090,175,582	      503,855	  25980	Took
		, new Timer(locking)
//   1465	    1,249,981	  1,831,222,805	      113,889	  16079	Took
//		, new Timer(new NakedSet(Tech.NakedPair))
//   1465	    1,403,264	  2,055,781,857	      140,479	  14634	Took
//		, new Timer(new NakedSet(Tech.NakedTriple))
//   1465	    1,193,686	  1,748,750,327	      122,831	  14237	Took
//		, new Timer(new Fisherman(Tech.Swampfish))
//   1465	    1,176,445	  1,723,492,740	      122,685	  14048	Took
		};
	}

	/**
	 * Solve the given Sudoku puzzle recursively, ie ASAP.
	 * @param grid Grid to solve.
	 * @return true if it can be solved, else false.
	 */
	public boolean solve(Grid grid) {
		numGuesses = 0;
		locking.reset();
		return recursiveSolve(grid, 1);
	}

	// WARNING: DO NOT RENAME THIS METHOD! IT'S LOOKED FOR IN THE CALLSTACK
	//          TO FILTER OUT SUPERFLOUS ERROR MESSAGES WHEN A CELLS MAYBES
	//          GET ZEROED BECAUSE WE'RE JUST GUESSING AT A CELLS VALUE.
	private boolean recursiveSolve(Grid g, int depth) { // throws UnsolvableException, but only from the top-level, before any cell values have been guessed!
//uncomment the logging code to debug. It's faster without.
//		indentf('>',depth, "%d %s%s", depth, "solveRecursively", NL);
//		String result = "exhuasted"; // used in finally block
//		try {
			if ( g.isInvalidated() ) {
//				result = "unsolvableA: "+g.invalidity;
				return false;
			}
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
			int[] valuesToGuess = cellToGuess.maybes.toArrayNew();
			String backup = g.toString();
			for ( int i=0,n=valuesToGuess.length,m=n-1; i<n; ++i ) {
				int valueToGuess = valuesToGuess[i];
				++numGuesses; // static so total since last call to solve(Grid)
				println(); println(g);
//				indentf('=',depth, "%d guess %s = %d%s", depth, cellToGuess, valueToGuess, NL);
				try {
					cellToGuess.set(valueToGuess, 0, true, null); // throws UnsolvableException  // use hardcoded true in case Grid.AUTOSOLVE is currently false
					if ( g.isFull() ) {
//						result = "solverated";
						return true;
					}
					if ( recursiveSolve(g, depth+1) ) { // throws UnsolvableException
//						result = "already solved";
						return true;
					}
				} catch (UnsolvableException meh) {
					// Do nothing, just continue with the next guess, if any
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
	 * Try to solve the puzzle with the Four Quick Foxes (fast hinters). There
	 * is only enough logic to solve really basic Sudoku puzzles; and for the
	 * remainder we guess cell values, which is frightfully bloody fast.
	 */
	private boolean solveLogically(Grid grid) { // throws UnsolvableException
		SingleHintsAccumulator accu = new SingleHintsAccumulator();
		AHint hint;
		for ( AHinter fox : fourQuickFoxes ) {
			if ( fox.findHints(grid, accu)
			  // NB: Locking uses a HintsApplicumulator which applys each
			  // hint as it's found so that all pointing/claimings are handled
			  // in ONE pass through the grid, because it's a bit faster that
			  // way. This subsequent call to accu.getHint() returns an
			  // AppliedHintsSummaryHint, whose apply method just returns the
			  // total number of eliminations.
			  // nb: with autosolve on Locking might fill the grid.
			  // nb: Locking misses hint/s when maybes are removed from
			  // cells in regions which have already been searched. So to do it
			  // properly I'd want an AutoLocking which reprocesses grid til no
			  // hints found; or trickier but more efficient: detect maybe
			  // removed from cell in region already processed, and redo them
			  // only which makes more dirty, and so-on til dirty-queue empty.
			  && (hint=accu.getHint()) != null )
				// apply da hint using Cell.set(Grid.AUTOSOLVE) which may throw
				// an UnsolvableException. No biggy, we've just guessed a wrong
				// cell value, but it'd be nice to react properly if we haven't
				// guessed any cell values yet, and I would do that, if I could
				// figure out HOW.
				// @stretch if recursionDepth==0 or something like that.
				// nb: My hinters are wrapped in timers
				// nb: pass hardcoded true in case Grid.AUTOSOLVE is false
				((Timer)fox).getTiming().numElims += hint.apply(true);
			if ( grid.isFull() )
				return true;
		}
		return false;
	}

	// Look for the cell with the least number of potentials.
	// Seems to give the best results (in term of speed) empirically
	private Cell getCellToGuess(Grid grid) {
		Cell leastCell = null;
		int leastCardinality = 10;
		int cardinality;
		for ( Cell cell : grid.cells )
			if ( cell.value == 0
			  && (cardinality=cell.maybes.size) < leastCardinality ) {
				if ( cardinality == 0 )
					throw new UnsolvableException(""+cell.id+" zero maybes");
				leastCardinality = cardinality;
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
		public int numCalls = 0;

		/** the total time elapsed in target.getHints. */
		public long nsTime;

		/** the total number of maybes eliminated. */
		public int numElims;
	}

	/**
	 * A Timer is just a "delegate wrapper" AHinter to accrue:
	 *    {@code callCount, totalNanoseconds, totalNumEliminations}
	 * of an actual AHinter.
	 */
	private static final class Timer extends AHinter {
		public final AHinter target;
		private final String targetTechName; // just for speed
		public final String name;
		/** Constructor. */
		public Timer(AHinter target) {
			super(target.tech);
			this.target = target;
			this.targetTechName = target.tech.name();
			this.name = target.tech.name()+" Timer";
		}
		/** Get hints delegates to the actual hinter passed to my constructor. */
		@Override
		public boolean findHints(Grid grid, IAccumulator accu) {
			long start = System.nanoTime();
			if ( target.findHints(grid, accu) ) {
				long finish = System.nanoTime();
				Timing timing = getTiming();
				++timing.numCalls;
				timing.nsTime += (finish - start);
				//NB: timing.numElims is updated after hint is applied
				return true;
			}
			// no hint was found
			long finish = System.nanoTime();
			Timing timing = getTiming();
			++timing.numCalls;
			timing.nsTime += (finish - start);
			// NB: timing.eliminated is unchanged
			return false;
		}
		/** @return my target hinters Timing. */
		private Timing getTiming() {
			Timing timing = TIMINGS.get(targetTechName);
			if ( timing == null )
				TIMINGS.put(targetTechName, timing=new Timing());
			return timing;
		}
		/** @return target.hinter.name+" Timer". */
		@Override
		public String toString() {
			return name;
		}
	}
}