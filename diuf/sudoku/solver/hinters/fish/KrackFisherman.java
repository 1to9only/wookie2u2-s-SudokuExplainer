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
import static diuf.sudoku.Grid.NUM_REGIONS;
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import static diuf.sudoku.Idx.IDX_SHFT;
import static diuf.sudoku.Idx.MASKED;
import diuf.sudoku.Run;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import static diuf.sudoku.solver.hinters.table.ATableChainer.TABLES;
import diuf.sudoku.utils.Debug;
import java.util.LinkedList;
import java.util.List;
import diuf.sudoku.utils.IntQueue;
import static diuf.sudoku.solver.hinters.table.ATableChainer.getEffects;

/**
 * KrackFisherman implements the {@link Tech#KrakenJellyfish} Sudoku solving
 * technique, which is just like totes ____ing Krackhead, hence my name.
 * <p>
 * I search "unpredicated" Krakens: all possible fish, whereas
 * {@link KrakenFisherman} is "predicated". It searches fish that aren't too
 * big for there fishType, which necessitates calculating the fish-type during
 * the search, which is slow, especially when there is no such restriction,
 * hence this special "unpredicated" class. Justification: This whole code-base
 * is an unmaintainable mess, so one more class makes little difference.
 * <p>
 * A Kraken fish is what happens when Max Chainer sneaks into your complex fish
 * tank and bangs your octopus to death, with a pillock! G'Day Barnabus. Don't
 * blame me mate. I ___e on her ___s.
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
 * KRC 2023-06-30 Superfisherman drowned. Moved Tables/Eff back into KF.
 *
 * KRC 2023-10-28 Split KrackFisherman from KrakenFisherman to do unpredicated
 * version here, leaving the predicated version to KrakenFisherman.
 * Also changeCands HACK finds ~60% of elims in ~25% of time. Bargain!
 * PST: 1,690,071,809,100  4346  388,879,845  1673  1,010,204,309  KrakenJellyfish
 * </pre>
 *
 * @author Keith Corlett 2020-10-11
 */
