/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import static diuf.sudoku.gui.Generator.FACTORY_THREAD_NAME;
import static diuf.sudoku.gui.Generator.PRODUCE_THREAD_NAME;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.HinterruptException;
import static diuf.sudoku.solver.LogicalSolver.ANALYSE_LOCK;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.io.IOException;
import static java.lang.Thread.currentThread;
import java.util.ArrayList;
import static diuf.sudoku.Constants.lieDown;

/**
 * A cache of 5 puzzles, one of each Difficulty, to insulate the user from the
 * delay (upto around 10 minutes) to generate an IDKFA (the hardest) Sudoku.
 * <p>
 * Generator delegates to me, and I delegate straight back to him. All I do is
 * the caching and background-threading.
 *
 * @author Keith Corlett Mar 2018
 */
final class GenPuzzleCache {

	// ---------------- static stuff ----------------

	// cache a puzzle for each Difficulty; hardest to easiest coz generating a
	// hard one takes many attempts, which typically finds all easy ones.
	private static final Difficulty[] DIFFICULTIES = Difficulty.reverseValues();

	// number of puzzles to cache.
	private static final int SIZE = DIFFICULTIES.length;

	// puzzle cache
	private static final Grid[] CACHE = new Grid[SIZE];

	// this is called by GenerateDialogs (static) class-init
	private static boolean needFactory = false;
	static void staticInitialiser() {
		try {
			if ( !loadPuzzleCache() ) {
				needFactory = true; // fix it in instance-land
				getInstance(SudokuExplainer.getInstance().getFrame());
			}
		} catch (IOException ex) {
			StdErr.whinge("WARN: staticInitialiser failed", ex);
		}
	}

	private static boolean loadPuzzleCache() throws IOException {
		final ArrayList<String> lines = IO.slurp(IO.PUZZLE_CACHE);
		final int n = lines.size();
		int loaded = 0; // a bitset of loaded Difficulty.ordinal()s
		String line;
		// for each difficulty
		for ( int i=0; i<SIZE; ++i ) {
			// if this line was read and its 81 characters
			if ( i<n && (line=lines.get(i))!=null && line.length()==81 ) {
				// add i into the loaded bitset
				CACHE[i] = new Grid(line);
				loaded |= 1<<i;
			} else {
				CACHE[i] = null;
			}
		}
		return loaded == (1<<SIZE)-1;
	}

	private static void savePuzzleCache() {
		try {
			// 83 is 81 + 2 for Windows lineSeparator
			final StringBuilder sb = SB(83 * SIZE);
			for ( int i=0; i<SIZE; ++i ) {
				if ( CACHE[i] == null )
					sb.append(NL);
				else
					sb.append(CACHE[i].toShortString()).append(NL);
			}
			IO.save(sb.toString(), IO.PUZZLE_CACHE);
		} catch ( IOException ex) {
			StdErr.whinge("WARN: savePuzzleCache failed", ex);
		}
	}

	private static GenPuzzleCache theInstance;
	static GenPuzzleCache getInstance(final SudokuFrame parent) {
		if ( theInstance == null )
			theInstance = new GenPuzzleCache(parent);
 		return theInstance;
	}

	// ---------------- instance stuff ----------------

	final SudokuFrame parent;

	private GenPuzzleCache(final SudokuFrame parent) {
		this.parent = parent;
		if ( needFactory ) { // static initaliser did not fill the cache
			needFactory = false;
			newFactoryThread();
		}
	}

	/**
	 * Set {@code PUZZLE_CACHE[diff.ordinal()] = puzzle} if diff is NOT null,
	 * but note that the given puzzle may be null.
	 *
	 * @param diff
	 * @param puzzle
	 */
	void set(final Difficulty diff, final Grid puzzle) {
		if ( diff != null )
			CACHE[diff.ordinal()] = puzzle;
	}

	// PuzzleCache.generate retrieves a puzzle from the cache and then replaces
	// it in the background; else (cacheMiss) I generate in-line and return it;
	// then generate all difficulties in a background thread.
	Grid generate(final ArrayList<GenSymmetry> symetries
			, final Difficulty difficulty, final boolean isExact) {
		final int d = difficulty.ordinal();
		Grid puzzle = CACHE[d];
		if ( puzzle != null ) {
			// cache-hit: generate a replacement Sudoku in the background.
			CACHE[d] = null;
			produce(symetries, difficulty);
		} else {
			// cache-miss: so generate a new Sudoku in this thread.
			try {
				Log.teeln(currentThread().getName()
						+": Generating "+difficulty+"...");
				final Generator gen = Generator.getInstance();
				gen.setTroubleInParadise(false);
				puzzle = CACHE[d] = gen.actuallyGenerate(this, symetries
						, difficulty, isExact);
				// if the cache is not already full
				if ( !gotAllDifficulties() )
					// fill the cache in the background
					newFactoryThread();
			} catch ( HinterruptException | TuringException eaten ) {
				Run.setHinterrupt(false);
				// Do nothing
			}
		}
		return puzzle;
	}

