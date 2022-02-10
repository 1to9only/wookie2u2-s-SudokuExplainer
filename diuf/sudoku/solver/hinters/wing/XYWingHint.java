/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.Ass;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedList;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.CSP;

/**
 * The DTO for XY-Wing and XYZ-Wing hints.
 */
public final class XYWingHint extends AHint implements IChildHint {

	private final boolean isXYZ;
	private final Cell xy;
	private final Cell xz;
	private final Cell yz;
	private final int x, y, z;
	private Collection<Link> links;

	public XYWingHint(final XYWing hinter, final Pots reds, final boolean isXYZ
			, final Cell xy, final Cell xz, final Cell yz, final int z) {
		super(hinter, reds);
		this.isXYZ = isXYZ;
		this.xy = xy; // XYZ-Wing=3, XY-Wing=2 values
		this.xz = xz; // allways 2 values
		this.yz = yz; // allways 2 values
		this.x = VFIRST[xz.maybes & ~VSHFT[z]];
		this.y = VFIRST[yz.maybes & ~VSHFT[z]];
		this.z = z;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(new Cell[]{xz, yz});
	}

	@Override
	public Set<Cell> getBlueCells(int notUsed) {
		return new MyLinkedHashSet<>(xy);
	}

	@Override
	public Pots getGreens(int notUsed) {
		if ( greenPots == null ) {
			Pots pots = new Pots();
			// z in xyz is green (xy is orange), and z is not in xy
			if ( isXYZ )
				pots.put(xy, VSHFT[z]);
			pots.put(xz, VSHFT[z]);
			pots.put(yz, VSHFT[z]);
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getOranges(int notUsed) {
		if ( orangePots == null ) {
			// remove z from xyz, coz it's green (z not in xy, so no effect).
			orangePots = new Pots(xy, xy.maybes & ~VSHFT[z], false);
			orangePots.put(xz, VSHFT[x]);
			orangePots.put(yz, VSHFT[y]);
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
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("XYWingHint.getLinks: "+ ex);
			return null;
		}
	}

	/**
	 * Get the complete parents (with ancestors) which must be applied before
	 * this hint becomes applicable; ie any parent assumptions.
	 * @param initGrid which still has the "erased"ed values.
	 * @param currGrid which no longer has the "erased"ed values.
	 * @param prntOffs an IAssSet of the potential parent "off" assumptions,
	 *  with parents.
	 * @return a LinkedList of Assumption as a Deque.
	 */
	@Override
	public MyLinkedList<Ass> getParents(final Grid initGrid, final Grid currGrid
			, final IAssSet prntOffs) {
		final Cell[] ic = initGrid.cells; // initialCell
		// the bitsets representing the maybes that've been removed.
		final int rmvdXy = ic[xy.i].maybes & ~xy.maybes;
		final int rmvdXz = ic[xz.i].maybes & ~xz.maybes;
		final int rmvdYz = ic[yz.i].maybes & ~yz.maybes;
		final MyLinkedList<Ass> result = new MyLinkedList<>();
		// parents := Asses at the indexes of the removed-bits.
		if ( rmvdXy!=0 || rmvdXz!=0 || rmvdYz!=0 ) {
			int v, sv; // value, shiftedValue
			for ( v=1; v<VALUE_CEILING; ++v ) {
				sv = VSHFT[v];
				if((rmvdXy & sv)!=0) result.linkLast(prntOffs.getAss(xy, v));
				if((rmvdXz & sv)!=0) result.linkLast(prntOffs.getAss(xz, v));
				if((rmvdYz & sv)!=0) result.linkLast(prntOffs.getAss(yz, v));
			}
		}
		return result; // maybe empty
	}

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		 String s = "Look for a " + getHintTypeName();
		 if ( isBig )
			 s += ON+x+CSP+y+AND+"<b>"+z+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(xy, xz, yz))
		  .append(ON).append(z).toString();
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
			// I guess that the zValue will be almost deterministic by itself,
			// which is a value 1..9 (ie 4 bits), so leftshift the rest 4 bits.
			// Then Cell.i left shift the rest another 4 bits each. Hope it's
			// unique enough.
			hashCode = (yz.i<<12) ^ (xz.i<<8) ^ (xy.i<<4) ^ z;
		}
		return hashCode;
	}
	private int hashCode;

}