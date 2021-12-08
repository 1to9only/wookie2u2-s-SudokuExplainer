/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.List;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;

/**
 * ComplexFishHint is a hint from the ComplexFisherman.
 * <p>
 * ComplexFishHint is used directly for "normal" (non-Kraken) hints, and<br>
 * ComplexFishHint is "wrapped" as the "cause" of a KrakenFishHint.
 *
 * @author Keith Corlett 2020 Sept/Oct
 */
public class ComplexFishHint extends AHint  {

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
	final Pots endoFins; // the endoFins Cell=>Values
	final Pots sharks; // the shark (canniblistic) Cell=>Values
	String tag; // the techies debug message, else "" (never null)
	ComplexFishHint(AHinter hinter, Type hintType, boolean isSashimi
			, int candidate, ARegion[] bases, ARegion[] covers
			, Pots redPots, Pots greenPots, Pots fins, Pots endoFins
			, Pots sharks, String debugMessage) {
		super(hinter, redPots, greenPots, null, fins, bases, covers);
		this.hType = hintType;
		this.isSashimi = isSashimi;
		this.candidate = candidate;
		this.endoFins = endoFins;
		this.sharks = sharks;
		this.tag = debugMessage;
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
	protected String getHintTypeNameImpl() {
		return hType.name;
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+candidate+"</b>";
		return s;
	}

	@Override
	protected String toStringImpl() {
		final StringBuilder sb = Frmt.getSB();
		if ( isInvalid )
			sb.append("#");
		sb.append(getHintTypeName()).append(COLON_SP).append(Frmu.csv(bases));
		if ( isSashimi )
			sb.append(" (fins ").append(blues.cells()).append(")");
		return sb.append(AND).append(Frmu.csv(covers))
		  .append(ON).append(candidate)
		  .toString();
	}

	String squeeze() {
		return toStringImpl()+" ("+reds+")"; // NO CACHING!
	}

	@Override
	protected String toHtmlImpl() {
		// Finned/Sashimi: hijack debugMessage = names of finned regions
		if ( (hType.isFinned || hType.isSashimi)
		  && !hType.isFranken && !hType.isMutant )
			tag = Regions.finned(bases, blues.keySet());
		else
			tag = EMPTY_STRING;
		final String filename =
			hType.isMutant ? "MutantFishHint.html"
			: hType.isFranken ? "FrankenFishHint.html"
			: hType.isFinned||hType.isSashimi ? "FinnedFishHint.html"
			: "BasicFishHint.html";
		final String nn; if(degree<2) nn=EMPTY_STRING; else nn=NUMBER_NAMES[degree-2];
		// add some color to toString() to make it easier to see what's what.
		final String coloredHint = Html.colorIn(toString()
				.replaceFirst(": ", ": <b1>")
				.replaceFirst(AND, "</b1> and <b2>")
				.replaceFirst(ON, "</b2> on "));
		return Html.produce(this, filename
			, getHintTypeName()				// {0}
			, Integer.toString(candidate)	//  1
			, Regions.typeNames(bases)		//  2
			, Regions.typeNames(covers)		//  3
			, nn							//  4 number name
			, tag							//  5 used in FinnedFishHint.html
			, reds.toString()			//  6 eliminations
			, blues.toString()				//  7 fins
			, coloredHint					//  8 hint (elims)
			, blues.size()==1?"has":"have"	//  9
		);
	}

}
