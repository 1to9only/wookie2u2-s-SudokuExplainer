/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.CellFilter;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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
 */
public class Idx implements Cloneable, Serializable, Comparable<Idx> {

	private static final long serialVersionUID = 342L;

//	/** my array (a) has 3 elements. */
//	public static final int ELEMENTS_PER_IDX = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_ELEMENT = 27;
	/** number of bits in two elements. */
	public static final int BITS_TWO_ELEMENTS = 2 * BITS_PER_ELEMENT;
//	/** we use 3 * 9 bit "words" = 27 bits in each element. */
//	public static final int WORDS_PER_ELEMENT = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_WORD = 9;
	/** the value of a full "word" (9 set bits). */
	public static final int WORD_MASK = 0x1FF; // Hex F = Dec 15 = Bin 1111

	/** the value of ALL bits (ie 27 set bits) in an Idx element. */
	public static final int ALL = (1<<BITS_PER_ELEMENT)-1;

	/** the left-shifted bitset value of my index. */
	public static final int[] SHFT = new int[BITS_PER_ELEMENT];

	/** WORDS[9-bit-word] contains the values that are packed into that
	 * 9-bit-word. There are 3 words per 27-bit element. */
	public static final int[][] WORDS = new int[1<<9][]; // [0..511][see SIZE]

	/** Int ArrayS used to iterate Idx's. One array-set per iteration.
	 * There's two of them to avert collisions in imbedded loops. */
	public static final int IAS_LENGTH = 82; // 81+1 or a full Idx AIOBE's
	/** iasA arrays: my size is my indice. */
	public static final int[][] IAS_A = new int[IAS_LENGTH][];
	/** iasB arrays: my size is my indice. Two of them support two imbedded
	 * iterations; else there could be collisions (using the same array twice).
	 * Names of methods using iasB should always end in a B, like toArrayB();
	 * everything else should use iasA. */
	public static final int[][] IAS_B = new int[IAS_LENGTH][];

	// used by below static block only
	private static int[] newArray(int bits) {
		final int n = Integer.bitCount(bits);
		int[] array = new int[n];
		int cnt = 0;
		for ( int i=0; cnt<n; ++i )
			if ( (bits & (1<<i)) != 0 )
				array[cnt++] = i;
		assert cnt == n; // even if they're both 0
		return array;
	}

	static {
		for ( int i=0; i<BITS_PER_ELEMENT; ++i )
			SHFT[i] = 1<<i;
		for ( int i=0; i<IAS_LENGTH; ++i ) {
			IAS_A[i] = new int[i];
			IAS_B[i] = new int[i];
		}
		for ( int i=0; i<WORDS.length; ++i ) {
			// nb: no need to roll my own when I can Indexes.toValuesArray it.
			// The downside is that Indexes.toValuesArray has to handle any i.
			WORDS[i] = newArray(i);
//			WORD_SIZE[i] = WORDS[i].length;
		}
	}

	/**
	 * Returns a new Idx containing the given cell indice.
	 *
	 * @param indice the grid.cells indice of the cell to be added to this Idx
	 * @return a new Idx containing only the given cell indice.
	 */
	public static Idx of(int indice) {
		Idx idx = new Idx();
		idx.add(indice);
		return idx;
	}

	/**
	 * Returns a new Idx containing the given cells.
	 *
	 * @param cells
	 * @return
	 */
	public static Idx of(Iterable<Cell> cells) {
		Idx idx = new Idx();
		for ( Cell c : cells )
			idx.add(c.i);
		return idx;
	}

	/**
	 * Returns a new Idx containing the given cells.
	 *
	 * @param cells
	 * @return
	 */
	public static Idx of(Cell[] cells) {
		Idx idx = new Idx();
		for ( Cell c : cells )
			idx.add(c.i);
		return idx;
	}

