/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu's Als class, by Bernhard Hobiger. Kudos to
 * hobiwan. Mistakes are mine.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
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
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.utils.Frmu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;


/**
 * An Almost Locked Set (ALS) is N cells in a region with N+1 maybes between
 * them. The largest possible ALS is one smaller than the number of empty cells
 * in the region.
 * <p>
 * ALSs of size one (a cell with two maybes) are ignored. Cells that take part
 * in an actual Locked Set (a Naked Pair/Triple/etc) in this region are ignored
 * in ALS_Chains.
 *
 * @author hobiwan. Adapted to SE by KRC.
 */
public class Als {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ static stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// turn the first n elements of array into a LinkedList
	static LinkedList<Als> linkedList(int n, Als[] array) {
		LinkedList<Als> list = new LinkedList<>();
		// the last ALS may not exist, so stop at end-of-array
		final int I;
		if ( n < array.length )
			I = n;
		else
			I = array.length - 1;
		for ( int i=0; i<=I; ++i )
			list.add(array[i]);
		return list;
	}

	public static Collection<Als> list(Als... alss) {
		ArrayList<Als> result = new ArrayList<>(alss.length);
		for ( Als a : alss )
			result.add(a);
		return result;
	}

	static List<ARegion> regionsList(Collection<Als> alss) {
		List<ARegion> ll = new LinkedList<>();
		for ( Als a : alss )
			if ( a != null ) // sometimes the last als in alss is null
				ll.add(a.region);
		return ll;
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
	public final Idx[] vs = new Idx[VALUE_CEILING];

	/** Cells which see all cells in this ALS which maybe each ALS value, (I
	 * think) excepting the ALS cells themselves. Other values are null. */
	public final Idx[] vBuds = new Idx[VALUE_CEILING];

	/** vs union vBuds: Cells which see all cells in this ALS which maybe each
	 * ALS value, including the ALS cells themselves. Other values are null. */
	public final Idx[] vAll = new Idx[VALUE_CEILING];

	/** The union of the buddies of all values in this ALS. */
	public Cell[] cells;

	/** The union of the buddies of all values in this ALS. */
	public Idx buddies;

	// prevent toString errors during construction
	private final boolean initialised;

	/**
	 * Constructs a new ALS.
	 * <p>
	 * <b>WARN:</b> You must call {@link #computeFields} before use!
	 *
	 * @param idx
	 * @param maybes
	 * @param region
	 */
	public Als(Idx idx, int maybes, ARegion region) {
		this.idx = new Idx(idx);
		this.maybes = maybes;
		this.region = region;
		this.initialised = true;
	}

	/**
	 * Constructs a new ALS.
	 * <p>
	 * <b>WARN:</b> You must call {@link #computeFields} before use!
	 *
	 * @param cells
	 * @param maybes
	 * @param region
	 */
	public Als(Cell[] cells, int maybes, ARegion region) {
		this(cells.clone(), maybes, region, false);
	}
	// the package contructor does not copy the given cells array
	Als(Cell[] cells, int maybes, ARegion region, boolean dummy) {
		this.cells = cells;
		this.idx = Idx.of(cells);
		this.maybes = maybes;
		this.region = region;
		this.initialised = true;
	}

	/**
	 * Compute additional fields, not in constructor so not run on double-up.
	 *
	 * @param grid the Grid we're currently solving
	 * @param candidates indices of Grid cells which maybe value 1..9
	 */
	public void computeFields(Grid grid, Idx[] candidates) {
		if ( this.cells == null )
			this.cells = idx.cells(grid, new Cell[idx.size()]);
		this.buddies = new Idx();
		for ( int v : VALUESES[maybes] ) {
			vs[v] = Idx.newAnd(idx, candidates[v]);
			vBuds[v] = vs[v].commonBuddies(new Idx()).andNot(idx).and(candidates[v]);
			vAll[v] = Idx.newOr(vs[v], vBuds[v]);
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
		return o!=null
			&& (o instanceof Als)
			&& idx.equals(((Als)o).idx);
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
		final String s; if(region==null) s=EMPTY_STRING; else s=region.id+COLON_SP;
		return s+Frmu.csv(cells)+" {"+Values.toString(maybes)+"}";
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
	 * DiufAdapter: an alternative to the {@link Idx#cellsA(Grid)} method;
	 * Returns a cached (in the given grid) array of the cells in the given
	 * Grid at the indices in this Als.
	 *
	 * @param grid the Grid to extract the cells from
	 * @return a cached Cell[], so clone() it before you store it!
	 */
	Cell[] cells(Grid grid) {
		return idx.cellsA(grid);
	}

	/**
	 * GUI Adapter: a list containing my region.
	 * @return {@code ArrayList<ARegion>}
	 */
	public ArrayList<ARegion> regions() {
		if ( regions == null )
			regions = Regions.list(region);
		return regions;
	}
	private ArrayList<ARegion> regions;

}
// Almost Locked Set from HoDoKu
