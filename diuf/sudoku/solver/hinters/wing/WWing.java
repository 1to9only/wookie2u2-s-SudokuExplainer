/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * The algorithm for the WWing solving technique was boosted from the current
 * release (AFAIK) of HoDoKu by Keith Corlett in March 2020, so here's hobiwans
 * licence statement. Any kudos should flow back to the algorithms originator.
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
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * WWing Implements the W-Wing Sudoku solving technique.
 * <p>
 * Look for all combinations of bivalue cells with the same candidates. If one
 * is found and it could theoretically eliminate something then we search for a
 * connecting strong link.
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

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// localise fields for speed
		final int[][] VALUESES = Values.ARRAYS;
		// dereference the grid.cells ONCE.
		final Cell[] gridCells = grid.cells;
		// the indices of cells which maybe each potential value
		final Idx[] indicesOf = grid.getIdxs();
		// Two pre-prepaired Idxs of siblings of cellA which maybe 0=v0, 1=v1
		final Idx[] ppis = this.ppis;
		ppis[0].clear();
		ppis[1].clear();
		// local variables
		// an index of the removable (red) cells
		Idx redIdx = new Idx();
		// we start the search with two cells, A and B, which may be the same
		// two values v0 and v1
		Cell cellA, cellB;
		// the easy way to read values from a Values bitset is into an array
		int[] valsA;
		// the cellA.maybes.bits, cell index b, generic index.
		int bitsA, b, i;
		// presume failure, ie that no hint will be found
		boolean result = false;
		AHint hint;
		// foreach cell in the grid except the last one
		MAIN_LOOP: for ( int a=0; a<80; ++a ) {
			// which has exactly 2 potential values
			if ( gridCells[a].maybes.size != 2 )
				continue;
			// hit rate too low to make pre-caching this (above) inefficient
			cellA = gridCells[a];
			// bivalue cell found, so prepare for the "pair" examination
			// get the 2 potential values of cellA into an array
			valsA = VALUESES[bitsA=cellA.maybes.bits];
			// pre-prepair two indexes: siblings of cellA which maybe value0/1
			// if either is empty then there's no W-Wing here.
			if ( ppis[0].set(cellA.buds).and(indicesOf[valsA[0]]).isEmpty() )
				continue;
			if ( ppis[1].set(cellA.buds).and(indicesOf[valsA[1]]).isEmpty() )
				continue;
			// examine each subsequent cell to find a "pair" with same 2 maybes
			for ( b=a+1; b<81; ++b ) {
				if ( gridCells[b].maybes.bits != bitsA )
					continue; // doesnt fit!
				cellB = gridCells[b];
//if ( cellA.id.equals("C6") && cellB.id.equals("H9") )
//	Debug.breakpoint();
				// ok, we have a pair; can anything be eliminated?
				for ( i=0; i<2; ++i ) {
					// find removable cells: siblings of both cellA and cellB
					// which maybe v0 or v1
					if ( redIdx.setAnd(ppis[i], cellB.buds).any()
					  // check for W-Wing for potential values valsA
					  && (hint=checkLink(grid, valsA, a, b, redIdx)) != null ) {
						result = true;
						if ( accu.add(hint) ) //add ignores nulls
							return true;
					}
				}
			}
		}
		return result;
	}
	// Two preprepaired indexes of siblings of cellA which maybe 0=v0, 1=v1
	private final Idx[] ppis = new Idx[]{new Idx(), new Idx()};

	// check for W-Wing in cells[a]'s regions on values
	private AHint checkLink(Grid grid, int[] values, int a, int b, Idx redIdx) {
		final int v1 = values[1];
		final int sv1 = Values.SHFT[v1];
		// the two bivalue cells
		final Cell cA = grid.cells[a];
		final Cell cB = grid.cells[b];
		// the two cells required to complete the WWing pattern
		Cell wA, wB;
		// foreach region with 2 possible positions for v1
		for ( ARegion region : grid.regions ) {
			if ( region.indexesOf[v1].size != 2 )
				continue;
			// strong link; but does it fit?
			wA = wB = null; // ie wing-cells not found
			for ( Cell c : region.cells ) {
				if ( (c.maybes.bits & sv1)==0 || c==cA || c==cB )
					continue;
				// nb: If 'c' sees BOTH bivalue cells it's not a WWing pattern!
				// This is handled with the if/else if.
				if ( !c.notSees[a] ) {
				  wA = c;
				  if ( wB != null ) // W-Wing found!
					return createHint(values, cA, cB, wA, wB, redIdx.cells(grid));
				} else if ( !c.notSees[b] ) {
				  wB = c;
				  if ( wA != null ) // W-Wing found!
					return createHint(values, cA, cB, wA, wB, redIdx.cells(grid));
				}
			}
		}
		return null;
	}

	private AHint createHint(int[] values, Cell cA, Cell cB, Cell wA, Cell wB
			, Cell[] redCells) {
		final int v0=values[0], v1=values[1];

		// This is a nuts, logically, but it works. The fastest way to check
		// that each redCell maybe v0 seems to be to calculate it all and only
		// THEN skip where it's untrue. There MUST be a faster way!
		// KRC 2020-04-30 FYI This happens in 7#top1465.d5.mt (below):
/*
.....9.4.9.28.4..1.7..5..9..83.97....4..68...69.1..8..32....18.85.9...6.....8...3
15,136,1568,2367,127,,2367,,2678,,36,,,37,,3567,357,,14,,1468,236,,1236,236,,268,125,,,245,,,456,125,456,1257,,157,235,,,3579,12357,579,,,57,,234,235,,237,457,,,4679,4567,47,56,,,4579,,,47,,13,13,247,,247,147,16,14679,24567,,256,4579,57,
*/
		final int sv0 = Values.SHFT[v0];
		for ( Cell redCell : redCells )
			if ( (redCell.maybes.bits & sv0) == 0 )
				return null;

		// build the highlighted (green) potential values
		Pots greenPots = new Pots(v1, cA,cB,wA,wB);
		// build the fins (blue) potential values
		Pots bluePots = new Pots(v0, cA,cB);
		// build the removeable (red) potential values
		Pots redPots = new Pots(v0, redCells);
		// build and return the hint
		return new WWingHint(this, v0, v1, cA, cB, wA, wB
				, greenPots, bluePots, redPots);
	}

}
