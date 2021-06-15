/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;


/** A link between two potential values (candidates) of two cells. */
public final class Link {

	public final Cell srcCell;  public final int srcValue;
	public final Cell endCell;  public final int endValue;

	public Link(Ass src, Ass dst) {
		this.srcCell = src.cell;  this.srcValue = src.value;
		this.endCell = dst.cell;  this.endValue = dst.value;
	}

	public Link(Cell srcCell, int srcValue, Cell dstCell, int dstValue) {
		this.srcCell = srcCell;  this.srcValue = srcValue;
		this.endCell = dstCell;  this.endValue = dstValue;
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
			&& srcCell.hashCode == that.srcCell.hashCode
			&& endCell.hashCode == that.endCell.hashCode;
	}

	@Override
	public int hashCode() {
		if ( hashCode == 0 ) {
			int h = srcCell.hashCode;
			h = (h<<4) + srcValue;
			h = (h<<8) + endCell.hashCode;
			h = (h<<4) + endValue;
			hashCode = h;
		}
		return hashCode;
	}
	public int hashCode;

	@Override
	public String toString() {
		// to build links statement in test-case from error message
		return "\""+srcCell.id+"\""; 
		//return srcCell.id+"-"+srcValue+"==>"+endCell.id+"-"+endValue;
	}
}
