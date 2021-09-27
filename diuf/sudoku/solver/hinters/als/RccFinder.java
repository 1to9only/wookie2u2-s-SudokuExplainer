/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;

/**
 * RccFinder finds Restricted Common Candidates. It was split-out of AAlsFinder
 * because it kept getting more-and-more complex, so deserves it's own class.
 * <pre>
 * KRC 2020-06-05 On Last nights changes.
 * AlsXz is the largest-runtime hinter that I still use, so speed it up!
 * BEFORE: 14,196,723,200  9791  1,449,976  3955  3,589,563 ALS-XZ
 * AFTER : 11,596,759,400  9791  1,184,430  3955  2,932,176 ALS-XZ
 * Finding RCC's takes Ken ages.
 * ALS_XZ: alss=902,700 rccs=13,988,600,100 self=303,228,500
 * alss and rccs are for ALL getAlss and getRccs runs, not just AlsXz's.
 * self is just AlsXz excl both, so AlsXz's total getAlss and getRccs time is
 * 11,596,759,400 - 303,228,500 = 11,293,530,900
 * So I inlined all method calls, for speed, but it's only around 1.5 seconds
 * faster, which is not enough, in my humble opinion, but I really don't know
 * what else I can do to speed it up. sigh.
 *
 * 09-27-59 Split getRccs into getRccsForwardOnly and getRccsAll
 * AFTER:  11,680,153,300  9791  1,192,947  3955  2,953,262 ALS-XZ
 * FMS it's actually about 0.1 seconds SLOWER, so I give up. Arrrgggghhhh!
 *
 * KRC 2021-06-15 17:07 Hunting speed again. Split getRccsForwardOnly
 * into getRccsForwardOnlyAllowOverlaps and getRccsForwardOnlyNoOverlaps.
 * AFTER : 12,068,262,700  9802  1,231,204  3925  3,074,716 ALS-XZ
 * methods:
 * getRccsForwardOnlyAllowOverlaps = 9,832
 * getRccsForwardOnlyNoOverlaps    =     0
 * getRccsAll                      = 4,439
 * getRccsForwardOnlyAllowOverlaps loops:
 * total ALS's        =   2,302,946
 * second ALS         = 272,928,890
 * with common maybes = 270,813,859
 * num common maybes  = 965,792,260
 * past bothVs/olap   = 730,526,968
 * total RCC's        =   9,082,542
 * RCC's with v2      =     437,014
 *
 * with VSIZE instead of vs.length is slower
 *         12,615,458,000  9802  1,287,028  3925  3,214,129 ALS-XZ
 * with post-tested loop is slower, but all slower, so I blame HOT BOX
 *         13,116,586,300  9802  1,338,154  3925  3,341,805 ALS-XZ
 * again after a 5 minute cool down
 *         12,192,202,100  9802  1,243,848  3925  3,106,293 ALS-XZ
 * for(;;) with break
 *         12,223,873,900  9802  1,247,079  3925  3,114,362 ALS-XZ
 * axs and ays
 *         12,025,245,800  9802  1,226,815  3925  3,063,756 ALS-XZ
 * aMaybes and lastAls everywhere: lost 5 hints, and there's something weird
 * going on with lastAls. It should be numAlss-1 but that NPE's in runtime but
 * not in Debugger, then lastAls=numAlss-2 works but misses 5 hints.
 *         11,706,983,600  9799  1,194,712  3920  2,986,475 ALS-XZ
 * again
 *         11,897,078,200  9799  1,214,111  3920  3,034,968 ALS-XZ
 * penultimateAls for the j-loop and lastAls for the i-loop
 *         12,596,531,900  9802  1,285,098  3925  3,209,307 ALS-XZ
 * bvs and bvAll
 *         11,818,566,700  9802  1,205,730  3925  3,011,099 ALS-XZ

 * </pre>
 *
 * @author Keith Corlett 2021-06-04
 */
final class RccFinder {

    /**
	 * The index of the first RC in {@code rccs} for each {@code alss}.
	 * This field is set by getRccs and used by AlsChain.
	 */
    final int[] startInds = new int[MAX_ALSS];

    /**
	 * The index of the last RC in {@code rccs} for each {@code alss}.
	 * This field is set by getRccs and used by AlsChain.
	 */
    final int[] endInds = new int[MAX_ALSS];

//	static final long[] COUNTS = new long[7];

