/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.utils.LongLongHashMap;
import static diuf.sudoku.solver.hinters.align.AlignedExclusion.CellStackEntry;

/**
 * The NonHinters class encapsulates total maybes that are known to NOT hint.
 * This relies on maybes only being removed from the cells (not added back-in)
 * so when any of the maybes in any of the cells is removed the total changes,
 * so we must re-examine these cells. If everything is the same as it was last
 * time, and we did not hint the last time, then it is safe to assume that we
 * will not hint this time, so we can skip this search, to save some time.
 * <p>
 * A9E currently takes a DAY (ie far too long to be useful), so speed it up.
 * <hr>
 * <p>
 * KRC 2020-12-15 Exhumed from AAlignedSetExclusionBase into own class, and
 * now pass in the "native" AlignedExclusion.CellStackEntry array, instead of
 * translating that into a Cell[] just for my purposes.
 * <p>
 * KRC 2020-12-16 Use LongLongHashMap so that shift is always 4 to eradicate
 * mass-collisions in A8+E when the uniqueness of a 32 bit hashCode gives way
 * under pressure. It is a bit (not a lot) faster, which is still too slow!
 * <p>
 * KRC 2023-06-13 13-34-42 This run is WITHOUT NonHinters. All HACKED permntly.
 * <pre>
 *        time(ns)  calls  time/call elims     time/elim hinter
 *      24,260,500 119782        202 66220           366 NakedSingle
 *      20,555,500  53562        383 16492         1,246 HiddenSingle
 *      91,169,100  37070      2,459 25279         3,606 Locking
 *     100,304,600  25182      3,983  8435        11,891 NakedPair
 *      76,633,800  22939      3,340  8563         8,949 HiddenPair
 *     127,321,400  20777      6,127  1802        70,655 NakedTriple
 *     117,976,200  20331      5,802  1076       109,643 HiddenTriple
 *      60,853,600  20112      3,025   636        95,681 Swampfish
 *      98,248,000  19859      4,947   744       132,053 XY_Wing
 *      72,241,200  19347      3,733   338       213,731 XYZ_Wing
 *      83,199,300  19029      4,372   274       303,647 Swordfish
 *     121,706,300  18955      6,420    25     4,868,252 Jellyfish
 *     791,236,300  18951     41,751    37    21,384,764 AlignedPair
 *   9,344,100,900  18914    494,030   673    13,884,250 AlignedTriple
 *  54,882,919,800  18298  2,999,394   771    71,184,072 AlignedQuad
 * 188,482,752,800  17630 10,691,023   692   272,373,920 AlignedPent
 * 406,399,958,700  17027 23,867,971   470   864,680,763 AlignedHex
 * 602,521,921,800  16626 36,239,740   225 2,677,875,208 AlignedSept    2868
 *  29,531,427,700  16427  1,797,737 29276     1,008,724 UnaryChain
 *  14,915,174,700   8895  1,676,804  1376    10,839,516 NishioChain
 *  12,503,878,500   7771  1,609,043 12032     1,039,218 MultipleChain
 *   2,230,565,900   2793    798,627  6416       347,656 DynamicChain
 *      21,870,700      4  5,467,675    22       994,122 DynamicPlus
 * 1465  1,367,827,196,600 (22:47)  933,670,441
 *
 * 14-01-54 Verses this run WITH NonHinters (on what is now a hot box, soz)
 *        time(ns)  calls  time/call elims     time/elim hinter
 *      32,782,800 119782        273 66220           495 NakedSingle
 *      28,144,000  53562        525 16492         1,706 HiddenSingle
 *     121,301,700  37070      3,272 25279         4,798 Locking
 *     132,593,300  25182      5,265  8435        15,719 NakedPair
 *     107,687,000  22939      4,694  8563        12,575 HiddenPair
 *     177,012,700  20777      8,519  1802        98,231 NakedTriple
 *     159,732,400  20331      7,856  1076       148,450 HiddenTriple
 *      80,094,300  20112      3,982   636       125,934 Swampfish
 *     132,670,000  19859      6,680   744       178,319 XY_Wing
 *      97,667,100  19347      5,048   338       288,955 XYZ_Wing
 *     109,917,200  19029      5,776   274       401,157 Swordfish
 *     170,296,800  18955      8,984    25     6,811,872 Jellyfish
 *     930,829,200  18951     49,117    37    25,157,545 AlignedPair
 *   8,755,743,000  18914    462,923   671    13,048,797 AlignedTriple
 *  36,641,784,000  18300  2,002,283   773    47,402,049 AlignedQuad
 * 105,520,439,100  17630  5,985,277   692   152,486,183 AlignedPent
 * 216,476,456,800  17027 12,713,716   470   460,588,205 AlignedHex
 * 322,330,084,200  16626 19,387,109   225 1,432,578,152 AlignedSept    2868
 *  39,910,695,100  16427  2,429,579 29276     1,363,256 UnaryChain
 *  19,604,693,000   8895  2,204,012  1376    14,247,596 NishioChain
 *  16,974,897,300   7771  2,184,390 12032     1,410,812 MultipleChain
 *   2,994,646,900   2793  1,072,197  6416       466,746 DynamicChain
 *      35,043,300      4  8,760,825    22     1,592,877 DynamicPlus
 * 1465  824,622,209,900 (13:44)  562,882,054
 * So with NonHinter takes 60.29% of the time, on a machine thats running ~40%
 * slower (by NakedSingle and Jellyfish times), to produce 100% of the hints.
 * Bargain! That is quite a gain from so little code (excl LongLongHashMap).
 * </pre>
 *
 * @author Keith Corlett 2020-12-06 (IIRC) created
 */
