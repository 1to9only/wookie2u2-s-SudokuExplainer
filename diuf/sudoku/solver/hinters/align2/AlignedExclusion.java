/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.CELLS_MAYBES_VISITOR;
import diuf.sudoku.Pots;
import diuf.sudoku.Settings;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.ARRAYS;
import static diuf.sudoku.Values.SHIFTED;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.als.HintValidator;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.MyArrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;


/**
 * AlignedExclusion implements the aligned exclusion Sudoku solving technique
 * for sets of between 2 and 10 cells.
 * <p>
 * Aligned Exclusion eliminates potential values of "candidate cells" that are
 * grouped around the "excluder cell/s". The candidate cells form "the aligned
 * set" and the "excluders" are the cells they're aligned around.
 * <p>
 * There's only one way to do exclusion, and it's expensive. We must go through
 * all the possible combinations of the potential values of each aligned set to
 * see if each combo is allowed (rules below). If it is then we record each
 * cells presumed value in its cell (that's important); then we just check if
 * all potential cell values have been "allowed"; and if not then there is an
 * "exclusion", so we raise a hint. It sounds simple enough; but the actual
 * implementation is mega-confundussed by details, trying to make an inherently
 * combinatorial-times-combinatorial process (ie bloody slow) a bit faster; to
 * bring it up to "usable" speed.
 * <p>
 * There are two exclusion rules:<ul>
 * <li>Candidate cells which see each other cannot contain the same value; a
 *  concept which I call (rightly or wrongly) the Hidden Single rule. If a
 *  candidate cell has no remaining potential values because all of its
 *  potential values are already taken by preceeding cells (that it sees) in
 *  the aligned set; then the combo is not allowed; indeed all combos which
 *  contain these values in the "defined" cells are not allowed.
 * <li>Because all of the candidate cells see all of the excluder cell/s, we
 *  can "drop" each candidate cells presumed value from the potential values
 *  of all the excluder cell/s; and if that leaves no potential values for an
 *  excluder cell (which MUST have A value) then the combo is not allowed.
 * </ul>
 * <p>
 * I talked about "candidate" cells without defining them: A "candidate" is an
 * unset cell which sees the requisite number of excluder cells. An excluder
 * cell is one with 2..degree potential values. The "requisite number" of
 * excluder cells depends on the size of the aligned set, and also on the users
 * "hacked" setting. An aligned set of 2 or 3 cells can only produce hints when
 * those cells are aligned around atleast two excluder cells. Aligned sets of 4
 * to 10 cells CAN still produce a hint when aligned around a single cell, it's
 * just that a hint from a single excluder cell is much much rarer.
 * <p>
 * Aligned4Exclusion is always "correct": aligned around a single cell, because
 * it's still "fast enough" when done that way.
 * <p>
 * I call aligning around a single cell "correct" coz it finds all available
 * hints; and I call aligning 5 to 10 cells around two or more cells "hacked"
 * coz it's a hack. Sets aligned around multiple excluder cells are much much
 * more likely to produce a hint; so "hacked mode" processes roughly one tenth
 * of the aligned sets to produce about a third of the hints. This matters coz
 * AlignedExclusion is inherently a slow process; so most users simply will not
 * wait for the correct version of the algorithm to run over larger sets; but
 * they may wait for the hacked version of 5, 6, or even 7 cells; but 8, 9 and
 * 10 cells are still too slow, even hacked, in my humble opinion. Ready please
 * Mr Moore.
 * <p>
 * Note that this implementation of AlignedExclusion uses the "stack iteration"
 * technique (twice) that I have boosted from the ALS's in HoDoKu, by hobiwan.
 * HoDoKu is another open source Sudoku solver.
 * <p>
 * This implementation is more compact than the existing A*E classes, which are
 * a pile of boiler-plate code. This version is maintainable, but it's complex,
 * and it's slower. Existing Aligned5Exclusion runs in 70 seconds (1:10), and
 * this new version runs in 1,133 seconds (18:53).
 * <p>
 * <hr>
 * <pre>
 * 2020-12-14..16 speed comparison with the old Aligned2345Exclusion classes
 * -- old top1465.d5.2020-12-14.13-16-54.log --
 *       282,610,000  4426       63,852   1    282,610,000 Aligned Pair
 *     1,818,507,000  4425      410,962  16    113,656,687 Aligned Triple
 *    25,741,133,200  4410    5,836,991  52    495,021,792 Aligned Quad
 *    70,588,426,700  4368   16,160,354  68  1,038,065,098 Aligned Pent
 * -- new --
 *       306,873,400  4426       69,334   1    306,873,400 Aligned Pair
 *     2,787,755,200  4425      630,001  15    185,850,346 Aligned Triple
 *   170,660,319,800  4411   38,689,712  53  3,220,006,033 Aligned Quad
 * 1,133,832,664,700  4368  259,577,075  66 17,179,282,798 Aligned Pent
 * -- conclusion --
 * This new version is MUCH slower, so I'm going to stick with the original,
 * and retain this new version just for interest sake, coz it's TOO SLOW!
 *
 * -- Excluderator interface --
 *       319,455,100  4420      72,274   1    319,455,100 Aligned Pair
 *     3,019,857,900  4419     683,380  15    201,323,860 Aligned Triple
 *   191,564,119,600  4405  43,487,881  54  3,547,483,696 Aligned Quad
 * and it's actually slower than without the call!
 *
 * -- if a single excluder --
 *       317,519,900 4420      71,837   1     317,519,900 Aligned Pair
 *     2,877,032,900 4419     651,059  15     191,802,193 Aligned Triple
 *   163,270,592,000 4405  37,064,833  54   3,023,529,481 Aligned Quad
 * a bit faster than "new" version, but still nowhere near the "old".
 * 
 * 2020-12-17 24:?? It's faster now with workLevel, but still FAR too slow!
 * The old version is still SHIP-LOADS faster!
 * We're down two Pents, which I presume explains why we're up two Hexs.
 * -- old benchmark --
 *         time (ns)  calls   time/call  elims       time/elim hinter
 *    73,723,230,400   4372  16,862,587     68   1,084,165,152 Aligned Pent
 *    15,640,636,400   4315   3,624,712      1  15,640,636,400 Aligned Hex
 *    22,960,919,200   4314   5,322,419      5   4,592,183,840 Aligned Sept
 *    16,017,992,900   4310   3,716,471      1  16,017,992,900 Aligned Oct
 *    14,246,617,200   4309   3,306,246      0               0 Aligned Nona
 *    15,518,004,900   4309   3,601,300      0               0 Aligned Dec
 * 1465   541,449,150,800 (09:01)       369,589,864
 * -- new ass bungle --
 *   958,969,243,100   4372 219,343,376     66  14,529,837,016 Aligned Pent
 *    77,667,041,500   4316  17,995,143      3  25,889,013,833 Aligned Hex
 *   122,936,573,600   4314  28,497,119      5  24,587,314,720 Aligned Sept
 *   153,877,088,600   4310  35,702,340      1 153,877,088,600 Aligned Oct
 *   140,057,190,800   4309  32,503,409      0               0 Aligned Nona
 *    97,272,082,900   4309  22,574,166      0               0 Aligned Dec
 * 1465 1,924,746,339,800 (32:04)     1,313,820,027
 *
 * 2020-12-17 09:05 Big sets are too slow I performance test upto A5E.
 * -- old top1465.d5.2020-12-14.13-16-54.log --
 *       282,610,000  4426       63,852   1    282,610,000 Aligned Pair
 *     1,818,507,000  4425      410,962  16    113,656,687 Aligned Triple
 *    25,741,133,200  4410    5,836,991  52    495,021,792 Aligned Quad
 *    70,588,426,700  4368   16,160,354  68  1,038,065,098 Aligned Pent
 * -- new new clean common excluders method --
 *       415,619,400  4426       93,904   1    415,619,400 Aligned Pair
 *     3,509,588,100  4425      793,127  15    233,972,540 Aligned Triple
 *    83,166,324,100  4411   18,854,301  53  1,569,175,926 Aligned Quad
 *   409,261,698,300  4368   93,695,443  66  6,200,934,822 Aligned Pent
 * So we're faster than the "old new" but still MUCH slower than the "old".
 * -- eliminated duplicate cells from aligned sets --
 *       308,723,100  4430       69,689   1    308,723,100 Aligned Pair
 *     2,067,078,400  4429      466,714  15    137,805,226 Aligned Triple
 *    48,678,688,700  4415   11,025,750  53    918,465,824 Aligned Quad
 *   211,558,026,100  4372   48,389,301  66  3,205,424,637 Aligned Pent
 *    33,824,109,400  4316    7,836,911   3 11,274,703,133 Aligned Hex
 *    45,457,069,500  4314   10,537,104   5  9,091,413,900 Aligned Sept
 *    46,985,433,100  4310   10,901,492   1 46,985,433,100 Aligned Oct
 *    36,872,632,100  4309    8,557,120   0              0 Aligned Nona
 *    23,259,449,700  4309    5,397,876   0              0 Aligned Dec
 * </pre>
 *
 * @author Keith Corlett 2020-12-10
 */
