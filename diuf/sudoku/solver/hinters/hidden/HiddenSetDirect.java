/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hidden;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;

/**
 * An instance of HiddenSetDirect implements {@link Tech#DirectHiddenPair} or
 * {@link Tech#DirectHiddenTriple} Sudoku Solving Technique, depending on the
 * Tech passed to my constructor.
 * <p>
 * HiddenSetDirect finds HiddenSets that cause a HiddenSingle. That is, when we
 * remove all other possible values from each of these cells it leaves ONE only
 * place for a value in the region.
 * <p>
 * If this hinter is not used all of its hints are found anyway, Direct just
 * produces hintier-hints, rarely. By which I mean the Direct variants are
 * completely pointless. All of the Direct hints are found by the HiddenSet
 * hinter; the difference is this Direct variant also performs the next step,
 * finding the subsequent hidden single, to produce a hintier-hint, where the
 * normal HiddenSet leaves the HiddenSingle to the next HiddenSingle call,
 * because that is exactly what it is for. Sigh.
 * <p>
 * So for speed unwant all the Direct hinters, because they are useless. <br>
 * For hintier-hints one might want them, but they are still bloody useless!
 */
public final class HiddenSetDirect extends AHiddenSet {

	/**
	 * Constructor.
	 * <p>
	 * HiddenSetDirect is useless, and is therefore not used in a chainer or
	 * the BruteForce solver! Anyone who tries just needs shooting.
	 *
	 * @param tech the Tech to implement, see class comments. <br>
	 *  Note that tech must be isDirect!
	 */
	public HiddenSetDirect(final Tech tech) {
		super(tech);
		assert tech.isDirect;
		assert tech.name().startsWith("DirectHidden"); // DirectHidden
		assert degree >=2 && degree <= 3; // Pair or Triple
	}

	/**
	 * Create and return a new HiddenSetDirectHint, else (most often) null
	 * meaning none.
	 * <p>
	 * If DirectHiddenSets are wanted they run before the "normal" HiddenSets.
	 * <p>
	 * DIRECT MODE: Do the HiddenSet eliminations cause a HiddenSingle? Note
	 * that direct mode is used only by wankers in the GUI. I produce a hint
	 * only when HiddenSet causes a HiddenSingle, which is rare, and just a
	 * waste of time really.
	 *
	 * @param region to search
	 * @param values a bitset of the values in the HiddenSet
	 * @param indexes region indexes of the cells in the HiddenSet
	 * @param reds the removable (red) Cell=>Values
	 * @return a new HiddenSetDirectHint, else null meaning none;
	 */
	@Override
	protected AHint createHint(final ARegion region, final int values
			, final int indexes, final Pots reds) {
		int last; // the ONE remaining place for value in region
		// foreach value eliminated by HiddenSet hint
		final int[] places = region.places;
		for ( int v : VALUESES[reds.candsOf()] ) {
			if ( VSIZE[last=places[v] & ~indexes] == 1 ) {
				// this aligned set causes a Hidden Single
				final Cell cellToSet = region.cells[IFIRST[last]];
				return new HiddenSetDirectHint(this, region.atNew(indexes)
					, values, region, v, cellToSet, reds
					, new Pots(cellToSet.indice, v) // greens
					, new Pots(region.cells, indexes, values, F, F) // oranges
				);
			}
		}
		return null; // No hidden single found
	}

}
