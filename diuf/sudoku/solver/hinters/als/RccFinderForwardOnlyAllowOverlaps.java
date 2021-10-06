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
 * RccFinderForwardOnlyAllowOverlaps implements RccFinder to find RCC's with: <ul>
 * <li>forwardOnly = true (ie foreach ALS search subsequent ALSs only), and
 * <li>allowOverlaps = true (ie find RCC's in ALS-pairs that overlap)
 * </ul>
 * Please note that AlsChain NEVER does a "forwardOnly" search, so I extend the
 * "base" RccFinderAbstract whose getStartIndexes and getEndIndexes methods
 * just return null, which is fine, because they're never called.
 *
 * @author Keith Corlett 2021-07-29
 */
final class RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract {

	@Override
	public int find(final Als[] alss, final int numAlss, final Rcc[] rccs) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on Restricted Common Candidate/s (RCC)
		Rcc rcc; // the current Restricted Common Candidate
		Idx[] avs, bvs		// a.vs,		b.vs
			, avAll, bvAll; // a.vAll,		b.vAll
		Idx aidx, bidx		// a.idx,		b.idx
		  , av, bv			// a.vs[v],		b.vs[v]
		  , aA, bA;			// a.vAll[v],	b.vAll[v]
		int[] vs; // VALUESES[cmnMaybes]
		int amaybes // a.maybes
		  , j // index in alss array of ALS b
		  , cmnMaybes // maybes common to both ALSs a and b
		  , k,K,v // index into vs, vs.length, and the value at vs[k]
		  , ol0,ol1,ol2 // overlap //inline for speed: call no methods
		  , bv0,bv1,bv2; // bothVs //inline for speed: call no methods
		final int lastAls = numAlss - 1; // for ++j>lastAls
		final int penultimateAls = lastAls - 1; // for ++i>penultimateAls
		int numRccs = 0; // number RCCs found so far.
		int i = 0; // index in alss array of the first ALS
//inline for speed: call no methods (original code retained as documentation)
//		final Idx overlap = this.overlap;//.clear();
//		final Idx bothVs = this.bothVs;//.clear();
//		final Idx vAlls = this.VAlls;//.clear();
		// foreach ALS (except the last)
//		System.out.println();
//		System.out.println("processing i="+i);
		for (;;) { // i-loop
//			++COUNTS[0];
// starts and ends are not used when forwardOnly is true, so commented out.
// This is a bit dodgy, but I hope it's a smidge faster.
//			starts[i] = numRccs;
			j = i + 1; // index of the first ALS b: a forward only search
			// unpack ALS a, for speed
			avAll = (a=alss[i]).vAll;
			avs = a.vs;
			aidx = a.idx;
			amaybes = a.maybes;
			// foreach subsequent ALS (a forward only search)
//			System.out.println("processing j="+j);
			for (;;) { // j-loop
//				++COUNTS[1];
				// get maybes common to ALSs a and b, skip if none
				if ( (cmnMaybes=(amaybes & (b=alss[j]).maybes)) != 0 ) {
					bvs = b.vs;
					bvAll = b.vAll;
					// calculate the overlap of ALSs a and b for later
					// overlap.setAnd(a.idx, b.idx);
//					++COUNTS[2];
					ol0 = aidx.a0 & (bidx=b.idx).a0;
					ol1 = aidx.a1 & bidx.a1;
					ol2 = aidx.a2 & bidx.a2;
					// we remember the rcc to add the second RC value, if any
					rcc = null;
					// get an array of the maybes common to ALSs a and b
					vs = VALUESES[cmnMaybes];
					// micro opting
					k = 0;
					K = vs.length - 1; // for ++k>K
					// foreach maybe common to ALSs a and b
					// there must be one, or cmnMaybes would have been 0
					for (;;) { // k-loop
//						++COUNTS[3];
						// get indices of v in both ALSs, none of which may be
						// in the overlap, even if allowOverlaps. And they all
						// must see each other: That's the "Restricted" in
						// Restricted Common Candidate, coz then v must be in
						// one ALS or the other (never both).
//inline for speed: call no methods (original code retained as documentation)
//						if ( bothVs.setOr(a.vs[v=vs[vi]], b.vs[v]).andNone(overlap)
//						  && bothVs.andEqualsThis(vAlls.setAnd(a.vAll[v], b.vAll[v])) )
//						  // v is an RC if all v's in both ALSs see each other
//						  // * bothVs        allVsInBothAlss
//						  // * andEqualsThis areAllIn
//						  // * vAlls         vsPlusTheirBudsCommonToBothAlss
//						  //   retains each v only if ALL other vsInBothAlss
//						  //   see it (and also external v's seen by all v's
//						  //   in both ALSs). vAlls is pretty tricky!
//						  // * bothVs areAllIn vsPlusTheirBudsCommonToBothAlss
						if ( ( ((bv0=(av=avs[v=vs[k]]).a0 | (bv=bvs[v]).a0) & ol0)
							 | ((bv1=av.a1 | bv.a1) & ol1)
							 | ((bv2=av.a2 | bv.a2) & ol2) ) == 0
//						  && ++COUNTS[4] > -1
						  // vAlls is calculated inline, so no var required
						  && (bv0 & (aA=avAll[v]).a0 & (bA=bvAll[v]).a0) == bv0
						  && (bv1 & aA.a1 & bA.a1) == bv1
						  && (bv2 & aA.a2 & bA.a2) == bv2 ) {
//							++COUNTS[5];
							if ( rcc == null ) {
								rccs[numRccs++] = rcc = new Rcc(i, j, v);
							} else { // a fairly rare second RC value in an RCC
//								++COUNTS[6];
								rcc.v2 = v;
								break; // there can NEVER be a third one!
							}
						}
						if ( ++k > K ) {
							break;
						}
					}
				}
				if ( ++j > lastAls ) {
					break;
				}
//				System.out.println("processing j="+j);
			}
// starts and ends are not used when forwardOnly is true, so commented out.
// This is a bit dodgy, but I hope it's a smidge faster.
//			ends[i] = numRccs;
			if ( ++i > penultimateAls ) {
				break;
			}
//			System.out.println();
//			System.out.println("processing i="+i);
		}
		return numRccs;
	}

}
