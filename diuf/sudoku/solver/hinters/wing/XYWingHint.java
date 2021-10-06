/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
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
import static diuf.sudoku.utils.Frmt.and;
import static diuf.sudoku.utils.Frmt.COMMA_SP;


/**
 * XW-Wing and XYZ-Wing hints
 */
public final class XYWingHint extends AHint implements IChildHint {

	private final boolean isXYZ;
	private final Cell xy;
	private final Cell xz;
	private final Cell yz;
	private final int zValue;
	private Collection<Link> links;

	public XYWingHint(XYWing hinter, Pots redPots, boolean isXYZ
			, Cell xy, Cell xz, Cell yz, int zValue) {
		super(hinter, redPots);
		this.isXYZ = isXYZ;
		this.xy = xy; // XYZ-Wing=3, XY-Wing=2 values
		this.xz = xz; // allways 2 values
		this.yz = yz; // allways 2 values
		this.zValue = zValue;
	}

	private int x() {
		return VFIRST[xz.maybes & ~VSHFT[zValue]];
	}

	private int y() {
		return VFIRST[yz.maybes & ~VSHFT[zValue]];
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(xz, yz);
	}

	@Override
	public Set<Cell> getBlueCells(int notUsed) {
		return new MyLinkedHashSet<>(xy);
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null ) {
			Pots pots = new Pots();
			// the z value is green (xy is orange)
			pots.put(xy, VSHFT[zValue]);
			pots.put(xz, VSHFT[zValue]);
			pots.put(yz, VSHFT[zValue]);
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			orangePots = new Pots(xy, xy.maybes, false);
			orangePots.put(xz, VSHFT[x()]);
			orangePots.put(yz, VSHFT[y()]);
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			if ( links == null ) {
				links = new ArrayList<>(2);
				int x = x();
				links.add(new Link(xy, x, xz, x));
				int y = y();
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
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet prntOffs) {
		final Cell[] ic = initGrid.cells; // initialCell
		// the bitsets representing the maybes that've been removed.
		final int rmvdXy = ic[xy.i].maybes & ~xy.maybes;
		final int rmvdXz = ic[xz.i].maybes & ~xz.maybes;
		final int rmvdYz = ic[yz.i].maybes & ~yz.maybes;
		MyLinkedList<Ass> result = new MyLinkedList<>();
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
	public String getClueHtmlImpl(boolean isBig) {
		 String s = "Look for a " + getHintTypeName();
		 if ( isBig )
			 s += ON+x()+COMMA_SP+y()+AND+"<b>"+zValue+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(xy, xz, yz))
		  .append(ON).append(zValue).toString();
	}

	@Override
	public String toHtmlImpl() {
		final String filename = isXYZ ? "XYZWingHint.html" : "XYWingHint.html";
		return Html.produce(this, filename
				, xy.id		// {0}
				, xz.id		//  1
				, yz.id		//  2
				, zValue	//  3
				, x()		//  4
				, y()		//  5
				, redPots.toString() // 6
		);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof XYWingHint))
			return false;
		XYWingHint other = (XYWingHint)o;
		if (this == other)
			return true;
		if (isXYZ != other.isXYZ)
			return false;
		if (zValue != other.zValue)
			return false;
		if (xy != other.xy)
			return false;
		if (xz != other.xz && xz != other.yz)
			return false;
		if (yz != other.xz && yz != other.yz)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		if ( hashCode == 0 ) {
			// I guess that the zValue will be almost deterministic by itself,
			// which is a value 1..9 (ie 4 bits), so leftshift the rest 4 bits.
			// Then Cell.hashCode=Grid.LSH4[y]^x; and x is 0..8 (4 bits) so we
			// left shift the rest another 4 bits each. Hope it's uniqie enough.
			// 1,2,4,8,16,32,64,128,256,512,1024,2048
			// 1 2 3 4  5  6  7   8   9  10   11   12
			hashCode = zValue
			   ^ (xy.hashCode()<<4)
			   ^ (xz.hashCode()<<8)
			   ^ (yz.hashCode()<<12);
		}
		return hashCode;
	}
	private int hashCode;

}