/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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

	public Link(final Ass src, final Ass dst) {
		this.srcIndice = src.indice;  this.srcValue = src.value;
		this.endIndice = dst.indice;  this.endValue = dst.value;
	}

	public Link(final Cell srcCell, final int srcValue, final Cell dstCell, final int dstValue) {
		this.srcIndice = srcCell.indice;  this.srcValue = srcValue;
		this.endIndice = dstCell.indice;  this.endValue = dstValue;
	}
	public Link(final int srcIndice, final int srcValue, final int dstIndice, final int dstValue) {
		this.srcIndice = srcIndice;  this.srcValue = srcValue;
		this.endIndice = dstIndice;  this.endValue = dstValue;
	}

	@Override
	public boolean equals(final Object obj) {
		// instanceof returns false when null but may be vendor dependant coz
		// generated code always tests explicitly for null first; so I dunno!
		return obj instanceof Link && equals((Link)obj);
	}
	public boolean equals(final Link that) {
		return srcValue == that.srcValue
			&& srcIndice == that.srcIndice
			&& endValue == that.endValue
			&& endIndice == that.endIndice;
	}

	@Override
	public int hashCode() {
		if ( hashCode == 0 ) {
			int h = srcIndice;
			h = (h<<4) ^ srcValue;
			h = (h<<8) ^ endIndice;
			h = (h<<4) ^ endValue;
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
