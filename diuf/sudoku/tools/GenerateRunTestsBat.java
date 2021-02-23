package diuf.sudoku.tools;

import java.io.File;

/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */

/**
 * The GenerateRunTestsBat tool regenerates PART OF the RunTests-script
 * including all the *Test.java files in the ${DiufSudoku}/test directory.
 * It's intended for use on Windows, unix/linux, or mac (@todo fix commands).
 * <p>
 * I want a convenient way to run the tests out-side of the IDE, so that it
 * matters not which IDE you use. I'm on Windows so I generate a .bat.
 * <p>
 * NOTE: This tool supersedes the old ListTestClasses which was Windows only.
 *
 * @author Keith Corlett 2020-10-18
 */
public class GenerateRunTestsBat {
	
	/** three Shell's to cover the three most common Operating Systems. */
	private enum Shell { bat, ksh, mac };
	
	private static final Shell sh; // sh is just short for the word shell, it does NOT imply "using sh", ya putz!
	static {
		// System.getProperty("os.name") returns "Windows 10" on my box
		String osName = System.getProperty("os.name");
		if ( osName == null )
			sh = Shell.bat;
		else {
			switch ( osName.toLowerCase() ) {
			case "unix" : sh = Shell.ksh; break;
			case "linux" : sh = Shell.ksh; break;
			case "mac" : sh = Shell.mac; break;
			default: sh = Shell.bat; break;
			}
		}
	}
	
	private static final String SET;
	private static final String ON;
	private static final String OFF;
	private static final String NL;
	static {
		switch ( sh ) {
		case ksh: // ksh is presumed, but adapt to suit you preferred shell
			SET = "";	 // command used to set a variable
			ON = "${";	 // the start of a variable reference
			OFF= "}";	 // the tail of a variable reference
			break;
		case mac: // I'm clueless! Never used a mac, and have no internet.
			SET = "";	 // command used to set a variable:
			ON = "$";	 // the start of a variable reference
			OFF= "";	 // the tail of a variable reference
			break;
		default: // .bat is presumed, but adapt me as you like
			SET = "set "; // command used to set a variable
			ON = "%";	  // the start of a variable reference
			OFF= "%";	  // the tail of a variable reference
			break;
		}
		// the new-line sequence, change if you're producing for other O/S
		NL = System.lineSeparator();
	}
	

	private static File topDir;
	private static int topDirLen;

	/**
	 * usage: java qualified-java-class-name top-directory (the directory which contains all your *Test.java files)
	 * example: {@code java diuf.sudoku.tools.GenerateRunTestsBat 'C:\Users\User\Documents\NetBeansProjects\DiufSudoku\test'}
	 *
	 * @param args 
	 */
	public static void main(String[] args) {
		topDir = new File(args[0]);
		topDirLen = topDir.getAbsolutePath().length() + 1;
		System.out.print(
SET+"SUDOKU=diuf.sudoku"+NL+
SET+"HINTERS="+ON+"SUDOKU"+OFF+".solver.hinters"+NL+
SET+"CHECKS="+ON+"SUDOKU"+OFF+".solver.checks"+NL+
SET+"TEST_CLASSES="
);
		recurse(topDir);
		System.out.println();
	}

	// I'm a field so only the VERY first item skips the preceeding space,
	// not the first item in each directory. Sigh. My bad! Fixed. Sigh.
	private static boolean first = true;

	private static void recurse(File dir) {
		for ( File f : dir.listFiles() ) {
			if ( f.isDirectory() ) // this is DFS, should be BFS, but meh!
				recurse(f);
			else if ( f.getName().matches(".*Test.java") ) {
				// strip topDir of the front of the path
				String s = f.getAbsolutePath().substring(topDirLen);
				// strip ".java" of the end of the string
				s = s.substring(0, s.length()-5);
				// replace all \\'s with .'s
				char[] chars = s.toCharArray();
				for ( int i=0,n=s.length(); i<n; ++i )
					if ( chars[i] == '\\' )
						chars[i] = '.';
				s = new String(chars);
				s = s.replace("diuf.sudoku.solver.hinters", ON+"HINTERS"+OFF);
				s = s.replace("diuf.sudoku.solver.checks", ON+"CHECKS"+OFF);
				s = s.replace("diuf.sudoku", ON+"SUDOKU"+OFF);
				// replace diuf.sudoku.solver.hinters
				if ( first )
					first = false;
				else // subsequent
					System.out.print(" ");
				System.out.print(s);
			}
		}
	}
	
}
