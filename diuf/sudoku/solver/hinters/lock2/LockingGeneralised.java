/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Implementation of the Generalized Locking Sudoku solving technique.
 *
 * @author Tarek Maani @SudokuMonster
 * @author KRC 2020 Dec boosted into SE. Speed: start with maybes(value); buds
 * don't contain cell itself; use Idx over BitIdx.
 */
public class LockingGeneralised extends AHinter {

	/**
	 * Eliminate from all cells which maybe value that are seen by ALL of the
	 * given cells, except the given cells themselves; thus finding all cells
	 * which share a second region with ALL cells, except cells themselves.
	 * <p>
	 * LockingGeneralised was incepted to deal with non-standard constraints,
	 * which I will NEVER have. I've only boosted it because I admire it. It's
	 * really quite clever, like an arch. Simple and effective.
	 * <p>
	 * My Locking class handles Siamese Locking, which can "promote" a hint to
	 * a Naked Pair/Triple, so while the hint-counts differ between them, they
	 * both find all of the same hints (AFAIK); just report them differently.
	 * <p>
	 * Note that Pointing difficulty is 2.1, Claiming is 2.2;<br>
	 * while Locking Generalised is 2.1 (both in one)
	 * <p>
	 * Note that my Locking class is faster, but they're both sub-second over
	 * 1465 puzzles, so that really matters not.
	 *
	 * @param grid the Grid to search
	 * @param n the number of cells in the cells array
	 * @param cells the cells in the region we're searching
	 * @param value the value we seek
	 * @param victims a mutable copy of all cells in grid which maybe value,
	 *  ie {@code victims.set(grid.getIdxs()[value])}. Note that victims are
	 *  set so that there's only ONE instance of Idx that's re-used for all
	 * @return Pots the removable (red) Cell=>Values, if any, else null.
	 */
	private static Pots eliminate(Grid grid, final int n, final Cell[] cells
			, final int value, final Idx victims) {
		// victims: I'm passed all cells in the grid which maybe value, so I
		// and-in the cells seen by each of the given cells (nb buds doesn't
		// contain the cell itself); to produce all cells which maybe value
		// that are seen by ALL of the given cells, except the cells demselves.
		for ( int i=0; i<n; ++i )
			victims.and(cells[i].buds);
		if ( victims.isEmpty() )
			return null; // none
		Pots reds = new Pots();
		for ( Cell victim : victims.cells(grid) )
			reds.upsert(victim, value);
		return reds; // some
	}

	public LockingGeneralised() {
		super(Tech.LockingGeneralised);
	}

	/**
	 * Find first/all Pointing and Claiming hint/s in the grid, depending on
	 * the passed IAccumulator.
	 *
	 * @param grid the Grid to search
	 * @param accu the IAccumulator to add any hints to
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Indexes riv; // region.indexesOf[value]
		Pots reds; // removable (red) Cell=>Values
		AHint hint; // if we ever find one
		int n; // the number of cells which maybe v in this region
		// get cells in grid which maybe each value 1..9
		final Idx[] vs = grid.getIdxs(); // cached!
		// re-used cells which maybe v in this region
		final Cell[] cells = new Cell[6];
		// this BitSet is set for each value in each region to all the cells in
		// the grid which maybe value, then eliminate removes cells to find its
		// victims, if any... pretty obviously mostly there are no vitims; but
		// atleast this way we don't need to create a new BitIdx for each item,
		// we just to clear it instead, which for some reason seems a bit slow!
		// So... this Set is re-set and mutated for each eliminations search.
		final Idx victims = new Idx();
		// presume that no hint will be found
		boolean result = false;
		// foreach region in the grid: boxs, rows, cols
		// nb: regions is outer loop to search boxs first, den rows, den cols.
		for ( ARegion r : grid.regions )
			// foreach potential value
			for ( int v=1; v<10; ++v ) {
				// if this region has 2..6 possible positions for this value
				if ( (riv=r.indexesOf[v]).size>1 && riv.size<7
				  // then get an array of those cells with region.at,
				  // and see if there's any eliminations
				  && (reds=eliminate(grid, n=r.at(riv.bits, cells), cells, v
						  , victims.set(vs[v]))) != null ) {
					// create the hint, and add it to accu.
					hint = new LockingGeneralisedHint(this, reds, n, cells
							, v, r);
					result = true;
					if ( accu.add(hint) )
						return true;
				}
			}
		return result; // were any hint/s found?
	}

}
