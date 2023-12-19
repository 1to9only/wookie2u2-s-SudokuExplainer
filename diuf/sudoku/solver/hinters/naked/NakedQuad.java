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
 * NakedQuad implements the {@link Tech#NakedQuad} Sudoku solving technique.
 * A NakedQuad is four cells that may only be four values, hence we call remove
 * these value from other cells in the common region. NakedQuads are rare!
 */
public class NakedQuad extends NakedSet
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[32];

	public NakedQuad() {
		super(Tech.NakedQuad);
	}

	/**
	 * Find NakedQuad hints in $grid and add them to $accu.
	 * <p>
	 * A NakedQuad is four cells that may only be these four shared values,
	 * hence no other cell in there common region may be any of these values.
	 *
	 * @param grid to search
	 * @param accu to add hints to
	 * @return was a hint/s found?
	 */
	@Override
	public final boolean findHints(final Grid grid, final IAccumulator accu) {
		// temporary Idx pointers, for exploded Idx ops.
		Idx a // BUDDIES[a] where a is first cell in NakedQuad
		  , b // BUDDIES[c] where c is third cell in NakedQuad
		  , c // BUDDIES[b] where b is second cell in NakedQuad
		  , d // BUDDIES[d] where d is fourth cell in NakedQuad
		  // I avoid using v, coz its sort-of-a-reserved-word in SE
		  , e // grid.idxs of first NakedQuad maybe
		  , f // grid.idxs of second NakedQuad maybe
		  , g // grid.idxs of third NakedQuad maybe
		  , h // grid.idxs of fourth NakedQuad maybe
		;
		int vs[] // VALUESES of the first NakedQuad cells maybes
		  , n // number of sized cells in this region
		  , cands // maybes of the four NakedSet cells
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
//<HAMMERED comment"iterates a billion times. Very low pass rate.">
					for ( int[] p : permuter.permute(n, pa) ) {
						// if there are 2 distinct maybes in these 2 cells
						if ( VSIZE[m[p[0]] | m[p[1]] | m[p[2]] | m[p[3]]] == degree ) {
//</HAMMERED>
							// its a NakedQuad, but any elims? (~96% have 0)
							// get the four maybes of these four cells
							// nb: faster to not cache (hit rate too low)
							vs = VALUESES[m[p[0]] | m[p[1]] | m[p[2]] | m[p[3]]];
							// read indices
							n = 0;
							for ( final Cell cell : region.cells )
								if ( cell.size>1 && cell.size<degreePlus1 )
									x[n++] = cell.indice;
							// FASTARD: victims are common buddies of all four
							// cells, which maybe any of these four maybes.
							vc0 = (a=BUDDIES[x[p[0]]]).m0
								& (b=BUDDIES[x[p[1]]]).m0
								& (c=BUDDIES[x[p[2]]]).m0
								& (d=BUDDIES[x[p[3]]]).m0
								& ( (e=idxs[vs[0]]).m0
								  | (f=idxs[vs[1]]).m0
								  | (g=idxs[vs[2]]).m0
								  | (h=idxs[vs[3]]).m0 );
							vc1 = a.m1 & b.m1 & c.m1 & d.m1
								& (e.m1 | f.m1 | g.m1 | h.m1);
							// if any victims
							if ( (vc0|vc1) > 0L ) {
								// FOUND a NakedQuad!
								result = true;
								if ( accu.add(hint(grid, region
								, cands = m[p[0]] | m[p[1]] | m[p[2]] | m[p[3]]
								, Idx.of(x[p[0]], x[p[1]], x[p[2]], x[p[3]])
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
