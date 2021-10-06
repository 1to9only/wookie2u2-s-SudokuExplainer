/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import diuf.sudoku.utils.Permutations;

/**
 * BasicFisherman implements the basic Swampfish (nee X-Wing) (2), Swordfish (3)
 * and Jellyfish (4) Sudoku solving techniques. Basic means no fins, Sashimi,
 * Franken, Mutant, or Kraken; just $degree bases (rows/cols) together having
 * exactly $degree places for v (the fish candidate value), because it's fast!
 * <p>
 * NOTE: I call X-Wing "Swampfish" because fish should have fish names, not
 * wing names. Recall that Yoda raises Lukes X-Wing from a Tatooeen swamp. When
 * I started, I wrongly thought X-Wing was a wing. Just two intelligent beings
 * on a whole planet and they still can't agree on what's important. "Confused
 * you are, hmmm." I think Swampfish is LESS confusing, but I could be wrong.
 */
public class BasicFisherman extends AHinter {

	private final int maxOccurences;
	private final int[] baseIndexes = new int[REGION_SIZE];
	private final int[] thePA; // the permutations array, for Permutations.

	public BasicFisherman(Tech tech) {
		super(tech);
		// Swampfish=2, Swordfish=3, Jellyfish=4; Smellyfish=5 is degenerate!
		assert tech==Tech.Swampfish || tech==Tech.Swordfish || tech==Tech.Jellyfish;
		this.maxOccurences = REGION_SIZE - degree*2;
		this.thePA = new int[degree];
	}

