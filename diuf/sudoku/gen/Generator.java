/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import static diuf.sudoku.Difficulty.Diabolical;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Run;
import static diuf.sudoku.gen.PuzzleCache.getPuzzleCache;
import static diuf.sudoku.gui.GenerateDialog.getGenerateDialog;
import diuf.sudoku.solver.LogicalSolver;
import static diuf.sudoku.solver.LogicalSolver.ANALYSE_INTERRUPTED;
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
	// This limit is arbitrary. It's only so high to almost always
	// generate a random IDKFA puzzle (they're rare) before giving-up.
	public static final int MAX_TRIES = 512*1024;

	// An arbitrary proportion of analyse failures are acceptable, but excess
	// means one of the Four Quick Foxes is most-probably broken; either that
	// or you broke BruteForce; or rethrown UnsolvableException as Exception.
	// The catch REQUIRES an UnsolvableException. Sigh.
	//
	// If a puzzle does NOT validate then it doesn't count as a failure.
	//
	// Arbitrarily, 1 in 32 getDifficulty failures is acceptable, but beyond
	// that one may be reasonably sure that ones s__t is completely ____ed.
	public static final int MAX_FAILURES = MAX_TRIES>>5;

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

	public static boolean isGeneratorRunning() {
		final Thread[] threads = new Thread[Thread.activeCount()];
		for ( int i=0,n=Thread.enumerate(threads); i<n; ++i ) {
			if ( GENERATOR_THREAD_NAME.equals(threads[i].getName()) )
				return true;
			if ( PRODUCE_THREAD_NAME.equals(threads[i].getName()) )
				return true;
			if ( FACTORY_THREAD_NAME.equals(threads[i].getName()) )
				return true;
		}
		return false;
	}

	// my analyser generates random puzzles
	private final BruteForce analyser;

	// my logicalSolver determines each puzzles difficulty
	private final LogicalSolver solver;

	// my cache of generated puzzles
	private final PuzzleCache cache;

	public static Generator getGenerator() {
		if ( theInstance == null )
			theInstance = new Generator();
		return theInstance;
	}
	private static Generator theInstance;

	private boolean troubleInParadise;
	void setTroubleInParadise(boolean b) {
		troubleInParadise = b;
	}
	boolean getAndResetTroubleInParadise() {
		boolean result = troubleInParadise;
		troubleInParadise = false;
		return result;
	}

	private Generator() {
		this.solver = LogicalSolverFactory.get();
		this.analyser = new BruteForce(solver.getBasicHinters());
		this.cache = getPuzzleCache();
	}

	/** The "Stop" button stops generate.*/
	public void interrupt() {
		Run.setStopGenerate(true);
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	/**
	 * Get a Sudoku grid from the cache matching the given parameters.
	 * <p>
	 * Called by {@link diuf.sudoku.gui.GenerateDialog.GeneratorThread#run()}
	 * <p>
	 * This implementation generates random puzzles until it hits one with a
	 * difficulty between the given minD and maxD. Depending on the hardware,
	 * generation of an IDKFA can take upto like an hour, ie too slow. JAVA.EXE
	 * (but not Netbeans VM) kills the generator thread; I suspect for safety
	 * because the bastard overheats the CPU. Who knows. IDKFA. Sigh.
	 *
	 * @param symetries {@code List<Symmetry>} used to strip cell
	 *  values symmetrically.
	 * @param difficulty Difficulty the desired degree of difficulty.
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
	public Grid cachedGenerate(final Symmetry[] symetries, final Difficulty difficulty, final boolean isExact) {
		// cache's before calling-back my generate method.
		return cache.generate(symetries, difficulty, isExact);
	}

	// This method recieves the call back from the cache to actually generate
	// a new Sudoku puzzle. It sticks each generated puzzle in the cache, and
	// keeps going until it hits one of the desired Difficulty. This can take
	// a while (10 minutes), so the puzzle cache is worth the hastle.
	// BUG: Sometimes generate IDKFA produces a Diabolical coz the actual hint
	// difficulties of the two Difficulty's overlap, coz da actual diff grows,
	// but it is still compared as if it were a "base" difficulty. sigh.
	Grid actuallyGenerate(final PuzzleCache cache, final Symmetry[] symmetries
			, final Difficulty targetDifficulty, final boolean isExact) {
		// one does NOT do null!
		assert cache != null;
		assert symmetries != null;
		assert targetDifficulty != null;
		Run.setStopGenerate(false); // reset from last-time
		Run.Type prevRunType = Run.setRunType(Run.Type.Generator);
		Grid puzzle = null; // returned only if in target.min..target.max
		try {
			double d; // the difficulty rating of this puzzle, as a double.
			Difficulty difficulty; // the Difficulty of d, as a Difficulty.
			// Diabolical+ are too slow with anything but Symmetry.None
			final boolean small = targetDifficulty.ordinal() < Diabolical.ordinal();
			final Symmetry[] syms = small ? symmetries : null;
			Symmetry sym = small ? null : Symmetry.None;
			final double minD = isExact ? targetDifficulty.min : 0.0D;
			final double maxD = targetDifficulty.max; // ceiling, not max. Sigh.
			assert minD < maxD; // maxD is exclusive, so they can't be equal
			final int n = syms!=null ? syms.length : 1; // n is 1 for "large"
			Random rnd = new Random();
			int failures = 0; // number of analyse-failures
			int tries = MAX_TRIES + 1;
			int i = rnd.nextInt(n);
			for (;;) {
				if ( --tries < 1 )
					throw new TuringException(Log.me()+" exceeded "+MAX_TRIES);
				if ( Run.stopGenerate() ) {
					puzzle = null;
					break; // interrupted
				}
				if ( small )
					sym = syms[i=(i+1)%n];
				puzzle = strip(rnd, analyser.buildRandomPuzzle(rnd), sym);
				if ( puzzle == null ) {
					break; // strip interrupted, or puzzle completely rooted
				}
				// is it a valid Sudoku? if not it doesn't count as a failure
				puzzle.rebuildMaybesAndS__t();
				puzzle.rebuildIndexes(false);
				if ( solver.validatePuzzleAndGrid(puzzle, false) == null ) {
					try {
						// prepare and solve, to findout how hard it is.
						solver.prepare(puzzle);
						d = solver.analyseDifficulty(puzzle, maxD);
						if ( d == ANALYSE_INTERRUPTED ) {
							System.out.println(Log.me()+": Interrupted!");
							puzzle = null;
							break;
						}
						difficulty = Difficulty.get(d);
						cache.set(difficulty, puzzle);
						if (Log.MODE >= Log.NORMAL_MODE) {
							System.out.format("%4d\t%-20s\t%5.2f\t%-10s\t%s%s", tries, sym, d, difficulty, puzzle.toShortString(), NL);
						}
						if ( d>=minD && d<maxD ) { // puzzle is pre-set
							break;
						}
					} catch (UnsolvableException ex) {
// Don't tell da user you're incompetent, let em figure it out for themselves
						++failures; // remove the below ++
						Log.println(puzzle);
						Log.println("WARN: analyse failure "+failures+": "+ex);
// I wish there was an easy way to print-out DISTINCT stack traces.
//						ex.printStackTrace(Log.out);
						Log.out.flush();
						if ( failures > MAX_FAILURES ) {
							Log.teeln(Log.me()+": GIVE-UP: MAX_FAILURES="+MAX_FAILURES+" exceeded");
							puzzle = null;
							break;
						}
					} catch (Exception ex) {
						setTroubleInParadise(true);
						throw ex;
					}
				}
				// recreate each time to generate more IDKFA's
				rnd = new Random();
			}
		} finally {
			Run.type = prevRunType;
		}
		try {
			if ( Run.stopGenerate() ) // stopped
				getGenerateDialog().generateKilled();
			else // completed
				getGenerateDialog().generateCompleted();
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
				if ( Run.stopGenerate() )
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
