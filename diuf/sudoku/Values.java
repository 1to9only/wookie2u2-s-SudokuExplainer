/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.utils.Frmt;
import java.util.Iterator;
import static diuf.sudoku.utils.Frmt.*;
import java.util.Collection;

/**
 * A 1-based (nonstandard) java.util.BitSet'ish set of the values 1..9.
 * <p>
 * I feel I must mention that {@code java.util.BitSet} is a bit s__t. The
 * only reason one ever uses a BitSet is performance, and Javas expansible
 * implementation is so non-performant that it is rendered fundamentally
 * useless. In my humble opinion, it exists just to sell you faster hardware,
 * and having purchased faster hardware, it'll still be faster NOT to use it!
 * <p>
 * Thanks to Stack Overflow for answers to many questions, including:
 * http://stackoverflow.com/questions/1092411/java-checking-if-a-bit-is-0-or-1-in-a-long
 * <p>
 * This is open source LGPL software. Use/fix/adapt it. It's not my problem.
 * <p>
 * NOTE: That because Values is wholey structured around there being 9 values,
 * and much of this s__t won't work for 16 values (unlike most of the code) so
 * it does NOT use Grid.REGION_SIZE or any of the related constants. Basically
 * if you upsize the grid (being an idiot) then you must completely rewrite the
 * Values class, from the bottom-up.
 *
 * @author Keith Corlett 2013 Aug
 */
public final class Values implements Iterable<Integer> {

	// ----------------------------- static stuff -----------------------------

	/** The value returned by various methods meaning "Not found". */
	private static final int NONE = -1;

	/** get last() a tad faster than numberOfLeadingZeros on my box. */
	private static final double LOG2 = Math.log(2);

	/** 32 1's: to left-shift for a partial word mask. */
	private static final int ONES = 0xffffffff; // 8 F's = 8 * 4 1's = 32 1's

	/**
	 * Produces a new int-array of the "value" of each set (1) bit in bits. The
	 * "value" of each set bit is it's distance-from-the-right-hand-end + 1 of
	 * the given bits in a standard LSR (Least Significant Right), just like
	 * the "normal" decimal number representation, except in binary. It's just
	 * a bit faster using an array, and cleaner.
	 * <p>
	 * For example: Given bits=9 (binary 1001) I return {1, 4}, an array of the
	 * distance of each set (1) bit from the right + 1.
	 *
	 * @param bits the bits to extract "distances" from. If the given bits is 0
	 *  then a zero-length array is returned.
	 * @return an array of the distance-from-right + 1 of each set (1) bit.
	 */
	public static int[] toValuesArrayNew(int bits) {
		final int size = Integer.bitCount(bits); // Don't use SIZE array here!
		int[] a = new int[size];
		int cnt = 0;
		// nb: Don't use SHFT (as per normal) in case it doesn't exist yet
		for ( int sv=1,i=1; sv<=bits; sv<<=1,++i )
			if ( (bits & sv) != 0 ) // the sv bit is set in bits
				a[cnt++] = i;
		assert cnt == size;
		return a;
	}

	/**
	 * Produces a new int-array of the left-shifted bitset representation of
	 * each set (1) bit in the given 'bits', to facilitate array-iteration of
	 * the set bits in a bitset, instead of repeatedly working-it-out ourself
	 * in-situ. It's just a bit faster using an array, and cleaner.
	 * <p>
	 * For example: Given bits=9 (binary 1001) I return {1, 8}, an array of
	 * the binary-value of each set (1) bit.
	 *
	 * @param bits
	 * @return
	 */
	private static int[] toShiftedArrayNew(int bits) {
		final int size = Integer.bitCount(bits); // Don't use SIZE array here!
		int[] a = new int[size]; // array
		int count = 0;
		// nb: Don't use SHFT (as per normal) in case it doesn't exist yet
		for ( int sv=1; sv<=bits; sv<<=1 ) // shiftedValue
			if ( (bits & sv) != 0 ) // the sv bit is set in bits
				a[count++] = sv;
		assert count == size;
		return a;
	}

