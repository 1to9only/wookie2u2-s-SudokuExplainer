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
 * RccFinderAllNoOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = false (ie search "All" ALS pairs), and
 * <li>allowOverlaps = false (ie ignore ALS's that overlap).
 * </ul>
 * <p>
 * AlsChain always does an "All" search (currently RccFinderAllAllowOverlaps)
 * using the start and end indexes, so I extend RccFinderAbstractForAlsChains
 * to inherit it's starts and ends fields, and it's implementations of
 * getStartIndexes and getEndIndexes, which return those arrays (not null).
 *
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderAllNoOverlaps extends RccFinderAbstractIndexed {

	public RccFinderAllNoOverlaps() {
	}

	@Override
	public int find(Als[] alss, int numAlss, Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Rcc rcc; // the current Restricted Common Candidate
		Idx[] avs // a.vs: indices of cells which maybe v in ALS a
		    , avAll // a.vAll: indices of cells which maybe v in ALS a + buds
			, bvs // b.vs: indices of cells which maybe v in ALS b
		    , bvAll; // a.vAll: indices of cells which maybe v in ALS b + buds
		Idx aidx // a.idx: indices of cells in ALS a
		  , av // a.vs[v]: indices of cells in ALS a which maybe v
		  , avA // a.vAll[v]: indices of cells which maybe v in ALS a + buddies
		  , bidx // b.*: is re-used for all of b's Idx's (currently three)
		  , bvsv; // b.vs[v]: indices if cells in ALS b which maybe v
		int[] vs; // VALUESES[cmnMaybes]: array of maybes common to ALSs a & b
		int aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index in the alss array of ALS b
		  , cmnMaybes // maybes common to both ALS's a and b
		  , vi,vn,v // index into vs, vs.length, and the value at vs[vi]
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
				// if the two ALSs share common maybes
				if ( (cmnMaybes=(aMaybes & (b=alss[j]).maybes)) != 0
				  // and the two ALSs do NOT physically overlap
				  && ( ( (aidx.a0 & (bidx=b.idx).a0)
					   | (aidx.a1 & bidx.a1)
					   | (aidx.a2 & bidx.a2) ) == 0 )
				) {
					// foreach common value
					for ( vs=VALUESES[cmnMaybes], vn=vs.length, vi=0, bvs=b.vs
							, bvAll=b.vAll, rcc=null; ; ) {
						// if all v's in both ALSs see each other
						if ( ((bv0=(av=avs[v=vs[vi]]).a0 | (bvsv=bvs[v]).a0) & (avA=avAll[v]).a0 & (bidx=bvAll[v]).a0) == bv0
						  && ((bv1 = av.a1 | bvsv.a1) & avA.a1 & bidx.a1) == bv1
						  && ((bv2 = av.a2 | bvsv.a2) & avA.a2 & bidx.a2) == bv2
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
				}
			}
			ends[i] = numRccs;
		}
		return numRccs;
	}

}
