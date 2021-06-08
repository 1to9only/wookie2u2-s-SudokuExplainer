/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import java.util.Iterator;

/**
 * A 0-based (standard) Bitset of 32 bits. The position of each set bit
 * represents the indice of a cell in a region. Note that the "shape" of the
 * region (ie if it's a Box, Row, or Col) does not matter.
 *
 * The interface is similar to {@code java.util.Set<Integer>} which isn't
 * implemented because this is a Set of ints and for performance we need to
 * avoid the overheads of boxing/unboxing the ints to/from Integers.
 *
 * Thanks to Stack Overflow for answers to many questions, including:
 * http://stackoverflow.com/questions/1092411/java-checking-if-a-bit-is-0-or-1-in-a-long
 *
 * This is free open-source software. Use and adapt it as you like, but you
 * accept all responsibility for any fallout.
 *
 * @author Keith Corlett 2013 August
 */
public final class Indexes implements Iterable<Integer>, Cloneable {

	// ---------------- static stuff ----------------

	/** The value returned by various methods meaning "Not found". */
	private static final int NONE = -1;

	/** get last() a tad faster than numberOfLeadingZeros on my box. */
	private static final double LOG2 = Math.log(2);

	/** Shifted left for a partial word mask. */
	private static final int ONES = 0xffffffff;

	/** The number of bits in an Index is 9. */
	private static final int NUM_BITS = 9; //invariant: NUM_BITS<32
	/** The size of the array/shifted is {@code 1<<NUM_BITS}. */
	private static final int ARRAY_SIZE = 1<<NUM_BITS; // 1<<9 = 512

	/** The bits of all values (0,1,2,3,4,5,6,7,8) == 111,111,111 (9 1's). */
	public static final int ALL_BITS = ARRAY_SIZE-1; // 511

	/** An array of the "shifted" bitset-values (faster than {@code 1<<v)}. */
	public static final int[] ISHFT = new int[NUM_BITS];
	/** An array-of-arrays of the unshifted values. The first index is your
	 * bitset => array of the values in that bitset. */
	public static int[][] INDEXES = new int[ARRAY_SIZE][];
//not used
//	/** An array-of-arrays of the left-shifted-bitset-values. The first index
//	 * is your bitset => array of the SHFT'ed values in that bitset. */
//	public static int[][] SHIFTED = new int[ARRAY_SIZE][];
	/** An array of Integer.numberOfTrailingZeros is faster than repeatedly
	 * calling the sucker on these same values. We can do this because there
	 * is only 512 values in question. */
	public static final int[] FIRST_INDEX = new int[ARRAY_SIZE];
	static {
		for ( int i=0; i<NUM_BITS; ++i )
			ISHFT[i] = 1<<i;
		for ( int i=0; i<ARRAY_SIZE; ++i) {
			INDEXES[i] = toValuesArray(i);
//			SHIFTED[i] = toShiftedArray(i);
			FIRST_INDEX[i] = Integer.numberOfTrailingZeros(i);
		}
	}

	/**
	 * An array of the Integer.bitCount of 0..511, for speed.<br>
	 * NOTE: that the number of bits in a bitset is the same regardless of
	 * whether the first bit represents 0 (Indexes) or 1 (Values); hence
	 * Indexes.ISIZE = Values.VSIZE, that is Indexes.ISIZE is just a pseudonym
	 * for the single array, which is kept in Values.
	 */
	public static final int[] ISIZE = Values.VSIZE;

	/** this static factory method is currently only used in test cases. */
	static Indexes empty() {
		return new Indexes(0); // int bits
	}

