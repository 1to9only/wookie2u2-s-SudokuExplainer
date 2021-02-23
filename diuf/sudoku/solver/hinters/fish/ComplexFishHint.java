/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;

/**
 * ComplexFishHint is a hint from ComplexFisherman. I'm used directly for
 * "normal" (non-Kraken) hints, and I'm also "wrapped" by a KrakenFishHint,
 * where I am the "base" or "cause" hint.
 *
 * @author Keith Corlett 2020 Sept/Oct
 */
public class ComplexFishHint extends AHint {

	enum Type {
		  SWAMPFISH("Swampfish", 2, true, false, false, false, false)
		, SWORDFISH("Swordfish", 3, true, false, false, false, false)
		, JELLYFISH("Jellyfish", 4, true, false, false, false, false)
		, SQUIRMBAG("Squirmbag", 5, true, false, false, false, false)
		, WHALE("Whale", 6, true, false, false, false, false)
		, LEVIATHAN("Leviathan", 7, true, false, false, false, false)

		, FINNED_SWAMPFISH("Finned Swampfish", 2, false, true, false, false, false)
		, FINNED_SWORDFISH("Finned Swordfish", 3, false, true, false, false, false)
		, FINNED_JELLYFISH("Finned Jellyfish", 4, false, true, false, false, false)
		, FINNED_SQUIRMBAG("Finned Squirmbag", 5, false, true, false, false, false)
		, FINNED_WHALE("Finned Whale", 6, false, true, false, false, false)
		, FINNED_LEVIATHAN("Finned Leviathan", 7, false, true, false, false, false)

		, SASHIMI_SWAMPFISH("Sashimi Swampfish", 2, false, false, true, false, false)
		, SASHIMI_SWORDFISH("Sashimi Swordfish", 3, false, false, true, false, false)
		, SASHIMI_JELLYFISH("Sashimi Jellyfish", 4, false, false, true, false, false)
		, SASHIMI_SQUIRMBAG("Sashimi Squirmbag", 5, false, false, true, false, false)
		, SASHIMI_WHALE("Sashimi Whale", 6, false, false, true, false, false)
		, SASHIMI_LEVIATHAN("Sashimi Leviathan", 7, false, false, true, false, false)

		, FRANKEN_SWAMPFISH("Franken Swampfish", 2, false, false, false, true, false)
		, FRANKEN_SWORDFISH("Franken Swordfish", 3, false, false, false, true, false)
		, FRANKEN_JELLYFISH("Franken Jellyfish", 4, false, false, false, true, false)
		, FRANKEN_SQUIRMBAG("Franken Squirmbag", 5, false, false, false, true, false)
		, FRANKEN_WHALE("Franken Whale", 6, false, false, false, true, false)
		, FRANKEN_LEVIATHAN("Franken Leviathan", 7, false, false, false, true, false)

		, FINNED_FRANKEN_SWAMPFISH("Finned Franken Swampfish", 2, false, true, false, true, false)
		, FINNED_FRANKEN_SWORDFISH("Finned Franken Swordfish", 3, false, true, false, true, false)
		, FINNED_FRANKEN_JELLYFISH("Finned Franken Jellyfish", 4, false, true, false, true, false)
		, FINNED_FRANKEN_SQUIRMBAG("Finned Franken Squirmbag", 5, false, true, false, true, false)
		, FINNED_FRANKEN_WHALE("Finned Franken Whale", 6, false, true, false, true, false)
		, FINNED_FRANKEN_LEVIATHAN("Finned Franken Leviathan", 7, false, true, false, true, false)

		, MUTANT_SWAMPFISH("Mutant Swampfish", 2, false, false, false, false, true)
		, MUTANT_SWORDFISH("Mutant Swordfish", 3, false, false, false, false, true)
		, MUTANT_JELLYFISH("Mutant Jellyfish", 4, false, false, false, false, true)
		, MUTANT_SQUIRMBAG("Mutant Squirmbag", 5, false, false, false, false, true)
		, MUTANT_WHALE("Mutant Whale", 6, false, false, false, false, true)
		, MUTANT_LEVIATHAN("Mutant Leviathan", 7, false, false, false, false, true)