	/**
	 * Find the Restricted Common Candidates in this ALSS array, add them to
	 * the RCCS array, and return how many.
	 * <p>
	 * "Restricted" means that all occurrences of the candidate (a "potential
	 * value" in my terminology) see (in same box, row, or col) each other.
	 * "Common" because obviously that value must be a potential value of a
	 * cell in both ALSs.
	 * "Candidate" is a potential value, typically value, or just plain v.
	 * <p>
	 * getRccs enumerates the ALSS array twice. If forwardOnly is true then it
	 * is a "forward only search" which means that for each ALS 'a' we examine
	 * only ALS 'b's to my right (with a larger index) so that we examine each
	 * distinct pair of ALS's once. If, however, forwardOnly is false then all
	 * possible combinations of ALSs are examined, including a duplicate of
	 * each combination (a-and-b, then later b-and-a). Now, for reasons that I
	 * do NOT understand AlsChains finds more hints when forwardOnly is false.
	 * I think because it's chaining ALS's it needs each pair in both possible
	 * orders in order to easily find all possible chains; but beware that when
	 * both allowNakedSets and allowOverlaps the number of RCC's explodes.
	 * <p>
	 * Note that the names ALSS and RCCS are all-caps just to denote that they
	 * are static final "fixed-size" arrays, so you can't just foreach them!
	 * <p>
	 * If the allowOverlaps parameter is true then ALS's which physically
	 * overlap can form a pair (an RCC), otherwise they're filtered out.
	 * <p>
	 * The useStartAndEnd parameter is no longer used: startInds and endInds
	 * are always populated regardless of the value passed coz it's faster to
	 * just do it than it is to avoid doing-it when not required. Nike rules.
	 */
	int getRccs(final Als[] ALSS, final int numAlss, final Rcc[] RCCS
			, final boolean forwardOnly, final boolean allowOverlaps) {
		// these methods where split-up for speed
		if ( forwardOnly ) {
			if ( allowOverlaps ) {
				// HAMMERED by AlsXz: need speed, so as simple as possible.
				return getRccsForwardOnlyAllowOverlaps(ALSS, numAlss, RCCS);
			} else { // this is never used, so is untested!
				return getRccsForwardOnlyNoOverlaps(ALSS, numAlss, RCCS);
			}
		} else { // the only caller is AlsChain which doesn't allowOverlaps
			return getRccsAll(ALSS, numAlss, RCCS, allowOverlaps);
		}
	}

	private int getRccsForwardOnlyAllowOverlaps(final Als[] ALSS, final int numAlss, final Rcc[] RCCS) {
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
			startInds[i] = numRccs;
			j = i + 1; // index of the first ALS b, for speed
			// unpack ALS a, for speed
			avAll = (a=ALSS[i]).vAll;
			avs = a.vs;
			aidx = a.idx;
			amaybes = a.maybes;
			// foreach subsequent ALS (a forward only search)
//			System.out.println("processing j="+j);
			for (;;) { // j-loop
//				++COUNTS[1];
				// get maybes common to ALSs a and b, skip if none
				if ( (cmnMaybes=(amaybes & (b=ALSS[j]).maybes)) != 0 ) {
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
							if ( rcc == null )
								RCCS[numRccs++] = rcc = new Rcc(i, j, v);
							else { // a fairly rare second RC value in an RCC
//								++COUNTS[6];
								rcc.v2 = v;
								break; // there can NEVER be a third one!
							}
						}
						if ( ++k > K )
							break;
					}
				}
				if ( ++j > lastAls )
					break;
//				System.out.println("processing j="+j);
			}
			endInds[i] = numRccs;
			if ( ++i > penultimateAls )
				break;
