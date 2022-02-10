/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
import static diuf.sudoku.Grid.*;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.*;
import diuf.sudoku.solver.UnsolvableException;
import diuf.sudoku.solver.accu.IAccumulator;
import static diuf.sudoku.solver.hinters.Validator.*;
import static diuf.sudoku.solver.hinters.als.AlsChainDebug.ALS_CHAIN_DEBUG_HINTS;
import static diuf.sudoku.utils.Html.colorIn;
import diuf.sudoku.utils.Log;
import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import java.util.Arrays;
import static java.util.Arrays.fill;

/**
 * AlsChain implements the Almost Locked Set XY-Chain Sudoku solving technique.
 * <pre>
 * The XY in XY-Chain just means two values. Two ALSs containing a common value
 * which all see each-other can be chained together such that either:
 * (a) this ALS contains the x-value, so that ALS must contain the y-value, coz
 *     only ONE value may be eliminated from all cells in the ALS before we run
 *     out of values to fill the cells, sending the Sudoku invalid; OR
 * (b) this ALS contains the y-value, so that ALS must contain the x-value.
 * Therefore we can chain ALSs together on common RC-Values (in the RCC) and we
 * know that a value (we know not which) is "pushed" along the chain. Thus when
 * we form a loop we know that ONE value (we know not which) is pushed along it
 * x -> y -> x -> y IN EITHER DIRECTION, hence the name XY-Chain.
 *
 * So all we need do is form a loop that links-back to the startAls on what are
 * called z-values: values common to both the first ALS and the last ALS in the
 * chain EXCEPT the "used" RC-values; there are two of them: the value used to
 * link the second ALS back-to the first ALS, and the value used to link the
 * last ALS back-to the one before it (because they're already used in links).
 * Thus each z-value may be eliminated from any Cell that maybe z (obviously)
 * that sees all occurrences of z in both the first and the last ALS. And that
 * my friends is AlsChaining in a nutshell. Sounds simple enough. sigh.
 *
 * Note that AlsChain can't pull-off AlsXz's double-linked stunt.
 *
 * I'm well proud of the speed of AlsChain, despite it's correctness. I guess
 * hobiwan stumbled over a hack to find a subset of AlsChains quickly. What he
 * did not do was state that it was a hack, which is an offence, IMHO. I state
 * unequivocably that this s__t is hacked; just a lesser hack than hobiwans,
 * which was substantially faster, but slower per elim. Sigh.
 *
 * KRC 2021-10-28 Implemented DUD_BRANCH so now use Tech.ALS_Chain only, with
 * degree = 32, so I search all possible combinations of from 4 upto 32 ALSs,
 * looking for a loop back to the start ALS.
 *
 * KRC 2021-10-31 Discovered max shortest path to a top1465 hint is 26, so
 * reduced Tech.ALS_Chain.degree to 26; so it'll still find-em it'll just take
 * longer, but it won't report 32 when 26 does the job. What I really want is
 * an AlsChain algorithm that always finds the shortest possible path first.
 *
 * KRC 2021-11-04 rccCache: drop IAS. count-only. drop TMP_RCCS. Down to 27.629
 * seconds, which I think is probably about as good as it gets.
 *
 * KRC 2021-11-04 Drop searchFast/recurseFast coz they save only about 800ms
 * and therefore aren't worth the fuss.
 *
 * KRC 2021-12-09 AlsChain is still the slowest (elapsed 12.9 seconds) hinter
 * that I use, so needs more speed that I can't find, which is frusterpating!
 * But AlsChain is twice as fast as UnaryChain per elim (not too shabby). Spent
 * two days failing to refastardise. Need a total rethink. Give up, for now.
 *
 * KRC 2022-01-10 J17 is about 30% slower than J8. Trying to speed-up.
 * PRE: 13,069,468,300  4457  2,932,346  1775  7,363,080  ALS_Chain
 * PST: 13,440,346,200  4457  3,015,558  1775  7,572,026  ALS_Chain
 *
 * KRC 2022-01-12 revert to J8 and part fastardise. Just inline all Idx ops, do
 * do not fully fastardise, which I have done previously and reverted coz it's
 * too bloody complicated, and top1465 batch is only about half a second faster
 * which makes ____all difference in the GUI. 30 seconds matters; 1 does not!
 * NOW: 12,420,265,100  4457  2,786,687  1775  6,997,332  ALS_Chain  13-15-11
 *
 * KRC 2022-01-13 completed partial fastardisation. All Idx ops inlined.
 *                 time(ns) call    t/call elim    t/elim nakedSetNs heat
 * 06-36-38: 12,449,303,700 4457 2,793,202 1775 7,013,692 17,638,700 COOL
 * 06-50-24: 12,631,105,900	4457 2,833,992 1775 7,116,116 19,941,300 WARM
 * 07-03-33: 12,495,092,500	4457 2,803,475 1775 7,039,488 19,665,300 WARM
 * process uses if to set result only when changes
 * 07-25-12: 12,468,242,800 4457 2,797,451 1775 7,024,362 18,869,700 COOL
 * skip alss of 7 (STUVWXYZ_Wing's) is over a second faster!
 * 15-24-41: 11,326,306,115 4482 2,527,065 1792 6,320,483 17,827,116 COOL
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
public final class AlsChain extends AAlsHinter
//implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": COUNTS="+Arrays.toString(COUNTS));
//	}
//	private final long[] COUNTS = new long[4]; // was 12

	// FASTARDISATION: TIMEOUT is how long findHints runs before it gives-up.
	private static final int TIMEOUT = 100; // milliseconds

	// FASTARDISATION: RCC_CACHE[alsIndex][prevCand] contains rcc-indexes for
	// alsIndex having cands other than prevCand. It makes each SUBSEQUENT
	// request for each [alsIndex][prevCand] faster.
	private static final int[][][] RCC_CACHE = new int[MAX_ALSS][VALUE_CEILING][];

	// FASTARDISATION: RCC_CACHE[*][0] is otherwise unused, so it is my ALL.
	private static final int ALL = 0;

	// FASTARDISATION (IN E-MINOR): there's one empty rcc-indexes-array
	private static final int[] EMPTY = new int[0];

	// FASTARDISATION: the .cands of each rccs (coz that's all I need)
	private static final int[] RCC_CANDS = new int[MAX_RCCS];

	// FASTARDISATION: does this alsIndex have any related Rcc's? Just 12.79%
	// of top1465s ALSs are viable branches (rest may still be leafs), so the
	// other 87.21% waste my time. This is a fast way to ignore them.
	private static final boolean[] ALS_HAS_ANY_RELATED_RCCS = new boolean[MAX_ALSS];

	// FASTARDISATION: does alsIndex NOT hint for this firstAls/firstCand?
	// so if DUD_LEAF[alsIndex] then alss[alsIndex] does NOT hint for current
	// firstAls and firstCand, so there's not point repeatedly searching it,
	// because it's still going to NOT hint.
	private static final boolean[] DUD_LEAF = new boolean[MAX_ALSS];

	// FASTARDISATION: When a branch (an ALS) is searched and found to NOT hint
	// it's pointless searching it again in this startAls/startCand so remember
	// it's a DUD, so that future rccCache calls return EMPTY. We record each
	// DUD as we ascend back-up the call stack, "pruning off" the whole path.
	// This is more efficient because it mitigates the exponential growth in
	// workload as the length of the chain increases, finding the same hints in
	// about a third of the time. I'm proud of working this out for myself.
	// HACK: clear DUD_BRANCH per startAls finds 10 less elims in half time.
	// FASTARDISED FASTARDISATION: boolean[] instead of boolean[][] to call
	// System.arraycopy ONCE, instead of 512 times; reducing Stackwork which
	// adds-up coz I'm cleared far too often, still.
	private static final boolean[] DUD_BRANCH = new boolean[MAX_ALSS * REGION_SIZE];

	// FASTARDISED FASTARDISATION: System.arraycopy is faster than Arrays.fill,
	// especially for large arrays. I'm just pissed that System.memset is MIA.
	// DUD_DONOR just remain all false: array is only copied, never changed.
	// It needs to be the larger of DUD_BRANCH.length or DUD_LEAF.length
	private static final boolean[] DUD_DONOR = new boolean[DUD_BRANCH.length];

	// FASTARDISED FASTARDISATION: array-lookup is faster than multiplication,
	// so do all multiplications ONCE; all because java lacks a System.memset.
	// I'm really scraping the bottom of the jar here, looking for the last 5%.
	private static final int[] ALS_BASE = new int[MAX_ALSS];
	static {
		for ( int i=1; i<MAX_ALSS; ++i )
			ALS_BASE[i] = i * REGION_SIZE;
	}

	// FASTARDISATION: first index of Rcc's that're related to each Als.
	private static int[] rccsStart;
	// FASTARDISATION: ceiling for the rcc-index (lastValidRccIndex+1).
	// nb: these arrays aren't mine, so they're set and forgotten in findHints,
	// else I hold the whole RccFinder in memory (it's static anyway, putz).
	private static int[] rccsEnd;

	// ------------------------------------------------------------------------
	// FASTARDISATION: chain* arrays are parallel arrays, each is an attribute
	// of a virtual ChainEntry struct: the ALSs (et al) that are in the chain.
	// chainAlss contains the Almost Locked Sets that are in the current chain.
	// Each possible ALS is tried in the attempt to eliminate buddies shared
	// with the startAls, ergo all possible paths are explored.
	private final Als[] chainAlss;
	private final Idx[] chainIdxs; // chainAlss[i].idx
	// An RCC Restricted Common Candidate is a link between two ALSs, on one or
	// occasionally two common candidates, which all see each other, and do not
	// appear in the physical overlap, if any, of the two ALSs; so that each RC
	// value must be in this ALS or that ALS, but never both. This is essential
	// to how ALS-chains work. A value is effectively pushed along the chain
	// (knocking-over each ALS in turn) because an ALS may have just ONE value
	// eliminated before it goes invalid because it has insufficient values to
	// fill its cells. This works in both directions, hence if we can loop back
	// to the start ALS we know that one of these two ALSs must contain the
	// common candidate/s, eliminating those value/s from any common buddies.
	private final Rcc[] chainRccs;
	// a bitset of used candidate value, so 1 set bit in pos 0..8 meaning 1..9.
	// ALSs are back-linked: chainsAlss[1] -chainCands[1]-> chainAlss[0];
	//                       chainsAlss[2] -chainCands[2]-> chainAlss[1];
	//                       ... and so on ...
	// hence chainCands[0] is unused coz chainAlss[0] (first) has no backlink
	private final int[] chainCands;
	// indices of all cells in the-chain-so-far
	private final Idx[] chainSoFar;
	// for debugging only
	private final int[] chainRccIndexes;
	// These methods localise swap from ChainEntry[] to parallel-arrays.
	// They help, a bit, but they're not the whole hog.
	private void chainClear() {
		Arrays.fill(chainAlss, null);
		Arrays.fill(chainRccs, null);
		Arrays.fill(chainCands, 0);
		chainSoFar[0] = null;
		for ( int i=1; i<degree; ++i )
			chainSoFar[i].clear();
	}
	private Als[] chainAlss(int size) {
		return Arrays.copyOf(chainAlss, size);
	}
	private int[] chainValues(int size) {
		final int[] result = new int[size];
		for ( int i=0; i<size; ++i )
			result[i] = VFIRST[chainCands[i]];
		return result;
	}
	private Rcc[] chainRccs(int size) {
		return Arrays.copyOf(chainRccs, size);
	}
	// RETAIN: for debugging only, which I need to make sense of it all
	private int[] chainRccIndexes(int size) {
		return Arrays.copyOf(chainRccIndexes, size);
	}
	// ------------------------------------------------------------------------

	// indices of cells from which we can remove this z-value
	private final Idx victims = new Idx();
	// the removable (red) potentials (Cell=>Cands)
	private final Pots reds = new Pots();
	// the last valid chain index, ie degree - 1
	private final int last;

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
	// the hint accumulator, to which I add hints
	private IAccumulator accu;
	// is it a SingleHintsAccumulator, ergo are we in the batch
	private boolean oneOnly;
	// --------------------------------------------------

	// --------------------------------------------------
	// these fields are set in search and read down in process
	// firstAls.buds (the firstAls is chain[0].als)
	private Idx firstBuds;
	// firstAls.vBuds (the firstAls is chain[0].als)
	private Idx[] firstVBuds;
	// bitset of candidates available as z-values: firstMaybes & ~firstUsedCand
	private int usableFirstCands;
	// --------------------------------------------------

	public AlsChain() {
		// nb: ALS_Chain cannot be forwardOnly because
		// ONLY all rccFinders set rccsStart and rccsEnd.
		super(Tech.ALS_Chain, true, true, true, false);
		// nb: degree is currently 26 (the longest chain <= 32)
		chainAlss = new Als[degree];
		chainIdxs = new Idx[degree];
		chainRccs = new Rcc[degree];
		chainCands = new int[degree];
		chainSoFar = new Idx[degree];
		chainRccIndexes = new int[degree];
		// nb: chainSoFar[0] is set to als.idx on the fly
		for ( int i=1; i<degree; ++i )
			chainSoFar[i] = new Idx();
		// the last valid index in the chain* arrays
		last = degreeMinus1;
	}

	@Override
	protected boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu) {
		boolean result = false;
		this.oneOnly = accu.isSingle();
		try {
			this.grid = grid;
			this.rccs = rccs;
			this.alss = alss;
			this.accu = accu;
			// set-up rcc-indexes-cache
			rccsStart = rccFinder.getStarts();
			rccsEnd = rccFinder.getEnds();
			rccCacheSetup(rccs, numRccs);
			// do the search
			search(numAlss);
			// sort the results
			if ( accu.size() > 1 ) // batch NEVER
				accu.sort(null);
			result = accu.size() > 0;
		} catch ( OverflowException eaten ) {
			// too many hints
			accu.sort(null);
			result = true;
		} finally {
			// clean-up
			chainClear();
			this.grid = null;
			this.rccs = null;
			this.alss = null;
			this.accu = null;
			// clean-up rcc-indexes-cache
			rccsStart = null;
			rccsEnd = null;
		}
		return result;
	}

	/**
	 * Search the grid for AlsChains. I add the first two ALSs to the chain,
	 * then I call recurse to add the third ALS onwards.
	 */
	private void search(final int numAlss) {
		Rcc rcc; // the current Restricted Common Candidate
		Als firstAls, secondAls; // first and second Als: chainAlss[0, 1]
		Idx t; // temporary Idx
		int[] kids; // indexes of rccs that're related to this ALS
		int i1,i2 // alss-index of first and second als
		  , j,lastJ // rccs-index and rccs-index-ceiling
		  , firstMaybes // the maybes of the firstAls
		  , i; // the ubiqitious index
		// presume that no hints will be found
		boolean any = false;
		// for the timeout mechanism
		final long stop = currentTimeMillis() + TIMEOUT;
		// set-up ALS_HAS_ANY_RELATED_RCCS (we need them all before we start)
		for ( i=0; i<numAlss; ++i )
			ALS_HAS_ANY_RELATED_RCCS[i] = rccsEnd[i] - rccsStart[i] > 0;
		// foreach ALS, which I use ONLY to look-up the-rccs-for-this-als
		for ( i1=0; i1<numAlss; ++i1 ) {
			// foreach firstRcc in rccs-related-to-the-firstAls
			if ( ALS_HAS_ANY_RELATED_RCCS[i1] ) {
				firstAls = chainAlss[0] = alss[i1];
				chainIdxs[0] = firstAls.idx;
				firstMaybes = firstAls.maybes;
				firstBuds = firstAls.buds;
				firstVBuds = firstAls.vBuds;
				chainSoFar[0] = firstAls.idx;
				// HACK: reset DUD_BRANCH/LEAF per firstAls, not per secondAls
				// and usedCand, takes half the time to find 10 less elims, coz
				// reset is slow, and most duds are duds for da whole firstAls,
				// not just for this secondAls and usedCand.
				arraycopy(DUD_DONOR, 0, DUD_BRANCH, 0, DUD_BRANCH.length);
				arraycopy(DUD_DONOR, 0, DUD_LEAF, 0, DUD_LEAF.length);
				for ( j=rccsStart[i1],lastJ=rccsEnd[i1]-1; ; ) {
					// rcc.related is the index of our second ALS
					if ( ALS_HAS_ANY_RELATED_RCCS[rccs[j].related] ) {
						// both of first-two ALSs are in the firstRcc, but only
						// the second ALS uses a candidate (the first doesn't).
						// nb: ALSs are back-linked: an ALS "uses" an RC-value
						// to link back-to the previous ALS in the chain; and
						// the next link is on a different value.
						i2 = (chainRccs[0]=chainRccs[1]=rcc=rccs[j]).related;
						// chainAlss[1] is the second ALS
						secondAls = chainAlss[1] = alss[i2];
						chainIdxs[1] = secondAls.idx;
						// chainSoFar[1] is the aggregate of both ALSs so far
						chainSoFar[1].setOr(chainSoFar[0], secondAls.idx);
						// for debugging only
						chainRccIndexes[0] = chainRccIndexes[1] = j;
						// foreach cand (usually one, just ~5% have two)
						for ( int usedCand : VSHIFTED[rcc.cands] )
							if ( (kids=rccCache(i2, usedCand)) != EMPTY ) {
								chainCands[1] = usedCand;
								usableFirstCands = firstMaybes & ~usedCand;
								if ( (any|=recurse(kids, usedCand, 2)) && oneOnly )
									return;
							}
					}
					if ( currentTimeMillis() > stop ) {
						timeout(i1, numAlss);
						return;
					}
					if ( ++j > lastJ )
						break;
				}
			}
		}
	}

	/**
	 * Recurse explores all possible combinations of ALSs
	 * at-and-to-the-right-of index 'n' in the chain.
	 * <p>
	 * Recurse handles the RCCs, and process handles each ALS, leading to more
	 * RCCs, and so on:
	 * <pre>
	 *    recurse rccs -&gt; process als -&gt; recurse rccs -&gt; process als ...
	 * </pre>
	 * An RCC is <u>just</u> the relationship between two ALSs. Rcc.source is
	 * the index in the alss array of the source ALS, and Rcc.related is the
	 * index in alss of the related ALS. Each ALS can be related to 0..many
	 * other ALSs. AlsChain's als-relationships are bidirectional, ie each
	 * relationship is listed twice: A-&gt;B and B-&gt;A, to keep the search
	 * algorithm simple (which doesn't appear to have worked-out very well).
	 *
	 * @param relatedRccIndexes the indexes of RCCs related to the previous ALS
	 *  in the chain (n-1)
	 * @param prevCand a bitset of the value used to form the previous link,
	 *  which is excluded from candidates available to form the current link
	 * @param n the number of ALSs in chainAlss; you always call me with 2, and
	 *  I find the third-onwards als by calling myself, to explore all possible
	 *  combinations of alss that start with the chain asat n-1
	 * @return any hint/s found
	 */
	private boolean recurse(final int[] relatedRccIndexes, final int prevCand
			, final int n) {
		assert relatedRccIndexes != EMPTY;
		Rcc rcc; // the current Rcc we're processing
		final int m = n - 1;
		boolean result = false;
		for ( int i : relatedRccIndexes ) {
			rcc = rccs[i];
			if ( !isLoop(alss[rcc.related].idx, m)
			  // rccCache fails to do this in Shft-F5, so I must ignore them.
			  && (rcc.cands & ~prevCand) != 0
			) {
				chainRccs[n] = rcc;
				chainRccIndexes[n] = i; // for debugging only
				// handle multiple usableCands, one-at-a-time
				for ( int usedCand : VSHIFTED[rcc.cands & ~prevCand] )
					// process the related als
					if ( (result|=process(rcc.related, chainCands[n]=usedCand, n))
					  && oneOnly )
						return result;
			}
		}
		return result;
	}

	// KRC 2022-01-12 FASTARDISED: inline all AlsChains Idx ops for speed.
	// nb: The profiler said isLoop is slow. Do NOT trust the profiler! It
	// seems to be more of a measure of how many calls you make, coz the
	// profiler puts a heavy impost on each call, especially for low level
	// methods where the overheads outweigh the actual work.
	// PRE: 12,393,073,100  4457  2,780,586  1775  6,982,013  ALS_Chain  NS 18,651,600 = COOL
	// PST: 13,284,782,100  4457  2,980,655  1775  7,484,384  ALS_Chain  NS 25,474,300 = HOT
	// it slower, but NakedSingle (NS) 18,651,600 vs 25,474,300 = HOT!
	// it was also slower on 3 subsequent tries with small cooldown time;
	// then I reverted it and went for a durry (~6 mins) and reran:
	// PRE: 12,449,738,600  4457  2,793,300  1775  7,013,937 ALS_Chain  NS 18,428,600 = COOL
	// reinstanted inline Idx ops
	// PST: 12,396,310,000  4457  2,781,312  1775  6,983,836 ALS_Chain  NS 19,232,900 = WARM
	// single-stack-frame; set i0,i1,i2 in the if statement
	// PST: 12,372,791,500  4457  2,776,035  1775  6,970,586 ALS_Chain  NS 17,792,600 = COOL
	/**
	 * Returns does 'als' make a loop out of the chain? Ergo is als NOT ok?
	 * Ergo does this ALS cause a "tangle". If an ALS wholey contains another
	 * in the chain then AlsChains "push" logic is borken, so we avoid adding
	 * any ALS that wholey contains any other ALS that is already in the chain,
	 * and vice-versa (if a chained-ALS contains this ALS then skip this ALS).
	 *
	 * @param alsIdx indices of cells in current als to test
	 * @param m last valid index in the chain (ie n - 1)
	 * @return is als NOT ok?
	 */
	private boolean isLoop(final Idx alsIdx, final int m) {
		// FASTARD: explode all Idx ops, for speed.
		// FASTARD: a single stack-frame (SSF), for speed.
		Idx t; // temporary idx pointer
		int i, m0,m1,m2; // an exploded temp Idx: chain.intersection(als)
		// FASTARD: explode the als.idx ONCE, for speed; pre-declare for SSF
	    // FASTARDISED FASTARD: set them in the if, only when needed.
		final int i0, i1, i2;
		// if the chainSoFar containsAll als cells
		// FASTARD: explode all Idx ops, for speed.
//		if ( chainSoFar[m].hasAll(alsIdx) )
		if ( ((t=chainSoFar[m]).a0 & (i0=alsIdx.a0)) == i0
		  && (t.a1 & (i1=alsIdx.a1)) == i1
		  && (t.a2 & (i2=alsIdx.a2)) == i2 ) {
//++COUNTS[0]; // 34,462,687 // 37.93%
			// previous ALS always related, so most likely to form a loop
			for ( i=m; i>-1; --i ) {
//++COUNTS[1]; // 179,524,691
				// if the currentAls contains all cells in this chainAls
				// or this chainAls contains all cells in the currentAls
				// FASTARD: explode all Idx ops, for speed.
//				if ( chainAlss[i].idx.inbread(alsIdx) )
				m0 = (t=chainIdxs[i]).a0 & i0;
				m1 = t.a1 & i1;
				m2 = t.a2 & i2;
				if ( (m0==t.a0 && m1==t.a1 && m2==t.a2)
				  || (m0==i0 && m1==i1 && m2==i2) )
//				{
//++COUNTS[2]; // 24,540,678
					return true;
//				}
			}
		}
//else ++COUNTS[3]; // 56,400,342 // 62.07%
		return false;
	}

	// KRC 2022-01-12 FASTARDISED: do all Idx ops inline for speed
	// PRE: 12,759,353,300  4457  2,862,767  1775  7,188,368  ALS_Chain
	// PST: 12,393,073,100  4457  2,780,586  1775  6,982,013  ALS_Chain
	// KRC 2022-01-13 Keep trying stuff
	// exploded firstBuds into fb0,fb1,fb2
	// NOW: 12,469,141,500  4457  2,797,653  1775  7,024,868 ALS_Chain  NS 17,794,700 = COOL
	// Na it's ____ing slower. Sigh. Reverted.
	// NOW: 12,449,303,700  4457  2,793,202  1775  7,013,692 ALS_Chain  NS 17,638,700 = COOL
	// FMS it's STILL Slower. What now? Sigh.
	/**
	 * Process this Als, only called in the multi-RC-value loop, ie rarely.
	 * <p>
	 * Note that recurse deals with the RCCs and process deals with the ALSs.
	 *
	 * @param alsIndex the index in alss of the Almost Locked Set to process
	 * @param usedCand the cand that's currently used (one of the usableCands);
	 *  ie a bitset of the RC-value linking the current ALS to the previous ALS
	 *  in the chain. This RC-value is excluded from the z-values linking this
	 *  ALS back-to the firstAls, and is unavailable as the next link value so
	 *  that each link value is distinct, so that the chain of ALSs all fall
	 *  down like dominoes, in either direction, forming a possible AlsChain
	 * @param n the number of ALS's already in chainAlss when you call me
	 * @return any hint/s found?
	 */
	private boolean process(final int alsIndex, final int usedCand, final int n) {
		// presume no hints will be found. It's a reasonable assumption. sigh.
		boolean result = false;
		// 1. Search this "leaf" for an AlsChain.
		// if there's atleast 4 ALSs in the chain
		if ( n > 2 // nb: n is a 0-based index, so >=3 is 4 in the chain
		  // and ALS is NOT already known to not hint
		  && !DUD_LEAF[alsIndex]
		) {
			Idx t0, t1;
			final Als als;
			int zs;
			int z // current zValue
			  , v0,v1,v2; // victims Idx exploded
			final Idx[] alsVBuds; // als.vBuds
			// if any zs: cands common first ALS and last ALS except used cands
			// (the used cands are the v that links second ALS back-to first,
			// and the v that links last ALS back to penultimate ALS in chain).
			// nb: ONCE: usableFirstCands = firstAls.maybes & ~firstUsedCand.
			// nb: zs first coz faster tho less detic; but is measurably faster
			// overall, which I noncomprendez. Reality trumps doctrine.
			if ( (zs=usableFirstCands & (als=alss[alsIndex]).maybes & ~usedCand) != 0
			  // and firstBuds intersects als.buds (ie some possible victims).
			  // nb: exploded firstBuds fb0,fb1,fb2 was slower.
			  && ( ((t0=firstBuds).a0 & (t1=als.buds).a0)
				 | (t0.a1 & t1.a1)
				 | (t0.a2 & t1.a2) ) != 0
			) {
				// cache is line-ball, coz about a third have only one zValue.
				alsVBuds = als.vBuds;
				// foreach zValue (posttested coz zs!=0 so there's atleast one)
				do {
					// if there are any external zs which see all zs in both
					// first and last ALS, then they can be removed.
					// nb: vBuds already contains external cells only so and
					// with another vBuds yields external cells seeing all zs
					// in both ALSs. This is why bitsets are SO much faster
					// than doing it all per cell. A bit clever.
					// nb: seems to faster to do this with an if to set result
					// only when result changes than all in one logical s'ment.
					if ( ( (v0=(t0=firstVBuds[z=VFIRST[zs]]).a0 & (t1=alsVBuds[z]).a0)
						 | (v1=t0.a1 & t1.a1)
						 | (v2=t0.a2 & t1.a2) ) != 0 )
						result |= reds.upsertAll(victims.set(v0,v1,v2), grid, z);
				} while ( (zs &= ~VSHFT[z]) != 0 );
				if ( result )
					result = createHint(als, n);
			}
			if ( !result )
				// remember this als doesn't hint (averts ALL usedCand)
				DUD_LEAF[alsIndex] = true;
		}
		// 2. "branch" to add the next (n+1) ALS
		// if the chain isn't already full
		if ( n < last
		  // and this ALS has any related RCCs
		  && ALS_HAS_ANY_RELATED_RCCS[alsIndex]
		) {
			// predeclare here for ONE extra stack-frame
			Idx t0, t1, t2;
			final int[] kids;
			// if any related RCCs with candidates OTHER THAN usedCand.
			// EMPTY means none OR this branch is already known to not hint.
			if ( (kids=rccCache(alsIndex, usedCand)) != EMPTY ) {
				// add the current ALS to chainAlss
				chainIdxs[n] = (chainAlss[n]=alss[alsIndex]).idx;
				// add the current ALS to chainSoFar
				// current level = previous level | current ALS
				//KEEP4DOC: chainSoFar[n].setOr(chainSoFar[n-1], chainAlss[n].idx);
				(t0=chainSoFar[n]).a0 = (t1=chainSoFar[n-1]).a0 | (t2=chainIdxs[n]).a0;
				t0.a1 = t1.a1 | t2.a1;
				t0.a2 = t1.a2 | t2.a2;
				// recurse the next (n+1) rcc/als
				if ( !(result |= recurse(kids, usedCand, n+1)) )
					// remember branch doesn't hint, to avoid repeat search
					DUD_BRANCH[ALS_BASE[alsIndex]+IFIRST[usedCand]] = true;
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
	private boolean createHint(final Als als, final int n) {
		assert !reds.isEmpty();
//		minGoodNumRccs = Math.min(minGoodNumRccs, numRccs);
//		maxGoodNumRccs = Math.max(maxGoodNumRccs, numRccs);
		// adding the last als to the chain is delayed until we hint!
		chainSoFar[n].setOr(chainSoFar[n-1], chainIdxs[n]=(chainAlss[n]=als).idx);
		final AlsChainHint hint = new AlsChainHint(this, reds.copyAndClear()
			, chainAlss(n+1), chainValues(n+1), chainRccs(n+1)
			, ALS_CHAIN_DEBUG_HINTS ? chainRccIndexes(n+1) : null);
		if ( VALIDATE_ALS ) {
			if ( !validOffs(grid, hint.reds) && !handle(hint) ) {
				return false; // without adding it!
			}
		}
		accu.add(hint);
		// Avoid sans-TIMEOUT OutOfMemoryError: confusing coz search/recurse
		// don't use "new", so it must be too many hints, so stop at 64 hints.
		if ( accu.size() > 63 )
			throw new OverflowException();
		return true; // regardless of what accu.add says (handled up the stack)
	}

	/**
	 * Handle a bad hint.
	 *
	 * @param badHint to handle appropriately in the current context.
	 * @return should we add this hint to IAccumulator anyway?
	 */
	private boolean handle(final AlsChainHint badHint) {
		reportRedPots(tech.name(), grid, Als.list(badHint.alss));
		switch ( Run.type ) {
		case GUI:
			if ( Run.ASSERTS_ENABLED ) {
				badHint.setIsInvalid(true);
				badHint.setDebugMessage(colorIn("<p><r>"+prevMessage+"</r>"));
				return true;
			}
			return false;
		case Batch:
			return false;
		default:
			throw new UnsolvableException(Log.me()+": bad reds: "+badHint.reds);
		}
	}

	// Disable AlsChain for this puzzle only (I'll get you next time Batman)
	private void timeout(final int alsIndex, final int numAlss) {
		setIsEnabled(false);
		Log.format("TIMEOUT: %s DISABLED! %d ms exceeded in %s at i=%d/%d\n"
				, tech.name(), TIMEOUT, grid.hintSource(), alsIndex, numAlss);
	}

	// Setup the rccs-cache
	private static void rccCacheSetup(final Rcc[] rccs, final int numRccs) {
		for ( int i=0; i<numRccs; ++i )
			RCC_CANDS[i] = rccs[i].cands;
		for ( int i=0; i<MAX_ALSS; ++i )
			fill(RCC_CACHE[i], null);
	}

	/**
	 * rccCache gets indexes of rccs related to 'ai' (alsIndex)
	 * with cands OTHER THAN 'pc' (prevCand).
	 * <p>
	 * rccCache is faster, but FMS its complicated, which I must apologise for.
	 * It turned into War and Peace while I wasn't looking. I tried to KISS it,
	 * but simplicity is at odds with speed, which we need, coz it's hammered!
	 *
	 * @param ai alsIndex: the index of the source ALS in the alss array
	 * @param pc prevCand: we need rccs with cands other than pc, so each link
	 *  in chainAlss has a different value, so that they fall-over like
	 *  dominoes, in either direction
	 * @return indexes of rccs related to ai having cands other than pc
	 */
	private static int[] rccCache(final int ai, final int pc) {
		// validate params
		assert rccsEnd[ai]-rccsStart[ai] > 0;
		assert VSIZE[pc] == 1 : "Not one bit pc="+pc;
		// see if it's a known dud
		if ( DUD_BRANCH[ALS_BASE[ai]+IFIRST[pc]] )
			return EMPTY;
		// fetch from the cache
		final int pv = VFIRST[pc];
		final int[][] alsCache = RCC_CACHE[ai];
		if ( alsCache[pv] != null )
			return alsCache[pv];
		// cache miss, so count them
		final int start = rccsStart[ai], end = rccsEnd[ai];
		int i=start, size=0;
		for ( ; i<end; ++i )
			if ( (RCC_CANDS[i] & ~pc) != 0 )
				size++;
		// we can skip none
		if ( size == 0 )
			return alsCache[pv] = EMPTY;
		// check for all
		final boolean isAll = size==end-start;
		if ( isAll && alsCache[ALL]!=null )
			return alsCache[pv] = alsCache[ALL];
		// create result and add it to the cache/s
		final int[] result = alsCache[pv] = new int[size];
		if ( isAll )
			alsCache[ALL] = result;
		// return it
		size = 0;
		for ( i=start; i<end; ++i )
			if ( (RCC_CANDS[i] & ~pc) != 0 )
				result[size++] = i;
		return result;
	}

	// we've already found too many bloody hints, so we stop here.
	private static class OverflowException extends RuntimeException {
		private static final long serialVersionUID = 4539675628269L;
	}

}
