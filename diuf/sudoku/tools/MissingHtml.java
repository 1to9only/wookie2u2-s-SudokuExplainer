/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * find .html files in the code which are NOT in file-system, esp warnings.
 *
 * @author Keith Corlett 2022-01-09
 */
public class MissingHtml {

	private static final File SRC = new File(IO.HOME+"/src/");

	public static void main(String[] args) {
		try {
			new MissingHtml().run();
		} catch ( Exception ex ) {
			ex.printStackTrace(System.err);
		}
	}

	private Pattern pattern; // set in go, and used all the way down in grep

	private MissingHtml() {
		super();
	}

	private void run() throws IOException {
		final Collection<File> javaFiles = new LinkedList<>();
		if ( !findFiles(SRC, javaFiles, ".java") )
			return;
		final Collection<File> htmlFiles = new LinkedList<>();
		if ( !findFiles(SRC, htmlFiles, ".html") )
			return;
//		for ( File f : htmlFiles )
//			System.out.println("html: "+f);
		pattern = Pattern.compile(".*(\".*\\.html\").*");
		for ( File javaFile : javaFiles )
			for ( String htmlFilename : matches(javaFile) )
				if ( !exists(htmlFiles, htmlFilename) )
					System.out.println("NOT_FOUND: "+htmlFilename+" in "+javaFile);
	}

	private Collection<String> matches(File f) throws IOException {
		Collection<String> matches = new LinkedList<>();
		try ( BufferedReader reader = new BufferedReader(new FileReader(f)) ) {
			String line;
			Matcher matcher;
			while ( (line=reader.readLine()) != null ) {
				matcher = pattern.matcher(line);
				if ( matcher.matches() ) {
//					System.out.println(""+f+": "+line);
					for ( int i=1,n=matcher.groupCount(); i<=n; ++i ) {
						String g = matcher.group(i);
//						System.out.println("group: "+g);
						// strip quotes: "whatever.html" -> whatever.html
						matches.add(g.substring(1, g.length()-1));
					}
				}
			}
		}
		return matches;
	}

	private boolean exists(Collection<File> files, String target) {
		for ( File f : files )
			if ( f.getName().equals(target) )
				return true;
		return false;
	}

}
