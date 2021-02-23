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
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.gen.IInterruptMonitor;
import diuf.sudoku.solver.LogicalSolver;


/**
 * ============================================================================
 *                              See A5E_1C first
 *                       it's full of nice big comments
 * ============================================================================
 *
 * The hacked version of the Aligned 9 (Nona) Exclusion technique.
 * <p>Partially implements the aligned set exclusion Sudoku solving technique
 * for sets of 9 Cells.
 */
public final class Aligned9Exclusion_2H extends Aligned9ExclusionBase
//		implements diuf.sudoku.solver.IReporter
//				 , java.io.Closeable
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 43; // <HACK/>

	// the maximim number of candidates to permute (process).
	// count(9,61) = 17,341,763,504
	private static final int MAX_CANDIDATES = 61; // <HACK/>

	// maximum number of common excluder cells
	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] commonExcludersArray = new Cell[NUM_CMN_EXCLS];
	private static final int[] commonExcluderBitsArray = new int[NUM_CMN_EXCLS];

	private static final Cell[] cellsArray = new Cell[9];
	private static final Cell[] cellsArray1 = new Cell[9];

	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5
	private final Idx idx06 = new Idx(); // = idx05 & idx6
	private final Idx idx07 = new Idx(); // = idx06 & idx7
	private final Idx idx08 = new Idx(); // = idx07 & idx8

	private final ACollisionComparator cc = new ACollisionComparator();

	private final NonHinters64 nonHinters = new NonHinters64(16*1024, 4);

	public Aligned9Exclusion_2H(int firstHintNumber, IInterruptMonitor monitor) {
		super(firstHintNumber, monitor);
	}

