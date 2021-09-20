/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Run;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.checks.RecursiveAnalyser;
import diuf.sudoku.utils.Log;
import java.util.Random;


/**
 * The Generator generates new Sudoku puzzles in conjunction with a fast
 * (brute-force) RecursiveAnalyser and a slower (but still reasonably fast)
 * LogicalSolver.
 * <p>
 * I implement the {@link IInterruptMonitor} interface to abort generation
 * before it is complete because generating a puzzle, especially an IDKFA,
 * can take an indeterminate period, ie ____ing ages.
 * <p>
 * All the IInterruptMonitor does is expose a boolean "isInterrupted" field
 * which I (the Generator) set when the user presses the stop button, and the
 * solver (running in a background thread) reads periodically, and if true
 * then it exits-early, so the whole setup stops reasonably quickly.
 * <p>
 * The generated puzzles are cached in a file so that the user need only wait
 * for the puzzle to analyse, not generate. The background generator must wait
 * for the foreground analyse to complete before it generates a replacement to
 * refill the cache. If you stop the generator then the cache remains incomplete
 * and will persist in that state, so the next time you press generate you must
 * wait for generation and analyse. Swings and round-a-bouts.
 * <p>
 * I can only offer my humble apologies for the insane complexity of this code.
 * It was not designed; more cobbled together stage by stage, adding new
 * requirements such as caching as I went along, hence someone with a much
 * higher IQ than I could conceivably look at the current requirements and
 * reverse engineer a MUCH simpler solution to this set of problems using the
 * latest and greatest libraries and what-not. This is JUST my humble attempt,
 * not an example to be followed. I'm no multi-threading expert, and it shows.
 * I'm also bright enough to not take myself too seriously, and I encourage you
 * to do likewise.
 */
public final class Generator implements IInterruptMonitor {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	// an arbitrary small number of UsolvableExceptions are acceptable, but too
	// many indicate that an IHinter is broken. Four seems to work OK for me.
	private static final int MAX_ANALYSE_FAILURES = 4;

	// Set by the Stop button to interrupt the symmetry loop in generate.
	// nb: I'm a volatile variable, meaning I'm accessed accross threads.
	private volatile boolean isInterrupted = false;

	// my cache of generated puzzles
	private final PuzzleCache cache;

	// my logicalSolver used to determine the difficulty of each puzzle
	private final LogicalSolver solver;

	// my recursiveAnalyser because I'm awkward and difficult and demanding,
	// both of myself and the poeple around me.
	private final RecursiveAnalyser analyser = new RecursiveAnalyser(); // SolutionMode.WANTED

	public Generator() {
		this.cache = new PuzzleCache(this);
		this.solver = LogicalSolverFactory.get();
	}

	@Override
	public boolean isInterrupted() {
		return isInterrupted;
	}

	/**
	 * Generate a Sudoku grid matching the given parameters. Called only by
	 * GenerateDialog.GeneratorThread.run()
	 * <p>
	 * This implementation generates random puzzles until it hits one with a
	 * difficulty between the given minD and maxD. Depending on the hardware,
	 * generation of an IDKFA can take upto 10 minutes, ie too slow.
	 *
	 * @param syms {@code List<Symmetry>} used to strip cell
	 *  values symmetrically.
	 * @param diff Difficulty the desired degree of difficulty.
	 * @param isExact should the Difficulty minimum be respected?<br>
     * true  means only this Difficulty puzzles are acceptable.<br>
     * false means upto this Difficulty puzzles are acceptable.<br>
     * For Easy, isExact makes no difference.<br>
     * For IDKFA, isExact=false accepts any valid puzzle.<br>
     * This parameter is really a hang-over from the days when the user had to
     * wait for a puzzle of specific difficulty to generate. Now that puzzles
	 * are cached it's pretty-much never used, but I've retained this parameter
	 * because there are no bones in ice-cream.
	 * @return the generated grid.
	 */
	public Grid generate(Symmetry[] syms, Difficulty diff, boolean isExact) {
		// tell logicalSolver to ask me if we've been interrupted by the user
		// every-so-often, and if so then exit immediately.
		solver.setInterruptMonitor((IInterruptMonitor)this);
		try {
			return cache.generate(syms, diff, isExact);
		} finally {
			// tell the logicalSolver that I won't be interrupting him now.
			solver.setInterruptMonitor(null);
		}
	}

