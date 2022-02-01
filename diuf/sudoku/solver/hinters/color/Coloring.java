/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.Grid.BOX_OF;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.COL_OF;
import static diuf.sudoku.Grid.ROW_OF;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.color.ColoringHint.Subtype;
import java.util.Arrays;
import static diuf.sudoku.utils.IntArray.each;

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

	/** First color of a coloring pair: 0. */
	private static final int GREEN = 0;
	/** Second color of a coloring pair: 1. */
	private static final int BLUE = 1;
	/** How many colors are there: 2. */
	private static final int NUM_COLORS = 2;

	/** Maximum number of color pairs: 20. */
	private static final int MAX_PAIRS = 20;

	/**
	 * indices of colored cells for each candidate.
	 * <pre>
	 * colors[GREEN][v][i] contains indices of cells which are GREEN.
	 * colors[BLUE][v][i] contains indices of cells which are BLUE.
	 * * GREEN and BLUE are (arbitrarily) our two colors (0 and 1).
	 * * v is the candidate value in 1..9.
	 * * i is the pair index in 0..numColorPairs[v]-1.
	 * * numColorPairs[v] is the number of pairs for v (ie the i ceiling).
	 * </pre>
	 * CAUTION: index order has changed. Care is required.
	 */
	private final Idx[][][] colors = new Idx[NUM_COLORS][VALUE_CEILING][MAX_PAIRS];
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
	/** The current colorSets in green and blue. */
	private Idx greenSet, blueSet;

	/** The grid to search. */
	private Grid grid;
	/** The regions of the grid to search. */
	private ARegion[] rows, cols, boxs;
	/** The idxs of the grid to search. */
	private Idx[] idxs;
	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	// for the eliminate method/s
	private final Idx cmnBuds = new Idx();

	// Idx of grid.cells which maybe v, set in colorIn for use in conjugate
	private Idx vidx;

	public Coloring() {
		super(Tech.Coloring);
		// create the coloring sets
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			hintNumbers[v] = -1;
			numColorPairs[v] = 0;
			for ( int j=0; j<MAX_PAIRS; ++j ) {
				colors[GREEN][v][j] = new Idx();
				colors[BLUE][v][j] = new Idx();
			}
		}
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		this.grid = grid;
		this.rows = grid.rows; this.cols = grid.cols; this.boxs = grid.boxs;
		this.idxs = grid.idxs; // cached indices which maybe each value
		this.accu = accu;
		// read the indices of biplaced cells, per value
		final Idx[] bips = grid.getBiplaces();

		// clear the cache each time I'm called coz I'm either on a new grid,
		// or the grid has changed since the last time I was called; else we
		// hit a bug only in 160 in LogicalSolverTester top1465.d5.mt
		Arrays.fill(hintNumbers, -1);
		Arrays.fill(numColorPairs, 0);

		boolean result;
		try {
			if ( accu.isSingle() ) {
				result = findSimpleColors(bips) || findMultiColors(bips);
			} else {
				result = findSimpleColors(bips) | findMultiColors(bips);
			}
		} finally {
			this.grid = null;
			this.rows = this.cols = this.boxs = null;
			this.idxs = null;
			this.accu = null;
//			Cells.cleanCasA();
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
	private boolean findSimpleColors(Idx[] biplaces) {
		Idx[] gv, bv; // green and blue v pairs-arrays
		Idx c1, c2; // indices of cells in the two colors
		AHint hint;
		int v, numColors, i;
		boolean any;
		final Pots reds = this.redPots;
		// presume that no hints will be found
		boolean result = false;
		reds.clear();
		// foreach value: foreach color
		for ( v=1; v<VALUE_CEILING; ++v ) {
			if ( biplaces[v].any() ) {
				// find all Simple Colors steps for v.
				numColors = colorIn(v, biplaces[v], gv=colors[GREEN][v], bv=colors[BLUE][v]);
				// now check for eliminations
				for ( i=0; i<numColors; ++i ) {
					try ( final IALease gLease = (c1=gv[i]).toArrayLease();
						  final IALease bLease = (c2=bv[i]).toArrayLease() ) {
						// color wrap (rare): if two cells with the same color see each
						// other, then we can remove all candidates of that color.
						any = false;
						if ( checkColorWrap(gLease.array) ) {
							any = reds.upsertAll(c1, grid, VSHFT[v], false);
						}
						if ( checkColorWrap(bLease.array) ) {
							any |= reds.upsertAll(c2, grid, VSHFT[v], false);
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
							if ( eliminate(gLease.array, bLease.array, v, reds) ) {
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
			}
		}
		return result;
	}
	private final Pots redPots = new Pots();

	/**
	 * If any two cells in indices see each other then
	 * all candidates in indices may be removed.
	 *
	 * @param indices The idx.toArray to examine
	 */
	private boolean checkColorWrap(int[] indices) {
		Idx buds;
		int i, j;
		final int n=indices.length, m=n-1;
		// foreach indice in set (except the last)
		for ( i=0; i<m; ++i ) {
			buds = BUDDIES[indices[i]];
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
	 * Note this eliminate is used in {@link #findMultiColors}.
	 * <p>
	 * It's faster to NOT iterate Idx's any more often than is required, but I
	 * have no pre-existing array of b, so we iterate b, then IntArray.forEach
	 * the 'a' array, so it's back-to-front but does the same thing.
	 * <p>
	 * That is: foreachA-foreachB is equivalent to foreachB-foreachA, it's just
	 * a bit faster coz we're not demangling two Idx's, only one once, which
	 * should be a bit faster, I hope. sigh.
	 * <p>
	 * Note that I wrote the IntArrays arrays class for this, because int[]
	 * has no innate forEach method, which is a shame really.
	 *
	 * @param a The first set to check
	 * @param b The second set to check
	 * @param v the candidate value 1..9 that we seek
	 * @param reds a non-null Pots to which I add eliminations, if any.
	 * @return true if any eliminated were found, else false
	 */
	private boolean eliminate(final int[] a, final Idx b, final int v, final Pots reds) {
		final Idx vs = idxs[v];
		final Idx cmnBuds = this.cmnBuds.clear();
		// foreach b foreach a: cmnBuds += buds[bi] & buds[ai] & idxs[v]
		b.forEach((bi)->each(a, (ai)->cmnBuds.orAnd(BUDDIES[bi], BUDDIES[ai], vs)));
		if ( cmnBuds.none() ) {
			return false;
		}
		return reds.upsertAll(cmnBuds, grid, VSHFT[v], false);
	}

	/**
	 * eliminate checks a candidate to be deleted: if any idx[v] sees a cell in
	 * 'a' and a cell in 'b', then it can be eliminated.
	 * <p>
	 * Note this eliminate is used in {@link #findSimpleColors} only.
	 * There is no reason to demangle the Idx repeatedly, so we get an array of
	 * indices once then read it twice: in {@link checkColorWrap} and here in
	 * the new, faster, eliminate method.
	 * <p>
	 * Note that I wrote the IntArrays arrays class for this, because int[]
	 * has no innate forEach method, which is a shame really.
	 *
	 * @param a GREEN indices
	 * @param b BLUE indices
	 * @param v the candidate value
	 * @param reds removable (red) potentials
	 * @return any eliminations found?
	 */
	private boolean eliminate(final int[] a, final int[] b, final int v, final Pots reds) {
		final Idx vs = idxs[v];
		final Idx cmnBuds = this.cmnBuds.clear();
		// foreach a foreach b: cmnBuds += buds[ai] & buds[bi] & idxs[v]
		each(a, (ai)->each(b, (bi)->cmnBuds.orAnd(BUDDIES[ai], BUDDIES[bi], vs)));
		if ( cmnBuds.none() ) {
			return false;
		}
		return reds.upsertAll(cmnBuds, grid, VSHFT[v], false);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ DO COLORING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * colorIn colors the grid, which means that each candidate that is part of
	 * at least one conjugate pair is assigned a color, which means that the
	 * candidate is added to one of the {@link #colors}.
	 * <p>
	 * Coloring is pretty straight-forward:<ul>
	 * <li>first eliminate all candidates, that are not part of at least one
	 *  conjugate pair
	 * <li>for each uncolored candidate remaining: do the coloring
	 * </ul>
	 * <p>
	 * Colored candidates are removed from {@link #startSet}.
	 * <p>
	 * Coloring sets are cached: If a set has already been calculated for the
	 * given candidate in current Sudoku-state then colorIn just returns the
	 * previously calculated number of color pairs, it doesn't repeat the
	 * calculations, for efficiency.
	 *
	 * @param v the candidate value in 1..9
	 * @param biplace are indices of cells that are biplaced in any region in
	 *  the grid, on the current v.
	 * @param gv is colors[GREEN][v]
	 * @param bv is colors[BLUE][v]
	 * @return the number of color pairs for the given cand
	 */
	private int colorIn(final int v, final Idx biplace, final Idx[] gv, final Idx[] bv) {
		// check the cache
		if ( hintNumbers[v] == grid.hintNumber ) {
			// sudoku has not changed since last calculation
			return numColorPairs[v];
		}
		// candidate is read down in the conjugate method
		candidate = v;
		// vidx is an Idx of grid.cells which maybe v, read in conjugate.
		vidx = idxs[v];
		// reset the number of pairs found for this value
		numColorPairs[v] = 0;
		// startSet: indices of cells with two places for candidate in a region
		startSet.set(biplace);
		// now do the coloring.
		int indice;
		while ( startSet.any() ) {
			// get the first (ie the next) indice
			// nb: peek is non-destructive (it doesn't mutate the startSet)
			indice = startSet.peek();
			// pre-set the colors-fields for recurse
			greenSet = gv[numColorPairs[v]].clear();
			blueSet  = bv[numColorPairs[v]].clear();
			// recursively search from indice for coloring pairs on candidate,
			// adding each candidate to it's appropriate colorSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// So this loop stops, despite having no VISIBLE stopper.
			recurse(indice, true); // GREEN, BLUE, GREEN, BLUE, ...
			// a chain has both green AND blue
			if ( greenSet.any() && blueSet.any() ) {
				++numColorPairs[v];
			}
		}
		hintNumbers[v] = grid.hintNumber;
		return numColorPairs[v];
	}

	/**
	 * Recursively search from indice for coloring pairs on candidate. Recurse
	 * recursively colors any conjugate pairs (regions with just two places for
	 * a value) in the three regions that contain the cell at indice.
	 * <p>
	 * Weird science: This method removes each index from the startSet field,
	 * which stops my callers containing loop. This method adds each indice to
	 * the colorSets array: (if on GREEN else BLUE); to split the candidates
	 * into two color-sets.
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
	 * @param i indice of the cell to color; -1 to stop coloring.
	 * @param on true for {@link #GREEN}, false for {@link #BLUE}
	 */
	private void recurse(final int i, final boolean on) {
		// we need a conjugate, and we've not already done this one
		if ( i>-1 && startSet.has(i) ) {
			// add this indice to the appropriate color: green or blue
			if(on) greenSet.add(i); else blueSet.add(i);
			// remove indice from the startSet, so that we explore the
			// conjugates of each indice just ONCE.
			startSet.remove(i);
			// recursion: a depth first search (DFS)
			recurse(conjugate(i, rows[ROW_OF[i]]), !on);
			recurse(conjugate(i, cols[COL_OF[i]]), !on);
			recurse(conjugate(i, boxs[BOX_OF[i]]), !on);
		}
	}

	/**
	 * Checks whether cell {@code indice} belongs to a conjugate pair on value
	 * {@code candidate} in ARegion {@code r}. Returns the indice of the other
	 * cell which maybe candidate in region, or -1 meaning no conjugate pair.
	 * <pre>
	 * The word "conjugate" means:
	 * * "con" means shared, and (usually) implies duality, ie two;
	 * * "jug" means a container; and
	 * * "ate" means of or pertaining to;
	 * * so conjugate: two of a shared container.
	 * * We add the pair because we overlook it's already there, but it's nice
	 *   to reinforce the two, because "con" only usually means two; sometimes
	 *   it means two-or-more, which is more of a "com" really: a fuzzy n/m.
	 * </pre>
	 * A conjugate is the other place for value in region: the region has just
	 * two places for value, here's one, what's the other one. We call both of
	 * these cells a conjugate pair.
	 * <p>
	 * This method reads the candidate field (set by colorIn);
	 * and the vidx field (set by colorIn);
	 * and sets and reads conjugateSet field, not referenced externally.
	 * <p>
	 * This method is the ONLY use of the conjugateSet field, which is a field
	 * to avoid repeatedly creating an Idx instance, and all stack-work. It's
	 * faster to not have to create a new stack-frame when invoking a method.
	 * <p>
	 * We poll()-off the first one or two indices in conjugateSet, because poll
	 * is faster than get(i), because poll doesn't initialise the conjugateSet.
	 *
	 * @param i indice of cell for which a conjugate is sought
	 * @param r the ARegion to look in
	 * @return An index, if the house has only one cell left, or -1
	 */
	private int conjugate(final int i, final ARegion r) {
		if ( r.ridx[candidate].size != 2 )
			return -1; // no conjugate pair, so stop coloring.
		// return the indice of the other cell in this conjugate pair
		return conjugateSet.setAnd(vidx, r.idx).otherThan(i);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ MULTI COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Multi-Coloring hints in the grid and adds them to accu.
	 *
	 * @return true if hint/s were found, else false
	 */
	private boolean findMultiColors(final Idx[] biplaces) {
		AHint hint;
		// indices of cells whose v is painted green/blue
		Idx[] gv, bv;
		// the first and second pair
		Idx a1,a2, b1,b2;
		// indices in the a1 and a2 Idx's
		int[] a1a, a2a;
		int v, numColors, i, j;
		final Pots reds = this.redPots;
		boolean any;
		// presume the no hints will be found
		boolean result = false;
		reds.clear();
		// foreach value: foreach color
		// NOTE: a->b != b->a, so check ALL combos, not just forward-only.
		for ( v=1; v<VALUE_CEILING; ++v ) {
			// color-in each conjugate pair of cells which maybe v
			numColors = colorIn(v, biplaces[v], gv=colors[GREEN][v], bv=colors[BLUE][v]);
			// foreach first set of colored cells
			A_LOOP: for ( i=0; i<numColors; ++i ) {
				if ( (a1=gv[i]).any()
				  && (a2=bv[i]).any() ) {
					// read greens and blues into arrays ONCE, for speed.
					try ( final IALease gLease = a1.toArrayLease();
						  final IALease bLease = a2.toArrayLease() ) {
						a1a = gLease.array;
						a2a = bLease.array;
						// foreach second set of colored cells
						B_LOOP: for ( j=0; j<numColors; ++j ) {
							if ( (b1=gv[j]).any()
							  && (b2=bv[j]).any()
							  && j != i
							) {
								any = false; // nb: redPots is cleared by hint-creation
								// first see if cells of one-color see cells of the
								// other color, eliminating all v's of that color.
								if ( isMultiColor1(a1a, b1, b2) )
									any |= reds.upsertAll(a1, grid, VSHFT[v], false);
								if ( isMultiColor1(a2a, b1, b2) )
									any |= reds.upsertAll(a2, grid, VSHFT[v], false);
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
									if ( isMultiColor2(a1a, b1) )
										any |= eliminate(a2a, b2, v, reds);
									if ( isMultiColor2(a1a, b2) )
										any |= eliminate(a2a, b1, v, reds);
									if ( isMultiColor2(a2a, b1) )
										any |= eliminate(a1a, b2, v, reds);
									if ( isMultiColor2(a2a, b2) )
										any |= eliminate(a1a, b1, v, reds);
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
		}
		return result;
	}

	/**
	 * Returns can cells in indices see cells in both a and b?
	 * If so, then all indices can be eliminated.
	 *
	 * @param indices to be checked
	 * @param a First color of other color pair
	 * @param b Second color of other color pair
	 * @return can cells in set see cells in both a and b?
	 */
	private boolean isMultiColor1(final int[] indices, final Idx a, final Idx b) {
		// buds no fit on line
		Idx x;
		// shorthands and speed
		final int a0=a.a0, a1=a.a1, a2=a.a2
				, b0=b.a0, b1=b.a1, b2=b.a2;
		// see if sets-buds intersect both a and b
		boolean seeA=false, seeB=false;
		for ( int i : indices ) {
			x = BUDDIES[i];
			seeA |= ((x.a0 & a0) | (x.a1 & a1) | (x.a2 & a2)) != 0;
			seeB |= ((x.a0 & b0) | (x.a1 & b1) | (x.a2 & b2)) != 0;
			if ( seeA && seeB ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Can a cell in 'indices' see any cell in Set 'b'?
	 * If so, then 'indices' can be eliminated.
	 *
	 * @param indices, ie a.toArray, ie the First set
	 * @param b Second set
	 * @return
	 */
	private boolean isMultiColor2(final int[] indices, final Idx b) {
		final int n = indices.length;
		Idx aBuds;
		for ( int i=0; i<n; ++i ) {
			if ( ( (b.a0 & (aBuds=BUDDIES[indices[i]]).a0)
				 | (b.a1 & aBuds.a1)
				 | (b.a2 & aBuds.a2) ) != 0 ) {
				return true;
			}
		}
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ HELPERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Create an array of Pots from cells in the given idxs => valueToRemove.
	 *
	 * @param v the value to be eliminated from idxs
	 * @param a idx to add
	 * @param b idx to add
	 * @return a new array of Pots
	 */
	private Pots[] potsArray(final int v, final Idx a, final Idx b) {
		return new Pots[] {
			  new Pots(a, grid, VSHFT[v], DUMMY)
			, new Pots(b, grid, VSHFT[v], DUMMY)
		};
	}

	/**
	 * Create an array of Pots from cells in the given idxs => valueToRemove.
	 *
	 * @param v the value to be eliminated from idxs
	 * @param a idx to add
	 * @param b idx to add
	 * @param c idx to add
	 * @param d idx to add
	 * @return a new array of Pots
	 */
	private Pots[] potsArray(final int v, final Idx a, final Idx b, final Idx c, final Idx d) {
		return new Pots[] {
			  new Pots(a, grid, VSHFT[v], DUMMY)
			, new Pots(b, grid, VSHFT[v], DUMMY)
			, new Pots(c, grid, VSHFT[v], DUMMY)
			, new Pots(d, grid, VSHFT[v], DUMMY)
		};
	}

}
