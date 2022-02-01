/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.Validator.INVALIDITIES;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_DEATH_BLOSSOM;
import static diuf.sudoku.solver.hinters.Validator.VALIDATE_ALS;

/**
 * AAlsHinter is the abstract ALS Hinter. I fetch ALSs and RCCs that are common
 * inputs to techniques: BigWings, AlsXz, AlsWing, AlsChain, and DeathBlossom.
 * Actually, I implement the standard getHints method to get: candidates, ALSs,
 * RCCs, and NakedSets; which I pass to my subtypes custom findHints method,
 * and I return its result.
 * <p>
 * Note that BigWings and DeathBlossom do not fetch RCCs, just ALSs.
 * <pre>
 * KRC 2021-06-01 cache to speed-up ALSWing, ALSChain, and DeathBlossom.
 * Note that all caching is static, ie one copy of the cache and all associated
 * variables are shared by ALL instances of AAlsHinter, so caches are shared
 * among AlsXz, AlsWing, AlsChain, and DeathBlossom. This works well because
 * 1. AlsXz and AlsWing use SAME AAlsHinter constructor params, especially both
 *    allowNakedSets; so that
 * 2. AlsWing doesn't need to fetch it's own ALSs or RCCs; then
 * 3. AlsChain just filters-out cached ALSs containing a cell in a NakedSet.
 *    It doesn't need to fetch it's own ALSs, just filter the existing alsCache
 *    which it replaces with the fitlered version.
 *    It does fetch RCCs because allowOverlaps and forwardOnly have changed;
 * 4. DeathBlossom re-uses the ALSs, and so fetches nothing.
 * Hence caching ALSs and RCCs works-out 16 seconds faster!
 *
 * BEFORE (without caching 2021-05-30.14-11-32)
 * 16,381,530,900  9978 1,641,764  3949   4,148,273 ALS-XZ
 * 22,568,789,600  7231 3,121,115  3330   6,777,414 ALS-Wing
 *  9,835,713,400  4482 2,194,492   602  16,338,394 ALS-Chain
 *  4,141,170,600  3995 1,036,588   163  25,405,954 Death Blossom
 * AFTER (with caching of both ALSs and RCCs 2021-06-01.10-41-26)
 * 15,915,234,859  9940 1,601,130  3961   4,017,984 ALS-XZ
 *  9,077,950,421  7179 1,264,514  3429   2,647,404 ALS-Wing
 *  8,678,898,336  4408 1,968,897   577  15,041,418 ALS-Chain
 *  2,933,325,664  3943   743,932   163  17,995,862 Death Blossom
 * so that's 16.321 seconds faster over top1465, a major improvement!
 * 2021-06-04.14-45-21 latest version with AlsFinder and RccFinder.
 * 13,792,733,500  9867 1,397,864  3962   3,481,255 ALS-XZ
 *  9,188,664,200  7175 1,280,650  3406   2,697,787 ALS-Wing
 *  8,692,515,500  4435 1,959,980   567  15,330,715 ALS-Chain
 *  2,937,086,600  3975   738,889   166  17,693,292 Death Blossom
 * 2021-06-05.10-15-19 with RccFinder split on forwardOnly
 *  3,712,255,500 12254   302,942  2210   1,679,753 Big-Wings     gets ALSs
 * 11,663,152,600  9867 1,182,036  3962   2,943,753 ALS-XZ        gets RCCs
 *  9,528,221,800  7175 1,327,975  3406   2,797,481 ALS-Wing      reuse both
 *  7,189,604,700  4435 1,621,105   567  12,680,078 ALS-Chain     gets both
 *  2,883,650,800  3975   725,446   166  17,371,390 Death Blossom reuse both
 *
 * 2021-12-29 14:19        total        getAlss         getRccs      remainder
 * BigWings    :   3,664,925,100  3,162,836,400               0    502,088,700
 * DeathBlossom:   9,903,029,600              0               0  9,903,029,600
 * ALS_XZ      :  12,533,737,200  2,181,480,400  10,195,763,200    156,493,600
 * ALS_Wing    :   8,825,495,300        741,200         428,600  8,824,325,500
 * ALS_Chain   :  18,605,335,700        521,100  12,358,090,900  6,246,723,700
 * NOTES:
 * * All the ALS and RCC getters are fully optimised.
 * * BigWings is fully optimised.
 * * DeathBlossom is near my best. I tried inline Idx ops, but it was slower.
 * * ALS_XZ time is 98.8% ALS and RCC getters.
 * * ALS_Wing is fully optimised. I know not why it's slower than ALS_Chain.
 *   As far as I'm concerned it can't be, but there's wiggle room here.
 * * ALS_Chain now takes ~twice time for ~threeTimes hints; which is not bad
 *   value, though I still wish it was faster.
 * * so I'm done here, I think.
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
abstract class AAlsHinter extends AHinter
//		implements diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		diuf.sudoku.utils.Log.teeln(tech.name()+": alss="+getAlssTime+" rccs="+getRccsTime);
//	}

	/** the size of the fixed-size alss array: 512 */
	protected static final int MAX_ALSS = 512; // Observed 283

	// 833#top1465.d5.mt=6764
	// KRC 2021-10-09 8k broken, so increased to 9k.
	/** the length of the fixed-size static RCCS array: 9k */
	protected static final int MAX_RCCS = 9*1024;

	// include single bivalue Cells in the ALSs list?
	// nb: 1 cell with 2 potential values is by definition an ALS. I don't know
	// what problems it causes if you set this to true. You have been warned!
	protected static final boolean SINGLE_CELL_ALSS = false;

	// the default rccs List capacity
	protected static final int NUM_RCCS = 2000;

	// DeathBlossom uses this to mark unused params; for self-doc'ing code.
	protected static final boolean UNUSED = false;

	/** ALS_FINDER protected so my subtypes can report its COUNTS. */
	protected static final AlsFinder ALS_FINDER = new AlsFinderPlain();

	// ALSS cache is static to share among all my subclasses,
	// especially AlsXz and AlsWing, which use the same ALSS and RCCS.
	private static final Als[] ALSS = new Als[MAX_ALSS];
	private static int numAlss;
	// these control the alss-cache which effects the rccs-cache
	private static int alssHn; // als-cache hintNumber
	private static long alssPid; // als-cache puzzleID
	private static boolean alssNs; // does alss-cache include Naked Sets

	// RCCS cache is static to share among all my subclasses,
	// especially AlsXz and AlsWing, which use the same ALSS and RCCS.
	private static final Rcc[] RCCS = new Rcc[MAX_RCCS];
	private static int numRccs;
	private static boolean rccsDirty, rccsAO, rccsFO;

	/**
	 * Just produce a hint for each Rcc, add it to the accu, and always return
	 * true; so a developer can see the Rcc's that're being processed by each
	 * subtype of AAlsHinter. Note that you can shft-right-click on the hints
	 * tree-view to copy all hints.toString to the clipboard, and paste it into
	 * a text-file, so it's fast and easy to produce an Rcc's list.
	 * <p>
	 * To use, copy-paste into the top of your subclass/s findHints: {@code
	 * if(true) return justShowTheRccs(alss, numAlss, rccs, numRccs, accu);
	 * }
	 *
	 * @param alss
	 * @param numAlss
	 * @param rccs
	 * @param numRccs
	 * @param accu
	 * @return true
	 */
	protected boolean justShowTheRccs(final Als[] alss, final int numAlss, final Rcc[] rccs, final int numRccs, final IAccumulator accu) {
		for ( int i=0; i<numRccs; ++i ) {
			accu.add(new RccHint(this, rccs[i], new Als[]{alss[rccs[i].source], alss[rccs[i].related]}));
		}
		return true;
	}

	// ----------------------------- instanceland -----------------------------

	/** include ALSs which overlap in my findHints method? */
	protected final boolean allowOverlaps;
	/** include ALSs which include cells that are part of a Naked Set? */
	protected final boolean allowNakedSets;
	/** should I run getRccs; false means I pass you rccs=null in findHints */
	protected final boolean findRccs;
	/** should getRccs do a fast forwardOnly search, or a full search? */
	protected final boolean forwardOnly;

	/** RCC_FINDER finds the Restricted Common Candidates between ALS's */
	protected final RccFinder rccFinder;

	// the actual constructor, which is wrapped for public presentation.
	// Basically, you workout which rccFinder you want and then call me.
	private AAlsHinter(final Tech tech, final boolean allowNakedSets
			, final boolean findRccs, final RccFinder rccFinder
			, final boolean allowOverlaps, final boolean forwardOnly) {
		super(tech);
		// getAlss params
		this.allowNakedSets = allowNakedSets;
		// getRccs params
		this.findRccs = findRccs;
		this.rccFinder = findRccs ? rccFinder : null;
		this.allowOverlaps = findRccs ? allowOverlaps : UNUSED;
		this.forwardOnly = findRccs ? forwardOnly : UNUSED;
	}

	/**
	 * Construct a new abstract Almost Locked Set (ALS) hinter, whose findHints
	 * implementation finds the ALSs and optionally there RCCs, and passes the
	 * whole mess down to my subclass's "custom" findHints method.
	 * <pre>
	 * Uses in order of execution           , nkd  , RCCs, olap , frwd , s&amp;e
	 * BigWings    : super(Tech.BigWings    , true);
	 * AlsXz       : super(Tech.ALS_XZ      , true , true, true , true , false);
	 * AlsWing     : super(Tech.ALS_Wing    , true , true, true , true , false);
	 * AlsChain    : super(Tech.ALS_Chain   , false, true, false, false, true);
	 * DeathBlossom: super(Tech.DeathBlossom, false);
	 * </pre>
	 * @param tech the Tech(nique) that we're implementing: Als*, DeathBlossom
	 *  is passed up to my super-class, a "normal" AHinter.
	 * @param allowNakedSets if false the getAlss method ignores cells that are
	 *  part of an actual NakedPair/Triple/etc in this region.<br>
	 *  HdkAlsXz and HdkAlsXyWing both use true<br>
	 *  HdkAlsXyChain = false because naked sets stuff it all up, and I don't
	 *  understand why!
	 * @param findRccs true to fetch RCCs, false to just fetch the ALSs.<br>
	 *  When false AAlsHinter does NOT run getRccs, so findHints rccs=null, and
	 *  the following parameters (effecting getRccs) have no effect, so there's
	 *  an UNUSED constant for use as a self-documenting parameter-value.
	 * @param allowOverlaps if false the getRccs method ignores two ALSs which
	 *  physically overlap (ie have any cell in common).<br>
	 *  HdkAlsXz and HdkAlsXyWing both use true<br>
	 *  HdkAlsXyChain = false because overlaps break the one-or-the-other rule
	 * @param forwardOnly if true then getRccs does a faster "forward only"
	 *  search that examines each distinct combination of ALSs once;<br>
	 *  else examine all possible combinations, including duplicates.<br>
	 *  HdkAlsXz and HdkAlsXyWing both use true<br>
	 *  HdkAlsXyChain = false to find more chains. It's slow anyway!
	 */
	public AAlsHinter(Tech tech, boolean allowNakedSets, boolean findRccs
			, boolean allowOverlaps, boolean forwardOnly) {
		this(tech, allowNakedSets, findRccs
		, findRccs ? RccFinderFactory.get(forwardOnly, allowOverlaps) : null
		, allowOverlaps, forwardOnly);
//System.out.println("DEBUG: "+tech+" "+rccFinder);
//DEBUG: ALS_XZ diuf.sudoku.solver.hinters.als.RccFinderForwardOnlyAllowOverlaps@56017d3c
//DEBUG: ALS_Wing diuf.sudoku.solver.hinters.als.RccFinderForwardOnlyAllowOverlaps@56017d3c
//DEBUG: ALS_Chain diuf.sudoku.solver.hinters.als.RccFinderAllAllowOverlaps@10b90bfd
	}

	/**
	 * A simplified constructor for BigWings and DeathBlossom,
	 * which fetch ALSs only, not RCCs.
	 */
	public AAlsHinter(Tech tech, boolean allowNakedSets) {
		this(tech, allowNakedSets, false, null, UNUSED, UNUSED);
	}

	/**
	 * AAlsHinter actually implements IPrepare, but leaves the declaration
	 * of {@code implements diuf.sudoku.solver.hinters.IReporter} up to each
	 * of my subclasses, so that you can decide weather or not you can be
	 * disabled, which is always nice.
	 * <p>
	 * The prepare method is called after the LogicalSolver is configured,
	 * but before we attempt to solve each Grid. It gives the LogicalSolver
	 * and each of it's hinters a chance to prepare to solve this new Grid;
	 * as distinct from getHints on probably-the-existing-grid. Note that
	 * Grid now has an id&nbsp;field to identify the puzzle that's loaded
	 * into this grid.
	 * <p>
	 * This instance re-enables this hinter, in case it has been disabled,
	 * and clears Validator.INVALIDITIES if the Validator is used.
	 *
	 * @param grid
	 */
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
		// if we're eradicating bugs from Als* or DeathBlossom hints
		if ( VALIDATE_ALS || VALIDATE_DEATH_BLOSSOM ) {
			// clear out-dated invalidities at start of a new Grid.
			INVALIDITIES.clear();
		}
	}

	/**
	 * findAlsHints is implemented by my subclasses to do the hint search.
	 * I just pass him everything I know, whether he uses it or not. But if
	 * nobody uses something (like grid.idxs for example) then it might be
	 * an idea to not calculate it in the first place. Sigh.
	 *
	 * @param grid The Grid that we're solving
	 * @param alss a fixed-array of the Almost Locked Sets
	 * @param numAlss the number of ALSs in the alss array
	 * @param rccs a fixed-array of the Restricted Common Candidates
	 * @param numRccs the number of RCCs in the rccs array
	 * @param accu The IAccumulator to which we add any hints
	 * @return true if a hint was found, else false
	 */
	protected abstract boolean findAlsHints(final Grid grid
			, final Als[] alss, final int numAlss
			, final Rcc[] rccs, final int numRccs
			, final IAccumulator accu);

	/**
	 * Find Almost Locked Set hints in Grid and add them to IAccumulator.
	 * <p>
	 * @param grid the grid to search for hints
	 * @param accu the chosen implementation of IAccumulator
	 * @return true if any hint/s were found. Different implementations of
	 *  IAccumulator find one or all hints
	 */
	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// LogicalSolver.solve doesn't disable hinters on da fly, only at start
		if ( !isEnabled )
			return false;
		// get the Almost Locked Sets (N cells with N+1 values between them)
