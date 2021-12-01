/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the Skyscraper solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020. Kudos
 * to hobiwan. Mistakes are mine.
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

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;

/**
 * Skyscraper implements the Skyscraper Sudoku solving technique.
 * <p>
 * A Skyscraper is a simplified Unary (single digit) Forcing Chain. A value has
 * two places in two rows/cols, one of which is in the same col/row, so one of
 * the two cells at the other "misaligned end" of the Skyscraper must be value;
 * eliminating value from cells seeing both "other" ends of the Skyscraper.
 * <p>
 * All Skyscrapers are found by the Unary Chainer. Skyscraper exists coz it's
 * faster to find this narrower pattern, so SE is probably faster without it,
 * but this applies to many hinters: many patterns are an over-simplification
 * of a heavier/slower generalised pattern, especially the chainers.
 * <p>
 * For speed rely on the chainers; but to explain a puzzle use lighter hinters,
 * with explanations that users can follow. Maybe 1% of users will ever walk
 * through the justification of a forcing-chain, let alone apply the technique
 * manually. Maybe 25% can/will follow a Skyscraper, hence Skyscraper exists.
 * <p>
 * It's around here that we loose most users that downloaded SE to learn how to
 * solve Sudoku puzzles manually; here-after it's hard work, which folks do NOT
 * like; so somewhere around SDP we start whittling our way down to the real
 * thinkers; probably mostly programmers. Solving hard puzzle manually would
 * take bloody years; so yeah, most folks tap-out somewhere around here.
 * <p>
 * The package name 'sdp' stands for SingleDigitPattern, which is the HoDoKu
 * class that fathered these hinters.
 *
 * @author Keith Corlett 2020-03-25
 */
public class Skyscraper extends AHinter {

	// the "other" type of region for row and col, box exists but is never used
	private static final int[] OTHER_TYPE = {0, 2, 1};

	/**
	 * clear: set each cell-reference in the pairs array to null.
	 *
	 * @param pairs array to clear
	 */
	private static void clear(Cell[][] pairs) {
		for ( Cell[] pair : pairs )
			pair[0] = pair[1] = null;
	}

	/**
	 * pairs is an array of arrays-of-two-cells. 18 is enough by experiment.
	 * It's a field rather than re-create the array in each findHints.
	 * <p>
	 * NOTE that each cell-reference is cleared upon exit, because each
	 * cell-reference holds the whole Grid in memory, ie a memory leak.
	 */
	private final Cell[][] pairs = new Cell[18][2];

	/**
	 * victims is an Idx containing the indices of cells from which v can be
	 * removed. It's a field rather than re-create an Idx in each findHints.
	 */
	private final Idx victims = new Idx();

	/**
	 * Constructor.
	 */
	public Skyscraper() {
		super(Tech.Skyscraper);
	}

	private Grid grid;
	private IAccumulator accu;

