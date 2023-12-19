/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import static diuf.sudoku.Constants.T;
import static diuf.sudoku.Constants.F;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Regions.BOX_MASK;
import static diuf.sudoku.Regions.COL_MASK;
import static diuf.sudoku.Regions.ROW_MASK;

/**
 * The type of a complex fish.
 *
 * @author Keith Corlett 2023 May 4
 */
public enum FishType {

	// Nope, not the minimum code solution.

	  SWAMPFISH("Swampfish", 2, T, F, F, F, F, F)
	, SWORDFISH("Swordfish", 3, T, F, F, F, F, F)
	, JELLYFISH("Jellyfish", 4, T, F, F, F, F, F)
	, SQUIRMBAG("Squirmbag", 5, T, F, F, F, F, F)
	, WHALEBONE("Whalebone", 6, T, F, F, F, F, F)
	, LEVIATHAN("Leviathan", 7, T, F, F, F, F, F)

	, FINNED_SWAMPFISH("Finned Swampfish", 2, F, T, F, F, F, F)
	, FINNED_SWORDFISH("Finned Swordfish", 3, F, T, F, F, F, F)
	, FINNED_JELLYFISH("Finned Jellyfish", 4, F, T, F, F, F, F)
	, FINNED_SQUIRMBAG("Finned Squirmbag", 5, F, T, F, F, F, F)
	, FINNED_WHALEBONE("Finned Whalebone", 6, F, T, F, F, F, F)
	, FINNED_LEVIATHAN("Finned Leviathan", 7, F, T, F, F, F, F)

	, SASHIMI_SWAMPFISH("Sashimi Swampfish", 2, F, F, T, F, F, F)
	, SASHIMI_SWORDFISH("Sashimi Swordfish", 3, F, F, T, F, F, F)
	, SASHIMI_JELLYFISH("Sashimi Jellyfish", 4, F, F, T, F, F, F)
	, SASHIMI_SQUIRMBAG("Sashimi Squirmbag", 5, F, F, T, F, F, F)
	, SASHIMI_WHALEBONE("Sashimi Whalebone", 6, F, F, T, F, F, F)
	, SASHIMI_LEVIATHAN("Sashimi Leviathan", 7, F, F, T, F, F, F)

	, FRANKEN_SWAMPFISH("Franken Swampfish", 2, F, F, F, T, F, F)
	, FRANKEN_SWORDFISH("Franken Swordfish", 3, F, F, F, T, F, F)
	, FRANKEN_JELLYFISH("Franken Jellyfish", 4, F, F, F, T, F, F)
	, FRANKEN_SQUIRMBAG("Franken Squirmbag", 5, F, F, F, T, F, F)
	, FRANKEN_WHALEBONE("Franken Whalebone", 6, F, F, F, T, F, F)
	, FRANKEN_LEVIATHAN("Franken Leviathan", 7, F, F, F, T, F, F)

	, FINNED_FRANKEN_SWAMPFISH("Finned Franken Swampfish", 2, F, T, F, T, F, F)
	, FINNED_FRANKEN_SWORDFISH("Finned Franken Swordfish", 3, F, T, F, T, F, F)
	, FINNED_FRANKEN_JELLYFISH("Finned Franken Jellyfish", 4, F, T, F, T, F, F)
	, FINNED_FRANKEN_SQUIRMBAG("Finned Franken Squirmbag", 5, F, T, F, T, F, F)
	, FINNED_FRANKEN_WHALEBONE("Finned Franken Whalebone", 6, F, T, F, T, F, F)
	, FINNED_FRANKEN_LEVIATHAN("Finned Franken Leviathan", 7, F, T, F, T, F, F)

	, SASHIMI_FRANKEN_SWAMPFISH("Sashimi Franken Swampfish", 2, F, F, T, T, F, F)
	, SASHIMI_FRANKEN_SWORDFISH("Sashimi Franken Swordfish", 3, F, F, T, T, F, F)
	, SASHIMI_FRANKEN_JELLYFISH("Sashimi Franken Jellyfish", 4, F, F, T, T, F, F)
	, SASHIMI_FRANKEN_SQUIRMBAG("Sashimi Franken Squirmbag", 5, F, F, T, T, F, F)
	, SASHIMI_FRANKEN_WHALEBONE("Sashimi Franken Whalebone", 6, F, F, T, T, F, F)
	, SASHIMI_FRANKEN_LEVIATHAN("Sashimi Franken Leviathan", 7, F, F, T, T, F, F)

