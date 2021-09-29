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
 * Aligned 7 (Sept) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 * of 7 Cells.
 */
public final class Aligned7Exclusion_1C extends Aligned7ExclusionBase
implements
//		diuf.sudoku.solver.IReporter,
		diuf.sudoku.solver.hinters.IPreparer,
		java.io.Closeable
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 34; // <HACK/>

	// the maximim number of candidates to permute (process).
	// FYI: 57 misses 7 hints: 3/11 11/34 50/9 98/11 583/24 771/12 1120/90 = 8 minutes
	private static final int MAX_CANDIDATES = 60;
	private static final int NUM_CMN_EXCLS = 10; // was 8

	private static final Cell[] COMMON_EXCLUDERS_ARRAY = new Cell[NUM_CMN_EXCLS];
	private static final int[] COMMON_EXCLUDERS_BITS = new int[NUM_CMN_EXCLS];

	private static final Cell[] CELLS_ARRAY = new Cell[7];
	private static final Cell[] CELLS_ARRAY_1 = new Cell[7];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4
	private final Idx idx05 = new Idx(); // = idx04 & idx5
	private final Idx idx06 = new Idx(); // = idx05 & idx6

	private final ACollisionComparator cc = new ACollisionComparator();

	// I checked by speed. 32k is fastest
	private final NonHinters nonHinters = new NonHinters(32*1024, 3);
	// What's that Skip? Why it's the skipper skipper flipper Flipper.
	private boolean firstPass = true;

//	//useless: protected final Counter cnt1col = new Counter("cnt1col");
//	//useless: protected final Counter cnt1sib = new Counter("cnt1sib");
//	protected final Counter cnt1cs  = new Counter("cnt1cs");
//	//useless: protected final Counter cnt1mbs = new Counter("cnt1mbs");
//	//useless: protected final Counter cnt1ces = new Counter("cnt1ces");
//	protected final Counter cnt1hit = new Counter("cnt1hit");
//	protected final Counter cnt1sum = new Counter("cnt1sum");
//	protected final Counter cnt1prg = new Counter("cnt1prg");
//
//	protected final Counter cnt2col = new Counter("cnt2col");
//	//useless: protected final Counter cnt2sib = new Counter("cnt2sib");
//	//useless: protected final Counter cnt2cs  = new Counter("cnt2cs");
//	protected final Counter cnt2mbs = new Counter("cnt2mbs");
//	protected final Counter cnt2ces = new Counter("cnt2ces");
//	protected final Counter cnt2hit = new Counter("cnt2hit");
//	protected final Counter cnt2sum = new Counter("cnt2sum");
//	protected final Counter cnt2prg = new Counter("cnt2prg");
//
//	protected final Counter cnt3col = new Counter("cnt3col");
//	protected final Counter cnt3sib = new Counter("cnt3sib");
//	//useless: protected final Counter cnt3cs  = new Counter("cnt3cs");
//	protected final Counter cnt3mbs = new Counter("cnt3mbs");
//	//useless: protected final Counter cnt3ces = new Counter("cnt3ces");
//	protected final Counter cnt3hit = new Counter("cnt3hit");
//	//useless: protected final Counter cnt3sum = new Counter("cnt3sum");
//	protected final Counter cnt3prg = new Counter("cnt3prg");

	//nb: there are no cnt4's coz A7E !hint when numCmnExclBits>3 in top1465

