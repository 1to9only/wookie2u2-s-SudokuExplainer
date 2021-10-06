/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * The RccFinderFactory exposes a static get method that returns an instance
 * of RccFinder that finds RCC's (Restricted Common Candidates) with the given
 * constraints: forwardOnly, and allowOverlaps.
 *
 * Please note that the RccFinderFactory exists for performance, because I
 * reckon this Factory, and an RccFinder interface with four implementations
 * is a nicer way of solving the problem than the old static RccFinder.getRccs
 * method, which made the same decision on which actual getRcc's method to call
 * each time it was called. This factory makes that decision ONCE, returning an
 * implementation that handles this use-case.
 *
 * @author Keith Corlett 2021-07-29
 */
class RccFinderFactory {

	private static final RccFinder[] FINDERS = {
		  new RccFinderForwardOnlyAllowOverlaps()
		, new RccFinderForwardOnlyNoOverlaps()
		, new RccFinderAllAllowOverlaps()
		, new RccFinderAllNoOverlaps()
	};

	/**
	 * The static get method that returns an instance of RccFinder that finds
	 * RCC's (Restricted Common Candidates) with the given constraints:
	 * forwardOnly, and allowOverlaps.
	 *
	 * @param forwardOnly if true the RccFinder does a forward-only search of
	 *  the given alss, that is for each ALS we examine only subsequent alss;
	 *  but if false the RccFinder does all full search of all possible pairs
	 *  of alss.
	 * @param allowOverlaps if true the RccFinder allows the ALSs to physically
	 *  overlap (that is the same cell may be present in both ALSs); but if
	 *  false ALSs pairs that contain a common cell are ignored (filtered out)
	 * @return a new instance of RccFinder that is optimised for searching for
	 *  the given RCC-search configuration.
	 */
	public static RccFinder get(final boolean forwardOnly, final boolean allowOverlaps) {
		// these methods where split-up for speed
		if ( forwardOnly ) {
			if ( allowOverlaps ) { // used by AlsXz and AlsWing
				return FINDERS[0];
			} else { // this is never used, so is untested!
				return FINDERS[1];
			}
		} else { // the only caller is AlsChain which doesn't allowOverlaps
			if ( allowOverlaps ) { // used by AlsChain
				return FINDERS[2];
			} else { // used by BigWings and DeathBlossom
				return FINDERS[3];
			}
		}
	}

	private RccFinderFactory() { } // Never used

}
