/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.MAYBES;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.utils.Frmt;
import java.util.Iterator;
import static diuf.sudoku.utils.Frmt.*;
import java.util.Collection;

/**
 * A 1-based (nonstandard) bitset of the values 1..9.
 * <p>
 * SE no longer uses instances of Values and Indexes, instead it uses native
 * ints, and relies heavily on the constants defined in Values and Indexes.
 * <p>
 * IMHO, {@code java.util.BitSet} is a bit s__t. We use BitSet for performance,
 * and Javas expansible implementation is non-performant, hence it is useless.
 * IMHO, BitSet exists to sell you faster hardware, and having purchased faster
 * hardware, it will still be faster NOT to use it.
 * <p>
 * Thanks to Stack Overflow for answers to many questions, including:
 * http://stackoverflow.com/questions/1092411/java-checking-if-a-bit-is-0-or-1-in-a-long
 * <p>
 * This is open source LGPL software. Use/fix/adapt it. It is not my problem.
 * <p>
 * NOTE: Values is wholey structured around there being 9 values, and much of
 * this s__t will not work for 16 values (unlike most code) so it does NOT use
 * Grid.REGION_SIZE or any of the related constants. Basically, if you are a
 * moron who upsizes the Grid then you rewrite the Values class (for a start)
 * from the bottom-up. And (AFAIK) this s__t can't work beyond 9, but there are
 * other (slower) ways of doing it.
 * <p>
 * If you require a adjustable-size Sudoku grid then I recommend starting with
 * another free open-source Sudoku solving software. SE is welded onto 9x9. If
 * you CAN workout how to do these tricks (or anything like them) with a 16x16
 * Grid, then I am all ears.
 * <p>
 * NOTE WELL that Values instances are no-longer used. Each former usage is
 * just an int bitset in Values format, that is manipulated using my public
 * constants, so my public constants are now used extensively throughout SE.
 * This is NOT a good design. I have done so for speed. This approach spreads
 * "implementation details" of the Values internal format through-out the
 * codebase; which is how things were done BEFORE object-oriented languages
 * came along to save us from our own inflexibility. If you are crazy enough to
 * change the Values format you will have to make thousands of code-changes all
 * over the shop. Not ideal! Use a tool. Write a tool. A tool for a tool.
 * <p>
 * So I plead guilty but my defence is straight forward. The Values format will
 * NEVER need to change. Its a solid 9x9 defence, so long as 9x9 rules the
 * roost, as it, in my humble opinion, should. Every other possibility produces
 * easier Sudoku puzzles. Sudoku exists for the challenge, not for the easy.
 * Hence every other size and region type was invented be some moron who could
 * not see the maths that underpins the genius of the standard Sudoku format.
 * There has never been a shortage of morons. So much water...
 * <p>
 * So, to me, Sudoku is an intellectual 9x9 mountain. Every other mountain is
 * less than ideal. Get over it!
 *
 * @author Keith Corlett 2013 Aug
 */
public final class Values implements Iterable<Integer> {

	// ----------------------------- static stuff -----------------------------

	/** The value returned by various methods meaning "Not found". */
	private static final int NONE = -1;

	/** for last() speed: faster Integer.numberOfLeadingZeros(bits). */
	private static final double LOG2 = Math.log(2);

	/** ONES is 32 1's: to left-shift for a partial word mask. */
	private static final int ONES = 0xffffffff; // 32 1's

	/** There are 512 possible combinations of the 9 digits 1..9. */
	public static final int NUM_MAYBES = 512;