	/**
	 * Find first/all Skyscraper hints in the grid, and add them to the accu.
	 * If the accu isSingle then stop looking when the first hint is found.
	 *
	 * @param grid the grid to search
	 * @param accu an implementation of IAccumulator
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		boolean result = false;
		this.grid = grid;
		this.accu = accu;
		try {
			if ( accu.isSingle() ) { // normal short-circuiting or
				result = search(grid.rows) || search(grid.cols);
			} else { // bitwise-or operator always executes both
				result = search(grid.rows) | search(grid.cols);
			}
		} finally {
			clear(this.pairs); // each cell reference holds the whole grid
			this.grid = null;
			this.accu = null;
			Cells.cleanCasA();
		}
		return result;
	}

	/**
	 * Search for Skyscrapers in rows or cols.
	 *
	 * @param lines grid.rows or grid.cols
	 * @return were any hints found?
	 */
	private boolean search(final ARegion[] lines) {
		// a pair is the 2 cells in a row/col which both maybe v
		Cell[] pA, pB; // pairA and pairB
		// the cross-regions of the two cells in pairA
		ARegion r0, r1;
		int n // the number of pairs collected
		  , m // n - 1
		  , o // the otherIndex
		  , a // pairA index
		  , b; // pairB index
		// localise field for speed
		final Cell[][] pairs = this.pairs; // an array of a-pair-of-cells
		// indices of cells in Grid which maybe value 1..9
		final Idx[] idxs = grid.idxs;
		// indices of removable v's, if any
		final Idx victims = this.victims;
		// work-out the regionType and otherType from the given lines
		final int rT = lines[0].typeIndex	// regionType
				, oT = OTHER_TYPE[rT];		// otherType
		// presume that no hint will be found
		boolean result = false;
		// foreach potential value
		for ( int v=1; v<VALUE_CEILING; ++v ) {
            // collect rows/cols with two places for v
			n = 0; // the number of pairs collected
			for ( ARegion r : lines ) { // grid.rows or grid.cols
				if ( r.ridx[v].size == 2 ) {
					r.at(r.ridx[v].bits, pairs[n++]);
				}
			}
			// if we collected atleast two pairs
			if ( n > 1 ) {
				// examine each combination of a and b pairs (forwards-only)
				for ( a=0,m=n-1; a<m; ++a ) {
					// the first pair
					r0 = (pA=pairs[a])[0].regions[oT]; // cross-region 0
					r1 = pA[1].regions[oT]; // cross-region 1
					for ( b=a+1; b<n; ++b ) {
						// the second pair
						pB = pairs[b];
						// if "this" end is in the same col/row
						if ( ( (r0==pB[0].regions[oT] && (o=1)==1) // "other" end is 1
							|| (r1==pB[1].regions[oT] && (o=0)==0) ) // "other" end is 0
						  // and the "other" end is NOT in same col/row,
						  // else it's an X-Wing, which is not my problem
						  && pA[o].regions[oT] != pB[o].regions[oT]
						  // and there are eliminations: common buddies of
						  // the-other-ends-of-the-two-pairs which maybe v
						  && victims.setAndAny(pA[o].buds, pB[o].buds, idxs[v])
						) {
							// FOUND a Skyscraper!
							// build the removable (red) potentials
							final Pots reds = new Pots(v, victims.cellsA(grid));
// clean removes an elimination that doesn't exist in the grid, ie this maybe
// has already been removed. I don't understand why such bad-elims occur, but
// this deals with them, it's just a bit slow.
//							if ( reds.clean() ) {
								result = true;
								if ( accu.add(createHint(reds, v, o, pA, pB
										, rT, oT)) ) {
									return result;
								}
//							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Create and return a new SkyscraperHint.
	 *
	 * @param reds the removable Cell=>Values
	 * @param v the Skyscraper candidate value
	 * @param o the index of the other end (0 or 1)
	 * @param pA pairA the first pair-of-v's
	 * @param pB pairB the second pair-of-v's
	 * @param rT regionType the region type ROW/COL
	 * @param oT otherType the other type COL/ROW
	 * @return a new SkyscraperHint
	 */
	private AHint createHint(final Pots reds, final int v, final int o
			, final Cell[] pA, final Cell[] pB, final int rT, final int oT) {
		// workout "this" end from the "other" end
		final int t = o==0 ? 1 : 0;
		// build the regions
		final List<ARegion> bases = Regions.list(pA[t].regions[rT]
											   , pB[t].regions[rT]);
		final List<ARegion> covers = Regions.list(pA[o].regions[oT]
												, pB[o].regions[oT]);
		// build the hightlighted (green) potential values map Cells->Value
		final Pots greens = new Pots();
		final Integer sv = VSHFT[v];
		greens.put(pA[0], sv);
		greens.put(pA[1], sv);
		greens.put(pB[0], sv);
		greens.put(pB[1], sv);
		// build and return the hint
		return new SkyscraperHint(this, v, bases, covers, reds, greens);
	}

}
