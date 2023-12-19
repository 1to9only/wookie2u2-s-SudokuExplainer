/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

// Sudoku Explainer (local) classes
// Tip: .* import local classes, but NEVER foreigners.
// You cannot change foreign names to resolve collisions.
import diuf.sudoku.solver.accu.ValidatingHintsAccumulator;
import diuf.sudoku.*;
import static diuf.sudoku.Constants.beep;
import static diuf.sudoku.Grid.GRID_SIZE;
import diuf.sudoku.io.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.utils.*;
import static diuf.sudoku.utils.Frmt.*;
import static diuf.sudoku.utils.MyStrings.BIG_BFR_SIZE;
import java.io.Closeable;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Predicate;
import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.SB;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.MAYBES_STR;
import static diuf.sudoku.Values.VSHFT;
import java.util.List;
import static diuf.sudoku.solver.Print.printGridFull;
import static diuf.sudoku.solver.Print.printHintFull;

/**
 * A LogicalSolver solves Sudoku puzzles using logic, and does so as quickly as
 * possible (as I am able). The challenge of doing this is what the whole Sudoku
 * Explainer project is about.
 * <p>
 * This version of Sudoku Explainer was written "just for fun" and the author
 * (Keith Corlett) feels he must humbly apologise for producing rather a lot of
 * code which is pretty hard to maintain because it does NOT, by design, follow
 * the edicts of any modern Java coding ethos. It is written to run fast, not to
 * be maintainable. Sorry about that.
 * <p>
 * If you are impatient then in the GUI (Options ~ Solving Techniques):<ul>
 * <li>keep means ticked, and drop means unticked. If it is not listed then you
 *  had better err on the side of caution and keep it. All current hinters are
 *  listed, so it will only be new hinters that could be missing.
 * <li>keep (tick) Locking (fast) instead of Locking Generalised (slower)
 * <li>drop (untick) Direct* for speed, because they are found anyway by there
 *  non-direct equivalents. Direct just sets the cell directly, that is all.
 * <li>keep Naked/Hidden Pair/Triple, Swampfish, Two String Kite, XY-Wing,
 *  XYZ-Wing, W-Wing, Swordfish, Skyscraper, and Empty Rectangle
 * <li>drop Jellyfish finds none if you follow this spec to the letter.
 *  In fact all *Jellyfish are pretty slow, so drop them all for speed.
 * <li>keep Coloring (only Multi), XColoring (GEM misses), and GEM (ultimate)
 * <li>keep Naked/Hidden Quad
 * <li>drop Naked/Hidden Pent they are degenerate, which means all patterns they
 *  found are found earlier by simpler techniques, so they find nothing
 * <li>keep BigWings (slower than S/T/U/V/WXYZ-Wing but faster overall because
 *  most of it is runtime is building the ALS cache that is reused by AlsXz,
 *  AlsWing and AlsChain) If you drop Als* then swap back to T/U/V/WXYZ-Wing,
 *  but I'd still drop the STUVWXYZ-Wing coz it is a little beet slow. But hey,
 *  it only took three hours to rub one out last Tuesday! Just kidding.
 * <li>drop WXYZ-Wing, VWXYZ-Wing, UVWXYZ-Wing, TUVWXYZ-Wing
 * <li>drop STUVWXYZ-Wing because it is a bit slow (border-line)
 * <li>keep URT (Unique Rectangles and Loops)
 * <li>keep FinnedSwampfish and FinnedSwordfish
 * <li>drop FinnedJellyfish for speed because it is a bit slow (border-line)
 * <li>keep ALS-XZ, Als-Wing, and Als-Chain they are all fast enough now
 * <li>keep DeathBlossom barely faster than slow
 * <li>drop SueDeCoq is a tad slow (border-line)
 * <li>drop Franken* for speed: Finned* is faster than Franken*, and<br>
 *  FrankenSwampfish is degenerate to FinnedSwampfish.<br>
 *  FrankenSwordfish is a bit slow.<br>
 *  FrankenJellyfish is slow.
 * <li>drop Krakens* (slow) especially KrakenJellyFish (too slow)
 * <li>drop Mutants* (slow) especially MutantJellyFish (far too slow)
 * <li>drop Aligned*Exclusion (slow); A7+E are far too slow and need shooting!
 * <li>keep UnaryChain, NishioChain, and MultipleChain are all needed. If you
 *  keep Kraken* you can drop UnaryChain because they seek the same pattern
 *  via different routes, but UnaryChain is faster. Kraken=nuts IMHO.
 *  Nishio is border line; they are all found (arguable faster) by Multiple.
 * <li>keep DynamicPlus as a catch-all for normal people, and/or NestedUnary
 *  as the catch-all for the <u>hardest</u> possible Sudoku puzzles
 * <li>drop the rest of Nested*. They are never called in anger, so it does not
 *  matter til you Shft-F5 to find MORE hints. They take distant monarch ages.
 * <li>My definition of "slow" is 100 milliseconds per elimination
 * <li>My definition of "too slow" is a second per elimination
 * <li>My definition of "far too slow" is about 10 seconds per elimination
 * <li>All of these hinters are as fast as I know how to make them, which is
 *  pretty fast, except the A*E debacle, but I cannot preclude the possibility
 *  that it can be done faster. Have a go if you are smart and have a few years
 *  to waste, but I warn you that gambling is addictive, so best left for
 *  retirement.
 * </ul>
 * <p>
 * <b>LogicalSolver is used (by both the GUI and the batch) to:</b><ul>
 *  <li>Check the validity of a Sudoku puzzle (ie a grid).
 *  <li>Get the simplest available hint (a Sudoku solving step) in a grid.
 *  <li>Get all available hints in a grid, including wanted chainers and
 *   nested-chainers.
 *  <li>Solve a Sudoku puzzle logically to calculate its difficulty rating,
 *   and get a summary list of the hint-types (usages) that are required.
 * </ul>
 * <p>
 * <b>Packaging:</b><ul>
 * <li>Validators are implemented in the {@code diuf.sudoku.solver.checks}
 *  package. Validity is checked ASAP because it is quicker to fail fast.
 * <li>The solving techniques are under the {@code diuf.sudoku.solver.hinters}
 *  package. There is too many to list, so I mention the odd-balls only: <ul>
 *  <li>{@link diuf.sudoku.solver.hinters.als.BigWings} finds all variants of
 *   {@link diuf.sudoku.solver.hinters.wing.BigWing} in one hit. Note well that
 *   wing.BigWing finds its own ALSs and predates als.BigWings which shares
 *   cached ALSs with the other ALS hinters. BigWing is faster outright, but
 *   BigWings is faster overall because it populates the ALS cache shared with
 *   other hinters, presuming that you want the other ALS hinters. sigh.
 *  <li>The sdp package contains SingleDigitPattern (donor HoDoKu class name)
 *   hinters: EmptyRectangle, SkyScraper, and TwoStringKite
 *  </ul>
 * <li>The hints-accumulators are in the {@code diuf.sudoku.solver.accu}
 *  package.
 * <li>All read/writing of puzzles is in the diuf.sudoku.io package.
 * <li>The GUI is in the {@code diuf.sudoku.gui} package.
 * <li>The Sudoku puzzle generator is in the {@code diuf.sudoku.gen} package.
 * <li>There is a bunch of crap in the utils package. Some of it might even be
 *  worth re-using.
 * </ul>
 * <p>
 * <b>Stolen Solvers:</b><ul>
 * <li>Sudoku Explainer includes many Sudoku solving techniques stolen from
 *  several other open-source Sudoku solvers, namely HoDoKu, Sukaku,
 *  SudokuMonster, and even the sudopedia.org website.
 * <li>The original Sudoku Explainer was heavily dependent on Chainers, so I
 *  went and stole some Solving Techniques. Never write anything from scratch,
 *  unless you have to. In the GUI each such hint is denoted by "$SourceApp" at
 *  the start of its name. Kudos goes to original authors, especially hobiwan
 *  (HoDoKu). Mistakes are all mine.
 * </ul>
 * <b>On HoDoKu:</b><ul>
 * <li>I recommend HoDoKu over SE for "normal users". My release of HoDoKu is a
 *  bit faster, especially for expert puzzles (nets). HoDoKu has a nicer GUI. I
 *  am not a GUI-programmers-asshole, and I know it. My stich is efficiency, so
 *  I fit better on the server-side.
 * <li>Bernhard Hobiger (hobiwan) is my hero because he has a cool moniker, and
 *  because he is/was a freeken genius. KrakenFisherman was based on hobiwans
 *  ideas, and it is <b>seriously</b> smart. Dont get me wrong, Kraken is s__t,
 *  but the code is smart.
 * <li>here is a list of all the hinters boosted from HoDoKu:<ul>
 *  <li>{@link diuf.sudoku.solver.hinters.sdp.EmptyRectangle}
 *  <li>{@link diuf.sudoku.solver.hinters.sdp.Skyscraper}
 *  <li>{@link diuf.sudoku.solver.hinters.sdp.TwoStringKite}
 *  <li>{@link diuf.sudoku.solver.hinters.wing.WWing}
 *  <li>{@link diuf.sudoku.solver.hinters.color.Coloring}
 *  <li>{@link diuf.sudoku.solver.hinters.als.AlsXz}
 *  <li>{@link diuf.sudoku.solver.hinters.als.AlsXyWing}
 *  <li>{@link diuf.sudoku.solver.hinters.als.AlsXyChain}
 *  <li>{@link diuf.sudoku.solver.hinters.aals.SueDeCoq}
 *  <li>{@link diuf.sudoku.solver.hinters.als.DeathBlossom}
 *  <li>{@link diuf.sudoku.solver.hinters.fish.ComplexFisherman}
 *   for Finned, Franken, and Mutant fish.
 *  <li>{@link diuf.sudoku.solver.hinters.fish.KrakenFisherman}
 *   for Kraken bananas, and fish, easy on the bananas.
 *  </ul>
 * </ul>
 * <b>On Sukaku:</b><ul>
 * <li>Some of the hinters here-in draw from Sukaku.
 *  The old {@link diuf.sudoku.solver.hinters.wing.BigWing} was from Sukaku.
 *  His replacement is {@link diuf.sudoku.solver.hinters.als.BigWings}.
 *  Both implementations are now about a hundred times faster than Sukakus, and
 *  BigWings is a LOT less code. Sukakus code is poor quality. I decommend it.
 * </ul>
 * <b>On sudopedia.org:</b><ul>
 * <li>Some of the hinters here-in draw from sudopedia.org which IMHO is the
 *  best free information source for Sudoku solving. The prime example being
 *  a specification for {@link diuf.sudoku.solver.hinters.color.GEM}.
 * </ul>
 * <b>On SudokuMonster:</b><ul>
 * <li>Some of the hinters here-in draw from SudokuMonster, but code was s__t,
 *  except {@link diuf.sudoku.solver.hinters.lock.LockingGen} is pure genius.
 *  Go figure!
 * </ul>
 * <p>
 * <b>Automated tests:</b><ul>
 * <li>There are {@code JUnit} tests in {@code ./test} directory, with the
 *  same packaging there-under as the class being tested; as per the JUnit
 *  and therefore NutBeans standard practice.<br>
 *  <br>
 *  These are tests for correctness, using the minimum amount of the codebase
 *  required to test the examined feature. The majority of tests are on a
 *  subtype-of-AHinter. They check that a given puzzle produces the expected
 *  hint, with the given hint-HTML. Keeping the expected-HTML files up-to-date
 *  is a pain-in-the-ass, but I its worth it. These tests have found bugs.<br>
 *  <br>
 *  In Nutbeans: run all JUnit tests with ALT-F6. They take about 30 seconds.
 *  These are mostly regression tests, diffing the output to a saved dump/html,
 *  so anytime you change just about anything you will also need to update the
 *  test-case. These tests pick-up any unintended consequences. Some classes do
 *  NOT have test-cases, coz I am lazy/stupid. Its a work in progress.
 * <li>And then I performance test {@link diuf.sudoku.test.LogicalSolverTester}
 *  on {@code top1465.d5.mt}, which is in the root directory of this project.
 *  This currently takes 2 to 3 minutes to run, depending on hinter selection.
 * </ul>
 * <p>
 * @see diuf.sudoku.BuildTimings for batch timings over top1465.
 */
