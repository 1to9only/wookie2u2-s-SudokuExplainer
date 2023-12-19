/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The concept of the EmptyRectangle solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020. Kudos to
 * hobiwan. I am just the monkey at da keyboard who is too thick to follow the
 * code, so I gave-up and rolled my own from scratch; so this code is (pretty
 * much) mine. Mistakes are most definately mine. But the big ideas are still
 * hobiwans, by rights.
 *
 * Here is hobiwans standard licence statement:
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
import static diuf.sudoku.Grid.BOX_OF;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Grid.Col;
import static diuf.sudoku.Grid.BY9;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.Slots.*;
import static java.lang.Integer.parseInt;

/**
 * EmptyRectangle implements the {@link Tech#EmptyRectangle} Sudoku solving
 * technique. This code is based on hobiwans EmptyRectangle solver, but I use
 * idxs, and being too stupid to follow hobiwans code I rolled my own.
 * <p>
 * An EmptyRectangle occurs when cells that maybe v in a box are in one row and
 * one col, so either the row or the col must contain v. If either of those
 * row/col has a cross-region (col/row) with two places for v then eliminate v
 * from the other (non-box) end of the other col/row (crossRegion). Confused?
 * Soz. Sigh. Look at an EmptyRectangle hint in the GUI to make sense of it.
 * <p>
 * The logic is each box contains one v, in either the row or the col, so if it
 * is in the row/col which intersects the biplace region then the other place
 * in that region must be v, so the victim (intersection of "the other place"s
 * cross-region and the boxs other col/row) cannot be v; and if the box v
 * happens to be in other col/row then the victim still cannot be v, therefore
 * the victim cannot be v.
 * <p>
 * My best explanation is still poor. See an EmptyRectangle hint in the GUI.
 * A picture is worth a thousand words. The GUIs explanation is much clearer
 * than the above over-abstracted pile of s__te (Scootish accent).
 * <p>
 * The package name SDP stands for SingleDigitPattern which is the name of the
 * HoDoKu class that fathered all the hinters in this package. SDPs are found
 * by ChainerUnary, but SDP is simpler and therefore faster; and produces hints
 * that users can follow, despite my piss-poor explanation.
 * <p>
 * This code avoids using the continue statement, coz its slow.
 *
 * @author hobiwan
 * @author Keith Corlett 2020-03-25 use idxs
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * <p>
 * Diagram: The 9 Empty Rectangle patterns
 * <p>
 * A box can have one of nine different empty rectangles. In the below diagrams
 * 'X' means v must NOT be present (ie is NOT a maybe of this cell); and a
 * period '.' means that v must be present (ie is a maybe), and further must
 * be in both the row and the col. v is, as usual, the candidate value, which
 * just means a value that remains to be placed in this box.
 * <p>
 * The digits at the top of each box are: the index of this ER pattern in the
 * array, the erRow offset, and the erCol offset of this ER pattern. These
 * offsets are relative to the top-left of the box, which is in Box fields.
 * <p>
 * Note that hobiwan uses 81 ER-patterns: every pattern for every box, where I
 * (KRC) have just 9 ER-patterns and the Box fields to hang them on.
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
 * The '.' cells (the erRow and erCol) must contain at least 3 vs, at least one
 * of which must be exclusively in the row or col (the cell at erRow erCol does
 * not count).
 * <p>
 * With 2 positions the ER degenerates into an X-Chain. With all 3 vs in one
 * row or col the ER pattern is useless (degenerate to Locking?).
 * <p>
 * For fast/easy comparison we use bitset-indexes of all possible combinations
 * of the required empty cells in each box (the 'X's), created ONCE at startup.
 * <p>
 * <pre>
 * KRC 2021-12-28 Tried to reinstitute hobiwans "search" method using lambda
 * expressions to handle the role reversal of rows/cols and x/y, but could not
 * workout how to get the victim cell. Problem is swap row and col roles. This
 * technique is less code, but it is slower coz it calls 6 million more methods,
 * each with 5 lambdas that may be invoked.
 *
 * KRC 2022-01-19 Attempt to make it a bit faster.
 * PRE: 104,704,800 14783 7,082 567 184,664 EmptyRectangle 1:14 ????
 * removed the useless bitCount call from c2y/x tautology in both big ifs.
 * PST: 104,581,800 14778 7,076 546 191,541 EmptyRectangle 1:12 COLD
 * replace dereference != c2.y/x with > -1
 * PST:  98,694,200 14783 6,676 567 174,063 EmptyRectangle 1:10 COLD
 * remove the tautology altogether, to make it faster, but harder to follow.
 * But this run was 600ms faster overall, which says keep it.
 * PST: 101,426,200 14783 6,861 567 178,882 EmptyRectangle 1:10 COLD
 * erBoxRidx to save dereferencing erBox.ridx 18 times more often.
 * PST: 101,218,700 14783 6,846 567 178,516 EmptyRectangle 1:11 WARM
 * move setting sv down inside the if
 * PST: 104,065,700 14783 7,039 567 183,537 EmptyRectangle 1:12 WARM
 * revert to the tautology to double-check (it might actually be faster).
 * PST: 102,981,200 14783 6,966 567 181,624 EmptyRectangle 1:14 WARM
 * revert move setting sv back-up outside of the if
 * PST:  98,389,400 14783 6,655 567 173,526 EmptyRectangle 1:13 WARM
 * ridxs are history, but ZERO gain, which is disappointing.
 * PST: 100,506,887 14783 6,798 567 177,260 EmptyRectangle 1:13 WARM
 *
 * KRC 2022-07-01 reinstated hobiwans search method, rather than repeat code.
 * It is slower, but not by much, and I think this version is nicer-enough to
 * justify a two millisecond loss.
 * PRE: 107,617,200 16139 6,668 566 190,136 EmptyRectangle
 * PST: 109,060,100 16139 6,757 566 192,685 EmptyRectangle
 *
 * KRC 2023-09-26 resimplified: split search into searchRows and searchCols.
 * The shenanigans required to do both in one method made it slow. This faster
 * mostly coz early if-clauses are faster, at the expense of latter clauses,
 * which execute much less frequently.
 * PST:  84,307,400 15821 5,328 570 147,907 EmptyRectangle

 * </pre>
 */
