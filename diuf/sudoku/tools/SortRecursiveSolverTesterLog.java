/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import static diuf.sudoku.utils.Frmt.TAB;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLong;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Sort the RecursiveSolverTester.stdout.log by solve time (the second column)
 * descending, to stdout.
 * @author Keith Corlett 2019 NOV
 */
public class SortRecursiveSolverTesterLog {
	public static void main(String[] args) {
		final File inputFile = new File(IO.HOME+"RecursiveSolverTester.stdout.log");
		final String[][] lines = new String[1465][];
		final List<String> trailer = new LinkedList<>();
		try ( BufferedReader reader = new BufferedReader(new FileReader(inputFile)) ) {
			int count = 0;
			String line;
			while ( (line=reader.readLine()) != null && count<1465 )
				lines[count++] = line.split(TAB, 3);
			while ( (line=reader.readLine()) != null )
				trailer.add(line);
			
			Arrays.sort(lines, new Comparator<String[]>() {
				@Override
				public int compare(String[] o1, String[] o2) {
					long a = MyLong.parse(o1[1]);
					long b = MyLong.parse(o2[1]);
					if ( a < b )
						return 1; // DESCENDING
					if ( a > b )
						return -1; // DESCENDING
					return 0;
				}
			});
			for ( String[] fields : lines )
				System.out.println(format(fields));
			for ( String myLine : trailer )
				System.out.println(myLine);
		} catch (IOException ex) {
			StdErr.whinge(Log.me()+" exception", ex);
		}
	}

	private static final StringBuilder SB = new StringBuilder(132);
	private static String format(String[] a) {
		SB.setLength(0);
		SB.append(a[0]);
		for ( int i=1,n=a.length; i<n; ++i )
			SB.append(TAB).append(a[i]);
		return SB.toString();
	}
}
