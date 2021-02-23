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
 * Aligned 8 (Oct) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 * of 8 Cells.
 */
public final class Aligned8Exclusion_1C extends Aligned8ExclusionBase
		implements java.io.Closeable
				 , diuf.sudoku.solver.IReporter
				 , diuf.sudoku.solver.IPreparer
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 34; // <HACK/>

	// the maximim number of candidates to permute (process).
	// the max possible is 81 - 17 (ie min clues) = 64, but
	// permutations(8,64) is 4,426,165,368 whereas
	// permutations(8,60) is 2,558,620,845
	// see diuf.sudoku.utils.Permutations for more.
	private static final int MAX_CANDIDATES = 60; // <HACK/>

	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] commonExcludersArray = new Cell[NUM_CMN_EXCLS];
	private static final int[] commonExcluderBitsArray = new int[NUM_CMN_EXCLS];

	private static final Cell[] cellsArray = new Cell[8];
	private static final Cell[] cellsArray1 = new Cell[8];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5
	private final Idx idx06 = new Idx(); // = idx05 & idx6
	private final Idx idx07 = new Idx(); // = idx06 & idx7

	private final ACollisionComparator cc = new ACollisionComparator();

	//  32*1024:    561,548,668,600	   4107	   136,729,649	    160	 3,509,679,178	Aligned Oct
	//  64*1024:    475,616,281,200	   4149	   114,633,955	    208	 2,286,616,736	Aligned Oct
	// 128*1024:    695,529,388,000	   4107	   169,352,176	    160	 4,347,058,675	Aligned Oct
	// 256*1024:    482,853,009,600	   4149	   116,378,165	    208	 2,321,408,700	Aligned Oct
	// 2020-12-05 14:35 AAlignedSetExclusionBase.NonHinters.maxSize = 281,675
	// I think we're starting to become memory bound, so large "helper sets"
	// like this one start to overwork the GC, so everything bogs down.
	private final NonHinters nonHinters = new NonHinters(64*1024, 3);
	// What's that Skip? Why it's the skipper skipper flipper Flipper.
	private boolean firstPass = true;

//	//useless: protected final Counter cnt1col = new Counter("cnt1col");
//	//useless: protected final Counter cnt1sib = new Counter("cnt1sib");
//	protected final Counter cnt1cs  = new Counter("cnt1cs");
//	//useless: protected final Counter cnt1mbs = new Counter("cnt1mbs");
//	protected final Counter cnt1ces = new Counter("cnt1ces");
//	protected final Counter cnt1hit = new Counter("cnt1hit");
//	//useless: protected final Counter cnt1sum = new Counter("cnt1sum");
//	//useless: protected final Counter cnt1prg = new Counter("cnt1prg");
//
//	protected final Counter cnt2col = new Counter("cnt2col");
//	//useless: protected final Counter cnt2sib = new Counter("cnt2sib");
//	protected final Counter cnt2cs  = new Counter("cnt2cs");
//	protected final Counter cnt2mbs = new Counter("cnt2mbs");
//	//useless: protected final Counter cnt2ces = new Counter("cnt2ces");
//	protected final Counter cnt2hit = new Counter("cnt2hit");
//	protected final Counter cnt2sum = new Counter("cnt2sum");
//	protected final Counter cnt2prg = new Counter("cnt2prg");
//
//	protected final Counter cnt3col = new Counter("cnt3col");
//	//useless: protected final Counter cnt3sib = new Counter("cnt3sib");
//	protected final Counter cnt3cs  = new Counter("cnt3cs");
//	protected final Counter cnt3mbs = new Counter("cnt3mbs");
//	//useless: protected final Counter cnt3ces = new Counter("cnt3ces");
//	protected final Counter cnt3hit = new Counter("cnt3hit");
//	protected final Counter cnt3sum = new Counter("cnt3sum");
//	protected final Counter cnt3prg = new Counter("cnt3prg");

