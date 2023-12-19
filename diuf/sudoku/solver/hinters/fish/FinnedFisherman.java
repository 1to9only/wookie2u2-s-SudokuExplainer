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
import static diuf.sudoku.Idx.IDX_SHFT;
import diuf.sudoku.Tech;

/**
 * FinnedFisherman implements {@link Tech#FinnedSwampfish},
 * {@link Tech#FinnedSwordfish}, and {@link Tech#FinnedJellyfish} Sudoku
 * solving techniques. These are size-variants of the "finned fish" technique:
 * Swamp=2, Sword=3, Jelly=4; ie the number of bases (and hence also covers) in
 * each fish. Note that all complex fischermen find all fish upto and including
 * degree size, because its actually easier to find subsize fish "in passing".
 * <p>
 * FinnedFisherman started life as a copy-paste of ComplexFisherman, which in
 * turn was a copy-paste of HoDoKus FishSolver, modified to use SudokuExplainer
 * model, hints, and plumbing; and import all HoDoKu dependencies. Kudos to
 * hobiwan. Mistakes are mine. ~KRC
 * <p>
 * Fish have there own language. Heres a brief precis, defining terminology.
 * Other folks have other perspectives. Theres more than one way to do it.
 * And theres more than one Fish language. Techies never agree on anything.
 * <p>
 * Basic Fish: $degree base regions (rows/cols) share $degree positions for a
 * value, and every region has exactly one instance of each value, hence value
 * is "locked into" these base regions, therefore value can be eliminated from
 * all other places in the cross-regions (cols/rows) that are called "covers".
 * <p>
 * Degree is the size of my fish, ie num bases, hence also num covers:<ul>
 * <li>The fish size is stored in super.degree, from the passed Tech.
 * <li>2 regions are Swampfish, not X-Wing. Recall Yoda raising Lukes X-Wing
 * from the swamp. I demur because fish should have fish-names, not wing-names;
 * especially in an application that already has wings. Wonky nominatives are
 * a pain in the ass when all you have to go-on are names. Name your son Mr Ed,
 * just for a laugh introducing him to blind c__ts.
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
 * basic fish to find more hints, but with reduced eliminations, so were doing
 * more work for less pay, ergo s__t gets harder.
 * <p>
 * A Finned Fish is a basic fish just with one-or-more fins. A fin is an extra
 * position for the fish value in a base region (not in a cover). Fins expand
 * the number of fish but reduce the number of eliminations; because each
 * elimination must see all the fins. The logic is that the value MUST be
 * either in the fish or one of the fins, so if all possible places see any
 * victims then the victim gets it. Finned Draculla only does gang-bangs.
 * <p>
 * Sashimi Fish rely on a fin (else they are degenerate) to allow us to drop a
 * corner from the normal Fish pattern: ie ONE cover may intersect ONE base.
 * Sashimi Fish are found in the Finned Fish search; other fish types do not
 * actively exclude Sashimi fish, so they do happen, rarely. I choose to
 * include the Sashimi in the fish type name Franken Sashimi.
 * <p>
 * Franken Fish adds a box to the bases OR the covers.
 * <p>
 * Mutant Fish adds boxes to the bases AND the covers. An all in orgy.
 * <p>
 * KrakenFisherman is fish with Chains: If each fish configuration and every
 * fin has a static XY Forcing Chain to the same victim, then kill it.
 * <pre>
 * 2023-07-28 Split from ComplexFisherman to avoid all those isFrutant switches
 * which slow us down marginally.
 * </pre>
 *
 * @author Keith Corlett 2020-09-24
 */
