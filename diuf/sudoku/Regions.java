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
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.utils.Frmt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


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

	// I've given up and I'm doing a varargs call. This method is non-performant!
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4) {
		ArrayList<ARegion> result = new ArrayList<>(4);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		return result;
	}

	// I give up. Wear a varargs call. This method is non-performant!
	public static ArrayList<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4, ARegion... regions) {
		ArrayList<ARegion> result = new ArrayList<>(4+regions.length);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		for ( ARegion r : regions )
			result.add(r);
		return result;
	}

	// a new java.util.ArrayList<ARegion> of an existing array, not the shit
	// local-ArrayList returned by Arrays.asList, which isn't comparable.
	public static ArrayList<ARegion> list(ARegion[] regions) {
		ArrayList<ARegion> result = new ArrayList<>(regions.length);
		for ( ARegion r : regions )
			result.add(r);
		return result;
	}

	public static ArrayList<ARegion> list(int size, ARegion[] regions, int bits) {
		return list(size, regions, INDEXES[bits]);
	}

	public static ArrayList<ARegion> list(int size, ARegion[] regions, int[] indexes) {
		ArrayList<ARegion> result = new ArrayList<>(size);
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
	 * Repopulate the given idx with the 'bits' cells in regions.
	 *
	 * @param regions
	 * @param bits
	 * @param idx the result Idx
	 */
	public static void select(ARegion[] regions, int bits, final Idx idx) {
		idx.clear();
		for ( int i=0; i<9; ++i )
			if ( (bits & ISHFT[i]) != 0 )
				idx.or(regions[i].idx);
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
		for ( int i=0; i<9; ++i )
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

	public static String typeName(List<ARegion> rs) {
		if ( rs!=null && rs.size()>0 && rs.get(0)!=null )
			return rs.get(0).typeName;
		return "";
	}

	public static int typeMask(List<ARegion> rs) {
		int mask = 0;
		for ( ARegion r : rs )
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

	public static String typeNames(List<ARegion> rs) {
		return TYPE_NAMES[typeMask(rs)];
	}

	public static String ids(List<ARegion> rs) {
		StringBuilder sb = new StringBuilder(rs.size() * 6);
		for ( ARegion r : rs)
			sb.append(' ').append(r.id);
		return sb.toString();
	}

//not used
//	public static String ids(Idx rs) {
//		final StringBuilder sb = new StringBuilder(rs.size() * 6);
//		rs.forEach((cnt, ri) -> {
//			if(cnt>0) sb.append(' ');
//			sb.append(IDS[ri]);
//		});
//		return sb.toString();
//	}

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
		return Frmt.and(list);
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

//	// only used for debugging
//	public static String usedString(boolean[] used, Grid grid) {
//		String s = "";
//		boolean first = true;
//		for ( ARegion r : used(used, grid) ) {
//			if ( first )
//				first = false;
//			else
//				s += ", ";
//			s += r.id;
//		}
//		return s;
//	}

	/**
	 * Region ID's, for when you don't HAVE a bloody Grid!
	 */
	public static final String[] IDS = {
		  "box 1", "box 2", "box 3", "box 4", "box 5", "box 6", "box 7", "box 8", "box 9"
		, "row 1", "row 2", "row 3", "row 4", "row 5", "row 6", "row 7", "row 8", "row 9"
		, "col A", "col B", "col C", "col D", "col E", "col F", "col G", "col H", "col I"
	};

	public static int index(String rid) {
		switch ( rid.charAt(0) ) {
		case 'b': return  0 + (rid.charAt(4)-'1'); // "box 8"
		case 'r': return  9 + (rid.charAt(4)-'1'); // "row 1"
		case 'c': return 18 + (rid.charAt(4)-'A'); // "col C"
		}
		throw new IllegalArgumentException("Bad rid: "+rid);
	}

	// only used for debugging
	// parse "row 1, row 3, row 6" into a "used" boolean array
	public static boolean[] used(String used) {
		boolean[] result = new boolean[27];
		for ( String rid : used.split(", ") )
			result[index(rid)] = true;
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

	// an array of the type of each of the 27 regions to look-up
	// NB: SE is (box, row, col) whereas HoDoKu is (row, col, box)
	public static final int[] REGION_TYPE = {
		BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX, BOX,
		ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW, ROW,
		COL, COL, COL, COL, COL, COL, COL, COL, COL
	};

	// a left-shifted mask for each of the 3 types of regions
	public static final int BOX_MASK = 0x1;
	public static final int ROW_MASK = 0x2;
	public static final int COL_MASK = 0x4;
	// both ROW and COL in the one mask
	public static final int ROW_COL_MASK = ROW_MASK | COL_MASK;
	// an array of the mask of each region type to look-up
	public static final int[] REGION_TYPE_MASK = {BOX_MASK, ROW_MASK, COL_MASK};

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
			sb.append(", ").append(regions[i].id);
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
			if (box != cell.box) {
				box = null;
			}
			if (row != cell.row) {
				row = null;
			}
			if (col != cell.col) {
				col = null;
			}
		}
		// add the common regions to result
		if (box != null) {
			result.add(box);
		}
		if (row != null) {
			result.add(row);
		}
		if (col != null) {
			result.add(col);
		}
		return result;
	}

	/**
	 * Populate 'result' with the regions common to the first 'n' 'cells',
	 * and return how many: 1 or 2. The intent is that if there weren't atleast
	 * one common region you would not call me, because that would be a silly
	 * waste of time, but if there are no common regions then 0 is returned and
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
		switch ( n ) {
			case 2: {
				// there are many 2-cell ALSs which can be handled more
				// efficiently, which I hope will prove faster overall.
				final Cell first = cells[0], second = cells[1];
				int cnt = 0;
				if ( first.box == second.box )
					result[cnt++] = first.box;
				if ( first.row == second.row )
					result[cnt++] = first.row;
				if ( first.col == second.col )
					result[cnt++] = first.row;
				return cnt;
			}
			case 1: {
				// just in case I'm ever called with n == 1
				final Cell first = cells[0];
				result[0] = first.box;
				result[1] = first.row;
				result[2] = first.col; // ARegion[3] is your responsibility!
				return 3;
			}
			default: { // n is 3 or more
				final Cell first = cells[0];
				ARegion box=first.box, row=first.row, col=first.col;
				for ( int i=1; i<n; ++i )
					if ( cells[i].box != box ) {
						box = null;
						break;
					}
				for ( int i=1; i<n; ++i )
					if ( cells[i].row != row ) {
						row = null;
						break;
					}
				for ( int i=1; i<n; ++i )
					if ( cells[i].col != col ) {
						col = null;
						break;
					}
				// add the common regions to result
				int cnt = 0;
				if(box!=null) result[cnt++] = box;
				if(row!=null) result[cnt++] = row;
				if(col!=null) result[cnt++] = col;
				// return how many
				return cnt;
			}
		}
	}

	/**
	 * Return the number of regions (1 or 2) common to these two cells.
	 * <p>
	 * This method is currently used only by Hidden Unique Rectangle, but is
	 * available for use elsewhere.
	 * <p>
	 * Note the "odd" order of comparisons here: row, col, box. So that if a
	 * cell is in the same row-or-col we return it FIRST, only then (unlike
	 * the rest of SE) do we consider the box.<br>
	 * I've done this simply because I prefer the row-or-col when explaining
	 * unique rectangles. I think they're just "more obvious", and therefore
	 * so likely will the punter. There's nothing wrong with a box, they're
	 * just a bit harder to get your head around in UR-land, that's all.
	 * <p>
	 * WARN: If a == b then I return all three of the cells regions. If you
	 * can't accept this then call commonRegionsSafe instead!
	 *
	 * @param a the first Cell
	 * @param b the second Cell
	 * @param result {@code ARegion[]} must be large enough: ie 2 for the
	 *  "standard" region types: box, row, col.
	 * @return the number of commonRegions added to the result array.
	 */
	public static int common(final Cell a, final Cell b, final ARegion[] result) {
		int cnt = 0;
		if (a.y == b.y) {
			result[cnt++] = a.row;
		}
		if (a.x == b.x) {
			result[cnt++] = a.col;
		}
		if (a.boxId == b.boxId) {
			result[cnt++] = a.box;
		}
		return cnt;
	}
//not used
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
