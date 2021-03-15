/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Ass.Cause;
import static diuf.sudoku.Idx.BITS_PER_ELEMENT;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Indexes.ISIZE;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.io.IO;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.urt.UniqueRectangle.IUrtCellSet;
import diuf.sudoku.solver.hinters.wing.BitIdx;
import diuf.sudoku.utils.Hash;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyStrings;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * A Sudoku grid.
 * <p>
 * Contains the 9x9 array of cells, as well as classes and methods
 * to manipulate regions (boxes, rows, and columns).
 * <p>
 * Vertical coordinates range from 0 (topmost) to 8 (bottommost).<br>
 * Horizontal coordinates range from 0 (leftmost) to 8 (rightmost).
 * <p>
 * This is the basis of all the Sudoku Explainer software. There's lots of
 * "weird stuff" in here, so please be careful when you change stuff, you
 * need to be aware of everything that uses everything. Adding new stuff
 * usually works-out less painful.
 */
// it's actually faster to copy small arrays into collections "manually"
@SuppressWarnings("ManualArrayToCollectionCopy")
public final class Grid {

	// always do this first, else toString exception in debugger
	private static final char[] DIGITS = ".123456789".toCharArray();

	private static final String NL = diuf.sudoku.utils.Frmt.NL;
	public static final Random RANDOM = new Random();

	/**
	 * AUTOSOLVE: true for speed, false to count Naked and Hidden Singles.
	 * A self-documenting switch for AHint.apply isAutosolving parameter.
	 * <p>
	 * If true Naked and Hidden Singles are set by Cell.set in AHint.apply,
	 * which is faster, but then LogicalSolver.solve doesn't count them as
	 * Naked/Hidden Singles, instead ascribing them to whichever hinter is
	 * lucky enough to hit the domino; funkenating ALL the counts, not just
	 * those for Naked Single and Hidden Single.
	 * <p>
	 * If false then Singles are left for the NakedSingle/HiddenSingle hinters
	 * to find, report, and apply. The recursive solver always uses AUTOSOLVE.
	 * SO: If Singles are unimportant to you then YOU change AUTOSOLVE to true.
	 * Everything that needs-to references it. Beware that LogicalSolver.solve
	 * hint/elim counts are then borken.
	 * <p>
	 * PS: Ideally true for -SPEED only but need compile-time switch, so YOU
	 * set me true before you -SPEED, and flip me back after.
	 */
	public static final boolean AUTOSOLVE = false;
	/** A self-documenting AHint.apply isAutosolving parameter. */
	public static final boolean NO_AUTOSOLVE = false;

	/** The ID field of each cell in this Grid. They're public static so that
	 * they can be referenced from anywhere just using a cell index. */
	public static final String[] ROW_IDS = {"1","2","3","4","5","6","7","8","9"};
	public static final String[] COL_IDS = {"A","B","C","D","E","F","G","H","I"};
	public static final int CELL_ID_LENGTH = 2;
	public static final String[] CELL_IDS = new String[81];
	static {
		for ( int i=0; i<81; ++i )
			CELL_IDS[i] = COL_IDS[i%9] + ROW_IDS[i/9];
	}

	/** The region.id's by index in the grid.regions array. */
	public static final String[] REGION_IDS = {
		"box 1", "box 2", "box 3", "box 4", "box 5", "box 6", "box 7", "box 8", "box 9",
		"row 1", "row 2", "row 3", "row 4", "row 5", "row 6", "row 7", "row 8", "row 9",
		"col A", "col B", "col C", "col D", "col E", "col F", "col G", "col H", "col I"
	};

	/** The buddies of each cell in the Grid, in an Idx. You can calculate this
	 * but I've just hit the problem with calculating this: bugs! */
	public static final int[] BOXS = {
		0, 0, 0, 1, 1, 1, 2, 2, 2
	  , 0, 0, 0, 1, 1, 1, 2, 2, 2
	  , 0, 0, 0, 1, 1, 1, 2, 2, 2
	  , 3, 3, 3, 4, 4, 4, 5, 5, 5
	  , 3, 3, 3, 4, 4, 4, 5, 5, 5
	  , 3, 3, 3, 4, 4, 4, 5, 5, 5
	  , 6, 6, 6, 7, 7, 7, 8, 8, 8
	  , 6, 6, 6, 7, 7, 7, 8, 8, 8
	  , 6, 6, 6, 7, 7, 7, 8, 8, 8
	};

	public static final IdxL[] BUDDIES = new IdxL[81];
	static {
		// faster and easier to static-array box instead of calculating it:
		// private static int box(int i) { return i/9/3*3 + i%9/3; }
		// called with: || box(cell) == box(bud)
		// first we calculate an index of all the buddies of each cell.
		// NB: DIY in static-land. Normally we use Cells, ie an instance.
		IdxL buds;
		int box, row, col; // of this cell
		for ( int i=0; i<81; ++i ) { // foreach cell-indice
			buds = BUDDIES[i] = new IdxL();
			row=i/9; col=i%9; box=BOXS[i];
			for ( int b=0; b<81; ++b ) // foreach buddy-cell-indice
				if ( (b/9==row || b%9==col || BOXS[b]==box) && i!=b )
					buds.add(b);
			buds.lock(); // so any attempt to mutate throws a LockedException
		}
	}

	/** Indices of siblings: The indices of cells which are in the same box,
	 * row, or col, as the index cell, except the index cell itself. */
	public static final int[][] VISIBLE_INDICES  = new int[][] {
			{ 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,18,19,20,27,36,45,54,63,72},
			{ 0, 2, 3, 4, 5, 6, 7, 8, 9,10,11,18,19,20,28,37,46,55,64,73},
			{ 0, 1, 3, 4, 5, 6, 7, 8, 9,10,11,18,19,20,29,38,47,56,65,74},
			{ 0, 1, 2, 4, 5, 6, 7, 8,12,13,14,21,22,23,30,39,48,57,66,75},
			{ 0, 1, 2, 3, 5, 6, 7, 8,12,13,14,21,22,23,31,40,49,58,67,76},
			{ 0, 1, 2, 3, 4, 6, 7, 8,12,13,14,21,22,23,32,41,50,59,68,77},
			{ 0, 1, 2, 3, 4, 5, 7, 8,15,16,17,24,25,26,33,42,51,60,69,78},
			{ 0, 1, 2, 3, 4, 5, 6, 8,15,16,17,24,25,26,34,43,52,61,70,79},
			{ 0, 1, 2, 3, 4, 5, 6, 7,15,16,17,24,25,26,35,44,53,62,71,80},
			{ 0, 1, 2,10,11,12,13,14,15,16,17,18,19,20,27,36,45,54,63,72},
			{ 0, 1, 2, 9,11,12,13,14,15,16,17,18,19,20,28,37,46,55,64,73},
			{ 0, 1, 2, 9,10,12,13,14,15,16,17,18,19,20,29,38,47,56,65,74},
			{ 3, 4, 5, 9,10,11,13,14,15,16,17,21,22,23,30,39,48,57,66,75},
			{ 3, 4, 5, 9,10,11,12,14,15,16,17,21,22,23,31,40,49,58,67,76},
			{ 3, 4, 5, 9,10,11,12,13,15,16,17,21,22,23,32,41,50,59,68,77},
			{ 6, 7, 8, 9,10,11,12,13,14,16,17,24,25,26,33,42,51,60,69,78},
			{ 6, 7, 8, 9,10,11,12,13,14,15,17,24,25,26,34,43,52,61,70,79},
			{ 6, 7, 8, 9,10,11,12,13,14,15,16,24,25,26,35,44,53,62,71,80},
			{ 0, 1, 2, 9,10,11,19,20,21,22,23,24,25,26,27,36,45,54,63,72},
			{ 0, 1, 2, 9,10,11,18,20,21,22,23,24,25,26,28,37,46,55,64,73},
			{ 0, 1, 2, 9,10,11,18,19,21,22,23,24,25,26,29,38,47,56,65,74},
			{ 3, 4, 5,12,13,14,18,19,20,22,23,24,25,26,30,39,48,57,66,75},
			{ 3, 4, 5,12,13,14,18,19,20,21,23,24,25,26,31,40,49,58,67,76},
			{ 3, 4, 5,12,13,14,18,19,20,21,22,24,25,26,32,41,50,59,68,77},
			{ 6, 7, 8,15,16,17,18,19,20,21,22,23,25,26,33,42,51,60,69,78},
			{ 6, 7, 8,15,16,17,18,19,20,21,22,23,24,26,34,43,52,61,70,79},
			{ 6, 7, 8,15,16,17,18,19,20,21,22,23,24,25,35,44,53,62,71,80},
			{ 0, 9,18,28,29,30,31,32,33,34,35,36,37,38,45,46,47,54,63,72},
			{ 1,10,19,27,29,30,31,32,33,34,35,36,37,38,45,46,47,55,64,73},
			{ 2,11,20,27,28,30,31,32,33,34,35,36,37,38,45,46,47,56,65,74},
			{ 3,12,21,27,28,29,31,32,33,34,35,39,40,41,48,49,50,57,66,75},
			{ 4,13,22,27,28,29,30,32,33,34,35,39,40,41,48,49,50,58,67,76},
			{ 5,14,23,27,28,29,30,31,33,34,35,39,40,41,48,49,50,59,68,77},
			{ 6,15,24,27,28,29,30,31,32,34,35,42,43,44,51,52,53,60,69,78},
			{ 7,16,25,27,28,29,30,31,32,33,35,42,43,44,51,52,53,61,70,79},
			{ 8,17,26,27,28,29,30,31,32,33,34,42,43,44,51,52,53,62,71,80},
			{ 0, 9,18,27,28,29,37,38,39,40,41,42,43,44,45,46,47,54,63,72},
			{ 1,10,19,27,28,29,36,38,39,40,41,42,43,44,45,46,47,55,64,73},
			{ 2,11,20,27,28,29,36,37,39,40,41,42,43,44,45,46,47,56,65,74},
			{ 3,12,21,30,31,32,36,37,38,40,41,42,43,44,48,49,50,57,66,75},
			{ 4,13,22,30,31,32,36,37,38,39,41,42,43,44,48,49,50,58,67,76},
			{ 5,14,23,30,31,32,36,37,38,39,40,42,43,44,48,49,50,59,68,77},
			{ 6,15,24,33,34,35,36,37,38,39,40,41,43,44,51,52,53,60,69,78},
			{ 7,16,25,33,34,35,36,37,38,39,40,41,42,44,51,52,53,61,70,79},
			{ 8,17,26,33,34,35,36,37,38,39,40,41,42,43,51,52,53,62,71,80},
			{ 0, 9,18,27,28,29,36,37,38,46,47,48,49,50,51,52,53,54,63,72},
			{ 1,10,19,27,28,29,36,37,38,45,47,48,49,50,51,52,53,55,64,73},
			{ 2,11,20,27,28,29,36,37,38,45,46,48,49,50,51,52,53,56,65,74},
			{ 3,12,21,30,31,32,39,40,41,45,46,47,49,50,51,52,53,57,66,75},
			{ 4,13,22,30,31,32,39,40,41,45,46,47,48,50,51,52,53,58,67,76},
			{ 5,14,23,30,31,32,39,40,41,45,46,47,48,49,51,52,53,59,68,77},
			{ 6,15,24,33,34,35,42,43,44,45,46,47,48,49,50,52,53,60,69,78},
			{ 7,16,25,33,34,35,42,43,44,45,46,47,48,49,50,51,53,61,70,79},
			{ 8,17,26,33,34,35,42,43,44,45,46,47,48,49,50,51,52,62,71,80},
			{ 0, 9,18,27,36,45,55,56,57,58,59,60,61,62,63,64,65,72,73,74},
			{ 1,10,19,28,37,46,54,56,57,58,59,60,61,62,63,64,65,72,73,74},
			{ 2,11,20,29,38,47,54,55,57,58,59,60,61,62,63,64,65,72,73,74},
			{ 3,12,21,30,39,48,54,55,56,58,59,60,61,62,66,67,68,75,76,77},
			{ 4,13,22,31,40,49,54,55,56,57,59,60,61,62,66,67,68,75,76,77},
			{ 5,14,23,32,41,50,54,55,56,57,58,60,61,62,66,67,68,75,76,77},
			{ 6,15,24,33,42,51,54,55,56,57,58,59,61,62,69,70,71,78,79,80},
			{ 7,16,25,34,43,52,54,55,56,57,58,59,60,62,69,70,71,78,79,80},
			{ 8,17,26,35,44,53,54,55,56,57,58,59,60,61,69,70,71,78,79,80},
			{ 0, 9,18,27,36,45,54,55,56,64,65,66,67,68,69,70,71,72,73,74},
			{ 1,10,19,28,37,46,54,55,56,63,65,66,67,68,69,70,71,72,73,74},
			{ 2,11,20,29,38,47,54,55,56,63,64,66,67,68,69,70,71,72,73,74},
			{ 3,12,21,30,39,48,57,58,59,63,64,65,67,68,69,70,71,75,76,77},
			{ 4,13,22,31,40,49,57,58,59,63,64,65,66,68,69,70,71,75,76,77},
			{ 5,14,23,32,41,50,57,58,59,63,64,65,66,67,69,70,71,75,76,77},
			{ 6,15,24,33,42,51,60,61,62,63,64,65,66,67,68,70,71,78,79,80},
			{ 7,16,25,34,43,52,60,61,62,63,64,65,66,67,68,69,71,78,79,80},
			{ 8,17,26,35,44,53,60,61,62,63,64,65,66,67,68,69,70,78,79,80},
			{ 0, 9,18,27,36,45,54,55,56,63,64,65,73,74,75,76,77,78,79,80},
			{ 1,10,19,28,37,46,54,55,56,63,64,65,72,74,75,76,77,78,79,80},
			{ 2,11,20,29,38,47,54,55,56,63,64,65,72,73,75,76,77,78,79,80},
			{ 3,12,21,30,39,48,57,58,59,66,67,68,72,73,74,76,77,78,79,80},
			{ 4,13,22,31,40,49,57,58,59,66,67,68,72,73,74,75,77,78,79,80},
			{ 5,14,23,32,41,50,57,58,59,66,67,68,72,73,74,75,76,78,79,80},
			{ 6,15,24,33,42,51,60,61,62,69,70,71,72,73,74,75,76,77,79,80},
			{ 7,16,25,34,43,52,60,61,62,69,70,71,72,73,74,75,76,77,78,80},
			{ 8,17,26,35,44,53,60,61,62,69,70,71,72,73,74,75,76,77,78,79}
	};

