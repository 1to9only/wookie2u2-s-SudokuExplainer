/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test;

import diuf.sudoku.utils.Debug;
import diuf.sudoku.Grid;
import diuf.sudoku.solver.RecursiveSolver;
import diuf.sudoku.solver.RecursiveSolver.Timing;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.utils.Log;
import static diuf.sudoku.test.TestHelpers.*;
import java.io.File;
import java.util.LinkedHashMap;


/**
 * Times the test-runs of RecursiveSolver.solve over a MagicTour file.
 * <pre>
 * In CMD
 * cd C:\Users\User\Documents\NetBeansProjects\DiufSudoku
 * java -Xms100m -Xmx1000m -jar dist\DiufSudoku.jar top1465.d5.mt >RecursiveSolverTester.stdout.log
 *   1465	      677,386	    992,370,784	       61,718	  16079	Took
 * Just under 1 second for 1465 puzzles is seriously fast!
 * </pre>
 *
 * @author Keith Corlett 2013 IIRC
 */
public final class RecursiveSolverTester {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	private static int ttlGuesses=0, numSolved=0, numFailed=0;

	private static final String USAGE = "usage: DiufSudoku.jar"
			+ " inputfile which is a .mt (for Magic Tour) file";

	/**
	 * Solve all the Sudokus in the input file.
	 * @param args {@String[]} inputFilename
	 */
	public static void main(String[] args) {
		if ( args.length!=1 ) {
			System.err.println(USAGE);
			return;
		}
		final String inputFilename = args[0];
		final File inputfile = new File(inputFilename);
		if ( !inputfile.exists() ) {
			System.err.println("File not found: "+inputfile.getAbsolutePath());
			return;
		}

		Log.out = System.out;
		try {
			LinkedHashMap<String, Timing> totalTimings = new LinkedHashMap<>(8, 1F);
			RecursiveSolver solver = new RecursiveSolver(); // clears RecursiveSolver.timings

			long start = System.nanoTime();
			if ( false ) {	// solve a puzzle
				try {
					assert false : "expected";
					throw new IllegalStateException("Assertions should be on");
				} catch (AssertionError expected) { }
				if ( !Debug.IS_ON ) throw new MoFoException("Debug should be on");
				// 33 top87.mt	 12,037,686,145	   93
				process(new Line(inputfile, 33), solver, totalTimings);
				// theres a problem with AlignedPairExclusion early in 2/1465
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 2), solver, totalTimings);
				// 1411 top1465.mt took 92,477,815 with 178 guesses
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 1411), solver, totalTimings);
				// 983/1465 is one of the fastest, pretty reliably.
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 983), solver, totalTimings);
				// 28/87 is just a nice quick puzzle
				//process(new Line(new File(DIRECTORY+"top87.mt"), 28), solver, totalTimings);
			} else {		// solve a file
				assert false : "assertions should be off";
				for ( Line line : slurp(inputfile) )
					process(line, solver, totalTimings);
			}
			long finish = System.nanoTime();
			long took = finish-start;

			if (numFailed>0)
				Log.println("FAILURES "+numFailed);
			else {
				Log.println("Performance of Solving Techniques:");
				Log.format("%7s\t%13s\t%15s\t%13s\t%7s\t%s%s"
					, "calls", "ns/call", "nanoseconds", "ns/elim", "elims"
					, "Tech", NL);
				long ttlNanos = 0L;
				for ( String name : totalTimings.keySet() ) {
					Timing t = totalTimings.get(name);
					Log.format("%7d\t%,13d\t%,15d\t%,13d\t%7d\t%s%s"
					 , t.numCalls, div(t.nsTime,t.numCalls), t.nsTime
					 , div(t.nsTime,t.numElims), t.numElims, name, NL);
					ttlNanos += t.nsTime;
				}
				Log.format("%7d\t%,13d\t%,15d\t%,13d\t%7d\t%s%s"
					, numSolved, div(ttlNanos,numSolved), ttlNanos
					, div(ttlNanos,ttlGuesses), ttlGuesses, "Total", NL);
				long ttlOther = took - ttlNanos;
				Log.format("%7d\t%,13d\t%,15d\t%,13d\t%7d\t%s%s"
					, numSolved, div(ttlOther,numSolved), ttlOther
					, div(ttlOther,ttlGuesses), ttlGuesses, "Other", NL);
				Log.format("%7s\t%,13d\t%,15d\t%,13d\t%7d\t%s%s"
					, numSolved, div(took,numSolved), took
					, div(took,ttlGuesses), ttlGuesses, "Took", NL);
				Log.println();
			}
		} catch (Exception ex) {
			ex.printStackTrace(Log.out);
		}
	}

	/** If true then process(...) writes stuff to Log.out */
	private static final boolean IS_NOISY = false; // @check false

	private static boolean process(Line line, RecursiveSolver solver
			, LinkedHashMap<String,Timing> totalTimings) {
		boolean result = false;

		Grid grid = new Grid(line.contents);
		RecursiveSolver.TIMINGS.clear();

		if ( IS_NOISY ) {
			Log.println(line.number+"#"+line.file.getAbsolutePath());
			Log.println(grid);
			Log.println();
		}

		long start = System.nanoTime();
		try {
			result = solver.solve(grid); // either solves or throws an exception
			long took = System.nanoTime() - start;
			assert result;
			++numSolved;
			Log.format("  %5d %s\t%,15d\t%5d"
					, line.number, line.file.getName(), took
					, RecursiveSolver.numGuesses);
			if(IS_NOISY){Log.print('\t');Log.print(grid);}
			Log.print(NL);

			ttlGuesses += RecursiveSolver.numGuesses;
			long ttlNanos = 0L;
			for ( String name : RecursiveSolver.TIMINGS.keySet() ) {
				Timing t = RecursiveSolver.TIMINGS.get(name);
				if ( IS_NOISY )
					Log.format("%5d\t%,15d\t%5d\t%s%s", t.numCalls
							, t.nsTime, t.numElims, name, NL);
				Timing ttl = totalTimings.get(name);
				if ( ttl == null )
					totalTimings.put(name, ttl = new Timing());
				ttl.numCalls += t.numCalls;
				ttl.nsTime += t.nsTime;
				ttl.numElims += t.numElims;
				ttlNanos += t.nsTime;
			}
			if ( IS_NOISY ) {
				Log.format("%5s\t%,15d%s", "rules", ttlNanos, NL);
				Log.format("%5s\t%,15d%s", "other", took - ttlNanos, NL);
				Log.format("%5s\t%,15d%s", "total", took, NL);
				Log.format("%s", NL);
			}
		} catch (UnsolvableException ex) { // thrown by solver.solve
			++numFailed;
			long took = System.nanoTime() - start;
			Log.format("F %5d %s\t%,15d\t%5d\t%s%s"
					, line.number, line.file.getName(), took
					, RecursiveSolver.numGuesses, ex, NL);
			Log.format("%s", NL);
			Log.format("%s%s", grid, NL);
			Log.format("%s", NL);
			ex.printStackTrace(Log.out);
			Log.flush();
		}
		return result;
	}
}