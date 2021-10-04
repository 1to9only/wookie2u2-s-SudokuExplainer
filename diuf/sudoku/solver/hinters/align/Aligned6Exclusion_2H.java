/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.io.IO;
import diuf.sudoku.solver.LogicalSolver;


/**
 * ============================================================================
 *                              See A5E_1C first
 *                       it's full of nice big comments
 * ============================================================================
 *
 * Aligned Hex (6 cells) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 * of 6 Cells, and there 6 values.
 * <p>2019-09-30 20:18 KRC I've just copy-pasted Aligned5Exclusion with the
 * idea that I'll just try 6, because 5 wasn't bad enough. I just want to see.
 * <p>2019-11-29 KRC isNotSiblingOf array. Loop while sv &lt;= maybes-bits.
 * <pre>
 * 2019-09-30 A6E 1465 in 1,277,065,852,379 (21:17) @ 871,717,305 impressive.
 * 2019-10-02 A6E 1465 in 1,247,960,738,508 (20:47) @ 851,850,333 IS_HACKY
 * 2019-11-13 A6E 1465 in   998,507,576,340	(16:38) @ 681,575,137 covers
 * 2019-11-29 A6E 1465 in   686,845,773,561 (11:26) @ 468,836,705 sv &lt;= maybes
 * </pre>
 * <p>2020-02-03 KRC sort with collisionComparator was SLOWER, so reverted. It
 * was 15:50 orig, 22:35 sort, and 13:50 reverted except for any*.
 */
public final class Aligned6Exclusion_2H extends Aligned6ExclusionBase
implements
		//diuf.sudoku.solver.IReporter,
		java.io.Closeable
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 27; // <HACK/>

	// the maximim number of candidates to permute (process).
	private static final int MAX_CANDIDATES = 61; // <HACK/>

	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[6];
//	private static final Cell[] CELLS_ARRAY_1 = new Cell[6];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5

//	private final ACollisionComparator cc = new ACollisionComparator();

	private final NonHinters nonHinters = new NonHinters(16*1024, 4);

	public Aligned6Exclusion_2H() {
		super(IO.A6E_2H_HITS);
	}

	@Override
	public void close() throws java.io.IOException {
		hits.save();
	}

	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
		nonHinters.clear();
	}

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
		final int hintNum = grid.hintNumber;
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
		// that are packed into your maybes 0..511. See Values for more.
		final int[][] SVS = Values.VSHIFTED;

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybesSize>=2 and has 2 excluders with maybesSize 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// the number of cells in each for-loop (ie the i vars)
		final int n0, n1, n2, n3, n4;

		// an array of the 6 cells in an aligned set, ready for exclusion.
		final Cell[] cells = CELLS_ARRAY;

//KRC#2020-06-30 09:50:00
//		// sortedCells the above cells array sorted by the CollisionComparator
//		final Cell[] scells = CELLS_ARRAY_1;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// bitset of positions of c0's excluder cells in Grid.cells
		Idx idx0;

		// indexes in the candidates array of the 5 candidate cells.
		int i0, i1, i2, i3, i4, i5;

		// a virtual array of the 6 candidate cells in an aligned exclusion set.
		Cell c0, c1, c2, c3, c4, c5;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01,sv02,sv03,sv04,sv05;

		// allowedValuesBitsets: an array of bitsets of values allowed in scells
		int avb0, avb1, avb2, avb3, avb4, avb5;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // the number of cells in the aligned set.

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is messier than just
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		int	c0b , c1b , c2b , c3b , c4b , c5b ;
		int	            c2b0, c3b0, c4b0, c5b0;
		int		              c3b1, c4b1, c5b1;
		int				      c3b2, c4b2, c5b2;
		int					        c4b3, c5b3;
		int							      c5b4;

		// notSibling cache: ns51 = c5.isNotSiblingOf[c4.i]
		boolean ns54, ns53, ns52, ns51, ns50;
		boolean       ns43, ns42, ns41, ns40;
		boolean             ns32, ns31, ns30;
		boolean                   ns21, ns20;
		boolean                         ns10;

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1, any2, any3, any4, any5;

		// commonExcluderBits[0], you can workout ceb1 for yourself.
		// ces0 is size[commonExcluderBits[0]]
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// should combo be "covers"-tested after adding 2,3,4 values
		boolean do30,do31,do32, do40,do41,do42, do50,do51,do52;

		// presume failure, ie presume we're not going to find a hint.
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//         and there off and wobbling in the fat Counter's marathon
		//              (there's a Mc____wits at the finishline)
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// this for the bloody debugger which insists that each variable be set
		// before it'll evaluate it. Just assume 0 yafugginknobjinas!
		c2b0=c3b0=c4b0=c5b0=c3b1=c4b1=c5b1=c4b2=c5b2=c4b3=c5b3=c5b4 = 0;

		// search for candidate cells that can contribute to an exclusion set.
		// NB: Each candidate cell needs at-least 2 excluder cells. Hopefully
		//     this will reduce the number of candidates by a few. Was 62!
		numCandidates = populateCandidatesAndAtleastTwoExcluders(
				candidates, excluders, grid);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

		n0 = numCandidates - 5;
		n1 = numCandidates - 4;
		n2 = numCandidates - 3;
		n3 = numCandidates - 3;
		n4 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