	/** Indices of "forward" siblings: The indices of sibling cells whose
	 * index is greater than the current cell; so that we can loop through
	 * this array, and search for sibling relationships which we have not
	 * already been examined. Supports a forward only siblings search. */
	public static final int[][] FORWARD_INDICES = new int [][] {
			  {1,2,3,4,5,6,7,8,9,10,11,18,19,20,27,36,45,54,63,72}
			, {2,3,4,5,6,7,8,9,10,11,18,19,20,28,37,46,55,64,73}
			, {3,4,5,6,7,8,9,10,11,18,19,20,29,38,47,56,65,74}
			, {4,5,6,7,8,12,13,14,21,22,23,30,39,48,57,66,75}
			, {5,6,7,8,12,13,14,21,22,23,31,40,49,58,67,76}
			, {6,7,8,12,13,14,21,22,23,32,41,50,59,68,77}
			, {7,8,15,16,17,24,25,26,33,42,51,60,69,78}
			, {8,15,16,17,24,25,26,34,43,52,61,70,79}
			, {15,16,17,24,25,26,35,44,53,62,71,80}
			, {10,11,12,13,14,15,16,17,18,19,20,27,36,45,54,63,72}
			, {11,12,13,14,15,16,17,18,19,20,28,37,46,55,64,73}
			, {12,13,14,15,16,17,18,19,20,29,38,47,56,65,74}
			, {13,14,15,16,17,21,22,23,30,39,48,57,66,75}
			, {14,15,16,17,21,22,23,31,40,49,58,67,76}
			, {15,16,17,21,22,23,32,41,50,59,68,77}
			, {16,17,24,25,26,33,42,51,60,69,78}
			, {17,24,25,26,34,43,52,61,70,79}
			, {24,25,26,35,44,53,62,71,80}
			, {19,20,21,22,23,24,25,26,27,36,45,54,63,72}
			, {20,21,22,23,24,25,26,28,37,46,55,64,73}
			, {21,22,23,24,25,26,29,38,47,56,65,74}
			, {22,23,24,25,26,30,39,48,57,66,75}
			, {23,24,25,26,31,40,49,58,67,76}
			, {24,25,26,32,41,50,59,68,77}
			, {25,26,33,42,51,60,69,78}
			, {26,34,43,52,61,70,79}
			, {35,44,53,62,71,80}
			, {28,29,30,31,32,33,34,35,36,37,38,45,46,47,54,63,72}
			, {29,30,31,32,33,34,35,36,37,38,45,46,47,55,64,73}
			, {30,31,32,33,34,35,36,37,38,45,46,47,56,65,74}
			, {31,32,33,34,35,39,40,41,48,49,50,57,66,75}
			, {32,33,34,35,39,40,41,48,49,50,58,67,76}
			, {33,34,35,39,40,41,48,49,50,59,68,77}
			, {34,35,42,43,44,51,52,53,60,69,78}
			, {35,42,43,44,51,52,53,61,70,79}
			, {42,43,44,51,52,53,62,71,80}
			, {37,38,39,40,41,42,43,44,45,46,47,54,63,72}
			, {38,39,40,41,42,43,44,45,46,47,55,64,73}
			, {39,40,41,42,43,44,45,46,47,56,65,74}
			, {40,41,42,43,44,48,49,50,57,66,75}
			, {41,42,43,44,48,49,50,58,67,76}
			, {42,43,44,48,49,50,59,68,77}
			, {43,44,51,52,53,60,69,78}
			, {44,51,52,53,61,70,79}
			, {51,52,53,62,71,80}
			, {46,47,48,49,50,51,52,53,54,63,72}
			, {47,48,49,50,51,52,53,55,64,73}
			, {48,49,50,51,52,53,56,65,74}
			, {49,50,51,52,53,57,66,75}
			, {50,51,52,53,58,67,76}
			, {51,52,53,59,68,77}
			, {52,53,60,69,78}
			, {53,61,70,79}
			, {62,71,80}
			, {55,56,57,58,59,60,61,62,63,64,65,72,73,74}
			, {56,57,58,59,60,61,62,63,64,65,72,73,74}
			, {57,58,59,60,61,62,63,64,65,72,73,74}
			, {58,59,60,61,62,66,67,68,75,76,77}
			, {59,60,61,62,66,67,68,75,76,77}
			, {60,61,62,66,67,68,75,76,77}
			, {61,62,69,70,71,78,79,80}
			, {62,69,70,71,78,79,80}
			, {69,70,71,78,79,80}
			, {64,65,66,67,68,69,70,71,72,73,74}
			, {65,66,67,68,69,70,71,72,73,74}
			, {66,67,68,69,70,71,72,73,74}
			, {67,68,69,70,71,75,76,77}
			, {68,69,70,71,75,76,77}
			, {69,70,71,75,76,77}
			, {70,71,78,79,80}
			, {71,78,79,80}
			, {78,79,80}
			, {73,74,75,76,77,78,79,80}
			, {74,75,76,77,78,79,80}
			, {75,76,77,78,79,80}
			, {76,77,78,79,80}
			, {77,78,79,80}
			, {78,79,80}
			, {79,80}
			, {80}
			, {}
	};

	public static final BitIdx[] BUDETTES = new BitIdx[81];
	public static BitIdx[] AFTER = new BitIdx[81];
	static {
		for ( int i=0; i<81; ++i ) {
			BUDETTES[i] = new BitIdx(VISIBLE_INDICES[i]);
			AFTER[i] = new BitIdx(FORWARD_INDICES[i]);
		}
	}

	public static Set<Cell> cellSet(Cell... cells) {
		Set<Cell> result = new HashSet<>(cells.length, 1.0F);
		for ( Cell c : cells )
			result.add(c);
		return result;
	}

	/**
	 * Return the common row, col, or box of two Cells; else null.
	 * Note that box comes LAST (for a change).
	 * @param a Cell
	 * @param b Cell
	 * @return ARegion
	 */
	public static ARegion commonRegion(Cell a, Cell b) {
		if ( a.row == b.row )
			return a.row;
		if ( a.col == b.col )
			return a.col;
		if ( a.box == b.box )
			return a.box;
		return null;
	}

	/**
	 * Returns the Grid.cells indice of the cell with the given id.
	 * <p>
	 * Note that I'm a static method, so you don't need the grid to
	 * reverse engineer a Cell.id into it's indice.
	 *
	 * @param id
	 * @return {@code (id.charAt(1)-'1')*9 + (id.charAt(0)-'A')}
	 */
	public static int indice(String id) {
		//     2nd char is y (row)    1st char is x (col)
		return (id.charAt(1)-'1')*9 + (id.charAt(0)-'A');
	}

//KEEP4DOC: not used but retain for noobs to see how to JUST CALCULATE IT!
//	/**
//	 * Get the Grid.cells indice from y and x coordinates.
//	 * <p>
//	 * <b>NOTE</b>: coordinates are given y, x; as per the matrix (not the
//	 * usual x, y).
//	 *
//	 * @param y the row number 0..8 (vertical coordinate)
//	 * @param x the col number 0..8 (horizontal coordinate)
//	 * @return the Grid.cells indice of the cell
//	 */
//	public static int indice(int y, int x) {
//		return y * 9 + x;
//	}

	/** Cell ArrayS ~ late populated! coz the larger arrays were never used. */
	private static final Cell[][] CAS = new Cell[82][]; // factorial 81
	static {
		// note that a cell has 20 siblings. larger arrays are late populated
		// if they are ever used, except the last one, which contains 82 cells
		// because remember I'm taking a size arguement, and just sometimes
		// they're a bit WRONG, so it won't AIOOBE if just create an extra.
		// Lazy but it works.
		for ( int i=0; i<21; ++i )
			CAS[i] = new Cell[i];
		CAS[81] = new Cell[81];
	}

	/**
	 * cas (Cell ArrayS) returns the {@code Cell[]} of the given size from the
	 * Grids cell array cache, rather than create temporary arrays (slow). If
	 * you're storing the array then it's up to you to clone() the bastard; coz
	 * the array I return might be returned again the next time I'm called, so
	 * it's contents will be arbitrarily overwritten.
	 * <p>
	 * <b>You have been warned!</b>
	 *
	 * @param size the size of the cached array to retrieve
	 * @return the <b>cached</b> {@code Cell[]}. Did I mention it's cached?
	 */
	public static Cell[] cas(int size) {
		Cell[] cells = CAS[size];
		// late populate CellsArrayS 21..80
		if ( cells == null )
			cells = CAS[size] = new Cell[size];
		return cells;
	}

	/**
	 * Convenience method to get a new List of a re-usable cells array.
	 * @param cells
	 * @param numCells
	 * @return
	 */
	public static ArrayList<Cell> list(Cell[] cells, int numCells) {
		ArrayList<Cell> result = new ArrayList<>(numCells);
		for ( int i=0; i<numCells; ++i )
			result.add(cells[i]);
		return result;
	}

	public static List<ARegion> regionList(ARegion... regions) {
		List<ARegion> result = new ArrayList<>(regions.length);
		for ( ARegion r : regions )
			result.add(r);
		return result;
	}

	// ============================= instance land ============================

	/** A flat view of the cells in the matrix */
	public final Cell[] cells = new Cell[81];
	/** An array of the 9 Boxes in this grid, indexed 0=top-left across and
	 * then down to 8=bottom-right. */
	public final Box[] boxs = new Box[9];
	/** An array of the 9 Rows in this grid, indexed 0=topmost down
	 * to 8=bottommost */
	public final Row[] rows = new Row[9];
	/** An array of the 9 Cols in this grid, indexed 0=leftmost across
	 * to 8=rightmost */
	public final Col[] cols = new Col[9];
	/** An array of all the regions in this grid, indexed 9 Boxs,
	 * then 9 Rows, then 9 Cols; sub-order same as boxs, rows, and cols */
	public final ARegion[] regions = new ARegion[27];

//	/**
//	 * An array of each cells buds: the 20 other cells in the same Box, Row,
//	 * or Col as this cell (excluding the cell itself). Note that these are the
//	 * same Idx instances referenced by {@link Cell#buds}; this array saves
//	 * us getting the cell in order to get it's buddies.
//	 */
//	public final IdxL[] buds = new IdxL[81];

	/** This switch is set to true when the maybes (the potential cell values)
	 * are loaded from the input file, otherwise the loader eliminates loaded
	 * cell values from the maybes. Copy/paste saves/loads maybes for debugging
	 * so that you can copy the grids-string from the debugger (or wherever)
	 * and paste it "as is" into a Sudoku Solver GUI to see any problems. */
	public boolean isMaybesLoaded = false;

	/** The PuzzleID (File and lineNumber for MagicTours Multi-puzzle format).*/
	public PuzzleID source = null;

	/** Set to false at END of Constructor. Used by Cells resetMaybes(), which
	 * is called by the rebuildMaybes() method, to avoid wasting time
	 * reinitialising empty cells during Grid construction. */
	final boolean initialised;

	/** cache the SolutionHint (in a list just coz that's the return type).
	 * used by LogicalSolver.getSolution (SudokuExplainer.getAllHints) */
	public List<AHint> solutionHints = null;

	/** solutionValues is set by RecursiveAnalyser.solve(Grid grid) and
	 * read by AAHdkAlsHinter.valid(Grid grid, Pots redPots) et al. */
	public int[] solutionValues;

	/** the puzzleID field is an identifier for the puzzle that's loaded into
	 * this grid. It's just a random long that's set whenever we create a new
	 * grid or load a puzzle into a grid, so that IHinter.getHints can tell if
	 * the grid contains a different puzzle than it did the last time I ran.
	 * As far as I can see (not far) Random.nextLong() guarantees that two
	 * successive calls won't return the same value. If it does then
	 * AAHdkAlsHinter.valid will report false-positives (invalid hints). */
	public long puzzleID;

	/** isInvalidated()'s message explaining why this grid is invalid. */
	public String invalidity;
	/** isInvalidated()'s region which is invalid. Nullable. */
	public ARegion invalidRegion;

	/** prepare sets grid.isPrepared, to avert unnecessary repetitions. */
	private boolean isPrepared = false;

	// ----------------------------- constructors -----------------------------

	/** Construct a new 9x9 Sudoku grid. All cells are set to empty. */
	public Grid() {
		for(int y=0; y<9; ++y) for(int x=0; x<9; ++x)
			cells[y*9+x] = new Cell(x, y, Values.all());
		initialise();
		initialised = true;
	}

	/**
	 * Construct a new 9x9 Sudoku Grid from the given 'line', which may
	 * actually be multiple lines.
	 * <p>
	 * The expected format is that produced by the {@link #toString()} method,
	 * which contains <b>three lines</b> of:<ul>
	 * <li>1. clues (given cell values); and
	 * <li>2. OPTIONAL maybes (potential values); and
	 * <li>3. OPTIONAL source (lineNumber#filename).
	 * </ul>
	 * <p>
	 * For example:<br>
	 * <tt><i>8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..<br>
	 * ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357<br>
	 * C:\Users\User\Documents\SodukuPuzzles\Conceptis\TheWorldsHardestSudokuPuzzle.txt<br></i></tt>
	 * <p>
	 * <br>
	 * The <b>first line</b><br>
	 * <tt><i>8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..</i></tt><br>
	 * is the clues (given cell values) and is 81 characters, with a digit 1..9
	 * representing a clue cell value, and the period (.) character (or indeed
	 * ANY other character) representing an empty (unknown value) cell.
	 * <p>
	 * <br>
	 * The <b>OPTIONAL second line</b><br>
	 * <tt><i>,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357</i></tt><br>
	 * contains the cells maybes (potential values). If is a list of 81 comma
	 * separated values (CSV). Each value in the list is a String of all of the
	 * potential values of this cell, just concatenated together. Note that a
	 * clue cells potential values are an empty string (an empty list element).
	 * Alternately, if a maybes string contains a single value then the cell is
	 * a Naked Single, and the reader leaves it as such (ie the Cell is NOT
	 * set to it's only possible value).
	 * <p>
	 * <br>
	 * The <b>OPIONAL third line</b><br>
	 * <tt><i>C:\Users\User\Documents\SodukuPuzzles\Conceptis\TheWorldsHardestSudokuPuzzle.txt</i></tt><br>
	 * contains the source of the puzzle, to enable a partially solved puzzle to
	 * report itself as it's ACTUAL source puzzle. So for example say I discover
	 * an EbulientAardvard hint in line 42 of top1465.d5.mt, so I copy the grid
	 * and paste it into EbulientAardvard_001.txt; so EbulientAardvard_001.txt
	 * continues to report itself as 42#top1465.d5.mt, allowing me to track the
	 * actual source of each test-case; and may also change how the puzzle is
	 * solved, ie whether or not it's "hacked", or atleast used to, before such
	 * over-the-top-cross-country-hackyness fell from grace. Sigh.
	 * <p>
	 * Programmers note this constructor calls {@link #rebuildMaybesAndS__t}
	 * as protection against "foreign agents" who can't be trusted to get the
	 * maybes right; but test-cases need to be able to load a Grid as is, even
	 * if it's bdaly borken, so JUnit test-cases use {@link #load(String) in
	 * place of the constructor, specifically to avoid this protection; but
	 * pretty-much everybody else uses this constructor. The current exceptions
	 * are: undo/redo, drop (of drag-and-drop), and the techie-only logView
	 * feature.
	 * <br>
	 * <hr>
	 * <p>
	 * The constructor can also read my hysterical old text-file format of 9
	 * or 18 lines.<ul>
	 * <li>The first block contains 9 lines of 9 clues (given cells values); and
	 * <li>the second block contains 9 lines of 9 potential values strings.
	 * </ul>
	 *
	 * @param line String the puzzle to load, probably in toString format. */
	public Grid(String line) {
		// splits on \r\n or \r or \n
		this(MyStrings.splitLines(line));
	}

	/**
	 * Constructor: Loads the given puzzle (in lines) into a new Grid.
	 * @param lines
	 */
	private Grid(String[] lines) {
		this(); // run the default constructor first
		load(lines); // just call load rather than re-implement it (badly)
	}

	/** The copy constructor builds an "exact" copy of the 'src' Grid. All cell
	 * values and maybes are copied, as are each regions idxOf, emptyCellCount,
	 * and containsValue arrays.
	 * @param src the source Grid to copy from. */
	public Grid(Grid src) { // 83+90+243+27=443
		int i;
		for ( i=0; i<81; ++i )
			cells[i] = new Cell(src.cells[i]);
		// build regions, crossingBoxs, cells.siblings, cells.notSees, regions.idx, puzzleID
		initialise();
		// copy src.regions: indexesOf, idxs, emptyCellCount, containsValue
		// overwrites the regions.idxs, but ____ it.
		for ( i=0; i<27; ++i )
			regions[i].copyFrom(src.regions[i]);
		if ( src.source != null ) // nullable, apparently
			source = new PuzzleID(src.source); // PuzzleID is immutable.
		initialised = true;
	}

	// Constructor assistant: Does NOT rely on state: cells values or maybes.
	// I just wire up the physical relationships between Grids component parts,
	// like putting each cell in it's regions, and finding the crossings, and
	// the "sibling" relationships, and creating indexes.
	// @param src is non-null only when called by the copy constructor, to copy
	// the regions over instead of initialising all there s__t from scratch.
	private void initialise() {
		// populate the grids regions arrays
		int i, x, y;  Row row;  Col col;
		// create boxs first so that rows and cols can intersectingBoxs them.
		for ( i=0; i<9; ++i )
			regions[i] = boxs[i] = new Box(i);
		for ( i=0; i<9; ++i ) {
			regions[i+ 9] = rows[i] = new Row(i);
			regions[i+18] = cols[i] = new Col(i);
		}
		// populate the row and col crossingBoxes arrays.
		// NB: the boxes must pre-exist for us to wire them up.
		for ( i=0; i<9; ++i ) {
			y = (row=rows[i]).vNum * 3; // index of the first box on this row
			row.crossingBoxs[0] = boxs[y];
			row.crossingBoxs[1] = boxs[++y];
			row.crossingBoxs[2] = boxs[++y];
			x = (col=cols[i]).hNum; // index of the first box in this column
			col.crossingBoxs[0] = boxs[x];
			col.crossingBoxs[1] = boxs[x+=3];
			col.crossingBoxs[2] = boxs[x+=3];
		}
		// populate each cells siblings and notSees array
		for ( Cell cell : cells ) {
			// get the siblings
			cell.siblings = cell.buds.cells(this, new Cell[20]);
			// populate notSees ONCE, instead of negating it a TRILLION times!
			Arrays.fill(cell.sees, false);
			Arrays.fill(cell.notSees, true);
			for ( Cell sib : cell.siblings ) {
				cell.sees[sib.i] = true;
				cell.notSees[sib.i] = false;
			}
		}
		// populate each regions idx
		for ( ARegion r : regions )
			r.idx.addAll(r.cells).lock();
		// grid.puzzleID tells hinters "we've changed puzzles".
		this.puzzleID = RANDOM.nextLong();
	}

	// -------------------------- demi-constructors ---------------------------

