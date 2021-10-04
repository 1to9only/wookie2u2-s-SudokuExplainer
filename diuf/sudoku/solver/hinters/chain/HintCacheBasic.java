/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.solver.AHint;
import java.util.ArrayList;

/**
 * HintCacheBasic is a vanilla HintCache: a FIFO queue of hints.
 */
public final class HintCacheBasic extends ArrayList<AHint> implements HintCache {
	private static final long serialVersionUID = 12424061400L;

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
	 * pollFirst implementation is only required in the ArrayList version.
	 * TreeSet has it's own which returns the the first according to it's
	 * comparator. I have no comparator so I just return the first added.
	 */
	@Override
	public AHint pollFirst() {
		if ( super.isEmpty() )
			return null;
		return super.remove(0);
	}

	@Override
	public AHint getFirst() {
		return pollFirst();
	}

}