	/**
	 * An array of bitset values. More formally, I'm an array of jagged-arrays
	 * of all the possible maybes, as left-shifted bitset values.
	 * First index is your maybes, which is a bitset, ie 0..511.
	 * The second index depends on the number of maybes, between 0 and 9.
	 * <p>
	 * The <b>old-school</b> method skips unset bits:
	 * <pre>{@code
	 *    final int bits = c0.maybes; // cache it coz it's hammered
	 *    for ( int sv=1; sv<=bits; sv<<=1 ) // shiftedValue
	 *        if ( (bits & sv) != 0 )
	 *            // process c0's potential value
	 * }</pre>
	 * becomes the <b>new-school</b> method using Values.VSHIFTED:
	 * <pre>{@code {@code
	 *   // NB: Testing says the new-school method is about }<b>25% faster.</b><br>{@code
	 *   final int[][] VSS = Values.VSHIFTED; // a local reference for speed
	 *   ....
	 *   for ( int sv : VSS[c0.maybes] ) // shiftedValue
	 *       // process c0's potential value
	 * }}</pre>
	 * <p>
	 * There's a marked performance impost somewhere in A9E, which I think is
	 * when there's multiple concurrent iterations of the one array. I think
	 * that each array comes with an iterator, and the VM creates additional
	 * iterators on the fly (ie they're not pooled), so that creating the first
	 * iterator is "free", so impossibly fast has become a normative reference.
	 * <p>
	 * This shows up in A9E where there's many more "collisions". It might be
	 * faster to do the first 4 or 5 values old-school and do the later 4 or 5
	 * values new-school, just to reduce collisions. Big Sigh.
	 * <p>
	 * <b>contents:</b><pre>{@code
	 *   VSS[  0] = {} // an empty array
	 *   VSS[  1] = {1}
	 *   VSS[  2] = {2}
	 *   VSS[  3] = {2,1}
	 *   VSS[  4] = {4}
	 *   VSS[  5] = {4,1}
	 *   VSS[  6] = {4,2}
	 *   VSS[  7] = {4,2,1}
	 *   VSS[  8] = {8}
	 *   VSS[  9] = {8,1}
	 *   VSS[ 10] = {8,2}
	 *   VSS[ 11] = {8,2,1}
	 *   VSS[ 12] = {8,4}
	 *   VSS[ 13] = {8,4,1}
	 *   VSS[ 14] = {8,4,2}
	 *   VSS[ 15] = {8,4,2,1}
	 *   VSS[ 16] = {16}
	 *   VSS[ 17] = {16,1}
	 *   ... and so on up to ...
	 *   VSS[511] = {256,128,64,32,16,8,4,2,1} ie {9,8,7,6,5,4,3,2,1}
	 * }</pre>
	 */
	public static final int[][] VSHIFTED = new int[512][];
	static {
		for ( int i=0; i<512; ++i ) // 512 for 9 bits
			VSHIFTED[i] = Values.toShiftedArrayNew(i);
	}

	/**
	 * An array of jagged-arrays of the values (unshifted) that are packed
	 * into the first index as a bitset (ie your cell.maybes).
	 * <p>
	 * <pre>{@code {@code
	 *   // NB: Testing says the new-school method is about }<b>25% faster.</b><br>{@code
	 *   final int[][] VS = Values.VALUESES; // a local reference for speed
	 *   ....
	 *   for ( int v : VS[c0.maybes] ) // value
	 *       // process c0's potential value
	 * }}</pre>
	 * <b>contents:</b><pre>{@code
	 *   VS[  0] = {} // an empty array
	 *   VS[  1] = {1}
	 *   VS[  2] = {2}
	 *   VS[  3] = {2,1}
	 *   VS[  4] = {3}
	 *   VS[  5] = {3,1}
	 *   VS[  6] = {3,2}
	 *   VS[  7] = {3,2,1}
	 *   VS[  8] = {4}
	 *   VS[  9] = {4,1}
	 *   VS[ 10] = {4,2}
	 *   VS[ 11] = {4,2,1}
	 *   VS[ 12] = {4,3}
	 *   VS[ 13] = {4,3,1}
	 *   VS[ 14] = {4,3,2}
	 *   VS[ 15] = {4,3,2,1}
	 *   VS[ 16] = {5}
	 *   VS[ 17] = {5,1}
	 *   ... and so on up to ...
	 *   VS[511] = {9,8,7,6,5,4,3,2,1}
	 * }</pre>
	 */
	public static final int[][] VALUESES = new int[512][];
	/** The number of elements in this element of the SHIFTED/VALUESES array:
	 * <pre>{@code
	 *   assert SIZE[c.maybes] == VALUESES[c.maybes].length;
	 *   assert SIZE[c.maybes] == SHIFTED[c.maybes].length;
	 *   assert SIZE[c.maybes] == Integer.bitCount(c.maybes);
	 * }</pre> */
	public static final int[] VSIZE = new int[512];
	static {
		for ( int i=0; i<512; ++i )
			VSIZE[i] = (VALUESES[i]=toValuesArrayNew(i)).length;
	}

