/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import java.util.Arrays;
import java.util.Comparator;


/**
 * HashA (hash array of int) provides a fast hashCode for an int array, for
 * use as a Key in a Hash table.
 * <p>
 * This class now differs from diuf.sudoku.solver.hinters.align.HashA in that
 * hashCode is now always a long of 64 bits to cater for Aligned9+Exclusion
 * instead of dropping the shift down to 2 bits, which blows-out collision rate
 */
public final class HashA {

	public static final Comparator<? super HashA> BY_VALUES_ASC
			= new Comparator<HashA>() {
		@Override
		public int compare(HashA a, HashA b) {
			//assert a.array.length == b.array.length;
			for ( int i=0,I=a.array.length; i<I; ++i ) {
				if ( a.array[i] < b.array[i] )
					return -1; // ASCENDING
				if ( a.array[i] > b.array[i] )
					return 1; // ASCENDING
			}
			return 0;
		}
	};

	public final int[] array;
	public final long hashCode;

	/**
	 * The constructor.
	 * <p>
	 * WARNING: You MUST pass me a new array, because I store the bastard,
	 * with it's hashCode, that is calculated ONCE, so you must not mutate
	 * the contents of the array after you've created a HashA from it. So
	 * just create a new array each time, OK?
	 *
	 * @param array
	 */
	HashA(int[] array) {
		this.array = array;
		long hc = array[0];
		for ( int i=1,n=array.length; i<n; ++i )
			hc = hc<<4L | array[i];
		this.hashCode = hc; // note: hashCode is a long
	}

	@Override
	public int hashCode() {
		return (int)((hashCode>>32L) ^ hashCode);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof HashA
			&& this.hashCode == ((HashA)o).hashCode;
	}

	@Override
	public String toString() {
		if ( ts != null )
			return ts;
		return ts = ""+hashCode+":"+Arrays.toString(array);
	}
	private String ts;

}
