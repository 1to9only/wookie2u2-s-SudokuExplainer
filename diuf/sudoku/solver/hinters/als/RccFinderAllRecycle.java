/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Run;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import java.util.LinkedList;

/**
 * Finds all pairs of ALSs intersecting on Restricted Common Candidate (RCC) by
 * recycling RccFinderForwardOnly expanding A-&gt;B into A-&gt;B and B-&gt;A.
 * <p>
 * {@link AAlsHinter#getRccFinder(diuf.sudoku.Tech)} uses RccFinderAllRecycle
 * only if either ALS_XZ or ALS_Wing is wanted, so I should always have rccs
 * to recycle. If, however, there are no rccs to recycle then I fetch my own in
 * the test-cases, but in the GUI/batch I throw an IllegalStateException.
 * <p>
 * <pre>
 * KRC 2023-08-17 Recycling is MUCH faster than RccFinderAllFast.
 * </pre>
 *
 * @author Keith Corlett 2021-08-17
 */
final class RccFinderAllRecycle implements RccFinder {

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
		try {
			Rcc rcc, cached;
			int i, j // index, index
			  , s = 0 // rcc.source
			  , r = 0 // rcc.related
			  , prevS = 0 // previous s
			  , numRccs = 0; // number of newRccs
			final Rcc[] newRccs = new Rcc[MAX_RCCS];
			// rccs indexed by rcc.source
			final RccQueue[] caches = new RccQueue[MAX_ALSS];
			// In GUI/batch rccs already contains RccFinderForwardOnly results,
			// but in test-cases nothing runs before me, so in order to test
			// this code I must fetch "forwardOnly" RCCs myself, and translate
			// them, which is still faster than RccFinderAll.
			// This NEVER happens in GUI/batch coz getRccFinder instantiates me
			// only if either ALS_XZ or ALS_Wing is wanted.
			final int oldN;
			if ( oldNumRccs == 0 ) {
				if ( Run.isTestCase() ) {
					oldN = new RccFinderForwardOnly().find(alss, numAlss, rccs, 0);
				} else {
					throw new IllegalStateException("No forwardOnly RCCs to recycle!");
				}
			} else {
				oldN = oldNumRccs;
			}
			// foreach RCC in RccFinderForwardOnly results (A->B only)
			// translate A->B into A->B and B->A, but we must delay the output
			// of B->A til we reach B; to put RCCS in increasing order, as per
			// RccFinderAll/Fast, so that everything else stays the same.
			for ( i=0; i<oldN; ++i ) {
				rcc = rccs[i];
				s = rcc.source;
				if ( s != prevS ) {
					// the starts and ends arrays are used by AlsChain only.
					// They are indexed by alsIndex (coincident with alss).
					// They contain the rccIndex, used to lookup related alss.
					// Remember, an RCC is a relationship between two alss.
					ends[prevS] = numRccs;
					starts[s] = numRccs;
					// foreach alsIndex from previousS+1 upto and including s.
					// alsIndexes are contigious, even if they have zero rccs.
					// starts/ends are coindicident with alss (same index).
					for ( j=prevS+1; j<=s; ++j ) {
						// write B->A rccs
						starts[j] = numRccs;
						if ( caches[j] != null ) {
							while ( (cached=caches[j].poll()) != null )
								newRccs[numRccs++] = cached;
							caches[j] = null;
						}
						ends[j] = numRccs;
					}
				}
				// straight copy over the A->B rcc
				newRccs[numRccs++] = rcc;
				// remember the B->A rcc for when we reach the alsIndex of B
				r = rcc.related;
				if ( caches[r] == null )
					caches[r] = new RccQueue();
				// nb: related and source (r, s) swap roles
				caches[r].add(new Rcc(r, s, rcc.cands));
				prevS = s;
			}
			// dont forget to set the end of the last source-alsIndex, dopey!
			ends[prevS] = numRccs;
			for ( i=s+1; i<numAlss; ++i )
				if ( caches[i] != null )
					r = i; // last
			for ( i=s+1,++r; i<r; ++i ) {
				starts[i] = numRccs;
				if ( caches[i] != null ) {
					while ( (cached=caches[i].poll()) != null )
						newRccs[numRccs++] = cached;
					caches[i] = null;
				}
				ends[i] = numRccs;
			}
			// nb: clear remainder is necessary; batch fails without, BFIIK why.
			for ( ; i<MAX_ALSS; ++i )
				starts[i] = ends[i] = 0;
			System.arraycopy(newRccs, 0, rccs, 0, numRccs);
			return numRccs;
		} catch ( ArrayIndexOutOfBoundsException ex ) {
			// presume numRccs > MAX_RCCS-1
			return MAX_RCCS;
		}
	}

	@Override
	public int[] getStarts() {
		return starts;
	}

	@Override
	public int[] getEnds() {
		return ends;
	}

	// extend generic collection to work-around the generic-array problem
	private static class RccQueue extends LinkedList<Rcc> {
		private static final long serialVersionUID = 4905729802944L;
	}

}
