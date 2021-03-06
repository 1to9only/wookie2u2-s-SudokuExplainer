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
 * Aligned 9 (Nona) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 * of 9 Cells.
 */
public final class Aligned9Exclusion_1C extends Aligned9ExclusionBase
		implements diuf.sudoku.solver.IReporter
				 , java.io.Closeable
				 , diuf.sudoku.solver.IPreparer
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 43; // <HACK/>

	// the maximim number of candidates to permute (process).
	// count(9,61) = 17,341,763,504
	private static final int MAX_CANDIDATES = 61; // <HACK/>

	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[9];
	private static final Cell[] CELLS_ARRAY_1 = new Cell[9];

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

	private final ACollisionComparator cc = new ACollisionComparator();

	private final NonHinters64 nonHinters = new NonHinters64(128*1024, 4);

//	protected final Counter cnt1col = new Counter("cnt1col");
//	protected final Counter cnt1sib = new Counter("cnt1sib");
//	protected final Counter cnt1cs  = new Counter("cnt1cs");
//	protected final Counter cnt1mbs = new Counter("cnt1mbs");
//	//useless: protected final Counter cnt1ces = new Counter("cnt1ces");
//	protected final Counter cnt1hit = new Counter("cnt1hit");
//	protected final Counter cnt1sum = new Counter("cnt1sum");
//	protected final Counter cnt1prg = new Counter("cnt1prg");
//
//	protected final Counter cnt2col = new Counter("cnt2col");
//	//useless: protected final Counter cnt2sib = new Counter("cnt2sib");
//	protected final Counter cnt2cs  = new Counter("cnt2cs");
//	protected final Counter cnt2mbs = new Counter("cnt2mbs");
//	protected final Counter cnt2ces = new Counter("cnt2ces");
//	protected final Counter cnt2hit = new Counter("cnt2hit");
//	protected final Counter cnt2sum = new Counter("cnt2sum");
//	protected final Counter cnt2prg = new Counter("cnt2prg");
//
//	protected final Counter cnt3col = new Counter("cnt3col");
//	//useless: protected final Counter cnt3sib = new Counter("cnt3sib");
//	protected final Counter cnt3cs  = new Counter("cnt3cs");
//	protected final Counter cnt3mbs = new Counter("cnt3mbs");
//	protected final Counter cnt3ces = new Counter("cnt3ces");
//	protected final Counter cnt3hit = new Counter("cnt3hit");
//	protected final Counter cnt3sum = new Counter("cnt3sum");
//	protected final Counter cnt3prg = new Counter("cnt3prg");

	//useless: cnt4*: A9E never hints from 4-or-more cmnExclBits