public class KrackFisherman extends AKrakenFisherman
//, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(""+tech+": COUNTS="+java.util.Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[7];

	/**
	 * The Constructor.
	 */
	public KrackFisherman() {
		super(Tech.KrakenJellyfish);
	}

	/**
	 * Find fish hints in $grid, encaching them in $hints, returning any.
	 * <p>
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
		for ( int v : VALUESES[changedCands] ) {
			ktOns = TABLES.ons[v];
			numBases = numAllCovers = 0;
			addRegions(rows, cols, v); // ROWS
			addRegions(cols, rows, v); // COLS
			addRegions(boxs, boxs, v); // BOXS
			if ( numBases>degreeMinus1 && numAllCovers>degreeMinus1
			  && search(v) ) {
				result = true;
				break;
			}
			// stop if the user has interrupted the Generator
			hinterrupt();
		}
		return result;
	}

	/**
	 * Search all combinations of 4 among 27 base regions, which <br>
	 * searches all combinations of 4 among 27 cover regions, which <br>
	 * is slow as ____. Get over it! I mean in top1465 some lines of code here
	 * in are executed 250 BILLION times. Billion is computerese for slow.
	 * Hence 250 slows ain't even a Falcon 500. Millenium Man already knows
	 * where he can stick his Death Star.
	 * <pre>
	 * N is number of regions available in Fish of this type
	 * K is number used (degree)
	 * fac is factorial = 1+2+3...me
	 * x = (facN / (facNMinusK * facK))
	 * degree: x*x      = number of covers combinations searched
	 * Standard fish:
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
	 * 4: 17,550*17,550 = 308,002,500 is slow as ____. Get over it!
	 * </pre>
	 *
	 * @param v the Fish candidate value (ie the value to search on)
	 * @return were hint/s found
	 */
	private boolean search(final int v) {
		StackEntry pB,cB , pC,cC; // previous/current bases/covers StackEntry
		IntQueue q; // interator!
		// searchKrakens pointer to Exploded Idx of elims
		long[] elims;
		// these are all exploded Idxs
		long efM0; int efM1; // endofins: vs in two+ bases
		long vsM0; int vsM1; // vs in current bases
		long fnM0; int fnM1; // fins: extra vs in bases
		long exM0; int exM1; // examines in searchKrakens
		long vcM0; int vcM1; // victims
		int ii, jj // indexes
		, baseIndex // current bases-array index (and parallel arrays baseVs)
		, baseLevel // depth in the baseStack
		, coverIndex // the current cover region index
		, coverLevel // current coversStack level, ie coversSet size
		, floor // partial-calculation of the maximum cover index
		, numExamines // examines in searchKrakens
		, usedCovers // 27bit bitset of the used covers
		, e // eliminateValue: the value to seek eliminations of
		, i // sigh.
		;
		// covers
		int numCovers; // the number of covers in the covers array
		final int[] coverUnits = new int[NUM_REGIONS]; // Grid.regions indexes
		final long[] coverVsM0 = new long[NUM_REGIONS]; // vs in covers
		final int[] coverVsM1 = new int[NUM_REGIONS]; // vs in covers
		// the covers recursion stack
		StackEntry[] baseStack = new StackEntry[REGION_SIZE];
		for ( ii=0; ii<baseStack.length; ++ii )
			baseStack[ii] = new StackEntry();
		// the bases recursion stack
		StackEntry[] coverStack = new StackEntry[REGION_SIZE];
		for ( ii=0; ii<coverStack.length; ++ii )
			coverStack[ii] = new StackEntry();
		final Idx victims = new Idx(); // potential eliminations
		// indices to examine in searchKrakens.
		// 32 is just a guess, if it breaks double it again.
		final int[] examinesA = new int[32];
		// searchKrakenOk
		boolean skOk = false;
		// presume that no hints will be found
		boolean result = false;
		// start at level 1 (level 0 is just a stopper)
		cB = baseStack[baseLevel=1];
		int usedBases = cB.index = 0; // clear basesUsed bitset, start at first index
		cB.prevIndex = -1;
		// foreach distinct combination of base regions
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
				|| Long.bitCount(efM0) + Integer.bitCount(efM1) <= maxEndofins)
			) {
				// get eligible covers for current usedBases.
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
					// partial-calculation of the maximum cover index.
					floor = numCovers - baseLevel - 1;
					// coverLevel: the current depth in the coverStack
					// coverLevel starts at level 1 (0 is just a stopper)
					// cC is shorthand for the current CoverStackEntry
					cC = coverStack[coverLevel=1];
					cC.index = 0;
					cC.prevIndex = -1;
					usedCovers = 0;
					// foreach each distinct combination of cover regions
					COVERS_COMBOS: for (;;) {
						// while level is exhausted fallback a level
						while ( (cC=coverStack[coverLevel]).index - coverLevel > floor ) {
							if ( cC.prevIndex > -1 ) {
								usedCovers &= ~MASKED[cC.prevIndex];
								cC.prevIndex = -1;
							}
							if ( --coverLevel < 1 )
								break COVERS_COMBOS; // all combos examined
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
						// if we now have a fish: same number of covers as bases
						if ( coverLevel == baseLevel ) {
							// we have degree covers, so now we seek a Fish
							// my-fins = current endo-fins | (vs in bases and not covers)
							fnM0 = efM0 | (vsM0 & ~cC.vsM0);
							fnM1 = efM1 | (vsM1 & ~cC.vsM1);
							// complex fish needs fins
							if ( (fnM0|fnM1) > 0L
							  // but not too many of them
							  && Long.bitCount(fnM0) + Integer.bitCount(fnM1) < maxFins // ceiling
							) {
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
								// if currentCover are all sharks then skip it
								// otherwise it WILL produce invalid hints
								if ( coverVsM0[coverIndex] != exM0
								  || coverVsM1[coverIndex] != exM1
								) {
									// add the fins
									exM0 |= fnM0;
									exM1 |= fnM1;
									// get an array of indices of the cells to be examined
									for ( numExamines=0,q=new Idx.MyIntQueue(exM0,exM1); (i=q.poll())>QEMPTY; )
										examinesA[numExamines++] = i;
//<HAMMERED comment="This loop executes 700 BILLION times, taking about 6 hours. No continue, no labels, no new vars, no terniaries, no method calls. Nothing slow! This loop MUST be as fast as possible, and preferably faster.">
									// foreach eliminateValue in 1..9
									e = 1;
									do {
										// victims start as all vs except the examines
										// nb: vidxM* are grid.idxs exploded into three arrays
										vcM0 = vidxM0[e] & ~exM0;
										vcM1 = vidxM1[e] & ~exM1;
										// NON_LOOP just saves resetting skOk 700 BILLION times.
										NON_LOOP: for(;;) {
										// victims are v2 OFFs common to every $examine+$v;
										// commonly none, ergo no hint.
										jj = 0;
										do
											// nb: ktOns is TABLES.ons[value]
											if ( ( (vcM0 &= (elims=ktOns[examinesA[jj]].elims[e])[0])
												 | (vcM1 &= elims[1]) ) < 1L )
												break NON_LOOP;
										while (++jj < numExamines);
										// we get here ONLY when any victim/s survive
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
													chains.add(getEffects(grid, examine, v).getAss(victim, e));
											// KT2s fins are just fins (no hijack for examines)
											final ComplexFishHint cause = hint(
											  v, usedBases, usedCovers, coverLevel
											, 0L, 0 // deletes
											, 0L, 0 // sharks
											, true // accept "No deletes or sharks"
											, fnM0, fnM1
											, cC.vsM0, cC.vsM1 // covers
											, cB.vsM0, cB.vsM1 // bases
											, cB.xxM0, cB.xxM1 // endofins
											); // reads fins; reds are empty
											final Pots reds = new Pots();
											for ( int victim : victims )
												reds.put(victim, VSHFT[e]);
											final AHint hint = new KrakenFishHint(
												  grid, this, "Kraken Type 2 "
												, reds, cause, chains, v);
											// debug message for hackers only, in the GUI
											if ( Run.ASSERTS_ENABLED && Run.isGui() )
												hint.setDebugMessage("currentCover="+Grid.REGION_LABELS[coverUnits[coverIndex]]+NL
												+"fins="+Idx.toString(fnM0,fnM1)+NL
												+"examines="+Idx.toString(exM0,exM1)+NL
												+"victims="+Idx.toString(vcM0,vcM1)+NL
												+"reds="+reds.toString()+NL
												);
											result = true;
											if ( accu.add(hint) )
												return result;
										}
									} while (++e < VALUE_CEILING);
								}
							}
						// else we are still collecting covers
						} else {
							assert coverLevel < baseLevel;
							// move onto the next level
							++coverLevel;
							// starting with the cover following the one at current level
							coverStack[coverLevel].index = coverIndex + 1;
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
