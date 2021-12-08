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
 * much) mine. Mistakes are most definately mine. But the big ideas are still
 * hobiwans, by rights.
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
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Grid.Col;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * EmptyRectangle implements the Empty Rectangle Sudoku solving technique.
 * <p>
 * An EmptyRectangle can occur when all cells which maybe v in a box are all in
 * one row or one col, so that either the row or the col must contain v. If
 * either of those row/col has a cross-region (col/row) with two places for v
 * then we can eliminate v from the other (non-box) end of the other col/row in
 * the box.
 * <p>
 * The logic is that the box must contain a v, which is on either the row or
 * the col, so if it's in the row/col which intersects the bi-place region then
 * the other place in that region must be v, so the victim (the intersection of
 * "the other place"s cross-region and the box's other col/row) cannot be v;
 * and if the v in the box happens to be in other col/row then the victim still
 * cannot be v, hence the victim cannot be v.
 * <p>
 * Sorry for the poor explanation. I'm doing my best. Use log-view in the GUI
 * to view an EmptyRectangle hint; the hint explanation is vastly superior to
 * this verbose, over-abstracted pile of festering fuss.
 * <p>
 * The package name SDP stands for SingleDigitPattern which is the name of the
 * HoDoKu class that fathered all the hinters in this directory. All SDP's are
 * found by the UnaryChainer, but the SDP's are simpler and therefore faster;
 * and produce hints that more than 1% of users can follow.
 * <p>
 * NB: All empty rectangles are simple region forcing chains, which finds many
 * others besides, but this specific pattern is arguably a "simpler technique"
 * which is faster than forcing chains, so I'm keeping it.
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
 * Note that this code avoids using the continue statement, coz it's slow.
 *
 * @author hobiwan
 */
public class EmptyRectangle extends AHinter {

	/**
	 * There's a problem referencing Grid.SQRT (I suspect infinite referential
	 * loop) so I create a local constant that just happens to have the same
	 * value. sigh.
	 */
	private static final int R = (int)Math.sqrt(Grid.REGION_SIZE); // 3

	/**
	 * EMPTY_BOXS is the box.ridx[$v].bits (box.cells indexes) which must NOT
	 * maybe $v in order for this box to form an ER. Note that not!
	 * <p>
	 * Foreach er 0..8: {@code erBox.ridx[v].bits & EMPTY_BOXS[er]==0},
	 * ie $v is not a maybe in any of the erBox cells that are denoted by a set
	 * (1) bit in EMPTY_BOXS.
	 * <p>
	 * nb: hobiwan's erOffsets were absolute Grid.cells indices, but I'm using
	 * the indices in region.cells coz that's what I've got in ridx[$v].bits;
	 * so I translated them over. That is, translated hobiwans erOffsets array
	 * of 9 boxs times 9 ER patterns, to EMPTY_BOXS array of 9 ER patterns
	 * (no need for one for each box), formatted to bitwise-and with existing
	 * region.ridx[$v].bits.
	 * <p>
	 * Diagram: ARegion.ridx[$v].bits indice numbers in a Box:
	 * <pre>
	 * + - - - +
	 * | 0 1 2 |
	 * | 3 4 5 |
	 * | 6 7 8 |
	 * + - - - +
	 * </pre>
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality, ie
	 * everything is mirrored; so you must mentally mirror it back to make it
	 * make sense. Sorry about that, but I see no alternative. Computers are
	 * like wiping your ass with wet-and-dry. I'll swap you four 320's for a
	 * 1200. Look mate, there's no global warming here. There's can't be or I'm
	 * a complete moron. Sigh. This is a 320 solution. Just grin and bear it.
	 */
	private static final int[] EMPTY_BOXS = new int[] {
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
	 * Bitsets used to detect if all v's in a box are in the same row.
	 * Also used to remove erBox from erRow.ridx[$v], despite my name.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally invert my rows for it to make sense.
	 */
	private static final int[] ROWS = {
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
	 * Bitsets used to detect if all v's in a box are in the same col.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally mirror my bits for it to make sense.
	 */
	private static final int[] COLS = {
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
	private static final int[][] erRows = new int[REGION_SIZE][REGION_SIZE];

	/** The erCol for each box for each ER pattern: [erBox.boxId][eri]. */
	private static final int[][] erCols = new int[REGION_SIZE][REGION_SIZE];

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
		// foreach box: create contigious sub-arrays, for speed
		for ( int b=0; b<REGION_SIZE; ++b )
			erRows[b] = new int[REGION_SIZE];
		// foreach box: create contigious sub-arrays, for speed
		for ( int b=0; b<REGION_SIZE; ++b )
			erCols[b] = new int[REGION_SIZE];
		// foreach box
		for ( int b=0; b<REGION_SIZE; ++b ) {
			// foreach ER pattern
			for ( int er=0; er<REGION_SIZE; ++er ) {
				// translate the relative erRowOff into an absolute
				// Grid indice for this ER pattern in this box.
				erRows[b][er] = erRowOff[er] + r;
				// translate the relative erColOff into an absolute
				// Grid indice for this ER pattern in this box.
				erCols[b][er] = erColOff[er] + c;
			}
			if ( (b % R) == 2 ) { // move onto the next row of boxs
				r += R;
				c = 0;
			} else// move on to the next box in this row
				c += R;
		}
	}

	// =========================== instance stuff ===========================

	public EmptyRectangle() {
		super(Tech.EmptyRectangle);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// ANSI-C style variables mean no stack-work inside this method.
		// Iterating arrays this way is painful, but it's worth it for speed.
		Box box;
		Row row;
		Col col;
		Cell victim, c1, c2;
		Cell[] cs; // the cell array returned by row/col.at
		int b, v, card, er, boxVs, erR, erC, i, n;
		// for the first verse (read on McDuff).
		// WTF: c2y/c2x both contain the bitset of the row/col index, and later
		// the row/col index itself (decoded from the bitset).
		int c2y;
		// for the second verse (an inversion of the first).
		// I'm using distinct vars just coz the names help explain the code.
		int c2x;
		// localise the grid fields for speed.
		final Box[] boxs = grid.boxs;
		final Row[] rows = grid.rows;
		final Col[] cols = grid.cols;
		final Cell[] cells = grid.cells;
		// presume that no hint will be found
		boolean result = false;
		// foreach box: foreach value: foreach ER pattern
		for ( b=0; b<REGION_SIZE; ++b ) {
			box = boxs[b];
			for ( v=1; v<VALUE_CEILING; ++v ) {
				// ER patterns need between 2 and 5 places for v, but my min is
				// 3 coz I'm not doing "the 1*1 cases": boxes having 1 value in
				// either the erRow or erCol; which was optional in hobiwans.
				if ( (card=box.ridx[v].size)>2 && card<6 ) {
					boxVs = box.ridx[v].bits;
					for ( er=0; er<REGION_SIZE; ++er ) { // ER pattern index
						// if this boxs v's match this ER pattern, ie the box
						// does not have any v's in places where it shouldn't.
						if ( (boxVs & EMPTY_BOXS[er]) == 0
						  // and box.v's are NOT all in (erRow or erCol)
						  && (boxVs & ~ROWS[(erR=erRows[b][er])%R]) != 0
						  && (boxVs & ~COLS[(erC=erCols[b][er])%R]) != 0
						) {
							row = rows[erR];
							col = cols[erC];

							// First verse: row => col
							//
							// We follow the erRow accross, examining each cell
							// which maybe v to see if there's two v's in that
							// col, and if so, jump up/down to the otherCell in
							// dat col to see if there's a v at da intersection
							// of his row and the erCol (the victim), and if so
							// it's an Empty Rectangle: $victim cannot be v.
							//
							// hobiwan did first and second verse in a method,
							// reversing the roles by swapping params and using
							// a "flip" variable, which I find uber-confusing,
							// so I've deabstracted it, making it easier to
							// follow. I'm an idiot, but this took 0.064s for
							// top1465, where hobiwan took 3.652s, so this is a
							// little bit faster.
							//
							// Computers are idiots that follow instructions so
							// programming is thinking clearly and efficiently,
							// like an idiot; and sometimes that means NOT the
							// simplest (the most abstract) possible solution.
							//
							// Note that hobiwans implementation covered da 1*1
							// cases which this, by choice, does not. If you
							// need/want the 1*1 cases then just use HoDoKu! I
							// am defensive about this choice, BECAUSE it's
							// indefensible, and I'm a lazy bastard.
							//
							// foreach c in erRow (except erBox) that maybe v
							// (ie remove erBox from the erRow.idxsOf[v])
							// nb: We use ROWS even though erBox isn't a row.
							// The concept is da same: we remove the three bits
							// that are the erBox from the erRow.ridx[v].
							cs = row.at(row.ridx[v].bits & ~ROWS[box.hNum], F);
							for ( i=0,n=cs.length; i<n; ++i ) {
								// if the col containing c1 has two v's
								if ( cs[i].col.ridx[v].size == 2 ) {
									// get the row of otherCell in c1's col which maybe v
									// nb: c2 becomes "the otherCell" later on, for now we
									//     need just his row number (y) as a bitset.
									c2y = (c1=cs[i]).col.ridx[v].bits & ~ISHFT[c1.y];
									assert Integer.bitCount(c2y) == 1;
									// now if the erCol has $v in c2's row then
									// it's an ER, and that's the victim.
									if ( (col.ridx[v].bits & c2y) != 0 ) {
										// get the victim cell
//KEEP4DOC: the combined line is ungrockable.
										c2y = IFIRST[c2y];
										victim = cells[c2y*REGION_SIZE + erC];
										// if he's NOT in the erBox
										if ( victim.box != box ) {
											// FOUND Empty Rectangle!
											c2 = cells[c2y*REGION_SIZE + c1.x];
											// create the hint and add to accu
											final AHint hint = createHint(v
												, box, row, col, c1, c2, boxVs
												, victim, false);
											result = true;
											if ( accu.add(hint) )
												return result;
										}
									}
								}
							}

							// Second verse: col => row
							// We reverse the roles of rows/cols, x/y, etc.
							//
							// Follow the erCol, examining each cell that maybe
							// v to see if there's two v's in the row, and if
							// so jump left/right to the otherCell in that row
							// to see if there's a v in the intersection of his
							// col and da erRow (the victim), and if so, then
							// it's an Empty Rectangle: victim cannot be v.
							//
							// foreach c in ER row (except ER box) that maybe v
							// (ie remove erBox from the erCol.idxsOf[$v])
							// nb: We use ROWS even though erBox isn't a row.
							// The concept is the same: we remove the 3 bits
							// that are the erBox from the erCol.ridx[$v].
							cs = col.at(col.ridx[v].bits & ~ROWS[box.vNum], F);
							for ( i=0,n=cs.length; i<n; ++i ) {
								// if the row containing c1 has 2 v's
								if ( cs[i].row.ridx[v].size == 2 ) {
									// get da col of otherCell in c1s row which
									// maybe v. nb: c2 becomes "the otherCell"
									// later, for now we need just his col (x)
									// as a bitset.
									c2x = (c1=cs[i]).row.ridx[v].bits & ~ISHFT[c1.x];
									assert Integer.bitCount(c2x) == 1;
									// now if the erRow has $v in c2's col then
									// it's an ER, and that's the victim.
									if ( (row.ridx[v].bits & c2x) != 0 ) {
										// get the red (removable value) cell
										// and check that it's not in the erBox
//KEEP4DOC: the combined line is ungrockable.
										c2x = IFIRST[c2x];
										victim = cells[erR*REGION_SIZE + c2x];
										if ( victim.box != box ) {
											// FOUND Empty Rectangle!
											c2 = cells[c1.y*REGION_SIZE + c2x];
											// create the hint and add to accu
											final AHint hint = createHint(v
												, box, row, col, c1, c2, boxVs
												, victim, true);
											result = true;
											if ( accu.add(hint) )
												return result;
										}
									}
								}
							} // next
						} // fi ER pattern
						// weird: a Box with 3-or-more vs can match only one ER
						// pattern, which we've just done, so break, for speed.
						// Don't believe me: batch, comment out, batch; to see
						// that number-of-ER's doesn't increase, and total-ER
						// time should increase just a smidge.
						if ( card > 2 ) {
							break; // Mike drop!
						}
					} // next er pattern
				} // if 3..6 v's in box
			} // next v
		} // next box
		return result;
	}

	private AHint createHint(final int v, final Box erBox, final Row erRow
			, final Col erCol, final Cell c1, final Cell c2, final int boxVBits
			, final Cell victim, final boolean isCol) {
		// build the regions: blue bases, and green covers.
		final ARegion[] bases = Regions.array(erBox);
		final ARegion[] covers;
		if ( isCol )
			covers = Regions.array(erCol, erBox, erRow, c2.col, c2.row);
		else
			covers = Regions.array(erRow, erBox, erCol, c2.col, c2.row);
		// build the hightlighted (green) potential values map
		final Pots greens = new Pots(v, c1, c2);
		// build the "fins" (blue) potential values map Cell->Values
		final Pots blues = new Pots(v, erBox.atNew(boxVBits));
		// build the removable (red) potential values map Cell->Values
		final Pots reds = new Pots(victim, v);
		// build and return the hint
		return new EmptyRectangleHint(this, v, bases, covers, greens, blues
				, reds, "");
	}

}
