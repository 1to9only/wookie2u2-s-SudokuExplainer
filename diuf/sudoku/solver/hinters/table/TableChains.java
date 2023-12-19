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
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.solver.hinters.IHinter.DUMMY;
import static diuf.sudoku.solver.hinters.table.ATableChainer.TABLES;
import java.util.TreeSet;

/**
 * TableChains wraps Contradictions and Reduction so that both times appear
 * together in the log. They should be used together or neither, so TableChains
 * makes it impossible to do otherwise, without code changes.
 *
 * @author Keith Corlett 2023-11-06
 */
public class TableChains extends ATableChainer {

	public TableChains() {
		this(true);
	}

	protected TableChains(final boolean useCache) {
		super(Tech.TableChain, useCache);
	}

	@Override
	public void setFields(final Grid grid) {
		this.grid = grid;
		maybes = grid.maybes;
		sizes = grid.sizes;
		regions = grid.regions;
	}

	@Override
	public void clearFields() {
		grid = null;
		maybes = null;
		sizes = null;
		regions = null;
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
	@Override
	public boolean findTableHints() {
		// reduction results are nondeterministic on sets with contradictions
		return contradictions()
			|| (cellReduction() | regionReduction());
	}

	private boolean contradictions() {
		boolean result = false;
		if ( !TABLES.contradictions.isEmpty() ) {
			Contradiction c;
			final TreeSet<Contradiction> contras = TABLES.contradictions;
			while ( (c=contras.pollFirst()) != null )
				// if the initial assumption still exists then add a hint
				// fyi: initOn is my eliminations (it's been proven false)
				if ( (maybes[c.initOn.indice] & VSHFT[c.initOn.value] ) > 0 )
					result |= hints.add(new ContradictionHint(grid, this
							, c.initOn, c.indice, c.value));
		}
		return result;
	}

	/**
	 * Find any eliminations common to setting Cell to every one of its maybes.
	 * The cell MUST be one of these values, hence the common eliminations, if
	 * any, are proven.
	 * <pre>
	 * for each unset cell in grid
	 *    foreach e (elim value) in cell.maybes
	 *       foreach v (value sought) in cell.maybes
	 *          seek: common $e elims of every $indice+$v
	 * </pre>
	 *
	 * @return any hints found
	 */
	private boolean cellReduction() {
		long elims[], vcM0, vcM1;
		int values[], j, size;
		// presume that no hints will be found
		boolean result = false;
		// all possible initial ON assumptions
		final Eff[][] ons = TABLES.ons;
		// foreach unset cell in the grid
		int i = 0;
		do {
			if ( (size=sizes[i]) > 1 ) {
				// v is the VALUE_SOUGHT (value of the initial Assumption).
				// e is the ELIM_VALUE (value eliminated by every possible $v).
				// They aren't the same value! Tables designed for Fish are a
				// kludge here, but I can't imagine a design that's faster to
				// create, which takes ~5 seconds, where querying takes ~30ms,
				// hence I just put-up with the kludge, coz Fish-based Tables
				// are fast-enough here anyway.
				values = VALUESES[maybes[i]];
				int e = 1;
				ELIM_VALUE: do {
					// for first valueSought in cell.maybes
					j = 0;
					elims = ons[values[j++]][i].elims[e];
					if ( ( (vcM0=elims[0])
					     | (vcM1=elims[1]) ) > 0L ) {
						// foreach subsequent valueSought in cell.maybes
						do
							elims = ons[values[j]][i].elims[e];
							// reduce victims to those common to every value
						while ( ((vcM0&=elims[0])|(vcM1&=elims[1])) > 0L
							   && ++j < size );
						// if any victims survived
						if ( (vcM0|vcM1) > 0L ) {
							// empty reds if none of the victims still exist;
							// happens coz the effs are cached (dirty data).
							final Pots reds = new Pots(vcM0,(int)vcM1, grid.maybes, VSHFT[e], DUMMY);
							if ( reds.any() ) {
								// FOUND a CellReduction!
								// nb: hints.add returns was it added (not isSingle)
								result |= hints.add(new CellReductionHint(grid
									, this, i, vcM0, (int)vcM1, e, reds));
							}
						}
					}
				} while (++e<VALUE_CEILING);
			}
		} while (++i < GRID_SIZE);
		return result;
	}

	/**
	 * Find any eliminations common to setting every possible place for value
	 * in this region to value. One of these cells MUST be this value, hence
	 * the common eliminations, if any, are proven.
	 * <pre>
	 * foreach region grid
	 *    foreach e (elim value) in unplaced values
	 *       foreach v (value sought) in unplaced values
	 *          foreach place in region.places[v]
	 *             seek: common $e elims of every $place+$v
	 * </pre>
	 *
	 * @return any hints found
	 */
	private boolean regionReduction() {
		long elims[], vcM0, vcM1;
		int places[], indexes[], indices[], values[], p;
		// presume that no hints will be found
		boolean result = false;
		// all possible initial ON assumptions
		final Eff[][] ons = TABLES.ons;
		// foreach region in the grid
		for ( ARegion region : regions ) {
			places = region.places;
			indices = region.indices;
			// each unsetCands has 2+ places in rgn, else HiddenSingle sets it.
			// WARN: Shft-F5 breaks this rule, hence j-loop is pretested.
			values = VALUESES[region.unsetCands];

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// This is a kludge: we seek offs that are common consequences of
			// EVERY possible place for a value in this region, but the value
			// eliminated (e) need not be the same as the value sought (v),
			// hence we need two values, making this process nine-times slower,
			// which is still fast-enough, but NOT ideal. Ideally we would have
			// a Set of both OFFS and ONS from each ON assumption (regardless
			// of e) to retainAll, but Tables were designed for Fish! So I'm a
			// kludge, but I'm also fast enough. The slow part is initialising
			// the tables, not querying them, so I retain it coz I'm too stupid
			// to imagine a Tables format that's faster to create. Basically,
			// Fish-based tables work well-enough for all. Hence I just put-up
			// with this kludge. It's fast enough anyway.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// I retain e coz it's only ~10% slower, and is more accurate.
			// I know not why it's only ~10% slower: O(n*n) verses O(n).
			// with e: 192,972,600  4208  45,858  1404  137,444  Reduction
			// sans e: 114,902,800  3930  29,237  1008  113,990  Reduction
			// 192,972,600 - 114,902,800 = 78,069,800 ns
			// 1404 - 1008               = 396 elims
			// 78,069,800 / 396          = 197,146 ns/elim
			// Half my hinters are slower. The worste, Jellyfish, is *90.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			// foreach possible 'elim value'
			int e = 1;
			ELIM_VALUE: do {
				// foreach 'value sought' remaining to be placed in this region
				VALUE_SOUGHT: for ( int v : values ) {
					// first place for 'value sought' in region
					indexes = INDEXES[places[v]];
					elims = ons[v][indices[indexes[p=0]]].elims[e];
					vcM0 = elims[0];
					vcM1 = elims[1];
					// foreach subsequent place for 'value sought' in region
					while ( ++p < indexes.length ) {
						elims = ons[v][indices[indexes[p]]].elims[e];
						// reduce victims to those common to EVERY possible place
						vcM0 &= elims[0];
						vcM1 &= elims[1];
					}
					// if any victims survived reduction
					if ( (vcM0|vcM1) > 0L ) {
						// empty reds if none of the victims still exist;
						// happens coz the effs are cached (dirty data).
						final Pots reds = new Pots(vcM0,(int)vcM1, maybes, VSHFT[e], DUMMY);
						if ( reds.any() ) {
							// FOUND a RegionReduction!
							// nb: hints.add returns was it added (not isSingle)
							result |= hints.add(new RegionReductionHint(grid, this
								, region, v, vcM0,(int)vcM1, e, reds));
						}
					}
				}
			} while (++e<VALUE_CEILING);
		}
		return result;
	}

}
