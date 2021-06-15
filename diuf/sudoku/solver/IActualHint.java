/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;


/**
 * IActualHint is a marker (aka empty) interface to differentiate the real
 * hints from the pseudo-hints such as messages, warnings, and analysis.
 * nb: I should probably invert this, to mark the "pretend hints", coz I keep
 * forgetting to implement this interface to mark my new hint types. sigh.
 */
public interface IActualHint {
}