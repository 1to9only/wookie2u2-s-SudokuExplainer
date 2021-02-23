/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import java.io.Serializable;
import java.util.Collection;
import java.util.regex.Pattern;


/**
 * An Idx (an index) wraps an {@code int[3]} each of 27 used bits for the 81
 * cells in the Grid.cells array. The position (right to left) of each set (1)
 * bit represents the Grid.cells index (ie Cell.i) of a cell that is present
 * in the set.
 * <p>
 * Bitsets do fast set-intersection with the bitwise-and-operator (&).
 * For example: To calculate cells common to set0 and set1 you could
 * (I recommend this code, where you need extreme speed):
 * <pre>{@code
 *     Idx i0 = set0.idx();
 *     Idx i1 = set1.idx();
 *     Idx intersection = new Idx(); // it'd be faster to re-use existing!
 *     intersection.a0 = i0.a0 & i1.a0;
 *     intersection.a1 = i0.a1 & i1.a1;
 *     intersection.a2 = i0.a2 & i1.a2;
 * }</pre>
 * which is bulls__tquick(TM) compared to the alternative using Sets addAll()
 * and retainAll() methods.
 * <p>
 * FYI: the equivalent method looks simpler, but requires invocation, which is
 * always a bit slow, so we use it "normally", when speed's not uber-critical:
 * <pre>{@code
 *     Idx i0 = set0.idx();
 *     Idx i1 = set1.idx();
 *     Idx intersection = Idx.newAnd(i0, i1);
 * }</pre>
 * <p>
 * So the Idx class exposes a number of methods that do bitset-index stuff,
 * both as "raw" {@code int[]} arrays and as instances of Idx, which basically
 * just encapsulates it's own {@code int[]} which it calls the static methods
 * on, in case you're currently towel-placement-challenged. The static int[]
 * methods tend to be more efficient, mainly because (as above) you can do the
 * calcs yourself locally, without having to wear the expense of invoking any
 * methods.
 * <p>
 * This class was originally written only as static helper methods for the
 * Aligned*Exclusion classes, but Idx is no longer used there, for speed.
 * Idx was later restored and converted to expose instances for use in the
 * XYWing class, where performance is much less critical. So Idx() has same
 * specifications as int[], but A*E doesn't use Idx's, it uses raw int[]
 * arrays instead, because speed is King; where-as XYWing holds instances of
 * Idx, which have internal int[] arrays, because speed is a mere prince.
 * <p>
 * Later I picked-up HoDoKu, which makes extensive use of Idx's (its SudokuSet)
 * so Idx's are now used in Aligned*Exclusion, Almost Locked Sets (als), the
 * various Fisherman, and pretty much everywhere because they are very useful.
 * Just pushing the cheese around on my cracker. Ye rolls yo dice and ye moves
 * yo mice. I apologise for any confusion.
 * <p>
 * <pre>
 * See: diuf.sudoku.solver.hinters.xyz.XYWing                  // uses Idx's
 * See: diuf.sudoku.solver.hinters.align.Aligned5Exclusion_1C  // uses int[]
 * See: diuf.sudoku.solver.hinters.align.LinkedMatrixCellSet.idx()
 * See: diuf.sudoku.Grid.cellsAt(int[] idx)
 * </pre>
 * <p>
 * KRC 2020-10-21 Idx.cells replaces Grid.at, localising all the tricky s__t.
 * I never liked Idx gizzards in my Grid, I was just too lazy to act. Simpler!
 * <p>
 * KRC 2020-10-22 IdxL extends Idx to provide a locking mechanism, but
 * <b>PLEASE NOTE</b> that IdxL <b>MUST</b> override <b>EVERY</b> mutator, or
 * it's basically useless!
 *
 * @author Keith Corlett 2019 DEC
 * KRC instance stuff 2020 Mar 06
 */
public class Idx implements Cloneable, Serializable, Comparable<Idx> {

	private static final long serialVersionUID = 342L;

	/** my array (a) has 3 elements. */
	public static final int ELEMENTS_PER_IDX = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_ELEMENT = 27;
	/** number of bits in two elements. */
	public static final int BITS_TWO_ELEMENTS = 2 * BITS_PER_ELEMENT;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int WORDS_PER_ELEMENT = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_WORD = 9;
	/** the value of a full "word" (9 set bits). */
	public static final int WORD_MASK = 0x1FF; // Hex F = Dec 15 = Bin 1111

	/** the value of ALL bits (ie 27 set bits) in an element. */
	public static final int ALL = (1<<BITS_PER_ELEMENT)-1;

	/** the left-shifted bitset value of my index. */
	public static final int[] SHFT = new int[BITS_PER_ELEMENT];

	/** ARRAYS[9-bit-word] contains the values that are packed into that
	 * 9-bit-word. There are 3 words per 27-bit element. */
	public static final int[][] ARRAYS = new int[1<<9][]; // [0..511][see SIZE]
	/** the number of elements in each ARRAYS sub-array. */
	public static int[] SIZE = new int[1<<9];

