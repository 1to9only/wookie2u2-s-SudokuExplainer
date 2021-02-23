/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing2;

import diuf.sudoku.Grid;

/**
 * XZ allows us to swap x and z, permanently.
 * <p>
 * Note that I'm called XZ in ALS-XZ nomenclature, even though you pass me
 * the yz cell in the Wing nomenclature. Yes, it's confusing!
 *
 * @author Keith Corlett 2021-01-12
 */
class XZ {
	int x;
	int z;
	boolean both; // do both x and z fit the Wing pattern?
	void set(Grid.Cell yz) {
		x = yz.maybes.first();
		z = yz.maybes.next(x + 1);
	}
	void swap() {
		int tmp = x;
		x = z;
		z = tmp;
	}
}
