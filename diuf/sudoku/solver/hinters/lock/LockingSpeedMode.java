/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Idx.IDX_SHFT;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.accu.SummaryHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.solver.hinters.Slots.*;

/**
 * LockingSpeedMode is unusual in that it extends the Locking hinter to apply
 * each hint to the grid immediately using a {@link HintsApplicumulator})
 * provided to my constructor. I am used only in
 * {@link diuf.sudoku.solver.checks.BruteForce}, where speed REALLY matters,
 * because BruteForce hammers everything, HARD!
 * <p>
 * This class was exhumed from Locking because Locking got too complex, so I
 * split Locking into subclasses to reduce the complexity of Locking, and
 * thereby hopefully make it all a bit easier to follow. I fear that the result
 * is actually more complex, but then I've never been a big OO fan. It is what
 * it is.
 * <p>
 * Johnny come lately: the whole "speed mode" thing was added after the fact.
 * I am only an accomplice to Lockings puzzle murder, for speed. I apply each
 * hint directly to the grid, so one pass through the grid finds all available
 * hints; then each "dirty" region is reprocessed, which saves reprocessing the
 * whole grid, repeatedly, on the off-chance that a dirty region now contains
 * another Locking hint, which is just a bit faster.
 * <p>
 * Note that Locking is swapped on-the-fly, hence nothing in the lock package
 * can be a prepper!
 *
 * @author Keith Corlett 2021-07-11
 */
public final class LockingSpeedMode extends Locking {

	/**
	 * Regions to be re-examined: ARegion =&gt; cands.
	 * <p>
	 * The algorithm is self-cleaning, when it completes, hence any Exception
	 * leaves dirt in the dirtyRegions, so findHints clears dirtyRegions upon
	 * Exception.
	 * <p>
	 * Note that {@link DirtyRegionsMap} is a <u>static</u> inner-class.
	 */
	private final DirtyRegionsMap dirtyRegions = new DirtyRegionsMap();

	/** The HintsApplicumulator to which I add all of my hints. */
	private final HintsApplicumulator aply;

	/**
	 * The Locking "speed mode" Constructor. Used only by BruteForce, ie
	 * the RecursiveSolver, passing a HintsApplicumulator, so that my getHints
	 * applies hints directly to the grid; so that subsequent hints are also
	 * found by ONE pass through the Grid, which is a bit faster, then I add a
	 * SummaryHint with {@code numElims = total elims} to the "normal"
	 * HintsAccumulator that is passed to findHints.
	 * <p>
	 * The interesting part is that the Grid mutates WHILE I am searching it,
	 * and it all works anyway, which is pretty cool. In my humble opinion,
	 * ConcurrentModificationException is programmereese for bloody-soft-cock,
	 * coz it is most often thrown unnecessarily, rather than the programmer
	 * stretching (we are a conservative bunch) to work-out how to avoid it.
	 *
	 * @param apcu HintsApplicumulator
	 */
	public LockingSpeedMode(final HintsApplicumulator apcu) {
		assert apcu != null;
		this.aply = apcu;
	}

	/**
	 * resets apcu.numElims (the total number of eliminations performed by the
	 * most recent execution of the getHints method) to 0.
	 */
	@Override
	public void reset() {
		aply.numElims = 0;
	}