//	private final java.io.PrintStream myLog = open("a9e.log", standardHeader());

	public Aligned9Exclusion_1C(IInterruptMonitor monitor) {
		super(monitor);
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

		// The populate populateCandidatesAndExcluders fields: a candidate Cell
		// has maybes.size>=2 and >1 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = CANDIDATES_ARRAY;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// the number of cells in each of the for-i-loops
		final int n0, n1, n2, n3, n4, n5, n6, n7;

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

		// array of 3*27-bits-bitsets of indexes of c0's excluders in Grid.cells
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

		// ces0=size[ceb0=commonExcluderBits[0]]
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// if do30 check if ceb0 (2-or-3 values) covers the-combo-so-far, to
		// which we've just added the 3rd value (sv2).
		// do31 is the same but for ceb1 (ie cmnExclBits[1]).
		// nb: do6+* no exist coz presumed always true. It certainly is for
		//     case 1: which is 99.67% of aligned sets so no care for 2 or 3.
		boolean do30,do31,do32, do40,do41,do42, do50,do51,do52;

		// filter variables
//		int fives, sixes, sevns, eigts, nines;
		int mbs=0, col=0, sib=0, cs=0, sum=0, hit=0, ces=0;
		double prang=0.0D;

		// presume that no hints will be found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//             Those occelots are eating my aartichokes!
		//					        Sal pepar?
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// search for candidate cells that can contribute to an exclusion set.
		// NB: 7 is means find common excluder cells with maybes.size 2..7 coz
		// 6 is max-size of a common excluder cell that contributes to a hint,
		// not 10 (degreePlus1) as per normal. That occelot ate my chitlins!
		numCandidates = populateCandidatesAndExcluders(candidates, excluders, grid, 7);
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
										if ( isInterrupted() )
											return false;
										for ( i8=i7+1; i8<numCandidates; ++i8 ) {
											if ( excluders[candidates[i8].i].idx1(idx08, idx07) )
												continue;
											cells[8] = candidates[i8];
											if(hitMe && cells[8]!=hitCells[8]) continue;

											if ( nonHinters.skip(cells) )
												continue;

											// filter top1465 even when !isHacky, to not take 34 hours again.
											// nb: the magic numbers are from a9e.log via tools.AnalyseLog
											if ( filterTop1465 ) {
												// filter by total number of maybes shared by siblings
												col = countCollisions(cells); // countCollisions
//												colCnt.count(col);
//												if(col<? || col>?) continue;
//												++colCnt.pass;

												// filter by total cells.maybes.size
												mbs = totalMaybesSize; // from countCollisions
//												mbsCnt.count(mbs);
//												if(mbs<? || mbs>?) continue;
//												++mbsCnt.pass;

												// filter by total number of sibling relationships (edges)
												sib = siblingsCount; // from countCollisions
//												sibCnt.count(sib);
//												if(sib<? || sib>?) continue;
//												++sibCnt.pass;

												// this one is filtered in the switch
												cs = col + sib;

//												// filter by maximum number of "large" cells
//												++maxMbs.cnt;
//												fives = sixes = sevns = eigts = nines = 0;
//												for ( Cell cell : cells )
//													switch ( cell.maybes.size ) {
//													case 9: ++nines; //fallthrough // 9 is the maximum possible
//													case 8: ++eigts; //fallthrough
//													case 7: ++sevns; //fallthrough
//													case 6: ++sixes; //fallthrough
//													case 5: ++fives; //fallout
//													}
//												// nb: at 0.08% this probably costs more than it saves!
//												if(fives>6) continue;
//												if(sixes>2) continue;
//												if(sevns>1) continue;
//												if(eigts>0) continue;
//												if(nines>0) continue;
//												++maxMbs.pass;
											}

											// read common excluder cells from grid at idx08
											if ( (numCmnExcls = idx08.cellsN(grid, cmnExcls)) == 1 ) {
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
											// =============================================================================
											// remove each common excluder bits which contains a value that is not in any of
											// the candidates maybes, because no combo of candidates maybes can cover it, so
											// it can't contribute to an exclusion.
											// @maybe we should check if each cmnExclBits IS actually covered by any combo,
											// but I suspect that'll take about as long as (is a similar order of complexity
											// to) actually DOING it; and never mind the bollocks. Try it and find out?
											// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
											// skip if there are none remaining
											if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
													, allMaybesBits(cells))) == 0 )
												continue;

											// filter top1465 even when !isHacky, to not take 34 hours again.
											// nb: the magic numbers are from a9e.log via tools.AnalyseLog
											if ( filterTop1465 ) {
												hit = countHits(cmnExclBits, numCmnExclBits, cells);
//												hitCnt.count(hit); // relies on sum
//												if(hit<? || hit>?) continue;
//												++hitCnt.pass;

												sum = col + hit;
//												sumCnt.count(sum);
//												if(sum<? || sum>?) continue;
//												++sumCnt.pass;

												prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//												prangCnt.count(prang);
//												if(prang<? || prang>?) continue;
//												++prangCnt.pass;
											}

											// the dog____ing algorithm is faster with dodgem-cars to the left, but the
											// above for-i-loops need a static cells array; so we copy cells to scells
											// (sortedCells) and sort that array DESCENDING by:
											//   4*maybesCollisions + 2*commonExcluderHits + maybes.size
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

											// should we test if 3/4/5 values cover ceb0
											ces0 = VSIZE[ceb0=cmnExclBits[0]];
											do30 = ces0 <= 3;
											do40 = ces0 <= 4;
											do50 = ces0 <= 5;

											// The anyLevel controls which for-i-loop to break-out-of when all maybes of all
											// cells to my right are already allowed; so that the FIRST allowed combo sets
											// my any* and we skip all other "like" combos. This is for performance only.
											// The algorithm works without it. We just don't waste time allowing multiple
											// combos whose right-hand-side-values are all already allowed.
											anyLevel = degree; // meaning that avb8 != c8b

											// The dog____ing loop calculates: is each combo allowed? ie: Is each possible
											// combination of the potential values of these $degree (9) cells allowed? Else
											// the combo has been disallowed, so it's skipped. We only populate the
											// allowedValuesBitsets virtual array here, we'll build the excludedCombosMap
											// later, if any value/s are excluded; ie all possible combinations of potential
											// values of these $degree (9) cells do not include certain value/s in certain
											// position/s.
											// After the dog____ing loop we check if any of each cells maybes have not been
											// allowed, and if so then it's an exclusion, ie we finally found a bloody hint!
											// There are 2 exclusion rules:
											// (1) Hidden Single: Using the same value for two cells is allowed only if the
											//     cells do not share a region.
											// (2) common excluder rule: If the combo covers (is a superset of) any
											//     common excluder cells maybes then the combo is not allowed, because the
											//     excluder cell must be one of those values (no matter which).
											//
											// nb: it's faster to "pre-eliminate" ie remove maybes from sibling cells for
											// large (4+) sets than "skip collisions" (as in A23E); because we do more of
											// the work less often. Compare DOG_1 with A3E's DOG_1 to see what I'm on about.
											//
											// nb: this switch for performance only, the default path works for any
											// numCmnExclBits. Just faster with no calls.
