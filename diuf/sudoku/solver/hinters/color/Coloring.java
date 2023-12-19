/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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

import static diuf.sudoku.Constants.BLUE;
import static diuf.sudoku.Constants.GREEN;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BOX_OF;
import static diuf.sudoku.Grid.BUDS_M0;
import static diuf.sudoku.Grid.BUDS_M1;
import static diuf.sudoku.Grid.COL_OF;
import static diuf.sudoku.Grid.ROW_OF;
import static diuf.sudoku.Grid.SEES;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.IdxC;
import diuf.sudoku.IdxL;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.color.ColoringHint.Subtype;
import java.util.Arrays;

/**
 * Coloring implements the {@link Tech#Coloring} Sudoku solving technique.
 * I implement both Simple-Coloring and the only Multi-Coloring in Sudoku
 * Explainer (SE).
 * <p>
 * Simple-coloring involves two sets of biplace candidates: regions with just
 * two positions for v are chained together into a forcing loop; so that either
 * set1 (color-1) contains v or set2 (color-2) contains v, leading to the two
 * simple coloring rules:<ul>
 * <li>color trap (common): cells which can see both colors may not be v; and
 * <li>color wrap (rarer): if two cells of the same color see each other then
 *  all vs of that color can be removed.
 * </ul>
 * <p>
 * Multi-Coloring involves multiple (more-than-two) coloring-sets that are
 * organised into parities to produce reduced eliminations.
 * <p>
 * KRC 2021-05-22 I just looked at splitting Tech.Coloring into ColoringSimple
 * and ColoringMulti, because XColoring finds more elims for a simple coloring.
 * Its better to leave it as it is, with XColoring finding JUST the additional
 * elims after findSimpleColoring has run.
 *
 * @author Keith Corlett 2020 Sept 21
 */
