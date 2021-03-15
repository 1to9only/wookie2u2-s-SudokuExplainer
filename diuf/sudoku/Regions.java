/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Static helper methods dealing with Grid.ARegion's. There's just so many of
 * them I thought it was worth creating a class to hold them all, and try to
 * get on top of my rather profligate enmethodonation methodology.
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
	public static List<ARegion> list(ARegion r1, ARegion r2) {
		List<ARegion> regions = new ArrayList<>(2);
		regions.add(r1);
		regions.add(r2);
		return regions;
	}

	// it's just faster to not do a varargs call unless you have/want to
	public static List<ARegion> list(ARegion r1, ARegion r2, ARegion r3) {
		List<ARegion> regions = new ArrayList<>(3);
		regions.add(r1);
		regions.add(r2);
		regions.add(r3);
		return regions;
	}

	// I've given up and I'm doing a varargs call. This method is non-performant!
	public static List<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4) {
		List<ARegion> result = new ArrayList<>(4);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
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

	// I give up. Wear a varargs call. This method is non-performant!
	public static List<ARegion> list(ARegion r1, ARegion r2, ARegion r3, ARegion r4, ARegion... regions) {
		List<ARegion> result = new ArrayList<>(4+regions.length);
		result.add(r1);
		result.add(r2);
		result.add(r3);
		result.add(r4);
		for ( ARegion r : regions )
			result.add(r);
		return result;
	}

	public static List<ARegion> select(int size, ARegion[] regions
			, Indexes indexes) {
		List<ARegion> result = new ArrayList<>(size);
		for ( int i : INDEXES[indexes.bits] )
			result.add(regions[i]);
		return result;
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

	public static String names(List<ARegion> rs) {
		StringBuilder sb = new StringBuilder(rs.size() * 6);
		for ( ARegion r : rs)
			sb.append(' ').append(r);
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

	private Regions() {} // never used
}
