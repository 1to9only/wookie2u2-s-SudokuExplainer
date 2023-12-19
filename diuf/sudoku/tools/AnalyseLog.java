/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.utils.MyInteger;
import java.io.BufferedReader;
import java.io.FileReader;
import static diuf.sudoku.utils.Frmt.SP;

/**
 * Analyse the a7/8/9/10e.log file to workout what the filters are. Note that
 * you need to change this code (not just the filename) to make it is processing
 * suit the aligned-set-size, which will be pretty tricky coz you are not me,
 * and you did not write it; be think how much more complex this code would be
 * if it had to cater for sets of 7, 8, 9, or 10 aligned cells. This is easier.
 * I tried and failed the other way.
 * <p>
 * @author Keith Corlett 2020 Mar 16
 */
public class AnalyseLog {
	@SuppressWarnings("fallthrough")
	public static void main(String[] args) {

		int mbs, mbsMin=Integer.MAX_VALUE, mbsMax=Integer.MIN_VALUE;
		int from, to;
		int fives, maxFives=0;
		int sixes, maxSixes=0;
		int sevns, maxSevns=0;
		int eigts, maxEigts=0;
		int col, colMin=Integer.MAX_VALUE, colMax=Integer.MIN_VALUE;
		int sib, sibMin=Integer.MAX_VALUE, sibMax=Integer.MIN_VALUE;
		int sum, sumMin=Integer.MAX_VALUE, sumMax=Integer.MIN_VALUE;
		int hit, hitMin=Integer.MAX_VALUE, hitMax=Integer.MIN_VALUE;
		double prang, prangMin=Double.MAX_VALUE, prangMax=Double.MIN_VALUE;

		try ( BufferedReader reader = new BufferedReader(new FileReader(IO.HOME+"a8e.log")) ) {

			String line;
			reader.readLine(); // throw away the header line
			while ( (line=reader.readLine()) != null ) {

outdent:
/*
A5E
0123456789 123456789 12345678
puzz hn ce eb cl ms sb hi|cells                                              ...|redPots ...|usedCmnExcl
   1 48  1  1 12 14  7  7|H1:3{125} H3:2{17} H4:4{1247} G6:2{25} H7:3{147}   ...|H1-1    ...|H6:3{245}
   1 49  1  1 13 16  6  7|H3:2{17} H4:4{1247} I4:5{12478} G6:2{25} H7:3{147} ...|I4-2    ...|H6:3{245}
   2 25  1  1  8 13  8  5|B1:3{389} D1:3{259} E1:3{257} G1:2{59} F2:2{47}    ...|B1-9    ...|F1:3{247}

A6E
0123456789 123456789 12345678
puzz hn ce eb cl ms sb hi|cells                                                       ...|redPots ...|usedCmnExcl
   2 66  2  2  9 16  9 12|A7:3{123} C7:3{289} D7:3{358} H7:3{259} E8:2{34} E9:2{14}   ...|A7-2    ...|F7:2{28} E7:3{135}
   4 11  1  1 20 18 11 15|D1:3{139} D2:3{379} D4:3{189} D5:5{13589} D8:2{89} F8:2{59} ...|D5-8    ...|D9:5{35789}
   4 69  2  2 10 16  7  8|A1:3{259} B1:3{234} G1:2{25} I1:4{1234} F2:2{24} F3:2{12}   ...|I1-2    ...|D1:2{13} E1:2{49}
*/

				mbs = MyInteger.parse(line, 17, 19); // totalMaybesSize
				if(mbs<mbsMin) mbsMin = mbs;
				if(mbs>mbsMax) mbsMax = mbs;

				// adapt to any of the 2-digit nums to my left being 3-digits
				// by finding from= nextTab+1 & to= nextTabAfterFrom.
				from = 26;
				do {
					to = line.indexOf('\t', from);
					// if $from is a tab then increment from; pop at 42 (Turings safety valve to prevent infinite loop)
					if (to<=from && ++from>=42)
						throw new RuntimeException("Pop!");
				} while (to<=from);
				//System.out.format("%4d ", lineCount);
				//System.out.print(line.substring(from, to));
				//System.out.println();
				fives = sixes = sevns = eigts = 0;
				for ( String cell : line.substring(from, to).trim().split(SP) ) {
					switch(cell.charAt(3) - '0') {
					case 9: //fallthrough // 9 is the maximum possible
					case 8: if(++eigts>maxEigts) maxEigts = eigts; //fallthrough
					case 7: if(++sevns>maxSevns) maxSevns = sevns; //fallthrough
					case 6: if(++sixes>maxSixes) maxSixes = sixes; //fallthrough
					case 5: if(++fives>maxFives) maxFives = fives; //fallout
					}
				}

				col = MyInteger.parse(line, 14, 16);
				if(col<colMin) colMin = col;
				if(col>colMax) colMax = col;

				sib = MyInteger.parse(line, 20, 22);
				if(sib<sibMin) sibMin = sib;
				if(sib>sibMax) sibMax = sib;

				hit = MyInteger.parse(line, 23, 25);
				if(hit<hitMin) hitMin = hit;
				if(hit>hitMax) hitMax = hit;

				sum = col + hit;
				if(sum<sumMin) sumMin = sum;
				if(sum>sumMax) sumMax = sum;

				prang = ((double)mbs/(double)col) + ((double)mbs/(double)hit);
				if(prang<prangMin) prangMin = prang;
				if(prang>prangMax) prangMax = prang;
			}

			System.out.format("if(mbs<%d || mbs>%d) continue;\n", mbsMin, mbsMax);
			System.out.format("if(col<%d || col>%d) continue;\n", colMin, colMax);
			System.out.format("if(sib<%d || sib>%d) continue;\n", sibMin, sibMax);
			System.out.format("if(fives>%d) continue;\n", maxFives);
			System.out.format("if(sixes>%d) continue;\n", maxSixes);
			System.out.format("if(sevns>%d) continue;\n", maxSevns);
			System.out.format("if(eigts>%d) continue;\n", maxEigts);
			System.out.format("if(hit<%d || hit>%d) continue;\n", hitMin, hitMax);
			System.out.format("if(sum<%d || sum>%d) continue;\n", sumMin, sumMax);
			System.out.format("if(prang<%5.3fD || prang>%5.3fD) continue;\n"
					, prangMin-0.001D, prangMax+0.001D);
			System.out.flush();

		} catch (Exception ex) {
			StdErr.printStackTrace(ex);
		}
	}
}
