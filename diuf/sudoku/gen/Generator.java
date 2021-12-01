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
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Run;
import diuf.sudoku.gui.GenerateDialog;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.checks.BruteForce;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.util.Random;

/**
 * The Generator generates new Sudoku puzzles.
 * <p>
 * The generated puzzles are cached in a file so that the user need only wait
 * for the puzzle to analyse, not generate. The background generator must wait
 * for the foreground analyse to complete before it generates a replacement to
 * refill the cache. If you stop the generator then da cache remains incomplete
 * and will persist in that state, so the next time you press generate you must
 * wait for generation and analyse. Swings and round-a-bouts.
 * <p>
 * Generating a new IDKFA puzzle can take an indeterminate time, ie ages. The
 * longest I've seen was seven minutes, but that was bloody unlucky. I noticed
 * that all IDKFA's used Symmetry.NONE, so now only NONE is used to generate
 * IDKFA's, so if it ever takes that long again you've been really unlucky. It
 * should take a few minutes, tops.
 * <p>
 * Patience is a virtue, but I am unvirtuous, so I cache generated puzzles.
 * I hope that Mr User solves each puzzle generated, giving adequate time to
 * generate a replacement, in the background. Mr Thick hammers the generate
 * button, killing and restarting generate. Intelligence is also a virtue.
 * <p>
 * I apologise for the complexity of this code. I'm a multi-threading noob,
 * which I imagine shows. This code wasn't designed, just hacked together,
 * adding new requirements such as caching as I went along. It is entirely
 * conceivable that a simpler solution is possible. This is just my humble
 * attempt, not an example to be followed.
 */
public final class Generator {

	public static final String GENERATOR_THREAD_NAME = "SE_Generator";
	public static final String PRODUCE_THREAD_NAME = "SE_Producer";
	public static final String FACTORY_THREAD_NAME = "SE_Factory";

	// maximum number of tries generating a puzzle before giving-up.
	// most folks won't wait this long.
	private static final int MAX_TRIES = 4096;

	// an arbitrary number of UsolvableExceptions are acceptable, but too many
	// means one of the Four Quick Foxes is most-probably broken; either that
	// or you've broken BruteForce; or you caught UnsolvableException as
	// Exception and rethrown it, when the intended catch needs UnsolvableEx.
	private static final int MAX_FAILURES = 512;

	// NN is the number of cells in a grid: 9*9 = 81.
	private static final int N = REGION_SIZE;
	private static final int NN = GRID_SIZE;

	// an array of indexes 0..80 in the grid to shuffle
	private static final int[] INDEXES = new int[NN];
	static {
		for ( int i=0; i<NN; ++i )
			INDEXES[i] = i;
	}

	// return an array of N indexes shuffled into pseudo-random order
	private static int[] shuffle(final Random rnd) {
		// shuffle once
		for ( int i=0; i<NN; ++i ) {
			final int a = rnd.nextInt(NN);
			final int b = otherThan(a, rnd); // random other than a
			// swap p1 and p2
			final int temp = INDEXES[a];
			INDEXES[a] = INDEXES[b];
			INDEXES[b] = temp;
		}
		return INDEXES;
	}

	// return a random indice 0..N-1 other than avoid
	private static int otherThan(int avoid, Random rnd) {
		int indice;
		do {
			indice = rnd.nextInt(NN);
		} while (indice == avoid);
		return indice;
	}

	/**
	 * get the Difficulty for this target maximum-difficulty-of-a-puzzle
	 * @param target the maximum of the hint difficulties in this puzzle
	 * @return the first (lowest) Difficulty that covers this puzzle.
	 */
	private static Difficulty getDifficulty(double target) {
		// Easy, Medium, Hard, Fiendish, Nightmare, Diabolical, IDKFA
		for ( Difficulty d : Difficulty.values() )
			if ( target < d.max )
				return d;
		return null;
	}

	// my analyser generates random puzzles
	private final BruteForce analyser;

