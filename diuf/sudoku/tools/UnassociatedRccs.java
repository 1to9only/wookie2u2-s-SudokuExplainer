/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.MyInteger;
import java.io.File;


/**
 * Sort a file lexicographically ASCENDING.
 *
 * @author Keith Corlett
 */
public class UnassociatedRccs {
	public static void main(String[] args) {
		try {
			for ( String line : IO.slurp(new File(IO.HOME+"_bug.txt")) ) {
				final String[] f = line.split("=|\\.\\.");
				if ( f.length==3
				  && MyInteger.parse(f[2]) < MyInteger.parse(f[1]) ) {
					System.out.println(line);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
