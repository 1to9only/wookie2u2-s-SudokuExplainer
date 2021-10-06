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
import diuf.sudoku.utils.IFilter;
import diuf.sudoku.utils.IVisitor;
import java.io.Serializable;
import java.util.ArrayList;
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
 * Just pushing the cheese around on my cracker. Ye rolls yon dice and ye moves
 * yon mice. I apologise for any confusion.
 * <p>
 * NOTE: because Idx is wholey structured around a GRID_SIZE of 81 it does not
 * use GRID_SIZE or any of the related constants.
 * <p>
 * <pre>
 * See: diuf.sudoku.solver.hinters.xyz.XYWing // uses Idx's
 * See: diuf.sudoku.solver.hinters.align.Aligned5Exclusion_1C // uses int[]
 * See: diuf.sudoku.solver.hinters.align.LinkedMatrixCellSet.idx()
 * See: diuf.sudoku.Grid.cellsAt(int[] idx)
 * </pre>
 * <pre>
 * KRC 2020-10-21 Idx.cells replaces Grid.at, localising all the tricky s__t.
 * I never liked Idx gizzards in my Grid, I was just too lazy to act. Simpler!
 *
 * KRC 2020-10-22 IdxL extends Idx to provide a locking mechanism, but
 * <b>PLEASE NOTE</b> that IdxL <b>MUST</b> override <b>EVERY</b> mutator, or
 * it's basically useless!
 *
 * KRC 2021-05-20 I really want to eradicate the BitIdx class, so I just tried
 * moving Idx into Grid as an inner-class, to get rid of the need to pass the
 * grid into forEach et al, but then you need a grid instance to create an Idx,
 * and the Grids static Idxs are "needed" for speed, so I gave up; leaving Idx
 * as an independant class, and just wear passing the Grid around, and the need
 * for a BitIdx to iterate Idx'd cells. It's messy, but the only other way I
 * can think of is a new IdxIt class which overrides Idx's forEach et al to
 * pass in Grid.this to iterate, but then the cut-lunch Idx's would lack this
 * feature, rendering it more-or-less useless, I think. sigh.
 * </pre>
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
	public static final int WORD_MASK = 0x1FF; // Binary 1111, Decimal 15

	/** the value of ALL bits (ie 27 set bits) in an Idx element. */
	public static final int ALL = (1<<BITS_PER_ELEMENT)-1;

	/** the left-shifted bitset value of each possible index. */
	public static final int[] SHFT = new int[BITS_PER_ELEMENT];

	/** WORDS[9-bit-word] contains the values that are packed into that
	 * 9-bit-word. There are 3 words per 27-bit element. */
	public static final int[][] WORDS = new int[1<<9][]; // [0..511][see WSIZE]

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

	static {
		for ( int i=0; i<BITS_PER_ELEMENT; ++i ) {
			SHFT[i] = 1<<i;
		}
		for ( int i=0; i<IAS_LENGTH; ++i ) {
			IAS_A[i] = new int[i];
			IAS_B[i] = new int[i];
		}
		for ( int i=0; i<WORDS.length; ++i ) {
			WORDS[i] = Indexes.toValuesArrayNew(i);
		}
	}

	public static Idx empty() {
		return new Idx(); // they're empty by default
	}
	public static Idx full() {
		return new Idx(true); // they're full by adding a dodgy true
	}

	/**
	 * Returns a new Idx containing the given cell indice.
	 *
	 * @param indice the grid.cells indice of the cell to be added to this Idx
	 * @return a new Idx containing only the given cell indice.
	 */
	public static final Idx of(final int indice) {
		final Idx idx = new Idx();
		idx.add(indice);
		return idx;
	}

	/**
	 * Returns a new Idx containing the given cells.
	 *
	 * @param cells
	 * @return
	 */
	public static final Idx of(final Iterable<Cell> cells) {
		final Idx idx = new Idx();
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
	public static final Idx of(final Cell[] cells) {
		final Idx idx = new Idx();
		for ( Cell c : cells )
			idx.add(c.i);
		return idx;
	}

	/**
	 * Returns a new Idx containing indices.
	 *
	 * @param indices
	 * @return
	 */
	public static Idx of(int[] indices) {
		int a0=0, a1=0, a2=0;
		for ( int i : indices )
			if ( i < BITS_PER_ELEMENT )
				a0 |= SHFT[i];
			else if ( i < BITS_TWO_ELEMENTS )
				a1 |= SHFT[i%BITS_PER_ELEMENT];
			else
				a2 |= SHFT[i%BITS_PER_ELEMENT];
		return new Idx(a0, a1, a2);
	}

	/**
	 * Returns a new Idx containing a copy of src.
	 *
	 * @param src Idx (which could be an IdxL or IdxI)
	 * @return
	 */
	static Idx of(Idx src) {
		return new Idx(src);
	}

	/**
	 * Returns a new Idx of ids.
	 * <p>
	 * Note that this method is currently only used by test-cases, where it's
	 * pretty central, so if this fails then many test-cases will fail.
	 *
	 * @param ids an array of cell-id's to add to the new Idx. Note that the
	 *  format of each cell-id must match the current Grid.ID_SCHEME, which is
	 *  currently Chess, so A1..I9
	 * @return a new Idx containing the indices of the cells in the given
	 *  array of cell-ids.
	 */
	static Idx of(String[] ids) {
		final Idx result = new Idx();
		for ( String id : ids )
			result.add(Grid.indice(id));
		return result;
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
	public static final boolean sizeLTE(final int a0, final int a1, final int a2, final int max) {
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
	public static final int size(final int a0, final int a1, final int a2) {
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
	public static final Idx parseCsv(final String csv) {
		final Idx idx = new Idx();
		if ( csv==null || csv.isEmpty() )
			return idx;
		final Pattern pattern = Pattern.compile("[A-I][1-9]");
		for ( final String id : csv.split(" *, *") ) // box 6, row 9, col A
			if ( pattern.matcher(id).matches() ) // ignore crap, just in case
				//        col               +  row
				idx.add( (id.charAt(0)-'A') + ((id.charAt(1)-'1')*9) );
		return idx;
	}

	/**
	 * Parse an Idx from SSV (space separated values) of Cell.id's.
	 * <p>
	 * For example: "E3 H3 I3 H8 I8 E6 I6"
	 * <p>
	 * Note that this is Idx's standard toString format! So WTF was I
	 * thinking implementing CSV and not SSV? I'm a putz!
	 *
	 * @param ssv space separated values (Cell.id's).
	 * @return a new Idx.
	 */
	public static final Idx parseSsv(final String ssv) {
		final Idx idx = new Idx();
		if ( ssv==null || ssv.isEmpty() )
			return idx;
		final Pattern pattern = Pattern.compile("[A-I][1-9]");
		for ( final String id : ssv.split(" +") )
			if ( pattern.matcher(id).matches() )
				//        col               +  row
				idx.add( (id.charAt(0)-'A') + ((id.charAt(1)-'1')*9) );
		return idx;
	}

	// Design Note: Home Boy is static: These "static query" methods have loose
	// euphamistic names to differentiate them from "instance queries", with
	// nice tight logic-based techie-speak type-names that actually make sense.

	/**
	 * Returns (s1 & s2) != 0.
	 *
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static final boolean intersects(final Idx s1, final Idx s2) {
		return ( (s1.a0 & s2.a0)
			   | (s1.a1 & s2.a1)
			   | (s1.a2 & s2.a2) ) != 0;
	}

	/**
	 * Returns (s1 & s2) == 0.
	 *
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static final boolean disjunct(final Idx s1, final Idx s2) {
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
	public static final boolean inbred(final Idx s1, final Idx s2) {
		final int m0=s1.a0&s2.a0, m1=s1.a1&s2.a1, m2=s1.a2&s2.a2;
		return ( m0==s1.a0 && m1==s1.a1 && m2==s1.a2 )
			|| ( m0==s2.a0 && m1==s2.a1 && m2==s2.a2 );
	}

	/**
	 * Constructs a new Idx(s1 & s2).
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static final Idx newAnd(final Idx s1, final Idx s2) {
		return new Idx(s1.a0&s2.a0, s1.a1&s2.a1, s1.a2&s2.a2);
	}

	/**
	 * Constructs a new Idx(s1 | s2).
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static final Idx newOr(final Idx s1, final Idx s2) {
		return new Idx(s1.a0|s2.a0, s1.a1|s2.a1, s1.a2|s2.a2);
	}

	/**
	 * Returns a cached (Idx.IAS_A) array of the indices contained in the
	 * Idx(m0,m1,m2), without creating a bloody Idx, or a new array. sigh.
	 *
	 * @param m0
	 * @param m1
	 * @param m2
	 * @return a cached array of indices (the set (1) bits) in the given Idx
	 */
	public static final int[] toArrayA(final int m0, final int m1, final int m2) {
		int j, i=0;
		final int n = Integer.bitCount(m0)+Integer.bitCount(m1)+Integer.bitCount(m2);
		final int[] a = IAS_A[n];
		if ( m0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(m0>>j)&WORD_MASK] )
					a[i++] = j+k;
		if ( m1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(m1>>j)&WORD_MASK] )
					a[i++] = BITS_PER_ELEMENT+j+k;
		if ( m2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(m2>>j)&WORD_MASK] )
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
	public static final int forEach(final int a0, final int a1, final int a2, final Visitor2 v) {
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
	public static final void forEach(final int a0, final int a1, final int a2, final Visitor1 v) {
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

	public static final String toString(final int a0, final int a1, final int a2) {
		if ( true ) // @check true
			// cell-ids toString for normal use
			return toStringCellIds(a0, a1, a2);
		else
			// debug only: the raw indices
			return toStringIndices(a0, a1, a2);
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
	 *
	 * @param src to copy
	 */
	public Idx(Idx src) {
		a0 = src.a0;
		a1 = src.a1;
		a2 = src.a2;
	}

	/**
	 * Constructs a new Idx containing the given 'bitsets': a0, a1, a2.
	 * <p>
	 * This constructor is currently only used by LinkedMatrixCellSet, of which
	 * there are two: in align and align2, because one day, hopefully, we'll be
	 * able to eliminate the old align package and all of its boiler-plate, but
	 * for the moment, align2 is slower. Sigh.
	 *
	 * @param bitsets a0, a1, a2.
	 */
	public Idx(int[] bitsets) {
		a0 = bitsets[0];
		a1 = bitsets[1];
		a2 = bitsets[2];
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
	 *
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
	 * Set this = trues in b.
	 *
	 * @param bits an array of 81 booleans
	 * @return this Idx, for method chaining.
	 */
	public Idx set(boolean[] bits) {
		int i = 0;
		for ( ; i<27; ++i )
			if(bits[i]) a0 |= SHFT[i];
		for ( ; i<54; ++i )
			if(bits[i]) a1 |= SHFT[i%BITS_PER_ELEMENT];
		for ( ; i<81; ++i )
			if(bits[i]) a2 |= SHFT[i%BITS_PER_ELEMENT];
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
	 *
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
	 * Set this Idx to indices of cells at indexes in the given cells array.
	 *
	 * @param cells
	 * @param indexes
	 * @return this Idx, for method chaining.
	 */
	public Idx set(Cell[] cells, int[] indexes) {
		a0 = a1 = a2 = 0;
		for ( int i : indexes )
			add(cells[i].i);
		return this;
	}

	/**
	 * Set this = (m0,m1,m2) and return any.
	 *
	 * @param m0 the mask of the first 27 indices in the Idx
	 * @param m1 the mask of the second 27 indices in the Idx
	 * @param m2 the mask of the third 27 indices in the Idx
	 * @return not empty
	 */
	public boolean setAny(int m0, int m1, int m2) {
		a0 = m0;
		a1 = m1;
		a2 = m2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa & bb).
	 *
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
	 *
	 * @param aa
	 * @param bb
	 * @return not empty
	 */
	public boolean setAndAny(Idx aa, Idx bb) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa & bb & cc) and returns any.
	 *
	 * @param aa
	 * @param bb
	 * @param cc
	 * @return not empty
	 */
	public boolean setAndAny(Idx aa, Idx bb, Idx cc) {
		a0 = aa.a0 & bb.a0 & cc.a0;
		a1 = aa.a1 & bb.a1 & cc.a1;
		a2 = aa.a2 & bb.a2 & cc.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (aa & bb) and returns size() > 1.
	 *
	 * @param aa
	 * @param bb
	 * @return 2 or more
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
	 * @return this Idx, for method chaining.
	 */
	public boolean setAndMin(Idx aa, Idx bb, int min) {
		a0 = aa.a0 & bb.a0;
		a1 = aa.a1 & bb.a1;
		a2 = aa.a2 & bb.a2;
		return (a0|a1|a2)!=0 && size()>=min;
	}

	/**
	 * Set this = src then remove indice and return self.
	 *
	 * @param src the source Idx
	 * @param indice to be removed
	 * @return this Idx, for method chaining.
	 */
	public Idx setExcept(Idx src, int indice) {
		set(src);
		remove(indice);
		return this;
	}

	/**
	 * Set this = (aa | bb).
	 *
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
	 *
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
	 *
	 * @param aa
	 * @param bb
	 * @param cc
	 * @return this Idx, for method chaining.
	 */
	public Idx setAndNot(Idx aa, Idx bb, Idx cc) {
		// "& ALL" clears bits[28+] for bitCount et al.
		a0 = (aa.a0 & ~bb.a0 & ~cc.a0) & ALL;
		a1 = (aa.a1 & ~bb.a1 & ~cc.a1) & ALL;
		a2 = (aa.a2 & ~bb.a2 & ~cc.a2) & ALL;
		return this;
	}

	/**
	 * Set this = aa & ~bb, and return any()
	 *
	 * @param aa
	 * @param bb
	 * @return are there any indices in this Idx
	 */
	public boolean setAndNotAny(Idx aa, Idx bb) {
		// "& ALL" clears bits[28+] for bitCount et al.
		a0 = (aa.a0 & ~bb.a0) & ALL;
		a1 = (aa.a1 & ~bb.a1) & ALL;
		a2 = (aa.a2 & ~bb.a2) & ALL;
		return (a0|a1|a2) != 0;
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
	 * Remove the given indice 'i' from this Idx.
	 *
	 * @param i the indice to be removed from this Idx
	 * @return false
	 */
	public boolean remove2(int i) {
		remove(i);
		return false;
	}

	/**
	 * Remove all of the given cells indices from this Set.
	 *
	 * @param cells
	 */
	public void removeAll(Iterable<Cell> cells) {
		for ( Cell c : cells )
			remove(c.i);
	}

	public void removeAll(Cell[] cells) {
		for ( Cell c : cells )
			remove(c.i);
	}

	/**
	 * Mutate this &= aa.
	 *
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
	 * Mutate this = this & aa & bb.
	 * Two and's, no waiting.
	 * @param aa
	 * @param bb
	 * @return this Idx, for method chaining.
	 */
	public Idx and(Idx aa, Idx bb) {
		a0 &= aa.a0 & bb.a0;
		a1 &= aa.a1 & bb.a1;
		a2 &= aa.a2 & bb.a2;
		return this;
	}

	/**
	 * Mutate this &= ~aa.
	 *
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
	 * Mutate this |= aa
	 *
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
	 *
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
	 * See: {@link diuf.sudoku.solver.hinters.color.Coloring#conjugate}
	 *
	 * @return the first (lowest) indice which has been removed from this Set,
	 *  else (this Set is empty) return -1.
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
			return BITS_PER_ELEMENT + Integer.numberOfTrailingZeros(x);
		}
		if ( a2 != 0 ) {
			x = a2 & -a2;	// lowestOneBit
			a2 &= ~x;		// remove x
			// 54 is 2 * Idx.BITS_PER_ELEMENT
			return BITS_TWO_ELEMENTS + Integer.numberOfTrailingZeros(x);
		}
		return -1; // this set is empty
	}

	/**
	 * returns contains, then add i; so you can visit each 'i' ONCE.
	 * @param i
	 * @return did this Idx already contain this 'i', coz it does now!
	 */
	boolean visit(final int i) {
		if ( i < BITS_PER_ELEMENT ) {
			if ( (a0 & SHFT[i]) != 0 )
				return false;
			a0 |= SHFT[i];
		} else if ( i < BITS_TWO_ELEMENTS ) {
			if ( (a1 & SHFT[i%BITS_PER_ELEMENT]) != 0 )
				return false;
			a1 |= SHFT[i%BITS_PER_ELEMENT];
		} else {
			if ( (a2 & SHFT[i%BITS_PER_ELEMENT]) != 0 )
				return false;
			a2 |= SHFT[i%BITS_PER_ELEMENT];
		}
		return true;
	}

	// -------------------------------- queries -------------------------------

	/**
	 * The peek method returns the first (lowest) indice in this Idx (without
	 * removing it) without having to initialise an array.
	 *
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
	 *
	 * @return true if this Idx contains no indices.
	 */
	public boolean none() {
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
	 * Return (this & aa) == this?
	 * ie is this Idx a subset of aa?
	 * ie is this all in aa?
	 * I found it pretty confusing, until I worked it out!
	 *
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
	 * <p>
	 * Note that I'm called "has" just for brevity, and formerly "contains".
	 *
	 * @param i
	 * @return true if this Idx contains the given indice.
	 */
	public boolean has(int i) {
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
	 * I do NOT mutate this Idx (remove then peek would mutate this Idx).
	 * <p>
	 * This method should only be called on an Idx containing two indices. This
	 * method is untested on Idx's containing other than two indices, so it's
	 * results should be regarded as "indeterminate", but here's what I think
	 * it "should" return:<ul>
	 * <li>When this Idx does NOT contain two indices -&gt; first other than i
	 * <li>When 'i' is not one of my indices -&gt; first
	 * </ul>
	 *
	 * @param i
	 * @return the other indice in this Idx
	 */
	public int otherThan(int i) {
		assert i>=0 && i<81;
		// take a local copy of this Idx's contents, to mutate
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
	 *
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
	 *
	 * @param array
	 * @return the number of elements added (ergo idx.size())
	 */
	public int toArrayN(int[] array) {
		try {
			return forEach((cnt, i) ->
					array[cnt] = i
			);
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
	 *
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
	 *
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

	public boolean[] toArrayBooleanNew() {
		final boolean[] result = new boolean[81];
		forEach((i)->result[i]=true);
		return result;
	}

	public ArrayList<Cell> toArrayListNew() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// You can't use IFilter on a primitive
	public static interface IndiceFilter {
		public boolean accept(int indice);
	}

	/**
	 * any matching indice?
	 *
	 * @return does this Idx contain any indice where f.accept(indice) returns
	 *  true?
	 */
	public boolean any(IndiceFilter f) {
		int bits, j;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if(f.accept(j+k)) return true;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if(f.accept(BITS_PER_ELEMENT+j+k)) return true;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if(f.accept(BITS_TWO_ELEMENTS+j+k)) return true;
		return false;
	}

	/**
	 * Returns a new array containing the indice in this Idx.
	 * <p>
	 * Prefer toArrayA or toArrayB in loops: if you call me in a loop then you
	 * are creating (potentially many) garbage arrays to keep the GC busy.
	 *
	 * @return
	 */
	public int[] toArrayNew() {
		// there is no need for a new 0-length array coz it's immutable.
		if ( none() )
			return IAS_A[0];
		return toArray(new int[size()]);
	}

	/**
	 * Called before and after the toArraySmart() loop; to reset my indexes.
	 * toArraySmart creates new arrays that are retained until all references
	 * to them are lost, so it's good practice to clean-up AFTER a toArraySmart
	 * loop, and we also do so beforehand just for safety (just be paranoid!).
	 */
	public void toArraySmartReset() {
		if ( usedA == null ) {
			usedA = new Idx();
			usedB = new Idx();
		} else {
			usedA.clear();
			usedB.clear();
		}
		if ( usedNew ) {
			usedNew = false;
		}
	}

	/**
	 * toArraySmart leases-out IAS_A[n], IAS_B[n], or a new int[n], such that
	 * there's one use of each array at any one time. Don't forget to Reset!
	 * <p>
	 * This cluster____ works-around Java taking ages to create a new int[].
	 * You call me in a loop: I return arrayA, else arrayB, else a new array;
	 * minimising garbage while still guaranteeing unique arrays, just do NOT
	 * use toArrayA or toArrayB (or anything else that uses AIS_A or IAS_B)
	 * while you're using my results, including any methods that you call.
	 * <p>
	 * You absolutely need to call {@link #toArraySmartReset} BEFORE your loop;
	 * and you can call it again afterwards to clean-up old indexes, but it's
	 * not required because my indexes are just Idx's, holding no references.
	 * <p>
	 * My public boolean {@code usedNew} is set to true if any new arrays are
	 * created during your "toArraySmart loop", which tells you that you need
	 * to forget all of my result-arrays (presuming they are held in a field)
	 * to prevent a memory leak.
	 *
	 * @return a unique int array, populated with the indices in this Idx.
	 */
	public int[] toArraySmart() {
		if ( (a0|a1|a2) == 0 ) // empty index
			return IAS_A[0]; // always returns an empty array
		final int n = size();
		if ( usedA.addOnly(n) )
			return toArrayA();
		if ( usedB.addOnly(n) )
			return toArrayB();
		usedNew = true;
		return toArrayNew();
	}
	// idx's of the size of arrayA's and arrayB's used.
	// each physical array may be used only once.
	private Idx usedA, usedB;
	// was a new array/s created in the previous toArraySmart session?
	public boolean usedNew;

	public String ids() {
		final StringBuilder sb = new StringBuilder(size()*3);
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
	 * I would nominate the dude/s who invented closures for a Nobel laurate,
	 * except I like pork.
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
		int bits // a0, a1, or a2: the element of 27 bits
		  , w // number of places to right-shift bits to get this 9-bit-word
		  , count=0; // the number of indices found so far
		if ( (bits=a0) != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(bits>>w)&WORD_MASK] )
					v.visit(count++, w+i);
		if ( (bits=a1) != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(bits>>w)&WORD_MASK] )
					v.visit(count++, BITS_PER_ELEMENT+w+i);
		if ( (bits=a2) != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(bits>>w)&WORD_MASK] )
					v.visit(count++, BITS_TWO_ELEMENTS+w+i);
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
	 * <p>
	 * Note that I can't use the "standard" IVisitor interface here because
	 * generics can't handle native types, and I won't wear the performance
	 * impost of boxing and unboxing to and from an Integer.
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
	 * forEach invokes {@code v.visit(indice)} for each indice in this Idx.
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

	public int forEach(Cell[] cells, IVisitor<Cell> v) {
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
	public void forEach(final Visitor1 first, final Visitor1 subsequent) {
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
	public Idx commonBuddies(final Idx result) {
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
	 * @param exceptThese after or-ing all buds should I buds.andNot(this)
	 * @return the buddies of cells in this idx (uncached).
	 */
	public Idx buds(boolean exceptThese) {
		if ( buds == null )
			buds = new Idx();
		else
			buds.clear();
		forEach((i)->buds.or(BUDDIES[i]));
		// remove me from buds, in case I contain cells which see each other.
		if ( exceptThese )
			buds.andNot(this);
		return buds;
	}
	private Idx buds;

	// --------------------------------- cells --------------------------------

	/**
	 * Get the cells from the given Grid at indices in this Idx.
	 * Use this one with the right sized array, which is returned.
	 *
	 * @param grid the Grid to read
	 * @param result the destination array to populate, which is presumed to
	 *  be large enough. You can size() the idx to get an array from the CAS,
	 *  or create a new array (if required), but if you're repeatedly getting
	 *  cells use cellsN which returns the number of cells added for use with
	 *  a single reusable array. Note that creating a bloody array takes longer
	 *  than populating it. I hate Java, but I Love it too. sigh.
	 * @return the given cells array, so create new if you must.
	 */
	public Cell[] cells(Grid grid, Cell[] result) {
		final Cell[] source = grid.cells;
		forEach((count, indice) -> result[count] = source[indice]);
		return result;
	}

	// cellsN is hammered by Aligned*Exclusion, so we create ONE instance of
	// CellsNVisitor, just to be more memory efficient, that's all.
	// package visible for the test-case
	private static final class CellsNVisitor implements Visitor2 {
		/** source cells to copy from */
		public Cell[] src;
		/** destination cells to copy into */
		public Cell[] dest;
		@Override
		public void visit(int count, int indice) {
			dest[count] = src[indice];
		}
	}
	// package visible for the test-case
	private static final CellsNVisitor CELLS_N_VISITOR = new CellsNVisitor();
	/**
	 * Get cells in Grid at indices in this Idx, and return how many. Use this
	 * one with a re-usable fixed-sized (atleast as large as required) array.
	 *
	 * @param grid the Grid to read
	 * @param result the array to populate with cells in this Idx.
	 * @return the number of indices visited, ie the size of this Idx
	 */
	public int cellsN(Grid grid, Cell[] result) {
		// NOTE: I tried native but the visitor is actually FASTER!
		final int count;
		try {
			CELLS_N_VISITOR.src = grid.cells;
			CELLS_N_VISITOR.dest = result;
			count = forEach(CELLS_N_VISITOR);
		} finally {
			// clean-up ALL Cell references, ya dummy!
			CELLS_N_VISITOR.src = CELLS_N_VISITOR.dest = null;
		}
		return count;
	}

	// cellsMaybes is hammered by AlignedExclusion, so we create ONE instance
	// of CellsMaybesVisitor, to be memory efficient. My two fields (the grid
	// and my maybes) are set by AlignedExclusion.prepare once per puzzle.
	public static final class CellsMaybesVisitor implements Visitor2 {
		public Grid grid;
		public int[] maybes;
		@Override
		public void visit(int count, int indice) {
			maybes[count] = grid.cells[indice].maybes;
		}
	}
	public static final CellsMaybesVisitor CELLS_MAYBES_VISITOR
			= new CellsMaybesVisitor();
	public int cellsMaybes() {
		return forEach(CELLS_MAYBES_VISITOR);
	}

	/**
	 * Get the cells from the given Grid at the indices in this Idx.
	 * <p>
	 * If you use CASA be sure clean-up with Cells.cleanCasA. Holding ONE Cell
	 * reference keeps the whole Grid in memory.
	 *
	 * @param grid the grid to read cells from
	 * @return a cached array from CASA of cells in this Idx, so clone to keep.
	 */
	public Cell[] cellsA(Grid grid) {
		return cells(grid, Cells.arrayA(size()));
	}

	/**
	 * Get the cells from the given Grid at the indices in this Idx.
	 * <p>
	 * Like cellsA but using a second CAS (CASB), so you can have a cellsB loop
	 * inside of a cellsA loop.
	 * <p>
	 * If you use CASB be sure clean-up with Cells.cleanCasB. Holding ONE Cell
	 * reference keeps the whole Grid in memory.
	 *
	 * @param grid the grid to read cells from
	 * @return a cached array from CASB of cells in this Idx, so clone to keep.
	 */
	public Cell[] cellsB(Grid grid) {
		return cells(grid, Cells.arrayB(size()));
	}

	// ----------------------------- stringy stuff ----------------------------

	/**
	 * Append space separated cell-IDs (SSV) of the given Idx(a0, a1, a2) to
	 * the given StringBuilder. First ensureCapacity of length + the given 'n'.
	 * <p>
	 * Each bitset uses 27 least significant (rightmost) bits, for 81 cells.
	 *
	 * @param sb to append to
	 * @param a0 the first bitset of the Idx
	 * @param a1 the second bitset of the Idx
	 * @param a2 the third bitset of the Idx
	 * @return the given StringBuilder for method chaining.
	 */
	public static StringBuilder append(StringBuilder sb, int a0, int a1, int a2) {
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
	 *
	 * @param a0 the first mask
	 * @param a1 the second mask
	 * @param a2 the third mask
	 * @return the cell-id's of indices in this Idx.
	 */
	public static String toStringCellIds(int a0, int a1, int a2) {
		return append(getSB(size(a0,a1,a2)*3), a0, a1, a2).toString();
	}

	/**
	 * For debugging.
	 *
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static String toStringIndices(int a0, int a1, int a2) {
		StringBuilder sb = getSB(size(a0,a1,a2) * 3);
		forEach(a0, a1, a2, (cnt, i) -> {
			if(cnt>0) sb.append(' ');
			sb.append(i); // the raw indice
		});
		return sb.toString();
	}
	public String toIndiceString() {
		return toStringIndices(a0, a1, a2);
	}

	/**
	 * Returns a String representation of this Idx.
	 *
	 * @return
	 */
	@Override
	public String toString() {
		if ( true ) // @check true
			// cell-ids toString for normal use
			return toStringCellIds(a0, a1, a2);
		else
			// debug only: the raw indices
			return toStringIndices(a0, a1, a2);
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
	 *
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

	public Idx where(final Cell[] cells, final IFilter<Cell> f) {
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

	public int cellsWhere(final Cell[] src, final Cell[] dest, final IFilter<Cell> f) {
		int bits, j, cnt=0;
		Cell c;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=src[j+k]) )
						dest[cnt++] = c;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=src[BITS_PER_ELEMENT+j+k]) )
						dest[cnt++] = c;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=src[BITS_TWO_ELEMENTS+j+k]) )
						dest[cnt++] = c;
		return cnt;
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
	 *
	 * @return
	 */
	@Override
	public int hashCode() {
		if ( hc == 0 )
			hc = hashCode(a0, a1, a2);
		return hc;
	}
	private int hc;

	public long hashCode64() {
		return hash64(a0, a1, a2);
	}

	/**
	 * Does this equal the Idx represented by the given binary masks.
	 *
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
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		return this==o || o instanceof Idx && equals((Idx)o);
	}

	/**
	 * Return does this == aa.
	 *
	 * @param aa
	 * @return
	 */
	public boolean equals(Idx aa) {
		return a0 == aa.a0
			&& a1 == aa.a1
			&& a2 == aa.a2;
	}

}
