/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

// Sudoku Explainer (local) classes
// Tip: .* import local classes, but NEVER foreigners, because
// you can't change foreign class-names to resolve collisions.
import diuf.sudoku.*;
import static diuf.sudoku.Settings.THE_SETTINGS;
import diuf.sudoku.gen.*;
import diuf.sudoku.gui.Print;
import diuf.sudoku.io.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.align.*;
import diuf.sudoku.solver.hinters.align2.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.hdnset.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.lock2.*;
import diuf.sudoku.solver.hinters.nkdset.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.utils.*;
import static diuf.sudoku.utils.Frmt.NL;
import static diuf.sudoku.utils.Frmt.SPACE;
import static diuf.sudoku.utils.MyStrings.BIG_BUFFER_SIZE;
// JAPI
import java.io.Closeable;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A LogicalSolver solves Sudoku puzzles as quickly as possible (as I'm able)
 * using logic, the challenge of which is what this whole project is about.
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
 * <li>drop BUG (replaced by Coloring) and Medusa3D (replaced by GEM)
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
 * <li>My definition of "far too slow" is a minute per elimination
 * <li>All of these hinters are as fast as I know how to make them, which is
 *  pretty fast, except the A*E debacle. Search APB. I cannot preclude the
 *  possibility that it can be done faster, so have a go if you're really smart
 *  and have a few years to invest, but I warn you that gambling is addictive.
 * </ul>
 * <p>
 * <b>LogicalSolver is used to:</b><ul>
 *  <li>Check the validity of a Sudoku puzzle (ie a grid).
 *  <li>Get the simplest available hint (a Sudoku solving step) from a grid.
 *  <li>Get all available hints, including chainers and wanted nested-chainers
 *   from a grid. Note that the GUI allows the user to manage the wantedHinters
 *   list, which I (the LogicalSolver) just get from the registry via Settings.
 *  <li>Solve a Sudoku puzzle logically to calculate its difficulty rating,
 *   and get a summary list of the hint-types (usages) that are required.
 * </ul>
 * <p>
 * <b>Packaging:</b><ul>
 * <li>Validators are implemented in the {@code diuf.sudoku.solver.checks}
 *  package. Validity is checked ASAP because it's quicker to fail fast.
 * <li>The solving techniques are under the {@code diuf.sudoku.solver.hinters}
 *  package. There's too many to want to list here. The only counterintuitive
 *  packaging is that {@link Locking} hinter is "out of place" in the
 *  {@code diuf.sudoku.solver.hinters.fish} package because it shares the
 *  {@code PointFishHint} with the {@link BasicFisherman}. The rest of it is
 *  pretty straight forward, to me at least.
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
 * @see ./LogicalSolverTimings for run timings.
 */
public final class LogicalSolver implements Cheeter {

	/**
	 * true uses new align2 package, which is slower than the original align
	 * package for large (5+) aligned sets. I attempted to eradicate all that
	 * ugly "boiler-plate" code, but failed to achieve acceptable performance
	 * without it. sigh.
	 */
	private static final boolean USE_NEW_ALIGN2 = false; //@check false

	/**
	 * true uses the new BasicFisherman1 class, which is slower than the old
	 * BasicFisherman. I applied hobiwans "stack recursion" technique but it's
	 * slower than the Permutations class, which I think is pretty slow. sigh.
	 */
	private static final boolean USE_NEW_BASIC_FISHERMAN = false; // @check false

	/** The ANALYSE_LOCK synchronises {@link diuf.sudoku.gen.PuzzleCache}
	 * with {@link LogicalAnalyser}, so that one solve runs at a time. */
	public static final Object ANALYSE_LOCK = new Object();

	/** Does this puzzle have at least 17 clues?. */
	private final TooFewClues tooFewClues = new TooFewClues();

	/** Does this puzzle contain 8 values?. */
	private final TooFewValues tooFewValues = new TooFewValues();

	/** gridValidators check the grid is still valid before hinting. */
	private final IHinter[] gridValidators = new IHinter[] {
		  new NoDoubleValues()	 // one value per box, row, col
		, new NoMissingMaybes()	 // value unset and no maybes
		, new NoHomelessValues() // value unplaced and no places
	};

	// a bunch of "basic" hinters that we do some interesting stuff with.

	/** A cell with only one potential value remaining. */
	private final NakedSingle nakedSingle = new NakedSingle();

	/** The only place in a region which maybe a value. */
	private final HiddenSingle hiddenSingle = new HiddenSingle();

	/** They're Naked but there's only two of them. Beer will do. */
	private final NakedSet nakedPair = new NakedSet(Tech.NakedPair);

	/** They're Naked and there's three of them. Bring champagne! */
	private final NakedSet nakedTriple = new NakedSet(Tech.NakedTriple);

	/** If you're using BasicFisherman1 then just swap the bastard in. */
	private final BasicFisherman swampfish = new BasicFisherman(Tech.Swampfish);

	/** We locking.setSiamese in the GUI, and now in LogicalSolverTester. */
	private final Locking locking = new Locking();

	/** Hidden Pair/Triple used (cheekily) by Locking to upgrade siamese
	 * locking hints to the full HiddenSet hint, with extra eliminations. */
	private final HiddenSet hiddenPair = new HiddenSet(Tech.HiddenPair);

	/** Hidden Pair/Triple used (cheekily) by Locking to upgrade siamese
	 * locking hints to the full HiddenSet hint, with extra eliminations. */
	private final HiddenSet hiddenTriple = new HiddenSet(Tech.HiddenTriple);

	// RecursiveAnalyser uses above basic hinters, so is constructed after.
	/** Does this puzzle have 1 solution?. */
	private final RecursiveAnalyser recursiveAnalyser
			= new RecursiveAnalyser(this);

	/** The Sudoku cannot be solved (a generic message). */
	public final AHint UNSOLVABLE_HINT = new WarningHint(recursiveAnalyser
			, "Unsolvable", "NoSolution.html");

	/** puzzleValidators check the puzzle obeys Sudoku rules ONCE. */
	private final IHinter[] puzzleValidators = new IHinter[] {
		  tooFewClues		// minimum 17 clues
		, tooFewValues		// minimum 8 values
		, recursiveAnalyser // single solution (brute-force solve)
	};

	/** All of the validators. */
	private final IHinter[] validators = new IHinter[] {
		  puzzleValidators[0], puzzleValidators[1], puzzleValidators[2]
		, gridValidators[0], gridValidators[1], gridValidators[2]
	};

	/**
	 * If the user wants each Tech then its used to solve puzzles, else it's
	 * not used; but it's hinter may be created anyway then lost. sigh.
	 * <p>
	 * see: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku}
	 */
	private final EnumSet<Tech> wantedTechs;

	/**
	 * A private list of all the wanted hinters, to which configureHinters
	 * adds hinters; and then reads the list into the wantedHinters array,
	 * which is used everywhere-else.
	 */
	private final List<IHinter> wantedHintersList;

	/**
	 * An array of the wantedHintersList, coz it's faster to iterate arrays
	 * (native) than a Collection (using an Iterator).
	 */
	public final IHinter[] wantedHinters;

	/**
	 * Each unwanted Tech is added to this set, which is printed in the
	 * LogicalSolverTester log-file, as a record of what's NOT wanted.
	 */
	private final EnumSet<Tech> unwantedHinters = EnumSet.noneOf(Tech.class);

	/**
	 * LogicalSolver solve/apply detect DEAD_CAT, a hint which does not set a
	 * Cell and does not remove any maybes). If we see a DEAD_CAT twice presume
	 * we're at the start of an infinite loop in solve/generate/whatevs (finds
	 * same eliminationless hint endlessly) so throw an UnsolvableException
	 * instead. I the disable the hinter, but (for reasons I don't understand)
	 * the hinter checks isDisabled to be dis/re-enabled on-the-fly; apparently
	 * LogicalSolver.solve only handles disabled at start-up, even though that
	 * is definitely NOT how the code reads. BFIIK!
	 */
	private final Set<AHint> DEAD_CATS = new MyFunkyLinkedHashSet<>(16, 0.25F);

	/**
	 * My interruptMonitor (the parent Generator) to pass to hinters so that
	 * they can interrupt themselves when the user interrupts the Generator;
	 * so always null except when the LogicalSolver is created by a Generator.
	 */
	private volatile transient IInterruptMonitor interruptMonitor = null;
	public void setInterruptMonitor(IInterruptMonitor im) {
		this.interruptMonitor = im;
	}
	public void clearInterruptMonitor() {
		this.interruptMonitor = null;
	}
	/**
	 * return has the monitor (parent Generator) been interrupted by the user.
	 */
	private boolean isInterrupted() {
		final IInterruptMonitor im = this.interruptMonitor;
		return im!=null && im.isInterrupted();
	}

	/**
	 * singlesSolution is weird: it's set by LS.solveWithSingles and then read
	 * back by SudokuExplainer.getTheNextHint.
	 */
	public Grid singlesSolution;

	/**
	 * The Sudoku has been solved (a generic message).
	 */
	public final AHint SOLVED_HINT = new SolvedHint();

	/**
	 * My clone of THE_SETTINGS, set in the constructor, and read elsewhere.
	 * <p>
	 * mySettings are package visible to allow the LogicalSolverFactory to see
	 * if mySettings differ from THE_SETTINGS, and if so recreate me.
	 */
	final Settings mySettings;

	/**
	 * The ONLY Constructor used <u>ONLY</u> by {@link LogicalSolverFactory}
	 * to construct a new LogicalSolver, that's why I'm only package visible.
	 * <p>
	 * Note that this constructor is package visible, so LogicalSolver is only
	 * created via the {@link LogicalSolverFactory}. This is intentional, to
	 * protect against accidental miss-use: the solve method uses statefull
	 * static variables, so only ONE solve may run at a time, and the cheapest
	 * way to achieve this is using external synchronisation. sigh.
	 * <p>
	 * I populate the wantedHinters array which is used by solve (et al).
	 * Sudoku Explainers goal is to produce a realistic metric of difficulty of
	 * all possible Sudoku puzzles, as quickly as possible; which in practice
	 * means "as quickly as I'm able". The key to finding the simplest solution
	 * to a puzzle is to apply the simplest possible hint at each step, so the
	 * hinters here-in are listed in increasing order of complexity.
	 * <p>
	 * <u>Hinter Selection</u>:<br>
	 * <p>
	 * Naked Single and Hidden Single are hardcoded because they're required.
	 * The user unwants techs in the GUI, changing registry Settings, which the
	 * "want" method reads via the wantedTechs field, which is refreshed at the
	 * start of this method. An IHinter is instantiated which implements each
	 * Tech, added to the list, and then the list is converted into the
	 * wantedHinters array (used everywhere else) at the end.
	 * <p>
	 * <u>Execution Order</u>:<br>
	 * The solve (et al) methods iterate the wantedHinters array, invoking
	 * hinters in the order they're added here, which is approximately by
	 * Tech.difficulty ascending. Some hinters are called sooner than there
	 * Tech.difficulty dictates, because they're faster than "easier" ones.
	 * <p>
	 * Producing the simplest possible solution to each Sudoku demands that
	 * the software produces simplest possible hint at each step, but the
	 * concept of "simplest" is fairly arbitrary. Nanos-per-elimination is
	 * the only empirical evidence available, but that depends on how good
	 * my implementation of each technique is, which is fairly arbitrary.
	 * <pre>
	 * KRC 2021-06-07 simplified configureHinters. A new want method takes a
	 * Tech, which now has className and passTech attributes used to look-up
	 * the constructor and construct it only if it's wanted, so that unwanted
	 * hinters are never constructed.
	 *
	 * configureHinters adds to the private wantedHintersList, which is then
	 * read into the public wantedHinters array, that's used everywhere-else
	 * because array-iterators are faster than Iterators.
	 *
	 * This also saves creating a private array in the solve method, which was
	 * nuts when you consider that the array only need be created when Settings
	 * change, so the LogicalSolver is recreated. I removed the category lists.
	 * They were a useless complication. This is cleaner, in my humble opinion.
	 *
	 * KRC 2021-06-07 simplified again: configureHinters moved into the only
	 * LogicalSolver constructor, coz it's now the only place cH was called.
	 * </pre>
	 * <p>
	 * See the class documentation for more discussion.
	 */
	LogicalSolver() {
		// We add each new hinter to it's category-list in order of increasing
		// complexity, ergo complexity is rising. The order in which hinters
		// are created is the order in which they execute, which is about equal
		// to Tech.difficulty ASCENDING, but there are discrepencies; some
		// hinters run earlier than difficulty dicates, because they're faster
		// than "harder" techniques. The hardest puzzles require NestedUnary.
		// Normally we never get past DynamicPlus.
		//
		// For a scientist, it'd be better to sort Tech by difficulty ascending
		// and just create hinters in that order, so all Tech's would REQUIRE
		// one-any-only-one-implementor, but s__t gets messy when/if a hinter
		// implements multiple Tech's, wiped by an instance per Tech.

		// T/F is shorthand for true/false; to make the code fit on a line.
		final boolean T = true, F = false;
		// im is shorthand for interruptMonitor
		final IInterruptMonitor im = interruptMonitor;

		try {
			this.mySettings = (Settings)THE_SETTINGS.clone();
		} catch ( CloneNotSupportedException ex ) {
			// Wrap the clone exception in a generic runtime exception. sigh.
			throw new RuntimeException("THE_SETTINGS.clone() failed", ex);
		}

		// refetch the wantedTechniques field, in case they've changed; then my
		// want method reads this field, rather than re-fetching it every time.
		this.wantedTechs = mySettings.getWantedTechs();

		// the want method adds hinters to the wantedHintersList, which is
		// then read into the wantedHinters array, for faster iteration.
		this.wantedHintersList = new ArrayList<>(wantedTechs.size());

		// empty the unwanted-techs set. Populated by want. Report at end.
		this.unwantedHinters.clear();

		// Direct hinters set cell values directly
		// Easy
		if ( Run.type == Run.Type.GUI ) // useless in batch and test-cases
			want(new LonelySingle()); // lol
		// the user can't unwant singles
		this.wantedHintersList.add(nakedSingle); // cell has 1 maybe
		this.wantedHintersList.add(hiddenSingle); // value has 1 place in region
		// Medium
		if ( Run.type == Run.Type.GUI ) { // useless in batch and test-cases
			want(Tech.DirectNakedPair); // 2 cells with only 2 maybes produces a single
			want(Tech.DirectHiddenPair); // 2 places for 2 values in region produces a single
			want(Tech.DirectNakedTriple); // 3 cells with only 3 maybes produces a single
			want(Tech.DirectHiddenTriple); // 3 places for 2 values in region produces a single
		}

		// Indirect hinters remove maybes, setting cell values only indirectly
		// Locking XOR LockingGeneralised (not neither, nor both).
		if ( !want(locking) ) // Pointing and Claiming
			this.wantedHintersList.add(new LockingGeneralised());
		// Hard
		want(nakedPair); // 2 cells with only 2 maybes
		// nb: Locking (cheekily) upgrades hints using hiddenPair and Triple.
		want(hiddenPair); // same 2 places for 2 values in a region
		want(nakedTriple); // 3 cells with only 3 maybes
		// nb: Locking (cheekily) upgrades hints using hiddenPair and Triple.
		want(hiddenTriple); // same 3 places for 3 values in a region
		want(swampfish); // same 2 places in 2 rows/cols
		want(Tech.TwoStringKite); // box and 2 biplace regions
		// Fiendish
		want(Tech.XY_Wing);  // 3 bivalue cells
		want(Tech.XYZ_Wing); // trivalue + 2 bivalues
		want(Tech.W_Wing); // biplace row/col and 2 bivalue cells
		want(Tech.Skyscraper); // 2 biplace rows/cols get bent
		want(Tech.EmptyRectangle); // box and biplace region
		wantFish(Tech.Swordfish); // same 3 places in 3 rows/cols

		// Heavies are slow indirect hinters. The heavy-weigth division.
		// Nightmare
		want(Tech.BUG); // Bivalue Universal Grave (coloring on crutches)
		want(Tech.Coloring); // 2 coloring sets and the only 3+ coloring sets
		want(Tech.XColoring); // 2 coloring sets with cheese
		want(Tech.Medusa3D); // 2 coloring sets with cheese and pickles
		want(Tech.GEM); // 2 all beef patties, special cause, lettuce, chee...
		want(Tech.NakedQuad); // 4 cells with only 4 maybes
		want(Tech.HiddenQuad); // same 4 places for 4 values in a region
		wantFish(Tech.Jellyfish); // same 4 places in 4 rows/cols
		want(Tech.NakedPent);	// DEGENERATE 5 values in 5 cells
		want(Tech.HiddenPent);	// DEGENERATE 5 places for 5 values in a region
		// BigWings XOR individual S/T/U/V/WXYZ-Wing
		if ( !want(Tech.BigWings) ) { // All below BigWing (faster overall)
			want(Tech.WXYZ_Wing);	  // 3 cell ALS + bivalue
			want(Tech.VWXYZ_Wing);	  // 4 cell ALS + biv
			want(Tech.UVWXYZ_Wing);	  // 5 cell ALS + biv
			want(Tech.TUVWXYZ_Wing);  // 6 cell ALS + biv
			want(Tech.STUVWXYZ_Wing); // 7 cell ALS + biv
		}
		want(Tech.URT); // UniqueRectangle: a rectangle of 2 values
		want(Tech.FinnedSwampfish); // OK // now includes Sashimi's
		want(Tech.FinnedSwordfish); // OK
		want(Tech.FinnedJellyfish); // SLOW
		want(Tech.ALS_XZ);		 // OK	 // 2 Almost Locked Sets + bivalue
		want(Tech.ALS_Wing);	 // OK	 // 3 Almost Locked Sets + bivalue
		want(Tech.ALS_Chain);	 // OK	 // 4+ Almost Locked Sets + bivalue
		want(Tech.DeathBlossom); // SLOW // N ALSs + Nvalued cell; N in 2,3
		want(Tech.SueDeCoq);	 // 2SLW // Almost Almost Locked Sets
		// Diabolical
		want(Tech.FrankenSwampfish);	 // NONE (DEGENATE to Finned, WTF?)
		want(Tech.FrankenSwordfish);	 // OK
		want(Tech.FrankenJellyfish);	 // OK
		// Krakens & Mutants interleaved: Swamp, Sword, then Jelly
		want(Tech.KrakenSwampfish);	// SLOW (28 seconds)
		want(Tech.MutantSwampfish);	// NONE
		want(Tech.KrakenSwordfish);	// TOO SLOW (84 seconds)
		want(Tech.MutantSwordfish);	// SLOW (39 seconds)
		// Complex Jellyfish are really really really really really slow
		want(Tech.KrakenJellyfish);	// WAY TOO SLOW (8 minutes)
		want(Tech.MutantJellyfish);	// WAY TOO SLOW (7 minutes)

		// Aligned Ally: One up from Diagon Ally.
		// align2 (new) is faster than align (old) for A234E;
		wantAE(true, Tech.AlignedPair,   T, null);	// OK
		wantAE(true, Tech.AlignedTriple, T, null);	// OK but a bit slow
		wantAE(true, Tech.AlignedQuad,   F, im);	// SLOW
		// align2 takes 3*A5E, 4*A6E, 5*A7E, presume so on for 8, 9, 10.
		// The user chooses if A5+E is correct (slow) or hacked (faster).
		// Hacked finds about a third of hints in about a tenth of time.
		wantAE(USE_NEW_ALIGN2, Tech.AlignedPent, T, im); // OK
		wantAE(USE_NEW_ALIGN2, Tech.AlignedHex,  T, im); // SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedSept, T, im);	 // TOO SLOW
		// Really large Aligned sets are too slow to be allowed
		wantAE(USE_NEW_ALIGN2, Tech.AlignedOct,  T, im);	 // FAR TOO SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedNona, T, im); // WAY TOO SLOW
		wantAE(USE_NEW_ALIGN2, Tech.AlignedDec,  T, im);	 // CONSERVATIVE

		// Chainers are slow, but faster than some heavies, especially A*E.
		// single value forcing chains and bidirectional cycles.
		want(new UnaryChainer());
		// contradictory consequences of a single (bad) assumption.
		want(new MultipleChainer(Tech.NishioChain));
		// forcing chains from more than 2 values/positions.
		want(new MultipleChainer(Tech.MultipleChain));
		// forcing chains with consequences recorded in the grid.
		// does Unary Chains not Cycles, and Multiple Chains not Nishios.
		want(new MultipleChainer(Tech.DynamicChain));
		// DynamicPlus is Dynamic Chaining + Four Quick Foxes (Point & Claim,
		// Naked Pairs, Hidden Pairs, and Swampfish). It only misses on hardest
		// puzzles, so hardcoded as safery-net, so user can't unwant it.
		// A safety-net for all the but the hardest puzzles
		want(new MultipleChainer(Tech.DynamicPlus, im));

		// IDKFA
		// Nested means imbedded chainers: assumptions on assumptions.
		// The only way to ever see a nested hint is with Shift-F5 in the GUI.
		// advanced					 // approx time per call on an i7 @ 2.9GHz
		// NestedUnary covers the hardest Sudoku according to conceptis.com so
		// it's a safety-net: it always hints!
		want(new MultipleChainer(Tech.NestedUnary, im));	//  4 secs
		want(new MultipleChainer(Tech.NestedMultiple, im));	// 10 secs
		// experimental
		want(new MultipleChainer(Tech.NestedDynamic, im));	// 15 secs
		// NestedPlus is Dynamic + Dynamic + Four Quick Foxes => confusing!
		// @bug 2020 AUG: NestedPlus PRODUCED INVALID HINTS so now uses the
		// HintValidator to log and ignore them. NOT adequate needs fixing!
		// I'm just ignoring this bug for now coz IT'S NEVER USED in anger.
		want(new MultipleChainer(Tech.NestedPlus, im));		// 70 secs

		wantedHinters = this.wantedHintersList.toArray(
				new IHinter[wantedHintersList.size()]);
		// weird: each hinter knows it's index in the wantedHinters array,
		// so that LogicalSolverTest can log hinters in execution order.
		for ( int i=0,n=wantedHinters.length; i<n; ++i )
			wantedHinters[i].setIndex(i);
	}

	/**
	 * A better name would be addHinterIfWanted but that's just too verbose.
	 * If the given tech is wanted then I instantiate it's className, passing
	 * the tech if it's passTech is true.
	 * Some techs have a null className, in which case you will need to supply
	 * the className (IHinter that is the default implementation of this Tech)
	 * and passTech in the Tech definition, or just instantiate your hinter
	 * manually, and pass it to the <u>other</u> want method.
	 *
	 * @param tech the Tech to add to wantedHinters, if the user wants it.
	 * @throws IllegalArgumentException, RuntimeException
	 */
	private boolean want(Tech tech) {
		final boolean result;
		if ( result=wantedTechs.contains(tech) ) {
			final IHinter hinter = createHinterFor(tech);
			if ( hinter != null )
				this.wantedHintersList.add(hinter);
		} else
			this.unwantedHinters.add(tech);
		return result;
	}

	/**
	 * A better name would be addThisHinterIfItsWanted but it's too long.
	 * If hinter.getTech() is wanted then add it to Collection 'c';
	 * otherwise add the tech to the unwanted list, for printing
	 * once we're done with all these bloody want method-calls.
	 *
	 * @param hinter to want, or not.
	 */
	private boolean want(IHinter hinter) {
		final boolean result;
		final Tech tech = hinter.getTech();
		if ( result=this.wantedTechs.contains(tech) )
			this.wantedHintersList.add(hinter);
		else
			this.unwantedHinters.add(tech);
		return result;
	}

	/**
	 * Adds a new instance of BasicFisherman/1(tech) to the wantedHintersList.
	 * If USE_NEW_BASIC_FISHERMAN_1 use the new BasicFisherman1, else the old
	 * BasicFisherman, which is faster. sigh.
	 *
	 * @param tech Tech.Swampfish, Tech.Swordfish, or Tech.Jellyfish
	 * @return is the given Tech wanted?
	 */
	private boolean wantFish(Tech tech) {
		final boolean result;
		if ( (result=wantedTechs.contains(tech)) ) {
			if ( USE_NEW_BASIC_FISHERMAN )
				this.wantedHintersList.add(new BasicFisherman1(tech));
			else
				this.wantedHintersList.add(new BasicFisherman(tech));
		} else
			this.unwantedHinters.add(tech);
		return result;
	}

	/**
	 * The wantAE method creates the appropriate Aligned Exclusion instance
	 * and adds it to the wantedHintersList, if it's wanted by the user.
	 * @param useNewAlign2 use the new "align2" package, or the old "align"
	 *  package; A234E are always true, and for A5678910E pass me the value
	 *  of the USE_NEW_ALIGN2 constant.
	 *
	 * @param tech one of the Aligned* Tech's
	 * @param defaultIsHacked should this Aligned Exclusion be hacked (finds
	 *  about a third of the hints in about a tenth of the time) by default.
	 *  This value is only used if there's no registry setting, yet.
	 * @param im the IInterruptMonitor which enables generate to interrupt.
	 */
	private boolean wantAE(boolean useNewAlign2, Tech tech
			, boolean defaultIsHacked, IInterruptMonitor im) {
		if ( useNewAlign2 ) {
			assert tech.isAligned;
			// AE constructor reads the "isa${degree}ehacked" Setting.
			// There is no more _1C/_2H version of the same class, and no
			// more class per size, reducing "boiler-plate" significantly.
			return want(new AlignedExclusion(tech, defaultIsHacked));
		} else {
			switch ( tech ) {
				// there is no hacked version of A234E
				case AlignedPair: return want(new Aligned2Exclusion());
				case AlignedTriple: return want(new Aligned3Exclusion());
				case AlignedQuad: return want(new Aligned4Exclusion(im));
				// A5678910E are all _2H hacked or _1C correct
				case AlignedPent:
				case AlignedHex:
				case AlignedSept:
				case AlignedOct:
				case AlignedNona:
				case AlignedDec:
//<OUTDENT>
//nb: all Aligned*Exclusion are NOT wanted by default
if ( mySettings.getBoolean(tech.name(), tech.defaultWanted) ) {
	try {
		// read "isa${degree}ehacked" Setting to choose which
		// Aligned*Exclusion (_1C or _2H) class to construct.
		final boolean isHacked =
				mySettings.get("isa"+tech.degree+"ehacked", defaultIsHacked);
		final String className = "diuf.sudoku.solver.hinters.align."
				+ "Aligned"+tech.degree+"Exclusion"+(isHacked?"_2H":"_1C");
		final Class<?> clazz = Class.forName(className);
		final java.lang.reflect.Constructor<?> constructor =
			clazz.getConstructor(IInterruptMonitor.class);
		final IHinter hinter =
			(IHinter)constructor.newInstance(im);
		return want(hinter);
	} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
		StdErr.exit("wantAE fubarred", ex);
		return false;
	}
} else {
	unwantedHinters.add(tech);
	return false;
}
//</OUTDENT>
				default:
					throw new IllegalArgumentException("Unknown Aligned* tech: "+tech);
			}
		}
	}

	/** LogicalSolverTester needs the option to set-up for siamese locking. */
	public void setSiamese() { locking.setSiamese(hiddenPair, hiddenTriple); }
	/** un-set-up after siamese locking. */
	public void clearSiamese() { locking.clearSiamese(); }

	/** @return the HiddenSingle hinter to friends. */
	public AHinter getHiddenSingle() { return hiddenSingle; }
	/** @return the NakedSingle hinter to friends. */
	public AHinter getNakedSingle() { return nakedSingle; }
	/** @return the Locking hinter to friends. */
	public AHinter getLocking() { return locking; }
	/** @return the NakedPair hinter to friends. */
	public AHinter getNakedPair() { return nakedPair; }
	/** @return the HiddenPair hinter to friends. */
	public AHinter getHiddenPair() { return hiddenPair; }
	/** @return the Swampfish hinter to friends. */
	public AHinter getSwampfish() { return swampfish; }

	/**
	 * Tester Logging Only: This method is used by LogicalSolverTester to
	 * display the names of the "wanted" hinters to the developer.
	 *
	 * @param label prefix before my output, "" for none.
	 * @return CSV of the wanted hinter.getTech().names.
	 */
	public final String getWantedHinterNames(String label) {
		return Frmt.csv(label, wantedHinters, HINTER_TECH_NAME);
	}
	private static final IFormatter<Object> HINTER_TECH_NAME = (Object o) ->
			o==null ? "" : ((IHinter)o).getTech().name();

	public String getUnwantedHinterNames(String label) {
		// remove the []'s from AbstractCollection toString
		String fms = unwantedHinters.toString();
		fms = fms.substring(1, fms.length()-1);
		return label+fms;
	}

	/**
	 * Debug only: dumps wanted hinters to the given PrintStream.
	 *
	 * @param out PrintStream to print to.
	 */
	public void printWantedEnabledHinters(PrintStream out) {
		out.print("wantedEnabledHinters: ");
		Frmt.csv(out, wantedHinters, ENABLED, HINTER_TECH_NAME);
		out.println();
	}
	private static final IFilter<Object> ENABLED = (Object o) ->
			((IHinter)o).isEnabled();

	// clean-up before the GC runs
	public void cleanUp() {
		for ( IHinter h : wantedHinters )
			if ( h instanceof ICleanUp )
				((ICleanUp)h).cleanUp();
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
	public boolean getFirstHint(Grid grid, SingleHintsAccumulator accu) {
		if ( grid.isFull() ) {
			accu.add(SOLVED_HINT);
			return true;
		}
		grid.rebuildAllRegionsS__t();
		if ( getFirstHint(validators, grid, accu, false)
		  || getFirstHint(wantedHinters, grid, accu, false) ) {

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
	 * If a validator hints we're done regardless of what the user wants.
	 * If !wantMore (F5) get all hints from the first hinty hinter only.
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
		final boolean isFilteringHints = mySettings.getBoolean(Settings.isFilteringHints, false);
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
		if ( grid.isFull() ) {
			hints.add(SOLVED_HINT);
			return hints; // empty if I was interrupted, or didn't find a hint
		}
		final long t0 = Run.time = System.nanoTime();
		// HintsAccumulator.add returns false so hinter keeps searching,
		// adding each hint to the hints List.
		final IAccumulator accu = new HintsAccumulator(hints);
		// rebuild all the grid's s__t.
		grid.rebuildAllRegionsS__t();
		// get hints from hinters until one of them returns true (I found a
		// hint) OR if wantMore then run all hinters. nb: DyamicPlus nearly
		// always finds a hint. NestedUnary ALWAYS finds a hint.
		boolean any = getFirstHint(validators, grid, accu, false);
		if ( !any ) {
			// tell locking to merge/upgrade any siamese hints
			// when the GUI wantsLess (F5/Enter) and isFilteringHints
			if ( Run.type==Run.Type.GUI && !wantMore && isFilteringHints )
				setSiamese();
			// run prepare AFTER validators and BEFORE wantedHinters
			if ( !grid.isPrepared() ) // when we get first hint from this grid
				prepare(grid); // prepare wanted IPreparer's to process grid
			any = getAllHints(wantedHinters, grid, accu, !wantMore, logHints);
			// unset doSiamese or Generate goes mental. I'm in two minds RE
			// putting this in a finally block: safer but slower. This works.
			clearSiamese();
		}
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			long took = System.nanoTime()-t0;
			if ( any && (logHints || printHints) ) { // where there any hints (there should be)
				final AHint hint = accu.peek(); // the first hint
				final int numElims = hint.getNumElims();
				if(logHints)Print.gridFull(Log.log, hint, grid, numElims, took);
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
			if ( isInterrupted()
			  || findHints(hinter, grid, accu, logHints) )
				break;
		}
		return accu.hasAny();
	}

	// getHints from the given hinters: if less then return when hint found as
	// per getFirst, else getHints from all the hinters, adding them to accu,
	// and then return "were any found?" (which'll allways be true, AFAIK).
	// When interrupt()'ed we break-out and return normally.
	// This method is only called by getAllHints directly, to handle wantLess.
	// Hinters are only called then they are enabled. A Hinter is disabled if
	// it's a very naughty boy and produces the same DEAD_CAT twice.
	private boolean getAllHints(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final boolean less
			, final boolean logHints) {
		for ( IHinter hinter : hinters )
			if ( isInterrupted()
			  || (findHints(hinter, grid, accu, logHints) && less) )
				break;
		return accu.hasAny() && less;
	}

	// findHints calls the hinter.findHints; log it if isNoisy.
	private boolean findHints(final IHinter hinter, final Grid grid
			, final IAccumulator accu, final boolean logHints) {
		// skip this hinter if it has been disabled
		if ( !hinter.isEnabled() )
			return false;
		// if we're logging (breaking the separate static/dynamic rule!)
		if (Log.MODE >= Log.VERBOSE_2_MODE && logHints ) {
			Log.format("%-40s ", hinter.getTech().name());
			final long t0 = System.nanoTime();
			final boolean result = hinter.findHints(grid, accu);
			final long took = System.nanoTime() - t0;
			Log.format("%,15d\t%s\n", took, hinter);
			return result;
		}
		// else just do it quietly
		return hinter.findHints(grid, accu);
	}

	/**
	 * Krakens go bugger-up in generate.
	 * @param enabled
	 * @param degree
	 */
	public void enableKrakens(boolean enabled, int degree) {
		for ( IHinter h : wantedHinters )
			if ( h instanceof KrakenFisherman && h.getDegree()>=degree )
				h.setIsEnabled(enabled);
	}

	/**
	 * Get the hinter which implements targetTech from wantedHinters list.
	 * @param targetTech
	 * @return the hinter; else null if the hinter which implements targetTech
	 *  is not wanted, or (improbable) no hinter implements targetTech.
	 */
	public IHinter findWantedHinter(Tech targetTech) {
		for ( IHinter hinter : wantedHinters )
			if ( hinter.getTech() == targetTech )
				return hinter;
		return null;
	}

	/**
	 * Fundamentally: Is this grids max-difficulty less than the given maxD?
	 * We find and apply hints to the grid, stopping at the first hint whose
	 * getDifficulty() exceeds <tt>maxD</tt>. There may not be one. I return
	 * the puzzles difficulty, that is the max hint-difficulty; even if it's
	 * above the given maxDif, which just tells me when to stop looking.
	 * <p>
	 * You can enforce minDif (or not) yourself. There is no consideration of
	 * minimum difficulty here, only maximum. The caller assesses the minimum
	 * (if any) for itself. I only monitor the maximum so that I can give-up
	 * early if/when it's exceeded.
	 * <p>
	 * NB: I read the IInterruptMonitor passed into my constructor (if any).
	 *
	 * @param grid to examine
	 * @param maxDif the maximum desired difficulty (inclusive)
	 * @return The puzzles difficulty if it's below the given maxDif,
	 *  else an arbitrary out-of-bounds value.
	 */
	public double analyseDifficulty(Grid grid, double maxDif) {
		double pd=0.0D; // puzzleDifficulty, my result
		final IAccumulator accu = new SingleHintsAccumulator();
		// re-enable all hinters just in case we hit a DEAD_CAT last time
		if ( anyDisabled )
			for ( IHinter hinter : wantedHinters )
				hinter.setIsEnabled(true);
		DEAD_CATS.clear(); // deal with deceased felines
		// Generator skips Kraken Swordfish & Jellyfish which bugger-up on
		// null table entries, and I do not understand why they are null!
		enableKrakens(false, 3); // No Phil McKraken!
		grid.hintNumberReset(); // Start from the start
		grid.rebuildAllRegionsS__t(); // esp indexesOf and idxsOf
		for ( int pre=grid.countMaybes(),now; pre>0; pre=now ) { // ie !isFull
			if ( getFirstHint(wantedHinters, grid, accu, false) ) {
				final AHint hint = accu.getHint();
				assert hint != null;
				// NB: AggregatedChainingHint.getDifficulty returns the maximum
				// difficulty of all the hints in the aggregate.
				final double d = hint.getDifficulty();
				assert d >= Difficulty.Easy.min; // there is no theoretical max
				if ( d>pd && (pd=d)>maxDif )
					return pd; // maximum desired difficulty exceeded
				try {
					hint.applyQuitely(Grid.NO_AUTOSOLVE, grid);
				} catch (UnsolvableException ex) {
//					// flick-pass UE to log the causal hint
//					Log.println("analyseDifficulty: "+ex+" applying "+hint);
					throw ex;
				}
				// deal with DEAD_CAT: a hint that doesn't remove any maybes.
				if ( (now=grid.countMaybes())==pre && !DEAD_CATS.add(hint) ) {
					anyDisabled = true;
					hint.hinter.setIsEnabled(false);
				}
			} else {
				Log.teeln("analyseDifficulty: No hint found in grid:");
				Log.teeln(grid);
				return Difficulty.IDKFA.max; // a largish number
			}
			if ( isInterrupted() )
				return 9999.0D; // an impossibly high puzzle-difficulty
		}
		enableKrakens(true, 3);
		return pd;
	}
	private boolean anyDisabled;

	/**
	 * Get the Solution of this puzzle. Used by generate and by the <i>Tools ~
	 * Analyse</i> menu-item in the SudokuFrame.
	 *
	 * @param grid a copy of the Grid to solve (not the GUI's grid)
	 * @param logHints if true then hints are printed in the Log. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 * @param logTimes if true then EVERY single hinter execution is logged. <br>
	 *  In the GUI that's HOME/SudokuExplainer.log in my normal setup.
	 * @return an AHint which is either an AnalysisHint or a WarningHint.
	 */
	public AHint analysePuzzle(final Grid grid, final boolean logHints, final boolean logTimes) {
		if (Log.MODE >= Log.NORMAL_MODE) {
			Log.format("\nanalyse: %s\n%s\n", grid.source, grid);
		}
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		final long t0 = System.nanoTime();
		if ( grid.isFull() )
			return SOLVED_HINT;
		// execute just the puzzle validators.
		if ( getFirstHint(puzzleValidators, grid, accu, false) )
			return accu.getHint();
		final LogicalAnalyser analyser = new LogicalAnalyser(this, logHints, logTimes);
		// LogicalAnalyser.findHints allways returns true, only the returned
		// hint-type varies: solved=AnalysisHint or invalid="raw" WarningHint,
		// which are both WarningHints; so telling them apart is a bit tricky.
		if ( analyser.findHints(grid, accu) ) {
			final long t1 = System.nanoTime();
			final AHint hint = accu.getHint(); // a SolutionHint
			if (Log.MODE >= Log.NORMAL_MODE) {
				Log.format("%,15d\t%2d\t%4d\t%3d\t%-30s\t%s\n"
						, t1-t0, grid.countFilledCells(), grid.countMaybes()
						, hint.getNumElims(), hint.getHinter(), hint);
				if ( hint instanceof AnalysisHint ) {
					final AnalysisHint ah = (AnalysisHint)hint;
					// this is a cluster____ because appendUsageMap calculates
					// ttlDifficulty but it's logged BEFORE appendUsageMap runs
					// so that log is consistent with the GUI. I'm a putz!
					final StringBuilder sb = new StringBuilder(BIG_BUFFER_SIZE);
					final String maxD = "Difficulty ##.##"; // largest possible
					sb.append(NL); // leading blank line
					sb.append(maxD).append(NL);
					ah.appendUsageMap(sb);
					sb.append("Total Difficulty ").append(ah.ttlDifficulty).append(NL); // trailing blank line (unusual, for me)
					int i = sb.indexOf(maxD);
					String newMaxD = "Difficulty "+Frmt.frmtDbl(ah.maxDifficulty);
					sb.replace(i, i+maxD.length(), newMaxD);
					Log.println(sb);
				}
			}
			return hint;
		}
		return null; // Never happens. Never say never.
	}

	/**
	 * Solve the grid using brute-force, set my solution field, and return the
	 * solution.
	 * <p>
	 * Note that repeated calls to solve the same grid will solve the same damn
	 * grid, so take care to NOT call me unless you need to. Check my solution
	 * field first.
	 * @param grid Grid to be solved
	 * @return solution Grid, which is NOT the given grid, it's a copy that has
	 * been solved.
	 */
	public Grid solveASAP(Grid grid) {
		return recursiveAnalyser.solveASAP(grid);
	}

	/**
	 * Count the number of ways this grid can be solved:
	 *
	 * @param grid Grid to be solved
	 * @param isNoisy
	 * @return 1 means valid, 0 means invalid (no solution), 2 means invalid
	 *  (multiple solutions).
	 */
	public int countSolutions(Grid grid, boolean isNoisy) {
		return recursiveAnalyser.countSolutions(grid, isNoisy);
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
	public AHint getSolutionHint(Grid grid) {
		IAccumulator accu = new SingleHintsAccumulator();
		// check the puzzle is basically valid
		if ( grid.isFull() ) // already solved
			return SOLVED_HINT;
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) ) // NumberOfValues >= 8
			return accu.getHint();
		// tell recursiveAnalyser I want the SolutionHint.
		recursiveAnalyser.wantSolutionHint = true;
		// set recursiveAnalyser.solutionHint
		if ( recursiveAnalyser.solveASAP(grid) == null )
			return UNSOLVABLE_HINT;
		// return recursiveAnalyser.solutionHint
		AHint result = recursiveAnalyser.solutionHint;
		assert result != null;
		recursiveAnalyser.solutionHint = null;
		return result;
	}

	/**
	 * Validate the puzzle only (not the grid).
	 * @param grid
	 * @param accu
	 * @return
	 */
	private boolean validatePuzzle(Grid grid, IAccumulator accu) {
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) // NumberOfValues >= 8
		  || recursiveAnalyser.findHints(grid, accu) )
			return carp("Invalid Sudoku Puzzle: "+accu.getHint(), grid, true);
		return true; // the puzzle is (probably) valid.
	}

	/**
	 * Checks the validity of the given Grid completely, ie using all puzzle
	 * and grid validators. If quite then there's no logging, else noisy.
	 * <p>
	 * Call validatePuzzleAndGrid ONCE before solving each puzzle, other
	 * validations should do the puzzle only.
	 *
	 * @param grid Grid to validate.
	 * @param logHints true to log, false to shut the ____ up.
	 * @return the first warning hint, else null. */
	public AHint validatePuzzleAndGrid(Grid grid, boolean logHints) {
		IAccumulator accu = new SingleHintsAccumulator();
		AHint problem = null;
		if ( getFirstHint(validators, grid, accu, logHints) )
			problem = accu.getHint();
		return problem;
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
	public AHint validateGrid(Grid grid, boolean logHints) {
		IAccumulator accu = new SingleHintsAccumulator();
		AHint problem = null;
		if ( getFirstHint(gridValidators, grid, accu, logHints) )
			problem = accu.getHint();
		return problem;
	}

	/**
	 * Solve 'grid' logically to populate 'usageMap' and return success.
	 * In English: Solves the given Sudoku puzzle (the 'grid') using logic to
	 * populate the given 'usageMap' with a summary of which Hinters were used
	 * how often, and how long each took to run; to return "was the puzzle
	 * solved", which should be true for any/every valid puzzle, but never say
	 * never. An invalid puzzle should throw an UnsolvableException (a subtype
	 * of RuntimeException) so solve never returns false. Never say never.
	 *
	 * @param grid Grid containing the Sudoku puzzle to be solved
	 * @param usage UsageMap (empty) that I populate with hinter usage: number
	 *  of calls, hint count, elapsed time, and number of maybes eliminated
	 * @param validate true means run the TooFewClues and TooFewValues
	 *  validators before solving the puzzle.
	 * @param isNoisy true prints local messages to stdout
	 * @param logHints true logs progress, false does it all quietly.
	 * @param logTimes NEVER true in LogicalSolverTester (too verbose).
	 * @return was it solved; else see grid.invalidity
	 * @throws UnsolvableException which is a RuntimeException means that this
	 *  puzzle is invalid and/or cannot be solved. See the exceptions message
	 *  (if any) for details, and/or grid.invalidity field.
	 */
	public boolean solve(final Grid grid, final UsageMap usage
			, final boolean validate, final boolean isNoisy
			, final boolean logHints, final boolean logTimes) {
		assert grid!=null && usage!=null;
		final boolean logHinterTimes = logTimes && Run.type==Run.Type.GUI;

//if(grid.source.lineNumber==6)
//	Debug.breakpoint();
		grid.invalidity = null; // assume sucess
		if ( isNoisy )
			System.out.println(">solve "+Settings.now()+"\n"+grid);
		final IAccumulator accu = new SingleHintsAccumulator();
		// time between hints incl activate and apply
		// nb: first hint-time is blown-out by enabling, activation, etc.
		long start = Run.time = System.nanoTime();
		// special case for being asked to solve a solved puzzle. Not often!
		if ( grid.isFull() ) {
			accu.add(SOLVED_HINT);
			return true; // allways, even if accu is NOT a Single
		}
		// when called by the GUI we don't need to validate any longer because
		// my caller (the LogicalAnalyser) already has, in order to handle the
		// hint therefrom properly; so now we only validate here when called by
		// the LogicalSolverTester, which is all a bit dodgy, but only a bit;
		// all because it's hard to differentiate "problem" messages. Sigh.
		if ( validate )
			validatePuzzle(grid, accu); // throws UnsolvableException
		assert grid.isPrepared();
		if ( !grid.isPrepared() )
			prepare(grid); // prepare hinters to process this grid
		// and they're off and hinting ...
		grid.hintNumberReset();

		AHint hint;
		long now;
		while ( !grid.isFull() ) {
			// getHints from each enabled hinter
			if ( logHinterTimes ) {
				if ( !timeHintersNoisy(wantedHinters, grid, accu, usage) )
					return carp("Hint not found", grid, true);
			} else {
				if ( !timeHinters(wantedHinters, grid, accu, usage) )
					return carp("Hint not found", grid, true);
			}
			// apply the hint
			now = System.nanoTime();
			hint = accu.getHint();
			// apply may throw UnsolvableException from Cell.set
			applyHint(hint, now-start, grid, usage, isNoisy, logHints);
			start = now;
		} // wend
		if ( isNoisy )
			System.out.format("<solve %s\t%s\n", Settings.took(), grid);
		return true;
	}

	// solve times the getHints of each hinter.
	private boolean timeHinters(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final UsageMap usage) {
		long start;  boolean any;
		for ( IHinter hinter : hinters )
			if ( hinter.isActive() ) {
				start = System.nanoTime();
				any = hinter.findHints(grid, accu);
				usage.addon(hinter, 1, 0, 0, System.nanoTime() - start);
				if ( any )
					return true;
			}
		return false;
	}
	// as above but log execution time of every single hinter (very verbose)
	private boolean timeHintersNoisy(final IHinter[] hinters, final Grid grid
			, final IAccumulator accu, final UsageMap usage) {
		long start, took;  boolean any;
		for ( IHinter hinter : hinters )
			if ( hinter.isActive() ) {
				start = System.nanoTime();
				any = hinter.findHints(grid, accu);
				took = System.nanoTime() - start;
				usage.addon(hinter, 1, 0, 0, took);
				Log.teef("\t%,14d\t%s\n", took, hinter);
				if ( any )
					return true;
			}
		return false;
	}

	// Apply this hint to the grid.
	// @param hint to apply
	// @param took nanoseconds between now and previous hint
	// @param grid to apply the hint to
	// @param usage for the updateUsage method (below)
	// @param logHints if true log hints
	// @throws UnsolvableException on the odd occassion
	// @return the number of cell-values eliminated (and/or 10 per cell set)
	private int applyHint(final AHint hint, final long took, final Grid grid
			, final UsageMap usage, final boolean logGrid, final boolean logHints) {
		if ( hint == null )
			return 0;
		if ( hint instanceof AWarningHint ) //NoDoubleValues or NoMissingMaybes
			throw new UnsolvableException("Warning: " + hint);
		assert hint.getDifficulty() >= 1.0; // 0.0 has occurred. Bug!
		// + the grid (2 lines per hint)
		if (Log.MODE >= Log.VERBOSE_3_MODE) {
			if ( logGrid && logHints ) { // the grid makes no sense sans hints
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
				if ( logHints )
					numElims = hint.applyNoisily(Grid.AUTOSOLVE, grid);
				else
					numElims = hint.applyQuitely(Grid.AUTOSOLVE, grid);
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
			if ( logHints )
				Print.hintFull(Log.log, hint, grid, numElims, took);
		}
		// NB: usage was inserted by doHinters2, we just update it
		if ( usage != null )
			updateUsage(usage, hint, numElims);
		if ( grid.isInvalidated() )
			throw new UnsolvableException("Invalidity: " + grid.invalidity);
		// if the hint yields 0-cells-set and 0-maybes-eliminated, then we have
		// a problem, which we ignore until it happens twice.
		// nb: DEAD_CATS.add is "addOnly"; returns false if already exists.
		if ( numElims==0 && !DEAD_CATS.add(hint) ) {
			// tell-em it's rooted
			Log.teeln("WARN: DEAD_CAT disabled "+hint.hinter.tech.name()+": "+hint.toFullString());
			// disable the offender
			hint.hinter.setIsEnabled(false);
		}
		grid.hintNumberIncrement();
		// rebuild before validate for RecursiveAnalyser.
		grid.rebuildAllRegionsS__t();
// done above with grid.isInvalidated() so this is just silly.
//		// detect invalid grid, meaning a hinter is probably borken!
//		final AHint problem = validateGrid(grid, false);
//		if ( problem != null )
//			throw new UnsolvableException("Houston: "+problem);
		return numElims;
	}

	// Updates the UsageMap with the hint details: adds numHints and numElims,
	// max's maxDifficulty, and addonates the subHints.
	// Called by: solve->apply->updateUsage
	// NB: usage was added by timeHinters, we just update the details.
	private void updateUsage(UsageMap usageMap, AHint hint, int numElims) {
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
		if ( d > u.maxDifficulty )
			u.maxDifficulty = d;
		u.ttlDifficulty += hint.getDifficultyTotal(); // GEM differs
		// addonate to subHints Map: for hinters producing multiple hint types.
		u.addonateSubHints(hint, 1);
	}

	public void prepare(Grid grid) {
		if ( grid == null )
			return; // safety first!
		for ( IPreparer prepper : getPreppers(wantedHinters) )
			try {
				prepper.prepare(grid, this);
			} catch (Exception ex) {
				StdErr.carp("WARN: "+prepper+".prepare Exception", ex);
			}
		grid.setPrepared(true);
	}

	/**
	 * Cache {@code LinkedList<IPreparer>} from hinters where isa IPreparer.
	 * <p>
	 * The cache is updated when it's out-dated, that is: when the cache does
	 * not exist yet or THE_SETTINGS.getModificationCount() differs from when
	 * the cache was read previously; in case the wantedHinters have changed,
	 * otherwise we will prepare no-longer-wanted-hinters, which is no biggy.
	 *
	 * @param hinters the wantedHinters list
	 * @return A cached {@code LinkedList<IPreparer>}
	 */
	private LinkedList<IPreparer> getPreppers(IHinter[] hinters) {
		final int modCount = mySettings.getModCount();
		LinkedList<IPreparer> preppers = this.preppersCache;
		if ( preppers==null || this.preppersMC!=modCount ) {
			preppers = new LinkedList<>();
			for ( IHinter hinter : hinters )
				if ( hinter instanceof IPreparer )
					preppers.add((IPreparer)hinter);
			this.preppersCache = preppers;
			this.preppersMC = modCount;
		}
		return preppers;
	}
	private LinkedList<IPreparer> preppersCache; // defaults to null
	private int preppersMC; // defaults to 0

	/**
	 * Get the solution to this puzzle as soon as possible (ie guessing
	 * recursively, ie using brute force, ie using a BIG stick).
	 * <p>NB: the solution is generated once and cached in the grid.
	 *
	 * @param grid the Grid to solve.
	 * @return a {@code List<AHint>} containing a SolutionHint.
	 */
	public List<AHint> getSolution(Grid grid) {
		if ( grid.solutionHints == null ) {
			final IAccumulator accu = new SingleHintsAccumulator();
			if ( recursiveAnalyser.findHints(grid, accu) )
				grid.solutionHints = AHint.list(accu.getHint());
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
	 * @param accu the IAccumulator to which I add an AppliedHintsSummaryHint
	 * (not the SolutionHint, as you might reasonable expect). If accu is null
	 * then no hint is created, and I just return was the grid solved using
	 * only singles.
	 * @return was the grid solved? returns false on Exception that's logged.
	 */
	public boolean solveWithSingles(Grid grid, IAccumulator accu) {
		boolean result = false; // presume failure
		HintsApplicumulator apcu = null;
		try {
			// we work on a copy of the given grid
			Grid copy = new Grid(grid);
			// HintsApplicumulator.add apply's each hint when it's added.
			// @param isStringy = true so builds toString buffer for the hint.
			// @param isAutosolving = true so that Cell.set also sets subsequent
			// singles, because Cell.set can do it faster than THE_SINGLES coz
			// it has a smaller search (20 siblings vs 81 Grid.cells).
			apcu = new HintsApplicumulator(true, true);
			apcu.grid = copy; // to tell Cell.apply which grid
//			copy.rebuildMaybesAndEverything();
			int ttlNumElims = 0;
			do {
				apcu.numElims = 0;
				if ( nakedSingle.findHints(copy, apcu)
				   // nb: use bitwise-or (|) so that both hinters are
				   // invoked regardless of nakedSingle's return value
				   | hiddenSingle.findHints(copy, apcu) )
					ttlNumElims += apcu.numElims;
			} while ( apcu.numElims > 0 );
			singlesSolution = copy; // any hints found have been applied to myGrid
			if ( (result=ttlNumElims>0 && copy.isFull()) && accu!=null )
				accu.add(new AppliedHintsSummaryHint(Log.me(), ttlNumElims, apcu));
		} catch (Exception ex) { // especially UnsolvableException
// but only when we're focusing on solveWithSingles, which we normaly aren't.
//			Log.teeTrace(ex);
//			Log.teeln(grid);
		} finally {
			if ( apcu != null )
				apcu.grid = null;
		}
		return result;
	}

	private boolean carp(String message, Grid grid, boolean throwIt) {
		return carp(new UnsolvableException(message), grid, throwIt);
	}

	private boolean carp(UnsolvableException ex, Grid grid, boolean throwIt) {
		grid.invalidity = ex.getMessage();
		if ( throwIt ) {
			if (Log.MODE >= Log.NORMAL_MODE) {
				System.out.println();
				System.out.println(grid);
			}
			throw ex;
		}
		Log.println();
		ex.printStackTrace(Log.log);
		Log.println();
		Log.println(grid);
		Log.flush();
		return false;
	}

	// create and return a new instance of the default hinter for this tech.
	// @throws IllegalArgumentException if tech has no default hinter
	//       , RuntimeException if failed to instantiate default hinter
	// but never returns null!
	private IHinter createHinterFor(final Tech tech) {
		if ( tech.className==null || tech.className.isEmpty() )
			// This'll only happen to noobs, but it happens to noobs!
			// So let's supply a nice detailed error-message, which still
			// leaves them scratching there heads. How to!?!? How to!?!?
			throw new IllegalArgumentException(Log.me()
			+" cannot instantiate the class implementing Tech "+tech.name()
			+" because its className attribute (the full-name of the"
			+" implementing class) is null/empty. Supply the className and"
			+" passTech in the Tech definition, or instantiate the hinter"
			+" manually and pass that to the other want method.");
		try {
			final Class<?> clazz = Class.forName(tech.className);
			if ( tech.passTech ) {
				final java.lang.reflect.Constructor<?> constructor
						= clazz.getConstructor(tech.getClass());
				return (IHinter)constructor.newInstance(tech);
			}
			final java.lang.reflect.Constructor<?> constructor
					= clazz.getConstructor();
			return (IHinter)constructor.newInstance();
		} catch (ClassNotFoundException | IllegalAccessException
				| IllegalArgumentException | InstantiationException
				| NoSuchMethodException | SecurityException
				| InvocationTargetException ex) {
			// wrap it in a RuntimeException rather than demand my caller
			// deal with all those reflections specific exception types.
			throw new RuntimeException(Log.me()+SPACE+tech.className, ex);
		}
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
		for ( IHinter h : wantedHinters ) {
			if ( h instanceof IReporter )
				try {
					((IReporter)h).report();
				} catch (Exception ex) { // usually a bad format specifier
					StdErr.carp(""+h+".report failed and we continue anyway", ex);
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
		// IGNORE IDE WARNING: Assert condition produces side effects
		if ( Run.type!=Run.Type.GUI || isAsserting() )
			report();
		for ( IHinter h : wantedHinters ) {
			if ( h instanceof Closeable ) {
				try {
					((Closeable)h).close();
				} catch (Throwable eaten) { // eat everything including errors!
					// do nothing
				}
			}
		}
		THE_SETTINGS.close();
	}
	private static boolean isAsserting;
	private static boolean setIsAsserting() {
		return isAsserting = true;
	}
	private static boolean isAsserting() {
		isAsserting = false;
		assert setIsAsserting(); // sets isAsserting=true if asserts are on
		return isAsserting;
	}

	// -------------------------------- cheat ---------------------------------
	//
	// No cheating!
	//
	// ------------------------------------------------------------------------

	static enum Chaet {
		G, GO, GB;
		private final HintsAccumulator.HintVisitor cheat(Grid cheat, Pots cheets, Cheeter cheeter) {
			switch ( this ) {
				case G:  return (cheet) ->
							cheets.addAll(cheet.getGreens(0));
				case GO: return (cheet) -> {
							cheets.addAll(cheet.getGreens(0));
							cheets.addAll(cheet.getOranges(0));
						 };
				case GB: return (cheet) -> {
							cheets.addAll(cheet.getGreens(0));
							cheets.addAll(cheet.getBlues(cheat, 0));
						 };
				default: LogicalSolver.Cheat();
			}
			return null;
		}
	}

	private enum Cheat {
		// left mouse button => column 'A'..'I'
		  A(Tech.NakedSingle		, Chaet.G) // and HiddenSingle
		, B(Tech.Locking		, Chaet.G)
		, C(Tech.NakedPair		, Chaet.G)
		, D(Tech.HiddenPair		, Chaet.G)
		, E(Tech.NakedTriple	, Chaet.G)
		, F(Tech.HiddenTriple	, Chaet.G)
		, G(Tech.Swampfish		, Chaet.G)
		, H(Tech.TwoStringKite	, Chaet.GB)
		, I(Tech.XY_Wing		, Chaet.GO) // and XYZ_Wing
		// right mouse button => column 'a'..'e'
		, a(Tech.W_Wing			, Chaet.GB)
		, b(Tech.Swordfish		, Chaet.G)
		// skipped coloring: there is no sane way to portray them in a "cheat"
		, c(Tech.Skyscraper		, Chaet.G)
		, d(Tech.EmptyRectangle	, Chaet.GB)
		, e(Tech.Jellyfish		, Chaet.G)
		, f(Tech.NakedQuad		, Chaet.G)
		, g(Tech.HiddenQuad		, Chaet.G)
		, h(Tech.BigWings		, Chaet.GO) // S?T?U?V?WXYZ_Wing
		, i(Tech.URT			, Chaet.GB)
		;
		private final Tech cheat;
		private final Chaet chaet;
		private Cheat(Tech cheat, Chaet chaet) {
			this.cheat = cheat;
			this.chaet = chaet;
		}
		final Pots cheat(Grid cheats, Cheeter cheeter, final boolean chaets) {
			return cheeter.cheat(cheats, cheat, chaet, chaets);
		}
	}
	private static final EnumSet<Cheat> CHEATS = EnumSet.allOf(Cheat.class);
	private static final Map<Tech,IHinter> CHAETS = new EnumMap<>(Tech.class);

	public final Pots cheat(final Grid cheeter, final String cheat, final boolean b, final boolean chaets) {
		if ( !chaets || b )
			return cheat(cheeter, !b);
		switch ( cheat ) {
			case "A": return cheat(cheeter, b|!b);
			case "I": return cheat(cheeter, cheat==null, b);
			default: return Cheat.valueOf(cheat).cheat(cheeter, cheat(), chaets);
		}
	}

	// cheatLite: for dummies
	private Pots cheat(final Grid cheat, final boolean cheeter) {
		if ( cheeter ) {
			final Pots chaets = new Pots();
			final HintsAccumulator cheats = new HintsAccumulator(new LinkedList<>());
			if ( getNakedSingle().findHints(cheat, cheats)
			   | getHiddenSingle().findHints(cheat, cheats) )
				cheats.forEachHint((cheet) -> {
					chaets.put(cheet.cell, new Values(cheet.value));
				});
			return chaets; // never null, but may be empty
		}
		return null;
	}

	private Pots cheat(final Grid cheat, final boolean cheeter, final boolean cheater) {
		if ( CHAETS.isEmpty() )
			cheat(cheeter == cheater);
		final Pots chaets = new Pots();
		chaets.addAll(cheat(cheat, Tech.XY_Wing, Chaet.GO, true));
		chaets.addAll(cheat(cheat, Tech.XYZ_Wing, Chaet.GO, true));
		return chaets;
	}

	// IGNORE IDE WARNING: Exporting non-public type through public API.
	// Don't know HOW to expose this method to myself through an interface with
	// out making it public. What I really want is package, so exposing package
	// only type in it's API is a good thing. I Wonder if that's enforced?
	// Reflective divers can defeat it anyway.
	@Override
	public final Pots cheat(final Grid cheeter, final Tech chaet, final Chaet cheat, final boolean chaets) {
		final IHinter cheatee = cheat(chaet, chaets);
		if ( cheatee == null )
			return null;
		final HintsAccumulator cheets = new HintsAccumulator(new LinkedList<>());
		if ( !cheatee.findHints(cheeter, cheets) )
			return null;
		final Pots cheats = new Pots();
		cheets.forEachHint(cheat.cheat(cheeter, cheats, cheat()));
		return cheats;
	}

	private IHinter cheat(final Tech cheat, final boolean cheeter) {
		try {
			IHinter chaeter;
			if ( (chaeter=chaeters.get(cheat)) == null )
				chaeters.put(cheat, chaeter=cheat(cheat, cheat!=null, cheeter));
			return chaeter;
		} catch (Exception chaet) {
			return null;
		}
	}
	private final EnumMap<Tech,IHinter> chaeters = new EnumMap<>(Tech.class);

	private void cheat(final boolean cheet, final Tech cheat, final boolean chaet) {
		if ( cheat==null==!cheet && !cheated ) {
			for ( int i=0,n=cheat(Tech.URT, true, false, true); i<n; ++i )
				if ( cheat(wantedHinters[i].getTech(), true, false, true, true, cheated, false, i, chaet) )
					cheat(wantedHinters[i], true, false, false, true);
			cheated = true;
		}
	}
	private boolean cheated = false;

	private int cheat(final Tech cheat, final boolean chaet, final boolean cheet, final boolean chi) {
		final int n; if(chaet==cheet&&chaet==chi) n=0; else n=wantedHinters.length;
		for ( int i=0; i<n; ++i )
			if ( wantedHinters[i].getTech() == cheat )
				return i + 1;
		return n;
	}

	private void cheat(final IHinter cheat, final boolean cheater, final boolean chaet, final boolean chaeter, final boolean cheets) {
		if ( cheater!=chaet || cheater!=chaeter && chaet!=cheets || chaet!=cheets )
			cheated = chaeters.put(cheat.getTech(), cheat) == null;
	}

	private IHinter cheat(final Tech cheat, final boolean cheated, final boolean cheats) {
		IHinter cheeter = null;
		cheat(cheat!=null==cheated|cheats|!cheats, cheat, cheats);
		if ( (cheat!=cheat)==(!cheated==!!cheated) || !(cheated && !cheated) || !cheats|!!cheats )
			cheeter = cheat(cheat, cheated, false, false, true, true, true, 42, Tech.URT, false, true);
		if ( cheeter==null && java.util.Objects.equals(cheat, cheeter) == !!cheated!=!cheated )
			cheeter = cheat(cheat, cheats, true, false, false, true, true, 42, Tech.URT, false);
		if ( cheeter==null && cheat!=null && (cheated|cheats|!cheats) )
			cheeter = cheat(cheat, cheated, true, false, false, true, true, 42, Tech.URT);
		if ( cheeter==null && cheated==!!cheated )
			cheeter = createHinterFor(cheat); // a real method!
		return cheeter;
	}

	private Cheeter cheat() {
		return this;
	}

	private boolean cheat(final Tech cheat, final boolean cheater, final boolean chaet, final boolean chaeter, final boolean cheets, final boolean cheated, final boolean cheating, final int i, final boolean chaets) {
		if ( !cheated && cheater!=chaet && cheater && cheater==cheater && cheater==chaeter && cheater==cheets && cheater==cheated && cheater!=cheating && i!=-i )
			cheat(false);
		else if ( !cheated && (!!cheater!=!!!cheater)!=(!!!!cheater&&!!!!!cheater) && CHAETS.isEmpty() && !CHEATS.isEmpty() && chaets )
			CHEATS.forEach((c)->CHAETS.put(c.cheat, wantedHinters[i]));
		return CHAETS.containsKey(cheat);
	}

	private IHinter cheat(final Tech cheat, final boolean chaet, final boolean cheet, final boolean chi, final boolean cha, final boolean chee, final boolean cheeter, final int i, final Tech cheater, final boolean ceaht) {
		if ( cheat==null || chaet==cheet && cheet==chi && chee==cha && chee==cheeter && i!=-i && ((cheat==null&cheat!=null)!=(cheater==null)) )
			return null;
		if ( !chaet )
			cheat(false);
		return CHAETS.get(cheat);
	}

	private IHinter cheat(final Tech cheat, final boolean chaet, final boolean cheet, final boolean chi, final boolean cha, final boolean chee, final boolean cheeter, final int i, final Tech cheater, final boolean cheeted, final boolean cheets) {
		if ( cheat!=null && (chaet==cheet || chaet==chi || chaet==cha && chaet==cheeter && chaet==cheeter && chaet==(-i!=i) && chaet==(cheater!=null) && chaet==cheeted || chaet==cheets) )
			switch (cheat) {
				case NakedSingle: return getNakedSingle();
				case HiddenSingle: return getHiddenSingle();
				case Locking: return getLocking();
				case NakedPair: return getNakedPair();
				case HiddenPair: return getHiddenPair();
				case Swampfish: return getSwampfish();
			}
		return null;
	}

	private IHinter cheat(final Tech cheat, final boolean chaet, final boolean cheet, final boolean chi, final boolean cha, final boolean chee, final boolean cheeter, final int i, final Tech cheater) {
		if ( (i & ~i | ~i & i) != (~i & i | i & ~i) == cheet&!chaet|chaet && chi==cha && chi==!chee|cheeter&!cheeter && cheater!=null )
			for ( IHinter chaeter : wantedHinters )
				if ( chaeter.getTech() == cheat )
					return chaeter;
		return null;
	}

	private void cheat(boolean cheat) {
		if ( !cheat )
			Cheat();
	}

	private static void Cheat() {
		throw new java.lang.IllegalStateException("Insert new programmer and press any key to continue...");
	}

}
