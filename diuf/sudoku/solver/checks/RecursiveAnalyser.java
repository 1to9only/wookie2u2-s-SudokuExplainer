/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.GrabBag;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.SingleHintsAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.fish.BasicFisherman;
import diuf.sudoku.solver.hinters.hdnset.HiddenSet;
import diuf.sudoku.solver.hinters.single.HiddenSingle;
import diuf.sudoku.solver.hinters.nkdset.NakedSet;
import diuf.sudoku.solver.hinters.single.NakedSingle;
import diuf.sudoku.solver.hinters.lock.Locking;
import diuf.sudoku.utils.Log;
import java.util.Random;

/**
 * RecursiveAnalyser implements the Brute Force Sudoku solving technique. It
 * determines: Does this Sudoku have one-and-only-one solution? The Brute
 * Force technique recursively guesses cell values, because it's FAST! We solve
 * the puzzle twice, once 'forwards' (smallest possible) and once 'backwards'
 * (largest possible) and if those two solutions are the same then the puzzle
 * has "a unique solution", ie one-and-only-one possible solution, ie the given
 * grid (ie puzzle) is valid. If the given grid is invalid then we add a
 * WarningHint (or subtype thereof) to the IAccumulator.
 * <p>
 * The handling of the solution grid (presuming there is one) is determined by
 * the SolutionMode passed into my constructor. Basically, we solve the puzzle
 * twice to validate it, so we don't want to wait for it to be solved again
 * if/when we want the bloody solution, we want to just rip-it-back out of the
 * LogicalSolver, because we can easily get him from him factory, anywhere,
 * without having to pass bloody references around everywhere.
 */
public final class RecursiveAnalyser extends AWarningHinter {

	/** If true && VERBOSE_3_MODE then I write stuff to Log.out */
	private static final boolean IS_NOISY = false; // @check false

	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noiseln(String msg) {
		if ( Log.MODE >= Log.VERBOSE_3_MODE )
			Log.format(NL+msg+NL);
	}
	/** Set IS_NOISY && Log.VERBOSE_3_MODE to turn me on. */
	private static void noisef(String fmt, Object... args) {
		if ( Log.MODE >= Log.VERBOSE_3_MODE )
			Log.format(fmt, args);
	}

	// for the solve methods isReverse parameter.
	private static final boolean FORWARDS = false;
	// for the solve methods isReverse parameter.
	private static final boolean BACKWARDS = true;

	/** the solution is derived once when mode = STORED. */
	public SolutionHint solutionHint = null;

	/** true produces a SolutionHint for F8 Tools ~ Solve; else<br>
	 * false a plain SudokuSolved hint for solution by natural causes.<br>
	 * Set to true by LogicalSolver.solveRecursively and then reset to false
	 * at the end of each getHints call. */
	public boolean wantSolutionHint = false;

	/** Constructs a new RecursiveAnalyser. */
	public RecursiveAnalyser() {
		super(Tech.SingleSolutions);
	}

	/** Build a random solved Sudoku puzzle. If the grid has only one solution,
	 * this solution will be found. If however the grid has multiple solutions,
	 * then a pseudo-random one will be returned.
	 * @param rnd the random number generator (not null).
	 * @return the solution Grid. */
	public Grid buildRandomPuzzle(Random rnd) {
		Grid solution = new Grid();
		boolean success = recursiveSolve(solution, 1, false, rnd);
		assert success;
		return solution;
	}

	/**
	 * Find the number of ways that this Sudoku grid can be solved.
	 * <p>
	 * The meaning of the returned value is:
	 * <ul>
	 *  <li><b>0</b> means the Sudoku has no solution.
	 *  This actually means that we failed to solve the puzzle using
	 *  RecursiveAnalyser (ie "The Brute Force" algorithm) and the
	 *  current Four Quick Foxes:
	 *  <ul>
	 *   <li>Locking using a HintsApplicumulator</li>
	 *   <li>NakedSet(Tech.NakedPair)</li>
	 *   <li>HiddenSet(Tech.HiddenPair)</li>
	 *	 <li>BasicFisherman(Tech.Swampfish).</li>
	 *	</ul>
	 *  This never happens with the code in its current stable state,
	 *  therefore this puzzle must actually be invalid.
	 *  Either that or (most likely) whichever hinter you are working on has
	 *  produced an invalid hint (Try using the HintValidator on it);
	 *  or (less likely) something is broken in the Four Quick Foxes,
	 *  or the HintsApplicumulator, or the RecursiveAnalyser
	 *  (whichever has changed most recently).</li>
	 *  <li><b>1</b> means the Sudoku has exactly one solution (ie is valid)
	 *  </li>
	 *  <li><b>2</b> means the Sudoku has <b>more than one</b> solution
	 *  (ie is invalid)</li>
	 * </ul>
	 *
	 * @param grid the Sudoku grid
	 * @return int the number of solutions to this puzzle (see above)
	 */
	public int countSolutions(Grid grid) {
		Grid grid1 = new Grid(grid);
		if ( !doRecursiveSolve(grid1, FORWARDS) )
			return 0; // no solution
		Grid grid2 = new Grid(grid);
		doRecursiveSolve(grid2, BACKWARDS);
		return grid1.equals(grid2) ? 1 : 2; // 2 means 2-or-more.
	}

