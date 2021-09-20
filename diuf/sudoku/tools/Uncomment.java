/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Uncomment reads a .java file (which presumably contains java source-code)
 * and prints it to $file.unco.java with the comments and blank-lines removed,
 * enabling one to see the actual bloody code. sigh.
 * <pre>
 * Ideally this would also remove lines containing only whitespace/s
 * and then remove all repeated end-of-line sequences;
 * BUT sorry I know not how to do it, so I do it "manually" in Netbeans
 * using search and replace regular expressions
 * ^[\t ]+$, EMPTY: to remove whitespace from whitespace-only lines
 * \n\n+, \n: takes out repeated newlines (run it repeatedly until not found)
 * and if the first line is empty just take it out manually. sigh.
 * and if the last line is empty just take it out manually. sigh.
 * 
 * I wish I was smart enough to write jksh: for uniphiles on windows
 * </pre>
 *
 * @author Keith Corlett 2021-06-03
 */
public class Uncomment {

	// the FILENAME of the input .java file
	private static final String HOME = "C:\\Users\\User\\Documents\\NetBeansProjects\\DiufSudoku\\";
	private static final String DIR = HOME+"src\\diuf\\sudoku\\solver\\hinters\\als\\";
	private static final String FILENAME = DIR+"BigWings.java";
	
	public static void main(String[] args) {
		int len; // the number of characters actually read
		int i; // the index of the character that we're upto in chars buffer
		final char[] chars = new char[4096]; // a buffer to read
		boolean quoted = false ; // are we currently in a quoted string?
		boolean blockC = false; // are we currently in a block comment?
		boolean lineC = false; // are we currently in a line comment?
		char pp=0, p=0, c=0; // previous-previous, previous and current char
		try ( BufferedReader reader = new BufferedReader(new FileReader(new File(FILENAME)));
//			PrintStream out = new PrintStream(FILENAME.replaceFirst("\\.java", ".unco.java"))
			PrintStream out = System.out // to debug; System.out.close is a no-op
		) {
			while ( (len=reader.read(chars, 0, chars.length)) != -1 ) {
				for ( i=0; i<len; ++i ) {
					c = chars[i];
					if ( p!='\\' && c=='\'' ) { // char quoted
						if ( !blockC && !lineC )
							quoted = !quoted;
					} else if ( p!='\\' && c=='"' ) { // string quoted
						if ( !blockC && !lineC )
							quoted = !quoted;
					} else if ( p=='/' && c=='/' ) { // line comment on
						if ( !quoted )
							lineC = true;
					} else if ( p=='/' && c=='*' ) { // block comment on
						if ( !quoted )
							blockC = true;
					} else if ( p=='*' && c=='/' ) { // block comment off
						if ( !quoted ) {
							blockC = false;
							// skip this character
							pp = p = 0;
							++i;
							continue; // skip writing two characters
						}
					} else if ( c=='\n' && p!='\\' ) { // line comment off
						if ( !quoted ) {
							lineC = false;
						}
					}
					// ok, write the previous character now
					// if it's NOT commented-out, and not empty
					if ( !blockC && !lineC && p!=0
					  // skip blank lines
					  && !(pp=='\n' && p=='\n') )
							out.write(p);
					pp = p;
					p = c;
				}
				// write the final character
				if ( !blockC && !lineC && c!=0 )
					out.write(c);
			}
		} catch (IOException ex) {
			System.err.println(ex);
			ex.printStackTrace(System.err);
		}
	}
}