	/** Int ArrayS used to iterate Idx's. One array-set per iteration.
	 * There's two of them to avert collisions in imbedded loops. */
	public static final int IAS_LENGTH = 82; // 81+1 or a full Idx AIOBE's
	/** Integer ArrayS: Aligned*Exclusion use 9 arrays of 3 ints for Idxs. */
	public static final int[][] ias = new int[9][3];
	/** iasA arrays: my size is my indice. */
	public static final int[][] iasA = new int[IAS_LENGTH][];
	/** iasB arrays: my size is my indice. Two of them support two imbedded
	 * iterations; else there could be collisions (using the same array twice).
	 * Names of methods using iasB should always end in a B, like toArrayB();
	 * everything else should use iasA. */
	public static final int[][] iasB = new int[IAS_LENGTH][];

	/** BITS is each of the 9 set (1) bits. Simples. */
	public static final int[] BITS = new int[9];
	static {
		for ( int k=0; k<9; ++k )
			BITS[k] = 1<<k;
	}

	// used by below static block only
	private static int[] newArray(int bits) {
		final int n = Integer.bitCount(bits);
		int[] result = new int[n];
		int cnt = 0;
		for ( int i=0; cnt<n; ++i )
			if ( (bits & (1<<i)) != 0 )
				result[cnt++] = i;
		assert cnt == n; // even if they're both 0
		return result;
	}

	static {
		for ( int i=0; i<BITS_PER_ELEMENT; ++i )
			SHFT[i] = 1<<i;
		for ( int i=0; i<IAS_LENGTH; ++i ) {
			iasA[i] = new int[i];
			iasB[i] = new int[i];
		}
		for ( int i=0; i<ARRAYS.length; ++i ) {
			// nb: no need to roll my own when I can Indexes.toValuesArray it.
			// The downside is that Indexes.toValuesArray has to handle any i.
			ARRAYS[i] = newArray(i);
			SIZE[i] = ARRAYS[i].length;
		}
	}

	public static Idx of(Iterable<Cell> cells) {
		Idx idx = new Idx();
		for ( Cell c : cells )
			idx.add(c.i);
		return idx;
	}

	public static Idx of(Cell[] cells) {
		Idx idx = new Idx();
		for ( Cell c : cells )
			idx.add(c.i);
		return idx;
	}

	public static boolean sizeLTE(int a0, int a1, int a2, int max) {
		int size = 0;
		if ( a0!=0 && (size=Integer.bitCount(a0)) > max )
			return false;
		if ( a1!=0 && (size+=Integer.bitCount(a1)) > max )
			return false;
		// The if statement is redundant, but it's debuggable this way!
		if ( a2!=0 && (size+=Integer.bitCount(a2)) > max )
			return false;
		return true;
	}

	/**
	 * Return the cardinality of the "virtual Idx" a0, a1, a2.
	 * <p>
	 * NB: Uses Integer.bitCount, coz that's faster than initialising.
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return the number of set (1) bits in the Idx
	 */
	public static int size(int a0, int a1, int a2) {
		return (a0!=0 ? Integer.bitCount(a0) : 0)
			 + (a1!=0 ? Integer.bitCount(a1) : 0)
			 + (a2!=0 ? Integer.bitCount(a2) : 0);
	}

	/**
	 * Parse an Idx from CSV (comma separated values) of Cell.id's.
	 * <p>
	 * For example: "E3, H3, I3, H8, I8, E6, I6"
	 *
	 * @param csv comma separated values String of Cell.id's.
	 * @return a new Idx.
	 */
	public static Idx parse(String csv) {
		final Idx idx = new Idx();
		if ( csv==null || csv.isEmpty() )
			return idx;
		final Pattern pattern = Pattern.compile("[A-I][1-9]");
		for ( String id : csv.split(" *, *") ) // box 6, row 9, col A
			if ( pattern.matcher(id).matches() ) // ignore crap, just in case
				//        col               +  row
				idx.add( (id.charAt(0)-'A') + ((id.charAt(1)-'1')*9) );
		return idx;
	}

	// ============== HdkIdxTest methods (for use in ALS package) =============

	public static boolean andEmpty(Idx s1, Idx s2) {
		return (s1.a0&s2.a0) == 0
			&& (s1.a1&s2.a1) == 0
			&& (s1.a2&s2.a2) == 0;
	}

	public static boolean andEqualsS2(Idx s1, Idx s2) {
		return (s1.a0&s2.a0) == s2.a0
			&& (s1.a1&s2.a1) == s2.a1
			&& (s1.a2&s2.a2) == s2.a2;
	}
	public static boolean andEqualsEither(Idx s1, Idx s2) {
		return andEqualsS2(s1, s2) || andEqualsS2(s2, s1);
	}

	public static boolean orEqualsS2(Idx s1, Idx s2) {
		return (s1.a0|s2.a0) == s2.a0
			&& (s1.a1|s2.a1) == s2.a1
			&& (s1.a2|s2.a2) == s2.a2;
	}
	public static boolean orEqualsEither(Idx s1, Idx s2) {
		return orEqualsS2(s1, s2) || orEqualsS2(s2, s1);
	}

	public static Idx newAnd(Idx aa, Idx bb) {
		return new Idx(aa.a0&bb.a0, aa.a1&bb.a1, aa.a2&bb.a2);
	}

	public static Idx newOr(Idx aa, Idx bb) {
		return new Idx(aa.a0|bb.a0, aa.a1|bb.a1, aa.a2|bb.a2);
	}

	// ============================ instance stuff ============================

	/** The 3 ints, each of 27 used bits, for the 81 cells in a Grid. These
	 * fields are readonly to the public; don't modify unless you have to. */
	public int a0, a1, a2;

