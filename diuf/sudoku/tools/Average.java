/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import diuf.sudoku.utils.MyLong;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * Average two columns in a tab separated values file.
 * @author 2020-02-28
 */
public class Average {
	public static void main(String[] args) {
		try {
			try ( BufferedReader reader = new BufferedReader(new FileReader("tmp.txt")) ) {
				String line;  long c=0L, e=0L; int cnt=0;
				while ( (line=reader.readLine()) != null ) {
//					int tab = line.indexOf('\t');
//					c += MyInteger.parse(line, 0, tab);
//					e += MyInteger.parse(line, tab+1);
					c += MyLong.parse(line);
					++cnt;
				}
//				System.out.format("Average calculated %4.2f experienced %4.2f\n"
//						, (double)c / (double)cnt
//						, (double)e / (double)cnt
//				);
				System.out.format("Total %,d", c);
				System.out.format(" / Cnt %,d", cnt);
				System.out.format(" = Average %,4.2f\n", (double)c/(double)cnt);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
