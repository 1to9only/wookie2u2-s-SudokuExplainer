/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The Tech(nique) enum of Sudoku Solving Techniques.
 * <p>
 * It's called Tech because the word Technique is too long and has three F's
 * a silent Q. All reasonable people hate Q's, and there smelly U-ism. It's a
 * well known fact that most people are totally unaware of there innate Queue-
 * Biff-Phobia. Some people are even allergic to quinces. Need I say more.
 * <p>
 * The constructor of each subtype of AHinter passes down a Tech to identify
 * which Sudoku solving technique is being implemented, his degree, and his
 * base difficulty (which is far more predictable than maxDifficulty).
 * <p>
 * Most AHinters implement one Tech, but a few (especially Chains) implement
 * multiple Techs, so the Solver now prints both hinter-names and hint-types,
 * so you can find the AHinter.
 * <p>
 * This is pretty confusing to start with, but it works, so please don't
 *  "fix" it until you really understand it (aka when you don't want to).
 * <p>
 * <b>WARNING:</b> The TechSelectDialog lists these enums in the order in
 *  which they appear in this file. <b>DO NOT</b> re-order them just to make it
 *  easier to organise your Difficulties, as I have just done. Sigh.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then please look at:
 * <ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#configureHinters} to keep the
 *   order of wantedHinters same as (or near) that of Tech difficulty; so that
 *   hinters are run in increasing difficulty, to produce the simplest solution
 *   for each puzzle.
 *  <li>{@link diuf.sudoku.Tech} // for the actual difficulties
 *  <li>{@link diuf.sudoku.Difficulty} // double check the ranges
 * </ul>
 * <p>
 * <b>WARNING:</b> If you rename a Tech add it to LogicalSolverTester.aliases!
 */
public enum Tech {

	// for the LogicalAnalyser
	Solution(0.0, 0, "The Solution")

	// puzzleValidators
	, TooFewClues		(0.0, 0, "Too Few Clues")
	, TooFewValues		(0.0, 0, "Too Few Values")
	, SingleSolutions	(0.0, 0, "Single Solution")

	// gridValidators
	, NoMissingMaybes	(0.0, 0, "No Missing Maybes")
	, NoDoubleValues		(0.0, 0, "No Double Values")
	, NoHomelessValues	(0.0, 0, "No Homeless Values")

// Easy
	// directs
	, LonelySingle		(1.0, 1, "Lonely Single")
	, NakedSingle		(1.1, 1, "Naked Single")
	, HiddenSingle		(1.2, 1, "Hidden Single")

// Medium
	// NB: constructor does name().startsWith("Direct")
	, Locking			(2.1, 1, "Locking", "OR Locking Generalised") // Pointing=2.1 Claiming=2.2
	, LockingGeneralised(2.1, 1, "Locking Generalised", "OR Locking") // 2.1 for both
	, DirectNakedPair	(2.3, 2, "Direct Naked Pair")
	, DirectHiddenPair	(2.4, 2, "Direct Hidden Pair")
	, DirectNakedTriple	(2.5, 3, "Direct Naked Triple")
	, DirectHiddenTriple	(2.6, 3, "Direct Hidden Triple") // longest nom is 20 characters
// Hard
	// indirects
	, NakedPair			(3.0, 2, "Naked Pair")
	, HiddenPair		(3.1, 2, "Hidden Pair")
	, NakedTriple		(3.2, 3, "Naked Triple")
	, HiddenTriple		(3.3, 3, "Hidden Triple")
	, TwoStringKite		(3.4, 4, "Two String Kite")
	, Swampfish			(3.5, 2, "Swampfish") // aka X-Wing: I call them Swampfish coz they're Fish, not Wings! Luke, Yoda, swamp.

// Fiendish
	, XY_Wing			(3.6, 2, "XY-Wing")
	, XYZ_Wing			(3.7, 3, "XYZ-Wing")
	, W_Wing			(3.8, 4, "W-Wing")
	, Swordfish			(3.9, 3, "Swordfish")
	, Skyscraper		(4.0, 4, "Skyscraper")
	, EmptyRectangle		(4.1, 4, "Empty Rectangle")
	, Jellyfish			(4.2, 4, "Jellyfish")
	// FYI: Don't try larger Fish, I did, and they find nothing.
	//      They're all degenerate (comprised of simpler hints)

// Nightmare
	// heavies
	, WXYZ_Wing			(4.21, 4, "WXYZ-Wing")			// limited-ALSs
	, VWXYZ_Wing		(4.22, 4, "VWXYZ-Wing")			// limited-ALSs
	, UVWXYZ_Wing		(4.23, 6, "UVWXYZ-Wing")		// limited-ALSs
	, FinnedSwampfish	(4.30, 2, "Finned Swampfish")	// Finned & Sashimi Fish (fast)
	, FinnedSwordfish	(4.31, 3, "Finned Swordfish")
	, FinnedJellyfish	(4.32, 4, "Finned Jellyfish")
	, Coloring			(4.40, 2, "Coloring", "or BUG (below)")	// Coloring (BUG++)
	, TUVWXYZ_Wing		(4.24, 7, "TUVWXYZ-Wing")		// limited-ALSs
	, SueDeCoq			(4.48, 0, "Sue De Coq")
	, DeathBlossom		(4.49, 0, "Death Blossom", "produces invalid hints!")
	, ALS_XZ			(4.50, 0, "ALS-XZ")				// ALSs
	, ALS_Wing			(4.51, 0, "ALS-Wing")
	, ALS_Chain			(4.52, 0, "ALS-Chain")
	, FrankenSwampfish	(4.60, 2, "Franken Swampfish")
	, FrankenSwordfish	(4.61, 3, "Franken Swordfish")
	, FrankenJellyfish	(4.62, 4, "Franken Jellyfish")
// 1,764 seconds for 20 hints is just too slow to be allowed. Mutants are dead.
// Having said that Mutant Swampfish was fast enough, for 0 hints. sigh.
	, MutantSwampfish	(4.70, 2, "Mutant Swampfish", "top1465 NONE")		// SLOW
	, MutantSwordfish	(4.71, 3, "Mutant Swordfish", "top1465 45 seconds")	// VERY SLOW
	, MutantJellyfish	(4.72, 4, "Mutant Jellyfish", "top1465 5 minutes")	// SUPER SLOW
	, NakedQuad			(5.0, 4, "Naked Quad")
	, HiddenQuad		(5.1, 4, "Hidden Quad", "top1465 NONE")
	, NakedPent			(5.2, 5, "Naked Pent", "top1465 NONE")
	, HiddenPent			(5.3, 5, "Hidden Pent", "top1465 NONE")
	, URT				(5.4, 0, "Unique Rectangle")
	, KrakenSwampfish	(5.80, 2, "Kraken Swampfish")	// SLOW
	, KrakenSwordfish	(5.81, 3, "Kraken Swordfish", "top1465 2 minutes")	// SLOW
	, KrakenJellyfish	(5.82, 4, "Kraken Jellyfish", "top1465 5 minutes")	// VERY SLOW

// Diabolical
	, BUG				(6.0, 0, "Bi-Uni Grave", "or Coloring (above)") // Type 1=6.0, 2=6.1, 3={2=6.2, 3=6.3, 4=6.4}, 4=6.0

	, AlignedPair		(6.1, 2, "Aligned Pair")
	, AlignedTriple		(6.2, 3, "Aligned Triple")
	, AlignedQuad		(6.3, 4, "Aligned Quad")
	, AlignedPent		(6.4, 5, "Aligned Pent", "top1465 01:16 m:s correct")
	, AlignedHex		(6.5, 6, "Aligned Hex", "top1465 03:28 m:s correct")
	, AlignedSept		(6.6, 7, "Aligned Sept", "top1465 06:17 m:s correct")
	, AlignedOct			(6.7, 8, "Aligned Oct", "top1465 19 minutes correct")
	, AlignedNona		(6.8, 9, "Aligned Nona", "top1465 3 hours correct")
	, AlignedDec			(6.9,10, "Aligned Dec", "top1465 6 hours correct")

	// chains           level                      Multi,Dynam,Nishi
	, UnaryChain		(7.0, 0, "Unary Chain",    false,false,false)
	, NishioChain		(7.5, 0, "Nishio Chain",   false,true ,true )
	, MultipleChain		(8.0, 0, "Multiple Chain", true ,false,false)
	, DynamicChain		(8.5, 0, "Dynamic Chain",  true ,true ,false)

// IDKFA
	, DynamicPlus		(9.0, 1, "Dynamic Plus",   true ,true ,false)

	// nested
	// NB: constructor does name().startsWith("Nested")
	, NestedUnary		( 9.5, 2, "Nested Unary",    true ,true ,false)
	, NestedMultiple		(10.0, 3, "Nested Multiple", true ,true ,false)
	, NestedDynamic		(10.5, 4, "Nested Dynamic",  true ,true ,false)
	, NestedPlus			(11.0, 5, "Nested Plus",     true ,true ,false)
	;

	public static EnumSet<Tech> forNames(Set<String> namesSet) {
		EnumSet<Tech> techs = EnumSet.allOf(Tech.class);
		for ( Iterator<Tech>it=techs.iterator(); it.hasNext();  )
			if ( !namesSet.contains(it.next().name()) )
				it.remove();
		return techs;
	}

//not used
//	public static String[] nomsArray() {
//		EnumSet<Tech> techs = EnumSet.allOf(Tech.class);
//		String[] noms = new String[techs.size()];
//		int cnt = 0;
//		for ( Tech t : techs )
//			noms[cnt++] = t.nom;
//		return noms;
//	}

	/**
	 * <p>If you change any hints difficulty then you'll need to update<ul>
	 *  <li>{@link diuf.sudoku.Tech}
	 *  <li>{@link diuf.sudoku.Difficulty}
	 *  <li>and {@code getDifficulty()} comment in {@link diuf.sudoku.solver.AHint}
	 * </ul>
	 */
	public final double difficulty;
	public final int degree;

	public String nom; // nom is frog for name, which is already taken.
	public final boolean isDirect; // true if this is a Direct rule.
	public final boolean isAligned; // true if this is an Aligned rule.
	public final String tip; // nom is frog for name, which is already taken.

	// these fields are only used for *Chain, DynamicPlues, and Nested*
	// they are all allways false for the "normal" (non-Chainer) Techniques.
	public final boolean isChainer; // true if we used the Chainer constructor.
	public final boolean isNested; // true if name().startsWith("Nested")
	public final boolean isMultiple; // search cell.maybes.size>2 && region.idxsOf[v].size>2
	public final boolean isDynamic; // combine effects of previous calculations
	public final boolean isNishio; // an assumption has both effect && !effect

	private Tech(double difficulty, int degree, String nom, String tip) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.nom = nom;
		this.isDirect = name().startsWith("Direct");
		this.isAligned = name().startsWith("Aligned");

		this.isChainer = false;
		this.isNested = false;
		this.isMultiple = false;
		this.isDynamic = false;
		this.isNishio = false;
		this.tip = tip;
	}

	private Tech(double difficulty, int degree, String nom) {
		this(difficulty, degree, nom, null); // tool tip = null, meaning none
	}

	private Tech(double difficulty, int degree, String nom
			, boolean isMultiple, boolean isDynamic, boolean isNishio) {
		this.difficulty = difficulty;
		this.degree = degree;
		this.nom = nom;
		this.isDirect = false;
		this.isAligned = false;

		this.isChainer = true;
		this.isNested = name().startsWith("Nested");
		this.isMultiple = isMultiple;
		this.isDynamic = isDynamic;
		this.isNishio = isNishio;
		this.tip = null;
	}

	public static Tech Solution(String nom) {
		Solution.nom = nom;
		return Solution;
	}

	// DO NOT USE: Just access the name attribute directly.
	@Override
	public String toString() {
		return nom;
	}
}