/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hidden;

import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.IFoxyHinter;
import diuf.sudoku.solver.hinters.lock.SiameseLocking;

/**
 * Implementation of the {@link Tech#HiddenPair} Sudoku solving technique.
 * A HiddenPair is two cells that are the only two places for these two values
 * in a region, hence we can remove all other maybe from these two cells.
 * <p>
 * HiddenPair performance matters because its in BruteForce, and DynamicPlus.
 * <pre>
 * 2023-07-20 Split HiddenSet into HiddenPair, HiddenTriple, and HiddenQuad
 * just for speed of the line that reads the permutation. One must know the
 * permutation-size AT COMPILE TIME in order to read it efficiently, in one
 * statement. This matters because there are BILLIONS of them.
 * </pre>
 */
public class HiddenPair extends AHiddenSet implements IFoxyHinter {

	/**
	 * Constructor.
	 */
	public HiddenPair() {
		super(Tech.HiddenPair);
	}

	/**
	 * Search this $region in the $grid for HiddenPairs.
	 * <p>
	 * WARN: This method is public, for use in {@link SiameseLocking}, so
	 * do NOT change its signature willy-nilly. This is a bit slower per call
	 * in the "normal" use, but provides SiameseLocking with a solution to an
	 * otherwise ugly problem. Also, every man and his dog overrides it.
	 * <p>
	 * HiddenPair is used in BruteForce, so performance really matters here.
	 *
	 * @param region the ARegion to search
	 * @param accu the implementation of IAccumulator to which I add hints
	 * @return were any hint/s found?
	 */
	@Override
	public boolean search(final ARegion region, final IAccumulator accu) {
		boolean result = false;
		if ( region.emptyCellCount > degree ) {
			final int[] v = this.candidateValues;
			final int[] numPlaces = region.numPlaces;
			int n = 0;
			for ( int value : VALUESES[region.unsetCands] )
				if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
					v[n++] = value;
			if ( n >= degree ) {
				final int[] l = region.places; // bitset indexes by value
//<HAMMERED>
				for ( int[] p : permuter.permute(n, pa) ) {
					if ( VSIZE[l[v[p[0]]] | l[v[p[1]]]] == degree ) {
//</HAMMERED>
						final int[] c = this.candidateCands;
						n = 0;
						for ( int value : VALUESES[region.unsetCands] )
							if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
								c[n++] = VSHFT[value];
						// WARN: this is NOT a tuatology in Generate!
						if ( VSIZE[c[p[0]] | c[p[1]]] == degree ) {
							// yep, its a HiddenPair, but any elims?
							final int cands = c[p[0]] | c[p[1]];
							final int[] indices = region.indices;
							final int[] gmaybes = region.getGrid().maybes;
							final int spots = l[v[p[0]]] | l[v[p[1]]];
							boolean any = false;
							int indice; // indice of this cell in this region
							int pinkos; // potential reds values
							Pots reds = null;
							for ( int ri : INDEXES[spots] ) {
								indice = indices[ri];
								if ( (pinkos=gmaybes[indice] & ~cands) > 0 ) { // 9bits
									if(reds==null) reds = new Pots();
									reds.put(indice, pinkos);
									any = true;
								}
							}
							if ( any ) {
								// FOUND HiddenPair!
								final AHint hint = createHint(region, cands, spots, reds);
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) ) {
										return result;
									}
								}
							}
						}
						// done if too few values remain to form another Pair
						if ( n < degree<<1 ) {
							return result;
						}
					}
				}
			}
		}
		return result;
	}

}
