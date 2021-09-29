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
import diuf.sudoku.gen.IInterruptMonitor;
import diuf.sudoku.io.IO;
import diuf.sudoku.solver.LogicalSolver;


/**
 * ============================================================================
 *                              See A5E_1C first
 *                       it's full of nice big comments
 * ============================================================================
 *
 * The Hacked version of the Aligned 5 (Pent) Exclusion technique.
 * <p>Partially implements the aligned set exclusion Sudoku solving technique
 * for sets of 5 Cells.
 * <p>
 * This "_2H" hacked implementation requires two common excluder cells per
 * aligned set, so it finds about a third of the hints (553 of 1477) in about a
 * tenth of the time (02:13 of 21:58). To be clear, this is a nasty little hack,
 * but the alternative is that A4+E is so slow that the user will disable it,
 * and some beats none, so the hack is in, and it's up to the user if they want
 * to use the hack (with the "hacked" CheckBoxes in TechSelectDialog) or wait
 * 20, 40, 80, 160... minutes for correct change. See idx2 & idx1 for more.
 * <p>
 * NB: The hacked version call populateCandidatesAndAtleastTwoExcluders
 * where correct versions call populateCandidatesAndExcluders
 * <p>
 */
public final class Aligned5Exclusion_2H extends Aligned5ExclusionBase
implements
		//diuf.sudoku.solver.IReporter,
		diuf.sudoku.solver.hinters.IPreparer,
		java.io.Closeable
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 25; // <HACK/>

	// the maximim number of candidates to permute (process).
	private static final int MAX_CANDIDATES = 60; // <HACK/>

	private static final int NUM_CMN_EXCLS = 6;

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[5];
	private static final Cell[] CELLS_ARRAY_1 = new Cell[5];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4

	private final ACollisionComparator cc = new ACollisionComparator();

	private final NonHinters nonHinters = new NonHinters(16*1024, 4);
	// What's that Skip? Why it's the skipper skipper flipper Flipper.
	private boolean firstPass = true;

	public Aligned5Exclusion_2H(IInterruptMonitor monitor) {
		super(monitor, IO.A5E_2H_HITS);
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
		// it's just easier to set firstPass ONCE, rather than deal with it in
		// each of the multiple exit-points from what is now findHintsImpl.
		boolean ret = findHintsImpl(grid, accu);
		firstPass = false;
		return ret;
	}

	private boolean findHintsImpl(Grid grid, IAccumulator accu) {

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
		// that are packed into your maybes.bits 0..511. See Values for more.
		// We create the local reference just for speed of access.
		final int[][] SVS = Values.VSHIFTED;

		// the populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 2 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// the number of cells in each for-i-loop
		final int n0, n1, n2, n3;

		// an array of the 5 cells in each aligned set.
		final Cell[] cells = CELLS_ARRAY;
		// sortedCells the above cells sorted by the CollisionComparator
		final Cell[] scells = CELLS_ARRAY_1;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// array of bitsets of the indexes of c0's excluder cells in Grid.cells
		Idx idx0;

		// indexes in the candidates array of the 5 candidate cells.
		int i0, i1, i2, i3, i4;

		// a virtual array of the 5 cells in each aligned set.
		// we use individual references for speed and brevity.
		// these variables are co-incident with scells (not cells).
		Cell c0, c1, c2, c3, c4;

		// notSiblings: a cache of the results of Cells isNotSiblingOf method.
		// EG: ns40 = c4.isNotSiblingOf[c0.i];
		boolean ns10, ns20, ns30, ns40;
		boolean		  ns21, ns31, ns41;
		boolean				ns32, ns42;
		boolean					  ns43;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01, sv02, sv03, sv04;

		// allowedValuesBitsets: an array of bitsets of values allowed in scells
		int avb0, avb1, avb2, avb3, avb4;

		// Working bitsets of the maybes of each cell. When this cells value is
		// chosen we remove that value from the maybes of each subsequent cell
		// in the set which is a sibling of this cell. It's more code than
		// "skip collision" (as per A234E) but it's faster, because it does
		// more of the work less often.
		int c0b, c1b , c2b , c3b , c4b;  // c4b = c4.maybes.bits // original
		int		       c2b0, c3b0, c4b0; // c4b0 = c4b  & ~sv0   // version 0
		int			         c3b1, c4b1; // c4b1 = c4b0 & ~sv1   // version 1
		int					 c3b2, c4b2; // c4b2 = c4b1 & ~sv2   // version 2
		int						   c4b3; // c4b3 = c4b2 & ~sv3   // version 3

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1,any2,any3,any4;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // the number of cells in the aligned set.

		// ceb0 is CommonExcluder(maybes)Bits[0]
		// ces0 is CommonExcluder(maybes)bitsSize[0]
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// should we test of combo of 3,4 values covers ceb0/1/2
		boolean do30,do31,do32, do40,do41,do42;

		// presume failure, ie no hint found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//            and there off and RACing in the blind mans
		//             skeet shoot whilst standing on your head
		//                        in a crocodile pit
		//                  (just ____off Rita, seriously)
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

		n0 = numCandidates - 4;
		n1 = numCandidates - 3;
		n2 = numCandidates - 2;
		n3 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				// nb: the difference between _1C (correct) and _2H (hacked)
				// is that _1C calls idx1 whereas _2H calls idx2 to require
				// 2 common excluder cells as locus of each "aligned set".
				// It finds about 1/3 of the hints in about 1/10 of time.
				if ( excluders[(cells[1]=candidates[i1]).i].idx2(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx2(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[candidates[i3].i].idx2(idx03, idx02) )
							continue;
						cells[3] = candidates[i3];
						if(hitMe && cells[3]!=hitCells[3]) continue;
						if ( isInterrupted() )
							return false;
						for ( i4=i3+1; i4<numCandidates; ++i4 ) {
							if ( excluders[candidates[i4].i].idx2(idx04, idx03) )
								continue;
							cells[4] = candidates[i4];
							if(hitMe && cells[4]!=hitCells[4]) continue;

							if ( nonHinters.skip(cells) )
								continue;

							// read common excluder cells from grid at idx04
							numCmnExcls = idx04.cellsN(grid, cmnExcls);
							// performance enhancement: examine smaller maybes sooner.
							//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
							bubbleSort(cmnExcls, numCmnExcls);
							// get common excluders bits, removing any supersets,
							// eg: {12,125}=>{12} coz 125 covers 12, so 125 does nada.
							numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);

							// remove each common excluder bits which contains a value that
							// is not in any cells maybes, because no combo can cover it,
							// so it can't contribute to any exclusions.
							// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
							// skip if there are no common excluders remaining.
							if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
									, allMaybesBits(cells))) == 0 )
								continue;

							// the dog____ing algorithm is faster with dodgem-cars to the left,
							// but the above for-i-loops need a static cells array; so we copy
							// cells to scells (sortedCells) and sort that array DESCENDING by:
							// 4*maybesCollisions + 2*cmnExclHits + maybes.size
							cc.set(cells, cmnExclBits, numCmnExclBits);
							System.arraycopy(cells, 0, scells, 0, degree);
							//MyTimSort.small(scells, degree, cc);
							bubbleSort(scells, degree, cc);

							// cache the sorted cells and there potential values
							c0b = (c0=scells[0]).maybes.bits;
							c1b = (c1=scells[1]).maybes.bits;
							c2b = (c2=scells[2]).maybes.bits;
							c3b = (c3=scells[3]).maybes.bits;
							c4b = (c4=scells[4]).maybes.bits;

							// complete the isSiblingOf cache
							ns10 = c1.notSees[c0.i];

							ns20 = c2.notSees[c0.i];
							ns21 = c2.notSees[c1.i];

							ns30 = c3.notSees[c0.i];
							ns31 = c3.notSees[c1.i];
							ns32 = c3.notSees[c2.i];

							ns40 = c4.notSees[c0.i];
							ns41 = c4.notSees[c1.i];
							ns42 = c4.notSees[c2.i];
							ns43 = c4.notSees[c3.i];

							// clear allowedValuesBitsets array.
							avb0=avb1=avb2=avb3=avb4 = 0;

							// anyLevel controls which for-i-loop to break-out-of when
							// all maybes of all cells to my right are already allowed;
							// so that the FIRST allowed combo sets my any* and we skip
							// all other "like" combos.
							// This is for performance only. Algorithm works without it.
							// We just don't waste time allowing multiple combos whose
							// right-hand-side-values are all already allowed.
							anyLevel = degree; // meaning that avb4 != c4b

							// should we test if 3/4 values cover ceb0
							ces0 = VSIZE[ceb0=cmnExclBits[0]];
							do30 = ces0 <= 3;
							do40 = ces0 <= 4;

							// The dog____ing loop works out if any maybes are excluded
							// (ie all possible combinations of potential values do not
							// include certain value/s in certain position/s)
							// by recording each value of each allowed combo in its
							// position in the avb (allowedValuesBitsets) array.
							// After the dog____ing loop we check if any potential
							// values have not been allowed (ie are excluded), and if
							// so we build the excludedCombosMap and the hint.
							// It's called the dog____ing loop because it's slow.
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

							// nb: this switch for performance only, the default path
							// works for any numCmnExclBits. Just faster with no calls.
