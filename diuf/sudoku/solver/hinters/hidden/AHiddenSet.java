/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hidden;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.utils.Permuter;

/**
 * AHiddenSet partially implements the HiddenSet Sudoku solving technique. It
 * is extended by {@link HiddenPair}, {@link HiddenTriple}, {@link HiddenQuad},
 * and {@link HiddenSetDirect}.
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
 * <pre>
 * KRC 2022-01-22 knownHiddenSets, trying to speed it up.
 * PRE: 69,798,500  21080  3,311  10380    6,724  HiddenPair
 *      99,460,100  17884  5,561   1470   67,659  HiddenTriple
 *     116,183,100  13993  8,302    221  525,715  HiddenQuad
 *     285,441,700
 * PST: 73,284,434  21120  3,469   4345   16,866  HiddenPair
 *     110,932,542  18727  5,923   2637   42,067  HiddenTriple
 *     135,033,064  14327  9,425    247  546,692  HiddenQuad
 *     319,250,040
 * And FMS it is slower, so rerun it to double check before reverting.
 * IDE: 68,531,600  21120  3,244   4345   15,772  HiddenPair
 *     102,018,300  18727  5,447   2637   38,687  HiddenTriple
 *     128,007,300  14327  8,934    247  518,248  HiddenQuad
 *     298,557,200
 * CONCLUSION: faster in IDE, but still slower than PRE java.exe, so REVERT!
 * RVT: 66,667,800  20964  3,180  10135    6,577  HiddenPair
 *      97,782,400  17853  5,477   1437   68,046  HiddenTriple
 *     121,690,400  13986  8,700    212  574,011  HiddenQuad
 *     286,140,600
 *
 * KRC 2023-07-20 Split into HiddenPair, HiddenTriple, HiddenQuad overriding
 * the search method just to read permutations efficiently; and all because
 * there are no macros in Java. An unmaintable mess for efficiency. Sigh.
 * </pre>
 */
public abstract class AHiddenSet extends AHinter {

	// values with 2..$degree possible positions in the region
	protected final int[] candidateValues = new int[REGION_SIZE];
	// shiftedValues with 2..$degree possible positions in the region
	protected final int[] candidateCands = new int[REGION_SIZE];

