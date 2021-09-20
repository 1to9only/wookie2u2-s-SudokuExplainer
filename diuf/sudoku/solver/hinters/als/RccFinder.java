/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
 * </pre>
 *
 * @author Keith Corlett 2021-06-04
 */
final class RccFinder {

//inline for speed: call no methods
//	private final Idx overlap = new Idx();
//	private final Idx bothVs = new Idx();
//	private final Idx VAlls = new Idx();
    /** The index of the first RC in {@code rccs} for each {@code alss}.
	 * This field is set by getRccs and used by AlsChain. */
    final int[] startInds = new int[MAX_ALSS];
    /** The index of the last RC in {@code rccs} for each {@code alss}.
	 * This field is set by getRccs and used by AlsChain. */
    final int[] endInds = new int[MAX_ALSS];

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
			, final boolean forwardOnly, final boolean allowOverlaps
			, final boolean useStartAndEnd) {
		// The only difference between these two methods is there start j.
		// I'm simplifying it, piece-by-little-piece, coz it takes too long.
		// SUCCINCTLY: terniaries are slow, so the "for loops" are split up.
		// For large changes to this method:
		// 1. just change getRccsForwardOnly, and test it, and when happy...
		// 2. copy-paste the code in the whole method into getRccsAll
		// 3. change j=0; and if j != i.
		// For minor changes, just be sure to change both methods!
		if ( forwardOnly )
			return getRccsForwardOnly(ALSS, numAlss, RCCS, allowOverlaps, useStartAndEnd);
		else
			return getRccsAll(ALSS, numAlss, RCCS, allowOverlaps, useStartAndEnd);
	}

	private int getRccsForwardOnly(final Als[] ALSS, final int numAlss, final Rcc[] RCCS
			, final boolean allowOverlaps, final boolean useStartAndEnd) {
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
				  // see if the ALSs overlap (we need overlap later anyway)
//inline for speed: call no methods (original code retained as documentation)
//				  && (overlap.setAnd(a.idx, b.idx).none() || allowOverlaps) )
				  && ( ( ( (ol0=(ai=a.idx).a0 & (bi=b.idx).a0)
					     | (ol1=ai.a1 & bi.a1)
					     | (ol2=ai.a2 & bi.a2) ) == 0 )
					|| allowOverlaps ) )
					// foreach maybe common to a and b
					// rcc=null to tell between 1st and 2nd value of an RCC
					// vs is an array of the values in cmnMaybes
					// the rest is a boring for loop
					for ( rcc=null,vs=VALUESES[cmnMaybes],vi=0,VI=vs.length; vi<VI; ++vi ) {
						// get indices of v in both ALSs, none of which may be
						// in the overlap, even if allowOverlaps==true. That's
						// the restriction in Restricted Common Candidate, coz
						// then v must be in one ALS or the other (never both).
//inline for speed: call no methods (original code retained as documentation)
//						if ( bothVs.setOr(a.vs[v=vs[vi]], b.vs[v]).andNone(overlap)
//						  // v is an RC if all v's in both ALSs see each other
//						  // * bothVs        allVsInBothAlss
//						  // * andEqualsThis areAllIn
//						  // * vAlls         vsPlusTheirBudsCommonToBothAlss
//						  //   retains each v only if ALL other vsInBothAlss
//						  //   see it (and also external v's seen by all v's
//						  //   in both ALSs). vAlls is pretty tricky!
//						  // && bothVs areAllIn vsPlusTheirBudsCommonToBothAlss
//						  && bothVs.andEqualsThis(vAlls.setAnd(a.vAll[v], b.vAll[v])) )
						// bothVs.setOr(a.vs[v=vs[vi]], b.vs[v])
						bv0 = (ai=a.vs[v=vs[vi]]).a0 | (bi=b.vs[v]).a0;
						bv1 = ai.a1 | bi.a1;
						bv2 = ai.a2 | bi.a2;
						// .andNone(overlap)
						if ( ( (bv0&ol0) | (bv1&ol1) | (bv2&ol2) ) == 0
						  // && bothVs.andEqualsThis(vAlls.setAnd(a.vAll[v], b.vAll[v]))
						  // there's no vAlls var here, it's just calc'd inline
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
			, final boolean allowOverlaps, final boolean useStartAndEnd) {
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
//		final Idx overlap = this.overlap;//.clear();
//		final Idx bothVs = this.bothVs;//.clear();
//		final Idx vAlls = this.VAlls;//.clear();
		// foreach pair of ALSs, distinct or otherwise, excluding masturbation.
		for ( i=0; i<numAlss; ++i ) {
			a = ALSS[i]; // nb: alss is an ArrayList with O(1) get
			startInds[i] = numRccs;
			for ( j=0; j<numAlss; ++j )
				// get maybes common to als1 and als2, skip if none
				if ( (cmnMaybes=(a.maybes & (b=ALSS[j]).maybes)) != 0
				  // see if the ALSs overlap (we need overlap later anyway)
//inline for speed: call no methods (original code retained as documentation)
//				  && (overlap.setAnd(a.idx, b.idx).none() || allowOverlaps) )
				  && ( ( ( (ol0=(ai=a.idx).a0 & (bi=b.idx).a0)
					     | (ol1=ai.a1 & bi.a1)
					     | (ol2=ai.a2 & bi.a2) ) == 0 )
					|| allowOverlaps )
				  && j != i ) // exclude masturbation.
					// foreach maybe common to a and b
					// rcc=null to tell between 1st and 2nd value of an RCC
					// vs is an array of the values in cmnMaybes
					// the rest is a boring for loop
					for ( rcc=null,vs=VALUESES[cmnMaybes],vi=0,VI=vs.length; vi<VI; ++vi ) {
						// get indices of v in both ALSs, none of which may be
						// in the overlap, even if allowOverlaps==true. That's
						// the restriction in Restricted Common Candidate, coz
						// then v must be in one ALS or the other (never both).
//inline for speed: call no methods (original code retained as documentation)
//						if ( bothVs.setOr(a.vs[v=vs[vi]], b.vs[v]).andNone(overlap)
//						  // v is an RC if all v's in both ALSs see each other
//						  // * bothVs        allVsInBothAlss
//						  // * andEqualsThis areAllIn
//						  // * vAlls         vsPlusTheirBudsCommonToBothAlss
//						  //   retains each v only if ALL other vsInBothAlss
//						  //   see it (and also external v's seen by all v's
//						  //   in both ALSs). vAlls is pretty tricky!
//						  // && bothVs areAllIn vsPlusTheirBudsCommonToBothAlss
//						  && bothVs.andEqualsThis(vAlls.setAnd(a.vAll[v], b.vAll[v])) )
						// bothVs.setOr(a.vs[v=vs[vi]], b.vs[v])
						bv0 = (ai=a.vs[v=vs[vi]]).a0 | (bi=b.vs[v]).a0;
						bv1 = ai.a1 | bi.a1;
						bv2 = ai.a2 | bi.a2;
						// .andNone(overlap)
						if ( ( (bv0&ol0) | (bv1&ol1) | (bv2&ol2) ) == 0
						  // && bothVs.andEqualsThis(vAlls.setAnd(a.vAll[v], b.vAll[v]))
						  // there's no vAlls var here, it's just calc'd inline
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

}
