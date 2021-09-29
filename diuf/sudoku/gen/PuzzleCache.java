/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.LogicalSolver;
import static diuf.sudoku.utils.Frmt.NL;
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

	// NB: it's hardest to easiest because generating a hard one takes
	//     MANY puzzles, so it fills in the easier ones along the way.
	private static final Difficulty[] DIFFICULTIES = new Difficulty[] {
			  Difficulty.IDKFA		// 6
			, Difficulty.Diabolical	// 5
			, Difficulty.Nightmare	// 4
			, Difficulty.Fiendish	// 3
			, Difficulty.Hard		// 2
			, Difficulty.Medium		// 1
			, Difficulty.Easy		// 0
	};

	// this is called by GenerateDialogs (static) class-init
	private static boolean needToGenerate = false;
	public static void staticInitialiser() {
		try {
			ArrayList<String> lines = IO.slurp(IO.PUZZLE_CACHE);
			for ( int i=0,n=Math.min(lines.size(),NUM_DIFFICULTIES); i<n; ++i )
				PUZZLE_CACHE[i] = new Grid(lines.get(i));
			if ( lines.size() < NUM_DIFFICULTIES )
				needToGenerate = true;
		} catch ( IOException ex) {
			StdErr.whinge("staticInitialiser failed", ex);
		}
	}

	private static void savePuzzleCache() {
		try {
			// 83 is 81 + 2 for EOL for Winblows.
			StringBuilder sb = new StringBuilder(83 * NUM_DIFFICULTIES);
			for ( int i=0; i<NUM_DIFFICULTIES; ++i ) {
				if ( PUZZLE_CACHE[i] == null )
					return; // No save if there's an empty slot
				sb.append(PUZZLE_CACHE[i].toShortString()).append(NL);
			}
			IO.save(sb.toString(), IO.PUZZLE_CACHE);
		} catch ( IOException ex) {
			StdErr.whinge("savePuzzleCache failed", ex);
		}
	}

	// ---------------- instance stuff ----------------

	private final Generator generator;

	PuzzleCache(Generator myOwnerGenerator) {
		this.generator = myOwnerGenerator;
		assert DIFFICULTIES.length == NUM_DIFFICULTIES;
		if ( needToGenerate )
			produceAll();
		needToGenerate = false;
	}

	void set(Difficulty diff, Grid puzzle) {
		PUZZLE_CACHE[diff.index] = puzzle;
	}

	// PuzzleCache.generate retrieves a puzzle from the cache and then replaces
	// it in the background; else (cacheMiss) I generate in-line and return it;
	// then generate all difficulties in a background thread.
	Grid generate(Symmetry[] syms, Difficulty d, boolean isExact) {
		Grid puzzle = PUZZLE_CACHE[d.index];
		if ( puzzle != null ) {
			// cache-hit: generate a replacement Sudoku in the background.
			PUZZLE_CACHE[d.index] = null;
			produce(syms, d);
		} else {
			// cache-miss: so generate a new Sudoku in this thread.
			puzzle = generator.generate(this, syms, d, isExact);
			// ensure the cache is full
			if ( !gotAllDifficulties() )
				produceAll(); // complete the cache in the background
		}
		return puzzle;
	}

	private boolean gotAllDifficulties() {
		for ( int i=0; i<NUM_DIFFICULTIES; ++i )
			if ( PUZZLE_CACHE[i] == null )
				return false;
		return true;
	}

	private void produceAll() {
		Thread thread = new Thread("GenerateAllPuzzles") {
			@Override
			public void run() {
				// A very small percentage of puzzles are IDKFA, so finding an
				// IDKFA is almost guaranteed to fill ALL of the easier levels,
				// so DIFFICULTIES is sorted hardest-to-easiest (IDKFA first).
				for ( Difficulty d : DIFFICULTIES )
					if ( PUZZLE_CACHE[d.index] == null )
						generate(Symmetry.ALL, d, true);
				savePuzzleCache();
			}
		};
		thread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(thread);
		thread.start();
	}

	/**
	 * produce runs after a cache-hit, to refill the cache by generating a
	 * puzzle to replace the one we just "vended"... I'm the self-refiller
	 * in a self-refilling vending machine.
	 *
	 * @param selectedSyms
	 * @param d
	 */
	private void produce(final Symmetry[] selectedSyms, final Difficulty d) {
		// determine the useful symmetries from the users selection
		final Symmetry[] syms = usefulSymmetries(d, selectedSyms);
		// now generate a replacement puzzle
		final Thread generatorThread = new Thread("GeneratePuzzle") {
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
						generate(syms, d, true);
					} catch (Throwable eaten) {
						// Do nothing ~ no "standard" stack trace to StdErr.
					}
				}
				savePuzzleCache();
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
