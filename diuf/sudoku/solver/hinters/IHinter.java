/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * I've abstracted the essentials of AHinter out to an interface IHinter, to
 * break the dependency on AHinter.
 * <p>All hinters directly or indirectly implement IHinter, which specifies
 * the getHints method, and half-a-dozen ancillary methods.
 * <p>AHinter (the abstract hinter class) now implements IHinter directly
 * Note that active has been moved up from INumberedHinter, so that all hinters
 * are now activatable; it's just that many of them don't have a method of
 * deactivation, which you can implement however you like.
 * <p>All the collections and arrays in LogicalSolver are now of IHinter's.
 * @author Keith Corlett 2019 OCT
 */
public interface IHinter {

	/** just shorthand to make code fit on one line. */
	public final boolean T=true, F=false;

	/**
	 * The getHints method searches the given Grid for hints which it adds to
	 * the given IAccumulator. Each implementation of IHinter implements a 
	 * specific Sudoku solving technique (ie a Tech). They all currently extend
	 * AHinter, but we're no longer bound to that. Any class which implements
	 * IHinter isa LogicalSolver-ready hinter, regardless of what it extends.
	 * @param grid Grid containing a puzzle to search for hints.
	 * @param accu IAccumulator accumulates the hints produced by the various
	 * hinters. If accu.add returns true then the hinter should immediately
	 * discontinue the search and return true (found any); if add returns false
	 * then hinter should continue the search, and so may add several hints,
	 * before returning true. If no hints are found then getHints returns false.
	 * @return were any hints found.
	 */
	public boolean findHints(Grid grid, IAccumulator accu);

	public Tech getTech();

	public double getDifficulty();

	public boolean isEnabled();
	public void setIsEnabled(boolean isEnabled);

	public boolean isActive();

	public int getDegree(); // the size of the fish, et al

}
