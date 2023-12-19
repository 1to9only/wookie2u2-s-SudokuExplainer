/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.*;
import diuf.sudoku.solver.LogicalAnalyser;
import diuf.sudoku.solver.checks.*;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.aals.*;
import diuf.sudoku.solver.hinters.align.*;
import diuf.sudoku.solver.hinters.als.*;
import diuf.sudoku.solver.hinters.chain.*;
import diuf.sudoku.solver.hinters.color.*;
import diuf.sudoku.solver.hinters.fish.*;
import diuf.sudoku.solver.hinters.hidden.*;
import diuf.sudoku.solver.hinters.lock.*;
import diuf.sudoku.solver.hinters.naked.*;
import diuf.sudoku.solver.hinters.sdp.*;
import diuf.sudoku.solver.hinters.single.*;
import diuf.sudoku.solver.hinters.table.*;
import diuf.sudoku.solver.hinters.urt.*;
import diuf.sudoku.solver.hinters.wing.*;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.IFilter;
import static java.lang.Character.isUpperCase;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Tech(nique) is the complete Set of Sudoku solving techniques, as an Enum.
 * <p>
 * A Tech is a light-weight place-holder for a possibly heavy implementation of
 * this Tech: a hinter, which is defined as a class that implements
 * {@link IHinter}, but in reality must extend AHinter, coz I'm an idiot.
 * <p>
 * Theoretically, each hinter implements one Tech, and conversely each Tech is
 * implemented by one hinter, but its not always that simple in practice. Most
 * hinters implement a single Tech. I call these "simple" and others "complex".
 * Some hinters (eg ChainerDynamic) implement many Techs, because implementing
 * them separately is harder and slower. A few Techs have multiple impls, but
 * when this occurs I (usually) split the Tech into one per implementation.
 * <p>
 * Its called Tech coz Technique is too long, and has three Fs and a silent Q.
 * All the best people hate Qs, and there smelly U-ism. Its a well-known fact
 * that most people are unaware of there innate Queue-Biff-Phobia. Time travel
 * may also cause Biff-Phobia, but alternate facts are non-conjugative, such
 * that any reality constructed from alternate facts ends in self-extinction.
 * Some folks suffer quince allergies. Other folks just hate bloody quinces.
 * Say no more. Basically, I like short names, and long, abstruse, irreverent,
 * inane, irrelevant, non-explanations; some even with cheese. AdamsD can bight
 * me. Sigh.
 * <p>
 * <b>WARN: DO NOT</b> rename Tech or Netbeans drops random methods!<br>
 * Fix-em and Delete {@code C:\Users\User\AppData\Local\NetBeans\Cache\8.2\*.*}
 * <b>WARN: DO NOT</b> rename an interface or Netbeans drops random methods!<br>
 * Fix-em and Delete {@code C:\Users\User\AppData\Local\NetBeans\Cache\8.2\*.*}
 * <p>
 * <b>WARN: DO NOT</b> change Tech names: tech.name().startsWith("Whatever")
 * determines some facts, so atleast test the hell out of EVERYthing! A smart
 * bastard would work-out how to subtype Enums. Im not that smart, and Im here
 * to solve Sudokus, not Java. What I really want a simple base Tech, plus a
 * FishyTech, plus a ChainTech, plus... Sigh.
 * <p>
 * <b>WARN: DO NOT</b> re-order Techs to match LogicalSolverBuilder, or by
 * Difficulty, or whatever. The TechSelectDialog displays Techs in the same
 * order they are listed in Tech, hence its the primary consideration. This is
 * not ideal, it just is what it is. Ideally the TechSelectDialog would have
 * its own order (a field), which would require yet more maintenance. Sigh.
 * <p>
 * <b>WARN: CHECK</b> If you change any hints difficulty then check:<ul>
 *  <li>{@link diuf.sudoku.Tech} difficulties are contiguous and increasing
 *  <li>{@link diuf.sudoku.Difficulty} sane ranges, and ends still current,
 *   keeping in mind that hard Sudokus are ____ing mental to start with.
 *  <li>diuf.sudoku.solver.LogicalSolverBuilder order of wantedHinters close to
 *   Tech.difficulty so hinters run by difficulty increasing, hence produce the
 *   simplest possible solution to each Sudoku.
 * </ul>
 * <p>
 * <b>WARN: DO NOT</b> exceed 18 characters in a Tech name. My longest is 18
 * characters. Keep it that way. If you exceed 18 then batch log format and all
 * parsers thereof are your problem. This includes many tools, logFollow, and
 * logView... a bloody big mess. Soz. Sigh.
 * <p>
 * <b>NOTE:</b> Difficulty is not proportional, just increasing. Generating a
 * puzzle that contains a NestedPlus hint (IDKFA) is nearly impossible. It is
 * NOT 13 times harder than a NakedSingle, as you might reasonably infer from
 * there relative difficulties, its nearly ____ing impossible. Clear?
 */
