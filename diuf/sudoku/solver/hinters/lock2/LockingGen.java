/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock2;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.NUM_REGIONS;
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
 * LockingGen implements the LockingBasic Sudoku solving technique.
 * <p>
 * Generalised Locking was originally intended to handle extra region types,
 * which I personally despise, because they make puzzles easier, which just
 * waters down the genius of original Sudoku. It's retained because it's an
 * extremely elegant solution, like an arch, and thou shalt never knock down
 * an arch, unless it really needs it, or you just really want to (Adams D.).
 * <pre>
 * LockingGen does about the same job as Locking in about a tenth of the code.
 * I love it, but I don't use it because Locking is faster, despite being ugly.
 * Disregard the difference in reported numElims: Locking donates some of it's
 * eliminations to HiddenPair and HiddenTriple hints, in the siamese search.
 * 149,785,100  31539  4,749  22805    6,568 Locking            DOWN numElims
 *  73,193,100  19905  3,677   9893    7,398 HiddenPair         UP numElims
 * 107,981,300  17415  6,200   1240   87,081 HiddenTriple       UP numElims
 * ----------------------------------------------------------------------------
 * 217,451,200  33191  6,551  25044    8,682 LockingGen ACTUAL
 *  71,069,400  19817  3,586   8202    8,664 HiddenPair
 * 107,880,500  17394  6,202    991  108,860 HiddenTriple
 * The really interesting part is Locking 149ms vs LockingGen 217ms, so Locking
 * takes about three-quarters of the time, but they're both really fast, so
 * using either in the GUI makes no discernable difference to response time.
 * Note that when LockingGen is used, it's used ONLY to findHints. BruteForce
 * still uses Locking, where it's speediness improves overall performance of
 * LogicalSolverTester, because it's totally ____ing hammered.
 * ============================================================================
 * KRC 2020 Dec Boosted into DIUF SudokuExplainer. Need More Speed!
 * KRC 2021 Mar faster having removed eliminate method.
 * KRC 2021 Jul faster now there is no getIdxs.
 * </pre>
 *
 * @author Tarek Maani @SudokuMonster
 * @author Keith Corlett 2020 Dec Boosted into SE.
 */
public class LockingGen extends AHinter {

	public LockingGen() {
		super(Tech.LockingBasic);
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
		// ANSI-C style variables, to eliminate stack-work
		Indexes[] rio; // region.ridx
		Cell[] cells; // region.cells
		Indexes riv; // region.ridx[v]
		ARegion r; // region
		Idx idx, buds; // so we also need a couple of Idx vars
		int[] a; // INDEXES array
		int i // regions index
		  , j,n // index in INDEXES array, and length thereof
		  , v // the locking potential value 1..9
		  , card // card-inality: the number cells in region.ridx[v]
		  , v0,v1,v2; // victims decomposed
		// indices of cells in grid which maybe each value 1..9 (cached)
		final Idx[] idxs = grid.idxs;
		final ARegion[] regions = grid.regions;
		// presume that no hint will be found
		boolean result = false;
		try {
			// foreach region in the grid
			for ( i=0; i<NUM_REGIONS; ++i ) {
				// dereference once, for speed (and brevity)
				rio = (r=regions[i]).ridx;
				cells = r.cells;
				// foreach potential value
				for ( v=1; v<VALUE_CEILING; ++v ) {
					// if this region has 2..3 places for this value.
					// 2 or 3 places in a region call all be in another region.
					// 1 place is a hidden single, which is not my problem.
					// 4 or more places cannot share another common region.
					if ( (card=(riv=rio[v]).size)>1 && card<4 ) {
						// find victims: grid.cells that are buddies of all
						// cells in this region which maybe v, except those
						// cells themselves.
						// NB: buds excludes this cell itself, so "and"ing buds
						// of each cell which maybe v in region retains only
						// buds of all cells in another common region.
						v0 = (idx=idxs[v]).a0;
						v1 = idx.a1;
						v2 = idx.a2;
						for ( a=INDEXES[riv.bits],n=a.length,j=0; j<n; ++j ) {
							v0 &= (buds=cells[a[j]].buds).a0;
							v1 &= buds.a1;
							v2 &= buds.a2;
						}
						if ( (v0|v1|v2) != 0 ) {
							// FOUND a Locking
							final AHint hint = new LockingGenHint(this
								, new Pots(grid.cellsA(v0,v1,v2), v)
								, r.atNew(riv.bits), v, r);
							result = true;
							if ( accu.add(hint) ) {
								return result;
							}
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
