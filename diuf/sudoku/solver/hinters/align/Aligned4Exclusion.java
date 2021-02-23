/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.gen.IInterruptMonitor;


/**
 * ============================================================================
 *                              See A5E_1C first
 *                       it's full of nice big comments
 * ============================================================================
 *
 * Aligned Quad Exclusion.
 * <p>Implements the Aligned Set Exclusion Sudoku solving technique for sets
 * of 4 Cells. This "_1E" implementation "correctly" requires one common
 * excluder cell per aligned set, so it finds one-and-a-half times as many hints
 * as the hacked version (_2E) but takes ten times as long to run. I use A4E_1
 * because it's fast enough... A5E_1 is ok, A6E_1 is barely fast enough, and
 * A7E_1C is too slow to wait for... A8+E are just dreamscape. I must find out
 * how long 9#top1465.d5.mt (my slowest puzzle) takes to solve with all _1C's.
 * <p>I run fast. I'm NOT impressive. Some of this code, like this _1C vs
 * _2H debacle, is a bloody mess that I wouldn't inflict upon a professional,
 * but this isn't a professional codebase, this is MY codebase, and I'm trying
 * to make it bloody fast, so yuz can all go and get, well you know.
 * <p>KRC 2019-09-11 relegated AlignedSetExclusion to the abstract class
 * AAlignedSetExclusionBase and modified Aligned3Exclusion, Aligned4Exclusion,
 * and Aligned5Exclusion to use the protected methods exposed therein, to
 * mostly clean-up my cut-and-paste coding. Also moved A5E to just before
 * DynamicPlus in the LogicalSolver chainers list. It's still too slow, but
 * that doesn't matter to me so much any more.
 * <p>KRC 2019-09-25 Rewrote Aligned*Exclusion to use the simple loop through
 * each cells maybes approach, as apposed to the Permutations approach. It's
 * faster because Permutations is slow, and because value-collisions between
 * sibling-candidate cells are dealt with ONCE only; ie we don't have to add
 * an excludedCombosMap entry for every combination of the other-non-sibling
 * maybes just because for example A1 and A3 can't contain the same value.
 * The hints are actually far more succinct and therefore readable, especially
 * for bigger sets: Quads and up.
 * <p>KRC 2019-10-02 abort when hackTop1465 (which includes IS_HACKY) and
 * MAX_CANDIDATES is exceeded.<br>
 * 2019-10-02 A4E 1465 in 344,699,776,511 (05:45) @ 235,289,949 ns/puzzle.<br>
 * FYI: Juillerat took 21:54 with no A4E, so we're looking pretty good now.
 * <p>KRC 2019-11-13 just a check-up on how fast we're running now.
 * <p>KRC 2019-11-29 isNotSiblingOf array. Loop while sv &lt;= maybes-bits.
 * <p>KRC 2020-01-07 Loop over SHIFTED_VALUES_ARRAY element
 * <pre>
 * 2019-11-13 A4E 1465 in 291,204,883,470 (04:51) @ 198,774,664 ns/puzzle.
 * 2019-11-29 A4E 1465 in 284,209,342,672 (04:44) @ 193,999,551 sv &lt;= maybes
 * </pre>
 */
