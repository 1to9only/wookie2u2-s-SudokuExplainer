/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import static diuf.sudoku.Constants.lieDown;
import diuf.sudoku.Grid;
import javax.swing.SwingUtilities;


/**
 * Standard Error stream static helper methods.
 * @author Keith Corlett 2019 NOV
 */
public final class StdErr {

	/** prints stackTrace of $t to stderr and returns false. */
	public static boolean whinge(final Throwable t) {
		System.out.flush();
		System.err.flush();
		t.printStackTrace(System.err);
		System.err.flush();
		return false;
	}

	/** prints msg to stderr and returns false. */
	public static boolean whinge(final String msg) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		System.err.flush();
		return false;
	}

	/** prints msg and stackTrace of $t to stderr and returns false. */
	public static boolean whinge(final String msg, final Throwable t) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		t.printStackTrace(System.err);
		Throwable c;
		for ( c=t.getCause(); c!=null; c=c.getCause() )
			c.printStackTrace(System.err);
		System.err.flush();
		return false;
	}

	public static boolean whingeLns(final Object... args) {
		for ( Object arg : args )
			System.err.println(arg);
		System.err.flush();
		return false;
	}

	/**
	 * carp prints msg and Throwable to stderr and returns false.
	 * <p>
	 * carp prints just the exception and message, not the full stackTrace; so
	 * you carp when you know where the bug is, you just do not understand it.
	 */
	public static boolean carp(final String msg, final Throwable t) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		System.err.println(t);
		System.err.flush();
		return false;
	}

	/**
	 * carp prints msg, Throwable, and Grid to stderr and returns false.
	 * <p>
	 * Use this one if you have the grid!
	 * <p>
	 * carp prints just the exception and message, not the full stackTrace; so
	 * you carp when you know where the bug is, you just do not understand it.
	 */
	public static boolean carp(final String msg, final Throwable t, final Grid grid) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		System.err.println(t);
		System.err.println(grid);
		System.err.flush();
		return false;
	}

	/**
	 * exit when S__t is ____ed: msg defines the s__t, and should also help
	 * narrow down the ____edness.
	 * @param msg
	 */
	public static void exit(final String msg) {
		lieDown();
		whinge(msg);
		System.exit(1);
	}

	/**
	 * exit when S__t is ____ed: msg defines the s__t, and Throwable defines
	 * the ____edness.
	 * @param msg where the hell are we (and/or what went wrong)
	 * @param t Throwable cause
	 */
	public static void exit(final String msg, final Throwable t) {
		lieDown();
		whinge(msg, t);
		System.exit(1);
	}

	/**
	 * Netbeans interleaves stderr with stdout so print stacktrace to stderr on
	 * a daemon after flushing stdout, a little lie down, and flushing stdout
	 * again. What is the bet that flush is a no-op in PrintStream? Maybe it is
	 * java? I know not. But I cannot put-up with stderr interleaving stdout.
	 * Redirecting both to a single console was bad. There should be two tabs.
	 * I am NOT a GUI programmer, I am a sexual organ!
	*/
	public static void printStackTrace(Exception ex) {
		System.out.flush();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				System.out.flush();
				lieDown();
				ex.printStackTrace(System.err);
				System.err.flush();
			}
		});
	}

	// never used
	private StdErr(){}

}
