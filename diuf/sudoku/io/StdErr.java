/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import diuf.sudoku.Grid;

/**
 * Standard Error stream static helper methods.
 * @author Keith Corlett 2019 NOV
 */
public final class StdErr {

	/** prints t's stackTrace to stderr and returns false. */
	public static boolean whinge(Throwable t) {
		System.out.flush();
		System.err.flush();
		t.printStackTrace(System.err);
		System.err.flush();
		return false;
	}

	/** prints msg to stderr and returns false. */
	public static boolean whinge(String msg) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		System.err.flush();
		return false;
	}

	/** prints msg and t's stackTrace to stderr and returns false. */
	public static boolean whinge(String msg, Throwable t) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		t.printStackTrace(System.err);
		System.err.flush();
		return false;
	}

	public static void whingeLns(Object... args) {
		for ( Object arg : args )
			System.err.println(arg);
		System.err.flush();
	}

	/** 
	 * carp prints msg and Throwable to stderr and returns false.
	 * <p>
	 * carp prints just the exception and message, not the full stackTrace
	 * (that's whinge); so you carp when you know where the bug is, you just
	 * don't understand it yet.
	 */
	public static boolean carp(String msg, Throwable t) {
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
	 * carp prints just the exception and message, not the full stackTrace
	 * (that's whinge); so you carp when you know where the bug is, you just
	 * don't understand it yet.
	 */
	public static boolean carp(String msg, Throwable t, Grid g) {
		System.out.flush();
		System.err.flush();
		System.err.println(msg);
		System.err.println(t);
		System.err.println(g);
		System.err.flush();
		return false;
	}

	/**
	 * exit when S__t is ____ed: msg defines the s__t, and should also help
	 * narrow down the ____edness.
	 * @param msg 
	 */
	public static void exit(String msg) {
		whinge(msg);
		System.exit(1);
	}

	/**
	 * exit when S__t is ____ed: msg defines the s__t, and Throwable defines
	 * the ____edness.
	 * @param msg where the hell are we (and/or what went wrong)
	 * @param t Throwable cause
	 */
	public static void exit(String msg, Throwable t) {
		whinge(msg, t);
		System.exit(1);
	}

	// never used
	private StdErr(){}

}
