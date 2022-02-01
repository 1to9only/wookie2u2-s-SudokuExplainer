/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Grid.F;
import static diuf.sudoku.Grid.T;
import diuf.sudoku.solver.LogicalAnalyser;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.aals.*;
import diuf.sudoku.solver.hinters.align.*;
import diuf.sudoku.solver.hinters.align2.*;
import diuf.sudoku.solver.hinters.als.*;
import diuf.sudoku.solver.hinters.chain.*;
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
import static diuf.sudoku.utils.Frmt.dbl;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.IFilter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A Tech(nique) is a Sudoku solving technique, which is a light-weight place
 * holder for a possibly heavy implementation of this technique, ergo a hinter.
 * In theory, a hinter implements a technique, and conversely a technique is
 * implemented by a hinter, but it's not always that simple in practice.
 * <p>
 * So Tech is a Set of Sudoku solving techniques, implemented as an Enum. Most
 * hinters implement a single Sudoku solving technique as described previously.
 * I call these simple hinters, and the rest complex. Some hinters (such as
 * MultipleChainer) implement several solving techniques, because implementing
 * them separately is harder and/or slower. Some techniques have multiple
 * implementations, but whenever this occurs I split the Tech into multiple
 * Tech's (for example BigWings vs S/T/U/V/WXYZ_Wing) allowing the user to
 * choose between implementations, not just techniques.
 * <p>
 * It's Tech coz Technique is too long, and has three F's and a silent Q. All
 * the smart people hate Q's, and there smelly U-ism. It's a little-known fact
 * that most people are unaware of there innate Queue-Biff-Phobia. Time travel
 * may also cause Biff-Phobia, but alternate facts still remain stubbornly
 * non-conjugative, such that any and all attempts to construct a reality from
 * alternate facts is likely to end in selfextinction. Some folks suffer quince
 * allergies, other folks are just allergic to bloody quinces. Virjuice anyone?
 * Say no more. Basically, I just like short names (with long explanations).
 * <p>
 * LogicalSolver constructor has two 'want' methods. The first is "normal": it
 * takes an IHinter (an instance of a class implementing IHinter to actually
 * implement a Tech, sigh) and adds it to the 'wanted' List if Tech is wanted.
 * The second 'want' method is a bit trickier: it takes a Tech and sees if it's
 * wanted. If so it instantiates da registered implementation with reflections,
 * and adds that to the 'wanted' List; which saves constructing hinters (which
 * may be a heavy process) that aren't wanted, and therefore won't be used.
 * <p>
 * As it works-out most "simple" hinters are want(Tech)'d, and most "complex"
 * hinters are created manually and want(IHinter)'d. The Four Quick Foxes, that
 * are included in the "basics", are created individually and perversely added
 * with the "complex" want(IHinter). Basically, I did all this coz I can, and
 * to avoid heavy constructors which are then not used. So caveat emptor: I did
 * this coz I wanted to, and for a reason, but my reasoning is not inviolate.
 * <p>
 * The constructor of each subtype of AHinter passes a Tech down-to AHinter to
 * identify the implemented Sudoku solving technique, it's degree, and base
 * difficulty (which is more predictable than maxDifficulty). Most hinters
 * implement one Tech, but a few (esp ChainerMulti) implement multiple Techs,
 * so LogicalSolver prints both hinter-names and hint-types, so that hopefully
 * you can find the hinter. Aligned*Exclusion reports implementation.className
 * instead of tech.name as it's hint-type coz each A*E has two impls, which we
 * choose between at compile-time with the constant
 * {@link diuf.sudoku.solver.LogicalSolver#USE_NEW_ALIGN2 USE_NEW_ALIGN2}
 * passed to the custom wantAE method. Big Sigh.
 * <p>
 * So this is all pretty bloody confusing to start with, but it sort-of-works,
 * so please don't "fix" it until you understand it, ie when you don't want-to.
 * Yes, it's a cluster____, but it's an organic cluster____ that's been allowed
 * to evolve by adapting to complications as they arise. Da problem with design
 * is you decide BEFORE you see da consequences, hence all "real" solutions are
 * evolutions. You can't design "real" solutions, they must evolve through
 * experience.
 * <pre>
 * Design -&gt; Implement -&gt; Design -&gt; Implement, in an endless loop.
 * </pre>
 * <b>WARNING:</b> DO NOT rename Tech or Netbeans goes straight to hell in a
 * hand-basket! Delete C:/Users/User/AppData/Local/NetBeans/Cache/8.2/*.*
 * <p>
 * <b>WARNING:</b> Some code does tech.name().startsWith("Whatever") to
 * determine types, so be real-bloody-careful changing names. Test everything!
 * <p>
 * <b>WARNING:</b> The TechSelectDialog displays Techs in the order given here,
 * So <b>DO NOT</b> re-order them to match LogicalSolver constructor, or by
 * Difficulty, or whatever. The TechSelectDialog is the primary consideration.
 * This is not ideal, it just is what it is. Ideally the TechSelectDialog would
 * have it's own order (layout), which would also require maintenance. sigh.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then also please:<ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#LogicalSolver} to keep order of
 *   wantedHinters near to Tech.difficulty; so hinters are run in increasing
 *   difficulty, to produce the simplest possible solution to each puzzle.
 *  <li>{@link diuf.sudoku.Tech} // double check your difficulties
 *  <li>{@link diuf.sudoku.Difficulty} // double check the ranges
 * </ul>
 * <p>
 * NOTE: The longest Tech.name() is 18 characters.
 */
public enum Tech {

	// LogicalAnalyser hint (note that this isn't really a solving technique)
	Analysis				(0.0, 0, LogicalAnalyser.class, F)

	// Validators: these aren't real techs (marked by difficulty=0.0), but each
	// validator has a Tech so it works a lot like a hinter, except it produces
	// a hint when there's a problem (not an elimination).
	// puzzleValidators (run once, after puzzle is loaded)
	, TooFewClues		(0.0, 0, TooFewClues.class, F)
	, TooFewValues		(0.0, 0, TooFewValues.class, F)
	, NotOneSolution	(0.0, 0, BruteForce.class, F)
	// gridValidators (run repeatedly, after each hint.apply)
	, MissingMaybes		(0.0, 0, MissingMaybes.class, F)
	, DoubleValues		(0.0, 0, DoubleValues.class, F)
	, HomelessValues		(0.0, 0, HomelessValues.class, F)

	// Easy
	// directs
	, LonelySingle		(1.0, 1, LonelySingle.class, F)
	, NakedSingle		(1.1, 1, NakedSingle.class, F)
	, HiddenSingle		(1.2, 1, HiddenSingle.class, F)

	// Medium
	// NB: constructor does name().startsWith("Direct")
	, DirectNakedPair	(2.0, 2, NakedSetDirect.class)
	, DirectHiddenPair	(2.1, 2, HiddenSetDirect.class)
	, DirectNakedTriple	(2.2, 3, NakedSetDirect.class)
	, DirectHiddenTriple	(2.3, 3, HiddenSetDirect.class)
	, Locking			(2.4, 1, Locking.class, T, "OR Locking Basic", F)
	, LockingBasic		(2.5, 1, LockingGen.class, F, "OR Locking", F)

	// Hard
	, NakedPair			(3.0, 2, NakedSet.class)
	, HiddenPair		(3.1, 2, HiddenSet.class)
	, NakedTriple		(3.2, 3, NakedSet.class)
	, HiddenTriple		(3.3, 3, HiddenSet.class)
	// Fish should have Fish names, not Wing names (confusing); so
	// Yoda raises Luke's X-Wing from a Tatooen swamp => Swampfish
	, Swampfish			(3.4, 2, BasicFisherman.class) // aka X-Wing
	, TwoStringKite		(3.5, 4, TwoStringKite.class)
	, W_Wing			(3.6, 4, WWing.class)
	, XY_Wing			(3.7, 2, XYWing.class)
	, Skyscraper		(3.8, 4, Skyscraper.class)
	, EmptyRectangle		(3.9, 4, EmptyRectangle.class)

	// Fiendish
	, XYZ_Wing			(4.0, 3, XYWing.class)
	, Swordfish			(4.1, 3, BasicFisherman.class)
	// NB: Coloring/GEM find all Jellyfish, so run before them if at all.
	, NakedQuad			(4.2, 4, NakedSet.class)
	, Jellyfish			(4.3, 4, BasicFisherman.class, F, "DROP: just a bit rare (5 in top1465)", F)
	, HiddenQuad		(4.4, 4, HiddenSet.class, F, "DROP: rare (2 in top1465) so a bit slow", F)
	// FYI: Larger (5+) Fish are all DEGENERATE (comprised of simpler hints)
	, Coloring			(4.5, 0, Coloring.class, T,  "KEEP SimpleColoring and the ONLY MultiColoring", F)
	, XColoring			(4.6, 0, XColoring.class, T, "KEEP SimpleColoring+ hits when GEM misses (by design)", F)
	, Medusa3D			(4.7, 0, Medusa3D.class, F,  "DROP SimpleColoring++ counter-productive with GEM", F)
	, GEM				(4.8, 0, GEM.class, T,       "KEEP SimpleColoring+++ Graded Equivalence Marks", F)
	, URT				(4.9, 0, UniqueRectangle.class, T, "Unique RecTangles and loops", F)

	// Airotic
	// BigWings is S+T+U+V+WXYZ_Wing: slower, but faster over-all with ALS_*.
	// difficulty + 0.1 foreach als-cell beyond 3.
	, BigWings			(5.0, 0, BigWings.class, T, "or individual S/T/U/V/WXYZ Wing", F)
	, WXYZ_Wing			(5.0, 3, BigWing.class, F, "or Big Wings", F) // 3 Cell ALS + bivalue
	, VWXYZ_Wing		(5.1, 4, BigWing.class, F, "or Big Wings", F) // 4 Cell ALS + bivalue
	, UVWXYZ_Wing		(5.2, 5, BigWing.class, F, "or Big Wings", F) // 5 Cell ALS + bivalue
	, TUVWXYZ_Wing		(5.3, 6, BigWing.class, F, "or Big Wings", F) // 6 Cell ALS + bivalue
	, STUVWXYZ_Wing		(5.4, 7, BigWing.class, F, "or Big Wings", F) // 7 Cell ALS + bivalue // SLOW'ish
	, NakedPent			(5.6, 5, NakedSet.class, F,  "DROP: degenerate", F)
	, HiddenPent			(5.7, 5, HiddenSet.class, F, "DROP: degenerate", F)

	// Fishalsic
	, FinnedSwampfish	(6.0, 2, ComplexFisherman.class, T, "Now with Sashimi!", F)
	, FinnedSwordfish	(6.1, 3, ComplexFisherman.class, T, "Now with Sashimi!", F)
	, FinnedJellyfish	(6.2, 4, ComplexFisherman.class, F, "DROP: a bit slow", F)
	, DeathBlossom		(6.3, 0, DeathBlossom.class) // 2or3 stem.maybes -> ALS
	, ALS_XZ			(6.4, 0, AlsXz.class)		 // 2 ALSs in a Chain
	, ALS_Wing			(6.5, 0, AlsWing.class)		 // 3 ALSs in a Chain
	, ALS_Chain			(6.6,26, AlsChain.class)	 // 4+ ALSs in a Chain
	, SueDeCoq			(6.7, 0, SueDeCoq.class, F, "DROP: a bit slow", F) // AALSs
	, FrankenSwampfish	(6.8, 2, ComplexFisherman.class, F, "DROP: a bit slow", F) // OK
	, FrankenSwordfish	(6.9, 3, ComplexFisherman.class, F, "DROP: a bit slow", F) // OK
	, FrankenJellyfish	(7.0, 4, ComplexFisherman.class, F, "DROP: SLOW 45 seconds", F) // SLOW
	, KrakenSwampfish	(7.1, 2, KrakenFisherman.class, F,  "DROP: Krakens are nuts!", F) // OK
	, MutantSwampfish	(7.2, 2, ComplexFisherman.class, F, "DROP: degenerate", T)
	, KrakenSwordfish	(7.3, 3, KrakenFisherman.class, F,  "DROP: SLOW 30 seconds and Krakens are nuts!", F)
	, MutantSwordfish	(7.4, 3, ComplexFisherman.class, F, "DROP: SLOW 2 minutes", F)
	// WARNING: Kraken and Mutant Jellyfish are too slow to be allowed!
	, KrakenJellyfish	(7.5, 4, KrakenFisherman.class, F,  "DROP: TOO SLOW 6 minutes and Krakens are nuts!", T)
	, MutantJellyfish	(7.6, 4, ComplexFisherman.class, F, "DROP: FAR TOO SLOW 20 minutes", T)

	// Ligature: Aligned Ally, one up from Diagon Ally.
	// NB: constructor does name().startsWith("Aligned")
	// A234E are all off to new school, yeah!
	, AlignedPair		(8.0, 2, AlignedExclusion.class, F, "DROP: Rare with my preferred hinters", F)	// OK
	, AlignedTriple		(8.1, 3, AlignedExclusion.class, F, "DROP: A tad slow", F)
	, AlignedQuad		(8.2, 4, AlignedExclusion.class, F, "DROP: A bit slow", F)
	// A5678910E old-school _2H is the "registered" implementation
	, AlignedPent		(8.3, 5, Aligned5Exclusion_2H.class, F, "DROP: SLOW 1 minute correct", F)
	, AlignedHex		(8.4, 6, Aligned6Exclusion_2H.class, F, "DROP: SLOW 3 minutes correct", F)
	// WARNING Aligned 7, 8, 9, and especially 10 are too slow to be allowed!
	, AlignedSept		(8.5, 7, Aligned7Exclusion_2H.class, F, "DROP: TOO SLOW 6 minutes correct", T)
	, AlignedOct			(8.6, 8, Aligned8Exclusion_2H.class, F, "DROP: FAR TOO SLOW 19 minutes correct", T)
	, AlignedNona		(8.7, 9, Aligned9Exclusion_2H.class, F, "DROP: WAY TOO SLOW 3 hours correct", T)
	, AlignedDec			(8.8,10, Aligned10Exclusion_2H.class, F, "DROP: CONSERVATIVE 6 hours correct", T)

	// Diabolical
	// chains			 diff,dg, className,          Multi,Dynam,Nishi,want
	// forcing chains on a single value               M,D,N,w
	, UnaryChain		( 9.0, 0, ChainerUnary.class, F,F,F,T) // OK
	// contradictory consequences of a single (bad) assumption.
	// NB: ChainerMulti Tech.className's are NOT used. Retained anyway. sigh.
	, NishioChain		(10.0, 0, ChainerMulti.class, F,T,T,T) // a tad slow
	// forcing chains from multiple values/places.
	, MultipleChain		(11.0, 0, ChainerMulti.class, T,F,F,T) // OK
	// forcing chains including consequences (aka a forcing net)
	// NB: all subsequent chainers are implicitly "dynamic"
	, DynamicChain		(12.0, 0, ChainerMulti.class, T,T,F,T) // OK

	// IDKFA (ironic)
	// NOTE: IDKFAs are rare, so randomly generating one takes ages, if at all.
	// dynamic chaining using the Four Quick Foxes
	, DynamicPlus		(15.0, 1, ChainerMulti.class, T,T,F,T) // SLOW near catch-all
	// NB: constructor does name().startsWith("Nested")
	// dynamic chaining using a UnaryChainer
	, NestedUnary		(20.0, 2, ChainerMulti.class, T,T,F,T)  // SLOW catch-all
	// dynamic chaining using a MultipleChainer
	, NestedMultiple		(25.0, 3, ChainerMulti.class, T,T,F,F) // SLOW
	// dynamic chaining using a DynamicChainer
	, NestedDynamic		(30.0, 4, ChainerMulti.class, T,T,F,F) // SLOW
	// dynamic chaining using a DynamicPlus chainer
	, NestedPlus			(35.0, 5, ChainerMulti.class, T,T,F,F) // BUGS!
	// note that difficulty isn't proportional, just increasing.
	// Generating a puzzle that contains a NestedPlus hint is impossible.
	// It is NOT 35 times harder than a NakedSingle, it is impossible.
	;

	/**
	 * Techs implemented by the ChainerMulti class.
	 */
	public static final Tech[] MULTI_CHAINER_TECHS = {
		  NishioChain
		, MultipleChain
		, DynamicChain
		, DynamicPlus
		, NestedUnary
		, NestedMultiple
		, NestedDynamic
		, NestedPlus
	};

	/**
	 * Techs implemented by the BigWing class.
	 */
	public static final Tech[] BIG_WING_TECHS = {
		  WXYZ_Wing
		, VWXYZ_Wing
		, UVWXYZ_Wing
		, TUVWXYZ_Wing
		, STUVWXYZ_Wing
	};

	/**
	 * Returns a new {@code EnumSet<Tech>} of given 'techs'
	 * which are accepted by IFilter 'f'.
	 *
	 * @param techs the set to select from
	 * @param f an implementation of {@link IFilter<Tech>} whose accept method
	 *  determines which members of 'techs' are added to the result set
	 * @return a {@code EnumSet<Tech>} of matching 'techs'
	 */
	public static EnumSet<Tech> where(final EnumSet<Tech> techs, final IFilter<Tech> f) {
		final EnumSet<Tech> result = EnumSet.noneOf(Tech.class);
		for ( final Tech t : techs )
			if ( f.accept(t) )
				result.add(t);
		return result;
	}

	/**
	 * Returns a new {@code EnumSet<Tech>} of all Tech's
	 * which are accepted by IFilter 'f'.
	 *
	 * @param f an implementation of {@link IFilter<Tech>} whose accept method
	 *  determines which members of 'techs' are added to the result set
	 * @return a {@code EnumSet<Tech>} of accepted 'techs'
	 */
	public static EnumSet<Tech> where(final IFilter<Tech> f) {
		return where(EnumSet.allOf(Tech.class), f);
	}

	/**
	 * Returns a new {@code EnumSet<Tech>} of all "hinters", ie those Tech's
	 * with {@code difficulty > 0.0D}.
	 * <p>
	 * Non-hinter Tech's (validators) are identified by a difficulty of 0.0D.
	 *
	 * @return a new {@code EnumSet<Tech>} of all hinter Tech's
	 */
	public static EnumSet<Tech> allHinters() {
		return where((t) -> t.difficulty > 0.0D);
	}

	/**
	 * Returns CSV of tech names.
	 *
	 * @param techs to get the names of
	 * @return a Comma Separated Values String of the names of these 'techs'
	 */
	public static String techNames(final Tech[] techs) {
		final StringBuilder sb = new StringBuilder(techs.length*22);
		boolean first = true;
		for ( Tech tech : techs ) {
			if(first) first=false; else sb.append(CSP);
			sb.append(tech.name());
		}
		return sb.toString();
	}

	/**
	 * Build an array of this List of 'techs'.
	 *
	 * @param techs to add to an array
	 * @return a new {@code Tech[]} containing the given 'techs'
	 */
	public static Tech[] array(final List<Tech> techs) {
		final Tech[] result = new Tech[techs.size()];
		int cnt = 0;
		for ( Tech tech : techs )
			result[cnt++] = tech;
		return result;
	}

	/**
	 * Build a {@code java.util.ArrayList<Tech>} of this array of Techs.
	 *
	 * @param techs to put in a List
	 * @return a new {@code ArrayList<Tech>} containing the given 'techs'
	 */
	public static ArrayList<Tech> list(final Tech[] techs) {
		final ArrayList<Tech> result = new ArrayList<>(techs.length);
		for ( Tech tech : techs )
			result.add(tech);
		return result;
	}

	/**
	 * Translate this Tech name into words by:
	 * <pre>
	 * inserting a space before each capital,
	 *       and replacing underscore with a space;
	 * but consecutive capitals remain unchanged,
	 *     as do those following an underscore.
	 *
	 * So, words are split-up:
	 * EmptyRectangle -> Empty Rectangle
	 *
	 * And we handle acronyms with an underscore:
	 * XY_Wing -> XY Wing
	 * </pre>
	 *
	 * @return the given Tech.name(), in English
	 */
	private static String wordonate(final String techName) {
		final char[] src = techName.toCharArray();
		final char[] dst = new char[src.length*2];
		int i = 0; // index in the dst array (the write pointer)
		char p = 0; // the previous character in src
		for ( char c : src ) { // the current character in src
			if ( p != 0 && p != '_'
			  && !Character.isUpperCase(p)
			  && Character.isUpperCase(c) )
				dst[i++] = ' ';
			if ( c == '_' )
				dst[i++] = ' ';
			else
				dst[i++] = c;
			p = c;
		}
		return new String(dst, 0, i);
	}

	/**
	 * Is this technique wanted by default.
	 * <p>
	 * True for most Tech's. False for:<ul>
	 * <li>LockingBasic (Locking is faster)
	 * <li>S/T/U/V/WXYZ-Wing (BigWings faster over-all)
	 * <li>Medusa3D (use Coloring, XColoring, and GEM)
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
	 * where degree is the number of base (and cover) regions.
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
	public final boolean isMultiple; // search cell.maybesSize>2 and region.ridx[v].size>2
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
	 * @param warning is tip actually a warning? ie should TechSelectDialog
	 *  annoy the user with a message-box when this Tech is selected
	 * @param clazz the class which implements this Tech, or null if there is
	 *  no default implementation
	 * @param defaultWanted is this Tech wanted (enabled) by default. This only
	 *  matters the first time SudokuExplainer runs, thereafter "wanted" is
	 *  determined by the users Settings. This field exists coz some hinters
	 *  (like large A*E and Kraken/MutantSwampFish) are too slow to be allowed,
	 *  and yet they are allowable, but they're unwanted by default, to avoid
	 *  despoiling first user-experiences by taking two days to scratch myself,
	 *  when I can in fact take upto ten days to do so. sigh.
	 */
	private Tech(final double difficulty, final int degree, final Class<? extends IHinter> clazz, final boolean defaultWanted, final String tip, final boolean warning) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isDirect = name().startsWith("Direct");
		this.isAligned = name().startsWith("Aligned");
		this.className = clazz==null ? null : clazz.getName();
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
	private Tech(final double difficulty, final int degree) {
		this(difficulty, degree, null, true, null, false);
	}

	// basic + clazz
	private Tech(final double difficulty, final int degree, final Class<? extends IHinter> clazz) {
		this(difficulty, degree, clazz, true, null, false);
	}

	// basic + clazz and defaultWanted
	private Tech(final double difficulty, final int degree, final Class<? extends IHinter> clazz, final boolean defaultWanted) {
		this(difficulty, degree, clazz, defaultWanted, null, false);
	}

	// basic + defaultWanted
	private Tech(final double difficulty, final int degree, final boolean defaultWanted) {
		this(difficulty, degree, null, defaultWanted, null, false);
	}

	// Chainer (clazz is mandatory not null)
	private Tech(final double difficulty, final int degree, final Class<? extends IHinter> clazz, final boolean isMultiple, final boolean isDynamic, final boolean isNishio, final boolean defaultWanted) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isDirect = false;
		this.isAligned = false;
		this.className = clazz.getName();
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
	 * @return the JCheckBox.text for this Tech.
	 */
	public String text() {
		if ( text == null )
			text = dbl(difficulty)+SP+wordonate(name());
		return text;
	}
	private String text;

	/**
	 * Returns is this Tech in techs?
	 *
	 * @param techs
	 * @return is this Tech in the given 'techs' array.
	 */
	public boolean in(final Tech[] techs) {
		for ( Tech tech : techs )
			if ( this == tech )
				return true;
		return false;
	}

	// DO NOT USE: Just access the name attribute directly.
	@Override
	public String toString() {
		return name();
	}

}
