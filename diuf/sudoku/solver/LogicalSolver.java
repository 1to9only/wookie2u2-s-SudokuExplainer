/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

// Sudoku Explainer (local) classes
// Tip: .* import local classes, but NEVER foreigners.
// You can't change foreign names to resolve collisions.
import diuf.sudoku.*;
import static diuf.sudoku.Grid.GRID_SIZE;
import static diuf.sudoku.Settings.THE_SETTINGS;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.utils.*;
import static diuf.sudoku.utils.Frmt.*;
import static diuf.sudoku.utils.MyStrings.BIG_BFR_SIZE;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A LogicalSolver solves Sudoku puzzles using logic, and does so as quickly as
 * possible (as I'm able). The challenge of doing this is what the whole Sudoku
 * Explainer project is about.
 * <p>
 * This version of Sudoku Explainer/HoDoKu/Sukaku/Sudopedia was written "just
 * for fun" and the author (Keith Corlett) feels he must humbly apologise for
 * producing rather a lot of code which is pretty hard to maintain because it
 * does NOT, by design, follow the edicts of any modern Java coding ethos. It's
 * written to run fast, not to be maintainable. Oops, I've just done a hammie.
 * <p>
 * If you're impatient then in the GUI (Options ~ Solving Techniques):<ul>
 * <li>keep means ticked, and drop means unticked. If it's not listed then you
 *  had better err on the side of caution and keep it. All current hinters are
 *  listed, so it will only be new hinters that could be missing.
 * <li>keep (tick) Locking (fast) instead of Locking Generalised (slower)
 * <li>drop (untick) Direct* for speed, because they're found anyway by there
 *  non-direct equivalents. Direct just sets the cell directly, that's all.
 * <li>keep Naked/Hidden Pair/Triple, Swampfish, Two String Kite, XY-Wing,
 *  XYZ-Wing, W-Wing, Swordfish, Skyscraper, and Empty Rectangle
 * <li>drop Jellyfish finds none if you follow this spec to the letter.
 *  In fact all *Jellyfish are pretty slow, so drop them all for speed.
 * <li>keep Coloring (only Multi), XColoring (GEM misses), and GEM (ultimate)
 * <li>keep Naked/Hidden Quad
 * <li>drop Naked/Hidden Pent they're degenerate, which means all patterns they
 *  found are found earlier by simpler techniques, so they find nothing
 * <li>keep BigWings (slower than S/T/U/V/WXYZ-Wing but faster overall because
 *  most of it's runtime is building the ALS cache that's reused by AlsXz,
 *  AlsWing and AlsChain) If you drop Als* then swap back to T/U/V/WXYZ-Wing,
 *  but I'd still drop the STUVWXYZ-Wing coz it's a little beet slow. But hey,
 *  it only took three hours to rub one out last Tuesday! Just kidding.
 * <li>drop WXYZ-Wing, VWXYZ-Wing, UVWXYZ-Wing, TUVWXYZ-Wing
 * <li>drop STUVWXYZ-Wing because it's a bit slow (border-line)
 * <li>keep URT (Unique Rectangles and Loops)
 * <li>keep FinnedSwampfish and FinnedSwordfish
 * <li>drop FinnedJellyfish for speed because it's a bit slow (border-line)
 * <li>keep ALS-XZ, Als-Wing, and Als-Chain they're all fast enough now
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
 *  Nishio is border line; they're all found (arguable faster) by Multiple.
 * <li>keep DynamicPlus as a catch-all for normal people, and/or NestedUnary
 *  as the catch-all for the <u>hardest</u> possible Sudoku puzzles
 * <li>drop the rest of Nested*. They're never called in anger, so it doesn't
 *  matter til you Shft-F5 to find MORE hints. They take distant monarch ages.
 * <li>My definition of "slow" is 100 milliseconds per elimination
 * <li>My definition of "too slow" is a second per elimination
 * <li>My definition of "far too slow" is about 10 seconds per elimination
 * <li>All of these hinters are as fast as I know how to make them, which is
 *  pretty fast, except the A*E debacle, but I cannot preclude the possibility
 *  that it can be done faster. Have a go if you're smart and have a few years
 *  to waste, but I warn you that gambling is addictive, so best left for
 *  retirement.
 * </ul>
 * <p>
 * <b>LogicalSolver is used to:</b><ul>
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
 *  package. Validity is checked ASAP because it's quicker to fail fast.
 * <li>The solving techniques are under the {@code diuf.sudoku.solver.hinters}
 *  package. There's too many to list, so I mention the odd-balls only: <ul>
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
 * <li>There's a bunch of crap in the utils package. Some of it might even be
 *  worth re-using.
 * </ul>
 * <p>
 * <b>On HoDoKu:</b><ul>
 * <li>The original Sudoku Explainer was heavily dependent on Chainers, so I
 *  boosted HoDoKu's Solving Techniques. In the GUI each such technique is
 *  denoted with "HoDoKu $TechniqueName". Kudos to hobiwan. Mistakes are mine.
 * <li>I actually recommend HoDoKu (my release is a bit faster, especially for
 *  expert puzzles) over Sudoku Explainer for "normal people" because it has a
 *  nicer GUI. I'm not a GUI-programmers-bung-hole, and I know it. My stick is
 *  efficiency, so I'm a better fit on the server-side of life.
 * <li>Bernhard Hobiger (hobiwan) is my hero, partly because he has a seriously
 *  cool moniker, and mostly because he was (he has sadly passed) a genius.
 *  KrakenFisherman was based on hobiwans ideas, and it's serious smart.
 * <li>Here's all the AHinters that where "boosted" from HoDoKu:<ul>
 *  <li>EmptyRectangle
 *  <li>Skyscraper
 *  <li>TwoStringKite
 *  <li>WWing
 *  <li>Coloring
 *  <li>AlsXz
 *  <li>AlsXyWing
 *  <li>AlsXyChain
 *  <li>SueDeCoq
 *  <li>DeathBlossom
 *  <li>ComplexFisherman for Finned, Franken, and Mutant fish.
 *  <li>KrakenFisherman for Kraken bananas, and fish, easy on the bananas.
 *  </ul>
 * </ul>
 * <b>On Sukaku:</b><ul>
 * <li>Some of the hinters here-in draw from Sukaku. BigWing/BigWings started
 *  life in Sukaku cluster____eese, and are now about 100 times faster, and all
 *  in maybe 100'th of the code. You have been warned.
 * </ul>
 * <b>On sudopedia.org:</b><ul>
 * <li>Some of the hinters here-in draw from sudopedia.org which IMHO is the
 *  best free information source for Sudoku solving. GEM is a prime example.
 * </ul>
 * <b>On SudokuMonster:</b><ul>
 * <li>Some of the hinters here-in draw from SudokuMonster, but the inherited
 *  code was a bit s__t, on an expert scale. I wouldn't recommend it.
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
 *  is a bit of a pain-in-the-ass, but I think it's worth it.<br>
 *  <br>
 *  In Nutbeans: run all JUnit tests with ALT-F6. They take about two minutes.
 *  These are mostly regression tests, which diff the output to a saved-html,
 *  so anytime you change just about anything you'll also need to update it's
 *  test-case. These tests pick-up any unintended consequences. Some classes
 *  do NOT yet have test-cases, coz I'm lazy. It's a work in progress.
 * <li>And then I use {@link diuf.sudoku.test.LogicalSolverTester} with
 *  {@code Log.MODE = VERBOSE_4_MODE} to performance test the hinters on
 *  {@code top1465.d5.mt}, which is in the root directory of this project.
 *  This currently takes about 2 minutes to run, sans slow hinters.
 * </ul>
 * <p>
 * @see diuf.sudoku.BuildTimings for batch timings over top1465.
 */
public final class LogicalSolver {

	/** The ANALYSE_LOCK synchronises {@link diuf.sudoku.gen.PuzzleCache}
	 * with {@link LogicalAnalyser}, so that one solve runs at a time. */
	public static final Object ANALYSE_LOCK = new Object();

	/** Returned by {@link #analyseDifficulty(diuf.sudoku.Grid, double) }
	 * when the process was interrupted by the user. */
	public static final double ANALYSE_INTERRUPTED = 999.99; // an impossibly high number

	/**
	 * Constructs and returns a new instance of ${className} which implements
	 * the given 'tech', passing the 'tech' if the constructor takes it.
	 * <p>
	 * I'm package visible for use by LogicalSolverBuilder. I'm static because
	 * I do not reference the current instance, if any.
	 * <p>
	 * This method is design for use with a "simple" hinter, which has ONE ONLY
	 * constructor, which may take a Tech parameter. Anything more complex gets
	 * hard, so use {@link #want(IHinter) the other want method} instead.
	 * I just use the "first" constructor (according to reflections).
	 * <p>
	 * FYI: I'm bound to simple hinters because I'm too dumb to work-out how to
	 * choose between constructors with multiple parameter-types. It's possible
	 * to expand the definition of simple to other parameters, BUT before doing
	 * so be sure it's for many (not just one) hinters, and try to NOT pass it
	 * as a constructor parameter: often a simple setter works, even if its not
	 * good design. Most final fields do not actually NEED to be final. Can you
	 * add the wanted-parameter to the Tech enum?
	 *
	 * @param tech the Tech parameter to the constructor
	 * @param className the full name of the IHinter class to be constructed
	 * @return a new instance of ${className} which must implement IHinter
	 */
	static IHinter constructHinter(final Tech tech, final String className) {
		try {
			final Class<?> clazz = Class.forName(className);
			// find the constructor
			final Constructor<?>[] cons = clazz.getConstructors();
			if ( cons==null || cons.length == 0 ) {
				throw new IllegalArgumentException("no constructors");
			}
			// classes with multiple constructors aren't handled competently.
			if ( cons.length > 1 ) {
				Log.teeln(Log.me()+": WARN: multiple constructors: "
						  +clazz.getSimpleName());
			}
			// get and execute the first (according to reflections) constructor
			Constructor<?> con = cons[0];
			final Class<?>[] params = con.getParameterTypes();
			if ( params.length == 0 ) {
				return (IHinter)con.newInstance();
			} else if ( params[0] == Tech.class ) {
				if ( tech == null ) {
					throw new IllegalArgumentException("needed tech is null");
				}
				return (IHinter)con.newInstance(tech);
			}
			throw new IllegalArgumentException("unknown param: "+params[0]);
		} catch (Exception ex) {
			Log.teeTrace(Log.me()+"("+className+", "+tech+") failed!", ex);
			return null;
		}
	}
	static IHinter constructHinter(final Tech tech) {
		return constructHinter(tech, tech.className);
	}

	/**
	 * NAME is a lambda returning the hinter name.
	 */
	private static final IFormatter<IHinter> NAME = (IHinter h) ->
			h==null ? NULL_ST : h.toString();

	/**
	 * ENABLED is a lambda returning is this hinter enabled.
	 */
	private static final IFilter<IHinter> ENABLED = (IHinter h)->h.isEnabled();

	// NOTE: We must create the basic hinters before the validators.
	// First the singles: NakedSingle and HiddenSingle;
	// then Four Quick Foxes: Locking, NakedPair, HiddenPair and SwampFish.
	// then BruteForce (a validator) which uses the basic hinters
	// NOTE: Locking uses HiddenPair and HiddenTriple.

	/** A cell with only one potential value remaining is that value. */
	private final NakedSingle nakedSingle;

	/** The only place in a region which maybe a value is that value. */
	private final HiddenSingle hiddenSingle;

	/** If the only cells in a region which maybe v also share another common
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
	 * LockingGen doesn't. It's a more elegant solution, that's slower. */
	private final Locking locking;

	/** siameseLocking used in the GUI, optional in LogicalSolverTester. */
	private final SiameseLocking siameseLocking;

	// NOTE: validators are created AFTER the hinters coz BruteForce uses
	// the Four Quick Foxes {NakedSingle, HiddenSingle, Locking, Swampfish}
	// to solve logically, which is faster than just guessing cell-values
	// that can be quickly proven invalid.

	/** puzzle validator: Does this puzzle have at least 17 clues?. */
	private final TooFewClues tooFewClues = new TooFewClues();

	/** puzzle validator: Does this puzzle contain 8 values?. */
	private final TooFewValues tooFewValues = new TooFewValues();

	/** puzzle validator: Does this puzzle have ONE solution? <br>
	 * NB: BruteForce uses the basic hinters, so constructed after them. */
	private final BruteForce bruteForce;

	/**
	 * The generic "Sudoku unsolvable" Hint. There's ONE public instance, for
	 * speed, rather than everybody rolling-there-own on the fly.
	 */
	public final AHint unsolvableHint;

	/** puzzleValidators check the puzzle conforms to the Sudoku rules ONCE. */
	private final IHinter[] puzzleValidators;

	/** gridValidators check the Sudoku grid is still valid after each hint. */
	private final IHinter[] gridValidators = new IHinter[] {
		  new DoubleValues()	 // one value per box, row, col
		, new MissingMaybes()	 // no value and no maybes
		, new HomelessValues() // no places for unplaced value
	};

	/** All of the validators. */
	private final IHinter[] validators;

	/**
	 * wanted is an array of IHinter which the user currently wants.
	 * <p>
	 * An array coz native-array-iterator is MUCH faster than an Iterator.
	 */
	public final IHinter[] wanted;

	/**
	 * A CSV String of the names of 'unwanted' Techs, for reporting only.
	 */
	private final EnumSet<Tech> unwanted;

	/** solve and apply both detect deadCats: hints which do not set a Cell and
	 * and do not remove any maybes. If a deadCat is seen twice then presume we
	 * are at the start of an infinite loop in solve/generate/whatever (finding
	 * the same eliminationless hint endlessly) so disable the hinter, which is
	 * re-enabled at the start of the next puzzle by prepare.
	 * NB: "funky" means that add is an addOnly operation, which by design does
	 * not update existing elements. */
	private final Set<AHint> deadCats = new MyFunkyLinkedHashSet<>(16, 0.25F);

	/**
	 * The Sudoku has been solved (a generic message).
	 */
	public final AHint solvedHint = new SolvedHint();

	/** A set of "basic hinters" (singles + Four Quick Foxes). */
	private final Map<Tech,IHinter> basics;

	/**
	 * singlesSolution is weird: it's set by solveWithSingles and then read
	 * back by SudokuExplainer.getTheNextHint.
	 */
	public Grid singlesSolution;

	/**
	 * Constructor sets-up this LogicalSolver to solve logically. All the hard
	 * work is done by {@link LogicalSolverBuilder#build} before I am called.
	 * <p>
	 * NOTE: the bruteForce validator uses the "basic hinters" so it's creation
	 * is delayed until this constructor, which in turn delays the creation of
	 * everything that uses bruteForce.
	 *
	 * @param basics the "basic hinters" {NakedSingle, HiddenSingle, Locking,
	 *  NakedPair, HiddenPair, Swampfish} are created even if not wanted, for
	 *  use in LogicalSolver, BruteForce, and the chainers
	 * @param wanted an array of those IHinter that are wanted by the user.
	 *  Note that this is an array for iteration speed: top1465 batch is about
	 *  30 seconds (ie s__tloads) slower with any sort of collection
	 * @param unwanted a set of the unwanted techs, for reporting only
	 */
	LogicalSolver(final Map<Tech,IHinter> basics, final IHinter[] wanted
			, final EnumSet<Tech> unwanted) {
		this.basics = basics;
		this.wanted = wanted;
		this.unwanted = unwanted;
		// weird: each hinter knows it's index in the wantedHinters array,
		// so that LogicalSolverTester can log hinters in execution order.
		for ( int i=0,n=wanted.length; i<n; ++i ) {
			wanted[i].setIndex(i);
		}
		// bruteForce uses the "basic hinters", so it's creation is delayed,
		// which in turn delays creation of all dependant artifacts. sigh.
		bruteForce = new BruteForce(basics);
		unsolvableHint = new WarningHint(bruteForce, "Unsolvable", "Unsolvable.html");
		puzzleValidators = new IHinter[] {
			  tooFewClues		// minimum 17 clues
			, tooFewValues		// minimum 8 values
			, bruteForce		// ONE solution
		};
		validators = new IHinter[] {
			  puzzleValidators[0], puzzleValidators[1], puzzleValidators[2]
			, gridValidators[0], gridValidators[1], gridValidators[2]
		};
		// set-up individual hinters
		nakedSingle = (NakedSingle)basics.get(Tech.NakedSingle);
		hiddenSingle = (HiddenSingle)basics.get(Tech.HiddenSingle);
		locking = (Locking)basics.get(Tech.Locking);
		siameseLocking = new SiameseLocking(basics);
		siameseLocking.index = locking.index; // not used if 0 (unwanted)
	}

	/**
	 * @return the existing map containing the basic hinters.
	 */
	public Map<Tech, IHinter> getBasicHinters() {
		return basics;
	}

	/**
	 * Set-up this LogicalSolver to use SiameseLocking instead of Locking,
	 * because LogicalSolverTester needs the option to use SiameseLocking,
	 * in order to find instances of any bugs in SiameseLocking.
	 * <p>
	 * if Locking (not LockingGen) then replace Locking with SiameseLocking.
	 * NB: setSiamese is ignored when LockingGen is wanted coz that means basic
	 * locking (precluding SiameseLocking); that and locking.index remains 0,
	 * so I replace NakedSingle with SiameseLocking, which is just plain wrong.
	 */
	public void setSiamese() { // wax on
		if ( locking.index != 0 ) {
			wanted[locking.index] = siameseLocking;
		}
	}

	/**
	 * Revert to standard locking (not siamese).
	 */
	public void clearSiamese() { // wax off
		if ( locking.index != 0 ) {
			wanted[locking.index] = locking;
		}
	}

	/**
	 * Disable a useless prick, I mean tech. No, I do mean prick. sigh.
	 *
	 * @param tech to disable
	 */
	public void disable(final Tech tech) {
		final IHinter h = getWantedHinter(tech);
		if ( h != null ) {
			h.setIsEnabled(false);
		}
	}

	/**
	 * Disable these useless pricks, I mean techs. No, I do mean pricks. sigh.
	 *
	 * @param techs to disable
	 */
	public void disable(final Tech[] techs) {
		for ( Tech tech : techs )
			disable(tech);
	}

	/**
	 * @return CSV of wanted hinters for logging
	 */
	public final String getWanteds() {
		final StringBuilder sb = new StringBuilder(512);
		sb.append("Wanted: ");
		Frmt.appendTo(sb, wanted, NAME, CSP, CSP);
		return sb.toString();
	}

	/**
	 * @return CSV of the wanted enabled hinters for logging
	 */
	public String getWantedEnableds() {
		try {
			final StringBuilder sb = new StringBuilder(512);
			sb.append("WantedEnabled: ");
			Frmt.appendTo(sb, wanted, ENABLED, NAME, CSP, CSP);
			return sb.toString();
		} catch (IOException ex) {
			return "WantedEnabledHinters: "+ex;
		}
	}

	/**
	 * @return CSV of the unwanted tech.names for logging
	 */
	public String getUnwanteds() {
		return "Unwanted: " + unwanted.toString().replaceFirst("\\[", "")
				.replaceFirst("\\]", "");
	}

	/** Called after the puzzle is solved. */
	public void after() {
		for ( IHinter h : wanted ) {
			if ( h instanceof IAfter ) {
				((IAfter)h).after();
			}
		}
	}

	/** Called before the GC runs. */
	public void cleanUp() {
		for ( IHinter h : wanted ) {
			if ( h instanceof ICleanUp ) {
				((ICleanUp)h).cleanUp();
			}
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
	 * (Default)HintsAccumulator then my behaviour is "interesting" (I don't
	 * really understand what happened) so now I insist on a SingleHA.
	 * @return true if a hint was found, else false; always true AFAIK.
	 */
	public boolean getFirstHint(final Grid grid, final SingleHintsAccumulator accu) {
		if ( grid.numSet > 80 ) {
			accu.add(solvedHint);
			return true;
		}
		if ( getFirstHint(validators, grid, accu, false)
		  || getFirstHint(wanted, grid, accu, false) ) {
			grid.hintNumberIncrement();
			return true;
		}
		return false;
	}

	//<NO_WRAPPING>
	/**
	 * getAllHints: Get all the hints from this Grid; it's just that "all" is
	 * defined by wantMore (does the user want to see more hints?).
	 * <p>
	 * If a validator hints we're done regardless of what the user wants. <br>
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
	 * @param logHints should I log hints.
	 * @param printHints should I print hints to stdout.
	 * @return List of AHint
	 */
	public List<AHint> getAllHints(final Grid grid, final boolean wantMore, final boolean logHints, final boolean printHints) {
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			Log.println();
			final String more; if(wantMore) more=" MORE"; else more="";
			final String src; if(grid.source!=null) src=grid.source.toStringShort(); else src="IGNOTO";
			// this is just noise in stdout (but I still want to log it)!
			Log.format("\n>getAllHints%s %d/%s\n", more, grid.hintNumber, src);
			Log.println(grid);
		}
		final LinkedList<AHint> hints = new LinkedList<>();
		// this doesn't appear to ever be triggered! So I guess we no longer
		// get here when the grid is already full, but I'm leaving it.
		if ( grid.numSet > 80 ) {
			hints.add(solvedHint);
			return hints; // empty if I was interrupted, or didn't find a hint
		}
		final long start = Run.time = System.nanoTime();
		// HintsAccumulator.add returns false so hinter keeps searching,
		// adding each hint to the hints List.
		final IAccumulator accu = new HintsAccumulator(hints);
		// get hints from hinters until one of them returns true (I found a
		// hint) OR if wantMore then run all hinters. nb: DyamicPlus nearly
		// always finds a hint. NestedUnary ALWAYS finds a hint.
		boolean any = getFirstHint(validators, grid, accu, false);
		if ( !any ) {
			// do siamese locking on F5 in GUI when filtering
			if ( Run.type==Run.Type.GUI
			  && !wantMore
			  // read the CURRENT global Settings, not my immutable copy.
			  // nb: that's why this is done repeatedly in getAllHints, and
			  // not just once when setting-up this LogicalSolver.
			  && THE_SETTINGS.isFilteringHints(false) ) {
				setSiamese();
			}
			// run prepare AFTER validators and BEFORE wantedHinters
			if ( !grid.isPrepared() ) { // once on each grid
				prepare(grid); // prepare wanted IPrepare's to process grid
			}
			any = getAllHints(wanted, grid, accu, !wantMore, logHints);
			// revert to standard locking
			clearSiamese();
		}
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			final long took = System.nanoTime()-start;
			// if there where any hints (there should be)
			if ( any && (logHints || printHints) ) {
				final AHint hint = accu.peek(); // the first hint, else null
				final int numElims = hint.getNumElims();
				if(logHints)Print.gridFull(Log.out, hint, grid, numElims, took);
				if(printHints)Print.gridFull(System.out, hint, grid, numElims, took);
			}
			final String more; if(wantMore) more=" MORE"; else more="";
			final String src; if(grid.source!=null) src=grid.source.toStringShort(); else src="IGNOTO";
			// this is useful in stdout as well as the log
			Log.teef("<getAllHints%s %d/%s %b %,15d\n", more, grid.hintNumber, src, any, took);
		}
		grid.hintNumberIncrement();
		return hints; // empty if I was interrupted, or didn't find a hint
	}
	//</NO_WRAPPING>

	// getHints from the given hinters until we find one, or I'm interrupted so
	// I exit-early returning accu.hasAny(), ie false.
	private boolean getFirstHint(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final boolean logHints) {
		for ( IHinter hinter : hinters ) {
			if ( hinter.isEnabled()
			  && findHints(hinter, grid, accu, logHints) ) {
				break;
			}
		}
		return accu.any();
	}

	// getHints from the given hinters: if less then return when hint found as
	// per getFirst, else getHints from all the hinters, adding them to accu,
	// and then return "were any found?" (which'll allways be true, AFAIK).
	// This method is only called by getAllHints directly, to handle wantLess.
	// Hinters are only called then they are enabled. A Hinter is disabled if
	// it's a very naughty boy and produces the same deadCat twice.
	private boolean getAllHints(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final boolean less
			, final boolean logHints) {
		for ( IHinter hinter : hinters ) {
			if ( hinter.isEnabled()
			  && findHints(hinter, grid, accu, logHints)
			  && less ) {
				break;
			}
		}
		return accu.any() && less;
	}

	// findHints calls the hinter.findHints; log it if isNoisy.
	private boolean findHints(final IHinter hinter, final Grid grid
			, final IAccumulator accu, final boolean logHints) {
		// if we're logging, do it noisily
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			if ( logHints ) {
				Log.format("%-40s ", hinter.getTech().name());
				final long t0 = System.nanoTime();
				final boolean result = hinter.findHints(grid, accu);
				final long took = System.nanoTime() - t0;
				Log.format("%,15d\t%s\n", took, hinter);
				return result;
			}
		}
		// else just do it quietly
		return hinter.findHints(grid, accu);
	}

	/**
	 * enableKrakens dis/enables Krakens, coz they break generate.
	 * <p>
	 * The Generator skips Kraken Swordfish & Jellyfish which bugger-up on null
	 * table entries, and I cannot understand why they are null, which has been
	 * driving me mad, to the point where I decided to just skip the whole damn
	 * mess, leaving the clean-up to someone with a "real" IQ. sigh.
	 *
	 * @param enabled if false then disable. if true then enable.
	 * @param degree the minimum size krakens to dis/enable: 3 (Sword).
	 */
	public void enableKrakens(final boolean enabled, final int degree) {
		for ( IHinter h : wanted ) {
			if ( h instanceof KrakenFisherman && h.getDegree()>=degree ) {
				h.setIsEnabled(enabled);
			}
		}
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
			if ( (hinter=cache.get(tech)) != null ) {
				return hinter;
			}
		} catch (Exception eaten) {
			// Do nothing
		}
		// empty/missing, so add current wanted hinters
		for ( IHinter h : wanted ) {
			try {
				cache.put(h.getTech(), h);
			} catch (Exception eaten) {
				// Do nothing
			}
		}
		// rescue: the old O(n) way (never happens)
		for ( IHinter hinter : wanted ) {
			if ( hinter.getTech() == tech ) {
				cache.put(tech, hinter);
				return hinter;
			}
		}
		return null;
	}
	// a cache to speed-up getWantedHinter
	private final EnumMap<Tech,IHinter> cache = new EnumMap<>(Tech.class);

	/**
	 * analyseDifficulty is used only by Generator to calculate how difficult a
	 * random generated puzzle is. The question that I answer is:
	 * Is this puzzles-difficulty (the max hint difficulty) less than maxD?
	 * <p>
	 * To calculate an answer to this question we find and apply hints to the
	 * grid, stopping at the first hint whose difficulty exceeds <tt>maxD</tt>.
	 * Pretty obviously, there may not be one. I return the puzzle-difficulty,
	 * ie the maximum hint difficulty, even if it's above the given maxD (which
	 * just tells me to stop analysing).
	 * <p>
	 * My caller enforces minD (or not) itself. There is no consideration of
	 * minimum difficulty here, only maximum. To enforce a minimum: if this
	 * puzzles difficulty (the value I return) is below Difficulty.min then
	 * reject this puzzle. I only monitor the maxD, so that I can return as
	 * soon as it's exceeded.
	 * <p>
	 * NB: minD is inclusive and maxD is EXCLUSIVE, so maxD is minD of the next
	 * Difficulty, so always {@code pd < maxD} (not {@code pd <= maxD}).
	 *
	 * @param gridParam to analyse to find the difficulty (the hardest step)
	 * @param maxD the maximum desired difficulty (inclusive), a ceiling that
	 *  stop at if exceeded
	 * @return The puzzles difficulty if it's below the given <tt>maxD</tt>,
	 *  else the arbitrary high value Difficulty.IDKFA.max, currently 100.0.
	 * @throws HinterruptException from AHinter.interrupt() back-up to generate.
	 */
	public double analyseDifficulty(final Grid gridParam, final double maxD) {
		double pd = 0.0D; // puzzleDifficulty, my result
		final Grid grid = new Grid(gridParam);
		final IAccumulator accu = new SingleHintsAccumulator();
		// re-enable all hinters just in case we hit a deadCat last time
		if ( anyDisabled )
			for ( IHinter hinter : wanted )
				hinter.setIsEnabled(true);
		deadCats.clear(); // deal with eliminationless hints
		grid.hintNumberReset(); // Start from the start
		enableKrakens(false, 3); // disable KrakenSwordfish and KrakenJellyfish
//collect hints in-case it goes Unsolvable
//StringBuilder sb = new StringBuilder(8192);
		for ( int pre=grid.totalSize,now; pre>0; pre=now ) { // ie !isFull
			if ( getFirstHint(wanted, grid, accu, false) ) {
				final AHint hint = accu.getHint();
//sb.append(NL)
//  .append(grid).append(NL)
//  .append(hint.toFullString()).append(NL);
//Debug.breakpoint();
				assert hint != null;
				// get the difficulty-rating of this hint
				final double hd = hint.getDifficulty(); // hintDifficulty
				// calculate puzzleDifficulty = maximum hintDifficulty,
				// and if the pd exceeds the maxD then return it
				if ( hd>pd && (pd=hd)>=maxD ) {
					return pd; // max target difficulty EXCLUSIVE exceeded
				}
//				try {
					hint.applyQuitely(false, grid); // NO_AUTOSOLVE
//				} catch (UnsolvableException ex) {
//					if ( Run.type != Run.Type.Generator ) {
//						Log.teeln(Log.me()+": "+ex);
//						Log.teeln(sb);
//					}
//					throw ex;
//				}
				// deal with deadCats: eliminationless hints.
				if ( (now=grid.totalSize)==pre && !deadCats.add(hint) ) {
					anyDisabled = true;
					hint.hinter.setIsEnabled(false);
				}
			} else {
				Log.teeln("analyseDifficulty: No hint found in grid:");
				Log.teeln(grid);
				pd = ANALYSE_INTERRUPTED; // an impossibly large puzzleDifficulty
				break;
			}
		}
		enableKrakens(true, 3);
		return pd;
	}
	private boolean anyDisabled;

	/**
	 * Get the Solution of this puzzle. Used by generate and by the <i>Tools ~
	 * Analyse</i> menu-item in the SudokuFrame. The normal hint returned is
	 * an AnalysisHint that contains a summary of the hints applied.
	 *
	 * @param grid a copy of the Grid to solve (not the GUI's grid)
	 * @param logHints are hints logged.
	 * @param logTimes is EVERY single hinter execution-time logged. <br>
	 *  This is HEAPS verbose, so don't use it in a full batch run!
	 * @return an AHint which is either an AnalysisHint or a WarningHint.
	 */
	public AHint analysePuzzle(final Grid grid, final boolean logHints
			, final boolean logTimes) {
		if ( grid.numSet > 80 ) {
			return solvedHint;
		}
		if (Log.MODE >= Log.NORMAL_MODE) {
			Log.format("\nanalyse: %s\n%s\n", grid.source, grid);
		}
		final long t0 = System.nanoTime();
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		// execute just the puzzle validators.
		if ( getFirstHint(puzzleValidators, grid, accu, false) ) {
			return accu.getHint();
		}
		// LogicalAnalyser.findHints allways returns true, only the returned
		// hint-type varies: solved=AnalysisHint/invalid=WarningHint which are
		// both WarningHints; so take care to differentiate them properly.
		if ( new LogicalAnalyser(this, logHints, logTimes).findHints(grid, accu) ) {
			final long t1 = System.nanoTime();
			final AHint hint = accu.getHint(); // a SolutionHint
			if (Log.MODE >= Log.NORMAL_MODE) {
				Log.format("%,15d\t%2d\t%4d\t%3d\t%-30s\t%s\n"
						, t1-t0, grid.numSet, grid.totalSize
						, hint.getNumElims(), hint.getHinter(), hint);
				if ( hint instanceof AnalysisHint ) {
					log((AnalysisHint) hint);
				}
			}
			return hint;
		}
		return null; // happens after InterruptException
	}

	// Log Difficulty BEFORE appendUsageMap, for consistency with GUI display.
	private void log(final AnalysisHint ah) {
		final StringBuilder sb = new StringBuilder(BIG_BFR_SIZE);
		final String maxD = "Difficulty ##.##"; // largest possible
		sb.append(NL); // leading blank line
		sb.append(maxD).append(NL);
		ah.appendUsageMap(sb);
		sb.append("Total Difficulty ").append(ah.ttlDifficulty)
				.append(NL); // an unusual trailing blank line
		final int i = sb.indexOf(maxD);
		String newMaxD = "Difficulty "+dbl(ah.maxDifficulty);
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
	 * ASolver. Add RecursiveSolver into SudokuExplainer. I'm just too lazy.
	 *
	 * @param grid the puzzle to solve
	 * @return a SolutionHint as an AHint, else null */
	public AHint getSolutionHint(final Grid grid) {
		final IAccumulator accu = new SingleHintsAccumulator();
		// check the puzzle is basically valid
		if ( grid.numSet > 80 ) { // already solved
			return solvedHint;
		}
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) ) { // NumberOfValues >= 8
			return accu.getHint();
		}
		final Grid solution = solve(grid);
		return solution==null ? unsolvableHint
			: new SolutionHint(bruteForce, grid, solution);
	}

	/**
	 * Validate the puzzle only (not the grid).
	 * @param grid
	 * @param accu
	 * @return
	 */
	private boolean validatePuzzle(final Grid grid, final IAccumulator accu) {
		if ( tooFewClues.findHints(grid, accu)	// numClues >= 17
		  || tooFewValues.findHints(grid, accu)	// numValues >= 8
		  || bruteForce.findHints(grid, accu)	// OneSolution
		) {
			return carp("Invalid Sudoku Puzzle: "+accu.getHint(), grid, true);
		}
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
	 * @return the first warning hint, else null. */
	public AHint validatePuzzleAndGrid(final Grid grid, final boolean logHints) {
		final IAccumulator accu = new SingleHintsAccumulator();
		if ( getFirstHint(validators, grid, accu, logHints) ) {
			return accu.getHint(); // problem
		}
		return null; // no problem
	}

	/**
	 * Checks the validity of the given Grid, using just the grid validators.
	 * If quite then there's no logging, else noisy.
	 * <p>
	 * Call validateGrid after applying each hint.
	 *
	 * @param grid Grid to validate.
	 * @param logHints true to log, false to shut the ____ up.
	 * @return the first warning hint, else null. */
	public AHint validateGrid(final Grid grid, final boolean logHints) {
		final IAccumulator accu = new SingleHintsAccumulator();
		if ( getFirstHint(gridValidators, grid, accu, logHints) ) {
			return accu.getHint(); // problem
		}
		return null; // no problem
	}

	/**
	 * Solve the puzzle in 'grid' logically populating 'usageMap' and return
	 * was it solved.
	 * <p>
	 * In English: Solves the given Sudoku puzzle (the 'grid') using logic (the
	 * hinters) and populate the given UsageMap with a summary of hinters used,
	 * how often, and how long each took to run; returning Did It Solve, always
	 * true for valid puzzles. Invalid puzzles throw an UnsolvableException, so
	 * solve never returns false. Never say never.
	 *
	 * @param grid Grid containing the Sudoku puzzle to be solved
	 * @param usage to populate with hinter usage: number of calls, hint count,
	 *  elapsed time, and number of maybes eliminated
	 * @param validatePuzzle true means run the TooFewClues and TooFewValues
	 *  validators before solving the puzzle
	 * @param isNoisy true prints local messages to standard output
	 * @param logHints true logs progress, false does it all quietly
	 * @param logTimes NEVER true in LogicalSolverTester (too verbose)
	 * @return was it solved; else see grid.invalidity
	 * @throws UnsolvableException (a RuntimeException) when puzzle is invalid,
	 *  ie cannot be solved. Details in the exception message (if any) and/or
	 *  the grid.invalidity field. sigh. <br>
	 *  A UE could just mean you've unwanted the rescue hinters: DynamicPlus
	 *  and NestedUnary, but typically a UE means you broke the hinter you are
	 *  working-on, so use the Validator to find examples in order to work-out
	 *  precisely where you have gone wrong. <br>
	 *  Note that a UE purports to say "puzzle is broken" but what it really
	 *  means is "failed to solve puzzle", which really means "you broke the
	 *  bloody solver again, ya putz!".
	 */
	public boolean solve(final Grid grid, final UsageMap usage
			, final boolean validatePuzzle, final boolean isNoisy
			, boolean logHints, boolean logTimes) {
		assert grid!=null && usage!=null;
		grid.invalidity = null; // assume sucess
		if ( isNoisy ) {
			System.out.println(">solve "+Settings.now()+"\n"+grid);
		}
		// the SingleHintsAccumulator collects just the first hint.
		final IAccumulator accu = new SingleHintsAccumulator();
		// time between hints incl activate and apply
		// nb: first hint-time is blown-out by enabling, activation, etc.
		long start = Run.time = System.nanoTime();
		// special case for being asked to solve a solved puzzle. Not often!
		if ( grid.numSet > 80 ) { // ie the grid is already solved
			accu.add(solvedHint);
			return true; // allways, even if accu is NOT a Single
		}
		// when called by the GUI we don't need to validate any longer because
		// my caller (the LogicalAnalyser) already has, in order to handle the
		// hint therefrom properly; so now we only validate here when called by
		// the LogicalSolverTester, which is all a bit dodgy, but only a bit;
		// all because it's hard to differentiate "problem" messages. Sigh.
		if ( validatePuzzle ) {
			validatePuzzle(grid, accu); // throws UnsolvableException
		}
		assert grid.isPrepared();
		// make all caches refresh
		grid.hintNumberReset();

		// solve the puzzle by finding and applying hints
		AHint hint;
		long now;
		logHints |= logTimes;
		while ( grid.numSet < GRID_SIZE ) {
			// getHints from each wanted hinter that's still enabled
			if ( logTimes ) { // very verbose
				if ( !timeHintersNoisily(wanted, grid, accu, usage) ) {
					return carp("Hint AWOL", grid, true);
				}
			} else { // the normal path
				if ( !timeHintersQuitely(wanted, grid, accu, usage) ) {
					return carp("Hint not found", grid, true);
				}
			}
			// apply the hint
			now = System.nanoTime();
			hint = accu.getHint();
			// apply may throw UnsolvableException from Cell.set
			apply(hint, now-start, grid, usage, logHints, isNoisy);
			start = now;
		}
		// puzzle solved, so tidy-up now
		after();
		if ( isNoisy ) {
			System.out.format("<solve %s\t%s\n", Settings.took(), grid);
		}
		return true;
	}

	/**
	 * timeHintersQuitely calls findHints on each hinter quitely, which is the
	 * normal path, as apposed to timeHintersNoisily which is, umm, noisy.
	 *
	 * @param hinters
	 * @param grid
	 * @param accu
	 * @param usage
	 * @return
	 * @throws HinterruptException if the Generator is stopped by the user.
	 */
	private boolean timeHintersQuitely(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final UsageMap usage) {
		long start;
		boolean any = false;
		for ( IHinter hinter : hinters ) {
			if ( Run.stopGenerate() )
				throw new HinterruptException();
			if ( hinter.isEnabled() ) {
				start = System.nanoTime();
				any = hinter.findHints(grid, accu);
				usage.addon(hinter, 1, 0, 0, System.nanoTime() - start);
				if ( any )
					return any;
			}
		}
		return any;
	}

	/**
	 * getHints from each hinter noisily, logging the execution time of every
	 * single bloody hinter, which is far too verbose for batch on top1465, so
	 * currently used only for a single puzzle re-run in LogicalSolverTester.
	 *
	 * @param hinters
	 * @param grid
	 * @param accu
	 * @param usage
	 * @return
	 * @throws HinterruptException if the Generator is stopped by the user.
	 */
	private boolean timeHintersNoisily(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final UsageMap usage) {
		long start, took;
		boolean any = false;
		for ( IHinter hinter : hinters ) {
			if ( Run.stopGenerate() )
				throw new HinterruptException();
			if ( hinter.isEnabled() ) {
				start = System.nanoTime();
				any = hinter.findHints(grid, accu);
				took = System.nanoTime() - start;
				usage.addon(hinter, 1, 0, 0, took);
				Log.teef("\t%,14d\t%s\n", took, hinter);
				if ( any )
					return any;
			}
		}
		return any;
	}

	/**
	 * Apply this hint to the grid.
	 *
	 * @param hint to apply
	 * @param took nanoseconds between now and previous hint
	 * @param grid to apply the hint to
	 * @param usage to be updated
	 * @param logHints if true then log hints
	 * @param logGrid if true then log the grid with the hints. Note that this
	 *  has no effect unless logHints is true, because the logging the grid
	 *  is pointless without the hints (it makes the log nonsensical)
	 * @throws UnsolvableException on the odd occasion
	 * @return the number of cell-values eliminated (and/or 10 per cell set)
	 */
	private int apply(final AHint hint, final long took, final Grid grid
			, final UsageMap usage, final boolean logHints
			, final boolean logGrid) {
		if ( hint == null ) {
			return 0;
		}
		if ( hint instanceof AWarningHint ) { // DoubleValue MissingMaybe
			throw new UnsolvableException("Fubarred: " + hint);
		}
		assert hint.getDifficulty() >= 1.0; // 0.0 has occurred. Bug!
		// + the grid (2 lines per hint)
		if (Log.MODE >= Log.VERBOSE_3_MODE) {
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
			if (Log.MODE >= Log.VERBOSE_2_MODE) {
				if ( logHints ) {
					numElims = hint.applyNoisily(Grid.AUTOSOLVE, grid);
				} else {
					numElims = hint.applyQuitely(Grid.AUTOSOLVE, grid);
				}
			} else {
				numElims = hint.applyQuitely(Grid.AUTOSOLVE, grid);
			}
		} catch (UnsolvableException ex) { // probably from Cell.set
			// stop the UnsolvableException obscuring the causal hint.
			Log.teeln("LogicalSolver.apply: "+hint.toFullString());
			Log.teeln("caused: "+ex);
			throw ex;
		}
		// + 1 line per Hint (detail)
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			if ( logHints ) {
				Print.hintFull(Log.out, hint, grid, numElims, took);
			}
		}
		// NB: usage was inserted by doHinters2, we just update it
		if ( usage != null ) {
			updateUsage(usage, hint, numElims);
		}
		if ( grid.isInvalidated() ) {
			throw new UnsolvableException("Invalidity: " + grid.invalidity);
		}
		// detect and deal-with eliminationless hints
		// nb: deadCats.add is "addOnly"; returns false if already exists.
		if ( numElims==0 && !deadCats.add(hint) ) {
			Log.teeln("WARN: deadCat disabled "+hint.hinter.tech.name()+": "
					  +hint.toFullString());
			// disable the offending hinter, averting infinite loop
			hint.hinter.setIsEnabled(false);
		}
		grid.hintNumberIncrement();
		return numElims;
	}

	// Updates the UsageMap with the hint details: adds numHints and numElims,
	// max's maxDifficulty, and addonates the subHints.
	// Called by: solve->apply->updateUsage
	// NB: usage was added by timeHinters, we just update the details.
	private void updateUsage(final UsageMap usageMap, final AHint hint
			, final int numElims) {
		Usage u = usageMap.get(hint.hinter);
		if ( u == null ) {
			// Locking upgrades siamese hints to HiddenPair/Triple, so usage
			// may not yet exist, so add it. The time is already allocated to
			// Locking even-though it's a hidden-set hint. Sigh.
			u = usageMap.addon(hint.hinter, 1, 1, numElims, 0L);
			u.maxDifficulty = hint.getDifficulty();
			u.ttlDifficulty = hint.getDifficultyTotal(); // GEM differs
			return;
		}
		++u.hints;
		u.elims += numElims;
		// nb: Math.max is slower.
		final double d = hint.getDifficulty();
		if ( d > u.maxDifficulty ) {
			u.maxDifficulty = d;
		}
		u.ttlDifficulty += hint.getDifficultyTotal(); // GEM differs
		// addonate to subHints Map: for hinters producing multiple hint types.
		u.addonateSubHints(hint, 1);
	}

	public void prepare(final Grid grid) {
		if ( grid==null || grid.isPrepared() ) {
			return; // safety first!
		}
		for ( IPrepare prepper : getPreppers(wanted) ) {
			try {
				prepper.prepare(grid, this);
			} catch (Exception ex) {
				Log.teeTrace("WARN: "+prepper+".prepare Exception", ex);
			}
		}
		grid.setPrepared(true);
	}

	/**
	 * Get the cached {@code LinkedList<IPrepare>} from hinters.
	 * <p>
	 * The preppers-cache is created ONCE, first-time only.
	 *
	 * @param hinters the wantedHinters list
	 * @return A cached {@code LinkedList<IPrepare>}
	 */
	private LinkedList<IPrepare> getPreppers(final IHinter[] hinters) {
		LinkedList<IPrepare> preppers = preppersCache;
		if ( preppers == null ) {
			preppers = new LinkedList<>();
			for ( IHinter hinter : hinters ) {
				if ( hinter instanceof IPrepare ) {
					preppers.add((IPrepare)hinter);
				}
			}
			preppersCache = preppers;
		}
		return preppers;
	}
	private LinkedList<IPrepare> preppersCache; // defaults to null

	/**
	 * Get the solution to this puzzle as soon as possible (ie guessing
	 * recursively, ie using brute force, ie using a BIG stick).
	 * <p>
	 * The solution is generated once and cached in {@link Grid#solution} and
	 * a SolutionHint is also cached in the {@link Grid#solutionHints} as the
	 * only element in a List (the requisite return type).
	 *
	 * @param grid the Grid to solve.
	 * @return a {@code List<AHint>} containing a SolutionHint.
	 */
	public List<AHint> getSolution(final Grid grid) {
		if ( grid.solutionHints == null ) {
			final IAccumulator accu = new SingleHintsAccumulator();
			// side-effect: sets grid.solution
			if ( bruteForce.findHints(grid, accu) ) {
				grid.solutionHints = AHint.list(accu.getHint());
			}
			// else WTF? No solution is available. Your options are:
			// 1. check expected hinters are in wantedHinters; or
			// 2. you've broken the hinter you were working on; or
			// 3. you've broken the solve method itself; or
			// 4. the Grid really is unsolvable; in which case it
			//    should not have passed upstream validation; or
			// 5. you're ____ed!
		}
		return grid.solutionHints;
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
	 * @return was the grid solved? returns false on Exception that's logged.
	 */
	public boolean solveWithSingles(final Grid grid, final IAccumulator accu) {
		// HintsApplicumulator.add apply's each hint when it's added.
		// @param isStringy=true for hint.toString's in the SummaryHint
		final HintsApplicumulator apcu = new HintsApplicumulator(true);
		// presume that no hint will be found
		boolean result = false;
		try {
			// we solve a copy of the given grid
			final Grid copy = new Grid(grid);
			apcu.grid = copy; // to tell Cell.apply which grid
			int ttlElims = 0;
			do {
				apcu.numElims = 0;
				if ( nakedSingle.findHints(copy, apcu)
				   | hiddenSingle.findHints(copy, apcu) ) {
					ttlElims += apcu.numElims;
				}
			} while ( apcu.numElims > 0 );
			if ( (result=ttlElims>0 && copy.numSet>80) && accu!=null ) {
				singlesSolution = copy; // hints pre-applied to copy
				accu.add(new SummaryHint(Log.me(), ttlElims, apcu));
			}
		} catch (Exception ex) { // especially UnsolvableException
// but only when we're focusing on solveWithSingles, which we normaly aren't.
//			Log.teeTrace(ex);
//			Log.teeln(grid);
		} finally {
			apcu.grid = null;
		}
		return result;
	}

	private boolean carp(final String message, final Grid grid, final boolean throwIt) {
		return carp(new UnsolvableException(message), grid, throwIt);
	}

	private boolean carp(final UnsolvableException ex, final Grid grid, final boolean throwIt) {
		grid.invalidity = ex.getMessage();
		if ( throwIt ) {
			if (Log.MODE >= Log.NORMAL_MODE) {
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

	// ----------------------------- shut-up time -----------------------------

	/**
	 * The report() method is called by the LogicalSolverTester to print a
	 * report from each IHinter which implements IReporter.
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
	 * For example:
	 * <pre>{@code public void report() {
	 *      // nb: use tech.name() for hinters which implement multiple techs.
	 *      Log.teef("\n%s: counts=%s\n", tech.name(), java.util.Arrays.toString(counts));
	 * }}</pre>
	 */
	public void report() {
//		Log.teeln("Idx MAX_SIZE="+java.util.Arrays.toString(Idx.MAX_SIZE));
		for ( IHinter h : wanted ) {
			if ( h instanceof IReporter ) {
				try {
					((IReporter)h).report();
				} catch (Exception ex) { // usually a bad format specifier
					StdErr.carp(""+h+".report failed and we continue anyway"
							, ex);
				}
			}
		}
	}

	/**
	 * The close() method releases any/all resources. In this case that means
	 * persisting any set-settings in any/all of the Filter/s we've been using.
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
		// report if we're not in the GUI (ie batch, test-cases, or whatever).
		// If we're in the GUI then report if we're -ea (used by techies).
		if ( Run.type!=Run.Type.GUI || Run.ASSERTS_ENABLED ) {
			report();
		}
		for ( IHinter h : wanted ) {
			if ( h instanceof Closeable ) {
				try {
					((Closeable)h).close();
				} catch (IOException eaten) {
					// do nothing
				}
			}
		}
		THE_SETTINGS.close();
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
	 * {@code diuf.sudoku.solver.hinters.als.RccFinderFactory#get}. What's
	 * going on under the hood is quite interesting. The first reference to
	 * LogicalSolver is the static constructHinter method, so a static CHEATS
	 * constructs Cheats then, creating each hinter, whereas delaying Cheats
	 * construction means this LogicalSolver pre-exists, so Cheats uses his
	 * hinters. This is a big difference for a simple code-change to avoid a
	 * big trap for young players that I fell into. Guess I'm a bit thick.
	 *
	 * @return THE instance of cheats, which is lazy-loaded.
	 */
	private Cheats cheats() {
		if ( cheats == null ) {
			cheats = new Cheats(this);
		}
		return cheats;
	}
	private Cheats cheats;

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
			result.addAll(hint.getGreens(0));
			return result;
		};
		/** The GREEN_ORANGE parser. */
		private static final Cheats.Parser GREEN_ORANGE
				= (final AHint hint, final Pots result, final Grid grid) -> {
			result.addAll(hint.getGreens(0));
			result.addAll(hint.getOranges(0));
			return result;
		};
		/** The GREEN_BLUE parser. */
		private static final Cheats.Parser GREEN_BLUE
				= (final AHint hint, final Pots result, final Grid grid) -> {
			result.addAll(hint.getGreens(0));
			result.addAll(hint.getBlues(grid, 0));
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
		 * silently: there's no logging, because I just don't care, but that
		 * makes Cheats harder to debug, so you need to mind how you go. Just
		 * ensure that Tech has a non-null className if/when you create the
		 * Cheat for it, and if it can't have a default implementation then
		 * think real-hard about the benefits of implementing a bloody cheat
		 * for the bastard. All things are possible, it's just that many are
		 * simply not worth the hassle.
		 * <p>
		 * The parser defines color/s of highlighted cell-values to display.
		 */
		private static enum Cheat {
			// left mouse button => column 'A'..'I'
			  A(Tech.NakedSingle, null) {
				// ODD_BALL: NakedSingle and HiddenSingle
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
				// ODD_BALL: XY_Wing and XYZ_Wing
				@Override
				String techName() { return "XY/Z_Wing"; }
			  }
			// right mouse button => column 'a'..'e'
			, a(Tech.W_Wing, GREEN_BLUE)
			, b(Tech.Swordfish)
			// skip coloring coz there hints can't be rendered as a cheat
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
		// I need to map each cheat-acronym to it's Tech and Parser; so then I
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
			   | ls.hiddenSingle.findHints(grid, hacu) ) {
				// nb: this is the only place we parse set-cell-values
				// into a Pots, so it's implemented in-line
				final Pots result = new Pots();
				hints.forEach((h)->result.put(h.cell, VSHFT[h.value]));
				return result;
			}
			return null;
		}

		// ODD_BALL: run both XY_Wing and XYZ_Wing over the 'grid', and parse
		// any resulting hints into a new Pots to return.
		private Pots runXyzWing(final Grid grid) {
			final LinkedList<AHint> hints = new LinkedList<>();
			final HintsAccumulator hacu = new HintsAccumulator(hints);
			boolean any = false;
			final IHinter xyWing = getHinterFor(Tech.XY_Wing);
			if ( xyWing != null ) {
				any |= xyWing.findHints(grid, hacu);
			}
			final IHinter xyzWing = getHinterFor(Tech.XYZ_Wing);
			if ( xyzWing != null ) {
				any |= xyzWing.findHints(grid, hacu);
			}
			// nb: use Cheat.I.parser for single point of truth
			return any ? parse(hints, Cheat.I.parser, grid) : null;
		}

		/**
		 * The "generic" run method gets the hinter that implements 'tech'
		 * and runs it over the 'grid', and parses the resulting hint/s
		 * into a Pots using the given 'parser', which is returned.
		 * <p>
		 * I'm called by {@link Cheat#run} to provide the 'tech' and 'parser'
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
			final IHinter hinter = getHinterFor(tech);
			if ( hinter != null ) {
				final LinkedList<AHint> hints = new LinkedList<>();
				if ( hinter.findHints(grid, new HintsAccumulator(hints)) ) {
					return parse(hints, parser, grid);
				}
			}
			return null; // no hint, or no hinter, or tilt
		}

		// get the hinter that implements 'tech', else null (Cheat disabled)
		private IHinter getHinterFor(final Tech tech) {
			// first-time only: setup the HINTERS map
			if ( HINTERS.isEmpty() ) {
				// build reverse-lookup-cache: Tech=>Hinter
				cache = new EnumMap<>(Tech.class);
				// add the basics to the cache, in case they're not wanted
				cache.putAll(ls.basics);
				// add each wanted hinter to the cache
				for ( IHinter hinter : ls.wanted ) {
					cache.put(hinter.getTech(), hinter);
				}
				// add each Cheat's hinter to HINTERS
				// if a Cheat hinter is not in the cache then we attempt to
				// construct our own, which fails if tech.className is null,
				// so this Cheat is permanently and silently disabled.
				EnumSet.allOf(Cheat.class).forEach((c) ->
						HINTERS.put(c.tech, cached(c.tech))
				);
				// we're done with the cache, which is used only to
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
		 * if it's null, so we do all this ONCE, disabling any AWOL hinters.
		 *
		 * @param tech you want the hinter for
		 * @return the hinter that implements 'tech', else null meaning that
		 *  constructHinter failed, so this cheat is disabled
		 */
		private IHinter cached(final Tech tech) {
			IHinter result;
			if ( (result=cache.get(tech)) == null ) {
				// if the hinter is not cached then attempt to construct it,
				// which will fail if tech.className is null, returning null,
				// so that this Cheat is permanently and silently disabled.
				result = constructHinter(tech);
			}
			return result;
		}

	}

}
