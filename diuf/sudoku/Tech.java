/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
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
import diuf.sudoku.solver.hinters.bug.*;
import diuf.sudoku.solver.hinters.chain.ChainerMulti;
import diuf.sudoku.solver.hinters.chain.ChainerUnary;
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
 * A Tech(nique) is a Sudoku solving technique, which is a light place-holder
 * for a heavy implementation of this technique, ergo a hinter. In theory, a
 * hinter implements a technique, and conversely a technique is implemented by
 * a hinter, but it is not that simple in reality. sigh.
 * <p>
 * So Tech is a set of Sudoku solving techniques. Most hinters implement a
 * single Sudoku solving technique (I call these "simple" hinters), but some
 * hinters (like MultipleChainer) implement several techniques (I call these
 * "complex" hinters), because implementing these techniques separately is
 * harder and/or slower. Also, some techniques have multiple implementations,
 * but when this occurs I split the technique into multiple Tech's (eg BigWings
 * vs S/T/U/V/WXYZ_Wing) to allow the user to choose between implementations.
 * <p>
 * It's called Tech because the word Technique is too long to type many times,
 * and has three F's and a silent Q. All reasonable people hate Q's, and there
 * smelly U-ism. It's a little-known fact that most people are unaware of there
 * innate Queue-Biff-Phobia. Jumping around in time may also cause Biff-Phobia,
 * but alternate facts remain stubbornly non-conjugative, such that any and all
 * attempts to construct a reality from alternate facts ends in selfextinction.
 * Some folks suffer quince allergies, other folks are just allergic to bloody
 * quinces. Say no more.
 * <p>
 * LogicalSolver constructor has two 'want' methods. The first is "normal": it
 * takes an IHinter (an instance of a class implementing IHinter to actually
 * implement a Tech, sigh) and adds it to the 'wanted' List if Tech is wanted.
 * The second 'want' method is a bit trickier: it takes a Tech and sees if it's
 * wanted. If so it instantiates da registered implementation with reflections,
 * and adds that to the 'wanted' List; which saves constructing hinters (which
 * may be a heavy process) that aren't wanted, and therefore won't be used.
 * <p>
 * As it works-out "simple" hinters are want(Tech)'d but many "complex" hinters
 * are created manually and want(IHinter)'d. sigh. Also my Four Quick Foxes are
 * created individually and perversely added with "complex" want(IHinter).
 * Basically, I did all this just coz I can, and to avoid heavy constructors
 * which are then not used; So caveat emptor: I did this coz I wanted to, and
 * for a reason, but my reasoning, as always, is questionable.
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
 * evolutions. You can't design a "real" solution.
 * <p>
 * <b>WARNING:</b> DO NOT rename Tech or Netbeans goes straight to hell in
 * hand-basket! Delete C:/Users/User/AppData/Local/NetBeans/Cache/8.2/*.*
 * <p>
 * <b>WARNING:</b> External code does tech.name().startsWith("Whatever") to
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
	, TooFewClues		(0.0, 0, InsufficientClues.class, F)
	, TooFewValues		(0.0, 0, InsufficientValues.class, F)
	, NotOneSolution	(0.0, 0, BruteForce.class, F)
	// gridValidators (run repeatedly, after each hint.apply)
	, MissingMaybes		(0.0, 0, NoMissingMaybes.class, F)
	, DoubleValues		(0.0, 0, NoDoubleValues.class, F)
	, HomelessValues		(0.0, 0, NoHomelessValues.class, F)

	// Easy
	// directs
	, LonelySingle		(1.0, 1, LonelySingle.class, F)
	, NakedSingle		(1.1, 1, NakedSingle.class, F)
	, HiddenSingle		(1.2, 1, HiddenSingle.class, F)

	// Medium
	, Locking			(2.0, 1, Locking.class, T, "OR Locking Basic", F)
	, LockingBasic		(2.0, 1, LockingGen.class, F, "OR Locking", F)
	// NB: constructor does name().startsWith("Direct")
	, DirectNakedPair	(2.1, 2, NakedSetDirect.class)
	, DirectHiddenPair	(2.2, 2, HiddenSetDirect.class)
	, DirectNakedTriple	(2.3, 3, NakedSetDirect.class)
	, DirectHiddenTriple	(2.4, 3, HiddenSetDirect.class)

	// Hard
	, NakedPair			(2.50, 2, NakedSet.class)
	, HiddenPair		(2.55, 2, HiddenSet.class)
	, NakedTriple		(2.60, 3, NakedSet.class)
	, HiddenTriple		(2.65, 3, HiddenSet.class)
	// Fish should have Fish names, not Wing names (confusing); so
	// Yoda raises Luke's X-Wing from a Tatooen swamp => Swampfish
	, Swampfish			(2.70, 2, BasicFisherman.class) // aka X-Wing
	, TwoStringKite		(2.80, 4, TwoStringKite.class)

	// Fiendish
	, XY_Wing			(3.00, 2, XYWing.class)
	, XYZ_Wing			(3.05, 3, XYWing.class)
	, W_Wing			(3.10, 4, WWing.class)
	, Swordfish			(3.20, 3, BasicFisherman.class)
	, Skyscraper		(3.30, 4, Skyscraper.class)
	, EmptyRectangle		(3.35, 4, EmptyRectangle.class)
	// There are no Jellyfish in top1465 with my preferred techs
	, Jellyfish			(3.40, 4, BasicFisherman.class, F)
	// FYI: Larger (5+) Fish are all DEGENERATE (comprised of simpler hints)
	, BUG				(3.50, 0, BUG.class, F,       "DROP (Bivalue Universal Grave) old and slow", T)
	, Coloring			(3.52, 0, Coloring.class, T,  "KEEP the only Multi-Coloring and basic Simple-Coloring", F)
	, XColoring			(3.54, 0, XColoring.class, T, "KEEP extended Simple-Coloring finds hints that GEM misses, by design", F)
	, Medusa3D			(3.56, 0, Medusa3D.class, F,  "DROP extended+ Simple-Coloring is counter-productive with GEM", T)
	, GEM				(3.58, 0, GEM.class, T,       "KEEP (Graded Equivalence Marks) the ultimate Simple-Coloring", F)
	, NakedQuad			(3.60, 4, NakedSet.class)
	, HiddenQuad		(3.62, 4, HiddenSet.class)
	, NakedPent			(3.64, 5, NakedSet.class, F,  "DEGENERATE", T)
	, HiddenPent			(3.66, 5, HiddenSet.class, F, "DEGENERATE", T)
	, URT				(3.70, 0, UniqueRectangle.class, T, "Unique Rectangles and loops", F)
	// BigWings is S+T+U+V+WXYZ_Wing: slower, but faster over-all with ALS_*.
	, BigWings			(3.80, 0, BigWings.class, T, "or individual S/T/U/V/WXYZ Wing", F)
	, WXYZ_Wing			(3.81, 3, BigWing.class, F, "or Big Wings", F) // 3 Cell ALS + bivalue
	, VWXYZ_Wing		(3.82, 4, BigWing.class, F, "or Big Wings", F) // 4 Cell ALS + bivalue
	, UVWXYZ_Wing		(3.83, 5, BigWing.class, F, "or Big Wings", F) // 5 Cell ALS + bivalue
	, TUVWXYZ_Wing		(3.84, 6, BigWing.class, F, "or Big Wings", F) // 6 Cell ALS + bivalue
	, STUVWXYZ_Wing		(3.85, 7, BigWing.class, F, "or Big Wings", F) // 7 Cell ALS + bivalue // SLOW'ish

	// Nightmare
	, FinnedSwampfish	(4.0, 2, ComplexFisherman.class) // with Sashimi
	, FinnedSwordfish	(4.1, 3, ComplexFisherman.class)
	, FinnedJellyfish	(4.2, 4, ComplexFisherman.class, F, null, F) // a bit slow
	, DeathBlossom		(4.3, 0, DeathBlossom.class, F) // stem.maybe->ALS
	, ALS_XZ			(4.4, 0, AlsXz.class, F)	// 2 ALSs in a Chain
	, ALS_Wing			(4.5, 0, AlsWing.class, F)	// 3 ALSs in a Chain
	, ALS_Chain_4		(4.60, 4, AlsChain.class, F) // 4 ALSs in a Chain
	, ALS_Chain_5		(4.61, 5, AlsChain.class, F) // 5 ALSs in a Chain
	, ALS_Chain_6		(4.62, 6, AlsChain.class, F) // 6 ALSs in a Chain (SLOW'ish)
	, ALS_Chain_7		(4.63, 7, AlsChain.class, F) // 7 ALSs in a Chain (TOO SLOW)
	, SueDeCoq			(4.8, 0, SueDeCoq.class, F) // AALSs // a bit slow
	, FrankenSwampfish	(5.0, 2, ComplexFisherman.class, F, null, F) // OK
	, FrankenSwordfish	(5.1, 3, ComplexFisherman.class, F, null, F) // OK
	, FrankenJellyfish	(5.2, 4, ComplexFisherman.class, F, null, F) // SLOW
	// Mutants are too slow to be allowed: 1,764 seconds for just 20 hints.
	, KrakenSwampfish	(5.3, 2, KrakenFisherman.class, F, null, F)  // OK
	, MutantSwampfish	(5.4, 2, ComplexFisherman.class, F, "DEGENERATE", T) // NONE
	, KrakenSwordfish	(5.5, 3, KrakenFisherman.class, F,  "30 seconds", F) // SLOW'ish
	, MutantSwordfish	(5.6, 3, ComplexFisherman.class, F, "2 minutes", F)  // SLOW
	, KrakenJellyfish	(5.8, 4, KrakenFisherman.class, F,  "9 minutes", T)	 // TOO SLOW
	, MutantJellyfish	(5.9, 4, ComplexFisherman.class, F, "20 minutes", T) // TOO SLOW

	// Diabolical
	// NB: constructor does name().startsWith("Aligned")
	// A234E are all off to new school, yeah!
	, AlignedPair		(6.0, 2, AlignedExclusion.class, F)	// OK
	, AlignedTriple		(6.1, 3, AlignedExclusion.class, F)	// a tad slow
	, AlignedQuad		(6.2, 4, AlignedExclusion.class, F)	// a bit slow
	// A5678910E old-school _2H is the "registered" implementation
	, AlignedPent		(6.3, 5, Aligned5Exclusion_2H.class, F, "1 minute correct", F)	 // SLOW
	, AlignedHex		(6.4, 6, Aligned6Exclusion_2H.class, F, "3 minutes correct", F)  // SLOW
	, AlignedSept		(6.5, 7, Aligned7Exclusion_2H.class, F, "6 minutes correct", T)	 // TOO SLOW
	, AlignedOct			(6.6, 8, Aligned8Exclusion_2H.class, F, "19 minutes correct", T) // FAR TOO SLOW
	, AlignedNona		(6.7, 9, Aligned9Exclusion_2H.class, F, "3 hours correct", T)    // WAY TOO SLOW
	, AlignedDec			(6.8,10, Aligned10Exclusion_2H.class, F, "6 hours correct", T)   // CONSERVATIVE
	// chains			 diff,dg, className,          Multi,Dynam,Nishi,want
	// forcing chains on a single value               M,D,N,w
	, UnaryChain		( 7.0, 0, ChainerUnary.class, F,F,F,T) // OK
	// contradictory consequences of a single (bad) assumption.
	// NB: ChainerMulti Tech.className's are NOT used. Retained anyway. sigh.
	, NishioChain		( 7.5, 0, ChainerMulti.class, F,T,T,T) // a tad slow
	// forcing chains from multiple values/places.
	, MultipleChain		( 8.0, 0, ChainerMulti.class, T,F,F,T) // OK
	// forcing chains including consequences (aka a forcing net)
	// NB: all subsequent chainers are implicitly "dynamic"
	, DynamicChain		( 8.5, 0, ChainerMulti.class, T,T,F,T) // OK

	// IDKFA (ironic)
	// NOTE: There's issues generating IDKFA: it'll find Diabolical instead.
	// dynamic chaining using the Four Quick Foxes
	, DynamicPlus		( 9.0, 1, ChainerMulti.class, T,T,F,T) // SLOW near catch-all
	// NB: constructor does name().startsWith("Nested")
	// dynamic chaining using a UnaryChainer
	, NestedUnary		( 9.5, 2, ChainerMulti.class, T,T,F,T)  // SLOW catch-all
	// dynamic chaining using a MultipleChainer
	, NestedMultiple		(10.0, 3, ChainerMulti.class, T,T,F,F) // SLOW
	// dynamic chaining using a DynamicChainer
	, NestedDynamic		(10.5, 4, ChainerMulti.class, T,T,F,F) // SLOW
	// dynamic chaining using a DynamicPlus chainer
	, NestedPlus			(11.0, 5, ChainerMulti.class, T,T,F,F) // BUGS!
	;

	/**
	 * An array of those Tech implemented by ChainerMulti.
	 */
	public static final Tech[] MULTI_CHAINERS = new Tech[] {
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
	 * An array of those Tech implemented by AlsChain.
	 */
	public static final Tech[] ALS_CHAIN_TECHS = new Tech[] {
		Tech.ALS_Chain_4, Tech.ALS_Chain_5, Tech.ALS_Chain_6, Tech.ALS_Chain_7
	};

	/**
	 * Returns an array of the ALS_CHAIN_TECHS except the given tech.
	 *
	 * @param tech to exclude
	 * @return a new Tech[] of the other three ALS_CHAIN_TECHS
	 */
	public static final Tech[] alsChainTechsExcept(Tech tech) {
		Tech[] result = new Tech[3];
		int cnt = 0;
		for ( Tech t : ALS_CHAIN_TECHS )
			if ( t != tech )
				result[cnt++] = t;
		return result;
	}

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
		for ( final Tech t : techs ) {
			if ( f.accept(t) ) {
				result.add(t);
			}
		}
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

//not_used 2021-09-12
//	/**
//	 * Returns a new {@code EnumSet<Tech>} of all non-hinters (validators),
//	 * ie those Tech's with {@code difficulty == 0.0D}.
//	 * <p>
//	 * Non-hinter Tech's (validators) are identified by a difficulty of 0.0D.
//	 *
//	 * @return a new {@code EnumSet<Tech>} of all validator Tech's
//	 */
//	public static EnumSet<Tech> allNonHinters() {
//		return where((t) -> t.difficulty == 0.0D);
//	}

	/**
	 * Build a String (a comma-separated list) of the names of these 'techs'.
	 * @param techs to get the names of
	 * @return CSV of the names of these 'techs'
	 */
	public static String names(final Tech[] techs) {
		final StringBuilder sb = new StringBuilder(techs.length*22);
		boolean first = true;
		for ( final Tech tech : techs ) {
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
		for ( final Tech tech : techs ) {
			result[cnt++] = tech;
		}
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
		for ( final Tech tech : techs ) {
			result.add(tech);
		}
		return result;
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
	private static String english(final String techName) {
		final char[] src = techName.toCharArray();
		final char[] dst = new char[src.length*2];
		int i = 0; // index in the dst array (the write pointer)
		char p = 0; // the previous character in src
		for ( final char c : src ) { // the current character in src
			if ( p != 0
			  && p != '_'
			  && !Character.isUpperCase(p)
			  && Character.isUpperCase(c) ) {
				dst[i++] = ' ';
			}
			if ( c == '_' ) {
				dst[i++] = ' ';
			} else {
				dst[i++] = c;
			}
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
		if ( text == null ) {
			text = dbl(difficulty)+SP+english(name());
		}
		return text;
	}
	private String text;

	// DO NOT USE: Just access the name attribute directly.
	@Override
	public String toString() {
		return name();
	}

}