	// The magnitude of the lowest order set bit in each bitset + 1.
	// A clone of Indexes.IFIRST with 1 added to each element.
	public static final int[] VFIRST = Indexes.IFIRST.clone();
	static {
		for ( int i=0; i<VFIRST.length; ++i )
			++VFIRST[i];
	}

	/** The minimum value storable in this 1-based Values Set is 1. */
	private static final int MIN_VALUE = 1;
	/** The maximum value storable in this Values Set plus 1 is 10. */
	private static final int VALUE_CEILING = 10;

	/** The number of all values: 9 */
	public static final int VALL_SIZE = VALUE_CEILING-1;

	// VALL also works for Indexes: VALL is just 9 (1) bits, regardless of
	// whether those bits represent 1..9 (Values) or 0..8 (Indexes).
	/** The bits of all values (1,2,3,4,5,6,7,8,9) == 111,111,111 == 511 */
	public static final int VALL = (1<<VALL_SIZE)-1;

	/** An array of shifted bitset-values (faster than 1&lt;&lt;v-1) with a
	 * representation of 0 (which isn't a value) but makes SHFT[1] the shifted
	 * value of 1, as you'd expect. Ie: without 0 in the array you'd have to do
	 * SHFT[value-1] instead of just SHFT[value], which'd suck even more than
	 * this small brown lump of ____. */
	public static final int[] VSHFT = new int[VALUE_CEILING]; // 10
	/** An array of "negated" bitset-values (faster than ~(1&lt;&lt;v-1)) */
	static {
		for ( int v=MIN_VALUE; v<VALUE_CEILING; ++v )
			VSHFT[v] = 1<<v-1;
	}

	/**
	 * Returns a bitset containing the 'degree' values that are common to all
	 * of these 'valueses', else 0. Also returns 0 if any 'valueses' has size
	 * less than 2.
	 *
	 * @param valueses {@code Values[]}
	 * @param degree the maximum size of a "common" values
	 * @return the common Values (a new instance), else null
	 */
	public static int common(int[] valueses, int degree) {
		int bits = 0;
		for ( int values : valueses )
			if ( VSIZE[values]<2 || VSIZE[bits|=values]>degree )
				return 0;
		if ( VSIZE[bits] == degree )
			return bits;
		return 0;
	}

	/** Creates a new filled (1,2,3,4,5,6,7,8,9) Values Set. */
	static Values all() {
		return new Values(VALL, VALL_SIZE, false);
	}
	/** Creates a new empty () Values Set. */
	static Values none() {
		return new Values(0, 0, false);
	}

	/**
	 * Return a new Values containing elements in 'a' plus elements in 'b'.
	 *
	 * @param a first Values
	 * @param b second Values
	 * @return a new Values containing 'a' | 'b'
	 */
	public static Values newOr(Values a, Values b) {
		return new Values(a.bits|b.bits, false);
	}

	/**
	 * Return a new array of the values in bits: a Values.bits.
	 * @param bits a bitset of values
	 * @return a new array of the values in bits.
	 */
	public static int[] arrayOf(int bits) {
		final int[] result = new int[VSIZE[bits]];
		int cnt = 0;
		for ( int v : VALUESES[bits] )
			result[cnt++] = v;
		// null terminate the array (if length > size)
		if ( cnt < result.length )
			result[cnt] = 0;
		return result;
	}

	/**
	 * Return a new array of plain (unshifted) values of these 'n' 'cands'.
	 *
	 * @param cands an array of bitsets
	 * @param n number of cands in cands
	 * @return a new array of {@code VFIRST[cands[i]]} of each
	 */
	public static int[] toValues(final int[] cands, final int n) {
		int[] result = new int[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = VFIRST[cands[i]];
		}
		return result;
	}

	/**
	 * Return a bitset of candidates in 's'.
	 *
	 * @param s to parse
	 * @return a bitset of candidates in 's'.
	 */
	public static int parse(String s) {
		int bitset = 0;
		for ( int i=0,n=s.length(); i<n; ++i )
			bitset |= VSHFT[s.charAt(i)-'0'];
		return bitset;
	}

	// for test-cases
	public static int bitset(int a) { return VSHFT[a]; }
	public static int bitset(int a, int b) { return VSHFT[a] | VSHFT[b]; }
	public static int bitset(int a, int b, int c) { return VSHFT[a] | VSHFT[b] | VSHFT[c]; }
	public static int bitset(int a, int b, int c, int d) { return VSHFT[a] | VSHFT[b] | VSHFT[c] | VSHFT[d]; }
//	public static int bitset(int a, int b, int c, int d, int... others) {
//		int bitset = VSHFT[a] | VSHFT[b] | VSHFT[c] | VSHFT[d];
//		for ( int o : others )
//			bitset |= VSHFT[o];
//		return bitset;
//	}