public class FinnedFisherman extends AComplexFisherman
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[7];

	/**
	 * The Constructor.
	 * @param tech Tech.Finned/Franken/Mutant Swamp/Sword/Jellyfish
	 */
	public FinnedFisherman(final Tech tech) {
		this(tech, true);
	}

	/**
	 * Protect constructor, with useCache. LogicalSolverBuilder want demands
	 * that hinters expose one only public constructor, taking no-args or Tech
	 * only. Hence all "special" constructors are protected.
	 *
	 * @param tech
	 * @param useCache
	 */
	protected FinnedFisherman(final Tech tech, final boolean useCache) {
		super(tech, useCache);
		assert tech.name().startsWith("Finned");
	}

	/**
	 * Find all Finned fish upto my size in the $grid, adding any hints to
	 * $accu. FinnedFisherman finds only Finned fish. My size is Tech.degree
	 * that was passed to my constructor.
	 * <p>
	 * foreach candidate in 1..9: {@link #search(boolean)} for Fish.
	 * Search rows first, then (except Mutant) cols.
	 * If accu.isSingle then exit-early when the first hint is found.
	 *
	 * @return were any hint/s found
	 */
	@Override
	protected boolean findComplexFishHints() { // 4,426
		boolean result = false;
		int value = 1;
		do {
			// search rows
			numBases = numAllCovers = 0;
			addRegions(rows, cols, value);
			if ( numBases>degreeMinus1 && numAllCovers>degreeMinus1
			  && search(value) ) {
				result = true;
				if(oneHintOnly) break; // exit-early
			}
			// search cols
			numBases = numAllCovers = 0;
			addRegions(cols, rows, value);
			if ( numBases>degreeMinus1 && numAllCovers>degreeMinus1
			  && search(value) ) {
				result = true;
				if(oneHintOnly) break; // exit-early
			}
			hinterrupt(); // stop if user interrupts Generator
		} while (++value < VALUE_CEILING);
		return result;
	}

	/**
	 * Search all possible combinations of 2..{@link #degree} base regions.
	 *
	 * @param value the Fish candidate value (the value to search on)
	 * @return were hint/s found
	 */
	private boolean search(final int value) {
		StackEntry pB,cB, pC,cC; // previous/current Bases/Covers StackEntry
		int baseLevel // current depth in the baseStack
		, baseIndex // index of the current base in Grid.regions
		, ii // the ubiquitous index
		, lastCoverIndex // partial calculation of the last cover index
		, numCovers // the number of covers in the coverUnits and Vs arrays
		, coverIndex // index of the current cover in Grid.regions
		, coverLevel // current depth in the coversStack
		;
		// these are all Exploded Idxs
		long vsM0; int vsM1; // vs in the current bases, sans cB dereference
		long fnM0; int fnM1; // exploded Idx of fins (extra vs in bases)
		long dlM0; int dlM1; // deletes: indices of potential eliminations
		long skM0; int skM1; // sharks: cannabilistic eliminations
		// array of current cover regions (indices in grid.regions)
		final int[] coverUnits = new int[NUM_REGIONS];
		// indices of grid.cells maybe v (Fish candidate value) in allCovers
		final long[] coverVsM0 = new long[NUM_REGIONS];
		final int[] coverVsM1 = new int[NUM_REGIONS];
		// recursion stack for the base region search
		final StackEntry[] baseStack = new StackEntry[degreePlus1];
		for ( int i=0; i<degreePlus1; ++i )
			baseStack[i] = new StackEntry();
		// the recursion stack for the cover region search
		final StackEntry[] coverStack = new StackEntry[degreePlus1];
		for ( int i=0; i<degreePlus1; ++i )
			coverStack[i] = new StackEntry();
		int usedBases = 0; // bitset of bases used
		int usedCovers = 0; // bitset of covers used
		// presume that no hints will be found
		boolean result = false;
		// the permutations stack starts at level 1 (0 is just a stopper)
		// try all combinations of base regions
		BASE_COMBOS: for ( baseLevel=1; ; ) {
			// while level is exhausted fallback a level
			while ( (cB=baseStack[baseLevel]).index >= numBases ) {
				cB.index = 0;
				if ( cB.prevIndex > -1 ) {
					usedBases &= ~IDX_SHFT[cB.prevIndex];
					cB.prevIndex = -1;
				}
				if ( --baseLevel < 1 )
					return result; // all combos examined
			}
			// unuse the-previous-base-at-this-level
			if ( cB.prevIndex > -1 )
				usedBases &= ~IDX_SHFT[cB.prevIndex];
			// get next baseIndex (exists or we would have fallen back)
			// use this region as a base
			// remember prevIndex to unuse this base later
			usedBases |= IDX_SHFT[cB.prevIndex=baseUnits[baseIndex=cB.index++]];
			// the previous BaseStackEntry, containing the existing candidates
			// and endoFins sets (aggregates of ALL so-far).
			pB = baseStack[baseLevel - 1];
			// v is shorthand for the fish candidate value
			// current vs = previous vs | vs in this base
			vsM0 = cB.vsM0 = pB.vsM0 | baseVsM0[baseIndex];
			vsM1 = cB.vsM1 = pB.vsM1 | baseVsM1[baseIndex];
			// if number of bases in 2..degree (legal size to degree)
			if ( baseLevel>1 && baseLevel<degreePlus1 ) {
				// Are there are enough eligible covers to form a Fish?
				// Done here coz depends on basesUsed. To "fit" a cover must:
				// * not be a current usedBases; and
				// * intersect (share a candidate with) the usedBases
				// * NOTE: Finned fish cannot be "Sashimi".
				ii = numCovers = 0;
				do
					// if this potential cover is NOT already used as a base
					if ( (usedBases & IDX_SHFT[allCoverUnits[ii]]) < 1
					  // and it intersects the usedBases
					  && ( (vsM0 & allCoverVsM0[ii])
						 | (vsM1 & allCoverVsM1[ii]) ) > 0L
					) {
						// this potential cover is a cover
						coverUnits[numCovers] = allCoverUnits[ii];
						coverVsM0[numCovers] = allCoverVsM0[ii];
						coverVsM1[numCovers++] = allCoverVsM1[ii];
					}
				while (++ii < numAllCovers);
				// need atleast baseLevel covers to form a Fish. A fish by definition
				// has the same number of covers as bases. If it doesnt it aint a fish.
				if ( numCovers >= baseLevel ) {
					// partial calculation of the last cover index
					// actual last depends on our depth in the stack
					lastCoverIndex = numCovers - baseLevel - 1;
//<HAMMERED comment="top1465 iterates below loop HUNDREDS OF MILLIONS of times, so no continue, label, variables, terniary, invocation, or anything slow. This code needs to base as fast as possible; preferably faster.">
					// foreach each distinct combo of covers (0 is just a stopper)
					COVER_COMBOS: for ( coverLevel=1; ; ) {
						// while level is exhausted fallback a level
						while ( (cC=coverStack[coverLevel]).index - coverLevel > lastCoverIndex ) {
							cC.index = 0;
							usedCovers &= ~IDX_SHFT[cC.prevIndex];
							cC.prevIndex = -1;
							if ( --coverLevel < 1 )
								break COVER_COMBOS; // all combos examined
						}
						// unuse the previous cover
						if ( cC.prevIndex > -1 )
							usedCovers &= ~IDX_SHFT[cC.prevIndex];
						// get next coverIndex (exists else fallen back)
						// remember prevIndex to unuse this cover later
						// remember that this cover is now used
						usedCovers |= IDX_SHFT[cC.prevIndex=coverUnits[coverIndex=cC.index++]];
						// set the previousCover (to agglomerate all covers)
						pC = coverStack[coverLevel - 1];
						// current vs = previous vs | vs in this cover (agglomerate)
						cC.vsM0 = pC.vsM0 | coverVsM0[coverIndex];
						cC.vsM1 = pC.vsM1 | coverVsM1[coverIndex];
						// if this cover shares candidates with existing covers
						// then those candidates become possible eliminations,
						// which I call sharks coz shorter than cannabilistic.
						// Great White: a non-placental bearthing live young.
						// sharks: curr = prev | (prevCovers & thisCover)
						cC.xxM0 = pC.xxM0 | (pC.vsM0 & coverVsM0[coverIndex]);
						cC.xxM1 = pC.xxM1 | (pC.vsM1 & coverVsM1[coverIndex]);
						// if we are still collecting covers
						if ( coverLevel < baseLevel ) {
							// move onto the next level
							// starting at the cell after the current cell at this level
							coverStack[++coverLevel].index = coverIndex + 1;
						} else {
							// same number of covers as bases (ie a legal fish)
							// fins = vs in bases and not covers
							fnM0 = vsM0 & ~cC.vsM0;
							fnM1 = vsM1 & ~cC.vsM1;
							// if this fish has fins
							if ( (fnM0|fnM1) > 0L
							  // and not too many of them
							  && Long.bitCount(fnM0)+Integer.bitCount(fnM1) < maxFins // ceiling
							) {
								// if fins have common buddies that maybe v,
								// ie possible eliminations
								// nb: slow but necessary (how else?)
								if ( cmnBuds(fnM0,fnM1, idxs[value].m0&~fnM0
													  , idxs[value].m1&~fnM1) ) {
//</HAMMERED>
									// deletes = (covers & ~bases) & finBuds
									dlM0 = (cC.vsM0 & ~vsM0) & cmnBudsM0;
									dlM1 = (cC.vsM1 & ~vsM1) & cmnBudsM1;
									// shark victims = sharks that see all fins
									skM0 = cC.xxM0 & cmnBudsM0;
									skM1 = cC.xxM1 & cmnBudsM1;
									// if any deletes or any shark victims
									if ( (dlM0|dlM1 | skM0|skM1) > 0L ) {
										// ******************* FINNED FISCH *******************
										// FOUND a Finned Fish!
										result = true;
										if ( accu.add(hint(value, usedBases
											, usedCovers, coverLevel
											, dlM0, dlM1 // deletes
											, skM0, skM1 // sharks
											, false // "No Deletes or Sharks"
											, fnM0, fnM1 // fins
											, cC.vsM0, cC.vsM1 // covers
											, vsM0, vsM1 // bases
										    , 0L, 0 // endofins
										)) )
											return result;
									}
								}
							}
						}
					}
				}
			}
			// if we are still collecting bases
			if ( baseLevel < degree ) {
				// onto the next level (to add another base to the stack)
				baseStack[++baseLevel].index = baseIndex + 1;
				baseStack[baseLevel].prevIndex = -1;
			}
		}
	}

}