public final class LogicalSolver {

	// should the validate method throw an exception or just mark invalid hints
	private static final boolean VALIDATE_THROWS = true; // @check true

	/**
	 * Returned by {@link #analyseDifficulty(diuf.sudoku.Grid, int) }
	 * when the process was interrupted by the user.
	 */
	public static final int ANALYSE_INTERRUPTED = 99999; // an impossibly high number

	/**
	 * The ANALYSE_LOCK synchronises {@link diuf.sudoku.gen.PuzzleCache}
	 * with {@link LogicalAnalyser}, so that one solve runs at a time.
	 */
	public static final Object ANALYSE_LOCK = new Object();

	// NOTE: We must create the basic hinters before the validators.
	// First the singles: NakedSingle and HiddenSingle;
	// then Four Quick Foxes: Locking, NakedPair, HiddenPair and SwampFish.
	// then BruteForce (a validator) which uses the basic hinters
	// NOTE: Locking uses HiddenPair and HiddenTriple.

	/**
	 * A cell with only one potential value remaining is that value.
	 */
	private final NakedSingle nakedSingle;

	/**
	 * The only place in a region which maybe a value is that value.
	 */
	private final HiddenSingle hiddenSingle;

	/**
	 * If the only cells in a region which maybe v also share another common
	 * region then we can claim v from that other region. I use "claim" to mean
	 * one of these cells must be this value (ie these cells are a Locked Set),
	 * eliminating v from all other cells in the region.
	 * <p>
	 * In practice: <ul>
	 * <li>Pointing: If all cells which maybe v in box 1 are also in row 1
	 * then claim v from row 1; and
	 * <li>Claiming: If all cells which maybe v in row 1 are also in box 1
	 * then claim v from box 1.
	 * <ul>
	 * NOTE: the delineation between pointing and claiming is arbitrary, as the
	 * initial rule statement alludes to. The Locking class delineates, but
	 * LockingGen does not. It is a more elegant solution, that is slower.
	 */
	private final Locking locking;

	/**
	 * siameseLocking used in the GUI, optional in LogicalSolverTester.
	 */
	private final SiameseLocking siameseLocking;

	// NOTE: validators are created AFTER the hinters coz BruteForce uses
	// the Four Quick Foxes {NakedSingle, HiddenSingle, Locking, Swampfish}
	// to solve logically, which is faster than just guessing cell-values
	// that can be quickly proven invalid.

	/**
	 * puzzle validator: Does this puzzle have at least 17 clues?.
	 */
	private final TooFewClues tooFewClues = new TooFewClues();

	/**
	 * puzzle validator: Does this puzzle contain 8 values?.
	 */
	private final TooFewValues tooFewValues = new TooFewValues();

	/**
	 * puzzle validator: Does this puzzle have ONE only solution? <br>
	 * NB: BruteForce uses the basic hinters, for speed.
	 */
	private final BruteForce bruteForce;

	/**
	 * puzzleValidators check the puzzle conforms to the Sudoku rules ONCE.
	 */
	private final IHinter[] puzzleValidators;

	/**
	 * gridValidators check the Sudoku grid is still valid after each hint.
	 */
	private final IHinter[] gridValidators = new IHinter[] {
		  new DoubleValues()	 // one value per box, row, col
		, new MissingMaybes()	 // no value and no maybes
		, new HomelessValues() // no places for unplaced value
	};

	/**
	 * All of the validators.
	 */
	private final IHinter[] validators;

	/**
	 * wantedHinters is an array of IHinter which the user currently wants.
	 * <p>
	 * An array coz native-array-iterator is MUCH faster than an Iterator.
	 */
	public IHinter[] wantedHinters;

	/**
	 * 'unwanted' Techs, for reporting only.
	 */
	private EnumSet<Tech> unwanted;

	/**
	 * 'unconstructable' Techs, for reporting only.
	 */
	private EnumSet<Tech> errors;

	/**
	 * Solve and apply both detect deadCats: hints which do not set a Cell and
	 * and do not remove any maybes. If a deadCat is seen twice then presume we
	 * are at the start of an infinite loop in solve/generate/whatever (finding
	 * the same eliminationless hint endlessly) so disable the hinter, which is
	 * re-enabled at the start of the next puzzle by prepare.
	 * nb: Funky means that (unlike java.util.Set impls) add is addOnly, which,
	 * by design, does NOT update existing elements before returning false.
	 */
	private final Set<AHint> deadCats = new MyFunkyLinkedHashSet<>();

	/**
	 * The Sudoku has been solved (a generic message).
	 */
	public final AHint solvedHint = new SolvedHint();

	/**
	 * A set of "basic hinters" (singles + Four Quick Foxes).
	 */
	private final Map<Tech,IHinter> basicHinters;

	/**
	 * The singles are weird: a side-effect of {@link #solveWithSingles},
	 * read by {@link diuf.sudoku.gui.SudokuExplainer#getNextHint} et al.
	 */
	public Grid singles;

	// a cache to speed-up getWantedHinter
	private final EnumMap<Tech,IHinter> hinterCache = new EnumMap<>(Tech.class);

	private boolean anyDisabled;

	private LinkedList<IPrepare> preppersCache; // defaults to null

	private Cheats cheats;

	/**
	 * Constructor sets-up this LogicalSolver to solve logically. All the hard
	 * work is done by {@link LogicalSolverBuilder#build} before I am called.
	 * <p>
	 * NOTE: the bruteForce validator uses the "basic hinters" so it is creation
	 * is delayed until this constructor, which in turn delays the creation of
	 * everything that uses bruteForce.
	 *
	 * @param basics are {NakedSingle, HiddenSingle, Locking, NakedPair,
	 *  HiddenPair, Swampfish} are created even if not wanted, for use in
	 *  any/all of LogicalSolver, BruteForce, and the chainers
	 * @param wantedTechs Techs that are wanted by the user.
	 * @param wantedHinters IHinters that are wanted by the user.
	 * @param unwanted the unwanted techs, for reporting only
	 * @param errors the techs we failed to construct, for reporting only
	 */
	LogicalSolver(
		  final Map<Tech,IHinter> basics
		, final IHinter[] wantedHinters
		, final EnumSet<Tech> unwanted
		, final EnumSet<Tech> errors
	) {
		this.basicHinters = basics;
		this.wantedHinters = wantedHinters;
		this.unwanted = unwanted;
		this.errors = errors;
		// weird: each hinter needs to know its index in wantedHinters,
		// so that the batch can log hinters in execution order.
		for ( int i=0,n=wantedHinters.length; i<n; ++i )
			wantedHinters[i].setArrayIndex(i);
		// bruteForce uses the "basic hinters", so it is creation is delayed,
		// which in turn delays creation of all dependant artifacts. sigh.
		bruteForce = new BruteForce(basics);
		puzzleValidators = new IHinter[] {
			  tooFewClues		// minimum 17 clues
			, tooFewValues		// minimum 8 values
			, bruteForce		// ONE solution
		};
		validators = new IHinter[] {
			  puzzleValidators[0], puzzleValidators[1], puzzleValidators[2]
			, gridValidators[0],   gridValidators[1],   gridValidators[2]
		};
		// set-up individual hinters
		nakedSingle = (NakedSingle)basics.get(Tech.NakedSingle);
		hiddenSingle = (HiddenSingle)basics.get(Tech.HiddenSingle);
		locking = (Locking)basics.get(Tech.Locking);
		siameseLocking = new SiameseLocking(basics);
		siameseLocking.arrayIndex = locking.arrayIndex; // not used if 0 (unwanted)
	}

	// ------------------------ logConfigurationReport ------------------------

	/**
	 * Lick that one Scoobie!
	 *
	 * @return CSV of the wanted enabled hinters for logging
	 */
	private String getWantedEnabledHinterNames() {
		try {
			return Frmt.appendTo(
			  SB(512).append("WantedEnabled: ")
			, wantedHinters
			, (IHinter h) -> h.isEnabled()
			, (IHinter h) -> h==null ? NULL_STRING : h.toString()
			, CSP
			, CSP
			).toString();
		} catch (IOException ex) {
			return "WantedEnabledHinters: "+ex;
		}
	}

	/**
	 * Consider it licked.
	 *
	 * @return CSV of the unwanted tech.names for logging
	 */
	private String getUnwantedHinterNames() {
		return "Unwanted: " + unwanted.toString()
				.replaceFirst("^\\[", "")
				.replaceFirst("\\]$", "");
	}

