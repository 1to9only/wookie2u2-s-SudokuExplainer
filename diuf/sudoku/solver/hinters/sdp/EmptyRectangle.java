/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * The concept of the EmptyRectangle solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020, so below is
 * hobiwans standard licence statement. Kudos should flow back to him. I'm just
 * the monkey at the keyboard who's too thick to follow hobiwans code, so I
 * gave-up and rolled my own from scratch. So this code is (pretty much) all
 * mine. All the mistakes are definately mine.
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
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
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Col;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;

/**
 * EmptyRectangle implements the Empty Rectangle Sudoku solving technique.
 *
 * NOTE: The package name: sdp stands for SingleDigitPatternSolver which is
 * the name of the HoDoKu class which all the hinters in this directory were
 * boosted from.
 *
 * @author Keith Corlett Mar 25
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Diagram: The 9 Empty Rectangle patterns
 * <p>
 * Each box can hold 9 different empty rectangles. 'X' means $v not present',
 * The digit: in the top-left of each box is the index of this ER pattern, and
 * the next 2 digits are the erRow,erCol offsets for this ER pattern.
 * <pre>
 * index:erRow,erCol
 * + 0:2,2 + 1:2,1 + 2:2,0 +
 * | X X . | X . X | . X X |
 * | X X . | X . X | . X X |
 * | . . . | . . . | . . . |
 * + 3:1,2 + 4:1,1 + 5:1,0 +
 * | X X . | X . X | . X X |
 * | . . . | . . . | . . . |
 * | X X . | X . X | . X X |
 * + 6:0,2 + 7:0,1 + 8:0,0 +
 * | . . . | . . . | . . . |
 * | X X . | X . X | . X X |
 * | X X . | X . X | . X X |
 * + - - - + - - - + - - - +
 * </pre>
 * <p>
 * The '.' cells (erRow and erCol) must contain at least 3 candidates, at least
 * one exclusively within the row or col.
 * <p>
 * NOTE: with 2 positions the ER degenerates into an X-Chain. With all 3
 * candidates in one row or col the ER pattern is useless.
 * <p>
 * For fast/easy comparison IdxSets of all possible combinations of empty
 * cells for all blocks are created at startup.
 *
 * @author hobiwan
 */
public class EmptyRectangle extends AHinter {

	/**
	 * EMPTY_BOX_BITS is the box.idxsOf[$v].bits of the cells in each ER box
	 * that must be "empty", ie not maybe $v; all in ARegion.cells indices.
	 * <p>
	 * For each ER pattern {@code erBox.idxsOf[v].bits & EMPTY_BOX_BITS[er]==0}
	 * ie $v is not a maybe in any of the erBox cells that are denoted by a set
	 * (1) bit in EMPTY_BOX_BITS.
	 * <p>
	 * nb: hobiwan's erOffsets are were absolute Grid.cells indices, but what
	 * I want is the indices in region.cells, coz that is what is in existing
	 * idxsOf[$v].bits; so I translated<br>
	 * <p>
	 * Translated hobiwans erOffsets array of 9 boxs * 9 ER patterns, to my
	 * EMPTY_BOX_BITS array of 9 ER patterns (no need for one for each box!)
	 * formatted to bitwise-and with the existing region.idxOf[$v].bits.
	 * <p>
	 * Diagram: ARegion.idxOf[$v].bits indice numbers in a Box:
	 * <pre>
	 * + - - - +
	 * | 0 1 2 |
	 * | 3 4 5 |
	 * | 6 7 8 |
	 * + - - - +
	 * </pre>
	 */
	private static final int[] EMPTY_BOX_BITS = new int[] {
		// bits R-to-L encodes a L-to-R reality (ie everthings back-to-front)
		// diagram        empty         erBoxOffsets erRowOffset,erColOffset
		// row 1           bits         0  1  2  3   R, C
		  Integer.parseInt("000"
						 + "011"
						 + "011", 2) //{0, 1, 3, 4}  2, 2
		, Integer.parseInt("000"
						 + "101"
						 + "101", 2) //{0, 2, 3, 5}  2, 1
		, Integer.parseInt("000"
						 + "110"
						 + "110", 2) //{1, 2, 4, 5}  2, 0
		// row 2
		, Integer.parseInt("011"
						 + "000"
						 + "011", 2) //{0, 1, 6, 7}  1, 2
		, Integer.parseInt("101"
						 + "000"
						 + "101", 2) //{0, 2, 6, 8}  1, 1
		, Integer.parseInt("110"
						 + "000"
						 + "110", 2) //{1, 2, 7, 8}  1, 0
		// row 3
		, Integer.parseInt("011"
						 + "011"
						 + "000", 2) //{3, 4, 6, 7}  0, 2
		, Integer.parseInt("101"
						 + "101"
						 + "000", 2) //{3, 5, 6, 8}  0, 1
		, Integer.parseInt("110"
						 + "110"
						 + "000", 2) //{4, 5, 7, 8} 0, 0
	};

