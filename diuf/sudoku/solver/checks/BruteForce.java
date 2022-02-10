/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Backup;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.lock.LockingSpeedMode;
import diuf.sudoku.utils.Log;
import java.util.Map;
import java.util.Random;

/**
 * BruteForce implements the NotOneSolution Sudoku solving technique, which
 * determines: Does this Sudoku have one-and-only-one solution. This is Donald
 * Knuths algorithm for recursively guessing cell-values, which is FAST!
 * <p>
 * We solve the puzzle twice:<ul>
 * <li>once forwards (smallest potential value), and
 * <li>once backwards (largest potential value); and
 * <li>if those two solutions are the same then <br>
 * the puzzle has a unique solution, ie the puzzle is valid.
 * <li>If however the puzzle has zero or many solutions then <br>
 * we add a WarningHint (or a subtype thereof) to the IAccumulator.
 * </ul>
 * Be warned that in practice, invalid puzzle often means the solver is broken.
 * Ie BruteForce or one of the fourQuickFoxes doesn't work properly anymore,
 * probably due to a recent change, so most recently modified first. Obviously
 * this only applies if you KNOW the puzzles you're testing ARE valid. Puzzles
 * produced by the Generator are often invalid (many solutions), coz it removes
 * each clue (in a random order) to see if that makes the puzzle invalid.
 * <p>
 * The handling of the solution (for valid puzzles) is interesting. We solve
 * the puzzle twice in order to validate it, so we definitely don't want to
 * wait for it to be solved again if/when we want that solution, instead we
 * cache the correct value of each cell in the {@link Grid#solution} field, so
 * a valid Sudoku Grid contains it's own solution, for future reference by the
 * Validator, et al. I feel the weird, the weird for speed.
 * <p>
 * nb: I kept renaming this class, but returned to BruteForce, because it's
 * effectively a proper noun to algorithm afficionados, apparently. My bad!
 */
public final class BruteForce extends AWarningHinter {

	// these are shorthand because the full names are too long
	private static final int N = REGION_SIZE;	// num cells in region
	private static final int M = N - 1;			// index of last cell
//	private static final int R = ROOT;			// square root of N

	/** If true && VERBOSE_3_MODE then I write stuff to Log.out */
	private static final boolean IS_NOISY = false; // @check false

	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noiseln(String msg) {
		if (Log.MODE >= Log.VERBOSE_3_MODE) {
			Log.format(NL+msg+NL);
		}
	}
	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noisef(String fmt, Object... args) {
		if (Log.MODE >= Log.VERBOSE_3_MODE) {
			Log.format(fmt, args);
		}
	}

	// for the solve methods isReverse parameter.
	private static final boolean FORWARDS = false;
	// for the solve methods isReverse parameter.
	private static final boolean BACKWARDS = true;

	/** the solution is derived. */
	public SolutionHint solutionHint = null;

	// ---------------------- the solveLogically section ----------------------

	// This is the "normal" hints accumulator, nb: we want only one hint.
	private final IAccumulator accu;

	// the singles are both passed the above apcu explicitly.
	private final IHinter[] singlesHinters;

	// HintsApplicumulator is used by locking. It immediately applies each hint
	// to the grid and we carry on searching, and later it'll return true,
	// which is handy for singles that tend to cause singles; and Locking which
	// tend to cause points/claims, and now Locking even searches exhuastively
	// when its accu is a HintsApplicumulator, instead of missing hints when
	// maybes were removed from cells that had already been searched.
	// @param isStringy = true to enbuffenate hints toFullString. // for debug!
	//        isStringy = false to not waste time enbuffenating. // normal use!
	//  the passed AHint.printHintHtml is only true in LogicalSolverTester when
	//  Run.PRINT_HINT_HTML is true and we re-process ONE puzzle, coz output is
	//  too verbose for "normal" use (could fill C: which is PANIC).
	// @param isAutosolving = true, so it'll set any subsequent singles coz it
	//  can do it faster than THE_SINGLES, coz Cell.set has less area to search
	//  (20 siblings, verses 81 cells) for the next cell to set. This also
	//  means that a Cell.set can "spontaneously" completely fill the grid.
	private final HintsApplicumulator apcu;

