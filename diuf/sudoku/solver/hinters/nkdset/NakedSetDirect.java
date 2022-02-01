/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;

/**
 * NakedSetDirect implements the DirectNakedPair and DirectNakedTriple Sudoku
 * solving techniques, depending on the Tech passed to my constructor.
 */
public final class NakedSetDirect extends NakedSet {

	/**
	 * Constructor.
	 *
	 * @param tech must be isDirect!
	 */
	public NakedSetDirect(final Tech tech) {
		super(tech);
		assert tech.isDirect;
		assert tech.name().startsWith("DirectNaked"); // DirectNaked
		assert degree>=2 && degree<=3; // Pair or Triple
	}

	/**
	 * This one's weird: I override createHint, which normally creates a hint,
	 * to refine search down to only NakedSet's which cause a subsequent Naked
	 * or Hidden Single, so this hinter finds LESS hints than actual NakedSet,
	 * but produces "richer" hints including subsequent Naked/Hidden Singles,
	 * which, if I'm unwanted, would be found by either of the Single hinters
	 * just a few microseconds later, so I'm useless, so unwant me, unless you
	 * want to want me, even though I'm useless. Politics. Sigh.
	 * <p>
	 * If this NakedSet strips any victim "naked" (ie down to 1) then it's a
	 * subsequent naked single, so create NakedSetDirectHint and return it;
	 * else return null.
	 * <p>
	 * nb: performance isn't an issue, coz Direct* are used only in the GUI.
	 *
	 * @param region the region we're searching
	 * @param cands the values of the hidden set
	 * @param indexes in r.cells of the cells in this hidden set
	 * @return a new NakedSetHint as a "base" AHint
	 */
	@Override
	protected AHint createHint(final ARegion region, final int cands
			, final int indexes, Pots reds) {
		Cell sib; // the sibling cell from which we shall attempt to eliminate
		int pinkos; // a bitset of the values to be removed from this cell
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : INDEXES[VALL & ~indexes] )
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=region.cells[i]).size == degreePlus1
			  // if sib has the nkdSetValues plus ONE other
			  && VSIZE[pinkos=sib.maybes & ~cands] == 1 )
				// then we can create the hint and add it to the accumulator
				return createHintImpl(region, sib, VFIRST[pinkos], indexes
						, cands, reds);
		return null;
	}

	// nb: performance isn't an issue, coz we're only in the GUI, rarely.
	private AHint createHintImpl(final ARegion region, final Cell cell
			, final int value, final int indexes, final int cands
			, final Pots reds) {
		// eliminations already exist, I just need to set subsequent single.
		assert !reds.isEmpty();
		// get the Grid
		final Grid grid = region.getGrid();
		// get an Idx of the cells in the NakedSet
		final Idx nsIdx = region.idxNew(indexes);
		// claim the NakedSet values from the other common region (if any)
		claimFromOtherCommonRegion(grid, nsIdx, region, cands, reds);
		// build and return the hint
		return new NakedSetDirectHint(this, cell, value, nsIdx
				, cands, new Pots(nsIdx, grid, cands, false), reds, region);
	}

}
