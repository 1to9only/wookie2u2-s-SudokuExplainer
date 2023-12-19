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
import diuf.sudoku.solver.hinters.lock.SiameseLocking;

/**
 * Implementation of the HiddenQuad (degree = 4) Sudoku solving technique.
 * A HiddenQuad is four cells that are the only four places for four values in
 * a region, hence we can remove all other maybes from these four cells.
 * <p>
 * Note that HiddenQuad performance matters not, but my compardrez does.
 * <pre>
 * 2023-07-20 Split HiddenSet into HiddenPair, HiddenTriple, HiddenQuad just
 * for the line that reads the permutation, as a performance measure. One must
 * know the size of the permutation AT COMPILE TIME in order to read it one
 * statement, ergo efficiently, and there are MANY BILLIONS of them.
 * </pre>
 */
public class HiddenQuad extends AHiddenSet
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[12];

	/**
	 * Constructor.
	 */
	public HiddenQuad() {
		super(Tech.HiddenQuad);
	}

	/**
	 * Search this $region in the $grid for HiddenTriples.
	 * <p>
	 * WARN: This method is public, for use in {@link SiameseLocking}, so
	 * do NOT change its signature willy-nilly. This is a bit slower per call
	 * in the "normal" use, but provides SiameseLocking with a solution to an
	 * otherwise ugly problem. Also, every man and his dog overrides it.
	 *
	 * @param r the ARegion to search
	 * @param accu the implementation of IAccumulator to which I add hints
	 * @return were any hint/s found?
	 */
	@Override
	public boolean search(final ARegion r, final IAccumulator accu) {			//   420,269
		boolean result = false;
		if ( r.emptyCellCount > degree ) {										//   349,082
			// "mathematically short" array names to avert TLDR
			final int[] v = this.candidateValues;
			final int[] numPlaces = r.numPlaces;
			int n = 0;
			for ( int value : VALUESES[r.unsetCands] )
				if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
					v[n++] = value;
			if ( n > degreeMinus1 ) {											//   339,507
				final int[] l = r.places; // bitset indexes by value
//<HAMMERED>
				for ( int[] p : permuter.permute(n, pa) ) {						// 3,643,124
					if ( VSIZE[l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]] | l[v[p[3]]]] == degree ) {
//</HAMMERED>																	//    11,765
						final int[] c = this.candidateCands;
						n = 0;
						for ( int value : VALUESES[r.unsetCands] )				//    74,477
							if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 )
								c[n++] = VSHFT[value];							//    74,473
						// WARN: this is NOT a tuatology in Generate!
						if ( VSIZE[c[p[0]] | c[p[1]] | c[p[2]] | c[p[3]]] == degree ) {
							// yep, its a HiddenPair, but any elims?			//    11,765
							final int cands = c[p[0]] | c[p[1]] | c[p[2]] | c[p[3]];
							Pots reds = null;
							boolean any = false;
							final int[] rindices = r.indices;
							final int[] gmaybes = r.getGrid().maybes;
							int indice, pinkos;
							final int spots = l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]] | l[v[p[3]]];
							for ( int ri : INDEXES[spots] ) {					//    47,060
								indice = rindices[ri];							//         1
								if ( (pinkos=(gmaybes[indice] & ~cands)) > 0 ) {
									if(reds==null) reds = new Pots();
									reds.put(indice, pinkos);
									any = true;
								}
							}
							if ( any ) {										//         1
								// FOUND HiddenQuad!
								final AHint hint = createHint(r, cands, spots, reds);
								if ( hint != null ) {							//         1
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							}
						}
						// done if too few values remain to form another Quad
						if(n < degreeBy2) return result;
					}
				}
			}
		}
		return result;
	}

}