		, FINNED_MUTANT_SWAMPFISH("Finned Mutant Swampfish", 2, false, true, false, false, true)
		, FINNED_MUTANT_SWORDFISH("Finned Mutant Swordfish", 3, false, true, false, false, true)
		, FINNED_MUTANT_JELLYFISH("Finned Mutant Jellyfish", 4, false, true, false, false, true)
		, FINNED_MUTANT_SQUIRMBAG("Finned Mutant Squirmbag", 5, false, true, false, false, true)
		, FINNED_MUTANT_WHALE("Finned Mutant Whale", 6, false, true, false, false, true)
		, FINNED_MUTANT_LEVIATHAN("Finned Mutant Leviathan", 7, false, true, false, false, true)
		;

		/** unfinned basic fish */
		static final Type[] BASIC = {
				  SWAMPFISH
				, SWORDFISH
				, JELLYFISH
				, SQUIRMBAG
				, WHALE
				, LEVIATHAN
		};
		/** finned fish */
		static final Type[] FINNED = {
				  FINNED_SWAMPFISH
				, FINNED_SWORDFISH
				, FINNED_JELLYFISH
				, FINNED_SQUIRMBAG
				, FINNED_WHALE
				, FINNED_LEVIATHAN
		};
		/** sashimi fish */
		static final Type[] SASHIMI = {
				  SASHIMI_SWAMPFISH
				, SASHIMI_SWORDFISH
				, SASHIMI_JELLYFISH
				, SASHIMI_SQUIRMBAG
				, SASHIMI_WHALE
				, SASHIMI_LEVIATHAN
		};
		/** franken fish */
		static final Type[] FRANKEN = {
				  FRANKEN_SWAMPFISH
				, FRANKEN_SWORDFISH
				, FRANKEN_JELLYFISH
				, FRANKEN_SQUIRMBAG
				, FRANKEN_WHALE
				, FRANKEN_LEVIATHAN
		};
		/** finned franken fish */
		static final Type[] FINNED_FRANKEN = {
				  FINNED_FRANKEN_SWAMPFISH
				, FINNED_FRANKEN_SWORDFISH
				, FINNED_FRANKEN_JELLYFISH
				, FINNED_FRANKEN_SQUIRMBAG
				, FINNED_FRANKEN_WHALE
				, FINNED_FRANKEN_LEVIATHAN
		};
		/** mutant fish */
		static final Type[] MUTANT = {
				  MUTANT_SWAMPFISH
				, MUTANT_SWORDFISH
				, MUTANT_JELLYFISH
				, MUTANT_SQUIRMBAG
				, MUTANT_WHALE
				, MUTANT_LEVIATHAN
		};
		/** finned mutant fish */
		static final Type[] FINNED_MUTANT = {
				  FINNED_MUTANT_SWAMPFISH
				, FINNED_MUTANT_SWORDFISH
				, FINNED_MUTANT_JELLYFISH
				, FINNED_MUTANT_SQUIRMBAG
				, FINNED_MUTANT_WHALE
				, FINNED_MUTANT_LEVIATHAN
		};

		public final String name;
		public final int size;
		public final boolean isBasic;
		public final boolean isFinned;
		public final boolean isSashimi;
		public final boolean isFranken;
		public final boolean isMutant;
		Type(String name, int size, boolean isBasic, boolean isFinned
				, boolean isSashimi, boolean isFranken, boolean isMutant) {
			this.name = name;
			this.size = size;
			this.isBasic = isBasic;
			this.isFinned = isFinned;
			this.isSashimi = isSashimi;
			this.isFranken = isFranken;
			this.isMutant = isMutant;
		}
	}