	/**
	 * Produces a new int-array of the "value" of each set (1) bit in bits. The
	 * "value" of each set bit is its distance-from-the-right-hand-end + 1 of
	 * the given bits in a standard LSR (Least Significant Right), just like
	 * the "normal" decimal number representation, except in binary. Its just
	 * a bit faster using an array, and cleaner.
	 * <p>
	 * For example: Given bits=9 (binary 1001) I return {1, 4}, an array of the
	 * distance of each set (1) bit from the right + 1.
	 *
	 * @param bits the bits to extract "distances" from. If the given bits is 0
	 *  then a zero-length array is returned.
	 * @return an array of the distance-from-right + 1 of each set (1) bit.
	 */
	public static int[] toValuesArrayNew(final int bits) {
		final int size = Integer.bitCount(bits); // I set the VSIZE array!
		final int[] result = new int[size];
		int count = 0;
		// nb: Dont use SHFT (as per normal) in case it doesnt exist yet
		for ( int sv=1,i=1; sv<=bits; sv<<=1,++i )
			// if the sv bit is set in bits
			if ( (bits & sv) > 0 ) // 9bits (is NEVER negative)
				result[count++] = i;
		assert count == size;
		return result;
	}

	/**
	 * Produces a new int-array of the left-shifted bitset representation of
	 * each set (1) bit in the given $bits, to facilitate array-iteration of
	 * the set bits in a bitset, instead of repeatedly working-it-out ourself
	 * in-situ. Its just a bit faster using an array, and cleaner.
	 * <p>
	 * For example: Given bits=9 (binary 1001) I return {1, 8}, an array of
	 * the binary-value of each set (1) bit.
	 *
	 * @param bits
	 * @return
	 */
	private static int[] toShiftedArrayNew(final int bits) {
		final int size = Integer.bitCount(bits); // Dont use SIZE array here!
		final int[] result = new int[size]; // array
		int count = 0;
		// nb: Dont use SHFT (as per normal) in case it doesnt exist yet
		for ( int sv=1; sv<=bits; sv<<=1 ) // shiftedValue
			// if the sv bit is set in bits
			if ( (bits & sv) > 0 ) // 9bits (is NEVER negative)
				result[count++] = sv;
		assert count == size;
		return result;
	}

	/**
	 * An array of bitset values. More formally, I am an array of jagged-arrays
	 * of all the possible maybes, as left-shifted bitset values.
	 * <p>
	 * First index is your maybes, which is a bitset, ie 0..511. <br>
	 * The second index is a jagged-array of length 0 to 9. The length of this
	 * array is the number of maybes in this index, which varies from bitset to
	 * bitset, but generally increasing as the index grows.
	 * <p>
	 * The <b>old-school</b> method skips unset bits:
	 * <pre>{@code
	 *    final int bits = c0.maybes; // cache it coz it is hammered
	 *    for ( int sv=1; sv<=bits; sv<<=1 ) // shiftedValue
	 *        if ( (bits & sv) > 0 ) // 9bits
	 *            // process c0s potential value
	 * }</pre>
	 * becomes the <b>new-school</b> method using Values.VSHIFTED:
	 * <pre>{@code {@code
	 *   // NB: Testing says the new-school method is about }<b>25% faster.</b><br>{@code
	 *   final int[][] VSS = Values.VSHIFTED; // a local reference for speed
	 *   ....
	 *   for ( int shiftedValue : VSS[maybes[c0]] )
	 *       // process c0s potential value
	 * }}</pre>
	 * <p>
	 * <b>contents:</b><pre>{@code
	 *   VSS[  0] = {} // an empty int[]
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
	public static final int[][] VSHIFTED = new int[NUM_MAYBES][];
	static {
		for ( int i=0; i<NUM_MAYBES; ++i ) // 512 for 9 bits
			VSHIFTED[i] = Values.toShiftedArrayNew(i);
	}

	/**
	 * VALUESES is an array of jagged-arrays of the values that are in the
	 * cell.maybes bitset that is my index, so VALUESES[7] is {1,2,3}.
	 * <p>
	 * NOTE: This trick works on 9-values, but 16-values is too many. It would
	 * use too much RAM, I think.
	 * <p>
	 * <b>usage</b>:<pre>{@code
	 *   for ( int v : VALUESES[maybes[indice]] ) {
	 *      whatever
	 *   }
	 * }</pre>
	 * <p>
	 * <b>contents:</b><pre>{@code
	 *   VS[  0] = {} // an empty array
	 *   VS[  1] = {1}
	 *   VS[  2] = {2}
	 *   VS[  3] = {1,2}
	 *   VS[  4] = {3}
	 *   VS[  5] = {1,3}
	 *   VS[  6] = {2,3}
	 *   VS[  7] = {1,2,3}
	 *   VS[  8] = {4}
	 *   VS[  9] = {1,4}
	 *   VS[ 10] = {2,4}
	 *   VS[ 11] = {1,2,4}
	 *   VS[ 12] = {3,4}
	 *   VS[ 13] = {1,3,4}
	 *   VS[ 14] = {2,3,4}
	 *   VS[ 15] = {1,2,3,4}
	 *   VS[ 16] = {5}
	 *   VS[ 17] = {1,5}
	 *   ... and so on up to ...
	 *   VS[511] = {1,2,3,4,5,6,7,8,9}
	 * }</pre>
	 */
	public static final int[][] VALUESES = new int[NUM_MAYBES][];
	/**
	 * VSIZE holds the bitCount of its index, ie the number of elements in this
	 * element of the SHIFTED/VALUESES array:
	 * <pre>{@code
	 *   assert SIZE[c.maybes] == VALUESES[c.maybes].length;
	 *   assert SIZE[c.maybes] == SHIFTED[c.maybes].length;
	 *   assert SIZE[c.maybes] == Integer.bitCount(c.maybes);
	 * }</pre>
	 * An array lookup is MUCH faster than Integer.bitCount. The downside is
	 * VSIZE is limited to 9bits, whereas bitCount goes to 32bits. My PC has
	 * 16 gig of RAM, so Integer.bitCount should cache Integer.populationCount,
	 * which shall remain public. Just saying.
	 */
	public static final int[] VSIZE = new int[NUM_MAYBES];
	/**
	 * VLAST is the last index of VALUESES, ergo VSIZE - 1.
	 */
	public static final int[] VLAST = new int[NUM_MAYBES];
	static {
		for ( int i=0; i<NUM_MAYBES; ++i ) {
			VSIZE[i] = (VALUESES[i]=toValuesArrayNew(i)).length;
			VLAST[i] = VSIZE[i] - 1;
		}
	}

