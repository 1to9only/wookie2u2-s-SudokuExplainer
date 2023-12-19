/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test;

import diuf.sudoku.*;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.F;
import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Constants.T;
import static diuf.sudoku.Constants.beep;
import static diuf.sudoku.test.TestHelp.*;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.*;
import static diuf.sudoku.solver.Print.PUZZLE_SUMMARY_HEADERS;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.utils.*;
import diuf.sudoku.utils.Formatter.FormatString;
import static diuf.sudoku.utils.Frmt.COMMA;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Log.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * LogicalSolverTester (aka "the batch") runs {@link LogicalSolver#solve} for
 * each puzzle in a MagicTour file.
 * <p>
 * The current setup solves <tt>top1465.d5.mt</tt> in about two minutes,
 * depending on wanted hinters: GUI ~ Options ~ Solving techniques.
 * <p>
 * Some hinters are faster than others. My chainers are pretty damn fast. Best
 * avoid SueDeCoq, Aligned*Exclusion, Finned Jellyfish, All Krakens, and All
 * Mutants.
 * The rest are all "fast enough" no matter what you do with them.
 * <p>
 * My Recommended Hinter Selection:<ul>
 * <li>All hinters should be selected by default, so just unselect stuff in
 * this list. Ergo: Select everything, then untick this s__t!
 * <li>The Naked Single and Hidden Single hinters are permanently selected coz
 *  it just does not make any sense to try to solve a Sudoku without them.
 * <li>Drop Naked and Hidden Pent (degenerate).
 * <li>Use BigWings instead of the individual S/T/U/V/WXYZ_Wing hinters, which
 *  are actually faster, but BigWings is faster overall because it primes the
 *  ALS cache that is used later in DeathBlossom, AlsXz, AlsWing, and AlsChain.
 * <li>Drop all JellyFish (too slow, except basic Jellyfish which finds naught)
 * <li>Drop all Krakens (too slow)
 * <li>Drop all Mutants (too slow)
 * <li><i>Kraken Jellyfish and Mutant Jellyfish are FAR too slow!</i>
 * <li>Drop all Aligned*Exclusion (too slow)
 * <li>Want DynamicPlus (sane) or NestedUnary (expert) to solve all puzzles.
 * </ul>
 *
 * @author Keith Corlett: based on Juillerat. Mistakes are mine.
 */
public final class LogicalSolverTester {

	// Assholometer: Should we hack the s__t out of s__t to make s__t faster?
	private static final boolean IS_HACKY = CFG.getBoolean(Config.isHacky);

	// KRC 2020-08-20 888#top1465.d5.mt siamese bug: SIAMESE_LOCKING=true makes
	// LogicalSolverTester behave like GUI, to find SiameseLocking test-cases.
	// Note that Locking must be wanted (not LockingGen) for siamese to work.
	private static final boolean SIAMESE_LOCKING = false; // @check false

	// KRC 2023-06-22 disable fisherman and chainers internal caches in order
	// to reproduce the SAME hints reliably in GUIs new logFollow feature.
	// This slows-down the batch ~15%, but I see no other option.
	// KRC 2023-06-27 Unnecessary. Grid.skip bug misses hints, not caching!
	// Retained coz its useful, and I may use it in future, and I like it.
	private static final boolean NO_CACHES = false; // @check false

	// When 1 puzzle is reprocessed print hint.toHtml to stdout
	private static final boolean PRINT_HINT_HTML = false; // #check false

	private static final Formatter FORMATTER = new Formatter();
	private static final Locale LOCALE = Locale.getDefault();

	private static final String ENABLE_ASSERTS = Run.ASSERTS_ENABLED ? " -enableassertions" : "";
	private static final String JAVA_VERSION = System.getProperty("java.runtime.version");
	private static final String WANTED_SLOWIES = Tech.wantedSlowHinters();

	private int printUsage() {
		IO.cat(IO.LOGICAL_SOLVER_TESTER_USAGE, System.err);
		return 1;
	}

	private int carp(final String msg) {
		System.err.println(msg);
		return 1;
	}

	private int carp(final String msg, final Throwable ex) {
		System.err.println(msg);
		System.err.flush();
		ex.printStackTrace(System.err);
		return 1;
	}

	private int carp(final String logger, final Grid grid, final Throwable ex
			, final PrintStream err) {
		err.println("WARN: "+logger+" carp");
		err.println(grid.toString());
		err.flush();
		ex.printStackTrace(err);
		return 1;
	}

	/**
	 * Solve Sudokus logically to $input.log progress only to stdout.
	 *
	 * @param args the name of the *.mt input file (atleast) see USAGE.
	 */
	public static void main(final String[] args) {
		try {
			Run.setRunType(Run.Type.Batch);
			new LogicalSolverTester().run(args);
		} catch (Exception ex) {
			StdErr.whinge("WARN: Batch failed.", ex);
		}
		beep(); beep(); beep();
	}

	private int numSolved=0, numFailed=0;

	private int solvedCount = 0;
	private long ttlTook = 0L;
	private LogicalSolver solver;

	// aliases future proofing for backwards compatibility if techs are renamed
	private static final Map<String,String> ALIASES = new HashMap<>(16, 0.75F);
	{
		// oldName => newName
		ALIASES.put("X-Wing", "Swampfish");
	}

	@SuppressWarnings({"unchecked", "rawtypes"}) // just for the array of Set
	public int run(String[] args) {
		int retval = 1; // presume failure
		long start = System.nanoTime();
		// ---------- read the command line arguments ----------
		if ( args.length < 1 ) // inputFilename required
			return printUsage();
		// poll the inputFilename from args
		final File inputFile = new File(args[0]);
		args = MyStrings.leftShift(args);
		String inputPath = inputFile.getAbsolutePath();
		if ( !inputFile.exists() )
			return carp("Input file not found: "+inputPath);
		// avert accidentally reading a .log file (has happened)
		// BEWARE: on *nix we are insisting on a lowercase .mt
		if ( !MyFile.nameEndsWith(inputPath, ".mt") )
			return carp("Expected *.mt as input not: "+inputPath);
		//<HACK>
		// AHinter.hackTop1465: we are processing top1465.d5.mt in HACK mode.
		// Aligned*Exclusion uses AHinter.hackTop1465 to enable speed hacks.
		// LogicalSolver uses AHinter.hackTop1465 to set the firstHintNumbers.
		// nb: 1465 is my batch-testing file: currently top1465.d5.mt
		final boolean isTop1465 = MyFile.nameContains(inputPath, "top1465");
		//</HACK>
		// remaining args are either -wantedHinters or pids (puzzle numbers)
		boolean isAssertOk = false;
		int[] pids = null;
		while ( args.length > 0 ) {
			if ( args[0].equals("-ea1465") ) {
				isAssertOk = true;
				args = MyStrings.leftShift(args);
			} else if ( args[0].startsWith("-wantedHinters") ) {
				// nb: CFG is not saved so we can do whatever we like to it
				CFG.setWantedTechs(parseWantedHinters(args));
				break;
			} else {
				pids = Frmt.toIntArray(args); // may still be null
				// printHintHtml is insanely verbose!
				// WARN: search PRINT_HINT_HTML to comment out other stuff.
				AHint.printHintHtml = PRINT_HINT_HTML && pids!=null && pids.length==1;
				break;
			}
		}
		// prevent accidental java -ea batch for consistent timings
		if ( isTop1465 && Run.ASSERTS_ENABLED && !isAssertOk )
			throw new IllegalArgumentException("No java -ea for batch (timings); -ea1465 overrides.");
		// get the output logFilename from the inputFilename
		// KRC 2019-10-03 put run date-time in logFilename.
		final String now = Config.startDateTime(); // used in log
		// stick $now in the filename so it will not be overwritten.
		final String ext = "."+now+".log";
		// Do two search-and-replaces for MAD Windows users with .MT files.
		final String logFilename = inputPath.replaceFirst("\\.mt$", ext)
				.replaceFirst("\\.MT$", ext.toUpperCase());
		final File logFile = new File(logFilename);
		// --------------------------- begin real work ------------------------
		// So lets run this little fire trucker down the list...
		try ( PrintStream out = new PrintStream(new FileOutputStream(logFile), true) ) {
			initialise(out);
			// nb: LogicalSolver.want (et al) write to Log.out
			solver = LogicalSolverFactory.get();
			// now unused but retained anyway, for future use.
			if ( NO_CACHES )
				solver.disableInternalCaches();
			// KRC 2020-08-20 find and fix SiameseLocking bug. Make the batch
			// behave like the GUI, to find test-cases for SiameseLocking.
			if ( SIAMESE_LOCKING ) {
				solver.setSiamese();
				if ( LOG_MODE >= VERBOSE_5_MODE )
					teeln("logicalSolver.locking.setSiamese!");
			}
			// =================== RETEST SPECIFIC PUZZLE/S ===================
			if ( pids!=null && pids.length>0 ) {
				final UsageMap totalUsages = new UsageMap();
				final boolean logIt = true;
				final boolean logTimes = false;
				for ( int pid : pids ) { // pids are 1 based
					System.out.println("processing pid "+pid+" ...");
					final Line line = readALine(inputFile, pid);
					solve(line, totalUsages, logIt, logTimes, true);
					// print running-total usages (for A*E monitoring)
					printTotalUsageMap(totalUsages);
				}
				// clean-up
				try {
					solver.close();
				} catch (IOException impossible) {
					// Do nothing
				}
				return 0;
			}
			// ====================== WHOLE PUZZLE FILE =======================
			// priming-solves: repeatedly solve to not time JIT compilation
			if ( !primingSolves(inputFile) )
				return 1;
			// preamble
			printHeaders(now, inputFile, logFile);
			if ( LOG_MODE >= VERBOSE_2_MODE )
				solver.reportConfiguration();
			// run
			final boolean logHints = true;
			final UsageMap ttlUsages = new UsageMap();
			final boolean isPrinting = LOG_MODE >= VERBOSE_1_MODE;
			final boolean wantSolution = LOG_MODE <= NORMAL_MODE;
			try ( BufferedReader reader = new BufferedReader(new FileReader(inputFile)) ) {
				Line line;
				String contents;
				int lineCount = 0;
				while ( (contents=reader.readLine()) != null ) {
					line = new Line(inputFile, ++lineCount, contents);
					if ( !solve(line, ttlUsages, logHints, false, wantSolution) )
						break;
					// print running total usages (for A*E monitoring)
					if ( isPrinting )
						printTotalUsageMap(ttlUsages);
				} // next line in .mt file
			}
			// post
			final long took = System.nanoTime() - start;
			if ( numFailed > 0 ) {
				format("FAILURES "+numFailed+NL);
				return carp("FAILURES "+numFailed);
			}
			// print running total usages (for A*E monitoring)
			if ( !isPrinting )
				printTotalUsageMap(ttlUsages);
			printRunSummary(took, numSolved);
			// here is where we print any extra stuff
//			teeln("Ass COUNTS="+java.util.Arrays.toString(Ass.COUNTS));
//			diuf.sudoku.solver.hinters.als.RccFinderForwardOnly.report();
//			teeln("Idx: COUNTS="+java.util.Arrays.toString(Idx.COUNTS));
//			teeln("ATableChainer: COUNTS="+java.util.Arrays.toString(diuf.sudoku.solver.hinters.table.ATableChainer.COUNTS));
			// KEEP: maxSize of each FunkyAssSet when REPORT.
			if ( FunkyAssSetFactory.REPORT )
				FunkyAssSetFactory.report();
			// clean-up
			try {
				solver.close();
			} catch (IOException impossible) {
				// Do nothing
			}
			System.out.flush(); // ____ing dildows
			Log.out.flush(); // ____ing dildows
			retval = 0; // it is all good
		} catch (Exception ex) {
			System.out.println("WARN: "+me()+" exception");
			System.out.flush();
			ex.printStackTrace(System.out);
			System.out.flush();
		}
		return retval; // it is all good
	}

	// Do 6 to 12 priming-solves to do all the JIT (Just In Time) compilation.
	// The JIT compiler effects timings, which I use to optimise code, so I JIT
	// compile (mostly) BEFORE I start timing. A few trailing-JITs dont matter.
	// This is the first execution of solve, so bugs there show-up here, hence
	// primingSolves is bug-savy, and the solve in process basically works.
	// Ergo, I find most solve-bugs, and process tends to find hinter-bugs.
	// NOTE: If slow techs are wanted then primingSolves is a no-op.
	// NOTE: If solve exceeds 3 seconds then primingSolves bails-out.
	private boolean primingSolves(final File inputFile) throws IOException {
		// Display the opening priming-solves tag
		System.out.println(primingSolvesTag());
		// minimum solveTime: we can stop now, it's fast enough
		int fastEnough = 300; // milliseconds for java 7
		if(JAVA_VERSION.startsWith("1.8")) fastEnough -= 100; // 1.8 < 7
		if(Run.ASSERTS_ENABLED) fastEnough += 50;
		if(LOG_MODE==NORMAL_MODE) fastEnough -= 100;
		// maximum solveTime: don't wait all day for primingSolves.
		final int tooSlow = 5000; //ms (5 seconds)
		// start the actual work
		final String puzzle = readALine(inputFile, 1).contents;
		final Grid grid = new Grid();
		for ( int times=1; times<13; ++times ) {
			grid.load(puzzle);
			solver.prepare(grid);
			try {
				final long start = System.currentTimeMillis();
				//                                             log, logHinters
				if ( !solver.solve(grid, new UsageMap(), T, T, false, false) ) {
					System.out.println("FAIL: solve returned false!");
					System.exit(1);
				}
				final long took = System.currentTimeMillis() - start;
//				if ( times>5 && took<fastEnough )
//					break; // fast enough
				// dont wait for priming-solves when it matters not
				if ( took>tooSlow )
					break; // slow hinter/s
			} catch (UnsolvableException ex) {
				teeTrace(ex);
				System.out.println("FAIL: solve failed! "+ex);
				System.exit(1);
			}
		}
		System.out.println("</priming-solves>");
		return true;
	}

	// build the priming-solves tag
	private String primingSolvesTag() {
		final StringBuilder tag = SB(128);
		// I allways want to know -version
		// nb: old Java 1.8.0_161-b12 is faster than 17.0.1+12-LTS-39
		//     -enableassertions also impedes solve, significantly
		tag.append("<priming-solves ver=\"").append(JAVA_VERSION).append("\"");
		// enableassertions makes me slower
		if ( Run.ASSERTS_ENABLED )
			tag.append(" -ea");
		// turbo makes DynamicChain and DynamicPlus faster
		if ( CFG.getBoolean("turbo") )
			tag.append(" turbot");
		// no verbose logging makes me faster
		if ( LOG_MODE != VERBOSE_5_MODE )
			tag.append(" LOG_MODE=").append(LOG_MODE);
		// A few techs are tooSlow for REPEATED primingSolve, hence we solve
		// ONE puzzle, then bail-out. No tech takes ALL day for ONE puzzle!
		// What I really want is a way to make it bail mid-puzzle if it takes
		// more than a minute, but how? Start a monitor thread that waits one
		// minute then interrupts the main solve thread, and post-solve kill
		// the monitor thread. I am HOPELESS at threading! I don't even try.
		if ( !WANTED_SLOWIES.isEmpty() )
			tag.append(" slow=\"").append(WANTED_SLOWIES).append("\"");
		tag.append(">");
		return tag.toString();
	}

	private boolean solve(
		  final Line line
		, final UsageMap ttlUsages
		, final boolean logHints
		, final boolean logTimes
		, final boolean wantSolution
	) {
		if ( logHints ) {
			++solvedCount;
			// puzzle-header-line for when we are logging a line per hint
			if ( LOG_MODE >= VERBOSE_2_MODE ) {
				println();
				println(line.toString()); // $pid#$absolutePath\t$contents
			}
		}
		final Grid grid = new Grid(line.contents);
		grid.source = new SourceID(line.file, line.number);
		long took = 0L;
		long start = System.nanoTime();
		try {
			// prepare any preppers
			solver.prepare(grid);
			// solve the puzzle!
			final UsageMap usageMap = new UsageMap(); // Hinter usage summary
			boolean isSolved = solver.solve(grid, usageMap, T, F, logHints, logTimes);
			if ( logHints )
				ttlTook += took = System.nanoTime() - start;
			if ( !isSolved )
				throw new UnsolvableException(grid.invalidity);
			++numSolved;
			if ( logHints ) {
				// print the hinter detail lines in VERBOSE_MODE
				int maxDifficulty=0, ttlDifficulty=0;
				IHinter hardestHinter = null;
				final Usage ttlUsage = new Usage("totals", 0, 0, 0, 0L);
				// if any logging
				if ( LOG_MODE >= NORMAL_MODE
					// if first puzzle only
					&& ( solvedCount == 1
					  // or repeat coz hints mean you cannot see previous
					  || LOG_MODE >= VERBOSE_2_MODE ) )
						print(PUZZLE_SUMMARY_HEADERS);
				for ( Entry<IHinter,Usage> entry : usageMap.entrySet() ) {
					final Usage usage = entry.getValue();
					ttlUsage.add(usage);
					if ( usage.elims > 0 ) {
						if ( usage.maxDifficulty > maxDifficulty ) {
							maxDifficulty = usage.maxDifficulty;
							hardestHinter = entry.getKey();
						}
						ttlDifficulty += usage.ttlDifficulty;
					}
					if ( LOG_MODE >= VERBOSE_2_MODE )
						printHintDetailsLine(Log.out, usage.time
							, usage.calls, usage.hints, usage.elims
							, maxDifficulty, ttlDifficulty
							, String.valueOf(entry.getKey()));
				}
				if ( LOG_MODE >= NORMAL_MODE )
					printPuzzleSummary(line, took, ttlTook/solvedCount
						, ttlUsage.calls, ttlUsage.hints, ttlUsage.elims
						, maxDifficulty, ttlDifficulty
						, String.valueOf(hardestHinter)
						, wantSolution ? grid : null);
				flush();
				if ( ttlUsages != null )
					ttlUsages.addonAll(usageMap);
			}
			return true;
		} catch (Exception ex) { // thrown by solver.solve
			if(took==0L) took = System.nanoTime() - start;
			++numFailed;
			//NB: we wait 50ms before printing to stderr so stdout goes first
			println();
			format("%5d", line.number);
			format(" %s", line.file.getName());
			format("\t%,15d", took);
			format("\t%5d", 0);
			format("\t%4d", 0);
			format("\t%s", ex);
			println();
			carp(me(), grid, ex, Log.out);
			if ( Log.out != System.out )
				carp(me(), grid, ex, System.out);
			return false;
		}
	}

	private static void printHeaders(final String now, final File inputFile, final File logFile) {
		// create solver here coz logs unwanted hinters in NORMAL_MODE
		teef("%s java%s %s ran %s%s", Build.ATV, ENABLE_ASSERTS
				, JAVA_VERSION, now, NL);
		// + 1 line per Hint (header)
		if ( LOG_MODE >= VERBOSE_2_MODE ) {
			teef("hacky   : %s%s", IS_HACKY?"HACKY ":"NOT_HACKY", NL);
			teef("input   : %s%s", inputFile.getAbsolutePath(), NL);
			teef("log %2d  : %s%s", LOG_MODE, logFile.getAbsolutePath(), NL);
			teef("stdout  : progress only%s", NL);
			println();
			printHintDetailsHeaders(Log.out);
		}
		println();
	}

	private static void printHintDetailsHeaders(final PrintStream out) {
		// left justified to differentiate it from the puzzleNumber in logFile,
		// so assuming that there is less than 10,000 puzzles in each file, and
		// that no puzzle can ever possibly require more than 9,999 hints:
		// search  ^\d+ +\t to find a hint detail line
		// search: ^ +\d+\t to find a puzzle summary line
		out.format("%-5s\t%15s\t%2s\t%4s\t%3s\t%-31s\t%s\n"
			, "hid", "time(ns)", "ce", "mayb", "eli", "hinter", "hint");
	}

	private static final FormatString[] HINT_DETAILS_FSA = FORMATTER.parse(
		"%5s\t%,17d\t%,17d\t%5d\t%5d\t%5d\t%5d\t%7d\t%s\n");

	private void printHintDetailsLine(final PrintStream out, final long time
			, final int calls, final int hints, final int elims
			, final int maxDifficulty, final int ttlDifficulty
			, final String hinterName) {
		// hint-details-line: + 1 line per Hint
		out.print(FORMATTER.format(LOCALE, HINT_DETAILS_FSA, EMPTY_STRING
			, time, div(time,calls), calls, hints, elims, maxDifficulty
			, ttlDifficulty, hinterName));
	}

	private static final String HINTER_SUMMARY_HEADER = String.format(
		  "%18s\t%7s\t%14s\t%7s\t%14s\t%s\n"
		, "time(ns)", "calls", "time/call", "elims", "time/elim", "hinter");

	private static final FormatString[] HINTER_SUMMARY_FSA = FORMATTER.parse(
			"%,18d\t%7d\t%,14d\t%7d\t%,14d\t%s\n");

	// totalUsageMap: + 1 line per Hinter (summary) HEADER
	private static void printTotalUsageMap(final UsageMap ttlUsages) {
		println();
		// totalUsageMap: + 1 line per Hinter (summary) DETAIL
		long ttlTime = 0L;
		Log.out.print(HINTER_SUMMARY_HEADER);
		for ( Usage u : ttlUsages.toArrayList() ) {
			Log.out.print(FORMATTER.format(LOCALE, HINTER_SUMMARY_FSA, u.time
			, u.calls, div(u.time,u.calls), u.elims, div(u.time,u.elims)
			, u.hinterName));
			ttlTime += u.time;
		}
		format("%,18d%s", ttlTime, NL);
	}

	// The format string need only be parsed ONCE, for speed
	// nb: my hacked version of java.util.Format exposes the parse mathod.
	private static final FormatString[] PUZZLE_SUMMARY_FSA = FORMATTER.parse(
			"%5d\t%,17d\t%,17d\t%5d\t%5d\t%5d\t%5d\t%7d\t%-20s\t%s");

	private static void printPuzzleSummary(final Line line, final long took
			, final long average, final int ttlCalls, final int ttlHints
			, final int ttlElims, final int maxDifficulty
			, final int ttlDifficulty, final String hardestHinter
			, final Grid solution) {
		// String formatting is a bit slow so we do it once, because this is
		// called 1465 times in top1465 and I'm being anally retentive.
		// nb: hacked version of java.util.Formatter exposes format taking
		// the pre-parsed FormatString[], instead of parsing the same bloody
		// format String every time we want to log a bloody puzzle, for speed.
		// This saves about 700ms over top1465. parse(format) is hard!
		final StringBuilder s = (StringBuilder)FORMATTER.format(LOCALE
			, PUZZLE_SUMMARY_FSA, line.number, took, average, ttlCalls
			, ttlHints, ttlElims, maxDifficulty, ttlDifficulty, hardestHinter
			, line.contents).out();
		// to stdout without the solution
		System.out.println(s);
		// to log with the solution, if any
		if ( solution != null )
			println(s.append("\t").append(solution));
		else
			println(s);
	}

	private static void printRunSummary(final long took, final int numSolved) {
		final int secs = (int)(took / 1000000000);
		final int mins = secs / 60;
		final int hrs = mins / 60;
		if ( hrs > 0 )
			teef("\n%5s\t%17s\t(hh:mm:ss)\t%17s\n"
					 +"%5d\t%,17d\t(%02d:%02d:%02d)\t%,17d\n"
				, "pzls", "total (ns)", "each (ns)"
				, numSolved, took, hrs, mins%60, secs%60, div(took,numSolved));
		else
			teef("\n%5s\t%17s\t(mm:ss)\t%17s\n"
					 +"%5d\t%,17d\t(%02d:%02d)\t%,17d\n"
				, "pzls", "total (ns)", "each (ns)"
				, numSolved, took, mins, secs%60, div(took,numSolved));
	}

	/**
	 * Parses the wantedHinter args into an {@code EnumSet<Tech>}.
	 * <p>
	 * Args is a comma-space separated list thats been through CMDs (Windows)
	 * parser which breaks it into a space-separated list, leaving a trailing
	 * comma on each element so we end up with the below mess. No wuckers: all
	 * we do is remove the trailing comma (if present) from each name.
	 * <p>
	 * I am pretty sure that *nix shells do the same thing (by default).
	 * <p>
	 * Example args:
	 * {@code {"-wantedHinters:", "NakedSingle,", "HiddenSingle,", ...}}
	 *
	 * @param args Tech.name()s of the hinters you want. See method comments.
	 * @return an EnumSet of the wantedHinter Tech
	 */
	private static EnumSet<Tech> parseWantedHinters(final String[] args) {
		final int n = args.length;
		if ( n < 2 ) // not including args[0]
			throw new IllegalArgumentException("Empty wantedHinters list!");
		// the result wantedTechs set
		final EnumSet<Tech> techs = EnumSet.noneOf(Tech.class);
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
			  && ( (newName=ALIASES.get(name)) == null
				|| (tech=Tech.valueOf(newName)) == null ) )
				throw new IllegalArgumentException("Unknown Tech: "+name
						+(newName!=null?" newName=\""+newName+"\"":""));
			techs.add(tech);
		}
		return techs;
	}

}