public enum Tech {

	// Validators                                       W,W, (Wanted, Warning)
	  Analysis			(0, 0, LogicalAnalyser.class,	F,F, "VLDT: Logical Solve to report difficulty")
	, TooFewClues		(0, 0, TooFewClues.class,		F,F, "VLDT: Valid Sudokus have 17 clues")
	, TooFewValues		(0, 0, TooFewValues.class,		F,F, "VLDT: Valid Sudokus have atleast 8 values")
	, NotOneSolution	(0, 0, BruteForce.class,		F,F, "VLDT: BruteForce Solve ASAP")
	, MissingMaybes		(0, 0, MissingMaybes.class,		F,F, "VLDT: Every unset cell must have maybes")
	, DoubleValues		(0, 0, DoubleValues.class,		F,F, "VLDT: Each value occurs ONCE per region")
	, HomelessValues		(0, 0, HomelessValues.class,	F,F, "VLDT: Every unplaced value has a place in region")

	// Easy
	, LonelySingle		(10, 1, LonelySingle.class,		F,F, "WANK: The last empty cell in a region")
	, NakedSingle		(11, 1, NakedSingle.class,		T,F, "REQD: Cells with one remaining maybe")
	, HiddenSingle		(12, 1, HiddenSingle.class,		T,F, "REQD: The last place for value in region")

	// Medium
	, DirectNakedPair	(20, 2, NakedSetDirect.class,	F,F, "WANK: NakedPair that causes a single")
	, DirectHiddenPair	(21, 2, HiddenSetDirect.class,	F,F, "WANK: HiddenPair that causes a single")
	, DirectNakedTriple	(22, 3, NakedSetDirect.class,	F,F, "WANK: NakedTriple that causes a single")
	, DirectHiddenTriple(23, 3, HiddenSetDirect.class,	F,F, "WANK: HiddenTriple that causes a single")
	, Locking			(24, 1, Locking.class,			T,F, "WANT: Faster. Overlap of box and row/col")
	, LockingBasic		(25, 1, LockingGen.class,		F,F, "DROP: Slower. Overlap of box and row/col")

	// Hard
	, NakedPair			(30, 2, NakedPair.class,		T,F, "WANT: Two cells can only be two values")
	, HiddenPair		(31, 2, HiddenPair.class,		T,F, "WANT: Two cells are only places for two values")
	, NakedTriple		(32, 3, NakedTriple.class,		T,F, "WANT: Three cells can only be three values")
	, HiddenTriple		(33, 3, HiddenTriple.class,		T,F, "WANT: Three cells are only places for three values")

	// Fiendish
	, TwoStringKite		(40, 4, TwoStringKite.class,	T,F, "WANT: A box (kite) with two biplaced regions (strings)")
	, Swampfish			(41, 2, Fisherman.class,		T,F, "WANT: Aka X-Wing. Two bases are only places for value in covers")
	, W_Wing			(42, 4, WWing.class,			T,F, "WANT: Biplaced region with two bivalue cells")
	, XY_Wing			(43, 2, XYWing.class,			T,F, "WANT: Three bivalue cells that all see each other")
	, XYZ_Wing			(44, 3, XYWing.class,			T,F, "WANT: A trivalue cell sees two bivalue cells")
	, Skyscraper		(45, 4, Skyscraper.class,		T,F, "WANT: Swampfish get stoned too ya know")
	, EmptyRectangle		(46, 4, EmptyRectangle.class,	T,F, "WANT: a biline box links two biplaced lines")

