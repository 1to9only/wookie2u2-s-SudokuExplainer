/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Sort a file lexicographically ASCENDING.
 *
 * @author Keith Corlett
 */
public class Sort {
	public static void main(String[] args) {
		ArrayList<String> lines = null;
		try {
			lines = IO.slurp(new File(IO.HOME+"_#new.txt"));
			lines.sort(new Comparator<String>() {
				@Override
				public int compare(String a, String b) {
					return a.compareTo(b);
				}
			});
			for ( String line : lines )
				System.out.println(line);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
