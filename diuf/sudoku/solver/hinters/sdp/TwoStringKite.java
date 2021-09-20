/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the TwoStringKite solving technique was boosted from the
 * current release (AFAIK) of hobiwan's HoDoKu by Keith Corlett in March 2020.
 * Kudos hobiwans. Mistakes mine.
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
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;


/**
 * TwoStringKite implements the TwoStringKite Sudoku solving technique.
 * <p>
 * A Two String Kite is a box (the kite) having both a row and a col with two
 * places for v (the two strings), such that no matter where we put v in the
 * box, any v's seeing both "other ends" of our two strings are history.
 * <p>
 * A TwoStringKite is a simplified faster Unary (single digit) Chain; so the
 * UnaryChainer finds all TwoStringKite hints (et al), but it's slower, and
 * the hint explanations are (IMHO) harder to follow.
 * <p>
 * The package name 'sdp' stands for SingleDigitPattern, which is the HoDoKu
 * class that fathered these hinters.
 *
 * @author Keith Corlett Mar 25
 */
public class TwoStringKite extends AHinter {

	/**
	 * Swap positions of pair[0] and pair[1] and return true.
	 * @param pair two cells to swap positions of.
	 * @return true
	 */
	private static boolean swap(Cell[] pair) {
		Cell tmp = pair[0];
		pair[0] = pair[1];
		pair[1] = tmp;
		return true;
	}

	private final Cell[][] rowPairs = new Cell[9][2];
	private final Cell[][] colPairs = new Cell[9][2];

	public TwoStringKite() {
		super(Tech.TwoStringKite);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Cell[] rowPair, colPair; // a pair of cells in the row, and col
		Cell victim; // the cell from which we eliminate v
		AHint hint; // the hint, if we ever find one
		int sv // shiftedValue: the bitset representation of v: 1<<(v-1)
		  , r, nRowPairs // rowPair index, number of rowPairs
		  , c, nColPairs; // colPair index, number of colPairs
		// localise fields for speed. I don't deeply understand this, but it
		// looks like each heap-reference is slower than each stack-reference,
		// so just putting heap-stuff on the stack saves time, especially in an
		// "all in one" method like this where all the heap-stuff is referenced
		// multiple times.
		// The cut-off seems to be 3, that is creating a stack-reference takes
		// about as long as 2.5 heap-references, so the stack is ahead as long
		// as it's referenced 3+ times. It's hard to write really fast code!
		final Cell[][] rowPairs = this.rowPairs;
		final Cell[][] colPairs = this.colPairs;
		final Cell[] cells = grid.cells;
		// presume that no hint will be found
		boolean result = false;
		// foreach possible value in 1..9
		for ( int v=1; v<10; ++v ) {
			sv = VSHFT[v]; // lookup once to save some time, I hope
			// get cell-pairs from rows and cols with two places for v
			if ( (nRowPairs=pairs(grid.rows, v, rowPairs)) != 0
			  && (nColPairs=pairs(grid.cols, v, colPairs)) != 0 ) {
				// examine each combination of rowPairs and colPairs
				for ( r=0; r<nRowPairs; ++r ) {
					rowPair = rowPairs[r];
					for ( c=0; c<nColPairs; ++c ) {
						colPair = colPairs[c];
						// we seek two pairs with an end in the same box, but
						// that's 4 possible end-combinations, so first we must
						// put cells in the same box in ...Pair[0],
						// so the "free" end ends-up in ...Pair[1]
						if ( (  rowPair[0].box==colPair[0].box // no swaps
							|| (rowPair[0].box==colPair[1].box && swap(colPair))
							|| (rowPair[1].box==colPair[0].box && swap(rowPair))
							|| (rowPair[1].box==colPair[1].box && (swap(rowPair) | swap(colPair))) )
						  // and we require zero double-ups
						  && rowPair[0] != colPair[0]
						  && rowPair[0] != colPair[1]
						  && rowPair[1] != colPair[0]
						  && rowPair[1] != colPair[1]
						  // found two strong links from a box, so any elims?
						  // get the y=row of the colPair
						  // and the x=col of the rowPair
						  // and examine the cell at there intersection
						  && ((victim=cells[colPair[1].y*9 + rowPair[1].x]).maybes.bits & sv) != 0
						) {
							// FOUND TwoStringKite!
							hint = createHint(rowPair, colPair, victim, v);
							result |= hint!=null;
							if ( accu.add(hint) )
								return result;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Add each region with two places for v to the pairs array,
	 * and return how many.
	 *
	 * @param regions to find pairs in: grid.rows or grid.cols
	 * @param v the candidate value
	 * @param pairs array to add pairs to
	 * @return the number of pairs added
	 */
	private int pairs(ARegion[] regions, int v, Cell[][] pairs) {
		int i = 0;
		for ( ARegion r : regions )
			if ( r.indexesOf[v].size == 2 )
				r.at(r.indexesOf[v].bits, pairs[i++]);
		return i;
	}

	/**
	 * Create and return a new TwoStringKiteHint.
	 *
	 * @param rowPair the pair of cells in the erRow
	 * @param colPair the pair of cells in the erCol
	 * @param victim the cell to eliminate from
	 * @param v the candidate value
	 * @return a new TwoStringKiteHint
	 */
	private AHint createHint(Cell[] rowPair, Cell[] colPair, Cell victim, int v) {
		// build a list of bases (the box), and a list of covers (row and col)
		List<ARegion> bases = Regions.list(rowPair[0].box);
		List<ARegion> covers = Regions.list(rowPair[1].row, colPair[0].col);
		// build the hightlighted (orange) Cell->Values
		Pots oranges = new Pots(v, rowPair[1], colPair[1]);
		// build the "fins" (blue) Cell->Values
		Pots blues = new Pots(v, rowPair[0], colPair[0]);
		// build the removeable (red) Cell->Values
		Pots reds = new Pots(victim, v);
		// build and return the hint
		return new TwoStringKiteHint(this, v, bases, covers, oranges, blues
				, reds, rowPair.clone(), colPair.clone());
	}

}