public final class Aligned4Exclusion extends AAlignedSetExclusionBase
//		implements diuf.sudoku.solver.IReporter
//				 , java.io.Closeable
{
	private static final int MIN_CANDIDATES = 20; // <HACK/>

	// the maximim number of candidates to permute (process).
	private static final int MAX_CANDIDATES = 60; // <HACK/>

	private static final int NUM_CMN_EXCLS = 16;
	private static final Cell[] commonExcludersArray = new Cell[NUM_CMN_EXCLS];
	private static final int[] commonExcluderBitsArray = new int[NUM_CMN_EXCLS];

	private static final Cell[] cellsArray = new Cell[4];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3

	public Aligned4Exclusion(int firstHintNumber, IInterruptMonitor monitor) {
		super(Tech.AlignedQuad, firstHintNumber, monitor);
		assert tech.isAligned;
		assert degree == 4;
	}

//	@Override
//	public void close() throws java.io.IOException {
//		hits.save();
//	}

	/**
	 * getHints: And now for something completely different.
	 * <p>
	 * This method just orchestrates finding Aligned Quad Exclusions. All (well
	 * most of) the variables are fields rather than passing around everywhere.
	 * The trick is that allowOrExclude goes through over a billion loops for
	 * top1465.d5.mt, so the rest of this is really just setting stuff up; and
	 * trying to avoid having to call the little ____er.
	 *
	 * @param grid to examine
	 * @param accu to add any hints to.\
	 * @return was a hint found.
	 * @return did it find any hint/s.
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// localise this variable for speed (and make it final).
		// hackTop1465 is isHacky && filePath.contains("top1465")
		final boolean hackTop1465 = AHinter.hackTop1465;
		// istop1465 is just the filePath.contains("top1465")
		// I use it to filter when not isHacky, otherwise takes 34:16:04
		// for top1465, and I'm not waiting that long again. It sux!
		final boolean isTop1465 = grid.source!=null && grid.source.isTop1465;
		// grid.source.lineNumber ie which puzzle are we currently solving
		final int gsl = grid.source!=null ? grid.source.lineNumber : 0;

		// localise fields for speed (if it's referenced more than three times)
		final int degree = this.degree;
		final int hintNum = AHint.hintNumber;
		final boolean useHits = this.useHits;

		// get an array of the Cells at which we hinted last time;
		// otherwise we skip this call to getHints
		final Cell[] hitCells = useHits // only true when AHinter.hackTop1465
				? hits.getHitCells(gsl, hintNum, degree, grid)
				: null;
		final boolean hitMe = hitCells != null;
		if ( useHits && !hitMe )
			return false; // no hints for this puzzle/hintNumber

		// shiftedValueses: an array of jagged-arrays of the shifted-values
		// that are packed into a maybes.bits 0..511. See Values.SHIFTED.
		final int[][] SVS = Values.VSHIFTED;

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 1 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = candidatesArray;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = excludersArray;

		// an array of the cells in each aligned set. Note that the array is
		// reused, not recreated for each aligned set.
		final Cell[] cells = cellsArray;

		// the number of cells in each of the for-loops
		final int n0, n1, n2;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = commonExcludersArray;
		// the common-excluder-cells-maybes-bits. This set differs from the
		// common-excluder-cells in that supersets & disjuncts are removed
		// from cmnExclBits WITHOUT modifying cmnExcls; which is required
		// "as is" to build the hint.
		final int[] cmnExclBits = commonExcluderBitsArray; // observed 6
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// array of 3*27-bit-bitsets of indexes of c0's excluders in Grid.cells
		Idx idx0;

		// this is a perfectly nice comment. please retain it. pat a dog.
		int i0, i1, i2, i3;

		// a virtual array of the 4 candidate cells in an aligned exclusion set.
		Cell c0, c1, c2, c3;

		// isNotSiblingOf cache: eg: s01=!c0.isNotSiblingOf(c1)
		boolean ns10, ns20, ns30
		            , ns21, ns31
		                  , ns32;

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is more code than just
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		int c0b, c1b , c2b , c3b ; // c3b  = c3.maybes.bits;
		int		       c2b0, c3b0; // c3b0 = s03 ? c3b  & ~sv0 : c3b;
		int			         c3b1; // c3b1 = s13 ? c3b0 & ~sv1 : c3b0;
		int			         c3b2; // c3b2 = s23 ? c3b1 & ~sv2 : c3b1;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2
		int sv01, sv02, sv03;

		// allowedValuesBitsets: values that've been allowed in each position
		int avb0, avb1, avb2, avb3;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // the number of cells in the aligned set.

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1, any2, any3;

		// commonExcluderBits[0]: the maybes.bits of the first common excluder
		int ceb0, ceb1, ceb2;

		// presume that no hint will be found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//                        there's the kick-off
		//                          and oooh bugger!
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also the set of sibling excluder-cells for each candidate.
		numCandidates = populateCandidatesAndExcluders(
				candidates, excluders, grid);
		if ( numCandidates < degree )
			return false; // this'll never happen. Never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

		n0 = numCandidates - 3;
		n1 = numCandidates - 2;
		n2 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			// get c0 and an index-into-Grid.cells of c0's excluders.
			idx0 = excluders[(c0=cells[0]=candidates[i0]).i].idx();
			if(hitMe && c0!=hitCells[0]) continue;
			c0b = c0.maybes.bits;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				// get c1 and index of excluders common to c1 and c0.
				if ( excluders[(c1=cells[1]=candidates[i1]).i].idx1(idx01, idx0) )
					continue;
				if(hitMe && c1!=hitCells[1]) continue;
				c1b = c1.maybes.bits;
				ns10 = c1.notSees[c0.i];
				for ( i2=i1+1; i2<n2; ++i2 ) {
					// get c2 and index of excluders common to c0,c1,c2
					// skip if the index is empty (ie there are no common excluders).
					if ( excluders[candidates[i2].i].idx1(idx02, idx01) )
						continue;
					c2 = cells[2]=candidates[i2];
					if(hitMe && c2!=hitCells[2]) continue;
					c2b = c2.maybes.bits;
					ns20 = c2.notSees[c0.i];
					ns21 = c2.notSees[c1.i];
					if ( isInterrupted() )
						return false;
					for ( i3=i2+1; i3<numCandidates; ++i3 ) {
						// get c3 and index of excluders common to c0,c1,c2,c3
						// skip if the index is empty (ie there are no common excluders).
						if ( excluders[candidates[i3].i].idx1(idx03, idx02) )
							continue;
						c3 = cells[3] = candidates[i3];
						if(hitMe && c3!=hitCells[3]) continue;

						// read common excluder cells from grid at idx03
						if ( (numCmnExcls = idx03.cellsN(grid, cmnExcls)) == 1 ) {
							cmnExclBits[0] = cmnExcls[0].maybes.bits;
							numCmnExclBits = 1;
						} else {
							// performance enhancement: examine smaller maybes sooner.
							//KRC#2020-06-30 10:20:00 bubbleSort is a bit faster
							//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
							bubbleSort(cmnExcls, numCmnExcls);

							// get common excluders bits, removing any supersets,
							// eg: {12,125}=>{12} coz 125 covers 12 so 125 does nada.
							numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);
						}

// This is actually slower in A4E.
// disdisjunct 10:04, disuselessenate 10:57, none 9:49
//						// remove each common excluder bits which contains a
//						// value that is not in any cells maybes, coz no combo
//						// can cover it, so it can't contribute to an exclusion.
//						// skip if there are no common excluders remaining.
//						// 267,283,801 of 365,467,445 skip 98,183,644 = 26.87%
//						if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
//								, c0b|c1b|c2b|(c3b=c3.maybes.bits))) == 0 )
//							continue;

						c3b=c3.maybes.bits;

						// complete the isSiblingOf cache
						ns30 = c3.notSees[c0.i];
						ns31 = c3.notSees[c1.i];
						ns32 = c3.notSees[c2.i];

						// clear the allowedValuesBitsets virtual array
						avb0=avb1=avb2=avb3 = 0;
						// reset anyLevel so no shortcircuit until avb4==c4b
						anyLevel = degree;

						// cache the first common excluder cells maybes.bits
						ceb0 = cmnExclBits[0];

						// The dog____ing loop works-out is each combo allowed?
						// ie: Is each possible combination of the potential values
						// of these $degree (4) cells allowed? (else the combo has
						// been excluded, so it is skipped). We only populate the
						// allowedValuesBitsets virtual array here, we'll build the
						// excludedCombosMap later, only if value/s are excluded.
						// There are 2 exclusion rules:
						// (1) Hidden Single: Using the same value for two cells
						//     is allowed only if the cells do not share a region.
						// (2) common excluder rule: If the combo covers
						//     (is a superset of) any common excluder cells maybes
						//     then the combo is not allowed, because the excluder
						//     cell must be one of those values (no matter which).
						// nb: it's faster to "pre-eliminate" ie remove maybes from
						// sibling cells for large (4+) sets than "skip collisions"
						// as in A23E; because we do more of the work less often.
						// Contrast DOG_1 with A3E's DOG_1 to see what I'm on about.
						// nb: on that nb: 4 isn't really a "large set", 7+ is a
						// "large set" but I'm applying (most of) the same techniques
						// all the way down to A4E coz I'm tryna get this dog movin,
						// and it's good, if you have to non-genericise everything
						// for speed like I've done with the A*E's, to atleast keep
						// them as similar as possible, so that they can be modified
						// with search & replace coding, rather than have to think
						// seperately about each and every one of them.

						// nb: this switch avoids calling the covers method just for
						// speed, the default path works for any numCmnExclBits, it's
						// just a bit slow Redge. It's under the bonnet son. One does
						// NOT call methods in a tight loop (or create variables),
						// unless of course there is no alternative, and there's
						// almost always an alternative which you haven't thought of
						// yet because it's contrary to coding convention.
++counts[0];
++counts[numCmnExclBits];
						switch ( numCmnExclBits ) {
						case 1:
							// we don't need a common excluders loop
							//  w/o HitSet: 1 = 250,017,061 of 273,342,654 = 91.47%
							// with HitSet: 1 = 620 of 1,089 = 56.93%
							DOG_1a: for ( int sv0 : SVS[c0b] ) {
								// (1) hidden single rule (pre-elimination)
								c3b0 = ns30 ? c3b : c3b & ~sv0;
								c2b0 = ns20 ? c2b : c2b & ~sv0;
								any1 = false;
								SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
									sv01 = sv0 | sv1;
									c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
									any2 = false;
									SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
										// (2) common excluder rule
										if ( (ceb0 & ~(sv02=sv01|sv2)) == 0
										  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
											continue;
										any3 = false;
										SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
											if ( (ceb0 & ~(sv02|sv3)) == 0 )
												continue;
											// Combo Allowed!
											any3 = true;
											// add to allowedValuesBitsets
											// and check if that makes all allowed.
											if ( (avb3|=sv3) != c3b )
												continue;
											// We allow only the FIRST combo at each level
											switch ( anyLevel ) {
											case 4: anyLevel=3; break SV3_LOOP;
											case 3: break SV3_LOOP;
											case 2: any2=true; break SV2_LOOP;
											case 1: any1=true; break SV1_LOOP;
											} // end-switch
										} // next sv3
										if ( any3 ) {
											any2 = true;
											if ( (avb2|=sv2)==c2b && anyLevel==3 )
												anyLevel = 2;
										}
									} // next sv2
									if ( any2 ) {
										any1 = true;
										if ( (avb1|=sv1)==c1b && anyLevel==2 )
											anyLevel = 1;
									}
								} // next sv1
								if(any1) avb0|=sv0;
							} // next sv0
							break;
						case 2:
							// we still don't need a common excluders loop
							//  w/o HitSet: 2 = 20,564,461 of 273,342,654 = 7.52%
							// with HitSet: 2 = 415 of 1,089 = 38.11%
							ceb1 = cmnExclBits[1];
							DOG_1b: for ( int sv0 : SVS[c0b] ) {
								// (1) hidden single rule (pre-elimination)
								c3b0 = ns30 ? c3b : c3b & ~sv0;
								c2b0 = ns20 ? c2b : c2b & ~sv0;
								any1 = false;
								SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
									sv01 = sv0 | sv1;
									c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
									any2 = false;
									SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
										// (2) common excluder rule
										if ( (ceb0 & ~(sv02=sv01|sv2)) == 0
										  || (ceb1 & ~sv02) == 0
										  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
											continue;
										any3 = false;
										SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
											if ( (ceb0 & ~(sv03=sv02|sv3)) == 0
											  || (ceb1 & ~sv03) == 0 )
												continue;
											// Combo Allowed!
											any3 = true;
											// add to allowedValuesBitsets
											// and check if that makes all allowed.
											if ( (avb3|=sv3) != c3b )
												continue;
											// We allow only the FIRST combo at each level
											switch ( anyLevel ) {
											case 4: anyLevel=3; break SV3_LOOP;
											case 3: break SV3_LOOP;
											case 2: any2=true; break SV2_LOOP;
											case 1: any1=true; break SV1_LOOP;
											} // end-switch
										} // next sv3
										if ( any3 ) {
											any2 = true;
											if ( (avb2|=sv2)==c2b && anyLevel==3 )
												anyLevel = 2;
										}
									} // next sv2
									if ( any2 ) {
										any1 = true;
										if ( (avb1|=sv1)==c1b && anyLevel==2 )
											anyLevel = 1;
									}
								} // next sv1
								if(any1) avb0|=sv0;
							} // next sv0
							break;
						case 3:
							// we still don't need a common excluders loop
							//  w/o HitSet: 3 = 2,450,203 of 273,342,654 = 0.90%
							// with HitSet: 3 = 44 of 1,089 = 4.04%
							ceb1 = cmnExclBits[1];
							ceb2 = cmnExclBits[2];
							DOG_1c: for ( int sv0 : SVS[c0b] ) {
								// (1) hidden single rule (pre-elimination)
								c3b0 = ns30 ? c3b : c3b & ~sv0;
								c2b0 = ns20 ? c2b : c2b & ~sv0;
								any1 = false;
								SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
									sv01 = sv0 | sv1;
									c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
									any2 = false;
									SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
										// (2) common excluder rule
										if ( (ceb0 & ~(sv02=sv01|sv2)) == 0
										  || (ceb1 & ~sv02) == 0
										  || (ceb2 & ~sv02) == 0 )
											continue;
										if ( (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
											continue;
										any3 = false;
										SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
											if ( (ceb0 & ~(sv03=sv02|sv3)) == 0
											  || (ceb1 & ~sv03) == 0
											  || (ceb2 & ~sv03) == 0 )
												continue;
											// Combo Allowed!
											any3 = true;
											// add to allowedValuesBitsets
											// and check if that makes all allowed.
											if ( (avb3|=sv3) != c3b )
												continue;
											// We allow only the FIRST combo at each level
											switch ( anyLevel ) {
											case 4: anyLevel=3; break SV3_LOOP;
											case 3: break SV3_LOOP;
											case 2: any2=true; break SV2_LOOP;
											case 1: any1=true; break SV1_LOOP;
											} // end-switch
										} // next sv3
										if ( any3 ) {
											any2 = true;
											if ( (avb2|=sv2)==c2b && anyLevel==3 )
												anyLevel = 2;
										}
									} // next sv2
									if ( any2 ) {
										any1 = true;
										if ( (avb1|=sv1)==c1b && anyLevel==2 )
											anyLevel = 1;
									}
								} // next sv1
								if(any1) avb0|=sv0;
							} // next sv0
							break;
						default:
							// I give up: we need a common excluders loop,
							// but I'm still not going to call any methods.
							// 3 = 474,662 of 102,167,989 = 0.46%
							//  w/o HitSet: 4 = 286,676 of 273,342,654 = 0.10%
							//  w/o HitSet: 5 = 22,879 of 273,342,654 = 0.01%
							//  w/o HitSet: 6 = 1,374 of 273,342,654 = 0.00%
							// with HitSet: 4 = 8 of 1,089 = 0.73%
							// with HitSet: 5 = 2 of 1,089 = 0.18%
							// I'm suprised that there's no hits for 6
							DOG_1d: for ( int sv0 : SVS[c0b] ) {
								// (1) hidden single rule (pre-elimination)
								c3b0 = ns30 ? c3b : c3b & ~sv0;
								c2b0 = ns20 ? c2b : c2b & ~sv0;
								any1 = false;
								SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
									sv01 = sv0 | sv1;
									c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
									any2 = false;
									SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
										if ( covers(cmnExclBits, numCmnExclBits, sv02=sv01|sv2) )
											continue;
										if ( (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
											continue;
										any3 = false;
										SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
											if ( covers(cmnExclBits, numCmnExclBits, sv03=sv02|sv3) )
												continue;
											// Combo Allowed!
											any3 = true;
											// add to allowedValuesBitsets
											// and check if that makes all allowed.
											if ( (avb3|=sv3) != c3b )
												continue;
											// We allow only the FIRST combo at each level
											switch ( anyLevel ) {
											case 4: anyLevel=3; break SV3_LOOP; // any3 is already true
											case 3: break SV3_LOOP; // any3 is already true; and anyLevel is already 3, obviously.
											case 2: any2=true; break SV2_LOOP;
											case 1: any1=true; break SV1_LOOP;
											} // end-switch
										} // next sv3
										if ( any3 ) {
											any2 = true;
											if ( (avb2|=sv2)==c2b && anyLevel==3 )
												anyLevel = 2;
										}
									} // next sv2
									if ( any2 ) {
										any1 = true;
										if ( (avb1|=sv1)==c1b && anyLevel==2 )
											anyLevel = 1;
									}
								} // next sv1
								if(any1) avb0|=sv0;
							} // next sv0
						} // end-switch

						// return is any value excluded (ie NOT allowed)?
						// ie are we going on to produce a hint?
						if ( avb0 == c0b
						  && avb1 == c1b
						  && avb2 == c2b
						  && avb3 == c3b )
							continue;

						// ===========================================
						// Performance is not an issue from here down.
						// To get here a value/s is excluded.
						// ===========================================

++hintCounts[numCmnExclBits];
						if ( isTop1465 )
							hits.add(gsl, hintNum, cells);

						// build the red (removable) potentials.
						Pots redPots = createRedPotentials(cells
								, avb0,avb1,avb2,avb3);
						assert !redPots.isEmpty();
						if ( redPots.isEmpty() )
							continue; // should never happen. Never say never.
						// build the excluded combos map for the hint
						ExcludedCombosMap map = buildExcludedCombosMap(
								cmnExcls, numCmnExcls, cells, redPots);
						// create the hint (and possibly return)
						AHint hint = new AlignedExclusionHint(
								  this, redPots, cells, numCmnExcls, cmnExcls, map);
						result = true; // in case add returns false
						if ( accu.add(hint) )
							return true;

					} // next c3
				} // next c2
			} // next c1
		} // next c0
		return result;
	}

	/**
	 * buildExcludedCombosMap adds the values of a combo (a permutation of
	 * potential cell values) which are "excluded" to map, and returns the
	 * ExcludedCombosMap for use in the resulting hint.
	 * Allowed means the cells may (AFAIK) contain this combination of values;
	 * excluded means that these cells cannot be this combination of values,
	 * and (optionally) here's the "locking" or "excluding" Cell.
	 * <p>This method is called when we already know that at least one value is
	 *  NOT allowed (ie excluded), ergo only when we already know that we're
	 *  going to create a hint, which means 1422 times in top1465.d5.mt, so the
	 *  performance imperatives inherent in DOG_1 (called over a billion times)
	 *  aren't an issue here, so we do things more straight-forwardly.
	 * @return a new {@code LinkedHashMap<HashA, Cell>} containing the excluded
	 *  combinations of the cells values.
	 */
	private static ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcls, int numCmnExcls
			, Cell[] cells, Pots redPots) {
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, redPots);
		final Cell c0=cells[0], c1=cells[1], c2=cells[2], c3=cells[3];

		// ALL other variables (create none in the loop)
		int i; // i general purpose index
		int combo; // used to substract combo from cmnExcls maybes

		int sv0,sv01,sv02;

		// Foreach distinct combo of 4 potential values of our 4 cells:
		// check if the combo "covers" the maybes of any common excluder cell,
		// ie the combo is a superset of the excluders maybes. The excluder doesn't need ALL the combo values, it just can't have
		// any OTHER potential values.
		// NB: Performance isn't a problem. This method is called maybe ?50?
		// times in top1465, only when we're producing a hint.
		DOG_2: for ( int v0 : c0.maybes ) { // anything is fast enough for a small enough n, even an iterator
			sv0 = VSHFT[v0];
			for ( int v1 : c1.maybes ) {
				if ( v1==v0 && !c1.notSees[c0.i] ) {
					map.put(new HashA(v0,v1,0,0), null);
					continue;
				}
				sv01 = sv0 | VSHFT[v1];
				for ( int v2 : c2.maybes ) {
					if ( v2==v0 && !c2.notSees[c0.i] ) {
						map.put(new HashA(v0,0,v2,0), null);
						continue;
					} else if ( v2==v1 && !c2.notSees[c1.i] ) {
						map.put(new HashA(0,v1,v2,0), null);
						continue;
					}
					sv02 = sv01 | VSHFT[v2];
					for ( int v3 : c3.maybes ) {
						if ( v3==v0 && !c3.notSees[c0.i] ) {
							map.put(new HashA(v0,0,0,v3), null);
							continue;
						} else if ( v3==v1 && !c3.notSees[c1.i] ) {
							map.put(new HashA(0,v1,0,v3), null);
							continue;
						} else if ( v3==v2 && !c3.notSees[c2.i] ) {
							map.put(new HashA(0,0,v2,v3), null);
							continue;
						}
						combo = sv02 | VSHFT[v3];
						for ( i=0; i<numCmnExcls; ++i )
							if ( (cmnExcls[i].maybes.bits & ~combo) == 0 ) {
								map.put(new HashA(v0,v1,v2,v3)
										, cmnExcls[i]);
								break; // we want only the first excluder of each combo
							}
						// all values are allowed! we've just got nothing to do
						// with allowed values (we're only interested in the
						// excluded ones), so we do nothing.
					} // next v3
				} // next v2
			} // next v1
		} // next v0
		assert !map.isEmpty();
		return map;
	}

}
