/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;

/**
 * HiddenSetDirect implements the DirectHiddenPair=2 and DirectHiddenTriple=3
 * Sudoku Solving Techniques, depending on the Tech passed to my constructor.
 * <p>
 * HiddenSetDirect does the "Direct" variants only, which seek HiddenSets that
 * cause a HiddenSingle; ie when we remove all other possible values from each
 * of these cells that leaves one only place for a value in the region. These
 * direct variants are useless, in that all hints are also found by the normal
 * HiddenSet hinter; the difference is this direct variant produces a "bigger"
 * hint with the "and subsequently set this hidden single", where-as the normal
 * HiddenSet leaves the hidden single to the next HiddenSingle call, because
 * that's what it's for. sigh.
 * <p>
 * For speed unwant all the Direct hinters (they're useless). <br>
 * For bigger hints want them, but they're still bloody useless.
 */
public final class HiddenSetDirect extends HiddenSet {

	/**
	 * Constructor.
	 * <p>
	 * HiddenSetDirect is useless, and is therefore not used in a chainer or
	 * the BruteForce solver! Anyone who tries just needs shooting.
	 *
	 * @param tech the Tech to implement, see class comments. <br>
	 *  Note that tech must be isDirect!
	 */
	public HiddenSetDirect(Tech tech) {
		super(tech);
		assert tech.name().startsWith("DirectHidden"); // DirectHidden
		assert degree >=2 && degree <= 3; // Pair or Triple
	}

	/**
	 * Create and return a new AHint, else null meaning none.
	 * <p>
	 * DIRECT MODE: Do the HiddenSet eliminations cause a HiddenSingle?
	 * <p>
	 * Note that I produce a hint only when HiddenSet causes a HiddenSingle,
	 * which is pretty bloody rare, and just a waste of bloody time really.
	 * <p>
	 * Note that if Direct hidden sets are wanted then they ALWAYS run before
	 * the "normal" hidden set hinters, so there will be no hidden sets which
	 * cause hidden singles remaining when the "normal" hinters run.
	 *
	 * @param r the region that we're searching
	 * @param values a bitset of the values in the hidden set
	 * @param indexes a bitset of indexes in r of the cells in the hidden set
	 * @param reds the removable (red) Cell=>Values
	 * @return a new HiddenSetDirectHint, else null meaning none;
	 */
	@Override
	protected AHint createHint(final ARegion r, int values, int indexes
			, Pots reds) {
		// build the highlighted (green) Cell=>Values
		final Pots greens = new Pots(r.cells, indexes, values, F, F);
		// build an array of the cells in this hidden set (for the hint)
		final Cell[] cells = r.atNew(indexes);
		int bits;
		// foreach value EXCEPT the hidden set values
		// NOTE: Logically it should be foreach value that's been removed, but
		// there's no fast way to calculate that, AFAIK, so this'll do; so I'm
		// leaving the "has been removed" part to the if-statement, which will
		// erroneously find ALL HiddenSingles in this region, but HiddenSingle
		// runs BEFORE HiddenSet, so it's all good, except in Shft-F5 without
		// filterHints, where this produces false positives, but run it up the
		// flagpole anyway, and see if it plays Dixie: a quick-and-dirty fix.
		for ( int v : VALUESES[VALL & ~values] ) {
			if ( VSIZE[bits=r.ridx[v].bits & ~indexes] == 1 ) {
				// this aligned set causes a Hidden Single
				return new HiddenSetDirectHint(this, cells, values, greens
						, reds, r, v, r.cells[IFIRST[bits]]);
			}
		}
		return null; // No hidden single found
	}

}