	/**
	 * Constructs a new empty Idx.
	 * <p>
	 * Design Note: There are no fields to initialise, so construction is fast
	 * compared to the previous 'a' array set-up. But we no longer needlessly
	 * construct an Idx, anywhere in the code, so there's little or no gain
	 * from this "upgrade", just more code handling a0, a1, and a2, which used
	 * to be the array 'a'. Sigh.
	 */
	public Idx() {
	}

	/**
	 * The Copy Constructor: Constructs a new Idx containing a copy of 'src's
	 * contents.
	 * @param src to copy
	 */
	public Idx(Idx src) {
		a0 = src.a0;
		a1 = src.a1;
		a2 = src.a2;
	}

	/**
	 * Constructs a new Idx with the given 'values' as it's internal array.
	 * <p>
	 * This method is currently only used by IO.loadIdxs, where it's exactly
	 * what is required.
	 * @param values to retain as my internal array.
	 */
	public Idx(int[] values) {
		a0 = values[0];
		a1 = values[1];
		a2 = values[2];
	}

	/**
	 * Constructs a new Idx and if isFull then fills it with all of the cells
	 * in a Grid (0..80) by calling the fill() method internally.
	 * <p>
	 * We currently only ever call this with true, but isFull might be handy
	 * down the track, so here it is.
	 *
	 * @param isFull true to fill this idx, false leaves it empty.
	 */
	public Idx(boolean isFull) {
		if ( isFull )
			a0=a1=a2=ALL; // 27 1's
	}

	/**
	 * Constructor from raw array values.
	 * @param a0
	 * @param a1
	 * @param a2
	 */
	public Idx(int a0, int a1, int a2) {
		this.a0 = a0;
		this.a1 = a1;
		this.a2 = a2;
	}

	@Override
	public Idx clone() throws CloneNotSupportedException {
		Idx clone = (Idx)super.clone();
		clone.a0 = a0;
		clone.a1 = a1;
		clone.a2 = a2;
		return clone;
	}

	// ------------------------------- mutators -------------------------------

	/**
	 * Set this = src
	 * @param src
	 * @return this Idx, for method chaining.
	 */
	public Idx set(Idx src) {
		// null check required by KrakenFisherman. sigh.
		if ( src == null ) {
			a0 = a1 = a2 = 0; // clear
		} else {
			a0 = src.a0;
			a1 = src.a1;
			a2 = src.a2;
		}
		return this;
	}

	/**
	 * Set this Idx to the raw array entries a0, a1, and a2.
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return this Idx, for method chaining.
	 */
	public Idx set(int a0, int a1, int a2) {
		this.a0 = a0;
		this.a1 = a1;
		this.a2 = a2;
		return this;
	}

	/**
	 * Set this = the cells in the given regions
	 * @param regions whose cell indices you want in this Idx.
	 * @return this Idx, for method chaining.
	 */
	public Idx set(ARegion[] regions) {
		a0 = a1 = a2 = 0; // clear
		for ( ARegion region : regions ) {
			// add cells in region to this Idx
			a0 |= region.idx.a0;
			a1 |= region.idx.a1;
			a2 |= region.idx.a2;
		}
		return this;
	}

