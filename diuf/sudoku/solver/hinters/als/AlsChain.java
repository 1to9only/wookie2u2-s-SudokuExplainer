/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * This started as a copy of ALL the HoDoKu code implementing ALS-XY-Chain.
 * Everything from AlsSolver to SudokuStepFinder, all fields, etc are all in
 * this class. Kudos to hobiwan. Mistakes are KRCs. The code is rewritten but
 * the fundamental algorithm is still hobiwans.
 *
 * Hobiwans licence statement is reproduced below.
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
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.Tech;
import static diuf.sudoku.Tech.ALS_CHAIN_TECHS;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.Validator;
import static diuf.sudoku.solver.hinters.Validator.ALS_VALIDATES;
import static diuf.sudoku.solver.hinters.Validator.isValid;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.utils.Html.colorIn;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * AlsChain implements the Almost Locked Set XY-Chain Sudoku solving technique.
 * <p>
 * <pre>
 * I'm not sure I know what the XY in XY-Chain means, but I think it just means
 * "two values", because two ALSs that contain a value which all see each-other
 * can be chained together such that either:
 * (a) this ALS contains the x-value, so that ALS must contain the y-value, coz
 *     only ONE value may be eliminated from all cells in the ALS before we run
 *     out of values to fill the cells, sending the Sudoku invalid; OR
 * (b) this ALS contains the y-value, so that ALS must contain the x-value.
 * Therefore we can chain ALSs together on common RC-Values (in the Rcc) and we
 * know that a value (we know not which) is "pushed" along the chain. Thus when
 * we form a loop we know that ONE value (we know not which) is pushed along it
 * x -> y -> x -> y IN EITHER DIRECTION.
 *
 * So all we need do is form a loop that links-back to the startAls on what are
 * called z-values: values common to both the first ALS and the last ALS in the
 * chain, excluding the "used" RC-values; there are two of them: the value used
 * to link the second ALS back-to the first ALS, and the value used to link the
 * last ALS back-to the one before it (because they're already used in links).
 * Thus each z-value may be eliminated from any Cell that maybe z (obviously)
 * which sees all occurrences of z in both the first and the last ALS. And that
 * my friends is AlsChaining in a nutshell. Sounds simple enough. sigh.
 *
 * Note that no AlsChain (unlike AlsXz) is double-linked.
 * </pre>
 * <pre>
 * KRC 2021-10-24 08:56 WTF: Looks like FASTARSISED is SLOWER. sigh.
 *
 * FASTARDISED = false:
 *   9,016,839,860    8177      1,102,707    2102      4,289,647 ALS_XZ
 *   6,577,266,674    6847        960,605    2824      2,329,060 ALS_Wing
 *  18,379,324,267    4616      3,981,656    1622     11,331,272 ALS_Chain
 *  77,624,485,963 all hinters total
 *
 * FASTARDISED = true:
 *   9,067,563,182    8180      1,108,504    2098      4,322,003 ALS_XZ
 *   6,664,236,989    6854        972,313    2837      2,349,043 ALS_Wing
 *  19,303,552,970    4616      4,181,878    1614     11,960,069 ALS_Chain
 *  79,108,654,187 all hinters total is 1.484 seconds slower!
 *
 * FASTARDISED is slower so I should blow it away, but I'm reluctant to do so
 * until I understand why FASTARDISED is slower, coz it goes against everything
 * I think I know about efficient programming. Apparently I know nothing, which
 * can't be true, coz I know enough to question whether or not I know nothing.
 *
 * I ran both versions through the profiler, which is utterly useless coz this
 * code is more efficient than the profiler, skewing times heavily towards the
 * FASTARDISED version because it has less methods. 80 secs vs 8 secs. sigh.
 *
 * I need to determine where the FASTARDISED version spends its time, to guess
 * where we are wasting time, so I build-in profiler code: COUNTS and NANOS.
 *
 * KRC 2021-10-25 I'm still dissatisfied with AlsChain. In the latest batch run
 * the time/elim of AlsChain is on par with UnaryChain, which will find most
 * AlsChains, so I think that now finally it's at a usable performance level.
 * If you want faster solving drop AlsChain. The chainers tend to find solvier
 * hints than AlsChain, so that if you drop AlsChain the number of calls to
 * AlsWing actually drops, because more effective hints are found more often.
 * If you want a faster AlsChain then LENGTH=4, and drop the length increase,
 * and all the TIMEOUT stuff, so number of elims drops-back to ~1000 IIRC.
 *
 * KRC 2021-10-26 I've just built DiufSudoku_V6_30.178.2021-10-26 including the
 * new ALS_Chain_4..7 techs, and AlsChain has reverted to seeking all AlsChains
 * of size 4..degree, so the TechSelectDialog selects ONE ALS_Chain at a time.
 * The CACHE is back, to speed it up, back to near where it was when I spat the
 * dummy and went back to torts, to make sense of ALS-land, which I have done.
 * This isn't perfect, but unarguably complete and correct code is vaporware.
 * No matter what you do, there are compromises. The lesser embarrassment wins.
 * I just wish I could explain why the CACHE looses a few hints. sigh.
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
public final class AlsChain extends AAlsHinter
		implements diuf.sudoku.solver.hinters.IReporter
{
	@Override
	public void report() {
//		Log.teeln("\n"+Log.me()+": HIT_RATES:");
//		diuf.sudoku.utils.MyArrays.hitRate(Log.out, HITS, CCOUNTS);
//		Log.teeln("\n"+Log.me()+": CCOUNT_SIZE_MAXS="+Arrays.toString(CCOUNT_SIZE_MAXS));
//		Log.teeln("\n"+Log.me()+": RCOUNTS="+Arrays.toString(RCOUNTS));
		if ( hasGrown ) {
			Log.teeln(Log.me()+": private static final int[] IAS_SIZES = "+Arrays.toString(IAS_SIZES).replaceFirst("\\[","{").replaceFirst("]","};"));
		}
	}
//	private static final int[] CCOUNT_SIZE_MAXS = new int[64];
//	private static final int[] RCOUNTS = new int[2]; // [893024, 6981566] = 12.79%

	// This setting effects Run.Type.Batch only.
	// true uses searchFast which uses recurseFast, which are fastardised to
	//      reduce batch times only, which is Poor Form but it is what it is.
	// false uses the original version of search and recurse, like the GUI,
	//      They're slower so, because of TIMEOUTs, find less AlsChains.
	// See class comments!
	static final boolean FASTARDISED = true; // @check true

	// true injects details into AlsChainHint's
	// package visible to read in AlsChainHint
	static final boolean DEBUG_MODE = false;

	// we've already found too many bloody hints, so we stop here.
	private static class OverflowException extends RuntimeException {
		private static final long serialVersionUID = 4539675628269L;
	}

	// TIMEOUTS contains the timeout for each degree.
	// * In the past 16 ms was the longest it took to do ALS_Chain_4.
	// * Now 20 ms is the longest it takes to do ALS_Chain_4, which isn't good,
	//   but having to prime the cache slows it down a bit, so it's acceptable.
	//   I want ALS_Chain_4 to always complete, except if it's really slow.
	// * It gets exponentially slower as length increases, so:
	//   * I give 5 twice 4
	//   * I give 6 three times 5
	//   * I give 7 four times 6
	// * The first AlsChain (presumably ALS_Chain_4) disables ALL AlsChains
	//   upon timeout, others disable themselves only upon timeout.
	private static final int[] TIMEOUTS = {
	//  0  1  2  3   4   5    6    7 // degree
		0, 0, 0, 0, 20, 40, 120, 240 // timeout (milliseconds), harsh on 7's
	};

	// is this the first AlsChain to run, in order to SetUp the CACHE
	private static Tech firstWantedAlsChainTech() {
		final EnumSet<Tech> wantedTechs = THE_SETTINGS.getWantedTechs();
		for ( int i=0; i<ALS_CHAIN_TECHS.length; ++i )
			if ( wantedTechs.contains(ALS_CHAIN_TECHS[i]) )
				return ALS_CHAIN_TECHS[i];
		return null;
	}

	// is this the last AlsChain to run, in order to CleanUp the CACHE
	private static Tech lastWantedAlsChainTech() {
		final EnumSet<Tech> wantedTechs = THE_SETTINGS.getWantedTechs();
		for ( int i=ALS_CHAIN_TECHS.length-1; i>-1; --i )
			if ( wantedTechs.contains(ALS_CHAIN_TECHS[i]) )
				return ALS_CHAIN_TECHS[i];
		return null;
	}

	// The chain* arrays are parallel arrays, each of which holds an attribute
	// of a virtual ChainEntry struct: the ALSs (et al) that are in the chain.
	// Parallel-arrays for speed: array-lookup faster than struct.deref, but
	// parallel-arrays do more array-lookups, so adding attrs eventually makes
	// a struct faster, despite deref imposts. My cross-over is 7 attrs.
	// For 4 attrs I use parallel arrays, because it's (usually) faster.
	private final Als[] chainAlss;
	private final Rcc[] chainRccs;
	// entries are back-linked, [1] -back-> [0], chainCands[0] not used
	private final int[] chainCands;
	// indices of all cells in the-chain-so-far
	private final Idx[] chainSoFar;
	// for debugging only
	private final int[] chainRccIndexes;

	// These methods help the swap between parallel-arrays and a ChainEntry[].
	// They help, but they're not the whole hog.
	private void chainClear() {
		Arrays.fill(chainAlss, null);
		Arrays.fill(chainRccs, null);
		Arrays.fill(chainCands, 0);
		chainSoFar[0] = null;
		for ( int i=1; i<degree; ++i ) {
			chainSoFar[i].clear();
		}
//		for ( int i=0; i<CCOUNT_SIZE_MAXS.length; ++i ) {
//			if ( CCOUNT_SIZES[i] > CCOUNT_SIZE_MAXS[i] ) {
//				CCOUNT_SIZE_MAXS[i] = CCOUNT_SIZES[i];
//			}
//		}
	}

	private Als[] chainAlss(int size) {
		return Arrays.copyOf(chainAlss, size);
	}

	private int[] chainValues(int size) {
		final int[] result = new int[size];
		for ( int i=0; i<size; ++i ) {
			result[i] = VFIRST[chainCands[i]];
		}
		return result;
	}

	private Rcc[] chainRccs(int size) {
		return Arrays.copyOf(chainRccs, size);
	}

	// RETAIN: for debugging only, which I need to make sense of it all
	private int[] chainRccIndexes(int size) {
		return Arrays.copyOf(chainRccIndexes, size);
	}

	/** indices of cells from which we can remove this z-value */
	private final Idx victims = new Idx();
	/** the removable (red) potentials (Cell=>Cands) */
	private final Pots reds = new Pots();
	/** the last valid chain index, ie degree - 1 */
	private final int last;
	/** is this the first wanted AlsChain instance? */
	private final boolean meFirst;
	/** is this the last wanted AlsChain instance? */
	private final boolean meLast;
	/** the TIMEOUT for this degree (in current Run.type: generate differs) */
	private final int timeout;

	// --------------------------------------------------
	// these fields are all set and cleared by findHints
	// the Grid to search
	private Grid grid;
	// Restricted Common Candidates: an Rcc is a link between two ALSs on
	// restricted values. Common just means this value occurs in both ALSs. The
	// "restricted" means that all occurrences of this value/s in either ALS
	// see each other, and the value does not occur in the physical overlap, if
	// any, of the two ALSs, so that this value/s must be in this ALS or that
	// ALS, but never both. ~5% of ALSs have two restricted values. No Rcc has
	// three restricted values, or the universe implodes. In fact, in this
	// implementation it's simply not possible. We hard-stop at two.
	private Rcc[] rccs;
	// Almost Locked Sets: 2 cells sharing 3 maybes, 3 cells 4 maybes, ...
	private Als[] alss;
	// the hints-accumulator to which I add hints
	private IAccumulator accu;
	// is it a SingleHintsAccumulator, ergo are we in the batch
	private boolean oneOnly;
	// the first ALS is chain[0].als
	private Als firstAls;
	// bitset of candidates available as z-values: firstMaybes & ~firstCand
	private int firstUsableCands;
	// --------------------------------------------------

	public AlsChain(Tech tech) {
		super(tech, true, true, true, false);
		chainAlss = new Als[degree];
		chainRccs = new Rcc[degree];
		chainCands = new int[degree];
		chainSoFar = new Idx[degree];
		chainRccIndexes = new int[degree];
		// nb: chainSoFar[0] is set to als.idx on the fly
		for ( int i=1; i<degree; ++i ) {
			chainSoFar[i] = new Idx();
		}
		// the last valid index in the chain* arrays
		last = degreeMinus1;
		// how many milliseconds does findHints run before pulling the pin
		timeout = TIMEOUTS[degree];
		if ( THE_SETTINGS.getWantedTechs().contains(tech) ) {
			// work-around the no other constructor params issue.
			// nb: this is ALWAYS true in GUI and batch.
			meFirst = tech == firstWantedAlsChainTech();
			meLast = tech == lastWantedAlsChainTech();
		} else {
			// work-around test-cases not being wanted techs.
			// performance of test-cases matters not anyway.
			// nb: only ALS_Chain_4 is currently tested.
			meFirst = meLast = true;
		}
	}

	@Override
	protected boolean findHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		this.grid = grid;
		this.rccs = rccs;
		this.alss = alss;
		this.accu = accu;
		this.oneOnly = accu.isSingle();
		AlsChain.rccsStart = rccFinder.getStarts();
		AlsChain.rccsEnd = rccFinder.getEnds();
		try {
			// all instances of AlsChain share a static rcc-indexes-cache
			if ( meFirst ) {
				rccCacheSetup(rccs, numRccs);
				for ( int i=0; i<numAlss; ++i )
					if ( ANY_RELATED_RCCS[i] = rccsEnd[i]-rccsStart[i] > 0 )
						alsCeiling = i;
//				// DEBUG
//				int cnt = 0;
//				for ( int i=0; i<alsCeiling; ++i )
//					if ( ANY_RELATED_RCCS[i] )
//						++cnt;
//				RCOUNTS[0] += cnt;
//				RCOUNTS[1] += numRccs;
			}
			if ( FASTARDISED && Run.type==Run.Type.Batch ) {
				// searchFast is faster coz it doesn't deal with the case that
				// an Rcc has two usableCands, which only happens in Shft-F5,
				// AFAIK. It crashes if it hits an Rcc with two usableCands.
				// It's been fastardised, so it's completely unmaintainable.
				//
				// To be crystal clear, searchFast crashes when it encounters
				// an Rcc with VSIZE[usableCands] > 1.
				//
				// Two usableCands only occurs on an Rcc with two RC-values (ie
				// v2!=0) and the previous link value (prevCand) is neither of
				// those two RCs. I do NOT understand why this never happens in
				// the batch, but I have confirmed it multiple times using "my"
				// hinters prior to AlsChain. It's unsafe if you're playing, so
				// probably best to switch-off FASTARDISED.
				//
				// If you make searchFast handle 2 usableCands it'll be slower,
				// but as reliable as the normal version. The probability of
				// hinting via 0.11*~5% of Rccs is tiny, with a tiny effect on
				// the results, it can't crash, that's all. But it will be
				// substantially slower coz 99.99% of Rccs will still have just
				// 1 usableCands, despite having all the plumbing for multiple.
				searchFast();
			} else {
				// the "normal" search method is fast enough, and less insane,
				// but it still uses the CACHE, which is pretty crazy.
				search();
			}
			if ( accu.size() > 1 ) { // batch NEVER
				accu.sort(null);
			}
			return accu.size() > 0;
		} catch ( OverflowException eaten ) { // too many hints
			return true;
		} finally {
			chainClear();
			this.grid = null;
			this.rccs = null;
			this.alss = null;
			this.accu = null;
			AlsChain.rccsEnd = null;
			AlsChain.rccsStart = null;
			// all instances of AlsChain share a static rcc-indexes-cache
			if ( meLast ) {
				rccCacheCleanup();
			}
		}
	}

	/**
	 * Search the grid for AlsChains. I add the first two ALSs to the chain,
	 * then call recurse to add the third-onwards.
	 *
	 * @param numAlss
	 */
	private void search() {
		Rcc rcc;
		Idx firstIdx;
		int[] kids;
		int i1,i2 // alss-index of first and second als
		  , j,J // rccs-index and rccs-index-ceiling
		  , firstMaybes; // 
		boolean any = false;
		final long begin = System.currentTimeMillis();
		// foreach ALS, which I use ONLY to look-up the-rccs-for-this-als
		for ( i1=0; i1<alsCeiling; ++i1 ) {
			firstAls = chainAlss[0] = alss[i1];
			firstMaybes = firstAls.maybes;
			firstIdx = chainSoFar[0] = firstAls.idx;
			// foreach firstRcc in rccs-related-to-the-firstAls
			if ( ANY_RELATED_RCCS[i1] ) {
				for ( j=rccsStart[i1],J=rccsEnd[i1]; j<J; ++j ) {
					i2 = (rcc=rccs[j]).related;
					if ( ANY_RELATED_RCCS[i2] ) {
						// both of the first-two ALSs come from the firstRcc, but
						// only the second ALS "uses" a candidate (first doesn't)
						chainRccIndexes[0] = chainRccIndexes[1] = j; // for debugging only
						chainRccs[0] = chainRccs[1] = rcc;
						// rcc.related is the index of the second ALS
						// chainAlss[1] is the second ALS
						chainAlss[1] = alss[i2];
						// my idx is the aggregate of all ALSs so far.
						// nb: firstIdx was set in i-loop
						chainSoFar[1].setOr(firstIdx, chainAlss[1].idx);
						// foreach cand (usually one, just ~5% have two)
						for ( int usedCand : VSHIFTED[rcc.cands] ) {
							if ( (kids=rccCache(i2, usedCand)) != EMPTY_RCCS ) {
								chainCands[1] = usedCand;
								firstUsableCands = firstMaybes & ~usedCand;
								if ( (any|=recurse(kids, usedCand, 2)) && oneOnly )
									return;
							}
						}
					}
					if ( System.currentTimeMillis() - begin > timeout ) {
						timeout(i1);
						return;
					}
				}
			}
		}
	}

	/**
	 * Recurse explores all possible combinations of ALSs at and to the right
	 * of index 'n' in the chain.
	 * <p>
	 * NOTE: recurse is unfastardised. Everything is methodised and there is
	 * no speedophilic mess, so it's maintainable but slower than recurseFast,
	 * which does approximately the same thing, but faster.
	 * <p>
	 * The only algorithmic diversion is recurse handles an Rcc with multiple
	 * usableCands, which NEVER happens in the batch (recurseFast), only when
	 * the user finds-MORE-Hints (Shft-F5) in the GUI on a puzzle that already
	 * solvesWithSingles. Handling multiple usableCands is slower. The batch
	 * need not handle it because it simply NEVER happens (with my preferred
	 * hinters selected).
	 *
	 * @param srcAlsIndex the index of my sourceAls in the alss-array
	 * @param prevCand a bitset of the value used to form the previous link
	 * @param n the number of ALSs in chainAlss when you call me (2)
	 * @return any hint/s found
	 */
	private boolean recurse(final int[] relatedRccIndexes, final int prevCand, final int n) {
		assert relatedRccIndexes != EMPTY_RCCS;
		Rcc rcc; // the current Rcc we're processing
		final int m = n - 1;
		boolean result = false;
		for ( int i : relatedRccIndexes ) {
			rcc = rccs[i];
			// the rccCache does "rcc.cands & ~prevCand != 0" for me
			assert (rcc.cands & ~prevCand) != 0;
			if ( noLoop(alss[rcc.related].idx, m) ) {
				chainRccs[n] = rcc;
				chainRccIndexes[n] = i; // for debugging only
				// handle multiple usableCands, one-at-a-time
				for ( int usedCand : VSHIFTED[rcc.cands & ~prevCand] ) {
					chainCands[n] = usedCand;
					if ( (result|=process(alss[rcc.related], usedCand, n)) && oneOnly ) {
						return result;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns does 'als' NOT make a loop out of the chain?
	 * Ergo is the bastard ok?
	 *
	 * @param alsIdx to test
	 * @param m last valid index in the chain (ie n - 1)
	 * @return does 'als' NOT make a loop out-of the existing chain alss?
	 */
	private boolean noLoop(final Idx alsIdx, final int m) {
		// if the previous chainSoFar containsAll als cells
		if ( chainSoFar[m].hasAll(alsIdx) ) {
			for ( int i=m; i>-1; --i ) {
				if ( chainAlss[i].idx.inbread(alsIdx) ) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Process this Als, only called in the multi-RC-value loop, ie rarely.
	 *
	 * @param als the "related" Als to process
	 * @param usedCand the cand that is currently used (from usableCands)
	 * @param n the number of ALS's already in chainAlss when you call me
	 * @return any hint/s found?
	 */
	private boolean process(final Als als, final int usedCand, final int n) {
		// presume no hints will be found, coz it's a good assumption: there's
		// 561,613 hinter-calls finding 105,731 hints, ergo a 20% hit-rate, so
		// four out of five times you call a hinter it finds nothing, but note
		// that some hinters are much hintier than others. ALS_Chain_7, just
		// for instance, is a hintless pig whajusneedsit some shoootin.
		boolean result = false;
		// if there are degree ALSs in the chain
		// nb: this was n > 2, but I've split 4, 5, 6, and 7 each into there
		// own tech so that users can tap-out at the maximum chain length by
		// deselecting the longer techs, so this approach PRESUMES that if you
		// use AlsChain5 you also use AlsChain4, if that's not the case then
		// you should put this back to n > 2, then AlsChain5 also finds 4s!
		// and pretty obviously 7 finds 4s, 5s, 6s, and 7s, it just takes ages!
		if ( n > 2 ) {
			final int zs = firstUsableCands & als.maybes & ~usedCand;
			if ( zs!=0 && firstAls.buds.intersects(als.buds) ) {
				for ( int z : VALUESES[zs] ) {
					if ( victims.setAndAny(firstAls.vBuds[z], als.vBuds[z]) ) {
						result |= reds.upsertAll(victims, grid, z);
					}
				}
				if ( result ) {
					result = hint(als, n);
				}
			}
		}
		// if the chain isn't full
		if ( n < last
		  // and there are any related Rccs
		  && ANY_RELATED_RCCS[als.index] ) {
			// and this Als has related Rccs with OTHER candidate values
			final int[] kids = rccCache(als.index, usedCand);
			if ( kids != EMPTY_RCCS ) {
				// then recurse to add the next (n+1) als
				chainAlss[n] = als;
				chainSoFar[n].setOr(chainSoFar[n-1], als.idx);
				result |= recurse(kids, usedCand, n+1);
			}
		}
		return result;
	}

	/**
	 * Create a new AlsChainHint, and if ALS_VALIDATES then validate it, and
	 * if it's still OK add it to the IAccumulator, returning was it added?
	 *
	 * @param n the number of ALSs in chainAlss
	 * @return was the hint added?
	 */
	private boolean hint(final Als als, final int n) {
		// adding the last als to the chain is delayed until we hint!
		chainAlss[n] = als;
		chainSoFar[n].setOr(chainSoFar[n-1], als.idx);
		final int[] rcs; if(DEBUG_MODE) rcs=chainRccIndexes(n+1); else rcs=null;
		final AlsChainHint hint = new AlsChainHint(this
			, reds.copyAndClear()
			, chainAlss(n+1)
			, chainValues(n+1)
			, chainRccs(n+1)
			, rcs  // chainRccIndexes in DEBUG_MODE, else null
			, ""); // debugMessage
		if ( ALS_VALIDATES && !isValid(grid, hint.reds) && !handle(hint) )
			return false; // without adding it!
		accu.add(hint);
		// Avoid sans-TIMEOUT OutOfMemoryError: confusing coz search/recurse
		// don't use "new", so it must be too many hints, so stop at 64 hints.
		if ( accu.size() > 63 ) {
			throw new OverflowException();
		}
		return true; // regardless of what accu.add says (handled up the stack)
	}

	/**
	 * Handle a bad hint.
	 *
	 * @param badHint to handle appropriately in the current context.
	 * @return should we add this hint to IAccumulator anyway?
	 */
	private boolean handle(final AlsChainHint badHint) {
		Validator.report(tech.name(), grid, Als.list(badHint.alss));
		switch ( Run.type ) {
		case GUI:
			if ( Run.ASSERTS_ENABLED ) {
				badHint.isInvalid = true;
				badHint.debugMessage = colorIn("<p><r>"+prevMessage+"</r>");
				return true;
			}
			return false;
		case Batch:
			return false;
		default:
			throw new UnsolvableException(Log.me()+": bad reds: "+badHint.reds);
		}
	}

	private void timeout(final int alsIndex) {
		setIsEnabled(false);
		Log.format("TIMEOUT: %s: %d milliseconds exceeded in %d/%s i=%d/%d DISABLED!\n"
				, tech, timeout, grid.hintNumber, grid.source(), alsIndex, alsCeiling);
	}

	// ============================= FASTARDISED ==============================

	/**
	 * Search the grid for AlsChains. I add the first two ALSs to the chain,
	 * then call recurse to add the third-onwards.
	 * <p>
	 * fastardised: no methods, no variables, no continue. Idx's exploded to
	 * reimplement all operations inline. I call recurseFast that only handles
	 * batch-runs where usableCands always contains 1 set bit, so it's faster.
	 *
	 * @param numAlss
	 */
	private void searchFast() {
		Rcc rcc;
		Idx firstIdx, alsIdx;
		int[] ua, kids;
		int ui, ul, usedCand, i1,i2, j, J, firstMaybes;
		final long begin = System.currentTimeMillis();
		final Idx secondIdx = chainSoFar[1];
		// presume that no hints will be found
		boolean any = false;
		// foreach ALS, which I use ONLY to look-up the-rccs-for-this-als
		for ( i1=0; i1<alsCeiling; ++i1 ) {
			firstAls = chainAlss[0] = alss[i1];
			firstMaybes = firstAls.maybes;
			firstIdx = chainSoFar[0] = firstAls.idx;
			if ( ANY_RELATED_RCCS[i1] ) {
				// foreach firstRcc in rccs-related-to-the-firstAls
				for ( j=rccsStart[i1],J=rccsEnd[i1]; j<J; ++j ) {
					// both of the first-two ALSs come from the firstRcc, but
					// only the second ALS "uses" a candidate (first doesn't)
					chainRccIndexes[0] = chainRccIndexes[1] = j; // for debugging only
					rcc = chainRccs[0] = chainRccs[1] = rccs[j];
					// rcc.related is the index of the second ALS
					// chainAlss[1] is the second ALS
					i2 = rcc.related;
					if ( ANY_RELATED_RCCS[i2] ) {
						chainAlss[1] = alss[i2];
						// chainSoFar is the aggregate of all ALSs so far
						secondIdx.a0 = firstIdx.a0 | (alsIdx=chainAlss[1].idx).a0;
						secondIdx.a1 = firstIdx.a1 | alsIdx.a1;
						secondIdx.a2 = firstIdx.a2 | alsIdx.a2;
						// foreach cand (usually one, just ~5% have v2!=0)
						for ( ua=VSHIFTED[rcc.cands],ul=ua.length-1,ui=0; ; ) {
							if ( (kids=rccCache(i2, (usedCand=ua[ui]))) != EMPTY_RCCS ) {
								chainCands[1] = usedCand;
								firstUsableCands = firstMaybes & ~usedCand;
								if ( (any|=recurseFast(kids, usedCand, 2)) && oneOnly )
									return;
							}
							if ( ++ui > ul )
								break;
						}
					}
				}
				if ( System.currentTimeMillis() - begin > timeout ) {
					timeout(i1);
					return;
				}
			}
		}
	}

	/**
	 * Recurse explores all possible combinations of ALSs at and to the right
	 * of index 'n' in the chain.
	 * <p>
	 * fastardised: I'm hammered so no methods, no variables, no continue.
	 * Idx's exploded to reimplement all operations inline. It's OK to call
	 * methods in order to hint, because it's rare. I handle only the use-case
	 * where usableCands contains just 1 set bit, ergo there are NEVER multiple
	 * usableCands in the batch or the test-cases, but the test-cases follow da
	 * normal path, as per the GUI, because I reckon the GUI (all users) needs
	 * testing more than the batch (techies only) does.
	 * <p>
	 * Verify the batch by running AlsChainBatchTest manually.
	 * Ctrl-F6 runs AlsChainTest, which tests the GUI version.
	 *
	 * @param srcAlsIndex the index of my sourceAls in the alss-array
	 * @param prevCand a bitset of the value used to form the previous link
	 * @param n the number of ALSs in chainAlss when you call me (2)
	 * @return any hint/s found
	 */
	private boolean recurseFast(final int[] relatedRccs, final int prevCand, final int n) {
		assert relatedRccs != EMPTY_RCCS;
		Rcc rcc; // the current Restricted Common Candidate
		Als als; // the alss[rcc.related] we're processing
		Idx t0, t1; // temporary Idx pointers
		int[] za, kids; // array of the z-values to examine for elimination
		int usableCands // bitset of RC-value/s that can be used in this rcc
		  , i0, i1, i2 // Idx intersection exploded
		  , zs // bitset of the z-values to examine for elimination
		  , zl,zi // za.length-1, za index
		  , ai; // als.index = rcc.related
		boolean ok; // pursue this als?
		// pre-existing cells in the chain (ie asat the previous level)
		final Idx chainBefore = chainSoFar[n-1];
		// presume that no hints will be found
		boolean result = false;
		// cache nothing! numRefs reduces so it cannot pay back, I think.
		for ( int i : relatedRccs ) {
			// the rccCache does "rcc.cands & ~prevCand != 0" for me
			assert (rccs[i].cands & ~prevCand) != 0;
			ok = true;
			if ( (chainBefore.a0 & (t0=alss[rccs[i].related].idx).a0) == t0.a0
			  && (chainBefore.a1 & t0.a1) == t0.a1
			  && (chainBefore.a2 & t0.a2) == t0.a2
			) {
				for ( int j=0; j<n; ++j ) {
					// nb: t0 has already been set in the above if
					if ( ( (i0=(t1=chainAlss[j].idx).a0 & t0.a0) == t0.a0
						 & (i1=t1.a1 & t0.a1) == t0.a1
						 & (i2=t1.a2 & t0.a2) == t0.a2 )
					  || ( i0==t1.a0 && i1==t1.a1 && i2==t1.a2 )
					) {
						ok = false;
						break;
					}
				}
			}
			if ( ok ) {
				rcc = chainRccs[n] = rccs[i];
				chainRccIndexes[n] = i; // for debugging only
				// nb: always 1 usableCands, so usableCands is THE usedCand.
				usableCands = chainCands[n] = rcc.cands & ~prevCand;
				assert VSIZE[usableCands] == 1;
				ai = rcc.related;
				als = alss[ai];
				// if there are atleast 4 ALSs in the chain
				if ( n > 2 // nb: n is a 0-based index
				  && (zs=firstUsableCands & als.maybes & ~usableCands) != 0
				  && ( (t1=firstAls.buds).a0 & (t0=als.buds).a0
				     | (t1.a1 & t0.a1)
				     | (t1.a2 & t0.a2) ) != 0
				) {
					for ( ok=false,za=VALUESES[zs],zl=za.length-1,zi=0; ; ) {
						if ( ( (i0=(t1=firstAls.vBuds[za[zi]]).a0 & (t0=als.vBuds[za[zi]]).a0)
							 | (i1=t1.a1 & t0.a1)
							 | (i2=t1.a2 & t0.a2) ) != 0
						) {
							ok |= reds.upsertAll(victims.set(i0,i1,i2), grid, za[zi]);
						}
						if ( ++zi > zl ) {
							break;
						}
					}
					if ( ok && (result|=hint(als, n)) && oneOnly ) {
						break;
					}
				}
				// if the chain is not yet full
				if ( n < last
				  && ANY_RELATED_RCCS[ai]
				  // and this ALS has kids (relatedRccs)
				  && (kids=rccCache(ai, usableCands)) != EMPTY_RCCS
				) {
					// add this ALS (n) to the chain
					chainAlss[n] = als;
					(t0=chainSoFar[n]).a0 = chainBefore.a0 | (t1=als.idx).a0;
					t0.a1 = chainBefore.a1 | t1.a1;
					t0.a2 = chainBefore.a2 | t1.a2;
					// recurse to add the next (n+1) ALS to the chain
					if ( (result|=recurseFast(kids, usableCands, n+1)) && oneOnly )
						break;
				}
			}
		}
		return result;
	}

	// The CACHE is a FASTARDISATION. It's rcc-indexes by [alsIndex][prevCand]
	// having cands other than prevCand. It's static, shared by all instances
	// of AlsChain, so each SUBSEQUENT request for an [ai][pc] is faster.
	//
	// The downside is the FIRST [ai][pc] request is slower coz each sub-array
	// is created, and creating arrays in Java is slow. But there tends to be
	// many subsequent [ai][pc] requests, so it's ~30% faster overall.
	//
	// The other downside is RAM usage. The CACHE array is pretty big: MAX_ALSS
	// is 512 (currently) and VALUE_CEILING is 9, and 512 * 9 = 4608, and each
	// element there-in is itself an array of upto 49 rcc-indexes. Most of this
	// sparse-array remains empty, even when "full", and ~40% are EMPTY_RCCS.
	//
	// The CACHE is shared by all instances of AlsChain, so the hit-rate rises.
	private static final int[][][] CACHE = new int[MAX_ALSS][VALUE_CEILING][];
	// a temporary rcc-indexes-array to populate and then copy-off when we know

	// how many there are
	private static final int[] TMP_RCCS = new int[64]; // observed 49

	// there is one only empty rcc-indexes-array
	private static final int[] EMPTY_RCCS = new int[0];

	// the .cands of each rccs (coz that's all I need)
	private static final int[] CACHE_RCC_CANDS = new int[MAX_RCCS];

	// rccsStart contains the first index of Rcc's that're related to each Als.
	// rccsEnd contains the ceiling for the rcc-index (last-valid-index+1).
	// nb: these arrays aren't mine, so they're set and forgotten in findHints,
	// else I hold the whole RccFinder in memory (it's static anyway, putz).
	private static int[] rccsStart;
	private static int[] rccsEnd;

	// does this alsIndex have any related Rcc's? Just 12.79% of top1465s Als's
	// are viable branches (the rest may be viable leafs), so seeking branches
	// from the other 87.21% is a waste of time, this is the fastest way (short
	// of weeding the alss array) I can think of to ignore them. It MIGHT be
	// faster to weed! I'll have to check. But first performance-test this.
	private static final boolean[] ANY_RELATED_RCCS = new boolean[MAX_ALSS];
	private static int alsCeiling;

	// setup the CACHE, and all of his messy friends
	private static void rccCacheSetup(final Rcc[] rccs, final int numRccs) {
//		Arrays.fill(CCOUNT_SIZES, 0);
		for ( int i=0; i<numRccs; ++i )
			CACHE_RCC_CANDS[i] = rccs[i].cands;
		// clear the CACHE, yes, really, in a Setup method, for realz.
		for ( int i=0; i<MAX_ALSS; ++i )
			Arrays.fill(CACHE[i], null);
		Arrays.fill(IAS_CNTS, 0);
	}

//	private static final int[][] HITS = new int[MAX_ALSS][VALUE_CEILING];
//	private static final int[][] CCOUNTS = new int[MAX_ALSS][VALUE_CEILING];
//	private static final int[] CCOUNT_SIZES = new int[64];

	// CACHE[*][0] is otherwise unused, so it's the ALL array.
	private static final int ALL = 0;

	// rccCache is called by recurseFast and process to fetch the indexes of
	// rccs that are related to the given alsIndex having cands other than
	// prevCand. This is supposed to be faster, but its doubtful despite high
	// hit-rates, coz creating new arrays is slow.
	// PRE: cacheEnds[ai]-cacheStarts[ai] > 0; or don't bother calling me
	// @param ai alsIndex the index of the source ALS in the alss array
	// @param pc previousCand and we need cands other than previousCand
	private static int[] rccCache(final int ai, final int pc) {
		// prevCand is ONE value only
		assert VSIZE[pc] == 1 : "prevCand="+pc;
//		++CCOUNTS[ai][VFIRST[pc]];
		// first try the cache
		int[] result = CACHE[ai][VFIRST[pc]];
		if ( result != null ) {
//			++HITS[ai][VFIRST[pc]];
			return result;
		}
		// cache miss, so count em, and copy em to TMP_RCCS
		int cnt = 0;
		// foreach rccIndex in starts[alsIndex]..ends[alsIndex]
		for ( int i=rccsStart[ai],I=rccsEnd[ai]-1; ; ) {
			if ( (CACHE_RCC_CANDS[i] & ~pc) != 0 )
				TMP_RCCS[cnt++] = i;
			if ( ++i > I )
				break;
		}
//		++CCOUNT_SIZES[cnt];
		// if that's NONE of them
		if ( cnt == 0 ) {
			return CACHE[ai][VFIRST[pc]] = EMPTY_RCCS;
		}
		// if that's ALL of them
		if ( cnt == rccsEnd[ai]-rccsStart[ai] ) {
//			++CCOUNTS[ai][0];
			if ( CACHE[ai][ALL] != null ) {
				// and the ALL array already exists
//				++HITS[ai][0];
				return CACHE[ai][VFIRST[pc]] = CACHE[ai][ALL];
			} else {
				// stash em in the ALL array as well
				try {
					result = CACHE[ai][ALL] = CACHE[ai][VFIRST[pc]] = IAS[cnt][IAS_CNTS[cnt]++];
				} catch (ArrayIndexOutOfBoundsException ex) {
					result = CACHE[ai][ALL] = CACHE[ai][VFIRST[pc]] = grow(cnt);
				}
				System.arraycopy(TMP_RCCS, 0, result, 0, cnt);
				return result;
			}
		}
		// so that must be SOME of them
		try {
			result = CACHE[ai][VFIRST[pc]] = IAS[cnt][IAS_CNTS[cnt]++];
		} catch (ArrayIndexOutOfBoundsException ex) {
			result = CACHE[ai][VFIRST[pc]] = grow(cnt);
		}
		System.arraycopy(TMP_RCCS, 0, result, 0, cnt);
		return result;
	}
	// These sizes are top1465.d5.mt maximums using ALS_Chain_4, ALS_Chain_5,
	// ALS_Chain_6, and then ALS_Chain_7, and a good Generator session. They'll
	// probably never need grow again (give it a week), but now there's fat, so
	// if you like, reduce-em all to 10 and let-em grow into your ALS_Chain_*
	// and your puzzles in a batch run, then copy-paste the IAS_SIZES line out
	// of the bottom of the log over this line, so YOUR SudokuExplainer is
	// setup specifically for how YOU use it, with no (or little) extra fat.
	// If the whole concept annoys you then replace this mess with an array of
	// 64 ArrayList<int[]>, and good luck to you, or just use new int[]'s, like
	// I did before I went mental and wrote the IAS, which probably uses so
	// much RAM it's slowing down the other hinters more than it's speeding-up
	// this one. It doesn't pay to be TOO greedy.
	private static final int[] IAS_SIZES = {0, 106, 89, 102, 80, 78, 72, 70, 79, 70, 67, 64, 115, 91, 61, 85, 80, 47, 65, 44, 24, 31, 25, 47, 25, 20, 38, 17, 38, 16, 10, 16, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 0, 10, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] IAS_CNTS = new int[IAS_SIZES.length];
	private static final int[][][] IAS = new int[IAS_SIZES.length][][];
	static {
		for ( int i=0; i<IAS_SIZES.length; ++i ) {
			IAS[i] = new int[IAS_SIZES[i]][];
			for ( int j=0; j<IAS_SIZES[i]; ++j ) {
				IAS[i][j] = new int[i];
			}
		}
	}

	// the grow method grows the IAS[cnt] array to 1.5 times it's existing size
	// and then returns IAS[cnt][IAS_CNTS[cnt]++], so it's as if the AIOOBE did
	// not happen, a new array has been created and returned.
	private static int[] grow(final int cnt) {
		int[][] tmp = IAS[cnt]; // a handle to the existing array-of-arrays
		final int oldSize = IAS[cnt].length;
		// 1.5 is the same growth factor as java.util.ArrayList
		int newSize = (int)(oldSize*1.5) + 1;
		if (newSize < 10)
			newSize = 10;
		Log.println("GROW: "+Log.me()+": AIS["+cnt+"] from "+oldSize+" to "+newSize);
		IAS_SIZES[cnt] = newSize;
		hasGrown = true;
		IAS[cnt] = new int[newSize][cnt]; // I replace existing array-of-arrays
		System.arraycopy(tmp, 0, IAS[cnt], 0, oldSize); // and copy tmp to it
		return IAS[cnt][IAS_CNTS[cnt]++]; // then return the new array
	}
	private static boolean hasGrown = false;

	// these arrays MUST be forgotten at clean-up, after the last AlsChain.
	private static void rccCacheCleanup() {
		rccsStart = null;
		rccsEnd = null;
	}

}