	// The "fast" hinters evaluated by ns/elim.
	// Locking uses the HintsApplicumulator under the hood, and then adds a
	// SummaryHint the "normal" accu, whose apply returns numElims, so that
	// everything else still works as per normal.
	private final IHinter[] fourQuickFoxes;

	/**
	 * Constructs a new BruteForce using the given basic hinters.
	 * @param basics the basic hinters
	 */
	public BruteForce(final Map<Tech,IHinter> basics) {
		super(Tech.NotOneSolution);
		accu = new SingleHintsAccumulator();
		apcu = new HintsApplicumulator(AHint.printHintHtml);
		this.singlesHinters = new IHinter[] {
			// nb: hidden first, for speed, when we run them both anyway
			  basics.get(Tech.HiddenSingle)
			, basics.get(Tech.NakedSingle)
		};
		this.fourQuickFoxes = new IHinter[] {
			  new LockingSpeedMode(apcu) // ALWAYS a new one, for speed mode!
			, basics.get(Tech.NakedPair)
			, basics.get(Tech.HiddenPair)
			, basics.get(Tech.Swampfish)
		};
	}

	/**
	 * Build a random solved Sudoku puzzle. If the grid has only one solution,
	 * this solution will be found. If however the grid has multiple solutions,
	 * then a pseudo-random one will be returned.
	 *
	 * @param rnd the random number generator (not null).
	 * @return the solution Grid.
	 */
	public Grid buildRandomPuzzle(final Random rnd) {
		final Grid solution = new Grid();
		try {
			apcu.grid = solution;
			boolean success = recursiveSolve(solution, 1, FORWARDS, rnd);
			assert success;
		} finally {
			apcu.grid = null;
		}
		return solution;
	}

	/**
	 * Find the number of ways that this Sudoku grid can be solved.
	 * <p>
	 * The meaning of the returned value is:<ul>
	 *  <li><b>0</b> means the Sudoku has no solution (ie is invalid).
	 * <pre>This actually means that we failed to solve the puzzle
	 * using BruteForce (ie Knuths Brute Force algorithm)
	 * and the Four Quick Foxes:
	 *   * Locking using the HintsApplicumulator
	 *   * NakedSet(Tech.NakedPair)
	 *   * HiddenSet(Tech.HiddenPair)
	 *   * BasicFisherman(Tech.Swampfish).
	 * With the code in good order, no solution is not possible,
	 * therefore (ie never) this puzzle is actually invalid;
	 * or (most likely) most-recently-modified-hinter produced an invalid hint
	 *    so use {@link diuf.sudoku.solver.hinters.Validator#isValid} on it
	 * or (less likely) s__t broke in the Four Quick Foxes;
	 * or (wtf) HintsApplicumulator or BruteForce is rooted.
	 *    Start with whichever has changed most recently.</pre>
	 *  <li><b>1</b> means the Sudoku has exactly one solution (ie is valid)
	 *  <li><b>2</b> means the Sudoku has <b>more than one</b> solution (ie is
	 *   invalid)
	 * </ul>
	 *
	 * @param grid the Sudoku grid
	 * @return int the number of solutions to this puzzle (see above)
	 */
	public int countSolutions(final Grid grid) {
		// NOTE: rebuild the maybes here before we countSolutions
		// (generate only place were grid IS borken; so fix here).
		final Grid f = new Grid(grid); // forwards
		if ( !doRecursiveSolve(f, FORWARDS) ) {
			return 0; // 0 solutions
		}
		final Grid b = new Grid(grid); // backwards
		if ( !doRecursiveSolve(b, BACKWARDS) ) {
			return 0; // 0 solutions (should never happen)
		}
		if ( f.equals(b) ) {
			return 1; // 1 solution: the Sudoku is valid.
		}
		return 2; // 2-or-more solutions: the Sudoku is invalid
	}

