/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test.knuth;

import diuf.sudoku.utils.Debug;
import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.test.knuth.RecursiveSolver.Timing;
import static diuf.sudoku.test.TestHelpers.*;
import static diuf.sudoku.test.knuth.RecursiveSolver.TIMINGS;
import static diuf.sudoku.utils.Frmt.NL;
import diuf.sudoku.utils.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * RecursiveSolverTester times test-runs of RecursiveSolver.solve
 * over a MagicTour (.mt) file.
 * <pre>
 * Set Grid.AUTOSOLVE = true (normally false for LogicalSolver reporting) then
 * In CMD
 * cd C:\Users\User\Documents\NetBeansProjects\DiufSudoku
 * java -Xms100m -Xmx1000m -jar dist\DiufSudoku.jar top1465.d5.mt RecursiveSolverTester.stdout.txt
 *   1465	      677,386	    992,370,784	       61,718	  16079	Took
 * 1465 puzzles in under a second is seriously serious fast!
 * </pre>
 * RecursiveSolverTester is all about solving Sudoku puzzles as quickly as
 * possible (as I'm able).
 *
 * @author Keith Corlett 2013
 */
public final class RecursiveSolverTester {

	/**
	 * The TOTALING Technique used here-in. This is an unnecessary complication
	 * that I've included because I buggered it up (summed the running total)
	 * causing an "O-s__t" moment, so I make lemonade. I hope that other folks
	 * can learn from my mistake. Search usages of Totaling.
	 * <p>
	 * Computer: Allows people to make mistakes faster than any invention in
	 * history, with the exception of tequila plus hand-guns.
	 */
	private static enum Totaling {
		  /** use this if you want a separate total for each puzzle. */
		  Puzzle
		  /** use this if you want a running-total <br>
		   * or (like me) just a total over all puzzles. */
		, Overall
	};
	/**
	 * TOTALING is a static compile-time constant, so only the code for the
	 * selected approach is ever built, so there's zero runtime overhead.
	 * <pre>
	 * Puzzle: a total for each puzzle (reset pre solve).
	 * Overall: a total overall, ie a running-total.
	 * </pre>
	 */
	private static final Totaling TOTALING = Totaling.Overall;

	private static int ttlGuesses=0, numSolved=0, numFailed=0;

	private static final String USAGE
="usage: java -jar DiufSudoku.jar inputfile [outputfile]\n"
+"\n"
+"where inputfile is the name of a .mt (MagicTour text format) file\n"
+"\n"
+"A .mt file contains a line of 81 characters per puzzle:\n"
+"* clues are digits 1..9\n"
+"* an empty cell is any other character, but I use .\n"
+"* an .mt file may (arbitrarily) contain upto 9999 puzzles.\n"
+"\n"
+"For example the first 2 lines of top1465.d5.mt are:\n"
+"7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5\n"
+"..1.......2..8.3.1..4.63......4...3.....95...9....16.2.6....4.7..57.....7....98..\n"
+"\n"
+"the optional outputfile parameter sends standard output to a buffered file,\n"
+"for speed. If this paramater is omitted then standard output is directed to\n"
+"the console (stdout) as per normal.\n"
+"\n"
;

	/**
	 * Solve all the Sudokus in the input file.
	 * @param args {@String[]} inputFilename
	 */
	public static void main(String[] args) {

		if ( args.length < 1 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		final String inputFilename = args[0];
		final File inputfile = new File(inputFilename);
		if ( !inputfile.exists() ) {
			System.err.println(Log.me()+": Input file does not exist: "+inputfile.getAbsolutePath());
			System.exit(2);
		}
		if ( args.length > 1 ) {
			try {
				Log.out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(args[1]))));
			} catch (FileNotFoundException ex) {
				System.err.println(Log.me()+": Buffered output file failed to open");
				System.err.flush();
				ex.printStackTrace(System.err);
				System.exit(3);
			}
		} // else Log.out is already set to System.out by default

		try {
			EnumMap<Tech, Timing> timings = new EnumMap<>(Tech.class);
			RecursiveSolver solver = new RecursiveSolver();

			if ( false ) { // solve 1 puzzle
				// I only do singles with asserts, and Debug
				try {
					assert false : "expected";
					throw new IllegalStateException("Assertions should be on");
				} catch (AssertionError expected) {
					// Do nothing, this is (unusually) the desired path
				}
				if(!Debug.IS_ON) throw new MoFoException("Debug should be on");
				// 33 top87.mt	 12,037,686,145	   93
				process(new Line(inputfile, 33), solver, timings);
				// theres a problem with AlignedPairExclusion early in 2/1465
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 2), solver, totalTimings);
				// 1411 top1465.mt took 92,477,815 with 178 guesses
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 1411), solver, totalTimings);
				// 983/1465 is one of the fastest, pretty reliably.
				//process(new Line(new File(DIRECTORY+"top1465.mt"), 983), solver, totalTimings);
				// 28/87 is just a nice quick puzzle
				//process(new Line(new File(DIRECTORY+"top87.mt"), 28), solver, totalTimings);
			} else { // solve a whole file
				assert false : "assertions should be off";

				// pre-read the file, before we start timing (a bit cheeky)
				// array-iteration is faster than an Iterator
				final Line[] lines;
				try  {
					final List<Line> list = slurp(inputfile);
					lines = list.toArray(new Line[list.size()]);
				} catch (IOException ex) {
					ex.printStackTrace(System.err);
					return;
				}

				// process each puzzle
				long start = System.nanoTime();
				for ( int i=0,n=lines.length; i<n; ++i )
					process(lines[i], solver, timings);
				long finish = System.nanoTime();
				long took = finish-start;

				// print the summary report
				if ( numFailed > 0 )
					Log.println("FAILURES "+numFailed);
				else { // performance stats
					Log.println("\nPerformance of Solving Techniques:");
					final String f = "%,11d\t%,10d\t%,15d\t%,10d\t%,12d\t%s%s";
					Log.format(      "%11s\t%10s\t%15s\t%10s\t%12s\t%s%s"
						, "calls", "ns/call", "nanoseconds"
						, "ns/elim", "elims", "Tech", NL);
					final Timing sum = new Timing();
					final Set<Map.Entry<Tech,Timing>> timingsToSum;
					if ( TOTALING == Totaling.Puzzle ) // total-for-each-puzzle
						timingsToSum = timings.entrySet();
					else // overall total only
						timingsToSum = RecursiveSolver.TIMINGS.entrySet();
					for ( Entry<Tech,Timing> e : timingsToSum )
						printAndSum(f, e.getKey(), e.getValue(), sum);
					Log.format(f, numSolved, div(sum.time,sum.calls), sum.time
						, div(sum.time,sum.elims), sum.elims, "Total", NL);
					Log.format(f, numSolved, div(took,numSolved), took
						, div(took,ttlGuesses), ttlGuesses, "Took", NL);
					Log.println();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(Log.out);
		} finally {
			if ( Log.out != System.out ) {
				Log.flush();
				Log.close();
			}
		}
	}

	// I was methodised coz I was the contents of two loops (that're now one),
	// and I retained the method coz I just like how e.getKey and e.getValue
	// work in the method call, and I abore them on two lines. I'm crazy!
	private static void printAndSum(String f, Tech tech, Timing t, Timing sum) {
		Log.format(f, t.calls, div(t.time,t.calls), t.time
		 , div(t.time,t.elims), t.elims, tech.name(), NL);
		sum.add(t);
	}