	public static int bitset(int[] values) {
		int result = 0;
		for ( int i=0,n=values.length; i<n; ++i )
			result |= VSHFT[values[i]]; // set value bit
		return result;
	}

	// ------------------------------ attributes ------------------------------

	/** readonly to public. A bitset of this Values Set. */
	public int bits;
	/** readonly to public. The number of values in this Values Set. */
	public int size;

	// ----------------------------- constructors -----------------------------

	/** Constructs a new empty set of Values 1..9 */
	public Values() {
		bits = size = 0;
	}

	/**
	 * Construct a new Values Set containing the given (unshifted) value.
	 *
	 * @param value the value (unshifted) to store.
	 */
	public Values(int value) {
		assert value>0 && value<10;
		bits = VSHFT[value];
		size = 1;
	}

	/** Constructs a new Values Set containing v1 and v2.
	 * @param v1 first value to set.
	 * @param v2 second value to set. */
	public Values(int v1, int v2) {
		assert v1!=0 && v2!=0 : "Zero: v1="+v1+" v2="+v2;
		// nb: v1==v2 in an XYZ-Wing, and it's valid.
		size = VSIZE[bits = VSHFT[v1] | VSHFT[v2]];
	}

	/** Constructs a new Values Set containing the given values.
	 * @param values {@code int[]} an int array of the values to set. */
	public Values(int[] values) {
		this.size = VSIZE[bits = bitset(values)];
	}

	/** Constructs a new Values containing the given bits. Size is calculated,
	 * which is pretty expensive by comparison to the next (bits, size, dummy)
	 * constructor or the Copy Constructor, which should therefore be preferred.
	 * @param bits int the bitset value.
	 * @param dummy is not used. This parameter just distinguishes this
	 * constructor among it's many alternatives.
	 */
	public Values(int bits, boolean dummy) {
		if ( bits<0 || bits>511 ) // is 0 to 9 set bits
			throw new IllegalArgumentException("bits "+bits+" not 0..511");
		size = VSIZE[this.bits=bits];
		assert size > -1;
		assert size < 10;
	}

	/** Constructs a new Values Set containing the given bits and size.
	 * <p>Be careful: the given values are just set, for speed, not validated.
	 * @param bits the bitset value
	 * @param size the number of set bits (ones).
	 * @param dummy */
	public Values(int bits, int size, boolean dummy) {
		this.bits = bits;
		this.size = size;
		assert bits > -1;  // ie not negative
		assert bits < 512; // ie less than 1<<10
		assert size > -1;
		assert size < 10;
	}

	/** Constructs a new Values Set containing these 'valueses'.
	 * @param valueses {@code Iterable<Values>} to set. */
	public Values(Iterable<Values> valueses) {
		int b = 0;
		for ( Values values : valueses )
			b |= values.bits;
		this.size = VSIZE[this.bits=b];
	}

	/** Constructs a new Values containing the values in 's'.
	 * @param s String to set. */
	public Values(String s) {
		int b = 0;
		for ( int i=0,n=s.length(); i<n; ++i )
			b |= VSHFT[s.charAt(i)-'0'];
		this.size = VSIZE[this.bits=b];
	}

	/** Constructs a new Values Set containing the 'src' values.
	 * @param src {@code Values} to set. */
	public Values(Values src) {
		if ( src == null )
			bits = size = 0; // ie empty
		else { // blindly copy whatever is in src, even if it's broken
			bits = src.bits;
			size = src.size;
		}
	}

	// ------------------------------- mutators -------------------------------

	/** Blindly sets this values to the 'src' Values. */
	void copyFrom(Values src) {
		bits = src.bits;
		size = src.size;
	}

	/** Sets all values (ie indexes) in this Values. */
	public void fill() {
		bits = VALL; //111,111,111
		size = VALL_SIZE; //9
	}

	/** Clears all values (ie indexes) from this Values. */
	public void clear() {
		bits = size = 0;
	}

	/** Remove the given 'value' from this Values Set, and return my new size.
	 * @param value to remove.
	 * @return the subsequent (post removal) size of this Values Set. */
	public int remove(int value) {
		if ( (bits & VSHFT[value]) != 0 ) {
			bits &= ~VSHFT[value]; // unset the value'th bit
			--size;
		}
		return size;
	}

