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
import diuf.sudoku.solver.hinters.IFoxyHinter;

/**
 * NakedPair implements the {@link Tech#NakedPair} Sudoku solving technique.
 * A NakedPair is two cells that maybe only two values, hence these values are
 * locked-into these two cell, therefore we can remove both these values from
 * other cells in all common regions. A "common region" is one that contains
 * both cells.
 */
public class NakedPair extends NakedSet implements IFoxyHinter {

	public NakedPair() {
		super(Tech.NakedPair);
	}

	/**
	 * Find NakedPair hints in $grid and add them to $accu.
	 *
	 * @param grid the grid to search
	 * @param accu IAccumulator to which I add any hints I find
	 * @return was a hint/s found?
	 */
	@Override
	public final boolean findHints(final Grid grid, final IAccumulator accu) {
		// temporary Idx pointers, for exploded Idx ops.
		Idx a // BUDDIES[a] where a is first cell in NakedPair
		  , b // BUDDIES[b] where b is second cell in NakedPair
		  , c // grid.idxs of first NakedPair value
		  , d; // grid.idxs of second NakedPair value
		int vs[] // VALUESES of the first NakedPair cells maybes
		  , n // number of sized cells in this region
		  , cands // candidates: values in the naked set
		;
		long vc0; int vc1; // victims Exploded Idx
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
						if ( VSIZE[m[p[0]] | m[p[1]]] == degree ) {
							// its a NakedTriple, but any elims? (~96% have 0)
							// nb: faster to NOT cache vs (hit rate too low)
							vs = VALUESES[m[p[0]] | m[p[1]]];
							// read indices
							n = 0;
							for ( final Cell cell : region.cells )
								if ( cell.size>1 && cell.size<degreePlus1 )
									x[n++] = cell.indice;
							// read values
							vc0 = (a=BUDDIES[x[p[0]]]).m0
								& (b=BUDDIES[x[p[1]]]).m0
								& ( (c=idxs[vs[0]]).m0
								  | (d=idxs[vs[1]]).m0 );
							vc1 = a.m1 & b.m1
								& (c.m1|d.m1);
							// if any victims
							if ( (vc0|vc1) > 0L ) {
								// FOUND a NakedPair!
								result = true;
								if ( accu.add(hint(grid, region
								, cands = m[p[0]] | m[p[1]]
								, Idx.of(x[p[0]], x[p[1]])
								, new Pots(vc0,vc1, grid.maybes, cands, DUMMY))) )
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
