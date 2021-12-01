/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_RCCS;
import diuf.sudoku.utils.Log;

/**
 * RccFinderAllNoOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = false (ie search "All" ALS pairs), and
 * <li>allowOverlaps = false (ie ignore ALS's that overlap).
 * </ul>
 * <p>
 * Used by BigWings (first), and DeathBlossom (last) so find twice per pass.
 *
 * @author Keith Corlett 2021-07-29
 */
public class RccFinderAllNoOverlaps extends RccFinderAbstractIndexed {

	public RccFinderAllNoOverlaps() {
	}

	@Override
	public int find(Als[] alss, int numAlss, Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs that intersect on one-or-two RC-value/s
		Idx[] avs // a.vs: indices of cells which maybe v in ALS a
		    , avAll // a.vAll: indices of cells which maybe v in ALS a + buds
			, bvs // b.vs: indices of cells which maybe v in ALS b
		    , bvAll; // a.vAll: indices of cells which maybe v in ALS b + buds
		Idx aidx // a.idx: indices of cells in ALS a
		  , av // a.vs[v]: indices of cells in ALS a which maybe v
		  , avA // a.vAll[v]: indices of cells which maybe v in ALS a + buddies
		  , bidx // b.*: is re-used for all of b's Idx's (currently three)
		  , bv; // b.vs[v]: indices if cells in ALS b which maybe v
		int[] vs; // VALUESES[cmnMaybes]: array of maybes common to ALSs a & b
		int i // alss-index in the outer 'a' loop
		  , aMaybes // a.maybes: all potential values of cells in ALS a
		  , j // the index in the alss array of ALS b
		  , vi,vn,v // index into vs, vs.length, and the value at vs[vi]
		  , bv0,bv1,bv2 // bothVs //inline for speed: call no methods
		  , v1, v2 // first and occassional second RC-value
		  ;
		boolean any = false; // any RCC for this pair of ALSs?
		int numRccs = 0; // number RCC's found so far.
		// foreach ALS
		for ( i=0; i<numAlss; ++i ) {
			starts[i] = numRccs;
			a = alss[i];
			avAll = a.vAll;
			avs = a.vs;
			aidx = a.idx;
			aMaybes = a.maybes;
			// foreach ALS again (a full n*n search)
			for ( j=0; j<numAlss; ++j ) {
				// if the two ALSs share any common maybes
				if ( (aMaybes & alss[j].maybes) != 0
				  // and the two ALSs do NOT physically overlap
				  // which also supresses j == i
				  && ( ( (aidx.a0 & (bidx=alss[j].idx).a0)
					   | (aidx.a1 & bidx.a1)
					   | (aidx.a2 & bidx.a2) ) == 0 )
				) {
					// foreach common value
					for ( v1 = v2 = 0
					    , b = alss[j]
						, bvs = b.vs
						, bvAll = b.vAll
					    , vs = VALUESES[aMaybes & b.maybes]
					    , vn = vs.length
						, vi = 0
						;//NO_STOPPER
						;//NO_INCREMENT
					) {
						// if all v's in both ALSs see each other
						if ( ((bv0=(av=avs[v=vs[vi]]).a0 | (bv=bvs[v]).a0) & (avA=avAll[v]).a0 & (bidx=bvAll[v]).a0) == bv0
						  && ((bv1 = av.a1 | bv.a1) & avA.a1 & bidx.a1) == bv1
						  && ((bv2 = av.a2 | bv.a2) & avA.a2 & bidx.a2) == bv2
						) {
							if ( any ) {
								v2 = v;
								break;
							} else {
								v1 = v;
								any = true;
							}
						}
						if ( ++vi == vn ) {
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
			ends[i] = numRccs;
		}
		return numRccs;
	}

}
