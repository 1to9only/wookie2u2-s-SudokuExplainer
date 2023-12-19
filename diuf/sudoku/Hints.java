/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.solver.AHint;
import java.util.ArrayList;

/**
 * Static helper methods dealing with lists and such of AHint.
 *
 * @author Keith Corlett 2021-07-12
 */
public class Hints {

	public static ArrayList<AHint> list(AHint hint) {
		final ArrayList<AHint> result = new ArrayList<>(1);
		result.add(hint);
		return result;
	}

	private Hints() { } // Never called
}