	/**
	 * Copy the contents of this grid to the 'dest' Grid.
	 * <p>See also: the copy constructor.
	 * @param dest Grid
	 * @return a reference to 'dest'.
	 */
	public Grid copyTo(Grid dest) {
		int i;
		for ( i=0; i<81; ++i )
			dest.cells[i].copyFrom(cells[i]);
		for ( i=0; i<27; ++i )
			dest.regions[i].copyFrom(regions[i]);
		// NB: no need to rebuild anything in dest, it's all copied over.
		return dest;
	}

	/** Copy this grid to the O/S clipboard in toString() format. */
	public void copyToClipboard() {
		String s = this.toString();
		if (source!=null && source.file!=null)
			s += NL + source;
		IO.copyToClipboard(s);
	}

	// ------------------------------ mutators --------------------------------

	public void clear() {
		for ( Cell cell : cells )
			cell.clear();
	}

	/**
	 * Load this grid from these 1, 2 or 3 lines in the format produced by
	 * the {@link #toString} method.
	 * <p>
	 * Programmers note that the load method is used extensively by the
	 * test-cases because, unlike the "usual" Constructor, it INTENTIONALLY
	 * does <b>NOT</b> call {@link #rebuildMaybesAndS__t} to fix invalid maybes;
	 * thus allowing the test to load a Grid, as is, even if that's
	 * tuterly borken!
	 * <p>
	 * You're not a tooting car mooner are ya Dave? Honk Honk!
	 * <p>
	 * Programmers also note that I still {@link #rebuildAllMyS__t}, because
	 * one really must have ones s__t together in order to locate ones towel,
	 * I just don't ____ with the maybes beforehand.
	 *
	 * @param lines expect 1, 2, or 3 lines: 1=values, 2=maybes, 3=PuzzleID
	 * @return success.
	 */
	public boolean load(String lines) {
		// splits on \r\n or \r or \n
		return load(MyStrings.splitLines(lines));
	}
	public boolean load(List<String> lines) {
		return load(lines.toArray(new String[lines.size()]));
	}

	/**
	 * Load this grid from these 1, 2 or 3 lines in a {@code String[]}.
	 * <p>
	 * load is used by the constructor/s,
	 * and the {@code pasteClipboardTo(Grid grid)} method,
	 * and the {@code KeithsIO.loadFromFile} method;
	 * and ALL the JUnit test-cases.
	 * <p>
	 * Load does NOT rebuild the maybes to replace any missing ones; so we can
	 * load exactly this puzzle, even if it's "broken" somehow. Obviously that
	 * caveats puzzle emptor. You have been warned.
	 * <p>
	 * Example lines:<tt><pre>
	 * 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
	 * ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
	 * 1#C:/Users/User/Documents/SodukuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt
	 * </pre></tt>
	 *
	 * @param lines expect 1, 2, or 3 lines: 1=values, 2=maybes, 3=PuzzleID
	 * @return success.
	 */
	public boolean load(String[] lines) {
		// update the puzzleID to say the puzzle has changed
		this.puzzleID = RANDOM.nextLong();
		// give up immediately if there's no chance of a sucessful load
		if ( lines==null || lines.length==0 )
			return false; // meaning load failed
		try {
			// values: first line contains 81 cell values:
			// 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
			// presume that maybes won't be loaded
			this.isMaybesLoaded = false;
			// set the cells
			setCellValues(lines[0]);
			// ---------------------------------------------------------------
			// from here down if it fails we keep going anyway, coz it's not
			// considered a critical failure: just load as much as possible,
			// and let the user work-out if the maybes need rebuilding.
			// ---------------------------------------------------------------
			// maybes: second line (if any) contains CSV of 81 maybe-values:
			// ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
			boolean wereMaybesLoaded = false; // presume failure
			try {
				String line;
				if ( lines.length>=2 && (line=lines[1])!=null
				  // validate with a regex (slow but worth it).
				  && line.matches("([1-9]*,){80}[1-9]*") ) {
					setMaybes(line.split(",", 81));
					wereMaybesLoaded = true;
				}
				// source: third line (if any) contains the source: a PuzzleID string.
				if ( lines.length>=3 && (line=lines[2])!=null )
					source = PuzzleID.parse(line);
			} catch (Exception ex) {
				StdErr.carp("Noncritical Grid.load", ex);
				wereMaybesLoaded = false; // re-assert failure
			}
			// if the maybes were NOT loaded from file then reinitialise them
			// nb: this can't fail. it only sets 2 existing ints in each never-
			// null-cell.maybes, cells is not null, and no cell is null (here).
			// so if it does fail it's critical. Do NOT eat any exceptions!
			if ( !wereMaybesLoaded )
				for ( Cell cell : cells )
					cell.reinitialiseMaybes(); // fill if empty, else clear
			// if you don't eat you don't s__t, and if you don't s__t you die!
			rebuildAllMyS__t();
			// remember isMaybesLoaded to avoid any future rebuilds.
			isMaybesLoaded = wereMaybesLoaded;
			AHint.hintNumber = 1; // reset the hint number
			return true; // meaning load succeeded
		} catch (Exception ex) {
			StdErr.whinge("Critical Grid.load", ex);
			return false; // meaning load failed
		}
	}

	/**
	 * Restores this grid from the given String, which is the format produced
	 * by {@code toString}, including the optional second line of maybes.
	 * There's no need to restore a grid that's solved. Also note that we only
	 * restore a grid to toString of itself, so there's no need (AFAIK) to
	 * change it's id.
	 * @param s String */
	public void restore(String s) {
		// line 1: 81 cell values ('.' means 0)
		// optional line 2: comma seperated list of 81 maybes-values-sets
		if ( s.length() < 81 )
			throw new IllegalArgumentException("bad s=\""+s+"\"");
		String maybes = s.substring(81).trim();
		if ( maybes.isEmpty() ) { // maybes maybe empty! Go figure, so I did.
			setCellValues(s.substring(0, 81));
			rebuildMaybesAndS__t(); // rebuilds maybes, and all effluvia
		} else {
			setCellValues(s.substring(0, 81));
			setMaybes(maybes.split(",", 81));
			rebuildAllMyS__t(); // just rebuild the effluvia, not maybes
		}
	}

	private boolean setCellValues(String line) {
		char ch;
		final int n = Math.min(line.length(),81);
		for ( int i=0; i<n; ++i ) {
			ch = line.charAt(i);
			if ( ch>='1' && ch<='9' )
				cells[i].value = ch-'0';
			else
				cells[i].value = 0;
		}
		return n == 81;
	}

	private void setMaybes(String[] fields) {
		int i = 0;
		for ( Cell cell : cells )
			cell.maybes.set(fields[i++]);
	}

//KEEP: KRC 2019-09-09: Used to create test cases.
//	boolean saveInToStringFormatTo(File file) {
//		try {
//			IO.save(toString(), file);
//			return true;
//		} catch (IOException ex) {
//			System.out.println(ex.getClass().getSimpleName()+": "+ex.getMessage());
//			return false;
//		}
//	}

	// -------------------------- Rob the rebuilder ---------------------------

	/**
	 * Rebuild everything: reset the maybes and then call rebuildAllMyS__t.
	 */
	public void rebuildMaybesAndS__t() {
		// reset the maybes
		if ( this.initialised ) {
			for ( Cell cell : cells )
				if ( cell.value == 0 ) {
					// fill empty cells maybes and put him back into the indexes
					// I'd like for this to be known as "the Hynamen manuever".
					cell.maybes.fill();
					cell.addMeBackIntoMyRegionsIndexes();
				} else {
					// empty filled cells maybes and remove him from indexes
					cell.maybes.clear();
					cell.removeMeFromMyRegionsIndexesOfAllValues();
				}
		} else {
			// NB: filling empty cells maybes is unnecessary when initialising
			// a grid coz cells and regions are created in a "virgin" state.
			for ( Cell cell : cells )
				if ( cell.value != 0 ) {
					cell.maybes.clear();
					cell.removeMeFromMyRegionsIndexesOfAllValues();
				}
		}
		// and then call rebuildAllMyS__t
		rebuildAllMyS__t();
	}

	/**
	 * The rebuildAllMyS__t method rebuilds all my s__t. Der!
	 * <p>
	 * More seriously, the rebuildAllMyS__t method runs at the tail of load,
	 * restore, and rebuildMaybesAndS__t (ie everybody else) to:<ol>
	 * <li>clear cell values from siblings;
	 * <li>rebuild regions empty-cell counts;
	 * <li>rebuild regions containsValue array;
	 * <li>rebuild regions idxsOf all values.
	 * </ol>
	 * <p>
	 * NB: I only run in the GUI and test-cases, so performance isn't an issue,
	 * so this code is not and should not be optimised for performance. KISS!
	 * <p>
	 * NB: rebuildAllMyS__t respects maybes read from file except if they break
	 * a Sudoku rule: like one instance of value per region, in which case they
	 * are history silently, which is a bit of a worry. But fixing it is ship
	 * loads faster than detecting that it's broken. Programming can be WEIRD
	 * like that!
	 * <p>
	 * NB: rebuilding r.indexesOf all values IS NECESSARY or test-cases fail!
	 * Which is a bit of a worry, because I feel that I should be able to do
	 * it all "minimally", without resorting to The Hammer of Odin.
	 */
	private void rebuildAllMyS__t() {

		// 1. clear cell values from siblings
		for ( Cell cell : this.cells )
			cell.knockCellValueOffSiblingsMaybes();

		// 2. rebuild regions empty-cell counts
		rebuildAllRegionsEmptyCellCounts();

		// 3. regions containsValue array
		rebuildAllRegionsContainsValues();

		// 4. rebuild regions indexesOf all values:
		//    ie set the Indexes region.indexesOf[value]
		//       of each region in regions
		//       of each value in 1..9
		rebuildAllRegionsIndexsOfAllValues();
	}

	/** set the r.emptyCellCount of all regions. */
	public void rebuildAllRegionsEmptyCellCounts() {
		for (ARegion r : regions)
			r.emptyCellCount(); // which sets r.emptyCellCount
	}

	public void rebuildAllRegionsContainsValues() {
		for ( ARegion r : regions )
			r.rebuildContainsValueArray();
	}

//@check commented out (no biggy)
//	/** DEBUG: dump all the containsValue's. */
//	public void dumpContainsValues() {
//		for ( ARegion r : regions )
//			System.out.println(r.id+" "+Frmt.indices(r.containsValue));
//	}

	/**
	 * Rebuild ARegion.indexesOf[value] arrays (not confusing r.idxs[v]!)
	 */
	private void rebuildAllRegionsIndexsOfAllValues() {
		boolean[] rContains;
		Indexes[] rIndexesOf;
		Cell[] rCells;
		final int[] rCellsMaybesBits = new int[9];
		int sv, bits, i;
		for ( ARegion r : this.regions ) { // there are 27 regions
			rContains = r.containsValue;
			rIndexesOf = r.indexesOf;
			rCells = r.cells;
			for ( i=0; i<9; ++i )
				rCellsMaybesBits[i] = rCells[i].maybes.bits;
			// foreach value in 1..9
			for ( int v=1; v<10; ++v ) { // 27*9 = 243
				// nb: pre-rebuildContainsValueArray() to set containsValue!
				if ( rContains[v] )
					rIndexesOf[v].clear();
				else {
					// set Indexes with bits built locally coz it's faster
					// than
					sv = VSHFT[v];
					bits = 0;
					for ( i=0; i<9; ++i )
						if ( (rCellsMaybesBits[i] & sv) != 0 )
							bits |= ISHFT[i];
					rIndexesOf[v].set(bits);
				}
			}
		}
	}

	/** Remove potential values rendered illegal by current cell values. */
	public void cancelMaybes() {
		for ( Cell cell : cells )
			cell.cancelMaybes();
	}

	// ------------------------- the invalidated zone -------------------------

	/**
	 * isInvalidated returns true if this grid is invalid for:<ul>
	 * <li>(a) hasMissingMaybes: a cell has no potential values remaining; or
	 * <li>(b) firstDoubledValue: a box, row, or col has two cells set to the
	 *  same value; or
	 * <li>(c) hasHomelessValues: a box, row, or col has no possible positions
	 *  remaining for a value that has not yet been placed in this region;
	 * </ul>
	 * and set the invalidity and invalidRegion fields.
	 * <p>
	 * Note that the order of these validations is unoptimised. They SHOULD run
	 * by determinism descending, ie the one which breaks most often is run
	 * first, but I guess that they all break about as rare as chickens lips,
	 * so in 99.9+% of cases they all run, so there order is insignificant.
	 * <p>
	 * Note that invalidity and invalidRegion are cleared each time this method
	 * executes, creating an inherent race-condition. This will work so long as
	 * we're single-threaded and the invalidity field is read BEFORE my next
	 * execution, but if you're calling me from multiple threads on a single
	 * Grid instance then you will need a new Invalidity class to hold the two
	 * fields (renamed message and region), and I will return null meaning it's
	 * all good, else a new instance of our new Invalidity class.
	 *
	 * @return true if this grid is invalid, else false.
	 */
	public boolean isInvalidated() {
		invalidity = null;
		invalidRegion = null;
		return hasMissingMaybes()
			|| firstDoubledValue() != 0
			|| hasHomelessValues();
	}

	/** @return Returns true, and set the invalidity message, if an empty cell
	 * has no potential values, else return false. */
	public boolean hasMissingMaybes() {
		for ( Cell cell : cells )
			if ( cell.value==0 && cell.maybes.size==0 ) {
				invalidity = cell.id+" has no potential values remaining";
				return true;
			}
		return false;
	}

	/**
	 * @return the first cell-value that appears multiple times in any region
	 * in this grid, and set invalidity message and invalidRegion, else 0.
	 */
	public int firstDoubledValue() {
		Values seen = new Values(); // the cell values that we've encountered
		for (ARegion region : regions) { // 9 boxs, 9 rows, 9 cols
			for (Cell cell : region.cells) { // 27 * 9 = 243
				if ( cell.value == 0		  // cell is empty
				  || seen.visit(cell.value) ) // OR this value is new to us
					continue;
				// Bugger: value appears more than once in this region
				invalidity = "Multiple "+cell.value+"'s in "+region;
				invalidRegion = region;
				return cell.value;
			}
			seen.clear();
		}
		return 0;
	}

	/**
	 * @return true if any unplaced value has no remaining possible positions
	 * in any region, and set the invalidity message and .
	 */
	public boolean hasHomelessValues() {

		//DEBUG: 2020-10-23 if s__t is ____ed then rebuild s__t before use.
		if ( true ) { // @check true
			// rebuild containsValue array for this and all subsequent hinters.
			rebuildAllRegionsContainsValues(); // safety first!
		} else { // DEBUG: rebuild ALL my s__t (slower but more reliable)
			// rebuild containsValue array for each region
			for ( ARegion r : regions ) {
				Arrays.fill(r.containsValue, false);
				for ( Cell c : r.cells ) {
					if ( c.value > 0 ) {
						r.containsValue[c.value] = true;
					}
				}
			}
			int bits, i;
			Cell[] rcs;
			for ( int v=1; v<10; ++v ) {
				int sv = VSHFT[v];
				for ( ARegion r : regions ) {
					rcs = r.cells; // regions cells array
					bits = 0; // a bitset of this regions places for v
					for ( i=0; i<9; ++i ) {
						if ( (rcs[i].maybes.bits & sv) != 0 ) {
							bits |= ISHFT[i];
						}
					}
					// note the outright set, overwriting existing contents!
					// if no cells in r which maybe v then bits is still 0.
					r.indexesOf[v].set(bits);
				}
			}
		}

		// each region which has no possible position for each value has that
		// value set in one of it's cells, else it's buggered.
		for ( ARegion r : regions ) // 27
			for ( int v=1; v<10; ++v ) // 27*9 = 243
				if ( r.indexesOf[v].size==0 && !r.containsValue[v] ) { // 243*9 = 4,374
					invalidity = "The value "+v+" has no place in "+r.id;
					invalidRegion = r;
					return true; // No room for v in this region
				}
		return false;
	}

	// --------------------------- stateful queries ---------------------------

	/** This is a convenience method for JUnit test cases.
	 * Translating from "E4" to [y][x] is a pain in the ass, and I've
	 * just gotten it wrong, which cost me about 10 minutes, which is
	 * a lot longer than it'll take me to write this method, so here
	 * I am.
	 * @param id to get: "A1".. "I9"
	 * @return the Cell, or you'll get an AIOOBE if your id is funky. */
	public Cell get(String id) {
		return cells[indice(id)];
	}

	/** get is a convenience method for JUnit test cases.
	 * This one is good for short lists or if you've already got an
	 * array of the ID's, other than that I prefer getCSV.
	 * @param ids Arguments array of cell.id strings: "A1", "I9"
	 * @return A Cell array. */
	public Cell[] get(String... ids) {
		final int n = ids.length;
		Cell[] array = cas(n);
		for ( int i=0; i<n; ++i )
			array[i] = get(ids[i]);
		return array;
	}