	/**
	 * Returns a copy of the given grid that has been solved,
	 * else throws an UnsolvableException.
	 *
	 * @param grid to be solved
	 * @return a copy of the given grid that has been solved
	 */
	public Grid solve(final Grid grid) {
		final Grid copy = new Grid(grid);
		if ( !doRecursiveSolve(copy, FORWARDS) ) {
			// invalid Puzzle or doRecursiveSolve is broken again.
			throw new UnsolvableException("invalid Puzzle.");
		}
		return copy;
	}

	/**
	 * Produce a WarningHint unless this Sudoku has one-and-only-one solution.
	 * <p>
	 * If the Sudoku has:
	 * <ul>
	 *  <li><b>0 solutions:</b> then a {@link WarningHint} is produced: either
	 *   a "Missing maybes" or a "No solution"
	 *  <li><b>1 solution:</b> the Sudoku is valid so the {@code grid.solution}
	 *   field is set, for later retrieval
	 *  <li><b>2 (or more) solutions:</b> The {@link MultipleSolutionsHint}
	 *   contains two of those solutions.
	 * </ul>
	 *
	 * @param grid
	 * @param accu
	 * @return true if the grid is invalid (hint added to accu), else false.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		boolean result = false;
		try {
			final Grid f = new Grid(grid); // FORWARDS solution
			if ( false ) { // @check false
				// DEBUG verify Grid's copy-constructor: VERY SLOW!
				if ( !new Grid.Diff(grid, f).diff(System.out) )
					System.out.println("...same...");
			}
			if ( doRecursiveSolve(f, FORWARDS) ) {
				final Grid b = new Grid(grid); // BACKWARDS solution
				doRecursiveSolve(b, BACKWARDS);
				if ( b.equals(f) ) {
					// the Sudoku is valid: so set grid.solution
					grid.solution = f.values();
					result = false; // there's no hint here
				} else {
					// the Sudoku is invalid: multiple solutions
					accu.add(new MultipleSolutionsHint(this, grid, f, b));
					result = true; // there's a hint here
				}
			} else {
				// the Sudoku is unsolvable (ie invalid)
				// but are we just missing some maybe/s?
				f.copyFrom(grid);
				// restore any missing maybes (effects only missolved puzzles)
				f.rebuildMaybesAndS__t();
				// if there's missing maybes and it solves with them restored
				if ( !f.equals(grid) && doRecursiveSolve(f, BACKWARDS) ) {
					// Missing maybe/s: a cells solution value has been removed
					// from its potential values, so the puzzle is unsolvable.
					accu.add(new WarningHint(this, "Missing maybes"
							, "MissingMaybes.html"));
				} else {
					// the Sudoku is unsolvable in it's current form
					accu.add(LogicalSolverFactory.get().unsolvableHint);
				}
				result = true; // there's a hint here
			}
		} catch (Exception ex) {
			Log.teeTrace(Log.me()+" caught "+ex, ex);
		}
		return result;
	}

	/**
	 * Try to solve the given grid, forwards or backwards.
	 * <p>
	 * The grid can be solved in forward or reverse direction. Both directions
	 * result in the same solution if, and only if, the Sudoku has exactly one
	 * solution.
	 *
	 * @param grid the grid to solve
	 * @param isReverse whether to solve in reverse direction
	 * @param isNoisy true to log, false to shut the ____ up.
	 * @return <tt>true</tt> if the given grid has been solved,
	 *  else <tt>false</tt> if the grid has no solution.
	 */
	private boolean doRecursiveSolve(final Grid grid, final boolean isReverse) {
		boolean result;
		try {
			apcu.grid = grid;
			grid.rebuildMaybes();
			result = recursiveSolve(grid, 1, isReverse, null);
		} catch (UnsolvableException unexpected) { // from the top level.
			result = false; // Sudoku is invalid.
		} finally {
			apcu.grid = null;
		}
		if ( IS_NOISY ) {
			final String s; if(isReverse) s="back"; else s="for";
			noiseln("doRecursiveSolve "+s+"wards returns "+result+NL+grid);
		}
		return result;
	}