	/**
	 * Set this = src and return any?
	 * @param src
	 * @return
	 */
	public boolean setAny(Idx src) {
		a0 = src.a0;
		a1 = src.a1;
		a2 = src.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa & bb)
	 * @param aa
	 * @param bb
	 * @return this Idx, for method chaining.
	 */
	public Idx setAnd(Idx aa, Idx bb) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return this;
	}

	/**
	 * Does setAnd and returns any
	 * @param aa
	 * @param bb
	 * @return
	 */
	public boolean setAndAny(Idx aa, Idx bb) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa | bb).
	 * @param aa
	 * @param bb
	 * @return this Idx, for method chaining.
	 */
	public Idx setOr(Idx aa, Idx bb) {
		a0 = aa.a0 | bb.a0;
		a1 = aa.a1 | bb.a1;
		a2 = aa.a2 | bb.a2;
		return this;
	}

	/**
	 * Set this = aa & ~bb.
	 * @param aa
	 * @param bb
	 * @return this Idx, for method chaining.
	 */
	public Idx setAndNot(Idx aa, Idx bb) {
		a0 = (aa.a0 & ~bb.a0) & ALL;
		a1 = (aa.a1 & ~bb.a1) & ALL;
		a2 = (aa.a2 & ~bb.a2) & ALL;
		return this;
	}

	/**
	 * Set this = !this; ie Negates (flips) indices in
	 * this Idx, so that everything in this Idx is now not in this Idx,
	 * and everything that is not in this Idx is now in this Idx.
	 */
	public void not() {
		a0 = ALL & ~a0;
		a1 = ALL & ~a1;
		a2 = ALL & ~a2;
	}

	/**
	 * Clears this Idx, ie removes all indices.
	 *
	 * @return this Idx, for method chaining.
	 */
	public Idx clear() {
		a0 = a1 = a2 = 0;
		return this;
	}

	/**
	 * Fills this Idx, ie adds all indices.
	 *
	 * @return this Idx, for method chaining.
	 */
	public Idx fill() {
		a0 = a1 = a2 = ALL; // 27 1's
		return this;
	}

	/**
	 * Add the given indice 'i' to this Idx.
	 * <p>
	 * Only used in test cases (and now Permutations).
	 *
	 * @param i
	 */
	public void add(int i) {
		assert i>=0 && i<81;
		if ( i < BITS_PER_ELEMENT )
			a0 |= SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 |= SHFT[i%BITS_PER_ELEMENT];
		else
			a2 |= SHFT[i%BITS_PER_ELEMENT];
	}

	/**
	 * Add the indice of each of the given cells to this Idx.
	 * <p>
	 * You can override this one!
	 *
	 * @param cells
	 * @return this Idx, for method chaining.
	 */
	public Idx addAll(Cell[] cells) {
		for ( Cell c : cells )
			add(c.i);
		return this;
	}

	/**
	 * Remove the given indice 'i' from this Idx.
	 *
	 * @param i the indice to be removed from this Idx
	 */
	public void remove(int i) {
		if ( i < BITS_PER_ELEMENT )
			a0 &= ~SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 &= ~SHFT[i%BITS_PER_ELEMENT];
		else
			a2 &= ~SHFT[i%BITS_PER_ELEMENT];
	}

	/**
	 * Remove all of the given cells indices from this Set.
	 *
	 * @param cells
	 */
	public void removeAll(Iterable<Cell> cells) {
		for ( Cell c : cells )
			// manually remove(c), to not checkLock repeatedly
			remove(c.i);
	}

	/**
	 * Set this = (this & other)
	 *
	 * @param other
	 * @return this Idx, for method chaining.
	 */
	public Idx and(Idx other) {
		a0 &= other.a0;
		a1 &= other.a1;
		a2 &= other.a2;
		return this;
	}

	/**
	 * Set this = (this & ~other
	 *
	 * @param other
	 * @return this Idx, for method chaining.
	 */
	public Idx andNot(Idx other) {
		a0 = (a0 & ~other.a0) & ALL;
		a1 = (a1 & ~other.a1) & ALL;
		a2 = (a2 & ~other.a2) & ALL;
		return this;
	}

	/**
	 * Set this = (this | other)
	 *
	 * @param other
	 * @return this Idx, for method chaining.
	 */
	public Idx or(Idx other) {
		a0 |= other.a0;
		a1 |= other.a1;
		a2 |= other.a2;
		return this;
	}

	// ------------------------ this mutator is a query -----------------------

	/**
	 * Remove and return the first (lowest) indice in this Set, without having
	 * to initialise() this Set.
	 * <p>
	 * The poll() method is my invention, and I'm quite proud of it.
	 * It's intent is to be faster than while size()>0 i=get(0) remove(i).
	 * <p>
	 * PERFORMANCE: numberOfTrailingZeros is a heavy O(1) operation called for
	 * each element, so will probably be outrun as the population (N) increases
	 * by ONE SudokuSet initialise() followed by N array look-ups. I have NOT
	 * tested this. All I can tell you is that my version of Coloring is faster
	 * than the original, I cannot tell you why, only speculate intelligently
	 * because several changes were made all-in-one coz (IMHO) it either works
	 * my way or the original way; ie intermediate changes are beyond me, so I
	 * haven't isolated the gains. I'm still proud of it.
	 * <p>
	 * See: {@link diuf.sudoku.solver.hinters.color.Coloring#conjugate}
	 *
	 * @return the first (lowest) indice which has been removed from this Set,
	 * else (this Set is empty) return -1.
	 */
	public int poll() {
		int x;
		if ( a0 != 0 ) {
			x = a0 & -a0; // lowestOneBit(a0);
			a0 &= ~x; // remove x from a0
			return Integer.numberOfTrailingZeros(x);
		}
		if ( a1 != 0 ) {
			x = a1 & -a1; // lowestOneBit(a1);
			a1 &= ~x; // remove x from a1
			// 27 is Idx.BITS_PER_ELEMENT
			return 27 + Integer.numberOfTrailingZeros(x);
		}
		if ( a2 != 0 ) {
			x = a2 & -a2; // lowestOneBit(a2);
			a2 &= ~x; // remove x from a2
			// 54 is 2 * Idx.BITS_PER_ELEMENT
			return 54 + Integer.numberOfTrailingZeros(x);
		}
		return -1; // this set is empty
	}

	// -------------------------------- queries -------------------------------

	/**
	 * HoDoKuAdapter: The get method returns the i'th indice from this Idx.
	 * <p>
	 * <b>WARNING</b>: get simulates HoDoKu's SudokuSet.get method but it's
	 * really slow, so prefer: {@link #forEach2}, {@link #toArrayA()},
	 * {@link toArrayB()}, or even {@link #toArrayNew()}. <b>DO NOT</b> use
	 * this piece of crap unless you have to.
	 *
	 * @param i the index of the indice you want in this Idx as an array, so
	 * just get the bloody array yourself ya putz!, and look-it-up yourself,
	 * I cannot and do not cache the array, I recalculate the bastard every
	 * time coz that'd be a less slow way of implementing the slow way! Because
	 * EVERY method invocation comes with an impost; so calling a method for
	 * each indice is poor form, let alone two methods per indice, as per
	 * hobiwans original. If HoDoKu wasn't so bloody ingenious I'd lock hobiwan
	 * up for crimes against performance. He's absolutely first class on all da
	 * big-stuff, but piss-poor on all da small-stuff, which soon adds-up to
	 * mostly mitigate all his ingenuity gains; so I'm attempting to do both:
	 * Big Sudoku Dynamite implemented with attention to detail. Fast, Dry, and
	 * simple. "Tell him he's dreaming."
	 * @return {@code toArrayB()[i]} you have been warned!
	 */
	public int get(int i) {
		assert butImaMoronSoIReallyWantToUseSizeAndGet : "No bananas!";
		return toArrayB()[i];
	}
	public static boolean butImaMoronSoIReallyWantToUseSizeAndGet = false;

	/**
	 * The peek method returns the first (lowest) indice in this Idx (without
	 * removing it) without having to initialise an array.
	 * @return the value of the first (lowest) indice in this Idx
	 */
	public int peek() {
		if ( a0 != 0 )
			return Integer.numberOfTrailingZeros(a0);
		if ( a1 != 0 )
			return BITS_PER_ELEMENT + Integer.numberOfTrailingZeros(a1);
		if ( a2 != 0 )
			return BITS_TWO_ELEMENTS + Integer.numberOfTrailingZeros(a2);
		return -1;
	}

	public boolean sizeLTE(int max) {
		return sizeLTE(a0, a1, a2, max);
	}

	/**
	 * Return is this Idx empty?
	 *
	 * @return true if this Idx contains no indices.
	 */
	public boolean isEmpty() {
		return (a0|a1|a2) == 0;
	}

	/**
	 * Return is this Idx NOT empty?
	 *
	 * @return true if this Idx contains any indices.
	 */
	public boolean any() {
		return (a0|a1|a2) != 0;
	}

	/**
	 * Returns !(this & other).isEmpty()
	 *
	 * @param other
	 * @return true if the intersection of this and other is not empty.
	 */
	public boolean andAny(Idx other) {
		return (a0 & other.a0) != 0
			|| (a1 & other.a1) != 0
			|| (a2 & other.a2) != 0;
	}

	/**
	 * Returns (this & other) == other.
	 * <p>
	 * <b>WARNING:</b> this operation is NOT transitive (unlike most logical
	 * operations), that is:<br>
	 * {@code    ((this & other) == other)  !=  ((other & this) == this)}<br>
	 * So make sure you're invoking andEquals on the correct Idx!
	 *
	 * @param other
	 * @return true if the intersection of this an other equals other,
	 * ie Is this a superset of other?
	 */
	public boolean andEqualsOther(Idx other) {
		return (a0 & other.a0) == other.a0
			&& (a1 & other.a1) == other.a1
			&& (a2 & other.a2) == other.a2;
	}

	/**
	 * Return the number of indices in this Idx.
	 *
	 * @return the sum of the Integer.bitCount of my three array elements.
	 */
	public int size() {
		return size(a0, a1, a2);
	}

	/**
	 * Does this Idx contain the given indice 'i'?
	 *
	 * @param i
	 * @return true if this Idx contains the given indice.
	 */
	public boolean contains(int i) {
		if ( i < BITS_PER_ELEMENT )
			return (a0 & SHFT[i]) != 0;
		else if ( i < BITS_TWO_ELEMENTS )
			return (a1 & SHFT[i%BITS_PER_ELEMENT]) != 0;
		else
			return (a2 & SHFT[i%BITS_TWO_ELEMENTS]) != 0;

	}

	/**
	 * When this Idx contains two indices, I return the one other than 'i'.
	 * <p>
	 * I don't mutate this Idx (remove then peek would mutate this Idx).
	 * <p>
	 * When this Idx does NOT contain two indices -> first other than i
	 * <p>
	 * When 'i' is not one of my indices -> first
	 * <p>
	 * This method is only called on an Idx containing two indices.
	 *
	 * @param i
	 * @return the other indice in this Idx
	 */
	public int otherThan(int i) {
		assert i>=0 && i<81;
		// mutate a local copy of this Idx's contents
		int a0=this.a0, a1=this.a1, a2=this.a2;
		// "remove" i
		if ( i < BITS_PER_ELEMENT )
			a0 &= ~SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 &= ~SHFT[i%BITS_PER_ELEMENT];
		else
			a2 &= ~SHFT[i%BITS_PER_ELEMENT];
		// peek
		if ( a0 != 0 )
			// the max 1<<26 is too large to cache so just calculate them
			return Integer.numberOfTrailingZeros(a0);
		if ( a1 != 0 )
			return BITS_PER_ELEMENT + Integer.numberOfTrailingZeros(a1);
		if ( a2 != 0 )
			return BITS_TWO_ELEMENTS + Integer.numberOfTrailingZeros(a2);
		return -1;
	}

	// ----------------------------- bulk queries -----------------------------

	/**
	 * Returns the given array (which is presumed to be of sufficient size)
	 * populated with the indices in this Idx.
	 * <p>
	 * Note that the array is NOT cleared first. It's presumed new or empty;
	 * or you want to add to the existing contents.
	 *
	 * @param array
	 * @return the given array, for method chaining.
	 */
	public int[] toArray(int[] array) {
		// from little things...
		forEach2((c, i) -> array[c] = i);
		return array;
	}

	/**
	 * Returns a cached array containing the indices in this Idx.
	 * <p>
	 * <b>WARN:</b> The array is cached, so if you call toArray twice in
	 * two imbedded loops the inner call may (if the idx sizes are equal)
	 * overwrite the array which underlies the outer iteration, so there
	 * are two toArrays: toArrayA and toArrayB.
	 * @return a cached array of the indices in this Idx.
	 */
	public int[] toArrayA() {
		if ( isEmpty() )
			return iasA[0];
		return toArray(iasA[size()]);
	}

	/**
	 * Returns a cached array containing the indices in this Idx.
	 * <p>
	 * <b>WARN:</b> The array is cached, so if you call toArray twice in
	 * two imbedded loops the inner call may (if the idx sizes are equal)
	 * overwrite the array which underlies the outer iteration, so there
	 * are two toArrays: toArrayA and toArrayB.
	 * @return a cached array of the indices in this Idx.
	 */
	public int[] toArrayB() {
		if ( isEmpty() )
			return iasB[0];
		return toArray(iasB[size()]);
	}

