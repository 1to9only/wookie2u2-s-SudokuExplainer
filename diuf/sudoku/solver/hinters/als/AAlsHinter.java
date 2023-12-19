/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import static diuf.sudoku.Config.CFG;
import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Debug;

/**
 * AAlsHinter is the abstract Almost Locked Set (ALS) hinter. I get the alss
 * and rccs (Restricted Common Candidates) for the BigWings, DeathBlossom,
 * AlsXz, AlsWing, and AlsChain Sudoku solving techniques.
 * <p>
 * To do this I implement IHinter#getHints to get the alss and rccs which I
 * pass down to my subclasses implementation of my abstract findAlsHints
 * method. My subclasses: BigWings, DeathBlossom, AlsXz, AlsWing, and AlsChain
 * all override findAlsHints to find the actual hints. All the AlsHinters (and
 * only the AlsHinters) reside in the als package (SueDeCoq is now in aals).
 * <p>
 * BigWings and DeathBlossom do not fetch RCCs, just ALSs. They are AlsHinters.
 * AlsXz, AlsWing, and AlsChain might more appropriately be called RccHinters,
 * but an RCC is just a connection between two ALSs, so they are all AlsHinters
 * really; so AAlsHinter I am, Stan. Fetch the gun. I ____ing hate cats; rather
 * I ____ing hate the fact that cats eat a million marsupials a day and rising.
 * Poor fella my country. I watch my planet dieing, and my heart aches. Sigh.
 * But anyway...
 * <p>
 * This is big-n country. Currently MAX_ALSS = 1536. Mathematically theres upto
 * 4k ALSs in a grid, but in practice, in MY puzzles, 1536 is sufficient, so my
 * maths is wrong. Sigh. Currently MAX_RCCS = 12k. Its logs a WARNING if these
 * "efficiency limits" are exceeded; it doesnt AIOOBE. Just enlarge the limits,
 * but AlsChains takes exponentially longer to process more ALSs, to find a
 * precipitously decreasing number of eliminations. Its a compromise I know not
 * how to calculate. I am too dumb to work-out how to work-it-out, but my gut
 * knows it exists; so I do what works for MY puzzles, in the hope it will work
 * well enough for ALL puzzles. So 1536 it is. Sigh.
 * <p>
 * Finding alss is O(n), ie straight forward, but rccs are O(n*n) with big-n,
 * hence RccFinder is decomposed by ForwardOnly/All for efficiency.<ul>
 * <li>ForwardOnly means A-&gt;B. Used in AlsXz and AlsWing.
 * <li>All lists both A-&gt;B and B-&gt;A. Used in AlsChain.
 * <li>A factory method selects the right RccFinder for each tech, ONCE.
 * <li>BigsWings and DeathBlossom do not use Rccs, so there RccFinder is null.
 * </ul>
 * <pre>
 * KRC 2021-06-01 cache to speed-up ALSWing, ALSChain, and DeathBlossom.
 * Note that all caching is static, ie one copy of the cache and all associated
 * variables are shared by ALL instances of AAlsHinter, so caches are shared
 * among BigWings, DeathBlossom, AlsXz, AlsWing, and AlsChain. This works well,
 * hence caching ALSs and RCCs works-out about 16 seconds faster. Here is the
 * story:
 * -- AlsFinder
 * 1. BigWings gets the ALSs (is doesnt use Rccs).
 * 2. DeathBlossom re-uses the cached ALSs (it gets nothing).
 * -- RccFinderForwardOnly
 * 3. AlsXz gets the Rccs based-on the cached alss.
 * 4. AlsWing re-uses the cached RCCs (another free ride).
 * -- RccFinderAll
 * 5. AlsChain filters cells in NakedSets from cached alss, and gets its own
 *    RCCs coz it needs B-&gt;A as well as A-&gt;B (which I call All).
 * ALSs are fetched once, and used in 5 hinters. Pretty bloody clever.
 * All Retarded Colostomy Calamities are another (slow) story. Sigh.
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
 * so that is 16.321 seconds faster over top1465, a major improvement!
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
 * * ALS_Wing is fully optimised. I know not why it is slower than ALS_Chain.
 *   As far as I am concerned it cannot be, but there is wiggle room here.
 * * ALS_Chain now takes ~twice time for ~threeTimes hints; which is not bad
 *   value, though I still wish it was faster.
 * * so I am done here, I think.
 * </pre>
 *
 * @author Keith Corlett 2020 May 24
 */
