/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.utils.MyInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import static diuf.sudoku.utils.Frmt.COMMA_SP;
import static diuf.sudoku.utils.Frmt.SPACE;
import static diuf.sudoku.utils.Frmt.TAB;

/**
 * Create a .hit from an a10e.log file.
 * @author Keith Corlett 2020 Mar 19
 */
public class LogToHitFile {
	@SuppressWarnings("fallthrough")
	public static void main(String[] args) {
		try ( BufferedReader reader = new BufferedReader(new FileReader(IO.HOME+"a10e.log")) ) {
			int puzz, hintNum;
			String line, cells;
			StringBuilder sb = new StringBuilder(32);
			reader.readLine(); // skip the header line
			while ( (line=reader.readLine()) != null ) {
/*			
          1         2
0123456789 123456789 123456789
puzz hn ce eb cl sb hi|cells                                                                                                         |redPots                  |usedCmnExcl
   1 48  1  1 23 22 19|B1:2{19} B3:2{45} A7:3{168} C7:2{67} E7:3{568} H7:3{147} I7:4{1478} A8:4{1258} C8:3{257} B9:3{478}            |E7-6                     |B7:4{1578}
   1 50  1  1 26 19 20|H1:3{125} H3:2{17} A4:3{289} B4:2{79} C4:3{247} E4:3{189} I4:5{12478} G6:2{25} H6:3{245} H7:3{147}            |H1-1, I4-2               |H4:4{1247}
   1 56  1  1 14 27  5|A2:2{19} A3:2{46} A4:2{89} C4:2{24} A5:2{35} B6:2{89} E6:2{39} A7:2{16} A8:4{1258} A9:3{248}                  |A8-1                     |A6:2{23}
*/
				puzz = MyInteger.parse(line, 0, 5);
				hintNum = MyInteger.parse(line, 5, 7);
				cells = line.substring(23, line.indexOf(TAB, 23)).trim();
				int cnt=0;
				sb.setLength(0);
				for ( String cell : cells.split(SPACE) ) {
					if(++cnt>1)sb.append(COMMA_SP);
					sb.append(cell.substring(0, 2));
				}
				System.out.format("%d\t%d\t%s\n", puzz, hintNum, sb);
			}
			System.out.flush();
		} catch (Exception ex) {
			try {Thread.sleep(50);}catch(InterruptedException eaten) {}
			ex.printStackTrace(System.err);
		}
	}
}
