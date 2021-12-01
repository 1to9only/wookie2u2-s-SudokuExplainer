/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Permutations;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;

/**
 * Implementation of the Hidden Set Sudoku solving technique:
 * (HiddenPair=2, HiddenTriple=3, HiddenQuad=4).
 * <p>
 * A "hidden set" is a set of N (degree) cells that are the only possible
 * positions for N values in a region, and every value must appear in each
 * region, hence these N values are "locked into" these N cells (ie each of
 * these values must appear only in one of these cells); therefore all other
 * potential values can be eliminated from each of these N cells.
 * <p>
 * "Locked sets" are fun: the logic is that if any of these cells where any
 * value other than the hidden set values that leaves no place in the region
 * for one of the hidden set values, rendering the Sudoku invalid, and as a
 * valid Sudoku is not invalid, we can conclude that none of cells may be any
 * other value, so we can remove all other potential values from these cells.
 */
public class HiddenSet extends AHinter {

	// the Permutations Array (used by the Permutations class)
	private final int[] thePA;

	// values with 2..$degree possible positions in the region
	private final int[] candidates = new int[REGION_SIZE];

	// the removable (red) potentials, if any for this hint. This is only a
	// field so that we need not create a Pots (a HashMap) for those 96+% of
	// cases where there are no eliminations. This is just a bit faster.
	private final Pots reds = new Pots();

	/**
	 * Constructor.
	 * <p>
	 * The passed Tech primarily determines the degree: the number of cells in
	 * the HiddenSets that this hinter seeks:<ul>
	 * <li>HiddenPair = 2
	 * <li>HiddenTriple = 3
	 * <li>HiddenQuad = 4
	 * <li>HiddenPent = 5 and up is degenerate, meaning that all occurrences
	 *  are combinations of simpler techniques (hidden pair plus a triple);
	 *  but they CAN appear in get MORE hints.<br>
	 *  So I unwant Naked and Hidden Pent, coz they're basically useless.
	 * </ul>
	 *
	 * @param tech the Tech to implement, see above.
	 */
	public HiddenSet(Tech tech) {
		super(tech);
		assert this instanceof HiddenSetDirect || !tech.isDirect;
		assert degree>1 && degree<=5;
		this.thePA = new int[degree]; // for Permutations
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// if accu.isSingle then it wants oneOnly hint
		final boolean oneOnly = accu.isSingle();
		// presume that no hint will be found
		boolean result = false;
		for ( ARegion r : grid.regions ) // 9boxs + 9rows + 9cols
			if ( (result|=search(r, grid, accu)) && oneOnly )
				break;
		return result;
	}

