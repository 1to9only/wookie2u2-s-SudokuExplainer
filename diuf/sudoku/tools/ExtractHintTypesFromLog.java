/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.MyInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.regex.Pattern;

/**
 * Extract puzzles which contain any ${hintTypes} from top1465.d5.mt
 *
 * @author Keith Corlett 2020 Jan
 */
public class ExtractHintTypesFromLog {
	private static final int HINT_START = 65; // the first char in the hint text
	public static void main(String[] args) {
		try {
			// 1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5
			final Pattern puzzlePattern = Pattern.compile("^\\d+#.*");
//			final Pattern hintPattern = Pattern.compile(".*\t(Mutant|Kraken) (Swamp|Sword|Jelly)fish: .*");
//			final Pattern hintPattern = Pattern.compile(".*\tMutant (Swamp|Sword|Jelly)fish: .*");
			final Pattern hintPattern = Pattern.compile(".*\tSwampfish: .*");
			boolean isNew = false;
			final String logFile;
			if ( isNew )
				logFile = IO.HOME+"top1465.d5.2021-05-02.09-19-06.log"; // NEW
			else
				logFile = IO.HOME+"top1465.d5.2021-05-02.09-27-44.log"; // OLD
//			final String mtFile = IO.HOME+"top1465.d5.mt";
//			final String outFile = IO.HOME+"top1465.mutant.mt";
			try ( BufferedReader log = new BufferedReader(new FileReader(logFile));
//				  BufferedReader mt = new BufferedReader(new FileReader(mtFile));
//				  PrintStream out = new PrintStream(outFile)
			) {
				int puzzleNumber = 0;
				String line;
				// seek first actual log line (skip jit-solve & header crap)
				while ( (line=log.readLine())!=null && !line.startsWith("hid  	") ); // empty statement intended
				// some sort of Set is required for uniqueness
				// use a BitSet of 1465/64=23 longs for it's O(1) get method
				BitSet puzzleNumbers = new BitSet(1466); // <<<<================ SIZE OF .MT FILE!!!!
//				String puzzleLine = null;
				while ( (line=log.readLine()) != null ) {
					if ( puzzlePattern.matcher(line).matches() ) {
						puzzleNumber = MyInteger.parse(line, 0, line.indexOf("#")-1);
//						puzzleLine = line;
					} else if ( hintPattern.matcher(line).matches() ) {
						puzzleNumbers.set(puzzleNumber);
//						System.out.print(puzzleLine.substring(puzzleLine.indexOf('\t')+1)+SPACE);
						System.out.println(line.substring(HINT_START));
					}
				}
//				int lineNumber = 0;
//				while ( (line=mt.readLine()) != null )
//					if ( puzzleNumbers.get(++lineNumber) ) // 1 based
//						out.println(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
