/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
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
	public static List<ARegion> list(ARegion region) {
		List<ARegion> regions = new ArrayList<>(1);
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
		for ( int i : Indexes.ARRAYS[indexes.bits] )
			result.add(regions[i]);
		return result;
	}

	public static String typeName(List<ARegion> rs) {
		return rs!=null && rs.size()>0 && rs.get(0)!=null ? rs.get(0).typeName : "";
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
		  "no_regions", "box", "row", "box/row", "col"
		, "box/col", "row/col", "regions"
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
	public static String finned(List<ARegion> bases, Set<Cell> fins) {
		// set for uniqueness
		HashSet<ARegion> finnedRegionsSet = new HashSet<>();
		for ( Cell cell : fins )
			for ( ARegion r : cell.regions )
				if ( bases.contains(r) )
					finnedRegionsSet.add(r);
		return Frmt.csv(finnedRegionsSet); // I have my own csv method!
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

	// only used for debugging
	// parse "row 1, row 3, row 6" into a "used" boolean array
	public static boolean[] parseUsed(String used) {
		boolean[] result = new boolean[27];
		for ( String s : used.split(", ") ) {
			switch ( s.charAt(0) ) { //                                 01234
			case 'r': result[ 9 + (s.charAt(4)-'1')] = true; break; // "row 1"
			case 'c': result[18 + (s.charAt(4)-'A')] = true; break; // "col C"
			default:  result[ 0 + (s.charAt(4)-'1')] = true; break; // "box 8"
			}
		}
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

	private Regions() {} // never used
}
