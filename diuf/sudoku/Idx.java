/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.CELL_ID_LENGTH;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.utils.IFilter;
import diuf.sudoku.utils.IVisitor;
import diuf.sudoku.utils.IntFilter;
import diuf.sudoku.utils.IntIntVisitor;
import diuf.sudoku.utils.IntVisitor;
import diuf.sudoku.utils.Mutator;
import java.io.Serializable;
import static java.lang.Integer.bitCount;
import static java.lang.Integer.numberOfTrailingZeros;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static diuf.sudoku.IntArrays.iaLease;

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

//Idx MAX_SIZE=[3, 1, 1, 1, 3, 1, 3, 2, 3, 2, 3, 2, 3, 2, 2, 2, 2, 2, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
//	public static final int[] MAX_SIZE = new int[81];

	private static final long serialVersionUID = 342L;

//	/** my array (a) has 3 elements. */
//	public static final int ELEMENTS_PER_IDX = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_ELEMENT = 27;
	/** number of bits in two elements. */
	public static final int BITS_TWO_ELEMENTS = BITS_PER_ELEMENT<<1;
//	/** we use 3 * 9 bit "words" = 27 bits in each element. */
//	public static final int WORDS_PER_ELEMENT = 3;
	/** we use 3 * 9 bit "words" = 27 bits in each element. */
	public static final int BITS_PER_WORD = 9;
	/** the value of a full "word" (9 set bits). */
	public static final int WORD_MASK = 0x1FF; // Binary 1111, Decimal 15

	/** the value of ALL bits (ie 27 set bits) in an Idx element. */
	public static final int IDX_ALL = (1<<BITS_PER_ELEMENT)-1;

	/** THE empty int[]. */
	public static final int[] EMPTY_INT_ARRAY = new int[0];

	/** the left-shifted bitset value of each possible index. */
	public static final int[] IDX_SHFT = new int[BITS_PER_ELEMENT];

	/** The number of combinations possible in 9 bits: 512. */
	public static final int WORDS_SIZE = 1<<9; // 512
	/** WORDS[9-bit-word] contains the indexes that are in this 9-bit-word.
	 * There are 3 words per 27-bit element. The size of each sub-array is
	 * variable 0..9, depending on the number of set (1) bits in my "word",
	 * ie my index. Sigh. */
	public static final int[][] WORDS = new int[WORDS_SIZE][];

	static {
		for ( int i=0,n=IDX_SHFT.length; i<n; ++i )
			IDX_SHFT[i] = 1<<i;
		for ( int i=0; i<WORDS.length; ++i )
			WORDS[i] = Indexes.toValuesArrayNew(i);
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
	 * Returns a new Idx of 'cells'.
	 *
	 * @param cells to add
	 * @return a new Idx of 'cells'
	 */
	public static Idx of(final Cell[] cells) {
		return of(cells, cells.length);
	}

	/**
	 * Returns a new Idx of the first 'n' 'cells'.
	 *
	 * @param cells to add
	 * @param n how many cells to add, so you can reuse a fixed-size array
	 * @return a new Idx of the first 'n' 'cells'
	 */
	public static Idx of(final Cell[] cells, final int n) {
		final Idx result = new Idx();
		for ( int i=0; i<n; ++i )
			result.add(cells[i].i);
		return result;
	}

	/**
	 * Set this Idx to indices of cells at indexes in the given cells array.
	 * nb: 'indexes' contains indexes in 'cells', not indices in Grid.cells.
	 * I do that conversion.
	 *
	 * @param cells
	 * @param indexes
	 * @return this Idx, for method chaining.
	 */
	public static Idx of(final Cell[] cells, final int[] indexes) {
		final Idx result = new Idx();
		for ( int i : indexes )
			result.add(cells[i].i);
		return result;
	}

	/**
	 * Returns a new Idx of 'n' cells at 'indexes' in 'cells'.
	 *
	 * @param cells to read
	 * @param indexes array of indexes in the cells array
	 * @param n number of indexes in indexes (for reusable array)
	 * @return a new Idx
	 */
	public static Idx of(final Cell[] cells, final int[] indexes, final int n) {
		final Idx result = new Idx();
		for ( int i=0; i<n; ++i )
			result.add(cells[indexes[i]].i);
		return result;
	}

	/**
	 * Returns a new Idx containing indices.
	 * <p>
	 * Overridden by IdxL!
	 *
	 * @param indices
	 * @return
	 */
	public static Idx of(final int[] indices) {
		int a0=0, a1=0, a2=0;
		for ( int i : indices )
			if ( i < BITS_PER_ELEMENT )
				a0 |= IDX_SHFT[i];
			else if ( i < BITS_TWO_ELEMENTS )
				a1 |= IDX_SHFT[i%BITS_PER_ELEMENT];
			else
				a2 |= IDX_SHFT[i%BITS_PER_ELEMENT];
		return new Idx(a0, a1, a2);
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
	static final Idx of(final String[] ids) {
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
		return bitCount(a0)+bitCount(a1)+bitCount(a2) <= max;
	}

	/**
	 * Return the cardinality of the "virtual Idx" a0, a1, a2.
	 * <p>
	 * NB: Uses bitCount, coz that's faster than initialising.
	 *
	 * @param a0
	 * @param a1
	 * @param a2
	 * @return the number of set (1) bits in the Idx
	 */
	public static final int size(final int a0, final int a1, final int a2) {
		return bitCount(a0)+bitCount(a1)+bitCount(a2);
	}

	/**
	 * Parse an Idx from CSV (comma separated values) of Cell.id's.
	 * <p>
	 * For example: "E3, H3, I3, H8, I8, E6, I6"
	 *
	 * @param csv a comma separated values String of Cell.id's.
	 * @return a new Idx.
	 */
	public static final Idx ofCsv(final String csv) {
		final Idx result = new Idx();
		if ( csv!=null && csv.length()>1 ) {
			final Pattern p = Pattern.compile("[A-I][1-9]");
			for ( final String id : csv.split(" *, *") )
				if ( p.matcher(id).matches() )
					result.add(Grid.indice(id));
		}
		return result;
	}

	/**
	 * Parse an Idx from SSV (space separated values) of Cell.id's,
	 * for use in the test-cases only.
	 * <p>
	 * For example: "E3 H3 I3 H8 I8 E6 I6"
	 * <p>
	 * Note that this is Idx's standard toString format!
	 * What was I thinking with CSV and not SSV? I'm a putz!
	 *
	 * @param ssv space separated values (Cell.id's).
	 * @return a new Idx.
	 */
	public static final Idx ofSsv(final String ssv) {
		if ( ssv.isEmpty() ) // throws NPE if null
			throw new IllegalArgumentException("empty ssv");
		final Idx result = new Idx();
		final Pattern p = Pattern.compile("[A-I][1-9]");
		for ( final String id : ssv.split(" +") )
			if ( p.matcher(id).matches() )
				result.add(Grid.indice(id));
			else
				throw new IllegalArgumentException("bad ssv: "+ssv);
		return result;
	}

	// Design Note: Home Boy is static: These "static query" methods have loose
	// euphamistic names to differentiate them from "instance queries", with
	// nice tight logic-based techie-speak type-names that actually make sense.

	/**
	 * Returns (a & b) != 0.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static final boolean intersect(final Idx a, final Idx b) {
		return ( (a.a0 & b.a0)
			   | (a.a1 & b.a1)
			   | (a.a2 & b.a2) ) != 0;
	}

	/**
	 * Returns (a & b) == 0.
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static final boolean disjunct(final Idx a, final Idx b) {
		return (a.a0 & b.a0) == 0
			&& (a.a1 & b.a1) == 0
			&& (a.a2 & b.a2) == 0;
	}

	/**
	 * Constructs a new Idx(a & b).
	 * @param a
	 * @param b
	 * @return
	 */
	public static final Idx newAnd(final Idx a, final Idx b) {
		return new Idx(a.a0&b.a0, a.a1&b.a1, a.a2&b.a2);
	}

	/**
	 * Constructs a new Idx(a | b).
	 * @param a
	 * @param b
	 * @return
	 */
	public static final Idx newOr(final Idx a, final Idx b) {
		return new Idx(a.a0|b.a0, a.a1|b.a1, a.a2|b.a2);
	}

	/**
	 * Returns a lease over an array containing the indices in the virtual
	 * Idx(a0,a1,a2) without creating the Idx or even (usually) a new array.
	 * So double-down on your KY order: McKraken is flappin.
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @return a Lease on a cached array of indices in the given Idx
	 */
	public static final IALease toArrayLease(final int a0, final int a1, final int a2) {
		final int n = bitCount(a0)+bitCount(a1)+bitCount(a2);
		final IALease lease = iaLease(n);
		final int[] a = lease.array;
		forEach(a0,a1,a2, (i, e) -> a[i] = e); // index, element
		return lease;
	}

	/**
	 * Static forEach invokes {@code v.visit(index, indice)} for each cell in
	 * the virtual Idx(a0,a1,a2); where index is 0-based number visited so far,
	 * and indice of each cell in the virtual Idx.
	 * <p>
	 * I'm a bit proud of this. It allows a single implementation of the indice
	 * finder to do whatever with the indices. It's clever, as are closures
	 * (lambda expressions) used to implement visitor succinctly.
	 *
	 * @param a0 Idx element 0: 27 used bits
	 * @param a1 Idx element 1: 27 used bits
	 * @param a2 Idx element 2: 27 used bits
	 * @param v visitor is invoked with index (0-based how many),
	 *  and indice (of this cell in the grid)
	 * @return the number of indices visited, ie the end count.
	 */
	public static final int forEach(final int a0, final int a1, final int a2, final IntIntVisitor v) {
		int ka[], j, kn,ki, i = 0; // ALL vars, for one stackframe
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a0>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, j+ka[ki]);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a1>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, BITS_PER_ELEMENT+j+ka[ki]);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a2>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, BITS_TWO_ELEMENTS+j+ka[ki]);
		return i;
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
	public static final void forEach(final int a0, final int a1, final int a2, final IntVisitor v) {
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
	 * @param a to copy
	 */
	public Idx(final Idx a) {
		a0 = a.a0;
		a1 = a.a1;
		a2 = a.a2;
	}

	/**
	 * Constructs a new Idx containing the given 'bitsets': a0, a1, a2.
	 * <p>
	 * This constructor is currently only used by LinkedMatrixCellSet, of which
	 * there are two: in align (to be replaced) and align2 (the replacement).
	 *
	 * @param bitsets a0, a1, a2.
	 */
	public Idx(final int[] bitsets) {
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
	public Idx(final boolean isFull) {
		if ( isFull )
			a0=a1=a2=IDX_ALL; // 27 1's
	}

	/**
	 * Constructor from raw array values.
	 * @param a0
	 * @param a1
	 * @param a2
	 */
	public Idx(final int a0, final int a1, final int a2) {
		this.a0 = a0;
		this.a1 = a1;
		this.a2 = a2;
	}

	/**
	 * Constructor from cells array.
	 *
	 * @param cells to add to this Idx
	 */
	public Idx(Cell[] cells) {
		for ( Cell c : cells )
			add(c.i);
	}

	/**
	 * Constructs a new Idx containing the complement of this Idx.
	 *
	 * @return a new Idx of all EXCEPT this Idx.
	 */
	Idx not() {
		return new Idx(IDX_ALL & ~a0, IDX_ALL & ~a1, IDX_ALL & ~a2);
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
	 * Add 'cells' which match 'f' into this Idx.
	 *
	 * @param cells to read (usually Grid.cells)
	 * @param f the filter which accept's cells to be added to this Idx
	 * @return this Idx for method chaining
	 */
	@Mutator
	public Idx read(final Cell[] cells, final IFilter<Cell> f) {
		for ( final Cell c : cells )
			if ( f.accept(c) )
				add(c.i);
		return this;
	}

	/**
	 * Set this = a.
	 *
	 * @param a
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final Idx a) {
		a0 = a.a0;
		a1 = a.a1;
		a2 = a.a2;
		return this;
	}

	/**
	 * Set this = trues in b.
	 *
	 * @param bits an array of 81 booleans
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final boolean[] bits) {
		int i = 0;
		for ( ; i<27; ++i )
			if ( bits[i] )
				a0 |= IDX_SHFT[i];
		for ( ; i<54; ++i )
			if ( bits[i] )
				a1 |= IDX_SHFT[i%BITS_PER_ELEMENT];
		for ( ; i<81; ++i )
			if ( bits[i] )
				a2 |= IDX_SHFT[i%BITS_PER_ELEMENT];
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
	@Mutator
	public void setNullSafe(final Idx src) {
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
	@Mutator
	public Idx set(final int a0, final int a1, final int a2) {
		this.a0 = a0;
		this.a1 = a1;
		this.a2 = a2;
		return this;
	}

	/**
	 * Set this Idx to the indices of the given cells.
	 *
	 * @param cells to add
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final Cell[] cells) {
		return set(cells, cells.length);
	}

	/**
	 * Set this Idx to the indices of the given cells.
	 *
	 * @param cells to add
	 * @param n the number of cells in cells
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final Cell[] cells, final int n) {
		a0 = a1 = a2 = 0;
		for ( int i=0; i<n; ++i )
			add(cells[i].i);
		return this;
	}

	/**
	 * Set this Idx to the indices of the given cells.
	 *
	 * @param indices to add
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final int[] indices) {
		a0 = a1 = a2 = 0;
		for ( int i : indices )
			add(i);
		return this;
	}

	/**
	 * Set this = (a0,a1,a2) and return any.
	 *
	 * @param a0 the mask of the first 27 indices in the Idx
	 * @param a1 the mask of the second 27 indices in the Idx
	 * @param a2 the mask of the third 27 indices in the Idx
	 * @return not empty
	 */
	@Mutator
	public boolean setAny(final int a0, final int a1, final int a2) {
		this.a0 = a0;
		this.a1 = a1;
		this.a2 = a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (a & b).
	 *
	 * @param a
	 * @param b
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAnd(final Idx a, final Idx b) {
		a0 = a.a0 & b.a0;
		a1 = a.a1 & b.a1;
		a2 = a.a2 & b.a2;
		return this;
	}

	/**
	 * Set this = (a & b) and returns any.
	 *
	 * @param a
	 * @param b
	 * @return not empty
	 */
	@Mutator
	public boolean setAndAny(final Idx a, final Idx b) {
		a0 = a.a0 & b.a0;
		a1 = a.a1 & b.a1;
		a2 = a.a2 & b.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (a & b & c) and returns any.
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @return not empty
	 */
	@Mutator
	public boolean setAndAny(final Idx a, final Idx b, final Idx c) {
		a0 = a.a0 & b.a0 & c.a0;
		a1 = a.a1 & b.a1 & c.a1;
		a2 = a.a2 & b.a2 & c.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = (a & b) and returns size() > 1.
	 *
	 * @param a
	 * @param b
	 * @return 2 or more
	 */
	@Mutator
	public boolean setAndMany(final Idx a, final Idx b) {
		a0 = a.a0 & b.a0;
		a1 = a.a1 & b.a1;
		a2 = a.a2 & b.a2;
		return (a0|a1|a2)!=0 && size()>1;
	}

	/**
	 * Set this = (a & b) and returns size()>=min.
	 *
	 * @param a
	 * @param b
	 * @param min
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public boolean setAndMin(final Idx a, final Idx b, final int min) {
		a0 = a.a0 & b.a0;
		a1 = a.a1 & b.a1;
		a2 = a.a2 & b.a2;
		return (a0|a1|a2)!=0 && size()>=min;
	}

	/**
	 * Set this = src then remove indice and return self.
	 *
	 * @param src the source Idx
	 * @param indice to be removed
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setExcept(final Idx src, final int indice) {
		set(src);
		remove(indice);
		return this;
	}

	/**
	 * Set this = ALL & ~excluded.
	 *
	 * @param excluded indices of cells to be excluded
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAllExcept(final Idx excluded) {
		a0 = IDX_ALL & ~excluded.a0;
		a1 = IDX_ALL & ~excluded.a1;
		a2 = IDX_ALL & ~excluded.a2;
		return this;
	}

	/**
	 * Set this = (a | b).
	 *
	 * @param a
	 * @param b
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setOr(final Idx a, final Idx b) {
		a0 = a.a0 | b.a0;
		a1 = a.a1 | b.a1;
		a2 = a.a2 | b.a2;
		return this;
	}

	/**
	 * Set this = a & ~b.
	 *
	 * @param a
	 * @param b
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAndNot(final Idx a, final Idx b) {
		a0 = a.a0 & ~b.a0;
		a1 = a.a1 & ~b.a1;
		a2 = a.a2 & ~b.a2;
		return this;
	}

	/**
	 * Set this = a & ~b & ~c.
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAndNot(final Idx a, final Idx b, final Idx c) {
		a0 = a.a0 & ~b.a0 & ~c.a0;
		a1 = a.a1 & ~b.a1 & ~c.a1;
		a2 = a.a2 & ~b.a2 & ~c.a2;
		return this;
	}

	/**
	 * Set this = a & ~b, and return any()
	 *
	 * @param a
	 * @param b
	 * @return are there any indices in this Idx
	 */
	@Mutator
	public boolean setAndNotAny(final Idx a, final Idx b) {
		a0 = a.a0 & ~b.a0;
		a1 = a.a1 & ~b.a1;
		a2 = a.a2 & ~b.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Set this = a & ~b & ~c, and return any()
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @return are there any indices in this Idx
	 */
	@Mutator
	public boolean setAndNotAny(final Idx a, final Idx b, final Idx c) {
		a0 = a.a0 & ~b.a0 & ~c.a0;
		a1 = a.a1 & ~b.a1 & ~c.a1;
		a2 = a.a2 & ~b.a2 & ~c.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Clears this Idx, ie removes all indices.
	 *
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx clear() {
		a0 = a1 = a2 = 0;
		return this;
	}

	/**
	 * Add the given indice 'i' to this Idx.
	 *
	 * @param i
	 */
	@Mutator
	public void add(final int i) {
		assert i>=0 && i<81;
		if ( i < BITS_PER_ELEMENT )
			a0 |= IDX_SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 |= IDX_SHFT[i%BITS_PER_ELEMENT];
		else
			a2 |= IDX_SHFT[i%BITS_PER_ELEMENT];
	}

//not_used
//	/**
//	 * Add the given indice 'i' to this Idx, and return this
//	 *
//	 * @param i
//	 * @return this Idx for method chaining
//	 */
//	@Mutator
//	public Idx add2(final int i) {
//		add(i);
//		return this;
//	}

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
	@Mutator
	public boolean addOnly(final int i) {
		assert i>=0 && i<81;
		if ( i < BITS_PER_ELEMENT ) {
			if ( (a0 & IDX_SHFT[i]) != 0 )
				return false;
			a0 |= IDX_SHFT[i];
		} else if ( i < BITS_TWO_ELEMENTS ) {
			if ( (a1 & IDX_SHFT[i-BITS_PER_ELEMENT]) != 0 )
				return false;
			a1 |= IDX_SHFT[i-BITS_PER_ELEMENT];
		} else {
			if ( (a2 & IDX_SHFT[i-BITS_TWO_ELEMENTS]) != 0 )
				return false;
			a2 |= IDX_SHFT[i-BITS_TWO_ELEMENTS];
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
	@Mutator
	public Idx addAll(final Cell[] cells) {
		for ( Cell c : cells )
			add(c.i);
		return this;
	}

	/**
	 * Remove the given indice 'i' from this Idx.
	 *
	 * @param i the indice to be removed from this Idx
	 */
	@Mutator
	public void remove(final int i) {
		if ( i < BITS_PER_ELEMENT )
			a0 &= ~IDX_SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			a1 &= ~IDX_SHFT[i-BITS_PER_ELEMENT];
		else
			a2 &= ~IDX_SHFT[i-BITS_TWO_ELEMENTS];
	}

//not_used
//	/**
//	 * Remove the given indice 'i' from this Idx, and return this Idx.
//	 *
//	 * @param i the indice to be removed from this Idx
//	 * @return this Idx for method chaining
//	 */
//	@Mutator
//	public Idx remove2(final int i) {
//		remove(i);
//		return this;
//	}

	/**
	 * Remove all of the given cells indices from this Set.
	 *
	 * @param cells
	 */
	@Mutator
	public void removeAll(final Iterable<Cell> cells) {
		for ( Cell c : cells )
			remove(c.i);
	}

	/**
	 * Remove all of the given cells indices from this Set.
	 *
	 * @param cells
	 * @return this Idx for method chaining
	 */
	@Mutator
	public Idx removeAll(final Cell[] cells) {
		for ( Cell c : cells )
			remove(c.i);
		return this;
	}

	/**
	 * Mutate this &= a.
	 *
	 * @param a
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx and(final Idx a) {
		a0 &= a.a0;
		a1 &= a.a1;
		a2 &= a.a2;
		return this;
	}

	/**
	 * Mutate this &= a returning any.
	 *
	 * @param a
	 * @return does this Idx contain any indices.
	 */
	@Mutator
	public boolean andAny(final Idx a) {
		a0 &= a.a0;
		a1 &= a.a1;
		a2 &= a.a2;
		return (a0|a1|a2) != 0;
	}

	/**
	 * Mutate this = this & a & b.
	 * Two and's, no waiting.
	 * @param a
	 * @param b
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx and(final Idx a, final Idx b) {
		a0 &= a.a0 & b.a0;
		a1 &= a.a1 & b.a1;
		a2 &= a.a2 & b.a2;
		return this;
	}

	/**
	 * Mutate this &= ~a.
	 *
	 * @param a
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx andNot(final Idx a) {
		a0 &= ~a.a0;
		a1 &= ~a.a1;
		a2 &= ~a.a2;
		return this;
	}

	/**
	 * Mutate this |= a
	 *
	 * @param a
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx or(final Idx a) {
		a0 |= a.a0;
		a1 |= a.a1;
		a2 |= a.a2;
		return this;
	}

	/**
	 * Mutate this |= (a & b & c)
	 * Add the intersection of the given sets to this set.
	 *
	 * @param a
	 * @param b
	 * @param c
	 */
	@Mutator
	public void orAnd(final Idx a, final Idx b, final Idx c) {
		a0 |= (a.a0 & b.a0 & c.a0);
		a1 |= (a.a1 & b.a1 & c.a1);
		a2 |= (a.a2 & b.a2 & c.a2);
	}

	// ----------------------- mutators that're queries -----------------------

	/**
	 * If 'i' is already in this Idx then I return false,
	 * else 'i' is added, and I return true;
	 * to visit each 'i' ONCE.
	 * <p>
	 * Basically, I'm an addIfNotExists method, returning was it added.
	 * I'm disproportionately proud of inventing this method.
	 * But I'm sure others must have done likewise before me.
	 *
	 * @param i in 0..80, typically a cell indice
	 * @return did this Idx NOT already contain this 'i', coz it does now!
	 */
	@Mutator
	boolean visit(final int i) {
		if ( i < BITS_PER_ELEMENT ) {
			if ( (a0 & IDX_SHFT[i]) != 0 )
				return false;
			a0 |= IDX_SHFT[i];
		} else if ( i < BITS_TWO_ELEMENTS ) {
			if ( (a1 & IDX_SHFT[i%BITS_PER_ELEMENT]) != 0 )
				return false;
			a1 |= IDX_SHFT[i%BITS_PER_ELEMENT];
		} else {
			if ( (a2 & IDX_SHFT[i%BITS_PER_ELEMENT]) != 0 )
				return false;
			a2 |= IDX_SHFT[i%BITS_PER_ELEMENT];
		}
		return true;
	}

	/**
	 * Remove and return the first (lowest) indice in this Set.
	 * <p>
	 * The poll() method is my invention. It's intent is to be fast, not just
	 * here, but also in the calling loop.
	 *
	 * @return the first (lowest) indice that is removed from this Set,
	 *  else (this Set is empty) return -1.
	 */
	@Mutator
	public int poll() {
		int x;
		if ( a0 != 0 ) {
			x = a0 & -a0;	// lowestOneBit
			a0 &= ~x;		// remove x
			return numberOfTrailingZeros(x);
		}
		if ( a1 != 0 ) {
			x = a1 & -a1;	// lowestOneBit
			a1 &= ~x;		// remove x
			return BITS_PER_ELEMENT + numberOfTrailingZeros(x);
		}
		if ( a2 != 0 ) {
			x = a2 & -a2;	// lowestOneBit
			a2 &= ~x;		// remove x
			// 54 is 2 * Idx.BITS_PER_ELEMENT
			return BITS_TWO_ELEMENTS + numberOfTrailingZeros(x);
		}
		return -1; // this set is empty
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
			return numberOfTrailingZeros(a0);
		if ( a1 != 0 )
			return BITS_PER_ELEMENT + numberOfTrailingZeros(a1);
		if ( a2 != 0 )
			return BITS_TWO_ELEMENTS + numberOfTrailingZeros(a2);
		return -1;
	}

	/**
	 * Return is this Idx empty?
	 *
	 * @return does this Idx contain no indices.
	 */
	public boolean none() {
		return (a0|a1|a2) == 0;
	}

	/**
	 * Return is this Idx NOT empty?
	 *
	 * @return does this Idx contain any indices.
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
	 * @return {@code bitCount(a0)+bitCount(a1)+bitCount(a2)}
	 */
	public int size() {
		return bitCount(a0)+bitCount(a1)+bitCount(a2);
	}

	/**
	 * Returns (this & a).any(), ie does this Idx intersect a.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(a).any()}.
	 *
	 * @param a
	 * @return is the intersection of this and other NOT empty.
	 */
	public boolean intersects(final Idx a) {
		return ( (a0 & a.a0) | (a1 & a.a1) | (a2 & a.a2) ) != 0;
	}

	/**
	 * Returns (this & a).none(), ie does this Idx NOT intersect a.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(a).none()}.
	 *
	 * @param a
	 * @return is the intersection of this and other empty.
	 */
	public boolean disjunct(final Idx a) {
		return ( (a0 & a.a0) | (a1 & a.a1) | (a2 & a.a2) ) == 0;
	}

	/**
	 * Does this Idx contain the given indice 'i'?
	 * <p>
	 * I'm called "has" for brevity, formerly "contains".
	 *
	 * @param i
	 * @return true if this Idx contains the given indice.
	 */
	public boolean has(final int i) {
		if ( i < BITS_PER_ELEMENT )
			return (a0 & IDX_SHFT[i]) != 0;
		else if ( i < BITS_TWO_ELEMENTS )
			return (a1 & IDX_SHFT[i%BITS_PER_ELEMENT]) != 0;
		return (a2 & IDX_SHFT[i%BITS_TWO_ELEMENTS]) != 0;
	}

	/**
	 * Return (this & a) == a?
	 * ie is 'a' a subset of this?
	 * or is this a superset of 'a'?
	 *
	 * @param a the other Idx
	 * @return does other Idx contain all indices in this Idx?
	 */
	public boolean hasAll(final Idx a) {
		return (a0 & a.a0) == a.a0
			&& (a1 & a.a1) == a.a1
			&& (a2 & a.a2) == a.a2;
	}

	/**
	 * Return (this & a) == this? <br>
	 * ie is this a subset of 'a'?
	 *
	 * @param a the other Idx
	 * @return does this Idx contain all indices in the other Idx?
	 */
	public boolean allIn(final Idx a) {
		return (a0 & a.a0) == a0
			&& (a1 & a.a1) == a1
			&& (a2 & a.a2) == a2;
	}

	/**
	 * Returns (this & a) == this || (this & a) == a. <br>
	 * ie is this a superset or a subset of a? <br>
	 * ergo does either set wholey contain the other?
	 *
	 * @param a the other Idx
	 * @return does either set wholey contain the other?
	 */
	public boolean inbread(final Idx a) {
		final int m0 = a0 & a.a0
				, m1 = a1 & a.a1
				, m2 = a2 & a.a2;
		return (m0==a0 && m1==a1 && m2==a2)
			|| (m0==a.a0 && m1==a.a1 && m2==a.a2);
	}

	/**
	 * When this Idx contains two indices, I return the one other than 'i'.
	 * <p>
	 * I do NOT mutate this Idx (remove then peek would mutate this Idx).
	 * <p>
	 * This method should only be called on an Idx containing two indices. I'm
	 * untested on Idx's containing other than two indices, but here's what I
	 * think it returns:<ul>
	 * <li>When I contain ONE indice -&gt; first other than i, or 32 when 'i'
	 *  is my only indice.
	 * <li>When I contain more than two indices -&gt; first other than i
	 * <li>When 'i' is not one of my indices -&gt; first
	 * <li>These are all untested!
	 * </ul>
	 *
	 * @param i
	 * @return the other indice in this Idx
	 */
	public int otherThan(final int i) {
		assert i>-1 && i<81;
		// a mutable local copy of this Idx
		int m0=this.a0, m1=this.a1, m2=this.a2;
		// remove i
		if ( i < BITS_PER_ELEMENT )
			m0 &= ~IDX_SHFT[i];
		else if ( i < BITS_TWO_ELEMENTS )
			m1 &= ~IDX_SHFT[i%BITS_PER_ELEMENT];
		else
			m2 &= ~IDX_SHFT[i%BITS_PER_ELEMENT];
		// peek
		if ( m0 != 0 )
			return numberOfTrailingZeros(m0);
		if ( m1 != 0 )
			return BITS_PER_ELEMENT + numberOfTrailingZeros(m1);
		if ( m2 != 0 )
			return BITS_TWO_ELEMENTS + numberOfTrailingZeros(m2);
		// found none
		return -1;
	}

	// ----------------------------- toArray -----------------------------

	/**
	 * Returns the given array (which is presumed to be of sufficient size)
	 * populated with the indices in this Idx.
	 * <p>
	 * Note that the array is NOT cleared first. It's presumed new or empty;
	 * or you want to add to the existing contents.
	 *
	 * @param result
	 * @return the given array, for method chaining.
	 */
	public int[] toArray(final int[] result) {
		forEach((cnt, i) -> result[cnt] = i);
		return result;
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
		if ( (a0|a1|a2) == 0 )
			return EMPTY_INT_ARRAY;
		return toArray(new int[size()]);
	}

	/**
	 * Read indices in this Idx into the given array, and return how many, so
	 * that you can use a re-usable (fixed-size) array.
	 *
	 * @param array
	 * @return the number of elements added (ergo idx.size())
	 */
	public int toArrayN(final int[] array) {
		try {
			return forEach((cnt, i) -> array[cnt] = i);
		} catch ( ArrayIndexOutOfBoundsException ex ) {
			return array.length;
		}
	}

	public boolean[] toArrayBooleanNew() {
		final boolean[] result = new boolean[81];
		forEach((i)->result[i]=true);
		return result;
	}

	// ----------------------------- to Set -----------------------------

	public Set<Cell> toCellSet(final Cell[] cells) {
		final Set<Cell> result = new LinkedHashSet<>(size(), 1F);
		forEach((i) -> result.add(cells[i]));
		return result;
	}

	// ---------------------- a leased array of indices -----------------------

	public IALease toArrayLease() {
		final int n = size();
		final IALease lease = iaLease(n);
		if ( n > 0 )
			toArray(lease.array);
		return lease;
	}

	// ------------------------------- visitors -------------------------------

	// NB: Previously each of these forEach methods called it's static
	// implementation, but repeating the code here is a bit faster.
	//
	// Invoking the static impl's costs about a second in top1465, which is
	// three-parts of eff-all really, but in my racing brain that's kenages!
	//
	// Downside: If the visitor modifies this Idx during iteration then hitting
	// each indice is indeterminate: it takes changes to subsequent words, but
	// ignores changes to this-and-previous words. The static impl's copy this
	// Idx BEFORE iterating to obviate da whole concurrent modification problem
	// completely: the iteration simply doesn't "see" any changes.
	//
	// If you're doing this sort of thing for-reals, just call the static impl!
	// This is adequate for solving Sudokus, but not for ICBM guidance systems.

	/**
	 * This forEach facilitates adding each cell in this Idx to an array.
	 * It invokes v.visit for each indice in this Idx passing:<ul>
	 * <li>index: 0-based number of indices visited so far.
	 * <li>indice: of this cell in Grid.cells.
	 * </ul>
	 *
	 * @param v an implementation of IntIntVisitor, typically a lambda
	 * @return total number of indices visited, ie the size of this Idx
	 */
	public int forEach(final IntIntVisitor v) {
//		if ( (a0|a1|a2) == 0 )
//			return;
		assert (a0|a1|a2) != 0;
		int ka[], j, kn,ki, i = 0; // ALL vars, for one stackframe
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a0>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, j+ka[ki]);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a1>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, BITS_PER_ELEMENT+j+ka[ki]);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a2>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(i++, BITS_TWO_ELEMENTS+j+ka[ki]);
		return i;
	}

	/**
	 * forEach invokes {@code v.visit(indice)} for each indice in this Idx.
	 *
	 * @param v your implementation of Visitor1 does whatever with indice
	 */
	public void forEach(final IntVisitor v) {
//		if ( (a0|a1|a2) == 0 )
//			return;
		assert (a0|a1|a2) != 0;
		int ka[], j, kn,ki; // ALL vars, for one stackframe
		if ( a0 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a0>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(j+ka[ki]);
		if ( a1 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a1>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(BITS_PER_ELEMENT+j+ka[ki]);
		if ( a2 != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( ka=WORDS[(a2>>j)&WORD_MASK],kn=ka.length,ki=0; ki<kn; ++ki )
					v.visit(BITS_TWO_ELEMENTS+j+ka[ki]);
	}

	/**
	 * doWhile invokes {@code f.accept(indice)} for each indice in this Idx,
	 * stopping as soon as accept returns false.
	 * <p>
	 * I use doWhile to stop and-ing when my result Idx is empty.
	 *
	 * @param f your implementation of IntFilter does whatever with the given
	 *  indice to determine if it's accepted, and if not I exit early.
	 * @return the return value of the last f.accept call. The return value is
	 *  intended to be "any remaining", but this depends on your IntFilter.
	 */
	public boolean doWhile(final IntFilter f) {
		if ( (a0|a1|a2) == 0 )
			return false;
		int bits, j;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( !f.accept(j+k) )
						return false;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( !f.accept(BITS_PER_ELEMENT+j+k) )
						return false;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( !f.accept(BITS_TWO_ELEMENTS+j+k) )
						return false;
		return true;
	}

	public int forEach(final Cell[] gridCells, final IVisitor<Cell> v) {
		int bits, j, cnt = 0;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(gridCells[j+k]);
					++cnt;
				}
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(gridCells[BITS_PER_ELEMENT+j+k]);
					++cnt;
				}
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] ) {
					v.visit(gridCells[BITS_TWO_ELEMENTS+j+k]);
					++cnt;
				}
		return cnt;
	}

//not_used 2021-12-17
//	/**
//	 * any matching indice?
//	 *
//	 * @param f
//	 * @return does this Idx contain any indice where f.accept(indice) returns
//	 *  true?
//	 */
//	public boolean any(final IntFilter f) {
//		int bits, j;
//		if ( (bits=a0) != 0 )
//			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
//				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
//					if ( f.accept(j+k) )
//						return true;
//		if ( (bits=a1) != 0 )
//			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
//				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
//					if ( f.accept(BITS_PER_ELEMENT+j+k) )
//						return true;
//		if ( (bits=a2) != 0 )
//			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
//				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
//					if ( f.accept(BITS_TWO_ELEMENTS+j+k) )
//						return true;
//		return false;
//	}

	// ---------------------------- common buddies ----------------------------

	/**
	 * This forEach invokes 'first' for the first indice in this Idx, and then
	 * 'subsequent' for each subsequent indice in this Idx.
	 * <p>
	 * It's intended for use by the commonBuddies method, but there's no
	 * reason to not use it elsewhere.
	 *
	 * @param first Visitor1 invoked for the first indice;
	 *  where commonBuddies does buds.set(BUDDIES[i])
	 * @param subsequent Visitor1 invoked for each subsequent indice;
	 *  where commonBuddies does buds.and(BUDDIES[i])
	 */
	public void forEach(final IntVisitor first, final IntVisitor subsequent) {
		IntVisitor v = first;
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

	// -------------------------------- where --------------------------------

	/**
	 * Returns a new Idx containing cells in this Idx that are accept'ed by
	 * this {@code IFilter<Cell>}.
	 *
	 * @param cells grid.cells to pass to filter.accept
	 * @param f the filter is usually a lambda expression which returns true
	 *  if the given cell should be included in the result
	 * @return a new Idx containing accepted members of this Idx.
	 */
	public Idx where(final Cell[] cells, final IFilter<Cell> f) {
		int bits, j, r0=0,r1=0,r2=0; // the result
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[j+k]) )
						r0 |= IDX_SHFT[j+k];
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[BITS_PER_ELEMENT+j+k]) )
						r1 |= IDX_SHFT[j+k];
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(cells[BITS_TWO_ELEMENTS+j+k]) )
						r2 |= IDX_SHFT[j+k];
		return new Idx(r0,r1,r2);
	}

	/**
	 * Returns a new Idx containing indices in this Idx that are accept'ed by
	 * the given {@code IntFilter}.
	 * <p>
	 * This method is a way of filtering an Idx. It separates the forEach code
	 * from the filter-code. I think it's quite clever, but I'm mad on foreach,
	 * I just wish it could be done generically WITHOUT sacrificing speed. If
	 * you toArray everything you wear creating an array, or pissing about with
	 * a cache, or you foreach, and every foreach has to have it's own foreach
	 * when I think it should be possible to do it generically, if I could just
	 * workout how, but I'm a putz! So, this is AS GENERICALLY as I know how.
	 *
	 * @param f the filter is usually a lambda expression which returns true
	 *  if the given indice should be included in the result
	 * @return a new Idx containing only the accepted indices of this Idx.
	 */
	public Idx where(final IntFilter f) {
		int bits, j, r0=0,r1=0,r2=0; // the result
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(j+k) )
						r0 |= IDX_SHFT[j+k];
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(BITS_PER_ELEMENT+j+k) )
						r1 |= IDX_SHFT[j+k];
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(BITS_TWO_ELEMENTS+j+k) )
						r2 |= IDX_SHFT[j+k];
		return new Idx(r0,r1,r2);
	}

	/**
	 * Repopulates 'dest' with 'src' cells that're accept'ed by this
	 * {@code IFilter<Cell>}.
	 *
	 * @param gridCells the source grid.cells
	 * @param result the array to repopulate with matching cells
	 * @param f the filter is usually a lambda expression, returning true if
	 *  the given Cell should be included in the result
	 * @return the number cells added to 'dest'
	 */
	public int where(final Cell[] gridCells, final Cell[] result, final IFilter<Cell> f) {
		int bits, j, cnt=0;
		Cell c;
		if ( (bits=a0) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=gridCells[j+k]) )
						result[cnt++] = c;
		if ( (bits=a1) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=gridCells[BITS_PER_ELEMENT+j+k]) )
						result[cnt++] = c;
		if ( (bits=a2) != 0 )
			for ( j=0; j<BITS_PER_ELEMENT; j+=BITS_PER_WORD )
				for ( int k : WORDS[(bits>>j)&WORD_MASK] )
					if ( f.accept(c=gridCells[BITS_TWO_ELEMENTS+j+k]) )
						result[cnt++] = c;
		return cnt;
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
			pb0=a0; pb1=a1; pb2=a2;
		} else if ( a0!=pb0 || a1!=pb1 || a2!=pb2 ) {
			plusBuds.set(this);
			forEach((i)->plusBuds.or(BUDDIES[i]));
			pb0=a0; pb1=a1; pb2=a2;
		}
		return plusBuds;
	}
	private Idx plusBuds;
	private int pb0, pb1, pb2;

	/**
	 * Get an Idx of the buddies of cells in this Idx.
	 *
	 * @param exceptThese after or-ing all buds should I buds.andNot(this)
	 * @return the buddies of cells in this idx (uncached).
	 */
	public Idx buds(final boolean exceptThese) {
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
	 * Returns a new array 'cells' in this Idx. Note that creating a new
	 * array takes time, so cellsN(grid, reusableArray) is a bit faster.
	 *
	 * @param gridCells to read
	 * @return a new {@code Cell[]} containing the cells in this Idx.
	 */
	public Cell[] cellsNew(final Cell[] gridCells) {
		return cells(gridCells, new Cell[size()]);
	}

	/**
	 * Repopulate 'result' with 'cells' in this Idx.
	 * <p>
	 * Use this one with the right sized array, which is returned.
	 * <p>
	 * Use cellsN if you need know how many cells are added to the array.
	 * Be warned that the passed array is NOT null terminated (as you might
	 * reasonably expect, from experience, sigh).
	 *
	 * @param gridCells to read
	 * @param result the destination array to populate
	 * @return the given cells array, so create new if you must.
	 */
	public Cell[] cells(final Cell[] gridCells, final Cell[] result) {
		forEach((count, indice) -> result[count] = gridCells[indice]);
		return result;
	}

	/**
	 * Return an array of the three bloody masks.
	 *
	 * @return {@code new int[]{a0,a1,a2}}
	 */
	public int[] masks() {
		if ( masks == null )
			masks = new int[]{a0,a1,a2};
		return masks;
	}
	private int[] masks;

	// cellsN is hammered by Aligned*Exclusion, so we create ONE instance of
	// CellsNVisitor, just to be more memory efficient, that's all.
	// package visible for the test-case
	private static final class CellsNVisitor implements IntIntVisitor {
		public Cell[] src; // to read
		public Cell[] dest; // resulting cells
		@Override
		public void visit(final int count, final int indice) {
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
	public int cellsN(final Grid grid, final Cell[] result) {
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

	// maybes is hammered by AlignedExclusion, so we create ONE instance of
	// MaybesVisitor, to be memory efficient. Note well I'm static. My public
	// fields are set by AlignedExclusion once per findHints, which is millions
	// of times less often than it fetches maybes from various Idxs.
	public static final class MaybesVisitor implements IntIntVisitor {
		public int[] gridMaybes; // source
		public int[] result; // destination
		@Override
		public void visit(final int count, final int indice) {
			result[count] = gridMaybes[indice];
		}
	}

	/**
	 * You setup the static MAYBES_VISITOR once at start of each findHints,
	 * then call maybes repeatedly on whatever Idx's,
	 * and finally tear it down at the end.
	 * <pre>
	 * * Set my cells to grid.cells of the grid you are searching
	 * * Set my maybes to an int array that is large enough for however many
	 *   cells you are processing, ie however large your Idx's are. If you do
	 *   not know then the max possible would be 64 = 81 - 17 (min clues).
	 *   Too large doesn't matter, too small throws an AIOOBE.
	 * </pre>
	 */
	public static final MaybesVisitor MAYBES_VISITOR = new MaybesVisitor();

	/**
	 * You setup the static MAYBES_VISITOR once at start of each findHints,
	 * then call maybes repeatedly on whatever Idx's,
	 * and finally tear it down at the end.
	 *
	 * @return the number of maybes added to the maybes array you setup
	 */
	public int maybes() {
		return forEach(MAYBES_VISITOR);
	}

//not_used 2021-12-27
//	// MaybesVisitorSingleUse is less messy: there's no need to cleanup.
//	private static final class MaybesVisitorSingleUse implements IntIntVisitor {
//		private final Cell[] gridCells; // to read
//		private final int[] maybes; // the result
//		MaybesVisitorSingleUse(final Cell[] gridCells, final int[] maybes) {
//			this.gridCells = gridCells;
//			this.maybes = maybes;
//		}
//		@Override
//		public void visit(final int count, final int indice) {
//			maybes[count] = gridCells[indice].maybes;
//		}
//	}
//	public int maybes(final Cell[] gridCells, final int[] maybes) {
//		return forEach(new MaybesVisitorSingleUse(gridCells, maybes));
//	}

//not_used 2021-12-27 prefer lease
//	/**
//	 * Get the cells from the given Grid at the indices in this Idx.
//	 * <p>
//	 * If you use CASA be sure clean-up with Cells.cleanCasA. Holding ONE Cell
//	 * reference keeps the whole Grid in memory.
//	 *
//	 * @param grid the grid to read cells from
//	 * @return a cached array from CASA of cells in this Idx, so clone to keep.
//	 */
//	public Cell[] cellsA(final Grid grid) {
//		final int n = size();
//		if ( n == 0 )
//			return Cells.EMPTY_ARRAY;
//		return cells(grid.cells, Cells.arrayA(n));
//	}

//not_used 2021-12-27 prefer lease
//	/**
//	 * Get the cells from the given Grid at the indices in this Idx.
//	 * <p>
//	 * Like cellsA but using a second CAS (CASB), so you can have a cellsB loop
//	 * inside of a cellsA loop.
//	 * <p>
//	 * If you use CASB be sure clean-up with Cells.cleanCasB. Holding ONE Cell
//	 * reference keeps the whole Grid in memory.
//	 *
//	 * @param cells to read
//	 * @return a cached array from CASB of cells in this Idx, so clone to keep.
//	 */
//	public Cell[] cellsB(final Cell[] cells) {
//		return cells(cells, Cells.arrayB(size()));
//	}

	// ----------------------------- stringy stuff ----------------------------

	// NOTE: The ids methods exist mainly to avoid having to get the grid in
	// order to get the cells just to turn those cells into id's, which are a
	// static attribute of the Grid, so we do NOT need a grid instance as long
	// as it's all done here, I think, where its convenient, except that we're
	// imbedding formatting concerns in the model, which is bad practice, but
	// then they're all the way through the grid, and the test-cases, so stuff
	// it, let's just hack something together that works then let smarty-pants
	// design gurus worry about design when stuff works. Rome wasn't designed
	// in a day either. In fact it was just cobbled together, and look how that
	// worked out for them. sigh. Robbing Peter to pay Paul works great right
	// up until it doesn't, and Peter shows up at your house, burns it down,
	// shoots you and your missus and rapes the ass of your kids (gender
	// non-specifically, of course; We are the MODERN hun) and generally makes
	// a mess out of everything, because you've made a mess out of everything.
	// Big sigh.
	public StringBuilder idsSB(final StringBuilder sb, final int n
			, final String sep, final String lastSep) {
		final int m = n - 1;
		forEach((cnt, i) -> {
			if ( cnt > 0 )
				if ( cnt == m )
					sb.append(lastSep);
				else
					sb.append(sep);
			sb.append(CELL_IDS[i]);
		});
		return sb;
	}
	public StringBuilder idsSB(final String sep, final String lastSep) {
		final int n = size();
		final int capacity = (CELL_ID_LENGTH+sep.length())*n
				+ Math.max(lastSep.length() - sep.length(), 0);
		return idsSB(new StringBuilder(capacity), n, sep, lastSep);
	}
	public String ids(final String sep, final String lastSep) {
		return idsSB(sep, lastSep).toString();
	}
	public String ids(final String sep) {
		return idsSB(sep, sep).toString();
	}

	/**
	 * Returns SSV (Space Separated Values) of the ID's of cells in this Idx.
	 *
	 * @return SSV of my cell-ids.
	 */
	public String ids() {
		return idsSB(" ", " ").toString();
	}

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
	public static StringBuilder append(final StringBuilder sb, final int a0
			, final int a1, final int a2) {
		forEach(a0, a1, a2, (cnt, i) -> {
			if ( cnt > 0 )
				sb.append(' ');
			sb.append(CELL_IDS[i]);
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
	public static String toStringCellIds(final int a0, final int a1, final int a2) {
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
	public static String toStringIndices(final int a0, final int a1, final int a2) {
		final StringBuilder sb = getSB(size(a0,a1,a2) * 3);
		forEach(a0, a1, a2, (cnt, i) -> {
			if ( cnt > 0 )
				sb.append(' ');
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
		// debug only: the raw indices
		return toStringIndices(a0, a1, a2);
	}

	// get my StringBuilder... I keep ONE that grows.
	private static StringBuilder getSB(final int capacity) {
		if ( SB == null )
			return SB = new StringBuilder(capacity);
		SB.setLength(0);
		if ( capacity > SB.capacity() )
			SB.ensureCapacity(capacity);
		return SB;
	}
	private static StringBuilder SB;

	/**
	 * Returns Comma Separated Values of CELL_IDS of the cells in this Idx.
	 *
	 * @return a CSV String of the cells in this Idx.
	 */
	public String csv() {
		final StringBuilder sb = getSB(size()<<2); // * 4
		forEach( (i)->sb.append(CELL_IDS[i])
			  ,  (i)->sb.append(", ").append(CELL_IDS[i]) );
		return sb.toString();
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Returns -1, 0, 1 as this is less than, equal, or more than a.
	 *
	 * @param a
	 * @return
	 */
	@Override
	public int compareTo(final Idx a) {
		int diff;
		if ( (diff=a2 - a.a2) == 0
		  && (diff=a1 - a.a1) == 0 )
			diff = a0 - a.a0;
		if ( diff < 0 )
			return -1;
		if ( diff > 0 )
			return 1;
		return 0;
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
	public static int hashCode(final int a0, final int a1, final int a2) {
		return ((a2^(a2>>>16))<<5)
			 ^ ((a1^(a1>>>16))<<3)
			 ^ (a0^(a0>>>16));
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

//not_used 2021-11-30 08:43 see GEM#hash method comments
//	/**
//	 * Hash this Idx into 64 bits, with less collisions than 32 bits. We're
//	 * squeezing 81 bits into 64, so 17 collide. In this implementation the
//	 * low 17 of a2 collide with the high 17 of a1. Best possible, AFAIK.
//	 * <p>
//	 * The a* parameters are longs only in order to make my mathematics work.
//	 * You can't get a long result by bit-twiddling ints. Just pass me ints
//	 * that're automatically widened to longs.
//	 *
//	 * @param a0 a long with an int value: first 27 cells
//	 * @param a1 a long with an int value: second 27 cells
//	 * @param a2 a long with an int value: third 27 cells
//	 * @return the hash64 of this Idx
//	 */
//	public static long hash64(final long a0, final long a1, final long a2) {
//		return (a2<<37L) ^ (a1<<27L) ^ a0;
//	}
//
//	public long hashCode64() {
//		return hash64(a0, a1, a2);
//	}

	/**
	 * Does this Idx equal the virtual Idx(o0,o1,o2).
	 *
	 * @param o0
	 * @param o1
	 * @param o2
	 * @return
	 */
	public boolean equals(final int o0, final int o1, final int o2) {
		return a0==o0 && a1==o1 && a2==o2;
	}

	/**
	 * Does this equal the given Object.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(final Object o) {
		return this==o || o instanceof Idx && equals((Idx)o);
	}

	/**
	 * Return does this == o.
	 *
	 * @param o
	 * @return
	 */
	public boolean equals(final Idx o) {
		return a0 == o.a0
			&& a1 == o.a1
			&& a2 == o.a2;
	}

}
