/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.io.IO;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;


/**
 * SortStdOutFileBySolveTimeDesc sorts a stdout.log by solve-times, descending.
 * <p>
 * This program is why the original puzzle (not the solution) is printed at the
 * end of the puzzle-summary-line in the logFile. Another reminder that one
 * f__ks with the logFile format at ones peril.
 * <p>
 * It should not matter which Log.MODE the logFile is in, because I only look at
 * the puzzle-summary-lines which are allways there, and I have gone all the way
 * on the puzzleSummaryLinePattern regex to select those (and only those) lines.
 * @author Keith Corlett 2019 OCT
 */
public class SortStdOutFileBySolveTimeDesc {

	private static final File INPUT_LOG_FILE = new File(IO.HOME+"top1465.d5.2023-05-27.18-25-47.stdout.log");
//	private static final File OUTPUT_MT_FILE = new File(IO.HOME+"top1465.d7.mt");

	public static void main(String[] args) {
//		final Pattern puzzleSummaryLinePattern = Pattern.compile("^ *\\d+\t.*[.123456789]{"+GRID_SIZE+"}$");
		try ( BufferedReader reader = new BufferedReader(new FileReader(INPUT_LOG_FILE))
//			; PrintWriter output = new PrintWriter(new FileWriter(OUTPUT_MT_FILE));
		) {
			LinkedList<PuzzleLine> puzzleList = new LinkedList<>();
			String line = ""; // = "" for debugger (no show until set)
			// skip the preamble
			int lineNumber = 0;
			while ( (line=reader.readLine())!=null && !line.startsWith("Superfisch") ) {
				++lineNumber;
			}
			reader.readLine(); // one last blank line
			reader.readLine(); // headers line
			lineNumber += 2;
			// foreach line with content (stopping at the first blank line)
			while ( (line=reader.readLine())!=null && line.length()>0 ) {
				++lineNumber;
				String[] fields = line.split(" +", 11);
				int puzzleNumber = Integer.parseInt(fields[1].trim().replaceAll(",",""));
				assert puzzleNumber>0 && puzzleNumber<10000;
				long solveTimeNanos = Long.parseLong(fields[2].replaceAll(",",""));
				assert solveTimeNanos>0;
				String contents = fields[10];
				if ( contents.length() != GRID_SIZE ) {
					System.out.println("POO "+lineNumber+": "+line);
					break;
				} else {
					puzzleList.add(new PuzzleLine(puzzleNumber, solveTimeNanos, contents));
				}
			}
			PuzzleLine[] puzzleArray = puzzleList.toArray(new PuzzleLine[puzzleList.size()]);
			Arrays.sort(puzzleArray, PuzzleLine.BY_SOLVE_TIME_NANOS_DESC);
			for ( PuzzleLine p : puzzleArray )
				System.out.println(p.contents);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	private static class PuzzleLine {
		public static final Comparator<PuzzleLine> BY_SOLVE_TIME_NANOS_DESC = (PuzzleLine a, PuzzleLine b) -> {
			if ( a.solveTimeNanos < b.solveTimeNanos )
				return 1; // DESCENDING
			if ( a.solveTimeNanos > b.solveTimeNanos )
				return -1; // DESCENDING
			return 0;
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