	private boolean gotAllDifficulties() {
		for ( int i=0; i<SIZE; ++i )
			if ( CACHE[i] == null )
				return false;
		return true;
	}

	private void newFactoryThread() {
		// one at a time!
		if ( Generator.isRunning() )
			return;
		// avert if the Producer hit a snag (an Exception)
		final Generator gen = Generator.getInstance();
		if ( gen.getAndResetTroubleInParadise() )
			return;
		final GenerateDialog dialog = GenerateDialog.getInstance();
		dialog.doDone = false; // the factory does not reset the stop button
		final Thread thread = new Thread(FACTORY_THREAD_NAME) {
			@Override
			public void run() {
				// tell dialog "stop this tread, when asked"
				dialog.addGeneratorThread(this);
				// A very small percentage of puzzles are hard, so finding a
				// hard one almost always fills all the easier level, hence
				// DIFFICULTIES is sorted hardest-to-easiest, so PUZZLE_CACHE
				// index is the reverse of DIFFICULTIES index!
				for ( Difficulty difficulty : DIFFICULTIES ) // hardest first
					if ( CACHE[difficulty.ordinal()] == null
					  // the factory does NOT generate IDKFAs coz it takes too
					  // long, so only happens on user request. Sadly the Genie
					  // does not play nice with GUI findHints/analyse/etc, but
					  // its fine for everything else.
					  && difficulty != Difficulty.IDKFA
					) try {
						final String threadName = currentThread().getName();
						Log.teeln(threadName+": Generating "+difficulty+"...");
						CACHE[difficulty.ordinal()] = gen.actuallyGenerate(
								GenPuzzleCache.this, GenSymmetry.alues()
								, difficulty, true);
					} catch (HinterruptException | TuringException eaten) {
						Run.setHinterrupt(false);
						break;
					}
				savePuzzleCache();
				dialog.doDone = true;
			}
		};
		thread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(thread);
		thread.start();
	}

	/**
	 * produce runs after a cache-hit, to refill the cache by generating a
	 * replacement puzzle: the self-refiller in a self-refilling machine.
	 *
	 * @param symetries Symmetrys selected by the user
	 * @param difficulty the target Difficulty
	 */
	private void produce(final ArrayList<GenSymmetry> symetries
			, final Difficulty difficulty) {
		// determine the useful symmetries from the users selection
		final ArrayList<GenSymmetry> mySyms = usefulSymmetries(difficulty
				, symetries);
		// generate a replacement puzzle in a new thread
		final Thread thread = new Thread(PRODUCE_THREAD_NAME) {
			@Override
			public void run() {
				// Wait a-quarter-second for GUIs analyse to start...
				lieDown(250);
				// then we synchronize so that my background generate starts
				// when the foreground analyse completes, to avoid two solves
				// running concurrently (tripping-over stateful static vars).
				synchronized (ANALYSE_LOCK) {
					try {
						generate(mySyms, difficulty, true);
					} catch (Exception eaten) {
						// Do nothing ~ no "standard" stack trace to StdErr.
					}
					savePuzzleCache();
				}
			}
		};
		thread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(thread);
		thread.start();
	}

	// Determine the useful symmetries to speed-up generation:
	// * IDKFA is never (AFAIK) produced with anything but NONE Symmetry, and
	//   its still too slow (upto like 10 minutes) with that.
	// * Diabolical is too slow (2+ mins) with only 4-and-8-point Symmetries,
	//   so add Symmetry.SMALLS to generate a replacement puzzle in ~20 secs.
	private ArrayList<GenSymmetry> usefulSymmetries(final Difficulty difficulty
			, final ArrayList<GenSymmetry> selectedSymetries) {
		switch ( difficulty ) {
			case IDKFA: return GenSymmetry.NONE;
			case Diabolical: return addSmalls(selectedSymetries);
			default: return selectedSymetries;
		}
	}

	// addSmalls returns all the Symmetry.SMALLS plus the 4-and-8-point
	// selected symmetries. Its a way of ensuring that there is enough
	// small syms to produce a Diabolical puzzle quickly enough. It can
	// take 3+ minutes with only large (4-and-8-point) symmetries. That
	// is an unacceptable excess of analysing for one puzzle.
	private ArrayList<GenSymmetry> addSmalls(final ArrayList<GenSymmetry>
			selectedSymetries) {
		final ArrayList<GenSymmetry> result = new ArrayList<>(
				GenSymmetry.SMALLS.size() + selectedSymetries.size());
		result.addAll(GenSymmetry.SMALLS);
		for ( GenSymmetry ss : selectedSymetries )
			if ( ss.getPoints(0, 0).length > 2 ) // ie 4 or 8
				result.add(ss);
		return result;
	}

}
