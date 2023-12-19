/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.Ass;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.solver.hinters.IFoxyHint;
import diuf.sudoku.solver.hinters.IHinter;

/**
 * The DTO for XY-Wing and XYZ-Wing hints.
 */
public final class XYWingHint extends AHint implements IFoxyHint {

	private final boolean isXYZ;
	private final Cell xy;
	private final Cell xz;
	private final Cell yz;
	private final int x, y, z;

	private Collection<Link> links;
	private Pots greenPots;
	private int hashCode;

	public XYWingHint(final Grid grid, final IHinter hinter, final Pots reds
			, final boolean isXYZ, final int z
			, final Cell xy, final Cell xz, final Cell yz
	) {
		super(grid, hinter, reds);
		this.isXYZ = isXYZ;
		this.xy = xy; // XYZ-Wing=3, XY-Wing=2 maybes
		this.xz = xz; // 2 maybes
		this.yz = yz; // 2 maybes
		this.x = VFIRST[xz.maybes & ~VSHFT[z]];
		this.y = VFIRST[yz.maybes & ~VSHFT[z]];
		this.z = z;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return MyLinkedHashSet.of(new int[]{xz.indice, yz.indice});
	}

	@Override
	public Set<Integer> getBlueBgIndices(int notUsed) {
		return MyLinkedHashSet.of(xy.indice);
	}

	@Override
	public Pots getGreenPots(int notUsed) {
		if ( greenPots == null ) {
			Pots pots = new Pots();
			// z in xyz is green (xy is orange), and z is not in xy
			if ( isXYZ ) {
				pots.put(xy.indice, VSHFT[z]);
			}
			pots.put(xz.indice, VSHFT[z]);
			pots.put(yz.indice, VSHFT[z]);
			greenPots = pots;
		}
		return greenPots;
	}

	@Override
	public Pots getOrangePots(int notUsed) {
		if ( orangePots == null ) {
			// remove z from xyz, coz it is green (z not in xy, so no effect).
			orangePots = new Pots(xy.indice, xy.maybes & ~VSHFT[z], DUMMY);
			orangePots.put(xz.indice, VSHFT[x]);
			orangePots.put(yz.indice, VSHFT[y]);
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public Collection<Link> getLinks(int notUsed) {
		try {
			if ( links == null ) {
				links = new ArrayList<>(2);
				links.add(new Link(xy, x, xz, x));
				links.add(new Link(xy, y, yz, y));
			}
			return links;
		} catch (Exception ex) {
			Log.teeln("WARN: "+Log.me()+": "+ ex);
			return null;
		}
	}

	/**
	 * Get the complete parents (with ancestors) which must be applied before
	 * this hint becomes applicable; ie the parent assumptions of this hint.
	 * <p>
	 * I have no Dynamic Plus XY/Z_Wings, so this code is untested! I presume
	 * that EVERY parent that I seek exists. This has proven untrue in other
	 * child hints in Shft-F5. I shall cross that bridge when I come to it.
	 *
	 * @param initialGrid which still has the "erased" values
	 * @param currentGrid from which "erased" values have been, umm, erased
	 * @param rents potential parent assumptions (each being the tail end of
	 *  a chain all the way back to the initialAss) from which I getAss each
	 *  parent of this hint
	 * @return the parent Asses of this hint
	 */
	@Override
	public MyLinkedList<Ass> getParents(final Grid initialGrid
			, final Grid currentGrid, final IAssSet rents) {
		final MyLinkedList<Ass> result = new MyLinkedList<>();
		// add an Ass foreach maybe that has been erased
		final int[] initMaybes = initialGrid.maybes;
		for ( int v : VALUESES[initMaybes[xy.indice] & ~xy.maybes] )
			result.linkLast(rents.getAss(xy, v));
		for ( int v : VALUESES[initMaybes[xz.indice] & ~xz.maybes] )
			result.linkLast(rents.getAss(xz, v));
		for ( int v : VALUESES[initMaybes[yz.indice] & ~yz.maybes] )
			result.linkLast(rents.getAss(yz, v));
		return result; // may be empty
	}

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig ) {
			s += ON+x+CSP+y+AND+"<b>"+z+"</b>";
		}
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(Frmu.csv(xy, xz, yz)).append(ON).append(z);
	}

	@Override
	public String toHtmlImpl() {
		final String filename = isXYZ ? "XYZWingHint.html" : "XYWingHint.html";
		return Html.produce(this, filename
			, xy.id				//{0}
			, xz.id				// 1
			, yz.id				// 2
			, z					// 3
			, x					// 4
			, y					// 5
			, reds.toString()	// 6
		);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof XYWingHint
			&& equals((XYWingHint)o);
	}
	public boolean equals(final XYWingHint o) {
		return this == o
			|| ( isXYZ == o.isXYZ
			  && z == o.z
			  && xy == o.xy
			  && (xz == o.xz || xz == o.yz)
			  && (yz == o.xz || yz == o.yz) );
	}

	@Override
	public int hashCode() {
		if ( hashCode == 0 ) {
			// I guess zValue is almost distinct all by itself.
			// Its a value 1..9 (ie 4 bits), so leftshift the rest 4 bits.
			// Then Cell.indice left shift the rest another 4 bits each.
			// I hope thats unique enough.
			hashCode = (yz.indice<<12) ^ (xz.indice<<8) ^ (xy.indice<<4) ^ z;
		}
		return hashCode;
	}

}
