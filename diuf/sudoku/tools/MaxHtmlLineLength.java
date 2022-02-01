/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import java.io.File;
import java.util.regex.Pattern;

/**
 * What's the maximum length of a HTML line in AChainingHint.recurseChainHtml,
 * just so that we do not need to grow the buffer? Ergo: I'm a putz!
 *
 * @author Keith Corlett 2021-06-20
 */
public final class MaxHtmlLineLength {

	private static final String INPUT = IO.HOME+"PrintHintHtml.txt";

	public static void main(String[] args) {
		try {
			int maxLen = 0;
			String longestLine = null;
			Pattern pattern = Pattern.compile("^\\(\\d{1,3}\\).*<br>$");
			for ( String line : IO.slurp(new File(INPUT)) ) {
				if ( pattern.matcher(line).matches() ) {
					int len = line.length();
					if ( len > maxLen ) {
						maxLen = len;
						longestLine = line;
					}
				}
			}
			if ( longestLine != null ) {
				System.out.println(""+maxLen+": "+longestLine);
			} else {
				System.err.println("not found");
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	private MaxHtmlLineLength() {} // Never used
}