	/** Remove these 'other' Values from this Values Set.
	 * @param values to clear.
	 * @return this Values, for chaining. */
	public Values remove(Values values) {
		size = VSIZE[bits &= ~values.bits];
		return this;
	}

	/** Remove these 'bits' from this Values Set.
	 * @param bits to clear.
	 * @return the new size. */
	public int removeBits(int bits) {
		return size = VSIZE[this.bits &= ~bits];
	}

	/**
	 * Set this Values to the given value only, and adjust size to 1.
	 * @param value
	 */
	public void set(int value) {
		this.bits = VSHFT[value];
		size = 1;
	}

	/**
	 * Set this Values to the given bits, and adjust size accordingly.
	 * @param bits
	 */
	public void setBits(int bits) {
		size = VSIZE[this.bits=bits];
	}

	/** Add this 'value' to this Values Set.
	 * @param value to add.
	 * @return was the value actually added (or did it already exist)? */
	public boolean add(int value) {
		if ( (bits & VSHFT[value]) == 0 ) {
			bits |= VSHFT[value]; // set the value'th bit
			++size;
			return true;
		}
		return false;
	}

	/** Add these two 'values' to this Values Set.
	 * @param v0
	 * @param v1
	 * @return this Values. */
	public Values add(int v0, int v1) {
		size = VSIZE[bits |= VSHFT[v0] | VSHFT[v1]];
		return this;
	}

	/** Add these 'values' to this Values Set.
	 * @param values {@code int[]} an arguments array of ints to add.
	 * @return this Values. */
	public Values add(int[] values) {
		int sv; // shiftedValue
		for ( int i=0,n=values.length; i<n; ++i ) {
			sv = VSHFT[values[i]];
			if ( (bits & sv) == 0 ) {
				bits |= sv; // set the value'th bit
				++size;
			}
		}
		return this;
	}

	/** Add these 'bits' to this Values Set.
	 * @param bits a Values.bits of the values to be added
	 * @return was any value actually added.
	 */
	boolean addBits(int bits) {
		int pre = size;
		return (size=VSIZE[this.bits |= bits]) > pre;
	}

	/** Set this Values Set to the digits in the given String.
	 * <p>String format is raw digits. EG: "1569"
	 * @param s digits to set. */
	public void set(String s) {
		size = s.length();
		bits = 0;
		for ( int i=0; i<size; ++i )
			bits |= VSHFT[s.charAt(i)-'0'];
	}

	/** Add these 'other' Values to this Values Set.
	 * @param other
	 * @return this Values. */
	public Values add(Values other) {
		size = VSIZE[bits |= other.bits];
		return this;
	}

	// ------------------------------- factories ------------------------------

	/** Create a new Values Set containing the intersection of this and 'a'.
	 * <p>ie: {@code new Values(this).and(a)}
	 * <p>ie: The values in both this and 'a'.
	 * @param a {@code Value}.
	 * @return a new Values. */
	public Values intersect(Values a) {
		return new Values(bits & a.bits, false);
	}

	/** Create a new Values containing the intersection of this and 'a' and 'b',
	 * ie values that are common to all three Values Sets.
	 * <p>ie: {@code new Values(this).and(a).and(b)}
	 * <p>ie: {@code return new Values(bits & a.bits & b.bits, false);}
	 * <p>ie: The values that are common to all of this and 'a' and 'b'.
	 * @param a {@code Value}.
	 * @param b {@code Value}.
	 * @return a new Values. */
	public Values intersect(Values a, Values b) {
		return new Values(bits & a.bits & b.bits, false);
	}

	/** Create a new Values containing the intersection of this and 'value'.
	 * <p>ie: {@code new Values(this).and(v) // doesn't exist for int}
	 * @param value int
	 * @return a new Values. */
	public Values intersect(int value) {
		return new Values(bits & VSHFT[value], false);
	}

	/** Create a new Values containing the intersection of this and 'bitset'.
	 * @param bitset the left-shifted-bitset of the values to intersect with.
	 * @return a new Values. */
	public Values intersectBits(int bitset) {
		return new Values(bits & bitset, false);
	}

	/** Create a new Values Set containing this one minus the given value.
	 * <p>ie: {@code new Values(thisOne).clear(v)}
	 * @param value int to clear.
	 * @return a new Values. */
	public Values minus(int value) {
		return new Values(bits & ~VSHFT[value], false);
	}