	/**
	 * Quietly find the number of ways that this Sudoku grid can be solved.
	 * <p>
	 * See the {@link #countSolutions} method documentation.
	 * <p>
	 * Quietly means suppress {@code RecursiveAnalyser.countSolutions} logging
	 * from {@code AHint.apply}, {@code Cell.set} and any/all other places
	 * where stuff is logged from inside countSolutions. If you have a bug in
	 * countSolutions then call the {@link countSolutionsNoisily} method in
	 * place of countSolutions/Quietly, debug it, and then revert the call.
	 *
	 * @param grid the Sudoku grid
	 * @return int the number of solutions to this puzzle (see above)
	 */
	public int countSolutionsQuietly(Grid grid) {
		return wrapCountSolutions(grid, null, true);
	}
	/**
	 * Noisily find the number of ways that this Sudoku grid can be solved.
	 * <p>
	 * See the {@link #countSolutions} method documentation.
	 * <p>
	 * Noisily means that logging inside RecursiveAnalyser is switched on.
	 * This version of the method is used for <b>DEBUGGING ONLY!</b>.
	 */
	public int countSolutionsNoisily(Grid grid) {
		return wrapCountSolutions(grid, LogicalSolverFactory.get(), false);
	}
	// wrap countSolutions to turn logging on/off
	public int wrapCountSolutions(Grid grid, LogicalSolver ls, boolean stfu) {
		LogicalSolver prevLS = GrabBag.setLogicalSolver(ls);
		boolean prevSTFU = AHint.setShutThe____Up(stfu);
		int result;
		try {
			result = countSolutions(grid);
		} finally {
			GrabBag.logicalSolver = prevLS;
			AHint.SHUT_THE______UP = prevSTFU;
		}
		return result;
	}

	/**
	 * solveQuicklyAndQuietly sets the given grids solutionValues field, and
	 * returns the solution Grid, just in case you want it.
	 * <p>
	 * The solution is calculated using Donald Knuth's recursive algorithm
	 * (guess the value of each unknown cell and prove it right/wrong)
	 * with FOUR QUICK FOXES (fast basic Sudoku solving logic).
	 * It really is frightfully fast.
	 * <p>
	 * None of the ideas here-in are mine/original, although I expect in time
	 * my implementation thereof will prove "better than a poke in the eye with
	 * a blunt stick".
	 * <p>
	 * CAVEAT EMPTOR: There may still be significant implementation gains to
	 * be had, but "I'm pretty good at this", and I've tried my best.
	 * ~KRC
	 *
	 * @param grid to be solved
	 * @return  solution Grid (a copy of grid that's been solved)
	 */
	public Grid solveQuicklyAndQuietly(Grid grid) {
		// Trixie: setting GrabBag.logicalSolver=null suppresses AHint.logHint
		// within the doRecursiveSolve method, where it's just NOISE.
		final LogicalSolver formerLS = GrabBag.setLogicalSolver(null);
		final boolean formerSTFU = AHint.setShutThe____Up(true);
		Grid solution; // a copy of the given Grid to solve
		try {
			solution = new Grid(grid);
			if ( !doRecursiveSolve(solution, FORWARDS) )
				throw new UnsolvableException("recursiveSolve failed. Puzzle is invalid (probable) or recursiveSolve is rooted again.");
			// set the given grid's solution field to my solved copy
			grid.solutionValues = solution.toValuesArray();
		} finally {
			GrabBag.logicalSolver = formerLS;
			AHint.SHUT_THE______UP = formerSTFU;
		}
		return solution;
	}

