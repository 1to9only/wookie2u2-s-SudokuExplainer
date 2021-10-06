/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * RccFinderAbstract (the abstract RccFinder) is a partial implementation of
 * the RccFinder interface, to make implementing RccFinder easier.
 * <p>
 * It provides no-op implementations of getStartIndexes() and getEndIndexes()
 * which just return null. Each of my subclasses implements find itself.
 *
 * @author Keith Corlett 2021-07-29
 */
public abstract class RccFinderAbstract implements RccFinder {

	// AlsXz: getRccsForwardOnlyAllowOverlaps size = 7
	// AlsChain: getRccsAllAllowOverlaps size = 2
//	/** Metricate getRccs */
//	public static final long[] COUNTS = new long[2];

	@Override
	public abstract int find(Als[] alss, int numAlss, Rcc[] rccs);

	@Override
	public int[] getStartIndexes() {
		return null;
	}

	@Override
	public int[] getEndIndexes() {
		return null;
	}
	
}
