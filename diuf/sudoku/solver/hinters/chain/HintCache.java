/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.solver.AHint;
import java.util.Collection;

/**
 * HintCache interface to either HintsListSorted or HintsListStraight.
 * This cluster____ is required because I discovered that my old HintCache
 * class (which is now HintsListSorted) wrongly found hints to be "stale"
 * so I've enabled the disabling of AChainers cache, so speed freaks can
 * continue to use it and wear the losses and us correctness pedants can
 * disable isFilteringHints and get ALL the bloody hints, especially ones
 * that ARE actually still fresh. Bastards!
 * 
 * @author Keith Corlett 2021-07-03
 */
interface HintCache extends Collection<AHint> {
	
	/**
	 * Adds the given AHint to the list, ignoring any nulls.
	 * <p>
	 * Both Set and List expose an add method, so see both because
	 * I'm trying to do both.
	 * 
	 * @param hint to add
	 * @return Has the hint been added? ie is the hint not null.
	 */
	@Override
	public boolean add(AHint h);
	
	/**
	 * Returns the first cached hint, if any, else null. Note that I extend
	 * TreeSet which is ordered by score DESCENDING, so I return the most
	 * effective hint available, if any, each time I'm called.
	 * @return 
	 */
	public AHint getFirst();
	
	/**
	 * See {@link java.util.TreeSet} 
	 * @return 
	 */
	public AHint pollFirst();
	
}