	// The magnitude of the lowest order set bit in each bitset + 1.
	// A clone of Indexes.IFIRST with 1 added to each element.
	public static final int[] VFIRST = Indexes.IFIRST.clone();
	static {
		VFIRST[0] = 0;
		for ( int i=1; i<VFIRST.length; ++i )
			++VFIRST[i];
	}

	/** The minimum value storable in this 1-based Values is 1. */
	private static final int MIN_VALUE = 1;
	/** The number of all values: 9. */
	public static final int VALL_SIZE = REGION_SIZE;
	/** The maximum value storable in a Values + 1 is 10. */
	private static final int VALUE_CEILING = VALL_SIZE + MIN_VALUE;
	// BITS9 (formerly VALL) works for Indexes et al: its just 9 (1) bits,
	// regardless of what those bits represent.
	/** The bits of all values (1,2,3,4,5,6,7,8,9) == 111,111,111 == 511. */
	public static final int BITS9 = (1<<VALL_SIZE)-1;

	/**
	 * Result Pots say "I'm an On" by SET_BIT (the tenth bit) in there values.
	 * On Results, by definition, have ONE value. When the hint is applied the
	 * cell is set to value.
	 */
	public static final int SET_BIT = 1<<9;

	/**
	 * VSHFT is an array of shifted-bitset-values, which is a bit faster than
	 * repeatedly doing {@code 1<<(v-1)}).
	 * <p>
	 * Note that VSHFT[0] is 0, but 0 is not a value, so it matters not.
	 * <p>
	 * Using VSHFT saves a tiny cost per use, but we do it ?trillions? of times
	 * in a top1465 batch-run, and it all adds-up, hence precalculating all
	 * such invariants is significantly faster overall.
	 */
	public static final int[] VSHFT = new int[VALUE_CEILING]; // 10
	static {
		// nb: VSHFT[0] remains 0
		for ( int v=MIN_VALUE; v<VALUE_CEILING; ++v )
			VSHFT[v] = 1 << (v-1);
	}