	, MUTANT_SWAMPFISH("Mutant Swampfish", 2, F, F, F, F, T, F)
	, MUTANT_SWORDFISH("Mutant Swordfish", 3, F, F, F, F, T, F)
	, MUTANT_JELLYFISH("Mutant Jellyfish", 4, F, F, F, F, T, F)
	, MUTANT_SQUIRMBAG("Mutant Squirmbag", 5, F, F, F, F, T, F)
	, MUTANT_WHALEBONE("Mutant Whalebone", 6, F, F, F, F, T, F)
	, MUTANT_LEVIATHAN("Mutant Leviathan", 7, F, F, F, F, T, F)

	, FINNED_MUTANT_SWAMPFISH("Finned Mutant Swampfish", 2, F, T, F, F, T, F)
	, FINNED_MUTANT_SWORDFISH("Finned Mutant Swordfish", 3, F, T, F, F, T, F)
	, FINNED_MUTANT_JELLYFISH("Finned Mutant Jellyfish", 4, F, T, F, F, T, F)
	, FINNED_MUTANT_SQUIRMBAG("Finned Mutant Squirmbag", 5, F, T, F, F, T, F)
	, FINNED_MUTANT_WHALEBONE("Finned Mutant Whalebone", 6, F, T, F, F, T, F)
	, FINNED_MUTANT_LEVIATHAN("Finned Mutant Leviathan", 7, F, T, F, F, T, F)

	, SASHIMI_MUTANT_SWAMPFISH("Sashimi Mutant Swampfish", 2, F, F, T, F, T, F)
	, SASHIMI_MUTANT_SWORDFISH("Sashimi Mutant Swordfish", 3, F, F, T, F, T, F)
	, SASHIMI_MUTANT_JELLYFISH("Sashimi Mutant Jellyfish", 4, F, F, T, F, T, F)
	, SASHIMI_MUTANT_SQUIRMBAG("Sashimi Mutant Squirmbag", 5, F, F, T, F, T, F)
	, SASHIMI_MUTANT_WHALEBONE("Sashimi Mutant Whalebone", 6, F, F, T, F, T, F)
	, SASHIMI_MUTANT_LEVIATHAN("Sashimi Mutant Leviathan", 7, F, F, T, F, T, F)

	, KRAKEN_SWAMPFISH("Kraken Swampfish", 2, F, T, F, F, T, T)
	, KRAKEN_SWORDFISH("Kraken Swordfish", 3, F, T, F, F, T, T)
	, KRAKEN_JELLYFISH("Kraken Jellyfish", 4, F, T, F, F, T, T)
	, KRAKEN_SQUIRMBAG("Kraken Squirmbag", 5, F, T, F, F, T, T)
	, KRAKEN_WHALEBONE("Kraken Whalebone", 6, F, T, F, F, T, T)
	, KRAKEN_LEVIATHAN("Kraken Leviathan", 7, F, T, F, F, T, T)
	;

	/** unfinned basic fish */
	public static final FishType[] BASIC = {
			  SWAMPFISH
			, SWORDFISH
			, JELLYFISH
			, SQUIRMBAG
			, WHALEBONE
			, LEVIATHAN
	};

	/** finned fish */
	public static final FishType[] FINNED = {
			  FINNED_SWAMPFISH
			, FINNED_SWORDFISH
			, FINNED_JELLYFISH
			, FINNED_SQUIRMBAG
			, FINNED_WHALEBONE
			, FINNED_LEVIATHAN
	};

	/** sashimi fish */
	public static final FishType[] SASHIMI = {
			  SASHIMI_SWAMPFISH
			, SASHIMI_SWORDFISH
			, SASHIMI_JELLYFISH
			, SASHIMI_SQUIRMBAG
			, SASHIMI_WHALEBONE
			, SASHIMI_LEVIATHAN
	};

	/** franken fish */
	public static final FishType[] FRANKEN = {
			  FRANKEN_SWAMPFISH
			, FRANKEN_SWORDFISH
			, FRANKEN_JELLYFISH
			, FRANKEN_SQUIRMBAG
			, FRANKEN_WHALEBONE
			, FRANKEN_LEVIATHAN
	};

	/** finned franken fish */
	public static final FishType[] FINNED_FRANKEN = {
			  FINNED_FRANKEN_SWAMPFISH
			, FINNED_FRANKEN_SWORDFISH
			, FINNED_FRANKEN_JELLYFISH
			, FINNED_FRANKEN_SQUIRMBAG
			, FINNED_FRANKEN_WHALEBONE
			, FINNED_FRANKEN_LEVIATHAN
	};