	/**
	 * Recursively solves the given puzzle by trying (not very hard) to solve
	 * the puzzle logically and guessing the value of each indeterminate cell.
	 * <p>
	 * When a non-null Random is given I use it to generate a random puzzle.
	 * <p>
	 * This is Donald Knuth's recursive algorithm (guess value of each unknown
	 * cell and prove it right/wrong) with FOUR QUICK FOXES (fast basic Sudoku
	 * solving logic). It's pretty fast.
	 */
	private boolean recursiveSolve(final Grid grid, final int depth
			, final boolean isReverse, final Random rnd) {
		assert depth < GRID_SIZE;
// generate: these are more trouble than they're worth!
//		// hasMissingMaybes() || firstDoubledValue()!=0 || hasHomelessValues()
//		if ( grid.isInvalidated() )
//			return false;
		// (1) Fill-in whatever you can logically (faster than just guessing)
		if ( solveLogically(grid) ) {
			return true;
		}
		// (2) Find the cell with the smallest number of potential values.
		//     nb: the first cell with two potential values is returned, which
		//     fits in with above solveLogically that fills-in any cells with
		//     only one remaining potential value (ie Naked Singles).
		final Cell smallestCell = getSmallestCell(grid);
		if ( smallestCell == null ) { // grid was solved by previous set
			return true;
		}
		// (3) Try each potential value for that cell
		// we need a backup to revert to if/when we guess a wrong value.
		final Backup backup = new Backup(grid);
		// nb: a new values array is used for each level of recursion. I tried
		// having an array-of-arrays (max recursion depth IS bounded) with each
		// auto-growing to be as large as required, but it was SLOWER than just
		// creating new each time, and I can't figure out WHY! Just Shoot Me!
		// LASTER: I now think it's the auto-grow part that snafooed you. Just
		// use an array of int[maxDepth][9] and a size=int[maxDepth]. I wonder
		// what the ACTUAL maxDepth is? Theoretically it's 80-17=63 for solve,
		// and I guess 80 for generate.
		final int[] values = getValuesToGuess(smallestCell, rnd, isReverse);
		final int n = values.length;
		int i = 0;
		for (;;) {
			try {
				// Guess the value of leastCell
				// nb: cell.set(... true) also sets any subsequent naked
				//     or hidden singles, and may throw UnsolvableException
				smallestCell.set(values[i], 0, true, null); // Grid.AUTOSOLVE
				if ( recursiveSolve(grid, depth+1, isReverse, rnd) ) {
					return true;
				}
			} catch (UnsolvableException eaten) {
				// guessed wrong, so guess again.
			}
			// note that if ++i==n then this is last possible value of cell, so
			// we don't need to restore the grid, because we're about to return
			// false, leaving it upto our caller to guess again, or return to
			// his caller to guess again, or return to...
			if ( ++i == n ) {
				break;
			}
			grid.restore(backup);
		}
		return false;
	}

	/**
	 * (1) Fill all naked and hidden singles, and eliminate using the four
	 * quick foxes: Locking, NakedPair, HiddenPair, Swampfish; exhaustively.
	 * <p>
	 * In theory logical-solving is not required (pure brute-force suffices),
	 * but in practice some Sudokus require many more iterations without this.
	 *
	 * @throws throws UnsolvableException if the puzzle is unsolvable, which in
	 *  the context of recursive-solving almost certainly means this puzzle
	 *  contains bad guess/s, ie this puzzle is rooted, so back-the-truck-up
	 *  and guess-again. Either that or you've buggered-up a basic hinter.
	 */
	private boolean solveLogically(final Grid grid) {
		AHint hint;
		boolean any; // were any hints found in this pass through the hinters?
		// localise fields for speed
		final HintsApplicumulator apcu = this.apcu;
		final IAccumulator accu = this.accu;
		// reset my hint applicumulator (and my hint accumulator just in case)
		apcu.reset();
		accu.reset();
		// We keep running through the hinters until nobody finds a hint,
		// returning true immediately if the grid is filled (ie solved).
		do {
			any = false;
			// singles use the apcu where-as foxes are applied manually.
			for ( IHinter hinter : singlesHinters ) {
				// apcu immediately applies hints
				if ( (hinter.findHints(grid, apcu)) // throws UnsolvableException
				  && (any|=true)
				  && grid.numSet > 80 ) {
					return true; // it's solved
				}
			}
			for ( IHinter hinter : fourQuickFoxes ) {
				// note that Locking is using the apcu "under the hood".
				if ( hinter.findHints(grid, accu) // throws UnsolvableException
				  // get any apply the hint
				  && (hint=accu.getHint()) != null
				  && (any|=true)
				  && hint.applyQuitely(true, grid) > 0 // throws UnsolvableException
				  && grid.numSet > 80 ) {
					return true;  // it's solved
				}
			}
		} while ( any );
		return false;
	}

