/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the WWing solving technique was boosted by Keith Corlett
 * in March 2020 from the current release AFAIK of HoDoKu by Bernhard Hobiger.
 * Kudos is hobiwans. Mistakes are mine.
 *
 * Here is hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Idx;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * WWing implements the {@link Tech#W_Wing} Sudoku solving technique. A W_Wing
 * is a pair of cells (not in a single region) with the same two maybes having
 * a strong link between them. That link is a region with two places for one of
 * these two values (bipr), where each cell in that region sees one of the two
 * bivalue cells (bivs).
 * <p>
 * <pre>
 * KRC 2021-08-19 tried everything I can think of to speed this up, but no joy.
 * bivi Idx reduces bivs*81 to bivs*bivs but its slower, even with local Idxs,
 * which I do not really understand. It is faster to just iterate grid.cells.
 *
 * KRC 2021-11-25 Search for biplace region on BOTH maybes of the biv, nearly
 * doubling the hint/elim count. Sigh.
 *
 * KRC 2023-06-29 Cleaning-up code. This is a bit faster, using Grids exploded
 * arrays instead of repeatedly dereferencing the cell to get it is attributes.
 * Changed parameters for efficiency and clearness. Forwards-only search does
 * (80*remainder) instead all cells twice (80*80) is a bit faster again.
 * Deleted all the old commented-out code. It was getting tiresome.
 * </pre>
 */
public final class WWing extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln("WWing COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[5];

	public WWing() {
		super(Tech.W_Wing);
	}

	@Override
	public boolean findHints() {												//     17,324
		Idx t0, t1; // temporary Idx pointers
		int[] vA; // an array of the two maybes of cellA
		int v0, v1; // the two maybes of cellA as plain values.
		int a, b, aMaybes;
		long budsA0_M0; int budsA0_M1; // buddies of cellA on vA[0]
		long budsA1_M0; int budsA1_M1; // buddies of cellA on vA[1]
		long vcM0; int vcM1; // victims
		AHint hint = null;
		boolean result = false;
		final int m = GRID_SIZE - 1; // m is n - 1
		// foreach cell in the grid (except the last)
		a = 0;
		do {																	//  1,352,914
			// that has two maybes
			if ( sizes[a] == 2 ) {												//    248,316
				aMaybes = maybes[a];
				vA = VALUESES[aMaybes];
				v0 = vA[0];
				v1 = vA[1];
				// bA0: buddies of cellA that maybe its lower maybe (v0)
				t0 = BUDDIES[a];
				t1 = idxs[v0];
				budsA0_M0 = t0.m0 & t1.m0;
				budsA0_M1 = t0.m1 & t1.m1;
				// bA1: buddies of cellA that maybe its higher maybe (v1)
				t1 = idxs[v1];
				budsA1_M0 = t0.m0 & t1.m0;
				budsA1_M1 = t0.m1 & t1.m1;
				// foreach subsequent cell in grid (a forwards only search).
				b = a + 1;
				do																// 10,317,705
					// with the same two maybes as cellA
					if ( maybes[b] == aMaybes ) {								//     97,225
						// victims are buds of cellA which maybe v0, that
						// are also buds of cellB. If there are any, then
						// prove looks for a strong link between them.
						// nb: vc* are read if/when we create a hint.
						if ( ( (vcM0=budsA0_M0 & BUDS_M0[b])
							 | (vcM1=budsA0_M1 & BUDS_M1[b]) ) > 0L
						  && (hint=prove(a, b, v1, v0, vcM0, vcM1)) != null ) {	//        303
							result = true;
							if ( accu.add(hint) )
								return result;
							hint = null;
						}
						// victims are buds of cellA which maybe v1, that
						// are also buds of cellB. If there are any, then
						// prove looks for a strong link between them.
						// nb: vic* are read down in createHint.
						// nb: t0 is already set to BUDDIES[b].
						if ( ( (vcM0=budsA1_M0 & BUDS_M0[b])
							 | (vcM1=budsA1_M1 & BUDS_M1[b]) ) > 0
						  && (hint=prove(a, b, v0, v1, vcM0, vcM1)) != null ) {	//        308
							result = true;
							if ( accu.add(hint) )
								return result;
							hint = null;
						}
					}
				while (++b < GRID_SIZE);
			}
		} while (++a < m);
		return result;
	}

	/**
	 * Prove is not a "normal" search for victims, instead it builds a case
	 * for conviction of the predetermined victims; by finding a region with
	 * two places for the value x, where each place sees cellA or cellB (one
	 * each), in which case I return the hint; else (usual) I return null.
	 *
	 * @param a indice of cellA: the first bivalue cell
	 * @param b indice of cellB: the second bivalue cell, which has the
	 *  same two maybes as cellA. cellB is NOT the same cell as cellA.
	 * @param x the "value to prove" is a maybe of both cellA and cellB.
	 *  I seek a region with two places for x, where those two cells see
	 *  cellA and cellB, which is a "strong link" between cellA and cellB,
	 *  hence one of cellA or cellB MUST be x
	 * @param y the value to remove from the predetermined victims
	 * @param vcM0 victims mask0
	 * @param vcM1 victims mask1
	 * @return a hint, if the case is proven, else null
	 */
	private AHint prove(final int a, final int b, final int x, final int y
			, final long vcM0, final int vcM1) {								//    61,910
		Cell rCells[], c, wA, wB; // cell, wingA, wingB
		for ( ARegion r : regions ) {											// 1,662,419
			if ( r.numPlaces[x] == 2 ) {										//   443,409
				wA = wB = null;
				rCells = r.cells;
				// seek two cells in r: wA sees cell a, and wB sees cell b.
				// smartass: wA and wB are NOT the same cell, via else if.
				for ( int p : INDEXES[r.places[x]] ) {							//   886,818
					c = rCells[p];
					if ( c.sees[a] && c.indice!=b ) {
						wA = c;
						if ( wB != null )
							return hint(x, y, cells[a], cells[b], wA, wB, r, vcM0,vcM1);
					} else if ( c.sees[b] && c.indice!=a ) {
						wB = c;
						if ( wA != null )
							return hint(x, y, cells[a], cells[b], wA, wB, r, vcM0,vcM1);
					}
				}
			}
		}
		return null;
	}

	// build a new WWingHint
	// x and y are the two values
	// cA and cB are cellA and cellB, the two cells
	// wA and wB are the two wing cells
	// r is the current region
	private AHint hint(final int x, final int y, final Cell cA, final Cell cB
			, final Cell wA, final Cell wB, final ARegion r
			, final long vcM0, final int vcM1) {
		return new WWingHint(grid, this, y, x, cA, cB, wA, wB, r
			, new Pots(x, cA, cB, wA, wB) // greens
			, new Pots(y, cA, cB) // blues
			, new Pots(vcM0,vcM1, maybes, VSHFT[y], DUMMY) // reds
		);
	}

}
