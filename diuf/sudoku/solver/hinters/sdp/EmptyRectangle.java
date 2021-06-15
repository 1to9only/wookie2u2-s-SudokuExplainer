/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The concept of the EmptyRectangle solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020. Kudos to
 * hobiwan. I'm just the monkey at the keyboard who's too thick to follow the
 * code, so I gave-up and rolled my own from scratch; so this code is (pretty
 * much) all mine. Mistakes are most definately mine.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
import static diuf.sudoku.Indexes.FIRST_INDEX;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.List;

/**
 * EmptyRectangle implements the Empty Rectangle Sudoku solving technique.
 * <p>
 * An Empty Rectangle is when all cells which maybe v in a box are in one row
 * and one col in a box; ie in each box all v's appear in one row or one col;
 * so either the row or the col must contain v. If there's another row or col
 * with two places for v then v can be eliminated from the cell in this box's
 * col-or-row at the far end of the other row-or-col. Hard to explain, sorry.
 * <p>
 * The package name SDP stands for SingleDigitPattern which is the name of the
 * HoDoKu class that fathered all the hinters in this directory. All SDP's are
 * found by the UnaryChainer, but the SDP's are simpler and therefore faster;
 * and produce hints that more than 1% of users can follow.
 *
 * @author Keith Corlett 2020-03-25
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Diagram: The 9 Empty Rectangle patterns
 * <p>
 * Each box can have one of nine different empty rectangles. In below diagram:
 * 'X' means v is NOT present; a period '.' means that v must be present in
 * both the row and the col (more on this below).
 * The digit in the top-left of each box is the index of this ER pattern, and
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
 * The '.' cells (in erRow and erCol) must contain at least 3 v's, at least one
 * of which must be exclusively in the row or col (the cell at erRow erCol does
 * not count).
 * <p>
 * With 2 positions the ER degenerates into an X-Chain. With all 3 v's in one
 * row or col the ER pattern is useless.
 * <p>
 * For fast/easy comparison we use bitset-indexes of all possible combinations
 * of the required empty cells in each box, created ONCE at startup.
 * <p>
 * Note that this code uses the continue statement, which is slow; but it's
 * uglier than a hatfull of assholes without it, so I've chosen to retain it
 * because it's "fast enough" as is. If you want this code to really zing then
 * performance test; eradicate continue; and test again. I reckon it'll be at
 * least 10% faster (possibly 50%); but it's already "fast enough".
 *
 * @author hobiwan
 */
public class EmptyRectangle extends AHinter {

