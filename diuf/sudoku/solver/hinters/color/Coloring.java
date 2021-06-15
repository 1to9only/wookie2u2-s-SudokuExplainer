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

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.color.ColoringHint.Subtype;
import java.util.Arrays;

/**
 * The Coloring class implements both the Simple and Multi Coloring Sudoku
 * solving techniques.
 * <p>
 * Simple Coloring involves chains of bi-position candidates. Regions with just
 * two positions for candidate v are chained together into a forcing loop; so
 * that either set1 (color-1) contains v or set2 (color-2) contains v, leading
 * to the two simple coloring rules:<ul>
 * <li>color trap (common): cells which can see both colors may not be v; and
 * <li>color wrap (rarer): if two cells of the same color see each other then
 *  all v's of that color can be removed.
 * </ul>
 * <p>
 * Multi Coloring is much more complex and I don't pretend to understand it.
 * It involves multiple (more-than-two) sets of cells (colors).
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
	private final Idx[][][] colors = new Idx[10][MAX_COLOR][2];
	/** Number of color pairs for each candidate. */
	private final int[] numColorPairs = new int[10];
	/** Step number of the sudoku for which coloring was calculated. -1 means
	 * "data invalid". */
	private final int[] hintNumbers = new int[10];

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
	/** The getIdxs() of the grid to search. */
	private Idx[] idxs;
	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	public Coloring() {
		super(Tech.Coloring);
		// create the coloring sets
		for ( int v=1; v<10; ++v ) {
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
		this.idxs = grid.getIdxs(); // get indices which maybe each value
		this.accu = accu;

		// clear the cache each time I'm called coz I'm either on a new grid,
		// or the grid has changed since the last time I was called; else we
		// hit a bug only in 160 in LogicalSolverTester top1465.d5.mt
		Arrays.fill(hintNumbers, -1);
		Arrays.fill(numColorPairs, 0);

		boolean result;
		try {
			if ( accu.isSingle() )
				result = findSimpleColors() || findMultiColors();
			else
				result = findSimpleColors() | findMultiColors();
		} finally {
			this.grid = null;
			this.idxs = null;
			this.accu = null;
		}
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Simple-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @param grid the Grid to search
	 * @param idxs pass me grid.getIdxs() which you need get only ONCE.
	 * @param accu the IAccumulator to which I add any hints
	 * @return true if any hint/s were found, else false
	 */
	private boolean findSimpleColors() {
		Idx c1, c2; // indices of cells in the two colors
		AHint hint;
		int v, numColors, i;
		boolean any;
		Pots redPots = this.redPots;
		redPots.clear();
		// presume that no hints will be found
		boolean result = false;
//find the caching problem with 160#top1465.d5.mt
//if ( grid.source.lineNumber==160 && AHint.hintNumber==20 )
//	Debug.breakpoint();
		// foreach value: foreach color
		for ( v=1; v<10; ++v ) {
			// find all Simple Colors steps for v.
			numColors = doColoring(v);
			// now check for eliminations
			for ( i=0; i<numColors; ++i ) {
				c1 = colors[v][i][C1];
				c2 = colors[v][i][C2];
				// color wrap (rare): if two cells with the same color see each
				// other, then we can remove all candidates of that color.
				any = false;
				if ( checkColorWrap(c1) )
					any = redPots.addAll(c1.cells(grid), new Values(v));
				if ( checkColorWrap(c2) )
					any |= redPots.addAll(c2.cells(grid), new Values(v));
				if ( any ) {
					// build the hint and add it to accu
					hint = new ColoringHint(this, Subtype.SimpleColorWrap
							, potsArray(v, c1, c2), v, redPots);
					result = true;
					if ( accu.add(hint) )
						return result;
				} else {
					// color trap (more common): any candidate that sees two
					// cells of opposite colors can be removed.
					if ( checkCandidateToDelete(c1, c2, v, redPots) ) {
						// nb: creating the hint clears the existing redPots!
						hint = new ColoringHint(this, Subtype.SimpleColorTrap
								, potsArray(v, c1, c2), v, redPots);
						result = true;
						if ( accu.add(hint) )
							return result;
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
		Idx budsIdx;
		int i, j;
		final int n=idx.size(), m=n-1;
		final int[] indices = idx.toArrayA(); // get array ONCE
		// foreach indice in set (except the last)
		for ( i=0; i<m; ++i ) {
			budsIdx = Grid.BUDDIES[indices[i]];
			// foreach subsequent indice in set
			for ( j=i+1; j<n; ++j )
				if ( budsIdx.contains(indices[j]) )
					return true;
		}
		return false;
	}

	/**
	 * checkCandidateToDelete: if any idx[v] sees a cell in set1 and
	 * a cell in set2, then they can be eliminated.
	 * <p>
	 * 20090414: Duplicate eliminations causes wrong sorting; so first
	 * collect all eliminations in a set.
	 * <p>
	 * Note that checkCandidateToDelete is used in both simple and
	 * multi-color searches.
	 *
	 * @param a The first set to check
	 * @param b The second set to check
	 * @param v the candidate value 1..9 that we seek
	 * @param redPots a non-null Pots to which I add eliminations, if any.
	 * @return true if any eliminated were found, else false
	 */
	private boolean checkCandidateToDelete(Idx a, Idx b, int v, Pots redPots) {
		final Idx cmnBuds = this.cmnBuds.clear();
		final Idx[] buds = Grid.BUDDIES;
		final Idx vs = idxs[v];
		// for each a, b: cmnBuds += grid.buds[a] & grid.buds[b] & idxs[v]
		a.forEach((ai)->b.forEach((bi)->cmnBuds.orAnd(buds[ai], buds[bi], vs)));
		if ( cmnBuds.none() )
			return false;
		cmnBuds.forEach(grid.cells, (c)->redPots.put(c, new Values(v)));
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
	 * @param cand the given candidate value 1..9
	 * @return the number of color pairs for the given cand
	 */
	private int doColoring(final int cand) {
		// check the cache
		if ( hintNumbers[cand] == AHint.hintNumber )
			// sudoku has not changed since last calculation
			return numColorPairs[cand];
		// reset everything
		numColorPairs[cand] = 0;
		hintNumbers[cand] = AHint.hintNumber;
		// candidateSet is an Idx of grid.cells which maybe cand
		// nb: candidateSet is used again down in conjugate.
		candidateSet = idxs[cand];
		// first: add all cells that may be part of a conjugate pair
		startSet.clear(); // we start from an empty set, then
		// add indice of each cell in a region with 2 places for cand
		candidateSet.forEach((i) -> {
			for ( ARegion r : grid.cells[i].regions )
				if ( r.indexesOf[cand].size == 2 ) {
					startSet.add(i);
					break;
				}
		});
		// now do the coloring.
		int indice;
		while ( startSet.any() ) {
			// get the first (ie the next) indice
			indice = startSet.peek();
			// pre-set the colors-fields for the new color
			candidate = cand;
			colorSets = colors[cand][numColorPairs[cand]];
			colorSets[C1].clear();
			colorSets[C2].clear();
			// recursively search indice/candidate for coloring pairs,
			// adding each candidate to it's appropriate colorSet.
			// NOTE that recurse removes each indice from startSet,
			// NOTE that recurse removes each indice from startSet,
			// NOTE that recurse removes each indice from startSet,
			// (if you say s__t three times c__ts actually listen)
			// so this loop does stop, despite having no VISIBLE stopper.
			recurse(indice, true); // start with C1, then C2, C1, C2...
			// a colorChain consists of atleast two cells (one on, one off)
			// HiddenSingles are discarded
			if ( colorSets[C1].none() || colorSets[C2].none() ) {
				colorSets[C1].clear();
				colorSets[C2].clear();
			} else
				++numColorPairs[cand];
		}
		return numColorPairs[cand];
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
		if ( indice==-1 || !startSet.contains(indice) )
			return;
		// record the color of this index
		if ( on )
			colorSets[C1].add(indice);
		else
			colorSets[C2].add(indice);
		// remove this index from the start set
		startSet.remove(indice);
		// recursion (a depth first search (DFS))
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
		if ( r.indexesOf[candidate].size != 2 )
			return -1; // no conjugate pair, so stop coloring.
		// return the indice of the other cell in this conjugate pair
		conjugateSet.setAnd(candidateSet, r.idx);
		int result = conjugateSet.poll();
		if ( result == indice )
			result = conjugateSet.poll();
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ MULTI COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Multi-Color 1/2 hints in the grid and adds them to accu.
	 *
	 * @return true if hint/s were found, else false
	 */
	private boolean findMultiColors() {
		Idx[][] vSets;
		Idx a1,a2, b1,b2;
		AHint hint;
		int v, numColors, i, j;
		final Pots reds = this.redPots;
		reds.clear();
		boolean any;
		// presume the no hints will be found
		boolean result = false;
		// foreach value: foreach color
		for ( v=1; v<10; ++v ) {
			numColors = doColoring(v);
			vSets = colors[v];
			// first see if cells of one color can see opposite cells of
			// another color pair. if so, eliminate all cells with that color.
			// NOTE: a->b != b->a, so ALL combinations must be checked.
			A_LOOP: for ( i=0; i<numColors; ++i )
				if ( (a1=vSets[i][C1]).any()
				  && (a2=vSets[i][C2]).any() )
					B_LOOP: for ( j=0; j<numColors; ++j )
						if ( j != i
						  && (b1=vSets[j][C1]).any()
						  && (b2=vSets[j][C2]).any()
						) {
							any = false; // nb: redPots is cleared by hint-creation
							if ( isMultiColor1(a1, b1, b2) )
								any = reds.addAll(a1.cells(grid), new Values(v));
							if ( isMultiColor1(a2, b1, b2) )
								any |= reds.addAll(a2.cells(grid), new Values(v));
							if ( any ) {
								hint = new ColoringHint(this, Subtype.MultiColor1
										, potsArray(v, a1, a2, b1, b2), v, reds);
								result = true;
								if ( accu.add(hint) )
									return result;
							} else {
								// now see if a two cells of different color pairs see
								// each other. If so, then all candidates that can see
								// cells of the two other colors can be eliminated.
								if ( isMultiColor2(a1, b1) )
									any = checkCandidateToDelete(a2, b2, v, reds);
								if ( isMultiColor2(a1, b2) )
									any |= checkCandidateToDelete(a2, b1, v, reds);
								if ( isMultiColor2(a2, b1) )
									any |= checkCandidateToDelete(a1, b2, v, reds);
								if ( isMultiColor2(a2, b2) )
									any |= checkCandidateToDelete(a1, b1, v, reds);
								if ( any ) {
									hint = new ColoringHint(this, Subtype.MultiColor2
										   , potsArray(v, a1, a2, b1, b2), v, reds);
									result = true;
									if ( accu.add(hint) )
										return result;
								}
							}
						}
		}
		return result;
	}

	/**
	 * Checks if cells in set see cells in both a and b. If so, then all
	 * candidates in set can be eliminated.
	 *
	 * @param set Set to be checked
	 * @param a First color of other color pair
	 * @param b Second color of other color pair
	 * @return
	 */
	private boolean isMultiColor1(Idx set, Idx a, Idx b) {
		// NOTE: none of the sets can be empty or you can't get here
		assert !set.none() && !a.none() && !b.none();
		// see if sets-buds intersect both a and b
		final int a0=a.a0, a1=a.a1, a2=a.a2;
		final int b0=b.a0, b1=b.a1, b2=b.a2;
		Idx bs; // buds doesn't fit on a line
		boolean seeA=false, seeB=false;
		for ( int i : set.toArrayA() ) {
			bs = Grid.BUDDIES[i];
			seeA |= (bs.a0 & a0)!=0 || (bs.a1 & a1)!=0 || (bs.a2 & a2)!=0;
			seeB |= (bs.a0 & b0)!=0 || (bs.a1 & b1)!=0 || (bs.a2 & b2)!=0;
			if ( seeA && seeB )
				return true;
		}
		return false;
	}

	/**
	 * Checks if a cell in Set 'a' can see any cell in Set 'b'.
	 *
	 * @param a First set
	 * @param b Second set
	 * @return
	 */
	private boolean isMultiColor2(Idx a, Idx b) {
		if ( a.none() || b.none() )
			return false;
		final int n = a.size();
		final int[] indices = a.toArrayA();
		for ( int i=0; i<n; ++i )
			if ( Grid.BUDDIES[indices[i]].andAny(b) ) // intersects
				return true;
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
	private Pots[] potsArray(int valueToRemove, Idx... idxs) {
		final int n = idxs.length;
		Pots[] result = new Pots[n];
		for ( int i=0; i<n; ++i )
			result[i] = new Pots(idxs[i].cells(grid), new Values(valueToRemove));
		return result;
	}

}