public class AlignedExclusion extends AHinter
		implements diuf.sudoku.solver.IPreparer
{

	//1 2 3 4 5  6  7  8   9   10  11   12   13   14   15    16    17    18
	//1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 32768 65536 131072
	//AlignedPair  : maxNonHinters=   583 504#top1465.d5.mt
	//AlignedTriple: maxNonHinters=  2373 18#top1465.d5.mt
	//AlignedQuad  : maxNonHinters= 36826 12#top1465.d5.mt
	//AlignedPent  : maxNonHinters=124193 4#top1465.d5.mt
	//first 10 only:
	//AlignedPair  : maxNonHinters=   433 1#top1465.d5.mt
	//AlignedTriple: maxNonHinters=  1991 6#top1465.d5.mt
	//AlignedQuad  : maxNonHinters= 34435 4#top1465.d5.mt
	//AlignedPent  : maxNonHinters=124193 4#top1465.d5.mt
	//AlignedHex   : maxNonHinters=265177 4#top1465.d5.mt
	//AlignedSept  : maxNonHinters=430726 4#top1465.d5.mt 52:06
	//AlignedOct   : maxNonHinters=513336 4#top1465.d5.mt 80:47
	// The capacity of the NonHinters LongLongHashMap, which does NOT grow.
	// The index of this array is the degree, so 0 and 1 are never used.
	private static final int[] MAP_SIZE = new int[] {
	//  0  1  2       3       4        5         6         7         8         9         10
		0, 0, 2*1024, 8*1024, 32*1024, 128*1024, 256*1024, 512*1024, 512*1024, 512*1024, 512*1024
	};

	// excluder cells maybes.bits; there is only ONE static excludersMaybes
	// array, that is "shared" by all instances of AlignedExclusion, via the
	// Idx.cellMaybes method.
	private static final int[] excludersMaybes = new int[20];
	static {
		CELLS_MAYBES_VISITOR.maybes = excludersMaybes;
	}

	// degree - 1
	private final int degreeMinus1;
	// the candidate cells are aligned around there common excluder cells.
	// A candidate is just an unset cell that has sufficient excluders, and
	// an excluder is a cell with 2..$degree maybes, so that it's maybes can
	// be "covered" by a set of $degree cells. "Covered" means that all the
	// potential values of the common excluder cell are taken by candidates,
	// so that this "combo" (distinct potential values of $degree cells) is
	// not allowed. Having examined all "combos", if no "combo" allows a
	// candidate cells potential value then that value has been "excluded",
	// so it can and will be removed.
	private final Cell[] candidates;
	private final CellSet[] excluderSets = new CellSet[81]; // 1 per candidate
	// an array of $degree cells: rightmost is each available candidate cell,
	// then the next-left is each available candidate cell, and so on, all the
	// way to the bottom (left) of the jar.
	private final CellStackEntry[] cells;
	// an array of $degree bitsets of the current presumed values of cells in
	// the aligned set.
	private final ValsStackEntry[] vals;
	// a bitset of the values which have been allowed in each candidate cell in
	// the aligned set.
	private final int[] allowed;
	// aligned sets around 2+ excluder cells?
	private final boolean needTwo;
	// calls excluders.idx1(...) or idx2(...)
	private final Excluderator excluderator;
	// hashCode of the aligned set => total maybes, to skip each set of cells
	// which have already been checked in there current state, which is faster,
	// but still not fast enough... the individual A*E classes are faster.
	private final NonHinters nonHinters;

	/**
	 * The constructor.
	 * @param tech
	 */
	public AlignedExclusion(Tech tech) {
		super(tech);
		degreeMinus1 = degree - 1;
		candidates = new Cell[64]; // 64 = 81 - 17 minimum clues
		cells = new CellStackEntry[degree];
		vals = new ValsStackEntry[degree];
		for ( int i=0; i<degree; ++i ) {
			cells[i] = new CellStackEntry();
			vals[i] = new ValsStackEntry();
		}
		// each CellStackEntry has ONE idx, which is re-used;
		// except level 0's get theres directly from the idx() method.
		for ( int i=1; i<degree; ++i )
			cells[i].excluders = new Idx();
		// an array of bitsets of the values that are known to be allowed in
		// each cell in the aligned set.
		allowed = new int[degree];
		// A23E always has two excluders; A5+E user chooses "hacked" for two.
		// The minimum number of excluder cells required is 1 or 2 depending on
		// the size of the aligned set, and the users "hacked" setting. An
		// aligned set of 2 or 3 cells NEEDS two excluders to produce a hint,
		// but 4-or-more aligned cells require just one excluder, but the user
		// can select "hacked" for Aligned5+Exclusion, to produce about a third
		// of the hints in about a tenth of the time. This matters because A*E
		// is inherently a heavy process, because it does combinatorial TIMES 
		// combinatorial comparisons.
		needTwo = degree<4 || Settings.THE.get("isa"+degree+"ehacked");
		// optimisation: create an "Excluderator" ONCE per AlignedExclusion
		// intead of deciding to call idx1 or idx2 repeatedly: once for each
		// cell in each possible combination of $degree cells, ie lots a times.
		if ( needTwo )
			excluderator = new Excluderator2();
		else
			excluderator = new Excluderator1();
		// NonHinters size really matters! It doesn't grow like a HashMap, it
		// just does it's best with what-ever it's given, but when it gets TOO
		// overloaded it runs like a three-legged dog; and (for reasons I do
		// not fully understand) it slows EVERYthing down when it's too big,
		// so aim for the fastest OVERALL; not the fastest for this hinter.
		nonHinters = new NonHinters(MAP_SIZE[degree], 4);
	}

	/**
	 * We prepare ONCE, before the first findHints on each puzzle.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		nonHinters.clear();
		HintValidator.clear();

		// Pre-set the grid ONCE for the Idx.cellsMaybes method.
		// Note that we set a static field to an attribute; so we're presuming
		// that all instances of AlignedExclusion run on a single thread,
		// ie: two AlignedExclusion instances NEVER run concurrently.
		//
		// Note also that multiple instances of AlignedExclusion may prepare,
		// each setting the static field to the same grid; which is fine.
		CELLS_MAYBES_VISITOR.grid = grid;
	}

	/**
	 * <pre>
	 * The Aligned Exclusion Sudoku solving technique.
	 * HIGH LEVEL PSEUDOCODE:
	 * foreach aligned set of $degree cells (aligned around the excluder cell/s)
	 * 	 foreach possible combination of the potential values of the cells in the aligned set
	 *   skipping this presumed value if it is already taken by a preceeding sibling cell
	 *     if this combo is NOT a superset of any excluder cells potential values then
	 *       add this combo allowedValues
	 * 	   endif
	 * 	 next combo
	 * 	 for i = 1 .. degreePlus1
	 *     if allowedValues[i] != cellStack[i].maybes.bits then
	 * 	     houston we have an exclusion
	 *     endif
	 * 	 next
	 * 	 if any exclusions then
	 * 	   raise a hint and add it to the accumulator
	 * 	 endif
	 * next aligned set
	 * </pre>
	 *
	 * @param grid containing the Sudoku puzzle to search
	 * @param accu the IAccumulator to which I add hint/s;<br>
	 *  If accu.add returns true then I terminate my search.
	 * @return did we find hint/s
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {

		// ALL variables are pre-declared, to reduce stack-work.
		// My "struct" variables are fields, to create them ONCE.
		Cell cell; // the current cell
		// NOTE: We loop thrue the cellsStack first, and then the valsStack for
		// each aligned set of $degree cells. By "loop thrue" I mean we use the
		// stack (which is just an array of structs) to look at each possible
		// combination of first the cells, then the potential values of those
		// cells, which I call a "combo".
		CellStackEntry ce; // cellEntry: cells stack entry pointer
		ValsStackEntry ve; // valsEntry: values stack entry pointer
		// the number of cells currently actually in the excluders array. The
		// excluders array is a SINGLE cells array; rather than create a new
		// array for each aligned-set, because it is MUCH faster to just stop
		// at numExcluders than it is to hammer the GC.
		int numExcluders;
		// theExcludersMaybes is only used when there is ONE common excluder
		// cell, to hold a bitset of it's potential values, just because its
		// a bit faster to have it in it's own variable instead of repeatedly
		// looking it up in the excludersMaybes array.
		int theExcluderCellsMaybes;
		int cl; // cellLevel: the current level in the cell stack
		int vl; // valsLevel: the current level in the values stack
		int i; // general purpose index
		// workLevel: all potential values of all the cells to the right of the
		// workLevel have already been allowed, so we don't need to waste time
		// examining them repeatedly repeatedly, so when first combo is allowed
		// that's enough to enable us to JUMP directly to examining the next
		// potential value of the cell at the "workLevel". So the workLevel is
		// the rightmost level which has maybes that have not yet been allowed.
		int wl;

		boolean b; // I told him we've already got one

		// presume that no hints will be found
		boolean result = false;

		// find cells that are candidates to participate in exclusion sets,
		// and get the possible excluder-cells of each candidate.
		final int lastCandidate = populateCandidatesAndExcluders(grid) - 1;
		if ( lastCandidate < degreeMinus1 )
			return result; // rare but required (else CELL loop is endless)

		// cellStackLevel = the first level
		cl = 0;
		// the first cell at each level is it's index: trixie.
		for ( i=0; i<degree; ++i )
			cells[i].index = i;

		// foreach possible combination of candidate cells (an aligned set)
		CELL: for(;;) {
			// fallback levels while this level has no more cells to search
			while ( cells[cl].index > lastCandidate ) {
				// reset the index at this level to the cell after the current
				// cell in the previous level, if there is one; else 0.
				if ( cl > 0 )
					cells[cl].index = cells[cl-1].index + 1;
				else
					cells[cl].index = 0;
				// then move left one to test the next index
				if ( --cl < 0 )
					return result; // done all combinations of degree cells
			}
			// get this cell, and move index along to the next candidate cell
			cell = candidates[cells[cl].index++];
//if ( "E4".equals(cell.id) )
//	Debug.breakpoint();
			// cl == 0 means this is the first cell in the aligned set, so we
			// set the idx field to the CACHED idx returned by the idx method.
			if ( cl == 0 ) { // the first cell in the aligned set
				// set this cell up in the cellsStack
				(ce=cells[cl]).cell = cell;
				// note that excluders Idx is set directly to the CACHED Idx
				// returned by the idx() method, so the Idx will be reused next
				// time we examine the excludersSet of this cell.
				ce.excluders = excluderSets[cell.i].idx();
				// set-up this cell in the valsStack
				// and increment the cellLevel to 1
				ve=vals[cl++];
				ve.set(cell);
			// does this cell align with others already in the aligned set?
			// if cl > 0 then cl must be 1..degreeMinus1, so see if this cell
			// has atleast the minimum number of required excluders in common
			// with all the cells that are already in this aligned set.
			} else if ( !excluderator.common(excluderSets[cell.i]
					, cells[cl].excluders, cells[cl-1].excluders) ) {
				// set-up this cell in the cellsStack
				(ce=cells[cl]).cell = cell;
				// set-up this cell in the valsStack
				ve = vals[cl];
				ve.set(cell);
				if ( cl == degreeMinus1 ) { // its a complete "aligned set"
//					assert unique(cells) : dump(cells);
					// skip this aligned-set if it's already known to not hint, in
					// its current state; ie if cell.maybes have changed since last
					// examination then re-examine them; elims like 99.3% re-exams.
					if ( !nonHinters.skip(cells, degree) ) {
						// set this aligned sets excludersMaybes, a static field.
						// The "excluder cells" are the siblings of all the cells in this
						// aligned set having maybes.size 2..degree, so that these $degree
						// cells can "cover" there upto $degree maybes.
						numExcluders = ce.excluders.cellsMaybes();
						// remove duplicate/superset excluders, and also remove
						// excluders which do not intersect with aligned set.
						if ( (numExcluders=clean(excludersMaybes, numExcluders, cells)) == 0 ) {
							nonHinters.put(); // no repeats
							continue;
						}
						// valuesLevel = the first
						vl = 0;
						// clear the allowedValues array for this aligned set.
						for ( i=0; i<degree; ++i )
							allowed[i] = 0;
						// reset the first valsStack entry to its first value.
						vals[0].index = 0;
						// reset workLevel to the rightmost level
						wl = degreeMinus1;
						if ( numExcluders == 1 ) {
							// theres 1 excluder cell which is 98% of cases
							// (except when needTwo, obviously) and 1 excluder
							// can be tested faster, so it's worth duplicating
							// the code; so this code is the same in the if and
							// the else branches, except the "excluders covers"
							// statement/loop.
							// shortcut maybes.bits of the single excluder cell
							theExcluderCellsMaybes = excludersMaybes[0];
							// DOG_____ING_LOOP (slow): populates allowed.
							// There are two rules used to disallow a combo:
							// 1. if an aligned cell has no maybes then all
							//    combos containing values are not allowed,
							//    ie just skip the bastard.
							// 2. if a combo contains all of a common excluder
							//    cells maybes then this combo is not allowed,
							//    ie just skip the bastard.
							//
							// foreach possible combination of potential values of the
							// degree cells in this aligned set of candidate cells.
							VAL1: for(;;) {
								// fallback levels while this level is out of values.
								while ( vals[vl].index > vals[vl].last )
									if ( --vl < 0 )
										break VAL1; // done all combos of aligned set
								// set the vals entry pointer.
								ve = vals[vl];
								// set my presumed value, and move index along
								// to the next available value, for next time.
								ve.value = ve.maybes[ve.index++];
								if ( vl == 0 ) { // first level
									// hard-set the combo to my presumed value
									ve.combo = ve.value;
									// move right to the next valuesLevel
									// starting with it's first maybe
									vals[++vl].index = 0;
								} else { // subseqent level
									// skip value if already taken by a sibling cell.
									for ( i=0; i<vl; ++i )
										// WARN: ve does NOT work here!
										if ( vals[i].value == vals[vl].value
										  && vals[vl].sees[vals[i].i] )
											continue VAL1;
									// build-up the "combo" (a combination of
									// the potential values of the aligned set)
									ve.combo = vals[vl-1].combo | ve.value;
									if ( vl == degreeMinus1 ) {
										// if combo does NOT cover the excluder cell
										//   add this combo to allowedValues
										if ( (theExcluderCellsMaybes & ~ve.combo) != 0 ) {
											// all vals right of "workLevel" are already allowed.
											for ( i=0; i<=wl; ++i )
												allowed[i] |= vals[i].value;
											// if all values are allowed at the workLevel then
											// move workLevel left one place, to JUMP straight
											// to the next value which has yet to be allowed.
											// This makes ZERO difference in A2E, but its about
											// 3000 miles an hour faster in A10E, because it
											// reduces a 10 * 10*10*10 * 10*10*10 * 10*10*10 problem
											// down to a 10 *  1* 1* 1 *  1* 1* 1 *  1* 1* 1 problem.
											// Note that actual performance for A10E is
											// more like  3 *  3* 3* 3 *  3* 3* 3 *  3* 3* 3,
											// which is still "slow", but not "impossible".
											if ( allowed[wl] == cells[wl].cell.maybes.bits ) {
												if ( (vl=--wl) < 0 ) { // all values allowed!
													// record this aligned set did not hint.
													nonHinters.put();
													continue CELL;
												}
											} else
												// test the next value at the workLevel!
												vl = wl;
										}
									} else { // its a complete combo
										// move right to the next level
										// starting with it's first maybe
										vals[++vl].index = 0;
									}
								}
							} // next combo of potential values
						} else {
							// only about 2% of aligned sets have 2 or more excluder
							// cells, so we just loop thrue them, which is slower, but
							// it's only 2% of cases, so it doesn't really matter.
							//
							// DOG_____ING_LOOP (slow): sets allowedValues to a bitset
							// (per Values) of the values that're allowed in each cell;
							// then if any cell maybes is NOT allowed we raise a hint.
							// There are two rules used to disallow a combo:
							// 1. if a cell in aligned set has no possible values then
							//    all combos containing these values are not allowed.
							// 2. if a combo contains all potential values of a common
							//    excluder cell then this combo is not allowed.
							//
							// foreach possible combination of potential values of the
							// degree cells in this aligned set of candidate cells.
							VAL2: for(;;) {
								// fallback levels while this level is out of values.
								while ( vals[vl].index > vals[vl].last )
									if ( --vl < 0 )
										break VAL2; // done all combos of this aligned set
								// set the vals entry pointer.
								ve = vals[vl];
								// set my presumed value, and move the index along to the
								// next available value, for next time.
								ve.value = ve.maybes[ve.index++];
								if ( vl == 0 ) { // first level
									ve.combo = ve.value;
									vals[++vl].index = 0;
								} else { // subsequent level
									// skip value if already taken by a sibling.
									for ( i=0; i<vl; ++i )
										// WARN: ve does NOT work here!
										if ( vals[i].value == vals[vl].value
										  && vals[vl].sees[vals[i].i] )
											continue VAL2;
									// build-up the combo of values
									ve.combo = vals[vl-1].combo | ve.value;
									if ( vl == degreeMinus1 ) { // final level
										// so now we test our completed combo
										// if this combo doesnt cover any excluder cell
										//   add this combo to allowed
										b = true;
										for ( i=0; i<numExcluders; ++i )
											if ( (excludersMaybes[i] & ~ve.combo) == 0 ) {
												b = false;
												break;
											}
										if ( b ) { // this combo is allowed
											// all vals right of "workLevel" are already allowed.
											for ( i=0; i<=wl; ++i )
												allowed[i] |= vals[i].value;
											// if all values are allowed at the workLevel then
											// move workLevel left one place, to JUMP straight
											// to the next value which has yet to be allowed.
											// This makes ZERO difference in A2E, but its about
											// 3000 miles an hour faster in A10E, because it
											// reduces a 10 * 10*10*10 * 10*10*10 * 10*10*10 problem
											// down to a 10 *  1* 1* 1 *  1* 1* 1 *  1* 1* 1 problem.
											// Note that actual performance for A10E is
											// more like  3 *  3* 3* 3 *  3* 3* 3 *  3* 3* 3,
											// which is still "slow", but no longer "impossible".
											if ( allowed[wl] == cells[wl].cell.maybes.bits ) {
												if ( (vl=--wl) < 0 ) { // all values allowed!
													// record this aligned set did not hint.
													nonHinters.put();
													continue CELL;
												}
											} else
												// test the next value at the workLevel!
												vl = wl;
										}
									} else {
										// move right to the next values-level
										// and start with its first value
										vals[++vl].index = 0;
									}
								}
							} // next combo of potential values
						}
						// NB: when we get here, the following is just a double-check
						// which is "almost guaranteed" to be true!
						// if any of potential values of the cells in this aligned set
						// have not been allowed by atleast one combo, then that value
						// may be eliminated from that cell, ie we found an exclusion.
						b = false;
						for ( i=0; i<degree; ++i )
							b |= allowed[i] != cells[i].cell.maybes.bits;
						if ( b ) {
							// Yeah! We found an Aligned Exclusion, so create a hint.
							// reds = cells potential values that are not in allowed
							Pots redPots = createRedPotentials();
							if ( redPots.isEmpty() )
								continue; // Should never happen. Never say never.
							// use a new cell array for each hint; which are few.
							Cell[] cellsA = new Cell[degree];
							for ( i=0; i<degree; ++i )
								cellsA[i] = cells[i].cell;
							// use a new excluders-cells array too
							Cell[] cmnExcluders = ce.excluders.cells(grid
									, new Cell[ce.excluders.size()]);
							// build the excluded combos map to go in the hint
							ExcludedCombosMap map = buildExcludedCombosMap(
									cmnExcluders, cellsA, redPots);
							// create the hint
							AHint hint = new AlignedExclusionHint(this, redPots, cellsA
									, Frmt.ssv(cmnExcluders), map);
							if ( HintValidator.ALIGNED_EXCLUSION_USES ) {
								if ( !HintValidator.isValid(grid, redPots) ) {
									HintValidator.report(tech.name(), grid, hint.toString());
									continue;
								}
							}
							// and add the bastard to the accumulator
							result = true; // in case add returns false
							if ( accu.add(hint) )
								return true;
						} else {
							// record the fact that this aligned set did not produce a
							// hint, so that we can skip-it the next time we see it if
							// all of its maybes are unchanged.
							nonHinters.put();
						}
					}
				} else {
					// the next level starts with the next cell (ie a forward-only
					// search). Note that cellStack[cl].index has already been
					// "pre-incremented" to the index of the next candidate cell!
					cells[cl+1].index = cells[cl].index;
					// move right to the next level in the cellStack
					++cl;
				}
			}
		}
	}

	/**
	 * Does this combo "cover" an excluder cell? Ie, does any excluder cell in
	 * the array have ONLY potential values that are ALL in the given combo?
	 * <p>
	 * A "combo" is a possible combination of potential values of cells in an
	 * aligned set of $degree cells. "Covered" means does the combo contain
	 * ALL the values in any excluderCell.maybes.bits. The excluders are the
	 * set-of-excluder-cells at the heart of an aligned set.
	 *
	 * @param combo a bitset containing the current presumed values of the
	 *  cells in the aligned set, as per {@link diuf.sudoku.Values#bits}.
	 * @param excluders an array containing numExcluders excluder cells.
	 * @param numExcluders the number of cells actually in the excluders array.
	 * @return true if any excluder cell disallows this combo; else false
	 */
	static boolean covers(final int combo, Cell[] excluders, int numExcluders) {
		for ( int i=0; i<numExcluders; ++i )
			// if the given combo is a superset of this excluder cells maybes,
			// then this combo "covers" this excluder cell, so return true.
			// note that the combo may also contain superflous values.
			if ( (excluders[i].maybes.bits & ~combo) == 0 )
				return true;
		return false;
	}

	/**
	 * Populates the candidateCellsArray and the cellsExcluders.
	 * <p>
	 * Finds Cells (candidates) which have atleast 2 maybes and have atleast 2
	 * siblings (excluders) with maybes.size between 2 and $degree (inclusive).
	 * This method differs from above populateCandidatesAndExcluders just in da
	 * minimum number of required excluders. A4+E all require atleast 2 common
	 * excluder cells to perform exclusion on (otherwise dey produce irrelevant
	 * hints), so we start with candidates that have atleast 2 excluders.
	 * <p>
	 * WARNING: I don't understand why this is the case. A real smart bastard
	 * might provide insight leading to an assured solution. I just fixed it,
	 * but I might be full of s__te. It's all I can do.
	 *
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndExcluders(Grid grid) {

		int card;
		// An excluder cell is a sibling with 2..5 maybes and numExcls>0
		// the firstExcluder is used only when needTwo, but always defined
		Cell firstExcluder;
		// cell's set of excluder cells. CellSet is a Set<Cell> with fast
		// contains, remove & clear; but a slow constructor
		CellSet set = null;
		// create a local ONCE instead of repeatedly dereferencing in the loop
		final int degreePlus1 = this.degreePlus1;
		final boolean needTwoExcluders = this.needTwo;
		final Cell[] candidates = this.candidates;
		final CellSet[] excluders = this.excluderSets;

		// clear my fields
		MyArrays.clear(candidates);
		MyArrays.clear(excluders);

		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		int numCandidates = 0;
		// foreach unset cell
		CELL_LOOP: for ( Cell cell : grid.cells ) {
			if ( cell.maybes.size > 1 ) {
				firstExcluder = null;
				// find cells excluders: ie siblings with maybes.size 2..5
				for ( Cell sib : cell.siblings ) { // 81*20=1620
					// sib is an excluder if it has maybes.size 2..degree
					// opt: do the < first because it's more deterministic.
					if ( (card=sib.maybes.size)<degreePlus1 && card>1 ) {
						// we needTwo in A23E or user says "hacked" in A5+E
						if ( needTwoExcluders ) {
							if ( firstExcluder == null )
								firstExcluder = sib;
							else if ( set == null ) {
								set = new CellSet(); // slow constructor!
								set.add(firstExcluder);
								set.add(sib);
							} else
								set.add(sib);
						} else {
							if ( set == null )
								set = new CellSet(); // slow constructor!
							set.add(sib);
						}
					} else if ( card == 1 )
						continue CELL_LOOP; // opt: skip sibs of naked singles
				}
				if ( set != null ) {
//					assert set.size() >= (needTwoExcluders ? 2 : 1);
					candidates[numCandidates++]=cell;
					excluders[cell.i] = set;
					set = null; // for the next iteration of dis loop (ie cell)
				}
			}
		} // next cell in grid // next cell in grid
		return numCandidates;
	}

	/**
	 * The createRedPotentials creates a map of the red (removable) potential
	 * values, from Cell => Values from the cellStack and allowedValues array.
	 * Basically if each cell value isn't allowed then it's excluded so add the
	 * bastard to redPots. Simples!
	 * @return a new Pots containing the excluded values, each in his Cell.
	 * Never null or empty.
	 */
	private Pots createRedPotentials() {
		Pots redPots = new Pots();
		// foreach candidate cell in this aligned set
		for ( int i=0,bits; i<degree; ++i )
			// does cell have any maybes that are not allowed at this position?
			if ( (bits=(cells[i].cell.maybes.bits & ~allowed[i])) != 0 )
				// foreach cell maybe that is not allowed
				for ( int v : ARRAYS[bits] )
					// Yeah, 'v' can be excluded from 'cell'
					redPots.upsert(cells[i].cell, v);
		assert !redPots.isEmpty(); // asserts are for developers only
		return redPots;
	}

	// Now we do that dog____ing loop again, except this time we build the
	// excluded combos map, which we need for the hint. This takes "some time",
	// mainly because HashMap insertion is a bit slow Redge), but this loop is
	// executed < 1000 times for top1465, not bloody billions of times, like
	// the real dog____ing loop.
	private ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcluders
			, Cell[] alignedSet, Pots reds) {
		ValsStackEntry e; // the current values stack entry (ie stack[l])
		int l, i; // l is stack level, i is a general purpose index
		final ExcludedCombosMap map = new ExcludedCombosMap(alignedSet, reds);
		// I create my own "valsStack" rather than ____ with my callers
		// "valsStack" field, which it's still using when !accu.isSingle.
		final ValsStackEntry[] stack = new ValsStackEntry[degree];
		for ( i=0; i<degree; ++i )
			stack[i] = new ValsStackEntry();
		stack[0].combo = 0; // ie an empty bitset
		l = 0; // the first stack level
		stack[0].index = 0; // start from the cells first potential value
		// copy immutable cell-data from the given cells to the valsStack (vs)
		for ( i=0; i<degree; ++i ) {
			stack[i].maybes = SHIFTED[alignedSet[i].maybes.bits];
			stack[i].last = alignedSet[i].maybes.size - 1;
		}
		VAL: for(;;) {
			// fallback a level while we're out of values at this level
			while ( stack[l].index > stack[l].last ) {
				// reset the index to "restart" this exhausted level
				stack[l].index = 0;
				if ( --l < 0 )
					return map; // we've done all combos of cells values
			}
			e = stack[l];
			// set the "presumed value" of this cell
			// and advance the index to the next potential value of this cell
			e.value = e.maybes[e.index++];
			// skip this value if it's already taken by a preceeding sibling
			for ( i=0; i<l; ++i ) {
				if ( e.value == stack[i].value
				  && alignedSet[l].sees[alignedSet[i].i] ) {
					// all combos containing both of these values are excluded,
					// so unpack the two equal-values into an array at da index
					// of the two sibling cells that cannot have the same value
					int[] a = new int[degree];
					a[l] = ARRAYS[stack[l].value][0];
					a[i] = ARRAYS[stack[i].value][0];
					// null means exclusion not specific to an excluder cell.
					map.put(new HashA(a), null);
					continue VAL;
				}
			}
			// build-up the combo = the preceeding values + my presumedValue
			if ( l == 0 )
				e.combo = e.value;
			else
				e.combo = stack[l-1].combo | e.value;
			if ( l == degreeMinus1 ) { // it's a complete combo
				// if this combo contains all of any excluders possible values
				// then add this "excluded combo" to the map
				for ( Cell x : cmnExcluders ) {
					if ( (x.maybes.bits & ~e.combo) == 0 ) {
						int[] a = new int[degree];
						for ( i=0; i<degree; ++i )
							a[i] = ARRAYS[stack[i].value][0];
						map.put(new HashA(a), x);
						break; // we want only the first excluder of each combo
					}
				}
			} else {
				// move right to the next values-level,
				// and start at its first potential value
				stack[++l].index = 0;
			}
		}
	}

	// clean-out all defective excludersMaybes
	// 1. remove excluders which contain all values in any other excluder, 
	//    including any duplicates.
	// 2. remove excluders which do not intersect with any cell in the aligned
	//    set (ie do not have atleast one common maybe).
	private int clean(int[] excludersMaybes, int numExcluders, CellStackEntry[] stack) {
		boolean any;
		for ( int i=0; i<numExcluders; ++i ) {
			// 1. if j is a superset of i then remove j
			for ( int j=i+1; j<numExcluders; ++j ) {
				if ( (excludersMaybes[i] & ~excludersMaybes[j]) == 0 ) {
					// move j and all to it's right up one.
					for ( int k=j,K=numExcluders-1; k<K; ++k )
						excludersMaybes[k] = excludersMaybes[k+1];
					--numExcluders;
				}
			}
			// 2. if i does NOT intersect any aligned cell then remove i
			any = false;
			for ( CellStackEntry e : stack ) {
				if ( (e.cell.maybes.bits & excludersMaybes[i]) != 0 ) {
					any = true;
					break;
				}
			}
			if ( !any ) {
				// move i and all to it's right up one.
				for ( int k=i,K=numExcluders-1; k<K; ++k )
					excludersMaybes[k] = excludersMaybes[k+1];
				--numExcluders;
			}
		}
		return numExcluders;
	}

