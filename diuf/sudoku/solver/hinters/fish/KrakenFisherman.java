/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * Based on HoDoKus FishSolver, by Bernhard Hobiger. KrakenFisherman has been
 * modified extensively from hobiwans original; especially funky Tables
 * caching all possible chains in a Grid. Kudos to hobiwan. Mistakes are mine.
 *
 * Heres hobiwans standard licence statement:
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

import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.Ass;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Grid.CELL_IDXS;
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Idx.MASKED;
import diuf.sudoku.Run;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Debug;
import java.util.LinkedList;
import java.util.List;
import static diuf.sudoku.solver.hinters.fish.FishType.*;
import static diuf.sudoku.solver.hinters.table.ATableChainer.TABLES;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.solver.hinters.table.ATableChainer.getEffects;

/**
 * KrakenFisherman implements the {@link Tech#KrakenSwampfish},
 * {@link Tech#KrakenSwordfish}, and {@link Tech#KrakenJellyfish}.
 * <p>
 * A Kraken fish is what happens when a chainer sneaks into your complex fish
 * tank and assbangs all of your octopus to death, with a pillock!
 * <p>
 * This is my second go at KrakenFisherman. It runs in about half the time of
 * the first version, to find the same hints, using Tables. Its still a
 * slow useless, insane piece of s__t, and you definitely should NOT use it!
 * <p>
 * Tables contains partial forcing chains, ie the Tables contain
 * all possible ONs and OFFs, wired together into chains. KrakenFisherman
 * searches these chains, instead of each findHints call building its own
 * chains from scratch, so its faster overall.
 * <p>
 * Please note that this code is evidently "not quite right", the details of
 * which have thus-far alluded me. Krakens are suppressed in Generate because
 * it "goes mental", so generate excludes Krakens!
 * <p>
 * KrakenSwampfish are still A BIT SLOW.
 * KrakenSwordfish are still SLOW.
 * KrakenJellyfish is still WAY TOO SLOW.
 * So KrakenFisherman is still a bit slow despite my best efforts to speed it
 * up using every trick in the book, and quite a few that arent, probably for
 * good reason, so please just dont use it!
 * <p>
 * It could possibly be done a bit faster, but whats really required is a new
 * approach. I lack the motivation (and brains), because mixing chaining into
 * Fish, and pretending its simpler than chaining is just wrong, IMHO.
 * <p>
 * I suspect that Kraken came from manual Sudoku solving, when somebody noticed
 * that, having looked for fish, it wasnt much extra work to also then seek
 * chains, which then have a higher hit-rate than "random" chains, which is
 * overall less work; but on a computer, unless you do the Kraken chaining IN
 * the Fish search, Krakens take longer, which is a now-obvious down-side of my
 * decision to separate-out Krakens, but I still refuse to put Krakens back
 * into ComplexFisherman. An opportunity exists to recombine Krakens back with
 * ComplexFisherman. I fear that doing so is beyond me. I struggled to get it
 * into its current state, which is "pretty fast" compared to the original.
 * Ive had a gutful of Krakens when I dont, reasonably, believe in them.
 * <p>
 * This code is a mess. Its totally fastardised. I tried everything for speed,
 * and the result is a poorly conceived mess. You can do better. I give up.
 *
 * No auto-eat {@link Debug} {@link Ass#toStringChain }
 *
 * <pre>
 * KRC 2021-11-01 10:54 a cache-miss creates new instances so that a cache-hit
 * can just return those instances, rather than copying cache to constants, coz
 * it spent about half its time just copying cached data!
 * 529,131,437,000 1467 360,689,459 35 15,118,041,057 KrakenJellyfish
 * 323,188,864,200 1467 220,305,974 35  9,233,967,548 faster caching technique
 * 309,076,138,200 1467 210,685,847 35  8,830,746,805 KT2_CACHE[][] was a Map
 *
 * KRC 2023-06-02 13:00 discovered that maxFins and maxEndofins were tightass,
 * so modified to read Superfisch CFG, and implemented maxKrakenSizes:
 * 7/5 = 24:08 plays Superfischermans 04:21 with these configs!
 * 6/4 = 09:07 with maxKrakenType=1 maxKrakenSizes={4,3,-1}
 * 5/3 = 09:25 (Hot box)
 * 4/3 = 09:20 (Hotter box)
 * 3/3 = 07:46 (Even Hotter box) tightass, as per original
 *
 * KRC 2023-06-30 Superfisherman drowned to moved Tables and Eff back
 * into KrakenFisherman.
 * </pre>
 *
 * @author Keith Corlett 2020-10-11
 */
