/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * The algorithm for the Skyscraper solving technique was boosted from the
 * current release (AFAIK) of HoDoKu by Keith Corlett in March 2020. Kudos
 * to hobiwan. Mistakes are mine.
 *
 * Here is hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
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
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Grid.pairs;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;

/**
 * Skyscraper implements the {@link Tech#Skyscraper} Sudoku solving technique.
 * <p>
 * A Skyscraper is a simplified Unary (single digit) Forcing Chain. A value has
 * two places in two rows/cols, one of which is in the same col/row, hence one
 * of the two cells at the "misaligned end" of the Skyscraper must be value;
 * therefore we can eliminate value from cells seeing both "misaligned ends".
 * NB: In this code "the misaligned ends" are called "the other ends".
 * <p>
 * All Skyscrapers are found by the Unary Chainer. Skyscraper exists coz it is
 * faster to find this specific pattern, so SE is probably faster without it,
 * but this applies to many hinters: many hinters are a simplification of a
 * heavier/slower generalised pattern, especially the chainers.
 * <p>
 * For speed rely on the chainers; but explaining a puzzle requires applying
 * the hint from the simplest possible hinter that produces a hint, probably
 * with an explanation that users can follow. Maybe 10% of users will ever walk
 * through the explanation of a forcing-chain, let alone apply the technique
 * manually. Maybe 20% can/will follow a Skyscraper, hence Skyscraper exists.
 * <p>
 * Its around here that we loose most users that downloaded SE to learn how to
 * solve Sudoku puzzles manually; here-after its hard work, which folks do NOT
 * like; so somewhere around SDP we start whittling our way down to the heavy
 * thinkers; probably mostly programmers. Solving hard Sudoku puzzles manually
 * could take years. Most people tap-out around here, and start just guessing,
 * coz Donald Knuth is a bastard! Tell him I said that. He will understand.
 * <p>
 * The package name "sdp" stands for SingleDigitPattern, which is the HoDoKu
 * class that fathered these hinters.
 * <pre>
 * Skyscraper is near fast enough to be a fox, so it now implements the full
 * findHints(Grid, IAccu) itself, rather than waste time setting unused fields.
 * </pre>
 *
 * @author Keith Corlett 2020-03-25
 */
