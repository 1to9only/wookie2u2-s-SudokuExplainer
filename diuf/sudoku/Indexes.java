/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import java.util.Iterator;

/**
 * Indexes are region indexes, ergo places in a region which maybe a value.
 * <p>
 * SE no longer uses instances of the Index class. Instead each index is a
 * native int, that is manipulated using Index constants and native operations.
 * This is faster, but spreads "implementation details" of the Index format
 * throughout SE's codebase. This is how things where done "in the olden days"
 * before object-oriented programming came along to save us from our own
 * inflexibility. So, my "native" approach is fast but inflexible. The Index
 * format will not need to change while the Sudoku grid remains 9x9; hence SE
 * is "welded" to a 9x9 grid. If you need variable grid sizes try another app.
 * <p>
 * Indexes is a 0-based (standard) Bitset of 32 bits. The position of each
 * set (1) bit represents the indice of a cell in a region. The "shape" of
 * the region (ie if its a Box, Row, or Col) does not matter.
 * <p>
 * The interface is inspired by {@code java.util.Set<Integer>} which is NOT
 * implemented because this is a Set of ints and performance dictates that
 * we avoid the overheads of boxing/unboxing the ints to/from Integers.
 * <p>
 * This is free open-source software. Use and adapt it as you like, but you
 * accept ALL responsibility.
 * <p>
 * Thanks to Stack Overflow contributors for answers to many questions.
 *
 * @author Keith Corlett 2013 August
 */
public final class Indexes implements Iterable<Integer>, Cloneable {

//	public static final long[] COUNTS = new long[2];

	// ---------------- static stuff ----------------

	/** The value returned by various methods meaning "Not found": -1. */
	private static final int NONE = -1;

	/** for last() speed: faster Integer.numberOfLeadingZeros(bits). */
	private static final double LOG2 = Math.log(2);

	/** Shifted left for a partial word mask. */
	private static final int ONES = 0xffffffff;

	/** The number of bits in an Index is 9. */
	private static final int NUM_INDEXES_BITS = 9; //invariant: NUM_BITS<32
	/** The size of the array/shifted is {@code 1<<NUM_BITS}. */
	private static final int ARRAY_SIZE = 1<<NUM_INDEXES_BITS; // 1<<9 = 512
	/** An array of the "shifted" bitset-values (faster than {@code 1<<v)}. */
	public static final int[] ISHFT = new int[NUM_INDEXES_BITS];
	/** An array-of-arrays of the unshifted values. The first index is your
	 * bitset => array of the values in that bitset. */
	public static int[][] INDEXES = new int[ARRAY_SIZE][];
//not used
//	/** An array-of-arrays of the left-shifted-bitset-values. The first index
//	 * is your bitset => array of the SHFTed values in that bitset. */
//	public static int[][] SHIFTED = new int[ARRAY_SIZE][];
	/** An array of Integer.numberOfTrailingZeros is faster than repeatedly
	 * calling the sucker on these same values. We can do this because there
	 * is only 512 values in question. */
	public static final int[] IFIRST = new int[ARRAY_SIZE];
	static {
		for ( int i=0; i<NUM_INDEXES_BITS; ++i )
			ISHFT[i] = 1<<i;
		for ( int i=0; i<ARRAY_SIZE; ++i) {
			INDEXES[i] = toIndexesArrayNew(i);
//			SHIFTED[i] = toShiftedArray(i);
			IFIRST[i] = Integer.numberOfTrailingZeros(i);
		}
	}

	// this one is public pseudonym
	public static final int[] ILAST = Values.VLAST;
	// a private copy, minimising exposure to the outside world
	private static final int BITS9 = Values.BITS9;

	/**
	 * An array of Integer.bitCount of 0..511, for speed.
	 * <p>
	 * NB: The number of bits in a bitset is the same regardless of whether the
	 * first bit represents 0 (Indexes) or 1 (Values); hence Indexes.ISIZE is
	 * actually Values.VSIZE, that is Indexes.ISIZE is just a pseudonym for the
	 * single array, which is "owned" by the Values class. Indexes.ISIZE exists
	 * to avoid getting Values.VSIZE of an Indexes bitset, thereby erroneously
	 * implying that this is a Values bitset. Keeping my s__t straight is hard
	 * enough without such inbuilt pitfalls.
	 */
	public static final int[] ISIZE = Values.VSIZE;