	/**
	 * Returns is size(a0,a1,a2) &lt;= max.
	 *
	 * @param a0
	 * @param a1
	 * @param a2
	 * @param max
	 * @return
	 */
	public static boolean sizeLTE(int a0, int a1, int a2, int max) {
		return Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2) <= max;
	}

	/**
	 * Return the cardinality of the "virtual Idx" a0, a1, a2.
	 * <p>
	 * NB: Uses Integer.bitCount, coz that's faster than initialising.
	 *
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return the number of set (1) bits in the Idx
	 */
	public static int size(int a0, int a1, int a2) {
		return Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2);
	}

	/**
	 * Parse an Idx from CSV (comma separated values) of Cell.id's.
	 * <p>
	 * For example: "E3, H3, I3, H8, I8, E6, I6"
	 *
	 * @param csv a comma separated values String of Cell.id's.
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

	/**
	 * Returns (s1 & s2) == 0.
	 *
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean andEmpty(Idx s1, Idx s2) {
		return (s1.a0 & s2.a0) == 0
			&& (s1.a1 & s2.a1) == 0
			&& (s1.a2 & s2.a2) == 0;
	}

	/**
	 * Returns is either s1 or s2 a superset of the other.
	 * Ie is s1 a superset of s2 OR s2 a superset of s1?
	 * Ie andEqualsS2(s1, s2) || andEqualsS2(s2, s1)
	 * Ie does either of these Sets contain all of the values in the other?
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean andEqualsEither(Idx s1, Idx s2) {
		int m0 = (s1.a0 & s2.a0)
		  , m1 = (s1.a1 & s2.a1)
		  , m2 = (s1.a2 & s2.a2);
		return ( m0==s1.a0 && m1==s1.a1 && m2==s1.a2 )
			|| ( m0==s2.a0 && m1==s2.a1 && m2==s2.a2 );
	}

	/**
	 * Returns does (s1 | s2) == s2.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean orEqualsS2(Idx s1, Idx s2) {
		return (s1.a0 | s2.a0) == s2.a0
			&& (s1.a1 | s2.a1) == s2.a1
			&& (s1.a2 | s2.a2) == s2.a2;
	}

	/**
	 * Returns does (s1 | s2) == either s1 or s2.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean orEqualsEither(Idx s1, Idx s2) {
		return orEqualsS2(s1, s2) || orEqualsS2(s2, s1);
	}

	/**
	 * Constructs a new Idx(s1 & s2).
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static Idx newAnd(Idx s1, Idx s2) {
		return new Idx(s1.a0&s2.a0, s1.a1&s2.a1, s1.a2&s2.a2);
	}

	/**
	 * Constructs a new Idx(s1 | s2).
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static Idx newOr(Idx s1, Idx s2) {
		return new Idx(s1.a0|s2.a0, s1.a1|s2.a1, s1.a2|s2.a2);
	}

	/**
	 * Returns a cached (Idx.IAS_A) array of the indices contained in the
	 * Idx(m0,m1,m2), without creating a bloody Idx, or a new array. sigh.
	 *
	 * @param m0
	 * @param m1
	 * @param m2
	 * @return
	 */
	public static int[] toArrayA(int m0, int m1, int m2) {
		int bits, j, i=0;
		final int n = Integer.bitCount(m0)+Integer.bitCount(m1)+Integer.bitCount(m2);
		int[] a = IAS_A[n];
		if ( (bits=m0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					a[i++] = j+k;
		if ( (bits=m1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					a[i++] = BITS_PER_ELEMENT+j+k;
		if ( (bits=m2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					a[i++] = BITS_TWO_ELEMENTS+j+k;
		return a;
	}

	/**
	 * Static forEach invokes {@code v.visit(count, indice)} for each indice in
	 * Idx(a1,a2,a3). The count is (0 based) number of indices visited so far.
	 * The indice is this cells indice in Grid.cells.
	 * <p>
	 * I'm well proud of these forEach methods. They save implementing the same
	 * tricky code many times, just to do different things with the indices.
	 * It's really quite clever; as are the closures allowing us to implement
	 * Visitor so succinctly.
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @param v is invoked with each count (number of indices so far) and
	 * the indice, for each set (1) bit in this Idx.
	 * @return the number of indices visited, ie the end count.
	 */
	public static int forEach(int a0, int a1, int a2, Visitor2 v) {
		int j, count=0;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a0>>j)&WORD_MASK] )
					v.visit(count++, j+k);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a1>>j)&WORD_MASK] )
					v.visit(count++, BITS_PER_ELEMENT+j+k);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a2>>j)&WORD_MASK] )
					v.visit(count++, BITS_TWO_ELEMENTS+j+k);
		return count;
	}

	/**
	 * Static forEach visits each indice in a1,a2,a3: ie invokes
	 * {@code v.visit(indice)} for each set (1) bit in the Idx(a1,a2,a3).
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @param v your implementation of Visitor1 does whatever with indice
	 */
	public static void forEach(int a0, int a1, int a2, Visitor1 v) {
		int j;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a0>>j)&WORD_MASK] )
					v.visit(j+k);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a1>>j)&WORD_MASK] )
					v.visit(BITS_PER_ELEMENT+j+k);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a2>>j)&WORD_MASK] )
					v.visit(BITS_TWO_ELEMENTS+j+k);
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
	 * The Copy Constructor: Returns a new Idx containing a copy of src.
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

	/**
	 * Returns a new Idx containing a copy of the contents of this Idx.
	 * @return
	 */
	@Override
	public Idx clone() {
		try {
			Idx clone = (Idx)super.clone();
			clone.a0 = a0;
			clone.a1 = a1;
			clone.a2 = a2;
			return clone;
		} catch (CloneNotSupportedException eaten) {
			return null; // Object.clone() never fails!
		}
	}

	// ------------------------------- mutators -------------------------------

	/**
	 * Set this = src.
	 * @param src
	 * @return this Idx, for method chaining.
	 */
	public Idx set(Idx src) {
		a0 = src.a0;
		a1 = src.a1;
		a2 = src.a2;
		return this;
	}

	/**
	 * Set this = src; if src is null then this Idx is empty.
	 * <p>
	 * This null-safe version of set is required by KrakenFisherman only, where
	 * it IS required, else there's s__t loads of ____ing around to avoid it.
	 *
	 * @param src
	 */
	public void setNullSafe(Idx src) {
		// null check required by KrakenFisherman. sigh.
		if ( src == null ) {
			a0 = a1 = a2 = 0; // clear
		} else {
			a0 = src.a0;
			a1 = src.a1;
			a2 = src.a2;
		}
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
	 * Set this = (m0,m1,m2) and return any.
	 * @param m0 the mask of the first 27 indices in the Idx
	 * @param m1 the mask of the second 27 indices in the Idx
	 * @param m2 the mask of the third 27 indices in the Idx
	 * @return any?
	 */
	public boolean setAny(int m0, int m1, int m2) {
		a0 = m0;
		a1 = m1;
		a2 = m2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa & bb).
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
	 * Set this = (aa & bb) and returns any.
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
	 * Set this = (aa & bb) and returns size() > 1.
	 *
	 * @param aa
	 * @param bb
	 * @return
	 */
	public boolean setAndMany(Idx aa, Idx bb) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return (a0|a1|a2)!=0 && size()>1;
	}

	/**
	 * Set this = (aa & bb) and returns size()>=min.
	 *
	 * @param aa
	 * @param bb
	 * @param min
	 * @return
	 */
	public boolean setAndMin(Idx aa, Idx bb, int min) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return (a0|a1|a2)!=0 && size()>=min;
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
		// "& ALL" clears bits[28+] for bitCount et al.
		a0 = (aa.a0 & ~bb.a0) & ALL;
		a1 = (aa.a1 & ~bb.a1) & ALL;
		a2 = (aa.a2 & ~bb.a2) & ALL;
		return this;
	}

	/**
	 * Set this = aa & ~bb & ~cc.
	 * @param aa
	 * @param bb
	 * @param cc
	 * @return this Idx, for method chaining.
	 */
	public Idx setAndNot(Idx aa, Idx bb, Idx cc) {
		a0 = (aa.a0 & ~bb.a0 & ~cc.a0) & ALL;
		a1 = (aa.a1 & ~bb.a1 & ~cc.a1) & ALL;
		a2 = (aa.a2 & ~bb.a2 & ~cc.a2) & ALL;
		return this;
	}

	/**
	 * Clears this Idx, ie removes all indices.
	 * @return this Idx, for method chaining.
	 */
	public Idx clear() {
		a0 = a1 = a2 = 0;
		return this;
	}

	/**
	 * Add the given indice 'i' to this Idx.
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
	 * Add the given indice 'i' to this Idx if it doesn't already exist, and
	 * return was it added; ie an indice visitor, returning true the first time
	 * an indice is seen, and false there-after.
	 * <p>
	 * My advantage is I'm only one method call verses contains(i) then add(i),
	 * so I'm faster outright. I am NOT atomic, as you might reasonably expect.
	 * I don't know how to do this operation atomically.
	 * <p>
	 * I'm currently used only by {@link #toArraySmart()}, but is public, for
	 * possible use elsewhere.
	 *
	 * @param i the indice to add.
	 * @return was 'i' added to this Idx?
	 */
	public boolean addOnly(int i) {
		assert i>=0 && i<81;
		if ( i < BITS_PER_ELEMENT ) {
			if ( (a0 & SHFT[i]) != 0 )
				return false;
			a0 |= SHFT[i];
		} else if ( i < BITS_TWO_ELEMENTS ) {
			if ( (a1 & SHFT[i-BITS_PER_ELEMENT]) != 0 )
				return false;
			a1 |= SHFT[i-BITS_PER_ELEMENT];
		} else {
			if ( (a2 & SHFT[i-BITS_TWO_ELEMENTS]) != 0 )
				return false;
			a2 |= SHFT[i-BITS_TWO_ELEMENTS];
		}
		return true;
	}

	/**
	 * Add the indice of each of the given cells to this Idx.
	 * <p>
	 * You can override this one!
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
	 * @param cells
	 */
	public void removeAll(Iterable<Cell> cells) {
		for ( Cell c : cells )
			remove(c.i);
	}

	/**
	 * Mutate this &= other.
	 * @param aa
	 * @return this Idx, for method chaining.
	 */
	public Idx and(Idx aa) {
		a0 &= aa.a0;
		a1 &= aa.a1;
		a2 &= aa.a2;
		return this;
	}

	/**
	 * Mutate this &= ~aa.
	 * @param aa
	 * @return this Idx, for method chaining.
	 */
	public Idx andNot(Idx aa) {
		a0 = (a0 & ~aa.a0) & ALL;
		a1 = (a1 & ~aa.a1) & ALL;
		a2 = (a2 & ~aa.a2) & ALL;
		return this;
	}

	/**
	 * Mutate this |= aa.
	 * @param aa
	 * @return this Idx, for method chaining.
	 */
	public Idx or(Idx aa) {
		a0 |= aa.a0;
		a1 |= aa.a1;
		a2 |= aa.a2;
		return this;
	}

	/**
	 * Mutate this |= (aa & bb & cc)
	 * @param aa
	 * @param bb
	 * @param cc
	 */
	public void orAnd(Idx aa, Idx bb, Idx cc) {
		a0 |= (aa.a0 & bb.a0 & cc.a0);
		a1 |= (aa.a1 & bb.a1 & cc.a1);
		a2 |= (aa.a2 & bb.a2 & cc.a2);
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
	 * @return the first (lowest) indice which has been removed from this Set,
	 * else (this Set is empty) return -1.
	 */
	public int poll() {
		int x;
		if ( a0 != 0 ) {
			x = a0 & -a0;	// lowestOneBit
			a0 &= ~x;		// remove x
			return Integer.numberOfTrailingZeros(x);
		}
		if ( a1 != 0 ) {
			x = a1 & -a1;	// lowestOneBit
			a1 &= ~x;		// remove x
			// 27 is Idx.BITS_PER_ELEMENT
			return 27 + Integer.numberOfTrailingZeros(x);
		}
		if ( a2 != 0 ) {
			x = a2 & -a2;	// lowestOneBit
			a2 &= ~x;		// remove x
			// 54 is 2 * Idx.BITS_PER_ELEMENT
			return 54 + Integer.numberOfTrailingZeros(x);
		}
		return -1; // this set is empty
	}

	// -------------------------------- queries -------------------------------

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

	/**
	 * Return is this Idx empty?
	 * @return true if this Idx contains no indices.
	 */
	public boolean none() {
		return (a0|a1|a2) == 0;
	}

	/**
	 * Return is this Idx NOT empty?
	 * @return true if this Idx contains any indices.
	 */
	public boolean any() {
		return (a0|a1|a2) != 0;
	}

	/**
	 * Return the number of indices in this Idx.
	 * <p>
	 * NOTE: I've tried everything, and just bitCounting all three segments
	 * and adding them together seems to be the fastest. KISS it!
	 *
	 * @return {@code Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2)}
	 */
	public int size() {
		return Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2);
	}

	/**
	 * Returns !(this & aa).none(), ie does this Idx intersect aa.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(aa).any()}.
	 *
	 * @param aa
	 * @return is the intersection of this and other NOT empty.
	 */
	public boolean andAny(Idx aa) {
		return ( (a0 & aa.a0) | (a1 & aa.a1) | (a2 & aa.a2) ) != 0;
	}

	/**
	 * Returns (this & aa).none(), ie does this Idx NOT intersect aa.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(aa).none()}.
	 *
	 * @param aa
	 * @return is the intersection of this and other empty.
	 */
	public boolean andNone(Idx aa) {
		return ( (a0 & aa.a0) | (a1 & aa.a1) | (a2 & aa.a2) ) == 0;
	}

	/**
	 * Return (this & aa) == this, ie is this a superset of aa?
	 * @param aa
	 * @return
	 */
	public boolean andEqualsThis(Idx aa) {
		return (a0 & aa.a0) == a0
			&& (a1 & aa.a1) == a1
			&& (a2 & aa.a2) == a2;
	}

	/**
	 * Does this Idx contain the given indice 'i'?
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
	 * @param i
	 * @return the other indice in this Idx
	 */
	public int otherThan(int i) {
		assert i>=0 && i<81;
		// make a local copy of this Idx to mutate
		int a0=this.a0, a1=this.a1, a2=this.a2;
		// remove i
		if ( i < BITS_PER_ELEMENT )
			a0 &= ~SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 &= ~SHFT[i%BITS_PER_ELEMENT];
		else
			a2 &= ~SHFT[i%BITS_PER_ELEMENT];
		// peek
		if ( a0 != 0 )
			return Integer.numberOfTrailingZeros(a0);
		if ( a1 != 0 )
			return BITS_PER_ELEMENT + Integer.numberOfTrailingZeros(a1);
		if ( a2 != 0 )
			return BITS_TWO_ELEMENTS + Integer.numberOfTrailingZeros(a2);
		// found none
		return -1;
	}

	// ----------------------------- bulk queries -----------------------------

	/**
	 * Returns the given array (which is presumed to be of sufficient size)
	 * populated with the indices in this Idx.
	 * <p>
	 * Note that the array is NOT cleared first. It's presumed new or empty;
	 * or you want to add to the existing contents.
	 * @param array
	 * @return the given array, for method chaining.
	 */
	public int[] toArray(int[] array) {
		forEach((cnt, i) -> array[cnt] = i);
		return array;
	}

	/**
	 * Read indices in this Idx into the given array, and return how many, so
	 * that you can use a re-usable (fixed-size) array.
	 * @param array
	 * @return the number of elements added (ergo idx.size())
	 */
	public int toArrayN(int[] array) {
		try {
			return forEach((cnt, i) -> array[cnt] = i);
		} catch ( ArrayIndexOutOfBoundsException ex ) {
			return array.length;
		}
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
		if ( (a0|a1|a2) == 0 )
			return IAS_A[0];
		int[] array = IAS_A[Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2)];
		int bits, j, count=0;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = j+k;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = BITS_PER_ELEMENT+j+k;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = BITS_TWO_ELEMENTS+j+k;
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
	public int[] toArrayB() {
		if ( (a0|a1|a2) == 0 )
			return IAS_B[0];
		int[] array = IAS_B[Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2)];
		int bits, j, count=0;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = j+k;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = BITS_PER_ELEMENT+j+k;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					array[count++] = BITS_TWO_ELEMENTS+j+k;
		return array;
	}

	/**
	 * Returns a new array containing the indice in this Idx.
	 * <p>
	 * Prefer toArrayA or toArrayB in loops: if you call me in a loop then you
	 * are creating (potentially many) garbage arrays to keep the GC busy.
	 * @return
	 */
	public int[] toArrayNew() {
		// there is no need for a new 0-length array coz it's immutable.
		if ( none() )
			return IAS_A[0];
		return toArray(new int[size()]);
	}

	/**
	 * Called once, before the toArraySmart() loop; to reset my indexes.
	 */
	public void toArraySmartReset() {
		if ( usedA == null ) {
			usedA = new Idx();
			usedB = new Idx();
		} else {
			usedA.clear();
			usedB.clear();
		}
	}

	/**
	 * You call me in a loop: I return arrayA, else arrayB, else a new array;
	 * minimising garbage while still guaranteeing unique arrays, just don't
	 * use toArrayA or toArrayB elsewhere in loop, including invoked methods.
	 *
	 * @return a unique int array, populated with the indices in this Idx.
	 */
	public int[] toArraySmart() {
		if ( (a0|a1|a2) == 0 )
			return IAS_A[0];
		final int n = Integer.bitCount(a0)+Integer.bitCount(a1)+Integer.bitCount(a2);
		if ( usedA.addOnly(n) )
			return toArrayA();
		if ( usedB.addOnly(n) )
			return toArrayB();
		return toArrayNew();
	}
	// idx's of the size of arrayA's and arrayB's used.
	// each physical array may be used only once.
	private Idx usedA, usedB;

	public String ids() {
		final StringBuilder sb = new StringBuilder(128);
		forEach((cnt, i) -> {
			if ( cnt > 0 )
				sb.append(' ');
			sb.append(CELL_IDS[i]);
		});
		return sb.toString();
	}

	public Set<Cell> toCellSet(Grid grid) {
		Set<Cell> result = new LinkedHashSet<>(size(), 1F);
		forEach((i) -> result.add(grid.cells[i]));
		return result;
	}

	// ------------------------ Visitor2: count, indice -----------------------
	//
	//                Welcome to the alien visitation centre.
	//                             Bring Towel!
	//

	/**
	 * You implement Visitor2 (typically with a lambda expression) in order to
	 * call my forEach method to invoke {@code visitor.visit(count, indice)}
	 * for each indice in the Idx. The count is the (0 based) number of indices
	 * we've visited so far. The indice is the cells indice in Grid.cells.
	 * <p>
	 * I like lambda expressions, but only simple ones. They're very clever!
	 * I would like to nominate the dude/s who invented closures for the Nobel
	 * prize for information technology.
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
	 * {@code forEach(Visitor2 v)} invokes v.visit for each indice in this Idx,
	 * passing:<ul>
	 * <li>count: 0-based count of indices visited so far.
	 * <li>indice: 0-based indice of this cell in Grid.cells.
	 * </ul>
	 * This version of forEach facilitates adding each cell in this Idx to an
	 * array.
	 *
	 * @param v is typically a lambda expression implementing Visitor2.visit,
	 *  but any implementation of Visitor2 will do nicely.
	 * @return total number of indices visited, ie the size of this Idx
	 */
	public int forEach(Visitor2 v) {
		int bits, j, count=0;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(count++, j+k);
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(count++, BITS_PER_ELEMENT+j+k);
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(count++, BITS_TWO_ELEMENTS+j+k);
		return count;
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
	 * forEach visits each indice in this Idx, ie invokes
	 * {@code v.visit(indice)} for each set (1) bit (the indice) in this Idx.
	 *
	 * @param v your implementation of Visitor1 does whatever with indice
	 */
	public void forEach(Visitor1 v) {
		if ( (a0|a1|a2) == 0 )
			return;
		int bits, j;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(j+k);
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(BITS_PER_ELEMENT+j+k);
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					v.visit(BITS_TWO_ELEMENTS+j+k);
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
	 * Visit each indice, until v.visit returns false.
	 *
	 * @param v an implementation of UntilFalseVisitor, usually a lambda.
	 * @return true if we made it through all indices without returning false,
	 * so note that calling me on an empty Idx always returns true. Not clever.
	 */
	public boolean untilFalse(UntilFalseVisitor v) {
		int j;
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a0>>j)&WORD_MASK] )
					if ( !v.visit(j+k) )
						return false;
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a1>>j)&WORD_MASK] )
					if ( !v.visit(BITS_PER_ELEMENT+j+k) )
						return false;
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(a2>>j)&WORD_MASK] )
					if ( !v.visit(BITS_TWO_ELEMENTS+j+k) )
						return false;
		return true;
	}

	// ------------------------- Just visiting! -------------------------------

	public interface CellVisitor {
		void visit(Cell cell);
	}
	public int forEach(Cell[] cells, CellVisitor v) {
		int bits, j, count = 0;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(cells[j+k]);
					++count;
				}
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(cells[BITS_PER_ELEMENT+j+k]);
					++count;
				}
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(cells[BITS_TWO_ELEMENTS+j+k]);
					++count;
				}
		return count;
	}

	// ---------------------------- common buddies ----------------------------

	/**
	 * This forEach invokes 'first' for the first indice in this Idx, and then
	 * 'subsequent' for each subsequent indice in this Idx.
	 * <p>
	 * It's intended for use by the commonBuddies method, but there's no
	 * reason to not use it elsewhere.
	 *
	 * @param first Visitor1 invoked for the first indice in this Idx
	 * @param subsequent Visitor1 invoked for each subsequent indice in this
	 *  Idx
	 */
	public void forEach(Visitor1 first, Visitor1 subsequent) {
		Visitor1 v = first;
		int bits, j;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(j+k);
					v = subsequent;
				}
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(BITS_PER_ELEMENT+j+k);
					v = subsequent;
				}
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(BITS_TWO_ELEMENTS+j+k);
					v = subsequent;
				}
	}

	/**
	 * Repopulates result with those cells which see (same box, row, or col)
	 * <b>ALL</b> of the cells in this Idx, and returns the result.
	 *
	 * @param result is repopulated with the buddies of ALL cells in this Idx.
	 * @return result
	 */
	public Idx commonBuddies(Idx result) {
		forEach(
			  (i) -> result.set(BUDDIES[i]) // first
			, (i) -> result.and(BUDDIES[i]) // subsequent
		);
		return result;
	}

	// --------------------------------- buds --------------------------------

	/**
	 * Get a cached Idx of this Idx plus the buddies of cells in this Idx.
	 * If this is the first buds call, or this idx has changed since the
	 * previous buds call, then buds are (re)calculated.
	 *
	 * @return the indices in this Idx and all of there buddies, CACHED!
	 */
	public Idx plusBuds() {
		if ( plusBuds == null ) {
			plusBuds = new Idx(this);
			forEach((i)->plusBuds.or(BUDDIES[i]));
			bp0=a0; bp1=a1; bp2=a2;
		} else if ( a0!=bp0 || a1!=bp1 || a2!=bp2 ) {
			plusBuds.set(this);
			forEach((i)->plusBuds.or(BUDDIES[i]));
			bp0=a0; bp1=a1; bp2=a2;
		}
		return plusBuds;
	}
	private Idx plusBuds;
	private int bp0, bp1, bp2;

	/**
	 * Get an Idx of the buddies of cells in this Idx.
	 *
	 * @return the buddies of cells in this idx (uncached).
	 */
	public Idx buds() {
		if ( buds == null )
			buds = new Idx();
		else
			buds.clear();
		forEach((i)->buds.or(BUDDIES[i]));
		// remove me from buds, in case I contain cells which see each other.
		buds.andNot(this);
		return buds;
	}
	private Idx buds;

	// --------------------------------- cells --------------------------------

	/**
	 * Get the cells from the given Grid at indices in this Idx.
	 * Use this one with the right sized array, which is returned.
	 * @param grid the grid to read cells from
	 * @param cells the array to populate with cells in this Idx.
	 * @return the given cells array, so create new if you must.
	 */
	public Cell[] cells(Grid grid, Cell[] cells) {
		final Cell[] source = grid.cells;
		forEach((count, indice) -> cells[count] = source[indice]);
		return cells;
	}

	// cellsN is hammered by Aligned*Exclusion, so create ONE instance of
	// CellsNVisitor, just to be more memory efficient, that's all.
	private static final class CellsNVisitor implements Visitor2 {
		public Cell[] source;
		public Cell[] destination;
		@Override
		public void visit(int count, int indice) {
			destination[count] = source[indice];
		}
	}
	private static final CellsNVisitor CELLS_N_VISITOR = new CellsNVisitor();
	/**
	 * Get the cells from the given Grid at indices in this Idx. Use this one
	 * with a fixed-sized array, and I return the size.
	 * @param source the grid.cells to read from
	 * @param destination the array to populate with cells in this Idx.
	 * @return the number of indices visited, ie the size of this Idx
	 */
	public int cellsN(Cell[] source, Cell[] destination) {
		CELLS_N_VISITOR.source = source;
		CELLS_N_VISITOR.destination = destination;
		return forEach(CELLS_N_VISITOR);
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
		return forEach(CELLS_MAYBES_VISITOR);
	}

	/**
	 * Get the cells from the given Grid at indices in this Idx.
	 * This one returns a Grid.cas array for speed and convenience, so clone()
	 * it to keep it (it's use or loose it Baby).
	 * @param grid the grid to read cells from
	 * @return the {@code Cell[]} from the Grid.cas (cache) so clone to keep.
	 */
	public Cell[] cells(Grid grid) {
		return cells(grid, Cells.array(size()));
	}

	// ----------------------------- stringy stuff ----------------------------

	/**
	 * Append space separated cell-IDs (SSV) of the given Idx(a0, a1, a2) to
	 * the given StringBuilder. First ensureCapacity of length + the given 'n'.
	 * <p>
	 * Each bitset uses 27 least significant (rightmost) bits, for 81 cells.
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
		forEach(a0, a1, a2, (cnt, g) -> {
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
	 * @param a0 the first mask
	 * @param a1 the second mask
	 * @param a2 the third mask
	 * @return the cell-id's of indices in this Idx.
	 */
	public static String toCellIdString(int a0, int a1, int a2) {
		int n = size(a0,a1,a2) * 3;
		return append(getSB(n), n, a0, a1, a2).toString();
	}

	/**
	 * For debugging.
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static String toIndiceString(int a0, int a1, int a2) {
		StringBuilder sb = getSB(size(a0,a1,a2) * 3);
		forEach(a0, a1, a2, (cnt, i) -> {
			if(cnt>0) sb.append(' ');
			sb.append(i); // the raw indice
		});
		return sb.toString();
	}
	public String toIndiceString() {
		return toIndiceString(a0, a1, a2);
	}

	/**
	 * Returns a String representation of this Idx.
	 * @return
	 */
	@Override
	public String toString() {
		if ( true ) // @check true
			// cell-ids toString for normal use
			return toCellIdString(a0, a1, a2);
		else
			// debug only: the raw indices
			return toIndiceString(a0, a1, a2);
	}

	// get my StringBuilder... I keep ONE that grows.
	private static StringBuilder getSB(int capacity) {
		if ( SB == null )
			return SB = new StringBuilder(capacity);
		SB.setLength(0);
		if ( capacity > SB.capacity() )
			SB.ensureCapacity(capacity);
		return SB;
	}
	private static StringBuilder SB;

	// ------------------------------- plumbing -------------------------------

	/**
	 * Returns -1, 0, 1 as this is less than, equal, or more than aa.
	 * @param aa
	 * @return
	 */
	@Override
	public int compareTo(Idx aa) {
		int diff;
		if ( (diff=a2 - aa.a2) == 0
		  && (diff=a1 - aa.a1) == 0 )
			diff = a0 - aa.a0;
		if ( diff < 0 )
			return -1;
		if ( diff > 0 )
			return 1;
		return 0;
	}

	/**
	 * Returns a new Idx of all cells in this Idx which match the CellFilter.
	 * @param cells
	 * @param f any CellFilter to accept (or reject) each cell.
	 * @return a new Idx of matching indices/cells in this Idx.
	 */
	public Idx where(Cell[] cells, CellFilter f) {
		int bits, j, r0=0,r1=0,r2=0; // the result
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[j+k]) )
						r0 |= SHFT[j+k];
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[BITS_PER_ELEMENT+j+k]) )
						r1 |= SHFT[j+k];
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[BITS_TWO_ELEMENTS+j+k]) )
						r2 |= SHFT[j+k];
		return new Idx(r0,r1,r2);
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Hash this Idx: fold each int in half and offset them a tad. The result
	 * is a pretty trashy hashCode, but we're smashing 81 bits into 32, so some
	 * loss, and therefore collisions, are inevitable. The trick is minimizing
	 * the collisions, and I haven't really done that: just thrown something
	 * together that works a bit, because I'm not a mathematicians asshole.
	 * <p>
	 * My humour is a bit off too! Binary LOLs! What's LOL in binary! NANANA!
	 * Come at me with that fruit!
	 *
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return the hashCode for this Idx
	 */
	public static int hashCode(int a0, int a1, int a2) {
		return ((a2^(a2>>>16))<<5)
			 ^ ((a1^(a1>>>16))<<3)
			 ^ (a0^(a0>>>16));
	}

	/**
	 * Hash this Idx into 64 bits, with less collisions than 32 bits. We're
	 * squeezing 81 bits into 64, so 17 collide. In this implementation the
	 * low 17 of a0 collide with the high 17 of a1. Best possible, AFAIK.
	 *
	 * @param a0 a long variable, but an int value is required
	 * @param a1 a long variable, but an int value is required
	 * @param a2 a long variable, but an int value is required
	 * @return the hash64 of this Idx
	 */
	public static long hash64(long a0, long a1, long a2) {
		return (a2<<37L) ^ (a1<<27L) ^ a0;
	}

	/**
	 * Note that the returned hashCode is cached, so it will NOT change after
	 * it's gotten, and so will not reflect any changes in this Idx; which
	 * breaks the {@code equals <-> hashCode} contract... so DO NOT modify an
	 * Idx AFTER you've added it a hash-anything. Clear! Which, luckily enough
	 * is how things tend to work anyways. You get it, you modify it, you stash
	 * it... it's just some assholes think they can reorder things without
	 * consequence because they're special, when they're not: the requirement
	 * for respect of the laws of physics is universal. This is just another
	 * annoying rule, like the zeroeth law of thermodynamics: if you piss the
	 * smart people off enough, they'll kill you, and ALL of your friends,
	 * your family, your neighborhood, your city, it's sister city, all the
	 * capital cities, and then set-about kicking the absolute living s__t out
	 * of the Pauline voters. Straight to the meat works.
	 * @return
	 */
	@Override
	public int hashCode() {
		if ( hc == 0 )
			hc = hashCode(a0, a1, a2);
		return hc;
	}
	private int hc;

	/**
	 * Does this equal the Idx represented by the given binary masks.
	 * @param m0
	 * @param m1
	 * @param m2
	 * @return
	 */
	public boolean equals(int m0, int m1, int m2) {
		return a0==m0 && a1==m1 && a2==m2;
	}

	/**
	 * Does this equal the given Object.
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		return this==o || o instanceof Idx && equals((Idx)o);
	}

	/**
	 * Return does this == aa.
	 * @param aa
	 * @return
	 */
	public boolean equals(Idx aa) {
		return a0 == aa.a0
			&& a1 == aa.a1
			&& a2 == aa.a2;
	}

}