//			System.out.println();
//			System.out.println("processing i="+i);
		}
		return numRccs;
	}

	private int getRccsForwardOnlyNoOverlaps(final Als[] ALSS, final int numAlss, final Rcc[] RCCS) {
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
//inline for speed: call no methods (original code retained as documentation)
//		final Idx overlap = this.overlap;//.clear();
//		final Idx bothVs = this.bothVs;//.clear();
//		final Idx vAlls = this.VAlls;//.clear();
		// foreach distinct pair of ALSs (a forward only search)
		for ( i=0; i<numAlss; ++i ) {
			a = ALSS[i]; // nb: alss is an ArrayList with O(1) get
			startInds[i] = numRccs;
			for ( j=i+1; j<numAlss; ++j )
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(a.maybes & (b=ALSS[j]).maybes)) != 0
				  // the ALSs can't overlap (we need overlap later anyway)
//inline for speed: call no methods (original code retained as documentation)
//				  && overlap.setAnd(a.idx, b.idx).none()
				  && ( ( (ol0=(ai=a.idx).a0 & (bi=b.idx).a0)
					   | (ol1=ai.a1 & bi.a1)
					   | (ol2=ai.a2 & bi.a2) ) == 0 )
				)
					// foreach maybe common to a and b
					// rcc=null to tell between 1st and 2nd value of an RCC
					// vs is an array of the values in cmnMaybes
					// the rest is a boring for loop
					for ( rcc=null,vs=VALUESES[cmnMaybes],vi=0,VI=vs.length; vi<VI; ++vi ) {
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
						if ( ( ((bv0=(ai=a.vs[v=vs[vi]]).a0 | (bi=b.vs[v]).a0) & ol0)
							 | ((bv1=ai.a1 | bi.a1) & ol1)
							 | ((bv2=ai.a2 | bi.a2) & ol2) ) == 0
						  // vAlls is calculated inline, so no var required
						  && (bv0 & (ai=a.vAll[v]).a0 & (bi=b.vAll[v]).a0) == bv0
						  && (bv1 & ai.a1 & bi.a1) == bv1
						  && (bv2 & ai.a2 & bi.a2) == bv2 )
							if ( rcc == null )
								RCCS[numRccs++] = rcc = new Rcc(i, j, v);
							else { // a rare second RC value in the one RCC
								rcc.v2 = v;
								break; // there can NEVER be a third one!
							}
					}
			endInds[i] = numRccs;
		}
		return numRccs;
	}

	private int getRccsAll(final Als[] ALSS, final int numAlss, final Rcc[] RCCS
			, final boolean allowOverlaps) {
		// ALL variables are pre-declared, ANSII-C style, for zero stack-work.
		Als a, b; // two ALSs intersect on a Restricted Common Candidate (RCC)
		Rcc rcc; // the current Restricted Common Candidate
		Idx[] avs // a.vs
		    , avAll; // a.vAll
		Idx ai // a.idx
		  , av // a.vs[v]
		  , avA // a.vAll[v]
		  , bi; // is used for all three of b's Idx's
		int[] vs; // VALUESES[cmnMaybes]
		int amaybes // a.maybes
		  , j // the index in the alss array of ALS b
		  , cmnMaybes // maybes common to both ALS's a and b
		  , vi,VI,v // index into vs, vs.length, and the value at vs[vi]
		  , ol0,ol1,ol2 // overlap //inline for speed: call no methods
		  , bv0,bv1,bv2; // bothVs //inline for speed: call no methods
		int numRccs = 0; // number RCC's found so far.
//inline for speed: call no methods (original code retained as documentation)
//		final Idx overlap = this.overlap;//.clear();
//		final Idx bothVs = this.bothVs;//.clear();
//		final Idx vAlls = this.VAlls;//.clear();
		// foreach ALS (except the last)
		for ( int i=0; i<numAlss; ++i ) {
			a = ALSS[i]; // nb: alss is an ArrayList with O(1) get
			startInds[i] = numRccs;
			avAll = a.vAll;
			avs = a.vs;
			ai = a.idx;
			amaybes = a.maybes;
			for ( j=0; j<numAlss; ++j )
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(amaybes & (b=ALSS[j]).maybes)) != 0
				  // see if the ALSs overlap (we need overlap later anyway)
//inline for speed: call no methods (original code retained as documentation)
//				  && (overlap.setAnd(a.idx, b.idx).none() || allowOverlaps) )
				  && ( ( (ol0=ai.a0 & (bi=b.idx).a0)
					   | (ol1=ai.a1 & bi.a1)
					   | (ol2=ai.a2 & bi.a2) ) == 0
					|| (allowOverlaps && j!=i) ) // can't be the same ALS
				)
					// foreach maybe common to a and b
					// rcc=null to tell between 1st and 2nd value of an RCC
					// vs is an array of the values in cmnMaybes
					// the rest is a boring for loop
					for ( rcc=null,vs=VALUESES[cmnMaybes],vi=0,VI=vs.length; vi<VI; ++vi ) {
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
						bv0 = (av=avs[v=vs[vi]]).a0 | (bi=b.vs[v]).a0;
						bv1 = av.a1 | bi.a1;
						bv2 = av.a2 | bi.a2;
						// .andNone(overlap)
						if ( ( (bv0&ol0) | (bv1&ol1) | (bv2&ol2) ) == 0
						  // vAlls is calculated inline, so no var required
						  && (bv0 & (avA=avAll[v]).a0 & (bi=b.vAll[v]).a0) == bv0
						  && (bv1 & avA.a1 & bi.a1) == bv1
						  && (bv2 & avA.a2 & bi.a2) == bv2 )
							if ( rcc == null )
								RCCS[numRccs++] = rcc = new Rcc(i, j, v);
							else { // a rare second RC value in the one RCC
								rcc.v2 = v;
								break; // there can NEVER be a third one!
							}
					}
			endInds[i] = numRccs;
		}
		return numRccs;
	}

}
