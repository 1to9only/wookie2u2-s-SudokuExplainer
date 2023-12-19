/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.MyInteger;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


/**
 * Summarises a LogicalSolverTest log-file which must be in the format produced
 * when Log.MODE=Log.VERBOSE_2_MODE; in order to focus in on a particular hint
 * type.
 * <p>We need to go through the log-file, remembering each puzzle-line, which
 * starts with \d+#, and looking for "Aligned Pair:" in the hint-lines, if we
 * find a matching hint-line we gather the puzzle-line, this-hint-line and any
 * subsequent matching hint-lines in this puzzle and output them.
 * <p>I have already written a PuzzleSummary class in the DiffLogSummaries class,
 * so I will reuse it here. It should make this class pretty straight-forward.
 * <p>FYI: In this case: Why does my new version of Aligned2Exclusion not
 * produce hints which the version 2019-09-20 18:08:31 (six days ago) did?
 * <br>It could be: <ul>
 * <li>that we are just following a different path through the puzzle now because
 * some other hinter is now getting in there first; OR
 * <li>that my latest re-write of Aligned*Exclusion for efficiency is another
 * total, complete, and utter a bag of ____. An utter failure. Again. Sigh.
 * </ul>
 * I am currently leaning towards the latter, and I am not happy about it.
 * Aligned*Exclusion bloody-well should be fast, but it MUST be accurate!
 * <p>Deprecated. Use the tools/DiffLogs class instead.
 * @author Keith Corlett 2019 SEPT
 */
//@Deprecated
public class Z_SummariseLog {

	private static final String NL = System.lineSeparator();

	private static final String HOME = IO.HOME;
	private static final File INPUT_FILE = new File(HOME+"top1465.d5.2019-09-26.15-07-09.AlignedPairExclusion.log");
	private static final File OUTPUT_FILE = new File(HOME+"top1465.d5.2019-09-26.15-07-09.AlignedPairExclusion.summary.txt");

	public static void main(String[] args) {
		try ( PrintStream out = new PrintStream(OUTPUT_FILE) ) {
			ArrayList<String> inputLines = IO.slurp(INPUT_FILE);
			// remove leading lines from inputLines
			for ( Iterator<String> it = inputLines.iterator(); it.hasNext(); ) {
				String inputLine = it.next();
				if ( inputLine.indexOf('#') > -1 )
					break;
				it.remove();
			}
			// remove trailing lines from inputLines
			for ( ListIterator<String> it = inputLines.listIterator(inputLines.size()); it.hasNext(); ) {
				String inputLine = it.previous();
				if ( inputLine.indexOf('#') > -1 )
					throw new RuntimeException("Oops: found # while backing-up from BOF for a line starting with \"        time (ns)\"");
				if ( inputLine.startsWith("         time(ns)") ) {
					it.remove(); // remove this line
					it.previous(); // and the blank line before it.
					it.remove();
					break;
				}
				it.remove();
			}
			// parse the remaining inputLines into Puzzles, which are actually
			// a puzzle summaries but I suck at English, so it will have to do.
			List<PuzzleSummary> puzzles = PuzzleSummary.parse(inputLines);
			for ( PuzzleSummary puzzle : puzzles ) {
				if ( puzzle.contents.contains("Aligned Pair:") ) {
					// now we want to output the puzzle-line (the first one)
					// and all the subseuquent "Aligned Pair:" hint-lines.
					String[] lines = puzzle.contents.split("\\r\\n|\\n|\\r");
					String output = lines[0];
					for ( int i=1; i<lines.length; ++i ) {
						String line = lines[i];
						if ( line.contains("Aligned Pair:") )
							output += NL + line;
					}
					out.println(output);
					System.out.println(output);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	// A puzzle summary: the contents of the puzzle-line plus 1..numHints
	// following hint-lines. Puzzles with no following hint-lines are ignored
	// (ie are NOT added to the returned ArrayList<Puzzle>).
	// The puzzleNumber is read from eg 44 in 44#C:\Users\User\Documents\NetB...
	// The numHints is the number of following non-hint-lines (ie no #).

	static class PuzzleSummary {

		public static final Comparator<PuzzleSummary> BY_PUZZLE_NUMBER_ASC
				= new Comparator<PuzzleSummary>() {
			@Override
			public int compare(PuzzleSummary a, PuzzleSummary b) {
				if ( a.puzzleNumber < b.puzzleNumber )
					return -1; // ASCENDING
				if ( a.puzzleNumber > b.puzzleNumber )
					return 1; // ASCENDING
				return 0;
			}
		};

		// This gives me a way of finding puzzles which contain "Aligned Pair:"
		// for example. I suppose you could also use it to sort puzzles by the
		// number of hints taken to solve them (an indicator of "difficulty").
		//
		// A wee control-break program on the log-summary $lines. It parses
		// the $lines of a logFile into a list of PuzzleSummary.
		// A PuzzleSummary contains:
		// 1. the contents of the puzzle (a puzzle-header-line followed by 1 or
		//    more hint-detail-lines),
		// 2. the puzzleNumber, and
		// 3. the numHints (number of hints), ie the number of hint-detail-lines
		//    in the contents.
		public static ArrayList<PuzzleSummary> parse(ArrayList<String> lines) {
			if ( lines==null || lines.isEmpty() )
				throw new RuntimeException("lines is null or empty.");
			ArrayList<PuzzleSummary> result = new ArrayList<>(lines.size());
			int puzzleNum;
			// the first $line is allways a puzzle line, else it is an error.
			String puzzleContents = lines.get(0);
			int hashIndex = Frmt.indexOf(puzzleContents, '#', 0, 6);
			if ( hashIndex < 0 )
				throw new RuntimeException("ArrayList<String> lines: Expected first line to start with \\d+#. Actual: "+puzzleContents);
			int numHints = 0;
			int prevHashIndex = hashIndex;
			// for each subsequent line in lines.
			for ( int i=1,n=lines.size(); i<n; ++i ) {
				String line = lines.get(i);
				hashIndex = Frmt.indexOf(line, '#', 0, 6);
				if ( hashIndex > -1 ) { // this is a puzzle line
					// output the previous, if it has any hints
					if ( numHints > 0 ) {
						puzzleNum = MyInteger.parse(puzzleContents, 0, prevHashIndex);
						result.add(new PuzzleSummary(puzzleContents, puzzleNum, numHints));
					}
					puzzleContents = line;
					numHints = 0;
					prevHashIndex = hashIndex;
				} else { // this is a hint-deails-line
					// append it to the existing puzzle
					puzzleContents += NL + line;
					++numHints;
				}
			}
			return result;
		}

		public String contents;
		public int puzzleNumber;
		public int numHints = 0;

		private PuzzleSummary(String contents, int puzzleNumber, int numHints) {
			this.contents = contents;
			this.puzzleNumber = puzzleNumber;
			this.numHints = numHints;
		}

		@Override
		public String toString() {
			return ""+puzzleNumber+":"+numHints+":"+contents;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof PuzzleSummary
				&& ((PuzzleSummary)o).puzzleNumber==puzzleNumber;
		}

		@Override
		public int hashCode() {
			return puzzleNumber;
		}
	}

}
