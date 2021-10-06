/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test;

import static diuf.sudoku.test.TestHelpers.*;

import diuf.sudoku.*;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.solver.Print;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.utils.*;
import static diuf.sudoku.utils.Frmt.COMMA;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Runs {@link LogicalSolver#solve} on each puzzle in a MagicTour file.
 * <p>
 * The current setup solves <tt>top1465.d5.mt</tt> in from 3 about minutes to
 * well over a day. It's totally dependant on which hinters you select in the
 * GUI using (Options Menu ~ Solving techniques).
 * <p>
 * Some hinters are faster than others. My chainers are pretty damn fast, and
 * reliably find hints.
 * <p>
 * Recommended Hinter Selection:<ul>
 * <li>All hinters should be selected by default, so just unselect stuff in
 * this list. Ergo: Select everything that's not mentioned here!
 * <li>The Naked Single and Hidden Single hinters are permanently selected coz
 *  it just doesn't make any sense to try to solve a Sudoku without them.
 * <li>Use Coloring OR BUG (Bivalue Universal Grave). Coloring is faster and
 *  finds a superset of BUG's hints, so I recommend Coloring; else BUG.
 * <li>There are no Naked Pent, or Hidden Pent in top1465 (they're degenerate).
 * <li>Mutant Swampfish produces no hints in top1465. sigh.<br>
 *  Mutant Swordfish is too slow (about 3 seconds per hint, IIRC).<br>
 *  Mutant Jellyfish is like watching the grass grow.
 * <li>Aligned Oct takes about 15 seconds per hint (IIRC) even hacked.
 * <li>Aligned Nona and Aligned Dec are both REALLY slow, so slow that they
 *  should probably be made unavailable to non-techies.
 * <li>Kraken Jellyfish are too slow. I don't routinely use ANY Krakens because
 *  I have an "intellectual objection" to combining Chaining with Fish, where
 *  Chaining alone is simpler and therefore faster. Just my humble opinion.
 * <li>Dynamic Plus fails to find hints ONLY in the very hardest puzzles, so
 *  it's permanently selected, as is Nested Plus, as a "catch all".
 * </ul>
 *
 * @author Keith Corlett (loosely based on Juillerat's original class).
 */
public final class LogicalSolverTester {

	private static final boolean IS_HACKY = THE_SETTINGS.getBoolean(Settings.isHacky);

	// KRC 2021-06-20 I'm trying to find my misplaced towel
	// When 1 puzzle is reprocessed print hint.toHtml to stdout (very verbose),
	// so that I can see all occurrences of misplaced towel in a puzzle.
	// WARNING: Search KrakenFisherman for Run.Type.Batch and comment-out if
	// you want Krakens. There may be other cases where it skips s__t in batch.
	private static final boolean PRINT_HINT_HTML = false; // #check false

	// KRC BUG 2020-08-20 888#top1465.d5.mt from hint.apply
	// DEBUG_SIAMESE_LOCKING_BUG=true makes LogicalSolverTester behave as per
	// the GUI to find a puzzle which trips over any mergeSiameseHints bug,
	// which previously wasn't bulk tested, only tripped-over in the GUI.
	// Also makes LogicalSolverTester follow (approximately) the same hint-path
	// through each puzzle as the GUI, which is quite handy coz no figuring out
	// where they diverge. Most useful EARLY in the hint-path, like Locking.
	// SLOWER: KRC 2021-06-28 disabled because it's slower!
	// FASTER: KRC 2021-07-12 new SiameseLocking class now it's 3 secs faster!
	// But the number of reported locking drops coz there's increases in both
	// NakedPairs and NakedTriples, as you'd expect because I donate my elims
	// as "additional" eliminations to the existing NakedWhatever hint, and it
	// takes about twice as long to run, but it's faster over-all; go figure!
	private static final boolean DEBUG_SIAMESE_LOCKING_BUGS = true; // @check true

	private static int numSolved=0, numFailed=0;

	private static int printUsage() {
		IO.cat(IO.LOGICAL_SOLVER_TESTER_USAGE, System.err);
		return 1;
	}

	private static int carp(String msg) {
		System.err.println(msg);
		return 1;
	}

	private static int carp(String msg, Throwable ex) {
		System.err.println(msg);
		System.err.flush();
		ex.printStackTrace(System.err);
		return 1;
	}

	private static int carp(String logger, Grid grid, Throwable ex, PrintStream err) {
		err.println(logger+" carp");
		err.println(grid.toString());
		err.flush();
		ex.printStackTrace(err);
		return 1;
	}

	/**
	 * Solve some Sudoku puzzles logically. Output to stdout and inputFile.log.
	 * @param args the name of the *.mt input file (atleast) see USAGE.
	 */
	public static void main(String[] args) {
		Run.type = Run.Type.Batch;
		try {
			run(args);
		} catch (Throwable t) {
			StdErr.whinge("run failed.", t);
		}
		beep(); beep(); beep();
	}

	@SuppressWarnings({"unchecked", "rawtypes"}) // just for the array of Set
	public static int run(String[] args) {

		int retval = 1; // presume failure
		long start = System.nanoTime();

		// ---------- read the command line arguments ----------

		if ( args.length < 1 ) // requires inputFileName atleast
			return printUsage();

		// get the inputFilename from the arguements
		final File inputFile = new File(args[0]);
		args = MyArrays.leftShift(args);
		String inputFilepath = inputFile.getAbsolutePath();
		if ( !inputFile.exists() )
			return carp("Input file not found: "+inputFilepath);
		// avert accidentally reading a .log file (has happened)
		// BEWARE: on *nix we're insisting on a lowercase .mt
		if ( !MyFile.nameEndsWith(inputFilepath, ".mt") )
			return carp("Expected *.mt as input: "+inputFilepath);

		//<HACK>
		// AHinter.hackTop1465: we're processing top1465.d5.mt in HACK mode.
		// Aligned*Exclusion uses AHinter.hackTop1465 to enable speed hacks.
		// LogicalSolver uses AHinter.hackTop1465 to set the firstHintNumbers.
		AHinter.hackTop1465 = IS_HACKY && MyFile.nameContains(inputFilepath, "top1465");
		//</HACK>

		// remaining args are either -wantedHinters or pids (puzzle numbers)
		final int[] pids;
		if ( args.length == 0 )
			pids = null;
		else if ( args[0].startsWith("-wantedHinters") ) {
			// "-wantedHinters:" MUST be the last arg, ie ALL following args
			// are the names of the wanted hinters, in a comma-space seperated
			// list... ie exactly as they appear in the log, so you can copy-
			// paste the whole damn line rather than piss-about with it.
			// WARN: NOT TESTED with any other args!
			// WARN: Settings changes are PERMANENT!
			THE_SETTINGS.setWantedTechs(parseWantedHinters(args));
			pids = null;
		} else {
			pids = Frmt.toIntArray(args); // may still be null
			// do BEFORE new LogicalSolver because new LogicalSolver ->
			// new SingleSolution -> new HintsApplicumulator ->
			// if AHint.printHtml then new SB, so hint.toString() contains
			// a list of the hints that have already been applied, the actual
			// HTML for which is above this entry in the log because it was
			// printed as each hint was applied, so this is just a summary.
			if ( PRINT_HINT_HTML ) {
				if ( pids!=null && pids.length==1 )
					AHint.printHintHtml = true; // this is REALLY verbose
			}
		}

		// get the output logFilename from the inputFilename
		// KRC 2019-10-03 put run date-time in logFilename.
		final String now = Settings.now(); // resused in the log-file itself
		// stick $now in the filename so it won't be overwritten.
		final String ext = "."+now+".log";
		// Do two search-and-replaces for MAD Windows users with .MT files.
		final String logFilename = inputFilepath.replaceFirst("\\.mt$", ext)
				.replaceFirst("\\.MT$", ext.toUpperCase());
		final File logFile = new File(logFilename);

		// --------------------------- begin real work ------------------------

		// So let's run this little fire trucker down the list...
		try ( PrintStream out = new PrintStream(new FileOutputStream(logFile), true) ) {
			Log.initialise(out);
			Print.initialise();

			// nb: LogicalSolver's want method (et al) write to Log.out
			solver = LogicalSolverFactory.get();

			// KRC BUG 2020-08-20 Find the mergeSiameseHints bug.
			// DEBUG_SIAMESE_LOCKING_BUGS makes LogicalSolverTester behave
			// as-per the GUI to find a puzzle which hits the siamese bug.
			if ( DEBUG_SIAMESE_LOCKING_BUGS ) {
				// also made mergeSiameseHints handle SingleHintsAccumulator
				// as well as it's intended [Default]HintsAccumulator.
				solver.setSiamese();
				if (Log.MODE >= Log.VERBOSE_5_MODE) {
					Log.teeln("logicalSolver.locking.setSiamese!");
				}
			}

			final boolean logHints = true;

			if ( pids!=null && pids.length>0 ) { // RETEST SPECIFIC PUZZLE/S
				UsageMap totalUsageMap = new UsageMap();
				for ( int pid : pids ) { // pids are 1 based
//					// -REDO dis/enable wantedHinters to what hinted last-time.
//					if ( redo )
//						logicalSolver.reconfigureToUse(hintyHinters[pid-1]);
					System.out.println("processing pid "+pid+" ...");
					final boolean logIt = pids.length==1;
					process(readALine(inputFile, pid), totalUsageMap, logIt, logIt); // ONE exception
					// print running-total usages (for A*E monitoring)
					printTotalUsageMap(totalUsageMap);
				}
				// report, close files, clean-up, etc.
				try { solver.close(); } catch (IOException impossible) { }
			} else { // WHOLE PUZZLE FILE

				// PRIMING SOLVE: don't time the bloody JIT compiler!
				if ( true ) {
					if (Log.MODE >= Log.VERBOSE_5_MODE) {
						System.out.println("<priming-solve>");
					}
					final Grid g = new Grid(readALine(inputFile, 1).contents);
					solver.prepare(g);
					// priming solve: manually coz process stuffs-up counts
					// nb: logTimes ALWAYS false for top1465 (too verbose)
					solver.solve(g, new UsageMap(), true, true, false, false);
					if (Log.MODE >= Log.VERBOSE_5_MODE) {
						System.out.println("</priming-solve>");
					}
				}

				// now the actual run
				printHeaders(now, inputFile, logFile);
				if (Log.MODE >= Log.VERBOSE_2_MODE) {
					Log.teeln(solver.getWantedHinterNames());
					Log.teeln(solver.getUnwantedHinterNames());
				}
				UsageMap totalUsageMap = new UsageMap();
				try ( BufferedReader reader = new BufferedReader(new FileReader(inputFile)) ) {
					int lineCount=0;  String contents;
					while ( (contents=reader.readLine()) != null ) {
						Line line = new Line(inputFile, ++lineCount, contents);
//						if(lineCount<9) continue;
						// totalUsageMap += process line with solver
						if ( !process(line, totalUsageMap, logHints, false) )
							break;
						// print running total usages (for A*E monitoring)
						printTotalUsageMap(totalUsageMap);
						if ( false ) {
							// I'm sick of GC making solve timings inconsistent,
							// but can't predict if it'll need to GC in the next
							// puzzle, so we full-GC after solving each puzzle.
							solver.cleanUp();
							contents = null;
							line = null;
							System.gc();
						}
						// just do the first however many
//						if(lineCount>=10) break; // 100 is standard profile size
						// just do the first one, and log its stats!
//						break;
//						Debug.breakpoint();
					} // next line in .mt file
				}

				long took = System.nanoTime() - start;
				if ( numFailed > 0 ) {
					Log.format("FAILURES "+numFailed+NL);
					return carp("FAILURES "+numFailed);
				}
				printRunSummary(took);

				// here we print hinters performance metrics/sizes/whatever
//				Log.teeln("AlignedExclusionHint.maxLen="+AlignedExclusionHint.maxLen);
//				Log.teeln("AChainingHint.maxLineLen="+AChainingHint.maxLineLen);

				// just leave me in (silent when unused). This reports the max
				// size of the Chainers HashSet's when you use FunkyAssSet2 in
				// place of FunkyAssSet.
				OverallSizes.THE.dump();

// Here we report static hinter variables.
//				Log.teef("\nLinkedMatrixCellSet.bitCountCnt = %,d\n", bitCountCnt);

				// report, close files, clean-up, etc.
				try { solver.close(); } catch (IOException impossible) { }

				System.out.flush(); // ____ing dildows
				Log.out.flush(); // ____ing dildows

			} // fi

			retval = 0; // it's all good

		} catch (Exception ex) {
			System.out.println(Log.me()+" exception");
			System.out.flush();
			ex.printStackTrace(System.out);
			System.out.flush();
		}
		return retval; // it's all good
	}

	private static int procCount = 0;
	private static long ttlTook = 0L;
	private static LogicalSolver solver;
	private static boolean process(final Line line, final UsageMap totalUsage
			, final boolean logHints, final boolean logTimes) {
		if ( logHints ) {
			++procCount;
			// this is the puzzle-header-line for when we're logging a line per hint
			if (Log.MODE >= Log.VERBOSE_2_MODE) {
				Log.println();
				Log.println(line.toString()); // $pid#$absolutePath\t$contents
			}
		}
		final Grid grid = new Grid(line.contents);
		grid.source = new PuzzleID(line.file, line.number);

		long took = 0L;
		long start = System.nanoTime();
		try {

			// prepare the preppers (I like them with dead horse. lols.)
			solver.prepare(grid);

			// solve the puzzle!
			final UsageMap usageMap = new UsageMap(); // Hinter usages
			boolean isSolved = solver.solve(grid, usageMap, true, false, logHints, logTimes);
			if ( logHints )
				ttlTook += took = System.nanoTime() - start;
			if ( !isSolved )
				throw new UnsolvableException(grid.invalidity);
			++numSolved;

			if ( logHints ) {
				// print the hinter detail lines in VERBOSE_MODE
				Usage u;
				double d, maxDifficulty=0.0D, ttlDifficulty=0.0D;
				IHinter hardestHinter=null;
				final Usage ttl = new Usage("totals", 0, 0, 0, 0L);
				if (Log.MODE >= Log.NORMAL_MODE) {
					if ( procCount == 1 ) // FIRST ONLY
						Log.tee(Print.PUZZLE_SUMMARY_HEADERS);
					else
						if (Log.MODE >= Log.VERBOSE_2_MODE) {
							// repeat coz hints mean you can't see previous
							Log.print(Print.PUZZLE_SUMMARY_HEADERS);
						}
				}
				for ( Entry<IHinter,Usage> e : usageMap.entrySet() ) {
					ttl.add(u=e.getValue());
					if ( u.elims > 0 ) {
						if ( (d=u.maxDifficulty) > maxDifficulty ) {
							maxDifficulty = d;
							hardestHinter = e.getKey();
						}
						ttlDifficulty += u.ttlDifficulty;
					}
					if (Log.MODE >= Log.VERBOSE_2_MODE) {
						printHintDetailsLine(Log.out, u.time
							, u.calls, u.hints, u.elims
							, maxDifficulty, ttlDifficulty
							, String.valueOf(e.getKey()));
					}
				}
				if (Log.MODE >= Log.NORMAL_MODE) {
					printPuzzleSummary(line, took, ttlTook/procCount
							, ttl.calls, ttl.hints, ttl.elims
							, maxDifficulty, ttlDifficulty
							, String.valueOf(hardestHinter));
				}
				Log.flush();

				if ( totalUsage != null )
					totalUsage.addonAll(usageMap);
			}

			return true;
		} catch (Exception ex) { // thrown by solver.solve
			if(took==0L) took = System.nanoTime() - start;
			++numFailed;

			//NB: we wait 50ms before printing to stderr so stdout goes first
			System.out.format(NL);

			Log.format(NL);
			Log.format("%5d", line.number);
			Log.format(" %s", line.file.getName());
			Log.format("\t%,15d", took);
			Log.format("\t%5d", 0);
			Log.format("\t%4.2f", 0.0);
			Log.format("\t%s", ex);
			Log.format(NL);
			carp(Log.me(), grid, ex, Log.out);

			if ( Log.out != System.out )
				carp(Log.me(), grid, ex, System.out);

			return false;
		}
	}

	private static void printHeaders(String now, File inputFile, File logFile) {
		// create solver here coz logs unwanted hinters in NORMAL_MODE
		Log.teeln();
		Log.teef("%s built %s ran %s%s", Build.ATV, Build.BUILT, now, NL);
		// + 1 line per Hint (header)
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			Log.teef("hacky   : %s%s", IS_HACKY?"HACKY ":"NOT_HACKY", NL);
			Log.teef("input   : %s%s", inputFile.getAbsolutePath(), NL);
			Log.teef("log %2d  : %s%s", Log.MODE, logFile.getAbsolutePath(), NL);
			Log.teef("stdout  : progress only%s", NL);
			Log.println();
			printHintDetailsHeaders(Log.out);
			if ( false )
				printAlignedExclusionHeaders(System.out);
		}
		Log.println();
	}

	private static void printHintDetailsHeaders(PrintStream out) {
		// left justified to differentiate it from the puzzleNumber in logFile,
		// so assuming that there's less than 10,000 puzzles in each file, and
		// that no puzzle can ever possibly require more than 9,999 hints:
		// search  ^\d+ +\t to find a hint detail line
		// search: ^ +\d+\t to find a puzzle summary line
		out.format("%-5s", "hid");			// hintCount (hint identifier)
		out.format("\t%15s", "time (ns)");	// took
		out.format("\t%2s", "ce");			// countFilledCells
		out.format("\t%4s", "mayb");		// countMaybes
		out.format("\t%3s", "eli");			// numElims
		out.format("\t%-31s", "hinter");
		out.format("\t%s", "hint");
		out.println();
	}

	private static void printHintDetailsLine(PrintStream out, long time, int calls
			, int hints, int elims, double maxDifficulty, double ttlDifficulty
			, String hinterName) {
		// hint-details-line
		// + 1 line per Hint
		out.format("%5s", EMPTY_STRING); // hid (hintNumber)
		out.format("\t%,17d", time);
		out.format("\t%,17d", div(time,calls));
		out.format("\t%5d", calls);
		out.format("\t%5d", hints);
		out.format("\t%5d", elims);
		out.format("\t%4.2f", maxDifficulty);
		out.format("\t%7.2f", ttlDifficulty);
		out.format("\t%s", hinterName);
		out.println();
	}

	public static void printAlignedExclusionHeaders(PrintStream out) {
		out.println();
		out.println("AnE: KEY:" // AnE means AlignedNumberExclusion
				+ " nc=numCandidates"
				+ " mc=maxCandidates"
				+ " <h=minNumCandidatesWithHint"
				+ " >h=maxNumCandidatesWithHint"
				+ " h=numHints"
		);
		out.format("AnE:");
		out.format(" nc");
		out.format(" mc");
		out.format(" <h"); // currently in A6E, A7E and A10E only (weird huh?)
		out.format(" >h");
		out.format(" h");
		out.format(" %13s", "totalTime");
		out.println();
	}

	private static void printHinterSummaryHeader(PrintStream out) {
		// totalUsageMap: + 1 line per Hinter (summary) HEADER
		out.format("%18s", "time (ns)");
		out.format("\t%7s\t%14s", "calls", "time/call");
		out.format("\t%7s\t%14s", "elims", "time/elim");
		out.format("\t%s%s", "hinter", NL);
	}

	private static void printHinterSummary(PrintStream out, Usage u) {
		out.format("%,18d", u.time);
		out.format("\t%7d\t%,14d", u.calls, div(u.time,u.calls));
		out.format("\t%7d\t%,14d", u.elims, div(u.time,u.elims));
		// nb: the hinterName field is just for this line. sigh.
		out.format("\t%s%s", u.hinterName, NL);
	}

	private static List<Usage> printTotalUsageMap(UsageMap totalUsageMap) {
		if (Log.MODE >= Log.VERBOSE_1_MODE) {
			Log.println();
			printHinterSummaryHeader(Log.out);
		}
		// totalUsageMap: + 1 line per Hinter (summary) DETAIL
		long ttlTime = 0L;
		final List<Usage> totalUsageList = totalUsageMap.toArrayList();
		for ( Usage u : totalUsageList ) {
			if (Log.MODE >= Log.VERBOSE_1_MODE) {
				printHinterSummary(Log.out, u);
			}
			ttlTime += u.time;
		}
		if (Log.MODE >= Log.VERBOSE_1_MODE) {
			Log.format("%,18d%s", ttlTime, NL);
		}
		return totalUsageList;
	}

	private static void printPuzzleSummary(Line line, long took, long average
			, int ttlCalls, int ttlHints, int ttlElims, double maxDifficulty
			, double ttlDifficulty, String hardestHinter) {
		// 1 line per puzzle (basics) tee to stdout & log
		Log.teef("%5d", line.number);
		Log.teef("\t%,17d", took);
		Log.teef("\t%,17d", average);
		Log.teef("\t%5d", ttlCalls);
		Log.teef("\t%5d", ttlHints);
		Log.teef("\t%5d", ttlElims);
		Log.teef("\t%4.2f", maxDifficulty); // upto 9.99, 10+ is disaligned!
		Log.teef("\t%7.2f", ttlDifficulty); // 100 hints * 9.99 = 999.00
		Log.teef("\t%-20s", hardestHinter); // longest "Direct Hidden Triple" is 20 chars
		Log.teef("\t%s", line.contents);
		Log.teeln();
	}

	private static void printRunSummary(long took) {
		Log.teeln();
		Log.teef("%5s", "pzls");
		Log.teef("\t%17s", "total (ns)");
		Log.teef("\t(mm:ss)");
		Log.teef("\t%17s", "each (ns)");
		Log.teeln();
		Log.teef("%5d", numSolved);
		Log.teef("\t%,17d", took);
		int secs = (int)(took / 1000000000);
		Log.teef("\t(%02d:%02d)", secs/60, secs%60);
		Log.teef("\t%,17d", div(took,numSolved));
		Log.teeln();
	}

	/**
	 * Parses the wantedHinter args into an {@code EnumSet<Tech>}.
	 * <p>
	 * Args is a comma-space separated list that's been through CMD's (Windows)
	 * parser which breaks it into a space-separated list, leaving a trailing
	 * comma on each element so we end up with the below mess. No wuckers: all
	 * we do is remove the trailing comma (if present) from each name.
	 * <p>
	 * I'm pretty sure that *nix shells do the same thing (by default).
	 * <p>
	 * Example args: {"-wantedHinters:", "NakedSingle,", "HiddenSingle,", ...}
	 * <p>
	 * NOTE: The example was generated by copying the wantedHinters line of a
	 * LogicalSolverTester .log file and pasting it here. I formatted as Java
	 * code manually. I think I got it all right.
	 *
	 * @param args a comma-space separated list of Tech.name()s of the hinters
	 * you want. See above explanation and example.
	 * @return the wantedHinter Tech's
	 */
	private static EnumSet<Tech> parseWantedHinters(String[] args) {
		final int n = args.length;
		if ( n < 2 ) // not including args[0]
			throw new IllegalArgumentException("Empty wantedHinters list!");
		// the result wantedTechs set
		EnumSet<Tech> techs = EnumSet.noneOf(Tech.class);
		String name, newName;
		Tech tech;
		// nb: args[0].startsWith("-wantedHinters") is not the name of a
		//     wanted hinter, so we skip it.
		for ( int i=1; i<n; ++i ) {
			name = args[i];
			// remove trailing comma, if any
			if ( name.endsWith(COMMA) )
				name = name.substring(0, name.length()-1);
			// fail on any unknown tech (check aliases first)
			if ( (tech=Tech.valueOf(name)) == null
			  // aliases: oldName=>newName for backwards name-compatibility
			  && ( (newName=aliases.get(name)) == null
				|| (tech=Tech.valueOf(newName)) == null ) )
				throw new IllegalArgumentException("Unknown Tech: "+name
						+(newName!=null?" newName=\""+newName+"\"":""));
			techs.add(tech);
		}
		return techs;
	}
	// aliases future proofing for backwards compatibility if techs are renamed
	private static final Map<String,String> aliases = new HashMap<>(16, 0.75F);
	static {
		// oldName => newName
		aliases.put("X-Wing", "Swampfish");
	}

}
