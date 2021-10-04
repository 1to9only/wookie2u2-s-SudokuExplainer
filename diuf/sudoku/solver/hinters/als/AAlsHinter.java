/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.Grid;
import diuf.sudoku.Idx;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import java.util.Arrays;

/**
 * The abstract ALS Hinter implements the fetching of ALSs and RCCs, which is
 * common to BigWings, AlsXz, AlsWing, AlsChain, and DeathBlossom. Actually, I
 * implement getHints to get: candidates, NakedSets, ALSs, and RCCs; which I
 * pass along to my subtypes "custom" findHints method, returning his result.
 *
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
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
abstract class AAlsHinter extends AHinter
		implements diuf.sudoku.solver.hinters.IPreparer
{

	/** the size of the fixed-size alss array: 512 */
	protected static final int MAX_ALSS = 512; // Observed 283

	/** the size of the fixed-size rccs array: 8192 */
	protected static final int MAX_RCCS = 8192; // 833#top1465.d5.mt=6764

	// include single bivalue Cells in the ALSs list?
	// nb: 1 cell with 2 potential values is by definition an ALS. I don't know
	// what problems it causes if you set this to true. You have been warned!
	protected static final boolean SINGLE_CELL_ALSS = false;

	// the default rccs List capacity
	protected static final int NUM_RCCS = 2000;

	// DeathBlossom uses this to mark unused params; for self-doc'ing code.
	protected static final boolean UNUSED = false;

	/** include ALSs which overlap in my findHints method? */
	protected final boolean allowOverlaps;
	/** include ALSs which include cells that are part of a Naked Set? */
	protected final boolean allowNakedSets;
	/** should I run getRccs; false means I pass you rccs=null in findHints */
	protected final boolean findRCCs;
	/** should getRccs do a fast forwardOnly search, or a full search? */
	protected final boolean forwardOnly;

	// in findHints: if !allowNakedSets we call findNakedSets to set
	// inNakedSet[region.index][cell.i]=true for each cell in a
	// NakedPair/Triple/etc in each region;
	// then recurseAlssNoNs ignores cell if inNakedSet[region.index][cell.i].
	// NOTE: stripNakedSets now also calls findNakedSets to remove ALSs
	// containing a cell in a NakedSet from the existing alsCache, for speed.
	protected final boolean[][] inNakedSets;

	/** for AlsXzTest: true uses AlsFinderRecursive, false uses AlsFinder. */
	static final boolean USE_ALS_FINDER_RECURSIVE = true;
	/** ALS_FINDER protected so that BigWings can report counts. */
	protected static final AlsFinder ALS_FINDER;
	static {
		if ( USE_ALS_FINDER_RECURSIVE )
			ALS_FINDER = new AlsFinderRecursive();
		else
			ALS_FINDER = new AlsFinder();
	}

	/** RCC_FINDER finds the Restricted Common Candidates between ALS's */
	static final RccFinder RCC_FINDER = new RccFinder();

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
	 * @param findRCCs true for "normal" ALS-use, false to just fetch ALSs.<br>
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
	public AAlsHinter(Tech tech, boolean allowNakedSets, boolean findRCCs
			, boolean allowOverlaps, boolean forwardOnly) {
		super(tech);
		this.allowNakedSets = allowNakedSets;
		// unused if allowNakedSets, else an array for each of the 27 regions
		if ( allowNakedSets )
			this.inNakedSets = null;
		else
			this.inNakedSets = new boolean[27][];
		this.findRCCs = findRCCs;
		this.allowOverlaps = allowOverlaps;
		this.forwardOnly = forwardOnly;
	}

	/**
	 * A simplified constructor for hinters that fetch ALSs only (no RCCs),
	 * currently BigWings and DeathBlossom.
	 */
	public AAlsHinter(Tech tech, boolean allowNakedSets) {
		this(tech, allowNakedSets, false, UNUSED, UNUSED);
	}

	/**
	 * Prepare is called after the LogicalSolver is configured and before we
	 * attempt to solve each Grid. It gives the LogicalSolver and it's hinters
	 * a chance to prepare to solve this new Grid; as distinct from getHints on
	 * probably-the-existing-grid. Also note that Grid now has an id field
	 * which identifies the puzzle that's loaded into the grid
	 *
	 * @param grid
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
		// if we're eradicating bugs from Als* or DeathBlossom hints
		if ( HintValidator.ALS_USES || HintValidator.DEATH_BLOSSOM_USES ) {
			// clear any existing invalidities before processing a new Grid.
			// note that HintValidator.solutionValues is already set.
			HintValidator.INVALIDITIES.clear();
		}
	}

	/**
	 * findHints is implemented by my subclasses to do the actual hint search.
	 * I pass him everything I know (whether he uses it or not):
	 * @param grid The Grid that we're solving
	 * @param candidates an array of the indices of Grid cells which maybe
	 *  each potential value 1..9
	 * @param alss a fixed-array of the Almost Locked Sets
	 * @param numAlss the number of ALSs in the alss array
	 * @param rccs a fixed-array of the Restricted Common Candidates
	 * @param numRccs the number of RCCs in the rccs array
	 * @param accu The IAccumulator to which we add any hints
	 * @return true if a hint was found, else false
	 */
	protected abstract boolean findHints(Grid grid, Idx[] candidates
			, Als[] alss, int numAlss, Rcc[] rccs, int numRccs
			, IAccumulator accu);

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
		// get indices of cells which maybe each value 1..9.
		final Idx[] candidates = grid.idxs;
		// get the Almost Locked Sets (N cells with N+1 values between them)