	/** Create a new Values Set containing the values that are in this Values
	 * Set but are not in Values 'a'.
	 * <p>ie: {@code new Values(this).andNot(a)}
	 * @param a {@code Values}
	 * @return a new Values. */
	public Values minus(Values a) {
		return new Values(bits & ~a.bits, false);
	}

	/** Create a new Values Set containing the values that are in this Values
	 * Set, but are not in Values 'a', clearing 'value'.
	 * <p>ie: {@code new Values(this).andNot(a).clear(value)}
	 * @param a {@code Values} to remove.
	 * @param value int to clear.
	 * @return a new Values. */
	public Values minus(Values a, int value) {
		return new Values(bits & ~a.bits & ~VSHFT[value], false);
	}

	/** Create a new Values Set containing the values that in this Values Set,
	 * minus v1, minus v2.
	 * <p>ie: {@code new Values(this).clear(v1).clear(v2)}
	 * @param v0 to clear.
	 * @param v1 to clear.
	 * @return a new Values. */
	public Values minus(int v0, int v1) {
		return new Values(bits & ~VSHFT[v0] & ~VSHFT[v1], false);
	}

	/** Create a new Values Set containing the values in this Values Set
	 * or in the given Values 'a'.
	 * <p>ie: {@code new Values(this).add(a)}
	 * @param a {@code Values} to add.
	 * @return a new Values. */
	public Values plus(Values a) {
		return new Values(bits | a.bits, false);
	}

	/** Create a new Values containing the values in this Values Set
	 * or in the given Values 'a' or 'b'.
	 * <p>ie: {@code new Values(this).add(a).add(b)}
	 * <p>ie: {@code return new Values(bits | a.bits | b.bits, false);}
	 * @param a {@code Values} to add.
	 * @param b {@code Values} to add.
	 * @return a new Values. */
	public Values plus(Values a, Values b) {
		return new Values(bits | a.bits | b.bits, false);
	}

	/**
	 * Create a new Values containing the values in this Values Set
	 * or in the given Values 'a', minus v1, minus v2.
	 * <p>ie: {@code new Values(this).add(a).clear(v1).clear(v2)}
	 *
	 * @param a {@code Values} to add.
	 * @param v1 to clear.
	 * @param v2 to clear.
	 * @return a new Values.
	 */
	public Values plusClear(Values a, int v1, int v2) {
		return new Values((bits | a.bits) & ~VSHFT[v1] & ~VSHFT[v2], false);
	}

	/** Retain only those values which are in the keepers bitset, removing
	 * all the rest, and return the new size.
	 * @param keepers
	 */
	int retainAll(int keepers) {
		return size = VSIZE[bits &= keepers];
	}

	public void and(Values a) {
		size = VSIZE[bits & a.bits];
	}

	// -------------------------------- queries -------------------------------

	/** Is the given 'value' in this Values Set?
	 * @param value to query.
	 * @return boolean. */
	public boolean contains(int value) {
		return (bits & VSHFT[value]) != 0;
	}

	/** Is the given 'value' <b>NOT</b> in this Values Set?
	 * @param value to query.
	 * @return boolean. */
	public boolean no(int value) {
		return (bits & VSHFT[value]) == 0;
	}

	/**
	 * Does this Values Set contain all of these 'other' values?
	 *
	 * @param other {@Values} to look for.
	 * @return boolean.
	 */
	public boolean containsAll(Values other) {
		return other.bits>0 && (bits & other.bits)==other.bits;
	}

	/** Does this Values Set contain ONLY these 'other' Values?
	 * @param other
	 * @return boolean. */
	public boolean isSubsetOf(Values other) {
		if ( other.bits == 0 )
			throw new UnsolvableException("other is empty");
		return this.bits!=0 && (this.bits & ~other.bits)==0;
	}

