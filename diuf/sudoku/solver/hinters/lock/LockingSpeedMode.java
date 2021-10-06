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
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.AppliedHintsSummaryHint;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyHashMap;
import diuf.sudoku.utils.MyLinkedHashMap;

/**
 * Locking for SpeedMode is unusual is that it extends the Locking hinter (the
 * only sub-typed actual hinter) to apply hints directly to the grid using the
 * apcu (a HintsApplicumulator) provided to my Constructor.
 * <p>
 * LockingSM was exhumed from Locking because Locking was too complex, so I
 * moved the complications into a subclass to reduce the overall complexity,
 * or to at-least keep Locking as simple as possible.
 * <p>
 * Johnny come lately: the whole "speed mode" thing was added after the fact.
 * It's only an accomplice to puzzle murder, for more speed, by applying hints
 * directly to the grid, so that one pass through the grid finds all available
 * hints, and then each "dirty" region is reprocessed, so that we no-longer
 * reprocess the whole grid repeatedly, on the off-chance that a dirty region
 * now contains another Locking hint.
 *
 * @author Keith Corlett 2021-07-11
 */
public class LockingSpeedMode extends Locking {

	/** The HintsApplicumulator to which I add all of my hints. */
	private final HintsApplicumulator apcu;

	/** each region of each effected cell, and it's maybes before elims. */
	private final RegionQueue dirtyRegions;

	/**
	 * The Locking "speed mode" Constructor. Used only by SingleSolution,
	 * ie RecursiveSolver, passing a HintsApplicumulator, so that my getHints
	 * applies it's hints the directly grid; so that any subsequent hints can
	 * also be found in ONE pass through the Grid, then I add an
	 * AppliedHintsSummaryHint (numElims = total elims)
	 * to the "normal" HintsAccumulator passed into findHints.
	 * <p>
	 * The notable bit is that the Grid mutates WHILE I'm searching it, and it
	 * all still works anyway, which is pretty cool. In my humble opinion,
	 * ConcurrentModificationException is how a real programmer pronounces
	 * soft-cock; because it is most often thrown unnecessarily, rather than
	 * the programmer stretching there mind as to how it could be avoided.
	 *
	 * @param apcu HintsApplicumulator
	 */
	public LockingSpeedMode(HintsApplicumulator apcu) {
		super();
		assert apcu != null;
		this.apcu = apcu;
		this.dirtyRegions = new RegionQueue();
//		// speedMode is only for use by SingleSolution.
//		// asserts are for techies (who java -ea) and this is a constructor,
//		// so not performance critical, but using Debug like this is a hack.
//		assert Debug.isClassNameInTheCallStack(7, "SingleSolution");
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
	 * LockingSpeedMode exists in SingleSolution only! This LockingSM was
	 * created by SingleSolution with a HintsApplicumulator to apply all point
	 * and claim hints in one pass through the grid, because it's a bit quicker
	 * that way.
	 * <p>
	 * I do an exhaustive search, so that when a maybe is removed from a region
	 * that's already been searched we search it again. It's a bit slower here,
	 * but this exhaustive-search is faster overall, because it doesn't miss a
	 * hint that leaves recursiveSolve guessing a cell value that can already
	 * be proven invalid, and therefore should have been removed already.
	 * <p>
	 * Then we accu.add an AppliedHintsSummaryHint and return have any hints
	 * been applied.
	 *
	 * @param grid
	 * @param accu
	 * @return
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		ARegion dirtyRegion;
		dirtyRegions.clear(); // Bog roll!
		final int preElims = apcu.numElims;
		// note the bitwise-or operator (|) so they're both executed.
		final boolean result = pointing(grid, apcu)
							 | claiming(grid, apcu);
		if ( result ) {
			// Second pass of the regions.
			// NB: I've never found anything in a third pass, so now it's just
			// an if (was a while loop), and we're only re-processing the dirty
			// regions, not doing all regions twice, for speed.
			MyHashMap.Entry<ARegion,Integer> e;
			while ( (e=dirtyRegions.poll()) != null ) // wax
				if ( (dirtyRegion=e.getKey()) instanceof Box ) // on
					pointFrom((Box)dirtyRegion, e.getValue(), grid);
				else // Row or Col // off
					claimFrom(dirtyRegion, e.getValue(), grid);
		}
		final int myElims = apcu.numElims - preElims;
		if ( myElims > 0 ) {
			final AHint hint = new AppliedHintsSummaryHint(Log.me(), myElims, apcu);
			if ( accu.add(hint) )
				return true;
		}
		return result;
	}

	/**
	 * Search this box for Pointing hints on 'cands'.
	 *
	 * @param box
	 * @param rows
	 * @param cols
	 * @param cands
	 * @param grid
	 * @return
	 */
	private boolean pointFrom(final Box box, final int cands, final Grid grid) {
		int card;
		boolean result = false;
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		for ( int v : VALUESES[cands] )
			if ( (card=box.ridx[v].size)>1 && card<4 ) {
				final int b = box.ridx[v].bits;
				if ( (b & ROW1) == b )
					result |= pfElims(rows[box.top], box, v, card, grid);
				else if ( (b & ROW2) == b )
					result |= pfElims(rows[box.top + 1], box, v, card, grid);
				else if ( (b & ROW3) == b )
					result |= pfElims(rows[box.top + 2], box, v, card, grid);
				else if ( (b & COL1) == b )
					result |= pfElims(cols[box.left], box, v, card, grid);
				else if ( (b & COL2) == b )
					result |= pfElims(cols[box.left + 1], box, v, card, grid);
				else if ( (b & COL3) == b )
					result |= pfElims(cols[box.left + 2], box, v, card, grid);
			}
		return result;
	}

	/**
	 * pfElims is called ONLY by above pointFrom, to handle it's eliminations,
	 * instead of farnarkelling my way around repeating the same code several
	 * times. Note that I add all my hints to the apcu.
	 *
	 * @param line is a row or a col
	 * @param box is the Box
	 * @param v is the value
	 * @param card is the number of cells in the box which maybe v
	 * @param grid currently only used for error messages.
	 * @return
	 */
	private boolean pfElims(final ARegion line, final Box box
			, final int v, final int card, final Grid grid) {
		// if v's in line other than those in the line-box-intersection
		if ( line.ridx[v].size > card ) {
			final Cell[] cells;
			if ( card == box.maybe(VSHFT[v], cells=new Cell[card]) ) {
				final AHint hint = createHint(LockingType.SiamesePointing, box, line
						, cells, card, v, grid);
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
				if ( ( ((b & ROW1)==b && (offset=0)==0)
					|| ((b & ROW2)==b && (offset=1)==1)
					|| ((b & ROW3)==b && (offset=2)==2) )
				  && line.crossingBoxs[offset].ridx[v].size > card ) {
					if ( card == line.maybe(VSHFT[v], cells=new Cell[card]) ) {
						final AHint hint = createHint(LockingType.SiameseClaiming, line
								, line.crossingBoxs[offset], cells, card, v, grid);
						if ( hint != null ) {
							result |= true; // never say never!
							apcu.add(hint);
						}
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
		// add's new cell.maybes to any existing ones for this cell,
		// retaining maybes BEFORE any eliminations are made.
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
