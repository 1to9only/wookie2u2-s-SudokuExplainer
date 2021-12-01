/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Idx.BITS_PER_ELEMENT;
import static diuf.sudoku.Idx.BITS_PER_WORD;
import static diuf.sudoku.Idx.BITS_TWO_ELEMENTS;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Idx.WORDS;
import static diuf.sudoku.Idx.WORD_MASK;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.Values.*;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.hinters.urt.UniqueRectangle.IUrtCellSet;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Hash;
import diuf.sudoku.utils.IFilter;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedList;
import diuf.sudoku.utils.MyMath;
import diuf.sudoku.utils.MyStrings;
import java.io.PrintStream;
import static java.lang.Integer.bitCount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

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

	/**
	 * AUTOSOLVE: prefer false to count Naked and Hidden Singles, or true for
	 * outright speed and extra mayhem. These constants are self-documenting
	 * code as AHint.apply's isAutosolving parameter.
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
	 * NOTE: true makes RecursiveSolverTester faster.
	 */
	public static final boolean AUTOSOLVE = false; // @check false
	/** A self-documenting AHint.apply isAutosolving parameter. */
	public static final boolean NO_AUTOSOLVE = false;

	/** The number of cells in an ARegion: 9. */
	public static final int REGION_SIZE = 9; // change is NOT an option!

	/** One more than the highest 1-based value: 10. */
	public static final int VALUE_CEILING = REGION_SIZE + 1;

	/** The number of cells across a Box: 3, the square-root of 9. */
	public static final int SQRT = (int)Math.sqrt(REGION_SIZE);

	/** 0 is the index of the Box in each Cells regions array. */
	public static final int BOX = 0;
	/** 1 is the index of the Row in each Cells regions array. */
	public static final int ROW = 1;
	/** 2 is the index of the Col in each Cells regions array. */
	public static final int COL = 2;
	/** The name of each type of region: "box", "row", "col". */
	public static final String[] REGION_TYPE_NAMES = {"box", "row", "col"};
	/** The number of region types: 3 (BOX, ROW, COL). */
	public static final int NUM_REGION_TYPES = 3;

	/** The index of the first col in grid.regions: 18. */
	public static final int FIRST_COL = REGION_SIZE<<1;

	/** The number of regions: 27 (9*box, 9*row, 9*col). */
	public static final int NUM_REGIONS = REGION_SIZE * NUM_REGION_TYPES;

	/** The number of cells in a Grid: 81. */
	public static final int GRID_SIZE = REGION_SIZE * REGION_SIZE;

	/** The minimum number of clues in a Grid: 17. */
	public static final int MIN_CLUES = 17;

	/** The maximum number of empty cells in a Grid: 81 - 17 = 64. */
	public static final int MAX_EMPTIES = GRID_SIZE - MIN_CLUES;

	/** The number of sibling cells that each cell has: 20. */
	public static final int NUM_SIBLINGS = 20;

	// Shorthands, because the longhands, while meaningful, are just too long.
	// NOTE that other classes create there own shorthands for these values,
	// but every class that uses shorthand defines it's own locally, which I
	// have brought all into alignment, which they'll wander away from again,
	// so maybe it'd be better to just make these "ungrocables" public?
	private static final int N = REGION_SIZE; // REGION_SIZE is 9 cells
	private static final int NN = GRID_SIZE; // REGION_SIZE squared is 81 cells
	private static final int R = SQRT; // squareroot of REGION_SIZE is 3 cells
	//                                    and so are the days of our Sudoku
	//                                    a lowku by KRC

	/** Regex to match the clues line in the input file. */
	private static final Pattern CLUES_PATTERN = Pattern.compile(
			"([1-"+N+"]*,){"+(NN-1)+"}[1-"+N+"]*");

	// do first else toString gets an exception in the debugger
	private static final char[] DIGITS = ".123456789".toCharArray();

	/**
	 * An IdScheme is how we identify cells and regions in a Grid.
	 * There are two IdScheme's: Chess (preferred) and RowCol (untested).
	 * <p>
	 * This enum encapsulates all the-facts-of-life of an IdScheme.
	 * It produces and parses both {@link Cell#id} and {@link ARegion#id}.
	 * <p>
	 * The private IdScheme's functionality is exposed by public static proxy
	 * methods, so there's ONE scheme, which is used everywhere that needs it.
	 * <p>
	 * NOTE: I've only ever used Chess, but try to retro-fit RowCol, only to
	 * discover that my test-cases are all welded-onto Chess, and I'm unwilling
	 * to spend a week (or two) rewriting them all for some silly idScheme, so
	 * we're permanently welded onto Chess.
	 */
	private static enum IdScheme {
		Chess { // A1..I9
			@Override
			public int length() {
				return 2;
			}
			@Override
			public String id(int indice) {
				return LETTERS[indice%N] + NUMBERS[indice/N];
			}
			@Override
			public int indice(String id) {
				final int row = id.charAt(1)-'1';
				if ( row<0 || row>8 ) {
					throw new IllegalArgumentException("Bad row: "+id);
				}
				final int col = id.charAt(0)-'A';
				if ( col<0 || col>8 ) {
					throw new IllegalArgumentException("Bad col: "+id);
				}
				return row*N + col;
			}
			@Override
			public int regionIndex(String rid) {
				final String type = rid.substring(0, 3);
				final char c = rid.charAt(4);
				if ( type.equals(REGION_TYPE_NAMES[BOX]) ) {
					return c - '1';
				}
				if ( type.equals(REGION_TYPE_NAMES[ROW]) ) {
					return REGION_SIZE + c - '1';
				}
				if ( type.equals(REGION_TYPE_NAMES[COL]) ) {
					if ( c<'A' || c>'I' ) {
						throw new IllegalArgumentException("Bad col: "+rid);
					}
					return FIRST_COL + c - 'A';
				}
				throw new IllegalArgumentException("Bad rid: "+rid);
			}
			@Override
			public String[] columnLabels() {
				return LETTERS;
			}
		}
		, RowCol { // 1-1..9-9
			@Override
			public int length() {
				return 3;
			}
			@Override
			public String id(int indice) {
				return NUMBERS[indice/N] + "-" + NUMBERS[indice%N];
			}
			@Override
			public int indice(String id) {
				return (id.charAt(0)-'1')*N + (id.charAt(3)-'1');
			}
			@Override
			public int regionIndex(String rid) {
				final String type = rid.substring(0, 3);
				final int x = rid.charAt(4) - '1';
				for ( int i=0; i<NUM_REGION_TYPES; ++i ) { // BOX, ROW, COL
					if ( REGION_TYPE_NAMES[i].equals(type) ) {
						return i*REGION_SIZE + x;
					}
				}
				throw new IllegalArgumentException("Bad rid: "+rid);
			}
			@Override
			public String[] columnLabels() {
				return NUMBERS;
			}
		}
		;
		/** @return the cell.id length under this IdScheme. */
		public abstract int length();
		/** @return the id for Cell at indice under this IdScheme. */
		public abstract String id(int indice);
		/** @return the indice for Cell at id under this IdScheme. */
		public abstract int indice(String id);
		/** @return the region.index for region.id under this IdScheme. */
		public abstract int regionIndex(String rid);
		/** @return an array of columnLabels[0..8] under this IdScheme. */
		public abstract String[] columnLabels();
	}

	/** The ID scheme: Chess (change NOT an option: test-cases welded-on). */
	private static final IdScheme ID_SCHEME = IdScheme.Chess;
	/** The length of the ID field of each cell in this Grid. */
	public static final int CELL_ID_LENGTH = ID_SCHEME.length();
	/** The ID field of each cell in this Grid. */
	public static final String[] CELL_IDS = new String[GRID_SIZE];
	/** The region.id's by index in the grid.regions array. */
	public static final String[] REGION_IDS = new String[27];
	/** An Idx containing the indice of each cell in the Grid. */
	public static final Idx[] CELL_IDXS = new Idx[GRID_SIZE];
	static {
		int i;
		for ( i=0; i<GRID_SIZE; ++i ) {
			CELL_IDS[i] = ID_SCHEME.id(i);
		}
		for ( i=0; i<REGION_SIZE; ++i ) {
			REGION_IDS[i] = REGION_TYPE_NAMES[BOX]+" "+NUMBERS[i];
		}
		for ( i=REGION_SIZE; i<FIRST_COL; ++i ) {
			REGION_IDS[i] = REGION_TYPE_NAMES[ROW]+" "+NUMBERS[i%REGION_SIZE];
		}
		final String[] cl = ID_SCHEME.columnLabels();
		for ( i=FIRST_COL; i<NUM_REGIONS; ++i ) {
			REGION_IDS[i] = REGION_TYPE_NAMES[COL]+" "+cl[i%REGION_SIZE];
		}
		for ( i=0; i<GRID_SIZE; ++i ) {
			CELL_IDXS[i] = Idx.of(i);
		}
	}

	/** The index of the box which contains each cell. <br>
	 * Can calculate on-the-fly, just faster to look it up. */
	public static final int[] BOX_OF = new int[GRID_SIZE];
	/** The index of the row which contains each cell. <br>
	 * Can calculate on-the-fly, just faster to look it up. */
	public static final int[] ROW_OF = new int[GRID_SIZE];
	/** The index of the row which contains each cell. <br>
	 * Can calculate on-the-fly, just faster to look it up. */
	public static final int[] COL_OF = new int[GRID_SIZE];
	/** a bitset of the 3 regions of each cell in the Grid. */
	public static final int[] CELLS_REGIONS = new int[GRID_SIZE];
	static {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			ROW_OF[i] = i/N;
			COL_OF[i] = i%N;
			BOX_OF[i] = ROW_OF[i]/R*R + COL_OF[i]/R;
			CELLS_REGIONS[i] = IDX_SHFT[BOX_OF[i]] // box
			                 | IDX_SHFT[N+ROW_OF[i]] // row
			                 | IDX_SHFT[FIRST_COL+COL_OF[i]]; // col
		}
	}

	/**
	 * An array containing an Idx of the 20 distinct sibling cells (precluding
	 * the cell itself) in the same box, row, and col as each cell in a Grid.
	 * My index is the cell indice.
	 * <p>
	 * The same Idx is referenced by {@link Cell#buds}. For speed, use BUDDIES
	 * if you have only the cell indice, or buds if you already have the Cell.
	 * <p>
	 * The Idx's are locked, so any attempt to modify throws a LockedException.
	 * <p>
	 * BUDDIES is static because a cell has the same siblings in all Grids, and
	 * these are only indices, not the cells themselves. If you want the cells
	 * themselves then {@link Cell#siblings} is probably faster, presuming that
	 * you already have the cell (sigh).
	 */
	public static final IdxL[] BUDDIES = new IdxL[GRID_SIZE];
	static {
		IdxL buds;
		int box, row, col; // of this cell
		for ( int i=0; i<GRID_SIZE; ++i ) { // foreach cell-indice
			buds = BUDDIES[i] = new IdxL();
			row = ROW_OF[i];
			col = COL_OF[i];
			box = BOX_OF[i];
			for ( int b=0; b<GRID_SIZE; ++b ) { // foreach buddy-cell-indice
				if ( ( ROW_OF[b] == row
				    || COL_OF[b] == col
				    || BOX_OF[b] == box)
				  && i!=b
				) {
					buds.add(b);
				}
			}
			buds.lock(); // so any attempt to mutate throws a LockedException
		}
	}

	/**
	 * Idx of the cells in each region in the grid.
	 * Note that IdxI's are immutable.
	 */
	public static final IdxI[] REGION_IDXS = new IdxI[NUM_REGIONS];
	static {
		final int[][] REGION_INDICES = new int [][] {
			  { 0, 1, 2, 9,10,11,18,19,20} // box 1
			, { 3, 4, 5,12,13,14,21,22,23} // box 2
			, { 6, 7, 8,15,16,17,24,25,26} // box 3
			, {27,28,29,36,37,38,45,46,47} // box 4
			, {30,31,32,39,40,41,48,49,50} // box 5
			, {33,34,35,42,43,44,51,52,53} // box 6
			, {54,55,56,63,64,65,72,73,74} // box 7
			, {57,58,59,66,67,68,75,76,77} // box 8
			, {60,61,62,69,70,71,78,79,80} // box 9
			, { 0, 1, 2, 3, 4, 5, 6, 7, 8} // row 1
			, { 9,10,11,12,13,14,15,16,17} // row 2
			, {18,19,20,21,22,23,24,25,26} // row 3
			, {27,28,29,30,31,32,33,34,35} // row 4
			, {36,37,38,39,40,41,42,43,44} // row 5
			, {45,46,47,48,49,50,51,52,53} // row 6
			, {54,55,56,57,58,59,60,61,62} // row 7
			, {63,64,65,66,67,68,69,70,71} // row 8
			, {72,73,74,75,76,77,78,79,80} // row 9
			, { 0, 9,18,27,36,45,54,63,72} // col A
			, { 1,10,19,28,37,46,55,64,73} // col B
			, { 2,11,20,29,38,47,56,65,74} // col C
			, { 3,12,21,30,39,48,57,66,75} // col D
			, { 4,13,22,31,40,49,58,67,76} // col E
			, { 5,14,23,32,41,50,59,68,77} // col F
			, { 6,15,24,33,42,51,60,69,78} // col G
			, { 7,16,25,34,43,52,61,70,79} // col H
			, { 8,17,26,35,44,53,62,71,80} // col I
		};
		for ( int i=0; i<NUM_REGIONS; ++i ) {
			REGION_IDXS[i] = IdxI.of(REGION_INDICES[i]);
		}
	}

	/**
	 * Idx of the "later" buddies (siblings) of each cell in the Grid.
	 * Later means has a greater index, for use in forwards-only search.
	 */
	public static final Idx[] LATER_BUDS = new Idx[GRID_SIZE];
	static {
		// Indices of "following" siblings, for a forward-only search.
		final int[][] LATER_BUDS_INDICES = new int [][] {
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
		for ( int i=0; i<GRID_SIZE; ++i ) {
			LATER_BUDS[i] = Idx.of(LATER_BUDS_INDICES[i]);
		}
	}

	/** Shorthand for true. */
	public static final boolean T = true;
	/** Shorthand for false. */
	public static final boolean F = false;

	/**
	 * Does each region in the Grid intersect each other other region,
	 * so the first index is me, and the second index is him.
	 */
	public static final boolean[][] REGIONS_INTERSECT = {
		// this code was generated by GridTest.testIntersects
		// box                 row                 col
		  {T,F,F,F,F,F,F,F,F, T,T,T,F,F,F,F,F,F, T,T,T,F,F,F,F,F,F} // box 1
		, {F,T,F,F,F,F,F,F,F, T,T,T,F,F,F,F,F,F, F,F,F,T,T,T,F,F,F} // box 2
		, {F,F,T,F,F,F,F,F,F, T,T,T,F,F,F,F,F,F, F,F,F,F,F,F,T,T,T} // box 3
		, {F,F,F,T,F,F,F,F,F, F,F,F,T,T,T,F,F,F, T,T,T,F,F,F,F,F,F} // box 4
		, {F,F,F,F,T,F,F,F,F, F,F,F,T,T,T,F,F,F, F,F,F,T,T,T,F,F,F} // box 5
		, {F,F,F,F,F,T,F,F,F, F,F,F,T,T,T,F,F,F, F,F,F,F,F,F,T,T,T} // box 6
		, {F,F,F,F,F,F,T,F,F, F,F,F,F,F,F,T,T,T, T,T,T,F,F,F,F,F,F} // box 7
		, {F,F,F,F,F,F,F,T,F, F,F,F,F,F,F,T,T,T, F,F,F,T,T,T,F,F,F} // box 8
		, {F,F,F,F,F,F,F,F,T, F,F,F,F,F,F,T,T,T, F,F,F,F,F,F,T,T,T} // box 9
		, {T,T,T,F,F,F,F,F,F, T,F,F,F,F,F,F,F,F, T,T,T,T,T,T,T,T,T} // row 1
		, {T,T,T,F,F,F,F,F,F, F,T,F,F,F,F,F,F,F, T,T,T,T,T,T,T,T,T} // row 2
		, {T,T,T,F,F,F,F,F,F, F,F,T,F,F,F,F,F,F, T,T,T,T,T,T,T,T,T} // row 3
		, {F,F,F,T,T,T,F,F,F, F,F,F,T,F,F,F,F,F, T,T,T,T,T,T,T,T,T} // row 4
		, {F,F,F,T,T,T,F,F,F, F,F,F,F,T,F,F,F,F, T,T,T,T,T,T,T,T,T} // row 5
		, {F,F,F,T,T,T,F,F,F, F,F,F,F,F,T,F,F,F, T,T,T,T,T,T,T,T,T} // row 6
		, {F,F,F,F,F,F,T,T,T, F,F,F,F,F,F,T,F,F, T,T,T,T,T,T,T,T,T} // row 7
		, {F,F,F,F,F,F,T,T,T, F,F,F,F,F,F,F,T,F, T,T,T,T,T,T,T,T,T} // row 8
		, {F,F,F,F,F,F,T,T,T, F,F,F,F,F,F,F,F,T, T,T,T,T,T,T,T,T,T} // row 9
		, {T,F,F,T,F,F,T,F,F, T,T,T,T,T,T,T,T,T, T,F,F,F,F,F,F,F,F} // col A
		, {T,F,F,T,F,F,T,F,F, T,T,T,T,T,T,T,T,T, F,T,F,F,F,F,F,F,F} // col B
		, {T,F,F,T,F,F,T,F,F, T,T,T,T,T,T,T,T,T, F,F,T,F,F,F,F,F,F} // col C
		, {F,T,F,F,T,F,F,T,F, T,T,T,T,T,T,T,T,T, F,F,F,T,F,F,F,F,F} // col D
		, {F,T,F,F,T,F,F,T,F, T,T,T,T,T,T,T,T,T, F,F,F,F,T,F,F,F,F} // col E
		, {F,T,F,F,T,F,F,T,F, T,T,T,T,T,T,T,T,T, F,F,F,F,F,T,F,F,F} // col F
		, {F,F,T,F,F,T,F,F,T, T,T,T,T,T,T,T,T,T, F,F,F,F,F,F,T,F,F} // col G
		, {F,F,T,F,F,T,F,F,T, T,T,T,T,T,T,T,T,T, F,F,F,F,F,F,F,T,F} // col H
		, {F,F,T,F,F,T,F,F,T, T,T,T,T,T,T,T,T,T, F,F,F,F,F,F,F,F,T} // col I
	};

	/**
	 * A bitset of the indexes in grid.regions of the boxs that are effected
	 * by each of the regions in the grid. The values here-in are box indexes
	 * 0..8, so you can read them with the Indexes.INDEXES array-of-arrays.
	 * <p>
	 * <pre>
	 * The meaning of these values are type dependant:
	 * Box: the 4 boxs which also intersect the rows and cols of this box,
	 *      ie the two boxs left/right, and the two boxs above/below,
	 *      or in ascii art (X is this box, #'s are effected boxs):
	 *            X##        .#.        ..#
	 *            #..   or   #X#   or   ..#
	 *            #..        .#.        ##X
	 * Row: the 3 boxs which intersect this row;
	 * Col: the 3 boxs which insersect this col;
	 * </pre>
	 * This is all static because it's the same for all grids, so you can get
	 * it without an instance of Grid.
	 * <p>
	 * EFFECTED_BOXS is used in Medusa3D and GEM, which both use ONLY the boxs,
	 * so rows and cols are populated but not (yet) used, mainly coz rows/cols
	 * have a pre-existing intersectingBoxs array that's preferred if you have
	 * the ARegion, but for me you need only it's index, and you get only
	 * indexes, which you read through Indexes.INDEXES, to get the ARegion's
	 * from grid.regions, which all looks pretty messy, but is very fast).
	 * Maybe I should read this array into each box's intersectingBoxs array,
	 * instead of leaving it null? No requirement to do so, yet.
	 * <p>
	 * Speed: I ran this ONCE to get the magic-numbers; now we just load them.
	 */
//	public static final int[] EFFECTED_BOXS = new int[NUM_REGIONS];
//	static {
//		// boxs: other boxs which also intersect rows/cols that intersect me.
//		for ( int i=0; i<REGION_SIZE; ++i ) {
//			// y and x are vertical and horizonal (NOT grid row and col).
//			int y = i / 3;
//			EFFECTED_BOXS[i] |= ISHFT[y];
//			EFFECTED_BOXS[i] |= ISHFT[y + 1];
//			EFFECTED_BOXS[i] |= ISHFT[y + 2];
//			int x = i % 3;
//			EFFECTED_BOXS[i] |= ISHFT[x];
//			EFFECTED_BOXS[i] |= ISHFT[x + 3];
//			EFFECTED_BOXS[i] |= ISHFT[x + 6];
//			// except myself
//			EFFECTED_BOXS[i] &= ~ISHFT[i];
//			// validate
//			assert VSIZE[EFFECTED_BOXS[i]] == 4;
//		}
//		// rows: intersectingBoxs
//		for ( int i=REGION_SIZE; i<FIRST_COL; ++i ) {
//			int y = i / 3;
//			EFFECTED_BOXS[i] |= ISHFT[y];
//			EFFECTED_BOXS[i] |= ISHFT[y + 1];
//			EFFECTED_BOXS[i] |= ISHFT[y + 2];
//			// validate
//			assert VSIZE[EFFECTED_BOXS[i]] == 3;
//		}
//		// cols: intersectingBoxs
//		for ( int i=FIRST_COL; i<NUM_REGIONS; ++i ) {
//			int x = i % 3;
//			EFFECTED_BOXS[i] |= ISHFT[x];
//			EFFECTED_BOXS[i] |= ISHFT[x + 3];
//			EFFECTED_BOXS[i] |= ISHFT[x + 6];
//			// validate
//			assert VSIZE[EFFECTED_BOXS[i]] == 3;
//		}
//	};
	// Magic-numbers are the results of the above code, for speed.
	public static final int[] EFFECTED_BOXS = {
		  78, 149, 291,  71, 142, 270,  29,  30,  60  // boxs
		, 56,  56,  56, 112, 112, 112, 224, 224, 224  // rows
		, 73, 146, 292,  73, 146, 292,  73, 146, 292  // cols
	};

	/**
	 * Get the indice of this cell ID.
	 *
	 * @param id the cell.id for which you seek the indice
	 * @return the indice of this Cell in grid.cells 0..80
	 */
	public static int indice(String id) {
		return ID_SCHEME.indice(id);
	}

	/**
	 * Read indices from a Space Separated Values string of cell-ids.
	 *
	 * @param idsSsv to read
	 * @return a new int[] containing the indices of the given cells.
	 */
	public static int[] indices(final String idsSsv) {
		final String[] idsA = idsSsv.split(" ");
		final int n = idsA.length;
		final int[] result = new int[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = indice(idsA[i]);
		}
		return result;
	}

	/**
	 * Get the index of this region ID.
	 *
	 * @param rid the region.id for which you seek the index
	 * @return the index of this region in grid.regions 0..26
	 */
	public static int regionIndex(String rid) {
		return ID_SCHEME.regionIndex(rid);
	}

	/**
	 * Return the region for this id.
	 *
	 * @param rid The region.id, eg "box 1" or "col H" or "row 9"
	 * @return the region with this id
	 */
	public ARegion region(final String rid) {
		return regions[ID_SCHEME.regionIndex(rid)];
	}

	/**
	 * Get an array of regions from CSV (Comma Separated Values) of names.
	 * <p>
	 * Note that this method is used extensively in the test-cases, so any
	 * problems here causes LOTS of test-cases to fail.
	 *
	 * @param csv Comma Separated Values of region.id's
	 * @return
	 */
	public ARegion[] regions(final String csv) {
		final String[] rids = csv.split(", *"); // region.id's
		final int n = rids.length;
		final ARegion[] result = new ARegion[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = regions[ID_SCHEME.regionIndex(rids[i])];
		}
		return result;
	}

	/**
	 * Repopulate result with CSV (Comma Separated Values) of region-ids.
	 * <p>
	 * Note that this method is used extensively in the test-cases, so any
	 * problems here causes LOTS of test-cases to fail.
	 *
	 * @param csv of region.id's
	 * @param result to repopulate
	 * @return number of regions added, which may be less than result.length
	 */
	public int regions(final String csv, final ARegion[] result) {
		final String[] rids = csv.split(", *"); // region.id's
		final int n = rids.length;
		for ( int i=0; i<n; ++i ) {
			result[i] = regions[ID_SCHEME.regionIndex(rids[i])];
		}
		return n;
	}

	/** A java.util.Random for private use. */
	private static final Random RANDOM = new Random();

	/** This exception is exhaustively thrown by canNotBe, and we need ONE
	 * instance, and I do NOT care about the stack trace, so here it is. */
	private static final UnsolvableException NO_MACS = new UnsolvableException();

	// ============================= instance land ============================

	/**
	 * A single-dimensional array of the cells in this grid.
	 */
	public final Cell[] cells = new Cell[GRID_SIZE];

	/**
	 * An array of the 9 Boxs in this grid, indexed 0=top-left across and
	 * then down to 8=bottom-right.
	 */
	public final Box[] boxs = new Box[REGION_SIZE];

	/**
	 * An array of the 9 Rows in this grid, indexed 0=topmost down
	 * to 8=bottommost.
	 */
	public final Row[] rows = new Row[REGION_SIZE];

	/**
	 * An array of the 9 Cols in this grid, indexed 0=leftmost across
	 * to 8=rightmost.
	 */
	public final Col[] cols = new Col[REGION_SIZE];

	/**
	 * An array of all the regions in this grid, indexed 9 Boxs,
	 * then 9 Rows, then 9 Cols; sub-order same as boxs, rows, and cols.
	 */
	public final ARegion[] regions = new ARegion[NUM_REGIONS];

	/**
	 * indices of cells in this Grid which maybe each value 1..9.
	 */
	public final IdxL[] idxs = new IdxL[VALUE_CEILING];

	/**
	 * The sequential number of the hint that we're up-to in solve (et al).
	 */
	public int hintNumber = 1; // anything but 0, the default int value.

	/**
	 * This switch is set to true when the maybes (the potential cell values)
	 * are loaded from the input file, otherwise the loader eliminates loaded
	 * cell values from the maybes. Copy/paste saves/loads maybes for debugging
	 * so that you can copy the grids-string from the debugger (or wherever)
	 * and paste it "as is" into a Sudoku Solver GUI to see any problems.
	 */
	public boolean isMaybesLoaded;

	/**
	 * The source (File and lineNumber for MagicTours Multi-puzzle format).
	 */
	public SourceID source;

	/**
	 * cache the SolutionHint (in a list just coz that's the return type).
	 * used by {@link diuf.sudoku.solver.LogicalSolver#getSolution}.
	 */
	public List<AHint> solutionHints;

	/**
	 * solution contains the correct cell values of the puzzle in this Grid.
	 * Set by {@link diuf.sudoku.solver.checks.BruteForce#solve} and read
	 * by {@link diuf.sudoku.solver.hinters.Validator#isValid} (et al).
	 */
	public int[] solution;

	/**
	 * pid is short for puzzle-identifier. The pid identifies the puzzle that's
	 * loaded into this grid. It's just a random long that is set whenever a
	 * new grid is created or a puzzle is loaded, so that caching methods can
	 * tell if the grid still contains "this puzzle".
	 * <p>
	 * As far as I can see (not far) Random.nextLong() guarantees that two
	 * successive calls won't return the same value. If it does then
	 * AAHdkAlsHinter.valid will report false-positives (invalid hints).
	 */
	public long pid;

	/** isInvalidated()'s message explaining why this grid is invalid. */
	public String invalidity;

	/** isInvalidated()'s region which is invalid. Nullable. */
	public ARegion invalidRegion;

	/** TooFewClues remembers this grid is valid. */
	public boolean enoughClues;

	/** TooFewValues remembers this grid is valid. */
	public boolean enoughValues;

	/** prepare sets grid.isPrepared, to avert unnecessary repetitions. */
	private volatile boolean isPrepared;

	/** The number of filled (value!=0) cells. */
	public int numSet;

	/** The total number of maybes in the grid. */
	public int totalSize;

	/** Has a puzzle been loaded into this grid, or is it still a virgin. */
	private boolean isLoaded;

	// ----------------------------- constructors -----------------------------

	/**
	 * Construct a new empty 9x9 Sudoku grid.
	 */
	public Grid() {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			cells[i] = new Cell(i, VALL);
		}
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			idxs[v] = IdxL.full();
		}
		initialise();
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
	 * C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt<br></i></tt><br>
	 * Note that I've replaced back-slashes with slashes coz Java hates them!
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
	 * <tt><i>C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt</i></tt><br>
	 * Note: I've replaced back-slashes with slashes coz Java hates them!<br>
	 * contains the source of the puzzle to enable a partially solved puzzle to
	 * report itself as its ACTUAL source puzzle. So for example say I discover
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
	 * The constructor also reads hysterical text-files of 9/18 lines:<ul>
	 * <li>first block contains 9 lines of 9 clues (given cells values); and
	 * <li>optional second block contains 9 lines of 9 potential values strings
	 * <li>but don't count on this actually working, it just might, that's all
	 * </ul>
	 *
	 * @param line String the puzzle to load, probably in toString format. */
	public Grid(final String line) {
		// splits on \r\n or \r or \n
		this(MyStrings.splitLines(line));
	}

	/**
	 * Construct a new 9x9 Sudoku Grid, and load the puzzle in 'lines'.
	 *
	 * @param lines
	 */
	private Grid(final String[] lines) {
		this(); // run the default constructor first
		load(lines); // just call load rather than re-implement it (badly)
	}

	/**
	 * The copy constructor creates a new "exact" copy of the 'src' Grid. Cell
	 * values and maybes are copied, as are each regions idxOf, emptyCellCount,
	 * and containsValue arrays.
	 * <p>
	 * If the given src is null then a new empty grid is constructed.
	 *
	 * @param src the source Grid to copy from.
	 */
	public Grid(final Grid src) {
		assert src != null; // techie only error, prod tries to handle it.
		int i;
		if ( src != null ) {
			for ( i=0; i<GRID_SIZE; ++i ) {
				this.cells[i] = new Cell(src.cells[i]);
			}
		} else {
			for ( i=0; i<GRID_SIZE; ++i ) {
				this.cells[i] = new Cell(i, VALL);
			}
		}
		// build regions, crossingBoxs, cells.siblings, cells.notSees
		// , regions.idx, puzzleID
		initialise();
		// copy src.regions: ridx, idxs, emptyCellCount, containsValue
		// overwrites the regions.idxs, but ____ it.
		if ( src != null ) {
			for ( i=0; i<NUM_REGIONS; ++i ) {
				this.regions[i].copyFrom(src.regions[i]);
			}
			for ( i=1; i<VALUE_CEILING; ++i ) {
				this.idxs[i] = new IdxL(src.idxs[i]);
			}
			if ( src.source != null ) { // nullable, apparently
				this.source = new SourceID(src.source); // SourceID immutable
			}
			this.numSet = src.numSet;
			this.totalSize = src.totalSize;
		}
	}

	// Constructor assistant: Does NOT rely on state: cells values or maybes.
	// I just wire up the physical relationships between Grids component parts,
	// like putting each cell in it's regions, and finding the crossings, and
	// the "sibling" relationships, and creating indexes. This method is run
	// ONCE only, when the grid is being constructed.
	private void initialise() {
		// populate the grids regions arrays
		int i, x, y;  Row row;  Col col;
		// create boxs first so that rows and cols can intersectingBoxs them.
		for ( i=0; i<REGION_SIZE; ++i ) {
			regions[i] = boxs[i] = new Box(i);
		}
		for ( i=0; i<REGION_SIZE; ++i ) {
			regions[i+REGION_SIZE] = rows[i] = new Row(i);
			regions[i+FIRST_COL] = cols[i] = new Col(i);
		}
		// populate the row and col crossingBoxes arrays.
		// NB: the boxes must pre-exist for us to wire them up.
		for ( i=0; i<REGION_SIZE; ++i ) {
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
		for ( final Cell cell : cells ) {
			// get the siblings
			cell.siblings = cell.buds.cells(cells);
			// populate notSees ONCE, instead of negating it a TRILLION times!
			Arrays.fill(cell.sees, false);
			Arrays.fill(cell.notSees, true);
			for ( Cell sib : cell.siblings ) {
				cell.sees[sib.i] = true;
				cell.notSees[sib.i] = false;
			}
		}
		// grid.puzzleID tells hinters "we've changed puzzles".
		pidReset();
		hintNumberReset();
	}

	// ------------------------------- plumbing -------------------------------

	/** Get a String of the source-file and lineNumber of this grid. */
	public String source() {
		if ( source==null ) {
			return "IGNOTO";
		}
		return source.toString();
	}

	/**
	 * Set this.pid to a random long.
	 * @return a random long that serves as this puzzles identity.
	 */
	public long pidReset() {
		return pid = RANDOM.nextLong();
	}

	/** The start hint number is 1, hintNumber is 1-based. */
	public void hintNumberReset() {
		hintNumber = 1; // the first hint
		// invalidate all of grids caches, the others can take there chances
//		maybesHN = -1;
		emptiesHN = -1;
		bivsHN = -1;
	}
	/** ++hintNumber */
	public void hintNumberIncrement() {
		++hintNumber;
	}

	// Logical Solver needs to keep track of whether or it's prepared to solve
	// this grid, and the easiest way to do that is stick an attribute on the
	// grid which presumes that it hasn't been prepared yet.
	public boolean isPrepared() {
		return isPrepared;
	}
	public void setPrepared(final boolean isPrepared) {
		this.isPrepared = isPrepared;
	}

	// -------------------------- demi-constructors ---------------------------

	/**
	 * Copy the contents of the given 'src' grid into this Grid.
	 * <p>
	 * @see #Grid(Grid) the copy constructor.
	 *
	 * @param src Grid
	 */
	public void copyFrom(final Grid src) {
		int i;
		for ( i=0; i<GRID_SIZE; ++i ) {
			cells[i].copyFrom(src.cells[i]);
		}
		for ( i=0; i<NUM_REGIONS; ++i ) {
			regions[i].copyFrom(src.regions[i]);
		}
		for ( i=1; i<VALUE_CEILING; ++i ) {
			idxs[i].set(src.idxs[i]);
		}
		numSet = src.numSet;
		totalSize = src.totalSize;
	}

	/** Copy this grid to the O/S clipboard in toString() format. */
	public void copyToClipboard() {
		String s = this.toString();
		if (source!=null && source.file!=null) {
			s += NL + source;
		}
		GridClipboard.copyTo(s);
	}

	// ------------------------------ mutators --------------------------------

	public void clear() {
		for ( Cell cell : cells ) {
			cell.clear();
		}
	}

	/**
	 * Load this grid from these 1, 2 or 3 lines, in the format produced by
	 * the {@link #toString} method.
	 * <p>
	 * The test-cases use load because load INTENTIONALLY does <b>NOT</b> call
	 * {@link #rebuildMaybesAndS__t} to fix invalid maybes; thus each puzzle is
	 * loaded "as-is" (even broken), but I still {@link #rebuildAllMyOtherS__t}
	 * because one must have ones s__t together, I just don't ____ with the
	 * maybes beforehand.
	 *
	 * @param lines expect 1, 2, or 3 lines: 1=values, 2=maybes, 3=SourceID
	 * @return success.
	 */
	public boolean load(final String lines) {
		// splits on \r\n or \r or \n
		return load(MyStrings.splitLines(lines));
	}
	public boolean load(final List<String> lines) {
		return load(lines.toArray(new String[lines.size()]));
	}

	/**
	 * Load this grid from these 1, 2 or 3 lines in a {@code String[]}
	 * containing a puzzle.
	 * <p>
	 * If maybes are supplied we load exactly the given puzzle; we don't
	 * fill in the missing maybes.
	 * <p>
	 * Example lines:<tt><pre>
	 * 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
	 * ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
	 * 1#C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt
	 * </pre></tt><br>
	 * Note that I've replaced back-slashes with slashes coz Java hates them!
	 * <p>
	 * load is used by:<ul>
	 * <li>the constructor/s, and
	 * <li>{@link #copyToClipboard}, and
	 * <li>{@link diuf.sudoku.gui.GridLoader.MagicTourFormat#load}
	 * <li>and ALL the JUnit test-cases.
	 * </ul>
	 *
	 * @param lines expect 1, 2, or 3 lines: 1=values, 2=maybes, 3=SourceID
	 * @return success.
	 */
	public boolean load(final String[] lines) {
		// give up immediately if there's no chance of a sucessful load
		if ( lines==null || lines.length==0 ) {
			return false; // meaning load failed
		}
		try {
			// if this Grid already contains a puzzle then clean it out.
			if ( this.isLoaded ) {
				this.clear();
				this.isLoaded = false;
				this.isMaybesLoaded = false;
				// run these validations ONCE per puzzle
				this.enoughClues = false;
				this.enoughValues = false;
			}
			// values: first line contains 81 cell values:
			// 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
			// any runtime exception here is thrown over my head
			loadCellValues(lines[0]);
			// ---------------------------------------------------------------
			// but if maybes or source fails we keep going anyway.
			// ---------------------------------------------------------------
			String line = null;
			try {
				// second line (if any) is CSV of 81 maybes
				// ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
				if ( lines.length>=2 && (line=lines[1])!=null
				  // validate with a regex (slow but worth it).
				  && CLUES_PATTERN.matcher(line).matches()
				) {
					loadMaybes(line.split(COMMA, GRID_SIZE));
					isMaybesLoaded = true;
				} else {
					for ( Cell cell : cells ) {
						cell.initMaybes(); // fill if empty, else clear
					}
					// note that cell-values are removed from siblings down in
					// rebuildAllMyOtherS__t, along with indexes, sizes, etc.
				}
			} catch (Exception ex) {
				StdErr.carp("WARN: Grid.load: bad maybes: "+line, ex);
			}
			try {
				// third line (if any) is a SourceID
				// 1#C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt
				if ( lines.length>=3 && (line=lines[2])!=null ) {
					source = SourceID.parse(line);
				}
			} catch (Exception ex) {
				StdErr.carp("WARN: Grid.load: bad source: "+line, ex);
			}
			// remove any bad maybes (sees a cell set this value)
			if ( !isMaybesLoaded ) {
				rebuildRemoveInvalidMaybes();
			}
			// rebuild the emptyCellCount arrays
			rebuildAllRegionsEmptyCellCounts();
			// rebuild the indexes
			rebuildIndexes(true);
			countSetCells(); // sets numSet
			totalSize(); // sets totalSize
			// update the puzzleID to say the puzzle has changed
			pidReset();
			hintNumberReset();
			// this Grid has now finished loading
			isLoaded = true;
			return true; // meaning load succeeded
		} catch (Exception ex) {
			StdErr.whinge("Grid.load critical", ex);
			isLoaded = true;
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
	public void restore(final String s) {
		// line 1: 81 cell values ('.' means 0)
		// optional line 2: comma seperated list of 81 maybes-values-sets
		if ( s.length() < GRID_SIZE ) {
			throw new IllegalArgumentException("bad s=\""+s+"\"");
		}
		if ( isLoaded ) {
			clear();
		}
		loadCellValues(s.substring(0, GRID_SIZE));
		final String ss = s.substring(GRID_SIZE).trim();
		if ( ss.isEmpty() ) {
			rebuildMaybes();
		} else {
			loadMaybes(ss.split(COMMA, GRID_SIZE));
		}
		// rebuilds everything (except maybes)
		rebuild();
	}

	/**
	 * The {@link Backup} class and this restore method are faster than
	 * the old toString method and restore.
	 *
	 * @param backup
	 */
	public void restore(final Backup backup) {
		Cell c;
		final int[] vs = backup.values;
		final int[] ms = backup.maybes;
		for ( int i=0; i<GRID_SIZE; ++i ) {
			(c=cells[i]).value = vs[i];
			size[i] = c.size = VSIZE[maybes[i] = c.maybes = ms[i]];
		}
		countSetCells(); // sets numSet
		totalSize(); // sets totalSize
		rebuildAllRegionsEmptyCellCounts();
		rebuildIndexes(true);
	}

	// load the given n clues into n cells, where max n is 81.
	private boolean loadCellValues(final String line) {
		int i;
		char c;
		final int n = MyMath.min(line.length(), GRID_SIZE);
		for ( i=0; i<n; ++i ) {
			if ( (c=line.charAt(i))>='1' && c<='9' ) {
				cells[i].value = c - '0';
			}
		}
		return n == GRID_SIZE; // even if line chopped (optimistic)
	}

	private void loadMaybes(final String[] fields) {
		int i = 0;
		for ( final Cell cell : cells ) {
			cell.setMaybes(Values.parse(fields[i++]));
		}
	}

//KEEP: KRC 2019-09-09: Used to create test cases.
//	boolean saveInToStringFormatTo(final File file) {
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
	 * rebuild the ____ out of everything (except the Maybes):<ul>
	 * <li>{@link #rebuildRemoveInvalidMaybes}<ul>
	 *  <li>{@link Cell#removeFromMySiblingsMaybes}<ul>
	 *   <li>{@link Cell#canNotBe}<ul>
	 *    <li>{@link Cell#removeMaybes}<ul>
	 *     <li>{@link Grid#idxs}[v].{@link IdxL#removeOK}
	 *     <li>{@link Cell#removeMeFromMyRegionsIdxsOfValue}<ul>
	 *      <li>{@link ARegion#idxs}[v].{@link Idx#remove} for box/row/col
	 *     </ul>
	 *     <li>{@link Cell#removeMeFromMyRegionsIndexesOfValue}<ul>
	 *      <li>{@link ARegion#ridx}[v].{@link Indexes#remove} for box/row/col
	 *     </ul>
	 *     <li>sets {@link Cell#maybes}, {@link Cell#size} and {@link Grid#totalSize}
	 *    </ul>
	 *   </ul>
	 *  </ul>
	 * <li>{@link #rebuildAllRegionsS__t}<ul>
	 *  <li>{@link #rebuildAllRegionsEmptyCellCounts}<ul>
	 *   <li>{@link ARegion#emptyCellCount}
	 *   </ul>
	 *  <li>{@link #rebuildAllRegionsContainsValues}<ul>
	 *   <li>{@link ARegion#containsValue()}
	 *   </ul>
	 *  </ul>
	 * </ul>
	 * <li>{@link #rebuildIndexes}<ul>
	 *  <li>{@link #rebuildAllRegionsIndexsOfAllValues}<ul>
	 *   <li>NOT HERE {@link #rebuildAllRegionsContainsValues}<ul>
	 *    <li>{@link ARegion#containsValue()}
	 *    </ul>
	 *   <li>{@link ARegion#rebuildIndexsOfAllValues}
	 *   </ul>
	 *  </ul>
	 * </ul>
	 * <p>
	 * 50 lines of documentation for a two line method!<br>
	 * Now you see why I <b>HATE</b> documentation?<br>
	 * Q: How long do you think it'll be before it's out of date?<br>
	 * A: It was out of date BEFORE I'd finished writing it! sigh.
	 */
	public void rebuild() {
		rebuildRemoveInvalidMaybes();
		rebuildAllRegionsS__t();
		rebuildIndexes(false);
	}

	/**
	 * rebuildMaybes does:<ul>
	 *  <li>foreach cell in {@link Grid#cells}<br>
	 *   if {@link Cell#value} == 0<br>
	 *    {@link Cell#setMaybes}({@link Values.VALL} & ~{@link Cell#seesValues()})<br>
	 *   else<br>
	 *    {@link Cell#clearMaybes()}
	 * </ul>
	 */
	public void rebuildMaybes() {
		for ( Cell cell : cells ) {
			if ( cell.value == 0 ) {
				cell.setMaybes(VALL & ~cell.seesValues());
			} else {
				cell.clearMaybes();
			}
		}
	}

	/**
	 * Rebuild all of the maybes and everything, from scratch.
	 * <p>
	 * rebuildMaybesAndS__t does:<ul>
	 *  <li>{@link rebuildMaybes}
	 *  <li>{@link rebuildAllRegionsS__t}
	 * </ul>
	 */
	public void rebuildMaybesAndS__t() {
		// reset the maybes
		rebuildMaybes();
		// rebuild all the s__t in all the regions
		rebuildAllRegionsS__t();
	}

	/**
	 * rebuildRemoveInvalidMaybes does:<ul>
	 *  <li>foreach cell in {@link Grid#cells}<br>
	 *   if {@link cell#value} != 0<br>
	 *   {@link Cell#removeFromMySiblingsMaybes}(cell.value)
	 * </ul>
	 */
	public void rebuildRemoveInvalidMaybes() {
		for ( final Cell cell : cells ) {
			if ( cell.value != 0 ) {
				cell.removeFromMySiblingsMaybes(cell.value);
			}
		}
	}

	/**
	 * rebuildAllRegionsS__t does:<ul>
	 *  <li>{@link #rebuildAllRegionsEmptyCellCounts}<ul>
	 *   <li>{@link ARegion#emptyCellCount}
	 *  </ul>
	 *  <li>{@link #rebuildAllRegionsContainsValues}<ul>
	 *   <li>{@link ARegion#containsValue()}
	 *  </ul>
	 * </ul>
	 */
	public void rebuildAllRegionsS__t() {
		rebuildAllRegionsEmptyCellCounts();
		rebuildAllRegionsContainsValues();
	}

	/**
	 * rebuildAllRegionsEmptyCellCounts does:<ul>
	 *  <li>foreach region in {@link Grid#regions}<br>
	 *   {@link ARegion#emptyCellCount()}<br>
	 *   which sets {@link ARegion#emptyCellCount}
	 * </ul>
	 */
	public void rebuildAllRegionsEmptyCellCounts() {
		for ( int i=0; i<NUM_REGIONS; ++i ) {
			regions[i].emptyCellCount(); // which sets r.emptyCellCount
		}
	}

	/**
	 * rebuildAllRegionsContainsValues does:<ul>
	 *  <li>foreach region in {@link Grid#regions}<br>
	 *   {@link ARegion#containsValue()}
	 * </ul>
	 */
	public void rebuildAllRegionsContainsValues() {
		for ( int i=0; i<NUM_REGIONS; ++i ) {
			regions[i].containsValue();
		}
	}

	/**
	 * rebuildAllRegionsIndexsOfAllValues does:<ul>
	 *  <li>if doContainsValues<br>
	 *   <li>{@link rebuildAllRegionsContainsValues}
	 *  <li>foreach region in {@link Grid#regions}<br>
	 *   <li>{@link ARegion#rebuildIndexsOfAllValues}
	 * </ul>
	 * @param doContainsValues
	 */
	public void rebuildAllRegionsIndexsOfAllValues(boolean doContainsValues) {
		// containsValue used in rebuildIndexsOfAllValues
		if ( doContainsValues ) {
			rebuildAllRegionsContainsValues();
		}
		for ( final ARegion r : regions ) { // there are 27 regions
			r.rebuildIndexsOfAllValues();
		}
	}

	/**
	 * rebuildIdxs does:<ul>
	 *  <li>foreach possible value<br>
	 *   {@link Grid#idxs}[v].{@link IdxL#clearOK}()
	 *  <li>foreach cell foreach maybe<br>
     *   {@link Grid#idxs}[v].{@link IdxL#addOK}(cell.i)
	 * </ul>
	 */
	public void rebuildIdxs() {
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			idxs[v].clearOK();
		}
		for ( int i=0,m; i<GRID_SIZE; ++i ) {
			if ( (m=maybes[i]) != 0 ) { // ie cell is not set
				for ( int v : VALUESES[m] ) {
					idxs[v].addOK(i);
				}
			}
		}
	}

	/**
	 * rebuildAllRegionsIdxsOfAllValues does:<ul>
	 *  <li>{@link rebuildIdxs}
	 *  <li>foreach cell foreach maybe<br>
	 *   {@link ARegion#rebuildIdxsOfAllValues}
	 * </ul>
	 */
	public void rebuildAllRegionsIdxsOfAllValues() {
		rebuildIdxs();
		for ( final ARegion r : regions ) {
			r.rebuildIdxsOfAllValues(); // reads idxs
		}
	}

	/**
	 * rebuildIndexes does:<ul>
	 *  <li>{@link #rebuildAllRegionsIndexsOfAllValues(boolean doContainsValues)}
	 *  <li>{@link #rebuildAllRegionsIdxsOfAllValues}<ul>
	 *   <li>{@link #rebuildIdxs}<ul>
	 *    <li>{@link Grid#idxs}[v].{@link IdxL#clearOK()}
	 *    <li>{@link ARegion#rebuildIdxsOfAllValues}<ul>
	 *     <li>{@link ARegion#idxs}[v].{@link Idx#setAnd}(idx, {@link Grid#idxs}[v])
	 *    </ul>
	 *    <li>{@link Grid#idxs}[v].{@link Idx#addOK}(cell.i)
	 *   </ul>
	 *  </ul>
	 * </ul>
	 * @param doContainsValues should we rebuildAllRegionsContainsValues
	 */
	public void rebuildIndexes(boolean doContainsValues) {
		rebuildAllRegionsIndexsOfAllValues(doContainsValues);
		rebuildAllRegionsIdxsOfAllValues(); // does rebuildIdxs
	}

	/**
	 * Remove potential values rendered illegal by current cell values.
	 * <p>
	 * This is part of {@link diuf.sudoku.gui.SudokuExplainer#candidateTyped},
	 * ie only in GUI.
	 * <p>
	 * cancelMaybes does:<ul>
	 *  <li>foreach cell in {@link Grid#cells}<br>
	 *   {@link Cell#cancelMaybes}
	 * </ul>
	 */
	public void cancelMaybes() {
		for ( final Cell cell : cells ) {
			cell.cancelMaybes();
		}
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

	/**
	 * hasMissingMaybes returns true if any unset cell in this grid has no
	 * potential values remaining, else false.
	 *
	 * @return Returns true, and set the invalidity message, if an empty cell
	 * has no potential values, else return false.
	 */
	public boolean hasMissingMaybes() {
		for ( final Cell cell : cells ) {
			if ( cell.value==0 && cell.size==0 ) {
				invalidity = cell.id+" has no potential values remaining";
				return true;
			}
		}
		return false;
	}

	/**
	 * firstDoubledValue returns the first cell.value which appears twice in
	 * any region in this grid, else 0 meaning none.
	 *
	 * @return 0 (valid) else (invalid) the first cell-value that appears
	 *  multiple times in any region in this grid, setting invalidity and
	 *  invalidRegion. Note that there (silently) may be multiple repeated
	 *  values, and there may be other regions having multiple values.
	 */
	public int firstDoubledValue() {
		int seen, sv;
		for ( final ARegion region : regions ) {
			seen = 0;
			for ( final Cell cell : region.cells ) {
				if ( cell.value != 0 ) {
					if ( (seen & (sv=VSHFT[cell.value])) == 0 ) {
						seen |= sv;
					} else {
						invalidity = "Multiple "+cell.value+"'s in "+region;
						invalidRegion = region;
						return cell.value;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * hasHomelessValues returns true if any region in this grid has no place
	 * remaining for any unplaced value, else false.
	 *
	 * @return true if any unplaced value has no place remaining in any region,
	 *  and sets the invalidity message and invalidRegion.
	 */
	public boolean hasHomelessValues() {
		for ( final ARegion r : regions ) {
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				if ( r.ridx[v].size==0 && !r.containsValue[v] ) {
					invalidity = "The value "+v+" has no place in "+r.id;
					invalidRegion = r;
					return true;
				}
			}
		}
		return false;
	}

	// --------------------------- stateful queries ---------------------------

	/**
	 * Get the cell at this cell.id, for the test-cases.
	 *
	 * @param id to get: "A1".."I9"
	 * @return the Cell, or an exception for a bad id.
	 */
	public Cell get(final String id) {
		return cells[ID_SCHEME.indice(id)];
	}

	/**
	 * Get Cell[]'s for ids, for the test-cases.
	 * <p>
	 * prefer getCSV, which is the toString format. I'm here if you've already
	 * got an array of id's.
	 *
	 * @param ids varargs of cell.id's: "A1", "I9"
	 * @return Cell[]
	 */
	public Cell[] get(final String... ids) {
		final int n = ids.length;
		final Cell[] array = Cells.arrayA(n);
		for ( int i=0; i<n; ++i ) {
			array[i] = get(ids[i]);
		}
		return array;
	}

	/**
	 * Get Cell[]'s for this CSV list of cell.id, for test-cases.
	 *
	 * @param csv of cell.id's: "A1, I9"
	 * @return Cell[]
	 */
	public Cell[] getCsv(final String csv) {
		return get(csv.split(" *, *"));
	}

	/**
	 * Get a set of cells from CSV of cell-ids. Only used by test-cases.
	 *
	 * @param idsCsv CSV of cell-ids
	 * @return a new {@code MyLinkedHashSet<Cell>}
	 */
	public MyLinkedHashSet<Cell> getCsvSet(final String idsCsv) {
		return new MyLinkedHashSet<>(getCsv(idsCsv));
	}

	/**
	 * Returns a new array of the cells at the given indices.
	 *
	 * @param indices to fetch
	 * @return a new Cell[] of cells at indices
	 */
	public Cell[] cells(final int[] indices) {
		final int n = indices.length;
		final Cell[] result = new Cell[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = cells[indices[i]];
		}
		return result;
	}

	/**
	 * Currently only used for logging, to monitor progress.
	 * @return Count the number of cells with value!=0
	 */
	public int countSetCells() {
		int cnt = 0;
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( cells[i].value != 0 ) {
				++cnt;
			}
		}
		return numSet = cnt;
	}

	/**
	 * Currently only used to log "has grid changed" after hint applied.
	 * @return Count the total number of maybes of all cells in this grid.
	 */
	public int totalSize() {
		int sum = 0;
		for ( int i=0; i<GRID_SIZE; ++i ) {
			sum += cells[i].size;
		}
		return totalSize = sum;
	}

	/**
	 * Count the number of occurrences of each cell value in this grid.
	 * <p>
	 * Currently only used by the Fisherman to workout whether or not we should
	 * be fishing for each value.
	 * @return int[VALUE_CEILING] occurrences.
	 */
	public int[] countOccurances() {
		final int[] occurances = new int[VALUE_CEILING];
		for ( final Cell cell : cells ) {
			++occurances[cell.value];
		}
		return occurances;
	}

	/**
	 * Returns CAS_A array of Idx'd cells in this grid.
	 *
	 * @param a0 first Idx element
	 * @param a1 second Idx element
	 * @param a2 third Idx element
	 * @return these cells from this grid
	 */
	public Cell[] cellsA(final int a0, final int a1, final int a2) {
		int w, count=0, n=bitCount(a0)+bitCount(a1)+bitCount(a2);
		final Cell[] result = Cells.arrayA(n);
		if ( a0 != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(a0>>w)&WORD_MASK] )
					result[count++] = cells[w+i];
		if ( a1 != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(a1>>w)&WORD_MASK] )
					result[count++] = cells[BITS_PER_ELEMENT+w+i];
		if ( a2 != 0 )
			for ( w=0; w<BITS_PER_ELEMENT; w+=BITS_PER_WORD )
				for ( int i : WORDS[(a2>>w)&WORD_MASK] )
					result[count++] = cells[BITS_TWO_ELEMENTS+w+i];
		return result;
	}

	// ---------------------------- IS_HACKY stuff ----------------------------

	/**
	 * hackTop1465() is a HACK controller: It returns: is Settings.isHacky and
	 * the grid.source (ie the file we're processing) startsWith "top1465".
	 * @return isHacky && source.filename startsWith top1465
	 */
	public boolean hackTop1465() {
		return THE_SETTINGS.getBoolean(Settings.isHacky)
			&& source != null
			&& source.isTop1465;
	}

	/**
	 * Get a {@code Deque<Single>} to stash singles.
	 *
	 * @return a queue for stashing singles
	 */
	public Deque<Single> getSinglesQueue() {
		if ( singlesQueue == null ) {
			singlesQueue = new MyLinkedList<>();
		} else {
			singlesQueue.clear();
		}
		return singlesQueue;
	}
	private Deque<Single> singlesQueue;

	// --------------------------- toString and s__t --------------------------

	// ---------------- toString and friends ----------------

	// append cell values to 'sb' (with '.' as 0)
	private StringBuilder appendCellValues(final StringBuilder sb) {
		for ( final Cell cell : cells ) {
			if ( cell != null ) { // just in case we're still initialising
				sb.append(DIGITS[cell.value]);
			}
		}
		return sb;
	}

	// append string-value of 'maybes' to 'sb'
	private static void appendMaybes(final StringBuilder sb, final int maybes) {
		for ( int v : VALUESES[maybes] ) {
			sb.append(DIGITS[v]);
		}
	}

	/**
	 * @return a String representation of the values in this grid, being a
	 * single line of 81 cell values, with 0's (empty cells) represented by a
	 * period character (.).
	 * <p>For example:<pre>
	 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
	 * </pre>
	 */
	public String toShortString() {
		return appendCellValues(new StringBuilder(GRID_SIZE)).toString();
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
	 *
	 * @return a String representation of this Grid.
	 */
	@Override
	public String toString() {
		if ( cells[0] == null ) { // under construction
			return "constructing";
		}
		if ( numSet > 80 ) { // grid full
			return toShortString();
		}
		// append the maybes to the same SB used by toShortString
		// max observed 447 (conceptis's hardest Sudoku puzzle);
		final StringBuilder sb = new StringBuilder(512);
		appendCellValues(sb).append(NL);
		appendMaybes(sb, maybes[0]);
		for ( int i=1; i<GRID_SIZE; ++i ) {
			appendMaybes(sb.append(','), maybes[i]);
		}
		return sb.toString();
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Compare two grids for equality on cell values and maybes. O(81).
	 *
	 * @param o Object - for other
	 * @return true if this Grid contains the same values and maybes as 'o',
	 *  else false.
	 */
	@Override
	public boolean equals(Object o) {
		return o!=null && (o instanceof Grid) && equals((Grid)o);
	}
	// WARNING: presumes the other Grid is NOT null, for speed!
	public boolean equals(final Grid other) {
		final Cell[] otherCells = other.cells;
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( cells[i].value != otherCells[i].value
			  || cells[i].maybes != otherCells[i].maybes ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Calculates the hashCode value for this Grid, derived from cell values
	 * and maybes, so same hashCodes means probably but not definitely equals,
	 * and two grids that are not equals almost certainly have different
	 * hashCodes, but two grids holding exactly the same puzzle have the same
	 * hashCode.
	 * <p>
	 * NB: This hashCode relies on mutable values, which is a bad idea, but we
	 * never mutate a Grid that is used as Hash-keys, so there is no problem.
	 * In fact I can't think of anything that uses a Grid as a hash-key!
	 *
	 * @return the hashCode of this Grid.
	 */
	@Override
	public int hashCode() {
		Cell c;
		int hc = 0;
		// only the last 32 cells appear in the hashCode
		for ( int i=GRID_SIZE-32; i<GRID_SIZE; ++i ) {
			hc = hc<<1 ^ (c=cells[i]).value ^ c.maybes;
		}
		return hc;
	}

	// ------------------------- Ayla the arrayonator -------------------------

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
		for ( final Cell cell : cells ) {
			cell.arrayonateShiftedMaybes();
		}
	}

	// used by Aligned2Exclusion, Aligned3Exclusion, Aligned4Exclusion.
	public void disarrayonateMaybes() {
		for ( final Cell cell : cells ) {
			cell.disarrayonateMaybes();
		}
	}

	// ------------------------------- maybes ---------------------------------

//not_used: the maybes array is now public, maintained by Cell.
//	/**
//	 * Get a cached int[] containing the maybes of each cell in this grid.
//	 *
//	 * @return cached int[GRID_SIZE] containing a bitset of the potential
//	 *  values of each cell in this grid.
//	 */
//	public int[] maybes() {
//		if ( maybesHN!=hintNumber || maybesPid!=pid ) {
//			if ( maybes == null ) {
//				maybes = new int[GRID_SIZE];
//			}
//			// fetch
//			for ( int i=0; i<GRID_SIZE; ++i ) {
//				maybes[i] = cells[i].maybes;
//			}
//			maybesHN = hintNumber;
//			maybesPid = pid;
//		}
//		return maybes;
//	}
//	private int[] maybes;
//	private int maybesHN; // hintNumber
//	private long maybesPid;  // puzzleID
	public final int[] maybes = new int[GRID_SIZE];
	public final int[] size = new int[GRID_SIZE];

	/**
	 * Used only in assert, to check grid.maybes equals cell.maybes.
	 * There seems to be differences when there should be none.
	 *
	 * @return do grid.maybes equals the cell.maybes.
	 */
	public boolean maybesCheck() {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( maybes[i] != cells[i].maybes ) {
				return false;
			}
		}
		return true;
	}

	// -------------------------------- Idx's ---------------------------------
	// NB: My at methods have been replaced by Idx.cells methods.

	/**
	 * Get an Idx of all empty (value == 0) cells in this grid.
	 * @return a CACHED Idx of all empty cells in this grid.
	 */
	public Idx getEmpties() {
		if ( ( emptiesIdx == null // first time for this Grid
			&& (emptiesIdx=new IdxL()) != null )
		  || ( (emptiesHN != hintNumber || emptiesPid != pid)
			&& emptiesIdx.unlock().clear() != null )
		) {
			for ( Cell c : cells ) {
				if ( c.value == 0 ) {
					emptiesIdx.add(c.i);
				}
			}
			emptiesHN = hintNumber;
			emptiesPid = pid;
			emptiesIdx.lock();
		}
		return emptiesIdx;
	}
	private IdxL emptiesIdx;
	private int emptiesHN;
	private long emptiesPid;

	/**
	 * Returns a new Idx of empty cells that are accepted by the
	 * {@code IFilter<Cell>},
	 * which is typically implemented by a lambda expression.
	 * <p>
	 * I'm inordinately proud of this method. I reckon it's pretty clever.
	 * The techies who implemented closures are bloody geniuses.
	 *
	 * @param f typically a lambda expression implementing IFilter<Cell> accept,
	 *  but any implementation of IFilter<Cell> will do nicely. Remember that
	 *  all cells passed to your filter are empty, ie have value == 0
	 * @return a new Idx (not the cached empties Idx) containing filtered
	 *  indices
	 */
	public Idx getEmptiesWhere(IFilter<Cell> f) {
		return getEmpties().where(cells, f);
	}

	/**
	 * Add indices of cells in this grid that're accepted by IFilter<Cell> 'f'
	 * to the 'result' Idx.
	 *
	 * @param result the Idx to add to
	 * @param f the {@code IFilter<Cell>} whose accept method determines the
	 *  cells to be added
	 * @return the result Idx, so that you can pass me a 'new Idx()'
	 */
	public Idx idx(IFilter<Cell> f, Idx result) {
		for ( Cell c : cells ) {
			if ( f.accept(c) ) {
				result.add(c.i);
			}
		}
		return result;
	}

	/**
	 * Get an Idx of cells in this grid with maybesSize == 2.
	 * @return a CACHED Idx of bivalue cells in this grid.
	 */
	public Idx getBivalue() {
		return getBivalueImpl(false);
	}

	/**
	 * Get an Idx of cells in this grid with maybesSize == 2.
	 * <p>
	 * NOTE WELL: For speed, I was using a lambda to fetch bivalue cells, but
	 * apparently even this simple task is beyond me, coz for reasons I cannot
	 * fathom (probably some smart-ass internal cache) the lambda finds cells
	 * whose value has recently been set, so it all goes into a tail spin. So
	 * lambda's don't play nice with recursion, so I revert to ASAP: As Simple
	 * As Possible. You can stick Soon up your jaxie.
	 *
	 * @param force should we force the fetch?
	 * @return a CACHED Idx of bivalue cells in this grid.
	 */
	public Idx getBivalueImpl(boolean force) {
		if ( bivs == null ) {
			bivs = new IdxL();
			readBivs();
			bivs.lock();
			bivsHN = hintNumber;
			bivsPid = pid;
		} else if ( bivsHN!=hintNumber || bivsPid!=pid || force ) {
			bivs.unlock();
			readBivs();
			bivs.lock();
			bivsHN = hintNumber;
			bivsPid = pid;
		}
		return bivs;
	}
	private IdxL bivs; // indices of bivalue cells in this grid
	private int bivsHN;
	private long bivsPid;

	private void readBivs() {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( cells[i].size == 2 ) {
				// if it's rooted then rebuild and go again from the top
				if ( cells[i].value != 0 ) {
					lastChanceBivs();
					return;
				}
				bivs.add(i);
			}
		}
	}

	// rebuild and try again before "I just want my Mom!"
	private void lastChanceBivs() {
// WARN: rooted hinter sends puzzle invalid, so I restore maybes -> rooted ->
// restored -> rooted -> Oh bugger! Traffic lights.
//		rebuildMaybes();
		rebuild();
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( cells[i].size == 2 ) {
				// if it's rooted then this time it's REALLY rooted!
				if ( cells[i].value != 0 ) {
					throw new UnsolvableException("I just want my Mom!");
				}
				bivs.add(i);
			}
		}
	}

	/**
	 * Returns a new array of the value of each of the 81 cells in this Grid.
	 *
	 * @return a new int[GRID_SIZE] of cell values
	 */
	public int[] values() {
		return values(new int[GRID_SIZE]);
	}

	/**
	 * Populate the given array with the value of each Cell in this Grid.
	 *
	 * @param values the array to populate
	 * @return the given values array, so that you can create it on-the-fly.
	 */
	public int[] values(final int[] values) {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			values[i] = cells[i].value;
		}
		return values;
	}

//not_used 2021-09-15 07:47
//	/**
//	 * Returns a new array of the maybes of each of the 81 cells in this Grid.
//	 *
//	 * @return a new int[GRID_SIZE] of maybes bitsets
//	 */
//	public int[] toMaybesArrayNew() {
//		return maybes(new int[GRID_SIZE]);
//	}

	/**
	 * Populate the given array with the maybes of each cell in this Grid.
	 *
	 * @param result the array to populate
	 * @return the passed maybes array, so that you can create it on-the-fly.
	 */
	public int[] maybes(final int[] result) {
		for ( int i=0; i<GRID_SIZE; ++i ) {
			result[i] = maybes[i];
		}
		return result;
	}

	/**
	 * Get a pots of each cell in this Grid which maybe v. This is used by the
	 * SudokuGridPanel when the user holds-down the left mouse button over a
	 * row-legend to highlight those potential cell values.
	 *
	 * @param value
	 * @return a new Pots containing the cells which maybe v in this Grid.
	 */
	public Pots getCandidatePots(final int value) {
		final Pots result = new Pots(64, 1.0F);
		final int sv = VSHFT[value];
		final Integer integer = sv;
		for ( int i=0; i<GRID_SIZE; ++i ) {
			if ( (maybes[i] & sv) != 0 ) {
				result.put(cells[i], integer);
			}
		}
		return result;
	}

	/**
	 * Get a pots of each cell in the grid by maybesSize. This is used by the
	 * SudokuGridPanel when the user holds-down the middle/right mouse button
	 * over a row-legend, whose value is used as $size 1..9:
	 * <pre>
	 * If $size in 1..5 highlight cells with maybesSize == $size.
	 * If $size in 6..8 highlight cells with maybesSize in 2..$size-3
	 *                  so 6=2..3, 7=2..4, 8=2..5.
	 * If $size == 9 then you're a cheating mofo!
	 * </pre>
	 *
	 * @param size the number of maybes in cells to highlight: 1..5 is exact,
	 *  6..9 is 2..size-3
	 * @return a new Pots containing the cells which maybe v in this Grid.
	 */
	public Pots getMaybesSizePots(final int size) {
		final Pots result = new Pots(64, 1.0F);
		if ( size < 6 ) {
			// cells with size == $size
			for ( Cell cell : cells ) {
				if ( cell.size == size ) {
					result.put(cell, cell.maybes);
				}
			}
		} else if ( size < 9 ) {
			// cells with size in 2..$size-2 EXCLUSIVE
			final int ceiling = size - 2; // 6=4, 7=5, 8=6
			for ( Cell cell : cells ) {
				if ( cell.size>1 && cell.size<ceiling ) {
					result.put(cell, cell.maybes);
				}
			}
		} else {
			// size == 9: you're a cheating mofo!
			getSolution();
			if ( solution != null ) { // ignore Exception
				for ( Cell cell : cells ) {
					if ( cell.value == 0 ) {
						result.put(cell, VSHFT[solution[cell.i]]);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Get a cached array containing the correct value of each cell in this
	 * grid.
	 *
	 * @return the solution array of 81 correct cell values.
	 */
	public int[] getSolution() {
		if ( solution == null ) { // a hint has not been gotten yet
			try {
				// getSolution sets solutionValues as a by-product
				LogicalSolverFactory.get().getSolution(this);
			} catch (Exception eaten) {
				// Do nothing
			}
		}
		return solution;
	}

	/**
	 * Returns a new {@code Set<Cell>} containing cells which have the wrong
	 * value, ie where the set value does not match the calculated solution.
	 *
	 * @return a {@code Set<Cell>} containing cells with bad values, else null
	 *  meaning that there are no cells with bad values.
	 */
	public Set<Cell> getWrongens() {
		getSolution(); // cached
		Set<Cell> wrongens = null;
		if ( solution != null ) { // ignore getSolution failed
			for ( final Cell cell : cells ) {
				if ( cell.value!=0 && cell.value!=solution[cell.i] ) {
					if ( wrongens == null) {
						wrongens = new HashSet<>();
					}
					wrongens.add(cell);
				}
			}
		}
		return wrongens;
	}

//not_used: speed: replaced below
//	/**
//	 * Returns the other ARegion that is common to all cells. Cells is expected
//	 * to contain two or three cells, which CAN have one "other" common region.
//	 * <p>
//	 * A List of less than two Cells always returns null coz "common" makes no
//	 * sense.
//	 * <p>
//	 * A list of four-or-more cells always returns null coz four cells can have
//	 * only one common region, so an "other" common region cannot exist.
//	 *
//	 * @param cells
//	 * @param region
//	 * @return the other ARegion common to all cells.
//	 */
//	public static ARegion otherCommonRegion(final List<Cell> cells, final ARegion region) {
//		ARegion result = null;
//		final int n = cells.size();
//		if ( n>1 && n<7 ) {
//			final List<ARegion> crs = Regions.common(cells, new ArrayList<>(2));
//			if ( crs.size() == 2 ) {
//				result = crs.get(0);
//				if ( result == region ) {
//					result = crs.get(1);
//				}
//			}
//		}
//		return result;
//	}

//not_used
//	/**
//	 * Get the regions of this type-index from this grid.
//	 *
//	 * @param rti regionTypeIndex
//	 * @return an array of ARegion of this type
//	 */
//	public ARegion[] getRegions(final int rti) {
//		switch (rti) {
//		case BOX: return boxs;
//		case ROW: return rows;
//		case COL: return cols;
//		}
//		throw new IllegalArgumentException("Unkown regionTypeIndex="+rti);
//	}

	// ============================ the Cell class ===========================

	/**
	 * Compares Cells by id ASCENDING.
	 * For test-cases only.
	 */
	public final static Comparator<Cell> BY_ID = new Comparator<Cell>() {
		@Override
		public int compare(final Cell a, final Cell b) {
			return a.id.compareTo(b.id);
		}
	};

	/**
	 * The public inner Cell class represents one of the 81 cells in a Sudoku
	 * Grid. It's reason for being is to remember stuff, not really do stuff.
	 * <p>
	 * A Cell holds:<ul>
	 * <li>i is my index in the Grid.cells array
	 * <li>id is my {@code $columnChar$rowNumber} A1..I9
	 * <li>value: 1..9 or 0 if the cell is empty
	 * <li>maybes: (aka potential values) A final but mutable Values
	 * <li>y is my (vertical) coordinate in the Grid.matrix
	 * <li>x is my (horizontal) coordinate in the Grid.matrix
	 * <li>boxIndex and b: I am {@code grid.boxs[boxIndex].cells[b]}
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
	 * I also maintain my Regions {@code ridx[value]} to expedite many of
	 * the hinters. I've accrued a lot of methods over time, especially for a
	 * class that "doesn't really do stuff". Don't panic.
	 */
	public final class Cell implements Comparable<Cell> {

		/**
		 * i is my indice in the grid.cells array, a single index array that is
		 * used to hold a matrix of cells: 9 rows of 9 cells.
		 * <pre>
		 * So: {@code i = y * 9 + x}
		 * and: {@code y = i / 9} where y is my vertical (row) index
		 * and: {@code x = i % 9} and x is my horizontal (col) index
		 * NOTE: In Sudoku Explainer the term "indice" always means a cells
		 * index in Grid.cells, to differentiate it from all other indexes in
		 * the codebase, because indices are used heavily, so they're special.
		 * </pre>
		 */
		public final int i;

		/**
		 * id is a String that uniquely identifies this Cell in the Grid.
		 * In IdScheme.Chess that's column-letter and row-number: A1..I9.
		 */
		public final String id;

		/**
		 * the "value" of this cell 1..9; 0 means this cell is "empty".
		 */
		public int value;

		/**
		 * the potential values of this cell; named for "A1 may be 2".
		 */
		public int maybes;

		/**
		 * the number of potential values of this cell.
		 */
		public int size;

		/**
		 * boxIndex and b: I am {@code grid.boxs[boxIndex].cells[b]}. <br>
		 * so: {@code boxIndex = (y/3*3) + (x/3)}
		 * <p>
		 * NOTE: The grid.boxs array is coincident with the first 9 elements in
		 * the grid.regions array. The boxs array is of type Grid.Box, where-as
		 * the regions array is of Grid.ARegion; Box has a few fields, like
		 * this one, that are not in ARegion because they're not relevant to
		 * Row or Col.
		 */
		public final int boxIndex;

		/**
		 * boxIndex and b: I am {@code grid.boxs[boxIndex].cells[b]}. <br>
		 * so: {@code b = (y%3*3) + (x%3)}
		 */
		public final int b;

		/**
		 * y is my vertical (first) index in the cells matrix. <br>
		 * also my index in my col.cells array.
		 * <p>
		 * x,y verses row,col where x=col and row=y: There's only 2 possible
		 * orders, and we habitually use both of them, ergo just shoot me!
		 */
		public final int y;

		/**
		 * x is my horizontal (second) index in the cells matrix. <br>
		 * also my index in my row.cells array.
		 * <p>
		 * x,y verses row,col where x=col and row=y: There's only 2 possible
		 * orders, and we habitually use both of them, ergo just shoot me!
		 */
		public final int x;

		/**
		 * indexInRegion is my index in my regions cells array (not to be
		 * confused with my indice in my regions idx, which I've just done).
		 */
		public final int[] indexIn = new int[3];

		/**
		 * My idxdex is the index of my element in the idx array.
		 * <pre>
		 * so: {@code idxdex = i/27}
		 * </pre>
		 * see: shft field for more info.
		 */
		public final int idxdex; // 0..2

		/**
		 * My idxshft is the left-shifted bitset-value of my index within my
		 * idx array element.
		 * <pre>
		 * so:{@code idxshft = 1<<(i%27);}
		 * so: Cell c is present in Idx idx{@code if ( (idx[c.idxdex] & c.idxshft) != 0 )}
		 * these two example lines of "user code" contrast before and after:
		 * was:{@code idx[c.i/27] |= 1<<(c.i%27); // add cell to idx}
		 * now:{@code idx[c.idxdex] |= c.idxshft; // add cell faster}
		 * idxdex and idxshft are used only by LinkedMatrixCellSet.idx() that's
		 * hammered by Aligned*Exclusion, saving 28 secs in A234E per top1465.
		 * To be clear, idxdex and idxshft exist only for speed, because it is
		 * faster to calculate these values ONCE and store them, than it is to
		 * recalculate these values trillions of times.
		 * </pre>
		 */
		public final int idxshft;

		/**
		 * the 3 regions which contain this Cell: 0=box, 1=row, 2=col.
		 */
		public final ARegion[] regions = new ARegion[3]; // set by Grid constructor

		/**
		 * hashCode identifies cell location = y&lt;&lt;4+x (8 bits).
		 */
		public final int hashCode;

		/**
		 * skip prevents useless assumptions being made in chaining.
		 */
		public boolean skip;

		/**
		 * This one's weird: arrayonateShiftedMaybes() populates shiftedMaybes
		 * with the left-shifted-bitset-values of this cells maybes.
		 * See {@link Values} regarding the bitset format.
		 */
		public int[] shiftedMaybes;

		/**
		 * notSees boolean[]: Does this Cell NOT see the given Cell?
		 * Coincident with Grid.cells.
		 * <p>
		 * RE the not: In Aligned*Exclusion we must act if this cell is NOT a
		 * sibling of other cell; so rather than negate repeatedly with
		 * {@code boolean ns01 = !c0.sees[i1]} we {@code c0.notSees[i1];}
		 * just to save a poofteenth of a nanosecond per flip, which add-up
		 * over top1465. Doing ANYthing a squintillion times takes a while.
		 */
		public final boolean[] notSees = new boolean[GRID_SIZE];

		/**
		 * sees boolean[]: Does this Cell see the given Cell?
		 * Coincident with Grid.cells.
		 */
		public final boolean[] sees = new boolean[GRID_SIZE];

		/**
		 * The indices of the 20 cells in same box, row, and col as this Cell,
		 * excepting this cell itself. Note that {@link Grid#BUDDIES} contains
		 * the same Idx's, to save you getting the cell to get it's buds.
		 */
		public final IdxL buds; // set in constructor

		/**
		 * An array of my set of 20 sibling cells.
		 */
		public Cell[] siblings;

		/**
		 * {@code public Map<String,Object> properties = null;} gives you a
		 * way to add any additional properties you require. Create a
		 * {@code new HashMap<String,Object>(8, 0.75F)} and attach it here.
		 * <p>
		 * Please document your properties <b>here</b> as well as where you're
		 * using them.
		 */
		public Map<String,Object> properties = null;

		// NB: If it were possible these would be final, but either the cells
		// or the regions must be created first and I've chosen cells first.
		/**
		 * The Box which contains this Cell.
		 */
		public Box box;
		/**
		 * The Row which contains this Cell.
		 */
		public Row row;
		/**
		 * The Col which contains this Cell.
		 */
		public Col col;

		/**
		 * Construct a new Cell in this grid.
		 * <b>NOTE</b> the parameters are "logical" x, y (not "physical" y, x).
		 * <b>NOTE</b> each cell has an intrinsic reference to its containing
		 * grid, whether you like it or not, so a reference to a Cell holds the
		 * whole Grid, so clean-up after yourself, or you'll OOME-out. Sigh.
		 *
		 * @param i the indice of this cell the grid.cells array; <br>
		 *  so my y coordinate (row) is i/9, <br>
		 *  and my x coordinate (col) is i%9, <br>
		 *  so I am grid.cells[y*9 + x], <br>
		 *  or simply grid.cells[c.i]
		 * @param cands a bitset of the potential values of this cell. <br>
		 *  For an empty grid pass me VALL=511="111111111"="123456789". <br>
		 *  For the copy constructor pass me the source-cells maybes.
		 */
		public Cell(final int i, final int cands) {
			// set the id first, for the toString method (a cheat)
			this.id = CELL_IDS[this.i = i];
			// y is my vertical (row) coordinate (0=top, 8=bottom),
			// nb: my indexIn[COL] is my row coordinate!
			this.y = this.indexIn[COL] = i / N;
			// x is my horizontal (col) coordinate (0=left, 8=right),
			// nb: my indexIn[ROW] is my col coordinate!
			this.x = this.indexIn[ROW] = i % N;
			// b is my index in my box.cells (0=top-left, 8=bottom-right)
			this.b = this.indexIn[BOX] = (y%R*R) + (x%R);
			// boxIndex is my boxs index in the Grid.boxs array, so I will be
			// boxs[boxIndex].cells[b] once regions are created (chicken/egg).
			this.boxIndex = (y/R*R) + (x/R);

			// super-charge LinkedMatrixCellSet.idx() with:
			//     a[n.cell.idxdex] |= n.cell.shft; // add n.cell.i to idx
			// coz it's faster than calculating idxdex and idxshft repeatedly
			this.idxdex = i/BITS_PER_ELEMENT;
			this.idxshft = 1<<(i%BITS_PER_ELEMENT);

			this.hashCode = Hash.LSH4[y] ^ x; // see also Ass.hashCode()
			totalSize += size = VSIZE[maybes = cands];
			Grid.this.size[i] = size;
			Grid.this.maybes[i] = maybes;
			// set buds last, for the toString method (a cheat)
			this.buds = Grid.BUDDIES[i];
		}

		/**
		 * Copy Constructor: Constructs a new Cell that is a "clone" of src.
		 *
		 * @param src the cell to copy.
		 */
		public Cell(final Cell src) {
			this(src.i, src.maybes);
			this.value = src.value;
		}

		/**
		 * Copy the cell-value and maybes from the 'src' cell.
		 *
		 * @param src the cell to copy.
		 */
		public void copyFrom(final Cell src) {
			value = src.value;
			Grid.this.maybes[i] = maybes = src.maybes;
			Grid.this.size[i] = size = src.size;
		}

		/**
		 * In Grid.load, having set all values, we clean-up the maybes.
		 * This is the first step in doing so:
		 * if the cell is empty I "fill" his maybes,
		 * otherwise I "clear" them.
		 * <p>
		 * <b>NOTE</b>: filling empty cells is unnecessary for new grids, but
		 * it is necessary after loading into a previously-used grid.
		 */
		private void initMaybes() {
			if ( value == 0 ) {
				setMaybes(VALL);
			} else {
				setMaybes(0);
			}
		}

		/**
		 * setMaybes sets the maybes, there size, and updates the totalSize.
		 *
		 * @param cands the maybes to be set
		 */
		private void setMaybes(final int cands) {
			// out with the old, and in with the new
//			totalSize += -VSIZE[maybes] + (size=VSIZE[maybes=cands]);
			totalSize += -size + (size=VSIZE[maybes=cands]);
			Grid.this.maybes[i] = maybes;
			Grid.this.size[i] = size;
		}

		/**
		 * Remove me (this Cell) from my box.idxs[v], row.idxs[v], and
		 * col.idxs[v].
		 *
		 * @param v
		 */
		private void removeMeFromMyRegionsIdxsOfValue(final int v) {
			box.idxs[v].remove(i);
			row.idxs[v].remove(i);
			col.idxs[v].remove(i);
		}

		/**
		 * Remove all of the cands from this cells box, row, and col idxs.
		 *
		 * @param cands
		 */
		void removeMeFromMyRegionsIdxsOfBits(final int cands) {
			for ( int v : VALUESES[cands] ) {
				removeMeFromMyRegionsIdxsOfValue(v);
			}
		}

		/**
		 * Remove me (this Cell) from my box.ridx[v], row.ridx[v], and
		 * col.ridx[v].
		 *
		 * @param v
		 */
		private void removeMeFromMyRegionsIndexesOfValue(final int v) {
			box.ridx[v].remove(b);
			row.ridx[v].remove(x);
			col.ridx[v].remove(y);
		}

		/**
		 * Remove all of the cands from this cells box, row, and col ridx.
		 *
		 * @param pinkos
		 */
		void removeMeFromMyRegionsIndexesOfBits(final int pinkos) {
			for ( int v : VALUESES[pinkos] ) {
				removeMeFromMyRegionsIndexesOfValue(v);
			}
		}

		/**
		 * Remove all of the pinkos from this cells box, row, and col ridx,
		 * adding any subsequent hidden singles to the singles queue, from
		 * whence {@link AHint#applyImpl} retrieves and sets each cell.
		 *
		 * @param pinkos bitset of values to be removed
		 * @param singles to which I add any hidden singles found
		 */
		private void removeMeFromMyRegionsIndexesOfBits(final int pinkos, final Deque<Single> singles) {
			for ( int v : VALUESES[pinkos] ) {
				if ( box.ridx[v].remove(b) == 1 ) {
					singles.add(new Single(box.cells[IFIRST[box.ridx[v].bits]], v));
				}
				if ( row.ridx[v].remove(x) == 1 ) {
					singles.add(new Single(row.cells[IFIRST[row.ridx[v].bits]], v));
				}
				if ( col.ridx[v].remove(y) == 1 ) {
					singles.add(new Single(col.cells[IFIRST[col.ridx[v].bits]], v));
				}
			}
		}

		/**
		 * Remove cands from this cells maybes, and update {@link #size} and
		 * the {@link Grid#totalSize}; and indexes {@link Grid#idxs},
		 * {@link ARegion#ridx}, and {@link ARegion#idxs}.
		 *
		 * @param cands to be removed
		 * @return the updated size
		 */
		private int removeMaybes(final int cands) {
			final int removed = cands & maybes;
			if ( removed != 0 ) {
				for ( int v : VALUESES[removed] ) {
					idxs[v].removeOK(i);
					removeMeFromMyRegionsIdxsOfValue(v);
					removeMeFromMyRegionsIndexesOfValue(v);
				}
				size = VSIZE[maybes &= ~removed];
				totalSize -= VSIZE[removed];
				Grid.this.maybes[i] = maybes;
				Grid.this.size[i] = size;
			}
			return size;
		}

		/**
		 * Add me (this Cell) back into the idxs[v] of my box, row, and col.
		 *
		 * @param v
		 */
		public void addMeToMyRegionsIdxsOfValue(final int v) {
			box.idxs[v].add(i);
			row.idxs[v].add(i);
			col.idxs[v].add(i);
		}

		/**
		 * Add me (this Cell) back into the ridx[v] of my box, row, and col.
		 *
		 * @param v
		 */
		private void addMeToMyRegionsIndexesOfValue(final int v) {
			box.ridx[v].add(b);
			row.ridx[v].add(x);
			col.ridx[v].add(y);
		}

		/**
		 * Add cands to this cells maybes, and update {@link #size} and
		 * the {@link Grid#totalSize}; and indexes {@link Grid#idxs},
		 * {@link ARegion#ridx}, and {@link ARegion#idxs}.
		 *
		 * @param cands to be added
		 * @return the updated size
		 */
		private int addMaybes(final int cands) {
			final int added = cands & ~maybes;
			if ( added != 0 ) {
				for ( int v : VALUESES[added]) {
					idxs[v].addOK(i);
					addMeToMyRegionsIdxsOfValue(v);
					addMeToMyRegionsIndexesOfValue(v);
				}
				size = VSIZE[maybes |= added];
				totalSize += VSIZE[added];
				Grid.this.maybes[i] = maybes;
				Grid.this.size[i] = size;
			}
			return size;
		}

		/**
		 * Remove all of the existing maybes from this cell.
		 *
		 * @return
		 */
		public int clearMaybes() {
			return removeMaybes(maybes);
		}

		/**
		 * Set this cells value to 0 (empty), and fill it's maybes.
		 */
		private void clear() {
			value = 0;
			setMaybes(VALL);
		}

		/**
		 * Just set the cells value minimally in the batch, with no GUI-focused
		 * complications: no formerValue, no autosolve, and no SB; which works
		 * out to be bugger-all faster (sigh), but it is (arguably) simpler.
		 * <p>
		 * If you're reverting a cells value in the GUI then it's up to YOU to
		 * call the full set method (below): this one will bugger it up!
		 *
		 * @param value the cells value to be set.
		 * @return 1 - the number of cells set.
		 */
		public int set(final int value) {
			// this set method is a pencil, not a rubber
			assert value != 0;
			if ( value == 0 ) {
				return 0; // ignore
			}
			// cell.value is a WORM (Write ONCE Read Many)
			if ( this.value != 0 ) {
				// but apply multiple hints can set a cell multiple times
				if ( value == this.value ) {
					return 0; // ignore
				}
				// cannot change the value of a set cell
				throw new UnsolvableException(id+"="+this.value+" not "+value);
			}
			this.value = value;
			++numSet;
			clearMaybes();
			removeFromMySiblingsMaybes(value);
			decrementMyRegionsEmptyCellCounts();
			addValueToMyRegionsContainsValues(value);
			skip = false;
			return 1;
		}

		/**
		 * Set the value of this cell, and remove that value from the potential
		 * values of each of my sibling cells; and remove this position from
		 * each of my three regions possible positions of my potential values.
		 *
		 * @param value to set.
		 * @param formerValue just pass me the cell.value and don't worry, this
		 *  won't hurt!
		 * @param isAutosolving should subsequent naked and hidden singles be
		 *  sought and set?
		 * @param sb nullable StringBuilder to which (when not null) I append
		 *  an "and therefore list", being any Naked/Hidden Singles that're set
		 *  as a subsequence of setting the given cell value.
		 *  Currently not-null only in HintsApplicumulator.
		 * @return the number of cells actually set. 1 normally, or 0 if this
		 *  the given value is the same as this cells value.
		 */
		public int set(final int value, final int formerValue
				, final boolean isAutosolving, final StringBuilder sb) {
			if ( value==this.value && size==0 ) {
				if ( maybes != 0 ) {
					clearMaybes();
					rebuild();
				}
				return 0;
			}
			this.value = value;
			clearMaybes();
			if ( value == 0 ) { // the user cleared a cell in the GUI
				assert !isAutosolving;
				addMeBackIntoMySiblingsMaybes(formerValue);
				resetMaybes();
				rebuildAllRegionsS__t();
			} else {
				removeFromMySiblingsMaybes(value);
				decrementMyRegionsEmptyCellCounts();
				addValueToMyRegionsContainsValues(value);
				++numSet;
			}
			skip = false; // Chains: I am NOT an unapplied naked/hidden single
			// we're done if we're not autosolving
			int count = 1; // 1 for me
			// Autosolve: Seek and set any consequent singles.
			if ( isAutosolving ) {
				Cell sib;
				int j, v, first;
				for ( ARegion r : regions ) {
					// look for any subsequent naked singles
					for ( j=0; j<REGION_SIZE; ++j ) {
						if ( r.cells[j].size == 1 ) {
							if ( (v=VFIRST[(sib=r.cells[j]).maybes]) < 1 ) {
								Log.teeln(Log.me()+": Nargle S__t!");
								sib.clearMaybes();
								continue;
							}
							if ( sb != null ) {
								sb.append(CSP).append(sib.id).append(EQUALS).append(v);
							}
							count += sib.set(v, 0, true, sb);
						}
					}
					// look for any subsequent hidden singles
					for ( v=1; v<VALUE_CEILING; ++v ) {
						if ( r.ridx[v].size == 1 ) {
							if ( (first=IFIRST[r.ridx[v].bits]) < 0 ) {
								Log.teeln(Log.me()+": Hyena S__t!");
								r.ridx[v].clear();
								continue;
							}
							if ( (sib=r.cells[first]) != this ) { // oops!
								if ( sb != null ) {
									sb.append(CSP).append(sib.id).append(EQUALS).append(v);
								}
								count += sib.set(v, 0, true, sb);
							}
						}
					}
				}
			}
			return count;
		}

//debug: dump this cells three regions ridx ALL values to stdout
//		void dumpRIVs() {
//			final StringBuilder sb = new StringBuilder(128);
//			String s;
//			for ( final ARegion r : Cell.this.regions ) {
//				sb.setLength(0);
//				sb.append(r.id);
//				for ( int v=1; v<VALUE_CEILING; ++v ) {
//					s = r.ridx[v].toString();
//					s += "         ".substring(0, VALUE_CEILING-s.length());
//					sb.append(TAB).append(v).append(EQUALS).append(s);
//				}
//				System.out.println(sb);
//			}
//		}

		/**
		 * This set method is used by SudokuExplainer (the engine) to set the
		 * cells value, it caters for when the user changes there mind, and
		 * removes a value from a Cell.
		 *
		 * @param value the value the user just set, may be 0 meaning empty.
		 * @param formerValue the value that this cell holds currently.
		 * @return the number of cells set (no autosolve, so always 1 here).
		 */
		public int setManually(final int value, final int formerValue) {
			set(value, formerValue, Grid.NO_AUTOSOLVE, null);
			return 1;
		}

		/** Called ONLY by {@link Grid#cancelMaybes}. */
		private void cancelMaybes() {
			if ( value != 0 ) {
				clearMaybes();
				removeFromMySiblingsMaybes(value);
			}
		}

		/**
		 * Remove this value from the maybes of each of my 20 sibling cells.
		 *
		 * @param v the value to be removed
		 */
		public void removeFromMySiblingsMaybes(final int v) {
			if ( v == 0 ) { // actually happened.
				throw new UnsolvableException(Log.me()+": v=0");
			}
			final int sv = VSHFT[v];
			for ( Cell sib : siblings ) {
				if ( (sib.maybes & sv) != 0 ) {
					sib.canNotBe(v);
				}
			}
		}

		/** Decrements the emptyCellCounts of my box, row, and col. */
		public void decrementMyRegionsEmptyCellCounts() {
			--box.emptyCellCount;
			--row.emptyCellCount;
			--col.emptyCellCount;
		}

		/** Adds v to containsValue of my box, row, and col. */
		private void addValueToMyRegionsContainsValues(final int v) {
			box.containsValue[v] = true;
			row.containsValue[v] = true;
			col.containsValue[v] = true;
		}

		/**
		 * Remove the given value from the potential values of this cell,
		 * and also remove me from each of my regions ridx[v].
		 * <p>
		 * This method <b>does nothing</b> if this Cell is set, or if this Cell
		 * already cannot-be theValue. This is a efficiency/convenience for the
		 * removeMyValueFromMySiblingsMaybes() to just invoke me for each unset
		 * sibling, allowing me workout weather I have stuff to do.
		 * <p>
		 * 2019-09-02 efficiency: assert cell must maybe before it canNotBe.
		 *
		 * @param v between 1 and 9 inclusive.
		 */
		public void canNotBe(final int v) {
//			assert (this.maybes & VSHFT[v]) != 0;
			if ( removeMaybes(VSHFT[v]) == 0 ) { // returns updated size
//// use this for speed
//				throw NO_MACS;
// use this if you care about the StackTrace, when you're debugging
				throw new UnsolvableException("no macs remaining");
			}
		}

		/**
		 * The canNotBeBits method removes the pinkos from the maybes of this
		 * cell (me).
		 * <p>
		 * Note that this one's used only when autosolving! The other one
		 * (below) handles the "normal" case.
		 * <p>
		 * If singles is not null:<ul>
		 * <li>If this disenpinkenectomy leaves me Naked then I add myself to
		 * singles, from which AHint.apply retrieves me and sets my value.
		 * <li>If this disenpinkenectomy leaves me with maybes==0 then I throw
		 * an UnsolvableException.
		 * </ul>
		 * This method is called only by {@code AHint.apply} with these
		 * <b>PRECONDITIONS:</b><ul>
		 * <li>pinkos != 0, and
		 * <li>this.maybes != 0, and
		 * <li>they intersect: (pinkos & this.maybes) != 0
		 * </ul>
		 *
		 * @param pinkos a bitset of the values that I can not be
		 * @param singles is a {@code Deque<Single>} when isAutosolving (else
		 *  null) to which I add this cell if removing pinkos leaves me a Naked
		 *  Single (this cell has just one remaining potential value). I also
		 *  add sibling cells that become a Hidden Single (last remaining
		 *  possible position for a value in this region) when I remove myself
		 *  from the index of that pinko-value in each of my regions.
		 * @return the number of bits actually removed, so that we can track
		 *  numElims in the hint.apply method.
		 * @throws UnsolvableException if removing valuesBits leaves this cell
		 *  with no maybes. This happens pretty routinely in BruteForce,
		 *  but should be impossible elsewhere, so all other occurrences need
		 *  to be hunted down and at least explained, if not fixed.
		 */
		public int canNotBeBits(final int pinkos, final Deque<Single> singles) {
			assert pinkos != 0;
			assert this.maybes != 0;
			assert (this.maybes & pinkos) != 0;
			final int pre = maybes; // just for the exception message
			// this still required to populate singles!
			removeMeFromMyRegionsIndexesOfBits(pinkos, singles);
			// calculate number of maybes removed BEFORE removing them
			final int numRmvd = VSIZE[maybes & pinkos];
			// remove the pinkBits from the maybes of this cell, and
			// switch on the number of maybes now remaining in this cell
			switch ( removeMaybes(pinkos) ) { // returns the new size
			case 0: // I am rooted, so we fail early
				throw new UnsolvableException("No maybes: "+id+":"
					+Values.toString(pre)+" - "+Values.toString(pinkos));
			case 1: // I am a naked single, to be set by AHint.apply
				singles.add(new Single(this, VFIRST[maybes]));
			}
			return numRmvd;
		}

		/**
		 * Updates the maybes to remove the pinkos from the potential values of
		 * this cell; throwing an UnsolvableException if that leaves this cell
		 * with no potential values, so that BruteForce fails ASAP, which
		 * is critical to it's speed.
		 * <p>
		 * Note that this one's used only when NOT autosolving. The other one
		 * (above) handles autosolve.
		 *
		 * @param pinkos a bitset of the values to be removed
		 * @return the updated size of this cell
		 * @throws UnsolvableException if this cell has no maybes remaining
		 */
		public int canNotBeBits(final int pinkos) {
// just for the UnsolvableException message
			final int preMaybes = maybes;
			// calculate number of maybes removed BEFORE removing them
			final int numRmvd = VSIZE[maybes & pinkos];
			// remove the pinkBits from the maybes of this cell, and
			// switch on the number of maybes now remaining in this cell
			if ( removeMaybes(pinkos) == 0 ) { // returns the new size
//// use this one for speed
//				throw NO_MACS;
// debug: use this one, with the custom message and stack-trace
				throw new UnsolvableException("Empty maybes: "+id+":"
					+Values.toString(preMaybes)+" - "+Values.toString(pinkos));
			}
			return numRmvd;
		}

		/**
		 * canSoBe restores the given maybe to this Cell.
		 *
		 * @param v the maybe to restore
		 */
		public void canSoBe(final int v) {
			addMaybes(VSHFT[v]);
		}

		/**
		 * Called only by Cell.set to reset this cells maybes to all of it's
		 * potential values when my value is removed (only in the GUI).
		 */
		private void resetMaybes() {
			for ( int v : VALUESES[VALL & ~seesValues()] ) {
				canSoBe(v);
			}
		}

		/**
		 * Add this value back into the maybes of each of my 20 sibling cells,
		 * and also update each siblings regions ridx[value].
		 * <p>
		 * Only called by Cell.set when clearing a cell in the GUI.
		 *
		 * @param v
		 */
		public void addMeBackIntoMySiblingsMaybes(final int v) {
			if ( v == 0 ) { // actually happened
				return;
			}
			final int sv = VSHFT[v];
			for ( final Cell sib : siblings ) {
				if ( sib.value==0 && (sib.seesValues() & sv)==0 ) {
					canSoBe(v);
				}
			}
		}

//KEEP4DOC: not used: keep to show noobs how to JUST CALCULATE IT!
//		/**
//		 * Is the given value among this Cell's potential values?
//		 * <p>
//		 * It takes longer to invoke this method than the method takes, so we
//		 * don't invoke any method, unless of course we really want to and we
//		 * are not on the critical path, like when creating a hint.
//		 *
//		 * @param value
//		 * @return true if the potential values of this cell contain the given
//		 * value, else false.
//		 */
//		public boolean maybe(final int value) {
//			return (maybes & VSHFT[value]) != 0;
//		}

		/**
		 * Returns a bitset of the values held by my sibling cells.
		 *
		 * @return a bitset of the values held by my sibling cells.
		 */
		public int seesValues() {
			int bits = 0;
			for ( Cell sib : siblings ) {
				bits |= VSHFT[sib.value];
			}
			return bits;
		}

		/**
		 * Returns does this cells potential values contain 'v'?
		 * <p>
		 * I only exist for convenience. Don't hammer me. It's faster to check
		 * yourself with {@code (cell.maybes & VSHFT[v]) != 0}
		 *
		 * @param v the potential value to test for
		 * @return does this cells potential values contain 'v'
		 */
		public boolean maybe(final int v) {
			return (maybes & VSHFT[v]) != 0;
		}

		/**
		 * Returns a String representation of this cell: A1=5 or A2:3{368}.
		 * Used only for debugging: ie is NOT part of the GUI application or
		 * used in LogicalSolverTester.
		 * <p>
		 * NOTE: toString deals with being called while construction is still
		 * underway by relying on the 'id' field being the first to be set, and
		 * the 'buds' field being the last to be set, such that all you see is
		 * my 'id' until the constructor completes; then you see the full Cell
		 * definition: "id:size{maybes}" or "id=value" if set.
		 * <p>
		 * NOTE: toString IS used in the application (it is not, like most of
		 * my toString methods, only used for debugging).
		 *
		 * @return "${id}:${size}{Values.toString(maybes)}" <br>
		 *  or "${id}=${value}" if this Cell is set; <br>
		 *  or simply "${id}" while constructing.
		 */
		@Override
		public String toString() {
			if ( buds != null ) { // that last Constructor instruction
				return toFullString();
			}
			return id; // the first Constructor instruction
		}

		/**
		 * Returns a String representation of this cell: A1=5 or A2:3{368}.<br>
		 * Note that toFullString explicitly gets the full string. Currently
		 * toString returns the same, but don't rely on that not changing when
		 * you really want the full string, so you call me explicitly.
		 *
		 * @return "${id}:${size}{Values.toString(maybes)}" <br>
		 *  or "${id}=${value}" if this Cell is set.
		 */
		public String toFullString() {
			if ( value != 0 ) {
				return id+EQUALS+value;
			} else {
				return id+":"+size+"{"+Values.toString(maybes)+"}";
			}
		}

		/**
		 * Does this Cell identity-equals the given Object?
		 * <p>Note that both equals and hashCode use only immutable identity
		 * attributes, so the equals contract is upheld.
		 *
		 * @param o Object - other
		 * @return true if the given Object is a Cell at my x,y, else false.
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof Cell
				&& hashCode == ((Cell)o).hashCode; // y<<4^x (identity only)
		}

		/**
		 * Returns the hashCode of this cell, which uniquely identifying this
		 * Cell within its Grid.
		 * <p>
		 * Note that both equals and hashCode use only immutable identity
		 * attributes, so the equals contract is upheld.
		 *
		 * @return int {@code y<<4 + x}
		 */
		@Override
		public int hashCode() {
			return hashCode; // y<<4 + x
		}

		// If you don't know what you're doing then don't use me.
		// If you need to use this method anyway then good luck.
		// You MUST finally disarrayonateMaybes.
		private void arrayonateShiftedMaybes() {
			if ( maybes == 0 ) {
				shiftedMaybes = null;
			} else {
				shiftedMaybes = VSHIFTED[maybes];
			}
		}

		// If you don't know what you're doing then don't use me.
		private void disarrayonateMaybes() {
			shiftedMaybes = null;
		}

		/**
		 * Compare this cell to other: just compares the hashcode, which is
		 * {@code Hash.LSH4[y] ^ x}, ergo identity.
		 *
		 * @param other the Cell to compare my to
		 * @return -1 if this is before other, 1 if this is after other, or 0
		 *  if this equal to other, which will happen only if these are the
		 *  same cell, just possibly from different grids.
		 */
		@Override
		public int compareTo(final Cell other) {
			final int ohc = other.hashCode;
			if ( ohc < hashCode ) {
				return -1;
			}
			if ( ohc > hashCode ) {
				return 1;
			}
			return 0;
		}

		/**
		 * Add the index of this cells box, row, col to the result idx.
		 *
		 * @param result to add my regions indexes to.
		 */
		public void accrueRegions(final Idx result) {
			result.add(box.index);
			result.add(row.index);
			result.add(col.index);
		}

		/**
		 * Return the instance of Grid that contains this region.
		 *
		 * @return the grid that contains this region.
		 */
		public Grid getGrid() {
			return Grid.this;
		}
	}

	// ============================== The Regions =============================

	/**
	 * ARegion is an abstract region (a 3*3 Box, a Row, or a Col)
	 * containing 9 cells in this Sudoku Grid.
	 */
	public abstract class ARegion {

		/** My grid.regions array index 0..26. */
		public final int index;

		/** 0=Grid.BOX, 1=Grid.ROW, 2=Grid.COL. */
		public final int typeIndex;

		/** "box", "row", or "col". */
		public final String typeName;

		/**
		 * id is a String that uniquely identifies this region in the grid.
		 * <p>
		 * In {@link IdScheme#Chess} that's "${region.typeName} ${identifier}"
		 * where the identifier depends on the region-type, for Box and Row
		 * it's a number 1..9, but for Col it's a letter A..I. <br>
		 * Examples: "box 6", "row 2", or "col I".
		 */
		public final String id;

		/** The boxs which cross this row or col. Empty in a box. */
		public final Box[] crossingBoxs;

		/** The cells of this region left-to-right and top-to-bottom. */
		public final Cell[] cells = new Cell[REGION_SIZE];

		/**
		 * Indexes-in-this-region of cells which maybe value 1..9.
		 * An ridx is an Indexes, which contains indexes of the 9 cells in the
		 * region.cells array (ie not an Idx of the 81 grid.cells indices).
		 * <p>
		 * The motivation for a region having its own cells array and Indexes
		 * is to abstract-away the regions "shape"; so we can query a region
		 * without worrying about its type: row, col, or box; since they are
		 * all fairly similar... but this still requires some care.
		 */
		public final Indexes[] ridx = new Indexes[VALUE_CEILING];

		/**
		 * The grid.cells indices of cells in this region which maybe each
		 * potential value 1..9. (not to be confused with {@link #ridx}
		 * containing indexes in the ARegion.cells array).
		 * <p>
		 * SE NOMENCLATURE: the word indice always means a grid.cells indice;
		 * everything else is an index (or some other not-indice word). An idx
		 * always contains indices, everything else is an Index (or some other
		 * non-idx word).
		 */
		public final Idx[] idxs = new Idx[VALUE_CEILING];

		/** An immutable Idx of the indices of cells in this region. */
		public final IdxI idx;

		/** The number of cells with value==0 calculated by countEmptyCells()
		 * which is called by Grid.rebuildAllRegionsEmptyCellCounts(). */
		public int emptyCellCount = REGION_SIZE;

		/**
		 * Does this region contain this value. This field is set by the
		 * containsValue() method, which is called by the hasHomelessValues
		 * validator, at the start of solve and after each hint is applied;
		 * so all the hinters can just use this array. Setting this thing
		 * up is pretty confused in the rebuild process; and frankly I doubt
		 * I've got it straight in my head, so I'm shy of explaining it, but
		 * what I'm trying to achieve is this bastard is available to the
		 * hinters, and I don't care about the rebuild process, because it
		 * doesn't happen often enough to make it an issue, especially now
		 * that the maybes and indexes are maintained internally. So I'll
		 * shut the ___-up now and get on with breaking stuff.
		 */
		public final boolean[] containsValue = new boolean[VALUE_CEILING];

		/** The 3 boxs which intersect this row or col; null for box. */
		public final Box[] intersectingBoxs;

		/** Does this region intersect the other regions in the Grid. */
		public final boolean[] INTERSECTS;

		/**
		 * The Constructor. I'm a Box, or a Row, or a Col.
		 *
		 * @param index the index of this region in the Grid.regions array.
		 * @param typeIndex Grid.BOX, Grid.ROW, Grid.COL
		 * @param numCrossings the number of crossingBoxes: 0 for each Box,
		 *  3 for each Row and Col. Passing this in makes it possible for the
		 *  crossingBoxes array to be final.
		 */
		protected ARegion(final int index, final int typeIndex
				, final int numCrossings) {
			this.index = index;
			this.typeName = REGION_TYPE_NAMES[this.typeIndex = typeIndex];
			this.id = REGION_IDS[index];
			this.crossingBoxs = new Box[numCrossings];
			// get my idx: an IdxI (immutable) of cells in this region
			idx = REGION_IDXS[index];
			// create the ridx and idxs arrays elements
			for ( int v=1; v<VALUE_CEILING; ++v ) { // 0 is left null (in both)
				ridx[v] = new Indexes(); // cells in this region
				idxs[v] = Idx.of(REGION_IDXS[index]); // cells in this region
			}
			INTERSECTS = REGIONS_INTERSECT[index];
			final int[] ibSize = {0, 3, 3};
			intersectingBoxs = new Box[ibSize[typeIndex]];
		}

		// used by Grid's copyFrom
		// NOTE: Just to be clear, ALL mutable fields are copied; if you add
		// a new mutable field to ARegion/Box/Row/Col copy it here. The copy
		// constructor calls me. If you forget and you use this field in a
		// hinter then BruteForce fails, and Generate probably fails.
		private void copyFrom(final ARegion src) {
			// deep copy the ridx and idxs of all values 1..9
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				ridx[v].copyFrom(src.ridx[v]);
				idxs[v].set(src.idxs[v]);
			}
			emptyCellCount = src.emptyCellCount;
			// deep copy the containsValue array
			System.arraycopy(src.containsValue, 0, containsValue, 0, VALUE_CEILING);
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
		public Cell[] atNew(final int[] indexes) {
			final int n = indexes.length;
			final Cell[] array = new Cell[n];
			for ( int i=0; i<n; ++i ) {
				array[i] = this.cells[indexes[i]];
			}
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
		public Cell[] atNew(final int bits) {
			final int n = ISIZE[bits];
			final Cell[] array = new Cell[n];
			final int cnt = at(bits, array);
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
		public ArrayList<Cell> list(final int bits) {
			final ArrayList<Cell> list = new ArrayList<>(ISIZE[bits]);
			for ( int i : INDEXES[bits] ) {
				list.add(this.cells[i]);
			}
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
		public Cell[] at(final int bits, final boolean dummy) {
			final int n = ISIZE[bits];
			final Cell[] array = Cells.arrayA(n);
			final int cnt = at(bits, array);
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
		public int at(final int bits, final Cell[] array) {
			int cnt = 0;
			for ( int i : INDEXES[bits] ) {
				array[cnt++] = this.cells[i];
			}
			return cnt;
		}

		/**
		 * Get the index of the given cell in this region.cells array.
		 * <p>The {@code cell} must be in this region (checked with assert).
		 * Returns the cells b, y, or x index: whichever is appropriate for
		 * this type of ARegion. So you CANNOT use indexOf as a contains test,
		 * as you might (reasonably) expect.
		 * <p>
		 * If this behaviour upsets you then fix it yourself (check the given
		 * cells b/x/y against my b/x/y) to return -1, which'll throw an AIOBE)
		 * I just prefer speed to correctness upon which I do not rely.
		 *
		 * @param cell the cell whose index you seek.
		 * @return the index of the cell in this regions cells array.
		 */
		public abstract int indexOf(final Cell cell);

		/**
		 * otherThan returns the other place for 'value' in this ARegion. That
		 * is the place other than the given cell, obviously.
		 * <p>
		 * otherThan is intended for use ONLY on regions with just two places
		 * for the given value (ergo conjugate pairs). You give me a Cell and
		 * the value, and I return the other Cell in this region which maybe
		 * this value, ergo the conjugate (the other cell in a conjugate pair).
		 * <p>
		 * If you call me on a region which does not have two places for this
		 * value then the result is untested and is regarded as indeterminate,
		 * but here's my best guess at what you'll see:<ul>
		 * <li>If this region does not contain cell -&gt; first cell in this
		 *  region which maybe value.
		 * <li>If this region does not have 2 places value -&gt; first cell in
		 *  this region which maybe value.
		 * <li>If value is already placed in this region -&gt; the first cell
		 *  in this region, period.
		 * </ul>
		 * Detecting and dealing with these situation is more complex than just
		 * solving the core problem, so I haven't even attempted to do so, I'm
		 * a happy-go-lucky sort of guy, so use me responsibly. tequila += 9mm;
		 * I'm the 9mm. I doubt this rant will have a significant impact on the
		 * consumption of tequila, which is just as well really. I you live on
		 * a planet that's being utterly destroyed by ____sticks, and you can't
		 * kill the _____sticks, being one yourself, then just get pissed. That
		 * is "reasonable" advice, isn't it? sigh.
		 *
		 * @param cell one of two cells in a conjugate pair
		 * @param value the value to conjugate on
		 * @return the other cell in the conjugate pair
		 */
		public Cell otherThan(final Cell cell, final int value) {
			// lick this one Scoobie!
			return cells[IFIRST[ridx[value].bits & ~ISHFT[indexOf(cell)]]];
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
		public LinkedList<Cell> otherThan(final Iterable<Cell> excluded) {
			final LinkedList<Cell> result = new LinkedList<>();
			CELL: for ( final Cell cell : cells ) {
				// NB: use == instead of equals; they're the SAME instances.
				// NB: an iterator is fast enough here, but only because I'm
				// only called when creating a hint, ie not too often. If you
				// ever hammer this method then pass an Idx with O(1) contains
				// and I'd better take an array and return it's new size. sigh.
				for ( Cell x : excluded ) {
					if ( x == cell ) {
						continue CELL;
					}
				}
				result.add(cell);
			}
			return result;
		}

		/**
		 * Does this region contain this Cell? Fast O(1).
		 *
		 * @param cell Cell to look for.
		 * @return true if this region contains this cell, else false.
		 */
		public abstract boolean contains(final Cell cell);

		/**
		 * Does this region intersect 'r'.
		 *
		 * @param r to examine
		 * @return Does this region intersect 'r'?
		 */
		public abstract boolean intersects(ARegion r);

		/** @return my containsValue array (rebuilt). */
		public boolean[] containsValue() {
			Arrays.fill(containsValue, false);
			for ( final Cell cell : cells ) {
				if ( cell.value > 0 ) {
					containsValue[cell.value] = true;
				}
			}
			return containsValue;
		}

		/**
		 * Called maybe because cellsWhichMaybe is too long.
		 * <p>
		 * Repopulate 'result' with cells in this region that maybe 'cands' and
		 * return how many. The given 'array' must be large enough. I don't
		 * null-terminate or anything, I just return the number of cells added
		 * to 'result', for use with a fixed-size array.
		 *
		 * @param cands a bitset of the value/s you seek.
		 * @param result {@code Cell[]} to populate. The array is presumed to
		 *  be large enough.
		 * @return the number of cells added to 'result'.
		 */
		public int maybe(final int cands, final Cell[] result) {
			final int capacity = result.length;
			int size = 0;
			for ( final Cell cell : cells ) {
				if ( (cell.maybes & cands) != 0 ) {
					if ( size < capacity ) {
						result[size] = cell;
					}
					++size;
				}
			}
			return size;
		}

		/**
		 * Return the cells in this region which maybe 'bits'.
		 * <p>
		 * This method only used by UniqueRectangle.createType4Hint, which is
		 * the only use of UrtCellSet; ie there's lots of special code under
		 * the bonnet.
		 * <p>
		 * Called 382,984 times in top1465: efficiency matters a bit, not lots.
		 *
		 * @param bits bitset (one-or-more left-shifted values) to get.
		 * @param results a LinkedHashCellSet as a CellSet to which I add
		 * @return a new {@code LinkedHashCellSet} as a {@code CellSet}.
		 */
		public IUrtCellSet maybe(final int bits, final IUrtCellSet results) {
			// all current calls clear results, so it's "oddly" done here.
			results.clear();
			for ( final Cell cell : cells ) { // 9 cells
				if ( (cell.maybes & bits) != 0 ) {
					results.add(cell);
				}
			}
			return results;
		}

		/**
		 * Count the number of empty (value==0) cells in this region,
		 * remembering that value in my {@link emptyCellCount} field.
		 * <p>
		 * emptyCellCount runs once at commencement of solving, so we can call
		 * emptyCellCount BEFORE calling emptyCells, which sets emptyCellCount.
		 *
		 * @return int the count.
		 */
		public int emptyCellCount() {
			int cnt = 0;
			for ( int i=0; i<REGION_SIZE; ++i ) {
				if ( cells[i].value == 0 ) {
					++cnt;
				}
			}
			return emptyCellCount = cnt;
		}

		/**
		 * Populate 'result' with empty cells in this region and return it,
		 * setting the {@link #emptyCellCount} field so that you can use a
		 * fixed-size array, for speed.
		 *
		 * @param result number of empty cells in result
		 * @return the result array, and sets the emptyCellCount field which
		 *  is a bit s__t really, because you need the emptyCellCount to get
		 *  the right-sized array to pass to me. It was designed to be used
		 *  with a re-usable array of 9 cells, not a right-sized array, which
		 *  is how it's actually being used.
		 */
		public int emptyCells(final Cell[] result) {
			int cnt = 0;
			for ( final Cell cell : cells ) {
				if ( cell.value == 0 ) {
					result[cnt++] = cell;
				}
			}
			return emptyCellCount = cnt;
		}

		/** @return id - so just access the id directly, it's faster. */
		@Override
		public String toString() {
			return id;
		}

		/**
		 * Returns the given Idx populated with the indices of the cells in
		 * this ARegion which maybe any of the given maybes (a bitset).
		 *
		 * @param cands a bitset of the maybes you seek
		 * @param result the result Idx
		 * @return the result Idx of cells in this region which maybe maybes
		 */
		public Idx idxOf(final int cands, final Idx result) {
			result.clear();
			for ( final Cell c : cells ) {
				if ( (c.maybes & cands) != 0 ) {
					result.add(c.i);
				}
			}
			return result;
		}

		/**
		 * Returns the first cell in this region which maybe v.
		 * <p>
		 * There must be atleast one cell in this region which maybe v.
		 * If there are none then I return the first cell in this region, which
		 * is a bit s__t, but it's fast this way.
		 * <p>
		 * It's faster to do this in-line, but the method is only one
		 * invocation slower, so exists for brevity and documentation.
		 *
		 * @param v the candidate value
		 * @return the first cell which maybe v */
		public Cell first(final int v) {
			return cells[IFIRST[ridx[v].bits]];
		}

		/**
		 * Returns the first empty cell in this region.
		 *
		 * @return the first cell in this region with value == 0
		 */
		public Cell firstEmptyCell() {
			for ( Cell c : cells ) {
				if ( c.value == 0 ) {
					return c;
				}
			}
			return null;
		}

		/**
		 * Returns the instance of Grid which contains this cell.
		 *
		 * @return the grid that contains this cell.
		 */
		public Grid getGrid() {
			return Grid.this;
		}

		/**
		 * Rebuild all this cells s__t, or to be more precise:<ul>
		 *  <li>{@link emptyCellCount()}
		 *  <li>{@link containsValue()}
		 *  <li>{@link rebuildIndexsOfAllValues()}
		 *  <li>{@link rebuildIdxsOfAllValues()}
		 * </ul>
		 */
		public void rebuildAllS__t() {
			emptyCellCount();
			containsValue();
			rebuildIndexsOfAllValues();
			rebuildIdxsOfAllValues();
		}

		/**
		 * Rebuild the {@link #ridx} of each value 1..9 with the index in this
		 * {@link #cellsA ARegion.cells} array of each Cell which maybe v.
		 */
		private void rebuildIndexsOfAllValues() {
			int i, bitset, sv, v;
			for ( v=1; v<VALUE_CEILING; ++v ) {
				if ( containsValue[v] ) {
					ridx[v].clear();
				} else {
					sv = VSHFT[v];
					bitset = 0;
					for ( i=0; i<REGION_SIZE; ++i ) {
						if ( (cells[i].maybes & sv) != 0 ) {
							bitset |= ISHFT[i];
						}
					}
					ridx[v].set(bitset);
				}
			}
		}

		/**
		 * Rebuild the {@link #idxs ARegion.idxs} with the indice in grid.cells
		 * of each cell in this region that maybe each value 1..9.
		 */
		private void rebuildIdxsOfAllValues() {
			for ( int v=1; v<VALUE_CEILING; ++v ) {
				idxs[v].setAnd(idx, Grid.this.idxs[v]);
			}
		}

		/**
		 * Repopulates 'result' with cells in this region having
		 * size 2(inclusive)..ceiling(EXCLUSIVE)
		 * and returns hows many.
		 *
		 * @param ceiling the maximum size of wanted cells + 1 (exclusive)
		 * @param result to repopulate
		 * @return how many
		 */
		public int sized(final int ceiling, final Cell[] result) {
			int size, cnt = 0;
			for ( final Cell c : cells ) {
				if ( (size=c.size)>1 && size<ceiling ) {
					result[cnt++] = c;
				}
			}
			return cnt;
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
		 * it's boxIndex, upon which contains relies, coz it's Fast. */
		public final int boxIndex;
		/**
		 * Construct a new Box in this Grid.
		 *
		 * @param index of this Box (0..8) in the Grid.boxs array, which is (by
		 *  design) coincident with the first 9 elements in Grid.regions. When
		 *  we process all regions we usually want to process the boxs first
		 *  because humans find it easier to find things in a Box verses a Row
		 *  or Col of the same size, I think because the brain finds it easier
		 *  to search three threes verses nine. Or it could be that our vision
		 *  renders a spot, not a strip. Anyway, my mental limit is seven.
		 *  Beyond that it registers as just "many", so upto 7 "feels real",
		 *  beyond seven is just an abstract concept, not "reality". I think
		 *  that Mathematicians demur, mainly BECAUSE they demur. <br>
		 *  <br>
		 *  I think this is WHY "standard" humans find Sudoku seductive, coz we
		 *  suck at it, and it's very nearly da simplest thing that we suck at,
		 *  so we think we shouldn't suck at it, so we suck on it, so suck less
		 *  at it, so we go right ahead and suck on it some more, BECAUSE we
		 *  suck at it, but we think we shouldn't suck at it, so we suck on it,
		 *  ad-infinitum! <br>
		 *  <br>
		 *  Not knowing is far more engaging than knowing. Nearly knowing is
		 *  such a profound pain in the ass that it drives scientists mental,
		 *  so mental in fact that they turn themselves into scientists. This,
		 *  I think, is the square-root of 42. Wisdom is accepting that you can
		 *  never know, and writing a computer program to figure it all out.
		 *  One step at a time. Intelligence is a two-edged sword. Mix-in even
		 *  a small dose of stupidity and self-extinction becomes warrantable.
		 */
		public Box(final int index) {
			super(index, BOX, 0);
			this.hNum = index%R;
			this.left = hNum*R;
			this.vNum = index/R;
			this.top = vNum*R;
			this.boxIndex = top + hNum;
			for ( int i=0; i<N; ++i ) {
				// nb: it sets cells[i] first, then it's attributes
				(cells[i]=Grid.this.cells[((top+i/R)*N)+(left+i%R)])
						.regions[BOX] = cells[i].box = this;
			}
		}
		/** Return the index of the given cell in this Regions cells array.
		 * @return Actually just returns the given cells 'b' index (for box),
		 *  we check that thisRegion.contains(cell) with an assert. */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.b; // cell.y%3*3 + cell.x%3;
		}
		/** @return Does this Box contain the given cell? */
		@Override
		public boolean contains(final Cell cell) {
			// boxIndex contains the index of box in Grid.regions
			return cell.boxIndex == this.boxIndex;
		}
		@Override
		public boolean intersects(final ARegion r) {
			switch ( r.typeIndex ) {
			case BOX: return this == r;
			case ROW: return ((Row)r).y >= this.top && ((Row)r).y <= this.top+2;
			case COL: return ((Col)r).x >= this.left && ((Col)r).x <= this.left+2;
			default: return false; // Never
			}
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
		public Row(final int y) {
			super(N+y, ROW, R);
			this.y = y;
			this.vNum = y / R;
			final int floor = y * N; // indice of first cell in this Row
			for ( int i=0; i<N; ++i ) {
				(cells[i]=Grid.this.cells[floor+i])
						.regions[ROW] = cells[i].row = this;
			}
			this.intersectingBoxs[0] = Grid.this.boxs[vNum];
			this.intersectingBoxs[1] = Grid.this.boxs[vNum+1];
			this.intersectingBoxs[2] = Grid.this.boxs[vNum+2];
		}
		/** Returns the index of the given Cell in this regions cells array.
		 * @param cell Cell to get the index of.
		 * @return the x (horizontal index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert. */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.x;
		}
		/** @return Does this Row contain the given Cell? */
		@Override
		public boolean contains(final Cell cell) {
			return cell.y == this.y;
		}
		@Override
		public boolean intersects(final ARegion r) {
			switch ( r.typeIndex ) {
			case BOX: return y >= ((Box)r).top && y <= ((Box)r).top+2;
			case ROW: return this == r;
			case COL: return true;
			default: return false; // Never
			}
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
		public Col(final int x) {
			super(FIRST_COL+x, COL, R);
			this.x = x;
			this.hNum = x / R;
			for ( int i=0; i<N; ++i ) {
				(cells[i]=Grid.this.cells[i*N+x])
						.regions[COL] = cells[i].col = this;
			}
			this.intersectingBoxs[0] = Grid.this.boxs[hNum];
			this.intersectingBoxs[1] = Grid.this.boxs[hNum+3];
			this.intersectingBoxs[2] = Grid.this.boxs[hNum+6];
		}
		/** Returns the index of the given Cell in this regions cells array.
		 * @return the y (vertical index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert. */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.y;
		}
		/** @return Does this Col contain the given Cell? */
		@Override
		public boolean contains(final Cell cell) {
			return cell.x == this.x;
		}
		@Override
		public boolean intersects(final ARegion r) {
			switch ( r.typeIndex ) {
			case BOX: return x >= ((Box)r).left && x <= ((Box)r).left+2;
			case ROW: return true;
			case COL: return this == r;
			default: return false; // Never
			}
		}
	}

	/**
	 * A Naked or Hidden Single element in a Deque.
	 *
	 * @author Keith Corlett 2021-06-18
	 */
	public static class Single {
		public final Cell cell;
		public final int value;
		public Single(final Cell cell) {
			this.cell = cell;
			this.value = 0;
		}
		public Single(final Cell cell, final int value) {
			this.cell = cell;
			this.value = value;
		}
	}

	/**
	 * DEBUG: Diff two grids: original and copy.
	 * <p>
	 * I'm an inner class coz I access ALL of Grid's privates.
	 * <p>
	 * Used by {@link diuf.sudoku.solver.checks.BruteForce} when you
	 * modify code to switch me on. I am a DEBUG only tool.
	 * <pre>
	 * <u><b>WARNING:</b></u>
	 * Ensure Diff code is still current BEFORE you rely on it!
	 * Attributes get added but missed by Diff, which may make
	 * BillyGoneBuggerUp.txt look like a picnic in the bloody park,
	 * with a really smoking hot chick, and her little sister, who
	 * is even hotter, and a total... Slap! Reality is persistant.
	 * </pre>
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
		public Diff(final Grid g0, final Grid g1) {
			this.g0 = g0;
			this.g1 = g1;
		}
		public boolean diff(final PrintStream out) {
			this.out = out;
			any = false;

			//
			// all output is original (g0) then copy (g1)
			//

			// If you get a poke in the 'i' you can forget the blunt stick
			for ( int i=0; i<GRID_SIZE; ++i ) {
				final Cell c0 = g0.cells[i];
				final Cell c1 = g1.cells[i];
				if ( c0.i != c1.i ) {
					return differs(""+i+": i "+c0.i+" != "+c1.i);
				}
			}

			// Cells
			for ( int i=0; i<GRID_SIZE; ++i ) {
				final Cell c0 = g0.cells[i];
				final Cell c1 = g1.cells[i];
				if ( c0.value != c1.value )
					differs(""+i+": value "+c0.value+" != "+c1.value);
				if ( c0.maybes != c1.maybes )
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
				if ( c0.boxIndex != c1.boxIndex )
					differs(""+i+": boxIndex "+c0.boxIndex+" != "+c1.boxIndex);
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
					final ARegion r0 = c0.regions[r];
					final ARegion r1 = c1.regions[r];
					if ( !r0.id.equals(r1.id) )
						differs(""+i+": regions "+r0.id+" != "+r1.id);
				}
				if ( c0.properties!=null && c1.properties!=null && !c0.properties.equals(c1.properties) )
					differs(""+i+": properties "+c0.properties+" != "+c1.properties);
			}

			// Boxs
			for ( int b=0; b<REGION_SIZE; ++b ) {
				final Box b0 = g0.boxs[b];
				final Box b1 = g1.boxs[b];
				if ( b0.boxIndex != b1.boxIndex )
					differs(""+b+": boxIndex "+b0.boxIndex+" != "+b1.boxIndex);
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
			for ( int r=0; r<REGION_SIZE; ++r ) {
				final Row r0 = g0.rows[r];
				final Row r1 = g1.rows[r];
				if ( r0.vNum != r1.vNum )
					differs(""+r+": vNum "+r0.vNum+" != "+r1.vNum);
				if ( r0.y != r1.y )
					differs(""+r+": y "+r0.y+" != "+r1.y);
			}
			// Cols
			for ( int c=0; c<REGION_SIZE; ++c ) {
				final Col r0 = g0.cols[c];
				final Col r1 = g1.cols[c];
				if ( r0.hNum != r1.hNum )
					differs(""+c+": hNum "+r0.hNum+" != "+r1.hNum);
				if ( r0.x != r1.x )
					differs(""+c+": x "+r0.x+" != "+r1.x);
			}

			// All Regions
			for ( int r=0; r<NUM_REGIONS; ++r ) {
				final ARegion r0 = g0.regions[r];
				final ARegion r1 = g1.regions[r];
				if ( !r0.id.equals(r1.id) )
					differs(""+r+": id "+r0.id+" != "+r1.id);
				if ( r0.index != r1.index )
					differs(""+r+": index "+r0.index+" != "+r1.index);
				if ( r0.typeIndex != r1.typeIndex )
					differs(""+r+": typeIndex "+r0.typeIndex+" != "+r1.typeIndex);
				if ( !r0.typeName.equals(r1.typeName) )
					differs(""+r+": typeName "+r0.typeName+" != "+r1.typeName);
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
				for ( int v=1; v<VALUE_CEILING; ++v ) {
					if ( !r0.idxs[v].equals(r1.idxs[v]) )
						differs(""+r+": idxs["+v+"] "+r0.idxs[v]+" != "+r1.idxs[v]);
					if ( !r0.ridx[v].equals(r1.ridx[v]) )
						differs(""+r+": ridx["+v+"] "+r0.ridx[v]+" != "+r1.ridx[v]);
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