class NonHinters {
	private long hashCode; // hashCode
	private long totalMaybes; // total maybes
	// nb: Use a LongLongHashMap to cater for larger A*E sets, for which a 32
	// bit hashCode is too small. totalMaybes is int-sized but bugger it, Long
	// Long it is, coz I am too lazy to write a LongIntHashMap just for this.
	private final LongLongHashMap store; // map hashCode => totalMaybes
	private final int shift; // number of bits to left-shift hashCode

	// used for reporting in AlignedExclusion
	int count;

	/**
	 * Construct a new NonHinters.
	 *
	 * @param capacity the size of the totalMaybes LongLongHashMap. Note that
	 *  unlike a java.util maps this does not grow, so whatever you give me is
	 *  all I will ever have, despite increased collisions on a too-small map.
	 *  Measuring number of collisions would be a quite interesting exercise in
	 *  a CS course. Doing so generically is a challenge for your professor.
	 * @param shift the number of bits to left-shift the hashCode.<br>
	 *  Left-shifting 3 caters for 8 cells in a set: 3 * 8 = 24 and
	 *  cell.hashCode is 8 bits, so 24 + 8 = 32 = perfect.<br>
	 *  NOTE: hashCodes are usually INTENDED to be lossy, but here we rely
	 *  totally on the hashCode, so any collisions are ACTUAL collisions;
	 *  ie two distinct sets of cells which produce the same hashCode are
	 *  treated as one, for speed.
	 */
	NonHinters(int capacity, int shift) {
		this.store = new LongLongHashMap(capacity);
		this.shift = shift;
	}

	/**
	 * Can we skip searching this combo, ie are these cells (in there
	 * current state) already known to NOT hint.
	 * <p>
	 * By "current state" I mean we must recheck each set of cells each
	 * time any of the maybes in any of those cells is removed, so all we
	 * do is total the maybes (a bitset), which is a bit cheeky but
	 * works because maybes are only ever removed from the cells (ie they
	 * are never added back-in) so a total of the maybes is sufficient to
	 * workout if a maybe has been removed from any of the cells in this
	 * set since the last time we examined them... so the total maybes of
	 * a set of cells can and does serve as its modification count.
	 * <p>
	 * The result is that each combo that does not hint is checked ONCE.
	 * <p>
	 * Note that we rely upon the LongLongHashMap class KRC wrote for HoDoKu,
	 * which is a (simplified) {@code HashMap<int, int>} so we do not need
	 * to create millions of Integers, so it is a bit faster.
	 * <p>
	 * 2020-12-07 tried skipping the skipper on the first pass of the grid,
	 * where we (pretty obviously) never skip, but it was actually slower.
	 *
	 * @param cellStack the CellStackEntry array
	 * @param degree the number of cells (2..10) in an aligned set
	 * @param numExcls the number of excluder-cells whose maybes are
	 * currently in the AlignedExclusion.EXCLUDERS_MAYBES array.
	 * @param firstPass is this the first run of this instance of AE through
	 *  this puzzle?
	 * @return should we skip searching the cells in the given stack
	 */
	boolean skip(final Cell[] gridCells, final CellStackEntry[] cellStack
			, final int degree, final int numExcls, final boolean firstPass
			, final int[] excludersMaybes) {
		// calculate hashCode and totalMaybes of cellStack, and
		// remember these for the presumed future call to put.
		// nb: the cellStack always contains atleast 2 cells.
		Cell c = gridCells[cellStack[0].indice];
		long hc = c.indice; // hashCode
		long tm = c.maybes; // totalMaybes
		for ( int i=1; i<degree; ++i ) {
			c = gridCells[cellStack[i].indice];
			hc = (hc<<shift) ^ c.indice;
			tm += c.maybes;
		}
		// if the number of excluders has changed
		// or any of there maybes have changed
		// then re-examine this aligned-set.
		for ( int i=0; i<numExcls; ++i )
			tm += excludersMaybes[i];
		this.hashCode = hc;
		this.totalMaybes = tm;
		if ( firstPass ) {
			// do store.get 100 times accross all AEs to JIT compile it
			// then do not bother (I always return false on first pass).
			if ( ++count > 100 )
				return false;
			return store.get(hc) == tm; // ie stored mb == current mb
		}
		// now return is the totalMaybes the same as the last time we examined
		// these cells;
		// else (virgin cells) then get returns NOT_FOUND (-1) which is NEVER
		// equal to the current total maybes, so skip returns false.
		// BFIIK: I tried skipping get in first getHints on each puzzle, when
		// it always returns -1 coz they are all "virgins"; but was SLOWER, so
		// I think get not JIT compiled, so all the subsequent calls slower, so
		// now we have above cluster____ caused by conflicting over-achievers.
		return store.get(hc) == tm; // ie stored tm == current tm
	}

	// put is called after skip when we do NOT hint, coz either hc was not in
	// in the map (a "virgin"), or the cells maybes have changed, so we update
	// the storedMb with the new totalMaybes (mb).
	void put() {
		store.put(hashCode, totalMaybes);
	}

	void clear() {
		store.clear();
	}

	int size() {
		return store.size();
	}

}
