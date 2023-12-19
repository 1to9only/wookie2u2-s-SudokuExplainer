/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Constants.SB;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.RIBS;
import static diuf.sudoku.Grid.REGION_LABELS;
import static diuf.sudoku.Grid.REGION_TYPE_NAMES;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Grid.regionIndex;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.Values.BITS9;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Frmu;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Regions class contains static helper methods for Grid.ARegion. There
 * are so many of them I thought it was worth gathering them together into a
 * class, mainly to keep them out of the Grid class, which is already to big.
 *
 * @author Keith Corlett 2020-10-14
 */
public final class Regions {

	public static final ARegion[] EMPTY_ARRAY = new ARegion[0];

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion region) {
		final ArrayList<ARegion> regions = new ArrayList<>(1);
		regions.add(region);
		return regions;
	}

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2) {
		final ArrayList<ARegion> regions = new ArrayList<>(2);
		regions.add(r1);
		regions.add(r2);
		return regions;
	}

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3) {
		final ArrayList<ARegion> regions = new ArrayList<>(3);
		regions.add(r1);
		regions.add(r2);
		regions.add(r3);
		return regions;
	}

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4) {
		final ArrayList<ARegion> result = new ArrayList<>(4);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		return result;
	}

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4, final ARegion r5) {
		final ArrayList<ARegion> result = new ArrayList<>(5);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		result.add(r5);
		return result;
	}

	// faster to not do a varargs call
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4, final ARegion r5, final ARegion... others) {
		final ArrayList<ARegion> result = new ArrayList<>(5+others.length);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		result.add(r5);
		for ( ARegion r : others )
			result.add(r);
		return result;
	}

	// a new java.util.ArrayList<ARegion> of an existing array, not the crap
	// local-ArrayList returned by Arrays.asList, which is not comparable.
	public static ArrayList<ARegion> list(final ARegion[] regions) {
		return list(regions, new ArrayList<>(regions.length));
	}

	public static ArrayList<ARegion> list(final ARegion[] regions, final ArrayList<ARegion> result) {
		for ( ARegion r : regions )
			result.add(r);
		return result;
	}

	public static ArrayList<ARegion> list(final int size, final ARegion[] regions, final int indexes) {
		return list(size, regions, INDEXES[indexes]);
	}
	public static ArrayList<ARegion> list(final int size, final ARegion[] regions, final int[] indexes) {
		return list(size, regions, indexes, new ArrayList<>(size));
	}
	public static ArrayList<ARegion> list(final int size, final ARegion[] regions, final int[] indexes, final ArrayList<ARegion> result) {
		for ( int i : indexes )
			result.add(regions[i]);
		return result;
	}

	public static ArrayList<ARegion> clear(final ArrayList<ARegion> c) {
		c.clear();
		return c;
	}

	public static List<ARegion> clear(final List<ARegion> c) {
		c.clear();
		return c;
	}

	// nb: these array methods make it easier to swap to/from Lists. If there
	// is no doubt that you are always going to use an array then create the
	// array natively, and save yourself the method invocation.
	public static ARegion[] array(final ARegion region) {
		return new ARegion[] { region };
	}
	public static ARegion[] array(final ARegion r1, final ARegion r2) {
		return new ARegion[] { r1, r2 };
	}
	public static ARegion[] array(final ARegion r1, final ARegion r2, final ARegion r3) {
		return new ARegion[] { r1, r2, r3 };
	}
	public static ARegion[] array(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4) {
		return new ARegion[] { r1, r2, r3, r4 };
	}
	public static ARegion[] array(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4, final ARegion r5) {
		return new ARegion[] { r1, r2, r3, r4, r5 };
	}

	/**
	 * Repopulate the given array with $indexes regions, and return how many.
	 *
	 * @param regions to select from
	 * @param indexes bitset of indexes in regions
	 * @param result to repopulate
	 * @return the number of regions added to array.
	 */
	public static int select(final ARegion[] regions, final int indexes, final ARegion[] result) {
		int count = 0;
		for ( int i=0; i<REGION_SIZE; ++i )
			if ( (indexes & ISHFT[i]) > 0 ) // 27bits
				result[count++] = regions[i];
		return count;
	}

	public static String typeName(final ARegion[] regions) {
		if ( regions==null || regions.length==0 || regions[0]==null )
			return "";
		return REGION_TYPE_NAMES[regions[0].rti];
	}

	public static int typeMask(final ARegion[] regions) {
		int mask = 0;
		for ( ARegion r : regions )
			switch (r.rti) {
			case BOX: mask |= 1; break;
			case ROW: mask |= 2; break;
			default: mask |= 4; break;
			}
		return mask;
	}

	private static final String[] TYPE_NAMES = {
		  "no_region", "box", "row", "box/row", "col"
		, "box/col", "row/col", "box/row/col"
	};

	public static String typeNames(final ARegion[] regions) {
		return TYPE_NAMES[typeMask(regions)];
	}

	public static String ids(final List<ARegion> regions) {
		final StringBuilder sb = SB(regions.size() * 6);
		for ( ARegion r : regions )
			sb.append(' ').append(r.label);
		return sb.toString();
	}

	// returns CSV of the finned region/s. Mostly there is one fin. Multiple
	// fins are almost always in the one base. Just occassionally: BUGGER!
	// so for the testcases these MUST be consistent order, so I have to put
	// them in a set for uniqueness, then put the set into a list, and sort
	// the damn list by id. Any order will do, it just MUST be consistent and
	// by id also works for humans, who do not want to have to think about
	// pernickity s__t like this, and then the Frmt.csv method I wrote which
	// takes a Set instead of a List is now not used, but retained coz I am an
	// ornery bastard, who put in the work, and I might use it one day.
	// Programming is complex. sigh.
	public static String finned(final Grid grid, final ARegion[] bases, final Set<Integer> fins) {
		// unique regions containing fins
		final HashSet<ARegion> set = new HashSet<>();
		final Cell[] cells = grid.cells;
		for ( int fin : fins )
			for ( ARegion r : cells[fin].regions )
				if ( in(bases, r) )
					set.add(r);
		// sort by index: box, row, col
		final List<ARegion> list = new ArrayList<>(set);
		list.sort((a, b) -> a.index - b.index);
		// Guinness, for strength!
		return Frmt.frmt(list, (r)->r.label, CSP, AND);
	}

	public static int count(final boolean[] unitsUsed) {
		int count = 0;
		for ( int i=0,n=unitsUsed.length; i<n; ++i )
			if ( unitsUsed[i] )
				++count;
		return count;
	}

	// get a List of the used regions from a used-regions array
	public static ARegion[] used(final boolean[] unitsUsed, final Grid grid) {
		final int n = count(unitsUsed);
		return used(unitsUsed, new ARegion[n], n, grid);
	}

	// get a List of the used regions from a used-regions array
	public static ARegion[] used(final boolean[] unitsUsed, final ARegion[] result, final int n, final Grid grid) {
		final ARegion[] regions = grid.regions;
		for ( int cnt=0,i=0; cnt<n && i<unitsUsed.length; ++i )
			if ( unitsUsed[i] )
				result[cnt++] = regions[i];
		return result;
	}

	public static ARegion[] used(final int unitsBits, final Grid grid) {
		final ARegion[] result = new ARegion[Integer.bitCount(unitsBits)];
		final ARegion[] source = grid.regions;
		for ( int cnt=0,i=0,bits; i<NUM_REGIONS; i+=REGION_SIZE )
			if ( (bits=unitsBits>>i & BITS9) > 0 )
				for ( int j : INDEXES[bits] )
					result[cnt++] = source[i+j];
		return result;
	}

	// for debugging: "row 1, row 3, row 6" from a "used" boolean-array (slow)
	public static String usedString(final boolean[] unitsUsed) {
		final StringBuilder sb = SB(32);
		boolean first = true;
		for ( int i=0,n=unitsUsed.length; i<n; ++i ) {
			if ( unitsUsed[i] ) {
				if ( first )
					first = false;
				else
					sb.append(", ");
				sb.append(REGION_LABELS[i]);
			}
		}
		return sb.toString();
	}

	// for debugging: "row 1, row 3, row 6" from bases/coversUsedBits (faster)
	public static String usedString(final int usedBits) {
		final StringBuilder sb = SB(32);
		boolean first = true;
		for ( int i=0; i<NUM_REGIONS; i+=REGION_SIZE ) // boxs, rows, cols
			for ( int j : INDEXES[(usedBits>>i) & BITS9] ) { // 9bits
				if ( first )
					first = false;
				else
					sb.append(", ");
				sb.append(REGION_LABELS[i+j]);
			}
		return sb.toString();
	}

	public static int parseUsedBits(final String usedRegions) {
		return parseUsedBits(usedRegions, ", ?");
	}

	// for debugging:
	// To breakpoint in a tight-loop:
	// final int baseSTOP == Regions.parseUsedBits("row 1, row 3, row 6"); // ONCE
	// .... then down in your tight-loop ....
	//          if ( basesBits == baseSTOP )
	//              Debug.breakpoint();
	public static int parseUsedBits(final String usedRegions, final String sep) {
		int result = 0; // usedBits
		final String[] fields = usedRegions.split(sep);
		String f;
		for ( int i=0,n=fields.length,j; i<n; ++i )
			switch ( (f=fields[i]).charAt(0) ) {
			case 'b': result |= IDX_SHFT[f.charAt(4)-'1']; break;
			case 'r': result |= IDX_SHFT[9+(f.charAt(4)-'1')]; break;
			case 'c': result |= IDX_SHFT[18+(f.charAt(4)-'A')]; break;
			}
		return result;
	}

	// only used for debugging
	// parse "row 1, row 3, row 6" into a "used" boolean array
	public static boolean[] parseUsed(final String used) {
		final boolean[] result = new boolean[NUM_REGIONS];
		for ( String rid : used.split(", ?") )
			result[regionIndex(rid)] = true;
		return result;
	}

	/**
	 * Is the given Cell in any of these regions?
	 *
	 * @param regions
	 * @param cell
	 * @return
	 */
	public static boolean contains(final ARegion[] regions, final Cell cell) {
		if ( regions == null )
			return false;
		for ( ARegion region : regions )
			if ( region.contains(cell) ) // fast O(1)
				return true;
		return false;
	}

	/**
	 * Is target in regions? Using reference-equals.
	 *
	 * @param field to search
	 * @param target to seek
	 * @return is target in regions
	 */
	public static boolean in(final Object[] field, final Object target) {
		if ( field == null )
			return false;
		for ( Object region : field )
			if ( region == target ) // reference equals intended
				return true;
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ types ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// a left-shifted mask for each of the 3 types of regions
	public static final int BOX_MASK = 0x1;
	public static final int ROW_MASK = 0x2;
	public static final int COL_MASK = 0x4;
	// both ROW and COL in the one mask
	public static final int ROW_COL_MASK = ROW_MASK | COL_MASK;
	// both BOX and ROW in the one mask
	public static final int ROW_BOX_MASK = ROW_MASK | BOX_MASK;
	// both BOX and COL in the one mask
	public static final int COL_BOX_MASK = COL_MASK | BOX_MASK;
	// an array of the mask of each region type to look-up
	public static final int[] REGION_TYPE_MASK = {BOX_MASK, ROW_MASK, COL_MASK};

	// an array of the type of each of the 27 regions to look-up
	// NB: SE is (box, row, col) whereas HoDoKu is (row, col, box)
	public static final int[] REGION_TYPE = {
		  BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX
		, ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW
		, COL, COL, COL, COL, COL, COL, COL, COL, COL
	};

	// Returns a mask containing the types of the used regions
	public static int types(final boolean[] used) {
		int mask = 0;
		for ( int i=0; i<NUM_REGIONS; ++i )
			if ( used[i] )
				mask |= REGION_TYPE_MASK[REGION_TYPE[i]];
		return mask;
	}

	// Returns a mask containing the types of these regions
	public static int types(final ARegion[] regions) {
		int mask = 0;
		for ( ARegion r : regions )
			mask |= REGION_TYPE_MASK[r.rti];
		return mask;
	}

	public static String toString(ARegion[] regions, int n) {
		final StringBuilder sb = SB(n<<3); // * 8
		sb.append(regions[0].label);
		for ( int i=1; i<n; ++i )
			sb.append(CSP).append(regions[i].label);
		return sb.toString();
	}

	//DEBUG
	public static String toString(final int[] regionIndexes, final int n) {
		final StringBuilder sb = SB(n<<3); // * 8
		sb.append(REGION_LABELS[regionIndexes[0]]);
		for ( int i=1; i<n; ++i )
			sb.append(CSP).append(REGION_LABELS[regionIndexes[i]]);
		return sb.toString();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ common ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns the first common row, col, or box of two cells; else null.
	 *
	 * @param a Cell
	 * @param b Cell
	 * @return ARegion
	 */
	public static ARegion common(final Cell a, final Cell b) {
		if ( a.row == b.row )
			return a.row;
		if ( a.col == b.col )
			return a.col;
		if ( a.box == b.box )
			return a.box;
		return null;
	}

	// returns a bitset of indexes of regions common to cells
	public static int common(final Cell[] cells) {
		return common(cells, cells.length);
	}

	// returns a bitset of indexes of regions common to cells
	public static int common(final Cell[] cells, final int n) {
		int cribs = RIBS[cells[0].indice]; // Regions Indexes BitS
		for ( int i=1; i<n; ++i )
			cribs &= RIBS[cells[i].indice];
		return cribs;
	}

	/**
	 * Returns the other ARegion that is common to all cells, else null.
	 *
	 * @param idx indices of the cells to find the other common region of. <br>
	 *  Two or three cells can have one "other" common region. <br>
	 *  Less than two cells always returns null coz "common" is nonsensical. <br>
	 *  Four-or-more cells always returns null because four cells can have only
	 *  one common region, so an "other" common region cannot exist.
	 * @param r the region that these cells were found in (and you want there
	 *  other common region, if any). Note that the "other" common region
	 *  returned comes from the same Grid that contains the given region.
	 * @return the other ARegion common to all cells, else null meaning none.
	 */
	public static ARegion otherCommonRegion(final Idx idx, final ARegion r) {
		final int n, cribs; // Common Regions Indexes BitSet, pun intended.
		// 2 or 3 cells can share two regions, 4 cannot. 1 always has three.
		if ( (n=idx.size())>1 && n<4
		  // if these cells share two common regions
		  && Integer.bitCount(cribs=idx.commonRibs()) == 2 )
			// return the other one (work this s__t out Scoobie!)
			// head____: Long.nOTZ coz IDX_SHFT is long[] but contains 32bits
			return r.getGrid().regions[Long.numberOfTrailingZeros(cribs & ~IDX_SHFT[r.index])];
		return null;
	}

	/**
	 * Read the RIBS (Region Index BitSet) regions from this grid.
	 *
	 * @param gridRegions grid.regions of the grid to search
	 * @param ribs Region Index BitSet from nakedSet.commonRibs()
	 * @return a new array of RIBS regions
	 */
	public static ARegion[] array(final ARegion[] gridRegions, final int ribs) {
		final int n = Integer.bitCount(ribs);
		final ARegion[] result = new ARegion[n];
		for ( int cnt=0,i=0; cnt<n && i<27; ++i )
			if ( (ribs & 1<<i) > 0 ) // 27bits
				result[cnt++] = gridRegions[i];
		return result;
	}

	private static final int BOX_OFFSET = -1;
	private static final int ROW_OFFSET = 9 - 1;
	private static final int COL_OFFSET = 16 - 'A';

	public static int usedBits(final String usedRegionsString) {
		final String[] labels = usedRegionsString.split(" *, *");
		int bits = 0;
		for ( String label : labels ) {
			if ( label.length() < 5 )
				throw new IllegalArgumentException("short: '"+label+"'");
			char c = label.charAt(4);
			switch ( label.charAt(0) ) {
				case 'b': bits |= 1<<(BOX_OFFSET + c); break;
				case 'r': bits |= 1<<(ROW_OFFSET + c); break;
				case 'c': bits |= 1<<(COL_OFFSET + c); break;
				default: throw new IllegalArgumentException("bad: '"+label+"'");
			}
		}
		return bits;
	}

	public static int usedBits(final boolean[] unitsUsed) {
		int result = 0;
		for ( int i=0; i<unitsUsed.length; ++i )
			if ( unitsUsed[i] )
				result |= IDX_SHFT[i];
		return result;
	}

	private Regions() {} // never used

}
