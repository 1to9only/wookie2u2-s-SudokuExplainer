/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Log represents a simple log-file. First thing I usually do is open a
 * PrintWriter on the log-file (in the main method) and then stuff it in here.
 * <p>
 * A simple log-file is preferable to a complex architectural framework.
 * @author Keith Corlett 2013
 */
public final class Log {

	public static final String NL = diuf.sudoku.utils.Frmt.NL;

//	public static final PrintStream DEV_NULL = DevNullPrintStream.out;

	// Use if(Log.mode>=Log.VERBOSE_MODE) with >= and the SEARCHABLE constant. Yes, greater-than-OR-EQUALS-TOO is a tad slower dad, but fickit (that's Scottish for ____IT!; which many people find a lot less confronting, for some reason. I guesse it might be that the Scottish have lived next door to the English for so ficking long that it's no wonder they've learned to swear there ____ing-heads-right-off with an alactrity which has even been known to raise a Jewish eyebrow): machines are like a 1,000,000 times faster now than they were when I was a lad, so just fickit! Do what works. And Trump and his ____wit Jewish mates could learn something from that sentence too. There is no perfect YOU MUPPETS! Killing of the neibours because there s__t stinks doesn't make you clever it just makes you a ____ing butthole, and if you ever attempt to move in next door to me I shall wag on you most wuffly, ____ING ____KNUCKLES!
	public static final int SILENCE_MODE   = -1; // also set Log.out = Log.DEV_NULL;
	// KRC need hinter-summary for -REDO so bumped-up NORMAL_MODE from 0 to 10,
	// so it'll always produce hinter-summary-lines, coz I've just run it for
	// 40-odd minutes for bloody nothing, just because I forgot to re-bump-up
	// the bloody log-mode following a ____ing build. Screw that up ya NUGGET!
	public static final int NORMAL_MODE    = 10;  // 1 line per puzzle (basics)
	public static final int VERBOSE_1_MODE = 10; // + 1 line per Hinter (summary)
	public static final int VERBOSE_2_MODE = 20; // + 1 line per Hint
	public static final int VERBOSE_3_MODE = 30; // + the grid (2 lines per hint)
	public static final int VERBOSE_4_MODE = 40; // + 1 line per Aggregated Chaining Hint (only in SPEED mode)
	public static final int VERBOSE_5_MODE = 50; // also logs grid + hint in GUI

	// Use: if(Log.mode==Log.MODE_100) NOTE the == and use of searchable constant
	public static final int MODE_100 = 100; // LogicalSolver noise
	public static final int MODE_200 = 200; // Grid iAmInMyRegionsIndexesOf
	public static final int MODE_300 = 300; // Generate noise

	// log-parsers: tools for VERBOSE_5_MODE format logs.
	// Really: I use the details to help find bugs.
	public static final int MODE = VERBOSE_5_MODE;

	public static PrintStream out = System.out;
	public static String PUZZLE_SUMMARY_HEADERS;

	public static void print(String msg) {
		out.print(msg);
	}
	public static void print(Object msg) {
		out.print(msg);
	}

	public static void println() {
		out.println();
	}
	public static void println(Object o) {
		out.println(o);
	}

	public static void format(String format, Object... args) {
		out.format(format, args);
	}

	public static void flush() {
		out.flush();
		if ( out != System.out )
			System.out.flush();
	}

	public static void close() {
		if ( out != System.out )
			out.close();
	}

	public static long div(long l, long i) {
		return i==0L ? 0L : l/i;
	}

	public static double div(double d, long i) {
		return i==0L ? 0D : d / (double)i;
	}

	public static double pct(long howMany, long of) {
		return howMany==0L||of==0L
				? 0.00D
				: (double)howMany / (double)of * 100.00D;
	}
	
	public static String tabs(int howMany) {
		return TABS.substring(0, howMany);
	}
	private static final String TABS
			= "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"; // 50 tabs

	public static void percent(long howMany, long of) {
		percent("%,d of %,d = %4.2f\n", howMany, of);
	}
	public static void percent(String frmt, long howMany, long of) {
		out.format(frmt, howMany, of, pct(howMany, of));
		if ( out != System.out )
			System.out.format(frmt, howMany, of, pct(howMany, of));
	}

	// this is used in recursive algorithms, where depth is a product of the
	// depth of the recursive call-stack, and char 'c' is a space.
	// You could also use this to right-justify a binary number string
	// (with '0'), or whatever.
	// s = cell.maybes.bits.toBinaryString();
	// indentf('0', 10-s.length(), "%s", s);
	public static void indentf(char c, int depth, String fmt, Object... args) {
		for (int i=0; i<depth; ++i)
			out.print(c);
		out.format(fmt, args);
	}

	public static void tee(String msg) {
		out.print(msg);
		if ( out != System.out )
			System.out.print(msg);
	}
	public static void teeln() {
		out.println();
		if ( out != System.out )
			System.out.println();
	}
	public static void teeln(Object... os) {
		printAll(out, " ", os);
		out.println();
		if ( out != System.out ) {
			printAll(System.out, " ", os);
			System.out.println();
		}
	}
	public static void printAll(PrintStream out, String sep, Object... objects) {
		final int n = objects==null ? 0 : objects.length;
		if ( n > 0 ) {
			out.print(objects[0]);
			for ( int i=1; i<n; ++i) {
				out.print(sep);
				out.print(objects[i]);
			}
		}
	}
	public static void teef(String frmt, Object... args) {
		out.format(frmt, args);
		if ( out != System.out )
			System.out.format(frmt, args);
	}
	public static void teeTrace(Throwable t) {
		t.printStackTrace(out);
		if ( out != System.out )
			t.printStackTrace(System.out);
	}

	/**
	 * Returns my callers class.methodName.
	 * @return 
	 */
	public static String me() {
		StackTraceElement e = new Throwable().getStackTrace()[1]; // 0 is this method, 1 is my direct caller
		return e.getClassName()+"."+e.getMethodName();
	}

	/**
	 * Returns my callers class.methodName.
	 * @param i the StackTrace index of me, for methods who log as there caller.
	 * @return 
	 */
	public static String me(int i) {
		StackTraceElement e = new Throwable().getStackTrace()[i]; // 0 is this method, 1 is my direct caller
		return e.getClassName()+"."+e.getMethodName();
	}

	public static void whinge(String msg, Throwable t) {
		teeln(msg);
		teeTrace(t);
		flush();
	}

	public static void initialise(PrintStream out) {
		Log.out = out;
		//nb: need the StringWriter coz PrintWriter.toString is Object.toString.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.format("%5s\t%17s\t%17s\t%5s\t%5s\t%5s\t%4s\t%7s\t%s"
				, "", "time (ns)", "average (ns)", "calls"
				, "hints", "elims", "maxD", "ttlDiff", "hinter");
		Log.PUZZLE_SUMMARY_HEADERS = sw.toString();
	}

//	// ============================= stats ==================================
//
//	public static PrintStream openStats() {
//		try {
//			IO.backup(IO.PERFORMANCE_STATS);
//			return new PrintStream(IO.PERFORMANCE_STATS);
//		} catch (Exception ex) {
//			StdErr.whinge("openStats failed", ex);
//			return null;
//		}
//	}

	// =========================== constructor ================================

	private Log() {} // never used

}