	/** getCSV is a convenience method for JUnit test cases.
	 * This one is most useful because you can just copy-paste the list
	 * directly from the output.
	 * @param idsCSV comma ", " separated cell.id's:: "A1, I9"
	 * @return A Cell array. */
	public Cell[] getCSV(String idsCSV) {
		return get(idsCSV.split(" *, *"));
	}

	public MyLinkedHashSet<Cell> getCSVSet(String idsCSV) {
		return new MyLinkedHashSet<>(getCSV(idsCSV));
	}

	/**
	 * Is every cell in this grid set?
	 * <p>
	 * DO NOT cache isFull, it causes bugs in recursiveSolve.
	 * @return true if this grid has a non-zero value in every cell, else false
	 */
	public boolean isFull() {
		// NB: grid autofills top-to-bottom-left-to-right so do this check in
		// reverse to find an empty cell (closer to bottom-right) sooner.
		for ( int i=80; i>-1; --i )
			// DEBUGGER: an uninitialised grid is not full
			if ( cells[i]==null || cells[i].value == 0 )
				return false;
		return true;
	}

	/**
	 * Currently only used for logging, to monitor progress.
	 * @return Count the number of cells with value!=0
	 */
	public int countFilledCells() {
		int cnt = 0;
		for ( Cell cell : cells )
			if ( cell.value != 0 )
				++cnt;
		return cnt;
	}

	/**
	 * Currently only used to log "has grid changed" after hint applied.
	 * @return Count the total number of maybes of all cells.
	 */
	public int countMaybes() {
		int cnt = 0;
		for ( Cell cell : cells )
			cnt += cell.maybes.size;
		return cnt;
	}

	/**
	 * Count the number of occurrences of each cell value in this grid.
	 * <p>
	 * Currently only used by the Fisherman to workout whether or not we should
	 * be fishing for each value.
	 * @return int[10] occurrences.
	 */
	public int[] countOccurances() {
		int[] occurances = new int[10];
		for ( Cell cell : cells )
			++occurances[cell.value];
		return occurances;
	}

	// ---------------------------- IS_HACKY stuff ----------------------------

	/**
	 * hackTop1465() is a HACK controller: It returns: is Settings.isHacky and
	 * the grid.source (ie the file we're processing) startsWith "top1465".
	 * @return isHacky && source.filename startsWith top1465
	 */
	public boolean hackTop1465() {
		return Settings.THE.get(Settings.isHacky) && source!=null && source.isTop1465;
	}

	// ---------------------------- testcase crap -----------------------------

	/**
	 * Get an array of regions from CSV (Comma Separated Values) of names.
	 *
	 * @param csv
	 * @return
	 */
	public ARegion[] regions(String csv) {
		String[] rids = csv.split(", *"); // region id's
		ARegion[] result = new ARegion[rids.length];
		int i = 0;
		for ( String rid : rids )
			result[i++] = regions[Regions.index(rid)];
		return result;
	}

	// ------------------------------ BitIdxville -----------------------------
	// Get BitIdx's for the diuf.sudoku.solver.hinters.wing2 package.

	public interface CellFilter {
		boolean accept(Cell c);
	}
	public BitIdx getBitIdx(CellFilter f) {
		return getBitIdx(new BitIdx(this), f);
	}
	public BitIdx getBitIdx(BitIdx result, CellFilter f) {
		for ( Cell c : cells )
			if ( f.accept(c) )
				result.add(c);
		return result;
	}

	// ------ empty cells ------
	private static final CellFilter EMPTY_FILTER = new CellFilter() {
		@Override
		public boolean accept(Cell c) {
			return c.value == 0;
		}
	};
	/**
	 * The "empties" BitIdx is re-read whenever a cell value is set.
	 * @return a cached BitIdx of the empty (value == 0) cells in this grid.
	 */
	public BitIdx getBitIdxEmpties() {
		if ( empties == null ) {
			empties = getBitIdx(EMPTY_FILTER);
		} else if ( emptiesHintNumber!=AHint.hintNumber || emptiesPuzzleID!=puzzleID ) {
			empties.clear();
			getBitIdx(empties, EMPTY_FILTER);
		}
		emptiesHintNumber = AHint.hintNumber;
		emptiesPuzzleID = puzzleID;
		return empties;
	}
	private BitIdx empties; // the empty (value == 0) cells in this grid
	private int emptiesHintNumber;
	private long emptiesPuzzleID;

	// ------ bivalue cells ------
	private static final CellFilter BIVALUE_FILTER = new CellFilter() {
		@Override
		public boolean accept(Cell c) {
			return c.maybes.size == 2;
		}
	};
	/**
	 * Get the cells in the grid with two potential values.
	 * @return cached BitIdx of cells with maybes.size == 2.
	 */
	public BitIdx getBitIdxBivalue() {
		if ( bivs == null ) {
			bivs = getBitIdx(BIVALUE_FILTER);
			bivsHintNumber = AHint.hintNumber;
			bivsPuzzleID = puzzleID;
		} else if ( bivsHintNumber!=AHint.hintNumber || bivsPuzzleID!=puzzleID ) {
			bivs.clear();
			getBitIdx(bivs, BIVALUE_FILTER);
			bivsHintNumber = AHint.hintNumber;
			bivsPuzzleID = puzzleID;
		}
		return bivs; // pre-cached
	}
	private BitIdx bivs; // bivalueCells
	private int bivsHintNumber;
	private long bivsPuzzleID;

	// ------- candidates ------
	/**
	 * Actually get an array of BitIdxs containing the cells which maybe each
	 * potential value 1..9 in this Grid.
	 * @return the CACHED bitIdxs array. Don't modify it's contents!
	 */
	private BitIdx[] getBitIdxsImpl() {
		if ( bitIdxs[1] == null )
			for ( int v=1; v<10; ++v )
				bitIdxs[v] = new BitIdx(this);
		else
			for ( int v=1; v<10; ++v )
				bitIdxs[v].clear();
		for ( Cell c : cells )
			for ( int v : VALUESES[c.maybes.bits] )
				bitIdxs[v].bits.set(c.i);
		bitIdxsHintNumber = AHint.hintNumber;
		bitIdxsPuzzleID = puzzleID;
		return bitIdxs;
	}
	private BitIdx[] bitIdxs = new BitIdx[10];
	private int bitIdxsHintNumber;
	private long bitIdxsPuzzleID;
	/**
	 * Get an array of BitIdxs containing the cells which maybe each potential
	 * value 1..9 in this Grid.
	 * @return the CACHED bitIdxs array. Don't modify it's contents!
	 */
	public BitIdx[] getBitIdxs() {
		if ( bitIdxsHintNumber!=AHint.hintNumber || bitIdxsPuzzleID!=puzzleID )
			getBitIdxsImpl(); // refresh
		return bitIdxs; // pre-cached
	}


	// ---------------- toString and friends ----------------

	/** @return a String representation of the values in this grid, being a
	 * single line of 81 cell values, with 0's (empty cells) represented by a
	 * period character (.).
	 * <p>For example:<pre>
	 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
	 * </pre> */
	public String toShortString() {
		StringBuilder sb = getSB();
		for ( Cell cell : cells )
			if ( cell != null ) // just in case we're still initialising
				sb.append(DIGITS[cell.value]);
		return sb.toString();
	}

	/**
	 * Returns a String representation of this Grid.
	 * <p>
	 * This String is only-sort-of human readable. It's primary focus is
	 * technical. It probably shouldn't be displayed to non-expert users.
	 * <p>
	 * EXAMPLE:<pre>
	 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
	 * ,568,456,2456,,245,247,,24678,468,689,,46,167,,1479,,14678,,3569,4569,,1256,1245,1249,12469,1246,256,156,,24569,12568,,12459,12489,12458,,1567,1567,23456,123568,12458,123457,1248,1234578,25,,,2359,1235,1259,,129,12357,34568,,145679,359,358,589,1345,146,13456,3568,3568,56,,,258,235,,,345,1359,1459,,235,,,124,12345
	 * </pre>
	 * FORMAT:<pre>
	 * Line 1 is 81 cell values, with an an empty cell as a period (.).
	 * Line 2 is 81 potential values as a comma separated values list.
	 * </pre>
	 * NOTES:<ul>
	 * <li>You can get Line 1 only using the {@link #toShortString} method.
	 * <li>If this grid isFull (ie puzzle is complete) then I return Line 1
	 * only, because Line 2 is just 80 commas (useless).
	 * </ul>
	 * @return a String representation of this Grid.
	 */
	@Override
	public String toString() {
		String shortString = toShortString(); // appends to the sb field
		if ( isFull() )
			return shortString;
		// append the maybes to the same SB used by toShortString
		SB.append(NL);
		int i = 0;
		for ( Cell cell : cells ) {
			if ( cell == null )
				return "...initialising...";
			if ( ++i > 1 )
				SB.append(',');
			for ( int v : VALUESES[cell.maybes.bits] )
				SB.append(DIGITS[v]);
		}
		return SB.toString(); // NB: toString creates a new copy of the char-array
	}

	private static StringBuilder getSB() {
		if ( SB == null )
			SB = new StringBuilder(400); // observed 83+298 = 381
		else
			SB.setLength(0); // clear the buffer
		return SB;
	}
	private static StringBuilder SB = null;

	// ---------------- plumbing ----------------

	/** Compare two grids for equality on cell values and maybes. O(81).
	 * @param o Object - for other
	 * @return true if this Grid contains the same values and maybes as 'o',
	 *  else false. */
	@Override
	public boolean equals(Object o) {
		return o!=null && (o instanceof Grid) && equals((Grid)o);
	}
	// WARNING: presumes that Grid is NOT null, for speed!
	public boolean equals(Grid other) {
		for ( int i=0; i<81; ++i )
			if ( cells[i].value       != other.cells[i].value
			  || cells[i].maybes.bits != other.cells[i].maybes.bits )
				return false;
		return true;
	}

	/** Calculates the hashCode value for this Grid, derived from cell values
	 * and maybes, so same hashCodes means probably but not definitely equals,
	 * and two grid that are not equals probably have different hashCodes, but
	 * two copies of the same grid will definitely have the same hashCode.
	 * <p>
	 * NB: This hashCode is reliant on mutable values, which is a bad idea,
	 * but I don't think we'll be using Grid's as Hash-keys, so no problem.
	 * @return the hashCode of this Grid. */
	@Override
	public int hashCode() {
		int result = 0;  Cell cell;
		for ( int i=0; i<81; ++i ) {
			cell = cells[i];
			result = result<<(i/3) ^ cell.value ^ cell.maybes.bits;
		}
		return result;
	}

	// ---------------- Ayla the arrayonator ----------------

	/**
	 * If you don't know what you're doing then don't use this method.
	 * If you need to use this method anyway then good luck.
	 * You MUST finally disarrayonateMaybes.
	 * Used by Aligned2Exclusion and Aligned3Exclusion.
	 * <p>
	 * <b>WARNING:</b> The shiftedMaybes array is <b>NOT</b> maintained!
	 * Create, use, and <b>finally</b> disarrayonateMaybes!
	 * <p>
	 * Note that now, no arrays are created, just wired-up.
	 */
	public void arrayonateShiftedMaybes() {
		for ( Cell cell : cells )
			cell.arrayonateShiftedMaybes();
	}

	// used by Aligned2Exclusion, Aligned3Exclusion, Aligned4Exclusion.
	public void disarrayonateMaybes() {
		for ( Cell cell : cells )
			cell.disarrayonateMaybes();
	}

	// -------------------------------- getIdxs -------------------------------

	/**
	 * The guts of {@link #getIdxs}: Returns an array of Idx's, one per value
	 * 1..9 of the Grid.cells indice of each cell which maybe each value;<br>
	 * so grid.getIdxs()[1] gets you indices of grid cells which maybe 1.
	 *
	 * @return Idx[potential value 1..9]
	 */
	private Idx[] getIdxsImpl() {
		for ( int v=1; v<10; ++v )
			idxs[v].unlock().clear();
		for ( Cell cell : cells )
			if ( cell.maybes.bits != 0 ) // ie cell.value==0, cell is not set
				for ( int v : VALUESES[cell.maybes.bits] )
					idxs[v].add(cell.i);
		for ( int v=1; v<10; ++v )
			idxs[v].lock(); // so it'll throw RTE when you try to change it
		// set the caching-control fields for the getIdxs method
		idxsPuzzleID = puzzleID;
		idxsHintNumber = AHint.hintNumber;
		// return the array (which is cached in a private field)
		return idxs;
	}

	/**
	 * Returns the existing indices if the AHint.hintNumber is the same (ie
	 * this Grid is the same) as last time indices were calculated, otherwise
	 * return getIdxsActual() to calculate the new indices and return them.
	 * @return the indices for the current grid
	 */
	public Idx[] getIdxs() {
		// re-populate idxs when grid has changed, or it's a new puzzle
		if ( idxsHintNumber==AHint.hintNumber && idxsPuzzleID==puzzleID )
			return idxs;
		return getIdxsImpl();
	}
	/** idxs[value] is the indices in this Grid which maybe value 1..9.
	 * You {@code Idx[] idxs = grid.getIdxs()}. The idxs are private and remain
	 * empty until YOU call getIdxs(), unlike most Grid fields. */
	private final IdxL[] idxs = new IdxL[] {
		  null      , new IdxL(), new IdxL(), new IdxL(), new IdxL()
		, new IdxL(), new IdxL(), new IdxL(), new IdxL(), new IdxL()
	};
	private long idxsPuzzleID; // 0 is invalid, to fire first time
	private int idxsHintNumber; // 0 is invalid, to fire first time

	/**
	 * Re-index the values in the regions. Sets each regions.idxsOf array field
	 * for each value 1..9, and returns the underlying grid.idxs array;
	 * which is a bit weird, but it works for me.
	 * <p>
	 * NB: this method exists for performance. It's faster to calculate all
	 * regions indices of all potential values ONCE than it is to calculate it
	 * multiple times in multiple hinters; ie hundreds of times.
	 * <p>
	 * Instituted for ComplexFisherman and FrankenFisherman.
	 */
	public void rebuildAllRegionsIdxsOfAllValues() {
		getIdxs(); // then I read this.idxs directly
		for ( ARegion r : regions )
			for ( int v=1; v<10; ++v )
				r.idxs[v].setAnd(r.idx, this.idxs[v]);
	}

	// -------------------------------- Idx's ---------------------------------
	// NB: My at methods have been replaced by Idx.cells methods.

	/**
	 * Get an Idx of all empty (value == 0) cells in this grid.
	 * @return a CACHED Idx of all empty cells in this grid.
	 */
	public Idx getEmpties() {
		boolean doGet;
		if ( doGet=(emptyCells == null) )
			emptyCells = new IdxL();
		else if ( doGet = ( emptyCellsHintNumber != AHint.hintNumber
				         || emptyCellsPuzzleID != puzzleID ) )
			emptyCells.unlock().clear();
		if ( doGet ) {
			for ( Cell c : cells )
				if ( c.value == 0 )
					emptyCells.add(c.i);
			emptyCellsHintNumber = AHint.hintNumber;
			emptyCellsPuzzleID = puzzleID;
			emptyCells.lock();
		}
		return emptyCells;
	}
	private IdxL emptyCells; // emptyCells
	private int emptyCellsHintNumber;
	private long emptyCellsPuzzleID;

	/**
	 * Returns a new Idx of empty cells matching the given CellFilter.
	 * @param f
	 * @return
	 */
	public Idx getEmptiesWhere(CellFilter f) {
		return getEmpties().where(cells, f);
	}

	public Idx getIdx(Idx result, CellFilter f) {
		for ( Cell c : cells )
			if ( f.accept(c) )
				result.add(c.i);
		return result;
	}

	/**
	 * Get an Idx of cells in this grid with maybes.size == 2.
	 * @return a CACHED Idx of bivalue cells in this grid.
	 */
	public Idx getBivalueCells() {
		if ( bivis == null ) {
			bivis = ((IdxL)getIdx(new IdxL(), BIVALUE_FILTER)).lock();
			bivisHintNumber = AHint.hintNumber;
			bivisPuzzleID = puzzleID;
		} else if ( bivisHintNumber!=AHint.hintNumber || bivisPuzzleID!=puzzleID ) {
			((IdxL)getIdx(bivis.unlock().clear(), BIVALUE_FILTER)).lock();
			bivisHintNumber = AHint.hintNumber;
			bivisPuzzleID = puzzleID;
		}
		return bivis; // pre-cached
	}
	private IdxL bivis; // bivalueCells
	private int bivisHintNumber;
	private long bivisPuzzleID;

