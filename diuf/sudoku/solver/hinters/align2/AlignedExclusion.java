/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.Validator;
import diuf.sudoku.utils.MyArrays;
import static diuf.sudoku.utils.Frmt.MINUS;
// JAPI
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * AlignedExclusion implements the Aligned Exclusion Sudoku solving technique
 * for sets of between 2 and 10 cells. AlignedExclusion is too slow, so I must
 * warn you to not use it. Don't use AlignedExclusion!
 * <p>
 * First, my terminology:<ul>
 * <li>Aligned Exclusion eliminates potential values from cells in an aligned
 * set.
 * <li>A "candidate" is an unset cell which sees (same box, row, or col) the
 * requisite number of excluder cells.
 * <li>The "excluder cells" are stored in sets indexes by the candidate cell
 * which sees them all. An "excluder cell" has 2..degree potential values, such
 * that an aligned set of degree cells may contain all of the potential values
 * of an excluder cell (which they all see) and that's disallowed, because
 * every cell in the solution must have a value.
 * <li>An "aligned set" is a possible combination of the candidate cells that
 * all see enough common excluder cells.
 * <li>The minimum number of common excluder cells is 1 or 2, depending on the
 * size of the aligned set, and on the users "hacked" setting. An aligned set
 * of 2 or 3 cells only hints when aligned around atleast two excluders.
 * Aligned sets of 4 to 10 cells CAN still hint when aligned around one cell,
 * but it's rare. About a third of hints come from da approximately two percent
 * of sets that are aligned around two-or-more common excluder cells.
 * <li>A "combo" is a possible combination of the potential values of the cells
 * in an aligned set.
 * </ul>
 * AFAIK, there's only one way to do exclusion, and it's bloody expensive. We
 * iterate all the possible combinations of the potential values (a "combo") of
 * each aligned set to see if each combo is allowed (rules below). If so record
 * that each cells presumed value is allowed in this cell (that's important);
 * then once all combos have been examined we check if all of the potential
 * values of each cell have been allowed, and if not then we have found an
 * exclusion, so we raise a hint.
 * <p>
 * To be clear, when a combo is disallowed we take no action, only when it IS
 * allowed do we record the allowed value of each cell, so that at the end we
 * can see if all of the potential values of each cell have been allowed by ANY
 * combo. Potential values that have not been allowed are "excluded".
 * <p>
 * It sounds simple enough; but the implementation is complicated by trying to
 * make an O(combinatorial*combinatorial) algorithm faster, to make it usable,
 * at which I have failed, thus far. So don't use AlignedExclusion! It's slow!
 * <p>
 * There are two rules used to disallow a combo:<ul>
 * <li>Aligned set cells which see each other cannot contain the same value; a
 *  concept which I call the "one per region" rule. If a cell has no remaining
 *  potential values because all of its potential values are already taken by
 *  preceeding cells (that it sees) in the aligned set then the combo is not
 *  allowed; indeed all possible combos containing these values in these cells
 *  are disallowed.
 * <li>Because all aligned set cells see all of the excluder cell/s, the cells
 *  in the aligned set cannot contain ALL of the potential values of any common
 *  excluder cell, because every cell must have a value; when that occurs then
 *  this combo is disallowed.
 * </ul>
 * <p>
 * I call aligning around a single cell "correct" coz it finds all available
 * hints; and I call aligning 5 to 10 cells around two or more cells "hacked"
 * coz it's a hack. Sets aligned around multiple excluder cells are much much
 * more likely to produce a hint; so "hacked mode" processes about a tenth of
 * the aligned sets to produce about a third of the hints. This matters because
 * AlignedExclusion is innately a slow process; so most users simply will not
 * wait for the correct version of the algorithm to run over larger sets; but
 * they may wait for the hacked version of 5, 6, or even 7 cells; but 8, 9 and
 * 10 cells are still too slow, even hacked, in my humble opinion.
 * A23E are now fast enough correct (aligned around a single), and A4E is now
 * always correct because it's also now deemed "fast enough".
 * <p>
 * Note that this implementation of AlignedExclusion uses the "stack iteration"
 * technique (twice) taken from the ALS's in HoDoKu, by hobiwan. HoDoKu is
 * another open source Sudoku solver. Download from the Sudoku Explainer
 * homepage on SourceForge (my version is faster for nets).
 * <p>
 * AlignedExclusion is MUCH more compact than the Aligned*Exclusion classes,
 * which are mostly boiler-plate code. AlignedExclusion is maintainable, but it
 * is tricky and slower. The old Aligned5Exclusion runs in 70 seconds, and this
 * version runs in 211 seconds (A5E top1465), ie THREE TIMES as long! BFFIIK!
 * <p>
 * Be warned that this class was a total-pain-in-the-ass to write and debug.
 * It's too complex. ____ with it at your peril! And keep your towel handy, for
 * unexpected emissions, obviously. Not for hitch-hiking. Never hitch-hiking.
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
 * -- Commonator interface --
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
 * 2021-01-23 12:57 AlignedExclusion is now faster for A2..4E but is slower for
 * A5..10E, so the old "align" package is still preferred for the big A*E's;
 * align2 is ALWAYS used for A234E. Here's the latest comparisons.
 * Note: A2E, A9E, and A10E are left out coz they don't hint.
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
 *
 * 2021-08-06 I was just pissing about with an explanation of used excluders
 * and I notice that, for some reason it's much faster than it was previously,
 * so I decided to add a comment to that effect. It's faster, but there ARE
 * now less Aligned hints, because ALS_* finds most of them.
 *       306,924,100  3703       82,885   0              0 AlignedPair
 *     2,315,394,500  3703      625,275   3    771,798,166 AlignedTriple
 *     8,646,212,200  3700    2,336,814   3  2,882,070,733 AlignedQuad
 *
 * 2021-09-08 decomposed CellStackEntry into parallel arrays, for speed; so we
 * repeatedly array-look-up rather than repeatedly dereference the struct,
 * because array-lookups are a bit faster than dereferencing (I know not why).
 *       334,794,000  3745       89,397   0              0 AlignedPair
 *     2,619,893,400  3745      699,571   3    873,297,800 AlignedTriple
 *    11,812,167,100  3742    3,156,645   3  3,937,389,033 AlignedQuad
 *    88,538,724,700  3739   23,679,787   6 14,756,454,116 Aligned5Exclusion_1C
 *   251,284,142,200  3733   67,314,262   4 62,821,035,550 Aligned6Exclusion_1C
 *    20,932,834,200  3729    5,613,524   2 10,466,417,100 Aligned7Exclusion_2H
 *    20,565,831,500  3727    5,518,065   2 10,282,915,750 Aligned8Exclusion_2H
 *
 * 2021-09-16 USE_NEW_ALIGN2=true to use me (align2.AlignedExclusion) for all
 * 1465	  248,470,988,000	(04:08)	      169,604,769
 * with stack references to heap fields is 1.8 seconds faster overall
 *       233,775,600  3753       62,290   0              0 AlignedPair
 *     1,796,749,300  3753      478,750   3    598,916,433 AlignedTriple
 *     8,629,548,500  3750    2,301,212   3  2,876,516,166 AlignedQuad
 *    25,308,465,000  3747    6,754,327   0              0 AlignedPent 2H
 *    48,601,280,600  3747   12,970,718   1 48,601,280,600 AlignedHex 2H
 *    70,076,336,000  3746   18,706,977   2 35,038,168,000 AlignedSept 2H
 * 1465	  246,685,752,100	(04:06)	      168,386,178
 * </pre>
 *
 * @author Keith Corlett 2020-12-10 created
 */
public class AlignedExclusion extends AHinter
implements diuf.sudoku.solver.hinters.IPreparer
//		, diuf.sudoku.solver.hinters.IReporter
		, diuf.sudoku.solver.hinters.IAfter
{
//	public void report() {
//		if ( tech == Tech.AlignedQuad ) {
//			Log.teeln("AlignedQuad: align2.NonHinters.cnt="+diuf.sudoku.solver.hinters.align2.NonHinters.cnt);
//		}
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

	// EXCLUDERS_MAYBES: excluder cells maybes; there's ONE static array shared
	// via Idx#cellMaybes by all instances of AlignedExclusion, which cannot
	// execute concurrently. Package visible to read in NonHinters#skip. This
	// array logically belongs to Idx, but created only when AE is referenced.
	static final int[] EXCLUDERS_MAYBES = new int[20];

	// candidates is an Idx of empty cells with sufficient excluder cells.
	// An excluder cell is a cell with 2..$degree maybes, so that it's maybes
	// can be "covered" by a set of $degree cells. "Covered" means that all the
	// potential values of the common excluder cell are taken by candidates,
	// so that this "combo" (distinct potential values of $degree cells) is
	// not allowed. Then having examined all "combos", if no "combo" allows a
	// candidates potential value then that value has been "excluded", and so
	// is eliminated by the hint.
	private final Idx candidates = new Idx();

	// the set of excluder-cells for each candidate cell.
	// My array index is the indice of the candidate cell.
	private final CellSet[] excls = new CellSet[GRID_SIZE];

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
	private final CellStackEntry[] cS;

	// an array of $degree bitsets of the current presumed values of cells in
	// the aligned set. You could call this variable valuesStack.

	// These coincident arrays constitute "the values stack", which used to be
	// an array of ValsStackEntry, but these parallel arrays are a bit faster
	// because we avoid repeatedly dereferencing the structure, which requires
	// many more array-look-ups, which are a bit faster than dereferencing.
	// The downside is this harder to follow, requiring more explanation.
	private final int[] vsCands;        // values stack cands
	private final int[] vsIndexes;      // values stack indexes
	private final int[] vsCand;         // values stack cand
	private final int[] vsCombo;        // values stack combo
	private final int[] vsIndices;      // values stack indices
	private final boolean[][] vsSees;   // values stack sees arrays
	// a bitset of the values which have been allowed in each candidate cell in
	// the aligned set.
	private final int[] allowed;

	// the minimum number of excluder-cells required to align a set around
	private final int minExcls;

	// calls excluders.idx1(...) or idx2(...)
	private final ICommonator common;

	// hashCode of the aligned set => total maybes, to skip each set of cells
	// which have already been checked in there current state, which is faster,
	// but still not fast enough... the individual A*E classes are faster.
	private final NonHinters nonHinters;

	// hacked: do we aligned sets around 2-or-more excluder cells?
	// Always true for A23E; false for A4E; "hacked" setting for A5+E.
	private final boolean hacked;

	// is this the first run of this instance of AE through this puzzle?
	private boolean firstPass = true;

	/**
	 * The constructor. I read my degree (number of cells in an aligned set)
	 * from the given Tech, which MUST be a Tech.Aligned*.
	 *
	 * @param tech to implement: Tech.Aligned*
	 */
	public AlignedExclusion(Tech tech) {
		super(tech);
		assert tech.isAligned;
		cS = new CellStackEntry[degree];
		for ( int i=0; i<degree; ++i ) {
			cS[i] = new CellStackEntry();
		}
		// these parallel arrays are a decomposed "values stack array",
		// coz array-look-ups are a bit faster than derefencing-structs
		vsCands = new int[degree];
		vsIndexes = new int[degree];
		vsCand = new int[degree];
		vsCombo = new int[degree];
		vsIndices = new int[degree];
		vsSees = new boolean[degree][];
		// subsequent CellStackEntry's have ONE idx, that is re-used
		// (cS[0].excls is assigned to the new Idx returned by idx())
		for ( int i=1; i<degree; ++i )
			cS[i].excls = new Idx();
		// an array of bitsets of the values that are known to be allowed in
		// each cell in the aligned set.
		allowed = new int[degree];
		// hacked settings:
		// A23E always has two excluders;
		// A4E finds more hints correct, and is fast enough;
		// A5+E are user choice: I hack them when I use AlignedExclusion;
		// A56E are "a bit slow" correct;
		// A7+E are "too slow" correct.
		//
		// The minimum number of excluder cells required is 1 or 2 depending on
		// the size of the aligned set, and the users "hacked" setting. An
		// aligned set of 2 or 3 cells NEEDS two excluders to produce a hint,
		// but 4-or-more aligned cells require just one excluder, but the user
		// can select "hacked" for Aligned5+Exclusion, to produce about a third
		// of the hints in about a tenth of the time. This matters because A*E
		// is inherently a heavy process, because it does combinatorial TIMES
		// combinatorial comparisons. Hacked takes about a tenth of the time,
		// and misses about two thirds of the hints. sigh.
		// if it's not-wanted by default then it's hacked by default.
		hacked = degree<4
			  || THE_SETTINGS.getBoolean("isa"+degree+"ehacked", !tech.defaultWanted);
		// hacked: create a Commonator ONCE per instance, instead of deciding
		// wether to call idx1 or idx2 once for each cell in each possible
		// combination of degree cells, ie many many times.
		if ( hacked ) {
			minExcls = 2;
			common = new Commonator2();
		} else {
			minExcls = 1;
			common = new Commonator1();
		}
		// NonHinters size matters. It doesn't grow like a HashMap, it does its
		// best with what-ever its given, but when it gets TOO overfull it runs
		// really slowly; and (for reasons I do not understand) it slows EVERY
		// thing down when its too big (underfull), so aim for fastest OVERALL,
		// not just the fastest for each hinter.
		// Using > 2 large AlignedExclusion's at one time uses so much RAM the
		// whole app starts to bog-down! So they're best not used at all. sigh.
		nonHinters = new NonHinters(MAP_SIZE[degree], 4);
	}

	/**
	 * We prepare after puzzle is loaded, and before the first findHints.
	 * The prepare method is defined by the IPreparer interface.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		nonHinters.clear();
		Validator.clear();
		// the next getHints will be the firstPass through this puzzle,
		// to speed-up NonHinters.skip.
		firstPass = true;
	}

	/**
	 * The tidyUp method runs after each puzzle is solved.
	 * The tidyUp method is defined by the ITidyUp interface.
	 */
	@Override
	public void after() {
		Arrays.fill(excls, null);
		for ( int i=0; i<degree; ++i )
			cS[i].cell = null;
		Cells.cleanCasA();
	}

	/**
	 * Find first/all Aligned Exclusion hints of $degree size (degree is passed
	 * to my constructor via the Tech) depending on the passed IAccumulator; if
	 * accu.add returns true then I exit-early returning just the first hint
	 * found, else I keep searching to find all available hints.
	 * <p>
	 * Noobs should see class comments for a summary of aligned exclusion, then
	 * study the below pseudocode, then study the code; it'll make no sense
	 * otherwise. It's ungrocable raw, you must cook yourself up to it. I have
	 * commented this class extensively, for comprehensibility. Good luck!
	 * <p>
	 * <pre>
	 * The Aligned Exclusion Sudoku solving technique.
	 * HIGH LEVEL PSEUDOCODE:
	 * foreach set of $degree cells aligned around there common excluder cell/s
	 * 	 foreach combo of the potential values of the cells in the aligned set
	 *   skipping each presumed value that is already taken by a sibling
	 *     if this combo does NOT contain all of any excluder cells maybes then
	 *       add this combo to allowedValues (the values allowed in each cell)
	 * 	   endif
	 * 	 next combo
	 * 	 for i in 0..degree
	 *     if allowedValues[i] != cells[i].maybes then
	 * 	     any exclusions = true
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
	 * @return any hint/s found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// ANSI-C style variables, to reduce stack-work.
		// We use the cellsStack (an array of structs) to examine each possible
		// combination of degree cells (an aligned set); then we use valsStack
		// to examine each possible combination of the potential values of the
		// cells in each aligned set (a combo)
		// COMBINATORIAL * COMBINATORIAL == SLOW!
		CellStackEntry ce; // cellEntry: cells stack entry pointer
		int ii // the indice of the current cell
		// the number of cells currently actually in the excluders array. The
		// excluders array is re-used; rather than create a new array for each
		// aligned-set, coz it's faster to avoid creating excessive garbage
		, n
		// x (theExcludersMaybes) is used only when theres ONE common excluder
		// x is a bitset of the common excluder cells potential values
		// This is faster than iterating an array of 1 element repeatedly
		, x
		, cl // cellLevel: the current level in the cell stack
		, vl // valsLevel: the current level in the values stack
		, i, j // indexes, hijacked as ve.combo for speed
		// workLevel: all potential values of all the cells to the right of the
		// workLevel have already been allowed, so we don't need to waste time
		// examining them again, so when the first combo is allowed it's enough
		// to enable us to JUMP directly to examining the next potential value
		// of the cell at da workLevel. So the workLevel is the rightmost level
		// which has maybes that have not yet been allowed.
		, wl
		, cand; // faster ve.cand
		boolean ok; // is this combo allowed
					// hijacked for does alignedSet have any excluded values?

		// presume that no hints will be found
		boolean result = false;

		// find cells that are candidates to participate in exclusion sets, and
		// get the possible excluder-cells of each candidate. A candidate is an
		// unset cell which has sufficient excluders. Degree is numCells in
		// each aligned set. An excluder cell is one with 2..degree maybes;
		// whose maybes can be "covered" by an "aligned set" of degree cells.
		// "Sufficient" excluders is determined by the degree and the "hacked"
		// setting. A23E only hint with two excluders, and A4+E hint with one.
		// A4E is correct only, because it's fast enough. Each A5+E has its own
		// hacked setting: false=correct, or true=hacked (ie needTwo is true)
		// is ten times faster to produce about a third of the hints.
		final int last; // index of last candidate (numCandidates - 1)
		if ( hacked ) {
			last = populateCandidatesAndTwoExcluders(grid) - 1;
		} else {
			last = populateCandidatesAndOneExcluders(grid) - 1;
		}
		if ( last < degreeMinus1 ) {
			return result; // rare but required (else CELL loop is endless)
		}

		// stack references to heap fields (1.8 seconds faster top1465)
		final Cell[] cells = grid.cells;
		final int[] maybes = grid.maybes;
		final CellSet[] excls = this.excls;
		final CellStackEntry[] cS = this.cS;
		final int[] vsCands = this.vsCands;
		final int[] vsIndexes = this.vsIndexes;
		final int[] vsCand = this.vsCand;
		final int[] vsCombo = this.vsCombo;
		final int[] vsIndices = this.vsIndices;
		final boolean[][] vsSees = this.vsSees;
		final ICommonator common = this.common;
		final int[] allowed = this.allowed;
		final NonHinters nonHinters = this.nonHinters;
		final int degree = this.degree;
		final int degreeMinus1 = this.degreeMinus1;
		final int minExcls = this.minExcls;

		// indices of candidate cells to be read repeatedly
		final int[] indices = candidates.toArrayA();

		// set-up the Idx.MAYBES_VISITOR for call to ce.excls.maybes()
		// NOTE: these MUST be cleared in a finally block
		Idx.MAYBES_VISITOR.maybes = EXCLUDERS_MAYBES;
		Idx.MAYBES_VISITOR.cells = cells;
		try {
			// the first cellStack level is 0
			cl = 0;
			// tricky: the first cell at each level is this levels index.
			for ( i=0; i<degree; ++i ) {
				cS[i].index = i;
			}

			// foreach possible aligned set of candidate cells
			CELL: for(;;) {
				// CELL_NONLOOP exists to break instead of continue;ing CELL,
				// coz break is faster than continue. sigh.
				CELL_NONLOOP: for(;;) {
					// fallback levels while this level is exhausted
					while ( (ce=cS[cl]).index > last ) {
						// this level is exhausted (all cells have been checked)
						// if this is the first level then we're done
						if ( cl == 0 ) {
							return result; // done
						}
						// reset this level's index to the cell after the previous
						// level's current cell; and move left one level
						ce.index = cS[--cl].index + 1;
					}
					// get this cell, incrementing index for next time
					// and set this cell up in the cellStack
					vsSees[cl] = (ce.cell=cells[ii=vsIndices[cl]=indices[ce.index++]]).sees;
					ce.maybes = vsCands[cl] = maybes[ii];
					// if this is the first cell in the aligned set then
					// assign excls to the CACHED idx returned by idx().
					if ( cl == 0 ) { // the first cell in the aligned set
						ce.excls = excls[ii].idx();
						cl++; // and move right in the cellStack
					// else it's a subsequent cell in the aligned set only if
					// enough (1 or 2) of this cells excluders coincide with
					// those common to the cells already in this aligned set.
					} else if ( common.idx(excls[ii], cS[cl-1].excls, ce.excls) ) {
						if ( cl < degreeMinus1 ) {
							// incomplete "aligned set"
							// cStack[cl].index is already incremented to index
							// of my next candidate cell, so the next level
							// starts at the next cell (forward-only search).
							// then move right to next level in the cellStack
							cS[cl+1].index = cS[cl++].index;
						} else {
							// we have a complete "aligned set"
							// Next-line is crazy. Here's an explanation:
							// 1. ce.excluders.maybes sets EXCLUDERS_MAYBES to
							//    the current excluder cells maybes
							// 2. clean deletes EXCLUDERS_MAYBES which have a maybe
							//    that is not anywhere in the aligned set;
							//    then sort EXCLUDERS_MAYBES by size ASCENDING;
							//    then remove superset/duplicate excluders;
							// 3. continue the CELL loop if < minExcls remaining;
							//    which happens "quite often".
							if ( (n=clean(ce.excls.maybes())) < minExcls ) {
								break; // to continue the CELL loop;
							}
							// skip this aligned-set if already known to not hint
							// in current state; reexamine if maybes of aligned set
							// or excluder cells have changed since last exam.
							// Avert ~99.3% reexams so faster despite slow Hash get
							// NB: skip now includes excluders in totalMaybes. sigh
							if ( !nonHinters.skip(cS, degree, n, firstPass) ) {
								// valuesLevel = the first
								vl = 0;
								// clear the allowedValues array for this aligned set.
								for ( i=0; i<degree; ++i ) {
									allowed[i] = 0;
								}
								// reset the first valsStack entry to its first value.
								vsIndexes[0] = 0;
								// reset the workLevel to the rightmost level. The
								// workLevel is the level that we're working on, ie
								// all values to the right of workLevel are already
								// allowed, so that each combo we test contains a
								// value that's not already allowed, for speed.
								wl = degreeMinus1;
								if ( n == 1 ) {
									// theres 1 excluder cell which is ~98% of cases
									// and 1 excluder can be tested faster, so its
									// worth duplicating code; so this code is the
									// same in the if and the else branches, except
									// the "excluders covers" if/loop.
									// shortcut maybes of the single excluder cell
									x = EXCLUDERS_MAYBES[0];
									// DOG_____ING_LOOP (slow): populates allowed.
									// There are two rules used to disallow a combo:
									// 1. if an aligned cell has no maybes then all
									//    combos containing values are not allowed,
									//    so skip this combo.
									// 2. if a combo contains all of a common excluder
									//    cells maybes then this combo is not allowed,
									//    so skip this combo.
									// Any combo that isn't skipped is allowed.
									//
									// foreach combo: a possible combination of the
									// potential values of the degree cells in the
									// current aligned set of cells, which all see
									// all of the cStack[cl].excls.
									VAL1: for(;;) {
										// while level exhausted fallback a level
										while ( vsIndexes[vl] >= VSIZE[vsCands[vl]] ) {
											if ( --vl < 0 ) {
												break VAL1; // done
											}
										}
										// set the presumed value of current cell,
										// incrementing index for next time.
										// nb: ve.cands excludes values held by
										// cells to my left in stack which I see.
										cand = vsCand[vl] = VSHIFTED[vsCands[vl]][vsIndexes[vl]++];
										if ( vl == 0 ) { // first level
											// foreach slave (cell to my right in
											// stack that I see) remove my presumed
											// value from slave.cands
											for ( j=1; j<degree; ++j ) {
												if ( vsSees[vl][vsIndices[j]] ) {
													vsCands[j] = cS[j].maybes & ~cand;
												}
											}
											// set combo = my presumed value
											vsCombo[vl] = cand;
											// move right to the next valuesLevel
											// starting with it's first maybe
											vsIndexes[++vl] = 0;
										} else { // subseqent level
											// foreach slave (cell to my right)
											// reset slave.cands to cell.maybes and
											// remove each master.cand from slave
											for ( j=vl+1; j<degree; ++j ) { //slave
												vsCands[j] = cS[j].maybes;
												for ( i=0; i<j; ++i ) { //master
													if ( vsSees[j][vsIndices[i]] ) {
														vsCands[j] &= ~vsCand[i];
													}
												}
											}
											// build-up combo: this combination of the
											// potential values of cells in the aligned set
											vsCombo[vl] = vsCombo[vl-1] | cand;
											if ( vl < degreeMinus1 ) { //incomplete
												// so keep building-up combo, ie:
												// move right to the next vl and
												// start with its first maybe
												vsIndexes[++vl] = 0;
											} else { // complete combo
//												assert vl == degreeMinus1;
												// "excluders covers"
												// if combo contains all excluders
												// then combo NOT allowed
												if ( (x & ~vsCombo[vl]) != 0 ) {
													// this combo is allowed
													// right of wl already allowed
													for ( i=0; i<=wl; ++i ) {
														allowed[i] |= vsCand[i];
													}
													// while all allowed at workLevel
													// move workLevel left one place
													while ( allowed[wl] == cS[wl].maybes ) {
														if ( wl == 0 ) {
															// nothing to see here
															nonHinters.put();
															// continue CELL;
															break CELL_NONLOOP;
														}
														// move wl left one place
														// and test the next value
														// at wl, so dat each combo
														// we test contains a value
														// that hasnt been allowed.
														vl = --wl;
													}
												}
											}
										}
									}
								} else {
									// about 2% of aligned sets have > 1 excluders, so we just
									// loop through the excluder-cells, which is a bit slower.
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
										// fallback levels while this level is exhausted.
										while ( vsIndexes[vl] >= VSIZE[vsCands[vl]] ) {
											if ( --vl < 0 ) {
												break VAL2; // done all combos of this aligned set
											}
										}
										// set my presumed value, increment index
										// to next available value, for next time.
										vsCand[vl] = VSHIFTED[vsCands[vl]][vsIndexes[vl]++];
										if ( vl == 0 ) { // first level
											// restore previous cand to slaves,
											// AND remove current cand from slaves.
											// foreach of my slaves (cells to my right)
											for ( j=1; j<degree; ++j ) { // slave (right)
												if ( vsSees[vl][vsIndices[j]] ) {
													vsCands[j] = cS[j].maybes & ~vsCand[vl];
												}
											}
											vsCombo[vl] = vsCand[vl];
											vsIndexes[++vl] = 0;
										} else { // subsequent level
											// foreach slave (cell to my right)
											// reset slave.cands to cell.maybes AND
											// remove each master.cand from slave.
											for ( j=vl+1; j<degree; ++j ) { //slave
												vsCands[j] = cS[j].maybes;
												for ( i=0; i<j; ++i ) { //master
													if ( vsSees[j][vsIndices[i]] ) {
														vsCands[j] &= ~vsCand[i];
													}
												}
											}
											// build-up the combo of values
											vsCombo[vl] = vsCombo[vl-1] | vsCand[vl];
											if ( vl < degreeMinus1 ) { //incomplete
												// so keep building-up the combo
												// move right to next values-level
												// and start with its first maybe
												vsIndexes[++vl] = 0;
											} else { // a complete values combo
//												assert vl == degreeMinus1;
												// "excluders covers"
												// if this combo is a superset of
												// all of any excluder cells maybes
												// then this combo is NOT allowed
												// (every cell must have a value).
												for ( ok=true,j=vsCombo[vl],i=0; i<n; ++i ) {
													if ( (EXCLUDERS_MAYBES[i] & ~j) == 0 ) {
														ok = false;
														break;
													}
												}
												if ( ok ) {
													// combo allowed, so add it to allowed array;
													// all values right of wl are already allowed.
													for ( i=0; i<=wl; ++i ) {
														allowed[i] |= vsCand[i];
													}
													// while all values are allowed at the workLevel
													// move the workLevel left one, so that all vals
													// to right of workLevel are already allowed.
													while ( allowed[wl] == cS[wl].maybes ) {
														if ( wl == 0 ) { // all values allowed!
															// nothing to see here
															nonHinters.put();
															// continue CELL;
															break CELL_NONLOOP;
														}
														// move the workLevel left one place, and we
														// test the next value at workLevel, so that
														// each combo we test (probably) contains a
														// value that has not yet been allowed.
														vl = --wl;
													} // wend
												} // fi allow
											} // fi
										} // fi
									} // next combo of values
								} // fi

								// if any of da maybes of da cells in dis aligned
								// set haven't been allowed by atleast 1 combo,
								// den dat value may be eliminated from dat cell.
								// NB: If all vals are allowed weve already skipped
								// this aligned set, so this is just a double-check
								// which is guaranteed true; tiny performance cost
								// coz only happens when were hinting (rarely).
								ok = false;
								for ( i=0; i<degree; ++i ) {
									ok |= allowed[i] != cS[i].maybes;
								}
								if ( ok ) {
									// FOUND AlignedExclusion, so create a hint.
									// reds = potential values not in allowed
									final Pots reds = createRedPotentials();
									// reds should never be empty. Never say never
									if ( !reds.isEmpty() ) {
										// new cell array for each hint (few).
										final Cell[] mine = new Cell[degree];
										for ( i=0; i<degree; ++i ) {
											mine[i] = cS[i].cell;
										}
										// new excluders array too
										final Cell[] excluders = ce.excls.cells(cells);
										// build ExcludedCombosMap for the hint
										final ExcludedCombosMap map = newExcludedCombosMap(
												excluders, mine, reds);
										// create the hint
										final AHint hint = new AlignedExclusionHint(
												this, reds, mine, excluders, map);
										if ( Validator.ALIGNED_EXCLUSION_VALIDATES ) {
											if ( !Validator.isValid(grid, reds) ) {
												hint.isInvalid = true;
												Validator.report(tech.name(), grid, hint.toString());
												if ( Run.type != Run.Type.GUI ) {
													break;
												}
											}
										}
										// and add the bastard to the accumulator
										result = true; // in case add returns false
										if ( accu.add(hint) ) {
											return result;
										}
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
					break; // NON_LOOP
				}
				interrupt();
			} // next combo of candidate cells // next combo of candidate cells
		} finally {
			firstPass = false; // no longer firstPass through this puzzle
			Idx.MAYBES_VISITOR.cells = null;
			Idx.MAYBES_VISITOR.maybes = null;
		}
	}

	/**
	 * Populates the candidates array and the excluders array of CellSets.
	 * <p>
	 * Finds cells (candidates) which have atleast 2 maybes and have atleast 1
	 * sibling (an excluder) with maybesSize between 2 and $degree (inclusive).
	 * This method differs from below populateCandidatesAndTwoExcluders just in
	 * the minimum number of required excluders.
	 *
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndOneExcluders(final Grid grid) {
		final Cell[] cells = grid.cells;
		// all empty cells in the grid (cached)
		final Idx empties = grid.getEmpties();
		// find all possible excluders: with size 2..$degree
		// a new Idx ONCE per call, which I can live with.
		final Idx excluderSized = empties.where(cells
				, (cell) -> cell.size < degreePlus1);
		// an Idx of the excluders of each cell
		final Idx cellExcluders = this.cellExcluders;
		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		candidates.clear();
		MyArrays.clear(excls);
		empties.forEach((i) -> {
			// if this cell has atleast 1 excluder
			if ( cellExcluders.setAndAny(BUDDIES[i], excluderSized) ) {
				candidates.add(i);
				excls[i] = new CellSet(cells, cellExcluders);
			}
		});
		return candidates.size();
	}
	private final Idx cellExcluders = new Idx();

	/**
	 * Populates the candidates array and the excluders array of CellSets.
	 * <p>
	 * Finds cells (candidates) which have atleast 2 maybes and have atleast 2
	 * siblings (excluders) with maybesSize between 2 and $degree (inclusive).
	 * This method differs from above populateCandidatesAndExcluders just in
	 * the minimum number of required excluders.
	 *
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndTwoExcluders(final Grid grid) {
		final Cell[] cells = grid.cells;
		// all empty cells in the grid (cached)
		final Idx empties = grid.getEmpties(); // cached
		// find all possible excluders: with size 2..$degree
		// a new Idx ONCE per call, which I can live with.
		final Idx excluderSized = empties.where(cells
				, (cell) -> cell.size < degreePlus1);
		// an Idx of the excluders of each cell
		final Idx cellExcluders = this.cellExcluders;
		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		candidates.clear();
		MyArrays.clear(excls);
		empties.forEach((i) -> {
			// if this cell has atleast 2 excluders
			if ( cellExcluders.setAndMin(BUDDIES[i], excluderSized, 2) ) {
				candidates.add(i);
				excls[i] = new CellSet(cells, cellExcluders);
			}
		});
		return candidates.size();
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
		for ( int i=0,bits; i<degree; ++i ) {
			// does cell have any maybes that are not allowed at this position?
			if ( (bits=(cS[i].maybes & ~allowed[i])) != 0 ) {
				// foreach cell maybe that is not allowed
				for ( int v : VALUESES[bits] ) {
					// Yeah, 'v' can be excluded from 'cell'
					reds.upsert(cS[i].cell, v);
				}
			}
		}
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
	private ExcludedCombosMap newExcludedCombosMap(Cell[] cmnExcluders
			, Cell[] cells, Pots reds) {
		ValsStackEntry e; // the current values stack entry (ie stack[l])
		int l, i; // l is stack level, i is a general purpose index
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, reds);
		// I create my own "valsStack" rather than ____ with my callers
		// "valsStack" field, which it's still using when !accu.isSingle.
		final ValsStackEntry[] stack = new ValsStackEntry[degree];
		for ( i=0; i<degree; ++i ) {
			stack[i] = new ValsStackEntry();
		}
		stack[0].combo = 0; // ie an empty bitset
		l = 0; // the first stack level
		stack[0].index = 0; // start from the cells first potential value
		// copy immutable cell-data from the given cells to the valsStack (vs)
		for ( i=0; i<degree; ++i ) {
			stack[i].cands = cells[i].maybes;
		}
		VAL: for(;;) {
			// fallback a level while we're out of values at this level
			while ( stack[l].index >= VSIZE[stack[l].cands] ) {
				// reset the index to "restart" this exhausted level
				stack[l].index = 0;
				if ( --l < 0 ) {
					return map; // we've done all combos of cells values
				}
			}
			e = stack[l];
			// set the "presumed value" of this cell
			// and advance the index to the next potential value of this cell
			e.cand = VSHIFTED[e.cands][e.index++];
			// skip this value if it's already taken by a sibling to my left
			for ( i=0; i<l; ++i ) {
				if ( e.cand==stack[i].cand && cells[l].sees[cells[i].i] ) {
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
			if ( l == 0 ) {
				e.combo = e.cand;
				stack[++l].index = 0; // move right
			} else {
				e.combo = stack[l-1].combo | e.cand;
				if ( l < degreeMinus1 ) { // the combo is incomplete
					// move right to the next values-level,
					// and start at its first potential value
					stack[++l].index = 0;
				} else { // the combo is complete
					// if this combo contains all of any excluder cells maybes
					//    (the excluder cell MUST have A value)
					// then map this "excluded combo" to its excluder cell
					for ( Cell x : cmnExcluders ) {
						if ( (x.maybes & ~e.combo) == 0 ) {
							int[] a = new int[degree];
							for ( i=0; i<degree; ++i ) {
								a[i] = VALUESES[stack[i].cand][0];
							}
							map.put(new HashA(a), x);
							break; // we want only the first excluder of each combo
						}
					}
				}
			}
		}
	}

	/**
	 * Remove "defective" excluders from the EXCLUDERS_MAYBES array,
	 * and return the new possibly-reduced numExcls.
	 * <pre>
	 * The rules for "defective" excluders are:
	 * 1. remove excluders which contain any value that is not in any cell in
	 *    the aligned set, because no combination of the potential values of
	 *    the cells in the aligned set can ever cover (be a superset of) it,
	 *    so it will never contribute to an exclusion, ergo it's useless.
	 * 2. sort the remaining maybes by size ASCENDING; because smaller sets are
	 *    more likely to covered, and it's faster to do the more deterministic
	 *    comparisons first. Note that bubbleSort is faster than TimSort for
	 *    small (approx n&lt;12) arrays (mainly because it's impl'd locally).
	 * 3. remove excluders that are a superset of (contain all values in) any
	 *    other excluder, including any duplicates. Every set that covers 125
	 *    also covers 12, so given 12,125 remove 125 without effecting result.
	 *    We do this because 125 just wastes CPU-time in the DOG_____ING loop.
	 *    A second 12 excluder would also just be a waste of CPU time.
	 * </pre>
	 * @return the new (possibly reduced) number of excluder cells.
	 */
	private int clean(int numExcls) {
		int i, j, k, tmp;
		boolean any;
		// localised for both brevity and speed
		final int[] a = EXCLUDERS_MAYBES;
		// 1. if any of my values are not in the aligned set then remove me
		for ( j=0,i=0; i<degree; ++i ) {
			j |= cS[i].maybes;
		}
		final int allCands = j;
		for ( i=0; i<numExcls; ++i ) {
			if ( (a[i] & ~allCands) != 0 ) {
				if ( --numExcls < minExcls ) {
					return numExcls; // don't bother removing
				}
				// remove i: move i-and-all-to-its-right left one spot.
				for ( j=i; j<numExcls; ++j ) {
					a[j] = a[j+1];
				}
				--i; // --i then ++i = i; where we moved the data down to.
			}
		}
		// 2. bubbleSort 'numExcls' EXCLUDERS_MAYBES by size ASCENDING
		if ( numExcls > 1 ) {
			for ( i=numExcls; i>1; --i ) { // the right cell EXCLUSIVE
				any = false;
				for ( j=1; j<i; ++j ) { // the left cell INCLUSIVE
					// if previous k=j-1 is larger than current j then swap
					if ( VSIZE[a[k=j-1]] > VSIZE[a[j]] ) {
						tmp = a[j];
						a[j] = a[k];
						a[k] = tmp;
						any = true;
					}
				}
				if ( !any ) {
					break;
				}
			}
		}
		// 3. if I'm a superset of any preceedent excluder then remove me.
		for ( i=0; i<numExcls-1; ++i ) { // foreach except last
			for ( j=i+1; j<numExcls; ++j ) { // foreach subsequent
				if ( (a[i] & ~a[j]) == 0 ) {
					if ( --numExcls < minExcls ) {
						return numExcls; // don't bother removing
					}
					// remove j: move j-and-all-to-its-right left one spot.
					for ( k=j; k<numExcls; ++k ) {
						a[k] = a[k+1];
					}
					--j; // --j then ++j = j; where I moved the data down to.
				}
			}
		}
		return numExcls;
	}

	static final class CellStackEntry {
		int index; // the index of the current in the candidates array
		Cell cell; // the cell at this level 1..degree in the stack
		Idx excls; // excludersIdx: buds of all cells in the aligned set
		int maybes; // remember the complete cell.maybes
		@Override
		public String toString() {
			if ( cell == null ) {
				return MINUS;
			}
			return ""+index+":"+cell.toFullString()+"->"+excls;
		}
	}

	/**
	 * The ValsStackEntry is now used only in {@link #newExcludedCombosMap}.
	 * The DOG_____ING loop use to use it, but that's now been decomposed into
	 * parallel arrays, for speed.
	 */
	static final class ValsStackEntry {
		// a bitset of the selectable potential values of my cell;
		// so each time a presumed cell value is selected (in a master)
		// we remove that value from each sibling cell to it's right (slaves)
		// in the valuesStack, ie vS[slave].cands & ~vS[master].cand;
		// When I change my value: each sibling to my right (slave) rebuilds
		// his cands from cStack[slave].maybes and the cand's of his masters.
		int cands;
		// the index of the current/next presumedValue in maybes
		int index;
		// a bitset of the potential value that this cell is presumed to hold.
		// It's a bitset for use in bitset operations, like combo | cand
		int cand;
		// a bitset of the value of this and all cells to my left
		int combo;
		// ---- shortcuts to avoid repeated double-dereferencing ----
		// the indice of my cell in the grid, ie cell.i
		int i;
		// does this cell see each cell in the grid, ie cell.sees.
		boolean[] sees;
		// set the given cells attributes in this ValsStackEntry.
		void set(Cell cell) {
			i = cell.i;
			cands = cell.maybes; // default to the full set of maybes
			sees = cell.sees;
		}
		@Override
		public String toString() {
			return diuf.sudoku.Values.toString(cand);
		}
	}

	/**
	 * ICommonator allows us to decide between "hacked" or "correct"
	 * ONCE, without too much bother in the rest of the code.
	 * <p>
	 * There are two implementations: Commonator1 or Commonator2.
	 * Each A*E consistently uses either of these implementations.
	 * There is no swapping on the fly: to change a "hacked" setting
	 * we recreate the whole LogicalSolver.
	 */
	private static interface ICommonator {
		/**
		 * sets 'dest' to cells that are common to both the existing
		 * aligned set (src) and the current cell (excluders);
		 * returning are there sufficient?
		 * <p>
		 * That is: <br>
		 * do {@code return !excluders.idx1(dest, src);} <br>
		 * or {@code return !excluders.idx2(dest, src);}.
		 * <p>
		 * Call idx1/2 on 'excls', which sets dest to the union (and) of
		 * src and excls.idx(), and I negate the return value to return
		 * "are there sufficient cells common to both src and excls"
		 * (the opposite of original align packages requirement).
		 *
		 * @param excls set of excluder cells (buddies of this candidate
		 *  cell with size 2..degree)
		 * @param src indices of excluders common to all cells currently
		 *  in the aligned set, to which we add this cell (or not)
		 * @param dest indices of excluders common to all cells in the
		 *  aligned set, including the current cell (which will become the
		 *  src when we add the next cell, if any, to the aligned set)
		 * @return are there sufficient cells common to both src and
		 *  excluders.
		 */
		boolean idx(CellSet excls, Idx src, Idx dest);
	}

	/**
	 * This commonator aligns around one cell, which I call "correct". <ul>
	 * <li>A23E always require 2 cells (they never hint with one).
	 * <li>A4E rarely produces a hint on one excluder cell, but it's fast
	 *  enough anyway, and so is now correct only.
	 * <li>A5+E are all users choice. Each has it's own hacked setting.
	 * </ul>
	 */
	private static class Commonator1 implements ICommonator {
		@Override
		public boolean idx(CellSet excls, Idx src, Idx dest) {
			return !excls.idx1(src, dest);
		}
	}

	/**
	 * This commonator aligns around two cells, which I call "hacked". <ul>
	 * <li>A23E always require 2 cells (they never hint with one).
	 * <li>A4E rarely produces a hint on one excluder cell, but it's fast
	 *  enough anyway, and so is now correct only.
	 * <li>A5+E are all users choice. Each has it's own hacked setting.
	 * </ul>
	 */
	private static class Commonator2 implements ICommonator {
		@Override
		public boolean idx(CellSet excls, Idx src, Idx dest) {
			return !excls.idx2(src, dest);
		}
	}

	/**
	 * A map of the excluded combos => the optional lockingCell.
	 * A null lockingCell means "one instance of value per region".
	 * <p>Extends a LINKEDHashMap because order is significant to
	 * user following the resulting hint.
	 * <p>The put method is overridden to ignore attempts to add a
	 * key-array which does NOT contain one of the red (removable)
	 * values of the hint we're building. This is a tad slow, but
	 * speed doesn't really matter coz there's so few hints, and
	 * it's the most succinct way I can think of. Turns out this
	 * override costs ____all time, it might even be saving us a
	 * bit, I guess the relevance check is faster than puts were.
	 */
	private static final class ExcludedCombosMap extends LinkedHashMap<HashA, Cell> {
		private static final long serialVersionUID = 245566510L;
		private final Cell[] cells;
		private final Pots reds;

		/**
		 * Constructs a new ExcludedCombosMap whose put method uses the
		 * {@link AlignedExclusionHint#isRelevent} method to determine if the
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
		 * Overridden to ignore irrelevant values (which aren't in the reds
		 * Pots that was passed to my constructor).
		 *
		 * <p><b>Enhancement:</b> AEH.isRelevant(Cell[], Pots)
		 * <b>When</b> all usages of the deprecated constructor are
		 * eliminated please remove the cells==null branch of this
		 * method, and the deprecated constructor, and it's field/s. <br>
		 * FYI: compile with -Xlint to find deprecated methods.
		 *
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
			if ( !AlignedExclusionHint.isRelevent(cells, reds, key.array) ) {
				return null;
			}
			return super.put(key, value);
		}

		// intended for use in a standard-logging method
		public LinkedHashSet<Cell> getUsedCommonExcluders(Cell cmnExcls[], int numCmnExcls) {
			final LinkedHashSet<Cell> set = new LinkedHashSet<>(16, 0.75f);
			for ( Cell c : super.values() ) {
				if ( c != null ) {
					set.add(c);
				}
			}
			if ( set.isEmpty() ) {
				for ( int i=0; i<numCmnExcls; ++i ) {
					set.add(cmnExcls[i]);
				}
			}
			return set;
		}
	}

}