	/**
	 * <b>CAUTION:</b> Seriously Weird S__t!
	 * <p>
	 * LockingSpeedMode is created by BruteForce with a HintsApplicumulator
	 * to apply all Locking hints in 1 pass through the grid, coz it is faster.
	 * Weirdly, I extend the existing Locking hinter.
	 * <p>
	 * I do an exhaustive search, so that when a maybe is removed from a region
	 * thats already been searched we search the modified region again. It is a
	 * bit slower here, but this exhaustive-search is faster overall, because
	 * it does not miss a hint that leaves recursiveSolve guessing a cell value
	 * that can already be proven invalid, and therefore should have been
	 * eliminated already already. Sheesh!
	 * <p>
	 * Then we accu.add a SummaryHint and return have any hints been applied.
	 *
	 * @param grid to search
	 * @param accu to add any hints to
	 * @return have any hints been applied already
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		final boolean result;
		try {
			// to calculate numElims in SummaryHint.
			final int preElims = aply.numElims;
			// NOTE: I pass the apcu up to super (not the normal accu).
			if ( result = super.findHints(grid, aply) ) {
				// re-process dirty regions (not all regions, for speed).
				ARegion region;
				java.util.Map.Entry<Integer,Integer> entry;
				while ( (entry=dirtyRegions.poll()) != null ) { // Hmmm...
					region = grid.regions[entry.getKey()];
					if ( region instanceof Box ) // Better!
						pointFrom(grid, (Box)region, entry.getValue());
					else // Row or Col
						claimFrom(grid, region, entry.getValue());
				}
				accu.add(new SummaryHint("LockingSpeedMode.findHints", aply.numElims-preElims, aply));
			}
		} catch (Exception ex) {
			dirtyRegions.clear();
			throw ex;
		}
		return result;
	}

	/**
	 * Search this box for Pointing hints on $cands.
	 *
	 * @param box
	 * @param cands
	 * @return
	 */
	private void pointFrom(final Grid grid, final Box box, final int cands) {
		ARegion line;
		int card, b;
		final int[] numPlaces = box.numPlaces;
		final int[] places = box.places;
		for ( int v : VALUESES[cands] ) {
			if ( (card=numPlaces[v])>1 && card<4 ) {
				b = places[v];
				for ( Slot slot : SLOTS ) {
					if ( (b & slot.bits) == b ) {
						if ( (line=slot.line(box)).numPlaces[v] > card )
							aply.add(hint(grid, box, line, card, v));
						break;
					}
				}
			}
		}
	}

	/**
	 * Search this $line (a Row or a Col) for claiming hints on cands.
	 *
	 * @param line is the row/col to claim in
	 * @param cands is the maybes to look at
	 * @return any hints found?
	 */
	private void claimFrom(final Grid grid, final ARegion line, final int cands) {
		int card, b, o;
		final ARegion[] boxs = line.intersectors;
		final int[] numPlaces = line.numPlaces;
		final int[] places = line.places;
		for ( int v : VALUESES[cands] ) {
			if ( (card=numPlaces[v])>1 && card<4
			  // nb: *_ROW also work for cols (poorly named).
			  && ( (((b=places[v]) & TOP_ROW)==b && (o=0)==0)
				|| ((b & MID_ROW)==b && (o=1)==1)
				|| ((b & BOT_ROW)==b && (o=2)==2) )
			  && boxs[o].numPlaces[v] > card )
				aply.add(hint(grid, line, boxs[o], card, v));
		}
	}

	/**
	 * Called only by Locking.createHint to allow me (its subclass) to
	 * handle each "new eliminations found" event.
	 * <p>
	 * Adds new eliminated cands to any existing ones for each region of
	 * each cell that has been eliminated from (reds are eliminations).
	 * nb: Pointlessly adds the region that was searched to find each elim,
	 * but I am unable to preclude that region given only the elims; so that
	 * region will be searched again. No biggy, just a bit inefficient.
	 * <p>
	 * <b>WARNING</b>: NO lambda! It screws with super.get, or atleast results
	 * are wrong in the debugger. Obviously that might JUST be a bug in the
	 * Netbeans 8.2 debugger, but safety first.
	 * <p>
	 * If it looks like stink-fish and smells like stink-fish, do NOT taste
	 * it in order be certain that it is in fact stink-fish, and not just a
	 * cleverly disguised simulade. Let us simply presume that it is in fact
	 * exactly what it purports to be, ____ing-stink-fish, and get on with
	 * making the ____er work anyway. Clear?
	 *
	 * @param gridCells grid.cells of the grid that we are searching
	 * @param reds eliminations
	 */
	@Override
	protected void eliminationsFound(final Cell[] gridCells, final Pots reds) {
		int cands;
		Integer existing;
		for ( java.util.Map.Entry<Integer, Integer> e : reds.entrySet() ) {
			cands = e.getValue();
			for ( final ARegion region : gridCells[e.getKey()].regions ) // cell.regions
				// nb: DirtyRegions.get is MUCH faster than HashMaps.
				if ( (existing=dirtyRegions.get(region)) != null ) {
					// nb: no point updating unless there are new cands.
					// nb: DirtyRegions.put is MUCH faster than HashMaps.
					if ( (cands & ~existing) > 0 ) // 9bits
						dirtyRegions.put(region, existing | cands);
				} else
					dirtyRegions.put(region, cands);
		}
	}

