/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu Als class, by Bernhard Hobiger. Kudos to
 * hobiwan. Mistakes are mine.
 *
 * Here is hobiwans standard licence statement:
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

import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxI;
import static diuf.sudoku.Values.VALUESES;
import java.util.ArrayList;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Grid.REGION_LABELS;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.utils.Frmt.*;

/**
 * Als (Almost Locked Set) is N cells in a region with N+1 maybes between
 * them. The largest possible ALS is one smaller than the number of empty
 * cells in the region.
 * <p>
 * ALSs of size one (a cell with two maybes) are ignored. Cells that take
 * part in an actual Locked Set (a Naked Pair/Triple/etc) in this region
 * are ignored by ALS_Chain.
 * <p>
 * I stole Als (Almost Locked Set) from HoDoKu. Kudos to hobiwan. Mistakes
 * are all mine. ~KRC.
 * <pre>
 * KRC 2021-10-05 Formerly Als held cell references, now it exposes JUST
 * indices/indexes which can be used to get the cells/regions from a
 * grid, but Als does not hold the cell/regions itself, so any hangovers
 * do not hold the whole Grid in memory.
 * </pre>
 *
 * @author hobiwan. Adapted to SE by KRC.
 */
public class Als {

	/**
	 * THE empty boolean[GRID_SIZE].
	 */
	private static final boolean[] EMPTY_BOOLEANS = new boolean[GRID_SIZE];

	public static ArrayList<Als> list(Als[] alss) {
		final ArrayList<Als> result = new ArrayList<>(alss.length);
		for ( Als a : alss ) {
			result.add(a);
		}
		return result;
	}

	public static StringBuilder regionLabels(final Als[] alss, final String sep) {
		final StringBuilder sb = SB(alss.length<<3); // * 8
		boolean first = true;
		for ( Als als : alss ) {
			if ( als != null ) {
				if(first) first = false; else sb.append(sep);
				sb.append(REGION_LABELS[als.regionIndex]);
			}
		}
		return sb;
	}

	/**
	 * Get CSV of $alss array.
	 * <p>
	 * In Als (not Frmt) because the last als in alss may be null,
	 * and I cause a generics type-erasure collision in Frmt.
	 *
	 * @param alss
	 * @return a comma separated values string.
	 */
	public static String csv(Als[] alss) {
		if ( alss == null )
			return "null";
		final StringBuilder sb = SB(alss.length<<4); // * 16 is just a guess
		boolean first = true;
		for ( Als als : alss ) {
			if ( als != null ) { // the last als in alss may be null. sigh.
				if(first) first=false; else sb.append(CSP);
				sb.append(als.toString());
			}
		}
		return sb.toString();
	}

	/**
	 * The indices of the cells in this ALS.
	 */
	public final IdxI idx;

	/**
	 * An exploded Idx of cells in this ALS, to avert double-dereferencing.
	 */
	public final long m0;
	public final int  m1;

	/**
	 * A bitset of the combined potential values of the cells in this ALS.
	 * Invariant: VSIZE[maybes] == idx.size() + 1; // or it is NOT an ALS.
	 */
	public final int maybes;

	/**
	 * Each of these maybes has vBuds, that is all cells in this als which
	 * maybe any of these values have some common buddies
	 */
	int buddedMaybes;

	/**
	 * The index in Grid.regions of the region which contains this ALS.
	 */
	public final int regionIndex;

	/**
	 * The indices of cells in this ALS which maybe each value of this ALS.
	 */
	public final IdxI[] vs = new IdxI[VALUE_CEILING];

	/**
	 * vs plus vBuds: cells in this ALS that maybe each ALS value (vs),
	 * plus (|) common buddies of those cells which maybe v (vBuds),
	 * ergo all the vs, or vAll.
	 */
	public final IdxI[] vAll = new IdxI[VALUE_CEILING];

	/**
	 * Buddies common to every cell in this ALS which maybe each ALS value,
	 * excluding the ALS cells themselves, ergo buds of vs, or vBuds.
	 */
	public final IdxI[] vBuds = new IdxI[VALUE_CEILING];

	/**
	 * vBuds Exploded into an array of 81 booleans, for speed in
	 * {@link diuf.sudoku.solver.hinters.als.DeathBlossom#recurse } (et al).
	 */
	public final boolean[][] vBudsB = new boolean[VALUE_CEILING][]; //GRID_SIZE

	/**
	 * The aggregate of the buddies of all values in this ALS.
	 */
	public final IdxI buds;

