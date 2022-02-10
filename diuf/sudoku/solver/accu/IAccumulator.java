/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.accu;

import diuf.sudoku.Grid;
import diuf.sudoku.solver.AHint;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The common interface of an Accumulator of Hints.
 *
 * <p>There are multiple implementations:<ul>
 * <li>The GUI (getAllHints) uses a
 * (Default)HintsAccumulator to find all available hints.
 * <li>The GUI (getFirstHint) and the LogicalSolverTester use a
 * SingleHintsAccumulator to find the first available hint.
 * <li>The BruteForce uses a SingleHintsAccumulator; and also a funky
 * HintsApplicumulator which applies each hint as soon as it's found allowing
 * it's hinters (HiddenSingle, NakedSingle, Locking, NakedPair,
 * HiddenPair, and Swampfish) to solve in one pass through the grid.
 * <li>The chainers use there own local ChainersHintsAccumulator to translate
 * hints into the underlying parent Ass's (assumptions) which must be true in
 * order for this hint to become applicable.
 * </ul>
 */
public interface IAccumulator {

	/**
	 * The isSingle method returns true if a single hint is sought, else false.
	 * <p>
	 * NOTE this method is part of the interface in preference to everyone
	 * testing if the given accu is an instanceOf SingleHintsAccumulator, which
	 * is type dependant: so what if we create another type of Accumulator that
	 * also wants to find a single hint? So here's a method: plain and simple.
	 *
	 * @return true if I'm a SingleHintsAccumulator (or another-future-type of
	 * IAccumulator that only wants to find a single hint), else false.
	 */
	public boolean isSingle();

	/**
	 * Add a hint to this accumulator. Null hints are ignored, as are those
	 * that do not haveWorth, as are duplicates.
	 *
	 * @param hint the hint to add
	 * @return true only if the caller should exit immediately, false if it
	 * should continue its search for more hints. So the SingleHintsAccumulator
	 * returns true (unless the hint is null) to find the first hint, and the
	 * (Default)HintsAccumulator always returns false, to find all possible
	 * hints in the given grid.
	 * <p>
	 * Note that no exceptions are thrown (any longer) because just raising and
	 * catching an exception is expensive. Binning the HintException spead up
	 * LogicalSolverTester top1465.d5.mt by nearly 10 seconds, and let's see you
	 * get that sort of gain out of ANY of my single hinters, without porting it
	 * to CSharp (et al). Makes you think doesn't it. We often loose the woods
	 * in all those damn trees.
	 */
	public boolean add(AHint hint);

	/**
	 * Add all hints to this accumulator. Hints are just added. There is ZERO
	 * validation.
	 *
	 * @param hints the hint to add
	 * @return has this collection changed, ie hints.size()>0.
	 */
	public boolean addAll(Collection<? extends AHint> hints);

	/**
	 * Removes and returns the next hint, as per a pollable set.
	 *
	 * @return The next hint, once only.
	 */
	public AHint getHint();

	/**
	 * Peek is borrowed from the Queue interface. It returns the first hint
	 * without removing it; so you can only peek at the first hint. Any
	 * subsequent calls to peek will all return the first hint (never null).
	 *
	 * @return The first hint.
	 */
	public AHint peek();

	/**
	 * peekLast returns the last hint that was added to this IAccumulator
	 * without removing it.
	 *
	 * @return The last-added hint.
	 */
	public AHint peekLast();

	/**
	 * Does this IAccumulator contain any hint/s?
	 *
	 * @return Does this accumulator contain a first/next hint. This method is
	 * the opposite of the JAPI standard isEmpty, because it's what we almost
	 * always actually want to know. Ergo: isEmpty is/was a mistake, IMHO, not
	 * that it really matters.
	 */
	public boolean any();

	/**
	 * @return the number of hints held by this accumulator.
	 */
	public int size();

	/**
	 * Resets this accumulator to its empty-state, so that you can reuse a
	 * single instance in a loop (not have to create a new instance for each
	 * iteration). Clears the hints-list if any.
	 */
	public void reset();

	/**
	 * Sorts the hints in a HintsAccumulator by scored descending.
	 * @param comparator your comparator, or null for AHint.BY_SCORE_DESC
	 */
	public void sort(final Comparator<AHint> comparator);

	/**
	 * Remove each toRemove hint from my list in a HintsAccumulator.
	 * @param toRemove
	 */
	public void removeAll(List<AHint> toRemove);

	public List<? extends AHint> getList();

}
