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
import diuf.sudoku.Values;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Implementation of the Generalized Locking Sudoku solving technique.
 * <p>
 * LockingGeneralised is retained because it's a simple solution to a complex
 * problem; like an arch. It is retained because it's beautiful, and I admire
 * it, but I never use it, because Locking is faster (but ugly).
 *
 * @author Tarek Maani @SudokuMonster
 * @author KRC 2020 Dec Boosted into DIUF SudokuExplainer.
 *  Speed: start with maybes(value); no cell in buds; use Idx over BitIdx.
 * @author KRC 2021 Mar No need for eliminate method. What was I thinking?
 */
public class LockingGeneralised extends AHinter {

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
		int n; // the number of cells which maybe v in this region
		int v; // the potential value 1..9
		int i; // the ubiquitious index
		// get cached cells in grid which maybe each value 1..9
		final Idx[] candidates = grid.getIdxs();
		// re-used cells which maybe v in this region
		final Cell[] cells = new Cell[6];
		// this Idx is set for each value in each region to all cells in the
		// grid which maybe value, then we retainAll each cells buddies to find
		// the victims, if any. Obviously mostly there are no victims; but this
		// way we don't need to create a new Idx for each item.
		final Idx victims = new Idx();
		// presume that no hint will be found
		boolean result = false;
		// foreach region in the grid: boxs, rows, cols
		// nb: regions is outer loop to search boxs first, den rows, den cols.
		for ( ARegion r : grid.regions )
			// foreach potential value
			for ( v=1; v<10; ++v )
				// if this region has 2..6 possible positions for this value
				if ( (riv=r.indexesOf[v]).size>1 && riv.size<7 ) {
					// repopulate cells with those in region which maybe v
					n = r.at(riv.bits, cells);
					// find the victims (if any): cells in this region which
					// maybe v, and are siblings of all cells, except the cells
					// themselves. Buds excludes this cell itself. Simples!
					victims.set(candidates[v]);
					for ( i=0; i<n; ++i )
						victims.and(cells[i].buds);
					if ( !victims.isEmpty() ) {
						result = true;
						// create the hint, and add it to accu.
						if ( accu.add(new LockingGeneralisedHint(this
								, new Pots(victims.cells(grid), new Values(v))
								, n, cells, v, r)) )
							return result;
					}
				}
		return result; // were any hint/s found?
	}

}
