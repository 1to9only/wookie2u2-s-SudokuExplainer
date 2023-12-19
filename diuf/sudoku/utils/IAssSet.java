/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Ass;

/**
 * A IAssSet is an {@code IMySet<Ass>} with a {@code get(Cell, value)} method.
 * <p>
 * LinkedMatrixAssSet is an IAssSet (extends {@code java.util.Set<Ass>}) with
 * high-performance contains and remove methods at the cost of more memory
 * causing slow construction. The other {@code MyAssSet} implementation is
 * {@code MyFunkyLinkedHashSet<Ass>}. It has faster construction but slower
 * contains and remove methods. Horses for courses. If you are going to hammer
 * contains (esp retainAll) then work-out how to use a LinkedMatrixAssSet even
 * if it requires a helper class, else just use a MyFunkyLinkedHashSet.
 * <p>
 * All implementations are "funky" (see LinkedMatrixAssSet.add).
 *
 * @author Keith Corlett 2018 Jan
 */
public interface IAssSet extends IMySet<Ass> {

	/**
	 * Gets the Ass(umption) from this Set by cell and value.
	 * <p>
	 * All Ass's in an IAssSet have the SAME isOn (ie they are either all On or
	 * all Off), so an isOn parameter is not required. java.util.Set has no get
	 * method, so get is defined in IMySet, but getAss is defined here in the
	 * type-specific IAssSet; so this interface is the one to use for Ass-sets.
	 * <p>
	 * This is useful when all the assumptions in a set have lineage: a list of
	 * parent assumptions which must be true in order for this assumption to
	 * be viable, so that we can walk the parents-lists to work-out who's your
	 * daddy.
	 *
	 * @param cell the cell of the target assumption
	 * @param v the value of the target assumption
	 * @return the specified Ass, else null meaning that this Set does not
	 *  contain the specified Ass.
	 */
	public Ass getAss(Cell cell, int v);

	/**
	 * Gets the Ass(umption) from this Set by indice and value.
	 * <p>
	 * All Ass's in an IAssSet have the SAME isOn (ie they are either all On or
	 * all Off), so an isOn parameter is not required. java.util.Set has no get
	 * method, so get is defined in IMySet, but getAss is defined here in the
	 * type-specific IAssSet; so this interface is the one to use for Ass-sets.
	 * <p>
	 * This is useful when all the assumptions in a set have lineage: a list of
	 * parent assumptions which must be true in order for this assumption to
	 * exist, so that we can walk the parents-lists all the way back to the
	 * initial Assumption.
	 *
	 * @param indice
	 * @param v
	 * @return the specified Ass, else null meaning that this Set does not
	 * contain the specified Ass.
	 */
	public Ass getAss(int indice, int v);

	/**
	 * Does this IAssSet contain the given Ass?
	 *
	 * @param a the Ass you seek, may NOT be null!
	 * @return true if this IAssSet contains the given Ass, else false.
	 */
	public boolean contains(Ass a);

	/**
	 * Return the Ass from this Set with the given hashCode.
	 * <p>
	 * The get(hashCode) method is the only way to get an On out of a set that
	 * notionally contains Offs.
	 * <p>
	 * Note that my impl in LinkedMatrixAssSet is a kludge!
	 *
	 * @param hashCode to get
	 * @return the Ass, or null meaning "not found"
	 */
	public Ass getAss(int hashCode);

}