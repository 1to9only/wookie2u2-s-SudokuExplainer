/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Backup;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.lock.LockingSpeedMode;
import diuf.sudoku.utils.Log;
import java.util.Map;
import java.util.Random;
import static diuf.sudoku.Grid.MAX_EMPTY_CELLS;
import static diuf.sudoku.Values.VSIZE;

/**
 * BruteForce implements {@link Tech#NotOneSolution} Sudoku puzzle validator.
 * I see if a Sudoku has ONE solution using Donald Knuths algorithm for
 * recursively guessing each cell-value, which is fast.
 * <p>
 * We solve the puzzle twice:<ul>
 * <li>once forwards (smallest potential value), and
 * <li>once backwards (largest potential value); and
 * <li>if those two solutions are the same then <br>
 * the puzzle has a unique solution, ergo the Sudoku is valid.
 * <li>If however the puzzle has zero or many solutions then <br>
 * we add a WarningHint (or a subtype thereof) to the IAccumulator.
 * </ul>
 * Note: In practice, invalid often means bad solver, NOT bad puzzle! That is,
 * BruteForce or one of the basic solvers (foxes) is broken, probably by a
 * recent change, so check most recently modified first. Obviously this is more
 * applicable if you are sure the puzzle is valid. Sudokus from the Generator
 * are often invalid (many solutions), because it removes random clues to see
 * if it sends the puzzle invalid.
 * <p>
 * The handling of the solution (for valid puzzles) is interesting. We solve
 * the puzzle twice in order to validate it, so we definitely do not want to
 * waste time solving it again if/when we want that solution, instead we cache
 * the correct value of each cell in the {@link Grid#solution} field, hence a
 * valid Sudoku Grid contains its own solution, for future reference by the
 * Validator, et al. I feel the need, the need for speed.
 * <p>
 * nb: One-upon-a-time I renamed this class, but reverted back to BruteForce,
 * because it is a proper noun for algorithmists. My bad. Soz!
 */
public final class BruteForce extends AWarningHinter {

	// these are shorthand because the full names are too long
	private static final int N = REGION_SIZE;	// num cells in region
	private static final int M = N - 1;			// index of last cell
//	private static final int R = ROOT;			// square root of N

	/** If true && VERBOSE_3_MODE then I write stuff to Log.out */
	private static final boolean IS_NOISY = false; // @check false

	/** {@link Grid#AUTOSOLVE} is false, but mine is true, for speed. */
	private static final boolean AUTOSOLVE = true;

	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noiseln(String msg) {
		if (Log.LOG_MODE >= Log.VERBOSE_3_MODE) {
			Log.format(NL+msg+NL);
		}
	}

	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noisef(String fmt, Object... args) {
		if (Log.LOG_MODE >= Log.VERBOSE_3_MODE) {
			Log.format(fmt, args);
		}
	}

	// for the solve methods isReverse parameter.
	private static final boolean FORWARDS = false;
	// for the solve methods isReverse parameter.
	private static final boolean BACKWARDS = true;

	// for recursiveSolve methods isLogical param
	private static final boolean LOGIC = true;
	// for recursiveSolve methods isLogical param
	private static final boolean NO_LOGIC = false;

	/** the solution is derived. */
	public SolutionHint solutionHint = null;

	// ---------------------- the solveLogically section ----------------------

	// This is the "normal" hints accumulator, nb: we want only one hint.
	private final IAccumulator accu;

	// the singles are both passed the above apcu explicitly.
	private final IHinter[] singles;

