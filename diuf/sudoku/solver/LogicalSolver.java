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
import diuf.sudoku.solver.hinters.als.SueDeCoq;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.*;
import diuf.sudoku.gen.*;
import diuf.sudoku.gui.Print;
import diuf.sudoku.io.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.solver.hinters.align.*;
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
import java.io.Closeable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
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
 * <li>untick Aligned*Exclusion (slow); A7+E are far too slow and need shootin
 * <li>for <u>really</u> hard puzzles you'll need Nested Unary, but the rest
 *  of Nested* is never used in anger. Nested Plus is a slow catch-all but is
 *  <u>never</u> executed in anger (exists ONLY for Shft-F5).
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
 *  <li>Solve a Sudoku puzzle logically to calculate its difficulty rating,
 *  and get a summary list of the hint-types (usages) that are required.
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
 * <li>The original Sudoku Explainer was heavily dependent on Chainers, so I
 *  boosted HoDoKu's Solving Techniques. In the GUI each such technique is
 *  denoted with "HoDoKu $TechniqueName". Kudos to hobiwan. Mistakes are mine.
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
 *  </ul>
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
 *  In Nutbeans: build HoDoKu.jar, then run all JUnit tests with ALT-F6. They
 *  take about a minute. These are mostly just regression tests, which just
 *  diff the output with a previous one, so anytime you change the display of
 *  a hint you also need to update the test-case/s. Some hint-types do NOT
 *  yet have test-cases, because I'm a slack/lazy/messy programmer. It's a
 *  work in progress, and I'm doing this just for fun, so it's not perfect.
 * <li>And then I use {@link diuf.sudoku.test.LogicalSolverTester} with
 *  {@code Log.MODE = VERBOSE_4_MODE} to performance test the hinters on
 *  {@code top1465.d5.mt}, which is in the root directory of this project.
 *  This currently takes under 4 minutes to run (-A5..10E).
 * </ul>
 * <p>
 * @see ./LogicalSolverTimings for run timings.
 */
public final class LogicalSolver {

	/**
	 * true uses new "align2", which is slower than original "align" package.
	 */
	private static final boolean USE_ALIGN2 = false; //@check false

	/**
	 * true uses original BasicFisherman, which is faster than BasicFisherman1.
	 */
	private static final boolean USE_OLD_FISH = true; // @check true

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
	public final RecursiveAnalyser recursiveAnalyser = new RecursiveAnalyser();
	// SolutionMode.STORED

	/** These Hinters workout if the grid is still valid. */
	private IHinter[] gridValidators;

	/** This is a list of both the puzzleValidators and gridValidators, which
	 * is what we want to run most of the time. */
	private IHinter[] validators;

	/** These Hinters set cell values directly. */
	private List<IHinter> directs;
	private final NakedSingle nakedSingle = new NakedSingle();
	private final HiddenSingle hiddenSingle = new HiddenSingle();

	/** These Hinters remove maybes, setting cell values only indirectly. */
	private List<IHinter> indirects;
	/** we locking.setSiamese in the GUI only. */
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

	/** If a Tech is wanted by the user (in the Options ~ Solving Techniques
	 * Dialog) then it'll be used to solve puzzles, else it won't.
	 * <p>see: {@code HKEY_CURRENT_USER\Software\JavaSoft\Prefs\diuf\sudoku} */
	private EnumSet<Tech> unwanted = EnumSet.noneOf(Tech.class);

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

