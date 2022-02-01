/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.utils.Frmt.MINUS;


/** A link between two potential values (candidates) of two cells. */
public final class Link {

	public final int srcIndice;  public final int srcValue;
	public final int endIndice;  public final int endValue;

	public Link(Ass src, Ass dst) {
		this.srcIndice = src.cell.i;  this.srcValue = src.value;
		this.endIndice = dst.cell.i;  this.endValue = dst.value;
	}

	public Link(Cell srcCell, int srcValue, Cell dstCell, int dstValue) {
		this.srcIndice = srcCell.i;  this.srcValue = srcValue;
		this.endIndice = dstCell.i;  this.endValue = dstValue;
	}
	public Link(int srcIndice, int srcValue, int dstIndice, int dstValue) {
		this.srcIndice = srcIndice;  this.srcValue = srcValue;
		this.endIndice = dstIndice;  this.endValue = dstValue;
	}

	@Override
	public boolean equals(Object obj) {
		// instanceof returns false when null but maybe that's vendor dependant
		// coz generated code always tests explicitly for null first. I dunno!
		return obj instanceof Link && equals((Link)obj);
	}
	public boolean equals(Link that) {
		return srcValue == that.srcValue
			&& endValue == that.endValue
			&& srcIndice == that.srcIndice
			&& endIndice == that.endIndice;
	}

	@Override
	public int hashCode() {
		if ( hashCode == 0 ) {
			int h = srcIndice;
			h = (h<<4) + srcValue;
			h = (h<<8) + endIndice;
			h = (h<<4) + endValue;
			hashCode = h;
		}
		return hashCode;
	}
	public int hashCode;

	@Override
	public String toString() {
// to build links statement in test-case from error message
//		return "\""+CELL_IDS[srcIndice]+"\""; 
		return CELL_IDS[srcIndice]+MINUS+srcValue+"->"+CELL_IDS[endIndice]+MINUS+endValue;
	}
}
