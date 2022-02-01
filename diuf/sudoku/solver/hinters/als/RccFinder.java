/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * An implementation of this RccFinder interface finds RCC's (Restricted Common
 * Candidates) in pairs of given alss and stores them in the given rccs array,
 * returning how many. RCC-finding is decomposed to optimise for performance.
 * <pre>
 * RccFinder is now an interface defining find, getStartIndexes, getEndIndexes.
 * There are two abstract classes (partial implementations):
 * 1. RccFinderAbstract is the "base", defining no-op getStart/EndIndexes.
 * 2. RccFinderAbstractIndexed extends RccFinderAbstract to define the starts
 *    and ends arrays, and overrides getStart/EndIndexes to return them.
 * There are four implementations:
 * 1. RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract
 * 2. RccFinderForwardOnlyNoOverlaps    extends RccFinderAbstract
 * 3. RccFinderAllAllowOverlaps			extends RccFinderAbstractIndexed
 * 4. RccFinderAllNoOverlaps			extends RccFinderAbstractIndexed
 * There is one factory:
 * 1. RccFinderFactory exposes a static get method that AAlsHinter uses to get
 *    the RccFinder suitable for this Sudoku solving technique, which boils
 *    down to the two booleans: forwardOnly and allowOverlaps.
 * </pre>
 * All RccFinder-classes are called RccFinder* to keep-em-together.
 * <p>
 * This cluster____ of complexity is a performance measure. I hope it's faster
 * to work-out which RccFinder we need ONCE, rather than deciding which getRccs
 * to call every time we getRccs, but rccs are cached, so I doubt this'll make
 * any difference, but it's arguably cleaner, so I've done it anyway. All this
 * decomposition allows me to write code that's highly specific to each flavour
 * of RCC-finding, so that each is as fast as possible at it's specific job.
 * It's certainly more classes, so the whole thing looks terribly complex to a
 * newbie, but it's simple enough really, but we're screwed if we ever need to
 * change how RCCs are found. sigh.
 *
 * @author Keith Corlett 2021-07-29.
 */
public interface RccFinder {

	/**
	 * Search the given alss for Restricted Common Candidates, storing each
	 * in the given rccs array and return how many.
	 * <p>
	 * Finding RCC's is inherently a heavy process at worst-case O(numAlss *
	 * numAlss), so each implementation is as fast as possible, which is still
	 * too slow.
	 * <p>
	 * If the size of the passed rccs is exceeded then a WARN: is printed to
	 * both the Log and to stdout. Please ignore these messages from generate,
	 * they're pretty much unavoidable there, because I don't want to tie-up
	 * more memory in a larger-than-required array just to suit generate, but
	 * if generate is your focus, and you have s__tloads of RAM then you might
	 * want to double the size of this array again, and if so then also consult
	 * java -help (or java /?) to give SE as much RAM as you possibly can; and
	 * it might be an idea to do it anyway, because SE is RAM hungry. sigh.
	 *
	 * @param alss the ALSs (Almost Locked Sets) to search, currently 512
	 * @param numAlss the number of alss actually in the alss array
	 * @param rccs the RCCs array to be repopulated, currently 8192
	 * @return the number of RCCs
	 */
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs);

	/**
	 * Returns the starts array: the RCC-index of the first RCC related to the
	 * source-ALS at this ALS-index.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and
	 * it currently uses RccFinderAllAllowOverlaps, and all other RccFinder's
	 * (for which starts and ends are irrelevant) just return null.
	 *
	 * @return an array if the first RCC-index foreach ALS.
	 */
	public int[] getStarts();

	/**
	 * Returns the ends array: the last RCC-index for this source-ALS, ergo an 
	 * INCLUSIVE boundary for source-ALS.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and it
	 * currently uses RccFinderAllAllowOverlaps, and all other RccFinder's (for
	 * which starts and ends are irrelevant) just return null.
	 *
	 * @return an array of the last RCC-index + 1 foreach ALS.
	 */
	public int[] getEnds();

}