	/** My interruptMonitor (the parent Generator) to pass to hinters so that
	 * they can interrupt themselves when the user interrupts the Generator;
	 * so always null except when the LogicalSolver is created by a Generator.
	 */
	IInterruptMonitor interruptMonitor = null;
	public void setInterruptMonitor(IInterruptMonitor im) {
		this.interruptMonitor = im;
	}

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
	 * Closeable says this method throws IOException, so I follow along, but
	 * this implementation eats all Throwables, ie it never throws anything,
	 * ever, which is a MUCH better approach for closing I/O.
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
					// do nothing
				}
			}
		}
	}

	/** return has the monitor (parent Generator) been interrupted by user.
	 * isInterrupted would be a better name, but it's too long. */
	private boolean interrupt() {
		final IInterruptMonitor m = this.interruptMonitor;
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
		// hinters are created is the order in which they execute. My order is
		// about equal to that of the Tech class, but there are variances.
		// The hardest puzzles require NestedUnary, but never beyond.

		// when isAccurate we run more hinters, to get the simplest hints.
		final boolean isAccurate = (mode == Mode.ACCURACY);
		// when isAggregate chainers lump all there hints into one.
		final boolean a = (mode == Mode.SPEED);

		// T/F is shorthand for true/false; to make the code fit on a line.
		final boolean T = true, F = false;
		// im is shorthand for interruptMonitor
		final IInterruptMonitor im = interruptMonitor;

		// refetch the wantedTechs field, in case they've changed; then my want
		// method reads this field, rather than re-fetching it every time.
		this.wantedTechs = Settings.THE.getWantedTechniques();

		// empty the unwanted-techs set, for the want method to populate.
		this.unwanted.clear();

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
		// Naked Single and Hidden Single hardcoded because not having them is
		// crazy. DynamicPlus and NestedPlus are catch-alls. The user unwants
		// other hinter types in the GUI, which changes the registry Settings,
		// which the want method reads via the wantedTechs field, refreshed at
		// the start of this method.

		// Regarding execution order:
		// The solve/etc methods just loop-through the wantedHinters list,
		// invoking each in the order that it's added by this method. The order
		// they appear in the log file (num calls) may not match the run-order,
		// especially if multiple successive hinters have not yet hinted.

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
		else
			unwanted.add(Tech.LockingGeneralised);
		want(indirects, new NakedSet(Tech.NakedPair));
		// nb: When FilterHints, Locking uses HiddenPair to upgrade siamese
		// locking hints to a HiddenSet hint with additional eliminations.
		// Always set the variables even in SPEED mode (for setSiamese).
		want(indirects, hiddenPair = new HiddenSet(Tech.HiddenPair));
		want(indirects, new NakedSet(Tech.NakedTriple));
		// nb: hiddenTriple used (cheekily) by Locking to upgrade siamese
		// locking hints to the full HiddenSet hint, with additional elims.
		want(indirects, hiddenTriple = new HiddenSet(Tech.HiddenTriple));
		if ( isAccurate ) {
			if ( USE_OLD_FISH )
				want(indirects, new BasicFisherman(Tech.Swampfish));
			else
				want(indirects, new BasicFisherman1(Tech.Swampfish));
			want(indirects, new TwoStringKite());
			want(indirects, new XYWing(Tech.XY_Wing));
			want(indirects, new XYWing(Tech.XYZ_Wing));
			want(indirects, new WWing());
			want(indirects, new Skyscraper());
			want(indirects, new EmptyRectangle());
			if ( USE_OLD_FISH )
				want(indirects, new BasicFisherman(Tech.Swordfish));
			else
				want(indirects, new BasicFisherman1(Tech.Swordfish));
		}

		// heavies are slower indirect hinters. The heavy-weigth division.
		heavies = new ArrayList<>(isAccurate ? 42 : 0); //Ready please Mr Adams
		if ( isAccurate ) {
			// Choosing Coloring: BUG, Coloring, XColoring, Mesuda, and/or GEM.
			// * DROP BUG: It's old and slow. Finds minimal hints.
			// * KEEP Coloring: A superset of BUG. The only multi-coloring:
			//   searches more than two coloring-sets.
			// * KEEP XColoring: A superset of Colorings simple-coloring:
			//   searches two coloring-sets. Hints where both Medusa and GEM
			//   miss, which is fine so long as we use XColoring.
			// * DROP Medusa3D: It's counter-productive when used with GEM.
			//   Medusa and GEM both paint cell-values, not just cells.
			// * KEEP GEM: the "ultimate" coloring is a superset of Medusa3D.
			//   Note that my impl doesn't completely impl the specification!
			// * They're all so fast it makes ____-all difference.
			want(heavies, new BUG());       // slow basic  DROP
			want(heavies, new Coloring());  // BUG++       KEEP multicolor
			want(heavies, new XColoring()); // Coloring++  KEEP GEM misses some
			want(heavies, new Medusa3D());  // XColoring++ DROP
			want(heavies, new GEM());       // Medusa3D++  KEEP "ultimate"
			want(heavies, new NakedSet(Tech.NakedQuad));
			want(heavies, new HiddenSet(Tech.HiddenQuad)); // NONE in top1465
			if ( USE_OLD_FISH )
				want(heavies, new BasicFisherman(Tech.Jellyfish));
			else
				want(heavies, new BasicFisherman1(Tech.Jellyfish));
			want(heavies, new NakedSet(Tech.NakedPent));	   // DEGENERATE
			want(heavies, new HiddenSet(Tech.HiddenPent)); // DEGENERATE
			want(heavies, new BigWing(Tech.WXYZ_Wing));
			want(heavies, new BigWing(Tech.VWXYZ_Wing));
			want(heavies, new BigWing(Tech.UVWXYZ_Wing));
			want(heavies, new BigWing(Tech.TUVWXYZ_Wing));
			want(heavies, new BigWing(Tech.STUVWXYZ_Wing));
			want(heavies, new UniqueRectangle());
			// ComplexFisherman now detects Sashimi's in a Finned search.
			want(heavies, new ComplexFisherman(Tech.FinnedSwampfish));
			want(heavies, new ComplexFisherman(Tech.FinnedSwordfish));
			want(heavies, new ComplexFisherman(Tech.FinnedJellyfish));
			want(heavies, new AlsXz()); // ALS + bivalue cell
			want(heavies, new AlsXyWing()); // 2 ALSs + bivalue cell
			want(heavies, new AlsXyChain()); // 3+ ALSs + bivalue cell
			want(heavies, new DeathBlossom()); // 2 ALSs + bivalue; 3 ALSs + trivalue; or even 4 ALSs + quadvalue
			want(heavies, new SueDeCoq()); // AALS's
			want(heavies, new ComplexFisherman(Tech.FrankenSwampfish));	// NONE
			want(heavies, new ComplexFisherman(Tech.FrankenSwordfish));	// OK
			want(heavies, new ComplexFisherman(Tech.FrankenJellyfish));	// OK
			// Krakens & Mutants interleaved: Swamp, Sword, then Jelly
			want(heavies, new KrakenFisherman(Tech.KrakenSwampfish));	// OK
			want(heavies, new ComplexFisherman(Tech.MutantSwampfish));	// NONE
			want(heavies, new KrakenFisherman(Tech.KrakenSwordfish));	// SLOW
			want(heavies, new ComplexFisherman(Tech.MutantSwordfish));	// SLOW
			want(heavies, new KrakenFisherman(Tech.KrakenJellyfish));	// SLOW
			want(heavies, new ComplexFisherman(Tech.MutantJellyfish));	// TOO SLOW

			// align2 (new) is faster than align (old) for A234E;
			if ( false ) { // always use align2 (old retained for prosterity)
				want(heavies, new Aligned2Exclusion()); // fast enough.
				want(heavies, new Aligned3Exclusion()); // fast enough.
				want(heavies, new Aligned4Exclusion(im)); // acceptable.
			} else {
				want(heavies, new AlignedExclusion(Tech.AlignedPair, T));
				want(heavies, new AlignedExclusion(Tech.AlignedTriple, T));
				want(heavies, new AlignedExclusion(Tech.AlignedQuad, F));
			}
			// align2 takes 3*A5E, 4*A6E, 5*A7E; presume so on for 8, 9, 10.
			// The user chooses if A5+E is correct (slow) or hacked (fast).
			// Hacked finds about a third of hints in about a tenth of time.
			if ( !USE_ALIGN2 ) {
				// wantAE reads the "isa${degree}ehacked" Setting to choose
				// which Aligned*Exclusion (_1C or _2H) class to construct.
				wantAE(heavies,  5, im, T); // isabeet slow! User choice.
				wantAE(heavies,  6, im, T); // reeally slow! User choice.
				wantAE(heavies,  7, im, T); // ____ing slow! User choice.
				wantAE(heavies,  8, im, T); // 21yr old dog! User choice.
				wantAE(heavies,  9, im, T); // old tortoise! User choice.
				wantAE(heavies, 10, im, T); // conservative! User choice.
			} else {
				// AE constructor reads the "isa${degree}ehacked" Setting.
				want(heavies, new AlignedExclusion(Tech.AlignedPent, T));
				want(heavies, new AlignedExclusion(Tech.AlignedHex, T));
				want(heavies, new AlignedExclusion(Tech.AlignedSept, T));
				want(heavies, new AlignedExclusion(Tech.AlignedOct, T));
				want(heavies, new AlignedExclusion(Tech.AlignedNona, T));
				want(heavies, new AlignedExclusion(Tech.AlignedDec, T));
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
		// A safety-net for all the but the hardest puzzles
		want(chainers, new MultipleChainer(Tech.DynamicPlus, a));

		// Nested means imbedded chainers: assumptions on assumptions.
		// The only way to ever see a nested hint is with Shift-F5 in the GUI.
		nesters = new ArrayList<>(4);
		// advanced					 // approx time per call on an i7 @ 2.9GHz
		// NestedUnary covers THE Hardest Sudoku according to conceptis.com.
		// The actual safety-net, always hints!
		want(nesters, new MultipleChainer(Tech.NestedUnary, a));	//  4 secs
		want(nesters, new MultipleChainer(Tech.NestedMultiple, a));	// 10 secs
		// experimental
		want(nesters, new MultipleChainer(Tech.NestedDynamic, a));	// 15 secs
		// NestedPlus is Dynamic + Dynamic + Four Quick Foxes => confusing!
		// @bug 2020 AUG: NestedPlus produced invalid hints, so now uses the	// PRODUCED INVALID HINTS!
		// HintValidator to log and ignore them. NOT adequate! Needs fixing!
		// I'm just ignoring this bug for now coz IT'S NEVER USED in anger.
		want(nesters, new MultipleChainer(Tech.NestedPlus, a));		// 70 secs

		if ( Log.MODE >= Log.NORMAL_MODE )
			if ( !unwanted.isEmpty() )
				Log.println("unwantedTechs: "+unwanted.toString());

		// repopulate the wantedHinters list
		populateWantedHinters();
	}

	/** Populate the wantedHinters. Method suppresses unchecked warnings. */
	@SuppressWarnings("unchecked")
	private void populateWantedHinters() {
		wantedHinters = new ArrayList<>(Settings.THE.getNumWantedTechniques());
		//NB: unwanted hinters are already filtered-out of the source lists.
		wantedHinters.addAll(directs);
		wantedHinters.addAll(indirects);
		wantedHinters.addAll(heavies);
		// chainers allways includes DynamicPlus which allways finds a hint.
		wantedHinters.addAll(chainers);
		//NB: I've added the nesters, even though I don't want to coz I dont
		// want to wait for them in the event that DynamicPlus fails to hint.
		// I still regard that as an "A grade" bug, but I've added them coz
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
	 * just add the tech to the unwanted list, for printing after all the want
	 * methods have been called.
	 *
	 * @param c {@code Collection<IHinter>} the hinters-list to add this hinter
	 *  to, if it's wanted.
	 * @param hinter to want, or not.
	 */
	private void want(Collection<IHinter> c, IHinter hinter) {
		final Tech tech = hinter.getTech();
		if ( wantedTechs.contains(tech) )
			c.add(hinter);
		else
			unwanted.add(tech);
	}

	/**
	 * The wantAE (addAlignedExclusionIfWanted) method creates either:<br>
	 * the _1C (Correct = align each cell set on a single cell);<br>
	 * or _2H (Hacked = align each cell set around at least two cells)<br>
	 * version of an Aligned*Exclusion class, depending on registry Settings.
	 * <p>
	 * See the Aligned*Exclusion classes for more on the difference between the
	 * Correct version and it's much faster but less-hinty Hacked equivalent.
	 * <p>
	 * It exists to handle the user-selectable A*E's:<ul>
	 * <li>A5E, A6E, A7E, A8E are user selectable (correct mode or hacked):<ul>
	 *  <li>_2H for Hacked (fast) 2-excluder-cells finds about a third of hints
	 *   in about a tenth of the time (ie cheating hack bastard mode); OR
	 *  <li>_1C for Correct (slow) 1-excluder-cell mode for more hints is
	 *   4*4*4 + 5*5*5*5 + 6*6*6*6*6 + 7*7*7*7*7*7 + 8*8*8*8*8*8*8
	 *   + 9*9*9*9*9*9*9*9 + 9*9*9*9*9*9*9*9*9 = 432,690,476 more head-aches.
	 *  </ul>
	 * <li>A2E, A3E, and A4E are correct only. There is no _1C or _2H.
	 * </ul>
	 * NB: looking-up the class name and instantiating it through reflection is
	 * slower than instantiating it directly, but requires a big switch, which
	 * I don't like. This is fast enough as long as the constructor/s and
	 * configureHinters stay off the critical path.<br>
	 * The downside of soft-linking is most design tools miss these usages.
	 * SEE: class comment block in AAlignedSetExclusionBase for more.
	 *
	 * @param list {@code List<IHinter>} the hinters list to add the new
	 *  instance to
	 * @param num the number of cells in the aligned sets to be processed
	 * @param im interruptMonitor to pass through to the A*E
	 * @param defaultIsHacked is used the first time SE runs, to provide a
	 *  default for "should this AlignedExclusion be hacked", meaning aligned
	 *  around two excluder cells; which for A5+E means searching about a tenth
	 *  of the aligned sets to find about a third of the hints; so it's MUCH
	 *  faster, which really matters because A5+E are all really slow.
	 */
	private void wantAE(List<IHinter> list, int num, IInterruptMonitor im
			, boolean defaultIsHacked) {
		try {
			final Tech tech = Tech.valueOf("Aligned"+WOG[num]);
			if ( Settings.THE.getBoolean(tech.nom, false) ) {
				final boolean isHacked =
						Settings.THE.get("isa"+num+"ehacked", defaultIsHacked);
				final String className = "diuf.sudoku.solver.hinters.align."
						+ "Aligned"+num+"Exclusion"+(isHacked?"_2H":"_1C");
				final Class<?> clazz = Class.forName(className);
				final java.lang.reflect.Constructor<?> constructor =
					clazz.getConstructor(IInterruptMonitor.class);
				final IHinter hinter =
					(IHinter)constructor.newInstance(im);
				list.add(hinter);
			} else
				unwanted.add(tech);
		} catch (Exception ex) {
			StdErr.exit("wantAE fubarred", ex);
		}
	}
	// Num in wogalini. Think shape: a hexagon has sex sides, in New Zealand.
	private static final String[] WOG = new String[] {
		  "#0", "#1", "Pair", "Triple", "Quad", "Pent", "Hex", "Sept", "Oct"
		, "Nona", "Dec"
	};

	private static final IFormatter<IHinter> TECH_NAME = (IHinter h) ->
			h==null ? "" : h.getTech().name();

	/** Tester Logging Only: This method is used by LogicalSolverTester to
	 * display the names of the "wanted" hinters to the developer.
	 * @return A CSV (", ") list of the wanted hinter.getTech().name()s. */
	public final String getWantedHinterNames() {
		return Frmt.csv(wantedHinters, TECH_NAME);
	}

	private static final IFilter<IHinter> ENABLED = (IHinter h)->h.isEnabled();

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
		grid.rebuildAllRegionsS__t();
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
		// rebuild all the grid's s__t.
		grid.rebuildAllRegionsS__t();
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
			if ( !grid.isPrepared() ) // when we get first hint from this grid
				prepare(grid); // prepare wanted IPreparer's to process grid
			any = getAll(wantedHinters, grid, accu, !wantMore, isNoisy);
			// unset doSiamese or Generate goes mental. I'm in two minds RE
			// putting this in a finally block: safer but slower. This works.
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
			  || ( hinter.isEnabled()
			    && (findHints(hinter, grid, accu, isNoisy) && less) ) )
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
		double d, pd=0.0D; // difficulty, puzzleDifficulty
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
		final IHinter[] hinters = wantedHinters.toArray(new IHinter[wantedHinters.size()]);
		grid.rebuildAllRegionsS__t();
		for ( int pre=grid.countMaybes(),now; pre>0; pre=now ) { // ie !isFull
			if ( getFirst(hinters, grid, accu, false) ) {
				if ( interrupt() )
					return pd; // not sure what I should return
				AHint hint = accu.getHint();
				assert hint != null;
				// NB: AggregatedChainingHint.getDifficulty returns the maximum
				// difficulty of all the hints in the aggregate.
				d = hint.getDifficulty();
				assert d >= Difficulty.Easy.min; // there is no theoretical max
				if ( d>pd && (pd=d)>maxDif )
					return pd; // maximum desired difficulty exceeded
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
		return pd;
	}
	private boolean anyDisabled;

	/**
	 * Get the Solution of this puzzle. Used by generate and by the <i>Tools ~
	 * Analyse</i> menu-item in the SudokuFrame.
	 *
	 * @param grid a copy of the Grid to solve (not the GUI's grid)
	 * @return an AHint which is either an AnalysisHint or a WarningHint.
	 */
	public AHint analyse(Grid grid) {
		GrabBag.grid = grid; // this is a copy-to-sove, not the GUI's grid.
		if (Log.MODE >= Log.NORMAL_MODE)
			Log.format("%sanalyse: %s%s%s%s", NL, grid.source, NL, grid, NL);
		final SingleHintsAccumulator accu = new SingleHintsAccumulator();
		final long t0 = System.nanoTime();
		if ( grid.isFull() )
			return new WarningHint(recursiveAnalyser
					, "The Sudoku has been solved", "SudokuSolved.html");
		// execute just the puzzle validators.
		if ( getFirst(puzzleValidators, grid, accu, false) )
			return accu.getHint();
		final LogicalAnalyser analyser = new LogicalAnalyser(this);
		// LogicalAnalyser.findHints allways returns true, only the returned
		// hint-type varies: solved=AnalysisHint or invalid="raw" WarningHint,
		// which are both WarningHints; so telling them apart is a bit tricky.
		if ( analyser.findHints(grid, accu) ) {
			final long t1 = System.nanoTime();
			final AHint hint = accu.getHint(); // a SolutionHint
			if (Log.MODE >= Log.NORMAL_MODE) {
				Log.format("%,15d\t%2d\t%4d\t%3d\t%-30s\t%s%s"
						, t1-t0, grid.countFilledCells(), grid.countMaybes()
						, hint.getNumElims(), hint.getHinter(), hint, NL);
				if ( hint instanceof AnalysisHint )
					Log.println(((AnalysisHint)hint).appendUsageMap());
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
	// throws UnsolvableException
	private boolean validatePuzzle(Grid grid, IAccumulator accu) {
		if ( tooFewClues.findHints(grid, accu) // NumberOfClues >= 17
		  || tooFewValues.findHints(grid, accu) ) // NumberOfValues >= 8
			// skip RecursiveAnalysis, the third puzzleValidator
			// throws UnsolvableException
			return carp("Invalid Sudoku: " + accu.getHint(), grid, true);
		return true; // the puzzle is (probably) valid.
	}

	/**
	 * Solve 'grid' logically to populate 'usageMap' and return success.
	 * In English: Solves the given Sudoku puzzle (the 'grid') using logic to
	 * populate the given 'usageMap' with a summary of which Hinters were used
	 * how often, and how long each took to run; to return "was the puzzle
	 * solved", which should be true for any/every valid puzzle, but never say
	 * never. An invalid puzzle should throw an UnsolvableException (a
	 * RuntimeException) so solve never returns false. Never say never.
	 *
	 * @param grid Grid containing the Sudoku puzzle to be solved
	 * @param usage UsageMap (empty) that I populate with hinter usage: number
	 *  of calls, hint count, elapsed time, and number of maybes eliminated
	 * @param validate true means run the TooFewClues and TooFewValues
	 *  validators before solving the puzzle.
	 * @param isNoisy true logs progress, false does it all quietly.
	 * @param logHints only true in LogicalSolverTester.
	 * @return was it solved; else see grid.invalidity
	 * @throws UnsolvableException which is a RuntimeException means that this
	 *  puzzle is invalid and/or cannot be solved. See the exceptions message
	 *  (if any) for details, and/or grid.invalidity field.
	 */
	public boolean solve(Grid grid, UsageMap usage, boolean validate
			, boolean isNoisy, boolean logHints) {
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
		// the LogicalSolverTester, which is all a bit dodgy, but only a bit;
		// all because it's hard to differentiate "problem" messages. Sigh.
		if ( validate )
			validatePuzzle(grid, accu); // throws UnsolvableException
		if ( grid.source != null )
			prepare(grid); // prepare hinters to process this grid
		// Get an array of the wanted hinters
		IHinter[] hinters = wantedHinters.toArray(new IHinter[wantedHinters.size()]);
		// and they're off and hinting ...
		int hintNum = AHint.hintNumber = 1; // at the first hint
		AHint problem, hint;
		long now;
		while ( !grid.isFull() ) {
			// rebuild before validate for RecursiveAnalyser.
			grid.rebuildAllRegionsS__t();
			// detect invalid grid, meaning a hinter is probably borken!
			if ( (problem=validatePuzzleAndGrid(grid, false)) != null )
				throw new UnsolvableException("Houston: "+problem);
			// getHints from each enabled hinter
			if ( !timeHinters(hinters, grid, accu, usage, hintNum) )
				// throws UnsolvableException
				return carp("Hint not found", grid, true);
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
		AHint.resetHintNumber();
		return true;
	}

	// Called by solve to time the execution of the getHints methods of
	// these enabledHinters.
	private boolean timeHinters(IHinter[] hinters, Grid grid
			, IAccumulator accu, UsageMap usageMap, int hintNum) {
		long start;  boolean any;
		for ( IHinter hinter : hinters )
			if ( hinter.isActive() ) {
				start = System.nanoTime();
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
	// @param hint to apply
	// @param hintCount is the 1 based count of hints in this puzzle
	// @param took nanoseconds between now and previous hint
	// @param grid to apply the hint to
	// @param usage for the updateUsage method (below)
	// @param noisy if true log progress, else do it quietly
	// @throws UnsolvableException on the odd occassion
	// @return the number of cell-values eliminated (and/or 10 per cell set)
	private int apply(AHint hint, int hintCount, long took, Grid grid
			, UsageMap usage, boolean isNoisy, boolean logHints) {
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
			Print.hint(Log.out, hintCount, took, grid, numElims, hint, true);
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
			return;
		}
		// nb: terniaries are slower.
		if ( hint instanceof AggregatedHint )
			u.numHints += ((AggregatedHint)hint).hints.size();
		else
			++u.numHints;
		u.numElims += numElims;
		// nb: Math.max is slower.
		double d = hint.getDifficulty();
		if ( d > u.maxDifficulty )
			u.maxDifficulty = d;
		// addonate to subHints Map: for hinters producing multiple hint types.
		u.addonateSubHints(hint, 1);
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
	 * Cache {@code LinkedList<IPreparer>} from hinters where isa IPreparer.
	 * <p>
	 * The cache is updated when it's out-dated, that is: when the cache does
	 * not exist yet or Settings.THE.getModificationCount() differs from when
	 * the cache was read previously; in case the wantedHinters have changed,
	 * otherwise we will prepare no-longer-wanted-hinters, which is no biggy.
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
	private LinkedList<IPreparer> preppersCache; // defaults to null
	private int prepyModCount; // defaults to 0

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
		if ( throwIt ) {
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

}
