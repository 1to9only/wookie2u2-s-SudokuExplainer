/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.gui.GenerateDialog;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.InterruptException;
import diuf.sudoku.solver.LogicalSolver;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.io.IOException;
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

	// cache a puzzle of each Difficulty level.
	private static final int NUM_DIFFICULTIES = Difficulty.values().length;

	private static final Grid[] PUZZLE_CACHE = new Grid[NUM_DIFFICULTIES];

	// NOTE: hardest to easiest because generating a hard one usually takes a
	// lot of attempts, so it tends to fill-in the easier ones along the way.
	private static final Difficulty[] DIFFICULTIES = Difficulty.reverseValues();

	// this is called by GenerateDialogs (static) class-init
	private static boolean needFactory = false;
	public static void staticInitialiser() {
		assert NUM_DIFFICULTIES == DIFFICULTIES.length; // invariant (no catch)
		try {
			if ( loadPuzzleCache() != (1<<NUM_DIFFICULTIES)-1 )
				needFactory = true; // fix it in instance-land
		} catch (IOException ex) {
			StdErr.whinge("staticInitialiser failed", ex);
		}
	}

	private static int loadPuzzleCache() throws IOException {
		final ArrayList<String> lines = IO.slurp(IO.PUZZLE_CACHE);
		final int n = lines.size();
		int loaded = 0; // a bitset of loaded Difficulty.ordinal()s
		String line;
		// for each difficulty
		for ( int i=0; i<NUM_DIFFICULTIES; ++i ) {
			// if this line was read and it's atleast 81 characters
			if ( i<n && (line=lines.get(i))!=null && line.length()>80 ) {
				// add i into the loaded bitset
				PUZZLE_CACHE[i] = new Grid(line);
				loaded |= 1<<i;
			} else {
				PUZZLE_CACHE[i] = null;
			}
		}
		return loaded;
	}

	private static void savePuzzleCache() {
		try {
			// 83 is 81 + 2 for EOL for Winblows.
			StringBuilder sb = new StringBuilder(83 * NUM_DIFFICULTIES);
			for ( int i=0; i<NUM_DIFFICULTIES; ++i ) {
				if ( PUZZLE_CACHE[i] == null )
					sb.append(NL);
				else
					sb.append(PUZZLE_CACHE[i].toShortString()).append(NL);
			}
			IO.save(sb.toString(), IO.PUZZLE_CACHE);
		} catch ( IOException ex) {
			StdErr.whinge("savePuzzleCache failed", ex);
		}
	}

	// ---------------- instance stuff ----------------

	PuzzleCache() {
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
	void set(Difficulty diff, Grid puzzle) {
		if ( diff!=null )
			PUZZLE_CACHE[diff.ordinal()] = puzzle;
	}

	// PuzzleCache.generate retrieves a puzzle from the cache and then replaces
	// it in the background; else (cacheMiss) I generate in-line and return it;
	// then generate all difficulties in a background thread.
	Grid generate(Symmetry[] syms, Difficulty difficulty, boolean isExact) {
		final int d = difficulty.ordinal();
		Grid puzzle = PUZZLE_CACHE[d];
		if ( puzzle != null ) {
			// cache-hit: generate a replacement Sudoku in the background.
			PUZZLE_CACHE[d] = null;
			produce(syms, difficulty);
		} else {
			// cache-miss: so generate a new Sudoku in this thread.
			try {
				Log.teeln(Thread.currentThread().getName()+": Generating "+difficulty+"...");
				PUZZLE_CACHE[d] = puzzle = Generator.getInstance().generate(
						this, syms, difficulty, isExact);
				// if the cache is not already full
				if ( !gotAllDifficulties() )
					// fill the cache in the background
					newFactoryThread();
			} catch ( InterruptException | TuringException eaten ) {
				// Do nothing
			}
		}
		return puzzle;
	}

	private boolean gotAllDifficulties() {
		for ( int i=0; i<NUM_DIFFICULTIES; ++i )
			if ( PUZZLE_CACHE[i] == null )
				return false;
		return true;
	}

	private void newFactoryThread() {
		// ONE factory at once!
		if ( Threads.exists(Generator.FACTORY_THREAD_NAME) ) {
			return;
		}
//		if ( IO.FACTORY_LOCK_DIR.exists() ) {
//			Log.teeln(Log.me()+": is already running.");
//			return;
//		}
//		IO.makeTempDir(IO.FACTORY_LOCK_DIR);
		final GenerateDialog gd = GenerateDialog.getInstance();
		gd.doFinished = false; // stop generate finishing itself till all done
		Thread thread = new Thread(Generator.FACTORY_THREAD_NAME) {
			@Override
			public void run() {
				// A very small percentage of puzzles are hard, so finding a
				// hard one almost always fills all the easier level, hence
				// DIFFICULTIES is sorted hardest-to-easiest, so PUZZLE_CACHE
				// index is the bloody reverse of DIFFICULTIES index!
				gd.setGeneratorThread(this);
				for ( Difficulty difficulty : DIFFICULTIES ) { // hardest first
					if ( PUZZLE_CACHE[difficulty.ordinal()] == null ) {
						try {
							Log.teeln(Thread.currentThread().getName()+": Generating "+difficulty+"...");
							final Generator g = Generator.getInstance();
							PUZZLE_CACHE[difficulty.ordinal()] =
								g.generate(PuzzleCache.this, Symmetry.values()
										, difficulty, true);
						} catch ( InterruptException | TuringException eaten ) {
							break;
						}
					}
				}
				savePuzzleCache();
				gd.doFinished = true;
//				IO.delete(IO.FACTORY_LOCK_DIR);
//				gd.stopGeneratorThread();
			}
		};
		thread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(thread);
		thread.start();
	}

	/**
	 * produce runs after a cache-hit, to refill the cache by generating a
	 * puzzle to replace the one we just "vended". The self-refiller in a
	 * self-refilling vending machine.
	 *
	 * @param syms Symmetry's selected by the user
	 * @param difficulty the target Difficulty
	 */
	private void produce(final Symmetry[] syms, final Difficulty difficulty) {
		// determine the useful symmetries from the users selection
		final Symmetry[] mySyms = usefulSymmetries(difficulty, syms);
		// now generate a replacement puzzle
		final Thread generatorThread = new Thread(Generator.PRODUCE_THREAD_NAME) {
			@Override
			public void run() {
				// Wait a-quarter-second for GUIs analyse to start...
				try{Thread.sleep(250);}catch(InterruptedException eaten){}
				// then we synchronize so that my background generate starts
				// when the foreground analyse completes, so two LogicalSolvers
				// don't run concurrently and trip over stateful static vars
				// like GrabBag. GrabBag.ANALYSE_LOCK is aggregiously ironic.
				synchronized (LogicalSolver.ANALYSE_LOCK) {
					try {
						generate(mySyms, difficulty, true);
					} catch (Throwable eaten) {
						// Do nothing ~ no "standard" stack trace to StdErr.
					}
					savePuzzleCache();
				}
			}
		};
		generatorThread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(generatorThread);
		generatorThread.start();
	}

	// Determine the useful symmetries to speed-up generation:
	// * IDKFA is never (AFAIK) produced with anything but NONE Symmetry, and
	//   it's still too slow (upto like 10 minutes) with that.
	// * Diabolical is too slow (2+ mins) with only 4-and-8-point Symmetries,
	//   so add Symmetry.SMALLS to generate a replacement puzzle in ~20 secs.
	private Symmetry[] usefulSymmetries(Difficulty d, Symmetry[] selectedSyms) {
		switch ( d ) {
			case IDKFA: return Symmetry.NONE;
			case Diabolical: return addSmalls(selectedSyms);
			default: return selectedSyms;
		}
	}

	// addSmalls returns all the Symmetry.SMALLS plus the 4-and-8-point
	// selected symmetries. It's a way of ensuring that there's enough
	// small syms to produce a Diabolical puzzle quickly enough. It can
	// take 3+ minutes with only large (4-and-8-point) symmetries. That's
	// an unacceptable excess of analysing for one puzzle.
	private Symmetry[] addSmalls(Symmetry[] syms) {
		List<Symmetry> list = new LinkedList<>();
		list.addAll(Arrays.asList(Symmetry.SMALLS));
		for ( Symmetry sym : syms )
			if ( sym.getPoints(0, 0).length > 2 ) // ie 4 or 8
				list.add(sym);
		return list.toArray(new Symmetry[list.size()]);
	}

}
