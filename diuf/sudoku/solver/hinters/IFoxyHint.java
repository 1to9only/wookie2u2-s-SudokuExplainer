/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;

/**
 * IFoxyHint is an interface for {@code IHint}s that are able to tell which
 * {@link diuf.sudoku.Ass}es, complete with ancestors, must be true (erased)
 * for this hint to exist. Ergo, find all the Asses that caused this hint.
 * <p>
 * All {@code AHint}s produced by the {@link IFoxyHinter}s in
 * {@link diuf.sudoku.solver.hinters.chain.ChainerDynamic#fourQuickFoxes }
 * implement {@code IFoxyHint}. There {@code getParents(...)} is called by
 * {@link diuf.sudoku.solver.hinters.chain.ChainerHintsAccumulator#add} to
 * find the {@code Ass}s that erased this hints causative "Offs" from the
 * {@code currGrid}, but are still in the {@code initGrid}.
 */
public interface IFoxyHint {

	/**
	 * See IFoxyHint (this interface) comments for details.
	 *
	 * @param init the unmodified {@code Grid} to search.
	 * @param curr the modified {@Code Grid} to search.
	 * @param rents the {@code IAssSet} of parent {@code Ass}es.
	 * @return a {@code MyLinkedList<Ass>} of parents of this {@code AHint}.
	 */
	public MyLinkedList<Ass> getParents(Grid init, Grid curr, IAssSet rents);

}
