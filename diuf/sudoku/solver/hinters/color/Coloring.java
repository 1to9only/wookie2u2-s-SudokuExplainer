/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * Coloring started life as a copy-paste of the ColoringSolver from HoDoKu by
 * Bernhard Hobiger, aka hobiwan. It has been converted to Sudoku Explainers
 * model, with some small efficiency improvements; but this is still basically
 * hobiwans code.
 *
 * Hobiwans standard licence statement is reproduced below.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Sudoku Explainer is released under more or less the same conditions. Use it.
 * Change it. But any problems are not my problem. Of course I care, because I
 * really want it to be useful, but I can not offer any warranty, because doing
 * so takes all the fun out of it.
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.color.ColoringHint.Subtype;
import java.util.Arrays;

/**
 * The Coloring class implements both Simple-Coloring and Multi-Coloring Sudoku
 * solving techniques.
 * <p>
 * Simple-Coloring involves just two sets of of bi-position candidates: regions
 * with just two positions for v are chained together into a forcing loop; so
 * that either set1 (color-1) contains v or set2 (color-2) contains v, leading
 * to the two simple coloring rules:<ul>
 * <li>color trap (common): cells which can see both colors may not be v; and
 * <li>color wrap (rarer): if two cells of the same color see each other then
 *  all v's of that color can be removed.
 * </ul>
 * <p>
 * Multi-Coloring involves multiple (more-than-two) coloring-sets that are
 * organised into parities to produce reduced eliminations.
 * <p>
 * KRC 2021-05-22 I just looked at splitting Tech.Coloring into ColoringSimple
 * and ColoringMulti, because XColoring finds more elims for a simple coloring.
 * It's a bust because LogicalSolver.configureHinters and the TechSelectDialog
 * get messy fast, so it's better to leave it as it is, with XColoring finding
 * JUST the additional elims after findSimpleColoring has run.
 *
 * @author Keith Corlett 2020 Sept 21
 */
public final class Coloring extends AHinter {

	/** First color of a coloring pair. Index in {@link #colors} */
	private static final int C1 = 0;
	/** Second color of a coloring pair. Index in {@link #colors} */
	private static final int C2 = 1;
	/** Maximum number of color pairs. */
	private static final int MAX_COLOR = 20;

	/**
	 * indices of colored cells for each candidate.
	 * colors[v] contains the color pairs for candidate v in 1..9.
	 * colors[v][i] contains a color pair for candidate v, i is a color-set
	 *  index 0..MAX_COLOR-1; the max i+1 for this v is in numColorPairs[v].
	 * colors[v][i][C1] contains indices of cells which are color-1 (blue).
	 * colors[v][i][C2] contains indices of cells which are color-2 (green).
	 */
	private final Idx[][][] colors = new Idx[VALUE_CEILING][MAX_COLOR][2];
	/** Number of color pairs for each candidate. */
	private final int[] numColorPairs = new int[VALUE_CEILING];
	/** Step number of the sudoku for which coloring was calculated. -1 means
	 * "data invalid". */
	private final int[] hintNumbers = new int[VALUE_CEILING];

	/** contains all candidates, that are part of at least one conjugate pair. */
	private final Idx startSet = new Idx();
	/** Idx used by the conjugate method only; contains the indices of the two
	 * cells in the current ARegion which maybe the candidate value. */
	private final Idx conjugateSet = new Idx();

	/** This candidate: the one we're currently processing. This is a field
	 * just to reduce the amount of stack-work: the handbrake of recursion. */
	private int candidate;
	/** theseSets = colors[candidate][anzColorPair].
	 * This variable exists just to save repeated array-look-ups into the colors
	 * array-of-arrays-of-arrays-of-SudokuSets, now it's just a look-up into an
	 * array-of-SudokuSets (Are you confused yet? Good, coz I sure am. Think
	 * about it.) */
	private Idx[] colorSets;

