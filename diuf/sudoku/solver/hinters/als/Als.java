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
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.utils.Frmt;
import java.util.LinkedList;


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
	public final Idx idx;

	/** A bitset of the combined potential values of the cells in this ALS.
	 * Invariant: VSIZE[maybes] == idx.size() + 1; // or it's NOT an ALS. */
	public final int maybes;

	/** The ARegion which contains this ALS. */
	public final ARegion region;

	/** The indices of cells in this ALS which maybe each value of this ALS. */
	public final Idx[] vs = new Idx[10];

	/** Cells which see all cells in this ALS which maybe each ALS value, (I
	 * think) excepting the ALS cells themselves. Other values are null. */
	public final Idx[] vBuds = new Idx[10];

	/** vs union vBuds: Cells which see all cells in this ALS which maybe each
	 * ALS value, including the ALS cells themselves. Other values are null. */
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
		this.idx = new Idx(indices);
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
		this.cells = idx.cells(grid).clone();
		this.buddies = new Idx();
		for ( int v : VALUESES[maybes] ) {
			vs[v] = Idx.newAnd(idx, candidates[v]);
			vBuds[v] = grid.cmnBuds(vs[v], new Idx())
						   .andNot(idx).and(candidates[v]);
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
		return idx.equals(((Als)o).idx);
	}

	/**
	 * hashCode to match {@link #equals(java.lang.Object) }.
	 * @return indices.hashCode();
	 */
	@Override
	public int hashCode() {
		return idx.hashCode();
	}

	/**
	 * The format method returns a human readable String representation of
	 * this DiufAls suitable for use in hint HTML.
	 *
	 * @return "region.id: cell.ids {maybes}"<br>
	 * Note that if region is null then "^[^:]+: " goes MIA.
	 */
	public String format() {
		// region should never be null. Never say never.
		final String s; if(region==null) s=""; else s=region.id+": ";
		return s+Frmt.and(cells)+" {"+Values.andS(maybes)+"}";
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
			ts = region.id+": "+idx+" {"+Values.toString(maybes)+"}";
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
		return idx.cells(grid);
	}

}