	// Nasty
	, Swordfish			(50, 3, Fisherman.class,		T,F, "WANT: Three bases are only places for value in covers")
	, URT				(51, 0, URT.class,				T,F, "WANT: Unique RecTangles and loops")
	, NakedQuad			(52, 4, NakedQuad.class,		T,F, "WANT: Four cells can only be four values")
	, HiddenQuad		(53, 4, HiddenQuad.class,		F,F, "DROP: Rare (1). Four cells are only places for four values")
	, Jellyfish			(54, 4, Fisherman.class,		T,F, "KEEP: Rare (8). Four bases are only places for value in covers")
	, Coloring			(55, 0, Coloring.class,			T,F, "WANT: for the only MultiColoring")
	, XColoring			(56, 0, XColoring.class,		T,F, "WANT: Coloring+ hits where GEM misses")
	, Medusa3D			(57, 0, Medusa3D.class,			F,F, "DROP: Coloring++ is counter-productive with GEM")
	, GEM				(58, 0, GEM.class,				T,F, "WANT: Coloring+++ Graded Equivalence Marks")

	// Alsic (Almost Locked Sets)
	, BigWings			(60, 8, BigWings.class,			T,F, "WANT: S/T/U/V/WXYZ Wings. AlmostLockedSet + bivalue cell")
	, DeathBlossom		(65, 8, DeathBlossom.class,		T,F, "WANT: 2 or 3 ALSs (petals) and a bi/trivalued stem cell")
	, ALS_XZ			(66, 8, AlsXz.class,			T,F, "WANT: A Chain of 2 ALSs")
	, ALS_Wing			(67, 8, AlsWing.class,			T,F, "WANT: A Chain of 3 ALSs")
	, ALS_Chain			(68,13, AlsChain.class,			T,F, "WANT: A Chain of 4-or-more ALSs")
	, SueDeCoq			(69, 0, SueDeCoq.class,			T,F, "KEEP: Barely: Rare (15). Almost Almost Locked Sets")

	// Fisch                                            F,W,W (Fishy, Wanted, Warning)
	, FinnedSwampfish	(70, 2, FinnedFisherman.class,  T,F,F, "DROP: Bad value. 2 bases/covers with fins (extra places in bases)")
	, FrankenSwampfish	(71, 2, FrankenFisherman.class, T,F,T, "NOPE: Degenerate! 2 bases/covers with boxs as either")
	, MutantSwampfish	(72, 2, MutantFisherman.class,  T,F,T, "DROP: Bad value. 2 bases/covers with boxs as both")
	// swordfish
	, FinnedSwordfish	(73, 3, FinnedFisherman.class,  T,F,F, "DROP: Bad value. 3 bases/covers with fins")
	, FrankenSwordfish	(74, 3, FrankenFisherman.class, T,T,F, "KEEP: Slow (12 secs). 3 bases/covers with boxs as either")
	, MutantSwordfish	(75, 3, MutantFisherman.class,  T,F,F, "DROP: Slow (30 secs). 3 bases/covers with boxes as both. Slow (1 min)")
	// jellyfish are slow
	, FinnedJellyfish	(76, 4, FinnedFisherman.class,  T,F,F, "DROP: Slow (14 secs). 4 bases/covers with fins")
	, FrankenJellyfish	(77, 4, FrankenFisherman.class, T,F,F, "DROP: Slow (45 secs). 4 bases/covers with boxes as either")
	, MutantJellyfish	(78, 4, MutantFisherman.class,  T,F,T, "DROP: TOO SLOW (3 mins). 4 bases/covers with boxes as both")
	// Kraken sux! HACKED into submission!
	, KrakenSwampfish	(79, 2, KrakenFisherman.class,  T,F,F, "DROP: Slow (20 secs). 2 bases/cover with fins and static forcing chains")
	, KrakenSwordfish	(80, 3, KrakenFisherman.class,  T,F,T, "DROP: TOO SLOW (8 mins). 3 bases/cover with fins and chains")
	, KrakenJellyfish	(81, 4, KrakenFisherman.class,  T,F,T, "DROP: TOO SLOW (30 mins). 4 bases/cover with fins and chains")

