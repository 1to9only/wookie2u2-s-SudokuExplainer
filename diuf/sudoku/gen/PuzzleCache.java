/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

import diuf.sudoku.Difficulty;
import diuf.sudoku.Grid;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.GrabBag;
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

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

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
			StringBuilder sb = new StringBuilder();
			for ( int i=0; i<NUM_DIFFICULTIES; ++i ) {
				if ( PUZZLE_CACHE[i] == null )
					return;
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

	// the PuzzleCache's generate retrieves a puzzle from the cache and then
	// replaces it in the background; else (cache-miss) I generate in-line and
	// return it; then also generate all difficulties in a background thread.
	Grid generate(Symmetry[] syms, Difficulty difficulty, boolean isExact) {
		Grid puzzle = PUZZLE_CACHE[difficulty.index];
		if ( puzzle != null ) { // cache-hit: subsequent calls come here
			PUZZLE_CACHE[difficulty.index] = null;
			produce(syms, difficulty); // replace him in the background
		} else { // cache-miss: probably the first "priming" read
			puzzle = generator.generate(this, syms, difficulty, isExact);
			// produceAll() attempts to fill the cache
			if ( !gotAllDifficulties() )
				produceAll(); // complete the cache in the background
		}
		return puzzle;
	}

	void set(Difficulty diff, Grid puzzle) {
		PUZZLE_CACHE[diff.index] = puzzle;
	}

	private void produceAll() {
		Thread thread = new Thread("GenerateAllPuzzles") {
			@Override
			public void run() {
				// Note that PUZZLE_CACHE recieves all generated puzzles, not
				// just the last one, with the desired degree of Difficulty.
				// A low percentage of puzzles are difficult, so just filling
				// IDKFA will (usually) also fill ALL the lesser levels.
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
	 * @param selectedSyms
	 * @param d 
	 */
	private void produce(final Symmetry[] selectedSyms, final Difficulty d) {
		// Too speed generation up:
		// * IDFKA is (apparently) only possible with NO symmetry. All I can
		//   say is I've never seen an IDKFA produced with any Symmetry.
		// * Diabolical too slow (3+ mins) with only 4-and-8-point Symmetries,
		//   so we add Symmetry.SMALLS to the useful symmetries, to generate a
		//   replacement puzzle relatively quickly (20 secs vs 2 min)
		final Symmetry[] usefulSyms =
				  d==Difficulty.IDKFA ? Symmetry.NONE
				: d==Difficulty.Diabolical ? addSmalls(selectedSyms)
				: selectedSyms;
		// now generate a replacement puzzle
		Thread thread = new Thread("GeneratePuzzle") {
			@Override
			public void run() {
				// generate a replacement puzzle. But first we wait a quarter
				// of a second for the GUIs analyse to start/run, then we...
				try{Thread.sleep(250);}catch(InterruptedException eaten){}
				// then we synchronize so that my background generate starts 
				// as soon as the foreground analyse completes, so that two
				// LogicalSolvers aren't solving concurrently, which trips over
				// stateful static vars like the GrabBag. Please note that the
				// use of GrabBag.ANALYSE_LOCK is pure unadulterated irony!
				synchronized ( GrabBag.ANALYSE_LOCK ) {
					try {
						generate(usefulSyms, d, true);
					} catch (Throwable eaten) {
						// Do nothing ~ no "standard" stack trace to StdErr.
					}
				}
				savePuzzleCache();
			}
		};
		thread.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(thread);
		thread.start();
	}

	private boolean gotAllDifficulties() {
		final Grid[] cache = PUZZLE_CACHE;
		for ( int i=0,n=NUM_DIFFICULTIES; i<n; ++i )
			if ( cache[i] == null )
				return false;
		return true;
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
