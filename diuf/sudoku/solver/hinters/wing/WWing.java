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
 * The mistakes are all mine. ~KRC.
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
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.LinkedList;


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

	// nb: caching this ____s-up the the generator!
	private static Cell[] getBivalueCells(Grid grid) {
		LinkedList<Cell> list = new LinkedList<>();
		for ( Cell cell : grid.cells )
			if ( cell.maybes.size == 2 )
				list.add(cell);
		return list.toArray(new Cell[list.size()]);
	}

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
		// The hint, if we ever find one
		AHint hint;
		// the easy way to read values from a Values bitset is into an array
		int[] vA;
		// the cellA.maybes.bits, cell index b, generic index.
		int bitsA;
		// the indices of cells which maybe each potential value
		final Idx[] candidates = grid.getIdxs();
		// get an array (for speed) of bivalue (maybes.size==2) cells in grid
		final Cell[] bivalueCells = getBivalueCells(grid);
		// two pre-prepaired Idxs of buddies of cellA which maybe 0=v0, 1=v1
		final Idx bud0 = this.bud0.clear();
		final Idx bud1 = this.bud1.clear();
		// an index of the (potentially) removable (red) cells
		final Idx reds = new Idx();
		// presume failure, ie that no hint will be found
		boolean result = false;
		// foreach cell in the grid except the last one
		for ( Cell cA : bivalueCells ) {
			// bivalue cell found, so prepare for the "pair" examination
			// get the 2 potential values of cellA into an array
			vA = VALUESES[bitsA=cA.maybes.bits];
			// pre-prepair 2 Idxs: buddies of cellA which maybe value0/1
			// if either is empty then there's no W-Wing here.
			if ( bud0.setAndAny(cA.buds, candidates[vA[0]])
			  && bud1.setAndAny(cA.buds, candidates[vA[1]]) ) {
				// find a "pair" cell (with same 2 maybes)
				for ( Cell cB : bivalueCells ) {
					if ( cB.maybes.bits==bitsA && cB!=cA ) {
//if ( cellA.id.equals("C6") && cellB.id.equals("H9") )
//	Debug.breakpoint();
						// ok, we have a pair; can anything be eliminated?
						// find removable cells: siblings of both A and B
						// which maybe v0 or v1
						if ( reds.setAndAny(bud0, cB.buds)
						  // check for W-Wing for potential values valsA
						  && (hint=checkLink(grid, vA, cA, cB, reds)) != null ) {
							result = true;
							if ( accu.add(hint) ) //add ignores nulls
								return true;
						}
						if ( reds.setAndAny(bud1, cB.buds)
						  // check for W-Wing for potential values valsA
						  && (hint=checkLink(grid, vA, cA, cB, reds)) != null ) {
							result = true;
							if ( accu.add(hint) ) //add ignores nulls
								return true;
						}
					}
				}
			}
		}
		return result;
	}
	// Two preprepaired indexes of siblings of cellA which maybe 0=v0, 1=v1
	private final Idx bud0 = new Idx();
	private final Idx bud1 = new Idx();

	// check for W-Wing in cells[a]'s regions on values
	private AHint checkLink(Grid grid, final int[] values, final Cell cA
			, final Cell cB, final Idx reds) {
		// BFIIK: the second value, yes really!
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
					if ( (c.maybes.bits & sv1)!=0 && c!=cA && c!=cB ) {
						// If 'c' sees BOTH bivalue cells it's NOT a WWing!
						// This is handled sneakily with the if/elseIf.
						if ( c.sees[cA.i] ) {
							wA = c;
							if ( wB != null ) // W-Wing found!
								return createHint(values, cA, cB, wA, wB
										, reds.cells(grid));
						} else if ( c.sees[cB.i] ) {
							wB = c;
							if ( wA != null ) // W-Wing found!
								return createHint(values, cA, cB, wA, wB
										, reds.cells(grid));
						}
					}
				}
			}
		}
		return null;
	}

	private AHint createHint(int[] values, Cell cA, Cell cB, Cell wA, Cell wB
			, Cell[] reds) {
		final int v0=values[0], v1=values[1];

		// This is a nuts, logically, but it works. The fastest way to check
		// that each redCell maybe v0 seems to be to calculate it all and only
		// THEN skip where it's untrue. There MUST be a faster way!
		// KRC 2020-04-30 FYI This happens in 7#top1465.d5.mt (below):
/*
.....9.4.9.28.4..1.7..5..9..83.97....4..68...69.1..8..32....18.85.9...6.....8...3
15,136,1568,2367,127,,2367,,2678,,36,,,37,,3567,357,,14,,1468,236,,1236,236,,268,125,,,245,,,456,125,456,1257,,157,235,,,3579,12357,579,,,57,,234,235,,237,457,,,4679,4567,47,56,,,4579,,,47,,13,13,247,,247,147,16,14679,24567,,256,4579,57,
*/
		final int sv0 = VSHFT[v0];
		for ( Cell red : reds )
			if ( (red.maybes.bits & sv0) == 0 )
				return null;

		// build the highlighted (green) potential values
		Pots greenPots = new Pots(v1, cA,cB,wA,wB);
		// build the fins (blue) potential values
		Pots bluePots = new Pots(v0, cA,cB);
		// build the removeable (red) potential values
		Pots redPots = new Pots(v0, reds);
		// build and return the hint
		return new WWingHint(this, v0, v1, cA, cB, wA, wB
				, greenPots, bluePots, redPots);
	}

}
