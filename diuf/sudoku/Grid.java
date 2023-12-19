/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Idx.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.UnsolvableException;
import static diuf.sudoku.utils.Frmt.*;
import static diuf.sudoku.utils.IntArray.THE_EMPTY_INT_ARRAY;
import diuf.sudoku.utils.IFilter;
import diuf.sudoku.utils.IVisitor2;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import diuf.sudoku.utils.MyMath;
import diuf.sudoku.utils.MyStrings;
import static java.lang.System.arraycopy;
import java.util.List;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Predicate;

/**
 * An instance of Grid represents a Sudoku grid. A grid can be retained and
 * have puzzles loaded into it serially, or you can create a new Grid for each
 * puzzle. Its a lot of code, sorry. It just keeps growing.
 * <p>
 * A Grid contains the array of 81 cells (in a 9*9 grid), as well as classes
 * and methods to manipulate regions (boxs, rows, and cols).
 * <p>
 * <pre>
 * <b>Grid indices (in chess notation):</b>
 *
 *      A  B  C    D  E  F    G  H  I
 *   +----------+----------+----------+
 * 1 | 00 01 02 | 03 04 05 | 06 07 08 | 1
 * 2 | 09 10 11 | 12 13 14 | 15 16 17 | 2
 * 3 | 18 19 20 | 21 22 23 | 24 25 26 | 3
 *   +----------+----------+----------+
 * 4 | 27 28 29 | 30 31 32 | 33 34 35 | 4
 * 5 | 36 37 38 | 39 40 41 | 42 43 44 | 5
 * 6 | 45 46 47 | 48 49 50 | 51 52 53 | 6
 *   +----------+----------+----------+
 * 7 | 54 55 56 | 57 58 59 | 60 61 62 | 7
 * 8 | 63 64 65 | 66 67 68 | 69 70 71 | 8
 * 9 | 72 73 74 | 75 76 77 | 78 79 80 | 9
 *   +----------+----------+----------+
 *      A  B  C    D  E  F    G  H  I
 * </pre>
 * <p>
 * Vertical (row) coordinates range from 0 (topmost) to 8 (bottommost).<br>
 * Horizontal (col) coordinates range from 0 (leftmost) to 8 (rightmost),<br>
 * in chess parlance that is rows 1 through 9, and cols A through I.
 * <p>
 * Grid is the guts of all the Sudoku Explainer software. Theres lots of
 * "weird code" in here, so please be careful when you change anything,
 * you must be aware of everything using anything. Adding new stuff is
 * usually less painful.
 */
@SuppressWarnings("ManualArrayToCollectionCopy") // its actually faster to copy small arrays into collections "manually"
public final class Grid {

	/**
	 * AUTOSOLVE: Self-documenting {@link AHint#applyImpl} isAutosolving params
	 * gathered together in a cave and grooving with a single point of truth.
	 * false for correct elimination counts in batch; true for speed and mahem.
	 * Changing the constant value SHOULD change everything that needs to, but
	 * sift gets forked-up over time, so you will also need to double-check.
	 * <p>
	 * If true then Naked and Hidden Singles are set by {@link Cell#set(int)}
	 * in {@link AHint#applyQuitely(boolean, diuf.sudoku.Grid) }, for speed.
	 * The downside is {@link diuf.sudoku.solver.LogicalSolver#solve} no longer
	 * counts them as Naked/Hidden Singles, ascribing them to whichever hinter
	 * was lucky enough to hit the first domino, pseudo-randomising ALL of the
	 * elimination-counts in the batch .log, rendering nanoseconds/elim useless
	 * and it is the only empirical performance metric, hence destroying any
	 * chance of improvement.
	 * <p>
	 * If false then Singles are left for the
	 * {@link diuf.sudoku.solver.hinters.single.NakedSingle} and
	 * {@link diuf.sudoku.solver.hinters.single.HiddenSingle} hinters to find,
	 * report, and apply. BruteForce has its own AUTOSOLVE.
	 * <p>
	 * SO: If Singles are unimportant to you then YOU change AUTOSOLVE to true.
	 * Everything that needs-to reads it. Just beware that the elim-counts from
	 * solve (batch .log) are then useless, and thats a key check; lost elims
	 * means investigate, so you CAN have more speed, but YOU mind the bergs.
	 * Do please put it back to false when you are done being a dick.
	 * <p>
	 * NB: true = faster {@link diuf.sudoku.test.knuth.RecursiveSolverTester}
	 */
	public static final boolean AUTOSOLVE = false; // @check false

	/** A self-documenting AHint.apply isAutosolving parameter. */
	public static final boolean NO_AUTOSOLVE = false;

	/** The number of cells in a region: 9. */
	public static final int REGION_SIZE = 9; // change is NOT an option!
	static {
		// I try to assure REGION_SIZE is a square number, whose square
		// root is an integer, but I am not sure how to do that.
		assert Math.sqrt(REGION_SIZE) == ((double)(int)Math.sqrt(REGION_SIZE));
	}

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

	/** The number of region types: 3 (BOX, ROW, COL). */
	public static final int NUM_REGION_TYPES = 3;

	/** The name of each type of region: "box", "row", "col". */
	public static final String[] REGION_TYPE_NAMES = {"box", "row", "col"};

	// FYI these three values depend on region-types remaining BOX, ROW, COL
	// but there usage allows for other region types, despite my feeling that
	// other "fancy" region types only degrade original-Sudokus pure genius.
	// I have similar (not as strong) feelings about larger (4x4) regions.
	/** The index of the first box in grid.regions: 0. */
	public static final int FIRST_BOX = 0;

	/** The index of the first row in grid.regions: 9. */
	public static final int FIRST_ROW = REGION_SIZE;

	/** The index of the first col in grid.regions: 18. */
	public static final int FIRST_COL = FIRST_ROW + REGION_SIZE;

	/** The number of regions: 27 (9*box, 9*row, 9*col). */
	public static final int NUM_REGIONS = REGION_SIZE * NUM_REGION_TYPES;

	/** The number of cells in a Grid: 81. */
	public static final int GRID_SIZE = REGION_SIZE * REGION_SIZE;

	/** The minimum number of clues in a valid Sudoku: 17. */
	public static final int MIN_CLUES = 17;

	/** The maximum number of empty cells in a Grid: 81 - 17 = 64. */
	public static final int MAX_EMPTY_CELLS = GRID_SIZE - MIN_CLUES;

	/** The number of sibling cells that each cell has: 20. */
	public static final int NUM_SIBLINGS = 20;

	// Shorthands, because the longhands, while meaningful, are just too long.
	// NOTE that other classes create there own shorthands for these values,
	// but every class that uses shorthand defines its own locally, which have
	// been brought into alignment, which they will wander away from again, so
	// maybe it would be better to just make these "ungrocables" public?
	private static final int N = REGION_SIZE; // REGION_SIZE is 9 cells
	private static final int NN = GRID_SIZE; // REGION_SIZE squared is 81 cells
	private static final int R = SQRT; // squareroot of REGION_SIZE is 3 cells
	//                                    and so are the days of our Sudoku.
	//                                    ~ loku by KRC from H Block. Sigh.

	/** index * REGION_SIZE (9) coz lookup is faster than multiplication. */
	public static final int[] BY9 = new int[REGION_SIZE];
	static {
		for ( int i=0; i<REGION_SIZE; ++i )
			BY9[i] = i * REGION_SIZE;
	}

	/** A regex to match the clues line in the input file. */
	private static final Pattern CLUES_PATTERN = Pattern.compile(
			"([1-"+N+"]*,){"+(NN-1)+"}[1-"+N+"]*");


	/** do first else toString gets an exception in the debugger. */
	private static final char[] DIGITS = ".123456789".toCharArray();

	/** returns a char-array of the digits in maybes. */
	private static char[] digits(final int maybes) {
		final char[] result = new char[VSIZE[maybes]];
		int count = 0;
		for ( int v : VALUESES[maybes] )
			result[count++] = DIGITS[v];
		return result;
	}

	/** digits representing each possible maybes for toString. */
	public static final char[][] MAYBES = new char[NUM_MAYBES][];
	public static final String[] MAYBES_STR = new String[NUM_MAYBES];
	static {
		MAYBES[0] = new char[]{}; // THE_EMPTY_CHAR_ARRAY
		MAYBES_STR[0] = EMPTY_STRING;
		for ( int n=VSIZE.length,i=1; i<n; ++i )
			MAYBES_STR[i] = new String(MAYBES[i]=digits(i));
	}

