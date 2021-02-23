/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;


public abstract class ABugHint extends AHint implements IActualHint {

	public ABugHint(AHinter hinter, Pots redPots) {
		super(hinter, redPots);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		// don't care if you want Big or not!
		return "Look for a " + getHintTypeName();
	}

}