++counts[0];
++counts[numCmnExclBits];
							switch ( numCmnExclBits ) {
							case 1:
								// we don't need a common excluders loop
								//  w/o HitSet: 1 = 14,893,692 of 47,578,398 = 31.30%
								// with HitSet: 1 = 40 of 882 = 4.54%
								// get the maybe.bits of the first common excluder cell
								DOG_1a: for ( int sv0 : SVS[c0b] ) {
									c4b0 = ns40 ? c4b : c4b & ~sv0;
									c3b0 = ns30 ? c3b : c3b & ~sv0;
									c2b0 = ns20 ? c2b : c2b & ~sv0;
									any1 = false;
									SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
										sv01 = sv0 | sv1;
										c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
										c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
										any2 = false;
										SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
											sv02 = sv01 | sv2;
											if ( (do30 && (ceb0 & ~sv02) == 0)
											  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
												continue;
											c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
											any3 = false;
											SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
												sv03 = sv02 | sv3;
												if ( (do40 && (ceb0 & ~sv03) == 0)
												  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
													continue;
												any4 = false;
												SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
													if ( (ceb0 & ~(sv03|sv4)) == 0 )
														continue;
													// Combo Allowed!
													any4 = true;
													// add to allowedValuesBitsets
													// and check if that makes all allowed.
													if ( (avb4|=sv4) != c4b )
														continue;
													// We allow only the FIRST combo at each level
													switch ( anyLevel ) {
													case 5: anyLevel=4; break SV4_LOOP;
													case 4: break SV4_LOOP;
													case 3: any3=true; break SV3_LOOP;
													case 2: any2=true; break SV2_LOOP;
													case 1: any1=true; break SV1_LOOP;
													} // end-switch
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
								// we still don't need a common excluders loop
								//  w/o HitSet: 2 = 30,306,821 of 47,578,398 = 63.70%
								// with HitSet: 2 = 775 of 882 = 87.87%
								ces1 = VSIZE[ceb1=cmnExclBits[1]];
								do31 = ces1 <= 3;
								do41 = ces1 <= 4;
								// nb: col2 is as per col, so not implemented
								DOG_1b: for ( int sv0 : SVS[c0b] ) {
									c4b0 = ns40 ? c4b : c4b & ~sv0;
									c3b0 = ns30 ? c3b : c3b & ~sv0;
									c2b0 = ns20 ? c2b : c2b & ~sv0;
									any1 = false;
									SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
										sv01 = sv0 | sv1;
										c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
										c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
										any2 = false;
										SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
											sv02 = sv01 | sv2;
											if ( (do30 && (ceb0 & ~sv02) == 0)
											  || (do31 && (ceb1 & ~sv02) == 0)
											  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
												continue;
											c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
											any3 = false;
											SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
												sv03 = sv02 | sv3;
												if ( (do40 && (ceb0 & ~sv03) == 0)
												  || (do41 && (ceb1 & ~sv03) == 0)
												  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
													continue;
												any4 = false;
												SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
													if ( (ceb0 & ~(sv04=sv03|sv4)) == 0
													  || (ceb1 & ~sv04) == 0 )
														continue;
													// Combo Allowed!
													any4 = true;
													// add to allowedValuesBitsets and
													// check if that makes all allowed.
													if ( (avb4|=sv4) != c4b )
														continue;
													// We allow only the FIRST combo at each level
													switch ( anyLevel ) {
													case 5: anyLevel=4; break SV4_LOOP;
													case 4: break SV4_LOOP;
													case 3: any3=true; break SV3_LOOP;
													case 2: any2=true; break SV2_LOOP;
													case 1: any1=true; break SV1_LOOP;
													} // end-switch
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
								// we still don't need a common excluders loop
								//  w/o HitSet: 3 = 2,293,660 of 47,578,398 = 4.82%
								// with HitSet: 3 = 49 of 882 = 5.56%
								ces1 = VSIZE[ceb1=cmnExclBits[1]];
								do31 = ces1 <= 3;
								do41 = ces1 <= 4;
								ces2 = VSIZE[ceb2 = cmnExclBits[2]];
								do32 = ces2 <= 3;
								do42 = ces2 <= 4;
								// nb: col2 is as per col, so not implemented
								DOG_1b: for ( int sv0 : SVS[c0b] ) {
									c4b0 = ns40 ? c4b : c4b & ~sv0;
									c3b0 = ns30 ? c3b : c3b & ~sv0;
									c2b0 = ns20 ? c2b : c2b & ~sv0;
									any1 = false;
									SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
										sv01 = sv0 | sv1;
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
											c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
											any3 = false;
											SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
												sv03 = sv02 | sv3;
												if ( (do40 && (ceb0 & ~sv03) == 0)
												  || (do41 && (ceb1 & ~sv03) == 0)
												  || (do42 && (ceb2 & ~sv03) == 0)
												  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
													continue;
												any4 = false;
												SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
													if ( (ceb0 & ~(sv04=sv03|sv4)) == 0
													  || (ceb1 & ~sv04) == 0
													  || (ceb2 & ~sv04) == 0 )
														continue;
													// Combo Allowed!
													any4 = true;
													// add to allowedValuesBitsets and
													// check if that makes all allowed.
													if ( (avb4|=sv4) != c4b )
														continue;
													// We allow only the FIRST combo at each level
													switch ( anyLevel ) {
													case 5: anyLevel=4; break SV4_LOOP;
													case 4: break SV4_LOOP;
													case 3: any3=true; break SV3_LOOP;
													case 2: any2=true; break SV2_LOOP;
													case 1: any1=true; break SV1_LOOP;
													} // end-switch
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
							default:
								// we need a common excluders loop
								//  w/o HitSet: 4 = 78,521 of 47,578,398 = 0.17%
								//  w/o HitSet: 5 =  4,884 of 47,578,398 = 0.01%
								//  w/o HitSet: 6 =    820 of 47,578,398 = 0.00%
								// with HitSet: 4 = 15 of 882 = 1.70%
								// with HitSet: 5 =  3 of 882 = 0.34%
								// suspect this may be allways true, so waste of time.
								do40 = VSIZE[cmnExclBits[0]] <= 4;
								DOG_1c: for ( int sv0 : SVS[c0b] ) {
									c4b0 = ns40 ? c4b : c4b & ~sv0;
									c3b0 = ns30 ? c3b : c3b & ~sv0;
									c2b0 = ns20 ? c2b : c2b & ~sv0;
									any1 = false;
									SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
										sv01 = sv0 | sv1;
										c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
										c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
										any2 = false;
										SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
											sv02 = sv01 | sv2;
											if ( (do30 && covers(cmnExclBits, numCmnExclBits, sv02))
											  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
												continue;
											c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
											any3 = false;
											SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
												sv03 = sv02 | sv3;
												if ( (do40 && covers(cmnExclBits, numCmnExclBits, sv03))
												  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
													continue;
												any4 = false;
												SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
													if ( covers(cmnExclBits, numCmnExclBits, sv04=sv03|sv4) )
														continue;
													// Combo Allowed!
													any4 = true;
													// add to allowedValuesBitsets and
													// check if that makes all allowed.
													if ( (avb4|=sv4) != c4b )
														continue;
													// We allow only the FIRST combo at each level
													switch ( anyLevel ) {
													case 5: anyLevel=4; break SV4_LOOP;
													case 4: break SV4_LOOP;
													case 3: any3=true; break SV3_LOOP;
													case 2: any2=true; break SV2_LOOP;
													case 1: any1=true; break SV1_LOOP;
													} // end-switch
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

							// check if any of each cells maybes have not been allowed,
							// and if so then it's an exclusion, ie we found a hint!
							if ( avb0 == c0b // c0b == c0.maybes.bits
							  && avb1 == c1b
							  && avb2 == c2b
							  && avb3 == c3b
							  && avb4 == c4b ) {
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
							Pots redPots = createRedPotentials(scells
									, avb0,avb1,avb2,avb3,avb4);
							if ( redPots.isEmpty() )
								continue; // Should never happen. Never say never.
							// create and add the hint
							AHint hint = new AlignedExclusionHint(this
									, redPots, cells, numCmnExcls, cmnExcls
									, buildExcludedCombosMap(cmnExcls, numCmnExcls, cells, redPots)
							);
							result = true; // in case add returns false
							if ( accu.add(hint) )
								return true;

						} // next c4
					} // next c3
				} // next c2
			} // next c1
		} // next c0
		return result;
	}

}