	/** "Unpacks" the given bits into an array of the index of each set (1)
	 * bit in the given bits.<br>
	 * For example: given binary {@code 1101} we return {@code {0,2,3}}.
	 * <p>
	 * WARNING: This method is hijacked by Idx static initialiser, so do NOT
	 * introduce anything (like SHFT) which assumes we know the size of the
	 * bloody set! There are perfectly good API methods which were designed
	 * to help us avoid having to do so, and they might be a bit slower but
	 * they do work for all possible bits... so just don't ____ with me or
	 * I'll send the pig around to your house to beat you most roughly; and
	 * there's nothing more humbling than having the absolute living piss
	 * smacked out of you by a girl. Har hjaa!
	 *
	 * @param bits to be unpacked
	 * @return given binary {@code 1101} we return {@code {0,2,3}}. */
	private static int[] toValuesArray(int bits) {
		final int n = Integer.bitCount(bits);
		int[] result = new int[n];
		int cnt = 0;
		for ( int i=0; cnt<n; ++i )
			if ( (bits & (1<<i)) != 0 )
				result[cnt++] = i;
		assert cnt == n; // even if they're both 0
		return result;
	}

	/** "Unpacks" the given bitset into an array of the outright value of each
	 * set (1) bit in the given bitset.
	 * For example: given binary {@code 1101} we return {@code {1,4,8}}.
	 * @param bitset to be unpacked
	 * @return given binary {@code 1101} we return {@code {1,4,8}}. */
	public static int[] toShiftedArray(int bitset) {
		final int n = Integer.bitCount(bitset);
		int[] result = new int[n];
		int cnt = 0;
		for ( int sv=1; cnt<n; sv<<=1 )
			if ( (bitset & sv) != 0 )
				result[cnt++] = sv;
		assert cnt == n; // even if they're both 0
		return result;
	}

	// ---------------- attributes ----------------

	/** readonly to public. The bitset value of this Indexes Set. */
	public int bits;
	/** readonly to public. The number of indexes in this Indexes Set. */
	public int size;

	// ---------------- constructors ----------------

	/** Constructs a new filled (0,1,2,3,4,5,6,7,8) Indexes Set. */
	public Indexes() {
		bits = ALL_BITS;
		size = NUM_BITS;
	}

	/** Constructs a new Indexes Set containing the given raw 'bits'.
	 * @param bits to set (a left-shifted bitset). */
	public Indexes(int bits) {
		this.size = ISIZE[this.bits = bits];
	}

	/** Constructs a new Indexes Set containing the given 'indexes'.
	 * <p>Now only used in test-cases, for readability. Angry people use the
	 * bits constructor.
	 * <p>EG: {@code Indexes idxs = new Indexes(0,1,2,3,8);}
	 * @param indexes {@code int...} an arguments array of "normal" (not
	 * left-shifted) indexes. */
	public Indexes(int... indexes) {
		for ( int i=0,n=indexes.length; i<n; ++i )
			bits |= ISHFT[indexes[i]]; // add idx to my bits
		this.size = ISIZE[bits]; // safety first, in case 2 idxs weren't distinct
	}

	/** Constructs a new Indexes Set containing the digits in 's'.
	 * @param s String of digits EG: "01238" to set. */
	public Indexes(String s) {
		for ( int i=0, n=this.size=s.length(); i<n; ++i )
			this.bits |= ISHFT[s.charAt(i)-'0'];
	}

	/** Copy-con: Constructs a new Indexes Set containing the indexes in 'src'.
	 * @param src {@code Indexes} to copy. */
	public Indexes(Indexes src) {
		this(src.bits);
	}

	// -------------------------------- setters -------------------------------

	// these methods set this Indexes outright, overwritting existing.

	/** Copies the indexes from 'src' into this Indexes Set. */
	void copyFrom(Indexes src) {
		this.bits = src.bits;
		this.size = src.size;
	}

	/** Clears (empties) this Indexes Set. */
	public final void clear() {
		bits = size = 0;
	}

	/** Fills (0,1,2,3,4,5,6,7,8) this Indexes Set. */
	public final void fill() {
		bits = ALL_BITS;
		size = NUM_BITS;
	}

	/**
	 * Set this Indexes to the given bits, and look-up the size.
	 * @param bits
	 * @return this Indexes for method chaining
	 */
	public Indexes set(int bits) {
		this.size = ISIZE[this.bits = bits];
		return this;
	}

