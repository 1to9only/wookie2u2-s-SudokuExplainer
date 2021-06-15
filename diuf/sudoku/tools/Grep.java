/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Shows the divergence of the hint-paths in 2 LogicalSolverTester
 * Log.MODE=VERBOSE_5_MODE logFiles. IE: it prints the first hint in each
 * puzzle which differs.
 * <p>
 * I presumed that all hints would differ from that point on, so I was a bit
 * surprised to find that this is commonly not the case. Two hinters find the
 * same elimination fairly commonly (maybe 20% of the time, I guess).
 * <p>
 * NB: this algorithm (et al) is dependant on "^\\d+#" matching the start of the
 * puzzle-header-line and being the only lines in the file witch match; and a
 * bunch of other stuff like %5d hint-numbers. IE: One f__ks with the logFile
 * format at ones own peril.
 *
 * @author Keith Corlett 2019 OCT
 */
public class Grep {

	public static void main(String[] args) {
		Pattern pattern = Pattern.compile("^[1-9]=.*");
		final File file = new File(IO.HOME+"tmp.log");
		final PrintStream out = System.out;
		try ( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				if ( pattern.matcher(line).matches() )
					out.println(line);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}
