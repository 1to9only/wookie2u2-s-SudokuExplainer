/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;


/**
 * The GrabBag class holds static references to stateful objects so that I can
 * share them between classes which, by design, shouldn't know about each other.
 *
 * I'm certain that this is NOT good OO design, but I'm too thick to figure out
 * how to do it properly, so I've done it this way, which works for me, although
 * I am sure that it's as brittle as ____. So what else is there?
 *
 * Eg: getting a reference to the current LogicalSolver from the AHint is such
 * a pain in the arse that you probably wouldn't if you couldn't just chuck it
 * in a kitty-bag to retrieve on demand; and I'm only doing so to log stuff, so
 * I feel "somewhat protected" by the fact that if at all goes to pot then I'll
 * just chuck the whole mess, and think again.
 *
 * @author Keith Corlett 2019 NOV (IIRC)
 */
public class GrabBag {

	public static LogicalSolver logicalSolver;
	public static LogicalSolver setLogicalSolver(LogicalSolver logicalSolver) {
		LogicalSolver formerLS = GrabBag.logicalSolver;
		GrabBag.logicalSolver = logicalSolver;
		return formerLS;
	}
	public static Grid grid;
	public static long time; // System.nanoTime() between hints in GUI

	public static final Object ANALYSE_LOCK = new Object();

}
