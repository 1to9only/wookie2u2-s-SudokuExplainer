/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implements the {@link Tech#Abduction} Sudoku solving technique. I find
 * "Cell Abduction" and "Region Abduction" hints in the {@link #TABLES}.
 * <pre>
 * Abduction finds any common consequence/s of every possibility in a complete
 * set, hence one of those possibilites MUST be true, therefore we may safely
 * apply those common conseqence/s. There are two "complete sets":
 * * "Cell Abdution" is every potential value of a cell.
 * * "Region Abduction" is every possible place for a value in a region.
 * * The difference between Abduction and Reduction is that reduction uses the
 *   pre-calculated Table elims, so it's faster. Abduction finds commonalities
 *   in ALL consequences, including Ons, so it's more thorough, but slower coz
 *   it performs the search itself. FYI I was hunting for a name for this
 *   process. I came up with abduction. I hope it doesn't annoy the linguists
 *   too much. I'm not a linguists asshole, and I know it.
 * </pre>
 *
 * @author Keith Corlett 2023-10-30
 */
public class Abduction extends ATableChainer
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln("Adbuction: COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[4];

	/**
	 * The constructor.
	 */
	public Abduction() {
		this(true);
	}

	@Override
	public void setFields(Grid grid) {
		this.grid = grid;
		sizes = grid.sizes;
		maybes = grid.maybes;
		regions = grid.regions;
	}

	@Override
	public void clearFields() {
		grid = null;
		maybes = sizes = null;
		regions = null;
	}

	/**
	 * LogicalSolverBuilder "want" requires ONE public constructor (or a
	 * factory method) that takes no-args or Tech only, hence all "special"
	 * constructors are protected.
	 *
	 * @param useCache
	 */
	protected Abduction(final boolean useCache) {
		super(Tech.Abduction, useCache);
	}

	/**
	 * Find Cell Reduction and Region Reduction hints in the grid and add
	 * them to hints.
	 * <p>
	 * Because my hints are cached I always find ALL available hints, hence
	 * the "rather odd" bitwise-or operator, hence this comment.
	 * <p>
	 * FYI, I find ALL hints coz 90% time spent initialising TABLES before
	 * I start searching for hints, so stopping at the first hint is a waste,
	 * so I find and cache ALL hints, so next time we need not initialise.
	 * Ammortized insanity. Fast and Simples.
	 *
	 * @return any hints found
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean findTableHints() {
		return cellAbduction() | regionAbduction();
	}

	/**
	 * Find any consequences common to every possible value of a Cell.
	 * This is cellReduction, except I find both Ons and Offs, and I am
	 * slower because I search the effects myself, where cellReduction
	 * searches the pre-calc'ed elims, hence finds elims only, quickly.
	 * He's fast. I'm thorough.
	 *
	 * @return any hints found
	 */
	private boolean cellAbduction() {
		// effects common to every possible value of this cell
		final TreeSet<Eff> cmn = new TreeSet<>(); // commonEffects is TLDR
		// presume that no hints will be found
		boolean result = false;
		// VALUESES[maybes[i]], index, size; indice
		int values[], vi, vn, i = 0; // indice
		do {
			if ( sizes[i] > 1 ) {
				NON_LOOP: for(;;) {
				vn = (values=VALUESES[maybes[i]]).length;
				vi = 0; // valuesIndex
				cmn.addAll(effsCached(i, values[vi++]));
				do {
					cmn.retainAll(effsCached(i, values[vi]));
					if ( cmn.isEmpty() )
						break NON_LOOP;
				} while ( ++vi < vn );
				// we get here only when !commonEffects.isEmpty()
				assert !cmn.isEmpty();
				final Pots reds = redPots(grid.maybes, cmn);
				if ( reds != null )
					result |= hints.add(new CellAbductionHint(grid, this, i
							, new TreeSet<>(cmn), reds));
				cmn.clear();
				break;
				}
			}
		} while (++i < GRID_SIZE);
		return result;
	}

	/**
	 * Find any consequences common to every place for value in Region.
	 * This is regionReduction, except I find both Ons and Offs, and I am
	 * slower because I search the effects myself, where regionReduction
	 * searches the pre-calc'ed elims, hence finds elims only, quickly.
	 * He's fast. I'm thorough.
	 *
	 * @return any hints found
	 */
	private boolean regionAbduction() {
		int indices[], rPlaces[], rNumPlaces[], places[], values[]
		  , pn, pi, cands, vn, penultimate, vi, v;
		boolean result = false;
		// effects common to every place for value in this region
		final TreeSet<Eff> cmn = new TreeSet<>();
		for ( ARegion region : regions ) {
			if ( (cands=region.unsetCands) > 0 ) {
				indices = region.indices;
				rPlaces = region.places;
				rNumPlaces = region.numPlaces;
				vn = (values=VALUESES[cands]).length;
				penultimate = vn - 1;
				vi = 0;
				do {
					v = values[vi];
					if ( rNumPlaces[v] > 1 ) {
						NON_LOOP: for(;;) {
						pn = (places=INDEXES[rPlaces[v]]).length;
						pi = 0; // placesIndex
						cmn.addAll(effsCached(indices[places[pi++]], v));
						do {
							cmn.retainAll(effsCached(indices[places[pi]], v));
							if ( cmn.isEmpty()
							  || (pi==penultimate && cmn.size()==1) )
								break NON_LOOP;
						} while ( ++pi < pn );
						// we get here only when !commonEffects.isEmpty()
						assert !cmn.isEmpty();
						final Pots reds = redPots(grid.maybes, cmn);
						if ( reds != null )
							result |= hints.add(new RegionAbductionHint(grid
								, this, region, v, new TreeSet<>(cmn), reds));
						cmn.clear();
						break;
						}
					}
				} while (++vi < vn);
			}
		}
		return result;
	}

	private static Pots redPots(final int[] gridMaybes, final Set<Eff> set) {
		Pots result = null;
		for ( Eff e : set ) {
			if ( !e.isOn && (gridMaybes[e.indice] & VSHFT[e.value]) > 0 ) {
				if(result==null) result = new Pots();
				result.put(e.indice, VSHFT[e.value]);
			}
		}
		return result;
	}

}