	/**
	 * Returns a list of Tech names with unconstructible IHinter.
	 * <p>
	 * Check the {@link Tech#className} default implementation class-name:
	 * <ul>
	 *  <li>Does this class actually exist?
	 *  <li>Is it still in the given package?
	 *  <li>Does it have <u>ONE</u> public constructor?
	 *  <ul>
	 *   <li>Does that constructor take no-args;
	 *   <li>OR Tech as its <u>ONLY</u> parameter?
	 *   <li>If not construct it manually and use the IHinter want method,
	 *    averting this whole mess.
	 *  </ul>
	 *  <li>Private and protected constructors don't count.
	 *  <li>Write a new want method for odd-ball classes; to keep
	 *   {@link diuf.sudoku.solver.LogicalSolverBuilder#build} clean. <br>
	 *   There are several existing examples. All you need do is look.
	 * </ul>
	 *
	 * @return CSV of the error tech.names for logging
	 */
	private String getErrorTechNames() {
		if ( errors.isEmpty() )
			return "Errors: none";
		return "Errors: " + errors.toString()
				.replaceFirst("^\\[", "")
				.replaceFirst("\\]$", "");
	}

	// getWantedHinter, else if $construct then construct it.
	private IHinter getHinter(final Tech tech, final boolean construct) {
		final IHinter hinter = getWantedHinter(tech);
		if ( hinter!=null || !construct )
			return hinter;
		// you get here only when $tech is not wanted and $construct
		try {
			// nb: I added optional isNoisy param to constructHinter for this.
			return LogicalSolverBuilder.constructHinter(tech, Class.forName(tech.className), false);
		} catch ( Exception ex ) {
			throw new IllegalStateException("Unconstructable: "+tech, ex);
		}
	}

	// implements getVeryNaughtyBoys and getVeryVeryNaughtyBoys (not to be
	// confused with brian, who is something completely different) are the
	// same except for calling two different hinter methods; so we pass in
	// the current naughtiness-test as a lambda expression.
	//
	// @param label "VeryNaughtyBoys" or "VeryVeryNaughtyBoys"
	// @param naughtiness a lambda expression wrapping the *NaughtyBoys method
	// @return null meaning none, else $label: CSV of distinct naughtyBoys
	private String naughtyBoys(final String label, final Predicate<IHinter> naughtiness) {
		// A Set of distinct *NaughtyBoy hinter classes
		// need className (not toString, ie tech.name())
		Set<Class<?>> boys = null;
		for ( Tech tech : Tech.hinters() ) {
			// NOTE: Every hinter must be checked in the test-cases, because
			// tests are part of the release procedure, and we cannot release
			// VeryNaughtyBoys (performance), even if the hinters are not
			// actually naughty anymore (ie no longer produce invalid hints);
			// so if hes not wanted hes constructed only in the test-cases.
			final IHinter hinter = LogicalSolver.this.getHinter(tech, Run.isTestCase());
			if ( hinter!=null && naughtiness.test(hinter) ) {
				if(boys==null) boys = new HashSet<>(8, 0.75F);
				boys.add(hinter.getClass());
			}
		}
		if ( boys == null ) {
			return null;
		}
		return label + Frmt.csv(boys, (b)->b.getSimpleName());
	}
	public String getVeryNaughtyBoys() {
		return naughtyBoys("VeryNaughtyBoys: ", (IHinter h) -> h.isAVeryNaughtyBoy());
	}
	public String getVeryVeryNaughtyBoys() {
		return naughtyBoys("VeryVeryNaughtyBoys: ", (IHinter h) -> h.isAVeryVeryNaughtyBoy());
	}

	public void reportConfiguration() {
//		Log.teeln(getWantedHinterNames());
		Log.teeln(getWantedEnabledHinterNames());
		Log.teeln(getUnwantedHinterNames());
		Log.teeln(getErrorTechNames());
		if ( !!CFG.getBoolean("turbo", false) )
			Log.teeln("turbo");
		Log.teeln(getVeryNaughtyBoys()); // ignores null
		Log.teeln(getVeryVeryNaughtyBoys()); // ingores null
		// I am now done with these, so they can be GC'ed
		unwanted = null;
		errors = null;
	}

	// ------------------------------------------------------------------------

	/**
	 * @return the existing map containing the basic hinters.
	 */
	public Map<Tech, IHinter> getBasicHinters() {
		return basicHinters;
	}

	/**
	 * If Locking (not LockingGen) then replace Locking with SiameseLocking.
	 * <p>
	 * Sets-up this LogicalSolver to use SiameseLocking instead of Locking, so
	 * the batch uses SiameseLocking, to find bugs in SiameseLocking. I seek
	 * bugs by the old mass-attack and trip-over the bastards method, ergo
	 * incompetence, like in Production. Sigh.
	 * <p>
	 * nb: locking.index==0 means LockingGen (not Locking) is wanted, which
	 * precludes SiameseLocking, so setSiamese does nothing.
	 */
	public void setSiamese() { // wax on
		if ( locking.arrayIndex > 0 ) // NEVER negative
			wantedHinters[locking.arrayIndex] = siameseLocking;
	}

	/**
	 * Revert to standard Locking (not SiameseLocking).
	 */
	public void clearSiamese() { // wax off
		if ( locking.arrayIndex > 0 ) // NEVER negative
			wantedHinters[locking.arrayIndex] = locking;
	}

	/** Called after the puzzle is solved and before GC is forced. */
	public void cleanUp() {
		for ( IHinter h : wantedHinters )
			if ( h instanceof ICleanUp )
				try {
					((ICleanUp)h).cleanUp();
				} catch (Exception ex) {
					Log.teeTrace(""+h.getClass().getCanonicalName()+".cleanUp() failed", ex);
				}
	}

	/**
	 * Find the first hint from all categories of Hinters.
	 * <p>
	 * Called only by SudokuExplainer.getNextHintImpl (F2) when !wantMore with
	 * a SingleHintsAccumulator, whose add method returns true so the hinter
	 * exits early.
	 * <p>
	 * NOTE: getFirstHint/AllHints were written prior to IAccumulator.
	 * I think getFirstHint can be replaced with a call to getAllHints passing
	 * a SingleHintsAccumulator. Get rid of me. Rename getAllHints to findHints
	 * after the IHinter method it calls.
	 *
	 * @param grid the Grid to examine.
	 * @param accu SingleHintsAccumulator whose add returns true so that the
	 * hinter abandons the search and exits early. If you pass me a
	 * (Default)HintsAccumulator then my behaviour is "interesting" (I do not
	 * really understand what happened) so now I insist on a SingleHA.
	 * @return true if a hint was found, else false; always true AFAIK.
	 */
	public boolean getFirstHint(final Grid grid, final SingleHintsAccumulator accu) {
		if ( grid.numSet > 80 ) {
			accu.add(solvedHint);
			return true;
		}
		return getFirstHint(validators, grid, false, accu)
			|| getFirstHint(wantedHinters, grid, false, accu);
	}

	/**
	 * getAllHints: Get all the hints from this Grid; it is just that "all" is
	 * defined by wantMore (does the user want to see more hints?).
	 * <p>
	 * If a validator hints we are done regardless of what the user wants. <br>
	 * If !wantMore (F5) get all hints from the first hinty hinter only. <br>
	 * If wantMore (Shift-F5) keep going to get all hints from all hinters.
	 * <p>
	 * Called by SudokuExplainer.getAllHints, which is F5 in the GUI, or when
	 * you press enter in HintsTreeView to apply this hint and get the next.
	 *
	 * @param grid the Grid to solve
	 * @param wantMore are MORE (the Shift in Shift-F5) hints wanted regardless
	 * of how "hard" they are, or how long it takes to find them; which means a
	 * wait of about 15 seconds on my i7.
	 * @param logGrid should I log the grid.
	 * @param logHints should I log hints.
	 * @param printHints should I print hints to stdout.
	 * @return List of AHint
	 */
	public LinkedList<AHint> getAllHints(final Grid grid, final boolean wantMore
			, final boolean logGrid, final boolean logHints, final boolean printHints) {
		if (Log.LOG_MODE >= Log.VERBOSE_2_MODE) {
			if ( logGrid ) {
				Log.println();
				final String more = wantMore ? " MORE" : "";
				Log.format("\n>getAllHints%s %d/%s\n", more, grid.hintNumber, grid.sourceShort());
				Log.println(grid);
			}
		}
		final LinkedList<AHint> hints = new LinkedList<>();
		// this does not appear to ever be triggered! So I guess we no longer
		// get here when the grid is already full, but I am leaving it.
		if ( grid.numSet > 80 ) {
			hints.add(solvedHint);
			return hints; // empty if I was interrupted, or did not find a hint
		}
		final long start = Run.time = System.nanoTime();
		// HintsAccumulator.add returns false so hinter keeps searching,
		// adding each hint to the hints List.
		final IAccumulator accu = new HintsAccumulator(hints);
		// get hints from hinters until one of them returns true (I found a
		// hint) OR if wantMore then run all hinters. nb: DyamicPlus nearly
		// always finds a hint. NestedUnary ALWAYS finds a hint.
		boolean any = getFirstHint(validators, grid, false, accu);
		if ( !any ) {
			// do siamese locking on F5 in GUI when filtering
			// nb: read the global config, not my immutable copy
			if ( Run.isGui() && !wantMore && CFG.isFilteringHints() )
				setSiamese();
			// run prepare AFTER validators and BEFORE wantedHinters
			if ( !grid.isPrepared() ) // once on each grid
				prepare(grid); // prepare wanted IPrepare's to process grid
			any = getAllHints(wantedHinters, grid, !wantMore, logHints, accu);
			// revert to standard locking
			clearSiamese();
		}
		if (Log.LOG_MODE >= Log.VERBOSE_2_MODE) {
			final long took = System.nanoTime()-start;
			// if there where any hints (there should be)
			if ( any ) {
				final AHint hint = accu.peek(); // the first hint, else null
				if ( Run.ASSERTS_ENABLED )
					validateHint(grid.solution(), hint, false);
				if ( logHints )
					printGridFull(Log.out, hint, grid, hint.getNumElims(), took);
				if ( printHints )
					printGridFull(System.out, hint, grid, hint.getNumElims(), took);
			}
			final String more; if(wantMore) more=" MORE"; else more="";
			// this is useful in stdout as well as the log
			Log.teef("<getAllHints%s %d/%s %b %,15d\n", more, grid.hintNumber, grid.sourceShort(), any, took);
		}
		return hints; // empty if I was interrupted, or did not find a hint
	}

