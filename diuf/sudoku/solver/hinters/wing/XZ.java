/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.wing;

import diuf.sudoku.Grid;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VSHFT;

/**
 * XZ is used by BigWing to swap x and z, permanently.
 * <p>
 * Note that I'm called XZ in ALS-XZ nomenclature, even though you pass me the
 * yz cell in the Wing nomenclature, and my variable names are Wing-based.
 * Yes, it's a bit confusing!
 *
 * @author Keith Corlett 2021-01-12
 */
class XZ {
	int x; // the x value (may be swapped)
	int z; // the z value (may be swapped)
	boolean both; // do both x and z fit the Wing pattern?
	void set(Grid.Cell yz) {
		x = FIRST_VALUE[yz.maybes.bits];
		z = FIRST_VALUE[yz.maybes.bits & ~VSHFT[x]];
	}
	void swap() {
		int tmp = x;
		x = z;
		z = tmp;
	}
}
