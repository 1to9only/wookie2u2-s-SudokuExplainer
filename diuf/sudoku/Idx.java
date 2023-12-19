/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.utils.IntArray;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.CELL_ID_LENGTH;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.RIBS;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.utils.IFilter;
import diuf.sudoku.utils.Mutator;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import static diuf.sudoku.Idx.FIFTY_FOUR;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.IntArray.THE_EMPTY_INT_ARRAY;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.utils.IntQueue.QEMPTY;
import static diuf.sudoku.utils.MyMath.powerOf2;

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
 *     Idx intersection = new Idx(); // it is faster to re-use existing!
 *     intersection.m0 = i0.m0 & i1.m0;
 *     intersection.m1 = i0.m1 & i1.m1;
 *     intersection.m2 = i0.m2 & i1.m2;
 * }</pre>
 * which is bulls__tquick(TM) compared to the alternative using Sets addAll()
 * and retainAll() methods.
 * <p>
 * FYI: the equivalent method looks simpler, but requires invocation, which is
 * always a bit slow, so we use it "normally", when speeds not uber-critical:
 * <pre>{@code
 *     Idx i0 = set0.idx();
 *     Idx i1 = set1.idx();
 *     Idx intersection = Idx.newAnd(i0, i1);
 * }</pre>
 * <p>
 * So the Idx class exposes a number of methods that do bitset-index stuff,
 * both as "raw" {@code int[]} arrays and as instances of Idx, which basically
 * just encapsulates its own {@code int[]} which it calls the static methods
 * on, in case you are currently towel-placement-challenged. The static int[]
 * methods tend to be more efficient, mainly because (as above) you can do the
 * calcs yourself locally, without having to wear the expense of invoking any
 * methods.
 * <p>
 * This class was originally written only as static helper methods for the
 * Aligned*Exclusion classes, but Idx is no longer used there, for speed.
 * Idx was later restored and converted to expose instances for use in the
 * XYWing class, where performance is much less critical. So Idx() has same
 * specifications as int[], but A*E does not use Idxs, it uses raw int[]
 * arrays instead, because speed is King; where-as XYWing holds instances of
 * Idx, which have internal int[] arrays, because speed is a mere prince.
 * <p>
 * Later I picked-up HoDoKu, which makes extensive use of Idxs (its SudokuSet)
 * so Idxs are now used in Aligned*Exclusion, Almost Locked Sets (als), the
 * various Fisherman, and pretty much everywhere because they are very useful.
 * Just pushing the cheese around on my cracker. Ye rolls yon dice and ye moves
 * yon mice. I apologise for any confusion.
 * <p>
 * Use the @Mutator annotation to denote methods that mutate the Idxs state,
 * hence are overridden by IdxL, to check the lock. If ALL IdxL mutators check
 * the lock then a locked IdxL cannot be modified, but missing just one renders
 * IdxL pointless. See {@link diuf.sudoku.tools.CheckIdxLMutators}.
 * <p>
 * because Idx is structured around a GRID_SIZE of 81 it does not use GRID_SIZE
 * or any of the associated constants. Changing the GRID_SIZE means rewriting
 * or adapting the whole Idx class to suit the new size, or resorting to Bitset
 * (which is still overkill) to support variable sized Idxs.
 * <p>
 * <pre>
 * See: diuf.sudoku.solver.hinters.xyz.XYWing // uses Idxs
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
 * its basically useless!
 *
 * KRC 2021-05-20 I really want to eradicate the BitIdx class, so I just tried
 * moving Idx into Grid as an inner-class, to get rid of the need to pass the
 * grid into each et al, but then you need a grid instance to create an Idx,
 * and the Grids static Idxs are "needed" for speed, so I gave up; leaving Idx
 * as an independant class, and just wear passing the Grid around, and the need
 * for a BitIdx to iterate Idxd cells. It is messy, but the only other way I
 * can think of is a new IdxIt class which overrides Idxs each et al to
 * pass in Grid.this to iterate, but then the cut-lunch Idxs would lack this
 * feature, rendering it more-or-less useless, I think. sigh.
 * </pre>
 *
 * @see IdxL
 * @see Mutator
 * @see diuf.sudoku.tools.CheckIdxLMutators
 * @author Keith Corlett 2019 DEC
 */
public class Idx implements Serializable, Comparable<Idx>, Iterable<Integer>, Set<Integer> {

//	public static final long[] COUNTS = new long[3];

	private static final long serialVersionUID = 342L;

	/** Three 9bit "words" = 27 bits per mask, and 3 masks per Idx. */
	public static final int BITS_PER_WORD = 9;

	/** Three 9bit "words" = 27 bits per mask, and 3 masks per Idx. */
	public static final int BITS_PER_ELEMENT = 27;
	/** 27 1's in a 32 bit int */
	public static final int BITS27 = (1<<BITS_PER_ELEMENT)-1;

	/** the value of a full "word" is 9 set (1) bits. <br>
	 * Decimal=511  Hexadecimal=0x1FF  Octal=777  Binary=1,1111,1111   9 1s */
	public static final int WORD_MASK = 0x1FF;

	/** The number of combinations possible in 9 bits: 512. */
	private static final int WORDS_SIZE = 1<<BITS_PER_WORD; // 512

	/** the left-shifted bitset value of each possible index. */
	public static final int[] IDX_SHFT = new int[BITS_PER_ELEMENT];

	/** the number of ints per Idx. */
	public static final int THREE = 3;

	/** WORDS[9-bit-word] contains the indexes that are in this 9-bit-word.
	 * There are 3 words per 27-bit element. The size of each sub-array is
	 * variable 0..9, depending on the number of set (1) bits in my "word",
	 * ie my index. Sigh. */
	public static final int[][] WORDS = new int[WORDS_SIZE][];
	static {
		for ( int i=0; i<BITS_PER_ELEMENT; ++i )
			IDX_SHFT[i] = 1<<i;
		for ( int i=0; i<WORDS_SIZE; ++i )
			WORDS[i] = Indexes.toIndexesArrayNew(i);
	}

	/** number of bits in two elements: 54 = 2*27. */
	public static final int FIFTY_FOUR = BITS_PER_ELEMENT<<1;

	// -----------------------------------------------------------------
	// these fields where added for the long mask 'a' int 'b' conversion
	// -----------------------------------------------------------------

	/**
	 * Idx's first mask is a long (64 bits) of which 6 * 9 = 54 bits are used.
	 * WARN: The unused 10 bits remain 0. Care is required with any BITWISE-OR
	 * operations to ensure this. If you OR-in odd high-order-bits then things
	 * WILL go bad fast, and they can be VERY difficult to find! So just dont
	 * introduce them in the first place. You have been warned.
	 * <p>
	 * Because only 54 bits are used this mask is positive (bit64 is never (1))
	 * and &gt; or &lt; are faster than != or = (YMMV!), so I can get away with
	 * using the faster comparator operators on Idx masks. Whenever I use these
	 * "cheat" &gt; or &lt; operator I try to remember to mark it: "// ?bits"
	 * where ? is the number of bits in this bitset; to remind me that I rely
	 * on NEVER negative bitset values.
	 * <p>
	 * Similarly, the second int mask uses 27/32 bits, so its always positive,
	 * never negative, except, on Tuesdays. We dont like Tuesdays, right
	 * Pauline. And that Friday bastard. Hang him, for thinking for himself.
	 * We bloody lost. AnIdokneewanna believe it. Fush munusta, and Chups.
	 * The pig raped my auntie!
	 * <p>
	 * YMMV! Your mileage may vary. There's a lot going on under Javas hood.
	 * We have a JVM with a JIT compiler and an optimiser, all running on
	 * arbitrary hardware. Hence overarching statements can be misleading or
	 * just plain WRONG. So when I say that "&gt; or &lt; are faster" what I
	 * actually mean is "this is usually the case", but YMMV! Mind you, this
	 * particular overarching statement is safe, until those subatomic bastards
	 * change the game again. Rely on it, but what this space.
	 */
	/** 54 1's in a 64 bit long, for use with long 'M0' masks */
	public static final long BITS54L = (1L<<FIFTY_FOUR)-1L;

	/** 9 1's in a 64 bit long, for use with long 'M0' masks */
	public static final long LONG9 = 0x1FFL;

	/** 27 1's in a 64 bit long, for use with long 'M0' masks */
	public static final long LONG27 = (1L<<27)-1L;

