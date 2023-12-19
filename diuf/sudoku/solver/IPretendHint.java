/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * IPretendHint is a marker (empty) interface to denote hints that are NOT
 * actually hints.
 * <p>
 * Back-story: I have inverted differentiation of pretend and actual hints.
 * Previously all actual hint-types implements IActualHint, which failed coz I
 * kept forgetting to implements IActualHint in new hint-types, so I inverted
 * the whole mess, so that "pretend" hints (a small static set) are marked to
 * differentiate them from the "actual" hints, which now require no marker, so
 * I do not have to remember to implement IActualHint in every single ____ing
 * hint-type. sigh.
 *
 * @author Keith Corlett 2021-07-09
 */
public interface IPretendHint {

}