public final class Coloring extends AHinter
//	implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln("Coloring: COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[2];

	/** How many colors are there: 2. */
	private static final int NUM_COLORS = 2;

	/** Maximum number of color pairs: 20 (just a guess). */
	private static final int MAX_PAIRS = 20;

	// NOTE: BLUE and GREEN are now defined in Constants, for use elsewhere
	// (outside of coloring package, in the GUI) so are shared with XColoring,
	// Medusa3D, GEM, etc)

	// NOTE WELL: All fields created/cleared by findHints to reduce memory
	// footprint between calls. We are close to running out of RAM, so GC goes
	// into overdrive, so reduce memory-hogs, picking-on worste cases first:
	// Coloring, XColoring, Medusa3D, and GEM. Then the chainers if necessary.
	// Sigh.

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
	private IdxC[][][] colors;

	/** Number of color pairs for each candidate. */
	private int[] numColorPairs;

	/** Step number of the sudoku for which coloring was calculated. -1 means
	 * "data invalid". */
	private int[] hintNumbers;

	/** contains all candidates, that are part of at least one conjugate pair. */
	private Idx startSet;

	/** The current colorSets in green and blue. */
	private Idx greenSet, blueSet;

	/** This candidate: the one we are currently processing. This is a field
	 * just to reduce the amount of stack-work: the handbrake of recursion. */
	private int candidate;

	// Idx of grid.cells which maybe v, set in colorIn for use in conjugate
	private Idx vidx;

	public Coloring() {
		super(Tech.Coloring);
	}

	@Override
	public void setFields(final Grid grid) {
		super.grid = grid;
		boxs = grid.boxs;
		rows = grid.rows;
		cols = grid.cols;
		cells = grid.cells;
		maybes = grid.maybes;
		idxs = grid.idxs;
	}

	@Override
	public void clearFields() {
		grid = null;
		boxs = null;
		rows = null;
		cols = null;
		cells = null;
		maybes = null;
		idxs = null;
	}

	/**
	 * Set-up all of the fields used in this hinter.
	 */
	private void setMyFields() {
		int color, value, i;
		// create/clear all fields rather than hog memory between calls
		colors = new IdxC[NUM_COLORS][VALUE_CEILING][MAX_PAIRS]; // [2][10][20]
		color = GREEN;
		do {
			value = 1;
			do {
				i = 0;
				do
					colors[color][value][i] = new IdxC();
				while (++i < MAX_PAIRS); // 0..MAX_PAIRS-1
			} while (++value < VALUE_CEILING); // 1..9
		} while (++color < 2); // GREEN, BLUE.
		numColorPairs = new int[VALUE_CEILING];
		hintNumbers = new int[VALUE_CEILING];
		Arrays.fill(hintNumbers, -1);
		startSet = new Idx();
	}

	/**
	 * Clear all of the field references set in setMyFields, and any others,
	 * thus minimising my memory footprint between calls.
	 */
	private void clearMyFields() {
		// create/clear all fields rather than hog memory between calls
		colors = null;
		numColorPairs = hintNumbers = null;
		startSet = greenSet = blueSet = null;
	}

	@Override
	public boolean findHints() {
		boolean result;
		try {
			setMyFields();
			// read the indices of biplaced cells, per value
			final IdxL[] bips = grid.getBiplaces();
			// my actual work
			if ( oneHintOnly )
				result = simpleColors(bips) || multiColors(bips);
			else
				result = simpleColors(bips) | multiColors(bips);
		} finally {
			clearMyFields();
		}
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Simple Wrap/Trap hints in the grid and add them to accu.
	 *
	 * @param biplaces Idxs of biplaced cells, by value
	 * @return true if any hint/s were found, else false
	 */
	private boolean simpleColors(final IdxL[] biplaces) {
		IdxC[] greens, blues; // indices of green/blue conjugate-pairs on value
		IdxC green, blue; // indices of cells in the two colors
		int greenA[], blueA[], numColors, i;
		// have any eliminations been found
		boolean any = false;
		// presume that no hints will be found
		boolean result = false;
		// foreach value: foreach color
		int value = 1;
		do {
			if ( biplaces[value].any() ) {
				// paint (colorIn) all Simple Colors steps for v.
				greens = colors[GREEN][value];
				blues = colors[BLUE][value];
				numColors = colorIn(value, biplaces[value], greens, blues);
				// now check for eliminations
				for ( i=0; i<numColors; ++i ) {
					greenA = (green=greens[i]).indicesCached();
					blueA = (blue=blues[i]).indicesCached();
					// color wrap (rare): if two cells with the same color see
					// each other, then eliminate all candidates of that color.
					if ( checkColorWrap(greenA) )
						any |= reds.upsertAll(green, maybes, VSHFT[value], DUMMY);
					if ( checkColorWrap(blueA) )
						any |= reds.upsertAll(blue, maybes, VSHFT[value], DUMMY);
					if ( any ) {
						any = false;
						result = true;
						if ( accu.add(new ColoringHint(grid, this
								, Subtype.SimpleColorWrap
								, potsArray(value, green, blue)
								, value
								, reds.copyAndClear())) )
							return result;
					} else {
						// color trap (more common): any candidate that sees
						// two cells of opposite colors can be removed.
						if ( eliminate(greenA, blueA, value) ) {
							result = true;
							if ( accu.add(new ColoringHint(grid, this
									, Subtype.SimpleColorTrap
									, potsArray(value, green, blue), value
									, reds.copyAndClear())) )
								return result;
						}
					}
				}
			}
		} while (++value < VALUE_CEILING);
		return result;
	}
	private final Pots reds = new Pots();

	/**
	 * If any two cells in indices see each other then
	 * all candidates in indices may be removed.
	 *
	 * @param indices The idx.toArray to examine
	 */
	private boolean checkColorWrap(final int[] indices) {
		boolean[] sees;
		int i, j;
		final int n=indices.length, m=n-1;
		for ( i=0; i<m; ++i ) // foreach except last
			for ( sees=SEES[indices[i]],j=i+1; j<n; ++j ) // subsequent
				if ( sees[indices[j]] )
					return true;
		return false;
	}

	/**
	 * eliminate checks a candidate to be deleted: if any idxs[$v] sees a cell
	 * in $green and a cell in $blue, then it can be eliminated.
	 *
	 * @param green GREEN indices
	 * @param blue BLUE indices
	 * @param v the candidate value
	 * @return any eliminations found?
	 */
	private boolean eliminate(final int[] green, final int[] blue, final int v) {
		final long vs0 = idxs[v].m0; final int vs1 = idxs[v].m1;
		long m0=0L; int m1=0;
		for ( int g : green )
			for ( int b : blue ) {
				// cmnBuds += idxs[v] & buds[g] & buds[b]
				m0 |= vs0 & BUDS_M0[g] & BUDS_M0[b];
				m1 |= vs1 & BUDS_M1[g] & BUDS_M1[b];
			}
		if ( (m0|m1) < 1L )
			return false;
		return reds.upsertAll(m0, m1, maybes, VSHFT[v], DUMMY);
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
	 * previously calculated number of color pairs, it does not repeat the
	 * calculations, for efficiency.
	 *
	 * @param v the candidate value in 1..9
	 * @param biplace are indices of cells that are biplaced in any region in
	 *  the grid, on the current v.
	 * @param greens colors[GREEN][v]
	 * @param blues colors[BLUE][v]
	 * @return the number of color pairs for the given cand
	 */
	private int colorIn(final int v, final Idx biplace, final Idx[] greens, final Idx[] blues) {
		// check the cache, so that multiColoring need not redo colorIn
		if ( hintNumbers[v] == grid.hintNumber )
			return numColorPairs[v]; // cache unchanged
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
			// nb: peek is non-destructive (it does not mutate the startSet)
			indice = startSet.peek();
			// pre-set the colors-fields for recurse
			greenSet = greens[numColorPairs[v]].clearMe();
			blueSet  = blues[numColorPairs[v]].clearMe();
			// recursively search from indice for coloring pairs on candidate,
			// adding each candidate to it is appropriate colorSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// NOTE that recurse removes each indice from startSet.
			// So this loop stops, despite having no VISIBLE stopper.
			recurse(indice, true); // GREEN, BLUE, GREEN, BLUE, ...
			// a chain has both green AND blue
			if ( greenSet.any() && blueSet.any() )
				++numColorPairs[v];
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
	 * an OO-design mind-set, that is all. I just prefer speed.
	 *
	 * @param i indice of the cell to color; -1 to stop coloring.
	 * @param on true for {@link #GREEN}, false for {@link #BLUE}
	 */
	private void recurse(final int i, final boolean on) {
		// if valid indice, and we have not already done this indice.
		// nb: we recurse conjugates each indice ONCE.
		if ( i>-1 && startSet.visit(i) ) {
			// add this indice to the appropriate color: green or blue
			if ( on )
				greenSet.add(i);
			else
				blueSet.add(i);
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
	 * * We add the pair because we overlook it is already there, but it is nice
	 *   to reinforce the two, because "con" only usually means two; sometimes
	 *   it means two-or-more, which is more of a "com" really: a fuzzy n/m.
	 * </pre>
	 * A conjugate is the other place for value in region: the region has just
	 * two places for value, here is one, whats the other one. We call both of
	 * these cells a conjugate pair.
	 * <p>
	 * This method reads the candidate field (set by colorIn);
	 * and the vidx field (set by colorIn);
	 * and sets and reads conjugateSet field, not referenced externally.
	 * <p>
	 * This method is the ONLY use of the conjugateSet field, which is a field
	 * to avoid repeatedly creating an Idx instance, and all stack-work. It is
	 * faster to not have to create a new stack-frame when invoking a method.
	 * <p>
	 * We poll()-off the first one or two indices in conjugateSet, because poll
	 * is faster than get(i), because poll does not initialise the conjugateSet.
	 *
	 * @param i indice of cell for which a conjugate is sought
	 * @param r the ARegion to look in
	 * @return An index, if the house has only one cell left, or -1
	 */
	private int conjugate(final int i, final ARegion r) {
		if ( r.numPlaces[candidate] != 2 )
			return -1; // no conjugate pair, so stop coloring.
		// return the indice of the other cell in this conjugate pair
		return r.indices[IFIRST[r.places[candidate] & ~cells[i].placeIn[r.rti]]];
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ MULTI COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all Multi-Coloring hints in the grid and adds them to accu.
	 *
	 * @return true if hint/s were found, else false
	 */
	private boolean multiColors(final Idx[] biplaces) {
		// nb: I use IdxC for indicesCached, which halves my runtime
		IdxC[] greens, blues; // indices for whom value is painted green/blue
		IdxC aG,aB; // the first pair
		IdxC bG, bB; // the second pair
		// indices in aG (green) and aB (blue), numColorSets, eye, jay, n-1
		int aGa[], aBa[], numColorSets, i, j, m;
		// are there any eliminations
		boolean any = false;
		// presume the no hints will be found
		boolean result = false;
		// foreach value: foreach color
		// NOTE: a->b != b->a, so check ALL combos, not just forward-only.
		int value = 1;
		do {
			// colorIn each conjugate pair of cells on value
			greens = colors[GREEN][value];
			blues = colors[BLUE][value];
			numColorSets = colorIn(value, biplaces[value], greens, blues);
			// foreach first set of conjugate cells (except the last)
			A_LOOP: for ( m=numColorSets-1,i=0; i<m; ++i ) {
				if ( (aG=greens[i]).any()
				  && (aB=blues[i]).any() ) {
					// read greens and blues into arrays ONCE, for speed.
					aGa = aG.indicesCached();
					aBa = aB.indicesCached();
					// foreach second set of conjugate cells
					j = i + 1; // a forwards only search
					B_LOOP: do {
						if ( (bG=greens[j]).any()
						  && (bB=blues[j]).any()
						) {
							// first see if cells of one-color see cells of the
							// other color, eliminating all vs of that color.
							if ( isMultiColor1(aGa, bG.m0,bG.m1, bB.m0,bB.m1) )
								any |= reds.upsertAll(aGa, maybes, VSHFT[value], DUMMY);
							if ( isMultiColor1(aBa, bG.m0,bG.m1, bB.m0,bB.m1) )
								any |= reds.upsertAll(aBa, maybes, VSHFT[value], DUMMY);
							if ( any ) {
								any = false;
								result = true;
								if ( accu.add(new ColoringHint(grid, this
										, Subtype.MultiColor1
										, potsArray(value, aG, aB, bG, bB)
										, value
										, reds.copyAndClear())) )
									return result;
							} else {
								// if 2 cells in green and blue see each other
								// then all vs seeing cells of opposite colors
								// are eliminated
								if ( isMultiColor2(aGa, bG.m0,bG.m1) )
									any |= eliminate(aBa, bB.indicesCached(), value);
								if ( isMultiColor2(aGa, bB.m0,bB.m1) )
									any |= eliminate(aBa, bG.indicesCached(), value);
								if ( isMultiColor2(aBa, bG.m0,bG.m1) )
									any |= eliminate(aGa, bB.indicesCached(), value);
								if ( isMultiColor2(aBa, bB.m0,bB.m1) )
									any |= eliminate(aGa, bG.indicesCached(), value);
								if ( any ) {
									any = false;
									result = true;
									if ( accu.add(new ColoringHint(grid, this
											, Subtype.MultiColor2
											, potsArray(value, aG, aB, bG, bB)
											, value
											, reds.copyAndClear())) )
										return result;
								}
							}
						}
					} while (++j < numColorSets);
				}
			}
		} while (++value < VALUE_CEILING);
		return result;
	}

	/**
	 * Returns do cells in $indices see both $g and $b?
	 * If so, then all indices can be eliminated.
	 *
	 * @param indices of the first color pair to be examined
	 * @param g0 mask0 of green of second color pair (a)
	 * @param g1 mask1 of green of second color pair
	 * @param b0 mask0 of blue of second color pair (b)
	 * @param b1 mask1 of blue of second color pair
	 * @return do cells in $indices see both $g and $b?
	 */
	private boolean isMultiColor1(final int[] indices
			, final long g0, final int g1
			, final long b0, final int b1) {
		boolean seeA=false, seeB=false;
		for ( int i : indices ) {
			seeA |= ((BUDS_M0[i] & g0) | (BUDS_M1[i] & g1)) > 0;
			seeB |= ((BUDS_M0[i] & b0) | (BUDS_M1[i] & b1)) > 0;
			if ( seeA && seeB )
				return true;
		}
		return false;
	}

	/**
	 * Can any cell in $indices see Set $b?
	 * If so, then $indices can be eliminated.
	 *
	 * @param indices, ie a.toArray, ie the First set
	 * @param b0 b.a Second set, first mask
	 * @param b1 b.b Second set, second mask
	 * @return does any cell in indices see set b?
	 */
	private boolean isMultiColor2(final int[] indices, final long b0, final int b1) {
		for ( int i : indices )
			if ( ( (b0 & BUDS_M0[i])
				 | (b1 & BUDS_M1[i]) ) > 0L )
				return true;
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
			  new Pots(a, maybes, VSHFT[v], DUMMY)
			, new Pots(b, maybes, VSHFT[v], DUMMY)
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
			  new Pots(a, maybes, VSHFT[v], DUMMY)
			, new Pots(b, maybes, VSHFT[v], DUMMY)
			, new Pots(c, maybes, VSHFT[v], DUMMY)
			, new Pots(d, maybes, VSHFT[v], DUMMY)
		};
	}

}