//not used, but retain: there's a strong logical case for having one.
//	/**
//	 * Returns a new array containing the indice in this Idx.
//	 * <p>
//	 * Prefer toArrayA or toArrayB in loops: if you call me in a loop then you
//	 * are creating (potentially many) garbage arrays to keep the GC busy.
//	 * @return
//	 */
//	public int[] toArrayNew() {
//		// there is no need for a new 0-length array coz it's immutable.
//		return isEmpty() ? iasA[0] : toArray(new int[size()]);
//	}

	// ------------------------ Visitor2: count, indice -----------------------
	//
	//                Welcome to the alien visitation centre.
	//                             Bring Towel!
	//

	/**
	 * You implement Visitor2 (typically with a lambda expression) in order to
	 * call my forEach method to invoke {@code visitor.visit(count, indice)}
	 * for each indice in the Idx. The count is the (0 based) number of indices
	 * we've visited so far.
	 * <p>
	 * I like lambda expressions, but only simple ones. They're very clever!
	 */
	public interface Visitor2 {
		/**
		 * visit this indice, which is the count'th (0 based) indice so far.
		 *
		 * @param count is the number of indices we've visited so far minus 1
		 * (to make it 0 based like an array indice) which you'll need to add
		 * this indice to an array; which is exactly what toArray does. The
		 * append and toString methods use count to work-out if this is the
		 * first indice, which doesn't want a space before it. You could also
		 * just for example, presuming you know my size, work-out if this is
		 * the last indice, to append an " and " instead of a ", "
		 * @param indice is the distance from the right (in a standard LSR
		 * (least significant right) representation of a binary number) of this
		 * set (1) bit in this Idx; ie just like a normal number, except
		 * different, obviously, coz it's in binary, not decimal. Sigh
		 */
		void visit(int count, int indice);
	}

	/**
	 * Static forEach2 actually invokes {@code v.visit(count, indice)} for each
	 * indice in the given Idx(a1,a2,a3). The count is (0 based) number of
	 * indices we've visited so far.
	 * <p>
	 * I'm a little bit proud of these forEach methods. They save implementing
	 * the same tricky bloody code many times just to do different things with
	 * the indices. It's really quite clever. As is the lambda plumbing which
	 * allows us to implement Visitor1/2 succinctly. It's all fairly clever.
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @param v is invoked with each count (number of indices so far) and
	 * the indice, for each set (1) bit in this Idx.
	 * @return the number of indices visited, ie the end count.
	 */
	public static int forEach2(int a0, int a1, int a2, Visitor2 v) {
		int j;
		int count = 0;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a0>>j)&WORD_MASK] )
					v.visit(count++, j+k);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a1>>j)&WORD_MASK] )
					v.visit(count++, BITS_PER_ELEMENT+j+k);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a2>>j)&WORD_MASK] )
					v.visit(count++, BITS_TWO_ELEMENTS+j+k);
		return count;
	}

	/**
	 * forEach invokes {@code v.visit(count, indice)} for each indice in this
	 * Idx. The count is the (0 based) number of indices we've visited so far.
	 * <p>
	 * I just delegate to the static forEach, passing this Idx.
	 *
	 * @param visitor is invoked passing the count (0 based number of indices
	 *  we've visited so far) and each indice in this Idx
	 * @return total number of indices visited, ie the size of this Idx
	 */
	public int forEach2(Visitor2 visitor) {
		return forEach2(a0, a1, a2, visitor);
	}

	// ------------------------- Visitor1: indice only ------------------------
	//
	//              Welcome back to the alien visitation centre.
	//                     Wear's your ____ing Towel!?!?
	//

	/**
	 * You implement Visitor1 (typically with a lambda expression) in order to
	 * call the forEach1 method, which invokes {@code v.visit(indice)} for
	 * each indice in this Idx.
	 */
	public interface Visitor1 {
		/**
		 * You implement Visitor1 to override visit to do whatever you like
		 * with the indice parameter.
		 *
		 * @param indice an indice (a set (1) bit) in this Idx
		 */
		void visit(int indice);
	}

	/**
	 * Static forEach1 visits each indice in a1,a2,a3: ie invokes
	 * {@code v.visit(indice)} for each set (1) bit in the Idx(a1,a2,a3).
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @param v your implementation of Visitor1 does whatever with indice
	 * @return the number of indices visited, ie the size of this Idx
	 */
	public static int forEach1(int a0, int a1, int a2, Visitor1 v) {
		int j;
		int count = 0;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a0>>j)&WORD_MASK] )
					v.visit(j+k);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a1>>j)&WORD_MASK] )
					v.visit(BITS_PER_ELEMENT+j+k);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a2>>j)&WORD_MASK] )
					v.visit(BITS_TWO_ELEMENTS+j+k);
		return count;
	}
	/**
	 * forEach1 visits each indice, ie invokes {@code v.visit(indice)} for each
	 * set (1) bit in this Idx.
	 * @param v your implementation of Visitor1 does whatever with indice
	 * @return the number of indices visited, ie the size of this Idx
	 */
	public int forEach1(Visitor1 v) {
		return forEach1(a0, a1, a2, v);
	}

	// ---------------------------- BooleanVisitor ----------------------------
	//                   Na, that's just a grey with a perm!
	//                    But I do like her cat skin cloak.

	/**
	 * Visit each indice, stopping as soon as visit returns false.
	 */
	public static interface UntilFalseVisitor {
		boolean visit(int indice);
	}

	/**
	 * Visit each indice, stopping as soon as visit returns false.
	 * @param v an implementation of UntilFalseVisitor, usually a lambda.
	 * @return true if we made it through all indices without returning false,
	 * so note that calling me on an empty Idx always returns true. Not clever.
	 */
	public boolean untilFalse(UntilFalseVisitor v) {
		int j;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a0>>j)&WORD_MASK] )
					if ( !v.visit(j+k) )
						return false;
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a1>>j)&WORD_MASK] )
					if ( !v.visit(BITS_PER_ELEMENT+j+k) )
						return false;
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : ARRAYS[(a2>>j)&WORD_MASK] )
					if ( !v.visit(BITS_TWO_ELEMENTS+j+k) )
						return false;
		return true;
	}

	// --------------------------------- cells --------------------------------

	/**
	 * Get the cells from the given Grid at indices in this Idx.
	 * Use this one with the right sized array, which is returned.
	 *
	 * @param grid the grid to read cells from
	 * @param cells the array to populate with cells in this Idx.
	 * @return the given cells array, so create new if you must.
	 */
	public Cell[] cells(Grid grid, Cell[] cells) {
		forEach2((count, indice) -> cells[count] = grid.cells[indice]);
		return cells;
	}
	
	// cellsN is hammered by Aligned*Exclusion, so create ONE instance of
	// CellsNVisitor, just to be more memory efficient, that's all.
	private static final class CellsNVisitor implements Visitor2 {
		public Grid grid;
		public Cell[] cells;
		@Override
		public void visit(int count, int indice) {
			cells[count] = grid.cells[indice];
		}
	}
	private static final CellsNVisitor CELLS_N_VISITOR = new CellsNVisitor();
	/**
	 * Get the cells from the given Grid at indices in this Idx. Use this one
	 * with a fixed-sized array, and I return the size.
	 *
	 * @param grid the grid to read cells from
	 * @param cells the array to populate with cells in this Idx.
	 * @return the number of indices visited, ie the size of this Idx
	 */
	public int cellsN(Grid grid, Cell[] cells) {
		CELLS_N_VISITOR.grid = grid;
		CELLS_N_VISITOR.cells = cells;
		return forEach2(CELLS_N_VISITOR);
	}

	// cellsMaybes is hammered by AlignedExclusion, so create ONE instance of
	// CellsMaybesVisitor, just to be more memory efficient, that's all.
	public static final class CellsMaybesVisitor implements Visitor2 {
		public Grid grid;
		public int[] maybes;
		@Override
		public void visit(int count, int indice) {
			maybes[count] = grid.cells[indice].maybes.bits;
		}
	}
	public static final CellsMaybesVisitor CELLS_MAYBES_VISITOR = new CellsMaybesVisitor();
	public int cellsMaybes() {
		return forEach2(CELLS_MAYBES_VISITOR);
	}

	/**
	 * Get the cells from the given Grid at indices in this Idx.
	 * This one returns a Grid.cas array for speed and convenience, so clone()
	 * it to keep it (it's use or loose it Baby).
	 *
	 * @param grid the grid to read cells from
	 * @return the {@code Cell[]} from the Grid.cas (cache) so clone to keep.
	 */
	public Cell[] cells(Grid grid) {
		return cells(grid, Grid.cas(size()));
	}

	/**
	 * This one returns the re-populated collection.
	 * @param grid
	 * @param cells
	 * @return
	 */
	public int cells(Grid grid, Collection<Cell> cells) {
		cells.clear();
		return forEach1((i) -> cells.add(grid.cells[i]));
	}

	// ----------------------------- stringy stuff ----------------------------

	/**
	 * Append this Idx to the given StringBuilder and return it.
	 * <p>
	 * This is all just to avoid creating thousands of intermediate Strings,
	 * because that's nuts (ie less efficient) and I can.
	 *
	 * @param sb to append me to.
	 * @return the given StringBuilder for method chaining.
	 */
	public StringBuilder append(StringBuilder sb) {
		return append(sb, size(a0, a1, a2)*3, a0, a1, a2);
	}

	/**
	 * Append space separated cell-IDs (SSV) of the given Idx(a0, a1, a2) to
	 * the given StringBuilder. First ensureCapacity of length + the given 'n'.
	 * <p>
	 * Each bitset uses 27 least significant (rightmost) bits, for 81 cells.
	 *
	 * @param sb to append to
	 * @param n use size(a0, a1, a2) or just guess, it's up to you.
	 * @param a0 the first bitset of the Idx
	 * @param a1 the second bitset of the Idx
	 * @param a2 the third bitset of the Idx
	 * @return the given StringBuilder for method chaining.
	 */
	public static StringBuilder append(StringBuilder sb, int n, int a0, int a1, int a2) {
		sb.ensureCapacity(sb.length()+n);
		// nb: we're using a lambda expression to implement Visitor2 here
		forEach2(a0, a1, a2, (cnt, g) -> {
			if(cnt>0) sb.append(' ');
			sb.append(Grid.CELL_IDS[g]);
		});
		return sb;
	}

	/**
	 * Returns a cell-id's String of a0, a1, a2; as if they where an Idx array.
	 * <p>
	 * NOTE: toString is no longer used in hint-HTML, so performance doesn't
	 * really matter any longer, so we COULD use a forEach and a Visitor.
	 *
	 * @param a0 the first mask
	 * @param a1 the second mask
	 * @param a2 the third mask
	 * @return the cell-id's of indices in this Idx.
	 */
	public static String toCellIdString(int a0, int a1, int a2) {
		int n = size(a0,a1,a2) * 3;
		return append(getSB(n), n, a0, a1, a2).toString();
	}

	// for debugging
	public static String toIndiceString(int a0, int a1, int a2) {
		StringBuilder sb = getSB(size(a0,a1,a2) * 3);
		forEach2(a0, a1, a2, (cnt, i) -> {
			if (cnt>0) sb.append(' ');
			sb.append(i); // the raw indice
		});
		return sb.toString();
	}

	@Override
	public String toString() {
		return true // @check true
			// cell-ids toString for normal use
			? toCellIdString(a0, a1, a2)
			// debug only: the raw indices
			: toIndiceString(a0, a1, a2);
	}

	public String toFullString(Grid grid) {
		StringBuilder sb = getSB(size()*12);
		forEach2((cnt, i) -> {
			if (cnt>0) sb.append(' ');
			sb.append(grid.cells[i].toFullString());
		});
		return sb.toString();
	}

	private static StringBuilder getSB(int capacity) {
		if ( SB == null )
			return SB = new StringBuilder(capacity);
		SB.setLength(0);
		if ( capacity > SB.capacity() )
			SB.ensureCapacity(capacity);
		return SB;
	}
	private static StringBuilder SB;

	// ------------------------- HoDoKuAdapter for als ------------------------

	// ---- mutators ----

	public Idx setAllExcept(Idx s2) {
		a0 = ALL & ~s2.a0;
		a1 = ALL & ~s2.a1;
		a2 = ALL & ~s2.a2;
		return this;
	}

	public void orNot(Idx s2) {
		a0 = (a0 | ~s2.a0) & ALL;
		a1 = (a1 | ~s2.a1) & ALL;
		a2 = (a2 | ~s2.a2) & ALL;
	}

	public void orAnd(Idx s1, Idx s2) {
		a0 |= (s1.a0 & s2.a0);
		a1 |= (s1.a1 & s2.a1);
		a2 |= (s1.a2 & s2.a2);
	}

	public void orAnd(Idx s1, Idx s2, Idx s3) {
		a0 |= (s1.a0 & s2.a0 & s3.a0);
		a1 |= (s1.a1 & s2.a1 & s3.a1);
		a2 |= (s1.a2 & s2.a2 & s3.a2);
	}

	// ---- queries ----

	public boolean none() {
		return (a0|a1|a2) == 0;
	}