	/**
	 * Does this grid have a single solution? ie is this a valid Sudoku?.
	 * <p>
	 * If the Sudoku has:
	 * <ul>
	 *  <li><b>0 solutions:</b> then a WarningHint is produced: either a
	 *   "Missing maybes" or a "No solution"
	 *  <li><b>1 solution:</b> the Sudoku is valid, so the {@code solutionMode}
	 *   field (passed to my constructor) determines if the solution is:
	 *   <ul>
	 *	  <li>IGNORED so we throw the solution away; or
	 *    <li>STORED so we store the solved grid in Grid.solution for later
	 *     retrieval by the kinky hinters who need to know if they're producing
	 *     an invalid hint (currently AlmostLockedSet only); and also store the
	 *     SolutionHint in my solutionHint field for later retrieval; or
	 *    <li>WANTED so store the solution grid in Grid.solution,
	 *     and add a SolutionHint to the IAccumulator.
	 *    <li>Note that using a constructor field works here coz you ALWAYS
	 *     create a RecursiveAnalyser (me), use me, and throw me away! You do
	 *     NOT keep me hanging around like a bad smell to go off in the sun,
	 *     except of course in LogicalSolver itself were we do exactly that,
	 *     rather than wear repeat creation costs, so we CAREfully test him for
	 *     side-effects of all my insane hangy-aroundy s__t, by re-using me
	 *     repeatedly and seeing where s__t breaks. Consider yourself warned.
	 *   </ul>
	 *  <li><b>2 (or more) solutions:</b> The MultipleSolutionsHint contains
	 *   two of those solutions.
	 * </ul>
	 *
	 * @return true if the grid is invalid (hint added to accu), else false.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		boolean result = false;
		try {
			Grid grid1 = new Grid(grid);

if ( false ) { // @check false (definately DEBUG only, way slow!)
	if ( !new Grid.Diff(grid, grid1).diff(System.out) )
		System.out.println("...same...");
}

			if ( doRecursiveSolve(grid1, FORWARDS) ) {
				Grid grid2 = new Grid(grid); // constructor rebuildsEverything
				doRecursiveSolve(grid2, BACKWARDS);
				if ( grid2.equals(grid1) ) {
					// the Sudoku is valid
					// store the solution to this puzzle in the grid itself
					grid.solutionValues = grid1.toValuesArray();
					// cache hint in public field for later collection.
					solutionHint = new SolutionHint(this, grid, grid1, wantSolutionHint);
					// "Did you produce a hint?" true if you wantSolutionHint
					// else it is false.
					result = wantSolutionHint;
				} else {
					// the Sudoku is invalid: multiple solutions
					accu.add(new MultipleSolutionsHint(this, grid, grid1, grid2));
					// so "Did you produce a hint?" is true
					result = true;
				}
			} else {
				String message, filename;
				// the Sudoku is unsolvable (ie invalid)
				// but are we just missing some maybe/s?
				grid.copyTo(grid1);
				// restore any missing maybes
				grid1.rebuildMaybesAndS__t();
				// if maybes were restored then solve it again Sam.
				if ( !grid1.equals(grid) && doRecursiveSolve(grid1, BACKWARDS) ) {
					// Just missing some maybe/s, ie a cells solution value has
					// been eliminated from its potential values, rendering the
					// puzzle unsolvable.
					message = "Missing maybes";
					filename = "NoMissingMaybes.html";
				} else {
					// Congratulations, it's a piece of crap ANNDD Frighten It!
					message = "No solution";
					filename = "NoSolution.html";
				}
				accu.add(new WarningHint(this, message, filename));
				result = true;
			}
		} finally {
			wantSolutionHint = false; // reset it for next time
		}
		return result;
	}

	/** Try to solve the given grid, forwards or backwards.
	 * <p>The grid can be solved in forward or reverse direction.
	 * Both direction will result in the same solution if, and only if,
	 * the sudoku has exactly one solution.
	 * @param grid the grid to solve
	 * @param isReverse whether to solve in reverse direction
	 * @return <tt>true</tt> if the grid has been solved successfully
	 * (in which case the grid is filled with the solution), or
	 * <tt>false</tt> if the grid has no solution. */
	private boolean doRecursiveSolve(Grid grid, boolean isReverse) {
		boolean isSolved;
		try {
			isSolved = recursiveSolve(grid, 1, isReverse, null);
		} catch (UnsolvableException unexpected) {
			// from the top level of recursiveSolve, meaning that the puzzle
			// is invalid somehow.
			isSolved = false;
		}
		if ( IS_NOISY )
			noiseln("\t\t\tRecursiveAnalyser.doRecursiveSolve "
			  +(isReverse?"back":"for")+"wards returns "+isSolved+NL+grid);
		return isSolved;
	}