	// This method recieves the call back from the cache to actually generate
	// a new Sudoku puzzle. It sticks each generated puzzle in the cache, and
	// keeps going until it hits one of the desired Difficulty. This can take
	// a while (10 minutes), so the puzzle cache is worth the hastle.
	Grid generate(PuzzleCache cache, Symmetry[] syms, Difficulty wantDifficulty
			, boolean isExact) {
		Run.Type prevRunType = Run.setRunType(Run.Type.Generator);
		try {
			// the puzzle, stripped of every clue that can be stripped without
			// sending the puzzle invalid
			Grid puzzle;
			Symmetry symm; // the current Symmetry
			double d; // the difficulty rating of this puzzle
			// evaluate this once and for all
			final boolean isIDKFA = wantDifficulty == Difficulty.IDKFA;
			// get and validate the minimum and maximum difficulty sought
			final double minD;
			if ( isExact )
				minD = wantDifficulty.min;
			else
				minD = 0.0D;
			final double maxD = wantDifficulty.max;
			assert maxD>minD;
			// a Symmetry is required
			final int n = syms.length;
			assert n>0; // 1 is OK
			// a random number generator is needed
			Random rnd = new Random();
			// how many UnsolvableExceptions so far
			int analyseFailures = 0;
			// most folks won't wait this long for it to fail
			int max = 81*81;
			// foreach symmetry, starting a random index in the array
			for ( int i=rnd.nextInt(n); ; i=(i+1)%n ) {
				if ( --max < 1 ) // shot cat: stop this endless loop
					throw new RuntimeException("Generator.generate: tapped out at 81*81 tries!");
				// Use only Symmetry.None for IDKFA else it takes too long!
				// There is something SERIOUSLY weird going on with symmetry,
				// I blame the JIT compiler, so I KISS it. A little too ironic?
				if ( isIDKFA )
					symm = Symmetry.None;
				else
					symm = syms[i];
				// build a solved random Sudoku (fast).
				Grid solution = analyser.buildRandomPuzzle(rnd);
				if(isInterrupted) return null;
				// strip as many clues as possible using this Symmetry (slow).
				puzzle = strip(rnd, solution, symm);
				if(isInterrupted) return null;
				// if puzzle NOT null then analyse it, else try next Symmetry.
				// Note that continue is (usually) slower.
				if ( puzzle != null ) {
					// analyse his difficulty and return him if he in desired range
					// NB: this is the slow part, @maybe make him interruptible?
					Grid copy = new Grid(puzzle);
					// analysing typically subsecond, but is "unbounded".
					// 15 mins is worste ever, so I no use BIG A*E's now.
					solver.prepare(copy);
					try {
						d = solver.analyseDifficulty(copy, maxD);
						cache.set(getDifficulty(d), puzzle);
						if ( Log.MODE >= Log.NORMAL_MODE )
							System.out.format("%4.1f\t%-20s\t%s%s", d, symm, puzzle.toShortString(), NL);
						if ( d>=minD && d<=maxD )
							return puzzle;
						if(isInterrupted) return null;
					} catch (UnsolvableException ex) {
						// a hinter produced an invalid hint on this random puzzle,
						// which is no biggy, unless it happens too often, in which
						// case a hinter is most likely COMPLETELY ROOTED (again)!
						if ( ++analyseFailures > MAX_ANALYSE_FAILURES ) {
	// this is just noise now, but keep for when we have problems.
	//						Log.teeln("Generator.generate failed: MAX_ANALYSE_FAILURES="+MAX_ANALYSE_FAILURES+" exceeded.");
	//						Log.teeTrace(ex);
							return null;
						}
					}
					rnd = new Random(); // reseed the Randomiser (the easy way)
					if(isInterrupted) return null;
				}
			}
		} finally {
			Run.type = prevRunType; // generator ONLY used in the GUI
		}
	}