	/** Masks used to detect if all v's in box are in the same row.
	 * Also used to remove erBox from erRow.idxOf[$v], despite my name. */
	private static final int[] ROW_BITS = {
		// 0 = top row
		  Integer.parseInt("000"		 // remember that bits are a right-to-left
						 + "000"	 // representatin of left-to-right reality,
						 + "111", 2) // so you need to mentally invert my rows!
		// 1 = middle row
		, Integer.parseInt("000"
						 + "111"
						 + "000", 2)
		// 2 = bottom row
		, Integer.parseInt("111"
						 + "000"
						 + "000", 2)
	};

	/** Masks used to detect if all v's in box are in the same col.
	 * Also used to remove erBox from erCol.idxOf[$v], despite my name. */
	private static final int[] COL_BITS = {
		// 0 = left col
		  Integer.parseInt("001"		 // remember that bits are a right-to-left
						 + "001"	 // representatin of left-to-right reality,
						 + "001", 2) // so you need to mentally mirror my cols!
		// 1 = middle col
		, Integer.parseInt("010"
						 + "010"
						 + "010", 2)
		// 2 = right col
		, Integer.parseInt("100"
						 + "100"
						 + "100", 2)
	};

	/** The erRow for each box for each ER pattern: [erBox.boxId][eri]. */
	private static final int[][] erRows = new int[9][9];

	/** The erCol for each box for each ER pattern: [erBox.boxId][eri]. */
	private static final int[][] erCols = new int[9][9];

