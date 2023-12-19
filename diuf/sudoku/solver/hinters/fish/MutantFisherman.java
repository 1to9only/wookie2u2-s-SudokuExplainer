/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * Based on HoDoKu FishSolver, so here is hobiwans licence statement:
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
 */
package diuf.sudoku.solver.hinters.fish;

import static diuf.sudoku.Grid.*;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.BITS9;

/**
 * MutantFisherman implements Mutant.*fish Sudoku solving techniques.
 * <pre>
 * {@link Tech#MutantSwampfish} degree = 2
 * {@link Tech#MutantSwordfish} degree = 3
 * {@link Tech#MutantJellyfish} degree = 4
 * </pre>
 * <p>
 * <pre>
 * The problem with DeathStars,
 * is they are expensive.
 * Let them burn coal.
 * I fixed it!
 * ~KRC.
 *
 * WARN: O( exp(degree) * exp(degree) ) DeathStar!
 * Output from {@link diuf.sudoku.utils.Permutations#main}
 * Standard fish:
 * degree: exp(degree)*exp(degree) = number of possible distinct combinations
 * 2: 36*36         =       1,296
 * 3: 84*84         =       7,056
 * 4: 126*126       =      15,876
 * Franken fish:
 * 2: 153*153       =      23,409
 * 3: 816*816       =     665,856
 * 4: 3,060*3,060   =   9,363,600
 * Mutant fish:
 * 2: 351*351       =     123,201
 * 3: 2,925*2,925   =   8,555,625
 * 4: 17,550*17,550 = 308,002,500 hence MutantJellyfish = RUSSIAN RULOTTO!
 * </pre>
 * <p>
 * The degree is the number of base regions, and therefore cover regions. The
 * first rule of Fish is that there are ALWAYS the same number of covers as
 * there are bases. Get that wrong and it all goes tits-up.
 * <p>
 * FrutantFisherman (Franken/Mutant) Fisherman started life as a copy-paste of
 * ComplexFisherman, which started as HoDoKus FishSolver with SudokuExplainers
 * model, hints, and plumbing; importing all HoDoKu dependencies. I exhumed
 * Krakens coz I cant work-out how to implement them with my chainers.
 * As always: Kudos to hobiwan. Mistakes are mine. ~KRC
 * <p>
 * Fish have there own language. Heres a run-down, to define my terminology.
 * Other folks have other perspectives. Theres more than one way to do it.
 * And theres more than one Fish language. Techies disagree on everything.
 * Thats cool. Its how we workout how to do it better.
 * <p>
 * Basic Fish: $degree base regions (rows/cols) share $degree positions for a
 * value, and every region has exactly one instance of each value, hence value
 * is "locked into" these base regions, therefore value can be eliminated from
 * all other places in the cross-regions (cols/rows), called "covers".
 * <p>
 * Degree is the size of my fish, ie num bases, hence also num covers:<ul>
 * <li>The fish size is stored in super.degree, from the passed Tech.
 * <li>2 regions are Swampfish, not X-Wing. Recall Yoda raising Lukes X-Wing
 * from the swamp. I demur because fish should have fish-names, not wing-names;
 * especially in an application that already has wings. Wonky nominatives are
 * a pain in the ass when all you have to go-on are names. Name your son Mr Ed,
 * just for a laugh introduce him to some blind c__ts. Nuff said.
 * <li>3 regions are called Swordfish.
 * <li>4 regions are called Jellyfish.
 * <li>Larger fish {5=Squirmbag, 6=Whale, 7=Leviathan} are all degenerate
 * (combinations of simpler patterns) so I do not (though I can) seek them.
 * </ul>
 * <p>
 * So, the regions we look in are called "bases". The crossing regions that we
 * actually eliminate from are called the "covers". So we: <ul>
 * <li>first search the rows (bases), and the cols are our covers;
 * <li>then bases are cols, and covers are rows (they swap roles).
 * <li>Mutant Fish (below) need only one search (includes all bases).
 * </ul>
 * <p>
 * All the "complex" fish types have fins (below) to expand the definitions of
 * basic fish to find more hints, but with reduced eliminations, so we are
 * doing more work for less pay. S__t keeps getting harder. Live with it.
 * <p>
 * A Finned Fish is a basic fish just with one-or-more fins. A fin is an extra
 * position for the fish value in a base region (not in a cover). Fins expand
 * the number of fish but reduce the number of eliminations; because each
 * elimination must see all the fins. The logic is that the value MUST be
 * either in the fish or one of the fins, so if all possible places see any
 * victim then that victim is a goner. Finned Draculla only does gang-bangs.
 * <p>
 * I call vs (cells which maybe v) in both bases and covers "corners". Sashimi
 * Fish rely on a fin (else they are degenerate) to allow us to drop a corner
 * from the normal Fish pattern: ie ONE cover may intersect ONE base. Sashimi
 * Fish are found in the Finned Fish search; other fish types do not actively
 * exclude Sashimi fish, so they do happen, rarely. I choose to include the
 * Sashimi in the fish type name: so Franken Sashimi it is, hmm. ~Yoda
 * <p>
 * The Frutant fish types are:<ul>
 * <li>Franken Fish has boxs as bases XOR covers.
 * <li>Mutant Fish has all region-types as both bases and covers:<ul>
 *  <li>so boxs as both bases AND covers
 *  <li>or rows as both bases AND covers
 *  <li>or cols as both bases AND covers
 *  <li>so theres ONE all-in mutant search (an all-in orgy).
 *  </ul>
 * </ul>
 * <p>
 * KrakenFisherman is fish with Chains: If each v in current (last added) cover
 * and every fin has a static XY Forcing Chain to a victim, then elim victim.
 * <p>
 * 2020-09-28 after some hard-thinking I dropped Kraken for now in order to get
 * the rest working first. The plan is to come back and nut-out how to find
 * kraken-chains efficiently. Hobiwans design relies on doing the table-search
 * for all chains ONCE, and re-using that and the ALS-search results in Kraken
 * search; I need a results cache to search each corner and each fin. By "each
 * corner" I mean each corner in the first base with > 1 corners, ie every
 * possible Fish configuration. I have no-idea how to handle ALSs.
 * <p>
 * 2020-09-29 Bit the bullet and ripped-out Kraken Fish completely, the plan
 * now is to implement Kraken independently in KrakenFisherman, which will do
 * its own chaining (ie have no dependency on either HoDoKu or my Chainers).
 * <p>
 * 2020-10-04 Progress update: Finned and Franken work pretty well, but Mutant
 * still finds just one hint (a Franken Jellyfish) where HoDoKu finds hundreds
 * of hints, and I cant see (yet) where Ive gone wrong.
 * <p>
 * <pre>
 * 2020-11-29 Progress update: Mutants are FAR too slow, esp Jellyfish
 *    10,916,885,082 4433   2,462,640  0               0 Mutant Swampfish    10 seconds
 *   215,801,070,442 4433  48,680,593  2 107,900,535,221 Mutant Swordfish  3.58 minutes
 * 2,091,340,950,760 4431 471,979,451 19 110,070,576,355 Mutant Jellyfish 34.85 minutes
 *
 * I cannot see any opportunities to speed-up the actual code, so Im left with
 * less savoury options. I could enspeedonate it for top1465 only, as per A*E,
 * but that stinks. Or I could just have another look at speeding-up mutants.
 *
 * 13:31 with the row/col AND box filter reinstated and logic fixed
 *   137,405,847,323 4409  31,164,855  2 68,702,923,661 Mutant Swordfish  2.28 minutes
 * 1,608,436,992,524 4407 364,973,222 18 89,357,610,695 Mutant Jellyfish 26.80 minutes
 *
 * 14:53 it cant be done any faster. sigh.
 *   140,582,626,255 4409  31,885,376  2 70,291,313,127 Mutant Swordfish
 * 1,624,673,985,496 4407 368,657,586 18 90,259,665,860 Mutant Jellyfish
 * pzls        total (ns) (mm:ss)         each (ns)
 * 1465 2,154,956,584,000 (35:54)     1,470,960,125
 *
 * 2020-12-02 09:53 reinstated mutants, without the filter
 *    11,373,373,900 4409   2,579,581  0               0 Mutant Swampfish
 *   221,869,645,200 4409  50,321,988  2 110,934,822,600 Mutant Swordfish
 * 2,148,729,373,100 4407 487,571,902 19 113,091,019,636 Mutant Jellyfish
 *
 * 2020-12-03 07:06 modified cover filter is SLOWER
 *    12,584,486,700 4409   2,854,272  0               0 Mutant Swampfish
 *   254,482,656,400 4409  57,718,905  2 127,241,328,200 Mutant Swordfish
 * 2,479,199,708,600 4407 562,559,498 19 130,484,195,189 Mutant Jellyfish
 *
 * 2020-12-03 07:?? HACK base filter and modified cover filter
 *     6,145,725,700 4415   1,392,010  0              0 Mutant Swampfish
 *    75,165,147,100 4415  17,024,948  1 75,165,147,100 Mutant Swordfish
 *   493,146,482,300 4414 111,723,262 11 44,831,498,390 Mutant Jellyfish
 *
 * 2020-12-03 08:06 HACK base filter only (remove cover filter coz it slower)
 *     4,990,931,400 4415  1,130,448  0              0 Mutant Swampfish
 *    60,627,458,000 4415 13,732,153  1 60,627,458,000 Mutant Swordfish
 *   394,362,063,900 4414 89,343,467 11 35,851,096,718 Mutant Jellyfish
 *
 * 2023-06-02 Implemented "standardised" validators, and discovered invalid
 * Franken/Mutant hints, so now searchCovers, for Franken/Mutant accepts covers
 * with multiple base intersections, finding less hints, but none invalid. I
 * also back-implement Superfisch config to get maxFins and maxEndofins, and
 * the maxFishSizes array: a fishSize for each of the three fishTypes: {4,3,2}
 * is fast enough.
 *
 * 2023-06-11 Post Superfischerman, I revert to fishing for size 2..degree.
 * hobiwans ComplexFisherman did it this way. I was too eager to force it into
 * SEs existing framework, with everything divided into discreet Techs. My bad.
 * So now the user selects the tech of the maximum size of fish to seek for a
 * type, and also type for size, so when Franken Jellyfish is wanted the GUI
 * automatically unselects other Jellyfish, and the other Frankens; so the
 * ComplexFisherman(FrankenJellyfish) finds both finned and franken swampfish,
 * swordfish, and jellyfish; as it did back when hobiwan was running things. I
 * was simply too dumb to understand and respect genius. Now I appreciate that.
 *
 * 2023-07-27 clean-up and make faster. Numerous fields are mashed together.
 * Considered hijacking to save RAM, but yeah na. Its already a horror show.
 *
 * 2023-07-28 Split ComplexFisherman into FinnedFisherman and FrutantFisherman,
 * to make them both a bit faster by eliminating all the isFrutant switching.
 * I use FrankenSwordfish in the batch, as best compromise between speed and
 * accuracy. FinnedFisherman is faster but not routinely performance tested.
 * Note that FrutantFisherman handles all kinds of complex fish (inc Finned);
 * it has endofins which only exist in Franken and Mutant fish with bases that
 * overlap rows/cols, producing endofins. FinnedFisherman has no endofins, and
 * also no Sashimi-check.
 * WEE:   5,679,003,200  4012   1,415,504  235   24,165,971 FinnedJellyfish
 * MID:  14,919,984,400  3972   3,756,290  322   46,335,355 FrankenSwordfish
 * LRG:  57,081,727,400  3972  14,371,029  405  140,942,536 FrankenJellyfish
 * BIG: 301,157,710,600  3984  75,591,794  415  725,681,230 MutantJellyfish
 *
 * 2023-08-02 No need for ridx (region indexes); just use baseVsM* instead.
 * Sashimi detection: raise the floor when you hit THE sashimi cover.
 * Cache vs in this potential cover in skM*, to avert repeat-deindexing.
 * PRE: 14,919,984,400  3972  3,756,290  322  46,335,355 FrankenSwordfish
 * PST: 12,852,482,200  3972  3,235,770  322  39,914,540 FrankenSwordfish
 *
 * 2023-10-07 Replace basesBits (int) with basesUsed (boolean[27]) for speed.
 * PRE: 16,367,555,500  4539  3,605,982  331  49,448,808 J17 OCT05 09:59
 * PST: 12,630,903,000  4539  2,782,750  331  38,159,827 J17 OCT07 09:00
 * So the latest version takes 77.17% of the time for same elims. Bargain!
 *
 * 2023-10-07 Split FrutantFisherman into FrankenFisherman and MutantFisherman.
 *
 * 2023-10-27 bring MutantFisherman upto-date with Finned and Franken Fisherman
 * create/destroy vars in findHints to reduce heap footprint between calls.
 * </pre>
 *
 * @author Keith Corlett 2020-09-24
 */
