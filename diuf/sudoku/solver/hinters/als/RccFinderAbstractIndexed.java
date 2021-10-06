/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;

/**
 * RccFinderAbstractIndexed is the abstract RccFinder for {@link AlsChain},
 * which requires non-null start and end indexes arrays, so it implements
 * the getStartIndexes() and getEndIndexes() methods to return the starts
 * and ends arrays that are populated by each of my subclasses find methods,
 * which is defined by the RccFinder interface.
 *
 * @author Keith Corlett 2021-07-29
 */
abstract class RccFinderAbstractIndexed extends RccFinderAbstract {

    /**
	 * The index of the first RCC in {@code rccs} for each {@code alss}.
	 * My index is the alss.index, and I contain the first rccs.index,
	 * so starts and ends map from alss.index to the rccs of this ALS.
	 * starts and ends are populated by getRccs and used by AlsChain only.
	 */
    protected final int[] starts = new int[MAX_ALSS];

    /**
	 * The index of the last RCC + 1 in {@code rccs} for each {@code alss}.
	 * My index is the alss.index, and I contain last rccs.index+1 (EXCLUSIVE),
	 * so starts and ends map from alss.index to the rccs of this ALS.
	 * starts and ends are populated by getRccs and used by AlsChain only.
	 */
    protected final int[] ends = new int[MAX_ALSS];

	@Override
	public int[] getStartIndexes() {
		return starts;
	}

	@Override
	public int[] getEndIndexes() {
		return ends;
	}

}