//	private java.io.PrintStream myLog = open("a8e.log", standardHeader());

	public Aligned8Exclusion_1C(int firstHintNumber, IInterruptMonitor monitor) {
		super(firstHintNumber, monitor);
	}

	@Override
	public void close() throws java.io.IOException {
		hits.save();
//		myLog.close();
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

	@SuppressWarnings("fallthrough")
	private boolean findHintsImpl(Grid grid, IAccumulator accu) {

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
		// that are packed into your maybes.bits 0..511. See Values.SHIFTED.
		final int[][] SVS = Values.VSHIFTED;

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 2 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = candidatesArray;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = excludersArray;

		// the number of cells in each of the for-i loops.
		final int n0, n1, n2, n3, n4, n5, n6;

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

		// indexes in the candidates array of the 5 candidate cells.
		int i0, i1, i2, i3, i4, i5, i6, i7;

		// a virtual array of the 7 candidate cells in an aligned exclusion set.
		Cell c0, c1, c2, c3, c4, c5, c6, c7;

		// array of 3*27-bit-bitsets of indexes of c0's excluders in Grid.cells
		Idx idx0;

		// notSiblings cache: ns10 = c1.isNotSiblingOf[c0.i];
		boolean ns10, ns20, ns30, ns40, ns50, ns60, ns70;
		boolean		  ns21, ns31, ns41, ns51, ns61, ns71;
		boolean				ns32, ns42, ns52, ns62, ns72;
		boolean					  ns43, ns53, ns63, ns73;
		boolean							ns54, ns64, ns74;
		boolean								  ns65, ns75;
		boolean										ns76;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01,sv02,sv03,sv04,sv05,sv06,sv07;

		// allowedValuesBits: bitsets of the allowedValues
		int avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7; // real bitsets

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is more code than
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		// c1b0 is an acronymn for: c1 bits zero, meaning the maybes.bits of c1
		// minus v0 (presuming that c1 is a sibling of c0).
		int c0b, c1b , c2b , c3b , c4b , c5b , c6b , c7b ;
		int		       c2b0, c3b0, c4b0, c5b0, c6b0, c7b0;
		int			         c3b1, c4b1, c5b1, c6b1, c7b1;
		int					 c3b2, c4b2, c5b2, c6b2, c7b2;
		int						   c4b3, c5b3, c6b3, c7b3;
		int								 c5b4, c6b4, c7b4;
		int									   c6b5, c7b5;
		int											 c7b6;

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1,any2,any3,any4,any5,any6,any7;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // ie degree: the number of cells in the aligned set.

		// ceb0=commonExcluderBits[0], ces0=size[ceb0]
		int ceb0,ces0, ceb1,ces1;

		// should we empty-test combo after adding 'n' values to it?
		boolean do30,do31, do40,do41, do50,do51;

		// filter variables
//		int fives, sixes, sevns, eigts;
		int mbs=0, col=0, sib=0, cs=0, sum=0, hit=0, ces=0;
		double prang=0.0D;

		// presume failure, ie no hint found.
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//              and they're off in the long-drop pole vault
		//                           stay up wind
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also the set of sibling excluder-cells for each candidate.
		numCandidates = LOCAL_populateCandidatesAndExcluders(candidates, excluders, grid);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

		n0 = numCandidates - 7;
		n1 = numCandidates - 6;
		n2 = numCandidates - 5;
		n3 = numCandidates - 4;
		n4 = numCandidates - 3;
		n5 = numCandidates - 2;
		n6 = numCandidates - 1;
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
									if ( isInterrupted() )
										return false;
									for ( i7=i6+1; i7<numCandidates; ++i7 ) {
										if ( excluders[candidates[i7].i].idx1(idx07, idx06) )
											continue;
										cells[7] = candidates[i7];
										if(hitMe && cells[7]!=hitCells[7]) continue;

											if ( nonHinters.skip(cells) )
												continue;

////1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
////7.8...4...2..7..83..32.8..9.7.5.63...6..2..9...1..7..6...3.29...3..4..6...9..1.35
////,19,,69,369,359,,25,12,19,,56,149,,45,156,,,46,45,,,15,,167,17,,89,,24,,189,,,124,148,35,,45,18,,34,178,,178,23,89,,489,39,,25,245,,168,158,67,,568,,,147,1478,1258,,257,789,,59,1278,,1278,2468,48,,678,68,,278,,
////54   	    188,936,100	31	 129	  1	Aligned Oct                   	Aligned Oct: A2, A3, A4, B6, E6, A7, A8, A9 (A8-1)
//if ( AHint.hintNumber == 54
//&& "A9".equals(cells[7].id)
//&& "A8".equals(cells[6].id)
//&& "A7".equals(cells[5].id)
//&& "E6".equals(cells[4].id)
//&& "B6".equals(cells[3].id)
//&& "A4".equals(cells[2].id)
//&& "A3".equals(cells[1].id)
//&& "A2".equals(cells[0].id)
//)
//	Debug.breakpoint();

										// filter top1465 even when !isHacky, to not take 34 hours again.
										// nb: the magic numbers are from a8e.log via tools.AnalyseLog
										if ( filterTop1465 ) {
											// filter by total number of maybes shared by siblings
											col = countCollisions(cells); // countCollisions
//											colCnt.count(col);
											// colCnt min=9/0 max=62/114 pass 1,202,308,440 of 1,259,170,435 skip 56,861,995 = 4.52%
											if(col<9 || col>62) continue;
//											++colCnt.pass;

											// filter by total cells.maybes.size
											mbs = totalMaybesSize; // from countCollisions
//											mbsCnt.count(mbs);
											// mbsCnt min=17/16 max=34/48 pass 1,146,988,339 of 1,202,308,440 skip 55,320,101 = 4.60%
											if(mbs<17 || mbs>34) continue;
//											++mbsCnt.pass;

											// filter by total number of sibling relationships (edges)
											sib = siblingsCount; // from countCollisions
//											sibCnt.count(sib);
											// sibCnt min=12/7 max=23/28 pass 671,531,500 of 1,146,988,339 skip 475,456,839 = 41.45%
											if(sib<12 || sib>23) continue;
//											++sibCnt.pass;

											// this one is filtered in the switch
											cs = col + sib;

											// filter by maximum number of "large" cells
//											++maxMbs.cnt;
//											fives = sixes = sevns = eigts = 0;
//											for ( Cell cell : cells )
//												switch ( cell.maybes.size ) {
//												case 9: //fallthrough // 9 is the maximum possible
//												case 8: ++eigts; //fallthrough
//												case 7: ++sevns; //fallthrough
//												case 6: ++sixes; //fallthrough
//												case 5: ++fives; //fallout
//												}
//											// maxMbs pass 670,977,050 of 671,531,500 skip 554,450 = 0.08%
//											// nb: with just 0.08% eliminated this probably costs more to calculate that it saves!
//											if(fives>6) continue;
//											if(sixes>2) continue;
//											if(sevns>1) continue;
//											if(eigts>0) continue;
//											++maxMbs.pass;
										}

										// get the common excluder cells from grid at idx07
										if ( (numCmnExcls = idx07.cellsN(grid, cmnExcls)) == 1 ) {
											cmnExclBits[0] = cmnExcls[0].maybes.bits;
											numCmnExclBits = 1;
										} else {
											// performance enhancement: examine smaller maybes sooner.
											//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
											bubbleSort(cmnExcls, numCmnExcls);
											// get the common excluder bits, minus any supersets,
											// eg: {12,125}=>{12} coz 125 covers 12 so 125 does nada.
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

										// filter top1465 even when !isHacky, to not take 34 hours again.
										// nb: the magic numbers are from a8e.log via tools.AnalyseLog
										if ( filterTop1465 ) {
											hit = countHits(cmnExclBits, numCmnExclBits, cells);
//											hitCnt.count(hit); // relies on sum
											// hitCnt min=2/2 max=32/89 pass 635,720,645 of 637,643,218 skip 1,922,573 = 0.30%
											if(hit<2 || hit>32) continue;
//											++hitCnt.pass;

											sum = col + hit;
//											sumCnt.count(sum);
											// sumCnt min=11/11 max=83/94 pass 635,701,012 of 635,720,645 skip 19,633 = 0.00%
											if(sum<11 || sum>83) continue;
//											++sumCnt.pass;

											prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//											prangCnt.count(prang);
											// prangRate min=1.654/1.501 max=11.667/18.700 pass 631,208,682 of 635,701,012 skip 4,492,330 = 0.71%
											if(prang<1.653D || prang>11.668D) continue;
//											++prangCnt.pass;
										}

										// the dog____ing algorithm is faster with dodgem-cars to the left,
										// but the above for-i-loops need a static cells array; so we copy
										// cells to scells (sortedCells) and sort that array DESCENDING by:
										// 4*maybesCollisions + 2*commonExcluderHits + maybes.size
										cc.set(cells, cmnExclBits, numCmnExclBits);
										System.arraycopy(cells, 0, scells, 0, degree);
										//MyTimSort.small(scells, degree, cc);
										bubbleSort(scells, degree, cc);

										// cache the sortedCells, and there maybes.
										c0b = (c0=scells[0]).maybes.bits;
										c1b = (c1=scells[1]).maybes.bits;
										c2b = (c2=scells[2]).maybes.bits;
										c3b = (c3=scells[3]).maybes.bits;
										c4b = (c4=scells[4]).maybes.bits;
										c5b = (c5=scells[5]).maybes.bits;
										c6b = (c6=scells[6]).maybes.bits;
										c7b = (c7=scells[7]).maybes.bits;

										// build the isNotSibingsOf cache
										ns10 = c1.notSees[c0.i];

										ns21 = c2.notSees[c1.i];
										ns20 = c2.notSees[c0.i];

										ns32 = c3.notSees[c2.i];
										ns31 = c3.notSees[c1.i];
										ns30 = c3.notSees[c0.i];

										ns43 = c4.notSees[c3.i];
										ns42 = c4.notSees[c2.i];
										ns41 = c4.notSees[c1.i];
										ns40 = c4.notSees[c0.i];

										ns54 = c5.notSees[c4.i];
										ns53 = c5.notSees[c3.i];
										ns52 = c5.notSees[c2.i];
										ns51 = c5.notSees[c1.i];
										ns50 = c5.notSees[c0.i];

										ns65 = c6.notSees[c5.i];
										ns64 = c6.notSees[c4.i];
										ns63 = c6.notSees[c3.i];
										ns62 = c6.notSees[c2.i];
										ns61 = c6.notSees[c1.i];
										ns60 = c6.notSees[c0.i];

										ns70 = c7.notSees[c0.i];
										ns71 = c7.notSees[c1.i];
										ns72 = c7.notSees[c2.i];
										ns73 = c7.notSees[c3.i];
										ns74 = c7.notSees[c4.i];
										ns75 = c7.notSees[c5.i];
										ns76 = c7.notSees[c6.i];

										// should we test if 3/4/5 values cover ceb0
										ces0 = VSIZE[ceb0=cmnExclBits[0]];
										do30 = ces0 <= 3;
										do40 = ces0 <= 4;
										do50 = ces0 <= 5;

										// clear the allowedValuesBitsets virtual array
										avb0=avb1=avb2=avb3=avb4=avb5=avb6=avb7 = 0;

										// anyLevel controls which for-i-loop to break-out-of when
										// all maybes of all cells to my right are already allowed;
										// so that the FIRST allowed combo sets my any* and we skip
										// all other "like" combos.
										// This is for performance only. Algorithm works without it.
										// We just don't waste time allowing multiple combos whose
										// right-hand-side-values are all already allowed.
										anyLevel = degree; // meaning that avb7 != c7b

										// The dog____ing loop calculates: is each combo allowed?
										// ie: Is each possible combination of the potential values
										// of these $degree (8) cells allowed? If so then we add
										// it's values to the allowedValuesBitsets (avb*) array;
										// else the combo is skipped with a continue statement.
										// At the end of the loop we see if all potential values of
										// all cells have been allowed, and if so we continue on to
										// the next aligned set (nothing to see here, move along!).
										// But if not then certain potential value/s of certain
										// cell/s are excluded, so we build the redPots (removable
										// potentials), the ExcludedCombosMap, and the hint.
										// There are 2 exclusion rules:
										// (1) Hidden Single: Using the same value for two cells
										//     is allowed only if the cells do not share a region.
										// (2) common excluder rule: If the combo covers
										//     (is a superset of) any common excluder cells maybes
										//     then the combo is not allowed, because the excluder
										//     cell must be one of those values (no matter which).
										//
										// NB: it's faster to "pre-eliminate" ie remove maybes from
										// subsequent sibling cells for large (4+) sets (rather than
										// "skipping collisions" as per A23E) because we do more of
										// the work less often. It's uglier (more code) but faster.
										// Compare DOG_1 with A3E's DOG_1 to see what I'm on about.
										//
										// NB: is*AnyAllowed* make it faster, because once all-the-
										// maybes of all-cells-to-my-right are already allowed any5
										// (for example) becomes "is ANY combo-to-my-right (c4, c5,
										// and c6) allowed". In the best case this is O(1), whereas
										// the "standard approach" of iterating all potential values
										// is allways worste case O(8*8*8=512), so is*AnyAllowed*
										// can be faster, but can't be slower; so is faster overall.
										//
										// The kicker in all this is that the bloody JIT compiler
										// stops work BEFORE is1AnyAllowed4567 is called, so it's
										// interpretted millions of times, so it actually executes
										// slower than the compiled slower version; hence the calls
										// to is1AnyAllowed1234567..is1AnyAllowed4567 are commented
										// out, because it's actually slower with them in. Bugger!
										//
										// I hate java's JIT compiler! Or rather, I hate the fact
										// that it stops work too early, and there's nothing I can
										// DO about it. I want a java.exe switch -Xjit:5000 to make
										// the JIT compiler stop work after 5 minutes.
										//
										// I'm going to try making is*AnyAllowed* static, in the
										// hope that might get them compiled. Sigh. Or it might
										// just be faster not to pass "this" 150 billion times.


										// nb: this switch for performance only, the default path
										// works for any numCmnExclBits, but he calls covers.
++counts[0];
++counts[numCmnExclBits];
										switch ( numCmnExclBits ) {
										case 1:
											// we don't need a common excluders loop
											//  w/o HitSet: 1 = 1,127,309,465 of 1,133,446,233 = 99.46%
											// with HitSet: 1 = 343 of 388 = 88.40%

											// filter top1465 even when !isHacky, to not take 34 hours again.
											// nb: the magic numbers are from a8e.log via tools.AnalyseLog
											if ( filterTop1465 ) {
//useless:										cnt1col.count(col);
//												// cnt1col min=9/9 max=62/62 pass 631,651,221
//												if(col<? || col>?) continue;
//												++cnt1col.pass;

//useless:										cnt1sib.count(sib);
//												// cnt1sib min=12/12 max=23/23 pass 631,651,221
//												if(sib<? || sib>?) continue;
//												++cnt1sib.pass;

//												cnt1cs.count(cs);
												// cnt1cs min=22/21 max=80/85 pass 631,651,221
												if(cs<22 || cs>80) continue;
//												++cnt1cs.pass;

//useless:										cnt1mbs.count(mbs);
//												// cnt1mbs min=17/17 max=34/34 pass 631,651,221
//												if(mbs<? || mbs>?) continue;
//												++cnt1mbs.pass;

												ces = ces0;
//												cnt1ces.count(ces);
												// cnt1ces min=2/2 max=6/8 pass 631,651,221
												if(ces>6) continue;
//												++cnt1ces.pass;

//												cnt1hit.count(hit);
												// cnt1hit min=2/2 max=27/32 pass 631,651,221
												if(hit>27) continue;
//												++cnt1hit.pass;

//useless:										cnt1sum.count(sum);
//												// cnt1sum min=11/11 max=83/83 pass 631,651,221
//												if(sum<? || sum>?) continue;
//												++cnt1sum.pass;

//useless:										cnt1prg.count(prang);
//												// cnt1prg min=1.653/1.653 max=11.668/11.667 pass 631,651,221
//												if(prang<? || prang>?) continue;
//												++cnt1prg.pass;
											}
											DOG_1a: for ( int sv0 : SVS[c0b] ) {
												c7b0 = ns70 ? c7b : c7b & ~sv0;
												c6b0 = ns60 ? c6b : c6b & ~sv0;
												c5b0 = ns50 ? c5b : c5b & ~sv0;
												c4b0 = ns40 ? c4b : c4b & ~sv0;
												c3b0 = ns30 ? c3b : c3b & ~sv0;
												c2b0 = ns20 ? c2b : c2b & ~sv0;
												any1 = false;
												SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
													sv01 = sv0 | sv1;
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
															c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
															c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
															c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
															any4 = false;
															SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																sv04 = sv03 | sv4;
																if ( (do50 && (ceb0 & ~sv04) == 0)
																  || (c7b4 = ns74 ? c7b3 : c7b3 & ~sv4) == 0
																  || (c6b4 = ns64 ? c6b3 : c6b3 & ~sv4) == 0
																  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																	continue;
																any5 = false;
																SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																	if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																	  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																	  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																		continue;
																	any6 = false;
																	SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																		if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																		  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																			continue;
																		any7 = false;
																		SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																			if ( (ceb0 & ~(sv06|sv7)) == 0 )
																				continue;
																			// Combo Allowed!
																			any7 = true;
																			// add to allowedValuesBitsets and
																			// check if that makes all allowed.
																			if ( (avb7|=sv7) != c7b )
																				continue;
																			// We allow only the FIRST combo at each level; and
																			// then move-on to evaluating the next value at this
																			// level, which has not yet been evaluated; instead
																			// of wasting time evaluating multiple combos-to-my-
																			// right which can not allow any additional maybes.
																			// This reduces O(n*n*n*n*n*n*n*n) to O(n*n*n'ish).
																			switch ( anyLevel ) {
																			case 8: anyLevel=7; break SV7_LOOP;
																			case 7: break SV7_LOOP;
																			case 6: any6=true; break SV6_LOOP;
																			case 5: any5=true; break SV5_LOOP;
																			case 4: any4=true; break SV4_LOOP;
																			case 3: any3=true; break SV3_LOOP;
																			case 2: any2=true; break SV2_LOOP;
																			case 1: any1=true; break SV1_LOOP;
																			} // end-switch
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
											// we still don't need a common excluders loop
											//  w/o HitSet: 2 = 5,936,814 of 1,133,446,233 = 0.52%
											// with HitSet: 2 = 44 of 388 = 11.34%

											// filter top1465 even when !isHacky, to not take 34 hours again.
											// nb: the magic numbers are from a8e.log via tools.AnalyseLog
											if ( filterTop1465 ) {
//												cnt2col.count(col);
												// cnt2col min=9/9 max=47/59 pass 3,816,088
												if(col>47) continue;
//												++cnt2col.pass;

//useless: 										cnt2sib.count(sib);
//												// cnt2sib min=12/12 max=22/22 pass 3,816,088
//												if(sib<? || sib>?) continue;
//												++cnt2sib.pass;

//												cnt2cs.count(cs);
												// cnt2cs min=21/21 max=65/81 pass 3,816,088
												if(cs>65) continue;
//												++cnt2cs.pass;

//												cnt2mbs.count(mbs);
												// cnt2mbs min=18/17 max=33/34 pass 3,816,088
//												if(mbs<18 || mbs>33) continue;
//												++cnt2mbs.pass;

//useless: 										ces = ces0;
//												cnt2ces.count(ces);
//												// cnt2ces min=2/2 max=6/6 pass 3,816,088
//												if(ces<? || ces>?) continue;
//												++cnt2ces.pass;

//												cnt2hit.count(hit);
												// cnt2hit min=8/4 max=32/32 pass 3,816,088
												if(hit<8) continue;
//												++cnt2hit.pass;

//												cnt2sum.count(sum);
												// cnt2sum min=19/13 max=79/83 pass 3,816,088
												if(sum<19 || sum>79) continue;
//												++cnt2sum.pass;

//												cnt2prg.count(prang);
												// cnt2prg min=1.714/1.653 max=4.319/7.933 pass 3,816,088
												if(prang<1.714 || prang>4.319) continue;
//												++cnt2prg.pass;
											}
											ces1 = VSIZE[ceb1=cmnExclBits[1]];
											do31 = ces1 <= 3;
											do41 = ces1 <= 4;
											do51 = ces1 <= 5;
											DOG_1b: for ( int sv0 : SVS[c0b] ) {
												c7b0 = ns70 ? c7b : c7b & ~sv0;
												c6b0 = ns60 ? c6b : c6b & ~sv0;
												c5b0 = ns50 ? c5b : c5b & ~sv0;
												c4b0 = ns40 ? c4b : c4b & ~sv0;
												c3b0 = ns30 ? c3b : c3b & ~sv0;
												c2b0 = ns20 ? c2b : c2b & ~sv0;
												any1 = false;
												SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
													sv01 = sv0 | sv1;
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
																c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																any5 = false;
																SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																	if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																	  || (ceb1 & ~sv05) == 0
																	  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																	  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																		continue;
																	any6 = false;
																	SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																		if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																		  || (ceb1 & ~sv06) == 0
																		  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																			continue;
																		any7 = false;
																		SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																			if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																			  || (ceb1 & ~sv07) == 0 )
																				continue;
																			// Combo Allowed!
																			any7 = true;
																			// add to allowedValuesBitsets and
																			// check if that makes all allowed.
																			if ( (avb7|=sv7) != c7b )
																				continue;
																			// We allow only the FIRST combo at each level
																			switch ( anyLevel ) {
																			case 8: anyLevel=7; break SV7_LOOP;
																			case 7: break SV7_LOOP;
																			case 6: any6=true; break SV6_LOOP;
																			case 5: any5=true; break SV5_LOOP;
																			case 4: any4=true; break SV4_LOOP;
																			case 3: any3=true; break SV3_LOOP;
																			case 2: any2=true; break SV2_LOOP;
																			case 1: any1=true; break SV1_LOOP;
																			} // end-switch
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
											// the common excluders loop is in the covers method
											//  w/o HitSet: 3 = 199,954 of 1,133,446,233 = 0.02%
											//  w/o HitSet: 4 = 0 of 1,133,446,233 = 0.00%
											// with HitSet: 3 = 1 of 388 = 0.26%
											// with HitSet: 4 = 0 of 388 = 0.00%

											// filter top1465 even when !isHacky, to not take 34 hours again.
											// nb: the magic numbers are from a8e.log via tools.AnalyseLog
											if ( filterTop1465 ) {
//												cnt3col.count(col);
												// cnt3col min=9/9 max=34/47 pass 35,508
												if(col>34) continue;
//												++cnt3col.pass;

//useless: 										cnt3sib.count(sib);
//												// cnt3sib min=12/12 max=16/16 pass 35,508
//												if(sib<? || sib>?) continue;
//												++cnt3sib.pass;

//												cnt3cs.count(cs);
												// cnt3cs min=21/21 max=49/63 pass 35,508
												if(cs>49) continue;
//												++cnt3cs.pass;

//												cnt3mbs.count(mbs);
												// cnt3mbs min=19/19 max=33/34 pass 35,508
												if(mbs>33) continue;
//												++cnt3mbs.pass;

//useless: 										ces = ces0;
//												cnt3ces.count(ces);
//												// cnt3ces min=2/2 max=4/4 pass 35,508
//												if(ces<? || ces>?) continue;
//												++cnt3ces.pass;

//												cnt3hit.count(hit);
												// cnt3hit min=10/7 max=32/32 pass 35,508
												if(hit<10) continue;
//												++cnt3hit.pass;

//												cnt3sum.count(sum);
												// cnt3sum min=20/16 max=65/79 pass 35,508
												if(sum<20 || sum>65) continue;
//												++cnt3sum.pass;

//												cnt3prg.count(prang);
												// cnt3prg min=1.968/1.786 max=3.950/4.825 pass 35,508
												if(prang<1.968 || prang>3.950) continue;
//												++cnt3prg.pass;
											}
											DOG_1c: for ( int sv0 : SVS[c0b] ) {
												c7b0 = ns70 ? c7b : c7b & ~sv0;
												c6b0 = ns60 ? c6b : c6b & ~sv0;
												c5b0 = ns50 ? c5b : c5b & ~sv0;
												c4b0 = ns40 ? c4b : c4b & ~sv0;
												c3b0 = ns30 ? c3b : c3b & ~sv0;
												c2b0 = ns20 ? c2b : c2b & ~sv0;
												any1 = false;
												SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
													sv01 = sv0 | sv1;
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
															c7b3 = ns73 ? c7b2 : c7b2 & ~sv3;
															c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
															c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
															any4 = false;
															SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
																sv04 = sv03 | sv4;
																if ( do50 && covers(cmnExclBits, numCmnExclBits, sv04)
																  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																	continue;
																c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																any5 = false;
																SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																	if ( covers(cmnExclBits, numCmnExclBits, sv05=sv04|sv5)
																	  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																	  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																		continue;
																	any6 = false;
																	SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																		if ( covers(cmnExclBits, numCmnExclBits, sv06=sv05|sv6)
																		  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																			continue;
																		any7 = false;
																		SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																			if ( covers(cmnExclBits, numCmnExclBits, sv07=sv06|sv7) )
																				continue;
																			// Combo Allowed!
																			any7 = true;
																			// add to allowedValuesBitsets and
																			// check if that makes all allowed.
																			if ( (avb7|=sv7) != c7b )
																				continue;
																			// We allow only the FIRST combo at each level
																			switch ( anyLevel ) {
																			case 8: anyLevel=7; break SV7_LOOP;
																			case 7: break SV7_LOOP;
																			case 6: any6=true; break SV6_LOOP;
																			case 5: any5=true; break SV5_LOOP;
																			case 4: any4=true; break SV4_LOOP;
																			case 3: any3=true; break SV3_LOOP;
																			case 2: any2=true; break SV2_LOOP;
																			case 1: any1=true; break SV1_LOOP;
																			} // end-switch
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
										  && avb7 == c7b ) {
											nonHinters.put();
											continue; // nothing to see here. move along!
										}

										// ===========================================
										// Performance is not an issue from here down.
										// To get here a value/s is excluded.
										// ===========================================

										// create the removable (red) potential values.
										Pots redPots = createRedPotentials(scells
												, avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7);
										if ( redPots.isEmpty() )
											continue; // Should never happen, but never say never.

++hintCounts[numCmnExclBits];
										if ( isTop1465 ) {
											hitCounters();
											hits.add(gsl, hintNum, cells);
										}

										// build the excluded combos map for the hint
										ExcludedCombosMap map = buildExcludedCombosMap(
												cmnExcls, numCmnExcls, cells, redPots);
										// create and add the hint
										AHint hint = new AlignedExclusionHint(
											  this, redPots, cells, numCmnExcls, cmnExcls, map);

//										standardLog(myLog, cells, gsl, hintNum
//												, cmnExcls, numCmnExcls
//												, cmnExclBits, numCmnExclBits
//												, redPots, map, hint);

										result = true; // in case add returns false
										if ( accu.add(hint) )
											return true;

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

	protected int LOCAL_populateCandidatesAndExcluders(Cell[] candidates
			, CellSet[] excluders, Grid grid) {
		final int degreePlus1 = this.degreePlus1;

		// cell's set of excluder cells. CellSet is a Set<Cell> with reasonably
		// fast contains, remove & clear; but a slow constructor
		// An excluder cell is a sibling with 2..5 maybes and numExcls>0
		CellSet set = null;
		int card,  n=0;

		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		CELL_LOOP: for ( Cell cell : grid.cells ) {
			if ( cell.maybes.size > 1 ) {
				// find cells excluders: ie siblings with maybes.size 2..5
				for ( Cell sib : cell.siblings ) { // 81*20=1620
					// sib is an excluder if it has maybes.size 2..degree
					// optimise: do the < first because it's more deterministic.
					if ( (card=sib.maybes.size)<degreePlus1 && card>1 ) {
						if ( set == null )
							set = new CellSet(); //slow constructor, O(1) contains
						set.add(sib);
					// optimize: skip any naked singles
					} else if ( card == 1 )
						continue CELL_LOOP; // skip siblings of naked singles
				}
				if ( set != null ) {
					// @task fBomb: Does A8E see cells with 8 or more maybes?
					// It must do, but none come through to the hints. WTF?
					// YES: Confirmed! I've seen this message I just didn't
					// remember what it meant.
					// Does this hold true for 9 and as well?
					// IBFIIK: I'll work that s__t out another day.
					if ( cell.maybes.size>=8 ) // kinky boots!
						System.out.format("fBomb: %s: %d %d %s\n"
							, classNameOnly
							, grid.source==null ? 0 : grid.source.lineNumber
							, AHint.hintNumber
							, cell.toFullString());
					candidates[n++] = cell;
					excluders[cell.i] = set;
					set = null; // ready? reset the set for the next set.
				}
			}
		} // next cell in grid
		return n;
	}

}