public class MutantFisherman extends AComplexFisherman
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[32];

	// this array puts the regions in the "normal" order for fisherman.
	// order is significant coz boxs,rows,cols (Grid.regions order) misses a
	// couple of the testcase hints, but it finds MORE hints in the batch.
	private static final int[] REGION_INDEXES = {
		 9, 10, 11, 12, 13, 14, 15, 16, 17, // rows
		18, 19, 20, 21, 22, 23, 24, 25, 26, // cols
		 0,  1,  2,  3,  4,  5,  6,  7,  8  // boxs
	};

	/**
	 * The Constructor.
	 *
	 * @param tech Tech.Finned/Franken/Mutant Swamp/Sword/Jellyfish
	 */
	public MutantFisherman(final Tech tech) {
		this(tech, true);
	}

	/**
	 * Protected constructor, with useCache. LogicalSolverBuilder want demands
	 * that hinters expose ONE public constructor taking no-args or Tech. Hence
	 * all "special" hinters are protected. So I'm special!
	 *
	 * @param tech
	 * @param useCache
	 */
	protected MutantFisherman(final Tech tech, final boolean useCache) {
		super(tech, useCache);
		assert !tech.name().startsWith("Kraken");
	}

	/**
	 * Find all fish upto my complexity and size in the $grid, adding any hints
	 * to $accu. MutantFisherman finds Finned, Franken and Mutant fish. My size
	 * is the Tech.degree that was passed to my constructor.
	 * <p>
	 * foreach candidate in 1..9: {@link #search(boolean)} for Fish.
	 * Search rows first, then (except Mutant) cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 *
	 * @return were any hint/s found
	 */
	@Override
	protected boolean findComplexFishHints() {
		boolean result = false;
		Idx vs;
		ARegion r;
		int value = 1;
		do {
			// search all combinations of bases and covers for mutant fish
			numBases = numAllCovers = 0;
			for ( int i : REGION_INDEXES ) // rows, cols, boxs
				if ( (r=regions[i]).numPlaces[value] > 1 ) {
					vs = r.idxs[value];
					// bases
					baseUnits[numBases] = r.index;
					unitBases[r.index] = numBases;
					baseVsM0[numBases] = vs.m0;
					baseVsM1[numBases++] = vs.m1;
					// covers
					allCoverUnits[numAllCovers] = r.index;
					allCoverVsM0[numAllCovers] = vs.m0;
					allCoverVsM1[numAllCovers++] = vs.m1;
				}
			if ( numBases>degreeMinus1 && numAllCovers>degreeMinus1
			  && search(value) ) {
				result = true;
				if(oneHintOnly) break; // exit-early
			}
			// stop if the user has interrupted the Generator
			hinterrupt();
		} while (++value < VALUE_CEILING);
		return result;
	}

	/**
	 * Search all combinations of 2..{@link #degree} in 27 bases, which <br>
	 * searches all combinations of baseLevel in 27 covers, which <br>
	 * is too ____ing slow. Get over it.
	 * <pre>
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 * Complex Fisherman is an exponential (slow) operation.
	 * where: fak is factorial(i) which is 1+2+3...+i
	 *        N is number of elements in the master set (numBases/Covers)
	 *        K is number of elements per combination (degree)
	 * exponential = (long)(fakN / (fakNMinusK * fakK));
	 * and covers also has the same exponential problem;
	 * Hence exp(bases) * exp(covers) is too ____ing slow!
	 * And there is absolutely nothing you can do about it. Sigh.
	 * Mutant fish:
	 * 2: 351*351       =     123,201
	 * 3: 2,925*2,925   =   8,555,625
	 * 4: 17,550*17,550 = 308,002,500 &lt;&lt;==== That's TOO BIG !!!
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 * </pre>
	 *
	 * @param value the Fish candidate value (ie the value to search on)
	 * @return were hint/s found
	 */
	private boolean search(final int value) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WARN: Its not you. This Algorithm is a self-cleaning cluster____.
		// My implementation doesn't help. So ____ with it at your own peril!
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		StackEntry pB,cB, pC,cC; // previous/current Bases/Covers StackEntry
		// these are all Exploded Idxs
		long bsM0; int bsM1; // cB.vsM* sans deref cB repeatedly for speed
		long efM0; int efM1; // endofins: vs in 2+ bases (bases may overlap)
		long fnM0; int fnM1; // fins: extra vs in bases
		long cvM0; int cvM1; // cC.vsM* sans deref cC repeatedly for speed
		long skM0; int skM1; // sharks: (cannabals) vs in 2+ covers
		long dlM0; int dlM1; // deletes: indices to eliminate from
		int array[] // temporary array pointer
		, baseLevel // depth in the baseStack, ie number of bases in baseSet.
		, baseIndex // index of the current base in grid.regions
		, numEndofins // number of endofins
		, ii, jj // indexes
		, coverIndex // index of the current cover in grid.regions
		, coverLevel // current coversStack level, ie coversSet size
		, numCovers // the number of covers in the covers array
		, floor // partial-calculation of the last cover index
		, count // array length (because I am too lazy to find a free variable)
		;
		// cover grid.regions indexes
		final int[] coverUnits = new int[NUM_REGIONS];
		// cache of vs in candidate bases, for speed. Arrays of exploded Idxs.
		// index is candBaseIndex: 0..degreeMinus1. This is fast, but ugly.
		final long[] cbM0 = new long[degree];
		final int[] cbM1 = new int[degree];
		// indices of grid.cells maybe v (Fish candidate value) in allCovers
		final long[] coverVsM0 = new long[NUM_REGIONS];
		final int[] coverVsM1 = new int[NUM_REGIONS];
		// permutation stack for the base region search.
		// my .xxM* fields are the endofins (vs in 2+ bases).
		final StackEntry[] baseStack = new StackEntry[degreePlus1];
		for ( int i=0; i<degreePlus1; ++i )
			baseStack[i] = new StackEntry();
		// permutation stack for the cover region search.
		// my .xxM* fields are the sharks (aka cannibals).
		final StackEntry[] coverStack = new StackEntry[degreePlus1];
		for ( ii=0; ii<degreePlus1; ++ii )
			coverStack[ii] = new StackEntry();
		int usedBases = 0; // used bases in a 27bit bitset
		int usedCovers = 0; // used covers in a 27bit bitset
		// presume that no hints will be found
		boolean result = false;
		// try all combinations of base regions
		// the permutations stack starts at level 1 (0 is just a stopper)
		for ( baseLevel=1; ; ) {
			// fallback a baseLevel if no unit is available at this baseLevel
			while ( (cB=baseStack[baseLevel]).index >= numBases ) {
				// reset this levels index, for next-time
				cB.index = 0;
				// fallback one level at a time to maintain basesUsed
				// nb: cB.prevIndex is -1 at endOfLevel, which is now "empty",
				// coz its pre-existing bS[baseLevel].index is >= numBases.		// Fully Liberal!
				// This if is a bit slow, but what else? And it's "only" bases.
				// This is if is NOT in searchCovers, where it really matters.
				if ( cB.prevIndex > -1 ) {
					usedBases &= ~IDX_SHFT[cB.prevIndex];
					cB.prevIndex = -1;
				}
				// fallback one level, stopping if that is level0
				if ( --baseLevel < 1 ) // ie<=0 (level 0 is just a stopper!)
					return result; // weve tried all combos!
			}
			// unuse the-previous-base-at-this-level
			if ( cB.prevIndex > -1 ) {
				usedBases &= ~IDX_SHFT[cB.prevIndex];
				cB.prevIndex = -1;
			}
			// get the next base (must exist or we would have fallen back)
			// remember prevIndex to unuse this base later
			// add this base to usedBases, to remember that this base is used.
			usedBases |= IDX_SHFT[cB.prevIndex=baseUnits[baseIndex=cB.index++]];
			// the previous BaseStackEntry, containing the existing candidates
			// and endoFins sets (aggregates of ALL so-far).
			pB = baseStack[baseLevel - 1];
			// v is shorthand for the fish candidate value
			// current vs = previous vs | vs in this base
			bsM0 = cB.vsM0 = pB.vsM0 | baseVsM0[baseIndex];
			bsM1 = cB.vsM1 = pB.vsM1 | baseVsM1[baseIndex];
			// endofins are vs that are in more than one base, in Franken and
			// Mutant fish, that include boxs as bases (ergo vs in overlap).
			// endofins: current = previous | (vs in prev bases & this base)
			efM0 = cB.xxM0 = pB.xxM0 | (pB.vsM0 & baseVsM0[baseIndex]);
			efM1 = cB.xxM1 = pB.xxM1 | (pB.vsM1 & baseVsM1[baseIndex]);
			if ( (efM0|efM1) < 1L )
				numEndofins = 0;
			else
				numEndofins = Long.bitCount(efM0)+Integer.bitCount(efM1);
			// if number of bases in 2..degree (legal size to degree)
			if ( baseLevel>1 && baseLevel<degreePlus1
			  // and theres no endofins
			  && ( numEndofins == 0
				// or not too many of them
				|| ( numEndofins < maxEndofins // ceiling
				  // which share common buds that maybe v (psbl elims).
				  // nb: slow, but faster than pointless searchCovers.
				  && anyCmnBuds(efM0,efM1, idxs[value].m0&~efM0
										 , idxs[value].m1&~efM1) ) )
			) {
				// Enough eligible covers to form a fish? Done here coz it
				// depends on current usedBases. To fit a cover must:
				// * not be a current base; and
				// * intersect (share candidate with) the usedBases; and
				// * ONE "Sashimi" cover may intersect ONE base, the rest
				//   intersect multiple bases.
				// NOTE: poor Sashimi handling is a choice. The correct answer
				// (search each Sashimi cover, one by one) finds ~6% more elims
				// but takes nearly twice as long.
				jj = ii = 0;
				do
					if ( (floor=usedBases>>ii & BITS9) > 0 ) {
						array = INDEXES[floor];
						count = array.length;
						floor = 0; // floor is the index here down
						do {
							dlM1 = unitBases[ii + array[floor]];
							cbM0[jj] = baseVsM0[dlM1];
							cbM1[jj++] = baseVsM1[dlM1];
						} while ( ++floor < count );
					}
				while ( (ii+=REGION_SIZE) < NUM_REGIONS );
				assert jj == baseLevel;
				ii = floor = numCovers = 0;
				do
					if ( (usedBases & IDX_SHFT[allCoverUnits[ii]]) < 1 ) {
						skM0 = allCoverVsM0[ii];
						skM1 = allCoverVsM1[ii];
						count = jj = 0;
						do
							if ( ( (cbM0[jj] & skM0)
								 | (cbM1[jj] & skM1) ) > 0L )
								++count;
						while ( ++jj < baseLevel );
						if ( count > floor ) {
							if(count == 1) floor = 1; // ONE sashimi cover
							coverUnits[numCovers] = allCoverUnits[ii];
							coverVsM0[numCovers] = skM0;
							coverVsM1[numCovers++] = skM1;
						}
					}
				while ( ++ii < numAllCovers );
				// need atleast baseLevel covers to form a Fish.
				// A fish has the same number of covers as bases.
				if ( numCovers >= baseLevel ) {
					// partial-calculation of the last cover index.
					// nb: the - 1 is for faster > as apposed to >=.
					floor = numCovers - baseLevel - 1;
//<HAMMERED comment="top1465 iterates below loop 250+ MILLION times, so no continues, labels, variables, terniaries, invocations, or anything at-all-slow, Everything here needs to be as fast as possible; preferably faster.">
					// foreach each distinct combo of covers
					// coverLevel: depth in coverStack is 1-based (0 is just a stopper)
					COVER_COMBOS: for ( coverLevel=1; ; ) {
						// while coverLevel exhausted fallback a coverLevel.
						// Exhausted means theres no more covers to be searched.
						// Calculating the last index at this coverLevel is tricky, it is
						// impossible to calculate quickly, this is the best I can do.
						// FYI: cC (current Cover) is also set here
						// nb: faster to do all the math on one side of the equation
						while ( (cC=coverStack[coverLevel]).index - coverLevel > floor ) {
							// reset this levels index, for next-time
							// WARN: This absolutely is necessary! Sigh.
							cC.index = 0;
							// unuse the previous cover, which must exist, because this
							// level is exhausted, and levels are not empty.
							usedCovers &= ~IDX_SHFT[cC.prevIndex];
							cC.prevIndex = -1;
							// fallback
							if ( --coverLevel < 1 ) // 0 is just a stopper
								break COVER_COMBOS; // all combos searched
						}
						// unuse the previous cover
						if ( cC.prevIndex > -1 ) {
							usedCovers &= ~IDX_SHFT[cC.prevIndex];
							cC.prevIndex = -1;
						}
						// get next coverIndex (must exist)
						// remember prevIndex to unuse this cover later
						// remember that this cover is used
						usedCovers |= IDX_SHFT[cC.prevIndex=coverUnits[coverIndex=cC.index++]];
						// set the previousCover (to agglomerate all covers)
						pC = coverStack[coverLevel - 1];
						// current vs = previous vs | vs in this cover (agglomerate)
						cvM0 = cC.vsM0 = pC.vsM0 | coverVsM0[coverIndex];
						cvM1 = cC.vsM1 = pC.vsM1 | coverVsM1[coverIndex];
						// if this cover has candidates in common with the existing covers
						// then those candidates become possible eliminations, which I call
						// sharks because its shorter than cannabilistic. The Great White
						// Shark: a non-placental bearthing predatory young. Do the math!
						// sharks: current = previous | (vs in prev covers & this cover)
						cC.xxM0 = pC.xxM0 | (pC.vsM0 & coverVsM0[coverIndex]);
						cC.xxM1 = pC.xxM1 | (pC.vsM1 & coverVsM1[coverIndex]);
						// if we are still collecting covers
						if ( coverLevel < baseLevel ) {
							// move onto the next level
							// starting at the cell after the current cell at this level
							coverStack[++coverLevel].index = coverIndex + 1;
						} else {
							// same number of covers as bases (ie a legal fish)
							assert coverLevel == baseLevel; // never larger
							// psbl deletes = ((covers & ~bases) | endofins)
							dlM0 = ((cvM0 & ~bsM0) | efM0);
							dlM1 = ((cvM1 & ~bsM1) | efM1);
							// psbl shark victims = sharks in current covers
							skM0 = cC.xxM0;
							skM1 = cC.xxM1;
							// if any psbl deletes or psbl sharks
							if ( (dlM0|dlM1 | skM0|skM1) > 0L ) { //27bits
								// fins = endofins | (bases & ~covers)
								fnM0 = efM0 | (bsM0 & ~cvM0);
								fnM1 = efM1 | (bsM1 & ~cvM1);
								// if some fins
								if ( (fnM0|fnM1) > 0L
								  // but not too many of them
								  && Long.bitCount(fnM0)+Integer.bitCount(fnM1) < maxFins
								  // which have common buds that maybe v.
								  // nb: done last (min times) coz its slow.
								  && cmnBuds(fnM0,fnM1, idxs[value].m0&~fnM0
													  , idxs[value].m1&~fnM1)
								) {
									// deletes = psbl delete that see all fins
									dlM0 &= cmnBudsM0;
									dlM1 &= cmnBudsM1;
									// shark victims = sharks that see all fins
									skM0 &= cmnBudsM0;
									skM1 &= cmnBudsM1;
									// if any deletes or sharks
									if ( (dlM0|dlM1 | skM0|skM1) > 0L ) { //27bits
//</HAMMERED>
										// **************** FRUTANT FISCH *****************
										// FOUND a Franken/Mutant Fish!
										result = true;
										if ( accu.add(hint(value, usedBases
										, usedCovers, coverLevel
										, dlM0, dlM1 // deletes
										, skM0, skM1 // sharks
										, false // reject No deletes or sharks
										, fnM0, fnM1 // fins
										, cvM0, cvM1 // covers
										, bsM0, bsM1 // bases
										, efM0, efM1)) ) // endofins
											return result;
									}
								}
							}
						}
					}
				}
			}
			// if we are still collecting bases
			if ( baseLevel < degree
			  // and there are not too many endofins already
			  && numEndofins < maxEndofins ) { //ceiling
				// onto the next level (to add another base to the stack)
				baseStack[++baseLevel].index = baseIndex + 1;
				baseStack[baseLevel].prevIndex = -1;
			}
		}
	}

}
