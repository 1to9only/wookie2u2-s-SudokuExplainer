/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * An implementation of this RccFinder interface finds RCCs (Restricted Common
 * Candidates) in pairs of given ALSs and stores them in the given RCCS array,
 * returning how many. RCC-finding is decomposed to optimise for performance.
 * <pre>
 * RccFinder is now an interface defining find, getStarts, and getEnds.
 * Classes implementing RccFinder:
 * 1. RccFinderForwardOnly A-&gt;B for AlsXz and AlsWing.
 * 2. RccFinderAll A-&gt;B and B-&gt;A for AlsChain:
 *    * ONLY AlsChain uses getStarts and getEnds, hence are only implemented
 *      in RccFinderAll (all three variants, sigh).
 *    * There are three variants of RccFinderAll:
 *      a. RccFinderAll is the original nieve slower simpler version, which is
 *         retained to help techies understand whats going on, which tends to
 *         get lost in the complexities of the later versions.
 *      b. RccFinderAllFast is a fastardised version of RccFinderAll.
 *         * About 10 seconds faster over top1465.
 *      c. RccFinderAllRecycle parses RccFinderForwardOnly results into All.
 *         * Relies on RccFinderForwardOnly results from AlsXz or AlsWing.
 *         * About 12 seconds faster over top1465.
 * There is a getRccFinder factory method in {@link AAlsHinter}:
 * 1. AAlsHinters constructor calls getRccFinder to get the RccFinder that is
 *    suitable for this Tech, which boils down to forwardOnly:
 *    * True means find only A-&gt;B; for AlsXz and AlsWing.
 *    * False means ALL being A-&gt;B and B-&gt;A; for AlsChains.
 * </pre>
 * All RccFinder-classes are called RccFinder* to keep-them-together.
 * <p>
 * This complexity is for performance. Its faster to decide which RccFinder we
 * need ONCE, rather than deciding which getRccs to call every time we getRccs.
 * Rccs are cached, so AlsXz fetches rccs, which are then used in both AlsWing
 * and AlsChian. RccFinderAllRecycle needs cached rccs for speed; it is slower
 * than RccFinderAll if it has to call RccFinderForwardOnly.
 *
 * @author Keith Corlett 2021-07-29.
 */
public interface RccFinder {

	/**
	 * Find RCCs (Restricted Common Candidates). An RCC is the intersection of
	 * two alss which share a common value, and all instances of that value in
	 * both alss see each other (the "restricted" in RCC). An RCC contains the
	 * common value/s. There is always one, and occasionally a second, but never
	 * three. If two alss have three rc-values the Sudoku is rooted. It also
	 * contains the indexes of both ALSs in the alss array. It also has a funny
	 * which variable for AlsChain, which I am sorry to admit, I still do not
	 * understand, despite hours of looking at the code. sigh.
	 * <p>
	 * The only way to calculate rccs is to go-through each possible pair of
	 * alss, which may be numerous: upto like 4K, so this method takes a while.
	 * Get over it. I have done my best to do this as quickly as possible, but
	 * this is still the slowest single operation in Sudoku Explainer. So any
	 * ideas for speeding this s__t up would be most welcome.
	 *
	 * @param alss Almost Locked Sets
	 * @param numAlss number of alss
	 * @param rccs Restricted Common Candidates
	 * @param oldNumRccs the existing number of rccs
	 * @return number of rccs
	 */
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs, final int oldNumRccs);

	/**
	 * Returns the starts array: the RCC-index of the first RCC related to the
	 * source-ALS at this ALS-index.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and
	 * it currently uses RccFinderAll, and all other RccFinders (for which
	 * starts and ends are irrelevant) just return null.
	 *
	 * @return an array if the first RCC-index foreach ALS.
	 */
	public default int[] getStarts() {
		return null;
	}

	/**
	 * Returns the ends array: the last RCC-index for this source-ALS, ergo an
	 * INCLUSIVE boundary for source-ALS.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and it
	 * currently uses RccFinderAll, and all other RccFinders (for which starts
	 * and ends are irrelevant) just return null.
	 *
	 * @return an array of the last RCC-index + 1 foreach ALS.
	 */
	public default int[] getEnds() {
		return null;
	}

	/**
	 * Does this RccFinder subclass do a forwards-only search for RCCs?
	 *
	 * @return false by default
	 */
	public default boolean forwardOnly() {
		return false;
	}

}
