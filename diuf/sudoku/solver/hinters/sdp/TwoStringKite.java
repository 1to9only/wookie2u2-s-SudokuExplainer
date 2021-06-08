/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * The algorithm for the TwoStringKite solving technique was boosted from the
 * current release (AFAIK) of hobiwan's HoDoKu by Keith Corlett in March 2020.
 * Here is hobiwans licence statement. Kudos to hobiwan. The mistakes are mine.
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
 * A Two String Kite is a box (the kite) having a row and a col (two strings)
 * both with two places for v, such that no matter where we put v in the box,
 * cells seeing both "far ends" of the strings cannot be v.
 * <p>
 * A TwoStringKite is a simplified faster Unary (single digit) Chain; so the
 * UnaryChainer finds all TwoStringKite hints (et al), but it does so slower,
 * and the hint explanations are (IMHO) harder to follow.
 * <p>
 * The package name SDP stands for SingleDigitPattern which is the HoDoKu class
 * which fathered all the hinters in this directory.
 *
 * @author Keith Corlett Mar 25
 */
public class TwoStringKite extends AHinter {

	private final Cell[][] rowPairs = new Cell[9][2];
	private final Cell[][] colPairs = new Cell[9][2];

	public TwoStringKite() {
		super(Tech.TwoStringKite);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// variables
		Cell[] rowPair, colPair;
		Cell victim;
		AHint hint;
		int sv; // shiftedValue
		int nRows, nCols, r, c;
		boolean go;
		// localise fields for speed
		final Cell[][] rowPairs = this.rowPairs;
		final Cell[][] colPairs = this.colPairs;
		// presume that no hint will be found
		boolean result = false;
		// search for rows and cols with exactly two values
		// foreach possible value
		for ( int v=1; v<10; ++v ) {
			sv = VSHFT[v];
			// get all rows and cols with only two values and the cells
			if ( (nRows=getPairs(grid.rows, v, rowPairs)) != 0
			  && (nCols=getPairs(grid.cols, v, colPairs)) != 0 ) {
				// ok: now try all combinations of those regions
				for ( r=0; r<nRows; ++r ) {
					rowPair = rowPairs[r];
					for ( c=0; c<nCols; ++c ) {
						colPair = colPairs[c];
						// one end has to be in the same row/col, but: all 4
						// combinations are possible.
						// put cells in the same box in ...Pairs[0],
						// and the "free ends" in ...Pairs[1]
						if (      rowPair[0].box == colPair[0].box )
							go = true; // nothing to swap
						else if ( rowPair[0].box == colPair[1].box )
							go = swap(colPair);
						else if ( rowPair[1].box == colPair[0].box )
							go = swap(rowPair);
						else if ( rowPair[1].box == colPair[1].box )
							go = swap(rowPair) | swap(colPair);
						else
							go = false; // nothing found so onto next col
						// the indices within the connecting box could be the
						// same, so not a TwoStringKite
						if ( go
						  && rowPair[0] != colPair[0]
						  && rowPair[0] != colPair[1]
						  && rowPair[1] != colPair[0]
						  && rowPair[1] != colPair[1]
						) {
							// ok: two strong links connected by a box,
							//     but does it eliminate any maybes?
							// take the row of the colPair
							//  and the col of the rowPair
							//   to get there intersection
							victim = grid.cells[colPair[1].y*9 + rowPair[1].x];
							if ( (victim.maybes.bits & sv) != 0 ) {
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
		}
		return result;
	}

	/**
	 * Add each region with two places for value to the pairs array, and return
	 * how many.
	 *
	 * @param regions to find pairs in
	 * @param v the candidate value
	 * @param pairs array to add pairs to
	 * @return the number of pairs added
	 */
	private int getPairs(ARegion[] regions, int v, Cell[][] pairs) {
		int p = 0; // pair index
		for ( ARegion row : regions )
			if ( row.indexesOf[v].size == 2 )
				row.at(row.indexesOf[v].bits, pairs[p++]);
		return p;
	}

	/**
	 * Swap positions of pair[0] and pair[1] and return true.
	 * @param pair two cells to swap positions of.
	 * @return true
	 */
	private boolean swap(Cell[] pair) {
		Cell tmp = pair[0];
		pair[0] = pair[1];
		pair[1] = tmp;
		return true;
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
		Pots reds = new Pots(v, victim);
		// build and return the hint
		return new TwoStringKiteHint(this, v, bases, covers, oranges, blues
				, reds, rowPair.clone(), colPair.clone());
	}

}
