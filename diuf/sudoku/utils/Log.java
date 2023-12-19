/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.PERIOD;
import java.io.PrintStream;
import static diuf.sudoku.utils.Frmt.SP;

/**
 * Log represents a simple log-file. First thing I usually do is open a
 * PrintWriter on the log-file (in the main method) and then stuff it in here.
 * <p>
 * A simple log-file is preferable to a complex architectural framework.
 * <p>
 * KRC 2021-10-05 out is now nullable, for the test-cases, where it often just
 * remains null, because I do not want logging from the test-cases, so whenever
 * out is referenced we first check out != null. Simple, but verbose. sigh.
 *
 * @author Keith Corlett 2013
 */
public final class Log {

//	public static final PrintStream DEV_NULL = DevNullPrintStream.out;

	// Use if(Log.mode>=Log.VERBOSE_MODE) with >= and the SEARCHABLE constant. Yes, greater-than-OR-EQUALS-TOO is a tad slower dad, but fickit (that is Scottish for ____it!; which many people find a lot less confronting, for some reason. I guesse it might be that the Scottish have lived next door to the English for so ficking long that it is no wonder they've learned to swear there ____ing-heads-right-off with an alactrity which has even been known to raise a Jewish eyebrow): machines are like a 1,000,000 times faster now than they were when I was a lad, so just fickit! Do what works. There is no perfect YOU MUPPETS! Killing of the neibours because there s__t stinks does not make you clever it just makes you a ____ing butthole, and if you ever attempt to move in next door to me I shall wag on you most wuffly, ____ING ____KNUCKLES!
	public static final int SILENCE_MODE   = -1; // also set Log.out = Log.DEV_NULL;
	public static final int NORMAL_MODE    = 0;  // 1 line per puzzle (basics)
	public static final int VERBOSE_1_MODE = 10; // + 1 line per Hinter (summary)
	public static final int VERBOSE_2_MODE = 20; // + hints (1 line per Hint)
	public static final int VERBOSE_3_MODE = 30; // + grids (2 lines per hint)
	public static final int VERBOSE_4_MODE = 40; // + 1 line per Aggregated Chaining Hint (only in SPEED mode)
	public static final int VERBOSE_5_MODE = 50; // also logs grid + hint in GUI

	// Use: if(Log.mode==Log.MODE_100) NOTE the == and use of searchable constant
	public static final int MODE_100 = 100; // LogicalSolver noise
	public static final int MODE_200 = 200; // Grid iAmInMyRegionsIndexesOf
	public static final int MODE_300 = 300; // Generate noise

	// log-parsers: tools for VERBOSE_5_MODE format logs ONLY.
	// Yes Really: I use all of that detail to help find bugs.
	public static final int LOG_MODE = VERBOSE_5_MODE; // debuggable
//	public static final int LOG_MODE = NORMAL_MODE; // ~10 seconds faster

	public static PrintStream out;

	public static void print(final String msg) {
		if ( out != null )
			out.print(msg);
	}

	public static void print(final Object msg) {
		if ( out != null )
			out.print(msg);
	}

	public static void println() {
		if ( out != null )
			out.println();
	}

	public static void println(final Object o) {
		if ( out != null )
			out.println(o);
	}

	public static void format(final String format, final Object... args) {
		if ( out != null )
			out.format(format, args);
	}

	public static void flush() {
		if ( out != null )
			out.flush();
		if ( out != System.out )
			System.out.flush();
	}

	public static void close() {
		if ( out!=null && out!=System.out )
			out.close();
	}

	public static long div(final long l, final long i) {
		return i==0L ? 0L : l/i;
	}

	public static double div(final double d, final long i) {
		return i==0L ? 0D : d / (double)i;
	}

	public static double pct(final long howMany, final long of) {
		return howMany==0L||of==0L ? 0.00D
			 : (double)howMany / (double)of * 100.00D;
	}

	public static String tabs(final int howMany) {
		return TABS.substring(0, howMany);
	}

	private static final String TABS
			= "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"
			+ "\t\t\t\t\t\t\t\t\t\t"; // 50 tabs

