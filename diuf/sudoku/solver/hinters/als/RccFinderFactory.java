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
 * <p>
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
	// Kick it, but not into Russia.
	private static final RccFinder[] FINDERS = {
		  new RccFinderForwardOnlyAllowOverlaps()
		, new RccFinderForwardOnlyNoOverlaps()
		, new RccFinderAllAllowOverlaps()
		, new RccFinderAllNoOverlaps()
	};

	/**
	 * The static get method returns an instance of RccFinder that finds
	 * Restricted Common Candidates for this forwardOnly and allowOverlaps.
	 * <p>
	 * This whole inheritance cluster____ is a performance measure. I broke the
	 * RccFinder down along forwardOnly and allowOverlaps so that we don't need
	 * to repeatedly make decisions on how to handle things. Instead we make
	 * those decisions ONCE to return an instance of RccFinder that handles
	 * precisely the current case, for speed. It's quite a lot more code, much
	 * of it repeated repetitiously. Sigh.
	 * <pre>
	 * forwardOnly,allowOverlaps = index = RccFinder = Tech's (asat 2022-01-15)
	 * T,T = 0 = RccFinderForwardOnlyAllowOverlaps = AlsXz, AlsWing
	 * T,F = 1 = RccFinderForwardOnlyNoOverlaps    = none currently
	 * F,T = 2 = RccFinderAllAllowOverlaps         = AlsChain
	 * F,F = 3 = RccFinderAllNoOverlaps            = none currently
	 * BigWings and DeathBlossom do NOT getRccs so there rccFinder is null.
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
		return FINDERS[forwardOnly ? allowOverlaps?0:1 : allowOverlaps?2:3];
	}

	private RccFinderFactory() { } // Never used

}
