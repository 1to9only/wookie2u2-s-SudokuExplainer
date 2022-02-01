/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.utils.IMyPollSet;

/**
 * The LinkedCellSet interface exists to ease the swap between my
 * LinkedMatrixCellSet and LinkedHashCellSet implementations.
 * <p>
 * I extend IMyPollSet to add {idx, idx1, and idx2} specific to A*E.
 *
 * @author Keith Corlett 2021-09-04
 */
public interface ILinkedCellSet extends IMyPollSet<Cell> {
	public Idx idx();
	public boolean idx1(final Idx other, final Idx result);
	public boolean idx2(final Idx other, final Idx result);
}