	/**
	 * Add/clear 'i' to/from this Indexes Set, depending on 'value'.
	 * <p>
	 * This set is used by the test-cases as a utility (not just as a test of
	 * self), so please retain it.
	 *
	 * @param i int to add/clear
	 * @param value true to add, false to clear.
	 */
	public void set(int i, boolean value) {
		if ( value ) { // add 'i' to bits
			if ( (bits & ISHFT[i]) == 0 ) { // 'i' is not already in bits
				bits |= ISHFT[i]; // set the bit
				++size; // bump-up the count
			}
		} else { // remove 'i' from bits
			if ( (bits & ISHFT[i]) != 0 ) { // 'i' is in bits
				bits &= ~ISHFT[i]; // unset the bit
				--size; // knock-down the count
			}
		}
	}

	// ------------------------------- mutators -------------------------------

	// these methods mutate this Indexes relative to it's current state

	/** Add 'i' to this Indexes Set.
	 * @param i int to add.
	 * @return this Indexes. */
	public Indexes add(int i) {
		// if 'i' is unset (0) in bits
		if ( (bits & ISHFT[i]) == 0 ) { // 'i' is not already in bits
			bits |= ISHFT[i]; // set the bit
			++size; // bump-up the count
		}
		return this;
	}

	/** Adds the indexes in these 'other' {@code Indexes} to this Indexes Set.
	 * @param other {@code Indexes}.
	 * @return this Indexes. */
	public Indexes add(Indexes other) {
		this.size = ISIZE[bits |= other.bits];
		return this;
	}

	/** Remove 'i' from this Indexes Set.
	 * @param i int to clear. */
	public void remove(int i) {
		// if 'i' is set (1) in bits
		if ( (bits & ISHFT[i]) != 0 ) {
			bits &= ~ISHFT[i]; // unset the bit
			--size; // knock-down the count
		}
	}

	// ---------------- queries ----------------
	// these methods read (but do NOT alter) the existing state of this Indexes.

	/**
	 * Is 'i' in this Indexes Set.
	 *
	 * @param i int to query.
	 * @return boolean.
	 */
	public boolean contains(int i) {
		return (bits & ISHFT[i]) != 0;
	}

	/** Does this Indexes Set contain all these values?
	 * <p>NOTE only currently used by test-cases but I'm keep it anyway.
	 * @param values {@code int...} an arguments array of int.
	 * @return boolean. */
	public boolean containsAll(int... values) {
		for ( int v : values )
			if ( (bits & ISHFT[v]) == 0 )
				return false;
		return true;
	}

	/** @return is this Indexes Set empty? */
	public boolean isEmpty() {
		return bits == 0; // true if there are NO bits set "on".
	}

	/** @return the number of indexes in this Indexes Set. */
	public int size() {
		return size = ISIZE[bits];
	}

	/** @return the binary String representation of this Indexes Set. */
	public String toBinaryString() {
		String s = Integer.toBinaryString(bits);
		return ZEROS[s.length()] + s;
	}
	private static final String[] ZEROS = new String[]{
			  "000000000", "00000000", "0000000", "000000", "00000"
			, "0000", "000", "00", "0", ""
	};

	/** @return index of the first (rightmost) set (1) bit, else NONE (-1). */
	public int first() {
		if ( bits == 0 )
			return NONE;
		return FIRST_INDEX[bits];
	}

	/** @return index of the last (leftmost) set (1) bit, else NONE (-1). */
	public int last() {
		// this is/seems faster than numberOfLeadingZeros on my i7.
		if ( bits == 0 )
			return NONE;
		return (int)(Math.log(bits)/LOG2);
	}

	/** @return the next index from 'i' inclusive, else NONE (-1). */
	public int next(int i) {
		final int b = bits & (ONES<<i);
		if ( b == 0 )
			return NONE;
		return FIRST_INDEX[b];
	}

	public int otherThan(int i) {
		return FIRST_INDEX[bits & ~ISHFT[i]];
	}

