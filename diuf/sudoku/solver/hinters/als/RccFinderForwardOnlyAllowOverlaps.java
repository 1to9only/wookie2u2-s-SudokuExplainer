/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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
 * Used by AlsXz and AlsWing.
 * <p>
 * AlsChain NEVER does a "forwardOnly" search, so I extend RccFinderAbstract
 * whose getStarts and getEnds methods return null, which works only because
 * they are never called.
 *
 * @author Keith Corlett 2021-07-29
 */
final class RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract {

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, reducing stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Idx[] avs // a.vs: indices of cells which maybe v in ALS a
			, bvs // b.vs: indices of cells which maybe v in ALS b
		    , avAll // a.vAll: indices of cells which maybe v in ALS a + buds
		    , bvAll; //b.vAll: indices of cells which maybe v in ALS b + buds
		Idx aI // a.idx: indices of cells in ALS a
		  , bI // b.idx: indices of cells in ALS b
		  , av // a.vs[v]: indices of cells in ALS a which maybe v
		  , bv // b.vs[v]: indices if cells in ALS b which maybe v
		  , avA // a.vAll[v]: indices of cells which maybe v in ALS a + buddies
		  , bvA;// b.vAll[v]: indices of cells which maybe v in ALS b + buddies
		int[] vs; // VALUESES[cmnMaybes]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index in the alss array of ALS b
		  , commonMaybes // candidates shared by ALS a and ALS b (inc v1/2)
		  , vi,vLast,v // index into vs, vs.length-1, value at vs[vi]
		  , ol0,ol1,ol2 // overlap exploded
		  , ev0,ev1,ev2 // eitherVs exploded
		  , i // alss-index for the outer 'a' loop
		  , v1, v2 // the first and occassional second RC-value
		  ;
		boolean any = false; // any RCC for this pair of ALSs?
		int numRccs = 0; // number RCC's found so far.
		// the last valid alss index, for a fast STOPPER test in inner ALS loop
		final int last = numAlss - 1
				, penultimate = last - 1;
		// foreach ALS a (except the last)
		for ( i = 0
			; //NO_STOPPER
			; //NO_INCREMENT
		) {
			// foreach subsequent ALS b (forwardOnly search)
			for (
				avAll = (a=alss[i]).vAll,
				avs = a.vs,
				aI = a.idx,
				aMaybes = a.maybes,
				j = i + 1 // FORWARD_ONLY
				; //NO_STOPPER
				; //NO_INCREMENT
			) {
				// if there are any maybes common to ALS a and ALS b.
				if ( (commonMaybes=(aMaybes & (b=alss[j]).maybes)) != 0 ) {
// this is faster but misses hints, including the one in AlsWingTest! I don't
// really understand WHY it ,misses coz the below logic adds-up according to my
// current, evidently flawed, understanding of AlsWings.
				// if there are 2+ maybes common to ALS a and ALS b.
				// An Rcc with just one common cand is useless. We need an
				// RC-value and another common candidate, restricted or not.
				// java bug: do NOT inline this statement
//				commonMaybes = (aMaybes & (b=alss[j]).maybes);
//				if ( VSIZE[commonMaybes] > 1 ) {
					// if these two ALSs do NOT overlap (ol* reused, if any).
					// MAGNITUDE: There are about 250 million ALS pairs with a
					// billion cmnMaybes, and about 20% of pairs overlap.
					if ( ( (ol0=aI.a0 & (bI=b.idx).a0)
						 | (ol1=aI.a1 & bI.a1)
						 | (ol2=aI.a2 & bI.a2) ) == 0
					) {
//						++COUNTS[0]; // 191,662,530
						// there is no overlap, so there is no point checking
						// that there are no v's in the overlap, as was done
						// previously. About 80% of pairs do not overlap.
						for (
							v1 = v2 = 0,
							bvs = b.vs,
							bvAll = b.vAll,
							vs = VALUESES[commonMaybes],
							vLast = vs.length - 1,
							vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if all v's in either ALS see each other
							if ( ((ev0=(av=avs[v=vs[vi]]).a0 | (bv=bvs[v]).a0) & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == ev0
							  && ((ev1=av.a1 | bv.a1) & avA.a1 & bvA.a1) == ev1
							  && ((ev2=av.a2 | bv.a2) & avA.a2 & bvA.a2) == ev2
							) {
								if ( any ) {
									v2 = v;
									break; // there can't be more than 2 RC-values
								} else {
									v1 = v;
									any = true;
								}
							}
							if ( ++vi > vLast ) {
								break;
							}
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, v1, v2);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
						}
					} else {
//						++COUNTS[1]; // 61,661,404
						// these two ALSs overlap. For algorithmic correctness
						// we must check that there are no v's in the overlap,
						// which costs time, now minimised by checking only in
						// those 20% of cases that actually overlap. sigh.
						for (
							v1 = v2 = 0,
							bvAll = b.vAll,
							bvs = b.vs,
							vs = VALUESES[commonMaybes],
							vLast = vs.length - 1,
							vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if there are no v's in the overlap
							if ( ( ((ev0=(av=avs[v=vs[vi]]).a0 | (bv=bvs[v]).a0) & ol0)
								 | ((ev1=av.a1 | bv.a1) & ol1)
								 | ((ev2=av.a2 | bv.a2) & ol2) ) == 0
							  // and all v's in both ALSs see each other
							  && (ev0 & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == ev0
							  && (ev1 & avA.a1 & bvA.a1) == ev1
							  && (ev2 & avA.a2 & bvA.a2) == ev2
							) {
								if ( any ) {
									v2 = v;
									break; // there can't be more than 2 RC-values
								} else {
									v1 = v;
									any = true;
								}
							}
							if ( ++vi > vLast ) {
								break;
							}
						}
						if ( any ) {
							any = false;
							rccs[numRccs] = new Rcc(i, j, v1, v2);
							if ( ++numRccs == MAX_RCCS ) {
								Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
								return numRccs; // no crash!
							}
						}
					}
				}
				if ( ++j > last ) {
					break;
				}
			}
			if ( ++i > penultimate ) {
				break;
			}
		}
		return numRccs;
	}

}
