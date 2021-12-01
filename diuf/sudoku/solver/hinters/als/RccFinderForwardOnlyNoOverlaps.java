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
 * RccFinderForwardOnlyNoOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = true (ie foreach ALS search subsequent ALSs only), and
 * <li>allowOverlaps = false (ie ignore RCC's in ALS-pairs that overlap).
 * </ul>
 * <p>
 * AlsChain NEVER does a "forwardOnly" search, so I extend RccFinderAbstract
 * whose getStarts and getEnds methods return null, which works only because
 * they are never called.
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
		Idx ai, bI; // aIdx and bIdx: Idx's in ALS's a and b //inline for speed
		Idx[] avs, avAll; // a.vs and a.vAll
		int[] vs; // VALUESES[cmnMaybes]
		int i, j // the indexes of two ALS's in the alss array
		  , vi,vl,v // index into vs, vs.length-1, and the value at vs[vi]
		  , ol0,ol1,ol2 // overlap //inline for speed: call no methods
		  , bv0,bv1,bv2 // bothVs //inline for speed: call no methods
		  , v1, v2 // first and occassional second RC-value of this ALS pair
		  , aMaybes
		  ;
		boolean any = false; // any RCC found for this pair of ALSs
		int numRccs = 0; // number RCC's found so far.
		// foreach distinct pair of ALSs (a forward only search)
		for ( i=0; i<numAlss; ++i ) {
			avAll = (a=alss[i]).vAll;
			avs = a.vs;
			ai = a.idx;
			aMaybes = a.maybes;
			for ( j=i+1; j<numAlss; ++j ) {
				// if these two ALSs have common maybes
				if ( (aMaybes & alss[j].maybes) != 0
				  // and the ALSs don't overlap (ol* for reuse later)
				  && ( ( (ol0=ai.a0 & (bI=alss[j].idx).a0)
					   | (ol1=ai.a1 & bI.a1)
					   | (ol2=ai.a2 & bI.a2) ) == 0 )
				) {
					// foreach maybe common to a and b
					for ( v1 = v2 = 0
						, b = alss[j]
						, vs = VALUESES[aMaybes & b.maybes]
					    , vl = vs.length-1
						, vi = 0
						; //NO_STOPPER
						; //NO_INCREMENT
					) {
						// if there are no v's in the overlap
						if ( ( ((bv0=(ai=avs[v=vs[vi]]).a0 | (bI=b.vs[v]).a0) & ol0)
							 | ((bv1=ai.a1 | bI.a1) & ol1)
							 | ((bv2=ai.a2 | bI.a2) & ol2) ) == 0
						  // and all v's in both ALSs see each other
						  && (bv0 & (ai=avAll[v]).a0 & (bI=b.vAll[v]).a0) == bv0
						  && (bv1 & ai.a1 & bI.a1) == bv1
						  && (bv2 & ai.a2 & bI.a2) == bv2 ) {
							if ( any ) {
								v2 = v;
								break; // there can't be more than 2 RC-values
							} else {
								v1 = v;
								any = true;
							}
						}
						if ( ++vi > vl ) {
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
		}
		return numRccs;
	}

}
