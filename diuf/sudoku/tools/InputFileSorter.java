/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import static diuf.sudoku.test.TestHelpers.*;
import diuf.sudoku.utils.MyFile;
import diuf.sudoku.utils.ParseException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;


/**
 * InputFileSorter main sorts an input LogicalSolverTester logFile by 
 * solve-time descending, to output a new MagicTour (.mt) file.
 * <p>
 * @author Keith Corlett, August 2019
 */
public final class InputFileSorter {

	private static final String NL = diuf.sudoku.utils.Frmt.NL;

	private static final String USAGE =
 "usage: DiufSudoku.jar inputLogFile outputFile" + NL
+"inputLogFile = the .log file from a previous LogicalSolverTester run." + NL
+"outputFile = the name of the new .mt file to be created." + NL
+ NL
+"The idea is to work primarily on expediting the slowest puzzles, so we solve" + NL
+"them first." + NL
+ NL
+"Note that the best inputLogFile is the result of a re-run (not the first run)" + NL
+"of this version of LogicalSolverTester, otherwise the first ?few? puzzles" + NL
+"solve-times are badly-inflated by Javas JIT compiler (which I hate).";

	public static void main(String[] args) {
		try {
			if ( args.length != 2 ) {
				System.err.println(USAGE);
				return;
			}
			// open the inputLogFile
			final String inputLogFilename = args[0];
			MyFile.mustExist(inputLogFilename);

			// open outputFile BEFORE attempting to parse input, in case it's
			// locked by a certain text-editor. I miss EditPlus! Sigh.
			final String outputFilename = args[1];
			final File outputFile = new File(outputFilename);
			try ( PrintStream out = new PrintStream(new FileOutputStream(outputFile), true) ) {
				List<Line> lines = slurp(inputLogFilename);
				// parse the logFile lines to get the execution times.
				// nb: if parse errors we remove the error line AND all lines
				//     there-after from lines; then sort and output lines; so 
				//     that we end-up with whatever was parsed before the error
				try {
					for ( Line line : lines )
						line.parse();
				} catch (ParseException ex) {
					while ( lines.size() > ex.lineNumber )
						lines.remove(ex.lineNumber);
				}
				// sort by solve times descending
				lines.sort(Line.BY_SOLVE_NANOS_DESC);		
				// print the output file = the mt lines by execution time descending
				for ( Line line : lines )
					out.println(line.puzzle);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.out);
		}
	}

}