	// Ligature                                         W,W, (Wanted, Warning)
	, AlignedPair		(90, 2, AlignedExclusion.class, F,F, "DROP: None! 2 cells aligned around two 2valued cells")
	, AlignedTriple		(91, 3, AlignedExclusion.class, F,F, "DROP: Rare (7). 3 cells aligned around two 2..3valued cells")
	, AlignedQuad		(92, 4, AlignedExclusion.class, F,F, "DROP: Rare (3). 4 cells aligned around two 2..4valued cells")
	, AlignedPent		(93, 5, AlignedExclusion.class, F,F, "DROP: None! 5 cells aligned around two 2..5valued cells")
	, AlignedHex		(94, 6, AlignedExclusion.class, F,F, "DROP: None! 6 cells aligned around two 2..6valued cells")
	, AlignedSept		(95, 7, AlignedExclusion.class, F,T, "DROP: Slow (1 min) and Rare (5). 7 cells aligned around two 2..7valued cells")
	, AlignedOct			(96, 8, AlignedExclusion.class, F,T, "DROP: Slow (1 min) and Rare (7). 8 cells aligned around two 2..8valued cells")
	, AlignedNona		(97, 9, AlignedExclusion.class, F,T, "DROP: Slow (45 secs) and Rare (10). 9 cells aligned around two cells")
	, AlignedDec			(98,10, AlignedExclusion.class, F,T, "DROP: Slow (30 secs) and Rare (5). 10 cells aligned around two cells")

	// Diabolical										M,D,N,W,W  (Multiple, Dynamic, Nishio, Wanted, Warning)
	, TableChain		(100, 0, TableChains.class,		F,F,F,T,F, "WANT: Static Contradiction and Reduction using the tables")
	, Abduction			(110, 0, Abduction.class,		F,F,F,F,F, "DROP: Abduction Effs himself. Slower but more accurate")
	, UnaryChain		(120, 0, ChainerUnary.class,	F,F,F,F,F, "DROP: Static Unary Chains and Bidirectional Cycles")
	, StaticChain		(130, 0, ChainerStatic.class,	T,F,F,T,F, "WANT: Static Multiple Forcing Chains")
	, NishioChain		(140, 0, ChainerNishio.class,	F,T,T,F,F, "DROP: Dynamic X-Chain Contradictions (Dynamic Fishies)")
	, DynamicChain		(150, 0, ChainerDynamic.class,	T,T,F,T,F, "WANT: Dynamic Multiple Forcing Chains")

	// IDKFA
	, DynamicPlus		(200, 1, ChainerDynamic.class,	T,T,F,T,F, "WANT: DynamicChain + Locking, HiddenPair, NakedPair, and Swampfish")
	, NestedUnary		(300, 2, ChainerDynamic.class,	T,T,F,T,F, "KEEP: DynamicPlus + UnaryChain. Catch-all!")
	, NestedStatic		(400, 3, ChainerDynamic.class,	T,T,F,F,T, "DROP: SLOW! DynamicPlus + UnaryChain + StaticChain. Overkill!")
	, NestedDynamic		(500, 4, ChainerDynamic.class,	T,T,F,F,T, "DROP: SLOW! DynamicPlus + DynamicChain. Double Overkill!")
	, NestedPlus			(600, 5, ChainerDynamic.class,	T,T,F,F,T, "DROP: SLOW! DynamicPlus + DynamicChain + DynamicPlus. BUGS! Triple Overkill!")
	;

