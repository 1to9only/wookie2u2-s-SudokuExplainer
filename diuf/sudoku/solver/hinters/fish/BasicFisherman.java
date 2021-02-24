/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Indexes;
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
import java.util.List;


/**
 * BasicFisherman implements the basic Swampfish (nee X-Wing) (2), Swordfish (3)
 * and Jellyfish (4) Sudoku solving techniques. Basic means no fins, sashimi,
 * franken, mutant, or kraken; just $degree bases (rows/cols) having exactly
 * $degree possible positions for $degree values (no fins here) coz it's fast!
 * <p>
 * NOTE: I call X-Wing "Swampfish" because fish should have fish-names,
 * not wing-names. Recall: Yoda raises Lukes X-Wing from a Tatooeen swamp.
 * When I started-out I wrongly associated X-Wings with the other wings (went
 * looking for the XWing hinter in the wings package). Two intelligent beings
 * on a whole planet and they still couldn't agree on anything important.
 * Confused you are, hmmm.
 */
public final class BasicFisherman extends AHinter
//		implements diuf.sudoku.solver.IReporter
{
//private int chCnt=0, chPass=0;

	private final int maxOccurences;
	private final int[] coreIdxs = new int[9]; // candidate region indexes
	private final int[] thePA; // the permutations array, for Permutations.

	public BasicFisherman(Tech tech) {
		super(tech);
		assert tech.name().toLowerCase().contains("fish"); // smells like fish!
		// ComplexFisherman do these!
		assert !tech.name().startsWith("Finned");
		assert !tech.name().startsWith("Franken");
		assert !tech.name().startsWith("Mutant");
		assert !tech.name().startsWith("Kraken");
		// Swampfish=2, Swordfish=3, Jellyfish=4
		// do not try Smellyfish=5, I did, and it doesn't.
		assert degree>=2 && degree<=4;
		this.maxOccurences = 9 - degree*2;
		this.thePA = new int[degree];
	}

//	@Override
//	public void report() {
//		Log.teef("// "+tech.name()+" pass %,d of %,d = skip %4.2f\n"
//				, chPass, chCnt, Log.pct(chPass, chCnt));
//	}

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
				return findFish(grid.cols, grid.rows, accu, occurances)
					|| findFish(grid.rows, grid.cols, accu, occurances);
			else // HintsAccumulator wants all available hints
				// note the "unusual" non-short-circuiting bitwise-or | operator
				return findFish(grid.cols, grid.rows, accu, occurances)
					 | findFish(grid.rows, grid.cols, accu, occurances);
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
	 * amongst $degree bases (rows or cols), to remove all non-base occurrences
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
	 * merely eliminate some bloody v's. They've got to go! All of them! Ha ya!
	 */
	private boolean findFish(ARegion[] bases, ARegion[] covers
			, IAccumulator accu, int[] occurances) {

		// candiIdxs := the index of each base (in bases) which has 2..$degree
		// possible positions for value. They're only "candidates" for now.
		final int[] candiIdxs = this.coreIdxs;
		final int degree = this.degree;
		final int[] thePA = this.thePA; // the permutations array
		final int maxOccurences = this.maxOccurences;
		final int degreePlus1 = this.degreePlus1;
		// All ANSI-C style variables for performance
		int n, i, card, vs, basesIdxs;
		AHint hint;

		// presume that no hint will be found
		boolean result = false;

		// foreach possible value
		for ( int v=1; v<10; ++v ) { // 9
			// This is only for efficiency: fish possible if grid has atmost
			// (degree*2) positions for $v (ie atleast degree*2 non-positions).
			if ( occurances[v] > maxOccurences )
				continue;
			// candiIdxs := the index of each base (in bases) with 2..$degree
			// possible positions for $v. They're only "candidates" for now.
			n = 0; // number of candidate regions
			for ( i=0; i<9; ++i ) // 9*9 = 81
				if ( (card=bases[i].indexesOf[v].size)>1 && card<degreePlus1 )
					candiIdxs[n++] = i;
			if ( n < degree )
				continue; // insufficient bases

			// we look for $degree positions of $v in $degree $bases;
			// foreach possible combination of $degree bases
			for ( int[] perm : new Permutations(n, thePA) ) { // nb thePA is an int[degree]
				// combine the Indexes of v in this combination of bases,
				// ie: the indexes of the "covers" c/r's IN THE BASES.
				vs = 0; // A better name might be aBitsetContainingTheIndicesOfCellsWhichMaybeTheFishCandidateValueInTheseBases_WhoMayOrMayNotHaveActuallyShagged_SeveralSpecisOfSmallFuryAnimalsGatheredTogetherInACaveAndGroovingWithAPict_ButIAmPrettySureThereWasSomeTongueInvolvedSomewhere_AndMyCrotchCheeseHasHadADistinctlyAcridFlavorEverSince, but I like plain old vs, because it's short, so I can spell it, and somewhat descriptive of it's use, as if that wasn't immediately obvious to absolutely everybody at first glance. Sigh.
				for ( i=0; i<degree; ++i )
					vs |= bases[candiIdxs[perm[i]]].indexesOf[v].bits;
				// if not $degree positions for $v in these $degree bases then
				// there's no fish here (well there might be later, with fins,
				// but not now, coz we're keeping it as simple as possible.)
				if ( VSIZE[vs] != degree )
					continue; // Empty net!

				// ------------------------------------------------------------
				// Hint (possibly) found! There are $degree positions for $v in
				// $degree bases, but are there any covers $v's outside bases?
				// NB: performance isn't an issue from here-down.
				// ------------------------------------------------------------

				// extract the baseIdxs from the candiIdxs. This is NOT done in
				// the above loop coz hit rate so low it's actually slower.
				basesIdxs = 0;
				for ( i=0; i<degree; ++i )
					basesIdxs |= ISHFT[candiIdxs[perm[i]]];

				// create the hint (if any) and add it to the IAccumulator
				hint = createHint(
						  bases, new Indexes(basesIdxs)
						, covers, new Indexes(vs)
						, v
				);
				result |= (hint != null);
				if ( accu.add(hint) )
					return true;
			} // next value
		} // next permutation
		return result;
	}

	// NB: "covers" is the indexes of the covers c/r's IN THE BASES (r/c's).
	private AHint createHint(
			  ARegion[] bases, Indexes basesIdxs
			, ARegion[] covers, Indexes coversIdxs
			, int v
	) {
		final int baseBits = basesIdxs.bits;
		// build removable (red) Cell=>Values: v's in covers andNot bases
		Pots reds = null;
		// foreach index of the covers EXCEPT the indexes of bases
		ARegion cover; // the col/row
		int red; // indices of the red (removable) v's in this cover
		for ( int i : INDEXES[coversIdxs.bits] )
			if ( (red=(cover=covers[i]).indexesOf[v].bits & ~baseBits) != 0 ) {
				if(reds==null) reds = new Pots(16); // observed 9
				reds.populate(cover.cells, red, v);
			}
//++chCnt;
		// Swampfish pass 261 of 34,100 = 0.77%
		// Swordfish pass  92 of  6,313 = 1.46%
		// Jellyfish pass   4 of     88 = 4.55%
		if ( reds == null )
			return null; // Nothing to see here 95+% of the time. Sigh.
//++chPass;

		// build highlighted (green) potentials
		Pots greens = new Pots(degree*degree, 1F);
		// Populate greenPots with coversIdxs cells (in covers and bases).
		final int sv = VSHFT[v]; // shiftedValue: the int who walks
		for ( int i : INDEXES[basesIdxs.bits] )
			greens.populate(bases[i].cells, coversIdxs.bits, sv, v);
		// build the bases and covers collections
		List<ARegion> baseL = Regions.select(degree, bases, basesIdxs);
		List<ARegion> coverL = Regions.select(degree, covers, coversIdxs);
		// build the hint
		return new BasicFishHint(this, reds, v, greens, baseL, coverL, "");
	}

}
