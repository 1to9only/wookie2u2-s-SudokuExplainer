/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.NUM_REGIONS;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;

/**
 * LockingGen implements the {@link Tech#LockingBasic} Sudoku solving
 * technique. LockingGen and Locking do roughly the same thing. This one is
 * beautifully simple, but I prefer Locking because its faster, just ugly.
 * <p>
 * Generalised Locking was originally intended to handle extra region types,
 * which I personally reject, because they make puzzles easier, watering-down
 * the genius of Sudoku. I retain LockingGen out of respect for its elegance.
 * <p>
 * Note that Locking is swapped on-the-fly, hence nothing in the lock package
 * can be a prepper!
 * <pre>
 * LockingGen does about the same job as Locking in about a tenth of the code.
 * I love it, but I do not use it just because Locking is faster. Disregard the
 * difference in reported numElims: Locking donates some of its eliminations to
 * HiddenPair and HiddenTriple hints, in the siamese search.
 * 149,785,100  31539  4,749  22805    6,568 Locking            DOWN numElims
 *  73,193,100  19905  3,677   9893    7,398 HiddenPair         UP numElims
 * 107,981,300  17415  6,200   1240   87,081 HiddenTriple       UP numElims
 * ----------------------------------------------------------------------------
 * 217,451,200  33191  6,551  25044    8,682 LockingGen ACTUAL
 *  71,069,400  19817  3,586   8202    8,664 HiddenPair
 * 107,880,500  17394  6,202    991  108,860 HiddenTriple
 * Locking 149ms plays LockingGen 217ms, so Locking takes about 3/4 time, but
 * they are both well-fast, so it makes no discernable difference in the GUI.
 * Locking differentiates between Pointing and Claiming; LockingGen does not.
 * When LockingGen is wanted, its used ONLY to findHints. BruteForce still uses
 * Locking, where its speed is required for overall performance (hammered).
 * ============================================================================
 * KRC 2020 Dec Boosted into DIUF SudokuExplainer. Need More Speed!
 * KRC 2021 Mar faster having removed eliminate method.
 * KRC 2021 Jul faster now there is no getIdxs.
 * KRC 2021-11-21 avoid Cell[] with the new Pots(Idx, ...) Constr, the values
 *                for which we already have. Simpler and faster.
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
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// ANSI-C style variables, to eliminate stack-work
		ARegion region; // region
		Idx idx; // pointer to the current Idx
		int places[] // region.places
		  , indexes[] // INDEXES[places[v]]
		  , numPlaces[] // region.numPlaces
		  , indices[] // region.indices
		  , indice // a Cell index in Grid.cells
		  , i // regions index
		  , j,n // index in INDEXES array, and length thereof
		  , card // card-inality: the number cells in region.places[v]
		  , va[], vn, vi, v // array, size, index, value
		;
		long vc0; int vc1; // victims Exploded Idx
		// indices of cells in grid which maybe each value 1..9 (cached)
		final Idx[] idxs = grid.idxs;
		final ARegion[] regions = grid.regions;
		// presume that no hint will be found
		boolean result = false;
		// foreach region in the grid
		// fastard: for-index is faster than Javas array-iterator
		for ( i=0; i<NUM_REGIONS; ++i ) {
			if ( (region=regions[i]).unsetCands > 0 ) { // 9bits
				indices = region.indices;
				places = region.places;
				numPlaces = region.numPlaces;
				// foreach potential value
				// fastard: avoid var creation for a single stack-frame
				for ( va=VALUESES[region.unsetCands],vn=va.length,vi=0; vi<vn; ++vi ) {
					// if this region has 2 or 3 places for this value.
					// 1 is a hidden single, which is not my problem.
					// 2/3 places in a region can share another common region.
					// 4 or more places cannot share another common region.
					if ( (card=numPlaces[v=va[vi]])>1 && card<4 ) {
						// victims: grid.cells seeing all cells in this region
						// which maybe v, except those cells themselves.
						// victims starts as all cells in grid which maybe v.
						vc0 = (idx=idxs[v]).m0;
						vc1 = idx.m1;
						// buds excludes the cell itself, so and-ing buds of
						// every cell which maybe v in this region retains only
						// buds of all those cells in a second common region,
						// which is seriously ____ing smart, IMHO.
						for ( indexes=INDEXES[places[v]],n=indexes.length,j=0; j<n; ++j ) {
							vc0 &= BUDS_M0[indice=indices[indexes[j]]];
							vc1 &= BUDS_M1[indice];
						}
						if ( (vc0|vc1) > 0L ) {
							// FOUND a Locking Generalised
							result = true;
							if ( accu.add(new LockingGenHint(this, region, v
							, new Pots(vc0,vc1, grid.maybes, VSHFT[v], DUMMY))) ) // reds
								return result;
						}
					}
				}
			}
		}
		return result;
	}

}