	public static void percent(final long howMany, final long of) {
		percent("%,d of %,d = %4.2f\n", howMany, of);
	}

	public static void percent(final String frmt, final long howMany, final long of) {
		if ( out != null )
			out.format(frmt, howMany, of, pct(howMany, of));
		if ( out != System.out )
			System.out.format(frmt, howMany, of, pct(howMany, of));
	}

	// this is used in recursive algorithms, where depth is a product of the
	// depth of the recursive call-stack, and char 'c' is a space.
	// You could also use this to right-justify a binary number string
	// (with '0'), or whatever.
	// s = cell.maybes.toBinaryString();
	// indentf('0', 10-s.length(), "%s", s);
	public static void indentf(final char c, final int depth, final String fmt, final Object... args) {
		if ( out == null )
			return;
		for ( int i=0; i<depth; ++i )
			out.print(c);
		out.format(fmt, args);
	}

	public static void tee(final String msg) {
		if ( msg != null ) {
			if ( out != null )
				out.print(msg);
			if ( out != System.out )
				System.out.print(msg);
		}
	}

	public static void teeln() {
		if ( out != null )
			out.println();
		if ( out != System.out )
			System.out.println();
	}

	public static void teeln(final String msg) {
		if ( msg != null ) {
			if ( out != null )
				out.println(msg);
			if ( out != System.out )
				System.out.println(msg);
		}
	}

	public static void teeln(final Object... os) {
		if ( out != null ) {
			printAll(out, SP, os);
			out.println();
		}
		if ( out != System.out ) {
			printAll(System.out, SP, os);
			System.out.println();
		}
	}

	public static void printAll(final PrintStream out, final String sep, final Object... objects) {
		final int n = objects==null ? 0 : objects.length;
		if ( n > 0 ) {
			out.print(objects[0]);
			for ( int i=1; i<n; ++i) {
				out.print(sep);
				out.print(objects[i]);
			}
		}
	}

	public static void teef(final String frmt, final Object... args) {
		if ( out != null )
			out.format(frmt, args);
		if ( out != System.out )
			System.out.format(frmt, args);
	}

	public static void teeTrace(final Throwable t) {
		if ( out != null ) {
			out.flush();
			t.printStackTrace(out);
			out.flush();
		}
		if ( out != System.out ) {
			System.out.flush();
			t.printStackTrace(System.out);
			System.out.flush();
		}
	}

	public static void teeTrace(final String msg, final Throwable t) {
		tee(msg);
		teeTrace(t);
	}

	public static void teef(final boolean l, final boolean p, final String f, final Object... args) {
		if ( out != null )
			if(l)out.format(f, args);
		if ( System.out != out )
			if(p)System.out.format(f, args);
	}

	public static String simple(final String className) {
		final int i = className.lastIndexOf('.');
		if ( i == -1 )
			return className;
		return className.substring(i+1);
	}

	/**
	 * Returns my callers class.methodName.
	 * @return
	 */
	public static String me() {
		// 0 is this method, 1 is my direct caller
		final StackTraceElement e = new Throwable().getStackTrace()[1];
		return simple(e.getClassName())+PERIOD+e.getMethodName();
	}

	/**
	 * Returns a class.methodName.
	 * @param i index of method to name in the stack-trace.
	 *  0 is this method, 1 is my direct caller (you), 2 is your caller,
	 *  3 is his caller, and so on back-up the call-stack.
	 * @return
	 */
	public static String nameOf(final int i) {
		final StackTraceElement e = new Throwable().getStackTrace()[i];
		return e.getClassName()+PERIOD+e.getMethodName();
	}

	public static void whinge(final String msg, final Throwable t) {
		teeln(msg);
		teeTrace(t);
		flush();
	}

	public static void initialise(final PrintStream out) {
		Log.out = out;
	}

	public static void stackTrace(final String msg, final Exception ex) {
		if ( out == null )
			return;
		println(msg);
		out.flush();
		ex.printStackTrace(out);
		out.flush();
	}

	// =========================== constructor ================================

	private Log() {} // never used

}