	/**
	 * commonBuddiesNew: a new Idx of buds common to all given cells.
	 * <p>
	 * Performance: I'm a tad slow because I use an iterator, which is the cost
	 * of taking an Iterable over a List, so it'll work for a Set also, but not
	 * an Cell[] array, which annoys me. Arrays should be Iterable.
	 *
	 * @param cells to get the common buds of
	 * @return a new Idx of the common buds
	 */
	public static Idx cmnBudsNew(Iterable<Cell> cells) {
		Iterator<Cell> it = cells.iterator();
		// it throws an NPE if cells is empty... let it!
		final Idx result = new Idx(it.next().buds);
		while ( it.hasNext() )
			result.and(it.next().buds);
		return result;
	}

	/**
	 * Returns a new array of the value of each cell in this Grid.
	 * <p>
	 * Note that this method is only called on solved (solution) Grids.
	 *
	 * @return a new int[81] of the 81 cell values
	 */
	public int[] toValuesArray() {
		int[] result = new int[81];
		for ( int i=0; i<81; ++i  )
			result[i] = cells[i].value;
		return result;
	}

	/**
	 * Get a pots of each cell in this Grid which maybe v. This is used by the
	 * SudokuGridPanel when the user holds-down a mouse button over a rowLegend
	 * to highlight candidates of that number. Ideally we'd cache the resulting
	 * pots, but that'd require a modificationCount which I've thus far refused
	 * to introduce on the grounds that simpler is better; and dis only effects
	 * da GUI, where Swing creates thousands of ephemeral objects anyways.
	 * @param value
	 * @return a new Pots containing the cells which maybe v in this Grid.
	 */
	public Pots getCandidatePots(int value) {
		Pots result = new Pots(64, 1.0F);
		// WARNING: This is efficient, but fragile. No operations are performed
		// on theValues, so all cells in result Pots share a single instance of
		// Values; which'll go bad if you modify vals later. So, if you're here
		// bug-hunting then try creating a new Values for each cell. Sols.
		final Values theValues = new Values(value);
		// theValueBit is value as a left-shifted bitset. Note that it's always
		// a single bit, ie 1 << (value - 1)
		final int theValueBit = VSHFT[value];
		for ( Cell cell : cells )
			if ( (cell.maybes.bits & theValueBit) != 0 )
				result.put(cell, theValues);
		return result;
	}

	// Logical Solver needs to keep track of whether or it's prepared to solve
	// this grid, and the easiest way to do that is stick an attribute on the
	// grid which presumes that it hasn't been prepared yet.
	public boolean isPrepared() {
		return isPrepared;
	}
	public void setPrepared(boolean isPrepared) {
		this.isPrepared = isPrepared;
	}

	// ---------------------- effing around with Regions ----------------------

	/**
	 * Return the ARegion of regionTypeIndex that's common to all cells,
	 * else null.
	 * <p>
	 * RANT: If cells is null then you're here because of the NPE which is my
	 * callers fault, not mine; so change my caller to ensure cells isn't null
	 * before you call me. If you absolutely need a null check then wrap this
	 * method to provide it. Or, let's face it, you can just do it all here and
	 * just delete this nice little rant. It's your code!
	 *
	 * @param cells {@code Iterable<Cell>} which I presume to contain at least
	 *  two cells.
	 * @param regionTypeIndex Grid.BOX, Grid.ROW, or Grid.COL (ie 0..2).
	 *  If it isn't one of these values I'll throw an AIOOBE
	 * @return the common ARegion, else null.<br>
	 *  If cells is null then I throw a NullPointerException.<br>
	 *  If cells is empty then I always return null.<br>
	 *  If cells contains only one cell then I just return it's
	 *  regions[regionTypeIndex].
	 */
	public ARegion commonRegion(Iterable<Cell> cells, int regionTypeIndex) {
		if(cells == null) throw new NullPointerException("cells is null!");
		assert hasTwo(cells) : "cells contains less than 2 cells!";
		ARegion commonRegion=null, cellsRegion;
		for ( Cell cell : cells ) {
			cellsRegion = cell.regions[regionTypeIndex];
			if ( commonRegion == null ) // the first cell
				commonRegion = cellsRegion;
			else if ( cellsRegion != commonRegion )
				return null; // not all cells share a region of this type
		}
		return commonRegion; // will still be null if cells is empty
	}

	// Method coz you can't define a variable in an assert, which sux!
	private static final boolean hasTwo(Iterable<?> cells) {
		Iterator<?>it=cells.iterator();
		return it.hasNext()
			&& it.next()!=null
			&& it.hasNext();
	}

	/**
	 * Return the regions (expect 1 or 2 of them) common to all these cells.
	 * Note that cells should be a Set of at least 2 cells. If there's only
	 * one then "common" regions is nonsensical. The result is indeterminate
	 * if cells contains duplicates, because again that's just nonsensical.
	 * @param cells
	 * @param result {@code ArrayList<ARegion>} is not cleared first.
	 * @return a {@code ArrayList<ARegion>} may be empty, but never null.
	 */
	public ArrayList<ARegion> commonRegions(Collection<Cell> cells, ArrayList<ARegion> result) {
		Iterator<Cell> it = cells.iterator();
		// for first cell in cells
		Cell cell = it.next();
		int box = cell.boxId;
		int row = cell.y;
		int col = cell.x;
		// foreach subsequent cell in cells
		while ( it.hasNext() ) {
			cell = it.next();
			if(box!=cell.boxId) box=-1;
			if(row!=cell.y) row=-1;
			if(col!=cell.x) col=-1;
		}
		// re-populate the result
//		result.clear();
		if(box > -1) result.add(boxs[box]);
		if(row > -1) result.add(rows[row]);
		if(col > -1) result.add(cols[col]);
		return result;
	}

	/**
	 * Returns the other ARegion that is common to all cells. Cells is expected
	 * to contain two or three cells, which CAN have one "other" common region.
	 * <p>
	 * A List of less than two Cells always returns null coz "common" makes no
	 * sense.
	 * <p>
	 * A list of four-or-more cells always returns null coz four cells can have
	 * only one common region, so an "other" common region cannot exist.
	 *
	 * @param cells
	 * @param region
	 * @return the other ARegion common to all cells.
	 */
	public ARegion otherCommonRegion(List<Cell> cells, ARegion region) {
		final int n = cells.size();
		if ( n<2 || n>6 )
			return null; // You shouldn't have called me!
		List<ARegion> cmnRgns = commonRegions(cells, new ArrayList<>(2));
		if ( cmnRgns.size() == 2 ) {
			// find the other common region
			ARegion r = cmnRgns.get(0);
			if ( r == region )
				return cmnRgns.get(1);
			else
				return r;
		}
		return null;
	}

	public ARegion[] getRegions(int regionTypeIndex) {
		switch (regionTypeIndex) {
		case BOX: return boxs;
		case ROW: return rows;
		case COL: return cols;
		}
		throw new IllegalArgumentException("Unkown regionTypeIndex="+regionTypeIndex);
	}

	// ============================ the Cell class ===========================

	/**
	 * Compares Cells by id ASCENDING.
	 * For test-cases only.
	 */
	public final static Comparator<Cell> BY_ID = new Comparator<Cell>() {
		@Override
		public int compare(Cell a, Cell b) {
			return a.id.compareTo(b.id);
		}
	};

	/**
	 * The public inner Cell class represents one of the 81 cells in a Sudoku
	 * Grid. It's reason for being is to remember stuff, not really do stuff.
	 * <p>A Cell holds:<ul>
	 * <li>i is my index in the Grid.cells array
	 * <li>id is my {@code $columnChar$rowNumber} A1..I9
	 * <li>value: 1..9 or 0 if the cell is empty
	 * <li>maybes: (aka potential values) A final but mutable Values
	 * <li>y is my (vertical) coordinate in the Grid.matrix
	 * <li>x is my (horizontal) coordinate in the Grid.matrix
	 * <li>boxId and b: I am {@code grid.boxs[boxId].cells[b]}
	 * <li>b is my index in my box.cells array = {@code (y%3*3) + (x%3)}
	 * <li>idxdex is the index of my element in the idx array.<br>
	 *  so: {@code idxdex = i/7}<br>
	 *  see: shft field (below) for more info.
	 * <li>shft The left-shifted bitset value of (my i modulo 7) to fit in my
	 *  idx array element.<br>
	 *  so: {@code shft = 1<<(i%7);}<br>
	 *  eg: {@code idx[cell.idxdex] |= cell.shft;} // add cell to idx fast<br>
	 *  ie: {@code idx[cell.i/7] |= 1<<(cell.i%7); // add cell to idx}
	 * <li>regions is my Box, my Row, and my Col
	 * <li>hashCode is {@code = y<<4 + x} so I can be a HashMap key
	 * <li>skip is used to prevent useless/erroneous assumptions in Chainer
	 * <li>{@code public int[] maybesArray;} is the potential values of this
	 * cell. Populated by arrayonateShiftedMaybes() and weweased with
	 * disarrayonateMaybes(). The array is <b>NOT maintained</b>!
	 * <li>{@code public Map<String,Object> properties = null;} gives you a
	 * way to add any additional properties you require. Create a new
	 * {@code HashMap<String,Object>} (or whatever) and attach it here.
	 * Also please document it right here!
	 * </ul>
	 * So my primary role is to "remember stuff", and my secondary role is to
	 * help autosolve Sudoku puzzles when {@code isAutosoving = true}.
	 * I also maintain my Regions {@code idxsOf[value]} to expedite many of
	 * the hinters. I've accrued a lot of methods over time, especially for a
	 * class that "doesn't really do stuff". Don't panic.
	 */
	public final class Cell implements Comparable<Cell> {

		/** i (short for index) is my indice in the Grid.cells array<br>
		 * so: {@code y*9 + x} */
		public final int i;

		/** id is my {@code $columnChar$rowNumber} A1..I9 */
		public final String id;

		/** The "value" of this cell 1..9, 0 means this cell is "empty" */
		public int value;

		/** The potential values of this cell. Named for "A1 may be 2" because
		 * potentialValues is too bloody long to type a thousand times. */
		public final Values maybes;

		/** boxId and b: I am {@code grid.boxs[boxId].cells[b]}. <br>
		 * so: {@code boxId = (y/3*3) + (x/3)} <br>
		 * nb: The grid.boxs array is coincident with the first 9 elements in
		 * the grid.regions array. The boxs array is of type Grid.Box, where-as
		 * the regions array is of Grid.ARegion; and Box has public fields which
		 * aren't in ARegion because they're not relevant to Row or Col. */
		public final int boxId;

		/** boxId and b: I am {@code grid.boxs[boxId].cells[b]}. <br>
		 * so: {@code b = (y%3*3) + (x%3)} */
		public final int b;

		/** x is my horizontal (second) index in the Grid.matrix. <br>
		 * also my index in my row.cells array. <br>
		 * x,y verses row,col where x=col and row=y: There's only 2 possible
		 * orders, and we habitually use both of them, ergo just shoot me!
		 * The only solution is to rehabituate ourselves to col,row. */
		public final int x;

		/** y is my vertical (first) index in the Grid.matrix. <br>
		 * also my index in my col.cells array. <br>
		 * x,y verses row,col where x=col and row=y: There's only 2 possible
		 * orders, and we habitually use both of them, ergo just shoot me!
		 * The only solution is to rehabituate ourselves to col,row. */
		public final int y;

		/** indexInRegion is my index in my regions cells array (not to be
		 * confused with my indice in my regions idx, which I've just done). */
		public final int[] indexIn = new int[3];

		/** idxdex is the index of my element in the idx array.<pre>
		 * so: {@code idxdex = i/27}
		 * </pre>
		 * see: shft field for more info.
		 */
		public final int idxdex; // 0..2

		/** The shft to add this cell to a idx. The left-shifted bitset-value
		 * of i (my Grid.cells array index) within my idx array element.<pre>
		 * so: {@code idxshft = 1<<(i%27);}
		 * was: {@code idx[cell.i/27] |= 1<<(cell.i%27); // add cell to idx}
		 * now: {@code idx[cell.idxdex] |= cell.idxshft; // add cell faster}
		 * </pre>
		 * <p>
		 * idxdex and idxshift used only by LinkedMatrixCellSet.idx() that's
		 * hammered by aligned exclusion. Saved 28 secs in A1234E per top1465.
		 */
		public final int idxshft;

		/** the 3 regions which contain this Cell: 0=box, 1=row, 2=col. */
		public final ARegion[] regions = new ARegion[3]; // set by Grid constructor

		/** hashCode is a unique identifying integer = y&lt;&lt;4+x. */
		public final int hashCode;

		/** Used to prevent useless assumptions being made in chaining. */
		public boolean skip;

		/** Populated by arrayonateShiftedMaybes() with the left-shifted-bitset-
		 * values of the potential values of this cell. See Values re bitsets.*/
		public int[] shiftedMaybes;

		/**
		 * notSees boolean[]: Does this Cell NOT see the given Cell?
		 * Coincident with Grid.cells.
		 * <p>
		 * RE the not: In Aligned*Exclusion we act if this cell is NOT a
		 * sibling of other cell; so rather than negate repeatedly with
		 * {@code boolean ns01 = !c0.sees[i1]} we {@code c0.notSees[i1];}
		 * just to save a poofteenth of a nanosecond per flip, which add-up
		 * over top1465. Doing ANYthing a quadrillion times takes a while.
		 * So we've just rubbed another one out. Sigh.
		 */
		public final boolean[] notSees = new boolean[81];

		/**
		 * notSees boolean[]: Does this Cell NOT see the given Cell?
		 * Coincident with Grid.cells.
		 * <p>
		 * RE the not: In Aligned*Exclusion we act if this cell is NOT a
		 * sibling of other cell; so rather than negate repeatedly with
		 * {@code boolean ns01 = !c0.sees[i1]} we {@code c0.notSees[i1];}
		 * just to save a poofteenth of a nanosecond per flip, which add-up
		 * over top1465. Doing ANYthing a quadrillion times takes a while.
		 * So we've just rubbed another one out. Sigh.
		 */
		public final boolean[] sees = new boolean[81];

		/**
		 * The indices of the 20 cells in same box, row, and col as this Cell,
		 * excepting this cell itself. Note that {@link Grid#BUDDIES} contains
		 * the same Idx's, to save you getting the cell to get it's buds. */
		public final IdxL buds; // set in constructor

		/** An array of my set of 20 sibling cells. */
		public Cell[] siblings;

		/** {@code public Map<String,Object> properties = null;} gives you a
		 * way to add any additional properties you require.<br>
		 * Create a {@code new HashMap<String,Object>(8, 0.75F)} (or whatever)
		 * and attach it here.<br>
		 * Also please document your properties right here! */
		public Map<String,Object> properties = null;

		// These are used privately to save deindexing the regions array
		// ?trillions? of times.
		public Box box;  public Row row;  public Col col;

		/**
		 * Construct a new Cell in this grid.
		 * <b>NOTE</b> the parameters are "logical" x, y (not "physical" y, x).
		 * <b>NOTE</b> each cell has an intrinsic reference to its containing
		 * grid, whether you like it or not, so a reference to a Cell holds the
		 * whole Grid, so clean-up after yourself, or you'll OOME-out. Sigh.
		 *
		 * @param x is my horizontal coordinate (0=left, 8=right),
		 *  ie my 0-based col index in the Grid.matrix
		 * @param y is my vertical coordinate (0=top, 8=bottom),
		 *  ie my 0-based row index in the Grid.matrix
		 * @param maybes the potential values of this cell. The default is
		 *  Values.all(), ie "123456789". If you pass null it will throw a
		 *  NullPointerException eventually, so pass me Values.all() or
		 *  Values.none(), never null. Pretty obviously a distinct instance of
		 *  Values is required for each cell; they must operate independently
		 *  of each other.
		 */
		public Cell(int x, int y, Values maybes) {
			this.id = CELL_IDS[this.i = y*9 + x]; // set id first for toString
			//value is 0 by default, and may be set by my calling constructor
			assert maybes != null;
			this.boxId = (y / 3 * 3) + (x / 3);
			this.indexIn[Grid.BOX] = this.b = (y % 3 * 3) + (x % 3);
			this.indexIn[Grid.ROW] = this.x = x; // index in, not of, ya putz!
			this.indexIn[Grid.COL] = this.y = y;

			// super-charge LinkedMatrixCellSet.idx() with:
			//     a[n.cell.idxdex] |= n.cell.shft; // add n.cell.i to idx
			// coz it's faster than calculating idxdex and shft repeatedly
			this.idxdex = i/BITS_PER_ELEMENT;
			this.idxshft = 1<<(i%BITS_PER_ELEMENT);

			this.hashCode = Hash.LSH4[y] ^ x; // see also Ass.hashCode()
			this.buds = Grid.BUDDIES[i];
			// always set maybes last for toString
			this.maybes = maybes;
		}

		/**
		 * Construct a new Cell having this value.
		 *
		 * @param i the indice of this cell in Grid.cells
		 * @param value the value of this cell. 0 for empty. 1..9 for filled.
		 */
		public Cell(int i, int value) {
			this(i/9, i%9, value==0 ? Values.all() : Values.none());
			this.value = value;
		}