	// initialize erSets, erRows, and erCols
	static {
		// erRow offsets: each erRow relative to row 0 of the erBox.<br>
		// nb: erRow's are full of .'s in the diagrams.
		final int[] erRowOff = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};
		// erCol offsets: each erCol relative to col 0 of the erBox.<br>
		// nb: erCol's are full of .'s in the diagrams.
		final int[] erColOff = new int[]{2, 1, 0, 2, 1, 0, 2, 1, 0};
		// row-col of top-left of each box
		int r=0, c=0;
		// foreach box
		for ( int b=0; b<9; ++b ) {
			erRows[b] = new int[9];
			erCols[b] = new int[9];
			// foreach ER pattern
			for ( int er=0; er<9; ++er ) {
				// translate the relative erRowOff into an absolute
				// Grid indice for this ER pattern in this box.
				erRows[b][er] = erRowOff[er] + r;
				// translate the relative erColOff into an absolute
				// Grid indice for this ER pattern in this box.
				erCols[b][er] = erColOff[er] + c;
			}
			if ( (b % 3) == 2 ) { // move onto the next row of boxs
				r += 3;
				c = 0;
			} else// move on to the next box in this row
				c += 3;
		}
	}

	// =========================== instance stuff ===========================

	public EmptyRectangle() {
		super(Tech.EmptyRectangle);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		final int[] ISHFT = Indexes.SHFT;
		final int[][] INDEXES = Indexes.ARRAYS;
		Box erBox;  Row erRow;  Col erCol;  Cell redCell;
		int card, boxVBits, erR, erC;
		// these are for the first verse (read on McDuff).
		int c2yBits, c2y;
		// these are for the second verse (reverse of the first)
		// I'm using distinct vars coz the names help explain the code.
		// Over-abstraction causes s__t/meaningless/confusing variable names.
		int c2xBits, c2x;
		// presume that no hint will be found
		boolean result = false;
		// foreach box: foreach value: foreach ER pattern
		for ( int b=0; b<9; ++b ) {
			erBox = grid.boxs[b];
			for ( int v=1; v<10; ++v ) {
				// ER patterns needs between 2 and 5 positions for $v, but my
				// minimum is 3 coz I'm not doing boxes which have 1 value in
				// either the erRow or erCol, which was optional in hobiwans.
				if ( (card=erBox.indexesOf[v].size)<3 || card>5 )
					continue;
				boxVBits = erBox.indexesOf[v].bits;
				for ( int er=0; er<9; ++er ) { // the ER index
					// skip if the boxs v's don't intersect the ER pattern
					if ( (boxVBits & EMPTY_BOX_BITS[er]) != 0 )
						continue;
					// if cells in the box which maybe $v are all in erRow
					// or all in erCol then this ER pattern is impossible.
					if ( (boxVBits & ~ROW_BITS[(erR=erRows[b][er])%3]) == 0 )
						continue; // all v's in erRow -> impossibles
					if ( (boxVBits & ~COL_BITS[(erC=erCols[b][er])%3]) == 0 )
						continue; // all v's in erCol -> impossibles
					erRow = grid.rows[erR];
					erCol = grid.cols[erC];

					// First verse: row => col
					//
					// We follow the erRow accross, examining each cell which
					// maybe $v to see if there's two $v's in that column, and
					// if so we'll jump up/down to the otherCell in that column
					// and see if there's a $v at the intersection of his row
					// and the erCol (redCell), and if so it's an:
					// Empty Rectangle: $redCell cannot be $v.
					//
					// Note that hobiwan did the first and second verse in a
					// method, reversing the roles by swapping the params and
					// using a "flip" variable, which I found more confusing
					// than Elle McPherson in red latex y-fronts, so I have
					// de-abstracted it, making it easier to follow. So I'm an
					// idiot but my version took 0.118s where hobiwans took
					// 3.652s for top1465. So yeah, this is a bit faster.
					//
					// foreach cell in the erRow (except erBox) which maybe $v
					//             (ie remove erBox from the erRow.idxsOf[$v])
					// nb: We use ROW_BITS even though erBox isn't a row. The
					//     concept is the same: we remove the three bits that
					//     are the erBox from the erRow.indexesOf[$v].
					for ( Cell c1 : erRow.at(erRow.indexesOf[v].bits
							& ~ROW_BITS[erBox.hNum], false) ) {
						// the col that contains c1 needs 2 positions for $v
						if ( c1.col.indexesOf[v].size != 2 )
							continue;
						// get the row of otherCell in c1's col which maybe v
						// nb: c2 becomes "the otherCell" later on, for now we
						//     need just c2yBits: his row number left-shifted.
						c2yBits = c1.col.indexesOf[v].bits & ~ISHFT[c1.y];
						assert Integer.bitCount(c2yBits) == 1;
						// now if the erCol has $v in c2's row then
						// it's an ER, and that's the redCell.
						if ( (erCol.indexesOf[v].bits & c2yBits) == 0 )
							continue;
						// get the red (removable value) cell
						// and check that it's not in the erBox
//KEEP4DOC: the combined line is ungrocable.
						c2y = INDEXES[c2yBits][0];
						redCell = grid.cells[c2y*9+erC];
						if ( redCell.box == erBox )
							continue; // Oops!
						// Empty Rectangle found! No matter which cell in the
						// erBox turns out to be $v the redCell cannot be $v
						Cell c2 = grid.cells[c2y*9+c1.x];
						// create the hint and add it to accu
						AHint hint = createHint(v, erBox, erRow, erCol
								, c1, c2, boxVBits, redCell, false);
						result = true;
						if ( accu.add(hint) )
							return true;
					}

					// Second verse: col => row
					// We reverse the roles of rows and cols, x and y, etc.
					//
					// We follow the erCol, examining each cell which maybe $v
					// to see if there's two $v's in that row, and if so we
					// jump left/right to the otherCell in that row and see if
					// there's a $v in the intersection of his column and the
					// erRow (redCell), and if so, then it's an:
					// Empty Rectangle: $redCell cannot be $v.
					//
					// foreach cell in ER row (except ER box) which maybe $v
					//           (ie remove erBox from the erCol.idxsOf[$v])
					// nb: We use ROW_BITS even though erBox isn't a row. The
					//     concept is the same: we remove the three bits that
					//     are the erBox from the erCol.idxOf[$v].
					for ( Cell c1 : erCol.at(erCol.indexesOf[v].bits
							& ~ROW_BITS[erBox.vNum], false) ) {
						// the row that contains c1 needs 2 positions for $v
						if ( c1.row.indexesOf[v].size != 2 )
							continue;
						// get the col of otherCell in c1's row which maybe v
						// nb: c2 becomes "the otherCell" later on, for now we
						//     need just his column number left-shifted.
						c2xBits = c1.row.indexesOf[v].bits & ~ISHFT[c1.x];
						assert Integer.bitCount(c2xBits) == 1;
						// now if the erRow has $v in c2's col then
						// it's an ER, and that's the redCell.
						if ( (erRow.indexesOf[v].bits & c2xBits) == 0 )
							continue;
						// get the red (removable value) cell
						// and check that it's not in the erBox
//KEEP4DOC: the combined line is ungrocable.
						c2x = INDEXES[c2xBits][0];
						redCell = grid.cells[erR*9+c2x];
						if ( redCell.box == erBox )
							continue; // Oops!
						// Empty Rectangle found! No matter which cell in the
						// erBox turns out to be $v the redCell cannot be $v
						Cell c2 = grid.cells[c1.y*9+c2x];
						// create the hint and add it to the IAccumulator
						AHint hint = createHint(v, erBox, erRow, erCol
								, c1, c2, boxVBits, redCell, true);
						result = true;
						if ( accu.add(hint) )
							return true;
					}

					// This one's a bit weird. A Box with 3-or-more positions
					// for $v can match only one er-pattern, which we've just
					// done, so we're outta here.
					if ( card > 2 )
						break; // Mike drop!
				}
			}
		}
		return result;
	}

	private AHint createHint(int v, Box erBox, Row erRow, Col erCol
			, Cell c1, Cell c2, int boxVBits, Cell redCell, boolean isCol) {

		// build the regions (evens are blue, odds are green)
		// isCol's are rarer: the roles of row and col are reversed, so
		// we need to swap the "assisting regions" in the static HTML.
		ARegion[] regions = new ARegion[]{
				//blues, greens
				//evens, odds
				  erBox, isCol ? erCol : erRow // #SWAPPED
				, null,  isCol ? erRow : erCol // #SWAPPED
				, null,  c2.col
				, null,  c2.row
			};
		List<ARegion> bases = Regions.list(erBox);
		List<ARegion> covers;
		if ( isCol )
			covers = Regions.list(erCol, erBox, c2.col, c2.row);
		else
			covers = Regions.list(erBox, erCol, c2.col, c2.row);

		// tag is for debugging: it identifies which method was used to find
		// this hint: " A:ROW", or " B:COL" designed to be left blank "" for
		// normal use, and for developers to switch-on when there's bugs.
//		String debugMessage = isCol ? "B:COL" : "A:ROW";	// for debugging
		String debugMessage = ""; // for "normal use"

		// build the hightlighted (orange) potential values map
		Pots orangePots = new Pots(v, c1, c2);
		// build the "fins" (blue) potential values map Cell->Values
		Pots bluePots = new Pots(v, erBox.atNew(boxVBits));
		// build the removable (red) potential values map Cell->Values
		Pots redPots = new Pots(v, redCell);
		// build and return the hint
		return new EmptyRectangleHint(this, v, bases, covers
				, orangePots, bluePots, redPots, debugMessage);
	}

}