	// findHints in the given hinters until one returns true,
	// or interrupted so I exit-early returning false.
	private boolean getFirstHint(final IHinter[] hinters, final Grid grid
			, final boolean logHints, final IAccumulator accu) {
		return getAllHints(hinters, grid, true, logHints, accu);
	}

	/**
	 * findHints from each the given hinters in turn: if wantLess then return
	 * when the first hint found is found, as per getFirst; else getHints from
	 * all the hinters, adding them to accu, and then return "were any found?".
	 * Which will always be true, AFAIK, unless the chainers go tits-up.
	 * <p>
	 * This method is only called by getAllHints directly, to handle wantLess.
	 * Hinters are only called then they are enabled. A Hinter is disabled if
	 * it is a very naughty boy and produces the same deadCat twice.
	 *
	 * @param hinters to use to find hints
	 * @param grid to search for hints
	 * @param wantLess should I stop at the first hint
	 * @param logHints do you want me to log each hint?
	 * @param accu IAccumulator to which I add hints
	 * @return any hints found, ergo true unless chainers go tits up
	 */
	private boolean getAllHints(final IHinter[] hinters, final Grid grid
		, final boolean wantLess, final boolean logHints
		, final IAccumulator accu
	) {
		try {
			setWantedHintersAccumulators(accu);
			for ( IHinter hinter : hinters )
				if ( hinter.isEnabled()
				  && findHints(hinter, grid, logHints, accu) && wantLess )
					break;
		} finally {
			clearWantedHintersAccumulators();
		}
		return wantLess && accu.any();
	}

	// findHints calls the hinter.findHints; log it if isNoisy.
	private boolean findHints(final IHinter hinter, final Grid grid
			, final boolean logHints, final IAccumulator accu) {
		// get the accumulator appropriate for this hinter
		IAccumulator accumulator = hinter.getAccumulator(accu);
		// if we are logging, do it noisily
		if (Log.LOG_MODE >= Log.VERBOSE_2_MODE) {
			if ( logHints ) {
				Log.format("%-40s ", hinter.getTechName());
				final long t0 = System.nanoTime();
				final boolean result = hinter.findHints(grid, accumulator);
				final long took = System.nanoTime() - t0;
				Log.format("%,15d\t%s\n", took, hinter);
				return result;
			}
		}
		// else just do it quietly
		return hinter.findHints(grid, accu);
	}

	/**
	 * enableKrakHeads dis/enables Krakens, coz they break generate.
	 * <p>
	 * The Generator skips Kraken Swordfish & Jellyfish which bugger-up on null
	 * table entries, and I cannot understand why they are null, which has been
	 * driving me mad, to the point where I decided to just skip the whole damn
	 * mess, leaving the clean-up to someone with a "real" IQ. sigh.
	 *
	 * @param enabled if false then disable. if true then enable.
	 * @param degree the minimum size krakens to dis/enable: 3 (Sword).
	 */
	public void enableKrakHeads(final boolean enabled, final int degree) {
		for ( IHinter h : wantedHinters )
			if ( h.isKrakHead() && h.getDegree()>=degree )
				h.setIsEnabled(enabled);
	}

	/**
	 * Get the wanted hinter which implements 'tech', else null.
	 *
	 * @param tech the target tech for which we seek the implementing hinter
	 * @return the implementing hinter only if 'tech' is wanted; <br>
	 *  else null probably means 'tech' not wanted, <br>
	 *  or (improbable) no implementation of 'tech', <br>
	 *  or (tilt) s__t happened
	 */
	public IHinter getWantedHinter(final Tech tech) {
		// fast O(1) look-up
		try {
			final IHinter hinter;
			if ( (hinter=hinterCache.get(tech)) != null )
				return hinter;
		} catch (Exception eaten) {
			// Do nothing
		}
		// empty/missing, so add current wanted hinters
		for ( IHinter h : wantedHinters )
			try {
				hinterCache.put(h.getTech(), h);
			} catch (Exception eaten) {
				// Do nothing
			}
		// rescue: the old O(n) way (never happens)
		for ( IHinter hinter : wantedHinters )
			if ( hinter.getTech() == tech ) {
				hinterCache.put(tech, hinter);
				return hinter;
			}
		return null;
	}

	/**
	 * analyseDifficulty is used only by Generator to calculate how difficult a
	 * random generated puzzle is. The question that I answer is:
	 * Is this puzzles-difficulty (the max hint difficulty) less than maxD?
	 * <p>
	 * To calculate an answer to this question we find and apply hints to the
	 * grid, stopping at the first hint whose difficulty exceeds <tt>maxD</tt>.
	 * Pretty obviously, there may not be one. I return the puzzle-difficulty,
	 * ie the maximum hint difficulty, even if it is above the given maxD (which
	 * just tells me to stop analysing).
	 * <p>
	 * My caller enforces minD (or not) itself. There is no consideration of
	 * minimum difficulty here, only maximum. To enforce a minimum: if this
	 * puzzles difficulty (the value I return) is below Difficulty.min then
	 * reject this puzzle. I only monitor the maxD, so that I can return as
	 * soon as it is exceeded.
	 * <p>
	 * NB: minD is inclusive and maxD is EXCLUSIVE, so maxD is minD of the next
	 * Difficulty, so always {@code pd < maxD} (not {@code pd <= maxD}).
	 *
	 * @param grid to analyse to find the difficulty (the hardest step)
	 * @param maxD the maximum desired difficulty (inclusive), a ceiling that
	 *  stop at if exceeded
	 * @return The puzzles difficulty if it is below the given <tt>maxD</tt>,
	 *  else the arbitrary high value Difficulty.IDKFA.max, currently 100.0.
	 * @throws HinterruptException from AHinter.interrupt() back-up to generate.
	 */
	public int analyseDifficulty(final Grid grid, final int maxD) {
		int pd = 0; // puzzleDifficulty, my result
		final Grid copy = new Grid(grid);
		final IAccumulator accu = new SingleHintsAccumulator();
		// re-enable all hinters just in case we hit a deadCat last time
		if ( anyDisabled ) {
			anyDisabled = false;
			for ( IHinter hinter : wantedHinters )
				hinter.setIsEnabled(true);
		}
		deadCats.clear(); // deal with eliminationless hints
		copy.hintNumberReset(); // Start from the start
		enableKrakHeads(false, 3); // disable KrakenSwordfish and KrakenJellyfish
		for ( int pre=copy.numMaybes,now; pre>0; pre=now ) // ie !isFull
			if ( getFirstHint(wantedHinters, copy, false, accu) ) {
				final AHint hint = accu.poll();
				assert hint != null;
				// get the difficulty-rating of this hint
				final int hd = hint.getDifficulty(); // hintDifficulty
				// calculate puzzleDifficulty = maximum hintDifficulty,
				// and if the pd exceeds the maxD then return it
				if ( hd>pd && (pd=hd)>=maxD )
					return pd; // max target difficulty EXCLUSIVE exceeded
				// apply the hint, and if he removes 0 then disable him.
				if ( hint.applyQuitely(false, copy) == 0 ) { // NO_AUTOSOLVE
					// second time around he gets the chop
					if ( !deadCats.add(hint) ) { // a funky set
						anyDisabled = true;
						hint.hinter.setIsEnabled(false);
					}
				}
				// check numMaybes myself (paranoid android)
				if ( (now=copy.numMaybes)==pre ) {
					// second time around he gets the chop
					if ( !deadCats.add(hint) ) { // a funky set
						anyDisabled = true;
						hint.hinter.setIsEnabled(false);
					}
				}
			} else {
				Log.teeln("analyseDifficulty: No hint found in grid:");
				Log.teeln(copy);
				pd = ANALYSE_INTERRUPTED; // an impossibly large puzzleDifficulty
				break;
			}
		if ( anyDisabled )
			Log.teeln("WARN: "+Log.me()+": deadCats: "+MyCollections.toString(deadCats));
		enableKrakHeads(true, 3);
		return pd;
	}

	/**
	 * Get the Solution of this puzzle. Used by generate and by the <i>Tools ~
	 * Analyse</i> menu-item in the SudokuFrame. The normal hint returned is
	 * an AnalysisHint that contains a summary of the hints applied.
	 *
	 * @param grid a copy of the Grid to solve (not the GUI's grid)
	 * @param logHints are hints logged.
	 * @param logHinterTimes is EVERY single hinter execution-time logged. <br>
	 *  This is HEAPS verbose, so do not use it in a full batch run!
	 * @return an AHint which is either an AnalysisHint or a WarningHint.
	 */
	public AHint analyse(final Grid grid, final boolean logHints, final boolean logHinterTimes) {
		if ( grid.numSet > 80 )
			return solvedHint;
		if (Log.LOG_MODE >= Log.NORMAL_MODE) {
			Log.format("\nanalyse: %s\n%s\n", grid.source, grid);
		}
		final long t0 = System.nanoTime();
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		// execute just the puzzle validators.
		if ( getFirstHint(puzzleValidators, grid, false, accu) )
			return accu.poll();
		// LogicalAnalyser.findHints allways returns true, only the returned
		// hint-type varies: solved=AnalysisHint/invalid=WarningHint which are
		// both WarningHints; so take care to differentiate them properly.
		if ( new LogicalAnalyser(this, logHints, logHinterTimes).findHints(grid, accu) ) {
			final long t1 = System.nanoTime();
			final AHint hint = accu.poll(); // a SolutionHint
			if (Log.LOG_MODE >= Log.NORMAL_MODE) {
				Log.format("%,15d\t%2d\t%4d\t%3d\t%-30s\t%s\n"
						, t1-t0, grid.numSet, grid.numMaybes
						, hint.getNumElims(), hint.getHinter(), hint);
				if ( hint instanceof AnalysisHint )
					log((AnalysisHint) hint);
			}
			return hint;
		}
		return null; // happens after InterruptException
	}

