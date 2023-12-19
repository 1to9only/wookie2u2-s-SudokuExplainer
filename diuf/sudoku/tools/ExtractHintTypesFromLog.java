/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;

/**
 * Extract puzzles which contain hintType/s from a batch log; or the hints.
 *
 * @author Keith Corlett 2020 Jan
 */
public class ExtractHintTypesFromLog {

	private static final int HINT_START = 65; // the first char in the hint text

	public static void main(String[] args) {
		try {
			final Pattern puzzlePattern = Pattern.compile("^\\d+#.*");
			final Pattern hintPattern = Pattern.compile(".*\t(Finned|Sashimi|Kraken).*");
			final String logFile = IO.HOME+"top1465.d5.2023-05-21.17-25-54.log";
			try ( BufferedReader log = new BufferedReader(new FileReader(logFile)) ) {
				String line;
				String puzzle="", prevPuzzle="";
				// seek first actual log line (skip the header crap)
				while ( (line=log.readLine())!=null && !line.startsWith("Superfisch:") ); // empty statement intended
				log.readLine(); // dispose following empty line
				// some sort of Set is required for uniqueness
				// use a BitSet of 1465/64=23 longs for it is O(1) get method
				while ( (line=log.readLine()) != null ) {
					if ( puzzlePattern.matcher(line).matches() ) {
						puzzle = line.substring(line.indexOf('\t')+1);
					} else if ( hintPattern.matcher(line).matches() ) {
						System.out.println(line); // for DistinctHintTypes
//						System.out.println(line.substring(HINT_START));
//						if ( !puzzle.equals(prevPuzzle) ) {
//							System.out.println(puzzle);
//							prevPuzzle = puzzle;
//						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
