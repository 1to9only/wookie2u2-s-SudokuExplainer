/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import static diuf.sudoku.tools.Files.findFiles;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;


/**
 * Find any .html files which are NOT mentioned in the code.
 * <p>
 * @stretch .html files in code which are NOT in file-system, esp warnings.
 *
 * @author Keith Corlett 2022-01-09
 */
public class UnusedHtml {

	private static final File SRC = new File(IO.HOME+"/src/");

	public static void main(String[] args) {
		try {
			new UnusedHtml().run();
		} catch ( Exception ex ) {
			ex.printStackTrace(System.err);
		}
	}

	private Pattern pattern; // set in go, and used all the way down in grep

	private UnusedHtml() {
		super();
	}

	private void run() throws IOException {
		final Collection<File> javaFiles = new LinkedList<>();
		if ( !findFiles(SRC, javaFiles, ".java") )
			return;
		final Collection<File> htmlFiles = new LinkedList<>();
		if ( !findFiles(SRC, htmlFiles, ".html") )
			return;
		for ( File htmlFile : htmlFiles ) {
			pattern = Pattern.compile(".*"+htmlFile.getName().replaceAll("\\.", "\\\\.")+".*");
			if ( !grep(javaFiles) )
				System.out.println("NOT_FOUND: "+htmlFile.getAbsolutePath());
		}
	}

	// search files for pattern, returning any
	// FYI grep is a *nix command: general regular expression parser
	private boolean grep(final Collection<File> files) throws IOException {
		for ( File f : files )
			if ( grep(f) )
//			{
//				System.out.println("FOUND: "+pattern+" in "+f);
				return true;
//			}
		return false;
	}

	// search f (a java file) for pattern (the html-filename)
	// FYI grep is a *nix command: general regular expression parser
	private boolean grep(final File f) throws IOException {
		try ( BufferedReader reader = new BufferedReader(new FileReader(f)) ) {
			String line;
			while ( (line=reader.readLine()) != null )
				if ( pattern.matcher(line).matches() )
					return true;
		}
		return false;
	}


}