	/**
	 * Constructs a new Almost Locked Set (ALS), being a set of cells that has
	 * one more combined maybes than there are cells.
	 * <p>
	 * <b>WARN:</b> You must call {@link #computeFields} before use!
	 * <b>WARN:</b> I store the passed Idx, so YOU create a new one to pass
	 * to me if you are (as is usual) reusing an Idx.
	 *
	 * @param idxs grid.idxs of the grid we search
	 * @param me indices of the cells in this ALS
	 * @param maybes the combined maybes of cells in this ALS
	 * @param regionIndex the index in Grid.regions of the region that contains
	 *  the cells in this ALS
	 */
	public Als(final Idx[] idxs, final IdxI me, final int maybes, final int regionIndex) {
		long vsM0; int vsM1; // vBuds exploded
		long vBudsM0; int vBudsM1; // vBuds exploded
		long allBudsM0=0L; int allBudsM1=0; // budsAll: buds of all values in this ALS
		this.idx = me;
		this.m0 = me.m0;
		this.m1 = me.m1;
		this.maybes = maybes;
		// calculate fields
		for ( int v : VALUESES[maybes] ) {
			// vBuds: starts as all cells in the grid which maybe v
			// and end-up as common buddies which maybe v
			vBudsM0 = idxs[v].m0;
			vBudsM1 = idxs[v].m1;
			// vs[v]: indices of cells in this als which maybe v
			// calculate cells that see all vs in this ALS
			NON_LOOP: for(;;) {
			for ( int i : vs[v]=new IdxI(vsM0=m0&vBudsM0, vsM1=m1&vBudsM1) ) {
				if ( ( (vBudsM0 &= BUDS_M0[i])
					 | (vBudsM1 &= BUDS_M1[i]) ) < 1L ) {
					vBuds[v] = IdxI.EMPTY;
					vBudsB[v] = EMPTY_BOOLEANS;
					vAll[v] = vs[v];
					break NON_LOOP;
				}
			}
			buddedMaybes |= VSHFT[v];
			vBuds[v] = new IdxI(vBudsM0, vBudsM1);
			vBudsB[v] = vBuds[v].toBooleans();
			// vAll are cells in this ALS which maybe v
			// plus there common buddies which maybe v
			vAll[v] = new IdxI(vsM0|vBudsM0, vsM1|vBudsM1);
			// aggregate buds of all values in this ALS
			allBudsM0 |= vBudsM0;
			allBudsM1 |= vBudsM1;
			break;
			}
		}
		this.regionIndex = regionIndex;
		// toString requires setting buds to be my last statement
		this.buds = new IdxI(allBudsM0, allBudsM1);
	}

	/**
	 * Two ALSs are equal if regions and idxs are equal.
	 *
	 * @param o
	 * @return are these bastards equals
	 */
	@Override
	public boolean equals(Object o) {
		return o!=null
			&& (o instanceof Als)
		    && equals((Als)o);
	}

	/**
	 * Two ALSs are equal if regions and idxs are equal.
	 *
	 * @param other
	 * @return {@code region.index==o.region.index && idx.equals(o.idx)}
	 */
	public boolean equals(Als other) {
		return regionIndex == other.regionIndex
			&& idx.equals(other.idx);
	}

	/**
	 * hashCode to match {@link #equals(java.lang.Object) }.
	 * @return {@code idx.hashCode() ^ region.index}
	 */
	@Override
	public int hashCode() {
		return idx.hashCode() ^ regionIndex;
	}

	/**
	 * The format method returns a human readable String representation of
	 * this Als suitable for use in hint HTML.
	 *
	 * @return Pattern: "region.label: cell.ids {maybes}" <br>
	 *  Example: "row 1: A1, D1, E1 {1347}"
	 */
	public StringBuilder format() {
		// be generous on SB size, coz other stuff is appended to it
		return SB(128).append(REGION_LABELS[regionIndex])
		.append(COLON_SP).append(idx.idsSB(CSP, CSP))
		.append(" {").append(MAYBES_STR[maybes]).append("}");
	}

	/**
	 * Return a human readable String representation of this Almost Locked Set.
	 * toString is cached because an ALS is immutable. It doesnt change once
	 * fields are computed.
	 *
	 * @return "$region.id $idx {$maybes}"
	 */
	@Override
	public String toString() {
		// prevent errors during construction
		if ( buds == null )
			return "#Als#";
		if ( toString == null )
			toString = REGION_LABELS[regionIndex]+": "+idx+" {"+MAYBES_STR[maybes]+"}";
		return toString;
	}
	private String toString; // toString cache

}