abstract class AAlsHinter extends AHinter {

	/* MAX_ALS_SIZE is the maximum number of cells in an ALS (ergo max degree).
	 *
	 * Your MAX_ALS_SIZE is a matter of choice.
	 * Choose 8 for correctness. Everything below 8 is a hack.
	 * I choose 5 for speed over correctness. A HACK!
	 * Choose 6 or 7 for a less dirty hack.
	 * Below 5 seems pointless, just unwant als hinters instead.
	 *
	 * The largest possible ALS is 8. All cells in a region (if all empty) are
	 * allways a locked set, never an almost locked set. I think I should also
	 * ignore regions that contain a naked/hidden-single when finding alss, for
	 * Shft-F5 (find more hints) but I currently do not. Too hard did not try.
	 * Its only Shft-F5 for ____s sake. Naked Sets also throw me a curveball,
	 * and I decided to not ignore them either. It still works, sometimes. */

	/*       time(ns)  calls  time/call elims  time/elim  hinter
	 *  4,146,334,900  12562    330,069  2168  1,912,516  BigWings
     *  5,658,477,600  11356    498,280  2758  2,051,659  DeathBlossom
     *  9,450,131,900   9010  1,048,849  2025  4,666,731  ALS_XZ
     * 10,634,181,200   7757  1,370,914  2773  3,834,901  ALS_Wing
     * 19,370,235,000   5549  3,490,761  2000  9,685,117  ALS_Chain
	 * 49,259,360,600                   11724  4,201,583  TOTAL */
// 	public static final int MAX_ALS_SIZE = Grid.REGION_SIZE - 1;  *  8

	/*       time(ns)  calls  time/call elims  time/elim  hinter
     *  3,929,310,600  12531    313,567  2148  1,829,287  BigWings
     *  5,085,782,600  11336    448,639  2714  1,873,906  DeathBlossom
     *  8,374,530,000   9023    928,131  2041  4,103,150  ALS_XZ
     *  9,551,062,700   7769  1,229,381  2751  3,471,851  ALS_Wing
     * 17,063,493,900   5575  3,060,716  2005  8,510,470  ALS_Chain
	 * 44,004,179,800			        11659  3,774,267  TOTAL */
// 	public static final int MAX_ALS_SIZE = 7;

	/*       time(ns)  calls  time/call elims  time/elim  hinter
     *  3,427,154,600  12527    273,581  2113  1,621,937  BigWings
     *  3,429,971,400  11353    302,120  2538  1,351,446  DeathBlossom
     *  5,290,525,700   9213    574,245  2048  2,583,264  ALS_XZ
     *  6,336,587,700   7953    796,754  2605  2,432,471  ALS_Wing
     * 12,057,130,900   5887  2,048,094  2106  5,725,133  ALS_Chain
	 * 30,541,370,300                   11410  2,676,720  TOTAL */
// 	public static final int MAX_ALS_SIZE = 6;

	/*       time(ns)  calls  time/call elims  time/elim  hinter
     *  2,084,588,300  12317    169,244  1715  1,215,503  BigWings
     *  1,300,498,300  11339    114,692  2016    645,088  DeathBlossom
     *  1,984,920,300   9657    205,542  1736  1,143,387  ALS_XZ
     *  2,230,500,000   8600    259,360  1727  1,291,546  ALS_Wing
     *  5,536,687,300   7221    766,747  2149  2,576,401  ALS_Chain
	 * 13,137,194,200                    9343  1,406,100  TOTAL */
	/**
	 * The maximum number of cells in an Almost Locked Set (ergo max degree).
	 */
	public static final int MAX_ALS_SIZE = 5;

	/**
	 * The maximum number of Almost Locked Sets (ergo static ALSS array size).
	 * <pre>
	 * MAX_ALS_SIZE    5    6     7     8
	 * MAX_ALSS      512  512  1024  1536
	 * </pre>
	 */
	protected static final int MAX_ALSS = 512;