	/** this static factory method is currently only used in test cases. */
	static Indexes empty() {
		return new Indexes(0); // int bits
	}

	/**
	 * "Unpacks" the given bits into an array of the index of each set (1)
	 * bit in the given bits.<br>
	 * For example: given binary {@code 1101} we return {@code {0,2,3}}.
	 * <p>
	 * WARN: This method is hijacked by Idx static initialiser, so do NOT
	 * introduce anything (like SHFT) which assumes we know the set.size. There
	 * are API methods that handle all possibilities, they are just slower, so
	 * do not ____ with it. If you must ____ with it then test everything.
	 *
	 * @param bits to be unpacked
	 * @return given binary {@code 1101} we return {@code {0,2,3}}.
	 */
	public static int[] toIndexesArrayNew(int bits) {
		final int n = Integer.bitCount(bits);
		final int[] result = new int[n];
		int cnt = 0;
		for ( int i=0; cnt<n; ++i )
			if ( (bits & (1<<i)) > 0 ) // 9bits
				result[cnt++] = i;
		assert cnt == n; // even if they are both 0
		return result;
	}

	/**
	 * "Unpacks" the given bitset into an array of the outright value of each
	 * set (1) bit in the given bitset.
	 * <p>
	 * Example: given binary {@code 1101} we return {@code {1,4,8}}.
	 *
	 * @param bitset to be unpacked
	 * @return given binary {@code 1101} we return {@code {1,4,8}}.
	 */
	public static int[] toShiftedArrayNew(int bitset) {
		final int n = Integer.bitCount(bitset);
		int[] result = new int[n];
		int cnt = 0;
		for ( int sv=1; cnt<n; sv<<=1 )
			if ( (bitset & sv) > 0 ) // 9bits
				result[cnt++] = sv;
		assert cnt == n; // even if they are both 0
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
		bits = BITS9;
		size = NUM_INDEXES_BITS;
	}

	/**
	 * Constructs a new Indexes Set containing the given raw $bits.
	 *
	 * @param bits to set (a left-shifted bitset).
	 */
	public Indexes(int bits) {
		this.size = ISIZE[this.bits = bits];
	}

	/**
	 * Constructs a new Indexes Set containing the given $indexes.
	 * <p>
	 * Now only used in test-cases, for readability. Angry people use
	 * the bits constructor.
	 * <p>
	 * eg: {@code Indexes idxs = new Indexes(0,1,2,3,8);}
	 *
	 * @param indexes {@code int...} an arguments array of "normal"
	 *  (NOT&nbsp;left-shifted) indexes.
	 */
	public Indexes(int... indexes) {
		for ( int i=0,n=indexes.length; i<n; ++i )
			bits |= ISHFT[indexes[i]]; // add idx to my bits
		this.size = ISIZE[bits]; // in case the two idxs were not distinct
	}

	/**
	 * Constructs a new Indexes Set containing the digits in $s.
	 *
	 * @param s String of digits EG: "01238" to set.
	 */
	public Indexes(String s) {
		for ( int i=0, n=this.size=s.length(); i<n; ++i )
			this.bits |= ISHFT[s.charAt(i)-'0'];
	}

	/**
	 * Copy-con: Constructs a new Indexes Set containing the indexes in $src.
	 *
	 * @param src {@code Indexes} to copy.
	 */
	public Indexes(Indexes src) {
		this(src.bits);
	}

	// -------------------------------- setters -------------------------------

	// these methods set this Indexes outright, overwritting existing.

	/**
	 * Clears (empties) this Indexes Set.
	 */
	public final void clear() {
		bits = size = 0;
	}

	/**
	 * Fills (0,1,2,3,4,5,6,7,8) this Indexes Set.
	 */
	public final void fill() {
		bits = BITS9;
		size = NUM_INDEXES_BITS;
	}

