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
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Settings;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.MyArrays;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;


/**
 * AlignedExclusion implements the Aligned Exclusion Sudoku solving technique
 * for sets of between 2 and 10 cells.
 * <p>
 * Aligned Exclusion eliminates potential values of "candidate cells" that are
 * aligned around the "excluder cell/s". The candidate cells form "the aligned
 * set" and the "common excluders" are the cells they're aligned around.
 * <p>
 * There's only one way to do exclusion, and it's expensive. We must go through
 * all the possible combinations of the potential values of each aligned set to
 * see if each combo is allowed (rules below). If it is then we record each
 * cells presumed value in its cell (that's important); then we just check if
 * all potential cell values have been "allowed"; and if not then there is an
 * "exclusion", so we raise a hint. It sounds simple enough; but the actual
 * implementation is mega-confundussed by details, trying to make an inherently
 * combinatorial-times-combinatorial process (ie bloody slow) a bit faster; to
 * bring it up to "usable" speed, which I have thus far failed to do. sigh.
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
 * I mentioned "candidate" cells without defining them: A "candidate" is an
 * unset cell which sees (same box, row, or col) the requisite number of
 * excluder cells. An excluder cell is one with 2..degree potential values.
 * The "requisite number" of excluder cells depends on the size of the aligned
 * set, and also on the users "hacked" setting. An aligned set of 2 or 3 cells
 * only produces hints when those cells are aligned around two excluder cells.
 * Aligned sets of 4 to 10 cells CAN still produce a hint when aligned around a
 * single cell, but hints from a single excluder cell are MUCH rarer.
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
 * HoDoKu is another open source Sudoku solver. Download my version from the
 * Sudoku Explainer homepage on SourceFroge: it's faster.
 * <p>
 * AlignedExclusion is MUCH more compact than the Aligned*Exclusion classes,
 * which are "mass boiler-plate" code. AlignedExclusion is maintainable, but it
 * is tricky and slower. The old Aligned5Exclusion runs in 70 seconds, and this
 * version runs in 211 seconds (A5E top1465), ie THREE TIMES as long! BFFIIK!
 * <p>
 * Be warned that I found this class to be a complete and utter pain in the ass
 * to develop and debug. It's just too complex. So ____ with it at your peril!
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
 *
 * 2021-01-23 12:57 AlignedExclusion is now faster for A2..4E and takes about
 * twice as long for A5..10E, so the old "align" package is still preferred for
 * the big slow A*E's, but align2 is ALWAYS used for the small ones. Here's the
 * latest comparisons. Note: A2E, A9E, and A10E left out coz they don't hint.
 * -- old align --
 *     2,020,619,100  3863      523,069   4    505,154,775 Aligned Triple
 *     7,659,148,200  3859    1,984,749   6  1,276,524,700 Aligned Quad
 *     9,978,906,800  3853    2,589,905   3  3,326,302,266 Aligned Pent (H)	FASTER
 *    17,765,837,000  3850    4,614,503   2  8,882,918,500 Aligned Hex (H)	FASTER
 *    16,033,720,600  3848    4,166,767   6  2,672,286,766 Aligned Sept (H)	FASTER
 *    15,688,375,300  3842    4,083,387   2  7,844,187,650 Aligned Oct (H)	FASTER
 * -- latest align2 --
 *     1,890,230,700  3863      489,316   4    472,557,675 Aligned Triple	FASTER
 *     7,087,961,000  3859    1,836,735   6  1,181,326,833 Aligned Quad		FASTER
 *    16,737,073,200  3853    4,343,906   4  4,184,268,300 Aligned Pent (H) BFFIK: finds an extra elim!
 *    27,929,457,200  3849    7,256,289   2 13,964,728,600 Aligned Hex (H)
 *    36,786,549,800  3847    9,562,399   6  6,131,091,633 Aligned Sept (H)
 *    39,009,751,800  3841   10,156,144   2 19,504,875,900 Aligned Oct (H)
 * -- populateCandidatesAndOne/TwoExcluders --
 * And ____ me sideways the easy part was the slow part!
 *     1,721,585,000  3863      445,660   4    430,396,250 Aligned Triple
 *     6,313,094,000  3859    1,635,940   6  1,052,182,333 Aligned Quad
 *    10,108,948,100  3853    2,623,656   3  3,369,649,366 Aligned Pent (H) old
 *    20,293,482,600  3850    5,271,034   2 10,146,741,300 Aligned Hex (H)  old
 *    18,047,778,100  3848    4,690,171   6  3,007,963,016 Aligned Sept (H) old
 *    14,850,689,800  3842    3,865,353   2  7,425,344,900 Aligned Oct (H)  old
 * So how does that stack-up out to A8E? A bit faster, but not upto the old.
 *     1,621,026,600  3863      419,628   4    405,256,650 Aligned Triple
 *     6,202,209,900  3859    1,607,206   6  1,033,701,650 Aligned Quad
 *    14,955,416,100  3853    3,881,499   4  3,738,854,025 Aligned Pent (H) new
 *    25,158,004,300  3849    6,536,244   2 12,579,002,150 Aligned Hex (H)  new
 *    33,476,126,200  3847    8,701,878   6  5,579,354,366 Aligned Sept (H) new
 *    35,599,522,400  3841    9,268,295   2 17,799,761,200 Aligned Oct (H)  new
 * 2021-01-24 07:41 changes made and shipped yesterday
 * -- remove collisions from slave ValsStackElement.cands --
 *     1,625,169,300  3863      420,701   4    406,292,325 Aligned Triple
 *    41,352,188,800  3859   10,715,778   6  6,892,031,466 Aligned Quad    WTF?
 *    14,201,018,000  3853    3,685,704   4  3,550,254,500 Aligned Pent (H)
 *    23,473,579,400  3849    6,098,617   2 11,736,789,700 Aligned Hex (H)
 *    31,325,968,300  3847    8,142,960   6  5,220,994,716 Aligned Sept (H)
 *    32,003,959,700  3841    8,332,194   2 16,001,979,850 Aligned Oct (H)
 * WTF? Why is AlignedQuad SOOOO much slower when all the rest are OK?
 * I'll run it again to see if it's repeatable, or just a JIT-narkle.
 *     2,289,893,800  3863      592,776   4    572,473,450 Aligned Triple
 *    54,494,033,000  3859   14,121,283   6  9,082,338,833 Aligned Quad
 *    19,026,731,500  3853    4,938,160   4  4,756,682,875 Aligned Pent
 *    32,120,286,400  3849    8,345,099   2 16,060,143,200 Aligned Hex
 *    41,246,071,000  3847   10,721,619   6  6,874,345,166 Aligned Sept
 *    42,878,437,200  3841   11,163,352   2 21,439,218,600 Aligned Oct
 * And running it again overheats the CPU so everything is slower. sigh.
 * But A4E is still comparatively a LOT slower than it should be. sigh.
 * </pre>
 *
 * @author Keith Corlett 2020-12-10 created
 */