//	private java.io.PrintStream myLog = open("a7e.log", standardHeader());

	public Aligned7Exclusion_1C(IInterruptMonitor monitor) {
		super(monitor, IO.A7E_1C_HITS);
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
		// cells position in the Grid.cells array, ie Cell.i, so it's 81 cells.
		final CellSet[] excluders = EXCLUDERS_ARRAY;

		// how many cells are there in each for-i-loop
		final int n0, n1, n2, n3, n4, n5;

		// an array of the 7 cells in an aligned set, ready for exclusion.
		final Cell[] cells = CELLS_ARRAY;
		// sortedCells: the above cells array sorted by collisionComparator.
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

		// indexes into the candidates array of the 7 cells in the aligned set.
		int i0, i1, i2, i3, i4, i5, i6;

		// the cells in the scells (sortedCells) aligned set.
		Cell c0, c1, c2, c3, c4, c5, c6;

		// array of 3*27-bit-bitsets of indexes of c0's excluders in Grid.cells
		Idx idx0;

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is more code than
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		// c1b0: c1 bits version zero = the maybes.bits of c1 minus v0,
		// presuming that c1 is a sibling of c0.
		int	c0b , c1b , c2b , c3b , c4b , c5b , c6b;
		int	            c2b0, c3b0, c4b0, c5b0, c6b0;
		int		              c3b1, c4b1, c5b1, c6b1;
		int				      c3b2, c4b2, c5b2, c6b2;
		int					        c4b3, c5b3, c6b3;
		int							      c5b4, c6b4;
		int								        c6b5;

		// notSibling cache: ns65 = c6.isNotSiblingOf[c5.i];
		boolean ns65, ns64, ns63, ns62, ns61, ns60;
		boolean       ns54, ns53, ns52, ns51, ns50;
		boolean             ns43, ns42, ns41, ns40;
		boolean                   ns32, ns31, ns30;
		boolean                         ns21, ns20;
		boolean                               ns10;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01, sv02, sv03, sv04, sv05, sv06;

		// allowedValuesBitsets: a virtual array of the values that are allowed
		// in each cell in the aligned set. Coincident with scells (sortedCells)
		int avb0, avb1, avb2, avb3, avb4, avb5, avb6;

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1, any2, any3, any4, any5, any6;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel;

		// ces0=size[ceb0=commonExcluderBits[0]]
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// should we empty-test combo after adding 'n' values to it?
		boolean do30,do31,do32, do40,do41,do42, do50,do51,do52;

		// filter variables
		int fives, sixes, sevns, eigts;
		int mbs=0, col=0, sib=0, cs=0, sum=0, hit=0, ces=0;
		double prang=0.0D;

		// presume failure, ie that no hints will be found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//                        Do you smell gas?
		//                         Pull my finger!
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also build a set of sibling excluder-cells for each candidate.
		// nb: top1465 has 0 hints from cells with 7 potential values.
		numCandidates = populateCandidatesAndExcluders(candidates, excluders
				, grid, isTop1465 ? degree : degreePlus1);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

		n0 = numCandidates - 6;
		n1 = numCandidates - 5;
		n2 = numCandidates - 4;
		n3 = numCandidates - 3;
		n4 = numCandidates - 2;
		n5 = numCandidates - 1;
		for ( i0=0; i0<n0; ++i0 ) {
			idx0 = excluders[(cells[0]=candidates[i0]).i].idx();
			if(hitMe && cells[0]!=hitCells[0]) continue;
			for ( i1=i0+1; i1<n1; ++i1 ) {
				if ( excluders[(cells[1]=candidates[i1]).i].idx1(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
				for ( i2=i1+1; i2<n2; ++i2 ) {
					// skips maybe 15% so worth caching
					if ( excluders[(cells[2]=candidates[i2]).i].idx1(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
					for ( i3=i2+1; i3<n3; ++i3 ) {
						// skips maybe 30% so borderline on caching
						if ( excluders[candidates[i3].i].idx1(idx03, idx02) )
							continue;
						cells[3] = candidates[i3];
						if(hitMe && cells[3]!=hitCells[3]) continue;
						for ( i4=i3+1; i4<n4; ++i4 ) {
							// skips about 60% so not worth caching
							if ( excluders[candidates[i4].i].idx1(idx04, idx03) )
								continue;
							cells[4] = candidates[i4];
							if(hitMe && cells[4]!=hitCells[4]) continue;
							for ( i5=i4+1; i5<n5; ++i5 ) {
								// skips about 70% so not worth caching
								if ( excluders[candidates[i5].i].idx1(idx05, idx04) )
									continue;
								cells[5] = candidates[i5];
								if(hitMe && cells[5]!=hitCells[5]) continue;
								// happens "often enough" in the penultimate loop.
								if ( isInterrupted() )
									return false;
								for ( i6=i5+1; i6<numCandidates; ++i6 ) {
									// skips about 90% so not worth caching
									if ( excluders[candidates[i6].i].idx1(idx06, idx05) )
										continue;
									cells[6] = candidates[i6];
									if(hitMe && cells[6]!=hitCells[6]) continue;

									if ( nonHinters.skip(cells) )
										continue;

									// filter top1465 even when !isHacky, to not take 34 hours again.
									// nb: the magic numbers are from a7e.log via tools.AnalyseLog
									if ( filterTop1465 ) {
										// filter by total number of maybes shared by siblings
										col = countCollisions(cells); // countCollisions
//										colCnt.count(col);
										// colCnt min=7/0 max=49/92 pass 1,298,497,489 of 1,393,794,600 skip 95,297,111 = 6.84%
										if(col<7 || col>49) continue;
//										++colCnt.pass;

										// filter by total cells.maybes.size
										mbs = totalMaybesSize; // from countCollisions
//										mbsCnt.count(mbs);
										// mbsCnt min=15/14 max=30/42 pass 1,252,380,927 of 1,298,497,489 skip 46,116,562 = 3.55%
										if(mbs<15 || mbs>30) continue;
//										++mbsCnt.pass;

										// filter by total number of sibling relationships (edges)
										sib = siblingsCount; // from countCollisions
//										sibCnt.count(sib);
										// sibCnt min=9/5 max=17/21 pass 712,936,510 of 1,252,380,927 skip 539,444,417 = 43.07%
										if(sib<9 || sib>17) continue;
//										++sibCnt.pass;

										// this one is filtered in the switch, just on the off chance.
										// I now don't think it'll do us any good: cost v rewards
										cs = col + sib;

										// filter by maximum number of "large" cells
//										++maxMbs.cnt;
										fives = sixes = sevns = eigts = 0;
										for ( Cell cell : cells )
											switch ( cell.maybes.size ) {
											case 9: //fallthrough // 9 is the maximum possible
											case 8: ++eigts; //fallthrough
											case 7: ++sevns; //fallthrough
											case 6: ++sixes; //fallthrough
											case 5: ++fives; //fallout
											}
										// maxMbs pass 712,428,688 of 712,936,510 skip 507,822 = 0.07%
										if(fives>4) continue;
										if(sixes>2) continue;
										if(sevns>1) continue;
										if(eigts>0) continue;
//										++maxMbs.pass;
									}

									// read common excluder cells from grid at idx06
									if ( (numCmnExcls = idx06.cellsN(grid, cmnExcls)) == 1 ) {
										cmnExclBits[0] = cmnExcls[0].maybes.bits;
										numCmnExclBits = 1;
									} else {
										// performance enhancement: examine smaller maybes sooner.
										//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
										bubbleSort(cmnExcls, numCmnExcls);
										// get the common excluder cells maybes bits, minus any supersets
										// eg: {125,12}=>{12} coz 12 subset of 125, so 125 does nada.
										numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);
									}
									assert numCmnExclBits >= 1;
									// remove each common excluder bits which contains a value
									// that is not in any candidates maybes, because no combo
									// can cover it, so it can't contribute to an exclusion.
									// eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
									// skip if there are no common excluder bits remaining
									if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
											, allMaybesBits(cells))) == 0 )
										continue;

									// filter top1465 even when !isHacky, to not take 34 hours again.
									// nb: the magic numbers are from a7e.log via tools.AnalyseLog
									if ( filterTop1465 ) {
										// 429 is the only puzzle in top1465 which hints
										// from a reduced number of common excluder bits
										if ( numCmnExclBits<numCmnExcls && gsl!=429 )
											continue;

										hit = countHits(cmnExclBits, numCmnExclBits, cells);
//										hitCnt.count(hit); // relies on sum
										// hitCnt min=2/2 max=29/81 pass 651,655,487 of 655,606,398 skip 3,950,911 = 0.60%
										if(hit<2 || hit>29) continue;
//										++hitCnt.pass;

										sum = col + hit;
//										sumCnt.count(sum);
										// sumCnt min=11/9 max=75/78 pass 649,667,881 of 651,655,487 skip 1,987,606 = 0.31%
										if(sum<11 || sum>75) continue;
//										++sumCnt.pass;

										prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//										prangCnt.count(prang);
										// prangRate min=1.630/1.512 max=12.219/17.308 pass 646,897,065 of 649,667,881 skip 2,770,816 = 0.43%
										if(prang<1.629D || prang>12.220D) continue;
//										++prangCnt.pass;
									}

									// the dog____ing algorithm is faster with dodgem-cars to the left,
									// but the above for-i-loops need a static cells array; so we copy
									// cells to scells (sortedCells) and sort that array DESCENDING by:
									// 4*maybesCollisions + 2*commonExcluderHits + maybes.size
									cc.set(cells, cmnExclBits, numCmnExclBits);
									System.arraycopy(cells, 0, scells, 0, degree);
									//MyTimSort.small(scells, degree, cc);
									bubbleSort(scells, degree, cc);

									// cache the cells in the aligned set, and there maybes.
									c0b = (c0=scells[0]).maybes.bits;
									c1b = (c1=scells[1]).maybes.bits;
									c2b = (c2=scells[2]).maybes.bits;
									c3b = (c3=scells[3]).maybes.bits;
									c4b = (c4=scells[4]).maybes.bits;
									c5b = (c5=scells[5]).maybes.bits;
									c6b = (c6=scells[6]).maybes.bits;

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

									// should we test if 3/4/5 values cover ceb0
									ces0 = VSIZE[ceb0=cmnExclBits[0]];
									do30 = ces0 <= 3;
									do40 = ces0 <= 4;
									do50 = ces0 <= 5;
									//nb: do60 is allways true so doesn't exist

									// clear allowedValuesBitsets virtual array
									avb0=avb1=avb2=avb3=avb4=avb5=avb6 = 0;

									// anyLevel is where I'm upto: it controls which for-i-loop
									// to break-out-of when all maybes of all cells to my right
									// are already allowed; so that the FIRST allowed combo sets
									// my any* and we skip all other "like" combos.
									// This is for performance only. Algorithm works without it.
									// We just don't waste time evaluating multiple combos whose
									// right-hand-side-values are all already allowed. See the
									// anyLevel switch in A5E_1C for a more complete explanation.
									anyLevel = degree; // means that avb6 != c6b

									// The dog____ing loop calculates: is each combo allowed?
									// ie: Is each possible combination of the potential values
									// of these $degree (7) cells (the aligned set) allowed?
									// If the combo is allowed then record it in the (avbs)
									// allowedValuesBitsets array; else the combo is skipped.
									// After the loop we check if all of each cells maybes have
									// been allowed, and if so we skip this aligned set (nothing
									// to see here, move along). But if all possible combinations
									// of potential values do NOT include certain value/s in
									// certain position/s then there's an exclusion, so we build
									// the excludedCombosMap and the hint.
									// There are 2 exclusion rules:
									// (1) Hidden Single rule: Using the same value for two cells
									//     cells is allowed only if the cells do not share a
									//     region, ie are NOT siblings.
									// (2) Common Excluder rule: If the combo covers (is a superset
									//     of) any common excluder cells maybes then the combo is
									//     NOT allowed, because the excluder cell must be one of
									//     those values (no matter which).
									// nb: it's faster to "pre-eliminate" ie remove maybes from
									// sibling cells for large (4+) sets than "skip collisions"
									// as in A23E; because we do more of the work less often.
									// Compare DOG_1 with A3E's DOG_1 to see what I'm on about.

									// nb: this switch for performance only, the default path
									// work for any numCmnExclBits, it just faster with no call.
++counts[0];
++counts[numCmnExclBits];
									switch (numCmnExclBits) {
									case 1:
										// there's no need for a common excluders loop
										//  w/o HitSet: 1 = 1,215,734,258 of 1,231,143,645 = 98.75%
										// with HitSet: 1 =           500 of           592 = 84.46%

										// filter top1465 even when !isHacky, to not take 34 hours again.
										// nb: the magic numbers are from a7e.log via tools.AnalyseLog
										if ( filterTop1465 ) {

//useless:									cnt1col.count(col);
//											// cnt1col min=7/7 max=49/49 pass 636,927,099 of 636,927,099 skip 0 = 0.00%
//											if(col<7 || col>49) continue;
//											++cnt1col.pass;

//useless:									cnt1sib.count(sib);
//											// cnt1sib min=9/9 max=17/17 pass 636,927,099 of 636,927,099 skip 0 = 0.00%
//											if(sib<7 || sib>17) continue;
//											++cnt1sib.pass;

//											cnt1cs.count(cs);
											// cnt1cs min=16/16 max=65/66 pass 640,341,326 of 640,347,854 skip 6,528 = 0.00%
											if(cs>65) continue;
//											++cnt1cs.pass;

//useless:									cnt1mbs.count(mbs);
//											// cnt1mbs min=15/15 max=30/30 pass 639,233,142
//											if(mbs<? || mbs>?) continue;
//											++cnt1mbs.pass;

//useless:									ces = ces0;
//											cnt1ces.count(ces);
//											// cnt1ces min=2/2 max=6/6 pass 636,920,590
//											if(ces>6) continue;
//											++cnt1ces.pass;

//											cnt1hit.count(hit);
											// cnt1hit min=2/2 max=23/29 pass 634,178,155 of 640,341,326 skip 6,163,171 = 0.96%
											if(hit>23) continue;
//											++cnt1hit.pass;

//											cnt1sum.count(sum);
											// cnt1sum min=11/11 max=67/72 pass 634,159,746 of 634,178,155 skip 18,409 = 0.00%
											if(sum>67) continue;
//											++cnt1sum.pass;

//											cnt1prg.count(prang);
											// cnt1prg min=1.763/1.647 max=12.220/12.219 pass 634,158,513 of 634,159,746 skip 1,233 = 0.00%
											if(prang<1.763) continue;
//											++cnt1prg.pass;
										}
										DOG_1a: for ( int sv0 : SVS[c0b] ) {
											// (1) Hidden Single rule: pre-elimination
											c6b0 = ns60 ? c6b : c6b & ~sv0;
											c5b0 = ns50 ? c5b : c5b & ~sv0;
											c4b0 = ns40 ? c4b : c4b & ~sv0;
											c3b0 = ns30 ? c3b : c3b & ~sv0;
											c2b0 = ns20 ? c2b : c2b & ~sv0;
											any1 = false;
											SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
												sv01 = sv0 | sv1; // build-up the combo
												c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
												c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
												c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
												c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
												any2 = false;
												SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
													sv02 = sv01 | sv2;
													// tune: test sv02,c3b2: wo 66.631 => w 60.305 F9
													if ( (do30 && (ceb0 & ~sv02) == 0)
													  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
														continue;
													// tune: test c6b2,c5b2,c4b2: wo 5:40 => w 06:50 F10 FAIL!
													//                  with pre recursiveIsOn 05:49 F10 FAIL!
													c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
													c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
													c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
													any3 = false;
													SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
														sv03 = sv02 | sv3;
														// tune: test c6b3,c5b3: wo 5:42 => w 5.40 F10
														if ( (do40 && (ceb0 & ~sv03) == 0)
														  || (c6b3 = ns63 ? c6b2 : c6b2 & ~sv3) == 0
														  || (c5b3 = ns53 ? c5b2 : c5b2 & ~sv3) == 0
														  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
															continue;
														any4 = false;
														SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
															sv04 = sv03 | sv4;
															// tune: test c6b4: wo 5:55 => w 5:42 F10
															if ( (do50 && (ceb0 & ~sv04) == 0)
															  || (c6b4 = ns64 ? c6b3 : c6b3 & ~sv4) == 0
															  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																continue;
															any5 = false;
															SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																	continue;
																any6 = false;
																SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																	if ( (ceb0 & ~(sv05|sv6)) == 0 )
																		continue;
																	// Combo Allowed!
																	any6 = true;
																	// add to allowedValuesBitsets and
																	// check if that makes all allowed.
																	if ( (avb6|=sv6) != c6b )
																		continue;
																	// We allow only the FIRST combo at each level
																	// and then move-on to evaluating the next value
																	// at this level which hasn't yet been evaluated;
																	// instead of wasting time evaluating multiple
																	// combos-to-my-right which can not allow any
																	// additional maybes. This is faster, reducing:
																	// O(n*n*n*n*n*n*n) to O(n*n'ish).
																	switch ( anyLevel ) {
																	case 7: anyLevel=6; break SV6_LOOP;
																	case 6: break SV6_LOOP;
																	case 5: any5=true; break SV5_LOOP;
																	case 4: any4=true; break SV4_LOOP;
																	case 3: any3=true; break SV3_LOOP;
																	case 2: any2=true; break SV2_LOOP;
																	case 1: any1=true; break SV1_LOOP;
																	} // end-switch
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
										// there's still no need for a common excluders loop
										//  w/o HitSet: 2 = 14,773,394 of 1,231,143,645 = 1.20%
										// with HitSet: 2 = 91 of 592 = 15.37%
										ces1 = VSIZE[ceb1=cmnExclBits[1]];
										do31 = ces1 <= 3;
										do41 = ces1 <= 4;
										do51 = ces1 <= 5;

										// filter top1465 even when !isHacky, to not take 34 hours again.
										// nb: the magic numbers are from a7e.log via tools.AnalyseLog
										if ( filterTop1465 ) {
//											cnt2col.count(col);
											// cnt2col min=7/7 max=46/49 pass 9,907,663 of 9,907,940 skip 277 = 0.00%
											if(col<7 || col>46) continue;
//											++cnt2col.pass;

//useless: 									cnt2sib.count(sib);
//											// cnt2sib min=9/9 max=16/16 pass 9,839,006
//											if(sib<9 || sib>16) continue;
//											++cnt2sib.pass;

//useless: 									cnt2cs.count(cs);
//											// cnt2cs min=16/16 max=62/62 pass 9,839,006
//											if(cs<16 || cs>62) continue;
//											++cnt2cs.pass;

//											cnt2mbs.count(mbs);
											// cnt2mbs min=16/15 max=30/30 pass 9,906,707 of 9,907,663 skip 956 = 0.01%
											if(mbs<16) continue;
//											++cnt2mbs.pass;

											ces = ces0 + ces1;
//											cnt2ces.count(ces);
											// cnt2ces min=4/4 max=11/12 pass 9,905,649 of 9,906,707 skip 1,058 = 0.01%
											if(ces>11) continue;
//											++cnt2ces.pass;

//											cnt2hit.count(hit);
											// cnt2hit min=5/4 max=29/29 pass 9,899,155 of 9,905,649 skip 6,494 = 0.07%
											if(hit<5) continue;
//											++cnt2hit.pass;

//											cnt2sum.count(sum);
											// cnt2sum min=12/12 max=75/75 pass 9,899,155 of 9,899,155 skip 0 = 0.00%
											if(sum<12) continue;
//											++cnt2sum.pass;

//											cnt2prg.count(prang);
											// cnt2prg min=1.629/1.630 max=5.539/6.857 pass 9,886,109 of 9,899,155 skip 13,046 = 0.13%
											if(prang>5.539) continue;
//											++cnt2prg.pass;
										}
										DOG_1b: for ( int sv0 : SVS[c0b] ) {
											c6b0 = ns60 ? c6b : c6b & ~sv0;
											c5b0 = ns50 ? c5b : c5b & ~sv0;
											c4b0 = ns40 ? c4b : c4b & ~sv0;
											c3b0 = ns30 ? c3b : c3b & ~sv0;
											c2b0 = ns20 ? c2b : c2b & ~sv0;
											any1 = false;
											SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
												sv01 = sv0 | sv1;
												c6b1 = ns61 ? c6b0 : c6b0 & ~sv1;
												c5b1 = ns51 ? c5b0 : c5b0 & ~sv1;
												c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
												c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
												any2 = false;
												SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) {
													sv02 = sv01 | sv2;
													if ( (do30 && (ceb0&~sv02)==0)
													  || (do31 && (ceb1&~sv02)==0)
													  || (c3b2 = ns32 ? c3b1 : c3b1 & ~sv2) == 0 )
														continue;
													c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
													c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
													c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
													any3 = false;
													SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
														sv03 = sv02 | sv3;
														if ( (do40 && (ceb0&~sv03)==0)
														  || (do41 && (ceb1&~sv03)==0)
														  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
															continue;
														c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
														c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
														any4 = false;
														SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
															sv04 = sv03 | sv4;
															if ( (do50 && (ceb0&~sv04)==0)
															  || (do51 && (ceb1&~sv04)==0)
															  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																continue;
															c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
															any5 = false;
															SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																  || (ceb1 & ~sv05) == 0
																  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																	continue;
																any6 = false;
																SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																	if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																	  || (ceb1 & ~sv06) == 0 )
																		continue;
																	// Combo Allowed!
																	any6 = true;
																	// add to allowedValuesBitsets and
																	// check if that makes all allowed.
																	if ( (avb6|=sv6) != c6b )
																		continue;
																	// We allow only the FIRST combo at each level
																	switch ( anyLevel ) {
																	case 7: anyLevel=6; break SV6_LOOP;
																	case 6: break SV6_LOOP;
																	case 5: any5=true; break SV5_LOOP;
																	case 4: any4=true; break SV4_LOOP;
																	case 3: any3=true; break SV3_LOOP;
																	case 2: any2=true; break SV2_LOOP;
																	case 1: any1=true; break SV1_LOOP;
																	} // end-switch
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
									case 3:
										// there's still no need for a common excluders loop
										//  w/o HitSet: 3 = 635,993 of 1,231,143,645 = 0.05%
										// with HitSet: 3 = 1 of 592 = 0.17%
										ces1 = VSIZE[ceb1=cmnExclBits[1]];
										do31 = ces1 <= 3;
										do41 = ces1 <= 4;
										do51 = ces1 <= 5;
										ces2 = VSIZE[ceb2=cmnExclBits[2]];
										do32 = ces2 <= 3;
										do42 = ces2 <= 4;
										do52 = ces2 <= 5;

										// filter top1465 even when !isHacky, to not take 34 hours again.
										// nb: the magic numbers are from a7e.log via tools.AnalyseLog
										if ( filterTop1465 ) {
//											cnt3col.count(col);
											// cnt3col min=7/7 max=32/40 pass 7,715 of 131,775 skip 124,060 = 94.15%
											if(col!=15) continue;
//											++cnt3col.pass;

//											cnt3sib.count(sib);
											// cnt3sib min=9/9 max=15/15 pass 5,830 of 7,715 skip 1,885 = 24.43%
											if(sib!=9) continue;
//											++cnt3sib.pass;

//useless: 15+9==24							cnt3cs.count(cs);
//											// cnt3cs min=24/24 max=24/24 pass 5,815
//											if(cs!=24) continue;
//											++cnt3cs.pass;

//useless:									ces = ces0 + ces1 + ces2;
//											cnt3ces.count(ces);
//											// cnt3ces min=6/6 max=12/12 pass 5,815
//											if(ces<? || ces>?) continue;
//											++cnt3ces.pass;

//											cnt3mbs.count(mbs);
											// cnt3mbs min=22/21 max=28/29 pass 5,826 of 5,830 skip 4 = 0.07%
											if(mbs>28) continue;
//											++cnt3mbs.pass;

//											cnt3hit.count(hit);
											// cnt3hit min=15/9 max=29/29 pass 5,750 of 5,826 skip 76 = 1.30%
											if(hit<15) continue;
//											++cnt3hit.pass;

//useless: 									cnt3sum.count(sum);
//											// cnt3sum min=30/30 max=44/44 pass 5,750 of 5,750 skip 0 = 0.00%
//											if(sum<30) continue;
//											++cnt3sum.pass;

//											cnt3prg.count(prang);
											// cnt3prg min=2.224/2.150 max=3.230/3.467 pass 5,727 of 5,750 skip 23 = 0.40%
											if(prang<2.224 || prang>3.230) continue;
//											++cnt3prg.pass;
										}
										DOG_1c: for ( int sv0 : SVS[c0b] ) {
											c6b0 = ns60 ? c6b : c6b & ~sv0;
											c5b0 = ns50 ? c5b : c5b & ~sv0;
											c4b0 = ns40 ? c4b : c4b & ~sv0;
											c3b0 = ns30 ? c3b : c3b & ~sv0;
											c2b0 = ns20 ? c2b : c2b & ~sv0;
											any1 = false;
											SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) {
												sv01 = sv0 | sv1;
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
															c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
															any5 = false;
															SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																if ( (ceb0 & ~(sv05=sv04|sv5)) == 0
																  || (ceb1 & ~sv05) == 0
																  || (ceb2 & ~sv05) == 0
																  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																	continue;
																any6 = false;
																SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																	if ( (ceb0 & ~(sv06=sv05|sv6)) == 0
																	  || (ceb1 & ~sv06) == 0
																	  || (ceb2 & ~sv06) == 0 )
																		continue;
																	// Combo Allowed!
																	any6 = true;
																	// add to allowedValuesBitsets and
																	// check if that makes all allowed.
																	if ( (avb6|=sv6) != c6b )
																		continue;
																	// We allow only the FIRST combo at each level
																	switch ( anyLevel ) {
																	case 7: anyLevel=6; break SV6_LOOP;
																	case 6: break SV6_LOOP;
																	case 5: any5=true; break SV5_LOOP;
																	case 4: any4=true; break SV4_LOOP;
																	case 3: any3=true; break SV3_LOOP;
																	case 2: any2=true; break SV2_LOOP;
																	case 1: any1=true; break SV1_LOOP;
																	} // end-switch
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
										//  w/o HitSet: 4 = 0 of 1,231,143,645 = 0.00%
										// with HitSet: 4 = 0 of 592 = 0.00%
										// so it looks like I never used this. Sigh.
										if ( isTop1465 )
											continue;
										DOG_1d: for ( int sv0 : SVS[c0b] ) {
											c6b0 = ns60 ? c6b : c6b & ~sv0;
											c5b0 = ns50 ? c5b : c5b & ~sv0;
											c4b0 = ns40 ? c4b : c4b & ~sv0;
											c3b0 = ns30 ? c3b : c3b & ~sv0;
											c2b0 = ns20 ? c2b : c2b & ~sv0;
											any1 = false;
											SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) { // ie c1b0
												sv01 = sv0 | sv1;
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
													c6b2 = ns62 ? c6b1 : c6b1 & ~sv2;
													c5b2 = ns52 ? c5b1 : c5b1 & ~sv2;
													c4b2 = ns42 ? c4b1 : c4b1 & ~sv2;
													any3 = false;
													SV3_LOOP: for ( int sv3 : SVS[c3b2] ) {
														sv03 = sv02 | sv3;
														if ( do40 && covers(cmnExclBits, numCmnExclBits, sv03)
														  || (c4b3 = ns43 ? c4b2 : c4b2 & ~sv3) == 0 )
															continue;
														c6b3 = ns63 ? c6b2 : c6b2 & ~sv3;
														c5b3 = ns53 ? c5b2 : c5b2 & ~sv3;
														any4 = false;
														SV4_LOOP: for ( int sv4 : SVS[c4b3] ) {
															sv04 = sv03 | sv4;
															if ( do50 && covers(cmnExclBits, numCmnExclBits, sv04)
															  || (c5b4 = ns54 ? c5b3 : c5b3 & ~sv4) == 0 )
																continue;
															c6b4 = ns64 ? c6b3 : c6b3 & ~sv4;
															any5 = false;
															SV5_LOOP: for ( int sv5 : SVS[c5b4] ) {
																if ( covers(cmnExclBits, numCmnExclBits, sv05=sv04|sv5)
																  || (c6b5 = ns65 ? c6b4 : c6b4 & ~sv5) == 0 )
																	continue;
																any6 = false;
																SV6_LOOP: for ( int sv6 : SVS[c6b5] ) {
																	if ( covers(cmnExclBits, numCmnExclBits, sv05|sv6) )
																		continue;
																	// Combo Allowed!
																	any6 = true;
																	// add to allowedValuesBitsets
																	// and check if that makes all allowed.
																	if ( (avb6|=sv6) != c6b )
																		continue;
																	// We allow only the FIRST combo at each level
																	switch ( anyLevel ) {
																	case 7: anyLevel=6; break SV6_LOOP;
																	case 6: break SV6_LOOP;
																	case 5: any5=true; break SV5_LOOP;
																	case 4: any4=true; break SV4_LOOP;
																	case 3: any3=true; break SV3_LOOP;
																	case 2: any2=true; break SV2_LOOP;
																	case 1: any1=true; break SV1_LOOP;
																	} // end-switch
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
									  && avb6 == c6b ) {
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

									// create the hint (and possibly return)
									Pots redPots = createRedPotentials(scells
										, avb0,avb1,avb2,avb3,avb4,avb5,avb6);
									if ( redPots.isEmpty() )
										continue; // Should never happen, but never say never.
									ExcludedCombosMap map = buildExcludedCombosMap(
										cmnExcls, numCmnExcls, cells, redPots);
									AHint hint = new AlignedExclusionHint(this
										, redPots, cells, numCmnExcls, cmnExcls
										, map);

//									standardLog(myLog, cells, gsl, hintNum
//											, cmnExcls, numCmnExcls
//											, cmnExclBits, numCmnExclBits
//											, redPots, map, hint);

									result = true; // in case add returns false
									if ( accu.add(hint) )
										return true;

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
