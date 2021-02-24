/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
	public static void main(String[] args) {
		try {
			// 1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt	7.....4...2..7..8...3..8..9...5..3...6..2..9...1..7..6...3..9...3..4..6...9..1..5
			final Pattern puzzlePattern = Pattern.compile("^\\d+#.*");
//			// 27   	    370,478,600	26	 180	  1	Kraken Swampfish              	Kraken type 2: Finned Franken Swampfish: box 1, row 8 and col A, col C on 5 (I7-2)
//			final Pattern fishPattern = Pattern.compile(".*\t(Mutant|Kraken) (Swamp|Sword|Jelly)fish .*");
			final Pattern fishPattern = Pattern.compile(".*\tMutant (Swamp|Sword|Jelly)fish .*");
			final String logFile = IO.HOME+"top1465.d5.2021-02-10.17-54-39.log";
			final String mtFile = IO.HOME+"top1465.d5.mt";
//			final String outFile = IO.HOME+"top1465.fish.mt";
			final String outFile = IO.HOME+"top1465.mutant.mt";
			try ( BufferedReader log = new BufferedReader(new FileReader(logFile));
				  BufferedReader mt = new BufferedReader(new FileReader(mtFile));
				  PrintStream out = new PrintStream(outFile) ) {
				int puzzleNumber = 0;
				String line;
				// some sort of Set is required for uniqueness
				// use a BitSet of 1465/64=23 longs for it's O(1) get method
				BitSet puzzleNumbers = new BitSet(1466); // <<<<================ SIZE OF .MT FILE!!!!
				while ( (line=log.readLine()) != null ) {
					if ( puzzlePattern.matcher(line).matches() ) {
						puzzleNumber = MyInteger.parse(line, 0, line.indexOf("#")-1);
					} else if ( fishPattern.matcher(line).matches() ) {
						puzzleNumbers.set(puzzleNumber);
					}
				}
				int lineNumber = 0;
				while ( (line=mt.readLine()) != null )
					if ( puzzleNumbers.get(++lineNumber) ) // 1 based
						out.println(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
