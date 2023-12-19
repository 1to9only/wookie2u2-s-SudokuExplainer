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

/**
 * HiddenTriple implements the {@link Tech#HiddenTriple} Sudoku solving
 * technique. A HiddenTriple is three cells that are the only place for three
 * values in a region, hence we can remove all other maybes from these three
 * cells.
 * <p>
 * Note that HiddenTriple performance REALLY matters coz its used BruteForce,
 * and in DynamicPlus+ chainers.
 * <pre>
 * 2023-07-20 Split HiddenSet into HiddenPair, HiddenTriple, HiddenQuad just
 * for the line that reads the permutation, as a performance measure. One must
 * know the size of the permutation AT COMPILE TIME in order to read it one
 * statement, ergo efficiently, and there are MANY BILLIONS of them.
 * </pre>
 */
public class HiddenTriple extends AHiddenSet {

	/**
	 * Constructor.
	 */
	public HiddenTriple() {
		super(Tech.HiddenTriple);
	}

	/**
	 * Search the given $region for HiddenTriples.
	 * <p>
	 * This method is (unusually) public, for use by SiameseLocking.
	 * It overrides the default implementation in AHiddenSet.
	 * HiddenTriple is NOT used in BruteForce.
	 *
	 * @param region the ARegion to search
	 * @param accu to which I add hints
	 * @return were any hint/s found?
	 */
	@Override
	public boolean search(final ARegion region, final IAccumulator accu) {
		boolean result = false;
		if ( region.emptyCellCount > degree ) {
			final int[] v = this.candidateValues; // plain values (for places)
			final int[] numPlaces = region.numPlaces;
			int n = 0;
			for ( int value : VALUESES[region.unsetCands] )
				if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
					v[n++] = value;
			if ( n > degreeMinus1 ) {
				// bitset of region indexes, by value
				final int[] l = region.places;
//<HAMMERED>
				for ( int[] p : permuter.permute(n, pa) ) {
					if ( VSIZE[l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]]] == degree ) {
//</HAMMERED>
						final int[] c = this.candidateCands; // bitset values
						n = 0;
						for ( int value : VALUESES[region.unsetCands] )
							if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
								c[n++] = VSHFT[value];
						// WARN: NOT a tuatology in Generate. BFIIK!
						if ( VSIZE[c[p[0]] | c[p[1]] | c[p[2]]] == degree ) {
							// yep, its a HiddenPair, but any elims?
							final int cands = c[p[0]] | c[p[1]] | c[p[2]];
							final int[] rindices = region.indices;
							final int[] gmaybes = region.getGrid().maybes;
							final int spots = l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]];
							boolean any = false;
							int indice; // indice of this cell in this region
							int pinkos; // bitset of candidates to remove
							Pots reds = null;
							for ( int index : INDEXES[spots] ) {
								indice = rindices[index];
								if ( (pinkos=gmaybes[indice] & ~cands) > 0 ) { // 9bits
									if(reds==null) reds = new Pots();
									reds.put(indice, pinkos);
									any = true;
								}
							}
							if ( any ) {
								// FOUND HiddenTriple!
								final AHint hint = createHint(region, cands, spots, reds);
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							}
						}
						// done if too few values remain to form another Set
						if ( n < degreeBy2 )
							return result;
					}
				}
			}
		}
		return result;
	}

}