	/**
	 * A Map of ARegion.index to a bitset of cands requiring re-examination.
	 * <p>
	 * dirtyRegions was a {@code MyLinkedHashMap<ARegion, Integer> } but ANY
	 * hash is just a bit slow for my purposes, so I rolled my own, based on a
	 * bitset. This class is called LockingSpeedMode coz it's supposed to be
	 * BLOODY FAST!
	 * <p>
	 * This class does <b>NOT</b> implements java.util.Map, by design, even
	 * though it is based on it, because attempting to do so turned it into a
	 * bloody big complex palava, and looked about as slow as a hash, and twice
	 * as ugly. This is MUCH simpler, faster, and entirely fit for my limited
	 * purposes. And it also looks fairly straight forward. Nice!
	 * <p>
	 * If I had to name this Map implementation it would be an IntBitsetMap,
	 * which is limited to 32 entries, pretty obviously; or 64 for a LongBSM.
	 * <pre>
	 * Subsequent remap {@code <Integer,Integer>} region.index as key instead
	 * of ARegion, for speed, and simplicity. The Map does NOT use ARegion,
	 * only poll did, so a DirtyRegions instance was locked onto a Grid, so was
	 * reconstructed in every findHints; but if we just use the index instead
	 * of ARegion then the whole slow mess goes away. Simples!
	 *
	 * Another Advantage: every ARegion reference holds whole Grid in memory,
	 * hence index-as-key cannot cause "hang-over" memory leaks.
	 *
	 * Another Advantage: clear used only upon Exception, coz the algorithm is
	 * self cleaning, so long as it completes.
	 * </pre>
	 */
	private static class DirtyRegionsMap {

		/**
		 * A Dirty Region: <br>
		 * key is ARegion.index of the "dirty" region <br>
		 * value is a Values bitset of "dirty" cands
		 */
		private static class MyEntry implements java.util.Map.Entry<Integer, Integer> {
			private final Integer key;
			private Integer value;
			public MyEntry(final Integer key, final Integer value) {
				this.key = key;
				this.value = value;
			}
			@Override
			public Integer getKey() {
				return key;
			}
			@Override
			public Integer getValue() {
				return value;
			}
			// nb: implemented, but unused, hence this impl remains untested!
			@Override
			public Integer setValue(final Integer value) {
				final Integer pre = this.value;
				this.value = value;
				return pre;
			}
		}

		// RegionIndexesBitSet: 27 bits, one for each region in the Grid.
		private int ribs;
		// a Values bitset for each region in the grid
		private final int[] values = new int[Grid.NUM_REGIONS];

		/**
		 * Remove and return the next dirty ARegion from this Queue/Set;
		 * else null meaning that this Queue/Set is empty.
		 *
		 * @return a new {@code Map.Entry<ARegion, Integer>}
		 *  containing the next dirty region.
		 */
		private java.util.Map.Entry<Integer, Integer> poll() {
			final int x = ribs & -ribs; // lowestOneBit
			ribs &= ~x; // remove x from dirtyRegions
			// indexOfLowestOneBit-1 which is spot-on for a 0-based index
			final int regionIndex = Integer.numberOfTrailingZeros(x);
			if ( regionIndex == 32 ) // 32 is nOTZ's NOT_FOUND magic number
				return null;
			// read and remove the values
			final int cands = values[regionIndex];
			values[regionIndex] = 0;
			// return regionIndex,cands as a Map.Entry
			return new MyEntry(regionIndex, cands);
		}

		private Integer get(final ARegion region) {
			final int cands = values[region.index];
			if ( cands == 0 )
				return null;
			return cands;
		}

		private void put(final ARegion region, final int cands) {
			ribs |= IDX_SHFT[region.index];
			values[region.index] |= cands;
		}

		/**
		 * remove all entries from this Map.
		 * <p>
		 * clear is used only upon Exception. The algorithm is self-cleaning
		 * (ie leaves DirtyRegions empty) so long as it completes.
		 */
		private void clear() {
			ribs = 0;
			java.util.Arrays.fill(values, 0);
		}
	}

}
