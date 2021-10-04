/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.align.*;
import diuf.sudoku.solver.hinters.align2.*;
import diuf.sudoku.solver.hinters.als.*;
import diuf.sudoku.solver.hinters.bug.*;
import diuf.sudoku.solver.hinters.color.*;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.hdnset.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.lock2.*;
import diuf.sudoku.solver.hinters.nkdset.*;
import diuf.sudoku.solver.hinters.sdp.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.solver.hinters.urt.*;
import diuf.sudoku.solver.hinters.wing.*;
import diuf.sudoku.utils.Frmt;
import java.util.EnumSet;
import static diuf.sudoku.utils.Frmt.COMMA_SP;
import static diuf.sudoku.utils.Frmt.SPACE;
import diuf.sudoku.utils.IFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * A Tech(nique) is a Sudoku Solving Technique: a place holder for a hinter.
 * This enum (a list) of Sudoku Solving Techniques. Each hinter implements a
 * Sudoku Solving Technique. Most hinters implement a single technique.
 * Some (like MultipleChainer) implement several techniques, because doing
 * them separately is harder.
 * <p>
 * It's called Tech because the word Technique is too long and has three F's
 * a silent Q. All reasonable people hate Q's, and there smelly U-ism. It's a
 * little known fact that most people are completely unaware of there innate
 * Queue-Biff-Phobia. Jumping around in time may also cause Biff-Phobia, but
 * alternate facts are non-conjugative. Some people are allergic to quinces.
 * Need I say more.
 * <p>
 * LogicalSolver constructor has two "want" methods. The first is "normal", it
 * takes an IHinter (an instance of a class that implements IHinter) and adds
 * it to the list if it's Tech is wanted. Simples. But the second is tricky: it
 * takes a Tech, sees if it's wanted, and if so it instantiates the registered
 * implementor (using reflections) of the Tech and adds that to the list; to
 * save creating a hinter that's not wanted. As it works-out simple hinters are
 * "want Tech"ed, but all the more complex hinters are created manually, and
 * then "want IHinter"ed. Sigh. Basically, I did this just because I can.
 * <p>
 * The constructor of each subtype of AHinter passes a Tech down-to AHinter to
 * identify the implemented Sudoku solving technique, it's degree, and base
 * difficulty (which is more predictable than maxDifficulty).
 * <p>
 * Most hinters implement one Tech, but a few (especially Chains) implement
 * multiple Techs, so the Solver now prints both hinter-names and hint-types,
 * so you can (hopefully) find the bloody hinter. sigh.
 * <p>
 * This is pretty confusing to start with, but it works, so please don't
 * "fix" it until you really understand it (aka when you don't want to).
 * <p>
 * <b>WARNING:</b> The TechSelectDialog displays Techs in the order they're
 *  given here. <b>DO NOT</b> re-order them to match LogicalSolver constructor,
 *  or by Difficulty, or whatever. TechSelectDialog is the key consideration.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then please look at:
 * <ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#LogicalSolver} to keep
 *   the order of wantedHinters near to that of Tech difficulty; so hinters run
 *   in increasing difficulty, to produce the simplest solution to each puzzle.
 *  <li>{@link diuf.sudoku.Tech} // for the actual difficulties
 *  <li>{@link diuf.sudoku.Difficulty} // double check the ranges
 * </ul>
 * <p>
 * <b>WARNING:</b> If you rename a Tech add it to LogicalSolverTester.aliases!
 * <p>
 * NOTE: The longest Tech.name() is 18 characters.
 */
public enum Tech {

	// LogicalAnalyser hint (note that this isn't really a solving technique)
	Analysis				(0.0, 0)

	// Validators (these aren't real techs either, marked by difficulty=0.0)
	// puzzleValidators
	, TooFewClues		(0.0, 0, TooFewClues.class, false)
	, TooFewValues		(0.0, 0, TooFewValues.class, false)
	, SingleSolution	(0.0, 0, SingleSolution.class, false)
	// gridValidators
	, NoMissingMaybes	(0.0, 0, NoMissingMaybes.class, false)
	, NoDoubleValues		(0.0, 0, NoDoubleValues.class, false)
	, NoHomelessValues	(0.0, 0, NoHomelessValues.class, false)

	// Easy
	// directs
	, LonelySingle		(1.0, 1, LonelySingle.class, false)
	, NakedSingle		(1.1, 1, NakedSingle.class, false)
	, HiddenSingle		(1.2, 1, HiddenSingle.class, false)

	// Medium
	// NB: constructor does name().startsWith("Direct")
	, Locking			(2.0, 1, "OR Locking Generalised", false, Locking.class, true)
	, LockingGeneralised(2.0, 1, "OR Locking", false, LockingGeneralised.class, false)
	, DirectNakedPair	(2.1, 2, NakedSet.class, true)
	, DirectHiddenPair	(2.2, 2, HiddenSet.class, true)
	, DirectNakedTriple	(2.3, 3, NakedSet.class, true)
	, DirectHiddenTriple	(2.4, 3, HiddenSet.class, true)

	// Hard
	, NakedPair			(2.50, 2, NakedSet.class, true)
	, HiddenPair		(2.55, 2, HiddenSet.class, true)
	, NakedTriple		(2.60, 3, NakedSet.class, true)
	, HiddenTriple		(2.65, 3, HiddenSet.class, true)
	, Swampfish			(2.70, 2) // Luke, Swampfish, Yoda, X-Wing.
	, TwoStringKite		(2.80, 4, TwoStringKite.class, false)

	// Fiendish
	, XY_Wing			(3.00, 2, XYWing.class, true)
	, XYZ_Wing			(3.05, 3, XYWing.class, true)
	, W_Wing			(3.10, 4, WWing.class, false)
	, Swordfish			(3.20, 3)
	, Skyscraper		(3.30, 4, Skyscraper.class, false)
	, EmptyRectangle		(3.35, 4, EmptyRectangle.class, false)
	, Jellyfish			(3.40, 4) // None in top1465 with my preferred techs
	// FYI: Larger Fish are all DEGENERATE (comprised of simpler hints)
	, BUG				(3.50, 0, "DROP (Bivalue Universal Grave) Old and slow", true, BUG.class, false)
	, Coloring			(3.52, 0, "KEEP BUG++ Simple-Coloring and the ONLY Multi-Coloring", false, Coloring.class, true)
	, XColoring			(3.54, 0, "KEEP Coloring++ finds stuff that GEM misses", false, XColoring.class, true)
	, Medusa3D			(3.56, 0, "DROP XColoring++ counter-productive with GEM", true, Medusa3D.class, false)
	, GEM				(3.58, 0, "KEEP (Graded Equivalence Marks) Medusa3D++", false, GEM.class, true)
	, NakedQuad			(3.60, 4, NakedSet.class, true)
	, HiddenQuad		(3.62, 4, HiddenSet.class, true)
	, NakedPent			(3.64, 5, "Degenerate", true, NakedSet.class, false)  // DEGENERATE
	, HiddenPent			(3.66, 5, "Degenerate", true, HiddenSet.class, false) // DEGENERATE
	// BigWings is all BigWing: slower (sigh), but faster over-all (yeah).
	, BigWings			(3.70, 0, "or individual S/T/U/V/WXYZ-Wing", false, BigWings.class, true)
	, WXYZ_Wing			(3.71, 3, "or BigWings", false, BigWing.class, false) // 3 Cell ALS + bivalue
	, VWXYZ_Wing		(3.72, 4, "or BigWings", false, BigWing.class, false) // 4 Cell ALS + bivalue
	, UVWXYZ_Wing		(3.73, 5, "or BigWings", false, BigWing.class, false) // 5 Cell ALS + bivalue
	, TUVWXYZ_Wing		(3.74, 6, "or BigWings", false, BigWing.class, false) // 6 Cell ALS + bivalue
	, STUVWXYZ_Wing		(3.75, 7, "or BigWings", false, BigWing.class, false) // 7 Cell ALS + bivalue // SLOW'ish
	, URT				(3.80, 0, "Unique Rectangles and loops", false, UniqueRectangle.class, true)
	
	// Nightmare
	, FinnedSwampfish	(4.0, 2, ComplexFisherman.class, true)	// with Sashimi
	, FinnedSwordfish	(4.1, 3, ComplexFisherman.class, true)
	, FinnedJellyfish	(4.2, 4, null, false, ComplexFisherman.class, false)
	, ALS_XZ			(4.3, 0, AlsXz.class, false)		 // 2 ALSs in a Chain
	, ALS_Wing			(4.4, 0, AlsWing.class, false)		 // 3 ALSs in a Chain
	, ALS_Chain			(4.5, 0, AlsChain.class, false)	 // 4+ ALSs in a Chain
	, DeathBlossom		(4.6, 0, DeathBlossom.class, false) // ALS for each stem.maybe
	, SueDeCoq			(4.7, 0, SueDeCoq.class, false)	 // AALSs // a bit slow
	, FrankenSwampfish	(5.0, 2, null, false, ComplexFisherman.class, false)	// OK
	, FrankenSwordfish	(5.1, 3, null, false, ComplexFisherman.class, false)	// OK
	, FrankenJellyfish	(5.2, 4, null, false, ComplexFisherman.class, false)	// SLOW
	// Mutants are too slow to be allowed: 1,764 seconds for just 20 hints.
	, KrakenSwampfish	(5.3, 2, null, false, KrakenFisherman.class, false)		 // OK
	, MutantSwampfish	(5.4, 2, "Degenerate", true, ComplexFisherman.class, false) // NONE
	, KrakenSwordfish	(5.5, 3, "30 seconds", false, KrakenFisherman.class, false) // SLOW'ish
	, MutantSwordfish	(5.6, 3, "2 minutes", false, ComplexFisherman.class, false) // SLOW
	, KrakenJellyfish	(5.8, 4, "9 minutes", true, KrakenFisherman.class, false)	 // TOO SLOW
	, MutantJellyfish	(5.9, 4, "20 minutes", true, ComplexFisherman.class, false) // TOO SLOW

	// Diabolical
	// new-school A234E
	, AlignedPair		(6.0, 2, AlignedExclusion.class, false)	// OK
	, AlignedTriple		(6.1, 3, AlignedExclusion.class, false)	// a bit slow
	, AlignedQuad		(6.2, 4, AlignedExclusion.class, false)	// a bit slow
	// old-school A5678910E (The _2H class is the "registered" implementation)
	, AlignedPent		(6.3, 5, "1 minute correct", false, Aligned5Exclusion_2H.class, false)	// SLOW
	, AlignedHex		(6.4, 6, "3 minutes correct", false, Aligned6Exclusion_2H.class, false) // SLOW
	, AlignedSept		(6.5, 7, "6 minutes correct", true, Aligned7Exclusion_2H.class, false)	// VERY SLOW
	, AlignedOct			(6.6, 8, "19 minutes correct", true, Aligned8Exclusion_2H.class, false)	// TOO SLOW
	, AlignedNona		(6.7, 9, "3 hours correct", true, Aligned9Exclusion_2H.class, false)    // DEAD SLOW
	, AlignedDec			(6.8,10, "6 hours correct", true, Aligned10Exclusion_2H.class, false)   // CONSERVATIVE
	// chains			 diff     Multi,Dynam,Nishi,want
	, UnaryChain		( 7.0, 0, false,false,false,true) // OK
	, NishioChain		( 7.5, 0, false,true ,true ,true) // a bit slow
	, MultipleChain		( 8.0, 0, true ,false,false,true) // OK
	, DynamicChain		( 8.5, 0, true ,true ,false,true) // OK

	// IDKFA
	, DynamicPlus		( 9.0, 1, true ,true ,false,true) // SLOW nearly catch-all
	// NB: constructor does name().startsWith("Nested")
	, NestedUnary		( 9.5, 2, true ,true ,false,true)  // SLOW actual catch-all
	, NestedMultiple		(10.0, 3, true ,true ,false,false) // SLOW
	, NestedDynamic		(10.5, 4, true ,true ,false,false) // SLOW
	, NestedPlus			(11.0, 5, true ,true ,false,false) // TOO SLOW !BUGS!
	;

	public static EnumSet<Tech> where(EnumSet<Tech> techs, IFilter<Tech> f) {
		final EnumSet<Tech> result = EnumSet.noneOf(Tech.class);
		for ( Tech t : techs )
			if ( f.accept(t) )
				result.add(t);
		return result;
	}
	public static EnumSet<Tech> where(IFilter<Tech> f) {
		return where(EnumSet.allOf(Tech.class), f);
	}
	public static EnumSet<Tech> allHinters() {
		return where((t) -> t.difficulty > 0.0D);
	}

	public static String names(Tech[] techs) {
		final StringBuilder sb = new StringBuilder(techs.length*22);
		boolean first = true;
		for ( Tech tech : techs ) {
			if(first) first=false; else sb.append(COMMA_SP);
			sb.append(tech.name());
		}
		return sb.toString();
	}

	public static Tech[] array(List<Tech> techs) {
		final Tech[] result = new Tech[techs.size()];
		int cnt = 0;
		for ( Tech tech : techs )
			result[cnt++] = tech;
		return result;
	}

	public static List<Tech> list(Tech[] techs) {
		final List<Tech> result = new ArrayList<>(techs.length);
		for ( Tech tech : techs )
			result.add(tech);
		return result;
	}

	/**
	 * Is this technique wanted by default.
	 * <p>
	 * True for most Tech's. False for:<ul>
	 * <li>LockingGeneralised (Locking is faster)
	 * <li>S/T/U/V/WXYZ-Wing (BigWings faster over-all)
	 * <li>BUG and Medusa3D (use Coloring, XColoring, and GEM)
	 * <li>Franken, Mutant and Kraken *fish (too slow)
	 * <li>Aligned Exclusion (too slow to far too slow)
	 * </ul>
	 */
	public final boolean defaultWanted;

	/**
	 * The minimum or "base" difficulty for hints produced by this technique.
	 * <p>If you change any hints difficulty then you'll need to look at:<ul>
	 *  <li>{@link diuf.sudoku.Tech}
	 *  <li>{@link diuf.sudoku.Difficulty}
	 *  <li>and {@code getDifficulty()} comment in {@link diuf.sudoku.solver.AHint}
	 * </ul>
	 */
	public final double difficulty;

	/**
	 * Some hinters implement multiple Techs of different sizes, degree is the
	 * size: typically the number of cells or regions to be examined.
	 * <pre>
	 * For instance:
	 *
	 * NakedSets implements: NakedPair=2, NakedTriple=3, NakedQuad=4
	 * where degree is the number of cells in the set.
	 *
	 * Fisherman implements: Swampfish=2, Swordfish=3, Jellyfish=4
	 * where degree is the number of base (and therefore cover) regions.
	 * </pre>
	 */
	public final int degree;

	public final boolean isDirect; // true if this is a Direct rule.
	public final boolean isAligned; // true if this is an Aligned rule.

	public final String tip; // hint to user regarding issues with this Tech
	public final boolean warning; // is the tool-tip actually a warning?

	// these fields are only used for *Chain, DynamicPlues, and Nested*
	// they are all allways false for the "normal" (non-Chainer) Techniques.
	public final boolean isChainer; // true if we used the Chainer constructor.
	public final boolean isNested; // true if name().startsWith("Nested")
	public final boolean isMultiple; // search cell.maybesSize>2 and region.indexesOf[v].size>2
	public final boolean isDynamic; // combine effects of previous calculations
	public final boolean isNishio; // an assumption has both effect && !effect

	public final String className; // the name of the class which implements this Tech, where known at compile time.

	/**
	 * Constructor: By default the tool-tip is used as a warning, because that
	 * is how I coded it initially, but now I need tip for mouse over only, so
	 * the TechSelectDialog only displays warning MsgBox when warning, which is
	 * counter-intuitively true by default.
	 *
	 * @param difficulty base difficulty for hints of this type
	 * @param degree the number of things (cells or regions) in this pattern,
	 *  or 0 meaning none
	 * @param tip helper text in the TechSelectDialog
	 * @param warning is this actually a warning?
	 * @param clazz the class which implements this Tech, or null if there is
	 *  not default implementation
	 * @param passTech does this implementation need me to pass the Tech into
	 *  his constructor (basically is degree non-zero)
	 * @param defaultWanted is this Tech wanted (enabled) by default. This
	 *  only matters the first time SudokuExplainer runs, there-after it's
	 *  in the Registry.
	 */
	private Tech(double difficulty, int degree, String tip, boolean warning
			, Class<? extends IHinter> clazz, boolean defaultWanted) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isDirect = name().startsWith("Direct");
		this.isAligned = name().startsWith("Aligned");
		if ( clazz == null )
			this.className = null;
		else
			this.className = clazz.getName();
		this.defaultWanted = defaultWanted;

		this.isChainer = false;
		this.isNested = false;
		this.isMultiple = false;
		this.isDynamic = false;
		this.isNishio = false;

		this.tip = tip;
		this.warning = warning;
	}

	// basic
	private Tech(double difficulty, int degree) {
		this(difficulty, degree, null, false, null, true);
	}

	// basic + className, passTech
	private Tech(double difficulty, int degree, Class<? extends IHinter> clazz) {
		this(difficulty, degree, null, false, clazz, true);
	}

	// basic + className, passTech
	private Tech(double difficulty, int degree, Class<? extends IHinter> clazz, boolean defaultWanted) {
		this(difficulty, degree, null, false, clazz, defaultWanted);
	}

	// basic + defaultWanted
	private Tech(double difficulty, int degree, boolean defaultWanted) {
		this(difficulty, degree, null, false, null, defaultWanted);
	}

	// Chainer
	private Tech(double difficulty, int degree, boolean isMultiple
			, boolean isDynamic, boolean isNishio, boolean defaultWanted) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isDirect = false;
		this.isAligned = false;
		this.className = null;
		this.defaultWanted = defaultWanted;

		this.isChainer = true;
		this.isNested = name().startsWith("Nested");
		this.isMultiple = isMultiple;
		this.isDynamic = isDynamic;
		this.isNishio = isNishio;

		this.tip = null;
		this.warning = false;
	}

	/**
	 * Translate this Tech name into English by:
	 * <pre>
	 * inserting a space before each capital,
	 *       and replacing underscore with a space;
	 * but consecutive capitals remain unchanged,
	 *     as do those following an underscore.
	 * </pre>
	 *
	 * @return the given Tech.name(), in English
	 */
	private static String english(String techName) {
		char[] src = techName.toCharArray();
		char[] dst = new char[src.length*2];
		int cnt = 0;
		char prev = 0;
		for ( char ch : src ) {
			if ( prev!=0 && prev!='_'
			  && !Character.isUpperCase(prev)
			  && Character.isUpperCase(ch) )
				dst[cnt++] = ' ';
			if ( ch=='_' )
				dst[cnt++] = ' ';
			else
				dst[cnt++] = ch;
			prev = ch;
		}
		return new String(dst, 0, cnt);
	}

	/**
	 * @return the JCheckBox.text for this Tech.
	 */
	public String text() {
		if ( text == null )
			text = Frmt.frmtDbl(difficulty)+SPACE+english(name());
		return text;
	}
	private String text;

	// DO NOT USE: Just access the name attribute directly.
	@Override
	public String toString() {
		return name();
	}

}
