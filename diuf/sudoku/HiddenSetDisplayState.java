/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;


/**
 * The state of hidden sets to be displayed. There are three types of region
 * (box, row, col), each of which may contain a hidden set, that the user can
 * cycle-through in the GUI by Ctrl-right-clicking the row labels: 2 for hidden
 * pair, 3 for hidden triple, 4 for (very rare) hidden quads. This feature is
 * intended to help the user find hidden sets.
 *
 * @author Keith Corlett 2022-07-29
 */
public class HiddenSetDisplayState {

	public int regionType = Grid.BOX; // 0=box, 1=row, 2=col

	public int getThenAdvanceRegionType() {
		int ret = regionType;
		regionType = (regionType + 1) % 3;
		return ret;
	}

	public void reset() {
		regionType = Grid.BOX; // 0
	}

}