	/**
	 * Creates a new filled (1,2,3,4,5,6,7,8,9) Values Set.
	 */
	static Values all() {
		return new Values(BITS9, VALL_SIZE, false);
	}
	/**
	 * Creates a new empty () Values Set.
	 */
	static Values none() {
		return new Values(0, 0, false);
	}

	/**
	 * Return a new Values containing elements in $a plus elements in $b.
	 *
	 * @param a first Values
	 * @param b second Values
	 * @return a new Values containing $a | $b
	 */
	public static Values newOr(final Values a, final Values b) {
		return new Values(a.bits|b.bits, false);
	}

	/**
	 * Return a bitset of candidates in $s.
	 *
	 * @param s to parse, for example: "12369"
	 * @return a maybes bitset of the candidates in $s.
	 */
	public static int parse(final String s) {
		int result = 0; // a maybes bitset of values 1..9
		char c;
		for ( int i=0,n=s.length(); i<n; ++i )
			if ( (c=s.charAt(i))>='1' && c<='9' )
				result |= VSHFT[c-'0'];
		return result;
	}

	/**
	 * For test-cases ONLY. This is a varargs call. Varargs are neat and handy.
	 * Every varargs invocation has a small overhead. If such a method goes in
	 * a loop there is a marked drop in performance, compared to a non-varargs
	 * alternative. Now if you put that loop in another loop you're walking to
	 * Mars with the hand-break on; so just dont. Do or do not. There is no
	 * hand-job. Slam that one down Granny. That's all. Test-cases excepted.
	 *
	 * @param values to enbitonate
	 * @return bits
	 */
	public static int bitset(final int... values) {
		int result = 0;
		for ( int v : values )
			result |= VSHFT[v];
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
	public Values(final int value) {
		assert value>0 && value<10;
		bits = VSHFT[value];
		size = 1;
	}

	/**
	 * Constructs a new Values Set containing v1 and v2.
	 *
	 * @param v1 first value to set.
	 * @param v2 second value to set.
	 */
	public Values(final int v1, final int v2) {
		assert v1!=0 && v2!=0 : "Zero: v1="+v1+" v2="+v2;
		// nb: v1==v2 in an XYZ-Wing, and its valid.
		size = VSIZE[bits = VSHFT[v1] | VSHFT[v2]];
	}

	/**
	 * Constructs a new Values Set containing the given values.
	 *
	 * @param values {@code int[]} an int array of the values to set.
	 */
	public Values(final int[] values) {
		this.size = VSIZE[bits = bitset(values)];
	}

	/**
	 * Constructs a new Values containing the given bits. Size is calculated,
	 * which is pretty expensive by comparison to the next (bits, size, dummy)
	 * constructor or da Copy Constructor, which should therefore be preferred.
	 *
	 * @param bits int the bitset value.
	 * @param dummy is not used. This parameter just distinguishes this
	 *  constructor among its many alternatives.
	 */
	public Values(final int bits, final boolean dummy) {
		if ( bits<0 || bits>511 ) // is 0 to 9 set bits
			throw new IllegalArgumentException("bits "+bits+" not 0..511");
		size = VSIZE[this.bits=bits];
		assert size > -1;
		assert size < 10;
	}

	/**
	 * Constructs a new Values Set containing the given bits and size.
	 * <p>
	 * Be careful: the given values are just set, for speed, not validated.
	 *
	 * @param bits the bitset value
	 * @param size the number of set bits (ones).
	 * @param dummy
	 */
	public Values(final int bits, final int size, final boolean dummy) {
		this.bits = bits;
		this.size = size;
		assert bits > -1;  // ie not negative
		assert bits < NUM_MAYBES; // ie less than 1<<10
		assert size > -1;
		assert size < 10;
	}

	/**
	 * Constructs a new Values Set containing these $valueses.
	 *
	 * @param valueses {@code Iterable<Values>} to set.
	 */
	public Values(final Iterable<Values> valueses) {
		int b = 0;
		for ( Values values : valueses )
			b |= values.bits;
		this.size = VSIZE[this.bits=b];
	}

	/**
	 * Constructs a new Values containing the values in $s.
	 *
	 * @param s String to set.
	 */
	public Values(final String s) {
		this.size = VSIZE[this.bits=parse(s)];
	}

	/**
	 * Constructs a new Values Set containing the $src values.
	 *
	 * @param src {@code Values} to set.
	 */
	public Values(final Values src) {
		if ( src == null )
			bits = size = 0; // ie empty
		else { // blindly copy whatever is in src, even if its broken
			bits = src.bits;
			size = src.size;
		}
	}

	// ------------------------------- mutators -------------------------------

	/** Blindly sets this values to the $src Values. */
	void copyFrom(final Values src) {
		bits = src.bits;
		size = src.size;
	}

	/** Sets all values (ie indexes) in this Values. */
	public void fill() {
		bits = BITS9; //111,111,111
		size = VALL_SIZE; //9
	}

	/** Clears all values (ie indexes) from this Values. */
	public void clear() {
		bits = size = 0;
	}

	/**
	 * Remove the given $value from this Values Set, and return my new size.
	 *
	 * @param value to remove.
	 * @return the subsequent (post removal) size of this Values Set.
	 */
	public int remove(int value) {
		// unset the valueth bit, and update the size
		size = VSIZE[bits &= ~VSHFT[value]];
		return size;
	}

	/**
	 * Remove these $other Values from this Values Set.
	 *
	 * @param values to clear.
	 * @return this Values, for chaining.
	 */
	public Values remove(Values values) {
		// set the bits, and update the size
		size = VSIZE[bits &= ~values.bits];
		return this;
	}

	/**
	 * Remove these $bits from this Values Set.
	 *
	 * @param bits to clear.
	 * @return the new size.
	 */
	public int removeBits(int bits) {
		// unset the valueth bit, and update the size
		return size = VSIZE[this.bits &= ~bits];
	}

	/**
	 * Set this Values to the given value only, and adjust size to 1.
	 *
	 * @param value
	 */
	public void set(int value) {
		// set ONLY the valueth bit, and update the size (to 1)
		size = VSIZE[bits = VSHFT[value]];
	}

	/**
	 * Set this Values to the given bits, and adjust size accordingly.
	 *
	 * @param bits
	 */
	public void setBits(int bits) {
		size = VSIZE[this.bits=bits];
	}

	/**
	 * Add this $value to this Values Set.
	 *
	 * @param value to add.
	 * @return was the value actually added (or did it already exist)?
	 */
	public boolean add(int value) {
		// set the valueth bit, and update the size
		// nb: Faster to ALWAYS do it, even if it requires a new var.
		//     Logic is (almost always) a slower path.
		//     Perverse that if is the most used and the slowest operation.
		int pre = size;
		size = VSIZE[bits |= VSHFT[value]];
		return size > pre; // is size bigger?
	}

	/**
	 * Add these two $values to this Values Set.
	 *
	 * @param v0
	 * @param v1
	 * @return this Values.
	 */
	public Values add(int v0, int v1) {
		size = VSIZE[bits |= VSHFT[v0] | VSHFT[v1]];
		return this;
	}

	/**
	 * Add these $values to this Values Set.
	 *
	 * @param values {@code int[]} an arguments array of ints to add.
	 * @return this Values.
	 */
	public Values add(int[] values) {
		int cands = 0;
		for ( int i=0,n=values.length; i<n; ++i )
			cands |= VSHFT[values[i]];
		size = VSIZE[bits |= cands];
		return this;
	}

	/**
	 * Add these $bits to this Values Set.
	 *
	 * @param bits a Values.bits of the values to be added
	 * @return was any value actually added.
	 */
	boolean addBits(int bits) {
		int pre = size;
		return (size=VSIZE[this.bits |= bits]) > pre;
	}

	/**
	 * Set this Values Set to the digits in the given String.
	 * <p>String format is raw digits. EG: "1569"
	 *
	 * @param s digits to set.
	 */
	public void set(String s) {
		size = s.length();
		bits = 0;
		for ( int i=0; i<size; ++i )
			bits |= VSHFT[s.charAt(i)-'0'];
	}

	/**
	 * Add these $other Values to this Values Set.
	 *
	 * @param other
	 * @return this Values.
	 */
	public Values add(Values other) {
		size = VSIZE[bits |= other.bits];
		return this;
	}

	// ------------------------------- factories ------------------------------

	/**
	 * Create a new Values Set containing the intersection of this and $a.
	 * <p>ie: {@code new Values(this).and(a)}
	 * <p>ie: The values in both this and $a.
	 *
	 * @param a {@code Value}.
	 * @return a new Values.
	 */
	public Values intersect(Values a) {
		return new Values(bits & a.bits, false);
	}

	/**
	 * Create a new Values containing the intersection of this and $a and $b,
	 * ie values that are common to all three Values Sets.
	 * <p>ie: {@code new Values(this).and(a).and(b)}
	 * <p>ie: {@code return new Values(bits & a.bits & b.bits, false);}
	 * <p>ie: The values that are common to all of this and $a and $b.
	 *
	 * @param a {@code Value}.
	 * @param b {@code Value}.
	 * @return a new Values.
	 */
	public Values intersect(Values a, Values b) {
		return new Values(bits & a.bits & b.bits, false);
	}

	/**
	 * Create a new Values containing the intersection of this and $value.
	 * <p>ie: {@code new Values(this).and(v) // does not exist for int}
	 *
	 * @param value int
	 * @return a new Values.
	 */
	public Values intersect(int value) {
		return new Values(bits & VSHFT[value], false);
	}

	/**
	 * Create a new Values containing the intersection of this and $bitset.
	 *
	 * @param bitset the left-shifted-bitset of the values to intersect with.
	 * @return a new Values.
	 */
	public Values intersectBits(int bitset) {
		return new Values(bits & bitset, false);
	}

	/**
	 * Create a new Values Set containing this one minus the given value.
	 * <p>ie: {@code new Values(thisOne).clear(v)}
	 *
	 * @param value int to clear.
	 * @return a new Values.
	 */
	public Values minus(int value) {
		return new Values(bits & ~VSHFT[value], false);
	}

	/**
	 * Create a new Values Set containing the values that are in this Values
	 * Set but are not in Values $a.
	 * <p>ie: {@code new Values(this).andNot(a)}
	 *
	 * @param a {@code Values}
	 * @return a new Values.
	 */
	public Values minus(Values a) {
		return new Values(bits & ~a.bits, false);
	}

	/**
	 * Create a new Values Set containing the values that are in this Values
	 * Set, but are not in Values $a, clearing $value.
	 * <p>ie: {@code new Values(this).andNot(a).clear(value)}
	 *
	 * @param a {@code Values} to remove.
	 * @param value int to clear.
	 * @return a new Values.
	 */
	public Values minus(Values a, int value) {
		return new Values(bits & ~a.bits & ~VSHFT[value], false);
	}

	/**
	 * Create a new Values Set containing the values that in this Values Set,
	 * minus v1, minus v2.
	 * <p>ie: {@code new Values(this).clear(v1).clear(v2)}
	 *
	 * @param v0 to clear.
	 * @param v1 to clear.
	 * @return a new Values.
	 */
	public Values minus(int v0, int v1) {
		return new Values(bits & ~VSHFT[v0] & ~VSHFT[v1], false);
	}

	/**
	 * Create a new Values Set containing the values in this Values Set
	 * or in the given Values $a.
	 * <p>ie: {@code new Values(this).add(a)}
	 *
	 * @param a {@code Values} to add.
	 * @return a new Values.
	 */
	public Values plus(Values a) {
		return new Values(bits | a.bits, false);
	}

	/**
	 * Create a new Values containing the values in this Values Set
	 * or in the given Values $a or $b.
	 * <p>ie: {@code new Values(this).add(a).add(b)}
	 * <p>ie: {@code return new Values(bits | a.bits | b.bits, false);}
	 *
	 * @param a {@code Values} to add.
	 * @param b {@code Values} to add.
	 * @return a new Values.
	 */
	public Values plus(Values a, Values b) {
		return new Values(bits | a.bits | b.bits, false);
	}

	/**
	 * Create a new Values containing the values in this Values Set
	 * or in the given Values $a, minus v1, minus v2.
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

	/**
	 * Retain only those values which are in the keepers bitset, removing
	 * all the rest, and return the new size.
	 *
	 * @param keepers
	 */
	int retainAll(int keepers) {
		return size = VSIZE[bits &= keepers];
	}

	public void and(Values a) {
		size = VSIZE[bits & a.bits];
	}

	// -------------------------------- queries -------------------------------

	/**
	 * Is the given $value in this Values Set?
	 *
	 * @param value to query.
	 * @return boolean.
	 */
	public boolean contains(int value) {
		return (bits & VSHFT[value]) > 0; // 9bits (is NEVER negative)
	}

	/**
	 * Is the given $value <b>NOT</b> in this Values Set?
	 *
	 * @param value to query.
	 * @return boolean.
	 */
	public boolean no(int value) {
		return (bits & VSHFT[value]) < 1; // 9bits (is NEVER negative)
	}

	/**
	 * Does this Values Set contain all of these $other values?
	 *
	 * @param other {@Values} to look for.
	 * @return boolean.
	 */
	public boolean containsAll(Values other) {
		return other.bits>0 && (bits & other.bits)==other.bits;
	}

	/**
	 * Does this Values Set contain ONLY these $other Values?
	 *
	 * @param other
	 * @return boolean.
	 */
	public boolean isSubsetOf(Values other) {
		if ( other.bits == 0 )
			throw new UnsolvableException("other is empty");
		return this.bits!=0 && (this.bits & ~other.bits)==0;
	}

	/**
	 * @return bits == 0
	 */
	public boolean isEmpty() {
		return bits == 0; // true if there are NO bits set "on".
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ iterator ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// these methods can be combined as an alternative to an iterator.

	/**
	 * Returns my first value.
	 *
	 * @return first value in this Values, else NONE (-1).
	 */
	public int first() {
		return bits==0 ? NONE : VFIRST[bits];
	}

//not used 2020-11-23, but solid logic, so retain anyway
	/**
	 * @return The last (largest) value; else NONE (-1)
	 */
	public int last() {
		if ( bits == 0 )
			return NONE;
		// faster than Integer.numberOfLeadingZeros(bits);
		return (int)(Math.log(bits)/LOG2)+1;
	}

	/**
	 * Returns the next value greater-than-or-equal-to $v, else NONE (-1).
	 *
	 * @param v to look from <b>(INCLUSIVE)</b>.
	 * @return the next highest value.
	 */
	public int next(int v) {
		assert v > 0;
		final int b = bits & (ONES<<v-1);
		if ( b == 0 )
			return NONE;
		return VFIRST[b];
	}

//not used 2020-11-23, but there is solid logic for keeping it anyway
	/**
	 * Return the next value (1..9) <b>NOT</b> in this Set that is
	 * greater-than-or-equal-to $v; else NONE (-1).
	 *
	 * @param v 1..10: is presumed be a value that is in this set
	 * @return next missing value
	 */
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
	 *
	 * @param zValue the value you do not want, which MUST be in this set.
	 * @return the other value in this Set.
	 */
	public int otherThan(int zValue) {
//		assert Integer.bitCount(bits) == 2;
//		assert (bits & ~VSHFT[zValue]) > 0; // 9bits
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
	 * @return the number of values in the $array. This allows you to reuse
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
	 * Creates and returns a new int array of $size elements containing the
	 * values that are in this Values Set.
	 * <p>
	 * <b>WARN:</b> Use this method sparingly, never in a loop.
	 * Prefer {@link #toArray(int[])} in a loop.
	 *
	 * @return a new {@code int[]} array.
	 */
	public int[] toArrayNew() {
		int[] result = new int[size];
		toArray(result);
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ toString & friends ~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns a String representation of the given bits.
	 *
	 * @param bits Values.bits (aka maybes, cands, or just bits).
	 * @return 7 => 1, 2, 3
	 */
	public static String toString(final int bits) {
		return MAYBES_STR[bits];
	}

	/**
	 * Returns a String representation of these Values.
	 *
	 * @return 7 => 1, 2, 3
	 */
	@Override
	public String toString() {
		return MAYBES_STR[bits];
	}

//not used 2020-10-23 but it is really only for debugging anyway, so kept
	/**
	 * @return Integer.toBinaryString(bits) left padded with 0's to 9-chars
	 */
	public String toBinaryString() {
		String s = Integer.toBinaryString(bits);
		return ZEROS[s.length()] + s;
	}
	private static final String[] ZEROS = new String[]{
			  "000000000", "00000000", "0000000", "000000", "00000"
			, "0000", "000", "00", "0", ""
	};

	public static String and(Collection<Integer> c) {
		return Frmt.frmt(c, (i)->String.valueOf(i), CSP, AND);
	}

	/**
	 * Append the virtual Values(bits) to sb.
	 *
	 * @param sb to append to
	 * @param bits to append
	 * @param sep between values
	 * @param lastSep before last value
	 * @return the given sb with my s__t appended to it
	 */
	public static StringBuilder append(final StringBuilder sb, final int bits, final String sep, final String lastSep) {
		return bits==0 ? sb.append(MINUS) : Frmt.append(sb, VALUESES[bits], sep, lastSep);
	}

	/**
	 * A fancy (with field separators) toString for displaying Values in hints.
	 *
	 * @param bits
	 * @param sep
	 * @param lastSep
	 * @return
	 */
	public static String toString(int bits, String sep, String lastSep) {
		return append(SB(16), bits, sep, lastSep).toString();
	}

	public static String orString(int bits) { return toString(bits, CSP, OR); }
	public static String andString(int bits) { return toString(bits, CSP, AND); }
	public static String csv(int bits) { return toString(bits, CSP, CSP); }

	/**
	 * Debug: AlignedExclusion's array of values bits.
	 *
	 * @param candss array of cands
	 * @param numCandss number of candss
	 * @return CSV of candss
	 */
	public static StringBuilder toSB(final int[] candss, final int numCandss) {
		final StringBuilder sb = SB(16);
		for ( int i=0; i<numCandss; ++i ) {
			if(i>0) sb.append(COMMA);
			sb.append(MAYBES[candss[i]]);
		}
		return sb;
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

	/**
	 * WARN: bits is NOT an invariant! I do not intend to use a Values Set
	 * as a HashKey so I do not think it will be a problem. If you want to then
	 * I suggest you put the bit-value into an Integer, which is immutable, and
	 * use it as the hash-key instead.
	 *
	 * @return int A dodgy <b>volatile</b> hash-code.
	 */
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
			private int current = 0; // before first
			private int next = 0; // before first
			@Override
			public boolean hasNext(){
				for ( next=current+1; next<10; ++next ) // start at current+1
					if ( (VSHFT[next] & bits) > 0 ) // 9bits
						return true;
				return false;
			}
			@Override
			public Integer next(){
//KRC 2019-09-19 Making iterator faster but less safe. It will now ONLY work
// in "normal operation" where hasNext() is called before each next() because
// hasNext() is doing all the advancing, and next() just returns the results.
// I allways knew that iterating a bitset was a bad idea, so let us dodgy it up.
//				for ( cv=nv; cv<10; ++cv ) // start at nextValue
//					if ( (SHFT[cv] & bits) > 0 ) // normal operation tests ONCE
//						nv = cv + 1; // incase next() without hasNext();
//						return cv;
//				throw new NoSuchElementException("Values iterator past EOL");
				return current = next;
			}
			@Override
			public void remove(){
				if ( current == 0 )
					throw new IllegalStateException("remove before next");
				Values.this.remove(current);
			}
		};
	}

}
