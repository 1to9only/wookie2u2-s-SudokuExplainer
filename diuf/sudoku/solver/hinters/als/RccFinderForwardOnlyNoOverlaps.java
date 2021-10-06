/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;

/**
 * RccFinderForwardOnlyNoOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = true (ie foreach ALS search subsequent ALSs only), and
 * <li>allowOverlaps = false (ie ignore RCC's in ALS-pairs that overlap)
 * </ul>
 * <p>
 * Please note that AlsChain NEVER does a "forwardOnly" search, so I extend the
 * "base" RccFinderAbstract whose getStartIndexes and getEndIndexes methods
 * just return null, which is fine, because they're never called.
 * 
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderForwardOnlyNoOverlaps extends RccFinderAbstract {

	public RccFinderForwardOnlyNoOverlaps() {
	}

	@Override
	public int find(Als[] alss, int numAlss, Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common value/s (RCC)
		Rcc rcc; // the current Restricted Common Candidate
		Idx ai, bi; // aIdx and bIdx: Idx's in ALS's a and b //inline for speed
		int[] vs; // VALUESES[cmnMaybes]
		int i, j // the indexes of two ALS's in the alss array
		  , cmnMaybes // maybes common to both ALS's a and b
		  , vi,VI,v // index into vs, vs.length, and the value at vs[vi]
		  , ol0,ol1,ol2 // overlap //inline for speed: call no methods
		  , bv0,bv1,bv2; // bothVs //inline for speed: call no methods
		int numRccs = 0; // number RCC's found so far.
		// foreach distinct pair of ALSs (a forward only search)
		for ( i=0; i<numAlss; ++i ) {
			a = alss[i]; // nb: alss is an ArrayList with O(1) get
			for ( j=i+1; j<numAlss; ++j ) {
				// if these two ALSs have common maybes
				if ( (cmnMaybes=(a.maybes & (b=alss[j]).maybes)) != 0
				  // and the ALSs don't overlap (ol* for reuse later)
				  && ( ( (ol0=(ai=a.idx).a0 & (bi=b.idx).a0)
					   | (ol1=ai.a1 & bi.a1)
					   | (ol2=ai.a2 & bi.a2) ) == 0 )
				) {
					// foreach maybe common to a and b
					for ( rcc=null,vs=VALUESES[cmnMaybes],vi=0,VI=vs.length; vi<VI; ++vi ) {
						// if there are no v's in the overlap
						if ( ( ((bv0=(ai=a.vs[v=vs[vi]]).a0 | (bi=b.vs[v]).a0) & ol0)
							 | ((bv1=ai.a1 | bi.a1) & ol1)
							 | ((bv2=ai.a2 | bi.a2) & ol2) ) == 0
						  // and all v's in both ALSs see each other
						  && (bv0 & (ai=a.vAll[v]).a0 & (bi=b.vAll[v]).a0) == bv0
						  && (bv1 & ai.a1 & bi.a1) == bv1
						  && (bv2 & ai.a2 & bi.a2) == bv2 ) {
							if ( rcc == null ) {
								rccs[numRccs++] = rcc = new Rcc(i, j, v);
							} else {
								rcc.v2 = v;
								break;
							}
						}
					}
				}
			}
		}
		return numRccs;
	}
	
}
