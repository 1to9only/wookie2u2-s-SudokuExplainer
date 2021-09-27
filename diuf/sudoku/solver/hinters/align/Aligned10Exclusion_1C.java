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
import diuf.sudoku.solver.LogicalSolver;


/**
 * ============================================================================
 *                              See A5E_1C first
 *                       it's full of nice big comments
 * ============================================================================
 *
 * Aligned 10 (Dec) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 * of 10 Cells.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *                                 W A R N I N G
 *                              This ____ing ____er
 *                       i s  f u c k i n g   f u c k e d!
 *
 * Just use the hacked version, and get back to pullin ya pud 34 hours sooner.
 * I suppose ya could pull ya pud while it runs but 34 hours of continuous
 * wanking sounds like a sore ____ing knob to me. Davo's gone Pull blind! But
 *
 *                          THIS CLASS IS TOO SLOW !!!!
 *
 * Ready please Mr Moore.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public final class Aligned10Exclusion_1C extends Aligned10ExclusionBase
		implements diuf.sudoku.solver.IReporter
//				 , java.io.Closeable
				 , diuf.sudoku.solver.IPreparer
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 10; // <HACK/>

	// the maximim number of candidates to permute (process).
	// count(10,56) = 35,607,051,479
	private static final int MAX_CANDIDATES = 56; // <HACK/>

	private static final int NUM_CMN_EXCLS = 12;

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[10];
	private static final Cell[] CELLS_ARRAY_1 = new Cell[10];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5
	private final Idx idx06 = new Idx(); // = idx05 & idx6
	private final Idx idx07 = new Idx(); // = idx06 & idx7
	private final Idx idx08 = new Idx(); // = idx07 & idx8
	private final Idx idx09 = new Idx(); // = idx08 & idx9

	private final MyCollisionComparator cc = new MyCollisionComparator();

	private final NonHinters64 nonHinters = new NonHinters64(256*1024, 4);

//	protected final Counter cnt1col = new Counter("cnt1col");
//	protected final Counter cnt1sib = new Counter("cnt1sib");
//	protected final Counter cnt1cs  = new Counter("cnt1cs");
//	protected final Counter cnt1mbs = new Counter("cnt1mbs");
//	protected final Counter cnt1ces = new Counter("cnt1ces");
//	protected final Counter cnt1hit = new Counter("cnt1hit");
//	protected final Counter cnt1sum = new Counter("cnt1sum");
//	protected final Counter cnt1prg = new Counter("cnt1prg");
//
//	protected final Counter cnt2col = new Counter("cnt2col");
//	protected final Counter cnt2sib = new Counter("cnt2sib");
//	protected final Counter cnt2cs  = new Counter("cnt2cs");
//	protected final Counter cnt2mbs = new Counter("cnt2mbs");
//	protected final Counter cnt2ces = new Counter("cnt2ces");
//	protected final Counter cnt2hit = new Counter("cnt2hit");
//	protected final Counter cnt2sum = new Counter("cnt2sum");
//	protected final Counter cnt2prg = new Counter("cnt2prg");

	//useless: cnt3*: A10E never hints from 3 cmnExclBits in top1465

//	private java.io.PrintStream myLog = open("a10e.log", standardHeader());

	public Aligned10Exclusion_1C(IInterruptMonitor monitor) {
		super(monitor);
	}

//	@Override
//	public void close() throws java.io.IOException {
//		hits.save();
//		//myLog.close();
//	}

	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		super.prepare(grid, logicalSolver);
		nonHinters.clear();
	}

	@SuppressWarnings("fallthrough")
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// these 4 vars are "special" for processing top1465.d5.mt faster
		// localise hackTop1465 for speed (and to make it final).
		// hackTop1465 is isHacky && puzzleFileName.contains("top1465")
		final boolean hackTop1465 = AHinter.hackTop1465;
		// istop1465 is just the puzzleFileName.contains("top1465")
		final boolean isTop1465 = grid.source!=null && grid.source.isTop1465;
		// filter top1465 even when !isHacky, else A5678910E took 34:16:04,
		// and I'm determined to not ever have to wait that long again.
		final boolean filterTop1465 = isTop1465 && !hackTop1465;
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
		final int[][] SVS = Values.VSHIFTED;

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 2 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// the number of cells in each for-i-loop
		final int n0, n1, n2, n3, n4, n5, n6, n7, n8;

		// an array of the 7 cells in an aligned set, ready for exclusion.
		final Cell[] cells = CELLS_ARRAY;
		// sortedCells: the above cells array sorted by the CollisionComparator.
		// nb: c0..c6 relate to scells array, not to the above cells array.
		// nb: coincident with avb* (allowedValuesBitsets)
		final Cell[] scells = CELLS_ARRAY_1;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// array of 3*27-bit-bitsets of indexes of c0's excluders in Grid.cells
		Idx idx0;

		// indexes in the candidates array of the 5 candidate cells.
		int i0, i1, i2, i3, i4, i5, i6, i7, i8, i9;

		// virtual array of the 10 candidate cells in an aligned exclusion set.
		Cell c0, c1, c2, c3, c4, c5, c6, c7, c8, c9;

		// notSiblings: a cache of the results of Cells isNotSiblingOf method.
		// EXAMPLE: ns10=!c1.isNotSiblingOf[c0.i];
		boolean ns10, ns20, ns30, ns40, ns50, ns60, ns70, ns80, ns90;
		boolean		  ns21, ns31, ns41, ns51, ns61, ns71, ns81, ns91;
		boolean				ns32, ns42, ns52, ns62, ns72, ns82, ns92;
		boolean					  ns43, ns53, ns63, ns73, ns83, ns93;
		boolean							ns54, ns64, ns74, ns84, ns94;
		boolean								  ns65, ns75, ns85, ns95;
		boolean										ns76, ns86, ns96;
		boolean											  ns87, ns97;
		boolean													ns98;

		// Bits of cell.maybes. When this cells value is chosen its value is
		// removed from the maybes of all the subsequent cells in the set which
		// are siblings of this cell. This is more code than "skip collision" as
		// as in A234E but it's faster, because it's doing more of the work less
		// often. Think DNA: a one-way trip departing the eye of my k___b.
		// So c1b0 is version-0 of c1.maybes.bits, with v0 removed if c1 is a
		// sibling of c0, and c1b (c1.maybes.bits) contained v0 to start with.
		int c0b, c1b , c2b , c3b , c4b , c5b , c6b , c7b , c8b , c9b;
		int		       c2b0, c3b0, c4b0, c5b0, c6b0, c7b0, c8b0, c9b0;
		int			         c3b1, c4b1, c5b1, c6b1, c7b1, c8b1, c9b1;
		int					 c3b2, c4b2, c5b2, c6b2, c7b2, c8b2, c9b2;
		int						   c4b3, c5b3, c6b3, c7b3, c8b3, c9b3;
		int								 c5b4, c6b4, c7b4, c8b4, c9b4;
		int									   c6b5, c7b5, c8b5, c9b5;
		int											 c7b6, c8b6, c9b6;
		int												   c8b7, c9b7;
		int														 c9b8;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01,sv02,sv03,sv04,sv05,sv06,sv07,sv08,sv09;

		// allowedValuesBitsets: a virtual array of bitsets of allowed values
		int avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7,avb8,avb9; // bitsets

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1,any2,any3,any4,any5,any6,any7,any8,any9;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // ie degree: the number of cells in the aligned set.

		// commonExcluderBits[0]
		int ceb0,ces0, ceb1,ces1;

		// if do30 check if ceb0 (2-or-3 values) covers the-combo-so-far, to
		// which we've just added the 3rd value (sv2).
		// do31 is the same but for ceb1 (ie cmnExclBits[1]).
		// nb: do6+* no exist coz presumed always true. It certainly is for
		//     case 1: which is 99.67% of aligned sets so don't care re 2 or 3.
		boolean do30,do31, do40,do41, do50,do51, do60,do61;

		// filter variables
		int fives, sixes, sevns, eigts;
		int mbs=0, col=0;
		int sib, sum, hit;
		//int cs=0, ces=0;
		double prang;//=0.0D;

		// presume failure (ie no hint found)
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//                     This method is so imbedded
		//               it's suffering from Aniocranial Inversion
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also the set of sibling excluder-cells for each candidate.
		// RE: That 8: The maximum aligned-set-cell.maybes.size which hints is 7
		numCandidates = populateCandidatesAndExcluders(candidates, excluders, grid, 8);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false; // this happens rarely, but those count BIG-time!
		}

		n0 = numCandidates - 9;
		n1 = numCandidates - 8;
		n2 = numCandidates - 7;
		n3 = numCandidates - 6;
		n4 = numCandidates - 5;
		n5 = numCandidates - 4;
		n6 = numCandidates - 3;
		n7 = numCandidates - 2;
		n8 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				if ( excluders[(cells[1]=candidates[i1]).i].idx1(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx1(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[(cells[3]=candidates[i3]).i].idx1(idx03, idx02) )
							continue;
						if(hitMe && cells[3]!=hitCells[3]) continue;
						for ( i4=i3+1; i4<n4; ++i4 ) {
							if ( excluders[(cells[4]=candidates[i4]).i].idx1(idx04, idx03) )
								continue;
							if(hitMe && cells[4]!=hitCells[4]) continue;
							for ( i5=i4+1; i5<n5; ++i5 ) {
								if ( excluders[candidates[i5].i].idx1(idx05, idx04) )
									continue;
								cells[5] = candidates[i5];
								if(hitMe && cells[5]!=hitCells[5]) continue;
								for ( i6=i5+1; i6<n6; ++i6 ) {
									if ( excluders[candidates[i6].i].idx1(idx06, idx05) )
										continue;
									cells[6] = candidates[i6];
									if(hitMe && cells[6]!=hitCells[6]) continue;
									for ( i7=i6+1; i7<n7; ++i7 ) {
										if ( excluders[candidates[i7].i].idx1(idx07, idx06) )
											continue;
										cells[7] = candidates[i7];
										if(hitMe && cells[7]!=hitCells[7]) continue;
										for ( i8=i7+1; i8<n8; ++i8 ) {
											if ( excluders[candidates[i8].i].idx1(idx08, idx07) )
												continue;
											cells[8] = candidates[i8];
											if(hitMe && cells[8]!=hitCells[8]) continue;
											if ( isInterrupted() )
												return false;
											for ( i9=i8+1; i9<numCandidates; ++i9 ) {
												if ( excluders[candidates[i9].i].idx1(idx09, idx08) )
													continue;
												cells[9] = candidates[i9];
												if(hitMe && cells[9]!=hitCells[9]) continue;

												if ( nonHinters.skip(cells) )
													continue;

												// filter top1465 when !isHacky rather than wait 34 hours again.
												// nb: magic numbers are from a10e.log via tools.AnalyseLog
												if ( filterTop1465 ) {
													// filter by total number of maybes shared by siblings
													col = countCollisions(cells); // countCollisions
//													colCnt.count(col);
													if(col<10 || col>91) continue;
//													++colCnt.pass;

													// filter by total cells.maybes.size
													mbs = totalMaybesSize; // from countCollisions
//													mbsCnt.count(mbs);
													if(mbs<21 || mbs>45) continue;
//													++mbsCnt.pass;

													// filter by total number of sibling relationships (edges)
													sib = siblingsCount; // from countCollisions
//													sibCnt.count(sib);
													if(sib<15 || sib>33) continue;
//													++sibCnt.pass;

													// this one is filtered in the switch
//													cs = col + sib;

													// filter by maximum number of "large" cells
													// aligned sets with more-than-6 cells with
													//    5-or-more maybes never produce a hint
													//    (guess this filter is probably pretty useless!);
													// aligned sets with more-than-2 cells with
													//    6-or-more maybes never produce a hint;
													// aligned sets with more-than-1 cell with
													//    7-or-more maybes never produce a hint
//													++maxMbs.cnt;
													fives = sixes = sevns = eigts = 0;
													for ( Cell cell : cells )
														switch ( cell.maybes.size ) {
														case 9: //fallthrough // 9 is the maximum possible
														case 8: ++eigts; //fallthrough
														case 7: ++sevns; //fallthrough
														case 6: ++sixes; //fallthrough
														case 5: ++fives; //fallout
														}
													if(fives>6) continue;
													if(sixes>2) continue;
													if(sevns>1) continue;
													if(eigts>0) continue;
//													++maxMbs.pass;
												}

												// read common excluder cells from grid at idx09
												if ( (numCmnExcls = idx09.cellsN(grid, cmnExcls)) == 1 ) {
													cmnExclBits[0] = cmnExcls[0].maybes.bits;
													numCmnExclBits = 1;
												} else {
													// performance enhancement: examine smaller maybes sooner.
													//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
													bubbleSort(cmnExcls, numCmnExcls);
													// get common excluders bits, removing any supersets,
													// eg: {12,125}=>{12} coz 125 covers 12, so 125 does nada.
													numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);
												}
												// remove each common excluder bits which contains a value
												// that is not in any candidates maybes, because no combo
												// can cover it, so it won't contribute to an exclusion.
												// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
												// skip if there are none remaining
												if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
														, allMaybesBits(cells))) == 0 )
													continue;

												// filter top1465 when !isHacky rather than wait 34 hours again.
												// nb: the magic numbers are from a10e.log via tools.AnalyseLog
												if ( filterTop1465 ) {
													hit = countHits(cmnExclBits, numCmnExclBits, cells);
//													hitCnt.count(hit); // relies on sum
													if(hit<1 || hit>36) continue;
//													++hitCnt.pass;

													sum = col + hit;
//													sumCnt.count(sum);
													if(sum<15 || sum>105) continue;
//													++sumCnt.pass;

													prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//													prangCnt.count(prang);
													if(prang<1.605D || prang>44.531D) continue;
//													++prangCnt.pass;
												}

												// set is overridden to sort cells by the number of intersections
												// with cmnExclBits then by cell.maybes.size. Note that I call
												// super.set and then change the scores[] AFTER the stats used by
												// the filters have all been calculated, so changing the scores[]
												// effects ONLY the sort, not the filters. Just beware of any odd
												// future filter which might reference the scores[]. Clear?
												cc.set(cells, cmnExclBits, numCmnExclBits);
												System.arraycopy(cells, 0, scells, 0, degree);
												//MyTimSort.small(scells, degree, cc);
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
												c9b = (c9=scells[9]).maybes.bits;

												// build the notSiblings cache
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

												ns90 = c9.notSees[c0.i];
												ns91 = c9.notSees[c1.i];
												ns92 = c9.notSees[c2.i];
												ns93 = c9.notSees[c3.i];
												ns94 = c9.notSees[c4.i];
												ns95 = c9.notSees[c5.i];
												ns96 = c9.notSees[c6.i];
												ns97 = c9.notSees[c7.i];
												ns98 = c9.notSees[c8.i];

												// should we test if 3/4/5 values cover ceb0
												ces0 = VSIZE[ceb0=cmnExclBits[0]];
												do30 = ces0 <= 3;
												do40 = ces0 <= 4;
												do50 = ces0 <= 5;
												do60 = ces0 <= 6;

												// clear the allowedValuesBits virtual array
												avb0=avb1=avb2=avb3=avb4=avb5=avb6=avb7=avb8=avb9 = 0;

												// anyLevel controls which for-i-loop to break-out-of when
												// all maybes of all cells to my right are already allowed;
												// so that the FIRST allowed combo sets my any* and we skip
												// all other "like" combos.
												// This is for performance only. Algorithm works without it.
												// It just avoids wasting time allowing multiple combos
												// whose right-hand-side-values are all already allowed.
												anyLevel = degree; // meaning that avb9 != c9b

												// The dog____ing loop determines if each combo is allowed;
												// ie: is each possible combination of the potential values
												// of these $degree (10) cells allowed? Else the combo is
												// disallowed, so it's skipped. We only populate a virtual
												// array of allowedValuesBitsets here, we'll build the
												// excludedCombosMap later, if any value/s are excluded;
												// ie all possible combinations of potential values do not
												// include certain value/s in certain position/s.
												// After this loop we check if any of each cells maybes is
												// not included in any allowed combo, ie an exclusion, ie
												// we finally found a bloody hint. It's allways the last
												// place you look!
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
												case 1: {
													// we don't need a common excluders loop
													// unfiltered: 1 = 438,294,614 of 438,576,903 = 99.94%
													// filtered  : 1 =  34,566,223 of  34,576,857 = 99.97%
													// wit HitSet: 1 =         136 of         139 = 97.84%

													// filter top1465 even when !isHacky, to not take 34 hours again.
													// nb: the magic numbers are from a8e.log via tools.AnalyseLog
													if ( filterTop1465 ) {
//														cnt1col.count(col);
		//												if(col<? || col>?) continue;
//														++cnt1col.pass;

//														cnt1sib.count(sib);
		//												if(sib<? || sib>?) continue;
//														++cnt1sib.pass;

//														cnt1cs.count(cs);
		//												if(cs<? || cs>?) continue;
//														++cnt1cs.pass;

//useless:												cnt1mbs.count(mbs);
		//												if(mbs<? || mbs>?) continue;
//														++cnt1mbs.pass;

//														ces = ces0;
//														cnt1ces.count(ces);
		//												if(ces>?) continue;
//														++cnt1ces.pass;

//														cnt1hit.count(hit);
		//												if(hit<? || hit>?) continue;
//														++cnt1hit.pass;

//														cnt1sum.count(sum);
		//												if(sum<? || sum>?) continue;
//														++cnt1sum.pass;

//														cnt1prg.count(prang);
//														if(prang<? || prang>?) continue;
//														++cnt1prg.pass;
													}
													DOG_1a: for ( int sv0 : SVS[c0b] ) {
														c9b0 = ns90 ? c9b : c9b & ~sv0;
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
															c9b1 = ns91 ? c9b0 : c9b0 & ~sv1;
															c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
															c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
															c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
															c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
															c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
															c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
															any2 = false;
															SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
																sv02 = sv01 | sv2;
																if ( (do30 && (ceb0 & ~sv02) == 0)
																  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
																	continue;
																c9b2 = ns92 ? c9b1 : c9b1 & ~sv2;
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
																	c9b3 = ns93 ? c9b2 : c9b2 & ~sv3;
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
																		c9b4 = ns94 ? c9b3 : c9b3 & ~sv4;
																		c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																		c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																		c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																		any5 = false;
																		SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																			sv05 = sv04 | sv5;
																			if ( (do60 && (ceb0 & ~sv05) == 0)
																			  || (c9b5 = ns95 ? c9b4 : c9b4 & ~sv5) == 0
																			  || (c8b5 = ns85 ? c8b4 : c8b4 & ~sv5) == 0
																			  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																			  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																				continue;
																			any6 = false;
																			SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																				if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																				  || (c9b6 = ns96 ? c9b5 : c9b5 & ~sv6) == 0
																				  || (c8b6 = ns86 ? c8b5 : c8b5 & ~sv6) == 0
																				  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																					continue;
																				any7 = false;
																				SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																					if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																					  || (c9b7 = ns97 ? c9b6 : c9b6 & ~sv7) == 0
																					  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																						continue;
																					any8 = false;
																					SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																						if ( (ceb0 & ~(sv08=sv07|sv8)) == 0
																						  || (c9b8 = ns98 ? c9b7 : c9b7 & ~sv8) == 0 )
																							continue;
																						any9 = false;
																						SV9_LOOP: for ( int sv9 : SVS[c9b8] ) {
																							if ( (ceb0 & ~(sv08|sv9)) == 0 )
																								continue;
																							// Combo Allowed!
																							any9 = true;
																							// add to allowedValuesBitsets
																							// and check if that makes all allowed.
																							if ( (avb9|=sv9) != c9b )
																								continue;
																							// We allow only the FIRST combo at each level; and then move
																							// on to evaluating the next value at this level which hasn't
																							// yet been evaluated; instead of wasting time evaluating
																							// multiple combos-to-my-right which can not allow any further
																							// maybes, because they're all already allowed.
																							// This reduces      O(n*n*n*n*n*n*n*n*n*n) to O(n*n*n'ish)
																							// or "Combo Allowed"s 11,944,875,814,008   to 16,254,988,836
																							// WTF: My n*n*n*n*n*n*n*n*n*n averages 345,564.97 and
																							// 34,566,223 sets * 345,564.97 = 11,944,875,814,008 allowed;
																							// and allowedCount = 16,254,988,836 with anyLevel enabled.
																							// So from 11.945 TRIL down to 16.255 BILL is only 735 times
																							// faster, on paper. So anyLevel works and beware that REALLY
																							// BIG numbers eat ugly babies, and you are seriously ugly!
																							switch ( anyLevel ) {
																							case 10: anyLevel=9; break SV9_LOOP; // any9 is already true
																							case 9: break SV9_LOOP; // any9 is already true, and anyLevel is already 9
																							case 8: any8=true; break SV8_LOOP;
																							case 7: any7=true; break SV7_LOOP;
																							case 6: any6=true; break SV6_LOOP;
																							case 5: any5=true; break SV5_LOOP;
																							case 4: any4=true; break SV4_LOOP;
																							case 3: any3=true; break SV3_LOOP;
																							case 2: any2=true; break SV2_LOOP;
																							case 1: any1=true; break SV1_LOOP;
																							} // end-switch
																						} // next sv9
																						if ( any9 ) {
																							any8 = true;
																							// nb: if anyLevel==9 stops any shortcuts
																							// until avb9==c9b
																							if ( (avb8|=sv8)==c8b && anyLevel==9 )
																								anyLevel = 8;
																						}
																					} // next sv8
																					if ( any8 ) {
																						any7 = true;
																						// nb: if anyLevel==8 stops any further shortcuts
																						// until avb8==c8b && avb9==c9b
																						if ( (avb7|=sv7)==c7b && anyLevel==8 )
																							anyLevel = 7;
																					}
																				} // next sv7
																				if ( any7 ) {
																					any6 = true;
																					// nb: if anyLevel==7 stops any further shortcuts
																					// until avb7==c7b && avb8==c8b && avb9==c9b
																					if ( (avb6|=sv6)==c6b && anyLevel==7 )
																						anyLevel = 6;
																				}
																			} // next sv6
																			if ( any6 ) {
																				any5 = true;
																				// nb: anyLevel==6 is avb6==c6b && avb7==c7b
																				//                 && avb8==c8b && avb9==c9b
																				if ( (avb5|=sv5)==c5b && anyLevel==6 )
																					anyLevel = 5;
																			}
																		} // next sv5
																		if ( any5 ) {
																			any4 = true;
																			// nb: anyLevel==5 is avb5==c5b && avb6==c6b && avb7==c7b
																			//                 && avb8==c8b && avb9==c9b
																			if ( (avb4|=sv4)==c4b && anyLevel==5 )
																				anyLevel = 4;
																		}
																	} // next sv4
																	if ( any4 ) {
																		any3 = true;
																		// nb: anyLevel==4 is avb4==c4b && avb5==c5b && avb6==c6b
																		//                 && avb7==c7b && avb8==c8b && avb9==c9b
																		if ( (avb3|=sv3)==c3b && anyLevel==4 )
																			anyLevel = 3;
																	}
																} // next sv3
																if ( any3 ) {
																	any2 = true;
																	// nb: anyLevel==3 is avb3==c3b && avb4==c4b && avb5==c5b && avb6==c6b
																	//                 && avb7==c7b && avb8==c8b && avb9==c9b
																	if ( (avb2|=sv2)==c2b && anyLevel==3 )
																		anyLevel = 2;
																}
															} // next sv2
															if ( any2 ) {
																any1 = true;
																// nb: anyLevel==2 is avb2==c2b && avb3==c3b && avb4==c4b && avb5==c5b
																//                 && avb6==c6b && avb7==c7b && avb8==c8b && avb9==c9b
																// Being that much more succinct yields a significant speed gain, even with
																// the switch on each "Combo Allowed" to jump straight to this "any2" block
																// to move-directly-onto the-next-value-at-this-level; so my anyLevel is an
																// on-water efficiency matter. Nothing to see here. Move along!
																if ( (avb1|=sv1)==c1b && anyLevel==2 )
																	anyLevel = 1;
															}
														} // next sv1
														if(any1) avb0|=sv0;
													} // next sv0
												} break;
												case 2: {
													// we still don't need a common excluders loop
													// unfiltered: 2 = 277,765 of 438,576,903 = 0.06%
													// filtered  : 2 =  10,623 of  34,576,857 = 0.03%
													// wit HitSet: 2 =       3 of         139 = 2.16%
													ces1 = VSIZE[ceb1=cmnExclBits[1]];
													do31 = ces1 <= 3;
													do41 = ces1 <= 4;
													do51 = ces1 <= 5;
													do61 = ces1 <= 6;

													// filter top1465 even when !isHacky, to not take 34 hours again.
													// nb: the magic numbers are from a8e.log via tools.AnalyseLog
													if ( filterTop1465 ) {
//														cnt2col.count(col);
		//												if(col<? || col>?) continue;
//														++cnt2col.pass;

//														cnt2sib.count(sib);
		//												if(sib<? || sib>?) continue;
//														++cnt2sib.pass;

//														cnt2cs.count(cs);
		//												if(cs<? || cs>?) continue;
//														++cnt2cs.pass;

//useless:												cnt2mbs.count(mbs);
		//												if(mbs<? || mbs>?) continue;
//														++cnt2mbs.pass;

//														ces = ces0;
//														cnt2ces.count(ces);
		//												if(ces<? || ces>?) continue;
//														++cnt2ces.pass;

//														cnt2hit.count(hit);
		//												if(hit<? || hit>?) continue;
//														++cnt2hit.pass;

//														cnt2sum.count(sum);
		//												if(sum<? || sum>?) continue;
//														++cnt2sum.pass;

//														cnt2prg.count(prang);
		//												if(prang<? || prang>?) continue;
//														++cnt2prg.pass;
													}
													DOG_1b: for ( int sv0 : SVS[c0b] ) {
														c9b0 = ns90 ? c9b : c9b & ~sv0;
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
															c9b1 = ns91 ? c9b0 : c9b0 & ~sv1;
															c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
															c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
															c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
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
																c9b2 = ns92 ? c9b1 : c9b1 & ~sv2;
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
																	c9b3 = ns93 ? c9b2 : c9b2 & ~sv3;
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
																		c9b4 = ns94 ? c9b3 : c9b3 & ~sv4;
																		c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																		c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																		c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																		any5 = false;
																		SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																			sv05 = sv04 | sv5;
																			if ( (do60 && (ceb0 & ~sv05) == 0)
																			  || (do61 && (ceb1 & ~sv05) == 0)
																			  || (c9b5 = ns95 ? c9b4 : c9b4 & ~sv5) == 0
																			  || (c8b5 = ns85 ? c8b4 : c8b4 & ~sv5) == 0
																			  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																			  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																				continue;
																			any6 = false;
																			SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																				if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																				  || (ceb1 & ~sv06) == 0
																				  || (c9b6 = ns96 ? c9b5 : c9b5 & ~sv6) == 0
																				  || (c8b6 = ns86 ? c8b5 : c8b5 & ~sv6) == 0
																				  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																					continue;
																				any7 = false;
																				SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																					if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																					  || (ceb1 & ~sv07) == 0
																					  || (c9b7 = ns97 ? c9b6 : c9b6 & ~sv7) == 0
																					  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																						continue;
																					any8 = false;
																					SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																						if ( (ceb0 & ~(sv08=sv07|sv8)) == 0
																						  || (ceb1 & ~sv08) == 0
																						  || (c9b8 = ns98 ? c9b7 : c9b7 & ~sv8) == 0 )
																							continue;
																						any9 = false;
																						SV9_LOOP: for ( int sv9 : SVS[c9b8] ) {
																							if ( (ceb0 & ~(sv09=sv08|sv9)) == 0
																							  || (ceb1 & ~sv09) == 0 )
																								continue;
																							// Combo Allowed!
																							any9 = true;
																							// add to allowedValuesBitsets
																							// and check if that makes all allowed.
																							if ( (avb9|=sv9) != c9b )
																								continue;
																							// We allow only the FIRST combo at each level.
																							switch ( anyLevel ) {
																							case 10: anyLevel=9; break SV9_LOOP;
																							case 9: break SV9_LOOP;
																							case 8: any8=true; break SV8_LOOP;
																							case 7: any7=true; break SV7_LOOP;
																							case 6: any6=true; break SV6_LOOP;
																							case 5: any5=true; break SV5_LOOP;
																							case 4: any4=true; break SV4_LOOP;
																							case 3: any3=true; break SV3_LOOP;
																							case 2: any2=true; break SV2_LOOP;
																							case 1: any1=true; break SV1_LOOP;
																							} // end-switch
																						} // next sv9
																						if ( any9 ) {
																							any8 = true;
																							if ( (avb8|=sv8)==c8b && anyLevel==9 )
																								anyLevel = 8;
																						}
																					} // next sv8
																					if ( any8 ) {
																						any7 = true;
																						if ( (avb7|=sv7)==c7b && anyLevel==8 )
																							anyLevel = 7;
																					}
																				} // next sv7
																				if ( any7 ) {
																					any6 = true;
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
												} break;
												default:
													// the common excluders loop is in the covers method.
													// unfiltered: 3 = 4,524 of 438,576,903 = 0.00%
													// filtered  : 3 =    11 of  34,576,857 = 0.00%
													// wit HitSet: 3 =     0 of         139 = 0.00%
													if ( isTop1465 )
														continue; // there are 0 hints from 3 cmnExcls
													DOG_1c: for ( int sv0 : SVS[c0b] ) {
														c9b0 = ns90 ? c9b : c9b & ~sv0;
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
															c9b1 = ns91 ? c9b0 : c9b0 & ~sv1;
															c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
															c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
															c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
															c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
															c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
															c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
															any2 = false;
															SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
																sv02 = sv01 | sv2;
																if ( (do30 && covers(cmnExclBits, numCmnExclBits, sv02))
																  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
																	continue;
																c9b2 = ns92 ? c9b1 : c9b1 & ~sv2;
																c8b2 = ns82 ? c8b1 : c8b1 & ~sv2;
																c7b2 = ns72 ? c7b1 : c7b1 & ~sv2;
																c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
																c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
																c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
																any3 = false;
																SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
																	sv03 = sv02 | sv3;
																	if ( (do40 && covers(cmnExclBits, numCmnExclBits, sv03))
																	  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
																		continue;
																	c9b3 = ns93 ? c9b2 : c9b2 & ~sv3;
																	c8b3 = ns83 ? c8b2 : c8b2 & ~sv3;
																	c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
																	c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
																	c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
																	any4 = false;
																	SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																		sv04 = sv03 | sv4;
																		if ( (do50 && covers(cmnExclBits, numCmnExclBits, sv04))
																		  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																			continue;
																		c9b4 = ns94 ? c9b3 : c9b3 & ~sv4;
																		c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																		c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																		c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																		any5 = false;
																		SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																			sv05 = sv04 | sv5;
																			if ( (do60 && covers(cmnExclBits, numCmnExclBits, sv05))
																			  || (c9b5 = ns95 ? c9b4 : c9b4 & ~sv5) == 0
																			  || (c8b5 = ns85 ? c8b4 : c8b4 & ~sv5) == 0
																			  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																			  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																				continue;
																			any6 = false;
																			SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																				if ( covers(cmnExclBits, numCmnExclBits, sv06=sv05|sv6)
																				  || (c9b6 = ns96 ? c9b5 : c9b5 & ~sv6) == 0
																				  || (c8b6 = ns86 ? c8b5 : c8b5 & ~sv6) == 0
																				  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																					continue;
																				any7 = false;
																				SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																					if ( covers(cmnExclBits, numCmnExclBits, sv07=sv06|sv7)
																					  || (c9b7 = ns97 ? c9b6 : c9b6 & ~sv7) == 0
																					  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																						continue;
																					any8 = false;
																					SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																						if ( covers(cmnExclBits, numCmnExclBits, sv08=sv07|sv8)
																						  || (c9b8 = ns98 ? c9b7 : c9b7 & ~sv8) == 0 )
																							continue;
																						any9 = false;
																						SV9_LOOP: for ( int sv9 : SVS[c9b8] ) {
																							if ( covers(cmnExclBits, numCmnExclBits, sv08|sv9) )
																								continue;
																							// Combo Allowed!
																							any9 = true;
																							// add to allowedValuesBitsets
																							// and check if that makes all allowed.
																							if ( (avb9|=sv9) != c9b )
																								continue;
																							// We allow only the FIRST combo at each level
																							switch ( anyLevel ) {
																							case 10: anyLevel=9; break SV9_LOOP;
																							case 9: break SV9_LOOP;
																							case 8: any8=true; break SV8_LOOP;
																							case 7: any7=true; break SV7_LOOP;
																							case 6: any6=true; break SV6_LOOP;
																							case 5: any5=true; break SV5_LOOP;
																							case 4: any4=true; break SV4_LOOP;
																							case 3: any3=true; break SV3_LOOP;
																							case 2: any2=true; break SV2_LOOP;
																							case 1: any1=true; break SV1_LOOP;
																							} // end-switch
																						} // next sv9
																						if ( any9 ) {
																							any8 = true;
																							if ( (avb8|=sv8)==c8b && anyLevel==9 )
																								anyLevel = 8;
																						}
																					} // next sv8
																					if ( any8 ) {
																						any7 = true;
																						if ( (avb7|=sv7)==c7b && anyLevel==8 )
																							anyLevel = 7;
																					}
																				} // next sv7
																				if ( any7 ) {
																					any6 = true;
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

												// if all values are allowed
												if ( avb0 == c0b
												  && avb1 == c1b
												  && avb2 == c2b
												  && avb3 == c3b
												  && avb4 == c4b
												  && avb5 == c5b
												  && avb6 == c6b
												  && avb7 == c7b
												  && avb8 == c8b
												  && avb9 == c9b ) {
													nonHinters.put();
													continue; // then no hint here. Move along!
												}

												// ===========================================
												// Performance is not an issue from here down.
												// To get here a value/s is/are excluded.
												// ===========================================

++hintCounts[numCmnExclBits];
												if ( isTop1465 ) {
													hitCounters();
													hits.add(gsl, hintNum, cells);
												}

												// create the removable (red) potential values.
												Pots redPots = createRedPotentials(scells
														, avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7,avb8,avb9);
												if ( redPots.isEmpty() )
													continue; // Should never happen. Never say never.
												// build the excluded combos map for the hint
												ExcludedCombosMap map = buildExcludedCombosMap(
														cmnExcls, numCmnExcls, cells, redPots);
												// create and add the hint
												AHint hint = new AlignedExclusionHint(this
														, redPots, cells, numCmnExcls, cmnExcls, map);

//												standardLog(myLog, cells, gsl, hintNum
//														, cmnExcls, numCmnExcls
//														, cmnExclBits, numCmnExclBits
//														, redPots, map, hint);

												result = true; // in case add returns false
												if ( accu.add(hint) )
													return result;

											} // next c9
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

	private final class MyCollisionComparator extends ACollisionComparator {

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overridden to sort cells by the number of intersections with
		 * cmnExclBits then by cell.maybes.size. Note that I call super.set
		 * and then change the scores[] AFTER the stats used by the filters
		 * have all been calculated, so this change to scores[] effects ONLY
		 * the sort, not the filters. Just beware of any odd future filter
		 * which might reference the scores[]. Clear?
		 */
		@Override
		void set(Cell[] cells, int[] cmnExclBits, int numCmnExclBits) {
			super.set(cells, cmnExclBits, numCmnExclBits);
			Values cellMaybes;  int intersections;
			for ( int i=0,n=degree; i<n; ++i ) {
				cellMaybes = cells[i].maybes;
				intersections = 0;
				for ( int j=0; j<numCmnExclBits; ++j )
					intersections += VSIZE[cellMaybes.bits & cmnExclBits[j]];
				// by intersections with cmnExclBits then cell.maybes.size
				scores[i] = (intersections << 4) | cellMaybes.size;
			}
		}
	}

}