	private void log(final AnalysisHint analysis) {
		final StringBuilder sb = SB(BIG_BFR_SIZE);
		// Difficulty goes BEFORE appendUsageMap, for consistency with HTML.
		final String maxD = "Difficulty ###"; // largest possible difficulty
		sb.append(NL); // leading blank line
		sb.append(maxD).append(NL);
		analysis.appendUsageMap(sb);
		// an unusual trailing blank line
		sb.append("Total Difficulty ").append(analysis.ttlDifficulty).append(NL);
		final int i = sb.indexOf(maxD);
		String newMaxD = "Difficulty "+lng(analysis.maxDifficulty);
		sb.replace(i, i+maxD.length(), newMaxD);
		Log.println(sb);
	}

	/**
	 * Returns a copy of the grid that has been solved using brute-force.
	 *
	 * @param grid to solve
	 * @return a copy of the given grid that has been solved
	 */
	public Grid solve(final Grid grid) {
		return bruteForce.solve(grid);
	}

	/**
	 * Returns the number of possible solutions for the given grid.
	 *
	 * @param grid Grid to be solved
	 * @return 1 means the puzzle is valid, <br>
	 *  0 means invalid (no solution), <br>
	 *  2 means invalid (multiple solutions).
	 */
	public int countSolutions(final Grid grid) {
		return bruteForce.countSolutions(grid);
	}

