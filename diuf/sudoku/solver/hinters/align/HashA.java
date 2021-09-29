/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import static diuf.sudoku.utils.Frmt.COLON_ONLY;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.Arrays;
import java.util.Comparator;


/**
 * HashA (hash array of int) provides a fast hashCode for an int array, for
 * use as a Key in a Hash table.
 * <p>The point of this class is to compute the hashCode ONCE, on the premise
 * that the array will NOT be modified after it's added to the HashMap/Set.
 * If you don't KNOW that that is the case then don't use this class, just use
 * a bare int array as your hash-key instead. It works, it's just a bit tardy.
 * <p>I accept a varargs array because it's anonymous to you, so you can't
 * mutate the array after its "wrapped". I wish there was a way to NOT accept
 * an actual array parameter being passed to a varargs method, but there isn't.
 * <p>This class is currently only used in Aligned*Exclusion to provide a fast
 * hashCode field (not even a method) for an int array (the combo values) so
 * that we can use them as the key to our comboMap of int[] => lockingCell.
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
	public final int hashCode;
	public final int hashCode2; // an additional hashCode used by equals.

	public HashA(int... vals) {
		assert vals.length <= 16;
		this.array = vals;
		// calculate the hashCode field ONCE
		final int n = vals.length;
		int hc = vals[0];
		int i, e;
		if ( n < 9 ) {
			this.hashCode2 = 0; // this value isn't used
			// 4 bits is enough to hold 9 (the largest value, 1001)
			// which will cover upto eight values.
			for ( i=1; i<n; ++i )
				hc = hc<<4 ^ vals[i];
		} else {
			// We need to fit more values into our bitset, so we reduce the
			// left-shift to 2 for each zero value, and where zeros exist
			// there'll be just two non-zeros, so 8*2bits + 2*4bits = 24 bits
			// which is less than 32, so it fits, even though the resulting
			// hashCode goes beyond the bounds of a "normal" sized HashMap,
			// that's a problem for the HashMap's hash method, not for us.

			// NB: This'll "eat" the first 2 values of an A10E cell-exclusion
			//     where none of the 10 values are 0, so we bump the first 2
			//     values off into hashCode2, for the equals method. This should
			//     allow us to handle upto 16 values, hence the above assert.
			//     If you need 17-or-more values than create a hashCode3, or
			//     it might be better with an array of hashCodes.
			if ( (e=vals[1]) > 0 )
				hc = hc<<4 ^ e;
			else
				hc<<=2;
			this.hashCode2 = hc; // may be 0, which is OK.

			hc = 0;
			for ( i=2; i<n; ++i )
				if ( (e=vals[i]) > 0 )
					hc = hc<<4 ^ e;
				else
					hc<<=2;
		}
		this.hashCode = hc;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof HashA
			// hashCode is deterministic for upto 8 values, beyond that we must
			// also equate the additional hashCode2, which is much faster than
			// equating the arrays, which is what I built this class to avoid.
			&& (array.length<9 ? this.hashCode == ((HashA)o).hashCode
				:    this.hashCode == ((HashA)o).hashCode
				  && this.hashCode2 == ((HashA)o).hashCode2);
	}

	@Override
	public String toString() {
		if ( ts != null )
			return ts;
		return ts = EMPTY_STRING+hashCode+COLON_ONLY+Arrays.toString(array);
	}
	private String ts;

}