	/**
	 * Set this Indexes to the given bits, and look-up the size.
	 * This method sets the Index.bits field directly.
	 *
	 * @param bits a bitset of the indexes to set
	 */
	public void setBits(final int bits) {
		size = ISIZE[this.bits = bits];
	}

	/**
	 * Add/remove $index to/from this Indexes Set, depending on $isAdd.
	 * <p>
	 * <b>RETAIN</b> 2023-10-17 test.diuf.sudoku.GridTest#testRebuildEverything
	 * uses this set as a utility: 462 add, 1725 remove. KEEP ME!
	 *
	 * @param index int to add/clear
	 * @param isAdd true to add, false to remove.
	 */
	public void set(int index, boolean isAdd) {
		if ( isAdd ) // add $i to bits and update size
			size = ISIZE[bits |= ISHFT[index]];
		else // remove $i from bits and update size
			size = ISIZE[bits &= ~ISHFT[index]];
	}

	// ------------------------------- mutators -------------------------------

	// these methods mutate this Indexes relative to its current state

	/**
	 * Add $index to this Indexes Set.
	 *
	 * @param index int to add.
	 * @return this Indexes.
	 */
	public Indexes add(int index) {
		// add $i to bits and update size
		size = ISIZE[bits |= ISHFT[index]];
		return this;
	}

	/**
	 * Adds the $other indexes to this Indexes Set.
	 *
	 * @param other {@code Indexes}.
	 * @return this Indexes.
	 */
	public Indexes add(Indexes other) {
		size = ISIZE[bits |= other.bits];
		return this;
	}

	/**
	 * Remove $index from this Indexes Set.
	 *
	 * @param index to remove
	 * @return the new size
	 */
	public int remove(int index) {
		// remove $i from bits and update size
		size = ISIZE[bits &= ~ISHFT[index]];
		return size;
	}

	// ---------------- queries ----------------
	// these methods read (but do NOT alter) the existing state of this Indexes.

	/**
	 * Is $index in this Indexes Set.
	 *
	 * @param index to query
	 * @return is the $index bit set (1) in this Indexes Set
	 */
	public boolean contains(int index) {
		return (bits & ISHFT[index]) > 0; // 9bits
	}

	/**
	 * Does this Indexes Set contain all these $indexes?
	 * <p>
	 * NOTE currently used by test-cases only, but I am keeping it anyway.
	 * Varargs invocation is a bit slow for a search.
	 *
	 * @param indexes {@code int...} an arguments array of int.
	 * @return boolean.
	 */
	public boolean containsAll(int... indexes) {
		for ( int index : indexes )
			if ( (bits & ISHFT[index]) == 0 )
				return false;
		return true;
	}

	/**
	 * Is this Indexes Set empty?
	 *
	 * @return does this Indexes Sets bits == 0?
	 */
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

	/**
	 * Return my first index.
	 *
	 * @return the index of my first (rightmost) set (1) bit, else NONE (-1).
	 */
	public int first() {
		if ( bits == 0 )
			return NONE;
		else
			return IFIRST[bits];
	}

	/**
	 * Return the first index in the given Index bits.
	 *
	 * @param bits an Indexes 9bit bitset
	 * @return the index of the first (rightmost) set (1) bit, else NONE (-1)
	 */
	public static int first(final int bits) {
		if ( bits == 0 )
			return NONE;
		else
			return IFIRST[bits];
	}

	/**
	 * Get the last index in this Indexes Set.
	 *
	 * @return index of the last (leftmost/highest) set (1) bit in an LSBR
	 *  world, else NONE (-1).
	 */
	public int last() {
		if ( bits == 0 )
			return NONE;
		// faster than Integer.numberOfLeadingZeros(bits);
		return (int)(Math.log(bits)/LOG2);
	}

	/**
	 * Get the next index following $index from this Indexes Set.
	 *
	 * @param index to find the one after
	 * @return the next index from $index inclusive, else NONE (-1).
	 */
	public int next(int index) {
		final int b = bits & (ONES<<index);
		if ( b == 0 )
			return NONE;
		return IFIRST[b];
	}