	/**
	 * Returns SSV (Space Separated Values) String of the wanted slowHinters.
	 * slowHinters are those that make a Java 1.8 top1465 batch exceed three
	 * minutes overall runTime, making the GUI feel "not snappy enough".
	 * <p>
	 * Note that AlignedExclusion (permanently hacked) is no longer "slow", so
	 * it gets primingSolves, which bails-out if a puzzle exceeds 3 secs.
	 *
	 * @return example "MutantJellyfish KrakenSwordfish"
	 */
	public static String wantedSlowHinters() { // these times are all in Java 1.8 (Java 17 is slower)
		final Tech[] s__t = {	//           time(ns) cals       ns/call elim      time/elim  runTime
			  FrankenJellyfish	//    114,036,230,100 4540    25,118,112  416    274,125,553 (  03:11)
			, MutantJellyfish	//    688,967,959,300 4546   151,554,764  422  1,632,625,495 (  12:27)
			, KrakenSwordfish	//  1,072,209,471,000 4551   235,598,653 2976	 360,285,440 (  18:45)
			, KrakenJellyfish	// 13,013,694,378,200 4607 2,824,765,439 3052  4,263,988,983 (3:37:52)
		};
		final String sep = SP;
		final StringBuilder sb = SB(s__t.length<<5); // * 32
		for ( Tech t : s__t )
			if ( CFG.getBoolean(t.name()) )
				sb.append(t.name()).append(sep);
		return sb.length()>0 ? sb.substring(0, sb.length()-sep.length()) : "";
	}

	/**
	 * Transcribe a {@code Tech[]} into a new {@code EnumSet<Tech>}, to
	 * provide a convenient way to hardcode a Set of Techs, in an array.
	 * <p>
	 * NOTE: varargs array parameters are rare in SE coz they have a small cost
	 * per invocation, thats fast enough until called a million times. Eels!
	 *
	 * @param techs to transcribe
	 * @return a new {@code EnumSet<Tech>} containing the given $techs
	 */
	public static EnumSet<Tech> enumSet(Tech... techs) {
		final EnumSet<Tech> result = EnumSet.noneOf(Tech.class);
		for ( Tech tech : techs )
			result.add(tech);
		return result;
	}

	/**
	 * Transcribe a {@code EnumSet<Tech>} into a new {@code Tech[]}. This is
	 * used to get a {@link #where(IFilter<Tech>) } result as an array.
	 * <p>
	 * This method is no longer used, but I retain it anyway, coz experience
	 * tells me ones ALWAYS wants an array of whatever.
	 *
	 * @param techs to transcribe
	 * @return a new {@code Tech[]} containing the given $techs
	 */
	public static Tech[] toArray(EnumSet<Tech> techs) {
		final Tech[] result = new Tech[techs.size()];
		int i = 0;
		for ( Tech tech : techs )
			result[i++] = tech;
		return result;
	}

	/**
	 * Returns a new {@code EnumSet<Tech>} of given $techs
	 * which are accepted by the given $filter.
	 *
	 * @param filter an implementation of {@link IFilter<Tech>} whose accept
	 *  method determines which members of $techs are added to the result set
	 * @param techs the set to select from
	 * @return a {@code EnumSet<Tech>} of matching $techs
	*/
	public static EnumSet<Tech> where(final IFilter<Tech> filter, final EnumSet<Tech> techs) {
		final EnumSet<Tech> result = EnumSet.noneOf(Tech.class);
		for ( final Tech tech : techs )
			if ( filter.accept(tech) )
				result.add(tech);
		return result;
	}

	/**
	 * Returns a new {@code EnumSet<Tech>} of all Techs
	 * which are accepted by IFilter $f.
	 *
	 * @param filter an implementation of {@link IFilter<Tech>} whose accept
	 *  method determines which members of $techs are added to the result set
	 * @return a {@code EnumSet<Tech>} of accepted $techs
	 */
	public static EnumSet<Tech> where(final IFilter<Tech> filter) {
		return where(filter, EnumSet.allOf(Tech.class));
	}

	/**
	 * Returns a new {@code EnumSet<Tech>} of all "hinters",
	 * ie Techs with {@code difficulty > 0}.
	 * <p>
	 * Non-hinter Techs (validators) are identified by difficulty==0.
	 *
	 * @return a new {@code EnumSet<Tech>} of all hinter Techs
	 */
	public static EnumSet<Tech> hinters() {
		return where((t) -> t.difficulty > 0);
	}

	/**
	 * Get Techs from min (inclusive) to less than max (exclusive).
	 *
	 * @param min inclusive
	 * @param max exclusive
	 * @return Techs in a {@link Difficulty}
	 */
	public static EnumSet<Tech> byDifficulty(final int min, final int max) {
		return where((t)->t.difficulty>=min && t.difficulty<max);
	}

