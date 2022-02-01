/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import static diuf.sudoku.Difficulty.reverseDifficulties;
import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import static diuf.sudoku.gen.Generator.FACTORY_THREAD_NAME;
import static diuf.sudoku.gen.Generator.PRODUCE_THREAD_NAME;
import static diuf.sudoku.gen.Generator.getGenerator;
import static diuf.sudoku.gen.Generator.isGeneratorRunning;
import diuf.sudoku.gui.GenerateDialog;
import static diuf.sudoku.gui.GenerateDialog.getGenerateDialog;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.HinterruptException;
import static diuf.sudoku.solver.LogicalSolver.ANALYSE_LOCK;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.io.IOException;
import static java.lang.Thread.currentThread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A cache of 5 puzzles, one of each Difficulty, to insulate the user from the
 * delay (upto around 10 minutes) to generate an IDKFA (the hardest) Sudoku.
 * <p>
 * Generator delegates to me, and I delegate straight back to him. All I do is
 * the caching and background-threading.
 *
 * @author Keith Corlett Mar 2018
 */
public final class PuzzleCache {

	// ---------------- static stuff ----------------

	// I cache a puzzle for each Difficulty.
	// NB: hardest to easiest because generating a hard one usually takes
	// many attempts, so it tends to fill-in all easier ones along the way.
	private static final Difficulty[] DIFFICULTIES = reverseDifficulties();

	// number of puzzles to cache.
	private static final int SIZE = DIFFICULTIES.length;

	// puzzle cache
	private static final Grid[] CACHE = new Grid[SIZE];

	// this is called by GenerateDialogs (static) class-init
	private static boolean needFactory = false;
	public static void staticInitialiser() {
		assert SIZE == DIFFICULTIES.length; // invariant (no catch)
		try {
			if ( !loadPuzzleCache() ) {
				needFactory = true; // fix it in instance-land
				getPuzzleCache();
			}
		} catch (IOException ex) {
			StdErr.whinge("staticInitialiser failed", ex);
		}
	}

	private static boolean loadPuzzleCache() throws IOException {
		final ArrayList<String> lines = IO.slurp(IO.PUZZLE_CACHE);
		final int n = lines.size();
		int loaded = 0; // a bitset of loaded Difficulty.ordinal()s
		String line;
		// for each difficulty
		for ( int i=0; i<SIZE; ++i ) {
			// if this line was read and it's atleast 81 characters
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
			// 83 is 81 + 2 for Windows NL.
			final StringBuilder sb = new StringBuilder(83 * SIZE);
			for ( int i=0; i<SIZE; ++i ) {
				if ( CACHE[i] == null )
					sb.append(NL);
				else
					sb.append(CACHE[i].toShortString()).append(NL);
			}
			IO.save(sb.toString(), IO.PUZZLE_CACHE);
		} catch ( IOException ex) {
			StdErr.whinge("savePuzzleCache failed", ex);
		}
	}

	private static PuzzleCache theInstance;
	static PuzzleCache getPuzzleCache() {
		if ( theInstance == null )
			theInstance = new PuzzleCache();
 		return theInstance;
	}

	// ---------------- instance stuff ----------------

	private PuzzleCache() {
		if ( needFactory ) { // static initaliser didn't fill the cache
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
	Grid generate(final Symmetry[] symetries, final Difficulty difficulty, final boolean isExact) {
		final int d = difficulty.ordinal();
		Grid puzzle = CACHE[d];
		if ( puzzle != null ) {
			// cache-hit: generate a replacement Sudoku in the background.
			CACHE[d] = null;
			produce(symetries, difficulty);
		} else {
			// cache-miss: so generate a new Sudoku in this thread.
			try {
				final String threadName = currentThread().getName();
				Log.teeln(threadName+": Generating "+difficulty+"...");
				final Generator gen = getGenerator();
				gen.setTroubleInParadise(false);
				puzzle = CACHE[d] = gen.actuallyGenerate(this, symetries, difficulty, isExact);
				// if the cache is not already full
				if ( !gotAllDifficulties() )
					// fill the cache in the background
					newFactoryThread();
			} catch ( HinterruptException | TuringException eaten ) {
				Run.setStopGenerate(false);
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
		if ( isGeneratorRunning() )
			return;
		// avert if the Producer hit a snag (an Exception)
		final Generator gen = getGenerator();
		if ( gen.getAndResetTroubleInParadise() )
			return;
		final GenerateDialog dialog = getGenerateDialog();
		dialog.doDone = false; // the factory doesn't reset the stop button
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
					  // the factory does NOT generate IDKFA's coz it takes too
					  // long, so only happens on user request. Sadly the Genie
					  // doesn't play nice with GUI findHints/analyse/etc, but
					  // it's fine for everything else.
					  && difficulty != Difficulty.IDKFA
					) {
						try {
							final String threadName = currentThread().getName();
							Log.teeln(threadName+": Generating "+difficulty+"...");
							CACHE[difficulty.ordinal()] = gen.actuallyGenerate(PuzzleCache.this, Symmetry.values(), difficulty, true);
						} catch (HinterruptException | TuringException eaten) {
							Run.setStopGenerate(false);
							break;
						}
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
	 * @param symetries Symmetry's selected by the user
	 * @param difficulty the target Difficulty
	 */
	private void produce(final Symmetry[] symetries, final Difficulty difficulty) {
		// determine the useful symmetries from the users selection
		final Symmetry[] mySyms = usefulSymmetries(difficulty, symetries);
		// generate a replacement puzzle in a new thread
		final Thread thread = new Thread(PRODUCE_THREAD_NAME) {
			@Override
			public void run() {
				// Wait a-quarter-second for GUIs analyse to start...
				try{Thread.sleep(250);}catch(InterruptedException eaten){}
				// then we synchronize so that my background generate starts
				// when the foreground analyse completes, to avoid two solves
				// running concurrently (tripping-over stateful static vars).
				synchronized (ANALYSE_LOCK) {
					try {
						generate(mySyms, difficulty, true);
					} catch (Throwable eaten) {
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
	//   it's still too slow (upto like 10 minutes) with that.
	// * Diabolical is too slow (2+ mins) with only 4-and-8-point Symmetries,
	//   so add Symmetry.SMALLS to generate a replacement puzzle in ~20 secs.
	private Symmetry[] usefulSymmetries(final Difficulty difficulty, final Symmetry[] selectedSymetries) {
		switch ( difficulty ) {
			case IDKFA: return Symmetry.NONE;
			case Diabolical: return addSmalls(selectedSymetries);
			default: return selectedSymetries;
		}
	}

	// addSmalls returns all the Symmetry.SMALLS plus the 4-and-8-point
	// selected symmetries. It's a way of ensuring that there's enough
	// small syms to produce a Diabolical puzzle quickly enough. It can
	// take 3+ minutes with only large (4-and-8-point) symmetries. That's
	// an unacceptable excess of analysing for one puzzle.
	private Symmetry[] addSmalls(final Symmetry[] selectedSymetries) {
		final List<Symmetry> list = new LinkedList<>();
		list.addAll(Arrays.asList(Symmetry.SMALLS));
		for ( Symmetry ss : selectedSymetries )
			if ( ss.getPoints(0, 0).length > 2 ) // ie 4 or 8
				list.add(ss);
		return list.toArray(new Symmetry[list.size()]);
	}

}