	// ----------------------- the guess a value section ----------------------

	/**
	 * (2) Look for the cell with the smallest number of potential values,
	 *     which is fastest because it gives us the highest probability of
	 *     guessing correctly.
	 */
	private static Cell getSmallestCell(final Grid grid) {
		int card;
		Cell leastCell = null;
		int least = VALUE_CEILING; // 1 more than the highest value
		for ( Cell cell : grid.cells ) {
			if ( cell.value == 0
			  && (card=cell.size) < least ) {
				leastCell = cell;
				if ( (least=card) < 3 ) {
					break; // no point looking any further
				}
			}
		}
		return leastCell;
	}

	/**
	 * (3) Try each potential value of the smallestCell.
	 * Note that reverse has no effect when rnd!=null; but AFAIK this is only
	 * ever used forwards, never in reverse, so it doesn't matter.
	 */
	private static int[] getValuesToGuess(final Cell least, final Random rnd
			, final boolean reverse) {
		int count, v, rv, value;
		final int cands = least.maybes;
		// nb: values is a new array, else big cache! So try big cache, but its
		// SLOWER! BFIIK! Upside is generating puzzle is already the fast part.
		// Miss Sudoku is Mrs Lincoln Continental: Easy to fill. Hard to strip.
		final int[] values = new int[least.size];
		final int start; if(reverse) start=M;  else start=0; // INCLUSIVE
		final int stop;  if(reverse) stop=-1;  else stop=N;  // EXCLUSIVE
		final int delta; if(reverse) delta=-1; else delta=1; // back/forwards
		if ( rnd == null ) {
			// populate values with the cells potential values
			for ( count=0,v=start; v!=stop; v+=delta ) {
				if ( (cands & VSHFT[value=v+1]) != 0 ) {
					values[count++] = value;
				}
			}
		} else {
			// random generator given so combine with a random value.
			for ( count=0,v=start,rv=rnd.nextInt(N); v!=stop; v+=delta ) {
				value = ((v+rv) % N) + 1;
				if ( (cands & VSHFT[value]) != 0 ) {
					values[count++] = value;
				}
			}
			// Shuffle the values into random order before returning them.
			// This solves the problem "top-lines tend towards order", it's
			// quick and it has no impact on the rest of the algorithm.
			shuffle(rnd, values);
		}
		return values;
	}

	/** shuffle values once. */
	private static int[] shuffle(final Random rnd, final int[] values) {
		int i1, i2, temp;
		// KRC introduced howMany to sort twice to increase randomness, coz I
		// am seeing LOTS of demi-shuffles, with a better than average chance
		// of atleast three values having not been moved at all. I hope that
		// doubling the number of swaps greatly decreases the chances.
		for ( int i=0,n=values.length,howMany=n<<1; i<howMany; ++i ) {
			i1 = rnd.nextInt(n);
			do {
				i2 = rnd.nextInt(n);
			} while (i2 == i1);
			// swap i1 and i2
			temp = values[i1];
			values[i1] = values[i2];
			values[i2] = temp;
		}
		return values;
	}

}