//KRC#2020-06-30 09:50:00
			c0b = (c0=cells[0]).maybes;
			// get each sortedCell and it's potential values
			for ( i1=i0+1; i1<n1; ++i1 ) {
				if ( excluders[(cells[1]=candidates[i1]).i].idx2(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
//KRC#2020-06-30 09:50:00
				c1b = (c1=cells[1]).maybes;
				ns10 = c1.notSees[c0.i];

				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx2(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
//KRC#2020-06-30 09:50:00
					c2b = (c2=cells[2]).maybes;
					ns21 = c2.notSees[c1.i];
					ns20 = c2.notSees[c0.i];

					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[(cells[3]=candidates[i3]).i].idx2(idx03, idx02) )
							continue;
						if(hitMe && cells[3]!=hitCells[3]) continue;
//KRC#2020-06-30 09:50:00
						c3b = (c3=cells[3]).maybes;
						ns32 = c3.notSees[c2.i];
						ns31 = c3.notSees[c1.i];
						ns30 = c3.notSees[c0.i];

						for ( i4=i3+1; i4<n4; ++i4 ) {
							if ( excluders[(cells[4]=candidates[i4]).i].idx2(idx04, idx03) )
								continue;
							if(hitMe && cells[4]!=hitCells[4]) continue;
							interrupt();
//KRC#2020-06-30 09:50:00
							c4b = (c4=cells[4]).maybes;
							ns43 = c4.notSees[c3.i];
							ns42 = c4.notSees[c2.i];
							ns41 = c4.notSees[c1.i];
							ns40 = c4.notSees[c0.i];

							for ( i5=i4+1; i5<numCandidates; ++i5 ) {
								if ( excluders[(cells[5]=candidates[i5]).i].idx2(idx05, idx04) )
									continue;
								if(hitMe && cells[5]!=hitCells[5]) continue;

								if ( nonHinters.skip(cells) )
									continue;

								// read common excluder cells from grid at idx05
								if ( (numCmnExcls = idx05.cellsN(grid, cmnExcls)) < 2 )
									continue;
								// performance enhancement: examine smaller maybes sooner.
//KRC#2020-06-30 10:20:00
//// cells only
//// Tim.small            :     25,203,573,400	   4473	     5,634,601	      2	12,601,786,700	Aligned Hex
//// w/o sort             :     27,832,412,400	   4473	     6,222,314	      2	13,916,206,200	Aligned Hex
//// Bubble-sort          :     36,958,448,600	   4473	     8,262,563	      2	18,479,224,300	Aligned Hex // Slower: Differs from A5E.
// cmnExcls/cells
// Tim.small/no-sort      :     27,832,412,400	   4473	     6,222,314	      2	13,916,206,200	Aligned Hex
// Bubble-Sort/no-sort    :     23,443,582,500	   4473	     5,241,131	      2	11,721,791,250	Aligned Hex
// redo                   :     27,382,249,500	   4473	     6,121,674	      2	13,691,124,750	Aligned Hex
// Bubble-Sort/Bubble-Sort:     27,139,758,600	   4473	     6,067,462	      2	13,569,879,300	Aligned Hex
// Bubble-Sort/Tim.small  :     25,682,424,800	   4473	     5,741,655	      2	12,841,212,400	Aligned Hex
// So Bubble-Sort cmnExcls and no-sort cells is the fastest, and that's that.
//								MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
								bubbleSort(cmnExcls, numCmnExcls);

								// get common excluders bits, removing any supersets,
								// eg: {12,125}=>{12} coz 125 covers 12, so 125 does nada.
								numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);

								// remove each common excluder bits which contains a value that
								// is not in any cells maybes, because no combo can cover it,
								// so it can't contribute to any exclusions.
								// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
								// skip if there are no common excluders remaining.
								if ( (numCmnExclBits = disdisjunct(cmnExclBits
										, numCmnExclBits, allMaybesBits(cells))) == 0 )
									continue;

								// the dog____ing algorithm is faster with dodgem-cars to the left,
								// but the above for-i-loops need a static cells array; so we copy
								// cells to scells (sortedCells) and sort that array DESCENDING by:
								// 4*maybesCollisions + 2*cmnExclHits + maybesSize

//								// performance enhancement: examine smaller maybes sooner.
////KRC#2020-06-30 09:50:00
//Q: Is it faster to sort the cells?
//// Tim.small  :     25,203,573,400	   4473	     5,634,601	      2	12,601,786,700	Aligned Hex
//// w/o sort   :     27,832,412,400	   4473	     6,222,314	      2	13,916,206,200	Aligned Hex
//// Bubble-sort:     36,958,448,600	   4473	     8,262,563	      2	18,479,224,300	Aligned Hex // Slower: Differs from A5E.
//A: The fastest is MyTimSort.small, then w/o sort, then Bubble-Sort; ie all
//completely different to A5E_1C, so either I'm an idiot or something smells
//fishy! So I shall rerun w/o sort, then Bubble-sort, then Tim.small to see if
//produces consistent results. I'm not real hopeful, and I hate Javas JIT
//compiler, which can never produce the same program twice.
//								cc.set(cells, cmnExclBits, numCmnExclBits);
//								System.arraycopy(cells, 0, scells, 0, degree);
//								//MyTimSort.small(scells, degree, cc);
//								bubbleSort(scells, degree, cc);
//
//								// get each sortedCell and it's potential values
//								c0b = (c0=scells[0]).maybes;
//								c1b = (c1=scells[1]).maybes;
//								c2b = (c2=scells[2]).maybes;
//								c3b = (c3=scells[3]).maybes;
//								c4b = (c4=scells[4]).maybes;
//								c5b = (c5=scells[5]).maybes;
//
//								// build the notSiblings cache
//								ns10 = c1.notSees[c0.i];
//
//								ns21 = c2.notSees[c1.i];
//								ns20 = c2.notSees[c0.i];
//
//								ns32 = c3.notSees[c2.i];
//								ns31 = c3.notSees[c1.i];
//								ns30 = c3.notSees[c0.i];
//
//								ns43 = c4.notSees[c3.i];
//								ns42 = c4.notSees[c2.i];
//								ns41 = c4.notSees[c1.i];
//								ns40 = c4.notSees[c0.i];

								c5b = (c5=cells[5]).maybes;
								ns54 = c5.notSees[c4.i];
								ns53 = c5.notSees[c3.i];
								ns52 = c5.notSees[c2.i];
								ns51 = c5.notSees[c1.i];
								ns50 = c5.notSees[c0.i];

								// clear allowedValuesBitsets array
								avb0=avb1=avb2=avb3=avb4=avb5 = 0;

								// should we test if combo of 3/4/5 values covers ceb0
								ces0 = VSIZE[ceb0=cmnExclBits[0]];
								do30 = ces0 <= 3;
								do40 = ces0 <= 4;
								do50 = ces0 <= 5;

								// anyLevel controls which for-i-loop to break-out-of when
								// all maybes of all cells to my right are already allowed;
								// so that the FIRST allowed combo sets my any* and we skip
								// all other "like" combos.
								// This is for performance only. Algorithm works without it.
								// We just don't waste time allowing multiple combos whose
								// right-hand-side-values are all already allowed.
								anyLevel = degree; // meaning that avb5 != c5b

								// The dog____ing loop calculates: is each combo allowed?
								// ie: Is each possible combination of the potential values
								// of these $degree (6) cells allowed? (else the combo has
								// been excluded, so it is skipped). We only populate the
								// allowedValuesBitsets virtual array here, we'll build the
								// excludedCombosMap later, if value/s are excluded; ie all
								// possible combinations of potential values do not include
								// certain value/s in certain position/s.
								// After this loop we check if any of each cells maybes
								// have not been allowed, and if so then it's an exclusion,
								// ie we found a hint!
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
								// Compare DOG_1 with A3E's DOG_1 to see what I'm on about.

								// nb: this switch for performance only, the default path
								// works for any numCmnExclBits. Just faster with no calls.
++counts[0];
++counts[numCmnExclBits];
								switch ( numCmnExclBits ) {
								case 1:
									// there's no need for common excluders loop
									//  w/o HitSet: 1 = 10,407,196 of 38,792,551 = 26.83%
									// with HitSet: 1 = 10 of 344 = 2.91%
									DOG_1a: for ( int sv0 : SVS[c0b] ) {
										c5b0 = ns50 ? c5b : c5b & ~sv0;
										c4b0 = ns40 ? c4b : c4b & ~sv0;
										c3b0 = ns30 ? c3b : c3b & ~sv0;
										c2b0 = ns20 ? c2b : c2b & ~sv0;
										any1 = false;
										SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
											sv01 = sv0 | sv1;
											c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
											c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
											c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
											any2 = false;
											SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
												sv02 = sv01 | sv2;
												if ( (do30 && (ceb0 & ~sv02) == 0)
												  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
													continue;
												c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
												c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
												any3 = false;
												SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
													sv03 = sv02 | sv3;
													if ( (do40 && (ceb0 & ~sv03) == 0)
													  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
														continue;
													c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
													any4 = false;
													SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
														sv04 = sv03 | sv4;
														if ( (do50 && (ceb0 & ~sv04) == 0)
														  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
															continue;
														any5 = false;
														SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
															if ( (ceb0 & ~(sv04|sv5)) == 0 )
																continue;
															// Combo Allowed!
															any5 = true;
															// add to allowedValuesBitsets and
															// check if that makes all allowed.
															if ( (avb5|=sv5) != c5b )
																continue;
															// We allow only the FIRST combo at each level; and
															// then move-on to evaluating the next value at this
															// level, which has not yet been evaluated; instead
															// of wasting time evaluating multiple combos-to-my-
															// right which can not allow any additional maybes.
															// This reduces O(n*n*n*n*n*n) to O(n*n'ish).
															switch ( anyLevel ) {
															case 6: anyLevel=5; break SV5_LOOP;
															case 5: break SV5_LOOP;
															case 4: any4=true; break SV4_LOOP;
															case 3: any3=true; break SV3_LOOP;
															case 2: any2=true; break SV2_LOOP;
															case 1: any1=true; break SV1_LOOP;
															} // end-switch
														} // next sv5
														if ( any5 ) {
															any4 = true;
															if ( (avb4|=sv4)==c4b && anyLevel==5 )
																anyLevel = 4;
														}
													} // next sv4
													if ( any4 ) {
														any3 = true;
														if ( (avb3|=sv3)==c3b && anyLevel==4 )
															anyLevel = 3;
													}
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
									// there's still no need for common excluders loop
									//  w/o HitSet: 2 = 26,939,863 of 38,792,551 = 69.45%
									ces1 = VSIZE[ceb1=cmnExclBits[1]];
									do31 = ces1 <= 3;
									do41 = ces1 <= 4;
									do51 = ces1 <= 5;
									DOG_1b: for ( int sv0 : SVS[c0b] ) {
										c5b0 = ns50 ? c5b : c5b & ~sv0;
										c4b0 = ns40 ? c4b : c4b & ~sv0;
										c3b0 = ns30 ? c3b : c3b & ~sv0;
										c2b0 = ns20 ? c2b : c2b & ~sv0;
										any1 = false;
										SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
											sv01 = sv0 | sv1;
											c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
											c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
											c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
											any2 = false;
											SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
												sv02 = sv01 | sv2;
												if ( (do30 && (ceb0 & ~sv02) == 0)
												  || (do31 && (ceb1 & ~sv02) == 0)
												  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
													continue;
												c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
												c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
												any3 = false;
												SV3_LOOP: for ( int sv3 : SVS[ns32 ? c3b1 : c3b1 & ~sv2] ) {
													sv03 = sv02 | sv3;
													if ( (do40 && (ceb0 & ~sv03) == 0)
													  || (do41 && (ceb1 & ~sv03) == 0)
													  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
														continue;
													c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
													any4 = false;
													SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
														sv04 = sv03 | sv4;
														if ( (do50 && (ceb0 & ~sv04) == 0)
														  || (do51 && (ceb1 & ~sv04) == 0)
														  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
															continue;
														any5 = false;
														SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
															if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
															  || (ceb1 & ~sv05) == 0 )
																continue;
															// Combo Allowed!
															any5 = true;
															// add to allowedValuesBitsets and
															// check if that makes all allowed.
															if ( (avb5|=sv5) != c5b )
																continue;
															// We allow only the FIRST combo at each level
															switch ( anyLevel ) {
															case 6: anyLevel=5; break SV5_LOOP;
															case 5: break SV5_LOOP;
															case 4: any4=true; break SV4_LOOP;
															case 3: any3=true; break SV3_LOOP;
															case 2: any2=true; break SV2_LOOP;
															case 1: any1=true; break SV1_LOOP;
															} // end-switch
														} // next sv5
														if ( any5 ) {
															any4 = true;
															if ( (avb4|=sv4)==c4b && anyLevel==5 )
																anyLevel = 4;
														}
													} // next sv4
													if ( any4 ) {
														any3 = true;
														if ( (avb3|=sv3)==c3b && anyLevel==4 )
															anyLevel = 3;
													}
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
									// there's still no need for common excluders loop
									//  w/o HitSet: 3 = 1,442,942 of 38,792,551 = 3.72%
									ces1 = VSIZE[ceb1=cmnExclBits[1]];
									do31 = ces1 <= 3;
									do41 = ces1 <= 4;
									do51 = ces1 <= 5;
									ces2 = VSIZE[ceb2=cmnExclBits[2]];
									do32 = ces2 <= 3;
									do42 = ces2 <= 4;
									do52 = ces2 <= 5;
									DOG_1c: for ( int sv0 : SVS[c0b] ) {
										c5b0 = ns50 ? c5b : c5b & ~sv0;
										c4b0 = ns40 ? c4b : c4b & ~sv0;
										c3b0 = ns30 ? c3b : c3b & ~sv0;
										c2b0 = ns20 ? c2b : c2b & ~sv0;
										any1 = false;
										SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
											sv01 = sv0 | sv1;
											c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
											c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
											c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
											any2 = false;
											SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
												sv02 = sv01 | sv2;
												if ( (do30 && (ceb0 & ~sv02) == 0)
												  || (do31 && (ceb1 & ~sv02) == 0)
												  || (do32 && (ceb2 & ~sv02) == 0)
												  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
													continue;
												c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
												c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
												any3 = false;
												SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
													sv03 = sv02 | sv3;
													if ( (do40 && (ceb0 & ~sv03) == 0)
													  || (do41 && (ceb1 & ~sv03) == 0)
													  || (do42 && (ceb2 & ~sv03) == 0)
													  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
														continue;
													c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
													any4 = false;
													SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
														sv04 = sv03 | sv4;
														if ( (do50 && (ceb0 & ~sv04) == 0)
														  || (do51 && (ceb1 & ~sv04) == 0)
														  || (do52 && (ceb2 & ~sv04) == 0)
														  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
															continue;
														any5 = false;
														SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
															if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
															  || (ceb1 & ~sv05) == 0
															  || (ceb2 & ~sv05) == 0 )
																continue;
															// Combo Allowed!
															any5 = true;
															// add to allowedValuesBitsets and
															// check if that makes all allowed.
															if ( (avb5|=sv5) != c5b )
																continue;
															// We allow only the FIRST combo at each level
															switch ( anyLevel ) {
															case 6: anyLevel=5; break SV5_LOOP;
															case 5: break SV5_LOOP;
															case 4: any4=true; break SV4_LOOP;
															case 3: any3=true; break SV3_LOOP;
															case 2: any2=true; break SV2_LOOP;
															case 1: any1=true; break SV1_LOOP;
															} // end-switch
														} // next sv5
														if ( any5 ) {
															any4 = true;
															if ( (avb4|=sv4)==c4b && anyLevel==5 )
																anyLevel = 4;
														}
													} // next sv4
													if ( any4 ) {
														any3 = true;
														if ( (avb3|=sv3)==c3b && anyLevel==4 )
															anyLevel = 3;
													}
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
								default :
									// we need a common excluders loop
									//  w/o HitSet: 4 = 1,973 of 38,792,551 = 0.01%
									//  w/o HitSet: 5 = 447 of 38,792,551 = 0.00%
									//  w/o HitSet: 6 = 130 of 38,792,551 = 0.00%
									ces0 = VSIZE[cmnExclBits[0]];
									do30 = ces0 <= 3;
									do40 = ces0 <= 4;
									do50 = ces0 <= 5;
									DOG_1d: for ( int sv0 : SVS[c0b] ) {
										c5b0 = ns50 ? c5b : c5b & ~sv0;
										c4b0 = ns40 ? c4b : c4b & ~sv0;
										c3b0 = ns30 ? c3b : c3b & ~sv0;
										c2b0 = ns20 ? c2b : c2b & ~sv0;
										any1 = false;
										SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
											sv01 = sv0 | sv1;
											c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
											c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
											c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
											any2 = false;
											SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
												sv02 = sv01 | sv2;
												if ( do30 && covers(cmnExclBits, numCmnExclBits, sv02)
												  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
													continue;
												c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
												c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
												any3 = false;
												SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
													sv03 = sv02 | sv3;
													if ( do40 && covers(cmnExclBits, numCmnExclBits, sv03)
													  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
														continue;
													c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
													any4 = false;
													SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
														sv04 = sv03 | sv4;
														if ( do50 && covers(cmnExclBits, numCmnExclBits, sv04)
														  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
															continue;
														any5 = false;
														SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
															if ( covers(cmnExclBits, numCmnExclBits, sv05=sv04|sv5) )
																continue;
															// Combo Allowed!
															any5 = true;
															// add to allowedValuesBitsets and
															// check if that makes all allowed.
															if ( (avb5|=sv5) != c5b )
																continue;
															// We allow only the FIRST combo at each level
															switch ( anyLevel ) {
															case 6: anyLevel=5; break SV5_LOOP;
															case 5: break SV5_LOOP;
															case 4: any4=true; break SV4_LOOP;
															case 3: any3=true; break SV3_LOOP;
															case 2: any2=true; break SV2_LOOP;
															case 1: any1=true; break SV1_LOOP;
															} // end-switch
														} // next sv5
														if ( any5 ) {
															any4 = true;
															if ( (avb4|=sv4)==c4b && anyLevel==5 )
																anyLevel = 4;
														}
													} // next sv4
													if ( any4 ) {
														any3 = true;
														if ( (avb3|=sv3)==c3b && anyLevel==4 )
															anyLevel = 3;
													}
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

								// if all combo values are allowed then no hint is possible.
								if ( avb0 == c0b
								  && avb1 == c1b
								  && avb2 == c2b
								  && avb3 == c3b
								  && avb4 == c4b
								  && avb5 == c5b ) {
									nonHinters.put();
									continue; // nothing to see here. move along!
								}

								// ===========================================
								// Performance is not an issue from here down.
								// To get here a value/s is excluded.
								// ===========================================

++hintCounts[numCmnExclBits];
								if ( isTop1465 )
									hits.add(gsl, hintNum, cells);

								// create the removable (red) potential values.
//KRC#2020-06-30 09:50:00 (cells or scells?)
								Pots redPots = createRedPotentials(cells
										, avb0,avb1,avb2,avb3,avb4,avb5);
								if ( redPots.isEmpty() )
									continue; // Should never happen, but never say never.
								// build the excluded combos map
								ExcludedCombosMap map = buildExcludedCombosMap(
										cmnExcls, numCmnExcls, cells, redPots);
								// create and add the hint
								AHint hint = new AlignedExclusionHint(this
										, redPots, cells, numCmnExcls, cmnExcls, map);
								result = true; // in case add returns false
								if ( accu.add(hint) )
									return true;

							} // next c5
						} // next c4
					} // next c3
				} // next c2
			} // next c1
		} // next c0
		return result;
	}

}