public class KrakenFisherman extends AKrakenFisherman
//		, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[7];

	/**
	 * Kraken Type 1 hints are as <b>RARE</b> as rocking horse s__t. There are
	 * just 2 in top1465, and KT1's are REALLY ____ING <b>SLOW</b>. This batch
	 * took 10 hours with KT1s. It takes about 90 minutes without KT1s. Hence,
	 * for efficiency, I recommend disabling KT1. However, if one obstinately
	 * adheres to the oxymoronic idea that fish with static chains are simpler
	 * than static chains, then one requires KT1s for completeness, to produce
	 * the simplest possible hint, even though most KT1's are also KT2's, hence
	 * KT1 has a switch, mostly for morons. Patient morons. Most morons are not
	 * patient. Impatience is a primary root cause of unredeemable moronity,
	 * but anybody who is KT1 level patient is an impractical extremist moron.
	 * Did I mention that I'm impatient? But am I a Moron? You'll work it out.
	 * <pre>
	 * As a techie, I observe that:
	 * DynamicChain         1,546,793 nanoseconds (1.5 milliseconds) per elim
	 * is 0.011% of (ergo a MUCH MUCH MUCH MUCH shorter period ** than):
	 * KrakenJellyfish 14,042,009,159 nanoseconds (14 seconds) per elim
	 * Hence its <b>MUCH FASTER</b> to not use Krakens, especially with KT1!
	 * One also observes that smart bastards don't write pointless software.
	 * (** given that MUCH is an order of magnitude, ie 10).
	 * </pre>
	 */
	public static final boolean IS_KRAKEN_TYPE_1_ENABLED = false;

	// self-documenting search.byRows param values
	private static final boolean BY_ROWS = true;
	private static final boolean BY_COLS = false;

	// set in constructor
	private final int[] maxKrakenSizes; // // max size of fish by {Basic,Franken,Mutant} {4,3,2}
	private final int maxKrakenType; // max kraken type {BASIC, FRANKEN, MUTANT} to search
	private final boolean seekFranken; // seek Franken fish
	private final boolean seekMutant; // seek Mutant fish
	private final boolean seekFrutant; // seekMutant || seekFranken

	/**
	 * The Constructor.
	 *
	 * @param tech Tech.KrakenSwampfish, Tech.KrakenSwordfish, or
	 *  Tech.KrakenJellyfish but <b>NOTE</b> KrackFisherman is faster for
	 *  KrakenJellyfish!
	 */
	public KrakenFisherman(final Tech tech) {
		this(tech, true);
	}

	/**
	 * The actual constructor is protected for used in the test-cases, which
	 * can't abide the hint cache. LogicalSolverBuilder.want insists that each
	 * hinter has ONE public constructor taking no-args or Tech.
	 *
	 * @param tech to implement
	 * @param useCache true normally, false in test-cases
	 */
	protected KrakenFisherman(final Tech tech, final boolean useCache) {
		super(tech, useCache);
		// Kraken only
		assert tech.name().startsWith("Kraken");
		// 2=Swampfish, 3=Swordfish, 4=Jellyfish
		assert degree>1 && degree<5;
		// maximum fishType in Krakens: -1=NONE, 0=BASIC, 1=FRANKEN, 2=MUTANT
		this.maxKrakenType = CFG.maxKrakenType();
		switch ( tech ) {                         // FINNED, FRANKEN, MUTANT
			case KrakenSwampfish: maxKrakenSizes = new int[]{4, 3, 2}; break; // fast enough
			case KrakenSwordfish: maxKrakenSizes = new int[]{4, 4, 3}; break; // 6 minutes
			case KrakenJellyfish: maxKrakenSizes = new int[]{4, 4, 4}; break; // 90 minutes
			default: throw new IllegalArgumentException("Bad tech: "+tech);
		}
		// the MAXIMUM maxKrakenSize is my degree, but maxKrakenSizes
		// may contain sizes less than my degree
		for ( int i=0; i<=maxKrakenType; ++i )
			if ( maxKrakenSizes[i] > degree )
				maxKrakenSizes[i] = degree;
		// which fishType do we seek?
		seekFranken = maxKrakenType==FRANKEN_FTYPE;
		seekMutant = maxKrakenType==MUTANT_FTYPE;
		seekFrutant = seekFranken || seekMutant;
	}

	/**
	 * foreach candidate 1..9 {@link #search(boolean)} for Fish.
	 * Search rows first, then cols.
	 * <p>
	 * If accu.isSingle then exit-early when the first hint is found.
	 *
	 * @return were any hint/s found
	 */
	@Override
	protected boolean findKrakenFishHints() {
		boolean result = false;
		try {
			// find new hints
			for ( int v : VALUESES[changedCands] ) {
				ktOns = TABLES.ons[v];
				if ( oneHintOnly ) { // search rows, else cols
					if ( (select(BY_ROWS, v) && search(v))
					  || (select(BY_COLS, v) && search(v)) ) {
						result = true;
						break;
					}
				} else { // always search both rows and cols
					if ( (select(BY_ROWS, v) && search(v))
					   | (select(BY_COLS, v) && search(v)) ) {
						result = true;
						break;
					}
				}
				// stop if the user has interrupted the Generator
				hinterrupt();
			}
		} finally {
			ktOns = null;
		}
		return result;
	}

	/**
	 * select bases and allCovers for Fish of my type and {@link #degree}.
	 * Actual covers selected later coz eligibility depends on usedBases.
	 *
	 * @param rowsAsBases true (BY_ROWS) means rows as bases; <br>
	 *  false (BY_COLS) means cols as bases
	 * @param v the Fish candidate value (ie the value to search for)
	 * @return are there atleast degree bases and allCovers: fishile material
	 */
	private boolean select(final boolean rowsAsBases, final int v) {
		numBases = numAllCovers = 0;
		// ROWS
		if ( rowsAsBases || seekMutant )
			addRegions(rows, cols, v);
		// COLS
		if ( !rowsAsBases || seekMutant )
			addRegions(cols, rows, v);
		// BOXS (KrakenFisherman seeks Franken fish,
		//       with boxs as bases XOR coverss)
		addRegions(boxs, boxs, v);
		return numBases>degreeMinus1 && numAllCovers>degreeMinus1;
	}

	/**
	 * search each distinct combination of {@link #degree} bases, which <br>
	 * searches each distinct combination of {@link #degree} covers, which <br>
	 * searches each examinable for any common forcing chains, which <br>
	 * takes ____ing ages. Get over it.
	 *
	 * @param the Fish candidate value (the value to search for)
	 * @return were hint/s found
	 */
	private boolean search(final int v) {
		// previous/current bases/covers StackEntry
		StackEntry pB,cB , pC,cC;
		IntQueue q; // interator!
		// Idx pointer to current victim (one cell).
		Idx vctm;
		// searchKrakens pointer to Exploded Idx of elims
		long[] elims;
		// these are all exploded Idxs
		long efM0; int efM1; // endofins: vs in two+ bases
		long vsM0; int vsM1; // vs in current bases
		long fnM0; int fnM1; // fins: extra vs in bases
		long exM0; int exM1; // examines in searchKrakens
		long vcM0; int vcM1; // victims
		long zzM0; int zzM1; // current victims
		int numCovers // the number of covers in the covers array
		, ii, jj // indexes
		, fishType // 0=BASIC, 1=FRANKEN, 2=MUTANT
		, baseIndex // current bases-array index (and parallel arrays baseVs)
		, baseLevel // depth in the baseStack
		, coverIndex // the current cover region index
		, coverLevel // current coversStack level, ie coversSet size
		, floor // partial-calculation of the maximum cover index
		, e // value
		, numExamines // examines in searchKrakens
		, numVictims // the victims
		;
		boolean isBoxBases; // Is there a box in usedBases
		boolean areRowsAndColsBothBases; // both rows and cols in usedBases
		final int[] coverUnits = new int[NUM_REGIONS]; // covers grid.regions indexes.
		final int[] examinesA = new int[32]; // indices to examine in searchKrakens
		final int[] victimsA = new int[32]; // indices of cells to (possibly) eliminate from
		// covers vs
		final long[] coverVsM0 = new long[NUM_REGIONS];
		final int[] coverVsM1 = new int[NUM_REGIONS];
		// the recursion stack for the cover region search.
		final StackEntry[] baseStack = new StackEntry[degreePlus1];
		for ( ii=0; ii<degreePlus1; ++ii )
			baseStack[ii] = new StackEntry();
		final StackEntry[] coverStack = new StackEntry[degreePlus1];
		for ( ii=0; ii<degreePlus1; ++ii )
			coverStack[ii] = new StackEntry();
		boolean skOk = false; // keep looking for a hint. Trick is to fail fast
		final Idx victims = new Idx(); // potential eliminations
		final Idx fins = new Idx(); // both exo and endofins: set pre createHint
		final Idx sharks = new Idx(); // cannibals: vs in multiple covers
		final Idx kSharks = new Idx(); // kraken sharks
		int usedBases; // 27bit bitset of the used bases
		int usedCovers; // 27bit bitset of the used covers
		// presume that no hints will be found
		boolean result = false;
		// start at level 1 (level 0 is just a stopper)
		cB = baseStack[baseLevel=1];
		usedBases = cB.index = 0; // clear basesUsed bitset, start at first index
		cB.prevIndex = -1;
		// foreach combo of base regions
		BASE_COMBOS: for (;;) {
			// fallback a baseLevel if no unit is available at this baseLevel
			while ( (cB=baseStack[baseLevel]).index >= numBases ) {
				// fallback one level at a time to maintain basesUsed
				if ( cB.prevIndex > -1 ) {
					usedBases &= ~MASKED[cB.prevIndex];
					cB.prevIndex = -1;
				}
				if ( --baseLevel < 1 ) // ie <= 0 (level 0 is just a stopper!)
					return result; // weve tried all combos!
			}
			// unuse the-previous-base-at-this-level
			if ( cB.prevIndex > -1 )
				usedBases &= ~MASKED[cB.prevIndex];
			// get currentBase.index (exists or would have fallenback);
			// post-incrementing currentBase.index, for next time;
			// remember my previous index, to unuse this base later;
			// and use the base with index baseUnits[currentBase]
			usedBases |= IDX_SHFT[cB.prevIndex=baseUnits[baseIndex=cB.index++]];
			// get BaseStackEntry at the previous level, contains the existing
			// candidates and endoFins (the union of ALL bases so-far).
			// NOTE that cB is already set in the above while loop!
			pB = baseStack[baseLevel - 1];
			// current vs = previous vs | vs in this base
			// NB: v is the Fish candidate value
			// NB: vsM* exist to not deref cB.vsM* repeatedly in searchCovers
			vsM0 = cB.vsM0 = pB.vsM0 | baseVsM0[baseIndex];
			vsM1 = cB.vsM1 = pB.vsM1 | baseVsM1[baseIndex];
			// my endos: vs in existing bases AND in this base
			// NB: efM* exist to not deref cB.efM* repeatedly in searchCovers
			efM0 = cB.xxM0 = pB.xxM0 | pB.vsM0 & baseVsM0[baseIndex];
			efM1 = cB.xxM1 = pB.xxM1 | pB.vsM1 & baseVsM1[baseIndex];
			// if this fish is now of legal size, basewise
			if ( baseLevel>1 && baseLevel<degreePlus1
			  // and there are no endofins, or not too many of them
			  && ( (efM0|efM1) < 1L
				|| Long.bitCount(efM0)+Integer.bitCount(efM1) <= maxEndofins )
			) {
				// Are there enough covers to form a fish with current bases?
				// Done here because depends on the current basesUsed.
				numCovers = ii = 0;
				do {
				// if this cover is NOT a base
				if ( (IDX_SHFT[allCoverUnits[ii]] & usedBases) < 1
				  // and this cover intersects the current bases
				  && ( (allCoverVsM0[ii] & vsM0)
					 | (allCoverVsM1[ii] & vsM1) ) > 0L
				) {
					coverUnits[numCovers] = allCoverUnits[ii];
					coverVsM0[numCovers] = allCoverVsM0[ii];
					coverVsM1[numCovers++] = allCoverVsM1[ii];
				}
				} while (++ii < numAllCovers);
				// need atleast baseLevel covers to form a Fish.
				if ( numCovers >= baseLevel ) {
					// search each distinct combo of covers to find fish.
					// precalculate these ONCE, not 18K times in the loop.
					isBoxBases = (usedBases & BOX_BITS) > 0;
					// are rows and cols both used as bases
					areRowsAndColsBothBases = (usedBases&ROW_BITS) > 0
										   && (usedBases&COL_BITS) > 0;
					// partial-calculation of the maximum cover index.
					floor = numCovers - baseLevel - 1;
					// coverLevel: the current depth in the coverStack
					// coverLevel starts at level 1 (0 is just a stopper)
					// cC is shorthand for the current CoverStackEntry
					cC = coverStack[coverLevel=1];
					cC.index = usedCovers = 0;
					cC.prevIndex = -1;
					// foreach each possible combination of covers
					COVER_COMBOS: for (;;) {
						// fallback level/s if theres no more covers in allCovers
						while ( (cC=coverStack[coverLevel]).index - coverLevel > floor ) {
							if ( cC.prevIndex > -1 ) {
								usedCovers &= ~MASKED[cC.prevIndex];
								cC.prevIndex = -1;
							}
							if ( --coverLevel < 1 )
								break COVER_COMBOS; // all combos examined
						}
						// unuse the previous cover
						if ( cC.prevIndex > -1 )
							usedCovers &= ~MASKED[cC.prevIndex];
						// get the next coverIndex (exists or would have fallenback)
						// post-incrementing the currentCover.index for next time.
						// remember previousIndex to unuse this cover when done with it.
						// add this cover to coversUsed (a bitset of region indexes).
						usedCovers |= IDX_SHFT[cC.prevIndex=coverUnits[coverIndex=cC.index++]];
						// set the previousCover (aggregate of all covers thereto)
						pC = coverStack[coverLevel-1];
						// current vs = previous vs | this covers vs
						cC.vsM0 = pC.vsM0 | coverVsM0[coverIndex];
						cC.vsM1 = pC.vsM1 | coverVsM1[coverIndex];
						// sharks: if this cover shares vs with existing covers they become
						// possible eliminations, which I call sharks coz its shorter than
						// cannabalistic, and has less balls (hence easier on the cannibs).
						// current sharks = previous sharks | this covers sharks
						cC.xxM0 = pC.xxM0 | (pC.vsM0 & coverVsM0[coverIndex]);
						cC.xxM1 = pC.xxM1 | (pC.vsM1 & coverVsM1[coverIndex]);
						// if we are still collecting covers
						if ( coverLevel < baseLevel ) {
							// move onto the next level, starting with the cover following
							// the current one at current level
							coverStack[++coverLevel].index = coverIndex + 1;
						} else { // its a fish: same number of covers as bases
							assert coverLevel == baseLevel;
							// my-fins = current endo-fins | (vs in bases and not covers)
							fnM0 = efM0 | (vsM0 & ~cC.vsM0);
							fnM1 = efM1 | (vsM1 & ~cC.vsM1);
							// complex fish needs fins
							if ( (fnM0|fnM1) > 0L
							  // but not too many of them
							  && Long.bitCount(fnM0)+Integer.bitCount(fnM1) < maxFins ) { // ceiling
								// to determine if this fish gets a ticket to ride,
								// calculate the fishType.			//   frequency
								// MUTANT:
								// if both row and col in bases		// 687,336,857
								// or both row and col in covers	// 613,297,968
								// or box in both bases and covers	// 612,506,978
								if ( seekMutant
								  && ( areRowsAndColsBothBases
									|| ((usedCovers&ROW_BITS)>0 && (usedCovers&COL_BITS)>0)
									|| (isBoxBases && (usedCovers&BOX_BITS)>0) ) )
									fishType = MUTANT_FTYPE;
								// FRANKEN:
								// is much simpler sans mutant -> lower overall cost
								// if box in bases or covers		// 740,327,161
								else if ( seekFrutant
								  && ((usedBases|usedCovers)&BOX_BITS) > 0 )
									fishType = FRANKEN_FTYPE;
								else
									fishType = FINNED_FTYPE;
								// if fishType gets under the kraken bar
								if ( fishType <= maxKrakenType
								  // and this fish is not too big for fish of this type
								  && coverLevel <= maxKrakenSizes[fishType]
								) {
									// --------------------------------------------------------------------
									// Kraken Type 1: if EVERY $examine+$candidate static XY Forcing Chains
									// to this $victim-$candidate, then its a KF1. Each victim is examined
									// individually, so each KT1 hint has one-only elimination. Note that
									// ~99.9% KT1s are KT2s: the difference is KT2s shark handling.
									// KT1 Logic: one of the examines MUST be candidate, and EVERY examine
									// causes $victim-$candidate, hence $victim-$candidate is proven.
									// --------------------------------------------------------------------
									// NOTE: Most KT1s are also KT2s, hence IS_KRAKEN_TYPE_1_ENABLED=false
									//                                   <<=======================
									if ( IS_KRAKEN_TYPE_1_ENABLED ) { // <<==== THIS IS FALSE ====
									//                                   <<=======================
										// examines: one of {currentCover or fins} MUST be $candidate.
										// FYI currentCover because it changes every call, trying all options.
										exM0 = coverVsM0[coverIndex] | fnM0;
										exM1 = coverVsM1[coverIndex] | fnM1;
										// victims: standard fish elims (covers and not bases) except examines
										vcM0 = (cC.vsM0 & ~vsM0) & ~exM0;
										vcM1 = (cC.vsM1 & ~vsM1) & ~exM1;
										if ( (exM0|exM1 | vcM0|vcM1) > 0L ) {
											for ( numExamines=0,q=new Idx.MyIntQueue(exM0, exM1); (ii=q.poll()) > QEMPTY; )
												examinesA[numExamines++] = ii;
											for ( numVictims=0,q=new Idx.MyIntQueue(vcM0, vcM1); (ii=q.poll()) > QEMPTY; )
												victimsA[numVictims++] = ii;
//<HAMMERED comment="No continue, no labels, no create var, no terniaries, no method calls.">
											ii = 0;
											do {
												// vtM* is a mutable exploded Idx of just da current victim
												zzM0 = (vctm=CELL_IDXS[victimsA[ii]]).m0;
												zzM1 = vctm.m1;
												jj = 0;
												NON_LOOP: for(;;) {
												do {
													// nb: ktOns = TABLES.ons[candidate]
													if ( ( (zzM0 &= (elims=ktOns[examinesA[jj]].elims[v])[0])
														 | (zzM1 &= elims[1]) ) < 1 )
														break NON_LOOP;
												} while (++jj < numExamines);
												skOk = true;
												break;
												}
												if ( skOk ) {
													skOk = false; // reset for next time
//</HAMMERED>
													// FOUND Kraken Type 1. Create a hint and add it to accu.
													// the problem with KT1 is it has ONE victim per hint.
													final int victim = victimsA[ii]; // an indice
													sharks.clear();
													if ( kSharks.setAny(cC.xxM0,cC.xxM1)
													  && kSharks.has(victim) ) {
														sharks.add(victim);
													}
													// hijack fins Idx as examines (examines are fins++)
													fins.set(exM0,exM1);
													// the "cause" is the base hint that holds the fish.
													// NB: createBH reads fins. reds are empty in cause hint.
													final ComplexFishHint cause = hint(
													  v, usedBases, usedCovers, coverLevel
													, 0L, 0 // deletes
													, 0L, 0 // sharks
													, true // accept "No deletes or sharks"
													, fnM0, fnM1
													, cC.vsM0, cC.vsM1 // covers
													, vsM0, vsM1 // bases
													, efM0, efM1 // endofins
													);
													assert cause != null;
													// build a List of the chains
													final List<Ass> chains = new LinkedList<>();
													for ( int examine : fins ) { // iterator fast enough
														// add the $victim-$candidate with parent links to
														// (ie is a consequence of) $examine+$candidate
														chains.add(getEffects(grid, examine, v).getAss(victim, v));
													}
													// the kraken hint "wraps" the cause hint
													victims.setIndice(victim);
													final Pots reds = new Pots(victims, grid.maybes, v);
													final AHint hint = new KrakenFishHint(grid, this
														,"Kraken Type 1 ", reds, cause, chains, v);
													// debug message for hackers only, in the GUI
													if ( Run.ASSERTS_ENABLED && Run.isGui() ) {
														hint.setDebugMessage(
														 "currentCover="+Grid.REGION_LABELS[coverUnits[coverIndex]]+NL
														+"fins="+fins.toString()+NL
														+"examines="+Idx.toString(exM0,exM1)+NL
														+"victims="+victims.toString()+NL
														+"reds="+reds.toString()+NL
														);
													}
													if ( result |= hints.add(hint)
													  && oneHintOnly )
														return result; // exit-early
												}
											} while (++ii < numVictims);
										}
									}
									// --------------------------------------------------------------------
									// Kraken Type 2: if EVERY $examine+$candidate static XY Chains to
									// common $victim/s-$v, then its a KT2 eliminating $victim/s-$v.
									// The logic is that one of the examine cells MUST be candidate;
									// hence any elimination/s caused by EVERY examine is proven.
									// Every possible v is checked, one by one, so KT2 is slower,
									// but finds more hints.
									// --------------------------------------------------------------------
									// examine: vs in this cover & current bases, except sharks.
									exM0 = coverVsM0[coverIndex] & cB.vsM0 & ~cC.xxM0;
									exM1 = coverVsM1[coverIndex] & cB.vsM1 & ~cC.xxM1;
									// if currentCover are all sharks then skip KT2 to avert invalid hints
									if ( coverVsM0[coverIndex] != exM0
									  || coverVsM1[coverIndex] != exM1 ) {
										// add the fins
										exM0 |= fnM0;
										exM1 |= fnM1;
										// get an array of indices of the cells to be examined
										for ( numExamines=0,q=new Idx.MyIntQueue(exM0, exM1); (ii=q.poll())>QEMPTY; )
											examinesA[numExamines++] = ii;
										// check each candidate
//<HAMMERED comment="This loop executes 700 BILLION times in the FULL_MONTY, taking about 6 hours. No continue, no labels, no new vars, no terniaries, no method calls, and nothing slow. This loop MUST be as fast as possible, and preferably faster.">
										e = 1; // we seek eliminations of e
										do {
											// victims start as all vs except the examines
											// nb: vidxM* are grid.idxs exploded into three arrays
											vcM0 = vidxM0[e] & ~exM0;
											vcM1 = vidxM1[e] & ~exM1;
											// reduce victims to OFFs common to every $exam+$cand;
											// most commonly to none, meaning theres no hint here.
											jj = 0;
											NON_LOOP: for(;;) {
											do
												// nb: ktOns is TABLES.ons[value]
												if ( ( (vcM0 &= (elims=ktOns[examinesA[jj]].elims[e])[0])
													 | (vcM1 &= elims[1]) ) < 1L )
													break NON_LOOP;
											while (++jj < numExamines);
											skOk = true;
											break;
											}
											if ( skOk ) {
//</HAMMERED>
												skOk = false; // reset for next time
												// FOUND Kraken Type 2
												// now get the chains and put them in the chainsList.
												// The Ass we add is the consequence-end of each chain
												final List<Ass> chains = new LinkedList<>();
												// iterator is fast enough when hinting
												for ( int victim : victims.set(vcM0,vcM1) )
													for ( int examine : examinesA )
														// the $victim-$v effect of $examine+$candidate
														chains.add(getEffects(grid, examine, e).getAss(victim, e));
												// there are no sharks in KT2
												sharks.clear();
												final ComplexFishHint cause = hint(
												  v, usedBases, usedCovers, coverLevel
												, 0L, 0 // deletes
												, 0L, 0 // sharks
												, true // accept "No deletes or sharks"
												, fnM0, fnM1
												, cC.vsM0, cC.vsM1 // covers
												, vsM0, vsM1 // bases
												, efM0, efM1 // endofins
												);
												final Pots reds = new Pots();
												for ( int victim : victims )
													reds.upsert(victim, VSHFT[e], DUMMY);
												final AHint hint = new KrakenFishHint(grid, this
													, "Kraken Type 2 ", reds, cause, chains, v);
												// debug message for hackers only, in the GUI
												if ( Run.ASSERTS_ENABLED && Run.isGui() )
													hint.setDebugMessage("currentCover="+Grid.REGION_LABELS[coverUnits[coverIndex]]+NL
													// KT2s fins are just fins (no hijack for examines)
													+"fins="+Idx.toString(fnM0,fnM1)+NL
													+"examines="+Idx.toString(exM0,exM1)+NL
													+"victims="+Idx.toString(vcM0,vcM1)+NL
													+"reds="+reds.toString()+NL
													);
												if ( result |= hints.add(hint)
												  && oneHintOnly )
													return result; // exit-early
											}
										} while (++e < VALUE_CEILING);
									}
								}
							}
						}
					}
				}
			}
			// if we are still gathering bases
			if ( baseLevel < degree ) {
				// move onto the next level
				baseStack[++baseLevel].index = baseIndex + 1;
				baseStack[baseLevel].prevIndex = -1;
			}
		}
	}

}
