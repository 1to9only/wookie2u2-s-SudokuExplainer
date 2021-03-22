/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

// Sudoku Explainer (local) classes
// Tip: .* import local classes, but NEVER foreigners, because
// you can't change foreign class-names to resolve collisions.
import diuf.sudoku.solver.hinters.als.SueDeCoq;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.*;
import diuf.sudoku.gen.*;
import diuf.sudoku.io.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.align2.*;
import diuf.sudoku.solver.hinters.als.*;
import diuf.sudoku.solver.hinters.bug.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.solver.hinters.color.*;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.hdnset.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.nkdset.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.solver.hinters.sdp.*;
import diuf.sudoku.solver.hinters.lock2.LockingGeneralised;
import diuf.sudoku.solver.hinters.urt.*;
import diuf.sudoku.solver.hinters.wing.*;
import diuf.sudoku.utils.*;
// JAPI
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A LogicalSolver solves Sudoku puzzles as quickly as possible (as I'm able)
 * using logic, the challenge of which is what this whole project is about.
 * This version of Sudoku Explainer/HoDoKu/Sukaku was written "just for fun"
 * and the author (Keith Corlett) feels he must humbly apologise for producing
 * rather a lot of code which is pretty hard to maintain because it does NOT,
 * by choice, follow the edicts of any modern Java coding ethos. It's written
 * to run fast, not to be maintainable. Oops, I've just done a mental hammie.
 * <p>
 * If you're impatient then in the GUI (Options ~ Solving Techniques):<ul>
 * <li>use Locking (fast) instead of Locking Generalised (succinct)
 * <li>untick Direct* (they're found anyway);
 * <li>use Coloring (BUG++) over BUG (Coloring--)
 * <li>Death Blossom and Sue De Coq are border-line, but I keep them.
 * <li>Franken Jellyfish are border-line, but I keep them.
 * <li>untick Mutants (slow); especially JellyFish (too slow)
 * <li>untick Naked Pent and Hidden Pent (degenerate);
 * <li>untick Krakens (slow); especially JellyFish (too slow)
 * <li>untick Aligned*Exclusion (slow); A7+E are far too slow, and need shootin'
 * <li>for <u>really</u> hard puzzles you'll need Nested Unary, but the rest
 *  of Nested* is never used in anger. Nested Plus is a mandatory slow catch-all
 *  but is <u>never</u> executed in anger (exists for Shft-F5).
 * <li>My definition of "slow" is 100 milliseconds per elimination.
 * <li>My definition of "too slow" is a second per elimination.
 * <li>All of these hinters are as fast as I know how to make them, which is
 * "pretty fast", but I certainly cannot preclude the possibility of someone
 * else doing it faster.
 * </ul>
 * <p>
 * <b>LogicalSolver is used to:</b><ul>
 *  <li>Check the validity of a Sudoku puzzle (ie a grid).
 *  <li>Get the simplest available hint (a Sudoku solving step) from a grid.
 *  <li>Get all available hints, including chainers and wanted nested-chainers
 *  from a grid. Note that the GUI allows the user to manage the wantedHinters
 *  list, which I (the LogicalSolver) just get from the registry via Settings.
 *  <li>Solve a Sudoku puzzle logically to calculate its difficulty rating, and
 *  get a summary list of the hint-types (usages) that are required.
 * </ul>
 * <p>
 * <b>On packaging:</b><ul>
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
 * <li>The original Sudoku Explainer was heavily dependent on the Chainers, so
 * I boosted Solving Techniques from HoDoKu, each of which starts with "HoDoKu"
 * in the GUI, because kudos flows back to hobiwan. The mistakes are mine.
 * <li>Here's all the AHinters which have been "boosted" from HoDoKu:<ul>
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
 * </ul>
 * </ul>
 * <p>
 * <b>Automated tests:</b><ul>
 *   <li>There are {@code JUnit} tests in {@code ./test} directory, with the
 *    same packaging there-under as the class being tested; as per the JUnit
 *    and therefore NutBeans standard practice.<br>
 *    <br>
 *    These are tests for correctness, using the minimum amount of the codebase
 *    required to test the examined feature. The majority of tests are on a
 *    subtype-of-AHinter. They check that a given puzzle produces the expected
 *    hint, with the given hint-HTML. Keeping the expected-HTML files up-to-date
 *    is a bit of a pain-in-the-ass, but I think it's worth it.<br>
 *    <br>
 *    In Nutbeans: build HoDoKu.jar, then run all JUnit tests with ALT-F6. They
 *    take about a minute. These are mostly just regression tests, which just
 *    diff the output with a previous one, so anytime you change the display of
 *    a hint you also need to update the test-case/s. Some hint-types do NOT
 *    yet have test-cases, because I'm a slack/lazy/messy programmer. It's a
 *    work in progress, and I'm doing this just for fun, so it's not perfect.
 *   <li>And then I use {@link diuf.sudoku.test.LogicalSolverTester} with
 *    {@code Log.MODE = VERBOSE_4_MODE} to performance test the hinters on
 *    {@code top1465.d5.mt}, which is in the root directory of this project.
 *    This currently takes under 4 minutes to run (-A5..10E).
 *  </ul>
 * <p>
 * @see ./LogicalSolverTimings for run timings.
 */
public final class LogicalSolver {

	/**
	 * true uses new "align2", which is a slower than the old "align" package.
	 * Normal people would chuck align2, but I can't because I just love how
	 * succinct and clever it is, so I seek a magic efficiency bullet. sigh.
	 */
	private static final boolean USE_ALIGN2 = false; // @check false align2 slow

	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	/** Enum for a self-documenting Mode parameter in the Constructor/s. */
	public enum Mode {
		/**
		 * ACCURACY mode uses the simplest (lowest difficulty) hinter to
		 * produce the next hint, which is slower than SPEED mode, but yields
		 * a far more accurate assessment of the difficulty involved in solving
		 * a Sudoku puzzle. If you're unsure then just use this one.
		 */
		ACCURACY
		/**
		 * SPEED mode throws all the toys out of the pram and attempts to solve
		 * each Sudoku puzzle as quickly as possible using logic, but if all
		 * you really want is the solutions then try the RecursiveSolverTester,
		 * which just left it's leftover lightning in your knicker draw.
		 */
		, SPEED
	}

	/** These Hinters check that the puzzle is valid. They should be used ONCE
	 * on each puzzle. They are fields coz they're called independently. */
	private IHinter[] puzzleValidators;
	/** Does this puzzle have at least 17 clues?. */
	private final TooFewClues tooFewClues = new TooFewClues();
	/** Does this puzzle contain 8 values?. */
	private final TooFewValues tooFewValues = new TooFewValues();
	/**
	 * Does this puzzle have 1 solution?<br>
	 * * solves the puzzle twice: once forwards and once backwards<br>
	 * * SolutionMode=STORED means we'll retrieve the solution later<br>
	 * * is a field for use by solveRecursively.
	 */
	public final RecursiveAnalyser recursiveAnalyser = new RecursiveAnalyser(); // SolutionMode.STORED

	/** These Hinters workout if the grid is still valid. */
	private IHinter[] gridValidators;

	/** This is a list of both the puzzleValidators and gridValidators, which is
	 * what we want to run most of the time. */
	private IHinter[] validators;

	/** These Hinters set cell values directly. */
	private List<IHinter> directs;
	private final NakedSingle nakedSingle = new NakedSingle();
	private final HiddenSingle hiddenSingle = new HiddenSingle();

	/** These Hinters remove maybes, setting cell values only indirectly. */
	private List<IHinter> indirects;
	/** we locking.setIsSiamese(true) in the GUI only. */
	public Locking locking;
	/** Hidden Pair/Triple used (cheekily) by Locking to upgrade siamese
	 * locking hints to the full HiddenSet hint, with extra eliminations. */
	public HiddenSet hiddenPair;
	/** Hidden Pair/Triple used (cheekily) by Locking to upgrade siamese
	 * locking hints to the full HiddenSet hint, with extra eliminations. */
	public HiddenSet hiddenTriple;

	/** These Hinters are a bit slow Redge. It's under the bonnet son. */
	private List<IHinter> heavies;

	/** These Hinters deduce from an assumption, which is damn slow.
	 * Includes the later A*E's coz they're even slower than the chainers. */
	private List<IHinter> chainers;

	/** These Hinters deduce from deductions. Number 5 deduced from deductions
	 * of deductions. He loves game shows and thinks he's a ____ing apricot. */
	private List<IHinter> nesters;

	/** This is a list of all the wanted hinters from the directs, indirect,
	 * heavies, and chainers (but NOT the nesters). */
	public List<IHinter> wantedHinters;

	/** If a Tech is wanted by the user (in the Options ~ Solving Techniques
	 * Dialog) then it'll be used to solve puzzles, else it won't.
	 * <p>see: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku} */
	private EnumSet<Tech> wantedTechs = Settings.THE.getWantedTechniques();

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

	/** The {@code Mode} of this Logical Solver. Determines which AHinters
	 * are used by: {@code solve}, {@code analyseDifficulty}, and
	 * {@code getFirstHint}. */
	final Mode mode; // SPEED or ACCURACY

	/** My monitor (the parent Generator) to pass to hinters so that they can
	 * interrupt themselves when the user interrupts the parent Generator.
	 * So null except when I'm created by the Generator. */
	IInterruptMonitor monitor = null;
	public void setMonitor(IInterruptMonitor monitor) {
		this.monitor = monitor;
	}

	/** Should LogicalSolverTester use the statistics of its previous execution
	 * to sort the wantedHinters by nanoseconds per elimination, so that it
	 * runs faster, but less accurately on each execution, pairing the list
	 * down to only the bang-for-buck hinters. I've written this only for the
	 * challenge of doing so, not because it's useful for anything. It was/is
	 * its own Sudoku Puzzle, for a programmer. */
	private final boolean isUsingStats;

	/** singlesSolution is weird: it's set by LS.solveWithSingles and then read
	 * back by SudokuExplainer.getTheNextHint. */
	public Grid singlesSolution;

	/**
	 * Constructs a new LogicalSolver in the given Mode using the given
	 * IInterruptMonitor. Note that this constructor is package visible, so the
	 * only way for the application to create a new LogicalSolver is via the
	 * {@link LogicalSolverFactory}. This is intentional.
	 *
	 * @param mode Mode see {@link configureHinters(Mode mode)} comments.
	 * @param monitor the parent Generator. Only non-null when created
	 * by the Generator so that analyseDifficulty and the big (long running)
	 * A*E's exit early when the user interrupts the Generator.
	 */
	LogicalSolver(Mode mode) {
		this.mode = mode;
		this.isUsingStats = false;
		configureHinters();
	}

	/**
	 * The report() method is called by the LogicalSolverTester to print a
	 * report from each IHinter which implements IReporter.
	 * <p>
	 * To date the only IReporters are Aligned*Exclusion, but the other
	 * IHinters could become IReporters in future.
	 * <p>
	 * The reporting convention is that you print a blank like, then a header
	 * line which should identify which class is reporting, and then all your
	 * report content with NO blank lines there-in.
	 * <p>
	 * For example:
	 * <pre>{@code public void report() {
	 *      Log.teef("\nAligned5Exclusion currently: %s\n", A5E_VERSION);
	 *      ... tee your report to both stdout and the Log file ...
	 * }}</pre>
	 */
	public void report() {
		for ( IHinter h : wantedHinters ) {
			if ( h instanceof IReporter ) try {
				((IReporter)h).report();
			} catch (Exception ex) { // probably an IOException
				StdErr.carp(""+h+" failed to report (and we continue anyway)", ex);
			}
		}
	}

	/**
	 * The close() method releases any/all resources. In this case that means
	 * persisting any set-settings in any/all of the Filter/s we've been using.
	 * <p>
	 * Closeable defines this method as throwing IOException, so I have followed
	 * along, but this implementation eats all Throwables, ie it never throws
	 * anything, ever, which is a MUCH better approach for closing I/O AFAIAC.
	 * <p>
	 * The Closeable.close() method may be called either by autoclose, which I
	 * guess is the {@code try(ClosableRdr rdr = new ClosableRdr()){}} syntax,
	 * or with a straight-up <pre>{@code solver.close();}</pre>.
	 *
	 * @throws java.io.IOException officially, but never in practice.
	 */
	public void close() throws java.io.IOException {
		for ( IHinter h : wantedHinters ) {
			if ( h instanceof Closeable ) {
				try {
					((Closeable)h).close();
				} catch (Throwable eaten) {
					// Do nothing. What can I do about it? We're closing down!
				}
			}
		}
	}

	/** return has the monitor (parent Generator) been interrupted by user.
	 * isInterrupted would be a better name, but it's too long. */
	private boolean interrupt() {
		final IInterruptMonitor m = this.monitor;
		return m!=null && m.isInterrupted();
	}

	/**
	 * Called by constructor/s and setMode to populate the lists of Hinters,
	 * especially the wantedHinters list, which is the one solve (et al) uses.
	 * The sub-lists except validators can go. Nobody cares about categories.
	 * <p>
	 * field mode {@code Mode.ACCURACY} includes the slow Techniques so that
	 * this LogicalSolver produces the simplest possible solution to the given
	 * puzzle; whereas {@code Mode.SPEED} cheats like a dirty mother trucker.
	 */
	private void configureHinters() { // Why so serious?

		// This method adds each new hinter to it's category-list in order of
		// increasing complexity, ergo the difficulty rises. The order in which
		// hinters are created is the order in which they normally execute.
		// Then we manually reorder the chainers if !CARE_ABOUT_ALIGNED_HINTS.
		// Then we populate the wantedHinters list from the category-lists.
		// The hardest puzzles require NestedUnary, but never used otherwise.
		// Then we sort the wantedHinters by ns/elim if we isUsingStats.

		// when isAccurate we run more hinters, to get the simplest hints.
		final boolean isAccurate = (mode == Mode.ACCURACY);
		// when isAggregate chainers lump all there hints into one.
		final boolean a = (mode == Mode.SPEED);

		// F is shorthand for false; to make the code fit on a line.
		final boolean F = false;
		// im is shorthand for InterruptMonitor
		final IInterruptMonitor im = monitor;

		// refetch the wantedTechs field, in case they've changed; then my want
		// method reads this field, rather than re-fetching it every time.
		this.wantedTechs = Settings.THE.getWantedTechniques();

		// puzzleValidators check puzzle obeys Sudoku rules once, upon load.
		puzzleValidators = new IHinter[] { // Dial 1900 Love-a-Duck Now!
			  tooFewClues		// minimum 17 clues
			, tooFewValues		// minimum 8 values
			, recursiveAnalyser // single solution (brute-force solve)
		};

		// gridValidators check invariants in the grids state. Run before the
		// actual hinters. A wrong guess in bruteforce eventually trips these.
		gridValidators = new IHinter[] { // Eel Vomit O'Gratten Batman!
			  new NoDoubleValues()		// one 1 per box, row, col
			, new NoMissingMaybes()		// value unset and no maybes
			, new NoHomelessValues()	// value unplaced and no places
		};

		// convenience list to run all the validators.
		validators = new IHinter[] { // A Chuckducken, obviously!
			  puzzleValidators[0]
			, puzzleValidators[1]
			, puzzleValidators[2]
			, gridValidators[0]
			, gridValidators[1]
			, gridValidators[2]
		};

		// Regarding hinter selection:
		// In ACCURACY mode we include many nice-but-unnecessary hinters, but
		// in SPEED mode we strip the hinters list down to the bare-minimum.
		// We allways use Naked Single and Hidden Single because not having
		// them is just plain bonkers; as well as DynamicPlus (the catch-all)
		// and NestedPlus (the real catch-all). The user unwants other hinter
		// types in the GUI, which changes the Settings (persist in registry),
		// which the want method reads via the wantedTechs field, which I
		// refresh at the start of this method.

		// Regarding execution order:
		// The solve/etc methods just loop-through the wantedHinters calling
		// each in the order that it appears in that list, which is MOSTLY the
		// order in which they are added to the list, ie the order in which
		// they appear in this method. Aligned*Exclusion are re-ordered if you
		// don't CARE_ABOUT_ALIGNED_HINTS. sigh. So, the run-order is (mostly)
		// the same as the order they're added in this method. Clear?

		// directs are hinters which set cell values directly
		directs = new ArrayList<>(isAccurate ? 7 : 2); // Tree point five?
		if ( isAccurate && Run.type==Run.Type.GUI )
			want(directs, new LonelySingles());
		directs.add(nakedSingle);
		directs.add(hiddenSingle);
		if ( isAccurate ) {
			want(directs, new NakedSet(Tech.DirectNakedPair));
			want(directs, new HiddenSet(Tech.DirectHiddenPair));
			want(directs, new NakedSet(Tech.DirectNakedTriple));
			want(directs, new HiddenSet(Tech.DirectHiddenTriple));
		}

		// indirects just remove maybes, setting cell values only indirectly
		indirects = new ArrayList<>(14); // one too many!
		// Locking or LockingGeneralised (you can't have neither, nor both.
		// Locking is a fundamental tech, like naked and hidden singles)
		want(indirects, locking = new Locking()); // Pointing and Claiming
		if ( !wantedTechs.contains(Tech.Locking) )
			indirects.add(new LockingGeneralised());
		else if ( Log.MODE >= Log.NORMAL_MODE )
			Log.println("LogicalSolver: unwanted: LockingGeneralised");
		want(indirects, new NakedSet(Tech.NakedPair));
		// nb: hiddenPair used (cheekily) by Locking to upgrade siamese
		// locking hints to the full HiddenSet hint, with extra eliminations.
		want(indirects, hiddenPair = new HiddenSet(Tech.HiddenPair));
		want(indirects, new NakedSet(Tech.NakedTriple));
		// nb: hiddenTriple used (cheekily) by Locking to upgrade siamese
		// locking hints to the full HiddenSet hint, with additional elims.
		want(indirects, hiddenTriple = new HiddenSet(Tech.HiddenTriple));
		want(indirects, new TwoStringKite());
		want(indirects, new BasicFisherman(Tech.Swampfish)); //aka X-Wing
		want(indirects, new XYWing(Tech.XY_Wing));
		want(indirects, new XYWing(Tech.XYZ_Wing));
		want(indirects, new WWing());
		want(indirects, new Skyscraper());
		want(indirects, new EmptyRectangle());
		want(indirects, new BasicFisherman(Tech.Swordfish));

		// heavies is the heavy weight division. Slow/er indirect hinters.
		heavies = new ArrayList<>(isAccurate ? 38 : 0); // Just 4 short Dougie!
		if ( isAccurate ) {
			// nb: NONE means that there are 0 top1465.d5.mt
			//     SLOW takes over a minute to run for top1465
			//     TOO SLOW is something like 5 minutes (can't make up my mind)
			//     Then there's Aligned Oct, Nona, and Dec taking hours/days!
			want(heavies, new NakedSet(Tech.NakedQuad));
			want(heavies, new HiddenSet(Tech.HiddenQuad)); // NONE
			want(heavies, new BasicFisherman(Tech.Jellyfish));
			want(heavies, new NakedSet(Tech.NakedPent));	   // NONE
			want(heavies, new HiddenSet(Tech.HiddenPent)); // NONE
			// these three are limited faster ALS-XZ
			want(heavies, new BigWing(Tech.WXYZ_Wing));
			want(heavies, new BigWing(Tech.VWXYZ_Wing));
			want(heavies, new BigWing(Tech.UVWXYZ_Wing));
			// these two are actually slower per elim than ALS-XZ.
			// unwant them for speed. It's only fractions of a second. Meh!
			want(heavies, new BigWing(Tech.TUVWXYZ_Wing));
			want(heavies, new BigWing(Tech.STUVWXYZ_Wing));
			// Coloring xor BUG (you can have neither, but not both). This is
			// enforced in the TechSelectDialog, so you can regedit around it.
			want(heavies, new BivalueUniversalGrave()); // slowish
			want(heavies, new Coloring()); // BUG++
			want(heavies, new XColoring()); // Coloring++ (CURRENTLY BROKEN!!!)
			want(heavies, new UniqueRectangle());
			// ComplexFisherman now detects Sashimi's in a Finned search.
			want(heavies, new ComplexFisherman(Tech.FinnedSwampfish));
			want(heavies, new ComplexFisherman(Tech.FinnedSwordfish));
			want(heavies, new ComplexFisherman(Tech.FinnedJellyfish));
			want(heavies, new AlsXz());
			want(heavies, new AlsXyWing());
			want(heavies, new AlsXyChain());
			want(heavies, new DeathBlossom());
			want(heavies, new SueDeCoq()); // AALS's
			want(heavies, new ComplexFisherman(Tech.FrankenSwampfish));	// NONE
			want(heavies, new ComplexFisherman(Tech.FrankenSwordfish));	// OK
			want(heavies, new ComplexFisherman(Tech.FrankenJellyfish));	// OK
			// Krakens and Mutants are interleaved: Swamp, Sword, and Jelly
			want(heavies, new KrakenFisherman(Tech.KrakenSwampfish));	// OK
			want(heavies, new ComplexFisherman(Tech.MutantSwampfish));	// NONE
			want(heavies, new KrakenFisherman(Tech.KrakenSwordfish));	// SLOW
			want(heavies, new ComplexFisherman(Tech.MutantSwordfish));	// SLOW
			want(heavies, new KrakenFisherman(Tech.KrakenJellyfish));	// SLOW
			want(heavies, new ComplexFisherman(Tech.MutantJellyfish));	// TOO SLOW

			// new align2.AlignedExclusion is faster than the old align package
			// for A2E, A3E, and A4E, which are all "relatively fast".
			// But takes 3 times A5E, 4 times A6E, 5 times A7E; presumably and
			// so on for A8E, A9E, and A10E; which is TOO LONG!
			want(heavies, new AlignedExclusion(Tech.AlignedPair));
			want(heavies, new AlignedExclusion(Tech.AlignedTriple));
			want(heavies, new AlignedExclusion(Tech.AlignedQuad));
			if ( USE_ALIGN2 ) {
				// The user chooses if A5+E is correct (slow) or hacked (fast).
				// The hacked version is about ten times faster.
				// The hacked version finds about a third of the hints.
				// AlignedExclusion constr read "isa"+degree+"ehacked" Setting.
				want(heavies, new AlignedExclusion(Tech.AlignedPent));
				want(heavies, new AlignedExclusion(Tech.AlignedHex));
				want(heavies, new AlignedExclusion(Tech.AlignedSept));
				want(heavies, new AlignedExclusion(Tech.AlignedOct));
				want(heavies, new AlignedExclusion(Tech.AlignedNona));
				want(heavies, new AlignedExclusion(Tech.AlignedDec));
			} else {
//				want(heavies, new Aligned2Exclusion()); // fast enough.
//				want(heavies, new Aligned3Exclusion()); // fast enough.
//				want(heavies, new Aligned4Exclusion(im)); // acceptable.
				// The user chooses if A5+E is correct (slow) or hacked (fast).
				// The hacked version is about ten times faster.
				// The hacked version finds about a third of the hints.
				// wantAE read "isa"+degree+"ehacked" Setting to constr appr.
				wantAE(heavies,  5, im); // isabeet slow! User choice.
				wantAE(heavies,  6, im); // reeally slow! User choice.
				wantAE(heavies,  7, im); // ____ing slow! User choice.
				wantAE(heavies,  8, im); // 29yr old dog! User choice.
				wantAE(heavies,  9, im); // old tortoise! User choice.
				wantAE(heavies, 10, im); // conservative! User choice.
			}
		}

		// Chainers are actually faster than the slow Heavies ()
		chainers = new ArrayList<>(isAccurate ? 5 : 1);
		if (isAccurate) {
			// single value forcing chains and bidirectional cycles.
			want(chainers, new UnaryChainer(F));
			// contradictory consequences of a single (bad) assumption.
			want(chainers, new MultipleChainer(Tech.NishioChain, F));
			// forcing chains from more than 2 values/positions.
			want(chainers, new MultipleChainer(Tech.MultipleChain, F));
			// forcing chains with consequences recorded in the grid.
			// does Unary Chains not Cycles, and Multiple Chains not Nishios.
			want(chainers, new MultipleChainer(Tech.DynamicChain, F));
		} // fi isAccurate
		// DynamicPlus is Dynamic Chaining + Four Quick Foxes (Point & Claim,
		// Naked Pairs, Hidden Pairs, and Swampfish). It only misses on hardest
		// puzzles, so hardcoded as safery-net, so user can't unwant it.
		chainers.add(new MultipleChainer(Tech.DynamicPlus, a)); // safety-net

		// Nested means imbedded chainers: assumptions on assumptions.
		// The only way to ever see a nested hint is with Shift-F5 in the GUI.
		nesters = new ArrayList<>(4);
		// advanced					 // approx time per call on an i7 @ 2.9GHz
		// NestedUnary covers The Hardest Sudoku according to conceptis.com
		want(nesters, new MultipleChainer(Tech.NestedUnary, a));	//  4 secs
		want(nesters, new MultipleChainer(Tech.NestedMultiple, a));	// 10 secs
		// experimental
		want(nesters, new MultipleChainer(Tech.NestedDynamic, a));	// 15 secs
		// NestedPlus is Dynamic + Dynamic + Four Quick Foxes => confusing!
		// NestedPlus is the real safety-net (so user can't unwant it), but
		// it's (mostly) only invoked in Shift-F5 coz NestedUnary hints first.
		// Only THE HARDEST puzzles get through to NestedUnary; so if it's on
		// it always hints before we get through to the ultimate keeper.
		// @bug 2020 AUG: NestedPlus produced invalid hints, so now uses the	// PRODUCED INVALID HINTS!
		// HintValidator to log and ignore them. NOT adequate! Needs fixing!
		// I'm just ignoring this bug for now coz IT'S NEVER USED in anger.
		nesters.add(new MultipleChainer(Tech.NestedPlus, a));		// 70 secs

		populateTheWantedHintersList(); // => wantedHinters

		// Sort the wantedHinters by nanoseconds per elimination ASCENDING
		// experienced by the previous execution when "-STATS", so that faster,
		// more effective hinters are executed sooner.
		// NB: This causes inaccuracies in the analysis results, but I'll wear
		// that (for now) just for the challenge of writing "self improving"
		// software, which uses the stats of its own performance to improve its
		// own performance; a bit like the stats underneath a RDBMS QEP.
		if ( isUsingStats ) {
			// NB: LogicalSolverTester writes IO.PERFORMANCE_STATS
			// NB: ByNsPerElimAsc constructor loads IO.PERFORMANCE_STATS
			ByNsPerElimAsc comparator = new ByNsPerElimAsc();
			if ( comparator.isFullyLoaded )
				wantedHinters.sort(comparator);
			if (Log.MODE >= Log.VERBOSE_1_MODE)
				dumpWantedEnabledHinters(Log.out); // defined below
		}
	}

	/** Populates the wantedHinters list with directs, indirects, heavies,
	 * chainers, and nesters; so that we can just loop through them without
	 * worrying about which category they're in. */
	@SuppressWarnings("unchecked")
	private void populateTheWantedHintersList() {
		wantedHinters = new ArrayList<>(Settings.THE.getNumWantedTechniques());
		//NB: unwanted hinters are already filtered-out of the source lists.
		wantedHinters.addAll(directs);
		wantedHinters.addAll(indirects);
		wantedHinters.addAll(heavies);
		// chainers allways includes DynamicPlus which allways finds a hint.
		wantedHinters.addAll(chainers);
		//NB: I've added the nesters, even though I don't want to because I dont
		// want to wait for them in the event that DynamicPlus fails to hint.
		// I still regard that as an "A grade" bug, but I've added them because
		// I want to find out-how long it takes to solve the hardest Sudoku
		// puzzle in the world (according to conceptis.com):
		//8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..
		wantedHinters.addAll(nesters);
	}

	/**
	 * Reconfigures this LogicalSolver to use only the given hintyHintersNoms,
	 * ie those hinters which hinted in a previous run of LogicalSolverTester.
	 * <p><b>WARNING</b>: This may result in hints being missed if the software
	 * has changed in the meantime, but it is faster when it works. Lookout for
	 * additional (other and 1 and 3) "Dynamic Plus"s in stdout. That means you
	 * are missing hints. Either you've broken a hinter, or you need to produce
	 * a new "keeper" logFile to update the hintyHintersNoms.
	 *
	 * @param hintyHintersNoms a {@code Set<String>} of the nom's (French for
	 * name, which is reserved by EnumSet) of hinters that produced hints in a
	 * recent run of LogicalSolverTester.<br>
	 * <b>ENHANCEMENT</b>: INumberedHinters optionally follows nom with a space
	 * then a space-separated-list of the hintNumbers at which this hinter
	 * hints (nullable, never an empty Set); if null then the hinter is
	 * activated at the firstHintNumber passed into its constructor, which was
	 * extracted manually from the "keeper" logFile. These hardcoded numbers
	 * are specific to top1465 (ie it's a dirty hack).
	 * @return true if successfully reconfigured, else false. You can workout
	 * if you want to proceed anyway or not.
	 */
	public boolean reconfigureToUse(Set<String> hintyHintersNoms) {
		if ( hintyHintersNoms==null || hintyHintersNoms.isEmpty() )
			return false;
		for ( IHinter hinter : wantedHinters ) {
			final Tech tech = hinter.getTech();
			switch ( tech ) {
			// these are allways on, regardless.
			case NakedSingle: // disabling singles is nonsensicle.
			case HiddenSingle:
			case DynamicPlus: // NB: the catch-all is allways enabled.
				hinter.setIsEnabled(true);
				break;
			default:
				if ( hinter instanceof INumberedHinter )
					// provide the hintNumbers to activate these on the fly.
					hinter.setIsEnabled(setHintNumbers(hintyHintersNoms, tech
							, (INumberedHinter)hinter));
				else
					hinter.setIsEnabled(hintyHintersNoms.contains(tech.nom));
			}
		}
		return true;
	}

	// sets the given hnah's hintNumbers and returns should it be enabled?
	private boolean setHintNumbers(
		  Set<String> hintyHintersNoms
		, Tech tech
		, INumberedHinter hnah
	) {
		assert hintyHintersNoms!=null && tech!=null && hnah!=null;
		boolean isEnabled = false;
		int nomLen = tech.nom.length();
		for ( String hhNom : hintyHintersNoms )
			// hhNom is hintyHinterNom (frog for Name) ie Tech.nom, which
			// may be followed by a space-seperated-list of hintNumbers.
			if ( hhNom.startsWith(tech.nom) ) {
				isEnabled = true; // it's enabled, but is it active?
				if ( hhNom.length() > nomLen )
					// will be activated only for the given hintNumbers
					hnah.setHintNumbers(Frmt.integerSetFromCsv(
							hhNom.substring(nomLen+1), " "));
				else
					// will be active from it's minHintNumber onwards
					hnah.setHintNumbers(null);
				break;
			}
		return isEnabled;
	}

	/**
	 * getNumberedHinterNoms: Used by LogicalSolverTester's static initialiser
	 * to get a list of the  wanted INumberedHinters, except DynamicPlus which
	 * is always wanted, hence we just don't check if it's wanted, which is
	 * rapidly becoming more complicated than just ensuring that the bastard's
	 * always in the wanted list. Sigh.
	 * @return {@code String[]} noms of wanted INumberedHinters.
	 */
	public String[] getNumberedHinterNoms() {
		LinkedList<String> list = new LinkedList<>();
		for ( IHinter hinter : wantedHinters )
			if ( hinter instanceof INumberedHinter
			  // the catch-all DynamicPlus is ALWAYS active so it's excluded
			  // from the list of things to be de-activated by hint number.
			  // Done ONCE when creating the list, instead of repeatedly while
			  // processing the list, where it probably should be. Sigh.
			  && hinter.getTech() != Tech.DynamicPlus )
				list.add(hinter.getTech().nom);
		return list.toArray(new String[list.size()]);
	}

	// clean-up before the GC runs
	public void cleanUp() {
		for ( IHinter h : wantedHinters )
			if ( h instanceof ICleanUp )
				((ICleanUp)h).cleanUp();
	}

	/**
	 * A better name would be addHinterIfWanted but that's just too verbose.
	 * If this 'hinter' is wanted then add it to the Collection 'c'; otherwise
	 * just whinge about it in the log-file: you may want that later.
	 *
	 * @param c {@code Collection<IHinter>} the hinters-list to add this hinter
	 *  to, if it's wanted.
	 * @param hinter to want, or not.
	 */
	private void want(Collection<IHinter> c, IHinter hinter) {
		// remember Resetables even if not wanted. It can't hurt.
		// add to c only if wanted
		if ( wantedTechs.contains(hinter.getTech()) )
			c.add(hinter);
		else if ( Log.MODE >= Log.NORMAL_MODE )
			Log.println("LogicalSolver: unwanted: "+hinter.getTech().name());
	}

	/**
	 * The wantAE (addAlignedExclusionIfWanted) method creates either:<br>
	 * the _1C (Correct meaning align each cell set on a single cell);<br>
	 * or _2H (Hacked meaning align each cell set around at least two cells)<br>
	 * version of an Aligned*Exclusion class, depending on registry Settings.
	 * <p>
	 * See the Aligned*Exclusion classes for more on the difference between the
	 * Correct version and it's much faster but less-hinty Hacked equivalent.
	 * <p>
	 * It exists to handle the user-selectable A*E's:<ul>
	 * <li>A5E, A6E, A7E, A8E are user selectable (correct mode or hacked):<ul>
	 *  <li>_2H for Hacked (fast) 2-excluder-cells finds about a third of hints
	 *   in about a tenth of the time (ie smartass cheating bastard mode); OR
	 *  <li>_1C for Correct (slow) 1-excluder-cell mode for more hints is
	 *   4*4*4 + 5*5*5*5 + 6*6*6*6*6 + 7*7*7*7*7*7 + 8*8*8*8*8*8*8
	 *   + 9*9*9*9*9*9*9*9 + 9*9*9*9*9*9*9*9*9 = 432,690,476 more head-aches.
	 *  </ul>
	 * <li>A2E, A3E, and A4E are correct only. There is no _1C or _2H.
	 * <li>A9E and A10E are both hacked permanently, ie checkbox disabled.
	 *   While both these _1C classes exist they're not tested (too slow).
	 * </ul>
	 * NB: looking-up the class name and instantiating it through reflection
	 * is slower than instantiating it directly, which would require a big
	 * switch, which I don't like. This is fast enough as long as the
	 * constructor/s and configureHinters stay off the critical path.
	 * The downside of late-linking is design tools miss these usages.
	 * SEE: class comment block in AAlignedSetExclusionBase for more.
	 *
	 *
	 * @param list {@code List<IHinter>} the hinters list to add the new
	 *  instance to
	 * @param num the number of cells in the aligned sets to be processed
	 * @param im interruptMonitor to pass through to the A*E
	 */
	private void wantAE(List<IHinter> list, int num, IInterruptMonitor im) {
		final String name = "Aligned "+WOG[num];
		if ( Settings.THE.getBoolean(name, false) ) {
			try {
				final String className = "diuf.sudoku.solver.hinters.align."
					+ "Aligned"+num+"Exclusion"
					+ (Settings.THE.get("isa"+num+"ehacked") ? "_2H" : "_1C");
				final Class<?> clazz = Class.forName(className);
				final java.lang.reflect.Constructor<?> constructor =
					clazz.getConstructor(IInterruptMonitor.class);
				final IHinter hinter =
					(IHinter)constructor.newInstance(im);
				list.add(hinter);
			} catch (Exception ex) {
				StdErr.exit("wantAE fubarred", ex);
			}
		} else if ( Log.MODE >= Log.NORMAL_MODE )
			Log.println("LogicalSolver: unwanted: "+name.replaceFirst(" ", ""));
	}
	// num in wogalini. Think shape: a hexagon has sex sides, in New Zealand.
	private static final String[] WOG = new String[] {
		  "#0", "#1", "Pair", "Triple", "Quad", "Pent", "Hex", "Sept", "Oct"
		, "Nona", "Dec"
	};

	private static final IFormatter<IHinter> TECH_NAME = (IHinter h) -> h==null
			? ""
			: h.getTech().name();

	/** Tester Logging Only: This method is used by LogicalSolverTester to
	 * display the names of the "wanted" hinters to the developer.
	 * @return A CSV (", ") list of the wanted hinter.getTech().name()s. */
	public final String getWantedHinterNames() {
		return Frmt.csv(wantedHinters, TECH_NAME);
	}

	private static final IFilter<IHinter> ENABLED = (IHinter h) -> h.isEnabled();

	/** Debug only: dumps wanted hinters to the given PrintStream.
	 * @param out PrintStream to print to. */
	public void dumpWantedEnabledHinters(PrintStream out) {
		out.print("wantedEnabledHinters: ");
		Frmt.csv(out, wantedHinters, ENABLED, TECH_NAME);
		out.println();
	}

	/**
	 * Checks the validity of the given Grid completely, ie using all puzzle
	 * and grid validators. If quite then there's no logging, else noisy.
	 * <p>
	 * Call validatePuzzleAndGrid ONCE before solving each puzzle, other
	 * validations should do the puzzle only.
	 *
	 * @param grid Grid to validate.
	 * @param isNoisy true to log, false to shut the ____ up.
	 * @return the first warning hint, else null. */
	public AHint validatePuzzleAndGrid(Grid grid, boolean isNoisy) {
		IAccumulator accu = new SingleHintsAccumulator();
		AHint problem = null;
		if ( getFirst(validators, grid, accu, isNoisy) )
			problem = accu.getHint();
		return problem;
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
	 * @param prev previousHints
	 * @param curr currentHints
	 * @param accu SingleHintsAccumulator whose add returns true so that the
	 * hinter abandons the search and exits early. If you pass me a
	 * (Default)HintsAccumulator then my behaviour is "interesting" (I don't
	 * really understand what happened) so now I insist on a SingleHA.
	 * @return true if a hint was found, else false; always true AFAIK.
	 */
	public boolean getFirstHint(Grid grid, List<AHint> prev, List<AHint> curr
			, SingleHintsAccumulator accu) {
		if ( grid.isFull() ) {
			accu.add(new WarningHint(recursiveAnalyser
					, "The Sudoku has been solved", "SudokuSolved.html"));
			return true;
		}
		++AHint.hintNumber;
		grid.rebuildAllRegionsEmptyCellCounts();
		grid.rebuildAllRegionsIdxsOfAllValues();
		if ( getFirst(validators, grid, accu, false)
		  || getFirstCat(grid, prev, curr, accu, "Directs", directs)
		  || getFirstCat(grid, prev, curr, accu, "Indirects", indirects)
		  || getFirstCat(grid, prev, curr, accu, "Heavies", heavies)
		  || getFirstCat(grid, prev, curr, accu, "Chains", chainers)
		  || getFirstCat(grid, prev, curr, accu, "Nesters", nesters) )
			return true;
		return false;
	}

	/**
	 * Get the first hint from this list (a named Category) of Hinters.
	 *
	 * @param grid the Grid to search for hints
	 * @param prev previousHints List may not be null
	 * @param curr currentHints
	 * @param accu an implementation of IAccumulator
	 * @param catName String name of this category
	 * @param hinters a {@code List<IHinter>} of the hinters in this category
	 */
	private boolean getFirstCat(Grid grid, List<AHint> prev, List<AHint> curr
			, IAccumulator accu, String catName, List<IHinter> hinters) {
		assert prev!=null;
		boolean result = false;
		// get the prevTech, to see if it's already passed on all its hints
		// WTWTF: in debugger terniary evaluates BOTH paths -> AIOOBE
		final Tech prevTech;
		if ( prev.isEmpty() ) // nb: prev List may not be null
			prevTech = null;
		else
			prevTech = prev.get(prev.size()-1).getHinter().getTech();
		for ( IHinter hinter : hinters ) {
			// get the last previousHints: in case this hinter has not already
			//                             passed-back all its hints
			if ( hinter.getTech() == prevTech
			  && curr.size() < prev.size()
			  && curr.size() > 0
			) {
				// pass-back the next previousHints from this hinter, if any
				AHint h;
				while ( (h=prev.get(curr.size()-1)).getHinter() == hinter ) {
					result = true;	// accu is SingleHintsAccumulator.add->true
					if(accu.add(h))	// but we also cater for the
						break;		// (Default)HintsAccumulator.add->false
				}
			}
			// found nada in the cache, so search now.
			if ( !result )
				result = hinter.findHints(grid, accu);
			if ( result )
				break;
		}
		return result;
	}

	/**
	 * getAllHints: Get all the hints from this Grid; it's just that "all" is
	 * defined by wantMore (does the user want to see more hints?).
	 * <p>
	 * If a validator hints we're done regardless of what the user wants.
	 * If !wantMore (F5) get all hints from the first hinty hinter only.
	 * If wantMore (Shift-F5) keep going to get all hints from all hinters.
	 * <p>
	 * Called by SudokuExplainer.getAllHints, which is F5 in the GUI.
	 * <p>
	 * nb: If you just wanted the first-hint from the first successful hinter
	 * (ie getFirstHint) you'd use a SingleHintsAcculator in place of the
	 * default one.
	 *
	 * @param grid the Grid to solve
	 * @param wantMore are MORE (the Shift in Shift-F5) hints wanted regardless
	 * of how "hard" they are, or how long it takes to find them; which means a
	 * wait of about 15 seconds on my i7.
	 * @param isNoisy should I print hints as they're applied.
	 * @return List of AHint
	 */
	public List<AHint> getAllHints(Grid grid, boolean wantMore, boolean isNoisy) {
		final boolean isFilteringHints = Settings.THE.getBoolean(Settings.isFilteringHints, false);
		++AHint.hintNumber;
		if (Log.MODE>=Log.VERBOSE_2_MODE) {
			Log.println();
			final String more; if(wantMore) more=" MORE"; else more="";
			Log.teef(">getAllHints%s %d/%s%s", more, AHint.hintNumber, grid.source, NL);
			Log.println(grid);
		}
		final LinkedList<AHint> hints = new LinkedList<>();
		// this doesn't appear to ever be triggered! So I guess we no longer
		// get here when the grid is already full, but I'm leaving it.
		if ( grid.isFull() ) {
			hints.add(new SolvedHint());
			return hints; // empty if I was interrupted, or didn't find a hint
		}
		final long t0 = GrabBag.time = System.nanoTime();
		// HintsAccumulator.add returns false so hinter keeps searching,
		// adding each hint to the hints List.
		final IAccumulator accu = new HintsAccumulator(hints);
		grid.rebuildAllRegionsEmptyCellCounts();
		grid.rebuildAllRegionsIdxsOfAllValues();
		// get hints from hinters until one of them returns true (I found a
		// hint) OR if wantMore then run all hinters. nb: wantedHinters now
		// includes the nesters, which are only required to solve the
		// hardest Sudoku puzzles, the rest of the time they're not
		// executed, coz DyamicPlus finds a hint. But If you Shift-F5 (ie
		// wantMore=true) you get the lot, including nesters. It takes about
		// 15 seconds on my i7. Be a bit patient.
		boolean any = getFirst(validators, grid, accu, false);
		if ( !any ) {
			// tell locking to merge/upgrade any siamese hints
			// when the GUI wantsLess (F5/Enter) and isFilteringHints
			if ( Run.type==Run.Type.GUI && !wantMore && isFilteringHints )
				locking.setSiamese(hiddenPair, hiddenTriple);
			// run prepare AFTER validators and BEFORE wantedHinters
			if ( !grid.isPrepared() ) // when we get the first hint from this grid
				prepare(grid); // prepare the wanted IPreparer's to process da grid
			any = getAll(wantedHinters, grid, accu, !wantMore, isNoisy);
			// and don't forget to revert doSiamese or Generate goes mad
			locking.clearSiamese();
		}
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			final String more; if(wantMore) more=" MORE"; else more="";
			System.out.format("<getAllHints %b %,15d%s%s"
				, any, System.nanoTime()-t0, more, NL);
		}
		return hints; // empty if I was interrupted, or didn't find a hint
	}

	// getHints from the given hinters until we find one, or I'm interrupted so
	// I exit-early returning accu.hasAny(), ie false.
	// called by: solve, checkValidity, getAllHints, analyseDifficulty,
	// analyse, solveRecursively
	private boolean getFirst(IHinter[] hinters, Grid grid, IAccumulator accu, boolean isNoisy) {
		for ( IHinter hinter : hinters ) {
//if ( hinter.getTech() == Tech.SingleSolutions )
//	Debug.breakpoint();
			if ( interrupt()
			  || (hinter.isEnabled() && findHints(hinter, grid, accu, isNoisy)) )
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
	private boolean getAll(List<IHinter> hinters, Grid grid, IAccumulator accu
			, boolean less, boolean isNoisy) {
		for ( IHinter hinter : hinters )
			if ( interrupt()
			  || (hinter.isEnabled() && (findHints(hinter, grid, accu, isNoisy) && less)) )
				break;
		return accu.hasAny() && less;
	}

	// findHints calls the hinter.findHints; log it if isNoisy.
	private boolean findHints(IHinter hinter, Grid grid, IAccumulator accu, boolean isNoisy) {
		if (Log.MODE >= Log.VERBOSE_2_MODE) {
			if ( isNoisy ) {
				long t0 = System.nanoTime();
				Log.format("%-40s ", hinter.getTech().name());
				boolean result = hinter.findHints(grid, accu);
				Log.format("%,15d\t%s%s", System.nanoTime()-t0, hinter, NL);
				return result;
			}
		}
		return hinter.findHints(grid, accu); // just do it quietly
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
	 * getDifficulty() exceeds <tt>maxD</tt>. There may not be one.
	 * <p>
	 * NB: there's no consideration for minimum difficulty here, only maximum.
	 * The caller assesses the minimum (if any) for itself. I only monitor the
	 * maximum so that I can give-up early if/when it's exceeded.
	 * <p>
	 * NB: I respect the IInterruptMonitor passed into my constructor (if any).
	 *
	 * @param grid to examine
	 * @param maxDif the maximum desired difficulty (inclusive)
	 * @return The actual difficulty if it is between the given bounds, else
	 *  an arbitrary out-of-bounds value.
	 */
	public double analyseDifficulty(Grid grid, double maxDif) {
		double dif, puzDif=0.0D; // difficulty, puzzleDifficulty
		IAccumulator accu = new SingleHintsAccumulator();
		AHint.hintNumber = 1; // reset the hint number
		// we're now attempting to deal with deceased felines
		DEAD_CATS.clear();
		// re-enable all hinters just in case we hit a DEAD_CAT last time
		if ( anyDisabled )
			for ( IHinter hinter : wantedHinters )
				hinter.setIsEnabled(true);
		// except Generator skips Kraken Swordfish & Jellyfish which bugger-up
		// on null table entries, which I do not understand why they are null.
		enableKrakens(false, 3);
		final IHinter[] enabledHinters = getEnableds(wantedHinters);
		grid.rebuildAllRegionsEmptyCellCounts();
		grid.rebuildAllRegionsIdxsOfAllValues();
		for ( int pre=grid.countMaybes(),now; pre>0; pre=now ) { // ie !grid.isFull
			if ( getFirst(enabledHinters, grid, accu, false) ) {
				if ( interrupt() )
					return puzDif; // not sure what I should return
				AHint hint = accu.getHint();
				assert hint != null;
				// NB: AggregatedChainingHint.getDifficulty returns the maximum
				// difficulty of all the hints in the aggregate.
				dif = hint.getDifficulty();
				assert dif >= Difficulty.Easy.min; // there is no theoretical max
				if ( dif>puzDif && (puzDif=dif)>maxDif )
					return puzDif; // maximum desired difficulty exceeded
				try {
					hint.apply(Grid.NO_AUTOSOLVE, false);
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
		}
		enableKrakens(true, 3);
		return puzDif;
	}
	private boolean anyDisabled;

	/** Get the solution of this Grid. Used by generate and by the Tools ~
	 * Analyse menu-item in the SudokuFrame.
	 * @param grid puzzle to solve
	 * @return an AHint which is either an AnalysisHint or a WarningHint. */
	public AHint analyse(Grid grid) {
		GrabBag.grid = grid; // this is a new grid, not the one in the GUI.
		if (Log.MODE >= Log.NORMAL_MODE)
			Log.format("%sanalyse: %s%s%s%s", NL, grid.source, NL, grid, NL);
		SingleHintsAccumulator accu = new SingleHintsAccumulator();
		long t0 = System.nanoTime();
		if ( grid.isFull() )
			return new WarningHint(recursiveAnalyser
					, "The Sudoku has been solved", "SudokuSolved.html");
		// execute just the puzzle validators.
		if ( getFirst(puzzleValidators, grid, accu, false) )
			return accu.getHint();
		LogicalAnalyser logicalAnalyser = new LogicalAnalyser(this);
		if ( logicalAnalyser.findHints(grid, accu) ) {
			// logicalAnalyser.findHints allways returns true,
			// only the type of hint returned varies for success or failure;
			// and they're both WarningHints; so s__t gets sticky downstream.
			long t1 = System.nanoTime();
			AHint hint = accu.getHint();
			if (Log.MODE >= Log.NORMAL_MODE) {
				Log.format("%,15d\t%2d\t%4d\t%3d\t%-30s\t%s%s"
						, t1-t0, grid.countFilledCells(), grid.countMaybes()
						, hint.getNumElims(), hint.getHinter(), hint, NL);
				if ( hint instanceof AnalysisHint )
					Log.println(((AnalysisHint)hint)
							.appendUsageMap(new StringBuilder(1024)));
			}
			return hint;
		}
		return null; // should never happen, but never say never.
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
	public Grid solveQuicklyAndQuietly(Grid grid) {
		return recursiveAnalyser.solveQuicklyAndQuietly(grid);
	}

	/**
	 * Solve the given Grid recursively, ASAP, with brute force, not logic.
	 * <p>
	 * Having recursive s__t in the LOGICALSolver simplifies SudokuExplainer,
	 * but it smells like dead fish jammed sideways up Smelly Cats a-hole. If
	 * it annoys you too then fix it. Create a RecursiveSolver, which will
	 * duplicate god-knows-what fields and code, which'll be pushed up into an
	 * ASolver. Add RecursiveSolver into SudokuExplainer. I'm just too lazy.
	 *
	 * @param grid the puzzle to solve
	 * @return a SolutionHint as an AHint, else null */
	public AHint solveRecursively(Grid grid) {
		// double-check that recursiveAnalyser is in puzzleValidators
		assert MyArrays.contains(puzzleValidators, recursiveAnalyser);
		// NB: the recursiveAnalyser is in the puzzleValidators list, so
		// the below line calls RecursiveAnalyser.getHints which stores the
		// solution (coz SolutionMode.STORED) so that we can retrieve it.
		IAccumulator accu = new SingleHintsAccumulator();
		// tell the RecursiveAnalyser that I want the full SolutionHint.html
		// which says that the solution is displayed as green potential values;
		// otherwise it returns the plain "natural causes" SudokuSolved.html
		recursiveAnalyser.wantSolutionHint = true;
		// get all the validators, including the recursiveAnalyser. We expect
		// that no hint will be returned, if one is then the grid is invalid,
		// so we return the validators hint
		if ( getFirst(validators, grid, accu, false) )
			return accu.getHint();
		// it's a valid grid, so we return recursiveAnalyser.solutionHint
		AHint result = recursiveAnalyser.solutionHint;
		assert result != null;
		recursiveAnalyser.solutionHint = null;
		return result;
	}

	// Is this puzzle basically valid? NOT running singleSolution validator.
	private boolean validatePuzzle(Grid grid, IAccumulator accu) { // throws UnsolvableException
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) ) // NumberOfValues >= 8
		  // skip RecursiveAnalysis, the third puzzleValidator
			return carp("Invalid Sudoku: " + accu.getHint(), grid, true); // throws UnsolvableException
		return true; // the puzzle is (probably) valid.
	}

	/**
	 * Solve 'grid' logically to populate 'usageMap' and return success.
	 * In English: Solves the given Sudoku puzzle (the 'grid') using logic
	 * to populate the given 'usageMap' with a summary of which Hinters were
	 * invoked how often, and how long each took to run; returning "was the
	 * puzzle solved", which should be true for any/every valid puzzle, but
	 * never say never. An invalid puzzle should throw an UnsolvableException
	 * (a RuntimeException) so solve should never return false. Never say
	 * never.
	 *
	 * @param grid Grid the Sudoku puzzle to solve
	 * @param usage UsageMap to populate with hinter usage: hint count, elapsed
	 *  (execution) time, and the number of maybes eliminated
	 * @param validate true means run the TooFewClues and TooFewValues
	 *  validators before solving the puzzle.
	 * @param isNoisy true logs progress, false does it all quietly.
	 * @return was it solved; else see grid.invalidity
	 * @throws UnsolvableException which is a RuntimeException meaning that this
	 *  puzzle is invalid and/or cannot be solved. See the exceptions message
	 *  (if any) for details, and/or the grid's invalidity field.<br>
	 *  Clear as bloody mud, right? Well good. Don't send me invalid crap and I
	 *  won't make you struggle to access the error message. Sigh.
	 */
	public boolean solve(Grid grid, UsageMap usage, boolean validate
			, boolean isNoisy, boolean logHints) { // throws UnsolvableException
		assert grid!=null && usage!=null;
//if(grid.source.lineNumber==6)
//	Debug.breakpoint();
		grid.invalidity = null; // assume sucess
		if ( isNoisy )
			System.out.println(">solve "+Settings.now()+"\n"+grid+"\n");
		final IAccumulator accu = new SingleHintsAccumulator();
		// time between hints incl activate and apply
		// nb: first hint-time is blown-out by enabling, activation, etc.
		long start = GrabBag.time = System.nanoTime();
		// special case for being asked to solve a solved puzzle. Not often!
		if ( grid.isFull() ) {
			accu.add(new SolvedHint());
			return true; // allways, even if accu is NOT a Single
		}
		// when called by the GUI we don't need to validate any longer because
		// my caller (the LogicalAnalyser) already has, in order to handle the
		// hint therefrom properly; so now we only validate here when called by
		// the LogicalSolverTester, which is all a bit dodgy, but only a bit...
		// and all because it's hard to differentiate "invalid" messages. Sigh.
		if ( validate )
			validatePuzzle(grid, accu); // throws UnsolvableException
		if ( grid.source != null )
			prepare(grid); // prepare hinters to process this grid
		// get the hintNumber activated hinters
		IHinter[] enableds = getEnableds(wantedHinters);
//		INumberedHinter[] numbereds = getNumberedHinters(enableds);
		// count the isActuallyHintNumberActivated hinters
//		final boolean anyNumbered = countActualHNAHs(numbereds) > 0;
		// if 0 actualHNAH's then just activate them all ONCE, coz it's faster.
//		if(!anyNumbered) activate(numbereds, true);
		// and they're off and hinting ...
		int hintNum = AHint.hintNumber = 1; // BEFORE the first hint
		AHint problem, hint;
		long now;
		while ( !grid.isFull() ) {
			// must rebuild BEFORE we validate (RecursiveAnalyser uses). Sigh.
			grid.rebuildAllRegionsEmptyCellCounts();
			grid.rebuildAllRegionsIdxsOfAllValues();
			// detect invalid grid, meaning a hinter is probably borken!
			if ( (problem=validatePuzzleAndGrid(grid, false)) != null )
				throw new UnsolvableException("Houston: "+problem);
			// activate the hintNumber activated hinters
//			if(anyNumbered) activate(numbereds, hintNum);
			// getHints from each enabled hinter
			if ( !timeHinters(enableds, grid, accu, usage, hintNum) )
				return carp("Hint not found", grid, true); // throws UnsolvableException
			// apply the hint
			hint = accu.getHint();
			now = System.nanoTime();
			// apply may throw UnsolvableException from Cell.set
			apply(hint, hintNum, now-start, grid, usage, isNoisy, logHints);
			start = now;
			AHint.hintNumber = ++hintNum;
		} // wend
		if ( isNoisy )
			System.out.println("<solve "+Settings.took()+"\n"+grid+"\n");
		AHint.hintNumber = 1; // reset the hint number
		return true;
	}

	// Called by solve to time the execution of the getHints methods of
	// these enabledHinters.
	private boolean timeHinters(IHinter[] enableds, Grid grid
			, IAccumulator accu, UsageMap usageMap, int hintNum) {
		long start;  boolean any;
		for ( IHinter hinter : enableds )
			if ( hinter.isActive() ) {
				start = System.nanoTime();
//if ( hintNum==40 ) //&& hinter.getTech()==Tech.AlignedSept )
//	Debug.breakpoint();
//if ( hinter.getTech() == Tech.Locking )
//	Debug.breakpoint();
				any = hinter.findHints(grid, accu);
				// NB: numHints & NumElims are set after hint is applied, so
				// that we know what it DID, not just what we expect it to do.
				usageMap.addon(hinter, 1, 0, 0, System.nanoTime()-start);
				if ( any )
					return true;
			}
		return false;
	}

	// Apply this hint to the grid.
	// @param hint AHint to apply
	// @param hintCount int is the 1 based count of hints in this puzzle
	// @param took long nanoseconds between now and previous hint
	// @param grid Grid to apply the hint to
	// @param usage UsageMap for the updateUsage method (below)
	// @param noisy if true log progress, else do it quietly
	private int apply(AHint hint, int hintCount, long took, Grid grid
			, UsageMap usage, boolean isNoisy, boolean logHints) { // throws UnsolvableException
		if ( hint == null )
			return 0;
		if ( hint instanceof AWarningHint ) // NoDoubleValues or NoMissingMaybes
			throw new UnsolvableException("Warning: " + hint);
		assert hint.getDifficulty() >= 1.0; // 0.0 has occurred. Bug!
		// + the grid (2 lines per hint)
		final boolean logIt = isNoisy || logHints;
		if ( Log.MODE>=Log.VERBOSE_3_MODE && logIt )
			Log.println(grid);
		int numElims = -1;
		try {
			// apply throws UnsolvableException on the odd occassion
			// AUTOSOLVE true sets subsequent naked and hidden singles
			// isNoisy logs crap when true (for debugging only, really)
			numElims = hint.apply(Grid.AUTOSOLVE, isNoisy);
		} catch (UnsolvableException ex) { // probably from Cell.set
			// stop the UnsolvableException obscuring the causal hint.
			Log.teeln("LogicalSolver.apply: "+hint.toFullString());
			Log.teeln("caused: "+ex);
			throw ex;
		}
		// + 1 line per Hint (detail)
		if ( Log.MODE>=Log.VERBOSE_2_MODE && logIt )
			printHintDetailsLine(Log.out, hintCount, took, grid, numElims, hint, true);
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
			Log.teeln("WARN: DEAD_CAT disabled "+hint.hinter.tech.name()+" "+hint.toFullString());
			// disable the offender
			hint.hinter.setIsEnabled(false);
		}
		return numElims;
	}

	private static void printHintDetailsLine(PrintStream out, int hintCount
			, long took, Grid grid, int numElims, AHint hint, boolean wantBlankLine) {
		out.format("%-5d", hintCount); // left justified to differentiate from puzzleNumber in the logFile.
		out.format("\t%,15d", took); // time between hints includes activate time and rebuilding empty cell counts
		out.format("\t%2d", grid.countFilledCells());
		out.format("\t%4d", grid.countMaybes());
		out.format("\t%3d", numElims);
		out.format("\t%-30s", hint.hinter);
		// squeeze hobiwans multiline Kraken format back onto one line
		if ( hint.hinter.tech.name().startsWith("Kraken") )
			out.format("\t%s", MyStrings.squeeze(hint.toFullString()));
		else
			out.format("\t%s", hint.toFullString());
		out.println();
		if ( wantBlankLine )
			out.println();
	}

	// Updates the UsageMap with the hint details: adds numHints and numElims,
	// max's maxDifficulty, and addonates the subHints.
	// Called by: solve->apply->updateUsage
	// NB: usage was added by timeHinters, we just update the details.
	private void updateUsage(UsageMap usageMap, AHint hint, int numElims) {
		Usage u = usageMap.get(hint.hinter);
		if ( u == null ) {
			// Locking upgrades siamese hints to HiddenPair/Triple, so
			// usage may not yet exist, so now we must add it.
			// nb: the time has already been allocated to Locking even
			// though it's actually (now) a hidden-set hint. Sigh.
			usageMap.addon(hint.hinter, 1, 1, numElims, 0L);
			u = usageMap.get(hint.hinter);
			u.maxDifficulty = hint.getDifficulty();
			return;
		}
		u.numHints += (hint instanceof AggregatedHint)
				? ((AggregatedHint)hint).hints.size()
				: 1;
		u.numElims += numElims;
		u.maxDifficulty = Math.max(hint.getDifficulty(), u.maxDifficulty);
		// addonate to subHints Map: for hinters producing multiple hint types.
		u.addonateSubHints(hint, 1);
	}

	// get the INumberedHinter[]'s from the enabledHinters[].
	private INumberedHinter[] getNumberedHinters(IHinter[] enabledHinters) {
		List<INumberedHinter> list = new LinkedList<>();
		for ( IHinter h : enabledHinters )
			if ( h instanceof INumberedHinter )
				list.add((INumberedHinter)h);
		return list.toArray(new INumberedHinter[list.size()]);
	}

	// Counts the isActuallyHintNumberActivated()-hinters, ignoring the
	// firstHintNumber-activated ones. If there are none we give-up hintNumber
	// activation, coz it takes about as long to do as it saves.
	private int countActualHNAHs(INumberedHinter[] activatables) {
		int count = 0;
		for ( INumberedHinter nh : activatables )
			if ( nh.isActuallyHintNumberActivated() )
				++count;
		return count;
	}

	public void prepare(Grid grid) {
		if ( grid == null )
			return; // safety first!
		if ( HintValidator.ANY_USES ) { // is anyone using the HintValidator?
			if ( grid.solutionValues == null ) // expect null
				solveQuicklyAndQuietly(grid); // sets grid.solutionValues
			HintValidator.setSolutionValues(grid.solutionValues, grid.puzzleID);
		}
		for ( IPreparer prepper : getPreppers(wantedHinters) )
			prepper.prepare(grid, this);
		grid.setPrepared(true);
	}

	/**
	 * USING CACHE: select {@code LinkedList<IPreparer>} from hinters where
	 * implements IPreparer.
	 * <p>
	 * The preppersCache is updated when it's out-dated. That is:<br>
	 * when preppersCache is null (ie doesn't exist yet);<br>
	 * or Settings.THE.getModificationCount() differs from when preppersCache
	 * was read previously; in case the wantedHinters have changed; otherwise
	 * we prepare no-longer-wanted-hinters, causing head-aches, and
	 * <b><i>excessive masturbation with eel spleen oil, which may send you blind.
	 * And "no" I don't want to taste your eel spleen oil. I don't care if it's
	 * very nice. Nor do I want to pay for power, which I can get free from the
	 * sun; or buy your oil, cosmetics, plastic bags, packaging, brand, wolf
	 * nipple chips, pop-up toaster, pop-up restaurant, your stupid 1000 hour
	 * television, or your mind-bogellingly dumb refried billy nut cheese.
	 * In fact I won't pay for you. You're worthless. Now go away, or I shall
	 * taunt you a second time! That does it: The next gannet who comes within
	 * fifty yards of me is gonna die!<br>
	 * <br>
	 * But that's a tautology Boris.<br>
	 * <br>
	 * I'll rip ya' bloody arms off.<br>
	 * <br>
	 * Yes, I know. Make it twenty-five.
	 * </i></b>
	 *
	 * @param hinters the wantedHinters list
	 * @return A cached {@code LinkedList<IPreparer>}
	 */
	private LinkedList<IPreparer> getPreppers(List<IHinter> hinters) {
		final int modCount = Settings.THE.getModificationCount();
		LinkedList<IPreparer> preppers = preppersCache;
		if ( preppers==null || prepyModCount!=modCount ) {
			preppers = new LinkedList<>();
			for ( IHinter hinter : hinters )
				if ( hinter instanceof IPreparer )
					preppers.add((IPreparer)hinter);
			preppersCache = preppers;
			prepyModCount = modCount;
		}
		return preppers;
	}
	private LinkedList<IPreparer> preppersCache; // defaults to null automagically
	private int prepyModCount; // defaults to 0 automagically

	// an enabled hinter is one which finds a hint (any hint) in this puzzle.
	private IHinter[] getEnableds(List<IHinter> wantedHinters) {
		List<IHinter> list = new LinkedList<>();
		for ( IHinter hinter : wantedHinters )
			if ( hinter.isEnabled() )
				list.add(hinter);
		return list.toArray(new IHinter[list.size()]);
	}

	// Each INumberedHinter is de/activated for each hint.
	// If it's not active then it's not executed by the timeHinters method.
	private void activate(INumberedHinter[] numberedHinters, int hintNum) {
		for ( INumberedHinter nh : numberedHinters )
			nh.activate(hintNum);
	}

	// When there are no actual-hintNumber-activated-hinters it's faster to
	// activate all the hinters once (ie not de/activate them for each hint).
	private void activate(INumberedHinter[] numbereds, boolean isActive) {
		for ( INumberedHinter hinter : numbereds )
			hinter.setIsActive(isActive);
	}

	// REQUIRED: Used by jUnit test-cases only, so that they don't have to
	// deal with the complexities of maintaining hinters-lists.
	public void activate_4JUnit(final boolean active) {
		IHinter[] wh = wantedHinters.toArray(new IHinter[wantedHinters.size()]);
		INumberedHinter[] nhs = getNumberedHinters(wh);
		for ( INumberedHinter nh : nhs )
			nh.setIsActive(active);
	}

//RETAIN for debugging
//	private List<IHinter> actives(IHinter[] enableds) {
//		List<IHinter> result = new LinkedList<>();
//		for ( IHinter h : enableds )
//			if ( h.isActive() )
//				result.add(h);
//		return result;
//	}

	/** Get the solution to this puzzle as soon as possible (ie guessing
	 * recursively, ie using brute force, ie using a BIG stick).
	 * <p>NB: the solution is generated once and cached in the grid.
	 * @param grid the Grid to solve.
	 * @return a {@code List<AHint>} containing a SolutionHint. */
	public List<AHint> getSolution(Grid grid) {
		if ( grid.solutionHints != null )
			return grid.solutionHints;
		IHinter analyser = new RecursiveAnalyser(); // SolutionMode.WANTED
		IAccumulator accu = new SingleHintsAccumulator();
		// WTWTF: grid.solutionHints is an object with no boolean value, yeah?
		return grid.solutionHints = analyser.findHints(grid, accu)
			? AHint.list(accu.getHint())
			// WTF? No solution is available. Your options are:
			// 1. check expected hinters are in wantedHinters; or
			// 2. you've broken the hinter you were working on; or
			// 3. you've broken the solve method itself; or
			// 4. the Grid really is unsolvable; in which case it
			//    should not have passed upstream validation; or
			// 5. you're just plain ole ____ed!
			: null;
	}

	/** Can this grid be solved just by filling in naked and hidden singles?
	 * These are the easiest two Sudoku solving techniques, which I find boring,
	 * so I want to just skip "the rest" once the interesting bits are done, so
	 * I'm making the GUI flash green when nothing but singles remain.
	 * @param grid puzzle to solve, or not.
	 * @param accu the IAccumulator to which I add an AppliedHintsSummaryHint.
	 * If null then no hint is created and I just return the result.
	 * @return was the grid solved? returns false on Exception that's logged. */
	public boolean solveWithSingles(Grid grid, IAccumulator accu) {
		boolean result = false; // presume failure
		try {
			// We work on a copy of the given grid
			Grid myGrid = new Grid(grid); // a copy
			// HintsApplicumulator.add apply's each hint when it's added.
			// @param isStringy = true so builds toString buffer for the hint.
			// @param isAutosolving = true so that Cell.set also sets subsequent
			// singles, because Cell.set can do it faster than THE_SINGLES coz
			// it has a smaller search (20 siblings vs 81 Grid.cells).
			HintsApplicumulator apcu = new HintsApplicumulator(true, true);
//			myGrid.rebuildMaybesAndEverything();
			int ttlNumElims = 0;
			do {
				apcu.numElims = 0;
				if ( nakedSingle.findHints(myGrid, apcu)
				   // nb: use bitwise-or (|) so that both hinters are
				   // invoked regardless of nakedSingle's return value
				   | hiddenSingle.findHints(myGrid, apcu) )
					ttlNumElims += apcu.numElims;
			} while ( apcu.numElims > 0 );
			singlesSolution = myGrid; // any hints found have been applied to myGrid
			if ( (result=ttlNumElims>0 && myGrid.isFull()) && accu!=null )
				accu.add(new AppliedHintsSummaryHint(ttlNumElims, apcu));
		} catch (Exception ex) { // especially UnsolvableException
// but only when we're focusing on solveWithSingles, which we normaly aren't.
//			Log.teeTrace(ex);
//			Log.teeln(grid);
		}
		return result;
	}

	private boolean carp(String message, Grid grid, boolean throwIt) {
		return carp(new UnsolvableException(message), grid, throwIt);
	}

	private boolean carp(UnsolvableException ex, Grid grid, boolean throwIt) {
		grid.invalidity = ex.getMessage();
		if (throwIt) {
			if (Log.MODE >= Log.NORMAL_MODE)
				System.out.print(NL+grid+NL);
			throw ex;
		}
		Log.println();
		ex.printStackTrace(Log.out);
		Log.print(NL+grid+NL);
		Log.flush();
		return false;
	}

	// ============================= INNER CLASSES =============================

	/**
	 * A Trixie hint fast or die! We sort the wantedHinters by nanoseconds per
	 * elimination of the previous execution of the LogicalSolverTester, so
	 * that the fastest, most effective hinters run earlier, and poor value
	 * ones drop-down the list until they come after DynamicPlus which always
	 * hints (in top1465), and so effectively drop-off the bottom of the list,
	 * leaving nothing but pure unadulterated gravy. It's so Chumpy you could
	 * carve it with a Scotsman, or two! Maybe throw in a rude Frog and some
	 * pickled cabbage. Run it repeatedly and see what happens. It'll get faster
	 * and faster, and then slower when hints that were found quickly now take
	 * longer to find because they're being left for to a slower hinter that's
	 * faster per elimination: go figure!
	 * <p>
	 * By the way: this Comparator only used in LogicalSolverTester -STATS.
	 * Basically I just wrote it because I can. That's what Sudoku Explainer is
	 * about: something to do, other than swear at the television. Jeez, it's
	 * all mightily ____ing stupid these days, or maybe I'm just getting old.
	 */
	private static class ByNsPerElimAsc implements Comparator<IHinter> {
		public final Map<String, Long> stats = new HashMap<>();
		public boolean isFullyLoaded = false;
		public ByNsPerElimAsc() {
			// Read the performance statistics of the previous run from file,
			// NB: File won't exist the first time you turn on STATS. That's OK.
			// NB: there are no header lines in the actual file
			// NB: the time/elim will be 0 when elims is 0
			//  time (ns)  calls  time/call   elims  time/elim  hinter
			//          0      1          2       3          4  5
			// 19,659,393  44948        437  210870         93  Naked Single
			// 42,842,032  42792      1,001  372730        114  Hidden Single
			try ( BufferedReader reader = new BufferedReader(
					new FileReader(IO.PERFORMANCE_STATS)) ) {
				String line;
				while ( (line=reader.readLine()) != null ) {
					if ( line.startsWith("//") ) // skip comments
						continue;
					String[] fields = line.split(" *\t *");
					String techNom = fields[5];
					long nsPerElim = MyLong.parse(fields[4]);
					stats.put(techNom, nsPerElim);
				}
				isFullyLoaded = stats.size() > 0;
			} catch (Exception ex) {
				StdErr.whinge("ByNsPerElimAsc: load failed", ex);
			}
		}
		@Override
		public int compare(IHinter a, IHinter b) {
			// you need to check isFullyLoaded before you sort!
			assert isFullyLoaded : "ByNsPerElimAsc !isFullyLoaded";
			if ( !isFullyLoaded )
				return 0; // turns the sort into an expensive no-op
			if ( a == b )
				return 0;
			Long bb = stats.get(b.getTech().nom); // ns per elimination
			if ( bb==null || bb==0L )
				return -1; // puts b last, even if aa is also null||0
			Long aa = stats.get(a.getTech().nom); // ns per elimination
			if ( aa==null || aa==0L )
				return 1; // puts a last
			// do the actual comparison (let it auto-unbox the Integers)
			if ( aa < bb )
				return -1;	// ASCENDING
			if ( aa > bb )
				return 1;	// ASCENDING
			return 0;
		}
	}

}
