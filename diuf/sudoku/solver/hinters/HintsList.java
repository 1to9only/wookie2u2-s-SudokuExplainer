/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.solver.AHint;
import java.util.TreeSet;

/**
 * A TreeSet of hints whose add ignores nulls, ordered by score (the number
 * of eliminations of each hint) DESCENDING, so that the most "effective"
 * hint is always first in the "list".
 * <p>
 * Note this class was exhumed from AChainer for use also in HdkFisherman, and
 * in future anybody else who declares themselves to be a bit slow Redge (it's
 * under the bonnet son) and so wants to cache his hints rather than repeatedly
 * search to find the same bloody hints.
 */
public final class HintsList extends TreeSet<AHint> {
	private static final long serialVersionUID = 12424061398L;

	public HintsList() {
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
	public AHint getFirst() {
		if ( super.size() > 0 ) {
			AHint cached;
			// note that polling off each entry "auto-empties" the cache.
			while ( (cached=super.pollFirst()) != null ) {
				// if the cached hint is still "fresh" then return it,
				// otherwise throw away this "gone off" hint.
				if ( cached.redPots.anyCurrent()
				  // for completeness only (None of the hinters which currently
				  // use the HintsList set cell values)
				  || cached.cell!=null && cached.cell.value==0 ) {
					return cached;
				}
			}
		}
		return null;
	}

}