	// HintsApplicumulator is used by locking. It immediately applies each hint
	// to the grid and we carry on searching, and later it will return true,
	// which is handy for singles that tend to cause singles; and Locking which
	// tend to cause points/claims, and now Locking even searches exhuastively
	// when its accu is a HintsApplicumulator, instead of missing hints when
	// maybes were removed from cells that had already been searched.
	// @param isStringy = true to enbuffenate hints toFullString. // for debug!
	//        isStringy = false to not waste time enbuffenating. // normal use!
	//  the passed AHint.printHintHtml is only true in LogicalSolverTester when
	//  PRINT_HINT_HTML is true and we re-process ONE puzzle, coz output is too
	//  verbose for "normal" use (could fill C: which is PANIC).
	// @param isAutosolving = true, so it will set any subsequent singles coz it
	//  can do it faster than THE_SINGLES, coz Cell.set has less area to search
	//  (20 siblings, verses 81 cells) for the next cell to set. This also
	//  means that a Cell.set can "spontaneously" completely fill the grid.
	private final HintsApplicumulator apcu;

	// fiveQuickFoxes are five FAST basic hinters, as evaluated by ns/elim.
	// Locking uses the HintsApplicumulator under the hood, and then adds a
	// SummaryHint the "normal" accu, whose apply returns numElims, so that
	// everything else still works as per normal.
	// TwoStringKite is a johnny-come-lately, making it fiveQuickFoxes.
	private final IHinter[] fiveQuickFoxes;

