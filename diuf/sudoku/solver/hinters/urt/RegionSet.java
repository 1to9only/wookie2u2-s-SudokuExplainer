/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.ARegion;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * RegionSet is a {@code Set<ARegion>} with fast add, remove, contains, and
 * equals methods.
 * <p>
 * Implementation is based on an array of booleans that is coincident with the
 * given 'regions' array, which I suspect will always be Grid.regions.
 * <p>
 * The RegionSet is currently only used in UniqueRectangle so it is only 
 * package visible.
 * <p>
 * I'm just playing here really. If there are significant problems then I shall
 * revert to a {@code HashSet<ARegion>} and chuck this in the bin. Sigh. I just
 * hope it'll be faster than a HashSet for my purposes, that's all. 
 * <p>
 * RegionSet saves a whole 63 milliseconds for top1465. Wow! You can blink
 * four times in 63 milliseconds (five if you practise) and it only took me a
 * morning to implement, so if I run LogicalSolverTester just 400000 times then
 * I'll get my time back. Fortunately running it just 400000 times will take
 * only 4.03 years, presuming that I never sleep, eat, or do anything other 
 * than run it repeatedly. Welcome to How To Go Broke as a Programmer 101!
 *
 * @author Keith Corlett 2020 July 4
 */
class RegionSet extends AbstractSet<ARegion> implements Set<ARegion> {

	/** the ARegions in this Set, concurrent with the regions array. */
	public final boolean[] in = new boolean[27];

	/** the number of regions in this set. */
	public int size;

	/**
	 * add is an addOnly: it ignores a request to add an existing region, 
	 * returning false.
	 * @param r the ARegion to add
	 * @return was the region added
	 */
	@Override
	public boolean add(ARegion r) {
		if ( in[r.index] )
			return false; // "I told him we've already got one"
		in[r.index] = true;
		++size;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		return o instanceof ARegion && remove((ARegion)o);
	}
	public boolean remove(ARegion r) {
		if ( !in[r.index] )
			return false;
		in[r.index] = false;
		--size;
		return true;
	}

	@Override
	public void clear() {
		Arrays.fill(in, false);
	}

	@Override
	public boolean contains(Object o) {
		return o instanceof ARegion && contains((ARegion)o);
	}
	public boolean contains(ARegion r) {
		return in[r.index];
	}

	@Override
	public int size() {
		return size;
	}

	/** 
	 * WARNING: all this code is UNTESTED!
	 * @return a new {@code Iterator<ARegion>}
	 * @throws UnsupportedOperationException("iterator() is not supported!");
	 */
	@Override
	public Iterator<ARegion> iterator() {
// Putz!
//		return new IteratorImpl();
		throw new UnsupportedOperationException("iterator() is not supported!");
	}
// Putz!
//	/** 
//	 * WARNING: UNTESTED! overriding equals renders IteratorImpl unused, so
//	 * it has NOT been tested, and if experience tells me anything it tells
//	 * me that I suck at iterators, because I hate them, because they're too
//	 * bloody slow. Two method calls per element is O(2n), which is S__T!
//	 */
//	private class IteratorImpl implements Iterator<ARegion> {
//		int i = -1; // before first
//		int j = -1; // before first, in case next() called before hasNext()
//		@Override
//		public boolean hasNext() {
//			for ( j=i+1; j<27; ++j )
//				if ( in[j] )
//					return true;
//			return false;
//		}
//		@Override
//		public ARegion next() {
//			if ( j > i ) // hasNext() was called first, as per normal.
//				return regions[i = j];
//			// else do hasNext() and return regions[the new i]
//			for ( j=i+1; j<27; ++j )
//				if ( in[j] )
//					return regions[i = j];
//			return null;
//		}
//	}
// Putz!
//	public void setRegions(ARegion[] regions) {
//		this.regions = regions;
//	}
// Putz!
//	/** the regions array underneath my iterator. */
//	public ARegion[] regions;

	/**
	 * The equals(Object o) method just calls equals(RegionSet).
	 * @param o
	 * @return 
	 */
	@Override
	public boolean equals(Object o) {
		// I hope this faster than using a ____ing iterator!
		return o instanceof RegionSet && equals((RegionSet)o);
	}
	/**
	 * Is this.in the same as other.in?
	 * @param other
	 * @return true if the two 'in' arrays are the same, else false.
	 */
	public boolean equals(RegionSet other) {
		// localise references for speed
		final boolean[] my = this.in;
		final boolean[] his = other.in;
		// ensure that both array lengths are 27
		assert my.length==27 && his.length==27;
		// return false at the first differing content
		for ( int i=0; i<27; ++i )
			if ( my[i] != his[i] )
				return false;
		// all right, I give up, they're equal!
		return true;
	}

	// hashCode will never be called, it's only here for completeness. Note
	// that my contents should not be changed after I'm added to a Hash*.
	@Override
	public int hashCode() {
		if ( hash == 0 )
			hash = Arrays.hashCode(this.in);
		return hash;
	}
	int hash;

}
