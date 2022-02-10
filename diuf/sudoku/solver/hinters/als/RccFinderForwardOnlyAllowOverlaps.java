/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;

/**
 * RccFinderForwardOnlyAllowOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = true (ie foreach ALS search subsequent ALSs only), and
 * <li>allowOverlaps = true (ie find RCC's in ALS-pairs that overlap); while
 *  the ALS pairs may overlap the RC-values cannot be in that overlap (if any),
 *  which is the "Restricted" in Restricted Common Candidate, so that each
 *  RC-value may be in one ALS or the other (not both). It's worth belabouring
 *  this point because calculating the absence of each potential RC-value from
 *  the overlap is much harder than calculating merely "do these ALSs overlap",
 *  so it pays to avoid it if you don't need overlapping ALSs.
 * </ul>
 * <p>
 * Used by AlsXz and AlsWing, so AlsXz primes the cache that AlsWing reads, at
 * no extra charge.
 * <p>
 * AlsChain NEVER does a "forwardOnly" search (it needs B->A as well as A->B)
 * hence I extend RccFinderAbstract whose getStarts and getEnds methods return
 * null, which works only because they are never called.
 *
 * @author Keith Corlett 2021-07-29
 */
final class RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract {

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, reducing stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Idx[] avs, bvs // a/b.vs: indices which maybe v in ALS a/b
		    , avAll, bvAll // a/b.vAll: indices which maybe v in ALS a/b + buds
			;
		Idx av, bv // a/b.vs[v]: indices in ALS a/b which maybe v
		  , avA, bvA // a/b.vAll[v]: indices which maybe v in ALS a/b + buddies
		  ;
		int[] va; // VALUESES[vs]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index of ALS 'b' in alss (the inner loop)
		  , vs // bitset of maybes common to ALS a and ALS b (inc v1/2)
		  , vi,vn,v // index into va, va.length, value at vs[vi]
		  , ai0,ai1,ai2 // a.idx exploded
		  , ol0,ol1,ol2 // overlap exploded
		  , ev0,ev1,ev2 // eitherVs exploded
		  ;
//++COUNTS[0]; // 8164
		int numRccs=0 // number RCC's found so far.
		  , i=0 // index of ALS 'a' in alss (the outer loop)
		  , v1=0,v2=0 // the first and occassional second RC-value
		  ;
		boolean any = false; // any RC-Value/s for this pair of ALSs?
		// the last valid alss index, for a fast STOPPER test in inner ALS loop
		final int last = numAlss - 1
				, penultimate = numAlss - 2;
		// foreach ALS a (except the last)
		for (;;) {
//++COUNTS[1]; // 1,499,408
			// foreach subsequent ALS b (a FORWARD_ONLY search).
			// dereference all 'a' attributes ONCE for speed; and it's faster
			// to do so in the for-loop-initialiser, which I don't understand.
			for ( avAll = (a=alss[i]).vAll
				, avs = a.vs
				, ai0=a.i0, ai1=a.i1, ai2=a.i2 // 750ms with that axe Eugeene
				, aMaybes = a.maybes
				, j = i + 1 // FORWARD_ONLY
				; //NO_STOPPER
				; //NO_INCREMENT
			) {
//++COUNTS[2]; // 142,661,283
				// if there are any maybes common to ALS a and ALS b.
				if ( (vs=(alss[j].maybes) & aMaybes) != 0 ) {
// faster but misses hints, including the one in AlsWingTest. I no understand
// WHY it misses. It adds-up according to my understanding of AlsWings.
				// if there are 2+ maybes common to ALS a and ALS b.
				// An Rcc with just one common cand is useless. We need an
				// RC-value and another common candidate, restricted or not.
				// java8 bug: do NOT inline this statement
//				commonMaybes = (aMaybes & (b=alss[j]).maybes);
//				if ( VSIZE[commonMaybes] > 1 ) {
					// if these two ALSs do NOT overlap (ol* reused, if any).
					// MAGNITUDE: There are about 250 million ALS pairs with
					// a billion cmnMaybes, and about 25% of pairs overlap.
					// nb: faster to cache ol* than recalculate 25% of time,
					// which is below my 33% rule of thumb. IDKFA. Sigh.
//++COUNTS[3]; // 140,303,168
					if ( ( (ol0=(b=alss[j]).i0 & ai0)
						 | (ol1=b.i1 & ai1)
						 | (ol2=b.i2 & ai2) ) == 0
					) {
//++COUNTS[4]; // 110,939,713
						//++COUNTS[0]; // 191,662,530
						// there is no overlap, so there is no point checking
						// that there are no v's in the overlap, as was done
						// previously. About 75% of pairs do not overlap.
						for ( bvs = b.vs
							, bvAll = b.vAll
							, va = VALUESES[vs]
							, vn = va.length
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
//++COUNTS[5]; // 300,377,580
							// if all v's in either ALS see each other
							if ( ((avA=avAll[v=va[vi]]).a0 & (bvA=bvAll[v]).a0
									& (ev0=(av=avs[v]).a0 | (bv=bvs[v]).a0)) == ev0
							  && (avA.a1 & bvA.a1 & (ev1=av.a1 | bv.a1)) == ev1
							  && (avA.a2 & bvA.a2 & (ev2=av.a2 | bv.a2)) == ev2 )
								if ( any ) {
									v2 = v;
									break;
								} else {
									v1 = v;
									any = true;
								}
							if ( ++vi == vn )
								break;
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, v1, v2);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
							v2 = 0;
						}
					} else {
						//++COUNTS[1]; // 61,661,404
						// these two ALSs overlap. For algorithmic correctness
						// we must check that there are no v's in the overlap,
						// which costs time, now minimised by checking only in
						// those 25% of cases that overlap at all. sigh.
//++COUNTS[6]; // 29,363,455
						for ( bvAll = b.vAll
							, bvs = b.vs
							, va = VALUESES[vs]
							, vn = va.length
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
//++COUNTS[7]; // 107,662,650
							// if there are no v's in the overlap
							if ( ( ((ev0=(av=avs[v=va[vi]]).a0 | (bv=bvs[v]).a0) & ol0)
								 | ((ev1=av.a1 | bv.a1) & ol1)
								 | ((ev2=av.a2 | bv.a2) & ol2) ) == 0
							  // and all v's in both ALSs see each other
							  && ((avA=avAll[v]).a0 & (bvA=bvAll[v]).a0 & ev0) == ev0
							  && (avA.a1 & bvA.a1 & ev1) == ev1
							  && (avA.a2 & bvA.a2 & ev2) == ev2 )
								if ( any ) {
									v2 = v;
									break;
								} else {
									v1 = v;
									any = true;
								}
							if ( ++vi == vn )
								break;
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, v1, v2);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
							v2 = 0;
						}
					}
				}
				if ( ++j > last )
					break;
			}
			if ( ++i > penultimate )
				break;
		}
		return numRccs;
	}

}