	public int otherThan(int i) {
		return IFIRST[bits & ~ISHFT[i]];
	}

	// ---------------- toArray ----------------
	// toArray reads but does NOT alter the state of this Indexes.

	/**
	 * Populates the given array (which is presumed to be big enough) with the
	 * indexes that are in this Indexes Set, and return the count; and also
	 * zeros the remainder of the array.
	 * <p>
	 * Prefer this method to the no-arg {@link #toArrayNew()} in a loop because
	 * it is MUCH faster to not create a garbage-array for each element. Just
	 * because you have a garbage-collection-service do NOT expect them to
	 * empty your industrial-bin three times a bloody second! Use your head!
	 *
	 * @param result to populate (and clear remainder).
	 * @return the number of values in the $result. This allows you to reuse
	 *  one array in a loop, instead of creating garbage arrays willy-nilly.
	 */
	public int toArray(final int[] result) {
		int count = 0;
		for ( int i : INDEXES[bits] )
			result[count++] = i;
		for ( int j=count, N=result.length; j<N; ++j )
			result[j] = 0;
		return count;
	}

	/**
	 * Creates and returns a new int array of $size containing the indexes
	 * that are in this Indexes Set.
	 * <p>
	 * <b>WARN:</b> Prefer {@link #toArray(int[])} in a loop!
	 * toArrayNew is used sparingly, ie not in a loop; like when creating a
	 * hint, or in a test-case. Never in a tight-loop!
	 * <p>
	 * <b>BUT:</b> If you MUST toArray() in a loop then you could borrow an
	 * array with {@link diuf.sudoku.IntArrays#iaLease} in a try-block and
	 * use toArray instead! Angering the GC is the easiest way to bog a JVM.
	 * Think of the GC as a lap-hulk. Pet him, but do NOT make him angry;
	 * which perversely is EXACTLY what my cache does. Love you long time!
	 *
	 * @return a new {@code int[]} array.
	 */
	public int[] toArrayNew() {
		final int[] result = new int[size()];
		toArray(result);
		return result;
	}

	// ---------------- toString ----------------

	// debug only: not used in anger
	@Override
	public String toString() {
		return toString(bits);
	}

	// debug only: not used in anger
	public static String toString(final int bits) {
		final StringBuilder sb = SB(NUM_INDEXES_BITS);
		for ( int index : INDEXES[bits] )
			sb.append(index);
		return sb.toString();
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

	/**
	 * WARN: bits is NOT invariant! No use Index as hashKey so no problem.
	 * If you must then put bits in an Integer and use it as hashKey instead.
	 *
	 * @return bits: a dodgy volatile cashHode
	 */
	@Override
	public int hashCode() {
		return this.bits;
	}

	// ------------------------------- Iterator -------------------------------
	//
	// DEPRECATED: because All iterators are a bit s__t because they involve
	// two method calls for each element: ie inherently an O(2n) operation.
	// The standard Java ethos is: yeah but it does not cost much. Hmmm. So:
	// NOOBS: please use toArray instead, coz its faster coz it does not call
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
					if ( (ISHFT[i] & bits) > 0 ) { // 9bits
						next = i;
						return true;
					}
				return false;
			}
			@Override
			public Integer next() {
//				for ( int i=curr+1; i<9; ++i )
//					if ( (SHFT[i] & bits) > 0 ) // 9bits
//						return curr = i;
//				throw new NoSuchElementException("Indexes iterator past EOL");
// Dodgy, but faster! Relies on hasNext before each next, which is hard to NOT
// do when using an iterator. The exception is when you just want to grab the
// first-and-only-element from a collection, so you just call next; which will
// FAIL with this dodgy setup, but I am not going to do that! So: I am sure this
// is all bad practice, but I am doing it anyway. If you needed speed you would
// NOT use an iterator, you would iterate INDEXES[bits] instead, so long as you
// do not need the iterator to reflect bitset changes. The iterator should be
// reliable UNDER ANY AND ALL FORSEEABLE CIRCUMSTANCES, and this one is not.
// So "yeah, yeah, na": I am not real sure about this Tim!
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
