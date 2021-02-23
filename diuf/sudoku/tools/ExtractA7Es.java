/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.utils.MyInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extract puzzle which contain an Aligned Sept from top1465.d5.mt using the
 * "special" A7E_1C.log
 * @author Keith Corlett 2020 Jan
 */
public class ExtractA7Es {
	public static void main(String[] args) {
		try {
			try ( BufferedReader log = new BufferedReader(new FileReader("A7E_1C.log"));
				  BufferedReader mt = new BufferedReader(new FileReader("top1465.d5.mt"));
				  PrintStream out = new PrintStream("top1465.d5.A7E.mt") ) {
				String line;
				Set<Integer> puzzleNumbers = new LinkedHashSet<>(); // use a set for uniqueness. HashSet for O(8)'ish contains.
				boolean first = true;
				while ( (line=log.readLine()) != null )
					if ( first ) // skip the first line (headers)
						first = false;
					else
						puzzleNumbers.add(MyInteger.parse(line, 0, 4));
				int lineNumber = 0;
				while ( (line=mt.readLine()) != null )
					if ( puzzleNumbers.contains(++lineNumber) )
						out.println(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
