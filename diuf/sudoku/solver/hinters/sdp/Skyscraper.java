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

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.ROW;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
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
 * eliminating value from cells seeing both misaligned ends of the Skyscraper.
 * <p>
 * All Skyscrapers are found by the Unary Chainer. Skyscraper exists coz it's
 * the fastest way to find this narrower pattern. SE is probably faster without
 * it; but this applies to most hinters. Most patterns are over-simplifications
 * of a heavier generalised pattern, especially the chainers.
 * <p>
 * For speed rely on the chainers; but to explain a puzzle use lighter hinters,
 * with explanations that users can follow. Maybe 1% of users will ever walk
 * through the justification of a forcing-chain, let alone apply the technique
 * manually. Maybe 25% can follow a Skyscraper, hence Skyscraper exists.
 * <p>
 * It's around here that we loose most users that downloaded SE to learn how to
 * solve Sudoku puzzles manually; here-after it's hard work, which folks do NOT
 * like; so somewhere around SDP we're down to the hardcore thinkers; probably
 * mostly programmers. Solving these bastards manually can take years. A Korean
 * family have been solving a Sudoku (a really hard one) for three generations.
 * Two members of that family have been recognised as Sudoku masters: seriously
 * ____ing smart bastards; so yeah, most folks tap-out somewhere around here.
 * <p>
 * The package-name SDP is an acronym of SingleDigitPattern, the HoDoKu class
 * from which all hinters in this directory originate.
 *
 * @author Keith Corlett 2020-03-25
 */
public class Skyscraper extends AHinter {

	/**
	 * pairs is an array of arrays-of-two-cells. 18 is large enough by
	 * experimentation. It's a field rather than re-create the array in each
	 * findHints. Note that all cell references are cleared upon exit, because
	 * each cell reference holds the whole Grid in memory (a memory leak).
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
			if ( accu.isSingle() ) // normal short-circuiting or
				result = search(grid.rows) || search(grid.cols);
			else // bitwise-or operator always executes both
				result = search(grid.rows) | search(grid.cols);
		} finally {
			clear(this.pairs); // each cell reference holds the whole grid
			this.grid = null;
			this.accu = null;
		}
		return result;
	}

	/**
	 * Search the grid for skyscraper hints by rows or cols.
	 *
	 * @param regions grid.rows/cols
	 * @return were any hints found?
	 */
	private boolean search(final ARegion[] regions) {
		// a pair is the 2 cells in a row/col which both maybe v
		Cell[] pairA, pairB;
		int p // the number of pairs collected
		  , o // the otherIndex
		  , a,A // pairA index
		  , b; // pairB index
		// work-out the regionType and otherType from regions
		final int rType, oType;
		if ( regions[0] instanceof Row ) {
			rType = ROW;
			oType = COL;
		} else {
			rType = COL;
			oType = ROW;
		}
		// localise field for speed (clear is for debugging only)
		final Cell[][] pairs = this.pairs; // clear(this.pairs); // this.pairs;
		// indices of removable v's, if any
		final Idx victims = this.victims;
		// indices of cells in Grid which maybe value 1..9 (cached)
		final Idx[] candidates = grid.getIdxs();
		// presume that no hint will be found
		boolean result = false;
		// foreach potential value
		for ( int v=1; v<10; ++v ) {
            // get rows/cols with two places for v
			p = 0; // p is the number of pairs-of-v's found
			for ( ARegion r : regions )
				if ( r.indexesOf[v].size == 2 )
					r.at(r.indexesOf[v].bits, pairs[p++]);
			// if there's atleast two pairs-of-v's
			if ( p > 1 ) {
				// examine each distinct pair (A and B) of pairs-of-v's,
				// using a forwards-only search.
				for ( a=0,A=p-1; a<A; ++a ) {
					pairA = pairs[a]; // the first pair of v's
					for ( b=a+1; b<p; ++b ) {
						pairB = pairs[b]; // the second pair of v's
						// if "this" end is aligned: in same col/row
						if ( ( (pairA[0].regions[oType]==pairB[0].regions[oType] && (o=1)==1) // "other" end is 1
							|| (pairA[1].regions[oType]==pairB[1].regions[oType] && (o=0)==0) ) // "other" end is 0
						  // and "other" end is misaligned: NOT in same col/row
						  // otherwise it's an X-Wing, which is not my problem
						  && pairA[o].regions[oType] != pairB[o].regions[oType]
						  // and it eliminates any maybes
						  && victims.setAnd(pairA[o].buds, pairB[o].buds)
								    .and(candidates[v]).any()
						) {
							// FOUND a Skyscraper!
							// build the removable (red) potentials
							final Pots reds = new Pots(v, victims.cells(grid));
							// it was producing hints with no eliminations
							if ( reds.clean() ) {
								result = true;
								if ( accu.add(createHint(reds, v, o, pairA
										, pairB, rType, oType)) )
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
	 * Create and return a new SkyscraperHint.
	 *
	 * @param reds the removable Cell=>Values
	 * @param v the Skyscraper candidate value
	 * @param o the index of the other end (0 or 1)
	 * @param pairA the first pair-of-v's
	 * @param pairB the second pair-of-v's
	 * @param rType the region type ROW/COL
	 * @param oType the other type COL/ROW
	 * @return a new SkyscraperHint
	 */
	private AHint createHint(final Pots reds, int v, int o, Cell[] pairA
			, Cell[] pairB, int rType, int oType) {
		// workout "this" end from the "other" end (backwards)
		final int r = o==0 ? 1 : 0;
		// build the regions
		final List<ARegion> bases = Regions.list(pairA[r].regions[rType]
											   , pairB[r].regions[rType]);
		final List<ARegion> covers = Regions.list(pairA[o].regions[oType]
												, pairB[o].regions[oType]);
		// build the hightlighted (orange) potential values map Cells->Value
		final Pots oranges = new Pots(v, pairA, pairB);
		// build and return the hint
		return new SkyscraperHint(this, v, bases, covers, reds, oranges);
	}

	/**
	 * clear: set every entry in the pairs array of arrays to null.
	 *
	 * @param pairs array to clear
	 */
	private void clear(Cell[][] pairs) {
		for ( Cell[] pair : pairs )
			pair[0] = pair[1] = null;
	}

}
