/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import java.util.List;

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
	 * @param r the region we're searching
	 * @param cands the values of the hidden set
	 * @param indexes in r.cells of the cells in this hidden set
	 * @return a new NakedSetHint as a "base" AHint
	 */
	@Override
	protected AHint createHint(final ARegion r, final int cands, final int indexes) {
		assert tech.isDirect;
		Cell sib; // the sibling cell from which we shall attempt to eliminate
		int pinkos; // a bitset of the values to be removed from this cell
		// foreach cell in the region EXCEPT the cells in this naked set
		for ( int i : INDEXES[VALL & ~indexes] ) {
			// skip if sib doesn't have $degreePlus1 maybes
			if ( (sib=r.cells[i]).size == degreePlus1
			  // if sib has the nkdSetValues plus ONE other
			  && VSIZE[pinkos=sib.maybes & ~cands] == 1 ) {
				// then we can create the hint and add it to the accumulator
				return createNakedSetDirectHint(r
					, sib				// cellToSet
					, VFIRST[pinkos]	// valueToSet
					, indexes
					, cands
				);
			}
		}
		return null;
	}

	// nb: performance isn't an issue, coz we're only in the GUI.
	private AHint createNakedSetDirectHint(final ARegion r, final Cell c
			, final int v, final int indexes, final int cands) {
		assert tech.isDirect;
		// build removable (red) potentials: each cell in this region EXCEPT
		// the naked set cells which maybe any of the naked set values (cands)
		final Pots reds = new Pots();
		Cell sib; // the sibling cell
		int pinkos; // bitset of values to remove from sib
		for ( int i : INDEXES[VALL & ~indexes] ) {
			// if sib maybe any of the naked set values
			if ( (pinkos=(sib=r.cells[i]).maybes & cands) != 0 ) {
				reds.put(sib, pinkos);
			}
		}
		assert !reds.isEmpty();
		// claim the NakedSet values from the other common region (if any)
		final List<Cell> list = r.list(indexes);
		final ARegion ocr = Regions.otherCommon(list, r);
		if ( ocr != null ) {
			claimFrom(ocr.otherThan(list), cands, reds);
		}
		final Pots oranges = new Pots();
		for ( Cell cell : list ) {
			oranges.put(cell, cell.maybes);
		}
		// build the hint
		return new NakedSetDirectHint(this, c, v, list
			, new Values(cands, degree, false) // nkdSetValues
			, oranges, reds, r);
	}

}
