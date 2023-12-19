/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.tools;

import static diuf.sudoku.Values.VSIZE;
import static java.lang.Integer.bitCount;
import java.util.Random;


public class CrashTestDummy {

	// which is faster: the bitCount method, or my inline hack
	public static void main(String[] args) {
		try {
			final Random random = new Random();
			final int CEILING = (1<<27);
			final int MASK = (1<<9) - 1; // 511 9bit "byte" (a third of 27)
			int m0, m1, m2, expected, actual;
			long a, b, c, method=0L, hack=0L;
			for ( int i=0; i<1000000; ++i ) {
				// three random 27bit bitsets 0..511
				m0 = random.nextInt(CEILING);
				m1 = random.nextInt(CEILING);
				m2 = random.nextInt(CEILING);
				// get cardinality, with a method
				a = System.nanoTime();
				expected = bitCount(m0) + bitCount(m1) + bitCount(m2);
				// get the size again, with my hack: VSIZE of each 9bit "byte"
				b = System.nanoTime();
				actual = VSIZE[m0&MASK] + VSIZE[(m0>>9)&MASK] + VSIZE[(m0>>18)&MASK]
					   + VSIZE[m1&MASK] + VSIZE[(m1>>9)&MASK] + VSIZE[(m1>>18)&MASK]
					   + VSIZE[m2&MASK] + VSIZE[(m2>>9)&MASK] + VSIZE[(m2>>18)&MASK];
				c = System.nanoTime();
				// they are both correct
				assert actual == expected;
				// but which is faster?
				method += b - a;
				hack   += c - b;
			}
			System.out.format("method=%,d\n", method);
			System.out.format("hack  =%,d\n", hack);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}

/*
with i<1000000:

method=29,268,200
hack  =38,297,200

method=29,172,100
hack  =38,813,600

method=28,758,700
hack  =37,643,800

method=28,203,300
hack  =37,420,100
*/