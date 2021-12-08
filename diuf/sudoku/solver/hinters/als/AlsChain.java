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
import static diuf.sudoku.Grid.REGION_SIZE;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.IFIRST;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
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
import static java.lang.System.arraycopy;
import java.util.Arrays;

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
 * Note that AlsChain can't pull off AlsXz's double-linked stunt.
 * </pre>
 * <pre>
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
 * KRC 2021-11-04 Dropped fastardised version coz it saves only about a second,
 * and therefore just isn't worth the fuss.
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
public final class AlsChain extends AAlsHinter
{
//	@Override
//	public void report() {
//		Log.teeln(Log.me()+": "+Arrays.toString(COUNTS));
//	}
//	private static final long[] COUNTS = new long[7];
//	// 1#top1465.d5.mt exponent 46,969,528,320,000 = 5*11*13*1*5*8*9*10*4*2*9*11*9*8*10*8*4

	// FASTARDISATION: TIMEOUT is how long findHints runs before it gives-up.
	private static final int TIMEOUT = 1000; // milliseconds

	// FASTARDISATION: RCC_CACHE[alsIndex][prevCand] contains rcc-indexes for
	// alsIndex having cands other than prevCand. It makes each SUBSEQUENT
	// request for each [alsIndex][prevCand] faster.
	private static final int[][][] RCC_CACHE = new int[MAX_ALSS][VALUE_CEILING][];

	// FASTARDISATION: RCC_CACHE[*][0] is otherwise unused, so it's ALL array.
	private static final int ALL = 0;

	// FASTARDISATION: there is one only empty rcc-indexes-array
	private static final int[] EMPTY = new int[0];

	// FASTARDISATION: the .cands of each rccs (coz that's all I need)
	private static final int[] RCC_CANDS = new int[MAX_RCCS];

	// FASTARDISATION: does this alsIndex have any related Rcc's? Just 12.79%
	// of top1465s ALSs are viable branches (rest may still be leafs), so the
	// other 87.21% waste my time. This is a fast way to ignore them.
	private static final boolean[] ANY_RELATED_RCCS = new boolean[MAX_ALSS];

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
	private static final boolean[] DUD_BRANCH = new boolean[MAX_ALSS*REGION_SIZE];

	// FASTARDISED FASTARDISATION: array-lookup is faster than multiplication,
	// so do all multiplications ONCE; all because java lacks a System.memset.
	// I'm really scraping the bottom of the jar here, looking for the last 5%.
	private static final int[] ALS_BASE = new int[MAX_ALSS];
	static {
		for ( int i=1; i<MAX_ALSS; ++i )
			ALS_BASE[i] = i * REGION_SIZE;
	}

	// FASTARDISED FASTARDISATION: System.arraycopy is faster than Arrays.fill,
	// especially for very large arrays. Just pissed coz System.memset is MIA.
	private static final boolean[] DUD_DONOR = new boolean[MAX_ALSS*REGION_SIZE];

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
	// the first ALS is chain[0].als
	private Als firstAls;
	// bitset of candidates available as z-values: firstMaybes & ~firstCand
	private int firstUsableCands;
	// --------------------------------------------------

