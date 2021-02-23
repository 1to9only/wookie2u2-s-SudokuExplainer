/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.checks;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * Valid Sudokus have at least 17 clues, otherwise produce a warning.
 */
public final class TooFewClues extends AWarningHinter {

	private String toString =  Tech.TooFewClues.nom;

	public TooFewClues() {
		super(Tech.TooFewClues);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		final int count = grid.countFilledCells();
		if ( count == 81 ) {
			toString = "Solverated"; // change the hinterNode name in hintsTree
			accu.add(new WarningHint(this, "Sudoku solverated"
					, "SudokuSolved.html"));
			return true;
		} else if ( count < 17 ) {
			toString = Tech.TooFewClues.nom;
			accu.add(new WarningHint(this
					, "Number of clues "+count+" is less than 17"
					, "TooFewClues.html", count));
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return toString;
	}

}
