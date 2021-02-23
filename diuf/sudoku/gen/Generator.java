/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Run;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.GrabBag;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.checks.RecursiveAnalyser;
import diuf.sudoku.utils.Debug;
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

	private static final int MAX_ANALYSE_FAILURES = 4; // an arbitrary small number

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
	 * Depending on the speed of your PC and the given parameters, generation
	 * can take "a while". This implementation actually repeatedly generates
	 * random puzzles until the difficulty is between the given minD and maxD.
	 * @param syms {@code List<Symmetry>} used to strip cell
	 *  values symmetrically.
	 * @param diff Difficulty the desired degree of difficulty.
	 * @param isExact should the Difficulty minimum be respected?<br>
     * true  means only this Difficulty puzzles are acceptable.<br>
     * false means upto this Difficulty puzzles are acceptable.<br>
     * For Easy, isExact makes no difference.<br>
     * For IDKFA, isExact=false accepts any valid puzzle.<br>
     * This parameter is really a hang-over from the days when the user had to
     * wait upto a minute for a puzzle of specific difficulty to generate. Now
	 * puzzles are cached, but I've retained this parameter because my cactus
	 * isn't done flowering (ie I'm too lazy to get rid of it).
	 * @return the generated grid.
	 */
	public Grid generate(Symmetry[] syms, Difficulty diff, boolean isExact) {
		// Right turn at Albaquerky: delegate to the cache which returns one
		// it prepared earlier, which it then replaces in the background by
		// delegating back to the generate method (below).
		LogicalSolver formerLS = GrabBag.logicalSolver;

		// tell logicalSolver to ask me if we've been interrupted by the user
		// every-so-often, and if so then exit immediately.
		solver.setMonitor((IInterruptMonitor)this);

		// These various ways of defeating logging are a mistake! I should have
		// a Log which can be silenced, and ALL noise goes through the Log.
		GrabBag.logicalSolver = null; // this HACK makes AHint.apply silent
		AHint.SHUT_THE______UP = true;
		try {
			Grid puzzle = cache.generate(syms, diff, isExact);
			return puzzle;
		} finally {
			// tell the logicalSolver that I won't be interrupting him now.
			solver.setMonitor(null);
			GrabBag.logicalSolver = formerLS;
			AHint.SHUT_THE______UP = false;
		}
	}

	// This method recieves the call back from the cache to actually generate
	// a new SudokuPuzzle. It sticks each generated puzzle in the cache, and
	// keeps going until it hits one of the desired Difficulty. This can take
	// a while (5+ minutes), so the puzzle cache is well worth the hastle.
	Grid generate(PuzzleCache cache, Symmetry[] syms, Difficulty wantDifficulty
			, boolean isExact) {
		GrabBag.isGenerating = true;
		AHint.SHUT_THE______UP = true;
		Run.Type prevRunType = Run.type;
		Run.type = Run.Type.Generator;
		try {
			final double minD;
			if ( isExact )
				minD = wantDifficulty.min;
			else 
				minD = 0.0D;
			final double maxD = wantDifficulty.max;
			assert maxD>minD;
			final int n = syms.length; // 1 is OK
			assert n>0;
			Random rnd = new Random();
			Grid puzzle;
			Symmetry symm;
			double pDiff; // puzzleDifficulty
			int analyseFailures = 0;
			int max = 81*81; // nb: most folks won't wait this long
			for ( int i=rnd.nextInt(n); ; i=(i+1)%n ) {
				if ( --max < 1 ) // shot cat: stop this endless loop
					throw new RuntimeException("Generator.generate: tapped out at 81*81 tries!");
				// IDKFA: hardcode symm=None to fix non-understood wandering.
				// (coz it takes distant monarch ages with a random symmetry)
				// There is something SERIOUSLY weird going on with symmetry,
				// I blame the JIT compiler, so I KISS it. A little too ironic?
				if ( wantDifficulty == Difficulty.IDKFA )
					symm = Symmetry.None;
				else
					symm = syms[i];
				// generate a random solution (ie build a solved Sudoku)
				// nb: this is the fast part.
				Grid solution = analyser.buildRandomPuzzle(rnd);
				if(isInterrupted) return null;
				// remove as many clues from solution as possible using symm.
				// nb: this is the slow part.
				puzzle = strip(rnd, solution, symm);
				if ( puzzle == null )
					continue; // just try again
				if(isInterrupted) return null;
				// analyse his difficulty and return him if he in desired range
				// NB: this is the slow part, @maybe make him interruptible?
				AHint.hintNumber = 1;
				Grid copy = new Grid(puzzle);
				// analysing typically subsecond, but is "unbounded".
				// 15 mins is worste ever, so I no use BIG A*E's now.
				solver.prepare(copy);
				try {
					pDiff = solver.analyseDifficulty(copy, maxD);
					if(isInterrupted) return null;
					cache.set(getDifficulty(pDiff), puzzle);
					if ( Log.MODE >= Log.NORMAL_MODE )
						System.out.format("%4.1f\t%-20s\t%s%s", pDiff
								, symm, puzzle.toShortString(), NL);
					if ( pDiff>=minD && pDiff<=maxD )
						return puzzle;
				} catch (UnsolvableException ex) {
					// a hinter produced an invalid hint on this random puzzle,
					// which is no biggy, unless it happens too often, in which
					// case the hinter is most probably totally ROOTED (again)!
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
		} finally {
			GrabBag.isGenerating = false;
			AHint.SHUT_THE______UP = false;
			Run.type = prevRunType; // generator ONLY used in the GUI
		}
	}

	// semi-randomly remove cell values using the given symmetry
	private Grid strip(Random rnd, Grid grid, Symmetry symmetry) {

		Grid solution = new Grid(grid);
		// build the indexes array := randomly shuffled array of ints 0..80
		final int[] indexes = shuffledIndexesNew(rnd, 81);

		int i, index, countDown, y, x, count;  Point[] points;  Cell cell;
		Cell[] gCells = grid.cells;
		Cell[] sCells = solution.cells;
		// the rubbish bin: indices of cells which when cleared don't remove
		// anything, so it's a bit faster if we never try that trick again.
		boolean[] rubbish = new boolean[81];
		boolean isAnyRemoved, wereAnyRemoved;
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
				// we get the random index of this i, and then we get the
				// 1,2,or4 symmetric points from the symmetry for that index.
				index = indexes[i];
				y = index / 9;
				x = index % 9;
				points = symmetry.getPoints(x, y);
				// remove the cell at each point, remembering isAnyRemoved.
				isAnyRemoved = false;
				for ( Point p : points )
					if ( (cell=gCells[p.y*9+p.x]).value != 0 ) {
						cell.value = 0; // nb: this leaves the grid in a dirty (borken) state: we MUST call grid.rebuildMaybesAndS__t() (below) to clean-up the maybes, indexes, counts, and all my s__t.
						isAnyRemoved = true;
					}
				if ( !isAnyRemoved ) {
					// none were removed so remember this index as rubbish
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
					// Solve the puzzle forwards and backwards:
					//  if there are zero solutions then there are 0 solutions
					//  if the two solutions are equal there is 1 solution
					//  if the two solutions differ there's 2-or-more solutions
					switch ( count = analyser.countSolutions(grid) ) {
						case 1: // Success: still a unique solution
							wereAnyRemoved = true; // cell values removed
							break;
						case 2: // Failed: so revert and try the next point.
							for ( Point p : points )
								gCells[p.y*9+p.x].value = sCells[p.y*9+p.x].value;
							// it broke, so remember that this index is rubbish
							rubbish[i] = true;
							// increment the index, skipping existing rubbish.
							for(;;) {
								i = (i+1) % 81; //next indexes
								if(!rubbish[i] || --countDown<1) break;
							}
							break;
						case 0:
							return null;
						default:
							throw new UnsolvableException("Invalid solution count="+count);
					}
				}
			} while ( !wereAnyRemoved && --countDown>0 );
		} while ( wereAnyRemoved );
		grid.rebuildMaybesAndS__t();
		return grid;
	}

	private int[] shuffledIndexesNew(final Random rnd, final int n) {
		final int[] indexes = new int[n];
		for ( int i=0; i<n; ++i )
			indexes[i] = i;
		// shuffle once
		for ( int i=0; i<n; ++i ) {
			int p1 = rnd.nextInt(n);
			int p2;
			do {
				p2 = rnd.nextInt(n);
			} while (p2 == p1);
			// swap p1 and p2
			int temp = indexes[p1];
			indexes[p1] = indexes[p2];
			indexes[p2] = temp;
		}
		return indexes;
	}

	private Difficulty getDifficulty(double desiredMaxDifficulty) {
		// in ASCENDING order: Easy, Medium, Hard, Fiendish, Nightmare
		//                   , Diabolical, IDKFA
		for ( Difficulty d : Difficulty.values() )
			if ( desiredMaxDifficulty <= d.max )
				return d;
		return null;
	}

	/** Called by the stop button to interrupt the symmetry loop in generate.*/
	public void interrupt() {
		this.isInterrupted = true;
	}
}