	/**
	 * Recursively solves the given puzzle by trying (not very hard) to solve
	 * the puzzle logically and guessing the value of each indeterminate cell.
	 * <p>
	 * When a non-null Random is given this method uses it to generate a random
	 * puzzle.
	 */
	private boolean recursiveSolve(Grid grid, int depth, boolean isReverse
			, Random rnd) {
		assert depth < 81;
		// hasMissingMaybes() || firstDoubledValue()!=0 || hasHomelessValues()
		if ( grid.isInvalidated() )
			return false;
		// (1) Fill-in whatever you can logically (faster than just guessing)
		if ( solveLogically(grid) )
			return true;
		// (2) Find the cell with the smallest number of potential values.
		//     nb: the first cell with two potential values is returned, which
		//     fits in with above solveLogically that fills-in any cells with
		//     only one remaining potential value (ie Naked Singles).
		final Cell leastCell = getLeastCell(grid);
		if ( leastCell == null ) // grid was solved by previous set
			return true;
		// (3) Try each potential value for that cell
		// we need a savePoint to revert to if/when we guess a wrong value.
		final Grid savePoint = new Grid(grid);
		// nb: a new values array is used for each level of recursion. I tried
		// having an array-of-arrays (max recursion depth IS bounded) with each
		// auto-growing to be as large as required, but it was SLOWER than just
		// creating new each time, and I can't figure out WHY! Just Shoot Me!
		// LASTER: I now think it's the auto-grow part that snafooed you. Just
		// use an array of int[maxDepth][9] and a size = int[maxDepth]. I wonder
		// what the ACTUAL maxDepth is? Theoretically it's 80-17=63 for solve,
		// and I guess 80 for generate.
		final int[] values = getValuesToGuess(leastCell, rnd, isReverse);
		final int n = values.length;
		int i = 0;
		for (;;) { // a faster while(true) which literally has NO conditional test
			try {
				// Guess the value of leastCell
				// nb: cell.set(... true) also sets any subsequent naked
				//     or hidden singles, and may throw UnsolvableException
				leastCell.set(values[i], 0, true, null); // Grid.AUTOSOLVE
				if ( recursiveSolve(grid, depth+1, isReverse, rnd) )
					return true;
			} catch (UnsolvableException meh) {
				// guessed wrong, so guess again.
			}
			// note that if ++i==n then this is last possible value of cell, and
			// we don't need to restore the grid, because we're about to return
			// false, leaving it upto our caller to guess again, or return to
			// his caller to guess again, or return to...
			if ( ++i == n )
				break;
			savePoint.copyTo(grid);
		}
		return false;
	}

	// ---------------------- the solveLogically section ----------------------

	// HintsApplicumulator immediately applies each hint to the grid and we
	// carry on searching, and later it'll return true, which is especially
	// handy for singles which tend to cause singles; and Locking which
	// tend to cause points/claims, and now Locking even searches
	// exhuastively when it's accu is a HintsApplicumulator. It used to miss
	// hints when maybes were removed from cells that'd already been searched.
	// @param isStringy = true to enbuffenate hints toFullString. // for debug!
	//        isStringy = false to not waste time enbuffenating. // normal use!
	// @param isAutosolving = true, so it'll set any subsequent singles coz it
	// can do it faster than THE_SINGLES, coz Cell.set has less area to search
	// (20 siblings, verses 81 cells) for the next cell to set. This also means
	// that a Cell.set can "spontaneously" completely fill the grid.
	private final HintsApplicumulator apcu = new HintsApplicumulator(false, true);

	// This is the "normal" hints accumulator, nb: we want only one hint.
	private final IAccumulator accu = new SingleHintsAccumulator();

	// the singles are both passed the above apcu explicitly.
	private final AHinter[] THE_SINGLES = new AHinter[] {
		    new HiddenSingle() // NB: hidden first, coz it's quicker this way
		  , new NakedSingle()  // when we run them both anyway. @strech justify.
	};

