/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import static diuf.sudoku.io.IO.HOME;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Sort the input file numerically.
 * @author Keith Corlett 2020 Mar 04
 */
public class SortN {

	/** A*E stores the vital statistics of each aligned set which hints. */
	public static final File A4E_HASHCODES = new File(HOME+"DiufSudoku_A4E_hash.txt");

	public static void main(String[] args) {
		ArrayList<String> lines = null;
		try {
			lines = IO.slurp(A4E_HASHCODES);
			lines.sort(new Comparator<String>() {
				@Override
				public int compare(String a, String b) {
					return Integer.compare(Integer.parseInt(a),Integer.parseInt(b));
				}
			});
			for ( String line : lines )
				System.out.println(line);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
