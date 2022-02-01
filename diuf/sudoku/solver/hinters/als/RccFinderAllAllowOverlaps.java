/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Run.ASSERTS_ENABLED;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;
import java.util.Arrays;

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
 *
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderAllAllowOverlaps extends RccFinderAbstractIndexed {

	// DEBUG: starts and ends for each source-ALS
	public static String startsAndEnds(final int[] starts, final int[] ends, final int numAlss, final String sep) {
		final StringBuilder sb = new StringBuilder(numAlss<<4); // * 16
		for ( int i=0; i<numAlss; ++i ) {
			// sourceAlsIndex=start..end,
			sb.append(i).append("=").append(starts[i]).append("..").append(ends[i]).append(sep);
		}
		return sb.toString();
	}

	public RccFinderAllAllowOverlaps() {
	}

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Idx[] avs // a.vs: indices of cells which maybe v in ALS a
			, bvs // b.vs: indices of cells which maybe v in ALS b
		    , avAll // a.vAll: indices of cells which maybe v in ALS a + buds
		    , bvAll; //b.vAll: indices of cells which maybe v in ALS b + buds
		Idx ai // a.idx: indices of cells in ALS a
		  , bi // b.idx: indices of cells in ALS b
		  , av // a.vs[v]: indices of cells in ALS a which maybe v
		  , bv // b.vs[v]: indices if cells in ALS b which maybe v
		  , avA // a.vAll[v]: indices of cells which maybe v in ALS a + buddies
		  , bvA; //b.vAll[v]: indices of cells which maybe v in ALS b + buddies
		int[] va; // VALUESES[cmnMaybes]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index in the alss array of ALS b
		  , cc // commonCands: candidates shared by ALS a and ALS b (inc v1&2)
		  , vi,vl,v // va index, va.length-1, value at va[vi]
		  , ai0,ai1,ai2 // a.idx exploded
		  , ol0,ol1,ol2 // overlap exploded
		  , ev0,ev1,ev2 // eitherVs exploded
		  , v1, v2 // first and occassional second RC-value
		  ;
		boolean any = false; // any RCC for this pair of ALSs?
		int numRccs = 0 // number RCC's found so far.
		  , i = 0;
		// the last valid alss index, for a fast STOPPER test in inner ALS loop
		final int last = numAlss - 1;
		// harder to debug without clearing rccs first, which is otherwise
		// unecessary, as long as everything works properly. sigh.
		if ( ASSERTS_ENABLED ) {
			Arrays.fill(rccs, null);
		}
		// foreach ALS a
		for (;;) {
			// foreach ALS b (a full n*n search)
			for (
				// starts and ends are used in AlsChain only
				starts[i] = numRccs,
				avAll = (a=alss[i]).vAll,
				avs = a.vs,
				ai0=(ai=a.idx).a0, ai1=ai.a1, ai2=ai.a2,
				aMaybes = a.maybes,
				j = 0 // FIRST
				; //NO_STOPPER
				; //NO_INCREMENT
			) {
				// if there are any maybes common to ALS a and ALS b
				if ( (cc=(aMaybes & alss[j].maybes)) != 0
// this finds ~60% of AlsChains in ~30% of time (dirty hack), but apparently I
// am too stupid to workout WHY it's loosing hints: I've tested a couple of
// theories and still haven't got it, so @stretch there's an opportunity here
// for more speed, available to a seriously smart bastard who can work it out.
//				// if the two ALSs share MULTIPLE common maybes
//				// java bug: do NOT inline this statement
//				cc = (aMaybes & alss[j].maybes);
//				if ( VSIZE[cc] > 1
// this is unnecessary because the no v's in overlap excludes them
//				  // and j and i are not the same ALS index
//				  && j != i
				) {
					// if these two ALSs do NOT overlap (ol* reused, if any).
					// MAGNITUDE: There are about 250 million ALS pairs with a
					// billion cmnMaybes, and about 20% of pairs overlap.
					if ( ( (ol0=(bi=alss[j].idx).a0 & ai0)
						 | (ol1=bi.a1 & ai1)
						 | (ol2=bi.a2 & ai2) ) == 0
					) {
//						++COUNTS[0]; // 191,662,530
						// there is no overlap, so there is no point checking
						// that there are no v's in the overlap, as was done
						// previously. About 80% of pairs do not overlap.
						for (
							  v1 = v2 = 0
							, b = alss[j]
							, bvs = b.vs
							, bvAll = b.vAll
							, va = VALUESES[cc]
							, vl = va.length - 1
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if all v's in either ALS see each other
							if ( ((ev0=(av=avs[v=va[vi]]).a0 | (bv=bvs[v]).a0) & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == ev0
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
						}
					} else {
//						++COUNTS[1]; // 61,661,404
						// these two ALSs overlap. For algorithmic correctness
						// we must check that there are no v's in the overlap,
						// which costs time, now minimised by checking only in
						// those 20% of cases that actually overlap. sigh.
						for (
							  v1 = v2 = 0
							, b = alss[j]
							, bvs = b.vs
							, bvAll = b.vAll
							, va = VALUESES[cc]
							, vl = va.length - 1
							, vi = 0
							; //NO_STOPPER
							; //NO_INCREMENT
						) {
							// if there are no v's in the overlap
							if ( ( ((ev0=(av=avs[v=va[vi]]).a0 | (bv=bvs[v]).a0) & ol0)
								 | ((ev1=av.a1 | bv.a1) & ol1)
								 | ((ev2=av.a2 | bv.a2) & ol2) ) == 0
							  // and all v's in both ALSs see each other
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
						}
					}
				}
				if ( ++j > last ) {
					break;
				}
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