	// ---------------- toArray ----------------
	// toArray reads but does NOT alter the state of this Indexes.

	/**
	 * Populates the given array (which is presumed to be big enough) with the
	 * indexes that are in this Indexes Set, and return the count; and also
	 * zeros the remainder of the array.
	 * <p>
	 * Prefer this method to the no-arg {@link #toArray()} in a loop because
	 * it is MUCH faster to not create a garbage-array for each element. Just
	 * because you have a garbage-collection-service do NOT expect them to
	 * empty your industrial-bin three times a bloody second! Use your head!
	 *
	 * @param array to populate (and clear remainder).
	 * @return the number of values in the 'array'. This allows you to reuse
	 *  one array in a loop, instead of creating garbage arrays willy-nilly.
	 */
	public int toArray(final int[] array) {
		int count = 0;
		for ( int i : INDEXES[bits] )
			array[count++] = i;
		for ( int j=count, N=array.length; j<N; ++j )
			array[j] = 0;
		return count;
	}

	/**
	 * Creates and returns a new int array of 'size' containing the indexes
	 * that are in this Indexes Set.
	 * <p>
	 * <b>WARNING:</b> Prefer {@link #toArray(int[])} in a loop!
	 * This method is only ever used "sparingly" and never in a loop; like
	 * when you're creating a hint, or in a test-case. Never in a loop!
	 * <p>
	 * <b>BUT:</b> If you MUST toArray() in a loop (external code) then try
	 * making toArray() "borrow" an array from Idx.ias (beware of collisions),
	 * or roll your own ias with reference to Idx.ias: You won't ever use
	 * anything beyond size 9 shoes. Big hands I know you're the one. Sigh.
	 * <p>
	 * Food for thought: I probably should have a utils class which allows one
	 * to "borrow" and "return" arrays from a cache using Closable interface:
	 * {@code int[] a; try ( a=IntArrayCache.get(size()) ) { ... use a ... }}
	 * but we have the problem of all loans: remembering to repay it! If we
	 * "borrow" in a "standard" like toArray we're reliant upon the caller
	 * knowing that the array must be "returned", which is just pie-in-the-sky
	 * wishful thinking, IMHO. I should learn how to return a "soft reference"
	 * to an array so that the caller cannot hold library property against the
	 * wishes of the librarian. But won't work with toArray where the return
	 * type is fixed as a hard native reference. So I know knufffink! So it is
	 * what it is. Not perfect. Just useful. Prefer toArray(int[]) in a LOOP!
	 *
	 * @return a new {@code int[]} array.
	 */
	public int[] toArray() {
		final int[] array = new int[size()];
		toArray(array);
		return array;
	}

	// ---------------- toString ----------------
	// toString reads but does NOT alter the state of this Indexes.

	/** Appends the string representation of this Indexes Set, separating each
	 * value with 'sep', except the last which is preceeded by 'lastSep'.
	 * @param sb to append to.
	 * @param bits the bitset of the index to be appended
	 * @param n the number of set (1) bits in the index to append. If you don't
	 * know then just pass me Indexes.ISIZE[bits].
	 * @param sep values separator.
	 * @param lastSep the last values separator.
	 * @return the given 'sb' so that you can chain method calls. */
	public static StringBuilder appendTo(StringBuilder sb, int bits, int n
			, String sep, String lastSep) {
		int count = 0;
		for ( int i : INDEXES[bits] ) {
			if ( ++count > 1 ) // 1 based count
				if ( count < n ) // 1 based, so n (not m)
					sb.append(sep);
				else
					sb.append(lastSep);
			sb.append(i);
		}
		return sb;
	}

	/** Appends the string representation of this Indexes Set, separating each
	 * value with 'sep', except the last which is preceeded by 'lastSep'.
	 * @param sb to append to.
	 * @param sep values separator.
	 * @param lastSep the last values separator.
	 * @return the given 'sb' so that you can chain method calls. */
	public StringBuilder appendTo(StringBuilder sb, String sep, String lastSep){
		return appendTo(sb, bits, size, sep, lastSep);
	}

