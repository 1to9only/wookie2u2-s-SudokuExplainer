/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.utils.LongLongHashMap;
import diuf.sudoku.utils.Counter;
import diuf.sudoku.Run;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VALL;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.utils.Frmt.SPACE;
import diuf.sudoku.utils.IntIntHashMap;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyArrays;
import diuf.sudoku.utils.MyClass;
import diuf.sudoku.utils.MyStrings;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.io.File;

/**
 * AAlignedSetExclusionBase is an abstract base class of all Aligned*Exclusion.
 * It contains common implementations and a few dirty hacks that are best kept
 * in the cupboard under the stairs. OWELS? WHAT OWELS!?!?
 *
 * A partial implementation of Aligned Set Exclusion Sudoku solving technique,
 * for sets of 2 to (currently) 10.
 * <p>
 * This is copy-paste of AlignedTripleExlusion asat 2019-09-02 10:16:00 with
 * the dependency on it being a Triple removed, so it's closer to Juillerats
 * original AlignedExclusion, which only actually covered sets of 3.<br>
 * Implements the aligned set exclusion Sudoku solving technique for sets of
 * [2 came later], 3, 4, or 5 Cells. I use AlignedPairExclusion for sets of 2
 * because the algorithm is different (no tail cells) [but no longer], and
 * AlignedTripleExclusion for a set of 3, because it's a bit faster; so asat
 * 2019-08-04 10:35 this is only been used to implement AlignQuadExclusion
 * with a degree of 4. I shall try AlignedQuadExclusion=4, then Pent=5, Hex=6,
 * Sept=7, Oct=8, Nona=9, and finally Dec=10. Note that nothing needs to change
 * to cover 5 or more Cells, but it might be quicker to roll your own Sudoku
 * Explainer whilst simultaneously huffing a line of <i>{@code Cletus's Organic
 * Sun-dried Nut Butter}</i> (mind the blue ones) through a didge.
 * <hr><p>
 * KRC 2019-09-04 09:40:00 looks like I've finally gotten a sane and reasonable
 * AlignedQuad hint, instead of a long list of false positives, so I'm going to
 * create a test-case for it, then build, then test with Shift-F5 (more hints).
 * <p>
 * KRC 2019-09-05 24:14:00 it looks like AlignedPents are just too slow.
 * I tried it on JUST the first 10 puzzles in top1465.d5.mt, ie the 10 hardest.
 * It broke the alignments in my log-file format. Don't try this at home kids.
 * <pre>
 *         time (ns) call        ns/call  elim         ns/elim  hinter
 *        67,918,215  374        181,599     7       9,702,602	Unique Rectangle
 *        11,942,059  371         32,188     2       5,971,029	Bi-Uni Grave
 *       178,670,580  370        482,893     1     178,670,580	Aligned Pair
 *     1,253,588,590  369      3,397,259     7     179,084,084	Aligned Triple
 *    72,313,949,523  364    198,664,696     7  10,330,564,217	Aligned Quad
 * 1,386,712,870,155  360  3,851,980,194    10 138,671,287,015	Aligned Pent for JUST the hardest 10 puzzles
 *     1,717,099,794  352      4,878,124  2778         618,106	Unary Chain
 *       465,149,482  243      1,914,195    14      33,224,963	Nishio Chain
 *       863,660,012  230      3,755,043   270       3,198,740	Multiple Chain
 *     1,540,880,976  116     13,283,456   316       4,876,205	Dynamic Chain
 *       132,454,495    2     66,227,247    20       6,622,724	Dynamic Plus
 * 1,465,348,355,039 (24 minutes) for the first 5 puzzles in top1465.d5.mt
 * </pre>
 * <p>
 * KRC 2019-09-06 14:24:00 AlignedPents are still really really really slow!
 * <pre>
 *         time (ns) call        ns/call  elim         ns/elim  hinter
 *        31,509,483  28      1,125,338	      0	             0	Aligned Pair
 *       157,317,150  28      5,618,469	      0	             0	Aligned Triple
 *     9,741,985,017  28    347,928,036	      0	             0	Aligned Quad
 *   246,579,104,988  28  8,806,396,606	      0	             0	Aligned Pent
 *       105,989,194  28      3,785,328	      2	    52,994,597	Unary Chain
 *        76,448,024  26      2,940,308	      1	    76,448,024	Nishio Chain
 *        83,186,175  25      3,327,447	     13	     6,398,936	Multiple Chain
 *       222,494,125  21     10,594,958	     38	     5,855,108	Dynamic Chain
 *       202,620,648   1    202,620,648	     10	    20,262,064	Dynamic Plus
 *   257,232,115,627 just for 5#top1465.d5.mt; for which A5E doesn't find a hint
 *   and displays the worst observed performance of 14 seconds per call. Sheesh!
 * </pre>
 * <hr><p>
 * KRC 2019-09-07ish I've tried an AlignedPentExclusion class, which used same
 * "twin" technique as AlignedTripleExclusion TWICE, but it doesn't work, and
 * I don't  understand why, so I'm feeling pretty stupid right now.
 * <br>
 * Anyways that 8.8 SECONDS per call is just too slow, especially when you
 * compare it to DynamicPlus at 0.2 seconds to never not find a bloody hint.
 * So I've just "unavailed" A5Es, unless you're programmer, who I presume is
 * interested in what's actually possible with a modern PC:
 * INSERT i10 AND PRESS ANY KEY TO CONTINUE...
 * If CPU's double in speed a couple of more times then A5Es aren't impossible,
 * but DynamicPlus chaining will STILL be faster, and allways find a hint. Why
 * do we do Sudoku puzzles anyways?
 * <hr><p>
 * KRC 2019-09-09 I've just missed 2019-09-09 09:09, damn it Janet!
 * implementing AlignedQuads relegating AlignedSets to Heeza Hazbeen, run out
 * at the bowlers end. We beat the poms! We beat the poms! We BEAT the poms!
 * <hr><p>
 * KRC 2019-09-09 Heeza Hazbeen. AlignTriples/Quads have replaced A7E 3/4
 * and AlignedPents are still too damn slow.
 * <hr><p>
 * KRC 2019-09-11 Re-instating AlignedSetExclusion as an abstract base class
 * for AlignedTripleExclusion, AlignedQuadExclusion, and AlignedPentExclusion
 * in an attempt to dry-out repeated-code inherent in my copy-paste coding. The
 * real differences lie in da allowOrExclude methods. I hope "generic versions"
 * of all the other methods are fast enough.
 * <p>
 * KRC 2019-09-11 relegated AlignedSetExclusion to the abstract class
 * AAlignedSetExclusionBase and modified AlignedTripleExclusion,
 * AlignedQuadExclusion, and AlignedPentExclusion to use the protected methods
 * exposed therein, to mostly-clean-up my cut-and-paste coding. Also moved A5E
 * to just before DynamicPlus in the LogicalSolver chainers list. It's still
 * too slow, but that doesn't matter so much any more.
 * <hr><p>
 * KRC 2019-09-25 Rewrote all Aligned*Exclusion classes to build the allowed
 * values array, to work-out if a hint is possible, and only if so then come
 * back and build the excludedCombosMap. The optimistic approach of building
 * the map on the presumption that it's going to be used is just too slow for
 * such large n's with such low hit rates. So allowOrExclude is history. Each
 * of my subclasses implements it's own isAnyComboValueExcluded, and then later
 * its own buildExcludedCombosMap. This solution runs in about 10% of the time
 * of Juillerats implementation. It's an improvement, so we're getting there.
 * <hr><p>
 * KRC 2019-09-30 V6 rewrite again to naively loop through the candidate cells
 * instead of using the Permutations class, so we get any-common-excluded and
 * isSiblingOf the minimum number of times, which QUARTERS the runtime. This
 * change renders AAlignedSetExclusionBase nearly useless. The only surviving
 * methods common to AlignedTripleExclusion and AlignedQuadExclusion are
 * populateCandidatesAndExcluders and createRedPotentials. Both should probably
 * be used by AlignedPentExclusion also. It was separated out when I started
 * down the idx path because I was exploring.
 * <hr><p>
 * KRC 2019-10-08 building 6.30.024 and I thought I should comment on the A*E
 * debacle. It's slower than a wet week, and it's not getting any faster under
 * my tutelage. I'm not saying it can't get it any faster, I'm just saying that
 * I don't know how to make it any faster; so I cheat and instituted and -REDO
 * switch in LogicalSolverTester which parses a previous logFile to calculate
 * which hinters to call. Pretty snazzy, and also a dirty-low-down-nasty-hack.
 * I also implemented populateCandidatesAndExcluders and createRedPotentials
 * in AAlignedSetExclusionBase. A3..8E extend AAlignedSetExclusionBase. A2E
 * extends my base-class AAAlignedExclusionBase, so we're all cousins atleast.
 * <hr><p>
 * KRC 2019-10-23 Here's the latest. All A*E's extend AAlignedSetExclusionBase,
 * including Aligned2Exclusion, which used to be the odd-man out. I decided the
 * speed gains of A2E's "custom algorithm" simply weren't worth the additional
 * code. It came to a point when I introduced IHintNumberActivatableHinter to
 * dis/activate hinters when they not/are going to hint, to work-around the big
 * A*E's still being slower than a wet bloody week. It brings runtime down to
 * about 3 minutes for top1465.d5.mt (from 56 mins), which I find acceptable.
 * So each Aligned*Exclusion hinter
 * extends AAlignedSetExclusionBase
 *     to implement populateCandidatesAndExcluders,
 *              and populateCandidatesAndAtleastTwoExcluders,
 *              and createRedPotentials
 * extends AHintNumberActivatableHinter
 *     to implement IHintNumberActivatableHinter
 * extends AHinter
 *     to partially implement IHinter
 * Clear as mud, right?
 * <p>
 * NOTE WELL: Aligned2Exclusion extends me to become IHintNumberActivatable,
 * but it still doesn't use my methods, because I reckon the code which is
 * specific to 2 cells is faster-enough to be worth retaining.
 * <hr><p>
 * KRC 2020-02-19 The latest A5+E uses a switch on anyLevel to determine if ALL
 * combos to my right are allowed when all maybes of all cells to my right have
 * already been allowed. So in A10E what was an O(n*n*n*n*n*n*n*n*n) ie n^9
 * operation in is now an O(n) operation at best, and O(n^9) at worst, ie same.
 * <pre>
 * If the rightmost cell is the excluded cell it's all worste-case (ie s__t).
 * If SECOND rightmost cell is the excluded cell it's NEARLY all worste-case.
 * If leftmost cell is the excluded cell it's best case.
 * If there is NO excluded cell (ie 99.99999% of cases) it's middle case; your
 * mileage may vary. I haven't measured it. To do so I'd stick a long counter
 * in each case of each anyLevel switch to be measured.
 * "My Uncle Rodney used his foreskin as an air-break in the downhill."
 * "You should have seen him in the ski jump."
 * </pre>
 * <hr><p>
 * KRC 2020-02-25 Make that A4+E, but I'm still not going to bother with A3E.
 * <p>
 * KRC 2020-02-26 Here's the latest inheritance tree:
 * <pre>
 * {@code
 * abstract class diuf.sudoku.solver.hinters.AHinter
 *         implements IHinter (which is LogicalSolver's reference type)
 *  + abstract class diuf.sudoku.solver.hinters.AHintNumberActivatableHinter
 *       extends AHinter
 *       implements INumberedHinter, ICleanUp
 *    + abstract class AAlignedSetExclusionBase
 *          extends AHintNumberActivatableHinter
 *       + final class Aligned2Exclusion extends AAlignedSetExclusionBase
 *         // implements diuf.sudoku.solver.IReporter, java.io.Closeable
 *       + final class Aligned3Exclusion extends AAlignedSetExclusionBase
 *         // ...
 *       + final class Aligned4Exclusion extends AAlignedSetExclusionBase
 *       + abstract class Aligned5ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned5Exclusion_1C extends Aligned5ExclusionBase
 *          + final class Aligned5Exclusion_2H extends Aligned5ExclusionBase
 *       + abstract class Aligned6ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned6Exclusion_1C extends Aligned6ExclusionBase
 *          + final class Aligned6Exclusion_2H extends Aligned6ExclusionBase
 *       + abstract class Aligned7ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned7Exclusion_1C extends Aligned7ExclusionBase
 *          + final class Aligned7Exclusion_2H extends Aligned7ExclusionBase
 *       + abstract class Aligned8ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned8Exclusion_1C extends Aligned8ExclusionBase
 *          + final class Aligned8Exclusion_2H extends Aligned8ExclusionBase
 *       + abstract class Aligned9ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned9Exclusion_1C extends Aligned9ExclusionBase
 *          + final class Aligned9Exclusion_2H extends Aligned9ExclusionBase
 *       + abstract class Aligned10ExclusionBase extends AAlignedSetExclusionBase
 *          + final class Aligned10Exclusion_1C extends Aligned10ExclusionBase
 *          + final class Aligned10Exclusion_2H extends Aligned10ExclusionBase
 * }NOTES:
 * * All branches are abstract and all leaves are final. If you override an/the
 *   A*E/s then make it/them abstract. A5+E's are NOT instantiated "normally".
 * * Aligned 2, 3, and 4 Exclusion are "correct" only.
 * * Each Aligned5+Exclusion class:
 *   * is introspectively instantiated by LogicalSolver's wantAE method using
 *     the Constructor.newInstance method depending upon the Settings (registry)
 *     values that are set in <i>Options menu ~ Solving techniques</i>.
 *   * extends an Aligned${n}ExclusionBase class that implements methods which
 *     are common to both flavours of aligned ${n} exclusion, especially the
 *     buildExcludedCombosMap method.
 *     The Aligned${n}ExclusionBase classes all
 *     extend AAlignedSetExclusionBase that
 *       extends AHintNumberActivatableHinter that
 *         extends AHinter
 *         which implements IHinter, which is LogicalSolver's reference type.
 * * All Aligned*Exclusion classes have the facility (currently all commented
 *   out) to independantly implement diuf.sudoku.solver.IReporter, and
 *   java.io.Closeable.
 *   * At the end of each run, the LogicalSolverTester run method:
 *     * invokes the LogicalSolvers report method, which in-turn invokes the
 *       report method of each wantedHinter that implements IReporter.
 *     * and then closes its LogicalSolver, which in turn closes each
 *       wantedHinter that implements Closeable.
 *   - NOTE: Currently there is no facility to report or close in the GUI where
 *     it'd be pretty useless because both these methods rely on a full set of
 *     statisitics from the puzzle-set to be analysed, ie top1465.d5.mt
 *   - WARN: Please <b>NEVER</b> mix java.io.Closeable with ICleanUp which is a
 *     hack I stuck in to allow INumberedHinters to wewease Woderwich at da end
 *     of processing of each puzzle, which ain't bloody Closeing time. Clear!
 * * To report (or close) from an A*E just uncomment the implements interface
 *   and the method definition (which may need a refresh). Suggest you try dis
 *   first with A5E_2H (disable the other A*E's) to get the hang of it BEFORE
 *   you start messing about with A10_1C which takes a wet week to run.
 * * The align2 package was intended to replace align; to remove boiler-plate,
 *   Faster for A234E, but slower for A5+E, so this old boiler-plate survives.
 * </pre>
 */
