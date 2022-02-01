/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;

/**
 * A Result is the result of a hint, as distinct from all the other 
 * consequences, and Chaining hints various ons and offs. 
 * @author Keith Corlett 2018 Mar
 */
public class Result {
	public Cell cell;
	public int value;
	public boolean isOn;
	public Result(Cell cell, Integer value, boolean isOn) {
		this.cell = cell;
		this.value = value;
		this.isOn = isOn;
	}
	/** this equals is only partial, it ignores is on, because that's what
	 * works.
	 * @param cell to test
	 * @param value to test
	 * @return true if this Result has the given Cell and the given value,
	 * else false */
	public boolean equals(Cell cell, int value) {
		return this.value == value
			&& this.cell.id.equals(cell.id);
	}
	@Override
	public String toString() {
		return "Result:"+cell.id+(isOn?PLUS:MINUS)+value; // Now what does that look like?
	}
}