//not used
//	public boolean andEmpty(Idx s2) {
//		return (a0&s2.a0) == 0
//			&& (a1&s2.a1) == 0
//			&& (a2&s2.a2) == 0;
//	}

	public boolean andEqualsThis(Idx s2) {
		return (a0&s2.a0) == a0
			&& (a1&s2.a1) == a1
			&& (a2&s2.a2) == a2;
	}

	// ---- plumbing ----

	@Override
	public int compareTo(Idx s2) {
		int diff;
		if ( (diff=a2 - s2.a2) == 0
		  && (diff=a1 - s2.a1) == 0 )
			diff = a0 - s2.a0;
		return diff < 0 ? -1
			 : diff > 0 ? 1
			 : 0;
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Hash this Idx: fold each int in half and offset them a tad.
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return a very hashy hashCode for this Idx
	 */
	public static int hashCode(int a0, int a1, int a2) {
		return ((a2^(a2>>>16))<<5)
			 ^ ((a1^(a1>>>16))<<3)
			 ^ (a0^(a0>>>16));
	}

	@Override
	public int hashCode() {
		if ( hc == 0 )
			hc = hashCode(a0, a1, a2);
		return hc;
	}
	private int hc;

	public boolean equals(int m0, int m1, int m2) {
		return a0==m0 && a1==m1 && a2==m2;
	}

	@Override
	public boolean equals(Object o) {
		return o==this || (o!=null && (o instanceof Idx) && equals((Idx)o));
	}

	public boolean equals(Idx other) {
		return a0 == other.a0
			&& a1 == other.a1
			&& a2 == other.a2;
	}

}