//	@Override
//	public void close() throws java.io.IOException {
//		hits.save();
//	}

	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
		nonHinters.clear();
	}

	@SuppressWarnings("fallthrough")
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
		// that are packed into your maybes.bits 0..511. See Values for more.
		final int[][] SVS = Values.SHIFTED;

		// Integer.bitCount of the bitsets 0..511
		final int[] SIZE = Values.SIZE;

		// The populate populateCandidatesAndExcluders fields: a candidate Cell
		// has maybes.size>=2 and >1 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = candidatesArray;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = excludersArray;

		// the number of cells in each of the for-i-loops
		final int n0, n1, n2, n3, n4, n5, n6, n7;

		// an array of the 7 cells in an aligned set, ready for exclusion.
		final Cell[] cells = cellsArray;
		// sortedCells: the above cells array sorted by the CollisionComparator.
		// nb: c0..c6 relate to scells array, not to the above cells array.
		// nb: coincident with avb* (allowedValuesBitsets)
		final Cell[] scells = cellsArray1;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = commonExcludersArray;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = commonExcluderBitsArray;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// array of 3*27-bit-bitset of indexes of c0's excluders in Grid.cells
		Idx idx0;

		// indexes in the candidates array of the 5 candidate cells.
		int i0, i1, i2, i3, i4, i5, i6, i7, i8;

		// a virtual array of the 9 candidate cells in an aligned exclusion set.
		Cell c0, c1, c2, c3, c4, c5, c6, c7, c8;

		// notSiblings: a cache of the results of isSiblingOf.
		// EXAMPLE: ns10=c1.isNotSiblingOf[c0.i];
		boolean ns10, ns20, ns30, ns40, ns50, ns60, ns70, ns80;
		boolean		  ns21, ns31, ns41, ns51, ns61, ns71, ns81;
		boolean				ns32, ns42, ns52, ns62, ns72, ns82;
		boolean					  ns43, ns53, ns63, ns73, ns83;
		boolean							ns54, ns64, ns74, ns84;
		boolean								  ns65, ns75, ns85;
		boolean										ns76, ns86;
		boolean											  ns87;

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is messier than just
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		int c0b, c1b , c2b , c3b , c4b , c5b , c6b , c7b , c8b;
		int		       c2b0, c3b0, c4b0, c5b0, c6b0, c7b0, c8b0;
		int			         c3b1, c4b1, c5b1, c6b1, c7b1, c8b1;
		int					 c3b2, c4b2, c5b2, c6b2, c7b2, c8b2;
		int						   c4b3, c5b3, c6b3, c7b3, c8b3;
		int								 c5b4, c6b4, c7b4, c8b4;
		int									   c6b5, c7b5, c8b5;
		int											 c7b6, c8b6;
		int												   c8b7;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01,sv02,sv03,sv04,sv05,sv06,sv07,sv08;

		// allowedValuesBitsets: bare bits of allowedValues[5].bits
		int avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7,avb8; // bitsets

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1,any2,any3,any4,any5,any6,any7,any8;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // ie degree: the number of cells in the aligned set.

		// commonExcluderBits[0]
		int ceb0,ces0, ceb1,ces1;

		// if do40 check if the-combo-so-far (of 4 values) covers ceb0
		// do31 is the same but for ceb1 (ie cmnExclBits[1]).
		// nb: do6+* no exist coz presumed always true. It certainly is for
		//     case 1: which is 99.67% of aligned sets so don't care re 2 or 3.
		boolean do30, do40,do41, do50,do51;

		// presume that no hint will be found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Liberal: The practice of pulling ones foreskin back over ones head
		// in order to improve the ____ing view.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// search for candidate cells that can contribute to an exclusion set.
		// NB: Each candidate cell needs at-least 2 excluder cells. This doesn't
		//     appear to reduce the number of candidates at all (still 62 for
		//     3#top1465.d5.mt) which is not what I expected/hoped; but I went
		//     ahead anyway and retrofitted it to A5E and A6E because it's
		//     logically correct and all three jUnit test-cases still pass.
		//     It'll make a lick of difference somewhere someday. Sigh.
		// NB: 81 - 17 (minimum clue cells in valid Sudoku) = 64, so 62 is MAD!
		numCandidates = populateCandidatesAndAtleastTwoExcluders(
				candidates, excluders, grid);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false; // this happens rarely, but those count BIG-time!
		}

		n0 = numCandidates - 8;
		n1 = numCandidates - 7;
		n2 = numCandidates - 6;
		n3 = numCandidates - 5;
		n4 = numCandidates - 4;
		n5 = numCandidates - 3;
		n6 = numCandidates - 2;
		n7 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				if ( excluders[(cells[1]=candidates[i1]).i].idx2(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx2(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[(cells[3]=candidates[i3]).i].idx2(idx03, idx02) )
							continue;
						if(hitMe && cells[3]!=hitCells[3]) continue;
						for ( i4=i3+1; i4<n4; ++i4 ) {
							if ( excluders[(cells[4]=candidates[i4]).i].idx2(idx04, idx03) )
								continue;
							if(hitMe && cells[4]!=hitCells[4]) continue;
							for ( i5=i4+1; i5<n5; ++i5 ) {
								if ( excluders[candidates[i5].i].idx2(idx05, idx04) )
									continue;
								cells[5] = candidates[i5];
								if(hitMe && cells[5]!=hitCells[5]) continue;
								for ( i6=i5+1; i6<n6; ++i6 ) {
									if ( excluders[candidates[i6].i].idx2(idx06, idx05) )
										continue;
									cells[6] = candidates[i6];
									if(hitMe && cells[6]!=hitCells[6]) continue;
									for ( i7=i6+1; i7<n7; ++i7 ) {
										if ( excluders[candidates[i7].i].idx2(idx07, idx06) )
											continue;
										cells[7] = candidates[i7];
										if(hitMe && cells[7]!=hitCells[7]) continue;
										if ( isInterrupted() )
											return false;
										for ( i8=i7+1; i8<numCandidates; ++i8 ) {
											if ( excluders[candidates[i8].i].idx2(idx08, idx07) )
												continue;
											cells[8] = candidates[i8];
											if(hitMe && cells[8]!=hitCells[8]) continue;

											if ( nonHinters.skip(cells) )
												continue;

											// read common excluder cells from grid at idx08
											numCmnExcls = idx08.cellsN(grid, cmnExcls);
											assert numCmnExcls >= 2;
											// performance enhancement: examine smaller maybes sooner.
											// KRC#2020-06-30 10:20:00
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
											if ( (numCmnExclBits = disdisjunct(cmnExclBits
													, numCmnExclBits, allMaybesBits(cells))) == 0 )
												continue;

											// the dog____ing algorithm is faster with dodgem-cars to the left,
											// but the above for-i-loops need a static cells array; so we copy
											// cells to scells (sortedCells) and sort that array DESCENDING by:
											// 4*maybesCollisions + 2*commonExcluderHits + maybes.size
											cc.set(cells, cmnExclBits, numCmnExclBits);
											System.arraycopy(cells, 0, scells, 0, degree);

//KRC#2020-06-30 09:50:00
// before :     22,779,490,800	   4466	     5,100,647	      1	22,779,490,800	Aligned Nona
// after  :     31,124,463,400	   4466	     6,969,203	      1	31,124,463,400	Aligned Nona
// methods:
											System.arraycopy(cells, 0, scells, 0, degree);
//											//MyTimSort.small(scells, degree, cc);
											bubbleSort(scells, degree, cc);


											// get c* + maybes.bits from sortedCells
											c0b = (c0=scells[0]).maybes.bits;
											c1b = (c1=scells[1]).maybes.bits;
											c2b = (c2=scells[2]).maybes.bits;
											c3b = (c3=scells[3]).maybes.bits;
											c4b = (c4=scells[4]).maybes.bits;
											c5b = (c5=scells[5]).maybes.bits;
											c6b = (c6=scells[6]).maybes.bits;
											c7b = (c7=scells[7]).maybes.bits;
											c8b = (c8=scells[8]).maybes.bits;

											// build the isNotSiblingOf cache
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

											ns50 = c5.notSees[c0.i];
											ns51 = c5.notSees[c1.i];
											ns52 = c5.notSees[c2.i];
											ns53 = c5.notSees[c3.i];
											ns54 = c5.notSees[c4.i];

											ns60 = c6.notSees[c0.i];
											ns61 = c6.notSees[c1.i];
											ns62 = c6.notSees[c2.i];
											ns63 = c6.notSees[c3.i];
											ns64 = c6.notSees[c4.i];
											ns65 = c6.notSees[c5.i];

											ns70 = c7.notSees[c0.i];
											ns71 = c7.notSees[c1.i];
											ns72 = c7.notSees[c2.i];
											ns73 = c7.notSees[c3.i];
											ns74 = c7.notSees[c4.i];
											ns75 = c7.notSees[c5.i];
											ns76 = c7.notSees[c6.i];

											ns80 = c8.notSees[c0.i];
											ns81 = c8.notSees[c1.i];
											ns82 = c8.notSees[c2.i];
											ns83 = c8.notSees[c3.i];
											ns84 = c8.notSees[c4.i];
											ns85 = c8.notSees[c5.i];
											ns86 = c8.notSees[c6.i];
											ns87 = c8.notSees[c7.i];

											// clear the allowedValuesBitsets virtual array
											avb0=avb1=avb2=avb3=avb4=avb5=avb6=avb7=avb8 = 0;

											// anyLevel controls which for-i-loop to break-out-of when
											// all maybes of all cells to my right are already allowed;
											// so that the FIRST allowed combo sets my any* and we skip
											// all other "like" combos.
											// This is for performance only. Algorithm works without it.
											// We just don't waste time allowing multiple combos whose
											// right-hand-side-values are all already allowed.
											anyLevel = degree; // meaning that avb8 != c8b

											// should we test if 3/4/5 values cover ceb0
											ces0 = SIZE[ceb0=cmnExclBits[0]];
											do30 = ces0 <= 3;
											do40 = ces0 <= 4;
											do50 = ces0 <= 5;
											// The dog____ing loop calculates: is each combo allowed?
											// ie: Is each possible combination of the potential values
											// of these $degree (9) cells allowed? (else the combo has
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
												// there's no need for a common excluders loop
												//  w/o HitSet: 1 = 403,086 of 2,423,889 = 16.63%
												// with HitSet: 1 = 0 of 22 = 0.00%
												DOG_1a: for ( int sv0 : SVS[c0b] ) {
													c8b0 = ns80 ? c8b : c8b & ~sv0;
													c7b0 = ns70 ? c7b : c7b & ~sv0;
													c6b0 = ns60 ? c6b : c6b & ~sv0;
													c5b0 = ns50 ? c5b : c5b & ~sv0;
													c4b0 = ns40 ? c4b : c4b & ~sv0;
													c3b0 = ns30 ? c3b : c3b & ~sv0;
													c2b0 = ns20 ? c2b : c2b & ~sv0;
													any1 = false;
													SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
														sv01 = sv0 | sv1;
														c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
														c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
														c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
														c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
														c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
														c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
														any2 = false;
														SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
															sv02 = sv01 | sv2;
															if ( do30 && covers(cmnExclBits, numCmnExclBits, sv02)
															  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
																continue;
															c8b2 = ns82 ? c8b1 : c8b1 & ~sv2;
															c7b2 = ns72 ? c7b1 : c7b1 & ~sv2;
															c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
															c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
															c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
															any3 = false;
															SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
																sv03 = sv02 | sv3;
																if ( (do40 && (ceb0 & ~sv03) == 0)
																  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
																	continue;
																c8b3 = ns83 ? c8b2 : c8b2 & ~sv3;
																c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
																c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
																c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
																any4 = false;
																SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																	sv04 = sv03 | sv4;
																	if ( (do50 && (ceb0 & ~sv04) == 0)
																	  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																		continue;
																	c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																	c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																	c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																	any5 = false;
																	SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																		if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		c8b5 = ns85 ? c8b4 : c8b4 & ~sv5;
																		c7b5 = ns75 ? c7b4 : c7b4 & ~sv5;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
																			c8b6 = ns86 ? c8b5 : c8b5 & ~sv6;
																			any7 = false;
																			SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																				if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																				  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																					continue;
																				any8 = false;
																				SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																					if ( (ceb0 & ~(sv07|sv8)) == 0 )
																						continue;
																					// Combo Allowed!
																					any8 = true;
																					// add to allowedValuesBitsets and
																					// check if that makes all allowed.
																					if ( (avb8|=sv8) != c8b )
																						continue;
																					// We allow only the FIRST combo at each level; and
																					// then move-on to evaluating the next value at this
																					// level, which has not yet been evaluated; instead
																					// of wasting time evaluating multiple combos-to-my-
																					// right which can not allow any additional maybes.
																					// This reduces O(n*n*n*n*n*n*n*n*n) to O(n*n*n'ish).
																					switch ( anyLevel ) {
																					case 9: anyLevel=8; break SV8_LOOP;
																					case 8: break SV8_LOOP;
																					case 7: any7=true; break SV7_LOOP;
																					case 6: any6=true; break SV6_LOOP;
																					case 5: any5=true; break SV5_LOOP;
																					case 4: any4=true; break SV4_LOOP;
																					case 3: any3=true; break SV3_LOOP;
																					case 2: any2=true; break SV2_LOOP;
																					case 1: any1=true; break SV1_LOOP;
																					} // end-switch
																				} // next sv8
																				if ( any8 ) {
																					any7 = true;
																					// nb: if anyLevel==8 stops any
																					// shortcuts until avb8==c8b
																					if ( (avb7|=sv7)==c7b && anyLevel==8 )
																						anyLevel = 7;
																				}
																			} // next sv7
																			if ( any7 ) {
																				any6 = true;
																				// nb: if anyLevel==7 stops any further
																				// shortcuts until avb8==c8b && avb7==c7b
																				// (and so on for the rest of the any*)
																				if ( (avb6|=sv6)==c6b && anyLevel==7 )
																					anyLevel = 6;
																			}
																		} // next sv6
																		if ( any6 ) {
																			any5 = true;
																			if ( (avb5|=sv5)==c5b && anyLevel==6 )
																				anyLevel = 5;
																		}
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
												// there's no need for a common excluders loop
												//  w/o HitSet: 2 = 1,971,292 of 2,423,889 = 81.33%
												// with HitSet: 2 = 21 of 22 = 95.45%
												ces1 = SIZE[ceb1=cmnExclBits[1]];
												do41 = ces1 <= 4;
												do51 = ces1 <= 5;
												DOG_1b: for ( int sv0 : SVS[c0b] ) {
													c8b0 = ns80 ? c8b : c8b & ~sv0;
													c7b0 = ns70 ? c7b : c7b & ~sv0;
													c6b0 = ns60 ? c6b : c6b & ~sv0;
													c5b0 = ns50 ? c5b : c5b & ~sv0;
													c4b0 = ns40 ? c4b : c4b & ~sv0;
													c3b0 = ns30 ? c3b : c3b & ~sv0;
													c2b0 = ns20 ? c2b : c2b & ~sv0;
													any1 = false;
													SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
														sv01 = sv0 | sv1;
														c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
														c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
														c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
														c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
														c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
														c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
														any2 = false;
														SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
															sv02 = sv01 | sv2;
															if ( do30 && covers(cmnExclBits, numCmnExclBits, sv02)
															  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
																continue;
															c8b2 = ns82 ? c8b1 : c8b1 & ~sv2;
															c7b2 = ns72 ? c7b1 : c7b1 & ~sv2;
															c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
															c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
															c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
															any3 = false;
															SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
																sv03 = sv02 | sv3;
																if ( (do40 && (ceb0 & ~sv03) == 0)
																  || (do41 && (ceb1 & ~sv03) == 0)
																  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
																	continue;
																c8b3 = ns83 ? c8b2 : c8b2 & ~sv3;
																c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
																c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
																c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
																any4 = false;
																SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																	sv04 = sv03 | sv4;
																	if ( (do50 && (ceb0 & ~sv04) == 0)
																	  || (do51 && (ceb1 & ~sv04) == 0)
																	  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																		continue;
																	c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																	c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																	c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																	any5 = false;
																	SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																		if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																		  || (ceb1 & ~sv05) == 0
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		c8b5 = ns85 ? c8b4 : c8b4 & ~sv5;
																		c7b5 = ns75 ? c7b4 : c7b4 & ~sv5;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																			  || (ceb1 & ~sv06) == 0
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
																			c8b6 = ns86 ? c8b5 : c8b5 & ~sv6;
																			any7 = false;
																			SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																				if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																				  || (ceb1 & ~sv07) == 0
																				  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																					continue;
																				any8 = false;
																				SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																					if ( (ceb0 & ~(sv08=sv07|sv8)) == 0
																					  || (ceb1 & ~sv08) == 0 )
																						continue;
																					// Combo Allowed!
																					any8 = true;
																					// add to allowedValuesBitsets and
																					// check if that makes all allowed.
																					if ( (avb8|=sv8) != c8b )
																						continue;
																					// We allow only the FIRST combo at each level
																					switch ( anyLevel ) {
																					case 9: anyLevel=8; break SV8_LOOP;
																					case 8: break SV8_LOOP;
																					case 7: any7=true; break SV7_LOOP;
																					case 6: any6=true; break SV6_LOOP;
																					case 5: any5=true; break SV5_LOOP;
																					case 4: any4=true; break SV4_LOOP;
																					case 3: any3=true; break SV3_LOOP;
																					case 2: any2=true; break SV2_LOOP;
																					case 1: any1=true; break SV1_LOOP;
																					} // end-switch
																				} // next sv8
																				if ( any8 ) {
																					any7 = true;
																					// nb: if anyLevel==8 stops any
																					// shortcuts until avb8==c8b
																					if ( (avb7|=sv7)==c7b && anyLevel==8 )
																						anyLevel = 7;
																				}
																			} // next sv7
																			if ( any7 ) {
																				any6 = true;
																				// nb: if anyLevel==7 stops any further
																				// shortcuts until avb8==c8b && avb7==c7b
																				// (and so on for the rest of the any*)
																				if ( (avb6|=sv6)==c6b && anyLevel==7 )
																					anyLevel = 6;
																			}
																		} // next sv6
																		if ( any6 ) {
																			any5 = true;
																			if ( (avb5|=sv5)==c5b && anyLevel==6 )
																				anyLevel = 5;
																		}
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
											default:
												// we need a common excluders loop
												//  w/o HitSet: 3 = 49,511 of 2,423,889 = 2.04%
												// with HitSet: 3 = 1 of 22 = 4.55%
												do40 = SIZE[ceb0] <= 4;
												do50 = SIZE[ceb0] <= 5;
												DOG_1c: for ( int sv0 : SVS[c0b] ) {
													c8b0 = ns80 ? c8b : c8b & ~sv0;
													c7b0 = ns70 ? c7b : c7b & ~sv0;
													c6b0 = ns60 ? c6b : c6b & ~sv0;
													c5b0 = ns50 ? c5b : c5b & ~sv0;
													c4b0 = ns40 ? c4b : c4b & ~sv0;
													c3b0 = ns30 ? c3b : c3b & ~sv0;
													c2b0 = ns20 ? c2b : c2b & ~sv0;
													any1 = false;
													SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
														sv01 = sv0 | sv1;
														c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
														c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
														c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
														c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
														c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
														c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
														any2 = false;
														SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
															sv02 = sv01 | sv2;
															if ( do30 && covers(cmnExclBits, numCmnExclBits, sv02)
															  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
																continue;
															c8b2 = ns82 ? c8b1 : c8b1 & ~sv2;
															c7b2 = ns72 ? c7b1 : c7b1 & ~sv2;
															c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
															c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
															c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
															any3 = false;
															SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
																sv03 = sv02 | sv3;
																if ( do40 && covers(cmnExclBits, numCmnExclBits, sv03)
																  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
																	continue;
																c8b3 = ns83 ? c8b2 : c8b2 & ~sv3;
																c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
																c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
																c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
																any4 = false;
																SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																	sv04 = sv03 | sv4;
																	if ( do50 && covers(cmnExclBits, numCmnExclBits, sv04)
																	  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																		continue;
																	c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																	c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																	c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																	any5 = false;
																	SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																		if ( covers(cmnExclBits, numCmnExclBits, sv05=sv04|sv5)
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		c8b5 = ns85 ? c8b4 : c8b4 & ~sv5;
																		c7b5 = ns75 ? c7b4 : c7b4 & ~sv5;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( covers(cmnExclBits, numCmnExclBits, sv06=sv05|sv6)
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
																			c8b6 = ns86 ? c8b5 : c8b5 & ~sv6;
																			any7 = false;
																			SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																				if ( covers(cmnExclBits, numCmnExclBits, sv07=sv06|sv7)
																				  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																					continue;
																				any8 = false;
																				SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																					if ( covers(cmnExclBits, numCmnExclBits, sv07|sv8) )
																						continue;
																					// Combo Allowed!
																					any8 = true;
																					// add to allowedValuesBitsets and
																					// check if that makes all allowed.
																					if ( (avb8|=sv8) != c8b )
																						continue;
																					// We allow only the FIRST combo at each level
																					switch ( anyLevel ) {
																					case 9: anyLevel=8; break SV8_LOOP;
																					case 8: break SV8_LOOP;
																					case 7: any7=true; break SV7_LOOP;
																					case 6: any6=true; break SV6_LOOP;
																					case 5: any5=true; break SV5_LOOP;
																					case 4: any4=true; break SV4_LOOP;
																					case 3: any3=true; break SV3_LOOP;
																					case 2: any2=true; break SV2_LOOP;
																					case 1: any1=true; break SV1_LOOP;
																					} // end-switch
																				} // next sv8
																				if ( any8 ) {
																					any7 = true;
																					// nb: if anyLevel==8 stops any
																					// shortcuts until avb8==c8b
																					if ( (avb7|=sv7)==c7b && anyLevel==8 )
																						anyLevel = 7;
																				}
																			} // next sv7
																			if ( any7 ) {
																				any6 = true;
																				// nb: if anyLevel==7 stops any further
																				// shortcuts until avb8==c8b && avb7==c7b
																				// (and so on for the rest of the any*)
																				if ( (avb6|=sv6)==c6b && anyLevel==7 )
																					anyLevel = 6;
																			}
																		} // next sv6
																		if ( any6 ) {
																			any5 = true;
																			if ( (avb5|=sv5)==c5b && anyLevel==6 )
																				anyLevel = 5;
																		}
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
											  && avb5 == c5b
											  && avb6 == c6b
											  && avb7 == c7b
											  && avb8 == c8b ) {
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
													, avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7,avb8);
											if ( redPots.isEmpty() )
												continue; // Should never happen, but never say never.
											// create and add the hint
											ExcludedCombosMap map = buildExcludedCombosMap(
													cmnExcls, numCmnExcls, cells, redPots);
											AHint hint = new AlignedExclusionHint(
												  this, redPots, cells, numCmnExcls, cmnExcls, map);
											result = true; // in case add returns false
											if ( accu.add(hint) )
												return true;

										} // next c8
									} // next c7
								} // next c6
							} // next c5
						} // next c4
					} // next c3
				} // next c2
			} // next c1
		} // next c0
		return result;
	}

}