++counts[0];
++counts[numCmnExclBits];
											switch ( numCmnExclBits ) {
											case 1:
												// we don't need a common excluders loop
												// with HitSet: 1 = 199 of 215 = 92.56%

												// filter top1465 even when !isHacky, to not take 34 hours again.
												// nb: the magic numbers are from a9e.log via tools.AnalyseLog
												if ( filterTop1465 ) {
//													cnt1col.count(col);
													// cnt1col min=11/0 max=84/124 pass 875,193,521
													if(col<11 || col>84) continue;
//													++cnt1col.pass;

//													cnt1sib.count(sib);
													// cnt1sib min=12/9 max=26/30 pass 875,193,521
													if(sib<12 || sib>26) continue;
//													++cnt1sib.pass;

//													cnt1cs.count(cs);
													// cnt1cs min=26/9 max=110/154 pass 875,193,521
													if(cs<26 || cs>110) continue;
//													++cnt1cs.pass;

//													cnt1mbs.count(mbs);
													// cnt1mbs min=21/18 max=40/54 pass 875,193,521
													if(mbs<21 || mbs>40) continue;
//													++cnt1mbs.pass;

//useless: 											ces = ces0;
//													cnt1ces.count(ces);
//													// cnt1ces min=2/2 max=6/6 pass 875,193,521
//													if(ces>?) continue;
//													++cnt1ces.pass;

//													cnt1hit.count(hit);
													// cnt1hit min=2/2 max=28/44 pass 875,193,521
													if(hit>28) continue;
//													++cnt1hit.pass;

//													cnt1sum.count(sum);
													// cnt1sum min=16/3 max=106/147 pass 875,193,521
													if(sum<16 || sum>106) continue;
//													++cnt1sum.pass;

//													cnt1prg.count(prang);
													// cnt1prg min=1.772/1.531 max=16.470/Infinity pass 875,193,521
													if(prang<1.772 || prang>16.470) continue;
//													++cnt1prg.pass;
												}
												DOG_1a: for ( int sv0 : SVS[c0b] ) {
													c8b0 = ns80 ? c8b : c8b & ~sv0;
													c7b0 = ns70 ? c7b : c7b & ~sv0;
													c6b0 = ns60 ? c6b : c6b & ~sv0;
													c5b0 = ns50 ? c5b : c5b & ~sv0;
													c4b0 = ns40 ? c4b : c4b & ~sv0;
													c3b0 = ns30 ? c3b : c3b & ~sv0;
													c2b0 = ns20 ? c2b : c2b & ~sv0;
													any1 = false;
													SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) { // ie c1b0
														// nb: faster without any covers/empty tests
														sv01 = sv0 | sv1;
														c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
														c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
														c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
														c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
														c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
														c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
														any2 = false;
														SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) { // ie c2b1
															sv02 = sv01 | sv2;
															if ( (do30 && (ceb0 & ~sv02) == 0)
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
																		  || (c8b5 = ns85 ? c8b4 : c8b4 & ~sv5) == 0
																		  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																			  || (c8b6 = ns86 ? c8b5 : c8b5 & ~sv6) == 0
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
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
																					// We allow only the FIRST combo at each level
																					// and then move-on to evaluating the next value
																					// at this level, which has not yet been evaluated;
																					// instead of wasting time evaluating multiple
																					// combos-to-my-right which can not allow any
																					// additional maybes. This is faster, reducing:
																					// O(n*n*n*n*n*n*n*n*n) to O(n*n*n'ish).
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
											case 2: {
												// we still don't need a common excluders loop
												// with HitSet: 2 = 15 of 215 = 6.98%
												// do31 means check if ceb1 covers 3 values, do41 is 4 values.
												ces1 = VSIZE[ceb1=cmnExclBits[1]];
												do31 = ces1 <= 3;
												do41 = ces1 <= 4;
												do51 = ces1 <= 5;

												// filter top1465 even when !isHacky, to not take 34 hours again.
												// nb: the magic numbers are from a8e.log via tools.AnalyseLog
												if ( filterTop1465 ) {
//													cnt2col.count(col);
													// cnt2col min=9/3 max=67/98 pass 1,747,686
													if(col<9 || col>67) continue;
//													++cnt2col.pass;

//useless: 											cnt2sib.count(sib);
//													// cnt2sib min=16/16 max=24/24 pass 1,747,686
//													if(sib<? || sib>?) continue;
//													++cnt2sib.pass;

//													cnt2cs.count(cs);
													// cnt2cs min=26/19 max=88/122 pass 1,747,686
													if(cs<26 || cs>88) continue;
//													++cnt2cs.pass;

//useless: 											cnt2mbs.count(mbs);
													// cnt2mbs min=20/18 max=43/52 pass 1,747,686
													if(mbs<20 || mbs>43) continue;
//													++cnt2mbs.pass;

													ces = ces0;
//													cnt2ces.count(ces);
													// cnt2ces min=2/2 max=5/6 pass 1,747,686
													if(ces>5) continue;
//													++cnt2ces.pass;

//													cnt2hit.count(hit);
													// cnt2hit min=9/4 max=56/78 pass 1,747,686
													if(hit<9 || hit>56) continue;
//													++cnt2hit.pass;

//													cnt2sum.count(sum);
													// cnt2sum min=19/11 max=113/161 pass 1,747,686
													if(sum<19 || sum>113) continue;
//													++cnt2sum.pass;

//													cnt2prg.count(prang);
													// cnt2prg min=1.296/1.136 max=4.445/8.129 pass 1,747,686
													if(prang<1.296 || prang>4.445) continue;
//													++cnt2prg.pass;
												}
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
															if ( (do30 && (ceb0 & ~sv02) == 0)
															  || (do31 && (ceb1 & ~sv02) == 0)
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
																					// check if that makes anyAllowed
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
											case 3: {
												// we still don't need a common excluders loop
												// with HitSet: 3 = 1 of 215 = 0.47%
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
//													cnt3col.count(col);
													// cnt3col min=4/3 max=52/71 pass 42,464
													if(col<4 || col>52) continue;
//													++cnt3col.pass;

//useless: 											cnt3sib.count(sib);
//													// cnt3sib min=16/16 max=18/18 pass 42,464
//													if(sib<? || sib>?) continue;
//													++cnt3sib.pass;

//													cnt3cs.count(cs);
													// cnt3cs min=20/19 max=70/89 pass 42,464
													if(cs<20 || cs>70) continue;
//													++cnt3cs.pass;

//													cnt3mbs.count(mbs);
													// cnt3mbs min=19/18 max=42/49 pass 42,464
													if(mbs<19 || mbs>42) continue;
//													++cnt3mbs.pass;

													ces = ces0;
//													cnt3ces.count(ces);
													// cnt3ces min=2/2 max=5/6 pass 42,464
													if(ces>5) continue;
//													++cnt3ces.pass;

//													cnt3hit.count(hit);
													// cnt3hit min=10/9 max=82/105 pass 42,464
													if(hit<10 || hit>82) continue;
//													++cnt3hit.pass;

//													cnt3sum.count(sum);
													// cnt3sum min=19/15 max=129/168 pass 42,464
													if(sum<19 || sum>129) continue;
//													++cnt3sum.pass;

//													cnt3prg.count(prang);
													// cnt3prg min=1.396/1.160 max=5.869/7.719 pass 42,464
													if(prang<1.396 || prang>5.869) continue;
//													++cnt3prg.pass;
												}
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
															if ( (do30 && (ceb0 & ~sv02) == 0)
															  || (do31 && (ceb1 & ~sv02) == 0)
															  || (do32 && (ceb2 & ~sv02) == 0)
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
																  || (do42 && (ceb2 & ~sv03) == 0)
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
																	  || (do52 && (ceb2 & ~sv04) == 0)
																	  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																		continue;
																	c8b4 = ns84 ? c8b3 : c8b3 & ~sv4;
																	c7b4 = ns74 ? c7b3 : c7b3 & ~sv4;
																	c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
																	any5 = false;
																	SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																		if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																		  || (ceb1 & ~sv05) == 0
																		  || (ceb2 & ~sv05) == 0
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		c8b5 = ns85 ? c8b4 : c8b4 & ~sv5;
																		c7b5 = ns75 ? c7b4 : c7b4 & ~sv5;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																			  || (ceb1 & ~sv06) == 0
																			  || (ceb2 & ~sv06) == 0
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
																			c8b6 = ns86 ? c8b5 : c8b5 & ~sv6;
																			any7 = false;
																			SV7_LOOP: for ( int sv7 : SVS[c7b6] ) {
																				if ( (ceb0 & ~(sv07=sv06|sv7)) == 0
																				  || (ceb1 & ~sv07) == 0
																				  || (ceb2 & ~sv07) == 0
																				  || (c8b7 = ns87 ? c8b6 : c8b6 & ~sv7) == 0 )
																					continue;
																				any8 = false;
																				SV8_LOOP: for ( int sv8 : SVS[c8b7] ) {
																					if ( (ceb0 & ~(sv08=sv07|sv8)) == 0
																					  || (ceb1 & ~sv08) == 0
																					  || (ceb2 & ~sv08) == 0 )
																						continue;
																					// Combo Allowed!
																					any8 = true;
																					// add to allowedValuesBitsets and
																					// check if that makes anyAllowed
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
												// the common excluders loop is in the covers method
												// with HitSet: 4 = 0 of 215 = 0.00%
												if ( isTop1465 )
													continue;
												DOG_1c: for ( int sv0 : SVS[c0b] ) {
													c8b0 = ns80 ? c8b : c8b & ~sv0;
													c7b0 = ns70 ? c7b : c7b & ~sv0;
													c6b0 = ns60 ? c6b : c6b & ~sv0;
													c5b0 = ns50 ? c5b : c5b & ~sv0;
													c4b0 = ns40 ? c4b : c4b & ~sv0;
													c3b0 = ns30 ? c3b : c3b & ~sv0;
													c2b0 = ns20 ? c2b : c2b & ~sv0;
													any1 = false;
													SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) { // ie c1b0
														sv01 = sv0 | sv1;
														c8b1 = ns81 ? c8b0 : c8b0 & ~sv1;
														c7b1 = ns71 ? c7b0 : c7b0 & ~sv1;
														c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
														c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
														c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
														c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
														any2 = false;
														SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) { // ie c2b1
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
																		  || (c8b5 = ns85 ? c8b4 : c8b4 & ~sv5) == 0
																		  || (c7b5 = ns75 ? c7b4 : c7b4 & ~sv5) == 0
																		  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																			continue;
																		any6 = false;
																		SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																			if ( covers(cmnExclBits, numCmnExclBits, sv06=sv05|sv6)
																			  || (c8b6 = ns86 ? c8b5 : c8b5 & ~sv6) == 0
																			  || (c7b6 = ns76 ? c7b5 : c7b5 & ~sv6) == 0 )
																				continue;
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
																					// check if that makes anyAllowed
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
												// nb: put re-uses fields calculated in get
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
											Pots redPots = createRedPotentials(scells,
													avb0,avb1,avb2,avb3,avb4,avb5,avb6,avb7,avb8);
											if ( redPots.isEmpty() )
												continue; // Should never happen, but never say never.
											// build the excluded combos map for the hint
											ExcludedCombosMap map = buildExcludedCombosMap(
													cmnExcls, numCmnExcls, cells, redPots);
											// create and add the hint
											AHint hint = new AlignedExclusionHint(this
													, redPots, cells, numCmnExcls, cmnExcls, map);

//											standardLog(myLog, cells, gsl, hintNum
//													, cmnExcls, numCmnExcls
//													, cmnExclBits, numCmnExclBits
//													, redPots, map, hint);

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
