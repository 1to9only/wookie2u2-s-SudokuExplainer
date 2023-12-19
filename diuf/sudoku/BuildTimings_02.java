/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

/**
 * Build comments continued.
 * <hr>
 * <p>
 * <b>KRC 6.30.225 2023-07-20</b> Split Naked/HiddenSet into Pair/TripleQuad to
 * read the permutation efficiently (one statement), which is only possible
 * when you know at compile-time how large each permutation is. There must be
 * other changes, but I forget what they are now. Like faster chainers and
 * stuff. Yeah, I am pretty sure I made the chainers faster again. And Als* got
 * more work on the finder. getAlss now knows nothing of NakedSets, because
 * only DeathBlossom has a problem with them, so it filters them out itself.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     22,564,000 107969       208  57054        395 NakedSingle
 *     37,294,200  50915       732  16437      2,268 HiddenSingle
 *     77,154,900  34478     2,237  24100      3,201 Locking
 *     75,218,400  23147     3,249   7802      9,640 NakedPair
 *     63,083,600  21117     2,987   8216      7,678 HiddenPair
 *    120,762,500  19040     6,342   1570     76,918 NakedTriple
 *     99,077,900  18650     5,312    991     99,977 HiddenTriple
 *     57,917,700  18452     3,138   1308     44,279 TwoStringKite
 *     49,100,800  17144     2,864    412    119,176 Swampfish
 *     55,940,900  16968     3,296    919     60,871 W_Wing
 *     85,586,100  16337     5,238    521    164,272 XY_Wing
 *     48,523,600  15990     3,034    325    149,303 Skyscraper
 *    102,374,400  15821     6,470    578    177,118 EmptyRectangle
 *     57,336,400  15243     3,761    264    217,183 XYZ_Wing
 *     61,514,700  14995     4,102    229    268,623 Swordfish
 *    162,026,200  14931    10,851     98  1,653,328 NakedQuad
 *     86,837,900  14912     5,823     16  5,427,368 Jellyfish
 *    120,382,200  14909     8,074      4 30,095,550 HiddenQuad
 *    320,414,900  14907    21,494    184  1,741,385 Coloring
 *    899,724,400  14785    60,853    793  1,134,583 XColoring
 *  1,967,447,700  14528   135,424 127686     15,408 GEM
 *  1,302,030,000  13172    98,848    894  1,456,409 URT
 *  3,786,867,100  12536   302,079   2154  1,758,062 BigWings
 *  4,877,487,800  11338   430,189   2715  1,796,496 DeathBlossom
 *  7,811,256,400   9025   865,513   2041  3,827,171 ALS_XZ
 *  9,408,304,200   7770 1,210,849   2762  3,406,337 ALS_Wing
 * 14,706,311,000   5569 2,640,745   2013  7,305,668 ALS_Chain
 *  1,119,183,000   3988   280,637     15 74,612,200 SueDeCoq
 * 17,112,536,100   3985 4,294,237    324 52,816,469 FrankenSwordfish
 *  7,444,647,400   3732 1,994,814    349 21,331,367 UnaryChain
 *  5,890,751,700   3457 1,704,006     95 62,007,912 NishioChain
 *  7,228,970,200   3362 2,150,199   5097  1,418,279 DynamicChain
 *     69,095,700     11 6,281,427     11  6,281,427 DynamicPlus
 * 85,327,724,000
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  133,978,916,200 (02:13)  91,453,185
 *
 * NOTES:
 * 1. 2:13 is 2 seconds slower. GAGF! Sigh. Its faster because ALS_Chain is
 *    slower because its searching alss of 7 cells again. 6 was faster, and
 *    still is, but I am just not that worried about performance any longer,
 *    anything under about 5 minutes feels pretty snappy in the GUI.
 * 2. DiufSudoku_V6_30.225.2023-07-20.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.226 2023-07-26</b> I continued trying to speed things up. The
 * AlsFinder is now just a class. AAlsFinder and AlsFinderRecursive got cut.
 * Reverted to standard VALUESES loops coz bit-twiddling was slower.
 * <pre>
 *          time(ns)  calls time/call  elims  time/elim hinter
 *        13,552,000 108097       125  57108        237 NakedSingle
 *        26,217,700  50989       514  16459      1,592 HiddenSingle
 *        71,246,700  34530     2,063  24102      2,956 Locking
 *        66,614,100  23193     2,872   7820      8,518 NakedPair
 *        57,001,400  21158     2,694   8221      6,933 HiddenPair
 *       115,180,800  19076     6,037   1557     73,976 NakedTriple
 *        97,161,900  18688     5,199    989     98,242 HiddenTriple
 *        47,255,200  18490     2,555   1311     36,045 TwoStringKite
 *        43,413,400  17179     2,527    411    105,628 Swampfish
 *        54,091,200  17004     3,181    917     58,987 W_Wing
 *        79,327,500  16374     4,844    523    151,677 XY_Wing
 *        47,374,000  16027     2,955    325    145,766 Skyscraper
 *        93,428,300  15858     5,891    577    161,920 EmptyRectangle
 *        53,173,200  15281     3,479    263    202,179 XYZ_Wing
 *        55,973,400  15034     3,723    230    243,362 Swordfish
 *       159,715,600  14969    10,669     98  1,629,751 NakedQuad
 *        78,616,200  14950     5,258     16  4,913,512 Jellyfish
 *       123,096,800  14947     8,235      4 30,774,200 HiddenQuad
 *       279,803,200  14945    18,722    192  1,457,308 Coloring
 *       839,478,600  14823    56,633    794  1,057,277 XColoring
 *     1,934,590,500  14565   132,824 126908     15,244 GEM
 *     1,485,966,000  13208   112,504    894  1,662,154 URT
 *     4,187,887,700  12569   333,191   2168  1,931,682 BigWings
 *     5,485,626,000  11363   482,762   2763  1,985,387 DeathBlossom
 *     9,064,502,800   9013 1,005,714   2029  4,467,473 ALS_XZ
 *     7,807,996,800   7757 1,006,574   2775  2,813,692 ALS_Wing
 *    16,485,582,000   5547 2,971,981   1995  8,263,449 ALS_Chain
 *     1,204,223,900   3973   303,101     15 80,281,593 SueDeCoq
 *    13,189,308,700   3970 3,322,244    326 40,458,002 FrankenSwordfish
 *     5,823,101,600   3721 1,564,929    337 17,279,233 UnaryChain
 *     3,894,842,300   3449 1,129,267     95 40,998,340 NishioChain
 *     5,151,192,200   3354 1,535,835   5107  1,008,653 DynamicChain
 *        65,914,800     11 5,992,254     11  5,992,254 DynamicPlus
 *    78,182,456,500
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  98,508,447,500 (01:38)  67,241,261
 *
 * NOTES:
 * 1. 1:38 is 25 seconds faster, but keep your pants on; it ran in the Profiler
 *    and its faster there, even with all the profiling overheads. BFIIK why.
 * 2. DiufSudoku_V6_30.226.2023-07-26.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.227 2023-07-30</b> De-optimised RccFinders because the extra
 * complexity wasnt worth four seconds. Anything under about 5 minutes feels
 * snappy enough in the GUI, which now has a hint caching switch. Generate also
 * updates hints-cache. Sigh.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     22,815,458 108100       211  57108        399 NakedSingle
 *     37,662,809  50992       738  16459      2,288 HiddenSingle
 *     79,456,989  34533     2,300  24103      3,296 Locking
 *     72,380,614  23196     3,120   7820      9,255 NakedPair
 *     62,920,254  21161     2,973   8221      7,653 HiddenPair
 *    121,158,309  19079     6,350   1557     77,815 NakedTriple
 *    100,919,581  18691     5,399    989    102,042 HiddenTriple
 *     65,778,784  18493     3,556   1311     50,174 TwoStringKite
 *     50,012,823  17182     2,910    411    121,685 Swampfish
 *     57,014,527  17007     3,352    917     62,175 W_Wing
 *     82,816,813  16377     5,056    523    158,349 XY_Wing
 *     52,043,647  16030     3,246    325    160,134 Skyscraper
 *    104,191,225  15861     6,569    577    180,574 EmptyRectangle
 *     57,133,673  15284     3,738    263    217,238 XYZ_Wing
 *     62,672,636  15037     4,167    230    272,489 Swordfish
 *    162,829,509  14972    10,875     98  1,661,525 NakedQuad
 *     87,809,741  14953     5,872     16  5,488,108 Jellyfish
 *    123,217,172  14950     8,241      4 30,804,293 HiddenQuad
 *    326,397,341  14948    21,835    192  1,699,986 Coloring
 *    895,151,969  14826    60,377    794  1,127,395 XColoring
 *  1,979,055,028  14568   135,849 126908     15,594 GEM
 *  1,327,379,088  13211   100,475    894  1,484,764 URT
 *  4,080,864,858  12572   324,599   2168  1,882,317 BigWings
 *  5,675,409,395  11366   499,332   2763  2,054,075 DeathBlossom
 * 10,258,217,312   9016 1,137,779   2029  5,055,799 ALS_XZ
 * 11,039,030,321   7760 1,422,555   2777  3,975,163 ALS_Wing
 * 18,553,223,707   5549 3,343,525   1995  9,299,861 ALS_Chain
 *  1,360,952,118   3975   342,377     15 90,730,141 SueDeCoq
 * 14,420,823,042   3972 3,630,620    322 44,785,164 FrankenSwordfish
 *  6,640,535,180   3721 1,784,610    337 19,704,852 UnaryChain
 *  6,110,175,540   3449 1,771,578     95 64,317,637 NishioChain
 *  7,130,100,677   3354 2,125,849   5107  1,396,142 DynamicChain
 *     79,153,802     11 7,195,800     11  7,195,800 DynamicPlus
 * 91,279,303,942
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  137,584,726,099 (02:17)  93,914,488
 *
 * NOTES:
 * 1. 2:17 is about par. De-optimised so I expected it to be a bit slower.
 * 2. DiufSudoku_V6_30.227.2023-07-30.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.228 2023-08-05</b> Still pissing about trying to make the slow
 * hinters faster. This time I have been elbows deep in RccFinders ass. Its
 * still not pregnant. Sigh. I modified AlsChain, making it slower. EPIC FAIL!
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     23,258,900 108105       215  57135        407 NakedSingle
 *     37,137,300  50970       728  16446      2,258 HiddenSingle
 *     77,742,600  34524     2,251  24121      3,223 Locking
 *     77,151,900  23179     3,328   7820      9,865 NakedPair
 *     63,189,800  21145     2,988   8233      7,675 HiddenPair
 *    123,144,700  19060     6,460   1557     79,091 NakedTriple
 *    100,825,100  18672     5,399    989    101,946 HiddenTriple
 *     59,680,400  18474     3,230   1310     45,557 TwoStringKite
 *     53,961,700  17164     3,143    411    131,293 Swampfish
 *     59,489,200  16989     3,501    916     64,944 W_Wing
 *     87,891,100  16360     5,372    520    169,021 XY_Wing
 *     52,598,900  16014     3,284    325    161,842 Skyscraper
 *    105,709,200  15845     6,671    577    183,204 EmptyRectangle
 *     57,809,700  15268     3,786    262    220,647 XYZ_Wing
 *     63,947,200  15022     4,256    230    278,031 Swordfish
 *    165,862,000  14957    11,089     98  1,692,469 NakedQuad
 *     89,668,800  14938     6,002     16  5,604,300 Jellyfish
 *    125,136,900  14935     8,378      4 31,284,225 HiddenQuad
 *    320,933,700  14933    21,491    192  1,671,529 Coloring
 *    908,158,900  14811    61,316    794  1,143,776 XColoring
 *  1,991,742,400  14553   136,861 126760     15,712 GEM
 *  1,309,372,900  13199    99,202    894  1,464,622 URT
 *  4,165,884,000  12560   331,678   2167  1,922,419 BigWings
 *  5,723,196,400  11355   504,024   2752  2,079,649 DeathBlossom
 *  9,622,724,700   9013 1,067,649   2025  4,751,962 ALS_XZ
 * 11,170,770,500   7760 1,439,532   2773  4,028,406 ALS_Wing
 * 19,877,426,200   5552 3,580,228   2001  9,933,746 ALS_Chain
 *  1,377,968,500   3977   346,484     15 91,864,566 SueDeCoq
 * 12,789,015,600   3974 3,218,172    322 39,717,439 FrankenSwordfish
 *  6,761,588,300   3723 1,816,166    337 20,064,060 UnaryChain
 *  6,107,550,500   3451 1,769,791     95 64,290,005 NishioChain
 *  7,084,989,600   3356 2,111,141   5109  1,386,766 DynamicChain
 *     70,038,700     11 6,367,154     11  6,367,154 DynamicPlus
 * 90,705,566,300
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  136,544,464,700 (02:16)  93,204,412
 *
 * NOTES:
 * 1. 2:16 is about par. I do wish it was faster though. Sigh.
 * 2. DiufSudoku_V6_30.228.2023-08-05.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.229 2023-08-08</b> Dropped IALease. I used IALease in
 * {@link diuf.sudoku.solver.hinters.als.AlsChain#kids} but it was slower,
 * which undermines IALease. So now I just use "normal" new arrays instead.
 * Reduced AAlsChain.MAX_ALS_SIZE from 8 to 5, to cap als-hinters.degree at 5;
 * so we no longer search ST, just UVWXYZ_Wings (6 values in 5 cells). This is
 * significantly faster: the only way I know how to make AlsChain faster.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     23,992,000 108258       221  57357        418 NakedSingle
 *     38,272,900  50901       751  16427      2,329 HiddenSingle
 *     77,192,100  34474     2,239  24148      3,196 Locking
 *     75,756,900  23141     3,273   7795      9,718 NakedPair
 *     62,261,500  21102     2,950   8259      7,538 HiddenPair
 *    119,676,600  19016     6,293   1578     75,840 NakedTriple
 *    102,093,900  18621     5,482    985    103,648 HiddenTriple
 *     58,601,600  18425     3,180   1304     44,939 TwoStringKite
 *     49,563,900  17121     2,894    426    116,347 Swampfish
 *     55,881,600  16942     3,298    892     62,647 W_Wing
 *     82,722,300  16328     5,066    529    156,374 XY_Wing
 *     49,063,300  15974     3,071    327    150,040 Skyscraper
 *    103,064,400  15804     6,521    582    177,086 EmptyRectangle
 *     56,196,400  15222     3,691    263    213,674 XYZ_Wing
 *     61,540,000  14977     4,108    223    275,964 Swordfish
 *    156,425,100  14916    10,487     97  1,612,629 NakedQuad
 *     87,484,400  14896     5,873     16  5,467,775 Jellyfish
 *    119,452,300  14893     8,020      4 29,863,075 HiddenQuad
 *    290,526,900  14891    19,510    178  1,632,173 Coloring
 *    911,677,400  14769    61,729    797  1,143,886 XColoring
 *  1,941,614,000  14505   133,858 124593     15,583 GEM
 *  1,310,031,600  13156    99,576    884  1,481,936 URT
 *  3,177,722,100  12527   253,669   2113  1,503,891 BigWings
 *  3,268,673,600  11353   287,912   2538  1,287,893 DeathBlossom
 *  5,019,269,400   9213   544,802   2048  2,450,815 ALS_XZ
 *  6,039,685,400   7953   759,422   2605  2,318,497 ALS_Wing
 *  9,938,562,000   5887 1,688,221   2106  4,719,165 ALS_Chain
 *  1,316,181,500   4237   310,639     26 50,622,365 SueDeCoq
 * 13,606,069,500   4232 3,215,044    322 42,254,874 FrankenSwordfish
 *  7,378,377,500   3979 1,854,329    431 17,119,205 UnaryChain
 *  6,478,867,600   3615 1,792,217     97 66,792,449 NishioChain
 *  7,424,058,300   3518 2,110,306   5397  1,375,589 DynamicChain
 *     72,393,800     11 6,581,254     11  6,581,254 DynamicPlus
 * 69,552,951,800
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  116,395,477,400 (01:56)  79,450,837
 *
 * NOTES:
 * 1. 1:56 is 20 seconds faster. She will do.
 * 2. DiufSudoku_V6_30.229.2023-08-08.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.229 2023-08-10</b> Delay set creation in chainers until first
 * call to findChains, and replace IAfter with ICleanUp (same s__t).
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     22,768,700 108258       210  57357        396 NakedSingle
 *     37,547,100  50901       737  16427      2,285 HiddenSingle
 *     77,821,000  34474     2,257  24148      3,222 Locking
 *     75,125,700  23141     3,246   7795      9,637 NakedPair
 *     61,343,200  21102     2,906   8259      7,427 HiddenPair
 *    120,909,100  19016     6,358   1578     76,621 NakedTriple
 *     98,370,200  18621     5,282    985     99,868 HiddenTriple
 *     56,937,500  18425     3,090   1304     43,663 TwoStringKite
 *     51,464,700  17121     3,005    426    120,809 Swampfish
 *     56,737,100  16942     3,348    892     63,606 W_Wing
 *     76,791,000  16328     4,703    529    145,162 XY_Wing
 *     47,330,400  15974     2,962    327    144,741 Skyscraper
 *    105,449,700  15804     6,672    582    181,185 EmptyRectangle
 *     47,452,700  15222     3,117    263    180,428 XYZ_Wing
 *     64,455,300  14977     4,303    223    289,037 Swordfish
 *    162,762,000  14916    10,911     97  1,677,958 NakedQuad
 *     89,205,000  14896     5,988     16  5,575,312 Jellyfish
 *    123,380,500  14893     8,284      4 30,845,125 HiddenQuad
 *    298,307,900  14891    20,032    178  1,675,887 Coloring
 *    908,693,200  14769    61,527    797  1,140,142 XColoring
 *  1,955,814,000  14505   134,837 124593     15,697 GEM
 *  1,296,356,900  13156    98,537    884  1,466,467 URT
 *  3,154,616,000  12527   251,825   2113  1,492,955 BigWings
 *  3,275,454,400  11353   288,510   2538  1,290,565 DeathBlossom
 *  4,981,164,800   9213   540,666   2048  2,432,209 ALS_XZ
 *  5,828,321,200   7953   732,845   2605  2,237,359 ALS_Wing
 * 10,058,510,600   5887 1,708,597   2106  4,776,120 ALS_Chain
 *  1,482,484,700   4237   349,890     26 57,018,642 SueDeCoq
 * 13,505,418,800   4232 3,191,261    322 41,942,294 FrankenSwordfish
 *  7,312,251,800   3979 1,837,710    431 16,965,781 UnaryChain
 *  6,419,440,100   3615 1,775,778     97 66,179,794 NishioChain
 *  7,311,671,100   3518 2,078,360   5397  1,354,765 DynamicChain
 *     71,943,800     11 6,540,345     11  6,540,345 DynamicPlus
 * 69,236,300,200
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  116,331,758,700 (01:56)  79,407,343
 *
 * NOTES:
 * 1. 1:56 is the new par. Similar code ran in 1:22, but thats a FREAK!
 *    See {@link diuf.sudoku.solver.hinters.chain.AChainerBase} comments.
 * 2. DiufSudoku_V6_30.230.2023-08-10.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.231 2023-08-14</b> Chainers have changed, for better or worse.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     22,636,946 108640       208  57480         393 NakedSingle
 *     37,347,022  51160       730  16321       2,288 HiddenSingle
 *     78,393,357  34839     2,250  24175       3,242 Locking
 *     76,609,955  23482     3,262   7828       9,786 NakedPair
 *     63,938,243  21413     2,985   8230       7,768 HiddenPair
 *    122,116,630  19340     6,314   1591      76,754 NakedTriple
 *    101,068,996  18947     5,334    980     103,131 HiddenTriple
 *     59,935,843  18755     3,195   1329      45,098 TwoStringKite
 *     51,270,897  17426     2,942    444     115,474 Swampfish
 *     58,574,066  17237     3,398    889      65,887 W_Wing
 *     79,175,030  16620     4,763    541     146,349 XY_Wing
 *     49,402,220  16258     3,038    315     156,832 Skyscraper
 *    104,577,671  16097     6,496    580     180,306 EmptyRectangle
 *     50,142,758  15517     3,231    253     198,192 XYZ_Wing
 *     63,828,711  15279     4,177    222     287,516 Swordfish
 *    165,385,443  15217    10,868    120   1,378,212 NakedQuad
 *     91,061,620  15194     5,993     19   4,792,716 Jellyfish
 *    120,911,461  15190     7,959      1 120,911,461 HiddenQuad
 *    314,092,635  15189    20,678    185   1,697,798 Coloring
 *    934,875,133  15068    62,043    763   1,225,262 XColoring
 *  1,976,134,039  14796   133,558 123619      15,985 GEM
 *  1,335,578,893  13430    99,447    888   1,504,030 URT
 *  3,234,575,883  12797   252,760   2068   1,564,108 BigWings
 *  3,336,689,003  11639   286,681   2487   1,341,652 DeathBlossom
 *  5,135,444,466   9538   538,419   2040   2,517,374 ALS_XZ
 *  6,134,364,666   8275   741,312   2649   2,315,728 ALS_Wing
 * 10,657,184,129   6176 1,725,580   2086   5,108,908 ALS_Chain
 *  1,571,949,889   4559   344,801     34  46,233,820 SueDeCoq
 * 14,714,442,252   4553 3,231,812    330  44,589,218 FrankenSwordfish
 *  7,229,253,363   4293 1,683,963    435  16,618,973 UnaryChain
 *  6,152,888,193   3947 1,558,877   3523   1,746,491 StaticChain
 *  4,221,265,563   2260 1,867,816     55  76,750,282 NishioChain
 *  4,995,458,693   2205 2,265,514   3013   1,657,968 DynamicChain
 *     67,509,198     11 6,137,199     11   6,137,199 DynamicPlus
 * 73,408,082,867
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  120,783,568,099 (02:00)  82,446,121
 *
 * NOTES:
 * 1. 2:00 is 4 seconds slower. Not happy Jan!
 * 2. DiufSudoku_V6_30.231.2023-08-14.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.232 2023-08-17</b> RccFinderAllRecycle turn RccFinderForwardOnly
 * results into RccFinderAll results, ie A-&gt;B into A-&gt;B and B-&gt;B.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,292,000 108640       223  57480         422 NakedSingle
 *     38,343,100  51160       749  16321       2,349 HiddenSingle
 *     76,910,500  34839     2,207  24175       3,181 Locking
 *     80,463,400  23482     3,426   7828      10,278 NakedPair
 *     62,563,000  21413     2,921   8230       7,601 HiddenPair
 *    120,090,500  19340     6,209   1591      75,481 NakedTriple
 *    100,156,000  18947     5,286    980     102,200 HiddenTriple
 *     59,506,900  18755     3,172   1329      44,775 TwoStringKite
 *     51,054,000  17426     2,929    444     114,986 Swampfish
 *     59,106,500  17237     3,429    889      66,486 W_Wing
 *     77,857,000  16620     4,684    541     143,913 XY_Wing
 *     47,692,000  16258     2,933    315     151,403 Skyscraper
 *    106,452,000  16097     6,613    580     183,537 EmptyRectangle
 *     46,984,800  15517     3,027    253     185,710 XYZ_Wing
 *     64,619,400  15279     4,229    222     291,078 Swordfish
 *    159,771,500  15217    10,499    120   1,331,429 NakedQuad
 *     92,889,600  15194     6,113     19   4,888,926 Jellyfish
 *    121,467,500  15190     7,996      1 121,467,500 HiddenQuad
 *    313,122,500  15189    20,615    185   1,692,554 Coloring
 *    937,557,400  15068    62,221    763   1,228,777 XColoring
 *  1,983,838,200  14796   134,079 123619      16,048 GEM
 *  1,319,020,000  13430    98,214    888   1,485,382 URT
 *  3,203,813,300  12797   250,356   2068   1,549,232 BigWings
 *  3,299,443,900  11639   283,481   2487   1,326,676 DeathBlossom
 *  5,168,696,500   9538   541,905   2040   2,533,674 ALS_XZ
 *  6,217,362,800   8275   751,342   2649   2,347,060 ALS_Wing
 *  4,556,994,100   6176   737,855   2086   2,184,560 ALS_Chain
 *  1,413,964,200   4559   310,147     34  41,587,182 SueDeCoq
 * 14,165,306,700   4553 3,111,202    330  42,925,171 FrankenSwordfish
 *  7,049,408,900   4293 1,642,070    435  16,205,537 UnaryChain
 *  6,055,167,500   3947 1,534,118   3523   1,718,753 StaticChain
 *  4,297,907,400   2260 1,901,728     55  78,143,770 NishioChain
 *  4,965,808,200   2205 2,252,067   3013   1,648,127 DynamicChain
 *     73,508,400     11 6,682,581     11   6,682,581 DynamicPlus
 * 66,411,139,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,632,335,600 (01:53)  77,564,734
 *
 * NOTES:
 * 1. 1:53 is 3 seconds under par. ALS_Chain dropped from 10,657,184,129 to
 *    just 4,556,994,100 because its not wasting time fetching its own rccs
 *    from scratch. Instead it parses RccFinderForwardOnly results into ALL.
 * 2. DiufSudoku_V6_30.232.2023-08-17.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.233 2023-08-25</b> DeathBlossom split search method out of
 * recurse. I build only because its overdue.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     26,527,600 108638       244  57480        461 NakedSingle
 *     39,267,000  51158       767  16321      2,405 HiddenSingle
 *     83,166,600  34837     2,387  24175      3,440 Locking
 *     76,057,000  23480     3,239   7828      9,716 NakedPair
 *     64,485,400  21411     3,011   8230      7,835 HiddenPair
 *    121,506,200  19338     6,283   1591     76,370 NakedTriple
 *     99,946,500  18945     5,275    980    101,986 HiddenTriple
 *     61,581,800  18753     3,283   1329     46,336 TwoStringKite
 *     51,431,300  17424     2,951    444    115,836 Swampfish
 *     60,303,700  17235     3,498    889     67,833 W_Wing
 *     79,720,400  16618     4,797    541    147,357 XY_Wing
 *     48,566,700  16256     2,987    315    154,180 Skyscraper
 *    106,521,100  16095     6,618    579    183,974 EmptyRectangle
 *     50,415,800  15516     3,249    253    199,271 XYZ_Wing
 *     64,644,900  15278     4,231    222    291,193 Swordfish
 *    164,118,100  15216    10,785    120  1,367,650 NakedQuad
 *     91,338,700  15193     6,011     19  4,807,300 Jellyfish
 *    325,916,100  15189    21,457    185  1,761,708 Coloring
 *    942,483,300  15068    62,548    763  1,235,233 XColoring
 *  1,993,094,800  14796   134,704 123619     16,122 GEM
 *  1,337,414,700  13430    99,584    888  1,506,097 URT
 *  3,213,159,100  12797   251,086   2068  1,553,751 BigWings
 *  3,619,497,600  11639   310,980   2487  1,455,366 DeathBlossom
 *  5,302,207,400   9538   555,903   2040  2,599,121 ALS_XZ
 *  6,098,638,400   8275   736,995   2649  2,302,241 ALS_Wing
 *  4,351,515,300   6176   704,584   2086  2,086,057 ALS_Chain
 *  1,516,479,100   4559   332,634     34 44,602,326 SueDeCoq
 * 14,216,320,900   4553 3,122,407    330 43,079,760 FrankenSwordfish
 *  6,152,032,400   4293 1,433,038    435 14,142,603 UnaryChain
 *  5,269,407,100   3947 1,335,041   3523  1,495,715 StaticChain
 *  4,071,594,200   2260 1,801,590     55 74,028,985 NishioChain
 *  4,884,031,300   2205 2,214,980   3013  1,620,986 DynamicChain
 *     73,294,700     11 6,663,154     11  6,663,154 DynamicPlus
 * 64,656,685,200
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  111,922,623,400 (01:51)  76,397,695
 *
 * NOTES:
 * 1. 1:51 is 2 seconds faster again.
 * 2. DiufSudoku_V6_30.233.2023-08-25.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.234 2023-08-26</b> Fixed nasty Swing NPE with fixedHeight JTree.
 * I had a super-fast profiler batch, hence want to keep this codebase. Note
 * that NakedSingle took HALF of the "normal" 26 milliseconds. Mad at Oracle!
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     13,271,400 108638       122  57480        230 NakedSingle
 *     26,890,000  51158       525  16321      1,647 HiddenSingle
 *     69,688,600  34837     2,000  24175      2,882 Locking
 *     67,922,500  23480     2,892   7828      8,676 NakedPair
 *     58,232,000  21411     2,719   8230      7,075 HiddenPair
 *    121,444,000  19338     6,280   1591     76,331 NakedTriple
 *    101,682,800  18945     5,367    980    103,757 HiddenTriple
 *     47,359,500  18753     2,525   1329     35,635 TwoStringKite
 *     43,831,700  17424     2,515    444     98,720 Swampfish
 *     52,332,700  17235     3,036    889     58,866 W_Wing
 *     75,474,300  16618     4,541    541    139,508 XY_Wing
 *     41,366,800  16256     2,544    315    131,323 Skyscraper
 *     93,918,300  16095     5,835    579    162,207 EmptyRectangle
 *     47,888,100  15516     3,086    253    189,281 XYZ_Wing
 *     55,908,400  15278     3,659    222    251,839 Swordfish
 *    161,226,700  15216    10,595    120  1,343,555 NakedQuad
 *     80,254,400  15193     5,282     19  4,223,915 Jellyfish
 *    291,543,600  15189    19,194    185  1,575,911 Coloring
 *    887,444,000  15068    58,895    763  1,163,098 XColoring
 *  1,938,584,100  14796   131,020 123619     15,681 GEM
 *  1,469,892,500  13430   109,448    888  1,655,284 URT
 *  3,209,621,400  12797   250,810   2068  1,552,041 BigWings
 *  3,893,334,400  11639   334,507   2487  1,565,474 DeathBlossom
 *  5,097,834,500   9538   534,476   2040  2,498,938 ALS_XZ
 *  4,576,603,900   8275   553,063   2649  1,727,672 ALS_Wing
 *  4,094,389,000   6176   662,951   2086  1,962,794 ALS_Chain
 *  1,566,733,200   4559   343,657     34 46,080,388 SueDeCoq
 * 14,147,695,100   4553 3,107,334    330 42,871,803 FrankenSwordfish
 *  5,423,777,300   4293 1,263,400    435 12,468,453 UnaryChain
 *  4,153,768,100   3947 1,052,386   3523  1,179,042 StaticChain
 *  2,615,153,000   2260 1,157,147     55 47,548,236 NishioChain
 *  3,467,604,200   2205 1,572,609   3013  1,150,880 DynamicChain
 *     74,050,200     11 6,731,836     11  6,731,836 DynamicPlus
 * 58,066,720,700
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  78,314,087,800 (01:18)  53,456,715
 *
 * NOTES:
 * 1. 1:18 is heaps faster, but this was profiler Java 1.8.0_161 verses normal
 *    java.exe 17.0.1, which is NOT a fair comparison coz 1.8 is ~30% faster.
 *    Oracle sells faster hardware, at a premium, hence they manufacture demand
 *    for it. Typical. In 17 this SAME CODE took 1:50, which is one under par.
 * 2. DiufSudoku_V6_30.234.2023-08-26.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.235 2023-09-04</b> UnaryChainer uses AssAnc extends Ass, to keep
 * ancestors out of the other chainers, because they dont use hasAncestor.
 * Reverted to 234 this morning coz I broke the chainers. This build backs-up
 * chainers before I redo other changes: remove Cell from Ass and Pots.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     25,154,900 108637       231  57489        437 NakedSingle
 *     37,931,800  51148       741  16329      2,322 HiddenSingle
 *     81,251,600  34819     2,333  24168      3,361 Locking
 *     77,393,500  23467     3,297   7823      9,893 NakedPair
 *     64,811,200  21400     3,028   8230      7,874 HiddenPair
 *    120,104,000  19327     6,214   1591     75,489 NakedTriple
 *     99,382,900  18934     5,248    980    101,411 HiddenTriple
 *     59,293,600  18742     3,163   1326     44,716 TwoStringKite
 *     51,874,300  17416     2,978    444    116,834 Swampfish
 *     60,427,400  17227     3,507    889     67,972 W_Wing
 *     80,057,900  16610     4,819    535    149,640 XY_Wing
 *     52,967,300  16250     3,259    315    168,150 Skyscraper
 *    105,059,300  16089     6,529    580    181,136 EmptyRectangle
 *     47,479,800  15509     3,061    253    187,667 XYZ_Wing
 *     64,153,700  15271     4,201    222    288,980 Swordfish
 *    164,540,300  15209    10,818    120  1,371,169 NakedQuad
 *     90,287,100  15186     5,945     19  4,751,952 Jellyfish
 *    311,778,200  15182    20,536    185  1,685,287 Coloring
 *    928,539,600  15061    61,651    762  1,218,555 XColoring
 *  1,964,012,900  14790   132,793 123458     15,908 GEM
 *  1,326,159,700  13427    98,768    888  1,493,423 URT
 *  3,234,193,200  12794   252,789   2065  1,566,195 BigWings
 *  3,568,265,100  11637   306,631   2487  1,434,766 DeathBlossom
 *  5,351,594,100   9536   561,199   2040  2,623,330 ALS_XZ
 *  6,123,891,200   8273   740,226   2649  2,311,774 ALS_Wing
 *  4,527,844,800   6174   733,372   2086  2,170,587 ALS_Chain
 *  1,571,312,600   4557   344,812     34 46,215,076 SueDeCoq
 * 14,356,239,500   4551 3,154,524    330 43,503,756 FrankenSwordfish
 *  8,267,993,200   4291 1,926,821    424 19,499,983 UnaryChain
 *  5,140,928,900   3947 1,302,490   3523  1,459,247 StaticChain
 *  4,211,004,400   2260 1,863,276     55 76,563,716 NishioChain
 *  4,897,720,200   2205 2,221,188   3013  1,625,529 DynamicChain
 *     83,429,600     11 7,584,509     11  7,584,509 DynamicPlus
 * 67,147,077,800
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  114,273,515,800 (01:54)  78,002,399
 *
 * NOTES:
 * 1. 1:54 is 3 seconds slower than the last standard run, which is suprising
 *    and disappointing. I keep it anyway, because this back-up has a purpose.
 *    Anything under three minutes is "fast enough". Under 2 minutes is great.
 * 2. DiufSudoku_V6_30.235.2023-09-04.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.236 2023-09-05</b> Removed Ass.cell and Pots key is now Integer
 * indice. SudokuGridPanel is now indice-based. All get${Color}BGCells methods
 * renamed get${Color}BgIndices and return {@code Set<Integer>}. Idx implements
 * {@code Set<Integer>} for use in SudokuGridPanel.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     24,516,300 108637       225  57489        426 NakedSingle
 *     39,057,000  51148       763  16329      2,391 HiddenSingle
 *     81,080,000  34819     2,328  24168      3,354 Locking
 *     77,312,200  23467     3,294   7823      9,882 NakedPair
 *     63,689,200  21400     2,976   8230      7,738 HiddenPair
 *    122,097,700  19327     6,317   1591     76,742 NakedTriple
 *     97,147,800  18934     5,130    980     99,130 HiddenTriple
 *     60,123,600  18742     3,207   1326     45,342 TwoStringKite
 *     51,678,400  17416     2,967    444    116,392 Swampfish
 *     60,352,500  17227     3,503    889     67,888 W_Wing
 *     77,576,000  16610     4,670    535    145,001 XY_Wing
 *     48,934,100  16250     3,011    315    155,346 Skyscraper
 *    104,953,700  16089     6,523    580    180,954 EmptyRectangle
 *     48,673,900  15509     3,138    253    192,386 XYZ_Wing
 *     64,571,300  15271     4,228    222    290,861 Swordfish
 *    163,403,900  15209    10,743    120  1,361,699 NakedQuad
 *     91,094,800  15186     5,998     19  4,794,463 Jellyfish
 *    315,590,800  15182    20,787    185  1,705,896 Coloring
 *    912,780,300  15061    60,605    762  1,197,874 XColoring
 *  1,986,022,300  14790   134,281 123458     16,086 GEM
 *  1,330,047,500  13427    99,057    888  1,497,801 URT
 *  3,216,447,500  12794   251,402   2065  1,557,601 BigWings
 *  3,536,807,500  11637   303,927   2487  1,422,118 DeathBlossom
 *  5,314,156,000   9536   557,273   2040  2,604,978 ALS_XZ
 *  6,086,764,100   8273   735,738   2649  2,297,759 ALS_Wing
 *  4,486,934,100   6174   726,746   2086  2,150,975 ALS_Chain
 *  1,549,864,500   4557   340,106     34 45,584,250 SueDeCoq
 * 14,243,148,500   4551 3,129,674    330 43,161,056 FrankenSwordfish
 *  7,991,577,000   4291 1,862,404    424 18,848,058 UnaryChain
 *  5,152,500,800   3947 1,305,422   3523  1,462,532 StaticChain
 *  4,071,091,300   2260 1,801,367     55 74,019,841 NishioChain
 *  4,781,703,800   2205 2,168,573   3013  1,587,024 DynamicChain
 *     76,897,800     11 6,990,709     11  6,990,709 DynamicPlus
 * 66,328,596,200
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,256,651,700 (01:53)  77,308,294
 *
 * NOTES:
 * 1. 1:53 is 1 second faster than previous.
 * 2. DiufSudoku_V6_30.236.2023-09-05.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.237 2023-09-10</b> All hints take IHinter, instead of AHinter.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     23,682,700 108602       218  57454         412 NakedSingle
 *     38,239,700  51148       747  16336       2,340 HiddenSingle
 *     79,555,100  34812     2,285  24167       3,291 Locking
 *     76,708,500  23462     3,269   7825       9,803 NakedPair
 *     63,975,700  21395     2,990   8232       7,771 HiddenPair
 *    123,213,800  19322     6,376   1591      77,444 NakedTriple
 *    100,405,300  18929     5,304    980     102,454 HiddenTriple
 *     58,980,300  18737     3,147   1327      44,446 TwoStringKite
 *     50,761,800  17410     2,915    447     113,561 Swampfish
 *     58,563,100  17221     3,400    891      65,727 W_Wing
 *     76,284,800  16603     4,594    533     143,123 XY_Wing
 *     48,538,000  16244     2,988    265     183,162 XYZ_Wing
 *     48,589,100  15996     3,037    314     154,742 Skyscraper
 *    103,931,600  15835     6,563    577     180,124 EmptyRectangle
 *     63,610,200  15258     4,168    223     285,247 Swordfish
 *     88,282,000  15195     5,809     19   4,646,421 Jellyfish
 *    168,921,300  15191    11,119    120   1,407,677 NakedQuad
 *    126,144,700  15168     8,316      1 126,144,700 HiddenQuad
 *    313,765,100  15167    20,687    185   1,696,027 Coloring
 *    932,928,100  15046    62,005    761   1,225,923 XColoring
 *  1,990,587,000  14776   134,717 123739      16,086 GEM
 *  1,324,701,300  13413    98,762    886   1,495,148 URT
 *  2,289,820,200  12782   179,144   2065   1,108,871 BigWings
 *  3,498,274,200  11625   300,926   2486   1,407,189 DeathBlossom
 *  5,109,122,800   9527   536,278   2037   2,508,160 ALS_XZ
 *  6,091,530,900   8266   736,938   2692   2,262,827 ALS_Wing
 *  4,248,058,400   6145   691,303   2046   2,076,274 ALS_Chain
 *  1,428,466,200   4544   314,363     34  42,013,711 SueDeCoq
 * 14,411,147,400   4538 3,175,660    329  43,802,879 FrankenSwordfish
 *  8,135,030,800   4279 1,901,152    423  19,231,751 UnaryChain
 *  5,236,531,400   3936 1,330,419   3532   1,482,596 StaticChain
 *  4,152,278,300   2240 1,853,695     54  76,894,042 NishioChain
 *  4,844,163,600   2186 2,215,994   2994   1,617,957 DynamicChain
 *     84,049,000     11 7,640,818     11   7,640,818 DynamicPlus
 * 65,488,842,400
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,270,911,000 (01:53)  77,318,027
 *
 * NOTES:
 * 1. 1:53 is same as previous. I hoped this would be faster. Sigh.
 * 2. DiufSudoku_V6_30.237.2023-09-10.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.238 2023-09-11</b> Made GEM, UnaryChain, Pots, etc faster.
 * <pre>
 *       time(ns)  calls time/call  elims  time/elim hinter
 *     14,476,900 108979       132  57591        251 NakedSingle
 *     26,614,600  51388       517  16374      1,625 HiddenSingle
 *     72,048,300  35014     2,057  24146      2,983 Locking
 *     71,205,500  23679     3,007   7798      9,131 NakedPair
 *     58,885,900  21616     2,724   8233      7,152 HiddenPair
 *    121,287,000  19535     6,208   1607     75,474 NakedTriple
 *     96,252,900  19130     5,031    983     97,917 HiddenTriple
 *     44,335,300  18936     2,341   1338     33,135 TwoStringKite
 *     42,501,300  17598     2,415    453     93,821 Swampfish
 *     52,392,300  17407     3,009    891     58,801 W_Wing
 *     74,824,600  16783     4,458    514    145,573 XY_Wing
 *     49,727,200  16437     3,025    263    189,076 XYZ_Wing
 *     37,802,000  16189     2,335    322    117,397 Skyscraper
 *    100,688,300  16024     6,283    591    170,369 EmptyRectangle
 *     55,865,400  15433     3,619    229    243,953 Swordfish
 *     81,127,100  15368     5,278     22  3,687,595 Jellyfish
 *    161,350,000  15363    10,502    118  1,367,372 NakedQuad
 *    311,623,200  15342    20,311    196  1,589,914 Coloring
 *    927,685,700  15214    60,975    840  1,104,387 XColoring
 *  1,992,918,400  14926   133,519 121716     16,373 GEM
 *  1,480,345,000  13578   109,025    894  1,655,866 URT
 *  2,333,803,300  12941   180,341   2059  1,133,464 BigWings
 *  4,032,194,800  11776   342,407   2490  1,619,355 DeathBlossom
 *  5,405,874,800   9675   558,746   2031  2,661,681 ALS_XZ
 *  5,316,394,200   8420   631,400   2674  1,988,180 ALS_Wing
 *  4,229,803,200   6312   670,120   2075  2,038,459 ALS_Chain
 *  6,737,158,900   4697 1,434,353    470 14,334,380 UnaryChain
 *  4,603,734,100   4307 1,068,895   3926  1,172,627 StaticChain
 *  3,907,843,600   2352 1,661,498   3241  1,205,752 DynamicChain
 *    109,418,300     11 9,947,118     11  9,947,118 DynamicPlus
 * 42,550,182,100
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  60,073,926,300 (01:00)  41,006,093
 *
 * NOTES:
 * 1. 1:00 is 53 seconds under par, but keep ya pants on. It was a profiler run
 *    and not all the usual hinters where wanted. Most notably: No big fish.
 * 2. DiufSudoku_V6_30.238.2023-09-11.7z is releasable, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.239 2023-09-17</b> Made LinkedMatrixAssSet simpler but slower.
 * This is not a release build, merely a staging point.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     25,175,100 108602       231  57454         438 NakedSingle
 *     46,973,000  51148       918  16336       2,875 HiddenSingle
 *     95,972,400  34812     2,756  24167       3,971 Locking
 *     83,128,300  23462     3,543   7825      10,623 NakedPair
 *     68,390,000  21395     3,196   8232       8,307 HiddenPair
 *    122,996,700  19322     6,365   1591      77,307 NakedTriple
 *     99,836,500  18929     5,274    980     101,873 HiddenTriple
 *     63,251,000  18737     3,375   1327      47,664 TwoStringKite
 *     54,375,000  17410     3,123    447     121,644 Swampfish
 *     60,746,600  17221     3,527    891      68,178 W_Wing
 *     75,839,500  16603     4,567    533     142,287 XY_Wing
 *     48,001,100  16244     2,955    265     181,136 XYZ_Wing
 *     51,840,800  15996     3,240    314     165,098 Skyscraper
 *    105,739,900  15835     6,677    577     183,258 EmptyRectangle
 *     63,624,000  15258     4,169    223     285,309 Swordfish
 *     87,626,000  15195     5,766     19   4,611,894 Jellyfish
 *    152,478,800  15191    10,037    120   1,270,656 NakedQuad
 *    114,203,000  15168     7,529      1 114,203,000 HiddenQuad
 *    307,914,100  15167    20,301    185   1,664,400 Coloring
 *    933,909,700  15046    62,070    761   1,227,213 XColoring
 *  1,961,581,300  14776   132,754 123739      15,852 GEM
 *  1,306,875,000  13413    97,433    886   1,475,028 URT
 *  2,279,939,800  12782   178,371   2065   1,104,087 BigWings
 *  3,449,232,000  11625   296,708   2486   1,387,462 DeathBlossom
 *  5,203,290,200   9527   546,162   2037   2,554,388 ALS_XZ
 *  6,236,859,400   8266   754,519   2692   2,316,812 ALS_Wing
 *  4,283,138,700   6145   697,011   2046   2,093,420 ALS_Chain
 *  1,301,733,100   4544   286,472     34  38,286,267 SueDeCoq
 * 13,972,756,100   4538 3,079,055    329  42,470,383 FrankenSwordfish
 *  7,889,012,600   4279 1,843,658    423  18,650,147 UnaryChain
 *  5,531,619,600   3936 1,405,391   3532   1,566,143 StaticChain
 *  4,122,238,500   2240 1,840,285     54  76,337,750 NishioChain
 *  4,826,717,000   2186 2,208,013   2994   1,612,129 DynamicChain
 *     73,525,200     11 6,684,109     11   6,684,109 DynamicPlus
 * 65,100,540,000
 * pzls       total (ns) (mm:ss)    each (ns)
 * 1465  151,870,703,700 (02:31)  103,666,009
 *
 * NOTES:
 * 1. 2:31 is 34 seconds slower. This is just a staging point.
 * 2. DiufSudoku_V6_30.239.2023-09-17.LOGS.7z is NOT release candidate.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.240 2023-09-19</b> Solved the slower problem; I forgot to remove the
 * countSolutions from solve. Oops!
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,068,509 108644       221  57508         418 NakedSingle
 *     40,724,500  51136       796  16319       2,495 HiddenSingle
 *     81,368,675  34817     2,337  24181       3,364 Locking
 *     75,931,345  23460     3,236   7831       9,696 NakedPair
 *     64,627,578  21391     3,021   8235       7,847 HiddenPair
 *    122,660,069  19321     6,348   1593      76,999 NakedTriple
 *    100,856,532  18925     5,329    980     102,914 HiddenTriple
 *     59,437,024  18733     3,172   1326      44,824 TwoStringKite
 *     53,007,621  17407     3,045    443     119,656 Swampfish
 *     60,543,925  17219     3,516    902      67,121 W_Wing
 *     78,169,777  16593     4,711    527     148,329 XY_Wing
 *     49,043,038  16237     3,020    266     184,372 XYZ_Wing
 *     52,493,110  15988     3,283    316     166,117 Skyscraper
 *    105,359,889  15825     6,657    574     183,553 EmptyRectangle
 *     64,368,136  15251     4,220    224     287,357 Swordfish
 *     89,597,324  15188     5,899     19   4,715,648 Jellyfish
 *    155,297,289  15184    10,227    120   1,294,144 NakedQuad
 *    118,671,554  15161     7,827      1 118,671,554 HiddenQuad
 *    316,992,079  15160    20,909    188   1,686,128 Coloring
 *    932,221,264  15036    61,999    760   1,226,606 XColoring
 *  2,037,140,396  14767   137,952 123320      16,519 GEM
 *  1,312,862,315  13407    97,923    883   1,486,820 URT
 *  2,332,258,179  12778   182,521   2060   1,132,164 BigWings
 *  3,524,720,828  11624   303,227   2482   1,420,113 DeathBlossom
 *  5,286,030,832   9530   554,672   2028   2,606,524 ALS_XZ
 *  6,331,809,173   8276   765,080   2709   2,337,323 ALS_Wing
 *  4,295,754,892   6144   699,178   2047   2,098,561 ALS_Chain
 *  1,402,898,433   4543   308,804     34  41,261,718 SueDeCoq
 * 13,355,060,751   4537 2,943,588    331  40,347,615 FrankenSwordfish
 *  8,172,142,598   4276 1,911,165    414  19,739,474 UnaryChain
 *  5,663,973,316   3933 1,440,115   3624   1,562,906 StaticChain
 *  4,152,228,464   2208 1,880,538     53  78,343,933 NishioChain
 *  4,949,899,763   2155 2,296,937   2972   1,665,511 DynamicChain
 *     97,140,700     11 8,830,972     11   8,830,972 DynamicPlus
 * 65,559,359,878
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  112,878,094,600 (01:52)  77,049,893
 *
 * NOTES:
 * 1. 1:52 is back on par, so this is releasable.
 * 2. DiufSudoku_V6_30.240.2023-09-19.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.241 2023-09-20</b> Renovated test-cases.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     25,564,500 108644       235  57508         444 NakedSingle
 *     40,770,300  51136       797  16319       2,498 HiddenSingle
 *     85,039,900  34817     2,442  24181       3,516 Locking
 *     79,731,200  23460     3,398   7831      10,181 NakedPair
 *     66,330,100  21391     3,100   8235       8,054 HiddenPair
 *    124,449,300  19321     6,441   1593      78,122 NakedTriple
 *    101,702,300  18925     5,373    980     103,777 HiddenTriple
 *     59,632,400  18733     3,183   1326      44,971 TwoStringKite
 *     52,066,300  17407     2,991    443     117,531 Swampfish
 *     63,281,900  17219     3,675    902      70,157 W_Wing
 *     83,039,800  16593     5,004    527     157,570 XY_Wing
 *     52,264,600  16237     3,218    266     196,483 XYZ_Wing
 *     53,517,000  15988     3,347    316     169,357 Skyscraper
 *    105,251,400  15825     6,650    574     183,364 EmptyRectangle
 *     64,772,000  15251     4,247    224     289,160 Swordfish
 *     89,838,700  15188     5,915     19   4,728,352 Jellyfish
 *    155,938,700  15184    10,269    120   1,299,489 NakedQuad
 *    125,007,200  15161     8,245      1 125,007,200 HiddenQuad
 *    325,637,000  15160    21,480    188   1,732,111 Coloring
 *    931,756,600  15036    61,968    760   1,225,995 XColoring
 *  2,079,286,800  14767   140,806 123320      16,860 GEM
 *  1,333,034,200  13407    99,428    883   1,509,665 URT
 *  2,346,174,100  12778   183,610   2060   1,138,919 BigWings
 *  3,507,103,400  11624   301,712   2482   1,413,015 DeathBlossom
 *  5,324,530,600   9530   558,712   2028   2,625,508 ALS_XZ
 *  6,396,459,500   8276   772,892   2709   2,361,188 ALS_Wing
 *  4,370,888,500   6144   711,407   2047   2,135,265 ALS_Chain
 *  1,465,014,300   4543   322,477     34  43,088,655 SueDeCoq
 * 13,773,807,300   4537 3,035,884    331  41,612,710 FrankenSwordfish
 *  8,270,535,900   4276 1,934,175    414  19,977,139 UnaryChain
 *  5,734,768,400   3933 1,458,115   3624   1,582,441 StaticChain
 *  4,156,943,200   2208 1,882,673     53  78,432,890 NishioChain
 *  4,627,832,000   2155 2,147,485   2972   1,557,144 DynamicChain
 *     94,874,100     11 8,624,918     11   8,624,918 DynamicPlus
 * 66,166,843,500
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,856,840,600 (01:53)  77,717,979
 *
 * NOTES:
 * 1. 1:53 is still on par, so this is releasable.
 * 2. DiufSudoku_V6_30.241.2023-09-20.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.242 2023-09-21</b> AHinter.findHints(Grid, IAccu) sets a list of
 * standard grid and accu fields, and calls the new findHints() method, which
 * is overridden by heavy hinters, to inherit the standard grid fields.
 * <p>
 * Fast hinters still override the actual findHints(Grid, IAccu) method, where
 * the cost of setting all the "standard" fields is prohibitive.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     26,315,100 108732       242  57499         457 NakedSingle
 *     38,273,300  51233       747  16328       2,344 HiddenSingle
 *    107,419,500  34905     3,077  25103       4,279 LockingBasic
 *     73,416,500  23241     3,158   7133      10,292 NakedPair
 *     63,580,300  21391     2,972   8235       7,720 HiddenPair
 *    121,419,800  19321     6,284   1593      76,220 NakedTriple
 *    100,873,500  18925     5,330    980     102,932 HiddenTriple
 *     55,374,500  18733     2,955   1326      41,760 TwoStringKite
 *     50,735,100  17407     2,914    443     114,526 Swampfish
 *     66,230,900  17219     3,846    902      73,426 W_Wing
 *     80,730,500  16593     4,865    527     153,188 XY_Wing
 *     47,732,800  16237     2,939    266     179,446 XYZ_Wing
 *     45,919,400  15988     2,872    316     145,314 Skyscraper
 *    101,470,300  15825     6,412    574     176,777 EmptyRectangle
 *     64,491,600  15251     4,228    224     287,908 Swordfish
 *     88,980,400  15188     5,858     19   4,683,178 Jellyfish
 *    157,176,900  15184    10,351    120   1,309,807 NakedQuad
 *    122,938,800  15161     8,108      1 122,938,800 HiddenQuad
 *    313,459,600  15160    20,676    188   1,667,338 Coloring
 *    962,025,400  15036    63,981    760   1,265,822 XColoring
 *  2,019,260,800  14767   136,741 123320      16,374 GEM
 *  1,324,365,300  13407    98,781    883   1,499,847 URT
 *  2,307,686,100  12778   180,598   2060   1,120,235 BigWings
 *  3,415,180,900  11624   293,804   2482   1,375,979 DeathBlossom
 *  5,218,812,200   9530   547,619   2028   2,573,378 ALS_XZ
 *  6,259,177,000   8276   756,304   2709   2,310,511 ALS_Wing
 *  4,223,642,300   6144   687,441   2047   2,063,332 ALS_Chain
 *  1,413,093,000   4543   311,048     34  41,561,558 SueDeCoq
 * 13,335,427,900   4537 2,939,261    331  40,288,301 FrankenSwordfish
 *  7,901,567,900   4276 1,847,887    414  19,085,912 UnaryChain
 *  5,533,605,700   3933 1,406,968   3624   1,526,933 StaticChain
 *  3,930,989,200   2208 1,780,339     53  74,169,607 NishioChain
 *  4,533,819,700   2155 2,103,860   2972   1,525,511 DynamicChain
 *     90,790,400     11 8,253,672     11   8,253,672 DynamicPlus
 * 64,195,982,600
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  111,758,698,200 (01:51)  76,285,800
 *
 * NOTES:
 * 1. 1:51 is two seconds under par (cool!) so this is releasable.
 * 2. DiufSudoku_V6_30.242.2023-09-21.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.243 2023-09-25</b> Enspeedonating hinters. The only real change
 * is fixed a reset bug in FrutantFisherman, which is now a whole second faster
 * as a result.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,216,700 108644       222  57508         421 NakedSingle
 *     39,369,600  51136       769  16319       2,412 HiddenSingle
 *     90,828,700  34817     2,608  24181       3,756 Locking
 *     77,310,800  23460     3,295   7831       9,872 NakedPair
 *     66,058,400  21391     3,088   8235       8,021 HiddenPair
 *    124,164,900  19321     6,426   1593      77,944 NakedTriple
 *    103,226,400  18925     5,454    980     105,333 HiddenTriple
 *     61,555,700  18733     3,285   1326      46,422 TwoStringKite
 *     52,294,300  17407     3,004    443     118,045 Swampfish
 *     70,564,800  17219     4,098    902      78,231 W_Wing
 *     85,289,700  16593     5,140    527     161,840 XY_Wing
 *     51,944,700  16237     3,199    266     195,280 XYZ_Wing
 *     49,963,700  15988     3,125    316     158,112 Skyscraper
 *    105,515,400  15825     6,667    574     183,824 EmptyRectangle
 *     64,639,900  15251     4,238    224     288,570 Swordfish
 *     90,484,800  15188     5,957     19   4,762,357 Jellyfish
 *    161,884,200  15184    10,661    120   1,349,035 NakedQuad
 *    125,488,600  15161     8,277      1 125,488,600 HiddenQuad
 *    319,548,900  15160    21,078    188   1,699,728 Coloring
 *    933,276,000  15036    62,069    760   1,227,994 XColoring
 *  2,049,923,900  14767   138,817 123320      16,622 GEM
 *  1,333,817,600  13407    99,486    883   1,510,552 URT
 *  2,388,839,000  12778   186,949   2060   1,159,630 BigWings
 *  3,508,620,200  11624   301,842   2482   1,413,626 DeathBlossom
 *  5,271,985,600   9530   553,198   2028   2,599,598 ALS_XZ
 *  6,269,143,600   8276   757,508   2709   2,314,191 ALS_Wing
 *  4,380,136,800   6144   712,912   2047   2,139,783 ALS_Chain
 *  1,441,405,500   4543   317,280     34  42,394,279 SueDeCoq
 * 13,897,650,100   4537 3,063,180    331  41,986,858 FrankenSwordfish
 *  8,119,833,600   4276 1,898,932    414  19,613,124 UnaryChain
 *  5,725,135,100   3933 1,455,666   3624   1,579,783 StaticChain
 *  4,101,454,600   2208 1,857,542     53  77,385,935 NishioChain
 *  4,570,265,400   2155 2,120,772   2972   1,537,774 DynamicChain
 *     83,643,500     11 7,603,954     11   7,603,954 DynamicPlus
 * 65,839,480,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,342,815,900 (01:53)  77,367,109
 *
 * NOTES:
 * 1. 1:53 is two seconds over par, which is disappointing, but the hinter I
 *    fixed, FrankenSwordfish, is about what I expected, so its all good.
 * 2. DiufSudoku_V6_30.243.2023-09-25.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.244 2023-09-29</b> I build because I should, every few days,
 * when I am working on SE.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,335,200 109157       222  57897         420 NakedSingle
 *     39,522,000  51260       771  16335       2,419 HiddenSingle
 *     84,207,200  34925     2,411  24202       3,479 Locking
 *     76,973,400  23554     3,267   7832       9,828 NakedPair
 *     64,688,200  21486     3,010   8244       7,846 HiddenPair
 *    120,731,100  19413     6,219   1598      75,551 NakedTriple
 *    100,863,900  19014     5,304    985     102,399 HiddenTriple
 *     58,357,900  18820     3,100   1330      43,878 TwoStringKite
 *     49,113,600  17490     2,808    449     109,384 Swampfish
 *     70,749,500  17300     4,089    900      78,610 W_Wing
 *     80,515,300  16674     4,828    529     152,202 XY_Wing
 *     50,812,500  16316     3,114    269     188,894 XYZ_Wing
 *     51,088,200  16064     3,180    318     160,654 Skyscraper
 *     89,047,300  15899     5,600    569     156,497 EmptyRectangle
 *     60,136,900  15330     3,922    228     263,758 Swordfish
 *    805,784,300  15267    52,779   1083     744,029 URT
 *     84,683,800  14505     5,838      8  10,585,475 Jellyfish
 *    150,018,600  14503    10,343    117   1,282,210 NakedQuad
 *    115,424,500  14481     7,970      1 115,424,500 HiddenQuad
 *    291,910,100  14480    20,159    180   1,621,722 Coloring
 *    888,801,000  14361    61,889    736   1,207,610 XColoring
 *  1,888,306,000  14100   133,922 119207      15,840 GEM
 *  2,341,315,000  12789   183,072   2055   1,139,326 BigWings
 *  3,475,924,200  11637   298,695   2481   1,401,017 DeathBlossom
 *  5,217,753,300   9546   546,590   2039   2,558,976 ALS_XZ
 *  6,217,278,900   8288   750,154   2716   2,289,130 ALS_Wing
 *  4,340,346,100   6149   705,862   2053   2,114,148 ALS_Chain
 *  1,296,754,700   4545   285,314     34  38,139,844 SueDeCoq
 * 14,395,013,200   4539 3,171,406    331  43,489,465 FrankenSwordfish
 *  8,346,133,000   4278 1,950,942    414  20,159,741 UnaryChain
 *  5,580,971,100   3935 1,418,289   3626   1,539,153 StaticChain
 *  4,057,641,100   2208 1,837,699     53  76,559,266 NishioChain
 *  4,568,285,300   2155 2,119,853   2972   1,537,108 DynamicChain
 *     81,643,600     11 7,422,145     11   7,422,145 DynamicPlus
 * 65,165,130,000
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,620,621,600 (01:53)  77,556,738
 *
 * NOTES:
 * 1. 1:53 is same as last.
 * 2. DiufSudoku_V6_30.244.2023-09-29.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.245 2023-10-07</b> I build because I should. I have been making
 * stuff faster. Split FrutantFisherman into FrankenFisherman and
 * MutantFisherman.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     25,374,800 109157       232  57897         438 NakedSingle
 *     40,071,600  51260       781  16335       2,453 HiddenSingle
 *     85,936,700  34925     2,460  24202       3,550 Locking
 *     84,075,900  23554     3,569   7832      10,734 NakedPair
 *     66,681,200  21486     3,103   8244       8,088 HiddenPair
 *    125,810,100  19413     6,480   1598      78,729 NakedTriple
 *    103,133,000  19014     5,424    985     104,703 HiddenTriple
 *     61,074,300  18820     3,245   1330      45,920 TwoStringKite
 *     50,807,400  17490     2,904    449     113,156 Swampfish
 *     71,860,700  17300     4,153    900      79,845 W_Wing
 *     86,218,100  16674     5,170    529     162,983 XY_Wing
 *     51,940,900  16316     3,183    269     193,088 XYZ_Wing
 *     47,853,700  16064     2,978    318     150,483 Skyscraper
 *     93,913,000  15899     5,906    569     165,049 EmptyRectangle
 *     61,265,200  15330     3,996    228     268,707 Swordfish
 *    828,688,300  15267    54,279   1083     765,178 URT
 *     86,339,100  14505     5,952      8  10,792,387 Jellyfish
 *    152,199,700  14503    10,494    117   1,300,852 NakedQuad
 *    122,655,300  14481     8,470      1 122,655,300 HiddenQuad
 *    307,693,800  14480    21,249    180   1,709,410 Coloring
 *    904,071,100  14361    62,953    736   1,228,357 XColoring
 *  1,947,469,800  14100   138,118 119207      16,336 GEM
 *  2,531,776,000  12789   197,965   2055   1,232,007 BigWings
 *  3,525,468,800  11637   302,953   2481   1,420,987 DeathBlossom
 *  5,315,756,800   9546   556,856   2039   2,607,041 ALS_XZ
 *  6,361,321,100   8288   767,533   2716   2,342,165 ALS_Wing
 *  4,382,638,900   6149   712,740   2053   2,134,748 ALS_Chain
 *  1,289,192,400   4545   283,650     34  37,917,423 SueDeCoq
 * 12,869,237,800   4539 2,835,258    331  38,879,872 FrankenSwordfish
 *  8,352,593,300   4278 1,952,452    414  20,175,346 UnaryChain
 *  5,839,403,300   3935 1,483,965   3626   1,610,425 StaticChain
 *  4,834,339,600   2208 2,189,465     53  91,213,954 NishioChain
 *  4,899,130,900   2155 2,273,378   2972   1,648,428 DynamicChain
 *     86,290,800     11 7,844,618     11   7,844,618 DynamicPlus
 * 65,692,283,400
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  113,744,421,300 (01:53)  77,641,243
 *
 * NOTES:
 * 1. 1:53 is same as previous, which is dissapointing. I am trying and failing
 *    to make it faster. Looks like I have reached (or exceeded) my limits.
 * 2. DiufSudoku_V6_30.245.2023-10-07.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.246 2023-10-12</b> Made complex Fisherman and Chainers faster.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,299,600 109157       222  57897         419 NakedSingle
 *     38,613,600  51260       753  16335       2,363 HiddenSingle
 *     78,049,100  34925     2,234  24202       3,224 Locking
 *     81,926,900  23554     3,478   7832      10,460 NakedPair
 *     65,588,500  21486     3,052   8244       7,955 HiddenPair
 *    123,128,700  19413     6,342   1598      77,051 NakedTriple
 *    100,345,700  19014     5,277    985     101,873 HiddenTriple
 *     58,738,400  18820     3,121   1330      44,164 TwoStringKite
 *     48,749,900  17490     2,787    449     108,574 Swampfish
 *     70,399,300  17300     4,069    900      78,221 W_Wing
 *     80,070,800  16674     4,802    529     151,362 XY_Wing
 *     51,780,400  16316     3,173    269     192,492 XYZ_Wing
 *     49,257,600  16064     3,066    318     154,898 Skyscraper
 *     89,885,600  15899     5,653    569     157,971 EmptyRectangle
 *     60,021,500  15330     3,915    228     263,252 Swordfish
 *    821,844,000  15267    53,831   1083     758,858 URT
 *     84,918,300  14505     5,854      8  10,614,787 Jellyfish
 *    148,449,900  14503    10,235    117   1,268,802 NakedQuad
 *    118,074,900  14481     8,153      1 118,074,900 HiddenQuad
 *    295,891,800  14480    20,434    180   1,643,843 Coloring
 *    887,648,400  14361    61,809    736   1,206,044 XColoring
 *  1,858,262,600  14100   131,791 119207      15,588 GEM
 *  2,416,341,400  12789   188,939   2055   1,175,835 BigWings
 *  3,431,961,400  11637   294,918   2481   1,383,297 DeathBlossom
 *  5,264,189,800   9546   551,455   2039   2,581,750 ALS_XZ
 *  6,179,155,700   8288   745,554   2716   2,275,094 ALS_Wing
 *  4,218,439,500   6149   686,036   2053   2,054,768 ALS_Chain
 *  1,265,111,600   4545   278,352     34  37,209,164 SueDeCoq
 * 12,915,961,400   4539 2,845,552    331  39,021,031 FrankenSwordfish
 *  7,951,085,300   4278 1,858,598    414  19,205,520 UnaryChain
 *  5,543,402,300   3935 1,408,742   3626   1,528,792 StaticChain
 *  3,941,328,000   2208 1,785,021     53  74,364,679 NishioChain
 *  4,572,578,800   2155 2,121,846   2972   1,538,552 DynamicChain
 *     82,037,000     11 7,457,909     11   7,457,909 DynamicPlus
 * 63,017,537,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  109,267,328,300 (01:49)  74,585,207
 *
 * NOTES:
 * 1. 1:49 is four seconds faster, which is a relief. Previous releases where
 *    two seconds slower than the previous best, 1:51, so yeah, just a relief.
 * 2. DiufSudoku_V6_30.246.2023-10-12.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.247 2023-10-13</b> Made AlsWing faster using the "All" RCCs plus
 * starts and ends, as per AlsChain. This version is ONLY suitable ONLY for use
 * with AlsChain, else getting the "All" RCCs is a waste of time. With AlsChain
 * it matters not when we do the upgrade, so its faster. I ship it, then think
 * about how to select which AlsWing is used depending on AlsChains wantedness.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,759,300 109057       227  57819         428 NakedSingle
 *     28,000,200  51238       546  16321       1,715 HiddenSingle
 *     79,854,700  34917     2,286  24205       3,299 Locking
 *     81,077,000  23545     3,443   7841      10,340 NakedPair
 *     63,563,300  21477     2,959   8234       7,719 HiddenPair
 *    122,047,300  19412     6,287   1605      76,041 NakedTriple
 *     95,356,800  19012     5,015    986      96,710 HiddenTriple
 *     58,977,700  18818     3,134   1327      44,444 TwoStringKite
 *     49,514,900  17491     2,830    452     109,546 Swampfish
 *     71,115,000  17299     4,110    898      79,192 W_Wing
 *     81,727,300  16675     4,901    535     152,761 XY_Wing
 *     50,196,600  16313     3,077    269     186,604 XYZ_Wing
 *     47,086,600  16061     2,931    318     148,071 Skyscraper
 *     88,530,700  15896     5,569    574     154,234 EmptyRectangle
 *     62,032,500  15322     4,048    228     272,072 Swordfish
 *    800,550,300  15259    52,464   1081     740,564 URT
 *     85,377,300  14498     5,888      8  10,672,162 Jellyfish
 *    144,342,400  14496     9,957    117   1,233,695 NakedQuad
 *    113,177,300  14474     7,819      1 113,177,300 HiddenQuad
 *    288,254,900  14473    19,916    183   1,575,163 Coloring
 *    873,471,700  14352    60,860    732   1,193,267 XColoring
 *  1,849,279,500  14094   131,210 120135      15,393 GEM
 *  2,475,883,600  12780   193,731   2047   1,209,518 BigWings
 *  3,407,252,200  11636   292,819   2495   1,365,632 DeathBlossom
 *  5,187,191,900   9533   544,130   2043   2,539,007 ALS_XZ
 *  1,540,984,600   8272   186,289   2707     569,259 ALS_Wing
 *  4,111,391,100   6149   668,627   2053   2,002,625 ALS_Chain
 *  1,326,190,900   4545   291,791     34  39,005,614 SueDeCoq
 * 12,713,867,400   4539 2,801,028    331  38,410,475 FrankenSwordfish
 *  8,316,000,900   4278 1,943,899    414  20,086,958 UnaryChain
 *  5,802,423,600   3935 1,474,567   3626   1,600,227 StaticChain
 *  4,035,558,400   2208 1,827,698     53  76,142,611 NishioChain
 *  4,551,642,700   2155 2,112,131   2972   1,531,508 DynamicChain
 *     82,276,400     11 7,479,672     11   7,479,672 DynamicPlus
 * 58,708,957,000
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  104,833,707,900 (01:44)  71,558,844
 *
 * NOTES:
 * 1. 1:44 is five seconds faster again, which is cause for real celebration.
 *    I'll go and have a durry.
 * 2. DiufSudoku_V6_30.247.2023-10-13.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.248 2023-10-16</b> Idx mask : a long (54bit) and b int (27bit).
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     27,061,500 109057       248  57819         468 NakedSingle
 *     29,819,400  51238       581  16321       1,827 HiddenSingle
 *     77,291,600  34917     2,213  24205       3,193 Locking
 *     82,015,800  23545     3,483   7841      10,459 NakedPair
 *     63,606,800  21477     2,961   8234       7,724 HiddenPair
 *    122,885,700  19412     6,330   1605      76,564 NakedTriple
 *     98,808,400  19012     5,197    986     100,211 HiddenTriple
 *     60,183,500  18818     3,198   1327      45,353 TwoStringKite
 *     49,483,300  17491     2,829    452     109,476 Swampfish
 *     68,797,500  17299     3,976    898      76,611 W_Wing
 *     78,493,700  16675     4,707    535     146,717 XY_Wing
 *     50,631,500  16313     3,103    269     188,221 XYZ_Wing
 *     49,398,900  16061     3,075    318     155,342 Skyscraper
 *     93,303,500  15896     5,869    574     162,549 EmptyRectangle
 *     60,957,700  15322     3,978    228     267,358 Swordfish
 *    809,591,000  15259    53,056   1081     748,927 URT
 *     85,394,400  14498     5,890      8  10,674,300 Jellyfish
 *    148,627,500  14496    10,253    117   1,270,320 NakedQuad
 *    116,578,700  14474     8,054      1 116,578,700 HiddenQuad
 *    274,926,000  14473    18,995    183   1,502,327 Coloring
 *    777,058,000  14352    54,142    732   1,061,554 XColoring
 *  1,713,680,300  14094   121,589 120135      14,264 GEM
 *  2,431,272,600  12780   190,240   2047   1,187,724 BigWings
 *  3,361,147,600  11636   288,857   2495   1,347,153 DeathBlossom
 *  4,270,103,800   9533   447,928   2043   2,090,114 ALS_XZ
 *  1,309,942,900   8272   158,358   2707     483,909 ALS_Wing
 *  3,774,206,500   6149   613,791   2053   1,838,386 ALS_Chain
 *  1,219,012,800   4545   268,209     34  35,853,317 SueDeCoq
 * 11,703,307,000   4539 2,578,388    331  35,357,422 FrankenSwordfish
 *  8,319,944,400   4278 1,944,821    414  20,096,484 UnaryChain
 *  5,843,964,200   3935 1,485,124   3626   1,611,683 StaticChain
 *  3,917,230,200   2208 1,774,107     53  73,910,003 NishioChain
 *  4,429,474,700   2155 2,055,440   2972   1,490,401 DynamicChain
 *     75,905,600     11 6,900,509     11   6,900,509 DynamicPlus
 * 55,594,107,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  99,804,517,400 (01:39)  68,125,950
 *
 * NOTES:
 * 1. 1:39 is five seconds faster again, again: more cause for celebration!
 *    I'll go and have another durry.
 * 2. DiufSudoku_V6_30.248.2023-10-16.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.249 2023-10-18</b> Complex fisherman are marginally faster.
 * I have an idea, so I build now for a backup. I am going to try to make each
 * hinter search only the "dirty" regions (modified since my last search). Its
 * going to be tricky. Every hinter has its own definition of dirty, so I guess
 * a subscriber event messaging service will be required.
 * <pre>
 * This is NOT a normal build-log. It's a KrakenJellyfish with Kraken Type 1's
 * enabled, which I don't do often because it's so slow, so I just thought it
 * might be a good idea to document it in a build-log, for future reference.
 *           time(ns)  calls     time/call  elims      time/elim hinter
 *         23,594,300 108957           216  57656            409 NakedSingle
 *         29,158,700  51301           568  16341          1,784 HiddenSingle
 *         80,820,500  34960         2,311  24110          3,352 Locking
 *         82,139,200  23622         3,477   7847         10,467 NakedPair
 *         62,964,400  21551         2,921   8244          7,637 HiddenPair
 *        124,922,800  19482         6,412   1605         77,833 NakedTriple
 *         96,532,900  19081         5,059    970         99,518 HiddenTriple
 *         60,993,600  18892         3,228   1317         46,312 TwoStringKite
 *         50,503,100  17575         2,873    453        111,485 Swampfish
 *         71,984,500  17386         4,140    894         80,519 W_Wing
 *         79,312,900  16762         4,731    518        153,113 XY_Wing
 *         53,931,200  16415         3,285    268        201,235 XYZ_Wing
 *         46,830,200  16166         2,896    328        142,775 Skyscraper
 *         90,598,600  15994         5,664    552        164,127 EmptyRectangle
 *         62,341,500  15442         4,037    227        274,632 Swordfish
 *        698,743,800  15380        45,431   1098        636,378 URT
 *         86,314,900  14616         5,905      8     10,789,362 Jellyfish
 *        153,481,200  14613        10,503    118      1,300,688 NakedQuad
 *        117,632,500  14590         8,062      1    117,632,500 HiddenQuad
 *        267,535,200  14589        18,338    187      1,430,669 Coloring
 *        786,431,400  14470        54,349    741      1,061,310 XColoring
 *      1,705,584,500  14211       120,018 123598         13,799 GEM
 *      2,469,909,700  12871       191,897   2121      1,164,502 BigWings
 *      3,356,113,400  11688       287,141   2546      1,318,190 DeathBlossom
 *      4,153,946,600   9538       435,515   2014      2,062,535 ALS_XZ
 *      1,334,112,400   8313       160,485   2726        489,402 ALS_Wing
 *      3,860,511,300   6175       625,184   2044      1,888,704 ALS_Chain
 *      1,221,092,500   4575       266,905     31     39,390,080 SueDeCoq
 *    187,602,476,900   4569    41,059,854    408    459,809,992 MutantJellyfish
 * 35,961,585,457,800   4209 8,543,973,736   2561 14,042,009,159 KrakenJellyfish
 *      3,856,946,200   2120     1,819,314      0              0 UnaryChain
 *      3,691,000,900   2120     1,741,038    510      7,237,256 StaticChain
 *      3,276,122,200   1853     1,768,009      0              0 NishioChain
 *      3,754,068,500   1853     2,025,940   2427      1,546,793 DynamicChain
 *        108,092,600     11     9,826,600     11      9,826,600 DynamicPlus
 * 36,185,102,202,900
 *  pzls          total (ns) (HH:mm:ss)       each (ns)
 *  1465  36,229,304,679,200 (10:03:49)  24,729,900,804
 *
 * NOTES:
 * 1. 10 hours is approximately ten hours too long. You fix it. Kraken sux!
 * 2. DiufSudoku_V6_30.249.2023-10-18.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.250 2023-10-19</b> Coloring and Chainers (RAM hoggers) now
 * release there buffers/sets when not in use. This is actually slower but
 * reduces RAM usage, so the GC no longer goes into overdrive.
 * <pre>
 *       time(ns)  calls time/call elims  time/elim hinter
 *     26,253,600 118963       220 65167        402 NakedSingle
 *     33,751,500  53796       627 17369      1,943 HiddenSingle
 *     81,198,300  36427     2,229 24523      3,311 Locking
 *     86,645,000  24787     3,495  7983     10,853 NakedPair
 *     67,940,600  22620     3,003  8347      8,139 HiddenPair
 *    132,619,400  20477     6,476  1623     81,712 NakedTriple
 *    103,972,300  20063     5,182  1001    103,868 HiddenTriple
 *     63,076,400  19861     3,175  1379     45,740 TwoStringKite
 *     53,729,700  18482     2,907   492    109,206 Swampfish
 *     75,271,400  18269     4,120   969     77,679 W_Wing
 *     86,559,300  17595     4,919   562    154,020 XY_Wing
 *     52,728,700  17217     3,062   279    188,991 XYZ_Wing
 *     52,167,000  16955     3,076   331    157,604 Skyscraper
 *     93,054,500  16779     5,545   597    155,870 EmptyRectangle
 *     64,258,100  16182     3,970   230    279,383 Swordfish
 *    834,660,600  16117    51,787  1125    741,920 URT
 *     90,844,100  15329     5,926     5 18,168,820 Jellyfish
 *    156,452,600  15328    10,206   122  1,282,398 NakedQuad
 *    119,484,000  15304     7,807     0          0 HiddenQuad
 *    333,407,300  15304    21,785   195  1,709,781 Coloring
 *    983,588,100  15172    64,829   769  1,279,048 XColoring
 *  1,364,858,100  14901    91,595 35591     38,348 Medusa3D
 *  2,529,576,100  13872   182,351  2574    982,741 BigWings
 *  3,670,707,900  12402   295,977  2942  1,247,691 DeathBlossom
 *  4,355,668,500   9919   439,123  2133  2,042,038 ALS_XZ
 *  1,404,065,600   8586   163,529  2892    485,499 ALS_Wing
 *  4,039,936,300   6306   640,649  2173  1,859,151 ALS_Chain
 *  1,316,871,500   4609   285,717    37 35,591,121 SueDeCoq
 * 11,735,329,500   4602 2,550,049   341 34,414,456 FrankenSwordfish
 *  7,992,236,000   4332 1,844,929   413 19,351,661 UnaryChain
 *  5,914,289,700   3983 1,484,883  3788  1,561,322 StaticChain
 *  3,921,651,200   2211 1,773,700    53 73,993,418 NishioChain
 *  4,132,063,000   2158 1,914,765  2984  1,384,739 DynamicChain
 *     83,805,800     11 7,618,709    11  7,618,709 DynamicPlus
 * 56,052,721,700
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  102,397,207,900 (01:42)  69,895,705
 *
 * NOTES:
 * 1. 1:42 is three seconds slower, but RAM use is dramatically reduced.
 * 2. DiufSudoku_V6_30.250.2023-10-19.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * NOTE: 251 and 252 where lost when I reverted to 250. So 253 is next build.
 * <hr>
 * <p>
 * <b>KRC 6.30.253 2023-10-21</b> Imbedded Chainers now initialise and deinit-
 * ialise with there parent ChainerDynamic, not individually, for speed. FYI, I
 * reverted to 250, because release testing of 252 revealed Generate is broken.
 * AlsWing now checks its eliminations exist. 251 was a bust. No idea. But 250
 * was only two days ago and it took all of two hours to repeat similar changes
 * to 252 and test them. Hence 253 I am Stan.
 * <pre>
 *       time(ns)  calls time/call elims  time/elim hinter
 *     27,162,500 119065       228 65204        416 NakedSingle
 *     30,600,000  53861       568 17374      1,761 HiddenSingle
 *     80,595,100  36487     2,208 24530      3,285 Locking
 *     88,928,600  24841     3,579  7981     11,142 NakedPair
 *     68,620,500  22669     3,027  8349      8,219 HiddenPair
 *    129,905,000  20516     6,331  1625     79,941 NakedTriple
 *    107,230,900  20102     5,334  1002    107,016 HiddenTriple
 *     64,173,000  19899     3,224  1382     46,434 TwoStringKite
 *     53,396,800  18517     2,883   500    106,793 Swampfish
 *     76,204,800  18299     4,164   963     79,132 W_Wing
 *     88,107,000  17631     4,997   568    155,117 XY_Wing
 *     53,805,800  17248     3,119   279    192,852 XYZ_Wing
 *     52,480,800  16986     3,089   329    159,516 Skyscraper
 *     98,415,700  16812     5,853   601    163,753 EmptyRectangle
 *     66,847,000  16211     4,123   231    289,380 Swordfish
 *    836,018,800  16145    51,781  1121    745,779 URT
 *     94,001,500  15358     6,120     5 18,800,300 Jellyfish
 *    153,034,600  15357     9,965   122  1,254,381 NakedQuad
 *    119,833,500  15333     7,815     0          0 HiddenQuad
 *    337,039,100  15333    21,981   195  1,728,405 Coloring
 *    977,971,700  15201    64,336   770  1,270,093 XColoring
 *  1,384,308,200  14929    92,726 35462     39,036 Medusa3D
 *  2,578,239,200  13903   185,444  2583    998,156 BigWings
 *  3,602,835,200  12427   289,919  2954  1,219,646 DeathBlossom
 *  4,423,636,800   9935   445,257  2144  2,063,263 ALS_XZ
 *  1,395,078,500   8593   162,350  2892    482,392 ALS_Wing
 *  4,009,830,900   6310   635,472  2176  1,842,753 ALS_Chain
 *  1,290,585,700   4610   279,953    37 34,880,694 SueDeCoq
 * 11,989,310,800   4603 2,604,673   339 35,366,698 FrankenSwordfish
 *  8,032,323,800   4335 1,852,900   424 18,944,159 UnaryChain
 *  4,900,970,200   3984 1,230,163  3478  1,409,134 StaticChain
 *  4,040,108,700   2243 1,801,207    53 76,228,466 NishioChain
 *  4,286,304,200   2190 1,957,216  3007  1,425,442 DynamicChain
 *     69,730,600     11 6,339,145    11  6,339,145 DynamicPlus
 * 55,607,635,500
 * pzls       total (ns) (mm:ss)   each (ns)
 * 1465  103,158,806,400 (01:43)  70,415,567
 *
 * NOTES:
 * 1. 1:43 is a second slower. Meh. This change doesn't effect the batch much
 *    because only two puzzles require DynamicPlus, and none NestedUnary.
 * 2. DiufSudoku_V6_30.253.2023-10-21.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.254 2023-10-25</b> freed more heapspace, and made many hinters a
 * bit faster with post-tested-loops where possible. XColoring and GEM faster.
 * If you want the batch to be 10 seconds faster then Log.MODE=NORMAL.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     23,925,100 109165       219  57885         413 NakedSingle
 *     27,760,100  51280       541  16354       1,697 HiddenSingle
 *     75,979,100  34926     2,175  24196       3,140 Locking
 *     80,662,600  23557     3,424   7835      10,295 NakedPair
 *     69,546,800  21490     3,236   8230       8,450 HiddenPair
 *    121,402,500  19422     6,250   1603      75,734 NakedTriple
 *    100,640,700  19025     5,289    986     102,069 HiddenTriple
 *     60,339,800  18831     3,204   1329      45,402 TwoStringKite
 *     50,155,700  17502     2,865    458     109,510 Swampfish
 *     70,256,600  17308     4,059    886      79,296 W_Wing
 *     76,644,200  16693     4,591    541     141,671 XY_Wing
 *     50,796,000  16328     3,110    269     188,832 XYZ_Wing
 *     51,157,100  16076     3,182    314     162,920 Skyscraper
 *     89,018,200  15914     5,593    577     154,277 EmptyRectangle
 *     61,594,600  15337     4,016    227     271,341 Swordfish
 *    801,604,100  15274    52,481   1082     740,854 URT
 *     87,402,300  14512     6,022      8  10,925,287 Jellyfish
 *    143,241,200  14510     9,871    117   1,224,283 NakedQuad
 *    115,994,700  14488     8,006      1 115,994,700 HiddenQuad
 *    322,615,800  14487    22,269    180   1,792,310 Coloring
 *    958,645,300  14369    66,716    730   1,313,212 XColoring
 *  1,505,878,800  14112   106,709 119429      12,608 GEM
 *  2,285,035,100  12791   178,643   2053   1,113,022 BigWings
 *  3,346,209,800  11643   287,400   2501   1,337,948 DeathBlossom
 *  4,105,267,100   9534   430,592   2052   2,000,617 ALS_XZ
 *  1,291,784,700   8266   156,276   2690     480,217 ALS_Wing
 *  3,790,287,100   6153   616,006   2051   1,848,019 ALS_Chain
 *  1,203,195,800   4550   264,438     34  35,388,111 SueDeCoq
 * 10,347,693,000   4544 2,277,221    329  31,451,954 FrankenSwordfish
 *  7,800,448,800   4285 1,820,408    423  18,440,777 UnaryChain
 *  4,833,693,800   3942 1,226,203   3340   1,447,213 StaticChain
 *  3,385,357,100   2240 1,511,320     54  62,691,798 NishioChain
 *  4,076,889,800   2186 1,864,999   2994   1,361,686 DynamicChain
 *     76,085,900     11 6,916,900     11   6,916,900 DynamicPlus
 * 51,487,209,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  98,219,132,200 (01:38)  67,043,776
 *
 * NOTES:
 * 1. 1:38 is 5 seconds faster. Mainly post-tested loops, I think.
 * 2. DiufSudoku_V6_30.254.2023-10-25.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.255 2023-10-27</b> Finished eliminating hang-over large sets and
 * buffers from all hinters, to reduce heap usage by limiting the amount held
 * by currently-not-in-use hinters.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     24,689,740 109165       226  57885         426 NakedSingle
 *     28,539,969  51280       556  16354       1,745 HiddenSingle
 *     76,639,171  34926     2,194  24196       3,167 Locking
 *     80,020,324  23557     3,396   7835      10,213 NakedPair
 *     67,900,406  21490     3,159   8230       8,250 HiddenPair
 *    122,152,196  19422     6,289   1603      76,202 NakedTriple
 *    100,218,682  19025     5,267    986     101,641 HiddenTriple
 *     60,667,272  18831     3,221   1329      45,648 TwoStringKite
 *     48,279,596  17502     2,758    458     105,413 Swampfish
 *     69,356,195  17308     4,007    886      78,280 W_Wing
 *     77,186,532  16693     4,623    541     142,673 XY_Wing
 *     50,743,335  16328     3,107    269     188,636 XYZ_Wing
 *     50,500,031  16076     3,141    314     160,828 Skyscraper
 *     91,043,136  15914     5,720    577     157,787 EmptyRectangle
 *     60,615,683  15337     3,952    227     267,029 Swordfish
 *    777,255,108  15274    50,887   1082     718,350 URT
 *     85,946,756  14512     5,922      8  10,743,344 Jellyfish
 *    144,919,890  14510     9,987    117   1,238,631 NakedQuad
 *    113,083,628  14488     7,805      1 113,083,628 HiddenQuad
 *    320,384,790  14487    22,115    180   1,779,915 Coloring
 *    939,835,185  14369    65,407    730   1,287,445 XColoring
 *  1,506,235,091  14112   106,734 119429      12,611 GEM
 *  2,175,568,583  12791   170,085   2053   1,059,702 BigWings
 *  3,276,905,001  11643   281,448   2501   1,310,237 DeathBlossom
 *  4,076,583,494   9534   427,583   2052   1,986,639 ALS_XZ
 *  1,268,098,856   8266   153,411   2690     471,412 ALS_Wing
 *  3,783,649,212   6153   614,927   2051   1,844,782 ALS_Chain
 *  1,203,056,105   4550   264,407     34  35,384,003 SueDeCoq
 *  9,576,400,087   4544 2,107,482    329  29,107,599 FrankenSwordfish
 *  9,047,530,457   4285 2,111,442    423  21,388,960 UnaryChain
 *  4,726,886,533   3942 1,199,108   3340   1,415,235 StaticChain
 *  3,408,034,089   2240 1,521,443     54  63,111,742 NishioChain
 *  4,072,128,941   2186 1,862,822   2994   1,360,096 DynamicChain
 *     82,486,297     11 7,498,754     11   7,498,754 DynamicPlus
 * 51,593,540,371
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  96,706,800,200 (01:36)  66,011,467
 *
 * NOTES:
 * 1. 1:36 is 2 seconds faster again. Mainly post-tested loops, I think.
 * 2. DiufSudoku_V6_30.255.2023-10-27.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.256 2023-10-28</b> Split KrackFisherman of KrakenFisherman to do
 * KrakenJellyfish unpredicated, leaving predicated version in KrakenFisherman.
 * Also HACK changeCands finds ~60% of elims in ~25% of time. She'll do.
 * <pre>
 *          time(ns)  calls   time/call  elims     time/elim hinter
 *        13,762,000 109179         126  57798           238 NakedSingle
 *        25,525,300  51381         496  16373         1,558 HiddenSingle
 *        70,579,200  35008       2,016  24145         2,923 Locking
 *        73,558,100  23656       3,109   7821         9,405 NakedPair
 *        61,947,600  21586       2,869   8240         7,517 HiddenPair
 *       120,691,300  19518       6,183   1591        75,858 NakedTriple
 *        96,714,500  19123       5,057    990        97,691 HiddenTriple
 *        48,232,600  18930       2,547   1320        36,539 TwoStringKite
 *        42,566,800  17610       2,417    446        95,441 Swampfish
 *        64,464,200  17421       3,700    892        72,269 W_Wing
 *        71,829,600  16804       4,274    526       136,558 XY_Wing
 *        48,052,000  16453       2,920    264       182,015 XYZ_Wing
 *        41,808,500  16207       2,579    314       133,148 Skyscraper
 *        88,408,500  16044       5,510    568       155,648 EmptyRectangle
 *        59,571,900  15476       3,849    230       259,008 Swordfish
 *       729,013,600  15413      47,298   1087       670,665 URT
 *        83,726,600  14654       5,713      8    10,465,825 Jellyfish
 *       146,802,100  14651      10,019    122     1,203,295 NakedQuad
 *       115,614,200  14627       7,904      1   115,614,200 HiddenQuad
 *       279,313,600  14626      19,097    172     1,623,916 Coloring
 *       876,747,600  14513      60,411    756     1,159,719 XColoring
 *     1,454,594,600  14254     102,048 121179        12,003 GEM
 *     2,156,045,200  12927     166,786   2082     1,035,564 BigWings
 *     3,737,260,400  11768     317,578   2516     1,485,397 DeathBlossom
 *     3,922,044,300   9646     406,597   2049     1,914,126 ALS_XZ
 *     1,308,294,000   8388     155,972   2733       478,702 ALS_Wing
 *     3,597,552,000   6242     576,346   2085     1,725,444 ALS_Chain
 *     1,220,204,300   4614     264,456     31    39,361,429 SueDeCoq
 *     9,566,589,300   4608   2,076,082    332    28,815,028 FrankenSwordfish
 * 1,690,071,809,100   4346 388,879,845   1673 1,010,204,309 KrakenJellyfish
 *     3,950,554,500   2956   1,336,452     36   109,737,625 UnaryChain
 *     3,339,011,800   2921   1,143,105   1688     1,978,087 StaticChain
 *     2,119,685,500   2025   1,046,758     12   176,640,458 NishioChain
 *     2,900,626,500   2013   1,440,947   2695     1,076,299 DynamicChain
 *        77,478,600     11   7,043,509     11     7,043,509 DynamicPlus
 * 1,732,580,679,900
 * pzls        total (ns) (mm:ss)      each (ns)
 * 1465 1,762,570,627,600 (29:22)  1,203,119,882
 *
 * NOTES:
 * 1. 22:29 is crap, but heaps faster than 10 HOURS, thanx to a HACK, so keep
 *    ya pants on. It finds ~60% elims in ~25% of previous time, so that's it.
 *    SudokuExplainer is now usable. Idiot users can't "break it" just by
 *    selecting a slow Tech, out of curiosity. Curiosity should be encouraged,
 *    rewarded even. So that's it from me. I know not how to do it better. Next
 *    week I might read a book. Dawkins Science in the Soul has been sitting on
 *    my bedside table for months, untouched. Too busy following my religion.
 *    PRE: 6,461,933,880,500  4300  1,502,775,321  2660  2,429,298,451  KrakenJellyfish
 *    PST: 1,690,071,809,100  4346    388,879,845  1673  1,010,204,309  KrakenJellyfish
 * 2. DiufSudoku_V6_30.256.2023-10-28.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.257 2023-11-02</b> diuf.sudoku.solver.hinters.table exposes the
 * KrakenTables as Tables, to do chaining. The slow part of Kraken is fish, not
 * chains. This faster than Unary/StaticChains for less hints. No bidirectional
 * cycles, but most are found by coloring. No contradictions, which are rare in
 * static chains.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     26,760,239 109401       244  58291         459 NakedSingle
 *     30,205,186  51110       590  16402       1,841 HiddenSingle
 *     79,146,955  34708     2,280  24181       3,273 Locking
 *     83,067,325  23357     3,556   7845      10,588 NakedPair
 *     68,601,186  21290     3,222   8272       8,293 HiddenPair
 *    123,014,693  19206     6,405   1613      76,264 NakedTriple
 *    101,585,039  18800     5,403    983     103,341 HiddenTriple
 *     61,402,275  18608     3,299   1327      46,271 TwoStringKite
 *     48,420,203  17281     2,801    450     107,600 Swampfish
 *     71,634,767  17086     4,192    912      78,546 W_Wing
 *     80,904,662  16452     4,917    532     152,076 XY_Wing
 *     52,075,318  16090     3,236    268     194,310 XYZ_Wing
 *     49,698,667  15839     3,137    315     157,773 Skyscraper
 *     94,644,620  15673     6,038    587     161,234 EmptyRectangle
 *     61,230,526  15086     4,058    227     269,738 Swordfish
 *    766,736,619  15024    51,034   1099     697,667 URT
 *     85,263,486  14256     5,980     11   7,751,226 Jellyfish
 *    149,030,013  14253    10,456    114   1,307,280 NakedQuad
 *    117,605,036  14232     8,263      1 117,605,036 HiddenQuad
 *    380,037,776  14231    26,704    188   2,021,477 Coloring
 *    965,488,227  14104    68,454    792   1,219,050 XColoring
 *  1,528,764,738  13836   110,491 116569      13,114 GEM
 *  2,443,013,776  12529   194,988   2036   1,199,908 BigWings
 *  3,248,673,913  11387   285,296   2490   1,304,688 DeathBlossom
 *  4,032,204,871   9288   434,130   2027   1,989,247 ALS_XZ
 *  1,262,777,224   8042   157,022   2634     479,414 ALS_Wing
 *  3,503,667,453   5973   586,584   2024   1,731,060 ALS_Chain
 *  1,232,621,926   4394   280,523     30  41,087,397 SueDeCoq
 *  7,098,920,065   4389 1,617,434    158  44,929,873 FrankenSwordfish
 *  6,192,480,347   4270 1,450,229   2455   2,522,395 Reduction
 *  4,837,959,594   2399 2,016,656   3351   1,443,736 DynamicChain
 *     92,255,401     11 8,386,854     11   8,386,854 DynamicPlus
 * 38,969,892,126
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  82,728,370,399 (01:22)  56,469,877
 *
 * NOTES:
 * 1. 1:22 is 14 seconds faster than last standard run, but this run is not
 *    standard. It uses Reduction instead of UnaryChain and StaticChain.
 * 2. DiufSudoku_V6_30.257.2023-11-02.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.258 2023-11-05</b> New tables.TableAbduction does reduction by
 * finding consequences itself, where TableReduction relies on pre-calculated
 * elims. This approach finds many more elims, but is much slower overall. FYI,
 * I suspect Tables elims must be incomplete, but I can't see how. Either that
 * or there's a mistake hidden in TableReduction. I am NOT perfect. Sigh.
 * <pre>
 *       time(ns)  calls  time/call  elims  time/elim hinter
 *     26,769,500 108780        246  58032        461 NakedSingle
 *     28,668,400  50748        564  16225      1,766 HiddenSingle
 *     78,405,000  34523      2,271  24180      3,242 Locking
 *     84,608,900  23168      3,651   7832     10,802 NakedPair
 *     76,350,000  21106      3,617   8226      9,281 HiddenPair
 *    122,883,200  19038      6,454   1626     75,573 NakedTriple
 *    104,533,400  18631      5,610    984    106,233 HiddenTriple
 *     63,145,100  18437      3,424   1331     47,441 TwoStringKite
 *     50,375,900  17106      2,944    452    111,451 Swampfish
 *     73,900,100  16914      4,369    897     82,385 W_Wing
 *     84,004,800  16289      5,157    535    157,018 XY_Wing
 *     52,939,100  15936      3,321    267    198,273 XYZ_Wing
 *     54,262,600  15686      3,459    312    173,918 Skyscraper
 *     96,620,400  15523      6,224    591    163,486 EmptyRectangle
 *     62,618,400  14932      4,193    221    283,341 Swordfish
 *    789,977,900  14871     53,122   1101    717,509 URT
 *     85,904,400  14100      6,092     11  7,809,490 Jellyfish
 *    150,676,000  14097     10,688    110  1,369,781 NakedQuad
 *    116,297,900  14078      8,260      1 16,297,900 HiddenQuad
 *    353,666,900  14077     25,123    188  1,881,206 Coloring
 *    980,644,800  13951     70,292    774  1,266,982 XColoring
 *  1,590,482,900  13691    116,169 116850     13,611 GEM
 *  2,459,219,300  12410    198,164   2029  1,212,035 BigWings
 *  3,348,658,300  11270    297,130   2488  1,345,923 DeathBlossom
 *  4,110,810,100   9166    448,484   2013  2,042,131 ALS_XZ
 *  1,278,195,800   7924    161,306   2649    482,520 ALS_Wing
 *  3,457,864,000   5839    592,201   2029  1,704,220 ALS_Chain
 *  1,231,292,100   4259    289,103     32 38,477,878 SueDeCoq
 *  7,187,217,900   4253  1,689,917    158 45,488,720 FrankenSwordfish
 * 10,993,825,600   4134  2,659,367   6779  1,621,747 Abduction
 *  4,795,287,200   2271  2,111,531   3160  1,517,495 DynamicChain
 *    110,144,400     11 10,013,127     11 10,013,127 DynamicPlus
 * 44,100,250,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  89,536,360,600 (01:29)  61,116,969
 *
 * NOTES:
 * 1. 1:29 is 7 seconds slower than previous. The interesting part is:
 *               time(ns) calls  time/call elims  time/elim  hinter
 *    PRE:  5,906,086,000  4270  1,383,158  2455  2,405,737  Reduction
 *    PST: 10,993,825,600  4134  2,659,367  6779  1,621,747  Abduction
 *    Abduction takes about twice as long as Reduction, to find a bit more than
 *    twice as many elims, so Abduction wins on time/elim, which is usually my
 *    preferred metric, but this time I'm backing outright time. Reduction is
 *    7 seconds faster, hence I prefer Reduction to the new Abduction, but you
 *    may demur. It's a subjective judgement call. I like speed. If you prefer
 *    accuracy then that's cool too.
 * 2. DiufSudoku_V6_30.258.2023-11-05.7z bug in CellReductionHint. NOT a
 *    release candidate, but I build anyway. It'll get fixed.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.259 2023-11-07</b> table.Contradictions gets contradictions out
 * of {@link diuf.sudoku.solver.hinters.table.ATableChainer.Tables#doElims() },
 * checks they are still fresh and turns them into hints. This is slower than I
 * had hoped. Just testing for contradictions in doElims makes it take about 9
 * seconds verses the previous 4 or 5 seconds, but how else?
 * <p>
 * Also I simplified all table class and Tech names.
 * <p>
 * Also former Result getResult() is now Pots getResults(). The On Results Pots
 * are a hack: each On Value has the SET_BIT (tenth bit) set. This still works
 * elsewhere, coz only the first 9 of 32 bits are used.
 * <p>
 * Also IAssSet exposes a getAss(hashCode), which is natural in FunkyAssSet but
 * a kludge in LinkedMatrixAssSet, which is unused and remains untested.
 * <p>
 * Also fixed MANY bugs in CellReductionHint and all like it. They where not
 * displaying the links correctly. It turns out that doing so is complicated,
 * and doing so efficiently is quite complicated.
 * <pre>
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     25,923,000 109480       236  58287         444 NakedSingle
 *     28,968,100  51193       565  16400       1,766 HiddenSingle
 *     75,368,300  34793     2,166  24176       3,117 Locking
 *     84,547,500  23441     3,606   7833      10,793 NakedPair
 *     76,393,000  21374     3,574   8275       9,231 HiddenPair
 *    120,283,700  19290     6,235   1616      74,432 NakedTriple
 *    100,323,400  18882     5,313    981     102,266 HiddenTriple
 *     59,795,800  18691     3,199   1329      44,993 TwoStringKite
 *     48,130,300  17362     2,772    453     106,247 Swampfish
 *     70,210,800  17166     4,090    909      77,239 W_Wing
 *     78,377,700  16532     4,740    539     145,413 XY_Wing
 *     51,280,600  16167     3,171    269     190,634 XYZ_Wing
 *     51,952,000  15915     3,264    316     164,405 Skyscraper
 *     91,579,800  15748     5,815    589     155,483 EmptyRectangle
 *     62,473,700  15159     4,121    229     272,810 Swordfish
 *    792,723,300  15096    52,512   1103     718,697 URT
 *     86,134,400  14324     6,013     11   7,830,400 Jellyfish
 *    144,582,500  14321    10,095    114   1,268,267 NakedQuad
 *    114,087,400  14300     7,978      1 114,087,400 HiddenQuad
 *    345,132,100  14299    24,136    190   1,816,484 Coloring
 *    948,321,100  14171    66,919    793   1,195,865 XColoring
 *  1,508,009,300  13902   108,474 116646      12,928 GEM
 *  2,470,975,800  12588   196,296   2039   1,211,856 BigWings
 *  3,232,693,700  11446   282,429   2492   1,297,228 DeathBlossom
 *  4,027,115,300   9349   430,753   2036   1,977,954 ALS_XZ
 *  1,255,720,800   8095   155,123   2647     474,393 ALS_Wing
 *  3,506,889,500   6019   582,636   2029   1,728,383 ALS_Chain
 *  1,166,336,000   4435   262,984     30  38,877,866 SueDeCoq
 *  7,182,092,100   4430 1,621,239    159  45,170,390 FrankenSwordfish
 *  9,053,433,700   4310 2,100,564    364  24,872,070 Contradiction
 *    171,240,300   3946    43,395   2055      83,328 Reduction
 *  4,742,506,300   2399 1,976,867   3351   1,415,251 DynamicChain
 *     76,104,700     11 6,918,609     11   6,918,609 DynamicPlus
 * 41,849,706,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  87,041,785,800 (01:27)  59,414,188
 *
 * NOTES:
 * 1. 1:27 is 2 seconds faster than previous.
 * 2. DiufSudoku_V6_30.259.2023-11-07.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.260 2023-11-08</b> CellReductionHint highlights and links more
 * than one elimination. CachingHintsAccumulator.add returns "added", NOT the
 * normal "stop now" as specified by IAccumulator, coz it ignores hints without
 * new eliminations, which ALL callers use to calculate there result, hence
 * Contradiction and Reduction are faster. To get 26 seconds I set dryer=true
 * (DynamicChain) and Log.MODE=NORMAL_MODE (undebuggable).
 * <pre>
 * This is not a standard run.
 * Hair-dryer required to achieve 26 seconds.
 *
 *       time(ns) calls time/call  elims  time/elim hinter
 *     16,119,496 81994       196  42651        377 NakedSingle
 *     19,820,743 39343       503  14034      1,412 HiddenSingle
 *     49,257,017 25309     1,946  22244      2,214 Locking
 *     50,374,183 15330     3,285   6921      7,278 NakedPair
 *     43,671,589 13717     3,183   7748      5,636 HiddenPair
 *     78,056,742 11923     6,546   1392     56,075 NakedTriple
 *     64,150,291 11623     5,519    902     71,120 HiddenTriple
 *     37,226,434 11466     3,246   1023     36,389 TwoStringKite
 *     30,034,171 10443     2,876    324     92,698 Swampfish
 *     46,811,893 10326     4,533    643     72,802 W_Wing
 *     54,503,308  9887     5,512    362    150,561 XY_Wing
 *     32,332,944  9645     3,352    186    173,833 XYZ_Wing
 *     36,719,721  9474     3,875    271    135,497 Skyscraper
 *     63,022,673  9335     6,751    441    142,908 EmptyRectangle
 *     38,311,967  8894     4,307    172    222,743 Swordfish
 *    558,379,052  8856    63,050    846    660,022 URT
 *     52,633,318  8279     6,357      5 10,526,663 Jellyfish
 *     94,587,321  8278    11,426     79  1,197,307 NakedQuad
 *     73,000,510  8266     8,831      1 73,000,510 HiddenQuad
 *    238,436,281  8265    28,848    132  1,806,335 Coloring
 *    573,278,304  8179    70,091    584    981,640 XColoring
 *  1,032,616,125  7985   129,319 100032     10,322 GEM
 *  1,471,736,570  7221   203,813   1560    943,420 BigWings
 *  1,845,743,038  6406   288,127   1711  1,078,751 DeathBlossom
 *  2,294,024,876  4983   460,370   1578  1,453,754 ALS_XZ
 *    638,162,857  4056   157,337   1812    352,187 ALS_Wing
 *  1,465,646,214  2679   547,087   1222  1,199,383 ALS_Chain
 *    620,585,277  1744   355,840     26 23,868,664 SueDeCoq
 *  5,048,151,640  1740 2,901,236    134 37,672,773 FrankenSwordfish
 *  1,267,815,056  1639   773,529    218  5,815,665 Contradiction
 *     86,733,100  1421    61,036    845    102,642 Reduction
 *  1,134,459,398   774 1,465,709 195516      5,802 DynamicChain
 *     13,756,900     3 4,585,633   1800      7,642 DynamicPlus
 * 19,170,159,009
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  26,942,259,500 (00:26)  18,390,620
 *
 * NOTES:
 * 1. 0:26 is 1:01 faster than previous, but this is NOT a standard run. This
 *    is what I can achieve when I throw all of my toys out of the pram:
 *    * CachingHintsAccumulator change make Contradiction and Reduction faster.
 *    * Turbo makes DynamicChain just solve it, which is faster.
 *    * Log.MODE=NORMAL_MODE renders batch undebuggable, saving ~10 secs.
 *    * 15 seconds would be better. Sigh.
 *
 * This standard run proves that CacheHintsAccumulator.add success is faster.
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     27,976,290 108986       256  57830         483 NakedSingle
 *     30,731,537  51156       600  16415       1,872 HiddenSingle
 *     80,821,205  34741     2,326  24191       3,340 Locking
 *     86,059,675  23384     3,680   7809      11,020 NakedPair
 *     73,645,374  21324     3,453   8261       8,914 HiddenPair
 *    127,035,610  19246     6,600   1593      79,746 NakedTriple
 *    104,170,175  18842     5,528    986     105,649 HiddenTriple
 *     63,279,842  18648     3,393   1324      47,794 TwoStringKite
 *     50,892,378  17324     2,937    445     114,364 Swampfish
 *     75,824,410  17139     4,424    902      84,062 W_Wing
 *     84,312,429  16505     5,108    527     159,985 XY_Wing
 *     55,208,971  16149     3,418    265     208,335 XYZ_Wing
 *     54,392,111  15902     3,420    332     163,831 Skyscraper
 *     97,162,992  15729     6,177    588     165,243 EmptyRectangle
 *     62,190,151  15141     4,107    224     277,634 Swordfish
 *    789,195,071  15078    52,340   1103     715,498 URT
 *     86,794,215  14304     6,067      5  17,358,843 Jellyfish
 *    158,067,713  14303    11,051    113   1,398,829 NakedQuad
 *    119,226,987  14281     8,348      1 119,226,987 HiddenQuad
 *    370,210,980  14280    25,925    187   1,979,737 Coloring
 *    993,349,764  14153    70,186    825   1,204,060 XColoring
 *  1,591,188,196  13883   114,614 120840      13,167 GEM
 *  2,333,755,164  12569   185,675   2044   1,141,758 BigWings
 *  3,417,740,332  11424   299,171   2541   1,345,037 DeathBlossom
 *  4,202,451,951   9283   452,704   2064   2,036,071 ALS_XZ
 *  1,280,403,349   8013   159,790   2691     475,809 ALS_Wing
 *  3,650,601,323   5898   618,955   2058   1,773,858 ALS_Chain
 *  1,293,585,310   4302   300,693     30  43,119,510 SueDeCoq
 *  7,166,487,497   4297 1,667,788    164  43,698,094 FrankenSwordfish
 *  1,930,494,321   4172   462,726    275   7,019,979 Contradiction
 *    185,680,789   3897    47,647   1401     132,534 Reduction
 *  5,056,821,997   2830 1,786,862   3998   1,264,837 DynamicChain
 *     97,203,499     11 8,836,681     11   8,836,681 DynamicPlus
 * 35,796,961,608
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  79,880,470,000 (01:19)  54,525,918
 *
 * NOTES:
 * 1. 1:19 is 8 seconds faster than previous 1:27, thanks to the change to
 *    CachingHintsAccumulator.add return value, making both Contradiction and
 *    Reduction much faster.
 * 3. DiufSudoku_V6_30.260.2023-11-08.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.261 2023-11-12</b> faster Idx iteration. IntQueue is less calls.
 * The code is also MUCH simpler. The old way was a cluster____. Also deleted
 * all of the visit/each methods. The only one I really miss is whileTrue, so
 * it will reappear next release, probably.
 * <p>
 * In detail: I changed Idx's iterator to IntQueue. My Idx iteration method was
 * slow and very complicated. IntQueues main advantage is it's MUCH simpler,
 * and it's also quite a bit faster, apparently. In fact it's so much faster I
 * am sceptical of my own success. Na, that can't be right. Time will tell.
 * <pre>
 * This non-standard is my new standard run (no slowies, no dryer).
 *       time(ns)  calls time/call  elims time/elim hinter
 *     22,092,400 109047       202  57853       381 NakedSingle
 *     27,186,200  51194       531  16394     1,658 HiddenSingle
 *     71,852,300  34800     2,064  24201     2,968 Locking
 *     82,928,900  23444     3,537   7808    10,621 NakedPair
 *     74,810,600  21383     3,498   8255     9,062 HiddenPair
 *    136,184,900  19303     7,055   1592    85,543 NakedTriple
 *    106,563,500  18898     5,638    991   107,531 HiddenTriple
 *     64,831,400  18703     3,466   1328    48,818 TwoStringKite
 *     58,090,400  17375     3,343    445   130,540 Swampfish
 *     76,136,500  17192     4,428    908    83,850 W_Wing
 *     67,025,400  16554     4,048    514   130,399 XY_Wing
 *     49,689,300  16206     3,066    265   187,506 XYZ_Wing
 *     57,799,100  15957     3,622    341   169,498 Skyscraper
 *    104,311,400  15781     6,609    595   175,313 EmptyRectangle
 *     75,769,000  15186     4,989    238   318,357 Swordfish
 *    818,903,300  15121    54,156   1095   747,856 URT
 *    166,356,300  14352    11,591    109 1,526,204 NakedQuad
 *    365,408,500  14331    25,497    185 1,975,181 Coloring
 *  1,002,959,800  14205    70,606    847 1,184,131 XColoring
 *  1,476,748,700  13927   106,034 120780    12,226 GEM
 *  2,169,239,200  12618   171,916   2053 1,056,619 BigWings
 *  2,455,730,500  11467   214,156   2526   972,181 DeathBlossom
 *  4,681,929,500   9341   501,223   2060 2,272,781 ALS_XZ
 *  1,442,312,800   8077   178,570   2678   538,578 ALS_Wing
 *  3,797,843,400   5971   636,048   2063 1,840,932 ALS_Chain
 *  2,335,188,500   4371   534,245    293 7,969,926 Contradiction
 *    222,013,300   4078    54,441   1546   143,604 Reduction
 *  5,541,026,000   2921 1,896,961   4107 1,349,166 DynamicChain
 *     94,573,800     11 8,597,618     11 8,597,618 DynamicPlus
 * 27,645,504,900
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465    38,287,563,100 (00:38)        26,134,855
 *
 * This is the last standard run. I now swap-over to the new standard hinters,
 * as listed in the above log.
 *       time(ns)  calls time/call  elims   time/elim hinter
 *     20,345,800 109340       186  57944         351 NakedSingle
 *     24,131,700  51396       469  16378       1,473 HiddenSingle
 *     69,618,300  35018     1,988  24198       2,877 Locking
 *     76,416,200  23646     3,231   7830       9,759 NakedPair
 *     65,088,000  21575     3,016   8233       7,905 HiddenPair
 *    121,327,600  19503     6,220   1607      75,499 NakedTriple
 *     96,795,600  19104     5,066    990      97,773 HiddenTriple
 *     57,380,400  18909     3,034   1325      43,305 TwoStringKite
 *     47,057,100  17584     2,676    459     102,520 Swampfish
 *     70,484,400  17389     4,053    880      80,095 W_Wing
 *     55,098,600  16775     3,284    545     101,098 XY_Wing
 *     40,758,000  16408     2,484    272     149,845 XYZ_Wing
 *     53,161,200  16153     3,291    314     169,303 Skyscraper
 *     94,778,100  15990     5,927    581     163,129 EmptyRectangle
 *     61,255,100  15409     3,975    235     260,660 Swordfish
 *    794,596,600  15344    51,785   1084     733,022 URT
 *     85,499,000  14581     5,863      8  10,687,375 Jellyfish
 *    146,104,200  14579    10,021    117   1,248,753 NakedQuad
 *    116,511,000  14557     8,003      1 116,511,000 HiddenQuad
 *    271,369,800  14556    18,643    186   1,458,977 Coloring
 *    901,748,800  14435    62,469    762   1,183,397 XColoring
 *  1,336,891,400  14169    94,353 118516      11,280 GEM
 *  1,872,575,700  12853   145,691   2037     919,281 BigWings
 *  2,375,532,500  11710   202,863   2509     946,804 DeathBlossom
 *  4,178,874,300   9591   435,707   2043   2,045,459 ALS_XZ
 *  1,307,621,100   8330   156,977   2687     486,647 ALS_Wing
 *  3,803,032,800   6219   611,518   2067   1,839,880 ALS_Chain
 *  1,020,116,200   4610   221,283     34  30,003,417 SueDeCoq
 *  7,544,393,200   4604 1,638,660    156  48,361,494 FrankenSwordfish
 *  9,278,541,900   4486 2,068,333    456  20,347,679 UnaryChain
 *  4,997,677,200   4119 1,213,322   3497   1,429,132 StaticChain
 *  3,455,748,600   2314 1,493,409    118  29,286,005 NishioChain
 *  4,051,269,400   2196 1,844,840   3004   1,348,624 DynamicChain
 *     80,165,200     11 7,287,745     11   7,287,745 DynamicPlus
 * 48,571,965,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  56,988,769,000 (00:56)  38,900,183
 *
 * NOTES:
 * 1. 56 is 23 secs faster than prev 1:19 thanks mainly to faster Idx iterator.
 *    Successive runtimes: 56, 59, 55 from ONE jar/CFG; guess CPU overheating.
 *    It retards itself as it warms-up, and that's persistent, making upto 30%
 *    difference in top1465 time. Slowies make the machine slow.
 *    Yeah, I let it cool down while I wrote the above and now it's 55 seconds.
 *    It's very difficult to write fast code without CONSISTENT times. JIT sux!
 *    You can't even test two solutions side-by-side. The order in which tests
 *    run can effect there time by 100%, or even more. Mad as a ____ing hatter!
 * 2. DiufSudoku_V6_30.261.2023-11-12.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.262 2023-11-13</b> faster again Idx iteration. Faster inline
 * Integer/Long.numberOfTrailingZeros(i); Henry S Warren Jr, Hacker's Delight,
 * (Addison Wesley, 2002). Faster Grid.toString. Faster printPuzzleSummary.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     21,846,000 109048       200  57853       377 NakedSingle
 *     26,219,600  51195       512  16394     1,599 HiddenSingle
 *     70,062,300  34801     2,013  24201     2,895 Locking
 *     79,544,600  23445     3,392   7808    10,187 NakedPair
 *     67,570,900  21384     3,159   8255     8,185 HiddenPair
 *    136,554,000  19304     7,073   1592    85,775 NakedTriple
 *    102,038,900  18899     5,399    991   102,965 HiddenTriple
 *     64,347,800  18704     3,440   1328    48,454 TwoStringKite
 *     53,765,900  17376     3,094    445   120,822 Swampfish
 *     75,118,600  17193     4,369    908    82,729 W_Wing
 *     77,656,500  16555     4,690    514   151,082 XY_Wing
 *     54,854,000  16207     3,384    265   206,996 XYZ_Wing
 *     57,497,800  15958     3,603    341   168,615 Skyscraper
 *    101,843,800  15782     6,453    595   171,166 EmptyRectangle
 *     69,421,100  15187     4,571    238   291,685 Swordfish
 *    823,160,000  15122    54,434   1095   751,744 URT
 *    165,501,400  14353    11,530    109 1,518,361 NakedQuad
 *    424,250,100  14332    29,601    185 2,293,243 Coloring
 *  1,065,458,100  14206    75,000    847 1,257,919 XColoring
 *  1,686,169,100  13928   121,063 120779    13,960 GEM
 *  2,341,400,100  12619   185,545   2053 1,140,477 BigWings
 *  2,444,763,100  11468   213,181   2526   967,839 DeathBlossom
 *  4,345,908,100   9342   465,201   2060 2,109,664 ALS_XZ
 *  1,332,169,100   8078   164,913   2678   497,449 ALS_Wing
 *  3,711,414,800   5972   621,469   2063 1,799,037 ALS_Chain
 *  2,183,833,100   4372   499,504    293 7,453,355 Contradiction
 *    213,550,100   4079    52,353   1547   138,041 Reduction
 *  5,298,530,100   2921 1,813,943   4107 1,290,121 DynamicChain
 *    104,903,000     11 9,536,636     11 9,536,636 DynamicPlus
 * 27,199,352,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  32,371,124,300 (00:32)  22,096,330
 *
 * NOTES:
 * 1. 32 secs is 24 secs faster than prev 56, thanks to faster Idx iterator.
 * 2. DiufSudoku_V6_30.262.2023-11-13.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.263 2023-11-18</b> TableChains replaces Contradiction and
 * Reduction.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     25,837,700 109717       235  58337       442 NakedSingle
 *     30,616,000  51380       595  16419     1,864 HiddenSingle
 *     76,142,800  34961     2,177  24165     3,150 Locking
 *     82,241,100  23620     3,481   7819    10,518 NakedPair
 *     71,074,600  21556     3,297   8232     8,633 HiddenPair
 *    137,734,300  19485     7,068   1608    85,655 NakedTriple
 *    105,470,000  19081     5,527    990   106,535 HiddenTriple
 *     66,571,300  18885     3,525   1351    49,275 TwoStringKite
 *     52,638,500  17534     3,002    457   115,182 Swampfish
 *     75,814,800  17342     4,371    898    84,426 W_Wing
 *     79,688,700  16713     4,768    537   148,396 XY_Wing
 *     61,638,700  16356     3,768    265   232,598 XYZ_Wing
 *     58,224,200  16106     3,615    319   182,521 Skyscraper
 *    103,087,200  15941     6,466    590   174,724 EmptyRectangle
 *     64,050,500  15351     4,172    229   279,696 Swordfish
 *    809,158,100  15289    52,924   1101   734,930 URT
 *    163,550,200  14516    11,266    110 1,486,820 NakedQuad
 *    424,706,300  14497    29,296    189 2,247,123 Coloring
 *  1,039,226,300  14371    72,314    816 1,273,561 XColoring
 *  1,675,951,500  14101   118,853 115160    14,553 GEM
 *  2,368,963,800  12803   185,031   2053 1,153,903 BigWings
 *  2,468,190,100  11652   211,825   2499   987,671 DeathBlossom
 *  4,242,844,300   9544   444,556   2034 2,085,960 ALS_XZ
 *  1,322,574,500   8288   159,577   2661   497,021 ALS_Wing
 *  3,906,071,500   6192   630,825   2060 1,896,151 ALS_Chain
 *  2,438,344,100   4581   532,273   1955 1,247,234 TableChain
 *  5,376,955,500   3056 1,759,474   1487 3,615,975 StaticChain
 *  4,700,749,600   2325 2,021,827   3223 1,458,501 DynamicChain
 *     91,023,800     11 8,274,890     11 8,274,890 DynamicPlus
 * 32,119,140,000
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  42,379,259,000 (00:42)  28,927,821
 *
 * NOTES:
 * 1. 42 secs is 10 secs slower than previous. StaticChain is most of that. Two
 *    minutes feels "snappy enough" in the GUI.
 * 2. DiufSudoku_V6_30.263.2023-11-18.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.264 2023-11-23</b> GEM is about 20% faster thanks to IdxPlusBuds
 * now caching an array of indices to iterate, instead of repeatedly iterating
 * an unchanged Idx using MyIntQueue, which is inefficient in this situation.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     25,620,592 109717       233  58337       439 NakedSingle
 *     32,121,822  51380       625  16419     1,956 HiddenSingle
 *     79,511,557  34961     2,274  24165     3,290 Locking
 *     81,442,679  23620     3,448   7819    10,415 NakedPair
 *     79,867,515  21556     3,705   8232     9,702 HiddenPair
 *    139,746,812  19485     7,172   1608    86,907 NakedTriple
 *    104,662,202  19081     5,485    990   105,719 HiddenTriple
 *     67,255,694  18885     3,561   1351    49,782 TwoStringKite
 *     53,194,145  17534     3,033    457   116,398 Swampfish
 *     75,612,278  17342     4,360    898    84,200 W_Wing
 *     81,825,424  16713     4,895    537   152,375 XY_Wing
 *     55,964,072  16356     3,421    265   211,185 XYZ_Wing
 *     58,779,875  16106     3,649    319   184,262 Skyscraper
 *    102,979,570  15941     6,460    590   174,541 EmptyRectangle
 *     66,776,535  15351     4,349    229   291,600 Swordfish
 *    819,854,061  15289    53,623   1101   744,644 URT
 *    173,602,657  14516    11,959    110 1,578,205 NakedQuad
 *    429,198,719  14497    29,606    189 2,270,892 Coloring
 *  1,104,326,650  14371    76,844    816 1,353,341 XColoring
 *  1,662,860,172  14101   117,924 115160    14,439 GEM
 *  2,360,764,551  12803   184,391   2053 1,149,909 BigWings
 *  2,395,015,692  11652   205,545   2499   958,389 DeathBlossom
 *  4,285,300,973   9544   449,004   2034 2,106,834 ALS_XZ
 *  1,355,360,044   8288   163,532   2661   509,342 ALS_Wing
 *  3,837,908,124   6192   619,817   2060 1,863,062 ALS_Chain
 *  2,437,695,339   4581   532,131   1955 1,246,902 TableChain
 *  5,360,851,854   3056 1,754,205   1487 3,605,145 StaticChain
 *  4,713,934,564   2325 2,027,498   3223 1,462,592 DynamicChain
 *    104,041,601     11 9,458,327     11 9,458,327 DynamicPlus
 * 32,146,075,773
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  42,180,924,700 (00:42)  28,792,440
 *
 * NOTES:
 * 1. 42 secs is same as previous. The worry is that GEM was just a bees-dick
 *    faster, repudiating the assertion that toArrayCached is more efficient.
 *    But it IS more efficient, making GEM 20% faster in Java 1.8. Sigh.
 * 2. DiufSudoku_V6_30.264.2023-11-23.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.264 2023-11-23</b> GEM is about 20% faster thanks to IdxPlusBuds
 * now caching an array of indices to iterate, instead of repeatedly iterating
 * an unchanged Idx using MyIntQueue, which is inefficient in this situation.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     25,620,592 109717       233  58337       439 NakedSingle
 *     32,121,822  51380       625  16419     1,956 HiddenSingle
 *     79,511,557  34961     2,274  24165     3,290 Locking
 *     81,442,679  23620     3,448   7819    10,415 NakedPair
 *     79,867,515  21556     3,705   8232     9,702 HiddenPair
 *    139,746,812  19485     7,172   1608    86,907 NakedTriple
 *    104,662,202  19081     5,485    990   105,719 HiddenTriple
 *     67,255,694  18885     3,561   1351    49,782 TwoStringKite
 *     53,194,145  17534     3,033    457   116,398 Swampfish
 *     75,612,278  17342     4,360    898    84,200 W_Wing
 *     81,825,424  16713     4,895    537   152,375 XY_Wing
 *     55,964,072  16356     3,421    265   211,185 XYZ_Wing
 *     58,779,875  16106     3,649    319   184,262 Skyscraper
 *    102,979,570  15941     6,460    590   174,541 EmptyRectangle
 *     66,776,535  15351     4,349    229   291,600 Swordfish
 *    819,854,061  15289    53,623   1101   744,644 URT
 *    173,602,657  14516    11,959    110 1,578,205 NakedQuad
 *    429,198,719  14497    29,606    189 2,270,892 Coloring
 *  1,104,326,650  14371    76,844    816 1,353,341 XColoring
 *  1,662,860,172  14101   117,924 115160    14,439 GEM
 *  2,360,764,551  12803   184,391   2053 1,149,909 BigWings
 *  2,395,015,692  11652   205,545   2499   958,389 DeathBlossom
 *  4,285,300,973   9544   449,004   2034 2,106,834 ALS_XZ
 *  1,355,360,044   8288   163,532   2661   509,342 ALS_Wing
 *  3,837,908,124   6192   619,817   2060 1,863,062 ALS_Chain
 *  2,437,695,339   4581   532,131   1955 1,246,902 TableChain
 *  5,360,851,854   3056 1,754,205   1487 3,605,145 StaticChain
 *  4,713,934,564   2325 2,027,498   3223 1,462,592 DynamicChain
 *    104,041,601     11 9,458,327     11 9,458,327 DynamicPlus
 * 32,146,075,773
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  42,180,924,700 (00:42)  28,792,440
 *
 * NOTES:
 * 1. 42 secs is same as previous. The worry is that GEM was just a bees-dick
 *    faster, repudiating the assertion that toArrayCached is more efficient.
 *    But it IS more efficient, making GEM 20% faster in Java 1.8. Sigh.
 * 2. DiufSudoku_V6_30.264.2023-11-23.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.265 2023-11-28</b> Coloring, XColoring, and GEM use new IdxC, an
 * Idx that caches it's indices array and plusBuds.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     25,304,500 109717       230  58337       433 NakedSingle
 *     31,801,200  51380       618  16419     1,936 HiddenSingle
 *     76,658,600  34961     2,192  24165     3,172 Locking
 *     80,724,100  23620     3,417   7819    10,324 NakedPair
 *     73,849,400  21556     3,425   8232     8,971 HiddenPair
 *    137,467,900  19485     7,055   1608    85,489 NakedTriple
 *    105,483,100  19081     5,528    990   106,548 HiddenTriple
 *     68,451,600  18885     3,624   1351    50,667 TwoStringKite
 *     52,719,100  17534     3,006    457   115,359 Swampfish
 *     78,602,100  17342     4,532    898    87,530 W_Wing
 *     79,889,200  16713     4,780    537   148,769 XY_Wing
 *     61,427,000  16356     3,755    265   231,800 XYZ_Wing
 *     58,457,600  16106     3,629    319   183,252 Skyscraper
 *    108,909,100  15941     6,832    590   184,591 EmptyRectangle
 *     64,891,100  15351     4,227    229   283,367 Swordfish
 *    820,355,200  15289    53,656   1101   745,100 URT
 *    169,549,700  14516    11,680    110 1,541,360 NakedQuad
 *    365,763,100  14497    25,230    189 1,935,254 Coloring
 *  1,290,082,000  14371    89,769    816 1,580,982 XColoring
 *  1,675,848,900  14101   118,846 115160    14,552 GEM
 *  2,412,090,100  12803   188,400   2053 1,174,909 BigWings
 *  2,404,813,700  11652   206,386   2499   962,310 DeathBlossom
 *  4,231,890,100   9544   443,408   2034 2,080,575 ALS_XZ
 *  1,364,134,500   8288   164,591   2661   512,639 ALS_Wing
 *  3,861,244,400   6192   623,585   2060 1,874,390 ALS_Chain
 *  2,468,493,100   4581   538,854   1955 1,262,656 TableChain
 *  5,490,886,900   3056 1,796,756   1487 3,692,593 StaticChain
 *  4,350,614,100   2325 1,871,231   3223 1,349,864 DynamicChain
 *    100,520,900     11 9,138,263     11 9,138,263 DynamicPlus
 * 32,110,922,300
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  42,070,840,700 (00:42)  28,717,297
 *
 * NOTES:
 * 1. 42 secs is same as previous. Coloring, XColoring, and GEM are just a bit
 *    faster. Shame it's not more.
 * 2. DiufSudoku_V6_30.265.2023-11-28.7z is a release candidate, AFAIK.
 * </pre>
 * <hr>
 * <p>
 * <b>KRC 6.30.266 2023-12-05</b> Idx masks a/b renamed m0/m1.
 * Idx gets offCounts and new only from GEM.
 * toStringImpl returns StringBuilder, as do several contributors.
 * SB(capacity) for powerOf2 StringBuilders everywhere.
 * Hinters set/clearFields to reduce fields being set/cleared.
 * TwoStringKite and SkyScraper use grid.pairs (ARegion.at was slow).
 * There are other changes, the reasons for which currently allude me. Sigh.
 * <pre>
 *       time(ns)  calls time/call  elims time/elim hinter
 *     23,176,103 109781       211  58407       396 NakedSingle
 *     29,128,689  51374       566  16411     1,774 HiddenSingle
 *     76,783,028  34963     2,196  24159     3,178 Locking
 *     77,681,533  23624     3,288   7821     9,932 NakedPair
 *     72,523,775  21559     3,363   8236     8,805 HiddenPair
 *    127,476,642  19488     6,541   1609    79,227 NakedTriple
 *    106,318,985  19084     5,571    993   107,068 HiddenTriple
 *     60,900,388  18888     3,224   1345    45,279 TwoStringKite
 *     51,261,272  17543     2,922    456   112,415 Swampfish
 *     79,092,889  17352     4,558    896    88,273 W_Wing
 *     71,958,058  16725     4,302    540   133,255 XY_Wing
 *     50,707,222  16365     3,098    265   191,348 XYZ_Wing
 *     78,779,633  16115     4,888    319   246,958 Skyscraper
 *     98,048,083  15950     6,147    592   165,621 EmptyRectangle
 *     65,432,348  15358     4,260    229   285,730 Swordfish
 *    807,917,419  15296    52,818   1103   732,472 URT
 *    158,625,801  14521    10,923    110 1,442,052 NakedQuad
 *    295,602,373  14502    20,383    189 1,564,033 Coloring
 *  1,137,128,909  14376    79,099    816 1,393,540 XColoring
 *  1,616,133,444  14106   114,570 114473    14,118 GEM
 *  2,271,142,745  12807   177,336   2055 1,105,178 BigWings
 *  2,310,284,970  11654   198,239   2493   926,708 DeathBlossom
 *  4,263,906,045   9554   446,295   2036 2,094,256 ALS_XZ
 *  1,339,441,233   8295   161,475   2668   502,039 ALS_Wing
 *  3,974,233,460   6193   641,729   2047 1,941,491 ALS_Chain
 *  2,336,570,360   4591   508,945   1966 1,188,489 TableChain
 *  5,946,126,540   3063 1,941,275   1557 3,818,963 StaticChain
 *  4,301,309,717   2325 1,850,025   3223 1,334,567 DynamicChain
 *     80,913,899     11 7,355,809     11 7,355,809 DynamicPlus
 * 31,908,605,563
 * pzls      total (ns) (mm:ss)   each (ns)
 * 1465  42,153,558,400 (00:42)  28,773,760
 * NOTES:
 * 1. 42 secs is same as previous, which is disappointing. Looks like I have
 *    reached the limit of my abilities. The point at which enormous effort is
 *    required in order to find any further improvement. This is my point of
 *    diminishing returns, ergo The Wall. Basically, I am too stupid to do much
 *    better than 42 seconds, which suits me fine.
 *
 *    I was pushing for 30 seconds, but it appears that's beyond my grasp, but
 *    some smart-ass could do it in future. My only proviso is use a laptop, or
 *    a desktop that's latop equivalent, precluding wet CPU's, and all manor of
 *    super computers. Let's set a max-price of 2K american (My I7 was 1600au).
 *    Home made hardware is acceptable. 2K applies to parts, not tools. If your
 *    workshop happens to make supercomputers then best of British ya bastard.
 *    3*27=81. That's all I'm sayin. Use of firmware is encouraged.
 *
 *    To be a useful challenge it must be accessible to a broad cross-section
 *    of smart-asses, so lets keep money out of it, shall we. Anybody bringing
 *    it to court will be shot. I give not three parts of an immitation eel
 *    flavoured flying ____ how sexy you are, dopey. Beat 42 seconds within the
 *    rules and I will snail mail you a gold star.
 *
 * 2. DiufSudoku_V6_30.266.2023-12-05.7z is a release candidate.
 * </pre>
 */
public class BuildTimings_02 {

}
