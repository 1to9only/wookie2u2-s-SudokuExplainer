/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VLAST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;

// ============================================================================
//
//                       THIS CLASS IS NO LONGER USED!
//
// ============================================================================

/**
 * RccFinderAllFast is a fastardised RccFinderAll, for use in AlsChains.
 * <p>
 * I am a cluster____ for speed, but my hovercraft remains steadfastly eelless.
 * This version is about 30 seconds faster for top1465, but is a bitch to groc
 * hence RccFinderAll still exists. Once you understand it you might be able to
 * follow this code, which is overly-complex (too fastardised).
 * <p>
 * If you think you know how to make this faster then shut-up and prove it.
 * I'll snail-mail a gold-star to anyone who makes AlsChain a second faster.
 * I care not HOW you do it (lie). My first question will be HOW?
 *
 * @author Keith Corlett 2023-08-02
 */
final class RccFinderAllFast implements RccFinder {

	/**
	 * The index of the first RCC in {@code rccs} for each {@code alss}.
	 * My index is the alss.index, and I contain the first rccs.index,
	 * so starts and ends map from alss.index to the rccs of this ALS.
	 * starts and ends are populated by find and used by AlsChain only.
	 */
	private final int[] starts = new int[MAX_ALSS];

	/**
	 * The index of the last RCC in {@code rccs} for each {@code alss} + 1.
	 */
	private final int[] ends = new int[MAX_ALSS];

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs, final int oldNumRccs) {
		// ALL vars pre-declared, ANSII-C style, to minimise stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Idx avs[],   bvs[]   // a.vs: indices which maybe v in ALS a
		  , avAll[], bvAll[] // a.vAll: vs plus there common buddies
		  , av,      bv;     // dual-use a/bvs[v] then a/bvAll[v]
		long o0; int o1; // overlap exploded (cells in both ALS a and ALS b)
		long e0; int e1; // eitherVs exploded (cells in either ALS a or ALS b)
		long a0; int a1; // a.idx exploded (cells in ALS a)
		int aMaybes // a.maybes: the potential values of all cells in ALS a
		  , j // index of ALS b in alss
		  // maybes common to both ALSs
		  , vs, va[], vm, vi, v // bitset, array, count, index, value
		  , cands=0 // bitset of first and occassional (~9%) second RC-value
		  , numRccs=0 // number of RCCs found so far.
		;
		final int starts[] = this.starts
		  , ends[] = this.ends
		  , last = numAlss-1; // last alss index
		// the last valid alss index, for a fast STOPPER test in inner ALS loop
		boolean any = false; // any RCC for this pair of ALSs?
		// foreach ALS a
		for ( int i=0; ; ) {
			starts[i] = numRccs; // used in AlsChain only
			avAll = (a=alss[i]).vAll;
			a0 = a.m0;
			a1 = a.m1;
			avs = a.vs;
			aMaybes = a.maybes;
			// foreach ALS b (a full n*n search)
			for ( j=0; ; ) {
				if ( (vs=aMaybes & (b=alss[j]).maybes) > 0 ) {
					vm = VLAST[vs];
					va = VALUESES[vs];
					bvs = b.vs;
					bvAll = b.vAll;
					// if these two ALSs physically overlap
					// (ie one or more cell is in both ALSs)
					if ( ( (o0=b.m0 & a0)
						 | (o1=b.m1 & a1) ) > 0L
					) { // ~23% overlap
						for ( vi=0; ; ) { // 250 million
							v = va[vi];
							av = avs[v];
							bv = bvs[v];
							// if there are no vs in the overlap
							// nb: two ifs are faster
							if ( ( ((e0=av.m0 | bv.m0) & o0)
								 | ((e1=av.m1 | bv.m1) & o1) ) < 1L
							) {
								// if all vs in either ALS see each other
								av = avAll[v];
								bv = bvAll[v];
								if ( (e0 & av.m0 & bv.m0) == e0
								  && (e1 & av.m1 & bv.m1) == e1
								) {
									cands |= VSHFT[v];
									any = true;
								}
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
					} else {
						// ~77% of ALS-pairs do NOT overlap, so dont check for
						// vs in overlap, which is marginally faster overall,
						// despite the cost of having to look for overlap per
						// ALS-pair (atleast its not per value).
						for ( vi=0; ; ) { // 750 million
							v = va[vi];
							av = avs[v];
							bv = bvs[v];
							e0 = av.m0 | bv.m0;
							e1 = av.m1 | bv.m1;
							// if all vs in either ALS see each other
							av = avAll[v];
							bv = bvAll[v];
							if ( (e0 & av.m0 & bv.m0) == e0
							  && (e1 & av.m1 & bv.m1) == e1
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
				}
				if(++j > last) break;
			}
			ends[i] = numRccs; // used in AlsChain only
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
