/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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

//	// AlsXz: getRccsForwardOnlyAllowOverlaps size = 7
//	// AlsChain: getRccsAllAllowOverlaps size = 2
//	public static final long[] COUNTS = new long[2];

	/**
	 * Find RCCs (Restricted Common Candidates). An RCC is the intersection of
	 * two alss which share a common value, and all instances of that value in
	 * both alss see each other (the "restricted" in RCC). An RCC contains the
	 * common value/s. There's always one, and occasionally a second, but never
	 * three. If two alss have three rc-values the Sudoku is rooted. It also
	 * contains the indexes of both ALSs in the alss array. It also has a funny
	 * which variable for AlsChain, which I am sorry to admit, I still do not
	 * understand, despite hours of looking at the code. sigh.
	 * <p>
	 * The only way to calculate rccs is to go-through each possible pair of
	 * alss, which may be numerous: upto like 4K, so this method takes a while.
	 * Get over it. I've done my best to do this as quickly as possible, but
	 * this is still the slowest single operation in Sudoku Explainer. So any
	 * ideas for speeding this s__t up would be most welcome.
	 *
	 * @param alss Almost Locked Sets
	 * @param numAlss number of alss
	 * @param rccs Restricted Common Candidates
	 * @return number of rccs
	 */
	@Override
	public abstract int find(Als[] alss, int numAlss, Rcc[] rccs);

	@Override
	public int[] getStarts() {
		return null;
	}

	@Override
	public int[] getEnds() {
		return null;
	}

}
