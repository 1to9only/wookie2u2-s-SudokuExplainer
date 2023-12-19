/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;

/**
 * RccFinderForwardOnly does a forwards-only search of ALSs for RCCs.
 * <p>
 * AlsXz caches rccs, which AlsWing reads for free. Bargain!
 * <p>
 * FASTARDISED: takes less than half the time of the nieve version!
 * <pre>
 * KRC 2023-10-14 Tried everthing I can think of and this is fastest to date,
 * but certainty is impossible coz JIT variability, heat, and not holding ones
 * toungue correctly. This is STILL the slowest part of SE, and I will be right
 * royally ____ed if I know how to make it any faster!
 *
 * I REALLY WANT TO WATCH THE MACHINE WORK HERE. Why does it take 4,686ms? From
 * billions I would accept that, but 369MILL is not a billion, hence I conclude
 * that I am doing something slow, but what? Frusterpated! Have a geezer at the
 * numbers in AlignedExclusion, and the timings, and tell me I'm wrong.
 *
 * 1,905 ms (1.6%) with the else branch commented out, so the top part is
 * 1,905/4,686*100=40.65% of time to process ~21% of als-pairs. That's ODD!
 * 3,937 ms (5.5%) with the if branch commeinted out, so bottom part is
 * 3,937/4,686*100=84.02% of time, to process ~79% of als-paits, but my maths
 * is clearly broken coz 1,905+3,937=5,842 so I don't know what to think,
 * except the top part is probably the culprit, if any.
 *
 * 4,436 ms (6.1%) with the calculation of either separated from the "no vs in
 * the overlap" if-statement. That is the fastest to date. A small win! I need
 * a win. How to write fast Java code is REALLY hard to get your head around.
 *
 * So I reverted back to the 4,436 ms version and now its 5,343 ms. You can see
 * why I am finding this frustrating. And it's repeatable: 5,404 ms BFIIK!
 * And then it drops again 4,908 ms, but not all the way. That's still half a
 * second slower than the fastest execution of the SAME code. This is MAD!
 *
 * 2023-10-16 3,904 ms (5.5%) which is a WHOLE second better than it was.
 * 2023-10-17 4,090 ms (5.8%) ergo about the same, and seems stable. BFIIK!
 *
 * 2023-10-29 3,871 ms (5.7%) bedded-in. Deoptimised is faster!
 * 2023-11-24 3,660 ms (9.6%)
 * 2023-11-24 3,408 ms (9.1%)
 * </pre>
 *
 * @author Keith Corlett 2021-07-29
 */
public final class RccFinderForwardOnly implements RccFinder
//		, diuf.sudoku.solver.hinters.IReporter
{
//	public static void report() {
//		diuf.sudoku.utils.Log.teeln("RccFinderForwardOnly: COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[13];

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs, final int oldNumRccs) {
		// maybes common to both ALSs, number of Retarded Cockhead Cannibals
		int cands = 0, numRccs = 0;
		// outer loop stops at the penultimate ALS
		final int m = numAlss - 1; // m is n-1
		// are there any cands (hence add a new RCC)
		boolean any = false;
		// foreach als
		int i = 0; // outer (ALSa) index
		do {
			final Als a = alss[i];
			final Idx avs[] = a.vs;
			final Idx avAll[] = a.vAll;
			final long aM0 = a.m0;
			final int aM1 = a.m1;
			final int amaybes = a.maybes;
			// foreach subsequent ALS: a forwards only search
			int j = i + 1; // inner (ALSb) index
			do {
				final Als b = alss[j];
				// if these two ALSs have common maybes
				final int vs = b.maybes & amaybes;
				if ( vs > 0 ) {
					final Idx bvs[] = b.vs;
					final Idx bvAll[] = b.vAll;
					// if these two ALSs overlap
					final long oM0 = b.m0 & aM0;
					final int oM1 = b.m1 & aM1;
					if ( (oM0|oM1) > 0L ) { // ~21% of ALS-pairs overlap
						// foreach maybe common to both ALSs
						for ( int v : VALUESES[vs] ) {
							// vs in the two ALSs
							final long eM0 = avs[v].m0 | bvs[v].m0;
							final int eM1 = avs[v].m1 | bvs[v].m1;
							// if there are no vs in the overlap
							if ( ((eM0&oM0)|(eM1&oM1)) < 1L ) { // 27bit
								// if all vs in either ALS see each other
								if ( (avAll[v].m0 & bvAll[v].m0 & eM0) == eM0
								  && (avAll[v].m1 & bvAll[v].m1 & eM1) == eM1 ) {
									cands |= VSHFT[v];
									any = true;
								}
							}
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, cands);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: " + Log.me() + ": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
							cands = 0;
						}
					} else {
						// no overlap, so no need to test for vs in overlap
						// ~79% of ALS-pairs dont overlap, so this is faster,
						// despite testing if overlap exists.
						// foreach common value
						for ( int v : VALUESES[vs] ) {
							// calculate vs in either ALS
							final long eM0 = avs[v].m0 | bvs[v].m0;
							final int eM1 = avs[v].m1 | bvs[v].m1;
							// if all vs in either ALS see each other
							if ( (avAll[v].m0 & bvAll[v].m0 & eM0) == eM0
							  && (avAll[v].m1 & bvAll[v].m1 & eM1) == eM1 ) {
								cands |= VSHFT[v];
								any = true;
							}
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, cands);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: " + Log.me() + ": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
							cands = 0;
						}
					}
				}
			} while (++j < numAlss);
		} while (++i < m);
		return numRccs;
	}

	@Override
	public boolean forwardOnly() {
		return true;
	}

}