	final Type hType; // the type of this Fish hint.
	final boolean isSashimi; // is this fish Sashimi.
	final int candidate; // the Fish candidate value 1..9 (aka just v)
	// we search the bases, but remove candidates from the covers
	final List<ARegion> bases; // the base regions in this Fish
	final List<ARegion> covers; // the cover regions in this Fish
	final Pots greenPots; // the corner (base & covers) Cell=>Values
	final Pots fins; // the exoFins Cell=>Values
	final Pots endoFins; // the endoFins Cell=>Values
	final Pots sharks; // the shark (canniblistic) Cell=>Values
	String debugMessage; // the techies debug message, else "" (never null)
	boolean isInvalid = false; // set to true BEFORE toString is called when the HintValidator finds me invalid, when the HintValidator is used, which I hope will soon be never!
	ComplexFishHint(AHinter hinter, Type hintType, boolean isSashimi
			, int candidate, List<ARegion> bases, List<ARegion> covers
			, Pots redPots, Pots greenPots, Pots fins, Pots endoFins
			, Pots sharks, String debugMessage) {
		super(hinter, redPots);
		this.hType = hintType;
		this.isSashimi = isSashimi;
		this.candidate = candidate;
		this.bases = bases;
		this.covers = covers;
		this.greenPots = greenPots;
		this.fins = fins;
		this.endoFins = endoFins;
		this.sharks = sharks;
		this.debugMessage = debugMessage;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNumUnused) {
		return greenPots.keySet();
	}

	@Override
	public Pots getGreens(int viewNumUnused) {
		return greenPots;
	}

	@Override
	public Pots getReds(int viewNumUnused) {
		return redPots;
	}

	@Override
	public Pots getBlues(Grid gridUnused, int viewNumUnused) {
		return fins;
	}

	@Override
	public Pots getPurples() {
		return endoFins;
	}

	@Override
	public Pots getYellows() {
		return sharks;
	}

	@Override
	public List<ARegion> getBases() {
		return bases;
	}

	@Override
	public List<ARegion> getCovers() {
		return covers;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hType.name;
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		return "Look for a "+getHintTypeName()
			+ (isBig ? " on <b>"+candidate+"</b>" : "");
	}

	@Override
	protected String toStringImpl() {
		return (isInvalid?"#":"")+getHintTypeName()+": "+Frmt.csv(bases)
				 // sashimi fins replace a corner
				 + (isSashimi ? " (fins "+fins.cells()+")" : "")
				 + " and "+Frmt.csv(covers)
				 + " on "+candidate;
	}

	String squeeze() {
		return toStringImpl()+" ("+redPots+")"; // NO CACHING!
	}

	@Override
	protected String toHtmlImpl() {
		if ( true ) {
			// Finned/Sashimi: hijack debugMessage = names of finned regions
			if ( (hType.isFinned || hType.isSashimi)
			  && !hType.isFranken && !hType.isMutant )
				debugMessage = Regions.finned(bases, fins.keySet());
			final String filename =
				hType.isMutant ? "MutantFishHint.html"
				: hType.isFranken ? "FrankenFishHint.html"
				: hType.isFinned||hType.isSashimi ? "FinnedFishHint.html"
				: "BasicFishHint.html";
			return Html.produce(this, filename
					, getHintTypeName()						// {0}
					, Integer.toString(candidate)			//  1
					, Regions.typeNames(bases)				//  2
					, Regions.typeNames(covers)				//  3
					, degree<2?"":NUMBER_NAMES[degree-2]	//  4
					, debugMessage							//  5
					, redPots.toString()					//  6
					, fins.toString()					//  7 fins
			);
		} else {
			// Developers only: HoDoKuFisherman#squeeze parses this into a log
			// line, so we use this to find any invalid hints.
			return "<html><body><pre>"
				 + NL+toString()+" ("+redPots+")" // "$#$hintType: $bases and $covers on $candidate ($redPots)"
				 + (fins.isEmpty() ? "" : NL+"exoFins (blue): "+fins)
				 + (endoFins.isEmpty() ? "" : NL+"endoFins (purple): "+endoFins)
				 + (sharks==null||sharks.isEmpty() ? "" : NL+"sharks (yellow): "+sharks)
				 + NL+"</pre></body></html>";
		}
	}

}