public class EmptyRectangle extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[32];

	/**
	 * The square root of Grid.REGION_SIZE, which is 9, so I am 3. Note that I
	 * got a static init error referencing Grid.SQRT, so just recalculate it.
	 */
	private static final int R = (int)Math.sqrt(REGION_SIZE); // 3

	/**
	 * EMPTY_CELLS are the places that the erBox should NOT have vs; that is
	 * the box.places[v] (a bitset of box.cells indexes which may be v)
	 * which must <b>NOT</b> maybe v in order for this box to match this
	 * Empty&nbsp;Rectangle pattern. Note well that NOT!
	 * <p>
	 * Foreach ER pattern 0..8: {@code erBox.places[v] & EMPTY_CELLS[er]==0}
	 * ie v is not a maybe in any of the erBox cells that are denoted by a set
	 * (1) bit in EMPTY_CELLS.
	 * <p>
	 * NB: hobiwans erOffsets were absolute Grid.cells indices, but I am using
	 * the indices in region.cells because each pattern can be genericised to
	 * the abstract box (as apposed to every specific box) using the existing
	 * places[v]; so I translated them over. That is, I translated hobiwans
	 * erOffsets array of 9 boxs times 9 ER patterns, to an EMPTY_CELLS array
	 * of 9 ER patterns (no need for one for each box) to bitwise-and with the
	 * existing region.places[v].
	 * <p>
	 * Diagram: ARegion.places[v] indice numbers in a Box:
	 * <pre>
	 * + - - - +
	 * | 0 1 2 |
	 * | 3 4 5 |
	 * | 6 7 8 |
	 * + - - - +
	 * </pre>
	 * <p>
	 * Sorry but the following bitsets are upside down, and left is right.
	 * <p>
	 * In a LeastSignificantBitRight (LSBR) world, bitsets are a right-to-left
	 * representation of a left-to-right reality, ie everything is mirrored; so
	 * you must mentally mirror it back to make sense of it. Sorry about that,
	 * but I see no alternative.
	 * <p>
	 * A computer is like wiping your ass with wet-and-dry: I will swap you four
	 * 320s for a 1200. This is a 320 solution. Grin and bear it. Either that
	 * or reindex your head from EOF, which is NOT how s__t works. Davoes just
	 * rubbed his knob off, so he is off to the ER. Pretty-piss-poor joke that,
	 * but on with the show. You will want a mirror and some alternate physics.
	 * They are upside down! And right is left! And 1s (in below binaries) are
	 * cells which NOT maybe v, in the generic erBox, not a specific box. But I
	 * want my mommy! Well, it is done now. Sigh.
	 */
	private static final int[] ER_EMPTY_CELLS = {
	    // diagramatic: rows are broken-up to show each erBox as a box.
		// field                       {boxOffsets}  erRowOffset,erColOffset
		// bits                         0  1  2  3   R, C
		// row 1
		  parseInt("000"
				 + "011"
				 + "011", 2) //        {0, 1, 3, 4}  2, 2
		, parseInt("000"
				 + "101"
				 + "101", 2) //        {0, 2, 3, 5}  2, 1
		, parseInt("000"
				 + "110"
				 + "110", 2) //        {1, 2, 4, 5}  2, 0
		// row 2
		, parseInt("011"
				 + "000"
				 + "011", 2) //        {0, 1, 6, 7}  1, 2
		, parseInt("101"
				 + "000"
				 + "101", 2) //        {0, 2, 6, 8}  1, 1
		, parseInt("110"
				 + "000"
				 + "110", 2) //        {1, 2, 7, 8}  1, 0
		// row 3
		, parseInt("011"
				 + "011"
				 + "000", 2) //        {3, 4, 6, 7}  0, 2
		, parseInt("101"
				 + "101"
				 + "000", 2) //        {3, 5, 6, 8}  0, 1
		, parseInt("110"
				 + "110"
				 + "000", 2) //        {4, 5, 7, 8} 0, 0
	};

	/**
	 * Masks used to determine if all vs in a box are in the same row.
	 * <p>
	 * nb: ER_ROW_SLOTS repeats Slots.ROW_SLOTS 3 times just to avoid having to
	 * calculate erR%R repeatedly in findHints, for speed. Less is more.
	 * <p>
	 * A "slot" is three cells in a row/col and a box.
	 */
	private static final int[] ER_ROW_SLOTS = {
		  TOP_ROW, MID_ROW, BOT_ROW
		, TOP_ROW, MID_ROW, BOT_ROW
		, TOP_ROW, MID_ROW, BOT_ROW
	};

	/**
	 * Masks used to determine if all vs in a box are in the same col.
	 * <p>
	 * nb: ER_COL_SLOTS repeats Slots.COL_SLOTS 3 times just to avoid having to
	 * calculate erC%R repeatedly in findHints, for speed. Less is more.
	 * <p>
	 * A "slot" is three cells in a row/col and a box.
	 */
	public static final int[] ER_COL_SLOTS = {
		  LEF_COL, MID_COL, RIG_COL
		, LEF_COL, MID_COL, RIG_COL
		, LEF_COL, MID_COL, RIG_COL
	};

	/** The erRow index by [boxIndex][erPatternIndex]. */
	private static final int[][] ER_ROWS = new int[REGION_SIZE][REGION_SIZE];

	/** The erCol index by [boxIndex][erPatternIndex]. */
	private static final int[][] ER_COLS = new int[REGION_SIZE][REGION_SIZE];

	// initialize erSets, erRows, and erCols
	static {
		// erRow offsets: each erRow relative to row 0 of the erBox.<br>
		// nb: in the diagrams, . denotes the erRows
		final int[] erRowOffset = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};
		// erCol offsets: each erCol relative to col 0 of the erBox.<br>
		// nb: erCols are full of .s in the diagrams.
		final int[] erColOffset = new int[]{2, 1, 0, 2, 1, 0, 2, 1, 0};
		// row and col of top-left of each box
		int rowTL=0, colTL=0;
		// foreach box
		int bi=0;
		do {
			// foreach ER pattern
			int er=0;
			do {
				// translate the relative erRowOffset into an absolute
				// Grid index for this ER pattern in this box.
				ER_ROWS[bi][er] = rowTL + erRowOffset[er];
				// translate the relative erColOffset into an absolute
				// Grid index for this ER pattern in this box.
				ER_COLS[bi][er] = colTL + erColOffset[er];
			} while (++er < REGION_SIZE);
			if ( (bi % R) == 2 ) { // move onto the next row of boxs
				rowTL += R;
				colTL = 0;
			} else // move on to the next box in this row
				colTL += R;
		} while (++bi < REGION_SIZE);
	}

	// =========================== instance stuff ===========================

	public EmptyRectangle() {
		super(Tech.EmptyRectangle);
	}

	/**
	 * Find EmptyRectangle hints in $grid, and add them to $accu.
	 * <p>
	 * NOTE: ER is fast enough to be foxy (but uses idxs, which go outOfDate
	 * somewhere in the Generate/BruteForce mess) hence I override the actual
	 * findHints(Grid, IAccumulator) method, as apposed to the normal no-args
	 * implementation method: findHints(). The normal grid-fields are null.
	 * <p>
	 * WARN: Dont trust my names for things. I am all muddled-up.
	 *
	 * @param grid the Sudoku puzzle to search
	 * @param accu to which I add hints
	 * @return any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {		//      16,226
		// all ANSI-C style vars means ONE stackframe, unless we hint.
		// This makes iterating arrays a pain in the ass. Get Over It!
		Box erBox;	// emptyRectangleBox where an ER starts
		int bi		// boxIndex: index in grid.boxs of the erBox
		  , card	// cardinality: number of cells which maybe v in erBox
		  , er		// emptyRectangle pattern Index 0..8
		  , erBoxVs // erBox.places[v]: bitset of indexes in erBox.cells
		  , erR		// erRow index: the row containing vs in the erBox
		  , erC		// erCol index: the col containing vs in the erBox
		  , erRows[] // the erRow of all ER-patterns for erBox
		  , erCols[] // the erCol of all ER-patterns for erBox
		;
		// presume that no hint will be found
		boolean result = false;
		try {
			super.grid = grid;
			super.boxs = grid.boxs;
			super.rows = grid.rows;
			super.cols = grid.cols;
			super.accu = accu;
			// foreach box
			bi=0;
			do {																//     146,034
				erRows = ER_ROWS[bi];
				erCols = ER_COLS[bi];
				erBox = boxs[bi];
				// foreach value that remains to placed in this box
				for ( int v : VALUESES[erBox.unsetCands] ) {					//     829,670
					// ERs start from an erBox with 2..5 vs.
					// nb: to remain unset by HiddenSingle, v has atleast two
					// places in this region.
					if ( (card=erBox.numPlaces[v]) < 6 ) {						//     790,379
						// bitset of vs in erBox.cells
						erBoxVs = erBox.places[v];
						// foreach ER pattern (diagrams in class commments)
						er = 0; // ER index
						do {													//   6,088,146
							// if erBoxVs are all in erRow or erCol (no others).
							if ( (erBoxVs & ER_EMPTY_CELLS[er]) < 1				//   1,234,785
							  // and there is a v in the erRow-erBox SLOT
							  && (erBoxVs & ~ER_ROW_SLOTS[erR=erRows[er]]) > 0	//     803,529
							  // and there is a v in the erCol-erBox SLOT
							  && (erBoxVs & ~ER_COL_SLOTS[erC=erCols[er]]) > 0	//     384,165
							) {
								// first verse: row => col
								if ( searchRow(bi, v, erR, erC) ) {
									result = true;
									if(oneHintOnly) return result;
									break; // skip remaining ER patterns
								// second verse: col => row
								// nb: erBox matches on row or col, never both.
								} else if ( searchCol(bi, v, erC, erR) ) {
									result = true;
									if(oneHintOnly) return result;
									break; // skip remaining ER patterns
								}
								// box with 2 vs can match two ER patterns, but
								// a box with 3+ vs can match only one pattern.
								if ( card > 2 )
									break; // mike drop!
							}
						} while (++er < REGION_SIZE);
					}
				}
			} while (++bi < REGION_SIZE);
		} finally {
			super.grid = null;
			super.boxs = null;
			super.rows = null;
			super.cols = null;
			super.accu = null;
		}
		return result;
	}

	/**
	 * Search the cells in the $primary region (a row) for an EmptyRectangle.
	 * <p>
	 * searchRow and searchCol are two mirrored methods, for speed. This was a
	 * both-in-one method, but it was more complex, so slower.
	 * <p>
	 * WARN: Dont trust my names for things. I am all muddled-up.
	 *
	 * @param bi index of the erBox in grid.boxs. The erBox contains one end of
	 *  each of the p (primary) and s (secondary) regions
	 * @param v the current search value
	 * @param erR erRow index (primary)
	 * @param erC erCol index (secondary)
	 * @return any hint/s found
	 */																			//     384,165
	private boolean searchRow(final int bi, final int v, final int erR, final int erC) {
		Cell c1; // the first cell in the primary region (erRow)
		Col col; // c1.col: cross (secondary) region of c1
		int c2y; // c2.y: vertical (row) index of c2
		int vctm; // indice of the cell to remove v from, if any
		final Col s = cols[erC]; // secondary region (erCol)
		final Row p = rows[erR]; // primary region (erRow)
		final Cell[] pCells = p.cells; // primary.cells
		final int[] pIndices = p.indices; // primary.indices
		// foreach place for v in the erRow (primary region)
		for ( int i : INDEXES[p.places[v]] ) {									//   1,279,465
			// if c1 is NOT in the erBox
			if ( BOX_OF[pIndices[i]] != bi										//     644,550
			  // and c1.col (primary) has ONE other place for v
			  && (col=(c1=pCells[i]).col).numPlaces[v] == 2						//     123,961
			  // and the other end of the erCol maybe v
			  && (s.places[v] & (c2y=col.places[v] & ~c1.placeIn[COL])) > 0		//      61,936
			  // and the victim is NOT in the erBox
			  && BOX_OF[vctm=BY9[IFIRST[c2y]]+erC] != bi						//         332
			) {
				accu.add(hint(grid.boxs[bi], v, p, s, c1
						, grid.cells[BY9[IFIRST[c2y]]+c1.x], vctm));
				return true;
			}
		}
		return false;
	}

	/**
	 * Search the cells in the $primary region (a col) for an EmptyRectangle.
	 * <p>
	 * searchRow and searchCol are two mirrored methods, for speed. This was a
	 * both-in-one method, but it was more complex, so slower.
	 * <p>
	 * WARN: Dont trust my names for things. I am all muddled-up.
	 *
	 * @param bi regions index of erBox, which contains one end of each
	 *  of the p (primary) and s (secondary) regions
	 * @param v the current search value
	 * @param erC erCol index (primary)
	 * @param erR erRow index (secondary)
	 * @return any hint/s found
	 */																			//     383,833
	private boolean searchCol(final int bi, final int v, final int erC, final int erR) {
		Cell c1; // the first cell in the primary region (erCol)
		Row row; // c1.row: cross (secondary) region of c1
		int c2x; // c2.x: horizontal (col) index of c2
		int vctm; // indice of the cell to remove v from, if any
		final Row s = rows[erR]; // secondary region (erRow)
		final Col p = cols[erC]; // primary region (erCol)
		final Cell[] pCells = p.cells; // primary.cells
		final int[] pIndices = p.indices; // primary.indices
		// foreach place for v in the erCol (primary region)
		for ( int i : INDEXES[p.places[v]] ) {									//   1,278,063
			// if c1 is NOT in the erBox
			if ( BOX_OF[pIndices[i]] != bi										//     644,046
			  // and c1.row (primary) has ONE other place for v
			  && (row=(c1=pCells[i]).row).numPlaces[v] == 2						//     125,563
			  // and the other end of the erRow maybe v
			  && (s.places[v] & (c2x=row.places[v] & ~c1.placeIn[ROW])) > 0		//      63,241
			  // and the victim is NOT in the erBox
			  && BOX_OF[vctm=BY9[erR]+IFIRST[c2x]] != bi						//         324
			) {
				accu.add(hint(grid.boxs[bi], v, p, s, c1
						, grid.cells[BY9[c1.y]+IFIRST[c2x]], vctm));
				return true;
			}
		}
		return false;
	}

	/**
	 * Construct a new EmptyRectangleHint.
	 *
	 * @param erBox the EmptyRectangle Box (the one we started from)
	 * @param v value to remove
	 * @param p primary region
	 * @param s secondary region
	 * @param c1 cell1
	 * @param c2 cell2 the second cell in the primary region
	 * @param vctm indice of cell to remove v from
	 * @return a new EmptyRectangleHint
	 */
	private AHint hint(final Box erBox, final int v, final ARegion p
			, final ARegion s, final Cell c1, final Cell c2, final int vctm) {
		// bases blue regions
		final ARegion[] bases = Regions.array(erBox);
		// covers green regions (nb: user does not see order of covers)
		final ARegion[] covers = {p, s, c2.col, c2.row};
		// green corners
		final Pots greens = new Pots(v, c1, c2);
		// blue vs in the erBox
		final Pots blues = new Pots(erBox.atNew(erBox.places[v]), v);
		// red eliminations
		final Pots reds = new Pots(vctm, v);
		// construct and return the bastard
		return new EmptyRectangleHint(grid, this, v, bases, covers
				, greens, blues, reds);
	}

}
