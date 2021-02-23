/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Ass;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;


/**
 * Interface for indirect hints that are able to tell which
 * {@link diuf.sudoku.Ass Ass}s (complete with ancestors)
 * must be true (ie erased) for this hint to become applicable.
 * IE: find all the parent {@code Ass}es of this {@code Ass}.
 * <p>All {@code AHint}s produced by {@code Chains.getHintersEffects(...)} 
 * implement {@code IChildHint}s, and {@code getParents(...)} is called only by
 * {@code ChainsHintsAccumulator.add(...)} to find the {@code Ass}s which
 * removed any causative "parentOffs" from the {@code currGrid}.
 */
public interface IChildHint {
	/**
	 * See class comment for details.
	 * @param initGrid the unmodified {@code Grid} to search.
	 * @param currGrid the (probably) modified {@Code Grid} to search.
	 * @param parentOffs the {@code IAssSet} of parent {@code Ass}es.
	 * @return a {@code LinkedList<Ass>} of the parents of this {@code AHint}.
	 */
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet parentOffs);
}