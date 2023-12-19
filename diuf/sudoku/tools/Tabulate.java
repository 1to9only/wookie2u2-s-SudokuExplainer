/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.io.IO;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Tabulate entabulates a tab delimited text file, by padding out entries so
 * that they all appear in the same column when TAB_WIDTH is an agreed value.
 * I use 4 in code, so I use 4 here as well.
 *
 * @author Keith Corlett 2023-05-30
 */
public class Tabulate {

	private static final String INPUT = IO.HOME+"MAX_FINS.txt";
	private static final String INPUT_DELIMITER = "\t";
	// 128 spaces should be enough
	private static final String SPACES = "                                                                                                                                ";

	// append a number
	private static StringBuilder leftPad(final StringBuilder sb, String s, int width) {
		return sb.append(SPACES.substring(0, width-s.length()+2)).append(s);
	}
	// append a label
	private static StringBuilder rightPad(final StringBuilder sb, String s, int width) {
		return sb.append(s).append(SPACES.substring(0, width-s.length()+2));
	}

//#max MAX_FINS is 10 in top1465
//#a MAX_FINS of 7 finds 98.6% of hints while searching 47.6% of finned fish

	public static void main(String[] args) {
		try {
			// calculate the maximum width of each field, and maxLineWidth
			final int[] maxWidths = new int[32];
			int maxLineWidth = 0;
			try ( final BufferedReader reader = new BufferedReader(new FileReader(INPUT)) ) {
				String line;
				while ( (line=reader.readLine()) != null ) {
					final String[] fields = line.split(INPUT_DELIMITER); // in
					if ( fields.length > maxWidths.length )
						throw new ArrayIndexOutOfBoundsException("maxWidths.length needs to be at least "+fields.length);
					for ( int i=0; i<fields.length; ++i ) {
						if ( fields[i].length() > maxWidths[i] )
							maxWidths[i] = fields[i].length();
						maxLineWidth += maxWidths[i];
					}
				}
			}
			// reformat each field to its maximum width, then print each line
			try ( final BufferedReader reader = new BufferedReader(new FileReader(INPUT)) ) {
				final StringBuilder sb = SB(maxLineWidth<<2); // out
				String line;
				while ( (line=reader.readLine()) != null ) {
					final String[] fields = line.split(INPUT_DELIMITER); // in
					rightPad(sb, fields[0], maxWidths[0]);
					for ( int i=1; i<fields.length; ++i )
						leftPad(sb, fields[i], maxWidths[i]);
					System.out.println(sb);
					sb.setLength(0);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
