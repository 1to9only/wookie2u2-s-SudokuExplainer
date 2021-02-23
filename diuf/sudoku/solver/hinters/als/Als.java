/*
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.als;
// Almost Locked Set from HoDoKu

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Values;
import diuf.sudoku.utils.Frmt;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * An Almost Locked Set (ALS) is N cells in a region with N+1 maybes between
 * them. The largest possible ALS is one smaller than the number of empty cells
 * in the region.
 * <p>
 * ALSs of size one (a cell with two maybes) are ignored. Cells that take part
 * in an actual Locked Set (a Naked Pair/Triple/etc) in this region are ignored
 * in ALS_Chains.
 *
 * @author hobiwan
 */
public class Als {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// cmnBuds (common buddies) was sucked out of HdkGrid into Als
	// coz HdkGrid has too many type-references, so I copy-pasted the
	// little bit of code that I need here.
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * commonBuddies: sets result to the buddies common to every cell in the
	 * given idx and returns it.
	 * <p>
	 * Note that the result set is reset (ie cleared) by each call.
	 * <p>
	 * Note: I'm only called privately, except in the test-cases, hence I'm
	 * package visible.
	 * <p>
	 * Note: I've tried a couple of times to do this with an Idx.forEach and
	 * failed miserably. I don't know how to make forEach exit early if the
	 * result Idx isEmpty already.
	 *
	 * @param idx indices of cells to get the common buddies of
	 * @param result to set to indices of common buddies
	 * @return the result SudokuSet for method chaining
	 */
	static Idx cmnBuds(Idx idx, Idx result) {
		// we start with a full result set
		result.fill();
		int bits;
		if ( (bits=idx.a0) != 0 )
			for ( int i=0,j=0; i<3; ++i,j+=9 )
				if ( result.and(grpBuds[i][(bits>>j) & 0x1FF]).none() )
					return result;
		if ( (bits=idx.a1) != 0 )
			for ( int i=3,j=0; i<6; ++i,j+=9 )
				if ( result.and(grpBuds[i][(bits>>j) & 0x1FF]).none() )
					return result;
		if ( (bits=idx.a2) != 0 )
			for ( int i=6,j=0; i<9; ++i,j+=9 )
				if ( result.and(grpBuds[i][(bits>>j) & 0x1FF]).none() )
					return result;
		return result;
	}

