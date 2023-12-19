/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the TwoStringKite solving technique was boosted from the
 * current release (AFAIK) of hobiwans HoDoKu by Keith Corlett in March 2020.
 * Kudos hobiwans. Mistakes mine.
 *
 * Here is hobiwans standard licence statement:
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
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.Grid.BY9;
import diuf.sudoku.Grid.Col;
import diuf.sudoku.Grid.Row;
import static diuf.sudoku.Grid.pairs;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * TwoStringKite implements the {@link Tech#TwoStringKite} Sudoku solving
 * technique. A TwoStringKite is a box (the kite) that links two biplaced
 * lines (the strings, a row and a col).
 * <p>
 * More completely, a TwoStringKite is a box (the kite) with one end of
 * both a row and a col having two places for v (the two strings), such
 * that no matter where we put v in the box, one of the two other ends of
 * the strings must be v, hence any vs seeing both "free ends" of my two
 * strings are eliminated.
 * <p>
 * A TwoStringKite is a basic Unary Chain, so ChainerUnary finds all TSK
 * hints, among others, but TSK is faster to find this specific pattern,
 * and TSK hints are easier to follow (IMHO), so one could argue that TSK
 * is a simpler hint type, hence SE has TSKs. You can choose to use them,
 * or not.
 * <p>
 * TwoStringKite is used by BruteForce so be VERY careful when ____ing
 * with it! Breaking TSK is a shooting offence! Performance matters here.
 * <p>
 * The package name "sdp" stands for SingleDigitPattern, which is the
 * HoDoKu class that fathered this hinter.
 * <p>
 * Kudos to hobiwan. Mistakes are mine. KRC.
 *
 * @author Keith Corlett 2020-03-25
 */
public class TwoStringKite extends AHinter {

	/**
	 * Swap positions of pair[0] and pair[1] and return true.
	 *
	 * @param pair array of two cells to swap positions of.
	 * @return true to include me in an if-statement predicate
	 */
	private static boolean swap(final Cell[] pair) {
		final Cell tmp = pair[0];
		pair[0] = pair[1];
		pair[1] = tmp;
		return true;
	}

	/**
	 * Constructor.
	 */
	public TwoStringKite() {
		super(Tech.TwoStringKite);
	}

	/**
	 * Search the $grid for TwoStringKiteHints to add to $accu.
	 * <p>
	 * TwoStringKite is used in BruteForce, so performance really matters here,
	 * as does accuracy. Trust the test-cases on this one: if they say you are
	 * broken then believe them!
	 *
	 * @return any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {		//    64,271
		Cell[] rp, cp; // two strings: row/colPair: two places for v in row/col
		int v // the current candidate value
		  , r, nRows // rowPair index, number of rowPairs
		  , c, nCols; // colPair index, number of colPairs
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		final int[] maybes = grid.maybes;
		final Cell[][] rowPairs = new Cell[REGION_SIZE][2];
		final Cell[][] colPairs = new Cell[REGION_SIZE][2];
		boolean result = false;
		// foreach possible value in 1..9
		for ( v=1; v<VALUE_CEILING; ++v )										//   543,177
			// get cell-pairs from rows and cols with two places for v
			if ( (nRows=pairs(rows, v, rowPairs)) > 0							//   442,950
			  && (nCols=pairs(cols, v, colPairs)) > 0 )							//   399,373
				// examine each combination of rowPairs and colPairs
				for ( r=0; r<nRows; ++r )										//   910,877
					for ( rp=rowPairs[r],c=0; c<nCols; ++c )					// 2,430,464
						// we seek a colPair with an end in the same box as the
						// current rowPair, but there are 4 ends, so we first
						// put the ends that are in the same box in 0, and the
						// "free" ends in 1, in both the cp and rp arrays (both
						// arrays are all mine, so are mutable).
						// WARN: It goes tits-up if you cache rp attributes.
						if ( (  rp[0].box==(cp=colPairs[c])[0].box // no swap coz both 0s are already in sameBox
							|| (rp[0].box==cp[1].box && swap(cp)) // swap cps to make 0 the sameBox end
							|| (rp[1].box==cp[0].box && swap(rp)) // swap rps to make 0 the sameBox end
							|| (rp[1].box==cp[1].box && (swap(rp)|swap(cp))) ) // the bitwise-or always swaps both rp and cp, to make both 0s the sameBox ends
							// so from here-down cp[0].box==rp[0].box; hence cp[1] and rp[1] are the "free ends"
						  // and each cell is distinct (no double-ups)
						  && rp[0] != cp[0]										// 1,231,258
						  && rp[0] != cp[1]										//   674,629
						  && rp[1] != cp[0]										//   482,895
						  // found two strong links from a box, so any elims?
						  // we calculate the indice of the victim cell by:
						  //   y = the row of the "free end" of the colPair
						  //   x = the col of the "free end" of the rowPair
						  // and if that cell maybe v, then it is a victim
						  // nb: caching indice is slower, hit-rate too low.
						  && (maybes[BY9[cp[1].y]+rp[1].x] & VSHFT[v]) > 0		//   284,523
						  // FOUND TwoStringKite!
						  && (result=true)										//     8,515
						  && accu.add(new TwoStringKiteHint(grid, this, v
							, new ARegion[] {rp[0].box} // base (the kite)
							, new ARegion[] {rp[1].row, cp[0].col} // covers (strings)
							, new Pots(v, rp[1], cp[1]) // greens (hilight)
							, new Pots(v, rp[0], cp[0]) // blues (hilight)
							, new Pots(BY9[cp[1].y]+rp[1].x, v) // reds (elims)
							, rp.clone() // rowPair
							, cp.clone())) ) // colPair
							return result;
		return result;
	}

}
