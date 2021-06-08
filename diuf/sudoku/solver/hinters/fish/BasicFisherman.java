/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Indexes.INDEXES;
import static diuf.sudoku.Indexes.ISHFT;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Permutations;


/**
 * BasicFisherman implements the basic Swampfish (nee X-Wing) (2), Swordfish (3)
 * and Jellyfish (4) Sudoku solving techniques. Basic means no fins, sashimi,
 * franken, mutant, or kraken; just $degree bases (rows/cols) having exactly
 * $degree possible positions for $degree values, because it's fast!
 * <p>
 * NOTE: I call X-Wing "Swampfish" because fish should have fish names, not
 * wing names. Recall: Yoda raises Lukes X-Wing from a Tatooeen swamp.
 * When I started, I wrongly mentally associated X-Wings with the other wings
 * and went looking for the XWing hinter in the wings package. Two intelligent
 * beings on a whole planet and they still can't agree on anything important.
 * Confused you are, hmmm. I think this is less confusing. I could be wrong.
 */
public final class BasicFisherman extends AHinter {

	private final int maxOccurences;
	private final int[] candiBits = new int[9]; // candidate region indexes
	private final int[] thePA; // the permutations array, for Permutations.

	public BasicFisherman(Tech tech) {
		super(tech);
		// Swampfish=2, Swordfish=3, Jellyfish=4; Smellyfish=5 is degenerate!
		assert tech==Tech.Swampfish || tech==Tech.Swordfish || tech==Tech.Jellyfish;
		this.maxOccurences = 9 - degree*2;
		this.thePA = new int[degree];
	}

	// NB: resulting hints are denoted in terms of covers (green ones).
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		try {
			int[] occurances = grid.countOccurances(); // O(81), ONCE!
			// This arsebungle was bought to you by the combination of an octet
			// arsecumulators, the letter 'r', and the numeral 2. Grouch'll be
			// along in a minute, with a telescope and a mop. Hide the pigs
			// panties! Yes very nice, "But what if another type of accumulator
			// only wants to find a single hint" you ask, and then I shoot you,
			// obviously, and make away with the pig. To the flintstonemobile!
			// I'm pretty sure that's a Chev. Yaba daba dunce! Gopher Cleetus?
			if ( accu.isSingle() )
				// SingleHintsAccumulator wants just the first hint encountered
				// note the "normal" short-circuiting boolean-or || operator
				return search(grid.rows, grid.cols, accu, occurances)
					|| search(grid.cols, grid.rows, accu, occurances);
			else // HintsAccumulator wants all available hints
				// note the "unusual" non-short-circuiting bitwise-or | operator
				return search(grid.rows, grid.cols, accu, occurances)
					 | search(grid.cols, grid.rows, accu, occurances);
		} catch ( UnsolvableException ex ) {
			// from getHintsImpl => createPointFishHint => interleave => build
			// catch UE to print grid with MIA region which can't exist! I don't
			// understand what's going on yet. Need to repeat to get more info.
			StdErr.carp("Fisherman drowned!", ex, grid);
			throw ex;
		}
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
	 * Well, either that or Douglas Adams is a deslyxic bisexual unicorn who
	 * moved it ALL down to the bottom peg, despite his brother having his hair
	 * cut yesterday, because there are only 5 states of matter, his towel is
	 * on strike, and we don't HAVE a brother. Oh Never MIND! Sheesh! Let us
	 * merely eliminate some bloody v's. They've ALL got to go! Ha ya!
	 */
	private boolean search(ARegion[] bases, ARegion[] covers, IAccumulator accu
			, int[] occurances) {
		// ANSI-C style variables, for performance
		int[] indexes;
		int n, i, card, coverBits, baseBits;
		int redBits; // bitset of the indices of removable v's in this cover
		// candiBits := the index of each base (in bases) which has 2..$degree
		// possible positions for value. They're only "candidates" for now.
		final int[] candiBits = this.candiBits; // a bitset
		final int degree = this.degree;
		final int[] thePA = this.thePA; // the permutations array
		final int maxOccurences = this.maxOccurences + 1; // SNEEKY!
		final int degreePlus1 = this.degreePlus1;
		// presume that no hint will be found
		boolean result = false;
		// the removable (red) potential cell values
		Pots reds = null;
		// foreach possible value
		for ( int v=1; v<10; ++v ) { // 9
			// efficiency: fish possible if grid has atmost degree*2 v's.
			if ( occurances[v] < maxOccurences ) {
				// candiBits := array of indexes of bases with 2..degree v's.
				n = 0; // number of candidate regions
				for ( i=0; i<9; ++i ) // 9*9 = 81
					if ( (card=bases[i].indexesOf[v].size)>1 && card<degreePlus1 )
						candiBits[n++] = i;
				if ( n >= degree ) { // there are sufficient bases
					// we look for $degree positions of $v in $degree $bases;
					// foreach possible combination of $degree bases
					for ( int[] perm : new Permutations(n, thePA) ) { // nb thePA is an int[degree]
						// build the indexes of the covers in the bases.
						coverBits = 0; // A better name might be anIntBitsetContainingTheIndicesOfCellsWhichMaybeTheFishCandidateValueInTheCurrentBasesAllGatheredTogetherInACaveAndGroovingWithAPict, but I like plain old coverBits, because it's short, so it fits on a line, and I can spell it, and the name is somewhat descriptive of it's use, as if that wasn't immediately obvious to absolutely everybody at first glance. Sigh.
						for ( i=0; i<degree; ++i )
							coverBits |= bases[candiBits[perm[i]]].indexesOf[v].bits;
						// if not degree positions for v in these degree bases
						// then there's no basic fish here.
//System.out.print("complete: "+v+":");
//for ( i=0; i<degree; ++i )
//	System.out.print(" "+bases[candiBits[perm[i]]].id);
//System.out.println(" = "+diuf.sudoku.Indexes.toString(coverBits));
						if ( VSIZE[coverBits] == degree ) {
							// ------------------------------------------------------------
							// There are $degree positions for $v in $degree bases, but
							// are there any $v's in covers and not bases to eliminate?
							// ------------------------------------------------------------
							// extract the indexes of the bases in the covers.
							// NOT done above coz the hit rate is so low it's slower.
							baseBits = 0;
							for ( i=0; i<degree; ++i )
								baseBits |= ISHFT[candiBits[perm[i]]];
							// get removable (red) potentials = v's in covers and not bases
							indexes = INDEXES[coverBits];
							for ( i=0; i<degree; ++i )
								if ( (redBits=covers[indexes[i]].indexesOf[v].bits & ~baseBits) != 0 ) {
									if ( reds == null )
										reds = new Pots(8); // observed max 9
									reds.populate(covers[indexes[i]].cells, redBits, v);
								}
							// there's nothing to remove at least 95% of the time.
							if ( reds != null ) {
								// create the hint and add it to the IAccumulator
								result = true;
								if ( accu.add(createHint(v, reds, bases, baseBits, covers, coverBits)) )
									return result;
								reds = null; // clean-up for next time
							}
						}
					} // next value
				}
			}
		} // next permutation
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
			greens.populate(bases[i].cells, coverBits, VSHFT[v], v);
		// build and return the new hint
		return new BasicFishHint(this, reds, v, greens, ""
				, Regions.list(degree, bases, baseBits)
				, Regions.list(degree, covers, coverBits));
	}

}
