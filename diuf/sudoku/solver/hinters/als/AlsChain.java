/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
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
import static diuf.sudoku.Grid.REGION_SIZE;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.solver.hinters.IHinter.DUMMY;
import static java.util.Arrays.copyOf;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import static diuf.sudoku.solver.hinters.als.AAlsHinter.MAX_ALSS;
import diuf.sudoku.utils.Log;
import static diuf.sudoku.utils.IntArray.THE_EMPTY_INT_ARRAY;
import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.copyOfRange;

/**
 * AlsChain implements the {@link Tech#ALS_Chain} Sudoku solving technique. An
 * Als_Chain is an XY-Chain of 4+ Almost Locked Sets. The term "XY-Chain" has a
 * different meaning in AlsChains than it does in chains-XY-Chains.
 * <pre>
 * Here the XY in XY-Chain just means two values. Two ALSs with a common value
 * that all see each-other can be chained together such that either:
 * (a) this ALS contains the x-value, so that ALS must contain the y-value, coz
 *     only ONE value may be eliminated from all cells of an ALS before we run
 *     out of values to fill the cells, sending the Sudoku invalid; OR
 * (b) this ALS contains the y-value, so that ALS must contain the x-value.
 *
 * Hence we can chain ALSs together on common RC-Values (in the RCC), and we
 * know that a value (we know not which) is "pushed" along the chain. Thus when
 * we make a loop we know that ONE value (we know not which) is pushed along it
 * x -> y -> x -> y IN EITHER DIRECTION, hence the name XY-Chain.
 *
 * So all we need do is make a loop that links-back to the startAls on what are
 * called z-values: values common to both the first and last ALS in the chain
 * EXCEPT the "used" RC-values; there are two of them: the value used to link
 * the second ALS back-to the first ALS, and the value used to link the last
 * ALS back-to the one before it (because they are already used in links). Thus
 * each z-value may be eliminated from any Cell that maybe z (obviously) that
 * sees all occurrences of z in both the first and last ALS. Thats AlsChaining
 * in a nutshell. Sounds simple enough.
 *
 * Note that AlsChain cannot pull-off AlsXzs double-linked stunt. ONE value is
 * pushed along the chain.
 *
 * I am proud of AlsChains speed, but its a hack. Hobiwans hack found a subset
 * of AlsChains quickly, but he did not state it was a hack. I state that this
 * s__t is hacked; just a lesser hack than hobiwans, which was quicker, but my
 * hack finds more elims, so its faster per elim. Search for HACK: to see the
 * joy of laziness. I wonder if hobiwan knew his s__t was a hack? Probably. He
 * was no idiot.
 *
 * The "best" ALS_Chain.degree is a matter of taste. Lowest that produces all
 * possible (AFAIK) hints is 26, but I use 13 for expediancy. Every extra link
 * in the chain takes time to find. I think 13 is a good compromise between
 * time and correctness. It finds ~90% of hints in ~%80 of time. I dont know
 * why its not faster, but then again I am an idiot who doesnt know enough to
 * ask the right question. Anyway, most of my time is spent in RccFinderAll.
 *
 * Any AlsChain-enspeedonating will happen in RccFinderAll. Good luck with it.
 * The only way I know how to make RccFinderAll faster is lower MAX_ALS_SIZE.
 * Currently I use 5 (HACK). 8 is correct. 7 is a bit faster. 6 faster still.
 * 5 finds ~80% of hints in ~37% of the time. Bargain. This is a HACK!
 * See {@link AAlsHinter#MAX_ALS_SIZE} comments for the details.
 *
 * KRC 2021-10-28 Implemented DUD_BRANCH so now use Tech.ALS_Chain only, with
 * degree = 32, so I search all possible combinations of from 4 upto 32 ALSs,
 * looking for a loop back to the start ALS.
 *
 * KRC 2021-10-31 Discovered max shortest path to a top1465 hint is 26, so
 * reduced Tech.ALS_Chain.degree to 26; so it will still find-em it will just take
 * longer, but it will not report 32 when 26 does the job. What I really want is
 * an AlsChain algorithm that always finds the shortest possible path first.
 *
 * KRC 2021-11-04 rccCache: drop IAS. count-only. drop TMP_RCCS. Down to 27.629
 * seconds, which I think is probably about as good as it gets.
 *
 * KRC 2021-11-04 Drop searchFast/recurseFast coz they save only about 800ms
 * and therefore are not worth the fuss.
 *
 * KRC 2021-12-09 AlsChain is still the slowest (elapsed 12.9 seconds) hinter
 * that I use, so needs more speed that I cannot find, which is frusterpating!
 * But AlsChain is twice as fast as UnaryChain per elim (not too shabby). Spent
 * two days failing to refastardise. Need a total rethink. Give up, for now.
 *
 * KRC 2022-01-10 J17 is about 30% slower than J8. Trying to speed-up.
 * PRE: 13,069,468,300  4457  2,932,346  1775  7,363,080  ALS_Chain
 * PST: 13,440,346,200  4457  3,015,558  1775  7,572,026  ALS_Chain
 *
 * KRC 2022-01-12 revert to J8 and part fastardise. Just inline all Idx ops, do
 * do not fully fastardise, which I have done previously and reverted coz it is
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
 * skip alss of 7 (STUVWXYZ_Wings) is over a second faster!
 * 15-24-41: 11,326,306,115 4482 2,527,065 1792 6,320,483 17,827,116 COOL
 *
 * KRC 2023-06-29 use vBudsX (exploded Idx) instead of vBuds
 * PRE: 17,563,046,200  5744  3,057,633  1989  8,830,088  ALS_Chain
 * PST: 17,594,444,100  5744  3,063,099  1989  8,845,874  ALS_Chain
 *
 * KRC 2023-07-11 As a matter of interest I decided to try AlsChainGui in the
 * batch, and found to my disgust that it was faster than AlsChainFast, so the
 * whole complex mess got the ass. This is AAlsChain with bits of AlsChainGUI.
 * AlsChainFast can go and get ____ed! Indeed he has!
 * PST: 17,881,650,700  5741  3,114,727  2012  8,887,500  ALS_Chain
 *
 * KRC 2023-08-01 still pissin about trying to make this faster.
 * reverted 2023-06-29 to use Idx ops instead of exploded Idx array ops. Most
 * of this speed increase is from a better RccFinderAll, not this change. Its
 * still along way from my best of 12.4 secs, but I am happy enough with this.
 * You dont see a few batch seconds in the GUI. I just wish I was smart enough
 * to do it all in milliseconds, thats all.
 * PST: 16,431,098,300  5549  2,961,091  1995  8,236,139  ALS_Chain
 *
 * KRC 2023-08-04 still pissin about
 * PST: 18,634,508,400  5549  3,358,174  1995  9,340,605  ALS_Chain SLOWER!
 *
 * KRC 2023-08-04 lowered {@link AAlsHinter#MAX_ALS_SIZE} to 5 coz AlsChain is,
 * despite my best efforts, still too slow. This finds ~80% elims in ~40% time.
 * Bargain! There is no way to reduce maxAlsSize for AlsChain only. It would
 * require public sized sets instead of the alss array; making all als-hinters
 * too complex, not just AlsChain, thats already past-it, IMHO.
 *
 * KRC 2023-12-03 use set/clearFields to free memory between calls. This is a
 * bit slower in AlsChain but speads everything else up coz I'm a RAM hog.
 * PRE: AlsChain.findAlsHints  4,220ms (10.9%) of 38,490ms
 * PST: AlsChain.findAlsHints  4,351ms (11.3%) of 38,256ms
 * </pre>
 *
 * @author Keith Corlett 2020-05-24
 */
