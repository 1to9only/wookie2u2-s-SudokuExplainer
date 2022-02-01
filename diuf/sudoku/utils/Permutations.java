/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import static diuf.sudoku.utils.Frmt.NL;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * A generator of binary permutations, ie all distinct possible combinations.
 * <p>
 * Given a degree <tt>nOnes</tt> and a length <tt>nBits</tt>, where
 * <tt>nOnes &lt;= nBits</tt>, this class computes all binary numbers of length
 * <tt>nBits</tt> which have exactly <tt>nOnes</tt> bits equal to <tt>1</tt>.
 * <p>
 * The binary numbers are generated in increasing order.
 * <p>
 * This class implements {@code Iterable<int[]>} whose next() method returns
 * an array of the indexes of the 'nOnes' elements that are <tt>1</tt>.
 * Actually it returns the 'array' that was passed to constructor, which has
 * been populated with the next possible combination of 'nOnes' in 'nBits'.
 * <p>
 * <b>For example:</b>
 * {@code
 * final int nOnes = 3; // the number of elements in a combination
 * final int nBits = 5; // the size of the master Set
 * final int[] array = new int[nOnes]; // working storage
 * // note that nOnes is taken from the array.length
 * Permutations permutations = new Permutations(nBits, array);
 * StringBuilder sb = new StringBuilder(256);
 * for ( int[] permutation : permutations )
 *     permutations.appendBinaryString(sb, permutation)
 *             .append(System.lineSeparator());
 * System.out.println(sb);
 * }
 * generates the following binary numbers:
 * {@code
 * 00111
 * 01011
 * 01101
 * 01110
 * 10011
 * 10101
 * 10110
 * 11001
 * 11010
 * 11100
 * }
 * Code adapted from "Hacker's Delight" by Henry S. Warren,
 * ISBN 0-201-91465-4
 * <p>
 * IF YOU'RE STEALING THIS CLASS: SPLIT THE CONSTRUCTOR INTO 2. THE ORIGINAL
 * (NO LONGER USED) TOOK nOnes and nBits, I SHOULD HAVE TAKEN nBits and array,
 * BUT I WAS TOO DUMB. AND REMOVE THIS COMMENT.
 *
 */
public final class Permutations implements Iterable<int[]> {

	public final int nBits;
	public final int nOnes;
	public final long mask;
	public final int[] array;

	/** public readonly access please */
	public boolean isLast;

	private long value;

	/**
	 * Permutations of $array.length elements in a master Set of size $nBits.
	 * <p>
	 * Constructs a new Permutations to iterate through each distinct possible
	 * combination of {@code array.length} elements in an array of {@code nBits}
	 * size.
	 * <p>
	 * The iterator populates and returns the {@code array} parameter with the
	 * indexes {@code (0 based)} of {@code array.length} elements which is a
	 * distinct combination of {@code array.length} elements of a {@code nBits}
	 * sized "master" set.
	 * @param nBits the number of bits (the size of the master list)<br>
	 *  &nbsp;required {@code nBits} must be >= 0 and &lt;= 64<br>
	 *  &nbsp;but {@code nBits} beyond 60 is really slow
	 * @param array to populate and return in the {@code iterator.next()}
	 */
	public Permutations(int nBits, int[] array) {
		if (nBits < 0) // 0 just means the master list is empty, so do nothing
			throw new IllegalArgumentException("nBits < 1");
		if (nBits > 64)
			throw new IllegalArgumentException("nBits "+nBits+" > 64");
		this.nBits = nBits;
		this.nOnes = array.length;
		// constant attributes
		this.mask = (1L << (nBits - nOnes)) - 1;
		this.array = array;
		// mutable attributes
		this.value = (1 << nOnes) - 1;
		this.isLast = (nBits == 0);
	}

	// sneeky fazeekie to use this class with the original code.
	public Permutations(int nOnes, int nBits) {
		this(nBits, new int[nOnes]);
	}

	/** @return Are there more permutations available? */
	public boolean hasNext() {
		boolean result = !isLast;
		isLast = ((value & -value) & mask) == 0;
		return result;
	}

