/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test;

import static diuf.sudoku.test.TestHelpers.*;

import diuf.sudoku.*;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.*;
import diuf.sudoku.solver.LogicalSolver.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.utils.*;
import java.io.*;
import java.util.*;


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

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	private static final boolean IS_HACKY = Settings.THE.get(Settings.isHacky);

	// KRC BUG 2020-08-20 888#top1465.d5.mt from hint.apply
	// DEBUG_SIAMESE_LOCKING_BUG=true makes LogicalSolverTester behave as per
	// the GUI to find a puzzle which trips over any mergeSiameseHints bug,
	// which previously wasn't bulk tested, only tripped-over in the GUI.
	// Also makes LogicalSolverTester follow (approximately) the same hint-path
	// through each puzzle as the GUI, which is quite handy coz no figuring out
	// where they diverge. Most useful EARLY in the hint-path, like Locking.
	private static final boolean DEBUG_SIAMESE_LOCKING_BUGS = true;

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
		ex.printStackTrace(System.err);
		return 1;
	}

	private static int carp(Grid grid, Throwable ex, PrintStream err) {
		err.println(grid.toString());
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

		// parse the switch arguements, if any
		Mode solverMode = Mode.ACCURACY; // ie the "normal" default mode
//		boolean stats=false;
		boolean redo=false; // "enhanced" modes
		Set<String>[] hintyHinters = null; // cheeky monkey.
		ARGS_LOOP: while ( args[0].charAt(0) == '-' ) { // evaludate the switch
			switch ( args[0] ) {
			case "-SPEED":
				solverMode = Mode.SPEED;
				break;
//			case "-STATS":
//				stats = true;
//				break;
//			//<HACK> // currently top1465.d5.mt only
//			case "-REDO":
//				try {
//					redo = true;
//					hintyHinters = new Set[1465]; // unchecked
//					if ( !parse(IO.KEEPER_LOG, hintyHinters) )
//						return carp("Failed parse: " + IO.KEEPER_LOG);
//				} catch (Exception ex) {
//					return carp("-REDO Exception:", ex);
//				}
//				break;
//			//</HACK>
			default:
				printUsage();
				return carp("unrecognised switch: \""+args[0]+"\"");
			}
			// eat this arg
			args = MyArrays.leftShift(args);
		}

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
		// AHinter.hackTop1465 means we're processing top1465.d5.mt in HACK mode.
		// Aligned*Exclusion uses AHinter.hackTop1465 to enable speed hacks.
		// LogicalSolver uses AHinter.hackTop1465 to set the firstHintNumbers.
		AHinter.hackTop1465 = IS_HACKY && MyFile.nameContains(inputFilepath, "top1465");
//		if ( redo && !AHinter.hackTop1465 )
//			if ( !IS_HACKY )
//				return carp("-REDO only works when isHacky setting is true");
//			else // must be !fileNameContains
//				return carp("-REDO only works on top1565, not: "+inputFilepath);
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
			Settings.THE.justSetWantedTechniques(parseWantedHinters(args));
			pids = null;
		} else
			pids = Frmt.toIntArray(args); // may be null

		// get the output logFilename from the inputFilename
		// KRC 2019-10-03 put run date-time in logFilename.
		final String now = Settings.now(); // resused in the log-file itself
		String ext = redo // ie LogicalSolver.usesJustHintyHinters
			? ".log" // it's not a keeper
			// it could be a keeper, so stick $now in the filename so it won't
			// be automatically overwritten if you just press play again next
			// time without thinking: "I need to store that logFile first".
			// It never happened to me, coz I'm intelligent. Sigh. But atleast
			// I've invented a permanent solution (sort-of) to the problem, so
			// maybe I'm not as dumb as I look. How dumb do you look Rodney?
			: "."+now+".log";
		// Do two search-and-replaces for MAD Windows users with .MT files.
		final String logFilename = inputFilepath.replaceFirst("\\.mt$", ext)
				.replaceFirst("\\.MT$", ext.toUpperCase());
		final File logFile = new File(logFilename);

		// --------------------------- begin real work ------------------------

		// Let's run this little fire trucker down the list...
		try ( PrintStream out = new PrintStream(new FileOutputStream(logFile), true) ) {
			Log.initialise(out);

			// nb: LogicalSolver's want method (et al) write to Log.out
			LogicalSolver solver = LogicalSolverFactory.get(solverMode);
			Log.tee("wantedHinters: ");
			Log.teeln(solver.getWantedHinterNames());

			// KRC BUG 2020-08-20 Find the mergeSiameseHints bug. Switch on
			// DEBUG_SIAMESE_LOCKING_BUGS to make LogicalSolverTester behave as
			// per the GUI to find the puzzle which hits a siamese bug.
			if ( DEBUG_SIAMESE_LOCKING_BUGS ) {
				// also made mergeSiameseHints handle SingleHintsAccumulator
				// as well as it's intended [Default]HintsAccumulator.
				solver.locking.setSiamese(solver.hiddenPair, solver.hiddenTriple);
				Log.teeln("logicalSolver.locking.setSiamese!");
			}

			if ( pids!=null && pids.length>0 ) { // RETEST SPECIFIC PUZZLE/S
				UsageMap totalUsageMap = new UsageMap();
				for ( int pid : pids ) { // pids are 1 based
//					// -REDO dis/enable wantedHinters to what hinted last-time.
//					if ( redo )
//						logicalSolver.reconfigureToUse(hintyHinters[pid-1]);
					System.out.println("processing pid "+pid+" ...");
					process(readALine(inputFile, pid), solver, totalUsageMap, redo, true, false, true);
					// print running-total usages (for A*E monitoring)
					printTotalUsageMap(totalUsageMap);
				}
				solver.report();
			} else { // WHOLE PUZZLE FILE

				// PRIMING SOLVE: don't time the JIT compiler!
				if ( true ) {
					// run solve manually (process buggers-up count)
					Grid g = new Grid(readALine(inputFile, 1).contents);
					solver.prepare(g);
					solver.solve(g, new UsageMap(), true, true, false);
				}

				// now the actual run
				printHeaders(now, solverMode, redo, inputFile, logFile);
				UsageMap totalUsageMap = new UsageMap();
				try ( BufferedReader reader = new BufferedReader(new FileReader(inputFile)) ) {
					int lineCount=0;  String contents;
					while ( (contents=reader.readLine()) != null ) {
						Line line = new Line(inputFile, ++lineCount, contents);
//						if(lineCount<9) continue;
						if ( redo ) // ie LogicalSolver.usesJustHintyHinters
							solver.reconfigureToUse(hintyHinters[line.index]);
						// totalUsageMap += process line with solver
						if ( !process(line, solver, totalUsageMap, redo, true, false, true) )
							break;
						// print running total usages (for A*E monitoring)
						printTotalUsageMap(totalUsageMap);
						if ( false ) {
							// I'm sick of GC making solve timings inconsistent,
							// but can't predict if it'll need to GC in the next
							// puzzle, so we full-GC after solving each puzzle.
							solver.cleanUp();
							if ( redo ) // ie solver.usesJustHintyHinters
								hintyHinters[line.index] = null;
							contents = null;
							line = null;
							System.gc();
						}
						// just do the first however many
//						if(lineCount>=10) break; // 100 is standard profile size
						// just do the first one, and log its stats!
//						break;
					} // next line in .mt file
				}

//				// save the hinter performance stats, if they're being used.
//				// NOTE: Prints a second usage map at end of run. So be it.
//				List<Usage> totalUsageList = printTotalUsageMap(totalUsageMap);
//				if ( stats ) {
//					totalUsageList.sort(Usage.BY_NS_PER_ELIM_ASC);
//					try ( PrintStream sout = Log.openStats() ) {
//						sout.print("//");
//						printHinterSummaryHeader(sout);
//						for ( Usage u : totalUsageList )
//							printHinterSummary(sout, u);
//					}
//				}

				long took = System.nanoTime() - start;
				if ( numFailed > 0 ) {
					Log.format("FAILURES "+numFailed+NL);
					return carp("FAILURES "+numFailed);
				}
				printRunSummary(took);

				// this is where we print "performance metrics" from static imported hinters

				// just leave me in (silent when unused). This reports the max
				// size of the Chainers HashSet's when you use FunkyAssSet2 in
				// place of FunkyAssSet.
				OverallSizes.THE.dump();

				// nb: A*E's are the only IReporter's to date.
				solver.report();

// Here we report static hinter variables.
//				Log.teef("\nLinkedMatrixCellSet.bitCountCnt = %,d\n", bitCountCnt);

				// save stats, close logs, etc.
				try { solver.close(); } catch (Throwable eaten) { }

				System.out.flush(); // ____ing dildows
				Log.out.flush(); // ____ing dildows

			} // fi

			retval = 0; // it's all good

		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			System.out.flush();
		}
		return retval; // it's all good
	}

	private static int procCount = 0;
	private static long ttlTook = 0L;
	private static boolean process(Line line, LogicalSolver solver
			, UsageMap totalUsage, boolean redo, boolean isNoisy
			, boolean showHints, boolean logHints
	) {
		if ( isNoisy ) {
			++procCount;
			// this is the puzzle-header-line for when we're logging a line per hint
			if (Log.MODE >= Log.VERBOSE_2_MODE) {
				Log.println();
				Log.println(line.toString()); // $pid#$absolutePath\t$contents
				// wantedEnabledHinters: belong directly below puzzle-header-line
				if ( redo )
					solver.dumpWantedEnabledHinters(Log.out);
			}
		}
		Grid grid = new Grid(line.contents);
		grid.source = new PuzzleID(line.file, line.number);
		GrabBag.grid = grid;

		long took = 0L;
		long start = System.nanoTime();
		try {

			// prepare the preppers (I like them with dead horse. lols.)
			solver.prepare(grid);

			// solve the puzzle!
			UsageMap usageMap = new UsageMap(); // Hinter usages
			boolean isSolved = solver.solve(grid, usageMap, true, showHints, logHints);
			if ( isNoisy )
				ttlTook += took = System.nanoTime() - start;
			if ( !isSolved )
				throw new UnsolvableException(grid.invalidity);
			++numSolved;

			if ( isNoisy ) {
				// print the hinter detail lines in VERBOSE_MODE
				String hinterName, hardestHinterName="Ignoto";
				double difficulty, maxDifficulty=0.0D;
				int calls, ttlCalls=0;
				int hints, ttlHints=0;
				int elims, ttlElims=0;
				long time;
				Usage u;
				if (Log.MODE >= Log.NORMAL_MODE) {
					if ( procCount == 1 ) // FIRST ONLY
						Log.teeln(Log.PUZZLE_SUMMARY_HEADERS);
					else if (Log.MODE >= Log.VERBOSE_2_MODE)
						// repeat coz hints mean you can't see previous
						Log.println(Log.PUZZLE_SUMMARY_HEADERS);
				}
				for ( IHinter hinter : usageMap.keySet() ) {
					hinterName = hinter.toString();
					difficulty = hinter.getDifficulty();
					u = usageMap.get(hinter);
					ttlCalls += (calls = u.numCalls);
					ttlHints += (hints = u.numHints);
					ttlElims += (elims = u.numElims);
					time = u.time;
					if ( elims>0 && difficulty>maxDifficulty ) {
						maxDifficulty = difficulty;
						hardestHinterName = hinterName;
					}
					if (Log.MODE >= Log.VERBOSE_2_MODE)
						printHintDetailsLine(Log.out, time, calls, hints, elims
							, difficulty, hinterName);
				}
				if (Log.MODE >= Log.NORMAL_MODE)
					printPuzzleSummary(line, took, ttlTook/procCount
							, ttlCalls, ttlHints, ttlElims
							, maxDifficulty, hardestHinterName);
				Log.flush();

				if ( totalUsage != null )
					totalUsage.addonate(usageMap);
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
			carp(grid, ex, Log.out);

			if ( Log.out != System.out )
				carp(grid, ex, System.out);

			return false;
		}
	}

	private static void printHeaders(String now, Mode solverMode
//			, boolean stats
			, boolean redo, File inputFile, File logFile) {
		// create solver here coz logs unwanted hinters in NORMAL_MODE
		Log.teef("%s built %s ran %s%s", Settings.ATV, Settings.BUILT, now, NL);
		// log the mode(s)
		Log.teef("mode    : %s", solverMode); // is -SPEED mode
//		Log.teef(" %sSTATS", stats?"":"!"); // is -STATS run
//		Log.teef(" %sREDO", redo?"":"!");   // is -REDO run
		Log.teef(" %sHACKY", IS_HACKY?"":"!");
		Log.teeln();
		Log.teef("input   : %s%s", inputFile.getAbsolutePath(), NL);
		Log.teef("log %2d  : %s%s", Log.MODE, logFile.getAbsolutePath(), NL);
		Log.teef("stdout  : progress only%s", NL);
		// + 1 line per Hint (header)
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
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
			, int hints, int elims, double difficulty, String hinterName) {
		// hint-details-line
		// + 1 line per Hint
		out.format("%5s", ""); // hid (hintNumber)
		out.format("\t%,17d", time);
		out.format("\t%,17d", div(time,calls));
		out.format("\t%5d", calls);
		out.format("\t%5d", hints);
		out.format("\t%5d", elims);
		out.format("\t%4.2f", difficulty);
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
		out.format("\t%7d\t%,14d", u.numCalls, div(u.time,u.numCalls));
		out.format("\t%7d\t%,14d", u.numElims, div(u.time,u.numElims));
		out.format("\t%s%s", u.hinterName, NL);
	}

	private static List<Usage> printTotalUsageMap(UsageMap totalUsageMap) {
		Log.println();
		if (Log.MODE >= Log.VERBOSE_1_MODE)
			printHinterSummaryHeader(Log.out);
		// totalUsageMap: + 1 line per Hinter (summary) DETAIL
		long ttlTime = 0L;
		List<Usage> totalUsageList = totalUsageMap.toArrayList();
		totalUsageList.sort(Usage.BY_NUM_CALLS_DESC);
		for ( Usage u : totalUsageList ) {
			if (Log.MODE >= Log.VERBOSE_1_MODE)
				printHinterSummary(Log.out, u);
			ttlTime += u.time;
		}
		Log.format("%,18d%s", ttlTime, NL);
		return totalUsageList;
	}

	private static void printPuzzleSummary(Line line, long took, long average
			, int ttlCalls, int ttlHints, int ttlElims, double maxDifficulty
			, String hardestHinterName) {
		// 1 line per puzzle (basics) tee to stdout & log
		Log.teef("%5d", line.number);
		Log.teef("\t%,17d", took);
		Log.teef("\t%,17d", average);
		Log.teef("\t%5d", ttlCalls);
		Log.teef("\t%5d", ttlHints);
		Log.teef("\t%5d", ttlElims);
		Log.teef("\t%4.2f", maxDifficulty);
		Log.teef("\t%-21s", hardestHinterName); // longest "Direct Hidden Triple" is 20 chars
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
 Example args: {"-wantedHinters:", "NakedSingle,", "HiddenSingle,"
 , "Locking,", "NakedPair,", "HiddenPair,", "NakedTriple,"
 , "HiddenTriple,", "Swampfish,", "Swordfish,", "XY_Wing,", "XYZ_Wing,"
 , "W_Wing,", "Skyscraper,", "TwoStringKite,", "EmptyRectangle,"
 , "NakedQuad,", "Jellyfish,", "URT,", "FinnedSwampfish,"
 , "FinnedSwordfish,", "FinnedJellyfish,", "SashimiSwampfish,"
 , "SashimiSwordfish,", "SashimiJellyfish,", "ALS_XZ,", "ALS_Wing,"
 , "ALS_Chain,", "AlignedPair,", "AlignedTriple,", "AlignedQuad,"
 , "AlignedPent,", "UnaryChain,", "NishioChain,", "MultipleChain,"
 , "DynamicChain,", "DynamicPlus,", "NestedUnary,", "NestedPlus"}
 <p>
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
			if ( name.endsWith(",") )
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

// -REDO is rooted by HoDoKu. The LogicalSolver we first touch it with MUST be
// the one that solves the puzzle, otherwise it's table entries go astray, and
// I'm not bright enough to work-out how to fix it; so given a choice between
// this piece of s__t and HoDoKu I'm taking HoDoKu.
//	/**
//	 * To setup for a -REDO run we parse the log-file into an array (indexed by
//	 * 0-based puzzleNumber) of a Set of hintyHinters nom's, which we then pass
//	 * to the LogicalSolver to reconfigure itself around.
//	 * @param inputLogFile File to read
//	 * @param hintyHinters output {@code Set<String>[]} an array of a set of
//	 * strings, that is indexed by 0-based puzzleNumber, so that we have a set
//	 * of strings (hinter nom's) per puzzle: each element of which startsWith
//	 * the tech.nom, and is optionally followed by a space-separated list of
//	 * hintNumbers at which this hinter is to be activated. If the hintNumbers
//	 * list is missing-or-empty then we set the hinters hintNumbers to null, so
//	 * that its activate method reverts to the firstHintNumber passed into it's
//	 * constructor.
//	 *
//	 * @return success
//	 */
//	private static boolean parse(File inputLogFile, Set<String>[] hintyHinters) {
//		final int NOM_BEGIN = 34; // nom is French for name, which is reserved by the Enum
//		final int NOM_END = NOM_BEGIN + 30; // length is 30
//		String line=null, puzzleNumberD5, nom;
//		boolean found;
//		// A Map of each wanted IHintNumberActivatableHinter
//		// hinter.tech.nom => Set<Integer> hintNumbers
//		Map<String,Set<Integer>> hintNumbersMap = new LinkedHashMap<>(NUM_NOMS, 0.75F);
//		// parse logFile into the hintyHinters array.
//		try ( SkipReader reader = new SkipReader(inputLogFile) ) {
//			for ( int pid=1; pid<=1465; ++pid ) { // 1 based puzzleNumber
//				puzzleNumberD5 = MyStrings.format("%5d", pid);
//				// re/create empty hintNumbers Sets for each puzzle
//				hintNumbersMap.clear();
//				for ( String nhNom : getNumberedHinterNoms() )
//					hintNumbersMap.put(nhNom, new LinkedHashSet<>(32, 0.75F));
//
//				// skip down to the puzzle-header-line
//				if ( !reader.skipDownToNextLineStartingWith(""+pid+"#") )
//					throw new IOException("Not found: "+pid+"#");
//				// now skip down to the hinter-summary-headers-line
//				//     	        time (ns)	     average (ns)	calls	hints	elims	diff	hinter
//				//     	         107,546	            1,707	   63	    0	    0	1.00	Naked Single
//				// detecting any Aligned10Exclusion hints to remember hintNumber
//				// detecting any Aligned6Exclusion hints to remember hintNumberS
//				//   hint-details-line
//				//  hid	      time (ns)	ce	mayb	eli	hinter                        	hint
//				//   35	    154,194,702	29	 154	  1	Aligned Dec                   	Aligned Dec: E1, E2, E3, D4, F4, D5, E5, F5, D6, F6 (F6-3)
//				found = false;
//				while ( (line=reader.readLine()) != null ) {
//					++lineNumber;
//					//look for the next hint-summary-headers line, starts with 5 spaces \t more spaces
//					//     	        time (ns)	     average (ns)	calls	hints	elims	diff	hinter
//					if ( line.startsWith("     ") ) { // test "    " first, coz it's longer
//						found = true;
//						break;
//					//a hint-details-line, starts with a space, unlike the puzzle-clues and puzzle-maybes lines
//					//   35	         19,394	28	 161	  1	Point and Claim               	Pointing: A5, C5 on 5 in box 4 and row 5 (G5-5)
//					} else if ( line.startsWith(" ") && line.length()>NOM_END ) {
//						nom = line.substring(NOM_BEGIN, NOM_END).trim();
//						Set<Integer> hintNumbersSet = hintNumbersMap.get(nom);
//						if ( hintNumbersSet != null )
//							hintNumbersSet.add(MyInteger.parse(line, 0, 5));
//					}
//				}
//				if ( !found )
//					throw new IOException("Not found: hinter-summary-headers-line");
//
//				// now parse each hinter-summary-line
//				Set<String> hintyHintersSet = new HashSet<>(32, 0.75F);
//				found = false;
//				while ( (line=reader.readLine()) != null ) {
//					++lineNumber;
//					if ( line.startsWith("     ") && !line.startsWith("                                Kraken") ) {
//						// parse the hinter-summary-line to workout if this
//						// hinter hinted in the previous run, and if so then
//						// add it to hintyHintersSet.
//						found = true;
//						String[] fields = line.split(" *\\t *");
//						if ( !"0".equals(fields[4]) ) { // numHints
//							nom = fields[7];
//							Set<Integer> hintNumbers = hintNumbersMap.get(nom);
//							if ( hintNumbers!=null && !hintNumbers.isEmpty() )
//								nom += asString(hintNumbers);
//							hintyHintersSet.add(nom);
//						}
//					} else if ( line.startsWith(puzzleNumberD5) ) {
//						// the puzzle-summary-line (last line for this puzzle)
//						break; // so we'll continue looking for next puzzle at
//						       // the line after this one
//					}
//				}
//				if ( !found )
//					throw new IOException("Not found: hinter-summary-line");
//				// store the hintyHintersSet
//				hintyHinters[pid-1] = hintyHintersSet;
//			}
//			return true;
//		} catch (Exception ex) {
//			System.err.println("lineNumber: "+lineNumber);
//			System.err.println("line: "+line);
//			ex.printStackTrace(System.err);
//			return false;
//		}
//	}
//	private static final int NUM_NOMS = 64;
//	public static long lineNumber = 0;
//
//	// Get the nom's of the wantedHinters which implement INumberedHinter,
//	// which we only use to parse the previous logFile in -SPEED mode.
//	// In "normal" ACCURACY mode this method is just never called.
//	private static String[] getNumberedHinterNoms() {
//		if ( numberedHinterNoms == null ) {
//		// nb: The LS must be in Mode.ACCURACY for this to work, else most
//		//     hinters are not wanted, so we'd miss almost all there nom's.
//			String[] noms = LogicalSolverFactory.get(LogicalSolver.Mode.ACCURACY, false, null)
//						.getNumberedHinterNoms();
//			numberedHinterNoms = noms;
//		}
//		return numberedHinterNoms;
//	}
//	// nb: nom is French for name, which is already taken by Enum.
//	private static String[] numberedHinterNoms;
//
//	private static String asString(Set<Integer> hintNumbers) {
//		if ( hintNumbers.isEmpty() )
//			return "";
//		return " " + Frmt.frmtIntegers(hintNumbers, " ", " ");
//	}
//
//	private static class SkipReader extends BufferedReader {
//		private SkipReader(File inputFile) throws FileNotFoundException {
//			super(new FileReader(inputFile));
//		}
//		// read lines from reader until line startsWith startOfLine when I
//		// return true, else (EOF searching) I return false.
//		// nb: starts at the NEXT line from reader, not the current line.
//		// nb: leaves the reader ready to read the line following the line
//		//     which startsWith startOfLine, NOT the line itself, which is
//		//     thrown away with all those which preceeded it.
//		boolean skipDownToNextLineStartingWith(String startOfLine) throws IOException {
//			String line;
//			while ( (line=super.readLine()) != null ) {
//				++lineNumber;
//				if ( line.startsWith(startOfLine) )
//					return true;
//			}
//			return false;
//		}
//	}

}