	/** The grid to search. */
	private Grid grid;
	/** The idxs of the grid to search. */
	private Idx[] idxs;
	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	public Coloring() {
		super(Tech.Coloring);
		// create the coloring sets
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			hintNumbers[v] = -1;
			numColorPairs[v] = 0;
			for ( int j=0; j<MAX_COLOR; ++j ) {
				colors[v][j][C1] = new Idx();
				colors[v][j][C2] = new Idx();
			}
		}
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.grid = grid;
		this.idxs = grid.idxs; // cached indices which maybe each value
		this.accu = accu;

		// clear the cache each time I'm called coz I'm either on a new grid,
		// or the grid has changed since the last time I was called; else we
		// hit a bug only in 160 in LogicalSolverTester top1465.d5.mt
		Arrays.fill(hintNumbers, -1);
		Arrays.fill(numColorPairs, 0);

		boolean result;
		try {
			if ( accu.isSingle() ) {
				result = findSimpleColors() || findMultiColors();
			} else {
				result = findSimpleColors() | findMultiColors();
			}
		} finally {
			this.grid = null;
			this.idxs = null;
			this.accu = null;
			Cells.cleanCasA();
		}
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Simple-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @param grid the Grid to search
	 * @param idxs pass me grid.idxs which you need get only ONCE.
	 * @param accu the IAccumulator to which I add any hints
	 * @return true if any hint/s were found, else false
	 */
	private boolean findSimpleColors() {
		Idx c1, c2; // indices of cells in the two colors
		AHint hint;
		int v, numColors, i;
		boolean any;
		final Pots reds = this.redPots;
		// presume that no hints will be found
		boolean result = false;
		reds.clear();
//find the caching problem with 160#top1465.d5.mt
//if ( grid.source.lineNumber==160 && AHint.hintNumber==20 )
//	Debug.breakpoint();
		// foreach value: foreach color
		for ( v=1; v<VALUE_CEILING; ++v ) {
			// find all Simple Colors steps for v.
			numColors = doColoring(v);
			// now check for eliminations
			for ( i=0; i<numColors; ++i ) {
				c1 = colors[v][i][C1];
				c2 = colors[v][i][C2];
				// color wrap (rare): if two cells with the same color see each
				// other, then we can remove all candidates of that color.
				any = false;
				if ( checkColorWrap(c1) ) {
					any = reds.addAll(c1.cellsA(grid), VSHFT[v]);
				}
				if ( checkColorWrap(c2) ) {
					any |= reds.addAll(c2.cellsA(grid), VSHFT[v]);
				}
				if ( any ) {
					// build the hint and add it to accu
					hint = new ColoringHint(this, Subtype.SimpleColorWrap
							, potsArray(v, c1, c2), v, reds);
					result = true;
					if ( accu.add(hint) ) {
						return result;
					}
				} else {
					// color trap (more common): any candidate that sees two
					// cells of opposite colors can be removed.
					if ( eliminate(c1, c2, v, reds) ) {
						// nb: creating the hint clears the existing redPots!
						hint = new ColoringHint(this, Subtype.SimpleColorTrap
								, potsArray(v, c1, c2), v, reds);
						result = true;
						if ( accu.add(hint) ) {
							return result;
						}
					}
				}
			}
		}
		return result;
	}
	private final Pots redPots = new Pots();