public final class AlsChain extends AAlsHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		Log.teeln("AlsChain: COUNTS="+Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[20];

	// FASTARDISATION: TIMEOUT is how long findHints runs before it gives-up.
	private static final int TIMEOUT = 100; // milliseconds

	// number of elements in the RCC_CACHE: 1536 * 9 = 13,824
	private static final int RCC_CACHE_SIZE = MAX_ALSS * REGION_SIZE;

	// number of longs required to hold MAX_ALSS*REGION_SIZE bits
	private static final int DUD_BRANCH_SIZE = MAX_ALSS * REGION_SIZE / 64;

	// NONE is a euphamism for THE_EMPTY_INT_ARRAY, to say there are
	// no related "child" rcc-indexes for this alsIndex and prevCand.
	// It exists for brevity and clarity. Note that null will NOT do.
	// null means yet-to-be-determined, NONE means there are none.
	private static final int[] NONE = THE_EMPTY_INT_ARRAY;

	// FASTARDISED FASTARDISATION: ALS_BASE is just i * REGION_SIZE (9).
	// An array-lookup is faster than a multiplication, so we do all common
	// multiplications ONCE. And all because java lacks a System.memset. I am
	// really scraping the bottom of the barrel here, looking for the last 5%,
	// when theres probably an O(n-log-n) algorithm out there begging to be...
	private int[] alsBase;

	// FASTER FASTARDISATION: RCC_CACHE[ALS_BASE[alsIndex] + IFIRST[prevCand]]
	// contains rcc-indexes for alsIndex having cands other than prevCand,
	// making every SUBSEQUENT request for each branch faster. Theres usually
	// many subsequent requests, so a high hit-rate.
	// My index is interesting too. ALS_BASE is just i*9 plus IFIRST[prevCand]
	// which is 0-based: 9 elements for 9 distinct values, so we can get the
	// rcc-cache-index using only array-lookup and addition, for speed!
	// Calculating my index quickly is essential for AlsChains speed, because
	// kids does it something like 100 million times per top1465 batch run.
	private int[][] rccCache;
	// FASTARDISATION: cache indexes in the cache instead of iterating twice
	// nb: max size is 45 in top1465 (which I presume happened in AlsChain)
	private int[] cacheCache;

	// FASTARDISATION: does this alsIndex have any related Rccs? Just 12.79%
	// of top1465s ALSs are viable branches (rest may still be leafs), so the
	// other 87.21% waste my time. This is a fast way to ignore them.
	private boolean[] anyRelated;
	// FASTARDISATION: does alsIndex NOT hint for this firstAls/firstCand?
	// so if dudLeaf[alsIndex] then alss[alsIndex] does NOT hint for current
	// firstAls and firstCand, so there is not point repeatedly searching it,
	// because its still going to NOT hint.
	private boolean[] dudLeaf;
	// FASTARDISED FASTARDISATION: System.arraycopy is faster than Arrays.fill,
	// especially for large arrays. Im just annoyed that System.memset is MIA.
	private boolean[] dudLeafDonor;
	// FASTARDISATION: When a branch (an ALS) is searched and found to NOT hint
	// its pointless searching it again in this startAls/startCand so remember
	// its a DUD, so that future rccCache calls return EMPTY. We record each
	// DUD as we ascend back-up the call stack, "pruning off" the whole path.
	// This is more efficient because it mitigates the exponential growth in
	// workload as the length of the chain increases, to find the same hints in
	// about a third of the time. I am proud of working this out for myself.
	// NOTE: index = ALS_BASE[alsIndex] + IFIRST[usedCand]. ALS_BASE is faster
	// alsIndex*9. IFIRST[usedCand] is zero-based first usedCand (usedCand is
	// one bit) rather than "normal" 1-based VFIRST, which would waste space.
	// HACK: clear dudBranch per startAls finds 10 less elims in half time.
	// FASTARDISED FASTARDISATION: boolean[] instead of boolean[][] to call
	// System.arraycopy ONCE, instead of 512 times, which is faster.
	private long[] dudBranch;
	// FASTARDISED FASTARDISATION: System.arraycopy is faster than Arrays.fill,
	// especially for large arrays. Im just annoyed that System.memset is MIA.
	private long[] dudBranchDonor;

	// FASTARDISATION: first index of Rccs that are related to each Als.
	private int[] rccsStart;
	// FASTARDISATION: ceiling for the rcc-index (lastValidRccIndex+1).
	private int[] rccsEnd;

	// FASTARDISATION: chain* arrays are parallel arrays, each is an attribute
	// of a virtual ChainEntry struct: the ALSs (et al) that are in the chain.
	// chainAlss contains the Almost Locked Sets that are in the current chain.
	// Each possible ALS is tried in the attempt to eliminate buddies shared
	// with the startAls, ergo all possible paths are explored.
	private Als[] chainAlss;
	// idxs of each chainAls, rather than repeatedly dereference, for speed
	private Idx[] chainIdxs;
	// a bitset of used candidate value, so 1 set bit in pos 0..8 meaning 1..9.
	// ALSs are back-linked: chainsAlss[1] -chainCands[1]-> chainAlss[0];
	//                       chainsAlss[2] -chainCands[2]-> chainAlss[1];
	//                       ... and so on ...
	// hence chainCands[0] is unused coz chainAlss[0] (first) has no backlink
	private int[] chainCands;
	// indices of all cells in the-chain-so-far
	private Idx[] chainSoFar;

	// these fields are set in search and read down in process
	// firstAls.vBuds (the firstAls is chain[0].als)
	private Idx[] firstVBuds;
	// bitset of candidates available as zs: maybes0 & ~usedCand
	private int firstUsableCands;

	// mine is private coz I need to change it's value in order to ascend the
	// whole recursive callstack.
	private boolean oneHintOnly;

	@Override
	public void setFields(Grid grid) {
		int i;
		this.grid = grid;
		maybes = grid.maybes;
		rccCache = new int[RCC_CACHE_SIZE][];
		cacheCache = new int[64];
		anyRelated = new boolean[MAX_ALSS];
		dudLeaf = new boolean[MAX_ALSS];
		dudLeafDonor = new boolean[MAX_ALSS];
		dudBranch = new long[DUD_BRANCH_SIZE];
		dudBranchDonor = new long[DUD_BRANCH_SIZE];
		alsBase = new int[MAX_ALSS];
		i=1; do alsBase[i] = i * REGION_SIZE; while(++i<MAX_ALSS);
		chainAlss = new Als[degree];
		chainIdxs = new Idx[degree];
		chainCands = new int[degree];
		chainSoFar = new Idx[degree];
		i=1; do chainSoFar[i] = new Idx(); while(++i<degree);
		this.oneHintOnly = super.oneHintOnly;
	}

	@Override
	public void clearFields() {
		this.grid = null;
		rccCache = null;
		maybes = cacheCache = chainCands = alsBase = null;
		anyRelated = dudLeaf = dudLeafDonor = null;
		dudBranch = dudBranchDonor = null;
		chainAlss = null;
		chainIdxs = chainSoFar = null;
	}

	/**
	 * Constructor.
	 */
	public AlsChain() {
		super(Tech.ALS_Chain);
	}

	@Override
	protected boolean findAlsHints() {
		Rcc rcc; // Restricted Common Candidate: a link between two ALSs.
		Als A, B; // first and second Als: chainAlss[0, 1]
		int[] kids; // indexes of rccs that are related to this ALS
		int a, b, r,rn, firstMaybes;
//		if(DEBUG)DEBUG("DEBUG: findAlsHints: "+grid.hintNumber+"/"+grid.sourceShort()+" "+numAlss+"/"+numRccs);
		// for the timeout mechanism
		final long stop = currentTimeMillis() + TIMEOUT;
		boolean result = false;
		// set-up rcc-indexes-cache
		rccsStart = rccFinder.getStarts();
		rccsEnd = rccFinder.getEnds();
		// set-up ALS_HAS_ANY_RELATED_RCCS (need them all before we start)
		// NB: hijack a, instead of creating another var just for this.
		a = 0;
		do
			anyRelated[a] = rccsEnd[a] - rccsStart[a] > 0;
		while (++a < numAlss);
		// foreach ALS, from which I look-up the-rccs-for-this-als, ie
		// her connections to other ALSs (on restricted common candidates)
		a = 0;
		do
			// foreach firstRcc in rccs-related-to-the-firstAls
			if ( anyRelated[a] ) {
				A = chainAlss[0] = ALSS[a];
				chainSoFar[0] = chainIdxs[0] = A.idx;
				firstMaybes = A.maybes;
				firstVBuds = A.vBuds;
				// HACK: reset DUD_BRANCH per firstAls, not per secondAls and
				// usedCand, takes half the time to find 10 less elims, mainly
				// coz reset is slow and most (~99%) duds are duds for da whole
				// firstAls, not just for this secondAls and usedCand.
				// nb: The DUD_DONOR array just remains empty. System.arraycopy
				// faster than fill for large arrays, coz Java has no memset,
				// because it has GC instead, but array creation is TOO SLOW!
				// I suspect they hamstrung it to make Collections look good by
				// comparison, so now everthing sux! But atleast I look good.
				arraycopy(dudBranchDonor, 0, dudBranch, 0, DUD_BRANCH_SIZE);
				arraycopy(dudLeafDonor, 0, dudLeaf, 0, MAX_ALSS);
//				assert diuf.sudoku.utils.LongArrays.isEmpty(DUD_BRANCH);
//				assert diuf.sudoku.utils.BooleanArrays.isEmpty(DUD_LEAF);
				r = rccsStart[a];
				rn = rccsEnd[a];
				do {
					rcc = RCCS[r];
					// rcc.related is the index of our second ALS
					if ( anyRelated[rcc.related] ) {
						// both of first-two ALSs are in the firstRcc, but only
						// the second ALS uses a candidate (first does not).
						// nb: ALSs are back-linked: an ALS "uses" an RC-value
						// to link back-to the previous ALS in the chain, so
						// the next back-link must be on a different value.
						b = rcc.related;
						// chainAlss[1] is the second ALS
						B = chainAlss[1] = ALSS[b];
						chainIdxs[1] = B.idx;
						// an aggregate of all cells in the chain so far
						// current = previous | als
						chainSoFar[1].m0 = chainSoFar[0].m0 | B.idx.m0;
						chainSoFar[1].m1 = chainSoFar[0].m1 | B.idx.m1;
						// foreach RC-value (typically one, ~5% have two)
						for ( int usedCand : VSHIFTED[rcc.cands] ) {
							kids = kids(b, usedCand);
							if ( kids != NONE ) {
								chainCands[1] = usedCand;
								firstUsableCands = firstMaybes & ~usedCand;
								// HACK: reset DUD_BRANCH/LEAF here is correct,
								// but takes about twice as long, because reset
								// is slow, coz there is no System.memset.
								if ( recurse(kids, usedCand, 1, 2, 3) ) {
									result = true;
									if ( oneHintOnly )
										return result;
								}
							}
						}
					}
					// timout only when NOT debugging!
					if ( !Run.ASSERTS_ENABLED
					  && currentTimeMillis() > stop
					  && timeout(a, numAlss) )
						return result;
				} while ( ++r < rn );
			}
		while (++a < numAlss);
		// sort hints by score descending in the GUI
		if ( result && Run.isGui() && accu.size()>1 )
			accu.sort(null);
		return result;
	}

	/**
	 * Returns rcc-indexes related to alss[$alsIndex] having als.cands OTHER
	 * THAN $prevCand. So, I index rcc-indexes by "OTHER THAN $prevCand", on
	 * top of the existing indexes on first..last alsIndex.
	 * <p>
	 * I considered doing this in RccFinderAll, but this is probably faster coz
	 * here its done only per REQUESTED prevCand, not all possible prevCands.
	 * I presume REQUESTED prevCands are a subset of ALL, but they may be ALL.
	 * I presume its impossible to predict which prevCands will be requested.
	 * I presume I am not very clever, so that one of these must be correct.
	 * Every alsIndex is tried, by definition, in a complete tree search.
	 * Complete logic!
	 * <p>
	 * Caching is faster, but FMS its complicated, which I apologise for.
	 * It turned into War and Peace while I wasnt looking. I tried to KISS it,
	 * but simplicity is at odds with speed, which we need, coz its hammered!
	 * <p>
	 * NOTE: I tried moving the DUD_BRANCH check out of kids and passing
	 * in the branchIndex (x), to save on invocation, but it was slower. Sigh.
	 *
	 * @param ai alsIndex is the index of the source ALS in the alss array
	 * @param pc prevCand we seek related rccs with cands other than prevCand,
	 *  so that each link in chainAlss has a different value, so that they
	 *  fall-over like dominoes, in either direction
	 * @return an array of indexes of rccs that are related to alss[alsIndex]
	 *  having RC-values other than prevCand, ergo alsIndexes kids
	 */
	private int[] kids(final int ai, final int pc) {							// 89,994,408
		// get the index of this "branch"
		final int x = alsBase[ai] + IFIRST[pc];
		if ( (dudBranch[x/64] & (1L<<(x%64))) != 0 )							// 61,447,143
			return NONE;
		// YOU check ANY_RELATED before calling kids
		assert rccsEnd[ai] - rccsStart[ai] > 0;
		// YOU assure that each prevCand is ONE candidate
		assert VSIZE[pc]==1 : "Not one bit prevCand="+pc;
		// check cache
		if ( rccCache[x] != null )												// 19,274,862
			return rccCache[x];
		// cache miss, so CACHE_CACHE Rccs with other cands
		int size = 0;															//  9,272,403
		final int n = rccsEnd[ai];
		int i = rccsStart[ai];
		do																		// 86,231,346
			if ( (RCCS[i].cands & ~pc) > 0 ) // 9bits							//  6,561,450
				cacheCache[size++] = i;
		while (++i < n);
		// create result, cache, and return it
		if ( size == 0 ) {														//  8,124,270
			dudBranch[x/64] |= (1L<<(x%64));
			return rccCache[x] = NONE;
		}
		return rccCache[x] = copyOfRange(cacheCache, 0, size);				//  1,148,133
	}

	/**
	 * Recurse explores all combinations of ALSs at-and-to-the-right-of index
	 * $n in the chain, to the depth/length of Tech.ALS_Chain.degree = 26.
	 * <p>
	 * recurse handles each RCC, calling process to handle each ALS, which
	 * calls recurse to handle any related RCCs, and so on:
	 * <pre>
	 * recurse rccs -&gt; process als -&gt; recurse rccs -&gt; process als ... 26
	 * </pre>
	 * <p>
	 * An RCC is <u>just</u> the relationship between two ALSs. Rcc.source is
	 * the index in the alss array of the source ALS, and Rcc.related is the
	 * index in the alss array of the "child" ALS. RCC means Restricted Common
	 * Candidate. Lets dig into what that name actually means.
	 * <p>
	 * The restriction is that in order to be an RC-value, all occurrences of
	 * that value in both ALSs "see" each other (ie same box/row/col). Common
	 * just means this candidate is common to both ALSs, ie one-or-more cells
	 * in both ALSs maybe this candidate. If you dont know what Candidate means
	 * youre in the wrong place. Start at top of {@link Tech} and work down.
	 * S__t gets harder the deeper you go. Theres some nice filth here dear.
	 * <p>
	 * ALS is Almost Locked Set: a set of cells with one more candidates than
	 * cells; eg: 2 cells with 3 values between them. The trick with using ALSs
	 * is that you can remove just ONE value from this set of cells before it
	 * goes bad; so removing one of 3 values from 2 cells leaves 2 values for 2
	 * cells, which is fine, but if you remove any second value then youre left
	 * with just 1 value for 2 cells, which is (pretty obviously) invalid.
	 * <p>
	 * Each ALS can be related to 0..many other ALSs. In AlsChain relationships
	 * between ALSs are bidirectional, that is each relationship is listed in
	 * the rccs-array twice:
	 * <pre>
	 *    A-&gt;B, and also as
	 *    B-&gt;A
	 * </pre>
	 * Listing each relationship twice simplifies both AlsFinder and AlsChains
	 * recursive dependency tree search algorithm, which is already "nasty".
	 * <p>
	 * NOTE: AlsChains recursion is bounded, so it never goes too deep, ie it
	 * never goes near the realms of stack-overflow, even with recurse and
	 * process calling each other. The boundary is Tech.AlsChain.degree = 26,
	 * calculated by trial-and-error to find all AlsChains in top1465, but note
	 * that I cannot prove this is the same for other puzzles, so we MAY miss
	 * some (long) AlsChains in other puzzles. I suspect a long-tail has been
	 * docked, for efficiency.
	 * <p>
	 * <b>NOTE</b>: Theres a WTF in Shft-F5 find more hints, so recurse
	 * refilters RCCs, and handles multiple usableCands.
	 *
	 * @param relatedRccIndexes the indexes of RCCs related to the previous ALS
	 *  in the chain (n-1)
	 * @param prevCand a bitset of the value used to form the previous link,
	 *  which is excluded from candidates available to form the current link
	 * @param m = n - 1
	 * @param n the number of ALSs in chainAlss; you call me with 2, and I find
	 *  the third-onwards als recursively, exploring all possible combinations
	 *  of alss starting with the chain as it was passed to me, ie asat n-1
	 * @param o = n + 1
	 * @return any hint/s found
	 */
	private boolean recurse(final int[] relatedRccIndexes, final int prevCand
			, final int m, final int n, final int o) {
//++COUNTS[0]; // 17,004,487
		assert relatedRccIndexes != NONE;
		Rcc rcc;
		Als als;
		Idx a, b, vBuds[];
		int usableCands, j, alsIndex, usedCand
		, kids[] // relatedRcc indexes
		, zs, za[], zn, zi, z // bitset, array, size, index, value
		;
		long o0; int o1; // overlap, hijacked as victims
		Pots reds = null; // eliminations
		boolean result = false // any hint/s found
		, any = false // any elims found
		, ok = true; // keep on truckin
		// foreach RCC that is related to the current ALS (rcc.source)
		for ( int i : relatedRccIndexes ) {
//++COUNTS[1]; // 84,843,082
			// if there is a usableCand
			// Shft-F5 ____s kids filter, so I MUST ignore them myself.
			rcc = RCCS[i];
			usableCands = rcc.cands & ~prevCand;
			if ( usableCands > 0 ) {
//++COUNTS[2]; // 84,843,082
				// get the idx of this als
				a = ALSS[rcc.related].idx;
				// No tangles: if consecutive ALSs in chain contain each other
				// then it breaks the ALS "push" logic.
				// if all cells in the chain so far contains this ALS then it
				// just might form a tangle, else it can't and this is faster.
				b = chainSoFar[m]; // all cells in the chain so far
				if ( (b.m0 & a.m0) == a.m0
				  && (b.m1 & a.m1) == a.m1
				) {
//++COUNTS[3]; // 31,998,065
					// foreach als in the chain (first is smallest)
					j = 0;
					do {
//++COUNTS[4]; // 148,087,670
						// if any cell/s common to both alss
						// for speed: faster to intersects-test every als, coz
						// ~23% of pairs intersect, then do two contains-tests
						// on the ~23%. Marginally less work overall.
						b = chainIdxs[j];
						if ( ( (o0=b.m0 & a.m0)
							 | (o1=b.m1 & a.m1) ) > 0
						) {
//++COUNTS[5]; // 65,112,300
							// if this als contains chain als
							// speed: alss is ordered by size increasing, hence
							// this als is same-or-larger than chain als. What
							// I really want to do is test the previous als 'm'
							// coz I know its related, then test 0..m-1.
							// I want to watch the machine working.
							if ( (o0==b.m0 && o1==b.m1)
							  // or chain als contains this als (rare)
							  || (o0==a.m0 && o1==a.m1)
							) {
//++COUNTS[6]; // 22,572,603
								ok = false; // tangle
								break;
							}
						}
					} while (++j < n);
				}
				// reset for next time
				if ( !ok ) { // this als made a tangle out of the chain
//++COUNTS[7]; // 22,572,603
					ok = true;
				// else this als does not make a tangle out of the chain
				// if there is just one usable cand (as per normal)
				} else if ( VSIZE[usableCands] == 1 ) {
//++COUNTS[8]; // 62,270,479
					// Process the related als (a "child" of this als)
					usedCand = chainCands[n] = usableCands;
					alsIndex = rcc.related;
					als = ALSS[alsIndex];
					// 1. Search this "leaf" for an AlsChain.
					// if there is atleast 4 ALSs in the chain
					if ( n > 2 // nb: n is a 0-based index, so >=3 is 4 ALSs in the chain
					  // and this leaf has not already been dudded
					  // HACK: this wrongly averts subsequent usedCands, which costs a few
					  // hints, but saves big time (there are exponential(degree) possible
					  // paths to each leaf) so I do it anyway. The correct bloody answer
					  // DUD_LEAF[alsIndex][usedCand] is too big, so you'd use long-bitsets
					  // like DUD_BRANCH, which is slower, as well as being slower.
					  && !dudLeaf[alsIndex]
					) {
//++COUNTS[9]; // 14,715,303
						// bitset of all z values (als.maybes -> next als in chain)
						// if any zs: cands common to first ALS and last ALS except used
						// cands (the v that links second ALS back-to first, and the v that
						// links last ALS back to penultimte ALS in chain).
						if ( (zs=als.maybes & ~usedCand & firstUsableCands) > 0 ) {
//++COUNTS[10]; // 12,482,866
							// eliminate external zs seeing all zs in first & last ALS
							vBuds = als.vBuds;
							zn = VSIZE[zs];
							za = VALUESES[zs];
							zi = 0;
							do {
//++COUNTS[11]; // 23,058,045
								z = za[zi];
								// victims see all zs in first and last ALS
								// nb: overlap (o* vars) hijacked as victims
								if ( ( (o0=firstVBuds[z].m0 & vBuds[z].m0)
									 | (o1=firstVBuds[z].m1 & vBuds[z].m1) ) > 0
								) {
//++COUNTS[12]; // 1,738
									if(reds==null) reds = new Pots();
									any |= reds.upsertAll(o0,o1, maybes, VSHFT[z], DUMMY);
								}
							} while(++zi < zn);
							if ( any ) {
//++COUNTS[13]; // 1,611
								// add the last als to the chain
								chainAlss[n] = als;
								// create a hint
								final AHint hint = new AlsChainHint(
										  grid, this, reds
										, copyOf(chainAlss, o)
										, chainValues(o));
								// clean-up for next time
								reds = null;
								any = false;
								result = true;
								// add hint to accu
								accu.add(hint);
								// half a dozen hints is enough
								if ( accu.size() > 5 )
									// exit the whole recursive callstack
									oneHintOnly = true;
								// if accu.isSingle we are done here
								if(oneHintOnly) return result;
							}
						}
						if ( !any ) {
//++COUNTS[14]; // 14,713,692
							// remember that this als does not hint
							// nb: averts ALL usedCand, costs few hints, saves big time.
							// Theres an exponential(degree) number of paths to each leaf.
							dudLeaf[alsIndex] = true;
						}
					}
					// 2. "branch" to add the next (n+1) ALS
					// if the chain is not already full
					// nb: n is 0-based, degree is 1-based, so n<degreeMinus1 is spot-on.
					if ( n<degreeMinus1 && anyRelated[alsIndex]
//&&++COUNTS[15]>0 // 59,028,500
					  // get all rcc.related alss for this als source which have cands
					  // OTHER THAN the current usedCand, so that the next als has a cand
					  // available to link to the next als in the chain, or eliminate on.
					  // if any related RCCs with candidates OTHER THAN usedCand.
					  // EMPTY means none OR this branch is already known to not hint.
					  && (kids=kids(alsIndex, usedCand)) != NONE ) {
//++COUNTS[16]; // 14,298,549
						// add the current ALS to chainAlss, and to the chain so far
						// current = previous | als
						chainAlss[n] = als;
						chainIdxs[n] = als.idx;
						chainSoFar[n].m0 = chainSoFar[m].m0 | als.idx.m0;
						chainSoFar[n].m1 = chainSoFar[m].m1 | als.idx.m1;
						// recurse the next (n+1) rcc/als
						if ( recurse(kids, usedCand, n, o, o+1) ) {
//++COUNTS[17]; // 6,751
							result = true;
							if(oneHintOnly) return result;
						} else {
//++COUNTS[18]; 14,291,798
							// avert repeated searches of this branch
							final int x = alsBase[alsIndex] + IFIRST[usedCand];
							dudBranch[x/64] |= (1L<<(x%64));
						}
					}
				} else {
//++COUNTS[19]; // 0
					// Shft-F5: handle multiple usableCands
					// injecting Shft-F5 into the batch is a capital offence
					for ( int uc : VSHIFTED[usableCands] ) { // usedCand2
						// process the related als (a "child" of this als)
						if ( process(rcc.related, chainCands[n]=uc, m, n, o) ) {
							result = true;
							if(oneHintOnly) return result;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Process the ALS at alsIndex, which is linked to the previous ALS in the
	 * chain by usedCand; where n is the current length of the chain.
	 * <p>
	 * Note that recurse deals with the RCCs and process deals with the ALSs.
	 * Process is used ONLY when there are multiple usableCands, ie in a tiny
	 * percentage (less than 1%) of cases. The normal single usableCand stays
	 * in recurse, which calls itself.
	 * <p>
	 * <pre>
	 * "Processing" an ALS happens in two stages.
	 *
	 * (1) Search this "leaf" for an AlsChain. An ALS is a "leaf" when its the
	 *     fourth-or-later ALS in da chain (recall 2 is ALS_XZ, 3 is ALS_Wing).
	 *     An AlsChain occurs when a usableCand (called the z-values) is common
	 *     to both this ALS and the first ALS, and all occurrences of that
	 *     value in both ALSs "see" each other (same box, row, or col). See the
	 *     Rcc class-comments for the full restrictions.
	 *
	 *     The z-values are all values common to thisAls and firstAls, EXCEPT
	 *     the two "used" candidate values:
	 *     (a) the value used to link the second ALS in the chain back-to the
	 *         first ALS in the chain; and also
	 *     (b) the value used to link this ALS back-to the previous ALS in the
	 *         chain.
	 *     These values are excluded because the value of each subsequent link
	 *     must be different to the previous link value in order to support
	 *     AlsChains "push" logic. You can remove just ONE value from an ALS,
	 *     hence (when all cells in two ALSs see each other) that value will
	 *     occur ONCE in both of these ALSs, so a DIFFERENT value is "pushed"
	 *     into the next ALS in the chain, and so on along the chain IN EITHER
	 *     DIRECTION. Note well that one of these two states MUST be true, so
	 *     we can chain ALSs together without knowing which value is "pushed"
	 *     forward by each link in the chain.
	 *
	 *     In this implementation the usableFirstCands variable is pre-computed
	 *     to contain a bitset of first ALSs candidate values minus the value
	 *     used to link the second ALS back-to the first ALS, because this
	 *     value changes foreach second ALS, ie far less often.
	 *
	 *     The "end" of the chain occurs when we find eliminations. Each victim
	 *     maybe this z-value, and sees all occurences of this z-value in both
	 *     the first and the last ALS, and are NOT in either the first or the
	 *     last ALS, ie "common buddies" that maybe z. Sigh.
	 *
	 * (2) If the chain is not already full, search each ALS thats linked back
	 *     to this ALS (my kids), and call recurse to explore all chains that
	 *     includes each of my kids. The links between ALSs are stored in RCCs:
	 *     Restricted Common Candidate/s (a link between two ALSs). An RCC is
	 *     one (and occassionally two, about 5%) values that are common to two
	 *     ALSs, that all see each other (same box, row, or col).
	 *
	 *     In this implementation the RCCs are cached, for speed of REPEATED
	 *     lookup. Most RCCs are "explored" many times, so we skip exploring
	 *     RCCs that are already known, by experience, to NOT hint for the
	 *     current first ALS. This is implemented here as DUD_*. Search DUD.
	 *     A speed HACK (search that) is DUDs are cleared once for each
	 *     firstAls, which is incorrect, but takes about half the time, mainly
	 *     because clearing a large array (or a [][]) in Java is slow compared
	 *     to C/++/# because Java has no System.memset. To do it properly we
	 *     need to reset DUD_* when the first usedCand changes, ie whenever the
	 *     second ALS changes (or usedCand changes when rcc.v2 != 0).
	 * </pre>
	 *
	 * @param alsIndex the index in alss of the Almost Locked Set to process
	 * @param usedCand the cand thats currently used (one of the usableCands);
	 *  ie a bitset of the RC-value linking the current ALS to the previous ALS
	 *  in the chain. This RC-value is excluded from the z-values linking this
	 *  ALS back-to the firstAls, and is unavailable as the next link value so
	 *  that each link value is distinct, so that the chain of ALSs all fall
	 *  down like dominoes, in either direction, forming a possible AlsChain
	 * @param m n - 1
	 * @param n the number of ALSs already in chainAlss when you call me
	 * @param o n + 1
	 * @return any hint/s found?
	 */
	private boolean process(final int alsIndex, final int usedCand, final int m
			, final int n, final int o) {
		// presume no hints will be found. It is a reasonable assumption. Sigh.
		boolean result = false;
		// 1. Search this "leaf" for an AlsChain.
		// if there is atleast 4 ALSs in the chain
		final Als als = ALSS[alsIndex];
		if ( n > 2 // nb: n is a 0-based index, so >=3 is 4 ALSs in the chain
		  // and this leaf has not already been dudded
		  // HACK: this wrongly averts subsequent usedCands, which costs a few
		  // hints, but saves big time (there are exponential(degree) possible
		  // paths to each leaf) so I do it anyway. The correct bloody answer
		  // DUD_LEAF[alsIndex][usedCand] is too big, so you'd use long-bitsets
		  // like DUD_BRANCH, which is also slower, as well as being slower.
		  && !dudLeaf[alsIndex]
		) {
			// bitset of all z values (als.maybes -> next als in chain)
			final int zs = als.maybes & ~usedCand & firstUsableCands;
			// if any zs: cands common to first ALS and last ALS except used
			// cands (the v that links second ALS back-to first, and the v that
			// links last ALS back to penultimate ALS in chain).
			if ( zs > 0 ) { // 9bits (NEVER negative)
				// eliminate external zs seeing all zs in first & last ALS
				Pots reds = null;
				final Idx victims = new Idx();
				for ( int z : VALUESES[zs] ) {
					// victims see all zs in first and last ALS
					if ( victims.setAndAny(firstVBuds[z], als.vBuds[z]) ) {
					   if(reds==null) reds = new Pots();
					   result |= reds.upsertAll(victims, maybes, VSHFT[z], DUMMY);
					}
				}
				if ( result ) {
					// create a hint and add it to accu
					// we delay adding the last als to the chain until we hint
					chainAlss[n] = als;
					accu.add(new AlsChainHint(grid, this, reds
							, copyOf(chainAlss, o), chainValues(o)));
					if ( accu.size() > 5 )
						oneHintOnly = true; // make whole callstack exit early
					if ( oneHintOnly )
						return result;
				}
			}
			if ( !result ) {
				// remember that this als does not hint
				// nb: averts ALL usedCand, costs few hints, saves big time.
				// Theres an exponential(degree) number of paths to each leaf.
				dudLeaf[alsIndex] = true;
			}
		}
		// 2. "branch" to add the next (n+1) ALS
		// if the chain is not already full
		// nb: n is 0-based, degree is 1-based, so n<degreeMinus1 is spot-on.
		if ( n<degreeMinus1 && anyRelated[alsIndex] ) {
			// get all rcc.related alss for this als source which have cands
			// OTHER THAN the current usedCand, so that the next als has a cand
			// available to link to the next als in the chain, or eliminate on.
			final int[] kids = kids(alsIndex, usedCand);
			// if any related RCCs with candidates OTHER THAN usedCand.
			// EMPTY means none OR this branch is already known to not hint.
			if ( kids != NONE ) {
				// add the current ALS to chainAlss
				chainAlss[n] = als;
				// chain so far: current = previous | als
				chainSoFar[n].setOr(chainSoFar[m], chainIdxs[n]=als.idx);
				// recurse the next (n+1) rcc/als
				if ( recurse(kids, usedCand, n, o, o+1) ){
					result = true;
				} else {
					// remember branch does not hint, to avoid repeat search
					final int x = alsBase[alsIndex] + IFIRST[usedCand];
					dudBranch[x/64] |= (1L<<(x%64));
				}
			}
		}
		return result;
	}

//	/**
//	 * Returns does this ALS turn the chain into a "tangle". An ALS that wholey
//	 * contains another ALS in the chain breaks AlsChains "push" logic, so we
//	 * avert adding any ALS that wholey contains any other ALS that is already
//	 * in the chain, and vice-versa (if a chained-ALS contains this ALS then
//	 * skip this ALS).
//	 *
//	 * @param alsIdx cells in the current als to test
//	 * @param m last valid index in the chain (ie n - 1)
//	 * @return is als NOT ok? ie does it make an endless-loop out of the chain.
//	 */
//	private boolean isLoop(final Idx alsIdx, final int m) {
//		// if this als is wholey in the chainSoFar it might form a tangle.
//		if ( chainSoFar[m].hasAll(alsIdx) )
//			// foreach als in the chain, last to first, coz last is related
//			for ( int i=m; i>-1; --i )
//				// if either als contains the other, then its a tangle
//				if ( alsIdx.tangle(chainIdxs[i]) )
//					return true;
//		return false;
//	}

	private int[] chainValues(int size) {
		final int[] result = new int[size];
		for ( int i=0; i<size; ++i ) {
			result[i] = VFIRST[chainCands[i]];
		}
		return result;
	}

	// Disable AlsChain for this puzzle only (I will get you next time Batman)
	private boolean timeout(final int alsIndex, final int numAlss) {
		setIsEnabled(false);
		Log.format("TIMEOUT: %s DISABLED! %d ms exceeded in %s at i=%d/%d\n", tech.name(), TIMEOUT, grid.hintSource(), alsIndex, numAlss);
		return true;
	}

}