	/** sashimi franken fish */
	public static final FishType[] SASHIMI_FRANKEN = {
			  SASHIMI_FRANKEN_SWAMPFISH
			, SASHIMI_FRANKEN_SWORDFISH
			, SASHIMI_FRANKEN_JELLYFISH
			, SASHIMI_FRANKEN_SQUIRMBAG
			, SASHIMI_FRANKEN_WHALEBONE
			, SASHIMI_FRANKEN_LEVIATHAN
	};

	/** mutant fish */
	public static final FishType[] MUTANT = {
			  MUTANT_SWAMPFISH
			, MUTANT_SWORDFISH
			, MUTANT_JELLYFISH
			, MUTANT_SQUIRMBAG
			, MUTANT_WHALEBONE
			, MUTANT_LEVIATHAN
	};

	/** finned mutant fish */
	public static final FishType[] FINNED_MUTANT = {
			  FINNED_MUTANT_SWAMPFISH
			, FINNED_MUTANT_SWORDFISH
			, FINNED_MUTANT_JELLYFISH
			, FINNED_MUTANT_SQUIRMBAG
			, FINNED_MUTANT_WHALEBONE
			, FINNED_MUTANT_LEVIATHAN
	};

	/** finned mutant fish */
	public static final FishType[] SASHIMI_MUTANT = {
			  SASHIMI_MUTANT_SWAMPFISH
			, SASHIMI_MUTANT_SWORDFISH
			, SASHIMI_MUTANT_JELLYFISH
			, SASHIMI_MUTANT_SQUIRMBAG
			, SASHIMI_MUTANT_WHALEBONE
			, SASHIMI_MUTANT_LEVIATHAN
	};

	/** kraken fish */
	public static final FishType[] KRAKEN = {
			  KRAKEN_SWAMPFISH
			, KRAKEN_SWORDFISH
			, KRAKEN_JELLYFISH
			, KRAKEN_SQUIRMBAG
			, KRAKEN_WHALEBONE
			, KRAKEN_LEVIATHAN
	};


	// constants for unitTypes and fishType methods
	// in basesUsedBit and coversUsedBits
	// the first 9bits are boxs
	// the second 9bits are rows
	// the third 9bits are cols
	public static final int BOX_BITS = 511; // (1<<9)-1, ie nine 1s
	public static final int ROW_BITS = BOX_BITS << 9;
	public static final int COL_BITS = BOX_BITS << 18;

	/**
	 * Converts basesUsedBits or coversUsedBits into the legacy unitType mask.
	 * Get the type of bases/covers used from basesUsedBits and coversUsedBits,
	 * which are now maintained by Superfischerman for this and other purposes.
	 * <p>
	 * Note unitTypes impedes a search. Fine when creating a hint.
	 * Use the new {@link #fishType() } method instead, if suitable.
	 *
	 * @param unitsUsedBits either basesUsedBits or coversUsedBits
	 * @return a mask of the types of regions used.
	 */
	public static int unitTypes(final int unitsUsedBits) {
		int result = 0;
		if((unitsUsedBits & BOX_BITS) > 0) result |= BOX_MASK;
		if((unitsUsedBits & ROW_BITS) > 0) result |= ROW_MASK;
		if((unitsUsedBits & COL_BITS) > 0) result |= COL_MASK;
		return result;
	}

	/**
	 * Get the fishType of the current fish, directly from basesUsedBits and
	 * coversUsedBits, without first converting to unitTypes, which is slow.
	 * This is used in the search, where it is still a bit slow.
	 * <p>
	 * WARN: Do not call fishType when maxFish/KrakenType is MUTANT, because it
	 * becomes a tautology, so calculating the fishType is a waste of time.
	 * <p>
	 * NOTE: Franken comes first because it is my maxKrakenType, and
	 * Frankens are a bit faster if there test comes first.
	 *
	 * @param basesUsedBits a 27bit bitset of the usedBases
	 * @param coversUsedBits a 27bit bitset of the usedCovers
	 * @return 0=BASIC_FISH_TYPE, 1=FRANKEN_FISH_TYPE, or 2=MUTANT_FISH_TYPE
	 */
	public static int fishType(final int basesUsedBits, final int coversUsedBits) {
		if ( ( (basesUsedBits&ROW_BITS)>0 && (basesUsedBits&COL_BITS)>0 )
		  || ( (coversUsedBits&ROW_BITS)>0 && (coversUsedBits&COL_BITS)>0 )
		  || ( (basesUsedBits&BOX_BITS)>0 && (coversUsedBits&BOX_BITS)>0 ) )
			return MUTANT_FTYPE;
		if ( ((basesUsedBits|coversUsedBits)&BOX_BITS) > 0 ) //NEVER negative
			return FRANKEN_FTYPE;
		return FINNED_FTYPE;
	}