	/** Appends the string representation of this Indexes Set to the given
	 * StringBuilder.
	 * @param sb to append to.
	 * @return the given 'sb' so that you can chain method calls. */
	public StringBuilder appendTo(StringBuilder sb) {
		for ( int i : INDEXES[bits] )
			sb.append(i);
		return sb;
	}

//2020-10-23 not used, but keep for ummm, a while
//	/** As if new Indexes(bits).toString(); without the garbage Indexes.
//	 * @param bits to stringify.
//	 * @return Indexes string representation of bits.
//	 */
//	public static String toString(int bits) {
//		SB.setLength(0);
//		appendTo(SB, bits, SIZE[bits], "", "");
//		return SB.toString();
//	}

	@Override
	public String toString() {
		SB.setLength(0);
		appendTo(SB);
		return SB.toString();
	}
	private static final StringBuilder SB = new StringBuilder(NUM_BITS);
	
	public static String toString(int bits) {
		SB.setLength(0);
		for ( int i : INDEXES[bits] )
			SB.append(i);
		return SB.toString();
	}

	// ---------------- plumbing ----------------
	// dealing with all Javas s__t.

	@Override
	public boolean equals(Object o) {
		if ( !(o instanceof Indexes) )
			return false;
		Indexes other = (Indexes)o;
		return this.bits == other.bits
			&& this.size == other.size;
	}
	public boolean equals(Indexes that) {
		return this.bits == that.bits
			&& this.size == that.size;
	}

	/** WARNING: bits is NOT an invariant! I don't intend to use an Index Set as
	 * a HashKey so I don't think it'll be a problem. If you want to then I
	 * suggest you put the bit-value into an immutable Integer and use it as
	 * the hash-key instead.
	 * @return int - A dodgy volatile hash-code. */
	@Override
	public int hashCode() {
		return this.bits;
	}

	// ------------------------------- Iterator -------------------------------
	//
	// DEPRECATED: because All iterators are a bit s__t because they involve
	// two method calls for each element: ie inherently an O(2n) operation.
	// The standard Java ethos is: yeah but it doesn't cost much. Hmmm. So:
	// NOOBS: please use toArray instead, coz it's faster coz it doesn't call
	// TWO methods PER ELEMENT, just one method full stop! So an array-iterator
	// is much faster than an Iterator.

	@Override
	@Deprecated
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int curr = -1; // current index = before first
			private int next = Integer.MAX_VALUE; // next index (invalid)
			@Override
			public boolean hasNext(){
				for ( int i=curr+1; i<9; ++i )
					if ( (ISHFT[i] & bits) != 0 ) {
						next = i;
						return true;
					}
				return false;
			}
			@Override
			public Integer next() {
//				for ( int i=curr+1; i<9; ++i )
//					if ( (SHFT[i] & bits) != 0 )
//						return curr = i;
//				throw new NoSuchElementException("Indexes iterator past EOL");
// Dodgy, but faster! Relies on hasNext before each next, which is hard to NOT
// do when using an iterator. The exception is when you just want to grab the
// first-and-only-element from a collection, so you just call next; which will
// FAIL with this dodgy setup, but I'm not going to do that! So: I'm sure this
// is all bad practice, but I'm doing it anyway. If you needed speed you would
// NOT use an iterator, you'd iterate INDEXES[bits] instead, so long as you
// don't need the iterator to reflect bitset changes. The iterator should be
// reliable UNDER ANY AND ALL FORSEEABLE CIRCUMSTANCES, and this one isn't.
// So "yeah, yeah, na": I'm not real sure about this Tim!
				int result = curr = next;
// Set next to MAX_VALUE as protection against not calling hasNext before next.
// Better to fail fast than end-up in an endless loop: The stopping problem.
				next = Integer.MAX_VALUE;
				return result;
			}
			@Override
			public void remove(){
				if ( curr == -1 )
					throw new IllegalStateException("remove before next");
				Indexes.this.remove(curr);
			}
		};
	}

}