	/**
	 * An IdScheme is how humans identify cells and regions in a Grid.
	 * There are two IdSchemes: Chess (preferred) and RowCol (untested).
	 * <p>
	 * This enum encapsulates all the-facts-of-life of an IdScheme.
	 * It produces and parses both {@link Cell#id} and {@link ARegion#label}.
	 * <p>
	 * The private IdSchemes functionality is exposed by public static proxy
	 * methods, so theres ONE scheme, which is used everywhere that needs it.
	 * <p>
	 * NOTE: I have only ever used Chess, but try to retro-fit RowCol, only to
	 * discover that my test-cases are all welded-onto Chess, and I am unwilling
	 * to spend a week (or two) rewriting them all for some silly idScheme, so
	 * I am permanently welded onto Chess, but you need not be. Blow away test
	 * cases and swap it over, but theres lots of hours in the testcases.
	 */
	private static enum IdScheme {
		Chess { // A1..I9
			@Override
			public int length() {
				return 2;
			}
			@Override
			public String id(final int indice) {
				try {
					return LETTERS[indice%N] + NUMBERS[indice/N];
				} catch ( Exception ex ) { // I am bullet proof
					throw new IllegalArgumentException("Bad indice: "+indice, ex);
				}
			}
			@Override
			public int indice(final String id) {
				try {
					final int row = id.charAt(1)-'1';
					if ( row<0 || row>8 )
						throw new IllegalArgumentException("Bad row: "+id);
					final int col = id.charAt(0)-'A';
					if ( col<0 || col>8 )
						throw new IllegalArgumentException("Bad col: "+id);
					return BY9[row] + col;
				} catch ( IllegalArgumentException ex ) {
					throw ex;
				} catch ( Exception ex ) {
					throw new IllegalArgumentException("Bad id: "+id, ex);
				}
			}
			@Override
			public int regionIndex(final String rid) {
				try {
					if ( rid==null || rid.length() != 5 )
						throw new IllegalArgumentException("Bad rid: "+rid);
					final char c = rid.charAt(4);
					final String type = rid.substring(0, 3);
					if ( type.compareToIgnoreCase(REGION_TYPE_NAMES[BOX]) == 0 )
						return c - '1';
					if ( type.compareToIgnoreCase(REGION_TYPE_NAMES[ROW]) == 0 )
						return REGION_SIZE + c - '1';
					if ( type.compareToIgnoreCase(REGION_TYPE_NAMES[COL]) == 0 ) {
						if ( c<'A' || c>'I' )
							throw new IllegalArgumentException("Bad col: "+rid);
						return FIRST_COL + c - 'A';
					}
					throw new IllegalArgumentException("Bad rid: "+rid);
				} catch ( IllegalArgumentException ex ) {
					throw ex;
				} catch ( Exception ex ) { // I am bullet proof
					throw new IllegalArgumentException("Bad rid: "+rid, ex);
				}
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
			public String id(final int indice) {
				try {
					return NUMBERS[indice/N] + "-" + NUMBERS[indice%N];
				} catch ( Exception ex ) { // I am bullet proof
					throw new IllegalArgumentException("Bad indice: "+indice, ex);
				}
			}
			@Override
			public int indice(final String id) {
				try {
					return BY9[id.charAt(0)-'1'] + (id.charAt(3)-'1');
				} catch ( Exception ex ) { // I am bullet proof
					throw new IllegalArgumentException("Bad id: "+id, ex);
				}
			}
			@Override
			public int regionIndex(final String rid) {
				try {
					final int x = rid.charAt(4) - '1';
					final String type = rid.substring(0, 3);
					if ( REGION_TYPE_NAMES[BOX].compareToIgnoreCase(type) == 0 )
						return x;
					if ( REGION_TYPE_NAMES[ROW].compareToIgnoreCase(type) == 0 )
						return FIRST_ROW + x;
					if ( REGION_TYPE_NAMES[COL].compareToIgnoreCase(type) == 0 )
						return FIRST_COL + x;
					throw new IllegalArgumentException("Bad rid: "+rid);
				} catch ( IllegalArgumentException ex ) {
					throw ex;
				} catch ( Exception ex ) { // I am bullet proof
					throw new IllegalArgumentException("Bad rid: "+rid, ex);
				}
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

	/** The ID scheme: Chess. The test-cases are welded-onto Chess, so RowCol
	 * is NOT really an option; ergo the IdScheme thing was a waste of time. */
	private static final IdScheme ID_SCHEME = IdScheme.Chess;

	/** The length of the ID field of each cell in this Grid. */
	public static final int CELL_ID_LENGTH = ID_SCHEME.length();

	/** The ID field of each cell in this Grid. */
	public static final String[] CELL_IDS = new String[GRID_SIZE];

	/** The region.ids by index in the grid.regions array. */
	public static final String[] REGION_LABELS = new String[27];

	/** An Idx containing the indice of each cell in the Grid. */
	public static final Idx[] CELL_IDXS = new Idx[GRID_SIZE];

	static {
		int i, n;
		i=0; do CELL_IDS[i] = ID_SCHEME.id(i); while(++i<GRID_SIZE);
		i=FIRST_BOX; n=FIRST_BOX+REGION_SIZE;
		do REGION_LABELS[i] = REGION_TYPE_NAMES[BOX]+" "+NUMBERS[i]; while(++i<n);
		i=FIRST_ROW; n=FIRST_ROW+REGION_SIZE;
		do REGION_LABELS[i] = REGION_TYPE_NAMES[ROW]+" "+NUMBERS[i%REGION_SIZE]; while(++i<n);
		final String[] c = ID_SCHEME.columnLabels();
		i=FIRST_COL; n=FIRST_COL+REGION_SIZE;
		do REGION_LABELS[i] = REGION_TYPE_NAMES[COL]+" "+c[i%REGION_SIZE]; while(++i<n);
		i=0; do CELL_IDXS[i] = Idx.of(i); while(++i<GRID_SIZE);
	}

	// nb: can calculate these three OTF, but look-up is faster.
	/** The index of the box in {@link #boxs} which contains each cell. */
	public static final int[] BOX_OF = new int[GRID_SIZE];

	/** The index of the row in {@link #rows} which contains each cell. */
	public static final int[] ROW_OF = new int[GRID_SIZE];

	/** The index of the col in {@link #cols} which contains each cell. */
	public static final int[] COL_OF = new int[GRID_SIZE];

	/**
	 * RIBS (Region Index BitSets) is a bitset of the indexes of the 3 regions
	 * of each cell in the Grid. RIBS is concurrent with Grid.cells. Each RIBS
	 * bitset is 27 bits of three Grid.regions indexes: my box, row, and col.
	 * <p>
	 * RIBS exist to be bitwise-anded and/or bitwise-ored together. This is
	 * Pure Deadly Evil Clever! One might call even call it Whiteboy Magic.
	 * My school councillor once advised me to take a job at the meatworks.
	 * My school councillor is a ____ing idiot, who failed vegi-math, and
	 * probably cheats a golf. Atleast shes not running for president. Sigh.
	 */
	public static final int[] RIBS = new int[GRID_SIZE];

	static {
		int i, b, r, c; // of this cell
		// set ROW_OF, COL_OF, and BOX_OF; plus RIBS
		for ( i=0; i<GRID_SIZE; ++i ) {
			r = ROW_OF[i] = i/N;
			c = COL_OF[i] = i%N;
			b = BOX_OF[i] = r/R*R + c/R;
			RIBS[i] = IDX_SHFT[b]			// box
			        | IDX_SHFT[FIRST_ROW+r]		// row
			        | IDX_SHFT[FIRST_COL+c];	// col
		}
	}

	/**
	 * BUDDIES is an array of Immutable Idxs of the 20 distinct sibling cells
	 * (precluding the cell itself) in the same box, row, and col as each cell
	 * in a Grid. BUDDIES is concurrent with Grid.cells. This is a speed thing;
	 * so that you dont need the actual cell object (you just need to know its
	 * index) in order to get its buddies.
	 * <p>
	 * The same Idx is referenced by {@link Cell#buds}. For speed, use BUDDIES
	 * if you have only the cell indice, or buds if you already have the Cell.
	 * <p>
	 * BUDDIES is static because a cell has the same siblings in all Grids, and
	 * these are only indices, not the cells themselves. If you want the cells
	 * themselves then {@link Cell#siblings} is probably faster, but if you
	 * have indices, especially in Idxs then BUDDIES is your best-friend. Sigh.
	 * <p>
	 * An IdxI is immutable. Any attempt to modify throws a LockedException.
	 */
	public static final IdxI[] BUDDIES = new IdxI[GRID_SIZE];
	public static final long[] BUDS_M0 = new long[GRID_SIZE];
	public static final int[] BUDS_M1 = new int[GRID_SIZE];

	/**
	 * SEES contains does each cell in grid see (populate the same box, row, or
	 * col) as every other cell in the grid. SEES for speed; BUDDIES for Idx
	 * operations and/or indices; SIBLINGS for cells.
	 */
	public static final boolean[][] SEES = new boolean[GRID_SIZE][GRID_SIZE];

	static {
		long m0=0L; int i, j, m1=0;
		// foreach cell-indice
		for ( i=0; i<GRID_SIZE; ++i ) {
			// foreach "other" cell-indice
			for ( j=0; j<GRID_SIZE; ++j ) {
				// if j is in the same (box, row, or col) as i
				// and they are not the same cell
				if ( (RIBS[i]&RIBS[j])!=0 && j!=i ) {
					// then j is a buddy of i
					SEES[i][j] = true;
					if ( j < FIFTY_FOUR )
						m0 |= MASKED81[j];
					else
						m1 |= MASKED[j];
				}
			}
			// exploded Idxs for speed
			BUDS_M0[i] = m0;
			BUDS_M1[i] = m1;
			// a new IdxImmutable of buddies
			BUDDIES[i] = new IdxI(m0,m1, NUM_SIBLINGS);
			m0 = m1 = 0;
		}
	}

//Generating REGION_INDICES is slower, so I do it once to hardcode them.
//See: diuf.sudoku.tools.RegionIndices
//	private static final int[][] REGION_INDICES = new int [NUM_REGIONS][REGION_SIZE];
//	static {
//		final int[] indices = new int[N]; // N is REGION_SIZE
//		// boxs
//		for ( int b=0; b<N; ++b) {
//			final int top = b/R; // R is Math.sqrt(REGION_SIZE)
//			final int left = b%R;
//			for ( int i=0; i<N; ++i )
//				indices[i] = top+i/R + left+i%R;
//			REGION_INDICES[FIRST_BOX+b] = copyOf(indices, N);
//		}
//		// rows
//		for ( int y=0; y<N; ++y ) {
//			for ( int x=0; x<N; ++x )
//				indices[x] = BY9[y] + x;
//			REGION_INDICES[FIRST_ROW+y] = copyOf(indices, N);
//		}
//		// cols
//		for ( int x=0; x<N; ++x ) {
//			for ( int y=0; y<N; ++y )
//				indices[y] = BY9[y] + x;
//			REGION_INDICES[FIRST_COL+x] = copyOf(indices, N);
//		}
//	}
	/**
	 * REGION_INDICES contains the indice of each of the 9 cells in each region
	 * of the 27 regions in the Grid: boxes, rows, cols.
	 */
	public static final int[][] REGION_INDICES = new int [][] {
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

	/**
	 * REGION_IDXS contains an Idx of the cells in each region in the grid.
	 * Note that IdxIs are immutable. Concurrent with grid.regions.
	 */
	public static final IdxI[] REGION_IDXS = new IdxI[NUM_REGIONS];
	static {
		for ( int i=0; i<NUM_REGIONS; ++i ) {
			REGION_IDXS[i] = IdxI.of(REGION_INDICES[i]);
		}
	}

//MagicNumbers where generated with this code. Now hardcoded for speed.
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
	/**
	 * A bitset of the indexes in grid.regions of the boxs that are effected
	 * by each of the regions in the grid. The values here-in are box indexes
	 * 0..8, so you can read them with the Indexes.INDEXES array-of-arrays.
	 * <p>
	 * <pre>
	 * The meaning of these values are type dependant:
	 * Box: the 4 boxs which also intersect the rows and cols of this box,
	 *      ie the two boxs left/right, and the two boxs above/below.
	 *      Examples in ascii art: X is this box, #s are effected boxs:
	 *            X##        .#.        ..#
	 *            #..        #X#        ..#
	 *            #..        .#.        ##X
	 * Row: the 3 boxs which intersect this row;
	 * Col: the 3 boxs which insersect this col;
	 * </pre>
	 * This is all static because its the same for all grids, so you can get
	 * it without an instance of Grid.
	 * <p>
	 * EFFECTED_BOXS is used in Medusa3D and GEM, which both use ONLY the boxs,
	 * so rows and cols are populated but not (yet) used, mainly coz rows/cols
	 * have a pre-existing intersectingBoxs array thats preferred if you have
	 * the ARegion, but for me you need only its index, and you get only
	 * indexes, which you read through Indexes.INDEXES, to get the ARegions
	 * from grid.regions, which all looks pretty messy, but is very fast).
	 * Maybe I should read this array into each boxs intersectingBoxs array,
	 * instead of leaving it null? No requirement to do so, yet.
	 * <p>
	 * Speed: I ran this ONCE to get the magic-numbers; now we just load them.
	 */
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
	public static int indice(final String id) {
		return ID_SCHEME.indice(id);
	}

	/**
	 * Get the index of this region ID.
	 *
	 * @param rid the region.id for which you seek the index
	 * @return the index of this region in grid.regions 0..26
	 */
	public static int regionIndex(final String rid) {
		return ID_SCHEME.regionIndex(rid);
	}

	/**
	 * UnsolvableException is thrown a lot by {@link Cell#canNotBeBits(int) }
	 * in the Generator, and we really need just one instance, so here it is.
	 */
	private UnsolvableException noMacsRemain;

	// ============================= instance land ============================

	/**
	 * A single-dimensional array of the cells in this grid.
	 */
	public final Cell[] cells = new Cell[GRID_SIZE];

	/**
	 * The maybes (potential values) of each Cell in this Grid, to save getting
	 * the cell in order to get its maybes when all you have is its indice.
	 */
	public final int[] maybes = new int[GRID_SIZE];

	/**
	 * The size (number of potential values) of each Cell in this Grid, to save
	 * getting the cell in order to get its size, when you have an indice.
	 */
	public final int[] sizes = new int[GRID_SIZE];

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
	 * An array of all the regions in this grid: 9 Boxs, 9 Rows, 9 Cols.
	 * Software relies on that box/row/col order, hence it is immutable.
	 */
	public final ARegion[] regions = new ARegion[NUM_REGIONS];

	/**
	 * regionIdxs: the vs in each region, by value and region.
	 */
	public final Idx[][] ridxs = new Idx[VALUE_CEILING][NUM_REGIONS];

	/**
	 * indices of cells in this Grid which maybe each value 1..9.
	 */
	public final IdxL[] idxs = new IdxL[VALUE_CEILING];

	/**
	 * The sequential number of the hint that we are up-to in solve (et al).
	 */
	public int hintNumber; // anything but 0, the default int value.

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
	 * solution contains the correct cell values of the puzzle in this Grid.
	 * Set by {@link diuf.sudoku.solver.checks.BruteForce#solve} and read
	 * by {@link diuf.sudoku.solver.hinters.Validator#isValid} (et al).
	 */
	public int[] solution;

	/**
	 * puzzle-identifier (pid). The pid identifies the puzzle that is loaded
	 * into this grid. Its just a random long that is set whenever a new grid
	 * is created or a puzzle is loaded, so that cachers can tell if the grid
	 * still contains "the cached puzzle".
	 * <p>
	 * If Random.nextLong() returns the same value on two "near" calls, then
	 * this crazy scheme is broken, so use a hash, or something. Sigh.
	 */
	public long puzzleId;

	/** isInvalidated()s message explaining why this grid is invalid. */
	public String invalidity;

	/** isInvalidated()s region which is invalid. Nullable. */
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
	public int numMaybes;

	/** Has a puzzle been loaded into this grid, or is it still a virgin. */
	private boolean isLoaded;

	/** Are the currently displayed hidden sets in box, row, or col. */
	public final HiddenSetDisplayState hiddenSetDisplayState = new HiddenSetDisplayState();

	/** for {@link #getSinglesQueue() } cells with one value, or one place */
	private Deque<Single> singlesQueue;

	/** for {@link #getEmpties() } indices of cells with value==0 */
	private IdxL empties;
	private int emptiesHn;
	private long emptiesPid;

	/** for {@link #getBiplaces() } biplaced regions, by value */
	private IdxL[] bips;
	private int bipsHn;
	private long bipsPid;

	/** for {@link #getBivalue() } indices of bivalue cells */
	private IdxL bivs;
	private int bivsHn;
	private long bivsPid;

	// ----------------------------- constructors -----------------------------

	/**
	 * Construct a new empty 9x9 Sudoku grid.
	 */
	public Grid() {
		int i=0; do cells[i] = new Cell(i, BITS9); while(++i<GRID_SIZE);
		i=1; do idxs[i] = IdxL.full(); while(++i<VALUE_CEILING);
		initialise();
		puzzleIdReset();
	}

	/**
	 * Construct a new 9x9 Sudoku Grid from the given $line, which may
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
	 * {@code 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
	 * ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
	 * C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt}
	 * <p>
	 * Note that I have replaced back-slashes with slashes coz Java hates them!
	 * <p>
	 * <br>
	 * The <b>first line</b><br>
	 * {@code 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..}<br>
	 * is the clues (given cell values) and is 81 characters, with a digit 1..9
	 * representing a clue cell value, and the period (.) character (or indeed
	 * ANY other character) representing an empty (unknown value) cell.
	 * <p>
	 * <br>
	 * The <b>OPTIONAL second line</b><br>
	 * {@code ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357}<br>
	 * contains the cells maybes (potential values). If is a list of 81 comma
	 * separated values (CSV). Each value in the list is a String of all of the
	 * potential values of this cell, just concatenated together. Note that a
	 * clue cells potential values are an empty string (an empty list element).
	 * Alternately, if a maybes string contains a single value then the cell is
	 * a Naked Single, and the reader leaves it as such (ie the Cell is NOT
	 * set to it is only possible value).
	 * <p>
	 * <br>
	 * The <b>OPIONAL third line</b><br>
	 * {@code C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt}<br>
	 * Note: I have replaced back-slashes with slashes coz Java hates them!<br>
	 * contains the source of the puzzle to enable a partially solved puzzle to
	 * report itself as its ACTUAL source puzzle. So for example say I discover
	 * an EbulientAardvard hint in line 42 of top1465.d5.mt, so I copy the grid
	 * and paste it into EbulientAardvard_001.txt; so EbulientAardvard_001.txt
	 * continues to report itself as 42#top1465.d5.mt, allowing me to track the
	 * actual source of each test-case; and may also change how the puzzle is
	 * solved, ie whether or not it is hacked, or atleast used to, before such
	 * over-the-top-cross-country-hackyness fell from grace. Sigh.
	 * <p>
	 * Programmers note this constructor calls {@link #rebuildMaybesAndS__t}
	 * as protection against "foreign agents" who cant be trusted to get the
	 * maybes right; but test-cases need to be able to load a Grid as is, even
	 * if its bdaly borken, so JUnit test-cases use {@link #load(String) in
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
	 * <li>but dont count on this actually working, it just might, thats all
	 * </ul>
	 *
	 * @param line String the puzzle to load, probably in toString format. */
	public Grid(final String line) {
		this(); // run the default constructor first
		// splits on \r\n or \r or \n
		load(MyStrings.splitLines(line)); // just call load rather than re-implement it (badly)
	}

	/**
	 * The copy constructor creates a new "exact" copy of the $src Grid. Cell
	 * values and maybes are copied, as are each regions idxOf, emptyCellCount,
	 * and containsValue arrays.
	 * <p>
	 * If the given src is null then a new empty grid is constructed.
	 *
	 * @param src the source Grid to copy from.
	 */
	public Grid(final Grid src) {
		int i;
		// asserts are programmer (java -ea) only. Prod handles a null src, but
		// this really should NEVER happen, ergo you have done something silly.
		assert src != null;
		if ( src != null ) {
			i=0; do cells[i] = new Cell(src.cells[i]); while(++i<GRID_SIZE);
		} else {
			i=0; do cells[i] = new Cell(i, BITS9); while(++i<GRID_SIZE);
		}
		// build regions, cells.siblings, cells.notSees, regions.idx, puzzleID
		initialise();
		if ( src != null ) {
			// copy everything from src
			i=0; do regions[i].copyFrom(src.regions[i]); while(++i<NUM_REGIONS);
			i=1; do idxs[i] = new IdxL(src.idxs[i]); while(++i<VALUE_CEILING);
			if ( src.source != null ) // nullable, apparently
				source = new SourceID(src.source); // SourceID immutable
			numSet = src.numSet;
			numMaybes = src.numMaybes;
			hintNumber = src.hintNumber;
		}
		// grid.puzzleID tells hinters "we have changed puzzles".
		// this must be the last step in constructor for toString
		puzzleIdReset();
	}

	// Constructor assistant: Does NOT rely on state: cells values or maybes.
	// I just wire up the physical relationships between Grids component parts,
	// like putting each cell in its regions, and finding the crossings, and
	// the "sibling" relationships, and creating indexes. This method is run
	// ONCE only, when the grid is being constructed.
	private void initialise() {
		// populate the grids regions arrays
		int i, v;
		// boxs first so rows and cols intersectingBoxs exist.
		i=0; do regions[i] = boxs[i] = new Box(i); while(++i<REGION_SIZE);
		i=0; do regions[i+REGION_SIZE] = rows[i] = new Row(i); while(++i<REGION_SIZE);
		i=0; do regions[i+FIRST_COL] = cols[i] = new Col(i); while(++i<REGION_SIZE);
		// populate grid.ridxs (two for loops, both post tested)
		i=0; do {v=1; do ridxs[v][i] = regions[i].idxs[v]; while(++v<VALUE_CEILING);} while(++i<NUM_REGIONS);
		// populate boxs intersectors now that rows and cols are populated.
		i=0; do boxs[i].initIntersectors(); while(++i<REGION_SIZE);
		// populate each cells siblings and notSees array
		for ( final Cell cell : cells )
			cell.siblings = cell.buds.cellsNew(cells);
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Get "[lineNumber#]filePath" of this puzzles source.
	 *
	 * @return
	 */
	public String sourceLong() {
		return source!=null ? source.toString() : "IGNOTO";
	}

	/**
	 * Get "[lineNumber#]fileNameOnly" of this puzzles source.
	 *
	 * @return
	 */
	public String sourceShort() {
		return source!=null ? source.toStringShort() : "IGNOTO";
	}

	/**
	 * Get "hintNumber/[lineNumber#]fileName" of this puzzles source.
	 *
	 * @return
	 */
	public String hintSource() {
		return ""+hintNumber+"/"+sourceLong();
	}

	/**
	 * Set this.puzzleId to a random long.
	 * @return a random long that serves as this puzzles identity.
	 */
	public long puzzleIdReset() {
		return puzzleId = new Random().nextLong();
	}

	/** The start hint number is 1, hintNumber is 1-based. */
	public void hintNumberReset() {
		hintNumber = 1; // the first hint
		emptiesHn = bivsHn = -1; // invalidate caches
	}

	// Logical Solver needs to keep track of whether or its prepared to solve
	// this grid, and the easiest way to do that is stick an attribute on the
	// grid which presumes that it has not been prepared yet.
	public boolean isPrepared() {
		return isPrepared;
	}
	public void setPrepared(final boolean isPrepared) {
		this.isPrepared = isPrepared;
	}

	// -------------------------- demi-constructors ---------------------------

	/**
	 * Copy the contents of the given $src grid into this Grid.
	 * <p>
	 * @see #Grid(Grid) the copy constructor.
	 * <p>
	 * WARN: Used in BruteForce, so do please be efficient.
	 *
	 * @param src Grid
	 */
	public void copyFrom(final Grid src) {
		int i;
		i=0; do cells[i].copyFrom(src.cells[i]); while(++i<GRID_SIZE);
		i=0; do regions[i].copyFrom(src.regions[i]); while(++i<NUM_REGIONS);
		i=1; do idxs[i].setOK(src.idxs[i]); while(++i<VALUE_CEILING);
		numSet = src.numSet;
		numMaybes = src.numMaybes;
	}

	/** Copy grid to the O/S clipboard in toString() format, with source. */
	public void copyToClipboard() {
		String s = this.toString();
		if ( source!=null && source.file!=null )
			s += NL + source;
		GridClipboard.copy(s);
	}

	// ------------------------------ mutators --------------------------------

	public void clear() {
		for ( Cell cell : cells )
			cell.clear();
	}

	/**
	 * Load this grid from these 1, 2 or 3 lines, in the format produced by
	 * the {@link #toString} method.
	 * <p>
	 * The test-cases use load because load INTENTIONALLY does <b>NOT</b> call
	 * {@link #rebuildMaybesAndS__t} to fix invalid maybes; thus each puzzle is
	 * loaded "as-is" (even broken), but I still {@link #rebuildAllMyOtherS__t}
	 * because one must have ones s__t together, I just dont ____ with the
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
	 * containing a puzzle, maybe some maybes, and a SourceID string.
	 * <pre>
	 * <b>Example lines</b>:
	 * {@code 8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
	 * ,1246,24569,2347,12357,1234,13569,4579,1345679,12459,124,,,12578,1248,1589,45789,14579,1456,,456,348,,1348,,458,13456,123469,,2469,2389,2368,,1689,2489,12469,12369,12368,269,2389,,,,289,1269,24679,2468,24679,,268,2689,5689,,24569,23457,234,,23479,237,2349,359,,,23467,2346,,,2367,23469,39,,2379,23567,,2567,2378,123678,12368,,257,2357
	 * 1#C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt}
	 * </pre>
	 * Note back-slashes are replaced with slashes because Java hates them.
	 * <p>
	 * If the maybes are supplied EXACTLY the given puzzle is loaded; missing
	 * maybes are NOT corrected, for the test-cases. The Grid must handle an
	 * invalid puzzle, and aspires to not sending puzzles invalid.
	 * <p>
	 * The load method is used by:<ul>
	 * <li>the constructor/s, and
	 * <li>{@link #copyToClipboard}, and
	 * <li>{@link diuf.sudoku.gui.GridLoader.MagicTourFormat#load}
	 * <li>and lots and lots of JUnit test-cases.
	 * </ul>
	 *
	 * @param lines expect 1, 2, or 3 lines: 1=values, 2=maybes, 3=SourceID
	 * @return success.
	 */
	public boolean load(final String[] lines) {
		puzzleId = 0;
		// give up immediately if there is no chance of a sucessful load
		if ( lines==null || lines.length==0 )
			return false; // meaning load failed
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
					for ( Cell cell : cells )
						cell.initMaybes(); // fill if empty, else clear
					// note that cell-values are removed from siblings down in
					// rebuildAllMyOtherS__t, along with indexes, sizes, etc.
				}
			} catch (Exception ex) {
				StdErr.carp("WARN: Grid.load: bad maybes: "+line, ex);
			}
			try {
				// third line (if any) is a SourceID
				// 1#C:/Users/User/Documents/SudokuPuzzles/Conceptis/TheWorldsHardestSudokuPuzzle.txt
				if ( lines.length>=3 && (line=lines[2])!=null )
					source = SourceID.parse(line);
			} catch (Exception ex) {
				StdErr.carp("WARN: Grid.load: bad source: "+line, ex);
			}
			// remove any bad maybes (sees a cell set this value)
			if ( !isMaybesLoaded )
				rebuildRemoveInvalidMaybes();
			// rebuild the emptyCellCount arrays
			rebuildAllRegionsEmptyCellCounts();
			// rebuild the indexes
			rebuildIndexes(true);
			// calculate numMaybes: total number of maybes in this grid
			countNumMaybes();
			// hintNumber is used to determine if caches are dirty
			countNumSet();
			// hintNumber is used to determine if caches are dirty
			hintNumberReset();
			// puzzleID is used to determine if caches are dirty
			puzzleIdReset();
			// reset the regionType for displaying hidden sets.
			hiddenSetDisplayState.reset();
			// this Grid has now finished loading
			isLoaded = true;
			// cache the solution, now, before user sets any cell values.
			solution();
			return true; // meaning load succeeded
		} catch (Exception ex) {
			StdErr.whinge("WARN: Grid.load critical", ex);
			isLoaded = true;
			return false; // meaning load failed
		}
	}

	/**
	 * Restores this grid from the given String, which is the format produced
	 * by {@code toString}, including the optional second line of maybes. We
	 * restore a grid only from a toString of itself, so his id remains, only
	 * his maybes and set cells change. I should use a modificationCount, like
	 * a java.util.Collection, but I do not. Grid.version is the slow process
	 * of counting the maybes.
	 *
	 * @param s String
	 */
	public void restore(final String s) {
		// line 1: 81 cell values ('.' means 0)
		// optional line 2: comma seperated list of 81 maybes-values-sets
		if ( s.length() < GRID_SIZE )
			throw new IllegalArgumentException("bad s=\""+s+"\"");
		if ( isLoaded )
			clear();
		loadCellValues(s.substring(0, GRID_SIZE)); // the first line
		final String ss = s.substring(GRID_SIZE).trim(); // the second line
		if ( ss.isEmpty() )
			rebuildMaybes();
		else
			loadMaybes(ss.split(COMMA, GRID_SIZE));
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
		Cell c, cs[];
		ARegion r;
		final int[] vs = backup.values;
		final int[] ms = backup.maybes;
		int i = 0, ri = 0;
		do {
			c = cells[i];
			c.value = vs[i];
			sizes[i] = c.size = VSIZE[maybes[i] = c.maybes = ms[i]];
		} while (++i < GRID_SIZE);
		do {
			r = regions[ri];
			cs = r.cells;
		} while (++ri < NUM_REGIONS);
		countNumSet(); // sets numSet
		countNumMaybes(); // sets numMaybes
		rebuildAllRegionsEmptyCellCounts();
		rebuildIndexes(true);
	}

	// load the given n clues into n cells, where max n is 81.
	private boolean loadCellValues(final String line) {
		final int n = MyMath.min(line.length(), GRID_SIZE);
		char c;
		for ( int i=0; i<n; ++i )
			if ( (c=line.charAt(i))>='1' && c<='9' )
				cells[i].value = c - '0';
		return n == GRID_SIZE; // even if line chopped (optimistic)
	}

	private void loadMaybes(final String[] fields) {
		for ( int i=0; i<GRID_SIZE; )
			cells[i].setMaybes(Values.parse(fields[i++]));
	}

	// -------------------------- Rob the rebuilder ---------------------------

	/**
	 * Rebuild everything, except the Maybes:<ul>
	 * <li>{@link #rebuildRemoveInvalidMaybes}<ul>
	 *  <li>{@link Cell#removeFromMySiblingsMaybes}<ul>
	 *   <li>{@link Cell#canNotBe}<ul>
	 *    <li>{@link Cell#removeMaybes}<ul>
	 *     <li>{@link Grid#idxs}[v].{@link IdxL#removeOK}
	 *     <li>{@link Cell#removeMeFromMyRegionsIdxsOf}<ul>
	 *      <li>{@link ARegion#idxs}[v].{@link Idx#remove} for box/row/col
	 *     </ul>
	 *     <li>{@link Cell#removeMeFromMyRegionsPlacesOf}<ul>
	 *      <li>{@link ARegion#places}[v].{@link Indexes#remove} for box/row/col
	 *     </ul>
	 *     <li>sets {@link Cell#getMaybes}, {@link Cell#count} and {@link Grid#numMaybes}
	 *    </ul>
	 *   </ul>
	 *  </ul>
	 * <li>{@link #rebuildAllRegionsS__t}<ul>
	 *  <li>{@link #rebuildAllRegionsEmptyCellCounts}<ul>
	 *   <li>{@link ARegion#emptyCellCount}
	 *   </ul>
	 *  <li>{@link #rebuildAllRegionsSetCands}<ul>
	 *   <li>{@link ARegion#rebuildSetCands}
	 *   </ul>
	 *  </ul>
	 * </ul>
	 * <li>{@link #rebuildIndexes}<ul>
	 *  <li>{@link #rebuildAllRegionsIndexsOfAllValues}<ul>
	 *   <li>NOT HERE {@link #rebuildAllRegionsSetCands}<ul>
	 *    <li>{@link ARegion#rebuildSetCands}
	 *    </ul>
	 *   <li>{@link ARegion#rebuildPlacesOfAllValues}
	 *   </ul>
	 *  </ul>
	 * </ul>
	 * <p>
	 * <pre>
	 * 50 lines of doco for a two line method!
	 * You see why I <i>mildly despise</i> doco?
	 * Q: How long will it take to go out of date?
	 * A: It went OD while I was writing it!
	 *    which improved software quality;
	 *    and thats why do doco Dicko.
	 * </pre>
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
		for ( Cell cell : cells )
			if ( cell.value > 0 )
				cell.clearMaybes();
			else
				cell.setMaybes(BITS9 & ~cell.seesValues());
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
	 *   if {@link cell#value} > 0<br>
	 *   {@link Cell#removeFromMySiblingsMaybes}(cell.value)
	 * </ul>
	 */
	public void rebuildRemoveInvalidMaybes() {
		for ( final Cell cell : cells )
			if ( cell.value > 0 )
				cell.removeFromMySiblingsMaybes(cell.value);
	}

	/**
	 * rebuildAllRegionsS__t does:<ul>
	 *  <li>{@link #rebuildAllRegionsEmptyCellCounts}<ul>
	 *   <li>{@link ARegion#emptyCellCount}
	 *  </ul>
	 *  <li>{@link #rebuildAllRegionsSetCands}<ul>
	 *   <li>{@link ARegion#rebuildSetCands}
	 *  </ul>
	 * </ul>
	 */
	public void rebuildAllRegionsS__t() {
		rebuildAllRegionsEmptyCellCounts();
		rebuildAllRegionsSetCands();
	}

	/**
	 * rebuildAllRegionsEmptyCellCounts does:<ul>
	 *  <li>foreach region in {@link Grid#regions}<br>
	 *   {@link ARegion#emptyCellCount()}<br>
	 *   which sets {@link ARegion#emptyCellCount}
	 * </ul>
	 */
	public void rebuildAllRegionsEmptyCellCounts() {
		for ( ARegion r : regions )
			r.emptyCellCount(); // which sets r.emptyCellCount
	}

	/**
	 * rebuildAllRegionsSetCands does:<ul>
	 *  <li>foreach region in {@link Grid#regions}<br>
	 *   rebuild regions setCands and unsetCands
	 * </ul>
	 */
	public void rebuildAllRegionsSetCands() {
		for ( ARegion r : regions ) {
			r.setCands = 0;
			for ( Cell cell : r.cells )
				r.setCands |= VSHFT[cell.value];
			r.unsetCands = BITS9 & ~r.setCands;
		}
	}

	/**
	 * rebuildIndexes does:<ul>
	 *  <li>if doSetCands then {@link #rebuildAllRegionsSetCands}
	 *  <li>rebuild all regions places of all values
	 *  <li>rebuild Grid.idxs of all values
	 *  <li>rebuild all regions Idxs of all values
	 * </ul>
	 *
	 * @param doSetCands should I {@link #rebuildAllRegionsSetCands}
	 */
	public void rebuildIndexes(boolean doSetCands) {
		int v, sv, i;
		long m0=0L; int m1=0;
		// rebuild all regions setCands
		if ( doSetCands )
			rebuildAllRegionsSetCands();
		// rebuild all regions places of all values
		for ( ARegion r : regions )
			r.rebuildPlacesOfAllValues();
		// rebuild grid.idxs of all values
		for ( v=1; v<VALUE_CEILING; ++v ) {
			sv = VSHFT[v];
			i = 0;
			do
				// if cell is not set
				if ( (maybes[i] & sv) > 0 ) // 9bits
					m0 |= MASKED81[i];
			while (++i < FIFTY_FOUR);
			do
				// if cell is not set
				if ( (maybes[i] & sv) > 0 ) // 9bits
					m1 |= MASKED[i];
			while (++i < GRID_SIZE);
			idxs[v].setOK(m0, m1);
			m0 = m1 = 0;
		}
		// rebuild all regions idxs of all values
		for ( final ARegion r : regions )
			for ( v=1; v<VALUE_CEILING; ++v )
				r.idxs[v].setAnd(r.idx, idxs[v]);
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
		for ( final Cell cell : cells )
			cell.cancelMaybes();
	}

	/**
	 * Used by {@link diuf.sudoku.gen.Generator} before solving the puzzle.
	 *
	 * @return
	 */
	public Grid prep() {
		rebuildMaybesAndS__t();
		rebuildIndexes(false);
		return this;
	}

	/**
	 * I rip-out any wrong cell values as part of the GUIs lastDitchRebuild.
	 */
	public void rebuildRemoveWrongCellValues() {
		final int[] solution = solution();
		if ( solution == null )
			throw new IllegalStateException("No Solution!");
		for ( Cell cell : cells )
			if ( cell.value!=0 && cell.value!=solution[cell.indice] )
				cell.value = 0;
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
	 * we are single-threaded and the invalidity field is read BEFORE my next
	 * execution, but if you are calling me from multiple threads on a single
	 * Grid instance then you will need a new Invalidity class to hold the two
	 * fields (renamed message and region), and I will return null meaning its
	 * all good, else a new instance of our new Invalidity class.
	 *
	 * @return true if this grid is invalid, else false.
	 */
	public boolean isInvalidated() {
		invalidity = null;
		invalidRegion = null;
		return hasMissingMaybes()
			|| firstDoubledValue() > 0
			|| hasHomelessValues();
	}

	/**
	 * returns true if any unset cell in this grid has no potential values
	 * remaining, else false. If the grid is invalid then the invalidity field
	 * is set.
	 *
	 * @return does any empty cell have no potential values.
	 */
	public boolean hasMissingMaybes() {
		for ( final Cell c : cells )
			if ( c.value==0 && c.size==0 ) {
				invalidity = c.id+" has no macs remaining";
				return true;
			}
		return false;
	}

	/**
	 * returns the first cell.value which appears twice in any region in this
	 * grid, else 0 meaning none. If the grid is invalid then the invalidity
	 * and invalidRegion fields are set. Note that there (silently) may be
	 * multiple repeated values in the grid, this finds just the first one.
	 *
	 * @return 0 (valid) else (invalid) the first cell-value that appears
	 *  multiple times in any region in this grid.
	 */
	public int firstDoubledValue() {
		int seen;
		for ( final ARegion r : regions ) {
			seen = 0;
			for ( final Cell c : r.cells )
				if ( c.value > 0 )
					if ( (seen & VSHFT[c.value]) == 0 )
						seen |= VSHFT[c.value];
					else {
						invalidity = "Multiple "+c.value+"s in "+r;
						invalidRegion = r;
						return c.value;
					}
		}
		return 0;
	}

	/**
	 * returns true if any region in this grid has no place remaining for any
	 * unplaced value, else false. If the grid is invalid then both the
	 * invalidity and invalidRegion fields are set.
	 *
	 * @return does any unplaced value have no place remaining in any region.
	 */
	public boolean hasHomelessValues() {
		for ( final ARegion r : regions )
			for ( int v=1; v<VALUE_CEILING; ++v )
				if ( r.places[v]==0 && (r.setCands & VSHFT[v])==0 ) {
					invalidity = "The value "+v+" has no place in "+r.label;
					invalidRegion = r;
					return true;
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
		return cells[indice(id)];
	}

	/**
	 * Get Cell[]s for ids, for the test-cases.
	 * <p>
	 * Prefer getCSV, which accepts toString format. I am here if you have
	 * already got an array of IDs.
	 * <p>
	 * I create a new {@code Cell[]} which is a bit slow. One MUST avoid the
	 * creation of garbage insides ones tight loops, and in Java this sort of
	 * secret-factory method a GREAT place to hide garbage construction, so we
	 * do not; we call toArray toArrayNew, for example.
	 * <p>
	 * This method is ONLY used in test-cases.
	 *
	 * @param ids varargs of cell.ids: "A1", "I9"
	 * @return Cell[]
	 */
	public Cell[] getNew(final String[] ids) {
		final int n = ids.length;
		final Cell[] result = new Cell[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = get(ids[i]);
		}
		return result;
	}

	/**
	 * Get Cell[]s for this CSV list of cell.id, for test-cases.
	 * <p>
	 * This method is ONLY used in test-cases.
	 *
	 * @param csv of cell.ids: "A1, I9"
	 * @return Cell[]
	 */
	public Cell[] getCsv(final String csv) {
		return getNew(csv.split(" *, *"));
	}

	/**
	 * Returns a new int-array containing the indices of the cells in grid that
	 * match the filter $f.
	 * <p>
	 * Used in {@link diuf.sudoku.solver.hinters.wing.XYWing#findHints }
	 *
	 * @param filter whose accepted cells are added
	 * @return a new int[numAccepted]
	 */
	public int[] indicesNew(final IFilter<Cell> filter) {
		// first count accepted cells, coz we need size to construct the array
		int count=0, i=0;
		do
			if ( filter.accept(cells[i]) )
				++count;
		while (++i < GRID_SIZE);
		if ( count < 1 )
			return THE_EMPTY_INT_ARRAY;
		final int[] array = new int[count];
		// populate the array with accepted cells
		count = i = 0;
		do
			if ( filter.accept(cells[i]) )
				array[count++] = i;
		while (++i < GRID_SIZE);
		return array;
	}

	/**
	 * Return numSet = the number of set cells
	 *
	 * @return Count the number of cells with value!=0
	 */
	public int countNumSet() {
		int count = 0, i = 0;
		do
			if ( cells[i].value > 0 )
				++count;
		while (++i < GRID_SIZE);
		return numSet = count;
	}

	/**
	 * Currently only used to log "has grid changed" after hint applied.
	 * @return Count the total number of maybes of all cells in this grid.
	 */
	int countNumMaybes() {
		int sum=0, i=0; do sum += sizes[i]; while(++i<GRID_SIZE);
		return numMaybes = sum;
	}

	// ---------------------------- IS_HACKY stuff ----------------------------

	/**
	 * Get THE {@code Deque<Single>} in which you stash the singles (cells with
	 * one possible value or a single place in one of its three regions, ergo
	 * a Naked or Hidden Single).
	 *
	 * @return THE queue for stashing singles
	 */
	public Deque<Single> getSinglesQueue() {
		if ( singlesQueue == null )
			return singlesQueue = new MyLinkedList<>();
		singlesQueue.clear();
		return singlesQueue;
	}

	// ------------------------- toString and friends -------------------------

	// append cell values to $sb (with '.' as 0)
	private StringBuilder appendCellValues(final StringBuilder sb) {
		if ( cells[80] != null ) // not under construction
			for ( final Cell cell : cells )
				sb.append(DIGITS[cell.value]);
		return sb;
	}

	/**
	 * @return a String representation of the values in this grid, being a
	 * single line of 81 cell values, with 0s (empty cells) represented by a
	 * period character (.).
	 * <p>For example:<pre>
	 * 1...9..3...2..3.5.7..8.......3..7...9.........48...6...2..........14..79...7.68..
	 * </pre>
	 */
	public String toShortString() {
		return appendCellValues(SB(128)).toString();
	}

	/**
	 * Returns a String representation of this Grid.
	 * <p>
	 * This String is sort-of human readable: its primary focus is technical.
	 * It should be kept away from GUI users/programmers. Kick!
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
		if ( cells[80] == null ) // under construction
			return "constructing";
		if ( numSet > 80 ) // grid full
			return toShortString();
		final StringBuilder sb = SB(512); // max observed 447
		appendCellValues(sb).append(NL);
		// append the maybes
		sb.append(MAYBES[maybes[0]]);
		int i=1; do sb.append(',').append(MAYBES[maybes[i]]); while(++i<GRID_SIZE);
		return sb.toString();
	}

	// ------------------------------- plumbing -------------------------------

	/**
	 * Compare two grids for equality on cell values and maybes. O(81).
	 *
	 * @param o o is for other. He Did It!
	 * @return do I have the same values and maybes as the other guy
	 */
	@Override
	public boolean equals(Object o) {
		return o!=null && (o instanceof Grid) && equals((Grid)o);
	}

	/**
	 * Does each cell in this Grid, be it set (value) or empty (maybes),
	 * equals each cell in other Grid.
	 * <p>
	 * <b>WARNING</b>: presumes the other Grid is NOT null, for speed!
	 *
	 * @param other to compare
	 * @return are all cell values/maybes equal
	 */
	public boolean equals(final Grid other) {
		final Cell[] him = other.cells;
		final Cell[] me = this.cells;
		int i = 0;
		do
			if ( me[i].value != him[i].value
			  || me[i].maybes != him[i].maybes )
				return false;
		while (++i<GRID_SIZE);
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
	 * never mutate a Grid used as a hashKeys, so no problem. I dont think Grid
	 * is used as a key, only a value.
	 *
	 * @return the hashCode of this Grid.
	 */
	@Override
	public int hashCode() {
		Cell c;
		int hc = 0;
		// only the last 32 cells appear in the hashCode
		for ( int i=GRID_SIZE-32; i<GRID_SIZE; ++i )
			hc = hc<<1 ^ (c=cells[i]).value ^ c.maybes;
		return hc;
	}

	// -------------------------------- Idxs ---------------------------------
	// NB: My at methods have been replaced by Idx.cells methods.

	/**
	 * Get an Idx of all empty (value == 0) cells in this grid.
	 * @return a CACHED Idx of all empty cells in this grid.
	 */
	public Idx getEmpties() {
		if ( empties == null ) // first time for this Grid
			return empties = fetchEmpties(new IdxL()).lock();
		if ( emptiesHn!=hintNumber || emptiesPid!=puzzleId )
			return fetchEmpties(empties.unlock().clearMe()).lock();
		return empties;
	}
	private IdxL fetchEmpties(final IdxL idx) {
		emptiesHn = hintNumber;
		emptiesPid = puzzleId;
		long m0=0L; int m1=0, i;
		// the first 54 go in m0
		for ( i=0; i<FIFTY_FOUR; ++i )
			if ( maybes[i] > 0 )
				m0 |= MASKED81[i];
		// the last 27 go in m1
		for ( ; i<GRID_SIZE; ++i )
			if ( maybes[i] > 0 )
				m1 |= MASKED[i];
		return idx.set(m0, m1);
	}

	/**
	 * Get an Idx of cells in this grid with maybesSize == 2.
	 *
	 * @return a locked cached Idx of bivalue cells in this grid.
	 */
	public IdxL getBivalue() {
		if ( bivs == null ) {
			bivs = new IdxL().read(cells, (c) -> c.size==2).lock();
			bivsHn = hintNumber;
			bivsPid = puzzleId;
		} else if ( bivsHn!=hintNumber || bivsPid!=puzzleId ) {
			bivs.unlock().clearMe().read(cells, (c) -> c.size==2).lock();
			bivsHn = hintNumber;
			bivsPid = puzzleId;
		}
		return bivs;
	}
	/**
	 * if force is true then {@link #getBivalue()} ALWAYS reinitialises; else
	 * bivs are cached until the grid.hintNumber changes (as per normal).
	 *
	 * @param isFetchForced true forces getBivalue to re-fetch? This caters for
	 *  generate (et al) where the bivalue cells are dirty, because the grid
	 *  has changed underneath us
	 * @return {@code getBivalue()}
	 */
	public IdxL getBivalue(final boolean isFetchForced) {
		if ( isFetchForced )
			bivsHn = -1; // impossible
		return getBivalue();
	}

	/**
	 * Get an IdxL per value of all cells that are "biplaced", ie there are two
	 * places for this value in a region. So an IdxL contains indices of cells,
	 * each of which is biplaced in ANY of its three regions.
	 *
	 * @return a CACHED IdxL[] of biplaced regions by value.
	 */
	public IdxL[] getBiplaces() {
		if ( bips == null ) {
			bips = new IdxL[VALUE_CEILING];
			for ( int v=1; v<VALUE_CEILING; ++v )
				bips[v] = new IdxL();
			fetchBiplaces();
		} else if ( bipsHn!=hintNumber || bipsPid!=puzzleId ) {
			// clearing should be unnecessary, but its MUCH cleaner
			for ( int v=1; v<10; ++v )
				bips[v].unlock().clear();
			fetchBiplaces();
		}
		return bips;
	}
	private void fetchBiplaces() {
		for ( ARegion r : regions )
			for ( int v : VALUESES[r.unsetCands] )
				if ( r.numPlaces[v] == 2 )
					bips[v].or(r.idxs[v]);
		bipsHn = hintNumber;
		bipsPid = puzzleId;
		for ( int v=1; v<VALUE_CEILING; ++v )
			bips[v].lock();
	}

	/**
	 * Populate the given array with the value of each Cell in this Grid.
	 *
	 * @param result the array to populate
	 * @return the given values array, so that you can create it on-the-fly.
	 */
	public int[] getValues(final int[] result) {
		final Cell[] mine = this.cells;
		int i = 0;
		do
			result[i] = mine[i].value;
		while (++i < GRID_SIZE);
		return result;
	}

	/**
	 * Returns a new array of the value of each of the 81 cells in this Grid.
	 *
	 * @return a new int[GRID_SIZE] of cell values
	 */
	public int[] getValues() {
		return getValues(new int[GRID_SIZE]);
	}

	/**
	 * Populate the given array with the maybes of each cell in this Grid.
	 *
	 * @param result the array to populate
	 * @return the passed maybes array, so that you can create it on-the-fly.
	 */
	public int[] getMaybes(final int[] result) {
		int i = 0;
		do
			result[i] = maybes[i];
		while (++i < GRID_SIZE);
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
		final Integer shiftedValue = sv;
		for ( int i=0; i<GRID_SIZE; ++i )
			if ( (maybes[i] & sv) > 0 ) // 9bits
				result.put(i, shiftedValue);
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
	 * If $size == 9 then you are a cheating mofo!
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
			for ( Cell cell : cells )
				if ( cell.size == size )
					result.put(cell.indice, cell.maybes);
		} else if ( size < 9 ) {
			// cells with size in 2..$size-2 EXCLUSIVE
			final int ceiling = size - 2; // 6=4, 7=5, 8=6
			for ( Cell cell : cells )
				if ( cell.size>1 && cell.size<ceiling )
					result.put(cell.indice, cell.maybes);
		} else {
			// size == 9
			final int[] sol = solution();
			if ( sol != null ) // ignore Exception
				for ( Cell cell : cells )
					if ( cell.value == 0 )
						result.put(cell.indice, VSHFT[sol[cell.indice]]);
		}
		return result;
	}

	/**
	 * Returns a Pots containing both Naked and Hidden Singles.
	 *
	 * @return a Pots containing any Naked and Hidden Singles.
	 */
	public Pots getSinglesPots() {
		final Pots result = new Pots(64, 1.0F);
		// Naked Singles
		for ( Cell c : cells )
			if ( c.size == 1 )
				result.put(c.indice, c.maybes);
		// Hidden Singles
		for ( ARegion r : regions )
			for ( int v : VALUESES[r.unsetCands] )
				if ( r.numPlaces[v] == 1 )
					result.put(r.indices[IFIRST[r.places[v]]], VSHFT[v]);
		return result;
	}

	/**
	 * returns boxs for $regionType=0, rows for 1, cols for 2.
	 *
	 * @param regionType 0=BOX, 1=ROW, 2=COL
	 * @return regions in this grid of the given type.
	 */
	private ARegion[] getRegionsByType(final int regionType) {
		switch ( regionType ) {
		case BOX: return boxs;
		case ROW: return rows;
		case COL: return cols;
		default: throw new IllegalArgumentException("Unknown regionType: "+ regionType);
		}
	}

	/**
	 * return a new Pots containing cells/values in regions of $regionType
	 * (BOX, ROW, or COL) having 2..$maxSize places for values 1..9.
	 *
	 * @param maxSize int 2, 3, or 4 please, else behaviour is undefined.
	 * @param regionType int; either boxs, rows, or cols.
	 * @return some Pots.
	 */
	public Pots getHiddenSets(final int maxSize, final int regionType) {
		final Pots result = new Pots(64, 1.0F);
		for ( ARegion r : getRegionsByType(regionType) )
			for ( int v=1; v<10; ++v )
				if ( r.numPlaces[v] > 1 && r.numPlaces[v] <= maxSize )
					for ( int i : INDEXES[r.places[v]] )
						result.upsert(r.indices[i], VSHFT[v], false);
		return result;
	}

	/**
	 * Store $regions with 2 places for $v in the $pairs array, returning how
	 * many.
	 * <p>
	 * Note that pairs is static. You pass me in my own rows or cols field as
	 * regions. I collect regions having two places for $v. Well, actually I
	 * collect the two cells that are the two places for $v, not the regions,
	 * but brains aren't compilers, and its short, hence meaningful. TLDR has
	 * overtaken Cornflakes adds as the leading cause of stupidity. One should
	 * learn the advertisers language, so thine mite render thou asses funny.
	 * They are DEFINTELY my elderberries you numpty! Malfoy ducks. Petunia was
	 * pleased.
	 *
	 * @param regions grid.rows/grid.cols
	 * @param v current potential value 1..9
	 * @param pairs array to add pairs to [howeverMany][2], 20 should do
	 * @return how many
	 */
	public static int pairs(final ARegion[] regions, final int v, final Cell[][] pairs) {
		int numPairs = 0;
		for ( ARegion r : regions )
			if ( r.numPlaces[v] == 2 ) {
				pairs[numPairs][0] = r.cells[INDEXES[r.places[v]][0]];
				pairs[numPairs][1] = r.cells[INDEXES[r.places[v]][1]];
				++numPairs;
			}
		return numPairs;
	}

	/**
	 * Fetch a cached array containing the correct value of each cell in this
	 * grid.
	 *
	 * @return the solution array of 81 correct cell values.
	 */
	public int[] solution() {
		// NOTE: BruteForce.solve now tries first with logic and
		// upon failure without logic, for speed AND reliability.
		if ( solution==null && numSolutionFailures<5 )
			try {
				solution = LogicalSolverFactory.get().solve(this).getValues();
			} catch (Exception ex) {
				++numSolutionFailures;
				Log.whinge("WARN: "+Log.me()+" failure "+numSolutionFailures, ex);
				if ( numSolutionFailures > 6 )
					throw ex;
			}
		return solution;
	}
	private int numSolutionFailures;

	/**
	 * Returns a new {@code Set<Cell>} containing cells which have the wrong
	 * value, ie where the cells value does not match the solution; else null
	 * meaning that there are no wrongens (cells with wrong values). No Warnies
	 * where harmed in the making of this software, just mildly peeved is all.
	 *
	 * @return a {@code Set<Cell>} containing cells with incorrect values, else
	 *  null meaning that there are no incorrect with bad values.
	 */
	public Set<Integer> getWrongens() {
		Set<Integer> result = null;
		final int[] sol = solution();
		if ( sol == null )
			return result; // ignore solution() failure
		for ( Cell c : cells )
			if ( c.value>0 && c.value!=sol[c.indice] ) {
				if(result == null) result = new HashSet<>();
				result.add(c.indice);
			}
		return result;
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
	 * @param csv Comma Separated Values of region.ids
	 * @return
	 */
	public ARegion[] regions(final String csv) {
		final String[] fields = csv.split(" *, *"); // region.ids
		final int n = fields.length;
		final ARegion[] result = new ARegion[n];
		for ( int i=0; i<n; ++i )
			result[i] = region(fields[i]);
		return result;
	}

	// ============================ the Cell class ===========================

	/**
	 * The public inner Cell class represents one of the 81 cells in a Sudoku
	 * Grid. Its purpose is to remember stuff, not really do stuff.
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
	 * <li>regions is my Box, my Row, and my Col
	 * <li>hashCode is {@code = y<<4 + x} so I can be a HashMap key
	 * <li>{@code public int[] maybesArray;} is the potential values of this
	 * cell. Populated by arrayonateShiftedMaybes() and weweased with
	 * disarrayonateMaybes(). The array is <b>NOT maintained</b>!
	 * <li>{@code public Map<String,Object> properties = null;} gives you a
	 * way to add any additional properties you require. Create a new
	 * {@code HashMap<String,Object>} (or whatever) and attach it here.
	 * Also please document it right here!
	 * </ul>
	 * So Grids primary role is to remember stuff to help solve Sudoku puzzles.
	 * I have accrued lots of methods over time, typical for a class that does
	 * not actually do anything. Dont panic, yet.
	 */
	public final class Cell implements Comparable<Cell> {

		/**
		 * indice in the grid.cells array, a single index array that holds a
		 * matrix of cells: 9 rows of 9 cells.
		 * <pre>
		 * So: {@code i = y * 9 + x}
		 * and: {@code y = i / 9} where y is my vertical (row) index
		 * and: {@code x = i % 9} and x is my horizontal (col) index
		 * NOTE: In SE "indice" always means a cells index in grid.cells,
		 * to differentiate this from all other indexes in the codebase,
		 * because indices are used heavily, so they are special.
		 * </pre>
		 */
		public final int indice;

		/**
		 * id is a String that uniquely identifies this Cell in the Grid.
		 * In IdScheme.Chess thats column-letter and row-number: A1..I9.
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
		 * I am {@code this.box.cells[b]}. <br>
		 * so: {@code b = (y%3*3) + (x%3)}
		 */
		public final int b;

		/**
		 * y is my vertical index in my col.
		 * <p>
		 * x,y verses row,col where x=col and row=y: There are 2 orders, so we
		 * use both of them. Houston We Have A Problem. Succinctly, ____wits!
		 */
		public final int y;

		/**
		 * x is my horizontal index in my row.
		 * <p>
		 * x,y verses row,col where x=col and row=y: There are 2 orders, so we
		 * use both of them. Houston We Have A Problem. Succinctly, ____wits!
		 */
		public final int x;

		/**
		 * The Box which contains this Cell.
		 */
		public Box box; // set by Box constructor

		/**
		 * The Row which contains this Cell.
		 */
		public Row row; // set by Row constructor

		/**
		 * The Col which contains this Cell.
		 */
		public Col col; // set by Col constructor

		/**
		 * This one is weird: arrayonateShiftedMaybes() populates shiftedMaybes
		 * with the left-shifted-bitset-values of this cells maybes.
		 * See {@link Values} regarding the bitset format.
		 */
		public int[] shiftedMaybes;

		/**
		 * An Idx of the 20 cells in same box, row, and col as this Cell,
		 * except this cell itself. Note that {@link Grid#BUDDIES} contains
		 * the same Idxs, to save you getting the cell to get its buddies.
		 */
		public final IdxL buds; // set in constructor

		/**
		 * An array of my set of 20 sibling cells.
		 */
		public Cell[] siblings;

		/**
		 * The 3 regions which contain this Cell: 0=box, 1=row, 2=col.
		 * <p>
		 * Unusually, regions is populated by the Box/Row/Col constructors, and
		 * not by the Cell itself.
		 */
		public final ARegion[] regions = new ARegion[3];

		/**
		 * Region Indexes BitSet of this cell. 27bits containing the indexes of
		 * the box, row, and col that contain this cell. These indexes are
		 * indexes in Grid.regions. Boxes are first 9bits, rows second 9bits,
		 * and cols third 9bits. If you need to you can demangle this number
		 * back into the indexes using Indexes.INDEXES on a shifted and masked
		 * bitset: {@code INDEXES[cell.ribs>>18 & ALL_INDEXES_BITS]} gets you
		 * the indexes in Grid.cols of this cells col, ergo cell.x.
		 * <p>
		 * What I do with these bitsets is look for distinctness in:
		 * {@link diuf.sudoku.solver.hinters.urt.URT#isLoop}, and
		 * {@link Regions#commonRibs(diuf.sudoku.Pots)}, and
		 * {@link diuf.sudoku.solver.hinters.chain.ChainerUnary#cycleHint}
		 */
		public final int ribs;

		/**
		 * My index in the cells array of my Box, Row, and Col.
		 */
		public final int[] placeIn;

		/**
		 * sees boolean[]: Does this Cell see the given Cell?
		 * Coincident with Grid.cells.
		 */
		public final boolean[] sees;

		/**
		 * Construct a new Cell in this grid.
		 * <b>NOTE</b> the parameters are "logical" x, y (not "physical" y, x).
		 * <b>NOTE</b> each cell has an intrinsic reference to its containing
		 * grid, whether you like it or not, so a reference to a Cell holds the
		 * whole Grid, so dont litter.
		 *
		 * @param indice the index this cell the grid.cells array; <br>
		 *  so my y coordinate (row) is i/9, <br>
		 *  and my x coordinate (col) is i%9, <br>
		 *  so I am grid.cells[y*9 + x], <br>
		 *  or simply grid.cells[c.i], where i is short for indice
		 * @param cands a bitset of the potential values of this cell. <br>
		 *  For an empty grid pass me VALL=511="111111111"="123456789". <br>
		 *  For the copy constructor pass me the source-cells maybes.
		 */
		public Cell(final int indice, final int cands) {
			// set the id first, for the toString method (a cheat)
			this.id = CELL_IDS[this.indice = indice];
			// y is my vertical (row) coordinate (0=top, 8=bottom),
			this.y = indice / N;
			// x is my horizontal (col) coordinate (0=left, 8=right),
			this.x = indice % N;
			// b is my index in my box.cells (0=top-left, 8=bottom-right)
			this.b = (y%R*R) + (x%R);
			// my place in my box, row, col; as a places bitset (pre-shifted)
			this.placeIn = new int[] {
					  ISHFT[this.b]		// BOX
					, ISHFT[this.x]		// ROW
					, ISHFT[this.y]		// COL
			};
			this.ribs = RIBS[indice];
			numMaybes += size = VSIZE[maybes = cands];
			Grid.this.sizes[indice] = size;
			Grid.this.maybes[indice] = cands;
			// get an array of cells that I see (same box/row/col)
			this.sees = SEES[indice];
			// FINALLY set buds for the Cell.toString method (a cheat).
			// To be clear, this MUST be the last constructor instruction.
			// If you must move it then rejig the Cell.toString cheat.
			this.buds = BUDDIES[indice];
		}

		/**
		 * Copy Constructor: Constructs a new Cell that is a "clone" of src.
		 *
		 * @param src the cell to copy.
		 */
		public Cell(final Cell src) {
			this(src.indice, src.maybes);
			this.value = src.value;
		}

		/**
		 * Copy the cell-value and maybes from the $src cell.
		 *
		 * @param src the cell to copy.
		 */
		public void copyFrom(final Cell src) {
			value = src.value;
			Grid.this.maybes[indice] = maybes = src.maybes;
			Grid.this.sizes[indice] = size = src.size;
		}

//KEEP4DOC: only place we do this interesting math.
//		/**
//		 * boxIndex is my boxs index in the Grid.boxs array, hence invariant:
//		 * {@code assert cell = Grid.boxs[cell.boxIndex()].cells[cell.b];}
//		 *
//		 * @return the Grid.boxs index of this.box, ergo this.box.index, which
//		 *  is faster, because it need not do the math.
//		 */
//		public int boxIndex() {
//			return (y/R*R) + (x/R);
//		}

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
			setMaybes(value==0 ? BITS9 : 0);
		}

		/**
		 * setMaybes sets the maybes, there size, and updates the totalSize.
		 *
		 * @param cands the maybes to be set
		 */
		private void setMaybes(final int cands) {
			// out with the old, and in with the new
			Grid.this.numMaybes += -size + (size=VSIZE[maybes=cands]);
			Grid.this.maybes[indice] = maybes;
			Grid.this.sizes[indice] = size;
		}

		/**
		 * Remove pinkos from this cells box/row/col places and numPlaces.
		 * <p>
		 * I am package visible for the test-cases. Sigh.
		 * <p>
		 * 2023-06-05: ONLY used in test-cases, to set-up grid, hence retained.
		 *
		 * @param pinkos a bitset of the potential values to be removed
		 */
		void removeMeFromMyRegionsPlacesOfCands(final int pinkos) {
			for ( int v : VALUESES[pinkos] ) {
				// remove me from my regions placesOf
				box.numPlaces[v] = ISIZE[box.places[v] &= ~ISHFT[b]];
				row.numPlaces[v] = ISIZE[row.places[v] &= ~ISHFT[x]];
				col.numPlaces[v] = ISIZE[col.places[v] &= ~ISHFT[y]];
			}
		}

		/**
		 * Remove all of the pinkos from this cells box, row, and col places,
		 * adding any subsequent hidden singles to the singles queue, from
		 * whence {@link AHint#applyImpl} retrieves and sets each cell.
		 *
		 * @param pinkos a bitset of values to be removed
		 * @param singles Queue to which I add any hidden singles I find
		 */
		private void removeMeFromMyRegionsPlacesOfCands(final int pinkos, final Deque<Single> singles) {
			for ( int v : VALUESES[pinkos] ) {
				if ( (box.numPlaces[v]=ISIZE[box.places[v] &= ~ISHFT[b]]) == 1 )
					singles.add(new Single(box.indices[IFIRST[box.places[v]]], v));
				if ( (row.numPlaces[v]=ISIZE[row.places[v] &= ~ISHFT[x]]) == 1 )
					singles.add(new Single(row.indices[IFIRST[row.places[v]]], v));
				if ( (col.numPlaces[v]=ISIZE[col.places[v] &= ~ISHFT[y]]) == 1 )
					singles.add(new Single(col.indices[IFIRST[col.places[v]]], v));
			}
		}

		/**
		 * Remove pinkos from this cells maybes,
		 * updating {@link #count},
		 * {@link Grid#maybes},
		 * {@link Grid#sizes},
		 * {@link Grid#numMaybes},
		 * {@link Grid#idxs},
		 * {@link ARegion#getMaybes},
		 * {@link ARegion#places},
		 * {@link ARegion#numPlaces},
		 * {@link #getMaybes},
		 * {@link #size},
		 * and {@link ARegion#idxs}.
		 *
		 * @param pinkos a bitset of values to be removed
		 * @return the updated size
		 */
		public int removeMaybes(final int pinkos) {
			int v, values[]=VALUESES[pinkos], n=values.length, i=0;
			do {
				v = values[i];
				idxs[v].removeOK(indice);
				// remove me from my regions idxs
				box.idxs[v].remove(indice);
				row.idxs[v].remove(indice);
				col.idxs[v].remove(indice);
				// remove me from my regions places
				box.numPlaces[v] = ISIZE[box.places[v] &= ~ISHFT[b]];
				row.numPlaces[v] = ISIZE[row.places[v] &= ~ISHFT[x]];
				col.numPlaces[v] = ISIZE[col.places[v] &= ~ISHFT[y]];
			} while(++i < n);
			numMaybes -= VSIZE[pinkos];
			Grid.this.maybes[indice] = maybes &= ~pinkos;
			return sizes[indice] = size = VSIZE[maybes];
		}

		/**
		 * Add cands to this cells maybes, updating stuff.
		 *
		 * @see #removeMaybes(int)
		 *
		 * @param cands to be added
		 * @return the updated size
		 */
		private int addMaybes(final int cands) {
			final int added = cands & ~maybes;
			if ( added > 0 ) {
				int v, values[]=VALUESES[added], n=values.length, i=0;
				do {
					v = values[i];
					idxs[v].addOK(indice);
					// add me to my regions idxs
					box.idxs[v].add(indice);
					row.idxs[v].add(indice);
					col.idxs[v].add(indice);
					// add me to my regions places
					box.numPlaces[v] = ISIZE[box.places[v] |= ISHFT[b]];
					row.numPlaces[v] = ISIZE[row.places[v] |= ISHFT[x]];
					col.numPlaces[v] = ISIZE[col.places[v] |= ISHFT[y]];
				} while(++i < n);
				size = VSIZE[maybes |= added];
				numMaybes += VSIZE[added];
				// set the grids maybes
				Grid.this.maybes[indice] = maybes;
				Grid.this.sizes[indice] = size;
			}
			return size;
		}

		/**
		 * Remove all of the existing maybes from this cell.
		 */
		public void clearMaybes() {
			if ( maybes > 0 )
				removeMaybes(maybes);
		}

		/**
		 * Set this cells value to 0 (empty), and fill its maybes.
		 */
		private void clear() {
			value = 0;
			setMaybes(BITS9);
		}

		/**
		 * Just set the cells value minimally in the batch, with no GUI-focused
		 * complications: no formerValue, no autosolve, and no SB; which works
		 * out to be bugger-all faster (sigh), but it is (arguably) simpler.
		 * <p>
		 * If you are reverting a cells value in the GUI then its up to YOU to
		 * call the full set method (below): this one will bugger it up!
		 *
		 * @param value the cells value to be set.
		 * @return 1 - the number of cells set.
		 */
		public int set(final int value) {
			// this set method is a pencil, not a rubber
			assert value > 0;
			if ( value < 1 )
				return 0; // ignore
			// cell.value is a WORM (Write ONCE Read Many)
			if ( this.value > 0 ) {
				// but apply multiple hints can set a cell multiple times
				if ( value == this.value )
					return 0; // ignore
				// cannot change the value of a set cell
				throw new UnsolvableException(id+"="+this.value+" not "+value);
			}
			this.value = value;
			++numSet;
			clearMaybes();
			removeFromMySiblingsMaybes(value);
			decrementMyRegionsEmptyCellCounts();
			updateMyRegionsSetAndUnsetCands(VSHFT[value]);
			return 1;
		}

		/**
		 * Set the value of this cell, and remove that value from the potential
		 * values of each of my sibling cells; and remove this position from
		 * each of my three regions possible positions of my potential values.
		 *
		 * @param value to set.
		 * @param formerValue just pass me the cell.value and dont worry, this
		 *  wont hurt!
		 * @param isAutosolving should subsequent naked and hidden singles be
		 *  sought and set?
		 * @param sb nullable StringBuilder to which (when not null) I append
		 *  an "and therefore list", being any Naked/Hidden Singles thatre set
		 *  as a subsequence of setting the given cell value.
		 *  Currently not-null only in HintsApplicumulator.
		 * @return the number of cells actually set. 1 normally, or 0 if this
		 *  the given value is the same as this cells value.
		 */
		public int set(final int value, final int formerValue
				, final boolean isAutosolving, final StringBuilder sb) {
			if ( value==this.value && size==0 ) {
				if ( maybes > 0 ) { // 9bits
					clearMaybes();
					rebuild();
				}
				return 0;
			}
			this.value = value;
			clearMaybes();
			if ( value == 0 ) {
				// NOTE WELL: cells are cleared only in the GUI. I considered
				// clearing cells in BruteForce and eventually decided against
				// coz its easier to reset EVERYthing by restoring a Backup.
				assert !isAutosolving;
				addMeBackIntoMySiblingsMaybes(formerValue);
				resetMaybes();
				rebuildAllRegionsS__t();
			} else {
				removeFromMySiblingsMaybes(value);
				decrementMyRegionsEmptyCellCounts();
				updateMyRegionsSetAndUnsetCands(VSHFT[value]);
				++numSet;
			}
			// we are done if we are not autosolving
			int count = 1; // 1 for me
			// Autosolve: Seek and set any consequent singles.
			if ( isAutosolving ) {
				Cell sib;
				int j, v, first;
				boolean ok = true; // avoid continue!
				for ( ARegion r : regions ) {
					// look for any subsequent naked singles
					for ( j=0; j<REGION_SIZE; ++j )
						if ( r.cells[j].size == 1 ) {
							if ( (v=VFIRST[(sib=r.cells[j]).maybes]) < 1 ) {
								// Never happens
								Log.teeln("WARN: "+Log.me()+": Nargle Snot!");
								sib.clearMaybes();
								ok = false;
							}
							if ( ok ) {
								if ( sb != null )
									sb.append(CSP).append(sib.id).append(EQUALS).append(v);
								count += sib.set(v, 0, true, sb);
							} else ok = true;
						}
					// look for any subsequent hidden singles
					for ( v=1; v<VALUE_CEILING; ++v )
						if ( r.numPlaces[v] == 1 ) {
							if ( (first=IFIRST[r.places[v]]) < 0 ) {
								// Never happens
								Log.teeln("WARN: "+Log.me()+": Hyena Snot!");
								r.numPlaces[v] = r.places[v] = 0;
								ok = false;
							}
							if ( ok ) {
								if ( (sib=r.cells[first]) != this ) { // oops!
									if ( sb != null )
										sb.append(CSP).append(sib.id).append(EQUALS).append(v);
									count += sib.set(v, 0, true, sb);
								}
							} else
								ok = true;
						}
				}
			}
			return count;
		}

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
			set(value, formerValue, NO_AUTOSOLVE, null);
			return 1;
		}

		/** Called ONLY by {@link Grid#cancelMaybes}. */
		private void cancelMaybes() {
			if ( value > 0 ) {
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
			if ( v == 0 ) // actually happened.
				throw new UnsolvableException(Log.me()+": v=0");
			final int sv = VSHFT[v];
			for ( Cell sib : siblings )
				if ( (sib.maybes & sv) > 0 ) // 9bits
					sib.canNotBeBits(sv);
		}

		/** Decrements the emptyCellCounts of my box, row, and col. */
		public void decrementMyRegionsEmptyCellCounts() {
			--box.emptyCellCount;
			--row.emptyCellCount;
			--col.emptyCellCount;
		}

		/**
		 * Adds sv (shiftedValue) to setCands of my box, row, and col, <br>
		 * and removes sv from unsetCands of my box, row and col.
		 */
		private void updateMyRegionsSetAndUnsetCands(final int sv) {
			box.setCands |= sv;  box.unsetCands &= ~sv;
			row.setCands |= sv;  row.unsetCands &= ~sv;
			col.setCands |= sv;  col.unsetCands &= ~sv;
		}

		/**
		 * Updates the maybes to remove the pinkos from the potential values of
		 * this cell; throwing an UnsolvableException if that leaves this cell
		 * with no potential values, so that BruteForce fails ASAP (no problem,
		 * we just guessed wrong) which is key to BruteForces speed.
		 *
		 * @param pinkos a bitset of the values to be removed
		 * @return the updated size of this cell
		 * @throws UnsolvableException if this cell has no maybes remaining
		 */
		public int canNotBeBits(final int pinkos) {
			final int numRemoved = VSIZE[pinkos & maybes];
			if ( removeMaybes(pinkos) < 1 ) { // returns the new size
				// save time repeatedly constructing exception in Generator
				if ( noMacsRemain == null )
					noMacsRemain = new UnsolvableException("No Macs Remain");
				throw noMacsRemain;
			}
			return numRemoved;
		}

		/**
		 * The canNotBeBits method removes the $pinkos
		 * from the maybes of this cell (me).
		 * <p>
		 * This one handles autosolving. The other one (below) is "normal".
		 * <p>
		 * If singles is not null:<ul>
		 * <li>If this disenpinkenectomy leaves me Naked then I add myself to
		 * singles, from which AHint.apply retrieves me and sets my value.
		 * <li>If this disenpinkenectomy leaves me with maybes==0 then I throw
		 * an UnsolvableException.
		 * </ul>
		 * This method is called only by {@code AHint.apply} with these
		 * <b>PRECONDITIONS:</b><ul>
		 * <li>pinkos > 0, and
		 * <li>this.maybes > 0, and
		 * <li>they intersect: (pinkos & this.maybes) > 0
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
		public int canNotBeBits(int pinkos, final Deque<Single> singles) {
			assert this.maybes > 0;
			assert pinkos > 0;
			pinkos &= maybes;
			assert pinkos > 0;
			final int pre = maybes; // just for the exception message
			// this still required to populate singles!
			removeMeFromMyRegionsPlacesOfCands(pinkos, singles);
			// calculate number of maybes removed BEFORE removing them
			final int numRmvd = VSIZE[pinkos];
			// remove the pinkBits from the maybes of this cell, and
			// switch on the number of maybes now remaining in this cell
			switch ( removeMaybes(pinkos) ) { // returns the new size
			case 0: // I am rooted, so we fail early
				throw new UnsolvableException("No maybes: "+id+":"
					+MAYBES_STR[pre]+" - "+MAYBES_STR[pinkos]);
			case 1: // I am a naked single, to be set by AHint.apply
				singles.add(new Single(indice, VFIRST[maybes]));
			}
			// returns the new size
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
		 * Called only by Cell.set to reset this cells maybes to all of its
		 * potential values when my value is removed (only in the GUI).
		 */
		private void resetMaybes() {
			for ( int v : VALUESES[BITS9 & ~seesValues()] )
				canSoBe(v);
		}

		/**
		 * Add this value back into the maybes of each of my 20 sibling cells,
		 * and also update each siblings regions places[value].
		 * <p>
		 * Only called by Cell.set when clearing a cell in the GUI.
		 *
		 * @param v
		 */
		public void addMeBackIntoMySiblingsMaybes(final int v) {
			if ( v == 0 ) // actually happened
				return;
			final int sv = VSHFT[v];
			for ( final Cell sib : siblings )
				if ( sib.value==0 && (sib.seesValues() & sv)==0 )
					canSoBe(v);
		}

		/**
		 * Returns does this cells maybes contain $value?
		 * <p>
		 * I only exist for convenience. Dont hammer me. It is faster to check
		 * yourself with {@code (cell.maybes & VSHFT[v]) > 0}
		 *
		 * @param value the potential value to test for
		 * @return does this cells potential values contain $v
		 */
		public boolean maybe(final int value) {
			return (maybes & VSHFT[value]) > 0;
		}

		/**
		 * Returns a bitset of the values held by my sibling cells.
		 *
		 * @return a bitset of the values held by my sibling cells.
		 */
		public int seesValues() {
			int result = 0;
			for ( Cell sib : siblings )
				result |= VSHFT[sib.value];
			return result;
		}

		/**
		 * Return the instance of Grid that contains this region.
		 *
		 * @return the grid that contains this region.
		 */
		public Grid getGrid() {
			return Grid.this;
		}

		/**
		 * Returns a String representation of this cell: A1=5 or A2:3{368}.
		 * Used only for debugging: ie is NOT part of the GUI application or
		 * used in LogicalSolverTester.
		 * <p>
		 * NOTE: toString deals with being called while construction is still
		 * underway by relying on the $id field being the first to be set, and
		 * the $buds field being the last to be set, such that all you see is
		 * my $id until the constructor completes; then you see the full Cell
		 * definition: "id:size{maybes}" or "id=value" if set.
		 * <p>
		 * NOTE: toString IS used in the application; it is not, like most
		 * toString methods, only used for debugging.
		 *
		 * @return "${id}:${size}{Values.toString(maybes)}" <br>
		 *  or "${id}=${value}" if this Cell is set; <br>
		 *  or simply "${id}" while constructing.
		 */
		@Override
		public String toString() {
			// setting id is the first Constructor instruction
			// setting buds is that last Constructor instruction
			return buds!=null ? toFullString() : id;
		}

		/**
		 * Returns a String representation of this cell: A1=5 or A2:3{368}.<br>
		 * Note that toFullString explicitly gets the full string. Currently
		 * toString returns the same, but dont rely on that not changing when
		 * you really want the full string, so you call me explicitly.
		 *
		 * @return "${id}:${size}{Values.toString(maybes)}" <br>
		 *  or "${id}=${value}" if this Cell is set.
		 */
		public String toFullString() {
			return value > 0
				 ? id+EQUALS+value
				 : id+":"+size+"{"+MAYBES_STR[maybes]+"}";
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
				&& indice == ((Cell)o).indice; // y<<4^x (identity only)
		}
		public boolean equals(Cell o) {
			return indice == o.indice;
		}

		/**
		 * Returns the hashCode of this cell, which uniquely identifying this
		 * Cell within its Grid.
		 *
		 * @return indice
		 */
		@Override
		public int hashCode() {
			return indice;
		}

		/**
		 * Compare this cell to other by comparing the indices.
		 *
		 * @param o the Cell to compare me to
		 * @return -1 if this is before other, 1 if this is after other, or 0
		 *  if this equal to other, which will happen only if these are the
		 *  same cell, just possibly from different grids.
		 */
		@Override
		public int compareTo(final Cell o) {
			return Integer.compare(indice, o.indice);
		}

	}

	// ============================== The Regions =============================

	/**
	 * ARegion is an abstract region (a Box, a Row, or a Col)
	 * that contains 9 cells in this Grid.
	 */
	public abstract class ARegion {

		/**
		 * My grid.regions array index 0..26.
		 */
		public final int index;

		/**
		 * RegionTypeIndex: 0=Grid.BOX, 1=Grid.ROW, 2=Grid.COL.
		 */
		public final int rti;

		/**
		 * label (aka rid) is a String that uniquely identifies this region in
		 * the grid.
		 * <p>
		 * In {@link IdScheme#Chess} thats "${region.typeName} ${identifier}"
		 * where the identifier depends on the region-type, for Box and Row
		 * its a number 1..9, but for Col its a letter A..I. <br>
		 * Examples: "box 6", "row 2", or "col I".
		 */
		public final String label;

		/**
		 * The cells of this region left-to-right and top-to-bottom.
		 */
		public final Cell[] cells = new Cell[REGION_SIZE];

		/**
		 * A bitset of indexes in region.cells which maybe value 1..9.
		 */
		public final int[] places = new int[VALUE_CEILING];

		/**
		 * The number of region.cells which maybe value 1..9.<br>
		 * For example: {@code if (region.numPlaces[v] == 2)} finds regions
		 * having two places for the candidate value v.
		 */
		public final int[] numPlaces = new int[VALUE_CEILING];

		/**
		 * The grid.cells indices of cells in this region which maybe each
		 * potential value 1..9. (not to be confused with {@link #places}
		 * that contains indexes in the ARegion.cells array).
		 * <p>
		 * SE NOMENCLATURE: the word indice always means a grid.cells indice;
		 * everything else is an index (or some other not-indice word). An idx
		 * always contains indices, everything else is an Index (or some other
		 * non-idx word).
		 */
		public final Idx[] idxs = new Idx[VALUE_CEILING];

		/**
		 * An immutable Idx of the indices of cells in this region.
		 */
		public final IdxI idx;

		/**
		 * The number of cells with value==0 calculated by
		 * {@link #emptyCellCount()}, which is called by
		 * {@link Grid#rebuildAllRegionsEmptyCellCounts}.
		 */
		public int emptyCellCount = REGION_SIZE;

		/**
		 * A Values bitset of the cell.values that are set in this region. This
		 * value is initialised by {@link #rebuildAllRegionsSetCands() }, and
		 * maintained by {@link Cell#updateMyRegionsSetAndUnsetCands(int) },
		 * for the {@link #hasHomelessValues} validator, at start of solve and
		 * after each hint is applied; so that hinters can just use the bitset.
		 * <p>
		 * I need set/unsetCands in the hinters. I care not about rebuild speed
		 * because it runs rarely. However, I do care about copyGrid speed, and
		 * it is merely "fast enough". I require a rich grid, so copyGrid takes
		 * time. Live with it.
		 * <p>
		 * setCands and {@link #unsetCands} are complementary so this inviolate
		 * always true: {@code assert unsetCands | setCands == Values.BITS9;}.
		 */
		public int setCands;

		/**
		 * A Values bitset of the values that are yet to be set in this region,
		 * that is all values except setCands.
		 * <p>
		 * {@link setCands} and unsetCands are complementary so this inviolate
		 * always true: {@code assert unsetCands | setCands == Values.BITS9;}.
		 */
		public int unsetCands = Values.BITS9;

		/**
		 * For Row and Col intersectors contains the 3 intersecting boxs; <br>
		 * for Box intersectors contains the 6 intersecting Rows and Cols.
		 * nb: these 3 intersecting cells are called a "Slot".
		 */
		public final ARegion[] intersectors;

		/**
		 * The Indice of each cell in this region.
		 */
		public final int[] indices;

		/**
		 * The Constructor. I am a Box, or a Row, or a Col.
		 *
		 * @param index the index of this region in the Grid.regions array.
		 * @param regionTypeIndex BOX=0, ROW=1, or COL=2
		 * @param numCrosses the number of crossingBoxes: 0 for each Box,
		 *  3 for each Row and Col. Passed-in for crossingBoxes to be final.
		 * @param numIntersectors the number of intersectors: 6 for each Box,
		 *  3 for each Row and Col. Passed-in for intersectors to be final.
		 */
		protected ARegion(final int index, final int regionTypeIndex
				, final int numCrosses, final int numIntersectors) {
			this.index = index;
			this.rti = regionTypeIndex;
			this.label = REGION_LABELS[index];
			// get my idx: an IdxI (immutable) of cells in this region
			idx = REGION_IDXS[index];
			// create the Idxs (0 remains null).
			for ( int v=1; v<VALUE_CEILING; ++v )
				idxs[v] = new Idx(REGION_IDXS[index]);
			intersectors = new ARegion[numIntersectors];
			indices = REGION_INDICES[index];
		}

		// used by Grid#copyFrom
		// NOTE: Just to be clear, ALL mutable fields are copied; if you add
		// a new mutable field to ARegion/Box/Row/Col copy it here. The copy
		// constructor calls me. If you forget and you use this field in a
		// hinter then BruteForce and/or Generator will fail.
		private void copyFrom(final ARegion src) {
			// copy places and numPlaces of all values 1..9
			arraycopy(src.places, 1, places, 1, REGION_SIZE);
			arraycopy(src.numPlaces, 1, numPlaces, 1, REGION_SIZE);
			// deep copy the idxs of all values 1..9
			for ( int v=1; v<VALUE_CEILING; ++v )
				idxs[v].set(src.idxs[v]);
			emptyCellCount = src.emptyCellCount;
			setCands = src.setCands;
			unsetCands = src.unsetCands;
		}

		/**
		 * Set numPlaces[v] = places[v] = 0, ie clear places of v.
		 *
		 * @param v
		 */
		public void clearPlaces(int v) {
			numPlaces[v] = places[v] = 0;
		}

		/**
		 * Get the index of the given cell in this region.cells array.
		 * <p>The {@code cell} must be in this region (checked with assert).
		 * Returns the cells b, y, or x index: whichever is appropriate for
		 * this type of ARegion. So you CANNOT use indexOf as a contains test,
		 * as you might (reasonably) expect.
		 * <p>
		 * If this behaviour upsets you then fix it yourself (check the given
		 * cells b/x/y against my b/x/y) to return -1, which throws an AIOBE)
		 * I just prefer speed to correctness upon which I do not rely.
		 *
		 * @param cell the cell whose index you seek.
		 * @return the index of the cell in this regions cells array.
		 */
		public abstract int indexOf(final Cell cell);

		/**
		 * Does this region contain this Cell? Fast O(1).
		 *
		 * @param cell Cell to look for.
		 * @return true if this region contains this cell, else false.
		 */
		public abstract boolean contains(final Cell cell);

		/**
		 * Count the number of empty (value==0) cells in this region, which is
		 * recorded in the {@link #emptyCellCount} field for public reference.
		 * <p>
		 * emptyCellCount() called by {@link #rebuildAllRegionsEmptyCellCounts}
		 * ONCE pre-solve, so that hinters can just reference the field.
		 *
		 * @return number of empty cells in this region, but my reason to exist
		 *  is I set {@link #emptyCellCount} for public reference.
		 */
		public int emptyCellCount() {
			int cnt = 0;
			for ( int i=0; i<REGION_SIZE; ++i )
				if ( cells[i].value == 0 )
					++cnt;
			return emptyCellCount = cnt;
		}

		/**
		 * Query invokes action on each region cell where predicate.
		 * <p>
		 * Invoke {@code action}
		 * foreach {@code Cell}
		 * in this {@code region.cells}
		 * where {@code predicate} is true.
		 * <p>
		 * Used in {@link diuf.sudoku.solver.hinters.nkdset.NakedSet#findHints(Grid grid, IAccumulator accu) } <br>
		 *
		 * @param predicate returns true when action is required
		 * @param visitor takes action when predicate is true
		 * @return number of times predicate was true (ie num actions taken)
		 */
		public int query(final Predicate<Cell> predicate
				, final IVisitor2<Cell, Integer> visitor) {
			int count = 0;
			for ( final Cell cell : cells )
				if ( predicate.test(cell) )
					visitor.visit(cell, count++);
			return count;
		}

		/**
		 * Repopulates $result with indices of cells in this region which maybe
		 * any of the given maybes (a bitset).
		 *
		 * @param cands a bitset of the maybes you seek
		 * @param result the result Idx
		 * @return the result Idx of cells in this region which maybe maybes
		 */
		public Idx idxOf(final int cands, final Idx result) {
			long m0=0; int m1=0;
			for ( int value : VALUESES[cands] ) {
				m0 |= idxs[value].m0;
				m1 |= idxs[value].m1;
			}
			result.m0 = m0;
			result.m1 = m1;
			return result;
		}

		/**
		 * Populates result with the cells at indexes in this region. Result is
		 * presumed to be large enough. This method is fast-enough for use in a
		 * loop with a fixed-size array.
		 *
		 * @param indexes a bitset of indexes into region.cells
		 * @param result Cell[] the cells array to populate
		 * @return the number of cells added
		 */
		public int at(final int indexes, final Cell[] result) {
			int cnt = 0;
			for ( int i : INDEXES[indexes] )
				result[cnt++] = cells[i];
			return cnt;
		}

		/**
		 * Return a new array of the cells at the given index $bits in this
		 * region.
		 *
		 * @param indexes {@code int} a bitset of the indexes to get, where the
		 * position (from the right) of each set (1) bit denotes the index in
		 * this.cells array of a Cell to retrieve.
		 * @return a new {@code Cell[]}.
		 */
		public Cell[] atNew(final int indexes) {
			final int n = ISIZE[indexes];
			final Cell[] result = new Cell[n];
			at(indexes, result);
			return result;
		}

		/** @return id - so just access the id directly, it is faster. */
		@Override
		public String toString() {
			return label;
		}

		/**
		 * Returns the first empty cell in this region.
		 *
		 * @return the first cell in this region with value == 0
		 */
		public Cell firstEmptyCell() {
			for ( Cell c : cells )
				if ( c.value == 0 )
					return c;
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
		 * Rebuild the {@link #places} of each value 1..9 with the index
		 * in this {@link #cells} of each Cell which maybe v.
		 */
		private void rebuildPlacesOfAllValues() {
			int i, bits, sv, v;
			for ( v=1; v<VALUE_CEILING; ++v ) {
				sv = VSHFT[v];
				if ( (setCands & sv) > 0 ) // 9bits
					numPlaces[v] = places[v] = 0;
				else { // v is not set, so it's still a maybe
					for ( i=bits=0; i<REGION_SIZE; ++i )
						if ( (cells[i].maybes & sv) > 0 )
							bits |= ISHFT[i];
					numPlaces[v] = ISIZE[places[v]=bits];
				}
			}
		}

		/**
		 * Returns the index as my hashCode.
		 * <p>
		 * LockingSpeedMode uses ARegion as a hash key.
		 *
		 * @return index as my hashCode
		 */
		@Override
		public int hashCode() {
			return index;
		}

		/**
		 * Returns is the given Object an ARegion, with the same index.
		 *
		 * @param obj
		 * @return is obj ARegion with same index
		 */
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof ARegion)
				&& index == ((ARegion)obj).index;
		}

	}

	/**
	 * A box (a 3-by-3 block of cells) in this Sudoku Grid.
	 */
	public final class Box extends ARegion {

		/** Vertical index of this box: 0=top, 1=middle, 2=bottom */
		public final int vNum;

		/** Top y of this box: 0=top, 3=middle, 6=bottom */
		public final int top;

		/** Horizontal index of this box: 0=left, 1=middle, 2=right */
		public final int hNum;

		/** Left x of this box: 0=left, 3=middle, 6=right */
		public final int left;

		/**
		 * Construct a new Box in this Grid.
		 * <p>
		 * When we process all regions its boxs first because humans find it
		 * easier to find stuff in a Box verses a Row or Col, I think because
		 * the brain finds it easier to search three threes, verses nine. Or
		 * it may be that our vision renders a spot, not a strip, which is, I
		 * think, the square-root of 42.
		 * <p>
		 * Atleast I avoided all the Box jokes, for now. Pick a box Dave?
		 *
		 * @param index of this Box (0..8) in the Grid.boxs array, which is (by
		 *  design) coincident with the first 9 elements in Grid.regions.
		 */
		private Box(final int index) {
			super(index, BOX, 0, R*2);
			this.hNum = index%R;
			this.left = hNum*R;
			this.vNum = index/R;
			this.top = vNum*R;
//			this.boxIndex = top + hNum;
			final Box me = this;
			final Cell[] gcs = Grid.this.cells;
			for ( int i=0; i<N; ++i )
				// nb: it sets cells[i] first, then its attributes
				// nb: Yes, that gcs maths is a bitch too!
				(cells[i]=gcs[((top+i/R)*N)+(left+i%R)]).regions[BOX] = cells[i].box = me;
		}

		// Boxs are created BEFORE Rows and Cols, so delayed til all exist.
		private void initIntersectors() {
			intersectors[0] = rows[top];
			intersectors[1] = rows[top + 1];
			intersectors[2] = rows[top + 2];
			intersectors[3] = cols[left];
			intersectors[4] = cols[left + 1];
			intersectors[5] = cols[left + 2];
		}

		/**
		 * Return the index of the given cell in this Regions cells array.
		 * This region MUST contain the given cell, checked with an assert.
		 *
		 * @return cell.b (for box)
		 */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.b; // cell.y%3*3 + cell.x%3;
		}

		/**
		 * @return Does this Box contain the given cell?
		 */
		@Override
		public boolean contains(final Cell cell) {
			return cell.box == this;
		}
	}

	/**
	 * A row in this Sudoku Grid.
	 */
	public final class Row extends ARegion {

		/** The index of this Row: 0=top, 8=bottom. */
		public final int y;

		/** The vertical index of the 3 Boxs which I cross. */
		public final int vNum;

		/**
		 * Constructs a new Row in this Grid.
		 *
		 * @param y int 0..8 the row (vertical) index
		 */
		private Row(final int y) {
			super(N+y, ROW, R, R);
			this.y = y;
			vNum = y / R;
			final Cell[] gcs = Grid.this.cells; // shorthand
			final int floor = BY9[y]; //y*N; // indice of first cell in this Row
			final Row me = this; // defeat IDE warn leaking this in constr
			for ( int i=0; i<N; ++i )
				(cells[i]=gcs[floor+i]).regions[ROW] = cells[i].row = me;
			final int left = y/R*R;
			intersectors[0] = boxs[left];
			intersectors[1] = boxs[left+1];
			intersectors[2] = boxs[left+2];
		}

		/**
		 * Returns the index of the given Cell in this regions cells array.
		 * This region MUST contain the given cell.
		 *
		 * @param cell Cell to get the index of.
		 * @return the x (horizontal index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert.
		 */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.x;
		}

		/**
		 * @return Does this Row contain the given Cell?
		 */
		@Override
		public boolean contains(final Cell cell) {
			return cell.y == this.y;
		}
	}

	/** A column in this Sudoku Grid */
	public final class Col extends ARegion {

		/** The index of this Col: 0=leftmost, 8=rightmost */
		public final int x;

		/**
		 * Constructs a new Col in this Grid.
		 *
		 * @param x int 0..8 the col (horizontal) index
		 */
		private Col(final int x) {
			super(FIRST_COL+x, COL, R, R);
			this.x = x;
			final int hNum = x / R;
			final Cell[] gcs = Grid.this.cells; // shorthand
			final Col me = this; // defeat IDE warn leaking this in constr
			for ( int i=0; i<N; ++i )
				(cells[i]=gcs[i*N+x]).regions[COL] = cells[i].col = me;
			intersectors[0] = boxs[hNum];
			intersectors[1] = boxs[hNum+3];
			intersectors[2] = boxs[hNum+6];
		}

		/**
		 * Returns the index of the given Cell in this regions cells array.
		 * This region MUST contain the given cell.
		 *
		 * @return the y (vertical index) of the given Cell, we check that
		 * {@code thisRegion.contains(cell)} with an assert.
		 */
		@Override
		public int indexOf(final Cell cell) {
			assert contains(cell);
			return cell.y;
		}

		/**
		 * @return Does this Col contain the given Cell?
		 */
		@Override
		public boolean contains(final Cell cell) {
			return cell.x == this.x;
		}
	}

	/**
	 * A Naked or Hidden Single element in a Deque.
	 *
	 * @author Keith Corlett 2021-06-18
	 */
	public static class Single {
		public final int indice;
		public final int value;
		public Single(final int indice, final int value) {
			this.indice = indice;
			this.value = value;
		}
		@Override
		public String toString() {
			return CELL_IDS[indice]+"="+value;
		}
	}

}
