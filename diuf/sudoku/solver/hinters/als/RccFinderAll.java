/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;
import static diuf.sudoku.Idx.seers;
import static diuf.sudoku.Values.VSHFT;

// ============================================================================
//
//                      THIS CLASS IS TWICE SUPERCEEDED!
//                   IT IS RETAINED TO EDIFY IDIOTS, LIKE ME
//                   COZ ITS REPLACEMENTS ARE SO MUCH FASTER
//                   YOU CAN EAR ALL THE SMARTASSES LAUGHTER
//                   BUT SOMEONE HAS TO LAY DOWN THINE PISTE
//                   YOU MAKE IT RHYME! YA DOZEY LEAPIN GIT!
//
// This code is simple enough to follow. Later versions get successively more
// and more complex, hence are hard to follow unless you already know what we
// are trying to do: woods/trees. Hence, this nieve version is still useful.
// Understanding this will help you get your head around the later versions.
// This tells you what the woods are, then we tackle the trees.
//
// ============================================================================

/**
 * Finds all pairs of ALSs intersecting on Restricted Common Candidate (RCC).
 * <p>
 * I'm a Plan-B for {@link RccFinderAllFast}. I'm nieve, which means I'm easier
 * to understand, hence I'm retained despite not being the sharpest tool in the
 * shed. I use Idx-ops and check the overlap for each v, which are both costly.
 * I exist to help you understand RccFinderAllFast, which is fastardised beyond
 * comprehensibility (wait till you see AlsChain!). So just to be clear, try to
 * understand RccFinderAll, then tackle RccFinderAllFast.
 * <p>
 * <pre>
 * KRC 2023-08-01 ~10 seconds slower than RccFinderFast, but still useable.
 * KRC 2023-08-04 now ~2 seconds slower, with tricks: caching, faster index
 * increment and test in one line, one stack-frame, and the new Idx.seers.
 * </pre>
 *
 * @author Keith Corlett 2021-07-29
 */
public final class RccFinderAll implements RccFinder {

//	public static final long[] COUNTS = new long[9];

	/**
	 * The index of the first RCC in {@code rccs} for each {@code alss}. My
	 * index is the alss.index, and I contain the first rccs.index, so starts
	 * and ends map from alss.index to the rccs of this ALS. starts and ends are
	 * populated by find and used by AlsChain only.
	 */
	private final int[] starts = new int[MAX_ALSS];

	/**
	 * The index of the last RCC in {@code rccs} for each {@code alss} + 1. My
	 * index is the alss.index, and I contain the last rccs.index, so that
	 * starts..ends (EXCLUSIVE) maps from alss.index to the rccs of this ALS.
	 * starts and ends are populated by find and used by AlsChain only.
	 */
	private final int[] ends = new int[MAX_ALSS];

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs, final int oldNumRccs) {
		Als a, b;
		Idx aidx, avs[],avAll[], bvs[],bvAll[];
		int j, vs, va[], vm, vi, v, aMaybes;
		final Idx overlap = new Idx()
				, either = new Idx();
		int numRccs = 0; // number RCCs found so far.
		int cands = 0;
		final int last = numAlss - 1;
		boolean any = false; // any RC-value/s found
		// foreach ALS a
		for (int i=0;;) {
			// starts and ends are used in AlsChain only
			starts[i] = numRccs;
			a = alss[i];
			avs = a.vs;
			avAll = a.vAll;
			aidx = a.idx;
			aMaybes = a.maybes;
			// foreach ALS b (a full n*n search)
			for(j=0;;) {
				// if there are any maybes common to ALS a and ALS b
				if ( (vs=aMaybes & (b=alss[j]).maybes) > 0 ) {
					va = VALUESES[vs];
					vm = va.length - 1;
					overlap.setAnd(aidx, b.idx);
					bvs = b.vs;
					bvAll = b.vAll;
					for ( vi=0; ; ) {
						// if all vs in either ALS see each other
						if ( seers(bvAll[v=va[vi]], avAll[v], either.setOr(bvs[v], avs[v]))
						  // and there are no vs in the overlap
						  && overlap.disjunct(either)
						) {
							cands |= VSHFT[v];
							any = true;
						}
						if(++vi > vm) break;
					}
					if ( any ) {
						any = false;
						rccs[numRccs] = new Rcc(i, j, cands);
						if ( ++numRccs == MAX_RCCS ) {
							Log.teeln("WARN: "+Log.me()+": MAX_RCCS reached!");
							return numRccs; // no crash!
						}
						cands = 0;
					}
				}
				if(++j > last) break;
			}
			// starts and ends are used in AlsChain only
			ends[i] = numRccs;
			if(++i > last) break;
		}
		return numRccs;
	}

	@Override
	public int[] getStarts() {
		return starts;
	}

	@Override
	public int[] getEnds() {
		return ends;
	}

}