	/**
	 * The size of the RCCS array, ie max Restricted Common Candidates.
	 * <p>
	 * Finding RCCs is an O(numAlss*numAlss) process, so each implementation is
	 * as fast as possible, which is still too slow.
	 * <p>
	 * If the RccFinder exceeds MAX_RCCS then a "WARN:"ing is printed to both
	 * Log and stdout. Ignore generate warnings. I dont tie-up RAM in a larger
	 * than required array just to suit generate, but if generate is your focus
	 * and you have heaps of RAM then increase MAX_ALSS, 1K at a time will do.
	 * The proportion of RCCs to ALSSs is pretty stable independent of size.
	 * <p>
	 * Largest 617#C:\Users\User\Documents\NetBeansProjects\DiufSudoku\top1465.d5.mt
	 * <pre>
	 * MAX_ALS_SIZE   5  6  7  8
	 * MAX_RCCS       7  ?  ?  8 K
	 * </pre>
	 */
	protected static final int MAX_RCCS = 7 * 1024;

	// DeathBlossom uses this to mark unused params; for self-doco code.
	protected static final boolean NOTUSED = false;

	// ALSS cache is static to share among all my subclasses,
	// especially AlsXz and AlsWing, which use the same ALSS and RCCS.
	protected static final Als[] ALSS = new Als[MAX_ALSS];
	protected static int numAlss;
	// these control the alss-cache which effects the rccs-cache
	private static int alssHn; // cached-alss grid.hintNumber
	private static long alssPid; // cached-alss grid.puzzleID

	// RCCS cache is static to share among all my subclasses,
	// especially AlsXz and AlsWing, which use the same ALSS and RCCS.
	protected static final Rcc[] RCCS = new Rcc[MAX_RCCS];
	protected static int numRccs;
	private static boolean rccsDirty, rccsFO;

	// the RccFinders are static to share between instances
	private static RccFinder all; // singleton
	protected static final RccFinder getRccFinderAll() {
		if ( all == null )
			all = new RccFinderAll();
		return all;
	}

	// the RccFinders are static to share between instances
	private static RccFinder fast; // singleton
	protected static final RccFinder getRccFinderAllFast() {
		if ( fast == null )
			fast = new RccFinderAllFast();
		return fast;
	}

	// the RccFinders are static to share between instances
	private static RccFinder recycle; // singleton
	protected static final RccFinder getRccFinderAllRecycle() {
		if ( recycle == null )
			recycle = new RccFinderAllRecycle();
		return recycle;
	}

	/**
	 * Returns an implementation of RccFinder suitable for the given Tech.
	 * <p>
	 * Inheritance is used for performance. RccFinder is split on forwardOnly.
	 * We avert repeatedly deciding how to handle each variant, by choosing an
	 * RccFinder ONCE, during construction.
	 * <p>
	 * I am package visible for the test-cases. Sigh. I am final because, umm,
	 * I dont know really, I just dont know how to over-anything static methods
	 * hence I reckon making them final might be safer, coz I am stupid. One
	 * embraces ones stupidity. Six times if you're Morisson. Hexowanker Man!
	 * I wonder if we can con him into moving to Gaza? Teach him about wankers.
	 * And private governance. It is what it is. Killing s__t loads of c__ts
	 * won't change what it is. Deal with what it actually is. So what's the
	 * actual problem? Too many c__ts, obviously! There is no them. There is
	 * only us, such as we are. Sigh.
	 * <pre>
	 * BigWings, DeathBlossom : null
	 * AlsXz, AlsWing         : RccFinderForwardOnly
	 * AlsChain               : RccFinderAll
	 * </pre>
	 *
	 * @param tech the Sudoku solving technique you need an RccFinder for
	 * @return a new RccFinder suitable for the given tech
	 */
	static final RccFinder getRccFinder(final Tech tech) {
		switch ( tech ) {
			case BigWings:
			case DeathBlossom:
				return null;
			case ALS_XZ:
				return new RccFinderForwardOnly();
			// nb: AlsWing and AlsChain share an RccFinder, hence we need a
			// static variable to hold the singleton instance, with a static
			// getter method that creates/returns-existing singleton.
			case ALS_Wing:
			case ALS_Chain:
//nieve implementation has been retained for edification. Understand it before
//you look at RccFinderAllFast. Recycle is a total head____. Good luck.
//				return getRccFinderAll(); // nieve
				// Recycle only if there will be existing rccs to recycle
				if ( CFG.getBoolean(Tech.ALS_XZ.name()) )
					return getRccFinderAllRecycle(); // ~16secs faster than nieve
				else
					return getRccFinderAllFast(); // ~10secs faster than nieve
			default: throw new IllegalArgumentException("Unknown tech: "+tech);
		}
	}

