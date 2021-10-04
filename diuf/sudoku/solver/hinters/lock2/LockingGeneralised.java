/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * Implementation of the Generalised Locking Sudoku solving technique.
 * <p>
 * LockingGeneralised is retained because it's an elegant solution; like an
 * arch. I adore it, but I don't use it because Locking is faster (but ugly).
 * Locking is 819 lines, this 90-odd lines does approximately the same job.
 * <p>
 * FYI: LockingGeneralised was intended to handle non-standard region types,
 * which I do NOT want, because they make a Sudoku easier, watering-down the
 * original genius, IMHO.
 *
 * @author Tarek Maani @SudokuMonster
 * @author KRC 2020 Dec Boosted into DIUF SudokuExplainer.
 *  Speed: start with maybes(value); no cell in buds; use Idx over BitIdx.
 * @author KRC 2021 Mar No need for eliminate method. What was I thinking?
 */
public class LockingGeneralised extends AHinter {

	private final Idx victims = new Idx();

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
		try {
			Indexes[] rio; // region.indexesOf
			Indexes riv; // region.indexesOf[v]
			int v // the locking potential value 1..9
			  , card; // card-inality: the number cells in region.indexesOf[v]
			// indices of cells in grid which maybe each value 1..9 (cached)
			final Idx[] candidates = grid.idxs;
			// indices of cells in grid from which we can eliminate v
			final Idx victims = this.victims;
			// presume that no hint will be found
			boolean result = false;
			// foreach region in the grid
			for ( ARegion region : grid.regions ) {
				// dereference once, for speed (and brevity)
				rio = region.indexesOf;
				// foreach potential value
				for ( v=1; v<10; ++v ) {
					// if this region has 2..6 places for this value
					if ( (card=(riv=rio[v]).size)>1 && card<7 ) {
						// find victims: grid.cells which maybe v seeing all cells
						// in region which maybe v, except those cells themselves.
						// NOTE: cell.buds excludes this cell itself, so we and the
						// buds of each cell which maybe v in the region, retaining
						// only those cells that are buddies of all of these cells
						// IN ANOTHER REGION. Simples!
						victims.set(candidates[v]);
						for ( int i : INDEXES[riv.bits] )
							victims.and(region.cells[i].buds);
						if ( victims.any() ) {
							// FOUND a Locking
							result = true;
							final AHint hint = new LockingGeneralisedHint(this
								, new Pots(victims.cellsA(grid), v)
								, riv.bits, v, region);
							if ( accu.add(hint) )
								return result;
						}
					}
				}
			}
			return result;
		} finally {
			Cells.cleanCasA();
		}
	}

}
