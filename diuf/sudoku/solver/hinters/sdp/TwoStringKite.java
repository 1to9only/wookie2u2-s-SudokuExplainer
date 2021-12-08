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
import diuf.sudoku.Grid.Col;
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Grid.Row;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * TwoStringKite implements the TwoStringKite Sudoku solving technique.
 * <p>
 * A TwoStringKite is a box (the kite) which is the intersections of both a row
 * and a col with two places for v (the two strings), such that no matter where
 * we put v in the box, one of the two "other ends" of the strings must be v,
 * therefore any v's that see (are in the same box, row, or col as) both "other
 * ends" of the two strings can be eliminated.
 * <p>
 * A TwoStringKite (TSK) is a simplified faster Unary (single digit) Chain; so
 * SE's UnaryChainer finds all TSK hints (among others), but TSK is faster to
 * find this specific pattern, and the explanation of a TSK hint is, IMHO, a
 * bit easier to follow, so one might argue that TSK is a "simpler" hint-type;
 * hence SE now includes the TSK hinter, which you can choose to use, or not.
 * <p>
 * The package name 'sdp' stands for SingleDigitPattern, which is the HoDoKu
 * class that fathered these hinters.
 *
 * @author Keith Corlett Mar 25
 */
public class TwoStringKite extends AHinter {

	/** N is just local shorthand for Grid.REGION_SIZE: 9. */
	private static final int N = REGION_SIZE;

	/**
	 * Add each region with two places for v to the pairs array,
	 * and return how many.
	 *
	 * @param regions to find pairs in: grid.rows or grid.cols
	 * @param v the candidate value
	 * @param pairs an array-of-pairs to add to
	 * @return the number of pairs found
	 */
	private static int pairs(final ARegion[] regions, final int v
			, final Cell[][] pairs) {
		int i = 0;
		for ( ARegion r : regions ) {
			if ( r.ridx[v].size == 2 ) {
				r.at(r.ridx[v].bits, pairs[i++]);
			}
		}
		return i;
	}

	/**
	 * Swap positions of pair[0] and pair[1] and return true.
	 *
	 * @param pair array of two cells to swap positions of.
	 * @return true
	 */
	private static boolean swap(final Cell[] pair) {
		final Cell tmp = pair[0];
		pair[0] = pair[1];
		pair[1] = tmp;
		return true;
	}

	private final Cell[][] rowPairs = new Cell[N][2];
	private final Cell[][] colPairs = new Cell[N][2];

	public TwoStringKite() {
		super(Tech.TwoStringKite);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		Cell[] rp // rowPair: a pair of cells in a row
			 , cp; // colPair: a pair of cells in a col
		int i // the ubiqitous index
		  , v // the current candidate value
		  , sv // shiftedValue: the bitset representation of v: 1<<(v-1)
		  , r, nRowPairs // rowPair index, number of rowPairs
		  , c, nColPairs; // colPair index, number of colPairs
		// localise fields for speed. I don't deeply understand this, but it
		// looks like each heap-reference is slower than each stack-reference,
		// so just putting heap-stuff on the stack saves time, especially in an
		// "all in one" method like this where each var is used many times.
		// The cut-off seems to be 3: creating a stack-reference takes about as
		// long as 2.5 heap-references, so the stack is ahead as long as it's
		// referenced 3-or-more times, including (especially) in loops.
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		final Cell[] cells = grid.cells;
		final int[] maybes = grid.maybes;
		final Cell[][] rowPairs = this.rowPairs;
		final Cell[][] colPairs = this.colPairs;
		// presume that no hint will be found
		boolean result = false;
		// foreach possible value in 1..9
		for ( v=1; v<VALUE_CEILING; ++v ) {
			sv = VSHFT[v]; // lookup once to save some time, I hope
			// get cell-pairs from rows and cols with two places for v
			if ( (nRowPairs=pairs(rows, v, rowPairs)) != 0
			  && (nColPairs=pairs(cols, v, colPairs)) != 0 ) {
				// examine each combination of rowPairs and colPairs
				for ( r=0; r<nRowPairs; ++r ) {
					rp = rowPairs[r];
					for ( c=0; c<nColPairs; ++c ) {
						cp = colPairs[c];
						// we seek a rowPair and a colPair having ends in the
						// same box, but that's 4 possible end-combos, so first
						// put ends in the same box in ...Pair[0], so the "free
						// ends" in go in ...Pair[1].
						if ( (  rp[0].box==cp[0].box // no swaps
							|| (rp[0].box==cp[1].box && swap(cp))
							|| (rp[1].box==cp[0].box && swap(rp))
							|| (rp[1].box==cp[1].box && (swap(rp)|swap(cp))) )
						  // and there must be no double-ups
						  && rp[0] != cp[0]
						  && rp[0] != cp[1]
						  && rp[1] != cp[0]
						  && rp[1] != cp[1]
						  // found two strong links from a box, so any elims?
						  // get the y=row of the "free end" of the colPair
						  // and the x=col of the "free end" of the rowPair
						  // and if the cell at there intersection maybe v
						  && (maybes[i=cp[1].y*N + rp[1].x] & sv) != 0
						) {
							// FOUND TwoStringKite!
							final AHint hint = createHint(rp, cp, cells[i], v);
							result |= hint!=null;
							if ( accu.add(hint) ) {
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
	 * Create and return a new TwoStringKiteHint.
	 *
	 * @param rowPair the pair of cells in a row
	 * @param colPair the pair of cells in a col
	 * @param victim the cell to eliminate from
	 * @param v the candidate value
	 * @return a new TwoStringKiteHint
	 */
	private AHint createHint(final Cell[] rowPair, final Cell[] colPair
			, final Cell victim, final int v) {
		// build a list of bases (the box), and a list of covers (row and col)
		final ARegion[] bases = Regions.array(rowPair[0].box);
		final ARegion[] covers = Regions.array(rowPair[1].row, colPair[0].col);
		// build the hightlighted (green) Cell->Values
		final Pots greens = new Pots(v, rowPair[1], colPair[1]);
		// build the "fins" (blue) Cell->Values
		final Pots blues = new Pots(v, rowPair[0], colPair[0]);
		// build the removeable (red) Cell->Values
		final Pots reds = new Pots(victim, v);
		// build and return the hint
		return new TwoStringKiteHint(this, v, bases, covers, greens, blues
				, reds, rowPair.clone(), colPair.clone());
	}

}
