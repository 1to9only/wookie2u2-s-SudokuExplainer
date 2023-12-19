/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import java.util.Iterator;

/**
 * Permuter facilitates the iteration of each distinct possible combination of
 * elements in a master set. It has replaced the Permutations class.
 * <p>
 * My iterator returns an array of $permutationSize indexes in a master-set of
 * $masterSetSize elements. The permutationSize is $permutationsArray.length.
 * Completely iterating that iterator allows you to examine each distinct
 * possible combination of elements in the master set.
 * <p>
 * Code adapted from:
 * Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002)
 * ISBN 0-201-91465-4
 * <p>
 * <pre>
 * 2023-08-14 Extracted Permuter from Permutations, to suit AlsFinders.
 * 2023-08-15 Permuter used throughout SE, replacing Permutations.
 * </pre>
 *
 * @author Keith Corlett 2023-08-14 based on Permutations by hobiwan (HoDoKu).
 */
public final class Permuter implements Iterable<int[]> {

	// one set (1) bit in each position of a 64-bit long. This is used to test
	// each bit. An array is a bit faster than calculating (1L<<i) repeatedly.
	private static final long[] SET_BITS = new long[64];
	static {
		for ( int i=0; i<64; ++i ) {
			SET_BITS[i] = (1L<<i);
		}
	}

	private int permutationSize; // size of each permutation
	private long mask; // caps the number of set bits in the value
	private int[] permutationsArray; // returned by Iterator.next
	private boolean isLast; // has Iterator reached the last element
	private long value; // persists between calls to Iterator.next

	/**
	 * Set-up this Permuter to iterate each
	 * distinct combination of $permutationsArray.length elements,
	 * in a master-set of $masterSetSize elements.
	 * <p>
	 * Example usage: diuf.sudoku.solver.hinters.als.AlsFinder
	 *
	 * @param masterSetSize the number of elements in the master set to permute
	 * @param permutationsArray populated and returned by Iterator.next method.
	 *  The length of this array is the permutationSize, ie the number of items
	 *  in each combination
	 * @return this Permuter, which is {@code Iterable<int[]>}.
	 */
	public Permuter permute(final int masterSetSize, final int[] permutationsArray) {
		if ( masterSetSize < 0 ) // 0 means empty list, so do nothing
			throw new IllegalArgumentException("masterSetSize "+masterSetSize+" < 0");
		if ( masterSetSize > 64 )
			throw new IllegalArgumentException("masterSetSize "+masterSetSize+" > 64");
		this.permutationsArray = permutationsArray;
		this.permutationSize = this.permutationsArray.length;
		if ( permutationSize > masterSetSize )
			throw new IllegalArgumentException(" permutationsArray.length "+permutationSize+" > masterSetSize "+masterSetSize);
		this.value = (1 << permutationSize) - 1;
		this.isLast = (masterSetSize == 0);
		// constant attributes
		this.mask = (1L << (masterSetSize - permutationSize)) - 1;
		// mutable attributes
		this.value = (1 << permutationSize) - 1;
		this.isLast = (masterSetSize == 0);
		return this;
	}

	@Override
	public final Iterator<int[]> iterator() {
		return new MyIterator();
	}

	private class MyIterator implements Iterator<int[]> {

		@Override
		public boolean hasNext() {
			boolean result = !isLast;
			isLast = ((value & -value) & mask) == 0;
			return result;
		}

		@Override
		public int[] next() {
			final long bitset;
			if ( isLast )
				bitset = value;
			else {
				// remember this value
				final long v = value;
				// calculate the next value
				final long smallest = v & -v;
				final long ripple = v + smallest;
				final long ones;
				if ( smallest == 0 )
					ones = 0;
				else
					ones = ((v^ripple) >>> 2) / smallest;
				value = ripple | ones;
				// return this value
				bitset = v;
			}
			for ( int bit=0,i=0; i<permutationSize; ++bit )
				if ( (bitset & SET_BITS[bit]) != 0 )
					permutationsArray[i++] = bit;
			return permutationsArray;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Permuter is an immutable set.");
		}
	}

}
