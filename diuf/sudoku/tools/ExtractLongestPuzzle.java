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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the longest puzzle (the one with the most solving steps) in a log.
 * <p>
 * FYI, I consider an undo-array (not a list) but the largest is too long:
 * 120/102#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
 *
 * @author Keith Corlett 2021-11-09
 */
public class ExtractLongestPuzzle {
	private static final String LOG = IO.HOME+"top1465.d5.2021-11-08.12-58-02.log";
	public static void main(String[] args) {
		try {
			// 1/1#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
			final Pattern hintPattern = Pattern.compile("^(\\d+)/\\d+#.*");
			try ( BufferedReader log = new BufferedReader(new FileReader(LOG)) ) {
				String line;
				int maxHintNumber = 0; String maxLine = null;
				while ( (line=log.readLine()) != null ) {
					final Matcher matcher = hintPattern.matcher(line);
					if ( matcher.matches() ) {
						final int hintNumber = Integer.parseInt(matcher.group(1));
						if ( hintNumber > maxHintNumber ) {
							maxHintNumber = hintNumber;
							maxLine = line;
						}
					}
				}
				System.out.println(maxLine);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
}