	/**
	 * If any two cells in idx see each other then all candidates
	 * in set may be removed.
	 *
	 * @param idx The Idx to examine
	 */
	private boolean checkColorWrap(Idx idx) {
		Idx buds;
		int i, j;
		final int n=idx.size(), m=n-1;
		final int[] indices = idx.toArrayA(); // get array ONCE
		// foreach indice in set (except the last)
		for ( i=0; i<m; ++i ) {
			buds = Grid.BUDDIES[indices[i]];
			// foreach subsequent indice in set
			for ( j=i+1; j<n; ++j ) {
				if ( buds.has(indices[j]) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * eliminate checks a candidate to be deleted: if any idx[v] sees
	 * a cell in set1, and a cell in set2, then it can be eliminated.
	 * <p>
	 * 20090414: Duplicate eliminations causes wrong sorting; so first
	 * collect all eliminations in a set.
	 * <p>
	 * Note that eliminate is used in both simple and multi-color searches.
	 *
	 * @param a The first set to check
	 * @param b The second set to check
	 * @param v the candidate value 1..9 that we seek
	 * @param reds a non-null Pots to which I add eliminations, if any.
	 * @return true if any eliminated were found, else false
	 */
	private boolean eliminate(final Idx a, final Idx b, final int v, final Pots reds) {
		final Idx cmnBuds = this.cmnBuds.clear();
		final Idx[] buds = Grid.BUDDIES;
		final Idx vs = idxs[v];
		// foreach a foreach b: cmnBuds += buds[ai] & buds[bi] & idxs[v]
		a.forEach((ai)->b.forEach((bi)->cmnBuds.orAnd(buds[ai], buds[bi], vs)));
		if ( cmnBuds.none() ) {
			return false;
		}
		reds.upsertAll(cmnBuds, grid, v);
		return true;
	}
	private final Idx cmnBuds = new Idx();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ DO COLORING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * doColoring actually colors the grid. "Coloring the grid" means that each
	 * candidate that is part of at least one conjugate pair is assigned a
	 * color. "Assigned a color" means that the candidate is added to one of
	 * the {@link #colors}.
	 * <p>
	 * The algorithm is pretty straight-forward:<ul>
	 * <li>first eliminate all candidates, that are not part of at least one
	 *  conjugate pair
	 * <li>for each uncolored candidate remaining: do the coloring
	 * </ul>
	 * <p>
	 * Colored candidates are removed from {@link #startSet}.
	 * <p>
	 * Coloring sets are cached: If a set has already been calculated for a
	 * given candidate and a certain state of a Sudoku then this method just
	 * returns the previously calculated number of color pairs, it does not
	 * repeat the calculations, for efficiency.
	 *
	 * @param v the candidate value in 1..9
	 * @return the number of color pairs for the given cand
	 */
	private int doColoring(final int v) {
		// check the cache
		if ( hintNumbers[v] == grid.hintNumber ) {
			// sudoku has not changed since last calculation
			return numColorPairs[v];
		}
		// reset everything
		numColorPairs[v] = 0;
		// candidateSet is an Idx of grid.cells which maybe cand
		// nb: candidateSet is used again down in conjugate.
		candidateSet = idxs[v];
		// first: add all cells that may be part of a conjugate pair
		startSet.clear(); // we start from an empty set, then
		// add indice of each cell in a region with 2 places for cand
		candidateSet.forEach((i) -> {
			for ( ARegion r : grid.cells[i].regions ) {
				if ( r.ridx[v].size == 2 ) {
					startSet.add(i);
					break; // first region only
				}
			}
		});
		// now do the coloring.
		int indice;
		while ( startSet.any() ) {
			// get the first (ie the next) indice
			indice = startSet.peek();
			// pre-set the colors-fields for the new color
			candidate = v;
			colorSets = colors[v][numColorPairs[v]];
			colorSets[C1].clear();
			colorSets[C2].clear();
			// recursively search indice/candidate for coloring pairs,
			// adding each candidate to it's appropriate colorSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// (if you say s__t three times c__ts actually listen)
			// so this loop does stop, despite having no VISIBLE stopper.
			recurse(indice, true); // start with C1, then C2, C1, C2...
			// a colorChain consists of atleast two cells (one on, one off)
			// HiddenSingles are discarded
			if ( colorSets[C1].none() || colorSets[C2].none() ) {
				colorSets[C1].clear();
				colorSets[C2].clear();
			} else {
				++numColorPairs[v];
			}
		}
		hintNumbers[v] = grid.hintNumber;
		return numColorPairs[v];
	}
	private Idx candidateSet;

	/**
	 * Recursively colors the candidate/cell index with any conjugate pairs in
	 * the three Regions which contain the cell at index.
	 * <p>
	 * Weird science: This method removes each index from the startSet field,
	 * which stops my callers containing loop. This method adds each indice to
	 * the colorSets array: (if on C1 else C2); to split the candidates into
	 * two color-sets.
	 * <p>
	 * Note that the conjugate method also reads the preset fields candidate
	 * and free, so these must be set before you call me:<ul>
	 * <li>The candidate field is just the value 1..9 of the candidate we seek
	 * <li>The free field is preset to sudoku.getFree()
	 * </ul>
	 * Why: Recursion is faster with a smaller stack-frame, so ONLY pass-down
	 * values that change during recursion; everything else is a field. In this
	 * case we also save on calling getter methods repeatedly. Every stackframe
	 * costs us something, so minimising them is optimal, which is at odds with
	 * an OO-design mind-set, that's all. I just prefer speed.
	 *
	 * @param indice the indice of the cell to color; -1 to stop coloring.
	 * @param on true for {@link #C1} (green), false for {@link #C2} (blue)
	 */
	private void recurse(int indice, boolean on) {
		// give-up if there's no conjugate, or we've already done this index
		if ( indice==-1 || !startSet.has(indice) ) {
			return;
		}
		// record the color of this index
		if ( on ) {
			colorSets[C1].add(indice);
		} else {
			colorSets[C2].add(indice);
		}
		// remove this index from the start set
		startSet.remove(indice);
		// recursion: a depth first search (DFS)
		recurse(conjugate(indice, grid.cells[indice].row), !on);
		recurse(conjugate(indice, grid.cells[indice].col), !on);
		recurse(conjugate(indice, grid.cells[indice].box), !on);
	}

	/**
	 * Checks whether cell {@code indice} belongs to a conjugate pair on value
	 * {@code candidate} in ARegion {@code r}. Returns the indice of the other
	 * cell which maybe candidate in region, or -1 meaning no conjugate pair.
	 * <p>
	 * A conjugate is the other place for value in region: the region has just
	 * two places for value, here's one, what's the other one. We call both
	 * cells a conjugate pair.
	 * <p>
	 * This method reads the candidate field; and
	 * sets/reads conjugateSet, not referenced externally.
	 * <p>
	 * This method is the ONLY use of the conjugateSet field, which is a field
	 * to avoid repeatedly creating an Idx instance, and all stack-work. It's
	 * faster to not have to create a new stack-frame when invoking a method.
	 * <p>
	 * We poll()-off the first one or two indices in conjugateSet, because poll
	 * is faster than get(i), because poll doesn't initialise the conjugateSet.
	 *
	 * @param indice grid.cells indice of cell for which a conjugate is sought
	 * @param r the ARegion to look in
	 * @return An index, if the house has only one cell left, or -1
	 */
	private int conjugate(int indice, ARegion r) {
		if ( r.ridx[candidate].size != 2 ) {
			return -1; // no conjugate pair, so stop coloring.
		}
		// return the indice of the other cell in this conjugate pair
		conjugateSet.setAnd(candidateSet, r.idx);
		int result = conjugateSet.poll();
		if ( result == indice ) {
			result = conjugateSet.poll();
		}
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ MULTI COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Multi-Coloring hints in the grid and adds them to accu.
	 *
	 * @return true if hint/s were found, else false
	 */
	private boolean findMultiColors() {
		Idx[][] vSets;
		Idx a1,a2, b1,b2;
		AHint hint;
		int v, numColors, i, j;
		final Pots reds = this.redPots;
		boolean any;
		// presume the no hints will be found
		boolean result = false;
		reds.clear();
		// foreach value: foreach color
		// NOTE: a->b != b->a, so check ALL combos, not just forward-only.
		for ( v=1; v<VALUE_CEILING; ++v ) {
			numColors = doColoring(v);
			vSets = colors[v];
			A_LOOP: for ( i=0; i<numColors; ++i ) {
				if ( (a1=vSets[i][C1]).any() && (a2=vSets[i][C2]).any() ) {
					B_LOOP: for ( j=0; j<numColors; ++j ) {
						if ( j != i
						  && (b1=vSets[j][C1]).any()
						  && (b2=vSets[j][C2]).any()
						) {
							any = false; // nb: redPots is cleared by hint-creation
							// first see if cells of one-color see cells of the
							// other color, eliminating all v's of that color.
							if ( isMultiColor1(a1, b1, b2) ) {
								any = reds.addAll(a1.cellsA(grid), VSHFT[v]);
							}
							if ( isMultiColor1(a2, b1, b2) ) {
								any |= reds.addAll(a2.cellsA(grid), VSHFT[v]);
							}
							if ( any ) {
								hint = new ColoringHint(this, Subtype.MultiColor1
										, potsArray(v, a1, a2, b1, b2), v, reds);
								result = true;
								if ( accu.add(hint) ) {
									return result;
								}
							} else {
								// now see if two cells of different color-sets
								// see each other. If so, then all v's seeing
								// cells of opposite colors are eliminated
								if ( isMultiColor2(a1, b1) ) {
									any = eliminate(a2, b2, v, reds);
								}
								if ( isMultiColor2(a1, b2) ) {
									any |= eliminate(a2, b1, v, reds);
								}
								if ( isMultiColor2(a2, b1) ) {
									any |= eliminate(a1, b2, v, reds);
								}
								if ( isMultiColor2(a2, b2) ) {
									any |= eliminate(a1, b1, v, reds);
								}
								if ( any ) {
									hint = new ColoringHint(this, Subtype.MultiColor2
										   , potsArray(v, a1, a2, b1, b2), v, reds);
									result = true;
									if ( accu.add(hint) ) {
										return result;
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns can cells in set see cells in both a and b?
	 * If so, then the whole set can be eliminated.
	 *
	 * @param set Set to be checked
	 * @param a First color of other color pair
	 * @param b Second color of other color pair
	 * @return can cells in set see cells in both a and b?
	 */
	private boolean isMultiColor1(final Idx set, final Idx a, final Idx b) {
		// NOTE: none of the sets are empty here
		assert !set.none() && !a.none() && !b.none();
		// buds no fit on line
		Idx x;
		// shorthands and speed
		final int a0=a.a0, a1=a.a1, a2=a.a2
				, b0=b.a0, b1=b.a1, b2=b.a2;
		// see if sets-buds intersect both a and b
		boolean seeA=false, seeB=false;
		for ( int i : set.toArrayA() ) {
			x = Grid.BUDDIES[i];
			seeA |= (x.a0 & a0)!=0 || (x.a1 & a1)!=0 || (x.a2 & a2)!=0;
			seeB |= (x.a0 & b0)!=0 || (x.a1 & b1)!=0 || (x.a2 & b2)!=0;
			if ( seeA && seeB ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Can a cell in Set 'a' see any cell in Set 'b'?
	 * If so, then 'a' can be eliminated.
	 *
	 * @param a First set
	 * @param b Second set
	 * @return
	 */
	private boolean isMultiColor2(final Idx a, final Idx b) {
		if ( a.none() || b.none() ) {
			return false;
		}
		final int n = a.size();
		final int[] indices = a.toArrayA();
		for ( int i=0; i<n; ++i ) {
			if ( Grid.BUDDIES[indices[i]].intersects(b) ) {
				return true;
			}
		}
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ HELPERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Create an array of Pots from cells in the given idxs => valueToRemove.
	 *
	 * @param valueToRemove the value to be eliminated from idxs
	 * @param Idx... idxs a param-array of the idx's to add
	 * @return a new array of Pots
	 */
	private Pots[] potsArray(final int valueToRemove, final Idx... idxs) {
		final int n = idxs.length;
		final Pots[] result = new Pots[n];
		for ( int i=0; i<n; ++i ) {
			result[i] = new Pots(idxs[i].cellsA(grid), valueToRemove);
		}
		return result;
	}

}