	/**
	 * Returns the next possible combination of nOnes elements in nBits.
	 *
	 * @return the next permutation.
	 * Use <tt>Long.toBinaryString(perms.next())</tt> to see what's going on.
	 */
	public long next() {
		if ( isLast )
			return value;
		// remember this value
		final long v = value;
		// calculate the next value
		final long smallest = v & -v;
		final long ripple = v + smallest;
		final long ones = smallest==0 ? 0 : ((v ^ ripple) >>> 2) / smallest;
		value = ripple | ones;
		// return this value
		return v;
	}

	/**
	 * Get the next binary permutation as an array
	 * of bit indexes.
	 * @return the 0-based indexes of the bits that are set
	 * to one.
	 */
	public int[] nextAsArray() {
		final long bits = next();
		for ( int i=0,cnt=0; i<nBits; ++i )
			if ( (bits & (1L<<i)) != 0 ) // Bit 'i' is set
				array[cnt++] = i;
		return array;
	}

	@Override
	public final Iterator<int[]> iterator() {
		return new Iterator<int[]>() {
			@Override
			public boolean hasNext() { return Permutations.this.hasNext(); }
			@Override
			public int[] next() { return Permutations.this.nextAsArray(); }
			@Override
			public void remove() { throw new UnsupportedOperationException("Permutations is an immutable set."); }
		};
	}

	public int printBinaryString(PrintStream out) {
		out.print("Permutations of ");
		out.print(nOnes);
		out.print(" elements in ");
		out.print(nBits);
		out.print(":");
		out.print(NL);
		StringBuilder sb = new StringBuilder(nBits*2);
		int count = 0;
		for ( int[] perm : new Permutations(nBits, new int[nOnes]) ) {
			sb.setLength(0);
			Permutations.appendBinaryString(sb, perm, nBits).append(NL);
			out.print(sb);
			++count;
		}
		out.print("Count: ");
		out.print(count);
		out.print(NL);
		return count;
	}

	// =============== main method exercises Permutations class ===============

	public static StringBuilder appendBinaryString(StringBuilder sb, int[] perm, int nBits) {
		// convert the perm back into long bits
		final long bits = toLong(perm);
		// append the formatted bits
		appendBinaryString(sb, bits, nBits);
		return sb;
	}
	public static StringBuilder appendBinaryString(StringBuilder sb, long bits, int nBits) {
		String s = Long.toBinaryString(bits);
		// add leading zeros
		return sb.append(ZEROS.substring(0, nBits-s.length())).append(s);
	}
	private static final String ZEROS =
"00000000000000000000000000000000000000000000000000000000000000000000000000000";

	/**
	 * Returns factorial(i) which is 1+2+3...+i
	 * @param i to calculate: 0 >= i &lt;= Integer.MAX_VALUE depending mainly
	 * upon how long you're willing to wait.
	 * @return 1+2+3...+i
	 */
	public static int fak(int i) {
		return i==0 ? 0 : i + fak(i-1);
	}

	/**
	 * The count method counts the permutations of $nOnes in $nBits
	 * algorithmically. This method now used only to validate the results of
	 * the combinations method. This is MUCH*MUCH*MUCH*MUCH*MUCH... slower!
	 * <p>
	 * For 7 cells in 62 candidates nOnes is 7, nBits is 62.
	 * <p>
	 * The count method is private. Use combinations instead, especially in
	 * production code. It's fast. This is slow.
	 *
	 * @param nOnes the number of elements in the permutations to produce.
	 * @param nBits the size of the master set.
	 * @return the number of distinct permutations of $degree in $numCandidates.
	 * @deprecated prefer combinations(k, n) calculates n over k. It's faster.
	 */
	@Deprecated
	private static long count(int nOnes, int nBits) {
		assert nOnes>0 && nOnes<11; // was 10, I wonder why?
		assert nBits>=nOnes && nBits<65;
		// This verion is a lot faster because it's doing a LOT less work, like
		// not uselessly reading the values of into an array. No variables are
		// created in the loop, and there's zero method calls. This works when
		// nBits=64, but it's still too slow: nearly 2 hours for (10,62)
		final long mask = (1L << (nBits - nOnes)) - 1;
		long bits, smallest, ripple, ones; // were final longs declared in loop
		long cnt = 0L; // (8,59) overflows an int!
		long value = (1L << nOnes) - 1;
		do {
			// calculate the next value (boosted from next())
			bits = value;
			smallest = bits & -bits;
			ripple = bits + smallest;
			ones = smallest==0 ? 0 : ((bits ^ ripple) >>> 2) / smallest;
			value = ripple | ones;
			++cnt;
			// we're done when there is no next value
		} while (((value & -value) & mask) != 0);
		// 1 permutation is reported as 1, but all others are 1 low.
		// I fear that the iterator might be likewise missing a combo!
		return cnt > 1L ? cnt+1L : cnt;
	}