	/**
	 * EMPTY_BOX_BITS is the box.indexesOf[$v].bits (box.cells indices) which
	 * must NOT maybe $v in order for this box to form an ER.
	 * <p>
	 * Foreach ER {@code erBox.indexesOf[v].bits & EMPTY_BOX_BITS[er]==0} ie
	 * $v is not a maybe in any of the erBox cells that are denoted by a set
	 * (1) bit in EMPTY_BOX_BITS.
	 * <p>
	 * nb: hobiwan's erOffsets were absolute Grid.cells indices, but what I
	 * want is the indices in region.cells, coz that is what is in existing
	 * indexesOf[$v].bits; so I translated them over.<br>
	 * <p>
	 * Translated hobiwans erOffsets array of 9 boxs * 9 ER patterns, to my
	 * EMPTY_BOX_BITS array of 9 ER patterns (no need for one for each box!)
	 * formatted to bitwise-and with the existing region.indexesOf[$v].bits.
	 * <p>
	 * Diagram: ARegion.indexesOf[$v].bits indice numbers in a Box:
	 * <pre>
	 * + - - - +
	 * | 0 1 2 |
	 * | 3 4 5 |
	 * | 6 7 8 |
	 * + - - - +
	 * </pre>
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality (ie
	 * everything is back-to-front; so you must mentally mirror my bits for
	 * it to make any sense. Sorry about that, but I see no alternative.
	 */
	private static final int[] EMPTY_BOX_BITS = new int[] {
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

	/**
	 * Masks used to detect if all v's in box are in the same row.
	 * Also used to remove erBox from erRow.idxOf[$v], despite my name.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally invert my rows for it to make sense.
	 */
	private static final int[] ROW_BITS = {
		// 0 = top row
		  Integer.parseInt("000"
						 + "000"
						 + "111", 2)
		// 1 = middle row
		, Integer.parseInt("000"
						 + "111"
						 + "000", 2)
		// 2 = bottom row
		, Integer.parseInt("111"
						 + "000"
						 + "000", 2)
	};

	/**
	 * Masks used to detect if all v's in box are in the same col.
	 * Also used to remove erBox from erCol.idxOf[$v], despite my name.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally mirror my bits for it to make sense.
	 */
	private static final int[] COL_BITS = {
		// 0 = left col
		  Integer.parseInt("001"
						 + "001"
						 + "001", 2)
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
		Box box;
		Row row;
		Col col;
		Cell redCell, c2;
		AHint hint;
		int b, v, er, card, boxVs, erR, erC;
		// these are for the first verse (read on McDuff).
		int c2yBits, c2y;
		// these are for the second verse (an inversion of the first)
		// I'm using distinct vars coz the names help explain the code.
		// Over-abstraction tends to cause meaningless variable names.
		int c2xBits, c2x;
		// presume that no hint will be found
		boolean result = false;
		// foreach box: foreach value: foreach ER pattern
		for ( b=0; b<9; ++b ) {
			box = grid.boxs[b];
			for ( v=1; v<10; ++v ) {
				// ER patterns needs between 2 and 5 positions for $v, but my
				// minimum is 3 coz I'm not doing boxes which have 1 value in
				// either the erRow or erCol, which was optional in hobiwans.
				if ( (card=box.indexesOf[v].size)>2 && card<6 ) {
					boxVs = box.indexesOf[v].bits;
					for ( er=0; er<9; ++er ) { // the ER pattern index
						// if this boxs v's match the ER pattern.
						// nb: EMPTY_BOX_BITS contains the cells that must not
						// contain the given value in order to form an ER.
						if ( (boxVs & EMPTY_BOX_BITS[er]) == 0
						  // and box.v's are NOT all in (erRow or erCol)
						  && (boxVs & ~ROW_BITS[(erR=erRows[b][er])%3]) != 0
						  && (boxVs & ~COL_BITS[(erC=erCols[b][er])%3]) != 0 ) {
							row = grid.rows[erR];
							col = grid.cols[erC];

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
							// 3.652s for top1465. So this is a bit faster. Computers
							// are utter idiots (they just follow instructions), so
							// programmers learn to think clearly, like idiots.
							//
							// foreach cell in the erRow (except erBox) which maybe $v
							//             (ie remove erBox from the erRow.idxsOf[$v])
							// nb: We use ROW_BITS even though erBox isn't a row. The
							//     concept is the same: we remove the three bits that
							//     are the erBox from the erRow.indexesOf[$v].
							for ( Cell c1 : row.at(row.indexesOf[v].bits
									& ~ROW_BITS[box.hNum], false) ) {
								// if the col containing c1 has two v's
								if ( c1.col.indexesOf[v].size == 2 ) {
									// get the row of otherCell in c1's col which maybe v
									// nb: c2 becomes "the otherCell" later on, for now we
									//     need just c2yBits: his row number left-shifted.
									c2yBits = c1.col.indexesOf[v].bits & ~ISHFT[c1.y];
									assert Integer.bitCount(c2yBits) == 1;
									// now if the erCol has $v in c2's row then
									// it's an ER, and that's the redCell.
									if ( (col.indexesOf[v].bits & c2yBits) != 0 ) {
										// get the red (removable value) cell
										// and check that it's not in the erBox
//KEEP4DOC: the combined line is ungrockable.
										c2y = FIRST_INDEX[c2yBits];
										redCell = grid.cells[c2y*9+erC];
										if ( redCell.box != box ) {
											// FOUND Empty Rectangle!
											c2 = grid.cells[c2y*9+c1.x];
											// create the hint and add it to accu
											hint = createHint(v, box, row, col
													, c1, c2, boxVs, redCell, false);
											result = true;
											if ( accu.add(hint) )
												return result;
										}
									}
								}
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
							for ( Cell c1 : col.at(col.indexesOf[v].bits
									& ~ROW_BITS[box.vNum], false) ) {
								// if row containing c1 has 2 v's
								if ( c1.row.indexesOf[v].size == 2 ) {
									// get the col of otherCell in c1's row which maybe v
									// nb: c2 becomes "the otherCell" later on, for now we
									//     need just his column number left-shifted.
									c2xBits = c1.row.indexesOf[v].bits & ~ISHFT[c1.x];
									assert Integer.bitCount(c2xBits) == 1;
									// now if the erRow has $v in c2's col then
									// it's an ER, and that's the redCell.
									if ( (row.indexesOf[v].bits & c2xBits) != 0 ) {
										// get the red (removable value) cell
										// and check that it's not in the erBox
//KEEP4DOC: the combined line is ungrockable.
										c2x = FIRST_INDEX[c2xBits];
										redCell = grid.cells[erR*9+c2x];
										if ( redCell.box != box ) {
											// FOUND Empty Rectangle!
											c2 = grid.cells[c1.y*9+c2x];
											// create the hint and add it to the IAccumulator
											hint = createHint(v, box, row, col
													, c1, c2, boxVs, redCell, true);
											result = true;
											if ( accu.add(hint) )
												return result;
										}
									}
								}
							}

							// weird: a Box with 3-or-more v's can match only
							// one er-pattern, which we've just done, so break
							if ( card > 2 )
								break; // Mike drop!
						}
					} // next er pattern
				}
			} // next v
		} // next box
		return result;
	}

	private AHint createHint(final int v, final Box erBox, final Row erRow
			, final Col erCol, final Cell c1, final Cell c2, final int boxVBits
			, final Cell redCell, final boolean isCol) {
		// build the regions: blue bases, and green covers.
		final List<ARegion> bases = Regions.list(erBox);
		final List<ARegion> covers;
		if ( isCol )
			covers = Regions.list(erCol, erBox, erRow, c2.col, c2.row);
		else
			covers = Regions.list(erRow, erBox, erCol, c2.col, c2.row);
		// for debugging to identify which method found this hint.
//		final String tag = isCol ? "COL" : "ROW";
		final String tag = "";
		// build the hightlighted (orange) potential values map
		final Pots oranges = new Pots(v, c1, c2);
		// build the "fins" (blue) potential values map Cell->Values
		final Pots blues = new Pots(v, erBox.atNew(boxVBits));
		// build the removable (red) potential values map Cell->Values
		final Pots reds = new Pots(v, redCell);
		// build and return the hint
		return new EmptyRectangleHint(this, v, bases, covers, oranges, blues
				, reds, tag);
	}

}
