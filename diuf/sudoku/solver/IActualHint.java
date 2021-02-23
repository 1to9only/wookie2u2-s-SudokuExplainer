/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;


/**
 * IActualHint is a marker (aka empty) interface to differentiate the real
 * hints from the pseudo-hints such as messages, warnings, and analysis.
 * nb: I probably should reverse this, and mark the "message hints", because
 * I keep forgetting to implement this interface to mark my new hints.
 */
public interface IActualHint {
}