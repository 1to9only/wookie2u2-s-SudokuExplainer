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
 * Here is hobiwans licence statement. Any kudos should flow back to him. The
 * mistakes are all mine.
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
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;

/**
 * Skycraper implements the Skyscraper Sudoku solving technique.
 *
 * NOTE: The package name: sdp stands for SingleDigitPatternSolver which is
 * the name of the HoDoKu class which all the hinters in this directory were
 * boosted from.
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
		// localise fields for speed
		final int[] SHFT = Values.SHFT;
		final Cell[][] rowPairs = this.rowPairs;
		final Cell[][] colPairs = this.colPairs;
		// local variables
		Cell[] rowPair, colPair;
		int sv; // shiftedValue
		int rowCnt, colCnt, r, c;
		// presume that no hint will be found
		boolean result = false;
		// search for rows and cols with exactly two values
		// foreach possible value
		for ( int v=1; v<10; ++v ) {
//			if ( v == 9 )
//				Debug.breakpoint();
			sv = SHFT[v];
			// get all rows and cols with only two values and the cells
			if ( (rowCnt=getPairs(grid.rows, v, rowPairs)) == 0 )
				continue;
			if ( (colCnt=getPairs(grid.cols, v, colPairs)) == 0 )
				continue;
			// ok: now try all combinations of those regions
			for ( r=0; r<rowCnt; ++r ) {
				rowPair = rowPairs[r];
				for ( c=0; c<colCnt; ++c ) {
					colPair = colPairs[c];
					// one end has to be in the same row/col, but: all 4
					// combinations are possible.
					// put cells in the same box in ...Pairs[0],
					// and the "free ends" in ...Pairs[1]
					if (      rowPair[0].box == colPair[0].box )
						; // Null statement; everything is as it should be
					else if ( rowPair[0].box == colPair[1].box )
						swap(colPair);
					else if ( rowPair[1].box == colPair[0].box )
						swap(rowPair);
					else if ( rowPair[1].box == colPair[1].box ) {
						swap(rowPair);
						swap(colPair);
					} else
						continue; // nothing found so continue with next col
					// the indices within the connecting box could be the same
					// so not a 2-String-Kite
					if ( rowPair[0] == colPair[0]
					  || rowPair[0] == colPair[1]
					  || rowPair[1] == colPair[0]
					  || rowPair[1] == colPair[1] )
						continue; // invalid!
					// ok: two strong links, connected in a box, but can
					//     anything be deleted?
					// take the row of the colPair and
					//      the col of the rowPair to get there intersection
//					Cell cross = grid.matrix[colPair[1].y][rowPair[1].x];
					Cell cross = grid.cells[colPair[1].y*9 + rowPair[1].x];
					if ( (cross.maybes.bits & sv) == 0 )
						continue;
					//
					// 2-String-Kite Found! so create hint and add it to accu
					//
					AHint hint = createHint(rowPair, colPair, cross, v);
					result |= hint!=null;
					if ( accu.add(hint) )
						return result;
				}
			}
		}
		return result;
	}

	// if region has only two locations left for value then add it to pairs.
	// @return the number of pairs added
	private int getPairs(ARegion[] regions, int v, Cell[][] pairs) {
		int cnt = 0;
		for ( ARegion row : regions )
			if ( row.indexesOf[v].size == 2 )
				row.at(row.indexesOf[v].bits, pairs[cnt++]);
		return cnt;
	}

	private void swap(Cell[] pair) {
		Cell tmp = pair[0];
		pair[0] = pair[1];
		pair[1] = tmp;
	}

	private AHint createHint(Cell[] rowPair, Cell[] colPair, Cell cross, int v) {
		// build the regions
		final ARegion[] regions = new ARegion[] {
			//blue			  green
			//boxs            row/col
			  rowPair[0].box, rowPair[1].row
			, null,           colPair[0].col
		};
		List<ARegion> bases = Regions.list(rowPair[0].box);
		List<ARegion> covers = Regions.list(rowPair[1].row, colPair[0].col);
		// build the hightlighted (orange) potential values map Cell->Values
		Pots orangePots = new Pots(v, rowPair[1], colPair[1]);
		// build the "fins" (blue) potential values map Cell->Values
		Pots bluePots = new Pots(v, rowPair[0], colPair[0]);
		// build the removeable (red) potential values map Cell->Values
		Pots redPots = new Pots(v, cross);
		// build and return the hint
		return new TwoStringKiteHint(this, v, bases, covers
				, orangePots, bluePots, redPots
				, rowPair.clone(), colPair.clone());
	}

}
