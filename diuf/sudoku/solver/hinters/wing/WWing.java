/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the WWing solving technique was boosted by Keith Corlett
 * in March 2020 from the current release AFAIK of HoDoKu by Bernhard Hobiger.
 * Kudos is hobiwans. Mistakes are mine.
 *
 * Here's hobiwans standard licence statement:
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
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * WWing Implements the W-Wing Sudoku solving technique.
 * <p>
 * A WWing is a pair of cells (not in a single region) with the same two maybes
 * having a strong link between them. That link is a region with two places for
 * one of these two values, and each cell in that region sees one of the two
 * bivalued cells.
 * <p>
 * KRC 2021-08-19 tried everything I can think of to speed this up, but no joy.
 * bivi Idx reduces bivs*81 to bivs*bivs but its slower, even with local Idx's,
 * which I don't really understand. It's faster to just iterate grid.cells.
 * 2021-11-25 Search for a biplace region on BOTH values of the bivalued cells.
 */
public final class WWing extends AHinter {

	// a fast way to get the other array index, of a bivalue cells maybes.
	// 1 is the OPPOSITE of 0; and 0 is the OPPOSITE of 1.
	private static final int[] OPPOSITE = {1, 0};

	private Grid grid;
	private ARegion[] regions;

	public WWing() {
		super(Tech.W_Wing);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Idx t0, t1;
		int[] vA; // an array to read values from a Values bitset
		final Idx[] idxs = grid.idxs;
		final Cell[] cells = grid.cells;
		AHint hint = null;
		boolean result = false;
		this.grid = grid;
		this.regions = grid.regions;
		try {
			for ( Cell cA : cells ) {
				if ( cA.size == 2 ) {
					vA = VALUESES[cA.maybes];
//					budA0.setAnd(cA.buds, idxs[vA[0]]);
					budA0.a0 = (t0=cA.buds).a0 & (t1=idxs[vA[0]]).a0;
					budA0.a1 = t0.a1 & t1.a1;
					budA0.a2 = t0.a2 & t1.a2;
//					budA1.setAnd(cA.buds, idxs[vA[1]]);
					budA1.a0 = (t0=cA.buds).a0 & (t1=idxs[vA[1]]).a0;
					budA1.a1 = t0.a1 & t1.a1;
					budA1.a2 = t0.a2 & t1.a2;
					for ( Cell cB : cells ) {
						if ( cB.maybes==cA.maybes && cB!=cA ) {
							if ( victims.setAndAny(budA0, cB.buds)
							  && (hint=prove(cA, cB, vA, 1)) != null ) {
								result = true;
								if ( accu.add(hint) ) {
									return result;
								}
								hint = null;
							}
							if ( victims.setAndAny(budA1, cB.buds)
							  && (hint=prove(cA, cB, vA, 0)) != null ) {
								result = true;
								if ( accu.add(hint) ) {
									return result;
								}
								hint = null;
							}
						}
					}
				}
			}
		} finally {
			this.grid = null;
			this.regions = null;
		}
		return result;
	}
	// buddies of cellA which maybe 0=v0, 1=v1
	private final Idx budA0 = new Idx();
	private final Idx budA1 = new Idx();
	private final Idx victims = new Idx();

	/**
	 * Prove isn't the normal "search" for victims; instead it builds the case
	 * for conviction of the predetermined victims. So I seek a region with two
	 * places for v (vA[i]), where both places see both cA and cB, one each, in
	 * which case I return a hint, else (most commonly) I return null.
	 *
	 * @param cA the first bivalue Cell
	 * @param cB the second bivalue Cell, with the same 2 maybes a cA
	 * @param vA an array of the maybes of cA and cB
	 * @param i the index of the maybe to check in vA. <br>
	 *  If victims is set to the buddies of cA and cB on vA[0] then i is 1. <br>
	 *  If victims is set to the buddies of cA and cB on vA[1] then i is 0.
	 * @return a hint, if the case is proven, else null.
	 */
	private AHint prove(final Cell cA, final Cell cB, final int[] vA, final int i) {
		Cell[] rCells; // ARegion.cells
		Cell wA, wB, c;
		int j;
		final int iA = cA.i;
		final int iB = cB.i;
		final int v = vA[i];
		final int sv = VSHFT[v];
		for ( ARegion r : regions ) {
			if ( r.ridx[v].size == 2 ) {
				rCells = r.cells;
				wA = wB = null;
				for ( j=0; j<REGION_SIZE; ++j ) {
					if ( (rCells[j].maybes & sv) != 0 ) {
						// if c sees BOTH bivs it's NOT a WWing
						// Handled sneakily with the if/elseif.
						if ( (c=rCells[j]).sees[iA] && c!=cB ) {
							wA = c;
							if ( wB != null ) {
								return createHint(vA[OPPOSITE[i]], vA[i]
										, cA, cB, wA, wB);
							}
						} else if ( c.sees[iB] && c!=cA ) {
							wB = c;
							if ( wA != null ) {
								return createHint(vA[OPPOSITE[i]], vA[i]
										, cA, cB, wA, wB);
							}
						}
					}
				}
			}
		}
		return null;
	}

	// A line of code so complex it has it's own method, rather than repeat it.
	private AHint createHint(final int v0, int v1, final Cell cA, final Cell cB
			, final Cell wA, final Cell wB) {
		return new WWingHint(this, v0, v1, cA, cB, wA, wB
				, new Pots(v1, cA,cB,wA,wB) // greens
				, new Pots(cA,cB, v0) // blues
				, new Pots(victims, grid, VSHFT[v0], F) // reds
		);
	}

}
