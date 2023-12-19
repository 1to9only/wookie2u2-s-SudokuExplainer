/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.naked;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * NakedTriple implements the {@link Tech#NakedTriple} Sudoku solving
 * technique. A NakedTriple is three cells that may only be these three maybes,
 * hence these three values are locked-into these three cells, therefore we can
 * remove these values from all other cells in all common regions.
 */
public class NakedTriple extends NakedSet {

	public NakedTriple() {
		super(Tech.NakedTriple);
	}

	/**
	 * Find NakedTriple hints in $grid and add them to $accu.
	 *
	 * @param grid the grid to search
	 * @param accu IAccumulator to which I add any hints I find
	 * @return was a hint/s found?
	 */
	@Override
	public final boolean findHints(final Grid grid, final IAccumulator accu) {
		// temporary Idx pointers, for exploded Idx ops.
		Idx a // BUDDIES[a] where a is first cell in NakedTriple
		  , b // BUDDIES[b] where b is second cell in NakedTriple
		  , c // BUDDIES[c] where c is third cell in NakedTriple
		  , d // grid.idxs of first NakedTriple value
		  , e // grid.idxs of second NakedTriple value
		  , f; // grid.idxs of third NakedTriple value
		int vs[] // VALUESES of the first NakedTriple cells maybes
		  , n // number of sized cells in this region
		  , cands // candidates: values in the naked set
		;
		long v0; int v1; // victims Exploded Idx
		final Idx[] idxs = grid.idxs;
		final int[] m = new int[REGION_SIZE];
		final int[] x = new int[REGION_SIZE];
		final int[] pa = new int[degree];
		boolean result = false;
		// foreach region in the grid
		for ( ARegion region : grid.regions ) {
			// if this region has an "extra" empty cell to remove maybes from
			if ( region.emptyCellCount > degree ) {
				// read maybes
				n = 0;
				for ( final Cell cell : region.cells )
					if ( cell.size>1 && cell.size<degreePlus1 )
						m[n++] = cell.maybes;
				if ( n > degreeMinus1 ) {
					// foreach pair of cells amongst our $n sized cells.
					for ( int[] p : permuter.permute(n, pa) ) {
						// if there are 2 distinct maybes in these 2 cells
						if ( VSIZE[m[p[0]] | m[p[1]] | m[p[2]]] == degree ) {
							// its a NakedTriple, but any elims? (~96% have 0)
							// nb: faster to NOT cache vs (hit rate too low)
							vs = VALUESES[m[p[0]] | m[p[1]] | m[p[2]]];
							n = 0;
							for ( final Cell cell : region.cells )
								if ( cell.size>1 && cell.size<degreePlus1 )
									x[n++] = cell.indice;
							v0 = (a=BUDDIES[x[p[0]]]).m0
							   & (b=BUDDIES[x[p[1]]]).m0
							   & (c=BUDDIES[x[p[2]]]).m0
							   & ( (d=idxs[vs[0]]).m0
								 | (e=idxs[vs[1]]).m0
								 | (f=idxs[vs[2]]).m0 );
							v1 = a.m1 & b.m1 & c.m1
							   & (d.m1 | e.m1 | f.m1);
							// if any victims
							if ( (v0|v1) > 0L ) {
								// FOUND a NakedTriple!
								result = true;
								if ( accu.add(hint(grid, region
								, cands = m[p[0]] | m[p[1]] | m[p[2]]
								, Idx.of(x[p[0]], x[p[1]], x[p[2]])
								, new Pots(v0,v1, grid.maybes, cands, DUMMY))) )
									return result;
							}
						}
					}
				}
			}
		}
		return result;
	}

}