		/** Copy Constructor: Constructs a new Cell that is a "clone" of src.
		 * @param src */
		public Cell(Cell src) {
			this(src.x, src.y, new Values(src.maybes));
			this.value = src.value;
		}

		private void clear() {
			value = 0;
			maybes.fill();
		}

		/** Copy the cell-value and maybes from the 'src' cell.
		 * @param src the cell to copy. */
		public void copyFrom(Cell src) {
			value = src.value;
			maybes.copyFrom(src.maybes);
		}

		/** In the Grid.load method, having set all values, we clean-up maybes.
		 * This is the first step in doing so: if the cell is empty we "fill"
		 * his maybes, otherwise we "clear" them, which is much faster than
		 * adding/removing individual values.
		 * <p><b>NOTE</b>: filling empty cells is unnecessary for new grids;
		 * but it is necessary after we've just loaded into a previously loved
		 * grid, so don't "efficiency" it out again, ya putz! */
		private void reinitialiseMaybes() {
			if ( value == 0 )
				maybes.fill();
			else
				maybes.clear();
		}

		/** Set the value of this cell, and remove that value from the potential
		 * values of each of my sibling cells; and remove this position from
		 * each of my three regions possible positions of my potential values.
		 * @param value to set.
		 * @param formerValue just pass me the cell.value and don't worry, this
		 *  won't hurt!
		 * @param isAutosolving should subsequent naked and hidden singles be
		 *  sought and set?
		 * @param sb nullable StringBuilder to which (when not null) I append an
		 * "and therefore list": a list of any naked and hidden singles that are
		 * set as a subsequence of setting the given cell value.
		 * Currently not-null only in HintsApplicumulator.
		 * @return the number of cells actually set. 1 normally, or 0 if this
		 *  the given value is the same as this cells value. */
		public int set(int value, int formerValue, boolean isAutosolving
				, StringBuilder sb) {
			if ( value==this.value && maybes.size==0 ) {
				// nb: clean-up any crap left behind by ____Knows, which causes
				// NakedSingle to go into an endless loop only when used within
				// the HintsApplicumulator, which is bloody FREAKY! IDFKA! FMS!
				if ( maybes.bits != 0 ) {
					removeMeFromMyRegionsIndexesOfBits(maybes.bits);
					maybes.clear();
				}
				return 0; // I've just been been set twice.
			}
			this.value = value;
			removeMeFromMyRegionsIndexesOfBits(maybes.bits);
			maybes.clear();
			if ( value == 0 ) { // the user cleared a cell in the GUI
				assert !isAutosolving; // A certain nosey hound dog just died.
				addMeBackIntoMySiblingsMaybes(formerValue);
				restoreMyMaybes();
			} else
				removeFromMySiblingsMaybes(value);
			// update the emptyCellCounts of the 3 regions which contain me.
			if ( value == 0)
				rebuildAllRegionsEmptyCellCounts();
			else // only happens in the GUI
				decrementMyRegionsEmptyCellCounts();
			// Chains: I am NOT an unapplied naked/hidden single
			skip = false;
			// we're done if we're not autosolving
			if ( !isAutosolving )
				return 1; // 1 for me only

			// Autosolve: Seek and set any consequent singles.
			Cell sib;
			int v, first;
			int count = 1; // 1 for me
			for ( ARegion r : regions ) {
				// look for any subsequent naked singles
				for ( int i=0; i<9; ++i ) {
					if ( r.cells[i].maybes.size == 1 ) {
						if ( (v=(sib=r.cells[i]).maybes.first()) < 1 ) { // invalid
							sib.maybes.clear(); // I shot my sibling, but I didno shoot no diputy!
							continue;			// I say, hay nan, nar nar, nung nung nung.
						}
						if ( sb != null )
							sb.append(", ").append(sib.id).append("=").append(v);
						count += sib.set(v, 0, true, sb);
					}
				}
				// look for any subsequent hidden singles
				for ( v=1; v<10; ++v ) {
					if ( r.indexesOf[v].size == 1 ) {
						if ( (first=r.indexesOf[v].first()) < 0 ) { // invalid
							r.indexesOf[v].clear(); // I shot my sibling, but I didno shoot no diputy!
							continue;				// He say, hey man, nar nar, argh argh argh.
						}
						if ( (sib=r.cells[first]) != this ) { // oops!
							if ( sb != null )
								sb.append(", ").append(sib.id).append("=").append(v);
							count += sib.set(v, 0, true, sb);
						}
					}
				}
			}
			return count;
		}

//		// DEBUG: dump this cells three regions indexesOf ALL values to stdout
//		void dumpRIVs() {
//			StringBuilder sb = new StringBuilder(128);
//			String s;
//			for ( ARegion r : Cell.this.regions ) {
//				sb.setLength(0);
//				sb.append(r.id);
//				for ( int v=1; v<10; ++v ) {
//					s = r.indexesOf[v].toString();
//					s += "         ".substring(0, 10-s.length());
//					sb.append("\t").append(v).append("=").append(s);
//				}
//				System.out.println(sb);
//			}
//		}

		private void restoreMyMaybes() {
			for ( int v=1; v<10; ++v )
				if ( !canSeeValue(v) )
					canSoBe(v);
		}

		/**
		 * This set method is used by the SudokuExplainer (the JFrames engine)
		 * to set the cells value, so we need to cater for user changing there
		 * mind and removing a value from a Cell.
		 * @param value the value they just set, may be 0 meaning empty.
		 * @param formerValue the value which the cell held previously.
		 * @return the number of cells set (always 1 here).
		 */
		public int setManually(int value, int formerValue) {
			set(value, formerValue, Grid.NO_AUTOSOLVE, null);
			return 1;
		}

		/**
		 * Remove my cell index from the {box,row,col} idxsOf all values.
		 * <p>
		 * NB: Remove all values coz it's nearly as quick as testing
		 * {@code maybes.get(v)}
		 * <p>
		 * Currently used only in test-cases, but still required.
		 */
		public void removeMyMaybesFromMyRegionsIndexes() {
			removeMeFromMyRegionsIndexesOfAllValues();
// Stick this assert back in if you've got ANY indexesOf trouble.
//			assert !iAmInMyRegionsIndexesOf(Values.all());
		}

		// KEEP: Used in test-cases, so don't comment-out!
		// iAmInMyRegionsIndexesOf is intended to make a "dodgy update" fail,
		// so that it happens when the problem is created, instead of waiting
		// for the inderminate future time when we "trip-over" the problem.
		// It was later changed to not fail, just log the problem, so it's now
		// up to the caller if he wants to throw if/when it's all gone bad.
		// Currently used in the above assert, and by test-cases as required.
		boolean iAmInMyRegionsIndexesOf(Values maybes) {
			final PrintStream out = System.out;
			int i = -1;
			boolean result = false;
			for ( int v : VALUESES[maybes.bits] ) {
				if ( ( box.indexesOf[v].contains(b) && (i=0)==i )
				  && ( row.indexesOf[v].contains(x) && (i=1)==i )
				  && ( col.indexesOf[v].contains(y) && (i=2)==i )
				) {
					result = true;
					if ( Log.MODE == Log.MODE_200 ) {
						String s = id+" iAmInMyRegionsIndexesOf("+maybes+"):"
								+ " %s idxsOf[%d]=%s contains %d"+NL;
						switch(i) {
						case 0: out.format(s, box.id, v, box.indexesOf[v], b); break;
						case 1: out.format(s, row.id, v, row.indexesOf[v], x); break;
						case 2: out.format(s, col.id, v, col.indexesOf[v], y); break;
						default: out.println("iAmInMyRegionsIndexesOf: i="+i);
						}
					} else
						break; // it won't get any more true
				}
			}
			return result;
		}

		// clear 'values' from the idxsOf arrays of this cells 3 regions.
		// Currently only used by test-cases, but it's required there.
		void removeMeFromMyRegionsIndexesOf(Values values) {
			switch (values.size) {
				case 9:
					removeMeFromMyRegionsIndexesOfAllValues();
					break;
				case 1:
					// this just means values.first(); but it's a tad faster
					final int v = FIRST_VALUE[values.bits];
					removeMeFromMyRegionsIndexesOfValue(v);
					break;
				default:
					removeMeFromMyRegionsIndexesOfBits(values.bits);
			}
		}

		public void removeMeFromMyRegionsIndexesOfAllValues() {
			for ( int v=1; v<10; ++v ) {
				box.indexesOf[v].remove(b);
				row.indexesOf[v].remove(x);
				col.indexesOf[v].remove(y);
			}
		}

		private void removeMeFromMyRegionsIndexesOfValue(int v) {
			box.indexesOf[v].remove(b);
			row.indexesOf[v].remove(x);
			col.indexesOf[v].remove(y);
		}

		// clear the idxsOf[values 'bits'] of this cells box, row, and col.
		private void removeMeFromMyRegionsIndexesOfBits(final int valuesBits) {
			for ( int v : VALUESES[valuesBits] ) {
				box.indexesOf[v].remove(b);
				row.indexesOf[v].remove(x);
				col.indexesOf[v].remove(y);
			}
		}

		/** Adds my index back into my 3 regions idxsOf[value]. */
		private void addMeBackIntoMyRegionsIndexesOfValue(int v) {
			box.indexesOf[v].add(b);
			row.indexesOf[v].add(x);
			col.indexesOf[v].add(y);
		}

		/** Add my index back into my 3 regions idxsOfs. */
		public void addMeBackIntoMyRegionsIndexes() {
			for ( int v=1; v<10; ++v )
				addMeBackIntoMyRegionsIndexesOfValue(v);
		}

		/** Called by the Grids cancelMaybes() method which is part of
		 * SudokuExplainers candidateTyped), ie only in the GUI. */
		private void cancelMaybes() {
			if ( value != 0 ) {
				maybes.clear();
				removeFromMySiblingsMaybes(value);
			}
		}

		/** Called by rebuildAllMyS__t because it's cleaner, and I'm only used
		 * in the GUI, so performance is a non-issue. */
		private void knockCellValueOffSiblingsMaybes() {
			if ( value != 0 )
				removeFromMySiblingsMaybes(value);
		}

		/** remove this Cells value from the maybes of each of my 20 siblings,
		 * and also update each siblings regions indexesOf[value].
		 * @param theValue */
		public void removeFromMySiblingsMaybes(int theValue) {
			if ( theValue == 0 ) // actually happened.
				throw new UnsolvableException("WTF: Desibonate 0?");
			final int sv = VSHFT[theValue];
			for ( Cell sib : siblings )
				if ( (sib.maybes.bits & sv) != 0 )
					sib.canNotBe(theValue);
		}

		/** add this given value back into the maybes of each of my 20 siblings,
		 * and also update each siblings regions idxsOf[value].
		 * Only called by Cell.set when clearing a cell in the GUI.
		 * @param theValue */
		public void addMeBackIntoMySiblingsMaybes(int theValue) {
			if ( theValue == 0 ) // this has actually happened.
				return;
			for ( ARegion region : regions )
				for ( Cell sib : region.cells )
					if ( sib.value == 0 ) // includes this cell, which is now unset.
						sib.politelySuggestThatYouPossiblyCouldBe(theValue);
		}

		/** Decrements the emptyCellCounts of the 3 regions which contain me. */
		public void decrementMyRegionsEmptyCellCounts() {
			--box.emptyCellCount;
			--row.emptyCellCount;
			--col.emptyCellCount;
		}

		/**
		 * Remove the given value from the potential values of this cell,
		 * and also remove me from each of my regions idxsOf[theValueToRemove].
		 * <p>This method <b>does nothing</b> if this Cell is set, or if this
		 *  Cell already cannot-be theValue. This is a efficiency/convenience
		 *  for the removeMyValueFromMySiblingsMaybes() to just invoke me for
		 *  each unset sibling, allowing me workout weather I have stuff to do.
		 * <p>2019-09-02 for efficiency: assert cell maybe before it canNotBe!
		 * @param theValueToRemove between 1 and 9 inclusive.
		 *  <p><b>NOTE:</b> called theValueToRemove to try very hard to really
		 *  differentiate it from the canNotBeBits parameter... which I've just
		 *  buggered-up on. I want my mommy!
		 * @return the number of maybes removed from this cell, as in 1 normally
		 *  or 0 if you're dumb enough to call me when I'm not needed.
		 */
		public int canNotBe(int theValueToRemove) {
			if ( (maybes.bits & VSHFT[theValueToRemove]) == 0 )
				return 0; // do nothing
			removeMeFromMyRegionsIndexesOfValue(theValueToRemove);
			if ( maybes.remove(theValueToRemove) == 0 ) // clear returns new size
				throw new UnsolvableException(id + " no maybes remain");
			return 1; // number of maybes removed
		}

		/**
		 * The canNotBeBits method removes the pinkBits from the maybes of this
		 * cell (me); and if nakedSingles is not null and my dispinkectomy
		 * leaves me naked then I add myself to the nakedSingles queue, from
		 * which AHint.apply retrieves me in order to set my value. If my
		 * dispinkectomy leaves me with no potential values then I always throw
		 * an UnsolvableException.
		 * <p>
		 * This method is called only by {@code AHint.apply} with these
		 * <b>PRECONDITIONS:</b><ul>
		 * <li>the given pinkBits is not null or empty, and
		 * <li>this.maybes is not empty, and
		 * <li>there's at least 1 intersection between the two.
		 * </ul>
		 * <p>
		 * ASIDE: I'm standing behind Donald Trump and I'm holding an angle
		 * grinder. Can you tell what I'm thinking? Yeah, like a bastard.
		 *
		 * @param pinkBits the Values bits which I can not be?
		 * @param nakedSingles is a {@code Deque<Cell>} when isAutosolving, to
		 * which I add this cell if removing pinkBits makes me a naked single
		 * (ie this cell has just one remaining potential value); else null.
		 * @return the number of bits actually removed, so we can keep track
		 * of numMaybesRemoved in the hint.apply method.
		 * @throws UnsolvableException if removing valuesBits leaves this cell
		 * with 0 maybes. This happens pretty routinely in recursiveSolve, but
		 * it should be rare otherwise. All instances outside of recursion
		 * should be hunted down and at least explained, if not fixed.
		 */
		public int canNotBeBits(int pinkBits, Deque<Cell> nakedSingles) {
			int preBits = maybes.bits; // just for the exception message
			removeMeFromMyRegionsIndexesOfBits(pinkBits);
			// this must be calculated BEFORE we remove the pinkBits
			int numRmvd = VSIZE[maybes.bits & pinkBits];
			// remove the pinkBits from the maybes of this cell, and
			// switch on the number of maybes now remaining in this cell
			switch ( maybes.removeBits(pinkBits) ) { // returns the new size
			case 0: // I am rooted, so we fail early
				throw new UnsolvableException("No maybes: "+id+":"
					+Values.toString(preBits)+" - "+Values.toString(pinkBits));
			case 1: // I am a naked single, to be set by AHint.apply
				if ( nakedSingles != null )
					nakedSingles.add(this);
			}
			return numRmvd;
		}

		// only called by the Grids addMeBackIntoMySiblingsMaybes method, which
		// is a bit of a special case. It's only used in the GUI, obviously, so
		// performance isn't an issue, so we just rebuilding all this cells
		// three regions containsValue arrays.
		private void politelySuggestThatYouPossiblyCouldBe(int theValue) {
			rebuildAllRegionsContainsValues();
			boolean iToldHimWeHaveAlreadyGotOne = false;
			for ( ARegion r : regions )
				if ( r.containsValue[theValue] ) {
					iToldHimWeHaveAlreadyGotOne = true;
					break;
				}
			if ( !iToldHimWeHaveAlreadyGotOne )
				canSoBe(theValue);
		}

		/** Called only by {@code SudokuExplainer.candidateTyped} to reinstate
		 * the given value to my maybes, and also put my index back into the
		 * {@code idxsOf[value]} in my box, row, and col.
		 * <p>We then call cancelMaybes, so we're still safe.
		 * @param value */
		public void canSoBe(int value) {
			maybes.add(value);
			addMeBackIntoMyRegionsIndexesOfValue(value);
		}

//KEEP4DOC: not used: keep to show noobs how JUST CALCULATE IT!
//		/**
//		 * City life's made him a bit slow Redge. It's under the bonnet son.
//		 * It takes longer to invoke a method than the method takes, so don't
//		 * invoke a method, unless of course you couldn't give a s__t about
//		 * performance because you're not, and never will be, on any critical
//		 * path, because you're doing something unusual like creating a hint.
//		 *
//		 * @param value
//		 * @return true if the potential values of this cell contain the given
//		 * value, else false.
//		 */
//		public boolean maybe(int value) {
//			return (maybes.bits & SHFT[value]) != 0;
//		}