//		start = System.nanoTime();
		getAlss(grid); // repopulates alss, sets numAlss
//		tookAlss += System.nanoTime() - start;

		// if findRccs then get the Restricted Common Candidates of those ALSs
		if ( findRccs )
//			start = System.nanoTime();
			// I've done my best to enspeedonate, but getRccs still TOO SLOW!
			getRccs(); // repopulates rccs, sets numRccs
//			tookRccs += System.nanoTime() - start;
		else
			numRccs = 0;
		// call each of my subclasses findAlsHints method to search the grid
		// for this specific als-pattern, raise hints, and add them to accu.
		return findAlsHints(grid, ALSS, numAlss, RCCS, numRccs, accu);
	}
//	/** reported in {@link AlsChain} */
//	protected long tookAlss=0L, tookRccs=0L;
//	private long start;

	/**
	 * getAlss does the recursive ALS search at each cell in each region,
	 * then computeFields of each result ALS (to not waste time computing
	 * unused ALSs) and return them.
	 * <p>
	 * I'm only called locally, but I'm package visible for my test-case.
	 * <p>
	 * NOTE: This caching technique relies on the fact that both hinters that
	 * allowNakedSets (AlsXz and AlsWing) are executed BEFORE all hinters that
	 * suppress Naked Sets (AlsChain and DeathBlossom). I do not know what
	 * happens with a static cache in the test-cases: I presume the cache is
	 * always null. Caching exists to speed-up LogicalSolverTester, which is
	 * the only place it's tested.
	 *
	 * @param grid
	 */
	void getAlss(final Grid grid) {
//		final long start = System.nanoTime(); // DEBUG
		if ( grid.hintNumber!=alssHn || grid.pid!=alssPid ) {
			numAlss = ALS_FINDER.getAlss(grid, ALSS, allowNakedSets);
			alssNs = allowNakedSets;
			alssHn = grid.hintNumber;
			alssPid = grid.pid;
			rccsDirty = true; // make getRccs refetch
		} else if ( allowNakedSets != alssNs ) {
			// if allowNakedSets has been turned ON then refetch all alss.
			if ( allowNakedSets )
				numAlss = ALS_FINDER.getAlss(grid, ALSS, allowNakedSets);
			// else just remove ALSS containing a cell in a NakedSet
			// no NakedSets means no need to filter them out
			else if ( ALS_FINDER.setNakedSetIdxs(grid) )
				filterOutNakedSets();
			alssNs = allowNakedSets;
			rccsDirty = true; // make getRccs refetch
		}
//		System.out.println(tech.name()+": allowNakedSets="+allowNakedSets+" getAlss took "+(System.nanoTime()-start)); // DEBUG
//		getAlssTime += System.nanoTime() - start; // DEBUG
	}
