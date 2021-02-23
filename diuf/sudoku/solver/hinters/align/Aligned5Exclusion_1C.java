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
 * Aligned 5 (Pent) Exclusion technique.
 * <p>Implements the aligned set exclusion Sudoku solving technique for sets
 *  of 5 Cells, and there 5 values.
 * <p>This "_1C" implementation "correctly" requires one common excluder cell
 * per aligned set, so it finds three times as many hints as the hacked version
 * (_2H) but takes ten times as long to run, so somebody might use this version
 * when Mr More catches up with me, until then I recommend that you just settle
 * for the hacked version, coz ...
 * <pre>
 *                       THE CORRECT VERSIONS ARE TOO SLOW!
 *                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * </pre>
 */
public final class Aligned5Exclusion_1C extends Aligned5ExclusionBase
		implements java.io.Closeable
//				 , diuf.sudoku.solver.IReporter
{
	// the minimim number of candidates to permute (process).
	private static final int MIN_CANDIDATES = 25; // <HACK/>

	// the maximim number of candidates to permute (process).
	private static final int MAX_CANDIDATES = 60; // <HACK/>

	private static final int NUM_CMN_EXCLS = 6;

	private static final Cell[] commonExcludersArray = new Cell[NUM_CMN_EXCLS];
	private static final int[] commonExcluderBitsArray = new int[NUM_CMN_EXCLS];

	private static final CellSet[] excludersArray = new CellSet[81];
	private static final Cell[] cellsArray = new Cell[5];

	// common excluders indexes: idx02 is an index of the siblings common
	// to c0 and c1 and c2. See LinkedMatrixCellSet.idx() for more.
	private final Idx idx01 = new Idx(); // = idx0 & idx1
	private final Idx idx02 = new Idx(); // = idx01 & idx2
	private final Idx idx03 = new Idx(); // = idx02 & idx3
	private final Idx idx04 = new Idx(); // = idx03 & idx4

//	private final ACollisionComparator cc = new ACollisionComparator();

	// AAlignedSetExclusionBase.NonHinters.maxSize = 65359 A little overloaded
	// is OK, it just means a tad more than about 8 max elements per list.
	private final NonHinters nonHinters = new NonHinters(64*1024, 4);

//	//useless: protected final Counter cnt1col = new Counter("cnt1col");
//	//useless: protected final Counter cnt1sib = new Counter("cnt1sib");
//	//useless: protected final Counter cnt1cs  = new Counter("cnt1cs");
//	//useless: protected final Counter cnt1mbs = new Counter("cnt1mbs");
//	//useless: protected final Counter cnt1ces = new Counter("cnt1ces");
//	protected final Counter cnt1hit = new Counter("cnt1hit");
//	protected final Counter cnt1sum = new Counter("cnt1sum");
//	protected final Counter cnt1prg = new Counter("cnt1prg");
//
//	protected final Counter cnt2col = new Counter("cnt2col");
//	//useless: protected final Counter cnt2sib = new Counter("cnt2sib");
//	protected final Counter cnt2cs  = new Counter("cnt2cs");
//	//useless: protected final Counter cnt2mbs = new Counter("cnt2mbs");
//	//useless: protected final Counter cnt2ces = new Counter("cnt2ces");
//	protected final Counter cnt2hit = new Counter("cnt2hit");
//	//useless: protected final Counter cnt2sum = new Counter("cnt2sum");
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
//
//	protected final Counter cnt4col = new Counter("cnt4col");
//	protected final Counter cnt4sib = new Counter("cnt4sib");
//	protected final Counter cnt4cs  = new Counter("cnt4cs");
//	protected final Counter cnt4mbs = new Counter("cnt4mbs");
//	protected final Counter cnt4ces = new Counter("cnt4ces");
//	protected final Counter cnt4hit = new Counter("cnt4hit");
//	protected final Counter cnt4sum = new Counter("cnt4sum");
//	protected final Counter cnt4prg = new Counter("cnt4prg");

//	protected long prangErrors = 0;

//	private java.io.PrintStream myLog = open("a5e.log", standardHeader());

	public Aligned5Exclusion_1C(int firstHintNumber, IInterruptMonitor monitor) {
		// firstHintNumber: A5E never finds the 1st, 2nd, or 3rd hint in
		// top1465.d5.mt, the first hint I ever find is the 4th one, so I am
		// deactivated (ie I am not even called) until the 4th hint, because
		// that's a bit faster.
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

//	@Override
//	public void report() {
//		super.report();
//		diuf.sudoku.utils.Log.format("prangErrors = %,d\n", prangErrors);
//	}

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
		// We create the local reference just for speed of access.
		final int[][] SVS = Values.SHIFTED;

		// Integer.bitCount of the bitsets 0..511, ie the size of each element
		// in the SVS (Values.SHIFTED) array-of-arrays. We use this array coz
		// it's much faster than calling Integer.bitCount billions of times.
		// We create the local reference just for speed of access.
		final int[] SIZE = Values.SIZE;

		// The populate populateCandidatesAndExcluders fields: a candidate has
		// maybes.size>=2 and has 2 excluders with maybes.size 2..$degree
		// NB: Use arrays for speed. They get HAMMERED!
		final Cell[] candidates = candidatesArray;
		final int numCandidates;
		// an array of each candidates set-of-excluder-cells, indexed by each
		// cells position in the Grid.cells array, ie Cell.i.
		final CellSet[] excluders = excludersArray;

		// an array of the 5 cells in each aligned set.
		// nb: when a candidate cell is added to cells it is "elected", ceasing
		//     to be a candidate and becoming "a member of an aligned set",
		//     which is aligned around 1-or-more common excluder cells.
		// nb: A5E_2H's (2hacked) magic is requiring 2-or-more common excluders,
		//     coz each such aligned set has a higher probability of hinting:
		//     it's like 1:12k with 2+cebs and a tiny 1:100k with 1ceb. A third
		//     of hints come from 2+cebs which are just 4% of the aligned sets.
		final Cell[] cells = cellsArray;

//KRC#2020-06-30 09:50:00
//		// sortedCells the above cells array sorted by the CollisionComparator
//		// with the dodgem-cars to the left, and the drag-cars to the right.
//		final Cell[] scells = new Cell[degree];

		// the number of cells in each of the for-i-loops
		// nb: n4 (the last one) is just numCandidates so it doesn't exist.
		final int n0, n1, n2, n3;

		// cmnExcls provides fast array access to the common-excluder-cells
		final Cell[] cmnExcls = commonExcludersArray;
		// the common-excluder-cells-maybes-bits. This set may differ from the
		// actual common-excluder-cells in that any supersets are removed.
		final int[] cmnExclBits = commonExcluderBitsArray;
		// number of common excluder cells, and common excluder bits (differs)
		int numCmnExcls, numCmnExclBits;

		// array of 3*27-bits-bitsets of indexes-in-Grid.cells of c0's excluders
		Idx idx0;

		// for-i-loop candidates indexes to build the array of 5 aligned cells.
		int i0, i1, i2, i3, i4;

		// a virtual array of the 5 cells in an aligned set.
		Cell c0, c1, c2, c3, c4;

		// cache of isNotSiblingOf. eg: ns10=c1.isNotSiblingOf[c0.i];
		boolean ns10, ns20, ns30, ns40;
		boolean		  ns21, ns31, ns41;
		boolean				ns32, ns42;
		boolean					  ns43;

		// shiftedValues aggregate bitsets: sv01:=sv0|sv1, sv02=sv01|sv2, ...
		int sv01, sv02, sv03, sv04;

		// allowedValuesBitsets: a virtual array of bitsets of the values that
		// are allowed in scells (sortedCells) array.
		int avb0, avb1, avb2, avb3, avb4;

		// Bitsets of the maybes of each cell. When this cells value is chosen
		// its value is removed from the maybes of all the subsequent cells in
		// the set which are siblings of this cell. This is more code than just
		// "skip collision" (as per A234E) but it is faster, because it does
		// more of the work less often.
		int c0b, c1b , c2b , c3b , c4b;  // c4b = c4.maybes.bits;
		int		       c2b0, c3b0, c4b0; // c4b0= c4.sib(c0) ? c4b&~sv0 : c4b;
		int			         c3b1, c4b1; // c4b1= c4.sib(c1) ? c4b0&~sv1 : c4b0;
		int					 c3b2, c4b2; // c4b2= c4.sib(c2) ? c4b1&~sv2 : c4b1;
		int						   c4b3; // c4b3= c4.sib(c3) ? c4b2&~sv3 : c4b2;

		// any*: were any sub-combos allowed? is a tad faster.
		boolean any1, any2, any3, any4;

		// The anyLevel determines which SV*_LOOP we break when all maybes of
		// all cells to my right have already been allowed. The smaller the
		// number the further left we are. So for example: anyLevel==5 is
		// equivalent to avb5==c5b && avb6==c6b && avb7==c7b && avb8==c8b;
		// ie all maybes of all cells to the right of 4 are already allowed.
		int anyLevel; // the number of cells in the aligned set.

		// commonExcluderBits[0] and [1]
		int ceb0,ces0, ceb1,ces1, ceb2,ces2;

		// should we test of combo of 3,4 values covers ceb0/1/2
		boolean do30,do31,do32, do40,do41,do42;

		// filter variables
		int fives;
		int mbs=0, col=0, sib=0, cs=0, sum=0, hit=0; //, ces=0;
		double prang=0.0D;

		// presume failure, ie that no hints will be found
		boolean result = false;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Here's where we stop creating variables. From this point down all
		// variables (except the loop indexes) are pre-existing, so that we're
		// not wasting time needlessly pushing-and-popping stack-frames.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// find cells that are candidates to participate in exclusion sets,
		// and also the set of sibling excluder-cells for each candidate.
		numCandidates = populateCandidatesAndExcluders(candidates, excluders, grid);
		if ( numCandidates < degree )
			return false; // this'll never happen, but never say never.

		if ( hackTop1465 ) {
			if ( numCandidates < MIN_CANDIDATES )
				return false; // this happens, just not real often.
			if ( numCandidates > MAX_CANDIDATES )
				return false;
		}

// =============================================================================
// The difference between _1C (correct) and _2H (hacked) is that _1C calls idx1
// and _2H calls idx2 to require 2 common excluder cells for each "aligned set".
// The number of sets with 1 common excluder is HUGE:
//		A5E_1C top1465 filtered: 541,954,122 of 568,000,214 = 95.41%
// but the hit rate in those sets is tiny (something like 1 in 5000), so that
// two-thirds of the hints found using idx1 are from those 95% of sets; but the
// other 5% of sets with 2-or-more common excluders produces third of the hints,
// so the hacked version just REQUIRES 2 common excluder (ignoring 95% of sets),
// to find a third of the hints in about 10% of the time.
//
// This all stinks like micky nuts left in the sun for 3 days: No self-respectin
// dog would allow it; but I reckon that its better to get a third of the hints
// that are there rather than not have the hacked version and miss the aligned
// 4-and-more exclusion hints all-together (as per Juillerat's original) because
// the user disables the technique because it's just too bloody slow; but I'm
// still not comfortable/confident with this position. It's indefensible. Hence
// do dog joke, to take my mind of it, and to relax you into loving it up ...
//
// I keep thinking that there must be a way to find aligned exclusions that does
// not just blindly go thrue ALL the possible combos of the potential values; ie
// there just HAS to be a way of focusing-in-on values that haven't been allowed
// yet (and/or quickly identifying useless aligned-sets in order to skip them
// completely) and thus get the performance of the complete/correct algorithm
// down within acceptable limits, so that we can actually use the damn thing
// instead of a nasty massively-lossy little hack like _2H.
//
// KRC 2020-03-01 pretty obviously the above paragraph is the RFC for anyLevel,
// which jumps to evaluate "values which haven't been allowed yet"; but the
// other idea: "quickly identifying useless aligned-sets (and common excluder
// cells)" requires ingeniation in your general direction now, but don't hold
// your bloody breath, obviously. filterTop1465 is my best attempt: a s__t hack
// that's unfit for publication. Sigh. But I feel it might be possible for a
// mathematician to universalise/formalise filters for aligned sets which cannot
// hint. There MUST be some rule for number of inter-cell-collisions and the
// number of common excluder hits.
// =============================================================================

		n0 = numCandidates - 4;
		n1 = numCandidates - 3;
		n2 = numCandidates - 2;
		n3 = numCandidates - 1;

		// foreach candidate cell, except the last 4, obviously.
		for ( i0=0; i0<n0; ++i0 ) {
			// Normally we do the next two steps in one, for efficiency.
			// I've broken it down here just to explain it more clearly.
			// So, the first cell in our aligned set is the candidate at i0.
			cells[0]=candidates[i0];
			// get an index of cells[0]'s excluders (qualified siblings) set.
			idx0 = excluders[cells[0].i].idx();
			// when hitMe is true we skip until we find the cell which hit last
			// time. Pretty obviously, this an O(n) way of performing an O(1)
			// operation (ie Grid.get(String id)), so you shan't see its ilk in
			// competently written code; but can you (given that this bloody big
			// slab of code is stuck being a bloody big slab of code, for speed,
			// obviously) do it in an O(1) way that also works when hitMe is
			// false? Yeah, I thought not. So it's O(n) to the races. Yes she's
			// ugly, but she gives fantasic ____jobs, so I pay the rent. Clear?
			if(hitMe && cells[0]!=hitCells[0]) continue;
//KRC#2020-06-30 09:50:00
			c0b = (c0=cells[0]).maybes.bits;

			for ( i1=i0+1; i1<n1; ++i1 ) {
				// get idx01 := an index of the cells common to c0 and c1; skip
				// if it's empty: ie all common siblings are inelligible.
				// nb: the difference between _1C and _2H is this line:
				// "1 correct" requires one common excluder cell around which
				//   we align a set of cells; whereas
				// "2 hacked" requires at least two cells coz 99% of sets have
				//   1 common excluder, but about a third of hints are from
				//   aligned sets with two-or-more common excluder cells: so
				//   _2H is hacked to find about a third of the hints in about
				//   a tenth of the time. It's all in the wriste, eh Warnie?
				if ( excluders[(cells[1]=candidates[i1]).i].idx1(idx01, idx0) )
					continue;
				if(hitMe && cells[1]!=hitCells[1]) continue;
//KRC#2020-06-30 09:50:00
				c1b = (c1=cells[1]).maybes.bits;
				ns10 = c1.notSees[c0.i];

				for ( i2=i1+1; i2<n2; ++i2 ) {
					if ( excluders[(cells[2]=candidates[i2]).i].idx1(idx02, idx01) )
						continue;
					if(hitMe && cells[2]!=hitCells[2]) continue;
//KRC#2020-06-30 09:50:00
					c2b = (c2=cells[2]).maybes.bits;
					ns20 = c2.notSees[c0.i];
					ns21 = c2.notSees[c1.i];

					for ( i3=i2+1; i3<n3; ++i3 ) {
						if ( excluders[candidates[i3].i].idx1(idx03, idx02) )
							continue;
						cells[3] = candidates[i3];
						if(hitMe && cells[3]!=hitCells[3]) continue;
						if ( isInterrupted() )
							return false;
//KRC#2020-06-30 09:50:00
						c3b = (c3=cells[3]).maybes.bits;
						ns30 = c3.notSees[c0.i];
						ns31 = c3.notSees[c1.i];
						ns32 = c3.notSees[c2.i];

						for ( i4=i3+1; i4<numCandidates; ++i4 ) {
							if ( excluders[candidates[i4].i].idx1(idx04, idx03) )
								continue;
							cells[4] = candidates[i4];
							if(hitMe && cells[4]!=hitCells[4]) continue;

							if ( nonHinters.skip(cells) )
								continue;

							// filter top1465 when !isHacky rather than wait 34 hours again.
							// nb: magic numbers are from a6e.log via tools.AnalyseLog
							if ( filterTop1465 ) {
								// filter by total number of maybes shared by siblings
								col = countCollisions(cells); // countCollisions
//								colCnt.count(col);
								// colCnt min=2/0 max=21/53 pass 774,930,334 of 804,487,379 skip 29,557,045 = 3.67%
								if(col<2 || col>21) continue;
//								++colCnt.pass;

								// filter by total cells.maybes.size
								mbs = totalMaybesSize; // from countCollisions
//								mbsCnt.count(mbs);
								// mbsCnt min=10/10 max=19/31 pass 654,947,227 of 774,930,334 skip 119,983,107 = 15.48%
								if(mbs<10 || mbs>19) continue;
//								++mbsCnt.pass;

								// filter by total number of sibling relationships (edges)
								sib = siblingsCount; // from countCollisions
//								sibCnt.count(sib);
								// sibCnt min=4/2 max=8/10 pass 434,909,828 of 654,947,227 skip 220,037,399 = 33.60%
								if(sib<4 || sib>8) continue;
//								++sibCnt.pass;

								// col+sib is filtered in the switch
								cs = col + sib;

								// filter by maximum number of "large" cells
								// @maybe: filter by minimum number of "small" cells
//								++maxMbs.cnt;
								fives = 0;
								for ( Cell cell : cells )
									if ( cell.maybes.size >= 5 )
										++fives; //fallout
								// maxMbs pass 434,614,570 of 434,909,828 skip 295,258 = 0.07%
								if(fives>2) continue;
//								++maxMbs.pass;
							}

							// =============================================================================
							// the cells array now contains a set of 5 cells that are "aligned" around 1-or-
							// more "common excluder cell/s" (which are contained in idx04). And if useHits
							// then the probilities are it'll even produce a bloody hint!

							// read common excluder cells from grid at idx04
							if ( (numCmnExcls = idx04.cellsN(grid, cmnExcls)) == 1 ) {
								cmnExclBits[0] = cmnExcls[0].maybes.bits;
								numCmnExclBits = 1;
							} else {
								// performance enhancement: examine smaller maybes sooner.
								//KRC#2020-06-30 09:50:00 bubbleSort 84.525s vs Tim.small 108.438s
								//MyTimSort.small(cmnExcls, numCmnExcls, Grid.BY_MAYBES_SIZE);
								bubbleSort(cmnExcls, numCmnExcls);

								// get common excluders bits, removing any supersets,
								// eg: {12,125}=>{12} coz 125 covers 12, so 125 does nada.
								numCmnExclBits = subsets(cmnExclBits, cmnExcls, numCmnExcls);
							}
							// =============================================================================
							// remove each common excluder bits which contains a value that is not in any
							// candidates maybes, because no combo can cover it, so it won't contribute to
							// an exclusion. eg: ({1236,1367}, 12,24,14,25,126,456,456) => skip coz no 3
							if ( (numCmnExclBits = disdisjunct(cmnExclBits, numCmnExclBits
									, allMaybesBits(cells))) < 1 )
								continue;

							// filter top1465 when !isHacky rather than wait 34 hours again.
							// nb: magic numbers are from a5e.log via tools.AnalyseLog
							if ( filterTop1465 ) {
								hit = countHits(cmnExclBits, numCmnExclBits, cells);
//								hitCnt.count(hit); // relies on sum
								// hitCnt min=2/2 max=31/66 pass 353,929,740 of 354,064,577 skip 134,837 = 0.04%
								if(hit<2 || hit>31) continue;
//								++hitCnt.pass;

								sum = col + hit;
//								sumCnt.count(sum);
								// sumCnt min=6/4 max=37/52 pass 348,768,042 of 353,929,740 skip 5,161,698 = 1.46%
								if(sum<6 || sum>37) continue;
//								++sumCnt.pass;

								try {
									prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
//									prangCnt.count(prang);
									// prangRate min=1.752/1.714 max=8.251/14.250 pass 331,169,198 of 348,768,042 skip 17,598,844 = 5.05%
									if(prang<1.752D || prang>8.251D) continue;
//									++prangCnt.pass;
								} catch (ArithmeticException eaten) {
									// division by zero only happens when not filtering by col and hit!
//									prangCnt.count(0.0D);
//									++prangErrors;
//									++prangCnt.pass;
									continue; // we never find a hint (in A5E) when either is zero
								}
							}

//KRC#2020-06-30 09:50:00
// Q: is sorting cells actually any faster?
// with sort  :    139,983,396,200	   4540	    30,833,347	     78	 1,794,658,925	Aligned Pent
// w/o sort   :     86,503,199,100	   4540	    19,053,568	     78	 1,109,015,373	Aligned Pent
// Bubble-Sort:    101,520,487,700	   4540	    22,361,340	     78	 1,301,544,714	Aligned Pent
// w/o sort is complete. I comment-out scells and ALL associated code and move
// the setting of: c0, c0b, ns* to the outer loops.
// A: Without sort is the fastest. Then Bubble-Sort, then MyTimSort.small, so
// I am going to go with no sort at all, and will test this theory again in A4E
// and A6E to see if the theory holds true. If it does then A4E, A3E, and A2E
// will not sort, and A6E+ (or possibly A7+E) will sort, against my desire for
// homogenous A*E's. One must do what one must do. And it's all so slow that
// nobody is going to actually USE it anyway. Sigh.
//							// =============================================================================
//							// the dog____ing algorithm should be faster with dodgem-cars to the left, but
//							// the above for-i-loops need a static cells array; so we copy cells to scells
//							// (sortedCells) and sort that array DESCENDING by:
//							//     4*maybesCollisions + 2*cmnExclHits + maybes.size
//							cc.set(cells, cmnExclBits, numCmnExclBits);
//							System.arraycopy(cells, 0, scells, 0, degree);
//							//MyTimSort.small(scells, degree, cc);
//							bubbleSort(scells, degree, cc);
//
//							// cache the sorted cells and there maybes.bits
//							c0b = (c0=scells[0]).maybes.bits;
//							c1b = (c1=scells[1]).maybes.bits;
//							c2b = (c2=scells[2]).maybes.bits;
//							c3b = (c3=scells[3]).maybes.bits;
//							c4b = (c4=scells[4]).maybes.bits;
//
//							// build the NOT sibling cache
//							ns10 = c1.notSees[c0.i];
//
//							ns20 = c2.notSees[c0.i];
//							ns21 = c2.notSees[c1.i];
//
//							ns30 = c3.notSees[c0.i];
//							ns31 = c3.notSees[c1.i];
//							ns32 = c3.notSees[c2.i];

							c4b = (c4=cells[4]).maybes.bits;
							ns40 = c4.notSees[c0.i];
							ns41 = c4.notSees[c1.i];
							ns42 = c4.notSees[c2.i];
							ns43 = c4.notSees[c3.i];

							// clear allowedValuesBitsets array.
							avb0=avb1=avb2=avb3=avb4 = 0;

							// =============================================================================
							// anyLevel controls which for-i-loop to break-out-of when all maybes of all
							// cells to my right are already allowed; so that the FIRST allowed combo sets
							// my any* and we skip all other "like" combos. This is for performance only.
							// The algorithm works without it.
							// We don't waste time allowing multiple combos whose right-hand-side-values are
							// all already allowed; which only works from right-to-left, so if the right-
							// most cell is an excluder (has a value that is never allowed) then it never
							// kicks-in, so we process the whole shebang as if there was no anyLevel.
							anyLevel = degree; // meaning that avb4 != c4b

							// should we test if 3/4 values cover ceb0
							ces0 = SIZE[ceb0 = cmnExclBits[0]];
							do30 = ces0 <= 3;
							do40 = ces0 <= 4;

							// =============================================================================
							// NB: This switch is for performance only, the default path works for any
							// numCmnExclBits, but it calls a method (covers), which is slower than doing it
							// all in-line. It matters because we get here quite often: over half-a-billion
							// times. It matters even more in A7E which does 1.2 BILLION sets in top1465.
							//
							// Even creating a stackframe is profligate waste in a tight-loop, especially in
							// Java for some reason I don't fully understand, so don't listen to me, I don't
							// really know what I'm talking about, but please do go find-out from someone
							// who does; which ain't me, obviously. I just use the fooker, so I do what
							// works, and avoid what doesn't. All those possibilities of wherefores on the
							// long drop remain a blank map to me, even (especially) the probable looking
							// ones with the large tracts of land. What I mean is I'm too stupid to figure
							// it out, and too lazy to go find out, so I'm just flying around with this pig.
							// There IS no rabbit Kermit ya ___knuckle. Piggy ate it! Thus we see the issue
							// with living next door to the English: the fat pig ate my Green Man. Have you
							// met Wodderwick? Haaa yaaa! In Scottish: Ya'll figure it owt, or no.
							//
							// It's a ____-of-a-lot-of extra code for not very much saving, so cutting it is
							// reasonable; but I like it Fast, as apposed to merely fast, or the fully ASM
							// (heaven forbid) FAST! So I'm Javanic and I keep her, if you get my drift.
							// WARN: BEFORE you get rid of it, quantify "not very much" by: comment-out the
							// switch in A10E_1C, turn off IS_HACKY, run LogicalSolverTester; and then run-
							// it-again with the switch restored. The differences in the times will be "not
							// very much", so decide you're keeping it, and don't forget restore IS_HACKY.
							// WARN: on that WARN: This process will only take two days. Restore IS_HACKY.
							// WARN: on that WARN: on that WARN: Coz tree times is Irish. Restore IS_HACKY!
++counts[0];
++counts[numCmnExclBits];
							switch ( numCmnExclBits ) {
							case 1:
								// we don't need a common excluders loop
								//  w/o HitSet: 1 = 628,601,986 of 661,506,417 = 95.03%
								// with HitSet: 1 = 827 of 1,192 = 69.38%

								// =============================================================================
								// The dog ____ing loop works out if any maybes are excluded: ie all possible
								// combinations of the potential values of the cells in this aligned set do not
								// include certain value/s in certain position/s; by recording each value of
								// each allowed combo in its position in the allowedValuesBitsets (avb) array.
								// After the DOG_1 loop we check if any potential values have not been allowed
								// (ie are excluded), and if so we build the excludedCombosMap and the hint.
								// It's called the DOG wording loop because it's slow.
								// There are 2 exclusion rules:
								// (1) Hidden Single: Using the same value for two cells is allowed only if the
								//     cells do not share a region.
								// (2) common excluder rule: If the combo covers (is a superset of) any
								//     common excluder cells maybes then the combo is not allowed, because the
								//     excluder cell must be one of those values (no matter which).
								// nb: it's faster to "pre-eliminate" ie remove maybes from sibling cells for
								// large (4+) sets than "skip collisions" as in A23E; because we do more of the
								// work less often. Contrast DOG_1 with A3E's DOG_1 to see what I'm on about.
								//
								// -----------------------------------------------------------------------------
								// PSEUDO-CODE
								// -----------------------------------------------------------------------------
								//
								// DOG_1 is now so confundussed that I translated it back into psuedo-code so
								// another human being can follow it. This algorithm is my original simpler one.
								// There's another block comment below regarding anyLevel. Basically it's the
								// same, except we skip some s__t to spend less time examining sub-combos that
								// have all-already-all-been-allowed; and we do that from right-to-left.
								//
								// LETTUCE PRESUME: The Cabbage is in the mail:
								// * The $degree (5) cells c0, c1, c2, c3, and c4 are pre-selected.
								// * The array of common excluder cells (and cmnExclBits) is set-up.
								// * ceb0 := the single (in case 1:) Common Excluder cell's maybes.bits
								//   ie: the potential values of the remaining common excluder cell as a bitset.
								//   ie: cmnExclBits[0] noting that one-or-more cmnExclBits may've been removed
								//       by the subsets or the disdisjunct method.
								//
								// DA PSEUDO-CODEZ:
								// // we iterate Values.SHIFTED[c0.maybes.bits] coz it is faster to use an
								// // array iterator than the old-school method. See Values.SHIFTED for more.
								// foreach v0 in c0's potential values
								//   // in large sets (A5..10E) it's faster to remove each selected value
								//   // from each subsequent sibling cells maybes that it is to just "skip
								//   // collisions" (as per A23E) because we do more of the work less often.
								//   // nb: These are done upside-down to increase the CPU-caches hit rate. // I just thought it might be fun to put a comment on a comment in a comment. I'm nearly smiling. Fish? What fish? ExcessiveCabbagesException: pull my finger to continue... but but but. Your legs off! No it isn't! There's an earwick in my salad. STOP SKITING! Animals or edibles? And we spend HUNDREDS OF MILLIONS on bloody fish farms, which KILL MORE FISH THAN THEY PRODUCE! Darwin says <BUP_BOW/>. Maslow was a ____wit! American's are all ____wits (apart from those few who aren't, obviously)! The Australian Pariament are DEFINATELY ____wits (all of them, no exceptions)! Trump is the referential architype of ____wit (Trump: see ____wit. Yep, it fits)! Morrison is an imitation, pineapple smuggling, dingle-berry flavoured ____wit. With cheese. Fry my Burgers would be much better off if he shut up a stopped proving beyond any reasonable doubt how incredibly ____ing ____witted he is. Captain ____ing Potato Head s__ts me to tears at 60 miles per second per second. I know right: 0 to puking pastille in 3.14 milliseconds. Freaky! The ____ing Sports minister stole TWO HUNDRED MILLION DOLLARS which she stuffs into her mates piggy banks in the vain hope that someone might actually like her, but who's ____ing stupid enough to STEAL HUNDREDS OF MILLIONS OF FUYCKING DOLLARS and actually think that people will LIKE her for it; she can't bloody figure out WHY nobody ____ing likes her. Sersiously, this level of stupididity stretches the the third law of thermodynamics and subsequently societies whose government persist with it fail (utterly, in the Darwinian sense, seriously peeps). And the ____ers just keep getting away with it: just sacrifice another stupid ____ing Nat bitch to the reasonableness-gods (good call, poor execution, if you get my drift). Government by half-wit, con-artist, thug, rip-off, psychopath, ____ing ____wit sheisters. Woohoo Loo the minister for ____ing Dog Bong Wok. The whole ____ing tribe are first class channel changers: ie when any of them come on I just change the ____ing channel: don't expect me to forgive you while you're p__sing in my ____ing ear you useless little c__ts. Well maybe it's you who's the ____wit? Yeah maybe, but I doubt it. Sigh. And they're all significantly superiour to Pauline. Dog s__t recently outscored Pauline on a standard aptitude test. As did a crow! FAAAARK! FAAAARK! FAAAARK! Don't ask the ____ing crows! It's all nuts!
								//   if c4 isSiblingOf c0 then remove v0 from c4's potential values (if present)
								//   if c3 isSiblingOf c0 then remove v0 from c3's potential values (if present)
								//   if c2 isSiblingOf c0 then remove v0 from c2's potential values (if present)
								//   if c1 isSiblingOf c0 then remove v0 from c1's potential values (if present)
								//   foreach v1 in c1's remaining potential values
								//     // nb: Turns-out it's actually faster to NOT test if ceb0 is a subset of
								//     //     {v0,v1} or removing v1 from c2's potential values empties the
								//     //     bitset because the hit-rate with only 2 values is so low that it
								//     //     ends-up costing us more to test for it than the "rare" skips save
								//     //     us, so it's faster to do nothing. I'm a lazy assed kisser.
								//     if c4 isSiblingOf c1 then rmv v1 from c4's potential values (if present)
								//     if c3 isSiblingOf c1 then rmv v1 from c3's potential values (if present)
								//     foreach v2 in c2's remaining potential values
								//       // We test ceb0 directly coz it's faster to not call the covers method
								//       // (or indeed ANY method in a "tight loop", butThatCanBeImpossible(tm))
								//       // Pretty obviously, adding more combo-values increases the hit-rate;
								//		 // my rule-of-thumb: hit rate of 1.5% pays its way, and 3% pays back;
								//       // so below 1.5% drop it; 1.5-3% guess; 3+% promulgate it.
								//       // nb: Turns-out it's faster to test with 3+ values in the-combo-so-far
								//       if ceb0 is a subset of {v0,v1,v2}
								//           or removing v2 from c3's potential values empties the bitset
								//         then skip this v2
								//       fi
								//       if c4 isSiblingOf c2 then rmv v2 from c4's potential values (if there)
								//       foreach v3 in c3's remaining potential values
								//         if ceb0 is a subset of {v0,v1,v2,v3}
								//             or removing v3 from c4's potential values empties the bitset
								//           then skip this v3
								//         fi
								//         foreach v4 in c4's remaining potential values
								//           skip this v4 if ceb0 is a subset of {v0,v1,v2,v3,v4}
								//           Combo Allowed! so add each v* to it's allowedValuesBitset
								//                          (avb0|=sv0; etc, etc)
								//         next v4
								//       next v3
								//     next v2
								//   next v1
								// next v0
								// if all potential values are NOT allowed
								//   then we found a hint! ie value/s is/are excluded!
								//   so we build the ExcludedCombosMap and the hint.
								// fi
								// mischief-managed
								//
								// nb: I've heavily commented DOG_1A to help you understand it and all the
								// following (A5+E) dog ____ers. Good luck with it. I barely understand the
								// bastard, and I wrote it!
								// =============================================================================

//AVG_1of3: System.out.format("%6d calculated %,5d", cnt, c0.maybes.size*c1.maybes.size*c2.maybes.size*c3.maybes.size*c4.maybes.size);
//long setCount = 0L;

								// filter top1465 even when !isHacky, to not take 34 hours again.
								// nb: the magic numbers are from a8e.log via tools.AnalyseLog
								// This is a hack on a hack on a hack. Don't try this at home.
								if ( filterTop1465 ) {
//useless:							cnt1col.count(col);
//									// cnt1col min=2/2 max=21/21 pass 309,714,381
//									if(col<? || col>?) continue;
//									++cnt1col.pass;

//useless: 							cnt1sib.count(sib);
//									// cnt1sib min=4/4 max=8/8 pass 309,714,381
//									if(sib<? || sib>?) continue;
//									++cnt1sib.pass;

//useless: 							cnt1cs.count(cs);
//									// cnt1cs min=6/6 max=29/29 pass 309,714,381
//									if(cs<? || cs>?) continue;
//									++cnt1cs.pass;

//useless:							cnt1mbs.count(mbs);
//									// cnt1mbs min=10/10 max=19/19 pass 309,714,381
//									if(mbs<? || mbs>?) continue;
//									++cnt1mbs.pass;

//useless: 							ces = ces0;
//									cnt1ces.count(ces);
//									// cnt1ces min=2/2 max=5/5 pass 309,714,381
//									if(ces>?) continue;
//									++cnt1ces.pass;

//									cnt1hit.count(hit);
									// cnt1hit min=2/2 max=16/19 pass 309,714,381
									if(hit>16) continue;
//									++cnt1hit.pass;

//									cnt1sum.count(sum);
									// cnt1sum min=6/6 max=36/37 pass 309,714,381
									if(sum>36) continue;
//									++cnt1sum.pass;

//									cnt1prg.count(prang);
									// cnt1prg min=1.912/1.762 max=8.251/8.250 pass 309,714,381
									if(prang<1.912) continue;
//									++cnt1prg.pass;
								}

								// =============================================================================
								// foreach of c0's potential values.
								// nb: SVS is a local reference (for speed) to Values.SHIFTED, an array of
								// jagged-arrays. The first index is "maybes.bits", so SVS[c0b] gives us an
								// array of shifted-values that've been "unpacked" from the bitset c0b.
								// I do this coz an array-iterator is about 25% faster than iterating all
								// possible shifted-values skipping unset bits, ie the old-shool method:
								//   DOG_1a: for ( int sv0=1; sv0<=c0b; sv0<<=1 ) { // all possible
								//       if((sv0 & c0b)==0) continue; // skip unset bits
								// see Values.SHIFTED for a more complete explanation.
								DOG_1a: for ( int sv0 : SVS[c0b] ) { // shiftedValue0 : Values.SHIFTED[c0b]
									// =============================================================================
									// (1) hidden single rule: pre-elimination: if c1 isSiblingOf c0 then remove v0
									// from c1's maybes (if present); and likewise for each cell in the aligned set.
									// We do c432b0 upside-down to hopefully keep the last one the CPU-cache. Also
									// we don't need (or have) a c1b0 variable, we just do the calculation directly
									// in the SV1_LOOP line. Same for c2b1 in SV2_LOOP.
									// IN ENGLISH: cell4.maybes.bits.version0 = cell0.isNotSiblingOf[cell4.index]    ?    cell4.maybes.bits  :    cell4.maybes.bits & ~     shiftedValueOfCell0
									// ie:     set cell4.maybes.bits.version0 = if cell0.isNotSiblingOf[cell4.index] then cell4.maybes.bits, else cell4.maybes.bits without the current valueOfCell0
									c4b0 = ns40 ? c4b : c4b & ~sv0;
									c3b0 = ns30 ? c3b : c3b & ~sv0;
									c2b0 = ns20 ? c2b : c2b & ~sv0;
									// =============================================================================
									// any1: is any-combo-in-the-sv1-loop-allowed? If any combo in the SV1_LOOP is
									// allowed we "avb0|=sv0", ie this sv0 is allowed. It's just faster to do it
									// this way, especially in the larger (A7+E) A*E's. When each combo is allowed
									// the naive:
									//   "avb0|=sv0; avb1|=sv1; avb2|=sv2; avb3|=sv3; avb4|=sv4;"
									// is algorithmically correct, but it works out slower because it does more of
									// the work more often, especially as the number of cells in each aligned set
									// increases. Here in A5E its not much faster to do it suavely, but I'm trying
									// to keep all A*E's as similar as possible, so I've done it this way in A4+E.
									any1 = false;
									// foreach of c1's potential values
									SV1_LOOP: for ( int sv1 : SVS[ns10 ? c1b : c1b & ~sv0] ) { // ie c1b0
										// =============================================================================
										// It's faster to NOT test if sv01 covers ceb0, which can only happen when
										// size[ceb0]==2, which is pretty rare.
										// It's also faster to NOT test if c2/3/4b1 is empty. c2b1 can only be empty
										// when c2.maybes.size==2, which is rare, and it only fires when sv01 ==
										// c2.maybes.bits, or 25% of the time at best. Running around to avoid kissing
										// 25% of chickens-lips is pointless: it costs more than it saves.
										// nb: Iterating an empty array no-longer appears "dead slow": real or just
										// perception? Probably just perception. Everything else is so expensive the
										// expensive bits have become invisible. Forest. Trees. Axe. Darwin. Irony.
										sv01 = sv0 | sv1;
										c4b1 = ns41 ? c4b0 : c4b0 & ~sv1;
										c3b1 = ns31 ? c3b0 : c3b0 & ~sv1;
										any2 = false;
										SV2_LOOP: for ( int sv2 : SVS[ns21 ? c2b0 : c2b0 & ~sv1] ) { // ie c2b1
											// =============================================================================
											// (2) common excluder rule: if the combo-so-far is already a superset of
											// any common excluders maybe cells then NO combo containing these values can be
											// allowed, so we skip all such combos with one continue statement. Cool!
											//
											// nb: above "c2b1" will be 0 rarely. No biggy. Just iterate the empty array coz
											// averting it costs more than it saves; whereas "c3b2" will be 0 often enough
											// for benefits to outweigh the cost of testing for empty, but we test it ONCE
											// before it's used, not repeatedly. This differs in A7+Es coz it's worth empty-
											// testing when you remove the fifth+ value from each subsequent cells maybes.
											// nb: on that nb: the fifth may be borderline, but definately the sixth value.
											//
											// nb: "covers common-excluder" test is more deterministic than "next-maybes
											// isEmpty" test, so we do ceb's first.
											//
											// CODE    sv02              = sv01                             |          sv01;
											// VERBOSE shiftedValues0to2 = shiftedValue01                   bitwise-or shiftedValue2;
											// PLAIN   the current value of c0 PLUS the current value of c1 PLUS       the current value of c2
											//
											// CODE    if ( (do30                                     &&                           (ceb0                                             &           ~                       sv02)             == 0)
											// VERBOSE if ( (size[ceb0]<=3                            short-circuiting-boolean-and theOnlyCommonExcluderCells.maybes.bits            bitwise-and the-bitwise-negation-of shiftedValues0to2 == 0
											// PLAIN   if ( first(shortest)CommonExcluderHas<=3Maybes and                          the single common excluder cells potential values without                             the-combo-so-far  is an empty-bitset
											//
											// CODE    || (c3b2            =    ns32                                           ?    c3b1             :    c3b1            &           ~                       sv2               ) == 0 )
											// ENGLISH or (c3.maybesBitsV2 = if c3.isNotSiblingOf[c2.i]                        then c3.maybesBitsV1, else c3.maybesBitsV1 bitwise-and the-bitwise-negation-of shiftedValue2     ) == 0 )
											// ie      or (c3.maybesBitsV2 = if c3 is NOT in the same {box, row, or col} as c2 then c3.maybesBitsV1, else c3.maybesBitsV1 without                             c2's current value) is an empty bitset )
											// ERGO: You can see why mathematicians like short names. E=mC^2 hides a porcupine to the echidna... goes off in all directions.

// Q: is using do30 and/or do40 actually any faster?
// A: Yes, using both is fastest. From 23:35 down to 17:48 = 5:47 or 24.52% faster
//            time (ns)  calls   time/call  elims    time/elim  hinter
//none: 485,794,499,800  14729  32,982,178   1484  327,354,784  Aligned Pent
//do30: 461,109,972,700  14729  31,306,264   1484  310,721,005  Aligned Pent
//do40: 389,309,286,200  14729  26,431,481   1484  262,337,793  Aligned Pent
//both: 374,882,166,100  14729  25,451,976   1484  252,616,014  Aligned Pent
//
//none: 1465  1,415,169,301,400 (23:35)  965,985,871 //forgot to comment out ces0,do30,do40 setup
//do30: 1465  1,348,094,853,000 (22:28)  920,201,264
//do40: 1465  1,107,733,871,000 (18:27)  756,132,335
//both: 1465  1,068,570,872,200 (17:48)  729,399,912 //which is 3:33 faster than the last run with this configuration, which shows the problem with trying to performance tune a Java application! Runtimes are seriously bloody inconsistent!
//
// Q: is empty-testing c3b2 (or c4b3) any faster?
// A: Yes, using both is fastest. From 23:49 down to 17:48 = 6:01 or 25.26% faster
// nb: all run with both do30 and do40
// with c3b2 && c4b3: 374,882,166,100  14729  25,451,976  1484  252,616,014 Aligned Pent
//  w/o c3b2        : 444,438,498,100  14729  30,174,383  1484  299,486,858 Aligned Pent
//  w/o c3b2 or c4b3: 501,034,084,400  14729  34,016,843  1484  337,624,046 Aligned Pent
// separate ifs     : 469,638,159,900  14729  31,885,271  1484  316,467,762 Aligned Pent
// with c3b2 && c4b3: 380,136,137,800  14729  25,808,686  1484  256,156,427 Aligned Pent //again
// promulgate       : 401,155,934,200  14729  27,235,788  1484  270,320,710 Aligned Pent
//
// with c3b2 && c4b3: 1465  1,068,570,872,200 (17:48)  729,399,912
//  w/o c3b2        : 1465  1,261,383,126,900 (21:01)  861,012,373
//  w/o c3b2 or c4b3: 1465  1,429,640,608,400 (23:49)  975,863,896
// separate ifs     : 1465  1,336,855,121,600 (22:16)  912,529,093
// with c3b2 && c4b3: 1465  1,074,692,403,900 (17:54)  733,578,432 //again
// promulgate       : 1465  1,116,126,419,700 (18:36)  761,861,037

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
//AVG_2of3: ++allowedCount;
													// Combo Allowed!
													any4 = true;
													// add v4 to c4's allowedValuesBitset;
													// and skip if all potential values of c4 are NOT yet allowed.
													if ( (avb4|=sv4) != c4b )
														continue;

													// =============================================================================
													// RE: anyLevel: Houston, we have a sort-of Solution.
													//
													// anyLevel is the level in this mess of loops (from inner 5 to outer 1)
													// where "any*" (and all to my right) is true.
													//
													// I (you) am the anyLevel. Be the anyLevel... Humina, humina, humina.
													//
													// When all values of all cells to my right have already been allowed then we
													// shall examine only the FIRST allowed combo at my level; and move straight-on
													// to the next value at my level, because it hasn't yet been evaluated; instead
													// of wasting time evaluating multiple-combos-to-my-right which can not allow
													// any additional cells-potential-values because they're all already allowed!
													//
													// This is faster, reducing: O(n*n*n*n*n) to best case O(n) and worste case
													// O(n*n*n*n*n) which is no worse than the original algorithm. So winners break-
													// out-as-far-as-possible (depending on anyLevel) and losers keep on trucking.
													//
													// The net-result is something like (n*n*n'ish), but I don't know exactly coz it
													// can't be calculated (AFAIK) only experienced so you uncomment the AVG_* hacks
													// and set IS_HACKY=false then run the GUI, do first A5E_1C of 1#top1465.d5.mt
													// and copy-paste the output into "tmp.txt", then run diuf.sudoku.tools.Average:
													//     Average calculated 433.12 experienced 19.52
													// Where "calculated" is the average n*n*n*n*n of the actual maybes.size's.
													//       "experienced" is average number of times we hit "Combo Allowed" line.
													// So DOG_1A in A5E_1C operated at O(square root of n*n*n*n*n) in this case, but
													// your mileage may vary significantly, it is dependant on your puzzle/s, but it
													// can't be much worse than the original (ie horrible); the only overhead is the
													// fast and simple "if(avb4!=c4b)" test, which is executed:
													//      with anyLevel  3,120,887,320 "Combo Allowed"s in 06:03
													//   without anyLevel 25,431,959,483 "Combo Allowed"s in 11:27
													// Yep, that's 25 BILLION with a B; and this is only A5E. In A10E_1C we weweased
													// Wodderwick, with 3 F's and a silent Q! So yeah, anyLevel works... sort of.
													switch ( anyLevel ) {
													case 5: // ie my degree
														// nb: any4 is already true
														// set anyLevel=4 to say avb4==c4b, ie all of c4's maybes are
														// allowed, which is used in the following "if(any4)" block.
														anyLevel = 4;
														break SV4_LOOP;
													case 4:
														// nb: any4 is already true, and anyLevel is already 4
														break SV4_LOOP;
													case 3: any3 = true;  break SV3_LOOP;
													case 2: any2 = true;  break SV2_LOOP;
													case 1: any1 = true;  break SV1_LOOP;
													} // end-switch
												} // next sv4
												// =============================================================================
												// RE: anyLevel again: Houston, We still have a Solution... sort of...
												//
												// We add sv3 to avb3 and if that means that all of c3's values are allowed &&
												// anyLevel==4 then we set anyLevel=3, so that next time we allow a combo we:
												// * break the SV3_LOOP (the next one out) to jump straight to the line
												//         "if(any3)" until all of c2's potential values are allowed; then
												//   set anyLevel=2 so that next time we allow a combo we break SV2_LOOP so:
												// * we do "if(any2)" until all c1's are allowed; then set anyLevel=1 so:
												// * we do "if(any1)"; and then we're all done.
												// Our hint-rate is tiny: 874/391,316,169*100 = 0.000223% or about 1 in 5000,
												// so it's really important to fail as quickly as possible, and anyLevel is the
												// best way I can think-of to do that. Suggestions welcome.
												if ( any4 ) {
													any3 = true;
													// =============================================================================
													// nb: the "&& anyLevel==4" test just stops us going off prematurely, before
													//     avb4==c4b which we could test direct, but this is consistent with the
													//     other anyLevels, where "avb3==c3b && avb4==c4b" is compressed into the
													//     single clause "anyLevel==3". Pretty obviously the larger the aligned-
													//     set the more clauses, so the larger the savings we gain from compres-
													//     sing all clauses into a single test, so this really pays in A7+E.
													//     It's in A456E mainly for consistency, where it's also a tad faster.
													//     See also A10E_1C (where it pays-back most) for why this is faster.
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
//AVG_3of3: System.out.format(" experienced %,5d\n", allowedCount);
								break;
							case 2:
								// we still don't need a common excluders loop
								//  w/o HitSet: 2 = 30,504,193 of 661,506,417 = 4.61%
								// with HitSet: 2 = 349 of 1,192 = 29.28%
								ces1 = SIZE[ceb1=cmnExclBits[1]];
								do31 = ces1 <= 3;
								do41 = ces1 <= 4;

								// filter top1465 even when !isHacky, to not take 34 hours again.
								// nb: the magic numbers are from a5e.log via tools.AnalyseLog
								if ( filterTop1465 ) {
//									cnt2col.count(col);
									// cnt2col min=2/2 max=17/21 pass 20,591,943
									if(col>17) continue;
//									++cnt2col.pass;

//useless: 							cnt2sib.count(sib);
//									// cnt2sib min=4/4 max=7/7 pass 20,591,943
//									if(sib<? || sib>?) continue;
//									++cnt2sib.pass;

//									cnt2cs.count(cs);
									// cnt2cs min=6/6 max=24/28 pass 20,591,943
									if(cs>24) continue;
//									++cnt2cs.pass;

//useless:							cnt2mbs.count(mbs);
//									// cnt2mbs min=10/10 max=19/19 pass 20,591,943
//									if(mbs<? || mbs>?) continue;
//									++cnt2mbs.pass;

//useless: 							ces = ces0;
//									cnt2ces.count(ces);
//									// cnt2ces min=2/2 max=5/5 pass 20,591,943
//									if(ces<? || ces>?) continue;
//									++cnt2ces.pass;

//									cnt2hit.count(hit);
									// cnt2hit min=4/4 max=29/31 pass 20,591,943
									if(hit>29) continue;
//									++cnt2hit.pass;

//useless: 							cnt2sum.count(sum);
//									// cnt2sum min=6/6 max=37/37 pass 20,591,943
//									if(sum<? || sum>?) continue;
//									++cnt2sum.pass;

//									cnt2prg.count(prang);
									// cnt2prg min=1.752/1.753 max=7.779/8.250 pass 20,591,943
									if(prang>7.779) continue;
//									++cnt2prg.pass;
								}
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
													// add to allowedValuesBitsets
													// and skip unless all c4's values are allowed.
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
								//  w/o HitSet: 3 = 2,313,827 of 661,506,417 = 0.35%
								// with HitSet: 3 = 14 of 1,192 = 1.17%
								ces1 = SIZE[ceb1=cmnExclBits[1]];
								do31 = ces1 <= 3;
								do41 = ces1 <= 4;
								ces2 = SIZE[ceb2=cmnExclBits[2]];
								do32 = ces2 <= 3;
								do42 = ces2 <= 4;

								// filter top1465 even when !isHacky, to not take 34 hours again.
								// nb: the magic numbers are from a8e.log via tools.AnalyseLog
								if ( filterTop1465 ) {
//									cnt3col.count(col);
									// cnt3col min=2/2 max=15/18 pass 855,817
									if(col>15) continue;
//									++cnt3col.pass;

//useless: 							cnt3sib.count(sib);
//									// cnt3sib min=4/4 max=6/6 pass 855,817
//									if(sib<? || sib>?) continue;
//									++cnt3sib.pass;

//									cnt3cs.count(cs);
									// cnt3cs min=6/6 max=21/24 pass 855,817
									if(cs>21) continue;
//									++cnt3cs.pass;

//									cnt3mbs.count(mbs);
									// cnt3mbs min=11/10 max=19/19 pass 855,817
									if(mbs<11) continue;
//									++cnt3mbs.pass;

//useless: 							ces = ces0;
//									cnt3ces.count(ces);
//									// cnt3ces min=2/2 max=5/5 pass 855,817
//									if(ces<? || ces>?) continue;
//									++cnt3ces.pass;

//									cnt3hit.count(hit);
									// cnt3hit min=7/6 max=31/31 pass 855,817
									if(hit<7) continue;
//									++cnt3hit.pass;

//									cnt3sum.count(sum);
									// cnt3sum min=9/8 max=37/37 pass 855,817
									if(sum<9) continue;
//									++cnt3sum.pass;

//									cnt3prg.count(prang);
									// cnt3prg min=2.055/1.875 max=8.251/8.250 pass 855,817
									if(prang<2.055) continue;
//									++cnt3prg.pass;
								}
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
													// add to allowedValuesBitsets
													// and skip unless all c4's values are allowed.
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
								//  w/o HitSet: 4 = 79,621 of 661,648,656 = 0.01% (hints 2)
								//  w/o HitSet: 5 = 5,162 of 661,648,656 = 0.00% (hints 0)
								//  w/o HitSet: 6 = 999 of 661,648,656 = 0.00% (hints 0)
								// with HitSet: 4 = 2 of 1,192 = 0.17%
								// with HitSet: 5 = 0 of 1,192 = 0.00%
								// with HitSet: 6 = 0 of 1,192 = 0.00%

								// top1465: there are 6,161 sets with 5 or 6 cmnExclBits producing
								// 0 hints, we so we skip the bloody lot.
								if ( isTop1465 && numCmnExclBits>4 )
									continue;

								// filter top1465 even when !isHacky, to not take 34 hours again.
								// nb: the magic numbers are from a8e.log via tools.AnalyseLog
								if ( filterTop1465 ) {
//									cnt4col.count(col);
//									if(col<? || col>?) continue;
//									++cnt4col.pass;

//									cnt4sib.count(sib);
//									if(sib<? || sib>?) continue;
//									++cnt4sib.pass;

//									cnt4cs.count(cs);
//									if(cs<? || cs>?) continue;
//									++cnt4cs.pass;

//useless:							cnt4mbs.count(mbs);
//									if(mbs<? || mbs>?) continue;
//									++cnt4mbs.pass;

//									ces = ces0;
//									cnt4ces.count(ces);
//									if(ces<? || ces>?) continue;
//									++cnt4ces.pass;

//									cnt4hit.count(hit);
//									if(hit<? || hit>?) continue;
//									++cnt4hit.pass;

//									cnt4sum.count(sum);
//									if(sum<? || sum>?) continue;
//									++cnt4sum.pass;

//									cnt4prg.count(prang);
//									if(prang<? || prang>?) continue;
//									++cnt4prg.pass;
								}
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
													// add to allowedValuesBitsets
													// and skip unless all c4's values are allowed.
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
							if ( isTop1465 ) {
								hitCounters();
								hits.add(gsl, hintNum, cells);
							}

							// create the removable (red) potential values.
//KRC#2020-06-30 09:50:00
//							Pots redPots = createRedPotentials(scells
//									, avb0,avb1,avb2,avb3,avb4);
							Pots redPots = createRedPotentials(cells
									, avb0,avb1,avb2,avb3,avb4);
							if ( redPots.isEmpty() )
								continue; // Should never happen. Never say never.
							// build the excluded combos map for the hint
							ExcludedCombosMap map = buildExcludedCombosMap(
									cmnExcls, numCmnExcls, cells, redPots);
							// create and add the hint
							AHint hint = new AlignedExclusionHint(this
									, redPots, cells, numCmnExcls, cmnExcls, map);

//							standardLog(myLog, cells, gsl, hintNum
//									, cmnExcls, numCmnExcls
//									, cmnExclBits, numCmnExclBits
//									, redPots, map, hint);

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

/*
wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair,
NakedTriple, HiddenTriple, Swampfish, Swordfish, XY_Wing, XYZ_Wing, W_Wing,
Skyscraper, TwoStringKite, EmptyRectangle, NakedQuad, Jellyfish, URT,
FinnedSwampfish, FinnedSwordfish, FinnedJellyfish, SashimiSwampfish,
SashimiSwordfish, SashimiJellyfish, ALS_XZ, ALS_XY_Wing, BUG, AlignedPair,
AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct,
AlignedNona, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus,
NestedUnary, NestedPlus
Sudoku Explainer 6.30.064 built 2020-04-28 07:51:04 ran 2020-04-28.12-21-41
mode    : ACCURACY !STATS !REDO !HACKY
input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
log 50  : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.2020-04-28.12-21-41.log
stdout  : progress only

Aligned5Exclusion_1C
HitSet.size = 263
							//  w/o HitSet: 1 = 194,211,904 of 207,712,049 = 93.50% (hints 204)
							//  w/o HitSet: 2 =  12,955,049 of 207,712,049 =  6.24% (hints 56)
							//  w/o HitSet: 3 =     540,983 of 207,712,049 =  0.26% (hints 2)
							//  w/o HitSet: 4 =       3,856 of 207,712,049 =  0.00% (hints 1)
							//  w/o HitSet: 5 =         249 of 207,712,049 =  0.00% (hints 0)
							//  w/o HitSet: 6 =           8 of 207,712,049 =  0.00% (hints 0)
// colCnt min=4/0 max=21/49 pass 479,126,287 of 495,933,101 skip 16,806,814 = 3.39%
// mbsCnt min=12/10 max=19/30 pass 408,226,934 of 479,126,287 skip 70,899,353 = 14.80%
// sibCnt min=4/2 max=8/10 pass 270,244,230 of 408,226,934 skip 137,982,704 = 33.80%
// maxMbs pass 270,090,251 of 270,244,230 skip 153,979 = 0.06%
// hitCnt min=3/2 max=29/66 pass 220,825,116 of 220,906,392 skip 81,276 = 0.04%
// sumCnt min=10/4 max=37/52 pass 218,114,621 of 220,825,116 skip 2,710,495 = 1.23%
// prangRate min=1.912/1.714 max=5.940/14.250 pass 207,712,049 of 218,114,621 skip 10,402,572 = 4.77%
// cnt1hit min=2/2 max=16/19 pass 193,921,605 of 194,211,904 skip 290,299 = 0.15%
// cnt1sum min=7/6 max=36/37 pass 193,918,921 of 193,921,605 skip 2,684 = 0.00%
// cnt1prg min=1.912/1.789 max=8.168/8.250 pass 193,918,665 of 193,918,921 skip 256 = 0.00%
// cnt2col min=2/2 max=17/21 pass 12,947,376 of 12,955,049 skip 7,673 = 0.06%
// cnt2cs min=6/6 max=24/24 pass 12,947,376 of 12,947,376 skip 0 = 0.00%
// cnt2hit min=5/4 max=29/31 pass 12,945,391 of 12,947,376 skip 1,985 = 0.02%
// cnt2prg min=1.896/1.753 max=7.779/8.250 pass 12,849,364 of 12,945,391 skip 96,027 = 0.74%
// cnt3col min=2/2 max=14/18 pass 540,740 of 540,983 skip 243 = 0.04%
// cnt3cs min=6/6 max=20/21 pass 540,740 of 540,740 skip 0 = 0.00%
// cnt3mbs min=11/10 max=19/19 pass 540,728 of 540,740 skip 12 = 0.00%
// cnt3hit min=7/6 max=31/31 pass 540,654 of 540,728 skip 74 = 0.01%
// cnt3sum min=11/9 max=37/37 pass 540,654 of 540,654 skip 0 = 0.00%
// cnt3prg min=2.028/1.897 max=7.934/8.250 pass 540,593 of 540,654 skip 61 = 0.01%
// cnt4col min=2/2 max=11/12 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4sib min=4/4 max=4/4 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4cs min=6/6 max=15/16 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4mbs min=10/10 max=19/19 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4ces min=2/2 max=3/3 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4hit min=11/10 max=31/31 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4sum min=13/12 max=37/37 pass 3,856 of 3,856 skip 0 = 0.00%
// cnt4prg min=2.342/2.220 max=7.683/8.250 pass 3,856 of 3,856 skip 0 = 0.00%
prangErrors = 0

*/