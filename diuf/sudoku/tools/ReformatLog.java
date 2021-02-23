/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.MyStrings;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Reformat the a10e.log file because I buggered it up originally, and it takes
 * over a day to run, so I'm just fixing the log-file. The A10E code has changed
 * to produce the same format that I produce, so this is a one-time fix.
 * @author Keith Corlett 2020 Mar 16
 */
public class ReformatLog {
	@SuppressWarnings("fallthrough")
	public static void main(String[] args) {
		try ( BufferedReader reader = new BufferedReader(new FileReader(IO.HOME+"a10e.log")) ) {
			String line;
			String[] fields;
			while ( (line=reader.readLine()) != null ) {
outdent:
/*
nb: | stands in for the tab character
A10E
          1         2
0123456789 123456789 123456789
                    ## \d\d\d? to be removed!
puzz hn ce eb cl sb sm hi|cells                                                                           |redPots             |usedCmnExcl
   1 48  1  1 23 22 45 19|B1:2{19} B3:2{45} A7:3{168} C7:2{67} E7:3{568} H7:3{147} I7:4{1478} A8:4{1258} C8:3{257} B9:3{478}|E7-6                |B7:4{1578}
   1 50  1  1 26 19 45 20|H1:3{125} H3:2{17} A4:3{289} B4:2{79} C4:3{247} E4:3{189} I4:5{12478} G6:2{25} H6:3{245} H7:3{147}|H1-1, I4-2          |H4:4{1247}
   1 56  1  1 14 27 41  5|A2:2{19} A3:2{46} A4:2{89} C4:2{24} A5:2{35} B6:2{89} E6:2{39} A7:2{16} A8:4{1258} A9:3{248}|A8-1                |A6:2{23}
... the problem child has a 3 digit "sm" (sum) field (which was useless anyway. Oops!) ...
 119 16  1  1 79 25 104 22|B1:5{25678} C1:6{245678} A2:4{2478} B2:5{25678} C2:7{2456789} C3:5{24589} E3:4{1258} F3:4{1245} G3:2{49} I3:2{89}|C2-2                |A3:3{248}
... this one has VERY long redPots compared to the others, so I'm still not going to cater for it (25 is enough) ...
... this one also plays a 57 chev at 45, but I'll need a numerologist to find the 454, and a mechanic to blow me (Sorry Shar. Big sigh.). Sigh. ...
  45 57  1  1 15 25  4|B1:2{79} B4:2{46} A7:2{49} C7:2{78} D7:2{18} E7:2{15} H7:3{248} I7:3{589} B8:2{49} B9:2{26}|A7-9, B1-9, B4-6, B8-4, B9-2, C7-8, D7-8, E7-1, H7-24, I7-58|B7:2{27}
*/
				fields = line.split("\t");
				assert fields.length == 4 : ""+fields.length+": "+line;
				
				// now we modify the fields in situ
				
				// remove the "sm" (sum) field which is 2-or-3 digits starting at index 20
				fields[0] = fields[0].substring(0, 20) + fields[0].substring(fields[0].indexOf(' ', 20)+1);

				// reformat cells to 110 characters (80 wasn't big enough)
				fields[1] = MyStrings.format("%-110s", fields[1]);

				// reformat redPots to 25 characters (20 was a bit ify)
				fields[2] = MyStrings.format("%-25s", fields[2]);

				// print the fields
				System.out.print(fields[0]);
				for ( int i=1,n=fields.length; i<n; ++i ) {
					System.out.print("\t");
					System.out.print(fields[i]);
				}
				System.out.println();
				System.out.flush();
			}
		} catch (Exception ex) {
			// Nutbeans interleaves stderr with stdout so we print stacktrace to
			// stdout (not stderr) and also flush the s__t out of it beforehand.
			// And what's the bet that it's all done on the main thread, and
			// that's WHY it's ____ed. ____ing nutbeans! Mongo ____ing GUI
			// programmers! Couldn't drape a choko vine over a s__thouse!
			// SHOOT the bastards! Haaa ya! Pork chop to the head! Grow a bamboo
			// up THERE ass for a change, just so we know that they know what it
			// bloody well feels like. Crap tools s__t me! Especially when I'm
			// trying to fix my own crap in my own tools. Sigh. Physician heal
			// thy self. Wanna borrow my cheese grater? Yeah, na, thought not.
			try {Thread.sleep(50);}catch(InterruptedException eaten) {}
			System.out.flush();
			ex.printStackTrace(System.out);
		}
	}
}
