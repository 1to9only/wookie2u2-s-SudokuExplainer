/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.accu.SummaryHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.solver.hinters.Slots.*;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedHashMap;
import java.util.Map;

/**
 * LockingSpeedMode is unusual is that it extends the Locking hinter to apply
 * each hint to the grid immediately using the apcu (a HintsApplicumulator)
 * provided to my constructor.
 * <p>
 * This class was exhumed from Locking because Locking was too complex, so I
 * refactored the complications into subclasses to reduce Locking complexity,
 * and thereby hopefully make it all a bit easier to follow.
 * <p>
 * Johnny come lately: the whole "speed mode" thing was added after the fact.
 * It's only an accomplice to puzzle murder, for more speed, by applying hints
 * directly to the grid, so that one pass through the grid finds all available
 * hints, and then each "dirty" region is reprocessed, so that we no-longer
 * reprocess the whole grid repeatedly, on the off-chance that a dirty region
 * now contains another Locking hint. Repeating: this is just a bit faster.
 *
 * @author Keith Corlett 2021-07-11
 */
public final class LockingSpeedMode extends Locking {

	/** The HintsApplicumulator to which I add all of my hints. */
	private final HintsApplicumulator apcu;

	/** each region of each effected cell, and it's maybes before elims. */
	private final RegionQueue dirtyRegions = new RegionQueue();

	/**
	 * The Locking "speed mode" Constructor. Used only by BruteForce, ie
	 * the RecursiveSolver, passing a HintsApplicumulator, so that my getHints
	 * applies hints directly to the grid; so that subsequent hints are also
	 * found by ONE pass through the Grid, which is a bit faster, then I add a
	 * SummaryHint with {@code numElims = total elims} to the "normal"
	 * HintsAccumulator that is passed to findHints.
	 * <p>
	 * The interesting part is that the Grid mutates WHILE I'm searching it,
	 * and it all works anyway, which is pretty cool. In my humble opinion,
	 * ConcurrentModificationException is programmereese for bloody-soft-cock,
	 * coz it's most often thrown unnecessarily, rather than the programmer
	 * stretching (we're a conservative bunch) to work-out how to avoid it.
	 *
	 * @param apcu HintsApplicumulator
	 */
	public LockingSpeedMode(final HintsApplicumulator apcu) {
		assert apcu != null;
		this.apcu = apcu;
	}

	/**
	 * resets apcu.numElims (the total number of eliminations performed by the
	 * most recent execution of the getHints method) to 0.
	 */
	@Override
	public void reset() {
		apcu.numElims = 0;
	}

	/**
	 * <b>CAUTION:</b> Seriously Weird S__t!
	 * <p>
	 * LockingSpeedMode is created by BruteForce with a HintsApplicumulator
	 * to apply all Locking hints in 1 pass through the grid, coz it's faster.
	 * Weirdly, I extend the existing Locking hinter.
	 * <p>
	 * I do an exhaustive search, so that when a maybe is removed from a region
	 * that's already been searched we search the modified region again. It's a
	 * bit slower here, but this exhaustive-search is faster overall, because
	 * it doesn't miss a hint that leaves recursiveSolve guessing a cell value
	 * that can already be proven invalid, and therefore should've been
	 * eliminated already already. Sheesh!
	 * <p>
	 * Then we accu.add a SummaryHint and return have any hints been applied.
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		ARegion r;
		dirtyRegions.clear(); // Bog roll!
		final boolean result;
		// to calculate numElims in SummaryHint
		final int pre = apcu.numElims;
		// NOTE: pass the apcu (not the normal accu, ya dumbass)
		if ( result = super.findHints(grid, apcu) ) {
			// re-process dirty regions (not all regions, for speed).
			Map.Entry<ARegion,Integer> e;
			while ( (e=dirtyRegions.poll()) != null )
				if ( (r=e.getKey()) instanceof Box )
					pointFrom((Box)r, e.getValue());
				else // Row or Col
					claimFrom(r, e.getValue());
			accu.add(new SummaryHint(Log.me(), apcu.numElims-pre, apcu));
		}
		return result;
	}

	/**
	 * Search this box for Pointing hints on 'cands'.
	 *
	 * @param box
	 * @param cands
	 * @return
	 */
	private void pointFrom(final Box box, final int cands) {
		int card, b;
		ARegion line;
		final Indexes[] bidx = box.ridx;
		for ( int v : VALUESES[cands] ) {
			if ( (card=bidx[v].size)>1 && card<4 ) {
				b = bidx[v].bits;
				for ( Slot slot : SLOTS ) {
					if ( (b & slot.bits) == b ) {
						line = slot.line(box);
						if ( line.ridx[v].size > card )
							apcu.add(createHint(box, line, card, v));
						break;
					}
				}
			}
		}
	}

	/**
	 * Search this 'line' (a Row or a Col) for claiming hints on cands.
	 *
	 * @param line is the row/col to claim in
	 * @param cands is the maybes to look at
	 * @return any hints found?
	 */
	private void claimFrom(final ARegion line, final int cands) {
		int card, b, offset;
		final Box[] coxs = line.crossingBoxs;
		for ( int v : VALUESES[cands] )
			if ( (card=line.ridx[v].size)>1 && card<4
			  // nb: ROW* also applies to cols (they're badly named).
			  && ( (((b=line.ridx[v].bits) & SLOT1)==b && (offset=0)==0)
				|| ((b & SLOT2)==b && (offset=1)==1)
				|| ((b & SLOT3)==b && (offset=2)==2) )
			  && coxs[offset].ridx[v].size > card )
				apcu.add(createHint(line, coxs[offset], card, v));
	}

	/**
	 * Called only by Locking.createHint to allow it's subtype (me)
	 * to handle each "new eliminations found" event.
	 *
	 * @param reds
	 */
	@Override
	protected void eliminationsFound(Pots reds) {
		dirtyRegions.add(reds);
	}

	/**
	 * {@link #dirtyRegions} is a MyLinkedHashMap plus add(Pots) which upserts
	 * each cell and it's eliminated maybe/s.
	 * <p>
	 * NB: We use MyLinkedHashMap.poll, which is not in java.util.Map.
	 */
	private static class RegionQueue extends MyLinkedHashMap<ARegion, Integer> {
		private static final long serialVersionUID = 1459048958904L;
		// add's new eliminated cands to any existing ones for each region
		// of each cell that has been eliminated from (reds are eliminations).
		// nb: Pointlessly adds the region that was searched to find each elim,
		// but I'm unable to preclude that region given only the elims; so that
		// region will be searched again. No biggy, just a bit inefficient.
		void add(final Pots reds) {
			// NOTE: Do NOT use a lambda here. It seems to screw with my calls,
			// or atleast behaviour is VERY odd/suspicious in the debugger, but
			// pretty obviously that could JUST be the debugger. IDKFAAN.
			for ( Map.Entry<Cell, Integer> e : reds.entrySet() ) {
				final int cands = e.getValue();
				final Cell cell = e.getKey();
				for ( final ARegion r : cell.regions ) {
					final Integer existing = get(r);
					if ( existing != null )
						put(r, existing | cands);
					else
						put(r, cands);
				}
			}
		}
	}

}