public class AlignedExclusion extends AHinter
		implements diuf.sudoku.solver.IPreparer
				 , diuf.sudoku.solver.hinters.ICleanUp
{
//	//@check: commented out: for debugging only
//	// "Hot" cands are the selectable ones, in the valuesStack
//	private String hotCandsToString() {
//		StringBuilder sb = new StringBuilder(14*degree);
//		for ( int i=0; i<degree; ++i ) {
//			if(i>0) sb.append(", ");
////			sb.append(vStack[i].cands)
////			  .append(':')
////			  .append(Values.toString(vStack[i].cands));
//			sb.append(Values.toString(vStack[i].cands));
//		}
//		return sb.toString();
//	}
//	// for assert only, to detect double ups (has happened)!
//	private boolean unique(CellStackEntry[] cells) {
//		final int n = cells.length, m = n - 1;
//		for ( int i=0; i<m; ++i )
//			for ( int j=i+1; j<n; ++j )
//				if ( cells[j].cell.i == cells[i].cell.i )
//					return false;
//		return true;
//	}
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
	// NOOB: The degree is the number of cells in the aligned set.
	private static final int[] MAP_SIZE = new int[] {
	//  0  1  2       3       4        5         6         7         8         9         10
		0, 0, 2*1024, 8*1024, 32*1024, 128*1024, 256*1024, 512*1024, 512*1024, 512*1024, 512*1024
	};

	// EXCLUDERS_MAYBES: excluder cells maybes.bits; there's ONE static array
	// shared by all instances of AlignedExclusion, via Idx#cellMaybes.
	// package visible to be read in NonHinters#skip
	// NOTE: This MUST be done static for LogicalAnalyserTest, apparently!
	static final int[] EXCLUDERS_MAYBES = new int[20];

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
	// so it will be removed by the hint we produce.
	private final Cell[] candidates;

	// the cells candidate can use as an excluder; so to build an aligned set
	// we're looking for the minimum required "common" excluders, which all
	// candidates in the set can "see" (ie same box, row, or col).
	private final CellSet[] exclSets = new CellSet[81]; // 1 per candidate

	// an array of $degree cells: rightmost is each available candidate cell,
	// then the next-left is each available candidate cell of the current set
	// of cells which is one smaller than this "level", and so on, all the way
	// to the bottom of the jar. You could call this variable cellsStack.
	// The cellsStack is populated left-to-right; then we "zero in on" the
	// workLevel, which is the leftmost level which still has values that have
	// not yet been allowed, and we try the next value at the work-level,
	// instead of wasting time repeatedly checking values that've already all
	// been allowed. All potential values of all cells to the right of the
	// "workLevel" have already been allowed. So we hammer away at da workLevel
	// until all of it's potential values are allowed, and then we move the
	// workLevel left one, and hammer away at his values, and so on, til done.
	private final CellStackEntry[] cStack;

	// an array of $degree bitsets of the current presumed values of cells in
	// the aligned set. You could call this variable valuesStack.

	private final ValsStackEntry[] vStack;
	// a bitset of the values which have been allowed in each candidate cell in
	// the aligned set.
	private final int[] allowed;

	// do we needTwoExcluderCells: aligned sets around 2+ excluder cells?
	// Always true for A23E; false for A4E; "hacked" setting for A5+E.
	private final boolean needTwo;

	// the minimum number of excluder-cells required to align a set around
	private final int minExcls;

	// calls excluders.idx1(...) or idx2(...)
	private final IExcluderator excluderator;

	// hashCode of the aligned set => total maybes, to skip each set of cells
	// which have already been checked in there current state, which is faster,
	// but still not fast enough... the individual A*E classes are faster.
	private final NonHinters nonHinters;

	private boolean firstPass = true;

	/**
	 * The constructor. I read my degree (number of cells in an aligned set)
	 * from the given Tech, which MUST be a Tech.Aligned*.
	 *
	 * @param tech to implement: Tech.Aligned*
	 * @param defaultIsHacked if there's no existing registry setting should
	 * this A*E be hacked (align around 2 cells) or not (1 cell).
	 * defaultIsHacked is true for A23E, because aligned sets of 2 or 3 cells
	 * need atleast two common excluders in order to produce a hint; larger
	 * sets of cells (A4+E) actually require 1 cell BUT these are much more
	 * numerous and hints are much rarer, so the time/elims equation quickly
	 * goes VERY bad as the size the set increases; to the point where A10E
	 * correct runs for over a day on top1465, producing less than a dozen
	 * eliminations. Piss-poor reward for the effort, so don't use it! By
	 * comparison the "hacked" version uses two excluder cells, which is much
	 * rarer and more likely to hint, so it examines about a tenth of the sets
	 * to produce about a third of the hints. Much better value, at the cost of
	 * missed tricks, which we'll catch later anyways, in the chainers.
	 */
	public AlignedExclusion(Tech tech, boolean defaultIsHacked) {
		super(tech);
		assert tech.name().startsWith("Aligned");
		// my super.degree comes from the passed Tech.
		degreeMinus1 = degree - 1;
		candidates = new Cell[64]; // 64 = 81 - 17 minimum clues
		cStack = new CellStackEntry[degree];
		vStack = new ValsStackEntry[degree];
		for ( int i=0; i<degree; ++i ) {
			cStack[i] = new CellStackEntry();
			vStack[i] = new ValsStackEntry();
		}
		// each CellStackEntry has ONE idx, which is re-used;
		// except level 0's get theres directly from the idx() method.
		for ( int i=1; i<degree; ++i )
			cStack[i].excls = new Idx();
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
		// combinatorial comparisons. Hacked takes about a tenth of the time,
		// and misses about two thirds of the hints. sigh.
		needTwo = Settings.THE.get("isa"+degree+"ehacked", defaultIsHacked);
		// optimisation: create an "Excluderator" ONCE per AlignedExclusion
		// intead of deciding to call idx1 or idx2 repeatedly: once for each
		// cell in each possible combination of $degree cells, ie many times.
		if ( needTwo ) {
			minExcls = 2;
			excluderator = new Excluderator2();
		} else {
			minExcls = 1;
			excluderator = new Excluderator1();
		}
		// NonHinters size really matters! It doesn't grow like a HashMap, it
		// just does it's best with what-ever it's given, but when it gets TOO
		// overloaded it runs like a three-legged dog; and (for reasons I do
		// not fully understand) it slows EVERYthing down when it's too big,
		// so aim for the fastest OVERALL; not the fastest for this hinter.
		// Using multiple AlignedExclusion instances concurrently takes so much
		// RAM the whole app bogs; so never use 7, 8, 9, and 10 concurrently!
		nonHinters = new NonHinters(MAP_SIZE[degree], 4);
		// create numExcludersArray
	}

	/**
	 * We prepare ONCE, after the puzzle is loaded and before the first call to
	 * findHints. The prepare method is defined by the IPreparer interface.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		nonHinters.clear();
		HintValidator.clear();
		/** Set the grid and maybes for {@link Idx#cellsMaybes()} method. */
		Idx.CELLS_MAYBES_VISITOR.grid = grid;
		Idx.CELLS_MAYBES_VISITOR.maybes = EXCLUDERS_MAYBES;
		// reset firstPass to speed-up NonHinters.skip.
		firstPass = true;
	}

	/**
	 * The cleanUp method is called after each Grid has been solved, to allow
	 * hinters to release Cell references, each of which holds the whole grid
	 * in memory. This is the cost of NOT creating array/structs on the fly.
	 * The cleanUp method is defined by the ICleanUp interface.
	 */
	@Override
	public void cleanUp() {
		Arrays.fill(candidates, null);
		Arrays.fill(exclSets, null);
		for ( int i=0; i<degree; ++i )
			cStack[i].cell = null;
	}

	/**
	 * Find first/all Aligned Exclusion hints of $degree size (degree is passed
	 * to my constructor via the Tech) depending on the passed IAccumulator; if
	 * accu.add returns true then I exit-early returning just the first hint
	 * found, else I keep searching to find all available hints.
	 * <p>
	 * Noobs should see class comments for a summary of aligned exclusion, then
	 * study the below pseudocode, then study the code; it'll make no sense
	 * otherwise. It's ungrokable raw, you must cook yourself up to it.
	 * <p>
	 * <pre>
	 * The Aligned Exclusion Sudoku solving technique.
	 * HIGH LEVEL PSEUDOCODE:
	 * foreach set of $degree cells aligned around there common excluder cell/s
	 *   // "combo" ONLY means a possible combination of potential values.
	 * 	 foreach combo of the potential values of the cells in the aligned set
	 *   skipping this presumed value if it is already taken by a sibling cell
	 *     if this combo does NOT contain all of any excluder cells maybes then
	 *       add this combo to allowedValues (the values allowed in each cell)
	 * 	   endif
	 * 	 next combo
	 * 	 for i in 0..degree
	 *     if allowedValues[i] != cells[i].maybes.bits then
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
		// ANSI-C style variables, to reduce stack-work.
		// My "struct" variables are all fields, so they're created ONCE.
		Cell cell; // the current cell
		// We use the cellsStack (an array of structs) to examine each possible
		// combination of $degree cells (an "aligned set"); then the valsStack
		// examines each possible combination of the potential values of the
		// cells in each aligned set (a "combo").
		// COMBINATORIAL * COMBINATORIAL == SLOW!
		CellStackEntry ce; // cellEntry: cells stack entry pointer
		ValsStackEntry ve; // valsEntry: values stack entry pointer
		// the number of cells currently actually in the excluders array. The
		// excluders array is re-used; rather than create a new array for each
		// aligned-set, coz it's faster to avoid creating excessive garbage.
		int numExcls;
		// theExcludersMaybes is only used when there is ONE common excluder.
		// It's a bitset of the common excluder cells potential values.
		// This is faster than iterating an array of 1 element repeatedly.
		int theExcludersMaybes;
		int cl; // cellLevel: the current level in the cell stack
		int vl; // valsLevel: the current level in the values stack
		int i; // general purpose index
		int j; // general purpose index
		// workLevel: all potential values of all the cells to the right of the
		// workLevel have already been allowed, so we don't need to waste time
		// examining them again, so when the first combo is allowed it's enough
		// to enable us to JUMP directly to examining the next potential value
		// of the cell at the "workLevel". So the workLevel is the rightmost
		// level which has maybes that have not yet been allowed.
		int wl;
		// does this alignedSet really have any excluded values?
		boolean anyExcludedValues;

		// presume that no hints will be found
		boolean result = false;

		// find cells that are candidates to participate in exclusion sets,
		// and get the possible excluder-cells of each candidate. A candidate
		// is just an unset cell which has sufficient excluders. An excluder
		// cell is one with 2..degree maybes; so it's maybes can be "covered"
		// by a set of $degree cells. "Sufficient" excluders is determined by
		// the degree and users "hacked" settings. A23E only hint with two
		// excluders, A4+E can hint with one. A4E is correct only. A5+E has a
		// "hacked" registry Setting: 1=correct; 2=hacked is 10 times faster,
		// but produces only about a third of the hints.
		final int lastCandidate;
		if ( needTwo )
			lastCandidate = populateCandidatesAndTwoExcluders(grid) - 1;
		else
			lastCandidate = populateCandidatesAndOneExcluders(grid) - 1;
		if ( lastCandidate < degreeMinus1 )
			return result; // rare but required (else CELL loop is endless)

		// cellStackLevel = the first level
		cl = 0;
		// the first cell at each level is the levels index: trixie.
		for ( i=0; i<degree; ++i )
			cStack[i].index = i;

		// foreach possible combination of candidate cells (an aligned set)
		// CELL_INNER non-loop exists to break instead of continue;ing CELL,
		// because continue is terminally bloody slow!
		CELL: for(;;) CELL_INNER: do {
			// fallback levels while this level has no more cells to search
			while ( cStack[cl].index > lastCandidate ) {
				// this level is exhausted (all cells have been checked)
				// if this is the first level then we're all done
				if ( cl == 0 ) {
					firstPass = false;
					return result; // done all combinations of $degree cells
				}
				// reset this level's index
				// to the cell after the previous level's current cell.
				cStack[cl].index = cStack[cl-1].index + 1;
				// move left one level, to check if that level is exhuasted
				--cl;
			}
			// get this cell, and move index along to the next candidate cell
			cell = candidates[cStack[cl].index++];
			// cl == 0 means this is the first cell in the aligned set, so we
			// HARD set excluders to the CACHED idx returned by idx().
			if ( cl == 0 ) { // the first cell in the aligned set
				// set this cell up in the cellsStack
				(ce=cStack[cl]).cell = cell;
				// remember the complete cell.maybes.bits
				ce.cands = cell.maybes.bits;
				// note that excluders Idx is set directly to the CACHED Idx
				// returned by the idx() method, the Idx is reused next time
				// we examine the excludersSet of this cell.
				ce.excls = exclSets[cell.i].idx();
				// set-up this cell in the valsStack
				// and increment the cellStackLevel to 1
				vStack[cl++].set(cell);
			// do enough (1or2) of dis cells excluders coincide wit dose common
			// to the cells that're already in this aligned set (cStack[cl-1])?
			} else if ( excluderator.get(exclSets[cell.i], cStack[cl].excls
					, cStack[cl-1].excls) ) {
//// Aligned Triple: A2, F2, I2 on C2 G2 (A2-1)
//if ( "A2".equals(cStack[0].cell.id)
//  && "F2".equals(cStack[1].cell.id)
//  && "I2".equals(cStack[2].cell.id)
//)
//	Debug.breakpoint();
				// set-up this cell in the cellsStack
				(ce=cStack[cl]).cell = cell;
				// remember the complete cell.maybes.bits
				ce.cands = cell.maybes.bits;
				// set-up this cell in the valsStack
				vStack[cl].set(cell);
				if ( cl < degreeMinus1 ) {
					// incomplete "aligned set"
					// the next level starts with the next cell (forward-only
					// search). Note that cellStack[cl].index has already been
					// pre-incremented to the index of my next candidate cell.
					cStack[cl+1].index = cStack[cl].index;
					// move right to the next level in the cellStack
					++cl;
				} else {
					// we have a complete "aligned set"
					// 1. ce.excluders.cellsMaybes(): set EXCLUDERS_MAYBES
					//    to maybes.bits of the excluder cells.
					// 2. clean: remove EXCLUDERS_MAYBES which have a maybe
					//    that is not anywhere in the aligned set;
					//    then sort EXCLUDERS_MAYBES by size ASCENDING;
					//    then remove superset/duplicate excluders.
					//    and repopulate the numExcls array with the number of
					//    excluders with size <= each level
					// 3. continue the CELL loop if < minExcls remaining;
					//    which happens "quite often".
					if ( (numExcls=clean(ce.excls.cellsMaybes(), minExcls)) < minExcls )
						break; // to continue the CELL loop;
					// skip this aligned-set if it's already known to not
					// hint, in its current state; but if cell.maybes have
					// changed since last examination then re-examine them;
					// eliminates something like 99.3% re-examinations.
					// NB: skip now includes the excluders in totalMaybes
					if ( !nonHinters.skip(cStack, degree, numExcls, firstPass) ) {
						// valuesLevel = the first
						vl = 0;
						// clear the allowedValues array for this aligned set.
						for ( i=0; i<degree; ++i )
							allowed[i] = 0;
						// reset the first valsStack entry to its first value.
						vStack[0].index = 0;
						// reset the workLevel to the rightmost level
						wl = degreeMinus1;
						if ( numExcls == 1 ) {
							// theres 1 excluder cell which is 98% of cases
							// (except when needTwo, obviously) and 1 excluder
							// can be tested faster, so it's worth duplicating
							// the code; so this code is the same in the if and
							// the else branches, except the "excluders covers"
							// statement/loop.
							// shortcut maybes.bits of the single excluder cell
							theExcludersMaybes = EXCLUDERS_MAYBES[0];
							// DOG_____ING_LOOP (slow): populates allowed.
							// There are two rules used to disallow a combo:
							// 1. if an aligned cell has no maybes then all
							//    combos containing values are not allowed,
							//    ie just skip the bastard.
							// 2. if a combo contains all of a common excluder
							//    cells maybes then this combo is not allowed,
							//    ie just skip the bitch.
							// combos that aren't skipped are allowed.
							//
							// foreach "combo" (a possible combination) of the
							// potential values of the $degree cells in the
							// current aligned set of candidate cells, which
							// all see all of the cStack[cl].excls.
							// The VAL1_INNER non-loop is break;ed instead of
							// continue;ing VAL1 because continue is slow.
							VAL1: for(;;) VAL1_INNER: do {
								// fallback levels while this level is out of values.
								while ( vStack[vl].index >= VSIZE[vStack[vl].cands] )
									if ( --vl < 0 )
										break VAL1; // done all combos of aligned set
								// set the vals entry pointer.
								ve = vStack[vl];
								// set my presumed value, and move index along
								// to the next available value, for next time.
								// NOTE that ve.cands does not include values
								// that are already taken by cells to my left.
								ve.cand = VSHIFTED[ve.cands][ve.index++];
								if ( vl == 0 ) { // first level
									// restore previous cand to slaves,
									// AND remove current cand from slaves.
									// foreach of my slaves (cells to my right)
									for ( j=1; j<degree; ++j ) // slave (right)
										if ( ve.sees[vStack[j].i] )
											// cStack[j].cands is cell.maybes.bits
											vStack[j].cands = cStack[j].cands & ~ve.cand;
									// hard-set the combo to my presumed value
									ve.combo = ve.cand;
									// move right to the next valuesLevel
									// starting with it's first maybe
									vStack[++vl].index = 0;
								} else { // subseqent level
									// restore previous cand to slaves,
									// AND remove current cand from slaves.
									// foreach of my slaves (cells to my right)
									for ( j=vl+1; j<degree; ++j ) { // slave (right)
										// reset slave.cands to cell.maybes.bits
										vStack[j].cands = cStack[j].cands;
										// foreach master (to left) of this slave
										for ( i=0; i<j; ++i ) // master (left)
											// if slave sees master
											if ( vStack[j].sees[vStack[i].i] )
												// slave.cands &= ~master.cand
												// no change if slave !has cand
												vStack[j].cands &= ~vStack[i].cand;
									}
									// build-up the "combo" (a combination of
									// the potential values of the aligned set)
									ve.combo = vStack[vl-1].combo | ve.cand;
									if ( vl < degreeMinus1 ) { // incomplete combo
										// so keep building-up the combo
										// move right to the next values-level
										// and start with its first maybe
										vStack[++vl].index = 0;
									} else { // a complete values combo
										assert vl == degreeMinus1; // NEVER larger
										// if combo does NOT cover the excluder cell
										//   add this combo to allowedValues
										if ( (theExcludersMaybes & ~ve.combo) != 0 ) {
											// all vals right of "workLevel" are already allowed.
											for ( i=0; i<=wl; ++i )
												allowed[i] |= vStack[i].cand;
											// while all values are allowed at the workLevel
											// move the workLevel left one place and try it
											while ( allowed[wl] == cStack[wl].cands ) {
												if ( wl == 0 ) {
													// record that this aligned set did not hint.
													nonHinters.put(); break CELL_INNER; // continue CELL;
												}
												--wl; // test the next value at the workLevel
											}
											vl = wl; // test the next value at the workLevel
										}
									}
								}
							} while ( false); // next potential value
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
							// The VAL2_INNER non-loop is break;ed instead of
							// continue;ing VAL2 because continue is slow.
							VAL2: for(;;) VAL2_INNER : do {
								// fallback levels while this level is out of values.
								while ( vStack[vl].index >= VSIZE[vStack[vl].cands] )
									if ( --vl < 0 )
										break VAL2; // done all combos of this aligned set
								// set the vals entry pointer.
								ve = vStack[vl];
								// set my presumed value, and move the index along to the
								// next available value, for next time.
								ve.cand = VSHIFTED[ve.cands][ve.index++];
								if ( vl == 0 ) { // first level
									// restore previous cand to slaves,
									// AND remove current cand from slaves.
									// foreach of my slaves (cells to my right)
									for ( j=1; j<degree; ++j ) // slave (right)
										if ( ve.sees[vStack[j].i] )
											// cStack[j].cands is cell.maybes.bits
											vStack[j].cands = cStack[j].cands & ~ve.cand;
									ve.combo = ve.cand;
									vStack[++vl].index = 0;
								} else { // subsequent level
									// restore previous cand to slaves,
									// AND remove current cand from slaves.
									// foreach of my slaves (cells to my right)
									for ( j=vl+1; j<degree; ++j ) { // slave (right)
										// reset slave.cands to cell.maybes.bits
										vStack[j].cands = cStack[j].cands;
										// foreach master (to left) of this slave
										for ( i=0; i<j; ++i ) // master (left)
											// if slave sees master
											if ( vStack[j].sees[vStack[i].i] )
												// slave.cands &= ~master.cand
												// no change if slave !has cand
												vStack[j].cands &= ~vStack[i].cand;
									}
									// build-up the combo of values
									ve.combo = vStack[vl-1].combo | ve.cand;
									if ( vl < degreeMinus1 ) { // incomplete combo
										// so keep building-up the combo
										// move right to the next values-level
										// and start with its first maybe
										vStack[++vl].index = 0;
									} else { // a complete values combo
										assert vl == degreeMinus1; // LEVER larger
										// if this combo does NOT cover all of
										// any excluder cells maybes then
										for ( i=0; i<numExcls; ++i )
											if ( (EXCLUDERS_MAYBES[i] & ~ve.combo) == 0 )
												break VAL2_INNER; // continue VAL2;
										// add this combo to the allowedValues array
										// all vals right of "workLevel" are already allowed.
										for ( i=0; i<=wl; ++i )
											allowed[i] |= vStack[i].cand;
										// while all values are allowed at the workLevel
										// move the workLevel left one place and check again
										while ( allowed[wl] == cStack[wl].cands ) {
											if ( wl == 0 ) { // all values allowed!
												// record this aligned set did not hint.
												nonHinters.put(); break CELL_INNER; // continue CELL;
											}
											--wl; // move the workLevel left one place
										}
										vl = wl; // test the next value at the workLevel
									}
								}
							} while ( false ); // next combo of potential values
						}

						// if any of da maybes of da cells in dis aligned set
						// haven't been allowed by atleast 1 combo, then that
						// value may be eliminated from that cell.
						// NB: If all vals are allowed we've already skipped
						// this aligned set, so this is just a double-check
						// which is "guaranteed true"; tiny performance sap
						// coz only happens when we're hinting (very rarely).
						anyExcludedValues = false;
						for ( i=0; i<degree; ++i )
							anyExcludedValues |= allowed[i] != cStack[i].cands;
						if ( anyExcludedValues ) {
							// Yeah! We found an Aligned Exclusion, so create a hint.
							// reds = cells potential values that are not in allowed
							Pots reds = createRedPotentials();
							if ( reds.isEmpty() )
								continue; // Should never happen. Never say never.
							// use a new cell array for each hint; which are few.
							Cell[] cellsA = new Cell[degree];
							for ( i=0; i<degree; ++i )
								cellsA[i] = cStack[i].cell;
							// use a new excluders-cells array too
							Cell[] cmnExcluders = ce.excls.cells(grid
									, new Cell[ce.excls.size()]);
							// build the excluded combos map to go in the hint
							ExcludedCombosMap map = buildExcludedCombosMap(
									cmnExcluders, cellsA, reds);
							// create the hint
							AHint hint = new AlignedExclusionHint(this, reds
									, cellsA, Frmt.ssv(cmnExcluders), map);
							if ( HintValidator.ALIGNED_EXCLUSION_USES ) {
								if ( !HintValidator.isValid(grid, reds) ) {
									hint.isInvalid = true;
									HintValidator.report(tech.name(), grid, hint.toString());
									if ( Run.type != Run.Type.GUI )
										break;
								}
							}
							// and add the bastard to the accumulator
							result = true; // in case add returns false
							if ( accu.add(hint) ) {
								firstPass = false;
								return true;
							}
						} else {
							// record the fact that this aligned set did not produce a
							// hint, so that we can skip-it the next time we see it if
							// all of its maybes are unchanged.
							// NOTE: This should never be executed, but safety first!
							nonHinters.put();
						}
					} // fi
				}
			}
		} while ( false ); // a non-loop
	}

	/**
	 * Populates the candidates array and the excluders array of CellSets.
	 * <p>
	 * Finds Cells (candidates) which have atleast 2 maybes and have atleast 1
	 * siblings (excluders) with maybes.size between 2 and $degree (inclusive).
	 * This method differs from below populateCandidatesAndTwoExcluders just in
	 * the minimum number of required excluders.
	 *
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndOneExcluders(Grid grid) {
		// all empty cells in the grid (cached)
		final Idx empties = grid.getEmpties();
		// find all possible excluders: with maybes.size 2..$degree
		final Idx excluderSized = empties.where(grid.cells, (c) -> {
					return c.maybes.size<degreePlus1;
				  });
		// an Idx of the excluders of each cell
		final Idx cellExcluders = new Idx();
		// clear my output fields
		MyArrays.clear(candidates);
		MyArrays.clear(exclSets);
		int numCandidates = 0;
		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		for ( Cell cell : empties.cells(grid) )
			// if this cell has atleast 1 excluders
			if ( cellExcluders.setAndAny(cell.buds, excluderSized) ) {
				candidates[numCandidates++] = cell;
				exclSets[cell.i] = new CellSet(grid.cells, cellExcluders);
			}
		return numCandidates;
	}

	/**
	 * Populates the candidates array and the excluders array of CellSets.
	 * <p>
	 * Finds Cells (candidates) which have atleast 2 maybes and have atleast 2
	 * siblings (excluders) with maybes.size between 2 and $degree (inclusive).
	 * This method differs from above populateCandidatesAndExcluders just in
	 * the minimum number of required excluders.
	 *
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndTwoExcluders(Grid grid) {
		// all empty cells in the grid (cached)
		final Idx empties = grid.getEmpties();
		// find all possible excluders: with maybes.size 2..$degree
		final Idx excluderSized = empties.where(grid.cells, (c) -> {
					return c.maybes.size<degreePlus1;
				  });
		// an Idx of the excluders of each cell
		final Idx cellExcluders = new Idx();
		// clear my output fields
		MyArrays.clear(candidates);
		MyArrays.clear(exclSets);
		int numCandidates = 0;
		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		for ( Cell cell : empties.cells(grid) )
			// if this cell has atleast 2 excluders
			if ( cellExcluders.setAndMin(cell.buds, excluderSized, 2) ) {
				candidates[numCandidates++] = cell;
				exclSets[cell.i] = new CellSet(grid.cells, cellExcluders);
			}
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
		Pots reds = new Pots();
		// foreach candidate cell in this aligned set
		for ( int i=0,bits; i<degree; ++i )
			// does cell have any maybes that are not allowed at this position?
			if ( (bits=(cStack[i].cands & ~allowed[i])) != 0 )
				// foreach cell maybe that is not allowed
				for ( int v : VALUESES[bits] )
					// Yeah, 'v' can be excluded from 'cell'
					reds.upsert(cStack[i].cell, v);
		assert !reds.isEmpty(); // asserts for techies only: java -ea
		return reds;
	}

	/**
	 * Now we do that dog____ing loop again, except this time we build the
	 * ExcludedCombosMap to go in the hint. This takes "some time", mainly coz
	 * HashMap.put is a bit slow, but this loop is executed about 100 times in
	 * top1465, not several hundred million times like the actual dog____ing
	 * loop, therefore performance is NOT critical.
	 *
	 * @param cmnExcluders
	 * @param cells an array of cells in this aligned set.
	 * @param reds
	 * @return a new ExcludedCombosMap.
	 */
	private ExcludedCombosMap buildExcludedCombosMap(Cell[] cmnExcluders
			, Cell[] cells, Pots reds) {
		ValsStackEntry e; // the current values stack entry (ie stack[l])
		int l, i; // l is stack level, i is a general purpose index
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, reds);
		// I create my own "valsStack" rather than ____ with my callers
		// "valsStack" field, which it's still using when !accu.isSingle.
		final ValsStackEntry[] stack = new ValsStackEntry[degree];
		for ( i=0; i<degree; ++i )
			stack[i] = new ValsStackEntry();
		stack[0].combo = 0; // ie an empty bitset
		l = 0; // the first stack level
		stack[0].index = 0; // start from the cells first potential value
		// copy immutable cell-data from the given cells to the valsStack (vs)
		for ( i=0; i<degree; ++i )
			stack[i].cands = cells[i].maybes.bits;
		VAL: for(;;) {
			// fallback a level while we're out of values at this level
			while ( stack[l].index >= VSIZE[stack[l].cands] ) {
				// reset the index to "restart" this exhausted level
				stack[l].index = 0;
				if ( --l < 0 )
					return map; // we've done all combos of cells values
			}
			e = stack[l];
			// set the "presumed value" of this cell
			// and advance the index to the next potential value of this cell
			e.cand = VSHIFTED[e.cands][e.index++];
			// skip this value if it's already taken by a sibling to my left
			for ( i=0; i<l; ++i ) {
				if ( e.cand == stack[i].cand
				  && cells[l].sees[cells[i].i] ) {
					// all combos containing both of these values are excluded,
					// so unpack the two equal-values into an array at da index
					// of the two sibling cells that cannot have da same value.
					// NOTE: We create a new array because HashA retains it!
					int[] a = new int[degree];
					a[l] = VALUESES[stack[l].cand][0];
					a[i] = VALUESES[stack[i].cand][0];
					// null means exclusion not specific to an excluder cell.
					map.put(new HashA(a), null);
					continue VAL;
				}
			}
			// build-up the combo = the preceeding values + my presumedValue
			if ( l == 0 )
				e.combo = e.cand;
			else
				e.combo = stack[l-1].combo | e.cand;
			if ( l == degreeMinus1 ) { // it's a complete combo
				// if this combo contains all of any excluders possible values
				// then add this "excluded combo" to the map
				for ( Cell x : cmnExcluders ) {
					if ( (x.maybes.bits & ~e.combo) == 0 ) {
						int[] a = new int[degree];
						for ( i=0; i<degree; ++i )
							a[i] = VALUESES[stack[i].cand][0];
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

	/**
	 * Clean-out all defective excludersMaybes.
	 * <pre>
	 * 1. remove excluders which contain any value that is not in any cell in
	 *    the aligned set, because no combo of the potential values of da cells
	 *    in da aligned set will cover it; so it's useless.
	 * 2. sort the remaining maybes by size ASCENDING; Note that bubbleSort
	 *    seems to be MORE efficient than TimSort for small (n&lt;12) arrays.
	 * 3. remove excluders that are a superset of (contain all values in) any
	 *    other excluder, including any duplicates. Every set that covers 125
	 *    also covers 12, so given 12,125 remove 125 without effecting result,
	 *    because 125 just wastes CPU-time in the DOG_____ING loop.
	 */
	private int clean(int numExcls, int minExcls) {
		final int[] a = EXCLUDERS_MAYBES;
		int i, j,J, k;
		boolean any;
		// 1. if all my values are not in the aligned set then remove me
		j = 0; // all values in the aligned set
		for ( i=0; i<degree; ++i )
			j |= cStack[i].cands;
		final int allCands = j;
		for ( i=0; i<numExcls; ++i )
			if ( (a[i] & ~allCands) != 0 ) {
				if ( --numExcls < minExcls )
					return numExcls; // don't both removing
				// remove i: move i-and-all-to-its-right left one spot.
				for ( j=i,J=numExcls; j<J; ++j )
					a[j] = a[j+1];
				--i; // --i then ++i = i; where we moved the data down to.
			}
		// 2. bubbleSort 'numExcls' EXCLUDERS_MAYBES by size ASCENDING
		if ( numExcls > 1 )
			for ( i=numExcls; i>1; --i ) { // the right cell EXCLUSIVE
				any = false;
				for ( j=1; j<i; ++j ) // the left cell INCLUSIVE
					// if previous k=j-1 is larger than current j then swap
					if ( VSIZE[a[k=j-1]] > VSIZE[a[j]] ) {
						J = a[j];
						a[j] = a[k];
						a[k] = J;
						any = true;
					}
				if ( !any )
					break;
			}
		// 3. if I'm a superset of any preceedent excluder then remove me.
		for ( i=0; i<numExcls-1; ++i ) // foreach except last
			for ( j=i+1; j<numExcls; ++j ) // foreach subsequent
				if ( (a[i] & ~a[j]) == 0 ) {
					if ( --numExcls < minExcls )
						return numExcls; // don't bother removing
					// remove j: move j-and-all-to-its-right left one spot.
					for ( k=j,J=numExcls; k<J; ++k )
						a[k] = a[k+1];
					--j; // --j then ++j = j; where I moved the data down to.
				}
		return numExcls;
	}

	static final class CellStackEntry {
		int index; // the index of the current in the candidates array
		Cell cell; // the cell at this level 1..degree in the stack
		Idx excls; // excludersIdx: excluders common to all cells in an aligned set
		int cands; // remember the complete cell.maybes.bits
		// @check: commented out: for debugging only
		@Override
		public String toString() {
			if ( cell == null )
				return "-";
			return ""+index+":"+cell.toFullString()+"->"+excls;
		}
	}

	static final class ValsStackEntry {
		// a bitset of the selectable potential values of cStack[me].cell;
		// so each time a presumed cell value is selected (in a master) we
		// remove that value from all sibling cells to it's right (slaves)
		// in the vStack {@code vStack[slave].hotCands & ~vStack[master].cand}
		// AFTER I change my value: each sibling to my right (slave) rebuilds
		// his cands from cStack[slave].cands and cells to his left (masters).
		int cands;
		// the index of the current/next presumedValue in maybes
		int index;
		// the potential value we presume this cell holds.
		// This value is, confusingly, a left-shifted bitset representation,
		// for the convenience of just "or-ing" it into the combo.
		int cand;
		// a bitset of the value of this and all cells to my left
		int combo;
		// ---- additions for speed, and comfort ----
		// the indice of my cell in the grid: cells[i].cell.i, rather than
		// repeatedly do the ugly dereferencing; so we can process the vals
		// without needing to refer back to the cellStack.
		int i;
		// my cell's sees array, rather than repeatedly cells[vl].cell.sees.
		boolean[] sees;
		// set the given cells attributes in this ValsStackEntry.
		void set(Cell cell) {
			i = cell.i;
			cands = cell.maybes.bits; // default to the full set of maybes
			sees = cell.sees;
		}
		// @check commented out: for debugging only
		@Override
		public String toString() {
			return diuf.sudoku.Values.toString(cand);
		}
	}

	/**
	 * IExcluderator interface allows us to do "hacked" or "correct" without
	 * too much bother in the rest of the code. There's two implementations:
	 * Excluderator1 or Excluderator2; each instance consistently uses either
	 * of these implementations; there's no swapping on the fly. To change a
	 * "hacked" setting we need to recreate the whole LogicalSolver. sigh.
	 */
	private static interface IExcluderator {
		/**
		 * common is {@code return !excluders.idx1(dest, src);}<br>
		 * or {@code return !excluders.idx2(dest, src);}.
		 * <p>
		 * So we call idx1/2 on the given CellSet, and negate it's return value
		 * to return "are there sufficient common excluders between src and
		 * dest, setting dest to the union (and) of src and excluders.idx().
		 * @param excluders
		 * @param dest
		 * @param src
		 * @return
		 */
		boolean get(CellSet excluders, Idx dest, Idx src);
	}

	/**
	 * This excluderator aligns around a single cell, which I call "correct".
	 */
	private static class Excluderator1 implements IExcluderator {
		@Override
		public boolean get(CellSet excluders, Idx dest, Idx src) {
			return !excluders.idx1(dest, src);
		}
	}

	/**
	 * This excluderator aligns around two cells, which I call "hacked".
	 * Aligned 2 and 3 exclusion actually require 2 cells. A4+E can (rarely)
	 * produce hints using a single excluder cell; so the user chooses this
	 * one for A5+E using the "hacked" check box in the GUI.
	 */
	private static class Excluderator2 implements IExcluderator {
		@Override
		public boolean get(CellSet excluders, Idx dest, Idx src) {
			return !excluders.idx2(dest, src);
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
		private final Pots reds;

		/**
		 * Constructs a new ExcludedCombosMap whose put method uses the
		 * {@link AlignedExclusionHint#isRelevant} method to determine if the
		 * combo is relevant to the hint, thus averting irrelevant hints.
		 * @param cells
		 * @param reds
		 */
		ExcludedCombosMap(Cell[] cells, Pots reds) {
			super(128, 0.75F);
			this.cells = cells;
			this.reds = reds;
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
			if ( !AlignedExclusionHint.isRelevant(cells, reds, key.array) )
				return null;
			return super.put(key, value);
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
