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
 * RccFinderAllAllowOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = false (ie search "All" ALS pairs), and
 * <li>allowOverlaps = true (ie find RCC's in ALS-pairs that overlap).
 * </ul>
 * <p>
 * I'm used by AlsChain, which uses the 'starts' and 'ends' indexes.
 * I extend RccFinderAbstractIndexed for it's starts and ends fields,
 * and the getter methods which return those arrays (not null).
 * <p>
 * I'm a cluster____ for speed, but my hovercraft remains eelless.
 * <pre>
 * 2022-01-14 I'm attempting to speed this up a bit just by cleaning up.
 * 15:27:19 7,661,210,000 4778 1,603,434 1873 4,090,341 ALS_Chain
 * 15:54:45 7,656,639,300 4778 1,602,477 1873 4,087,901 ALS_Chain
 * </pre>
 *
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderAllAllowOverlaps extends RccFinderAbstractIndexed {

//	// DEBUG: starts and ends for each source-ALS
//	public static String startsAndEnds(final int[] starts, final int[] ends
//			, final int numAlss, final String sep) {
//		final StringBuilder sb = new StringBuilder(numAlss<<4); // * 16
//		for ( int i=0; i<numAlss; ++i )
//			sb.append(i).append("=").append(starts[i]).append("..").append(ends[i]).append(sep);
//		return sb.toString();
//	}

	public RccFinderAllAllowOverlaps() {
	}

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Idx[] avs, bvs // a/b.vs: indices which maybe v in ALS a/b
		    , avAll, bvAll // a/b.vAll: indices which maybe v in ALS a/b + buds
		    ;
		Idx av, bv // a/b.vs[v]: indices in ALS a/b which maybe v
		  , avA, bvA // a/b.vAll[v]: indices which maybe v in ALS a/b + buddies
		  ;
		int[] va; // VALUESES[vs]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: the potential values of all cells in ALS a
		  , j // the index in the alss array of ALS b
		  , vs // values: bitset of maybes common to ALS a and ALS b
		  , vi,vl,v // va index, va.length-1, value at va[vi]
		  , ai0,ai1,ai2 // a.idx exploded
		  , ol0,ol1,ol2 // overlap exploded
		  , ev0,ev1,ev2 // eitherVs exploded
		  ;
		boolean any = false; // any RCC for this pair of ALSs?
		int numRccs=0 // number RCC's found so far.
		  , i=0 // index of ALS 'a' in alss
		  , v1=0, v2=0 // first and occassional (~9%) second RC-value
		  ;
		// the last valid alss index, for a fast STOPPER test in inner ALS loop
		final int last = numAlss - 1;
//		// harder to debug without clearing rccs first, which is otherwise
//		// unecessary, as long as everything works properly. sigh.
//		if ( ASSERTS_ENABLED ) {
//			java.util.Arrays.fill(rccs, null);
//		}
		// foreach ALS a
		for (;;) {
			// foreach ALS b (a full n*n search)
			// starts and ends are used in AlsChain only
			for ( starts[i] = numRccs
				, avAll = (a=alss[i]).vAll
				, avs = a.vs
				, ai0=a.i0, ai1=a.i1, ai2=a.i2
				, aMaybes = a.maybes
				, j = 0 // FIRST
				; //NO_STOPPER
				; //NO_INCREMENT
			) {
				// if there are any maybes common to ALS a and ALS b
				if ( (vs=(aMaybes & alss[j].maybes)) != 0
//				  // and HACK: In AlsChain (my only use) each ALS pair needs 2
//				  // common maybes to be a branch (but only one to be a leaf).
//				  // PRE: 1873 in 7,656,639,300 ALS_Chain
//				  // PST: 1721 in 7,144,536,200 ALS_Chain
//				  && VSIZE[vs] > 1 // static import Values.VSIZE
// unnecessary because the no v's in overlap rule excludes j==i
//				  // and j and i are not the same ALS index
//				  && j != i
				) {
					// if these two ALSs do NOT overlap.
					// MAGNITUDE: There are about 253 million ALS pairs with a
					// billion cmnMaybes, and about 25% of pairs overlap.
					// The ol* vars are reused only in the 25% overlaps, so it
					// might be faster to not cache ol*?
					if ( ( (ol0=(b=alss[j]).i0 & ai0)
						 | (ol1=b.i1 & ai1)
						 | (ol2=b.i2 & ai2) ) == 0
					) {
//						++COUNTS[0]; // 191,662,530
						// there is no phyical overlap, so no point checking
						// there's no v's in the overlap, as was always done
						// previously. About 75% of pairs do not overlap.
						for ( bvs = b.vs
							, bvAll = b.vAll
							, va = VALUESES[vs]
							, vl = va.length - 1
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if all v's in either ALS are siblings
							if ( ((ev0=(av=avs[v=va[vi]]).a0 | (bv=bvs[v]).a0)
									& (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == ev0
							  && ((ev1=av.a1 | bv.a1) & avA.a1 & bvA.a1) == ev1
							  && ((ev2=av.a2 | bv.a2) & avA.a2 & bvA.a2) == ev2 )
								if ( any ) {
									v2 = v;
									break;
								} else {
									v1 = v;
									any = true;
								}
							if ( ++vi > vl )
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
//						++COUNTS[1]; // 61,661,404
						// these two ALSs overlap. For algorithmic correctness
						// we must check that there are no v's in the overlap,
						// which costs time, now minimised by checking only in
						// those 25% of cases that actually overlap; at da cost
						// of the overlap test, which is faster, but not free.
						for ( bvs = b.vs
							, bvAll = b.vAll
							, va = VALUESES[vs]
							, vl = va.length - 1
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if there are no v's in the overlap
							if ( ( ((ev0=(av=avs[v=va[vi]]).a0 | (bv=bvs[v]).a0) & ol0)
								 | ((ev1=av.a1 | bv.a1) & ol1)
								 | ((ev2=av.a2 | bv.a2) & ol2) ) == 0
							  // and all v's in either ALS are siblings
							  && (ev0 & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == ev0
							  && (ev1 & avA.a1 & bvA.a1) == ev1
							  && (ev2 & avA.a2 & bvA.a2) == ev2 )
								if ( any ) {
									v2 = v;
									break;
								} else {
									v1 = v;
									any = true;
								}
							if ( ++vi > vl )
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
			// starts and ends are used in AlsChain only
			ends[i] = numRccs;
			if ( ++i > last ) {
				// null terminate the static RCCS array incase it's smaller.
				rccs[numRccs] = null;
				break;
			}
		}
		return numRccs;
	}

}
