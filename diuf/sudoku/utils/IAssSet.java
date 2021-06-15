/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Ass;
import diuf.sudoku.utils.IMySet;

/**
 * A IAssSet is an {@code IMySet<Ass>} with a {@code get(Cell, value)} method.
 * <p>
 * LinkedMatrixAssSet is an IAssSet (extends {@code java.util.Set<Ass>}) with
 * high-performance contains and remove methods at the cost of more memory
 * causing slow construction. The other {@code MyAssSet} implementation is
 * {@code MyFunkyLinkedHashSet<Ass>}. It has faster construction but slower
 * contains and remove methods. Horses for courses. If you're going to hammer
 * contains (esp retainAll) then work-out how to use a LinkedMatrixAssSet even
 * if it requires a helper class, else just use a MyFunkyLinkedHashSet.
 * @author Keith Corlett 2018 Jan
 */
public interface IAssSet extends IMySet<Ass> {

	/**
	 * Gets the Ass(umption) from this Set with cell and value.
	 * <p>
	 * Note that all Ass's in an IAssSet have the SAME isOn (ie they're either
	 * all On or all Off), so an isOn parameter is not required.
	 * @param cell
	 * @param v
	 * @return the specified Ass, else null meaning that this Set does not
	 * contain the specified Ass.
	 */
	public Ass getAss(Cell cell, int v);

	/**
	 * Does this IAssSet contain the given Ass?
	 * @param a the Ass you seek, may NOT be null!
	 * @return true if this IAssSet contains the given Ass, else false.
	 */
	public boolean contains(Ass a);

}