	/**
	 * Constructs a new BruteForce using the given basic hinters.
	 * @param basics the basic hinters
	 */
	public BruteForce(final Map<Tech,IHinter> basics) {
		super(Tech.NotOneSolution);
		accu = new SingleHintsAccumulator();
		apcu = new HintsApplicumulator(AHint.printHintHtml);
		this.singles = new IHinter[] {
			// nb: hidden first, for speed, when we run them both anyway
			  basics.get(Tech.HiddenSingle)
			, basics.get(Tech.NakedSingle)
		};
		this.fiveQuickFoxes = new IHinter[] {
			  // nb: LockingSpeedMode uses my apcu under the hood.
			  new LockingSpeedMode(apcu)
			, basics.get(Tech.NakedPair)
			, basics.get(Tech.HiddenPair)
			, basics.get(Tech.TwoStringKite)
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
		final Grid result = new Grid();
		try {
			apcu.grid = result;
			boolean success = recursiveSolve(result, FORWARDS, rnd, LOGIC, 0);
			assert success;
		} finally {
			apcu.grid = null;
		}
		return result;
	}

	/**
	 * Find the number of ways that this Sudoku grid can be solved.
	 * <p>
	 * The meaning of the returned value is:<ul>
	 *  <li><b>0</b> means the Sudoku has no solution (ie is invalid).
	 * <pre>0 actually means that we failed to solve the puzzle with BruteForce
	 * (Donald Knuths algorithm) only, using no logic, which allways solves
	 * every valid Sudoku, eventually. Hence outside of the Generator, with
	 * (only) the BruteForce code in good order, no solution is simply not
	 * possible. You do not see invalid puzzles in the Generator, so if you are
	 * reading this (and you are not deep in Generator filth) the puzzle is
	 * <b>NOW</b> actually invalid, which does NOT imply that it was invalid to
	 * start with; noting that BruteForce is <u>typically</u> used only on a
	 * freshly loaded puzzle (to solve it twice before we start solving it).
	 *
	 * Most likely theMostRecentlyModifiedHinter applied an invalid hint to the
	 * puzzle sometime <b>before</b> countSolutions is called, specifically to
	 * find out that the puzzle has been rendered invalid...
	 * so use {@link diuf.sudoku.solver.hinters.Validator#isValid} on it
	 * or (wtf) {@link HintsApplicumulator} or BruteForce is rooted, again.
	 * Start with whatever has changed most recently.</pre></li>
	 *  <li><b>1</b> means the Sudoku has exactly one solution (ie is valid)</li>
	 *  <li><b>2</b> means the Sudoku has <b>more than one</b> solution (ie is
	 *   invalid)</li>
	 *  <li><b>other</b> means What are You Doing Dave?</li>
	 * </ul>
	 *
	 * @param grid the Sudoku grid<br>
	 *  WARN: rebuild maybes and the grid before you call countSolutions
	 * @return int the number of solutions to this puzzle (see above)
	 */
	public int countSolutions(final Grid grid) {
		// NO solveLogically, to avert bugs in generator.
		final boolean isLogical = NO_LOGIC;
		final Grid f = new Grid(grid); // forwards
		if ( !doRecursiveSolve(f, FORWARDS, isLogical) )
			return 0; // 0 solutions: the Sudoku is invalid
		final Grid b = new Grid(grid); // backwards
		if ( !doRecursiveSolve(b, BACKWARDS, isLogical) )
			return 0; // should never happen
		if ( f.equals(b) )
			return 1; // the Sudoku is valid
		// There are two-or-more distinct solutions!
		// An invalid puzzle indicates a bug outside of the Generator; but
		// MANY MANY invalid puzzles are routine in the Generator, so ignore.
		if ( !Run.isGenerator() ) {
			// see the two grids to double-check Grid.equals
			Log.teef("BruteForce.countSolutions: 2-or-more solutions isLogical=%s\n", isLogical);
			Log.teef("ORIGINAL:\n%s\n", grid);
			Log.teef("FORWARDS:\n%s\n", f);
			Log.teef("BACKWARDS:\n%s\n", b);
		}
		return 2; // the Sudoku is invalid
	}

	/**
	 * Returns a copy of the given grid that has been solved,
	 * using BruteForce with logic,
	 * else BruteForce without logic,
	 * else throws an UnsolvableException.
	 *
	 * @param grid to be solved
	 * @return a copy of the given grid that has been solved
	 */
	public Grid solve(final Grid grid) {
		// LOGIC is faster! Averting multiple guesses in BruteForce!
		final Grid copy = new Grid(grid);
		if ( doRecursiveSolve(copy, FORWARDS, LOGIC) )
			return copy;
		// NO_LOGIC is safer! Not broken by bugs in basic hinters.
		final Grid copy2 = new Grid(grid);
		if ( doRecursiveSolve(copy2, FORWARDS, NO_LOGIC) )
			return copy2;
		// invalid Puzzle or doRecursiveSolve is broken again.
		throw new UnsolvableException("invalid Puzzle.");
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
	 * @param grid to search
	 * @param accu to add hints to
	 * @return true if the grid is invalid (hint added to accu), else false.
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		boolean result = false;
		try {
			final Grid f = new Grid(grid); // FORWARDS solution
			grid.solution = null;
			if ( doRecursiveSolve(f, FORWARDS, LOGIC) ) {
				final Grid b = new Grid(grid); // BACKWARDS solution
				doRecursiveSolve(b, BACKWARDS, LOGIC);
				if ( b.equals(f) ) {
					// the Sudoku is valid: so set grid.solution
					grid.solution = f.getValues();
					result = false; // there is no hint here
				} else {
					// the Sudoku is invalid: multiple solutions
					accu.add(new MultipleSolutionsHint(this, grid, f, b));
					result = true; // there is a hint here
				}
			} else {
				// the Sudoku is unsolvable (ie invalid)
				// but are we just missing some maybe/s?
				f.copyFrom(grid);
				// restore any missing maybes, ie part correct missolved puzzle
				f.rebuildMaybesAndS__t();
				// if there are missing maybes and it solves with them restored
				if ( !f.equals(grid) && doRecursiveSolve(f, BACKWARDS, LOGIC) )
					// Missing maybe/s: a cells solution value has been removed
					// from its potential values, so the puzzle is unsolvable.
					accu.add(new WarningHint(this, "Missing maybes", "MissingMaybes.html"));
				else // the Sudoku is unsolvable in its current form
					accu.add(new WarningHint(this, "Unsolvable", "Unsolvable.html"));
				result = true; // there is a hint here
			}
		} catch (Exception ex) {
			Log.teeTrace("WARN: "+Log.me()+" caught "+ex, ex);
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
	 * @param isLogical true calls solveLogically for speed, false does not.
	 * @return {@code true} meaning the given puzzle is valid; else <br>
	 *  {@code false} means the puzzle has not been solved, which means that <br>
	 *  (improbable) the puzzle is actually invalid, or <br>
	 *  (far more probable) one of the fiveQuickFoxes or BruteForce is broken,
	 *  so start at the most recent change, and work back.
	 */
	private boolean doRecursiveSolve(final Grid grid, final boolean isReverse
			, final boolean isLogical) {
		boolean result;
		try {
			apcu.grid = grid;
			grid.rebuildMaybes();
			result = recursiveSolve(grid, isReverse, null, isLogical, 0);
		} catch (UnsolvableException ex) {
			// nb: UnsolvableExceptions stack-trace is useless coz it is a
			// pre-generated instance, for speed.
			Log.teeln("WARN: "+Log.me()+": top level of recursiveSolve threw "+ex);
			result = false; // Sudoku is invalid.
		} catch (Exception ex) {
			Log.teeTrace("WARN: "+Log.me()+": top level of recursiveSolve threw ", ex);
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
	 * This is Donald Knuths recursive algorithm (guess value of each unknown
	 * cell and prove it right/wrong) with FIVE QUICK FOXES (fast basic Sudoku
	 * solving logic). It is pretty fast.
	 *
	 * @param grid
	 * @param depth is the recursion depth, for debugging
	 * @param isReverse true solves backwards, ie guess the highest possible
	 *  value first
	 * @param rnd a java.util.Random only when generating a new puzzle, which
	 *  I use to read random numbers from. Note that recursiveSolve does NOT
	 *  reseed rnd, I use whatever I am given. If you have got trouble generating
	 *  an IDKFA then some rnds seem randomer than others, so reseed rnd and
	 *  retry.
	 * @param isLogical true calls solveLogically for speed, false does not.
	 * @param depth zero based current recursion depth, for debugging.
	 * @return was it solved
	 */
	private boolean recursiveSolve(final Grid grid, final boolean isReverse
			, final Random rnd, final boolean isLogical, final int depth) {
		assert depth < MAX_EMPTY_CELLS; // the first level is 0.
// generate: these are more trouble than they are worth!
//		// hasMissingMaybes() || firstDoubledValue()!=0 || hasHomelessValues()
//		if ( grid.isInvalidated() )
//			return false;
		// (1) Fill-in whatever you can logically (faster than just guessing)
		if ( isLogical && solveLogically(grid) )
			return true; // solved
		// (2) Find the cell with the smallest number of potential values.
		//     nb: the first cell with two potential values is returned, which
		//     fits in with above solveLogically that fills-in any cells with
		//     only one remaining potential value (ie Naked Singles).
		final Cell smallestCell = getSmallestCell(grid);
		if ( smallestCell == null )
			return true; // solved
		// (3) Try each potential value for that cell
		// we need a backup to revert to if/when we guess a wrong value.
		final Backup backup = new Backup(grid);
		// nb: a new values array is used for each level of recursion. I tried
		// having an array-of-arrays (max recursion depth IS bounded) with each
		// auto-growing to be as large as required, but it was SLOWER than just
		// creating new each time, and I cannot figure out WHY! Just Shoot Me!
		// LASTER: I now think it is the auto-grow part that snafooed you. Just
		// use an array of int[maxDepth][9] and a size=int[maxDepth]. I wonder
		// what the ACTUAL maxDepth is? Theoretically it is 80-17=63 for solve,
		// and I guess 80 for generate.
		final int[] values = getValuesToGuess(smallestCell.maybes, rnd, isReverse);
		final int n = values.length;
		int i = 0;
		for (;;) {
			try {
				// Guess the value of leastCell
				// nb: cell.set(... true) also sets any subsequent naked
				//     or hidden singles, and may throw UnsolvableException
				smallestCell.set(values[i], 0, true, null); // Grid.AUTOSOLVE
				if ( recursiveSolve(grid, isReverse, rnd, isLogical, depth+1) )
					return true; // solved
			} catch (UnsolvableException eaten) {
				// guessed wrong, so guess again.
			}
			// note that if ++i==n then this is last possible value of cell, so
			// we do not need to restore the grid, because we are about to return
			// false, leaving it upto our caller to guess again, or return to
			// his caller to guess again, or return to...
			if ( ++i == n )
				break;
			grid.restore(backup);
		}
		return false; // unsolvable
	}

	/**
	 * (1) Fill all naked and hidden singles, and eliminate using the five
	 * quick foxes: Locking, NakedPair, HiddenPair, Swampfish; exhaustively.
	 * <p>
	 * In theory logical-solving is not required (pure brute-force suffices),
	 * but in practice some Sudokus require many more iterations without this.
	 *
	 * @throws throws UnsolvableException if the puzzle is unsolvable, which in
	 *  the context of recursive-solving almost certainly means this puzzle
	 *  contains bad guess/s, ie this puzzle is rooted, so back-the-truck-up
	 *  and guess-again. Either that or you have buggered-up a basic hinter.
	 */
	private boolean solveLogically(final Grid grid) {
		AHint hint;
		boolean any; // were any hints found in this pass through the hinters?
		// localise fields for speed
		final HintsApplicumulator apcu = this.apcu;
		final IAccumulator accu = this.accu;
		// reset my hint applicumulator (and my hint accumulator just in case)
		apcu.clear();
		accu.clear();
		// We keep running through the hinters until nobody finds a hint,
		// returning true immediately if the grid is filled (ie solved).
		do {
			any = false;
			// singles use the apcu where-as foxes are applied manually.
			for ( IHinter hinter : singles )
				// apcu immediately applies hints
				if ( (hinter.findHints(grid, apcu)) // throws UnsolvableException
				  && (any|=true)
				  && grid.numSet > 80 )
					return true; // it is solved
			for ( IHinter hinter : fiveQuickFoxes )
				// nb: LockingSpeedMode uses my apcu under the hood.
				if ( hinter.findHints(grid, accu) // throws UnsolvableException
				  // get any apply the hint
				  && (hint=accu.poll()) != null
				  && (any|=true)
				  && hint.applyQuitely(AUTOSOLVE, grid) > 0 // throws UnsolvableException
				  && grid.numSet > 80 )
					return true;  // it is solved
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
		for ( Cell cell : grid.cells )
			if ( cell.value == 0
			  && (card=cell.size) < least ) {
				leastCell = cell;
				if ( (least=card) < 3 )
					break; // no point looking any further
			}
		return leastCell;
	}

	/**
	 * (3) Try each potential value of the smallestCell.
	 * Note that reverse has no effect when rnd!=null; but AFAIK this is only
	 * ever used forwards, never in reverse, so it does not matter.
	 */
	private static int[] getValuesToGuess(final int cands, final Random rnd
			, final boolean reverse) {
		int count, v, rv, value;
		// nb: values is a new array, else big cache! So try big cache, but its
		// SLOWER! BFIIK! Upside is generating puzzle is already the fast part.
		// Miss Sudoku is Mrs Lincoln Continental: Easy to fill. Hard to strip.
		final int[] values = new int[VSIZE[cands]];
		final int start; if(reverse) start=M;  else start=0; // INCLUSIVE
		final int stop;  if(reverse) stop=-1;  else stop=N;  // EXCLUSIVE
		final int delta; if(reverse) delta=-1; else delta=1; // back/forwards
		if ( rnd == null ) {
			// populate values with the cells potential values
			for ( count=0,v=start; v!=stop; v+=delta )
				if ( (cands & VSHFT[value=v+1]) > 0 ) // 9bits
					values[count++] = value;
		} else {
			// random generator given so combine with a random value.
			for ( count=0,v=start,rv=rnd.nextInt(N); v!=stop; v+=delta )
				if ( (cands & VSHFT[value=((v+rv) % N)+1]) > 0 ) // 9bits
					values[count++] = value;
			// Shuffle the values into random order before returning them.
			// This solves the problem "top-lines tend towards order", it is
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
			do i2 = rnd.nextInt(n); while (i2 == i1);
			// swap i1 and i2
			temp = values[i1];
			values[i1] = values[i2];
			values[i2] = temp;
		}
		return values;
	}

}
