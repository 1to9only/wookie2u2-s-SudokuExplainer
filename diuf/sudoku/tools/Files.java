/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import java.io.File;
import java.util.Collection;

/**
 *
 * @author User
 */
class Files {

	// add ls -r *.ext to results and return any
	// FYI ls is a *nix command: list files -recursively *.${ext}
	static boolean findFiles(final File path, final Collection<File> results, final String ext) {
		boolean result = false;
		for ( File f : path.listFiles() ) {
			if ( f.isDirectory() ) {
				result |= findFiles(f, results, ext);
			} else if ( f.getName().toLowerCase().endsWith(ext) ) {
				results.add(f);
				result = true;
			}
		}
		return result;
	}

}
