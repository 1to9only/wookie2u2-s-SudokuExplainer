/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.BOX;
import static diuf.sudoku.Grid.COL;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.RIBS;
import static diuf.sudoku.Grid.ROW;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Frmu;
import static java.lang.Integer.bitCount;
import static java.lang.Integer.numberOfTrailingZeros;
import java.util.ArrayList;
import java.util.HashSet;
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

	public static final ARegion[] EMPTY_ARRAY = new ARegion[0];

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(ARegion region) {
		ArrayList<ARegion> regions = new ArrayList<>(1);
		regions.add(region);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2) {
		final ArrayList<ARegion> regions = new ArrayList<>(2);
		regions.add(r1);
		regions.add(r2);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3) {
		final ArrayList<ARegion> regions = new ArrayList<>(3);
		regions.add(r1);
		regions.add(r2);
		regions.add(r3);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4) {
		final ArrayList<ARegion> result = new ArrayList<>(4);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		return result;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static ArrayList<ARegion> list(final ARegion r1, final ARegion r2, final ARegion r3, final ARegion r4, final ARegion r5) {
		final ArrayList<ARegion> result = new ArrayList<>(5);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		result.add(r5);
		return result;
	}

	// I give up and do a varargs call, which is a bit slower.
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

	public static ArrayList<ARegion> clear(final ArrayList<ARegion> c) {
		c.clear();
		return c;
	}

	public static List<ARegion> clear(final List<ARegion> c) {
		c.clear();
		return c;
	}

	// nb: these array methods make it easier to swap to/from Lists. If there's
	// no doubt that you're always going to use an array then just create the
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
	 * Repopulate the given array with 'bits' regions, and return how many.
	 *
	 * @param regions to select from
	 * @param bits bitset of indexes in regions
	 * @param result to repopulate
	 * @return the number of regions added to array.
	 */
	public static int select(ARegion[] regions, int bits, final ARegion[] result) {
		int n = 0;
		for ( int i=0; i<REGION_SIZE; ++i )
			if ( (bits & ISHFT[i]) != 0 )
				result[n++] = regions[i];
		return n;
	}

	public static String typeName(ARegion[] regions) {
		if ( regions==null || regions.length==0 || regions[0]==null )
			return "";
		return regions[0].typeName;
	}

	public static int typeMask(ARegion[] regions) {
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

	public static String typeNames(ARegion[] regions) {
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
	public static String finned(final ARegion[] bases, final Set<Cell> fins) {
		// A Set, for bastard uniqueness
		HashSet<ARegion> set = new HashSet<>();
		for ( Cell cell : fins )
			for ( ARegion r : cell.regions )
				if ( contains(bases, r) )
					set.add(r);
		// list, to sort the bastards by id, for consistent ordering
		List<ARegion> list = new ArrayList<>(set);
		list.sort((a, b)->{return a.id.compareTo(b.id);});
		// format the bastards, and is more human than CSV. sigh.
		return Frmu.and(list);
		// Guinness, for strength!
	}

	public static int count(final boolean[] used) {
		int cnt = 0;
		for ( int i=0,n=used.length; i<n; ++i )
			if ( used[i] )
				++cnt;
		return cnt;
	}

	// get a List of the used regions from a used-regions array
	public static ARegion[] used(final boolean[] used, final Grid grid) {
		final ARegion[] result = new ARegion[count(used)];
		int cnt = 0;
		for ( int i=0,n=used.length; i<n; ++i )
			if ( used[i] )
				result[cnt++] = grid.regions[i];
		return result;
	}

	// used only for debugging
	public static String usedString(final boolean[] used, final Grid grid) {
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

	/**
	 * Is the given Cell in any of these regions?
	 *
	 * @param regions
	 * @param cell
	 * @return
	 */
	public static boolean contains(ARegion[] regions, Cell cell) {
		if ( regions == null )
			return false;
		for ( ARegion region : regions )
			if ( region.contains(cell) ) // fast O(1)
				return true;
		return false;
	}

	/**
	 * Is target in regions?
	 *
	 * @param regions
	 * @param target
	 * @return
	 */
	public static boolean contains(ARegion[] regions, ARegion target) {
		if ( regions == null )
			return false;
		for ( ARegion region : regions )
			if ( region == target ) // fast O(1)
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ common ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns the first common row, col, or box of two cells; else null.
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

	// returns a bitset of indexes of regions common to cells
	public static int common(Cell[] cells) {
		return common(cells, cells.length);
	}

	// returns a bitset of indexes of regions common to cells
	public static int common(final Cell[] cells, final int n) {
		int cribs = RIBS[cells[0].i]; // Regions Indexes BitS
		for ( int i=1; i<n; ++i )
			cribs &= RIBS[cells[i].i];
		return cribs;
	}

	// get a bitset of the indexes of common regions of cells in this Idx.
	public static int common(final Idx idx) {
		ribs = 0;
		idx.forEach( (i)->ribs = RIBS[i] // first
				   , (i)->ribs &= RIBS[i] // subsequent
		);
		return ribs;
	}
	// We need to mutate a lamda param. A field works. I know not why.
	private static int ribs;

	// We don't even need cells, just indices, so no Cell refs to clean-up.
	public static int commonI(final int[] indices) {
		return commonI(indices, indices.length);
	}
	public static int commonI(final int[] indices, final int n) {
		// a bitset of common regions indexes
		int crs = RIBS[indices[0]];
		for ( int i=1; i<n; ++i )
			crs &= RIBS[indices[i]];
		return crs;
	}

	/**
	 * Returns the other ARegion that is common to all cells, else null.
	 *
	 * @param idx Idx of the cells to find the other common region of.
	 *  Two or three cells can have one "other" common region.
	 *  Less than two cells always returns null coz "common" is nonsensical.
	 *  Four-or-more cells always returns null because four cells can have only
	 *  one common region, so an "other" common region simply cannot exist.
	 * @param r the region that these cells were found in (and you want there
	 *  other common region, if any). Note that the "other" common region
	 *  returned comes from the same Grid that contains the given region.
	 * @return the other ARegion common to all cells, else null meaning none.
	 */
	public static ARegion otherCommon(final Idx idx, final ARegion r) {
		final int n, cribs; // Common Regions Indexes BitS
		// 2 or 3 cells can share two regions, 4 can't. 1 always has three.
		if ( (n=idx.size())>1 && n<4
		  // if these cells share two common regions
		  && bitCount(cribs=Regions.common(idx)) == 2 )
			// return the other one
			return r.getGrid().regions[numberOfTrailingZeros(cribs & ~IDX_SHFT[r.index])];
		return null;
	}

	private Regions() {} // never used

}
