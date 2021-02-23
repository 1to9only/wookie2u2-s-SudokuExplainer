/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.solver.hinters.align2.AlignedExclusion.CellStackEntry;
import diuf.sudoku.utils.LongLongHashMap;


/**
 * The NonHinters class encapsulates total maybes that are known to NOT hint.
 * This relies on maybes only being removed from the cells (not added back-in)
 * so when any of the maybes in any of the cells is removed the total changes,
 * so we must re-examine these cells. If everything is the same as it was last
 * time, and we did not hint the last time, then it's safe to assume that we
 * won't hint this time, so we can skip this search, to save some time.
 * <p>
 * A9E currently takes a DAY (ie far too long to be useful), so speed it up.
 * <p>
 * KRC 2020-12-15 Exhumed from AAlignedSetExclusionBase into own class, and
 * now pass in the "native" AlignedExclusion.CellStackEntry array, instead of
 * translating that into a Cell[] just for my purposes.
 * <p>
 * KRC 2020-12-16 Use LongLongHashMap so that shift is always 4 to eradicate
 * mass-collisions in A8+E when the uniqueness of a 32 bit hashCode gives way
 * under pressure. It's a bit (not a lot) faster, which is still too slow!
 *
 * @author Keith Corlett 2020-12-06 (IIRC) created
 */
class NonHinters {
	private long hashCode; // hashCode
	private long totalMaybes; // total maybes
	// nb: Use a LongLongHashMap to cater for larger A*E sets, for which a 32
	// bit hashCode is simply too small. totalMaybes is always int-sized but
	// bugger it: LongLong it is, coz I'm too lazy to write a LongIntHashMap
	// just for this.
	private final LongLongHashMap store; // map hashCode => totalMaybes
	private final int shift; // number of bits to left-shift hashCode

	/**
	 * Construct a new NonHinters.
	 *
	 * @param capacity the size of the totalMaybes LongLongHashMap.
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
	 * do is total the maybes.bits (a bitset), which is a bit cheeky but
	 * works because maybes are only ever removed from the cells (ie they
	 * are never added back-in) so a total of the maybes is sufficient to
	 * workout if a maybe has been removed from any of the cells in this
	 * set since the last time we examined them... so the total maybes of
	 * a set of cells can and does serve as its modification count.
	 * <p>
	 * The result is that each combo that doesn't hint is checked ONCE.
	 * <p>
	 * Note that we rely upon the LongLongHashMap class KRC wrote for HoDoKu,
	 * which is a (simplified) {@code HashMap<int, int>} so we do not need
	 * to create millions of Integers, so it's a bit faster.
	 * <p>
	 * 2020-12-07 tried skipping the skipper on the first pass of the grid,
	 * where we (pretty obviously) never skip, but it was actually slower.
	 *
	 * @param cellStack the CellStackEntry array
	 * @param size the degree (the number of cells that need to align in order
	 *  to form an aligned set: 2..10)
	 * @return should we skip searching the cells in the given stack
	 */
	boolean skip(final CellStackEntry[] cellStack, final int size) {
		// calculate my hashCode and totalMaybes
		// and remember these for the presumed future call to put
		// the cellStack always contains atleast 2 cells; it is NEVER empty!
		Cell c = cellStack[0].cell;
		long hc = c.hashCode; // hashCode
		long mb = c.maybes.bits; // totalMaybes
		for ( int i=1; i<size; ++i ) {
			c = cellStack[i].cell;
			// NOTE: shift is set by my constructor; it varies for $degree
			hc = (hc<<shift) ^ c.hashCode;
			mb += c.maybes.bits;
		}
		this.hashCode = hc;
		this.totalMaybes = mb;
		// now return is the totalMaybes unchanged since last time we saw them;
		// else (virgin cells) then get returns NOT_FOUND (-1) which is NEVER
		// equal to the current total maybes, so skip returns false.
		// BFIIK: That I tried skipping get in first getHints on each puzzle,
		// when it always returns -1 coz they're all "virgins"; but was SLOWER,
		// I think get not JIT compiled, so all the subsequent calls slower.
		return store.get(hc) == mb; // ie stored mb == current mb
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