	// semi-randomly remove cell values using the given symmetry
	private Grid strip(Random rnd, Grid grid, Symmetry symmetry) {
		int i, index, countDown, count;  Point[] points;  Cell cell;
		boolean any, wereAnyRemoved;
		final Grid solution = new Grid(grid);
		// randomly shuffle indexes: an array of ints 0..80
		final int[] indexes = shuffledIndexes(rnd, 81);
		final Cell[] gridCells = grid.cells;
		final Cell[] solutionCells = solution.cells;
		// the rubbish bin: indices of cells which when cleared don't remove
		// anything, so it's a bit faster if we never try that trick again.
		final boolean[] rubbish = new boolean[81];
		// do while wereAnyRemoved: ie: we found some cells to clear.
		do {
			countDown = 81; // Number of indexes to check: we stop at 0.
			// select a random index 'i' (that's not rubbish) to clear
			for ( i=rnd.nextInt(81); rubbish[i] && --countDown>0; )
				i = (i+1) % 81;
			// do while ( No cell values were removed
			//         OR All 81 indexes have been tested )
			// Eventually we'll go through all 81 indexes without removing any
			// cell values, ie: (!wereAnyRemoved && countDown==0). To expedite
			// process I've introduced a rubbish-bin for indexes that're known
			// to be bad, rather than repeatedly retesting the same index.
			// I presume that once removal of index-and-his-symmetries breaks
			// the puzzle it always breaks the puzzle; ie removal of other cell
			// values (as the algorithm continues) only ever make breakage more
			// probable (never less possible).
			wereAnyRemoved = false;
			do {
				// isInterrupted is set when the user presses the Stop button.
				if(isInterrupted) return null;
				// get the random index at this i
				index = indexes[i];
				// get 1,2,or4 points from the symmetry for that index.
				points = symmetry.getPoints(index%9, index/9); // (x, y)
				// remove the cell at each point, remembering isAnyRemoved.
				any = false;
				for ( Point p : points )
					if ( (cell=gridCells[p.i]).value != 0 ) {
						cell.value = 0; // nb: leave grid "dirty", then I call grid.rebuildMaybesAndS__t() to clean-up maybes, indexes, counts, etc.
						any = true;
					}
				if ( !any ) {
					// none were removed so remember that this index is rubbish
					rubbish[i] = true;
					// increment the index, skipping existing rubbish.
					for(;;) {
						i = (i+1) % 81; //next indexes
						if(!rubbish[i] || --countDown<1) break;
					}
				} else {
					// NOTE: rebuild the maybes here before we countSolutions
					// (generate only place were grid IS borken; so fix here).
					grid.rebuildMaybesAndS__t();
					// bruteForceSolve the puzzle both forwards and backwards
					switch ( count = analyser.countSolutions(grid, false) ) {
						case 1: // Success: still a unique solution
							wereAnyRemoved = true; // cell values removed
							break;
						case 2: // Failed: so revert and try the next point.
							for ( Point p : points )
								gridCells[p.i].value = solutionCells[p.i].value;
							// it broke, so remember that this index is rubbish
							rubbish[i] = true;
							// increment the index, skipping existing rubbish.
							for(;;) {
								i = (i+1) % 81; //next indexes
								if(!rubbish[i] || --countDown<1) break;
							}
							break;
						case 0: // the puzzle is COMPLETELY rooted
							return null; // Never happens. Never say never.
						default:
							throw new UnsolvableException("Unknown countSolutions="+count);
					}
				}
			} while ( !wereAnyRemoved && --countDown>0 );
		} while ( wereAnyRemoved );
		grid.rebuildMaybesAndS__t();
		return grid;
	}

	// return a new array of n indexes shuffled into pseudo-random order
	private static int[] shuffledIndexes(final Random rnd, final int n) {
		assert n <= 81;
		// shuffle once
		for ( int i=0; i<n; ++i ) {
			final int p1 = rnd.nextInt(n);
			final int p2 = otherThan(p1, rnd, n); // random other than p1
			// swap p1 and p2
			final int temp = indexes[p1];
			indexes[p1] = indexes[p2];
			indexes[p2] = temp;
		}
		return indexes;
	}
	private static final int[] indexes = new int[81];
	static {
		// a new array of 0..80
		for ( int i=0; i<81; ++i )
			indexes[i] = i;
	}

	// return a random integer 0..n-1 other than p1
	private static int otherThan(int p1, Random rnd, final int n) {
		int p2;
		do {
			p2 = rnd.nextInt(n);
		} while (p2 == p1);
		return p2;
	}

	private static Difficulty getDifficulty(double targetMax) {
		// Easy, Medium, Hard, Fiendish, Nightmare, Diabolical, IDKFA
		for ( Difficulty d : Difficulty.values() )
			if ( targetMax <= d.max )
				return d;
		return null;
	}

	/** Called by the stop button to interrupt the symmetry loop in generate.*/
	public void interrupt() {
		this.isInterrupted = true;
	}
}