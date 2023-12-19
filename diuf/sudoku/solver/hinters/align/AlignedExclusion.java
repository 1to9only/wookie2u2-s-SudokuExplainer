/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Grid.SEES;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.MASKED81;
import static diuf.sudoku.Idx.MASKOF;
import diuf.sudoku.IdxI;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VLAST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.Validator.*;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.MINUS;
import diuf.sudoku.utils.IntQueue;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * AlignedExclusion implements the AlignedExclusion Sudoku solving technique
 * for sets of between 2 and 10 cells:
 * {@link Tech#AlignedPair},
 * {@link Tech#AlignedTriple},
 * {@link Tech#AlignedQuad},
 * {@link Tech#AlignedPent},
 * {@link Tech#AlignedHex},
 * {@link Tech#AlignedSept},
 * {@link Tech#AlignedOct},
 * {@link Tech#AlignedNona}, and
 * {@link Tech#AlignedDec}.
 * <p>
 * <b>WARNING</b>: AlignedExclusion is too slow. Do not use AlignedExclusion!
 * <p>
 * First, my terminology:
 * <ul>
 * <li>AlignedExclusion eliminates maybes from cells in an aligned set.
 * <li>A "candidate" is an unset cell which sees (same box, row, or col) the
 * requisite number of excluder cells (those are the ones we align around).
 * <li>The "excluder cells" are stored in sets indexes by the candidate cell
 * which sees them all. An "excluder cell" has 2..degree potential values, such
 * that an aligned set of degree cells may contain all of the potential values
 * of an excluder cell (which they all see) and that is disallowed, because
 * every cell in the solution must have a value.
 * <li>An "aligned set" is a possible combination of the candidate cells that
 * all see enough common excluder cells.
 * <li>AlignedExclusion uses 2 as the minimum number of common excluder cells,
 * always. An aligned set of 2 or 3 cells only hints when aligned around two+
 * excluders. Aligned sets of 4 to 10 cells CAN still hint when aligned around
 * one cell, but its about as rare rocking horse s__t. About a third of hints
 * come from the ~2% of sets that are aligned around two+ excluder cells; hence
 * AlignedExclusion searches ONLY those aligned sets having 2+ excluder cells.
 * This is a hack! It takes a tenth of the time to find a third of the hints.
 * <li>A "combo" is a distinct possible combination of the maybes of the cells
 * in an aligned set.
 * </ul>
 * AFAIK there is only one way to do exclusion, and its expensive. Iterate each
 * possible combination of maybes (a "combo") of each aligned set to see if dis
 * combo is allowed (rules below). If so record that each cells presumed value
 * is allowed in this cell (thats important); then once all combos have been
 * examined see if all of the potential values of each cell have been allowed,
 * and if not then it/they have been excluded; so raise a hint.
 * <p>
 * To be clear, when a combo is disallowed we take no action, only when it IS
 * allowed do we record the presumed value of each cell, so that at the end we
 * can see if all of the potential values of each cell have been allowed by ANY
 * combo. Potential values that have not been allowed are "excluded".
 * <p>
 * It sounds simple enough; but the implementation is complicated by trying to
 * make an O(combinatorial*combinatorial) algorithm faster, to make it usable,
 * at which I have failed, thus far. So do not use AlignedExclusion! Its slow!
 * <p>
 * These two rules disallow a combo:<ul>
 *  <li>Aligned set cells which see each other cannot contain the same value; a
 *  concept which I call the "distinct" rule. If a cell has no remaining maybes
 *  because all of its maybes are already taken by preceeding cells (that it
 *  sees) in the aligned set then the combo is not allowed; indeed all possible
 *  combos containing these values in these cells are disallowed.
 *  <li>Because all aligned set cells see all of the excluder cell/s, the cells
 *  in the aligned set cannot contain ALL of the potential values of any common
 *  excluder cell, because every cell must have a value; when that occurs then
 *  this combo is disallowed.
 * </ul>
 * <p>
 * Sets aligned around multiple excluder cells are much more likely to hint; so
 * "hacked mode" processes about a tenth of the aligned sets to produce about a
 * third of the hints. This matters because AlignedExclusion is a ____ing slow
 * process. Most users simply will not wait for the "correct" version of the
 * algorithm to run over larger (5+) sets, but they might wait for this hacked
 * version over 5, 6, or even 7 cells; but 8, 9 and 10 are distant monarch slow
 * even hacked. Just my humble opinion.
 * <p>
 * Note that this implementation of AlignedExclusion uses the "stack iteration"
 * technique (twice) taken from the ALSs in HoDoKu, by hobiwan. HoDoKu is a
 * fantastic open source Sudoku solver. Download it from the Sudoku Explainer
 * homepage on SourceForge (my version is faster for nets).
 * <p>
 * AlignedExclusion replaces old Aligned*Exclusion for maintainability. The old
 * Aligned5Exclusion ran in 70 seconds. This one takes 211 seconds. Suck it.
 * <p>
 * Be warned that this class was a total-pain-in-the-ass to write and debug.
 * Its too complex. ____ with it at your peril! Do keep your towel handy.
 * <p>
 * This javadoc stops the auto-importer dropping {@link Debug}, {@link Values}.
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
 * This new version is MUCH slower, so I am going to stick with the original,
 * and retain this new version just for interest sake, coz it is TOO SLOW!
 *
 * -- Commonator interface --
 *       319,455,100  4420      72,274   1    319,455,100 Aligned Pair
 *     3,019,857,900  4419     683,380  15    201,323,860 Aligned Triple
 *   191,564,119,600  4405  43,487,881  54  3,547,483,696 Aligned Quad
 * and it is actually slower than without the call!
 *
 * -- if a single excluder --
 *       317,519,900 4420      71,837   1     317,519,900 Aligned Pair
 *     2,877,032,900 4419     651,059  15     191,802,193 Aligned Triple
 *   163,270,592,000 4405  37,064,833  54   3,023,529,481 Aligned Quad
 * a bit faster than "new" version, but still nowhere near the "old".
 *
 * 2020-12-17 24:?? It is faster now with workLevel, but still FAR too slow!
 * The old version is still SHIP-LOADS faster!
 * We are down two Pents, which I presume explains why we are up two Hexs.
 * -- old benchmark --
 *          time(ns)  calls   time/call  elims       time/elim hinter
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
 * So we are faster than the "old new" but still MUCH slower than the "old".
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
 * A5..10E, so the old "align" package is still preferred for the big A*Es;
 * align2 is ALWAYS used for A234E. here is the latest comparisons.
 * Note: A2E, A9E, and A10E are left out coz they do not hint.
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
 * I will run it again to see if it is repeatable, or just a JIT-narkle.
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
 * and I notice that, for some reason it is much faster than it was previously,
 * so I decided to add a comment to that effect. It is faster, but there ARE
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
 *
 * ran 2023-06-11.19-27-35 Trying to make AlignedExclusion faster. We no longer
 * create a new Idx for EVERY AlignedSet. CellSet is gone. All that is required
 * is a final Idx in each CellStackEntry which is set, not created. CellSets
 * idx1 and idx2 are now lambda expressions in AlignedExclusion. Everything,
 * especially the hammered bits, is now my best effort, I think.
 *
 * WantedEnabled: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair,
 * NakedTriple, HiddenTriple, Swampfish, XY_Wing, XYZ_Wing, Swordfish,
 * Jellyfish, UniqueRectangle, AlignedPair, AlignedTriple, AlignedQuad,
 * AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec,
 * UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus,
 * NestedUnary
 *
 * Unwanted: TwoStringKite, W_Wing, Skyscraper, EmptyRectangle, NakedQuad,
 * HiddenQuad, Coloring, XColoring, Medusa3D, GEM, BigWings, WXYZ_Wing,
 * VWXYZ_Wing, UVWXYZ_Wing, TUVWXYZ_Wing, STUVWXYZ_Wing, FinnedSwampfish,
 * FinnedSwordfish, FinnedJellyfish, DeathBlossom, ALS_XZ, ALS_Wing, ALS_Chain,
 * SueDeCoq, Superfisch, FrankenSwampfish, FrankenSwordfish, FrankenJellyfish,
 * MutantSwampfish, KrakenSwampfish, MutantSwordfish, KrakenSwordfish,
 * MutantJellyfish, KrakenJellyfish, NestedMultiple, NestedDynamic, NestedPlus
 *
 *          time(ns)  calls  time/call elims      time/elim hinter
 *        26,215,400 120533        217 66341            395 NakedSingle
 *        21,024,900  54192        387 16611          1,265 HiddenSingle
 *        93,941,000  37581      2,499 25138          3,737 Locking
 *       103,930,500  25762      4,034  8323         12,487 NakedPair
 *        83,513,300  23546      3,546  8536          9,783 HiddenPair
 *       131,408,900  21394      6,142  1745         75,305 NakedTriple
 *       123,154,500  20962      5,875  1088        113,193 HiddenTriple
 *        65,323,500  20737      3,150   637        102,548 Swampfish
 *       107,459,500  20492      5,243   752        142,898 XY_Wing
 *        75,774,900  19970      3,794   331        228,927 XYZ_Wing
 *        88,587,100  19660      4,505   308        287,620 Swordfish
 *       128,872,800  19574      6,583    21      6,136,800 Jellyfish
 *     1,948,946,200  19570     99,588  1517      1,284,737 UniqueRectangle
 *       731,619,000  18524     39,495    35     20,903,400 AlignedPair
 *     7,033,715,900  18489    380,427   650     10,821,101 AlignedTriple
 *   215,266,277,000  17896 12,028,736  1565    137,550,336 AlignedQuad
 *    86,639,258,200  16509  5,248,001   538    161,039,513 AlignedPent
 *   180,941,839,200  16024 11,291,927   407    444,574,543 AlignedHex
 *   264,116,697,500  15671 16,853,850   196  1,347,534,170 AlignedSept
 *   296,787,843,300  15491 19,158,727    98  3,028,447,380 AlignedOct
 *   238,274,099,800  15408 15,464,310    24  9,928,087,491 AlignedNona
 *   147,957,862,800  15385  9,617,020     5 29,591,572,560 AlignedDec
 *    28,404,442,300  15381  1,846,722 27174      1,045,280 UnaryChain
 *    14,216,307,000   8373  1,697,874  1291     11,011,856 NishioChain
 *    12,134,866,100   7307  1,660,717 11330      1,071,038 MultipleChain
 *     2,137,581,500   2645    808,159  6061        352,678 DynamicChain
 *        19,462,800      4  4,865,700    22        884,672 DynamicPlus
 * 1,497,660,024,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 1,541,557,696,600 (25:41)     1,052,257,813
 *                                                       0           1           2           3          4          5          6           7           8            9          10           11         12           13           14          15 16
 * AlignedPair   : nonHinters.count=   43069 COUNTS=[18558,   15996920,   15241044,          0,   3905051,    917521,    226490,          0,          0,           0,          0,     2483914,    730311,     1753568,           0,    1753568, 0]
 * AlignedTriple : nonHinters.count=  491566 COUNTS=[18523,  224328763,  223402771,          0,  33478818,   8900522,   2385752,          0,          0,           0,          0,    71775236,  15880844,    63834218,    37068444,   45299996, 0]
 * AlignedQuad   : nonHinters.count=21074748 COUNTS=[17927, 2316850153,          0, 2315979972, 601551791, 319024279, 119840070, 8716781694, 1071962349, 10331659194, 5467109970,   936602551, 116508900,   897765651,  1130454766,  581167003, 0]
 * AlignedPent   : nonHinters.count= 3020853 COUNTS=[16540, 1924689716, 1923857264,          0, 196013410,  40715237,  12502945,          0,          0,           0,          0,  2734869672, 171155332,  2692080353,  5264204588, 1685530841, 0]
 * AlignedHex    : nonHinters.count= 2816532 COUNTS=[16054, 2987600042, 2986787089,          0, 258516975,  35838603,  11855182,          0,          0,           0,          0,  6685081245, 203264720,  6644427948, 17941028416, 4043414308, 0]
 * AlignedSept   : nonHinters.count= 1770961 COUNTS=[15701, 3704003188, 3703201839,          0, 288389540,  21457138,   7639581,          0,          0,           0,          0, 10683043439, 157283664, 10656829315, 37795259298, 6258754943, 0]
 * AlignedOct    : nonHinters.count=  781789 COUNTS=[15521, 4069978769, 4069182673,          0, 299123097,   9052525,   3456002,          0,          0,           0,          0, 11522896560,  83250202, 11511003591, 52389802306, 6456255751, 0]
 * AlignedNona   : nonHinters.count=  236483 COUNTS=[15438, 4203015294, 4202221416,          0, 301560429,   2637272,   1081393,          0,          0,           0,          0,  8274258298,  29859200,  8270525875, 47564031802, 4385760524, 0]
 * AlignedDec    : nonHinters.count=   47710 COUNTS=[15415, 4239427151, 4238633612,          0, 302010107,    508879,    224593,          0,          0,           0,          0,  3755156258,   6978150,  3754380904, 26968614091, 1863096111, 0]
 *
 * KRC 2023-06-13 It not being Friday, I pulled the pin on correct, so AE is
 * now hacked only. All code pertaining to aligning sets round ONE common buddy
 * with 2..degree maybes is history. Ekythump! If you want correct then tough.
 *
 * KRC 2023-10-12 made the slow bit (calculating vsCands[s]) faster
 *       116,224,600    4281         27,148       0              0 AlignedPair
 *     1,322,703,000    4281        308,970       6    220,450,500 AlignedTriple
 *     6,501,768,400    4275      1,520,881       4  1,625,442,100 AlignedQuad
 *    19,653,747,300    4271      4,601,673       0              0 AlignedPent
 *    38,019,150,000    4271      8,901,697       3 12,673,050,000 AlignedHex
 *    54,836,388,700    4268     12,848,263       1 54,836,388,700 AlignedSept
 *    57,476,714,500    4267     13,470,052       3 19,158,904,833 AlignedOct
 *    43,452,092,900    4264     10,190,453       1 43,452,092,900 AlignedNona
 *    24,256,995,600    4263      5,690,123       0              0 AlignedDec
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   338,804,037,500 (05:38)       231,265,554
 * </pre>
 *
 * @author Keith Corlett 2020-12-10 created
 */
public final class AlignedExclusion extends AHinter
implements diuf.sudoku.solver.hinters.IPrepare
//		, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
////		Log.teeln(tech.name()+": nonHinters.count="+nonHinters.count);
//		Log.teeln(tech.name()+": COUNTS="+Arrays.toString(COUNTS));
//	}
//	private long[] COUNTS = new long[17];

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

	// hashCode of the aligned set => total maybes, to skip each set of cells
	// which have already been checked in there current state, which is faster,
	// but still not fast enough... the individual A*E classes are faster.
	private final NonHinters nonHinters;
	// is this the first run of this instance of AE through this puzzle?
	private boolean firstPass = true;

	// the minimum number of excluder-cells required to align a set around
	private final int minExcls;

	/**
	 * The constructor. I read my degree (number of cells in an aligned set)
	 * from the given Tech, which MUST be a Tech.Aligned*.
	 *
	 * @param tech to implement: Tech.Aligned*
	 */
	public AlignedExclusion(final Tech tech) {
		super(tech);
		assert tech.isAligned;
		// 2 for hacked, 1 for correct, except in A2E
		minExcls = 2;
		// NonHinters size matters. It doesnt grow like a HashMap, it does its
		// best with what-ever its given. When its overfull its really slow,
		// but underfull (too large) also slows EVERYthing down.
		// Shoot for fastest OVERALL, not just the fastest for this hinter.
		// Using 2+ Aligned(5+)Exclusions at one time uses so much RAM the
		// whole app bogs-down! So A*E is best not used at all.
		nonHinters = new NonHinters(MAP_SIZE[degree], 4);
	}

	/**
	 * We prepare after puzzle is loaded, and before the first findHints.
	 * The prepare method is defined by the IPrepare interface.
	 *
	 * @param grid
	 * @param logicalSolver
	 */
	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		nonHinters.clear();
		clearValidator();
		// the next getHints will be the firstPass through this puzzle,
		// to speed-up NonHinters.skip.
		firstPass = true;
	}

	/**
	 * Find first/all Aligned Exclusion hints of $degree size (degree is passed
	 * to my constructor via the Tech) depending on the passed IAccumulator; if
	 * accu.add returns true then I exit-early returning just the first hint
	 * found, else I keep searching to find all available hints.
	 * <p>
	 * Noobs should see class comments for a summary of aligned exclusion, then
	 * study the below pseudocode, then study the code; it will make no sense
	 * otherwise. It is ungrocable raw, you must cook yourself up to it. I have
	 * commented this class extensively, for comprehensibility. Good luck!
	 * <p>
	 * <pre>
	 * The Aligned Exclusion Sudoku solving technique.
	 * HIGH LEVEL PSEUDOCODE:
	 * degree is the number of cells in an aligned set.
	 * common excluder cells are my "nice-sized buddies": cells with 2..$degree
	 *        maybes seen by this cell thats a candidate to be in aligned sets.
	 * used common excluders:
	 *   are HACKED: two excluders are required 1/10 sets -> 1/3 hints.
	 *   FYI CORRECT: one excluder is required, but that is too slow!
	 *   and have no maybes that are not in this aligned set;
	 *   and do not "cover" each other, the smaller being retained;
	 *   and for efficiency are sorted by VSIZE[maybes] DESCENDING
	 * foreach set of $degree cells aligned around there common excluder cell/s
	 * 	 foreach combo of the potential values of the cells in the aligned set
	 *                skipping presumed values of my siblings
	 *     if this combo does NOT contain all of any excluder cells maybes then
	 *       add this combo to allowedValues (the values allowed in each cell)
	 * 	   endif
	 * 	 next combo
	 * 	 for i in 0..degree
	 *     if allowedValues[i] != cells[i].maybes then
	 * 	     anyExclusions = true
	 *     endif
	 * 	 next
	 * 	 if anyExclusions then
	 * 	   raise a hint and add it to the accumulator
	 * 	 endif
	 * next aligned set
	 * </pre>
	 *
	 * @return any hint/s found?
	 */
	@Override
	public boolean findHints() {
		// ------------------------------------------
		// | COMBINATORIAL * COMBINATORIAL == SLOW! |
		// ------------------------------------------
		// ANSI-C style variables, to reduce stack-work.
		// We use the cellsStack (an array of structs) to examine each possible
		// combination of degree cells (an aligned set); then we use valsStack
		// decomposed into parallel arrays (because its ____ing hammered, hard)
		// to examine each possible combination of the potential values of the
		// cells in each aligned set (a combo). That is BILLIONS of iterations.
		CellStackEntry cC; // current CellStackEntry
		IntQueue q; // I break my own rule that IntQueue should be final coz
					// here ONE STACKFRAME beats iterator speed.
		Idx mine; // excluders[indice], ie current candidate cells excluders
		boolean[] sees; // said the cabbage foreskin to the tub mulcher.
		int n // number of cells currently actually in the excluders array.
		, cL // cellLevel: the current level in the cell stack.
		, vL // valsLevel: the current level in the values stack.
		, m, s // master and slave indexes, hijacked as i and j.
		, r // r = s - 1, to use > instead of >= for speed
		, cand // bitset of the current presumed value of the current cell.
			   // hijacked as slaveCand: possible values of slave cell
		, indice // the indice of the current cell (ie index in grid.cells)
		, i, count // index
		;
		// workLevel: all potential values of all cells below workLevel have
		// already been allowed, so do NOT waste time re-examining them. When
		// the first combo is allowed with a value start looking for a combo
		// that allows the next potential value of the cell at the workLevel.
		// So workLevel is the lowest (biggest number) level with maybes that
		// have not yet been allowed by any combo. If workLevel reaches -1 then
		// all values have been allowed, ergo theres no hint here, again.
		int wL, wN; // wN is wL+1
		boolean isAllowed; // keep trying to process this combo
		// candidates is an Idx of empty cells with sufficient excluder cells.
		// An excluder cell is a cell with 2..$degree maybes, so that ez maybes
		// can be "covered" by a set of $degree cells. "Covered" means that all
		// the potential values of common excluder cell are taken by candidates
		// so that this "combo" (distinct potential values of $degree cells) is
		// not allowed. Then having examined all "combos", if no combo allows a
		// candidates potential value then that value has been "excluded", and
		// so is eliminated by the hint.
		final Idx candidates = new Idx();
		// stack references to heap fields (1.8 seconds faster top1465)
		// the set of excluder-cells for each candidate cell.
		// My array index is the indice of the candidate cell.
		final Idx[] excluders = new IdxI[GRID_SIZE];
		// EXCLUDERS_MAYBES: excluder cells maybes; there is ONE static array
		// shared via Idx#cellMaybes by all instances of AlignedExclusion which
		// cannot execute concurrently. I belong in Idx, but I exist only while
		// A*E is in use.
		final int[] excludersMaybes = new int[20];
		// an array of $degree cells: sets of aligned cells are built-up from
		// top[0] to bottom[degreeMinus1]. Da cells at each level are aligned
		// around 1 (CORRECT) or 2 (HACKED) excluder cells; adding another cell
		// to the cellStack means finding the next cell that also shares these
		// 1-or-2 common excluder cells. That is your task young Hobiwan. Hold
		// my ____ing beer.
		// The cellsStack is populated top-to-bottom; then we zero-in on the
		// workLevel, which is da bottom-most level that still has values which
		// have not yet been allowed. We keep trying to find a combo dat allows
		// the next value at the work-level, instead of wasting time checking
		// random vals that have already all been allowed. All potential values
		// of all cells above the workLevel have already been allowed. So we
		// hammer away at the workLevel until all of its potential values are
		// allowed, then ascend in the cellStack with --workLevel, and hammer
		// away at those values, and (in in A10E) so on, and so on, and so on,
		// and so on, and so on, and so on, and so on, and so on, and so on,
		// til finally all done at workLevel 0. That's a TRILLION iterations
		// for all-sized aligned sets over top1465. No wonder its slow!
		final CellStackEntry[] cellStack = new CellStackEntry[degree];
		n=0; do cellStack[n] = new CellStackEntry(); while (++n < degree);
		// vs* parallel arrays are a decomposed "values stack array",
		// coz array-look-ups are a bit faster than derefencing array of
		// ValsStackEntry structs. We avoid repeated struct deref'ing, at
		// the cost of more array-look-ups, which works-out faster. Later
		// Java releases substantially ate this AL/SD gap; then it went
		// bad again in 17. Ya can't win!
		final int[] vsMaybes = new int[degree];	// values stack maybes
		final int[] vsIndexes = new int[degree]; // values stack indexes
		final int[] vsCand = new int[degree];	// values stack cand
		final int[] vsCombo = new int[degree];	// values stack combo
		final int[] vsCands = new int[degree];	// values stack cands
		final boolean[][] vsSees = new boolean[degree][degree]; // vs sees arys
		// a bitset of the values which have been allowed in each candidate
		// cell in the aligned set.
		final int[] allowed = new int[degree];
		// see the NonHinters class for verbose commentary.
		final NonHinters nonHinters = this.nonHinters;
		// presume that no hints will be found
		// 15,701 A7e top1465 nonHinters.count=1,770,961
		boolean result = false;

		try {
			// find cells that are candidates for aligned exclusion, and get
			// the possible excluder-cells of each candidate. A candidate is
			// an unset cell with sufficient excluders. My degree is number
			// of cells in each aligned set. An excluder cell is one having
			// 2..degree maybes, so that ALL of those maybes can be "covered"
			// by an aligned set of degree cells.
			final int lastCellStackIndex = populate(candidates, excluders) - 1;
			if ( lastCellStackIndex < degreeMinus1 )
				return result; // rare but required (else CELL_LOOP endless)
			// indices of candidate cells to be read repeatedly
			final int[] indices = candidates.toArrayNew();
			// the first level of the cellStack is 0 (no stopper level here)
			cL = 0;
			// tricky: this levels index = the first cell at this level.
			m = 0;
			do
				cellStack[m].index = m;
			while (++m  <degree);
			// foreach distinct possible combination of degree candidate cells
			CELL_LOOP: for(;;) {
				// break CELL_NONLOOP instead of continue;ing CELL_LOOP, coz
				// break is faster than continue. It matters here. This loop
				// runs 3,704,003,188 times in HACKED A7E in a top1465 batch.
				// ?,???,??? A7E top1465 is numberOfTimes this block executes.
				// EVERYthing must be as efficient as possible, even more so.
				CELL_NONLOOP: for(;;) {
					// while this level is exhausted, fallback a level
					while ( (cC=cellStack[cL]).index > lastCellStackIndex ) {
						if(cL == 0) return result; // all examined -> done
						// set this levels index to cell after the previous
						// levels current cell (a forwards-only search); and
						// move up one level in the cellStack.
						cC.index = cellStack[--cL].index + 1;
						// check interrupt on ascent (ie as rarely as possible)
						hinterrupt();
					}
					// get current indice, incrementing for next time,
					indice = cC.indice = indices[cC.index++];
					// my excluders
					mine = excluders[indice];
					// set this cell up in the cellStack
					// nb: vsMaybes[cL] dont change; vsCands[cL] change often
					cC.maybes = vsMaybes[cL] = vsCands[cL] = maybes[indice];
					// if this is the first cell in the aligned set then
					if ( cL == 0 ) {
						// cC.excluders = nice-sized-buddies of currentCell
						cC.excluders.set(mine);
						++cL; // move down the cellStack
					// else does this cell line-up? Current cell is the next
					// cell in this AlignedSet only if enough (2) of his nice-
					// sized-buddies coincide with cells already in the set.
					} else if ( cC.excluders.setAndMany(
							cellStack[cL-1].excluders, mine) ) {
						// 288,389,540 A7E top1465
						// if the AlignedSet is incomplete
						if ( cL < degreeMinus1 ) {
							// cS[cL].index is already incremented to index of
							// the next candidate cell, so next level starts at
							// the next cell (a forwards-only search); then we
							// move down to the next level in the cellStack.
							cellStack[cL+1].index = cellStack[cL++].index;
						} else {
							// this AlignedSet is complete (has degree cells).
							assert cL == degreeMinus1;
							// set excludersMaybes to the excluder cells maybes
							count = 0;
							for ( q=cC.excluders.indices(); (i=q.poll())>QEMPTY; )
								excludersMaybes[count++] = maybes[i];
							// clean deletes excludersMaybes having a maybe not
							// in aligned set; sort excludersMaybes by size ASC
							// then removes superset/duplicate excluders->speed
							if ( (n=clean(cellStack, excludersMaybes, count)) < minExcls )
								break; // to continue the CELL_LOOP;
							// 21,457,138 A7E top1465
							// skip AlignedSet if already known to not hint in
							// current state; reexamine if maybes in AlignedSet
							// or maybes of excluder cells have changed since
							// there last visit to the proctologist.
							// Avert ~99.3% reexams so faster despite Hash get
							// NB: skip now includes excluders in totalMaybes.
							// NB: skip is not perfect. There are collisions,
							// which WILL wrongly skip sets, which might hint,
							// but run it ONCE without me to see why I exist.
							if ( nonHinters.skip(cells, cellStack, degree
									, n, firstPass, excludersMaybes) )
								break; // to continue the CELL_LOOP;
							// 7,639,581 A7E top1465
							// reset first valsStack entry to its first value.
							// valuesLevel=0 the first (no stopper level).
							// clear allowedValues for the new AlignedSet.
							vsIndexes[0] = vL = m = 0;
							do
								allowed[m] = 0;
							while (++m < degree);
							// init NON-TRANSITIVE vsSees rather than interpret
							// cS[?].indice BILLIONS of times in DOG_____ER.
							// This makes quite a big difference to my speed.
							m = 0;
							do { //master
								sees = SEES[cellStack[m].indice];
								s = m + 1;
								do //slave
									vsSees[s][m] = sees[cellStack[s].indice];
								while (++s < degree);
							} while (++m < degreeMinus1);
							// workLevel starts at the bottom (maximum) level.
							// workLevel is the valuesStack level to work on.
							// All values below workLevel are allowed, so that
							// each combo we test should (not allways, sadly)
							// contain a value that is not already allowed.
							wL = degreeMinus1;
							wN = degree;
							// populate the allowed array.
							// foreach combo of maybes of cells in AlignedSet.
							// nb: I call it nasty names because it is slow!
							// so slow it warrants graphotardenometry ==
							DOG_____ING_LOOP: for(;;) {
								// 10,683,043,439 ==========
								while ( vsIndexes[vL] > VLAST[vsCands[vL]] )
									if ( --vL < 0 )
										break DOG_____ING_LOOP;
								cand = vsCand[vL] = VSHIFTED[vsCands[vL]][vsIndexes[vL]++];
								if ( vL == 0 ) {
									vsCombo[vL] = cand;
									vsCands[vL] = vsMaybes[vL];
									s = 1;
									do //slaves
										// 157,283,664 A7E top1465
										if ( vsSees[s][vL] )
											vsCands[s] = vsMaybes[s] & ~cand;
										else
											vsCands[s] = vsMaybes[s];
									while (++s < degree);
									vsIndexes[++vL] = 0;
								} else {
									// 10,656,829,315 ==========
									vsCombo[vL] = vsCombo[vL-1] | cand;
									if ( vL < degreeMinus1 ) {
										// 1# hijack cand as slaveCand: useable
										// maybes of slave cell; because it is
										// faster to mutate the array minimal
										// times; coz every array reference is
										// an array-lookup.
										// 2# s is index of the current slave.
										// post-test s coz it NEVER starts as
										// degree, because vL < degreeMinus1,
										// ergo there is always 1+ slave level,
										// hence we need not check first slave
										// is beyond the last slave.
										// 3# r is always s-1 but repeated s-1
										// is expensive so we ++r instead.
										// Simply: r is pre-incremented, s is
										// post-incremented. s controls loop.
										// r is along for the ride.
										// SUIMMARY# all this bloody mess does
										// is save some time! Every bit matters
										// coz it executes many many times.
										r = vL;
										s = vL + 1;
										do { //slave
											// post-test m: coz s is never < 1
											sees = vsSees[s];
											cand = vsMaybes[s];
											++r;
											m = 0; //master
											do // 37,795,259,298 ==========,==========,==========,=======
												if ( sees[m] )
													cand &= ~vsCand[m];
											while (++m < r);
											vsCands[s] = cand;
										} while (++s < degree);
										vsIndexes[++vL] = 0;
									} else {
										// 6,258,754,943 ======
										isAllowed = true;
										s = vsCombo[degreeMinus1];
										m = 0;
										do
											if ( (excludersMaybes[m] & ~s) == 0 ) {
												isAllowed = false;
												break;
											}
										while (++m < n);
										if ( isAllowed ) {
											m = 0;
											do
												allowed[m] |= vsCand[m];
											while (++m < wN);
											while ( allowed[wL] == vsMaybes[wL] ) {
												if ( wL == 0 ) {
													nonHinters.put();
													break CELL_NONLOOP;
												}
												--wN;
												vL = --wL;
											}
										}
									}
								}
							}
							// if any of the maybes of the cells in the Aligned
							// Set have not been allowed by atleast one combo,
							// then they may be eliminated from that cell.
							// NB: If all values are allowed you cant get here,
							// coz we would have skipped this AlignedSet, hence
							// we double-check to guarantee quality, at a tiny
							// performance impost, because this happens only
							// when hinting, ie VERY VERY rarely.
							isAllowed = false;
							for ( m=0; m<degree; ++m )
								isAllowed |= allowed[m] != cellStack[m].maybes;
							if ( isAllowed ) {
								// FOUND AlignedExclusion!
								// reds = potential values not in allowed
								final Pots reds = createRedPots(cellStack, allowed);
								// reds never empty. Never say never.
								if ( reds != null ) {
									// FOUND AlignedExclusion with eliminations!
									// new cell array for each hint (few).
									final Cell[] cellsA = new Cell[degree];
									for ( m=0; m<degree; ++m )
										cellsA[m] = cells[cellStack[m].indice];
									// new excluders array too
									final Cell[] excluderCells
										= cC.excluders.cellsNew(cells);
									// build ExcludedCombosMap for the hint
									final ExcludedCombosMap map = newMap(
											excluderCells, cellsA, reds);
									// create the hint
									final AHint hint = new AlignedExclusionHint(
									grid, this, reds, cellsA, excluderCells, map);
									// and add the bastard to the accumulator
									result = true; // in case add returns false
									if ( accu.add(hint) )
										return result;
								}
							} else { // NEVER! But safety first!
								// record da fact that this aligned set did not
								// produce a hint, so that we skip-it next time
								// we see it, presuming no maybes have changed.
								nonHinters.put();
							}
						}
					}
					break; // NON_LOOP
				}
			}
		} finally {
			this.firstPass = false; // no longer firstPass through this puzzle
		}
	}

	private Idx sized(final Idx src, final int sizeCeiling) {
		int i;
		// the first mask must be a long, the second is an int value which also
		// fits in a long, so an array of long I am, Stan. Cats in hats Pearce.
		final long[] masks = new long[2];
		// nb: cnt remains unused coz the Idx.each that takes a filter was
		// designed to populate an output array, not an Idx. No problem.
		for ( final IntQueue q=src.indices(); (i=q.poll())>QEMPTY; )
			if ( sizes[i] < sizeCeiling )
				masks[MASKOF[i]] |= MASKED81[i];
		return new Idx(masks[0], (int)masks[1]);
	}

	/**
	 * Populate the candidates Idx and the excluders array of Idxs.
	 * <p>
	 * Finds cells (candidates) with atleast 2 maybes and atleast 2 siblings
	 * (excluders) of size 2..$degree (inclusive).
	 * <p>
	 * Note that "atleast 2 siblings" is "hacked", and <br>
	 * for A3E up "atleast 1 sibling" is "correct".
	 * <p>
	 * Hacked finds about a third of the hints in about a tenth of the time.
	 * Hacked takes a hundred times longer than chaining, hence its SE's only
	 * option, but this method exists to facilitate the alternative. If you
	 * want "correct" then the code change is ludicrously simple: just select
	 * Ma and press A.
	 *
	 * @param candidates
	 * @param excluders
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populate(final Idx candidates, final Idx[] excluders) {
		int i;
		final Idx empties = grid.getEmpties();
		final Idx sized = sized(empties, degreePlus1);
		final Idx sizedBuds = new Idx();
		for ( final IntQueue q=empties.indices(); (i=q.poll())>QEMPTY; ) {
			if ( sizedBuds.setAndMany(BUDDIES[i], sized) ) {
				candidates.add(i);
				excluders[i] = new IdxI(sizedBuds);
			}
		}
		return candidates.size();
	}

	/**
	 * The createRedPotentials creates a map of the red (removable) potential
	 * values, from Cell => Values from the cellStack and allowedValues array.
	 * Basically if each cell value is not allowed then it is excluded so add the
	 * bastard to redPots. Simples!
	 * @return a new Pots containing the excluded values, each in his Cell.
	 * Never null or empty.
	 */
	private Pots createRedPots(final CellStackEntry[] cellStack, final int[] allowed) {
		Pots reds = null;
		// foreach candidate cell in this aligned set
		for ( int i=0,bits; i<degree; ++i ) {
			// does cell have any maybes that are not allowed at this position?
			if ( (bits=(cellStack[i].maybes & ~allowed[i])) > 0 ) { // 9bits
				// foreach cell maybe that has not been allowed by any combo
				for ( int v : VALUESES[bits] ) {
					// Yeah, $v can be excluded from $cell
					if(reds==null) reds = new Pots();
					reds.upsert(cellStack[i].indice, VSHFT[v], DUMMY);
				}
			}
		}
		assert reds != null; // should NEVER happen
		return reds;
	}

	/**
	 * Now we do that dog____ing loop again, except this time we build the
	 * ExcludedCombosMap to go in the hint. This takes "some time", mainly coz
	 * HashMap.put is a bit slow, but this loop is executed about 100 times in
	 * top1465, not several hundred million times like the actual dog____ing
	 * loop, therefore performance is NOT critical.
	 *
	 * @param commonExcluderCells commonExcluderCells
	 * @param cells an array of cells in this aligned set.
	 * @param reds
	 * @return a new ExcludedCombosMap.
	 */
	private ExcludedCombosMap newMap(final Cell[] commonExcluderCells, final Cell[] cells, final Pots reds) {
		ValsStackEntry e; // the current values stack entry (ie stack[l])
		int l, i; // l is stack level, i is a general purpose index
		final ExcludedCombosMap map = new ExcludedCombosMap(cells, reds);
		// I create my own "valsStack" rather than ____ with my callers
		// "valsStack" field, which it is still using when !accu.isSingle.
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
			// fallback a level while we are out of values at this level
			while ( stack[l].index >= VSIZE[stack[l].cands] ) {
				// reset the index to "restart" this exhausted level
				stack[l].index = 0;
				if ( --l < 0 ) {
					return map; // we have done all combos of cells values
				}
			}
			e = stack[l];
			// set the "presumed value" of this cell
			// and advance the index to the next potential value of this cell
			e.cand = VSHIFTED[e.cands][e.index++];
			// skip this value if it is already taken by a sibling to my left
			for ( i=0; i<l; ++i ) {
				if ( e.cand==stack[i].cand && cells[l].sees[cells[i].indice] ) {
					// all combos containing both of these values are excluded,
					// so unpack the two equal-values into an array at da index
					// of the two sibling cells that cannot have da same value.
					// NOTE: We create a new array because HashA retains it!
					final int[] a = new int[degree];
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
					for ( Cell c : commonExcluderCells ) {
						if ( (c.maybes & ~e.combo) == 0 ) {
							int[] a = new int[degree];
							for ( i=0; i<degree; ++i ) {
								a[i] = VALUESES[stack[i].cand][0];
							}
							map.put(new HashA(a), c);
							break; // we want only the first excluder of each combo
						}
					}
				}
			}
		}
	}

	/**
	 * Clean removes "defective" excluders from the EXCLUDERS_MAYBES array,
	 * returning the possibly reduced numExcls.
	 * <pre>
	 * The rules used to "defect" an excluder are:
	 * 1. remove excluders which contain any value that is not in any cell in
	 *    the aligned set, because no combination of the potential values of
	 *    the cells in the aligned set can ever cover (be a superset of) it,
	 *    so it will never contribute to an exclusion, ergo it is useless.
	 * 2. sort the remaining maybes by size ASCENDING; because smaller sets are
	 *    more likely to be covered, and its faster to do da more deterministic
	 *    comparisons first. Note that bubbleSort is faster than TimSort for
	 *    small (approx n&lt;12) arrays (mainly coz its implemented locally).
	 * 3. remove excluders that are a superset of (contain all values in) any
	 *    other excluder, including any duplicates. Every set that covers 125
	 *    also covers 12, so given 12,125 remove 125 without effecting result.
	 *    We do this because 125 just wastes CPU-time in the DOG_____ING loop.
	 *    A second 12 excluder would also just be a waste of CPU time.
	 * </pre>
	 * @return the new (possibly reduced) number of excluder cells.
	 */
	private int clean(final CellStackEntry[] cellStack, final int[] maybes, int numExcls) {
		int i, j, k, tmp;
		boolean any;
		// 1. if any of my values are not in the aligned set then remove me
		j=i=0; do j |= cellStack[i].maybes; while(++i<degree);
		final int allCands = j;
		for ( i=0; i<numExcls; ++i ) {
			if ( (maybes[i] & ~allCands) > 0 ) { // 9bits
				if ( --numExcls < minExcls ) {
					return numExcls; // do not bother removing
				}
				// remove i: move i-and-all-to-its-right left one spot.
				for ( j=i; j<numExcls; ++j ) {
					maybes[j] = maybes[j+1];
				}
				--i; // --i then ++i = i; where we moved the data down to.
			}
		}
		// 2. bubbleSort $numExcls EXCLUDERS_MAYBES by size ASCENDING
		if ( numExcls > 1 ) {
			for ( i=numExcls; i>1; --i ) { // the right cell EXCLUSIVE
				any = false;
				for ( j=1; j<i; ++j ) { // the left cell INCLUSIVE
					// if previous k=j-1 is larger than current j then swap
					if ( VSIZE[maybes[k=j-1]] > VSIZE[maybes[j]] ) {
						tmp = maybes[j];
						maybes[j] = maybes[k];
						maybes[k] = tmp;
						any = true;
					}
				}
				if ( !any ) {
					break;
				}
			}
		}
		// 3. if I am a superset of any preceedent excluder then remove me.
		for ( i=0; i<numExcls-1; ++i ) { // foreach except last
			for ( j=i+1; j<numExcls; ++j ) { // foreach subsequent
				if ( (maybes[i] & ~maybes[j]) == 0 ) {
					if ( --numExcls < minExcls ) {
						return numExcls; // do not bother removing
					}
					// remove j: move j-and-all-to-its-right left one spot.
					for ( k=j; k<numExcls; ++k ) {
						maybes[k] = maybes[k+1];
					}
					--j; // --j then ++j = j; where I moved the data down to.
				}
			}
		}
		return numExcls;
	}

	/**
	 * A map of the excluded combos => the optional lockingCell.
	 * A null lockingCell means "one instance of value per region".
	 * <p>
	 * Extends a LINKEDHashMap because order is significant to user following
	 * the resulting hint.
	 * <p>
	 * The put method is overridden to ignore attempts to add a key-array which
	 * does NOT contain one of the red (removable) values of hint we are building.
	 * This is a tad slow, but speed does not really matter coz there is so few
	 * hints, and it is the most succinct way I can think of. Turns out this
	 * override costs ____all time, it might even be saving us a bit, I guess the
	 * relevance check is faster than puts were.
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
		 * Overridden to ignore irrelevant values (which are not in the reds
		 * Pots that was passed to my constructor).
		 *
		 * <p><b>Enhancement:</b> AEH.isRelevant(Cell[], Pots)
		 * <b>When</b> all usages of the deprecated constructor are
		 * eliminated please remove the cells==null branch of this
		 * method, and the deprecated constructor, and it is field/s. <br>
		 * FYI: compile with -Xlint to find deprecated methods.
		 *
		 * @param key HashA to put
		 * @param value Cell the locking cell (aka excluding cell), or null
		 *  meaning "only one instance of value is allowed per region".
		 * @return null if none of the values in key are relevant, else the
		 *  previous value (Cell) associated with this HashA (which should
		 *  NEVER exist), else null meaning none.<br>
		 *  This return value is not used. It would be hard to use coz nulls
		 *  are ambiguous. If you need a real return value then go ahead change
		 *  this code to make it useful for your circumstances. Make it return
		 *  an OccelotsSpleen for all I care. Dotaise sauce is extra.
		 */
		@Override
		public Cell put(final HashA key, final Cell value) {
			if ( !AlignedExclusionHint.isRelevent(cells, reds, key.array) ) {
				return null;
			}
			return super.put(key, value);
		}

		// intended for use in a standard-logging method
		public LinkedHashSet<Cell> getUsedCommonExcluders(final Cell cmnExcls[], final int numCmnExcls) {
			final LinkedHashSet<Cell> set = new LinkedHashSet<>(16, 0.75F);
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

	/**
	 * An Entry in the CellStack.
	 * <p>
	 * package visible for use in {@link NonHinters#skip}
	 */
	static final class CellStackEntry {
		int index; // the index of the current in the candidates array
		int indice = -1; // the cell at this level 1..degree in the stack
		final Idx excluders = new Idx(); // buddies common to all cells in the aligned set
		int maybes; // remember the complete cell.maybes
		@Override
		public String toString() {
			if ( indice == -1 ) {
				return MINUS;
			}
			return ""+index+"="+CELL_IDS[indice]+"->"+excluders;
		}
	}

	/**
	 * An Entry in the Values Stack.
	 */
	private static class ValsStackEntry {
		int index; // index of the current/next presumedValue in maybes
		int cands; // bitset of the selectable potential values of my cell
		int cand; // bitset of potential value of the current cell
		int combo; // bitset of values of this and all cells to my left
		int indice; // the indice of my cell in the grid, ie cell.i
		void set(Cell cell) {
			indice = cell.indice;
			cands = cell.maybes;
		}
		@Override
		public String toString() {
			return MAYBES_STR[cand];
		}
	}

}