//	/** If true then process(...) writes stuff to Log.out */
//	private static final boolean IS_NOISY = false; // #check false

	private static boolean process(Line line, RecursiveSolver solver, EnumMap<Tech,Timing> timings) {
		boolean result = false;

		Grid grid = new Grid(line.contents);

		Log.format("%-5d\t", line.number);
		long start = System.nanoTime();
		try {
			// solve isNoisy is verbose, so use it only to debug.
			// reset the timings, coz they're totalised at end of puzzle;
			// else (Overall) skip this reset and total-for-each-puzzle.
			if ( TOTALING == Totaling.Puzzle )
				for ( Timing t : TIMINGS.values() )
					t.reset();
			result = solver.solve(grid, false); // solves or throws
			long took = System.nanoTime() - start;
			assert result;
			++numSolved;
			Log.format("%,11d\t", took);
			Log.format("%3d\t", RecursiveSolver.numGuesses);
			Log.format("%s", grid);
			Log.println();

			ttlGuesses += RecursiveSolver.numGuesses;
			// if we want a total-per-puzzle then we need to add-up the timings
			// for this puzzle, they where reset in RecursiveSolver.
			if ( TOTALING == Totaling.Puzzle )
				for ( Entry<Tech,Timing> e : RecursiveSolver.TIMINGS.entrySet() ) {
					Tech tech = e.getKey();
					Timing t = e.getValue();
					Timing ttl = timings.get(tech);
					if ( ttl == null )
						timings.put(tech, ttl = new Timing());
					ttl.add(t);
				}
			// else we only want Overall totals, and they're already accrued in TIMINGS
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