	/**
	 * Solve the given Grid recursively, with brute force, not logic.
	 * <p>
	 * Having recursive s__t in the LOGICALSolver simplifies SudokuExplainer,
	 * but it smells like dead fish jammed sideways up Smelly Cats a-hole. If
	 * it annoys you too then fix it. Create a RecursiveSolver, which will
	 * duplicate god-knows-what fields and code, which'll be pushed up into an
	 * ASolver. Add RecursiveSolver into SudokuExplainer. I am just too lazy.
	 *
	 * @param grid the puzzle to solve
	 * @return a SolutionHint as an AHint, else null
	 */
	public AHint getSolutionHint(final Grid grid) {
		final IAccumulator accu = new SingleHintsAccumulator();
		// check the puzzle is basically valid
		if ( grid.numSet > 80 ) // already solved
			return solvedHint;
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) ) // NumberOfValues >= 8
			return accu.poll();
		final Grid solution = solve(grid);
		if ( solution == null )
			return new WarningHint(bruteForce, "Unsolvable", "Unsolvable.html");
		return new SolutionHint(bruteForce, grid, solution);
	}

	/**
	 * Validate the puzzle only (not the grid).
	 * @param grid
	 * @param accu
	 * @return
	 */
	private boolean validatePuzzle(final Grid grid, final IAccumulator accu) {
		if ( tooFewClues.findHints(grid, accu)  // numClues >= 17
		  || tooFewValues.findHints(grid, accu) // numValues >= 8
		  || bruteForce.findHints(grid, accu) ) // OneSolution
			return carp(new UnsolvableException("Invalid Sudoku Puzzle: "+accu.poll()), grid, true);
		return true; // the puzzle is (probably) valid.
	}

	/**
	 * Checks the validity of the given Grid completely, ie using all puzzle
	 * and grid validators.
	 * <p>
	 * Call validatePuzzleAndGrid ONCE before solving each puzzle, other
	 * validations should do the puzzle only.
	 *
	 * @param grid Grid to validate.
	 * @param logHints true to log, false to shut the ____ up.
	 * @return the first warning hint, else null.
	 */
	public AHint validatePuzzleAndGrid(final Grid grid, final boolean logHints) {
		final IAccumulator accu = setWantedHintersAccumulators(
				new SingleHintsAccumulator());
		if ( getFirstHint(validators, grid, logHints, accu) )
			return accu.poll(); // problem
		return null; // no problem
	}

	/**
	 * Solve the puzzle in 'grid' logically populating 'usageMap' and return
	 * was it solved.
	 * <p>
	 * In English: the solve method solves the given Sudoku puzzle (grid) using
	 * logic (a rather long array of hinters) and populate the given UsageMap
	 * with a summary of hinters used, how often, and how long each took;
	 * returning "Did it solve?", which is always true for valid puzzles.
	 * Invalid puzzles throw an UnsolvableException, so solve is never false.
	 * Never say never.
	 * <p>
	 * The {@link #timeHinters} method is the guts of solve. It invokes
	 * {@link IHinter#findHints(Grid, IAccumulator) } on each hinter in the
	 * {@link #wantedHinters} array. wantedHinters is pre-ordered (roughly) by
	 * increasing {@link IHinter#getDifficulty() }, so we find the simplest
	 * available hint at every step, and apply that hint to the puzzle, then
	 * search again, and again, until solved; thus producing the simplest
	 * possible solution to any Sudoku puzzle. Simples!
	 *
	 * @param grid Grid containing the Sudoku puzzle to be solved
	 * @param usage to populate with hinter usage: number of calls, hint count,
	 *  elapsed time, and number of maybes eliminated
	 * @param doValidations true means run the TooFewClues and TooFewValues
	 *  validators before solving the puzzle
	 * @param isNoisy true prints local messages to standard output
	 * @param logHints true logs progress, false does it all quietly
	 * @param logHinterTimes NEVER true in LogicalSolverTester (too verbose)
	 * @return was it solved; else see grid.invalidity
	 * @throws UnsolvableException (a RuntimeException) when puzzle is invalid,
	 *  ie cannot be solved. Details in the exception message (if any) and/or
	 *  the grid.invalidity field. sigh. <br>
	 *  A UE could just mean you have unwanted the rescue hinters: DynamicPlus
	 *  and NestedUnary, but typically a UE means you broke the hinter you are
	 *  working-on, so use the Validator to find examples in order to work-out
	 *  precisely where you have gone wrong. <br>
	 *  Note that a UE purports to say "puzzle is broken" but what it really
	 *  means is "failed to solve puzzle", which really means "you broke the
	 *  bloody solver again!".
	 */
	public boolean solve(final Grid grid, final UsageMap usage
		, final boolean doValidations, final boolean isNoisy
		, boolean logHints, boolean logHinterTimes
	) {
		AHint hint;
		long now;
		assert grid != null;
		assert usage != null;
		logHints |= logHinterTimes;
		grid.invalidity = null; // assume sucess
		if ( isNoisy )
			System.out.println(">solve "+Config.startDateTime()+"\n"+grid);
		// the SingleHintsAccumulator collects just the first hint.
		final IAccumulator accu = setWantedHintersAccumulators(new SingleHintsAccumulator());
		// time between hints incl activate and apply
		// nb: first hint-time is blown-out by enabling, activation, etc.
		long start = Run.time = System.nanoTime();
		// special case for being asked to solve a solved puzzle. Not often!
		if ( grid.numSet > 80 ) { // ie the grid is already solved
			accu.add(solvedHint);
			return true; // allways, even if accu is NOT a Single
		}
		// when called by the GUI we do not need to validate any longer because
		// my caller (the LogicalAnalyser) already has, in order to handle the
		// hint therefrom properly; so now we only validate here when called by
		// the LogicalSolverTester, which is all a bit dodgy, but only a bit;
		// all because it is hard to differentiate "problem" messages. Sigh.
		if ( doValidations )
			validatePuzzle(grid, accu); // throws UnsolvableException
		assert grid.isPrepared();
		// make all caches refresh
		grid.hintNumberReset();
		// create usage entries
		usage.addAll(wantedHinters);
		// solve the puzzle by finding and applying hints
		while ( grid.numSet < GRID_SIZE ) {
			// getHints from each wanted hinter that is still enabled
			if ( !timeHinters(logHinterTimes, wantedHinters, grid, usage, accu) )
				break;
			// apply the hint
			now = System.nanoTime();
			hint = accu.poll();
			if ( Run.ASSERTS_ENABLED && Run.isGui() ) // java -ea GUI only
				validateHint(grid.solution(), hint);
			// apply may throw UnsolvableException from Cell.set
			apply(hint, now-start, grid, usage, logHints, isNoisy);
			if ( Run.ASSERTS_ENABLED && Run.isGui() ) // java -ea GUI only
				if ( bruteForce.countSolutions(grid) != 1 )
					throw new UnsolvableException("Not ONE solution");
			start = now;
		}
		// puzzle solved, so clean-up now
		cleanUp();
		if ( isNoisy )
			System.out.format("<solve %s\t%s\n", Config.took(), grid);
		// remove unused hinters
		usage.removeAll((u)->u.calls == 0);
		clearWantedHintersAccumulators();
		return true;
	}

	/**
	 * Validate $hint against the given $solution.
	 * <p>
	 * NOTE: If hint.cell != null then dont validate reds; they are required
	 * by ChainerHintsAccumulator, but are NOT used as eliminations!
	 *
	 * @param solution grid.solution() is an array of the 81 cell values from
	 *  the solved puzzle, generated by BruteForce
	 * @param hint to validate against the solution
	 */
	private <T extends AHint> T validateHint(final int[] solution, final T hint) {
		return validateHint(solution, hint, VALIDATE_THROWS);
	}

	private <T extends AHint> T validateHint(final int[] solution, final T hint, final boolean chuck) {
		if ( solution == null )
			return null;
		if ( hint.cell != null )
			if ( hint.value != solution[hint.cell.indice] ) {
				hint.setIsInvalid(true); // BEFORE toString!
				final String msg = "BAD set: "+hint.cell.id+"+"+hint.value
				+" != solution "+solution[hint.cell.indice]+" in "+hint.toFullString();
				hint.setDebugMessage(msg);
				if ( chuck )
					throw new UnsolvableException(msg);
			}
		else if ( hint.reds != null )
			for ( Map.Entry<Integer,Integer> e : hint.reds.entrySet() )
				if ( (e.getValue() & VSHFT[solution[e.getKey()]]) > 0 ) { // 9bits
					hint.setIsInvalid(true); // BEFORE toString!
					final String msg = "BAD red: "
					+CELL_IDS[e.getKey()]+"-"+MAYBES_STR[e.getValue()]
					+" includes solution value "+solution[e.getKey()]
					+" in "+hint.toFullString();
					hint.setDebugMessage(msg);
					if ( chuck )
						throw new UnsolvableException(msg);
				}
		return hint;
	}

	/**
	 * Prompts each hinter to remember whichever IAccumulator is appropriate
	 * for it.
	 *
	 * @param accu the standard IAccumulator, either a HintsAccumulator or a
	 *  SingleHintsAccumulator
	 * @return the given standard accu
	 */
	private IAccumulator setWantedHintersAccumulators(final IAccumulator accu) {
		final ExplodingHintsAccumulator eha = new ExplodingHintsAccumulator(accu); // VeryVeryNaughty
		final ValidatingHintsAccumulator vha = new ValidatingHintsAccumulator(accu); // VeryNaughty
		for ( IHinter hinter : wantedHinters )
			hinter.setAccumulator(accu, eha, vha);
		return accu;
	}

	private void clearWantedHintersAccumulators() {
		for ( IHinter hinter : wantedHinters )
			hinter.clearAccumulator();
	}

	/**
	 * getHints from each hinter.
	 *
	 * @param logHinterTimes true logs execution time of every hinter, which is
	 *  too-verbose for a top1465 batch, so use only for selected puzzles
	 * @param hinters to search with
	 * @param grid to search
	 * @param usage summary of hinters and timings
	 * @param accu the IAccumulator to add hints to
	 * @param eha the ExplodingHintsAccumulator in case any hinters are
	 *  isAVeryVeryNaughtyBoy
	 * @param vha the ValidatingHintsAccumulator in case any hinters are
	 *  isAVeryNaughtyBoy
	 * @return any hints found, ergo always true, until it isnt
	 * @throws HinterruptException if the Generator is stopped by the user.
	 */
	private boolean timeHinters(final boolean logHinterTimes
		, final IHinter[] hinters, final Grid grid, final UsageMap usage
		, final IAccumulator accu
	) {
		for ( IHinter hinter : hinters )
			if ( hinter.isEnabled() ) {
				if ( timeHinter(logHinterTimes, hinter, grid, usage, accu) )
					return true;
				if ( Run.isHinterrupted() )
					throw new HinterruptException();
			}
		return false;
	}

	private boolean timeHinter(final boolean logHinterTimes
		, final IHinter hinter, final Grid grid, final UsageMap usage
		, final IAccumulator accu
	) {
		final IAccumulator accumulator = hinter.getAccumulator(accu);
		final long start = System.nanoTime();
		final boolean result = hinter.findHints(grid, accumulator);
//if ( hinter.getTech() == Tech.TableChain && result )
//	Debug.breakpoint();
		final long took = System.nanoTime() - start;
		usage.get(hinter).add(1, 0, 0, took);
		if ( logHinterTimes ) // very verbose
			Log.teef("\t%,14d\t%s\n", took, hinter);
		if ( result && !accumulator.any() )
			return brian(hinter);
		else if ( !result && accumulator.any() )
			Log.teeln("WARN: "+hinter+" said false but accu is not empty");
		return result;
	}

	// He's not the masiah, he's just a very naughty boy!
	private boolean brian(final IHinter hinter) {
		// batch stops here
		final String msg = ""+hinter+" says true but nada in accu.";
		if ( !Run.isGui() )
			throw new UnsolvableException(msg);
		// GUI drops this hinter
		final IHinter[] a = new IHinter[wantedHinters.length - 1];
		String pre = java.util.Arrays.toString(wantedHinters);
		final int n = hinter.getArrayIndex();
		System.arraycopy(wantedHinters, 0, a, 0, n);
		System.arraycopy(wantedHinters, n+1, a, n, a.length-n);
		wantedHinters = a;
		beep();
		Log.teeln("WARN: Brianed "+msg);
		Log.teeln(pre);
		Log.teeln(java.util.Arrays.toString(wantedHinters));
		return false;
	}

	/**
	 * Apply this hint to the grid.
	 *
	 * @param hint to apply
	 * @param took nanoseconds between now and previous hint
	 * @param grid to apply the hint to
	 * @param usage update num calls, hints, elims, and time
	 * @param logHints if true then log hints
	 * @param logGrid if true then log the grid with the hints. Note that this
	 *  has no effect unless logHints is true, because the logging the grid
	 *  is pointless without the hints (it makes the log nonsensical)
	 * @throws UnsolvableException on the odd occasion
	 * @return the number of cell-values eliminated (and/or 10 per cell set)
	 */
	private int apply(final AHint hint, final long took, final Grid grid
		, final UsageMap usage, final boolean logHints, final boolean logGrid) {
		if ( hint == null )
			return 0;
		if ( hint instanceof AWarningHint ) // DoubleValue MissingMaybe
			throw new UnsolvableException("Fubarred: " + hint);
		assert hint.getDifficulty() >= 1.0; // 0.0 has occurred. Bug!
		// + the grid (2 lines per hint)
		if (Log.LOG_MODE >= Log.VERBOSE_3_MODE) {
			// the grid makes no sense without the hints
			if ( logHints && logGrid ) {
				Log.println();
				Log.println(grid);
			}
		}
		int numElims = -1;
		try {
			// apply throws UnsolvableException on the odd occassion
			// AUTOSOLVE true sets subsequent naked and hidden singles
			// isNoisy logs crap when true (for debugging only, really)
			if (Log.LOG_MODE >= Log.VERBOSE_2_MODE) {
				if ( logHints )
					numElims = hint.applyNoisily(Grid.AUTOSOLVE, grid);
				else
					numElims = hint.applyQuitely(Grid.AUTOSOLVE, grid);
			} else
				numElims = hint.applyQuitely(Grid.AUTOSOLVE, grid);
			++grid.hintNumber;
		} catch (UnsolvableException ex) { // probably from Cell.set
			// stop the UnsolvableException obscuring the causal hint.
			Log.teeln("LogicalSolver.apply: "+hint.toFullString());
			Log.teeln("caused: "+ex);
			throw ex;
		}
		// + 1 line per Hint (detail)
		if ( Log.LOG_MODE >= Log.VERBOSE_2_MODE && logHints )
			printHintFull(Log.out, hint, grid, numElims, took);
		// NB: usage was inserted by doHinters2, we just update it
		if ( usage != null )
			updateUsage(usage, hint, numElims);
		if ( grid.isInvalidated() )
			throw new UnsolvableException("Invalidity: " + grid.invalidity);
		// detect and deal-with eliminationless hints
		// nb: deadCats.add is "addOnly"; returns false if already exists.
		if ( numElims == 0 ) {
			if ( !deadCats.add(hint) ) {
				final String details = hint.hinter.getClass().getSimpleName()
					+"("+hint.hinter.getTechName()+"): "+hint.toFullString();
				// throw if hinter is known Naughty!
				if ( hint.hinter.isAVeryNaughtyBoy()
				  || hint.hinter.isAVeryVeryNaughtyBoy() )
					throw new UnsolvableException("NaughtyDeadCat: "+details);
				// avert infinite loop by disabling the ____wit.
				Log.teeln("WARN: deadCat disabled "+details);
				hint.hinter.setIsEnabled(false);
			}
		}
		return numElims;
	}

	// Updates the UsageMap with the hint details: adds numHints and numElims,
	// max's maxDifficulty, and addonates the subHints.
	// Called by: solve->apply->updateUsage
	// NB: usage was added by timeHinters, we just update the details.
	private void updateUsage(final UsageMap map, final AHint hint, final int numElims) {
		Usage u;
		if ( (u=map.get(hint.hinter)) == null ) {
			// Locking upgrades siamese hints to HiddenPair/Triple, so usage
			// may not yet exist, so add it. The time is already allocated to
			// Locking even-though it is a hidden-set hint. Sigh.
			u.add(1, 1, numElims, 0L);
			u.maxDifficulty = hint.getDifficulty();
			u.ttlDifficulty = hint.getDifficultyTotal(); // GEM differs
			return;
		}
		++u.hints;
		u.elims += numElims;
		// nb: Math.max is slower.
		final int d = hint.getDifficulty();
		if ( d > u.maxDifficulty )
			u.maxDifficulty = d;
		u.ttlDifficulty += hint.getDifficultyTotal(); // GEM differs
		// upsert subHints Map: for hinters producing multiple hint types.
		u.upsertSubHints(hint, 1);
	}

	public void prepare(final Grid grid) {
		if ( grid==null || grid.isPrepared() )
			return; // safety first!
		getPreppers().forEach((prepper) -> {
			try {
				prepper.prepare(grid, this);
			} catch (Exception ex) {
				Log.teeTrace("WARN: "+Log.me()+": "+prepper+".prepare threw: ", ex);
			}
		});
		grid.setPrepared(true);
	}

	/**
	 * Get the cached {@code LinkedList<IPrepare>} from wantedHinters.
	 * <p>
	 * The preppers-cache is created ONCE, first-time only. This works because
	 * wantedHinters DOES NOT CHANGE during the lifetime of a LogicalSolver.
	 * <p>
	 * The only exception, AFAIK, is Locking: implementations are swapped.
	 * Hence, neither {@link diuf.sudoku.solver.hinters.lock.Locking} nor
	 * {@link diuf.sudoku.solver.hinters.lock.LockingGen} can be Preppers!
	 * If you make either a prepper it won't get prepped reliably. Best just
	 * avoid the whole mess by not prepping anywhere in the lock package.
	 *
	 * @return A cached {@code LinkedList<IPrepare>}
	 */
	private LinkedList<IPrepare> getPreppers() {
		synchronized ( this ) {
			if ( preppersCache == null ) {
				final LinkedList<IPrepare> preppers = new LinkedList<>();
				for ( IHinter hinter : wantedHinters )
					if ( hinter instanceof IPrepare )
						preppers.add((IPrepare)hinter);
				preppersCache = preppers;
			}
			return preppersCache;
		}
	}

	/**
	 * Can this grid be solved just by filling in naked and hidden singles?
	 * I find these easiest two Sudoku solving techniques mundane, so the GUI
	 * flashes green when only singles remain, and I Ctrl-P onto next puzzle.
	 *
	 * @param grid puzzle to solve, or not.
	 * @param accu If null then no hint is created, otherwise accu is the
	 *  IAccumulator to which I add a SummaryHint (not a SolutionHint, as
	 *  you might reasonably expect).
	 * @param useCache will you accept a cached {@link #singles} or should I
	 *  recalculate the solution now. If you dont know then pass me false coz I
	 *  am fast enough to solve or not-solve, so caching is a highspeed wanking
	 *  accident: easy to jerk but tough to clean-up after.
	 * @return was the grid solved? returns false on Exception that is logged.
	 */
	public boolean solveWithSingles(final Grid grid, final IAccumulator accu, final boolean useCache) {
		// clear the cache if !useCache
		if ( useCache && this.singles!=null )
			return true;
		else
			this.singles = null;
		// presume that no hint will be found
		boolean result = false;
		// HintsApplicumulator.add apply's each hint when it is added.
		// @param isStringy=true for hint.toString's in the SummaryHint
		final HintsApplicumulator apcu = new HintsApplicumulator(true);
		Grid copy = null;
		try {
			// we solve a copy of the given grid
			copy = new Grid(grid);
			apcu.grid = copy; // to tell Cell.apply which grid
			int ttlElims = 0;
			do {
				apcu.numElims = 0;
				if ( nakedSingle.findHints(copy, apcu)
				   | hiddenSingle.findHints(copy, apcu) )
					ttlElims += apcu.numElims;
			} while ( apcu.numElims > 0 );
			if ( (result=ttlElims>0 && copy.numSet>80) && accu!=null ) {
				singles = copy; // hints pre-applied to copy
				accu.add(new SummaryHint("LogicalSolver.solveWithSingles", ttlElims, apcu));
			}
		} catch (Exception ex) { // especially UnsolvableException
// report when we are focusing on solveWithSingles, which we normaly are not.
			Log.teef("%s\n%s\n", copy!=null?copy.toString():"null copy", ex);
		} finally {
			apcu.grid = null;
		}
		return result;
	}

	// pass me any RuntimeException and I log and optionally throwIt.
	private boolean carp(final RuntimeException ex, final Grid grid, final boolean throwIt) {
		grid.invalidity = ex.getMessage();
		if ( throwIt ) {
			if (Log.LOG_MODE >= Log.NORMAL_MODE) {
				System.out.println();
				System.out.println(grid);
			}
			throw ex;
		}
		Log.println();
		ex.printStackTrace(Log.out);
		Log.println();
		Log.println(grid);
		Log.flush();
		return false;
	}

	/**
	 * Disable internal caches in wantedHinters.
	 *
	 * @return a list of hinters where useCache was true before this
	 *  method was called, for use in {@link #restoreInternalCaches}.
	 */
	public List<IHinter> disableInternalCaches() {
		final List<IHinter> preCaches = new LinkedList<>();
		for ( IHinter hinter : wantedHinters )
			if ( hinter.setCaching(false) )
				preCaches.add(hinter);
		return preCaches;
	}
	/**
	 * Restore internal caches after {@link disableInternalCaches()}.
	 *
	 * @param preCaches a list of hinters where useCache was true
	 *  before {@link #disableInternalCaches} was called
	 */
	public void restoreInternalCaches(final Iterable<IHinter> preCaches) {
		preCaches.forEach((e) -> e.setCaching(true));
	}

	/**
	 * Get a map of all hinters by Tech.name().
	 *
	 * @return
	 */
	public EnumMap<Tech, IHinter> getHintersMap() {
		if ( hinterCache.isEmpty() )
			// I hijack Cheats hinterCache (so this sets him up too)
			// Run ONCE at start of logFollow, so dont cache the cache.
			for ( IHinter hinter : wantedHinters )
				hinterCache.put(hinter.getTech(), hinter);
		return hinterCache;
	}

	// ----------------------------- shut-up time -----------------------------

	/**
	 * The report() method is called by the LogicalSolverTester (et al) to
	 * print a report from each IHinter which implements IReporter.
	 * <p>
	 * An IHinter can declare itself to be an IReporter and implement the
	 * standard report method, which typically prints performance metrics
	 * that are intended to always be from LogicalSolverTester top1465.d5.mt
	 * to give me a comparative standard for performance testing, but each
	 * IReporter also reports at the end of each GUI run, which is acceptable.
	 * <p>
	 * The reporting convention is that you print a blank like, then a header
	 * line which should identify which class is reporting, and then all your
	 * report content with NO blank lines there-in.
	 * <p>
	 * NOTE: libate() now just calls report(). Much simpler, and it works.
	 * <p>
	 * For example:
	 * <pre>{@code public void report() {
	 *      // nb: use tech.name() for hinters which implement multiple techs.
	 *      Log.teef("\n%s: counts=%s\n", tech.name(), java.util.Arrays.toString(counts));
	 * }}</pre>
	 */
	public void report() {
		for ( IHinter hinter : wantedHinters )
			if ( hinter instanceof IReporter )
				try {
					((IReporter)hinter).report();
				} catch (Exception ex) { // usually a bad printf specifier
					StdErr.carp(""+hinter+".report failed.", ex);
				}
	}

	/**
	 * The close() method releases any/all resources. In this case that means
	 * persisting any set-config in any/all of the Filter/s we have been using.
	 * <p>
	 * Closeable says this method throws IOException, so I follow along, but
	 * this implementation eats all Throwables, ie it never throws anything,
	 * ever, which is a MUCH better approach for closing I/O.
	 * <p>
	 * The Closeable.close() method may be called either by autoclose, which I
	 * guess is the {@code try(ClosableRdr rdr = new ClosableRdr()){}} syntax,
	 * or with a straight-up <pre>{@code solver.close();}</pre>.
	 * <p>
	 * In addition, each wantedHinter that implements IReporter reports.
	 *
	 * @throws java.io.IOException officially, but never in practice.
	 */
	public void close() throws java.io.IOException {
		// report if we are not in the GUI (ie batch, test-cases, or whatever).
		// If we are in the GUI then report if we are -ea (used by techies).
		if ( !Run.isGui() || Run.ASSERTS_ENABLED )
			report();
		for ( IHinter h : wantedHinters )
			if ( h instanceof Closeable )
				try {
					((Closeable)h).close();
				} catch (IOException eaten) {
					// Do nothing!
				}
		CFG.close();
	}

	// ========================================================================
	//
	// No cheating!
	//
	// ========================================================================

	/**
	 * The public cheat method points the user towards available hints, but
	 * allows the user to work-out what these interesting cell-values mean,
	 * so a cheat is sort-of a hint-lite, but not all hint-types are covered.
	 * <p>
	 * All a cheat does is run the hinter specified by the 'key' column label:
	 * A..I for the left-mouse button, then a..i for the right-mouse button,
	 * and parse the hint/s (if any) into a Pots, which my caller (the GUI)
	 * paints. I return null if no hints are found, or the hinter is not found,
	 * or something goes wrong, so I may return null, but never an empty Pots.
	 * Never say never.
	 * <p>
	 * Implementing this in an inner-class was harder than I expected. It took
	 * me several tries to get it to work, and it was so ugly that I decided to
	 * obfuscate it, to hide my shame; but I have since decided to de-obfuscate
	 * it for universal edification, that and obfuscation stopped being fun and
	 * started to become just obfuscation.
	 * <p>
	 * Note well that any RuntimeException experienced here-in propagates out
	 * to my caller, so handling them is upto my caller!
	 *
	 * @param grid to find cheat-hints in
	 * @param key a letter identifying which cheat to run, starting with the
	 *  column headers A..I, and continuing with the right mouse-button a..i
	 * @return Pots to paint, else null (never empty please)
	 */
	public Pots cheat(final String key, final Grid grid) {
		return cheats().execute(key, grid);
	}

	/**
	 * Get THE instance of Cheats, which is lazy-loaded, that is construction
	 * is delayed until the {@link #cheat(String, Grid)} method is called
	 * if/when the user wants to cheat (long after LogicalSolver constructed).
	 * <p>
	 * Previously I had a static Cheats instance, but the LogicalSolverBuilder
	 * has to then construct two LogicalSolvers, one for itself, and one just
	 * for the static Cheats. FYI, I found this in stack-traces produced by
	 * {@code diuf.sudoku.solver.hinters.als.RccFinderFactory#get}. What is
	 * going on under the hood is quite interesting. The first reference to
	 * LogicalSolver is the static constructHinter method, so a static CHEATS
	 * constructs Cheats then, creating each hinter, whereas delaying Cheats
	 * construction means this LogicalSolver pre-exists, so Cheats uses his
	 * hinters. This is a big difference for a simple code-change to avoid a
	 * big trap for young players that I fell into. Guess I am a bit thick.
	 *
	 * @return THE instance of cheats, which is lazy-loaded.
	 */
	private Cheats cheats() {
		if ( cheats == null )
			cheats = new Cheats(this);
		return cheats;
	}

	public String[] cheatNames() {
		return cheats().names();
	}

	/**
	 * The Cheats class (pretty-much) encapsulates SudokExplainers cheats.
	 * A "cheat" is just a short-hand hint. All Cheats does is get the hinter
	 * for a tech, run it, and parse resulting hint/s into a Pots, which my
	 * caller (the GUI) just displays in the grid. Sounds easy enough.
	 */
	private static class Cheats {
		/**
		 * A Parser knows what-colored highlighted-cell-values in the resulting
		 * hint/s (if any) are added to the 'result' Pots. Each Parser parses
		 * the given 'hint' adding "these-colored" highlighted-cell-values to
		 * the 'result' Pots. The 'grid' parameter is needed only to getBlues
		 * from the hint, but using lambda expressions requires it to be passed
		 * into all Parser's.
		 */
		private interface Parser {
			Pots parse(final AHint hint, final Pots result, final Grid grid);
		}

		/** The GREEN parser. */
		private static final Cheats.Parser GREEN
				= (final AHint hint, final Pots result, final Grid grid) -> {
			result.addAll(hint.getGreenPots(0));
			return result;
		};
		/** The GREEN_ORANGE parser. */
		private static final Cheats.Parser GREEN_ORANGE
				= (final AHint hint, final Pots result, final Grid grid) -> {
			result.addAll(hint.getGreenPots(0));
			result.addAll(hint.getOrangePots(0));
			return result;
		};
		/** The GREEN_BLUE parser. */
		private static final Cheats.Parser GREEN_BLUE
				= (final AHint hint, final Pots result, final Grid grid) -> {
			result.addAll(hint.getGreenPots(0));
			result.addAll(hint.getBluePots(grid, 0));
			return result;
		};

		/**
		 * The Cheat enum defines each available cheat. It maps each key to a
		 * Tech and a Parser. The key is A..I (left-button) and a..i (right).
		 * <p>
		 * Each Cheat Tech has a non-null className (a default implementation)
		 * so that Cheats can create an instance of className if (and only if)
		 * the tech is not currently wanted by the user. If tech is not wanted
		 * and tech.className is null then this Cheat is permanently disabled,
		 * silently: there is no logging, because I just do not care, but that
		 * makes Cheats harder to debug, so you need to mind how you go. Just
		 * ensure that Tech has a non-null className if/when you create the
		 * Cheat for it, and if it cannot have a default implementation then
		 * think real-hard about the benefits of implementing a bloody cheat
		 * for the bastard. All things are possible, it is just that some are
		 * are not worth it.
		 * <p>
		 * The parser defines color/s of highlighted cell-values to display.
		 */
		private static enum Cheat {
			// left mouse button => column 'A'..'I'
			  A(Tech.NakedSingle, null) {
				@Override
				String techName() { return "Naked/HiddenSingle"; }
			  }
			, B(Tech.Locking)
			, C(Tech.NakedPair)
			, D(Tech.HiddenPair)
			, E(Tech.NakedTriple)
			, F(Tech.HiddenTriple)
			, G(Tech.Swampfish)
			, H(Tech.TwoStringKite, GREEN_BLUE)
			, I(Tech.XY_Wing, GREEN_ORANGE) {
				@Override
				String techName() { return "XY/Z_Wing"; }
			  }
			// right mouse button => column 'a'..'e'
			, a(Tech.W_Wing, GREEN_BLUE)
			, b(Tech.Swordfish)
			// skip coloring coz there hints cannot be rendered as a cheat
			, c(Tech.Skyscraper)
			, d(Tech.EmptyRectangle, GREEN_BLUE)
			, e(Tech.Jellyfish)
			, f(Tech.NakedQuad)
			, g(Tech.HiddenQuad)
			, h(Tech.BigWings, GREEN_ORANGE) // S?T?U?V?WXYZ_Wing
			, i(Tech.URT, GREEN_BLUE)
			;
			// the Tech-to-run
			private final Tech tech;
			// which color cell-values to return
			private final Parser parser;
			// the all-args actual constructor
			private Cheat(final Tech tech, final Parser parser) {
				this.tech = tech;
				this.parser = parser;
			}
			// default to the green Parser, which is the most common
			private Cheat(final Tech cheat) {
				this(cheat, GREEN);
			}
			/**
			 * The Cheat enum exists for this run method to pass the Tech and
			 * Parser for this Cheat to the {@link Cheats#run} method; so
			 * effectively Cheat just maps each key to a Tech and a Parser!
			 *
			 * @param cheats the containing instance of Cheats is passed-in
			 *  because an enum has no implicit Container.this, to allow me
			 *  to call-back {@link Cheats#run}
			 * @param grid the grid to search
			 * @return the Pots to be painted, else null meaning:
			 *  (usual) the hinter did not find any hints,
			 *  or (bug) no hinter for this tech,
			 *  or (tilt) s__t happened
			 */
			final Pots run(final Cheats cheats, final Grid grid) {
				return cheats.run(tech, parser, grid);
			}
			/**
			 * The name of the Technique for this Cheat.
			 * @return tech.name()
			 */
			String techName() {
				return tech.name();
			}
		}

		// a reverse-lookup-cache Tech=>Hinter of each Cheatable hinter
		private static final Map<Tech,IHinter> HINTERS
				= new EnumMap<>(Tech.class);

		// parse the given 'hints' (not null/empty) into a new Pots using the
		// given 'parser'. The 'grid' is needed only to hint.getBlues. sigh.
		private static Pots parse(final Iterable<AHint> hints
				, final Parser parser, final Grid grid) {
			final Pots result = new Pots();
			hints.forEach((h)->parser.parse(h, result, grid));
			return result;
		}

		// Cheats is a static-inner-class in order to have the internal enum's
		// I need to map each cheat-acronym to it is Tech and Parser; so then I
		// need a "custom" explicit reference to LogicalSolver.this that is
		// normally implicit in a non-static-inner-class. sigh.
		private final LogicalSolver ls;

		// Constructor.
		private Cheats(final LogicalSolver ls) {
			this.ls = ls;
		}

		/**
		 * names are displayed when cheats are password authorised.
		 * Cheats are organised A..I (left) then a..i (right)
		 * so names are: "$left / $right"
		 * and we need an empty first element for to-left-of-the-grid
		 *
		 * @return a new array of the names of the Cheats
		 */
		private String[] names() {
			// names are: "$left / $right"
			final EnumSet<Cheat> cheats = EnumSet.allOf(Cheat.class);
			final int n = cheats.size() / 2;
			final String[] names = new String[n + 1];
			names[0] = ""; // first element is "", and the rest move down 1
			int i = 0;
			for ( Cheat c : cheats ) {
				if ( i < n ) { // A..I (left-button)
					names[i + 1] = c.techName();
				} else { // a..i (right-button)
					names[i%n + 1] += " / " + c.techName();
				}
				++i;
			}
			return names;
		}

		// execute is the top-level method in cheats, it runs the hinter for
		// 'key' Cheat over this 'grid', and parses the resulting hint/s (if
		// any) into the returned Pots, else null (ie never an empty Pots).
		private Pots execute(final String key, final Grid grid) {
			// this switch handles the ODD_BALL cheats
			switch ( key ) {
				// ODD_BALL: run both NakedSingle and HiddenSingle
				case "A": return runSingles(grid);
				// ODD_BALL: run both XY_Wing and XYZ_Wing
				case "I": return runXyzWing(grid);
				// run 'key' Cheat over the grid.
				default: return Cheat.valueOf(key).run(this, grid);
			}
		}

		// ODD_BALL: run both NakedSingle and HiddenSingle over the 'grid',
		// and parse any resulting hint/s into a new Pots to return, by
		// reading the set-cell-values (not the more usual eliminations).
		private Pots runSingles(final Grid grid) {
			final LinkedList<AHint> hints = new LinkedList<>();
			final HintsAccumulator hacu = new HintsAccumulator(hints);
			if ( ls.nakedSingle.findHints(grid, hacu)
			   | ls.hiddenSingle.findHints(grid, hacu) )
				return new Pots(hints);
			return null;
		}

		// ODD_BALL: run both XY_Wing and XYZ_Wing over the 'grid', and parse
		// any resulting hints into a new Pots to return.
		private Pots runXyzWing(final Grid grid) {
			final LinkedList<AHint> hints = new LinkedList<>();
			final HintsAccumulator hacu = new HintsAccumulator(hints);
			boolean any = false;
			final IHinter xyWing = getHinterFor(Tech.XY_Wing);
			if ( xyWing != null )
				any |= xyWing.findHints(grid, hacu);
			final IHinter xyzWing = getHinterFor(Tech.XYZ_Wing);
			if ( xyzWing != null )
				any |= xyzWing.findHints(grid, hacu);
			// nb: use Cheat.I.parser for single point of truth
			return any ? parse(hints, Cheat.I.parser, grid) : null;
		}

		/**
		 * The "generic" run method gets the hinter that implements 'tech'
		 * and runs it over the 'grid', and parses the resulting hint/s
		 * into a Pots using the given 'parser', which is returned.
		 * <p>
		 * I am called by {@link Cheat#run} to provide the 'tech' and 'parser'
		 * appropriate for this cheat.
		 *
		 * @param tech I get the hinter implementing tech, upon which I call
		 *  the findHints methods, to find any available hints in the grid
		 * @param parser that parses any hint/s into a Pots
		 * @param grid to search for hints
		 * @return a new Pots containing all cell-values to paint in the GUI,
		 *  else null means (usual) hint not found, or (bug) hinter not found,
		 *  or (tilt) s__t happened; so possibly null, but never empty. Note
		 *  that any runtime exception in findHints or parse propagates out.
		 */
		private Pots run(final Tech tech, final Parser parser, final Grid grid) {
			final IHinter hinter;
			if ( (hinter=getHinterFor(tech)) != null ) {
				final LinkedList<AHint> hints = new LinkedList<>();
				if ( hinter.findHints(grid, new HintsAccumulator(hints)) )
					return parse(hints, parser, grid);
			}
			return null; // no hint, or no hinter, or tilt
		}

		// get the hinter that implements 'tech', else null (Cheat disabled)
		private IHinter getHinterFor(final Tech tech) {
			// first-time only: setup the HINTERS map
			if ( HINTERS.isEmpty() ) {
				// build reverse-lookup-cache: Tech=>Hinter
				cache = new EnumMap<>(Tech.class);
				// add the basics to the cache, in case they are not wanted
				cache.putAll(ls.basicHinters);
				// add each wanted hinter to the cache
				for ( IHinter hinter : ls.wantedHinters )
					cache.put(hinter.getTech(), hinter);
				// add each Cheat's hinter to HINTERS
				// if a Cheat hinter is not in the cache then we attempt to
				// construct our own, which fails if tech.className is null,
				// so this Cheat is permanently and silently disabled.
				EnumSet.allOf(Cheat.class).forEach((c) ->
						HINTERS.put(c.tech, cached(c.tech)));
				// we are done with the cache, which is used only to
				// construct the HINTERS, limited to Cheats. sigh.
				cache = null;
			}
			return HINTERS.get(tech); // null means: Cheat disabled
		}
		// reverse-lookup-cache: Tech=>Hinter of the wantedHinters
		private EnumMap<Tech,IHinter> cache;

		/**
		 * Get the hinter that implements 'tech' from the reverse-lookup-cache,
		 * else constructHinter, which my caller adds to the HINTERS Map, even
		 * if it is null, so we do all this ONCE, disabling any AWOL hinters.
		 *
		 * @param tech you want the hinter for
		 * @return the hinter that implements 'tech', else null meaning that
		 *  constructHinter failed, so this cheat is disabled
		 */
		private IHinter cached(final Tech tech) {
			IHinter hinter;
			if ( (hinter=cache.get(tech)) != null )
				return hinter;
			return constructHinter(tech);
		}

	}

	/**
	 * Present LogicalSolverBuilder.constructHinter to public,
	 * because LogicalSolverBuilder is not public.
	 *
	 * @param tech to construct a new hinter for
	 * @return a new instance of the hinter that implements Tech
	 */
	public static final IHinter constructHinter(Tech tech) {
		return LogicalSolverBuilder.constructHinter(tech);
	}

}