	public AlsChain() {
		super(Tech.ALS_Chain, true, true, true, false);
		chainAlss = new Als[degree]; // currently 26 (longest according to GUI)
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
			// set-up the rcc-indexes cache
			rccCacheSetup(rccs, numRccs);
			search(numAlss);
			if ( accu.size() > 1 ) // batch NEVER
				accu.sort(null);
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
			// clean-up rcc-indexes-cache
			rccsStart = null;
			rccsEnd = null;
		}
	}

	/**
	 * Search the grid for AlsChains. I add the first two ALSs to the chain,
	 * then I call recurse to add the third ALS onwards.
	 */
	private void search(final int numAlss) {
		Rcc rcc; // the current Restricted Common Candidate
		Als secondAls; // the second Als is chainAlss[1]
		int[] kids; // indexes of rccs that're related to this ALS
		int i1,i2 // alss-index of first and second als
		  , j,lastJ // rccs-index and rccs-index-ceiling
		  , firstMaybes // the maybes of the firstAls
		  , i; // the ubiqitious index
		// presume that no hints will be found
		boolean any = false;
		// for the timeout mechanism
		final long stop = System.currentTimeMillis() + TIMEOUT;
		// set-up ANY_RELATED_RCCS
		for ( i=0; i<numAlss; ++i )
			ANY_RELATED_RCCS[i] = rccsEnd[i] - rccsStart[i] > 0;
		// foreach ALS, which I use ONLY to look-up the-rccs-for-this-als
		for ( i1=0; i1<numAlss; ++i1 ) {
			// foreach firstRcc in rccs-related-to-the-firstAls
			if ( ANY_RELATED_RCCS[i1] ) {
				firstAls = chainAlss[0] = alss[i1];
				firstMaybes = firstAls.maybes;
				chainSoFar[0] = firstAls.idx;
// HACK: half time for 10 less elims: reset DUD_BRANCH per firstAls ONLY.
				arraycopy(DUD_DONOR, 0, DUD_BRANCH, 0, DUD_DONOR.length);
				for ( j=rccsStart[i1],lastJ=rccsEnd[i1]-1; ; ) {
					// rcc.related is the index of our second ALS
					if ( ANY_RELATED_RCCS[rccs[j].related] ) {
						i2 = (rcc=rccs[j]).related;
						// both of the first-two ALSs come from the firstRcc, but
						// only the second ALS "uses" a candidate (first doesn't)
						chainRccIndexes[0] = chainRccIndexes[1] = j; // for debugging only
						chainRccs[0] = chainRccs[1] = rcc;
						// chainAlss[1] is the second ALS
						secondAls = chainAlss[1] = alss[i2];
						// chainSoFar[1] is the aggregate of both ALSs so far
						chainSoFar[1].setOr(chainSoFar[0], secondAls.idx);
						// foreach cand (usually one, just ~5% have two)
						for ( int usedCand : VSHIFTED[rcc.cands] ) {
							if ( (kids=rccCache(i2, usedCand)) != EMPTY ) {
								// reset DUD_LEAF for firstAls/firstCand
								Arrays.fill(DUD_LEAF, false);
// CORRECT: double time for 10 more elims.
//								// reset DUD_BRANCH per firstAls/firstCand
//								arraycopy(DUD_DONOR, 0, DUD_BRANCH, 0, DUD_DONOR.length);
								chainCands[1] = usedCand;
								firstUsableCands = firstMaybes & ~usedCand;
								if ( (any|=recurse(kids, usedCand, 2)) && oneOnly )
									return;
							}
						}
					}
					if ( System.currentTimeMillis() > stop ) {
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
	private boolean recurse(final int[] relatedRccIndexes, final int prevCand
			, final int n) {
		assert relatedRccIndexes != EMPTY;
		Rcc rcc; // the current Rcc we're processing
		final int m = n - 1;
		boolean result = false;
		for ( int i : relatedRccIndexes ) {
			rcc = rccs[i];
			if ( noLoop(alss[rcc.related].idx, m)
			  // rccCache fails to do this in Shft-F5, so I must ignore them.
			  && (rcc.cands & ~prevCand) != 0
			) {
				chainRccs[n] = rcc;
				chainRccIndexes[n] = i; // for debugging only
				// handle multiple usableCands, one-at-a-time
				for ( int usedCand : VSHIFTED[rcc.cands & ~prevCand] )
					if ( (result|=process(rcc.related, chainCands[n]=usedCand, n))
					  && oneOnly )
						return result;
			}
		}
		return result;
	}

	/**
	 * Does 'als' NOT make a loop out of the chain? Ergo is als ok?
	 *
	 * @param alsIdx to test
	 * @param m last valid index in the chain (ie n - 1)
	 * @return does 'als' NOT make a loop out-of the existing chain alss?
	 */
	private boolean noLoop(final Idx alsIdx, final int m) {
		// if the previous chainSoFar containsAll als cells
		if ( chainSoFar[m].hasAll(alsIdx) )
			// previous ALS always related, so most likely to form a loop
			for ( int i=m; i>-1; --i )
				if ( chainAlss[i].idx.inbread(alsIdx) )
					return false;
		return true;
	}

	/**
	 * Process this Als, only called in the multi-RC-value loop, ie rarely.
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
		// presume no hints will be found. It's a good assumption. sigh.
		boolean result = false;
		// if there's atleast 4 ALSs in the chain
		if ( n > 2 // nb: n is a 0-based index, so >=3 is 4 in the chain
		  // and this Als is NOT already known to not hint against the current
		  // firstAls/firstCand, to save repeatedly searching multiple z-values
		  // when the same als is found by another possible path through the
		  // intermediate ALSs. This is a bit faster.
		  // HACK: clear DUD_LEAF only when firstAls changes. Correctness needs
		  // a reset whenever firstAls or firstCand changes, but firstCand may
		  // change thousands-of-times more often, so reset per firstAls ONLY
		  // takes half the time and finds just 10 less elims, a bargain, so da
		  // hack is in: not perfect, just fast, and still useful.
		  && !DUD_LEAF[alsIndex]
		) {
			final Als als = alss[alsIndex];
			final int zs = firstUsableCands & als.maybes & ~usedCand;
			if ( zs!=0 && firstAls.buds.intersects(als.buds) ) {
				for ( int z : VALUESES[zs] )
					if ( victims.setAndAny(firstAls.vBuds[z], als.vBuds[z]) )
						result |= reds.upsertAll(victims, grid, z);
				if ( result )
					result = hint(als, n);
			}
			// remember that als is NOT an AlsChain with firstAls/firstCand
			if ( !result )
				DUD_LEAF[alsIndex] = true;
		}
		// if the chain isn't full
		if ( n < last
		  // and there are any related Rccs
		  && ANY_RELATED_RCCS[alsIndex] ) {
			// and als has related Rccs with candidates OTHER THAN usedCand
			final int[] kids = rccCache(alsIndex, usedCand);
			if ( kids != EMPTY ) {
				// then recurse to add the next (n+1) als
				chainSoFar[n].setOr(chainSoFar[n-1], (chainAlss[n]=alss[alsIndex]).idx);
				// remember branch doesn't hint, to avoid searching it again in
				// this firstAls/firstCand. This stops workload growing expone-
				// ntially, which is much faster, especially for longer chains.
				// Prior it was too slow with 7 alss in a chain, now it's fast
				// enough for hobiwans max 32 alss. This may be a "meh" to you,
				// but not to me. I'm proud of working it out for myself.
				if ( !(result |= recurse(kids, usedCand, n+1)) )
					// * slow, but faster than resetting an array-of-arrays.
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
	private boolean hint(final Als als, final int n) {
		assert !reds.isEmpty();
		// adding the last als to the chain is delayed until we hint!
		chainAlss[n] = als;
		chainSoFar[n].setOr(chainSoFar[n-1], als.idx);
		final int[] rcis = AlsChainDebug.HINTS ? chainRccIndexes(n+1) : null;
		final AlsChainHint hint = new AlsChainHint(this, reds.copyAndClear()
			, chainAlss(n+1), chainValues(n+1), chainRccs(n+1), rcis, "");
		if ( ALS_VALIDATES && !isValid(grid, hint.reds) && !handle(hint) )
			return false; // without adding it!
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

	// Disable the bastard
	private void timeout(final int alsIndex, final int numAlss) {
		setIsEnabled(false);
		Log.format("TIMEOUT: %s DISABLED! %d ms exceeded in %s at i=%d/%d\n"
				, tech.name(), TIMEOUT, grid.place(), alsIndex, numAlss);
	}

	// Setup the rccs-cache
	private static void rccCacheSetup(final Rcc[] rccs, final int numRccs) {
		for ( int i=0; i<numRccs; ++i )
			RCC_CANDS[i] = rccs[i].cands;
		for ( int i=0; i<MAX_ALSS; ++i )
			Arrays.fill(RCC_CACHE[i], null);
	}

	/**
	 * rccCache is called by process and recurseFast to fetch indexes of rccs
	 * that are related to the given ai (alsIndex) with cands OTHER THAN pc
	 * (prevCand).
	 * <p>
	 * rccCache is faster, but FMS its complicated, which I must apologise for.
	 * It turned into War and Peace while I wasn't looking. I tried to KISS it,
	 * but simplicity is at odds with speed, which we need, coz it's hammered!
	 * <p>
	 * PRE: cacheEnds[ai]-cacheStarts[ai] > 0; or don't bother calling me.
	 *
	 * @param ai alsIndex: the index of the source ALS in the alss array
	 * @param pc prevCand: we need rccs with cands other than pc, so each link
	 *  in the chain has a different value, so that these ALSs fall over like
	 *  dominoes, in either direction
	 * @return indexes of rccs related to ai having cands other than pc
	 */
	private static int[] rccCache(final int ai, final int pc) {
		assert rccsEnd[ai]-rccsStart[ai] > 0;
		assert VSIZE[pc] == 1 : "prevCand="+pc;
		if ( DUD_BRANCH[ALS_BASE[ai]+IFIRST[pc]] )
			return EMPTY;
		return rccCacheGet(ai, pc, VFIRST[pc], RCC_CACHE[ai]);
	}

	private static int[] rccCacheGet(final int ai, final int pc, final int pv
			, final int[][] alsCache) {
		if ( alsCache[pv] != null )
			return alsCache[pv];
		return rccCacheSet(ai, pc, pv, alsCache, rccsStart[ai], rccsEnd[ai]);
	}

	private static int[] rccCacheSet(final int ai, final int pc, final int pv
			, final int[][] alsCache, final int start, final int end) {
		int i=start, size=0;
		for ( ; i<end; ++i )
			if ( (RCC_CANDS[i] & ~pc) != 0 )
				size++;
		if ( size == 0 )
			return alsCache[pv] = EMPTY;
		final boolean isAll = size==end-start;
		if ( isAll && alsCache[ALL]!=null )
			return alsCache[pv] = alsCache[ALL];
		final int[] result;
		if ( isAll )
			result = alsCache[ALL] = alsCache[pv] = new int[size];
		else
			result = alsCache[pv] = new int[size];
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
