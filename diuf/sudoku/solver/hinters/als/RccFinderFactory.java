/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

/**
 * The RccFinderFactory exposes a static get method that returns an instance
 * of RccFinder that finds RCC's (Restricted Common Candidates) with the given
 * constraints: forwardOnly, and allowOverlaps.
 *
 * The RccFinderFactory exists for performance, because I reckon this Factory,
 * and an RccFinder interface with four implementations is a nicer way to solve
 * the problem than the old static RccFinder.getRccs method, which made da same
 * decision on which actual getRcc's method to call each time it was called.
 * This factory makes that decision ONCE, returning an implementation to handle
 * this use-case. There are downsides: this is architecturally more complex,
 * like moch-gothic siding on a minimalist barn-sit. Get Over It Kevin!
 *
 * @author Keith Corlett 2021-07-29
 */
class RccFinderFactory {

	// We require ONE only instance of each type of RccFinder, which fails to
	// account for the case where a finder-type is never used, but you can't
	// have everything, unless you really want to go really mental and have
	// everything, like Hitler. I'm not (much) like Hitler, so this suffices. 
	// Kick it, not into Russia.
	private static final RccFinder[] FINDERS = {
		  new RccFinderForwardOnlyAllowOverlaps()
		, new RccFinderForwardOnlyNoOverlaps()
		, new RccFinderAllAllowOverlaps()
		, new RccFinderAllNoOverlaps()
	};

	/**
	 * The static get method returns an instance of RccFinder that finds RCC's
	 * (Restricted Common Candidates) with the given forwardOnly and
	 * allowOverlaps constraints. This cluster____ is a performance measure.
	 * <pre>
	 * as at 2021-09-11 10:30
	 * BigWings     : forwardOnly=false, allowOverlaps=false
	 * AlsXz        : forwardOnly=true , allowOverlaps=true
	 * AlsWing      : forwardOnly=true , allowOverlaps=true
	 * AlsChain     : forwardOnly=false, allowOverlaps=true
	 * DeathBlossom : forwardOnly=false, allowOverlaps=false
	 * </pre>
	 *
	 * @param forwardOnly if true the RccFinder does a forward-only search of
	 *  the given ALSs, that is for each ALS we examine only subsequent ALSs;
	 *  but if false the RccFinder does all full search of all possible pairs
	 *  of ALSs.
	 * @param allowOverlaps if true the RccFinder allows the ALSs to physically
	 *  overlap (that is the same cell may be present in both ALSs); but if
	 *  false ALSs pairs that contain a common cell are ignored (filtered out)
	 * @return an instance of RccFinder that searches for the given forwardOnly
	 *  and allowOverlaps constraints.
	 */
	public static RccFinder get(final boolean forwardOnly, final boolean allowOverlaps) {
//// to produce the state-of-play report in method comment
//		System.out.println("forwardOnly="+forwardOnly+", allowOverlaps="+allowOverlaps);
//		new Exception().printStackTrace(System.out); // get caller manually
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