	/** the left-shifted bitset value of each possible index. */
	public static final long[] MASKED81 = new long[GRID_SIZE];
	static {
		for ( int i=0; i<GRID_SIZE; ++i )
			MASKED81[i] = 1L<<(i%FIFTY_FOUR);
	}

	/** self-documenting Idx constructor param. */
	public static final boolean FULL = true;

	/**
	 * MASKOF is the mask (0, 1, or 2) this indice belongs-in. <br>
	 * It facilitates adding an indice to an Exploded Idx, as in:
	 * <pre>
	 *    {@code explodedIdxArray[MASKOF[indice] |= MASKED[indice];}
	 * </pre>
	 * It also facilitates removing an indice from an Exploded Idx, as in:
	 * <pre>
	 *    {@code explodedIdxArray[MASKOF[indice] &= UNMASKED[indice];}
	 * </pre>
	 * My value is:
	 * <pre>
	 *    {@code i / BITS_PER_ELEMENT }
	 * </pre>
	 * <p>
	 * This field was modified for the long 'a' int 'b' conversion.
	 * Its now (0, or 1) coz there are now only two masks, for speed.
	 */
	public static final int[] MASKOF = {
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
	};

	/**
	 * MASKED contains the 27bit bitset representation of this cell indice. <br>
	 * It enables the inclusion of an indice in an exploded Idx, as in:
	 * <pre>
	 *    {@code explodedIdx[MASKOF[indice] |= MASKED[indice];}
	 * </pre>
	 * My value is:
	 * <pre>
	 *    {@code 1 << (i % BITS_PER_ELEMENT)}
	 * </pre>
	 */
	public static final int[] MASKED = new int[GRID_SIZE];
	static {
		for ( int i=0; i<GRID_SIZE; ++i )
			MASKED[i] = IDX_SHFT[i % BITS_PER_ELEMENT];
	}

	/**
	 * Returns a new Idx containing the given cell indice.
	 *
	 * @param indice indice of the cell to add
	 * @return a new Idx containing the given indice.
	 */
	public static final Idx of(final int indice) {
		if ( indice < FIFTY_FOUR )
			return new Idx(MASKED81[indice], 0);
		return new Idx(0L, MASKED[indice]);
	}

	/**
	 * Returns a new Idx containing the given cell indices.
	 *
	 * @param indice0 indice of first cell to add
	 * @param indice1 indice of second cell to add
	 * @return a new Idx containing the given indices.
	 */
	public static Idx of(final int indice0, final int indice1) {
		final Idx result = Idx.of(indice0);
		result.add(indice1);
		return result;
	}

	/**
	 * Returns a new Idx containing the given cell indices.
	 *
	 * @param indice0 indice of first cell to add
	 * @param indice1 indice of second cell to add
	 * @param indice2 indice of third cell to add
	 * @return a new Idx containing the given indices.
	 */
	public static Idx of(final int indice0, final int indice1, final int indice2) {
		final Idx result = Idx.of(indice0);
		result.add(indice1);
		result.add(indice2);
		return result;
	}

	/**
	 * Returns a new Idx containing the given cell indices.
	 *
	 * @param indice0 indice of first cell to add
	 * @param indice1 indice of second cell to add
	 * @param indice2 indice of third cell to add
	 * @param indice3 indice of fourth cell to add
	 * @return a new Idx containing the given indices.
	 */
	public static Idx of(final int indice0, final int indice1, final int indice2, final int indice3) {
		final Idx result = Idx.of(indice0);
		result.add(indice1);
		result.add(indice2);
		result.add(indice3);
		return result;
	}

	/**
	 * Build a new Idx of this Collection of Integer: indices.
	 *
	 * @param col is untyped, but must contain Integers (indices)
	 * @return a new Idx containing indices
	 */
	private static Idx of(final Collection<?> col) {
		long m0=0L; int m1=0, i; Object f;
		if ( (f=first(col)) instanceof Integer )
			for ( Object obj : col )
				if ( (i=(Integer)obj) < FIFTY_FOUR )
					m0 |= MASKED81[i];
				else
					m1 |= MASKED[i];
		else if ( f instanceof String ) {
			for ( Object obj : col )
				if ( (i=Grid.indice((String)obj)) < FIFTY_FOUR )
					m0 |= MASKED81[i];
				else
					m1 |= MASKED[i];
		}
		return new Idx(m0, m1);
	}

	/**
	 * Constructs a new Idx(idxA & idxB).
	 *
	 * @param idxA
	 * @param idxB
	 * @return the new index
	 */
	public static Idx ofAnd(final Idx idxA, final Idx idxB) {
		return new Idx(idxA.m0&idxB.m0, idxA.m1&idxB.m1);
	}

	/**
	 * Parse an Idx from CSV (comma separated values) of Cell.ids.
	 * <p>
	 * For example: "E3, H3, I3, H8, I8, E6, I6"
	 *
	 * @param csv a comma separated values String of Cell.ids.
	 * @return a new Idx.
	 */
	public static final Idx ofCsv(final String csv) {
		final Idx result = new Idx();
		if ( csv==null || csv.isEmpty() )
			return result;
		for ( final String id : csv.split(" *, *") )
			result.add(Grid.indice(id));
		return result;
	}

	/**
	 * Parse an Idx from SSV (space separated values) of Cell.ids,
	 * for use in the test-cases only.
	 * <p>
	 * For example: "E3 H3 I3 H8 I8 E6 I6"
	 * <p>
	 * Note that this is Idxs standard toString format!
	 * What was I thinking with CSV and not SSV? I am a putz!
	 *
	 * @param ssv space separated values (Cell.ids).
	 * @return a new Idx.
	 */
	public static final Idx ofSsv(final String ssv) {
		if ( ssv.isEmpty() ) // throws NPE if null
			throw new IllegalArgumentException("empty ssv");
		final Idx result = new Idx();
		for ( final String id : ssv.split(",? +") )
			result.add(Grid.indice(id));
		return result;
	}

	/**
	 * Return the cardinality of the virtual Idx: m0, m1, m2.
	 * <p>
	 * NB: bitCount is faster than anything. Cant beat it. Trust me, I tried.
	 * If you CAN beat it then my only question is WTF.
	 *
	 * @param m0 mask 0: 27 used bits
	 * @param m1 mask 1: 27 used bits
	 * @return the number of set (1) bits in the Idx
	 */
	public static final int size(final long m0, final int m1) {
		return Long.bitCount(m0) + Integer.bitCount(m1);
	}

	/**
	 * Returns (idxA & idxB) > 0 (27bits),
	 * ie the two sets share common elements,
	 * ergo they overlap.
	 * <p>
	 * I must explain the 27bits thing. Each Idx mask contains just 27 bits and
	 * in Javas integer-number system the 32nd bit is used as the sign-bit, so
	 * negative numbers have 1 (a set bit) as there 32nd bit. An Idx uses just
	 * 27 bits, hence I can get away with testing for empty using &lt; 1, and
	 * for not-empty using &gt; 0. The equals and not-equals operations are
	 * slower than greater-than and less-than IN HARDWARE. Its under the bonnet
	 * son. Java runs through a virtual machine, so your milage may vary. Sigh.
	 * I leave a // 27bit breadcrumb wherever I do this coz using the 32nd bit
	 * WILL cause mahem. I dont control the future. I merely plan to meat it.
	 *
	 * @param idxA the first Idx
	 * @param idxB the second Idx
	 * @return does idxA intersect idxB
	 */
	public static final boolean overlaps(final Idx idxA, final Idx idxB) {
		return ((idxA.m0&idxB.m0) | (idxA.m1&idxB.m1)) > 0;
	}

	/**
	 * seers is for {@link diuf.sudoku.solver.hinters.als.RccFinderAll#find}
	 * which is currently its only use, and I expect it will remain that way.
	 * <p>
	 * Returns (idxA & idxB & idxC) == idxC,
	 * ie do both idxA and idxA contain all elements in idxC,
	 * ergo do the bastards see 'c'.
	 * <p>
	 * This method exists for performance. Its faster all in one statement, to
	 * not test mask 'b' unless 'a' is true, and likewise not test 'c' unless
	 * both 'a' and 'b' are true. Thats the bloody theory anyway. If you have
	 * empirical evidence demurring from this contention then I am all ears.
	 *
	 * @param idxA the first Idx
	 * @param idxB the second Idx
	 * @param idxC the idx that is, allegedly, contained by both a and b
	 * @return (idxA & idxB & idxC) == idxC
	 */
	public static boolean seers(final Idx idxA, final Idx idxB, final Idx idxC) {
		return (idxA.m0 & idxB.m0 & idxC.m0) == idxC.m0
			&& (idxA.m1 & idxB.m1 & idxC.m1) == idxC.m1;
	}