	/**
	 * Returns CSV of tech names.
	 *
	 * @param techs to get the names of
	 * @return a Comma Separated Values String of the names of these $techs
	 */
	public static String names(final Collection<Tech> techs) {
		final StringBuilder sb = SB(techs.size()<<5); // * 32
		for ( Tech tech : techs )
			sb.append(CSP).append(tech.name());
		return sb.substring(2, sb.length());
	}

	/**
	 * Wordonate: Translate $techName into words by:
	 * <pre>
	 * inserting a space before each capital,
	 *       and replacing underscore with a space;
	 * but consecutive capitals remain unchanged,
	 *     as do those following an underscore.
	 *
	 * So, words are split-up:
	 * EmptyRectangle -> Empty Rectangle
	 *
	 * And acronymic underscores are enspaced:
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
			if ( p!=0 && p!='_' && !isUpperCase(p) && isUpperCase(c) )
				dst[i++] = ' ';
			if ( c == '_' )
				dst[i++] = ' ';
			else
				dst[i++] = c;
			p = c;
		}
		return new String(dst, 0, i);
	}

	// ---------------------------- instance stuff ----------------------------

	/**
	 * Is this technique wanted by default.
	 * <p>
	 * True for most Techs. False for:<ul>
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
	 * <p>If you change any hints difficulty then you will need to look at:<ul>
	 *  <li>{@link diuf.sudoku.Tech}
	 *  <li>{@link diuf.sudoku.Difficulty}
	 *  <li>and {@code getDifficulty()} comment in {@link diuf.sudoku.solver.AHint}
	 * </ul>
	 */
	public final int difficulty;

	/**
	 * Some hinters implement multiple Techs of varying size, degree is that
	 * size: typically the number of cells or regions to be examined.
	 * <pre>
	 * Examples:
	 *
	 * NakedSets implements: NakedPair=2, NakedTriple=3, NakedQuad=4
	 * where degree is the number of cells in the set.
	 *
	 * Fisherman implements: Swampfish=2, Swordfish=3, Jellyfish=4
	 * where degree is the number of base (and cover) regions.
	 *
	 * ALS_Chains=26 means that upto 26 ALSs may be chained together.
	 *
	 * DynamicPlus=1   means DynamicChain + FourQuickFoxes
	 * NestedUnary=2   means DC + FQF + UnaryChainer
	 * NestedMulti=3   means DC + FQF + UnaryChainer + MultiChainer
	 * NestedDynamic=4 means DC + FQF + DynamicChainer
	 * NestedPlus=5    means DC + FQF + DynamicChainer + DynamicPlus
	 * </pre>
	 */
	public final int degree;

	/** true if this Tech tastes like chicken */
	public final boolean isFishy;
	/** true if this Tech Directly sets a cells value */
	public final boolean isDirect;
	/** true if this is an Aligned Tech */
	public final boolean isAligned;

	/** a hint for the user regarding selection of this Tech */
	public final String tip;
	/** is the tip actually a warning? */
	public final boolean isWarning;

	// these fields are only used for *Chain, DynamicPlus, and Nested*
	// they are false for non-Chainer Techs.
	/** is an XY Forcing Chains hinter (used the Chainer constructor) */
	public final boolean isChainer;
	/** does hinter own other chaining hinter/s (hierarchical slavery) */
	public final boolean isNesting;
	/** static search cell.size>2 and region.numPlaces[v]>2 */
	public final boolean isMultiple;
	/** combine the effects of previous assumptions, ie erase each off */
	public final boolean isDynamic;
	/** Dynamic assumption causes contradiction: has both effect && !effect */
	public final boolean isNishio;

	// Currently EVERY Tech has a default implementation, so null would be new
	// territory, ie: s__t will break.
	/** the name of the class which implements this Tech, where known */
	public final String className;

	/** for the text method */
	private String text;