	/*
	 * LICENCE: The combinations method was boosted from HoDoKu's SudokuUtil
	 * class. I presume it's OK to modify GNU GPL code under the LGPL licence.
	 * IIRC, this code originates from "The Hackers Cookbook", but the name of
	 * it's author has coopted my towel. I have not modified it (much).
	 *
	 * http://sourceforge.net/projects/hodoku/files/
	 *
	 * Here's hobiwans standard licence statement:
	 *
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *
	 * Copyright (C) 2008-12  Bernhard Hobiger
	 *
	 * HoDoKu is free software: you can redistribute it and/or modify
	 * it under the terms of the GNU General Public License as published by
	 * the Free Software Foundation, either version 3 of the License, or
	 * (at your option) any later version.
	 *
	 * HoDoKu is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	 * GNU General Public License for more details.
	 *
	 * You should have received a copy of the GNU General Public License
	 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
	 */

	/**
	 * Calculates the number of possible combinations of k elements, in a
	 * master-list of size n, ergo {@code fakN / (fakNMinusK * fakK)}
	 * <p>
	 * Rodney's other party, at the faka whackers club, where fak means
	 * factorial, obviously.
	 * <p>
	 * You can use this to limit k and n to the maximum time you are willing to
	 * wait. Use combinations to estimate how long it'll take to iterate k in n
	 * and limit k, n, or both.
	 * <pre>{@code
	 * assert combinations(nOnes, nBits) < Integer.MAX_VALUE
	 *         : "Permutations("+nOnes+", "+nBits+") time-out!";
	 * }</pre>
	 * to make it throw at (10, 44) which takes 34.151 seconds, so expect
	 * it to take atleast 100 seconds JUST to iterate them all... try it!
	 * <p>
	 * KRC's only real change is to widen the return value to a long. Why the
	 * hell did he limit it to an int? There might be a reason, other than
	 * chronic acute impatience turbidity (ergo being a lazy c__t).
	 *
	 * @param k the number of elements in each combination (nOnes)
	 * @param n the size of the master list (nBits), so we seek all possible
	 *  combinations of 'k' elements in a master list of 'n' elements
	 * @return {@code (int)(fakN / (fakNMinusK * fakK));} where fak means
	 *  factorial.
	 */
	public static long combinations(final int k, final int n) {
		if (n < 168) {
			double fakN=1, fakNMinusK=1, fakK=1;
			for (int i=2; i<=n; i++)
				fakN *= i;
			for (int i=2; i<=n-k; i++)
				fakNMinusK *= i;
			for (int i=2; i<=k; i++)
				fakK *= i;
			return (long)(fakN / (fakNMinusK * fakK));
		} else {
			// maintain a cache of positive BigIntegers 0..largest-ever-n.
			// Note that this only happens ONCE when, as normally occurs,
			// combinations is invoked repeatedly with the same n (size of
			// master list) and only k (num elements in a combo) varies.
			if ( biN<n ) { // biN starts life as 0.
				BigInteger[] a = new BigInteger[n];
				for (int i=biN; i<=n; i++)
					a[i] = new BigInteger(""+i);
				bi = a;
				biN = n;
			}
			BigInteger fakN = BigInteger.ONE
					 , fakNMinusK = BigInteger.ONE
					 , fakK = BigInteger.ONE;
			for (int i=2; i<=n; i++)
				fakN = fakN.multiply(bi[i]);
			for (int i=2; i<=n-k; i++)
				fakNMinusK = fakNMinusK.multiply(bi[i]);
			for (int i=2; i<=k; i++)
				fakK = fakK.multiply(bi[i]);
			fakNMinusK = fakNMinusK.multiply(fakK);
			fakN = fakN.divide(fakNMinusK);
			return fakN.longValue();
		}
	}
	private static int biN = 0;
	private static BigInteger[] bi;

	/**
	 * convert the given perm back into a long, with a set (1) bit at each
	 * value in perm.
	 * @param perm a permutation to be converted back into a long
	 * @return the long version of perm[]
	 */
	public static long toLong(int[] perm) {
		long bits = 0;
		for ( int i : perm )
			bits |= (1L << i);
		return bits;
	}