		/**
		 * Are any of this cells siblings this 'value'?
		 * @param value int - the value to look for.
		 * @return true if any of this cells siblings are 'value', else false.
		 */
		public boolean canSeeValue(int value) {
			for ( Cell sib : siblings )
				if ( sib.value == value )
					return true;
			return false;
		}

		/**
		 * Get the cell indexes that form the "house" of this cell. The cell
		 * indexes have to be greater than this cell index. The "house" cells
		 * are all the cells that are in the same block, row or column.
		 * <p>
		 * The iteration order is guaranteed to be the same on each invocation
		 * of this method for the same cell. This is necessary to ensure that
		 * hints of the same difficulty are always returned in the same order.
		 *
		 * @return array of the cell indexes that are controlled by this cell
		 */
		public int[] forwardIndices() {
			return Grid.FORWARD_INDICES[i];
		}

		/**
		 * Get siblings with indexes greater than mine.
		 * <p>
		 * The iteration order is guaranteed to be the same on each invocation
		 * of this method for the same cell. This is necessary to ensure that
		 * hints of the same difficulty are always returned in the same order.
		 *
		 * @return an index of my "forward" siblings
		 */
		public BitIdx forwards() {
			BitIdx result = Grid.AFTER[i];
			result.grid = Grid.this;
			return result;
		}

		/**
		 * Get a Set of the cells in the same box, row or col as this cell,
		 * excluding this cell itself.
		 * <p>
		 * @return the cells that are controlled by this cell
		 */
		public BitIdx visible() {
			BitIdx result = Grid.BUDETTES[i];
			result.grid = Grid.this;
			return result;
		}

		/**
		 * Returns does this cells potential values contain 'v'?
		 * <p>
		 * Don't hammer me. It's faster to check yourself with
		 * {@code (cell.maybes.bits & SHFT[v]) != 0}
		 * I only exist for convenience, like in hints and s__t.
		 *
		 * @param v the potential value to test for
		 * @return does this cells potential values contain 'v'
		 */
		public boolean maybe(int v) {
			return (maybes.bits & VSHFT[v]) != 0;
		}

		/** @return String representation of this cell: A1=5 or A2:3{368}.<br>
		 * Used only for debugging: ie is NOT part of the GUI application.<br>
		 * Note that toFullString explicitly gets the full string. Currently
		 * toString returns the same, but don't rely on that not changing when
		 * you really want the full string, so use toFullString explicitly.
		 * <p>toString may be called BEFORE the Cell is done initialising. */
		@Override
		public String toString() {
			if ( maybes == null )
				return id; // Cell is initialising
			if ( value != 0 )
				return id+"="+value;
			else
				return id+":"+maybes.size+"{"+maybes+"}";
		}

		/** @return String representation of this cell: A1=5 or A2:3{368}.<br>
		 * Note that toFullString explicitly gets the full string. Currently
		 * toString returns the same, but don't rely on that not changing when
		 * you really want the full string, so you call me explicitly. */
		public String toFullString() {
			if ( value != 0 )
				return id+"="+value;
			else
				return id+":"+maybes.size+"{"+maybes+"}";
		}

		/** Does this Cell identity-equals the given Object?
		 * <p>Note that both equals and hashCode use only immutable identity
		 * attributes, so the equals contract is upheld.
		 * @param o Object - other
		 * @return true if the given Object is a Cell at my x,y, else false. */
		@Override
		public boolean equals(Object o) {
			return o instanceof Cell
				&& hashCode == ((Cell)o).hashCode; //y*9 + x (identity only)
		}

		/** Returns the hashCode of this cell, which uniquely identifying this
		 * Cell within its Grid.
		 * <p>Note that both equals and hashCode use only immutable identity
		 *  attributes, so the equals contract is upheld.
		 * @return int {@code y<<4 + x} */
		@Override
		public int hashCode() {
			return hashCode; //y<<4 + x
		}

		// If you don't know what you're doing then don't use this method.
		// If you need to use this method anyway then good luck.
		// You MUST finally disarrayonateMaybes.
		private void arrayonateShiftedMaybes() {
			if ( maybes.bits == 0 )
				shiftedMaybes = null;
			else
				shiftedMaybes = VSHIFTED[maybes.bits];
		}

		private void disarrayonateMaybes() {
			shiftedMaybes = null;
		}

		@Override
		public int compareTo(Cell other) {
			final int ohc = other.hashCode;
			if ( ohc < hashCode )
				return -1;
			if ( ohc > hashCode )
				return 1;
			return 0;
		}
	}

	// ---------------- The Regions ----------------

	/** Return the region for this id.
	 * @param id The region.id, eg "box 1" or "col H" or "row 9"
	 * @return  */
	public ARegion region(String id) {
		// Presumes that ALL region-id's are exactly 5 character in length!
		if ( "null".equals(id) || id.length()!=5 )
			return null;
		// idx is the fifth character in the given id.
		// It's a digit or a letter, depending on the region type.
		char idx = id.charAt(4);
		// regionType is the first character in the given id
		switch ( Character.toLowerCase(id.charAt(0)) ) {
			case 'b': return regions[   idx-'1']; // box 1
			case 'r': return regions[ 9+idx-'1']; // row 9
			case 'c': return regions[18+idx-'A']; // col H
			default: throw new IllegalArgumentException("Illegal id=\""+id+"\"");
		}
	}

	/** "box", "row", "col". */
	public static final String[] REGION_TYPE_NAMES = {
		"box", "row", "col"
	};
	/** 0 is the index of the Box in each Cells regions array. */
	public static final int BOX = 0;
	/** 1 is the index of the Row in each Cells regions array. */
	public static final int ROW = 1;
	/** 2 is the index of the Col in each Cells regions array. */
	public static final int COL = 2;

	/** ARegion is an abstract class which represents a region (a 3*3 Box,
	 * a Row, or a Col) of cells in this Sudoku Grid. */
	public abstract class ARegion {

		/** My grid.regions array index 0..26. */
		public final int index;

		/** 0=Grid.BOX, 1=Grid.ROW, 2=Grid.COL. */
		public final int typeIndex;

		/** "box", "row", "col". */
		public final String typeName;

		/** "box 1", "row 4", "col E". */
		public final String id;

		/** Cause.HiddenBox, Cause.HiddenRow, Cause.HiddenCol. */
		public final Cause cause;

		/** The boxs which cross this row or col. Empty in a box. */
		public final Box[] crossingBoxs;

		/** The cells of this region left-to-right and top-to-bottom. */
		public final Cell[] cells = new Cell[9];

		/**
		 * Indexes-in-this-region of cells which maybe value 1..9. The indices
		 * stored in each Indexes are into the region.cells array. indexesOf
		 * should NOT be confused with idxsOf which stores grid.cells indices.
		 * <p>
		 * The motivation for each region having its own cells array and
		 * accompanying Indexes is to abstract away the regions "shape";
		 * so that we can QUICKLY query a region without worrying about
		 * its actual type: row, col, or box; they are all equivalent.
		 * <p>
		 * nb: Renamed from idxsOf to differentiate
		 * an Indexes (indexes in this region)
		 * from an Idx (indices in the whole grid). Sigh.
		 * Maybe we should rename Indexes "Ridx" and call indexesOf "ridOf"?
		 * I never know these things. How long is a piece of string?
		 */
		public final Indexes[] indexesOf = new Indexes[10];

		/**
		 * The Grid.this.cells indices of cells in this region which maybe each
		 * potential value 1..9; not to be confused with indexesOf which
		 * contains indexes into ARegion.this.cells array.
		 */
		public Idx[] idxs = new Idx[10];

		/** The Grid.cells indices of the cells in this region. */
		public final IdxL idx = new IdxL();

		/** The number of cells with value==0 calculated by countEmptyCells()
		 * which is called by Grid.rebuildAllRegionsEmptyCellCounts(). */
		public int emptyCellCount = 9;

		/** The rectangle around this region, in cells. */
		public final Bounds bounds;

		/**
		 * Does this region contain this value. NoHomelessValues calls
		 * rebuildContainsValueArray, so the array is up-to-date in all the
		 * actual hinters. Check the validators running-order if you want me
		 * in another validator. The rule is: The first validator/hinter that
		 * references containsValue does the ONLY call to
		 * rebuildContainsValueArray(). Everybody else piggy-backs on him!
		 */
		public final boolean[] containsValue = new boolean[10];

		/** The 3 boxs which intersect this row or col; null for box. */
		public Box[] intersectingBoxs;

		/**
		 * The A(bstract)Region Constructor. I'm a Box, or a Row, or a Col.
		 *
		 * @param index the index of this region in the Grid.regions array.
		 * @param typeIndex Grid.BOX, Grid.ROW, Grid.COL
		 * @param cause Cause.HiddenBox, Cause.HiddenRow, Cause.HiddenCol
		 * @param numCrossings the number of crossingBoxes: 0 for each Box,
		 *  3 for each Row and Col. Passing this in makes it possible for the
		 *  crossingBoxes array to be final.
		 * @param bounds the Bounds of this region.
		 */
		protected ARegion(int index, int typeIndex, Cause cause
				, int numCrossings, Bounds bounds) {
			this.bounds = bounds;
			this.index = index;
			this.typeName = Grid.REGION_TYPE_NAMES[this.typeIndex = typeIndex];
			this.id = REGION_IDS[index];
			this.cause = cause;
			this.crossingBoxs = new Box[numCrossings];
			// create the indexesOf and idxs arrays elements
			for ( int v=1; v<10; ++v ) { // 0 is left null (in both)
				indexesOf[v] = new Indexes(); // Indexes is full
				idxs[v] = new Idx(); // Idx is empty
			}
		}

		// used by Grid's copyFrom
		private void copyFrom(ARegion src) {
			// deep copy the indexesOf and idxs of all values 1..9
			for ( int v=1; v<10; ++v ) {
				indexesOf[v].copyFrom(src.indexesOf[v]);
				idxs[v].set(src.idxs[v]);
			}
			emptyCellCount = src.emptyCellCount;
			System.arraycopy(src.containsValue, 0, containsValue, 0, 10);
			//NB: crossingRegions are immutable, so remain unchanged
		}

		/**
		 * Return the cells at the given indexes in this region.
		 * <p>
		 * <b>WARNING:</b> Use me sparingly coz I create a new {@code Cell[]},
		 * so calling me in a search creates LOTS of garbage arrays.
		 *
		 * @param indexes {@code int[]} to get.
		 * @return a new {@code Cell[]}.
		 */
		public Cell[] atNew(int[] indexes) {
			final int n = indexes.length;
			Cell[] array = new Cell[n];
			for ( int i=0; i<n; ++i )
				array[i] = this.cells[indexes[i]];
			return array;
		}

		/**
		 * Return a new array of the cells at the given index 'bits' in this
		 * region.
		 * <p>
		 * <b>WARNING:</b> Use me sparingly because I create a new {@code int[]}
		 * on each invocation. Do NOT call me in a loop, or you will create
		 * garbage arrays, which must be collected. NEVER call me in a tight
		 * loop: use another at method!
		 *
		 * @param bits {@code int} a bitset of the indexes to get, where the
		 * position (from the right) of each set (1) bit denotes the index in
		 * this.cells array of a Cell to retrieve.
		 * @return a new {@code Cell[]}.
		 */
		public Cell[] atNew(int bits) {
			final int n = ISIZE[bits];
			Cell[] array = new Cell[n];
			int cnt = at(bits, array);
			assert cnt == n;
			return array;
		}

		/**
		 * Returns a new {@code ArrayList<Cell>} containing the cells at the
		 * given bits in this regions cells array.
		 * <p>
		 * <b>WARNING:</b> Use me sparingly because I create a new
		 * {@code ArrayList<Cell>}.
		 *
		 * @param bits the bits of an Indexes of the cells you require
		 * @return a new {@code ArrayList<Cell>} containing the requested cells
		 */
		public ArrayList<Cell> atNewArrayList(int bits) {
			ArrayList<Cell> list = new ArrayList<>(ISIZE[bits]);
			for ( int i : INDEXES[bits] )
				list.add(this.cells[i]);
			return list;
		}

		/**
		 * Populates and returns the appropriately sized cas-array with the
		 * cells at the given index 'bits' in this region.
		 * <p>
		 * This version of 'at' uses the cas (a cache of cell arrays) because
		 * that's faster than creating garbage arrays when called from a loop.
		 *
		 * @param bits {@code int} a bitset of the indexes to get, where the
		 * position (from the right) of each set (1) bit denotes the index in
		 * this.cells array of a Cell to retrieve
		 * @param dummy not used, this parameter exists just to differentiate
		 * this methods signature from those of the other 'at' methods
		 * @return the selected cas-array (of the correct size)
		 */
		public Cell[] at(int bits, boolean dummy) {
			final int n = ISIZE[bits];
			Cell[] array = cas(n);
			int cnt = at(bits, array);
			assert cnt == n;
			return array;
		}

		/**
		 * Populates the given array with the cells at the given index 'bits'
		 * in this region. The array is presumed to be large enough.
		 * <p>
		 * This version of at is preferred for use in a loop.
		 *
		 * @param bits {@code int} a bitset of the indexes to get, where the
		 * position (from the right) of each set (1) bit denotes the index in
		 * this.cells array of a Cell to retrieve.
		 * @param array Cell[] the cells array to populate.
		 * @return the number of cells in the array.
		 */
		public int at(int bits, Cell[] array) {
			int cnt = 0;
			for ( int i : INDEXES[bits] )
				array[cnt++] = this.cells[i];
			return cnt;
		}

		/**
		 * Returns a new LinkedList of the cells in this region other than
		 * those in the given excluded collection.
		 * <p>
		 * WARN: I return new {@code LinkedList<Cell>()} so call me sparingly,
		 * like during hint creation; not as part of your bloody search, which
		 * will hammer the s__t out of me, coz I'm not really up for it.
		 *
		 * @param excluded the {@code Iterable<Cell>} cells to be excluded
		 * @return a new {@code LinkedList<Cell>()} so call me sparingly!
		 */
		public LinkedList<Cell> otherThan(Iterable<Cell> excluded) {
			LinkedList<Cell> result = new LinkedList<>();
			CELL: for ( Cell c : this.cells ) {
				// if ( !excluded.contains(c) ) result.add(c);
				// except I can use == instead of equals
				for ( Cell e : excluded )
					if ( e == c )
						continue CELL;
				result.add(c);
			}
			return result;
		}

		/**
		 * Get the index of the given cell within this region.
		 * <p>Note that the <b>cell</b> is assumed (Ass U Me) to be in this
		 * region; in fact that's checked with an assert. All that is returned
		 * is the cells b, y, or x index: whichever is appropriate for this type
		 * of ARegion. So you CANNOT use indexOf as a contains test, as you
		 * might (reasonably) expect.
		 * If this behaviour upsets you then fix it yourself (check the given
		 * cells b/x/y against my b/x/y) to return -1, which'll throw an AIOBE)
		 * I just prefer speed to correctness upon which I do not rely.
		 *
		 * @param cell the cell whose index you seek.
		 * @return the index of the cell in this regions cells array.
		 */
		public abstract int indexOf(Cell cell);

		/**
		 * Does this region contain this Cell? Fast O(1).
		 *
		 * @param cell Cell to look for.
		 * @return true if this region contains this cell, else false.
		 */
		public abstract boolean contains(Cell cell);

		/** Rebuild this regions containsValue array from the cell values. */
		public void rebuildContainsValueArray() {
			Arrays.fill(containsValue, false);
			for ( Cell c : cells )
				if ( c.value > 0 )
					containsValue[c.value] = true;
		}

		/**
		 * Populate the given array with the cells in this region which maybe
		 * bits and return how many. The given array is presumed to be large
		 * enough (or the exact size), so I don't null-terminate or anything,
		 * I just return the count for use with a "large" re-usable array.
		 * <p>
		 * NOTE WELL: This method is a bit slow. It's currently used only by
		 * Locking when creating a hint, ie not too often. If you have
		 * want of me then it'll be faster to grid.getIdxs() ONCE instead
		 * of hammering the s__t of me, coz I'm not up to it. But you can use
		 * me "not too often", like hint creation.
		 *
		 * @param bits a left-shifted bitset of the value/s you seek.
		 * @param array {@code Cell[]} to populate. The array is presumed to
		 *  be large enough.
		 * @return the count: the number of cells added to the array.
		 */
		public int maybe(int bits, Cell[] array) {
			int cnt = 0;
			for ( Cell cell : cells ) // 2,916 * 9 = 26,244
				if ( (cell.maybes.bits & bits) != 0 )
					array[cnt++] = cell;
			return cnt;
		}

