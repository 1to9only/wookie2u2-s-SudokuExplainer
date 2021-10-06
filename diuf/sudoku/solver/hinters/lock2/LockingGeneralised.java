/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.VALUE_CEILING;
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
 * LockingGeneralised was intended to handle non-standard region types, which I
 * despise, coz they make puzzles easier, watering-down the original genius.
 * <pre>
 * LockingGeneralised is retained coz it's an elegant solution, like an arch.
 * This class does nearly the same job as Locking in about a tenth of the code.
 * I love it, but I don't use it because Locking is faster, despite being ugly.
 * Disregard the difference in reported numElims: Locking donates some of it's
 * eliminations to HiddenPair and HiddenTriple hints, in the siamese search.
 * 149,785,100  31539  4,749  22805    6,568 Locking            DOWN numElims
 *  73,193,100  19905  3,677   9893    7,398 HiddenPair         UP numElims
 * 107,981,300  17415  6,200   1240   87,081 HiddenTriple       UP numElims
 * ----------------------------------------------------------------------------
 * 217,451,200  33191  6,551  25044    8,682 LockingGeneralised ACTUAL
 *  71,069,400  19817  3,586   8202    8,664 HiddenPair
 * 107,880,500  17394  6,202    991  108,860 HiddenTriple
 * The really interesting part is Locking 149ms vs LockingGeneralised 217ms, so
 * Locking takes about three-quarters of the time, but they're both really fast
 * so using either in the GUI makes no discernable difference to response time.
 * Note that when LockingGeneralised is used, it's used ONLY to find hints.
 * SingleSolution still uses Locking, where it's speediness improves overall
 * performance of LogicalSolverTester, because it's totally ____ing hammered.
 * ============================================================================
 * KRC 2020 Dec Boosted into DIUF SudokuExplainer. Need More Speed!
 * KRC 2021 Mar faster having removed eliminate method.
 * KRC 2021 Jul faster now there is no getIdxs.
 * </pre>
 *
 * @author Tarek Maani @SudokuMonster
 * @author Keith Corlett 2020 Dec Boosted into SE.
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
			Indexes[] rio; // region.ridx
			Indexes riv; // region.ridx[v]
			int v // the locking potential value 1..9
			  , card; // card-inality: the number cells in region.ridx[v]
			// indices of cells in grid which maybe each value 1..9 (cached)
			final Idx[] candidates = grid.idxs;
			// indices of cells in grid from which we can eliminate v
			final Idx victims = this.victims;
			// presume that no hint will be found
			boolean result = false;
			// foreach region in the grid
			for ( ARegion region : grid.regions ) {
				// dereference once, for speed (and brevity)
				rio = region.ridx;
				// foreach potential value
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// if this region has 2..6 places for this value
					if ( (card=(riv=rio[v]).size)>1 && card<7 ) {
						// find victims: grid.cells that maybe v see all cells
						// in region which maybe v, except cells themselves.
						// NOTE: cell.buds excludes cell itself, "and"ing the
						// buds of each cell which maybe v in region retains
						// only buddies of all cells in other common region.
						victims.set(candidates[v]);
						for ( int i : INDEXES[riv.bits] )
							victims.and(region.cells[i].buds);
						if ( victims.any() ) {
							// FOUND a Locking
							result = true;
							final AHint hint = new LockingGeneralisedHint(
								  this
								, new Pots(victims.cellsA(grid), v)
								, region.atNew(riv.bits)
								, v
								, region
							);
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
