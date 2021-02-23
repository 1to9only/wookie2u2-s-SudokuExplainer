/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.tools.Z_SummariseLog.PuzzleSummary;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * <p>KRC 2019-10-09 <b>This approach is superceeded by DiffLogs</b>.
 * 
 * Show the differences between two log-summaries. A diff basically tells you
 * how to turn the left-hand file into the right-hand file. So we have the
 * following 3 cases to deal with.<ul>
 * <li>if the puzzle is in the left-hand log only then we SUBTRACT
 * <li>if the puzzle is in the right-hand log only then we ADD
 * <li>if the puzzle is in both logs then there is no output.
 * </ul>
 * <p>NB: I'll just do this hard bit now, and then automate producing the
 * log-summaries later (it at all). Currently I just do RE search in Notepad++
 * and then manually remove the puzzles-with-no-hints.
 * <p>Deprecated. Use the tools/DiffLogs class instead.
 * @author Keith Corlett 2019 SEPT
 */
@Deprecated
public class Z_DiffLogSummaries {

	private static final String NL = System.lineSeparator();

	private static final String DIR = IO.HOME;
	private static final File LEFT_FILE = new File(DIR+"top1465.d5.2019-09-23.14-18-11.AlignedPairExclusion015.summary.txt");
	private static final File RIGHT_FILE = new File(DIR+"top1465.d5.2019-09-26.15-07-09.AlignedPairExclusion.summary.txt"); // later date-time
	private static final PrintStream OUT = System.out;

	public static void main(String[] args) {
		try {
			// the two lists of PuzzleSummary: which is just puzzleNumber + contents
			// with any/all puzzles which don't have any hint-lines removed.
			List<PuzzleSummary> leftList = PuzzleSummary.parse(IO.slurp(LEFT_FILE));
			List<PuzzleSummary> rightList = PuzzleSummary.parse(IO.slurp(RIGHT_FILE));
			// the two sets
			Set<PuzzleSummary> leftSet = new LinkedHashSet<>(leftList);
			Set<PuzzleSummary> rightSet = new LinkedHashSet<>(rightList);
			// left only
			Set<PuzzleSummary> onlyLeftSet = new LinkedHashSet<>(leftSet);
			onlyLeftSet.removeAll(rightSet);
			// right only
			Set<PuzzleSummary> onlyRightSet = new LinkedHashSet<>(rightSet);
			onlyRightSet.removeAll(leftSet);
			// all puzzles list (sorted)
			int n = (int)((leftSet.size()+onlyRightSet.size())*1.25);
			Set<PuzzleSummary> allSet = new LinkedHashSet<>(n, 0.75F);
			allSet.addAll(leftSet);
			allSet.addAll(onlyRightSet);
			List<PuzzleSummary> allList = new ArrayList<>(allSet);
			allList.sort(PuzzleSummary.BY_PUZZLE_NUMBER_ASC);
			for ( PuzzleSummary p : allList ) {
				if ( onlyLeftSet.contains(p) ) {
					output("+", p);
				} else {
					assert onlyRightSet.contains(p);
					output("-", p);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	private static void output(String marker, PuzzleSummary left) {
		OUT.print(marker);
		OUT.println(left.contents);
	}

}

/*
LEFT:
4#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	..7..8.6.1...5...8...6.....7...6..35..4..7....9.2......5.4...2..7....3......16.4.
   23	        833,938	26	 189	  1	Aligned Pair                  	Aligned Pair: I8, D9 (I8-9)
44#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	3..5..1...7.....8...8..4..66..4..9...3..6..5...7..1..48..1..3......2..6...2..9...
   36	        232,727	35	 129	  1	Aligned Pair                  	Aligned Pair: A2, C2 (A2-9)
   38	        552,198	35	 127	  1	Aligned Pair                  	Aligned Pair: D3, E9 (E9-8)
103#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.2....4..7.......6..8..7.....3..89.........42.541.........6....5....93.....34..81
   36	        456,638	31	 133	  1	Aligned Pair                  	Aligned Pair: G5, E6 (E6-7)
120#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.5.6..1......35...67...84..2.....8.......3.1.....7..5......4.6...9.1.3....2.....4
   45	        516,583	34	 125	  1	Aligned Pair                  	Aligned Pair: E4, E5 (E5-2)
218#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	...5..94..5.6...8...4.9.3..3...7.1..4........297......6..95...25..2...1..2..8....
   41	        251,416	39	 115	  1	Aligned Pair                  	Aligned Pair: H5, I5 (I5-5)
396#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	4...1..8...9.2..5.1.......2.3.9.8..7..5.........63......6...4....1.832..7....6..8
    9	        304,308	25	 191	  1	Aligned Pair                  	Aligned Pair: A7, D8 (A7-9)
   10	        267,284	25	 190	  1	Aligned Pair                  	Aligned Pair: B7, D8 (B7-9)
427#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	38.6.......9.......2..3.51......5....3..1..6....4......17.5..8.......9.......7.32
   13	        290,204	29	 184	  1	Aligned Pair                  	Aligned Pair: D4, D5 (D4-8)

RIGHT:
4#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	..7..8.6.1...5...8...6.....7...6..35..4..7....9.2......5.4...2..7....3......16.4.
   23	        886,831	26	 189	  1	Aligned Pair                  	Aligned Pair: I8, D9 (I8-9)
17#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	37.6.......4.......5..8.19......5....9..1..3....4......85.3..7.......6.......7.42
   67	        380,474	43	  83	  1	Aligned Pair                  	Aligned Pair: C8, C9 (C9-6)
103#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.2....4..7.......6..8..7.....3..89.........42.541.........6....5....93.....34..81
   37	        377,653	31	 133	  1	Aligned Pair                  	Aligned Pair: G5, E6 (E6-7)
120#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	.5.6..1......35...67...84..2.....8.......3.1.....7..5......4.6...9.1.3....2.....4
   46	        451,701	34	 124	  1	Aligned Pair                  	Aligned Pair: E4, E5 (E5-2)
141#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	..4.93.8..68..............9.3..4.6.....6.5..2.8....7..6..71.....179............4.
   45	        432,309	37	 117	  1	Aligned Pair                  	Aligned Pair: E8, E9 (E8-2)
218#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	...5..94..5.6...8...4.9.3..3...7.1..4........297......6..95...25..2...1..2..8....
   41	        226,028	39	 115	  1	Aligned Pair                  	Aligned Pair: H5, I5 (I5-5)

SUMMARY of summary differences:
4   is the same, so no output.
44  is only in the left.
141 and 218 are only in the right.

So the output (except this is truncated) will be:
-44#C:\Users\User\Documents\Net...
   36	        232,727	35	 12...
+141#C:\Users\User\Documents\Ne...
   45	        432,309	37	 11...
+218#C:\Users\User\Documents\Ne...
   41	        226,028	39	 11...

*/