public class Skyscraper extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[14];

	// the "other" type of region: 1=ROW, 2=COL ([0] unused)
	private static final int[] OTHER_TYPE = {0, 2, 1};

	// the "other" end of this pair 0->1, 1->0
	private static final int[] OTHER_END = {1, 0};

	/**
	 * Constructor.
	 */
	public Skyscraper() {
		super(Tech.Skyscraper);
	}

	@Override
	public void setFields(Grid grid) {
		this.grid = grid;
		maybes = grid.maybes;
		idxs = grid.idxs;
		rows = grid.rows;
		cols = grid.cols;
	}

	@Override
	public void clearFields() {
		this.grid = null;
		maybes = null;
		idxs = null;
		rows = null;
		cols = null;
	}

	/**
	 * Find first/all Skyscraper hints in the grid, and add them to the accu.
	 * If the accu isSingle then stop looking when the first hint is found.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints() { // 16,352
		if ( oneHintOnly ) // short-circuiting or (batch)
			return search(rows) || search(cols);
		// bitwise-or operator always executes both (GUI)
		return search(rows) | search(cols);
	}

	/**
	 * Search for Skyscrapers in rows or cols.
	 *
	 * @param lines grid.rows or grid.cols
	 * @return were any hints found?
	 */
	private boolean search(final ARegion[] lines) {								//  32,613
		// a "line" is a row or a col by the way.
		Cell[] pA, pB; // pairA and pairB
		// the cross-regions of the two cells in pairA.
		// so if lines is rows, cross-regions is cols, and vice-versa.
		ARegion cr0, cr1;
		long vcM0; int vcM1; // victims exploded Idx
		int n // the number of pairs collected
		  , m // n - 1
		  , o // the otherIndex
		  , a // pairA index
		  , b; // pairB index
		// pairs of cells in lines
		final Cell[][] pairs = new Cell[REGION_SIZE][2];
		// a pair is the 2 cells in a line which both maybe v.
		// work-out the regionType and otherType from lines
		final int rT = lines[0].rti; // regionType
		final int oT = OTHER_TYPE[rT]; // otherType (the cross regions)
		// presume that no hint will be found
		boolean result = false;
		// foreach potential value
		int v = 1;
		do {																	// 292,820
			// if there are atleast two biplaced lines
			if ( (n=pairs(lines, v, pairs)) > 1 ) {								// 174,342
				// examine each combo of $a and $b pairs (forwards-only search)
				a = 0;
				m = n - 1;
				do { // first pair												// 306,946
					pA = pairs[a];
					cr0 = pA[0].regions[oT]; // crossRegion1
					cr1 = pA[1].regions[oT]; // crossRegion2
//					for ( b=a+1; b<n; ++b ) { // subsequent pair				// 499,559
					b = a + 1;
					do { // subsequent pair				// 499,559
						// Theres two "ends" to each pair of pairs:
						// * "this end" are in the same crossRegion, and the
						// * "other end" are not (thats a Swampfish).
						// if "this end" of pairB is in the same crossRegion
						// as "this end" of pairA, that determines "other end"
						pB = pairs[b];
						if ( ( (cr0==pB[0].regions[oT] && (o=1)==1) // "other end" is 1 (t=0)
							|| (cr1==pB[1].regions[oT] && (o=0)==0) ) // "other end" is 0 (t=1)
						  // and there are victims: buddies common to the		// 135,858
						  // "other ends" of pairA and pairB, which maybe v
						  && ( (vcM0=pA[o].buds.m0 & pB[o].buds.m0 & idxs[v].m0)
							 | (vcM1=pA[o].buds.m1 & pB[o].buds.m1 & idxs[v].m1) ) > 0L
						  // and "other ends" are in different crossRegions,
						  // else its a Swampfish, which is not my problem
						  && pA[o].regions[oT] != pB[o].regions[oT]
						  // FOUND a Skyscraper!
						  && (result=true)
						  // and if the IAccumulator says tap-out
						  && accu.add(hint(v, o, pA, pB, rT, oT, vcM0, vcM1)) )
							return result; // then tap-out
					} while (++b < n);
				} while (++a < m);
			}
		} while (++v < VALUE_CEILING);
		return result;
	}

	/**
	 * Returns a new SkyscraperHint.
	 *
	 * @param v the Skyscraper candidate value
	 * @param o the index of the "other end" (0 or 1) of pairA and pairB. <br>
	 *  "this end" are in the same crossRegion, and the <br>
	 *  "other ends" are in different crossRegions
	 * @param pA pairA the first pair-of-vs in a region
	 * @param pB pairB the second pair-of-vs in a distinct second region <br>
	 *  note that pB != pA is invariant here
	 * @param rT regionType is ROW/COL
	 * @param oT otherRegionType is COL/ROW
	 * @param reds the removable Cell=>Values
	 * @return a new SkyscraperHint
	 */
	private AHint hint(final int v, final int o, final Cell[] pA, final Cell[] pB
			, final int rT, final int oT, final long vcM0, final int vcM1) {
		final int t = OTHER_END[o]; // this end
		return new SkyscraperHint(grid, this, v
			, new ARegion[] {pA[t].regions[rT], pB[t].regions[rT]} // bases
			, new ARegion[] {pA[o].regions[oT], pB[o].regions[oT]} // covers
			, new Pots(vcM0,vcM1, maybes, VSHFT[v], DUMMY) // reds
			, new Pots(new Cell[]{pA[0], pA[1], pB[0], pB[1]}, VSHFT[v], false) // greens
		);
	}

}