	/**
	 * if size(m0,m1)==1 return the only indice; else -1.
	 *
	 * @param m0 the first mask of 54bits
	 * @param m1 the second mask of 27bits
	 * @return the only indice in the virtual Idx(m0,m1); else -1
	 */
	public static int only(final long m0, final int m1) {
		int x, y, n;
		if ( m0 > 0L ) {
			if ( m1 > 0 )
				return -1; // two masks means size > 1
			long l = m0 & -m0;
			if ( (m0 & ~l) > 0L )
				return -1; // any remaining means size > 1
			//return Long.numberOfTrailingZeros(l); // inline for speed
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		if ( m1 > 0 ) {
			// nb: we get here only when m0 is 0
			x = m1 & -m1;
			if ( (m1 & ~x) > 0L )
				return -1; // any remaining means size > 1
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(m1);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		return QEMPTY;
	}

	/**
	 * Count the number of values offed from each cell in the grid.
	 * <p>
	 * Note that this method exists for GEM only (currently), but it's better
	 * to keep the bit-twiddling in Idx if at all possible, so that I can be
	 * the responsibility solely of the team bit-twiddler, instead of spreading
	 * my overly complex s__t throughout the whole app.
	 *
	 * @param offs offs of the current color
	 * @param counts output number of values offed from each cell
	 */
	public static void offCounts(final Idx[] offs, final int[] counts) {
		long m0, l;
		int m1, i, y, n, x;
		i = 0; // i is the indice
		do
			counts[i] = 0;
		while(++i < GRID_SIZE);
		i = 1; // i is now v
		do
			if ( offs[i] != null ) {
				// read the Idx inline, for speed
				// pre: 314 ms (0.8%)
				// pst: 193 ms (0.5%)
				m0 = offs[i].m0;
				m1 = offs[i].m1;
				while ( m0 > 0L ) {
					l = m0 & -m0; // lowestOneBit
					m0 &= ~l; // remove l
					//return Long.numberOfTrailingZeros(l);
					//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
					n = 63;
					y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
					y=x<<16; if(y!=0){n-=16; x=y;}
					y=x<< 8; if(y!=0){n-= 8; x=y;}
					y=x<< 4; if(y!=0){n-= 4; x=y;}
					y=x<< 2; if(y!=0){n-= 2; x=y;}
					++counts[n - ((x<<1)>>>31)];
				}
				while ( m1 > 0 ) {
					x = m1 & -m1; // lowestOneBit
					m1 &= ~x; // remove x
					//return FIFTY_FOUR + Integer.numberOfTrailingZeros(i);
					//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
					n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
					y=x<<16; if(y!=0){n-=16; x=y;}
					y=x<< 8; if(y!=0){n-= 8; x=y;}
					y=x<< 4; if(y!=0){n-= 4; x=y;}
					y=x<< 2; if(y!=0){n-= 2; x=y;}
					++counts[n - ((x<<1)>>>31)];
				}
			}
		while (++i < VALUE_CEILING);
	}

	/**
	 * Read indices from the virtual Idx(m0,m1) into result, and return how
	 * many, so that you can re-use a fixed-sized array.
	 *
	 * @param m0 mask 0: 54 bit long
	 * @param m1 mask 1: 27 bit int
	 * @param result array to populate with indices
	 * @return how many
	 */
	public static int toArray(long m0, int m1, final int[] result) {
		long l; int x, y, n, count = 0;
		while ( m0 > 0L ) {
			l = m0 & -m0; // lowestOneBit
			m0 &= ~l; // remove l
			//return Long.numberOfTrailingZeros(l); // inline for speed
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			result[count++] = n - ((x<<1)>>>31);
		}
		while ( m1 > 0 ) {
			x = m1 & -m1; // lowestOneBit
			m1 &= ~x; // remove x
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(i); // inline for speed
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			result[count++] = n - ((x<<1)>>>31);
		}
		return count;
	}

	public static int[] toArrayNew(long m0, int m1) {
		final int[] result = new int[size(m0,m1)];
		toArray(m0, m1, result);
		return result;
	}

	/**
	 * Append space separated cell-IDs (SSV) of the given Idx(m0,m1) to
	 * the given StringBuilder. First ensureCapacity of length + the given $n.
	 * <p>
	 * Each bitset uses 27 least significant (rightmost) bits, for 81 cells.
	 *
	 * @param sb to append to
	 * @param m0 mask 0: 54 bit long
	 * @param m1 mask 1: 27 bit int
	 * @return the given StringBuilder for method chaining.
	 */
	public static StringBuilder append(final StringBuilder sb, final long m0, final int m1) {
		int i;
		final IntQueue q = new MyIntQueue(m0,m1);
		if ( (i=q.poll()) > QEMPTY ) {
			sb.append(CELL_IDS[i]);
			while ( (i=q.poll()) > QEMPTY )
				sb.append(" ").append(CELL_IDS[i]);
		}
		return sb;
	}

	/**
	 * Returns a cell-ids String of m0, m1, m2; as if they where an Idx array.
	 * <p>
	 * NOTE: toString is no longer used in hint-HTML, so performance does not
	 * really matter any longer, so we COULD use a each and a Visitor.
	 *
	 * @param m0 mask 0: 54 bit long
	 * @param m1 mask 1: 27 bit int
	 * @return the cell-ids of indices in this Idx.
	 */
	public static String toStringCellIds(final long m0, final int m1) {
		return append(SB(size(m0,m1)<<2), m0,m1).toString();
	}

	/**
	 * For debugging.
	 *
	 * @param m0 mask 0: 54 bit long
	 * @param m1 mask 1: 27 bit int
	 * @return SSV indices (not cell-ids)
	 */
	public static String toStringIndices(final long m0, final int m1) {
		int i;
		final StringBuilder sb = SB(size(m0,m1)<<2); // * 4
		final IntQueue q = new MyIntQueue(m0,m1);
		if ( (i=q.poll()) > QEMPTY ) {
			sb.append(i);
			while ( (i=q.poll()) > QEMPTY )
				sb.append(" ").append(i);
		}
		return sb.toString();
	}

	/**
	 * RETAIN: for debugging. Don't care if unused. My uncle Rodney concurs.
	 * <p>
	 * Stringify elims: an array of exploded Idxs, one per value 1..9.
	 *
	 * @param elims Exploded Idxs [value 1..9][mask 0..2]
	 * @return Nine lines for 9 Idxs, or a haiku.
	 */
	public static String toString(final long[][] elims) {
		final StringBuilder sb = SB(1024);
		if ( elims.length == 10 && elims[1].length == 2 ) {
			for ( int v=1; v<10; ++v ) {
				sb.append(v).append(": ");
				append(sb, elims[v][0], (int)elims[v][1]).append(NL);
			}
		} else {
			sb.append("1: I just want my mom.").append(NL)
			  .append("2: My uncle Rodney wants my mom too.").append(NL)
			  .append("3: I format only elims[10][2].").append(NL)
			  .append("4: My uncle Rodney is a dirty old man!").append(NL);
		}
		return sb.toString();
	}

	/**
	 * Get the toString() of a virtual Idx.
	 *
	 * @param m0 mask 0: 27 used bits
	 * @param m1 mask 1: 27 used bits
	 * @return the cell.ids (or plain indexes when debugging)
	 */
	public static final String toString(final long m0, final int m1) {
		if ( true ) // @check true
			// cell-ids toString for normal use
			return toStringCellIds(m0, m1);
		// debug only: the raw indices
		return toStringIndices(m0, m1);
	}

	/**
	 * Hash this Idx: fold each int in half and offset them. The result is a
	 * pretty trashy hashCode, but we are smashing 81 bits into 32, so some
	 * loss, and therefore collisions, are inevitable. The trick is minimising
	 * the collisions, and I have not really done that: just thrown something
	 * together that works a bit, because I am not a mathematicians asshole.
	 * <p>
	 * My humour is a bit off too! Binary LOLs! What is LOL in binary! NANANA!
	 * Come at me with that fruit!
	 *
	 * @param m0 mask 0: 27 used bits
	 * @param m1 mask 1: 27 used bits
	 * @return the hashCode for this Idx
	 */
	public static int hashCode(final long m0, final int m1) {
		// NASTY: m1 is an int: fold it in half;
		// m0 is a long: fold it in half and quarters;
		// thus increasing probabilty of lower 16 bits containing 1's
		// ----------------------------------------------------------
		// If you want small fast hashes then you should also m*&>>>8
		// to increase probability of lower 8 bits containing 1's.
		// I say here that I care not for small hash performance.
		// 1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16K 32K
		// 1 2 3 4  5  6  7   8   9  10   11   12   13   14  15  16
		// ----------------------------------------------------------
		return ((m1^(m1>>>16))<<3) ^ (int)(m0^(m0>>>32)^(m0>>>16));
	}

	/**
	 * Hash this Idx into a long.
	 *
	 * @param m0 mask 0: 54 used bits
	 * @param m1 mask 1: 27 used bits
	 * @return the long hashCode of this Idx
	 */
	public static long hashCode64(final long m0, final long m1) {
		return (m0<<10L) ^ m1;
	}

	// ============================ instance stuff ============================

	/**
	 * A long and b int are a bitset of the 81 cells in a grid. 54 bits of the
	 * long are used, and 27 bits of the int, and 54+27=81; one bit for each
	 * cell in a Grid. If the bit is set (1) it denotes that cell is present in
	 * this Set. Simples. We can do all-sorts of bitwise magic on bitsets, just
	 * for fun.
	 * <p>
	 * These mask fields are readonly to public. Please avoid modifying them,
	 * unless you really want to (Adams D.), but please provide a comment
	 * justifying your decision, even if its only (Adams D.). Give it some
	 * thought. That's all.
	 * <p>
	 * You can axedintely mutate an IdxI by sticking your hand down his pants.
	 * You dont want that. Rules create more interesting solutions than Grud.
	 * Grug STROOONG! Grud good. Grud take! The only rule is that you cannot
	 * tell me what the rules are. Be patient, and invest the time in letting
	 * me work them out for myself. I really am quite intelligent you know. But
	 * when Grud thinks he has the right to destroy my planet, for money, then
	 * I find myself compelled to shoot the bastard. Twas ever thus. Sigh. I'll
	 * just stick to being a smartass then shall I. Peace works too ya know.
	 */
	public long m0; // long of 2*27=54 used bits
	public int m1; // int of 27 used bits (54+27=81 == GRID_SIZE)

	/** for {@link #hashCode() } */
	private int hashCode;

	/**
	 * Constructs a new empty Idx.
	 * <p>
	 * Design Note: There are few fields to initialise, so construction is fast
	 * compared to the previous array param set-up. But we no longer needlessly
	 * construct an Idx, anywhere in the code, so there is little or no gain,
	 * just more code handling m0,m1 that used to be an array. Sigh.
	 */
	public Idx() {
	}

	/**
	 * The Copy Constructor: Returns a new Idx containing a copy of src.
	 *
	 * @param src the source Idx to copy
	 */
	public Idx(final Idx src) {
		this.m0 = src.m0;
		this.m1 = src.m1;
	}

	/**
	 * Constructs a new Idx and if isFull then fills it with all of the cells
	 * in a Grid (0..80) by doing the fill() method in-line.
	 * <p>
	 * We currently only ever call this with true, but isFull might be handy
	 * down the track, so here it is.
	 * <p>
	 * NOTE: This constructor is currently used only in the test-cases, where
	 * its used to test other-stuff, hence its retained despite never being
	 * invoked in the actual system.
	 *
	 * @param isFull true to fill this idx, false leaves it empty.
	 */
	public Idx(final boolean isFull) {
		if ( isFull ) {
			m0 = BITS54L; // 54 1's
			m1 = BITS27; // 27 1's
		}
	}

	/**
	 * Constructor from raw array values.
	 *
	 * @param m0 mask 0: 27 used bits
	 * @param m1 mask 1: 27 used bits
	 */
	public Idx(final long m0, final int m1) {
		this.m0 = m0;
		this.m1 = m1;
	}

	/**
	 * Constructor from cells array.
	 *
	 * @param cells to add to this Idx
	 */
	public Idx(final Cell[] cells) {
		long m0=0L; int i, m1=0;
		for ( Cell cell : cells )
			if ( (i=cell.indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		this.m0 = m0; this.m1 = m1;
	}

	/**
	 * Constructor from cells collection.
	 *
	 * @param cells to add to this Idx
	 */
	public Idx(final Collection<Cell> cells) {
		long m0=0L; int i, m1=0;
		for ( Cell cell : cells )
			if ( (i=cell.indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		this.m0 = m0; this.m1 = m1;
	}

	/**
	 * Constructor from indices array.
	 *
	 * @param indices to add
	 * @param n number of indices to add
	 */
	public Idx(final int[] indices, final int n) {
		long m0=0L; int i, j, m1=0;
		for ( j=0; j<n; ++j )
			if ( (i=indices[j]) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		this.m0 = m0; this.m1 = m1;
	}

	/**
	 * Constructor for indice Set.
	 *
	 * @param indices
	 */
	public Idx(final Set<Integer> indices) {
		if ( indices instanceof Idx ) {
			m0 = ((Idx)indices).m0;
			m1 = ((Idx)indices).m1;
		} else {
			long m0=0L; int m1=0;
			for ( int i : indices )
				if ( i < FIFTY_FOUR )
					m0 |= MASKED81[i];
				else
					m1 |= MASKED[i];
			this.m0 = m0; this.m1 = m1;
		}
	}

	/**
	 * Constructor for array of masks.
	 *
	 * @param masks three ints that are my raw bitset mask values
	 * @param dummy says theses are mask values, not indices
	 */
	public Idx(final int[] masks, final boolean dummy) {
		m0 = masks[0];
		m1 = masks[1];
	}

	/**
	 * Constructs a new Idx containing the complement of this Idx,
	 * ergo all indices EXCEPT those in this Idx.
	 *
	 * @return a new Idx of all EXCEPT this Idx.
	 */
	Idx not() {
		return new Idx(BITS54L & ~m0, BITS27 & ~m1);
	}

	// ------------------------------- mutators -------------------------------

	/**
	 * Fill this Idx with all 81 indices.
	 *
	 * @return this Idx, for method chaining
	 */
	@Mutator
	public Idx fill() {
		m0 = BITS54L;
		m1 = BITS27;
		return this;
	}

	/**
	 * Read the $cells matching $filter into this Idx.
	 *
	 * @param cells source (usually grid.cells)
	 * @param filter a closure whose accept method determines which cells are
	 *  added to this Idx; FYI a "closure" is "code encapsulated by pattern"
	 * @return this Idx for method chaining
	 */
	@Mutator
	public Idx read(final Cell[] cells, final IFilter<Cell> filter) {
		long m0=0L; int m1=0, i=0;
		do
			if ( filter.accept(cells[i]) )
				m0 |= MASKED81[i];
		while (++i < FIFTY_FOUR);
		do
			if ( filter.accept(cells[i]) )
				m1 |= MASKED[i];
		while (++i < GRID_SIZE);
		this.m0 = m0; this.m1 = m1;
		return this;
	}

	/**
	 * Set this Idx to contain only the given indice.
	 *
	 * @param indice
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setIndice(final int indice) {
		if ( indice < FIFTY_FOUR ) {
			m0 = MASKED81[indice]; m1 = 0;
		} else {
			m0 = 0; m1 = MASKED[indice];
		}
		return this;
	}

	/**
	 * Set this Idx to contain only the given indices.
	 *
	 * @param indiceA
	 * @param indiceB
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setIndices(final int indiceA, final int indiceB) {
		m0 = m1 = 0;
		add(indiceA);
		add(indiceB);
		return this;
	}

	/**
	 * Set this Idx to contain only the given indices.
	 *
	 * @param indiceA
	 * @param indiceB
	 * @param indiceC
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setIndices(final int indiceA, final int indiceB, final int indiceC) {
		m0 = m1 = 0;
		add(indiceA);
		add(indiceB);
		add(indiceC);
		return this;
	}

	/**
	 * Set this Idx to contain only the given indices.
	 *
	 * @param indiceA
	 * @param indiceB
	 * @param indiceC
	 * @param indiceD
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setIndices(final int indiceA, final int indiceB, final int indiceC, final int indiceD) {
		m0 = m1 = 0;
		add(indiceA);
		add(indiceB);
		add(indiceC);
		add(indiceD);
		return this;
	}

	/**
	 * Set this = src,
	 * ergo copy src into this Idx.
	 *
	 * @param src
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final Idx src) {
		m0 = src.m0;
		m1 = src.m1;
		return this;
	}

	/**
	 * Set this Idx to the raw masks/bitsets: a, b.
	 * <p>
	 * This allows you to calculate an Idx using native ops, and set it into an
	 * Idx for storage, or to use Idxs more complex operations. This is da most
	 * heavily-used Idx operation, because it suits hammered code.
	 *
	 * @param m0 mask 0: 27 used bits
	 * @param m1 mask 1: 27 used bits
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx set(final long m0, final int m1) {
		this.m0 = m0;
		this.m1 = m1;
		return this;
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
		m0 = m1 = 0;
		for ( int i, j=0; j<n; ++j ) {
			if ( (i=cells[j].indice) < FIFTY_FOUR )
				m0 |= MASKED81[i];
			else
				m1 |= MASKED[i];
		}
		return this;
	}

	/**
	 * Set this = masks (a,b), and return any.
	 *
	 * @param m0 a 54bit long of the first 54 indices in the Idx
	 * @param m1 a 27bit int of the second 27 indices in the Idx
	 * @return not empty
	 */
	@Mutator
	public boolean setAny(final long m0, final int m1) {
		this.m0 = m0;
		this.m1 = m1;
		return (m0|m1) > 0L;
	}

	/**
	 * Set this = (idxA & idxB).
	 *
	 * @param idxA
	 * @param idxB
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAnd(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 & idxB.m0;
		m1 = idxA.m1 & idxB.m1;
		return this;
	}

	/**
	 * Set this = (idxA & idxB & idxC),
	 * ergo common to all three indexes.
	 *
	 * @param idxA
	 * @param idxB
	 * @param idxC
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAnd(final Idx idxA, final Idx idxB, final Idx idxC) {
		m0 = idxA.m0 & idxB.m0 & idxC.m0;
		m1 = idxA.m1 & idxB.m1 & idxC.m1;
		return this;
	}

	/**
	 * Set this = (idxA & idxB) and returns any,
	 * ergo common to both indexes.
	 *
	 * @param idxA
	 * @param idxB
	 * @return not empty
	 */
	@Mutator
	public boolean setAndAny(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 & idxB.m0;
		m1 = idxA.m1 & idxB.m1;
		return (m0|m1) > 0L;
	}

	/**
	 * Set this = (idxA & idxB & idxC) and returns any,
	 * ergo common to all three indexes.
	 *
	 * @param idxA
	 * @param idxB
	 * @param idxC
	 * @return not empty
	 */
	@Mutator
	public boolean setAndAny(final Idx idxA, final Idx idxB, final Idx idxC) {
		return ( (m0 = idxA.m0 & idxB.m0 & idxC.m0)
			   | (m1 = idxA.m1 & idxB.m1 & idxC.m1) ) > 0L;
	}

	/**
	 * Set this = (idxA & idxB) and returns size() > 1,
	 * ergo get the many that are common to both indexes.
	 *
	 * @param idxA
	 * @param idxB
	 * @return 2 or more
	 */
	@Mutator
	public boolean setAndMany(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 & idxB.m0;
		m1 = idxA.m1 & idxB.m1;
		return (m0|m1)>0L && ((m0>0L && m1>0) || size()>1);
	}

	/**
	 * Set this = (idxA | idxB),
	 * ergo set me to the union.
	 *
	 * @param idxA
	 * @param idxB
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setOr(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 | idxB.m0;
		m1 = idxA.m1 | idxB.m1;
		return this;
	}

	/**
	 * Set this = idxA & ~idxB,
	 * ergo set me to a-only elements.
	 *
	 * @param idxA
	 * @param idxB
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx setAndNot(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 & ~idxB.m0;
		m1 = idxA.m1 & ~idxB.m1;
		return this;
	}

	/**
	 * Set this = idxA & ~idxB, and return any(),
	 * ergo set me to a-only elements.
	 *
	 * @param idxA
	 * @param idxB
	 * @return are there any indices in this Idx
	 */
	@Mutator
	public boolean setAndNotAny(final Idx idxA, final Idx idxB) {
		m0 = idxA.m0 & ~idxB.m0;
		m1 = idxA.m1 & ~idxB.m1;
		return (m0|m1) > 0L;
	}

	/**
	 * Set this = idxA & ~idxB & ~idxC, and return any()
	 * ergo set me to a-only elements.
	 *
	 * @param idxA
	 * @param idxB
	 * @param idxC
	 * @return are there any indices in this Idx
	 */
	@Mutator
	public boolean setAndNotAny(final Idx idxA, final Idx idxB, final Idx idxC) {
		m0 = idxA.m0 & ~idxB.m0 & ~idxC.m0;
		m1 = idxA.m1 & ~idxB.m1 & ~idxC.m1;
		return (m0|m1) > 0L;
	}

	/**
	 * Clears this Idx, ie removes all indices.
	 *
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx clearMe() {
		m0 = m1 = 0;
		return this;
	}

	/**
	 * Clears this Idx, ie removes all indices.
	 */
	@Mutator
	@Override
	public void clear() {
		m0 = m1 = 0;
	}

	/**
	 * Add the given indice to this Idx.
	 * <p>
	 * SPEED: When adding many it is faster to build your masks locally and set
	 * the Idx ONCE. This reduces stack-work.
	 *
	 * @param i indice to add to this Idx
	 */
	@Mutator
	public void add(final int i) {
		assert i>-1 && i<81;
		if ( i < FIFTY_FOUR )
			m0 |= MASKED81[i];
		else
			m1 |= MASKED[i];
	}

	/**
	 * Remove the given indice from this Idx.
	 *
	 * @param i the indice to be removed from this Idx
	 */
	@Mutator
	public void remove(final int i) {
		assert i>-1 && i<81;
		if ( i < FIFTY_FOUR )
			m0 &= ~MASKED81[i];
		else
			m1 &= ~MASKED[i];
	}

	/**
	 * Visit: If has(i) then remove(i) and return true, else just return false.
	 *
	 * @param i the indice to remove, if present
	 * @return was i present (if so then he's gone now)
	 */
	@Mutator
	public boolean visit(int i) {
		if ( i < FIFTY_FOUR ) {
			if ( (m0 & MASKED81[i]) > 0L ) {
				m0 &= ~MASKED81[i];
				return true;
			}
		} else if ( (m1 & MASKED[i]) > 0 ) {
			m1 &= ~MASKED[i];
			return true;
		}
		return false;
	}

	/**
	 * Mutate this &= idx,
	 * ergo set this to the intersection of both sets.
	 *
	 * @param idx
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx and(final Idx idx) {
		m0 &= idx.m0;
		m1 &= idx.m1;
		return this;
	}

	/**
	 * Mutate this &= ~idx.
	 *
	 * @param idx
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx andNot(final Idx idx) {
		m0 &= ~idx.m0;
		m1 &= ~idx.m1;
		return this;
	}

	/**
	 * Mutate this |= idx
	 *
	 * @param idx
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx or(final Idx idx) {
		m0 |= idx.m0;
		m1 |= idx.m1;
		return this;
	}

	/**
	 * Mutate this |= idxA | idxB
	 *
	 * @param idxA
	 * @param idxB
	 * @return this Idx, for method chaining.
	 */
	@Mutator
	public Idx or(final Idx idxA, final Idx idxB) {
		m0 |= idxA.m0 | idxB.m0;
		m1 |= idxA.m1 | idxB.m1;
		return this;
	}

	// ----------------------- mutators that are queries ----------------------

	/**
	 * Remove and return the first (lowest) indice in this Set.
	 * <p>
	 * The poll() method is my invention. Its intent is to make the calling
	 * loop faster.
	 *
	 * @return the first (lowest) indice that is removed from this Set,
	 *  else (this Set is empty) return -1.
	 */
	@Mutator
	public int poll() {
		int x, y, n;
		if ( m0 > 0L ) {
			long l = m0 & -m0; // lowestOneBit
			m0 &= ~l; // remove l
			//return Long.numberOfTrailingZeros(m0);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		if ( m1 > 0 ) {
			x = m1 & -m1; // lowestOneBit
			m1 &= ~x; // remove x
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(m1);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		return QEMPTY;
	}

	// -------------------------------- queries -------------------------------

	/**
	 * The peek method returns the first (lowest) indice in this Idx (without
	 * removing it) without having to initialise an array.
	 *
	 * @return the value of the first (lowest) indice in this Idx
	 */
	public int peek() {
		int x, y, n;
		if ( m0 > 0L ) {
			long l = m0 & -m0; // lowestOneBit
			//return Long.numberOfTrailingZeros(l);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		if ( m1 > 0 ) {
			x = m1 & -m1; // lowestOneBit
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(b);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		return QEMPTY;
	}

	/**
	 * Return is this Idx empty?
	 *
	 * @return does this Idx contain no indices.
	 */
	public boolean none() {
		return (m0|m1) < 1L; // 54bits can NEVER be negative
	}

	/**
	 * Return is this Idx NOT empty?
	 *
	 * @return does this Idx contain any indices.
	 */
	public boolean any() {
		return (m0|m1) > 0L; // 54bits can NEVER be negative
	}

	/**
	 * Return the number of indices in this Idx.
	 * <p>
	 * PERFORMANCE: I have tried everything, and just bitCounting all three
	 * masks and adding them together seems to be the fastest. KISS it! The
	 * downside is that bitCount is a bit slow for the number of times it is
	 * invoked. An actual smart person might make quite a lot of money by
	 * implementing bitCount in firmware and selling it to PC manufacturers;
	 * to allow lazy programmers to performantly get-away with this s__t.
	 * Also 3*Integer.bitCount faster than 2*Long.bitCount hence int masks.
	 *
	 * @return {@code return Long.bitCount(a)+Integer.bitCount(b); }
	 */
	@Override
	public int size() {
//Inline for speed. Jury still out on whether or not it's actually any faster.
//I suspect MUCH slower until it gets compiled, and the more the bastard runs
//sans modification the better it seems to get at compiling s__t early. Guess
//the JVM is using stats to determine what to compile. New jar means new stats.
//		return Long.bitCount(a) + Integer.bitCount(b);
		//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
		// Long.bitCount(a)
		long l = m0;
        l = l - ((l >>> 1) & 0x5555555555555555L);
        l = (l & 0x3333333333333333L) + ((l >>> 2) & 0x3333333333333333L);
        l = (l + (l >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        l = l + (l >>> 8);
        l = l + (l >>> 16);
        l = l + (l >>> 32);
		// Integer.bitCount(b)
		int i = m1;
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return ((int)l & 0x7f) + (i & 0x3f);
	}

	/**
	 * Returns (this & a).any(), ie does this Idx intersect a.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(a).any()}.
	 *
	 * @param x
	 * @return is the intersection of this and other NOT empty.
	 */
	public boolean intersects(final Idx x) {
		return ( (m0 & x.m0) | (m1 & x.m1) ) > 0L;
	}

	/**
	 * Returns (this & a).none(), ie does this Idx NOT intersect a.
	 * <p>
	 * CAUTION: Query only! To mutate: {@code idx.and(a).none()}.
	 *
	 * @param x
	 * @return is the intersection of this and other empty.
	 */
	public boolean disjunct(final Idx x) {
		return ( (m0 & x.m0) | (m1 & x.m1) ) < 1L;
	}

	/**
	 * Does this Idx contain the given indice $i?
	 * <p>
	 * I am called "has" for brevity, formerly "contains".
	 *
	 * @param i
	 * @return true if this Idx contains the given indice.
	 */
	public boolean has(final int i) {
		if ( i < FIFTY_FOUR )
			return (m0 & MASKED81[i]) > 0L;
		return (m1 & MASKED[i]) > 0;
	}

	/**
	 * Return (this & x) == x.
	 * <pre>
	 * ie do I have everything in $x,
	 * ergo a containsAll plex,
	 * which I call hasAll, because I like sex.
	 * Yeah na, its just a short meaningful name.
	 * Dont overthink it. Just let it stick.
	 * </pre>
	 *
	 * @param x the other Idx
	 * @return does this Idx contain all indices in the other Idx?
	 */
	public boolean hasAll(final Idx x) {
		return (m0 & x.m0) == x.m0
			&& (m1 & x.m1) == x.m1;
	}

	/**
	 * Return (this & x) == this.
	 * <pre>
	 * ie has $x got all of this,
	 * ergo an isContainedBy bliss,
	 * which I call allIn, because I drink piss.
	 * Yeah na, its just a short meaningful name.
	 * Dont overthink it. Just let it stick.
	 * </pre>
	 *
	 * @param x the other Idx
	 * @return does the other Idx contain all indices in this Idx?
	 */
	public boolean allIn(final Idx x) {
		return (m0 & x.m0) == m0
			&& (m1 & x.m1) == m1;
	}

	/**
	 * When this Idx contains two indices, I return the one other than $i.
	 * <p>
	 * I do NOT mutate this Idx (remove then peek would mutate this Idx).
	 * <p>
	 * This method should only be called on an Idx containing two indices. I am
	 * untested on Idxs containing other than two indices, but here is what I
	 * think it returns:<ul>
	 * <li>When I contain ONE indice -&gt; first other than i, or 32 when $i
	 *  is my only indice.
	 * <li>When I contain more than two indices -&gt; first other than i
	 * <li>When $i is not one of my indices -&gt; first
	 * <li>Caveat emptor: all these statements are untested.
	 * </ul>
	 *
	 * @param indice
	 * @return the other indice in this Idx
	 */
	public int otherThan(final int indice) {
		assert indice>-1 && indice<81;
		// a mutable local copy of this Idx
		long m0=this.m0; int m1=this.m1;
		// remove indice
		if ( indice < FIFTY_FOUR )
			m0 &= ~MASKED81[indice];
		else
			m1 &= ~MASKED[indice];
		// peek
		int x, y, n;
		if ( m0 > 0L ) {
			long l = m0 & -m0; // lowestOneBit
			//return Long.numberOfTrailingZeros(m0);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		if ( m1 > 0 ) {
			x = m1 & -m1; // lowestOneBit
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(m1);
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			return n - ((x<<1)>>>31);
		}
		return QEMPTY; // empty
	}

	// ----------------------------- toArray -----------------------------

	/**
	 * Returns the given array (which is presumed to be of sufficient size)
	 * populated with the indices in this Idx.
	 * <p>
	 * The result array is NOT cleared first. It is presumed new or empty;
	 * or you want to just overwrite the existing contents. The given array
	 * must be atleast as large as this.size(), or you will get an
	 * ArrayIndexOutOfBoundsException.
	 *
	 * @param result an int-array of length &gt;= this.size()
	 * @return the given array, for method chaining.
	 */
	public int[] toArray(final int[] result) {
		Idx.toArray(m0, m1, result);
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
		// There is ONE 0-length array coz it's immutable.
		if ( (m0|m1) < 1L )
			return THE_EMPTY_INT_ARRAY;
		final int[] result = new int[size()];
		Idx.toArray(m0, m1, result);
		return result;
	}

	/**
	 * Read indices in this Idx into the given array, and return how many, so
	 * that you can use a re-usable (fixed-size) array.
	 *
	 * @param result must be large enough to hold size()
	 * @return the number of elements added (ergo idx.size())
	 */
	public int toArrayN(final int[] result) {
		return Idx.toArray(m0, m1, result);
	}

	/**
	 * Returns a new boolean[GRID_SIZE] with true meaning indice in this Idx.
	 * Exists to expedite has/contains/sees operations where hammered.
	 *
	 * @return an array of 81 booleans where true means indice is in this Idx
	 */
	public boolean[] toBooleans() {
		assert (m0|m1) > 0;
		final boolean[] result = new boolean[GRID_SIZE];
		long l, m0=this.m0;
		int x, y, n, m1=this.m1;
		while ( m0 > 0L ) {
			l = m0 & -m0; // lowestOneBit
			m0 &= ~l; // remove l
			//return Long.numberOfTrailingZeros(l); // inline for speed
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 63;
			y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
			y= x<<16; if(y!=0){n-=16; x=y;}
			y= x<< 8; if(y!=0){n-= 8; x=y;}
			y= x<< 4; if(y!=0){n-= 4; x=y;}
			y= x<< 2; if(y!=0){n-= 2; x=y;}
			result[n - ((x<<1)>>>31)] = true;
		}
		while ( m1 > 0 ) {
			x = m1 & -m1; // lowestOneBit
			m1 &= ~x; // remove x
			//return FIFTY_FOUR + Integer.numberOfTrailingZeros(i); // inline for speed
			//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
			n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
			y=x<<16; if(y!=0){n-=16; x=y;}
			y=x<< 8; if(y!=0){n-= 8; x=y;}
			y=x<< 4; if(y!=0){n-= 4; x=y;}
			y=x<< 2; if(y!=0){n-= 2; x=y;}
			result[n - ((x<<1)>>>31)] = true;
		}
		return result;
	}

	// ----------------------------- to Set -----------------------------

	/**
	 * Get cells of the indices in this Idx.
	 *
	 * @param gridCells to read cells from
	 * @return a {@code LinkedHashSet<Cell>} of the cells in this Idx.
	 */
	public Set<Cell> toCellSet(final Cell[] gridCells) {
		final Set<Cell> result = new LinkedHashSet<>(size(), 1F);
		int i;
		for ( final IntQueue q=new MyIntQueue(m0,m1); (i=q.poll())>QEMPTY; )
			result.add(gridCells[i]);
		return result;
	}

	/**
	 * Returns the RIBS (Regions Indexes BitSet) of regions that are common to
	 * all indices/cells in this Idx.
	 * <p>
	 * <b>WARNING</b>: Common is a concept that applies to two or more things.
	 * When called on an Idx that contains less then two indices commonRibs
	 * just returns 0, meaning NONE.
	 *
	 * @return RIBS (Regions Indexes BitSet) is a 27 bit bitset, one for each
	 *  index in the Grid.regions array.
	 */
	public int commonRibs() {
		int ribs = 0;
		if ( size() > 1 ) { // common applies to two or more cells.
			int i;
			final IntQueue q = new MyIntQueue(m0, m1);
			if ( (i=q.poll()) > QEMPTY ) {
				ribs = RIBS[i];
				while ( (i=q.poll()) > QEMPTY )
					ribs &= RIBS[i];
			}
		}
		return ribs;
	}

	// --------------------------------- cells --------------------------------

	/**
	 * Repopulate $result with $source cells whose indices are in this Idx.
	 * <p>
	 * Use this one with the right sized array, which is returned.
	 * <p>
	 * Use cellsN if you need know how many cells are added to the array.
	 * Be warned that the passed array is NOT null terminated (as you might
	 * reasonably expect, from experience, sigh).
	 *
	 * @param gridCells cells to read, typically grid.cells
	 * @param result the destination array to populate
	 * @return the given $result cells array
	 */
	public Cell[] cells(final Cell[] gridCells, final Cell[] result) {
		int i, count = 0;
		for ( final IntQueue q=new MyIntQueue(m0,m1); (i=q.poll())>QEMPTY; )
			result[count++] = gridCells[i];
		return result;
	}

	/**
	 * Returns a new array $cells in this Idx. Note that creating a new
	 * array takes time, so cellsN(grid, reusableArray) is a bit faster.
	 *
	 * @param cells to read
	 * @return a new {@code Cell[]} containing the cells in this Idx.
	 */
	public Cell[] cellsNew(final Cell[] cells) {
		return cells(cells, new Cell[size()]);
	}

	// NOTE: The ids methods exist mainly to avoid having to get the grid in
	// order to get the cells just to turn those cells into ids, which are a
	// static attribute of the Grid, so we do NOT need a grid instance as long
	// as its all done here, I think, where its convenient, except that we are
	// imbedding formatting concerns in the model, which is bad practice, but
	// then theyre all the way through the grid, and the test-cases, so stuff
	// it, lets just hack something together that works then let smarty-pants
	// design gurus worry about design when stuff works. Rome was not designed
	// in a day either. In fact it was just cobbled together, and look how that
	// worked out for them. sigh. Robbing Peter to pay Paul works great right
	// up until it does not, and Peter shows up at your house, burns it down,
	// shoots you and your missus and rapes the ass of your kids (gender
	// non-specifically, of course; We are the MODERN hun) and generally makes
	// a mess out of everything, coz you have made a mess out of everything.
	// Sigh.
	public StringBuilder idsSB(final StringBuilder sb, final int n
			, final String sep, final String lastSep) {
		int i, count=0;
		final int m = n - 1;
		final IntQueue q = new MyIntQueue(m0,m1);
		if ( (i=q.poll()) > QEMPTY ) {
			sb.append(CELL_IDS[i]);
			while ( (i=q.poll()) > QEMPTY ) {
				sb.append(++count==m ? lastSep : sep);
				sb.append(CELL_IDS[i]);
			}
		}
		return sb;
	}
	public StringBuilder idsSB(final String sep, final String lastSep) {
		final int n = size();
		final int capacity = (CELL_ID_LENGTH+sep.length())*n
				+ Math.max(lastSep.length() - sep.length(), 0);
		return idsSB(SB(capacity), n, sep, lastSep);
	}
	public String ids(final String sep, final String lastSep) {
		return idsSB(sep, lastSep).toString();
	}
	public String ids(final String sep) {
		return idsSB(sep, sep).toString();
	}

	/**
	 * Returns SSV (Space Separated Values) of the ids of cells in this Idx.
	 *
	 * @return SSV of my cell-ids.
	 */
	public String ids() {
		return idsSB(" ", " ").toString();
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Returns a String representation of this Idx.
	 *
	 * @return
	 */
	@Override
	public String toString() {
		if ( true ) // @check true
			// cell-ids toString for normal use
			return Idx.toStringCellIds(m0, m1);
		// debug only: the raw indices
		return Idx.toStringIndices(m0, m1);
	}

	/**
	 * Returns Comma Separated Values of CELL_IDS of the cells in this Idx.
	 *
	 * @return a CSV String of the cells in this Idx.
	 */
	public String csv() {
		final StringBuilder sb = SB(size()<<2); // * 4
		int i;
		final IntQueue it = new MyIntQueue(m0,m1);
		if ( (i=it.poll()) > QEMPTY ) {
			sb.append(CELL_IDS[i]);
			while ( (i=it.poll()) > QEMPTY )
				sb.append(", ").append(CELL_IDS[i]);
		}
		return sb.toString();
	}

	/**
	 * Returns the default order for Idx: first cell comes first. <br>
	 * ie: The earlier my first indice, the sooner I come in the list.
	 * <p>
	 * First compare Mask 'a' low 27, then top 27 ('a' is a 54 bit long),
	 * then compare the 27 bits in 'b' ('b' is a 27 bit int);
	 * <p>
	 * NOTE: Looks like I'm unused, or used rarely. Difficult to say.
	 * But if an Idx ever goes in a TreeSet/Map I will be used. Hence
	 * I am retained despite not being used (by the debugger method).
	 *
	 * @param other Idx to compare this Idx to
	 * @return this compared to other for order
	 */
	@Override
	public int compareTo(final Idx other) {
		int diff;
		// if the low-order 27 bits of 'a' are equal
		if ( (diff=(int)(this.m0&BITS27 - other.m0&BITS27)) == 0
		  // and the high-order 27 bits of 'a' are equal
		  && (diff=(int)(this.m0>>27 - other.m0>>27)) == 0 )
			// then compare 'b'
			diff = this.m1 - other.m1;
		return diff;
	}

	/**
	 * Note that the returned hashCode is cached, so it will NOT change after
	 * its gotten, and so will not reflect any changes in this Idx; breaking
	 * the {@code equals <-> hashCode} contract; so <b>DO NOT</b> modify an
	 * Idx AFTER adding it to a hash-anything. Which, luckily enough, is how
	 * things typically work anyway: input, process, output.
	 *
	 * @return
	 */
	@Override
	public int hashCode() {
		if ( hashCode == 0 )
			hashCode = hashCode(m0, m1);
		return hashCode;
	}

	/**
	 * Does this equal the given Object.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(final Object o) {
		if ( o instanceof Idx ) {
			final Idx other = (Idx)o;
			return other.m0==m0 && other.m1==m1;
		}
		return false;
	}

	/**
	 * Return does this == other.
	 *
	 * @param o other Idx
	 * @return {@code a==o.a && b==o.b}
	 */
	public boolean equals(final Idx o) {
		return m0==o.m0 && m1==o.m1;
	}

	// ============================= Set<Integer> =============================

	private static boolean illegalArgumentType(final Object o) {
		throw new IllegalArgumentException("Unknown type: "+(o==null?"null":o.getClass().getCanonicalName()));
	}

	@Override
	public boolean isEmpty() {
		return (m0|m1) == 0;
	}

	@Override
	public boolean contains(final Object o) {
		if (o instanceof Integer)
			return has((Integer)o);
		if (o instanceof Idx)
			return hasAll((Idx)o);
		return illegalArgumentType(o);
	}

	@Override
	public Object[] toArray() {
		return IntArray.integerArray(toArrayNew());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(final T[] array) {
		int count=0, i;
		for ( final IntQueue q=new MyIntQueue(m0,m1); (i=q.poll())>QEMPTY; )
			//Ignore IDE warning: unecessary boxing. It is necessary!
			array[count++] = (T)Integer.valueOf(i);
		return array;
	}

	@Override
	public boolean add(final Integer e) {
		final int i = e;
		final boolean ret = !has(i);
		add(i);
		return ret;
	}

	@Override
	public boolean remove(final Object o) {
		if ( o instanceof Integer ) {
			final int i = (Integer)o;
			final boolean ret = has(i);
			remove(i);
			return ret;
		}
		if ( o instanceof String ) {
			final int i = Grid.indice((String)o);
			final boolean ret = has(i);
			remove(i);
			return ret;
		}
		if ( o instanceof Idx ) {
			return removeAll((Idx)o);
		}
		if ( o instanceof Collection ) {
			return removeAll((Collection) o);
		}
		return illegalArgumentType(o);
	}

	public static Object first(final Collection<?> col) {
		try {
			return col.iterator().next();
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public boolean containsAll(final Collection<?> col) {
		if ( col instanceof Idx )
			return hasAll((Idx)col);
		final Object f;
		if ( (f=first(col)) instanceof Integer )
			return hasAll(Idx.of(col));
		if ( f instanceof String ) {
			if ( !has(Grid.indice((String)f)) )
				return false;
			for ( Object o : col )
				if ( !has(Grid.indice((String)o)) )
					return false;
			return true;
		}
		return illegalArgumentType(f);
	}

	@Override
	public boolean addAll(final Collection<? extends Integer> col) {
		if ( col instanceof Idx ) {
			final int pre = size();
			or((Idx)col);
			return size() > pre;
		}
		final Object f;
		if ( (f=first(col)) instanceof Integer ) {
			final int pre = size();
			or(Idx.of(col));
			return size() > pre;
		}
		if ( f instanceof String ) {
			final int pre = size();
			or(Idx.of(col));
			return size() > pre;
		}
		return illegalArgumentType(f);
	}

	@Override
	public boolean retainAll(final Collection<?> keepers) {
		if ( keepers instanceof Idx ) {
			final int pre = size();
			and((Idx)keepers);
			return size() < pre;
		}
		final Object f;
		if ( (f=first(keepers)) instanceof Integer
		  || f instanceof String ) {
			final int pre = size();
			and(Idx.of(keepers)); // reads either
			return size() < pre;
		}
		return illegalArgumentType(f);
	}

	@Override
	public boolean removeAll(final Collection<?> col) {
		if ( col instanceof Idx ) {
			final int pre = size();
			andNot((Idx)col);
			return size() < pre;
		}
		final Object f;
		if ( (f=first(col)) instanceof Integer ) {
			int i; boolean result = false;
			for ( Object o : col ) {
				i = (Integer)o;
				result |= has(i);
				remove(i);
			}
			return result;
		}
		if ( f instanceof String ) {
			int i; boolean result = false;
			for ( Object o : col ) {
				i = Grid.indice((String)o);
				result |= has(i);
				remove(i);
			}
			return result;
		}
		return illegalArgumentType(f);
	}

	@Override
	public void forEach(Consumer<? super Integer> action) {
		for ( int indice : this.toArrayNew() )
			action.accept(indice);
	}

	/**
	 * Returns a new {@code Iterator<Integer>} over the indices in this Idx.
	 * <p>
	 * <b>NOTE WELL</b>: The {@link #indices() IntQueue} achieves the same ends
	 * as an Iterator, except it makes ONE call per indice, and does NOT box
	 * and unbox each int into an Integer, so it's faster. Iterator is NOT the
	 * preferred option peeps!
	 *
	 * @return a new Iterator, which is NOT the preferred option!
	 */
	@Override
	public Iterator<Integer> iterator() {
//		if ( Debug.isClassNameInTheCallStack(12, Medusa3D.class.getName()) )
//			Debug.breakpoint();
		return new MyIterator(m0, m1);
	}

	private static class MyIterator implements Iterator<Integer> {
		private long m0;
		private int m1;
		public MyIterator(final long m0, final int m1) {
			this.m0 = m0;
			this.m1 = m1;
		}
		@Override
		public boolean hasNext() {
			return (m0|m1) > 0L;
		}
		@Override
		public Integer next() {
			int x, y, n;
			if ( m0 > 0L ) {
				long l = m0 & -m0; // lowestOneBit
				m0 &= ~l; // remove l
				//return Long.numberOfTrailingZeros(l); // inline for speed
				//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
				n = 63;
				y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
				y= x<<16; if(y!=0){n-=16; x=y;}
				y= x<< 8; if(y!=0){n-= 8; x=y;}
				y= x<< 4; if(y!=0){n-= 4; x=y;}
				y= x<< 2; if(y!=0){n-= 2; x=y;}
				return n - ((x<<1)>>>31);
			}
			if ( m1 > 0 ) {
				x = m1 & -m1; // lowestOneBit
				m1 &= ~x; // remove x
				//return FIFTY_FOUR + Integer.numberOfTrailingZeros(i); // inline for speed
				//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
				n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
				y=x<<16; if(y!=0){n-=16; x=y;}
				y=x<< 8; if(y!=0){n-= 8; x=y;}
				y=x<< 4; if(y!=0){n-= 4; x=y;}
				y=x<< 2; if(y!=0){n-= 2; x=y;}
				return n - ((x<<1)>>>31);
			}
//			return new NoSuchElementException();
			return null;
		}
	}

	/**
	 * Returns an {@link IntQueue} of the indices in this Idx.
	 *
	 * @return a new {@link MyIntQueue}
	 */
	public IntQueue indices() {
		return new MyIntQueue(m0, m1);
	}

	/**
	 * MyIntQueue implements the {@link IntQueue} interface to iterate the
	 * indices in the given virtual Idx(m0,m1). It exists simply because
	 * {@code Iterator<Integer>} is a bit slow Redge. It's under the bonnet
	 * son. Avery Iterator requires two invocations per element, and O(n*2)
	 * is always slower than O(n), hence IntQueue is a queue, exposing only
	 * the {@link #poll()} method, hence there is ONE invocation per element.
	 * Further, the no-natives nature of Java require that each int, which I
	 * have, and my user requires, must be wrapped in an instance of the
	 * Integer class, which are atleast cached (Sigh), but both boxing and
	 * unboxing the int value take time, which soon adds-up when you are
	 * playing with literally HUNDREDS OF BILLIONS of indices.
	 * <p>
	 * The upshot is that MyIntQueue beats MyIterator over top1465 by in the
	 * order of 10 seconds, which is SUBSTANTIAL given that we're talking 40
	 * seconds verses 30 seconds. So let that be a lesson to the java-guys.
	 * Sometimes your s__t just ____ing stinks! Interator FAIL. No wuckers.
	 * We just go around it.
	 */
	public static class MyIntQueue implements IntQueue {
		private long m0; private int m1;
		public MyIntQueue(final long m0, final int m1) {
			this.m0 = m0;
			this.m1 = m1;
		}
		@Override
		public int poll() {
			int x, y, n;
			if ( m0 > 0L ) {
				long l = m0 & -m0; // lowestOneBit
				m0 &= ~l; // remove l
				//return Long.numberOfTrailingZeros(l);
				//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
				n = 63;
				y=(int)l; if(y!=0){n-=32; x=y;} else x=(int)(l>>>32);
				y= x<<16; if(y!=0){n-=16; x=y;}
				y= x<< 8; if(y!=0){n-= 8; x=y;}
				y= x<< 4; if(y!=0){n-= 4; x=y;}
				y= x<< 2; if(y!=0){n-= 2; x=y;}
				return n - ((x<<1)>>>31);
			}
			if ( m1 > 0 ) {
				x = m1 & -m1; // lowestOneBit
				m1 &= ~x; // remove x
				//return FIFTY_FOUR + Integer.numberOfTrailingZeros(i);
				//Henry S Warren Jr, Hacker's Delight, (Addison Wesley, 2002).
				n = 85; // 85 is 31 (from nOTZ) + 54 (start indice of m1)
				y=x<<16; if(y!=0){n-=16; x=y;}
				y=x<< 8; if(y!=0){n-= 8; x=y;}
				y=x<< 4; if(y!=0){n-= 4; x=y;}
				y=x<< 2; if(y!=0){n-= 2; x=y;}
				return n - ((x<<1)>>>31);
			}
			return QEMPTY; // this set is empty
		}
	}

}
