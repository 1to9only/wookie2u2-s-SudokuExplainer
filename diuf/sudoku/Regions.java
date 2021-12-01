/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.utils.Frmu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.NoSuchElementException;
import static diuf.sudoku.utils.Frmt.CSP;


/**
 * The Regions class contains static helper methods for Grid.ARegion's. There
 * are so many of them I thought it was worth gathering them together into a
 * class, mainly to keep them out of the Grid class, which is already to big.
 *
 * @author Keith Corlett 2020-10-14
 */
public final class Regions {

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion region) {
		ArrayList<ARegion> regions = new ArrayList<>(1);
		regions.add(region);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2) {
		ArrayList<ARegion> regions = new ArrayList<>(2);
		regions.add(r1);
		regions.add(r2);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3) {
		ArrayList<ARegion> regions = new ArrayList<>(3);
		regions.add(r1);
		regions.add(r2);
		regions.add(r3);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4) {
		ArrayList<ARegion> result = new ArrayList<>(4);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		return result;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4, ARegion r5) {
		ArrayList<ARegion> result = new ArrayList<>(5);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		result.add(r5);
		return result;
	}

	// I give up and do a varargs call, which is a bit slower.
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4, ARegion r5, ARegion... others) {
		ArrayList<ARegion> result = new ArrayList<>(5+others.length);
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
	// local-ArrayList returned by Arrays.asList, which isn't comparable.
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

	public static ArrayList<ARegion> clear(ArrayList<ARegion> c) {
		c.clear();
		return c;
	}

	public static List<ARegion> clear(List<ARegion> c) {
		c.clear();
		return c;
	}

	/**
	 * Repopulate the given array with 'bits' regions.
	 *
	 * @param regions to select from
	 * @param bits bitset of indexes in regions
	 * @param array to repopulate
	 * @return the number of regions added to array.
	 */
	public static int select(ARegion[] regions, int bits, final ARegion[] array) {
		int n = 0;
		for ( int i=0; i<REGION_SIZE; ++i )
			if ( (bits & ISHFT[i]) != 0 )
				array[n++] = regions[i];
		return n;
	}

	/**
	 * Return the common row, col, or box of two Cells; else null.
	 * Note that box comes LAST (for a change).
	 *
	 * @param a Cell
	 * @param b Cell
	 * @return ARegion
	 */
	public static ARegion common(Cell a, Cell b) {
		if ( a.row == b.row )
			return a.row;
		if ( a.col == b.col )
			return a.col;
		if ( a.box == b.box )
			return a.box;
		return null;
	}

	public static String typeName(List<ARegion> regions) {
		if ( regions!=null && regions.size()>0 && regions.get(0)!=null )
			return regions.get(0).typeName;
		return "";
	}

	public static int typeMask(List<ARegion> regions) {
		int mask = 0;
		for ( ARegion r : regions )
			switch (r.typeIndex) {
			case Grid.BOX: mask |= 1; break;
			case Grid.ROW: mask |= 2; break;
			default: mask |= 4;
			}
		return mask;
	}

	private static final String[] TYPE_NAMES = {
		  "no_region", "box", "row", "box/row", "col"
		, "box/col", "row/col", "box/row/col"
	};

	public static String typeNames(List<ARegion> regions) {
		return TYPE_NAMES[typeMask(regions)];
	}

	public static String ids(List<ARegion> regions) {
		StringBuilder sb = new StringBuilder(regions.size() * 6);
		for ( ARegion r : regions)
			sb.append(' ').append(r.id);
		return sb.toString();
	}

	// returns CSV of the finned region/s. Mostly there's one fin. Multiple
	// fins are almost always in the one base. Just occassionally: BUGGER!
	// so for the testcases these MUST be consistent order, so I have to put
	// them in a set for uniqueness, then put the set into a list, and sort
	// the damn list by id. Any order will do, it just MUST be consistent and
	// by id also works for human beings, who don't want to have to think about
	// pernickity s__t like this, and then the Frmt.csv method I wrote which
	// takes a Set instead of a List is now not used, but retained coz I'm an
	// ornery bastard, who put in the work, and I might use it one day.
	// Programming is complex. sigh.
	public static String finned(List<ARegion> bases, Set<Cell> fins) {
		// A Set, for bastard uniqueness
		HashSet<ARegion> set = new HashSet<>();
		for ( Cell cell : fins )
			for ( ARegion r : cell.regions )
				if ( bases.contains(r) )
					set.add(r);
		// list, to sort the bastards by id, for consistent ordering
		List<ARegion> list = new ArrayList<>(set);
		list.sort((a, b)->{return a.id.compareTo(b.id);});
		// format the bastards, and is more human than CSV. sigh.
		return Frmu.and(list);
		// Guinness, for strength!
	}

	// get a List of the used regions from a used-regions array
	public static List<ARegion> used(boolean[] used, Grid grid) {
		List<ARegion> result = new LinkedList<>();
		for ( int i=0,n=used.length; i<n; ++i )
			if ( used[i] )
				result.add(grid.regions[i]);
		return result;
	}

	// used only for debugging
	public static String usedString(boolean[] used, Grid grid) {
		String s = "";
		boolean first = true;
		for ( ARegion r : used(used, grid) ) {
			if ( first )
				first = false;
			else
				s += ", ";
			s += r.id;
		}
		return s;
	}

	// only used for debugging
	// parse "row 1, row 3, row 6" into a "used" boolean array
	public static boolean[] used(String used) {
		final boolean[] result = new boolean[NUM_REGIONS];
		for ( String rid : used.split(CSP) )
			result[Grid.regionIndex(rid)] = true;
		return result;
	}

	/** Is the given Cell in any of these regions? */
	public static boolean contains(Iterable<ARegion> regions, Cell cell) {
		if ( regions == null )
			return false;
		for ( ARegion region : regions )
			if ( region.contains(cell) ) // fast O(1)
				return true;
		return false;
	}

	public static int usedTypes(ARegion[] regions) {
		int usedTypes = 0;
		for ( ARegion r : regions )
			usedTypes |= ISHFT[r.typeIndex];
		return usedTypes;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ types ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// a left-shifted mask for each of the 3 types of regions
	public static final int BOX_MASK = 0x1;
	public static final int ROW_MASK = 0x2;
	public static final int COL_MASK = 0x4;
	// both ROW and COL in the one mask
	public static final int ROW_COL_MASK = ROW_MASK | COL_MASK;
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
	// See: BOX_MASK, ROW_MASK, COL_MASK
	public static int types(boolean[] used) {
		int mask = 0;
		for ( int i=0,n=used.length; i<n; ++i )
			if ( used[i] )
				mask |= REGION_TYPE_MASK[REGION_TYPE[i]];
		return mask;
	}

	public static String toString(ARegion[] regions, int n) {
		final StringBuilder sb = new StringBuilder(n*6);
		sb.append(regions[0].id);
		for ( int i=1; i<n; ++i )
			sb.append(CSP).append(regions[i].id);
		return sb.toString();
	}

	/**
	 * Return the regions (expect 1 or 2 of them) common to all these cells.
	 * Note that cells should be a Set of at least 2 cells. If there's only
	 * one then "common" regions is nonsensical. The result is indeterminate
	 * if cells contains duplicates, because again that's just nonsensical.
	 * @param cells
	 * @param result {@code ArrayList<ARegion>} is not cleared first.
	 * @return a {@code ArrayList<ARegion>} result may be empty, but not null.
	 */
	public static ArrayList<ARegion> common(Collection<Cell> cells, ArrayList<ARegion> result) {
		Iterator<Cell> it = cells.iterator();
		// for first cell in cells
		Cell cell = it.next();
		ARegion box = cell.box;
		ARegion row = cell.row;
		ARegion col = cell.col;
		// foreach subsequent cell in cells
		while (it.hasNext()) {
			cell = it.next();
			if (box != cell.box) { box = null; }
			if (row != cell.row) { row = null; }
			if (col != cell.col) { col = null; }
		}
		// add the common regions to result
		if (box != null) { result.add(box); }
		if (row != null) { result.add(row); }
		if (col != null) { result.add(col); }
		return result;
	}

	/**
	 * Return a bitset containing the indexes of the regions common to all of
	 * these cells. Expect 1, 2, or 3 of them.
	 *
	 * @param cells not null, and not empty. I am this method. I'm expecting a
	 *  Set of at least two cells. If there's only one then you'll get back all
	 *  three of it's regions (the term "common" is pretty nonsensical here,
	 *  but it's the simplest to implement, so I go with it, for now). If cells
	 *  contains duplicates then the result is indeterminate. Four or more
	 *  cells can share only one common region. Also, because cells is only an
	 *  Iterable (for universality) I do not have Collections size() method, so
	 *  I ignore it and continue as if it's all good, in the hope that it's all
	 *  good; so filtering-out a poorly-sized cells-set is your problem, if
	 *  necessary. I'm hoping that it's not necessary, but at O(n) I'm a heavy
	 *  process, so it could be faster to check {@code cells.size() < 4} before
	 *  you call me, but if it NEVER happens it's just a waste of time; and if
	 *  it happens very rarely (I guess 5% +/- 1%) leave it because filtering
	 *  them out probably costs more than it saves. I'm a pretty good guesser.
	 * @return a bitset of the indexes of common regions.
	 * @throws NullPointerException if cells is null.
	 * @throws NoSuchElementException if cells is empty.
	 */
	public static int common(Iterable<Cell> cells) {
		Iterator<Cell> it = cells.iterator();
		// for first cell in cells
		Cell cell = it.next();
		ARegion box = cell.box;
		ARegion row = cell.row;
		ARegion col = cell.col;
		// foreach subsequent cell in cells
		while (it.hasNext()) {
			cell = it.next();
			if (box != cell.box) { box = null; }
			if (row != cell.row) { row = null; }
			if (col != cell.col) { col = null; }
		}
		// add the common regions to result
		int result = 0;
		if (box != null) { result |= IDX_SHFT[box.index]; }
		if (row != null) { result |= IDX_SHFT[row.index]; }
		if (col != null) { result |= IDX_SHFT[col.index]; }
		return result;
	}
	public static int common(Cell[] cells) {
		// for first cell in cells
		Cell cell = cells[0];
		ARegion box = cell.box;
		ARegion row = cell.row;
		ARegion col = cell.col;
		// foreach subsequent cell in cells
		for ( int i=1,n=cells.length; i<n; ++i) {
			cell = cells[i];
			if (box != cell.box) { box = null; }
			if (row != cell.row) { row = null; }
			if (col != cell.col) { col = null; }
		}
		// add the common regions to result
		int result = 0;
		if (box != null) { result |= IDX_SHFT[box.index]; }
		if (row != null) { result |= IDX_SHFT[row.index]; }
		if (col != null) { result |= IDX_SHFT[col.index]; }
		return result;
	}

	/**
	 * Returns the other ARegion that is common to all cells, else null.
	 *
	 * @param cells the cells to find the other common region of. Two or three
	 *  cells can have one "other" common region. Less than two cells always
	 *  returns null because "common" is nonsensical. Four-or-more cells always
	 *  returns null because four cells can have only one common region, so an
	 *  "other" common region simply cannot exist.
	 * @param region the region that these cells were found in (and you want
	 *  there other common region, if any). Note that the "other" common region
	 *  returned comes from {@code region.getGrid().regions}, ergo the same
	 *  Grid that contains the given region.
	 * @return the other ARegion common to all cells, else null meaning none.
	 */
	public static ARegion otherCommon(List<Cell> cells, ARegion region) {
		ARegion ocr = null; // otherCommonRegion (the result)
		final int n = cells.size();
		if ( n>1 && n<4 ) {
			final int crs = common(cells); // bitset of commonRegions indexes
			if ( Integer.bitCount(crs) == 2 ) {
				ocr = region.getGrid().regions[Integer.numberOfTrailingZeros(
						crs & ~IDX_SHFT[region.index])];
			}
		}
		return ocr;
	}

	/**
	 * Returns the other ARegion that is common to all cells, else null.
	 *
	 * @param cells the cells to find the other common region of. Two or three
	 *  cells can have one "other" common region. Less than two cells always
	 *  returns null because "common" is nonsensical. Four-or-more cells always
	 *  returns null because four cells can have only one common region, so an
	 *  "other" common region simply cannot exist.
	 * @param region the region that these cells were found in (and you want
	 *  there other common region, if any). Note that the "other" common region
	 *  returned comes from {@code region.getGrid().regions}, ergo the same
	 *  Grid that contains the given region.
	 * @return the other ARegion common to all cells, else null meaning none.
	 */
	public static ARegion otherCommon(Cell[] cells, ARegion region) {
		ARegion ocr = null; // otherCommonRegion (the result)
		final int n = cells.length;
		if ( n>1 && n<4 ) {
			final int crs = common(cells); // bitset of commonRegions indexes
			if ( Integer.bitCount(crs) == 2 ) {
				ocr = region.getGrid().regions[Integer.numberOfTrailingZeros(
						crs & ~IDX_SHFT[region.index])];
			}
		}
		return ocr;
	}

	// return do 'cells' 1..'n' have regions['rti']=='r'
	// called by the commonN method (below)
	private static boolean same(final ARegion r, final Cell[] cells
			, final int n, final int rti) {
		for ( int i=1; i<n; ++i ) {
			if ( cells[i].regions[rti] != r ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Populate 'result' with the regions common to the first 'n' 'cells', and
	 * return how many: 1 or 2. The intent is that if there weren't atleast one
	 * common region you would not call me, because that would be a silly waste
	 * of time, but if there are no common regions then 0 is returned and
	 * result is not modified. I am called on ALS's, of upto 8 cells.
	 * <p>
	 * I also handle n==1 (result is all three regions), but not tested.
	 * <p>
	 * There are many ALS's of 2 cells, which I handle efficiently.
	 *
	 * @param cells a fixed-size cells array
	 * @param n the number of cells to compare
	 * @param result if n is always 2 or more (normal use case) then ARegion[2]
	 *  will do, coz two cells can share a box and (a row or a col); but if you
	 *  ever call me with n==1, then ARegion[3] is required, else AIOOBE.
	 * @return the result
	 */
	public static int commonN(final Cell[] cells, final int n, final ARegion[] result) {
		if ( cells==null || n<1 )
			return 0;
		final Cell a = cells[0];
		switch ( n ) {
			case 2: {
				// there are many 2-cell ALSs which can be handled more
				// efficiently, which I hope will prove faster overall.
				final Cell b = cells[1];
				int cnt = 0;
				if(a.box == b.box) { result[cnt++] = a.box; }
				if(a.row == b.row) { result[cnt++] = a.row; }
				if(a.col == b.col) { result[cnt++] = a.row; }
				return cnt;
			}
			case 1: {
				// just in case I'm ever called with n == 1
				result[0] = a.box;
				result[1] = a.row;
				result[2] = a.col; // ARegion[3] is your responsibility!
				return 3;
			}
			default: { // n is 3 or more
				// add the common regions to result
				int cnt = 0;
				if(same(a.box, cells, n, BOX)) result[cnt++] = a.box;
				if(same(a.row, cells, n, ROW)) result[cnt++] = a.row;
				if(same(a.col, cells, n, COL)) result[cnt++] = a.col;
				// return how many
				return cnt;
			}
		}
	}

	/**
	 * How many common regions to these 'n' 'cells' share?
	 * I don't retrieve the common regions, just count them, for speed.
	 *
	 * @param cells a fixed-size cells array
	 * @param n the number of cells to compare
	 * @return the result
	 */
	public static int commonCount(final Cell[] cells, final int n) {
		if ( cells==null || n<1 )
			return 0;
		final Cell a = cells[0];
		switch ( n ) {
			case 2: {
				// there are many 2-cell ALSs which can be handled more
				// efficiently, which I hope will prove faster overall.
				final Cell b = cells[1];
				int cnt = 0;
				if(a.box == b.box) { cnt++; }
				if(a.row == b.row) { cnt++; }
				if(a.col == b.col) { cnt++; }
				return cnt;
			}
			case 1: {
				// just in case I'm ever called with n == 1
				return 3;
			}
			default: { // n is 3 or more
				int cnt = 0;
				if(same(a.box, cells, n, BOX)) cnt++;
				if(same(a.row, cells, n, ROW)) cnt++;
				if(same(a.col, cells, n, COL)) cnt++;
				return cnt;
			}
		}
	}

	/**
	 * Do these 'n' 'cells' share any common regions?
	 *
	 * @param cells a fixed-size cells array
	 * @param n the number of cells to compare
	 * @return the result
	 */
	public static boolean anyCommon(final Cell[] cells, final int n) {
		if ( cells==null || n<1 )
			return false;
		final Cell a = cells[0];
		switch ( n ) {
			case 2: {
				// there are many 2-cell ALSs which can be handled more
				// efficiently, which I hope will prove faster overall.
				final Cell b = cells[1];
				if(a.box == b.box) return true;
				if(a.row == b.row) return true;
				return a.col == b.col;
			}
			case 1: {
				// just in case I'm ever called with n == 1
				return true;
			}
			default: { // n is 3 or more
				if(same(a.box, cells, n, BOX)) return true;
				if(same(a.row, cells, n, ROW)) return true;
				return same(a.col, cells, n, COL);
			}
		}
	}

	/**
	 * Populates result with regions (1 or 2) common to cells 'a' and 'b'
	 * (which are distinct) and return how many.
	 * <p>
	 * This method is currently used only by Hidden Unique Rectangle, but is
	 * available for use elsewhere. It prefers the row/col to the box as the
	 * "reported" common region. So note the "odd" order of comparisons here:
	 * row, col, box. So that if cells are in the same row-or-col we return it
	 * FIRST, only then (unlike the rest of SE) do we consider the box. I have
	 * done this because the row-or-col is colinear so I find them easier to
	 * understand, and so, I guess, the punters will too.
	 * <p>
	 * There is nothing WRONG with a box (or indeed any other region type),
	 * they're just harder to understand. This is just my humble opinion, NOT
	 * thoroughly thought-through, with or without Da Delaware Destroyers.
	 * <p>
	 * WARNING: If a == b then I return all three of the cells regions. If you
	 * are fixing an AIOOBE then call commonSafe (below) instead!
	 *
	 * @param a the first Cell
	 * @param b the second Cell
	 * @param result {@code ARegion[]} must be large enough: ie 2 for the
	 *  "standard" region types: box, row, col.
	 * @return the number of commonRegions added to the result array.
	 */
	public static int common(final Cell a, final Cell b, final ARegion[] result) {
		assert a != b; // techies only: see method comment
		int cnt = 0;
		if (a.y == b.y) {
			result[cnt++] = a.row;
		}
		if (a.x == b.x) {
			result[cnt++] = a.col;
		}
		if (a.boxIndex == b.boxIndex) {
			result[cnt++] = a.box;
		}
		return cnt;
	}

//not_used but retain for future possible use (safety first)
//	/**
//	 * A null-safe common(Cell, Cell, ARegion) ignoring a==b;
//	 * returns result (truncated) even if it's not big enough.
//	 * @param a
//	 * @param b
//	 * @param result
//	 * @return
//	 */
//	public static int commonSafe(final Cell a, final Cell b, final ARegion[] result) {
//		// a==b is a bit odd: if the same cell is passed as both a and b then
//		// it has three common regions, throwing an AIOOBE, but my caller does
//		// not really want to process the bastard anyway, so it's expediant to
//		// completely ignore the situation here by just returning 0. all good.
//		if (a == null || b == null || a == b) {
//			return 0;
//		}
//		try {
//			return Regions.common(a, b, result);
//		} catch (Throwable eaten) {
//			// esp ArrayIndexOutOfBoundsException
//			return result.length; // should no longer happen, but safe(ish)
//		}
//	}

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
	public static ARegion common(Iterable<Cell> cells, int regionTypeIndex) {
		if (cells == null) {
			throw new NullPointerException("cells is null!");
		}
		assert atleastTwo(cells) : "cells contains less than 2 cells!";
		ARegion commonRegion = null;
		ARegion cellsRegion;
		for (Cell cell : cells) {
			cellsRegion = cell.regions[regionTypeIndex];
			if (commonRegion == null) {
				commonRegion = cellsRegion;
			} else if (cellsRegion != commonRegion) {
				return null; // not all cells share a region of this type
			}
		}
		return commonRegion; // will still be null if cells is empty
	}

	// Does cells contain atleast two cells? The slow way with Iterable.
	private static boolean atleastTwo(Iterable<?> cells) {
		Iterator<?> it = cells.iterator();
		return it.hasNext() && it.next() != null && it.hasNext();
	}

	private Regions() {} // never used
}