//		long start = System.nanoTime();
		getAlss(grid, candidates); // repopulates alss, sets numAlss
//		tookAlss += System.nanoTime() - start;

		// if findRCCs then get the Restricted Common Candidates of those ALSs
		if ( findRCCs ) {
//			start = System.nanoTime();
			getRccs(); // repopulates rccs, sets numRccs
//			tookRccs += System.nanoTime() - start;
		} else
			numRccs = 0;
		// call my subclasses "custom" findHints method to search the grid for
		// instances of this specific pattern; raise a hint and add it to accu.
		return findHints(grid, candidates, ALSS, numAlss, RCCS, numRccs, accu);
	}
//	protected long tookAlss=0L, tookRccs=0L; // reported in AlsChain

	/**
	 * getAlss starts the recursive ALS search at each cell in each region,
	 * then computeFields of each result ALS (don't waste time computing unused
	 * ALSs) and return them.
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
	 * @param candidates
	 * @param alss
	 * @param nulAlss
	 * @return the ALSs in the given grid
	 */
	void getAlss(Grid grid, Idx[] candidates) {
//		final long start = System.nanoTime();
		if ( ALSS==null || grid.hintNumber!=alssHn || grid.pid!=alssPid ) {
			numAlss = ALS_FINDER.getAlss(grid, candidates, ALSS, allowNakedSets);
			alssNs = allowNakedSets;
			alssHn = grid.hintNumber;
			alssPid = grid.pid;
			rccsDirty = true; // make getRccs refetch
		} else if ( allowNakedSets != alssNs ) {
			if ( allowNakedSets )
				numAlss = ALS_FINDER.getAlss(grid, candidates, ALSS, allowNakedSets);
			else // remove each ALS with a cell in a NakedSet from alsCache
				stripNakedSets(grid);
			alssNs = allowNakedSets;
			rccsDirty = true; // make getRccs refetch
		}
//		getAlssTime += System.nanoTime() - start;
	}
//	protected long getAlssTime; // DEBUG
	// this cache is shared accross all instances of AAlsHinter!
	private static final Als[] ALSS = new Als[MAX_ALSS];
	private static int numAlss;
	// these control both the alsCache and the rccCache!
	private static int alssHn; // als-cache hintNumber
	private static long alssPid; // als-cache puzzleID
	private static boolean alssNs; // does alss-cache include Naked Sets

	// remove each ALS with a cell in a NakedSet from alsCache
	private void stripNakedSets(Grid grid) {
		// set-up inNakedSets (27 * 81 = 2187 booleans)
		// no NakedSets means there is no need to filter them out
		if ( ALS_FINDER.findNakedSetIdxs(grid) ) {
			int cnt = 0;
			// temp array to hold ALSs with no cell in any NakedSet
			final Als[] temp = new Als[numAlss];
			for ( int i=0; i<numAlss; ++i ) {
				final Als als = ALSS[i];
				if ( !als.idx.andAny(ALS_FINDER.nakedSetIdxs[als.region.index]) )
					temp[cnt++] = als;
			}
			// release memory back to system
			Arrays.fill(this.inNakedSets, null);
			// copy into the fixed-size array
			System.arraycopy(temp, 0, ALSS, 0, numAlss=cnt);
		}
	}

	/**
	 * See if each distinct pair of ALSs have one or possibly two RC values.
	 * An RC value is one that is common to both ALSs where all instances of
	 * that value in both ALSs see each other (that's the restriction).
	 * <p>
	 * Two ALSs can have a maximum of two RC values, which are both stored in
	 * the one Rcc.
	 * <p>
	 * ALSs can overlap as long as the overlapping area doesn't contain an RC.
	 * <p>
	 * I'm only called locally, but I'm package visible for my test-case. sigh.
	 *
	 * @param alss
	 * @return
	 */
	void getRccs() {
		// if allowOverlaps or forwardOnly has changed then refetch
		rccsDirty |= rccsAO!=allowOverlaps || rccsFO!=forwardOnly;
		if ( RCCS==null || rccsDirty ) {
			numRccs = RCC_FINDER.getRccs(ALSS, numAlss, RCCS, forwardOnly
					, allowOverlaps);
			rccsDirty = false;
			rccsAO = allowOverlaps;
			rccsFO = forwardOnly;
		}
	}
	// Note that caching is static to share among all my subclasses, especially
	// between AlsXz and AlsWing, which use exactly the same constructor params
	// package visible for acces by testcases only!
	static final Rcc[] RCCS = new Rcc[MAX_RCCS];
	static int numRccs;
	private static boolean rccsDirty, rccsAO, rccsFO;

}
