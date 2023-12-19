/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.naked;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import static diuf.sudoku.Regions.otherCommonRegion;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;

/**
 * NakedSetDirect implements the {@link Tech#DirectNakedPair} and
 * {@link Tech#DirectNakedTriple} Sudoku solving techniques, depending on the
 * Tech passed to my constructor.
 * <p>
 * Direct everything is a wank! If this hinter is not used all of its hints are
 * found anyway, this just affects the order in which hints are found.
 * NakedSetDirect does the "Direct" variants only, which seek NakedSets that
 * cause a HiddenSingle; ie when we remove all other possible values from each
 * of these cells that leaves one only place for a value in the region. These
 * direct variants are useless, in that all hints are also found by the normal
 * NakedSet hinter; the difference is this direct variant produces a "bigger"
 * hint with the "and subsequently set this hidden single", where-as the normal
 * NakedSet leaves the hidden single to the next NakedSingle call, because
 * that is what it is for. Sigh.
 * <p>
 * For speed unwant all the Direct hinters (they are useless). <br>
 * For bigger hints want them, but they are still bloody useless.
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
	 * override {@link NakedSet#hint} refining the search to NakedSets
	 * that cause a Subsequent Naked or Hidden Single; so this NakedSetDirect
	 * hinter finds HEAPS LESS hints, which set a cells value directly.
	 * <p>
	 * If NakedSetDirect is unwanted then all of its hints are found by the
	 * following Naked/HiddenSingle, a few nanoseconds later, so Direct* is
	 * completely useless, so unwant me, unless you want useless sexy hints.
	 * ____ing politics!
	 * <p>
	 * If this NakedSet strips a victim "naked" (down to 1) it is a consequent
	 * single, so create NakedSetDirectHint and return it, else null.
	 * <p>
	 * Performance is <b>NOT</b> an issue. Direct* used only in GUI by putzes!
	 *
	 * @param grid the grid we are searching
	 * @param region the region we are searching
	 * @param cands a bitset of the values of the NakedSet found
	 * @param reds the eliminations found by standard NakedSet
	 * @return a new NakedSetHint
	 */
	@Override
	protected AHint hint(final Grid grid, final ARegion region, final int cands
			, final Idx nakedSet, Pots reds) {
		int cand; // a bitset of the value of this cell
		final Idx myOthers = new Idx();
		final Idx hisOthers = new Idx();
		// myOthers = indices in regions common to nakedSet EXCEPT nakedSet
		myOthers.setAndNot(region.idx, nakedSet); // cells in region except the
		final ARegion otherCommonRegion = otherCommonRegion(nakedSet, region);
		if ( otherCommonRegion != null ) {
			myOthers.or(hisOthers.setAndNot(otherCommonRegion.idx, nakedSet));
		}
		// foreach cell seen by all nakedSet cells
		for ( int indice : myOthers ) {
			if ( VSIZE[cand=grid.maybes[indice] & ~cands] == 1 ) {
				final Pots oranges = new Pots(nakedSet, grid.maybes, cands, DUMMY);
				return new NakedSetDirectHint(grid, this, grid.cells[indice]
				, VFIRST[cand], nakedSet, cands, oranges, reds, region);
			}
		}
		return null;
	}

}
