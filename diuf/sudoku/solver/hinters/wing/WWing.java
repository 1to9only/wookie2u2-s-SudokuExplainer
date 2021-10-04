/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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

import diuf.sudoku.Cells;
import diuf.sudoku.Idx;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Debug;


/**
 * WWing Implements the W-Wing Sudoku solving technique.
 * <p>
 * A W-Wing is a pair of cells (not in a single region) with the same two
 * maybes having a strong link between them.
 * <p>
 * KRC 2021-07-07 //Change how WWing works: It was processing every cell in the
 * grid in the cell-B-loop. Now both loops start with the cached Bivalue index,
 * and cellB uses the new Idx.setExcept method to drop cellA from an Idx, then
 * the new Idx.and(aa, bb) method to reduce it down to cell-B's with the same
 * two maybes as cellA, which should all be more efficient; but it isn't, it's
 * actually about 20% slower, so I'll BFIIK! I'm keeping it anyway, for now.
 */
public final class WWing extends AHinter
//		implements diuf.sudoku.solver.IReporter
{

	public WWing() {
		super(Tech.W_Wing);
	}

//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teef("// "+tech.name()+" pass victims %d of %d, empty %d of %d\n"
//				, victPass,victCnt, emptyPass,emptyCnt);
//	}

	private void clean() {
		Cells.cleanCasA();
		Cells.cleanCasB();
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		AHint hint; // The hint, if we ever find one
		int[] vA; // an array to read values from a Values bitset
		// indices of cells which maybe each value 1..9
		final Idx[] candidates = grid.idxs; // CACHED!
		final Idx bud0 = this.bud0; // indices of buds of cA which maybe v0
		final Idx bud1 = this.bud1; // indices of buds of cA which maybe v1
		final Idx victims = this.victims; // indices of removable (red) cells
		// presume that no hint will be found
		boolean result = false;
		// foreach bivalue cell in the grid
		for ( Cell cA : grid.cells )
			// if cell-a has 2 maybes
			if ( cA.size==2
			  // and a tautology, to set vA
			  && (vA=VALUESES[cA.maybes]) != null
			  // and buddies of cellA which maybe v0/v1
			  //     if either is empty then there's no W-Wing here.
			  && bud0.setAndAny(cA.buds, candidates[vA[0]])
			  && bud1.setAndAny(cA.buds, candidates[vA[1]]) )
				// find a "pair" cell (with same 2 maybes)
				for ( Cell cB : grid.cells )
					if ( cB.maybes==cA.maybes && cB!=cA ) {
						// ok, we have a pair; can anything be eliminated?
						// find removable cells: siblings of both A and B
						// which maybe v0 or v1
						if ( victims.setAndAny(bud0, cB.buds)
						  // search reds for a strong link
						  && (hint=checkLink(grid, vA, cA, cB, victims)) != null ) {
							result = true;
							if ( accu.add(hint) ) {
								clean();
								return result;
							}
							hint = null;
						}
						if ( victims.setAndAny(bud1, cB.buds)
						  // search reds for a strong link
						  && (hint=checkLink(grid, vA, cA, cB, victims)) != null ) {
							result = true;
							if ( accu.add(hint) ) {
								clean();
								return result;
							}
							hint = null;
						}
					}
		clean();
		return result;
	}
	// buddies of cellA which maybe 0=v0, 1=v1
	private final Idx bud0 = new Idx();
	private final Idx bud1 = new Idx();
	private final Idx victims = new Idx();

	// check for W-Wing in cells[a]'s regions on values
	private AHint checkLink(Grid grid, final int[] values, final Cell cA
			, final Cell cB, final Idx victims) {
		// BFIIK: why seek the second value first, and not first value first?
		final int v1 = values[1];
		final int sv1 = VSHFT[v1];
		// the two wing cells required to complete the WWing pattern
		Cell wA, wB;
		// foreach region with 2 possible positions for v1
		for ( ARegion region : grid.regions ) {
			if ( region.indexesOf[v1].size == 2 ) {
				// strong link; but does it fit?
				wA = wB = null; // not found
				for ( Cell c : region.cells ) {
					if ( (c.maybes & sv1)!=0 && c!=cA && c!=cB ) {
						// If 'c' sees BOTH bivalue cells it's NOT a WWing!
						// This is handled sneakily with the if/elseIf.
						if ( c.sees[cA.i] ) {
							wA = c;
							if ( wB != null ) // W-Wing found!
								return createHint(values, cA, cB, wA, wB
										, victims.cellsA(grid));
						} else if ( c.sees[cB.i] ) {
							wB = c;
							if ( wA != null ) // W-Wing found!
								return createHint(values, cA, cB, wA, wB
										, victims.cellsA(grid));
						}
					}
				}
			}
		}
		return null;
	}

	private AHint createHint(int[] values, Cell cA, Cell cB, Cell wA, Cell wB
			, final Cell[] victimCells) {
		final int v0=values[0], v1=values[1];
		// Check each victimCell maybe v0 by calculating everything and only
		// then skipping when untrue; which works, but
		// There MUST be a faster way!
		final int sv0 = VSHFT[v0];
		for ( Cell c : victimCells )
			if ( (c.maybes & sv0) == 0 )
				return null;
		// build the highlighted (green) potential values
		final Pots greenPots = new Pots(v1, cA,cB,wA,wB);
		// build the fins (blue) potential values
		final Pots bluePots = new Pots(v0, cA,cB);
		// build the removeable (red) potential values
		final Pots redPots = new Pots(v0, victimCells);
		// build and return the hint
		return new WWingHint(this, v0, v1, cA, cB, wA, wB
				, greenPots, bluePots, redPots);
	}

}
