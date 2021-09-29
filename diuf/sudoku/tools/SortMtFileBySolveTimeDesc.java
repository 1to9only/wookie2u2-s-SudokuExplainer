/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Pattern;


/**
 * SortMtFileBySolveTimeDesc produces an .mt file sorted by the solve-time 
 * descending (slowest to fastest) from a LogicalSolverTester logFile.
 * <p>
 * This program is why the original puzzle (not the solution) is printed at the
 * end of the puzzle-summary-line in the logFile. Another reminder that one 
 * f__ks with the logFile format at ones peril.
 * <p>
 * It shouldn't matter which Log.MODE the logFile is in, because I only look at
 * the puzzle-summary-lines which are allways there, and I've gone all the way
 * on the puzzleSummaryLinePattern regex to select those (and only those) lines.
 * @author Keith Corlett 2019 OCT
 */
public class SortMtFileBySolveTimeDesc {

	private static final File INPUT_LOG_FILE = new File(IO.HOME+"top1465.d5.log");
	private static final File OUTPUT_MT_FILE = new File(IO.HOME+"top1465.d6.mt");

	public static void main(String[] args) {
		final Pattern puzzleSummaryLinePattern = Pattern.compile("^ *\\d+\t.*[.123456789]{81}$");
		try ( BufferedReader reader = new BufferedReader(new FileReader(INPUT_LOG_FILE))
			; PrintWriter output = new PrintWriter(new FileWriter(OUTPUT_MT_FILE)) ) {
			LinkedList<PuzzleLine> puzzleList = new LinkedList<>();
			String line;
			while ( (line=reader.readLine()) != null ) {
				if ( line.charAt(0)==' '
				  && puzzleSummaryLinePattern.matcher(line).matches() ) {
					//dumpLine(line);
					String[] fields = line.split(" *\t *",9);
					int puzzleNumber = Integer.parseInt(fields[0].trim());
					assert puzzleNumber>0 && puzzleNumber<10000;
					long solveTimeNanos = Long.parseLong(fields[1].replaceAll(",", ""));
					assert solveTimeNanos>0 && solveTimeNanos<120000000000L; // 2 minutes
					String contents = fields[8];
					assert contents.length() == 81;
					puzzleList.add(new PuzzleLine(puzzleNumber, solveTimeNanos, contents));
				}
			}
			PuzzleLine[] puzzleArray = puzzleList.toArray(new PuzzleLine[puzzleList.size()]);
			Arrays.sort(puzzleArray, PuzzleLine.BY_SOLVE_TIME_NANOS_DESC);
			for ( PuzzleLine p : puzzleArray )
				output.println(p.contents);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

//	private static void dumpLine(String line) {
//		String myLine = line.replaceAll(TAB, SPACE); // tabs ____ this all up.
//		System.out.println(myLine);
//		// print a line containing the index of the start of each word.
//		boolean prevIsWhitespace = true; // is the previous char tab or space
//		int i = 0; // the 0 based index of char c in line
//		int goQuiteUntil = -1; // the i at which we start to print spaces again
//		for ( char c : myLine.toCharArray() ) {
//			if ( c==' ' ) {
//				System.out.print(' ');
//				prevIsWhitespace = true;
//			} else if ( prevIsWhitespace ) {
//				System.out.print(i);
//				goQuiteUntil = i + numberOfDigitsIn(i) - 1;
//				prevIsWhitespace = false;
//			} else if ( i>goQuiteUntil )
//				System.out.print(' ');
//			++i;
//		}
//		System.out.println();
//		System.out.println();
//	}
//
//	private static int numberOfDigitsIn(int i) {
//		int count = 1;
//		while ( i > 10 ) {
//			++count;
//			i/=10;
//		}
//		return count;
//	}

	private static class PuzzleLine {
		public static final Comparator<PuzzleLine> BY_SOLVE_TIME_NANOS_DESC
				= new Comparator<PuzzleLine>() {
			@Override
			public int compare(PuzzleLine a, PuzzleLine b) {
				if ( a.solveTimeNanos < b.solveTimeNanos )
					return 1; // DESCENDING
				if ( a.solveTimeNanos > b.solveTimeNanos )
					return -1; // DESCENDING
				return 0;
			}
		};
		public final int puzzleNumber;
		public final long solveTimeNanos;
		public final String contents;
		private PuzzleLine(int puzzleNumber, long solveTimeNanos, String contents) {
			this.puzzleNumber = puzzleNumber;
			this.solveTimeNanos = solveTimeNanos;
			this.contents = contents;
		}
	}

}
