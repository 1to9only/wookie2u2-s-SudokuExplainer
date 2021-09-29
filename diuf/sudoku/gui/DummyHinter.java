/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;


/**
 * The DummyHinter is a stand-in IHinter for solution hints.
 * <p>
 * Do <b>NOT</b> call me from LogicalSolver.solve (et al), I'm not real!
 * 
 * @author Keith Corlett 2019 DEC
 */
public class DummyHinter extends AHinter {
	public DummyHinter() {
		super(Tech.Analysis);
	}
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		throw new UnsupportedOperationException("DummyHinter is not a real hinter!");
	}
}
