/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * I exist to help DEBUG the AlsChain class.
 *
 * @author Keith Corlett 2021-10-31
 */
class AlsChainDebug {

	// true changes AlsChainHint.toString, which fails test-cases
	// and also changes the hint HTML
	public static final boolean HINTS = false; // @check false

	// true prints DEBUG messages
	// false is shut your noise
	public static final boolean NOISE = false; // @check false

	static void msg(final String msg) {
		if(!NOISE) return;
		System.out.print("DEBUG: ");
		System.out.println(msg);
	}

	static void array(final String label, int[] a, final int n) {
		if(!NOISE) return;
		System.out.print("DEBUG: ");
		System.out.print(label);
		if ( a.length == 0 ) {
			System.out.println("EMPTY");
			return;
		}
		System.out.print(a[0]);
		for ( int i=1; i<n; ++i ) {
			System.out.print(", ");
			System.out.print(a[i]);
		}
		System.out.println();
	}

	static void array(final String label, boolean[] a, final int numAlss) {
		if(!NOISE) return;
		System.out.print("DEBUG: ");
		System.out.print(label);
		if ( a.length == 0 ) {
			System.out.println("EMPTY");
			return;
		}
		System.out.print(a[0] ? "T" : "F");
		for ( int i=1; i<numAlss; ++i ) {
			System.out.print(", ");
			System.out.print(a[i] ? "T" : "F");
		}
		System.out.println();
	}

}