//	protected long getAlssTime; // DEBUG

	// remove ALSS which contain a cell that is in a NakedSet in the als.region
	private static void filterOutNakedSets() {
		Als als;
		int cnt = 0;
		// cells in nakedSets in each region
		final Idx[] nakedSetIdxs = ALS_FINDER.nakedSetIdxs;
		// newAlss holds ALSs with no cell in a NakedSet
		final Als[] newAlss = new Als[numAlss];
		// foreach als (as found by a previous hinter)
		for ( int i=0; i<numAlss; ++i )
			// if als contains no cells that're in nakedSets in its region
			if ( (als=ALSS[i]).idx.disjunct(nakedSetIdxs[als.region.index]) )
				// then add the bastard to the temp array, for now
				newAlss[cnt++] = als;
		// copy newAlss back over the fixed ALSS array
		// and set numAlss to the possibly reduced cnt
		System.arraycopy(newAlss, 0, ALSS, 0, numAlss=cnt);
	}

	/**
	 * See if each distinct pair of ALSs have one or possibly two RC values.
	 * RC is an acronym for Restricted Candidate. An RC value is one that is
	 * common to both ALSs where all instances of that value in both ALSs see
	 * each other (that's the restriction) so that either of these ALSs will
	 * contain this value, but never both.
	 * <p>
	 * Two ALSs can have a maximum of two RC values, which are both stored in
	 * the one Rcc. A third RC value cannot exist it a valid Sudoku, and since
	 * our Sudokus are known to be valid to get here, this situation cannot
	 * exist here, so we can get away with just ignoring it.
	 * <p>
	 * if allowOverlaps then ALSs can overlap as long as the overlapping area
	 * does not contain the RC. If however allowOverlaps is false then ALSs
	 * that physically overlap are excluded from the RCC search.
	 * <p>
	 * if forwardOnly then we find distinct pairs of ALSs only, else we find
	 * all possible pairs of ALSs, producing an RCC for ALSs a-b and also b-a,
	 * which AlsChain requires.
	 * <p>
	 * I'm only called locally. I'm package visible for the test-cases. sigh.
	 */
	void getRccs() {
//		final long start = System.nanoTime(); // DEBUG
		// if allowOverlaps or forwardOnly has changed then refetch
		if ( rccsDirty || rccsAO!=allowOverlaps || rccsFO!=forwardOnly ) {
			numRccs = rccFinder.find(ALSS, numAlss, RCCS);
			rccsDirty = false;
			rccsAO = allowOverlaps;
			rccsFO = forwardOnly;
		}
//		getRccsTime += System.nanoTime() - start; // DEBUG
//		System.out.println(tech.name()+": allowNakedSets="+allowNakedSets+" getRccs took "+(System.nanoTime()-start)); // DEBUG
	}
//	protected long getRccsTime; // DEBUG

}