	// The "fast" hinters evaluated by ns/elim.
	// Locking uses the HintsApplicumulator under the hood, and then adds
	// to the "normal" accu an AppliedHintsSummaryHint, whose apply method
	// returns numElims, so that everything else still works as per normal.
	private final AHinter[] FOUR_QUICK_FOXES = new AHinter[] {
		  new Locking(apcu) // nb: the apcu applies hints immediately
		, new NakedSet(Tech.NakedPair)
		, new HiddenSet(Tech.HiddenPair)
		, new BasicFisherman(Tech.Swampfish)
	};

	/** (1) Fill all naked and hidden singles.
	 * Again, not necessary in theory (pure brute-force will suffice), but
	 * some 17-clued Sudoku would require (too?) many iterations without. */
	private boolean solveLogically(Grid grid) {
		// localise fields for speed
		final HintsApplicumulator myApcu = apcu;
		final IAccumulator myAccu = accu;
		final AHinter[] theSingles = THE_SINGLES;
		final AHinter[] fourQuickFoxes = FOUR_QUICK_FOXES;

		// reset my hint applicumulator and my hint accumulator, just in case
		myApcu.reset();
		myAccu.reset();

		AHint hint;
		boolean any; // were any hints found in this pass through the hinters?
		// We keep running through the hinters until nobody finds a hint,
		// returning true immediately if the grid is filled (ie solved).
		do {
			any = false;
			// the SINGLES use the apcu whereas the FOXES are manually applied.
			for ( AHinter hinter : theSingles )
				// apcu immediately applies hints
				if ( (hinter.findHints(grid, myApcu)) // throws UnsolvableException
				  && (any|=true)
				  && grid.isFull() )
					return true;
			for ( AHinter hinter : fourQuickFoxes )
				// note that Locking is using the apcu "under the hood".
				if ( hinter.findHints(grid, myAccu) // throws UnsolvableException
				  && (hint=myAccu.getHint()) != null
				  && (any|=true)
				  && hint.apply(true) > 0 // Grid.AUTOSOLVE // throws UnsolvableException
				  && grid.isFull() )
					return true;
		} while ( any );
		return false;
	}

	// ----------------------- the guess a value section ----------------------

	/** (2) Look for the cell with the least number of potential values,
	 *     which is fastest because it gives us the highest probability
	 *     of guessing correctly. */
	private Cell getLeastCell(Grid grid) {
		Cell leastCell = null;
		int leastCard = 10;
		int card;
		for ( Cell cell : grid.cells )
			if ( cell.value == 0
			  && (card=cell.maybes.size) < leastCard ) {
				leastCard = card;
				leastCell = cell;
				if ( card == 2 ) // there's no point looking any further
					break;
			}
		return leastCell;
	}

	/** (3) Try each potential value for the leastCell.
	 * Note that isReverse has no effect when rnd!=null. */
	private int[] getValuesToGuess(Cell leastCell, Random rnd
			, boolean isReverse) {
		final int[] SHFT = Values.SHFT;
		final int maybesBits = leastCell.maybes.bits;
		// nb: values is a new array, or we need a big cache! So I tried a big
		// cache, but it was SLOWER than new, and I can't figure out WHY!?!?
		// The upside is generating a random puzzle is already the fast part.
		// Mrs Sudoku is a Lincoln Continental: Easy to fill. Bitch to strip.
		final int[] values = new int[leastCell.maybes.size];
		final int start = isReverse ?  8 : 0; // INCLUSIVE
		final int stop  = isReverse ? -1 : 9; // EXCLUSIVE
		final int delta = isReverse ? -1 : 1; // backwards or forwards
		int count, v, value;
		if ( rnd == null ) {
			// populate 'values' array with the cells maybes (potential values)
			for ( count=0, v=start; v!=stop; v+=delta )
				if ( (maybesBits & SHFT[value=v+1]) != 0 )
					values[count++] = value;
		} else {
			// random generator given so combine with a random value.
			int randomValue = rnd.nextInt(9); // a random start point
			for ( count=0, v=start; v!=stop; v+=delta ) {
				value = ((v+randomValue) % 9) + 1;
				if ( (maybesBits & SHFT[value]) != 0 )
					values[count++] = value;
			}
			// Shuffle the values into random order before returning them.
			// This solves the problem "top-lines tend towards order", it's
			// quick and it has no impact on the rest of the algorithm.
			shuffle(rnd, values);
		}
		return values;
	}

	/** shuffle values once. */
	private int[] shuffle(Random rnd, int[] values) {
		int i1, i2, temp;
		for ( int i=0,n=values.length; i<n; ++i ) {
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