	// ----------------------------- instanceland -----------------------------

	// getRccs
	/** rccFinder finds the Restricted Common Candidates between ALSs */
	protected final RccFinder rccFinder;
	/** should getRccs do a fast forwardOnly search, or a full search? */
	protected final boolean forwardOnly;

	/** my alsFinder finds the ALSs (Almost Locked Sets). */
	private final AlsFinder alsFinder;

	/**
	 * Construct a new abstract Almost Locked Set (ALS) hinter, whose findHints
	 * implementation finds the ALSs and optionally there RCCs, and passes them
	 * down to my subclasses "custom" findHints method.
	 * <pre>
	 * Tech          RccFinder
	 * BigWings      null
	 * DeathBlossom  null
	 * ALS_XZ        RccFinderForwardOnly
	 * ALS_Wing      RccFinderForwardOnly
	 * ALS_Chain     RccFinderAll
	 * </pre>
	 *
	 * @param tech the Tech(nique) we implement: BigWings, DeathBlossom, AlsXz,
	 *  AlsWing, or AlsChain is passed to my super, a "normal" AHinter.
	 */
	public AAlsHinter(final Tech tech) {
		super(tech);
		assert tech.isAlsic();
		this.alsFinder = new AlsFinder();
		// getRccs
		this.rccFinder = getRccFinder(tech);
		this.forwardOnly = rccFinder!=null ? rccFinder.forwardOnly() : NOTUSED;
	}

	/**
	 * AAlsHinter actually implements IPrepare, but leaves the declaration
	 * of {@code implements diuf.sudoku.solver.hinters.IReporter} up to each
	 * of my subclasses, so that you can decide weather or not you can be
	 * disabled, which is always nice.
	 * <p>
	 * The prepare method is called after the LogicalSolver is configured,
	 * but before we attempt to solve each Grid. It gives the LogicalSolver
	 * and each of it is hinters a chance to prepare to solve this new Grid;
	 * as distinct from getHints on probably-the-existing-grid. Note that
	 * Grid now has an id&nbsp;field to identify the puzzle that is loaded
	 * into this grid.
	 * <p>
	 * This instance re-enables this hinter, in case it has been disabled,
	 * and clears Validator.INVALIDITIES if the Validator is used.
	 *
	 * @param grid
	 */
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
	}

	/**
	 * findAlsHints is implemented by my subclasses to do the hint search.
	 * I just pass him everything I know, whether he uses it or not. But if
	 * nobody uses something (like grid.idxs for example) then it might be
	 * an idea to not calculate it in the first place. Sigh.
	 *
	 * @return true if a hint was found, else false
	 */
	protected abstract boolean findAlsHints();

	/**
	 * Find Almost Locked Set hints in Grid and add them to IAccumulator.
	 * <p>
	 * @return were any hint/s found.
	 */
	@Override
	public final boolean findHints() {
		getAlss();
		getRccs();
		return findAlsHints();
	}

	/**
	 * getAlss does the recursive ALS search at each cell in each region, then
	 * computeFields of each result ALS (dont waste time computing unused ALSs)
	 * and return them.
	 */
	private void getAlss() {
		if ( grid.hintNumber!=alssHn || grid.puzzleId!=alssPid ) {
			numAlss = alsFinder.getAlss(grid, ALSS, MAX_ALS_SIZE);
			alssHn = grid.hintNumber;
			alssPid = grid.puzzleId;
			rccsDirty = true;
		}
	}

	/**
	 * See if each distinct pair of ALSs have one or possibly two RC values.
	 * RC is an acronym for Restricted Candidate. An RC value is one that is
	 * common to both ALSs where all instances of that value in both ALSs see
	 * each other (that is the restriction) so that either of these ALSs will
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
	 */
	private void getRccs() {
		if ( rccFinder!=null && (rccsDirty || rccsFO!=forwardOnly) ) {
			// static: subsequent test-cases fail if oldNumRccs != 0
			final int oldNumRccs = Run.isTestCase() ? 0 : numRccs;
			numRccs = rccFinder.find(ALSS, numAlss, RCCS, oldNumRccs);
			rccsDirty = false;
			rccsFO = forwardOnly;
		}
	}

}
