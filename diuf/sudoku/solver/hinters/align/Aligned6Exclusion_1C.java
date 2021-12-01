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
 * The hacked version of Aligned 6 (Hex) Exclusion technique.
 * <p>Partially implements the aligned set exclusion Sudoku solving technique
 * for sets of 6 Cells.
 */
public final class Aligned6Exclusion_1C extends Aligned6ExclusionBase
		implements java.io.Closeable
//				 , diuf.sudoku.solver.hinters.IReporter
				 , diuf.sudoku.solver.hinters.IPreparer
{
	
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 27; // <HACK/>

	// the maximim number of candidates to permute (process).
	private static final int MAX_CANDIDATES = 61; // <HACK/>

	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[6];
	private static final Cell[] CELLS_ARRAY_1 = new Cell[6];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5

	private final ACollisionComparator cc = new ACollisionComparator();

// 16*1024:   248,675,971,200	   4133	    60,168,393	    138	 1,801,999,791	Aligned Hex
// 1465	  605,959,271,800	(10:05)	      413,624,076
//  8*1024:   215,151,807,900	   4133	    52,057,054	    138	 1,559,071,071	Aligned Hex
// 1465	  478,329,628,900	(07:58)	      326,504,866
//  4*1024:   252,879,124,100	   4133	    61,185,367	    138	 1,832,457,421	Aligned Hex
// 1465	  516,644,847,700	(08:36)	      352,658,599
	private final NonHinters nonHinters = new NonHinters(8*1024, 4);

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
//
//	protected final Counter cnt3col = new Counter("cnt3col");
//	protected final Counter cnt3sib = new Counter("cnt3sib");
//	protected final Counter cnt3cs  = new Counter("cnt3cs");
//	protected final Counter cnt3mbs = new Counter("cnt3mbs");
//	protected final Counter cnt3ces = new Counter("cnt3ces");
//	protected final Counter cnt3hit = new Counter("cnt3hit");
//	protected final Counter cnt3sum = new Counter("cnt3sum");
//	protected final Counter cnt3prg = new Counter("cnt3prg");

	//useless: cnt4*: A6E never hints from 4-or-more cmnExclBits

//	private java.io.PrintStream myLog = open("a6e.log", standardHeader());

	public Aligned6Exclusion_1C() {
		super(IO.A6E_1C_HITS);
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
	@SuppressWarnings("fallthrough")
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

		// localise fields for speed (if it's referenced more than twice)
		final int degree = this.degree;
		final int hintNum = grid.hintNumber;
		final boolean useHits = this.useHits;

		// get an array of the Cells at which we hinted last time;
		// otherwise we skip this call to getHints
		final Cell[] hitCells = useHits // only true when AHinter.hackTop1465
				? hits.cells(gsl, hintNum, degree, grid)
				: null;
		final boolean hitMe = hitCells != null;
		if ( useHits && !hitMe )
			return false; // no hints for this puzzle/hintNumber

		// shiftedValueses: an array of jagged-arrays of the shifted-values
		// that are packed into your maybes 0..511. See Values for more.
		final int[][] SVS = Values.VSHIFTED;

		// The populateCandidatesAndExcluders fields: a candidate has
		// maybesSize>=2 and has an excluder with maybesSize 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		// the number of candidates actually in the candidates array
		final int n;
		// an array of each cells set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// an array of the 6 cells in an aligned set, ready for exclusion.
		final Cell[] cells = CELLS_ARRAY;
		// sortedCells the above cells array sorted by the CollisionComparator
		final Cell[] scells = CELLS_ARRAY_1;

		// the number of cells in each of the 6 for-i-loops
		final int n0, n1, n2, n3, n4;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = COMMON_EXCLUDERS_ARRAY;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = COMMON_EXCLUDERS_BITS;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// indexes in the candidates array of the 6 cells in the aligned set.
		int i0, i1, i2, i3, i4, i5;

		// our 6 cells (the aligned set) from above sortedCells (scells) array
		Cell c0, c1, c2, c3, c4, c5;

		// indices of c0's excluder cells in Grid.cells
		Idx idx0;

		// bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of each subsequent sibling cell.
		// This is more code than "collision skipping" (as per A234E) but it is
		// faster, because it does more of the work less often.
		// c5b0: c5 bits version zero = maybes of c5 minus v0 (the current
		// value of c0), presuming that c0 and c5 are siblings (ie c0 and c5 are
		// in the same row, col, and/or box).
		// c5b1: c5 bits version one = c5.maybes & ~sv0 & ~sv1; where "& ~"
		// is how we remove a value from a bitset; and presuming that c5 is a
		// sibling of both c0 and c1, got it?
		int	c0b , c1b , c2b , c3b , c4b , c5b;
		int	            c2b0, c3b0, c4b0, c5b0;
		int		              c3b1, c4b1, c5b1;
		int				      c3b2, c4b2, c5b2;
		int					        c4b3, c5b3;
		int							      c5b4;

		// notSibling cache: ns54 = c5.isNotSiblingOf[c4.i]
		boolean ns54, ns53, ns52, ns51, ns50;
		boolean       ns43, ns42, ns41, ns40;
		boolean             ns32, ns31, ns30;
		boolean                   ns21, ns20;
		boolean                         ns10;

		// commonExcluderBits[0], size[ceb0]
		// sed '1,$:s/____/____/g' but stay away from fes, for ____s sake.
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// should we test of combo of 3,4,5 values covers ceb0/1/2
		boolean do30,do31,do32, do40,do41,do42, do50,do51,do52;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01, sv02, sv03, sv04, sv05;

		// allowedValuesBitsets: an array of bitsets of values allowed in scells
		int avb0, avb1, avb2, avb3, avb4, avb5;

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1, any2, any3, any4, any5;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // the number of cells in the aligned set.

		// filter variables
		int fives, sixes;
		int mbs=0, col=0, sib=0, cs=0, sum=0, hit=0, ces=0;
		double prang=0.0D;

		// presume failure, ie presume we're not going to find a hint.
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//           New McVitties Nut Cheese: For bugger tutties
		//                  (bitteries sould superitoly)
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also the set of sibling excluder-cells for each candidate.
		n = populateCandidatesAndExcluders(candidates, excluders, grid);
		if ( n < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( n < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( n > MAX_CANDIDATES )
				return false;
		}

		n4 = n - 1;
		n3 = n - 3;
		n2 = n - 3;
		n1 = n - 4;
		n0 = n - 5;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				if ( excluders[(cells[1]=candidates[i1]).i].idx1(idx0, idx01) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx1(idx01, idx02) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[candidates[i3].i].idx1(idx02, idx03) )
							continue;
						cells[3] = candidates[i3];
						if(hitMe && cells[3]!=hitCells[3]) continue;
						for ( i4=i3+1; i4<n4; ++i4 ) {
							if ( excluders[candidates[i4].i].idx1(idx03, idx04) )
								continue;
							cells[4] = candidates[i4];
							if(hitMe && cells[4]!=hitCells[4]) continue;
							interrupt();
							for ( i5=i4+1; i5<n; ++i5 ) {
								if ( excluders[candidates[i5].i].idx1(idx04, idx05) )
									continue;
								cells[5] = candidates[i5];
								if(hitMe && cells[5]!=hitCells[5]) continue;

								if ( nonHinters.skip(cells) )
									continue;

								// filter top1465 when !isHacky rather than wait 34 hours again.
								// nb: the filter numbers are from a10e.log via tools.AnalyseLog
								if ( filterTop1465 ) {
									// filter by total number of maybes shared by siblings
									col = countCollisions(cells); // countCollisions
//									colCnt.count(col);
									if(col<4 || col>37) continue;
//									++colCnt.pass;

									// filter by total cells.maybesSize
									mbs = totalMaybesSize; // from countCollisions
//									mbsCnt.count(mbs);
									if(mbs<12 || mbs>25) continue;
//									++mbsCnt.pass;

									// filter by total number of sibling relationships (edges)
									sib = siblingsCount; // from countCollisions
//									sibCnt.count(sib);
									if(sib<6 || sib>12) continue;
//									++sibCnt.pass;

									// this one is filtered in the switch
									cs = col + sib;

									// filter by maximum number of "large" cells
//									++maxMbs.cnt;
									fives = sixes = 0;
									for ( Cell cell : cells )
										switch ( cell.size ) {
										case 6: ++sixes; //fallthrough
										case 5: ++fives; //fallout
										}
									if(sixes>2) continue;
									if(fives>3) continue;
//									++maxMbs.pass;
								}

								// read common excluder cells from grid at idx05
								if ( (numCmnExcls = idx05.cellsN(grid, cmnExcls)) == 1 ) {
									cmnExclBits[0] = cmnExcls[0].maybes;
									numCmnExclBits = 1;
								} else {
									// performance: look at the smallest maybes first.
									//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
									bubbleSort(cmnExcls, numCmnExcls);
									// get common excluders bits, removing any supersets,
									// eg: {12,125}=>{12} coz 125 covers 12, so 125 does nada.
									// nb: "common excluders bits" is just the maybes of
									// the common excluder cells. It's faster to put them in
									// there own array than it is to repeatedly dereference.
									numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);
								}
								// remove each common excluder bits which contains a value
								// that is not in any candidates maybes, because no combo
								// can cover it, so it won't contribute to an exclusion.
								// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip because
								// there are no cells in the aligned set which maybe 3, and
								// both common excluder cells maybe 3, so there can't be an
								// exclusion here; so we skip this aligned set now rather
								// than waste time pointlessly examining it the dog____er.
								if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
										, allMaybesBits(cells))) == 0 )
									continue;

								// filter top1465 when !isHacky rather than wait 34 hours again.
								if ( filterTop1465 ) {
									hit = countHits(cmnExclBits, numCmnExclBits, cells);
//									hitCnt.count(hit); // relies on sum
									if(hit<2 || hit>29) continue;
//									++hitCnt.pass;

									sum = col + hit;
//									sumCnt.count(sum);
									if(sum<9 || sum>57) continue;
//									++sumCnt.pass;

									prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//									prangCnt.count(prang);
									if(prang<1.543D || prang>8.751D) continue;
//									++prangCnt.pass;
								}

								// the dog____ing algorithm is faster with dodgem-cars to the left,
								// but the above for-i-loops need a static cells array; so we copy
								// cells to scells (sortedCells) and sort that array DESCENDING by:
								// 4*maybesCollisions + 2*cmnExclHits + maybesSize
								cc.set(cells, cmnExclBits, numCmnExclBits);
								System.arraycopy(cells, 0, scells, 0, degree);
								//MyTimSort.small(scells, degree, cc);
								bubbleSort(scells, degree, cc);

								// get each sortedCell and it's potential values
								c0b = (c0=scells[0]).maybes;
								c1b = (c1=scells[1]).maybes;
								c2b = (c2=scells[2]).maybes;
								c3b = (c3=scells[3]).maybes;
								c4b = (c4=scells[4]).maybes;
								c5b = (c5=scells[5]).maybes;

								// build the notSiblings cache
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

								// clear allowedValuesBitsets array
								avb0=avb1=avb2=avb3=avb4=avb5 = 0;

								// anyLevel controls which for-i-loop to break-out-of when
								// all maybes of all cells to my right are already allowed;
								// so that the FIRST allowed combo sets my any* and we skip
								// all other "like" combos.
								// This is for performance only. Algorithm works without it.
								// We just don't waste time allowing multiple combos whose
								// right-hand-side-values are all already allowed.
								anyLevel = degree; // meaning that avb5 != c5b

								// should we test if combo of 3/4/5 values covers ceb0
								ces0=VSIZE[ceb0=cmnExclBits[0]];
								do30 = ces0 <= 3;
								do40 = ces0 <= 4;
								do50 = ces0 <= 4;

								// The dog____ing loop calculates: is each combo allowed?
								// ie: Is each possible combination of the potential values
								// of these $degree (6) cells allowed? (else we continue
								// to skip this combo). We only populate the (avb*)
								// allowedValuesBitsets virtual array in the big loop.
								// After the loop we check if all of each cells potential
								// values have been allowed, and if so we continue: ie
								// jump to the next aligned set (nothing to see here,
								// move along!), but if not then there's an exclusion,
								// ie all possible combinations of potential values do not
								// include certain value/s in certain position/s, so we
								// build the excludedCombosMap and the hint.
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
									//  w/o HitSet: 1 = 1,031,511,274 of 1,059,060,093 = 97.40%
									// with HitSet: 1 = 696 of 942 = 73.89%

									// filter top1465 even when !isHacky, to not take 34 hours again.
									// nb: the magic numbers are from a8e.log via tools.AnalyseLog
									if ( filterTop1465 ) {
//										cnt1col.count(col);
//										if(col<? || col>?) continue;
//										++cnt1col.pass;

//										cnt1sib.count(sib);
//										if(sib<? || sib>?) continue;
//										++cnt1sib.pass;

//										cnt1cs.count(cs);
//										if(cs<? || cs>?) continue;
//										++cnt1cs.pass;

//useless:								cnt1mbs.count(mbs);
//										if(mbs<? || mbs>?) continue;
//										++cnt1mbs.pass;

//										ces = ces0;
//										cnt1ces.count(ces);
//										if(ces>?) continue;
//										++cnt1ces.pass;

//										cnt1hit.count(hit);
//										if(hit<? || hit>?) continue;
//										++cnt1hit.pass;

//										cnt1sum.count(sum);
//										if(sum<? || sum>?) continue;
//										++cnt1sum.pass;

//										cnt1prg.count(prang);
//										if(prang<? || prang>?) continue;
//										++cnt1prg.pass;
									}
									DOG_1a: for ( int sv0 : SVS[c0b] ) {
										c5b0 = ns50 ? c5b : c5b & ~sv0;
										c4b0 = ns40 ? c4b : c4b & ~sv0;
										c3b0 = ns30 ? c3b : c3b & ~sv0;
										c2b0 = ns20 ? c2b : c2b & ~sv0;
										any1 = false;
										SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) { // c1b0
											sv01 = sv0 | sv1;
											c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
											c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
											c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
											any2 = false;
											SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) { // c2b1
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
															continue;;
														any5 = false;
														SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
															if ( (ceb0 & ~(sv04|sv5)) == 0 )
																continue;
															// Combo Allowed!
															any5 = true;
															// add v5 to allowedValuesBitsets
															// and skip if all of c5's values are NOT allowed.
															if ( (avb5|=sv5) != c5b )
																continue;
															// We allow only the FIRST combo at each level
															// and then move-on to evaluating the next value
															// at this level, which has not yet been evaluated;
															// ie not wasting time evaluating multiple combos-
															// to-my-right which can not allow any additional
															// maybes. This is much faster, reducing:
															// O(n*n*n*n*n*n) to O(n*n'ish).
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
									//  w/o HitSet: 2 = 26,140,797 of 1,059,060,093 = 2.47%
									// with HitSet: 2 = 241 of 942 = 25.58%
									ces1 = VSIZE[ceb1=cmnExclBits[1]];
									do31 = ces1 <= 3;
									do41 = ces1 <= 4;
									do51 = ces1 <= 5;

									// filter top1465 even when !isHacky, to not take 34 hours again.
									// nb: the magic numbers are from a8e.log via tools.AnalyseLog
									if ( filterTop1465 ) {
//										cnt2col.count(col);
//										if(col<? || col>?) continue;
//										++cnt2col.pass;

//										cnt2sib.count(sib);
//										if(sib<? || sib>?) continue;
//										++cnt2sib.pass;

//										cnt2cs.count(cs);
//										if(cs<? || cs>?) continue;
//										++cnt2cs.pass;

//useless:								cnt2mbs.count(mbs);
//										if(mbs<? || mbs>?) continue;
//										++cnt2mbs.pass;

										ces = ces0;
//										cnt2ces.count(ces);
//										if(ces<? || ces>?) continue;
//										++cnt2ces.pass;

//										cnt2hit.count(hit);
//										if(hit<? || hit>?) continue;
//										++cnt2hit.pass;

//										cnt2sum.count(sum);
//										if(sum<? || sum>?) continue;
//										++cnt2sum.pass;

//										cnt2prg.count(prang);
//										if(prang<? || prang>?) continue;
//										++cnt2prg.pass;
									}
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
												SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
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
									//  w/o HitSet: 3 = 1,405,487 of 1,059,060,093 = 0.13%
									// with HitSet: 3 = 5 of 942 = 0.53%
									ces1 = VSIZE[ceb1=cmnExclBits[1]];
									do31 = ces1 <= 3;
									do41 = ces1 <= 4;
									do51 = ces1 <= 5;
									ces2 = VSIZE[ceb2=cmnExclBits[2]];
									do32 = ces2 <= 3;
									do42 = ces2 <= 4;
									do52 = ces2 <= 5;

									// filter top1465 even when !isHacky, to not take 34 hours again.
									// nb: the magic numbers are from a8e.log via tools.AnalyseLog
									if ( filterTop1465 ) {
//										cnt3col.count(col);
//										if(col<? || col>?) continue;
//										++cnt3col.pass;

//										cnt3sib.count(sib);
//										if(sib<? || sib>?) continue;
//										++cnt3sib.pass;

//										cnt3cs.count(cs);
//										if(cs<? || cs>?) continue;
//										++cnt3cs.pass;

//useless:								cnt3mbs.count(mbs);
//										if(mbs<? || mbs>?) continue;
//										++cnt3mbs.pass;

//										ces = ces0;
//										cnt3ces.count(ces);
//										if(ces<? || ces>?) continue;
//										++cnt3ces.pass;

//										cnt3hit.count(hit);
//										if(hit<? || hit>?) continue;
//										++cnt3hit.pass;

//										cnt3sum.count(sum);
//										if(sum<? || sum>?) continue;
//										++cnt3sum.pass;

//										cnt3prg.count(prang);
//										if(prang<? || prang>?) continue;
//										++cnt3prg.pass;
									}
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
									// the common excluders loop is in the covers method
									//  w/o HitSet: 4 = 1,982 of 1,059,060,093 = 0.00%
									//  w/o HitSet: 5 =   428 of 1,059,060,093 = 0.00%
									//  w/o HitSet: 6 =   125 of 1,059,060,093 = 0.00%
									// with HitSet: 4 = 0 of 942 = 0.00%
									// with HitSet: 5 = 0 of 942 = 0.00%
									// with HitSet: 6 = 0 of 942 = 0.00%
									if ( isTop1465 ) // you don't get here when hitMe==true
										continue; // top1465 never hints here
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
												if ( do30 && covers(cmnExclBits, numCmnExclBits, sv02) )
													continue;
												if ( (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
													continue;
												c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
												c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
												any3 = false;
												SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
													sv03 = sv02 | sv3;
													if ( do40 && covers(cmnExclBits, numCmnExclBits, sv03) )
														continue;
													if ( (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
														continue;
													c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
													any4 = false;
													SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
														sv04 = sv03 | sv4;
														if ( do50 && covers(cmnExclBits, numCmnExclBits, sv04) )
															continue;
														if ( (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
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
								if ( isTop1465 ) {
									hitCounters();
									hits.add(gsl, hintNum, cells);
								}

								// create the removable (red) potential values.
								// nb: sortedCells is simultanious with avbs.
								Pots redPots = createRedPotentials(scells
										, avb0,avb1,avb2,avb3,avb4,avb5);
								if ( redPots.isEmpty() )
									continue; // Should never happen, but never say never.
								// create the excluded combos map for the hint
								ExcludedCombosMap map = buildExcludedCombosMap(
										cmnExcls, numCmnExcls, cells, redPots);
								// create and add the hint
								AHint hint = new AlignedExclusionHint(this
										, redPots, cells, numCmnExcls, cmnExcls, map);

//								standardLog(myLog, cells, gsl, hintNum
//										, cmnExcls, numCmnExcls
//										, cmnExclBits, numCmnExclBits
//										, redPots, map, hint);

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
