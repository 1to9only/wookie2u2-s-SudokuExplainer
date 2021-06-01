/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * The algorithm for the Skyscraper solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020, so here's
 * hobiwans licence statement, because any kudos should flow back to where it
 * is due.
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
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;

/**
 * Skycraper implements the Skyscraper Sudoku solving technique.
 *
 * NOTE: The package name sdp stands for SingleDigitPattern, the HoDoKu class
 * from which all hinters in this directory come.
 *
 * @author Keith Corlett 2020-03-25
 */
public class Skyscraper extends AHinter {

	public Skyscraper() {
		super(Tech.Skyscraper);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		if ( accu.isSingle() )
			return search(grid, accu, true)
				|| search(grid, accu, false);
		else // nb: use bitwise-or-operator, so both are always executed
			return search(grid, accu, true)
				 | search(grid, accu, false);
	}

	/**
	 * Search the grid for skyscraper hints by rows or cols.
	 *
	 * @param grid the grid to search
	 * @param accu the hints accumulator to which I add hints
	 * @param isRows true for rows, false for cols
	 * @return were any hints found?
	 */
	private boolean search(Grid grid, IAccumulator accu, final boolean isRows) {
		// localise field for speed
		final Cell[][] pairs = clear(this.pairs);
		// regions is rows or cols, and "other" is cols or rows
		final ARegion[] rowsOrCols = isRows ? grid.rows : grid.cols;
		final int rType = isRows ? Grid.ROW : Grid.COL;
		final int oType = isRows ? Grid.COL : Grid.ROW;
		final Idx[] indicesOf = grid.getIdxs();
		// my variables: a pair is two cells, both in the same row/col
		Cell[] pairA, pairB;
		// the index (positions of cells) of victim cells
		final Idx idx = new Idx();
		// the number pairs collected, and o is short for for otherIndex
		int pairCnt, o;
		// presume that no hint will be found
		boolean result = false;
		// foreach potential value
		for ( int v=1; v<10; ++v ) {
            // find all rowsOrCols with two possible positions for the value 'v'
			pairCnt = 0;
			for ( ARegion r : rowsOrCols )
				if ( r.indexesOf[v].size == 2 )
					r.at(r.indexesOf[v].bits, pairs[pairCnt++]);
			if ( pairCnt < 2 )
				continue;
            // now we evaluate all combinations of those regions
            for ( int a=0,A=pairCnt-1; a<A; ++a ) {
				pairA = pairs[a]; // the first pair of cells
				// foreach subsequent pair (a forward search)
                for ( int b=a+1; b<pairCnt; ++b ) {
					pairB = pairs[b]; // the second pair of cells
                    // one end has to be in the same col/row
					if (      pairA[0].regions[oType] == pairB[0].regions[oType] )
						o = 1; // "other" end is 1
					else if ( pairA[1].regions[oType] == pairB[1].regions[oType] )
						o = 0; // "other" end is 0
					else
						continue; // neither end lines up so not a Skyscraper
                    // and the "other ends" need to NOT be in same col/row.
					if ( pairA[o].regions[oType] == pairB[o].regions[oType] )
						continue; // it's an X-Wing which is not my problem.
					// so: does it eliminate any maybes?
					if ( idx.setAnd(pairA[o].buds, pairB[o].buds)
							.and(indicesOf[v]).isEmpty() )
						continue; // nothing to eliminate
					// Skyscraper found!
					AHint hint = createHint(grid, idx, v, o, pairA, pairB
							, rType, oType);
					if ( hint != null ) {
						result = true;
						if ( accu.add(hint) )
							return true;
					}
				}
			}
		}
		return result;
	}
	// 18 is large enough, by experimentation
	private final Cell[][] pairs = new Cell[18][2];

	private AHint createHint(Grid grid, Idx idx, int v, int o
			, Cell[] pairA, Cell[] pairB, int rType, int oType) {
		// build the removeable (red) potential values map Cell/s->Value
		Pots reds = new Pots(v, idx.cells(grid));
		// Skyscraper is producing hints with no eliminations!
		if ( !reds.clean() ) // !!redPots.isEmpty(). sigh.
			return null; // Never happens. Never say never.
		// workout the region type from the other-region-type (weird huh?)
		final int r = o==0 ? 1 : 0;
		// build the regions
		List<ARegion> bases = Regions.list(pairA[r].regions[rType]
				                         , pairB[r].regions[rType]);
		List<ARegion> covers = Regions.list(pairA[o].regions[oType]
										  , pairB[o].regions[oType]);
		// build the hightlighted (orange) potential values map Cells->Value
		Pots oranges = new Pots(v, pairA, pairB);
		// build and return the hint
		return new SkyscraperHint(this, v, bases, covers, reds, oranges);
	}

	private Cell[][] clear(Cell[][] pairs) {
		for ( Cell[] pair : pairs )
			pair[0] = pair[1] = null;
		return pairs;
	}

}