//	// for assert only, to detect double ups (has happened)!
//	private boolean unique(CellStackEntry[] cells) {
//		final int n = cells.length, m = n - 1;
//		for ( int i=0; i<m; ++i )
//			for ( int j=i+1; j<n; ++j )
//				if ( cells[j].cell.i == cells[i].cell.i )
//					return false;
//		return true;
//	}
//
//	// for assert only, to detect double ups (has happened)!
//	private String dump(CellStackEntry[] cells) {
//		final int n = cells.length;
//		StringBuilder sb = new StringBuilder(20+n*13);
//		sb.append("double-up: ");
//		for ( int i=0; i<n; ++i ) {
//			if(i>0) sb.append(' ');
//			sb.append(cells[i].cell.toFullString());
//		}
//		return sb.toString();
//	}

	class CellStackEntry {
		int index; // the index of the current in the candidates array
		Cell cell; // the cell at this level 1..degree in the stack
		Idx excluders; // excludersIdx: excluders common to all cells in an aligned set
	}

	class ValsStackEntry {
		// Values.SHIFTED[cell.maybes.bits] of the current cell at level
		// 1..degree in the cellsStack.
		int[] maybes;
		// the index of the current/next presumedValue in maybes
		int index;
		// the last valid index (ie maybes.size-1)
		int last;
		// the potential value we presume this cell holds.
		// This value is a left-shifted bitset representaton
		int value;
		// a bitset of the value of this and all cells to my left
		int combo;
		// ---- additions for speed ----
		// the indice of my cell in the grid: cells[i].cell.i
		int i;
		// my cell's sees array
		boolean[] sees;
		// set this ValsStackEntry to the given cell.
		void set(Cell cell) {
			i = cell.i;
			maybes = SHIFTED[cell.maybes.bits];
			last = cell.maybes.size - 1;
			sees = cell.sees;
		}
	}

	private static interface Excluderator {
		boolean common(CellSet excluders, Idx dest, Idx src);
	}

	private static class Excluderator1 implements Excluderator {
		@Override
		public boolean common(CellSet excluders, Idx dest, Idx src) {
			return excluders.idx1(dest, src);
		}
	}

	private static class Excluderator2 implements Excluderator {
		@Override
		public boolean common(CellSet excluders, Idx dest, Idx src) {
			return excluders.idx2(dest, src);
		}
	}

	/**
	 * A map of the excluded combos => the optional lockingCell.
	 * A null lockingCell means "one instance of value per region".
	 * <p>Extends a LINKEDHashMap because order is significant to user
	 * following the resulting hint.
	 * <p>The put method is overridden to ignore attempts to add a key-array
	 * which does NOT contain one of the red (removable) values of the hint
	 * we're building. This is a tad slow, but speed doesn't really matter coz
	 * there's so few hints, and it's the most succinct way I can think of.
	 * Turns out this override costs ____all time, it might even be saving us
	 * a bit, I guess the relevance check is faster than extraneous puts where.
	 */
	private static final class ExcludedCombosMap extends LinkedHashMap<HashA, Cell> {
		private static final long serialVersionUID = 245566510L;
		private final Cell[] cells;
		private final Pots redPots;

		/**
		 * Constructs a new ExcludedCombosMap whose put method will use the
		 * AlignedExclusionHint.isRelevant(cells, redPots, key.array) method
		 * to determine if the combo is relevant to the hint, thus hopefully
		 * avoiding ALL those annoying irrelevant hints. I need to test this
		 * now to see if it's much slower. We create 5,318 maps in for top1465,
		 * so I can't see performance being much of an issue.
		 * @param cells
		 * @param redPots
		 */
		ExcludedCombosMap(Cell[] cells, Pots redPots) {
			super(128, 0.75F);
			this.cells = cells;
			this.redPots = redPots;
		}

		/**
		 * Overridden to ignore irrelevant values (which aren't in the redBits
		 * bitset that was passed to my constructor).
		 *
		 * <p><b>Enhancement:</b> now alternately uses the Cell[] and Pots from
		 * the preferred constructor to call AlignedExclusionHint.isRelevant,
		 * so that we can finally eradicate those annoying irrelevant hints.
		 * <b>When</b> all usages of the deprecated constructor are eliminated
		 * please remove the cells==null branch of this method, and the
		 * deprecated constructor, and it's field/s.<br>
		 * FYI: compile with -Xlint to find all usages of deprecated methods.
		 * In Nutbeans: right-click on the project ~ Properties ~ Build /
		 * Compiling ~ Additional Compiler Options: paste in -Xlint
		 * @param key HashA to put
		 * @param value Cell the locking cell (aka excluding cell), or null
		 *  meaning "only one instance of value is allowed per region".
		 * @return null if none of the values in key are relevant, else the
		 *  previous value (Cell) associated with this HashA (which should
		 *  NEVER exist), else null meaning none.<br>
		 *  This return value isn't used. It'd be a pig to use because nulls
		 *  are ambiguous. If you need a real return value then go ahead change
		 *  this code to make it useful for your circumstances. Make it return
		 *  an OccelotsSpleen for all I care. Dotaise sauce is extra.
		 */
		@Override
		public Cell put(HashA key, Cell value) {
			return AlignedExclusionHint.isRelevant(cells, redPots, key.array)
					? super.put(key, value) : null;
		}

		public LinkedHashSet<Cell> getUsedCommonExcluders(Cell cmnExcls[], int numCmnExcls) {
			LinkedHashSet<Cell> set = new LinkedHashSet<>(16, 0.75f);
			Iterator<HashA> it = super.keySet().iterator();
			Cell c;
			while ( it.hasNext() )
				if ( (c=get(it.next())) != null )
					set.add(c);
			if ( set.isEmpty() )
				for ( int i=0; i<numCmnExcls; ++i )
					set.add(cmnExcls[i]);
			return set;
		}
	}

}
