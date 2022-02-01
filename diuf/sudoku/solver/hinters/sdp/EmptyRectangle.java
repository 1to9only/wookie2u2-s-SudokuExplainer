/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.solver.hinters.Slots.*;
import static java.lang.Integer.bitCount;
import static java.lang.Integer.parseInt;
import static diuf.sudoku.Grid.BY9;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Values.VSHFT;

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
 * and produce hints that users can follow.
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
 * <pre>
 * KRC 2021-12-28 I tried to reinstitute hobiwans "search" method using lambda
 * expressions to handle the role reversal of rows/cols and x/y, but I couldn't
 * workout how to get the victim cell. The problem is theres no way to BY9[row]
 * when row and col are indeterminate coz they're swapping roles.
 * </pre>
 * @author hobiwan
 */
public class EmptyRectangle extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS_A="+java.util.Arrays.toString(COUNTS_A)+" COUNTS_B="+java.util.Arrays.toString(COUNTS_B));
//	}
//	private static final long[] COUNTS_A = new long[6], COUNTS_B = new long[6];

	/** Static init error referencing Grid.SQRT, so just recalculate it. */
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
	private static final int[] EMPTY_BOXS = {
		// diagram        empty         erBoxOffsets erRowOffset,erColOffset
		// row 1           bits         0  1  2  3   R, C
		  parseInt("000"
				 + "011"
				 + "011", 2) //{0, 1, 3, 4}  2, 2
		, parseInt("000"
				 + "101"
				 + "101", 2) //{0, 2, 3, 5}  2, 1
		, parseInt("000"
				 + "110"
				 + "110", 2) //{1, 2, 4, 5}  2, 0
		// row 2
		, parseInt("011"
				 + "000"
				 + "011", 2) //{0, 1, 6, 7}  1, 2
		, parseInt("101"
				 + "000"
				 + "101", 2) //{0, 2, 6, 8}  1, 1
		, parseInt("110"
				 + "000"
				 + "110", 2) //{1, 2, 7, 8}  1, 0
		// row 3
		, parseInt("011"
				 + "011"
				 + "000", 2) //{3, 4, 6, 7}  0, 2
		, parseInt("101"
				 + "101"
				 + "000", 2) //{3, 5, 6, 8}  0, 1
		, parseInt("110"
				 + "110"
				 + "000", 2) //{4, 5, 7, 8} 0, 0
	};

	/**
	 * Masks used to detect if all v's in a box are in the same row.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally invert my rows for it to make sense.
	 */
	private static final int[] ROWS = {SLOT1, SLOT2, SLOT3};

	/**
	 * Masks used to detect if all v's in a box are in the same col.
	 * <p>
	 * This is a right-to-left representation of a left-to-right reality,
	 * so you need to mentally mirror my bits for it to make sense.
	 */
	private static final int[] COLS = {SLOT4, SLOT5, SLOT6};

	/** The erRow for each box for each ER pattern: [erBox.boxId][eri]. */
	private static final int[][] ER_ROWS = new int[REGION_SIZE][REGION_SIZE];

	/** The erCol for each box for each ER pattern: [erBox.boxId][eri]. */
	private static final int[][] ER_COLS = new int[REGION_SIZE][REGION_SIZE];

	// initialize erSets, erRows, and erCols
	static {
		// erRow offsets: each erRow relative to row 0 of the erBox.<br>
		// nb: erRow's are full of .'s in the diagrams.
		final int[] erRowOffset = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};
		// erCol offsets: each erCol relative to col 0 of the erBox.<br>
		// nb: erCol's are full of .'s in the diagrams.
		final int[] erColOffset = new int[]{2, 1, 0, 2, 1, 0, 2, 1, 0};
		// row-col of top-left of each box
		int r=0, c=0;
		// foreach box: create contigious sub-arrays, for speed
		for ( int bi=0; bi<REGION_SIZE; ++bi )
			ER_ROWS[bi] = new int[REGION_SIZE];
		// foreach box: create contigious sub-arrays, for speed
		for ( int bi=0; bi<REGION_SIZE; ++bi )
			ER_COLS[bi] = new int[REGION_SIZE];
		// foreach box
		for ( int bi=0; bi<REGION_SIZE; ++bi ) {
			// foreach ER pattern
			for ( int er=0; er<REGION_SIZE; ++er ) {
				// translate the relative erRowOffset into an absolute
				// Grid indice for this ER pattern in this box.
				ER_ROWS[bi][er] = erRowOffset[er] + r;
				// translate the relative erColOffset into an absolute
				// Grid indice for this ER pattern in this box.
				ER_COLS[bi][er] = erColOffset[er] + c;
			}
			if ( (bi % R) == 2 ) { // move onto the next row of boxs
				r += R;
				c = 0;
			} else // move on to the next box in this row
				c += R;
		}
	}

	// =========================== instance stuff ===========================

	public EmptyRectangle() {
		super(Tech.EmptyRectangle);
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// all ANSI-C style vars means ONE stackframe, unless we hint.
		// This makes iterating arrays a pain in the ass. Get Over It!
		Cell[] rCells; // region (erRow or erCol) cells
		Box erBox; // emptyRectangleBox
		Row erRow; // emptyRectangleRow
		Col erCol; // emptyRectangleCol
		Cell c1, victim; // cell1, the victim cell
		Indexes crvs; // crossRegions.ridx[v]
		int bi // boxIndex
		  , v,sv // value, shiftedValue
		  , card // cardinality: number of cells which maybe v in erBox
		  , er // emptyRectangle pattern Index
		  , erBoxVs  // emptyRectangle.ridx[v].bits
		  , erR // emptyRectangleRow index
		  , erC // emptyRectangleCol index
		  , i // the ubiqitous index
		  // c2y for first verse; c2x for second verse (the inverse).
		  // I use distinct vars coz the names help explain the code.
		  // They both contain the bitset of the row/col index, and
		  // later the row/col index itself, decoded from the bitset.
		  , c2y, c2x
		  ;
		// deref grid fields ONCE for speed.
		final Box[] gBoxs = grid.boxs;
		final Row[] gRows = grid.rows;
		final Col[] gCols = grid.cols;
		final Cell[] gCells = grid.cells;
		// presume that no hint will be found
		boolean result = false;
		// foreach erBox
		for ( bi=0; bi<REGION_SIZE; ++bi ) {
			erBox = gBoxs[bi];
			// foreach value
			for ( v=1; v<VALUE_CEILING; ++v ) {
				sv = VSHFT[v];
				// ER patterns need 2..5 places for v, with the 1*1 cases:
				// boxes with ONE v in either erRow or erCol. These where
				// optional in hobiwans impl. To exclude change >1 to >2.
				if ( (card=erBox.ridx[v].size)>1 && card<6 ) {
					erBoxVs = erBox.ridx[v].bits;
					// foreach ER (EmptyRectangle) pattern
					PATTERN: for ( er=0; er<REGION_SIZE; ++er ) { // ER index
						// if this boxs v's match this ER pattern, ie box
						// doesn't have v's in places where it shouldn't.
						if ( (erBoxVs & EMPTY_BOXS[er]) == 0
						  // and erRow/Col v's are NOT all in the erBox
						  && (erBoxVs & ~ROWS[(erR=ER_ROWS[bi][er])%R]) != 0
						  && (erBoxVs & ~COLS[(erC=ER_COLS[bi][er])%R]) != 0
						) {
							erRow = gRows[erR];
							erCol = gCols[erC];

							// First verse: row => col
							//
							// We follow the erRow accross, examining each cell
							// that maybe v to see if there's two v's in that
							// col, and if so, jump up/down to the otherCell in
							// dat col to see if there's a v at intersection of
							// his row and the erCol (the victim), and if so it
							// is an EmptyRectangle: $victim cannot be v.
							//
							// hobiwan did first/second verse in a method by
							// reversing roles with swap-params and a flip var,
							// which I found confusing, so I simplified it, in
							// order to follow it. So I'm an idjit, but 0.107s
							// for top1465 verses hobiwans 3.652, so this is
							// just a little bit faster.
							//
							// Computers are idiots that follow instructions,
							// so a programmer thinks clearly and efficiently,
							// like an idiot; and sometimes that means NOT the
							// simplest (most abstract) possible solution.
							// Sometimes one gets ones knuckles dirty.
							//
							// foreach cell1 in the erRow
							for ( rCells=erRow.cells,i=0; i<REGION_SIZE; ++i ) {
								// and cell1 maybe v
//++COUNTS_A[0]; // 3,048,433
								if ( ((c1=rCells[i]).maybes & sv) != 0
//&& ++COUNTS_A[1] > 0L // 1,127,190
								  // and cell1 is NOT in the erBox
								  && c1.box != erBox
//&& ++COUNTS_A[2] > 0L // 566,237
								  // and cell1's col has ONE other place for v
								  // nb: the col is the cross region
								  && (crvs=c1.col.ridx[v]).size == 2
								  && bitCount(c2y=crvs.bits & ~ISHFT[c1.y]) == 1
//&& ++COUNTS_A[3] > 0L // 112,837
								  // and that other place maybe v
								  && (erCol.ridx[v].bits & c2y) != 0
//&& ++COUNTS_A[4] > 0L // 56,033
								  // and the victim is NOT in the erBox
								  && (victim=gCells[BY9[IFIRST[c2y]]+erC]).box != erBox
								) {
//++COUNTS_A[5]; // 293
									result = true;
									final Cell c2 = gCells[BY9[IFIRST[c2y]]+c1.x];
									final ARegion[] covers = {erRow, erBox, erCol, c2.col, c2.row};
									if ( accu.add(new EmptyRectangleHint(this, v
											, Regions.array(erBox)				// blue base regions
											, covers							// green covers regions
											, new Pots(c1, c2, v)				// greens
											, new Pots(erBox.atNew(erBoxVs), v)	// blues
											, new Pots(victim, v))) )			// reds
										return result;
									break PATTERN; // skip remaining cells and patterns
								}
							}

							// Second verse: col => row.
							// rows/cols and x/y reverse roles.
							//
							// Follow the erCol down, examining each cell
							// that maybe v to see if there's two v's in
							// that row, and if so, jump left/right to the
							// otherCell in that row to see if there's a v
							// at the intersection of his col and the erRow
							// (the victim), and if so, then it's an Empty
							// Rectangle: victim cannot be v.
							//
							// foreach cell1 in the erCol
							for ( rCells=erCol.cells,i=0; i<REGION_SIZE; ++i ) {
//++COUNTS_B[0]; // 3,045,974
								// if cell1 maybe v
								if ( ((c1=rCells[i]).maybes & sv) != 0
//&& ++COUNTS_B[1] > 0L // 1,126,606
								  // and cell1 is not in the erBox
								  && c1.box != erBox
//&& ++COUNTS_B[2] > 0L // 567,175
								  // and cell1's row has ONE other place for v
								  // nb: the row is the cross region
								  && (crvs=c1.row.ridx[v]).size == 2
								  && bitCount(c2x=crvs.bits & ~ISHFT[c1.x]) == 1
//&& ++COUNTS_B[3] > 0L // 114,496
								  // and that other place maybe v
								  && (erRow.ridx[v].bits & c2x) != 0
//&& ++COUNTS_B[4] > 0L // 57,806
								  // and the victim is NOT in the erBox
								  && (victim=gCells[BY9[erR]+IFIRST[c2x]]).box != erBox
								) {
//++COUNTS_B[5]; // 294
									// FOUND Empty Rectangle!
									result = true;
									final Cell c2 = gCells[BY9[c1.y]+IFIRST[c2x]];
									final ARegion[] covers = {erCol, erBox, erRow, c2.col, c2.row};
									if ( accu.add(new EmptyRectangleHint(this, v
											, Regions.array(erBox)				// blue base regions
											, covers							// green covers regions
											, new Pots(c1, c2, v)				// greens
											, new Pots(erBox.atNew(erBoxVs), v)	// blues
											, new Pots(victim, v))) )			// reds
										return result;
									break PATTERN; // skip remaining cells and patterns
								}
							}

							// A Box with 3+ v's matches only one ER which
							// we've just done, so break for speed.
							if ( card > 2 )
								break; // Mike drop!
						}
					}
				}
			}
		}
		return result;
	}

}