	// my logicalSolver determines each puzzles difficulty
	private final LogicalSolver solver;

	// my cache of generated puzzles
	private final PuzzleCache cache;

	public static Generator getInstance() {
		if ( theInstance == null )
			theInstance = new Generator();
		return theInstance;
	}
	private static Generator theInstance;

	private Generator() {
		this.solver = LogicalSolverFactory.get();
		this.analyser = new BruteForce(solver.getBasicHinters());
		this.cache = new PuzzleCache();
	}

	/** The "Stop" button stops generate.*/
	public void interrupt() {
		Run.stopGenerate = true;
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	/**
	 * Get a Sudoku grid from the cache matching the given parameters. Called
	 * only by GenerateDialog.GeneratorThread.run()
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
	public Grid cachedGenerate(Symmetry[] syms, Difficulty diff, boolean isExact) {
		// cache's before calling-back my generate method.
		return cache.generate(syms, diff, isExact);
	}

	// This method recieves the call back from the cache to actually generate
	// a new Sudoku puzzle. It sticks each generated puzzle in the cache, and
	// keeps going until it hits one of the desired Difficulty. This can take
	// a while (10 minutes), so the puzzle cache is worth the hastle.
	// BUG: Sometimes generate IDKFA produces a Diabolical coz the actual hint
	// difficulties of the two Difficulty's overlap, coz da actual diff grows,
	// but it is still compared as if it were a "base" difficulty. sigh.
	Grid generate(final PuzzleCache cache, final Symmetry[] symmetries
			, final Difficulty target, final boolean isExact) {
		// one does NOT do null!
		assert cache != null;
		assert symmetries != null;
		assert target != null;
		Run.Type prevRunType = Run.setRunType(Run.Type.Generator);
		Run.stopGenerate = false; // reset from last-time
		Grid puzzle = null; // returned only if in target.min..target.max
		try {
			double d;
			Difficulty difficulty;
			// Diabolical+ are too slow with anything but Symmetry.None
			final boolean small = target.ordinal() < Difficulty.Diabolical.ordinal();
			final Symmetry[] syms = small ? symmetries : null;
			Symmetry sym = small ? null : Symmetry.None;
			final double minD = isExact ? target.min : 0.0D;
			final double maxD = target.max;
			assert minD < maxD; // maxD is exclusive, so they can't be equal
			final int n = small ? syms.length : 1;
			Random rnd = new Random();
			int failures = 0; // number of analyse-failures
			int tries = MAX_TRIES + 1;
			int i = rnd.nextInt(n);
			for (;;) {
				if ( --tries < 1 )
					throw new TuringException(Log.me()+" exceeded "+MAX_TRIES);
				if ( Run.stopGenerate ) {
					puzzle = null;
					break; // interrupted
				}
				if ( small )
					sym = syms[i=(i+1)%n];
				puzzle = strip(rnd, analyser.buildRandomPuzzle(rnd), sym);
				if ( puzzle == null ) {
					break; // strip interrupted, or puzzle completely rooted
				}
				try {
					solver.prepare(puzzle);
					d = solver.analyseDifficulty(puzzle, maxD);
					if ( d == 999.99 ) { // analyse interrupted
						System.out.println(Log.me()+": Interrupted!");
						puzzle = null;
						break;
					}
					difficulty = getDifficulty(d);
					cache.set(difficulty, puzzle);
					if (Log.MODE >= Log.NORMAL_MODE) {
						System.out.format("%4d\t%-20s\t%4.2f %-10s\t%s%s", tries, sym, d, difficulty, puzzle.toShortString(), NL);
					}
					if ( d>=minD && d<=maxD ) { // puzzle is pre-set
						break;
					}
				} catch (UnsolvableException ex) {
//// Silence: I Kill You! Don't tell the user you're incompetent, let them figure it out for themselves!
//					++analyseFailures; // remove the below ++
//					System.out.println(puzzle);
//					System.out.println("WARN: "+analyseFailures+": "+ex);
//					System.out.flush();
//					ex.printStackTrace(System.out);
					if ( ++failures > MAX_FAILURES ) {
						Log.teeln(Log.me()+": GIVE-UP: "+MAX_FAILURES+" exceeded");
						puzzle = null;
						break;
					}
				}
				// recreate each time to generate more IDKFA's
				rnd = new Random();
			}
		} finally {
			Run.type = prevRunType;
		}
		try {
			// if generate completed (ie it was NOT stopped)
			if ( !Run.stopGenerate )
				GenerateDialog.getInstance().generateCompleted();
		} catch (Exception eaten) {
			// Do nothing
		}
//		if ( puzzle == null )
//			Debug.breakpoint();
		return puzzle;
	}

//	private static String debug(boolean[] rubbish) {
//		StringBuilder sb = new StringBuilder(N);
//		for ( int i=0; i<N; ++i )
//			if ( rubbish[i] )
//				sb.append(i%10);
//			else
//				sb.append("_");
//		return sb.toString();
//	}

	// semi-randomly remove cell values using the given symmetry
	private Grid strip(final Random rnd, final Grid grid, final Symmetry symmetry) {
		Point[] points;
		Cell cell;
		int i, idx, countDown, numSolutions, numZeroed;
		boolean anyRemoved;
		final Grid solution = new Grid(grid);
		// randomly shuffle 81 indexes: an array of ints 0..80
		final int[] idxs = shuffle(rnd);
		final Cell[] cells = grid.cells;
		final Cell[] solutionCells = solution.cells;
		// the rubbish bin: indices of cells which when cleared don't remove
		// anything, so it's a bit faster if we never try that trick again.
		final boolean[] rubbish = new boolean[NN];
		final int floor = 18;
		// do while wereAnyRemoved: ie: we found some cells to clear.
		OUTER: do {
			// select a random index 'i' (that's not rubbish) to clear
			for ( countDown=NN,i=rnd.nextInt(NN); rubbish[i] && --countDown>0; )
				i = (i+1) % NN;
			// do while ( none removed OR all 81 have been tested )
			anyRemoved = false;
			do {
				if ( Run.stopGenerate )
					return null; // user pressed Stop
				// get the random index at this i
				idx = idxs[i];
				// get 1,2,or4 points from the symmetry for that index.
				points = symmetry.getPoints(idx%N, idx/N); // (x, y)
				// remove value at each point, remembering how many.
				numZeroed = 0;
				for ( Point p : points )
					if ( (cell=cells[p.i]).value != 0 ) {
						cell.value = 0; // nb: leave grid "dirty", then I call grid.rebuildMaybesAndS__t() to clean-up maybes, indexes, counts, etc.
						++numZeroed;
					}
				if ( numZeroed == 0 ) {
					// none were removed so remember that this index is rubbish
					rubbish[i] = true;
					// increment the index, skipping existing rubbish.
					for(;;) {
						i = (i+1) % NN; //next indexes
						if(!rubbish[i] || --countDown<1) break;
					}
				} else {
					// brute-force solve the puzzle forwards and backwards
					switch ( numSolutions = analyser.countSolutions(grid) ) {
						case 1: // Success: still a unique solution
							anyRemoved = true; // cell values removed
							if ( (grid.numSet-=numZeroed) < floor )
								return grid; // we're all done!
							break;
						case 2: // Failed: so revert and try the next point.
							for ( Point p : points )
								cells[p.i].value = solutionCells[p.i].value;
							// it broke, so remember that this index is rubbish
							rubbish[i] = true;
							// increment the index, skipping existing rubbish.
							for(;;) {
								i = (i+1) % NN; //next indexes
								if(!rubbish[i] || --countDown<1) break;
							}
							break;
						case 0: // the puzzle is COMPLETELY rooted
							return null; // Never happens. Never say never.
						default:
							throw new UnsolvableException("bad numSolutions="+numSolutions);
					}
				}
			} while ( !anyRemoved && --countDown>0 );
		} while ( anyRemoved );
		return grid;
	}

}
