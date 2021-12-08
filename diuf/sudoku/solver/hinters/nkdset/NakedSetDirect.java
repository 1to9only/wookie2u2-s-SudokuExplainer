/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import java.util.Arrays;

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
		assert tech.name().startsWith("DirectNaked"); // DirectNaked
		assert degree>=2 && degree<=3; // Pair or Triple
	}

	/**
	 * If this NakedSet strips any victim "naked" (ie down to 1) then it's a
	 * subsequent naked single, so create NakedSetDirectHint and return it;
	 * else return null.
	 * <p>
	 * nb: performance isn't an issue, coz we're only in the GUI.
	 *
	 * @param region the region we're searching
	 * @param cands the values of the hidden set
	 * @param indexes in r.cells of the cells in this hidden set
	 * @return a new NakedSetHint as a "base" AHint
	 */
	@Override
	protected AHint createHint(final ARegion region, final int cands
			, final int indexes, Pots reds) {
		assert tech.isDirect;
		Cell sib; // the sibling cell from which we shall attempt to eliminate
		int pinkos; // a bitset of the values to be removed from this cell
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : INDEXES[VALL & ~indexes] )
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=region.cells[i]).size == degreePlus1
			  // if sib has the nkdSetValues plus ONE other
			  && VSIZE[pinkos=sib.maybes & ~cands] == 1 )
				// then we can create the hint and add it to the accumulator
				return createNakedSetDirectHint(region, sib, VFIRST[pinkos]
						, indexes, cands, reds);
		return null;
	}

	// nb: performance isn't an issue, coz we're only in the GUI.
	private AHint createNakedSetDirectHint(final ARegion region
			, final Cell cell, final int value, final int indexes
			, final int cands, final Pots reds) {
		assert tech.isDirect;
		// build removable (red) potentials: each cell in this region EXCEPT
		// the naked set cells which maybe any of the naked set values (cands)
		Cell sib; // the sibling cell
		int pinkos; // bitset of values to remove from sib
		for ( int cellIndex : INDEXES[VALL & ~indexes] )
			if ( (pinkos=(sib=region.cells[cellIndex]).maybes & cands) != 0 )
				reds.put(sib, pinkos);
		assert !reds.isEmpty();
		// claim the NakedSet values from the other common region (if any)
		final Cell[] nsCells = region.atNew(indexes);
		final ARegion ocr = Regions.otherCommon(nsCells, degree, region);
		if ( ocr != null ) {
			// an array of the other cells in the other common region. sigh.
			final Cell[] ocs = Cells.arrayB(REGION_SIZE);
			final int numOcs = ocr.otherThan(nsCells, degree, ocs);
			claimFrom(ocs, numOcs, cands, reds);
			Arrays.fill(ocs, null);
		}
		// build the hint
		final AHint hint = new NakedSetDirectHint(this, cell, value, nsCells
				, cands, new Pots(nsCells, cands, false), reds, region);
		return hint;
	}

}