	/** @return bits == 0 */
	public boolean isEmpty() {
		return bits == 0; // true if there are NO bits set "on".
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ iterator ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// these methods can be combined as an alternative to an iterator.

	/** @return the first (right-most) index in this Values Set,
	 * else NONE (-1). */
	public int first() {
		if ( bits == 0 )
			return NONE; // WTF: was 0 pre KRC 2021-01-22
		return VFIRST[bits];
	}

//not used 2020-11-23, but there's solid logic for keeping it anyway
	/** @return The last (left-most) index in this Values Set,
	 * else NONE (-1). */
	public int last() {
		// this is a tad faster than numberOfLeadingZeros on my box.
		if ( bits == 0 )
			return NONE;
		return (int)(Math.log(bits)/LOG2)+1;
	}

	/** Returns the next value in this Values Set that is greater-than-or-
	 * equal-to the given 'v', else NONE (-1).
	 * @param v to look from <b>(INCLUSIVE)</b>.
	 * @return int the next value. */
	public int next(int v) {
		assert v > 0;
		int b = bits & (ONES << v-1);
		// let it AIOOBE if v is too big!
		if ( b == 0 )
			return NONE;
		return VFIRST[b];
	}

//not used 2020-11-23, but there's solid logic for keeping it anyway
	/** Return the next value (1..9) that is <b>NOT</b> in this Values Set which
	 * is greater-than-or-equal-to the given 'v', else NONE (-1).
	 * @param v 1..10: is presumed be a value that is in this set
	 * @return next missing value */
	public int next0(int v) {
		assert v>0 && v<=VALUE_CEILING; // nb MAX is invalid but allowed
		for ( int z=v; z<VALUE_CEILING; ++z )
			if ( (bits & VSHFT[z]) == 0 )
				return z;
		return NONE;
	}

	/**
	 * Get the other value in this Values Set (which contains 2 values).
	 * Used by XYWingHint to get the x and y values from xz and yz.
	 * @param zValue the value you don't want, which MUST be in this set.
	 * @return the other value in this Set.
	 */
	public int otherThan(int zValue) {
//		assert Integer.bitCount(bits) == 2;
//		assert (bits & ~VSHFT[zValue]) != 0;
//		assert Integer.bitCount(bits & ~VSHFT[zValue]) == 1;
		return VFIRST[bits & ~VSHFT[zValue]];
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ toArray & friends ~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Populates the given array (which is presumed to be big enough) with the
	 * values that are in this Values Set, and return the count.
	 * <p>Also sets array[count] = 0 as a "null terminator" (not relied upon).
	 * <p>Prefer this method to the {@link #toArrayNew()} in a loop!
	 *
	 * @param array to populate (and clear remainder).
	 * @return the number of values in the 'array'. This allows you to reuse
	 * one array in a loop, instead of creating garbage arrays willy-nilly.
	 */
	public int toArray(final int[] array) {
		int cnt = 0;
		for ( int v : VALUESES[bits] )
			array[cnt++] = v;
		// null terminate the array (if length > size)
		if ( cnt < array.length )
			array[cnt] = 0;
		return cnt;
	}

	/**
	 * Let's pretend that the given bits is already a values, and read it into
	 * a values array already, because I'm a lazy sneeky cheating bastard.
	 * @param bits
	 * @return
	 */
	public static int[] toArrayNew(final int bits) {
		int[] array = new int[VSIZE[bits]];
		int i = 0;
		for ( int v : VALUESES[bits] )
			array[i++] = v;
		return array;
	}

	/**
	 * Creates and returns a new int array of 'size' elements containing the
	 * values that are in this Values Set.
	 * <p>
	 * <b>WARNING:</b> Use this method sparingly, never in a loop.
	 * Prefer {@link #toArray(int[])} in a loop.
	 *
	 * @return a new {@code int[]} array.
	 */
	public int[] toArrayNew() {
		int[] a = new int[size];
		toArray(a);
		return a;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ toString & friends ~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static final StringBuilder SB = new StringBuilder(3*VALUE_CEILING+5);

	/** Appends the string representation of bits (a Values bitset) to the
	 * given StringBuilder, in "plain format". EG: "1389"
	 * @param sb to append to.
	 * @param bits int to format.
	 * @return the given 'sb' so that you can chain method calls. */
	public static StringBuilder appendTo(StringBuilder sb, int bits) {
		if ( bits == 0 )
			return sb.append(MINUS);
		for ( int v : VALUESES[bits] )
			sb.append(v);
		return sb;
	}

//not_used
//	/** Appends the string representation of this Values to the
//	 * given StringBuilder, in "plain format". EG: "1389"
//	 * @param sb to append to.
//	 * @return the given 'sb' so that you can chain method calls. */
//	public StringBuilder appendTo(StringBuilder sb) {
//		return Values.appendTo(sb, this.bits);
//	}

	/**
	 * Returns a String representation of the given bits.
	 * @param bits Values.bits (commonly called maybes, bitset, or just bits).
	 * @return 7 => 1, 2, 3
	 */
	public static String toString(int bits) {
		SB.setLength(0);
		return Values.appendTo(SB, bits).toString();
	}

//not_used
//	/**
//	 * Returns a String representation of the given bits, for debugging.
//	 * @param bits Values.bits (commonly called maybes, bitset, or just bits).
//	 * @return 7 => 3:123
//	 */
//	public static String toFullString(int bits) {
//		SB.setLength(0);
//		SB.append(VSIZE[bits]).append(":");
//		Values.appendTo(SB, bits);
//		return SB.toString();
//	}

	/**
	 * Returns a String representation of these Values.
	 * @return 7 => 1, 2, 3
	 */
	@Override
	public String toString() {
		return Values.toString(bits);
	}

//not used 2020-10-23 but it's really only for debugging anyway, so kept
	/** @return Integer.toBinaryString(bits) left padded with 0's to 9-chars */
	public String toBinaryString() {
		String s = Integer.toBinaryString(bits);
		return ZEROS[s.length()] + s;
	}
	private static final String[] ZEROS = new String[]{
			  "000000000", "00000000", "0000000", "000000", "00000"
			, "0000", "000", "00", "0", ""
	};

	public static String and(Collection<Integer> c) {
		if(c==null) return NULL_ST;
		SB.setLength(0);
		final int m = c.size() - 1;
		int i = 0;
		for ( Integer value : c ) {
			if ( ++i > 1 )
				if ( i<m )
					SB.append(CSP);
				else
					SB.append(AND);
			SB.append(Integer.toString(value));
		}
		return SB.toString();
	}

	public static String orString(int bits) { return toString(bits, CSP, OR); }
	public static String andString(int bits) { return toString(bits, CSP, AND); }
	public static String csv(int bits) { return toString(bits, CSP, CSP); }
	public static String ssv(int bits) { return toString(bits, SP, SP); }

	/**
	 * A fancy (with field separators) toString for displaying Values in hints.
	 * @param bits
	 * @param sep
	 * @param lastSep
	 * @return
	 */
	public static String toString(int bits, String sep, String lastSep) {
		if ( bits == 0 )
			return MINUS;
		SB.setLength(0);
		final int[] values = VALUESES[bits];
		final int n = values.length;
		int i = 0;
		for ( int v : values ) {
			if ( ++i > 1 )
				if ( i < n )
					SB.append(sep);
				else
					SB.append(lastSep);
			SB.append(v);
		}
		return SB.toString();
	}

	/**
	 * ToString of AlignedExclusion's array of values bits.
	 * <p>
	 * Used only for debugging.
	 * @param valueses
	 * @param numValueses
	 * @return
	 */
	public static String toString(int[] valueses, int numValueses) {
		SB.setLength(0);
		for ( int i=0; i<numValueses; ++i ) {
			if(i>0) SB.append(',');
			for ( int v : VALUESES[valueses[i]] )
				SB.append(v);
		}
		return SB.toString();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ plumbing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean equals(Object other) {
		return other instanceof Values
			&& ((Values)other).bits == this.bits;
	}
	public boolean equals(Values other) {
		return other.bits == this.bits;
	}

	/** WARNING: bits is NOT an invariant! I don't intend to use a Values Set
	 * as a HashKey so I don't think it'll be a problem. If you want to then I
	 * suggest you put the bit-value into an Integer, which is immutable, and
	 * use it as the hash-key instead.
	 * @return int A dodgy <b>volatile</b> hash-code. */
	@Override
	public int hashCode() {
		return this.bits;
	}

	// ------------------------------- iterator -------------------------------

	// DEPRECATED: because all iterators are much slower than array-iterators,
	// so toArray the bastard, and iterate the elements speedily yourself.

	@Override
	@Deprecated
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int cv = 0; // currentValue = before first
			private int nv = 0; // nextValue = before first
			@Override
			public boolean hasNext(){
				for ( nv=cv+1; nv<10; ++nv ) // start at currentValue+1
					if ( (VSHFT[nv] & bits) != 0 )
						return true;
				return false;
			}
			@Override
			public Integer next(){
//KRC 2019-09-19 Making iterator faster but less safe. It'll now ONLY work
// in "normal operation" where hasNext() is called before each next() because
// hasNext() is doing all the advancing, and next() just returns the results.
// I allways knew that iterating a bitset was a bad idea, so let's dodgy it up.
//				for ( cv=nv; cv<10; ++cv ) // start at nextValue
//					if ( (SHFT[cv] & bits) != 0 ) // normal operation tests ONCE
//						nv = cv + 1; // incase next() without hasNext();
//						return cv;
//				throw new NoSuchElementException("Values iterator past EOL");
				return cv = nv;
			}
			@Override
			public void remove(){
				if ( cv == 0 )
					throw new IllegalStateException("remove before next");
				Values.this.remove(cv);
			}
		};
	}

}