	/**
	 * Group Buddies: buddies common to all in each group of upto 9 cells.<br>
	 * First index: 0..8 because there are 9 groups.<br>
	 * Second index: 0..511 for a group of upto 9 cells (ie 9 bits).
	 */
	private static final Idx[][] grpBuds = new Idx[9][512];
	static {
		// foreach cell in Grid: an Idx of my buddies.
		final Idx[] buds = Grid.BUDDIES;
		// use prebuilt bits array rather than left-shift 41,472 times.
		final int[] bits = Idx.BITS;
		// the indices of a group of upto 9 cells.
		Idx grp = new Idx();
		// gi: group index: 0..8
		// go: group offset (start of group) in the Grid: 0, 9, 18... 72
		// combo: this combination of upto 9 cells: 0..511
		// bi: bits index 0..8, so bits[bi]: 1,2,4,8,16,32,64,128,256
		int gi, go, combo, bi; // there's gi go joke in there somewhere
		// foreach group
		for ( gi=0,go=0; gi<9; ++gi,go+=9 ) { // 0..8
			// foreach combo of up to 9 cells
			for ( combo=0; combo<512; ++combo ) { // 0..511
				// build a bitset of the group to 'and' ALL of them at once
				grp.clear();
				// foreach set (1) bit in combo, add groupOffset+bit
				for ( bi=0; bi<9; ++bi )
					if ( (combo & bits[bi]) != 0 )
						grp.add(go+bi);
				// calculate buddies common to all cells in this combo
				final Idx gb = grpBuds[gi][combo] = new Idx(true); // FULL
				grp.forEach1((indice) -> gb.and(buds[indice]));
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ static stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// turn the first n elements of array into a LinkedList
	static LinkedList<Als> linkedList(int n, Als[] array) {
		LinkedList<Als> list = new LinkedList<>();
		for ( int i=0; i<=n; ++i )
			list.add(array[i]);
		return list;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ instance stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** The indices of the cells in this ALS. */
	public final Idx set;

	/** A bitset of the combined potential values of the cells in this ALS. */
	public final int maybes;

	/** The ARegion which contains this ALS. */
	public final ARegion region;

	/** The indices of cells in this ALS which maybe each value of this ALS. */
	public final Idx[] vs = new Idx[10];

	/** Buddies common to all cells in this ALS which maybe each value of this
	 * ALS. Other values are left null. */
	public final Idx[] vBuds = new Idx[10];

	/** All cells in this ALS which maybe v: vs union vBuds. */
	public final Idx[] vAll = new Idx[10];

	/** The union of the buddies of all values in this ALS. */
	public Cell[] cells;

	/** The union of the buddies of all values in this ALS. */
	public Idx buddies;

	// prevent toString errors during construction
	private final boolean initialised;

	/**
	 * Constructs a new ALS.
	 * <p>
	 * <b>Note Well:</b> An ALS cannot be used until
	 * {@link #computeFields(solver.SudokuStepFinder)} has been called.
	 * @param indices
	 * @param candidates
	 * @param region
	 */
	public Als(Idx indices, int candidates, ARegion region) {
		this.set = new Idx(indices);
		this.maybes = candidates;
		this.region = region;
		this.initialised = true;
	}

	/**
	 * Computes all the additional fields; these fields are not calculated in
	 * the constructor to avoid doing so for any double-ups.
	 *
	 * @param grid the Grid we're currently solving
	 * @param candidates an array of HdkSet 1..9 containing the indices of the
	 * cells in the sudoku grid which maybe this value.
	 */
	public void computeFields(Grid grid, Idx[] candidates) {
		this.cells = set.cells(grid).clone();
		this.buddies = new Idx();
		for ( int v : Values.ARRAYS[maybes] ) {
			vs[v] = Idx.newAnd(set, candidates[v]);
			vBuds[v] = cmnBuds(vs[v], new Idx())
							.andNot(set).and(candidates[v]);
			vAll[v] = Idx.newOr(vBuds[v], vs[v]);
			buddies.or(vBuds[v]);
		}
	}

	/**
	 * Two ALSs are equal if there indices are equal.
	 * @param o
	 * @return indices.equals(((Als)o).indices);
	 */
	@Override
	public boolean equals(Object o) {
		if ( o==null || !(o instanceof Als) )
			return false;
		return set.equals(((Als)o).set);
	}

	/**
	 * hashCode to match {@link #equals(java.lang.Object) }.
	 * @return indices.hashCode();
	 */
	@Override
	public int hashCode() {
		return set.hashCode();
	}

	/**
	 * The format method returns a human readable String representation of
	 * this DiufAls suitable for use in hint HTML.
	 *
	 * @return "region.id: cell.ids {maybes}"<br>
	 * Note that if region is null then "^[^:]+: " goes MIA.
	 */
	public String format() {
		return (region==null ? "" : region.id+": ") // it shouldn't be null
			 + Frmt.and(cells)
			 + " {"+Values.toString(maybes, ", ", " and ")+"}";
	}

	/**
	 * Return a human readable String representation of this Almost Locked Set
	 * (ALS). An ALS is N cells in a region with N+1 maybes between them. The
	 * largest possible ALS is one smaller than the number of empty cells in
	 * the region. ALSs of size one (a cell with two maybes) are ignored. Cells
	 * that take part in an actual Locked Set (a Naked Pair/Triple/etc) in this
	 * region are ignored in ALS_Chains.
	 * @return "$region.id $idx {$maybes}"
	 */
	@Override
	public String toString() {
		// prevent errors during construction
		if ( !initialised )
			return "!initialised";
		if ( ts == null )
			ts = region.id+": "+set+" {"+Values.toString(maybes)+"}";
		return ts;
	}
	private String ts;

	/**
	 * DiufAdapter: an alternative to the {@link Idx#cells(Grid)} method;
	 * Returns a cached (in the given grid) array of the cells in the given
	 * Grid at the indices in this Als.
	 *
	 * @param grid the Grid to extract the cells from
	 * @return a cached Cell[], so clone() it before you store it!
	 */
	Cell[] cells(Grid grid) {
		return set.cells(grid);
	}

}
