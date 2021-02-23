/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.solver.hinters.align.HitSet;
import diuf.sudoku.solver.hinters.align.HitSet.Hit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Analyse the a10e.log file to workout what the filters are.
 * @author Keith Corlett 2020 Mar 16
 */
public class MergeHitsFiles {
	public static void main(String[] args) {
		try {
			Set<Hit> set = new LinkedHashSet<>(1024, 0.75F);
			for ( String inFilename : new String[] { "A10E.old.hits.txt"
												   , "A10E.new.hits.txt"} )
				try ( BufferedReader rdr = new BufferedReader(new FileReader(new File(IO.HOME+inFilename))) ) {
					String line;
					while ( (line=rdr.readLine()) != null )
						set.add(HitSet.parse(line));
				}
			Hit[] array = set.toArray(new Hit[set.size()]);
			Arrays.sort(array);
			try ( PrintStream out = new PrintStream(IO.HOME+"top1465.d5.Aligned10Exclusion_1C.hits.txt") ) {
				for ( Hit hit : array )
					hit.println(out);
			}
		} catch (Exception ex) {
			System.out.flush();
			try {Thread.sleep(50);}catch(InterruptedException eaten) {}
			System.out.flush();
			ex.printStackTrace(System.out);
			System.out.flush();
		}
	}
}
