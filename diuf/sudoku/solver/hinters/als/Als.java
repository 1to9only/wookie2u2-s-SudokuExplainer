/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxI;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An Almost Locked Set (ALS) is N cells in a region with N+1 maybes between
 * them. The largest possible ALS is one smaller than the number of empty cells
 * in the region.
 * <p>
 * ALSs of size one (a cell with two maybes) are ignored. Cells that take part
 * in an actual Locked Set (a Naked Pair/Triple/etc) in this region are ignored
 * by ALS_Chain.
 * <p>
 * Sudoku Explainer picked-up Almost Locked Sets from HoDoKu.
 * <pre>
 * KRC 2021-10-05 Formerly Als held cell references, now it exposes JUST an idx
 * which can be used to get the cells in this Almost Locked Set, but does not
 * hold the cell-references itself, so that any Als-based-fields don't hold the
 * whole Grid in memory.
 * </pre>
 *
 * @author hobiwan. Adapted to SE by KRC.
 */
public class Als {

//public static long cfTook;
//private static long start;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ static stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static ArrayList<Als> list(Als[] alss) {
		final ArrayList<Als> result = new ArrayList<>(alss.length);
		for ( Als a : alss )
			result.add(a);
		return result;
	}

	static List<ARegion> regionsList(Als[] alss) {
		final List<ARegion> result = new LinkedList<>();
		for ( Als a : alss )
			if ( a != null ) // sometimes the last als in alss is null
				result.add(a.region);
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~ instance stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** The indices of the cells in this ALS. */
	public final IdxI idx;
	/** this.idx exploded, which is always nice when you're purloining it. */
	public final int i0,i1,i2;

	/** A bitset of the combined potential values of the cells in this ALS.
	 * Invariant: VSIZE[maybes] == idx.size() + 1; // or it's NOT an ALS. */
	public final int maybes;

	/** The region which contains this ALS. */
	public final ARegion region;

	/** The indices of cells in this ALS which maybe each value of this ALS. */
	public final Idx[] vs = new Idx[VALUE_CEILING];

	/** Buddies of all cells in this ALS which maybe each ALS value, excluding
	 * the ALS cells themselves, ergo buds of vs. Other values remain null. */
	public final Idx[] vBuds = new Idx[VALUE_CEILING];

	/** Buddies of all cells in this ALS which maybe each ALS value, plus the
	 * ALS cells themselves, ergo vBuds plus vs. Other values remain null. */
	public final Idx[] vAll = new Idx[VALUE_CEILING];

	/** The aggregate of the buddies of all values in this ALS. */
	public Idx buds;

	/** The index of this Als in the alss array. sigh. */
	public int index;

	// prevent toString errors during construction
	private final boolean initialised;

	/**
	 * Constructs a new ALS.
	 * <p>
	 * <b>WARN:</b> You must call {@link #computeFields} before use!
	 * <b>WARN:</b> I store the passed Idx, so YOU create a new one to pass
	 * to me if you are (as is usual) reusing an Idx.
	 *
	 * @param idx
	 * @param maybes
	 * @param region
	 */
	public Als(final IdxI idx, final int maybes, final ARegion region) {
		this.idx = idx;
		i0=idx.a0; i1=idx.a1; i2=idx.a2;
		this.maybes = maybes;
		this.region = region;
		this.initialised = true;
	}

	/**
	 * computeFields after construction, to not run on double-ups.
	 * You MUST computeFields of each ALS before you query it!
	 * I populate cells when Idx constructor was used (AlsFinder)!
	 *
	 * @param grid the Grid we're currently solving
	 * @param index the index of this Als in the alss array
	 * @return this Als for method chaining
	 */
	public Als computeFields(final Grid grid, final int index) {
//		start = System.nanoTime();
//		if ( false ) {
//			// UNFASTARDISED
//			final Idx[] idxs = grid.idxs; // indices of cells that maybe 1..9
//			this.buds = new Idx();
//			for ( int v : VALUESES[maybes] ) {
//				vs[v] = Idx.ofAnd(idx, idxs[v]);
//				vBuds[v] = vs[v].commonBuddies(new Idx()).andNot(idx).and(idxs[v]);
//				vAll[v] = Idx.ofOr(vs[v], vBuds[v]);
//				buds.or(vBuds[v]);
//			}
//			this.index = index;
//		} else {
			// FASTARDISED: Idx ops done inline, for speed.
			Idx iv, vv;
			int b0=0, b1=0, b2=0;
			final Idx[] idxs = grid.idxs; // indices of cells that maybe 1..9
			for ( int v : VALUESES[maybes] ) {
				vv = vs[v] = new IdxI(i0&(iv=idxs[v]).a0, i1&iv.a1, i2&iv.a2);
// commonBuddies is slow, which really means forEach is slow. BFFIIK.
// So I went mental and wrote forBuds, which is just as slow, but fancier!
// PRE: 9,200,267,800 7908 1,163,412 2013 4,570,426 ALS_XZ
// PST: 9,216,938,200 7908 1,165,520 2013 4,578,707 ALS_XZ
// AGN: 9,296,150,600 7908 1,175,537 2013 4,618,057 ALS_XZ
// So yeah, not faster, just fancier.
//				vb = vBuds[v] = vv.commonBuddies(new Idx());
				vv.forBuds( // visit b = BUDDIES[indice]
					  (b) -> {vb0=b.a0; vb1=b.a1; vb2=b.a2;} // first
					, (b) -> {vb0&=b.a0; vb1&=b.a1; vb2&=b.a2;} // subsequent
				);
				vb0 &= ~i0 & iv.a0;
				vb1 &= ~i1 & iv.a1;
				vb2 &= ~i2 & iv.a2;
				vBuds[v] = new IdxI(vb0,vb1,vb2);
				vAll[v] = new IdxI(vv.a0|vb0, vv.a1|vb1, vv.a2|vb2);
				b0|=vb0; b1|=vb1; b2|=vb2;
			}
			this.buds = new IdxI(b0,b1,b2);
			this.index = index;
//		}
//		cfTook += System.nanoTime() - start;
		return this;
	}
	// fields can be referenced from a lambda expression
	private int vb0, vb1, vb2;

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
		return region.index == other.region.index
			&& idx.equals(other.idx);
	}

	/**
	 * hashCode to match {@link #equals(java.lang.Object) }.
	 * @return {@code idx.hashCode() ^ region.index}
	 */
	@Override
	public int hashCode() {
		return idx.hashCode() ^ region.index;
	}

	/**
	 * The format method returns a human readable String representation of
	 * this Als suitable for use in hint HTML.
	 *
	 * @return "region.id: cell.ids {maybes}"<br>
	 * Note that if region is null then "^[^:]+: " goes MIA.
	 */
	public String format() {
		// region should now never be null. Never say never.
		final String rid; if(region==null) rid=""; else rid=region.id+": ";
		return rid+idx.ids(", ")+" {"+Values.toString(maybes)+"}";
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
			return "#Als#";
		if ( ts == null )
			ts =
// for debug AlsChain, but breaks test-cases
//				""+index+": "+ // @check commented out (fails test-cases)
				region.id+": "+idx+" {"+Values.toString(maybes)+"}";
		return ts;
	}
	private String ts;

}