	// permutationsArray: each possible combination of $degree values in $n
	protected final int[] pa = new int[degree];
	// permuter: iterates every possible combination of $degree values in $n
	protected final Permuter permuter = new Permuter();

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
	 *  So I unwant Naked and Hidden Pent, coz they are basically useless.
	 * </ul>
	 *
	 * @param tech the Tech to implement, see above.
	 */
	protected AHiddenSet(Tech tech) {
		super(tech);
	}

	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// chainer never uses direct mode
		assert !tech.isDirect || !accu.isChaining();
		// if accu.isSingle then it wants oneOnly hint
		final boolean oneHintOnly = accu.isSingle();
		maybes = grid.maybes;
		// presume that no hint will be found
		boolean result = false;
		for ( ARegion region : grid.regions ) { // 9boxs + 9rows + 9cols
			// nb: the search method is also called-by SiameseLocking;
			// so DO NOT change the bastards signature willy-nilly.
			// Its just plain silly for a good bloody reason. It works!
			if ( search(region, accu) ) {
				result = true;
				if(oneHintOnly) break;
			}
		}
		maybes = null;
		return result;
	}

	/**
	 * Search this $region in the $grid for HiddenSets.
	 * <p>
	 * <b>WARNING</b>: Overridden by HiddenPair, HiddenTriple, and HiddenQuad. <br>
	 * DO NOT change this methods signature willy-nilly! <br>
	 * This method is weirdly public. SiameseLocking finds HiddenPairs/Triples
	 * but cannot do all the eliminations itself, so it asks HiddenSet "Is this
	 * a HiddenSet? And if so please do the eliminations for me?", hence
	 * {@link diuf.sudoku.solver.hinters.lock.SiameseLocking#mergeSiameseHints}
	 * calls this search method, which then must be, weirdly, public, because
	 * SiameseLocking is in the "lock" package, coz its not "single". Sigh.
	 * <p>
	 * This search method has all of its variables recreated for every region,
	 * which is a bit slower in the normal use-case, but SiameseLocking shows
	 * the user all available eliminations in one hint, so its worth the cost.
	 * This retards BruteForce, which is NOT ideal! Merely acceptable.
	 * <p>
	 * As of 2023-07-20 This implementation of the search method is used ONLY
	 * by {@link HiddenSetDirect} which is used only in the GUI. My subclasses
	 * HiddenPair, HiddenTriple, and HiddenQuad override search to read the
	 * permutations efficiently. So performance of this implementation does not
	 * really matter any longer. So go nuts!
	 * <p>
	 * This implementation is overridden by HiddenPair/Triple/Quad.
	 *
	 * @param region the ARegion to search
	 * @param accu the implementation of IAccumulator to which I add hints
	 * @return were any hint/s found?
	 */
	public boolean search(final ARegion region, final IAccumulator accu) {
		// presume that no hint will be found
		boolean result = false;
		// if there are sufficient empty cells in this region.
		// we need atleast 3 empty cells in the region for a Hidden Pair
		// to remove any maybes (2 cells in Pair + 1 to remove from)
		if ( region.emptyCellCount > degree ) {
			// the red (removable) potentials
			Pots reds = null;
			// a candidateValue has 2..degree places in this region
			final int[] v = this.candidateValues;
			// a candidateValue has 2..degree places in this region
			final int[] c = this.candidateCands;
			// dereference once per region
			final int[] numPlaces = region.numPlaces;
			// get candidateValues having 2..degree places in this region
			int n = 0; // number of candidateValues
			for ( int value : VALUESES[region.unsetCands] ) {
				if ( numPlaces[value]>1 && numPlaces[value]<degreePlus1 ) {
					c[n] = VSHFT[value];
					v[n++] = value;
				}
			}
			// if there are atleast degree candidateValues
			if ( n >= degree ) {
				// last stackframe til hint (put ALL your s__t here, dopey).
				int cands // bitset of values in the hidden set
				  , spots // bitset of places for this permutation of values in this region
				  , pinkos; // the other maybe to remove
				// ONCE instead of per permutation
				final int[] l = region.places; // bitset indexes by value
				// ------------------------------------------------------------
				// Each perm is an array of degree indexes in candidates array.
				// The perm array is actually thePA, that is repopulated with
				// a distinct set of indexes; so all perms equals all possible
				// combos of degree candidates amongst these $n candidates.
				// nb: DirectHiddenPair is first use of Permutations in each
				// solve, so any bugs there tend to show up here.
				// ------------------------------------------------------------
//<HAMMERED>
				// foreach possible combination of degree candidate values.
				// fastard: 382,336 times. permuter.permute(n, pa) si faster
				// than a new Permutations(n, PA) for every region.
				for ( int[] p : permuter.permute(n, pa) ) { // 2,454,460
					// nb: this switch ~5% faster than a lambda. BFIIK!
					switch ( degree) {
						case 2: spots = l[v[p[0]]] | l[v[p[1]]]; break;
						case 3: spots = l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]]; break;
						case 4: spots = l[v[p[0]]] | l[v[p[1]]] | l[v[p[2]]] | l[v[p[3]]]; break;
						default: throw new IllegalStateException("bad degree="+degree);
					}
					if ( VSIZE[spots] == degree ) { // 24,193
//</HAMMERED>
						// cands is a bitset of the values of the HiddenSet.
						switch ( degree ) {
						case 2: cands = c[p[0]] | c[p[1]]; break;
						case 3: cands = c[p[0]] | c[p[1]] | c[p[2]]; break;
						case 4: cands = c[p[0]] | c[p[1]] | c[p[2]] | c[p[3]]; break;
						default: throw new IllegalStateException("bad degree="+degree);
						}
						// WARN: NOT a tautology in Generate. BFIIK!
						if ( VSIZE[cands] == degree ) { // 24,193
							// foreach cell in HiddenSet with maybes other than
							// HiddenSet values, elim other maybes from cell.
							for ( int index : INDEXES[spots] ) {
								int indice = region.indices[index];
								if ( (pinkos=(maybes[indice] & ~cands)) > 0 ) {
									if(reds==null) reds = new Pots();
									reds.put(indice, pinkos);
								}
							}
							// if any eliminations then create and add hint
							if ( reds != null ) {
								// FOUND HiddenSet!
								// WARN: HiddenSetDirect overides createHint
								//          and it returns null, most often.
								final AHint hint = createHint(region, cands, spots, reds);
								if ( hint != null ) {
									result = true;
									if ( accu.add(hint) ) {
										return result;
									}
								}
								reds = null;
							}
						}
						// done if too few cells for another HiddenSet,
						// even if there was no hint
						if ( n < degreeBy2 ) {
							return result;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Create and return a new AHint, else null meaning none.
	 * <p>
	 * <b>WARN:</b> HiddenSetDirect overrides creatHint, and it returns null
	 * when eliminations do not cause a single, ie almost always.
	 *
	 * @param region the region that we are searching
	 * @param values a bitset of the values in the hidden set
	 * @param indexes a bitset of indexes in r of the cells in the hidden set
	 * @param reds the removable (red) Cell=>Values
	 * @return the new hint, else null meaning none
	 */
	protected AHint createHint(final ARegion region, final int values
			, final int indexes, final Pots reds) {
		return new HiddenSetHint(this, region, values, indexes, reds);
	}

	// CandsDepermuter is a lambda expression that reads a permutation into
	// the combined maybes of cells having this combination of degree values.
	private static interface CandsDepermuter {
		int read(int[] candss, int[] perm);
	}

}
