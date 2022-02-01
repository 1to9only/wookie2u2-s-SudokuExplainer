/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

/**
 * Static methods to help with managing Threads.
 *
 * @author Keith Corlett 2021-07-19
 */
public class Threads {

	public static boolean exists(String targetThreadName) {
		final Thread[] threads = new Thread[Thread.activeCount()];
		int n = Thread.enumerate(threads);
		for ( int i=0; i<n; ++i )
			if ( targetThreadName.equals(threads[i].getName()) )
				return true;
		return false;
	}

	private Threads() {} // Never used

}