	/**
	 * The actual all-args Constructor. All of the below short-cut constructors
	 * come through here. They just provide various default attributes.
	 * <p>
	 * Note that I am no longer ALL args. I don't do the chainers!
	 *
	 * @param difficulty base difficulty for hints of this type
	 * @param degree the number of things (cells or regions) in this pattern,
	 *  or 0 meaning none
	 * @param clazz the class which implements this Tech, or null if there is
	 *  no default implementation. Be warned that all Techs, to date, have a
	 *  default implementation so use null, but here be dragons
	 * @param isFishy does this Tech taste like chicken
	 * @param defaultWanted should this Tech be wanted by default. This matters
	 *  the first time SudokuExplainer runs, thereafter "wanted" is determined
	 *  by the users Config. This field exists coz some hinters (like large
	 *  A*E and Kraken/MutantSwampFish) are too slow to be allowed and yet they
	 *  are allowable, but are unwanted by default, to avoid despoiling first
	 *  user-experiences by taking all day to achieve not very much. Sigh.
	 * @param isWarning is tip actually a warning? ie should TechSelectDialog
	 *  annoy the user with a message-box when this Tech is selected
	 * @param tip helper text in the TechSelectDialog
	 */
	private Tech(final int difficulty
		, final int degree
		, final Class<? extends IHinter> clazz
		, final boolean isFishy
		, final boolean defaultWanted
		, final boolean isWarning
		, final String tip
	) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isFishy = isFishy;
		this.isDirect = name().startsWith("Direct");
		this.isAligned = name().startsWith("Aligned");
		this.className = clazz==null ? null : clazz.getName();
		this.defaultWanted = defaultWanted;

		this.isChainer = false;
		this.isNesting = false;
		this.isMultiple = false;
		this.isDynamic = false;
		this.isNishio = false;

		this.tip = tip;
		this.isWarning = isWarning;
	}

	/**
	 * Non-fishy constructor.
	 */
	private Tech(final int difficulty, final int degree
	, final Class<? extends IHinter> clazz, final boolean defaultWanted
	, final boolean warning, final String tip
	) {
		this(difficulty, degree, clazz, false, defaultWanted, warning, tip);
	}

	/**
	 * Basic constructor (every Tech now has an implementing clazz).
	 */
	private Tech(final int difficulty, final int degree
	, final Class<? extends IHinter> clazz
	) {
		this(difficulty, degree, clazz, true, false, null);
	}

	/**
	 * Basic + clazz and defaultWanted constructor.
	 */
	private Tech(final int difficulty, final int degree
	, final Class<? extends IHinter> clazz, final boolean defaultWanted
	) {
		this(difficulty, degree, clazz, defaultWanted, false, null);
	}

	/**
	 * The Chainer constructor (clazz is mandatory not null) with isWarning.
	 * <p>
	 * I exist separately from the above all-args constructor. I do Chainers.
	 * I don't do Fishy, or Direct, or Aligned. They are disjunct sets.
	 */
	private Tech(final int difficulty, final int degree
	, final Class<? extends IHinter> clazz, final boolean isMultiple
	, final boolean isDynamic, final boolean isNishio
	, final boolean defaultWanted, final boolean isWarning, final String tip
	) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.isFishy = false;
		this.isDirect = false;
		this.isAligned = false;
		this.className = clazz.getName();
		this.defaultWanted = defaultWanted;

		this.isChainer = true;
		this.isNesting = name().startsWith("Nested");
		this.isMultiple = isMultiple;
		this.isDynamic = isDynamic;
		this.isNishio = isNishio;

		this.isWarning = isWarning;
		this.tip = tip;
	}

	/**
	 * Returns difficulty + wordonate(name()).
	 *
	 * @return the JCheckBox.text for this Tech.
	 */
	public String text() {
		if ( text == null )
			text = ""+difficulty+SP+wordonate(name());
		return text;
	}

	/**
	 * Is this Tech in {@link Difficulty#Alsic}?
	 * <p>
	 * Ie, is this Tech an AAlsHinter tech?
	 *
	 * @return is this Tech in {BigWings, DeathBlossom, ALS_XZ, ALS_Wing,
	 *  ALS_Chain}
	 */
	@SuppressWarnings("fallthrough")
	public boolean isAlsic() {
		switch ( this ) {
			case BigWings:
			case DeathBlossom:
			case ALS_XZ:
			case ALS_Wing:
			case ALS_Chain:
				return true;
			default:
				return false;
		}
	}

	// nb: It's faster to access the name attribute directly.
	@Override
	public String toString() {
		return name();
	}

}
