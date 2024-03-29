/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.test;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.utils.Frmt.TAB;
import diuf.sudoku.utils.MyInteger;
import diuf.sudoku.utils.MyLong;
import diuf.sudoku.utils.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * Static helper methods and constants used in test classes, in the main
 * package; not to be confused with TestHelp in the test package. sigh.
 * And now also tools (so I am public) but never in Sudoku Solver itself.
 *
 * @author Keith Corlett
 */
 public final class TestHelp {

	static final String NL = diuf.sudoku.utils.Frmt.NL;

	public static String readLine(File file, int lineNumber) throws FileNotFoundException, IOException {
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			for ( int i=1; i<lineNumber; ++i )
				reader.readLine();
			return reader.readLine();
		}
	}

	public static void carp(Grid grid, Exception ex, PrintStream out) {
		out.flush();
		out.println();
		out.println(ex);
		out.println();
		out.println(grid);
		out.println();
		out.flush();
		ex.printStackTrace(out);
		out.println();
		out.flush();
	}

	// I import static TestHelpers.* in all testcases, so it is easier to do dis
	// once than it is to remember where I put beep in every test case. Hard to
	// believe the JDK-dudes made such a mess out of a hardware interrupt. Also
	// note that I am NOT public, else I appear in Netbeans import lists in the
	// main app, which, afaik, has no access to the test/ code. Morons!
	static final void beep() {
		diuf.sudoku.Constants.beep();
	}

	/**
	 * A Line represents a line in an input log file. It encapsulates the
	 * source File, the lineNumber and the lines contents; and then it has
	 * a bunch of fields to hold the values parsed from the line.
	 */
	public static final class Line {

		/**
		 * Line order by solveNanos DESCENDING. Used only by a tool. sigh.
		 */
		public static final Comparator<Line> BY_TIME_DESC = new Comparator<Line>() {
			@Override
			public int compare(Line a, Line b) {
				if ( a.solveNanos < b.solveNanos )
					return 1; // DESCENDING
				if ( a.solveNanos > b.solveNanos )
					return -1; // DESCENDING
				return a==b ? 0 : 1; // DESCENDING
			}
		};

		public final File file;
		public final int number;
		public final String contents;

		public int index;
		public long solveNanos;
		public String puzzle;

		private String toString;

		public Line(File file, int number) throws FileNotFoundException, IOException {
			this(file, number, readLine(file, number));
		}

		public Line(File file, int number, String contents) {
			this.file = file;
			this.number = number; // 1 based
			this.index = number - 1; // 0 based
			this.contents = contents;
			this.toString = null;
		}

		@Override
		public String toString() {
			if (toString!=null)
				return toString;
			return toString = (number==0 ? "" : ""+number+"#")
							+ file.getAbsolutePath()
							+ TAB + contents;
		}

		// only used in InputFileSorter
		public void parse() {
			final int N = REGION_SIZE;
			String[] fields = contents.trim().split(" *\\t *", N);
			if (fields.length != N)
				throw new ParseException("fields.length "+fields.length+" != "+N+" in: "+contents, this.number);
			index = MyInteger.parse(fields[0]);
			solveNanos = MyLong.parse(fields[1]);
			puzzle = fields[N-1];
		}
	}

	/**
	 * Reads the given lineNumber (1 based) from the given File.
	 * @param file to read
	 * @param lineNumber to read (1 based)
	 * @return the Line (defined here-in)
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Line readALine(File file, int lineNumber) throws FileNotFoundException, IOException {
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			// first we skip all lines BEFORE the lineNumber we want.
			for ( int i=1; i<lineNumber; ++i ) // note the 1
				reader.readLine();
			// now we read and return the line we want.
			return new Line(file, lineNumber, reader.readLine());
		}
	}

	public static List<Line> slurp(String... filenames) throws FileNotFoundException, IOException {
		List<Line> lines = new LinkedList<>();
		for ( String filename : filenames )
			slurp(new File(filename), lines);
		return lines;
	}

	public static List<Line> slurp(File inputfile) throws FileNotFoundException, IOException {
		List<Line> lines = new LinkedList<>();
		slurp(inputfile, lines);
		return lines;
	}

	public static int slurp(File file, List<Line> lines) throws FileNotFoundException, IOException {
		int count = 0;
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String s;
			while ( (s=reader.readLine()) != null )
				lines.add(new Line(file, ++count, s));
		}
		return count;
	}

	public static final class MoFoException extends RuntimeException {
		private static final long serialVersionUID = 696969L;
		public MoFoException(String msg) { super(msg); }
	}

	private TestHelp() {} // never  used
}