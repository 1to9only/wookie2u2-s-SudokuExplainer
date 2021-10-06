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
 * RccFinderAllAllowOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = false (ie search "All" ALS pairs), and
 * <li>allowOverlaps = true (ie find RCC's in ALS-pairs that overlap).
 * </ul>
 * <p>
 * AlsChain always does an "All" search (currently RccFinderAllAllowOverlaps)
 * using the start and end indexes, so I extend RccFinderAbstractForAlsChains
 * to inherit it's starts and ends fields, and it's implementations of
 * getStart/EndIndexes that return the starts and ends arrays (not null).
 *
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderAllAllowOverlaps extends RccFinderAbstractIndexed {

	public RccFinderAllAllowOverlaps() {
	}

	@Override
	public int find(Als[] alss, int numAlss, Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Rcc rcc; // the current Restricted Common Candidate
		Idx[] avs // a.vs: indices of cells which maybe v in ALS a
			, bvs // b.vs: indices of cells which maybe v in ALS b
		    , avAll // a.vAll: indices of cells which maybe v in ALS a + buds
		    , bvAll; //b.vAll: indices of cells which maybe v in ALS b + buds
		Idx aidx // a.idx: indices of cells in ALS a
		  , bidx // b.idx: indices of cells in ALS b
		  , av // a.vs[v]: indices of cells in ALS a which maybe v
		  , bv // b.vs[v]: indices if cells in ALS b which maybe v
		  , avA // a.vAll[v]: indices of cells which maybe v in ALS a + buddies
		  , bvA; //b.vAll[v]: indices of cells which maybe v in ALS b + buddies
		int[] vs; // VALUESES[cmnMaybes]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index in the alss array of ALS b
		  , cmnMaybes // maybes common to both ALS's a and b
		  , vi,vn,v // index into vs, vs.length, and the value at vs[vi]
		  , ol0,ol1,ol2 // overlap //inline for speed: call no methods
		  , bv0,bv1,bv2; // bothVs //inline for speed: call no methods
		int numRccs = 0; // number RCC's found so far.
		// foreach ALS
		for ( int i=0; i<numAlss; ++i ) {
			a = alss[i];
			starts[i] = numRccs;
			avAll = a.vAll;
			avs = a.vs;
			aidx = a.idx;
			aMaybes = a.maybes;
			// foreach ALS again (a full n*n search)
			for ( j=0; j<numAlss; ++j ) {
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(aMaybes & (b=alss[j]).maybes)) != 0
				  // and a and b are not the same ALS
				  && j != i
				) {
					// if these two ALSs do NOT overlap (ol* reused, if any).
					// SIZE: There are about 250 million pairs, with a billion
					// cmnMaybes, and about a fifth of ALS-pairs overlap.
					if ( ( (ol0=aidx.a0 & (bidx=b.idx).a0)
						 | (ol1=aidx.a1 & bidx.a1)
						 | (ol2=aidx.a2 & bidx.a2) ) == 0
					) {
//						++COUNTS[0]; // 191,662,530
						// there is no overlap, so there is no point checking
						// that there are no v's in the overlap, as was done in
						// the previous version of this method, wasting time.
						// about four fifths of ALS-pairs have no overlap.
						for ( vs=VALUESES[cmnMaybes], vn=vs.length, vi=0
								, bvs=b.vs, bvAll=b.vAll, rcc=null; ; ) {
							// if all v's in both ALSs see each other
							if ( ((bv0=(av=avs[v=vs[vi]]).a0 | (bv=bvs[v]).a0) & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == bv0
							  && ((bv1=av.a1 | bv.a1) & avA.a1 & bvA.a1) == bv1
							  && ((bv2=av.a2 | bv.a2) & avA.a2 & bvA.a2) == bv2
							) {
								if ( rcc == null ) {
									rccs[numRccs++] = rcc = new Rcc(i, j, v);
									if ( numRccs == MAX_RCCS ) {
										Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
										return numRccs;
									}
								} else {
									rcc.v2 = v;
									break;
								}
							}
							if ( ++vi == vn ) {
								break;
							}
						}
					} else {
//						++COUNTS[1]; // 61,661,404
						// these two ALSs overlap. For algorithmic correctness
						// we must check that there are no v's in the overlap.
						// This test costs us a small time impost, which is now
						// minimised by only being done when an overlap exists,
						// ergo in about a fifth of cases.
						for ( vs=VALUESES[cmnMaybes], vn=vs.length, vi=0
								, bvs=b.vs, bvAll=b.vAll, rcc=null; ; ) {
							// if there are no v's in the overlap
							if ( ( ((bv0=(av=avs[v=vs[vi]]).a0 | (bv=bvs[v]).a0) & ol0)
								 | ((bv1=av.a1 | bv.a1) & ol1)
								 | ((bv2=av.a2 | bv.a2) & ol2) ) == 0
							  // and all v's in both ALSs see each other
							  && (bv0 & (avA=avAll[v]).a0 & (bvA=bvAll[v]).a0) == bv0
							  && (bv1 & avA.a1 & bvA.a1) == bv1
							  && (bv2 & avA.a2 & bvA.a2) == bv2
							) {
								if ( rcc == null ) {
									rccs[numRccs++] = rcc = new Rcc(i, j, v);
									if ( numRccs == MAX_RCCS ) {
										Log.teeln("WARN: "+Log.me()+": MAX_RCCS exceeded!");
										return numRccs; // out of space, no crash!
									}
								} else {
									rcc.v2 = v;
									break;
								}
							}
							if ( ++vi == vn ) {
								break;
							}
						}
					}
				}
			}
			// starts and ends are used when forwardOnly is false
			ends[i] = numRccs;
		}
		return numRccs;
	}

}
