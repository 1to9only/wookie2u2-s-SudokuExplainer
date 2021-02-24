/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * The LogicalSolverTimings class exists to hold this comment of timings from a
 * recent LogicalSolverTester log near each build; to keep track of how long it
 * takes to solve all 1465 Sudoku puzzles with logic, to see if we're making
 * progress in the quest to solve all possible Sudoku puzzles as simply and
 * quickly as possible.
 * <p>
 * That and it's just nice to remember stuff like it took Juillerat just over
 * 27 minutes to solve 1465 puzzles; and now that's down to like 3 or 4 mins.
 * So all it takes is the investment of years and years of hard complex bloody
 * pernickety work, and you too can claim to be a total, complete and utter
 * time-wasting Putz! Nag! Putz!
 * <p>
 * This comment was boosted from the LogicalSolver class comments because it's
 * too bloody long, so Nutbeans bogs down when it's in with the code, but here
 * with no code it seems to be fine. Go figure.
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
 *     9,946,812,269   16347    1113 Unique Rectangle or Loop
 *       759,591,584   15824      59 Bivalue Universal Grave
 *     1,045,301,780   15796       2 Aligned Pair Exclusion
 *   381,454,233,202   15794     645 Aligned Triple Exclusion
 *   324,716,699,628   15217  203576 Unary and Cycle Chain
 *   104,867,749,248    7749    2011 Nishio Chain
 *    99,642,081,087    6637   20452 Multiple Chain
 *   139,180,249,531    1895   15677 Dynamic Chain
 *     1,364,683,988       9      82 Dynamic Plus
 * </pre>
 * <hr><p>
 * <b>KRC 2018-02-14 running all nesters on raw 2#top1465.d2.mt</b>
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
 *       time (ns)  calls   time/call   elims    time/elim hinter
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
 *     comparison. It took 27 minutes 43 seconds to solve the same 1465 puzzles.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-09-10</b> with A3E + A4E but NO A5E.
 * <pre>
 *        time (ns)  calls   time/call   elims      time/elim hinter
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
 * 2. A3E now runs AFTER UnaryChain, plus a couple of performance tweaks brought
 *    it down to just under 18 seconds. Acceptable. If I could get A4E down to
 *    24 seconds I'd take it in a second.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-09-24</b> with A3E, A4E, A5E.
 * <pre>
 *        time (ns)  calls      time/call   elims    time/elim hinter
 *       14,961,463  42576            351  135050          110 Naked Single
 *       45,516,188  41228          1,104  355750          127 Hidden Single
 *      156,016,062  32883          4,744   33694        4,630 Direct Naked Pair
 *      120,961,186  32452          3,727   46176        2,619 Direct Hidden Pair
 *      203,284,743  31448          6,464    7212       28,187 Direct Naked Triple
 *      171,422,105  31396          5,459    8680       19,749 Direct Hidden Triple
 *      108,075,928  31258          3,457   22364        4,832 Point and Claim
 *       84,615,682  20492          4,129    3931       21,525 Naked Pair
 *       55,377,414  19025          2,910    5288       10,472 Hidden Pair
 *      102,776,253  17587          5,843    1194       86,077 Naked Triple
 *       84,516,885  17249          4,899    1016       83,185 Hidden Triple
 *      200,353,063  17051         11,750     817      245,230 XY-Wing
 *       56,771,945  16503          3,440     653       86,940 Swampfish
 *       66,468,686  16248          4,090     319      208,365 Swordfish
 *      183,617,463  16158         11,363     129    1,423,391 Naked Quad
 *       18,886,894  16138          1,170      12    1,573,907 Jellyfish
 *      198,167,455  16134         12,282     377      525,643 XYZ-Wing
 *      142,852,465  16134          8,854       0            0 Hidden Quad
 *      161,170,299  16134          9,989       0            0 Naked Pent
 *      125,508,662  16134          7,779       0            0 Hidden Pent
 *    1,712,653,661  15782        108,519    1122    1,526,429 Unique Rectangle
 *      205,039,849  15249         13,446      52    3,943,074 Bi-Uni Grave
 *    4,226,374,655  15223        277,630      23  183,755,419 Aligned Pair
 *   57,645,339,830  15200      3,792,456  252114      228,647 Unary Chain
 *   27,136,037,780   7401      3,666,536     108  251,259,609 Aligned Triple
 *    8,725,338,643   7304      1,194,597    2511    3,474,846 Nishio Chain
 *   17,042,232,868   6218      2,740,790   21629      787,934 Multiple Chain
 *   16,510,659,946   1694      9,746,552   15878    1,039,845 Dynamic Chain
 *      338,240,936      3    112,746,978       0            0 Aligned Quad
 *    4,896,278,836      3  1,632,092,945       0            0 Aligned Pent
 *      163,206,909      3     54,402,303      30    5,440,230 Dynamic Plus
 *  140,902,724,754
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
 *        time (ns)  calls   time/call   elims    time/elim hinter
 *       18,052,498  46008         392  225530           80 Naked Single
 *       46,726,398  43738       1,068  391100          119 Hidden Single
 *      107,817,083  34911       3,088   26459        4,074 Point and Claim
 *      121,137,957  22574       5,366    6663       18,180 Naked Pair
 *       90,090,958  20392       4,417    8517       10,577 Hidden Pair
 *      133,328,909  18286       7,291    1620       82,301 Naked Triple
 *      115,189,957  17887       6,439    1102      104,528 Hidden Triple
 *      197,520,833  17672      11,177     764      258,535 XY-Wing
 *       58,826,272  17144       3,431     649       90,641 Swampfish
 *       67,631,290  16892       4,003     317      213,347 Swordfish
 *      184,852,330  16802      11,001     130    1,421,941 Naked Quad
 *       20,438,369  16780       1,218      12    1,703,197 Jellyfish
 *      187,670,393  16776      11,186     365      514,165 XYZ-Wing
 *    1,698,245,523  16435     103,331    1106    1,535,484 Unique Rectangle
 *      194,761,605  15907      12,243      64    3,043,150 Bi-Uni Grave
 *    1,998,883,121  15875     125,913      27   74,032,708 Aligned Pair
 *   26,324,092,870  15848   1,661,035     562   46,840,022 Aligned Triple
 *  137,966,158,983  15342   8,992,710     843  163,660,924 Aligned Quad
 *  216,229,695,438  14607  14,803,155     542  398,947,777 Aligned Pent
 *   52,577,575,759  14115   3,724,943  217432      241,811 Unary Chain
 *    8,329,930,804   7137   1,167,147    2470    3,372,441 Nishio Chain
 *   16,017,181,673   6054   2,645,718   20900      766,372 Multiple Chain
 *   15,863,185,465   1680   9,442,372   15760    1,006,547 Dynamic Chain
 *      151,796,199      3  50,598,733      30    5,059,873 Dynamic Plus
 *  478,700,790,687
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
 *       time (ns)        calls   time/call elim      time/elim  hinter
 *   2,132,186,446        16090     132,516   29     73,523,670  Aligned Pair
 *  29,161,434,276        16061   1,815,667  557     52,354,460  Aligned Triple
 * 152,089,171,294  2:32  15561   9,773,740  821    185,248,686  Aligned Quad
 * 262,133,574,714  4:22  14846  17,656,848  644    407,039,712  Aligned Pent
 * 612,845,635,986 10:12  14264  42,964,500  411  1,491,108,603  Aligned Hex
 * 914,140,777,296 15:14  13907  65,732,420  182  5,022,751,523  Aligned Sept
 * NB: the price goes up, and the returns go down, so dare we try A8E? I think
 * so, but first 3#top1465.d5.mt which took 33,743,216,168 ns. I don't like it!
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-04</b> added A8E.
 * <pre>
 * top1465.d5.mt 2019-10-04.13-14-43
 *         time (ns)  calls   time/call  elim      time/elim  hinter
 *     1,652,508,503  16109     102,582    30     55,083,616 Aligned Pair
 *    27,032,816,722  16079   1,681,249   569     47,509,343 Aligned Triple
 *   146,494,334,180  15567   9,410,569   892    164,231,316 Aligned Quad
 *   226,324,167,577  14788  15,304,582   567    399,160,789 Aligned Pent
 *   530,340,130,297  14274  37,154,275   411  1,290,365,280 Aligned Hex
 *   693,079,616,124  13917  49,800,935   178  3,893,705,708 Aligned Sept
 *   594,076,016,953  13758  43,180,405    81  7,334,271,814 Aligned Oct
 * 2,315,795,801,110 @ 1,587,693,428 ns/puzzle
 * NB: We dared A8E. The price went up, and the returns went down, as expected.
 * 7 seconds per elimination is bonkers! I got 3#top1465.d5.mt (et al) down by
 * putting a time limit on A3E-and-up. In A5E-and-up this limit now reduces the
 * maxCandidates so we don't try again until a candidate is killed elsewhere.
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-04</b> with A8E.
 * <pre>
 *       time (ns)  calls   time/call   elims      time/elim  hinter
 *      33,377,414  46283         721  229270            145  Naked Single
 *      59,125,946  43987       1,344  391270            151  Hidden Single
 *     139,072,377  35131       3,958   26418          5,264  Point and Claim
 *     149,521,562  22810       6,555    6649         22,487  Naked Pair
 *     101,189,990  20625       4,906    8465         11,953  Hidden Pair
 *     146,736,148  18530       7,918    1598         91,824  Naked Triple
 *     122,477,141  18131       6,755    1100        111,342  Hidden Triple
 *     225,913,149  17915      12,610     758        298,038  XY-Wing
 *      70,253,746  17385       4,041     652        107,751  Swampfish
 *      69,815,125  17129       4,075     319        218,856  Swordfish
 *     199,112,164  17038      11,686     129      1,543,505  Naked Quad
 *      20,385,919  17016       1,198      12      1,698,826  Jellyfish
 *     217,955,007  17012      12,811     359        607,117  XYZ-Wing
 *   1,851,736,378  16676     111,041    1110      1,668,230  Unique Rectangle
 *     257,221,110  16148      15,928      62      4,148,727  Bi-Uni Grave
 *   1,844,881,090  16117     114,468      30     61,496,036  Aligned Pair
 *  28,501,469,004  16087   1,771,708     584     48,803,885  Aligned Triple
 * 152,740,937,228  15560   9,816,255     879    173,766,709  Aligned Quad
 * 232,033,151,060  14790  15,688,515     568    408,509,068  Aligned Pent
 * 553,588,140,557  14274  38,782,971     409  1,353,516,236  Aligned Hex
 * 723,794,413,105  13919  52,000,460     180  4,021,080,072  Aligned Sept
 * 599,526,759,080  13759  43,573,425      81  7,401,564,926  Aligned Oct
 *  55,388,539,343  13685   4,047,390  213460        259,479  Unary Chain
 *   8,759,665,370   7025   1,246,927    2462      3,557,946  Nishio Chain
 *  17,360,074,664   5950   2,917,659   20650        840,681  Multiple Chain
 *  17,075,776,084   1664  10,261,884   15747      1,084,382  Dynamic Chain
 *     161,868,013      3  53,956,004      30      5,395,600  Dynamic Plus
 * 2,394,439,567,774 / 1465 = 1,634,429,739 ns/puzzle (1.6 seconds)
 * </pre>
 * <hr><p>
 * <b>KRC 2019-10-08</b> Hack: New -REDO argument to LogicalSolverTester parses
 * a previous logFile to see which hinters to apply to each puzzle.
 * <pre>
 *       time (ns)  calls   time/call   elims    time/elim hinter
 *      25,374,419  46276         548  229310          110 Naked Single
 *      61,138,533  43979       1,390  391240          156 Hidden Single
 *     135,930,629  35123       3,870   26414        5,146 Point and Claim
 *     141,214,807  18571       7,604    6640       21,267 Naked Pair
 *      91,479,466  15664       5,840    8460       10,813 Hidden Pair
 *  66,025,341,862  13526   4,881,364  213456      309,315 Unary Chain
 *     138,149,887   7427      18,601     756      182,737 XY-Wing
 *  83,503,693,374   7417  11,258,418     858   97,323,651 Aligned Quad
 *  21,101,527,017   5912   3,569,270   20650    1,021,865 Multiple Chain
 *      60,950,294   5856      10,408    1598       38,141 Naked Triple
 *   1,048,865,034   5790     181,151    1110      944,923 Unique Rectangle
 *  12,447,607,042   5742   2,167,817     611   20,372,515 Aligned Triple
 * 101,982,425,510   5695  17,907,361     568  179,546,523 Aligned Pent
 *      98,632,746   5178      19,048     359      274,743 XYZ-Wing
 *   7,265,182,790   4926   1,474,864    2462    2,950,927 Nishio Chain
 *      25,070,799   4616       5,431     652       38,452 Swampfish
 * 191,164,018,256   4258  44,895,260     409  467,393,687 Aligned Hex
 *      28,533,093   3206       8,899    1100       25,939 Hidden Triple
 * 148,072,604,302   2068  71,601,839     179  827,221,253 Aligned Sept
 *      14,835,670   1853       8,006     319       46,506 Swordfish
 *  20,543,122,988   1663  12,353,050   15737    1,305,402 Dynamic Chain
 *  55,320,695,749   1149  48,146,819      81  682,971,552 Aligned Oct
 *      13,921,344    550      25,311      62      224,537 Bi-Uni Grave
 *      62,143,905    458     135,685      30    2,071,463 Aligned Pair
 *       5,191,936    308      16,856     129       40,247 Naked Quad
 *         541,274     81       6,682      12       45,106 Jellyfish
 *     154,043,077      3  51,347,692      30    5,134,769 Dynamic Plus
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
 *       time (ns)  calls   time/call   elims    time/elim hinter
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
 *       time (ns)  calls   time/call   elims   time/elim hinter
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
 *      time (ns)  calls   time/call   elims    time/elim hinter
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
 *      time (ns)  calls   time/call   elims   time/elim hinter
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
 *         time (ns)  calls  time/call  elims      time/elim hinter
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
 *         time (ns)  calls  time/call  elims      time/elim hinter
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *        time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 * * NOTES:
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *    see: KRC 2020-02-04 15:03 hacked
 *    and: KRC 2020-02-04 16:50 correct
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
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter              was
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 *          time (ns) calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * AChainer offToOns. First I ran all hacked with IS_HACKY=false.
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
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 * <p>
 * <b>KRC 2020-02-26 morning</b> Ran !IS_HACKY A2345678910_1C just to see how
 * long it takes. Something like 10 or 11 hours is my guess.
 * <p>
 * <b>KRC 2020-02-26 13:26</b> A9E_1C is running like a dog for some reason:
 * <pre>
 * So A9E_1C has taken 19.8 times longer than A8E_1C, and that's just wrong!
 * I still haven't decided wether or not I'm going to kill the run, but I'm
 * leaning towards the affirmative. I'm just going to take a look at the code
 * to see if I've left a breakpoint or some other stupidity in there.
 *
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
 *          time (ns)  calls   time/call  elims      time/elim hinter
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
 *         time (ns)  calls   time/call  elims      time/elim hinter
 *        21,873,700 118471         184 662950             32 Naked Single
 *        20,669,000  52176         396 162540            127 Hidden Single
 *        72,714,600  35922       2,024  26317          2,763 Point and Claim
 *       134,212,700  23606       5,685   6481         20,708 Naked Pair
 *        87,657,500  21466       4,083   8455         10,367 Hidden Pair
 *       140,911,000  19378       7,271   1576         89,410 Naked Triple
 *       119,387,900  18983       6,289   1025        116,476 Hidden Triple
 *       103,525,500  18786       5,510    727        142,400 XY-Wing
 *        66,248,000  18269       3,626    690         96,011 Swampfish
 *        71,458,200  17998       3,970    326        219,196 Swordfish
 *       192,036,500  17908      10,723    128      1,500,285 Naked Quad
 *        21,283,000  17885       1,189     10      2,128,300 Jellyfish
 *        78,273,400  17882       4,377    327        239,368 XYZ-Wing
 *     2,024,953,300  17577     115,204   1136      1,782,529 Unique Rectangle
 *       246,671,200  17040      14,476     62      3,978,567 Bi-Uni Grave
 *     1,623,770,400  17010      95,459     21     77,322,400 Aligned Pair
 *    19,979,498,800  16989   1,176,025    585     34,152,989 Aligned Triple
 *    70,891,158,100  16460   4,306,874   1393     50,890,996 Aligned Quad
 *   388,079,152,700  15220  25,497,973   1366    284,098,940 Aligned Pent
 * 1,018,288,385,400  14019  72,636,306   1083    940,247,816 Aligned Hex
 * 2,180,247,942,200  13096 166,481,974    658  3,313,446,720 Aligned Sept
 * 3,571,428,370,100  12519 285,280,643    442  8,080,154,683 Aligned Oct
 * 4,747,309,451,500  12129 391,401,554    257 18,472,021,212 Aligned Nona
 * 6,533,910,249,500  11904 548,883,589    144 45,374,376,732 Aligned Dec
 *    43,503,916,400  11783   3,692,091  24589      1,769,243 Unary Chain
 *     7,384,768,100   6054   1,219,816   1251      5,903,092 Nishio Chain
 *    14,083,215,400   5055   2,785,997  10228      1,376,927 Multiple Chain
 *    14,788,112,800   1541   9,596,439   9683      1,527,224 Dynamic Chain
 *       135,967,800      3  45,322,600     30      4,532,260 Dynamic Plus
 *18,615,055,834,700
 * pzls          total (ns) (mmm:ss) (h:mm:ss)        each (ns)
 * 1465  18,637,189,690,500 (310:37) (5:10:37)  12,721,631,188
 * NOTES:
 * 1. So !IS_HACKY top1465.d5.mt with A2345678910E_1C in 310:37 or 5:10:37
 *    is 8 minutes faster than previous. No miracles here.
 * 2. Next we run IS_HACKY again with "HIT" puzzle numbers from lastnights run,
 *    then I guess we build, release, and backup.
 * </pre>
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *         time (ns)  calls  time/call  elims   time/elim hinter
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
 *
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
 *         time (ns)  calls  time/call  elims     time/elim hinter
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
 *
 * NOTES:
 * 1. IS_HACKY top1465 A5678910E_1C in 3:03 was 53:30 = 50 minutes faster.
 * 2. Now I release 6.30.054 2020-03-05 21:46:54
 *    => DiufSudoku_V6_30.054.2020-03-05.OK.7z
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 2020-03-06 21:59:55</b> Stuck a WindexSet in each A*E.
 * <pre>
 *         time (ns)  calls  time/call  elims   time/elim hinter
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
 *
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
 *         time (ns)  calls     time/call  elims       time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *
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
 *      time (ns)  calls  time/call  elims time/elim hinter
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
 *
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 *        time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *        time (ns)  calls  time/call  elims     time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 * KRC 6.30.078 2020-07-20 11:42:18 Forgive me father it's been 10 days since
 * my last release and I just need to rub one out. Actually I need to delete my
 * log-files and I want a backup before I do so. I've been pissin about trying
 * to make HoDoKu faster, with some (but not much) success. The main snags are
 * repeated calls to "heavy" getters and Integer.compareTo(Integer). Sigh.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.079 2020-07-21 12:26:19 profile to expedite getAllKrakenFishes->
 * TablingSolver.buildChain-> TreeMap<Int,Int>-> Int.of -> CACHE_SIZE
 * (4K -> 75% hit rate) but now there's s__tloads of logs so I release to back
 * and clean them up.
 * <pre>
 * <b>BROKEN:</b> I discovered that my desktop "release" SudokuExplainer no
 *			longer runs (HoDoKu ClassNotFound) so I only run in situ now.
 *			I need a real Java programmer to workout what's happened to
 *			the classpath. I just blame Nutbeans! Weird dependancy s__t!
 * </pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * <pre>
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
 * KRC 6.30.080 2020-08-01 16:34:20 I broke HoDoKu so I had to revert it.
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
 * </pre>
 *         time (ns)  calls  time/call  elims   time/elim hinter
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
 * <pre>
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
 * KRC 6.30.081 2020-08-06 22:26:21 There is no try. Been pissin about trying
 * to make FishSolver faster, then tried implementing FrankenFisherman myself
 * and it totally sucks bags. It's slow and doesn't work and I feel pretty
 * stupid right now. But there was a nice tender lesso kiss on the tele and
 * that's always nice. So maybe I'll have another go at it tomorrow.
 * <pre>
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 * KRC 6.30.082 2020-08-10 21:42:22 Implemented HdkColoring. Made
 * HdkFisherman marginally faster. Standardised caching in the HintsList.
 * Dropped Nishio Chainer which took 4 seconds to produce 1 hint, and the
 * Franken Swordfish which took 21 seconds to produce 9 hints.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.083 2020-08-21 10:08:83 I went through the first 1000 puzzles in
 * the GUI looking for bugs, weak hint explanations and the like. Fixed a few
 * things like painting fins blue, and bug in Locking mergeSiameseHints.
 * This run has siamese checks switched on, so Locking actually finds
 * MORE eliminations even though it says it finds less because it adds elims to
 * HiddenSet hints, but keeps the search time. Not ideal.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.084 2020-08-21 20:37:24 Discovered a bug in NakedSet, it was
 * falsely returning false after finding hints. Doh! How embarrassment!
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.085 2020-08-31 17:39:25 Simplified colors by adding a getOrange
 * independent of getGreens and getReds. Fixed all test cases.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.087 2020-09-03 08:05:27 Fixed logView. Previous release is a bust.
 * Bugs in the load process makes it not work at all; so I must release patch
 * ASAP. I fixed the logView, which is what I thought I had done in the previous
 * release, which was rushed out because I don't control when we go to town. I
 * should NOT have released untested. That was a BIG mistake and I won't make it
 * again. Sigh. logView now takes a Java regex (was just a plain string).
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.088 2020-09-07 11:20:28 Release coz I'm at the library and I can.
 * No notable changes to the best of my recollection.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.089 2020-09-12 11:55:29 Bug fix: Subsequent views not shown for
 * RegionReductionHint and CellReductionHint.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.090 2020-09-24 13:30:30 Boosted ColoringSolver as color.Coloring.
 * Renamed package diuf.sudoku.solver.hinters.alshdk to als.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.091 2020-10-06 18:06:31 Boost HoDoKuFisherman does Finned, Franken,
 * and Mutant fish. It can do Basic but doesn't coz Fisherman is faster. It's
 * still too slow for Mutant (not really useable). Stripped Kraken from hobiwan
 * code, which I plan to put in a KrakenFisherman class.
 * <pre>
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 * KRC 6.30.092 2020-10-11 18:46:32 Boost Kraken Fish from HoDoKuFisherman into
 * KrakenFisherman. It works, but it's so slow it won't even fit on one line.
 * <pre>
 *         time (ns)  calls     time/call  elims       time/elim hinter
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
 * KRC 6.30.093 2020-10-14 18:03:33 New version of KrakenFisherman is twice as
 * fast as original, so now it's just very very slow.
 * <pre>
 *         time (ns)  calls   time/call  elims     time/elim hinter
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
 * KRC 6.30.094 2020-10-15 15:37:34 Replaced the "interleaved" getRegions
 * method in AHint and all subtypes (30 of them) with getBase and getCovers.
 * This change should have no consequence to non-developers, but to a techie
 * combining two different types of things into one array was MENTAL. So now
 * the code is not quite so mental. I probably haven't eradicated all of the
 * helper methods which supported interleaving and dis-interleaving regions.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.095 2020-10-17 22:25:35 test-cases for Idx, HdkSet and HdkSetBase.
 * I've given-up on cleaning-up "the mess" I've made out of ALSs package, to
 * get rid of the HoDoKu types, especially HdkAls; I've tried 4 times and it
 * just fails like a bastard, so I feel totally inadequate, but none the less
 * I have given-up on it. So it's past time to ship it again.
 * <pre>
 *      time (ns)  calls  time/call  elims time/elim hinter
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
 * KRC 2020-10-18 06:28 I ran it again with the big fish overnight.
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
 *         time (ns)  calls     time/call  elims       time/elim hinter
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
 * KRC 2020-10-19 21:14 I finally found my bug in eradicating the HoDoKu types
 * from the als package; so they're gone now, except HdkAls becomes Als and the
 * DiufAls class is history.
 * <pre>
 *       time (ns)  calls   time/call  elims     time/elim hinter
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
 * KRC 6.30.098 2020-10-23 23:20:39 I went mad and cleaned-up. There should be
 * no visible changes, but LOTS of dead and dying code has been removed. The
 * only notable for users is that the Grid constructor now just calls load,
 * instead of doing it all itself the pernickety way; so the Grid constructor
 * now loads an invalid grid, doesn't "fix" it, and doesn't complain; just does
 * the best it can with what it's given. I'm a bit proud of Idx.untilFalse: I
 * worked-out how to early-exit from a forEach.
 * <pre>
 *       time (ns)  calls  time/call  elims  time/elim hinter
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
 * KRC 2020-10-24 06:10 Here's da hinter summary of the overnight Big Fish run.
 * <pre>
 *         time (ns)  calls     time/call  elims       time/elim hinter
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
 * KRC 2020-10-26 FYI I ran HoDoKu over top1465.d5.mt
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
 * KRC 2020-11-29 08:45 This is a redo of the above BIG FISH run, with lessons
 * from a month of performance tuning HoDoKu, especially the caching in
 * {@link diuf.sudoku.solver.hinters.fish.KrakenFisherman#kt2Search}, plus bug
 * fixes from generate.
 * <pre>
 *         time (ns)  calls   time/call  elims       time/elim hinter
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
 * KRC 2020-11-30 07:01 Disabled mutant hinters by commenting out Tech's.
 * <pre>
         time (ns)	  calls	     time/call	  elims	     time/elim	hinter
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
 * KRC 2020-12-05 10:05:40 Polished KrakenFisherman. Added nonHinters filter to
 * A5+E; which is what I SHOULD have done instead of all those months trying to
 * work-out how to do it faster, so the ideal implementation now would be an
 * A*E class implementing A5678910E, and another simpler one for A234E. The key
 * to this is the IntIntHashMap I wrote originally for HoDoKu.
 * <pre>
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 * KRC 6.30.101 2020-12-08 07:58:40 Back-up prior to Aligned*Exclusion redo.
 * This version has NonHinters64 and LongLongHashMap. Now I will try iterating
 * a stack in A*E (ala hobiwan) to remove most of the A*E classes, ie eliminate
 * my previous code-bloat.
 * <pre>
 *       time (ns)  calls   time/call  elims      time/elim hinter
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
 * <hr>
 * <p>
 * KRC 6.30.102 2020-12-17 10:54:42 Release align2.AlignedExclusion squashing
 * all Aligned*Exclusion's boiler-plate code down into one complex succinct
 * class using hobiwans stack iteration technique twice: once for cells, and
 * again for vals.
 * <pre>
 * -- "old version" for comparison --
 *         time (ns)  calls  time/call  elims      time/elim hinter
 *        19,812,600 116122        170 666010             29 Naked Single
 *        14,401,500  49521        290 168620             85 Hidden Single
 *        91,215,500  32659      2,792   5847         15,600 Direct Naked Pair
 *        82,771,900  32248      2,566  12324          6,716 Direct Hidden Pair
 *       203,858,400  31307      6,511    884        230,609 Direct Naked Triple
 *       186,843,600  31252      5,978   1812        103,114 Direct Hidden Triple
 *        99,576,700  31114      3,200  18527          5,374 Locking
 *        52,314,200  21962      2,382   4410         11,862 Naked Pair
 *        44,350,400  20848      2,127   8527          5,201 Hidden Pair
 *       107,202,800  19233      5,573   1307         82,022 Naked Triple
 *        98,394,600  18863      5,216   1028         95,714 Hidden Triple
 *        62,941,000  18664      3,372   1401         44,925 Two String Kite
 *        39,629,400  17263      2,295    448         88,458 Swampfish
 *        91,374,700  17061      5,355    620        147,378 XY-Wing
 *        63,655,100  16616      3,830    303        210,082 XYZ-Wing
 *        68,472,200  16331      4,192    466        146,936 W-Wing
 *        50,511,500  16018      3,153    351        143,907 Skyscraper
 *        56,370,200  15833      3,560    480        117,437 Empty Rectangle
 *        68,918,700  15353      4,488    259        266,095 Swordfish
 *       175,257,700  15277     11,471     98      1,788,343 Naked Quad
 *       140,974,500  15258      9,239     13     10,844,192 Hidden Quad
 *        19,857,200  15256      1,301      9      2,206,355 Jellyfish
 *     1,637,667,900  15253    107,366    860      1,904,265 Unique Rectangle
 *     2,650,147,700  14850    178,461    506      5,237,446 Finned Swampfish
 *     6,019,139,500  14423    417,329    429     14,030,628 Finned Swordfish
 *     7,223,144,500  14076    513,153     21    343,959,261 Finned Jellyfish
 *       456,780,500  14059     32,490    108      4,229,449 Coloring
 *    22,284,415,200  13975  1,594,591   8358      2,666,237 ALS-XZ
 *    22,331,816,900   8276  2,698,382   4329      5,158,654 ALS-Wing
 *     3,747,979,500   4673    802,049      0              0 Franken Swampfish
 *    18,170,867,300   4673  3,888,480    178    102,083,524 Franken Swordfish
 *    44,356,454,800   4524  9,804,698    103    430,645,192 Franken Jellyfish
 *       282,292,600   4430     63,722      1    282,292,600 Aligned Pair
 *     2,100,666,900   4429    474,298     16    131,291,681 Aligned Triple
 *    31,186,149,200   4414  7,065,280     52    599,733,638 Aligned Quad
 *    69,763,009,900   4372 15,956,772     68  1,025,926,616 Aligned Pent
 *    21,958,348,900   4315  5,088,840      1 21,958,348,900 Aligned Hex
 *    20,710,004,900   4314  4,800,650      5  4,142,000,980 Aligned Sept
 *    16,488,024,100   4310  3,825,527      1 16,488,024,100 Aligned Oct
 *    17,186,799,700   4309  3,988,581      0              0 Aligned Nona
 *    12,924,583,400   4309  2,999,439      0              0 Aligned Dec
 *    13,416,250,900   4309  3,113,541   2179      6,157,067 Unary Chain
 *     5,148,660,700   3510  1,466,854     28    183,880,739 Nishio Chain
 *     9,284,748,300   3482  2,666,498   5716      1,624,343 Multiple Chain
 *    11,527,092,500   1447  7,966,200   7942      1,451,409 Dynamic Chain
 *       115,788,600      3 38,596,200     30      3,859,620 Dynamic Plus
 *   362,809,538,800
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  416,922,969,300 (06:56)  284,589,057
 * -- new version --
 *         time (ns)  calls  time/call  elims      time/elim hinter
 *        22,967,700 116122        197 666010             34 Naked Single
 *        18,510,400  49521        373 168620            109 Hidden Single
 *       106,958,800  32659      3,275   5847         18,292 Direct Naked Pair
 *        90,391,900  32248      2,803  12324          7,334 Direct Hidden Pair
 *       229,001,100  31307      7,314    884        259,051 Direct Naked Triple
 *       198,434,500  31252      6,349   1812        109,511 Direct Hidden Triple
 *       103,989,600  31114      3,342  18527          5,612 Locking
 *        59,913,100  21962      2,728   4410         13,585 Naked Pair
 *        51,542,700  20848      2,472   8527          6,044 Hidden Pair
 *       129,033,200  19233      6,708   1307         98,724 Naked Triple
 *       106,144,500  18863      5,627   1028        103,253 Hidden Triple
 *        66,694,300  18664      3,573   1401         47,604 Two String Kite
 *        44,284,300  17263      2,565    448         98,848 Swampfish
 *       100,668,900  17061      5,900    620        162,369 XY-Wing
 *        69,403,900  16616      4,176    303        229,055 XYZ-Wing
 *        67,785,700  16331      4,150    466        145,462 W-Wing
 *        51,487,500  16018      3,214    351        146,688 Skyscraper
 *        60,154,000  15833      3,799    480        125,320 Empty Rectangle
 *        75,379,300  15353      4,909    259        291,039 Swordfish
 *       204,867,900  15277     13,410     98      2,090,488 Naked Quad
 *       153,609,200  15258     10,067     13     11,816,092 Hidden Quad
 *        21,898,600  15256      1,435      9      2,433,177 Jellyfish
 *     1,695,816,000  15253    111,179    860      1,971,879 Unique Rectangle
 *     2,988,384,000  14850    201,237    506      5,905,897 Finned Swampfish
 *     6,838,824,700  14423    474,161    429     15,941,316 Finned Swordfish
 *     8,068,908,900  14076    573,238     21    384,233,757 Finned Jellyfish
 *       466,421,300  14059     33,175    108      4,318,715 Coloring
 *    24,684,311,300  13975  1,766,319   8358      2,953,375 ALS-XZ
 *    25,874,960,300   8276  3,126,505   4329      5,977,121 ALS-Wing
 *     4,025,086,900   4673    861,349      0              0 Franken Swampfish
 *    19,970,723,300   4673  4,273,640    178    112,195,074 Franken Swordfish
 *    49,605,385,800   4524 10,964,939    103    481,605,687 Franken Jellyfish
 *       356,226,000   4430     80,412      1    356,226,000 Aligned Pair
 *     2,371,181,100   4429    535,376     15    158,078,740 Aligned Triple
 *    56,179,909,200   4415 12,724,781     53  1,059,998,286 Aligned Quad
 *   244,482,183,100   4372 55,919,986     66  3,704,275,501 Aligned Pent
 *    38,714,087,800   4316  8,969,899      3 12,904,695,933 Aligned Hex
 *    52,396,760,600   4314 12,145,748      5 10,479,352,120 Aligned Sept
 *    53,700,368,200   4310 12,459,482      1 53,700,368,200 Aligned Oct
 *    41,702,234,300   4309  9,677,937      0              0 Aligned Nona
 *    26,156,226,600   4309  6,070,138      0              0 Aligned Dec
 *    15,658,818,500   4309  3,633,979   2179      7,186,240 Unary Chain
 *     5,775,262,500   3510  1,645,373     28    206,259,375 Nishio Chain
 *    10,395,368,500   3482  2,985,459   5716      1,818,643 Multiple Chain
 *    12,421,114,000   1447  8,584,045   7942      1,563,978 Dynamic Chain
 *        99,803,900      3 33,267,966     30      3,326,796 Dynamic Plus
 *   706,661,487,900
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
 * KRC 6.30.103 2021-01-01 15:17:43 Release "wing2" package with WXYZWing,
 * UVWXYZWing, and TUVWXYZWing boosted from Sukaku, by Nicolas Juillerat (the
 * original authors rewrite of Sudoku Explainer) who boosted them from
 * SudokuMonster.
 * <pre>
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 * KRC NO-RELEASE 2021-01-03 18:05 I just ran EVERYthing to see how slow it was
 * and got a pleasant surprise 21:45. Not too shabby, considering A10E used to 
 * take over a day on it's own. Although it looks like Franken Fish is rooted!
 * <pre>
 *         time (ns)  calls   time/call  elims      time/elim hinter
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
 * KRC 6.30.104 2021-01-06 19:06:44 I'm releasing just to clean-up the logs.
 * I've implemented SueDeCoq in the als package, and also tried but failed at
 * DeathBlossom. 
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.105 2021-01-12 10:05:45 Ship SueDeCoq. Failed at DeathBlossom.
 * This Run: hinters which take less than a tenth of a second per elimination.
 * <pre>
 *       time (ns)  calls  time/call  elims  time/elim hinter
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
 * KRC 6.30.106 2021-01-14 09:02:46 Ship DeathBlossom. Last top1465 4:10 is a
 * minute slower, but that's still OK coz I have no love for DeathBlossom, so
 * I won't be using it.
 * <pre>
 *       time (ns)  calls  time/call  elims     time/elim hinter
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
 * KRC 6.30.107 2021-01-15 09:44:47 ship DeathBlossom today; now faster, and
 * with a better test-case.
 * <pre>
 *       time (ns)  calls  time/call  elims   time/elim hinter
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
 * KRC 6.30.108 2021-01-23 14:40:48 Faster align2.AlignedExclusion, which still
 * isn't competitive with the old align.Aligned*Exclusion classes but it's
 * getting there, and it's just SOOOO much more succinct.
 * <pre>
 * This is an oldish log (ran 2021-01-23.13-25-10) coz Im in a hurry to package
 * this release to get to the library/shops before closing; and I tried running
 * it again AFTER testing the GUI (which was fine) and its runs like a dog, but
 * it's a hot afternoon so Im blaming HOT BOX SYNDROME, and releasing anyway.
 * SOLVED: LogicalAnalyserTest hung -> registry ALL wantedHinters.
 *
 *       time (ns)  calls  time/call  elims      time/elim hinter
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
 * KRC 6.30.109 2021-02-13 07:00:49 BigWings, faster Complex and Kraken fish.
 * Wrote the BigWing class to succinctify it. Tried and mostly failed to make
 * Complex and KrakenFisherman faster.
 * <pre>
 *         time (ns)  calls   time/call  elims       time/elim hinter
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
 */
final class LogicalSolverTimings {

	// never called
	private LogicalSolverTimings(){}

}