	/**
	 * Search the grid for Swamp/Sword/Jellyfish hints which I add to accu.
	 * @param grid to be searched
	 * @param accu the IAccumulator to which I add any hints found
	 * @return any hints found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		assert grid != null;
		assert accu != null;
		boolean result = false;
		try {
			if ( accu.isSingle() ) { // short-circuiting boolean-or || operator
				result = search(grid.rows, grid.cols, accu)
					  || search(grid.cols, grid.rows, accu);
			} else { // unusual non-short-circuiting bitwise-or | operator
				result = search(grid.rows, grid.cols, accu)
					   | search(grid.cols, grid.rows, accu);
			}
		} catch ( UnsolvableException ex ) {
			// catch UE to print grid with MIA region which can't exist! Me no
			// comprende what's going on, yet. Repeat for more info.
			StdErr.carp("BasicFisherman drowned!", ex, grid);
			throw ex;
		}
		return result;
	}

	/**
	 * Basic fish are those with $degree possible positions for $value shared
	 * amongst $degree bases (rows or cols), removing all non-base occurrences
	 * from $degree covers (cols or rows).<br>
	 * SO bases is the rows AND covers is the cols;<br>
	 * OR bases is the cols AND covers is the rows.
	 * <p>
	 * EXAMPLE: if the only cells which maybe 3 in rows 2 and 4 are both in
	 * columns B and H then the 3 must be in B2, H2, B4 or H4; and nowhere
	 * else in columns B and H, so we can remove 3 from the maybes of any
	 * other cells in columns B and H which currently may be 3.
	 * <p>
	 * The complex fish types are not implemented here. Finned/Sashimi,
	 * Franken, and Mutant are in HoDoKuFisherman; and Kraken will go in
	 * KrakenFisherman.
	 * <p>
	 * Well, either that or Douglas Adams is a dyslexic bisexual unicorn moving
	 * it all down to the bottom peg, despite his brother having his hair-cut
	 * yesterday, because there's 5 states of matter, his towel is on strike,
	 * and Davo did his brother. Oh Never MIND! Sheesh! Let us merely eliminate
	 * some of those bloody maybes. They've all got to go! Ha ya!
	 * @param bases  grid.rows or grid.cols
	 * @param covers grid.cols or grid.rows
	 * @return any hints found?
	 */
	private boolean search(final ARegion[] bases, final ARegion[] covers
			, final IAccumulator accu) {
		// ANSI-C style variables, for performance
		// INDEXES[vs]: an array of indexes in the vs bitset; brings reference
		// onto stack from heap, I think: not sure about this coz it depends on
		// how static references are compiled, which I haven't delved into. I
		// think they're slower, but I may be wrong.
		int[] indexes;
		int n // number of bases in the baseIndexes array (num actual bases)
		  , i // ubiqitious index
		  , card // cardinality: number of v's in this potential base
		  , vs // bitset of indexes of v's among the current bases
		  , baseBits // bitset of indexes of the current bases
		  , pink; // bitset of indices of removable v's in this cover
		// these vars bring references from the heap onto the stack, for speed.
		// baseIndexes := index of each base (in bases) with 2..$degree v's
		final int[] baseIndexes = this.baseIndexes; // an array of indexes
		final int degree = this.degree;
		final int[] PA = this.thePA; // the permutations array
		final int maxOccurences = this.maxOccurences + 1; // SNEEKY!
		final int degreePlus1 = this.degreePlus1;
		// presume that no hint will be found
		boolean result = false;
		// the removable (red) potential cell values
		Pots reds = null;
		// foreach possible value
		for ( int v=1; v<VALUE_CEILING; ++v ) { // 9
			// candiBits := array of indexes of bases with 2..degree v's.
			n = 0; // number of candidate regions
			for ( i=0; i<REGION_SIZE; ++i ) { // 9*9 = 81
				if ( (card=bases[i].ridx[v].size)>1 && card<degreePlus1 ) {
					baseIndexes[n++] = i;
				}
			}
			if ( n >= degree ) { // there are sufficient bases
				// we look for $degree positions of $v in $degree $bases;
				// foreach possible combination of $degree bases
				for ( int[] perm : new Permutations(n, PA) ) { // int[degree]
					// build a bitset of indexes of v's in these bases.
					// NOTE: A better name for vs might be anIntBitsetContainingTheIndicesOfCellsWhichMaybeTheFishCandidateValueInTheCurrentBases_GatheredTogetherInACaveAndGroovingWithAPict,
					// but I prefer vs, because it's short, so I can spell it,
					// and the name is somewhat descriptive of it's use, as if
					// it wasn't obvious to everybody at first glance. Sigh.
					vs = 0;
					for ( i=0; i<degree; ++i ) {
						vs |= bases[baseIndexes[perm[i]]].ridx[v].bits;
					}
					// if not degree positions for v in these degree bases
					// then there's no basic fish here.
//System.out.print("complete: "+v+":");
//for ( i=0; i<degree; ++i )
//	System.out.print(SPACE+bases[candiBits[perm[i]]].id);
//System.out.println(" = "+diuf.sudoku.Indexes.toString(coverBits));
					if ( VSIZE[vs] == degree ) {
						// ----------------------------------------------------
						// There's degree positions for v in degree bases, but
						// are there any elims: $v's in covers and not bases?
						// ----------------------------------------------------
						// get a bitset of the indexes of my bases: so if rows
						// are bases then I'm row-numbers, else I'm col-nums.
						// NOTE: NOT done above coz the hit rate is too low.
						baseBits = 0;
						for ( i=0; i<degree; ++i ) {
							baseBits |= ISHFT[baseIndexes[perm[i]]];
						}
						// get removable (red) pots = vs in covers except bases
						indexes = INDEXES[vs];
						for ( i=0; i<degree; ++i ) {
							if ( (pink=covers[indexes[i]].ridx[v].bits & ~baseBits) != 0 ) {
								if ( reds == null ) {
									reds = new Pots();
								}
								reds.addAll(covers[indexes[i]].cells, pink, v);
							}
						}
						// there's nothing to remove at least 95% of the time.
						if ( reds != null ) {
							// create the hint and add it to the IAccumulator
							result = true;
//if ( accu == null )
//	Debug.breakpoint();
//if ( reds == null )
//	Debug.breakpoint();
//if ( bases == null )
//	Debug.breakpoint();
//if ( covers == null )
//	Debug.breakpoint();
							if ( accu.add(createHint(v, reds, bases, baseBits, covers, vs)) ) {
								return result;
							}
							reds = null; // clean-up for next time
						}
					} // VSIZE filter
				} // next permutation
			} // n filter
		} // next v
		return result;
	}

	// @param baseBits is the indexes of the bases in the covers.
	// @param coverBits is the indexes of the covers in the bases.
	private AHint createHint(final int v, final Pots reds
			, final ARegion[] bases, final int baseBits
			, final ARegion[] covers, final int coverBits) {
		// highlighted (green) pots = v's in covers and bases (the corners).
		final Pots greens = new Pots(degree*degree, 1F);
		for ( int i : INDEXES[baseBits] )
			greens.addAll(bases[i].cells, coverBits, v);
		// build and return the new hint
		return new BasicFishHint(this, reds, v, greens, EMPTY_STRING
				, Regions.list(degree, bases, baseBits)
				, Regions.list(degree, covers, coverBits));
	}

}
