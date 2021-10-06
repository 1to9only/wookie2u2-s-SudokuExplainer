/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * An implementation of RccFinder finds RCC's (Restricted Common Candidates)
 * in pairs of given alss and stores them in the given rccs array, returning
 * how many.
 * <p>
 * RccFinder is now an interface defining find, getStartIndexes, getEndIndexes.
 * There's two abstract classes:
 * RccFinderAbstract is the "base", defining no-op getStart/EndIndexes.
 * RccFinderAbstractIndexed extends RccFinderAbstract to define the starts and
 * ends arrays, and overrides getStart/EndIndexes to return them.
 * <pre>
 * There are four implementations:
 * 1. RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract
 * 2. RccFinderForwardOnlyNoOverlaps    extends RccFinderAbstract
 * 3. RccFinderAllAllowOverlaps			extends RccFinderAbstractIndexed
 * 4. RccFinderAllNoOverlaps			extends RccFinderAbstractIndexed
 * </pre>
 * And finally the RccFinderFactory exposes a static get method that AAlsHinter
 * uses to get the RccFinder suitable for this Sudoku solving technique, which
 * boils-down-to the two booleans: forwardOnly and allowOverlaps.
 * <p>
 * All RccFinder-classes are called RccFinder* to keep-em-together.
 * <p>
 * This cluster____ of complexity is a performance measure. I hope it's a bit
 * faster to work-out which RccFinder we need ONCE, rather than working-out
 * which actual getRcc's method to execute every time we getRccs, but rccs are
 * cached, so I doubt this'll make much difference, but it's arguably cleaner,
 * so I'm doing it anyway. It's certainly more classes, and now the whole thing
 * may feel overwhelmingly complex to newbs, but it's simple enough really.
 * sigh.
 *
 * @author Keith Corlett 2021-07-29.
 */
public interface RccFinder {

	/**
	 * Search the given alss for Restricted Common Candidates, storing each
	 * in the given rccs array and return how many.
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
	 * Returns the starts array: the RCC-index of the first RCC for the ALS at
	 * each ALS-index.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and
	 * it currently uses RccFinderAllAllowOverlaps, and all other RccFinder's
	 * (for which starts and ends are irrelevant) just return null.
	 *
	 * @return an array if the first RCC-index foreach ALS.
	 */
	public int[] getStartIndexes();

	/**
	 * Returns the ends array: one more than the last RCC-index for this ALS,
	 * ergo an exclusive boundary line.
	 * <p>
	 * Note that currently only AlsChain uses the start and end indexes, and
	 * it currently uses RccFinderAllAllowOverlaps, and all other RccFinder's
	 * (for which starts and ends are irrelevant) just return null.
	 *
	 * @return an array of the last RCC-index + 1 foreach ALS.
	 */
	public int[] getEndIndexes();

}