	/**
	 * Produces a line of below format, so you can see what's going on:
	 * <pre><tt>
	 * 1111111111__________________________________
	 * 111111111_1_________________________________
	 * 11111111_11_________________________________
	 * 1111111_111_________________________________
	 * 111111_1111_________________________________
	 * 11111_11111_________________________________
	 * 1111_111111_________________________________
	 * 111_1111111_________________________________
	 * 11_11111111_________________________________
	 * 1_111111111_________________________________
	 * _1111111111_________________________________
	 * 111111111__1________________________________
	 * 11111111_1_1________________________________
	 * 1111111_11_1________________________________
	 * 111111_111_1________________________________
	 * 11111_1111_1________________________________
	 * 1111_11111_1________________________________
	 * 111_111111_1________________________________
	 * 11_1111111_1________________________________
	 * 1_11111111_1________________________________
	 * _111111111_1________________________________
	 * 11111111__11________________________________
	 * ...
	 * <tt></pre>
	 * <p>
	 * Note that there's no extraneous white-space, newlines or the like.
	 * @param perm the permutation to format
	 * @param n numBits: the size of the master set
	 * @return a String. For example:
	 * <tt>"1_11111111_1________________________________"</tt>
	 */
	public static String format(int[] perm, int n) {
		StringBuilder sb = new StringBuilder(n);
		// convert the perm back into long bits
		final long bits = toLong(perm);
		// build the string in the sb
		for ( int i=0; i<n; ++i )
			sb.append((bits & 1<<i)!=0 ? '1' : '_');
		return sb.toString();
	}

