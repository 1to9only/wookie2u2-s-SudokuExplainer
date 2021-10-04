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
import java.util.Comparator;


/**
 * I've abstracted the essentials of AHinter out to an interface IHinter, to
 * remove the dependency on AHinter, so that I know longer know-or-care what
 * the implementation is, so long as it implements IHinter it's a hinter.
 * <p>
 * All hinters directly or indirectly implement IHinter, which primarily just
 * specifies the getHints method, plus half-a-dozen ancillary methods.
 * <p>
 * AHinter (the abstract hinter) now implements IHinter directly.
 * <p>
 * All LogicalSolver's collections and arrays are now of IHinter.
 *
 * @author Keith Corlett 2019 OCT
 */
public interface IHinter {

	/**
	 * Order by index in the wantedHinters array, ergo execution order.
	 * Used by the UsageMap: a TreeMap.
	 */
	public static final Comparator<IHinter> BY_EXECUTION_ORDER = new Comparator<IHinter>() {
		@Override
		public int compare(IHinter a, IHinter b) {
			return a.getIndex() - b.getIndex(); // ASCENDING
		}
	};

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

	public int getDegree(); // the size of the fish, et al

	public int getIndex(); // my index in the wantedHinters array
	public void setIndex(int i);

	/**
	 * Each IHinter MUST override toString() to produce the hinter-name.
	 * @return the name of this hinter, which is usually the tech.name(), but
	 *  Aligned*Exclusion return class-name so user sees "hacked" v "correct".
	 *  Other hinters might also get-creative in future. sigh.
	 */
	@Override
	public String toString();

}
