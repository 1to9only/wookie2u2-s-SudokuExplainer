/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.solver.AHint;
import java.util.TreeSet;

/**
 * HintCacheSorted returns the hint with the highest-score first. The score is
 * the total number of eliminations, with each cell-value set counting as 10.
 * <p>
 * NOTE WELL: I extend a TreeSet, which stores only elements that are distinct
 * according to it's Comparator, so the {@link AHint#BY_SCORE_DESC} Comparator
 * now includes hint.toString to retain distinct hints.
 */
public final class HintCacheSorted extends TreeSet<AHint> implements HintCache {
	private static final long serialVersionUID = 12424061401L;

	public HintCacheSorted() {
		super(AHint.BY_SCORE_DESC);
	}

	/**
	 * Adds the given AHint to the list, ignoring any nulls.
	 * @param hint to add
	 * @return Has the hint been added? ie is the hint not null.
	 */
	@Override
	public boolean add(AHint hint) {
		return hint!=null && super.add(hint);
	}

	/**
	 * Returns the first cached hint, if any, else null. Note that I extend
	 * TreeSet which is ordered by score DESCENDING, so I return the most
	 * effective hint available, if any, each time I'm called.
	 * @return 
	 */
	@Override
	public AHint getFirst() {
		if ( super.size() > 0 ) {
			AHint cached;
			// note that polling off each entry "auto-empties" the cache.
			while ( (cached=super.pollFirst()) != null ) {
				// if the cached hint is still "fresh" then return it,
				// otherwise throw away this "gone off" hint.
				if ( cached.redPots.anyCurrent()
				  // for completeness only (None of the hinters which currently
				  // use the HintCache set cell values)
				  || cached.cell!=null && cached.cell.value==0 ) {
					return cached;
				}
			}
		}
		return null;
	}

}