	// Franken and Mutant fish can be Sashimi too.
	// Does this fish have fins, and any (base & corners) is just one cell
	public static boolean isSashimi(final ARegion[] bases
			, final long coversM0, final int coversM1) {
		for ( ARegion base : bases )
			if ( Long.bitCount(base.idx.m0 & coversM0)
			   + Integer.bitCount(base.idx.m1 & coversM1) < 2 )
				return true;
		return false;
	}

	/**
	 * Get the array of the fish-types appropriate for this fish,
	 * from which you read the fishType using its size-2.
	 *
	 * @param baseM a bitset containing the types of bases used
	 * @param coverM a bitset containing the types of covers used
	 * @param isSashimi does this fish contain a base with just one occurrence
	 *  of the fish candidate value. See {@link Superfischerman#isSashimi}
	 * @param anyFins are there any fins in this fish
	 * @return an array of the appropriate fishTypes for your fish, from which
	 *  you read the fishType using its size-2.
	 */
	public static FishType[] fishTypes(final int baseM, final int coverM
			, final boolean isSashimi, final boolean anyFins) {
		if ( (baseM==ROW_MASK && coverM==COL_MASK)
		  || (baseM==COL_MASK && coverM==ROW_MASK) )
			return isSashimi? SASHIMI : anyFins? FINNED : BASIC;
		// Mutant: (rows AND cols) as bases
		//      or (rows AND cols) as covers
		if ( ( (baseM&ROW_MASK)>0 && (baseM&COL_MASK)>0 )
		  || ( (coverM&ROW_MASK)>0 && (coverM&COL_MASK)>0 )
		  || ( (baseM&BOX_MASK)>0 && (coverM&BOX_MASK)>0 ) )
			return isSashimi? SASHIMI_MUTANT : anyFins? FINNED_MUTANT : MUTANT;
		// Franken: not BOTH rows and cols, as bases or covers
		return isSashimi? SASHIMI_FRANKEN : anyFins? FINNED_FRANKEN : FRANKEN;
	}

	/**
	 * Search for Basic fish.
	 */
	public static final int BASIC_FTYPE = 0;
	/**
	 * Search for Finned (ie Basic) fish.
	 */
	public static final int FINNED_FTYPE = 0;
	/**
	 * Search for Franken fish (boxes as bases XOR covers).
	 */
	public static final int FRANKEN_FTYPE = 1;
	/**
	 * Search for Mutant fish (rows/cols/boxs as both bases AND covers).
	 */
	public static final int MUTANT_FTYPE = 2;
	/**
	 * Set if search is for an unknown fish type.
	 */
	public static final int UNDEFINED_FTYPE = 3; // last valid + 1

	/** Complexity of fish (ignore Kraken) in increasing order */
	public static final int[] FISH_TYPES = {FINNED_FTYPE, FRANKEN_FTYPE, MUTANT_FTYPE};
	public static final String[] FISH_TYPE_NAMES = {"Finned", "Franken", "Mutant"};

	public final String name;
	public final int size;
	public final boolean isBasic;
	public final boolean isFinned;
	public final boolean isSashimi;
	public final boolean isFranken;
	public final boolean isMutant;
	public final boolean isKraken;

	FishType(final String name, final int size
			, final boolean isBasic, final boolean isFinned
			, final boolean isSashimi, final boolean isFranken
			, final boolean isMutant, final boolean isKraken) {
		this.name = name;
		this.size = size;
		this.isBasic = isBasic;
		this.isFinned = isFinned;
		this.isSashimi = isSashimi;
		this.isFranken = isFranken;
		this.isMutant = isMutant;
		this.isKraken = isKraken;
	}

	public int complexity() {
		return isMutant ? MUTANT_FTYPE
			 : isFranken ? FRANKEN_FTYPE
			 : isFinned ? FINNED_FTYPE
			 : isBasic ? BASIC_FTYPE
			 : UNDEFINED_FTYPE;
	}

}
