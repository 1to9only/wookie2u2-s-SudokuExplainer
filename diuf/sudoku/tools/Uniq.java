/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import static diuf.sudoku.io.IO.HOME;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Uniq prints distinct lines from it's input file, which must be pre-sorted.
 * @author Keith Corlett 2020 Mar 04
 */
public class Uniq {

	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A4E_HASHCODES = new File(HOME+"DiufSudoku_A4E_hash.txt");

	public static void main(String[] args) {
		final File inputFile = A4E_HASHCODES;
		try ( BufferedReader reader = new BufferedReader(new FileReader(inputFile)) ) {
			String line, prev=null;
			while ( (line=reader.readLine()) != null ) {
				if ( !line.equals(prev) )
					System.out.println(line);
				prev = line;
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