public abstract class AAlignedSetExclusionBase extends AHinter
		implements diuf.sudoku.solver.hinters.IPreparer
{
	/**
	 * Set this ALLWAYS if you want the HitSet to always save, even when hits
	 * are disabled by turning-off hackTop1465, which is one way to create a
	 * set of "virgin" hit-files (with ALL other non-basic hinters disabled).
	 * Then set me to NO_SAVE and use those hitFiles with hackTop1465 true.
	 * This setting is not final, so that it can be changed on-the-fly.
	 */
	public static final int ALLWAYS_SAVE = 0; // save even when !hackTop1465
	public static final int NORMAL = 1; // allows overwrites
	public static final int NO_SAVE = 2; // never save
	public static int hitFileMode = NORMAL; // @check NORMAL

	// coz getCounters() reflectively references by superclass by SUPER_NAME,
	// so if you rename me you need to change SUPER_NAME also.
	private static final String SUPER_NAME = "AHinter";

	/** The size of the CANDIDATES_ARRAY. */
	protected static final int NUM_CANDIDATES = 64; // array size; 81-17=64

	/** We need only one CANDIDATES_ARRAY for ALL Aligned*Exclusion so long as
	 * we're single threaded. If you multithread drop the static. */
	protected static final Cell[] CANDIDATES_ARRAY = new Cell[NUM_CANDIDATES];

	/** We need only one excludersArray for ALL Aligned*Exclusion so long as
	 * we're single threaded. If you're multi-threading kill the static! */
	protected static final CellSet[] EXCLUDERS_ARRAY = new CellSet[81];

	protected static final PrintStream open(String filename, String headerLine) {
		try {
			if ( Run.type == Run.Type.Batch ) {
				PrintStream ps = new PrintStream(filename);
				if ( headerLine != null )
					ps.println(headerLine);
				return ps;
			} else // in the GUI just print to stdout, rather than rewriting a
				   // a "good" log file with a basically empty one.
				return System.out;
		} catch (Exception ex ) {
			StdErr.exit("Failed to open: "+filename, ex);
			return null;
		}
	}

	protected static final String standardHeader() {
		return MyStrings.format(
			  "%4s %2s %2s %2s"
			+ " %2s %2s %2s %2s"
			+ "\t%-110s\t%-25s\t%s"
			, "puzz", "hn", "ce", "eb"	// lineNumber, hintNum, ces, cebs
			, "cl", "ms", "sb", "hi"	// collisions, maybesSize, siblings, hits
			, "cells", "redPots", "usedCmnExcl");
	}

	protected static void standardLog(
			  PrintStream log
			, Cell[] cells
			, int gsl
			, int hintNum
			, Cell[] cmnExcls, int numCmnExcls
			, int[] cmnExclBits, int numCmnExclBits
			, Pots redPots
			, ExcludedCombosMap map
			, AHint hint // not used, but it was, so retained for future
	) {
		log.format("%4d %2d %2d %2d", gsl, hintNum, numCmnExcls, numCmnExclBits);
		log.format(" %2d", countCollisions(cells));
		log.format(" %2d", totalMaybesSize);
		log.format(" %2d", siblingsCount);
		log.format(" %2d", countHits(cmnExclBits, numCmnExclBits, cells));
		log.format("\t%-110s", diuf.sudoku.utils.Frmu.toFullString(SPACE, cells.length, cells));
		log.format("\t%-25s", redPots);
		log.format("\t%s", diuf.sudoku.utils.Frmu.toFullString(SPACE, map.getUsedCommonExcluders(cmnExcls, numCmnExcls)));
		log.println();
		log.flush();
	}

	public static void report(Counter[] counters) {
		for ( Counter counter : counters )
			counter.report();
	}

	public static void hit(Counter[] counters) {
		for ( Counter counter : counters )
			counter.hit();
	}

	// where E*E logging goes to.
	public static PrintStream out = System.out;

	// =========================== instance stuff ==============================

//	// vars to be overridden by each subclass: because an array iterator creates
//	// its own var, but I need all vars to exist in order to see them in a watch
//	// expression that still evaluates despite not all of the local variables
//	// actually existing yet, so we just inherit and override each of the little
//	// bastards. Problem solvered! NB: This may have a small performance impact.
//	// You NEVER get something for nothing. Never say never. Batteries sold out.
//	protected int v0=0, v1=0, v2=0, v3=0, v4=0
//				, v5=0, v6=0, v7=0, v8=0, v9=0;
//
//	// shiftedValues (as per above)
//	protected int sv0=0, sv1=0, sv2=0, sv3=0, sv4=0
//				, sv5=0, sv6=0, sv7=0, sv8=0, sv9=0;
//
//	// these fields exist to be overridden.
//	protected Cell c0, c1, c2, c3, c4, c5, c6, c7, c8, c9;

//	protected final Counter uselessCnt = new Counter("disuselessenate");

	protected final long[] counts = new long[12];
	protected final int[] hintCounts = new int[12];
	protected final String classNameOnly = MyClass.nameOnly(this);

	// Request For Change (RFC):
	// if(hackTop1465) in top1465.d5.${classNameOnly}.hits.txt store each hints:
	// 1. gsl - grid.source.lineNumber: puzzles (1 based) lineNumber in file
	// 2. AHint.hintNumber: the hint number (1 based) in this file
	// 3. the id's of the cells in the aligned set
	// and use them in the next run, to ignore all others. UBER HACK!
	protected final HitSet hits;
	protected String loadedFileName = null; // which puzzle file is loaded into the hits
	protected boolean useHits = false;

	// The "standard" counters for A5+E, so all A*E's get em, just USED in A5+E.
	// BEWARE: these and any Counter's declared in my subclasses are accessed
	// via reflection (see getCounters()), so take care if you ___ with them.
	// @maybe Create abstract class AAlignedLargeSetExclusionBase extends AAlignedSetExclusionBase
	// and make Aligned5+ExclusionBase extend AAlignedLargeSetExclusionBase instead of AAlignedSetExclusionBase
	// to hold all A5+E specific s__t, so that A234E don't have a load of s__t they don't use?
	// @maybe might be better use a HashSet<Counter> and require subclasses to
	// add there own, instead of arbitrarily just getting all of them? Atleast
	// allow subclasses to REMOVE entries from the HashSet if there being in
	// "the standard collection" causes any issues down the track.
//	protected final Counter colCnt = new Counter("colCnt");
//	protected final Counter mbsCnt = new Counter("mbsCnt");
//	protected final Counter sibCnt = new Counter("sibCnt");
//	protected final Counter maxMbs = new Counter("maxMbs", Counter.Type.Pass);
//	protected final Counter hitCnt = new Counter("hitCnt");
//	protected final Counter sumCnt = new Counter("sumCnt");
//	protected final Counter prangCnt = new Counter("prangRate");

	public AAlignedSetExclusionBase(Tech tech, File hitFile) {
		super(tech);
		assert tech.isAligned;
		// 2 and 3 were original
		// 4 was incquisitiveness
		// 5 was greed
		// 6 was insanity
		// 7 was bonkers
		// 8 was madness
		// 9 was certifiable
		// 10 is masturbating with a cheese grater:
		//    slightly ammusing, but mostly painful
		assert degree>=2 && degree<=10;
		if ( Log.log != null ) // BUGGER!
			Log.println(classNameOnly);
		this.hits = new HitSet(hitFile);
	}

	/**
	 * The prepare method prepares this hinter to solve the given mt-file: its
	 * called after we know which puzzle-set we're processing (obviously, coz
	 * we pass in the grid) but before the first call to getHints.
	 * <p>
	 * prepare is actually called shortly before the first call to getHints in
	 * each puzzle, but only actually does anything when the loadedFileName is
	 * not equal to the given grid.source.fileName. The loadedFileName is null
	 * initially, so the first call always loads; subsequent calls don't do
	 * anything unless the puzzle-set changes (ie you open another .mt file).
	 * <p>
	 * This prepare method loads this hinters "hit file" for top1465.d5.mt.
	 * <p>
	 * <pre>
	 * This method is always called by LogicalSolvers prepare method
	 *   (foreach wantedHinter which implements IPreparer)
	 * which is called by LogicalSolvers solve and getAllHints methods,
	 *   and by SudokuExplainers recreateLogicalSolver method.
	 * </pre>
	 * @param grid the grid we're about to solve.
	 */
	@Override
	public void prepare(Grid grid, LogicalSolver logicalSolver) {
		// load top1465.d5.Aligned*Exclusion.hits.txt when puzzleFile changes
		final String fn;
		if ( grid != null // anally retentive
		  && grid.source != null // Avert NPE from pasted puzzles
		  && (fn=grid.source.fileName) != null // anally retentive
		  && !fn.equals(loadedFileName) ) {
			// setting the hitFile name is a bit complicated
			if ( AHinter.hackTop1465 || hitFileMode==ALLWAYS_SAVE )
				hits.setHitFile(HitSet.getHitFile(fn, this));
			useHits = AHinter.hackTop1465 && hits.load();
			if ( useHits && hitFileMode==NO_SAVE )
				hits.setHitFile(null); // so that save aborts
			loadedFileName = fn; // load ONCE per file
		}
//		Log.teef("%s.prepare: %s%s\n", classNameOnly, useHits, useHits ? SPACE+hits.size() : EMPTY_STRING);
	}

	/**
	 * The default implementation of the report() method for those subclasses
	 * which choose to: implements diuf.sudoku.solver.IReporter
	 * so that statistics are uniformly and reliably reported.
	 */
	public void report() {
		Log.teef("\n%s\n", classNameOnly);
		hits.report();
		for ( int i=1,n=counts.length; i<n; ++i ) {
			if ( i>1 && counts[i]==0 )
				break; // end-of null-terminated-list.
			Log.teef("%s// %s HitSet: %d = %,11d of %,11d = %5.2f%% (hints %d)\n"
				, Log.tabs(2+degree), useHits?"with":" w/o", i, counts[i]
				, counts[0], Log.pct(counts[i], counts[0]), hintCounts[i]);
		}
		if ( degree>=5 && classNameOnly.contains("_1C") )
			report(getCounters());
//		uselessCnt.printPass();
//		Log.teef("coversCalls = %,14d\n", coversCalls);
	}

	protected Counter[] getCounters() {
		if ( countersArray == null ) {
			// foreach class from me upto my AAlignedSetExclusionBase
			// add class to a queue to reverse the order of processing.
			Deque<Class<?>> ancestry = new LinkedList<>();
			Class<?> clazz;
			for ( clazz = getClass()
				; !clazz.getName().endsWith(SUPER_NAME)
				; clazz = clazz.getSuperclass() )
				ancestry.addFirst(clazz);
			List<Counter> list = new LinkedList<>();
			// from AAlignedSetExclusionBase downto me, so that "standard"
			// counters are printed first, followed by any "custom" counters.
			while ( (clazz=ancestry.poll()) != null )
				for ( java.lang.reflect.Field field : clazz.getDeclaredFields() )
					if ( field.getType() == COUNTER_CLASS ) try {
						list.add((Counter)field.get(this));
					} catch (Exception eaten) {
						// Do nothing.
					}
			countersArray = list.toArray(new Counter[list.size()]);
		}
		return countersArray;
	}
	private static final Class<?> COUNTER_CLASS = new Counter("mue").getClass();
	private Counter[] countersArray;

	protected void hitCounters() {
		hit(getCounters());
	}

	@Override
	public abstract boolean findHints(Grid grid, IAccumulator accu);

	/**
	 * Populates candidates and excluders arrays, and return numCandidates.
	 * A "candidate" is a cell which can participate in an exclusion set, and
	 * an "excluder set" is the qualified siblings of that candidate cell.
	 * The candidates array is a "normal" compact array, whilst excluders is a
	 * "sparse array" that's coincident with Grid.cells (ie index is Cell.i).
	 * <p>
	 * Finds cells (candidates) which have at least 2 maybes and have at least 1
	 * sibling (excluder) with maybesSize between 2 and $degree (inclusive).
	 * @param candidates {@code Cell[]} to (re)populate
	 * @param excluders {@code CellSet[81]} to (re)populate. Simultaneous
	 * @param grid {@code Grid} to examine
	 * with the Grid.cells array.
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndExcluders(
			  Cell[] candidates		// output
			, CellSet[] excluders	// output
			, Grid grid				// input
	) {
		return populateCandidatesAndExcluders(
				candidates, excluders, grid, this.degreePlus1);
	}
	protected int populateCandidatesAndExcluders(Cell[] candidates
			, CellSet[] excluders, Grid grid, final int degreePlus1) {

		// cell's set of excluder cells. CellSet is a Set<Cell> with reasonably
		// fast contains, remove & clear; but a slow constructor
		// An excluder cell is a sibling with 2..5 maybes and numExcls>0
		CellSet set = null;
		int card,  n=0;

		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		CELL_LOOP: for ( Cell cell : grid.cells ) {
			if ( cell.size > 1 ) {
				// find cells excluders: ie siblings with maybesSize 2..5
				for ( Cell sib : cell.siblings ) { // 81*20=1620
					// sib is an excluder if it has maybesSize 2..degree
					// optimise: do the < first because it's more deterministic.
					if ( (card=sib.size)<degreePlus1 && card>1 ) {
						if ( set == null )
							set = new CellSet(); //slow constructor, O(1) contains
						set.add(sib);
					// optimize: skip any naked singles
					} else if ( card == 1 )
						continue CELL_LOOP; // skip siblings of naked singles
				}
				if ( set != null ) {
					candidates[n++] = cell;
					excluders[cell.i] = set;
					set = null; // ready? reset the set for the next set. lols\
								// pretty piss poor joke that, blood Vessels,
								// but anyway who gives a ____... Moving right
								// along now. How's the hair Ritchie?
				}
			}
		} // next cell in grid
		return n;
	}

	/**
	 * Populates the candidateCellsArray and the cellsExcluders.
	 * <p>
	 * Finds Cells (candidates) which have atleast 2 maybes and have atleast 2
	 * siblings (excluders) with maybesSize between 2 and $degree (inclusive).
	 * This method differs from above populateCandidatesAndExcluders just in the
	 * minimum number of required excluders. A4+E all require atleast two common
	 * excluder cells to perform exclusion on (otherwise they produce irrelevant
	 * hints), so we start with candidates that have atleast 2 excluders.
	 * <p>
	 * WARNING: I don't understand why this is the case. A real smart bastard
	 * might provide insight leading to an assured solution. I just fixed it,
	 * but I might be full of s__te. It's all I can do.
	 *
	 * @param candidates {@code Cell[]} to (re)populate
	 * @param excludersArray {@code CellSet[81]} to (re)populate
	 * @param grid {@code Grid} to examine
	 * @return numCandidates the number of cells in the candidates array.
	 */
	protected int populateCandidatesAndAtleastTwoExcluders(
			  Cell[] candidates			// output
			, CellSet[] excludersArray	// output
			, Grid grid					// input
	) {
		final int degreePlus1 = this.degreePlus1; // localise field for speed.

		// cell's set of excluder cells. CellSet is a Set<Cell> with reasonably
		// fast contains, remove & clear; but a slow constructor
		// An excluder cell is a sibling with 2..5 maybes and numExcls>0
		CellSet exclsSet = null;
		int card;
		// clear my fields
		MyArrays.clear(candidates);
		MyArrays.clear(excludersArray);
		Cell firstExcluder;

		// build the excluder-sibling-cells-set of each candidate cell
		// foreach cell in grid which has more than one potential value
		int numCandidates = 0;
		CELL_LOOP: for ( Cell cell : grid.cells ) {
			if ( cell.size > 1 ) {
				firstExcluder = null;
				// find cells excluders: ie siblings with maybesSize 2..5
				for ( Cell sib : cell.siblings ) { // 81*20=1620
					// sib is an excluder if it has maybesSize 2..degree
					// optimise: do the < first because it's more deterministic.
					if ( (card=sib.size)>1 && card<degreePlus1 ) {
						if ( firstExcluder == null )
							firstExcluder = sib;
						else if ( exclsSet == null ) {
							exclsSet = new CellSet(); //slow constructor, O(1) contains
							exclsSet.add(firstExcluder);
							exclsSet.add(sib);
						} else
							exclsSet.add(sib);
					// optimize: skip any naked singles
					} else if ( card == 1 )
						continue CELL_LOOP; // skip siblings of naked singles
				}
				if ( exclsSet!=null ) {
					assert exclsSet.size() > 1;
					candidates[numCandidates++]=cell; // candidateCellsArray
					excludersArray[cell.i] = exclsSet; // cellsExcludersMap
					exclsSet = null; // for the next iteration of this loop (ie cell)
				}
			}
		} // next cell in grid // next cell in grid
		return numCandidates;
	}

	// return the bitwise-or of all cells.maybes
	protected static int allMaybesBits(Cell... cells) {
		int all = 0;
		for ( Cell cell : cells )
			all |= cell.maybes;
		return all;
	}

//not used
//	// return a new array of the maybes of each cell
//	protected static int[] getMaybesesNew(Cell[] cells) {
//		final int n = cells.length;
//		int[] maybeses = new int[n];
//		for ( int i=0; i<n; ++i )
//			maybeses[i] = cells[i].maybes;
//		return maybeses;
//	}

	/**
	 * The static subsets method reads the common-excluder-cells.maybes
	 * into an int array, suppressing each excluder which is a superset of any
	 * other/s; so for example if one common excluder maybe 12 and another maybe
	 * 125 then the output (cmnExclBits) will contain just the 12, because 125
	 * "covers" (is a superset of) 12, so the 125 is useless because every set
	 * which covers 125 also covers 12, so it's quicker to leave out the 125
	 * and only test the 12; so we're doing the leaving-out here, to save time
	 * repeatedly testing it in the covers method in the dog____ing loop.
	 * <p>
	 * NB: The larger the set the larger the savings... but this may actually
	 * slow-down A234E, coz the overhead of doing this for every common
	 * excluders set outweighs the savings (which grow exponentially with set
	 * size) resulting from the small percentage of sets that we leave-out.
	 * <p>
	 * NB: if numCmnExclBits is smaller than a previous one then hangover
	 * element/s are retained in cmnExclBits (ie the array is NOT cleared),
	 * which won't matter as long as we use numCmnExclBits (not numCmnExcls)
	 * whenever we reference cmnExclBits.
	 *
	 * @param cmnExclBits int[] output common excluder maybes bits
	 * @param cmnExcls Cell[] input the common excluder cells array.
	 * This method doesn't rely on the order of cmnExcls. Unsorted will do
	 * perfectly, but it's assumed that cmnExcls is pre-sorted ByMaybesSize.
	 * @param numCmnExcls int input the number of common excluder cells
	 * @return numCmnExclBits the number of elements in the cmnExclBits array
	 */
	protected static int subsets(int[] cmnExclBits, Cell[] cmnExcls, int numCmnExcls) {
		int i, candidate, j;
		// add the first common excluder cells maybes bits to cmnExclBits array
		cmnExclBits[0] = cmnExcls[0].maybes;
		int numCmnExclBits = 1; // number of elements in the output (modified) cmnExclBits array
		// foreach subsequent common excluder cell (note the 1)
		OUTER_LOOP: for ( i=1; i<numCmnExcls; ++i ) {
			// get candidate for election into da house of common excluder bits.
			candidate = cmnExcls[i].maybes;
			// foreach existing common excluder bits element
			for ( j=0; j<numCmnExclBits; ++j ) {
				// if this candidate equals da existing cmnExclBits then discard
				// this candidate, because 12&~12 && 12&~12 is bloody pointless.
				if ( cmnExclBits[j] == candidate
				  // if this candidate isSupersetOf da existing cmnExclBits then
				  // discard this candidate, coz every combo which covers this
				  // candidate also covers the excluder, so this candidate won't
				  // gain us any more exclusions, just eat CPU to evaluate in
				  // each "covers" test, which is hammered in the DOG_1 loop.
				  || (cmnExclBits[j] & ~candidate) == 0 )
					continue OUTER_LOOP; // leave-out this candidate
				// if this candidate isSubsetOf da existing cmnExclBits then
				// replace the existing common excluder with this candidate,
				// coz every combo which covers the excluder also covers this
				// candidate, so da excluder won't gain us any more exclusions,
				// just eat CPU to evaluate in each "covers" test, which is
				// hammered in the DOG_1 loop.
				if ( (candidate & ~cmnExclBits[j]) == 0 ) {
					// NB: should NEVER happen coz cmnExclBits is pre-sorted
					//     by size (ie Integer.bitCount), but never say never.
					// NB: typically only developers run java -ea
					assert false : "should NEVER happen!";
					cmnExclBits[j] = candidate;
					continue OUTER_LOOP;
				}
			}
			cmnExclBits[numCmnExclBits++] = candidate;
		}
		return numCmnExclBits;
	}

	/**
	 * The static disdisjunct method removes each common excluder bits (it's an
	 * array of bitsets) that contains a value which is disjunct, ie not in
	 * the maybes of any cell in the aligned set. We do this because no combo
	 * can possibly cover it, so it can't contribute to an exclusion, so it's a
	 * pointless waste of CPU in the dog____ing loop; so it's faster to remove
	 * it here once than it is to repeatedly examine it whilst dog____ing.
	 * This is more true in larger (5+) sets. It's a waste of time in A234E.
	 * <p>
	 * In A234E this method is actually costing us a lick of time, ie it'd be
	 * faster to repeatedly test the occasional disjunct excluder than it is to
	 * remove disjuncts from common excluders of every aligned set; but as the
	 * set-size increases then the number of combos grows exponentially, so
	 * this method becomes more-and-more valuable, and it's the big sets that
	 * are slow, especially A7+E in correct (align on single cell) mode.
	 * <p>
	 * disdisjunct markedly reduces the number of common excluders we have to
	 * dog____ with and thereby the number of dog____ers we execute:<ul>
	 * <li>A7E :  3.40% skipped (a bit of a disappointment).
	 * <li>A8E : 20.79% skipped
	 * <li>A9E : 14.19% skipped
	 * <li>A10E: 14.30% skipped
	 * </ul>
	 * Which expedites things rather nicely.
	 * <p>
	 * The disdisjunct method is always applied, whether or not it actually
	 * saves us anything.
	 * <p>
	 * The name "disdisjunct" is a poor name. If you can think of a better name
	 * then please go ahead and rename it.
	 * <p>
	 * This implementation removes elements from the given cmnExclBits array,
	 * and returns the new numCmnExclBits. There's probably a "better way" of
	 * doing it in OO-land (which costs 1000 times as much), but I'm an old
	 * man, I only measure the performance of my code in nanoseconds (reread
	 * this comment in 30 years time, it'll grow on you), so I can grock it.
	 *
	 * @param cmnExclBits int[] input/output the maybes of the common
	 * excluder cells.
	 * @param numCmnExclBits int input the number of common excluder bits in the
	 * array.
	 * @param allMaybesBits int input the maybes of each cell in the
	 * aligned set. Just bitwise-or them together or use allMaybesBits(). Turns
	 * out it's MUCH faster to have one long or'ism than several smaller ones.
	 * @return the new numCmnExclBits, which may be reduced from the input
	 * numCmnExclBits, as low as 0 (but not negative).
	 */
	protected static int disdisjunct(int[] cmnExclBits, int numCmnExclBits
			, int allMaybesBits) {
		assert numCmnExclBits>0 && allMaybesBits!=0;
		// if allBits is all potential values then we're done, because no
		// reduction of common excluders will be possible.
		if ( allMaybesBits == VALL )
			return numCmnExclBits; // never occurs. Never say never.
		// subtract allBits from each common excluders maybes, and if there
		// are any left-overs then we remove that excluder.
		int i, j;
		for ( i=0; i<numCmnExclBits; ) {
			// if there are any cmnExcl values which are not in allMaybes then
			// this cmnExcls element is useless, so we remove it
			if ( (cmnExclBits[i] & ~allMaybesBits) != 0 ) {
				// remove cmnExclBits[i] by moving each subsequent element up
				// one element, and reducing numCmnExclBits; and we do NOT
				// increment i (the cursor) because we just brought the data
				// down to the cursor instead of the usual: moving the cursor
				// up to the data.
				for ( j=i+1; j<numCmnExclBits; ++j )
					cmnExclBits[j-1] = cmnExclBits[j];
				--numCmnExclBits;
			} else
				++i;
		}
		// return the new, possibly reduced (possibly 0) numCmnExclBits
		return numCmnExclBits;
	}

// The algorithmic complexity of disuselessenate is the same order of magnitude
// as the actual dog____ing loop, hence it doesn't save any time: ie it's just
// as slow to detect "we don't need to ____ this dog" as it is to just go ahead
// and ____ the damn dog, so we just ____ it.
//	/**
//	 * disuselessenate removes each cmnExclBits which doesn't cover a single
//	 * combination of the maybeses: the potential values of the cells in the
//	 * aligned set.
//	 * <p>Performance WARNING: This takes about the same time as doing nothing
//	 * because its complexity is on par with the dog____ing loop. The code has
//	 * been retained for now, though I don't think it's actually used anywhere
//	 * any longer. I dream of speeding it up, but my juju well has run dry with
//	 * the Corona virus muting all hope... for the moment/year/lifetime. Sigh.
//	 * This too will pass. None shall pass! But you're legs off. No it isn't.
//	 * Come on, come on, I'll ave ya. Runnin' away are ya'. IT'S ONLY A RABBIT!
//	 * Yes, but it has sharp gnashing teeth. Run away! Run away! sigh.
//	 * <pre>
//	 * 1. Don't panic.
//	 * 2. Cry if you need to. It helps. I feel that way now. It'll pass.
//	 * 3. Avoid touching your face.
//	 * 4. Wash your hands. Use soap.
//	 * 5. Sneeze into your elbow.
//	 * 6. Stay in touch with friends.
//	 * 7. Avoid large gatherings, especially in confined spaces. Which means
//	 *    stay OFF aircraft. Go local this year... somewhere in the bush!
//	 *    I'd avoid mass gatherings like rock concerts, sporting events, and
//	 *    religious festivals. If you have to go, go... but only if you've
//	 *    worked out how to avoid touching your face, and please clue me in.
//	 * 8. You may need 2 weeks worth of bog roll, long-life milk, coffee, and
//	 *    sugar; and something to chew on might be nice too, but hoarding a
//	 *    dozen crates of canned soup is insanity, unless of course you really
//	 *    really like canned soup. Pumpkin soup again love? Na. This situation
//	 *    requires a measured response. You need some perspective in order to
//	 *    measure your response.
//	 * 9. Don't watch the news. It's depressing. An insane hour of all of the
//	 *    bad and most of the ugly. COVID 19 exists, and it will always exist.
//	 *    Accept that fact. There's no point trying to eliminate the threat.
//	 *    The people pushing this line are hope-mongers, who over-estimate the
//	 *    threat, and under-estimate the impacts of there response to that
//	 *    threat (as classic overreaction). These silly people tend to be the
//	 *    ones who end-up doing silly things like punching-on over bog-rolls,
//	 *    just (usually, and thankfully) less dramatic.
//   * 10. What we can do, through INTELLIGENT action, is SLOW DOWN the spread
//   *    of the virus, so that it doesn't overrun our medicos, destroy our
//	 *    economy, our spirits, and our hope. Virus plus highly mobile populace
//	 *    equals nightmare. The virus is here to stay. The populace is to be
//	 *    maintained. What MUST give is the mobility. Some nightmare is given,
//	 *    but it's when people feel that it's all nothing BUT nightmare that
//	 *    societies crumble, and/because people start doing silly things like
//	 *    punching-on over bloody bog-rolls. Perspective people, please.
//	 * Summary: Don't panic. Hope. Keep calm. Laugh. Think. Prepare. Love.
//	 *    GO to the funerals. Cry. This too will pass. Perspective!
//	 * </pre>
//	 * @param cmnExclBits the common excluder (maybes) bits array
//	 * @param numCmnExclBits the number of elements in the cmnExclBits array
//	 * @param allMaybesBits all the maybes of all the cells in the
//	 * aligned set agglomerated into one bitset.<br>
//	 * eg: {@code c0b|c1b|c2b|(c3b=c3.maybes)}
//	 * @param maybeses the potential values (ie .maybes) of the cells in
//	 * the aligned set
//	 * @return the new (possibly reduced) numCmnExclBits; and also the contents
//	 * of the cmnExclBits array is modified to remove any which cover nada.
//	 */
//	protected static int disuselessenate(final int[] cmnExclBits, final int numCmnExclBits,
//			final int allMaybesBits, final int... maybeses) {
//		final int[][] SVS = SHIFTED;
//		int n = numCmnExclBits;
//		int ceb;
//		int i = 0;
//		MAIN_LOOP: while ( i < n ) {
//			// if the common excluder contains a value that is not all in the
//			// aglomerate of all cells maybes then the common excluder is
//			// history (as per the old disdisjunct method).
//			if ( ((ceb=cmnExclBits[i]) & ~allMaybesBits) != 0 ) {
//				// remove cmnExclBits[i] and decrement n
//				for ( int j=i+1; j<n; ++j)
//					cmnExclBits[j-1] = cmnExclBits[j];
//				--n;
//			} else {
//				// else if the common excluder won't cover a single combination
//				// of the maybes of the cells in the aligned set then the
//				// common excluder is history (my NEW "do it faster" itch).
//				for ( int m=0,M=maybeses.length; m<M; ++m )
//					for ( int collision : SVS[ceb & maybeses[m]] )
//						if ( recursiveCoversAny(ceb & ~collision, except(maybeses, m)) ) {
//							++i; // we need to manually increment the index
//								 // because we don't increment i when we delete
//								 // cmnExclBits[i], instead we pull the next
//								 // element down to i.
//							continue MAIN_LOOP;
//						}
//				// remove cmnExclBits[i] and decrement n
//				for ( int j=i+1; j<n; ++j)
//					cmnExclBits[j-1] = cmnExclBits[j];
//				--n;
//			}
//		}
//		return numCmnExclBits;
//	}
//
//	// returns a-copy-of-array with the element i removed.
//	private static int[] except(final int[] array, final int i) {
//		final int n = array.length;
////		assert i>=0 && i<n;
//		int[] result = new int[n-1];
//		int cnt = 0;
//		// copy the entries to the left of 'i'
//		for ( int j=0; j<i; ++j )
//			result[cnt++] = array[j];
//		// copy the entries to the right of 'i'
//		for ( int j=i+1; j<n; ++j )
//			result[cnt++] = array[j];
////		assert cnt == n - 1;
//		return result;
//	}
//
//	// recursively determine if ceb covers any combo of these maybeses.
//	private static boolean recursiveCoversAny(final int ceb, final int[] maybeses) {
//		if ( ceb == 0 ) // common excluder bits is covered by these maybeses
//			return true;
//		final int[][] SVS = SHIFTED;
//		for ( int m=0,M=maybeses.length; m<M; ++m )
//			for ( int collision : SVS[ceb & maybeses[m]] ) {
//				// pre check this just so we don't call except to create a new
//				// array only to immediately see that ceb==0 and return true.
//				if ( (ceb & ~collision) == 0 )
//					return true;
//				if ( recursiveCoversAny(ceb & ~collision, except(maybeses,m)) )
//					return true;
//			}
//		return false;
//	}

	/**
	 * Does this combo contain ALL of any common excluder cells maybes?
	 * <p>
	 * The static covers method is the implementation of Aligned Set Exclusions
	 * (2) Common Excluders rule: Does the combo (eg (sv0|sv1|sv2|sv3)) cover
     * (ie is a superset of) any cmnExclBits element (ie the maybes of any
     * common-excluder-cell)?
	 * <p>
	 * The name "covers" is just the informal "this set covers this other one",
     * meaning that this combo is a superset of any element of cmnExclBits.
	 * And by "is a superset of" we mean does this combo contain ALL of any
	 * common-excluders-cells.maybes? (nb: there may be left-over combo).
	 * <p>
     * Called by the getHints method of each of my subclasses. This method is
	 * totally hammered, as in Billions with a B:<pre>
	 *     covers true  =  23,046,605,814  12.89%
	 *     covers false = 155,763,312,871  88.11%
	 *	                  178,809,918,685
     * Also note that it returns false 88.11% of the time.
	 * </pre>
	 * NB: Testing has shown that calling a method in the loop is actually
	 * FASTER than doing it all in-line, contrary to previous experiences.
	 * @param cmnExclBits the .maybes of the common excluders cells.
	 * @param numCmnExclBits the number of cmnExclBits elements, ie the number
	 * of elements actually in the cmnExclBits array which is fixed size rather
	 * than build a single-use-array for each combo, to minimise garbage and GC.
	 * @param combo a left shifted bitset (ie the "bits" from a Values set) of
	 * this combination of potential values of the cells in the aligned set.<br>
	 * eg: sv0|sv1|sv2|sv3 for a combo of 4 left-shifted values.
	 * @return does this combo contain ALL of any common excluder cells maybes?
	 */
	protected static boolean covers(final int[] cmnExclBits
			, final int numCmnExclBits, final int combo) {
//++coversCalls;
		for ( int i=0; i<numCmnExclBits; ++i )
			if ( (cmnExclBits[i] & ~combo) == 0 )
				return true;
		return false;
	}

	/**
	 * The createRedPotentials method creates a map of the red (removable)
	 * potential values, from Cell => Values from the given cells and avbs
	 * (allowedValuesBitsets).
	 * @param cells the cells in this aligned set; simultaneous with the avbs
	 * array, ie scells (sortedCells) please peeps, not the raw cells array.
	 * @param avbs int... the allowedValuesBitsets, which is simultaneous with
	 * the cells array.
	 * @return a new Pots containing the excluded values, each in his Cell.
	 * Never null or empty.
	 */
	protected static Pots createRedPotentials(Cell[] cells, int... avbs) {
		int i,n, bits; // index, bits, value
		Pots redPots = new Pots();
		// foreach candidate cell in this aligned set
		for ( i=0,n=cells.length; i<n; ++i )
			// does cell have any maybes that are not allowed at this position?
			if ( (bits=(cells[i].maybes & ~avbs[i])) != 0 )
				// foreach cell maybe that is not allowed
				for ( int v : VALUESES[bits] )
					// Yeah, 'v' can be excluded from 'cell'
					redPots.upsert(cells[i], v);
//		if ( redPots.isEmpty() )
//			Debug.breakpoint();
		assert !redPots.isEmpty(); // asserts are for developers only
		return redPots;
	}

	/**
	 * Count the number of values-in-common between siblings; and also set
	 * siblingsCount to the number of sibling relationships; and also set
	 * totalMaybesSize to the total number of potential values in the given
	 * cells.
	 * @param cells
	 * @return the number of values common to siblings.
	 */
	protected static int countCollisions(Cell[] cells) {
		final int n = cells.length;
		int sib=0, col=0, mbs=0;
		Cell ci, cj;
		for ( int i=0,m=n-1; i<m; ++i ) { // for each cell except the last
			ci = cells[i];
			mbs += ci.size;
			for ( int j=i+1; j<n; ++j ) { // for each subsequent cell
				cj = cells[j];
				if ( !ci.notSees[cj.i] ) {
					++sib;
					col += VSIZE[ci.maybes & cj.maybes];
				}
			}
		}
		totalMaybesSize = mbs + cells[n-1].size;
		siblingsCount = sib;
		return col;
	}
	protected static int totalMaybesSize;
	protected static int siblingsCount;

	/**
	 * Counts the intersections between cmnExclBits and cells.maybes
	 * @param cmnExclBits
	 * @param numCmnExclBits
	 * @param cells
	 * @return
	 */
	protected static int countHits(int[] cmnExclBits, int numCmnExclBits, Cell[] cells) {
		int bits, i, hitCnt = 0;
		for ( Cell cell : cells ) {
			bits = cell.maybes;
			for ( i=0; i<numCmnExclBits; ++i )
				hitCnt += VSIZE[cmnExclBits[i] & bits];
		}
		return hitCnt;
	}

	/**
	 * Sort n cells by maybesSize ASCENDING. Smaller cells to the left.
	 * @param cells
	 * @param n
	 */
	protected static void bubbleSort(Cell[] cells, int n) {
		int i, j, k;
		boolean any;
		Cell tmp;
		for ( i=n; i>1; --i ) { // the right cell EXCLUSIVE
			any = false;
			for ( k=1; k<i; ++k ) // the left cell INCLUSIVE
				// if previous (j=k-1) is larger than current (k) then swapem
				if ( cells[j=k-1].size > cells[k].size ) {
					tmp = cells[j];
					cells[j] = cells[k];
					cells[k] = tmp;
					any = true;
				}
			if ( !any )
				break;
		}
	}

	/**
	 * Sort n cells by cc
	 * @param cells the cells to sort
	 * @param n the number of cells to sort
	 * @param cc {@code Comparator<Cell>}
	 */
	protected static void bubbleSort(Cell[] cells, int n, Comparator<Cell> cc){
		int i, j, k;
		boolean any;
		Cell tmp;
		for ( i=n; i>1; --i ) { // the right cell EXCLUSIVE
			any = false;
			for ( k=1; k<i; ++k ) // the left cell INCLUSIVE
				// if prev (j=k-1) is larger than curr (k) then swap them
				// compare returns: -1 if a<b; 0 if a==b; or 1 if a>b.
				if ( cc.compare(cells[j=k-1], cells[k]) > 0 ) {
					tmp = cells[j];
					cells[j] = cells[k];
					cells[k] = tmp;
					any = true;
				}
			if ( !any )
				break;
		}
	}

	// ========================================================================

	/**
	 * This {@code Comparator<Cell>} makes the most useful cells leftmost.
	 * Basically we're just making the dog____ing loop faster by shifting the
	 * continues (ie the skips) up that deep-assed nest of for-loops.
	 * This base-class implements the stuff that can be implemented universally,
	 * any specific stuff goes in an extension of this class in your subclass.
	 * The empty reject() and hit() are for this purpose. You write them.
	 * You call them. Your problem.
	 *
	 * This class currently used in A5678910E _1C & _2H
	 */
	protected static class ACollisionComparator implements Comparator<Cell> {

		// scores field is primarily for use the compare(...) method.
		// it's a sparse array, concurrent with Grid.cells (ie Cell.i)
		// protected coz I can't think of good reason to deny reject() method.
		protected final int[] scores = new int[81];
//These fields no longer used. Delete them and associated code if you like.
//		// protected for access by the reject() method.
//		protected int siblingCount = 0;
//		protected int ttlScore = 0;
//		protected int maxScore = 0;
//		protected int minScore = Integer.MAX_VALUE;

		/**
		 * The setAndFilter method calculates a score of:<ul>
		 * <li>4 * number of maybes collisions of each cell in the aligned set
		 * <li>plus 2 for each collision with common excluders
		 * <li>plus 1 for each maybe
		 * </ul>
		 * To make the most useful cells the leftmost.
		 * @param cells the cells in the aligned set.
		 * @param cmnExclBits common excluder bits: the maybes of the
		 * common excluder cells, with any subsets and disjuncts removed (not
		 * that matters here).
		 * @param numCmnExclBits number of common excluder bits
		 */
		void set(Cell[] cells, int[] cmnExclBits, int numCmnExclBits) {
			// local all field references for speed
			int[] myScores = this.scores;
			// create my variables
			final int m = cells.length - 1; // m is the one before n
			Cell a, b; // cells[i], cells[j]
			boolean[] aIsNotSiblingOf; // Cell a's isNotSiblingsOf array
			int ai, j, bits, score;
//			int sibCnt=0, ttlSc=0, minSc=Integer.MAX_VALUE, maxSc=0;
			// clear my fields
			MyArrays.clear(myScores);
			// calculate my values and set my fields, especially scores[]
			for ( int i=0; i<m; ++i ) { // foreach cell, except the last
				bits = (a=cells[i]).maybes;
				aIsNotSiblingOf = a.notSees;
				ai = a.i;
				for ( j=m; j>i; --j ) { // foreach subsequent cell (backwards, coz it's faster)
					if ( !aIsNotSiblingOf[(b=cells[j]).i] ) {
//						++sibCnt;
						// score = 4 for each value that a & b have in common
						score = VSIZE[bits & b.maybes] << 2;
						// add score to both a & b, because each sibling
						// relationship is counted only once.
						myScores[ai] += score;
						myScores[b.i] += score;
					}
				}
			}
			for ( Cell cell : cells ) {
				// add 1 for each maybe
				// I'm not real sure about this. To filter I think we should
				// leave out the maybesSize, but to sort I think we should
				// leave it in, so we either have two scores or we make the
				// best of it with a single score, and I suspect that sorting
				// is more potent than filtering, so sorting has precedence.
				score = myScores[cell.i] + cell.size;
				// add 2 for each collision with cmnExclBits
				bits = cell.maybes;
				for ( int i=0; i<numCmnExclBits; ++i )
					score += VSIZE[bits & cmnExclBits[i]] << 1;
				// set my local variables
				myScores[cell.i] = score;
//				ttlSc += score;
//				if(score>maxSc) maxSc=score;
//				if(score<minSc) minSc=score;
			}
//			// set my fields (ONCE)
//			siblingCount = sibCnt; // set field
//			ttlScore = ttlSc; // set field
//			maxScore = maxSc;
//			minScore = minSc;
		}

		@Override
		public int compare(Cell a, Cell b) {
			return Integer.compare(scores[b.i], scores[a.i]); // DESC
		}

		/**
		 * You implement reject to return true if your A*E's getHints method
		 * should skip this aligned set, else false; using the fields {scores,
		 * siblingCount, totalScore, maxScore, minScore} that where set by the
		 * most recent invocation of my set method. This is a pre-negated accept
		 * method.
		 * @return true if you should skip this aligned set, else false.
		 */
		boolean reject() {
			return false;
		}

		/**
		 * You override hit to update the hit counters in your various Counter's,
		 * and then your A*E's getHints method calls me (ONCE and ONCE only)
		 * whenever a hint is produced.
		 */
		void hit() {
		}

		/**
		 * You override report to report on your hit counters or whatever.
		 */
		void report() {
		}

	}

	// =========================================================================

	/**
	 * A map of the excluded combos => the optional lockingCell.
	 * A null lockingCell means "one instance of value per region".
	 * <p>Extends a LINKEDHashMap because order is significant to user following
	 * the resulting hint.
	 * <p>The put method is overridden to ignore attempts to add a key-array
	 * which does NOT contain one of the red (removable) values of the hint
	 * we're building. This is a tad slow, but speed doesn't really matter coz
	 * there's so few hints, and it's the most succinct way I can think of.
	 * Turns out this override costs ____all time, it might even be saving us
	 * a bit, I guess the relevance check is faster than extraneous puts where.
	 */
	protected static final class ExcludedCombosMap extends LinkedHashMap<HashA, Cell> {
		private static final long serialVersionUID = 245566510L;
		private final Cell[] cells;
		private final Pots redPots;

		/**
		 * Constructs a new ExcludedCombosMap whose put method will use the
		 * AlignedExclusionHint.isRelevant(cells, redPots, key.array) method
		 * to determine if the combo is relevant to the hint, thus hopefully
		 * avoiding ALL those annoying irrelevant hints. I need to test this
		 * now to see if it's much slower. We create 5,318 maps in for top1465,
		 * so I can't see performance being much of an issue.
		 * @param cells
		 * @param redPots
		 */
		ExcludedCombosMap(Cell[] cells, Pots redPots) {
			super(128, 0.75F);
			this.cells = cells;
			this.redPots = redPots;
		}
		/**
		 * Overridden to ignore irrelevant values (which aren't in the redBits
		 * bitset that was passed to my constructor).
		 *
		 * <p><b>Enhancement:</b> now alternately uses the Cell[] and Pots from
		 * the preferred constructor to call AlignedExclusionHint.isRelevant,
		 * so that we can finally eradicate those annoying irrelevant hints.
		 * <b>When</b> all usages of the deprecated constructor are eliminated
		 * please remove the cells==null branch of this method, and the
		 * deprecated constructor, and it's field/s.<br>
		 * FYI: compile with -Xlint to find all usages of deprecated methods.
		 * In Netbeans: right-click on the project ~ Properties ~ Build /
		 * Compiling ~ Additional Compiler Options: paste in -Xlint
		 * @param key HashA to put
		 * @param value Cell the locking cell (aka excluding cell), or null
		 * meaning "only one instance of value is allowed per region".
		 * @return null if none of the values in key are relevant, else the
		 * previous value (Cell) associated with this HashA (which should NEVER
		 * exist), else null meaning none.<br>
		 * This return value isn't used. It'd be a pig to use because nulls are
		 * ambiguous. If you need a real return value then go ahead change this
		 * code to make it useful for your circumstances. Make it return a new
		 * BarbequedOccelotsSpleen for all I care. Doataise sauce costs extra.
		 */
		@Override
		public Cell put(HashA key, Cell value) {
			if ( !AlignedExclusionHint.isRelevent(cells, redPots, key.array) )
				return null;
			return super.put(key, value);
		}

		public LinkedHashSet<Cell> getUsedCommonExcluders(Cell cmnExcls[], int numCmnExcls) {
			final LinkedHashSet<Cell> set = new LinkedHashSet<>(16, 0.75f);
			for ( Cell c : super.values() )
				if ( c != null )
					set.add(c);
			if ( set.isEmpty() )
				for ( int i=0; i<numCmnExcls; ++i )
					set.add(cmnExcls[i]);
			return set;
		}
	}

	// =========================================================================

	/**
	 * Total maybes known to NOT hint. totalMaybes relies on maybes only being
	 * removed from the cells (never added back-in or anything) so that anytime
	 * any of the maybes in any of the cells is removed the total changes, so
	 * we must re-examine these cells. If everything is the same as it was last
	 * time and we did not hint the last time then it's safe to assume that we
	 * won't hint this time, so its safe to skip this search, and hopefully
	 * save some time. A9E currently takes about a DAY (ie far too long to be
	 * practical), so I try to speed it up.
	 * @author Keith Corlett 2020-12-06 (IIRC) created
	 */
	protected static class NonHinters {
		// IntIntHashMap does NOT grow so its size really matters.
		// Observed 84 thousand in A8E: FMS BIG. No wonder it takes for ever.
		private int hc; // hashCode
		private int mb; // total maybes
		private final IntIntHashMap totalMaybes;
		private final int shift;

		/**
		 * Construct a new NonHinters.
		 *
		 * @param capacity the size of the totalMaybes IntIntHashMap.
		 * @param shift the number of bits to left-shift the hashCode.<br>
		 *  Left-shifting 3 caters for 8 cells in a set: 3 * 8 = 24 and
		 *  cell.hashCode is 8 bits, so 24 + 8 = 32 = perfect.<br>
		 *  NOTE: hashCodes are usually INTENDED to be lossy, but here we rely
		 *  totally on the hashCode, so any collisions are ACTUAL collisions;
		 *  ie two distinct sets of cells which produce the same hashCode are
		 *  treated as one, for speed.
		 */
		public NonHinters(int capacity, int shift) {
			totalMaybes = new IntIntHashMap(capacity);
			this.shift = shift;
		}

		/**
		 * Can we skip searching this combo, ie are these cells (in there
		 * current state) already known to NOT hint.
		 * <p>
		 * By "current state" I mean we must recheck each set of cells each
		 * time any of the maybes in any of those cells is removed, so all we
		 * do is total the maybes (a bitset), which is a bit cheeky but
		 * works because maybes are only ever removed from the cells (ie they
		 * are never added back-in) so a total of the maybes is sufficient to
		 * workout if a maybe has been removed from any of the cells in this
		 * set since the last time we examined them... so the total maybes of
		 * a set of cells can and does serve as its modification count.
		 * <p>
		 * The result is that each combo that doesn't hint is checked ONCE.
		 * <p>
		 * Note that we rely upon the IntIntHashMap class KRC wrote for HoDoKu,
		 * which is a (simplified) {@code HashMap<int, int>} so we do not need
		 * to create millions of Integers, so it's a bit faster.
		 * <p>
		 * 2020-12-07 tried skipping the skipper on the first pass of the grid,
		 * where we (pretty obviously) never skip, but it was actually slower.
		 *
		 * @param cells
		 * @return
		 */
		public boolean skip(Cell[] cells) {
			int storedMb, hc=0, mb=0;
			for ( Cell cell : cells ) {
				// left-shifting 3 caters for 8 cells in a set: 3 * 8 = 24 and
				// cell.hashCode is 8 bits, so 24 + 8 = 32 = perfect.
				hc = (hc<<shift) ^ cell.hashCode;
				mb += cell.maybes;
			}
			this.hc = hc;
			this.mb = mb;
			// return: are these cells, with these maybes, known to NOT hint
			return (storedMb=totalMaybes.get(hc)) != IntIntHashMap.NOT_FOUND
				&& mb == storedMb; // then skip it coz we know it doesnt hint.
		}

		// put is called after get (when we do NOT hint) coz either hc is not
		// already in the map or the cells maybes have changed so that totalMb
		// != storedMb, so we update the storedMb with the totalMaybes (mb).
		public void put() {
			totalMaybes.put(hc, mb);
		}

		public void clear() {
			totalMaybes.clear();
		}
	}

	// =========================================================================

	/**
	 * A LongLongHashMap version of NonHinters to speed-up A9E and A10E. Each
	 * cells hashCode (ie its identity) is 8 bits so when we smash 10 * 8 = 80
	 * into 32 bits the result is FAR too indistinctive, so the hashCode has
	 * been expanded out to a 64 bit long (and therefore so has the value
	 * despite it fitting comfortably in a 32 bit int); so each cell.hashCode
	 * is left-shifted 4 bits, so that they tend to stay distinctive. If there
	 * is a collision then both combos in the collision will be rechecked
	 * unless they (by pure happenstance) have the same totalMaybes, which may
	 * happen, but only about 1 in 10000, I guess. 1 in 10000 means it almost
	 * certainly won't drop a hint, coz they're rare to start with. sigh.
	 * <p>
	 * It'd be well funny if aligned sets with collisions turned out to have a
	 * much higher than average hint-rate. There's one for the mathematicians.
	 */
	public static class NonHinters64 {
		private long hc; // hashCode
		private long mb; // total maybes
		private final LongLongHashMap totalMaybes;
		private final long shift;

		/**
		 * Construct a new NonHinters.
		 *
		 * @param capacity the size of the totalMaybes LongLongHashMap.
		 * @param shift the number of bits to left-shift the longHashCode.<br>
		 *  Left-shifting 4 caters for 8 cells in a set: 3 * 8 = 24 and
		 *  cell.hashCode is 8 bits, so 24 + 8 = 32 = perfect.<br>
		 *  NOTE: hashCodes are usually INTENDED to be lossy, but here we rely
		 *  totally on the hashCode, so any collisions are ACTUAL collisions;
		 *  ie two distinct sets of cells which produce the same hashCode are
		 *  treated as one, for speed.
		 */
		public NonHinters64(int capacity, long shift) {
			totalMaybes = new LongLongHashMap(capacity);
			this.shift = shift;
		}

		/**
		 * Can we skip searching this combo, ie are these cells (in there
		 * current state) already known to NOT hint.
		 *
		 * @param cells
		 * @return
		 */
		public boolean skip(Cell[] cells) {
			long storedMb, mb=0;
			long hc=0;
			for ( Cell cell : cells ) {
				// left-shifting 3 caters for 8 cells in a set: 3 * 8 = 24
				// and cell.hashCode is 8 bits, so 24 + 8 = 32 = perfect.
				hc = (hc<<shift) ^ cell.hashCode;
				mb += cell.maybes;
			}
			this.hc = hc;
			this.mb = mb;
			// return: are these cells, with these maybes, known to NOT hint
			return (storedMb=totalMaybes.get(hc)) != LongLongHashMap.NOT_FOUND
				&& mb == storedMb; // then skip it coz we know it doesnt hint.
		}

		// put is called after skip if this combo does not hint (ie 99.93% of
		// cases) to update the old totalMb (if any) to the current total.
		public void put() {
			totalMaybes.put(hc, mb);
		}

		public void clear() {
			totalMaybes.clear();
		}
	}

}
