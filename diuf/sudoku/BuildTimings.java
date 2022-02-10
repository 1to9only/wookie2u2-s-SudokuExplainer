/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * The BuildTimings class exists specifically to hold this over-large comment.
 * Each entry is a LogicalSolverTester log from shortly before each build;
 * to keep track of how long it takes to solve all 1465 Sudoku puzzles using
 * logic, to see if we're making progress in the quest to solve all possible
 * Sudoku puzzles as simply, accurately, and quickly as possible.
 * <p>
 * That and it's just nice to remember stuff like it took Juillerat 17:48 to
 * solve 1465 puzzles; and my best is 01:11. So all it takes is the investment
 * of years and years of hard complex bloody pernickety work, and you too can
 * claim to be a total, complete and utter time-wasting Putz! Nag! Putz! Nag!
 * <p>
 * This comment was moved from the LogicalSolver class comments because it's
 * too bloody long, bogging Nutbeans, but here (no code) it seems to be OK.
 * <p>
 * <hr>
 * <p>
 * <b>KRC top1465.descending.27.01.verbose.log</b>
 * <pre>
 *       nanoseconds      calls   hints hinter
 *       260,202,511   48136       0 No Double Values
 *       121,860,362   48136       0 No Missing Candidates
 *       129,587,192   48136  155550 Naked Single
 *       222,402,619   45929  476800 Hidden Single
 *       867,946,257   34824   27155 Pointing and Claiming
 *       497,689,666   22204    5812 Naked Pair
 *       302,703,175   20322    8524 Hidden Pair
 *       476,303,536   18205    1639 Naked Triple
 *       218,766,936   17800    1122 Hidden Triple
 *       793,433,915   17584     764 XY-Wing
 *       218,533,651   17083     695 Swampfish
 *       309,806,521   16811     323 Swordfish
 *       201,603,670   16719     125 Naked Quad
 *        97,959,001   16698      14 Jellyfish
 *        39,045,423   16694       1 Hidden Quad
 *       791,894,951   16693     370 XYZ-Wing
 *     9,946,812,269   16347    1113 Unique Rectangle or Loop (URT)
 *       759,591,584   15824      59 Bivalue Universal Grave (BUG)
 *     1,045,301,780   15796       2 Aligned Pair Exclusion
 *   381,454,233,202   15794     645 Aligned Triple Exclusion
 *   324,716,699,628   15217  203576 Unary Chain and Cycle
 *   104,867,749,248    7749    2011 Nishio Chain
 *    99,642,081,087    6637   20452 Multiple Chain
 *   139,180,249,531    1895   15677 Dynamic Chain
 *     1,364,683,988       9      82 Dynamic Plus
 * 1,068,527,141,703 (17:48)
 * </pre>
 * <hr><p>
 * <b>KRC 2018-02-14 running all nesters on puzzle 2#top1465.d2.mt</b>
 * <pre>
 *     nanoseconds  hinter
 *       5,381,689  Too Few Clues
 *         284,254  Too Few Values
 *     109,546,325  Bruteforce Analysis
 *          39,390  No Double Values
 *          28,425  No Missing Candidates
 *           6,356  Naked Single
 *         143,244  Hidden Single
 *          13,410  Direct Naked Pair
 *          35,550  Direct Naked Triple
 *          23,816  Direct Hidden Pair
 *          28,076  Direct Hidden Triple
 *         162,520  Point and Claim
 *          10,547  Naked Pair
 *          23,746  Hidden Pair
 *          34,362  Naked Triple
 *          27,168  Hidden Triple
 *         177,746  XY-Wing
 *          17,391  Swampfish
 *          11,524  Swordfish
 *         139,892  Naked Quad
 *           7,962  Jellyfish
 *         111,537  Hidden Quad
 *         541,270  XYZ-Wing
 *       4,728,534  Unique Rectangle or Loop
 *         677,111  Bivalue Universal Grave
 *       8,144,121  Aligned Pair Exclusion
 *      49,282,730  Aligned Triple Exclusion
 *     173,001,527  Unary Chain and Cycle
 *      45,337,326  Nishio Chain
 *      72,860,588  Multiple Chain
 *     112,883,551  Dynamic Chain
 *     679,471,762  Dynamic Plus     // about two thirds of a second
 *  50,208,103,544  Nested Chain (+ Unary Chain and Cycle)
 * 130,316,973,666  Nested Chain (+ Multiple Chain)
 * 124,920,802,434  Nested Chain (+ Dynamic Chain)
 * 118,965,091,025  Nested Chain (+ Dynamic Plus)
 * 425,708,996,814  getAllHints  (7 minutes 5 seconds)
 * </pre>
 * <hr><p>
 * <b>KRC 2019-08-14</b> from here forward (unless explicitly stated otherwise)
 * all timings are for ACCURACY VERBOSE_4_MODE (the most verbose, a tad slow)
 * of top1465.d5.mt
 * on my new <b>intel i7 @ ~2900 Mhz</b> laptop.
 * <p><hr><p>
 * <b>KRC 2019-08-14</b>
 * <pre>
 *        time(ns)  calls   time/call   elims    time/elim hinter
 *      19,761,810  44955         439  110970          178 Naked Single
 *      49,318,000  43524       1,133  447320          110 Hidden Single
 *     159,814,822  33015       4,840   19031        8,397 Naked Pair
 *     127,687,281  32597       3,917   37323        3,421 Hidden Pair
 *     206,072,013  31588       6,523    4013       51,351 Naked Triple
 *     171,808,261  31533       5,448    7024       24,460 Hidden Triple
 *     126,478,212  31393       4,028   22372        5,653 Point and Claim
 *      89,681,726  20625       4,348    3928       22,831 Naked Pair
 *      56,959,112  19154       2,973    5293       10,761 Hidden Pair
 *     106,910,987  17708       6,037    1201       89,018 Naked Triple
 *      85,986,993  17367       4,951    1016       84,632 Hidden Triple
 *     233,711,341  17170      13,611     800      292,139 XY-Wing
 *      60,248,328  16631       3,622     652       92,405 Swampfish
 *      69,414,695  16380       4,237     321      216,245 Swordfish
 *     187,446,467  16291      11,506     134    1,398,854 Naked Quad
 *      20,066,676  16270       1,233      12    1,672,223 Jellyfish
 *     230,234,463  16266      14,154     371      620,578 XYZ-Wing
 *     145,172,298  16266       8,924       0            0 Hidden Quad
 *   1,919,624,533  15921     120,571    1122    1,710,895 Unique Rectangle
 *     187,013,405  15389      12,152      58    3,224,369 Bi-Uni Grave
 *   4,324,610,846  15360     281,550      27  160,170,772 Aligned Pair
 *  34,311,786,490  15333   2,237,773     648   52,950,287 Aligned Triple
 *  56,789,208,185  14751   3,849,854  215697      263,282 Unary Chain
 *   9,070,887,206   7277   1,246,514    2471    3,670,937 Nishio Chain
 *  17,028,082,732   6192   2,750,013   19899      855,725 Multiple Chain
 *  16,150,009,867   1674   9,647,556   14918    1,082,585 Dynamic Chain
 *     164,433,664      3  54,811,221      30    5,481,122 Dynamic Plus
 * 142,092,430,413
 * NB: 142,092,430,413 for 1465 puzzles is about 2 mins 22 secs, which is
 *     96,991,420 nanoseconds per puzzle, or 97 milliseconds, or 0.097 secs.
 * NB: The first 200-or-so puzzles in top1465.d5.mt are the hardest I've ever
 *     seen. I suppose they must've been handmade specifically to be HARD.
 * NB: I've just run the original project by Nicolas Juillerat on this box, for
 *     comparison. It took 17 minutes 48 seconds to solve the same 1465 puzzles
 *     so updated comments to compare my i7 with my i7 (not an i4).
 * </pre>
 * <hr><p>
 * <b>KRC 2019-09-10</b> with A3E + A4E but NO A5E.
 * <pre>
 *         time(ns)  calls   time/call   elims      time/elim hinter
 *       21,198,622  42660         496  133810            158 Naked Single
 *       52,907,558  41305       1,280  357030            148 Hidden Single
 *      155,915,871  32947       4,732   32915          4,736 Direct Naked Pair
 *      121,028,279  32517       3,722   46295          2,614 Direct Hidden Pair
 *      199,209,152  31512       6,321    7212         27,621 Direct Naked Triple
 *      168,298,463  31460       5,349    8620         19,524 Direct Hidden Triple
 *      105,393,318  31324       3,364   22377          4,709 Point and Claim
 *       83,887,737  20550       4,082    3929         21,350 Naked Pair
 *       53,939,813  19088       2,825    5278         10,219 Hidden Pair
 *      101,144,992  17651       5,730    1190         84,995 Naked Triple
 *       81,910,726  17314       4,730    1012         80,939 Hidden Triple
 *      213,501,392  17117      12,473     814        262,286 XY-Wing
 *       57,417,572  16569       3,465     661         86,864 Swampfish
 *       66,521,178  16310       4,078     321        207,231 Swordfish
 *      181,446,417  16219      11,187     129      1,406,561 Naked Quad
 *       19,215,075  16199       1,186      12      1,601,256 Jellyfish
 *      215,667,465  16195      13,316     376        573,583 XYZ-Wing
 *      140,059,352  16195       8,648       0              0 Hidden Quad
 *      158,501,671  16195       9,787       0              0 Naked Pent
 *      121,353,424  16195       7,493       0              0 Hidden Pent
 *    1,629,698,824  15844     102,859    1118      1,457,691 Unique Rectangle
 *      182,453,105  15313      11,914      52      3,508,713 Bi-Uni Grave
 *    5,576,011,727  15287     364,755      28    199,143,275 Aligned Pair
 *   59,967,620,562  15259   3,929,983  253196        236,842 Unary Chain
 *   17,983,070,877   7434   2,419,030      99    181,647,180 Aligned Triple
 *  424,327,893,776   7345  57,770,986     166  2,556,192,131 Aligned Quad
 *    8,752,378,721   7207   1,214,427    2482      3,526,341 Nishio Chain
 *   16,082,097,620   6121   2,627,364   21258        756,519 Multiple Chain
 *   15,435,089,607   1683   9,171,176   15811        976,224 Dynamic Chain
 *      171,700,036      3  57,233,345      30      5,723,334 Dynamic Plus
 *  552,426,532,932
 * NOTES:
 * 1. 552,426,532,932 ns (9:12) @ 377,082,958 ns (0.377 secs) puzzle. Flash!
 *    A4E took 424,327,893,776 ns or 76.8% of total. How to? The Codez!
 * 2. A3E now runs AFTER UnaryChain plus a couple of performance tweaks brought
 *    it down to just under 18 seconds. Acceptable. If I could get A4E down to
 *    24 seconds I'd take it in a second.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-09-24</b> with A3E, A4E, A5E.
 * <pre>
 *        time(ns) calls     time/call  elims   time/elim hinter
 *      14,961,463 42576           351 135050         110 Naked Single
 *      45,516,188 41228         1,104 355750         127 Hidden Single
 *     156,016,062 32883         4,744  33694       4,630 Direct Naked Pair
 *     120,961,186 32452         3,727  46176       2,619 Direct Hidden Pair
 *     203,284,743 31448         6,464   7212      28,187 Direct Naked Triple
 *     171,422,105 31396         5,459   8680      19,749 Direct Hidden Triple
 *     108,075,928 31258         3,457  22364       4,832 Point and Claim
 *      84,615,682 20492         4,129   3931      21,525 Naked Pair
 *      55,377,414 19025         2,910   5288      10,472 Hidden Pair
 *     102,776,253 17587         5,843   1194      86,077 Naked Triple
 *      84,516,885 17249         4,899   1016      83,185 Hidden Triple
 *     200,353,063 17051        11,750    817     245,230 XY-Wing
 *      56,771,945 16503         3,440    653      86,940 Swampfish
 *      66,468,686 16248         4,090    319     208,365 Swordfish
 *     183,617,463 16158        11,363    129   1,423,391 Naked Quad
 *      18,886,894 16138         1,170     12   1,573,907 Jellyfish
 *     198,167,455 16134        12,282    377     525,643 XYZ-Wing
 *     142,852,465 16134         8,854      0           0 Hidden Quad
 *     161,170,299 16134         9,989      0           0 Naked Pent
 *     125,508,662 16134         7,779      0           0 Hidden Pent
 *   1,712,653,661 15782       108,519   1122   1,526,429 Unique Rectangle
 *     205,039,849 15249        13,446     52   3,943,074 Bi-Uni Grave
 *   4,226,374,655 15223       277,630     23 183,755,419 Aligned Pair
 *  57,645,339,830 15200     3,792,456 252114     228,647 Unary Chain
 *  27,136,037,780  7401     3,666,536    108 251,259,609 Aligned Triple
 *   8,725,338,643  7304     1,194,597   2511   3,474,846 Nishio Chain
 *  17,042,232,868  6218     2,740,790  21629     787,934 Multiple Chain
 *  16,510,659,946  1694     9,746,552  15878   1,039,845 Dynamic Chain
 *     338,240,936     3   112,746,978      0           0 Aligned Quad
 *   4,896,278,836     3 1,632,092,945      0           0 Aligned Pent
 *     163,206,909     3    54,402,303     30   5,440,230 Dynamic Plus
 * 140,902,724,754
 * NOTES:
 * 1. 140,902,724,754 ns is about 2 mins 21 secs; which is 96,179,334 ns/puzzle,
 *    which is better than a poke in the eye with a blunt stick. I said "If I
 *    could get A4E down to 24 seconds I'd take it in a second." Well I got it
 *    down to 0.338 seconds, but only because it's running after the "normal"
 *    Chainers (just before the catch-all Dynamic Plus) so finds 0 hints.
 *    If we put A3E, A4E & A5E back before UnaryChain it slows right-down again.
 * 2. A5E took 4.896 seconds to run 3 times and not produce any hints. Too slow!
 *    with A5E running : F100 in 1,826,458,383,111 @ 18,264,583,831 ns.
 *    with A5E disabled: F100 in   126,526,023,470 @  1,265,260,234 ns. Sigh.
 * 3. -SPEED mode is up from 16 seconds to 1465 in 27,191,341,848 @ 18,560,642
 *    which is a bit of a worry. I thought I was making da simple stuff quicker!
 * </pre>
 * <hr><p>
 * <b>KRC 2019-09-30</b> with A3E, A4E, A5E.
 * <pre>
 *        time(ns) calls  time/call  elims   time/elim hinter
 *      18,052,498 46008        392 225530          80 Naked Single
 *      46,726,398 43738      1,068 391100         119 Hidden Single
 *     107,817,083 34911      3,088  26459       4,074 Point and Claim
 *     121,137,957 22574      5,366   6663      18,180 Naked Pair
 *      90,090,958 20392      4,417   8517      10,577 Hidden Pair
 *     133,328,909 18286      7,291   1620      82,301 Naked Triple
 *     115,189,957 17887      6,439   1102     104,528 Hidden Triple
 *     197,520,833 17672     11,177    764     258,535 XY-Wing
 *      58,826,272 17144      3,431    649      90,641 Swampfish
 *      67,631,290 16892      4,003    317     213,347 Swordfish
 *     184,852,330 16802     11,001    130   1,421,941 Naked Quad
 *      20,438,369 16780      1,218     12   1,703,197 Jellyfish
 *     187,670,393 16776     11,186    365     514,165 XYZ-Wing
 *   1,698,245,523 16435    103,331   1106   1,535,484 Unique Rectangle
 *     194,761,605 15907     12,243     64   3,043,150 Bi-Uni Grave
 *   1,998,883,121 15875    125,913     27  74,032,708 Aligned Pair
 *  26,324,092,870 15848  1,661,035    562  46,840,022 Aligned Triple
 * 137,966,158,983 15342  8,992,710    843 163,660,924 Aligned Quad
 * 216,229,695,438 14607 14,803,155    542 398,947,777 Aligned Pent
 *  52,577,575,759 14115  3,724,943 217432     241,811 Unary Chain
 *   8,329,930,804  7137  1,167,147   2470   3,372,441 Nishio Chain
 *  16,017,181,673  6054  2,645,718  20900     766,372 Multiple Chain
 *  15,863,185,465  1680  9,442,372  15760   1,006,547 Dynamic Chain
 *     151,796,199     3 50,598,733     30   5,059,873 Dynamic Plus
 * 478,700,790,687
 * NOTES:
 * 1. This not as fast as 2019-09-24 but it does find some hints. because A3E,
 *    A4E, and A5E ran before UnaryChain because they're now fast enough, thanks
 *    to persistence on my part.
 * 2. Original :  381,454,233,202  15794  645  Aligned Triple Exclusion
 *    Latest   :   26,324,092,870  15848  562  is a tad over 6.9% of original
 * 3. A5E now runs in about half the time that A4E used to.
 * 4. Disable A5E and solve time actually goes UP at bit:
 *    1465 151,642,277,288 @ 103,510,086 coz A5E and UnaryChain are now on par.
 *    Still seeing the occassional irrelevant hint, so that's next on my todos.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-01</b> added A6E and A7E.
 * <pre>
 * top1465.d5.mt 2019-10-01 08:21:42
 *        time(ns)       calls  time/call elim     time/elim  hinter
 *   2,132,186,446       16090    132,516   29    73,523,670  Aligned Pair
 *  29,161,434,276       16061  1,815,667  557    52,354,460  Aligned Triple
 * 152,089,171,294  2:32 15561  9,773,740  821   185,248,686  Aligned Quad
 * 262,133,574,714  4:22 14846 17,656,848  644   407,039,712  Aligned Pent
 * 612,845,635,986 10:12 14264 42,964,500  411 1,491,108,603  Aligned Hex
 * 914,140,777,296 15:14 13907 65,732,420  182 5,022,751,523  Aligned Sept
 * NB: the price goes up, and the returns go down, so dare we try A8E? I think
 * so, but first 3#top1465.d5.mt which took 33,743,216,168 ns. I don't like it!
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-04</b> added A8E.
 * <pre>
 * top1465.d5.mt 2019-10-04.13-14-43
 *          time(ns) calls  time/call elim     time/elim  hinter
 *     1,652,508,503 16109    102,582   30    55,083,616 Aligned Pair
 *    27,032,816,722 16079  1,681,249  569    47,509,343 Aligned Triple
 *   146,494,334,180 15567  9,410,569  892   164,231,316 Aligned Quad
 *   226,324,167,577 14788 15,304,582  567   399,160,789 Aligned Pent
 *   530,340,130,297 14274 37,154,275  411 1,290,365,280 Aligned Hex
 *   693,079,616,124 13917 49,800,935  178 3,893,705,708 Aligned Sept
 *   594,076,016,953 13758 43,180,405   81 7,334,271,814 Aligned Oct
 * 2,315,795,801,110 @ 1,587,693,428 ns/puzzle
 * NB: We dared A8E. The price went up, and the returns went down, as expected.
 * 7 seconds per elimination is bonkers! I got 3#top1465.d5.mt (et al) down by
 * putting a time limit on A3E-and-up. In A5E-and-up this limit now reduces the
 * maxCandidates so we don't try again until a candidate is killed elsewhere.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-04</b> with A8E.
 * <pre>
 *        time(ns) calls  time/call  elims     time/elim hinter
 *      33,377,414 46283        721 229270           145 Naked Single
 *      59,125,946 43987      1,344 391270           151 Hidden Single
 *     139,072,377 35131      3,958  26418         5,264 Point and Claim
 *     149,521,562 22810      6,555   6649        22,487 Naked Pair
 *     101,189,990 20625      4,906   8465        11,953 Hidden Pair
 *     146,736,148 18530      7,918   1598        91,824 Naked Triple
 *     122,477,141 18131      6,755   1100       111,342 Hidden Triple
 *     225,913,149 17915     12,610    758       298,038 XY-Wing
 *      70,253,746 17385      4,041    652       107,751 Swampfish
 *      69,815,125 17129      4,075    319       218,856 Swordfish
 *     199,112,164 17038     11,686    129     1,543,505 Naked Quad
 *      20,385,919 17016      1,198     12     1,698,826 Jellyfish
 *     217,955,007 17012     12,811    359       607,117 XYZ-Wing
 *   1,851,736,378 16676    111,041   1110     1,668,230 Unique Rectangle
 *     257,221,110 16148     15,928     62     4,148,727 Bi-Uni Grave
 *   1,844,881,090 16117    114,468     30    61,496,036 Aligned Pair
 *  28,501,469,004 16087  1,771,708    584    48,803,885 Aligned Triple
 * 152,740,937,228 15560  9,816,255    879   173,766,709 Aligned Quad
 * 232,033,151,060 14790 15,688,515    568   408,509,068 Aligned Pent
 * 553,588,140,557 14274 38,782,971    409 1,353,516,236 Aligned Hex
 * 723,794,413,105 13919 52,000,460    180 4,021,080,072 Aligned Sept
 * 599,526,759,080 13759 43,573,425     81 7,401,564,926 Aligned Oct
 *  55,388,539,343 13685  4,047,390 213460       259,479 Unary Chain
 *   8,759,665,370  7025  1,246,927   2462     3,557,946 Nishio Chain
 *  17,360,074,664  5950  2,917,659  20650       840,681 Multiple Chain
 *  17,075,776,084  1664 10,261,884  15747     1,084,382 Dynamic Chain
 *     161,868,013     3 53,956,004     30     5,395,600 Dynamic Plus
 * 2,394,439,567,774 / 1465 = 1,634,429,739 ns/puzzle (1.6 seconds)
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-08</b> Hack: New -REDO argument to LogicalSolverTester parses
 * a previous logFile to see which hinters to apply to each puzzle.
 * <pre>
 *        time(ns) calls  time/call  elims   time/elim hinter
 *      25,374,419 46276        548 229310         110 Naked Single
 *      61,138,533 43979      1,390 391240         156 Hidden Single
 *     135,930,629 35123      3,870  26414       5,146 Point and Claim
 *     141,214,807 18571      7,604   6640      21,267 Naked Pair
 *      91,479,466 15664      5,840   8460      10,813 Hidden Pair
 *  66,025,341,862 13526  4,881,364 213456     309,315 Unary Chain
 *     138,149,887  7427     18,601    756     182,737 XY-Wing
 *  83,503,693,374  7417 11,258,418    858  97,323,651 Aligned Quad
 *  21,101,527,017  5912  3,569,270  20650   1,021,865 Multiple Chain
 *      60,950,294  5856     10,408   1598      38,141 Naked Triple
 *   1,048,865,034  5790    181,151   1110     944,923 Unique Rectangle
 *  12,447,607,042  5742  2,167,817    611  20,372,515 Aligned Triple
 * 101,982,425,510  5695 17,907,361    568 179,546,523 Aligned Pent
 *      98,632,746  5178     19,048    359     274,743 XYZ-Wing
 *   7,265,182,790  4926  1,474,864   2462   2,950,927 Nishio Chain
 *      25,070,799  4616      5,431    652      38,452 Swampfish
 * 191,164,018,256  4258 44,895,260    409 467,393,687 Aligned Hex
 *      28,533,093  3206      8,899   1100      25,939 Hidden Triple
 * 148,072,604,302  2068 71,601,839    179 827,221,253 Aligned Sept
 *      14,835,670  1853      8,006    319      46,506 Swordfish
 *  20,543,122,988  1663 12,353,050  15737   1,305,402 Dynamic Chain
 *  55,320,695,749  1149 48,146,819     81 682,971,552 Aligned Oct
 *      13,921,344   550     25,311     62     224,537 Bi-Uni Grave
 *      62,143,905   458    135,685     30   2,071,463 Aligned Pair
 *       5,191,936   308     16,856    129      40,247 Naked Quad
 *         541,274    81      6,682     12      45,106 Jellyfish
 *     154,043,077     3 51,347,692     30   5,134,769 Dynamic Plus
 * 709,532,235,803
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  715,388,763,279 (11:55)  488,319,974 (a tad under 0.5 seconds)
 * NOTES:
 * 1. DiffLogs finds first diversion of hint-path in two VERBOSE_5_MODE logs.
 * 2. Also logFileName format: top1465.d5.YYYY-MM-DD.hh-mm-ss.log
 *    where "top1465.d5" is the input file name minus it's extension (.mt)
 *    includes the current date time to avoid accidental log-overwrites.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-10</b> Aligned*Exclusion uses excluders array instead of
 * excludersMap because profiler says it was taking kenages in HashMap.get,
 * but before and after timings tell me there's something wrong with the
 * profilers performance stats, like maybe the profiler itself is screwing
 * the timings of the tiny/fast methods it's timing.<br>
 * 2019-10-15 I now know that the profilers performance stats are screwy, in
 * fast non-instrumented mode it only profiles the slowest 8 methods called
 * and lumps all the others into the callers self-time, so it wasn't actually
 * taking kenages in hashMap.get, but it was/is significantly slower than
 * direct array access, so the excluders array stays, even though the logic
 * which lead to it's inception was flawed buy my misunderstanding of what
 * the profiler displays. When you want to use the profiler to workout what
 * needs changing you need to bight the bullet and instrument the method in
 * question, even though it produces hundreds of annoying message-boxes and
 * runs something like 10 times as long, making 100 puzzles still an acceptable
 * profile size, even for an impatient old ass like me. FYI an instrumented
 * profiler creates loads of class files in the projects root directory.
 * <pre>
 *        time(ns)  calls   time/call   elims    time/elim hinter
 *      18,003,324  46276         389  229310           78 Naked Single
 *      41,436,472  43979         942  391240          105 Hidden Single
 *     110,432,012  35123       3,144   26414        4,180 Point and Claim
 *     109,891,143  18571       5,917    6640       16,549 Naked Pair
 *      73,884,950  15664       4,716    8460        8,733 Hidden Pair
 *  52,635,651,925  13526   3,891,442  213456      246,587 Unary Chain
 *     109,355,136   7427      14,723     756      144,649 XY-Wing
 *  65,758,407,288   7417   8,865,903     858   76,641,500 Aligned Quad
 *  16,724,843,336   5912   2,828,965   20650      809,919 Multiple Chain
 *      54,422,626   5856       9,293    1598       34,056 Naked Triple
 *     866,070,570   5790     149,580    1110      780,243 Unique Rectangle
 *   9,598,924,054   5742   1,671,703     611   15,710,186 Aligned Triple
 *  84,340,052,392   5695  14,809,491     568  148,486,007 Aligned Pent
 *      77,110,065   5178      14,891     359      214,791 XYZ-Wing
 *   5,983,040,844   4926   1,214,584    2462    2,430,154 Nishio Chain
 *      19,116,462   4616       4,141     652       29,319 Swampfish
 * 161,065,221,924   4258  37,826,496     409  393,802,498 Aligned Hex
 *      23,416,637   3206       7,304    1100       21,287 Hidden Triple
 * 128,462,527,702   2068  62,119,210     179  717,667,752 Aligned Sept
 *      11,156,466   1853       6,020     319       34,973 Swordfish
 *  16,588,788,037   1663   9,975,218   15737    1,054,126 Dynamic Chain
 *  49,114,840,835   1179  41,658,049      81  606,356,059 Aligned Oct
 *      12,353,595    550      22,461      62      199,251 Bi-Uni Grave
 *      53,393,682    458     116,580      30    1,779,789 Aligned Pair
 *       4,302,280    308      13,968     129       33,351 Naked Quad
 *         449,235     81       5,546      12       37,436 Jellyfish
 *     195,178,731      3  65,059,577      30    6,505,957 Dynamic Plus
 * 592,052,271,723
 * pzls	       total (ns)	(mm:ss)	        each (ns)
 * 1465	  596,652,988,398	(09:56)	      407,271,664
 * NOTES:
 * 1. We're under 10 minutes with A8E enabled, which is gratifying, but half
 *    that would be a LOT better. We're only solving Sudoku puzzles, not
 *    waiting for The London Bus. Maybe it's a bit of both. Sigh.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-15</b> Added A9E and A10E, and I'm stopping there.
 * In -REDO mode A3..10E are now invoked only when they're going to find the
 * next hint. I've got it parsing the hintNumbers of each A*E out of a keeper
 * log. It's a hack, reading a BIG part of the solution out of a previous log,
 * but it's DAMN fast!
 * <pre>
 *        time(ns)  calls   time/call   elims   time/elim hinter
 *      17,854,760  46283         385  229680          77 Naked Single
 *      24,843,818  43984         564  391290          63 Hidden Single
 *      77,482,611  35124       2,205   26408       2,934 Point and Claim
 *     134,215,738  18560       7,231    6636      20,225 Naked Pair
 *      77,864,214  15668       4,969    8465       9,198 Hidden Pair
 *  58,660,546,753  13477   4,352,641  213012     275,386 Unary Chain
 *     117,443,111   7427      15,812     756     155,348 XY-Wing
 *   3,313,613,068   7418     446,698     858   3,862,019 Aligned Quad
 *  18,300,159,669   5910   3,096,473   20657     885,905 Multiple Chain
 *      56,167,082   5854       9,594    1598      35,148 Naked Triple
 *   1,039,447,976   5790     179,524    1110     936,439 Unique Rectangle
 *     512,189,621   5757      88,968     612     836,911 Aligned Triple
 *   3,483,598,351   5673     614,066     567   6,143,912 Aligned Pent
 *      79,512,064   5152      15,433     358     222,100 XYZ-Wing
 *   6,379,606,158   4924   1,295,614    2462   2,591,229 Nishio Chain
 *      20,161,969   4617       4,366     652      30,923 Swampfish
 *   7,580,078,017   4243   1,786,490     407  18,624,270 Aligned Hex
 *      25,973,843   3176       8,178    1099      23,634 Hidden Triple
 *   4,454,364,241   2070   2,151,866     178  25,024,518 Aligned Sept
 *      12,235,812   1877       6,518     322      37,999 Swordfish
 *  17,924,934,068   1663  10,778,673   15737   1,139,031 Dynamic Chain
 *   1,375,951,286   1093   1,258,875      78  17,640,401 Aligned Oct
 *      14,132,195    550      25,694      62     227,938 Bi-Uni Grave
 *      73,176,962    458     159,775      30   2,439,232 Aligned Pair
 *   1,865,563,881    365   5,111,133      23  81,111,473 Aligned Nona
 *       5,020,571    308      16,300     129      38,919 Naked Quad
 *     351,765,239    104   3,382,358       6  58,627,539 Aligned Dec
 *         441,835     81       5,454      12      36,819 Jellyfish
 *     163,444,214      3  54,481,404      30   5,448,140 Dynamic Plus
 * 126,141,789,127
 * pzls       total (ns)  (mm:ss)   each (ns)
 * 1465  132,961,855,248  (02:12)  90,758,945
 * NOTES:
 * 1. 2 minutes and 12 seconds with all A*E's is, umm, damn fast. FIGJAM!
 *    But of course it's a total HACK, coz it relies on having solved these
 *    puzzles previously. But I enjoyed the challenge of writing it.
 * 2. Without -REDO it's still 1465 in 2,558,947,881,721 (42:38) @ 1,746,722,103
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-17</b> I've generalised the mechanism to de/activate hinters
 * into INumberedHinter and applied it to Chainer which now extends
 * AHintNumberActivatableHinter which extends AHinter. It's a bit faster again.
 * Now I'm thinking of applying it to all the heavies, but I think we're going
 * to hit the law of diminishing returns. One can only try.
 * <pre>
 *       time(ns)  calls   time/call   elims    time/elim hinter
 *     14,316,641  46283         309  229680           62 Naked Single
 *     21,967,781  43984         499  391290           56 Hidden Single
 *     78,231,640  35124       2,227   26408        2,962 Point and Claim
 *    120,703,413  18560       6,503    6636       18,189 Naked Pair
 *     74,656,217  15668       4,764    8465        8,819 Hidden Pair
 * 33,460,488,094  13476   2,482,968  213011      157,083 Unary Chain
 *    115,408,184   7427      15,539     756      152,656 XY-Wing
 *  3,245,454,241   7418     437,510     858    3,782,580 Aligned Quad
 * 13,740,818,385   5910   2,325,011   20657      665,189 Multiple Chain
 *     55,150,754   5854       9,421    1598       34,512 Naked Triple
 *  1,133,396,983   5790     195,750    1110    1,021,078 Unique Rectangle
 *    494,599,317   5757      85,912     612      808,168 Aligned Triple
 *  3,117,759,405   5673     549,578     567    5,498,693 Aligned Pent
 *     79,576,602   5152      15,445     358      222,281 XYZ-Wing
 *  1,437,533,798   4924     291,944    2462      583,888 Nishio Chain
 *     20,123,229   4617       4,358     652       30,863 Swampfish
 *  8,031,450,354   4243   1,892,870     407   19,733,293 Aligned Hex
 *     25,297,840   3176       7,965    1099       23,018 Hidden Triple
 *  4,319,866,755   2070   2,086,892     178   24,268,914 Aligned Sept
 *     12,722,439   1877       6,778     322       39,510 Swordfish
 * 18,108,928,475   1663  10,889,313   15737    1,150,723 Dynamic Chain
 *  1,343,594,397   1093   1,229,272      78   17,225,569 Aligned Oct
 *     13,939,666    550      25,344      62      224,833 Bi-Uni Grave
 *      4,707,445    458      10,278      30      156,914 Aligned Pair
 *  3,550,378,468    365   9,727,064      24  147,932,436 Aligned Nona
 *      4,510,343    308      14,643     129       34,963 Naked Quad
 *    146,030,901    104   1,404,143       6   24,338,483 Aligned Dec
 *        424,551     81       5,241      12       35,379 Jellyfish
 *    206,447,328      3  68,815,776      30    6,881,577 Dynamic Plus
 * 92,978,483,646
 * pzls      total (ns)  (mm:ss)   each (ns)
 * 1465  98,811,650,413  (01:38)  67,448,225
 * NOTES:
 * 1. 1 minute and 38 seconds is pretty damn fast, but of course it's a total
 *    HACK, coz it relies on having solved these puzzles previously.
 * 2. Without -REDO it's still 40-odd minutes, ie slower than a wet week.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-26</b> I've applied INumberedHinter to ALL the heavier
 * hinters, and it's slower than applying it to just the uber-heavies,
 * but I'm keeping it anyway, and I'll build and release now.
 * <pre>
 * There was a LOG here but it was rooted, because the keeper didn't match the
 * software version, so it fell-through to DynamicPlus (the catch all), so I've
 * removed the rooted log to avert any future confusion.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-13</b> Made A*E about 25% faster by checking common excluders
 * while we're still building the combo, so that an excluded combo is checked
 * once, not multiple times: once for each subsequent value in the combo. It's
 * touch and go how often it should be checked. An enthusiast could dick around
 * with it to see what's actually fastest. I might even do that, but not today.
 * <pre>
 *       time(ns)  calls   time/call   elims   time/elim hinter
 *     22,234,202 114489         194  648190          34 Naked Single
 *     17,251,122  49670         347  157810         109 Hidden Single
 *     70,104,584  32258       2,173   22372       3,133 Point and Claim
 *     98,712,827  15743       6,270    3895      25,343 Naked Pair
 *     54,516,415  13620       4,002    5242      10,399 Hidden Pair
 *     57,533,007  13149       4,375   13233       4,347 Direct Hidden Pair
 * 34,216,317,087   6564   5,212,723   26829   1,275,348 Unary Chain
 *     46,522,211   5819       7,994    5629       8,264 Direct Naked Pair
 *     52,433,159   5473       9,580    1172      44,738 Naked Triple
 * 14,361,859,993   4210   3,411,368   11752   1,222,077 Multiple Chain
 *     22,997,687   3098       7,423    1007      22,837 Hidden Triple
 *     17,439,396   2795       6,239    1819       9,587 Direct Hidden Triple
 * 18,715,459,537   1649  11,349,581   10469   1,787,702 Dynamic Chain
 *  1,437,303,212   1070   1,343,274    1376   1,044,551 Nishio Chain
 *  4,976,133,780   1026   4,850,032    1149   4,330,838 Aligned Quad
 *      8,413,791    895       9,400     844       9,968 Direct Naked Triple
 *    503,090,333    551     913,049     612     822,043 Aligned Triple
 *    183,808,583    529     347,464    1113     165,146 Unique Rectangle
 *     18,472,926    525      35,186     747      24,729 XY-Wing
 *  3,012,746,789    502   6,001,487     553   5,448,005 Aligned Pent
 *  6,761,919,359    348  19,430,802     397  17,032,542 Aligned Hex
 *      6,540,706    325      20,125     348      18,795 XYZ-Wing
 *      4,527,611    310      14,605     129      35,097 Naked Quad
 *      2,784,253    256      10,875     653       4,263 Swampfish
 *  3,582,508,261    155  23,112,956     174  20,589,127 Aligned Sept
 *        947,133     90      10,523     321       2,950 Swordfish
 *  1,140,398,896     65  17,544,598      72  15,838,873 Aligned Oct
 *      1,970,424     32      61,575      64      30,787 Bi-Uni Grave
 *     10,648,692     28     380,310      28     380,310 Aligned Pair
 *  1,495,820,425     23  65,035,670      24  62,325,851 Aligned Nona
 *    141,512,462      7  20,216,066       7  20,216,066 Aligned Dec
 *         37,379      4       9,344      12       3,114 Jellyfish
 *    163,642,032      3  54,547,344      30   5,454,734 Dynamic Plus
 * 91,206,608,274
 * pzls       total (ns)  (mm:ss)   each (ns)
 * 1465  126,732,322,403  (02:06)  86,506,704
 * NOTES:
 * 1. The Aligned*Exclusion times are near 2019-10-17, which is NOT what I
 *    expected, but the total runtime is about the same. I suspect my PC is
 *    running slower because it's so hot this afternoon.
 *    2019-11-14 1465 in 132,878,014,439 (02:12) @ 90,701,716 -REDO
 *    That's even slower than yesterday! I can feel a reversion coming on.
 * 2. Just noticed that Dynamic Plus took 20 seconds in the previous run, which
 *    means that the keeper log didn't "match" the software so it didn't run the
 *    right hinter at the right time, which makes this slower run even more of
 *    a worry. I'll run it again tonight, when it's cooler.
 *    This just in: Yep, The previous run was rooted. I've removed the log.
 * 3. I've just thought, I changed AUTOSOLVE in apply hint, so that we see the
 *    naked singles again. It's definately slower, but it can't be that much
 *    slower, surely. NakedSingle only took twice as long, and found 10 times
 *    the elims, which is pretty much what I expected.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-17</b> Made A*E faster with subset method: removes supersets
 * from the common excluders. The trick is to not ____ with cmnExcls themselves,
 * just cmnExclBits which is ONLY used to test for exclusion in the primary
 * loop (DOG_1), the secondary loop (DOG_2) uses the actual cmnExcls.
 * <pre>
 * NOTE: This isn't a -REDO run
 *          time(ns)  calls  time/call  elims      time/elim hinter
 *        21,435,834 114489        187 648190             33 Naked Single
 *        16,829,092  49670        338 157810            106 Hidden Single
 *       203,824,105  33889      6,014   5629         36,209 Direct Naked Pair
 *       136,999,301  33461      4,094  13233         10,352 Direct Hidden Pair
 *       212,762,377  32453      6,556    844        252,088 Direct Naked Triple
 *       181,029,248  32399      5,587   1819         99,521 Direct Hidden Triple
 *        65,270,867  32261      2,023  22372          2,917 Point and Claim
 *        88,119,421  21460      4,106   3895         22,623 Naked Pair
 *        56,126,494  19969      2,810   5242         10,707 Hidden Pair
 *       105,792,708  18524      5,711   1172         90,266 Naked Triple
 *        93,218,321  18188      5,125   1007         92,570 Hidden Triple
 *       205,152,855  17937     11,437    747        274,635 XY-Wing
 *        61,245,017  17457      3,508    653         93,790 Swampfish
 *        67,114,397  17201      3,901    321        209,079 Swordfish
 *       188,236,825  17113     10,999    129      1,459,200 Naked Quad
 *       199,452,745  17046     11,700    348        573,140 XYZ-Wing
 *     1,829,279,937  16663    109,780   1113      1,643,557 Unique Rectangle
 *    24,981,589,878  16132  1,548,573    612     40,819,591 Aligned Triple
 *   145,964,168,768  15611  9,350,084   1149    127,035,830 Aligned Quad
 *     1,587,525,259  15377    103,240     28     56,697,330 Aligned Pair
 *   207,670,743,058  14556 14,267,019    553    375,534,797 Aligned Pent
 *   456,582,465,329  13909 32,826,404    397  1,150,081,776 Aligned Hex
 *   601,160,426,464  13651 44,037,830    174  3,454,944,979 Aligned Sept
 *   564,541,788,709  13496 41,830,304     72  7,840,858,176 Aligned Oct
 *    50,416,653,549  13496  3,735,673  26829      1,879,184 Unary Chain
 *   348,557,787,366  13037 26,736,042     25 13,942,311,494 Aligned Nona
 *    39,832,066,955   8517  4,676,771      6  6,638,677,825 Aligned Dec
 *        13,292,261   8499      1,563     12      1,107,688 Jellyfish
 *       104,116,981   7469     13,939     64      1,626,827 Bi-Uni Grave
 *     8,386,286,490   6932  1,209,793   1376      6,094,684 Nishio Chain
 *    16,841,212,455   5862  2,872,946  11752      1,433,050 Multiple Chain
 *    16,794,110,862   1652 10,165,926  10469      1,604,175 Dynamic Chain
 *       151,271,856      3 50,423,952     30      5,042,395 Dynamic Plus
 * 2,487,317,395,784
 * pzls	        total (ns)  (mm:ss)      each (ns)
 * 1465	 2,509,103,876,888  (41:49)  1,712,698,892
 * NOTES:
 * 1. The overall runtime is down from a worste of 58 minutes. A bit better,
 *    but I still think there has to be a fundamentally faster way, that doesn't
 *    involve repeatedly examining values that're already known to be allowed.
 *    2020-02-03: I now think that maybe I should try to make it so that every
 *    combo examined includes atleast 1 unknown value. Off the top of my head:
 *    if we make the "focus" cell-to-be-examined the last (right-most) because
 *    it could take just one pass to allow-all it's values; upon which we throw,
 *    and the catch shall "rotate" the "ring" of aligned set cells one position,
 *    so that the next cell becomes the "focus"... and when a cell gets all the
 *    way through as the focus we know we've found an exclusion (like now) but
 *    we don't spend ken-ages checking and rechecking new combinations of values
 *    that're already known to be allowed. So yeah, the trick is my new beaut
 *    ute: AllAllowedException.
 * 2. REDO 1465 in 124,784,829,619 (02:04) @ 85,177,358 which ain't too shabby.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-22</b> Made A*E faster again by removing each selected cell
 * value from the maybes of each subsequent sibling. This is faster because
 * it's doing more of the work less often. It's 10 minutes faster overall.
 * <pre>
 * NOTE: This isn't a -REDO run
 *          time(ns)  calls  time/call  elims      time/elim hinter
 *        18,971,469 114489        165 648190             29 Naked Single
 *        20,160,898  49670        405 157810            127 Hidden Single
 *       198,482,285  33889      5,856   5629         35,260 Direct Naked Pair
 *       136,570,871  33461      4,081  13233         10,320 Direct Hidden Pair
 *       212,567,500  32453      6,550    844        251,857 Direct Naked Triple
 *       181,008,007  32399      5,586   1819         99,509 Direct Hidden Triple
 *        69,434,998  32261      2,152  22372          3,103 Point and Claim
 *        88,391,663  21460      4,118   3895         22,693 Naked Pair
 *        57,762,979  19969      2,892   5242         11,019 Hidden Pair
 *       107,736,944  18524      5,816   1172         91,925 Naked Triple
 *        90,296,801  18188      4,964   1007         89,669 Hidden Triple
 *       207,666,345  17984     11,547    747        278,000 XY-Wing
 *        63,833,346  17459      3,656    653         97,753 Swampfish
 *        70,080,628  17203      4,073    321        218,319 Swordfish
 *       191,881,108  17113     11,212    129      1,487,450 Naked Quad
 *        20,838,716  17091      1,219     12      1,736,559 Jellyfish
 *       201,205,277  17087     11,775    348        578,176 XYZ-Wing
 *     1,815,024,638  16762    108,282   1113      1,630,749 Unique Rectangle
 *       231,126,924  16233     14,238     64      3,611,358 Bi-Uni Grave
 *     1,675,683,241  16201    103,430     28     59,845,830 Aligned Pair
 *    24,552,975,080  16173  1,518,145    612     40,119,240 Aligned Triple
 *   143,453,087,125  15622  9,182,760   1149    124,850,380 Aligned Quad
 *   184,047,591,366  14596 12,609,454    553    332,816,620 Aligned Pent
 *   352,021,701,217  14094 24,976,706    389    904,940,105 Aligned Hex
 *   408,718,660,272  13754 29,716,348    183  2,233,435,302 Aligned Sept
 *   361,896,322,232  13591 26,627,644     72  5,026,337,808 Aligned Oct
 *   221,621,856,307  13526 16,384,877     24  9,234,244,012 Aligned Nona
 *    73,747,604,458  13503  5,461,571      7 10,535,372,065 Aligned Dec
 *    50,631,397,641  13496  3,751,585  26829      1,887,189 Unary Chain
 *     8,493,897,553   6932  1,225,317   1376      6,172,890 Nishio Chain
 *    16,814,687,821   5862  2,868,421  11752      1,430,793 Multiple Chain
 *    16,709,945,295   1652 10,114,978  10469      1,596,135 Dynamic Chain
 *       147,439,256      3 49,146,418     30      4,914,641 Dynamic Plus
 * 1,868,515,890,261
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 1,889,295,456,806 (31:29)     1,289,621,472
 * NOTES:
 * 1. Overall runtime is nearly half of the worste of 58 minutes. Much Better!
 * 2. REDO 1465 in 125,004,538,187 (02:05) @ 85,327,329
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-24</b> Made A*E faster again by checking if maybes are empty,
 * but only when they're likely to be empty.
 * <pre>
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        20,196,114 114489        176 648190            31 Naked Single
 *        21,152,471  49670        425 157810           134 Hidden Single
 *       194,793,610  33889      5,747   5629        34,605 Direct Naked Pair
 *       137,379,825  33461      4,105  13233        10,381 Direct Hidden Pair
 *       208,242,169  32453      6,416    844       246,732 Direct Naked Triple
 *       178,541,874  32399      5,510   1819        98,153 Direct Hidden Triple
 *        69,125,355  32261      2,142  22372         3,089 Point and Claim
 *        90,434,426  21460      4,214   3895        23,218 Naked Pair
 *        54,615,609  19969      2,735   5242        10,418 Hidden Pair
 *       106,267,629  18524      5,736   1172        90,672 Naked Triple
 *        88,406,135  18188      4,860   1007        87,791 Hidden Triple
 *       207,458,538  17984     11,535    747       277,722 XY-Wing
 *        60,132,255  17459      3,444    653        92,086 Swampfish
 *        68,235,488  17203      3,966    321       212,571 Swordfish
 *       190,110,598  17113     11,109    129     1,473,725 Naked Quad
 *        20,465,230  17091      1,197     12     1,705,435 Jellyfish
 *       204,333,439  17087     11,958    348       587,165 XYZ-Wing
 *     1,754,193,204  16762    104,652   1113     1,576,094 Unique Rectangle
 *       223,052,438  16233     13,740     64     3,485,194 Bi-Uni Grave
 *     1,697,286,738  16201    104,764     28    60,617,383 Aligned Pair
 *    24,844,338,351  16173  1,536,161    612    40,595,324 Aligned Triple
 *   144,785,452,080  15622  9,268,048   1149   126,009,966 Aligned Quad
 *   179,922,259,454  14596 12,326,819    553   325,356,707 Aligned Pent
 *   345,179,220,120  14094 24,491,217    389   887,350,180 Aligned Hex
 *   399,030,898,399  13754 29,011,989    183 2,180,496,712 Aligned Sept
 *   340,308,798,749  13591 25,039,275     72 4,726,511,093 Aligned Oct
 *   209,100,678,433  13526 15,459,165     24 8,712,528,268 Aligned Nona
 *    69,406,487,121  13503  5,140,079      7 9,915,212,445 Aligned Dec
 *    50,714,652,037  13496  3,757,754  26829     1,890,292 Unary Chain
 *     8,584,987,157   6932  1,238,457   1376     6,239,089 Nishio Chain
 *    16,960,849,283   5862  2,893,355  11752     1,443,230 Multiple Chain
 *    16,894,045,498   1652 10,226,419  10469     1,613,721 Dynamic Chain
 *       148,221,715      3 49,407,238     30     4,940,723 Dynamic Plus
 * 1,811,475,311,542
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 1,831,810,882,763 (30:31)     1,250,382,855
 * NOTES:
 * 1. It's only a minute faster than previous. We're getting close to the point
 *    of diminishing returns again. I'll need to do something fundamentally
 *    different if A*E is going to get much faster; like trying to allow the
 *    most excluded value using an A*ish algorithm.
 * 2. REDO 1465 130,215,282,161 (02:10) @ 88,884,151
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-25</b> Made A*E faster again by disdisjuncting: filtering-out
 * each common excluder bits which contains a value that isn't in the maybes of
 * any of the cells in the aligned set, because it can not be covered, and
 * therefore this common excluder can not contribute to an exclusion... it's
 * just a waste of time, so we axe it.
 * <pre>
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        20,023,317 114489        174 648190            30 Naked Single
 *        14,854,987  49670        299 157810            94 Hidden Single
 *       196,145,151  33889      5,787   5629        34,845 Direct Naked Pair
 *       132,164,239  33461      3,949  13233         9,987 Direct Hidden Pair
 *       208,826,710  32453      6,434    844       247,425 Direct Naked Triple
 *       176,522,222  32399      5,448   1819        97,043 Direct Hidden Triple
 *        70,712,756  32261      2,191  22372         3,160 Point and Claim
 *        90,481,669  21460      4,216   3895        23,230 Naked Pair
 *        54,763,668  19969      2,742   5242        10,447 Hidden Pair
 *       106,354,009  18524      5,741   1172        90,745 Naked Triple
 *        88,632,836  18188      4,873   1007        88,016 Hidden Triple
 *       177,339,999  17984      9,860    747       237,402 XY-Wing
 *        60,058,165  17459      3,439    653        91,972 Swampfish
 *        69,911,680  17203      4,063    321       217,793 Swordfish
 *       187,486,645  17113     10,955    129     1,453,384 Naked Quad
 *        20,922,239  17091      1,224     12     1,743,519 Jellyfish
 *       168,705,655  17087      9,873    348       484,786 XYZ-Wing
 *     1,795,616,253  16762    107,124   1113     1,613,311 Unique Rectangle
 *       232,696,236  16233     14,334     64     3,635,878 Bi-Uni Grave
 *     1,633,481,920  16201    100,825     28    58,338,640 Aligned Pair
 *    24,443,398,602  16173  1,511,370    612    39,940,193 Aligned Triple
 *   148,672,250,859  15622  9,516,851   1149   129,392,733 Aligned Quad
 *   165,763,139,183  14596 11,356,751    553   299,752,512 Aligned Pent
 *   305,850,156,240  14094 21,700,734    389   786,247,188 Aligned Hex
 *   362,937,070,833  13754 26,387,746    183 1,983,262,682 Aligned Sept
 *   312,659,397,604  13591 23,004,885     72 4,342,491,633 Aligned Oct
 *   199,281,748,737  13526 14,733,235     24 8,303,406,197 Aligned Nona
 *    66,275,249,352  13503  4,908,187      7 9,467,892,764 Aligned Dec
 *    50,301,157,545  13496  3,727,116  26829     1,874,880 Unary Chain
 *     8,568,377,467   6932  1,236,061   1376     6,227,018 Nishio Chain
 *    17,305,038,131   5862  2,952,070  11752     1,472,518 Multiple Chain
 *    17,134,097,634   1652 10,371,729  10469     1,636,650 Dynamic Chain
 *       140,307,920      3 46,769,306     30     4,676,930 Dynamic Plus
 * 1,684,837,090,463
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 1,705,245,465,515 (28:25)     1,163,990,078
 * NOTES:
 * 1. It's 3 minutes faster than previous, which is pretty good considering
 *    we're on the point of diminishing returns.
 * 2. REDO 1465 in 121,839,843,947 (02:01) @ 83,167,128
 * </pre>
 * <hr><p>
 * <b>KRC 2019-11-29</b> Made A*E faster again by using a notSees array instead
 * of the method, and by looping while the shiftedValue is less than or equal
 * to the cells-maybes-bits.
 * <pre>
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        23,496,086 114489        205 648190            36 Naked Single
 *        15,636,687  49670        314 157810            99 Hidden Single
 *       206,872,613  33889      6,104   5629        36,751 Direct Naked Pair
 *       144,025,225  33461      4,304  13233        10,883 Direct Hidden Pair
 *       213,972,775  32453      6,593    844       253,522 Direct Naked Triple
 *       185,305,083  32399      5,719   1819       101,871 Direct Hidden Triple
 *        71,550,515  32261      2,217  22372         3,198 Point and Claim
 *        90,440,041  21460      4,214   3895        23,219 Naked Pair
 *        57,342,598  19969      2,871   5242        10,939 Hidden Pair
 *       107,838,284  18524      5,821   1172        92,012 Naked Triple
 *        91,145,284  18188      5,011   1007        90,511 Hidden Triple
 *       227,127,017  17984     12,629    747       304,052 XY-Wing
 *        68,695,134  17459      3,934    653       105,199 Swampfish
 *        69,256,605  17203      4,025    321       215,752 Swordfish
 *       190,445,919  17113     11,128    129     1,476,324 Naked Quad
 *        20,611,258  17091      1,205     12     1,717,604 Jellyfish
 *       217,466,915  17087     12,727    348       624,904 XYZ-Wing
 *     1,911,363,323  16762    114,029   1113     1,717,307 Unique Rectangle
 *       226,461,094  16233     13,950     64     3,538,454 Bi-Uni Grave
 *     1,618,417,878  16201     99,896     28    57,800,638 Aligned Pair
 *    25,049,764,926  16173  1,548,863    612    40,930,988 Aligned Triple
 *   142,915,694,839  15622  9,148,360   1149   124,382,676 Aligned Quad
 *   133,718,830,899  14596  9,161,333    553   241,806,204 Aligned Pent
 *   259,901,778,760  14094 18,440,597    389   668,127,965 Aligned Hex
 *   311,452,585,927  13754 22,644,509    183 1,701,926,699 Aligned Sept
 *   267,353,094,348  13591 19,671,333     72 3,713,237,421 Aligned Oct
 *   171,576,248,943  13526 12,684,921     24 7,149,010,372 Aligned Nona
 *    57,305,406,203  13503  4,243,901      7 8,186,486,600 Aligned Dec
 *    51,208,224,850  13496  3,794,326  26829     1,908,689 Unary Chain
 *     8,509,964,276   6932  1,227,634   1376     6,184,567 Nishio Chain
 *    17,194,571,555   5862  2,933,226  11752     1,463,118 Multiple Chain
 *    16,971,638,959   1652 10,273,389  10469     1,621,132 Dynamic Chain
 *       141,014,212      3 47,004,737     30     4,700,473 Dynamic Plus
 * 1,469,056,289,031
 * pzls        total (ns)  (mm:ss)      each (ns)
 * 1465 1,491,209,814,826  (24:51)  1,017,890,658
 * 1465 1,544,763,884,138  (25:44)  1,054,446,337 same code next day
 *
 * NOTES:
 * 1. It's about three and a half minutes faster than previous, which is good.
 * 2. But my previous comments regarding needing a complete rethink of A*E still
 *    stand. If it's going to get much faster then I'm going to need to dream up
 *    a way of doing it atleast an order of magnitude more efficiently, and
 *    frankly, I'm too stupid. This is about as good as I know how to make it.
 * 3. The original Juillerat took 27 minutes 43 seconds to solve the same 1465
 *    puzzles on this box. So I'm now faster doing A4..10E.
 * 3. REDO 1465 in 120,263,169,030 (02:00) @ 82,090,900
 *
 * Here's a summary of how it slows down as we enable each large A*E.
 * 2019-11-29 A3E 1465 in   149,701,028,655 (02:29) @   102,185,002 baseline
 * 2019-11-29 A4E 1465 in   284,209,342,672 (04:44) @   193,999,551 + 1:15
 * 2019-11-29 A5E 1465 in   423,881,675,314 (07:03) @   289,339,027 + 2:19
 * 2019-11-29 A6E 1465 in   686,845,773,561 (11:26) @   468,836,705 + 4:23
 * 2019-11-29 A7E 1465 in   979,281,186,374 (16:19) @   668,451,321 + 4:53
 * 2019-11-29 A8E 1465 in 1,254,142,114,378 (20:54) @   856,069,702 + 4:35
 * 2019-11-29 A9E 1465 in 1,430,545,200,629 (23:50) @   976,481,365 + 2:56
 * 2019-11-29 ATE 1465 in 1,544,763,884,138 (25:44) @ 1,054,446,337 + 1:56
 * </pre>
 * <hr><p>
 * <b>KRC 2019-12-12</b> Foreskin me fungus, it's been a roller-coaster ride
 * since my last comment. I reverted to DiufSudoku_V6_30.034.2019-11-29.OK.7z
 * coz A10E started running like a dog for no apparent reason. So this has the
 * same changes as DiufSudoku_V6_30.035.2019-12-10.OK.7z plus I've been off on
 * a correctness binge in A*E: A23E are now always correct; A456E are user-
 * selectable for correct or hacked in TechSelectDialog; and A78910E are hacked,
 * because they're just too slow when correct.
 *
 * I'll release this as version 6_30.036 this afternoon, killing-off 035.
 *
 * Then it's time to go to work on A7E_1C, to get its runtime down to acceptable
 * limit, so let's arbitrarily choose an hour for the whole runtime; so we must
 * promulgate any learnings to the other A*E's also.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        21,616,737 118162         182 662380            32 Naked Single
 *        20,083,331  51924         386 162000           123 Hidden Single
 *        75,281,291  35724       2,107  26434         2,847 Point and Claim
 *       130,728,863  23367       5,594   6548        19,964 Naked Pair
 *        90,341,840  21218       4,257   8461        10,677 Hidden Pair
 *       144,984,450  19118       7,583   1614        89,829 Naked Triple
 *       120,622,490  18712       6,446   1064       113,367 Hidden Triple
 *       213,359,367  18507      11,528    747       285,621 XY-Wing
 *        63,758,901  17972       3,547    683        93,351 Swampfish
 *        70,262,830  17705       3,968    316       222,350 Swordfish
 *       199,858,431  17617      11,344    126     1,586,178 Naked Quad
 *        21,020,269  17596       1,194     16     1,313,766 Jellyfish
 *       205,457,598  17591      11,679    342       600,753 XYZ-Wing
 *     1,776,093,814  17271     102,836   1134     1,566,220 Unique Rectangle
 *       243,621,761  16732      14,560     62     3,929,383 Bi-Uni Grave
 *     1,655,447,284  16702      99,116     23    71,975,968 Aligned Pair
 *    22,842,870,670  16679   1,369,558    594    38,456,011 Aligned Triple
 *   260,974,879,786  16145  16,164,439   1457   179,117,968 Aligned Quad
 * 1,360,049,406,248  14848  91,598,155   1392   977,046,987 Aligned Pent
 * 5,165,075,921,601  13625 379,088,141   1150 4,491,370,366 Aligned Hex
 *   307,852,840,497  12641  24,353,519    133 2,314,683,011 Aligned Sept
 *   269,161,245,384  12521  21,496,785     67 4,017,332,020 Aligned Oct
 *   184,824,074,432  12460  14,833,392     27 6,845,336,090 Aligned Nona
 *    68,687,129,270  12434   5,524,137      8 8,585,891,158 Aligned Dec
 *    47,925,514,147  12427   3,856,563  25313     1,893,316 Unary Chain
 *     8,326,850,870   6395   1,302,087   1296     6,425,039 Nishio Chain
 *    16,205,959,713   5369   3,018,431  10782     1,503,056 Multiple Chain
 *    16,923,095,283   1598  10,590,172  10037     1,686,071 Dynamic Chain
 *       159,456,941      3  53,152,313     30     5,315,231 Dynamic Plus
 * 7,734,061,784,099
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  7,760,400,448,810  (129:20)  5,297,201,671    2 hours 9 minutes
 *
 * NOTES:
 * 1. 2 hours and 9 minutes is seriously slow, but it's using correct A4E, A5E,
 *    and A6E, so it's not as bad as it looks. I find that this configuration
 *    is still usable in the GUI, it's just really slow for around 50 puzzles
 *    in top1465.d5.mt. The slowest being 9# which takes 128 seconds to analyse.
 * 2. I keep thinking that to get A*E performant I shall need to dream-up an
 *    algorithm that focuses on maybes that have not yet been allowed, somehow
 *    magically ignoring the millions of other combos. I guess it boils down
 *    to which cell goes where in the dog____ing loop, and somehow dropping
 *    each cell when all of it's potential values have been allowed, while
 *    retaining it's covers functionalty somehow: a bitset-based approach.
 * 3. REDO isn't relevant to these changes, which are about accuracy over speed.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-08</b> I just ran this for comparison with the above comment,
 * to assure myself that I'm making some progress.
 * <pre>
 *         time(ns)  calls   time/call  elims      time/elim hinter
 *       24,121,700 118162         204 662380             36 Naked Single
 *       22,203,700  51924         427 162000            137 Hidden Single
 *       74,208,900  35724       2,077  26434          2,807 Point and Claim
 *      147,169,700  23367       6,298   6548         22,475 Naked Pair
 *      100,694,900  21218       4,745   8461         11,901 Hidden Pair
 *      154,575,600  19118       8,085   1614         95,771 Naked Triple
 *      129,424,000  18712       6,916   1064        121,639 Hidden Triple
 *      119,656,900  18507       6,465    747        160,183 XY-Wing
 *       66,834,200  17972       3,718    683         97,853 Swampfish
 *       76,582,000  17705       4,325    316        242,348 Swordfish
 *      214,475,900  17617      12,174    126      1,702,189 Naked Quad
 *       24,019,400  17596       1,365     16      1,501,212 Jellyfish
 *       92,247,500  17591       5,244    342        269,729 XYZ-Wing
 *    1,971,993,500  17271     114,179   1134      1,738,971 Unique Rectangle
 *      275,995,000  16732      16,495     62      4,451,532 Bi-Uni Grave
 *    1,618,153,100  16702      96,883     23     70,354,482 Aligned Pair
 *   21,020,257,800  16679   1,260,282    594     35,387,639 Aligned Triple
 *  257,607,628,000  16145  15,955,876   1457    176,806,882 Aligned Quad
 *  847,586,431,800  14848  57,084,215   1392    608,898,298 Aligned Pent
 *3,490,962,383,900  13625 256,217,422   1150  3,035,619,464 Aligned Hex
 *  203,905,210,000  12641  16,130,465    133  1,533,121,879 Aligned Sept
 *  173,685,172,400  12521  13,871,509     67  2,592,316,005 Aligned Oct
 *  692,527,142,300  12460  55,580,027     27 25,649,153,418 Aligned Nona
 *  106,478,807,600  12434   8,563,519      8 13,309,850,950 Aligned Dec
 *   51,564,282,400  12427   4,149,374  25313      2,037,067 Unary Chain
 *    8,790,243,900   6395   1,374,549   1296      6,782,595 Nishio Chain
 *   16,989,218,500   5369   3,164,317  10782      1,575,701 Multiple Chain
 *   17,247,555,400   1598  10,793,213  10037      1,718,397 Dynamic Chain
 *      147,004,500      3  49,001,500     30      4,900,150 Dynamic Plus
 *5,893,623,694,500
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 5,931,319,893,600 (98:51)     4,048,682,521    1 hour 39 minutes
 * NOTES:
 * 1. Yes 1:39 is better than 2:09, but it's only 20 minutes of the hour
 *    that I have to find. 1 hour is my arbitrary cap for top1465.d5.mt.
 *    I still feel that a complete rethink is required, and I still haven't
 *    dreamt-up a faster solution to the basic A*E problem. I'm still just
 *    "blindly" testing every possible combo; and I still keep thinking "there
 *    just HAS to be a better way", to skip "obviously unsucessful combo's" a
 *    LOT faster; but every stat I can think of fails: it either takes longer
 *    to calculate than just blindly iterating the combos, or it's non-bloody-
 *    deterministic, or both.
 *    So I'm at an impass: I either need to have a genius moment right now,
 *    or accept the hacked A6E and A7E and slink away with my tail between
 *    my legs. But hey, atleast I got A4E and A5E down correct, right?
 *    Maybe A6E and A7E will have to wait for CPU's to double in speed again;
 *    but unfortunately that's not ever likely to happen. Cores are as fast
 *    now as they will ever be, they're just adding more cores and claiming
 *    the machine is faster. Still, atleast I can listen to Dire Straights
 *    whilst wrestling with them. Irony?
 * 2. Maybe I should multi-thread the dog____ing loop? with a thread-pool: a
 *    producer which adds each aligned set to a queue, and a consumer which
 *    polls it off to start a new daemon thread on a free CPU which works-out
 *    if any maybes are excluded, and if so then (syncronized: adds a hint to
 *    a queue); then we just addAll the hints-queue to the accumalator.
 *    The only problem with the theory is I know nothing about producer-consumer
 *    thread queues, and the idea of working it all out scares me. S__t gets
 *    really complicated really fast so I'm afraid that I'm too afraid to try.
 *    This is where I'd google "Java thread queue" if I could afford a bloody
 *    internet connection. The internet makes programmers braver, and therefore
 *    less miserable. BIG Sigh. Follow Me Home. Irony? Nope back to "Once Upon
 *    A Time In The West"... They're (definately) still gonna get you if you
 *    don't do something. If you do something they're probably still going to
 *    get you, but maybe not... Let's try to concentrate on the maybe. Hell,
 *    it's only a bloody Sudoku puzzle solver, not an ICBM guidance system,
 *    which, IMHO, is probably simpler. Deadlier, but simpler.
 * 3. REDO isn't relevant.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-08</b> I'm still trying to make the "correct" version of
 * Aligned 7+ Exclusion fast enough to use in practice, and I'm still failing
 * miserably. The latest version takes 4.5 hours on top1465.d5.mt with A7E_1C;
 * which is better than a poke in the eye with a blunt stick, but still far too
 * slow... I'd happily accept an hour at this stage, and half an hour makes the
 * GUI "snappy". Tell him he's dreaming.
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        32,035,800 118246         270 662790             48 Naked Single
 *        25,956,200  51967         499 162220            160 Hidden Single
 *        89,852,600  35745       2,513  26393          3,404 Point and Claim
 *       169,258,800  23406       7,231   6527         25,932 Naked Pair
 *       109,802,700  21270       5,162   8451         12,992 Hidden Pair
 *       175,763,100  19178       9,164   1599        109,920 Naked Triple
 *       148,929,100  18778       7,931   1045        142,515 Hidden Triple
 *       128,722,700  18576       6,929    726        177,303 XY-Wing
 *        73,347,000  18057       4,061    683        107,389 Swampfish
 *        83,828,200  17790       4,712    313        267,821 Swordfish
 *       240,071,700  17703      13,561    129      1,861,020 Naked Quad
 *        26,081,300  17681       1,475     16      1,630,081 Jellyfish
 *        98,755,800  17676       5,586    332        297,457 XYZ-Wing
 *     2,393,284,000  17366     137,814   1133      2,112,342 Unique Rectangle
 *       291,257,000  16827      17,308     62      4,697,693 Bi-Uni Grave
 *     1,826,086,100  16797     108,715     23     79,395,047 Aligned Pair
 *    23,248,634,400  16774   1,385,992    598     38,877,315 Aligned Triple
 *   282,081,467,400  16233  17,377,038   1422    198,369,527 Aligned Quad
 *   926,804,059,000  14964  61,935,582   1372    675,513,162 Aligned Pent
 * 3,836,889,711,600  13762 278,803,205   1116  3,438,073,218 Aligned Hex
 *10,017,801,130,900  12813 781,846,650    703 14,250,072,732 Aligned Sept
 *   185,345,034,900  12200  15,192,215     57  3,251,667,278 Aligned Oct
 *   696,080,736,600  12147  57,304,744     23 30,264,379,852 Aligned Nona
 *   113,523,757,100  12125   9,362,784      4 28,380,939,275 Aligned Dec
 *    57,337,280,500  12121   4,730,408  24758      2,315,909 Unary Chain
 *     8,934,315,400   6258   1,427,663   1280      6,979,933 Nishio Chain
 *    17,137,037,000   5239   3,271,051  10543      1,625,442 Multiple Chain
 *    17,483,967,200   1581  11,058,802   9957      1,755,947 Dynamic Chain
 *       139,202,200      3  46,400,733     30      4,640,073 Dynamic Plus
 *16,188,719,366,300
 * pzls          total (ns) (mmm:ss) (h:mm:ss)        each (ns)
 * 1465  16,229,575,573,600 (270:29) (4:30:29)   11,078,208,582
 * NOTES:
 * 1. Four and a half hours is obviously too slow. It took upto 9.356 seconds
 *    to produce a single hint, which I think is too long for a user to wait.
 *    I'd like to cap that at 3 seconds, which is about where kids presume
 *    they've "broken it", and start looking to there peers for reassurance.
 * 3. REDO isn't relevant.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-17</b> I'm still still trying to make the "correct" version of
 * Aligned 7+ Exclusion fast enough to use in practice, and I'm still failing.
 * The latest version takes 4 hours on top1465.d5.mt with A4567E_1C; which is
 * still still better than a poke in the eye with a blunt stick.
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        32,784,400 118223         277 662560             49 Naked Single
 *        22,201,900  51967         427 162410            136 Hidden Single
 *        95,370,400  35726       2,669  26393          3,613 Point and Claim
 *       182,423,100  23386       7,800   6528         27,944 Naked Pair
 *       119,079,200  21249       5,603   8454         14,085 Hidden Pair
 *       194,345,200  19156      10,145   1599        121,541 Naked Triple
 *       163,192,600  18756       8,700   1046        156,015 Hidden Triple
 *       138,628,500  18554       7,471    725        191,211 XY-Wing
 *        78,747,600  18036       4,366    683        115,296 Swampfish
 *        92,431,700  17769       5,201    313        295,308 Swordfish
 *       274,372,400  17682      15,517    129      2,126,917 Naked Quad
 *        28,405,000  17660       1,608     16      1,775,312 Jellyfish
 *       108,323,700  17655       6,135    332        326,276 XYZ-Wing
 *     2,557,757,500  17345     147,463   1133      2,257,508 Unique Rectangle
 *       309,293,600  16806      18,403     62      4,988,606 Bi-Uni Grave
 *     1,720,888,900  16776     102,580     23     74,821,256 Aligned Pair
 *    24,950,163,200  16753   1,489,295    597     41,792,568 Aligned Triple
 *   217,857,295,000  16213  13,437,198   1420    153,420,630 Aligned Quad
 *   910,262,011,100  14946  60,903,386   1375    662,008,735 Aligned Pent
 * 3,942,995,333,800  13741 286,951,119   1112  3,545,859,113 Aligned Hex
 * 9,375,693,412,500  12796 732,705,018    665 14,098,787,086 Aligned Sept
 *   100,246,411,400  12219   8,204,142     64  1,566,350,178 Aligned Oct
 *    77,561,226,700  12160   6,378,390     23  3,372,227,247 Aligned Nona
 *    27,067,974,800  12138   2,230,019      4  6,766,993,700 Aligned Dec
 *    64,780,301,800  12134   5,338,742  24806      2,611,477 Unary Chain
 *    10,177,372,300   6260   1,625,778   1280      7,951,072 Nishio Chain
 *    20,547,426,600   5241   3,920,516  10535      1,950,396 Multiple Chain
 *    20,122,576,200   1582  12,719,706   9967      2,018,920 Dynamic Chain
 *       251,310,300      3  83,770,100     30      8,377,010 Dynamic Plus
 *14,798,631,061,400
 * pzls         total (ns) (mmm:ss) (h:mm:ss)       each (ns)
 * 1465 14,835,135,511,200 (247:15) (4:07:15)  10,126,372,362
 * NOTES:
 * 1. This is 23 minutes faster than the previous version, which is a lot less
 *    that the three and a half-hours I was hoping for. It's not the required
 *    complete rethink, just refinements, using various stats to skip unhintable
 *    aligned sets.
 * 2. I'm shipping it as version 6.30.040 2020-01-17 12:38:40.
 * 3. REDO isn't relevant.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-20</b> HACK: Filters reduce A*E time for top1465.d5.mt only.
 * Similar filters may work for other puzzle-sets, and it may be possible to
 * extrapolate some of these filters mathematically into well-founded filters,
 * but these are just my lazy-assed happenstance effective filters, which
 * speed things up a bit, but only for top1465.d5.mt.<br>
 * FYI: I'm comparing timings to those from <b>KRC 2019-11-29</b>.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      22,141,800 117572        188 660730             33 Naked Single
 *      16,793,300  51499        326 161830            103 Hidden Single
 *      70,924,500  35316      2,008  26402          2,686 Point and Claim
 *     139,834,700  22995      6,081   6597         21,196 Naked Pair
 *      94,405,600  20823      4,533   8475         11,139 Hidden Pair
 *     144,644,100  18719      7,727   1611         89,785 Naked Triple
 *     123,480,200  18315      6,742   1102        112,050 Hidden Triple
 *     114,455,000  18098      6,324    739        154,878 XY-Wing
 *      64,108,500  17576      3,647    662         96,840 Swampfish
 *      72,794,400  17316      4,203    323        225,369 Swordfish
 *     200,661,300  17225     11,649    129      1,555,513 Naked Quad
 *      22,648,900  17203      1,316     12      1,887,408 Jellyfish
 *      87,664,500  17199      5,097    347        252,635 XYZ-Wing
 *   1,929,909,000  16875    114,364   1120      1,723,133 Unique Rectangle
 *     249,387,900  16343     15,259     68      3,667,469 Bi-Uni Grave
 *   1,276,973,100  16309     78,298     26     49,114,350 Aligned Pair
 *  18,912,407,900  16283  1,161,481    610     31,003,947 Aligned Triple
 * 134,137,076,100  15734  8,525,300   1518     88,364,345 Aligned Quad
 *  68,840,248,200  14383  4,786,223    515    133,670,384 Aligned Pent
 * 114,673,925,100  13916  8,240,437    375    305,797,133 Aligned Hex
 * 119,777,519,300  13586  8,816,246    180    665,430,662 Aligned Sept
 *  87,012,458,200  13426  6,480,892     68  1,279,594,973 Aligned Oct
 *  77,554,355,200  13365  5,802,795     25  3,102,174,208 Aligned Nona
 *  49,130,305,300  13341  3,682,655      2 24,565,152,650 Aligned Dec ========= THIS SHOULD BE 8!!!
 *  52,652,550,200  13339  3,947,263  26518      1,985,540 Unary Chain
 *   8,919,976,400   6884  1,295,754   1350      6,607,389 Nishio Chain
 *  17,616,441,800   5822  3,025,840  11694      1,506,451 Multiple Chain
 *  17,512,741,400   1652 10,600,933  10442      1,677,144 Dynamic Chain
 *     166,794,100      3 55,598,033     30      5,559,803 Dynamic Plus
 * 771,537,626,000
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   801,981,489,500 (13:21)       547,427,637
 * NOTES:
 * 1. 2019-11-29 took 24:51, compared to now 13:21, but don't get too excited
 * 2. I'm going to release this as version 6.30.040 2020-01-17 12:38:40.
 * 3. REDO isn't relevant.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-20</b> Fixed broken A10E filters to get my hints back.<br>
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      21,575,000 114640        188 648150            33 Naked Single
 *      15,683,600  49825        314 158220            99 Hidden Single
 *     166,378,500  34003      4,893   5615        29,631 Direct Naked Pair
 *     134,331,700  33576      4,000  13182        10,190 Direct Hidden Pair
 *     214,566,400  32573      6,587    844       254,225 Direct Naked Triple
 *     183,548,900  32519      5,644   1810       101,408 Direct Hidden Triple
 *      72,073,700  32382      2,225  22370         3,221 Point and Claim
 *      92,832,700  21582      4,301   3900        23,803 Naked Pair
 *      57,829,100  20089      2,878   5246        11,023 Hidden Pair
 *     113,745,000  18640      6,102   1173        96,969 Naked Triple
 *      93,418,900  18302      5,104   1007        92,769 Hidden Triple
 *     111,817,500  18097      6,178    739       151,309 XY-Wing
 *      63,235,000  17575      3,598    662        95,521 Swampfish
 *      71,122,100  17315      4,107    323       220,192 Swordfish
 *     202,720,600  17224     11,769    129     1,571,477 Naked Quad
 *      20,832,900  17202      1,211     12     1,736,075 Jellyfish
 *      86,111,400  17198      5,007    347       248,159 XYZ-Wing
 *   1,919,452,200  16874    113,752   1120     1,713,796 Unique Rectangle
 *     236,702,500  16342     14,484     68     3,480,919 Bi-Uni Grave
 *   1,264,973,400  16308     77,567     26    48,652,823 Aligned Pair
 *  19,051,062,300  16282  1,170,068    610    31,231,249 Aligned Triple
 * 133,867,839,800  15733  8,508,729   1518    88,186,982 Aligned Quad
 *  65,332,387,100  14382  4,542,649    515   126,859,004 Aligned Pent
 * 115,947,591,100  13915  8,332,561    375   309,193,576 Aligned Hex
 * 122,327,554,800  13585  9,004,604    179   683,394,160 Aligned Sept
 *  88,118,111,600  13426  6,563,243     68 1,295,854,582 Aligned Oct
 *  80,830,725,200  13365  6,047,940     24 3,367,946,883 Aligned Nona
 *  22,965,217,800  13342  1,721,272      7 3,280,745,400 Aligned Dec ========= ____ IT! CLOSE ENOUGH!
 *  52,570,099,200  13335  3,942,264  26496     1,984,076 Unary Chain
 *   9,066,614,300   6884  1,317,056   1350     6,716,010 Nishio Chain
 *  17,631,733,900   5822  3,028,466  11694     1,507,759 Multiple Chain
 *  18,042,679,700   1652 10,921,718  10442     1,727,895 Dynamic Chain
 *     197,365,500      3 65,788,500     30     6,578,850 Dynamic Plus
 * 751,091,933,400
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  781,099,630,100 (13:01)  533,173,808
 * NOTES:
 * 1. It's 30.0c and 95.8% humidity. The ceiling fan is on high, and my balls
 *    are still doing backstroke. Global bloody warming.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-27</b> This is the fastest run (over-night).
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        22,434,000 118246         189 662790             33 Naked Single
 *        21,298,700  51967         409 162220            131 Hidden Single
 *        79,572,000  35745       2,226  26394          3,014 Point and Claim
 *       139,073,800  23406       5,941   6527         21,307 Naked Pair
 *       100,317,300  21270       4,716   8451         11,870 Hidden Pair
 *       152,542,400  19178       7,954   1599         95,398 Naked Triple
 *       130,718,000  18778       6,961   1045        125,088 Hidden Triple
 *       119,826,400  18576       6,450    726        165,050 XY-Wing
 *        69,085,400  18057       3,825    683        101,149 Swampfish
 *        75,815,800  17790       4,261    313        242,223 Swordfish
 *       214,799,300  17703      12,133    129      1,665,110 Naked Quad
 *        22,790,000  17681       1,288     16      1,424,375 Jellyfish
 *        91,151,900  17676       5,156    332        274,553 XYZ-Wing
 *     1,885,394,900  17366     108,568   1133      1,664,073 Unique Rectangle
 *       271,316,800  16827      16,123     62      4,376,077 Bi-Uni Grave
 *     1,366,027,500  16797      81,325     23     59,392,500 Aligned Pair
 *    20,170,517,700  16774   1,202,487    599     33,673,652 Aligned Triple
 *   142,424,528,200  16232   8,774,305   1421    100,228,380 Aligned Quad
 *   657,587,146,600  14964  43,944,610   1370    479,990,617 Aligned Pent
 * 3,256,635,564,600  13763 236,622,507   1117  2,915,519,753 Aligned Hex
 * 7,979,553,029,700  12813 622,770,079    703 11,350,715,547 Aligned Sept
 *    79,697,874,700  12200   6,532,612     57  1,398,208,328 Aligned Oct
 *    59,276,329,400  12147   4,879,915     23  2,577,231,713 Aligned Nona
 *    24,234,269,600  12125   1,998,702      4  6,058,567,400 Aligned Dec
 *    49,785,152,000  12121   4,107,346  24758      2,010,871 Unary Chain
 *     8,552,062,900   6258   1,366,580   1280      6,681,299 Nishio Chain
 *    17,156,882,000   5239   3,274,839  10543      1,627,324 Multiple Chain
 *    18,059,578,000   1581  11,422,882   9957      1,813,756 Dynamic Chain
 *       162,991,400      3  54,330,466     30      5,433,046 Dynamic Plus
 *12,318,058,091,000
 * pzls         total (ns) (mm:ss)       each (ns)
 * 1465 12,354,683,692,700 (205:54)  8,433,231,189
 * NOTES:
 * 1. 205:54 has gone up to 238:01, without any credible cause. Sigh.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-27</b> Made A7E_1C sorts aligned set by collisions (etc).<br>
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        34,225,800 118246         289 662790             51 Naked Single
 *        28,080,700  51967         540 162220            173 Hidden Single
 *        94,474,900  35745       2,643  26394          3,579 Point and Claim
 *       182,573,300  23406       7,800   6527         27,972 Naked Pair
 *       115,622,000  21270       5,435   8451         13,681 Hidden Pair
 *       187,767,300  19178       9,790   1599        117,427 Naked Triple
 *       154,780,700  18778       8,242   1045        148,115 Hidden Triple
 *       135,235,900  18576       7,280    726        186,275 XY-Wing
 *        78,377,100  18057       4,340    683        114,754 Swampfish
 *        88,256,500  17790       4,961    313        281,969 Swordfish
 *       255,904,000  17703      14,455    129      1,983,751 Naked Quad
 *        27,820,800  17681       1,573     16      1,738,800 Jellyfish
 *       104,853,300  17676       5,931    332        315,823 XYZ-Wing
 *     2,698,740,400  17366     155,403   1133      2,381,942 Unique Rectangle
 *       316,979,700  16827      18,837     62      5,112,575 Bi-Uni Grave
 *     1,646,589,500  16797      98,028     23     71,590,847 Aligned Pair
 *    23,829,678,100  16774   1,420,631    599     39,782,434 Aligned Triple
 *   169,544,524,600  16232  10,445,079   1421    119,313,528 Aligned Quad
 *   749,079,384,800  14964  50,058,766   1370    546,773,273 Aligned Pent
 * 3,766,242,094,700  13763 273,649,792   1117  3,371,747,622 Aligned Hex
 * 9,223,334,092,800  12813 719,841,886    703 13,119,963,147 Aligned Sept
 *    94,706,960,500  12200   7,762,865     57  1,661,525,622 Aligned Oct
 *    70,712,628,100  12147   5,821,406     23  3,074,462,091 Aligned Nona
 *    28,861,793,300  12125   2,380,354      4  7,215,448,325 Aligned Dec
 *    61,006,960,900  12121   5,033,162  24758      2,464,131 Unary Chain
 *     9,615,748,600   6258   1,536,552   1280      7,512,303 Nishio Chain
 *    18,875,326,600   5239   3,602,849  10543      1,790,318 Multiple Chain
 *    19,399,160,900   1581  12,270,183   9957      1,948,293 Dynamic Chain
 *       194,030,500      3  64,676,833     30      6,467,683 Dynamic Plus
 *14,241,552,666,300
 * pzls         total (ns) (mm:ss)       each (ns)
 * 1465 14,281,921,444,800 (238:01)  9,748,751,839
 * NOTES:
 * 1. From 247:15 down to 238:01. Woop de bloody do!
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-28</b> The only difference between this and prev is I reverted
 * the in-situ declaration of all the int and boolean variables in A7E_1C to
 * make them final back to "externalised" automatic variables (ie how you'd do
 * it in ANSI-C), and I ran it overnight. Bit of a difference, eh what?<br>
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        22,421,900 118246         189 662790             33 Naked Single
 *        19,859,200  51967         382 162220            122 Hidden Single
 *        72,107,900  35745       2,017  26394          2,731 Point and Claim
 *       131,963,300  23406       5,638   6527         20,218 Naked Pair
 *        92,056,600  21270       4,328   8451         10,892 Hidden Pair
 *       140,995,100  19178       7,351   1599         88,177 Naked Triple
 *       120,675,500  18778       6,426   1045        115,478 Hidden Triple
 *       109,950,800  18576       5,918    726        151,447 XY-Wing
 *        62,661,900  18057       3,470    683         91,745 Swampfish
 *        69,922,000  17790       3,930    313        223,392 Swordfish
 *       194,958,100  17703      11,012    129      1,511,303 Naked Quad
 *        21,257,800  17681       1,202     16      1,328,612 Jellyfish
 *        85,260,500  17676       4,823    332        256,808 XYZ-Wing
 *     1,812,776,700  17366     104,386   1133      1,599,979 Unique Rectangle
 *       250,664,000  16827      14,896     62      4,042,967 Bi-Uni Grave
 *     1,252,788,200  16797      74,584     23     54,469,052 Aligned Pair
 *    18,685,826,100  16774   1,113,975    599     31,195,035 Aligned Triple
 *   133,857,366,600  16232   8,246,511   1421     94,199,413 Aligned Quad
 *   604,321,322,000  14964  40,385,012   1370    441,110,454 Aligned Pent
 * 2,937,206,668,900  13763 213,413,257   1117  2,629,549,390 Aligned Hex
 * 7,231,687,204,300  12813 564,402,341    703 10,286,895,027 Aligned Sept
 *    72,641,941,300  12200   5,954,257     57  1,274,420,022 Aligned Oct
 *    54,234,063,300  12147   4,464,811     23  2,358,002,752 Aligned Nona
 *    21,834,006,300  12125   1,800,742      4  5,458,501,575 Aligned Dec
 *    45,873,116,000  12121   3,784,598  24758      1,852,860 Unary Chain
 *     7,801,622,100   6258   1,246,663   1280      6,095,017 Nishio Chain
 *    15,219,904,200   5239   2,905,116  10543      1,443,602 Multiple Chain
 *    16,030,750,300   1581  10,139,627   9957      1,609,998 Dynamic Chain
 *       144,986,700      3  48,328,900     30      4,832,890 Dynamic Plus
 *11,163,999,097,600
 * pzls        total (ns)  (mmm:ss)         each (ns)
 * 1465 11,195,224,866,600 (186:35)     7,641,791,717
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 186.35 is 87:40, or 1 hour, 27 minutes,
 *    and 40 seconds faster.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-01-31</b> Finished A7E_1C filter in COLLISIONS_COMPARATOR.
 * I've exhumed the siblingsCount filter to the mainline to apply it earlier,
 * and promulgated that to A7E_2H, but I'm not going to bother promulgating it
 * to the other A*Es (it's too much of a pain in the ass).
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        24,187,800 118252         204 662770             36 Naked Single
 *        22,835,100  51975         439 162240            140 Hidden Single
 *        77,656,900  35751       2,172  26397          2,941 Point and Claim
 *       152,738,100  23411       6,524   6529         23,393 Naked Pair
 *        99,521,700  21273       4,678   8459         11,765 Hidden Pair
 *       157,022,400  19179       8,187   1599         98,200 Naked Triple
 *       133,501,300  18779       7,109   1045        127,752 Hidden Triple
 *       112,825,500  18577       6,073    727        155,193 XY-Wing
 *        69,917,700  18057       3,872    683        102,368 Swampfish
 *        78,052,500  17790       4,387    313        249,369 Swordfish
 *       215,551,700  17703      12,175    129      1,670,943 Naked Quad
 *        23,085,500  17681       1,305     16      1,442,843 Jellyfish
 *        86,261,100  17676       4,880    333        259,042 XYZ-Wing
 *     2,176,393,400  17365     125,332   1133      1,920,912 Unique Rectangle
 *       273,714,500  16826      16,267     62      4,414,750 Bi-Uni Grave
 *     1,403,647,800  16796      83,570     23     61,028,165 Aligned Pair
 *    20,589,702,000  16773   1,227,550    603     34,145,442 Aligned Triple
 *   143,943,446,000  16227   8,870,613   1422    101,226,052 Aligned Quad
 *   490,311,279,500  14958  32,779,200   1338    366,450,881 Aligned Pent
 * 1,832,804,854,800  13790 132,908,256   1145  1,600,702,929 Aligned Hex
 * 4,276,625,739,900  12812 333,798,449    703  6,083,393,655 Aligned Sept
 *    77,692,975,600  12200   6,368,276     57  1,363,034,659 Aligned Oct
 *    62,373,110,400  12147   5,134,857     23  2,711,874,365 Aligned Nona
 *    97,799,518,200  12125   8,065,939      4 24,449,879,550 Aligned Dec
 *    50,683,417,000  12121   4,181,455  24758      2,047,153 Unary Chain
 *     8,599,151,100   6258   1,374,105   1280      6,718,086 Nishio Chain
 *    16,895,633,800   5239   3,224,973  10543      1,602,545 Multiple Chain
 *    17,810,512,300   1581  11,265,346   9957      1,788,742 Dynamic Chain
 *       185,331,000      3  61,777,000     30      6,177,700 Dynamic Plus
 * 7,101,421,584,600
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  7,136,858,924,400  (118:56)  4,871,576,057
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 118:56 = 155:19 or 2 hrs 35 mins faster.
 * 2. I'm shipping it as version 6.30.043 2020-01-31 19:08:43
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-01</b> Today I tried filtering by alignedSetNumber. It was a
 * failure. For some reason (I suspect the optimiser) it was substantially
 * (like an hour) slower, so I reverted to lastnights 7z and made a couple
 * of relatively simple filter changes, to repeat ancillary lessons, which
 * is really what I already knew: apply the most deterministic filter first.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        23,527,900 118252         198 662770            35 Naked Single
 *        20,585,400  51975         396 162240           126 Hidden Single
 *        78,363,500  35751       2,191  26397         2,968 Point and Claim
 *       143,477,300  23411       6,128   6529        21,975 Naked Pair
 *        95,445,700  21273       4,486   8459        11,283 Hidden Pair
 *       148,890,900  19179       7,763   1599        93,115 Naked Triple
 *       126,310,700  18779       6,726   1045       120,871 Hidden Triple
 *       107,943,400  18577       5,810    727       148,477 XY-Wing
 *        66,936,300  18057       3,706    683        98,003 Swampfish
 *        70,929,900  17790       3,987    313       226,613 Swordfish
 *       202,620,000  17703      11,445    129     1,570,697 Naked Quad
 *        21,123,500  17681       1,194     16     1,320,218 Jellyfish
 *        80,876,700  17676       4,575    333       242,872 XYZ-Wing
 *     2,027,450,000  17365     116,754   1133     1,789,452 Unique Rectangle
 *       260,375,100  16826      15,474     62     4,199,598 Bi-Uni Grave
 *     1,296,802,100  16796      77,208     23    56,382,700 Aligned Pair
 *    19,269,500,300  16773   1,148,840    603    31,956,053 Aligned Triple
 *   136,392,807,000  16227   8,405,300   1422    95,916,179 Aligned Quad
 *   453,663,559,000  14958  30,329,158   1338   339,060,955 Aligned Pent
 * 1,716,695,533,600  13790 124,488,436   1145 1,499,297,409 Aligned Hex
 * 3,976,365,925,600  12812 310,362,622    703 5,656,281,544 Aligned Sept
 *    74,000,478,000  12200   6,065,612     57 1,298,254,000 Aligned Oct
 *    56,322,351,900  12147   4,636,729     23 2,448,797,908 Aligned Nona
 *    22,316,872,500  12125   1,840,566      4 5,579,218,125 Aligned Dec
 *    46,473,897,900  12121   3,834,163  24758     1,877,126 Unary Chain
 *     7,827,153,000   6258   1,250,743   1280     6,114,963 Nishio Chain
 *    15,115,700,000   5239   2,885,226  10543     1,433,719 Multiple Chain
 *    15,936,408,600   1581  10,079,954   9957     1,600,523 Dynamic Chain
 *       134,305,200      3  44,768,400     30     4,476,840 Dynamic Plus
 * 6,545,286,151,000
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,577,964,039,300  (109:37)  4,490,077,842
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 109:37 = 164:38 or 2 hrs 44 mins faster.
 *    I'll be satisfied if/when I get it down to an hour, so I still have to
 *    find 50 minutes somewhere.
 * 2. I have no idea where to go from here. This thing (especially A7E_1C) is
 *    about as fast as I know how to make it. I still have a nagging feeling
 *    that I'm missing the magic, but I'm missing it, because I'm not magic.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-02</b> Working on A7E_1C DOG_1A "is empty tests":<ul>
 *  <li>tune: test c6b4: wo 5:55 => w 5:42 F10
 *  <li>tune: test c6b3,c5b3: wo 5:42 => w 5.40 F10
 *  <li>tune: test c6b2,c5b2,c4b2: wo 5:40 => w 06:50 F10 FAIL!
 *  <li>same again but with pre recursiveIsOn 05:49 FAIL!
 * </ul>
 * These came out faster for the first 10 puzzles, which I presumed would be
 * representative of performance for all top1465. It isn't, apparently. Sigh.
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        26,092,700 118252         220 662770             39 Naked Single
 *        23,167,100  51975         445 162240            142 Hidden Single
 *        77,902,600  35751       2,179  26397          2,951 Point and Claim
 *       162,086,000  23411       6,923   6529         24,825 Naked Pair
 *       103,797,100  21273       4,879   8459         12,270 Hidden Pair
 *       166,508,600  19179       8,681   1599        104,132 Naked Triple
 *       138,755,400  18779       7,388   1045        132,780 Hidden Triple
 *       117,622,800  18577       6,331    727        161,792 XY-Wing
 *        69,670,800  18057       3,858    683        102,007 Swampfish
 *        80,474,200  17790       4,523    313        257,106 Swordfish
 *       227,967,300  17703      12,877    129      1,767,188 Naked Quad
 *        25,408,100  17681       1,437     16      1,588,006 Jellyfish
 *        86,667,900  17676       4,903    333        260,263 XYZ-Wing
 *     2,340,695,800  17365     134,793   1133      2,065,927 Unique Rectangle
 *       274,969,200  16826      16,341     62      4,434,987 Bi-Uni Grave
 *     1,460,546,800  16796      86,958     23     63,502,034 Aligned Pair
 *    21,530,396,200  16773   1,283,634    603     35,705,466 Aligned Triple
 *   150,302,591,900  16227   9,262,500   1422    105,698,025 Aligned Quad
 *   510,554,222,700  14958  34,132,519   1338    381,580,136 Aligned Pent
 * 1,870,253,341,100  13790 135,623,882   1145  1,633,409,031 Aligned Hex
 * 4,201,172,233,200  12812 327,909,165    703  5,976,062,920 Aligned Sept
 *    79,505,432,700  12200   6,516,838     57  1,394,832,152 Aligned Oct
 *    63,307,307,500  12147   5,211,764     23  2,752,491,630 Aligned Nona
 *   104,546,017,500  12125   8,622,351      4 26,136,504,375 Aligned Dec
 *    52,871,897,000  12121   4,362,007  24758      2,135,547 Unary Chain
 *     8,403,871,200   6258   1,342,900   1280      6,565,524 Nishio Chain
 *    16,266,583,900   5239   3,104,902  10543      1,542,880 Multiple Chain
 *    17,026,540,300   1581  10,769,475   9957      1,710,007 Dynamic Chain
 *       153,008,100      3  51,002,700     30      5,100,270 Dynamic Plus
 * 7,101,275,775,700
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  7,137,815,459,700  (118:57)  4,872,228,982
 * NOTES:
 * 1. Back up from 109:37 to 118:57, ie about 9 mins slower. Sigh. I shall try
 *    it again tonight, in the cool of the night.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-02</b> Just reduced A7E_1C MAX_CANDIDATES from 60 to 57, which
 * drops 7 hints: {@code 3/11 11/34 50/9 98/11 583/24 771/12 1120/90} and saves
 * nearly 8 minutes. Sounds like a fair trade.
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        23,564,100 118249         199 662770             35 Naked Single
 *        21,086,400  51972         405 162240            129 Hidden Single
 *        73,737,300  35748       2,062  26397          2,793 Point and Claim
 *       145,429,000  23408       6,212   6529         22,274 Naked Pair
 *        99,493,900  21270       4,677   8460         11,760 Hidden Pair
 *       154,256,000  19176       8,044   1599         96,470 Naked Triple
 *       128,978,000  18776       6,869   1045        123,423 Hidden Triple
 *       109,009,200  18574       5,868    727        149,943 XY-Wing
 *        67,800,300  18054       3,755    683         99,268 Swampfish
 *        76,275,900  17787       4,288    313        243,692 Swordfish
 *       210,858,300  17700      11,912    129      1,634,560 Naked Quad
 *        23,198,700  17678       1,312     16      1,449,918 Jellyfish
 *        82,756,500  17673       4,682    333        248,518 XYZ-Wing
 *     2,025,922,200  17362     116,687   1133      1,788,104 Unique Rectangle
 *       263,313,800  16823      15,652     62      4,246,996 Bi-Uni Grave
 *     1,334,259,500  16793      79,453     23     58,011,282 Aligned Pair
 *    20,019,663,700  16770   1,193,778    603     33,200,105 Aligned Triple
 *   138,551,230,800  16224   8,539,893   1422     97,434,058 Aligned Quad
 *   469,050,575,100  14955  31,364,130   1338    350,560,967 Aligned Pent
 * 1,781,503,838,100  13787 129,216,206   1145  1,555,898,548 Aligned Hex
 * 3,854,401,330,000  12809 300,913,524    694  5,553,892,406 Aligned Sept
 *    74,901,481,100  12205   6,136,950     60  1,248,358,018 Aligned Oct
 *    58,417,602,000  12149   4,808,428     23  2,539,895,739 Aligned Nona
 *    89,054,548,900  12127   7,343,493      4 22,263,637,225 Aligned Dec
 *    48,694,166,600  12123   4,016,676  24759      1,966,725 Unary Chain
 *     8,361,710,200   6259   1,335,949   1280      6,532,586 Nishio Chain
 *    16,465,205,200   5240   3,142,214  10544      1,561,571 Multiple Chain
 *    16,960,394,000   1581  10,727,636   9957      1,703,363 Dynamic Chain
 *       157,630,600      3  52,543,533     30      5,254,353 Dynamic Plus
 * 6,581,379,315,400
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,614,276,114,300  (110:14)  4,514,864,241
 * NOTES:
 * 1. Back down from last 118:57 to 110:14. Still not fastest. I shall run it
 *    again tonight, in the cool.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-02</b> Just replaced A7E_1C's avbs array with a virtual array.
 * Hard to believe such a simple thing cost 3 minutes. There's a lesson. I shall
 * promulgate everywhere and rerun it tonight (I mean right now, really).
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        21,845,000 118249         184 662770            32 Naked Single
 *        18,750,200  51972         360 162240           115 Hidden Single
 *        67,992,000  35748       1,901  26397         2,575 Point and Claim
 *       138,057,900  23408       5,897   6529        21,145 Naked Pair
 *        91,903,900  21270       4,320   8460        10,863 Hidden Pair
 *       148,085,100  19176       7,722   1599        92,611 Naked Triple
 *       124,523,400  18776       6,632   1045       119,161 Hidden Triple
 *       103,988,400  18574       5,598    727       143,037 XY-Wing
 *        64,556,200  18054       3,575    683        94,518 Swampfish
 *        70,511,100  17787       3,964    313       225,275 Swordfish
 *       201,644,400  17700      11,392    129     1,563,134 Naked Quad
 *        20,998,600  17678       1,187     16     1,312,412 Jellyfish
 *        78,048,100  17673       4,416    333       234,378 XYZ-Wing
 *     1,947,097,900  17362     112,147   1133     1,718,533 Unique Rectangle
 *       253,294,900  16823      15,056     62     4,085,401 Bi-Uni Grave
 *     1,281,981,400  16793      76,340     23    55,738,321 Aligned Pair
 *    19,026,872,800  16770   1,134,577    603    31,553,686 Aligned Triple
 *   136,305,403,300  16224   8,401,467   1422    95,854,713 Aligned Quad
 *   452,637,027,600  14955  30,266,601   1338   338,293,742 Aligned Pent
 * 1,695,653,394,900  13787 122,989,293   1145 1,480,919,995 Aligned Hex
 * 3,626,926,698,200  12809 283,154,555    694 5,226,119,161 Aligned Sept
 *    72,102,389,500  12205   5,907,610     60 1,201,706,491 Aligned Oct
 *    55,853,016,300  12149   4,597,334     23 2,428,392,013 Aligned Nona
 *    21,936,047,400  12127   1,808,860      4 5,484,011,850 Aligned Dec
 *    46,714,399,000  12123   3,853,369  24759     1,886,764 Unary Chain
 *     7,881,683,900   6259   1,259,256   1280     6,157,565 Nishio Chain
 *    15,513,555,200   5240   2,960,602  10544     1,471,315 Multiple Chain
 *    16,226,963,500   1581  10,263,734   9957     1,629,704 Dynamic Chain
 *       156,529,400      3  52,176,466     30     5,217,646 Dynamic Plus
 * 6,171,567,259,500
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,209,060,952,500  (103:29)  4,238,266,861
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 103:29 = 170:46 or 2 hrs 51 mins faster.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-03 07:28</b> Last nights re-run, as promised.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        21,136,100 118249         178 662770            31 Naked Single
 *        19,358,500  51972         372 162240           119 Hidden Single
 *        73,457,000  35748       2,054  26397         2,782 Point and Claim
 *       133,743,200  23408       5,713   6529        20,484 Naked Pair
 *        90,955,600  21270       4,276   8460        10,751 Hidden Pair
 *       143,488,800  19176       7,482   1599        89,736 Naked Triple
 *       122,351,800  18776       6,516   1045       117,083 Hidden Triple
 *       103,513,600  18574       5,573    727       142,384 XY-Wing
 *        62,043,500  18054       3,436    683        90,839 Swampfish
 *        71,522,000  17787       4,021    313       228,504 Swordfish
 *       196,452,900  17700      11,099    129     1,522,890 Naked Quad
 *        21,642,900  17678       1,224     16     1,352,681 Jellyfish
 *        76,881,800  17673       4,350    333       230,876 XYZ-Wing
 *     1,956,424,300  17362     112,684   1133     1,726,764 Unique Rectangle
 *       240,426,500  16823      14,291     62     3,877,846 Bi-Uni Grave
 *     1,254,807,600  16793      74,722     23    54,556,852 Aligned Pair
 *    18,780,398,800  16770   1,119,880    603    31,144,939 Aligned Triple
 *   134,871,737,000  16224   8,313,100   1422    94,846,509 Aligned Quad
 *   438,555,976,500  14955  29,325,040   1338   327,769,788 Aligned Pent
 * 1,755,877,752,400  13787 127,357,492   1145 1,533,517,687 Aligned Hex
 * 3,552,868,239,900  12809 277,372,803    694 5,119,406,685 Aligned Sept
 *    69,847,431,700  12205   5,722,853     60 1,164,123,861 Aligned Oct
 *    54,887,793,900  12149   4,517,885     23 2,386,425,821 Aligned Nona
 *    21,544,900,200  12127   1,776,605      4 5,386,225,050 Aligned Dec
 *    45,934,201,000  12123   3,789,012  24759     1,855,252 Unary Chain
 *     7,737,666,400   6259   1,236,246   1280     6,045,051 Nishio Chain
 *    14,839,529,600   5240   2,831,971  10544     1,407,390 Multiple Chain
 *    15,674,036,100   1581   9,914,001   9957     1,574,172 Dynamic Chain
 *       154,976,000      3  51,658,666     30     5,165,866 Dynamic Plus
 * 6,136,162,845,600
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,175,376,908,300  (102:55)  4,215,274,340
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 102:55 = 171:20 or 2 hrs 51 mins faster.
 *    One hundred is a significant psychological milestone. I shall prevail!
 * 2. Promulgate "filter" ie COLLISIONS_COMPARATOR.reject() to A234,6_2H,8910E.
 *    * A6E_2H 15:50 => collisionComparator 22:35 =revert=> 13:50 with any*,
 *      so belay CC to A2345E (but still promulgate to A8910E),
 *      and promulgate any* to A4,8910E.
 *    * Latest (2020-02-04 07:10:10) with any* in A45678910E all hacked:
 *      pzls       total (ns)  (mm:ss)    each (ns)
 *      1465  790,423,237,500  (13:10)  539,538,046  on a cool PC, which is
 *      actually 9 secs slower than prev fastest all-hacked run
 *    * Now to promulgate COLLISIONS_COMPARATOR to A8910E (hacked).
 * 3. Recall 2020-02-01 "tried filtering by alignedSetNumber (cnt)", but now
 *    we just return when the maxSuccessfulAlingedSetNumber is exceeded.
 *    I suspect that the optimizer only has a problem when you skip logic in
 *    "lead up" use-cases, so that your logic "kicks in" after the JIT compiler
 *    has finished it's examination. Piece of s__t! C# anyone? Except I don't
 *    want to suck Bills cock either. Sigh.
 *    .... release 6.30.045 for a fresh 7z ....
 *    see: <b>KRC 2020-02-04 15:03 hacked
 *    and: <b>KRC 2020-02-04 16:50 correct
 * 4. Recall 2020-02-01 "no idea where to go from here?" Well let's: every combo
 *    examined must include atleast 1 unknown value. If we make the cell-to-be
 *    -examined the "focus" is last (right-most) because it could take one pass
 *    to allow all values; upon which we throw an AllAllowedException, the
 *    catch of which "rotates" the "ring" of aligned set cells one position,
 *    to make the next cell the "focus". When a "focus cell" goes all the way
 *    we've found an exclusion, like now except we're not spending ken-ages
 *    blindly testing and retesting each and every possible combination of
 *    values that're ALL already bloody-well known to be allowed.
 *    Mathematically: I'm trying to fail linearly 7+6+6+3+3+2 =    27
 *                       instead of exponentially 7*6*6*3*3*2 = 4,536
 *    The hit-rate is so low that what really matters is "failing fast"; not the
 *    positive path, as is far more usual. So the trick is AllAllowedException.
 * 5. I shall try ideas 3 & 4 in A7E_1C, but I shall release first, to get
 *    a backup 7z. "Focus" has a high probability of multiple failures, so I
 *    expect the next release will take a week or two.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-04 15:03</b> Finished promulgating collisionComparator and
 * the any* technique to update the avb VIRTUAL array. It's time to release to
 * get a fresh 7z. Here's the latest timings.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      21,124,300 117579        179 660860            31 Naked Single
 *      19,452,300  51493        377 161700           120 Hidden Single
 *      73,166,300  35323      2,071  26406         2,770 Point and Claim
 *     128,182,900  23000      5,573   6596        19,433 Naked Pair
 *      89,795,900  20829      4,311   8477        10,592 Hidden Pair
 *     143,403,700  18723      7,659   1611        89,015 Naked Triple
 *     121,290,900  18319      6,621   1101       110,164 Hidden Triple
 *     102,824,200  18103      5,679    739       139,139 XY-Wing
 *      62,720,600  17581      3,567    662        94,744 Swampfish
 *      67,303,400  17321      3,885    323       208,369 Swordfish
 *     196,707,500  17230     11,416    129     1,524,864 Naked Quad
 *      20,723,300  17208      1,204     12     1,726,941 Jellyfish
 *      76,544,900  17204      4,449    347       220,590 XYZ-Wing
 *   1,903,135,500  16880    112,744   1120     1,699,228 Unique Rectangle
 *     232,158,100  16348     14,201     68     3,414,089 Bi-Uni Grave
 *   1,224,076,400  16314     75,032     26    47,079,861 Aligned Pair
 *  18,259,171,700  16288  1,121,019    610    29,933,068 Aligned Triple
 * 119,737,979,500  15739  7,607,724   1519    78,826,846 Aligned Quad
 *  74,316,261,300  14387  5,165,514    514   144,584,165 Aligned Pent
 * 108,731,291,800  13921  7,810,594    378   287,648,920 Aligned Hex
 *  90,415,260,600  13588  6,654,052    175   516,658,632 Aligned Sept
 *  68,041,553,000  13432  5,065,630     74   919,480,445 Aligned Oct
 *  65,286,130,300  13366  4,884,492     24 2,720,255,429 Aligned Nona
 *  31,273,287,400  13343  2,343,797      7 4,467,612,485 Aligned Dec
 *  50,451,259,500  13336  3,783,087  26515     1,902,744 Unary Chain
 *   8,718,865,700   6884  1,266,540   1350     6,458,419 Nishio Chain
 *  17,127,322,900   5822  2,941,828  11694     1,464,624 Multiple Chain
 *  17,334,985,800   1652 10,493,332  10442     1,660,121 Dynamic Chain
 *     148,637,200      3 49,545,733     30     4,954,573 Dynamic Plus
 * 674,324,616,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   712,335,487,800 (11:52)       486,235,827
 * NOTES:
 * 1. The latest 11:52 (all hacked) is comparable to 2019-11-29 25:44, so it's
 *    taking half the time, but before you get too excited, hackTop1465 denotes
 *    hacks that are applicable to top1465.d5.mt only. It would be possible to
 *    "screw this implementation down" to any collection of puzzles, I chose
 *    these puzzles because they're the ones I could find for free, quickly.
 * 2. I shall release this now as V6_30.045 2020-02-04 15:03:45.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-04 16:50</b> A "correct" run after promulgating CC and any*.
 * I'm angling for the 100 minute barrier.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        21,308,500 118250         180 662780            32 Naked Single
 *        19,724,200  51972         379 162230           121 Hidden Single
 *        77,496,100  35749       2,167  26397         2,935 Point and Claim
 *       128,758,600  23409       5,500   6529        19,721 Naked Pair
 *        89,976,500  21271       4,230   8460        10,635 Hidden Pair
 *       142,705,000  19177       7,441   1599        89,246 Naked Triple
 *       121,344,700  18777       6,462   1045       116,119 Hidden Triple
 *       103,070,800  18575       5,548    727       141,775 XY-Wing
 *        64,920,700  18055       3,595    683        95,052 Swampfish
 *        69,725,800  17788       3,919    313       222,766 Swordfish
 *       196,038,300  17701      11,074    129     1,519,676 Naked Quad
 *        20,445,800  17679       1,156     16     1,277,862 Jellyfish
 *        77,244,700  17674       4,370    333       231,966 XYZ-Wing
 *     1,926,335,600  17363     110,944   1135     1,697,211 Unique Rectangle
 *       240,734,300  16823      14,309     62     3,882,811 Bi-Uni Grave
 *     1,251,012,800  16793      74,496     23    54,391,860 Aligned Pair
 *    18,784,772,600  16770   1,120,141    603    31,152,193 Aligned Triple
 *   116,864,210,500  16224   7,203,168   1422    82,182,989 Aligned Quad
 *   451,268,188,400  14955  30,175,071   1338   337,270,693 Aligned Pent
 * 1,678,428,838,000  13787 121,739,960   1147 1,463,320,695 Aligned Hex
 *
 * 3,579,184,637,300  12808 279,449,144    694 5,157,326,566 Aligned Sept
 *    57,874,404,000  12204   4,742,248     59   980,922,101 Aligned Oct ======= SHOULD BE 60
 *    55,600,797,800  12149   4,576,574     23 2,417,425,991 Aligned Nona
 *    14,109,073,900  12127   1,163,443      3 4,703,024,633 Aligned Dec ======= SHOULD BE 4
 *
 * REPRINT 2020-02-02 reduced A7E_1C MAX_CANDIDATES from 60 to 57 for speed)
 * 3,854,401,330,000  12809 300,913,524    694  5,553,892,406 Aligned Sept
 *    74,901,481,100  12205   6,136,950     60  1,248,358,018 Aligned Oct
 *    58,417,602,000  12149   4,808,428     23  2,539,895,739 Aligned Nona
 *    89,054,548,900  12127   7,343,493      4 22,263,637,225 Aligned Dec
 *
 * RERUN without any filters in A8E and A10E and two hints are back
 * 4,341,473,592,200  12809  338,939,307   694  6,255,725,637 Aligned Sept
 *   203,116,013,400  12205   16,642,033    60  3,385,266,890 Aligned Oct
 *    69,764,731,700  12149    5,742,425    23  3,033,249,204 Aligned Nona
 *   178,668,826,700  12127   14,733,143     4 44,667,206,675 Aligned Dec
 *
 *    45,398,665,400  12124   3,744,528  24760     1,833,548 Unary Chain
 *     7,872,237,200   6259   1,257,746   1280     6,150,185 Nishio Chain
 *    14,788,469,700   5240   2,822,227  10544     1,402,548 Multiple Chain
 *    15,712,245,700   1581   9,938,169   9957     1,578,010 Dynamic Chain
 *       149,817,000      3  49,939,000     30     4,993,900 Dynamic Plus
 * 6,060,587,199,900
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,099,238,285,200  (101:39)  4,163,302,583
 * NOTES:
 * 1. We found 59 of 60 A8E hints, and 3 of 4 A10E hints, so it's wrong, so I
 *    shall have to remove the filters from A8E and A10E and try again.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-06 00:40</b> fixed bug in collisionComparator.score which
 * omits COLLISIONS! Declare ALL variables at start of method, no exclusions.
 * Stop at max successful alignedSetCount in ALL A*E's.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        20,189,900 118249         170 662770            30 Naked Single
 *        19,561,200  51972         376 162240           120 Hidden Single
 *        70,324,600  35748       1,967  26397         2,664 Point and Claim
 *       134,365,400  23408       5,740   6529        20,579 Naked Pair
 *        92,790,700  21270       4,362   8460        10,968 Hidden Pair
 *       144,107,600  19176       7,514   1599        90,123 Naked Triple
 *       122,862,000  18776       6,543   1045       117,571 Hidden Triple
 *       102,365,000  18574       5,511    727       140,804 XY-Wing
 *        63,046,700  18054       3,492    683        92,308 Swampfish
 *        70,910,400  17787       3,986    313       226,550 Swordfish
 *       198,197,700  17700      11,197    129     1,536,416 Naked Quad
 *        21,578,800  17678       1,220     16     1,348,675 Jellyfish
 *        76,769,200  17673       4,343    333       230,538 XYZ-Wing
 *     1,919,081,600  17362     110,533   1133     1,693,805 Unique Rectangle
 *       235,861,900  16823      14,020     62     3,804,224 Bi-Uni Grave
 *     1,205,548,500  16793      71,788     23    52,415,152 Aligned Pair
 *    18,566,183,800  16770   1,107,106    603    30,789,691 Aligned Triple
 *   117,766,121,600  16224   7,258,759   1422    82,817,244 Aligned Quad
 *   425,986,334,600  14955  28,484,542   1338   318,375,436 Aligned Pent
 * 1,671,404,258,800  13787 121,230,453   1145 1,459,741,710 Aligned Hex
 * 3,259,051,191,200  12809 254,434,475    694 4,696,039,180 Aligned Sept
 *    72,912,874,600  12205   5,974,016     60 1,215,214,576 Aligned Oct
 *    52,505,412,700  12149   4,321,788     23 2,282,844,030 Aligned Nona
 *    13,006,227,900  12127   1,072,501      4 3,251,556,975 Aligned Dec
 *    45,971,139,900  12123   3,792,059  24759     1,856,744 Unary Chain
 *     7,822,974,600   6259   1,249,876   1280     6,111,698 Nishio Chain
 *    15,379,823,900   5240   2,935,080  10544     1,458,632 Multiple Chain
 *    16,293,055,800   1581  10,305,538   9957     1,636,341 Dynamic Chain
 *       158,123,100      3  52,707,700     30     5,270,770 Dynamic Plus
 * 5,721,321,283,700
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  5,752,453,357,200  (95:52)  3,926,589,322
 * NOTES:
 * 1. All hint counts are as expected, and we're under 100 minutes. Yeah!
 * 2. I shall now release V6_30.046 2020-02-06 00:40:46 and then try a
 *    "focus" implementation in A5E first.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-10 09:23</b> Reverted to DiufSudoku_V6_30.046.2020-02-06.OK.7z
 * and put isAnyAllowed* in A7E_1C. At each stage in the DOG_1A loop we check
 * if all-potential-values-of-all-cells-to-my-right are already allowed, and if
 * so we just need to check if ANY combo-to-my-right is allowed, and if so we
 * allow the current potential value of the current cell.
 * So isAnyAllowed123456 reduces an O(7*7*7*7*7*7=117649) to just O(1'ish) for
 * success but still O(117649) for failure, and here's a table of the number of
 * calls of each isAnyAllowed* method for top1465:<pre>{@code
 *     123456 =  1,177,806,388
 *      23456 =  1,132,427,506
 *       3456 =  1,511,417,475
 *        456 =  2,829,841,339
 *         56 =  5,810,640,285
 *          6 = 10,001,013,133
 * }</pre>
 * so it's a bit faster.
 * <p>NB: The hard part has been dealing with the JIT compiler which stops-work
 * before the larger (isAnyAllowed123456/23456) methods are invoked, so they're
 * NOT compiled (they're interpreted, which is slower) but at least they call
 * the smaller methods (isAnyAllowed3456/456/56/6) which are compiled to do the
 * bulk of the repetative work. I wish I could force methods to be JIT compiled!
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        36,732,400 118252         310 662770            55 Naked Single
 *        25,317,100  51975         487 162240           156 Hidden Single
 *        91,944,700  35751       2,571  26397         3,483 Point and Claim
 *       173,144,800  23411       7,395   6529        26,519 Naked Pair
 *       112,546,200  21273       5,290   8459        13,304 Hidden Pair
 *       178,201,500  19179       9,291   1599       111,445 Naked Triple
 *       152,075,100  18779       8,098   1045       145,526 Hidden Triple
 *       130,570,000  18577       7,028    727       179,601 XY-Wing
 *        77,489,100  18057       4,291    683       113,454 Swampfish
 *        86,892,900  17790       4,884    313       277,613 Swordfish
 *       252,168,000  17703      14,244    129     1,954,790 Naked Quad
 *        26,564,800  17681       1,502     16     1,660,300 Jellyfish
 *        95,031,200  17676       5,376    333       285,378 XYZ-Wing
 *     2,505,443,700  17365     144,281   1133     2,211,336 Unique Rectangle
 *       320,205,100  16826      19,030     62     5,164,598 Bi-Uni Grave
 *     1,587,466,500  16796      94,514     23    69,020,282 Aligned Pair
 *    23,823,265,600  16773   1,420,334    603    39,507,903 Aligned Triple
 *   149,740,017,500  16227   9,227,831   1422   105,302,403 Aligned Quad
 *   547,884,827,500  14958  36,628,214   1338   409,480,439 Aligned Pent
 * 2,199,463,251,000  13790 159,496,972   1145 1,920,928,603 Aligned Hex
 * 1,717,685,320,800  12812 134,068,476    703 2,443,364,609 Aligned Sept	MAX_CANDIDATES restored to 60 (all hints).
 *    97,045,020,900  12200   7,954,509     57 1,702,544,226 Aligned Oct
 *    71,302,222,100  12147   5,869,945     23 3,100,096,613 Aligned Nona
 *    17,287,508,300  12125   1,425,773      4 4,321,877,075 Aligned Dec
 *    60,099,285,800  12121   4,958,277  24758     2,427,469 Unary Chain
 *    10,041,556,700   6258   1,604,595   1280     7,844,966 Nishio Chain
 *    20,574,444,500   5239   3,927,170  10543     1,951,479 Multiple Chain
 *    21,519,434,500   1581  13,611,280   9957     2,161,236 Dynamic Chain
 *       404,891,200      3 134,963,733     30    13,496,373 Dynamic Plus
 * 4,942,722,839,500
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 4,978,994,244,900 (82:58)     3,398,630,883
 * NOTES:
 * 1. A7E hint count is 703, as expected. Runtime reduced from 95:52 mm:ss down
 *    to 82:58 which is a gain of 12:54.
 * 2. So isAnyAllowed* in now in:
 *    * A7E_1C from 3,259 to 1,420 saving 30:39
 *    * A7E_2H from 27:43 but no comparer
 *    * A6E_1C from 2,199 to   735 saving 24:24
 *    * A6E_2H from   108 to    50 saving  0:28
 *    * A5E_1C from   548 to   336 saving  3:32
 *    * A4E_1C from   149 to    79 saving  1:10
 *    then release V6_30.047 2020-02-10 20:07:47
 * 3. A234E_1C + A5678910E_2H took 10:22 from 11:52 saving 1:30
 * 4. Next is A8E_2H then A8E_1C if I'm not dead yet.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-11 06:44</b> Promulgated isAnyAllowed* to A4E_1C and A8E_2H.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter              was
 *        20,803,200 118277         175 662610            31 Naked Single     662770
 *        19,931,000  52016         383 162370           122 Hidden Single    162240
 *        76,512,200  35779       2,138  26392         2,899 Point and Claim   26397
 *       135,034,700  23440       5,760   6536        20,660 Naked Pair         6529
 *        93,860,900  21295       4,407   8456        11,099 Hidden Pair        8459
 *       146,184,400  19205       7,611   1605        91,080 Naked Triple       1599
 *       124,238,600  18804       6,607   1043       119,116 Hidden Triple      1045
 *       107,493,300  18602       5,778    729       147,453 XY-Wing             727
 *        62,085,200  18081       3,433    685        90,635 Swampfish           683
 *        75,147,800  17812       4,218    313       240,088 Swordfish
 *       201,278,400  17725      11,355    129     1,560,297 Naked Quad
 *        22,367,200  17703       1,263     16     1,397,950 Jellyfish
 *        78,186,700  17698       4,417    333       234,794 XYZ-Wing
 *     1,999,944,500  17387     115,025   1133     1,765,176 Unique Rectangle
 *       240,015,200  16848      14,245     62     3,871,212 Bi-Uni Grave
 *     1,254,371,800  16818      74,585     23    54,537,904 Aligned Pair
 *    19,118,025,500  16795   1,138,316    604    31,652,360 Aligned Triple      603
 *    78,431,351,400  16249   4,826,841    985    79,625,737 Aligned Quad       1422  -437 The root cause, me thinks.
 *   290,264,294,200  15338  18,924,520   1764   164,548,919 Aligned Pent       1338
 *   724,797,931,800  13801  52,517,783   1149   630,807,599 Aligned Hex        1145
 * 1,260,851,409,600  12819  98,358,016    706 1,785,908,512 Aligned Sept        703
 *    51,208,927,600  12205   4,195,733     33 1,551,785,684 Aligned Oct          57
 *    60,420,773,600  12174   4,963,099     47 1,285,548,374 Aligned Nona         23
 *    18,386,276,900  12132   1,515,519      4 4,596,569,225 Aligned Dec
 *    47,406,850,800  12128   3,908,876  24791     1,912,260 Unary Chain
 *     8,073,187,900   6259   1,289,852   1279     6,312,109 Nishio Chain
 *    15,813,062,100   5241   3,017,184  10545     1,499,579 Multiple Chain
 *    16,730,942,100   1581  10,582,506   9957     1,680,319 Dynamic Chain
 *       151,395,500      3  50,465,166     30     5,046,516 Dynamic Plus
 * 2,596,311,884,100
 * pzls         total (ns)  (mm:ss)       each (ns)
 * 1465  2,636,797,633,500  (43:56)  1,799,861,865
 * NOTES:
 * 1. Forget the time saved for now, because we're missing hints. I suspect all
 *    caused by an A4E_1C bug, so I ripped isAnyAllowed* out of A4E_1C.
 * 2. Next up is A8E_1C, also needs everything since the COLLISSION_COMPARATOR.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-11 20:39</b> Promulgated isAnyAllowed* to A8E_1C.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        25,784,500 118368         217 662830            38 Naked Single
 *        21,664,000  52085         415 162520           133 Hidden Single
 *        80,647,100  35833       2,250  26359         3,059 Point and Claim
 *       140,552,600  23505       5,979   6497        21,633 Naked Pair
 *        97,064,200  21364       4,543   8477        11,450 Hidden Pair
 *       150,792,100  19265       7,827   1581        95,377 Naked Triple
 *       133,990,700  18869       7,101   1036       129,334 Hidden Triple
 *       109,005,000  18669       5,838    732       148,913 XY-Wing
 *        65,278,800  18148       3,597    686        95,158 Swampfish
 *        75,504,400  17879       4,223    311       242,779 Swordfish
 *       204,854,700  17793      11,513    128     1,600,427 Naked Quad
 *        21,259,400  17770       1,196     10     2,125,940 Jellyfish
 *        80,309,300  17767       4,520    332       241,895 XYZ-Wing
 *     2,056,728,300  17457     117,816   1130     1,820,113 Unique Rectangle
 *       255,483,700  16921      15,098     62     4,120,704 Bi-Uni Grave
 *     1,262,452,700  16891      74,741     23    54,889,247 Aligned Pair
 *    19,315,280,000  16868   1,145,084    592    32,627,162 Aligned Triple
 *   123,229,461,100  16331   7,545,738   1407    87,583,128 Aligned Quad
 *   289,677,792,300  15078  19,211,950   1334   217,149,769 Aligned Pent
 *   722,273,219,900  13912  51,917,281   1122   643,737,272 Aligned Hex
 * 1,222,106,021,000  12952  94,356,548    662 1,846,081,602 Aligned Sept
 * 3,313,434,278,400  12375 267,752,264    464 7,141,022,151 Aligned Oct
 *    52,029,349,800  11969   4,347,008     22 2,364,970,445 Aligned Nona
 *    13,076,967,800  11948   1,094,490      3 4,358,989,266 Aligned Dec
 *    46,397,389,000  11945   3,884,251  24680     1,879,959 Unary Chain
 *     7,658,040,300   6144   1,246,425   1262     6,068,177 Nishio Chain
 *    14,843,453,200   5134   2,891,206  10315     1,439,016 Multiple Chain
 *    15,798,285,900   1560  10,127,106   9801     1,611,905 Dynamic Chain
 *       225,078,600      3  75,026,200     30     7,502,620 Dynamic Plus
 * 5,844,845,988,800
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  5,880,779,287,000  (98:00)  4,014,183,813
 * NOTES:
 * 1. From 247:15 (20-01-17) down to 98:00 = 2 hours and 56 minutes faster with
 *    Aligned8Exclusion_1C, that's the complete and correct version.
 * 2. I'm not going to promulgate to A9E or A10E. There's NO chance of getting
 *    the correct versions down to a "reasonable" runtime, so why bother?
 * 3. So I think I'm done, finally. I can say quite honestly that I don't know
 *    how to do it faster, in java, though I suspect I could do it faster in C#,
 *    because there it has no slacker JIT-compiler to contend with, and it lets
 *    you put structs on the stack, which would expedite chaining dramatically.
 * 4. So I'm doing my final release DiufSudoku_V6_30.048.2020-02-11.OK.7z
 *    6.30.048 2020-02-11 20:39:48 isAnyAllowed* in A8E_2H and A8E_1C, and
 *    that's as far as I'm going to push it, so this'll be my last release.
 *    It's been, umm, not fun exactly, more engaging.
 *    top1465.d5.mt with A2345678E_1C + A910E_2H in 98 minutes flat.
 * 5. Now I shall have to work-out how/where to release it.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-12 09:50</b> A8E_1C uncomment is1AnyAllowed4567 is nearly 10
 * minutes faster, though one suspects this may not be stable!
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        27,341,500 118368         230 662830            41 Naked Single
 *        18,808,200  52085         361 162520           115 Hidden Single
 *        82,663,200  35833       2,306  26359         3,136 Point and Claim
 *       147,711,300  23505       6,284   6497        22,735 Naked Pair
 *        99,601,900  21364       4,662   8477        11,749 Hidden Pair
 *       150,696,200  19265       7,822   1581        95,317 Naked Triple
 *       129,644,400  18869       6,870   1036       125,139 Hidden Triple
 *       108,171,200  18669       5,794    732       147,774 XY-Wing
 *        66,633,300  18148       3,671    686        97,133 Swampfish
 *        72,307,800  17879       4,044    311       232,500 Swordfish
 *       205,324,600  17793      11,539    128     1,604,098 Naked Quad
 *        21,815,400  17770       1,227     10     2,181,540 Jellyfish
 *        81,606,200  17767       4,593    332       245,801 XYZ-Wing
 *     2,091,393,600  17457     119,802   1130     1,850,790 Unique Rectangle
 *       269,828,900  16921      15,946     62     4,352,079 Bi-Uni Grave
 *     1,280,312,000  16891      75,798     23    55,665,739 Aligned Pair
 *    19,435,022,300  16868   1,152,182    592    32,829,429 Aligned Triple
 *   122,073,619,700  16331   7,474,962   1407    86,761,634 Aligned Quad
 *   302,064,123,100  15078  20,033,434   1334   226,434,874 Aligned Pent
 *   738,354,209,700  13912  53,073,189   1122   658,069,705 Aligned Hex
 * 1,270,597,819,200  12952  98,100,511    662 1,919,332,053 Aligned Sept
 * 2,656,527,715,200  12375 214,668,906    464 5,725,275,248 Aligned Oct
 *    52,193,094,200  11969   4,360,689     22 2,372,413,372 Aligned Nona
 *    13,320,209,000  11948   1,114,848      3 4,440,069,666 Aligned Dec
 *    47,055,944,000  11945   3,939,384  24680     1,906,642 Unary Chain
 *     7,866,009,700   6144   1,280,275   1262     6,232,971 Nishio Chain
 *    15,662,824,000   5134   3,050,803  10315     1,518,451 Multiple Chain
 *    16,486,787,400   1560  10,568,453   9801     1,682,153 Dynamic Chain
 *       150,591,500      3  50,197,166     30     5,019,716 Dynamic Plus
 * 5,266,641,828,700
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  5,301,953,134,500  (88:21)  3,619,080,637
 * NOTES:
 * 1. A8E_1C uncomment is1AnyAllowed4567
 *    top1465.d5.mt with A2345678E_1C + A910E_2H in 88 minutes 21 seconds.
 *    which is nearly 10 minutes faster than previous.
 * 2. RERELEASE 6.30.048 2020-02-12 09:29:48
 *    =&gt; DiufSudoku_V6_30.048.2020-02-12.OK.7z
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-13 13:18</b> I'm playing with an "automatic" Filter class in
 * A8E_1C. This is the lead-up run, which sets the filters. Now I shall run it
 * again, to apply the filters.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        27,341,500 118368         230 662830            41 Naked Single
 *        18,808,200  52085         361 162520           115 Hidden Single
 *        82,663,200  35833       2,306  26359         3,136 Point and Claim
 *       147,711,300  23505       6,284   6497        22,735 Naked Pair
 *        99,601,900  21364       4,662   8477        11,749 Hidden Pair
 *       150,696,200  19265       7,822   1581        95,317 Naked Triple
 *       129,644,400  18869       6,870   1036       125,139 Hidden Triple
 *       108,171,200  18669       5,794    732       147,774 XY-Wing
 *        66,633,300  18148       3,671    686        97,133 Swampfish
 *        72,307,800  17879       4,044    311       232,500 Swordfish
 *       205,324,600  17793      11,539    128     1,604,098 Naked Quad
 *        21,815,400  17770       1,227     10     2,181,540 Jellyfish
 *        81,606,200  17767       4,593    332       245,801 XYZ-Wing
 *     2,091,393,600  17457     119,802   1130     1,850,790 Unique Rectangle
 *       269,828,900  16921      15,946     62     4,352,079 Bi-Uni Grave
 *     1,280,312,000  16891      75,798     23    55,665,739 Aligned Pair
 *    19,435,022,300  16868   1,152,182    592    32,829,429 Aligned Triple
 *   122,073,619,700  16331   7,474,962   1407    86,761,634 Aligned Quad
 *   302,064,123,100  15078  20,033,434   1334   226,434,874 Aligned Pent
 *   738,354,209,700  13912  53,073,189   1122   658,069,705 Aligned Hex
 * 1,270,597,819,200  12952  98,100,511    662 1,919,332,053 Aligned Sept
 * 2,656,527,715,200  12375 214,668,906    464 5,725,275,248 Aligned Oct
 *    52,193,094,200  11969   4,360,689     22 2,372,413,372 Aligned Nona
 *    13,320,209,000  11948   1,114,848      3 4,440,069,666 Aligned Dec
 *    47,055,944,000  11945   3,939,384  24680     1,906,642 Unary Chain
 *     7,866,009,700   6144   1,280,275   1262     6,232,971 Nishio Chain
 *    15,662,824,000   5134   3,050,803  10315     1,518,451 Multiple Chain
 *    16,486,787,400   1560  10,568,453   9801     1,682,153 Dynamic Chain
 *       150,591,500      3  50,197,166     30     5,019,716 Dynamic Plus
 * 5,266,641,828,700
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  5,301,953,134,500  (88:21)  3,619,080,637
 * NOTES:
 * 1. A8E_1C uncomment is1AnyAllowed4567
 *    top1465.d5.mt with A2345678E_1C + A910E_2H in 88 minutes 21 seconds.
 *    which is nearly 10 minutes faster than previous.
 * 2. RERELEASE 6.30.048 2020-02-12 09:29:48
 *    =&gt; DiufSudoku_V6_30.048.2020-02-12.OK.7z
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-16 16:39</b> Here's what I guess is the final A8E_1C run.
 * I shall use the hacked version from now on. I've given up on trying to make
 * the correct version fast enough to be practicably usable. It's useless.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        23,336,760 118371         197 662920            35 Naked Single
 *        20,559,318  52079         394 162450           126 Hidden Single
 *        80,219,205  35834       2,238  26359         3,043 Point and Claim
 *       143,909,039  23506       6,122   6497        22,150 Naked Pair
 *        98,530,726  21365       4,611   8477        11,623 Hidden Pair
 *       155,763,106  19266       8,084   1581        98,521 Naked Triple
 *       131,654,292  18870       6,976   1036       127,079 Hidden Triple
 *       109,342,714  18670       5,856    731       149,579 XY-Wing
 *        65,851,821  18150       3,628    686        95,993 Swampfish
 *        76,087,178  17881       4,255    311       244,653 Swordfish
 *       216,329,096  17795      12,156    128     1,690,071 Naked Quad
 *        23,525,705  17772       1,323     10     2,352,570 Jellyfish
 *        82,374,305  17769       4,635    332       248,115 XYZ-Wing
 *     2,078,550,088  17459     119,053   1130     1,839,424 Unique Rectangle
 *       261,642,829  16923      15,460     62     4,220,045 Bi-Uni Grave
 *     1,307,139,328  16893      77,377     23    56,832,144 Aligned Pair
 *    19,751,894,295  16870   1,170,829    593    33,308,422 Aligned Triple
 *   127,142,021,411  16332   7,784,840   1408    90,299,731 Aligned Quad
 *   303,462,437,596  15078  20,126,173   1333   227,653,741 Aligned Pent
 *   759,842,472,557  13913  54,613,848   1122   677,221,455 Aligned Hex
 * 1,278,435,509,036  12953  98,698,024    662 1,931,171,463 Aligned Sept
 * 4,043,835,639,029  12376 326,748,193    468 8,640,674,442 Aligned Oct
 *    53,694,015,729  11966   4,487,215     22 2,440,637,078 Aligned Nona
 *    14,405,200,561  11945   1,205,960      3 4,801,733,520 Aligned Dec
 *    48,281,541,671  11942   4,043,002  24660     1,957,888 Unary Chain
 *     8,207,577,020   6143   1,336,086   1262     6,503,626 Nishio Chain
 *    15,779,230,745   5133   3,074,075  10314     1,529,884 Multiple Chain
 *    16,798,556,036   1560  10,768,305   9801     1,713,963 Dynamic Chain
 *       161,245,100      3  53,748,366     30     5,374,836 Dynamic Plus
 * 6,694,672,156,296
 * pzls         total (ns)  (mmm:ss)      each (ns)
 * 1465  6,710,950,617,300  (111:50)  4,580,853,663
 * NOTES:
 * 1. A8E_1C commented is1AnyAllowed4567
 *    top1465.d5.mt with A2345678E_1C + A910E_2H in 112 minutes. It's not the
 *    fastest, but it is finding a few hints missed by the previous version.
 * 2. release 6.30.049 2020-02-16 16:39:49
 *    =&gt; DiufSudoku_V6_30.049.2020-02-16.OK.7z
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-19 21:32</b> Here's a log from this mornings run, which I've
 * been hacking the s__t-out of all day, and now it's taking like 12 hours.
 * So here's proof that it happened, even if it isn't reproducable.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A4E: correct
 * A5E: correct
 * A6E: correct
 * A7E: correct
 * A8E: correct
 * A9E: correct &lt;&lt;&lt;&lt;================== THE DOG ____ER
 * A10E: hacked
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair,
 * HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish,
 * NakedQuad, Jellyfish, XYZ_Wing, URT, BUG,
 * AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex,
 * AlignedSept, AlignedOct, AlignedNona, AlignedDec,
 * UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus,
 * NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.049 built 2020-02-16 16:39:49 ran 2020-02-19.07-33-31
 * mode    : ACCURACY !STATS !REDO HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-19.07-33-31.log
 * stdout  : progress only
 *
 *           time(ns) calls   time/call  elims      time/elim hinter
 *        23,193,600 118400         195 662910             34 Naked Single
 *        15,290,300  52109         293 162500             94 Hidden Single
 *        81,468,400  35859       2,271  26332          3,093 Point and Claim
 *       139,012,600  23542       5,904   6475         21,469 Naked Pair
 *       103,436,900  21407       4,831   8461         12,225 Hidden Pair
 *       149,869,300  19318       7,758   1581         94,793 Naked Triple
 *       128,891,400  18924       6,811   1035        124,532 Hidden Triple
 *       109,758,200  18725       5,861    728        150,766 XY-Wing
 *        69,116,500  18207       3,796    688        100,460 Swampfish
 *        71,358,100  17938       3,978    322        221,609 Swordfish
 *       206,517,700  17850      11,569    128      1,613,419 Naked Quad
 *        21,779,500  17827       1,221     10      2,177,950 Jellyfish
 *        82,305,500  17824       4,617    331        248,657 XYZ-Wing
 *     2,054,977,600  17515     117,326   1133      1,813,748 Unique Rectangle
 *       262,707,700  16980      15,471     62      4,237,220 Bi-Uni Grave
 *     1,286,757,900  16950      75,914     23     55,945,995 Aligned Pair
 *    19,587,105,200  16927   1,157,151    589     33,254,847 Aligned Triple
 *   126,624,940,400  16393   7,724,329   1397     90,640,615 Aligned Quad
 *   293,876,829,700  15148  19,400,371   1329    221,126,282 Aligned Pent
 *   736,203,949,300  13986  52,638,635   1118    658,500,849 Aligned Hex
 * 1,258,716,217,000  13030  96,601,398    654  1,924,642,533 Aligned Sept
 * 3,914,624,937,300  12458 314,225,793    436  8,978,497,562 Aligned Oct
 * 2,718,059,653,200  12076 225,079,467    261 10,414,021,659 Aligned Nona
 *    13,200,745,600  11845   1,114,457      3  4,400,248,533 Aligned Dec
 *    46,986,144,100  11842   3,967,754  24615      1,908,841 Unary Chain
 *     7,778,412,600   6087   1,277,872   1255      6,197,938 Nishio Chain
 *    14,958,181,500   5084   2,942,207  10290      1,453,661 Multiple Chain
 *    15,946,141,200   1553  10,267,959   9731      1,638,695 Dynamic Chain
 *       161,184,700      3  53,728,233     30      5,372,823 Dynamic Plus
 * 9,171,530,883,000
 *
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 9,187,141,274,800 (153:07)     6,271,086,194
 *
 * Aligned9Exclusion currently: correct
 * // A9ECC_maxsc min=23/17 max=124/155 pass 483,837,685 of 484,371,598 skip 533,913 = 0.11%
 * // A9ECC_minsc min=8/2 max=43/66 pass 381,096,040 of 483,837,685 skip 102,741,645 = 21.23%
 * // A9ECC_ttlsc min=133/129 max=756/778 pass 381,083,597 of 381,096,040 skip 12,443 = 0.00%
 * // 1 = 379,821,062 of 381,083,597 = 99.67%
 * // A9E_mbs1 min=21/21 max=40/40 pass 379,821,062 of 379,821,062 skip 0 = 0.00%
 * // A9E_col1 min=13/13 max=84/84 pass 379,821,062 of 379,821,062 skip 0 = 0.00%
 * // A9E_ces1 min=2/2 max=6/8 pass 375,664,917 of 379,821,062 skip 4,156,145 = 1.09%
 * // 2 = 1,256,794 of 381,083,597 = 0.33%
 * // A9E_mbs2 min=25/21 max=37/40 pass 1,123,153 of 1,256,794 skip 133,641 = 10.63%
 * // A9E_col2 min=18/13 max=57/67 pass 1,076,123 of 1,123,153 skip 47,030 = 4.19%
 * // A9E_sib2 min=16/16 max=21/24 pass 1,045,323 of 1,076,123 skip 30,800 = 2.86%
 * // A9E_ces2 min=5/4 max=10/10 pass 1,033,804 of 1,045,323 skip 11,519 = 1.10%
 * // 3 = 5,741 of 381,083,597 = 0.00%
 * // A9E_mbs3 min=28/24 max=28/40 pass 344 of 5,741 skip 5,397 = 94.01%
 * // A9E_col3 min=18/13 max=18/26 pass 38 of 344 skip 306 = 88.95%
 * // A9E_sib3 min=16/16 max=16/18 pass 35 of 38 skip 3 = 7.89%
 * // A9E_ces3 min=9/7 max=9/10 pass 14 of 35 skip 21 = 60.00%
 *
 * NOTES:
 * 1. 2.5 hours with correct A9E is ok, but I can do better.
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-21 16:52</b> I used an IntegerSet to only apply all of the
 * correct Aligned*Exclusion_1C hinters to puzzles on which they produce a hint
 * in order to apply "all correct" in a reasonable time-frame. So I'm done now.
 * The challenge was to solve these 1465 puzzles "as simply as possible" in
 * under and hour, and I've sort-of done that. I'm relying on knowledge gained
 * from a previous run, which is cheating. I'd need a radically faster aligned
 * set exclusion algorithm to do better: a fast way to filter-out aligned-sets
 * which can't produce a hint, and I'm not bright enough to dream it up; so I
 * gave up and cheated.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A4E: correct 609
 * A5E: correct 608
 * A6E: correct 507
 * A7E: correct 395
 * A8E: correct 264
 * A9E: correct 172
 * A10E: correct 104
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.049 built 2020-02-16 16:39:49 ran 2020-02-21.15-57-17
 * mode    : ACCURACY !STATS !REDO HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-21.15-57-17.log
 * stdout  : progress only
 *
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        21,411,700 118476        180 662940            32 Naked Single
 *        19,722,400  52182        377 162540           121 Hidden Single
 *        74,759,200  35928      2,080  26323         2,840 Point and Claim
 *       127,419,200  23610      5,396   6483        19,654 Naked Pair
 *        93,750,300  21468      4,366   8463        11,077 Hidden Pair
 *       145,413,800  19378      7,504   1578        92,150 Naked Triple
 *       125,766,100  18983      6,625   1025       122,698 Hidden Triple
 *       108,806,100  18786      5,791    728       149,458 XY-Wing
 *        63,462,500  18268      3,473    690        91,974 Swampfish
 *        70,143,100  17997      3,897    326       215,162 Swordfish
 *       202,465,100  17907     11,306    128     1,581,758 Naked Quad
 *        20,778,000  17884      1,161     10     2,077,800 Jellyfish
 *        79,450,900  17881      4,443    328       242,228 XYZ-Wing
 *     1,923,033,400  17575    109,418   1136     1,692,811 Unique Rectangle
 *       242,138,200  17038     14,211     62     3,905,454 Bi-Uni Grave
 *        34,141,200  17008      2,007     21     1,625,771 Aligned Pair
 *     6,638,517,800  16987    390,799    590    11,251,725 Aligned Triple
 *    77,944,343,800  16453  4,737,394   1393    55,954,302 Aligned Quad
 *   168,831,552,700  15213 11,097,847   1325   127,420,039 Aligned Pent
 *   413,660,028,100  14053 29,435,709   1116   370,663,107 Aligned Hex
 *   584,494,789,800  13097 44,628,143    653   895,091,561 Aligned Sept
 *   663,961,310,000  12526 53,006,650    432 1,536,947,476 Aligned Oct
 *   728,116,347,100  12146 59,947,007    257 2,833,137,537 Aligned Nona
 *   404,126,395,200  11920 33,903,221    158 2,557,761,994 Aligned Dec
 *    44,897,817,800  11788  3,808,773  24589     1,825,931 Unary Chain
 *     7,617,907,200   6059  1,257,287   1252     6,084,590 Nishio Chain
 *    14,605,052,200   5059  2,886,944  10241     1,426,135 Multiple Chain
 *    15,712,937,000   1541 10,196,584   9683     1,622,734 Dynamic Chain
 *       160,725,200      3 53,575,066     30     5,357,506 Dynamic Plus
 * 3,134,120,385,100
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  3,148,613,523,800  (52:28)  2,149,224,248
 * NOTES:
 * 1. Under an hour with all A*E's correct, but I'm cheating!
 * 2. I'll do my final "correct" release now, and then it's back to work on the
 *    hacked version of A7+E. I guess I need to workout how to make each A*E
 *    remove the HIT_SET element for those puzzle upon which it does not hint.
 *    That's tricky coz each getHints call can't know if the next call will
 *    hint or not, so it'll have to be done either in the LogicalSolver or the
 *    LogicalSolverTester, which "breaks containment". Hmmm...
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-22 11:51</b> Adjusted A*E_2H filters to suit the gsl filter.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: hacked 325
 * A6E: hacked 222
 * A7E: hacked 128
 * A8E: hacked 57
 * A9E: hacked 23
 * A10E: hacked 3 ============================================================== WANT 3
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.050 built 2020-02-21 19:35:50 ran 2020-02-22.11-45-22
 * mode    : ACCURACY !STATS !REDO HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-22.11-45-22.log
 * stdout  : progress only
 *
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      23,606,000 117534        200 660780          35 Naked Single
 *      17,130,500  51456        332 161600         106 Hidden Single
 *      75,288,800  35296      2,133  26413       2,850 Point and Claim
 *     144,131,900  22975      6,273   6616      21,785 Naked Pair
 *      95,534,300  20798      4,593   8475      11,272 Hidden Pair
 *     153,419,400  18696      8,206   1610      95,291 Naked Triple
 *     126,936,500  18293      6,939   1101     115,292 Hidden Triple
 *     109,259,000  18077      6,044    745     146,656 XY-Wing
 *      67,887,300  17553      3,867    664     102,239 Swampfish
 *      75,801,100  17292      4,383    322     235,407 Swordfish
 *     211,435,400  17200     12,292    129   1,639,034 Naked Quad
 *      21,289,800  17178      1,239     12   1,774,150 Jellyfish
 *      82,596,500  17174      4,809    349     236,666 XYZ-Wing
 *   2,033,483,200  16848    120,695   1120   1,815,610 Unique Rectangle
 *     253,171,000  16316     15,516     66   3,835,924 Bi-Uni Grave
 *      32,686,300  16283      2,007     21   1,556,490 Aligned Pair
 *   6,890,573,600  16262    423,722    570  12,088,725 Aligned Triple
 *  80,339,418,900  15749  5,101,239   1450  55,406,495 Aligned Quad
 *  18,239,508,700  14461  1,261,289    568  32,111,811 Aligned Pent
 *  18,308,692,200  13951  1,312,356    382  47,928,513 Aligned Hex
 *  13,440,991,200  13616    987,146    176  76,369,268 Aligned Sept
 *   6,975,775,600  13459    518,298     70  99,653,937 Aligned Oct
 *   2,975,436,500  13396    222,113     24 123,976,520 Aligned Nona
 *      96,790,000  13373      7,237      2  48,395,000 Aligned Dec ============ GOT 2
 *  53,654,734,700  13371  4,012,769  26641   2,013,991 Unary Chain
 *   9,305,042,800   6895  1,349,534   1352   6,882,428 Nishio Chain
 *  18,712,355,600   5831  3,209,116  11766   1,590,375 Multiple Chain
 *  18,674,352,800   1652 11,304,087  10442   1,788,388 Dynamic Chain
 *     154,943,400      3 51,647,800     30   5,164,780 Dynamic Plus
 * 251,292,273,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  266,770,962,700 (04:26)  182,096,220
 * Q: Why 2 not 3 A10E hints? Which hint is missing and why? Is it broken?
 *    Keeper log says the missing hint is 298/26, ie this one:
 *    ..3..29619..3.1...1.2659.8.........2..5..3.98...4..1...97.....5.28.6...94.19.....
 *    578,4578,,78,478,,,,,,4678,46,,478,,25,25,47,,47,,,,,37,,347,378,1378,469,1578,1789,5678,34567,3457,,267,1467,,127,127,,46,,,2378,378,69,,2789,5678,,357,367,36,,,128,1238,48,23468,1234,,35,,,157,,457,347,1347,,,356,,,2378,578,23678,237,367
 *    298/26   	    494,026,900	31	 158	  1	Aligned Dec                   	Aligned Dec: A4, D4, E4, F4, G4, H4, A5, A6, B6, C6 (E4-1)
 * A: The whole puzzle solves totally differently using all-hacked A*E's so
 *    we don't get to the puzzle that produces this hint. When you paste the
 *    puzzle into the GUI it is found (without filtering), so not broken.
 * NOTES:
 * 1. I have no idea if that's the fastest it's ever been with all A*E's hacked
 *    but I reckon 4:26 is "acceptable", so I'm shipping it.
 * 2. Release 6.30.051 2020-02-22 11:59:51
 *    => DiufSudoku_V6_30.051.2020-02-22.OK.7z
 * </pre>
 * <hr><p>
 * <b>KRC 2020-02-25 18:10:52</b> Promulgated anyLevel and filters to A4E.
 * Changed NakedSet, HiddenSet and everything that uses Values and Indexes by
 * using the ARRAY through-out and a new NUM_TRAILING_ZEROS instead of calling
 * Integer.numberOfTrailingZeros everywhere. Cleaned-up inline otherCell in
 * ChainerBase offToOns. First I ran all hacked with IS_HACKY=false.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: hacked &lt;&lt;=============================================================== ALL HACKED
 * A6E: hacked
 * A7E: hacked
 * A8E: hacked
 * A9E: hacked
 * A10E: hacked
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.052 built 2020-02-25 18:10:52 ran 2020-02-25.19-19-02
 * mode    : ACCURACY !STATS !REDO !HACKY &lt;&lt;==================================== !HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-25.19-19-02.log
 * stdout  : progress only
 *
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      34,019,900 117578        289 660850            51 Naked Single
 *      21,489,200  51493        417 161710           132 Hidden Single
 *      88,399,400  35322      2,502  26407         3,347 Point and Claim
 *     179,642,200  22999      7,810   6592        27,251 Naked Pair
 *     116,191,100  20829      5,578   8477        13,706 Hidden Pair
 *     178,242,400  18723      9,519   1611       110,640 Naked Triple
 *     146,887,400  18319      8,018   1101       133,412 Hidden Triple
 *     120,804,200  18103      6,673    739       163,469 XY-Wing
 *      80,612,400  17581      4,585    661       121,955 Swampfish
 *      87,173,200  17321      5,032    322       270,724 Swordfish
 *     248,615,700  17230     14,429    129     1,927,253 Naked Quad
 *      27,032,100  17208      1,570     12     2,252,675 Jellyfish
 *      90,038,800  17204      5,233    347       259,477 XYZ-Wing
 *   2,816,369,100  16880    166,846   1118     2,519,113 Unique Rectangle
 *     295,263,200  16349     18,060     68     4,342,105 Bi-Uni Grave
 *   1,985,220,600  16315    121,680     27    73,526,688 Aligned Pair
 *  24,188,335,000  16288  1,485,040    609    39,718,119 Aligned Triple
 *  88,839,517,800  15740  5,644,187   1519    58,485,528 Aligned Quad
 *  63,180,484,200  14389  4,390,887    517   122,205,965 Aligned Pent
 *  86,587,644,600  13921  6,219,929    378   229,067,842 Aligned Hex
 *  99,204,962,800  13588  7,300,924    178   557,331,251 Aligned Sept
 *  89,923,608,800  13429  6,696,225     73 1,231,830,257 Aligned Oct
 *  57,071,540,200  13363  4,270,862     21 2,717,692,390 Aligned Nona
 *  52,521,755,500  13343  3,936,277      6 8,753,625,916 Aligned Dec
 *  64,586,765,400  13337  4,842,675  26518     2,435,582 Unary Chain
 *  10,017,645,000   6883  1,455,418   1350     7,420,477 Nishio Chain
 *  20,558,555,800   5821  3,531,791  11693     1,758,193 Multiple Chain
 *  18,968,315,500   1652 11,482,031  10442     1,816,540 Dynamic Chain
 *     160,320,800      3 53,440,266     30     5,344,026 Dynamic Plus
 * 682,325,452,300
 *  pzls       total (ns)  (mm:ss)    each (ns)
 *  1465  706,861,695,200  (11:46)  482,499,450
 * NOTES:
 * 1. top1465.d5.mt with A234E_1C &amp; A5678910_2H !IS_HACKY in 11:46. Not bad.
 * 2. Release 6.30.052 2020-02-25 18:10:52
 *    => DiufSudoku_V6_30.052.2020-02-25.OK.7z
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-02-25 21:09:52</b> Ran IS_HACKY A2345678910_1C to compare times.
 * Rerun of Release 6.30.052 2020-02-25 18:10:52 (just set IS_HACKY=true).
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: correct 608
 * A6E: correct 507
 * A7E: correct 395
 * A8E: correct 264
 * A9E: correct 172
 * A10E: correct 104
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.052 built 2020-02-25 18:10:52 ran 2020-02-25.20-12-57
 * mode    : ACCURACY !STATS !REDO HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-25.20-12-57.log
 * stdout  : progress only
 *
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        22,284,600 118476        188 662940            33 Naked Single
 *        20,401,200  52182        390 162540           125 Hidden Single
 *        76,038,800  35928      2,116  26323         2,888 Point and Claim
 *       134,132,200  23610      5,681   6483        20,689 Naked Pair
 *        93,503,000  21468      4,355   8463        11,048 Hidden Pair
 *       139,396,500  19378      7,193   1578        88,337 Naked Triple
 *       119,678,100  18983      6,304   1025       116,759 Hidden Triple
 *       104,708,300  18786      5,573    728       143,830 XY-Wing
 *        68,360,300  18268      3,742    690        99,072 Swampfish
 *        72,796,800  17997      4,044    326       223,303 Swordfish
 *       190,107,400  17907     10,616    128     1,485,214 Naked Quad
 *        21,837,400  17884      1,221     10     2,183,740 Jellyfish
 *        79,833,200  17881      4,464    328       243,393 XYZ-Wing
 *     2,051,215,500  17575    116,712   1136     1,805,647 Unique Rectangle
 *       240,992,500  17038     14,144     62     3,886,975 Bi-Uni Grave
 *        34,233,400  17008      2,012     21     1,630,161 Aligned Pair
 *     6,741,334,900  16987    396,852    590    11,425,991 Aligned Triple
 *    31,726,955,300  16453  1,928,338   1393    22,775,990 Aligned Quad
 *   165,882,185,100  15213 10,903,975   1325   125,194,101 Aligned Pent
 *   413,190,180,900  14053 29,402,275   1116   370,242,097 Aligned Hex
 *   573,728,336,400  13097 43,806,088    653   878,603,884 Aligned Sept
 *   655,449,293,000  12526 52,327,103    432 1,517,243,733 Aligned Oct
 *   707,150,671,900  12146 58,220,868    257 2,751,559,034 Aligned Nona
 *   393,987,995,400  11920 33,052,684    158 2,493,594,907 Aligned Dec
 *    43,536,425,600  11788  3,693,283  24589     1,770,565 Unary Chain
 *     7,416,531,700   6059  1,224,052   1252     5,923,747 Nishio Chain
 *    14,325,610,600   5059  2,831,707  10241     1,398,848 Multiple Chain
 *    15,049,640,700   1541  9,766,152   9683     1,554,233 Dynamic Chain
 *       140,693,500      3 46,897,833     30     4,689,783 Dynamic Plus
 * 3,031,795,374,200
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  3,046,435,475,600  (50:46)  2,079,478,140
 * NOTES:
 * 1. So !IS_HACKY top1465.d5.mt with A234E_1C &amp; A5678910_2H in 11:46
 *    and IS_HACKY top1465.d5.mt with A2345678910E_1C        in 50:46.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-02-26 morning</b> Ran !IS_HACKY A2345678910_1C just to see how
 * long it takes. Something like 10 or 11 hours is my guess.
 * <hr>
 * <p>
 * <b>KRC 2020-02-26 13:26</b> A9E_1C is running like a dog for some reason:
 * <p>
 * So A9E_1C has taken 19.8 times longer than A8E_1C, and that's just wrong!
 * I still haven't decided wether or not I'm going to kill the run, but I'm
 * leaning towards the affirmative. I'm just going to take a look at the code
 * to see if I've left a breakpoint or some other stupidity in there.
 * <pre>
 * As at puzzle 60 where I killed it:
 *    748,603,735,600  1454     514,858,140  35   21,388,678,160  Aligned Oct
 * 14,824,806,195,900  1421  10,432,657,421  27  549,066,896,144  Aligned Nona = TOO SLOW
 *  1,700,135,241,500  1399   1,215,250,351  11  154,557,749,227  Aligned Dec
 * Fixed! Though I know not how (asat 60 again):
 *    810,437,885,900  1634     495,984,018  39   20,780,458,612  Aligned Oct
 *  1,183,192,838,900  1597     740,884,683  30   39,439,761,296  Aligned Nona = OK
 *  2,121,766,644,500  1573   1,348,866,271  13  163,212,818,807  Aligned Dec == SIGH!
 *
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: correct
 * A6E: correct
 * A7E: correct
 * A8E: correct
 * A9E: correct
 * A10E: correct
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.052 built 2020-02-25 18:10:52 ran 2020-02-26.15-14-15
 * mode    : ACCURACY !STATS !REDO !HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-26.15-14-15.log
 * stdout  : progress only
 *
 *           time(ns)  calls   time/call  elims      time/elim hinter
 *         22,376,000 118471         188 662950             33 Naked Single
 *         16,007,700  52176         306 162540             98 Hidden Single
 *         76,128,200  35922       2,119  26317          2,892 Point and Claim
 *        137,872,300  23606       5,840   6481         21,273 Naked Pair
 *         92,658,900  21466       4,316   8455         10,959 Hidden Pair
 *        142,894,000  19378       7,374   1576         90,668 Naked Triple
 *        123,120,400  18983       6,485   1025        120,117 Hidden Triple
 *        105,262,800  18786       5,603    727        144,790 XY-Wing
 *         68,312,200  18269       3,739    690         99,003 Swampfish
 *         73,612,900  17998       4,090    326        225,806 Swordfish
 *        194,221,700  17908      10,845    128      1,517,357 Naked Quad
 *         22,647,300  17885       1,266     10      2,264,730 Jellyfish
 *         80,299,600  17882       4,490    327        245,564 XYZ-Wing
 *      2,056,392,400  17577     116,993   1136      1,810,204 Unique Rectangle
 *        263,289,800  17040      15,451     62      4,246,609 Bi-Uni Grave
 *      1,651,610,100  17010      97,096     21     78,648,100 Aligned Pair
 *     20,385,340,300  16989   1,199,914    585     34,846,735 Aligned Triple
 *     72,286,333,400  16460   4,391,636   1393     51,892,558 Aligned Quad
 *    385,507,836,300  15220  25,329,029   1366    282,216,571 Aligned Pent
 *    999,047,222,100  14019  71,263,800   1083    922,481,276 Aligned Hex
 *  2,130,794,218,900  13096 162,705,728    658  3,238,289,086 Aligned Sept
 *  3,508,647,492,100  12519 280,265,795    442  7,938,116,497 Aligned Oct
 *  4,872,153,953,300  12129 401,694,612    257 18,957,797,483 Aligned Nona
 *  7,038,781,011,600  11904 591,295,447    144 48,880,423,691 Aligned Dec
 *     44,286,777,400  11783   3,758,531  24589      1,801,080 Unary Chain
 *      7,354,052,700   6054   1,214,742   1251      5,878,539 Nishio Chain
 *     14,434,518,000   5055   2,855,493  10228      1,411,274 Multiple Chain
 *     15,126,244,600   1541   9,815,862   9683      1,562,144 Dynamic Chain
 *        151,234,300      3  50,411,433     30      5,041,143 Dynamic Plus
 * 19,114,082,941,300
 *  pzls          total (ns)  (mmm:ss)       each (ns)
 *  1465  19,129,274,972,000  (318:49)  13,057,525,578
 * NOTES:
 * 1. So !IS_HACKY top1465.d5.mt with A234E_1C + A5678910_2H in  11:46
 *    or  IS_HACKY top1465.d5.mt with A2345678910E_1C        in  50:46
 *    or !IS_HACKY top1465.d5.mt with A2345678910E_1C        in 318:49 (5:18:49)
 * 2. That'll do pig. Coz it'll bloody well have to.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-02-28 07:33</b> Made some performance improvements to Hidden Set
 * and ran it again overnight to see how it goes.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: correct
 * A6E: correct
 * A7E: correct
 * A8E: correct
 * A9E: correct
 * A10E: correct
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.052 built 2020-02-25 18:10:52 ran 2020-02-28.00-01-21
 * mode    : ACCURACY !STATS !REDO !HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-28.00-01-21.log
 * stdout  : progress only
 *
 *           time(ns)  calls   time/call  elims      time/elim hinter
 *         21,873,700 118471         184 662950             32 Naked Single
 *         20,669,000  52176         396 162540            127 Hidden Single
 *         72,714,600  35922       2,024  26317          2,763 Point and Claim
 *        134,212,700  23606       5,685   6481         20,708 Naked Pair
 *         87,657,500  21466       4,083   8455         10,367 Hidden Pair
 *        140,911,000  19378       7,271   1576         89,410 Naked Triple
 *        119,387,900  18983       6,289   1025        116,476 Hidden Triple
 *        103,525,500  18786       5,510    727        142,400 XY-Wing
 *         66,248,000  18269       3,626    690         96,011 Swampfish
 *         71,458,200  17998       3,970    326        219,196 Swordfish
 *        192,036,500  17908      10,723    128      1,500,285 Naked Quad
 *         21,283,000  17885       1,189     10      2,128,300 Jellyfish
 *         78,273,400  17882       4,377    327        239,368 XYZ-Wing
 *      2,024,953,300  17577     115,204   1136      1,782,529 Unique Rectangle
 *        246,671,200  17040      14,476     62      3,978,567 Bi-Uni Grave
 *      1,623,770,400  17010      95,459     21     77,322,400 Aligned Pair
 *     19,979,498,800  16989   1,176,025    585     34,152,989 Aligned Triple
 *     70,891,158,100  16460   4,306,874   1393     50,890,996 Aligned Quad
 *    388,079,152,700  15220  25,497,973   1366    284,098,940 Aligned Pent
 *  1,018,288,385,400  14019  72,636,306   1083    940,247,816 Aligned Hex
 *  2,180,247,942,200  13096 166,481,974    658  3,313,446,720 Aligned Sept
 *  3,571,428,370,100  12519 285,280,643    442  8,080,154,683 Aligned Oct
 *  4,747,309,451,500  12129 391,401,554    257 18,472,021,212 Aligned Nona
 *  6,533,910,249,500  11904 548,883,589    144 45,374,376,732 Aligned Dec
 *     43,503,916,400  11783   3,692,091  24589      1,769,243 Unary Chain
 *      7,384,768,100   6054   1,219,816   1251      5,903,092 Nishio Chain
 *     14,083,215,400   5055   2,785,997  10228      1,376,927 Multiple Chain
 *     14,788,112,800   1541   9,596,439   9683      1,527,224 Dynamic Chain
 *        135,967,800      3  45,322,600     30      4,532,260 Dynamic Plus
 * 18,615,055,834,700
 * pzls          total (ns) (mmm:ss) (h:mm:ss)        each (ns)
 * 1465  18,637,189,690,500 (310:37) (5:10:37)  12,721,631,188
 * NOTES:
 * 1. So !IS_HACKY top1465.d5.mt with A2345678910E_1C in 310:37 or 5:10:37
 *    is 8 minutes faster than previous. No miracles here.
 * 2. Next we run IS_HACKY again with "HIT" puzzle numbers from lastnights run,
 *    then I guess we build, release, and backup.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-02-28 11:14</b> A log of a HACKY A2345678910E_1C run.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: correct 623
 * A6E: correct 497
 * A7E: correct 398
 * A8E: correct 270
 * A9E: correct 178
 * A10E: correct 104
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.052 built 2020-02-25 18:10:52 ran 2020-02-28.08-39-30
 * mode    : ACCURACY !STATS !REDO HACKY &lt;====================================== HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-28.08-39-30.log
 * stdout  : progress only
 *
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        24,174,300 118464        204 662890            36 Naked Single
 *        21,412,300  52175        410 162600           131 Hidden Single
 *        79,481,000  35915      2,213  26320         3,019 Point and Claim
 *       143,178,700  23598      6,067   6488        22,068 Naked Pair
 *        93,227,400  21455      4,345   8459        11,021 Hidden Pair
 *       148,775,700  19366      7,682   1576        94,400 Naked Triple
 *       122,882,700  18972      6,477   1025       119,885 Hidden Triple
 *       109,502,500  18775      5,832    728       150,415 XY-Wing
 *        79,891,100  18257      4,375    679       117,659 Swampfish
 *        74,968,400  17988      4,167    326       229,964 Swordfish
 *       204,319,600  17898     11,415    128     1,596,246 Naked Quad
 *        22,924,100  17875      1,282     10     2,292,410 Jellyfish
 *        83,912,800  17872      4,695    328       255,831 XYZ-Wing
 *     2,175,651,600  17566    123,855   1134     1,918,564 Unique Rectangle
 *       250,726,800  17030     14,722     62     4,043,980 Bi-Uni Grave
 *        34,799,200  17000      2,047     21     1,657,104 Aligned Pair
 *     6,978,747,800  16979    411,022    586    11,909,125 Aligned Triple
 *    33,256,156,800  16449  2,021,773   1398    23,788,381 Aligned Quad
 *   180,823,762,500  15206 11,891,606   1325   136,470,764 Aligned Pent
 *   420,914,152,400  14046 29,966,834   1105   380,917,784 Aligned Hex
 *   599,475,631,700  13101 45,758,005    657   912,443,883 Aligned Sept
 *   683,052,422,700  12526 54,530,769    431 1,584,808,405 Aligned Oct
 *   745,853,599,100  12147 61,402,288    252 2,959,736,504 Aligned Nona
 *   434,847,090,700  11926 36,462,107    158 2,752,196,776 Aligned Dec
 *    46,511,732,600  11794  3,943,677  24582     1,892,105 Unary Chain
 *     7,919,617,500   6063  1,306,220   1253     6,320,524 Nishio Chain
 *    15,464,352,800   5062  3,054,988  10244     1,509,601 Multiple Chain
 *    16,183,072,300   1541 10,501,669   9683     1,671,287 Dynamic Chain
 *       146,280,100      3 48,760,033     30     4,876,003 Dynamic Plus
 * 3,195,096,447,200
 * pzls         total (ns) (mm:ss)      each (ns)
 * 1465  3,210,470,664,400 (53:30)  2,191,447,552
 * NOTES:
 * 1. IS_HACKY top1465 A2345678910E_1C in 53:30 was 50:46 = 2:44 slower!
 * 2. Now I run it all hacked, and if slower I consider reverting.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-02-28 11:35:53</b> A HACKY A5678910E_2H run.
 * <pre>
 * LogicalSolver: unwanted: DirectNakedPair
 * LogicalSolver: unwanted: DirectHiddenPair
 * LogicalSolver: unwanted: DirectNakedTriple
 * LogicalSolver: unwanted: DirectHiddenTriple
 * LogicalSolver: unwanted: HiddenQuad
 * LogicalSolver: unwanted: NakedPent
 * LogicalSolver: unwanted: HiddenPent
 * A5E: hacked 325
 * A6E: hacked 222
 * A7E: hacked 128
 * A8E: hacked 57
 * A9E: hacked 23
 * A10E: hacked 96
 * LogicalSolver: unwanted: NestedMultiple
 * LogicalSolver: unwanted: NestedDynamic
 * wantedHinters: NakedSingle, HiddenSingle, Locking, NakedPair, HiddenPair, NakedTriple, HiddenTriple, XY_Wing, Swampfish, Swordfish, NakedQuad, Jellyfish, XYZ_Wing, URT, BUG, AlignedPair, AlignedTriple, AlignedQuad, AlignedPent, AlignedHex, AlignedSept, AlignedOct, AlignedNona, AlignedDec, UnaryChain, NishioChain, MultipleChain, DynamicChain, DynamicPlus, NestedUnary, NestedPlus
 * Sudoku Explainer 6.30.053 built 2020-02-28 11:25:53 ran 2020-02-28.11-31-58
 * mode    : ACCURACY !STATS !REDO HACKY
 * input   : C:/Users/User/Documents/NetBeansProjects/DiufSudoku/top1465.d5.mt
 * log 50  : c:/users/user/documents/netbeansprojects/diufsudoku/top1465.d5.2020-02-28.11-31-58.log
 * stdout  : progress only
 *
 *          time(ns)  calls  time/call  elims   time/elim hinter
 *        24,514,600 117533        208 660780          37 Naked Single
 *        18,853,800  51455        366 161600         116 Hidden Single
 *        74,383,600  35295      2,107  26413       2,816 Point and Claim
 *        98,643,900  22974      4,293   6614      14,914 Naked Pair
 *        91,381,400  20798      4,393   8475      10,782 Hidden Pair
 *       130,453,800  18696      6,977   1610      81,027 Naked Triple
 *       118,476,300  18293      6,476   1101     107,607 Hidden Triple
 *       108,125,000  18077      5,981    746     144,939 XY-Wing
 *        68,404,200  17553      3,897    664     103,018 Swampfish
 *        76,971,100  17292      4,451    322     239,040 Swordfish
 *       186,077,400  17200     10,818    129   1,442,460 Naked Quad
 *        21,800,300  17178      1,269     12   1,816,691 Jellyfish
 *        83,580,200  17174      4,866    349     239,484 XYZ-Wing
 *     2,137,634,100  16848    126,877   1120   1,908,601 Unique Rectangle
 *       259,256,000  16316     15,889     66   3,928,121 Bi-Uni Grave
 *        31,421,600  16283      1,929     21   1,496,266 Aligned Pair
 *     6,739,371,200  16262    414,424    567  11,886,016 Aligned Triple
 *    34,015,604,900  15752  2,159,446   1454  23,394,501 Aligned Quad
 *    17,155,266,300  14460  1,186,394    567  30,256,201 Aligned Pent
 *    20,001,682,900  13951  1,433,709    382  52,360,426 Aligned Hex
 *    13,883,285,400  13616  1,019,630    176  78,882,303 Aligned Sept
 *     4,785,064,000  13459    355,528     70  68,358,057 Aligned Oct
 *     2,059,703,100  13396    153,755     24  85,820,962 Aligned Nona
 *     1,896,148,000  13373    141,789      2 948,074,000 Aligned Dec
 *    53,299,283,300  13371  3,986,185  26641   2,000,648 Unary Chain
 *     9,093,316,300   6895  1,318,827   1352   6,725,825 Nishio Chain
 *    18,059,682,500   5831  3,097,184  11766   1,534,904 Multiple Chain
 *    17,753,118,700   1652 10,746,439  10442   1,700,164 Dynamic Chain
 *       162,412,900      3 54,137,633     30   5,413,763 Dynamic Plus
 *   202,433,916,800
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   218,357,239,100 (03:38)       149,049,309
 * NOTES:
 * 1. IS_HACKY top1465 A5678910E_2H in 3:38 was 11:46 = 8:08 faster.
 * 2. Now I release 6.30.053 2020-02-28 11:35:53
 *    => DiufSudoku_V6_30.053.2020-02-28.OK.7z
 *    and finally I think it's time DiufSudoku_V6 was released into the wild.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-05 21:48:54</b> Stuck a filter HashSet in A*E
 * <pre>
 *          time(ns)  calls  time/call  elims     time/elim hinter
 *        25,354,900 117476        215 660640            38 Naked Single
 *        16,591,800  51412        322 161510           102 Hidden Single
 *        71,901,300  35261      2,039  26419         2,721 Point and Claim
 *        96,132,000  22937      4,191   6618        14,525 Naked Pair
 *        92,212,900  20759      4,442   8479        10,875 Hidden Pair
 *       127,826,900  18654      6,852   1606        79,593 Naked Triple
 *       118,635,700  18252      6,499   1102       107,654 Hidden Triple
 *       105,905,400  18038      5,871    744       142,345 XY-Wing
 *        67,736,800  17516      3,867    662       102,321 Swampfish
 *        73,551,200  17257      4,262    322       228,419 Swordfish
 *       180,502,800  17165     10,515    129     1,399,246 Naked Quad
 *        21,841,500  17143      1,274     12     1,820,125 Jellyfish
 *        81,728,500  17139      4,768    353       231,525 XYZ-Wing
 *     2,067,002,100  16809    122,969   1120     1,845,537 Unique Rectangle
 *       244,473,600  16277     15,019     64     3,819,900 Bi-Uni Grave
 *        36,607,400  16245      2,253     20     1,830,370 Aligned Pair
 *     3,835,394,000  16225    236,387    569     6,740,586 Aligned Triple
 *    25,516,339,500  15714  1,623,796   1331    19,170,803 Aligned Quad
 *    12,488,026,200  14523    859,879    589    21,202,081 Aligned Pent
 *    11,557,798,100  13998    825,674    390    29,635,379 Aligned Hex
 *     6,028,426,600  13658    441,384    175    34,448,152 Aligned Sept
 *     3,693,417,800  13502    273,545     70    52,763,111 Aligned Oct
 *     1,607,671,400  13439    119,627     24    66,986,308 Aligned Nona
 *     2,094,804,300  13416    156,142      2 1,047,402,150 Aligned Dec
 *    52,971,971,300  13414  3,949,006  26870     1,971,416 Unary Chain
 *     9,070,485,200   6898  1,314,944   1352     6,708,938 Nishio Chain
 *    18,457,900,400   5834  3,163,849  11787     1,565,954 Multiple Chain
 *    18,119,106,100   1652 10,967,981  10442     1,735,214 Dynamic Chain
 *       164,398,900      3 54,799,633     30     5,479,963 Dynamic Plus
 *   169,033,744,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  184,364,591,200 (03:04)  125,846,137
 * NOTES:
 * 1. IS_HACKY top1465 A5678910E_1C in 3:03 was 53:30 = 50 minutes faster.
 * 2. Now I release 6.30.054 2020-03-05 21:46:54
 *    => DiufSudoku_V6_30.054.2020-03-05.OK.7z
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-06 21:59:55</b> Stuck a WindexSet in each A*E.
 * <pre>
 *          time(ns)  calls  time/call  elims   time/elim hinter
 *        22,951,900 118423        193 662880          34 Naked Single
 *        20,786,100  52135        398 162540         127 Hidden Single
 *        77,518,100  35881      2,160  26315       2,945 Point and Claim
 *        97,267,300  23566      4,127   6485      14,998 Naked Pair
 *        88,696,800  21426      4,139   8452      10,494 Hidden Pair
 *       129,274,200  19339      6,684   1572      82,235 Naked Triple
 *       119,873,600  18948      6,326   1025     116,949 Hidden Triple
 *       105,618,100  18751      5,632    727     145,279 XY-Wing
 *        69,591,800  18233      3,816    677     102,794 Swampfish
 *        72,936,000  17965      4,059    326     223,730 Swordfish
 *       184,754,600  17875     10,335    128   1,443,395 Naked Quad
 *        21,857,800  17852      1,224     10   2,185,780 Jellyfish
 *        83,646,200  17849      4,686    327     255,798 XYZ-Wing
 *     2,029,735,800  17544    115,694   1130   1,796,226 Unique Rectangle
 *       249,802,700  17010     14,685     62   4,029,075 Bi-Uni Grave
 *        39,561,700  16980      2,329     21   1,883,890 Aligned Pair
 *    32,297,584,300  16959  1,904,450    560  57,674,257 Aligned Triple
 *   496,548,800,600  16453 30,179,833   1350 367,813,926 Aligned Quad
 *   722,928,889,400  15251 47,402,064   1338 540,305,597 Aligned Pent
 *   485,811,887,200  14081 34,501,234   1035 469,383,465 Aligned Hex
 *   262,064,492,100  13205 19,845,853    637 411,404,226 Aligned Sept
 *   121,653,230,000  12649  9,617,616    485 250,831,402 Aligned Oct
 *    97,397,490,100  12227  7,965,771    289 337,015,536 Aligned Nona
 *    37,378,851,400  11974  3,121,667    212 176,315,336 Aligned Dec
 *    44,266,090,900  11802  3,750,727  24644   1,796,221 Unary Chain
 *     7,538,880,800   6064  1,243,219   1253   6,016,664 Nishio Chain
 *    14,656,600,300   5063  2,894,845  10254   1,429,354 Multiple Chain
 *    15,488,942,200   1541 10,051,227   9683   1,599,601 Dynamic Chain
 *       149,875,800      3 49,958,600     30   4,995,860 Dynamic Plus
 * 2,341,595,487,800
 * pzls         total (ns)  (mm:ss)      each (ns)
 * 1465  2,356,828,977,000  (39:16)  1,608,756,980
 * NOTES:
 * 1. IS_HACKY top1465 A5678910E_1C in 39:16 minus nasty custom filter code.
 * 2. Now I release 6.30.055 2020-03-06 21:59:55
 *    => DiufSudoku_V6_30.055.2020-03-06.OK.7z
 * 3. Next I'll combine the 3 filters: hitSet, windex, and filter. so it only
 *    examines cell sets which hint in this puzzle with these vital statistics.
 *    Also I should build the siblingsCount back into the stats.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-09 ??:??:??</b> !IS_HACKY with A2345678910E_1C takes kenages.
 * <pre>
 *          time(ns)  calls     time/call  elims       time/elim hinter
 *        22,578,500 118471           190 662950              34 Naked Single
 *        16,599,500  52176           318 162540             102 Hidden Single
 *        74,717,500  35922         2,079  26317           2,839 Point and Claim
 *        93,952,000  23606         3,980   6481          14,496 Naked Pair
 *        91,518,700  21466         4,263   8455          10,824 Hidden Pair
 *       133,545,200  19378         6,891   1576          84,736 Naked Triple
 *       124,297,900  18983         6,547   1025         121,266 Hidden Triple
 *       112,294,300  18786         5,977    727         154,462 XY-Wing
 *        70,217,900  18269         3,843    690         101,765 Swampfish
 *        74,108,400  17998         4,117    326         227,326 Swordfish
 *       188,731,100  17908        10,538    128       1,474,461 Naked Quad
 *        23,623,200  17885         1,320     10       2,362,320 Jellyfish
 *        81,909,400  17882         4,580    327         250,487 XYZ-Wing
 *     2,087,520,300  17577       118,764   1136       1,837,605 Unique Rectangl
 *       249,378,900  17040        14,634     62       4,022,240 Bi-Uni Grave
 *     1,691,129,400  17010        99,419     21      80,529,971 Aligned Pair
 *    20,693,156,100  16989     1,218,032    585      35,372,916 Aligned Triple
 *    72,601,583,600  16460     4,410,788   1393      52,118,868 Aligned Quad
 *   400,364,534,500  15220    26,305,159   1366     293,092,631 Aligned Pent
 * 1,024,519,475,000  14019    73,080,781   1083     946,001,361 Aligned Hex
 * 2,155,580,220,000  13096   164,598,367    658   3,275,957,781 Aligned Sept
 * 3,677,139,042,500  12519   293,724,661    442   8,319,319,100 Aligned Oct
 * 4,803,689,285,200  12129   396,049,903    257  18,691,397,996 Aligned Nona
 *84,722,939,987,000  11904 7,117,182,458    144 588,353,749,909 Aligned Dec
 *    44,641,685,600  11783     3,788,651  24589       1,815,514 Unary Chain
 *     7,616,627,900   6054     1,258,114   1251       6,088,431 Nishio Chain
 *    14,309,994,600   5055     2,830,859  10228       1,399,099 Multiple Chain
 *    15,073,441,400   1541     9,781,597   9683       1,556,691 Dynamic Chain
 *       132,904,000      3    44,301,333     30       4,430,133 Dynamic Plus
 *96,964,438,059,600
 * pzls          total (ns) (mmmm:ss) (hh:mm:ss)       each (ns)
 * 1465  96,979,452,107,100 (1616:19) (26:56:19)  66,197,578,230
 *
 * top1465 IS_HACKY in 1:47 with A2345678910E_1C. It'll do.
 * top1465 IS_HACKY in 1:53 with A5678910E_2H coz less hints.
 * NOTES:
 * 1. So !IS_HACKY top1465.d5.mt with A5678910E_1C in 26:56:19, ie over a day,
 *    is FAR too slow for practical use, obviously, but it's the best I know how
 *    to do with an incredibly heavy problem. But remember that IS_HACKY takes
 *    about two minutes, by cheating like a bastard.
 * 2. Now I release 6.30.056 2020-03-09 19:05:56
 *    => DiufSudoku_V6_30.056.2020-03-09.OK.7z
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-11 06:54:57</b> I shot the covers method in the hope that NOT
 * EVER calling a method in a tight loop would be substantially faster, but it's
 * actually substantially slower, which I don't understand. Experience tells me
 * that ANY method call in a tight-loop takes kenages compared to inlining-it,
 * because (I presume) Java "does some other stuff" whenever you call a method.
 * Also fixed GUI&lt;->HitSet wiring bugs introduced by using only LSTester.
 * Fixed AHint.hintNumber not incrementing in F5.
 * This run is !IS_HACKY A234E_1C + A5678910_2H (ie not hacky but all hacked).
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      34,925,362 117578        297 660850             52 Naked Single
 *      20,965,416  51493        407 161710            129 Hidden Single
 *      92,221,763  35322      2,610  26407          3,492 Point and Claim
 *     112,333,435  22999      4,884   6592         17,040 Naked Pair
 *     105,400,307  20829      5,060   8477         12,433 Hidden Pair
 *     153,173,280  18723      8,181   1611         95,079 Naked Triple
 *     139,761,173  18319      7,629   1101        126,940 Hidden Triple
 *     115,720,016  18103      6,392    739        156,590 XY-Wing
 *      77,408,624  17581      4,402    661        117,108 Swampfish
 *      85,284,647  17321      4,923    322        264,859 Swordfish
 *     222,051,673  17230     12,887    129      1,721,330 Naked Quad
 *      26,357,570  17208      1,531     12      2,196,464 Jellyfish
 *      91,699,587  17204      5,330    347        264,263 XYZ-Wing
 *   3,142,975,879  16880    186,195   1118      2,811,248 Unique Rectangle
 *     293,367,639  16349     17,944     68      4,314,229 Bi-Uni Grave
 *   1,940,129,809  16315    118,916     27     71,856,659 Aligned Pair
 *  23,207,652,635  16288  1,424,831    609     38,107,803 Aligned Triple
 *  84,458,841,145  15740  5,365,873   1519     55,601,607 Aligned Quad
 *  59,330,334,044  14389  4,123,311    517    114,758,866 Aligned Pent
 *  81,759,053,756  13921  5,873,073    378    216,293,793 Aligned Hex
 *  91,026,830,476  13588  6,699,060    178    511,386,688 Aligned Sept
 *  82,667,204,611  13429  6,155,871     73  1,132,427,460 Aligned Oct
 *  58,227,735,259  13363  4,357,384     21  2,772,749,298 Aligned Nona
 * 342,106,858,970  13343 25,639,425      6 57,017,809,828 Aligned Dec
 *  62,334,465,583  13337  4,673,799  26518      2,350,647 Unary Chain
 *   9,852,455,415   6883  1,431,418   1350      7,298,115 Nishio Chain
 *  20,017,152,788   5821  3,438,782  11693      1,711,891 Multiple Chain
 *  19,305,205,413   1652 11,685,959  10442      1,848,803 Dynamic Chain
 *     291,132,100      3 97,044,033     30      9,704,403 Dynamic Plus
 * 941,238,698,375
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  960,920,456,100 (16:00)  655,918,400
 * NOTES:
 * 1. Benchmrk 2020-02-26 15-14-15 !IS_HACKY A234E_1C + A5678910_2H in 11:46
 *    This run 2020-03-11 06-30-34 !IS_HACKY A234E_1C + A5678910_2H in 16:00
 * 2. I back-up to DiufSudoku_V6_30.057.2020-03-11.06-54-57.SLOWER.7z
 *    and then revert to the covers method only in A10E_2H and run it again.
 *
 * With covers method back in default: path of A10E_2H only 10:12 = 5:48 faster
 * was: 342,106,858,970  13343 25,639,425      6 57,017,809,828 Aligned Dec
 * now:  96,925,250,950  13343  7,264,127      6 16,154,208,491 Aligned Dec
 * and that's just from w/o HitSet: 3 = 5,655 of 385,289 = 1.47% of use cases!
 * now now I've tried making the paramters to covers method all final, in the
 * hope that the JIT compiler will be able to do some magic with them.
 *
 * The "final covers parameters" run took 09:28 = 44 seconds faster again
 * was:  96,925,250,950  13343  7,264,127      6 16,154,208,491 Aligned Dec
 * now:  48,609,263,005  13343  3,643,053      6  8,101,543,834 Aligned Dec
 *
 * So yes, I should reinstate the covers methods in all A*E's. It'll take me
 * about an hour to change the code and 2 days to test. I wish I was smarter!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-19 18:22:58</b> Pushed the HitSet, prepare and report methods
 * up into the abstract AAlignedSetExclusionBase class. Stuck basic filtering in
 * A10E even when !isHacky coz vanilla took 34 hours down to 22 hours. BIG Sigh.
 * Might do the same with A987E, or not.
 * This run is IS_HACKY A5678910_1C (ie hacky with all correct).
 * <pre>
 *       time(ns)  calls  time/call  elims time/elim hinter
 *     21,897,400 118445        184 662970        33 Naked Single
 *     20,521,600  52148        393 162470       126 Hidden Single
 *     76,385,000  35901      2,127  26306     2,903 Point and Claim
 *     95,270,800  23591      4,038   6485    14,690 Naked Pair
 *     92,552,500  21451      4,314   8450    10,952 Hidden Pair
 *    134,404,200  19364      6,940   1574    85,390 Naked Triple
 *    122,921,800  18973      6,478   1028   119,573 Hidden Triple
 *    110,742,700  18776      5,898    727   152,328 XY-Wing
 *     67,596,600  18260      3,701    688    98,250 Swampfish
 *     77,265,700  17991      4,294    329   234,850 Swordfish
 *    191,804,700  17900     10,715    128 1,498,474 Naked Quad
 *     23,320,300  17877      1,304     10 2,332,030 Jellyfish
 *     83,560,200  17874      4,674    328   254,756 XYZ-Wing
 *  2,106,577,000  17568    119,909   1134 1,857,651 Unique Rectangle
 *    238,461,200  17032     14,000     58 4,111,400 Bi-Uni Grave
 *      9,699,900  17004        570     21   461,900 Aligned Pair
 *     55,489,400  16983      3,267    562    98,735 Aligned Triple
 *    119,920,900  16476      7,278   1382    86,773 Aligned Quad
 *    154,278,000  15246     10,119   1344   114,790 Aligned Pent
 *    108,637,700  14067      7,722   1097    99,031 Aligned Hex
 *     81,396,600  13131      6,198    678   120,053 Aligned Sept
 *     71,626,100  12540      5,711    441   162,417 Aligned Oct
 *    137,174,500  12153     11,287    239   573,951 Aligned Nona
 *    135,960,300  11943     11,384    159   855,096 Aligned Dec
 * 47,235,027,800  11808  4,000,256  24598 1,920,279 Unary Chain
 *  8,033,920,600   6065  1,324,636   1252 6,416,869 Nishio Chain
 * 15,954,582,700   5065  3,149,966  10282 1,551,700 Multiple Chain
 * 16,445,176,600   1542 10,664,835   9693 1,696,603 Dynamic Chain
 *    148,760,800      3 49,586,933     30 4,958,693 Dynamic Plus
 * 92,154,933,600
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   107,159,231,700 (01:47)        73,146,233
 * NOTES:
 * 1. 1:47 is fast enough, even though I'm cheating like a mo-fo.
 * 2. Release 6.30.058 2020-03-19 18:22:58
 *    => DiufSudoku_V6_30.058.2020-03-19.7z
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-19 18:22:58</b> Pushed A5+E's "standard" counters up into
 * AAlignedSetExclusionBase. Use a getCounters method to reflectively find
 * all Counter fields in this A*E-object, so that report() and hitCounters()
 * can both be pushed up also.
 * <hr>
 * <p>
 * <b>KRC 2020-04-03 16:37:59</b> HoDoKu SingleDigitPattern first pass.
 * Downloaded HoDoKu code to boost from. Started with sdp which is now complete:
 * Skyscraper, TwoStringKite are algorithmically "as is". EmptyRectangle is all
 * my implementation, but still based on the ideas I first saw in HoDoKu. WWing
 * (in wing package with XYWing and XYZWing, which it most resembles) is also
 * basically boosted "as is" from HoDoKu.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      21,940,700 118241        185 665300             32 Naked Single
 *      15,667,700  51711        302 163500             95 Hidden Single
 *      72,719,600  35361      2,056  26388          2,755 Point and Claim
 *      89,645,700  23039      3,891   6648         13,484 Naked Pair
 *      86,637,100  20862      4,152   8442         10,262 Hidden Pair
 *     122,716,600  18771      6,537   1667         73,615 Naked Triple
 *     115,150,400  18355      6,273   1095        105,160 Hidden Triple
 *      88,858,200  18139      4,898    741        119,916 XY-Wing
 *      64,774,800  17613      3,677    354        182,979 XYZ-Wing
 *     188,063,400  17285     10,880    632        297,568 W-Wing
 *      80,640,400  16847      4,786    942         85,605 Skyscraper
 *      46,677,100  16350      2,854    920         50,735 Two String Kite
 *      58,717,600  15430      3,805    520        112,918 Empty Rectangle
 *      53,996,100  14910      3,621    381        141,722 Swampfish
 *      61,869,500  14732      4,199    256        241,677 Swordfish
 *     156,191,000  14657     10,656    104      1,501,836 Naked Quad
 *      17,735,300  14639      1,211      8      2,216,912 Jellyfish
 *   1,556,226,600  14637    106,321    873      1,782,619 Unique Rectangle
 *     198,491,600  14225     13,953     32      6,202,862 Bi-Uni Grave
 *     969,218,200  14210     68,206     22     44,055,372 Aligned Pair
 *   5,532,311,900  14188    389,928    524     10,557,847 Aligned Triple
 *  71,232,115,300  13709  5,196,011   1321     53,922,873 Aligned Quad
 *  44,894,941,800  12534  3,581,852    468     95,929,362 Aligned Pent
 *  65,937,630,600  12108  5,445,790    370    178,209,812 Aligned Hex
 *  72,632,825,900  11783  6,164,204    154    471,641,726 Aligned Sept
 *  62,940,001,300  11641  5,406,752     66    953,636,383 Aligned Oct
 *  48,216,020,000  11582  4,163,013     19  2,537,685,263 Aligned Nona
 *  84,881,075,200  11564  7,340,113      5 16,976,215,040 Aligned Dec
 *  42,930,412,100  11559  3,714,024  19464      2,205,631 Unary Chain
 *   8,272,168,600   6626  1,248,440    976      8,475,582 Nishio Chain
 *  17,225,076,700   5821  2,959,126  11693      1,473,110 Multiple Chain
 *  16,789,406,500   1652 10,163,078  10442      1,607,872 Dynamic Chain
 *     149,652,300      3 49,884,100     30      4,988,410 Dynamic Plus
 * 545,699,575,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  560,613,094,600 (09:20)  382,671,054
 * NOTES:
 * 1. 9:20 is acceptable. No cheating here. Just good honest hacking.
 * 2. Release 6.30.059 2020-04-03 16:37:59
 *    => DiufSudoku_V6_30.059.2020-04-03.7z
 * 3. Where to next I do not know exactly. I'll have a look at hobiwan's Finned
 * Fish and see if I can pinch the concepts (but not the code) and tack them
 * onto Jullerat's fisherman.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-04-05 15:10:60</b> HoDoKu FinnedFisherman first cut.
 * integrated Finned Fish into DIUF without copy-pasting hobiwans code into DIUF
 * which would've been pretty useless anyway because I'm too stupid to follow
 * it.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      24,485,400 118234        207 665540             36 Naked Single
 *      19,110,900  51680        369 163380            116 Hidden Single
 *      72,631,900  35342      2,055  26345          2,756 Point and Claim
 *     100,605,500  23027      4,369   6635         15,162 Naked Pair
 *      97,461,700  20849      4,674   8421         11,573 Hidden Pair
 *     133,860,600  18768      7,132   1659         80,687 Naked Triple
 *     127,176,600  18354      6,929   1082        117,538 Hidden Triple
 *     146,549,700  18139      8,079    746        196,447 XY-Wing
 *      76,771,400  17614      4,358    350        219,346 XYZ-Wing
 *     213,963,900  17290     12,375    620        345,103 W-Wing
 *      90,844,000  16859      5,388    904        100,491 Skyscraper
 *      49,453,600  16383      3,018    907         54,524 Two String Kite
 *      64,726,300  15476      4,182    516        125,438 Empty Rectangle
 *      61,188,400  14960      4,090    372        164,484 Swampfish
 *      66,328,600  14784      4,486    257        258,087 Swordfish
 *     171,522,600  14708     11,661    103      1,665,267 Naked Quad
 *      19,306,200  14691      1,314      8      2,413,275 Jellyfish
 *   1,740,227,400  14689    118,471    868      2,004,870 Unique Rectangle
 *     105,376,900  14279      7,379    418        252,097 Finned Swampfish
 *      69,531,400  13927      4,992    100        695,314 Finned Swordfish
 *      40,941,600  13842      2,957      0              0 Finned Jellyfish
 *     243,579,900  13842     17,597     32      7,611,871 Bi-Uni Grave
 *   1,045,093,500  13827     75,583     23     45,438,847 Aligned Pair
 *   5,828,448,200  13804    422,228    515     11,317,375 Aligned Triple
 *  77,398,617,800  13333  5,805,041   1338     57,846,500 Aligned Quad
 *  47,743,656,800  12145  3,931,136    452    105,627,559 Aligned Pent
 *  69,353,350,700  11735  5,909,957    378    183,474,472 Aligned Hex
 *  76,661,568,300  11404  6,722,340    153    501,056,001 Aligned Sept
 *  67,321,982,400  11263  5,977,269     64  1,051,905,975 Aligned Oct
 *  52,782,060,000  11205  4,710,580     19  2,778,003,157 Aligned Nona
 *  88,949,801,900  11187  7,951,175      5 17,789,960,380 Aligned Dec
 *  46,308,874,000  11182  4,141,376  19265      2,403,782 Unary Chain
 *   8,886,769,300   6377  1,393,565    724     12,274,543 Nishio Chain
 *  19,232,302,500   5815  3,307,360  11690      1,645,192 Multiple Chain
 *  18,536,364,000   1649 11,240,972  10412      1,780,288 Dynamic Chain
 *     178,289,200      3 59,429,733     30      5,942,973 Dynamic Plus
 * 583,962,823,100
 * pzls       total (ns)  (mm:ss)    each (ns)
 * 1465  600,570,686,200  (10:00)  409,945,860
 * NOTES:
 * 1. 10:00 40 seconds slower, which ain't great, but there you have it. My
 *    Chainers are pretty bloody fast, so my best attempts at a first cut of
 *    other stuff looks second hand by comparison.
 * 2. Release 6.30.060 2020-04-05 15:10:60
 *    => DiufSudoku_V6_30.060.2020-04-05.7z
 * 3. I'm pretty sure there must be some bugs remaining in all of my HoDoKu
 *    boosting, so I guess I should put some test-cases in next. I'm NOT going
 *    to attempt HoDoKu's "advanced" Fishing techniques: (Finned) Franken Fish,
 *    (Finned) Mutant Fish, Siamese Fish, and Shark Finned Franken
 *    Swordfish can go____themselves (roughly) as far as I'm concerned. Finned
 *    Fish was complex enough for me, and they're defined as "simple" in
 *    hobiwans documentation. To me a "simple" fish is a "basic" fish, no fins,
 *    no fuss, just good fast eliminations, with surprisingly little code.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-04-10 15:38:61</b> HoDoKu FinnedFisherman second cut, with
 * variable number of core and finned bases, to find more hints. Updated
 * FinnedFishermanTest, and fixed hint-building.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      21,780,800 118249        184 665910             32 Naked Single
 *      19,870,500  51658        384 163290            121 Hidden Single
 *      65,540,100  35329      1,855  26312          2,490 Point and Claim
 *      90,844,500  23013      3,947   6638         13,685 Naked Pair
 *      85,778,700  20838      4,116   8440         10,163 Hidden Pair
 *     123,501,000  18750      6,586   1661         74,353 Naked Triple
 *     116,542,000  18333      6,356   1072        108,714 Hidden Triple
 *      94,145,600  18121      5,195    754        124,861 XY-Wing
 *      70,549,100  17591      4,010    347        203,311 XYZ-Wing
 *     184,356,600  17269     10,675    622        296,393 W-Wing
 *      82,367,800  16833      4,893    892         92,340 Skyscraper
 *      46,953,200  16362      2,869    897         52,344 Two String Kite
 *      57,723,000  15465      3,732    517        111,649 Empty Rectangle
 *      52,948,500  14948      3,542    368        143,881 Swampfish
 *      60,868,600  14772      4,120    255        238,700 Swordfish
 *     152,669,700  14696     10,388    103      1,482,230 Naked Quad
 *      17,639,800  14679      1,201      8      2,204,975 Jellyfish
 *   1,566,026,000  14677    106,699    868      1,804,177 Unique Rectangle
 *     178,990,600  14267     12,545    408        438,702 Finned Swampfish
 *     551,262,800  13925     39,587    378      1,458,367 Finned Swordfish
 *   1,013,931,100  13635     74,362     61     16,621,821 Finned Jellyfish
 *     189,362,500  13583     13,941     30      6,312,083 Bi-Uni Grave
 *     937,331,300  13569     69,078     22     42,605,968 Aligned Pair
 *   5,267,617,200  13547    388,840    504     10,451,621 Aligned Triple
 *  66,267,759,500  13084  5,064,793   1326     49,975,685 Aligned Quad
 *  42,302,533,900  11907  3,552,744    451     93,797,192 Aligned Pent
 *  61,367,527,700  11500  5,336,306    379    161,919,598 Aligned Hex
 *  67,690,602,300  11168  6,061,121    152    445,332,909 Aligned Sept
 *  57,970,896,500  11027  5,257,177     65    891,859,946 Aligned Oct
 *  49,445,662,600  10968  4,508,174     18  2,746,981,255 Aligned Nona
 *  78,618,642,900  10951  7,179,129      5 15,723,728,580 Aligned Dec
 *  41,493,384,800  10946  3,790,734  18982      2,185,933 Unary Chain
 *   7,961,565,400   6235  1,276,915    513     15,519,620 Nishio Chain
 *  17,157,724,000   5821  2,947,556  11716      1,464,469 Multiple Chain
 *  16,997,948,600   1647 10,320,551  10392      1,635,676 Dynamic Chain
 *     140,907,400      3 46,969,133     30      4,696,913 Dynamic Plus
 * 518,463,756,600
 * pzls       total (ns)  (mm:ss)    each (ns)
 * 1465  533,345,017,200  (08:53)  364,058,032
 * NOTES:
 * 1. 08:53 is a about 40 seconds faster than previous. Cool!
 * 2. Release 6.30.061 2020-04-10 15:38:61
 *    => DiufSudoku_V6_30.061.2020-04-10.7z
 * 3. I'll Probably look at Sashimi next, although I have no idea how to find
 *    them.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-04-11 16:32:02</b> HoDoKu SashimiFisherman first pass, finding
 * all Sashimi fish, and test cases.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      23,601,800 118256        199 665900             35 Naked Single
 *      16,299,700  51666        315 163450             99 Hidden Single
 *      68,917,000  35321      1,951  26318          2,618 Point and Claim
 *      96,070,200  23002      4,176   6631         14,488 Naked Pair
 *      86,620,100  20826      4,159   8439         10,264 Hidden Pair
 *     155,883,100  18738      8,319   1657         94,075 Naked Triple
 *     116,591,300  18323      6,363   1072        108,760 Hidden Triple
 *      97,302,200  18111      5,372    759        128,197 XY-Wing
 *      72,690,400  17576      4,135    348        208,880 XYZ-Wing
 *     200,827,900  17253     11,640    625        321,324 W-Wing
 *      83,927,000  16815      4,991    883         95,047 Skyscraper
 *      45,770,400  16349      2,799    892         51,312 Two String Kite
 *      61,416,500  15457      3,973    514        119,487 Empty Rectangle
 *      54,653,400  14943      3,657    368        148,514 Swampfish
 *      62,695,900  14767      4,245    257        243,952 Swordfish
 *     158,976,500  14690     10,822    103      1,543,461 Naked Quad
 *      18,052,400  14673      1,230      8      2,256,550 Jellyfish
 *   1,563,983,100  14671    106,603    870      1,797,681 Unique Rectangle
 *     188,117,000  14260     13,191    410        458,821 Finned Swampfish
 *     571,516,200  13916     41,068    377      1,515,958 Finned Swordfish
 *   1,053,068,700  13627     77,278     62     16,984,979 Finned Jellyfish
 *     150,581,200  13574     11,093     62      2,428,729 Sashimi Swampfish
 *     160,520,200  13518     11,874     37      4,338,383 Sashimi Swordfish
 *     222,955,400  13487     16,531      3     74,318,466 Sashimi Jellyfish
 *     193,026,300  13484     14,315     26      7,424,088 Bi-Uni Grave
 *     978,318,200  13472     72,618     22     44,469,009 Aligned Pair
 *   5,415,118,800  13450    402,611    504     10,744,283 Aligned Triple
 *  70,457,470,800  12987  5,425,230   1322     53,296,120 Aligned Quad
 *  43,932,465,900  11812  3,719,307    450     97,627,702 Aligned Pent
 *  63,066,611,300  11406  5,529,248    379    166,402,668 Aligned Hex
 *  68,713,705,800  11074  6,204,958    151    455,057,654 Aligned Sept
 *  60,079,801,300  10934  5,494,768     65    924,304,635 Aligned Oct
 *  48,288,310,000  10875  4,440,304     18  2,682,683,888 Aligned Nona
 *  84,140,220,100  10858  7,749,145      5 16,828,044,020 Aligned Dec
 *  44,246,154,900  10853  4,076,859  18807      2,352,642 Unary Chain
 *   8,323,028,100   6191  1,344,375    460     18,093,539 Nishio Chain
 *  18,277,582,100   5821  3,139,938  11716      1,560,053 Multiple Chain
 *  17,665,421,100   1647 10,725,817  10392      1,699,905 Dynamic Chain
 *     150,200,100      3 50,066,700     30      5,006,670 Dynamic Plus
 * 539,258,472,400
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   555,615,835,300 (09:15)       379,259,955
 * NOTES:
 * 1. 09:15 is a about 20 seconds SLOWER than previous. Could be worse.
 * 2. Release 6.30.062 2020-04-11 16:32:02
 *    => DiufSudoku_V6_30.062.2020-04-11.7z
 * 3. I guess I'll look around HoDoKu to see what else I can boost. I might get
 *    into Mutant Fish, or not.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-04-21 23:17:03</b> HoDoKu AlmostLockedSets first pass, includes
 * ALS-XZ (suspect) and ALS-XY-Wing (broken).
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      21,594,500 118303        182 666740             32 Naked Single
 *      20,868,400  51629        404 164670            126 Hidden Single
 *      72,274,900  35162      2,055  26225          2,755 Point and Claim
 *      90,060,700  22886      3,935   6570         13,707 Naked Pair
 *      82,674,600  20734      3,987   8452          9,781 Hidden Pair
 *     124,110,600  18631      6,661   1629         76,188 Naked Triple
 *     114,767,000  18213      6,301   1068        107,459 Hidden Triple
 *      61,303,400  18003      3,405    643         95,339 Swampfish
 *      71,699,200  17739      4,041    320        224,060 Swordfish
 *      92,165,700  17644      5,223    749        123,051 XY-Wing
 *      69,389,400  17108      4,055    347        199,969 XYZ-Wing
 *     187,078,200  16784     11,146    614        304,687 W-Wing
 *      74,856,300  16363      4,574    829         90,297 Skyscraper
 *      43,546,000  15940      2,731    724         60,146 Two String Kite
 *      59,327,400  15216      3,899    455        130,389 Empty Rectangle
 *     158,902,200  14761     10,765    102      1,557,864 Naked Quad
 *      19,159,200  14744      1,299      8      2,394,900 Jellyfish
 *   1,646,099,700  14742    111,660    882      1,866,326 Unique Rectangle
 *     183,977,500  14329     12,839    412        446,547 Finned Swampfish
 *     556,696,500  13984     39,809    381      1,461,145 Finned Swordfish
 *   1,033,682,300  13693     75,489     66     15,661,853 Finned Jellyfish
 *     149,291,200  13639     10,945     62      2,407,922 Sashimi Swampfish
 *     157,625,000  13583     11,604     38      4,148,026 Sashimi Swordfish
 *     217,674,000  13551     16,063      3     72,558,000 Sashimi Jellyfish
 *  26,162,288,900  13548  1,931,081   2603     10,050,821 ALS-XZ
 *       2,472,400  11238        220      0              0 ALS-Chain
 *     173,717,900  11238     15,458     20      8,685,895 Bi-Uni Grave
 *     767,006,100  11229     68,305     11     69,727,827 Aligned Pair
 *   4,475,741,100  11218    398,978    253     17,690,676 Aligned Triple
 *  58,780,421,900  10988  5,349,510    612     96,046,441 Aligned Quad
 *  37,571,084,000  10481  3,584,685    206    182,383,902 Aligned Pent
 *  56,950,845,900  10304  5,527,061    179    318,161,150 Aligned Hex
 *  62,641,955,700  10154  6,169,190     76    824,236,259 Aligned Sept
 *  55,293,975,300  10088  5,481,163     30  1,843,132,510 Aligned Oct
 *  43,126,355,400  10059  4,287,340     14  3,080,453,957 Aligned Nona
 *  71,785,955,800  10046  7,145,725      3 23,928,651,933 Aligned Dec
 *  38,885,652,000  10043  3,871,915  16923      2,297,798 Unary Chain
 *   7,487,571,600   5901  1,268,864    450     16,639,048 Nishio Chain
 *  16,695,324,100   5541  3,013,052  11024      1,514,452 Multiple Chain
 *  16,902,317,500   1618 10,446,426  10282      1,643,874 Dynamic Chain
 *     174,857,300      3 58,285,766     30      5,828,576 Dynamic Plus
 * 503,186,366,800
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   538,770,447,400 (08:58)       367,761,397
 * NOTES:
 * 1. 08:58 is a 20 seconds faster than previous. But ALS-XY is still suspect,
 *    coz it's got a "dodgy" check in it to detect and throw-away invalid hints
 *    which break the puzzle, and one really should not need to do that, should
 *    one... so it's still a piece of s__t, I just want it in the tin before I
 *    I hack the s__t out of it trying to solve these problems, so I release.
 * 2. Release 6.30.063 2020-04-21 23:17:03
 *    => DiufSudoku_V6_30.063.2020-04-21.7z
 * 3. Problems described above are my todo list.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.064 2020-04-30 10:41:04</b> HoDoKu AlmostLockedSets 2nd and 3rd
 * pass with ALS-XZ working properly and optimised + ALS-XY-Wing still broken.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      34,770,400 118679        292 672800            51 Naked Single
 *      21,433,000  51399        416 168230           127 Hidden Single
 *      77,544,700  34576      2,242  25991         2,983 Point and Claim
 *      85,193,900  22382      3,806   6539        13,028 Naked Pair
 *      75,034,200  20252      3,705   8479         8,849 Hidden Pair
 *     140,096,200  18115      7,733   1662        84,293 Naked Triple
 *     127,628,600  17695      7,212   1025       124,515 Hidden Triple
 *      53,505,200  17499      3,057    680        78,684 Swampfish
 *      79,700,400  17230      4,625    320       249,063 Swordfish
 *     106,815,300  17139      6,232    755       141,477 XY-Wing
 *      78,121,300  16610      4,703    350       223,203 XYZ-Wing
 *     208,810,400  16282     12,824    621       336,248 W-Wing
 *      92,533,100  15857      5,835    852       108,606 Skyscraper
 *      50,609,500  15418      3,282    751        67,389 Two String Kite
 *      65,524,900  14667      4,467    457       143,380 Empty Rectangle
 *     174,663,900  14210     12,291    105     1,663,465 Naked Quad
 *      20,282,900  14192      1,429      9     2,253,655 Jellyfish
 *   1,799,318,100  14189    126,810    857     2,099,554 Unique Rectangle
 *     191,652,200  13791     13,896    396       483,970 Finned Swampfish
 *     579,333,800  13460     43,041    365     1,587,215 Finned Swordfish
 *   1,075,450,700  13176     81,621     68    15,815,451 Finned Jellyfish
 *     152,688,100  13119     11,638     63     2,423,620 Sashimi Swampfish
 *     161,662,700  13063     12,375     43     3,759,597 Sashimi Swordfish
 *     226,450,900  13028     17,381      6    37,741,816 Sashimi Jellyfish
 *  91,517,990,700  13022  7,027,951   8329    10,987,872 ALS-XZ
 *  52,718,084,600   7507  7,022,523      0             0 ALS-XY-Wing
 *     129,058,400   7507     17,191      4    32,264,600 Bi-Uni Grave
 *     506,651,200   7505     67,508      7    72,378,742 Aligned Pair
 *   3,057,875,700   7498    407,825     53    57,695,767 Aligned Triple
 *  42,462,621,800   7448  5,701,211    116   366,057,084 Aligned Quad
 *  26,890,215,600   7355  3,656,045     35   768,291,874 Aligned Pent
 *  41,156,892,500   7321  5,621,758     23 1,789,430,108 Aligned Hex
 *  44,293,854,900   7302  6,065,989     22 2,013,357,040 Aligned Sept
 *  38,881,769,300   7282  5,339,435      6 6,480,294,883 Aligned Oct
 *  31,581,984,900   7276  4,340,569      6 5,263,664,150 Aligned Nona
 *  25,562,156,500   7271  3,515,631   8928     2,863,144 Unary Chain
 *   6,006,195,100   4783  1,255,738    406    14,793,583 Nishio Chain
 *  13,326,082,300   4476  2,977,230   8352     1,595,555 Multiple Chain
 *  15,005,557,000   1497 10,023,752   9396     1,597,015 Dynamic Chain
 *     139,218,500      3 46,406,166     30     4,640,616 Dynamic Plus
 * 438,915,033,400
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   894,919,637,000 (14:54)       610,866,646
 * NOTES:
 * 1. 14:54 is a 6 minutes slower than previous, which is a bit s__t, but
 *    ALS-XY-Wing is still rooted, and I hope that'll improve when it's fixed.
 *    I just want a backup before I hack it, hence I release. That and it's
 *    been a week since my last release, and weekly backups sounds good to me.
 * 2. I also note that Aligned Dec no longer finds any hints, so I shall just
 *    drop it from my wanted hinters list. I've had my fun there.
 * 2. Release 6.30.064 2020-04-30 10:41:04
 *    => DiufSudoku_V6_30.064.2020-04-30.7z
 * 3. Next cab off the rank is why doesn't ALS-XY-Wing find any hints?
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.065 2020-05-07 12:34:05</b> HoDoKu ALS-XY-Wing. Imported hobiwan
 * code implementing ALS-XY-Wing and did the minimum required to get it working
 * in DIUF. It works AND it's faster than my hack implementation.
 * <pre>
 *         time(ns)  calls  time/call  elims      time/elim hinter
 *       33,287,597 120585        276 677400             49 Naked Single
 *       25,332,054  52845        479 172350            146 Hidden Single
 *       75,248,476  35610      2,113  25840          2,912 Point and Claim
 *       91,324,809  23435      3,896   6486         14,080 Naked Pair
 *       81,648,567  21275      3,837   8432          9,683 Hidden Pair
 *      153,316,190  19117      8,019   1658         92,470 Naked Triple
 *      140,000,908  18672      7,497   1010        138,614 Hidden Triple
 *       57,967,344  18469      3,138    709         81,759 Swampfish
 *       89,021,266  18182      4,896    321        277,324 Swordfish
 *      120,487,184  18088      6,661    699        172,370 XY-Wing
 *       88,050,635  17601      5,002    356        247,333 XYZ-Wing
 *      229,299,418  17266     13,280    605        379,007 W-Wing
 *       99,104,603  16850      5,881    841        117,841 Skyscraper
 *       53,969,240  16414      3,288    746         72,344 Two String Kite
 *       68,330,962  15668      4,361    466        146,632 Empty Rectangle
 *      191,329,589  15202     12,585    100      1,913,295 Naked Quad
 *       22,942,250  15182      1,511     11      2,085,659 Jellyfish
 *    1,869,520,762  15179    123,164    861      2,171,336 Unique Rectangle
 *      206,787,092  14773     13,997    410        504,358 Finned Swampfish
 *      626,055,771  14428     43,391    393      1,593,017 Finned Swordfish
 *    1,175,984,555  14126     83,249     72     16,333,118 Finned Jellyfish
 *      169,230,752  14066     12,031     58      2,917,771 Sashimi Swampfish
 *      182,443,511  14014     13,018     41      4,449,841 Sashimi Swordfish
 *      254,859,374  13981     18,228      7     36,408,482 Sashimi Jellyfish
 *  101,346,141,277  13974  7,252,478   8442     12,004,991 ALS-XZ
 *   25,656,864,712   8330  3,080,055   4983      5,148,879 ALS-XY-Wing
 *       79,586,008   4187     19,007      0              0 Bi-Uni Grave
 *      274,671,970   4187     65,601      0              0 Aligned Pair
 *    1,825,435,692   4187    435,976      5    365,087,138 Aligned Triple
 *   25,002,499,928   4182  5,978,598     16  1,562,656,245 Aligned Quad
 *   16,643,848,329   4169  3,992,287      6  2,773,974,721 Aligned Pent
 *   23,791,813,101   4165  5,712,320      2 11,895,906,550 Aligned Hex
 *   29,227,089,589   4163  7,020,679      6  4,871,181,598 Aligned Sept
 *   26,022,111,833   4158  6,258,324      0              0 Aligned Oct
 *   23,499,947,972   4158  5,651,743      1 23,499,947,972 Aligned Nona
 *   12,984,766,133   4157  3,123,590   2136      6,079,010 Unary Chain
 *    4,264,735,198   3412  1,249,922    338     12,617,559 Nishio Chain
 *    9,141,542,061   3164  2,889,235   5444      1,679,195 Multiple Chain
 *   12,879,668,085   1284 10,030,894   8094      1,591,261 Dynamic Chain
 *      142,203,700      3 47,401,233     30      4,740,123 Dynamic Plus
 *  318,888,468,497
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  759,755,112,500 (12:39)  518,604,172
 * NOTES:
 * 1. 12:36 is a 2 minutes faster than previous, which was 6 minutes slower
 *    than his previous, so it's still "a bit s__t", but ALS-XY-Wing improved
 *    things, just not as much as I'd hoped they would.
 * 2. Note that: Bi-Uni Grave, Aligned Pair and Aligned Oct no longer find any
 *    hints, so I've unwanted these hinters. I've had my fun with them.
 * 2. Release 6.30.065 2020-05-07 12:34:05
 *    => DiufSudoku_V6_30.065.2020-05-07.7z
 * 3. Next to do is pull the mimimum required ALS-XY-Wing code from HoDoKu
 *    classes into my HoDoKuAlsXyWing, eradicating the HoDoKu classes (bloat).
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.066 2020-05-07 12:34:05</b> HoDoKu ALS-XY-Wing. Imported hobiwan
 * code implementing ALS-XY-Wing and did the minimum required to get it working
 * in DIUF. It works AND it's faster than my hack implementation.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      30,464,800 120309        253 676960             45 Naked Single
 *      21,961,200  52613        417 172150            127 Hidden Single
 *      75,860,500  35398      2,143  25832          2,936 Point and Claim
 *      89,688,300  23234      3,860   6433         13,941 Naked Pair
 *      81,337,200  21102      3,854   8442          9,634 Hidden Pair
 *     149,361,100  18950      7,881   1660         89,976 Naked Triple
 *     137,344,900  18505      7,422   1009        136,119 Hidden Triple
 *      58,983,100  18306      3,222    675         87,382 Swampfish
 *      85,874,000  18033      4,762    323        265,863 Swordfish
 *     114,255,400  17939      6,369    689        165,827 XY-Wing
 *      83,597,400  17456      4,789    341        245,153 XYZ-Wing
 *     223,841,100  17134     13,064    587        381,330 W-Wing
 *      97,120,800  16729      5,805    835        116,312 Skyscraper
 *      53,531,000  16301      3,283    743         72,047 Two String Kite
 *      68,098,000  15558      4,377    461        147,718 Empty Rectangle
 *     185,007,200  15097     12,254    102      1,813,796 Naked Quad
 *      21,952,700  15076      1,456      8      2,744,087 Jellyfish
 *   2,112,244,200  15074    140,124    872      2,422,298 Unique Rectangle
 *     203,650,600  14666     13,885    409        497,923 Finned Swampfish
 *     621,771,300  14321     43,416    379      1,640,557 Finned Swordfish
 *   1,150,487,800  14030     82,001     68     16,918,938 Finned Jellyfish
 *     165,423,500  13973     11,838     56      2,953,991 Sashimi Swampfish
 *     180,587,600  13922     12,971     43      4,199,711 Sashimi Swordfish
 *     250,860,600  13887     18,064      6     41,810,100 Sashimi Jellyfish
 *  99,067,851,400  13881  7,136,939   8447     11,728,169 ALS-XZ
 *  22,475,748,400   8241  2,727,308   4360      5,154,988 ALS-XY-Wing
 *   1,966,948,700   4607    426,947     16    122,934,293 Aligned Triple
 *  26,609,603,800   4592  5,794,774     63    422,374,663 Aligned Quad
 *  18,274,616,100   4542  4,023,473      7  2,610,659,442 Aligned Pent
 *  28,253,014,200   4537  6,227,245      2 14,126,507,100 Aligned Hex
 *  32,844,417,700   4535  7,242,429      5  6,568,883,540 Aligned Sept
 *  24,097,158,000   4531  5,318,286      2 12,048,579,000 Aligned Nona
 *  14,999,142,200   4529  3,311,800   2513      5,968,620 Unary Chain
 *   4,501,170,000   3614  1,245,481    345     13,046,869 Nishio Chain
 *   9,652,892,300   3359  2,873,739   5809      1,661,713 Multiple Chain
 *  13,072,708,100   1330  9,829,103   8302      1,574,645 Dynamic Chain
 *     119,728,100      3 39,909,366     30      3,990,936 Dynamic Plus
 * 302,198,303,300
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  729,834,872,300 (12:09)  498,180,800
 * NOTES:
 * 1. 12:09 is about 30 seconds faster than previous, so it's still about 5
 *    minutes slower than before ALSs: ie "a bit s__t", just less s__t.
 * 2. Release 6.30.066 2020-05-23 17:23:06
 *    => DiufSudoku_V6_30.066.2020-05-23.7z
 * 3. Don't know what, if anything, I might do next. I'm sick of ALSs, but I
 *    had always planned to do ALS-XY-Chains, so I guess I should proceed.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.067 2020-05-25 10:12:07</b> HoDoKu ALS-XY-Chain. Imported HoDoKu
 * code implementing ALS-XY-Chain and did the minimum required to get it working
 * in DIUF. It works BUT it relies on the invalid(hint) method to ignore a few
 * hints which break the puzzle, which is total complete and utter HACK! Note
 * that HdkAlsXyWing also now relies on invalid coz it fail on 87#top1465.d5.mt.
 * <pre>
 *         time(ns)  calls  time/call  elims     time/elim hinter
 *       36,257,600 119593        303 678040            53 Naked Single
 *       20,590,300  51789        397 172260           119 Hidden Single
 *       74,956,800  34563      2,168  25855         2,899 Point and Claim
 *       85,036,500  22439      3,789   6468        13,147 Naked Pair
 *       76,024,600  20294      3,746   8418         9,031 Hidden Pair
 *      139,371,800  18174      7,668   1674        83,256 Naked Triple
 *      129,461,300  17730      7,301   1012       127,926 Hidden Triple
 *       55,348,900  17528      3,157    687        80,566 Swampfish
 *       79,344,000  17252      4,599    307       258,449 Swordfish
 *      110,838,200  17165      6,457    700       158,340 XY-Wing
 *       80,763,000  16676      4,843    353       228,790 XYZ-Wing
 *      221,302,900  16349     13,536    601       368,224 W-Wing
 *       92,800,400  15926      5,826    815       113,865 Skyscraper
 *       50,795,800  15510      3,275    749        67,818 Two String Kite
 *       63,735,900  14761      4,317    458       139,161 Empty Rectangle
 *      175,363,400  14303     12,260     97     1,807,870 Naked Quad
 *       19,822,200  14284      1,387     11     1,802,018 Jellyfish
 *    1,682,592,900  14281    117,820    853     1,972,559 Unique Rectangle
 *      192,378,700  13877     13,863    372       517,147 Finned Swampfish
 *      585,522,400  13562     43,173    379     1,544,913 Finned Swordfish
 *    1,091,125,000  13267     82,243     66    16,532,196 Finned Jellyfish
 *      157,561,700  13212     11,925     58     2,716,581 Sashimi Swampfish
 *      168,199,500  13160     12,781     37     4,545,932 Sashimi Swordfish
 *      232,931,200  13130     17,740      7    33,275,885 Sashimi Jellyfish
 *   93,024,122,500  13123  7,088,632   8328    11,170,043 ALS-XZ
 *   17,505,359,500   7586  2,307,587   3266     5,359,877 ALS-XY-Wing
 *    6,191,399,900   4896  1,264,583    686     9,025,364 ALS-Chain
 *    1,876,669,200   4435    423,149     18   104,259,400 Aligned Triple
 *   27,241,734,000   4419  6,164,682     70   389,167,628 Aligned Quad
 *   17,629,899,400   4363  4,040,774     11 1,602,718,127 Aligned Pent
 *   25,233,634,100   4352  5,798,169      4 6,308,408,525 Aligned Hex
 *   32,446,952,900   4349  7,460,784      6 5,407,825,483 Aligned Sept
 *   25,202,696,600   4344  5,801,725      3 8,400,898,866 Aligned Nona
 *   13,256,810,600   4341  3,053,860   2627     5,046,368 Unary Chain
 *    4,254,786,900   3400  1,251,407    337    12,625,480 Nishio Chain
 *    8,942,593,700   3153  2,836,217   5114     1,748,649 Multiple Chain
 *   12,240,200,100   1252  9,776,517   7639     1,602,330 Dynamic Chain
 *      139,406,600      3 46,468,866     30     4,646,886 Dynamic Plus
 *  290,808,391,000
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   717,959,688,900 (11:57)       490,074,872
 * NOTES:
 * 1. 11:57 is about 10 seconds faster than previous, so it's still about 5
 *    minutes slower than before ALSs: ie "a bit s__t", just less s__t.
 * 2. Release 6.30.067 2020-05-25 10:12:07
 *    => DiufSudoku_V6_30.067.2020-05-25.7z
 * 3. What I should do next is fix my bugs: ie identify all invalid(hint) from
 *    HdkAlsXyWing and HdkAlsXyChain and work-out how/why they're invalid and
 *    bloody-well fix the bloody bugs! Wish me luck. I'll need it. This s__t is
 *    seriously complicated, which is why I came-up with my invalid(hint) hack.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.068 2020-05-26 10:16:08</b> HdkAlsXyChain and HdkAlsXyWing now
 * both use clean to remove invalid eliminations and skip if redPots isEmpty.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      35,597,461 120289        295 677110             52 Naked Single
 *      21,887,978  52578        416 172730            126 Hidden Single
 *      75,928,861  35305      2,150  25863          2,935 Point and Claim
 *      88,808,817  23130      3,839   6446         13,777 Naked Pair
 *      81,596,628  20989      3,887   8426          9,683 Hidden Pair
 *     147,515,162  18844      7,828   1656         89,079 Naked Triple
 *     135,429,856  18401      7,359    995        136,110 Hidden Triple
 *      57,270,755  18207      3,145    680         84,221 Swampfish
 *      84,135,545  17933      4,691    321        262,104 Swordfish
 *     116,008,827  17843      6,501    690        168,128 XY-Wing
 *      85,464,174  17360      4,923    335        255,116 XYZ-Wing
 *     226,773,446  17044     13,305    598        379,219 W-Wing
 *      98,986,467  16635      5,950    823        120,275 Skyscraper
 *      53,393,623  16215      3,292    737         72,447 Two String Kite
 *      67,458,440  15478      4,358    459        146,968 Empty Rectangle
 *     183,915,512  15019     12,245     97      1,896,036 Naked Quad
 *      21,950,986  15000      1,463      9      2,438,998 Jellyfish
 *   1,767,812,000  14997    117,877    874      2,022,668 Unique Rectangle
 *     202,623,460  14587     13,890    409        495,411 Finned Swampfish
 *     623,091,684  14242     43,750    389      1,601,778 Finned Swordfish
 *   1,164,885,395  13945     83,534     64     18,201,334 Finned Jellyfish
 *     163,320,030  13892     11,756     55      2,969,455 Sashimi Swampfish
 *     175,255,905  13842     12,661     40      4,381,397 Sashimi Swordfish
 *     244,893,035  13810     17,733      6     40,815,505 Sashimi Jellyfish
 *  99,329,116,219  13804  7,195,676   8441     11,767,458 ALS-XZ
 *  22,113,682,919   8172  2,706,030   4306      5,135,551 ALS-XY-Wing
 *   4,831,635,516   4576  1,055,864    175     27,609,345 ALS-Chain
 *   1,888,892,899   4422    427,158     20     94,444,644 Aligned Triple
 *  25,611,718,031   4403  5,816,878     63    406,535,206 Aligned Quad
 *  17,632,326,155   4353  4,050,614      7  2,518,903,736 Aligned Pent
 *  28,971,472,577   4348  6,663,172      2 14,485,736,288 Aligned Hex
 *  29,816,676,162   4346  6,860,717      5  5,963,335,232 Aligned Sept
 *  23,722,674,771   4342  5,463,536      2 11,861,337,385 Aligned Nona
 *  13,444,754,657   4340  3,097,869   2256      5,959,554 Unary Chain
 *   4,361,242,752   3493  1,248,566    340     12,827,184 Nishio Chain
 *   9,467,703,218   3243  2,919,427   5334      1,774,972 Multiple Chain
 *  13,162,043,794   1311 10,039,697   8184      1,608,265 Dynamic Chain
 *     130,930,901      3 43,643,633     30      4,364,363 Dynamic Plus
 * 300,408,874,618
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  735,297,295,400 (12:15)  501,909,416
 * NOTES:
 * 1. 12:15 is about 15 seconds SLOWER than previous. It's all crap Ray.
 * 2. Release 6.30.068 2020-05-26 10:16:08
 *    => DiufSudoku_V6_30.068.2020-05-26.7z
 * 3. Next I create test-cases for HdkAlsXyChain and HdkAlsXyWing. Then I don't
 *    know... so I'll play some Sudokus and see if I can find bugs to fix.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.070 2020-06-05 09:32:10</b> Had to revert to the last release
 * DiufSudoku_V6_30.069.2020-05-26.7z because ALS-XY-Wing went nuts and I was
 * too stupid to work-out why it went nuts, so I revert, and redo my changes.
 * <pre>
 * 1. Fixed ALS-Chain bug: ignore ALS whose super/subset already in chain.
 *    Fixed ALS-XY-Wing bug: bad blues.
 * 2. No timings for this release, coz it's only a intermediate step.
 * 3. Next redo DoubleLinked ALS-XZ hint changes, to get back to now. Sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.071 2020-06-05 20:09:11</b> Eradicated ALL invalid hints from
 * HdkAlsXyChain by ignoring ALSs which contain a cell that is involved in any
 * actual Locked Set (a naked pair/triple/etc) in this region.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      34,401,500 120344        285 676940             50 Naked Single
 *      21,428,100  52650        406 172360            124 Hidden Single
 *      73,967,700  35414      2,088  25836          2,862 Point and Claim
 *      90,653,000  23247      3,899   6429         14,100 Naked Pair
 *      78,489,700  21117      3,716   8440          9,299 Hidden Pair
 *     149,521,600  18967      7,883   1666         89,748 Naked Triple
 *     133,911,800  18518      7,231   1006        133,113 Hidden Triple
 *      56,368,600  18320      3,076    674         83,632 Swampfish
 *      85,938,300  18048      4,761    323        266,062 Swordfish
 *     111,757,500  17954      6,224    687        162,674 XY-Wing
 *      81,178,600  17472      4,646    340        238,760 XYZ-Wing
 *     217,578,400  17151     12,686    586        371,294 W-Wing
 *      92,867,700  16746      5,545    836        111,085 Skyscraper
 *      50,165,500  16317      3,074    745         67,336 Two String Kite
 *      67,064,300  15572      4,306    459        146,109 Empty Rectangle
 *     184,733,700  15113     12,223    102      1,811,114 Naked Quad
 *      21,642,700  15092      1,434      8      2,705,337 Jellyfish
 *   2,010,148,800  15090    133,210    872      2,305,216 Unique Rectangle
 *     209,701,400  14682     14,282    410        511,466 Finned Swampfish
 *     629,303,200  14336     43,896    379      1,660,430 Finned Swordfish
 *   1,184,030,900  14045     84,302     68     17,412,219 Finned Jellyfish
 *     167,033,200  13988     11,941     56      2,982,735 Sashimi Swampfish
 *     180,142,600  13937     12,925     43      4,189,362 Sashimi Swordfish
 *     244,036,800  13902     17,554      6     40,672,800 Sashimi Jellyfish
 * 100,206,222,800  13896  7,211,155   8470     11,830,722 ALS-XZ
 *  23,595,519,500   8246  2,861,450   4368      5,401,904 ALS-XY-Wing
 *   5,314,685,000   4605  1,154,111     56     94,905,089 ALS-Chain
 *   1,952,073,000   4568    427,336     15    130,138,200 Aligned Triple
 *  27,223,476,200   4554  5,977,926     63    432,118,669 Aligned Quad
 *  17,652,842,500   4504  3,919,370      7  2,521,834,642 Aligned Pent
 *  27,420,641,600   4499  6,094,830      2 13,710,320,800 Aligned Hex
 *  30,858,171,700   4497  6,861,946      5  6,171,634,340 Aligned Sept
 *  23,382,642,600   4493  5,204,238      2 11,691,321,300 Aligned Nona
 *  14,047,364,600   4491  3,127,892   2406      5,838,472 Unary Chain
 *   4,476,888,500   3601  1,243,234    346     12,938,984 Nishio Chain
 *   9,703,915,800   3345  2,901,021   5755      1,686,171 Multiple Chain
 *  13,004,770,100   1325  9,814,920   8252      1,575,953 Dynamic Chain
 *     117,119,100      3 39,039,700     30      3,903,970 Dynamic Plus
 * 305,132,398,600
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   744,434,364,300 (12:24)       508,146,323
 * NOTES:
 * 1. Fixed ALS-Chain bug: ignore ALS containing any cell that is involved in
 *    any Locked Set in his "home" region. Redone DoubleLinked ALS-XZ hints.
 * 2. It's about 12 seconds slower than previous, which I think is acceptable
 *    for the complexity of finding all LockedSets and seeing if any ALS cell
 *    is involved in any locked set.
 * 3. Release 6.30.071 2020-06-05 20:09:11
 *    => DiufSudoku_V6_30.070.2020-06-05.7z
 * 4. Next , to get back to now. Sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.072 2020-06-15 12:47:12</b> eradicated the invalid method by
 * making chains ignore cells which are part of an actual Locked Set in this
 * region. Also merged the AlsXy hinter into the (HoDoKu) alshdk package, then
 * deleted the whole (diuf) als package.
 * [2020-09-23 renamed alshdk to diuf.sudoku.solver.hinters.als]
 * A few changes to hint text. Cleaned-up unused code in Grid and Idx, and need
 * to continue this search in all the other classes.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      34,077,800 120454        282 677160             50 Naked Single
 *      26,333,900  52738        499 172180            152 Hidden Single
 *      79,140,900  35520      2,228  25832          3,063 Point and Claim
 *      88,711,400  23366      3,796   6450         13,753 Naked Pair
 *      81,997,400  21225      3,863   8437          9,718 Hidden Pair
 *     149,752,000  19080      7,848   1632         91,759 Naked Triple
 *     140,749,400  18639      7,551   1008        139,632 Hidden Triple
 *      59,064,600  18440      3,203    676         87,373 Swampfish
 *      90,530,500  18166      4,983    320        282,907 Swordfish
 *     116,922,500  18073      6,469    660        177,155 XY-Wing
 *      85,725,800  17602      4,870    335        255,897 XYZ-Wing
 *     134,875,300  17285      7,803    578        233,348 W-Wing
 *     117,261,700  16887      6,943    837        140,097 Skyscraper
 *      54,320,100  16458      3,300    745         72,912 Two String Kite
 *      70,970,000  15713      4,516    459        154,618 Empty Rectangle
 *     188,901,700  15254     12,383    102      1,851,977 Naked Quad
 *      23,152,200  15233      1,519      8      2,894,025 Jellyfish
 *   1,831,075,300  15231    120,220    864      2,119,300 Unique Rectangle
 *     210,997,800  14827     14,230    411        513,376 Finned Swampfish
 *     643,852,400  14480     44,464    382      1,685,477 Finned Swordfish
 *   1,215,978,600  14186     85,716     69     17,622,878 Finned Jellyfish
 *     198,190,200  14128     14,028     57      3,477,021 Sashimi Swampfish
 *     210,750,700  14076     14,972     43      4,901,179 Sashimi Swordfish
 *     254,469,300  14041     18,123      6     42,411,550 Sashimi Jellyfish
 *  21,997,857,300  14035  1,567,357   8477      2,595,004 ALS-XZ
 *  24,157,097,200   8260  2,924,588   4367      5,531,737 ALS-Wing
 *   4,894,702,400   4620  1,059,459     50     97,894,048 ALS-Chain
 *   2,033,972,400   4582    443,904     15    135,598,160 Aligned Triple
 *  29,343,482,300   4568  6,423,704     62    473,281,972 Aligned Quad
 * 104,452,478,800   4519 23,114,069     78  1,339,134,343 Aligned Pent
 *  25,015,697,000   4452  5,618,979      2 12,507,848,500 Aligned Hex
 *  30,161,864,300   4450  6,777,947      5  6,032,372,860 Aligned Sept
 *  27,508,653,600   4446  6,187,281      1 27,508,653,600 Aligned Oct
 *  23,838,414,800   4445  5,362,972      1 23,838,414,800 Aligned Nona
 *  14,943,247,400   4444  3,362,566   2399      6,228,948 Unary Chain
 *   4,523,059,300   3568  1,267,673    345     13,110,316 Nishio Chain
 *   9,758,332,400   3313  2,945,467   5718      1,706,598 Multiple Chain
 *  13,499,469,900   1321 10,219,129   8212      1,643,871 Dynamic Chain
 *     126,794,200      3 42,264,733     30      4,226,473 Dynamic Plus
 * 342,362,924,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  776,968,987,200 (12:56)  530,354,257
 * NOTES:
 * 1. 12:56 is about 30 seconds slower than previous, which is rather
 *    disappointing because I expected it to be a bit faster. Sigh.
 * 2. Release 6.30.072 2020-06-15 12:47:12
 *    => DiufSudoku_V6_30.072.2020-06-15.7z
 * 3. Next I need to find and remove all unused code. There is rather a lot of
 *    it... a side-effect of constantly hackin the s__t out of s__t.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.073 2020-06-22 22:17:13</b> Improved GUI after Sue test.
 * Locking now detects Siamese hints and defers to Hidden Pair/Triple,
 * and then removes each hint whose redPots is a subset of anothers. In the GUI
 * only, the new HomeAlone hinter detects lonely naked singles first and sets
 * them before other naked singles. I should workout how to disable it in the
 * LogicalSolverTester because it will never find anything, coz it exits early.
 * Also the GUI now flashes pink when it detects a busted grid. Also cleaned-up
 * Settings, and re-ordered the code in SudokuFrame. It was a dogs breakfast.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      15,966,500 119789        133      0              0 Lonely Naked Single
 *      36,379,200 119789        303 677550             53 Naked Single
 *      20,515,200  52034        394 171850            119 Hidden Single
 *      89,157,300  34849      2,558  24520          3,636 Point and Claim
 *      90,789,100  23385      3,882   8058         11,266 Naked Pair
 *      80,037,100  21239      3,768   8436          9,487 Hidden Pair
 *     150,803,800  19092      7,898   1701         88,655 Naked Triple
 *     142,712,700  18656      7,649   1011        141,159 Hidden Triple
 *      59,579,000  18454      3,228    680         87,616 Swampfish
 *      87,928,400  18178      4,837    314        280,026 Swordfish
 *     119,407,500  18088      6,601    657        181,746 XY-Wing
 *      88,796,700  17619      5,039    333        266,656 XYZ-Wing
 *     138,016,100  17304      7,975    577        239,196 W-Wing
 *     119,894,300  16907      7,091    840        142,731 Skyscraper
 *      55,756,200  16476      3,384    743         75,041 Two String Kite
 *      69,712,800  15733      4,430    459        151,879 Empty Rectangle
 *     195,128,500  15274     12,775    102      1,913,024 Naked Quad
 *      22,514,300  15253      1,476      8      2,814,287 Jellyfish
 *   2,125,224,700  15251    139,349    868      2,448,415 Unique Rectangle
 *     213,369,100  14845     14,373    409        521,684 Finned Swampfish
 *     650,651,200  14500     44,872    381      1,707,745 Finned Swordfish
 *   1,218,291,200  14207     85,752     69     17,656,394 Finned Jellyfish
 *     172,239,100  14149     12,173     57      3,021,738 Sashimi Swampfish
 *     186,405,500  14097     13,223     43      4,335,011 Sashimi Swordfish
 *     254,305,400  14062     18,084      6     42,384,233 Sashimi Jellyfish
 *  21,817,933,800  14056  1,552,214   8456      2,580,171 ALS-XZ
 *  23,680,812,700   8295  2,854,829   4366      5,423,914 ALS-Wing
 *   4,792,859,400   4654  1,029,836    119     40,276,129 ALS-Chain
 *   2,045,821,700   4553    449,334     14    146,130,121 Aligned Triple
 *  27,384,985,600   4540  6,031,935     62    441,693,316 Aligned Quad
 * 107,575,964,100   4491 23,953,677     75  1,434,346,188 Aligned Pent
 *  26,450,599,000   4427  5,974,836      2 13,225,299,500 Aligned Hex
 *  32,200,485,500   4425  7,276,945      5  6,440,097,100 Aligned Sept
 *  27,442,129,700   4421  6,207,222      1 27,442,129,700 Aligned Oct
 *  21,909,119,100   4420  4,956,814      1 21,909,119,100 Aligned Nona
 *  14,445,221,500   4419  3,268,889   2243      6,440,134 Unary Chain
 *   4,555,485,500   3570  1,276,046    349     13,052,967 Nishio Chain
 *  10,138,764,800   3311  3,062,145   5728      1,770,035 Multiple Chain
 *  13,986,168,200   1327 10,539,689   8272      1,690,784 Dynamic Chain
 *     127,591,400      3 42,530,466     30      4,253,046 Dynamic Plus
 * 344,957,522,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   782,100,470,800 (13:02)       533,856,976
 * NOTES:
 * 1. 12:02 is about 6 seconds slower than previous, which is disappointing.
 * 2. Release 6.30.073 2020-06-22 22:17:13
 *    => DiufSudoku_V6_30.073.2020-06-22.7z
 * 3. Next I really don't know. I keep thinking I should have a look at Mutant
 *    Fish... so I'll do that, but I'm not real hopeful. I suspect they'll be
 *    a bigger waste of time than they're worth, but my predictive capacity is
 *    prodigously weak, so I shall look, at the very least.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.074 2020-06-27 14:12:14</b> I'm releasing because it's time to.
 * Fixed a few bugs. Ctrl-L now works properly, but valid does a brute-force
 * solve every time HdkAlsXyWing & HdkAlsXyChain hint. I'm getting close the
 * point where valid is no longer required, so I'm not too worried if it's a
 * bit slow Redge. Test-cases now all pass.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      32,372,785 119745        270 677770             47 Naked Single
 *      20,889,059  51968        401 171460            121 Hidden Single
 *      87,582,449  34822      2,515  24516          3,572 Point and Claim
 *      91,591,064  23364      3,920   8049         11,379 Naked Pair
 *      80,927,317  21224      3,813   8440          9,588 Hidden Pair
 *     160,346,484  19075      8,406   1706         93,989 Naked Triple
 *     143,702,478  18637      7,710   1011        142,138 Hidden Triple
 *      57,863,981  18437      3,138    676         85,597 Swampfish
 *      87,723,169  18163      4,829    320        274,134 Swordfish
 *     120,984,155  18070      6,695    659        183,587 XY-Wing
 *      89,427,796  17600      5,081    334        267,747 XYZ-Wing
 *     137,667,631  17284      7,965    577        238,592 W-Wing
 *     118,073,701  16887      6,991    836        141,236 Skyscraper
 *      58,341,745  16459      3,544    743         78,521 Two String Kite
 *      69,482,343  15716      4,421    460        151,048 Empty Rectangle
 *     206,517,313  15256     13,536    102      2,024,679 Naked Quad
 *      22,775,756  15235      1,494      8      2,846,969 Jellyfish
 *   1,805,585,698  15233    118,531    864      2,089,798 Unique Rectangle
 *     212,508,088  14829     14,330    411        517,051 Finned Swampfish
 *     642,073,983  14482     44,336    382      1,680,821 Finned Swordfish
 *   1,227,569,067  14188     86,521     69     17,790,856 Finned Jellyfish
 *     172,890,497  14130     12,235     57      3,033,166 Sashimi Swampfish
 *     185,159,404  14078     13,152     43      4,306,032 Sashimi Swordfish
 *     255,348,821  14043     18,183      6     42,558,136 Sashimi Jellyfish
 *  22,770,759,588  14037  1,622,195   8458      2,692,215 ALS-XZ
 *  35,897,750,326   8268  4,341,769   4367      8,220,231 ALS-Wing
 *   4,877,594,446   4628  1,053,931     30    162,586,481 ALS-Chain
 *   2,056,005,027   4603    446,666     15    137,067,001 Aligned Triple
 *  26,855,849,446   4589  5,852,222     62    433,158,862 Aligned Quad
 * 104,631,843,594   4540 23,046,661     78  1,341,433,892 Aligned Pent
 *  28,618,341,262   4473  6,398,019      2 14,309,170,631 Aligned Hex
 *  30,138,309,740   4471  6,740,843      5  6,027,661,948 Aligned Sept
 *  27,091,566,965   4467  6,064,823      1 27,091,566,965 Aligned Oct
 *  21,977,812,250   4466  4,921,140      1 21,977,812,250 Aligned Nona
 *  14,119,227,209   4465  3,162,200   2447      5,770,015 Unary Chain
 *   4,470,132,189   3577  1,249,687    345     12,956,904 Nishio Chain
 *   9,684,176,944   3322  2,915,164   5750      1,684,204 Multiple Chain
 *  13,271,362,998   1325 10,016,123   8252      1,608,260 Dynamic Chain
 *     140,292,600      3 46,764,200     30      4,676,420 Dynamic Plus
 * 352,688,429,368
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  784,889,782,700 (13:04)  535,760,943
 * NOTES:
 * 1. 13:04 is about a minute slower than previous, because HdkAlsXyWing and
 *    HdkAlsXyChain both run brute-force solve whenever they hint.
 * 2. Release 6.30.074 2020-06-27 14:12:14
 *    => DiufSudoku_V6_30.074.2020-06-27.7z
 * 3. Next I still don't know. I looked at Mutant (and Franked) Fish, but gave
 *    up. It's too complex for me. I'll think of something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.075 2020-06-29 21:35:15</b> Re-profiled. Made Aligned*Exclusion
 * et al a bit faster by expediting at(Cell[] array, int[] idx) with a bit of
 * bit-twiddling which I then promulgated back into Idx.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      33,842,700 119745        282 677770             49 Naked Single
 *      20,229,600  51968        389 171460            117 Hidden Single
 *      75,999,400  34822      2,182  24516          3,099 Point and Claim
 *      89,738,400  23364      3,840   8049         11,149 Naked Pair
 *      77,134,300  21224      3,634   8440          9,139 Hidden Pair
 *     161,178,200  19075      8,449   1706         94,477 Naked Triple
 *     139,503,800  18637      7,485   1011        137,985 Hidden Triple
 *      58,787,600  18437      3,188    676         86,963 Swampfish
 *      87,879,100  18163      4,838    320        274,622 Swordfish
 *     118,585,600  18070      6,562    659        179,947 XY-Wing
 *      89,638,800  17600      5,093    334        268,379 XYZ-Wing
 *     135,646,300  17284      7,848    577        235,088 W-Wing
 *     116,375,800  16887      6,891    836        139,205 Skyscraper
 *      57,337,300  16459      3,483    743         77,169 Two String Kite
 *      75,435,600  15716      4,799    460        163,990 Empty Rectangle
 *     204,093,500  15256     13,377    102      2,000,916 Naked Quad
 *      22,741,800  15235      1,492      8      2,842,725 Jellyfish
 *   1,836,316,700  15233    120,548    864      2,125,366 Unique Rectangle
 *     208,022,400  14829     14,028    411        506,137 Finned Swampfish
 *     635,810,500  14482     43,903    382      1,664,425 Finned Swordfish
 *   1,191,586,900  14188     83,985     69     17,269,375 Finned Jellyfish
 *     172,488,800  14130     12,207     57      3,026,119 Sashimi Swampfish
 *     185,845,600  14078     13,201     43      4,321,990 Sashimi Swordfish
 *     261,320,600  14043     18,608      6     43,553,433 Sashimi Jellyfish
 *  21,276,166,300  14037  1,515,720   8458      2,515,507 ALS-XZ
 *  23,699,708,500   8268  2,866,437   4367      5,426,999 ALS-Wing
 *   4,542,533,800   4628    981,532     30    151,417,793 ALS-Chain
 *   2,011,756,700   4603    437,053     15    134,117,113 Aligned Triple
 *  28,279,257,000   4589  6,162,400     62    456,117,048 Aligned Quad
 * 103,908,161,600   4540 22,887,260     78  1,332,155,917 Aligned Pent
 *  24,943,821,500   4473  5,576,530      2 12,471,910,750 Aligned Hex
 *  30,339,327,400   4471  6,785,803      5  6,067,865,480 Aligned Sept
 *  26,492,839,400   4467  5,930,790      1 26,492,839,400 Aligned Oct
 *  23,542,345,500   4466  5,271,461      1 23,542,345,500 Aligned Nona
 *  14,168,849,800   4465  3,173,314   2447      5,790,294 Unary Chain
 *   4,492,775,000   3577  1,256,017    345     13,022,536 Nishio Chain
 *   9,707,822,200   3322  2,922,282   5750      1,688,316 Multiple Chain
 *  13,453,829,600   1325 10,153,833   8252      1,630,371 Dynamic Chain
 *     124,123,000      3 41,374,333     30      4,137,433 Dynamic Plus
 * 337,038,856,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  756,785,077,500 (12:36)  516,576,844
 * NOTES:
 * 1. 12:36 is about a 30 seconds faster than previous.
 * 2. Release 6.30.075 2020-06-29 21:35:15
 *    => DiufSudoku_V6_30.075.2020-06-29.7z
 * 3. Next I'm still scratching me 'ead.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.076 2020-07-01 19:45:16</b> Profiler performance tuning. Faster
 * build siblings in Grid constructor in BruteForce, also reduces memory usage.
 * Iterate Indexes.ARRAYS in region chains. Also some consistent naming.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      19,401,500 119709        162 677650          28 Naked Single
 *      13,538,900  51944        260 171470          78 Hidden Single
 *      49,628,300  34797      1,426  24511       2,024 Point and Claim
 *      58,401,900  23341      2,502   8050       7,254 Naked Pair
 *      51,871,100  21199      2,446   8439       6,146 Hidden Pair
 *     131,438,400  19053      6,898   1710      76,864 Naked Triple
 *     121,987,600  18615      6,553   1012     120,541 Hidden Triple
 *      39,952,900  18415      2,169    681      58,667 Swampfish
 *      76,653,100  18138      4,226    320     239,540 Swordfish
 *      92,785,700  18045      5,141    655     141,657 XY-Wing
 *      64,705,200  17574      3,681    337     192,003 XYZ-Wing
 *     100,947,100  17255      5,850    581     173,747 W-Wing
 *      93,527,400  16857      5,548    838     111,607 Skyscraper
 *      49,195,200  16428      2,994    739      66,569 Two String Kite
 *      57,758,100  15689      3,681    460     125,561 Empty Rectangle
 *     167,712,200  15229     11,012    102   1,644,237 Naked Quad
 *      19,951,100  15208      1,311      8   2,493,887 Jellyfish
 *   1,516,510,200  15206     99,731    864   1,755,220 Unique Rectangle
 *     186,096,500  14802     12,572    410     453,893 Finned Swampfish
 *     582,054,300  14456     40,263    381   1,527,701 Finned Swordfish
 *   1,091,315,600  14163     77,053     68  16,048,758 Finned Jellyfish
 *     158,926,700  14106     11,266     55   2,889,576 Sashimi Swampfish
 *     167,412,800  14056     11,910     43   3,893,320 Sashimi Swordfish
 *     234,376,000  14021     16,716      6  39,062,666 Sashimi Jellyfish
 *  19,955,244,200  14015  1,423,849   8491   2,350,164 ALS-XZ
 *  22,754,843,600   8235  2,763,186   4359   5,220,198 ALS-Wing
 *     282,448,900   4602     61,375      1 282,448,900 Aligned Pair
 *   1,836,628,300   4601    399,180     16 114,789,268 Aligned Triple
 *  27,321,472,500   4586  5,957,582     62 440,668,911 Aligned Quad
 *  13,867,796,900   4537  3,056,600   2518   5,507,465 Unary Chain
 *   4,233,782,600   3617  1,170,523    345  12,271,833 Nishio Chain
 *   8,980,730,600   3362  2,671,246   5801   1,548,134 Multiple Chain
 *  12,371,830,600   1332  9,288,161   8304   1,489,863 Dynamic Chain
 *     117,534,800      3 39,178,266     30   3,917,826 Dynamic Plus
 * 116,868,460,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  160,593,284,700 (02:40)  109,619,989
 * NOTES:
 * 1. 02:40 but don't get over excited. I dropped A5..10E completely to see how
 *    it'd go. I was hoping for under 2 minutes with ALS-XZ and ALS-XY-Wing
 *    (but no ALS-XY-Chain).
 * 2. Release 6.30.076 2020-07-01 19:45:16
 *    => DiufSudoku_V6_30.076.2020-07-01.7z
 * 3. Next I'm still scratching me 'ead. More profiling I guess.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.077 2020-07-10 17:24:17</b> Using HoDoKu's "complex" Fish hinter
 * from Sudoku Explainer turned out to be pretty easy. Making it run faster is
 * the difficult part. It's really really really hard! Seriously complex s__t.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      21,983,826 119877        183 679070            32 Naked Single
 *      16,080,886  51970        309 171480            93 Hidden Single
 *      49,943,360  34822      1,434  24526         2,036 Point and Claim
 *      65,146,758  23323      2,793   8155         7,988 Naked Pair
 *      52,132,701  21164      2,463   8464         6,159 Hidden Pair
 *     134,786,512  19002      7,093   1701        79,239 Naked Triple
 *     122,806,076  18560      6,616   1005       122,195 Hidden Triple
 *      41,441,469  18358      2,257    685        60,498 Swampfish
 *      77,090,737  18074      4,265    323       238,671 Swordfish
 *      93,942,394  17981      5,224    676       138,968 XY-Wing
 *      63,098,022  17501      3,605    340       185,582 XYZ-Wing
 *     102,998,344  17180      5,995    570       180,698 W-Wing
 *      97,275,448  16788      5,794    784       124,075 Skyscraper
 *      49,621,512  16396      3,026    725        68,443 Two String Kite
 *      57,448,763  15671      3,665    462       124,347 Empty Rectangle
 *     165,706,412  15209     10,895    101     1,640,657 Naked Quad
 *      20,025,006  15189      1,318      9     2,225,000 Jellyfish
 *   1,683,979,498  15186    110,890    874     1,926,749 Unique Rectangle
 *     189,704,430  14777     12,837    397       477,844 Finned Swampfish
 *     584,868,996  14444     40,492    363     1,611,209 Finned Swordfish
 *   1,087,201,230  14165     76,752     62    17,535,503 Finned Jellyfish
 *     156,062,530  14113     11,058     58     2,690,733 Sashimi Swampfish
 *     168,266,069  14060     11,967     35     4,807,601 Sashimi Swordfish
 *     230,651,838  14031     16,438      6    38,441,973 Sashimi Jellyfish
 *   8,830,947,406  14025    629,657     23   383,954,235 Franken Swampfish
 *  10,156,604,866  14002    725,368    466    21,795,289 Franken Swordfish
 *  15,127,298,602  13652  1,108,064    164    92,239,625 Franken Jellyfish
 *  68,622,649,405  13509  5,079,772     40 1,715,566,235 Mutant Jellyfish
 *  19,822,472,104  13476  1,470,946   8365     2,369,691 ALS-XZ
 *  21,711,311,455   7785  2,788,864   4174     5,201,560 ALS-Wing
 *   7,458,476,256   4311  1,730,103    434    17,185,429 ALS-Chain
 *     250,693,786   3967     63,194      1   250,693,786 Aligned Pair
 *   1,667,493,027   3966    420,447     14   119,106,644 Aligned Triple
 *  25,510,752,637   3953  6,453,516     57   447,557,063 Aligned Quad
 *  16,640,844,809   3907  4,259,238      4 4,160,211,202 Aligned Pent
 *  22,649,493,706   3904  5,801,612      3 7,549,831,235 Aligned Hex
 *  22,242,226,007   3901  5,701,672      4 5,560,556,501 Aligned Sept
 *  12,020,975,087   3898  3,083,882   1727     6,960,610 Unary Chain
 *   4,010,523,833   3207  1,250,553     18   222,806,879 Nishio Chain
 *   8,919,971,576   3189  2,797,106   5237     1,703,259 Multiple Chain
 *  12,569,562,012   1291  9,736,299   8029     1,565,520 Dynamic Chain
 *     127,300,301      3 42,433,433     30     4,243,343 Dynamic Plus
 * 283,671,859,692
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  329,761,375,901 (05:29)  225,093,089
 * NOTES:
 * 1. 05:29 is 3 MINUTES slower but yet get a bit of that on the big jobs. It's
 *    seriously complex, highly abstracted code, so making it faster is very
 *    challenging. I'm releasing so I can clean-up the old logs.
 * 2. Release 6.30.077 2020-07-10 17:24:17
 *    => DiufSudoku_V6_30.077.2020-07-10.7z
 * 3. Next I keep trying to make hobiwans AlsSolver more performant.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.078 2020-07-20 11:42:18</b> Forgive me father it's been 10 days since
 * my last release and I just need to rub one out. Actually I need to delete my
 * log-files and I want a backup before I do so. I've been pissin about trying
 * to make HoDoKu faster, with some (but not much) success. The main snags are
 * repeated calls to "heavy" getters and Integer.compareTo(Integer). Sigh.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      20,627,400 119187        173 681060          30 Naked Single
 *      16,728,900  51081        327 172910          96 Hidden Single
 *      49,832,200  33790      1,474  24181       2,060 Point and Claim
 *      56,666,300  22474      2,521   8163       6,941 Naked Pair
 *      50,067,900  20288      2,467   8423       5,944 Hidden Pair
 *     124,936,900  18138      6,888   1671      74,767 Naked Triple
 *     115,435,400  17708      6,518    992     116,366 Hidden Triple
 *      41,097,600  17509      2,347    638      64,416 Swampfish
 *      75,038,600  17247      4,350    300     250,128 Swordfish
 *      90,964,900  17158      5,301    684     132,989 XY-Wing
 *      62,565,600  16678      3,751    337     185,654 XYZ-Wing
 *      97,814,800  16360      5,978    534     183,173 W-Wing
 *      94,446,200  15988      5,907    776     121,709 Skyscraper
 *      47,929,200  15594      3,073    710      67,505 Two String Kite
 *      56,047,600  14884      3,765    440     127,380 Empty Rectangle
 *     155,754,400  14444     10,783     98   1,589,330 Naked Quad
 *      19,061,200  14425      1,321     10   1,906,120 Jellyfish
 *   1,505,721,300  14421    104,411    864   1,742,732 Unique Rectangle
 *     179,732,500  14015     12,824    364     493,770 Finned Swampfish
 *     548,971,100  13710     40,041    342   1,605,178 Finned Swordfish
 *   1,027,707,100  13443     76,449     59  17,418,764 Finned Jellyfish
 *     146,042,500  13393     10,904     50   2,920,850 Sashimi Swampfish
 *     160,158,700  13348     11,998     30   5,338,623 Sashimi Swordfish
 *     222,964,200  13324     16,734      6  37,160,700 Sashimi Jellyfish
 *   8,215,081,100  13318    616,840     20 410,754,055 Franken Swampfish
 *   9,185,100,500  13298    690,712    392  23,431,378 Franken Swordfish
 *  11,201,141,800  13002    861,493    166  67,476,757 Franken Jellyfish
 *   8,261,435,100  12855    642,663     10 826,143,510 Mutant Swordfish
 *  18,824,206,600  12848  1,465,146     31 607,232,470 Mutant Jellyfish
 *  20,153,916,800  12821  1,571,945   8190   2,460,795 ALS-XZ
 *  18,938,496,600   7223  2,621,971   4035   4,693,555 ALS-Wing
 *   7,201,261,500   3871  1,860,310    429  16,786,157 ALS-Chain
 *  53,914,544,000   3529 15,277,569   3895  13,841,988 Kraken Swampfish
 *   1,108,593,200   2612    424,423      8 138,574,150 Aligned Triple
 *  16,341,832,200   2605  6,273,256     25 653,673,288 Aligned Quad
 *   7,327,477,800   2586  2,833,518    533  13,747,613 Unary Chain
 *   2,826,430,700   2370  1,192,586      3 942,143,566 Nishio Chain
 *   6,182,209,000   2367  2,611,833   2698   2,291,404 Multiple Chain
 *  11,012,726,900   1208  9,116,495   7352   1,497,922 Dynamic Chain
 *     175,414,700      3 58,471,566     30   5,847,156 Dynamic Plus
 * 205,836,181,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  251,749,806,900 (04:11)  171,842,871
 * NOTES:
 * 1. 04:11 is a minute faster, which is nice, but don't get too excited: I've
 *    dropped the non-performant hinters to get it down that far. I've been
 *    working mainly on Kraken Swampfish. I release to back-up and delete logs.
 * 2. Release 6.30.078 2020-07-20 11:42:18
 *    => DiufSudoku_V6_30.078.2020-07-20.7z
 * 3. Next I keep trying to make hobiwans FishSolver faster, and have a chook
 *    at the DeathBlossom.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.079 2020-07-21 12:26:19</b> profile to expedite getAllKrakenFishes->
 * TablingSolver.buildChain-> TreeMap<Int,Int>-> Int.of -> CACHE_SIZE
 * (4K -> 75% hit rate) but now there's s__tloads of logs so I release to back
 * and clean them up.
 * <p>
 * <b>BROKEN:</b> I discovered that my desktop "release" SudokuExplainer doesn't
 * run: HoDoKu ClassNotFound, so can only run in situ now. I need a real
 * programmer to workout what's happened to the classpath. Blame Nutbeans!
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      19,947,100 119400        167 680640          29 Naked Single
 *      15,009,300  51336        292 173320          86 Hidden Single
 *      47,370,400  34004      1,393  24263       1,952 Point and Claim
 *      56,035,100  22636      2,475   8075       6,939 Naked Pair
 *      48,737,500  20469      2,381   8409       5,795 Hidden Pair
 *     125,914,100  18325      6,871   1669      75,442 Naked Triple
 *     115,780,200  17896      6,469    985     117,543 Hidden Triple
 *      58,263,200  17700      3,291   1350      43,157 Two String Kite
 *      38,590,500  16350      2,360    436      88,510 Swampfish
 *      84,499,700  16160      5,228    633     133,490 XY-Wing
 *      57,842,400  15715      3,680    306     189,027 XYZ-Wing
 *      89,832,000  15427      5,823    435     206,510 W-Wing
 *      81,255,700  15128      5,371    346     234,843 Skyscraper
 *      52,437,800  14945      3,508    458     114,493 Empty Rectangle
 *      64,666,000  14487      4,463    235     275,174 Swordfish
 *     156,613,600  14418     10,862     98   1,598,097 Naked Quad
 *      17,980,900  14399      1,248      9   1,997,877 Jellyfish
 *   1,472,203,300  14396    102,264    862   1,707,892 Unique Rectangle
 *     177,632,900  13991     12,696    373     476,227 Finned Swampfish
 *     544,707,000  13678     39,823    352   1,547,463 Finned Swordfish
 *   1,020,742,500  13402     76,163     68  15,010,919 Finned Jellyfish
 *     144,397,200  13345     10,820     50   2,887,944 Sashimi Swampfish
 *     156,687,700  13300     11,781     31   5,054,441 Sashimi Swordfish
 *     213,913,800  13275     16,114      5  42,782,760 Sashimi Jellyfish
 *  18,709,591,800  13270  1,409,916   8328   2,246,588 ALS-XZ
 *  17,858,920,500   7585  2,354,505   4173   4,279,635 ALS-Wing
 *   6,689,360,700   4118  1,624,419    441  15,168,618 ALS-Chain
 *   2,484,146,400   3764    659,975      5 496,829,280 Franken Swampfish
 *   2,942,083,800   3759    782,677    192  15,323,353 Franken Swordfish
 *   3,705,171,300   3617  1,024,376     90  41,168,570 Franken Jellyfish
 *   5,924,561,400   3537  1,675,024     15 394,970,760 Mutant Jellyfish
 *  51,034,726,600   3525 14,477,936   3881  13,149,890 Kraken Swampfish
 *   1,094,009,200   2609    419,321      8 136,751,150 Aligned Triple
 *  16,358,576,500   2602  6,286,924     25 654,343,060 Aligned Quad
 *   7,091,436,300   2583  2,745,426    534  13,279,843 Unary Chain
 *   6,068,156,100   2366  2,564,732   2696   2,250,799 Multiple Chain
 *  10,872,900,100   1209  8,993,300   7362   1,476,894 Dynamic Chain
 *     124,216,000      3 41,405,333     30   4,140,533 Dynamic Plus
 * 155,818,916,600
 *  pzls       total (ns) (mm:ss)    each (ns)
 *  1465  200,534,855,600 (03:20)  136,883,860
 * NOTES:
 * 1. 03:20 is nearly minute faster, which is nice, but don't get too excited:
 *    I've been pissin about with hinter selection again. I made improvements
 *    to the KRAKEN search, especially Int's CACHE_SIZE from half a K to 4K.
 * 2. Release 6.30.079 2020-07-21 12:26:19
 *    => DiufSudoku_V6_30.079.2020-07-21.7z
 * 3. Next I keep trying to make hobiwans FishSolver faster, and maybe even
 *    eventually still have a chook at the DeathBlossom.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.080 2020-08-01 16:34:20</b> I broke HoDoKu so I had to revert it.
 * This release I'll pack HoDoKu and SudokuExplainer into a single 7z. No more
 * dumb mistakes like this. It's really annoying because at one stage I had
 * it finding a couple of thousand fish hints, (I've got the logs to prove it)
 * and then I had to revert when I broke s__t I couldn't workout how to fix.
 * And now I can't get it back to it's uber-hinty state. And I'm REALY REALY
 * annoyed with myself for not doing a release and getting a backup before
 * continuing to piss-about and eventually break it. Sigh. I also need a decent
 * tool to help find the problem, instead of relying on backups too heavily.
 * Nutbeans has a history feature, but I find it useless.
 * <pre>
 *          time(ns)  calls  time/call  elims   time/elim hinter
 *        20,040,300 120288        166 678010          29 Naked Single
 *        15,874,800  52487        302 173070          91 Hidden Single
 *        49,530,600  35180      1,407  24623       2,011 Point and Claim
 *        60,533,600  23622      2,562   8051       7,518 Naked Pair
 *        52,946,200  21466      2,466   8451       6,265 Hidden Pair
 *       134,910,900  19305      6,988   1710      78,895 Naked Triple
 *       124,987,000  18864      6,625   1002     124,737 Hidden Triple
 *        57,358,300  18662      3,073   1381      41,533 Two String Kite
 *        49,632,500  17281      2,872    464     106,966 Swampfish
 *        91,562,400  17074      5,362    610     150,102 XY-Wing
 *        61,970,700  16637      3,724    301     205,882 XYZ-Wing
 *        99,737,000  16354      6,098    456     218,721 W-Wing
 *        82,820,900  16047      5,161    347     238,676 Skyscraper
 *        62,723,000  15864      3,953    484     129,592 Empty Rectangle
 *        67,967,400  15380      4,419    253     268,645 Swordfish
 *       169,093,200  15309     11,045     97   1,743,228 Naked Quad
 *        19,121,900  15291      1,250      8   2,390,237 Jellyfish
 *     1,544,612,300  15289    101,027    872   1,771,344 Unique Rectangle
 *       189,852,700  14880     12,758    404     469,932 Finned Swampfish
 *       590,182,100  14542     40,584    377   1,565,469 Finned Swordfish
 *     1,088,826,900  14252     76,398     73  14,915,436 Finned Jellyfish
 *       157,304,600  14192     11,084     54   2,913,048 Sashimi Swampfish
 *       169,817,100  14144     12,006     37   4,589,651 Sashimi Swordfish
 *       233,873,600  14113     16,571      6  38,978,933 Sashimi Jellyfish
 *    22,672,881,500  14107  1,607,207   8454   2,681,911 ALS-XZ
 *    22,720,866,100   8366  2,715,857   4343   5,231,606 ALS-Wing
 *     8,855,908,300   4753  1,863,224    456  19,420,851 ALS-Chain
 *     2,200,558,500   4391    501,152      9 244,506,500 Franken Swampfish
 *     2,873,772,300   4382    655,812    220  13,062,601 Franken Swordfish
 *     5,618,841,700   4216  1,332,742     91  61,745,513 Franken Jellyfish
 *    17,074,969,200   4134  4,130,374     20 853,748,460 Mutant Jellyfish
 *    20,269,113,200   4117  4,923,272     59 343,544,291 Kraken Swampfish
 *     1,706,092,400   4099    416,221     14 121,863,742 Aligned Triple
 *    24,569,298,800   4086  6,013,044     54 454,987,014 Aligned Quad
 *    12,116,576,200   4042  2,997,668   1733   6,991,676 Unary Chain
 *     3,973,053,800   3353  1,184,925      6 662,175,633 Nishio Chain
 *     8,666,492,500   3347  2,589,331   5243   1,652,964 Multiple Chain
 *    10,231,449,900   1407  7,271,819   7695   1,329,623 Dynamic Chain
 *       128,787,400      3 42,929,133     30   4,292,913 Dynamic Plus
 *   168,873,941,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  214,367,479,200 (03:34)  146,325,924
 * NOTES:
 * 1. 03:34 is 12 secs slower than prev, which I can account for because it's
 *    unoptimised because I had to revert HoDoKu to the zip, coz I'm a putz!
 * 2. Release 6.30.080 2020-08-01 16:34:20
 *    => DiufSudoku_V6_30.080.2020-08-01.7z (including HoDoKu from now on)
 * 3. Next I re-apply the lessons (if I can remember them) learned in the last
 *    session with the optimiser.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.081 2020-08-06 22:26:21</b> There is no try. Been pissin about trying
 * to make FishSolver faster, then tried implementing FrankenFisherman myself
 * and it totally sucks bags. It's slow and doesn't work and I feel pretty
 * stupid right now. But there was a nice tender lesso kiss on the tele and
 * that's always nice. So maybe I'll have another go at it tomorrow.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      18,985,500 120287        157 678010            28 Naked Single
 *      14,788,800  52486        281 173070            85 Hidden Single
 *      50,757,600  35179      1,442  24623         2,061 Point and Claim
 *      60,948,400  23621      2,580   8051         7,570 Naked Pair
 *      54,698,700  21465      2,548   8451         6,472 Hidden Pair
 *     133,818,800  19304      6,932   1710        78,256 Naked Triple
 *     127,273,700  18863      6,747   1002       127,019 Hidden Triple
 *      62,953,700  18661      3,373   1381        45,585 Two String Kite
 *      39,418,700  17280      2,281    464        84,954 Swampfish
 *      89,071,200  17073      5,217    610       146,018 XY-Wing
 *      62,718,300  16636      3,770    301       208,366 XYZ-Wing
 *     100,609,700  16353      6,152    456       220,635 W-Wing
 *      88,379,800  16046      5,507    347       254,696 Skyscraper
 *      54,906,800  15863      3,461    484       113,443 Empty Rectangle
 *      68,202,200  15379      4,434    253       269,573 Swordfish
 *     167,857,100  15308     10,965     97     1,730,485 Naked Quad
 *      19,088,800  15290      1,248      8     2,386,100 Jellyfish
 *   1,590,672,200  15288    104,047    872     1,824,165 Unique Rectangle
 *     185,455,300  14879     12,464    404       459,047 Finned Swampfish
 *     580,755,000  14541     39,939    377     1,540,464 Finned Swordfish
 *   1,073,715,600  14251     75,343     73    14,708,432 Finned Jellyfish
 *     146,335,000  14191     10,311     54     2,709,907 Sashimi Swampfish
 *     158,046,800  14143     11,174     37     4,271,535 Sashimi Swordfish
 *     221,185,800  14112     15,673      6    36,864,300 Sashimi Jellyfish
 *  22,076,355,300  14106  1,565,032   8454     2,611,350 ALS-XZ
 *  22,401,034,000   8365  2,677,947   4343     5,157,963 ALS-Wing
 *   8,569,280,800   4752  1,803,299    456    18,792,282 ALS-Chain
 *   1,935,616,800   4390    440,914      9   215,068,533 Franken Swampfish
 *   2,539,462,300   4381    579,653    220    11,543,010 Franken Swordfish
 *   4,946,580,900   4215  1,173,566     91    54,358,031 Franken Jellyfish
 *  13,116,919,400   4133  3,173,704     20   655,845,970 Mutant Jellyfish
 *  17,412,705,900   4116  4,230,492     62   280,850,095 Kraken Swampfish
 *   1,653,735,100   4097    403,645     14   118,123,935 Aligned Triple
 *  23,457,467,400   4084  5,743,748     54   434,397,544 Aligned Quad
 *  11,790,060,500   4040  2,918,331   1733     6,803,266 Unary Chain
 *   4,037,737,200   3351  1,204,935      4 1,009,434,300 Nishio Chain
 *   8,677,210,900   3347  2,592,533   5243     1,655,008 Multiple Chain
 *  10,585,248,600   1407  7,523,275   7695     1,375,600 Dynamic Chain
 *     135,302,800      3 45,100,933     30     4,510,093 Dynamic Plus
 * 158,505,361,400
 * pzls       total (ns)  (mm:ss)    each (ns)
 * 1465  205,301,657,000  (03:25)  140,137,649
 * NOTES:
 * 1. 03:25 is 9 secs faster than prev, and that's about as fast as I know how
 *    to make it.
 * 2. Release 6.30.081 2020-08-06 22:26:21
 *    => DiufSudoku_V6_30.081.2020-08-06.7z (including HoDoKu from now on)
 * 3. Next I don't know. Maybe nothing. I haven't made substantial advances in
 *    the war on Sudoku in months... so I really do think I'm all in. I reckon
 *    I should just piss about cleaning-up the GUI this week and finally ship
 *    it on my birthday.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.082 2020-08-10 21:42:22</b> Implemented HdkColoring. Made
 * HdkFisherman marginally faster. Standardised caching in the HintCache.
 * Dropped Nishio Chainer which took 4 seconds to produce 1 hint, and the
 * Franken Swordfish which took 21 seconds to produce 9 hints.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      18,959,500 120295        157 678570          27 Naked Single
 *      14,806,300  52438        282 172520          85 Hidden Single
 *      53,977,700  35186      1,534  24636       2,191 Point and Claim
 *      60,758,700  23619      2,572   8081       7,518 Naked Pair
 *      53,258,000  21453      2,482   8462       6,293 Hidden Pair
 *     134,067,300  19286      6,951   1714      78,218 Naked Triple
 *     126,114,500  18843      6,692   1007     125,237 Hidden Triple
 *      60,597,800  18639      3,251   1386      43,721 Two String Kite
 *      39,741,000  17253      2,303    463      85,833 Swampfish
 *      87,784,600  17044      5,150    614     142,971 XY-Wing
 *      61,403,400  16605      3,697    303     202,651 XYZ-Wing
 *      90,834,200  16320      5,565    451     201,406 W-Wing
 *      83,632,700  16015      5,222    348     240,323 Skyscraper
 *      55,803,800  15831      3,524    483     115,535 Empty Rectangle
 *      68,494,400  15348      4,462    256     267,556 Swordfish
 *     166,079,100  15275     10,872     97   1,712,155 Naked Quad
 *      18,653,400  15257      1,222      8   2,331,675 Jellyfish
 *   1,591,190,300  15255    104,306    870   1,828,954 Unique Rectangle
 *     183,346,400  14847     12,349    405     452,707 Finned Swampfish
 *     568,835,600  14508     39,208    373   1,525,028 Finned Swordfish
 *   1,059,092,800  14222     74,468     70  15,129,897 Finned Jellyfish
 *     601,778,900  14164     42,486    157   3,832,986 Coloring
 *     148,002,500  14059     10,527     40   3,700,062 Sashimi Swampfish
 *     160,511,800  14025     11,444     25   6,420,472 Sashimi Swordfish
 *     223,775,800  14006     15,977      5  44,755,160 Sashimi Jellyfish
 *  19,894,514,800  14001  1,420,935   8441   2,356,890 ALS-XZ
 *  20,659,012,100   8280  2,495,049   4304   4,799,956 ALS-Wing
 *   7,698,336,500   4703  1,636,899    458  16,808,594 ALS-Chain
 *   4,075,858,300   4339    939,354    196  20,795,195 Franken Swordfish
 *   6,317,806,600   4191  1,507,469     89  70,986,591 Franken Jellyfish
 *  12,803,537,000   4111  3,114,458     21 609,692,238 Mutant Jellyfish
 *  23,030,848,500   4093  5,626,887    227 101,457,482 Kraken Swampfish
 *   1,687,836,600   4062    415,518     13 129,833,584 Aligned Triple
 *  22,766,719,400   4050  5,621,412     54 421,605,914 Aligned Quad
 *  12,092,204,100   4006  3,018,523   1712   7,063,203 Unary Chain
 *   8,965,296,600   3328  2,693,899   5234   1,712,895 Multiple Chain
 *  10,435,055,200   1406  7,421,803   7685   1,357,847 Dynamic Chain
 *     106,212,900      3 35,404,300     30   3,540,430 Dynamic Plus
 * 156,264,739,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  205,931,433,300 (03:25)  140,471,646
 * NOTES:
 * 1. 03:25 is same as last time, but I dropped the Nishio Chainer and Franken
 *    Swampfish to achieve it. They were both over a second a hint. That's my
 *    limit. I'm really only releasing to take a back-up and clean-up logs.
 * 2. Release 6.30.082 2020-08-10 21:42:22
 *    => DiufSudoku_V6_30.082.2020-08-10.7z (including HoDoKu from now on)
 * 3. Next I don't know. I'd still like to ship it on my birthday.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.083 2020-08-21 10:08:83</b> I went through the first 1000 puzzles in
 * the GUI looking for bugs, weak hint explanations and the like. Fixed a few
 * things like painting fins blue, and bug in Locking mergeSiameseHints.
 * This run has siamese checks switched on, so Locking actually finds
 * MORE eliminations even though it says it finds less because it adds elims to
 * HiddenSet hints, but keeps the search time. Not ideal.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      21,424,500 118507        180 678610          31 Naked Single
 *      17,745,300  50646        350 172480         102 Hidden Single
 *     111,278,700  33398      3,331  21041       5,288 Point and Claim
 *      63,389,000  23110      2,742   7389       8,578 Naked Pair
 *      56,219,500  21460      2,619  13319       4,221 Hidden Pair
 *      61,468,200  23110      2,659   7389       8,318 Naked Pair
 *     137,819,100  19286      7,146   1714      80,407 Naked Triple
 *     140,722,500  19286      7,296   1714      82,101 Naked Triple
 *     131,639,800  18858      6,980   1181     111,464 Hidden Triple
 *      66,899,400  18639      3,589   1386      48,267 Two String Kite
 *      41,409,500  17253      2,400    463      89,437 Swampfish
 *      88,463,700  17044      5,190    614     144,077 XY-Wing
 *      61,189,200  16605      3,684    303     201,944 XYZ-Wing
 *     103,955,100  16320      6,369    451     230,499 W-Wing
 *      95,461,100  16015      5,960    348     274,313 Skyscraper
 *      60,777,500  15831      3,839    483     125,833 Empty Rectangle
 *      71,759,000  15348      4,675    256     280,308 Swordfish
 *     174,662,300  15275     11,434     97   1,800,642 Naked Quad
 *      20,139,900  15257      1,320      8   2,517,487 Jellyfish
 *   1,629,459,400  15255    106,814    870   1,872,941 Unique Rectangle
 *     196,226,400  14847     13,216    405     484,509 Finned Swampfish
 *     602,984,300  14508     41,562    373   1,616,579 Finned Swordfish
 *   1,113,386,500  14222     78,286     70  15,905,521 Finned Jellyfish
 *     462,678,900  14164     32,665    157   2,946,999 Coloring
 *     155,019,200  14059     11,026     40   3,875,480 Sashimi Swampfish
 *     166,254,400  14025     11,854     25   6,650,176 Sashimi Swordfish
 *     230,833,600  14006     16,481      5  46,166,720 Sashimi Jellyfish
 *  23,410,358,500  14001  1,672,049   8441   2,773,410 ALS-XZ
 *  23,385,433,500   8280  2,824,327   4304   5,433,418 ALS-Wing
 *   9,256,956,400   4703  1,968,308    458  20,211,695 ALS-Chain
 *   2,907,267,000   4339    670,031    196  14,832,994 Franken Swordfish
 *   5,323,458,600   4191  1,270,212     89  59,814,141 Franken Jellyfish
 *  11,943,627,000   4111  2,905,285     21 568,744,142 Mutant Jellyfish
 *  23,077,921,600   4093  5,638,387    227 101,664,852 Kraken Swampfish
 *   1,744,812,400   4062    429,545     13 134,216,338 Aligned Triple
 *  25,834,030,500   4050  6,378,772     54 478,407,972 Aligned Quad
 *  12,887,575,600   4006  3,217,068   1712   7,527,789 Unary Chain
 *   9,641,526,100   3328  2,897,093   5234   1,842,095 Multiple Chain
 *  11,368,023,500   1406  8,085,365   7685   1,479,248 Dynamic Chain
 *     155,827,700      3 51,942,566     30   5,194,256 Dynamic Plus
 * 166,820,797,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  218,426,017,400 (03:38)  148,994,554
 * NOTES:
 * 1. 03:38 13 seconds slower with inefficient siamese check: processes 21,041
 *    hints to find about 100 elims which'll be found anyway. It wasn't intended
 *    to be a race-car, but I use it to find a puzzle which trips over a bug;
 *    and you can switch it off at will. It was on for this run. I'll switch it
 *    off when I get bored with siamese locking.
 * 2. Release 6.30.083 2020-08-21 10:08:83
 *    => DiufSudoku_V6_30.083.2020-08-21.7z (including HoDoKu from now on)
 * 3. Next I don't know. I still haven't shipped it. Don't think it'll work on
 *    another machine. Nutbeans spazs-up the jar as soon as you link-in another
 *    project as a library.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.084 2020-08-21 20:37:24</b> Discovered a bug in NakedSet, it was
 * falsely returning false after finding hints. Doh! How embarrassment!
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      20,387,800 118507        172 678610          30 Naked Single
 *      17,925,000  50646        353 172480         103 Hidden Single
 *     105,647,200  33398      3,163  21041       5,021 Point and Claim
 *      61,468,200  23110      2,659   7389       8,318 Naked Pair
 *      52,092,300  21460      2,427  13319       3,911 Hidden Pair
 *     137,819,100  19286      7,146   1714      80,407 Naked Triple
 *     123,722,400  18858      6,560   1181     104,760 Hidden Triple
 *      61,022,800  18639      3,273   1386      44,027 Two String Kite
 *      39,892,500  17253      2,312    463      86,160 Swampfish
 *      84,916,800  17044      4,982    614     138,300 XY-Wing
 *      57,796,200  16605      3,480    303     190,746 XYZ-Wing
 *      90,322,800  16320      5,534    451     200,272 W-Wing
 *      84,915,600  16015      5,302    348     244,010 Skyscraper
 *      56,232,800  15831      3,552    483     116,424 Empty Rectangle
 *      67,269,800  15348      4,382    256     262,772 Swordfish
 *     172,937,400  15275     11,321     97   1,782,859 Naked Quad
 *      18,829,700  15257      1,234      8   2,353,712 Jellyfish
 *   1,546,513,000  15255    101,377    870   1,777,601 Unique Rectangle
 *     184,281,800  14847     12,412    405     455,016 Finned Swampfish
 *     571,450,900  14508     39,388    373   1,532,039 Finned Swordfish
 *   1,068,467,000  14222     75,127     70  15,263,814 Finned Jellyfish
 *     436,999,600  14164     30,852    157   2,783,436 Coloring
 *     147,601,600  14059     10,498     40   3,690,040 Sashimi Swampfish
 *     159,636,000  14025     11,382     25   6,385,440 Sashimi Swordfish
 *     222,891,300  14006     15,913      5  44,578,260 Sashimi Jellyfish
 *  21,677,981,600  14001  1,548,316   8441   2,568,176 ALS-XZ
 *  21,760,864,700   8280  2,628,123   4304   5,055,962 ALS-Wing
 *   8,442,935,800   4703  1,795,223    458  18,434,357 ALS-Chain
 *   2,784,287,100   4339    641,688    196  14,205,546 Franken Swordfish
 *   5,041,519,500   4191  1,202,939     89  56,646,286 Franken Jellyfish
 *  11,477,261,700   4111  2,791,841     21 546,536,271 Mutant Jellyfish
 *  21,745,515,600   4093  5,312,855    227  95,795,222 Kraken Swampfish
 *   1,598,454,400   4062    393,514     13 122,958,030 Aligned Triple
 *  23,601,842,900   4050  5,827,615     54 437,071,164 Aligned Quad
 *  12,069,100,400   4006  3,012,755   1712   7,049,708 Unary Chain
 *   8,840,452,700   3328  2,656,386   5234   1,689,043 Multiple Chain
 *  10,504,200,200   1406  7,470,981   7685   1,366,844 Dynamic Chain
 *     109,306,600      3 36,435,533     30   3,643,553 Dynamic Plus
 * 155,244,762,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  204,387,831,200 (03:24)  139,418,711
 * NOTES:
 * 1. Last top1465: 03:24 is 16 seconds faster.
 * 2. Release 6.30.084 2020-08-21 20:37:24
 *    => DiufSudoku_V6_30.084.2020-08-21.7z (including HoDoKu from now on)
 * 3. Next I don't know. I pre-shipped it this arvo, BEFORE I discovered the
 *    NakedSet bug. Double embarrassment!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.085 2020-08-31 17:39:25</b> Simplified colors by adding a getOrange
 * independent of getGreens and getReds. Fixed all test cases.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      41,209,600 118507        347 678610          60 Naked Single
 *      23,640,000  50646        466 172480         137 Hidden Single
 *     147,126,100  33398      4,405  21041       6,992 Point and Claim
 *      89,149,800  23110      3,857   7389      12,065 Naked Pair
 *      78,361,500  21460      3,651  13319       5,883 Hidden Pair
 *     186,305,800  19286      9,660   1714     108,696 Naked Triple
 *     174,085,200  18858      9,231   1181     147,404 Hidden Triple
 *      84,157,200  18639      4,515   1386      60,719 Two String Kite
 *      56,211,200  17253      3,258    463     121,406 Swampfish
 *     111,553,900  17044      6,545    614     181,683 XY-Wing
 *      79,580,500  16605      4,792    303     262,641 XYZ-Wing
 *     126,569,400  16320      7,755    451     280,641 W-Wing
 *     127,403,900  16015      7,955    348     366,103 Skyscraper
 *      78,366,800  15831      4,950    483     162,250 Empty Rectangle
 *      94,647,100  15348      6,166    256     369,715 Swordfish
 *     242,053,000  15275     15,846     97   2,495,391 Naked Quad
 *      27,292,900  15257      1,788      8   3,411,612 Jellyfish
 *   2,387,373,300  15255    156,497    870   2,744,107 Unique Rectangle
 *     260,607,700  14847     17,552    405     643,475 Finned Swampfish
 *     789,196,000  14508     54,397    373   2,115,806 Finned Swordfish
 *   1,573,331,200  14222    110,626     70  22,476,160 Finned Jellyfish
 *     635,179,200  14164     44,844    157   4,045,727 Coloring
 *     197,764,100  14059     14,066     40   4,944,102 Sashimi Swampfish
 *     215,918,600  14025     15,395     25   8,636,744 Sashimi Swordfish
 *     316,184,100  14006     22,574      5  63,236,820 Sashimi Jellyfish
 *  30,992,541,100  14001  2,213,594   8441   3,671,666 ALS-XZ
 *  30,002,400,300   8280  3,623,478   4304   6,970,817 ALS-Wing
 *  11,682,557,100   4703  2,484,064    458  25,507,766 ALS-Chain
 *   3,653,094,000   4339    841,920    196  18,638,234 Franken Swordfish
 *   6,773,428,700   4191  1,616,184     89  76,105,940 Franken Jellyfish
 *  15,718,270,100   4111  3,823,466     21 748,489,052 Mutant Jellyfish
 *  28,308,580,800   4093  6,916,340    227 124,707,404 Kraken Swampfish
 *   2,221,134,400   4062    546,808     13 170,856,492 Aligned Triple
 *  32,264,276,900   4050  7,966,488     54 597,486,609 Aligned Quad
 *  17,604,911,200   4006  4,394,635   1712  10,283,242 Unary Chain
 *  12,718,441,000   3328  3,821,646   5234   2,429,965 Multiple Chain
 *  14,504,392,700   1406 10,316,068   7685   1,887,364 Dynamic Chain
 *     124,981,400      3 41,660,466     30   4,166,046 Dynamic Plus
 * 214,712,277,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  286,826,115,000 (04:46)  195,652,193
 * NOTES:
 * 1. Last top1465: 4:46 is 1:22 slower, so I am worried; but I've just fixed
 *    all the test-cases, which took a day, so I release before I look at it.
 *    Having done the release and started to investigate I can tell you that
 *    EVERYthing ran slower, so I shall run it again this evening after letting
 *    the machine cool down. I think it "dropped back to safety-mode" because
 *    the CPU got too hot with me running three times in a row.
 * 2. Release 6.30.085 2020-08-31 17:39:25
 *    => DiufSudoku_V6_30.085.2020-08-31.7z (including HoDoKu from now on)
 * 3. Next I look at why it's now a minute and twenty-two seconds slower.
 *
 * Just confirming it was just a hot PC. Same code ran in 3:23 after cooldown.
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      19,016,700 118507        160 678610          28 Naked Single
 *      14,642,000  50646        289 172480          84 Hidden Single
 *     104,952,700  33398      3,142  21041       4,988 Point and Claim
 *      59,397,000  23110      2,570   7389       8,038 Naked Pair
 *      51,342,000  21460      2,392  13319       3,854 Hidden Pair
 *     132,486,500  19286      6,869   1714      77,296 Naked Triple
 *     121,904,000  18858      6,464   1181     103,220 Hidden Triple
 *      61,367,900  18639      3,292   1386      44,276 Two String Kite
 *      38,115,300  17253      2,209    463      82,322 Swampfish
 *      86,397,700  17044      5,069    614     140,712 XY-Wing
 *      57,614,800  16605      3,469    303     190,147 XYZ-Wing
 *      99,507,100  16320      6,097    451     220,636 W-Wing
 *      85,089,400  16015      5,313    348     244,509 Skyscraper
 *      56,130,900  15831      3,545    483     116,213 Empty Rectangle
 *      67,624,900  15348      4,406    256     264,159 Swordfish
 *     166,060,900  15275     10,871     97   1,711,968 Naked Quad
 *      18,729,600  15257      1,227      8   2,341,200 Jellyfish
 *   1,587,176,800  15255    104,043    870   1,824,341 Unique Rectangle
 *     183,333,900  14847     12,348    405     452,676 Finned Swampfish
 *     561,458,600  14508     38,699    373   1,505,250 Finned Swordfish
 *   1,048,282,300  14222     73,708     70  14,975,461 Finned Jellyfish
 *     430,297,800  14164     30,379    157   2,740,750 Coloring
 *     146,908,500  14059     10,449     40   3,672,712 Sashimi Swampfish
 *     155,030,100  14025     11,053     25   6,201,204 Sashimi Swordfish
 *     217,337,800  14006     15,517      5  43,467,560 Sashimi Jellyfish
 *  21,498,142,000  14001  1,535,471   8441   2,546,871 ALS-XZ
 *  21,845,088,600   8280  2,638,295   4304   5,075,531 ALS-Wing
 *   8,390,101,300   4703  1,783,989    458  18,318,998 ALS-Chain
 *   2,583,904,800   4339    595,506    196  13,183,187 Franken Swordfish
 *   4,947,052,600   4191  1,180,399     89  55,584,860 Franken Jellyfish
 *  11,591,201,000   4111  2,819,557     21 551,961,952 Mutant Jellyfish
 *  21,397,497,100   4093  5,227,827    227  94,262,101 Kraken Swampfish
 *   1,621,683,400   4062    399,232     13 124,744,876 Aligned Triple
 *  23,065,614,800   4050  5,695,213     54 427,141,014 Aligned Quad
 *  11,971,917,900   4006  2,988,496   1712   6,992,942 Unary Chain
 *   8,869,357,600   3328  2,665,071   5234   1,694,565 Multiple Chain
 *  10,481,898,200   1406  7,455,119   7685   1,363,942 Dynamic Chain
 *     129,611,200      3 43,203,733     30   4,320,373 Dynamic Plus
 * 153,963,275,700
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  203,040,552,900 (03:23)  138,499,695
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.087 2020-09-03 08:05:27</b> Fixed logView. Previous release is a bust.
 * Bugs in the load process makes it not work at all; so I must release patch
 * ASAP. I fixed the logView, which is what I thought I had done in the previous
 * release, which was rushed out because I don't control when we go to town. I
 * should NOT have released untested. That was a BIG mistake and I won't make it
 * again. Sigh. logView now takes a Java regex (was just a plain string).
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      37,937,600 118507        320 678610          55 Naked Single
 *      21,785,100  50646        430 172480         126 Hidden Single
 *     137,779,100  33398      4,125  21041       6,548 Point and Claim
 *      87,807,600  23110      3,799   7389      11,883 Naked Pair
 *      78,289,500  21460      3,648  13319       5,878 Hidden Pair
 *     188,037,200  19286      9,749   1714     109,706 Naked Triple
 *     170,424,400  18858      9,037   1181     144,305 Hidden Triple
 *      80,383,300  18639      4,312   1386      57,996 Two String Kite
 *      54,650,000  17253      3,167    463     118,034 Swampfish
 *     104,073,700  17044      6,106    614     169,501 XY-Wing
 *      70,767,700  16605      4,261    303     233,556 XYZ-Wing
 *     119,399,200  16320      7,316    451     264,743 W-Wing
 *     115,436,200  16015      7,208    348     331,713 Skyscraper
 *      69,144,800  15831      4,367    483     143,156 Empty Rectangle
 *      95,030,600  15348      6,191    256     371,213 Swordfish
 *     246,609,100  15275     16,144     97   2,542,361 Naked Quad
 *      26,638,700  15257      1,745      8   3,329,837 Jellyfish
 *   2,446,862,100  15255    160,397    870   2,812,485 Unique Rectangle
 *     254,316,900  14847     17,129    405     627,942 Finned Swampfish
 *     775,237,500  14508     53,435    373   2,078,384 Finned Swordfish
 *   1,551,747,800  14222    109,108     70  22,167,825 Finned Jellyfish
 *     615,392,000  14164     43,447    157   3,919,694 Coloring
 *     191,965,600  14059     13,654     40   4,799,140 Sashimi Swampfish
 *     227,653,000  14025     16,231     25   9,106,120 Sashimi Swordfish
 *     332,255,200  14006     23,722      5  66,451,040 Sashimi Jellyfish
 *  26,833,855,200  14001  1,916,567   8441   3,178,990 ALS-XZ
 *  27,579,138,300   8280  3,330,813   4304   6,407,792 ALS-Wing
 *  10,445,315,000   4703  2,220,989    458  22,806,364 ALS-Chain
 *   3,601,437,700   4339    830,015    196  18,374,682 Franken Swordfish
 *   6,788,664,800   4191  1,619,819     89  76,277,132 Franken Jellyfish
 *  15,593,971,900   4111  3,793,230     21 742,570,090 Mutant Jellyfish
 *  27,707,464,700   4093  6,769,475    227 122,059,315 Kraken Swampfish
 *   2,316,501,000   4062    570,285     13 178,192,384 Aligned Triple
 *  33,545,454,100   4050  8,282,828     54 621,212,112 Aligned Quad
 *  18,477,018,700   4006  4,612,336   1712  10,792,651 Unary Chain
 *  13,298,477,200   3328  3,995,936   5234   2,540,786 Multiple Chain
 *  14,777,847,500   1406 10,510,560   7685   1,922,946 Dynamic Chain
 *     143,872,500      3 47,957,500     30   4,795,750 Dynamic Plus
 * 209,208,642,500
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  277,753,804,400 (04:37)  189,463,713
 * NOTES:
 * 1. Last top1465 03:03 but no Krakens so don't get excited. Ran again with all
 *    Krakens for 4:04, so Krakens take 1:01. Ran again w/out Kraken Swordfish
 *    (2.285 s/e) and Kraken Jellyfish (20.567 s/e) overheated 4:37. Can't win!
 *    So I'm not concerned by speed (or lack thereof) at the moment.
 * 2. Release 6.30.087 2020-09-03 08:05:27
 *    => DiufSudoku_V6_30.087.2020-09-03.7z (including HoDoKu from now on)
 * 3. Next I run through puzzles looking for any "bugs" in the hints. Like I
 *    said before: this is about as far as I can take it. Others may be able
 *    to (if they are willing) take it further, but this is the limit of my
 *    intellect; so I shall release it to the wild and hope for the best.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.088 2020-09-07 11:20:28</b> Release coz I'm at the library and I can.
 * No notable changes to the best of my recollection.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      22,423,500 118507        189 678610          33 Naked Single
 *      15,103,700  50646        298 172480          87 Hidden Single
 *     114,933,000  33398      3,441  21041       5,462 Point and Claim
 *      63,533,200  23110      2,749   7389       8,598 Naked Pair
 *      58,391,300  21460      2,720  13319       4,384 Hidden Pair
 *     141,057,200  19286      7,313   1714      82,297 Naked Triple
 *     130,447,300  18858      6,917   1181     110,454 Hidden Triple
 *      62,402,000  18639      3,347   1386      45,023 Two String Kite
 *      41,615,100  17253      2,412    463      89,881 Swampfish
 *      93,278,000  17044      5,472    614     151,918 XY-Wing
 *      65,253,000  16605      3,929    303     215,356 XYZ-Wing
 *      93,646,700  16320      5,738    451     207,642 W-Wing
 *      88,892,300  16015      5,550    348     255,437 Skyscraper
 *      56,721,400  15831      3,582    483     117,435 Empty Rectangle
 *      73,708,600  15348      4,802    256     287,924 Swordfish
 *     178,929,100  15275     11,713     97   1,844,629 Naked Quad
 *      19,939,000  15257      1,306      8   2,492,375 Jellyfish
 *   1,677,236,600  15255    109,946    870   1,927,858 Unique Rectangle
 *     197,427,700  14847     13,297    405     487,475 Finned Swampfish
 *     606,910,600  14508     41,832    373   1,627,106 Finned Swordfish
 *   1,130,982,800  14222     79,523     70  16,156,897 Finned Jellyfish
 *     444,491,200  14164     31,381    157   2,831,154 Coloring
 *     154,704,800  14059     11,003     40   3,867,620 Sashimi Swampfish
 *     166,577,900  14025     11,877     25   6,663,116 Sashimi Swordfish
 *     232,276,600  14006     16,584      5  46,455,320 Sashimi Jellyfish
 *  21,187,915,300  14001  1,513,314   8441   2,510,119 ALS-XZ
 *  21,198,293,500   8280  2,560,180   4304   4,925,254 ALS-Wing
 *   7,947,271,100   4703  1,689,830    458  17,352,120 ALS-Chain
 *   2,826,922,400   4339    651,514    196  14,423,073 Franken Swordfish
 *   5,130,882,800   4191  1,224,262     89  57,650,368 Franken Jellyfish
 *  11,505,369,800   4111  2,798,679     21 547,874,752 Mutant Jellyfish
 *  22,009,177,700   4093  5,377,272    227  96,956,729 Kraken Swampfish
 *   1,651,446,800   4062    406,560     13 127,034,369 Aligned Triple
 *  24,326,838,000   4050  6,006,626     54 450,497,000 Aligned Quad
 *  12,284,986,700   4006  3,066,646   1712   7,175,809 Unary Chain
 *   9,001,789,100   3328  2,704,864   5234   1,719,867 Multiple Chain
 *  10,792,151,400   1406  7,675,783   7685   1,404,313 Dynamic Chain
 *     105,455,800      3 35,151,933     30   3,515,193 Dynamic Plus
 * 155,899,383,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  219,080,861,700 (03:39)  149,441,242
 * NOTES:
 * 1. Last top1465 03:39 About right.
 * 2. Release 6.30.088 2020-09-07 11:20:28
 *    => DiufSudoku_V6_30.088.2020-09-07.7z (including HoDoKu from now on)
 * 3. Next I keep running through puzzles in the GUI looking for "bugs".
 *    A notable "bug" found: hint cached in chainer but initial assumption no
 *    longer valid (cell now set by previously hint) but elim stil current: so
 *    so we ignore it and apply the hint despite it's predicate being outdated.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.089 2020-09-12 11:55:29</b> Bug fix: Subsequent views not shown for
 * RegionReductionHint and CellReductionHint.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      20,489,700 116248        176 666580          30 Naked Single
 *      16,534,800  49590        333 168900          97 Hidden Single
 *      99,902,100  32700      3,055   5855      17,062 Direct Naked Pair
 *      87,664,200  32288      2,715  12465       7,032 Direct Hidden Pair
 *     211,116,700  31334      6,737    879     240,178 Direct Naked Triple
 *     187,903,200  31279      6,007   1832     102,567 Direct Hidden Triple
 *     100,780,800  31139      3,236  18529       5,439 Point and Claim
 *      57,564,800  21967      2,620   4448      12,941 Naked Pair
 *      46,745,000  20836      2,243   8525       5,483 Hidden Pair
 *     111,519,200  19211      5,804   1289      86,516 Naked Triple
 *      94,069,600  18844      4,992   1021      92,134 Hidden Triple
 *      62,788,800  18644      3,367   1386      45,302 Two String Kite
 *      41,107,800  17258      2,381    463      88,785 Swampfish
 *      84,322,900  17049      4,945    614     137,333 XY-Wing
 *      56,047,500  16610      3,374    303     184,975 XYZ-Wing
 *      90,205,000  16325      5,525    450     200,455 W-Wing
 *      85,228,500  16021      5,319    347     245,615 Skyscraper
 *      57,501,700  15838      3,630    483     119,051 Empty Rectangle
 *      68,476,500  15355      4,459    255     268,535 Swordfish
 *     173,894,200  15283     11,378     97   1,792,723 Naked Quad
 *      19,138,800  15265      1,253      8   2,392,350 Jellyfish
 *   1,594,961,000  15263    104,498    872   1,829,083 Unique Rectangle
 *     184,051,000  14854     12,390    406     453,327 Finned Swampfish
 *     585,257,400  14514     40,323    374   1,564,859 Finned Swordfish
 *   1,093,522,500  14227     76,862     70  15,621,750 Finned Jellyfish
 *     442,379,400  14169     31,221    157   2,817,703 Coloring
 *     145,294,000  14064     10,330     40   3,632,350 Sashimi Swampfish
 *     158,985,200  14030     11,331     25   6,359,408 Sashimi Swordfish
 *     220,660,800  14011     15,749      5  44,132,160 Sashimi Jellyfish
 *  22,095,891,800  14006  1,577,601   8444   2,616,756 ALS-XZ
 *  21,713,031,100   8282  2,621,713   4304   5,044,849 ALS-Wing
 *   8,628,030,900   4705  1,833,800    458  18,838,495 ALS-Chain
 *   2,743,209,700   4341    631,930    196  13,995,967 Franken Swordfish
 *   5,185,866,300   4193  1,236,791     89  58,268,160 Franken Jellyfish
 *  11,968,443,400   4113  2,909,906     21 569,925,876 Mutant Jellyfish
 *  21,552,324,000   4095  5,263,082    212 101,661,905 Kraken Swampfish
 *   1,631,894,200   4062    401,746     13 125,530,323 Aligned Triple
 *  22,818,690,800   4050  5,634,244     54 422,568,348 Aligned Quad
 *  12,208,668,500   4006  3,047,595   1712   7,131,231 Unary Chain
 *   9,011,509,700   3328  2,707,785   5234   1,721,725 Multiple Chain
 *  10,740,940,700   1406  7,639,360   7685   1,397,650 Dynamic Chain
 *     117,796,200      3 39,265,400     30   3,926,540 Dynamic Plus
 * 156,614,410,400
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  206,144,198,200 (03:26)  140,616,779
 * NOTES:
 * 1. Last top1465 03:26 is 13 second faster, with Directs. I don't know why.
 * 2. Release 6.30.089 2020-09-12 11:55:29
 *    => DiufSudoku_V6_30.089.2020-09-12.7z (including HoDoKu from now on)
 * 3. Next I keep running through puzzles in the GUI looking for "bugs".
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.090 2020-09-24 13:30:30</b> Boosted ColoringSolver as color.Coloring.
 * Renamed package diuf.sudoku.solver.hinters.alshdk to als.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      18,818,600 116232        161 666640          28 Naked Single
 *      14,477,000  49568        292 168840          85 Hidden Single
 *      96,761,700  32684      2,960   5855      16,526 Direct Naked Pair
 *      86,531,100  32272      2,681  12453       6,948 Direct Hidden Pair
 *     211,911,300  31319      6,766    879     241,082 Direct Naked Triple
 *     191,461,100  31264      6,124   1832     104,509 Direct Hidden Triple
 *      99,563,700  31124      3,198  18525       5,374 Point and Claim
 *      52,693,300  21956      2,399   4444      11,857 Naked Pair
 *      46,565,900  20826      2,235   8528       5,460 Hidden Pair
 *     110,795,100  19199      5,770   1288      86,021 Naked Triple
 *      93,986,600  18833      4,990   1021      92,053 Hidden Triple
 *      61,462,500  18633      3,298   1388      44,281 Two String Kite
 *      40,354,500  17245      2,340    468      86,227 Swampfish
 *      83,754,300  17035      4,916    611     137,077 XY-Wing
 *      56,283,200  16597      3,391    305     184,535 XYZ-Wing
 *      99,701,900  16310      6,112    446     223,546 W-Wing
 *      51,833,300  16009      3,237    346     149,807 Skyscraper
 *      55,759,700  15826      3,523    481     115,924 Empty Rectangle
 *      70,495,900  15345      4,594    255     276,454 Swordfish
 *     181,350,700  15273     11,873     97   1,869,594 Naked Quad
 *     142,700,900  15255      9,354     13  10,976,992 Hidden Quad
 *      19,749,300  15253      1,294      8   2,468,662 Jellyfish
 *   1,603,031,100  15251    105,109    872   1,838,338 Unique Rectangle
 *     184,375,200  14842     12,422    405     455,247 Finned Swampfish
 *     564,943,200  14503     38,953    376   1,502,508 Finned Swordfish
 *   1,047,419,700  14214     73,689     70  14,963,138 Finned Jellyfish
 *     442,924,000  14156     31,288    153   2,894,928 Coloring
 *     145,624,700  14049     10,365     40   3,640,617 Sashimi Swampfish
 *     159,054,500  14015     11,348     25   6,362,180 Sashimi Swordfish
 *     218,851,200  13996     15,636      5  43,770,240 Sashimi Jellyfish
 *  21,786,161,400  13991  1,557,155   8433   2,583,441 ALS-XZ
 *  21,810,166,900   8271  2,636,944   4304   5,067,417 ALS-Wing
 *   8,302,461,300   4695  1,768,362    456  18,207,151 ALS-Chain
 *   2,692,695,000   4333    621,438    196  13,738,239 Franken Swordfish
 *   4,990,669,400   4185  1,192,513     89  56,074,937 Franken Jellyfish
 *  12,050,930,400   4105  2,935,671     21 573,853,828 Mutant Jellyfish
 *  21,355,513,900   4087  5,225,229    207 103,166,733 Kraken Swampfish
 *       4,998,700   4055      1,232      1   4,998,700 Aligned Pair
 *       5,518,100   4054      1,361      4   1,379,525 Aligned Triple
 *      16,606,800   4051      4,099     19     874,042 Aligned Quad
 *       8,746,200   4037      2,166      1   8,746,200 Aligned Pent
 *       4,089,200   4036      1,013      1   4,089,200 Aligned Sept
 *  12,269,471,900   4035  3,040,761   1736   7,067,668 Unary Chain
 *   8,964,310,400   3342  2,682,319   5256   1,705,538 Multiple Chain
 *  10,592,862,000   1407  7,528,686   7677   1,379,817 Dynamic Chain
 *      92,736,400      3 30,912,133     30   3,091,213 Dynamic Plus
 * 131,201,173,200
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  179,931,104,200 (02:59)  122,736,087
 * NOTES:
 * 1. Last top1465 2:59 is 27 seconds faster coz Grid.getIdx now caches.
 * 2. Release 6.30.090 2020-09-24 13:30:30
 *    => DiufSudoku_V6_30.090.2020-09-24.7z (including HoDoKu from now on)
 * 3. Next I'm boosting FishSolver, to remove the HoDoKu dependancy.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.091 2020-10-06 18:06:31</b> Boost HoDoKuFisherman does Finned, Franken,
 * and Mutant fish. It can do Basic but doesn't coz Fisherman is faster. It's
 * still too slow for Mutant (not really useable). Stripped Kraken from hobiwan
 * code, which I plan to put in a KrakenFisherman class.
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        19,665,900 116246         169 666800             29 Naked Single
 *        15,775,500  49566         318 168720             93 Hidden Single
 *       125,091,200  32694       3,826   5823         21,482 Direct Naked Pair
 *        90,428,100  32285       2,800  12441          7,268 Direct Hidden Pair
 *       211,094,100  31333       6,737    879        240,152 Direct Naked Triple
 *       187,879,500  31278       6,006   1831        102,610 Direct Hidden Triple
 *        95,015,000  31138       3,051  18529          5,127 Point and Claim
 *        54,438,000  21968       2,478   4432         12,282 Naked Pair
 *        49,004,900  20845       2,350   8521          5,751 Hidden Pair
 *       107,286,500  19220       5,582   1288         83,296 Naked Triple
 *        92,449,200  18854       4,903   1021         90,547 Hidden Triple
 *        60,834,300  18654       3,261   1386         43,891 Two String Kite
 *        40,160,600  17268       2,325    465         86,366 Swampfish
 *        89,407,900  17059       5,241    616        145,142 XY-Wing
 *        62,316,800  16617       3,750    306        203,649 XYZ-Wing
 *        62,030,500  16330       3,798    446        139,081 W-Wing
 *        45,307,700  16030       2,826    348        130,194 Skyscraper
 *        53,820,700  15846       3,396    484        111,199 Empty Rectangle
 *        67,155,900  15362       4,371    252        266,491 Swordfish
 *       170,960,900  15291      11,180     97      1,762,483 Naked Quad
 *       138,185,600  15273       9,047     13     10,629,661 Hidden Quad
 *        21,182,600  15271       1,387      8      2,647,825 Jellyfish
 *     1,600,013,200  15269     104,788    872      1,834,877 Unique Rectangle
 *     3,139,298,200  14860     211,258    500      6,278,596 Finned Swampfish
 *     6,774,386,300  14438     469,205    435     15,573,301 Finned Swordfish
 *     8,172,555,800  14088     580,107     23    355,328,513 Finned Jellyfish
 *       448,192,200  14069      31,856    107      4,188,712 Coloring
 *    21,777,465,400  13985   1,557,201   8423      2,585,476 ALS-XZ
 *    21,539,783,700   8267   2,605,513   4296      5,013,916 ALS-Wing
 *     8,505,291,300   4701   1,809,251    456     18,651,954 ALS-Chain
 *     3,685,456,400   4339     849,379      0              0 Franken Swampfish
 *    17,947,207,300   4339   4,136,254    168    106,828,614 Franken Swordfish
 *    44,952,414,900   4198  10,708,055     98    458,698,111 Franken Jellyfish
 *     7,498,446,200   4108   1,825,327      0              0 Mutant Swampfish
 *   105,529,349,600   4108  25,688,741      1 105,529,349,600 Mutant Swordfish == ASS-RAPED BY JESSIE THE ELEPHANT
 *   716,097,977,000   4107 174,360,354     11 65,099,816,090 Mutant Jellyfish  == That's nearly 12 minutes.
 *    22,028,511,900   4098   5,375,429    326     67,572,122 Kraken Swampfish
 *    20,759,255,800   4058   5,115,637      9  2,306,583,977 Kraken Swordfish
 *    20,712,072,800   4051   5,112,829      1 20,712,072,800 Kraken Jellyfish
 *         5,509,500   4050       1,360      1      5,509,500 Aligned Pair
 *         5,828,400   4049       1,439      4      1,457,100 Aligned Triple
 *        16,519,900   4046       4,083     19        869,468 Aligned Quad
 *         8,508,600   4032       2,110      1      8,508,600 Aligned Pent
 *         4,920,300   4031       1,220      0              0 Aligned Hex
 *         3,824,400   4031         948      1      3,824,400 Aligned Sept
 *    12,175,016,900   4030   3,021,096   1730      7,037,582 Unary Chain
 *     8,658,721,600   3336   2,595,540   5260      1,646,144 Multiple Chain
 *     9,946,616,500   1406   7,074,407   7676      1,295,807 Dynamic Chain
 *        82,567,900      3  27,522,633     30      2,752,263 Dynamic Plus
 * 1,063,935,203,400
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1466 1,118,927,964,000 (18:38)       763,252,362
 * NOTES:
 * 1. Last top1465 18:38 is 15:31 slower coz Mutants are too slow!
 * 2. Release 6.30.091 2020-10-06 18:06:31
 *    => DiufSudoku_V6_30.091.2020-10-06.7z (including HoDoKu from now on)
 * 3. Next I'm boosting Kraken into KrakenFisherman.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.092 2020-10-11 18:46:32</b> Boost Kraken Fish from HoDoKuFisherman into
 * KrakenFisherman. It works, but it's so slow it won't even fit on one line.
 * <pre>
 *          time(ns)  calls     time/call  elims       time/elim hinter
 *        19,878,600 117278           169 670340              29 Naked Single
 *        15,549,000  50244           309 170540              91 Hidden Single
 *        94,570,900  33190         2,849   5878          16,088 Direct Naked Pair
 *        86,072,900  32776         2,626  12506           6,882 Direct Hidden Pair
 *       213,994,500  31819         6,725    867         246,821 Direct Naked Triple
 *       188,137,900  31765         5,922   1862         101,040 Direct Hidden Triple
 *        97,010,700  31623         3,067  18541           5,232 Point and Claim
 *        53,627,800  22421         2,391   4517          11,872 Naked Pair
 *        46,366,900  21244         2,182   8522           5,440 Hidden Pair
 *       113,230,500  19612         5,773   1312          86,303 Naked Triple
 *        94,602,500  19229         4,919   1001          94,507 Hidden Triple
 *        59,610,200  19039         3,130   1372          43,447 Two String Kite
 *        41,514,800  17667         2,349    464          89,471 Swampfish
 *        87,478,500  17458         5,010    660         132,543 XY-Wing
 *        60,848,200  16991         3,581    312         195,026 XYZ-Wing
 *        66,866,500  16702         4,003    467         143,183 W-Wing
 *        46,006,000  16385         2,807    347         132,582 Skyscraper
 *        53,237,200  16200         3,286    479         111,142 Empty Rectangle
 *        71,106,700  15721         4,523    246         289,051 Swordfish
 *       188,237,000  15651        12,027    101       1,863,732 Naked Quad
 *       141,949,100  15632         9,080     13      10,919,161 Hidden Quad
 *        20,043,400  15630         1,282      5       4,008,680 Jellyfish
 *     1,581,231,800  15629       101,172    853       1,853,730 Unique Rectangle
 *     2,801,602,900  15230       183,952    490       5,717,556 Finned Swampfish
 *     6,470,874,200  14817       436,719    413      15,667,976 Finned Swordfish
 *     7,735,971,500  14480       534,252     28     276,284,696 Finned Jellyfish
 *       400,873,400  14456        27,730    109       3,677,737 Coloring
 *    20,453,476,100  14371     1,423,246   8442       2,422,823 ALS-XZ
 *    22,332,944,500   8622     2,590,227   4315       5,175,653 ALS-Wing
 *     8,201,567,000   5016     1,635,081    458      17,907,351 ALS-Chain
 *    18,484,087,100   4648     3,976,782    172     107,465,622 Franken Swordfish
 *    45,796,275,400   4506    10,163,398     96     477,044,535 Franken Jellyfish
 *         2,658,000   4419           601      0               0 Existing Hints
 *   993,138,371,700   4419   224,742,786   3571     278,112,117 Kraken Swampfish
 * 2,845,465,102,100   1660 1,714,135,603    134  21,234,814,194 Kraken Swordfish
 * 5,971,616,894,000   1546 3,862,624,122     13 459,355,145,692 Kraken Jellyfish
 *     3,769,611,500   1535     2,455,772      0               0 Unary Chain
 *     1,873,357,500   1535     1,220,428      6     312,226,250 Nishio Chain
 *     3,673,459,700   1529     2,402,524    554       6,630,793 Multiple Chain
 *     8,831,367,500   1236     7,145,119   6651       1,327,825 Dynamic Chain
 *        88,774,900      3    29,591,633     30       2,959,163 Dynamic Plus
 * 9,964,578,440,600
 * pzls          total (ns)   (mm:ss)       each (ns)
 * 1466  10,129,922,591,100  (168:49)   6,909,906,269
 * NOTES:
 * 1. Last top1465 168:49 or two and a half hours slower than the previous run.
 *    Don't panic, just don't use Kraken Swordfish at 21 seconds per elim,
 *    or Kraken Jellyfish at just 7 minutes and 39 seconds per elimination.
 *    There's a second version called KrakenFisherman1 that's faster, but finds
 *    less hints (I know not why), so ends-up just a tad faster.
 *      587,959,366,800 4216   139,459,052 1829     321,464,935 Kraken Swampfish
 *    2,420,093,870,400 2775   872,105,899   82  29,513,339,882 Kraken Swordfish
 *    5,391,113,891,800 2702 1,995,230,900   13 414,701,068,600 Kraken Jellyfish
 *    pzls          total (ns)   (mm:ss)       each (ns)
 *    1466   8,703,061,863,100  (145:03)   5,936,604,272
 * 2. All I can say about Existing Hints is "don't use it". The code is far
 *    too complex. It's so complex I can't understand why it doesn't work.
 *    I think I shall comment out the Tech.
 * 3. Release 6.30.092 2020-10-11 18:46:32
 *    => DiufSudoku_V6_30.092.2020-10-11.7z (without HoDoKu now)
 * 4. Next I'm trying again to make KrakenFisherman servicably quick.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.093 2020-10-14 18:03:33</b> New version of KrakenFisherman is twice as
 * fast as original, so now it's just very very slow.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        20,275,200 117261         172 670640            30 Naked Single
 *        15,190,800  50197         302 170260            89 Hidden Single
 *        97,453,300  33171       2,937   5868        16,607 Direct Naked Pair
 *        90,991,100  32758       2,777  12518         7,268 Direct Hidden Pair
 *       214,893,300  31800       6,757    867       247,858 Direct Naked Triple
 *       198,247,300  31746       6,244   1806       109,771 Direct Hidden Triple
 *       100,110,900  31609       3,167  18563         5,393 Point and Claim
 *        56,238,600  22407       2,509   4512        12,464 Naked Pair
 *        51,104,500  21233       2,406   8513         6,003 Hidden Pair
 *       114,840,700  19608       5,856   1311        87,597 Naked Triple
 *       100,960,100  19229       5,250   1006       100,357 Hidden Triple
 *        64,421,500  19038       3,383   1380        46,682 Two String Kite
 *        43,215,900  17658       2,447    457        94,564 Swampfish
 *        89,517,000  17452       5,129    662       135,222 XY-Wing
 *        62,180,300  16987       3,660    313       198,659 XYZ-Wing
 *        69,135,500  16697       4,140    468       147,725 W-Wing
 *        47,903,400  16381       2,924    354       135,320 Skyscraper
 *        58,091,600  16192       3,587    479       121,276 Empty Rectangle
 *        73,540,600  15713       4,680    244       301,395 Swordfish
 *       190,645,400  15644      12,186     99     1,925,711 Naked Quad
 *        20,615,200  15626       1,319      5     4,123,040 Jellyfish
 *     1,621,285,200  15625     103,762    849     1,909,640 Unique Rectangle
 *     2,760,623,600  15228     181,286    492     5,611,023 Finned Swampfish
 *     6,327,064,700  14816     427,042    433    14,612,158 Finned Swordfish
 *       421,291,000  14460      29,134    118     3,570,262 Coloring
 *    20,842,605,800  14369   1,450,525   8446     2,467,748 ALS-XZ
 *    22,837,936,200   8642   2,642,667   4324     5,281,668 ALS-Wing
 *     8,396,029,600   5036   1,667,202    457    18,372,056 ALS-Chain
 *   535,480,245,700   4669 114,688,422   3858   138,797,367 Kraken Swampfish
 * 1,466,336,256,300   1681 872,299,974    170 8,625,507,390 Kraken Swordfish
 *     3,894,392,000   1532   2,542,031      0             0 Unary Chain
 *     3,828,516,000   1532   2,499,031    552     6,935,717 Multiple Chain
 *     9,137,816,500   1241   7,363,268   6683     1,367,322 Dynamic Chain
 *        85,498,900      3  28,499,633     30     2,849,963 Dynamic Plus
 * 2,083,749,133,700
 * pzls         total (ns) (mm:ss)      each (ns)
 * 1466  2,149,917,244,000 (35:49)  1,466,519,266
 * NOTES:
 * 1. Last top1465 168:49 35:49 is two hours faster than the previous run, but
 *    excluding KrakenJellyfish, so don't get excited. KrakenSwordfish is down
 *    from 21 seconds per elim to 8.6. Worked-out why KrakenFisherman1 missed
 *    hints and promoted it to KrakenFisherman.
 * 3. Release 6.30.093 2020-10-14 18:03:33
 *    => DiufSudoku_V6_30.093.2020-10-14.7z
 * 4. Next I investigate dis-interleaving the base and cover regions, and I
 *    still need to clean-up "the mess" I've made out of ALSs package, to get
 *    rid of the HoDoKu types, especially HdkAls. I also need to make Franken,
 *    Mutant, and Kraken fish faster; but I'll be buggered if I know how.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.094 2020-10-15 15:37:34</b> Replaced the "interleaved" getRegions
 * method in AHint and all subtypes (30 of them) with getBase and getCovers.
 * This change should have no consequence to non-developers, but to a techie
 * combining two different types of things into one array was MENTAL. So now
 * the code is not quite so mental. I probably haven't eradicated all of the
 * helper methods which supported interleaving and dis-interleaving regions.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      19,639,300 116340        168 666020          29 Naked Single
 *      14,726,300  49738        296 169180          87 Hidden Single
 *      99,450,900  32820      3,030   5812      17,111 Direct Naked Pair
 *      86,874,000  32412      2,680  12373       7,021 Direct Hidden Pair
 *     206,276,100  31466      6,555    879     234,671 Direct Naked Triple
 *     187,462,100  31411      5,968   1806     103,799 Direct Hidden Triple
 *      98,618,900  31273      3,153  18522       5,324 Locking
 *      56,667,200  22108      2,563   4419      12,823 Naked Pair
 *      47,485,600  20985      2,262   8541       5,559 Hidden Pair
 *     108,359,900  19348      5,600   1289      84,065 Naked Triple
 *      95,631,300  18983      5,037   1026      93,207 Hidden Triple
 *      61,361,100  18782      3,267   1387      44,240 Two String Kite
 *      40,596,600  17395      2,333    477      85,108 Swampfish
 *      85,981,200  17183      5,003    609     141,184 XY-Wing
 *      60,591,300  16746      3,618    305     198,660 XYZ-Wing
 *      67,406,400  16459      4,095    453     148,800 W-Wing
 *      46,441,600  16154      2,874    354     131,190 Skyscraper
 *      54,964,700  15969      3,441    481     114,271 Empty Rectangle
 *      70,888,000  15488      4,576    254     279,086 Swordfish
 *     172,073,300  15415     11,162     97   1,773,951 Naked Quad
 *      20,293,400  15397      1,318      8   2,536,675 Jellyfish
 *   1,527,276,400  15395     99,206    872   1,751,463 Unique Rectangle
 *   2,676,358,200  14986    178,590    504   5,310,234 Finned Swampfish
 *   6,168,615,500  14562    423,610    456  13,527,665 Finned Swordfish
 *   7,310,508,100  14193    515,078     24 304,604,504 Finned Jellyfish
 *  21,676,570,500  14173  1,529,427   8477   2,557,103 ALS-XZ
 *  21,771,269,500   8420  2,585,661   4349   5,006,040 ALS-Wing
 *   8,484,831,500   4801  1,767,305    455  18,647,981 ALS-Chain
 *      59,410,000   4438     13,386      0           0 Bi-Uni Grave
 *  13,534,302,700   4438  3,049,640   2039   6,637,715 Unary Chain
 *   4,306,029,700   3636  1,184,276    345  12,481,245 Nishio Chain
 *   8,847,640,600   3390  2,609,923   5334   1,658,725 Multiple Chain
 *  10,178,964,700   1413  7,203,796   7737   1,315,621 Dynamic Chain
 *     101,064,000      3 33,688,000     30   3,368,800 Dynamic Plus
 * 108,344,630,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  154,258,546,000 (02:34)  105,224,110
 * NOTES:
 * 1. Last top1465 2:34 is about expected for the wanted hinters.
 * 2. Release 6.30.094 2020-10-15 15:37:34
 *    => DiufSudoku_V6_30.094.2020-10-15.7z
 * 3. Next I need to clean-up "the mess" I've made out of ALSs package, to get
 *    rid of the HoDoKu types, especially HdkAls.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.095 2020-10-17 22:25:35</b> test-cases for Idx, HdkSet and HdkSetBase.
 * I've given-up on cleaning-up "the mess" I've made out of ALSs package, to
 * get rid of the HoDoKu types, especially HdkAls; I've tried 4 times and it
 * just fails like a bastard, so I feel totally inadequate, but none the less
 * I have given-up on it. So it's past time to ship it again.
 * <pre>
 *       time(ns)  calls  time/call  elims time/elim hinter
 *     19,738,000 114601        172 661410        29 Naked Single
 *     14,260,000  48460        294 163590        87 Hidden Single
 *     91,136,500  32101      2,839   5926    15,379 Direct Naked Pair
 *     86,213,300  31686      2,720  12427     6,937 Direct Hidden Pair
 *    193,085,300  30738      6,281    885   218,175 Direct Naked Triple
 *    177,376,400  30684      5,780   1772   100,099 Direct Hidden Triple
 *     98,943,400  30550      3,238  18835     5,253 Locking
 *     50,272,000  21338      2,355   4482    11,216 Naked Pair
 *     43,637,400  20193      2,161   8564     5,095 Hidden Pair
 *     97,388,500  18592      5,238   1286    75,729 Naked Triple
 *     87,823,600  18250      4,812   1063    82,618 Hidden Triple
 *     60,087,100  18052      3,328   1482    40,544 Two String Kite
 *     38,460,700  16570      2,321    547    70,312 Swampfish
 *     81,016,300  16336      4,959    667   121,463 XY-Wing
 *     56,855,500  15865      3,583    320   177,673 XYZ-Wing
 *     62,354,100  15568      4,005    476   130,996 W-Wing
 *     44,842,600  15245      2,941    423   106,010 Skyscraper
 *     53,712,800  15020      3,576    499   107,640 Empty Rectangle
 *     62,065,800  14521      4,274    256   242,444 Swordfish
 *    154,807,000  14450     10,713    125 1,238,456 Naked Quad
 *     17,402,300  14427      1,206      7 2,486,042 Jellyfish
 *  1,552,320,800  14424    107,620    892 1,740,269 Unique Rectangle
 *    405,309,500  14009     28,932    433   936,049 Coloring
 * 19,401,925,500  13707  1,415,475   8433 2,300,714 ALS-XZ
 * 26,851,409,800   8032  3,343,054   8958 2,997,478 Unary Chain
 * 14,193,857,900   5360  2,648,107   9798 1,448,648 Multiple Chain
 * 12,664,236,000   1745  7,257,441   9572 1,323,050 Dynamic Chain
 *     99,550,000      3 33,183,333     30 3,318,333 Dynamic Plus
 * 76,760,088,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1466  121,777,266,200 (02:01)   83,067,712
 * NOTES:
 * 1. Last top1465 02:01 is expected for these hinters. Note that none of the
 *    funky stuff is enabled, so we're producing a solution, but it's nowhere
 *    near as simple as the simplest possible solution.
 * 2. Release 6.30.095 2020-10-17 22:25:35
 *    => DiufSudoku_V6_30.095.2020-10-17.7z
 * 3. Next, I don't know. I really want the complex fish to be fast enough for
 *    practical use, but haven't got a clue how.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-10-18 06:28</b> I ran it again with the big fish overnight.
 * <p>
 * Note that all the fish are enabled, including the big ones, so this run
 * produced the simplest possible solution using my implemented hinter set.
 * <p>
 * Note that there are other hint-types, like Death Blossom that I refuse to
 * implement, because I don't want to tempt fate, and also because I just don't
 * like them, mainly because I didn't invent them, that and I am too stupid to
 * understand them, unlike my large aligned sets (which are a bit s__t). Sigh.
 * <p>
 * In a low-koo:<pre>
 *     My stuff is stuff.
 *     Your stuff is s__t.
 *     Some of my stuff could possibly nearly be s__t.
 *     Some of your stuff is NOT total complete utter s__t.
 *     Brains suck!
 *     Sigh.
 * </pre>
 * <p>
 * I simply haven't invested the time required to understand them in order
 * to re-implement them on my boy-racer Sudoku model, nor do I intend to.
 * <p>
 * And I must argue the toss over the "difficulty" of Krakens. Krakens come
 * before chains, which implies that fish and chains is simpler than chains.
 * It isn't, because it can't be! It's an oximoron ya muppets! It simply can
 * not be, or logic disappears up an eleph... oh wait, can I bum a lift. This
 * planet's overrun by half-wits. Conservative. Tub-mulcher. Out.
 * <p>
 * Krakens are more likely than chains (once a fish is found), but are also
 * more complex; so they end-up taking longer to calculate. On a computer it
 * is faster to look for all possible chains (lower hit-rate), than to find
 * fish (which is very expensive) and then look for chains with-in them (for
 * a higher hit-rate). I guess Kraken was invented by a manual Sudokuist.
 * It is just a way of improving chain hit-rate (to merely frightfully low);
 * and so may end-up taking less time MANUALLY, but more time on a computer.
 * Hence I argue against the inclusion of Kraken in the "Standard Heuristics
 * (for the) International Technique To Enable Rating (of) Sudokus" (SHITTERS).
 * <p>
 * How hard's that sheep? That's a goat ya muppet! Sigh.
 * <pre>
 *          time(ns)  calls     time/call  elims       time/elim hinter
 *        20,296,200 117277           173 670290              30 Naked Single
 *        15,243,300  50248           303 170580              89 Hidden Single
 *        99,398,000  33190         2,994   5878          16,910 Direct Naked Pair
 *        87,858,800  32776         2,680  12495           7,031 Direct Hidden Pair
 *       208,112,600  31820         6,540    867         240,037 Direct Naked Triple
 *       192,446,300  31766         6,058   1862         103,354 Direct Hidden Triple
 *        97,990,400  31624         3,098  18541           5,285 Locking
 *        55,723,500  22418         2,485   4514          12,344 Naked Pair
 *        48,331,100  21243         2,275   8519           5,673 Hidden Pair
 *       108,210,000  19612         5,517   1310          82,603 Naked Triple
 *        97,975,100  19230         5,094   1001          97,877 Hidden Triple
 *        62,023,400  19040         3,257   1370          45,272 Two String Kite
 *        41,343,500  17670         2,339    461          89,682 Swampfish
 *        87,421,700  17463         5,006    657         133,061 XY-Wing
 *        61,597,900  16998         3,623    315         195,548 XYZ-Wing
 *        68,680,600  16706         4,111    465         147,700 W-Wing
 *        48,010,000  16390         2,929    348         137,959 Skyscraper
 *        55,574,000  16204         3,429    476         116,752 Empty Rectangle
 *        70,783,800  15728         4,500    246         287,739 Swordfish
 *       178,971,100  15658        11,430    101       1,771,991 Naked Quad
 *        19,761,300  15639         1,263      5       3,952,260 Jellyfish
 *     1,590,473,700  15638       101,705    853       1,864,564 Unique Rectangle
 *     2,887,174,300  15239       189,459    490       5,892,192 Finned Swampfish
 *     6,736,885,600  14826       454,396    410      16,431,428 Finned Swordfish
 *     8,200,674,300  14492       565,875     28     292,881,225 Finned Jellyfish
 *       403,463,400  14468        27,886    107       3,770,685 Coloring
 *    21,155,390,600  14385     1,470,656   8450       2,503,596 ALS-XZ
 *    22,790,784,200   8642     2,637,211   4316       5,280,533 ALS-Wing
 *     8,719,202,100   5034     1,732,062    463      18,831,969 ALS-Chain
 *     3,899,865,800   4663       836,342      0               0 Franken Swampfish
 *    19,323,064,200   4663     4,143,912    172     112,343,396 Franken Swordfish
 *    48,905,550,600   4521    10,817,418     97     504,180,934 Franken Jellyfish
 *    11,534,352,700   4433     2,601,929      0               0 Mutant Swampfish
 *   226,740,872,400   4433    51,148,403      2 113,370,436,200 Mutant Swordfish
 * 2,161,188,001,700   4431   487,742,722     19 113,746,736,931 Mutant Jellyfish
 *   526,161,895,000   4415   119,175,967   3548     148,298,166 Kraken Swampfish
 * 1,500,449,871,400   1680   893,124,923    134  11,197,387,100 Kraken Swordfish
 * 3,161,397,253,500   1566 2,018,772,192     13 243,184,404,115 Kraken Jellyfish &lt;&lt;==== 4 minutes per elimination
 *        88,155,100   1555        56,691      0               0 Aligned Pair
 *       637,160,400   1555       409,749      1     637,160,400 Aligned Triple
 *     9,654,420,900   1554     6,212,626     21     459,734,328 Aligned Quad
 *     6,714,383,400   1539     4,362,822      3   2,238,127,800 Aligned Pent
 *    10,006,658,200   1537     6,510,512      0               0 Aligned Hex
 *    11,894,686,800   1537     7,738,898      3   3,964,895,600 Aligned Sept
 *    13,149,680,800   1535     8,566,567      1  13,149,680,800 Aligned Oct
 *    11,767,213,500   1534     7,670,934      0               0 Aligned Nona
 *    17,478,889,600   1534    11,394,321      0               0 Aligned Dec
 *     4,022,259,700   1534     2,622,072      0               0 Unary Chain
 *     1,939,834,700   1534     1,264,559      5     387,966,940 Nishio Chain
 *     3,898,729,600   1529     2,549,855    554       7,037,418 Multiple Chain
 *     9,270,867,100   1236     7,500,701   6669       1,390,143 Dynamic Chain
 *       108,531,200      3    36,177,066     30       3,617,706 Dynamic Plus
 * 7,834,441,999,100
 * pzls         total (ns) (mmm:ss)      each (ns)
 * 1466  7,974,356,843,900 (132:54)  5,439,533,999
 * NOTES:
 * 1. Last top1465 132:54 is expected with the big fish. They're all too slow.
 * 2. Release 6.30.096 2020-10-18 06:36:36
 *    => DiufSudoku_V6_30.096.2020-10-18.7z
 * 3. Next, I don't know. I really want the complex fish to be fast enough for
 *    practical use, but haven't got a clue how.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-10-19 21:14</b> I finally found my bug in eradicating the HoDoKu types
 * from the als package; so they're gone now, except HdkAls becomes Als and the
 * DiufAls class is history.
 * <pre>
 *        time(ns)  calls   time/call  elims     time/elim hinter
 *      19,642,873 117227         167 670230            29 Naked Single
 *      14,733,129  50204         293 170320            86 Hidden Single
 *      97,894,052  33172       2,951   5867        16,685 Direct Naked Pair
 *      84,616,958  32759       2,583  12516         6,760 Direct Hidden Pair
 *     212,605,004  31801       6,685    879       241,871 Direct Naked Triple
 *     189,965,577  31746       5,983   1882       100,938 Direct Hidden Triple
 *      99,111,850  31602       3,136  18557         5,340 Locking
 *      56,588,006  22389       2,527   4513        12,538 Naked Pair
 *      47,060,198  21212       2,218   8513         5,528 Hidden Pair
 *     111,973,764  19586       5,717   1302        86,001 Naked Triple
 *      94,982,942  19207       4,945   1001        94,888 Hidden Triple
 *      64,214,373  19018       3,376   1372        46,803 Two String Kite
 *      42,220,456  17646       2,392    459        91,983 Swampfish
 *      88,701,608  17439       5,086    656       135,215 XY-Wing
 *      61,804,814  16974       3,641    312       198,092 XYZ-Wing
 *      65,817,656  16684       3,944    458       143,706 W-Wing
 *      47,423,244  16372       2,896    339       139,891 Skyscraper
 *      54,850,913  16192       3,387    483       113,562 Empty Rectangle
 *      71,287,680  15709       4,538    247       288,614 Swordfish
 *     177,676,259  15638      11,361    105     1,692,154 Naked Quad
 *      19,995,854  15618       1,280      5     3,999,170 Jellyfish
 *   1,619,396,175  15617     103,694    866     1,869,972 Unique Rectangle
 *   2,805,571,854  15211     184,443    486     5,772,781 Finned Swampfish
 *   6,533,763,660  14800     441,470    417    15,668,497 Finned Swordfish
 *   7,852,537,162  14460     543,052     28   280,447,755 Finned Jellyfish
 *     409,785,818  14436      28,386    106     3,865,903 Coloring
 *  22,481,540,313  14354   1,566,221   8456     2,658,649 ALS-XZ
 *  23,224,792,749   8609   2,697,734   4336     5,356,271 ALS-Wing
 *   9,270,007,966   4985   1,859,580    465    19,935,501 ALS-Chain
 *  18,710,811,125   4613   4,056,104    170   110,063,594 Franken Swordfish
 *  46,372,661,760   4471  10,371,876     97   478,068,677 Franken Jellyfish
 * 498,784,405,165   4383 113,799,773   3561   140,068,633 Kraken Swampfish
 *     663,162,145   1624     408,351      1   663,162,145 Aligned Triple
 *   9,776,110,727   1623   6,023,481     21   465,529,082 Aligned Quad
 *   6,899,415,726   1608   4,290,681      3 2,299,805,242 Aligned Pent
 *  12,829,874,027   1606   7,988,713      3 4,276,624,675 Aligned Sept
 *   1,988,772,191   1604   1,239,882      7   284,110,313 Nishio Chain
 *   4,070,077,903   1597   2,548,577    582     6,993,261 Multiple Chain
 *   9,499,284,194   1285   7,392,439   6961     1,364,643 Dynamic Chain
 *      98,509,901      3  32,836,633     30     3,283,663 Dynamic Plus
 * 685,613,647,771
 * pzls       total (ns) (mm:ss)     each (ns)
 * 1466  740,758,501,200 (12:20)   505,292,292
 * NOTES:
 * 1. Last top1465 12:20 mainly because Krakens, even Swampfish, are too slow.
 * 2. Release 6.30.097 2020-10-19 21:14:37
 *    => DiufSudoku_V6_30.097.2020-10-19.7z
 * 3. Next, I don't know. I still really want to make complex fish fast enough
 *    for practical use, but haven't got a clue how.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.098 2020-10-23 23:20:39</b> I went mad and cleaned-up. There should be
 * no visible changes, but LOTS of dead and dying code has been removed. The
 * only notable for users is that the Grid constructor now just calls load,
 * instead of doing it all itself the pernickety way; so the Grid constructor
 * now loads an invalid grid, doesn't "fix" it, and doesn't complain; just does
 * the best it can with what it's given. I'm a bit proud of Idx.untilFalse: I
 * worked-out how to early-exit from a forEach.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      21,350,000 116110        183 666180         32 Naked Single
 *      16,027,500  49492        323 168360         95 Hidden Single
 *      90,604,400  32656      2,774   5836     15,525 Direct Naked Pair
 *      82,856,800  32246      2,569  12311      6,730 Direct Hidden Pair
 *     198,911,400  31306      6,353    884    225,012 Direct Naked Triple
 *     182,173,000  31251      5,829   1800    101,207 Direct Hidden Triple
 *      96,345,000  31114      3,096  18533      5,198 Locking
 *      51,463,100  21965      2,342   4422     11,637 Naked Pair
 *      44,645,600  20844      2,141   8534      5,231 Hidden Pair
 *     103,800,900  19225      5,399   1303     79,663 Naked Triple
 *      92,170,200  18857      4,887   1033     89,225 Hidden Triple
 *      59,609,900  18657      3,195   1402     42,517 Two String Kite
 *      37,527,100  17255      2,174    454     82,658 Swampfish
 *      91,141,600  17050      5,345    626    145,593 XY-Wing
 *      63,983,400  16602      3,853    307    208,414 XYZ-Wing
 *      67,528,400  16314      4,139    462    146,165 W-Wing
 *      47,752,200  16006      2,983    350    136,434 Skyscraper
 *      52,989,300  15822      3,349    478    110,856 Empty Rectangle
 *      70,460,800  15344      4,592    259    272,049 Swordfish
 *     169,118,800  15268     11,076     98  1,725,702 Naked Quad
 *      19,819,100  15249      1,299      9  2,202,122 Jellyfish
 *   1,566,243,000  15246    102,731    862  1,816,987 Unique Rectangle
 *   2,610,398,000  14842    175,879    506  5,158,889 Finned Swampfish
 *   6,075,823,700  14415    421,493    438 13,871,743 Finned Swordfish
 *     410,071,100  14060     29,165    114  3,597,114 Coloring
 *  22,624,446,900  13973  1,619,154   8415  2,688,585 ALS-XZ
 *  22,317,862,600   8266  2,699,959   4336  5,147,108 ALS-Wing
 *  17,943,831,900   4658  3,852,261    180 99,687,955 Franken Swordfish
 *  13,801,295,300   4508  3,061,511   2285  6,039,954 Unary Chain
 *   5,267,275,900   3657  1,440,327    125 42,138,207 Nishio Chain
 *   9,305,461,200   3541  2,627,919   5776  1,611,056 Multiple Chain
 *  11,401,129,300   1455  7,835,827   7986  1,427,639 Dynamic Chain
 *      91,625,500      3 30,541,833     30  3,054,183 Dynamic Plus
 * 115,075,742,900
 * pzls       total (ns) (mm:ss)     each (ns)
 * 1466  157,183,193,400 (02:37)   107,219,095
 * NOTES:
 * 1. Last top1465 02:37 with none of the big fish. She'll do.
 * 2. Release 6.30.098 2020-10-23 23:20:39
 *    => DiufSudoku_V6_30.098.2020-10-23.7z
 * 3. Next, I don't know. I still really want to make complex fish fast enough
 *    for practical use, but haven't got a clue how. I'll ship this release
 *    tomorrow.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-10-24 06:10</b> Here's da hinter summary of the overnight Big Fish run.
 * <pre>
 *          time(ns)  calls     time/call  elims       time/elim hinter
 *        19,916,500 117272           169 670290              29 Naked Single
 *        14,144,500  50243           281 170580              82 Hidden Single
 *        96,236,600  33185         2,900   5878          16,372 Direct Naked Pair
 *        83,755,000  32771         2,555  12495           6,703 Direct Hidden Pair
 *       212,613,000  31815         6,682    867         245,228 Direct Naked Triple
 *       187,322,800  31761         5,897   1862         100,603 Direct Hidden Triple
 *        95,600,800  31619         3,023  18545           5,155 Locking
 *        57,089,200  22414         2,547   4518          12,635 Naked Pair
 *        47,105,000  21238         2,217   8519           5,529 Hidden Pair
 *       113,343,700  19607         5,780   1307          86,720 Naked Triple
 *        95,173,000  19226         4,950   1001          95,077 Hidden Triple
 *        60,938,000  19036         3,201   1371          44,447 Two String Kite
 *        40,572,400  17665         2,296    458          88,586 Swampfish
 *        90,708,200  17459         5,195    654         138,697 XY-Wing
 *        62,958,500  16997         3,704    315         199,868 XYZ-Wing
 *        69,102,300  16705         4,136    468         147,654 W-Wing
 *        50,698,300  16388         3,093    348         145,684 Skyscraper
 *        53,358,100  16202         3,293    476         112,096 Empty Rectangle
 *        72,148,300  15726         4,587    246         293,285 Swordfish
 *       179,463,100  15656        11,462    101       1,776,862 Naked Quad
 *        20,254,200  15637         1,295      5       4,050,840 Jellyfish
 *     1,590,946,900  15636       101,748    853       1,865,119 Unique Rectangle
 *     2,813,078,900  15237       184,621    489       5,752,717 Finned Swampfish
 *     6,599,354,100  14825       445,150    413      15,979,065 Finned Swordfish
 *       437,411,100  14488        30,191    111       3,940,640 Coloring
 *    23,207,884,400  14401     1,611,546   8457       2,744,221 ALS-XZ
 *    23,854,121,400   8653     2,756,745   4321       5,520,509 ALS-Wing
 *     9,678,544,600   5040     1,920,346    464      20,858,932 ALS-Chain
 *     3,699,595,300   4668       792,543      0               0 Franken Swampfish
 *    18,476,607,300   4668     3,958,142    174     106,187,398 Franken Swordfish
 *    46,635,221,500   4525    10,306,126    103     452,769,140 Franken Jellyfish
 *    11,043,508,000   4433     2,491,204      0               0 Mutant Swampfish
 *   218,558,057,100   4433    49,302,516      2 109,279,028,550 Mutant Swordfish
 * 2,119,058,467,000   4431   478,234,815     19 111,529,393,000 Mutant Jellyfish
 *   512,096,844,400   4415   115,990,225   3548     144,333,947 Kraken Swampfish
 * 1,446,227,434,500   1680   860,849,663    134  10,792,742,048 Kraken Swordfish
 * 3,057,083,843,900   1566 1,952,160,819     13 235,160,295,684 Kraken Jellyfish
 *        89,581,400   1555        57,608      0               0 Aligned Pair
 *       681,555,700   1555       438,299      1     681,555,700 Aligned Triple
 *     9,721,451,600   1554     6,255,760     21     462,926,266 Aligned Quad
 *     6,748,245,900   1539     4,384,825      3   2,249,415,300 Aligned Pent
 *     9,444,140,700   1537     6,144,528      0               0 Aligned Hex
 *    11,271,409,600   1537     7,333,382      3   3,757,136,533 Aligned Sept
 *    11,585,641,600   1535     7,547,649      1  11,585,641,600 Aligned Oct
 *     9,451,744,100   1534     6,161,502      0               0 Aligned Nona
 *     8,574,469,200   1534     5,589,614      0               0 Aligned Dec
 *     3,811,023,900   1534     2,484,370      0               0 Unary Chain
 *     2,326,741,000   1534     1,516,780      5     465,348,200 Nishio Chain
 *     3,709,807,700   1529     2,426,296    554       6,696,403 Multiple Chain
 *     9,447,225,800   1236     7,643,386   6669       1,416,588 Dynamic Chain
 *       101,837,500      3    33,945,833     30       3,394,583 Dynamic Plus
 * 7,589,748,297,600
 * pzls        total (ns)  (mm:ss)         each (ns)
 * 1466 7,735,294,323,600 (128:55)     5,276,462,703
 * NOTES:
 * 1. Big Fish top1465 in 128:55 is 4 minutes faster than previous 132:54.
 *    So now it just needs to get 1 hour and 19 minutes faster, so I'll write
 *    a batch processor in HoDoKu, to run it over top1465, to compare times.
 * </pre>
 * <hr>
 * <p>
 * <pre>
 * <b>KRC 2020-10-26</b> FYI I ran HoDoKu over top1465.d5.mt
 * 1465 puzzles in 6,932,786 ms (01:55:32.786) at 4,732.277 ms/puzzle
 * with a minor performance tweaks, like a modified IntIntTreeMap.
 *
 * 2020-10-29 21:12 FYI I fixed the performance issues, and I'm chuffed!
 * 1465 puzzles in   249,098 ms (00:04:09.098) at   170.033 ms per puzzle
 * That's 27.832 times faster!
 *
 * 2020-11-26 I released HoDoKu the other day when I got it down to 2 minutes,
 * seriously quick... my goal weight. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-11-29 08:45</b> This is a redo of the above BIG FISH run, with lessons
 * from a month of performance tuning HoDoKu, especially the caching in
 * {@link diuf.sudoku.solver.hinters.fish.KrakenFisherman#kt2Search}, plus bug
 * fixes from generate.
 * <pre>
 *          time(ns)  calls   time/call  elims       time/elim hinter
 *        21,819,590 117272         186 670290              32 Naked Single
 *        15,036,451  50243         299 170580              88 Hidden Single
 *        93,321,458  33185       2,812   5878          15,876 Direct Naked Pair
 *        82,905,576  32771       2,529  12495           6,635 Direct Hidden Pair
 *       221,099,273  31815       6,949    867         255,016 Direct Naked Triple
 *       193,055,062  31761       6,078   1862         103,681 Direct Hidden Triple
 *       103,184,992  31619       3,263  18545           5,564 Locking
 *        52,979,047  22414       2,363   4518          11,726 Naked Pair
 *        51,014,509  21238       2,402   8519           5,988 Hidden Pair
 *       114,752,135  19607       5,852   1307          87,798 Naked Triple
 *        99,457,046  19226       5,173   1001          99,357 Hidden Triple
 *        63,005,966  19036       3,309   1371          45,956 Two String Kite
 *        40,367,994  17665       2,285    458          88,139 Swampfish
 *        92,129,306  17459       5,276    654         140,870 XY-Wing
 *        63,797,777  16997       3,753    315         202,532 XYZ-Wing
 *        70,274,928  16705       4,206    468         150,160 W-Wing
 *        47,908,318  16388       2,923    348         137,667 Skyscraper
 *        57,935,331  16202       3,575    476         121,712 Empty Rectangle
 *        70,791,370  15726       4,501    246         287,769 Swordfish
 *       189,391,572  15656      12,097    101       1,875,164 Naked Quad
 *        19,690,873  15637       1,259      5       3,938,174 Jellyfish
 *     1,603,139,741  15636     102,528    853       1,879,413 Unique Rectangle
 *     2,748,132,949  15237     180,359    489       5,619,903 Finned Swampfish
 *     6,460,599,565  14825     435,790    413      15,643,098 Finned Swordfish
 *       414,088,263  14488      28,581    111       3,730,524 Coloring
 *    22,716,806,412  14401   1,577,446   8457       2,686,154 ALS-XZ
 *    23,837,744,113   8653   2,754,853   4321       5,516,719 ALS-Wing
 *     9,444,795,590   5040   1,873,967    464      20,355,162 ALS-Chain
 *     3,661,343,093   4668     784,349      0               0 Franken Swampfish
 *    18,332,357,955   4668   3,927,240    174     105,358,379 Franken Swordfish
 *    46,511,952,622   4525  10,278,884    103     451,572,355 Franken Jellyfish
 *    10,916,885,082   4433   2,462,640      0               0 Mutant Swampfish
 *   215,801,070,442   4433  48,680,593      2 107,900,535,221 Mutant Swordfish
 * 2,091,340,950,760   4431 471,979,451     19 110,070,576,355 Mutant Jellyfish
 *    40,891,290,852   4415   9,261,900   3548      11,525,166 Kraken Swampfish
 *   127,131,133,182   1680  75,673,293    134     948,739,799 Kraken Swordfish
 *   286,748,649,905   1566 183,108,971     13  22,057,588,454 Kraken Jellyfish
 *        93,533,104   1555      60,149      0               0 Aligned Pair
 *       686,664,453   1555     441,584      1     686,664,453 Aligned Triple
 *     9,904,004,616   1554   6,373,233     21     471,619,267 Aligned Quad
 *     6,900,341,131   1539   4,483,652      3   2,300,113,710 Aligned Pent
 *     9,853,232,503   1537   6,410,691      0               0 Aligned Hex
 *    12,225,485,825   1537   7,954,122      3   4,075,161,941 Aligned Sept
 *    12,488,862,604   1535   8,136,066      1  12,488,862,604 Aligned Oct
 *     9,864,663,599   1534   6,430,680      0               0 Aligned Nona
 *    10,071,379,096   1534   6,565,436      0               0 Aligned Dec
 *     3,945,626,498   1534   2,572,116      0               0 Unary Chain
 *     2,352,680,424   1534   1,533,689      5     470,536,084 Nishio Chain
 *     3,773,231,788   1529   2,467,777    554       6,810,887 Multiple Chain
 *     9,647,088,397   1236   7,805,087   6669       1,446,556 Dynamic Chain
 *       102,779,500      3  34,259,833     30       3,425,983 Dynamic Plus
 * 3,002,234,432,638
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 3,084,561,893,599 (51:24)     2,105,502,999
 * NOTES:
 * 1. Big Fish top1465 in 51:24 down from 128:55 is better than twice as fast!
 *    Kraken Jellyfish (for eg) down from 3,057,083,843,900 to 286,748,649,905
 *    or 9.38% of the time; but the Mutants are still FAR too slow.
 * 2. Release 6.30.099 2020-11-29 08:45:39 =>
 *    DiufSudoku_V6_30.099.2020-11-29.7z
 * 3. Next I need to enspeedonate mutants. Wish me luck!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-11-30 07:01</b> Disabled mutant hinters by commenting out Tech's.
 * <pre>
          time(ns)	  calls	     time/call	  elims	     time/elim	hinter
        32,559,400	 117263	           277	 670380	            48	Naked Single
        19,543,000	  50225	           389	 170470	           114	Hidden Single
       122,345,400	  33178	         3,687	   5878	        20,814	Direct Naked Pair
       116,933,600	  32764	         3,568	  12506	         9,350	Direct Hidden Pair
       272,554,600	  31807	         8,569	    867	       314,365	Direct Naked Triple
       245,489,200	  31753	         7,731	   1862	       131,841	Direct Hidden Triple
       126,843,100	  31611	         4,012	  18555	         6,836	Locking
        75,884,100	  22401	         3,387	   4515	        16,807	Naked Pair
        68,100,900	  21226	         3,208	   8521	         7,992	Hidden Pair
       162,905,900	  19594	         8,314	   1307	       124,641	Naked Triple
       146,138,900	  19213	         7,606	   1001	       145,992	Hidden Triple
        76,554,300	  19023	         4,024	   1369	        55,919	Two String Kite
        52,243,100	  17654	         2,959	    458	       114,067	Swampfish
       105,340,800	  17448	         6,037	    653	       161,318	XY-Wing
        73,268,200	  16987	         4,313	    314	       233,338	XYZ-Wing
        86,302,200	  16696	         5,169	    469	       184,013	W-Wing
        57,897,800	  16379	         3,534	    346	       167,334	Skyscraper
        65,537,000	  16195	         4,046	    479	       136,820	Empty Rectangle
        90,866,200	  15716	         5,781	    245	       370,882	Swordfish
       241,654,400	  15646	        15,445	    101	     2,392,617	Naked Quad
        25,889,200	  15627	         1,656	      5	     5,177,840	Jellyfish
     2,205,016,200	  15626	       141,112	    853	     2,585,013	Unique Rectangle
     3,655,387,400	  15227	       240,059	    488	     7,490,547	Finned Swampfish
     8,632,172,500	  14816	       582,625	    416	    20,750,414	Finned Swordfish
       522,987,700	  14476	        36,127	    111	     4,711,600	Coloring
    28,152,570,600	  14389	     1,956,534	   8462	     3,326,940	ALS-XZ
    28,431,019,300	   8638	     3,291,389	   4325	     6,573,646	ALS-Wing
    11,074,357,500	   5025	     2,203,852	    464	    23,867,149	ALS-Chain
    23,194,288,100	   4653	     4,984,802	    174	   133,300,506	Franken Swordfish
    56,661,879,800	   4510	    12,563,609	    104	   544,825,767	Franken Jellyfish
    46,970,766,600	   4417	    10,634,087	   3566	    13,171,835	Kraken Swampfish
   142,911,774,600	   1666	    85,781,377	    136	 1,050,821,872	Kraken Swordfish
       776,691,200	   1550	       501,091	      1	   776,691,200	Aligned Triple
    11,420,147,900	   1549	     7,372,593	     21	   543,816,566	Aligned Quad
     4,788,803,800	   1534	     3,121,775	      0	             0	Unary Chain
     2,596,785,600	   1534	     1,692,819	      6	   432,797,600	Nishio Chain
     4,418,402,400	   1528	     2,891,624	    551	     8,018,879	Multiple Chain
    10,897,021,800	   1238	     8,802,117	   6680	     1,631,290	Dynamic Chain
        92,972,300	      3	    30,990,766	     30	     3,099,076	Dynamic Plus
   389,667,896,600
 pzls	       total (ns)	(mm:ss)	        each (ns)
 1465	  452,343,584,200	(07:32)	      308,766,951
 * NOTES:
 * 1. BIG FISH minus Mutants, so top1465 in 07:32 from 128:55 is fast enough;
 *    but mutants commented-out coz 29 minutes for 20 hints is unacceptable.
 * 2. Release 6.30.099 2020-11-30 07:01:39 =>
 *    DiufSudoku_V6_30.099.2020-11-30.7z
 * 3. Next I do not know, I guess I'll just have a poke around and try to find
 *    something to play with.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-12-05 10:05:40</b> Polished KrakenFisherman. Added nonHinters filter to
 * A5+E; which is what I SHOULD have done instead of all those months trying to
 * work-out how to do it faster, so the ideal implementation now would be an
 * A*E class implementing A5678910E, and another simpler one for A234E. The key
 * to this is the IntIntHashMap I wrote originally for HoDoKu.
 * <pre>
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      24,769,400 116325        212 666320             37 Naked Single
 *      18,831,700  49693        378 169200            111 Hidden Single
 *     102,668,300  32773      3,132   5845         17,565 Direct Naked Pair
 *      88,479,900  32362      2,734  12431          7,117 Direct Hidden Pair
 *     228,678,800  31411      7,280    879        260,157 Direct Naked Triple
 *     198,607,600  31356      6,333   1856        107,008 Direct Hidden Triple
 *     107,634,500  31214      3,448  18557          5,800 Locking
 *      57,219,500  22031      2,597   4433         12,907 Naked Pair
 *      48,163,300  20911      2,303   8534          5,643 Hidden Pair
 *     122,302,300  19280      6,343   1284         95,251 Naked Triple
 *      99,030,800  18915      5,235   1020         97,089 Hidden Triple
 *      68,221,900  18716      3,645   1385         49,257 Two String Kite
 *      43,689,400  17331      2,520    457         95,600 Swampfish
 *     101,621,400  17127      5,933    608        167,140 XY-Wing
 *      72,454,600  16691      4,340    301        240,712 XYZ-Wing
 *      81,876,000  16408      4,990    457        179,159 W-Wing
 *      54,415,300  16100      3,379    352        154,588 Skyscraper
 *      60,913,900  15915      3,827    487        125,079 Empty Rectangle
 *      74,754,200  15428      4,845    253        295,471 Swordfish
 *     200,978,500  15356     13,087     97      2,071,943 Naked Quad
 *     149,683,700  15338      9,759     13     11,514,130 Hidden Quad
 *      21,947,800  15336      1,431      8      2,743,475 Jellyfish
 *   1,754,224,500  15334    114,400    870      2,016,350 Unique Rectangle
 *   2,802,480,600  14926    187,758    503      5,571,532 Finned Swampfish
 *   6,438,346,700  14503    443,932    435     14,800,797 Finned Swordfish
 *   7,695,808,900  14151    543,834     23    334,600,386 Finned Jellyfish
 *     470,788,400  14132     33,313    109      4,319,159 Coloring
 *  24,086,353,600  14047  1,714,697   8382      2,873,580 ALS-XZ
 *  24,559,084,700   8337  2,945,794   4316      5,690,242 ALS-Wing
 *   9,562,743,400   4749  2,013,633    456     20,970,928 ALS-Chain
 *  17,887,496,800   4386  4,078,316    169    105,843,176 Franken Swordfish
 *  44,550,727,100   4243 10,499,817     97    459,285,846 Franken Jellyfish
 *     301,640,500   4154     72,614      1    301,640,500 Aligned Pair
 *   1,903,980,500   4153    458,459     13    146,460,038 Aligned Triple
 *  27,076,077,700   4141  6,538,536     52    520,693,801 Aligned Quad
 *  77,542,971,800   4099 18,917,533     60  1,292,382,863 Aligned Pent
 * 211,908,460,700   4049 52,335,999     27  7,848,461,507 Aligned Hex
 * 353,822,087,900   4024 87,927,954     35 10,109,202,511 Aligned Sept
 *  13,283,973,500   3996  3,324,317   1714      7,750,276 Unary Chain
 *   5,212,818,300   3330  1,565,410     28    186,172,082 Nishio Chain
 *   9,531,198,100   3302  2,886,492   5206      1,830,810 Multiple Chain
 *  11,871,733,000   1399  8,485,870   7651      1,551,657 Dynamic Chain
 *      96,624,600      3 32,208,200     30      3,220,820 Dynamic Plus
 * 854,386,564,100
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   917,968,207,800 (15:17)       626,599,459
 * NOTES:
 * 1. Last top1465 15:17 with A567E_1C, which is an improvement, I think, but
 *    do not have anything to compare it to (Poor form Mr Pan).
 * 2. Release 6.30.100 2020-12-05 10:05:40 =>
 *    DiufSudoku_V6_30.100.2020-12-05.7z
 * 3. Next I do not know, I guess I'll just have a poke around and try to find
 *    something to play with.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.101 2020-12-08 07:58:40</b> Back-up prior to Aligned*Exclusion redo.
 * This version has NonHinters64 and LongLongHashMap. Now I will try iterating
 * a stack in A*E (ala hobiwan) to remove most of the A*E classes, ie eliminate
 * my previous code-bloat.
 * <pre>
 *        time(ns)  calls   time/call  elims      time/elim hinter
 *      26,675,900 116154         229 665940             40 Naked Single
 *      18,455,300  49560         372 168790            109 Hidden Single
 *     113,502,400  32681       3,473   5847         19,412 Direct Naked Pair
 *     103,425,200  32270       3,204  12321          8,394 Direct Hidden Pair
 *     243,923,100  31329       7,785    884        275,931 Direct Naked Triple
 *     229,905,200  31274       7,351   1812        126,879 Direct Hidden Triple
 *     122,122,000  31136       3,922  18538          6,587 Locking
 *      67,579,500  21976       3,075   4414         15,310 Naked Pair
 *      60,389,400  20859       2,895   8530          7,079 Hidden Pair
 *     140,955,300  19243       7,325   1293        109,014 Naked Triple
 *     131,600,100  18878       6,971   1024        128,515 Hidden Triple
 *      77,454,300  18681       4,146   1400         55,324 Two String Kite
 *      48,889,100  17281       2,829    447        109,371 Swampfish
 *     110,141,200  17080       6,448    620        177,647 XY-Wing
 *      76,247,600  16637       4,583    305        249,992 XYZ-Wing
 *      78,223,500  16350       4,784    466        167,861 W-Wing
 *      62,230,300  16037       3,880    354        175,791 Skyscraper
 *      68,881,100  15851       4,345    481        143,203 Empty Rectangle
 *      87,912,000  15370       5,719    259        339,428 Swordfish
 *     218,211,400  15294      14,267     98      2,226,646 Naked Quad
 *     185,748,500  15275      12,160     13     14,288,346 Hidden Quad
 *      26,422,000  15273       1,729      9      2,935,777 Jellyfish
 *   1,953,527,400  15270     127,932    858      2,276,838 Unique Rectangle
 *   3,510,548,900  14868     236,114    510      6,883,429 Finned Swampfish
 *   8,121,679,500  14438     562,521    430     18,887,626 Finned Swordfish
 *   9,531,727,100  14090     676,488     21    453,891,766 Finned Jellyfish
 *     568,760,100  14073      40,414    108      5,266,297 Coloring
 *  28,675,638,400  13989   2,049,870   8358      3,430,921 ALS-XZ
 *  29,017,003,800   8291   3,499,819   4334      6,695,201 ALS-Wing
 *   4,879,550,500   4683   1,041,971      0              0 Franken Swampfish
 *  24,257,395,900   4683   5,179,883    178    136,277,505 Franken Swordfish
 *  60,410,981,500   4534  13,323,992    104    580,874,822 Franken Jellyfish
 *     388,174,100   4439      87,446      1    388,174,100 Aligned Pair
 *   2,549,628,900   4438     574,499     16    159,351,806 Aligned Triple
 *  35,523,909,500   4423   8,031,632     52    683,152,105 Aligned Quad
 * 100,576,296,500   4381  22,957,383     68  1,479,063,183 Aligned Pent
 * 324,667,762,200   4324  75,085,051     31 10,473,153,619 Aligned Hex
 * 469,436,187,500   4295 109,298,297     37 12,687,464,527 Aligned Sept
 *  18,498,397,000   4265   4,337,256   2134      8,668,414 Unary Chain
 *   6,769,638,900   3486   1,941,950     28    241,772,817 Nishio Chain
 *  12,925,889,900   3458   3,737,967   5642      2,291,012 Multiple Chain
 *  15,604,700,400   1443  10,814,068   7920      1,970,290 Dynamic Chain
 *     129,220,800      3  43,073,600     30      4,307,360 Dynamic Plus
 * 1,160,295,513,200
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 1,231,905,303,100 (20:31)       840,890,991
 * NOTES:
 * 1. Last top1465 20:31 with A567E_1C, is 5 minutes slower than previous, so
 *    Houston: I think we have a problem; but I am about to completely rewrite
 *    the problem, which will likely cause many more problems, so no worries.
 * 2. Release 6.30.101 2020-12-08 07:58:40 =>
 *    DiufSudoku_V6_30.101.2020-12-08.7z
 * 3. Next I apply hobiwans stack-iteration technique to Aligned*Exclusion.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.102 2020-12-17 10:54:42</b> Release align2.AlignedExclusion squashing
 * all Aligned*Exclusion's boiler-plate code down into one complex succinct
 * class using hobiwans stack iteration technique twice: once for cells, and
 * again for vals.
 * <pre>
 * The "old version" for comparison:
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      19,812,600 116122        170 666010             29 Naked Single
 *      14,401,500  49521        290 168620             85 Hidden Single
 *      91,215,500  32659      2,792   5847         15,600 Direct Naked Pair
 *      82,771,900  32248      2,566  12324          6,716 Direct Hidden Pair
 *     203,858,400  31307      6,511    884        230,609 Direct Naked Triple
 *     186,843,600  31252      5,978   1812        103,114 Direct Hidden Triple
 *      99,576,700  31114      3,200  18527          5,374 Locking
 *      52,314,200  21962      2,382   4410         11,862 Naked Pair
 *      44,350,400  20848      2,127   8527          5,201 Hidden Pair
 *     107,202,800  19233      5,573   1307         82,022 Naked Triple
 *      98,394,600  18863      5,216   1028         95,714 Hidden Triple
 *      62,941,000  18664      3,372   1401         44,925 Two String Kite
 *      39,629,400  17263      2,295    448         88,458 Swampfish
 *      91,374,700  17061      5,355    620        147,378 XY-Wing
 *      63,655,100  16616      3,830    303        210,082 XYZ-Wing
 *      68,472,200  16331      4,192    466        146,936 W-Wing
 *      50,511,500  16018      3,153    351        143,907 Skyscraper
 *      56,370,200  15833      3,560    480        117,437 Empty Rectangle
 *      68,918,700  15353      4,488    259        266,095 Swordfish
 *     175,257,700  15277     11,471     98      1,788,343 Naked Quad
 *     140,974,500  15258      9,239     13     10,844,192 Hidden Quad
 *      19,857,200  15256      1,301      9      2,206,355 Jellyfish
 *   1,637,667,900  15253    107,366    860      1,904,265 Unique Rectangle
 *   2,650,147,700  14850    178,461    506      5,237,446 Finned Swampfish
 *   6,019,139,500  14423    417,329    429     14,030,628 Finned Swordfish
 *   7,223,144,500  14076    513,153     21    343,959,261 Finned Jellyfish
 *     456,780,500  14059     32,490    108      4,229,449 Coloring
 *  22,284,415,200  13975  1,594,591   8358      2,666,237 ALS-XZ
 *  22,331,816,900   8276  2,698,382   4329      5,158,654 ALS-Wing
 *   3,747,979,500   4673    802,049      0              0 Franken Swampfish
 *  18,170,867,300   4673  3,888,480    178    102,083,524 Franken Swordfish
 *  44,356,454,800   4524  9,804,698    103    430,645,192 Franken Jellyfish
 *     282,292,600   4430     63,722      1    282,292,600 Aligned Pair
 *   2,100,666,900   4429    474,298     16    131,291,681 Aligned Triple
 *  31,186,149,200   4414  7,065,280     52    599,733,638 Aligned Quad
 *  69,763,009,900   4372 15,956,772     68  1,025,926,616 Aligned Pent
 *  21,958,348,900   4315  5,088,840      1 21,958,348,900 Aligned Hex
 *  20,710,004,900   4314  4,800,650      5  4,142,000,980 Aligned Sept
 *  16,488,024,100   4310  3,825,527      1 16,488,024,100 Aligned Oct
 *  17,186,799,700   4309  3,988,581      0              0 Aligned Nona
 *  12,924,583,400   4309  2,999,439      0              0 Aligned Dec
 *  13,416,250,900   4309  3,113,541   2179      6,157,067 Unary Chain
 *   5,148,660,700   3510  1,466,854     28    183,880,739 Nishio Chain
 *   9,284,748,300   3482  2,666,498   5716      1,624,343 Multiple Chain
 *  11,527,092,500   1447  7,966,200   7942      1,451,409 Dynamic Chain
 *     115,788,600      3 38,596,200     30      3,859,620 Dynamic Plus
 * 362,809,538,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  416,922,969,300 (06:56)  284,589,057
 *
 * The "new version" for edification:
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      22,967,700 116122        197 666010             34 Naked Single
 *      18,510,400  49521        373 168620            109 Hidden Single
 *     106,958,800  32659      3,275   5847         18,292 Direct Naked Pair
 *      90,391,900  32248      2,803  12324          7,334 Direct Hidden Pair
 *     229,001,100  31307      7,314    884        259,051 Direct Naked Triple
 *     198,434,500  31252      6,349   1812        109,511 Direct Hidden Triple
 *     103,989,600  31114      3,342  18527          5,612 Locking
 *      59,913,100  21962      2,728   4410         13,585 Naked Pair
 *      51,542,700  20848      2,472   8527          6,044 Hidden Pair
 *     129,033,200  19233      6,708   1307         98,724 Naked Triple
 *     106,144,500  18863      5,627   1028        103,253 Hidden Triple
 *      66,694,300  18664      3,573   1401         47,604 Two String Kite
 *      44,284,300  17263      2,565    448         98,848 Swampfish
 *     100,668,900  17061      5,900    620        162,369 XY-Wing
 *      69,403,900  16616      4,176    303        229,055 XYZ-Wing
 *      67,785,700  16331      4,150    466        145,462 W-Wing
 *      51,487,500  16018      3,214    351        146,688 Skyscraper
 *      60,154,000  15833      3,799    480        125,320 Empty Rectangle
 *      75,379,300  15353      4,909    259        291,039 Swordfish
 *     204,867,900  15277     13,410     98      2,090,488 Naked Quad
 *     153,609,200  15258     10,067     13     11,816,092 Hidden Quad
 *      21,898,600  15256      1,435      9      2,433,177 Jellyfish
 *   1,695,816,000  15253    111,179    860      1,971,879 Unique Rectangle
 *   2,988,384,000  14850    201,237    506      5,905,897 Finned Swampfish
 *   6,838,824,700  14423    474,161    429     15,941,316 Finned Swordfish
 *   8,068,908,900  14076    573,238     21    384,233,757 Finned Jellyfish
 *     466,421,300  14059     33,175    108      4,318,715 Coloring
 *  24,684,311,300  13975  1,766,319   8358      2,953,375 ALS-XZ
 *  25,874,960,300   8276  3,126,505   4329      5,977,121 ALS-Wing
 *   4,025,086,900   4673    861,349      0              0 Franken Swampfish
 *  19,970,723,300   4673  4,273,640    178    112,195,074 Franken Swordfish
 *  49,605,385,800   4524 10,964,939    103    481,605,687 Franken Jellyfish
 *     356,226,000   4430     80,412      1    356,226,000 Aligned Pair
 *   2,371,181,100   4429    535,376     15    158,078,740 Aligned Triple
 *  56,179,909,200   4415 12,724,781     53  1,059,998,286 Aligned Quad
 * 244,482,183,100   4372 55,919,986     66  3,704,275,501 Aligned Pent
 *  38,714,087,800   4316  8,969,899      3 12,904,695,933 Aligned Hex
 *  52,396,760,600   4314 12,145,748      5 10,479,352,120 Aligned Sept
 *  53,700,368,200   4310 12,459,482      1 53,700,368,200 Aligned Oct
 *  41,702,234,300   4309  9,677,937      0              0 Aligned Nona
 *  26,156,226,600   4309  6,070,138      0              0 Aligned Dec
 *  15,658,818,500   4309  3,633,979   2179      7,186,240 Unary Chain
 *   5,775,262,500   3510  1,645,373     28    206,259,375 Nishio Chain
 *  10,395,368,500   3482  2,985,459   5716      1,818,643 Multiple Chain
 *  12,421,114,000   1447  8,584,045   7942      1,563,978 Dynamic Chain
 *      99,803,900      3 33,267,966     30      3,326,796 Dynamic Plus
 * 706,661,487,900
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  763,321,961,900 (12:43)  521,038,881
 * NOTES:
 * 1. Last top1465 12:43 with A234E A5E correct and A678910E hacked, but the
 *    "existing version" of Aligned*Exclusion was just 06:56, or about half the
 *    time, so this release still uses the "old Aligned*Exclusion" classes.
 * 2. Release 6.30.102 2020-12-17 10:54:42 =>
 *    DiufSudoku_V6_30.102.2020-12-17.7z
 * 3. Next I do not know: Maybe install an elephant into my ear-wax.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.103 2021-01-01 15:17:43</b> Release "wing2" package with WXYZWing,
 * UVWXYZWing, and TUVWXYZWing boosted from Sukaku, by Nicolas Juillerat (the
 * original authors rewrite of Sudoku Explainer) who boosted them from
 * SudokuMonster.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      25,407,800 116225        218 666340            38 Naked Single
 *      15,228,900  49591        307 169120            90 Hidden Single
 *      99,431,500  32679      3,042   5821        17,081 Direct Naked Pair
 *      86,885,500  32270      2,692  12430         6,989 Direct Hidden Pair
 *     218,804,100  31320      6,986    879       248,923 Direct Naked Triple
 *     198,697,000  31265      6,355   1849       107,461 Direct Hidden Triple
 *     106,393,800  31124      3,418  18556         5,733 Locking
 *      56,337,300  21938      2,568   4424        12,734 Naked Pair
 *      48,251,300  20817      2,317   8524         5,660 Hidden Pair
 *     115,665,700  19192      6,026   1307        88,497 Naked Triple
 *     100,443,300  18827      5,335   1028        97,707 Hidden Triple
 *      68,965,300  18625      3,702   1385        49,794 Two String Kite
 *      42,976,400  17240      2,492    465        92,422 Swampfish
 *      96,676,100  17035      5,675    616       156,941 XY-Wing
 *      68,682,200  16593      4,139    302       227,424 XYZ-Wing
 *      89,508,900  16308      5,488    446       200,692 W-Wing
 *      55,333,800  16005      3,457    363       152,434 Skyscraper
 *      59,624,600  15813      3,770    486       122,684 Empty Rectangle
 *      73,613,300  15327      4,802    256       287,551 Swordfish
 *     186,562,800  15253     12,231    101     1,847,156 Naked Quad
 *     151,406,900  15234      9,938     13    11,646,684 Hidden Quad
 *      21,192,100  15232      1,391      8     2,649,012 Jellyfish
 *     600,189,300  15230     39,408   1539       389,986 WXYZ-Wing
 *   2,035,879,200  14134    144,041    786     2,590,177 UVWXYZ-Wing
 *   1,605,117,600  13700    117,161    774     2,073,795 Unique Rectangle
 *   2,646,684,700  13342    198,372    471     5,619,288 Finned Swampfish
 *   6,007,903,000  12943    464,181    406    14,797,790 Finned Swordfish
 *   7,138,501,000  12618    565,739     21   339,928,619 Finned Jellyfish
 *     440,173,000  12600     34,934    102     4,315,421 Coloring
 *   1,334,586,500  12521    106,587     86    15,518,447 TUVWXYZ-Wing
 *  21,483,821,600  12446  1,726,162   6213     3,457,882 ALS-XZ
 *  23,161,722,000   8137  2,846,469   4190     5,527,857 ALS-Wing
 *   9,496,593,100   4649  2,042,717    453    20,963,781 ALS-Chain
 *  18,096,170,500   4288  4,220,189    170   106,448,061 Franken Swordfish
 *  44,628,484,200   4145 10,766,823     96   464,880,043 Franken Jellyfish
 *     290,626,700   4057     71,635      0             0 Aligned Pair
 *   1,827,704,600   4057    450,506      5   365,540,920 Aligned Triple
 *  25,639,154,700   4052  6,327,530     11 2,330,832,245 Aligned Quad
 *  13,636,845,600   4043  3,372,952   1723     7,914,594 Unary Chain
 *   5,485,210,000   3369  1,628,141     28   195,900,357 Nishio Chain
 *   9,574,698,400   3341  2,865,818   5272     1,816,141 Multiple Chain
 *  12,114,885,700   1408  8,604,322   7714     1,570,506 Dynamic Chain
 *     125,649,400      3 41,883,133     30     4,188,313 Dynamic Plus
 * 209,356,689,400
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  262,688,715,100 (04:22)  179,309,703
 * NOTES:
 * 1. Last top1465 04:22 the new "wing2" classes. Unwanted AlignedQuad too slow
 *    and AlignedPair no hints.
 * 2. Release 6.30.103 2021-01-01 15:17:43 =>
 *    DiufSudoku_V6_30.103.2021-01-01.7z
 * 3. Next I do not know: There's nothing else to steal from Sukaku, as far as
 *    I can see. All the other techniques appear to be SLOWER chaining.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC NO-RELEASE 2021-01-03 18:05</b> I just ran EVERYthing to see how slow it was
 * and got a pleasant surprise 21:45. Not too shabby, considering A10E used to
 * take over a day on it's own. Although it looks like Franken Fish is rooted!
 * <pre>
 *          time(ns)  calls   time/call  elims      time/elim hinter
 *        22,582,300 118513         190 669380             33 Naked Single
 *        14,262,600  51575         276 171100             83 Hidden Single
 *        98,986,000  34465       2,872   5958         16,613 Direct Naked Pair
 *        89,609,100  34047       2,631  13129          6,825 Direct Hidden Pair
 *       221,650,900  33046       6,707    831        266,727 Direct Naked Triple
 *       197,558,400  32994       5,987   1817        108,727 Direct Hidden Triple
 *       531,931,000  32856      16,189  21525         24,712 Generalised Locking
 *        56,931,300  22383       2,543   4060         14,022 Naked Pair
 *        48,496,400  21037       2,305   5233          9,267 Hidden Pair
 *       113,125,600  19520       5,795   1335         84,738 Naked Triple
 *        98,772,300  19130       5,163    900        109,747 Hidden Triple
 *        58,170,800  18948       3,070   1362         42,709 Two String Kite
 *        42,210,900  17586       2,400    460         91,762 Swampfish
 *        92,622,500  17380       5,329    668        138,656 XY-Wing
 *        61,725,900  16908       3,650    324        190,512 XYZ-Wing
 *        74,356,700  16607       4,477    454        163,781 W-Wing
 *        47,060,100  16293       2,888    345        136,406 Skyscraper
 *        56,550,000  16111       3,510    468        120,833 Empty Rectangle
 *        72,761,400  15643       4,651    247        294,580 Swordfish
 *       182,376,900  15572      11,711    105      1,736,922 Naked Quad
 *       144,953,800  15552       9,320     13     11,150,292 Hidden Quad
 *        20,514,200  15550       1,319      5      4,102,840 Jellyfish
 *       534,154,800  15549      34,353   1596        334,683 WXYZ-Wing
 *       152,250,500  15549       9,791      0              0 Naked Pent
 *       126,524,100  15549       8,137      0              0 Hidden Pent
 *       820,158,000  14475      56,660   1168        702,190 VWXYZ-Wing
 *     1,066,330,800  13801      77,264    627      1,700,687 UVWXYZ-Wing
 *     1,394,610,000  13447     103,711    754      1,849,615 Unique Rectangle
 *     7,532,427,600  13099     575,038    479     15,725,318 Finned Swampfish
 *    38,440,118,200  12691   3,028,927    736     52,228,421 Finned Swordfish
 *    97,230,636,200  12081   8,048,227    186    522,745,355 Finned Jellyfish
 *       377,634,400  11914      31,696      3    125,878,133 Coloring
 *     1,131,327,600  11911      94,981     71     15,934,191 TUVWXYZ-Wing
 *    19,328,231,100  11847   1,631,487   5276      3,663,425 ALS-XZ
 *    24,872,221,100   8099   3,071,023   4040      6,156,490 ALS-Wing
 *     9,250,813,200   4715   1,961,996    444     20,835,164 ALS-Chain
 *     2,805,470,200   4360     643,456      0              0 Franken Swampfish
 *    14,848,333,100   4360   3,405,580      0              0 Franken Swordfish
 *    38,362,203,600   4360   8,798,670      0              0 Franken Jellyfish
 *     4,535,282,300   4360   1,040,202      0              0 Mutant Swampfish
 *    54,375,603,900   4360  12,471,468      1 54,375,603,900 Mutant Swordfish
 *   342,751,358,200   4359  78,630,731     11 31,159,214,381 Mutant Jellyfish
 *    43,918,261,600   4350  10,096,152   3493     12,573,221 Kraken Swampfish
 *   142,309,650,600   1660  85,728,705    134  1,062,012,317 Kraken Swordfish
 *   316,522,418,200   1546 204,736,363     12 26,376,868,183 Kraken Jellyfish
 *        33,085,600   1536      21,540      0              0 Bi-Uni Grave
 *        90,213,600   1536      58,732      0              0 Aligned Pair
 *       663,998,400   1536     432,290      1    663,998,400 Aligned Triple
 *     9,951,260,600   1535   6,482,905      2  4,975,630,300 Aligned Quad
 *     4,892,181,800   1533   3,191,247      1  4,892,181,800 Aligned Pent
 *     7,145,582,300   1532   4,664,218      0              0 Aligned Hex
 *     8,374,552,500   1532   5,466,418      3  2,791,517,500 Aligned Sept
 *     7,984,448,200   1530   5,218,593      1  7,984,448,200 Aligned Oct
 *     7,342,797,500   1529   4,802,352      0              0 Aligned Nona
 *     6,379,334,400   1529   4,172,226      0              0 Aligned Dec
 *     4,025,766,600   1529   2,632,940      0              0 Unary Chain
 *     2,452,104,900   1529   1,603,731      6    408,684,150 Nishio Chain
 *     3,899,756,100   1523   2,560,575    554      7,039,270 Multiple Chain
 *     9,782,591,700   1230   7,953,326   6627      1,476,171 Dynamic Chain
 *       120,745,500      3  40,248,500     30      4,024,850 Dynamic Plus
 * 1,238,171,648,100
 * pzls        total (ns) (mm:ss)     each (ns)
 * 1465 1,305,589,783,800 (21:45)   891,187,565
 * NOTES:
 * 1. Last top1465 21:45 with EVERthing turned on is a pleasant surprise.
 * 2. I'm working on boosting hinters from Sukaku. VWXYZ-Wing is new. My own
 *    invention based on WXYZ-Wing and UVWXYZ-Wing. I just figured there was
 *    one missing, and I was right. It's nice to guess right occassionally.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.104 2021-01-06 19:06:44</b> I'm releasing just to clean-up the logs.
 * I've implemented SueDeCoq in the als package, and also tried but failed at
 * DeathBlossom.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      25,568,483 118498        215 669470          38 Naked Single
 *      16,842,581  51551        326 170730          98 Hidden Single
 *     112,048,091  34478      3,249   5947      18,841 Direct Naked Pair
 *     103,318,435  34061      3,033  13150       7,856 Direct Hidden Pair
 *     254,925,632  33058      7,711    843     302,402 Direct Naked Triple
 *     228,001,946  33005      6,908   1827     124,795 Direct Hidden Triple
 *     522,902,893  32866     15,910  21573      24,238 Generalised Locking
 *      69,561,021  22363      3,110   4060      17,133 Naked Pair
 *      58,990,502  21018      2,806   5224      11,292 Hidden Pair
 *     145,376,439  19507      7,452   1330     109,305 Naked Triple
 *     120,932,830  19118      6,325    901     134,220 Hidden Triple
 *      62,990,001  18936      3,326   1366      46,112 Two String Kite
 *      48,692,506  17570      2,771    459     106,083 Swampfish
 *      99,480,305  17364      5,729    665     149,594 XY-Wing
 *      68,408,636  16892      4,049    323     211,791 XYZ-Wing
 *      92,363,191  16591      5,567    452     204,343 W-Wing
 *      53,194,765  16279      3,267    336     158,317 Skyscraper
 *      60,545,729  16103      3,759    472     128,274 Empty Rectangle
 *      82,740,532  15631      5,293    246     336,343 Swordfish
 *     223,550,012  15560     14,366    109   2,050,917 Naked Quad
 *     168,873,563  15539     10,867     13  12,990,274 Hidden Quad
 *      23,742,589  15537      1,528      5   4,748,517 Jellyfish
 *     569,463,259  15536     36,654   1603     355,248 WXYZ-Wing
 *     887,050,258  14459     61,349   1142     776,751 VWXYZ-Wing
 *   1,201,034,993  13790     87,094    618   1,943,422 UVWXYZ-Wing
 *   1,568,372,812  13441    116,685    761   2,060,936 Unique Rectangle
 *   9,184,559,033  13089    701,700    478  19,214,558 Finned Swampfish
 *  46,017,737,457  12680  3,629,159    739  62,270,280 Finned Swordfish
 * 115,137,870,870  12065  9,543,130    187 615,710,539 Finned Jellyfish
 *     468,255,738  11897     39,359      3 156,085,246 Coloring
 *   1,361,906,492  11894    114,503    127  10,723,673 TUVWXYZ-Wing
 *   4,245,297,629  11815    359,314    188  22,581,370 Sue De Coq
 *  22,790,969,535  11753  1,939,161   5073   4,492,601 ALS-XZ
 *  27,875,681,552   8076  3,451,669   4067   6,854,113 ALS-Wing
 *  10,773,447,427   4662  2,310,906    453  23,782,444 ALS-Chain
 *  46,684,083,403   4299 10,859,288   3499  13,342,121 Kraken Swampfish
 *   2,926,327,297   1593  1,836,991      7 418,046,756 Nishio Chain
 *   4,932,625,335   1586  3,110,104    581   8,489,888 Multiple Chain
 *  12,213,710,021   1275  9,579,380   6888   1,773,186 Dynamic Chain
 *     110,751,100      3 36,917,033     30   3,691,703 Dynamic Plus
 * 311,592,194,893
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  367,004,905,900 (06:07)  250,515,294
 * NOTES:
 * 1. Last top1465 06:07 isn't as bad as it sounds. It's a hot box.
 * 2. Release 6.30.104 2021-01-06 19:06:44 =>
 *    DiufSudoku_V6_30.104.2021-01-06.7z
 * 3. I don't really know what to do next. I've tried and failed DeathBlossom
 *    and there's nothing else worth boosting in Sukaku, so I suppose I should
 *    ship it, and while there download another open source Sudoku solver to
 *    riffle for hinters.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.105 2021-01-12 10:05:45</b> Ship SueDeCoq. Failed at DeathBlossom.
 * This Run: hinters which take less than a tenth of a second per elimination.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      23,139,900 116228        199 666800         34 Naked Single
 *      17,816,600  49548        359 168700        105 Hidden Single
 *      91,213,000  32678      2,791   5826     15,656 Direct Naked Pair
 *      84,121,200  32269      2,606  12384      6,792 Direct Hidden Pair
 *     203,384,300  31324      6,492    879    231,381 Direct Naked Triple
 *     188,806,500  31269      6,038   1824    103,512 Direct Hidden Triple
 *     101,401,300  31130      3,257  18575      5,459 Locking
 *      51,560,900  21919      2,352   4449     11,589 Naked Pair
 *      45,713,200  20785      2,199   8512      5,370 Hidden Pair
 *     106,627,300  19165      5,563   1316     81,023 Naked Triple
 *      96,365,000  18797      5,126   1031     93,467 Hidden Triple
 *      64,939,300  18594      3,492   1384     46,921 Two String Kite
 *      39,299,700  17210      2,283    465     84,515 Swampfish
 *      91,353,100  17001      5,373    636    143,636 XY-Wing
 *      60,419,900  16550      3,650    302    200,065 XYZ-Wing
 *      85,621,800  16266      5,263    445    192,408 W-Wing
 *      51,995,400  15960      3,257    353    147,295 Skyscraper
 *      57,241,900  15777      3,628    475    120,509 Empty Rectangle
 *      70,297,300  15302      4,593    251    280,068 Swordfish
 *     172,572,600  15229     11,331    100  1,725,726 Naked Quad
 *     141,726,800  15211      9,317     13 10,902,061 Hidden Quad
 *      38,774,800  15209      2,549      9  4,308,311 Jellyfish
 *     545,173,600  15206     35,852   1541    353,779 WXYZ-Wing
 *     725,849,100  14169     51,227   1161    625,193 VWXYZ-Wing
 *     963,579,500  13491     71,423    607  1,587,445 UVWXYZ-Wing
 *   1,310,054,500  13148     99,639    754  1,737,472 Unique Rectangle
 *   5,591,985,600  12799    436,908    507 11,029,557 Finned Swampfish
 *  29,681,881,200  12365  2,400,475    772 38,448,032 Finned Swordfish
 *     400,364,400  11723     34,152     26 15,398,630 Coloring
 *   1,070,354,000  11705     91,444    126  8,494,873 TUVWXYZ-Wing
 *   3,061,345,100  11625    263,341    192 15,944,505 Sue De Coq
 *  20,388,573,000  11566  1,762,802   5070  4,021,414 ALS-XZ
 *  23,088,267,600   7899  2,922,935   4084  5,653,346 ALS-Wing
 *   9,348,953,800   4489  2,082,636    443 21,103,733 ALS-Chain
 *  13,058,870,700   4137  3,156,603   1771  7,373,727 Unary Chain
 *   5,445,510,900   3443  1,581,618    120 45,379,257 Nishio Chain
 *   8,944,458,800   3332  2,684,411   5308  1,685,090 Multiple Chain
 *  11,666,919,600   1399  8,339,470   7660  1,523,096 Dynamic Chain
 *     121,196,700      3 40,398,900     30  4,039,890 Dynamic Plus
 * 137,297,729,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   184,103,275,700 (03:04)       125,667,764
 * NOTES:
 * 1. Last top1465 03:04 is OK.
 * 2. Release 6.30.105 2021-01-12 10:05:45 =>
 *    DiufSudoku_V6_30.105.2021-01-12.LOGS.7z
 * 3. I don't know what to do next. DeathBlossom is dead. I'm sick of it. I'll
 *    download another Sudoku solver to riffle for hinters.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.106 2021-01-14 09:02:46</b> Ship DeathBlossom. Last top1465 4:10 is a
 * minute slower, but that's still OK coz I have no love for DeathBlossom, so
 * I won't be using it.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      23,094,400 117626        196 665680            34 Naked Single
 *      16,439,300  51058        321 169540            96 Hidden Single
 *      96,492,100  34104      2,829   5911        16,324 Direct Naked Pair
 *      90,081,100  33690      2,673  13040         6,908 Direct Hidden Pair
 *     217,412,900  32696      6,649    843       257,903 Direct Naked Triple
 *     197,977,600  32643      6,064   1856       106,668 Direct Hidden Triple
 *     193,898,300  32501      5,965  21597         8,978 Locking Generalised
 *      53,495,800  22029      2,428   3988        13,414 Naked Pair
 *      48,407,600  20734      2,334   5253         9,215 Hidden Pair
 *     111,921,400  19216      5,824   1309        85,501 Naked Triple
 *      99,422,400  18841      5,276    922       107,833 Hidden Triple
 *      57,917,800  18647      3,106   1395        41,518 Two String Kite
 *      40,037,100  17252      2,320    475        84,288 Swampfish
 *      86,990,500  17042      5,104    624       139,407 XY-Wing
 *      57,196,900  16593      3,447    300       190,656 XYZ-Wing
 *      87,878,900  16310      5,388    444       197,925 W-Wing
 *      46,693,400  16005      2,917    372       125,519 Skyscraper
 *      57,157,300  15807      3,615    486       117,607 Empty Rectangle
 *      71,266,900  15321      4,651    253       281,687 Swordfish
 *     179,254,200  15249     11,755    100     1,792,542 Naked Quad
 *     146,438,300  15231      9,614     13    11,264,484 Hidden Quad
 *      19,994,000  15229      1,312      8     2,499,250 Jellyfish
 *     564,777,500  15227     37,090   1527       369,860 WXYZ-Wing
 *     743,396,100  14201     52,348   1189       625,228 VWXYZ-Wing
 *     994,180,600  13514     73,566    613     1,621,828 UVWXYZ-Wing
 *   1,371,170,900  13168    104,129    759     1,806,549 Unique Rectangle
 *   1,819,065,000  12816    141,937    469     3,878,603 Finned Swampfish
 *   4,191,728,200  12420    337,498    403    10,401,310 Finned Swordfish
 *   5,290,882,000  12099    437,299     20   264,544,100 Finned Jellyfish
 *     396,433,900  12082     32,811    105     3,775,560 Coloring
 *   1,115,935,600  12003     92,971    122     9,147,013 TUVWXYZ-Wing
 *  22,378,621,400  11927  1,876,299   5324     4,203,347 ALS-XZ
 *  24,872,379,400   8140  3,055,574   4143     6,003,470 ALS-Wing
 *  10,702,364,000   4684  2,284,877    448    23,889,205 ALS-Chain
 *   6,118,803,900   4324  1,415,079    194    31,540,226 Death Blossom
 *   1,321,585,400   4155    318,071     14    94,398,957 Sue De Coq
 *   2,587,270,300   4152    623,138      0             0 Franken Swampfish
 *  13,459,744,100   4152  3,241,749    167    80,597,270 Franken Swordfish
 *  35,162,415,600   4012  8,764,310     95   370,130,690 Franken Jellyfish
 *     259,995,700   3925     66,240      0             0 Aligned Pair
 *   1,740,244,000   3925    443,374      4   435,061,000 Aligned Triple
 *  23,959,957,000   3921  6,110,675      5 4,791,991,400 Aligned Quad
 *  12,781,343,100   3916  3,263,877   1604     7,968,418 Unary Chain
 *   5,330,508,400   3286  1,622,187     28   190,375,300 Nishio Chain
 *   9,014,303,700   3258  2,766,821   5166     1,744,929 Multiple Chain
 *  11,841,729,600   1386  8,543,816   7593     1,559,558 Dynamic Chain
 *     144,371,900      3 48,123,966     30     4,812,396 Dynamic Plus
 * 200,162,675,500
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   250,015,335,200 (04:10)       170,658,931
 * NOTES:
 * 1. Last top1465 4:10 is a minute slower, but that's still OK coz I no use!
 * 2. Release 6.30.106 2021-01-14 09:02:46 =>
 *    DiufSudoku_V6_30.106.2021-01-14.LOGS.7z
 * 3. I don't know what to do next. DeathBlossom is dead. I'm sick of it. I'll
 *    download another Sudoku solver to riffle for hinters.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.107 2021-01-15 09:44:47</b> ship DeathBlossom today; now faster, and
 * with a better test-case.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      21,781,600 116300        187 666420          32 Naked Single
 *      16,307,400  49658        328 169330          96 Hidden Single
 *      94,181,300  32725      2,877   5834      16,143 Direct Naked Pair
 *      81,875,000  32315      2,533  12370       6,618 Direct Hidden Pair
 *     207,465,700  31371      6,613    879     236,024 Direct Naked Triple
 *     185,944,700  31316      5,937   1846     100,728 Direct Hidden Triple
 *     100,211,500  31175      3,214  18568       5,397 Locking
 *      54,038,800  21972      2,459   4431      12,195 Naked Pair
 *      44,014,500  20841      2,111   8514       5,169 Hidden Pair
 *     110,449,400  19214      5,748   1309      84,376 Naked Triple
 *      91,796,800  18849      4,870   1029      89,209 Hidden Triple
 *      62,393,100  18645      3,346   1395      44,726 Two String Kite
 *      39,840,600  17250      2,309    475      83,874 Swampfish
 *      84,326,500  17040      4,948    624     135,138 XY-Wing
 *      55,664,600  16591      3,355    300     185,548 XYZ-Wing
 *      74,956,400  16308      4,596    444     168,820 W-Wing
 *      49,641,300  16003      3,101    372     133,444 Skyscraper
 *      55,179,300  15805      3,491    486     113,537 Empty Rectangle
 *      69,526,700  15319      4,538    253     274,809 Swordfish
 *     180,199,000  15247     11,818    100   1,801,990 Naked Quad
 *     137,875,900  15229      9,053     13  10,605,838 Hidden Quad
 *      19,200,600  15227      1,260      8   2,400,075 Jellyfish
 *     530,964,800  15225     34,874   1528     347,490 WXYZ-Wing
 *     714,379,500  14198     50,315   1189     600,823 VWXYZ-Wing
 *     934,610,600  13511     69,174    613   1,524,650 UVWXYZ-Wing
 *   1,338,064,200  13165    101,637    759   1,762,930 Unique Rectangle
 *   1,706,268,800  12813    133,167    469   3,638,099 Finned Swampfish
 *   4,049,921,500  12417    326,159    404  10,024,558 Finned Swordfish
 *   5,127,420,300  12095    423,928     20 256,371,015 Finned Jellyfish
 *     378,641,300  12078     31,349    105   3,606,107 Coloring
 *   1,058,267,100  11999     88,196    122   8,674,320 TUVWXYZ-Wing
 *  19,412,775,100  11923  1,628,178   5324   3,646,276 ALS-XZ
 *  22,519,738,000   8137  2,767,572   4143   5,435,611 ALS-Wing
 *   9,193,676,300   4681  1,964,041    449  20,475,893 ALS-Chain
 *   3,796,612,800   4320    878,845    194  19,570,169 Death Blossom
 *   1,116,020,700   4151    268,855     14  79,715,764 Sue De Coq
 *  12,787,168,100   4148  3,082,730    167  76,569,868 Franken Swordfish
 *  33,367,540,300   4008  8,325,234     95 351,237,266 Franken Jellyfish
 *  11,792,753,700   3921  3,007,588   1606   7,342,935 Unary Chain
 *   5,165,054,000   3289  1,570,402     27 191,298,296 Nishio Chain
 *   8,454,677,300   3262  2,591,869   5170   1,635,334 Multiple Chain
 *  11,354,058,800   1386  8,191,961   7593   1,495,332 Dynamic Chain
 *      86,722,700      3 28,907,566     30   2,890,756 Dynamic Plus
 * 156,722,206,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  204,625,950,300 (03:24)  139,676,416
 * NOTES:
 * 1. Last top1465 3:24. DeathBlossom takes half the time. That's better!
 * 2. Release 6.30.107 2021-01-15 09:44:47 =>
 *    DiufSudoku_V6_30.107.2021-01-15.LOGS.7z
 * 3. Dunno what to do next. I shall download some other open source Sudoku
 *    solvers to riffle for boostable hinters.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.108 2021-01-23 14:40:48</b> Faster align2.AlignedExclusion, which still
 * isn't competitive with the old align.Aligned*Exclusion classes but it's
 * getting there, and it's just SOOOO much more succinct.
 * <pre>
 * This is an oldish log (ran 2021-01-23.13-25-10) coz Im in a hurry to package
 * this release to get to the library/shops before closing; and I tried running
 * it again AFTER testing the GUI (which was fine) and its runs like a dog, but
 * it's a hot afternoon so Im blaming HOT BOX SYNDROME, and releasing anyway.
 * SOLVED: LogicalAnalyserTest hung -> registry ALL wantedHinters.
 *
 *        time(ns)  calls  time/call  elims      time/elim hinter
 *      21,125,200 116292        181 667090             31 Naked Single
 *      16,621,500  49583        335 169080             98 Hidden Single
 *      92,180,800  32675      2,821   5868         15,709 Direct Naked Pair
 *      82,174,600  32262      2,547  12333          6,662 Direct Hidden Pair
 *     205,395,300  31320      6,557    816        251,709 Direct Naked Triple
 *     190,588,800  31270      6,094   1800        105,882 Direct Hidden Triple
 *     101,525,000  31133      3,261  18590          5,461 Locking
 *      52,321,900  21943      2,384   4479         11,681 Naked Pair
 *      48,947,400  20780      2,355   8523          5,742 Hidden Pair
 *     106,841,500  19154      5,578   1304         81,933 Naked Triple
 *      97,264,000  18785      5,177   1012         96,110 Hidden Triple
 *      63,165,800  18586      3,398   1390         45,443 Two String Kite
 *      38,141,400  17196      2,218    488         78,158 Swampfish
 *      82,990,400  16984      4,886    662        125,363 XY-Wing
 *      60,639,200  16514      3,671    308        196,880 XYZ-Wing
 *      92,817,600  16223      5,721    442        209,994 W-Wing
 *      50,235,200  15916      3,156    380        132,197 Skyscraper
 *      56,050,900  15716      3,566    480        116,772 Empty Rectangle
 *      70,440,600  15236      4,623    250        281,762 Swordfish
 *     169,792,900  15165     11,196    105      1,617,075 Naked Quad
 *     143,311,200  15144      9,463     12     11,942,600 Hidden Quad
 *      19,086,600  15142      1,260      8      2,385,825 Jellyfish
 *     546,476,500  15140     36,094   1553        351,884 WXYZ-Wing
 *     692,102,400  14093     49,109   1197        578,197 VWXYZ-Wing
 *     914,003,400  13400     68,209    625      1,462,405 UVWXYZ-Wing
 *   1,074,391,300  13055     82,297    130      8,264,548 TUVWXYZ-Wing
 *   1,282,416,600  12974     98,845    771      1,663,315 Unique Rectangle
 *   1,737,675,000  12614    137,757    472      3,681,514 Finned Swampfish
 *   4,177,086,200  12215    341,963    404     10,339,322 Finned Swordfish
 *   5,175,381,700  11890    435,271     20    258,769,085 Finned Jellyfish
 *     393,965,100  11873     33,181    102      3,862,402 Coloring
 *  17,219,673,000  11796  1,459,789   1482     11,619,212 ALS-XZ
 *  28,629,366,000  11457  2,498,853   7806      3,667,610 ALS-Wing
 *   8,952,429,400   4952  1,807,841    791     11,317,862 ALS-Chain
 *   3,833,370,200   4302    891,066    235     16,312,213 Death Blossom
 *   1,159,507,200   4093    283,290     14     82,821,942 Sue De Coq
 *  12,742,933,400   4090  3,115,631    164     77,700,813 Franken Swordfish
 *  33,212,806,500   3952  8,404,050     97    342,400,067 Franken Jellyfish
 *   1,621,026,600   3863    419,628      4    405,256,650 Aligned Triple
 *   6,202,209,900   3859  1,607,206      6  1,033,701,650 Aligned Quad
 *  14,955,416,100   3853  3,881,499      4  3,738,854,025 Aligned Pent (H)
 *  25,158,004,300   3849  6,536,244      2 12,579,002,150 Aligned Hex (H)
 *  33,476,126,200   3847  8,701,878      6  5,579,354,366 Aligned Sept (H)
 *  35,599,522,400   3841  9,268,295      2 17,799,761,200 Aligned Oct (H)
 *  12,096,878,500   3839  3,151,049   1443      8,383,145 Unary Chain
 *   5,009,014,900   3240  1,545,992     27    185,519,070 Nishio Chain
 *   8,602,214,700   3213  2,677,315   4931      1,744,517 Multiple Chain
 *  11,209,682,400   1387  8,081,962   7603      1,474,376 Dynamic Chain
 *     107,329,100      3 35,776,366     30      3,577,636 Dynamic Plus
 * 277,642,666,800
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   328,779,553,500 (05:28)       224,422,903
 * NOTES:
 * 1. Last top1465 5:28 isn't too bad considering I'm using the BIG excluders.
 * 2. Release 6.30.108 2021-01-23 14:40:48 =>
 *    DiufSudoku_V6_30.108.2021-01-23.7z
 * 3. Next I keep trying to find a magic bullet for AlignedExclusion, I guess.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.109 2021-02-13 07:00:49</b> BigWings, faster Complex and Kraken fish.
 * Wrote the BigWing class to succinctify it. Tried and mostly failed to make
 * Complex and KrakenFisherman faster.
 * <pre>
 *          time(ns)  calls   time/call  elims       time/elim hinter
 *        21,807,800 117269         185 670060              32 Naked Single
 *        14,422,400  50263         286 171070              84 Hidden Single
 *        95,343,600  33156       2,875   5894          16,176 Direct Naked Pair
 *        83,516,700  32740       2,550  12444           6,711 Direct Hidden Pair
 *       215,128,600  31789       6,767    841         255,800 Direct Naked Triple
 *       192,417,600  31737       6,062   1833         104,974 Direct Hidden Triple
 *        99,914,500  31598       3,162  18561           5,383 Locking
 *        54,580,600  22382       2,438   4442          12,287 Naked Pair
 *        47,214,100  21224       2,224   8493           5,559 Hidden Pair
 *       117,523,900  19590       5,999   1334          88,098 Naked Triple
 *        98,012,900  19210       5,102   1003          97,719 Hidden Triple
 *        63,294,300  19018       3,328   1370          46,200 Two String Kite
 *        41,554,500  17648       2,354    458          90,730 Swampfish
 *        84,184,800  17442       4,826    638         131,951 XY-Wing
 *        64,785,100  16989       3,813    321         201,822 XYZ-Wing
 *        84,075,400  16690       5,037    454         185,188 W-Wing
 *        51,236,200  16375       3,128    363         141,146 Skyscraper
 *        54,642,200  16180       3,377    478         114,314 Empty Rectangle
 *        70,852,600  15702       4,512    233         304,088 Swordfish
 *       189,147,000  15635      12,097     98       1,930,071 Naked Quad
 *       146,773,800  15616       9,398     13      11,290,292 Hidden Quad
 *        19,876,700  15614       1,273      5       3,975,340 Jellyfish
 *       508,710,400  15613      32,582   1561         325,887 WXYZ-Wing
 *       659,292,600  14568      45,256   1336         493,482 VWXYZ-Wing
 *       776,200,700  13798      56,254    580       1,338,277 UVWXYZ-Wing
 *       616,226,800  13466      45,761    110       5,602,061 TUVWXYZ-Wing
 *       321,260,800  13392      23,989     14      22,947,200 STUVWXYZ-Wing
 *       414,177,000  13384      30,945    289       1,433,138 Coloring
 *     1,259,178,300  13162      95,667    739       1,703,894 Unique Rectangle
 *     1,583,241,000  12821     123,488    347       4,562,654 Finned Swampfish
 *     3,447,678,800  12545     274,824    327      10,543,360 Finned Swordfish
 *     4,570,106,100  12291     371,825     21     217,624,100 Finned Jellyfish
 *    18,612,031,400  12273   1,516,502   5231       3,558,025 ALS-XZ
 *    22,474,789,400   8525   2,636,338   4132       5,439,203 ALS-Wing
 *     9,581,130,200   5066   1,891,261    717      13,362,803 ALS-Chain
 *     4,448,100,800   4477     993,544    181      24,575,142 Death Blossom
 *     1,172,017,100   4315     271,614     11     106,547,009 Sue De Coq
 *     3,291,985,200   4313     763,270      0               0 Franken Swampfish
 *    16,694,132,200   4313   3,870,654    173      96,497,873 Franken Swordfish
 *    50,939,958,200   4169  12,218,747     86     592,325,095 Franken Jellyfish
 *     7,255,462,600   4091   1,773,518      0               0 Mutant Swampfish
 *   115,769,344,200   4091  28,298,544      1 115,769,344,200 Mutant Swordfish
 * 1,226,333,780,800   4090 299,837,110     18  68,129,654,488 Mutant Jellyfish
 *    28,782,377,300   4075   7,063,160   3150       9,137,262 Kraken Swampfish
 *    85,630,046,500   1649  51,928,469    162     528,580,533 Kraken Swordfish
 *   511,551,158,700   1516 337,434,801     36  14,209,754,408 Kraken Jellyfish
 *     3,877,368,800   1484   2,612,782      0               0 Unary Chain
 *     2,362,564,400   1484   1,592,024      3     787,521,466 Nishio Chain
 *     3,771,607,900   1481   2,546,662    527       7,156,751 Multiple Chain
 *     9,657,853,500   1206   8,008,170   6486       1,489,030 Dynamic Chain
 *        93,697,900      3  31,232,633     30       3,123,263 Dynamic Plus
 * 2,138,365,784,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 2,209,254,556,900 (36:49)     1,508,023,588
 * NOTES:
 * 1. top1465 36:49 not too bad with Jelly Mutant and Kraken, but they're both
 *    still well too slow; and I stand little chance of improving them.
 * 2. Release 6.30.109 2021-02-13 07:00:49 =>
 *    DiufSudoku_V6_30.109.2021-02-13.7z
 * 3. Next I seek magic bullets, that fire post-humourously, around corners.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.110 2021-02-17 14:31:50</b> Faster Complex and Kraken fisherman.
 * <pre>
          time(ns)  calls   time/call  elims      time/elim hinter
        23,041,800 117429         196 669940             34 Naked Single
        15,788,100  50435         313 171300             92 Hidden Single
        94,973,800  33305       2,851   5870         16,179 Direct Naked Pair
        83,394,100  32891       2,535  12388          6,731 Direct Hidden Pair
       216,371,200  31945       6,773    797        271,482 Direct Naked Triple
       191,226,600  31897       5,995   1832        104,381 Direct Hidden Triple
       101,060,100  31758       3,182  18594          5,435 Locking
        54,868,800  22542       2,434   4430         12,385 Naked Pair
        47,040,900  21382       2,200   8498          5,535 Hidden Pair
       115,665,700  19743       5,858   1306         88,564 Naked Triple
        97,463,500  19367       5,032   1001         97,366 Hidden Triple
        65,637,800  19176       3,422   1414         46,419 Two String Kite
        40,103,600  17762       2,257    481         83,375 Swampfish
        80,755,000  17549       4,601    636        126,973 XY-Wing
        61,988,300  17097       3,625    316        196,165 XYZ-Wing
        93,503,300  16803       5,564    466        200,650 W-Wing
        51,301,500  16483       3,112    401        127,933 Skyscraper
        56,766,000  16272       3,488    488        116,323 Empty Rectangle
        73,680,100  15784       4,668    244        301,967 Swordfish
       187,735,800  15712      11,948     98      1,915,671 Naked Quad
       146,247,300  15693       9,319      2     73,123,650 Hidden Quad
        20,085,400  15692       1,279      5      4,017,080 Jellyfish
       514,308,100  15691      32,777   1554        330,957 WXYZ-Wing
       657,400,900  14651      44,870   1336        492,066 VWXYZ-Wing
       778,480,000  13888      56,054    585      1,330,735 UVWXYZ-Wing
       620,761,200  13554      45,799    106      5,856,237 TUVWXYZ-Wing
       322,312,100  13480      23,910     11     29,301,100 STUVWXYZ-Wing
       396,127,200  13473      29,401    335      1,182,469 Coloring
     1,288,482,000  13226      97,420    751      1,715,688 Unique Rectangle
    19,871,931,200  12880   1,542,851   5387      3,688,867 ALS-XZ
    26,439,586,300   9053   2,920,533   4346      6,083,659 ALS-Wing
    10,585,436,600   5416   1,954,475    762     13,891,649 ALS-Chain
     4,640,782,000   4792     968,443    187     24,817,016 Death Blossom
     1,290,547,000   4625     279,037     11    117,322,454 Sue De Coq
     6,333,565,700   4623   1,370,012    233     27,182,685 Mutant Swampfish
   103,223,378,900   4427  23,316,778    305    338,437,307 Mutant Swordfish
   997,617,680,500   4158 239,927,292     91 10,962,831,653 Mutant Jellyfish
    29,202,869,600   4075   7,166,348   3150      9,270,752 Kraken Swampfish
    83,093,202,600   1649  50,390,056    162    512,921,003 Kraken Swordfish
   492,228,962,800   1516 324,689,289     36 13,673,026,744 Kraken Jellyfish
     3,425,040,300   1484   2,307,978      0              0 Unary Chain
     2,262,745,900   1484   1,524,761      3    754,248,633 Nishio Chain
     3,244,037,800   1481   2,190,437    527      6,155,669 Multiple Chain
     8,804,261,500   1206   7,300,382   6486      1,357,425 Dynamic Chain
        91,686,300      3  30,562,100     30      3,056,210 Dynamic Plus
 1,798,852,285,200
 pzls        total (ns) (mm:ss)         each (ns)
 1465 1,863,915,262,300 (31:03)     1,272,297,107
 * NOTES:
 * 1. top1465 31:03 is about 7 minutes faster than 36:49. Mutant and Kraken
 *    Jellyfish are still "too slow" to use in the GUI, but the rest is OK.
 * 2. Release 6.30.110 2021-02-17 14:31:50 =>
 *    DiufSudoku_V6_30.110.2021-02-17.7z
 * 3. I give-up on ComplexFisherman and KrakenFisherman. They're just too slow!
 *    So I really don't know what I'll tackle next... maybe nothing... making
 *    this faster has ceased to be fun coz 90% of attempts are failures; so I'm
 *    starting to really struggle to see how it can be done any faster; which
 *    to me means it MAY not be able to be done much faster; or not. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.111 2021-02-23 08:33:50</b> Oops! Last release used align2. My bad.
 * <pre>
 * NOTES:
 * 1. There's no "serious" code changes since last release, so no log posted.
 * 2. Release 6.30.111 2021-02-23 08:33:50 =>
 *    DiufSudoku_V6_30.111.2021-02-23.7z
 * 3. Next I don't know. I'll keep java-docing and see if anything grabs me.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.112 2021-03-04 10:59:52</b> Hidden Unique Rectangles and XColoring.
 * Last top1465 run took 04:36, which is over a minute slower than previous,
 * which is a bit of a worry, but still acceptable.
 * <p>
 * Both Hidden Unique Rectangles and XColoring are a bit slower than the more
 * complex techniques they're pinching hints off, but they ARE simpler, and my
 * goal is to provide an explanation of the SIMPLEST possible solution to all
 * possible Sudoku puzzles, and to do so as quickly as possible; which is more
 * akin to NASA than to F1, so I just have to accept there tardiness, for now.
 * <pre>
 *        time(ns)  calls  time/call  elims     time/elim hinter
 *      34,997,400 118882        294 678290            51 Naked Single
 *      26,903,000  51053        526 172830           155 Hidden Single
 *     146,774,000  33770      4,346  21115         6,951 Locking
 *      85,115,500  23445      3,630   7400        11,502 Naked Pair
 *      75,631,100  21774      3,473  13293         5,689 Hidden Pair
 *     183,009,300  19599      9,337   1733       105,602 Naked Triple
 *     165,804,200  19175      8,646   1174       141,230 Hidden Triple
 *      78,359,200  18957      4,133   1396        56,131 Two String Kite
 *      51,898,800  17561      2,955    474       109,491 Swampfish
 *     108,727,100  17352      6,265    632       172,036 XY-Wing
 *      77,078,000  16896      4,561    303       254,382 XYZ-Wing
 *     120,274,900  16611      7,240    440       273,352 W-Wing
 *      63,422,600  16304      3,890    381       166,463 Skyscraper
 *      65,661,300  16100      4,078    473       138,818 Empty Rectangle
 *      91,866,600  15627      5,878    245       374,965 Swordfish
 *     231,011,700  15560     14,846    100     2,310,117 Naked Quad
 *     194,945,900  15541     12,543     13    14,995,838 Hidden Quad
 *      27,187,600  15539      1,749      8     3,398,450 Jellyfish
 *     678,511,600  15537     43,670   1542       440,020 WXYZ-Wing
 *     924,726,400  14504     63,756   1353       683,463 VWXYZ-Wing
 *   1,102,790,100  13720     80,378    583     1,891,578 UVWXYZ-Wing
 *     857,015,500  13395     63,980    130     6,592,426 TUVWXYZ-Wing
 *     435,847,100  13316     32,731     13    33,526,700 STUVWXYZ-Wing
 *   2,010,105,600  13308    151,044   1183     1,699,159 XColoring
 *   2,942,716,600  12920    227,764   1127     2,611,106 Unique Rectangle
 *   1,724,855,900  12150    141,963    287     6,009,950 Finned Swampfish
 *   4,118,330,300  11907    345,874    289    14,250,277 Finned Swordfish
 *   5,366,470,400  11691    459,025     17   315,674,729 Finned Jellyfish
 *  23,039,613,600  11677  1,973,076   5149     4,474,580 ALS-XZ
 *  29,352,683,400   8001  3,668,626   4050     7,247,576 ALS-Wing
 *  11,862,166,700   4618  2,568,680    678    17,495,821 ALS-Chain
 *   5,268,072,200   4069  1,294,684    171    30,807,439 Death Blossom
 *   1,437,836,100   3916    367,169     11   130,712,372 Sue De Coq
 *   3,269,564,800   3914    835,351      3 1,089,854,933 Franken Swampfish
 *  17,768,779,900   3911  4,543,283    138   128,759,274 Franken Swordfish
 *  53,714,697,400   3796 14,150,341     70   767,352,820 Franken Jellyfish
 *  14,038,483,500   3732  3,761,651   1428     9,830,870 Unary Chain
 *   5,862,757,700   3163  1,853,543     23   254,902,508 Nishio Chain
 *   9,608,375,300   3140  3,059,992   4764     2,016,871 Multiple Chain
 *  12,447,541,600   1373  9,065,944   7508     1,657,903 Dynamic Chain
 *      93,084,200      3 31,028,066     30     3,102,806 Dynamic Plus
 * 209,753,694,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  276,324,127,400 (04:36)  188,617,151
 * NOTES:
 * 1. Last top1465 took 04:36, as discussed above.
 * 2. Release 6.30.112 2021-03-04 10:59:52 =>
 *    DiufSudoku_V6_30.112.2021-03-04.7z
 * 3. Next I shall search sudopedia for more techniques to implement, or not.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.113 2021-03-11 16:38:53</b> 3D Medusa Coloring that finds a "few more"
 * hints than the standard algorithm on sudopedia by also coloring the last
 * remaining v in each effected box, so long as it also reduces the origin
 * cells box to one place for v, to form a "strong" (bidirectional) link.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      23,001,100 115665        198 653390          35 Naked Single
 *      16,205,100  50326        322 170750          94 Hidden Single
 *     105,393,800  33251      3,169  21067       5,002 Locking
 *      58,487,000  22960      2,547   7392       7,912 Naked Pair
 *      50,848,400  21308      2,386  13287       3,826 Hidden Pair
 *     135,059,400  19160      7,049   1741      77,575 Naked Triple
 *     125,071,000  18740      6,674   1187     105,367 Hidden Triple
 *      60,097,900  18522      3,244   1405      42,774 Two String Kite
 *      37,196,100  17117      2,173    455      81,749 Swampfish
 *      81,792,700  16919      4,834    639     128,001 XY-Wing
 *      59,691,900  16463      3,625    309     193,177 XYZ-Wing
 *     106,235,800  16173      6,568    421     252,341 W-Wing
 *      47,470,800  15877      2,989    380     124,923 Skyscraper
 *      54,948,600  15672      3,506    473     116,170 Empty Rectangle
 *      84,901,200  15199      5,585    249     340,968 Swordfish
 *     168,694,300  15131     11,148    103   1,637,808 Naked Quad
 *     144,239,300  15109      9,546     12  12,019,941 Hidden Quad
 *      19,179,600  15107      1,269      8   2,397,450 Jellyfish
 *     484,415,000  15105     32,069   1527     317,233 WXYZ-Wing
 *     619,535,500  14079     44,004   1343     461,307 VWXYZ-Wing
 *     734,682,800  13311     55,193    576   1,275,490 UVWXYZ-Wing
 *     586,026,400  12996     45,092    131   4,473,483 TUVWXYZ-Wing
 *     304,092,300  12922     23,532     13  23,391,715 STUVWXYZ-Wing
 *     383,334,500  12914     29,683    280   1,369,051 Coloring
 *   1,253,874,200  12696     98,761    604   2,075,950 XColoring
 *   2,668,809,000  12469    214,035  28599      93,318 3D Medusa Coloring
 *   1,962,234,400  11751    166,984    981   2,000,238 Unique Rectangle
 *   1,234,304,300  11080    111,399    245   5,037,976 Finned Swampfish
 *   2,835,210,300  10878    260,637    285   9,948,106 Finned Swordfish
 *   3,850,223,500  10666    360,981     13 296,171,038 Finned Jellyfish
 *  15,859,672,700  10656  1,488,332   4423   3,585,727 ALS-XZ
 *  20,173,503,900   7563  2,667,394   3643   5,537,607 ALS-Wing
 *   8,858,656,000   4535  1,953,397    647  13,691,894 ALS-Chain
 *   3,944,250,400   4011    983,358    171  23,065,791 Death Blossom
 *   1,206,386,700   3859    312,616     11 109,671,518 Sue De Coq
 *   2,540,747,500   3857    658,736      0           0 Franken Swampfish
 *  13,964,580,300   3857  3,620,580    136 102,680,737 Franken Swordfish
 *  42,678,581,700   3744 11,399,193     69 618,530,169 Franken Jellyfish
 *   9,764,791,700   3681  2,652,755   1335   7,314,450 Unary Chain
 *   4,667,356,100   3157  1,478,414     23 202,928,526 Nishio Chain
 *   7,005,680,000   3134  2,235,379   4731   1,480,803 Multiple Chain
 *   9,894,368,500   1373  7,206,386   7508   1,317,843 Dynamic Chain
 *      86,134,300      3 28,711,433     30   2,871,143 Dynamic Plus
 * 158,939,966,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  206,956,155,200 (03:26)  141,267,000
 * NOTES:
 * 1. Last top1465 run took 03:26 so we're back on track at a minute faster
 *    than my previous blow-out.
 * 2. Release 6.30.113 2021-03-11 16:38:53 =>
 *    DiufSudoku_V6_30.113.2021-03-11.7z
 * 3. Next I don't know. I've looked at everything on sudopedia. All solving
 *    techniques (or equivalent) are now implemented in Sudoku Explainer, so
 *    one could now call it a "complete solution". So I think I've achieved my
 *    stated goal: to solve all Sudoku puzzles, as simply and as quickly as
 *    possible, and explain that solution to the user in terms they can (sort
 *    of) understand. Kudos to Juillerat and hobiwan. The mistakes are mine.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.114 2021-03-22 12:04:54</b> GEM (Graded Equivalence Marks) is all-out
 * Coloring. It (51224) finds more hints than Medusa 3D Coloring (28599).
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      23,350,200 112996        206 633290          36 Naked Single
 *      16,677,900  49667        335 169160          98 Hidden Single
 *     105,741,100  32751      3,228  21013       5,032 Locking
 *      57,100,400  22510      2,536   7323       7,797 Naked Pair
 *      49,470,400  20895      2,367  13259       3,731 Hidden Pair
 *     132,396,000  18768      7,054   1710      77,424 Naked Triple
 *     122,329,100  18364      6,661   1190     102,797 Hidden Triple
 *      61,773,000  18145      3,404   1385      44,601 Two String Kite
 *      37,847,000  16760      2,258    445      85,049 Swampfish
 *      78,510,100  16567      4,738    635     123,637 XY-Wing
 *      60,115,300  16117      3,729    294     204,473 XYZ-Wing
 *      89,072,100  15841      5,622    411     216,720 W-Wing
 *      48,013,300  15553      3,087    370     129,765 Skyscraper
 *      53,937,400  15355      3,512    462     116,747 Empty Rectangle
 *      65,925,000  14893      4,426    238     276,995 Swordfish
 *     168,400,900  14829     11,356    107   1,573,840 Naked Quad
 *     144,590,600  14807      9,765     12  12,049,216 Hidden Quad
 *      18,682,500  14805      1,261      8   2,335,312 Jellyfish
 *     479,059,500  14803     32,362   1496     320,226 WXYZ-Wing
 *     610,086,600  13796     44,221   1339     455,628 VWXYZ-Wing
 *     725,776,400  13035     55,679    554   1,310,065 UVWXYZ-Wing
 *     578,498,900  12730     45,443    127   4,555,109 TUVWXYZ-Wing
 *     302,475,100  12656     23,899     13  23,267,315 STUVWXYZ-Wing
 *     380,077,000  12648     30,050    293   1,297,191 Coloring
 *   1,204,889,800  12429     96,941    584   2,063,167 XColoring
 *   2,342,735,300  12210    191,870  51224      45,735 GEM
 *   1,881,585,900  11346    165,836    952   1,976,455 Unique Rectangle
 *   1,203,882,800  10691    112,607    242   4,974,722 Finned Swampfish
 *   2,757,530,600  10492    262,822    283   9,743,924 Finned Swordfish
 *   3,771,791,600  10282    366,834     13 290,137,815 Finned Jellyfish
 *  15,830,412,800  10272  1,541,122   4170   3,796,262 ALS-XZ
 *  19,377,809,900   7348  2,637,154   3456   5,607,005 ALS-Wing
 *   8,974,073,400   4478  2,004,036    629  14,267,207 ALS-Chain
 *   3,942,160,900   3968    993,488    167  23,605,753 Death Blossom
 *   1,089,638,900   3820    285,245     11  99,058,081 Sue De Coq
 *   2,536,712,500   3818    664,408      0           0 Franken Swampfish
 *  13,878,632,900   3818  3,635,053    133 104,350,623 Franken Swordfish
 *  42,708,114,700   3708 11,517,830     69 618,958,184 Franken Jellyfish
 *   9,459,461,900   3645  2,595,188   1283   7,372,924 Unary Chain
 *   4,819,662,000   3139  1,535,413     23 209,550,521 Nishio Chain
 *   6,991,226,300   3116  2,243,654   4677   1,494,809 Multiple Chain
 *  10,105,023,000   1373  7,359,812   7508   1,345,900 Dynamic Chain
 *     154,294,100      3 51,431,366     30   5,143,136 Dynamic Plus
 * 157,439,545,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  206,948,518,800 (03:26)  141,261,787
 * NOTES:
 * 1. Last top1465 run took 03:26, same as last time. Still back on track.
 * 2. Release 6.30.114 2021-03-22 12:04:54 =>
 *    DiufSudoku_V6_30.114.2021-03-22.7z
 * 3. Next I don't know. I've implemented all sudopedia solving techniques. I
 *    guess I'm finally done.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.115 2021-03-24 08:53:55</b> GEM Mark 2 now finds more hints (75550)
 * than Mark 1 (51224). I'll actually release this build, I think, today.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      22,822,500 110005        207 613290          37 Naked Single
 *      15,779,200  48676        324 164590          95 Hidden Single
 *     105,890,200  32217      3,286  20946       5,055 Locking
 *      55,017,800  22039      2,496   7290       7,547 Naked Pair
 *      48,248,200  20450      2,359  13229       3,647 Hidden Pair
 *     128,799,600  18348      7,019   1689      76,257 Naked Triple
 *     115,274,000  17957      6,419   1184      97,359 Hidden Triple
 *      59,310,200  17740      3,343   1361      43,578 Two String Kite
 *      35,159,900  16379      2,146    446      78,833 Swampfish
 *      74,468,100  16188      4,600    604     123,291 XY-Wing
 *      59,599,000  15761      3,781    295     202,030 XYZ-Wing
 *      81,463,900  15484      5,261    403     202,143 W-Wing
 *      49,786,400  15202      3,274    368     135,289 Skyscraper
 *      55,259,500  15005      3,682    460     120,129 Empty Rectangle
 *      64,957,200  14545      4,465    237     274,081 Swordfish
 *     164,024,100  14482     11,326    104   1,577,154 Naked Quad
 *     135,841,200  14463      9,392     12  11,320,100 Hidden Quad
 *      18,272,400  14461      1,263      9   2,030,266 Jellyfish
 *     460,371,800  14458     31,842   1464     314,461 WXYZ-Wing
 *     603,143,900  13482     44,736   1300     463,956 VWXYZ-Wing
 *     710,106,200  12754     55,677    548   1,295,814 UVWXYZ-Wing
 *     572,297,600  12452     45,960    121   4,729,732 TUVWXYZ-Wing
 *     301,006,900  12382     24,310     12  25,083,908 STUVWXYZ-Wing
 *     378,001,100  12375     30,545    292   1,294,524 Coloring
 *   1,202,970,600  12159     98,936    545   2,207,285 XColoring
 *   2,862,185,700  11945    239,613  75550      37,884 GEM
 *   1,849,796,000  10960    168,777    898   2,059,906 Unique Rectangle
 *   1,152,739,900  10341    111,472    236   4,884,491 Finned Swampfish
 *   2,591,861,200  10148    255,406    279   9,289,825 Finned Swordfish
 *   3,505,877,100   9942    352,632     11 318,716,100 Finned Jellyfish
 *  15,263,757,100   9934  1,536,516   3954   3,860,333 ALS-XZ
 *  20,793,975,200   7180  2,896,096   3332   6,240,688 ALS-Wing
 *   8,923,613,300   4424  2,017,091    609  14,652,895 ALS-Chain
 *   3,932,047,900   3930  1,000,521    165  23,830,593 Death Blossom
 *   1,013,748,100   3784    267,903     11  92,158,918 Sue De Coq
 *   2,504,517,000   3782    662,220      0           0 Franken Swampfish
 *  13,587,353,700   3782  3,592,637    130 104,518,105 Franken Swordfish
 *  42,690,336,200   3675 11,616,418     68 627,799,061 Franken Jellyfish
 *   9,312,198,100   3613  2,577,414   1157   8,048,572 Unary Chain
 *   4,764,150,800   3125  1,524,528     22 216,552,309 Nishio Chain
 *   6,948,252,000   3103  2,239,204   4556   1,525,077 Multiple Chain
 *   9,975,618,300   1373  7,265,563   7508   1,328,665 Dynamic Chain
 *     112,156,200      3 37,385,400     30   3,738,540 Dynamic Plus
 * 157,302,055,300
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  203,957,039,500 (03:23)  139,219,822
 * NOTES:
 * 1. Last top1465 run took 03:23, 3 seconds faster. Wow. Sigh.
 * 2. Release 6.30.114 2021-03-22 12:04:54 =>
 *    DiufSudoku_V6_30.115.2021-03-24.7z
 * 3. Next I don't know. I've implemented all sudopedia solving techniques, so
 *    I guess SE is finally complete. I think I've achieved my goal: To explain
 *    any/all Sudoku puzzles, as simply and quickly as possible, using logic.
 *    Well it's as fast as I'm able to make it. Somebody else could possibly
 *    get it running faster, but I can gaurantee you they'll be trying. I can
 *    dance by the record machine.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.116 2021-03-25 13:28:55</b> GEM mark 3, with it's own Multi hint.
 * Also contradictions uses ons as well as colors, so finds 77283 vs 75550,
 * but it does take longer, about 2 seconds. sigh.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      21,321,900 109798        194 611860          34 Naked Single
 *      16,206,200  48612        333 164230          98 Hidden Single
 *     108,385,000  32189      3,367  20938       5,176 Locking
 *      54,640,400  22017      2,481   7290       7,495 Naked Pair
 *      48,333,400  20428      2,366  13228       3,653 Hidden Pair
 *     127,527,100  18327      6,958   1689      75,504 Naked Triple
 *     113,249,900  17936      6,314   1184      95,650 Hidden Triple
 *      58,794,300  17719      3,318   1361      43,199 Two String Kite
 *      34,680,400  16358      2,120    447      77,584 Swampfish
 *      75,431,200  16166      4,666    606     124,473 XY-Wing
 *      58,519,500  15737      3,718    294     199,045 XYZ-Wing
 *      83,784,400  15461      5,419    404     207,387 W-Wing
 *      48,462,600  15179      3,192    365     132,774 Skyscraper
 *      57,423,100  14983      3,832    459     125,104 Empty Rectangle
 *      63,265,700  14524      4,355    237     266,943 Swordfish
 *     163,438,300  14461     11,302    104   1,571,522 Naked Quad
 *     131,426,100  14442      9,100     12  10,952,175 Hidden Quad
 *      17,890,700  14440      1,238      9   1,987,855 Jellyfish
 *     465,447,600  14437     32,239   1456     319,675 WXYZ-Wing
 *     597,869,000  13465     44,401   1295     461,674 VWXYZ-Wing
 *     712,925,300  12740     55,959    547   1,303,336 UVWXYZ-Wing
 *     577,559,300  12439     46,431    121   4,773,217 TUVWXYZ-Wing
 *     299,033,800  12369     24,176     12  24,919,483 STUVWXYZ-Wing
 *     372,833,600  12362     30,159    291   1,281,215 Coloring
 *   1,188,896,500  12147     97,875    545   2,181,461 XColoring
 *   3,171,443,900  11933    265,770  77283      41,036 GEM
 *   1,856,519,100  10950    169,545    893   2,078,968 Unique Rectangle
 *   1,137,567,200  10334    110,080    236   4,820,200 Finned Swampfish
 *   2,582,339,000  10141    254,643    279   9,255,695 Finned Swordfish
 *   3,486,326,500   9935    350,913     11 316,938,772 Finned Jellyfish
 *  15,235,301,000   9927  1,534,733   3946   3,860,948 ALS-XZ
 *  20,842,211,300   7178  2,903,623   3330   6,258,922 ALS-Wing
 *   8,907,917,400   4424  2,013,543    609  14,627,122 ALS-Chain
 *   3,925,448,300   3930    998,841    165  23,790,595 Death Blossom
 *   1,236,808,500   3784    326,852     11 112,437,136 Sue De Coq
 *   2,483,888,200   3782    656,765      0           0 Franken Swampfish
 *  13,290,588,400   3782  3,514,169    130 102,235,295 Franken Swordfish
 *  43,079,866,700   3675 11,722,412     68 633,527,451 Franken Jellyfish
 *   9,507,586,200   3613  2,631,493   1157   8,217,447 Unary Chain
 *   4,826,062,000   3125  1,544,339     22 219,366,454 Nishio Chain
 *   7,125,038,600   3103  2,296,177   4556   1,563,880 Multiple Chain
 *  10,367,049,400   1373  7,550,655   7508   1,380,800 Dynamic Chain
 *      98,563,200      3 32,854,400     30   3,285,440 Dynamic Plus
 * 158,657,870,200
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  205,364,167,700 (03:25)  140,180,319
 * NOTES:
 * 1. Last top1465 run took 03:25, 2 seconds slower. No biggy.
 * 2. Release 6.30.116 2021-03-25 13:28:55 =>
 *    DiufSudoku_V6_30.116.2021-03-25.7z
 * 3. Next I don't know. I've implemented all sudopedia solving techniques, so
 *    SE is complete. I've achieved my goal: To explain any Sudoku puzzle, as
 *    simply and quickly as possible. Well it's as fast as I'm able to make it.
 *    Somebody else might make it faster, but they'll certainly be trying.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.117 2021-03-27 14:02:57</b> GEM mark 4, sets subsequent singles to find
 * 130790 eliminations verses 77283 previously, and GEM is 1.5 secs faster.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      21,006,000 104385        201 560460          37 Naked Single
 *      16,691,700  48339        345 162140         102 Hidden Single
 *     104,003,700  32125      3,237  20923       4,970 Locking
 *      55,600,200  21964      2,531   7287       7,630 Naked Pair
 *      49,624,700  20375      2,435  13233       3,750 Hidden Pair
 *     127,720,200  18274      6,989   1691      75,529 Naked Triple
 *     114,937,100  17884      6,426   1184      97,075 Hidden Triple
 *      59,327,000  17667      3,358   1364      43,494 Two String Kite
 *      35,684,400  16303      2,188    443      80,551 Swampfish
 *      75,632,900  16114      4,693    607     124,601 XY-Wing
 *      59,063,600  15683      3,766    292     202,272 XYZ-Wing
 *      82,585,500  15409      5,359    404     204,419 W-Wing
 *      46,926,200  15127      3,102    367     127,864 Skyscraper
 *      56,066,900  14931      3,755    461     121,620 Empty Rectangle
 *      63,759,600  14470      4,406    236     270,167 Swordfish
 *     164,800,500  14408     11,438    104   1,584,620 Naked Quad
 *     134,166,900  14389      9,324     12  11,180,575 Hidden Quad
 *      17,992,900  14387      1,250     10   1,799,290 Jellyfish
 *     463,318,700  14384     32,210   1456     318,213 WXYZ-Wing
 *     593,859,400  13413     44,274   1291     459,999 VWXYZ-Wing
 *     708,657,800  12694     55,826    550   1,288,468 UVWXYZ-Wing
 *     570,517,000  12391     46,042    123   4,638,349 TUVWXYZ-Wing
 *     298,596,000  12319     24,238     12  24,883,000 STUVWXYZ-Wing
 *     377,962,100  12312     30,698    290   1,303,317 Coloring
 *   1,186,685,000  12096     98,105    573   2,071,003 XColoring
 *   1,596,875,100  11873    134,496 130790      12,209 GEM
 *   1,911,826,000  10858    176,075    896   2,133,734 Unique Rectangle
 *   1,175,803,500  10241    114,813    234   5,024,801 Finned Swampfish
 *   2,704,661,600  10050    269,120    276   9,799,498 Finned Swordfish
 *   3,691,387,300   9847    374,874     11 335,580,663 Finned Jellyfish
 *  15,207,892,700   9839  1,545,674   3919   3,880,554 ALS-XZ
 *  18,524,520,100   7107  2,606,517   3296   5,620,303 ALS-Wing
 *   8,819,496,900   4386  2,010,829    602  14,650,327 ALS-Chain
 *   3,957,830,200   3898  1,015,348    163  24,281,166 Death Blossom
 *   1,264,708,200   3754    336,896     11 114,973,472 Sue De Coq
 *  13,884,110,600   3752  3,700,455    126 110,191,353 Franken Swordfish
 *  42,561,126,200   3648 11,666,975     69 616,827,915 Franken Jellyfish
 *   9,517,834,400   3585  2,654,904   1154   8,247,690 Unary Chain
 *   7,238,384,300   3100  2,334,962   4519   1,601,766 Multiple Chain
 *  10,176,558,500   1380  7,374,317   7551   1,347,710 Dynamic Chain
 *     106,069,000      3 35,356,333     30   3,535,633 Dynamic Plus
 * 147,824,270,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  194,287,288,300 (03:14)  132,619,309
 * NOTES:
 * 1. Last top1465 run took 03:14, 11 seconds faster, which is nice.
 * 2. Release 6.30.117 2021-03-27 14:02:57 =>
 *    DiufSudoku_V6_30.117.2021-03-27.7z
 * 3. Next I don't know. I might take a week off, or atleast a day.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.118 2021-03-30 20:21:58</b> GEM mark 5 efficiency.
 * <pre>
 *          time(ns)  calls  time/call  elims   time/elim hinter
 *        21,249,200 104385        203 560460          37 Naked Single
 *        19,458,500  48339        402 162140         120 Hidden Single
 *       105,518,700  32125      3,284  20923       5,043 Locking
 *        55,520,100  21964      2,527   7287       7,619 Naked Pair
 *        50,278,000  20375      2,467  13233       3,799 Hidden Pair
 *       128,635,400  18274      7,039   1691      76,070 Naked Triple
 *       116,519,400  17884      6,515   1184      98,411 Hidden Triple
 *        60,692,300  17667      3,435   1364      44,495 Two String Kite
 *        35,579,500  16303      2,182    443      80,314 Swampfish
 *        72,596,600  16114      4,505    607     119,599 XY-Wing
 *        59,832,700  15683      3,815    292     204,906 XYZ-Wing
 *        84,953,200  15409      5,513    404     210,280 W-Wing
 *        49,623,400  15127      3,280    367     135,213 Skyscraper
 *        55,127,900  14931      3,692    461     119,583 Empty Rectangle
 *        64,923,000  14470      4,486    236     275,097 Swordfish
 *       166,869,700  14408     11,581    104   1,604,516 Naked Quad
 *       134,265,000  14389      9,331     12  11,188,750 Hidden Quad
 *        18,280,600  14387      1,270     10   1,828,060 Jellyfish
 *       476,107,700  14384     33,099   1456     326,997 WXYZ-Wing
 *       609,393,600  13413     45,433   1291     472,032 VWXYZ-Wing
 *       731,611,600  12694     57,634    550   1,330,202 UVWXYZ-Wing
 *       596,254,700  12391     48,119    123   4,847,599 TUVWXYZ-Wing
 *       306,235,100  12319     24,858     12  25,519,591 STUVWXYZ-Wing
 *       379,243,100  12312     30,802    290   1,307,734 Coloring
 *     1,277,793,500  12096    105,637    573   2,230,006 XColoring
 *     1,502,419,200  11873    126,540 130790      11,487 GEM
 *     1,773,795,900  10858    163,363    896   1,979,682 Unique Rectangle
 *     1,195,804,200  10241    116,766    234   5,110,274 Finned Swampfish
 *     2,769,168,900  10050    275,539    276  10,033,220 Finned Swordfish
 *     3,783,300,600   9847    384,208     11 343,936,418 Finned Jellyfish
 *    15,542,621,000   9839  1,579,695   3919   3,965,966 ALS-XZ
 *    18,939,020,100   7107  2,664,840   3296   5,746,061 ALS-Wing
 *     9,173,567,000   4386  2,091,556    602  15,238,483 ALS-Chain
 *     4,083,935,300   3898  1,047,700    163  25,054,817 Death Blossom
 *     1,309,324,200   3754    348,781     11 119,029,472 Sue De Coq
 *    14,389,175,400   3752  3,835,068    126 114,199,804 Franken Swordfish
 *    43,924,037,000   3648 12,040,580     69 636,580,246 Franken Jellyfish
 *     9,869,519,700   3585  2,753,004   1154   8,552,443 Unary Chain
 *     7,560,696,800   3100  2,438,934   4519   1,673,090 Multiple Chain
 *    10,451,295,500   1380  7,573,402   7551   1,384,094 Dynamic Chain
 *        97,693,100      3 32,564,366     30   3,256,436 Dynamic Plus
 *   152,041,936,400
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  198,912,677,000 (03:18)  135,776,571
 * NOTES:
 * 1. Last top1465 run took 03:18, 4 seconds slower, but I'm not worried,
 *    because anything under 4 minutes feels "pretty snappy" in the GUI.
 * 2. Release 6.30.118 2021-03-30 20:21:58 =>
 *    DiufSudoku_V6_30.118.2021-03-30.7z
 * 3. Next I don't know. I might take a week off, or atleast a day.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.119 2021-04-01 09:15:59</b> GEM mark 6 improved explanation.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      20,663,200 104385        197 560460          36 Naked Single
 *      18,123,800  48339        374 162140         111 Hidden Single
 *     103,570,900  32125      3,223  20923       4,950 Locking
 *      56,371,100  21964      2,566   7287       7,735 Naked Pair
 *      49,298,500  20375      2,419  13233       3,725 Hidden Pair
 *     127,278,300  18274      6,964   1691      75,268 Naked Triple
 *     113,959,200  17884      6,372   1184      96,249 Hidden Triple
 *      59,972,000  17667      3,394   1364      43,967 Two String Kite
 *      35,120,500  16303      2,154    443      79,278 Swampfish
 *      73,557,600  16114      4,564    607     121,182 XY-Wing
 *      59,486,900  15683      3,793    292     203,722 XYZ-Wing
 *      80,928,700  15409      5,252    404     200,318 W-Wing
 *      47,355,700  15127      3,130    367     129,034 Skyscraper
 *      56,066,800  14931      3,755    461     121,619 Empty Rectangle
 *      65,165,500  14470      4,503    236     276,125 Swordfish
 *     163,377,600  14408     11,339    104   1,570,938 Naked Quad
 *     133,237,500  14389      9,259     12  11,103,125 Hidden Quad
 *      18,664,600  14387      1,297     10   1,866,460 Jellyfish
 *     470,473,700  14384     32,708   1456     323,127 WXYZ-Wing
 *     597,421,400  13413     44,540   1291     462,758 VWXYZ-Wing
 *     716,557,200  12694     56,448    550   1,302,831 UVWXYZ-Wing
 *     575,821,400  12391     46,470    123   4,681,474 TUVWXYZ-Wing
 *     302,586,400  12319     24,562     12  25,215,533 STUVWXYZ-Wing
 *     384,576,100  12312     31,235    290   1,326,124 Coloring
 *   1,211,580,700  12096    100,163    573   2,114,451 XColoring
 *   1,474,974,500  11873    124,229 130790      11,277 GEM
 *   1,758,153,900  10858    161,922    896   1,962,225 Unique Rectangle
 *   1,194,728,700  10241    116,661    234   5,105,678 Finned Swampfish
 *   2,743,112,100  10050    272,946    276   9,938,811 Finned Swordfish
 *   3,737,852,600   9847    379,593     11 339,804,781 Finned Jellyfish
 *  15,661,999,800   9839  1,591,828   3919   3,996,427 ALS-XZ
 *  19,207,213,300   7107  2,702,576   3296   5,827,431 ALS-Wing
 *   9,162,988,700   4386  2,089,144    602  15,220,911 ALS-Chain
 *   4,015,911,100   3898  1,030,249    163  24,637,491 Death Blossom
 *   1,258,609,200   3754    335,271     11 114,419,018 Sue De Coq
 *  14,116,840,600   3752  3,762,484    126 112,038,417 Franken Swordfish
 *  43,212,091,400   3648 11,845,419     69 626,262,194 Franken Jellyfish
 *   9,467,510,100   3585  2,640,867   1154   8,204,081 Unary Chain
 *   7,247,729,700   3100  2,337,977   4519   1,603,834 Multiple Chain
 *  10,397,860,600   1380  7,534,681   7551   1,377,017 Dynamic Chain
 *     109,328,300      3 36,442,766     30   3,644,276 Dynamic Plus
 * 150,308,119,900
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  197,730,380,500 (03:17)  134,969,543
 * NOTES:
 * 1. Last top1465 run took 03:17, a second faster. Woop-de-doo.
 * 2. Release 6.30.119 2021-04-01 09:15:59 =>
 *    DiufSudoku_V6_30.119.2021-04-01.7z
 * 3. Next I don't know. I need some time off to think.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.120 2021-04-04 06:11:00</b> GEM mark 7 more eliminations.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      22,321,000 102906        216 549570          40 Naked Single
 *      20,978,800  47949        437 161050         130 Hidden Single
 *     107,127,300  31844      3,364  20832       5,142 Locking
 *      57,107,800  21752      2,625   7226       7,903 Naked Pair
 *      49,505,000  20199      2,450  13203       3,749 Hidden Pair
 *     127,972,400  18117      7,063   1683      76,038 Naked Triple
 *     121,392,900  17732      6,845   1182     102,701 Hidden Triple
 *      61,711,400  17516      3,523   1342      45,984 Two String Kite
 *      37,462,500  16174      2,316    436      85,923 Swampfish
 *      77,214,900  15990      4,828    592     130,430 XY-Wing
 *      61,943,600  15570      3,978    290     213,598 XYZ-Wing
 *      81,119,800  15298      5,302    403     201,289 W-Wing
 *      49,095,800  15017      3,269    362     135,623 Skyscraper
 *      58,058,600  14825      3,916    459     126,489 Empty Rectangle
 *      65,840,800  14366      4,583    239     275,484 Swordfish
 *     163,324,600  14303     11,418    104   1,570,428 Naked Quad
 *     141,763,900  14284      9,924     12  11,813,658 Hidden Quad
 *      19,446,700  14282      1,361     10   1,944,670 Jellyfish
 *     487,615,400  14279     34,149   1419     343,633 WXYZ-Wing
 *     619,620,000  13336     46,462   1274     486,357 VWXYZ-Wing
 *     725,514,400  12626     57,461    541   1,341,061 UVWXYZ-Wing
 *     593,821,300  12330     48,160    122   4,867,387 TUVWXYZ-Wing
 *     315,338,900  12259     25,723      9  35,037,655 STUVWXYZ-Wing
 *     386,369,100  12253     31,532    283   1,365,261 Coloring
 *   1,252,704,400  12042    104,027    575   2,178,616 XColoring
 *   1,600,294,600  11818    135,411 142775      11,208 GEM
 *   1,973,844,500  10851    181,904    896   2,202,951 Unique Rectangle
 *   1,208,517,000  10234    118,088    233   5,186,768 Finned Swampfish
 *   2,806,181,900  10044    279,388    276  10,167,325 Finned Swordfish
 *   3,854,020,300   9841    391,628     11 350,365,481 Finned Jellyfish
 *  16,013,772,900   9833  1,628,574   3916   4,089,318 ALS-XZ
 *  19,699,029,200   7104  2,772,948   3294   5,980,276 ALS-Wing
 *   9,272,910,700   4385  2,114,688    601  15,429,135 ALS-Chain
 *   4,155,689,900   3898  1,066,108    163  25,495,030 Death Blossom
 *   1,115,244,000   3754    297,081     11 101,385,818 Sue De Coq
 *  14,533,075,400   3752  3,873,420    126 115,341,868 Franken Swordfish
 *  43,804,304,300   3648 12,007,758     69 634,844,989 Franken Jellyfish
 *   9,860,796,900   3585  2,750,570   1154   8,544,884 Unary Chain
 *   7,297,765,200   3100  2,354,117   4519   1,614,907 Multiple Chain
 *  10,431,224,900   1380  7,558,858   7551   1,381,436 Dynamic Chain
 *     118,765,900      3 39,588,633     30   3,958,863 Dynamic Plus
 * 153,449,808,900
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  201,716,598,100 (03:21)  137,690,510
 * NOTES:
 * 1. Last top1465 run took 03:21, 4 seconds slower, which is a bit of a worry,
 *    but anything under 4 minutes feels "pretty snappy" in the GUI, so I'm not
 *    too worried. I note that EVERYthing is slower in this run, so ascribe a
 *    10% slowdown to "hot box", despite the cool of morning (a worry).
 * 2. Release 6.30.120 2021-04-04 06:11:00 =>
 *    DiufSudoku_V6_30.120.2021-04-04.7z
 * 3. Next I don't know. I need some time off to think.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.121 2021-04-04 10:01:01</b> GEM mark 8 even more eliminations, chasing
 * Naked/Hidden Singles from little hints as well. Eliminations are upto 162376
 * from 142775. IMHO this greedy behaviour is warranted because GEM is often
 * the last "serious" hint, so it should solve the puzzle whenever possible.
 * <p>
 * GEM's test-cases are still broken. Hint-types changed coz of "upgrades".
 * I'll deal with it later: find some new "simple type" hints.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      20,272,200 100941        200 531210          38 Naked Single
 *      15,798,500  47820        330 159090          99 Hidden Single
 *     101,601,000  31911      3,183  20864       4,869 Locking
 *      55,150,100  21798      2,530   7253       7,603 Naked Pair
 *      48,775,400  20234      2,410  13208       3,692 Hidden Pair
 *     126,422,400  18149      6,965   1685      75,028 Naked Triple
 *     112,097,400  17763      6,310   1181      94,917 Hidden Triple
 *      59,394,700  17549      3,384   1346      44,126 Two String Kite
 *      35,839,300  16203      2,211    438      81,824 Swampfish
 *      76,690,000  16019      4,787    591     129,763 XY-Wing
 *      60,532,400  15600      3,880    292     207,302 XYZ-Wing
 *      84,874,500  15326      5,537    404     210,085 W-Wing
 *      47,664,800  15045      3,168    362     131,670 Skyscraper
 *      59,285,800  14853      3,991    457     129,728 Empty Rectangle
 *      64,647,300  14396      4,490    239     270,490 Swordfish
 *     163,700,000  14333     11,421    104   1,574,038 Naked Quad
 *     132,609,900  14314      9,264     12  11,050,825 Hidden Quad
 *      18,513,100  14312      1,293     10   1,851,310 Jellyfish
 *     464,461,900  14309     32,459   1414     328,473 WXYZ-Wing
 *     601,260,500  13369     44,974   1272     472,689 VWXYZ-Wing
 *     715,633,600  12660     56,527    545   1,313,089 UVWXYZ-Wing
 *     576,694,700  12361     46,654    122   4,727,005 TUVWXYZ-Wing
 *     301,643,900  12290     24,543      9  33,515,988 STUVWXYZ-Wing
 *     376,162,800  12284     30,622    284   1,324,516 Coloring
 *   1,251,456,800  12072    103,666    575   2,176,446 XColoring
 *   1,560,177,300  11848    131,682 162376       9,608 GEM
 *   1,882,524,500  10851    173,488    896   2,101,031 Unique Rectangle
 *   1,163,001,200  10234    113,640    233   4,991,421 Finned Swampfish
 *   2,660,191,300  10044    264,853    276   9,638,374 Finned Swordfish
 *   3,582,900,600   9841    364,078     11 325,718,236 Finned Jellyfish
 *  15,663,703,100   9833  1,592,972   3916   3,999,924 ALS-XZ
 *  18,909,935,600   7104  2,661,871   3294   5,740,721 ALS-Wing
 *   9,253,769,500   4385  2,110,323    601  15,397,287 ALS-Chain
 *   4,027,892,100   3898  1,033,322    163  24,710,994 Death Blossom
 *   1,330,165,600   3754    354,332     11 120,924,145 Sue De Coq
 *  13,805,659,400   3752  3,679,546    126 109,568,725 Franken Swordfish
 *  41,849,864,600   3648 11,472,002     69 606,519,776 Franken Jellyfish
 *   9,695,605,600   3585  2,704,492   1154   8,401,737 Unary Chain
 *   7,377,868,700   3100  2,379,957   4519   1,632,633 Multiple Chain
 *  10,817,858,400   1380  7,839,027   7551   1,432,639 Dynamic Chain
 *     103,049,100      3 34,349,700     30   3,434,970 Dynamic Plus
 * 149,255,349,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  192,952,417,800 (03:12)  131,708,135
 * NOTES:
 * 1. Last top1465 run took 03:12, 9 seconds faster. That's better.
 * 2. Release 6.30.121 2021-04-04 10:01:01 =>
 *    DiufSudoku_V6_30.121.2021-04-04.7z
 * 3. Next I don't know. I need some time off to think.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.122 2021-04-05 07:17:02</b> GEM mark 9 fix bug in mark 8: an AIOOBE
 * building the why string (steps) when goodColor still == -1. My bad.
 * <p>
 * Also fixed GEM test-cases so this is the last GEM build. I promise to take
 * some time off.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      36,417,200 101047        360 532930          68 Naked Single
 *      18,016,000  47754        377 159100         113 Hidden Single
 *     111,705,100  31844      3,507  20832       5,362 Locking
 *      59,244,000  21752      2,723   7226       8,198 Naked Pair
 *      52,096,600  20199      2,579  13203       3,945 Hidden Pair
 *     136,580,400  18117      7,538   1683      81,152 Naked Triple
 *     129,764,000  17732      7,318   1182     109,783 Hidden Triple
 *      66,056,800  17516      3,771   1342      49,222 Two String Kite
 *      38,453,000  16174      2,377    436      88,194 Swampfish
 *      79,314,300  15990      4,960    592     133,976 XY-Wing
 *      65,083,200  15570      4,180    290     224,424 XYZ-Wing
 *      88,220,900  15298      5,766    403     218,910 W-Wing
 *      52,086,800  15017      3,468    362     143,886 Skyscraper
 *      63,955,600  14825      4,314    459     139,336 Empty Rectangle
 *      69,612,100  14366      4,845    239     291,264 Swordfish
 *     176,610,000  14303     12,347    104   1,698,173 Naked Quad
 *     149,952,200  14284     10,497     12  12,496,016 Hidden Quad
 *      19,771,600  14282      1,384     10   1,977,160 Jellyfish
 *     515,821,600  14279     36,124   1419     363,510 WXYZ-Wing
 *     648,401,900  13336     48,620   1274     508,949 VWXYZ-Wing
 *     773,439,100  12626     61,257    541   1,429,647 UVWXYZ-Wing
 *     632,511,200  12330     51,298    122   5,184,518 TUVWXYZ-Wing
 *     327,670,900  12259     26,729      9  36,407,877 STUVWXYZ-Wing
 *     410,512,500  12253     33,503    283   1,450,574 Coloring
 *   1,328,825,100  12042    110,349    575   2,311,000 XColoring
 *   1,737,260,600  11818    147,001 160824      10,802 GEM
 *   1,922,667,000  10851    177,188    896   2,145,833 Unique Rectangle
 *   1,293,009,500  10234    126,344    233   5,549,396 Finned Swampfish
 *   2,946,165,200  10044    293,325    276  10,674,511 Finned Swordfish
 *   4,023,443,100   9841    408,844     11 365,767,554 Finned Jellyfish
 *  16,874,738,700   9833  1,716,133   3916   4,309,177 ALS-XZ
 *  20,509,469,200   7104  2,887,031   3294   6,226,311 ALS-Wing
 *   9,977,296,800   4385  2,275,324    601  16,601,159 ALS-Chain
 *   4,350,635,200   3898  1,116,119    163  26,691,013 Death Blossom
 *   1,368,609,800   3754    364,573     11 124,419,072 Sue De Coq
 *  15,191,529,900   3752  4,048,915    126 120,567,697 Franken Swordfish
 *  46,769,257,500   3648 12,820,520     69 677,815,326 Franken Jellyfish
 *  10,314,302,500   3585  2,877,071   1154   8,937,870 Unary Chain
 *   8,015,088,400   3100  2,585,512   4519   1,773,642 Multiple Chain
 *  11,411,140,500   1380  8,268,942   7551   1,511,209 Dynamic Chain
 *     112,134,900      3 37,378,300     30   3,737,830 Dynamic Plus
 * 162,866,870,900
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  213,073,777,500 (03:33)  145,442,851
 * NOTES:
 * 1. Last top1465 run took 03:33, 21 seconds slower, which is a worry. sigh.
 *    But if you look at the Naked Singles time, it's nearly double, so I blame
 *    "hot box" syndrome again, despite the coolness of morning.
 * 2. Release 6.30.122 2021-04-05 07:17:02 =>
 *    DiufSudoku_V6_30.122.2021-04-05.7z
 * 3. Next I don't know. I need some time off to think.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.123 2021-04-07 19:50:03</b> GEM mark 10 seriously greedy gemSolve. Made
 * addConsequentSingles use gemSolve which uses 9 HINTERS to solve most puzzles
 * in order to get seriously greedy about setting as many cells as possible.
 * <pre>
 *        time(ns) calls  time/call  elims   time/elim hinter
 *      20,228,700 99146        204 516330          39 Naked Single
 *      17,943,500 47513        377 158210         113 Hidden Single
 *     108,333,300 31692      3,418  20771       5,215 Locking
 *      55,613,500 21643      2,569   7145       7,783 Naked Pair
 *      47,107,700 20122      2,341  13181       3,573 Hidden Pair
 *     127,946,700 18049      7,088   1682      76,068 Naked Triple
 *     111,646,000 17665      6,320   1182      94,455 Hidden Triple
 *      56,029,100 17449      3,211   1328      42,190 Two String Kite
 *      34,093,200 16121      2,114    434      78,555 Swampfish
 *      71,736,800 15939      4,500    548     130,906 XY-Wing
 *      60,035,200 15551      3,860    285     210,649 XYZ-Wing
 *      78,793,400 15284      5,155    386     204,127 W-Wing
 *      43,628,200 15017      2,905    362     120,519 Skyscraper
 *      54,713,000 14825      3,690    459     119,200 Empty Rectangle
 *      64,057,200 14366      4,458    239     268,021 Swordfish
 *     163,238,300 14303     11,412    104   1,569,599 Naked Quad
 *     131,562,400 14284      9,210     12  10,963,533 Hidden Quad
 *      17,979,000 14282      1,258     10   1,797,900 Jellyfish
 *     460,314,500 14279     32,237   1419     324,393 WXYZ-Wing
 *     586,859,300 13336     44,005   1274     460,643 VWXYZ-Wing
 *     698,863,800 12626     55,351    541   1,291,800 UVWXYZ-Wing
 *     564,444,900 12330     45,778    122   4,626,597 TUVWXYZ-Wing
 *     295,728,300 12259     24,123      9  32,858,700 STUVWXYZ-Wing
 *     370,475,100 12253     30,235    283   1,309,099 Coloring
 *   1,202,031,700 12042     99,819    575   2,090,489 XColoring
 *   1,551,961,800 11818    131,321 178270       8,705 GEM
 *   1,748,221,500 10851    161,111    896   1,951,140 Unique Rectangle
 *   1,169,094,100 10234    114,236    233   5,017,571 Finned Swampfish
 *   2,684,239,100 10044    267,248    276   9,725,503 Finned Swordfish
 *   3,656,867,400  9841    371,595     11 332,442,490 Finned Jellyfish
 *  15,291,848,600  9833  1,555,155   3916   3,904,966 ALS-XZ
 *  18,319,918,800  7104  2,578,817   3294   5,561,602 ALS-Wing
 *   8,936,811,400  4385  2,038,041    601  14,869,902 ALS-Chain
 *   3,861,018,800  3898    990,512    163  23,687,231 Death Blossom
 *   1,234,323,700  3754    328,802     11 112,211,245 Sue De Coq
 *  13,882,026,700  3752  3,699,900    126 110,174,815 Franken Swordfish
 *  42,195,000,500  3648 11,566,611     69 611,521,746 Franken Jellyfish
 *   9,509,897,100  3585  2,652,690   1154   8,240,812 Unary Chain
 *   7,170,042,900  3100  2,312,917   4519   1,586,643 Multiple Chain
 *  10,191,543,200  1380  7,385,176   7551   1,349,694 Dynamic Chain
 *      98,893,100     3 32,964,366     30   3,296,436 Dynamic Plus
 * 146,945,111,500
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  192,310,949,900 (03:12)  131,270,272
 * NOTES:
 * 1. Last top1465 run took 03:12, which is about right.
 * 2. Release 6.30.123 2021-04-07 19:50:03 =>
 *    DiufSudoku_V6_30.123.2021-04-07.7z
 * 3. Next I don't know. I really need a break!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.124 2021-04-11 09:26:04</b> GEM mark 11 uber greedy gemSolve using
 * additional hinters.
 * <pre>
 *        time(ns) calls  time/call  elims   time/elim hinter
 *      19,389,500 98266        197 509030          38 Naked Single
 *      17,255,500 47363        364 157790         109 Hidden Single
 *     107,865,000 31584      3,415  20736       5,201 Locking
 *      55,112,300 21560      2,556   7116       7,744 Naked Pair
 *      46,964,100 20056      2,341  13168       3,566 Hidden Pair
 *     125,132,800 17990      6,955   1678      74,572 Naked Triple
 *     110,688,900 17608      6,286   1180      93,804 Hidden Triple
 *      57,444,300 17392      3,302   1305      44,018 Two String Kite
 *      35,035,400 16087      2,177    434      80,726 Swampfish
 *      73,093,500 15905      4,595    543     134,610 XY-Wing
 *      58,639,700 15520      3,778    285     205,753 XYZ-Wing
 *      79,775,500 15253      5,230    382     208,836 W-Wing
 *      47,412,000 14989      3,163    355     133,554 Skyscraper
 *      54,789,300 14802      3,701    454     120,681 Empty Rectangle
 *      63,409,100 14348      4,419    239     265,310 Swordfish
 *     162,900,400 14285     11,403    104   1,566,350 Naked Quad
 *     130,443,100 14266      9,143     12  10,870,258 Hidden Quad
 *      18,115,600 14264      1,270     10   1,811,560 Jellyfish
 *     461,350,500 14261     32,350   1415     326,042 WXYZ-Wing
 *     595,269,400 13321     44,686   1272     467,979 VWXYZ-Wing
 *     734,841,300 12613     58,260    541   1,358,301 UVWXYZ-Wing
 *     577,161,700 12317     46,858    122   4,730,833 TUVWXYZ-Wing
 *     298,116,100 12246     24,343      9  33,124,011 STUVWXYZ-Wing
 *     373,344,700 12240     30,502    283   1,319,239 Coloring
 *   1,192,311,900 12029     99,119    576   2,069,985 XColoring
 *   1,595,891,100 11804    135,199 185959       8,581 GEM
 *   1,736,932,400 10845    160,159    886   1,960,420 Unique Rectangle
 *   1,188,883,800 10234    116,170    233   5,102,505 Finned Swampfish
 *   2,713,469,000 10044    270,158    276   9,831,409 Finned Swordfish
 *   3,706,973,000  9841    376,686     11 336,997,545 Finned Jellyfish
 *  15,596,324,700  9833  1,586,120   3916   3,982,718 ALS-XZ
 *  18,714,304,400  7104  2,634,333   3294   5,681,331 ALS-Wing
 *   9,121,570,300  4385  2,080,175    601  15,177,321 ALS-Chain
 *   4,002,648,400  3898  1,026,846    163  24,556,125 Death Blossom
 *   1,041,590,200  3754    277,461     11  94,690,018 Sue De Coq
 *  14,182,599,400  3752  3,780,010    126 112,560,312 Franken Swordfish
 *  43,189,291,100  3648 11,839,169     69 625,931,755 Franken Jellyfish
 *   9,766,740,400  3585  2,724,334   1154   8,463,379 Unary Chain
 *   7,276,291,500  3100  2,347,190   4519   1,610,155 Multiple Chain
 *  10,461,619,600  1380  7,580,883   7551   1,385,461 Dynamic Chain
 *      99,679,100     3 33,226,366     30   3,322,636 Dynamic Plus
 * 149,890,670,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  195,998,542,000 (03:15)  133,787,400
 * NOTES:
 * 1. Last top1465 run took 03:15, three seconds slower, but GEM is the only
 *    thing that's changed, and it took about the same time, so not worried.
 *    I'm blaming the bloody JIT compiler, or maybe it's just happenstance.
 * 2. Release 6.30.124 2021-04-11 09:26:04 =>
 *    DiufSudoku_V6_30.124.2021-04-11.7z
 * 3. Next I don't know. I really need a break!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.125 2021-04-13 10:04:05</b> GEM mark 12 ultra greedy gemSolve using all
 * hinters under 20ms/elim. Last top1465 run took 03:09 with 194557 GEM elims,
 * which is up from 185959 previously, and is as far as I'd ever want to go.
 * There are other usable hinters, but they're all too slow to warrant. SE will
 * still solve the puzzle, without GEM being so bloody greedy. I'm only doing
 * this because I'm an ornery old git who can't keep it simple.
 * <pre>
 *        time(ns) calls  time/call  elims   time/elim hinter
 *      19,724,400 97192        202 500770          39 Naked Single
 *      17,047,700 47115        361 157410         108 Hidden Single
 *     102,781,200 31374      3,275  20683       4,969 Locking
 *      57,984,400 21392      2,710   7072       8,199 Naked Pair
 *      48,547,500 19907      2,438  13154       3,690 Hidden Pair
 *     126,020,800 17851      7,059   1674      75,281 Naked Triple
 *     112,375,800 17470      6,432   1167      96,294 Hidden Triple
 *      55,140,700 17259      3,194   1287      42,844 Two String Kite
 *      35,370,300 15972      2,214    427      82,834 Swampfish
 *      70,672,300 15796      4,474    537     131,605 XY-Wing
 *      58,626,200 15416      3,802    282     207,894 XYZ-Wing
 *      77,237,000 15152      5,097    376     205,417 W-Wing
 *      45,621,200 14892      3,063    352     129,605 Skyscraper
 *      56,046,500 14707      3,810    452     123,996 Empty Rectangle
 *      63,064,900 14255      4,424    238     264,978 Swordfish
 *     159,083,100 14193     11,208    104   1,529,645 Naked Quad
 *     133,581,100 14174      9,424     12  11,131,758 Hidden Quad
 *      17,463,900 14172      1,232      9   1,940,433 Jellyfish
 *     463,620,100 14170     32,718   1382     335,470 WXYZ-Wing
 *     593,550,800 13251     44,792   1261     470,698 VWXYZ-Wing
 *     709,227,300 12550     56,512    539   1,315,820 UVWXYZ-Wing
 *     573,592,400 12256     46,800    122   4,701,577 TUVWXYZ-Wing
 *     302,094,800 12185     24,792      9  33,566,088 STUVWXYZ-Wing
 *     371,248,400 12179     30,482    268   1,385,255 Coloring
 *   1,205,477,700 11974    100,674    572   2,107,478 XColoring
 *   1,877,282,900 11751    159,755 194557       9,649 GEM
 *   1,878,555,600 10830    173,458    895   2,098,944 Unique Rectangle
 *   1,112,822,500 10214    108,950    232   4,796,648 Finned Swampfish
 *   2,441,525,900 10025    243,543    275   8,878,276 Finned Swordfish
 *   3,302,016,500  9823    336,151     11 300,183,318 Finned Jellyfish
 *  15,640,085,900  9815  1,593,488   3903   4,007,195 ALS-XZ
 *  19,022,980,000  7096  2,680,803   3289   5,783,818 ALS-Wing
 *   9,171,214,500  4382  2,092,928    601  15,259,924 ALS-Chain
 *   3,977,615,900  3895  1,021,210    161  24,705,688 Death Blossom
 *   1,100,685,700  3753    293,281     11 100,062,336 Sue De Coq
 *  13,129,323,700  3751  3,500,219    126 104,200,981 Franken Swordfish
 *  39,395,058,600  3647 10,802,045     69 570,942,878 Franken Jellyfish
 *   9,831,651,400  3584  2,743,206   1144   8,594,100 Unary Chain
 *   7,442,379,800  3100  2,400,767   4519   1,646,908 Multiple Chain
 *  10,545,437,600  1380  7,641,621   7551   1,396,561 Dynamic Chain
 *     107,375,300     3 35,791,766     30   3,579,176 Dynamic Plus
 * 145,451,212,300
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   189,055,959,600 (03:09)       129,048,436
 * NOTES:
 * 1. Last top1465 run took 03:09, six seconds faster. A good time.
 * 2. Release 6.30.125 2021-04-13 10:04:05 =>
 *    DiufSudoku_V6_30.125.2021-04-13.7z
 * 3. GEM is finito. Next I don't know. I still need a break!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.126 2021-04-17 06:09:06</b> GEM mark 13 Moved coloring up the hinters
 * list and dropped all hinters taking longer than 100ms/elim; so the last
 * top1465 run took 02:09, which is a minute faster, but most of that comes
 * from just dropping the slow hinters. GEM is now up to 278910 (from 194557)
 * mostly from moving it up the food chain. I have also tightened the speed
 * constraints on gemSolve's hinters, so GEM is now uber-greedy, and still
 * relatively fast. I'm building to clean-up the logs and create a back-up.
 * <pre>
 *       time(ns) calls  time/call  elims  time/elim hinter
 *     18,156,600 86629        209 423870         42 Naked Single
 *     16,642,500 44242        376 149460        111 Hidden Single
 *    100,671,500 29296      3,436  20177      4,989 Locking
 *     52,518,600 19675      2,669   6739      7,793 Naked Pair
 *     44,153,700 18363      2,404  12987      3,399 Hidden Pair
 *    113,283,500 16403      6,906   1644     68,907 Naked Triple
 *    102,664,100 16041      6,400   1142     89,898 Hidden Triple
 *     47,632,100 15840      3,007   1190     40,026 Two String Kite
 *     31,980,700 14650      2,182    372     85,969 Swampfish
 *     62,144,100 14500      4,285    444    139,964 XY-Wing
 *     52,964,900 14194      3,731    257    206,089 XYZ-Wing
 *     77,237,800 13954      5,535    333    231,945 W-Wing
 *     37,568,700 13730      2,736    321    117,036 Skyscraper
 *     52,157,200 13567      3,844    435    119,901 Empty Rectangle
 *     57,162,300 13132      4,352    221    258,652 Swordfish
 *    403,944,800 13079     30,884    265  1,524,320 Coloring
 *  1,276,818,000 12870     99,208    698  1,829,252 XColoring
 *  6,299,794,900 12634    498,638 278910     22,587 GEM
 *    134,091,300 11545     11,614     85  1,577,544 Naked Quad
 *    108,628,300 11530      9,421     11  9,875,300 Hidden Quad
 *    364,676,200 11529     31,631    790    461,615 WXYZ-Wing
 *    483,880,200 11026     43,885    825    586,521 VWXYZ-Wing
 *    596,417,200 10609     56,218    369  1,616,306 UVWXYZ-Wing
 *    496,292,500 10422     47,619     79  6,282,183 TUVWXYZ-Wing
 *  1,809,912,900 10378    174,399    819  2,209,905 Unique Rectangle
 *  1,100,125,400  9814    112,097    230  4,783,153 Finned Swampfish
 *  2,494,679,100  9628    259,106    278  8,973,665 Finned Swordfish
 * 14,820,512,500  9422  1,572,968   3697  4,008,794 ALS-XZ
 * 17,571,300,600  6884  2,552,484   3162  5,557,021 ALS-Wing
 *  8,836,008,300  4276  2,066,419    564 15,666,681 ALS-Chain
 * 10,069,899,600  3822  2,634,719   1247  8,075,300 Unary Chain
 *  7,718,521,800  3283  2,351,057   4691  1,645,389 Multiple Chain
 * 11,357,538,900  1454  7,811,237   7940  1,430,420 Dynamic Chain
 *    127,383,700     3 42,461,233     30  4,246,123 Dynamic Plus
 * 86,937,364,500
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  129,327,119,200 (02:09)  88,277,897
 * NOTES:
 * 1. Last top1465 run took 02:09, minute faster, discussed above.
 * 2. Release 6.30.126 2021-04-17 06:09:06 =>
 *    DiufSudoku_V6_30.126.2021-04-17.7z
 * 3. GEM is polished. Next I don't know. I still need a break!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.127 2021-04-24 08:18:07</b> GEM mark 14 is not greedy.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      22,620,300 101549        222 553210          40 Naked Single
 *      17,785,700  46228        384 156010         114 Hidden Single
 *      94,835,100  30627      3,096   5884      16,117 Direct Naked Pair
 *      87,407,500  30214      2,892  12300       7,106 Direct Hidden Pair
 *     211,672,600  29276      7,230    853     248,150 Direct Naked Triple
 *     188,055,400  29223      6,435   1777     105,827 Direct Hidden Triple
 *     107,225,900  29089      3,686  18197       5,892 Locking
 *      52,540,200  20195      2,601   4268      12,310 Naked Pair
 *      46,116,700  19176      2,404   8351       5,522 Hidden Pair
 *     114,472,600  17666      6,479   1246      91,872 Naked Triple
 *      95,945,300  17348      5,530   1017      94,341 Hidden Triple
 *      60,002,000  17152      3,498   1297      46,262 Two String Kite
 *      40,565,800  15855      2,558    421      96,355 Swampfish
 *      76,827,000  15677      4,900    572     134,312 XY-Wing
 *      62,806,400  15277      4,111    283     221,930 XYZ-Wing
 *      95,346,700  15012      6,351    383     248,946 W-Wing
 *      48,277,700  14745      3,274    350     137,936 Skyscraper
 *      70,290,600  14562      4,826    452     155,510 Empty Rectangle
 *      68,512,400  14110      4,855    231     296,590 Swordfish
 *     477,219,800  14050     33,965    312   1,529,550 Coloring
 *   1,600,190,900  13808    115,888    663   2,413,560 XColoring
 *   2,247,021,200  13566    165,636 129408      17,363 GEM
 *     173,201,900  12163     14,240     96   1,804,186 Naked Quad
 *     128,341,600  12146     10,566     11  11,667,418 Hidden Quad
 *     429,181,700  12145     35,338    871     492,745 WXYZ-Wing
 *     579,277,000  11591     49,976    877     660,521 VWXYZ-Wing
 *     716,192,000  11137     64,307    388   1,845,855 UVWXYZ-Wing
 *     590,075,300  10941     53,932     83   7,109,340 TUVWXYZ-Wing
 *   2,085,174,600  10893    191,423    888   2,348,169 Unique Rectangle
 *   1,183,275,500  10281    115,093    230   5,144,676 Finned Swampfish
 *   2,570,434,400  10094    254,649    281   9,147,453 Finned Swordfish
 *   3,393,808,600   9888    343,224     11 308,528,054 Finned Jellyfish
 *  18,566,933,000   9880  1,879,244   3916   4,741,300 ALS-XZ
 *  21,861,341,000   7153  3,056,247   3293   6,638,730 ALS-Wing
 *  10,901,786,700   4435  2,458,125    597  18,260,949 ALS-Chain
 *   4,735,235,700   3952  1,198,187    162  29,229,850 Death Blossom
 *  11,636,156,900   3809  3,054,911   1189   9,786,507 Unary Chain
 *   5,733,136,100   3310  1,732,065    218  26,298,789 Nishio Chain
 *   8,614,406,000   3110  2,769,905   4549   1,893,692 Multiple Chain
 *  12,104,962,200   1378  8,784,442   7558   1,601,609 Dynamic Chain
 *     212,457,800      3 70,819,266     30   7,081,926 Dynamic Plus
 * 112,101,115,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  162,353,180,600 (02:42)  110,821,283
 * NOTES:
 * 1. Last top1465 run took 02:42, 31 seconds slower, coz of Finned Jellyfish
 *    and GEM not being greedy so subsequent (slower) hinters`run more often.
 *    That's fine: anything under 4 minutes feels "pretty snappy" in the GUI.
 * 2. Release 6.30.127 2021-04-24 08:18:07 =>
 *    DiufSudoku_V6_30.127.2021-04-24.7z
 * 3. I'm done with Sudoku Explainer, I guess. I don't think there are any
 *    unimplemented hinters that I can understand well enough to do them any
 *    sort of justice, so that's it from me I suppose. Good luck with it.
 *    From here on out I'll fix reported bugs, probably for a couple of years,
 *    but other than that I'll find something else to dedicate myself to; after
 *    I take a good long break. I've been working on this (on and off) for a
 *    decade, so I've got the s__ts with it. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.128 2021-05-04 14:37:08</b> No real changes, just cleaning up crap, and
 * building to release because I'm going shopping this afternoon.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,469,147 101571        201 553450         36 Naked Single
 *     17,987,166  46226        389 155970        115 Hidden Single
 *     86,628,008  30629      2,828   5884     14,722 Direct Naked Pair
 *     76,280,251  30216      2,524  12300      6,201 Direct Hidden Pair
 *    184,254,967  29278      6,293    853    216,008 Direct Naked Triple
 *    169,920,952  29225      5,814   1777     95,622 Direct Hidden Triple
 *     97,855,023  29091      3,363  18204      5,375 Locking
 *     46,651,802  20194      2,310   4269     10,928 Naked Pair
 *     40,356,091  19174      2,104   8350      4,833 Hidden Pair
 *     95,260,054  17665      5,392   1244     76,575 Naked Triple
 *     83,670,009  17348      4,823   1021     81,949 Hidden Triple
 *     57,220,586  17151      3,336   1295     44,185 Two String Kite
 *     50,410,316  15856      3,179    420    120,024 Swampfish
 *     68,524,587  15679      4,370    572    119,798 XY-Wing
 *     56,964,668  15279      3,728    285    199,876 XYZ-Wing
 *     90,307,573  15013      6,015    383    235,790 W-Wing
 *     48,045,933  14746      3,258    351    136,883 Skyscraper
 *     57,777,317  14563      3,967    451    128,109 Empty Rectangle
 *     67,955,459  14112      4,815    230    295,458 Swordfish
 *    157,352,309  14052     11,197    101  1,557,943 Naked Quad
 *    129,889,877  14033      9,256     11 11,808,170 Hidden Quad
 *    428,461,366  14032     30,534    312  1,373,273 Coloring
 *  1,408,882,005  13790    102,166    663  2,125,010 XColoring
 *  1,961,667,516  13548    144,793 129204     15,182 GEM
 *    103,724,266  12145      8,540      0          0 Jellyfish
 *    393,416,126  12145     32,393    871    451,683 WXYZ-Wing
 *    510,989,131  11591     44,084    877    582,655 VWXYZ-Wing
 *    636,199,928  11137     57,124    386  1,648,186 UVWXYZ-Wing
 *    525,946,077  10942     48,066     83  6,336,699 TUVWXYZ-Wing
 *  1,839,036,783  10894    168,811    887  2,073,322 Unique Rectangle
 *  1,139,694,201  10283    110,832    229  4,976,830 Finned Swampfish
 *  2,564,155,987  10097    253,952    282  9,092,751 Finned Swordfish
 * 14,983,891,340   9890  1,515,054   3917  3,825,348 ALS-XZ
 * 18,053,314,973   7162  2,520,708   3296  5,477,340 ALS-Wing
 *  8,859,084,237   4441  1,994,839    596 14,864,235 ALS-Chain
 *  3,981,054,266   3959  1,005,570    162 24,574,409 Death Blossom
 * 10,163,700,410   3816  2,663,443   1192  8,526,594 Unary Chain
 *  5,151,647,433   3314  1,554,510    231 22,301,504 Nishio Chain
 *  7,271,437,929   3110  2,338,082   4549  1,598,469 Multiple Chain
 * 10,364,200,487   1378  7,521,190   7558  1,371,288 Dynamic Chain
 *     93,848,599      3 31,282,866     30  3,128,286 Dynamic Plus
 * 92,138,135,155
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   137,644,704,100 (02:17)        93,955,429
 * NOTES:
 * 1. Last top1465 run took 02:17, 25 seconds faster, with Jellyfish.
 * 2. Release 6.30.128 2021-05-04 14:37:08 =>
 *    DiufSudoku_V6_30.128.2021-05-04.7z
 * 3. No idea what I should do next. Maybe nothing.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.129 2021-05-13 10:25:09</b> Build for backup before I experiment with
 * some GUI changes. A non-release build, but all tests pass anyway.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     19,359,300 101570        190 553450         34 Naked Single
 *     17,683,800  46225        382 155970        113 Hidden Single
 *     87,421,200  30628      2,854   5884     14,857 Direct Naked Pair
 *     74,455,600  30215      2,464  12300      6,053 Direct Hidden Pair
 *    185,843,600  29277      6,347    853    217,870 Direct Naked Triple
 *    164,674,100  29224      5,634   1777     92,669 Direct Hidden Triple
 *     99,759,800  29090      3,429  18204      5,480 Locking
 *     48,114,900  20193      2,382   4269     11,270 Naked Pair
 *     40,126,700  19173      2,092   8350      4,805 Hidden Pair
 *     96,705,000  17664      5,474   1244     77,737 Naked Triple
 *     81,801,300  17347      4,715   1021     80,118 Hidden Triple
 *     56,865,200  17150      3,315   1295     43,911 Two String Kite
 *     34,453,500  15855      2,173    420     82,032 Swampfish
 *     71,327,700  15678      4,549    572    124,698 XY-Wing
 *     57,120,500  15278      3,738    285    200,422 XYZ-Wing
 *     88,144,900  15012      5,871    383    230,143 W-Wing
 *     44,865,200  14745      3,042    351    127,821 Skyscraper
 *     55,871,600  14562      3,836    451    123,883 Empty Rectangle
 *     60,824,500  14111      4,310    230    264,454 Swordfish
 *    159,247,300  14051     11,333    101  1,576,705 Naked Quad
 *    125,781,500  14032      8,963     11 11,434,681 Hidden Quad
 *    412,763,900  14031     29,417    312  1,322,961 Coloring
 *  1,376,994,400  13789     99,861    663  2,076,914 XColoring
 *  1,944,406,600  13547    143,530 129205     15,049 GEM
 *    377,655,300  12143     31,100    871    433,588 WXYZ-Wing
 *    504,093,000  11589     43,497    877    574,792 VWXYZ-Wing
 *    618,649,100  11135     55,558    386  1,602,717 UVWXYZ-Wing
 *    515,542,000  10940     47,124     83  6,211,349 TUVWXYZ-Wing
 *  1,893,507,900  10892    173,843    887  2,134,732 Unique Rectangle
 *  1,136,341,600  10281    110,528    229  4,962,190 Finned Swampfish
 *  2,557,078,900  10095    253,301    282  9,067,655 Finned Swordfish
 * 14,506,942,000   9888  1,467,126   3915  3,705,476 ALS-XZ
 * 17,613,812,300   7162  2,459,342   3296  5,343,996 ALS-Wing
 *  8,485,136,300   4441  1,910,636    596 14,236,805 ALS-Chain
 *  3,982,628,600   3959  1,005,968    162 24,584,127 Death Blossom
 *  9,981,436,300   3816  2,615,680   1192  8,373,688 Unary Chain
 *  4,884,515,100   3314  1,473,903    231 21,145,087 Nishio Chain
 *  7,199,133,600   3110  2,314,833   4549  1,582,574 Multiple Chain
 * 10,198,821,100   1378  7,401,176   7558  1,349,407 Dynamic Chain
 *     96,973,500      3 32,324,500     30  3,232,450 Dynamic Plus
 * 89,956,878,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  134,077,309,500 (02:14)  91,520,347
 * NOTES:
 * 1. Last top1465 run took 02:14, 3 seconds faster, but BFIIK what config.
 * 2. A Non-release build, to back-up and remove the log-files.
 * 3. Next I experiment with making several GUI controls use the same event
 *    handler, so that I can just press enter in grid, view-number-combo, and
 *    hints-tree and it'll apply the current hint and get the next one.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.130 2021-05-17 10:19:10</b> GenerateDialog and TechSelectDialog are now
 * more flexible. The description in the GenerateDialog is a generated list of
 * Tech's in this Difficulty range. The Tech difficulty (a base for hints of
 * this type) is displayed in the TechSelectDialog. The user can look at
 * TechSelect and Generate side-by-side to see which tech's generate Difficulty
 * requires, but unfortunately TechSelect is still modal. sigh.
 * <p>
 * FYI I looked at the event handlers, but there propagation makes no sense to
 * me, so I gave up. I still think it's possible for each control event-handler
 * to delegate to a "generic" form-level event handler, but I can't see HOW.
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      28,968,500 101535        285 552860          52 Naked Single
 *      24,499,500  46249        529 155910         157 Hidden Single
 *     108,106,700  30658      3,526   5884      18,372 Direct Naked Pair
 *     101,726,600  30245      3,363  12313       8,261 Direct Hidden Pair
 *     224,181,300  29306      7,649    853     262,815 Direct Naked Triple
 *     203,392,400  29253      6,952   1797     113,184 Direct Hidden Triple
 *     119,831,500  29118      4,115  18194       6,586 Locking
 *      61,683,000  20225      3,049   4268      14,452 Naked Pair
 *      55,403,200  19207      2,884   8354       6,631 Hidden Pair
 *     126,927,700  17695      7,173   1246     101,868 Naked Triple
 *     111,705,600  17377      6,428   1017     109,838 Hidden Triple
 *      61,245,200  17181      3,564   1296      47,257 Two String Kite
 *      42,018,900  15885      2,645    420     100,045 Swampfish
 *      80,002,600  15708      5,093    572     139,864 XY-Wing
 *      66,131,500  15308      4,320    283     233,680 XYZ-Wing
 *     108,803,400  15043      7,232    383     284,081 W-Wing
 *      45,905,700  14776      3,106    350     131,159 Skyscraper
 *      63,264,800  14593      4,335    451     140,276 Empty Rectangle
 *      73,325,000  14142      5,184    230     318,804 Swordfish
 *     501,761,600  14082     35,631    312   1,608,210 Coloring
 *   1,791,742,600  13840    129,461    664   2,698,407 XColoring
 *   2,370,067,300  13597    174,308 129849      18,252 GEM
 *      19,856,800  12192      1,628      0           0 Jellyfish
 *     478,146,600  12192     39,218    928     515,244 WXYZ-Wing
 *     668,803,700  11627     57,521    882     758,280 VWXYZ-Wing
 *     831,104,000  11170     74,405    394   2,109,401 UVWXYZ-Wing
 *     665,841,000  10972     60,685     83   8,022,180 TUVWXYZ-Wing
 *     344,049,000  10924     31,494      4  86,012,250 STUVWXYZ-Wing
 *   2,386,183,700  10922    218,474    891   2,678,096 Unique Rectangle
 *   1,359,340,000  10308    131,872    230   5,910,173 Finned Swampfish
 *   3,090,471,500  10121    305,352    281  10,998,119 Finned Swordfish
 *   4,133,428,300   9915    416,886     11 375,766,209 Finned Jellyfish
 *  18,189,039,000   9907  1,835,978   3950   4,604,820 ALS-XZ
 *  24,185,097,700   7173  3,371,685   3306   7,315,516 ALS-Wing
 *  10,125,477,600   4443  2,278,973    600  16,875,796 ALS-Chain
 *   4,509,156,100   3957  1,139,539    163  27,663,534 Death Blossom
 *   1,150,254,000   3813    301,666     11 104,568,545 Sue De Coq
 *  12,136,811,300   3811  3,184,678   1193  10,173,353 Unary Chain
 *   5,323,764,700   3308  1,609,360    218  24,420,938 Nishio Chain
 *   8,489,509,900   3108  2,731,502   4569   1,858,067 Multiple Chain
 *  11,208,957,500   1374  8,157,902   7527   1,489,166 Dynamic Chain
 *     108,720,100      3 36,240,033     30   3,624,003 Dynamic Plus
 * 115,774,707,100
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   173,260,347,000 (02:53)       118,266,448
 * NOTES:
 * 1. Last top1465 run took 02:53, 39 secs slower, but I'm using STUVWXYZ-Wing,
 *    FinnedJellyfish, DeathBlossom, and Sue De Coq; so it's all bloody good.
 *    Anything under about 5 minutes feels "pretty snappy" in the GUI.
 * 2. A Non-release build, to back-up and remove the log-files.
 * 3. Next I really don't know.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.131 2021-05-19 08:07:11</b> Building for release because we're going
 * shopping today. I've been pissing-about with BasicFisherman to make it
 * faster than BasicFisherman, and failing. sigh.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,251,900 101447        199 553440         36 Naked Single
 *     15,889,800  46103        344 155980        101 Hidden Single
 *     85,186,000  30505      2,792   5884     14,477 Direct Naked Pair
 *     77,786,100  30092      2,584  12313      6,317 Direct Hidden Pair
 *    188,329,700  29153      6,460    853    220,785 Direct Naked Triple
 *    166,498,300  29100      5,721   1797     92,653 Direct Hidden Triple
 *     96,065,700  28965      3,316  18203      5,277 Locking
 *     48,071,800  20069      2,395   4269     11,260 Naked Pair
 *     41,759,400  19049      2,192   8357      4,996 Hidden Pair
 *     96,761,900  17537      5,517   1244     77,782 Naked Triple
 *     82,827,500  17220      4,809   1021     81,123 Hidden Triple
 *     36,552,800  17023      2,147    620     58,956 Swampfish
 *     53,020,100  16787      3,158   1109     47,808 Two String Kite
 *     69,311,400  15678      4,420    572    121,173 XY-Wing
 *     59,104,600  15278      3,868    285    207,384 XYZ-Wing
 *     87,834,200  15012      5,850    383    229,332 W-Wing
 *     41,489,400  14745      2,813    351    118,203 Skyscraper
 *     54,627,700  14562      3,751    451    121,125 Empty Rectangle
 *     79,500,900  14111      5,633    230    345,656 Swordfish
 *    413,795,700  14051     29,449    312  1,326,268 Coloring
 *  1,379,306,800  13809     99,884    663  2,080,402 XColoring
 *  1,963,830,900  13567    144,750 129187     15,201 GEM
 *    101,914,300  12164      8,378      0          0 Jellyfish
 *    378,247,400  12164     31,095    929    407,155 WXYZ-Wing
 *    499,497,500  11598     43,067    879    568,256 VWXYZ-Wing
 *    622,960,400  11143     55,905    390  1,597,334 UVWXYZ-Wing
 *    515,965,200  10947     47,133     83  6,216,448 TUVWXYZ-Wing
 *  1,870,560,600  10899    171,626    887  2,108,862 Unique Rectangle
 *  1,137,611,600  10288    110,576    229  4,967,736 Finned Swampfish
 *  2,557,964,100  10102    253,213    282  9,070,794 Finned Swordfish
 * 15,967,336,900   9895  1,613,677   3954  4,038,274 ALS-XZ
 * 21,225,784,800   7162  2,963,667   3296  6,439,861 ALS-Wing
 *  9,312,628,500   4441  2,096,966    596 15,625,215 ALS-Chain
 *  3,902,142,600   3959    985,638    162 24,087,300 Death Blossom
 *  9,851,201,000   3816  2,581,551   1192  8,264,430 Unary Chain
 *  4,829,186,400   3314  1,457,207    231 20,905,568 Nishio Chain
 *  7,139,132,600   3110  2,295,541   4549  1,569,385 Multiple Chain
 * 10,130,730,900   1378  7,351,764   7558  1,340,398 Dynamic Chain
 *    111,687,200      3 37,229,066     30  3,722,906 Dynamic Plus
 * 95,312,354,600
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  140,409,620,900 (02:20)  95,842,744
 * NOTES:
 * 1. Last top1465 run took 02:20, 19 secs faster than previous, but no longer
 *    using STUVWXYZ-Wing, FinnedJellyfish, and Sue De Coq. It's all good.
 *    Anything under about 5 minutes feels "pretty snappy" in the GUI. Overall,
 *    BasicFisherman is about 8 seconds slower than BasicFisherman so I stick
 *    with the original BasicFisherman, which keeps getting better and better.
 * 2. Release 6.30.131 2021-05-19 08:07:11
 *    as DiufSudoku_V6_30.131.2021-05-19.7z
 * 3. Next I really don't know, bit I'll think of something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.132 2021-05-25 09:24:12</b> Release coz we're shopping today. I've been
 * updating comments, hint explanations, and documentation. Still no comprende
 * why BigWing swaps x and z. It's nonsensical.
 * <pre>
 *        time(ns)  calls   time/call  elims      time/elim hinter
 *      18,828,900 102068         184 554470             33 Naked Single
 *      17,486,400  46621         375 157780            110 Hidden Single
 *      85,163,200  30843       2,761   5892         14,454 Direct Naked Pair
 *      76,446,400  30430       2,512  12303          6,213 Direct Hidden Pair
 *     184,995,500  29493       6,272    841        219,970 Direct Naked Triple
 *     171,361,500  29441       5,820   1814         94,466 Direct Hidden Triple
 *      95,830,900  29303       3,270  18193          5,267 Locking
 *      46,525,900  20398       2,280   4299         10,822 Naked Pair
 *      45,353,800  19357       2,343   8344          5,435 Hidden Pair
 *      94,524,000  17839       5,298   1246         75,861 Naked Triple
 *      84,473,200  17520       4,821    988         85,499 Hidden Triple
 *      37,788,800  17341       2,179    619         61,048 Swampfish
 *      54,292,700  17098       3,175   1104         49,178 Two String Kite
 *      70,127,300  15994       4,384    568        123,463 XY-Wing
 *      57,830,400  15603       3,706    301        192,127 XYZ-Wing
 *      83,890,000  15324       5,474    401        209,201 W-Wing
 *      46,178,600  15045       3,069    335        137,846 Skyscraper
 *      52,848,700  14871       3,553    457        115,642 Empty Rectangle
 *      80,966,200  14414       5,617    212        381,916 Swordfish
 *     409,988,000  14358      28,554    307      1,335,465 Coloring
 *   1,366,934,200  14117      96,828    710      1,925,259 XColoring
 *   1,982,025,400  13873     142,869 130980         15,132 GEM
 *     145,234,200  12457      11,658     88      1,650,388 Naked Quad
 *     113,419,500  12442       9,115     11     10,310,863 Hidden Quad
 *     415,299,900  12441      33,381    875        474,628 WXYZ-Wing
 *     562,353,200  11887      47,308    856        656,954 VWXYZ-Wing
 *     693,474,800  11447      60,581    385      1,801,233 UVWXYZ-Wing
 *     568,793,000  11251      50,554     78      7,292,217 TUVWXYZ-Wing
 *   1,783,294,100  11206     159,137    894      1,994,736 Unique Rectangle
 *   1,158,449,900  10597     109,318    220      5,265,681 Finned Swampfish
 *   2,583,552,200  10419     247,965    265      9,749,253 Finned Swordfish
 *  15,546,156,200  10218   1,521,448   3907      3,979,052 ALS-XZ
 *  22,172,228,900   7494   2,958,664   3331      6,656,328 ALS-Wing
 *   9,517,424,600   4744   2,006,202    607     15,679,447 ALS-Chain
 *   4,158,205,300   4246     979,322    168     24,751,222 Death Blossom
 *  28,088,612,800   4096   6,857,571   3111      9,028,805 Kraken Swampfish
 *  82,878,788,400   1676  49,450,351    196    422,850,961 Kraken Swordfish
 * 473,152,644,400   1511 313,138,745     42 11,265,539,152 Kraken Jellyfish
 *   3,336,874,700   1473   2,265,359      0              0 Unary Chain
 *   2,196,823,600   1473   1,491,394      4    549,205,900 Nishio Chain
 *   3,298,564,800   1469   2,245,449    479      6,886,356 Multiple Chain
 *   8,809,970,300   1206   7,305,116   6495      1,356,423 Dynamic Chain
 *      79,200,600      3  26,400,200     30      2,640,020 Dynamic Plus
 * 666,423,225,400
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   725,414,460,200 (12:05)       495,163,454
 * NOTES:
 * 1. Last top1465 run took 12:05 with all Krakens, so no speed comparison.
 * 2. Release 6.30.132 2021-05-25 09:24:12
 *    as DiufSudoku_V6_30.132.2021-05-25.7z
 * 3. Next I don't know. I'll poke around to find something to improve.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.133 2021-05-30 14:26:13</b> Build to clean-up logs and create backup.
 * I've just been pissin-about making SDP/ALS hinters faster.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      20,451,800 101355        201 553150         36 Naked Single
 *      15,323,000  46040        332 155710         98 Hidden Single
 *     120,608,400  30469      3,958   5859     20,585 Direct Naked Pair
 *      82,417,800  30058      2,741  12343      6,677 Direct Hidden Pair
 *     192,548,100  29116      6,613    853    225,730 Direct Naked Triple
 *     165,780,100  29063      5,704   1735     95,550 Direct Hidden Triple
 *      97,420,500  28932      3,367  18191      5,355 Locking
 *      47,765,600  20045      2,382   4259     11,215 Naked Pair
 *      42,445,600  19033      2,230   8351      5,082 Hidden Pair
 *      97,514,000  17523      5,564   1235     78,958 Naked Triple
 *      82,441,600  17209      4,790   1029     80,118 Hidden Triple
 *      37,172,400  17010      2,185    615     60,442 Swampfish
 *      54,250,000  16774      3,234   1120     48,437 Two String Kite
 *      71,483,600  15654      4,566    566    126,296 XY-Wing
 *      60,670,300  15257      3,976    280    216,679 XYZ-Wing
 *     101,267,700  14996      6,752    379    267,197 W-Wing
 *      53,717,700  14730      3,646    356    150,892 Skyscraper
 *      28,590,300  14546      1,965     51    560,594 Empty Rectangle
 *      84,189,400  14495      5,808    233    361,327 Swordfish
 *     435,989,400  14436     30,201    348  1,252,843 Coloring
 *   1,483,513,200  14171    104,686    897  1,653,860 XColoring
 *   2,059,541,200  13862    148,574 129701     15,879 GEM
 *     147,364,100  12422     11,863     99  1,488,526 Naked Quad
 *     116,201,400  12404      9,368     11 10,563,763 Hidden Quad
 *     394,507,200  12403     31,807    882    447,287 WXYZ-Wing
 *     533,155,800  11845     45,011    883    603,800 VWXYZ-Wing
 *     660,595,700  11386     58,018    388  1,702,566 UVWXYZ-Wing
 *     539,086,400  11189     48,180     83  6,495,016 TUVWXYZ-Wing
 *   1,960,257,300  11141    175,949    899  2,180,486 Unique Rectangle
 *   1,200,250,300  10521    114,081    359  3,343,315 Finned Swampfish
 *   2,765,242,800  10211    270,810    308  8,978,061 Finned Swordfish
 *  16,381,530,900   9978  1,641,764   3949  4,148,273 ALS-XZ
 *  22,568,789,600   7231  3,121,115   3330  6,777,414 ALS-Wing
 *   9,835,713,400   4482  2,194,492    602 16,338,394 ALS-Chain
 *   4,141,170,600   3995  1,036,588    163 25,405,954 Death Blossom
 *     933,106,800   3851    242,302     11 84,827,890 Sue De Coq
 *  10,568,785,600   3849  2,745,852   1188  8,896,284 Unary Chain
 *   4,971,317,500   3339  1,488,864    258 19,268,672 Nishio Chain
 *   7,640,859,900   3108  2,458,449   4569  1,672,326 Multiple Chain
 *  10,558,235,700   1374  7,684,305   7527  1,402,714 Dynamic Chain
 *     159,617,000      3 53,205,666     30  5,320,566 Dynamic Plus
 * 101,510,889,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  144,324,912,200 (02:24)  98,515,298
 * NOTES:
 * 1. Last top1465 run took 02:24 using SueDeCoq
 * 2. 6.30.133 2021-05-30 14:26:13 (not for release, back-up only)
 *    as DiufSudoku_V6_30.133.2021-05-30.7z
 * 3. Next I don't know. Just pissin about finding hinters to make faster.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.134 2021-06-01 17:56:14</b> Made all ALS hinters cache ALSs and RCCs,
 * saving 20 seconds on top1465.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,462,800 101274        202 553040         37 Naked Single
 *     15,617,100  45970        339 155610        100 Hidden Single
 *     90,144,000  30409      2,964   5871     15,354 Direct Naked Pair
 *     78,614,200  29997      2,620  12315      6,383 Direct Hidden Pair
 *    190,081,000  29057      6,541    853    222,838 Direct Naked Triple
 *    166,867,800  29004      5,753   1745     95,626 Direct Hidden Triple
 *     93,538,900  28872      3,239  18184      5,144 Locking
 *     49,302,400  19998      2,465   4251     11,597 Naked Pair
 *     42,174,700  18990      2,220   8345      5,053 Hidden Pair
 *     96,516,600  17482      5,520   1231     78,405 Naked Triple
 *     84,067,300  17166      4,897   1025     82,016 Hidden Triple
 *     36,060,700  16967      2,125    618     58,350 Swampfish
 *     51,943,100  16731      3,104   1118     46,460 Two String Kite
 *     68,846,300  15613      4,409    572    120,360 XY-Wing
 *     56,134,900  15213      3,689    276    203,387 XYZ-Wing
 *     99,793,300  14955      6,672    382    261,239 W-Wing
 *     43,228,400  14689      2,942    354    122,114 Skyscraper
 *     22,569,800  14506      1,555     50    451,396 Empty Rectangle
 *     82,964,400  14456      5,739    233    356,070 Swordfish
 *    423,305,600  14397     29,402    354  1,195,778 Coloring
 *  1,372,291,100  14130     97,118    896  1,531,574 XColoring
 *  1,994,049,700  13822    144,266 130002     15,338 GEM
 *    149,236,000  12382     12,052    104  1,434,961 Naked Quad
 *    111,658,400  12363      9,031     11 10,150,763 Hidden Quad
 *    381,760,500  12362     30,881    886    430,880 WXYZ-Wing
 *    509,819,200  11803     43,194    881    578,682 VWXYZ-Wing
 *    632,418,400  11346     55,739    387  1,634,156 UVWXYZ-Wing
 *    519,412,400  11151     46,579     86  6,039,679 TUVWXYZ-Wing
 *  1,759,073,000  11102    158,446    904  1,945,877 Unique Rectangle
 *  1,147,139,300  10480    109,459    354  3,240,506 Finned Swampfish
 *  2,572,414,700  10175    252,817    310  8,298,111 Finned Swordfish
 * 15,077,235,600   9940  1,516,824   3961  3,806,421 ALS-XZ
 *  9,070,016,600   7179  1,263,409   3429  2,645,090 ALS-Wing
 *  8,241,562,700   4408  1,869,683    577 14,283,470 ALS-Chain
 *  2,762,092,300   3943    700,505    163 16,945,351 Death Blossom
 *    893,483,200   3799    235,189     11 81,225,745 Sue De Coq
 *  9,896,422,000   3797  2,606,379   1169  8,465,715 Unary Chain
 *  4,803,323,600   3298  1,456,435    255 18,836,563 Nishio Chain
 *  7,081,392,900   3070  2,306,642   4516  1,568,067 Multiple Chain
 * 10,000,969,400   1362  7,342,855   7452  1,342,051 Dynamic Chain
 *    127,207,900      3 42,402,633     30  4,240,263 Dynamic Plus
 * 80,915,212,200
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  124,793,538,600 (02:04)  85,183,302
 * NOTES:
 * 1. Last top1465 run took 02:04 which is 20 seconds faster.
 * 2. 6.30.134 2021-06-01 17:56:14 for release, tomorrow, I think.
 *    as DiufSudoku_V6_30.134.2021-06-01.7z
 * 3. Next I don't know, but I'll find something to do.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.135 2021-06-04 16:04:15</b> Speed-up AAlsHinter by introducing new
 * AlsFinder and RccFinder classes. It's actually a second slower, but it's
 * cleaner, so I'm keeping it anyway. sigh.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,088,400 101260        198 553810         36 Naked Single
 *     17,897,800  45879        390 155480        115 Hidden Single
 *     81,645,700  30331      2,691   5859     13,935 Direct Naked Pair
 *     75,755,500  29920      2,531  12327      6,145 Direct Hidden Pair
 *    181,533,000  28979      6,264    853    212,817 Direct Naked Triple
 *    163,142,200  28926      5,639   1755     92,958 Direct Hidden Triple
 *     97,590,900  28793      3,389  18173      5,370 Locking
 *     44,900,500  19924      2,253   4261     10,537 Naked Pair
 *     40,907,100  18909      2,163   8346      4,901 Hidden Pair
 *     92,459,200  17400      5,313   1234     74,926 Naked Triple
 *     81,428,500  17082      4,766   1022     79,675 Hidden Triple
 *     36,473,300  16884      2,160    629     57,986 Swampfish
 *     49,009,100  16641      2,945   1117     43,875 Two String Kite
 *     70,585,400  15524      4,546    588    120,043 XY-Wing
 *     56,556,300  15116      3,741    280    201,986 XYZ-Wing
 *     85,675,500  14855      5,767    390    219,680 W-Wing
 *     39,745,100  14584      2,725    351    113,233 Skyscraper
 *     25,290,500  14403      1,755     50    505,810 Empty Rectangle
 *     82,518,000  14353      5,749    231    357,220 Swordfish
 *    436,705,800  14296     30,547    351  1,244,176 Coloring
 *  1,413,709,900  14031    100,756    901  1,569,045 XColoring
 *  2,000,643,600  13718    145,840 129286     15,474 GEM
 *    143,543,500  12274     11,694    104  1,380,225 Naked Quad
 *    111,474,800  12255      9,096     11 10,134,072 Hidden Quad
 *  3,737,474,300  12254    305,000   2210  1,691,164 Big-Wings
 *  1,901,311,500  11027    172,423    903  2,105,549 Unique Rectangle
 *  1,143,799,800  10404    109,938    352  3,249,431 Finned Swampfish
 *  2,573,831,100  10100    254,834    306  8,411,212 Finned Swordfish
 * 13,792,733,500   9867  1,397,864   3962  3,481,255 ALS-XZ
 *  9,188,664,200   7175  1,280,650   3406  2,697,787 ALS-Wing
 *  8,692,515,500   4435  1,959,980    567 15,330,715 ALS-Chain
 *  2,937,086,600   3975    738,889    166 17,693,292 Death Blossom
 * 10,102,402,400   3830  2,637,702   1206  8,376,784 Unary Chain
 *  5,055,027,200   3321  1,522,140    257 19,669,366 Nishio Chain
 *  7,140,070,600   3091  2,309,954   4537  1,573,742 Multiple Chain
 * 10,155,103,100   1371  7,407,077   7515  1,351,311 Dynamic Chain
 *    107,548,300      3 35,849,433     30  3,584,943 Dynamic Plus
 * 81,976,847,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  125,308,710,400 (02:05)  85,534,955
 * NOTES:
 * 1. Last top1465 run took 02:05 which is a second slower. sigh.
 * 2. 6.30.135 2021-06-04 16:04:15 for release, tomorrow, I think.
 *    as DiufSudoku_V6_30.135.2021-06-04.7z
 * 3. Next I keep trying to speed s__t up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.136 2021-06-05 11:19:16</b> Split RccFinder on forwardOnly.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,146,800 101260        198 553810         36 Naked Single
 *     15,736,700  45879        343 155480        101 Hidden Single
 *     86,808,100  30331      2,862   5859     14,816 Direct Naked Pair
 *     76,984,300  29920      2,573  12327      6,245 Direct Hidden Pair
 *    188,402,300  28979      6,501    853    220,870 Direct Naked Triple
 *    165,706,200  28926      5,728   1755     94,419 Direct Hidden Triple
 *     95,579,300  28793      3,319  18173      5,259 Locking
 *     48,427,300  19924      2,430   4261     11,365 Naked Pair
 *     41,464,600  18909      2,192   8346      4,968 Hidden Pair
 *     97,894,800  17400      5,626   1234     79,331 Naked Triple
 *     82,730,300  17082      4,843   1022     80,949 Hidden Triple
 *     36,111,100  16884      2,138    629     57,410 Swampfish
 *     52,957,200  16641      3,182   1117     47,410 Two String Kite
 *     69,899,400  15524      4,502    588    118,876 XY-Wing
 *     55,937,700  15116      3,700    280    199,777 XYZ-Wing
 *     96,270,400  14855      6,480    390    246,847 W-Wing
 *     44,678,900  14584      3,063    351    127,290 Skyscraper
 *     23,519,500  14403      1,632     50    470,390 Empty Rectangle
 *     82,188,000  14353      5,726    231    355,792 Swordfish
 *    425,487,500  14296     29,762    351  1,212,215 Coloring
 *  1,431,951,300  14031    102,056    901  1,589,291 XColoring
 *  1,995,385,000  13718    145,457 129286     15,433 GEM
 *    147,653,700  12274     12,029    104  1,419,747 Naked Quad
 *    111,278,300  12255      9,080     11 10,116,209 Hidden Quad
 *  3,712,255,500  12254    302,942   2210  1,679,753 Big-Wings
 *  1,860,080,000  11027    168,684    903  2,059,889 Unique Rectangle
 *  1,158,080,800  10404    111,311    352  3,290,002 Finned Swampfish
 *  2,632,071,900  10100    260,601    306  8,601,542 Finned Swordfish
 * 11,663,152,600   9867  1,182,036   3962  2,943,753 ALS-XZ
 *  9,528,221,800   7175  1,327,975   3406  2,797,481 ALS-Wing
 *  7,189,604,700   4435  1,621,105    567 12,680,078 ALS-Chain
 *  2,883,650,800   3975    725,446    166 17,371,390 Death Blossom
 * 10,222,799,100   3830  2,669,138   1206  8,476,616 Unary Chain
 *  4,862,184,100   3321  1,464,072    257 18,919,004 Nishio Chain
 *  7,249,707,000   3091  2,345,424   4537  1,597,907 Multiple Chain
 * 10,193,482,100   1371  7,435,070   7515  1,356,418 Dynamic Chain
 *    131,522,000      3 43,840,666     30  4,384,066 Dynamic Plus
 * 78,780,011,100
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  121,883,741,000 (02:01)  83,197,092
 * NOTES:
 * 1. Last top1465 run took 02:01 which is a whole 4 seconds faster. Wow. Sigh.
 * 2. 6.30.136 2021-06-05 11:19:16 for release now.
 *    as DiufSudoku_V6_30.136.2021-06-05.7z
 * 3. Next I keep trying to speed s__t up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.137 2021-06-09 15:28:17</b> Cleaning-up messy crap, and stuff I don't
 * use any longer.
 * <p>
 * Here's the release README:
 * <pre>
 * 1. LogViewHintRegexDialog: the Log-view gets its hint-regex from a custom
 *    dialog-box. The contents of combo-box's drop-down list is stored in the
 *    plain-text file: $HOME/DiufSudoku_log_view_hint_regexs.txt.
 *    A regex (regular expression) typed into combo-box will be saved at the
 *    top of this list. It's just a text-file, so you can maintain it using
 *    your text-file editor (Netbeans, vi, or whatever).
 *
 * 2. It's now BigWings OR S/T/U/V/WXYZWings in LogicalSolver.configureHinters.
 *
 * 3. LogicalSolverTester .log now has hinter usage ordered by numCalls then by
 *    there order in the wantedHinters list, so that hinters should always be
 *    listed in the order that they are executed.
 *
 * 4. gemSolve: The hintNumber is now a "normal" attribute in the Grid.
 *    It was static in AHint, which caused problems in gemSolve because it
 *    can't increment a "global" static, so all of grids caches were "dirty".
 *    However, gemSolve can increment the grid.attribute, because it can just
 *    throw-away it's own copy of the grid when it's finished with it. Simples.
 *
 * 5. Got da s__ts with gemSolve so kill consequentSingles (including gemSolve)
 *    in GEM. It was buggier than a VeeBuggerYou bus full of bettles. sigh.
 *    I'm keeping hintNumber in Grid though, because it's a better design.
 *
 * 6. Cleaned up LogicalSolver constructor. The wantedHintersList is private.
 *    Everything else uses the wantedHinters array, coz arrays are faster to
 *    iterate. Internal "category" lists gone. Added className to Tech so that
 *    constructor doesn't need to create unwanted hinters. Downside is that
 *    Settings references Tech and now Tech references all the individual
 *    IHinter implementations, which reference Tech. Inbreeders!
 *
 * 7. Aligned Pair/Triple/Quad went galavanting up the pop-charts this week,
 *    collecting more smack-heads than covid-jabs, so they ran home, because
 *    I still can't use them, because they're still too bloody slow. sigh.
 *
 * 8. I noticed memory leaks: Multiple grids are held in memory, after GC, but
 *    lacking the tools to trace the references that keep them in memory I know
 *    not why so many (a dozen) are being retained. I've cleaned-up all hint
 *    references, because the first one I tripped-over was a List of hints that
 *    should have been cleared, each with cells, each holding there whole grid.
 *    Actually solving this seems impossible. I need a tool that goes through
 *    memory after GC and shows not just what's retained but da reference chain
 *    that retains it, and such a tool doesn't exist, AFAIK.
 *
 * 9. Improved TechSelectDialog usability. Selecting a Tech which excludes
 *    other tech/s now unticks those techs automatically. Mutually exclusive
 *    tech's are also enforced in LogicalSolver.constructor so that you can't
 *    even hack-around it by changing the registry manually. I might want to
 *    rip that out: it's nice to be able to hack around problems by changing
 *    the registry directly.
 *
 * 10. RecursiveSolverTester updated. It now eats UnsolvableException except
 *    at the top level (depth==1). solveLogically should NEVER have eaten
 *    the UnsolvableException from AHint.apply, which at the top-level now
 *    propagates back to RecursiveSolverTester.process, which spits-and-quits;
 *    where-as recursiveSolve's loop eats UnsolvableException and just guesses
 *    again. So top-level UnsolvableException thrown, and others eaten. Ok.
 *
 *    Sort-of-sorted the isNoisy fiasco by setting it false and declaring that
 *    it can only be used for debugging. It's too verbose for "normal use".
 *
 * 11. Replaced the IActualHint marker interface with IPretendHint because I
 *    kept forgetting to implements IActualHint in new hint-types; so now the
 *    "pretend" hints (a small static set) are marked, so I need not remember
 *    to implement IActualHint in every "actual" bloody hint-type. sigh.
 *
 * 12. Added a ttlDiff column to LogicalSolverTester standard-output, to give
 *    a better feeling for the actual complexity of solving this puzzle. It's
 *    inspired by hobiwans approach. I used max-difficulty, because that's what
 *    made sense to me. Humans put more weigth on "how hard is it overall"
 *    verses "how hard is it's hardest".
 *    NOTE: This required a new getDifficultyTotal in AHint which defaults to
 *    getDifficulty, but is overridden in GEMHint and GemHintBig to take the
 *    number of maybes-eliminated and number of cells-set into account, so that
 *    a GEMHintBig doesn't have a far lower difficulty than setting all it's
 *    setPots as NakedSingles, which would happen if GEM was unwanted.
 *
 * 13. Ripped-out LogicalAnalyser.Mode because SPEED mode is never used, and
 *    doing so simplifies everything.
 *
 *       time(ns)  calls  time/call  elims time/elim hinter
 *     19,369,213 103316        187 566410        34 Naked Single
 *     15,812,327  46675        338 159030        99 Hidden Single
 *    100,423,521  30772      3,263  20697     4,852 Locking
 *     53,648,568  20814      2,577   7051     7,608 Naked Pair
 *     43,933,107  19335      2,272  13125     3,347 Hidden Pair
 *    119,288,747  17296      6,896   1673    71,302 Naked Triple
 *    102,824,292  16917      6,078   1189    86,479 Hidden Triple
 *     34,464,324  16707      2,062    615    56,039 Swampfish
 *     50,742,215  16466      3,081   1156    43,894 Two String Kite
 *     65,282,666  15310      4,264    592   110,274 XY-Wing
 *     55,076,448  14896      3,697    298   184,820 XYZ-Wing
 *     95,751,918  14618      6,550    393   243,643 W-Wing
 *     49,966,635  14346      3,482    348   143,582 Skyscraper
 *     22,226,166  14166      1,568     54   411,595 Empty Rectangle
 *     79,884,197  14112      5,660    235   339,932 Swordfish
 *    411,713,920  14054     29,295    364 1,131,082 Coloring
 *  1,453,036,574  13787    105,391    967 1,502,623 XColoring
 *  1,936,137,615  13461    143,833 126714    15,279 GEM
 *    139,986,721  12057     11,610    100 1,399,867 Naked Quad
 *  3,656,985,877  12039    303,761   2210 1,654,744 Big-Wings
 *  1,769,773,230  10815    163,640    889 1,990,746 Unique Rectangle
 *  1,037,810,071  10202    101,726    364 2,851,126 Finned Swampfish
 *  2,217,627,459   9888    224,274    304 7,294,827 Finned Swordfish
 * 11,612,466,183   9655  1,202,741   3922 2,960,853 ALS-XZ
 *  8,750,823,399   7019  1,246,733   3433 2,549,030 ALS-Wing
 * 11,460,415,607   4254  2,694,032   1755 6,530,151 Unary Chain
 *  8,264,740,796   3559  2,322,208   5589 1,478,751 Multiple Chain
 * 11,069,891,288   1498  7,389,780   8191 1,351,470 Dynamic Chain
 *    108,379,801      3 36,126,600     30 3,612,660 Dynamic Plus
 * 64,798,482,885
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  107,095,277,899 (01:47)  73,102,578
 * NOTES:
 * 1. Last top1465 run took 01:47 which is 14 seconds faster, but don't get
 *    excited, it's only faster for dropping slowish hinters.
 * 2. 6.30.137 2021-06-09 15:28:17 for release soon, I promise.
 *    as DiufSudoku_V6_30.137.2021-06-09.7z
 * 3. Next I keep cleaning s__t up, and looking to speed s__t up. And ripping
 *    old s__t out. Basically it's a s__tfest.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.138 2021-06-13 11:43:18</b> Still cleaning-up my own mess.
 * <p>
 * Here's the additions to README.txt:
 * <pre>
 * 14. Cheat: mouseDown on row and column headers highlight interesting values:
 * Left button on row number 1..9 highlights instances of this value in grid.
 * To keep the highlights drag the mouse into the grid before you release it.
 * Right button on row number 1..9 highlights:
 *    1..5: cells with that many potential values.
 *    6..8: cells with 2..digit-3 (6=3, 7=4, 8=5) potential values.
 *    9: solution values (the ultimate cheat)
 * Left button on column label 'A'..'I':
 *    'A': NakedSingle and HiddenSingle
 *    'B': Locking (Pointing and Claiming)
 *    'C': NakedPair
 *    'D': HiddenPair
 *    'E': NakedTriple
 *    'F': HiddenTriple
 *    'G': Swampfish
 *    'H': TwoStringKite
 *    'I': XYWing, XYZWing
 * Right button on the column label 'a'..'i':
 *    'a': W_Wing
 *    'b': Swordfish
 *    // skipped coloring: There is no sane "cheat" for a coloring hint
 *    'c': Skyscraper
 *    'd': EmptyRectangle
 *    'e': Jellyfish
 *    'f': NakedQuad
 *    'g': HiddenQuad
 *    'h': BigWings
 *    'i': URT
 *
 * 15. AlsFinderRecursive.getAlss now uses new Cells.array to not create new
 * Cell arrays on the fly, because Java's new array (and GC) is a bit slow.
 * </pre>
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,734,800 103583        200 566560         36 Naked Single
 *     15,952,500  46927        339 158800        100 Hidden Single
 *    101,514,400  31047      3,269  20755      4,891 Locking
 *     55,186,400  21027      2,624   7155      7,712 Naked Pair
 *     44,969,500  19504      2,305  13094      3,434 Hidden Pair
 *    119,018,500  17468      6,813   1659     71,741 Naked Triple
 *    104,288,700  17093      6,101   1188     87,785 Hidden Triple
 *     35,229,900  16872      2,088    648     54,367 Swampfish
 *     51,948,100  16619      3,125   1117     46,506 Two String Kite
 *     84,331,100  15502      5,440    597    141,258 XY-Wing
 *     68,665,600  15091      4,550    280    245,234 XYZ-Wing
 *     82,566,900  14830      5,567    387    213,351 W-Wing
 *     51,834,700  14560      3,560    352    147,257 Skyscraper
 *     22,925,500  14378      1,594     49    467,867 Empty Rectangle
 *     82,202,700  14329      5,736    236    348,316 Swordfish
 *    413,025,700  14269     28,945    354  1,166,739 Coloring
 *  1,388,664,700  13998     99,204    915  1,517,666 XColoring
 *  1,989,857,200  13690    145,351 128550     15,479 GEM
 *    137,846,100  12258     11,245    109  1,264,643 Naked Quad
 *    110,990,500  12238      9,069     11 10,090,045 Hidden Quad
 *  3,944,940,100  12237    322,378   2233  1,766,654 Big-Wings
 *  1,782,850,100  10979    162,387    922  1,933,676 Unique Rectangle
 *  1,047,196,900  10343    101,246    356  2,941,564 Finned Swampfish
 *  2,239,198,600  10034    223,161    304  7,365,784 Finned Swordfish
 * 12,192,881,600   9802  1,243,917   3925  3,106,466 ALS-XZ
 *  8,147,789,600   7143  1,140,667   3471  2,347,389 ALS-Wing
 *  7,688,534,800   4410  1,743,431    571 13,465,034 ALS-Chain
 *  2,788,824,700   3940    707,823    161 17,321,892 Death Blossom
 * 10,016,240,800   3797  2,637,935   1166  8,590,257 Unary Chain
 *  4,862,776,100   3300  1,473,568    255 19,069,710 Nishio Chain
 *  7,111,815,900   3072  2,315,044   4496  1,581,809 Multiple Chain
 * 10,006,792,300   1366  7,325,616   7483  1,337,270 Dynamic Chain
 *    133,295,900      3 44,431,966     30  4,443,196 Dynamic Plus
 * 76,944,890,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   121,428,109,300 (02:01)        82,886,081
 * NOTES:
 * 1. Last top1465 run took 02:01 using "normal" hinters, which is OK.
 * 2. 6.30.138 2021-06-13 11:43:18 for release asap, I promise.
 *    as DiufSudoku_V6_30.138.2021-06-13.7z
 * 3. Next I keep cleaning up my own mess. I need to make a concerted effort
 *    to locate and remove all unused methods.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.139</b> is MIA. I don't know how I stuffed-up, but stuff-up I did.
 * <hr>
 * <p>
 * <b>KRC 6.30.140 2021-06-19 12:06:20</b> Removed tech.nom. AHint.apply takes grid.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,605,628 103675        179 566410         32 NakedSingle
 *     17,692,755  47034        376 158930        111 HiddenSingle
 *    106,653,502  31141      3,424  20739      5,142 Locking
 *     84,124,326  21134      3,980   7095     11,856 NakedPair
 *     71,084,130  19619      3,623  13079      5,434 HiddenPair
 *    124,025,387  17588      7,051   1639     75,671 NakedTriple
 *    108,589,785  17217      6,307   1182     91,869 HiddenTriple
 *     56,212,274  17002      3,306    632     88,943 Swampfish
 *     51,668,454  16755      3,083   1111     46,506 TwoStringKite
 *     89,691,239  15644      5,733    596    150,488 XY_Wing
 *     68,761,424  15234      4,513    286    240,424 XYZ_Wing
 *    103,222,222  14967      6,896    373    276,735 W_Wing
 *     43,137,924  14705      2,933    342    126,134 Skyscraper
 *     22,047,826  14527      1,517     50    440,956 EmptyRectangle
 *     83,986,103  14477      5,801    234    358,914 Swordfish
 *    419,408,959  14416     29,093    357  1,174,815 Coloring
 *  1,356,354,358  14139     95,930    919  1,475,902 XColoring
 *  1,874,192,579  13825    135,565 129219     14,504 GEM
 *    141,758,574  12378     11,452    107  1,324,846 NakedQuad
 *    116,195,757  12359      9,401     11 10,563,250 HiddenQuad
 *  3,839,089,808  12358    310,656   2255  1,702,478 BigWings
 *  1,951,604,941  11080    176,137    912  2,139,917 URT
 *  1,220,019,776  10450    116,748    359  3,398,383 FinnedSwampfish
 *  2,640,073,412  10139    260,387    308  8,571,666 FinnedSwordfish
 * 12,464,900,591   9905  1,258,445   3959  3,148,497 ALS_XZ
 *  9,693,629,155   7224  1,341,864   3494  2,774,364 ALS_Wing
 * 14,114,182,416   4480  3,150,487   1076 13,117,269 ALS_Chain
 *  2,862,277,479   3624    789,811    134 21,360,279 DeathBlossom
 *    698,899,134   3502    199,571     11 63,536,284 SueDeCoq
 *  9,287,957,521   3500  2,653,702    866 10,725,124 UnaryChain
 *  4,693,789,075   3111  1,508,771    231 20,319,433 NishioChain
 *  7,031,908,363   2907  2,418,957   4153  1,693,211 MultipleChain
 * 10,200,514,109   1328  7,681,110   7283  1,400,592 DynamicChain
 *    103,495,701      3 34,498,567     30  3,449,856 DynamicPlus
 * 85,759,754,687
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  104,132,271,200 (01:44)  71,080,048
 * NOTES:
 * 1. Last top1465 run took 1:44 is 16 seconds faster than previous, but keep
 *    your pants on, that's only because Grid.AUTOSOLVE = true, so it's faster
 *    but each hinters reported elims are mine and all subsequent singles.
 * 2. Build 6.30.140 2021-06-19 12:06:20 for release tomorrow, I think.
 *    as DiufSudoku_V6_30.140.2021-06-19.7z
 * 3. Next I really don't know, but I'll think of something. This is ultimate
 *    ninja-level fast, but Knuth is still 100 times faster, and even solves
 *    invalid Sudokus. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.141 2021-06-22 16:02:21</b> LogicalSolver mySettings. Hinter summaries
 * in LogicalSolverTester log now in wantedHinters order. Replace keySet+get
 * with entrySet on all HashMaps. IMySet.visit. Split Frmu out of Frmt.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     22,378,929 103429        216 565210         39 NakedSingle
 *     19,233,942  46908        410 158700        121 HiddenSingle
 *    116,408,644  31038      3,750  20746      5,611 Locking
 *     90,674,796  21048      4,308   7082     12,803 NakedPair
 *     71,867,208  19537      3,678  13077      5,495 HiddenPair
 *    122,126,683  17512      6,973   1642     74,376 NakedTriple
 *    107,360,308  17139      6,264   1181     90,906 HiddenTriple
 *     56,463,428  16926      3,335    632     89,340 Swampfish
 *     51,588,278  16679      3,093   1128     45,734 TwoStringKite
 *     95,240,755  15551      6,124    591    161,151 XY_Wing
 *     73,833,498  15144      4,875    287    257,259 XYZ_Wing
 *    101,660,925  14876      6,833    372    273,282 W_Wing
 *     44,635,522  14613      3,054    345    129,378 Skyscraper
 *     22,404,977  14435      1,552     53    422,735 EmptyRectangle
 *     83,694,428  14382      5,819    231    362,313 Swordfish
 *    425,257,830  14322     29,692    366  1,161,906 Coloring
 *  1,595,606,924  14038    113,663   1024  1,558,209 XColoring
 *  1,922,120,159  13704    140,259 130027     14,782 GEM
 *    145,183,722  12274     11,828    107  1,356,857 NakedQuad
 *    116,063,357  12255      9,470     11 10,551,214 HiddenQuad
 *  3,932,462,205  12254    320,912   2240  1,755,563 BigWings
 *  1,856,580,428  10983    169,041    911  2,037,958 URT
 *  1,186,944,309  10353    114,647    367  3,234,180 FinnedSwampfish
 *  2,682,820,892  10036    267,319    312  8,598,784 FinnedSwordfish
 * 12,594,603,100   9797  1,285,557   3924  3,209,633 ALS_XZ
 *  9,166,641,316   7149  1,282,227   3478  2,635,607 ALS_Wing
 * 14,460,460,555   4415  3,275,302   1043 13,864,295 ALS_Chain
 *  2,956,826,521   3583    825,237    134 22,065,869 DeathBlossom
 *  9,264,861,318   3461  2,676,931    885 10,468,769 UnaryChain
 *  7,453,701,790   3071  2,427,125   4373  1,704,482 MultipleChain
 * 10,859,308,922   1398  7,767,746   7677  1,414,525 DynamicChain
 *    139,549,898      3 46,516,632     30  4,651,663 DynamicPlus
 * 81,838,565,567
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  99,386,563,100 (01:39)  67,840,657
 * NOTES:
 * 1. Last top1465 run took 1:39, which is 5 seconds faster again, and you CAN
 *    get excited: Grid.AUTOSOLVE is off (I checked), so that 20 seconds is coz
 *    we're not sorting hinters every time we print a summary, and not finding
 *    the Grid in AHint.apply. So yeah, cop that one up ya!
 *
 *    As a baseline HoDoKu took 1465 in 165,680 ms (02:45.680) at 113.092 msp
 *    with my fix to it's Nets (Dynamic Chaining), and now SE is nearly twice
 *    as fast as that. Two minutes is a HUGE psychological milestone for me,
 *    and twenty-one seconds under that feels damn good, and beating hobiwan,
 *    who (trust me on this) was a freeken genius, is totally amazing. I'm a
 *    bit bloody chuffed! Woo-____ing-hoo!
 *
 *    Juillerat took 17:48. I'll bet that SudokuMonster would fail to finish.
 *    So yeah, this is a bit smart, AND Pretty Bloody Fast (tm)!
 *
 * 2. Build 6.30.141 2021-06-22 16:02:21 for release ASAP.
 *    as DiufSudoku_V6_30.141.2021-06-22.7z
 * 3. Next I keep cleaning-up. Deleting unused methods, and looking for stuff
 *    that just looks wrong, and generally having a good ole clean-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.142 2021-06-23 10:06:22</b> Simplified Locking.pointFrom with the new
 * pointFromElims method, to not farnarkle my way around not repeating the same
 * code 6 times: just put it in a method and wear the extra stack-work, which
 * worked out to be less work (programming can be strange like that); and so
 * runs 4 seconds faster than previous, which is "quite a lot" if you consider
 * how little work was required in comparison to the months I've spent getting
 * no-where fast on the nominally "slow bits". Sigh. Java ain't MASM. Sigh.
 * It's more of an ASSMASM. lols.
 * <p>
 * Q: What do you get when you throw a dozen smart asses on a fire?
 * A: Burnt!
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,617,401 103429        170 565210         31 NakedSingle
 *     17,768,515  46908        378 158700        111 HiddenSingle
 *    106,920,164  31038      3,444  20746      5,153 Locking
 *     83,675,565  21048      3,975   7082     11,815 NakedPair
 *     68,803,512  19537      3,521  13077      5,261 HiddenPair
 *    119,572,312  17512      6,828   1642     72,821 NakedTriple
 *    105,303,683  17139      6,144   1181     89,164 HiddenTriple
 *     54,295,639  16926      3,207    632     85,910 Swampfish
 *     50,027,668  16679      2,999   1128     44,350 TwoStringKite
 *     89,826,836  15551      5,776    591    151,991 XY_Wing
 *     67,202,806  15144      4,437    287    234,156 XYZ_Wing
 *     94,084,520  14876      6,324    372    252,915 W_Wing
 *     42,529,713  14613      2,910    345    123,274 Skyscraper
 *     22,384,668  14435      1,550     53    422,352 EmptyRectangle
 *     81,136,560  14382      5,641    231    351,240 Swordfish
 *    408,919,135  14322     28,551    366  1,117,265 Coloring
 *  1,432,148,805  14038    102,019   1024  1,398,582 XColoring
 *  1,825,060,070  13704    133,177 130027     14,036 GEM
 *    141,186,498  12274     11,502    107  1,319,499 NakedQuad
 *    112,000,938  12255      9,139     11 10,181,903 HiddenQuad
 *  3,762,832,953  12254    307,069   2240  1,679,836 BigWings
 *  1,908,457,474  10983    173,764    911  2,094,903 URT
 *  1,147,284,325  10353    110,816    367  3,126,115 FinnedSwampfish
 *  2,578,038,005  10036    256,879    312  8,262,942 FinnedSwordfish
 * 12,098,127,401   9797  1,234,880   3924  3,083,110 ALS_XZ
 *  8,384,164,705   7149  1,172,774   3478  2,410,628 ALS_Wing
 * 13,701,535,149   4415  3,103,405   1043 13,136,658 ALS_Chain
 *  2,884,794,843   3583    805,133    134 21,528,319 DeathBlossom
 *  8,971,996,801   3461  2,592,313    885 10,137,849 UnaryChain
 *  7,137,297,163   3071  2,324,095   4373  1,632,128 MultipleChain
 * 10,240,144,123   1398  7,324,852   7677  1,333,873 DynamicChain
 *    127,296,499      3 42,432,166     30  4,243,216 DynamicPlus
 * 77,882,434,449
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  95,703,756,999 (01:35)  65,326,796
 * NOTES:
 * 1. Last top1465 run took 1:35, which is 4 seconds faster again, which I'm
 *    pretty stoked about. Maybe I should spend some time optimising the bits
 *    that I've previously dismissed because at-face-value they don't look at
 *    all like they need optimising? Arcana cubed! Once more into the matrix.
 *    We will fight them on the bit shifts, we will beat them with an and not.
 *    And so are the dingos in our dives.
 * 2. Build 6.30.142 2021-06-23 10:06:22 for release today, now, pronto, ASAP!
 *    as DiufSudoku_V6_30.141.2021-06-22.7z
 * 3. Next I keep cleaning-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.143 2021-06-25 13:55:23</b> cleaning-up:
 * <pre>
 * Tech.nom gone.
 * AHint.apply takes grid.
 * HashMaps entrySet.
 * LogicalSolver clones THE_SETTINGS.
 * IMySet.visit.
 * Split Frmu from Frmt.
 * Locking.pointFromElims.
 * New Build class.
 * Improved AboutDialog.
 * Settings.THE renamed.
 * jar runs without files. Fixed A*E HitSet.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.144 2021-06-25 19:36:24</b> Fixed bugs in jar runs without files.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      24,098,800 116010        207 661550         36 NakedSingle
 *      20,131,800  49855        403 161260        124 HiddenSingle
 *     119,240,500  33729      3,535  21653      5,506 Locking
 *     100,961,900  23253      4,341   7576     13,326 NakedPair
 *      83,351,800  21568      3,864  13282      6,275 HiddenPair
 *     141,222,200  19475      7,251   1754     80,514 NakedTriple
 *     119,414,900  19063      6,264   1244     95,992 HiddenTriple
 *      64,903,100  18846      3,443    710     91,412 Swampfish
 *     113,319,700  18565      6,103    711    159,380 XY_Wing
 *   1,251,414,500  18042     69,361    371  3,373,084 AlignedPair
 *   6,873,305,600  17676    388,849    685 10,034,022 AlignedTriple
 *  23,446,820,200  17062  1,374,212    735 31,900,435 AlignedQuad
 *     220,464,600  16442     13,408   1893    116,463 AlignedPent
 *     141,760,300  14784      9,588   1035    136,966 AlignedHex
 *      82,231,000  13900      5,915     96    856,572 AlignedSept
 *      47,490,600  13818      3,436     50    949,812 AlignedOct
 *  42,489,073,600  13770  3,085,626  27443  1,548,266 UnaryChain
 *  10,794,089,300   7032  1,534,995   1478  7,303,172 NishioChain
 *  14,221,608,100   5842  2,434,373  11450  1,242,061 MultipleChain
 *  14,080,583,100   1817  7,749,357  10040  1,402,448 DynamicChain
 *     126,041,500      3 42,013,833     30  4,201,383 DynamicPlus
 * 114,561,527,100
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  144,125,837,400 (02:24)  98,379,411
 * NOTES:
 * 1. 02:24 is actually pretty good for a large A*E run.
 * 2. Build 6.30.144 2021-06-25 19:36:24 for release on Monday. I promise!
 *    as DiufSudoku_V6_30.144.2021-06-25.7z
 * 3. Next I keep cleaning-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.145 2021-06-27 22:23:25</b> Cheats obfiscated. One instance of each four
 * quick fox. Run TooFewClues and TooFewValues ONCE per puzzle.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,917,850 103646        182 566360         33 NakedSingle
 *     18,177,418  47010        386 158980        114 HiddenSingle
 *    110,691,015  31112      3,557  20737      5,337 Locking
 *     84,021,141  21107      3,980   7095     11,842 NakedPair
 *     70,106,865  19591      3,578  13078      5,360 HiddenPair
 *    119,595,952  17561      6,810   1639     72,968 NakedTriple
 *    106,148,625  17190      6,175   1182     89,804 HiddenTriple
 *     55,398,148  16975      3,263    632     87,655 Swampfish
 *     50,458,257  16728      3,016   1111     45,416 TwoStringKite
 *     90,067,995  15617      5,767    596    151,120 XY_Wing
 *     67,705,629  15207      4,452    286    236,732 XYZ_Wing
 *     94,547,855  14940      6,328    373    253,479 W_Wing
 *     44,945,509  14678      3,062    342    131,419 Skyscraper
 *     21,654,930  14500      1,493     50    433,098 EmptyRectangle
 *     80,204,382  14450      5,550    234    342,753 Swordfish
 *    416,313,137  14389     28,932    357  1,166,143 Coloring
 *  1,458,462,663  14112    103,349    919  1,587,010 XColoring
 *  1,838,777,953  13798    133,264 129208     14,231 GEM
 *    140,473,051  12353     11,371    107  1,312,832 NakedQuad
 *    112,553,638  12334      9,125     11 10,232,148 HiddenQuad
 *  3,771,079,386  12333    305,771   2251  1,675,290 BigWings
 *  1,816,870,200  11057    164,318    909  1,998,757 URT
 *  1,152,089,082  10429    110,469    359  3,209,161 FinnedSwampfish
 *  2,578,888,667  10118    254,881    308  8,373,015 FinnedSwordfish
 * 12,320,868,997   9884  1,246,546   3958  3,112,902 ALS_XZ
 *  8,755,116,026   7207  1,214,807   3484  2,512,949 ALS_Wing
 * 14,006,982,509   4472  3,132,151   1073 13,054,037 ALS_Chain
 *  2,875,718,783   3619    794,616    133 21,621,945 DeathBlossom
 *  8,968,797,369   3498  2,563,978    862 10,404,637 UnaryChain
 *  4,633,600,929   3113  1,488,468    231 20,058,878 NishioChain
 *  6,755,506,104   2909  2,322,277   4133  1,634,528 MultipleChain
 * 10,001,764,804   1332  7,508,832   7314  1,367,482 DynamicChain
 *    108,233,602      3 36,077,867     30  3,607,786 DynamicPlus
 * 82,744,738,471
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  101,341,344,501 (01:41)  69,174,979
 * NOTES:
 * 1. 1:41 is actually about 6 seconds slower than recent fastest, which is a
 *    bit dissapointing, but the GUI feel like lightning, so ship it anyway.
 * 2. Build 6.30.145 2021-06-27 22:23:25 for release tomorrow. I promise!
 *    as DiufSudoku_V6_30.145.2021-06-27.7z
 * 3. Next I keep cleaning-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.146 2021-06-29 22:26:26</b> Cleaning-up Log and stdout, which are still
 * a bloody nightmare. I should port it to Logging, but I'm too lazy.
 * Constantised common strings, but it's no faster. sigh.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,358,800 105400        193 566380         35 NakedSingle
 *     16,155,300  48762        331 158960        101 HiddenSingle
 *     82,123,000  32866      2,498  24087      3,409 Locking
 *     85,562,000  21617      3,958   7797     10,973 NakedPair
 *     72,710,000  19584      3,712   8216      8,849 HiddenPair
 *    118,579,700  17561      6,752   1639     72,348 NakedTriple
 *    106,228,200  17175      6,185   1008    105,385 HiddenTriple
 *     54,227,400  16975      3,194    632     85,802 Swampfish
 *     51,010,900  16728      3,049   1111     45,914 TwoStringKite
 *     91,552,100  15617      5,862    596    153,610 XY_Wing
 *     68,302,900  15207      4,491    286    238,821 XYZ_Wing
 *     95,422,400  14940      6,387    373    255,824 W_Wing
 *     43,784,900  14678      2,983    342    128,026 Skyscraper
 *     22,228,000  14500      1,532     50    444,560 EmptyRectangle
 *     80,771,900  14450      5,589    234    345,179 Swordfish
 *    418,794,600  14389     29,105    357  1,173,094 Coloring
 *  1,425,280,900  14112    100,997    919  1,550,904 XColoring
 *  1,839,590,000  13798    133,322 129208     14,237 GEM
 *    139,511,000  12353     11,293    107  1,303,841 NakedQuad
 *    113,462,900  12334      9,199     11 10,314,809 HiddenQuad
 *  3,764,924,300  12333    305,272   2251  1,672,556 BigWings
 *  1,800,686,600  11057    162,854    909  1,980,953 URT
 *  1,164,928,700  10429    111,700    359  3,244,926 FinnedSwampfish
 *  2,603,740,100  10118    257,337    308  8,453,701 FinnedSwordfish
 * 12,211,340,500   9884  1,235,465   3958  3,085,230 ALS_XZ
 *  8,770,306,400   7207  1,216,914   3484  2,517,309 ALS_Wing
 * 14,022,799,300   4472  3,135,688   1073 13,068,778 ALS_Chain
 *  2,831,358,000   3619    782,359    133 21,288,406 DeathBlossom
 *  9,077,650,600   3498  2,595,097    862 10,530,917 UnaryChain
 *  4,798,446,900   3113  1,541,422    231 20,772,497 NishioChain
 *  6,913,852,500   2909  2,376,711   4133  1,672,841 MultipleChain
 *  9,980,861,000   1332  7,493,138   7314  1,364,624 DynamicChain
 *    108,197,300      3 36,065,766     30  3,606,576 DynamicPlus
 * 82,994,749,100
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  102,586,160,100 (01:42)  70,024,682
 * NOTES:
 * 1. 1:42 is another 1 second slower than last time, which is a worry.
 * 2. Build 6.30.146 2021-06-29 22:26:26 for release tomorrow. I insist!
 *    as DiufSudoku_V6_30.146.2021-06-29.7z
 * 3. Next I keep cleaning-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.147 2021-07-04 00:05:27</b> Not clean-up NakedSets Collections. maybes and
 * maybesSize, with no Values instances. Sexy GRID_WATCH. Turbo. Fixed TreeSet
 * comparator in HintsListOrdered with toString.
 * <pre>
 *       time(ns) calls  time/call  elims  time/elim hinter
 *     14,131,400 87511        161 471050         29 NakedSingle
 *     13,167,600 40406        325 142920         92 HiddenSingle
 *     61,236,600 26114      2,344  22440      2,728 Locking
 *     59,762,000 15967      3,742   7025      8,507 NakedPair
 *     51,595,200 14301      3,607   7774      6,636 HiddenPair
 *     81,741,100 12489      6,545   1467     55,719 NakedTriple
 *     73,772,800 12171      6,061    937     78,732 HiddenTriple
 *     37,975,600 12002      3,164    470     80,799 Swampfish
 *     37,336,300 11840      3,153    900     41,484 TwoStringKite
 *     62,957,800 10940      5,754    433    145,399 XY_Wing
 *     48,495,600 10646      4,555    217    223,482 XYZ_Wing
 *     65,623,200 10445      6,282    292    224,736 W_Wing
 *     31,409,100 10238      3,067    303    103,660 Skyscraper
 *     15,453,900 10085      1,532     44    351,225 EmptyRectangle
 *     55,982,900 10041      5,575    202    277,143 Swordfish
 *    294,884,800  9996     29,500    244  1,208,544 Coloring
 *    982,763,700  9808    100,200    754  1,303,400 XColoring
 *  1,299,716,500  9573    135,768  81864     15,876 GEM
 *     97,356,500  8642     11,265     87  1,119,040 NakedQuad
 *     79,138,500  8627      9,173     11  7,194,409 HiddenQuad
 *  2,741,642,500  8626    317,834   1799  1,523,981 BigWings
 *  1,444,066,600  7660    188,520    764  1,890,139 URT
 *    811,905,600  7141    113,696    297  2,733,688 FinnedSwampfish
 *  1,837,667,000  6885    266,908    244  7,531,422 FinnedSwordfish
 *  8,569,772,600  6701  1,278,879   3063  2,797,836 ALS_XZ
 *  5,222,444,100  4696  1,112,104   2546  2,051,234 ALS_Wing
 *  8,612,924,900  2732  3,152,607    731 11,782,387 ALS_Chain
 *  1,665,859,300  2164    769,805     93 17,912,465 DeathBlossom
 *  5,024,446,800  2079  2,416,761    608  8,263,892 UnaryChain
 *  2,468,782,900  1808  1,365,477    179 13,792,083 NishioChain
 *  2,923,041,200  1647  1,774,766   2193  1,332,896 MultipleChain
 *  3,683,253,000   696  5,292,030 164871     22,340 DynamicChain
 *    141,740,000     3 47,246,666   1800     78,744 DynamicPlus
 * 48,612,047,600
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  65,911,621,100 (01:05)  44,990,867
 * NOTES:
 * 1. 1:05 is impressive, but I bolted on a turbo, which blows head-gaskets.
 * 2. Build 6.30.147 is not releasable (Turbo). Apart from that it seems OK.
 *    as DiufSudoku_V6_30.147.2021-07-04.LOGS.7z
 * 3. Next I keep cleaning-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.148 2021-07-09 06:25:28</b> Reapplied Grid.numSet and totalSize, change how
 * WWing works, simplified cell.set method, Cells and Regions test-cases.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,225,500 105664        163  56618        304 NakedSingle
 *     18,917,300  49046        385  15939      1,186 HiddenSingle
 *     77,840,200  33107      2,351  24127      3,226 Locking
 *     80,812,000  21837      3,700   7785     10,380 NakedPair
 *     71,215,800  19817      3,593   8202      8,682 HiddenPair
 *    118,046,200  17795      6,633   1642     71,891 NakedTriple
 *    107,132,700  17394      6,159    991    108,105 HiddenTriple
 *     55,460,400  17204      3,223    607     91,368 Swampfish
 *     52,211,100  16971      3,076   1128     46,286 TwoStringKite
 *     86,835,300  15843      5,480    574    151,281 XY_Wing
 *     65,289,700  15454      4,224    281    232,347 XYZ_Wing
 *    107,501,700  15190      7,077    372    288,983 W_Wing
 *     64,471,500  14926      4,319    354    182,122 Skyscraper
 *     20,855,200  14743      1,414     53    393,494 EmptyRectangle
 *     84,492,400  14690      5,751    226    373,860 Swordfish
 *    437,452,500  14631     29,899    367  1,191,968 Coloring
 *  1,441,268,200  14342    100,492   1015  1,419,968 XColoring
 *  1,877,347,500  14014    133,962 131424     14,284 GEM
 *    141,722,800  12559     11,284     95  1,491,818 NakedQuad
 *    115,221,900  12542      9,186     11 10,474,718 HiddenQuad
 *  3,852,659,300  12541    307,205   2214  1,740,135 BigWings
 *  1,943,503,000  11303    171,945    917  2,119,414 URT
 *  1,115,237,300  10670    104,520    356  3,132,689 FinnedSwampfish
 *  2,379,650,800  10364    229,607    308  7,726,138 FinnedSwordfish
 * 12,673,940,600  10123  1,251,994   3936  3,220,005 ALS_XZ
 *  8,558,341,300   7454  1,148,154   3478  2,460,707 ALS_Wing
 * 14,913,764,500   4727  3,155,016   1090 13,682,352 ALS_Chain
 *  3,104,434,300   3852    805,927    130 23,880,263 DeathBlossom
 *  9,334,445,800   3737  2,497,844    920 10,146,136 UnaryChain
 *  5,791,577,100   3334  1,737,125   4033  1,436,046 MultipleChain
 *  3,833,349,700   1569  2,443,180   5616    682,576 DynamicChain
 *    123,376,000      3 41,125,333     30  4,112,533 DynamicPlus
 * 72,665,599,600
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  92,365,405,400 (01:32)  63,048,058
 * NOTES:
 * 1. 1:32 is 10 seconds better than previous, ignoring that 1:05 Turbo run.
 * 2. Build 6.30.148 is releasable. I'll ship it as soon as I can.
 *    as DiufSudoku_V6_30.148.2021-07-09.7z
 * 3. Next I think I give up. I'm struggling for motivation. We'll see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.149 2021-07-09 17:45:29</b> Settings written-to/read-from file instead of
 * the Windows registry, using diuf.sudoku.utils.MyPreferences class.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,570,300 105664        166  56618        310 NakedSingle
 *     19,864,000  49046        405  15939      1,246 HiddenSingle
 *     80,675,500  33107      2,436  24127      3,343 Locking
 *     81,472,100  21837      3,730   7785     10,465 NakedPair
 *     71,300,300  19817      3,597   8202      8,693 HiddenPair
 *    118,848,900  17795      6,678   1642     72,380 NakedTriple
 *    108,484,200  17394      6,236    991    109,469 HiddenTriple
 *     56,133,400  17204      3,262    607     92,476 Swampfish
 *     53,136,200  16971      3,130   1128     47,106 TwoStringKite
 *     89,658,200  15843      5,659    574    156,198 XY_Wing
 *     67,565,500  15454      4,372    281    240,446 XYZ_Wing
 *    108,485,800  15190      7,141    372    291,628 W_Wing
 *     64,315,000  14926      4,308    354    181,680 Skyscraper
 *     21,195,800  14743      1,437     53    399,920 EmptyRectangle
 *     85,469,100  14690      5,818    226    378,181 Swordfish
 *    444,356,700  14631     30,370    367  1,210,781 Coloring
 *  1,528,943,600  14342    106,606   1015  1,506,348 XColoring
 *  1,909,040,100  14014    136,223 131424     14,525 GEM
 *    143,437,300  12559     11,421     95  1,509,866 NakedQuad
 *    117,546,400  12542      9,372     11 10,686,036 HiddenQuad
 *  3,881,631,900  12541    309,515   2214  1,753,221 BigWings
 *  1,851,125,700  11303    163,772    917  2,018,675 URT
 *  1,124,726,100  10670    105,410    356  3,159,342 FinnedSwampfish
 *  2,413,868,100  10364    232,908    308  7,837,234 FinnedSwordfish
 * 12,866,266,900  10123  1,270,993   3936  3,268,868 ALS_XZ
 *  8,698,878,000   7454  1,167,008   3478  2,501,115 ALS_Wing
 * 14,967,540,500   4727  3,166,393   1090 13,731,688 ALS_Chain
 *  3,178,217,400   3852    825,082    130 24,447,826 DeathBlossom
 *  9,380,179,100   3737  2,510,082    920 10,195,846 UnaryChain
 *  5,993,422,000   3334  1,797,667   4033  1,486,095 MultipleChain
 *  3,863,707,800   1569  2,462,528   5616    687,982 DynamicChain
 *    134,988,500      3 44,996,166     30  4,499,616 DynamicPlus
 * 73,542,050,400
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  93,493,407,100 (01:33)  63,818,025
 * NOTES:
 * 1. 1:33 is a second slower than previous. Meh.
 * 2. Build 6.30.149 is releasable. I'll ship it as soon as I can.
 *    as DiufSudoku_V6_30.149.2021-07-09.7z
 * 3. Next I think I give up. I'm struggling for motivation. We'll see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.150 2021-07-12 09:58:30</b> Generate -> analyseDifficulty now validates,
 * and so rebuildMaybesAndS__t, solving all generate's index issues.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,363,600 105664        192  56618        359 NakedSingle
 *     20,333,700  49046        414  15939      1,275 HiddenSingle
 *     84,059,700  33107      2,539  24127      3,484 Locking
 *     83,798,700  21837      3,837   7785     10,764 NakedPair
 *     77,727,600  19817      3,922   8202      9,476 HiddenPair
 *    124,388,500  17795      6,990   1642     75,754 NakedTriple
 *    114,172,000  17394      6,563    991    115,208 HiddenTriple
 *     60,590,000  17204      3,521    607     99,818 Swampfish
 *     57,000,600  16971      3,358   1128     50,532 TwoStringKite
 *     96,694,000  15843      6,103    574    168,456 XY_Wing
 *     73,794,900  15454      4,775    281    262,615 XYZ_Wing
 *    144,982,200  15190      9,544    372    389,737 W_Wing
 *     70,537,600  14926      4,725    354    199,258 Skyscraper
 *     22,329,000  14743      1,514     53    421,301 EmptyRectangle
 *     90,361,800  14690      6,151    226    399,830 Swordfish
 *    476,084,500  14631     32,539    367  1,297,232 Coloring
 *  1,633,780,900  14342    113,915   1015  1,609,636 XColoring
 *  2,044,545,900  14014    145,893 131424     15,556 GEM
 *    150,791,400  12559     12,006     95  1,587,277 NakedQuad
 *    124,247,500  12542      9,906     11 11,295,227 HiddenQuad
 *  4,216,049,500  12541    336,181   2214  1,904,268 BigWings
 *  1,947,581,000  11303    172,306    917  2,123,861 URT
 *  1,195,780,600  10670    112,069    356  3,358,934 FinnedSwampfish
 *  2,582,818,900  10364    249,210    308  8,385,775 FinnedSwordfish
 * 13,670,076,800  10123  1,350,397   3936  3,473,088 ALS_XZ
 *  9,084,818,400   7454  1,218,784   3478  2,612,081 ALS_Wing
 * 16,194,658,700   4727  3,425,990   1090 14,857,485 ALS_Chain
 *  3,422,781,000   3852    888,572    130 26,329,084 DeathBlossom
 *  9,966,227,200   3737  2,666,905    920 10,832,855 UnaryChain
 *  6,369,406,900   3334  1,910,439   4033  1,579,322 MultipleChain
 *  4,099,857,100   1569  2,613,038   5616    730,031 DynamicChain
 *    132,892,000      3 44,297,333     30  4,429,733 DynamicPlus
 * 78,453,532,200
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  99,495,564,500 (01:39)  67,915,061
 * NOTES:
 * 1. 1:39 is 6 seconds slower than previous, but I'm not worried coz I was
 *    running Windows Media Player at the time, which is a CPU hog. So all in
 *    all it's just another prick I've ignored. Careful with that axe Eugeene.
 * 2. Build 6.30.150 is an improvement over existing. Ship it ASAP!
 *    as DiufSudoku_V6_30.150.2021-07-12.7z
 * 3. Next I think I give up. I'm struggling for motivation. We'll see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.151 2021-07-12 21:56:31</b> SiameseLocking factored-out of Locking, which
 * is now basically back to itself, and comprehensible.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,706,078 104096        198  56569        366 NakedSingle
 *     20,075,094  47527        422  15988      1,255 HiddenSingle
 *    151,846,967  31539      4,814  22805      6,658 Locking
 *     82,838,629  21661      3,824   7351     11,269 NakedPair
 *     71,503,445  19905      3,592   9893      7,227 HiddenPair
 *    124,622,236  17794      7,003   1640     75,989 NakedTriple
 *    109,231,380  17415      6,272   1240     88,089 HiddenTriple
 *     57,663,394  17204      3,351    607     94,997 Swampfish
 *     52,909,675  16971      3,117   1128     46,905 TwoStringKite
 *     95,813,705  15843      6,047    574    166,922 XY_Wing
 *     72,672,176  15454      4,702    281    258,619 XYZ_Wing
 *    137,465,528  15190      9,049    372    369,530 W_Wing
 *     73,338,038  14926      4,913    354    207,169 Skyscraper
 *     27,272,331  14743      1,849     53    514,572 EmptyRectangle
 *     90,975,723  14690      6,193    226    402,547 Swordfish
 *    457,792,141  14631     31,289    367  1,247,390 Coloring
 *  1,580,231,608  14342    110,182   1015  1,556,878 XColoring
 *  1,958,444,736  14014    139,749 131424     14,901 GEM
 *    147,069,741  12559     11,710     95  1,548,102 NakedQuad
 *    119,558,963  12542      9,532     11 10,868,996 HiddenQuad
 *  3,912,529,617  12541    311,979   2214  1,767,176 BigWings
 *  1,998,051,919  11303    176,771    917  2,178,900 URT
 *  1,157,234,357  10670    108,456    356  3,250,658 FinnedSwampfish
 *  2,444,092,166  10364    235,825    308  7,935,364 FinnedSwordfish
 * 13,081,469,677  10123  1,292,252   3936  3,323,544 ALS_XZ
 *  8,750,416,381   7454  1,173,922   3478  2,515,933 ALS_Wing
 * 15,125,561,186   4727  3,199,822   1090 13,876,661 ALS_Chain
 *  3,273,751,809   3852    849,883    130 25,182,706 DeathBlossom
 *  9,400,010,695   3737  2,515,389    920 10,217,402 UnaryChain
 *  5,999,747,649   3334  1,799,564   4033  1,487,663 MultipleChain
 *  3,838,233,194   1569  2,446,292   5616    683,446 DynamicChain
 *    144,825,400      3 48,275,133     30  4,827,513 DynamicPlus
 * 74,577,955,638
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  92,997,869,400 (01:32)  63,479,774
 * NOTES:
 * 1. 1:32 is 7 seconds faster than previous, but don't get excited, he was a
 *    bit slow. This was with Siamese on. The run after with it off was 4 secs
 *    slower, so it looks like Siamese is now faster.
 * 2. Build 6.30.151 2021-07-12 21:56:31 is releasable. Ship ASAP!
 *    as DiufSudoku_V6_30.151.2021-07-12.7z
 * 3. Next I think I give up. I'm struggling for motivation. We'll see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.152 2021-07-13 23:00:32</b> indexes maintained on the fly, instead of
 * rebuilding everything all the time; now we only rebuild after loading the
 * puzzle, or before analysing it in generate, but no more maintenance is
 * required, it maintains them itself, which is, surprisingly, no faster.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     16,050,100 104096        154  56569        283 NakedSingle
 *     18,394,200  47527        387  15988      1,150 HiddenSingle
 *    143,092,800  31539      4,537  22805      6,274 Locking
 *     78,237,600  21661      3,611   7351     10,643 NakedPair
 *     70,177,800  19905      3,525   9893      7,093 HiddenPair
 *    115,957,300  17794      6,516   1640     70,705 NakedTriple
 *    107,370,600  17415      6,165   1240     86,589 HiddenTriple
 *     56,361,600  17204      3,276    607     92,852 Swampfish
 *     51,712,400  16971      3,047   1128     45,844 TwoStringKite
 *     92,370,100  15843      5,830    574    160,923 XY_Wing
 *     67,039,200  15454      4,337    281    238,573 XYZ_Wing
 *    126,636,200  15190      8,336    372    340,419 W_Wing
 *     64,629,200  14926      4,329    354    182,568 Skyscraper
 *     22,539,500  14743      1,528     53    425,273 EmptyRectangle
 *     85,199,900  14690      5,799    226    376,990 Swordfish
 *    443,974,300  14631     30,344    367  1,209,739 Coloring
 *  1,490,680,100  14342    103,938   1015  1,468,650 XColoring
 *  1,870,243,100  14014    133,455 131424     14,230 GEM
 *    141,800,800  12559     11,290     95  1,492,640 NakedQuad
 *    116,550,100  12542      9,292     11 10,595,463 HiddenQuad
 *  3,814,533,100  12541    304,164   2214  1,722,914 BigWings
 *  1,864,195,500  11303    164,929    917  2,032,928 URT
 *  1,111,296,500  10670    104,151    356  3,121,619 FinnedSwampfish
 *  2,377,352,200  10364    229,385    308  7,718,675 FinnedSwordfish
 * 12,709,399,200  10123  1,255,497   3936  3,229,014 ALS_XZ
 *  8,601,752,300   7454  1,153,978   3478  2,473,189 ALS_Wing
 * 15,022,176,900   4727  3,177,951   1090 13,781,813 ALS_Chain
 *  3,166,943,000   3852    822,155    130 24,361,100 DeathBlossom
 *  9,236,445,600   3737  2,471,620    920 10,039,614 UnaryChain
 *  5,834,497,600   3334  1,749,999   4033  1,446,689 MultipleChain
 *  3,881,975,600   1569  2,474,171   5616    691,234 DynamicChain
 *    129,398,700      3 43,132,900     30  4,313,290 DynamicPlus
 * 72,928,983,100
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  91,666,974,900 (01:31)  62,571,313
 * NOTES:
 * 1. 1:31 is a whole second faster than previous. Wow. sigh. For I went I went
 *    through I was hoping for 5 seconds, at least.
 * 2. Build 6.30.152 2021-07-13 23:00:32 is releasable. Ship ASAP!
 *    as DiufSudoku_V6_30.152.2021-07-13.7z
 * 3. Next I think I give up. I'm struggling for motivation. We'll see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.153 2021-07-17 08:29:33</b> make generate stoppable, but batch secs slower.
 * <hr>
 * <p>
 * <b>KRC 6.30.154 2021-07-18 11:08:33</b> make generate stoppable, not 30 seconds slower.
 * I made the back-ground Generate thread, which replenishes the cache stop
 * with the existing "Generate/Stop" button, but then the batch, for reasons
 * I still do not understand, ran 30 seconds slower, so I went trolling around
 * the code base trying to workout what the hell has happened, and after fixing
 * a few ancillaries I tried it again and it's fixed, so I'm building it for
 * release now while my luck is in. I hate it when stuff like that happens and
 * I really hate being unable to explain it. All I can do is blame the JIT
 * compiler. It would appear that I changed a magic file, which forces the
 * compiler to actually recompile it, bring it into line with everything else,
 * so it all lines up and runs nicely. This all makes me feel stupid. FYI: The
 * ancillaries where renaming the hint-cache in Chaining from HintsList to
 * HintCache, and likewise both of it's implementations, to better reflect
 * there current status.
 * <p>
 * During GUI testing for this release I've discovered that the "Stop" button
 * is not working, and I'll be ____ed if I know why, but I'm building anyway.
 * It also appears to be MUCH better at generating IDKFA's: what used to take
 * thousands of puzzles now seems to take dozens, and I know not why. Maybe my
 * hinter selection is different, or one of the hinters isn't working? Anyway,
 * there's MUCH less motivation for the user to press "Stop", so I'm past
 * caring that the Generator refuses to stop!
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     15,487,500 104096        148  56569        273 NakedSingle
 *     18,480,500  47527        388  15988      1,155 HiddenSingle
 *    144,843,500  31539      4,592  22805      6,351 Locking
 *     83,934,400  21661      3,874   7351     11,418 NakedPair
 *     70,005,200  19905      3,516   9893      7,076 HiddenPair
 *    118,299,600  17794      6,648   1640     72,133 NakedTriple
 *    105,974,500  17415      6,085   1240     85,463 HiddenTriple
 *     58,004,300  17204      3,371    607     95,558 Swampfish
 *     51,835,300  16971      3,054   1128     45,953 TwoStringKite
 *     89,728,700  15843      5,663    574    156,321 XY_Wing
 *     68,817,200  15454      4,453    281    244,901 XYZ_Wing
 *    133,930,000  15190      8,816    372    360,026 W_Wing
 *     67,060,900  14926      4,492    354    189,437 Skyscraper
 *     22,650,700  14743      1,536     53    427,371 EmptyRectangle
 *     82,990,900  14690      5,649    226    367,216 Swordfish
 *    453,382,900  14631     30,987    367  1,235,375 Coloring
 *  1,494,354,900  14342    104,194   1015  1,472,270 XColoring
 *  1,893,442,600  14014    135,110 131424     14,407 GEM
 *    142,539,900  12559     11,349     95  1,500,420 NakedQuad
 *    114,955,600  12542      9,165     11 10,450,509 HiddenQuad
 *  3,933,814,000  12541    313,676   2214  1,776,790 BigWings
 *  1,923,113,700  11303    170,141    917  2,097,179 URT
 *  1,126,104,600  10670    105,539    356  3,163,215 FinnedSwampfish
 *  2,379,037,500  10364    229,548    308  7,724,147 FinnedSwordfish
 * 12,763,977,900  10123  1,260,888   3936  3,242,880 ALS_XZ
 *  8,567,650,300   7454  1,149,403   3478  2,463,384 ALS_Wing
 * 14,835,192,900   4727  3,138,394   1090 13,610,268 ALS_Chain
 *  3,164,093,600   3852    821,415    130 24,339,181 DeathBlossom
 *  8,883,513,700   3737  2,377,177    920  9,655,993 UnaryChain
 *  6,114,558,400   3334  1,834,000   4033  1,516,131 MultipleChain
 *  4,014,335,900   1569  2,558,531   5616    714,803 DynamicChain
 *    131,995,200      3 43,998,400     30  4,399,840 DynamicPlus
 * 73,068,106,800
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  91,655,292,000 (01:31)  62,563,339
 * NOTES:
 * 1. 1:31 is same as previous. I hoped it might be a bit faster, but it's
 *    cleaner, so I'll take it anyway.
 * 2. Build 6.30.154 2021-07-18 11:08:33 is releasable. Ship it!
 *    as DiufSudoku_V6_30.154.2021-07-18.7z
 * 3. Next I try to work-out why the bastard now won't stop in java.exe, when
 *    it did stop in the bloody IDE! I'm not sure it ever stopped in java.exe,
 *    simply because I never had call to test it previously. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.155 2021-07-18 17:56:35</b> make generate actually stop. sigh. Note that
 * when you catch and re-throw an exception the type that you caught is the
 * type that's thrown, not the type of the actual exception. Oops!
 * <pre>
 * NOTES:
 * 1. No need to re-run the batch. It's the GUI that matters. Generate stops!
 * 2. Build 6.30.155 2021-07-18 17:56:35 is releasable. Ship it ASAP!
 *    as DiufSudoku_V6_30.155.2021-07-18.7z
 * 3. Next I'm gonna needs me a stem gun, and a sianide pill. Vielette was HOT!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.156 2021-07-19 09:25:36</b> I wish I could stop stopping bloody Generate.
 * I think this is about the best I can do. It is still not perfect, but it
 * stops when you ask it to, and the "Generate" button reverts nicely, and you
 * can generate again having stopped, and it all seems to work fairly nicely
 * except for the stuff that doesn't. sigh.
 * <p>
 * We still have problems solving an IDKFA (and presumably other Difficulty,
 * it's just MUCH harder to see) while actively solving the generated IDKFA, so
 * it's better to wait for it to generate the replacement before solving the
 * current puzzle; which exactly what I was trying to avoid. I wanted to
 * generate a replacement "quitely" in the background, leaving the user to play
 * with the current puzzle, but that doesn't seem achievable coz of hinters
 * static statefull variables.
 * <p>
 * The ONLY practical answer is moving the factory to it's own JVM, and have it
 * generate a file (the current standard out will do), then parse those puzzles
 * into a multi-puzzle cache: say a 1024 puzzles per difficulty, with each
 * Difficulty stored in it's own cache-file. I haven't done this, I would just
 * like to. Basically, generate should be a batch job, and the GUI just loads
 * pre-generated puzzles.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     16,744,500 104096        160  56569        296 NakedSingle
 *     18,075,300  47527        380  15988      1,130 HiddenSingle
 *    143,300,300  31539      4,543  22805      6,283 Locking
 *     80,852,800  21661      3,732   7351     10,998 NakedPair
 *     70,491,600  19905      3,541   9893      7,125 HiddenPair
 *    116,106,300  17794      6,525   1640     70,796 NakedTriple
 *    106,533,400  17415      6,117   1240     85,914 HiddenTriple
 *     55,747,600  17204      3,240    607     91,841 Swampfish
 *     54,820,000  16971      3,230   1128     48,599 TwoStringKite
 *     94,021,100  15843      5,934    574    163,799 XY_Wing
 *     73,970,000  15454      4,786    281    263,238 XYZ_Wing
 *    140,137,200  15190      9,225    372    376,712 W_Wing
 *     65,485,000  14926      4,387    354    184,985 Skyscraper
 *     25,534,400  14743      1,731     53    481,781 EmptyRectangle
 *     84,429,400  14690      5,747    226    373,581 Swordfish
 *    456,610,000  14631     31,208    367  1,244,168 Coloring
 *  1,552,899,700  14342    108,276   1015  1,529,950 XColoring
 *  1,915,432,100  14014    136,679 131424     14,574 GEM
 *    140,094,500  12559     11,154     95  1,474,678 NakedQuad
 *    116,098,800  12542      9,256     11 10,554,436 HiddenQuad
 *  3,927,232,400  12541    313,151   2214  1,773,817 BigWings
 *  1,854,635,200  11303    164,083    917  2,022,502 URT
 *  1,132,142,400  10670    106,105    356  3,180,175 FinnedSwampfish
 *  2,389,589,800  10364    230,566    308  7,758,408 FinnedSwordfish
 * 12,654,400,300  10123  1,250,064   3936  3,215,040 ALS_XZ
 *  8,580,644,200   7454  1,151,146   3478  2,467,120 ALS_Wing
 * 14,901,575,600   4727  3,152,438   1090 13,671,170 ALS_Chain
 *  3,188,001,900   3852    827,622    130 24,523,091 DeathBlossom
 *  8,942,518,800   3737  2,392,967    920  9,720,129 UnaryChain
 *  6,161,753,300   3334  1,848,156   4033  1,527,833 MultipleChain
 *  3,978,247,200   1569  2,535,530   5616    708,377 DynamicChain
 *    124,950,600      3 41,650,200     30  4,165,020 DynamicPlus
 * 73,163,075,700
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  92,698,289,100 (01:32)  63,275,282
 * NOTES:
 * 1. 1:32 is a second slower than previous. Meh!
 * 2. Build 6.30.156 2021-07-19 09:25:36 is fixed, I think. Ship ASAP!
 *    as DiufSudoku_V6_30.156.2021-07-19.7z
 * 3. Next I'm gonna have a chook around for something to tidy-up.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.157 2021-07-20 08:51:37</b> jar-only-distribution rolls it's own Settings.
 * While "acceptance testing" the previous build I discovered jar-only-release
 * produces an annoying override warning msg-box when you generate, so now
 * Settings creates a default-settings-file from DEFAULT_DEFAULT_SETTINGS, if
 * the file doesn't exist yet.
 * NOTES:
 * 1. No LogicalSolverTester run because this is a GUI-only change only.
 * 2. Build 6.30.157 2021-07-20 08:51:37 is releasable. Ship ASAP!
 *    as DiufSudoku_V6_30.157.2021-07-20.7z
 * 3. Next I don't know. The story of my life really.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.158 2021-07-28 11:22:38</b> I'm building for a back-up before I change
 * AlsChain for speed, ie rewrite RccFinder "properly" in the OO-sense.
 * <pre>
 * 1. io.IO.load is now gui.GridLoader.load coz it's only used in the GUI, and
 *    asks the user questions, so it should be in gui package, so now it is.
 *    The grid now does all the actual loading of MagicTour (.mt), and "old"
 *    Text (.txt) formats still implemented, even-though they're deprecated.
 *
 *    I'll write a tool that rewrites *.txt files in-or-under a directory, and
 *    replace the "old" Text reader with the instruction "run this tool".
 *
 * 2. Grid now has constants for "the facts of life": 9, 10, 17, 27, 64, 81.
 *
 * 3. Idx.FIRST: Tried putting the results of Integer.numberOfTrailingZeros in
 *    a VERY large array of 134 million int's, but it's no faster. I presume
 *    that what we gain by not calling Integer.numberOfTrailingZeros millions
 *    of times we loose by decreased available RAM, working the GC harder, so
 *    the GC runs all-the-time, so no, it's no faster. sigh.
 *    I wish the bit-twiddling in Integer, Long and StrictMath (et al) was
 *    implemented in firmware! I also wish that long math wasn't slower than
 *    int math in Java. Paying for 7 64-bit cores and then using bloody half of
 *    them, because it's faster, really ____ing s__ts me!
 *
 * 4. Grid.otherCommonRegion moved house to Regions.otherCommon. It's used only
 *    in NakedSet. It turns-out that finding the "other" common region of 2 or
 *    3 cells is quite a complex and therefore interesting little problem. I
 *    vagely recall that somewhere in this system I smash the indexes of all of
 *    a cells three constraints (ARegion.index) into bitsets. I now wonder if
 *    that's how they should be stored; then to get the common regions you just
 *    and-together all of the cell.ribs (RegionIndexBitS). Food for thought,
 *    but I haven't done this because my callers aren't performance critical.
 *    I'm called by two methods:
 *    * NakedSet.claimFromOtherCommonRegion only to produce hint, so not often;
 *    * and NakedSet.createNakedSetDirectHint only when producing hint and only
 *      in the GUI when a putz wants Direct*, so I don't care.
 *    so performance of Regions.otherCommon is NOT important! That is WHY my
 *    Idx.FIRST idea was a dismal failure: everything I've done so far presumes
 *    it's calling Integer.numberOfTrailingZeros, so it's always the last-thing
 *    we do creating the hint; so my bottom-up performance seeking was stupid!
 *
 * 5. I refactored all the cheat methods into a new Cheats inner class to clean
 *    it up a bit, so now it's even more of a brain-bender. There's some clever
 *    ideas buried in all that obfuscation. It's still a pig though.
 *
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,826,800 104096        171  56569        315 NakedSingle
 *     19,350,600  47527        407  15988      1,210 HiddenSingle
 *    143,948,600  31539      4,564  22805      6,312 Locking
 *     80,580,500  21661      3,720   7351     10,961 NakedPair
 *     71,238,900  19905      3,578   9893      7,200 HiddenPair
 *    118,406,700  17794      6,654   1640     72,199 NakedTriple
 *    107,321,700  17415      6,162   1240     86,549 HiddenTriple
 *     55,994,600  17204      3,254    607     92,248 Swampfish
 *     52,783,700  16971      3,110   1128     46,794 TwoStringKite
 *     95,902,800  15843      6,053    574    167,078 XY_Wing
 *     71,176,000  15454      4,605    281    253,295 XYZ_Wing
 *    144,272,600  15190      9,497    372    387,829 W_Wing
 *     67,834,000  14926      4,544    354    191,621 Skyscraper
 *     23,748,900  14743      1,610     53    448,092 EmptyRectangle
 *     85,934,200  14690      5,849    226    380,239 Swordfish
 *    458,108,000  14631     31,310    367  1,248,250 Coloring
 *  1,586,750,600  14342    110,636   1015  1,563,301 XColoring
 *  1,924,198,000  14014    137,305 131424     14,641 GEM
 *    146,732,600  12559     11,683     95  1,544,553 NakedQuad
 *    116,189,000  12542      9,263     11 10,562,636 HiddenQuad
 *  3,979,703,400  12541    317,335   2214  1,797,517 BigWings
 *  2,005,060,100  11303    177,391    917  2,186,543 URT
 *  1,146,024,400  10670    107,406    356  3,219,169 FinnedSwampfish
 *  2,429,491,400  10364    234,416    308  7,887,959 FinnedSwordfish
 * 12,408,149,800  10123  1,225,738   3936  3,152,477 ALS_XZ
 *  8,556,138,400   7454  1,147,858   3478  2,460,074 ALS_Wing
 * 15,103,646,400   4727  3,195,186   1090 13,856,556 ALS_Chain
 *  3,178,422,800   3852    825,135    130 24,449,406 DeathBlossom
 *  9,105,745,400   3737  2,436,645    920  9,897,549 UnaryChain
 *  6,212,022,000   3334  1,863,233   4033  1,540,298 MultipleChain
 *  3,988,134,600   1569  2,541,832   5616    710,137 DynamicChain
 *    134,771,700      3 44,923,900     30  4,492,390 DynamicPlus
 * 73,635,609,200
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  92,319,944,700 (01:32)  63,017,027
 * NOTES:
 * 1. 1:32 is same as previous.
 * 2. Build 6.30.158 2021-07-28 11:22:38 is releasable. No rush to ship.
 *    as DiufSudoku_V6_30.158.2021-07-28.7z
 * 3. Next I try to speed-up AlsChain by changing RccFinder.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.159 2021-07-30 06:38:39</b> RccFinder completely OO-decomposed.
 * <p>
 * RccFinder is now an interface defining find, getStartIndexes, getEndIndexes.
 * There's two abstract classes: RccFinderAbstract is "base", defining no-op
 * getStart/EndIndexes. RccFinderAbstractIndexed extends RccFinderAbstract,
 * furnishes starts and ends, and overrides getStart/EndIndexes to return them.
 * <pre>
 * There are four implementations:
 * 1. RccFinderForwardOnlyAllowOverlaps extends RccFinderAbstract
 * 2. RccFinderForwardOnlyNoOverlaps    extends RccFinderAbstract
 * 3. RccFinderAllAllowOverlaps			extends RccFinderAbstractIndexed
 * 4. RccFinderAllNoOverlaps			extends RccFinderAbstractIndexed
 * </pre>
 * And finally the RccFinderFactory exposes a static get method that AAlsHinter
 * now uses to get the RccFinder suitable for this Sudoku solving technique,
 * which boils down-to the two booleans: forwardOnly and allowOverlaps.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     16,816,123 104096        161  56569        297 NakedSingle
 *     21,018,731  47527        442  15988      1,314 HiddenSingle
 *    149,051,243  31539      4,725  22805      6,535 Locking
 *     80,926,821  21661      3,736   7351     11,008 NakedPair
 *     72,153,281  19905      3,624   9893      7,293 HiddenPair
 *    121,376,904  17794      6,821   1640     74,010 NakedTriple
 *    109,012,883  17415      6,259   1240     87,913 HiddenTriple
 *     57,012,733  17204      3,313    607     93,925 Swampfish
 *     52,798,840  16971      3,111   1128     46,807 TwoStringKite
 *     96,266,113  15843      6,076    574    167,710 XY_Wing
 *     72,093,013  15454      4,665    281    256,558 XYZ_Wing
 *    145,763,985  15190      9,596    372    391,838 W_Wing
 *     71,738,822  14926      4,806    354    202,652 Skyscraper
 *     23,784,591  14743      1,613     53    448,765 EmptyRectangle
 *     86,092,559  14690      5,860    226    380,940 Swordfish
 *    460,781,158  14631     31,493    367  1,255,534 Coloring
 *  1,546,280,514  14342    107,814   1015  1,523,429 XColoring
 *  1,928,691,007  14014    137,626 131424     14,675 GEM
 *    145,154,993  12559     11,557     95  1,527,947 NakedQuad
 *    117,374,906  12542      9,358     11 10,670,446 HiddenQuad
 *  4,030,041,813  12541    321,349   2214  1,820,253 BigWings
 *  1,957,738,567  11303    173,205    917  2,134,938 URT
 *  1,149,839,482  10670    107,763    356  3,229,886 FinnedSwampfish
 *  2,466,442,444  10364    237,981    308  8,007,930 FinnedSwordfish
 * 12,490,485,410  10123  1,233,871   3936  3,173,395 ALS_XZ
 *  8,641,239,605   7454  1,159,275   3478  2,484,542 ALS_Wing
 * 14,259,791,105   4727  3,016,668   1090 13,082,377 ALS_Chain
 *  3,195,190,808   3852    829,488    130 24,578,390 DeathBlossom
 *  8,937,150,109   3737  2,391,530    920  9,714,293 UnaryChain
 *  6,194,544,567   3334  1,857,991   4033  1,535,964 MultipleChain
 *  4,034,196,915   1569  2,571,189   5616    718,339 DynamicChain
 *    187,359,099      3 62,453,033     30  6,245,303 DynamicPlus
 * 72,918,209,144
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  91,904,891,000 (01:31)  62,733,713
 * NOTES:
 * 1. 1:31 is a second faster than previous. I was hoping for more. sigh.
 * 2. Build 6.30.159 2021-07-30 06:38:39 is releasable. No rush to ship.
 *    as DiufSudoku_V6_30.159.2021-07-30.7z
 * 3. Next I still need to speed-up RccFinder, but it's really hard.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.160 2021-08-11 07:10:40</b> build to clean-up old logs. I honestly can't
 * recall what the hell I've done, if anything, in the past two weeks. I just
 * need shooting. All I can recall is UniqueRectangle now keeps a Set of loops
 * instead of a List, to save some time processing the same loop multiple times
 * but it's only using ArrayList.contains, which is slow on a list-of-lists, so
 * there is an opportunity for improvement here, but not much, and its quite a
 * lot of work to replace the {@code ArrayList<ArrayList<Cell>>} with a
 * {@code Set<Idx>} so I haven't bothered. I tried, so I know its lots of work.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     16,210,460 104096        155  56569        286 NakedSingle
 *     18,169,254  47527        382  15988      1,136 HiddenSingle
 *    150,451,158  31539      4,770  22805      6,597 Locking
 *     80,705,775  21661      3,725   7351     10,978 NakedPair
 *     70,105,710  19905      3,522   9893      7,086 HiddenPair
 *    120,637,649  17794      6,779   1640     73,559 NakedTriple
 *    107,758,416  17415      6,187   1240     86,901 HiddenTriple
 *     56,040,464  17204      3,257    607     92,323 Swampfish
 *     53,612,067  16971      3,159   1128     47,528 TwoStringKite
 *     94,873,909  15843      5,988    574    165,285 XY_Wing
 *     71,192,428  15454      4,606    281    253,353 XYZ_Wing
 *    143,053,352  15190      9,417    372    384,552 W_Wing
 *     67,693,801  14926      4,535    354    191,225 Skyscraper
 *     22,676,593  14743      1,538     53    427,860 EmptyRectangle
 *     84,713,889  14690      5,766    226    374,840 Swordfish
 *    455,103,648  14631     31,105    367  1,240,064 Coloring
 *  1,648,355,594  14342    114,932   1015  1,623,995 XColoring
 *  1,912,660,897  14014    136,482 131424     14,553 GEM
 *    144,378,740  12559     11,496     95  1,519,776 NakedQuad
 *    116,769,450  12542      9,310     11 10,615,404 HiddenQuad
 *  4,047,540,226  12541    322,744   2214  1,828,157 BigWings
 *  1,513,917,416  11303    133,939    917  1,650,945 URT
 *  1,207,839,019  10670    113,199    356  3,392,806 FinnedSwampfish
 *  2,457,083,684  10364    237,078    308  7,977,544 FinnedSwordfish
 * 12,471,037,654  10123  1,231,950   3936  3,168,454 ALS_XZ
 *  8,724,798,978   7454  1,170,485   3478  2,508,567 ALS_Wing
 * 14,337,455,202   4727  3,033,098   1090 13,153,628 ALS_Chain
 *  3,237,111,313   3852    840,371    130 24,900,856 DeathBlossom
 *  9,184,468,754   3737  2,457,711    920  9,983,118 UnaryChain
 *  6,319,278,928   3334  1,895,404   4033  1,566,892 MultipleChain
 *  4,121,301,382   1569  2,626,705   5616    733,849 DynamicChain
 *    174,081,699      3 58,027,233     30  5,802,723 DynamicPlus
 * 73,231,077,509
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  91,985,187,300 (01:31)  62,788,523
 * NOTES:
 * 1. 1:31 is same as last time. I was hoping for faster. sigh.
 * 2. Build 6.30.160 2021-08-11 07:10:40 is releasable. No rush to ship.
 *    as DiufSudoku_V6_30.160.2021-08-11.7z
 * 3. Next I'll just keep poking around looking for something to play with.
 *    I've given-up any hope of enspeedonating the RccFinder. It's too hard!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.161 2021-08-12 12:57:42</b> BasicFisherman1 now faster than BasicFisherman.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     15,802,813 104096        151  56569        279 NakedSingle
 *     18,150,416  47527        381  15988      1,135 HiddenSingle
 *    149,079,592  31539      4,726  22805      6,537 Locking
 *     77,456,480  21661      3,575   7351     10,536 NakedPair
 *     69,514,702  19905      3,492   9893      7,026 HiddenPair
 *    116,519,079  17794      6,548   1640     71,048 NakedTriple
 *    106,319,129  17415      6,105   1240     85,741 HiddenTriple
 *     50,767,622  17204      2,950    607     83,636 Swampfish
 *     52,125,102  16971      3,071   1128     46,210 TwoStringKite
 *     94,391,073  15843      5,957    574    164,444 XY_Wing
 *     70,207,196  15454      4,542    281    249,847 XYZ_Wing
 *    134,598,671  15190      8,861    372    361,824 W_Wing
 *     67,943,012  14926      4,551    354    191,929 Skyscraper
 *     23,863,861  14743      1,618     53    450,261 EmptyRectangle
 *     59,591,450  14690      4,056    226    263,678 Swordfish
 *    456,281,918  14631     31,185    367  1,243,274 Coloring
 *  1,493,654,487  14342    104,145   1015  1,471,580 XColoring
 *  1,905,490,463  14014    135,970 131424     14,498 GEM
 *    142,915,545  12559     11,379     95  1,504,374 NakedQuad
 *    114,902,549  12542      9,161     11 10,445,686 HiddenQuad
 *  4,009,662,584  12541    319,724   2214  1,811,049 BigWings
 *  1,471,766,650  11303    130,210    917  1,604,979 URT
 *  1,196,147,398  10670    112,103    356  3,359,964 FinnedSwampfish
 *  2,456,601,760  10364    237,032    308  7,975,979 FinnedSwordfish
 * 12,416,913,503  10123  1,226,604   3936  3,154,703 ALS_XZ
 *  8,623,640,806   7454  1,156,914   3478  2,479,482 ALS_Wing
 * 14,094,024,427   4727  2,981,600   1090 12,930,297 ALS_Chain
 *  3,153,239,037   3852    818,597    130 24,255,684 DeathBlossom
 *  8,925,656,779   3737  2,388,455    920  9,701,800 UnaryChain
 *  5,988,962,706   3334  1,796,329   4033  1,484,989 MultipleChain
 *  3,931,791,410   1569  2,505,921   5616    700,105 DynamicChain
 *    140,048,400      3 46,682,800     30  4,668,280 DynamicPlus
 * 71,628,030,620
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  90,433,194,700 (01:30)  61,729,143
 * NOTES:
 * 1. 1:30 is a second faster than last time. I was hoping for more. sigh.
 * 2. Build 6.30.161 2021-08-11 11:14:41 is releasable, and there is now some
 *    pressure to ship, just because, after this release, we can get rid of the
 *    old BasicFisherman class, so there's really still no rush to ship, but I
 *    need to goto the library to complete the Census, so I may as well today.
 *    as DiufSudoku_V6_30.161.2021-08-12.7z
 * 3. Next I really don't know. I think I'll take a couple of days off and read
 *    my book: The Theory of Everything by Stephen Hawking. It's really quite
 *    interesting, but he has a singularity fixation. Each to there own. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.162 2021-09-04 04:57:42</b> new Hidden/NakedSetDirect, Tech.MULTI_CHAINERS,
 * de-obfuscate Cheats, no pass Grid.idxs to ALS-hinters, LogicalSolverBuilder.
 * Build for back-up and to clean-up logs.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     16,592,100 105691        156  56621        293 NakedSingle
 *     18,746,800  49070        382  15934      1,176 HiddenSingle
 *     75,685,200  33136      2,284  24129      3,136 Locking
 *     82,933,900  21864      3,793   7785     10,653 NakedPair
 *     73,695,300  19845      3,713   8203      8,983 HiddenPair
 *    119,528,100  17822      6,706   1642     72,794 NakedTriple
 *    109,187,300  17421      6,267    991    110,178 HiddenTriple
 *     51,650,800  17231      2,997    607     85,091 Swampfish
 *     52,771,500  16998      3,104   1128     46,783 TwoStringKite
 *     94,185,800  15870      5,934    574    164,086 XY_Wing
 *     70,197,700  15481      4,534    281    249,813 XYZ_Wing
 *    134,626,600  15217      8,847    372    361,899 W_Wing
 *     72,088,800  14953      4,821    354    203,640 Skyscraper
 *     21,799,100  14770      1,475     53    411,303 EmptyRectangle
 *     59,351,800  14717      4,032    226    262,618 Swordfish
 *    455,908,600  14658     31,103    367  1,242,257 Coloring
 *  1,537,712,900  14369    107,015   1014  1,516,482 XColoring
 *  1,905,802,100  14042    135,721 131453     14,497 GEM
 *    146,075,800  12586     11,606     95  1,537,640 NakedQuad
 *    119,705,600  12569      9,523     11 10,882,327 HiddenQuad
 *  4,029,164,700  12568    320,589   2218  1,816,575 BigWings
 *  1,463,252,000  11328    129,171    919  1,592,221 URT
 *  1,190,764,500  10694    111,348    356  3,344,844 FinnedSwampfish
 *  2,443,707,200  10388    235,243    308  7,934,114 FinnedSwordfish
 * 12,589,973,600  10147  1,240,758   3936  3,198,672 ALS_XZ
 *  9,000,570,900   7475  1,204,089   3487  2,581,178 ALS_Wing
 * 14,227,353,100   4739  3,002,184   1093 13,016,791 ALS_Chain
 *  3,253,049,100   3861    842,540    131 24,832,435 DeathBlossom
 *    781,693,900   3745    208,730     11 71,063,081 SueDeCoq
 *  9,177,397,100   3743  2,451,882    924  9,932,247 UnaryChain
 *  6,265,253,600   3336  1,878,073   4046  1,548,505 MultipleChain
 *  4,067,355,000   1567  2,595,631   5596    726,832 DynamicChain
 *    147,849,900      3 49,283,300     30  4,928,330 DynamicPlus
 * 73,855,630,400
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  92,629,113,000 (01:32)  63,228,063
 * NOTES:
 * 1. 1:32 is two seconds slower than last time. Meh! SueDeCoq is 0.7 of that,
 *    and I'm writing 1.3 off to a bad run.
 * 2. 6.30.162 2021-09-04 04:57:42 looks releasable, but there's no rush.
 *    DiufSudoku_V6_30.162.2021-09-04.7z
 * 3. Next I'm still trying to cleanup after myself.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.164 2021-09-21 09:28:44</b> failed to speed-up AlsWing and AlsChain.
 * sigh. I build just to clean-up the log-files.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,843,700 105390        169  56406        316 NakedSingle
 *     17,856,700  48984        364  15919      1,121 HiddenSingle
 *     76,041,900  33065      2,299  24133      3,150 Locking
 *     82,550,600  21782      3,789   7806     10,575 NakedPair
 *     70,923,000  19756      3,589   8210      8,638 HiddenPair
 *    117,540,600  17734      6,627   1638     71,758 NakedTriple
 *    109,602,500  17337      6,321    998    109,822 HiddenTriple
 *     48,669,700  17142      2,839    588     82,771 Swampfish
 *     52,615,600  16916      3,110   1126     46,727 TwoStringKite
 *     82,869,000  15790      5,248    588    140,933 XY_Wing
 *     69,001,100  15385      4,484    278    248,205 XYZ_Wing
 *    121,267,700  15124      8,018    376    322,520 W_Wing
 *     64,214,100  14853      4,323    346    185,589 Skyscraper
 *     20,576,400  14673      1,402     53    388,233 EmptyRectangle
 *     58,836,800  14620      4,024    219    268,661 Swordfish
 *    451,091,600  14564     30,973    358  1,260,032 Coloring
 *  1,477,355,700  14281    103,449   1017  1,452,660 XColoring
 *  1,903,572,400  13948    136,476 133396     14,270 GEM
 *    144,521,700  12500     11,561     96  1,505,434 NakedQuad
 *    121,772,400  12483      9,755     11 11,070,218 HiddenQuad
 *  2,973,498,800  12482    238,222   2158  1,377,895 BigWings
 *  1,477,872,900  11276    131,063    920  1,606,383 URT
 *  1,160,558,500  10642    109,054    350  3,315,881 FinnedSwampfish
 *  2,369,842,800  10341    229,169    302  7,847,161 FinnedSwordfish
 * 13,308,544,900  10104  1,317,156   3961  3,359,895 ALS_XZ
 * 10,437,110,100   7417  1,407,187   3425  3,047,331 ALS_Wing
 * 15,402,498,900   4695  3,280,617    828 18,602,051 ALS_Chain
 *  3,564,108,800   4038    882,642    137 26,015,392 DeathBlossom
 *    816,694,700   3917    208,500     11 74,244,972 SueDeCoq
 *  9,415,170,800   3915  2,404,896   1072  8,782,808 UnaryChain
 *  6,129,129,800   3459  1,771,936   4240  1,445,549 MultipleChain
 *  4,153,896,900   1595  2,604,324   5732    724,685 DynamicChain
 *    132,852,400      3 44,284,133     30  4,428,413 DynamicPlus
 * 76,450,503,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  95,028,890,900 (01:35)  64,866,137
 * NOTES:
 * 1. 1:35 is three seconds slower again than last time, so we're now up 5
 *    seconds, which has become a worry, so this build should not be released.
 * 2. 6.30.164 2021-09-21 09:28:44 should NOT be released, coz it's slower.
 *    DiufSudoku_V6_30.164.2021-09-21.LOGS.7z
 * 3. Next I continue trying to make AlsWing and AlsChain faster.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.166 2021-10-07 09:00:44</b> AlsChain1 intended to be a faster AlsChain,
 * but it's not right. It's faster, but relies on the Validator to suppress
 * invalid hints; so unusable. Build to clean-up old log-files.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,361,200 104239        166  58799        295 NakedSingle
 *     16,464,600  45440        362  16510        997 HiddenSingle
 *     64,499,200  28930      2,229  23694      2,722 Locking
 *     66,794,800  18047      3,701   7668      8,710 NakedPair
 *     58,015,300  16162      3,589   8212      7,064 HiddenPair
 *     89,643,200  14185      6,319   1643     54,560 NakedTriple
 *     81,299,700  13805      5,889    986     82,454 HiddenTriple
 *     36,884,200  13619      2,708    584     63,157 Swampfish
 *     44,481,900  13408      3,317   1095     40,622 TwoStringKite
 *     63,968,000  12313      5,195    575    111,248 XY_Wing
 *     53,453,000  11918      4,485    294    181,812 XYZ_Wing
 *     99,087,900  11646      8,508    367    269,994 W_Wing
 *     48,951,000  11389      4,298    328    149,240 Skyscraper
 *     17,299,500  11220      1,541     46    376,076 EmptyRectangle
 *     43,841,200  11174      3,923    214    204,865 Swordfish
 *    342,950,000  11125     30,826    281  1,220,462 Coloring
 *  1,119,587,800  10899    102,723    979  1,143,603 XColoring
 *  1,434,191,700  10610    135,173 109523     13,094 GEM
 *    105,367,400   9410     11,197     96  1,097,577 NakedQuad
 *     84,446,300   9392      8,991     11  7,676,936 HiddenQuad
 *  2,102,705,400   9391    223,906   2115    994,186 BigWings
 *  1,210,880,100   8217    147,362    861  1,406,364 URT
 *    835,121,600   7644    109,251    335  2,492,900 FinnedSwampfish
 *  1,712,877,100   7358    232,791    275  6,228,644 FinnedSwordfish
 *  8,045,870,300   7149  1,125,453   3707  2,170,453 ALS_XZ
 *  4,431,014,900   4692    944,376   2973  1,490,418 ALS_Wing
 *  5,219,155,900   2347  2,223,756   1981  2,634,606 ALS_Chain
 *    655,983,700    926    708,405     42 15,618,659 DeathBlossom
 *    205,183,800    892    230,026      0          0 SueDeCoq
 *  1,911,030,600    892  2,142,410    138 13,848,047 UnaryChain
 *  1,778,337,400    821  2,166,062    528  3,368,063 MultipleChain
 *  1,545,148,600    518  2,982,912   1579    978,561 DynamicChain
 *     55,859,600      1 55,859,600     10  5,585,960 DynamicPlus
 * 33,597,756,900
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  51,758,553,600 (00:51)  35,330,070
 * NOTES:
 * 1. 0:51 is 34 seconds faster, but relies on Validator, so ignore this.
 * 2. 6.30.166 2021-10-07 09:00:44 should NOT be released, coz it's slower.
 *    DiufSudoku_V6_30.166.2021-10-07.LOGS.7z
 * 3. Next I continue trying to make AlsWing and AlsChain faster.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.167 2021-10-10 16:35:47</b> AlsChain now takes about 4.5 times to find
 * 1.5 times as many hints. I also reordered the hinters but that's slower too.
 * sigh. I'm just not was worried about performance as I was coz it's all now
 * "basically fast enough", and any top1465 time under about three mins feels
 * "snappy enough" in the GUI (on an i7).
 * <pre>
 *        time(ns)  calls  time/call  elims   time/elim hinter
 *      18,704,000 105881        176  56291         332 NakedSingle
 *      18,413,700  49590        371  16013       1,149 HiddenSingle
 *      76,154,200  33577      2,268  24160       3,152 Locking
 *      83,243,900  22257      3,740   7904      10,531 NakedPair
 *      71,346,200  20221      3,528   8253       8,644 HiddenPair
 *     123,984,200  18164      6,825   1607      77,152 NakedTriple
 *     109,926,900  17761      6,189    972     113,093 HiddenTriple
 *      48,612,100  17575      2,765    600      81,020 Swampfish
 *      54,632,000  17344      3,149   1139      47,964 TwoStringKite
 *      79,150,000  16205      4,884    591     133,925 XY_Wing
 *      71,413,900  15801      4,519    297     240,450 XYZ_Wing
 *     131,738,400  15522      8,487    392     336,067 W_Wing
 *      65,826,300  15240      4,319    357     184,387 Skyscraper
 *      21,400,400  15049      1,422     51     419,615 EmptyRectangle
 *      58,590,300  14998      3,906    224     261,563 Swordfish
 *     454,250,400  14937     30,411    347   1,309,078 Coloring
 *   1,471,976,000  14668    100,352   1039   1,416,723 XColoring
 *   1,907,480,900  14327    133,138 134561      14,175 GEM
 *     149,308,200  12845     11,623     95   1,571,665 NakedQuad
 *     120,120,800  12828      9,363     11  10,920,072 HiddenQuad
 *   1,808,604,100  12827    140,999   1028   1,759,342 URT
 *   2,758,262,400  12111    227,748   2143   1,287,103 BigWings
 *   1,200,956,100  10921    109,967    350   3,431,303 FinnedSwampfish
 *   2,462,640,200  10621    231,865    308   7,995,585 FinnedSwordfish
 *   3,287,833,900  10382    316,685     17 193,401,994 FinnedJellyfish
 *   9,087,634,300  10368    876,507   2547   3,567,975 DeathBlossom
 *  10,945,866,600   8217  1,332,100   2078   5,267,500 ALS_XZ
 *   8,736,853,100   6898  1,266,577   2817   3,101,474 ALS_Wing
 *  39,856,095,400   4658  8,556,482   1613  24,709,296 ALS_Chain
 *     727,307,000   3362    216,331     15  48,487,133 SueDeCoq
 *   8,003,461,300   3359  2,382,691    640  12,505,408 UnaryChain
 *   5,729,791,900   3067  1,868,207   3579   1,600,947 MultipleChain
 *   3,989,257,700   1513  2,636,654   5389     740,259 DynamicChain
 *     116,520,600      3 38,840,200     30   3,884,020 DynamicPlus
 * 103,847,357,400
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  123,115,939,700 (02:03)  84,038,184
 * NOTES:
 * 1. 02:03 is 1:02 slower, sigh, but it no longer relies on Validator, and
 *    three minutes is still OK. So ____ It She'll Do!
 * 2. DiufSudoku_V6_30.167.2021-10-10.LOGS.7z can be released, but it's slower.
 * 3. Next I bend my head around making AlsChain faster (again).
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.168 2021-10-11 17:33:48</b> AlsChain faster, so now it takes 3 times to
 * find 1.5 times as many hints, by caching related RCC's by previousAlsIndex
 * and previousCandToExclude.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,544,700 105987        165  56524        310 NakedSingle
 *     18,564,800  49463        375  15994      1,160 HiddenSingle
 *     78,212,400  33469      2,336  24186      3,233 Locking
 *     86,071,400  22140      3,887   7869     10,938 NakedPair
 *     72,053,500  20114      3,582   8224      8,761 HiddenPair
 *    120,279,500  18070      6,656   1593     75,505 NakedTriple
 *    108,709,500  17673      6,151    975    111,496 HiddenTriple
 *     49,653,900  17485      2,839    602     82,481 Swampfish
 *     54,333,300  17253      3,149   1144     47,494 TwoStringKite
 *     84,744,200  16109      5,260    590    143,634 XY_Wing
 *     76,316,000  15704      4,859    293    260,464 XYZ_Wing
 *    135,699,900  15429      8,795    395    343,544 W_Wing
 *     67,260,900  15146      4,440    356    188,935 Skyscraper
 *     22,299,300  14957      1,490     51    437,241 EmptyRectangle
 *     59,144,500  14906      3,967    219    270,066 Swordfish
 *    464,730,800  14847     31,301    345  1,347,045 Coloring
 *  1,492,611,900  14580    102,373   1038  1,437,969 XColoring
 *  1,960,942,500  14245    137,658 132422     14,808 GEM
 *    147,093,200  12767     11,521     95  1,548,349 NakedQuad
 *    117,635,000  12750      9,226     11 10,694,090 HiddenQuad
 *  1,730,404,500  12749    135,728   1030  1,680,004 URT
 *  2,693,817,300  12031    223,906   2103  1,280,940 BigWings
 *  1,199,087,200  10862    110,392    351  3,416,202 FinnedSwampfish
 *  2,452,027,900  10561    232,177    315  7,784,215 FinnedSwordfish
 *  9,021,215,600  10316    874,487   2556  3,529,427 DeathBlossom
 * 10,897,785,200   8155  1,336,331   2101  5,186,951 ALS_XZ
 *  9,523,719,800   6824  1,395,621   2772  3,435,685 ALS_Wing
 * 30,231,822,000   4629  6,530,961   1565 19,317,458 ALS_Chain
 *  8,202,403,000   3390  2,419,587    641 12,796,260 UnaryChain
 *  5,949,096,700   3097  1,920,922   3574  1,664,548 MultipleChain
 *  4,035,466,600   1539  2,622,135   5469    737,880 DynamicChain
 *    137,932,700      3 45,977,566     30  4,597,756 DynamicPlus
 * 91,308,679,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  110,562,592,900 (01:50)  75,469,346
 * NOTES:
 * 1. 1:50 is 13 seconds faster, but we're still WELL up on the best-ever valid
 *    run at 1:30, but anything under 3 about minutes feels snappy enough in
 *    GUI, and I'm definately not as performance-centric as I was in the past.
 * 2. DiufSudoku_V6_30.168.2021-10-11.LOGS.7z can be released, but no rush.
 * 3. Next I don't know. I'll find something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.169 2021-10-13 10:18:49</b> Still playing with AlsChain: it now filters
 * its filter: the inline loop method (an O(n) operation) executes only when da
 * new ALS (a) might be in the loop. First check 'a' allIn allCellsInTheChain
 * (kept in an Idx, for speed) or allCells allIn 'a' (which is superfluous but
 * faster, WTF?)
 * <p>
 * If so it checks each ALS in chainAlss individually (the O(n) operation), but
 * before it examines each ALS it checks that these two ALSs might intersect
 * using the new ARegion.INTERSECTS array. If the two regions which contain
 * these two ALSs do not intersect that these two ALSs cannot intersect, let
 * alone contain each other, so it's a tad faster with this check.
 * <p>
 * <pre>
 * The upshot is that now (after this series of AlsChain changes):
 * PRE: 14,227,353,100   4739  3,002,184   1093 13,016,791 ALS_Chain 6.30.162
 * NOW: 32,486,048,500   4623  7,027,049   1665 19,511,140 ALS_Chain 6.30.168
 * So it now takes about 1.5 times as long per elimination, which is more than
 * twice as long in wait-time, but I reckon it's worth keeping anyway, despite
 * the performance loss, coz I (mostly) value accuracy more than speed (now).
 * You might quite reasonably demur.
 *
 * In a perfect world there would be user-options:
 * AlsChain ala hobiwan (hacked) or KRC (correct); and
 * AlsChain.LENGTH would also be optional 4..8.
 * And AlsChain wouldn't be so bloody slow.
 * But it's not an ideal world. sigh.
 *
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,218,300 105863        172  56423        322 NakedSingle
 *     19,212,000  49440        388  16003      1,200 HiddenSingle
 *     76,891,500  33437      2,299  24190      3,178 Locking
 *     83,312,100  22112      3,767   7866     10,591 NakedPair
 *     73,605,500  20079      3,665   8235      8,938 HiddenPair
 *    121,849,900  18033      6,757   1585     76,876 NakedTriple
 *    112,984,800  17641      6,404    977    115,644 HiddenTriple
 *     52,172,500  17451      2,989    608     85,810 Swampfish
 *     57,885,700  17216      3,362   1142     50,688 TwoStringKite
 *     82,454,000  16074      5,129    586    140,706 XY_Wing
 *     75,115,200  15671      4,793    289    259,914 XYZ_Wing
 *    131,686,200  15400      8,551    399    330,040 W_Wing
 *     68,097,000  15113      4,505    354    192,364 Skyscraper
 *     21,680,200  14925      1,452     51    425,101 EmptyRectangle
 *     62,210,800  14874      4,182    223    278,972 Swordfish
 *    469,927,900  14813     31,724    345  1,362,109 Coloring
 *  1,605,280,400  14546    110,358   1067  1,504,480 XColoring
 *  1,971,736,000  14210    138,756 133335     14,787 GEM
 *    150,166,500  12735     11,791     95  1,580,700 NakedQuad
 *    124,959,100  12718      9,825     11 11,359,918 HiddenQuad
 *  1,759,672,000  12717    138,371   1027  1,713,409 URT
 *  2,694,326,800  12002    224,489   2105  1,279,965 BigWings
 *  1,213,667,100  10826    112,106    347  3,497,599 FinnedSwampfish
 *  2,461,942,600  10527    233,869    318  7,741,957 FinnedSwordfish
 *  9,039,089,400  10280    879,288   2535  3,565,715 DeathBlossom
 *  9,158,551,800   8137  1,125,544   2086  4,390,485 ALS_XZ
 *  8,488,292,400   6818  1,244,982   2771  3,063,259 ALS_Wing
 * 32,486,048,500   4623  7,027,049   1665 19,511,140 ALS_Chain
 *  8,117,502,200   3353  2,420,966    630 12,884,924 UnaryChain
 *  5,928,389,800   3062  1,936,116   3523  1,682,767 MultipleChain
 *  4,118,204,000   1528  2,695,159   5449    755,772 DynamicChain
 *    131,898,200      3 43,966,066     30  4,396,606 DynamicPlus
 * 90,977,030,400
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  109,812,271,700 (01:49)  74,957,182
 * NOTES:
 * 1. 1:49 is a whole second faster than previous. Gee wow! We're still WELL up
 *    on the best-ever valid run at 1:30, but anything under about 3 minutes
 *    feels snappy enough in the GUI, and my speed-o-philia is waining.
 * 2. DiufSudoku_V6_30.169.2021-10-13.7z slower but releasable.
 * 3. Next I don't know. I'll find something. AlsChains are starting to REALLY
 *    shit me, so I reckon leave it for a month or two, then come back to it.
 *    It's amazing how much you don't see when you've been staring straight at
 *    it for six weeks. Male pattern blindness. @STRETCH for faster AlsChain!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.170 2021-10-17 11:01:50</b> "nieve" AlsChain is about 10% slower than
 * "fast" but is maintainable; so LENGTH=6, TIMEOUT=500 elims 1776 in 03:06,
 * and top1465 in about 3 mins still feels "pretty snappy" in the GUI, so OK.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      26,116,500 105794        246  56228        464 NakedSingle
 *      19,298,500  49566        389  15996      1,206 HiddenSingle
 *      78,615,800  33570      2,341  24145      3,255 Locking
 *      86,522,400  22255      3,887   7899     10,953 NakedPair
 *      77,139,200  20204      3,818   8238      9,363 HiddenPair
 *     129,187,500  18150      7,117   1596     80,944 NakedTriple
 *     116,145,800  17759      6,540    981    118,395 HiddenTriple
 *      52,402,800  17567      2,983    608     86,188 Swampfish
 *      55,579,600  17334      3,206   1125     49,404 TwoStringKite
 *      85,261,100  16209      5,260    595    143,295 XY_Wing
 *      75,483,700  15803      4,776    290    260,288 XYZ_Wing
 *     146,561,100  15530      9,437    389    376,763 W_Wing
 *      70,383,500  15249      4,615    361    194,968 Skyscraper
 *      22,779,900  15059      1,512     48    474,581 EmptyRectangle
 *      62,272,800  15011      4,148    230    270,751 Swordfish
 *     467,231,400  14946     31,261    351  1,331,143 Coloring
 *   1,539,364,700  14678    104,875    971  1,585,339 XColoring
 *   1,998,283,200  14357    139,185 135767     14,718 GEM
 *     154,431,300  12867     12,002     90  1,715,903 NakedQuad
 *     128,613,100  12851     10,008     11 11,692,100 HiddenQuad
 *   1,759,250,900  12850    136,906   1036  1,698,118 URT
 *   2,843,975,700  12132    234,419   2128  1,336,454 BigWings
 *   1,232,914,600  10936    112,739    342  3,605,013 FinnedSwampfish
 *   2,547,489,700  10639    239,448    310  8,217,708 FinnedSwordfish
 *   9,351,343,600  10397    899,427   2592  3,607,771 DeathBlossom
 *   9,374,037,500   8212  1,141,504   2102  4,459,580 ALS_XZ
 *   9,859,613,000   6883  1,432,458   2838  3,474,141 ALS_Wing
 * 102,076,601,600   4589 22,243,757   1776 57,475,563 ALS_Chain
 *   7,866,195,200   3277  2,400,425    610 12,895,401 UnaryChain
 *   4,558,846,100   2997  1,521,136    206 22,130,320 NishioChain
 *   5,483,142,800   2800  1,958,265   3320  1,651,549 MultipleChain
 *   3,950,331,900   1415  2,791,753   5093    775,639 DynamicChain
 *     146,402,100      3 48,800,700     30  4,880,070 DynamicPlus
 * 166,441,818,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  186,242,933,600 (03:06)  127,128,282
 * NOTES:
 * 1. 03:06 is 01:36 SLOWER than fastest, but yields a more accurate assessment
 *    of the difficult of each puzzle. About three mins for top1465 still feels
 *    "pretty snappy" in the GUI. IMHO AlsChain was the best hinter to invest
 *    the available 90 seconds, which I've just done. It's worth noting that
 *    accuracy is a compromise with performance. If you have days to wait then
 *    you can reduce the difficulty of almost all puzzles by overdoing simpler
 *    hinters, like hobiwan did with his AlsChain MAX_LENGHT=32.
 * 2. DiufSudoku_V6_30.170.2021-10-17.7z MUCH slower, but still releasable.
 * 3. Next I don't know. I tap-out on AlsChain. It officially just s__ts me.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.171 2021-10-18 19:02:51</b> in-lined everything in AlsChain again, for
 * speed, and it's about 20% faster than the nieve version.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      18,200,200 105780        172  56250        323 NakedSingle
 *      18,448,900  49530        372  16031      1,150 HiddenSingle
 *      77,611,500  33499      2,316  24132      3,216 Locking
 *      82,623,900  22193      3,722   7886     10,477 NakedPair
 *      73,001,400  20147      3,623   8208      8,893 HiddenPair
 *     120,367,200  18099      6,650   1601     75,182 NakedTriple
 *     110,326,300  17704      6,231    977    112,923 HiddenTriple
 *      51,182,100  17514      2,922    604     84,738 Swampfish
 *      53,884,600  17284      3,117   1138     47,350 TwoStringKite
 *      78,631,900  16146      4,870    601    130,835 XY_Wing
 *      68,308,300  15734      4,341    291    234,736 XYZ_Wing
 *     131,029,600  15460      8,475    403    325,135 W_Wing
 *      66,306,700  15169      4,371    362    183,167 Skyscraper
 *      21,294,700  14980      1,421     48    443,639 EmptyRectangle
 *      60,278,900  14932      4,036    230    262,082 Swordfish
 *     455,865,200  14868     30,660    340  1,340,780 Coloring
 *   1,450,579,500  14610     99,286    932  1,556,415 XColoring
 *   1,917,139,300  14296    134,103 135248     14,174 GEM
 *     144,680,100  12817     11,288     93  1,555,700 NakedQuad
 *     121,327,500  12801      9,477     11 11,029,772 HiddenQuad
 *   1,728,936,100  12800    135,073   1032  1,675,325 URT
 *   2,703,107,900  12085    223,674   2105  1,284,136 BigWings
 *   1,183,583,400  10910    108,486    343  3,450,680 FinnedSwampfish
 *   2,439,268,300  10613    229,837    312  7,818,167 FinnedSwordfish
 *   8,890,776,500  10370    857,355   2572  3,456,756 DeathBlossom
 *   9,059,357,400   8201  1,104,664   2103  4,307,825 ALS_XZ
 *   8,279,797,300   6874  1,204,509   2847  2,908,253 ALS_Wing
 * 108,861,565,500   4623 23,547,818   1764 61,712,905 ALS_Chain
 *   7,538,720,200   3263  2,310,364    592 12,734,324 UnaryChain
 *   4,349,728,200   2992  1,453,786    206 21,115,185 NishioChain
 *   5,138,382,900   2795  1,838,419   3306  1,554,259 MultipleChain
 *   3,819,679,600   1415  2,699,420   5093    749,986 DynamicChain
 *     150,531,000      3 50,177,000     30  5,017,700 DynamicPlus
 * 169,264,522,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  188,120,236,900 (03:08)  128,409,718
 * NOTES:
 * 1. 03:08 ain't great but ____ it, she'll do.
 * 2. DiufSudoku_V6_30.171.2021-10-18.7z slower, but still releasable.
 * 3. Next I don't know. I really tap-out on AlsChain this time.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.172 2021-10-19 09:37:52</b> Still playing with AlsChain. Dropped LENGTH
 * to 6, reinstated length loop and moved TIMEOUT=125 into i loop. This is both
 * faster and finds more elims.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      18,474,300 105735        174  56190        328 NakedSingle
 *      19,285,200  49545        389  15989      1,206 HiddenSingle
 *      79,179,200  33556      2,359  24144      3,279 Locking
 *      83,782,600  22241      3,767   7900     10,605 NakedPair
 *      72,284,400  20190      3,580   8242      8,770 HiddenPair
 *     121,325,600  18137      6,689   1596     76,018 NakedTriple
 *     112,662,800  17746      6,348    984    114,494 HiddenTriple
 *      51,610,100  17553      2,940    608     84,885 Swampfish
 *      56,454,100  17320      3,259   1126     50,136 TwoStringKite
 *      82,603,000  16194      5,100    590    140,005 XY_Wing
 *      70,880,400  15791      4,488    289    245,260 XYZ_Wing
 *     134,754,100  15519      8,683    389    346,411 W_Wing
 *      69,045,900  15238      4,531    361    191,262 Skyscraper
 *      21,274,000  15048      1,413     48    443,208 EmptyRectangle
 *      61,938,000  15000      4,129    230    269,295 Swordfish
 *     487,561,600  14935     32,645    351  1,389,064 Coloring
 *   1,487,570,100  14667    101,422    971  1,531,998 XColoring
 *   1,959,592,000  14346    136,595 136231     14,384 GEM
 *     147,865,600  12855     11,502     90  1,642,951 NakedQuad
 *     122,019,300  12839      9,503     11 11,092,663 HiddenQuad
 *   1,680,291,700  12838    130,884   1036  1,621,903 URT
 *   2,657,203,400  12120    219,241   2124  1,251,037 BigWings
 *   1,225,575,200  10926    112,170    342  3,583,553 FinnedSwampfish
 *   2,484,200,300  10629    233,719    310  8,013,549 FinnedSwordfish
 *   9,085,047,100  10387    874,655   2587  3,511,807 DeathBlossom
 *   9,152,252,500   8206  1,115,312   2103  4,351,998 ALS_XZ
 *   8,533,980,900   6876  1,241,125   2833  3,012,347 ALS_Wing
 *  75,141,770,400   4634 16,215,315   1782 42,167,098 ALS_Chain
 *   7,661,717,800   3266  2,345,902    594 12,898,514 UnaryChain
 *   4,503,175,000   2993  1,504,568    206 21,860,072 NishioChain
 *   5,263,391,900   2796  1,882,472   3307  1,591,591 MultipleChain
 *   3,755,256,400   1415  2,653,891   5093    737,336 DynamicChain
 *     173,915,200      3 57,971,733     30  5,797,173 DynamicPlus
 * 136,577,940,100
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  154,394,397,700 (02:34)  105,388,667
 * NOTES:
 * 1. 02:34 is 30 seconds faster than previous with more ALS_Chain elims. But
 *    we're still over a minute slower than the fastest ever, which found 1090
 *    ALS_Chain elims, so if you need speed over accuracy unwant ALS_Chain.
 *    If you want faster AlsChain keep halving TIMEOUT and/or set LENGTH = 5.
 * 2. DiufSudoku_V6_30.172.2021-10-19.7z faster, but still slow = Releasable.
 * 3. Next I don't know. I really really tap-out on AlsChain this time. sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.173 2021-10-20 10:21:53</b> Updated AlsWing and AlsXz with learnings
 * from AlsChain. They're both just a little faster, and cleaner, IMHO.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      17,893,100 105735        169  56190        318 NakedSingle
 *      19,362,300  49545        390  15989      1,210 HiddenSingle
 *      77,540,200  33556      2,310  24144      3,211 Locking
 *      83,384,200  22241      3,749   7900     10,554 NakedPair
 *      74,388,500  20190      3,684   8242      9,025 HiddenPair
 *     123,255,900  18137      6,795   1596     77,228 NakedTriple
 *     112,363,400  17746      6,331    984    114,190 HiddenTriple
 *      49,422,400  17553      2,815    608     81,286 Swampfish
 *      56,211,900  17320      3,245   1126     49,921 TwoStringKite
 *      84,825,100  16194      5,238    590    143,771 XY_Wing
 *      72,612,100  15791      4,598    289    251,252 XYZ_Wing
 *     137,318,500  15519      8,848    389    353,003 W_Wing
 *      63,903,500  15238      4,193    361    177,018 Skyscraper
 *      23,230,600  15048      1,543     48    483,970 EmptyRectangle
 *      60,498,700  15000      4,033    230    263,037 Swordfish
 *     468,464,400  14935     31,366    351  1,334,656 Coloring
 *   1,537,582,400  14667    104,832    971  1,583,504 XColoring
 *   1,977,090,100  14346    137,814 136231     14,512 GEM
 *     150,465,200  12855     11,704     90  1,671,835 NakedQuad
 *     123,728,000  12839      9,636     11 11,248,000 HiddenQuad
 *   1,703,977,000  12838    132,729   1036  1,644,765 URT
 *   2,713,241,800  12120    223,864   2124  1,277,420 BigWings
 *   1,215,551,300  10926    111,253    342  3,554,243 FinnedSwampfish
 *   2,486,891,200  10629    233,972    310  8,022,229 FinnedSwordfish
 *   9,166,757,700  10387    882,522   2587  3,543,393 DeathBlossom
 *   9,293,705,200   8206  1,132,549   2103  4,419,260 ALS_XZ
 *   9,327,200,300   6876  1,356,486   2833  3,292,340 ALS_Wing
 *  73,382,025,400   4634 15,835,568   1782 41,179,587 ALS_Chain
 *   7,582,535,300   3266  2,321,658    594 12,765,210 UnaryChain
 *   4,478,525,600   2993  1,496,333    206 21,740,415 NishioChain
 *   5,229,948,400   2796  1,870,510   3307  1,581,478 MultipleChain
 *   3,850,312,000   1415  2,721,068   5093    756,000 DynamicChain
 *     153,983,600      3 51,327,866     30  5,132,786 DynamicPlus
 * 135,898,195,300
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   155,054,722,600 (02:35)       105,839,401
 * NOTES:
 * 1. 02:35 is a second slower than previous. No worries. More of a worry is
 *    that both AlsXz and AlsWing are slower, and I just enspeedonated them,
 *    but the JIT compiler is muercurial, so I'm hoping they'll bed-in nicely.
 * 2. DiufSudoku_V6_30.173.2021-10-20.7z faster, but still slow = Releasable.
 * 3. Next I don't know. I need to look for something else to play with for a
 *    while, because over-familiarity breeds over-contempt, but AlsChain is
 *    still the slowest hinter that I still use, and it can still be faster.
 *    AlsChain's recalcitrance is a challenge, which I accept, just not now.
 *    I might trying sticking a cache in the latest version. Memory efficiency
 *    would be nice, to avoid bogging the other hinters.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.174 2021-10-20 14:29:54</b> AlsChain.TIMEOUT from 125 to 16, which
 * costs 23 elims but saves 34 seconds.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,766,400 105677        168  56143        316 NakedSingle
 *     23,559,900  49534        475  15986      1,473 HiddenSingle
 *     76,017,800  33548      2,265  24143      3,148 Locking
 *     82,345,000  22234      3,703   7898     10,426 NakedPair
 *     72,196,100  20184      3,576   8239      8,762 HiddenPair
 *    119,561,200  18134      6,593   1597     74,866 NakedTriple
 *    108,998,500  17743      6,143    984    110,770 HiddenTriple
 *     47,109,800  17550      2,684    608     77,483 Swampfish
 *     53,457,900  17317      3,087   1125     47,518 TwoStringKite
 *     79,811,700  16192      4,929    589    135,503 XY_Wing
 *     69,395,900  15790      4,394    290    239,296 XYZ_Wing
 *    140,773,800  15517      9,072    392    359,116 W_Wing
 *     60,601,000  15235      3,977    361    167,869 Skyscraper
 *     21,772,900  15045      1,447     48    453,602 EmptyRectangle
 *     58,145,100  14997      3,877    229    253,908 Swordfish
 *    455,652,500  14933     30,513    352  1,294,467 Coloring
 *  1,478,015,700  14664    100,792    971  1,522,158 XColoring
 *  1,915,677,500  14343    133,561 136701     14,013 GEM
 *    146,121,500  12850     11,371     90  1,623,572 NakedQuad
 *    119,482,000  12834      9,309     11 10,862,000 HiddenQuad
 *  1,778,828,100  12833    138,613   1036  1,717,015 URT
 *  2,704,102,600  12115    223,202   2125  1,272,518 BigWings
 *  1,195,523,900  10920    109,480    342  3,495,683 FinnedSwampfish
 *  2,447,040,200  10623    230,353    310  7,893,678 FinnedSwordfish
 *  9,044,361,400  10381    871,241   2585  3,498,785 DeathBlossom
 *  9,122,861,100   8201  1,112,408   2103  4,338,022 ALS_XZ
 *  8,278,842,600   6872  1,204,720   2830  2,925,386 ALS_Wing
 * 38,576,840,100   4633  8,326,535   1759 21,931,120 ALS_Chain
 *  7,837,004,600   3287  2,384,242    598 13,105,358 UnaryChain
 *  4,462,289,300   3010  1,482,488    206 21,661,598 NishioChain
 *  5,307,996,000   2813  1,886,952   3351  1,584,003 MultipleChain
 *  3,812,040,700   1415  2,694,021   5093    748,486 DynamicChain
 *    128,676,300      3 42,892,100     30  4,289,210 DynamicPlus
 * 99,842,869,100
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  118,648,012,400 (01:58)  80,988,404
 * NOTES:
 * 1. 01:58 is 37 seconds faster than previous, and two minutes feels snappy in
 *    the GUI, and three minutes is OK, so it's all good.
 * 2. DiufSudoku_V6_30.174.2021-10-20.7z RELEASE THIS!
 * 3. Next I don't know. I shall see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.175 2021-10-21 09:30:55</b> Killed Idx.pots method coz mutating a param
 * is poor form: mutate THIS, not that, ergo Pots.upsertAll. My bad. Soz.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,129,400 105578        171  56090        323 NakedSingle
 *     18,820,600  49488        380  15983      1,177 HiddenSingle
 *     75,553,800  33505      2,255  24133      3,130 Locking
 *     82,150,200  22198      3,700   7894     10,406 NakedPair
 *     72,932,600  20150      3,619   8239      8,852 HiddenPair
 *    118,482,700  18100      6,546   1591     74,470 NakedTriple
 *    111,299,500  17712      6,283    984    113,109 HiddenTriple
 *     48,174,000  17519      2,749    607     79,364 Swampfish
 *     53,183,200  17287      3,076   1126     47,231 TwoStringKite
 *     86,760,500  16161      5,368    589    147,301 XY_Wing
 *     68,216,000  15759      4,328    289    236,041 XYZ_Wing
 *    127,311,800  15487      8,220    392    324,775 W_Wing
 *     59,658,000  15205      3,923    361    165,257 Skyscraper
 *     21,155,000  15015      1,408     48    440,729 EmptyRectangle
 *     59,423,900  14967      3,970    229    259,493 Swordfish
 *    443,731,800  14903     29,774    350  1,267,805 Coloring
 *  1,458,200,300  14636     99,631    971  1,501,751 XColoring
 *  1,928,320,000  14315    134,706 136699     14,106 GEM
 *    145,709,500  12824     11,362     90  1,618,994 NakedQuad
 *    122,346,600  12808      9,552     11 11,122,418 HiddenQuad
 *  1,645,985,800  12807    128,522   1036  1,588,789 URT
 *  2,778,794,000  12089    229,861   2122  1,309,516 BigWings
 *  1,189,267,700  10896    109,147    342  3,477,390 FinnedSwampfish
 *  2,440,253,300  10599    230,234    310  7,871,784 FinnedSwordfish
 *  9,015,062,100  10357    870,431   2580  3,494,210 DeathBlossom
 *  9,369,024,700   8179  1,145,497   2102  4,457,195 ALS_XZ
 *  9,031,292,000   6851  1,318,244   2824  3,198,049 ALS_Wing
 * 38,005,175,700   4618  8,229,791   1761 21,581,587 ALS_Chain
 *  7,591,787,900   3269  2,322,357    605 12,548,409 UnaryChain
 *  4,383,416,900   2994  1,464,067    206 21,278,722 NishioChain
 *  5,189,153,100   2797  1,855,256   3348  1,549,926 MultipleChain
 *  3,802,455,400   1402  2,712,165   5629    675,511 DynamicChain
 *    123,543,000      3 41,181,000     30  4,118,100 DynamicPlus
 * 99,684,771,000
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  117,942,536,400 (01:57)  80,506,850
 * NOTES:
 * 1. 01:57 is a second faster than previous, from fixing a basic mistake.
 * 2. DiufSudoku_V6_30.175.2021-10-21.7z is releasable
 * 3. Next I don't know. I shall see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.176 2021-10-22 18:11:56</b> Reduced AlsChain.TIMEOUT to 8 ms for speed.
 * I tried making RccFinder find only Rccs with a second common value. Faster
 * (~30% time) but finds ~60% hints, so @stretch "hacked" AlsChain. Reverted to
 * the evidently more correct version.
 * <pre>
 * WARN: this log is from the hacked version, but I reverted it out.
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,561,057 105851        165  56371        311 NakedSingle
 *     18,463,712  49480        373  16008      1,153 HiddenSingle
 *     77,056,017  33472      2,302  24184      3,186 Locking
 *     83,584,789  22146      3,774   7927     10,544 NakedPair
 *     72,368,125  20097      3,600   8235      8,787 HiddenPair
 *    121,364,921  18057      6,721   1602     75,758 NakedTriple
 *    111,165,205  17666      6,292    991    112,174 HiddenTriple
 *     48,992,820  17472      2,804    593     82,618 Swampfish
 *     54,435,383  17246      3,156   1125     48,387 TwoStringKite
 *     84,596,440  16121      5,247    600    140,994 XY_Wing
 *     73,468,379  15714      4,675    285    257,783 XYZ_Wing
 *    138,039,428  15446      8,936    389    354,857 W_Wing
 *     64,074,582  15167      4,224    356    179,984 Skyscraper
 *     22,065,759  14980      1,473     50    441,315 EmptyRectangle
 *     58,484,588  14930      3,917    224    261,091 Swordfish
 *    471,511,888  14870     31,708    352  1,339,522 Coloring
 *  1,541,745,651  14600    105,599    959  1,607,659 XColoring
 *  1,927,138,320  14283    134,925 133574     14,427 GEM
 *    148,089,521  12788     11,580     90  1,645,439 NakedQuad
 *    119,719,326  12772      9,373     11 10,883,575 HiddenQuad
 *  1,652,864,139  12771    129,423   1037  1,593,890 URT
 *  2,718,650,078  12047    225,670   2159  1,259,217 BigWings
 *  1,212,568,326  10833    111,932    341  3,555,918 FinnedSwampfish
 *  2,509,924,835  10537    238,201    306  8,202,368 FinnedSwordfish
 *  9,277,471,719  10297    900,987   2561  3,622,597 DeathBlossom
 *  9,165,675,944   8139  1,126,142   2094  4,377,113 ALS_XZ
 *  6,725,623,588   6824    985,583   2824  2,381,594 ALS_Wing
 * 13,068,090,387   4597  2,842,743   1290 10,130,302 ALS_Chain
 *  8,714,331,316   3600  2,420,647    821 10,614,289 UnaryChain
 *  5,004,829,421   3244  1,542,795    227 22,047,706 NishioChain
 *  5,700,580,410   3035  1,878,280   3832  1,487,625 MultipleChain
 *  4,020,723,847   1453  2,767,187   5284    760,924 DynamicChain
 *    133,160,898      3 44,386,966     30  4,438,696 DynamicPlus
 * 75,158,420,819
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  93,933,208,600 (01:33)  64,118,231
 * NOTES:
 * 1. 01:33 is 24 seconds faster than previous, but we're only finding ~60% of
 *    the AlsChains. Impatience costs inaccuracy.
 * 2. DiufSudoku_V6_30.176.2021-10-22.7z is releasable
 * 3. Next I don't know. I shall see.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.177 2021-10-25 11:08:57</b> Still playin with AlsChain. I hit a problem
 * with the non-determinism of the TIMEOUT approach so I've ripped-out da grow-
 * length search and dropped LENGTH back to 4, but da code that grows-length is
 * still there, just commented out. It finds less AlsChains, in about the same
 * time, but is deterministic; which I now feel trumps all other concerns, even
 * correctness. I'd rather be consistently wrong than occassionaly right.
 * So that worked out rather well. Sighing lols.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,022,115 105750        170  56303        320 NakedSingle
 *     18,761,324  49447        379  16012      1,171 HiddenSingle
 *     77,761,638  33435      2,325  24167      3,217 Locking
 *     82,547,313  22118      3,732   7922     10,420 NakedPair
 *     71,472,597  20072      3,560   8233      8,681 HiddenPair
 *    119,974,317  18032      6,653   1599     75,030 NakedTriple
 *    109,777,377  17641      6,222    990    110,886 HiddenTriple
 *     47,611,141  17447      2,728    597     79,750 Swampfish
 *     54,652,957  17219      3,173   1122     48,710 TwoStringKite
 *     83,950,961  16097      5,215    600    139,918 XY_Wing
 *     73,844,171  15689      4,706    285    259,102 XYZ_Wing
 *    135,136,325  15421      8,763    390    346,503 W_Wing
 *     66,878,189  15141      4,417    355    188,389 Skyscraper
 *     20,943,136  14955      1,400     50    418,862 EmptyRectangle
 *     59,296,250  14905      3,978    227    261,216 Swordfish
 *    470,499,697  14844     31,696    352  1,336,646 Coloring
 *  1,526,925,279  14574    104,770    958  1,593,867 XColoring
 *  1,951,798,061  14258    136,891 134289     14,534 GEM
 *    148,379,104  12772     11,617     90  1,648,656 NakedQuad
 *    121,044,946  12756      9,489     11 11,004,086 HiddenQuad
 *  1,720,257,614  12755    134,869   1035  1,662,084 URT
 *  2,711,235,735  12033    225,316   2158  1,256,365 BigWings
 *  1,214,432,752  10821    112,229    341  3,561,386 FinnedSwampfish
 *  2,469,360,026  10525    234,618    306  8,069,804 FinnedSwordfish
 *  9,088,545,402  10285    883,669   2553  3,559,947 DeathBlossom
 *  9,074,152,168   8132  1,115,857   2087  4,347,940 ALS_XZ
 *  6,774,422,960   6824    992,734   2827  2,396,329 ALS_Wing
 * 14,359,470,871   4597  3,123,661   1302 11,028,779 ALS_Chain
 *  8,748,245,136   3587  2,438,875    784 11,158,475 UnaryChain
 *  4,931,305,071   3241  1,521,538    227 21,723,810 NishioChain
 *  5,799,354,402   3032  1,912,715   3782  1,533,409 MultipleChain
 *  4,029,657,833   1455  2,769,524   5286    762,326 DynamicChain
 *    133,813,601      3 44,604,533     30  4,460,453 DynamicPlus
 * 76,313,530,469
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  95,537,317,701 (01:35)  65,213,186
 * NOTES:
 * 1. 01:35 is a couple of seconds slower, and we find minimal AlsChains.
 *    Impatience costs inaccuracy. I am impatient: maybe too impatient?
 * 2. DiufSudoku_V6_30.177.2021-10-25.7z is releasable
 * 3. Next I don't know. I shall see. I need to stop playing with AlsChain but
 *    it's still the slowest hinter that I still use, so it's still the s__t
 *    that most needs attention, but I really don't know what else to try.
 *    I found a @stretch in RccFinderAllAllowOverlaps but it's a big one. I've
 *    no idea if its implementable, or how to work out how to work that out.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.178 2021-10-26 13:34:58</b> Separate Techs for ALS_Chain_4..ALS_Chain_7
 * each of which finds all 4..degree chains, so you select 1 of them at a time,
 * coz it's faster. RCC_CACHE is back for speed. TechSelectDialog selects one
 * ALS_Chain_* at a time.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      19,112,494 105766        180  56215        339 NakedSingle
 *      18,414,779  49551        371  16039      1,148 HiddenSingle
 *      78,258,065  33512      2,335  24136      3,242 Locking
 *      82,666,071  22204      3,723   7887     10,481 NakedPair
 *      72,207,068  20156      3,582   8200      8,805 HiddenPair
 *     120,166,620  18111      6,635   1602     75,010 NakedTriple
 *     109,187,906  17716      6,163    976    111,872 HiddenTriple
 *      46,937,497  17527      2,678    604     77,711 Swampfish
 *      53,781,565  17297      3,109   1139     47,218 TwoStringKite
 *      81,570,388  16158      5,048    603    135,274 XY_Wing
 *      70,877,784  15745      4,501    293    241,903 XYZ_Wing
 *     134,252,928  15469      8,678    404    332,309 W_Wing
 *      58,612,273  15177      3,861    362    161,912 Skyscraper
 *      22,657,414  14988      1,511     48    472,029 EmptyRectangle
 *      59,011,708  14940      3,949    230    256,572 Swordfish
 *     452,945,332  14876     30,448    340  1,332,192 Coloring
 *   1,514,250,743  14618    103,588    923  1,640,575 XColoring
 *   1,918,551,817  14305    134,117 135450     14,164 GEM
 *     150,276,609  12824     11,718     93  1,615,877 NakedQuad
 *     117,024,514  12808      9,136     11 10,638,592 HiddenQuad
 *   1,751,592,402  12807    136,768   1029  1,702,227 URT
 *   2,736,586,069  12094    226,276   2105  1,300,040 BigWings
 *   1,189,978,542  10921    108,962    344  3,459,239 FinnedSwampfish
 *   2,437,382,686  10623    229,443    312  7,812,123 FinnedSwordfish
 *   8,805,412,938  10380    848,305   2578  3,415,598 DeathBlossom
 *   9,331,899,182   8207  1,137,065   2103  4,437,422 ALS_XZ
 *   6,620,018,519   6880    962,211   2852  2,321,184 ALS_Wing
 *  47,169,430,109   4487 10,512,464   1720 27,424,087 ALS_Chain_6
 *   7,601,046,889   3292  2,308,944    621 12,240,011 UnaryChain
 *   4,373,492,336   3010  1,452,987    206 21,230,545 NishioChain
 *   5,173,467,295   2813  1,839,128   3386  1,527,899 MultipleChain
 *   3,747,280,754   1416  2,646,384   5094    735,626 DynamicChain
 *     166,167,296      3 55,389,098     30  5,538,909 DynamicPlus
 * 106,284,518,592
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  124,828,511,002 (02:04)  85,207,174
 * NOTES:
 * 1. 02:04 is 31 seconds slower, but this one has ALS_Chain_6, and 1720 is a
 *    bit more accurate than the previous lesserer AlsChains run at 1302 elims.
 * 2. DiufSudoku_V6_30.178.2021-10-26.7z is releasable
 * 3. Next I don't know, but I promise to stop fiddling with AlsChain!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.179 2021-10-27 17:56:59</b> AlsChain rccCache has it's own IAS
 * (an int[]'s cache) instead of creating-em on the fly, and tada: ALS_Chain_7
 * has been wrangled into the three minute batch window, for a "snappy" GUI.
 * <p>
 * The hunter, endlessly seeking.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      18,363,661 105841        173  56290        326 NakedSingle
 *      18,194,394  49551        367  16038      1,134 HiddenSingle
 *      78,205,005  33513      2,333  24141      3,239 Locking
 *      83,284,954  22202      3,751   7885     10,562 NakedPair
 *      73,682,140  20147      3,657   8203      8,982 HiddenPair
 *     121,196,385  18102      6,695   1608     75,370 NakedTriple
 *     111,842,472  17708      6,315    975    114,710 HiddenTriple
 *      47,911,734  17520      2,734    604     79,324 Swampfish
 *      53,894,865  17290      3,117   1143     47,152 TwoStringKite
 *      90,409,927  16147      5,599    593    152,461 XY_Wing
 *      73,264,955  15741      4,654    293    250,051 XYZ_Wing
 *     134,213,941  15465      8,678    402    333,865 W_Wing
 *      62,856,703  15175      4,142    362    173,637 Skyscraper
 *      21,552,502  14987      1,438     48    449,010 EmptyRectangle
 *      57,944,111  14939      3,878    229    253,031 Swordfish
 *     458,350,793  14876     30,811    337  1,360,091 Coloring
 *   1,528,253,541  14620    104,531    923  1,655,745 XColoring
 *   1,934,710,411  14307    135,228 134651     14,368 GEM
 *     145,708,257  12825     11,361     93  1,566,755 NakedQuad
 *     123,275,970  12809      9,624     11 11,206,906 HiddenQuad
 *   1,769,608,821  12808    138,164   1033  1,713,077 URT
 *   2,742,339,765  12092    226,789   2116  1,296,001 BigWings
 *   1,214,192,953  10914    111,250    345  3,519,399 FinnedSwampfish
 *   2,458,924,631  10615    231,646    310  7,932,014 FinnedSwordfish
 *   8,896,765,893  10374    857,602   2588  3,437,699 DeathBlossom
 *   9,011,550,661   8190  1,100,311   2102  4,287,131 ALS_XZ
 *   6,498,541,294   6865    946,619   2842  2,286,608 ALS_Wing
 * 102,763,606,524   4390 23,408,566   1697 60,556,043 ALS_Chain_7
 *   7,685,503,912   3310  2,321,904    621 12,376,012 UnaryChain
 *   4,418,066,701   3010  1,467,796    204 21,657,189 NishioChain
 *   5,080,859,675   2815  1,804,923   3423  1,484,329 MultipleChain
 *   3,823,794,021   1417  2,698,513   5104    749,175 DynamicChain
 *     133,398,400      3 44,466,133     30  4,446,613 DynamicPlus
 * 161,734,269,972
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  180,354,536,699 (03:00)  123,108,898
 * NOTES:
 * 1. 03:00 is 56 seconds slower than previous, but this one has ALS_Chain_7
 *    with 1697 elims verses ALS_Chain_6's 1720, so its a bit more accurate,
 *    and as I've said repeatedly the GUI feels OK when batch is three minutes.
 *    But Oh wait, 1697 is less than 1720, so it's NOT "more accurate", so calm
 *    down and think it through. The actual determinant is the number of times
 *    it's called. 4390 is lower than 4487, which means ALS_Chain_7 is finding
 *    more effective hints, just (as you'd expect) less of them. I don't really
 *    understand why, but I really like this version. It could be faster but
 *    then again EVERYTHING could be faster, even an F18. Just ask NASA. The
 *    only thing that couldn't be faster is Voyager, and it's out of my league.
 * 2. DiufSudoku_V6_30.179.2021-10-27.7z needs releasing, tomorrow, I promise.
 * 3. Next I don't know, but I need to stop pulling my AlsChain!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.180 2021-10-28 10:10:10</b> AlsChain has ALS_HAS_ANY_RELATED_RCCS and an
 * alsCeiling, so it's a tad faster. The hunter, is still endlessly seeking.
 * <pre>
 *        time(ns)  calls  time/call  elims  time/elim hinter
 *      15,701,900 105794        148  56254        279 NakedSingle
 *      18,587,500  49540        375  16033      1,159 HiddenSingle
 *      76,666,300  33507      2,288  24137      3,176 Locking
 *      83,442,400  22200      3,758   7882     10,586 NakedPair
 *      95,942,900  20147      4,762   8203     11,696 HiddenPair
 *     121,359,800  18102      6,704   1608     75,472 NakedTriple
 *     111,711,100  17708      6,308    975    114,575 HiddenTriple
 *      47,900,300  17520      2,734    604     79,305 Swampfish
 *      53,554,400  17290      3,097   1141     46,936 TwoStringKite
 *      88,257,600  16149      5,465    591    149,336 XY_Wing
 *      71,355,500  15744      4,532    293    243,534 XYZ_Wing
 *     136,106,700  15468      8,799    401    339,418 W_Wing
 *      62,517,800  15179      4,118    362    172,701 Skyscraper
 *      22,196,700  14991      1,480     48    462,431 EmptyRectangle
 *      58,670,700  14943      3,926    229    256,203 Swordfish
 *     455,013,300  14880     30,578    337  1,350,187 Coloring
 *   1,514,566,300  14624    103,567    923  1,640,916 XColoring
 *   1,918,930,600  14311    134,087 135056     14,208 GEM
 *     150,831,500  12829     11,757     93  1,621,844 NakedQuad
 *     122,842,000  12813      9,587     11 11,167,454 HiddenQuad
 *   1,794,229,600  12812    140,042   1033  1,736,911 URT
 *   2,730,682,100  12096    225,750   2117  1,289,882 BigWings
 *   1,208,023,900  10917    110,655    345  3,501,518 FinnedSwampfish
 *   2,556,416,600  10618    240,762    310  8,246,505 FinnedSwordfish
 *   8,990,663,400  10377    866,402   2589  3,472,639 DeathBlossom
 *   9,178,378,900   8192  1,120,407   2102  4,366,498 ALS_XZ
 *   6,532,452,400   6867    951,281   2842  2,298,540 ALS_Wing
 * 101,998,826,500   4394 23,213,205   1700 59,999,309 ALS_Chain_7
 *   7,780,794,800   3310  2,350,693    621 12,529,460 UnaryChain
 *   4,475,167,900   3010  1,486,766    204 21,937,097 NishioChain
 *   5,235,370,800   2815  1,859,812   3423  1,529,468 MultipleChain
 *   3,917,462,500   1417  2,764,617   5104    767,527 DynamicChain
 *     117,635,900      3 39,211,966     30  3,921,196 DynamicPlus
 * 161,742,260,600
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465   181,176,008,600 (03:01)       123,669,630
 * NOTES:
 * 1. 03:01 is a second slower than previous, but ALS_Chain_7 is 700ms faster,
 *    and that's what I've changed, so accepted. More-RAM-slow! More-RAM-slow!
 * 2. DiufSudoku_V6_30.180.2021-10-28.7z needs releasing, today!
 * 3. Next I don't know, but I still need to stop pulling my AlsChain!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.181 2021-10-30 14:52:01</b>AlsChain DUD_BRANCH prune when branch
 * known to not hint in [alsIndex][prevCand], solving the exponential workload
 * growth problem. I'm a bit proud of this, in fact I'm chuffed.
 * <p>
 * BigWings bug: Grid.getBivalue forgot to clear Idx. Oops! How embarrassment!
 * That shrunk my head back down to size rather nicely. sigh.
 * <pre>
 *       time(ns)  calls  time/call elims  time/elim hinter
 *     18,173,700 105764        171 56179        323 NakedSingle
 *     18,551,200  49585        374 16053      1,155 HiddenSingle
 *     78,393,100  33532      2,337 24124      3,249 Locking
 *     83,115,400  22229      3,739  7871     10,559 NakedPair
 *     72,698,400  20182      3,602  8212      8,852 HiddenPair
 *    120,103,800  18131      6,624  1601     75,017 NakedTriple
 *    108,866,600  17737      6,137   977    111,429 HiddenTriple
 *     47,650,600  17547      2,715   604     78,891 Swampfish
 *     54,497,100  17317      3,147  1138     47,888 TwoStringKite
 *     80,046,300  16179      4,947   597    134,080 XY_Wing
 *     71,230,500  15771      4,516   290    245,622 XYZ_Wing
 *    136,624,500  15498      8,815   400    341,561 W_Wing
 *     61,976,700  15209      4,075   365    169,799 Skyscraper
 *     21,349,200  15019      1,421    48    444,775 EmptyRectangle
 *     58,373,700  14971      3,899   228    256,025 Swordfish
 *    460,963,800  14908     30,920   338  1,363,798 Coloring
 *  1,523,404,900  14651    103,979   932  1,634,554 XColoring
 *  1,920,514,300  14337    133,955 35750     14,147 GEM
 *    147,787,800  12857     11,494    93  1,589,116 NakedQuad
 *    119,330,500  12841      9,292    11 10,848,227 HiddenQuad
 *  1,653,845,500  12840    128,804  1032  1,602,563 URT
 *  2,768,578,800  12124    228,355  2125  1,302,860 BigWings
 *  1,202,881,200  10936    109,992   344  3,496,747 FinnedSwampfish
 *  2,468,863,600  10638    232,079   308  8,015,790 FinnedSwordfish
 *  9,024,694,800  10398    867,926  2582  3,495,234 DeathBlossom
 *  9,171,335,000   8222  1,115,462  2104  4,358,999 ALS_XZ
 *  6,709,904,700   6895    973,155  2844  2,359,319 ALS_Wing
 * 26,103,046,900   4647  5,617,182  1809 14,429,544 ALS_Chain_10
 *  7,617,988,100   3248  2,345,439   591 12,889,996 UnaryChain
 *  4,445,679,100   2978  1,492,840   205 21,686,239 NishioChain
 *  5,208,352,300   2782  1,872,161  3285  1,585,495 MultipleChain
 *  3,882,793,600   1414  2,745,964  5083    763,878 DynamicChain
 *    125,724,200      3 41,908,066    30  4,190,806 DynamicPlus
 * 85,587,339,900
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  104,685,484,300 (01:44)  71,457,668
 * NOTES:
 * 1. 01:44 is 01:17 faster than previous. Suck on that!
 * 2. DiufSudoku_V6_30.181.2021-10-30.7z needs releasing. Monday, I promise.
 * 3. Next I don't know, but now I CAN stop pulling my AlsChain! Funny how s__t
 *    like AlsChain keeps bothering you until you fix it. I've STILL got it for
 *    AlignedExclusion, and I packed that s__t in years ago.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2021-11-01 16:27:02</b> KrakenFisherman is much faster. sigh.
 * <pre>
 * WAS 529,131,437,000 1467 360,689,459 35 15,118,041,057 KrakenJellyfish
 * NOW 309,076,138,200 1467 210,685,847 35  8,830,746,805 faster caching
 * </pre>
 * <pre>
 *        time(ns)  calls   time/call  elims     time/elim hinter
 *      19,171,400 106095         180  56078           341 NakedSingle
 *      17,730,300  50017         354  16189         1,095 HiddenSingle
 *      76,246,400  33828       2,253  24093         3,164 Locking
 *      81,565,400  22529       3,620   7845        10,397 NakedPair
 *      72,059,100  20483       3,517   8199         8,788 HiddenPair
 *     120,040,100  18428       6,514   1603        74,884 NakedTriple
 *     114,183,700  18032       6,332    977       116,871 HiddenTriple
 *      47,875,100  17845       2,682    601        79,659 Swampfish
 *      53,122,500  17610       3,016   1141        46,557 TwoStringKite
 *      78,647,100  16469       4,775    605       129,995 XY_Wing
 *      68,125,200  16055       4,243    287       237,370 XYZ_Wing
 *     126,866,400  15787       8,036    415       305,702 W_Wing
 *      60,816,400  15493       3,925    356       170,832 Skyscraper
 *      20,816,100  15309       1,359     48       433,668 EmptyRectangle
 *      58,722,300  15261       3,847    219       268,138 Swordfish
 *     466,255,300  15203      30,668    346     1,347,558 Coloring
 *   1,542,290,300  14944     103,204    949     1,625,174 XColoring
 *   1,935,311,200  14638     132,211 138221        14,001 GEM
 *     151,902,200  13119      11,578     89     1,706,766 NakedQuad
 *     120,918,600  13104       9,227     11    10,992,600 HiddenQuad
 *   1,678,638,400  13103     128,110   1025     1,637,696 URT
 *   3,042,330,800  12398     245,388   2139     1,422,314 BigWings
 *   1,312,827,500  11191     117,311    345     3,805,297 FinnedSwampfish
 *   2,828,567,700  10894     259,644    296     9,555,971 FinnedSwordfish
 *   9,306,405,300  10663     872,775   2594     3,587,665 DeathBlossom
 *   9,530,602,900   8479   1,124,024   2122     4,491,330 ALS_XZ
 *   7,214,754,900   7147   1,009,480   2823     2,555,704 ALS_Wing
 *  31,020,145,900   4907   6,321,611   1862    16,659,584 ALS_Chain
 *  17,110,677,000   3468   4,933,874   2366     7,231,900 KrakenSwampfish
 *  51,852,106,400   1628  31,850,188    191   271,476,996 KrakenSwordfish
 * 309,076,138,200   1467 210,685,847     35 8,830,746,805 KrakenJellyfish
 *   3,479,499,000   1439   2,417,997      0             0 UnaryChain
 *   2,155,157,500   1439   1,497,677      1 2,155,157,500 NishioChain
 *   3,344,698,500   1438   2,325,937    390     8,576,150 MultipleChain
 *   3,610,625,500   1237   2,918,856   4393       821,904 DynamicChain
 *     118,657,000      3  39,552,333     30     3,955,233 DynamicPlus
 * 461,914,497,600
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  487,249,948,600 (08:07)  332,593,821
 * NOTES:
 * 1. Took 08:07 but 6.30.132 (last KrakenJellyfish run, below) Took 12:05
 *     28,088,612,800   4096   6,857,571   3111      9,028,805 Kraken Swampfish
 *     82,878,788,400   1676  49,450,351    196    422,850,961 Kraken Swordfish
 *    473,152,644,400   1511 313,138,745     42 11,265,539,152 Kraken Jellyfish
 * 2. DiufSudoku_V6_30.182.2021-11-01.7z needs releasing. Tomorrow, I promise.
 * 3. Next I don't know, but I'll find something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.183 2021-11-02 20:25:03</b> eradicating Collections in favour of
 * arrays, especially the bases and covers in the hints
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,365,831 105806        164  56202        308 NakedSingle
 *     18,635,382  49604        375  16059      1,160 HiddenSingle
 *     76,023,437  33545      2,266  24129      3,150 Locking
 *     78,396,654  22235      3,525   7870      9,961 NakedPair
 *     72,776,888  20185      3,605   8212      8,862 HiddenPair
 *    119,923,898  18134      6,613   1602     74,858 NakedTriple
 *    112,926,414  17739      6,365    977    115,584 HiddenTriple
 *     46,059,742  17549      2,624    604     76,257 Swampfish
 *     53,532,597  17319      3,090   1139     46,999 TwoStringKite
 *     80,802,998  16180      4,994    601    134,447 XY_Wing
 *     77,497,553  15769      4,914    291    266,314 XYZ_Wing
 *    115,798,918  15495      7,473    402    288,057 W_Wing
 *     51,300,913  15205      3,373    363    141,324 Skyscraper
 *     23,590,235  15016      1,571     48    491,463 EmptyRectangle
 *     57,391,495  14968      3,834    228    251,717 Swordfish
 *    449,496,234  14905     30,157    337  1,333,816 Coloring
 *  1,562,127,770  14649    106,637    932  1,676,102 XColoring
 *  1,934,012,402  14335    134,915 135458     14,277 GEM
 *    157,843,035  12855     12,278     93  1,697,236 NakedQuad
 *    125,536,409  12839      9,777     11 11,412,400 HiddenQuad
 *  1,524,608,975  12838    118,757   1032  1,477,334 URT
 *  2,750,224,677  12122    226,878   2124  1,294,832 BigWings
 *  1,202,067,134  10935    109,928    344  3,494,381 FinnedSwampfish
 *  2,459,681,719  10637    231,238    308  7,985,979 FinnedSwordfish
 *  5,914,174,990  10397    568,834   2573  2,298,552 DeathBlossom
 *  9,220,832,057   8228  1,120,665   2106  4,378,362 ALS_XZ
 *  6,729,629,150   6899    975,449   2847  2,363,761 ALS_Wing
 * 27,126,262,685   4651  5,832,350   1807 15,011,766 ALS_Chain
 *  7,567,230,700   3248  2,329,812    591 12,804,112 UnaryChain
 *  4,393,157,305   2978  1,475,203    205 21,430,035 NishioChain
 *  5,170,484,029   2782  1,858,549   3285  1,573,967 MultipleChain
 *  3,839,361,787   1414  2,715,248   5083    755,333 DynamicChain
 *    140,324,299      3 46,774,766     30  4,677,476 DynamicPlus
 * 83,269,078,312
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  101,772,838,200 (01:41)  69,469,514
 * NOTES:
 * 1. Took 01:41 Which is a whole three seconds faster, for a days work. sigh.
 * 2. DiufSudoku_V6_30.183.2021-11-02.7z needs releasing, asap.
 * 3. Next I don't know, but I'll find something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.184 2021-11-04 12:40:04</b> Remove IAS from AlsChain.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,484,300 105724        165  56214        311 NakedSingle
 *     18,063,000  49510        364  16017      1,127 HiddenSingle
 *     81,971,000  33493      2,447  24137      3,396 Locking
 *     75,464,600  22192      3,400   7832      9,635 NakedPair
 *     71,238,900  20158      3,534   8222      8,664 HiddenPair
 *    119,026,100  18103      6,574   1585     75,095 NakedTriple
 *    108,758,800  17709      6,141    985    110,415 HiddenTriple
 *     47,719,600  17519      2,723    608     78,486 Swampfish
 *     52,969,300  17285      3,064   1150     46,060 TwoStringKite
 *     79,138,500  16135      4,904    584    135,511 XY_Wing
 *     76,057,300  15732      4,834    291    261,365 XYZ_Wing
 *    111,417,100  15459      7,207    403    276,469 W_Wing
 *     53,938,200  15169      3,555    355    151,938 Skyscraper
 *     23,050,100  14982      1,538     50    461,002 EmptyRectangle
 *     58,004,200  14932      3,884    224    258,947 Swordfish
 *    442,833,000  14871     29,778    345  1,283,573 Coloring
 *  1,524,935,800  14608    104,390   1076  1,417,226 XColoring
 *  1,914,618,700  14271    134,161 135453     14,134 GEM
 *    152,238,700  12788     11,904     93  1,636,975 NakedQuad
 *  1,467,756,100  12772    114,919   1022  1,436,160 URT
 *  2,654,336,900  12062    220,057   2105  1,260,967 BigWings
 *  1,201,859,400  10886    110,404    349  3,443,723 FinnedSwampfish
 *  2,449,800,700  10585    231,440    317  7,728,077 FinnedSwordfish
 *  5,840,687,200  10339    564,917   2556  2,285,088 DeathBlossom
 *  9,061,177,800   8186  1,106,911   2085  4,345,888 ALS_XZ
 *  6,724,478,800   6870    978,817   2800  2,401,599 ALS_Wing
 * 27,729,284,500   4652  5,960,723   1791 15,482,570 ALS_Chain
 *  7,681,602,400   3263  2,354,153    594 12,931,990 UnaryChain
 *  5,712,826,600   2990  1,910,644   3379  1,690,685 MultipleChain
 *  3,973,048,600   1519  2,615,568   5386    737,662 DynamicChain
 *    112,190,500      3 37,396,833     30  3,739,683 DynamicPlus
 * 79,637,976,700
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  98,216,566,100 (01:38)  67,042,024
 * NOTES:
 * 1. Took 01:38 Which is another whole three seconds faster, for another two
 *    days work. We're will past the point of dimishing returns here. sigh.
 * 2. DiufSudoku_V6_30.184.2021-11-04.7z needs releasing, asap.
 * 3. Next I don't know, but I'll find something.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.185 2021-11-04 18:37:05</b> HACK: Reset DUD_BRANCH per startAls
 * instead of per startAls/startCand (ie FAR less often) to find 10 less elims
 * in about half of the time.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     19,016,600 105784        179  56301        337 NakedSingle
 *     18,262,300  49483        369  15993      1,141 HiddenSingle
 *     78,102,800  33490      2,332  24136      3,235 Locking
 *     75,713,200  22191      3,411   7832      9,667 NakedPair
 *     70,569,900  20160      3,500   8233      8,571 HiddenPair
 *    118,432,400  18100      6,543   1583     74,815 NakedTriple
 *    108,090,800  17707      6,104    981    110,184 HiddenTriple
 *     51,237,400  17518      2,924    604     84,830 Swampfish
 *     53,239,300  17285      3,080   1148     46,375 TwoStringKite
 *     80,131,100  16137      4,965    581    137,919 XY_Wing
 *     76,426,100  15734      4,857    290    263,538 XYZ_Wing
 *    108,021,300  15462      6,986    406    266,062 W_Wing
 *     50,984,100  15170      3,360    355    143,617 Skyscraper
 *     22,559,300  14982      1,505     50    451,186 EmptyRectangle
 *     59,734,700  14932      4,000    223    267,868 Swordfish
 *    446,696,400  14872     30,036    346  1,291,030 Coloring
 *  1,549,741,700  14608    106,088   1091  1,420,478 XColoring
 *  1,932,290,500  14265    135,456 134811     14,333 GEM
 *    151,468,900  12782     11,850     93  1,628,697 NakedQuad
 *  1,416,898,800  12766    110,990   1025  1,382,340 URT
 *  2,676,902,300  12054    222,075   2110  1,268,674 BigWings
 *  1,199,618,100  10875    110,309    348  3,447,178 FinnedSwampfish
 *  2,463,433,700  10575    232,948    318  7,746,646 FinnedSwordfish
 *  5,814,903,900  10328    563,023   2552  2,278,567 DeathBlossom
 *  9,352,537,300   8178  1,143,621   2081  4,494,251 ALS_XZ
 *  6,734,352,400   6866    980,826   2789  2,414,611 ALS_Wing
 * 15,055,361,200   4657  3,232,845   1781  8,453,319 ALS_Chain
 *  7,751,918,000   3267  2,372,794    594 13,050,367 UnaryChain
 *  5,684,081,900   2994  1,898,490   3392  1,675,731 MultipleChain
 *  4,014,824,200   1519  2,643,070   5386    745,418 DynamicChain
 *    135,893,900      3 45,297,966     30  4,529,796 DynamicPlus
 * 67,371,444,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  85,722,596,700 (01:25)  58,513,717
 * NOTES:
 * 1. 01:25 is thirteen seconds faster than previous, but ALS_Chain is a HACK,
 *    taking half the time to find 10 less eliminations. SOLD!
 * 2. DiufSudoku_V6_30.185.2021-11-04.7z needs releasing, asap.
 * 3. Next I don't know, but I'll find something. I'm done with AlsChain.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.186 2021-11-06 21:11:06</b> The final development build.
 * <p>
 * I ran ALL HINTERS one last time to double-check that it all runs-through, ie
 * all the springs don't come flying out of the box if/when a user wants some
 * piece-of-s__t-hinter that should've been written-off years ago. Yes BUG, I'm
 * looking straight at you. That's it: BUG has to go!
 * <pre>
 * The final ALL HINTERS run:
 *          time(ns)  calls   time/call elims       time/elim hinter
 *        27,855,300 111502         249 60579             459 NakedSingle
 *        19,471,300  50923         382 16506           1,179 HiddenSingle
 *        78,842,000  34417       2,290 24266           3,249 Locking
 *        84,588,200  22986       3,679  7950          10,640 NakedPair
 *        81,045,500  20883       3,880  8264           9,807 HiddenPair
 *       132,792,200  18793       7,066  1624          81,768 NakedTriple
 *       124,222,600  18380       6,758   976         127,277 HiddenTriple
 *        51,492,200  18191       2,830   618          83,320 Swampfish
 *        59,703,800  17945       3,327  1150          51,916 TwoStringKite
 *        85,627,700  16795       5,098   626         136,785 XY_Wing
 *        80,960,000  16363       4,947   301         268,970 XYZ_Wing
 *       121,878,800  16081       7,579   425         286,773 W_Wing
 *        60,878,100  15779       3,858   356         171,005 Skyscraper
 *        27,342,100  15591       1,753    48         569,627 EmptyRectangle
 *        63,766,000  15543       4,102   218         292,504 Swordfish
 *        89,046,300  15486       5,750     7      12,720,900 Jellyfish
 *       487,143,100  15484      31,461   362       1,345,699 Coloring
 *     1,689,206,500  15214     111,029   905       1,866,526 XColoring
 *     1,562,030,400  14908     104,777 31098          50,229 Medusa3D
 *     2,062,934,400  13958     147,795 59299          34,788 GEM
 *       167,516,700  13127      12,761    89       1,882,210 NakedQuad
 *       131,443,000  13112      10,024    11      11,949,363 HiddenQuad
 *       143,430,000  13111      10,939     0               0 NakedPent
 *       116,631,000  13111       8,895     0               0 HiddenPent
 *     1,630,188,000  13111     124,337  1032       1,579,639 URT
 *     3,498,572,400  12401     282,120  2155       1,623,467 BigWings
 *     1,418,385,800  11185     126,811   345       4,111,263 FinnedSwampfish
 *     3,116,376,600  10887     286,247   293      10,636,097 FinnedSwordfish
 *     4,213,428,400  10658     395,330    13     324,109,876 FinnedJellyfish
 *     6,751,152,100  10648     634,030  2597       2,599,596 DeathBlossom
 *    11,028,237,700   8465   1,302,804  2109       5,229,131 ALS_XZ
 *     7,802,989,800   7142   1,092,549  2829       2,758,214 ALS_Wing
 *    17,516,870,500   4894   3,579,254  1857       9,432,886 ALS_Chain
 *       803,824,800   3448     233,127    11      73,074,981 SueDeCoq
 *     2,472,395,700   3446     717,468    22     112,381,622 FrankenSwampfish
 *    13,691,877,100   3424   3,998,795   108     126,776,639 FrankenSwordfish
 *    40,774,136,100   3337  12,218,800    63     647,208,509 FrankenJellyfish
 *    15,859,923,500   3279   4,836,817  2168       7,315,462 KrakenSwampfish
 *     2,559,001,600   1593   1,606,404     0               0 MutantSwampfish
 *    48,191,585,000   1593  30,252,093   159     303,091,729 KrakenSwordfish
 *    41,922,791,400   1462  28,674,959     0               0 MutantSwordfish
 *   287,741,682,500   1462 196,813,736    30   9,591,389,416 KrakenJellyfish
 *   433,180,055,700   1439 301,028,530     1 433,180,055,700 MutantJellyfish
 *        88,361,700   1438      61,447     0               0 AlignedPair
 *       731,022,400   1438     508,360     0               0 AlignedTriple
 *     3,813,659,600   1438   2,652,058     0               0 AlignedQuad
 *    26,270,451,000   1438  18,268,742     2  13,135,225,500 Aligned5Exclusion_1C
 *    81,530,271,600   1436  56,775,955     0               0 Aligned6Exclusion_1C
 *   141,695,936,900   1436  98,674,050     4  35,423,984,225 Aligned7Exclusion_1C
 *     6,543,583,300   1432   4,569,541     1   6,543,583,300 Aligned8Exclusion_2H
 *     8,318,357,200   1431   5,812,967     0               0 Aligned9Exclusion_2H
 *    18,950,704,500   1431  13,242,980     0               0 Aligned10Exclusion_2H
 *     4,382,774,000   1431   3,062,735     0               0 UnaryChain
 *     2,475,277,500   1431   1,729,753     0               0 NishioChain
 *     4,141,774,500   1431   2,894,321   393      10,538,866 MultipleChain
 *     4,176,739,800   1227   3,404,025  4356         958,847 DynamicChain
 *       245,583,800      3  81,861,266    30       8,186,126 DynamicPlus
 * 1,255,087,819,700
 * pzls         total (ns) (mm:ss)    each (ns)
 * 1465  1,309,121,308,800 (21:49)  893,598,163
 * NOTES:
 * 1. 21:49 is OK, I think, for ALL HINTERS. On par with Juillerat original at
 *    17:48, but a "little bit" more thorough. Ergo, I think it's safe to say
 *    that SE is now "much faster" than the Juillerat original. Sigh.
 * 2. Note that BUG is MIA in above log, but I ran it separately, to unsure
 *    that all of the springs don't fly out of the box no matter which hinter/s
 *    the user wants. I have commented-out BUG, in favour of Coloring/GEM coz
 *    BUG is old and tired, and needed a place by the fire, so he got it, in
 *    the end. The core BUG code is still there, just commented out.
 *
 * A "normal" time comparable run:
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,219,500 105141        173  55906        325 NakedSingle
 *     18,751,700  49235        380  16030      1,169 HiddenSingle
 *     75,740,900  33205      2,281  24081      3,145 Locking
 *     74,771,200  21954      3,405   7870      9,500 NakedPair
 *     72,807,700  19916      3,655   8201      8,877 HiddenPair
 *    118,827,500  17864      6,651   1566     75,879 NakedTriple
 *    110,756,700  17480      6,336    971    114,064 HiddenTriple
 *     50,832,200  17293      2,939    646     78,687 Swampfish
 *     53,339,900  17038      3,130   1149     46,422 TwoStringKite
 *     78,170,300  15889      4,919    601    130,067 XY_Wing
 *     70,953,600  15472      4,585    298    238,099 XYZ_Wing
 *    109,919,600  15192      7,235    405    271,406 W_Wing
 *     52,647,900  14898      3,533    350    150,422 Skyscraper
 *     22,668,600  14715      1,540     49    462,624 EmptyRectangle
 *     58,639,500  14666      3,998    227    258,323 Swordfish
 *     80,632,900  14608      5,519     15  5,375,526 Jellyfish
 *    444,958,100  14603     30,470    358  1,242,899 Coloring
 *  1,491,345,000  14327    104,093   1076  1,386,008 XColoring
 *  1,897,096,300  13979    135,710 137003     13,847 GEM
 *    147,694,800  12516     11,800     93  1,588,116 NakedQuad
 *    121,291,200  12500      9,703     11 11,026,472 HiddenQuad
 *  1,485,327,900  12499    118,835   1023  1,451,933 URT
 *  2,629,748,100  11787    223,105   2110  1,246,326 BigWings
 *  1,173,127,400  10603    110,641    350  3,351,792 FinnedSwampfish
 *  2,401,205,200  10301    233,104    308  7,796,120 FinnedSwordfish
 *  5,672,944,600  10063    563,742   2585  2,194,562 DeathBlossom
 *  8,834,681,800   7884  1,120,583   2047  4,315,916 ALS_XZ
 *  6,574,160,100   6595    996,840   2752  2,388,866 ALS_Wing
 * 13,407,398,400   4415  3,036,783   1758  7,626,506 ALS_Chain
 *  7,210,726,300   3046  2,367,277    570 12,650,397 UnaryChain
 *  5,378,771,100   2779  1,935,505   3490  1,541,195 MultipleChain
 *  7,058,811,100   1341  5,263,841   6333  1,114,607 DynamicChain
 *    119,395,400      3 39,798,466     30  3,979,846 DynamicPlus
 * 67,116,362,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  85,755,974,200 (01:25)  58,536,501
 * NOTES:
 * 1. 1:25 is same as previous, but AlsChain is 1.5 secs faster and that's what
 *    I've been working on. The other change is the chainer cache now refreshes
 *    whenever a cell is set, to avert "REALLY funny looking" chaining hints.
 *    There is more to be had: I pulled the pin on fastardised AlsChain, but I
 *    give-up now, so this is as good as it's going to get on my watch.
 * 2. DiufSudoku_V6_30.186.2021-11-06.7z ship today!
 * 3. Next nothing. I give up. Bug fixes only from now on.
 *
 * SE is my Telegraph Road. Mind over matter. Be persistant. I hope you find it
 * useful and edifying. Enjoy!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.187 2021-11-12 09:33:07</b> Bug fixes.
 * <p>
 * I ran ALL HINTERS one last time to double-check that it all runs-through, ie
 * all the springs don't come flying out of the box if/when a user wants some
 * piece-of-s__t-hinter that should've been written-off years ago. Yes BUG, I'm
 * looking straight at you. That's it: BUG has to go!
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,213,200 105141        163  55906        307 NakedSingle
 *     18,510,200  49235        375  16030      1,154 HiddenSingle
 *     73,503,000  33205      2,213  24081      3,052 Locking
 *     71,928,200  21954      3,276   7870      9,139 NakedPair
 *     71,086,400  19916      3,569   8201      8,668 HiddenPair
 *    115,412,200  17864      6,460   1566     73,698 NakedTriple
 *    108,512,300  17480      6,207    971    111,753 HiddenTriple
 *     46,070,000  17293      2,664    646     71,315 Swampfish
 *     53,048,100  17038      3,113   1149     46,168 TwoStringKite
 *     77,120,700  15889      4,853    601    128,320 XY_Wing
 *     70,346,200  15472      4,546    298    236,061 XYZ_Wing
 *    107,799,300  15192      7,095    405    266,171 W_Wing
 *     50,358,900  14898      3,380    350    143,882 Skyscraper
 *     22,105,400  14715      1,502     49    451,130 EmptyRectangle
 *     56,033,500  14666      3,820    227    246,843 Swordfish
 *     77,777,400  14608      5,324     15  5,185,160 Jellyfish
 *    326,140,900  14603     22,333    358    911,008 Coloring
 *    865,554,500  14327     60,414   1076    804,418 XColoring
 *  1,875,126,400  13979    134,138 137003     13,686 GEM
 *    145,387,200  12516     11,616     93  1,563,303 NakedQuad
 *    118,454,800  12500      9,476     11 10,768,618 HiddenQuad
 *  1,468,199,000  12499    117,465   1023  1,435,189 URT
 *  2,575,474,300  11787    218,501   2110  1,220,603 BigWings
 *  1,148,774,700  10603    108,344    350  3,282,213 FinnedSwampfish
 *  2,351,548,400  10301    228,283    308  7,634,897 FinnedSwordfish
 *  5,572,018,900  10063    553,713   2585  2,155,519 DeathBlossom
 *  8,803,552,900   7884  1,116,635   2047  4,300,709 ALS_XZ
 *  6,507,006,300   6595    986,657   2752  2,364,464 ALS_Wing
 * 13,016,909,000   4415  2,948,337   1758  7,404,385 ALS_Chain
 *  7,113,063,200   3046  2,335,214    570 12,479,058 UnaryChain
 *  5,316,552,500   2779  1,913,117   3490  1,523,367 MultipleChain
 *  6,988,168,200   1341  5,211,161   6333  1,103,453 DynamicChain
 *    113,369,700      3 37,789,900     30  3,778,990 DynamicPlus
 * 65,342,125,900
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  84,029,300,800 (01:24)  57,357,884
 * NOTES:
 * 1. 1:24 is about a second faster than previous. Meh.
 * 2. DiufSudoku_V6_30.187.2021-11-12.7z I do not plan to release this one.
 * 3. Next more bug fixes, for a week or two.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.188 2021-11-25 20:15:07</b> Fixing bugs and oversights in
 * NakedSetDirect, Regions.common, XColoring, AlsChain, and WWing.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,151,300 105909        171  56543        321 NakedSingle
 *     18,888,400  49366        382  16117      1,171 HiddenSingle
 *     75,607,200  33249      2,273  24090      3,138 Locking
 *     77,545,300  21993      3,525   7851      9,877 NakedPair
 *     72,495,300  19958      3,632   8210      8,830 HiddenPair
 *    118,691,600  17903      6,629   1563     75,938 NakedTriple
 *    112,876,700  17520      6,442    971    116,247 HiddenTriple
 *     46,834,600  17333      2,702    644     72,724 Swampfish
 *     55,298,600  17078      3,238   1154     47,919 TwoStringKite
 *     77,570,400  15924      4,871    615    126,130 XY_Wing
 *     70,270,500  15498      4,534    299    235,018 XYZ_Wing
 *     90,716,900  15217      5,961    749    121,117 W_Wing
 *     49,589,700  14689      3,375    330    150,271 Skyscraper
 *     22,212,700  14517      1,530     48    462,764 EmptyRectangle
 *     54,584,400  14469      3,772    227    240,459 Swordfish
 *     77,026,800  14411      5,345     25  3,081,072 Jellyfish
 *    329,969,100  14406     22,904    330    999,906 Coloring
 *    867,308,200  14155     61,272   1060    818,215 XColoring
 *  1,864,086,500  13810    134,980 129700     14,372 GEM
 *    145,854,300  12413     11,750     93  1,568,325 NakedQuad
 *    118,154,300  12397      9,530     11 10,741,300 HiddenQuad
 *  1,461,910,600  12396    117,934   1013  1,443,149 URT
 *  2,889,028,000  11690    247,136   2095  1,379,010 BigWings
 *  1,154,353,800  10518    109,750    347  3,326,668 FinnedSwampfish
 *  2,358,946,500  10218    230,861    307  7,683,864 FinnedSwordfish
 *  6,091,556,700   9981    610,315   2546  2,392,598 DeathBlossom
 *  9,181,884,900   7829  1,172,804   2036  4,509,766 ALS_XZ
 *  6,518,115,400   6549    995,284   2720  2,396,365 ALS_Wing
 * 13,197,529,800   4396  3,002,167   1749  7,545,757 ALS_Chain
 *  7,209,142,300   3033  2,376,901    557 12,942,804 UnaryChain
 *  5,446,163,000   2779  1,959,756   3490  1,560,505 MultipleChain
 *  7,102,355,700   1341  5,296,312   6333  1,121,483 DynamicChain
 *    142,842,800      3 47,614,266     30  4,761,426 DynamicPlus
 * 67,117,562,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  85,521,047,100 (01:25)  58,376,141
 * NOTES:
 * 1. 1:25 is a second slower than previous. Meh.
 * 2. DiufSudoku_V6_30.188.2021-11-25.7z I'll release this one. No rush.
 * 3. Next nothing, unless I find something really bad, like WWing missing half
 *    it's onions.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.189 2021-11-28 06:51:08</b> Fix two bugs in EmptyRectangle that
 * missed hints, taking elims from 48 to 588, was 21ms now 85ms, so that is 12
 * times the elims in about 4 times the time. Better.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,361,300 105891        163  56431        307 NakedSingle
 *     18,236,300  49460        368  16142      1,129 HiddenSingle
 *     79,524,100  33318      2,386  24098      3,300 Locking
 *     76,427,000  22056      3,465   7860      9,723 NakedPair
 *     69,947,100  20013      3,495   8221      8,508 HiddenPair
 *    117,631,400  17954      6,551   1576     74,639 NakedTriple
 *    104,443,200  17568      5,945    965    108,231 HiddenTriple
 *     47,627,900  17382      2,740    650     73,273 Swampfish
 *     53,020,600  17126      3,095   1141     46,468 TwoStringKite
 *     77,384,700  15985      4,841    624    124,013 XY_Wing
 *     56,193,700  15556      3,612    303    185,457 XYZ_Wing
 *     84,946,000  15271      5,562    760    111,771 W_Wing
 *     42,453,200  14742      2,879    315    134,772 Skyscraper
 *     85,231,300  14577      5,846    588    144,951 EmptyRectangle
 *     54,876,300  13989      3,922    215    255,238 Swordfish
 *     74,184,500  13934      5,323     23  3,225,413 Jellyfish
 *    310,716,700  13929     22,307    177  1,755,461 Coloring
 *    849,947,200  13814     61,527    788  1,078,613 XColoring
 *  1,821,695,200  13546    134,482 130711     13,936 GEM
 *    144,184,800  12187     11,831     90  1,602,053 NakedQuad
 *    110,812,800  12172      9,103     11 10,073,890 HiddenQuad
 *  1,366,400,400  12171    112,266   1008  1,355,555 URT
 *  2,806,193,200  11471    244,633   2079  1,349,780 BigWings
 *  1,131,847,800  10310    109,781    217  5,215,888 FinnedSwampfish
 *  2,354,278,800  10132    232,360    275  8,561,013 FinnedSwordfish
 *  6,045,113,700   9926    609,018   2547  2,373,425 DeathBlossom
 *  8,877,263,300   7776  1,141,623   1995  4,449,756 ALS_XZ
 *  6,515,689,200   6516    999,952   2710  2,404,313 ALS_Wing
 * 13,208,050,900   4368  3,023,821   1737  7,603,944 ALS_Chain
 *  7,139,058,700   3013  2,369,418    554 12,886,387 UnaryChain
 *  5,468,163,200   2762  1,979,783   3464  1,578,569 MultipleChain
 *  7,114,971,500   1341  5,305,720   6333  1,123,475 DynamicChain
 *    152,185,000      3 50,728,333     30  5,072,833 DynamicPlus
 * 66,476,061,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  84,867,307,000 (01:24)  57,929,902
 * NOTES:
 * 1. 1:24 is a second faster than previous. Meh.
 * 2. DiufSudoku_V6_30.189.2021-11-28.7z I'll release this one. No rush.
 * 3. Next I seek more "my bads" by continueing to desk-check each hinter, but
 *    everything from-here-down is beyond my brain: I can't hold it all in my
 *    head at once, so desk-checking it is likely to be much less productive.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.190 2021-12-01 18:22:10</b> Fix ColoringHint. Check Coloring,
 * XColoring, Medusa3D, and GEM. Check BigWing. Idx leases-out int-arrays.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,958,700 105891        169  56431        318 NakedSingle
 *     17,796,300  49460        359  16142      1,102 HiddenSingle
 *     80,211,800  33318      2,407  24098      3,328 Locking
 *     77,750,200  22056      3,525   7860      9,891 NakedPair
 *     70,182,600  20013      3,506   8221      8,536 HiddenPair
 *    116,604,800  17954      6,494   1576     73,987 NakedTriple
 *    107,763,800  17568      6,134    965    111,672 HiddenTriple
 *     48,338,700  17382      2,780    650     74,367 Swampfish
 *     52,151,400  17126      3,045   1141     45,706 TwoStringKite
 *     79,670,800  15985      4,984    624    127,677 XY_Wing
 *     55,540,500  15556      3,570    303    183,301 XYZ_Wing
 *     85,282,600  15271      5,584    760    112,213 W_Wing
 *     43,007,500  14742      2,917    315    136,531 Skyscraper
 *     85,839,000  14577      5,888    588    145,984 EmptyRectangle
 *     54,588,300  13989      3,902    215    253,899 Swordfish
 *     76,285,400  13934      5,474     23  3,316,756 Jellyfish
 *    293,783,300  13929     21,091    177  1,659,792 Coloring
 *    846,106,600  13814     61,249    788  1,073,739 XColoring
 *  1,871,539,800  13546    138,161 130711     14,318 GEM
 *    141,629,800  12187     11,621     90  1,573,664 NakedQuad
 *    113,591,700  12172      9,332     11 10,326,518 HiddenQuad
 *  1,393,274,400  12171    114,474   1008  1,382,216 URT
 *  2,788,395,400  11471    243,082   2079  1,341,219 BigWings
 *  1,137,648,600  10310    110,344    217  5,242,620 FinnedSwampfish
 *  2,381,760,600  10132    235,073    275  8,660,947 FinnedSwordfish
 *  6,004,766,300   9926    604,953   2547  2,357,583 DeathBlossom
 *  8,562,457,700   7776  1,101,139   1995  4,291,958 ALS_XZ
 *  6,532,446,000   6516  1,002,523   2710  2,410,496 ALS_Wing
 * 12,778,266,200   4368  2,925,427   1737  7,356,514 ALS_Chain
 *  7,108,827,300   3013  2,359,385    554 12,831,818 UnaryChain
 *  5,415,373,700   2762  1,960,671   3464  1,563,329 MultipleChain
 *  6,991,002,500   1341  5,213,275   6333  1,103,900 DynamicChain
 *    141,155,700      3 47,051,900     30  4,705,190 DynamicPlus
 * 65,570,998,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  83,898,302,400 (01:23)  57,268,465
 * NOTES:
 * 1. 1:23 is a second faster than previous. Meh.
 * 2. DiufSudoku_V6_30.190.2021-12-01.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs".
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.191 2021-12-03 10:05:11</b> Eradicated semi-thunked crap from
 * Locking and LockingSpeedMode, making them easier to follow.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,354,000 105891        163  56431        307 NakedSingle
 *     18,346,000  49460        370  16142      1,136 HiddenSingle
 *     78,191,800  33318      2,346  24098      3,244 Locking
 *     78,500,400  22056      3,559   7860      9,987 NakedPair
 *     70,253,300  20013      3,510   8221      8,545 HiddenPair
 *    116,479,700  17954      6,487   1576     73,908 NakedTriple
 *    106,223,700  17568      6,046    965    110,076 HiddenTriple
 *     48,076,400  17382      2,765    650     73,963 Swampfish
 *     53,485,500  17126      3,123   1141     46,875 TwoStringKite
 *     79,863,200  15985      4,996    624    127,985 XY_Wing
 *     59,423,300  15556      3,819    303    196,116 XYZ_Wing
 *     85,214,500  15271      5,580    760    112,124 W_Wing
 *     43,483,400  14742      2,949    315    138,042 Skyscraper
 *     85,770,900  14577      5,883    588    145,868 EmptyRectangle
 *     54,247,800  13989      3,877    215    252,315 Swordfish
 *     74,936,800  13934      5,377     23  3,258,121 Jellyfish
 *    296,480,500  13929     21,285    177  1,675,031 Coloring
 *    863,796,500  13814     62,530    788  1,096,188 XColoring
 *  1,872,592,700  13546    138,239 130711     14,326 GEM
 *    144,186,300  12187     11,831     90  1,602,070 NakedQuad
 *    113,040,400  12172      9,286     11 10,276,400 HiddenQuad
 *  1,385,915,000  12171    113,870   1008  1,374,915 URT
 *  2,767,546,000  11471    241,264   2079  1,331,190 BigWings
 *  1,120,578,100  10310    108,688    217  5,163,954 FinnedSwampfish
 *  2,320,187,300  10132    228,995    275  8,437,044 FinnedSwordfish
 *  5,941,253,800   9926    598,554   2547  2,332,647 DeathBlossom
 *  8,791,676,000   7776  1,130,616   1995  4,406,855 ALS_XZ
 *  6,467,922,900   6516    992,621   2710  2,386,687 ALS_Wing
 * 12,665,666,500   4368  2,899,648   1737  7,291,690 ALS_Chain
 *  7,129,096,100   3013  2,366,112    554 12,868,404 UnaryChain
 *  5,304,108,600   2762  1,920,386   3464  1,531,209 MultipleChain
 *  6,988,971,200   1341  5,211,760   6333  1,103,579 DynamicChain
 *    103,177,600      3 34,392,533     30  3,439,253 DynamicPlus
 * 65,346,046,200
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  84,567,082,700 (01:24)  57,724,971
 * NOTES:
 * 1. 1:24 is a second slower than previous. Meh.
 * 2. DiufSudoku_V6_30.191.2021-12-03.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs", especially unused code. I used to have a
 *    tool that found unused code. I'll update Java versions, and find a tool.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.192 2021-12-03 13:12:12</b> Removed Grid.Cell.hashCode using
 * Grid.Cell.i (indice) instead coz it's a bit shorter, and more significant
 * bits further left is more deterministic, ie reduces hash-collisions.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     19,329,600 105891        182  56431        342 NakedSingle
 *     18,627,300  49460        376  16142      1,153 HiddenSingle
 *     79,730,600  33318      2,393  24098      3,308 Locking
 *     78,569,300  22056      3,562   7860      9,996 NakedPair
 *     71,184,700  20013      3,556   8221      8,658 HiddenPair
 *    120,628,200  17954      6,718   1576     76,540 NakedTriple
 *    109,490,300  17568      6,232    965    113,461 HiddenTriple
 *     49,216,100  17382      2,831    650     75,717 Swampfish
 *     54,638,900  17126      3,190   1141     47,886 TwoStringKite
 *     83,685,100  15985      5,235    624    134,110 XY_Wing
 *     62,437,300  15556      4,013    303    206,063 XYZ_Wing
 *     86,129,400  15271      5,640    760    113,328 W_Wing
 *     48,683,000  14742      3,302    315    154,549 Skyscraper
 *     86,691,700  14577      5,947    588    147,434 EmptyRectangle
 *     55,527,800  13989      3,969    215    258,268 Swordfish
 *     76,938,700  13934      5,521     23  3,345,160 Jellyfish
 *    289,814,400  13929     20,806    177  1,637,369 Coloring
 *    905,623,400  13814     65,558    788  1,149,268 XColoring
 *  1,910,446,500  13546    141,033 130711     14,615 GEM
 *    146,571,400  12187     12,026     90  1,628,571 NakedQuad
 *    114,907,200  12172      9,440     11 10,446,109 HiddenQuad
 *  1,381,972,100  12171    113,546   1008  1,371,004 URT
 *  2,868,242,200  11471    250,042   2079  1,379,625 BigWings
 *  1,140,600,800  10310    110,630    217  5,256,224 FinnedSwampfish
 *  2,373,491,900  10132    234,256    275  8,630,879 FinnedSwordfish
 *  6,010,499,000   9926    605,530   2547  2,359,834 DeathBlossom
 *  8,660,773,600   7776  1,113,782   1995  4,341,239 ALS_XZ
 *  6,488,495,300   6516    995,778   2710  2,394,278 ALS_Wing
 * 12,800,400,500   4368  2,930,494   1737  7,369,257 ALS_Chain
 *  7,154,836,500   3013  2,374,655    554 12,914,867 UnaryChain
 *  5,356,004,400   2762  1,939,176   3464  1,546,190 MultipleChain
 *  7,089,654,100   1341  5,286,841   6333  1,119,477 DynamicChain
 *    152,296,800      3 50,765,600     30  5,076,560 DynamicPlus
 * 65,946,138,100
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  85,650,041,000 (01:25)  58,464,191
 * NOTES:
 * 1. 1:25 is a second slower than previous. Meh. I don't care! Really. Sigh.
 *    Anything under 1:30 is bloody fine with me. Honest. BIG Sigh.
 * 2. DiufSudoku_V6_30.192.2021-12-03.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs", especially unused code. I used to have a
 *    tool that found unused code. I'll update Java versions, and find a tool.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.193</b> It's a bust! So it got the chop.
 * <hr>
 * <p>
 * <b>KRC 6.30.194 2021-12-04 08:00:14</b> Chainers use circular-arrays instead
 * of Queues. They make chainers about 10% faster, but're unmaintainable, so do
 * this at home folks, not in a real system, unless you really want to. Sigh.
 * Also, ChainerUnary and ChainerMulti now have Ctrl-F6 tests-cases.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,515,600 105891        174  56431        328 NakedSingle
 *     18,972,100  49460        383  16142      1,175 HiddenSingle
 *     79,054,300  33318      2,372  24098      3,280 Locking
 *     76,663,900  22056      3,475   7860      9,753 NakedPair
 *     72,607,500  20013      3,628   8221      8,831 HiddenPair
 *    118,186,600  17954      6,582   1576     74,991 NakedTriple
 *    111,694,300  17568      6,357    965    115,745 HiddenTriple
 *     48,037,300  17382      2,763    650     73,903 Swampfish
 *     53,926,700  17126      3,148   1141     47,262 TwoStringKite
 *     79,794,900  15985      4,991    624    127,876 XY_Wing
 *     62,787,100  15556      4,036    303    207,218 XYZ_Wing
 *     86,036,700  15271      5,633    760    113,206 W_Wing
 *     48,712,900  14742      3,304    315    154,644 Skyscraper
 *     88,832,700  14577      6,094    588    151,076 EmptyRectangle
 *     53,666,100  13989      3,836    215    249,609 Swordfish
 *     73,935,300  13934      5,306     23  3,214,578 Jellyfish
 *    284,915,000  13929     20,454    177  1,609,689 Coloring
 *    879,099,200  13814     63,638    788  1,115,608 XColoring
 *  1,915,865,900  13546    141,434 130711     14,657 GEM
 *    145,178,100  12187     11,912     90  1,613,090 NakedQuad
 *    116,490,200  12172      9,570     11 10,590,018 HiddenQuad
 *  1,389,512,400  12171    114,165   1008  1,378,484 URT
 *  2,767,943,300  11471    241,299   2079  1,331,382 BigWings
 *  1,151,118,100  10310    111,650    217  5,304,691 FinnedSwampfish
 *  2,344,856,200  10132    231,430    275  8,526,749 FinnedSwordfish
 *  5,998,775,600   9926    604,349   2547  2,355,231 DeathBlossom
 *  8,533,215,100   7776  1,097,378   1995  4,277,300 ALS_XZ
 *  6,493,028,600   6516    996,474   2710  2,395,951 ALS_Wing
 * 12,733,247,600   4368  2,915,120   1737  7,330,597 ALS_Chain
 *  6,431,402,400   3013  2,134,551    554 11,609,029 UnaryChain
 *  4,858,514,800   2762  1,759,056   3464  1,402,573 MultipleChain
 *  6,558,154,000   1341  4,890,495   6333  1,035,552 DynamicChain
 *    137,418,900      3 45,806,300     30  4,580,630 DynamicPlus
 * 63,830,159,400
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  83,156,894,800 (01:23)  56,762,385
 * NOTES:
 * 1. 1:23 is two seconds faster than previous, but is it worth all the mess?
 * 2. DiufSudoku_V6_30.194.2021-12-04.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs", especially unused code. I used to have a
 *    tool that found unused code. I'll update Java versions, and find a tool.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.195 2021-12-06 10:55:15</b> Use circular-arrays instead of
 * Queues for the local On and Off Queues in ChainerMulti.doChains.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     15,285,500  87724        174  47072        324 NakedSingle
 *     15,535,600  40652        382  14386      1,079 HiddenSingle
 *     61,995,100  26266      2,360  22422      2,764 Locking
 *     67,625,600  16116      4,196   7060      9,578 NakedPair
 *     57,213,400  14442      3,961   7799      7,335 HiddenPair
 *     94,252,700  12611      7,473   1444     65,271 NakedTriple
 *     86,414,700  12295      7,028    913     94,649 HiddenTriple
 *     39,529,700  12133      3,258    459     86,121 Swampfish
 *     37,718,700  11975      3,149    915     41,222 TwoStringKite
 *     59,326,300  11060      5,364    455    130,387 XY_Wing
 *     44,165,600  10750      4,108    223    198,052 XYZ_Wing
 *     75,683,400  10543      7,178    595    127,198 W_Wing
 *     37,522,600  10139      3,700    267    140,534 Skyscraper
 *     69,906,500  10002      6,989    450    155,347 EmptyRectangle
 *     50,989,800   9552      5,338    177    288,077 Swordfish
 *     74,457,500   9516      7,824     16  4,653,593 Jellyfish
 *    286,739,900   9513     30,141    129  2,222,789 Coloring
 *    667,067,400   9436     70,693    536  1,244,528 XColoring
 *  1,548,194,900   9253    167,318  78869     19,629 GEM
 *    126,237,900   8376     15,071     82  1,539,486 NakedQuad
 *     93,958,700   8363     11,235     11  8,541,700 HiddenQuad
 *  1,145,999,100   8362    137,048    846  1,354,608 URT
 *  2,140,466,400   7786    274,912   1677  1,276,366 BigWings
 *    910,848,000   6895    132,102    180  5,060,266 FinnedSwampfish
 *  1,984,501,300   6748    294,087    222  8,939,195 FinnedSwordfish
 *  5,128,031,200   6586    778,626   1829  2,803,734 DeathBlossom
 *  7,372,687,300   5065  1,455,614   1615  4,565,131 ALS_XZ
 *  4,824,854,500   4084  1,181,404   1991  2,423,332 ALS_Wing
 *  8,740,603,100   2540  3,441,182   1142  7,653,768 ALS_Chain
 *  3,774,464,500   1667  2,264,225    403  9,365,916 UnaryChain
 *  2,443,300,700   1493  1,636,504    142 17,206,342 NishioChain
 *  2,565,839,100   1360  1,886,646   1908  1,344,779 MultipleChain
 *  1,118,138,600    586  1,908,086 167448      6,677 DynamicChainT
 *     33,340,400      3 11,113,466   1800     18,522 DynamicPlusT
 * 45,792,895,700
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  63,846,932,100 (01:03)  43,581,523
 * NOTES:
 * 1. 1:03 is fast, but that's with the hair-dryer, so ney come in ye pant.
 * 2. DiufSudoku_V6_30.195.2021-12-06.7z unreleasable: VALIDATE_XCOLORING=true.
 * 3. Next find more "bad eggs", especially unused code. I need a tool. Sigh.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.196 2021-12-08 06:12:16</b> Wee fixes to Coloring, XColoring and
 * AlsChain.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     20,608,900 105806        194  56327        365 NakedSingle
 *     18,474,200  49479        373  16124      1,145 HiddenSingle
 *     84,228,400  33355      2,525  24100      3,494 Locking
 *     77,776,500  22087      3,521   7866      9,887 NakedPair
 *     72,716,600  20041      3,628   8221      8,845 HiddenPair
 *    119,100,400  17981      6,623   1601     74,391 NakedTriple
 *    109,430,000  17590      6,221    969    112,930 HiddenTriple
 *     48,798,500  17404      2,803    644     75,774 Swampfish
 *     52,163,200  17154      3,040   1129     46,203 TwoStringKite
 *     84,159,500  16025      5,251    637    132,118 XY_Wing
 *     62,277,600  15589      3,994    301    206,902 XYZ_Wing
 *     89,505,900  15306      5,847    763    117,307 W_Wing
 *     41,742,800  14771      2,825    320    130,446 Skyscraper
 *     87,988,400  14604      6,024    572    153,825 EmptyRectangle
 *     54,509,700  14032      3,884    218    250,044 Swordfish
 *    295,645,600  13974     21,156    183  1,615,549 Coloring
 *    862,782,500  13856     62,267    686  1,257,700 XColoring
 *  1,882,535,400  13608    138,340 132157     14,244 GEM
 *    146,817,400  12245     11,989     90  1,631,304 NakedQuad
 *    113,925,900  12230      9,315     11 10,356,900 HiddenQuad
 *  1,401,177,500  12229    114,578   1013  1,383,195 URT
 *  2,829,613,800  11525    245,519   2109  1,341,685 BigWings
 *  1,151,690,600  10348    111,295    212  5,432,502 FinnedSwampfish
 *  2,373,589,100  10174    233,299    269  8,823,751 FinnedSwordfish
 *  6,117,707,500   9973    613,427   2530  2,418,066 DeathBlossom
 *  8,705,525,300   7841  1,110,257   2011  4,328,953 ALS_XZ
 *  6,513,629,900   6570    991,420   2738  2,378,973 ALS_Wing
 * 12,905,287,200   4406  2,929,025   1747  7,387,113 ALS_Chain
 *  6,695,450,700   3042  2,201,002    554 12,085,651 UnaryChain
 *  4,049,384,200   2791  1,450,872    188 21,539,277 NishioChain
 *  4,467,747,200   2612  1,710,469   3459  1,291,629 MultipleChain
 *  5,966,255,200   1250  4,773,004   6035    988,608 DynamicChain
 *    122,750,700      3 40,916,900     30  4,091,690 DynamicPlus
 * 67,624,996,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  86,992,541,900 (01:26)  59,380,574
 * <pre>
 * NOTES:
 * 1. 1:26 is 3 seconds slower than best, but this one uses NishioChain.
 * 2. DiufSudoku_V6_30.196.2021-12-08.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs".
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.197 2021-12-16 04:52:17</b> Fix bugs, and bring UniqueRectangle
 * upto date.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,450,900 106032        164  56542        308 NakedSingle
 *     18,019,400  49490        364  16174      1,114 HiddenSingle
 *     75,364,600  33316      2,262  24075      3,130 Locking
 *     76,745,900  22062      3,478   7884      9,734 NakedPair
 *     71,171,700  20013      3,556   8228      8,649 HiddenPair
 *    118,316,300  17949      6,591   1595     74,179 NakedTriple
 *    106,631,900  17559      6,072    979    108,919 HiddenTriple
 *     46,042,800  17371      2,650    652     70,617 Swampfish
 *     50,878,900  17114      2,972   1149     44,281 TwoStringKite
 *     80,697,800  15965      5,054    625    129,116 XY_Wing
 *     61,236,200  15535      3,941    308    198,818 XYZ_Wing
 *     87,140,200  15245      5,715    766    113,760 W_Wing
 *     41,640,300  14711      2,830    314    132,612 Skyscraper
 *     87,735,500  14547      6,031    582    150,748 EmptyRectangle
 *     53,525,400  13965      3,832    216    247,802 Swordfish
 *    281,187,500  13909     20,216    175  1,606,785 Coloring
 *    868,606,600  13794     62,969    795  1,092,586 XColoring
 *  1,860,950,100  13522    137,623 129237     14,399 GEM
 *    146,291,800  12173     12,017     92  1,590,128 NakedQuad
 *  1,388,196,900  12158    114,179    907  1,530,536 URT
 *  2,751,222,500  11513    238,966   2076  1,325,251 BigWings
 *  1,148,007,800  10354    110,875    218  5,266,090 FinnedSwampfish
 *  2,384,030,800  10175    234,302    280  8,514,395 FinnedSwordfish
 *  6,373,652,800   9966    639,539   2543  2,506,351 DeathBlossom
 *  8,733,043,700   7821  1,116,614   1995  4,377,465 ALS_XZ
 *  6,636,041,300   6560  1,011,591   2713  2,446,015 ALS_Wing
 * 12,824,417,800   4411  2,907,371   1756  7,303,199 ALS_Chain
 *  6,534,257,200   3032  2,155,098    568 11,503,973 UnaryChain
 *  4,762,527,800   2776  1,715,607   3475  1,370,511 MultipleChain
 *  6,406,347,100   1344  4,766,627   6363  1,006,812 DynamicChain
 *    104,446,800      3 34,815,600     30  3,481,560 DynamicPlus
 * 64,195,826,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  83,203,219,200 (01:23)  56,794,006
 * NOTES:
 * 1. 1:23 is back to best, three seconds faster than previous, sans Nishio. I
 *    have been "fixing" UniqueRectangle, which's down about 100 elims. I think
 *    that what I've done is correct, but I've really made no attempt to verify
 *    that is in fact correct. I'm just winging it. Sigh.
 * 2. DiufSudoku_V6_30.197.2021-12-16.7z is OK to release. No rush.
 * 3. Next I seek more "bad eggs".
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.198 2021-12-18 11:25:18</b> Endless Pissing About. BigWing speed
 * and move and rename Idx visitor interfaces.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,851,300 105885        168  56360        316 NakedSingle
 *     18,731,400  49525        378  16152      1,159 HiddenSingle
 *     78,991,700  33373      2,366  24071      3,281 Locking
 *     75,678,700  22112      3,422   7891      9,590 NakedPair
 *     70,830,600  20058      3,531   8233      8,603 HiddenPair
 *    115,952,300  17990      6,445   1621     71,531 NakedTriple
 *    106,061,000  17595      6,027    986    107,566 HiddenTriple
 *     47,501,800  17406      2,729    643     73,875 Swampfish
 *     49,595,200  17155      2,891   1137     43,619 TwoStringKite
 *     79,250,500  16018      4,947    632    125,396 XY_Wing
 *     60,479,600  15584      3,880    306    197,645 XYZ_Wing
 *     88,330,000  15296      5,774    772    114,417 W_Wing
 *     42,326,200  14753      2,868    319    132,684 Skyscraper
 *     87,008,000  14587      5,964    566    153,724 EmptyRectangle
 *     55,649,900  14021      3,969    219    254,109 Swordfish
 *    284,022,400  13962     20,342    178  1,595,631 Coloring
 *    872,636,900  13846     63,024    692  1,261,035 XColoring
 *  1,874,521,900  13595    137,883 131495     14,255 GEM
 *    145,320,200  12245     11,867     92  1,579,567 NakedQuad
 *  1,442,381,400  12230    117,937    905  1,593,791 URT
 *    296,125,000  11585     25,561    796    372,016 WXYZ_Wing
 *    424,288,400  11084     38,279    856    495,664 VWXYZ_Wing
 *    495,451,500  10634     46,591    372  1,331,858 UVWXYZ_Wing
 *    381,281,900  10448     36,493     81  4,707,183 TUVWXYZ_Wing
 *    192,566,100  10402     18,512      8 24,070,762 STUVWXYZ_Wing
 *  1,160,044,000  10398    111,564    213  5,446,215 FinnedSwampfish
 *  2,420,293,900  10223    236,749    275  8,801,068 FinnedSwordfish
 *  8,690,797,800  10019    867,431   2533  3,431,029 DeathBlossom
 *  9,051,060,200   7886  1,147,737   2012  4,498,538 ALS_XZ
 *  6,614,201,400   6614  1,000,030   2744  2,410,423 ALS_Wing
 * 13,080,431,200   4447  2,941,405   1770  7,390,074 ALS_Chain
 *  6,914,907,400   3060  2,259,773    568 12,174,132 UnaryChain
 *  4,109,940,800   2804  1,465,742    187 21,978,293 NishioChain
 *  4,631,395,900   2626  1,763,669   3470  1,334,696 MultipleChain
 *  6,157,667,300   1253  4,914,339   6065  1,015,279 DynamicChain
 *    118,572,700      3 39,524,233     30  3,952,423 DynamicPlus
 * 70,352,146,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  90,213,694,100 (01:30)  61,579,313
 * NOTES:
 * 1. 1:30 is 7 seconds above par but uses 5 BigWing instead of BigWings, which
 *    is actually faster for not using the ALS-cache, but doesn't use ALS-cache
 *    so DeathBlossom has to prime the cache, and consequently takes longer,
 *    hence BigWings is preferred, because it's faster overall. Sigh.
 * 2. DiufSudoku_V6_30.198.2021-12-18.7z is OK to release. No rush.
 * 3. Next I still seek "bad eggs" but not finding much. I reckon it's about as
 *    good as I can make it. Another might do orders of magnitude better.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.199 2021-12-24 05:59:19</b> Reorder Techs to run faster hinters
 * first which forces a rejig of Difficulty's to reduce largest, so I went the
 * whole hog and split Techs into 10 Difficulty, for a nicer Generator. Also
 * Generator hits a BFIIK bug on Lockings grid field, so I removed the field.
 * This piece of s__t really is NOT very reliable. Poorly engineered. Sigh.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,505,500 106001        165  56528        309 NakedSingle
 *     18,686,600  49473        377  16188      1,154 HiddenSingle
 *     76,574,500  33285      2,300  24073      3,180 Locking
 *     78,083,700  22035      3,543   7879      9,910 NakedPair
 *     70,866,400  19986      3,545   8223      8,618 HiddenPair
 *    118,060,400  17926      6,585   1595     74,019 NakedTriple
 *    106,508,500  17536      6,073    981    108,571 HiddenTriple
 *     46,891,900  17348      2,703    640     73,268 Swampfish
 *     49,766,100  17094      2,911   1145     43,463 TwoStringKite
 *     93,921,700  15949      5,888    906    103,666 W_Wing
 *     71,258,400  15321      4,651    529    134,703 XY_Wing
 *     43,308,600  14968      2,893    319    135,763 Skyscraper
 *     89,226,300  14802      6,027    586    152,263 EmptyRectangle
 *     52,148,600  14216      3,668    267    195,313 XYZ_Wing
 *     54,034,400  13966      3,868    216    250,159 Swordfish
 *    282,028,200  13910     20,275    175  1,611,589 Coloring
 *    899,167,200  13795     65,180    795  1,131,027 XColoring
 *  1,875,817,400  13523    138,713 129237     14,514 GEM
 *  1,362,008,400  12174    111,878    908  1,500,009 URT
 *    135,972,000  11528     11,794     91  1,494,197 NakedQuad
 *  2,852,636,000  11513    247,775   2076  1,374,102 BigWings
 *  1,140,731,500  10354    110,173    218  5,232,713 FinnedSwampfish
 *  2,373,017,800  10175    233,220    280  8,475,063 FinnedSwordfish
 *  6,473,106,800   9966    649,519   2543  2,545,460 DeathBlossom
 *  8,796,405,400   7821  1,124,716   1995  4,409,225 ALS_XZ
 *  6,566,347,800   6560  1,000,967   2713  2,420,327 ALS_Wing
 * 12,816,889,000   4411  2,905,665   1756  7,298,911 ALS_Chain
 *  6,523,024,500   3032  2,151,393    568 11,484,198 UnaryChain
 *  4,718,613,800   2776  1,699,788   3475  1,357,874 MultipleChain
 *  6,414,501,800   1344  4,772,694   6363  1,008,093 DynamicChain
 *    112,813,100      3 37,604,366     30  3,760,436 DynamicPlus
 * 64,329,922,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  82,910,858,800 (01:22)  56,594,442
 * NOTES:
 * 1. 1:22 is a second under par, which is about what I'd expect from putting
 *    the-faster-less-used-hinters before slower ones.
 * 2. DiufSudoku_V6_30.199.2021-12-24.7z is OK to release. No rush.
 * 3. Next I shall extract white phosporous from dog s__t in order to build my
 *    own 81 bit semiconductors (3 9-bit bytes is an int, 3 of them is a long),
 *    in order to REALLY kick Sudoku's ass. Please don't feed them after dark,
 *    never get them wet, and don't take them seriously. Nor me. Sigh smile.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.200 2021-12-27 07:11:20</b> faster BigWings. new IntArrays.
 * Chainers isX/YChain fields. Remove CASs from Cells.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     19,237,300 106001        181  56528        340 NakedSingle
 *     18,299,900  49473        369  16188      1,130 HiddenSingle
 *     77,698,500  33285      2,334  24073      3,227 Locking
 *     79,194,900  22035      3,594   7879     10,051 NakedPair
 *     71,449,300  19986      3,574   8223      8,688 HiddenPair
 *    117,319,700  17926      6,544   1595     73,554 NakedTriple
 *    107,689,600  17536      6,141    981    109,775 HiddenTriple
 *     45,190,600  17348      2,604    640     70,610 Swampfish
 *     49,128,900  17094      2,874   1145     42,907 TwoStringKite
 *     93,397,000  15949      5,855    906    103,087 W_Wing
 *     72,078,100  15321      4,704    529    136,253 XY_Wing
 *     48,622,900  14968      3,248    319    152,422 Skyscraper
 *     92,284,500  14802      6,234    586    157,482 EmptyRectangle
 *     53,895,500  14216      3,791    267    201,855 XYZ_Wing
 *     52,716,200  13966      3,774    216    244,056 Swordfish
 *    276,537,300  13910     19,880    175  1,580,213 Coloring
 *    861,886,900  13795     62,478    795  1,084,134 XColoring
 *  1,838,092,600  13523    135,923 129237     14,222 GEM
 *  1,350,888,700  12174    110,965    908  1,487,762 URT
 *    137,006,800  11528     11,884     91  1,505,569 NakedQuad
 *  2,650,495,500  11513    230,217   2076  1,276,731 BigWings
 *  1,124,440,000  10354    108,599    218  5,157,981 FinnedSwampfish
 *  2,342,677,400  10175    230,238    280  8,366,705 FinnedSwordfish
 *  6,581,696,800   9966    660,415   2543  2,588,162 DeathBlossom
 *  8,681,504,500   7821  1,110,024   1995  4,351,631 ALS_XZ
 *  6,594,637,200   6560  1,005,280   2713  2,430,754 ALS_Wing
 * 12,848,937,200   4411  2,912,930   1756  7,317,162 ALS_Chain
 *  6,761,683,300   3032  2,230,106    568 11,904,372 UnaryChain
 *  4,850,240,300   2776  1,747,204   3475  1,395,752 MultipleChain
 *  6,273,320,000   1344  4,667,648   6363    985,906 DynamicChain
 *    120,168,500      3 40,056,166     30  4,005,616 DynamicPlus
 * 64,292,415,900
 * pzls     total (ns) (mm:ss)   each (ns)
 * 1465 82,361,085,600 (01:22)  56,219,171
 * NOTES:
 * 1. 1:22 is same as last time, so I think I can move "par" downto 1:22. These
 *    changes are mostly about better RAM usage, which I hoped would be faster.
 *    I'd also hoped that isX/YChain fields would be faster. They aren't, which
 *    is a disappointment, so I might revert this change in a future release.
 * 2. DiufSudoku_V6_30.200.2021-12-27.7z is OK to release. No rush.
 * 3. Next I keep hunting my bads. There's lots of silly code in SE. The more I
 *    look at it the less I like it. I prefer HoDoKu, despite its slow nets.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.201 2021-12-27 12:21:21</b> Cells and IntArrays use MyArrayList
 * instead of an Iterator.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,753,500 106001        167  56528        314 NakedSingle
 *     18,778,700  49473        379  16188      1,160 HiddenSingle
 *     77,412,000  33285      2,325  24073      3,215 Locking
 *     76,924,700  22035      3,491   7879      9,763 NakedPair
 *     72,840,800  19986      3,644   8223      8,858 HiddenPair
 *    120,357,300  17926      6,714   1595     75,459 NakedTriple
 *    105,933,000  17536      6,040    981    107,984 HiddenTriple
 *     47,588,300  17348      2,743    640     74,356 Swampfish
 *     50,079,400  17094      2,929   1145     43,737 TwoStringKite
 *     93,489,500  15949      5,861    906    103,189 W_Wing
 *     70,465,700  15321      4,599    529    133,205 XY_Wing
 *     45,301,500  14968      3,026    319    142,010 Skyscraper
 *     90,506,000  14802      6,114    586    154,447 EmptyRectangle
 *     57,288,900  14216      4,029    267    214,565 XYZ_Wing
 *     54,173,400  13966      3,878    216    250,802 Swordfish
 *    268,552,800  13910     19,306    175  1,534,587 Coloring
 *    853,750,100  13795     61,888    795  1,073,899 XColoring
 *  1,841,118,900  13523    136,147 129237     14,246 GEM
 *  1,381,736,900  12174    113,499    908  1,521,736 URT
 *    136,986,200  11528     11,882     91  1,505,342 NakedQuad
 *  2,655,209,100  11513    230,627   2076  1,279,002 BigWings
 *  1,130,100,400  10354    109,146    218  5,183,946 FinnedSwampfish
 *  2,346,156,800  10175    230,580    280  8,379,131 FinnedSwordfish
 *  6,545,876,600   9966    656,820   2543  2,574,076 DeathBlossom
 *  8,624,637,400   7821  1,102,753   1995  4,323,126 ALS_XZ
 *  6,502,186,400   6560    991,186   2713  2,396,677 ALS_Wing
 * 12,783,065,300   4411  2,897,997   1756  7,279,649 ALS_Chain
 *  6,732,295,900   3032  2,220,414    568 11,852,633 UnaryChain
 *  4,956,096,800   2776  1,785,337   3475  1,426,214 MultipleChain
 *  6,354,195,000   1344  4,727,823   6363    998,616 DynamicChain
 *    121,309,800      3 40,436,600     30  4,043,660 DynamicPlus
 * 64,232,167,100
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  82,396,745,100 (01:22)  56,243,512
 * NOTES:
 * 1. 1:22 is actually about 35ms slower, which is a real disappointment. I had
 *    hoped it would be half-a-second faster, but I'll stick with it, to let it
 *    bed in, before I decide, but watch this space. java.util is so highly
 *    optimised that any replacement (without the proprietory opts) is slower.
 * 2. DiufSudoku_V6_30.201.2021-12-27.7z is OK to release. No rush.
 * 3. Next I keep hunting stuff to fix and improve.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.202 2021-12-28 11:33:22</b> Chainers need not check for empty
 * parents list because the rule is parents is null or populated, NEVER empty.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     19,166,200 106001        180  56528        339 NakedSingle
 *     20,584,900  49473        416  16188      1,271 HiddenSingle
 *     79,768,000  33285      2,396  24073      3,313 Locking
 *     80,881,800  22035      3,670   7879     10,265 NakedPair
 *     73,297,600  19986      3,667   8223      8,913 HiddenPair
 *    119,703,800  17926      6,677   1595     75,049 NakedTriple
 *    108,934,900  17536      6,212    981    111,044 HiddenTriple
 *     46,895,200  17348      2,703    640     73,273 Swampfish
 *     50,164,400  17094      2,934   1145     43,811 TwoStringKite
 *     98,154,700  15949      6,154    906    108,338 W_Wing
 *     76,741,400  15321      5,008    529    145,068 XY_Wing
 *     49,892,700  14968      3,333    319    156,403 Skyscraper
 *     99,370,200  14802      6,713    586    169,573 EmptyRectangle
 *     61,649,900  14216      4,336    267    230,898 XYZ_Wing
 *     55,497,400  13966      3,973    216    256,932 Swordfish
 *    292,240,600  13910     21,009    175  1,669,946 Coloring
 *    920,777,400  13795     66,747    795  1,158,210 XColoring
 *  1,991,403,000  13523    147,260 129237     15,408 GEM
 *  1,404,554,500  12174    115,373    908  1,546,866 URT
 *    143,809,000  11528     12,474     91  1,580,318 NakedQuad
 *  2,798,773,700  11513    243,096   2076  1,348,156 BigWings
 *  1,184,556,200  10354    114,405    218  5,433,744 FinnedSwampfish
 *  2,487,535,200  10175    244,475    280  8,884,054 FinnedSwordfish
 *  7,042,097,300   9966    706,612   2543  2,769,208 DeathBlossom
 *  9,138,927,900   7821  1,168,511   1995  4,580,916 ALS_XZ
 *  6,882,784,400   6560  1,049,204   2713  2,536,964 ALS_Wing
 * 13,893,220,600   4411  3,149,675   1756  7,911,856 ALS_Chain
 *  6,807,379,400   3032  2,245,177    568 11,984,822 UnaryChain
 *  4,956,697,400   2776  1,785,553   3475  1,426,387 MultipleChain
 *  6,702,997,800   1344  4,987,349   6363  1,053,433 DynamicChain
 *    128,042,500      3 42,680,833     30  4,268,083 DynamicPlus
 * 67,816,500,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  86,602,089,900 (01:26)  59,114,054
 * NOTES:
 * 1. 1:26 is 4 seconds over par, which is a worry, but all hinters (not just
 *    the ones I've changed) are slower so I blame hot box syndrome, where the
 *    CPU slows itself down instead of overheating, despite the fact that it's
 *    only about 25c today, which is "not at all hot" in my mind. Sigh.
 * 2. DiufSudoku_V6_30.202.2021-12-29.7z is OK to release. No rush.
 * 3. Next I look for more stuff to fix.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.203 2021-12-31 13:42:24</b> DeathBlossom is a little bit faster.
 * It now uses parallel arrays to hold als-fields in the alssByValue array, to
 * save dereferencing them 270/85/30 million times, which was a bit slow Redge.
 * Recurse now also builds-up the dbIdx, commonCands and freeCands fields as it
 * calls itself, instead of setting local vars from which "old" values must be
 * removed. DeathBlossomData is history. Repeated dereferencing wasted time. It
 * was a mistake. My bad!
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,979,600 106001        179  56528        335 NakedSingle
 *     20,073,700  49473        405  16188      1,240 HiddenSingle
 *     76,640,700  33285      2,302  24073      3,183 Locking
 *     75,230,400  22035      3,414   7879      9,548 NakedPair
 *     72,129,600  19986      3,609   8223      8,771 HiddenPair
 *    116,220,900  17926      6,483   1595     72,865 NakedTriple
 *    106,949,700  17536      6,098    981    109,021 HiddenTriple
 *     48,055,200  17348      2,770    640     75,086 Swampfish
 *     49,115,500  17094      2,873   1145     42,895 TwoStringKite
 *     95,928,300  15949      6,014    906    105,881 W_Wing
 *     74,738,600  15321      4,878    529    141,282 XY_Wing
 *     46,473,100  14968      3,104    319    145,683 Skyscraper
 *    106,189,200  14802      7,173    586    181,210 EmptyRectangle
 *     61,505,000  14216      4,326    267    230,355 XYZ_Wing
 *     54,548,600  13966      3,905    216    252,539 Swordfish
 *    276,249,800  13910     19,859    175  1,578,570 Coloring
 *    881,117,600  13795     63,872    795  1,108,324 XColoring
 *  1,863,363,000  13523    137,792 129237     14,418 GEM
 *  1,370,673,900  12174    112,590    908  1,509,552 URT
 *    136,418,400  11528     11,833     91  1,499,103 NakedQuad
 *  2,659,068,300  11513    230,962   2076  1,280,861 BigWings
 *  1,236,009,000  10354    119,375    218  5,669,766 FinnedSwampfish
 *  2,682,150,900  10175    263,602    280  9,579,110 FinnedSwordfish
 *  4,653,150,100   9966    466,902   2543  1,829,787 DeathBlossom
 *  8,802,086,700   7821  1,125,442   1995  4,412,073 ALS_XZ
 *  6,472,043,300   6560    986,591   2713  2,385,567 ALS_Wing
 * 12,937,551,600   4411  2,933,020   1756  7,367,626 ALS_Chain
 *  6,466,455,800   3032  2,132,736    568 11,384,605 UnaryChain
 *  4,711,879,800   2776  1,697,363   3475  1,355,936 MultipleChain
 *  6,355,866,800   1344  4,729,067   6363    998,878 DynamicChain
 *    119,115,400      3 39,705,133     30  3,970,513 DynamicPlus
 * 62,645,978,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  81,099,071,100 (01:21)  55,357,727
 * NOTES:
 * 1. 1:21 is a second under par, but DeathBlossom is what I've changed and it
 *    was 7.042 and is now 4.653 which is about 2.4 seconds faster, so I know
 *    not what's going on with the rest of it, but it'll come good again.
 * 2. DiufSudoku_V6_30.203.2021-12-31.7z is OK to release. No rush.
 * 3. Next I find more of my badness, and fix it.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.204 2022-01-01 10:11:24</b> RccFinders idx caching. Promoted
 * NakedQuad and HiddenQuad.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,737,100 105881        167  56359        314 NakedSingle
 *     18,851,300  49522        380  16180      1,165 HiddenSingle
 *     78,801,200  33342      2,363  24077      3,272 Locking
 *     78,676,500  22083      3,562   7880      9,984 NakedPair
 *     74,550,400  20032      3,721   8228      9,060 HiddenPair
 *    118,265,500  17968      6,582   1621     72,958 NakedTriple
 *    108,568,100  17573      6,178    968    112,157 HiddenTriple
 *     47,730,000  17388      2,744    624     76,490 Swampfish
 *     46,507,000  17141      2,713   1131     41,120 TwoStringKite
 *     97,512,500  16010      6,090    914    106,687 W_Wing
 *     76,727,600  15373      4,991    535    143,416 XY_Wing
 *     46,639,200  15017      3,105    324    143,948 Skyscraper
 *     99,522,100  14849      6,702    569    174,907 EmptyRectangle
 *     61,045,400  14280      4,274    262    232,997 XYZ_Wing
 *     55,882,100  14035      3,981    219    255,169 Swordfish
 *    163,945,600  13976     11,730     94  1,744,102 NakedQuad
 *     78,743,000  13960      5,640     18  4,374,611 Jellyfish
 *    130,109,200  13956      9,322     12 10,842,433 HiddenQuad
 *    286,207,800  13954     20,510    175  1,635,473 Coloring
 *    915,349,000  13840     66,137    692  1,322,758 XColoring
 *  1,954,226,200  13589    143,809 131243     14,890 GEM
 *  1,398,916,500  12240    114,290    908  1,540,656 URT
 *  2,761,153,900  11593    238,174   2107  1,310,466 BigWings
 *  1,280,419,400  10418    122,904    213  6,011,358 FinnedSwampfish
 *  2,782,086,200  10243    271,608    275 10,116,677 FinnedSwordfish
 *  4,879,096,300  10039    486,014   2530  1,928,496 DeathBlossom
 *  8,912,592,800   7908  1,127,035   2013  4,427,517 ALS_XZ
 *  6,645,076,800   6633  1,001,820   2753  2,413,758 ALS_Wing
 * 12,951,348,300   4457  2,905,844   1775  7,296,534 ALS_Chain
 *    790,701,600   3065    257,977     15 52,713,440 SueDeCoq
 *  6,923,654,500   3062  2,261,154    571 12,125,489 UnaryChain
 *  4,287,998,800   2803  1,529,789    187 22,930,474 NishioChain
 *  4,799,561,200   2625  1,828,404   3471  1,382,760 MultipleChain
 *  6,226,839,700   1251  4,977,489   6045  1,030,081 DynamicChain
 *    139,567,500      3 46,522,500     30  4,652,250 DynamicPlus
 * 69,334,610,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  88,143,351,600 (01:28)  60,166,110
 * NOTES:
 * 1. 1:28 is 7 seconds over par, but I've reinstated all the hinters that I'd
 *    previously dropped for being "a bit slow". They're still a bit slow but
 *    there's no point pushing the batch below about 3:00. In the GUI I'll wait
 *    a few milliseconds for a more accurate answer to the question "What's the
 *    next simplest available hint?". Basically, I wait for hinters that take
 *    upto 100ms per elimination in the batch. If I can't bring a hinter under
 *    100ms/elim, then he gets the chop, so the GUI is snappy enough. Simples.
 * 2. DiufSudoku_V6_30.204.2022-01-01.7z is OK to release. Soon.
 * 3. Next I find some more stupidity, and fix it.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.205 2022-01-06 16:36:25</b> ChainerUnary seeks only XY-Cycles
 * and XY-Chains. MyLinkedHashMap.clear uses the linked list on small maps.
 * IDKFA is now DynamicPlus+ only (not high DynamicChain).
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,815,500 105881        168  56359        316 NakedSingle
 *     18,619,700  49522        375  16180      1,150 HiddenSingle
 *     80,833,600  33342      2,424  24077      3,357 Locking
 *     75,950,000  22083      3,439   7880      9,638 NakedPair
 *     72,453,800  20032      3,616   8228      8,805 HiddenPair
 *    118,851,800  17968      6,614   1621     73,320 NakedTriple
 *    107,087,500  17573      6,093    968    110,627 HiddenTriple
 *     46,341,100  17388      2,665    624     74,264 Swampfish
 *     47,110,400  17141      2,748   1131     41,653 TwoStringKite
 *     96,336,300  16010      6,017    914    105,400 W_Wing
 *     76,982,800  15373      5,007    535    143,893 XY_Wing
 *     46,341,000  15017      3,085    324    143,027 Skyscraper
 *    104,371,500  14849      7,028    569    183,429 EmptyRectangle
 *     61,675,100  14280      4,318    262    235,401 XYZ_Wing
 *     54,894,800  14035      3,911    219    250,661 Swordfish
 *    163,309,300  13976     11,684     94  1,737,332 NakedQuad
 *     76,930,900  13960      5,510     18  4,273,938 Jellyfish
 *    129,064,200  13956      9,247     12 10,755,350 HiddenQuad
 *    281,747,200  13954     20,191    175  1,609,984 Coloring
 *    912,341,400  13840     65,920    692  1,318,412 XColoring
 *  1,923,558,300  13589    141,552 131243     14,656 GEM
 *  1,373,698,300  12240    112,230    908  1,512,883 URT
 *  2,802,733,900  11593    241,760   2107  1,330,201 BigWings
 *  1,252,447,000  10418    120,219    213  5,880,032 FinnedSwampfish
 *  2,742,267,100  10243    267,721    275  9,971,880 FinnedSwordfish
 *  4,897,177,300  10039    487,815   2530  1,935,643 DeathBlossom
 *  8,991,515,000   7908  1,137,015   2013  4,466,723 ALS_XZ
 *  6,714,289,400   6633  1,012,255   2753  2,438,899 ALS_Wing
 * 13,069,468,300   4457  2,932,346   1775  7,363,080 ALS_Chain
 *    786,451,900   3065    256,591     15 52,430,126 SueDeCoq
 *  5,277,646,600   3062  1,723,594    571  9,242,813 UnaryChain
 *  4,346,535,500   2803  1,550,672    187 23,243,505 NishioChain
 *  4,813,564,900   2625  1,833,739   3471  1,386,794 MultipleChain
 *  6,125,563,600   1251  4,896,533   6045  1,013,327 DynamicChain
 *    114,519,000      3 38,173,000     30  3,817,300 DynamicPlus
 * 67,820,494,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  86,836,365,500 (01:26)  59,273,969
 * NOTES:
 * 1. 1:26 is a couple of seconds faster, but ____ it, she'll have to do.
 * 2. DiufSudoku_V6_30.205.2022-01-06.7z is OK to release. Soon.
 * 3. Next I find some more of my own stupidity, and fix it.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.206 2022-01-13 15:25:26</b> tried to upgrade to Java 17 but VM
 * is about 30% slower than 1.8 for this rather odd application, so I decided
 * against the upgrade. You can run it in later VM's, but the music died some
 * time between 1.8.0_271 and 17.0.1+12-LTS-39. 17 is bad at bit-twiddling. I
 * suppose it could be box: Winblows Home 10.1903 on Aspire Intel i7-7500U.
 * <p>
 * I reduce AlsFinderPlain.DEGREE_CEILING from 8 to 7 to skip alss with 7 cells
 * (STUVWXYZ_Wing's) because they take more time than they are worth.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     17,827,116 105812        168  56340        316 NakedSingle
 *     18,692,381  49472        377  16171      1,155 HiddenSingle
 *     79,100,213  33301      2,375  24079      3,285 Locking
 *     83,317,565  22046      3,779   7866     10,592 NakedPair
 *     75,217,767  19997      3,761   8225      9,145 HiddenPair
 *    123,036,678  17937      6,859   1637     75,159 NakedTriple
 *    112,885,329  17539      6,436    970    116,376 HiddenTriple
 *     49,746,911  17354      2,866    625     79,595 Swampfish
 *     47,844,476  17106      2,796   1130     42,340 TwoStringKite
 *     95,128,906  15976      5,954    918    103,626 W_Wing
 *     75,315,981  15338      4,910    533    141,305 XY_Wing
 *     44,371,945  14983      2,961    324    136,950 Skyscraper
 *    102,136,726  14815      6,894    569    179,502 EmptyRectangle
 *     59,642,847  14246      4,186    262    227,644 XYZ_Wing
 *     56,499,910  14001      4,035    218    259,173 Swordfish
 *    166,313,859  13943     11,928     94  1,769,296 NakedQuad
 *     77,534,272  13927      5,567     18  4,307,459 Jellyfish
 *    133,981,098  13923      9,623     12 11,165,091 HiddenQuad
 *    293,812,819  13921     21,105    167  1,759,358 Coloring
 *    916,444,837  13807     66,375    694  1,320,525 XColoring
 *  1,994,637,092  13554    147,162 131498     15,168 GEM
 *  1,411,869,767  12211    115,622    912  1,548,102 UniqueRectangle
 *  2,588,157,848  11561    223,869   2108  1,227,778 BigWings
 *  1,299,172,381  10388    125,064    211  6,157,215 FinnedSwampfish
 *  2,877,598,819  10215    281,703    275 10,463,995 FinnedSwordfish
 *  4,666,075,032  10011    466,094   2469  1,889,864 DeathBlossom
 *  8,193,698,603   7923  1,034,166   2020  4,056,286 ALS_XZ
 *  5,860,570,707   6648    881,553   2742  2,137,334 ALS_Wing
 * 11,326,306,115   4482  2,527,065   1792  6,320,483 ALS_Chain
 *    758,162,932   3084    245,837     15 50,544,195 SueDeCoq
 *  5,355,928,997   3081  1,738,373    582  9,202,627 UnaryChain
 *  4,187,876,110   2820  1,485,062    187 22,395,059 NishioChain
 *  4,673,246,310   2642  1,768,829   3478  1,343,659 MultipleChain
 *  6,189,432,823   1252  4,943,636   6046  1,023,723 DynamicChain
 *    118,058,900      3 39,352,966     30  3,935,296 DynamicPlus
 * 64,129,644,072
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  83,247,732,700 (01:23)  56,824,390
 * NOTES:
 * 1. 01:23 is a whole 3 seconds faster than previous, but non ye wet ye pant.
 *    I'm concreted onto 1.8 to achieve anything like it; the best I did in JDK
 *    17 was 1:54, ergo down like 30 secs, hence I refuse the platform upgrade.
 *    Most of this gain comes from AlsFinderPlain.DEGREE_CEILING reduction.
 * 2. DiufSudoku_V6_30.206.2022-01-13.7z is OK to release.
 * 3. Next nothing. No platform upgrade so SE can die with 1.8. I'm disgusted
 *    with Oracle. I'm sure they have reasons, which may be beyond me, but a
 *    30% disformance in 5 years is shootin time were I live. Platforms are
 *    supposed to improve! Why bother when the JDK has snail-mailed itself
 *    straight to hell. Needles McKelly sighing-off from the Mooritanian Front.
 *    BIG SIGH!
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.207 2022-01-19 11:29:27</b> Reduced ALS_CEILING to 6. Big ALSs
 * aren't worth there salts. EmptyRectangle is a bit faster. Build for backup
 * pre split ridx's into parallel int[] places (.bits) and spots (.size).
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     18,057,700 105926        170  56551        319 NakedSingle
 *     18,030,400  49375        365  16116      1,118 HiddenSingle
 *     81,047,500  33259      2,436  24095      3,363 Locking
 *     81,083,200  22014      3,683   7853     10,325 NakedPair
 *     74,668,600  19980      3,737   8255      9,045 HiddenPair
 *    119,013,000  17912      6,644   1644     72,392 NakedTriple
 *    112,089,100  17519      6,398    963    116,395 HiddenTriple
 *     49,511,700  17332      2,856    632     78,341 Swampfish
 *     47,955,000  17081      2,807   1130     42,438 TwoStringKite
 *     97,248,000  15951      6,096    913    106,514 W_Wing
 *     79,763,200  15314      5,208    531    150,213 XY_Wing
 *     44,898,700  14959      3,001    336    133,627 Skyscraper
 *     98,389,400  14783      6,655    567    173,526 EmptyRectangle
 *     59,044,600  14216      4,153    261    226,224 XYZ_Wing
 *     56,236,900  13972      4,024    222    253,319 Swordfish
 *    161,913,800  13913     11,637     94  1,722,487 NakedQuad
 *     76,932,800  13897      5,535     18  4,274,044 Jellyfish
 *    132,390,700  13893      9,529     12 11,032,558 HiddenQuad
 *    294,908,500  13891     21,230    165  1,787,324 Coloring
 *    921,017,600  13775     66,861    695  1,325,205 XColoring
 *  1,931,173,500  13521    142,827 129665     14,893 GEM
 *  1,448,578,100  12177    118,960    896  1,616,716 UniqueRectangle
 *  2,123,707,200  11541    184,014   2059  1,031,426 BigWings
 *  1,338,267,900  10403    128,642    217  6,167,133 FinnedSwampfish
 *  2,896,227,900  10226    283,221    271 10,687,187 FinnedSwordfish
 *  3,143,506,500  10026    313,535   2257  1,392,780 DeathBlossom
 *  5,340,077,000   8125    657,240   2050  2,604,915 ALS_XZ
 *  3,729,684,900   6834    545,754   2613  1,427,357 ALS_Wing
 *  7,637,143,400   4778  1,598,397   1873  4,077,492 ALS_Chain
 *    834,213,300   3328    250,665     15 55,614,220 SueDeCoq
 *  5,798,303,900   3325  1,743,850    736  7,878,130 UnaryChain
 *  4,322,064,300   2982  1,449,384    193 22,394,115 NishioChain
 *  4,951,675,500   2798  1,769,719   3633  1,362,971 MultipleChain
 *  6,351,722,600   1298  4,893,468   6290  1,009,812 DynamicChain
 *    137,195,100      3 45,731,700     30  4,573,170 DynamicPlus
 * 54,607,741,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  73,392,694,600 (01:13)  50,097,402
 * NOTES:
 * 1. 01:13 is 10 seconds faster than previous, but non ye wet ye pant. Most of
 *    this is DEGREE_CEILING reduced to 6. Large ALSs aren't worth there salts.
 *    There's about a half a second from the other eight days work. Gee wow!
 * 2. DiufSudoku_V6_30.207.2022-01-19.7z is OK to release.
 * 3. Next nothing. Bail coz JVM is shot: C#, GNUCC, or the full MASM?
 * </pre>
 */
final class BuildTimings {

	private BuildTimings() { } // Never used

}
