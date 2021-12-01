/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.Col;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.SummaryHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyHashMap;
import diuf.sudoku.utils.MyLinkedHashMap;

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
public class LockingSpeedMode extends Locking {

	/** The HintsApplicumulator to which I add all of my hints. */
	private final HintsApplicumulator apcu;

	/** each region of each effected cell, and it's maybes before elims. */
	private final RegionQueue dirtyRegions;

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
	public LockingSpeedMode(HintsApplicumulator apcu) {
		super();
		assert apcu != null;
		this.apcu = apcu;
		this.dirtyRegions = new RegionQueue();
//		// speedMode is only for use by BruteForce.
//		// asserts are for techies (who java -ea) and this is a constructor,
//		// so not performance critical, but using Debug like this is a hack.
//		assert Debug.isClassNameInTheCallStack(7, "BruteForce");
	}

	/**
	 * resets apcu.numElims (the total number of eliminations performed by the
	 * most recent execution of the getHints method) to 0.
	 */
	@Override
	public void reset() {
		// let it NPE if this Locking wasn't created with an apcu. If you
		// make Locking Resetable it NPE's in LogicalSolver.reset, so do
		// NOT make Locking Resetable!
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
	public boolean findHints(Grid grid, IAccumulator accu) {
		ARegion dr; // dirtyRegion
		dirtyRegions.clear(); // Bog roll!
		final int preElims = apcu.numElims;
		// note the bitwise-or operator (|) so they're both executed.
		final boolean result = pointing(grid, apcu)
							 | claiming(grid, apcu);
		if ( result ) {
			// re-process the dirty regions (not all regions twice, for speed).
			MyHashMap.Entry<ARegion,Integer> e;
			while ( (e=dirtyRegions.poll()) != null ) {
				if ( (dr=e.getKey()) instanceof Box ) {
					pointFrom((Box)dr, e.getValue(), grid);
				} else { // Row or Col
					claimFrom(dr, e.getValue(), grid);
				}
			}
		}
		final int myElims = apcu.numElims - preElims;
		if ( myElims > 0 ) {
			if ( accu.add(new SummaryHint(Log.me(), myElims, apcu)) ) {
				return true;
			}
		}
		return result;
	}

	/**
	 * Search this box for Pointing hints on 'cands'.
	 *
	 * @param box
	 * @param cands
	 * @param grid
	 * @return
	 */
	private boolean pointFrom(final Box box, final int cands, final Grid grid) {
		int card;
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		boolean result = false;
		for ( int v : VALUESES[cands] )
			if ( (card=box.ridx[v].size)>1 && card<4 ) {
				final int b = box.ridx[v].bits;
				if ( (b & ROW1) == b )
					result |= pfElim(rows[box.top], box, v, card, grid);
				else if ( (b & ROW2) == b )
					result |= pfElim(rows[box.top + 1], box, v, card, grid);
				else if ( (b & ROW3) == b )
					result |= pfElim(rows[box.top + 2], box, v, card, grid);
				else if ( (b & COL1) == b )
					result |= pfElim(cols[box.left], box, v, card, grid);
				else if ( (b & COL2) == b )
					result |= pfElim(cols[box.left + 1], box, v, card, grid);
				else if ( (b & COL3) == b )
					result |= pfElim(cols[box.left + 2], box, v, card, grid);
			}
		return result;
	}

	/**
	 * pfElim is called ONLY by above pointFrom, to do it's eliminations,
	 * instead of hacking my way around repeating the same code repeatedly.
	 * Note that I add my hints to the apcu.
	 *
	 * @param line is a row or a col
	 * @param box is the Box
	 * @param v is the value
	 * @param card is the number of cells in the box which maybe v
	 * @param grid currently only used for error messages.
	 * @return
	 */
	private boolean pfElim(final ARegion line, final Box box, final int v
			, final int card, final Grid grid) {
		// if v's in line other than those in the line-box-intersection
		if ( line.ridx[v].size > card ) {
			final Cell[] cells;
			if ( card == box.maybe(VSHFT[v], cells=new Cell[card]) ) {
				final AHint hint = createHint(LockType.SiamesePointing
						, box, line, cells, card, v, grid);
				if ( hint != null ) {
					apcu.add(hint);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Search this 'line' (a Row or a Col) for claiming hints on cands.
	 *
	 * @param line is the row/col to claim in
	 * @param cands is the maybes to look at
	 * @param grid is the grid to look in
	 * @return any hints found?
	 */
	private boolean claimFrom(final ARegion line, final int cands, final Grid grid) {
		Cell[] cells;
		int card, b, offset;
		boolean result = false;
		for ( int v : VALUESES[cands] ) {
			if ( (card=line.ridx[v].size)>1 && card<4 ) {
				b = line.ridx[v].bits;
				// note that ROW* also applies to cols (they're badly named).
				if ( ( ((b & ROW1)==b && (offset=0)==0)
					|| ((b & ROW2)==b && (offset=1)==1)
					|| ((b & ROW3)==b && (offset=2)==2) )
				  && line.crossingBoxs[offset].ridx[v].size > card
				  && card == line.maybe(VSHFT[v], cells=new Cell[card])
				) {
					final AHint hint = createHint(LockType.SiameseClaiming
							, line, line.crossingBoxs[offset], cells, card
							, v, grid);
					if ( hint != null ) {
						result |= true; // never say never!
						apcu.add(hint);
					}
				}
			}
		}
		return result;
	}


	/**
	 * Called only by Locking.createHint to allow it's subtype (me)
	 * to handle each "new eliminations found" event.
	 *
	 * @param redPots
	 */
	@Override
	protected void eliminationsFound(Pots redPots) {
		dirtyRegions.add(redPots.keySet());
	}

	/**
	 * {@link #dirtyRegions} is a MyLinkedHashMap plus add(cells) to upsert
	 * (update or insert) all these cells and all of there maybes. Note that
	 * we use MyLinkedHashMap's poll() method, which is not in java.util.Map.
	 */
	private static class RegionQueue extends MyLinkedHashMap<ARegion, Integer> {
		private static final long serialVersionUID = 1459048958903L;
		// add's new cell.maybes to any existing ones for this cell.
		void add(Iterable<Cell> cells) {
			Integer existing;
			for ( Cell cell : cells )
				for ( ARegion r : cell.regions )
					if ( (existing=super.get(r)) == null )
						super.put(r, cell.maybes);
					else
						super.put(r, existing | cell.maybes);
		}
	}

}