	/**
	 * this main-line is a bloody hackfest, so do whatever you like with it,
	 * just preserve the existing please. You'll see how. And please tell us
	 * what you were up-to in a comment.
	 */
	public static void main(String[] args) {
		if ( true ) {
			// Q: Time to iterate 10 elements in a Set of 44?
			// A: took 208.732 seconds.
			long start, finish;
			start = System.currentTimeMillis();

			// use combinations to calculate how many combos in 10 elements in
			// a Set of 44, to switch output back on for last 100 elements.
			long last100 = combinations(10, 44) - 100;

			long i = 0;
			for ( int[] perm : new Permutations(10,44) ) {
				if ( ++i>100 && i<last100 ) // skip the middle
					continue;
				if ( i == last100 ) // first line of the last 100
					System.out.println("...");
				System.out.print(format(perm, 44));
				System.out.println();
			}
			finish = System.currentTimeMillis();
			System.out.printf("took %dms\n", finish - start);
		} else if ( false ) {
			// Q: Is combinations faster than count.
			// A: Yes it is.

			// k is permutation size: the number of 1's in each permutation.
			final int k = 10; // %,15d guess
			long start, finish, x;
			// n is the size of the master set, so k must be <= n,
			// which I've implemented as "n starts at k".
			for ( int n=k; n<=64; ++n ) {
				System.out.format("%d,%2d", k,n);

				start = System.currentTimeMillis();
				x = combinations(k,n);
				finish = System.currentTimeMillis();
				System.out.format("\tcombinations=%,15d (%dms)", x, finish-start);

				start = System.currentTimeMillis();
				x = count(k,n);
				finish = System.currentTimeMillis();
				System.out.format("\tcount=%,15d (%dms)", x, finish-start);

				System.out.println();
			}
		} else if ( false ) {
			// Q: Use .printBinaryString on 2 elements in a Set of 9
			System.out.println(Permutations.class.getCanonicalName()+".main");
			// NB: 0 ones and 0 bits are nonsensical but must be handled elegantly.
			for ( int nBits=2; nBits<=9; ++nBits ) {
				for ( int nOnes=1; nOnes<=nBits; ++nOnes )
					new Permutations(nBits, new int[nOnes])
							.printBinaryString(System.out);
				System.out.println();
			}
			System.out.println();
		} else if ( false ) {
			// Q: Does .printBinaryString work?
			new Permutations(2, 57).printBinaryString(System.out);
		} else if ( false ) {
			// Q: How many combos in 2 in 61?
			int nOnes = 2;
			new Permutations(61, new int[nOnes])
					.printBinaryString(System.out);
		} else if ( false ) {
			// Q: How many combos in 5 in 53?
			int nOnes=5, nBits=53;
			new Permutations(nBits, new int[nOnes])
					.printBinaryString(System.out);
		}
	}

}
/*
These are main outputs, so that I don't have to wait for them twice.

run:
10,10	combinations=              1 (0ms)	count=              1 (0ms)
10,11	combinations=             11 (0ms)	count=             10 (0ms)
10,12	combinations=             66 (0ms)	count=             65 (0ms)
10,13	combinations=            286 (0ms)	count=            285 (0ms)
10,14	combinations=          1,001 (0ms)	count=          1,000 (0ms)
10,15	combinations=          3,003 (0ms)	count=          3,002 (0ms)
10,16	combinations=          8,008 (0ms)	count=          8,007 (0ms)
10,17	combinations=         19,448 (0ms)	count=         19,447 (0ms)
10,18	combinations=         43,758 (0ms)	count=         43,757 (0ms)
10,19	combinations=         92,378 (0ms)	count=         92,377 (0ms)
10,20	combinations=        184,756 (0ms)	count=        184,755 (16ms)
10,21	combinations=        352,716 (0ms)	count=        352,715 (0ms)
10,22	combinations=        646,646 (0ms)	count=        646,645 (16ms)
10,23	combinations=      1,144,066 (0ms)	count=      1,144,065 (15ms)
10,24	combinations=      1,961,256 (0ms)	count=      1,961,255 (31ms)
10,25	combinations=      3,268,760 (0ms)	count=      3,268,759 (47ms)
10,26	combinations=      5,311,735 (0ms)	count=      5,311,734 (78ms)
10,27	combinations=      8,436,285 (0ms)	count=      8,436,284 (110ms)
10,28	combinations=     13,123,109 (0ms)	count=     13,123,109 (187ms)
10,29	combinations=     20,030,009 (0ms)	count=     20,030,009 (281ms)
10,30	combinations=     30,045,014 (0ms)	count=     30,045,014 (407ms)
10,31	combinations=     44,352,164 (0ms)	count=     44,352,164 (624ms)
10,32	combinations=     64,512,239 (0ms)	count=     64,512,239 (891ms)
10,33	combinations=     92,561,039 (0ms)	count=     92,561,039 (1281ms)
10,34	combinations=    131,128,139 (0ms)	count=    131,128,139 (1796ms)
10,35	combinations=    183,579,395 (0ms)	count=    183,579,395 (2531ms)
10,36	combinations=    254,186,855 (0ms)	count=    254,186,855 (3483ms)
10,37	combinations=    348,330,136 (0ms)	count=    348,330,135 (4811ms)
10,38	combinations=    472,733,756 (0ms)	count=    472,733,755 (6499ms)
10,39	combinations=    635,745,396 (0ms)	count=    635,745,395 (8748ms)
10,40	combinations=    847,660,528 (0ms)	count=    847,660,527 (11638ms)
10,41	combinations=  1,121,099,407 (0ms)	count=  1,121,099,407 (15403ms)
10,42	combinations=  1,471,442,972 (0ms)	count=  1,471,442,972 (20214ms)
10,43	combinations=  1,917,334,783 (0ms)	count=  1,917,334,782 (26338ms)
10,44	combinations=  2,147,483,647 (0ms)	count=  2,481,256,777 (34101ms)
10,45	combinations=  2,147,483,647 (0ms)	count=  3,190,187,285 (43849ms)
10,46	combinations=  2,147,483,647 (0ms)	count=  4,076,350,420 (56049ms)
10,47	combinations=  2,147,483,647 (0ms)	count=  5,178,066,750 (71171ms)
10,48	combinations=  2,147,483,647 (0ms)	count=  6,540,715,895 (89917ms)
10,49	combinations=  2,147,483,647 (0ms)	count=  8,217,822,535 (112973ms)
10,50	combinations=  2,147,483,647 (0ms)	count= 10,272,278,169 (141793ms)
10,51	combinations=  2,147,483,647 (0ms)	count= 12,777,711,869 (176302ms)
10,52	combinations=  2,147,483,647 (0ms)	count= 15,820,024,219 (217559ms)
10,53	combinations=  2,147,483,647 (0ms)BUILD STOPPED (total time: 18 minutes 4 seconds)

You get the picture... this little duck plucker was brought to you by hobiwan.

*/