	/**
	 * Search this region in the grid for Naked Sets.
	 * <p>
	 * This method is weirdly public. Locking finds NakedPairs/Triples but can
	 * not do all the eliminations itself, so it needs a way to ask NakedPair
	 * and NakedTriple "Is this a Naked Set? And if so do the elims for me?"
	 * So this search method is, weirdly, public, with variables created for
	 * each region, which is a bit slower in the normal use-case, but shows
	 * the user all available eliminations, so it's worth the hassle.
	 *
	 * @param r the ARegion to search
	 * @param grid the Grid to search
	 * @param accu the implementation of IAccumulator to which I add hints
	 * @return were any hint/s found?
	 */
	public boolean search(ARegion r, Grid grid, IAccumulator accu) {
		// presume that no hint will be found
		boolean result = false;
		// we need atleast 3 empty cells in the region for a Hidden Pair
		// to remove any maybes (2 cells in Pair + 1 to remove from)
		if ( r.emptyCellCount > degree ) {
			// there are sufficient empty cells in this region
			// a candidateValue has 2..degree possible positions in this region
			final int[] candidates = this.candidates;
			// the number of cells in each aligned set
			final int degree = this.degree;
			// this regions ridx values 1..9
			final Indexes[] rio = r.ridx;
			// select the candidate values (with 2..degree places in region)
			int n = 0; // number of candidate values
			for ( int v=1,card; v<VALUE_CEILING; ++v ) // 27*9 = 243
				if ( (card=rio[v].size)>1 && card<degreePlus1 ) // card-inality
					candidates[n++] = v;
			// if there are at least degree candidate values
			if ( n >= degree ) {
				// Last stack-frame til we hint. Declare all here, not in loop.
				int i // the ubiquitous index
					  // hijacked as bitset of removable potential values
				  , indexes // bitset of indexes-in-region-cells of hidden set
				  , cands; // bitset of values in the hidden set
				final Pots reds = this.reds; // removable (red) potentials
				boolean any = false;
				// foreach possible combination of degree candidate values.
				// ------------------------------------------------------------
				// Each perm is an array of degree indexes in candidates array.
				// The perm array is actually thePA, that next repopulates with
				// a distinct set of indexes; so all perm's equals all possible
				// combinations of degree (thePA.length) candidate values
				// amongst these 'n' candidate values.
				// Note: [Direct]HiddenPairs is the first use of Permutations
				// in each solve, so any bugs there tend to show up here.
				// ------------------------------------------------------------
				for ( int[] perm : new Permutations(n, thePA) ) {
					// look for $degree positions for these $degree values.
					// indexes is a bitset of the combined indexes of cells
					// having our degree values (ie a possible Hidden Set).
					indexes = 0;
					for ( i=0; i<degree; ++i )
						indexes |= rio[candidates[perm[i]]].bits;
					// if there are degree positions for our degree values
					if ( VSIZE[indexes] == degree ) {
						// build a bitset of the hidden set values
						cands = 0;
						for ( i=0; i<degree; ++i )
							cands |= VSHFT[candidates[perm[i]]];
						// WTF: seen 1 in Generate
// never happens: code retained just in case it ever happens again.
//						if ( VSIZE[cands] != degree )
//							throw new UnsolvableException("Eel vomit!");
// techies only: in Generate but not in the batch.
						assert VSIZE[cands] == degree : "VSIZE[cands]="+VSIZE[cands]+" != degree="+degree;
						// foreach cell in the hidden set which has maybes
						// other than the hidden set values, eliminate all
						// "other" maybes from this cell
						for ( int j : INDEXES[indexes] )
							// i hijacked as a bitset of maybes to remove
							if ( (i=(r.cells[j].maybes & ~cands)) != 0 ) {
								reds.put(r.cells[j], i);
								any = true;
							}
						// if any eliminations then create and add hint
						if ( any ) {
							// FOUND HiddenSet! (about 80% skip)
							// NOTE: HiddenSetDirect overrides createHint.
							final AHint hint = createHint(r, cands, indexes
									, reds.copyAndClear());
							if ( hint != null ) {
								result = true;
								if ( accu.add(hint) )
									return result;
							}
						}
						// exit if not enough cells for another set,
						// even if we didn't actually hint here
						if ( n < degree<<1 )
							return result;
						any = false;
					}
				} // next permutation
			}
		}
		return result;
	}

	/**
	 * Create and return a new AHint, else null meaning none.
	 * <p>
	 * Note: createHint is protected coz it's overridden by HiddenSetDirect.
	 *
	 * @param r the region that we're searching
	 * @param values a bitset of the values in the hidden set
	 * @param indexes a bitset of indexes in r of the cells in the hidden set
	 * @param reds the removable (red) Cell=>Values
	 * @return the new hint, else null meaning none
	 */
	protected AHint createHint(final ARegion r, final int values
			, final int indexes, final Pots reds) {
		// build the highlighted (green) Cell=>Values
		final Pots greens = new Pots(r.cells, indexes, values, F, F);
		// build an array of the cells in this hidden set (for the hint)
		final Cell[] cells = r.atNew(indexes);
		return new HiddenSetHint(this, cells, values, greens, reds, r
				, indexes);
	}

}