		/**
		 * Return the cells in this region which maybe 'bits'.
		 * <p>
		 * This method only used by UniqueRectangle.createType4Hint.
		 * <p>
		 * Called 382,984 times in top1465: efficiency matters a bit, not lots.
		 * <p>
		 * Also this is the only use of local CellSet + LinkedHashCellSet;
		 * so another way of doing this efficiently could eradicate lots of
		 * "special" code, which is desirable. Sigh.
		 *
		 * @param bits bitset (one-or-more left-shifted values) to get.
		 * @param results a LinkedHashCellSet as a CellSet to which I add
		 * @return a new {@code LinkedHashCellSet} as a {@code CellSet}.
		 */
		public IUrtCellSet maybe(int bits, IUrtCellSet results) {
			// all current calls require a "clean" result set. If you want
			// otherwise then roll-your-own method by copy-pasting this one
			// to add a clearResult parameter, then change me to call him.
			results.clear();
			for ( Cell c : this.cells ) // 9 cells
				if ( (c.maybes.bits & bits) != 0 )
					results.add(c);
			return results;
		}

		/**
		 * Count the number of empty (value==0) cells in this region,
		 * remembering that value in my {@code emptyCellCount} field.
		 * <p>
		 * emptyCellCount runs once at commencement of solving, so we can call
		 * emptyCellCount BEFORE calling emptyCells, which sets emptyCellCount.
		 *
		 * @return int the count.
		 */
		public int emptyCellCount() {
			int cnt = 0;
			for ( Cell cell : cells )
				if ( cell.value == 0 )
					++cnt;
			return emptyCellCount = cnt;
		}

		public Cell[] emptyCells(Cell[] result) {
			int cnt = 0;
			for ( Cell cell : cells )
				if ( cell.value == 0 )
					result[cnt++] = cell;
			emptyCellCount = cnt;
			return result;
		}

		/** @return id - so just access the id directly, it's faster. */
		@Override
		public String toString() {
			return id;
		}

		/**
		 * Returns the given Idx populated with the indices of the cells in
		 * this ARegion which maybe any of the given maybes (a bitset).
		 * @param maybes a bitset of the maybes you seek
		 * @param result the result Idx
		 * @return the result Idx of cells in this region which maybe maybes
		 */
		public Idx idxOf(int maybes, Idx result) {
			result.clear();
			for ( Cell c : cells )
				if ( (c.maybes.bits & maybes) != 0 )
					result.add(c.i);
			return result;
		}

		public Cell firstEmptyCell() {
			for ( Cell c : cells )
				if ( c.value == 0 )
					return c;
			return null;
		}

	}

	/** A box (a 3-by-3 block of cells) in this Sudoku Grid. */
	public final class Box extends ARegion {
		/** Vertical index of this box: 0=top, 1=middle, 2=bottom */
		public final int vNum;
		/** Top y of this box: 0=top, 3=middle, 6=bottom */
		public final int top;
		/** Horizontal index of this box: 0=left, 1=middle, 2=right */
		public final int hNum;
		/** Left x of this box: 0=left, 3=middle, 6=right */
		public final int left;
		/** Index of this box in the Grids boxs array, which (by design)
		 * is coincident with the Grids regions array. Each Cell also knows
		 * it's boxId, upon which contains relies, coz it's Fast. */
		public final int boxId;
		/**
		 * Construct a new Box in this Grid.
		 * @param index the index of this Box (0..8) in the Grid.regions array.
		 */
		public Box(int index) {
			super(index, BOX, Cause.HiddenBox, 0
				, new Bounds(index%3*3, index/3*3, 3, 3));
			this.hNum = index%3;  this.left = hNum*3;
			this.vNum = index/3;  this.top = vNum*3;
			this.boxId = top + hNum;
			// initialise the cells array, and set each cells box
			for ( int i=0; i<9; ++i )
				// nb: it assigns cells[i] BEFORE it assigns this.
				(cells[i]=Grid.this.cells[((top+i/3)*9)+(left+i%3)]).regions[BOX]
						= cells[i].box = this;
		}
		/** Return the index of the given cell in this Regions cells array.
		 * @return Actually just returns the given cells 'b' index (for box),
		 *  we check that thisRegion.contains(cell) with an assert. */
		@Override
		public int indexOf(Cell cell) {
			assert contains(cell);
			return cell.b; // cell.y%3*3 + cell.x%3;
		}
		/** @return Does this region contain the given cell? */
		@Override
		public boolean contains(Cell cell) {
			return cell.boxId == this.boxId; //cell.y/3==this.vNum && cell.x/3==this.hNum;
		}
	}

	/** A row in this Sudoku Grid. */
	public final class Row extends ARegion {
		/** The index of this Row: 0=top, 8=bottom. */
		public final int y;
		/** The vertical index of the 3 Boxs which I cross. */
		public final int vNum;
		/** Constructs a new Row in this Grid.
		 * @param y int 0..8 the row (vertical) index */
		public Row(int y) {
			super(9+y, ROW, Cause.HiddenRow, 3, new Bounds(0, y, 9, 1));
			this.y = y;
			this.vNum = y / 3;
			for ( int i=0; i<9; ++i )
				(cells[i]=Grid.this.cells[y*9+i]).regions[ROW] = cells[i].row = this;
			intersectingBoxs = new Box[] {
					  Grid.this.boxs[vNum]
					, Grid.this.boxs[vNum+1]
					, Grid.this.boxs[vNum+2]
			};
		}
		/** Returns the index of the given Cell in this regions cells array.
		 * @param cell Cell to get the index of.
		 * @return the x (horizontal index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert. */
		@Override
		public int indexOf(Cell cell) {
			assert contains(cell);
			return cell.x;
		}
		/** @return Is 'cell' in this Row? */
		@Override
		public boolean contains(Cell cell) {
			return cell.y == this.y;
		}
	}

	/** A column in this Sudoku Grid */
	public final class Col extends ARegion {
		/** The index of this Col: 0=leftmost, 8=rightmost */
		public final int x;
		/** The horizontal index of the 3 Boxs which I cross. */
		public final int hNum;
		/** Constructs a new Col in this Grid.
		 * @param x int 0..8 the col (horizontal) index */
		public Col(int x) {
			super(18+x, COL, Cause.HiddenCol, 3, new Bounds(x, 0, 1, 9));
			this.x = x;
			this.hNum = x / 3;
			for ( int i=0; i<9; ++i )
				(cells[i]=Grid.this.cells[i*9+x]).regions[COL] = cells[i].col = this;
			intersectingBoxs = new Box[] {
					  Grid.this.boxs[hNum]
					, Grid.this.boxs[hNum+3]
					, Grid.this.boxs[hNum+6]
			};
		}
		/** Returns the index of the given Cell in this regions cells array.
		 * @return the y (vertical index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert. */
		@Override
		public int indexOf(Cell cell) {
			assert contains(cell);
			return cell.y;
		}
		/** @return Does this column contain the given Cell? */
		@Override
		public boolean contains(Cell cell) {
			return cell.x == this.x;
		}
	}

	/**
	 * The (x (left), y (top), width, height) of a region, in cells.
	 * <p>
	 * All units are expressed in cells (the most abstract abstraction).
	 * <p>
	 * The Bounds class is only used in the
	 * {@link diuf.sudoku.gui.SudokuGridPanel#paintRegions} method, it's just
	 * a way of calculating ONCE and storing in the model the outline of each
	 * ARegion (which is presentation data). Ergo: I probably shouldn't do it,
	 * but I'm doing it anyway, just because I want to.
	 * <p>
	 * I disagree with the Java ethos of creating new s__t everywhere to uphold
	 * nefarious arbitrary boundaries between concerns. So if my model gets
	 * mildly polluted with presentation data then that doesn't worry me. It
	 * only worries me when presentation types, over which I have no control,
	 * appear in my model, because any changes there may effect me here; and
	 * this, being MY type, doesn't set off any Warning Will Robinson's, simply
	 * because it's MINE; so the presentation concern has been abstracted to
	 * a local type, using an abstract unit (cells) so no matter which
	 * presentation framework we use, a Bounds is a Bounds is a Bounds is a...
	 * So it's all good!
	 */
	public static final class Bounds {
		public final int x, y, w, h;
		public Bounds(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		@Override
		public boolean equals(Object o) {
			return o!=null && (o instanceof Bounds) && equals((Bounds)o);
		}
		@Override
		public int hashCode() {
			return (w<<12) ^ (h<<10) ^ (x<<5) ^ y ;
		}
		public boolean equals(Bounds o) {
			return x==o.x && y==o.y && w==o.w && h==o.h;
		}
		@Override
		public String toString() {
			return "x="+x+", y="+y+", w="+w+", h="+h;
		}
	}

	/**
	 * DEBUG: Diff two grids: original and copy.
	 * <p>
	 * I'm an inner class coz I access ALL of Grid's privates.
	 * <p>
	 * Used by {@link diuf.sudoku.solver.checks.RecursiveAnalyser} when you
	 * modify code to switch me on. I am a DEBUG only tool.
	 */
	public static class Diff {
		public final Grid g0;
		public final Grid g1;
		public boolean any = false;
		public PrintStream out;
		/**
		 * Diff two grids
		 * @param g0 original
		 * @param g1 copy
		 */
		public Diff(Grid g0, Grid g1) {
			this.g0 = g0;
			this.g1 = g1;
		}
		public boolean diff(PrintStream out) {
			this.out = out;
			any = false;

			//
			// all output is original (g0) then copy (g1)
			//

			// If you get a poke in the 'i' you can forget the blunt stick
			for ( int i=0; i<81; ++i ) {
				Cell c0 = g0.cells[i];
				Cell c1 = g1.cells[i];
				if ( c0.i != c1.i ) {
					return differs(""+i+": i "+c0.i+" != "+c1.i);
				}
			}

			// Cells
			for ( int i=0; i<81; ++i ) {
				Cell c0 = g0.cells[i];
				Cell c1 = g1.cells[i];
				if ( c0.value != c1.value )
					differs(""+i+": value "+c0.value+" != "+c1.value);
				if ( !c0.maybes.equals(c1.maybes) )
					differs(""+i+": maybes "+c0.maybes+" != "+c1.maybes);
				if ( c0.b != c1.b )
					differs(""+i+": b "+c0.b+" != "+c1.b);
				if ( c0.x != c1.x )
					differs(""+i+": x "+c0.x+" != "+c1.x);
				if ( c0.y != c1.y )
					differs(""+i+": y "+c0.y+" != "+c1.y);
				if ( !c0.box.id.equals(c1.box.id) )
					differs(""+i+": box "+c0.box+" != "+c1.box);
				if ( !c0.col.id.equals(c1.col.id) )
					differs(""+i+": col "+c0.col+" != "+c1.col);
				if ( !c0.row.id.equals(c1.row.id) )
					differs(""+i+": row "+c0.row+" != "+c1.row);
				if ( c0.boxId != c1.boxId )
					differs(""+i+": boxId "+c0.boxId+" != "+c1.boxId);
				if ( c0.hashCode != c1.hashCode )
					differs(""+i+": hashCode "+c0.hashCode+" != "+c1.hashCode);
				if ( c0.idxdex != c1.idxdex )
					differs(""+i+": idxdex "+c0.idxdex+" != "+c1.idxdex);
				if ( c0.idxshft != c1.idxshft )
					differs(""+i+": shft "+c0.idxshft+" != "+c1.idxshft);
				if ( c0.skip != c1.skip )
					differs(""+i+": skip "+c0.skip+" != "+c1.skip);
				for ( int j=0; j<3; ++j )
					// my indexIn each of my three constraints (regions)
					if ( c0.indexIn[j] != c1.indexIn[j] )
						differs(""+i+": indexIn["+j+"] "+c0.indexIn[j]+" != "+c1.indexIn[j]);
				for ( int j=0; j<c0.notSees.length; ++j )
					if ( c0.notSees[j] != c1.notSees[j] )
						differs(""+i+": notSees["+j+"] "+c0.notSees[j]+" != "+c1.notSees[j]);
				for ( int r=0; r<3; ++r ) {
					// each cell is in 3 constraints (regions). just ensure
					// they're the same ones: compared independantly later.
					ARegion r0 = c0.regions[r];
					ARegion r1 = c1.regions[r];
					if ( !r0.id.equals(r1.id) )
						differs(""+i+": regions "+r0.id+" != "+r1.id);
				}
				if ( c0.properties!=null && c1.properties!=null && !c0.properties.equals(c1.properties) )
					differs(""+i+": properties "+c0.properties+" != "+c1.properties);
			}

			// Boxs
			for ( int b=0; b<9; ++b ) {
				Box b0 = g0.boxs[b];
				Box b1 = g1.boxs[b];
				if ( b0.boxId != b1.boxId )
					differs(""+b+": boxId "+b0.boxId+" != "+b1.boxId);
				if ( b0.hNum != b1.hNum )
					differs(""+b+": hNum "+b0.hNum+" != "+b1.hNum);
				if ( b0.vNum != b1.vNum )
					differs(""+b+": vNum "+b0.vNum+" != "+b1.vNum);
				if ( b0.top != b1.top )
					differs(""+b+": top "+b0.top+" != "+b1.top);
				if ( b0.left != b1.left )
					differs(""+b+": left "+b0.left+" != "+b1.left);
			}
			// Rows
			for ( int r=0; r<9; ++r ) {
				Row r0 = g0.rows[r];
				Row r1 = g1.rows[r];
				if ( r0.vNum != r1.vNum )
					differs(""+r+": vNum "+r0.vNum+" != "+r1.vNum);
				if ( r0.y != r1.y )
					differs(""+r+": y "+r0.y+" != "+r1.y);
			}
			// Cols
			for ( int c=0; c<9; ++c ) {
				Col r0 = g0.cols[c];
				Col r1 = g1.cols[c];
				if ( r0.hNum != r1.hNum )
					differs(""+c+": hNum "+r0.hNum+" != "+r1.hNum);
				if ( r0.x != r1.x )
					differs(""+c+": x "+r0.x+" != "+r1.x);
			}

			// All Regions
			for ( int r=0; r<27; ++r ) {
				ARegion r0 = g0.regions[r];
				ARegion r1 = g1.regions[r];
				if ( !r0.id.equals(r1.id) )
					differs(""+r+": id "+r0.id+" != "+r1.id);
				if ( r0.index != r1.index )
					differs(""+r+": index "+r0.index+" != "+r1.index);
				if ( r0.typeIndex != r1.typeIndex )
					differs(""+r+": typeIndex "+r0.typeIndex+" != "+r1.typeIndex);
				if ( !r0.typeName.equals(r1.typeName) )
					differs(""+r+": typeName "+r0.typeName+" != "+r1.typeName);
				if ( !r0.bounds.equals(r1.bounds) )
					differs(""+r+": bounds "+r0.bounds+" != "+r1.bounds);
				if ( !r0.cause.equals(r1.cause) )
					differs(""+r+": cause "+r0.cause+" != "+r1.cause);
				if ( !Arrays.equals(r0.cells, r1.cells) )
					differs(""+r+": cells "+Arrays.toString(r0.cells)+" != "+Arrays.toString(r1.cells));
				if ( !Arrays.equals(r0.containsValue, r1.containsValue) )
					differs(""+r+": containsValue "+Arrays.toString(r0.containsValue)+" != "+Arrays.toString(r1.containsValue));
				// nb: for some reason Arrays.equals returns a false negative
				//     when toString returns the same for both arrays. IDKFA!
				if ( r0.crossingBoxs.length != r1.crossingBoxs.length )
					differs(""+r+": crossingBoxs.length "+r0.crossingBoxs.length+" != "+r1.crossingBoxs.length);
				if ( r > 8 ) // suppress for boxes (which don't HAVE crossingBoxs, allthough one of them once toungue-pashed a hot cross bun. Don't tell the Easter bunny! She gets jealous, and you don't want to make her cross; unless you WANT chocolate nuts for Christmas. Not that sort of nuts Byron, ya putz!)
					for ( int b=0; b<3; ++b )
						if ( !r0.crossingBoxs[b].equals(r1.crossingBoxs[b]) )
							differs(""+r+": crossingBoxs["+b+"] "+r0.crossingBoxs[b]+" != "+r1.crossingBoxs[b]);
				if ( r0.emptyCellCount != r1.emptyCellCount )
					differs(""+r+": emptyCellCount "+r0.emptyCellCount+" != "+r1.emptyCellCount);
				if ( !r0.idx.equals(r1.idx) )
					differs(""+r+": idx "+r0.idx+" != "+r1.idx);
				for ( int v=1; v<10; ++v ) {
					if ( !r0.idxs[v].equals(r1.idxs[v]) )
						differs(""+r+": idxs["+v+"] "+r0.idxs[v]+" != "+r1.idxs[v]);
					if ( !r0.indexesOf[v].equals(r1.indexesOf[v]) )
						differs(""+r+": indexesOf["+v+"] "+r0.indexesOf[v]+" != "+r1.indexesOf[v]);
				}
			}
			return any;
		}

		private boolean differs(String msg) {
			out.println(msg);
			return any = true;
		}
	}

}
