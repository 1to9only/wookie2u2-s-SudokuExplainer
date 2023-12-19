/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import java.awt.Toolkit;
import static diuf.sudoku.utils.MyMath.powerOf2;

/**
 * Exhumed constants from Grid so Tech need not back-reference to Grid. Sigh.
 *
 * @author Keith Corlett 2022-01-09
 */
public final class Constants {

	/** Shorthand for true. */
	public static final boolean T = true;
	/** Shorthand for false. */
	public static final boolean F = false;

	/**
	 * DUMMY is a self-documenting value passed to dummy parameters, which are
	 * not used, and exist to differentiate the method from overloaded methods
	 * that would otherwise conflict. By convention the more complex method
	 * has the dummy parameter. For example Pots methods with a dummy parameter
	 * accept an int "cands" (bitset of combined candidate values), where the
	 * "standard" equivalent takes an int "value" (plain old candidate value).
	 */
	public static final boolean DUMMY = false;

	private static final Toolkit TOOLKIT = java.awt.Toolkit.getDefaultToolkit();
	public static void beep() {
		TOOLKIT.beep();
	}

	/** GEM/XColoring/Turbo setPots in 0=GREEN, 1=BLUE, or -1=NONE. */
	public static final int GREEN = 0; // do NOT rename these, you idiot! If that means your code doesnt fit on one line then (a) tough, (b) suck it, (c) use two lines ya moron, (d) all of the above, (e) nuclear arsenal, (f) all of the above, except the above, obviously. Thats just overkill! (g) A Gannet! Sans Cheese! (and pickles)! (f) Q: Now what does "f" stand for? A: What rhymes with fit? (g) Netbeans has a bug that will eat whole random methods and/or blocks, because life is a bitch, and Netbeans authors are morons, just like the rest of us. Sigh. You do better!
	public static final int BLUE = 1;

	/**
	 * Returns a new StringBuilder of the given capacity.
	 *
	 * @param initialCapacity the starting size. It can grow from there
	 * @return a new StringBuilder(initialCapacity)
	 */
	public static StringBuilder SB(final int initialCapacity) {
		return diuf.sudoku.utils.MyStrings.SB(initialCapacity);
	}

	public static int NAP_TIME = 10; // millis

	/**
	 * A Microsleep: lieDown delays the current thread for a standard short
	 * period of time, hopefully giving the other stream (stdout) some time.
	 */
	public static void lieDown() {
		lieDown(NAP_TIME); // milliseconds
	}
	/**
	 * Take a little lieDown because catching InterruptedException alloys me!
	 *
	 * @param millis number of milliseconds to delay execution of the current
	 *  thread.
	 */
	public static void lieDown(final int millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException eaten) {
			// Do nothing
		}
	}

	private Constants() { } // Never used

}
