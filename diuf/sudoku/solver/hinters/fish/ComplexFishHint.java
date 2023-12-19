/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.fish;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;

/**
 * ComplexFishHint is the DTO for a hint from the ComplexFisherman.
 * <p>
 * ComplexFishHint is used directly for "normal" (non-Kraken) hints, and <br>
 * ComplexFishHint is "wrapped" as the "cause" of a KrakenFishHint.
 *
 * @author Keith Corlett 2020 Sept/Oct
 */
public class ComplexFishHint extends AHint  {

	FishType fishType; // type of this Fish hint.
	private final int candidate; // Fish candidate value 1..9 (aka just v)
	// we search the bases, but remove candidates from the covers
	private final Pots endoFins; // endoFins Cell=>Values
	private final Pots sharks; // shark (canniblistic) Cell=>Values

	/**
	 * Construct a new ComplexFishHint
	 * @param grid the grid upon which this hint sits
	 * @param hinter the hinter that created this hint
	 * @param fishType type of this fish
	 * @param candidate the fish candidate value
	 * @param bases the base regions
	 * @param covers the cover regions
	 * @param reds eliminations
	 * @param greens corners
	 * @param fins blue extra vs in the bases (ie not corners)
	 * @param endoFins fins that are in two+ bases
	 * @param sharks additional eliminations: corners that are in two+ covers
	 *  (hobiwan calls them cannibles)
	 */
	ComplexFishHint(final Grid grid, final IHinter hinter
			, final FishType fishType, final int candidate
			, final ARegion[] bases, final ARegion[] covers
			, final Pots reds, final Pots greens
			, final Pots fins // blue
			, final Pots endoFins // purple (nullable)
			, final Pots sharks // yellow
	) {
		super(grid, hinter, reds, greens, null, fins, bases, covers);
		this.fishType = fishType;
		this.candidate = candidate;
		this.endoFins = endoFins;
		this.sharks = sharks;
	}

	@Override
	public Pots getPurplePots() {
		return endoFins; // may be null
	}

	@Override
	public Pots getYellowPots() {
		return sharks;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return fishType.name;
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+candidate+"</b>";
		return s;
	}

	@Override
	protected StringBuilder toStringImpl() {
		final StringBuilder sb = Frmt.getSB();
		if ( isInvalid )
			sb.append("#");
		sb.append(getHintTypeName()).append(COLON_SP).append(Frmu.csv(bases));
		return sb.append(AND).append(Frmu.csv(covers))
		.append(ON).append(candidate);
	}

	@Override
	protected String toHtmlImpl() {
		// Finned/Sashimi: hijack debugMessage = names of finned regions
		if ( (fishType.isFinned || fishType.isSashimi)
		  && !fishType.isFranken && !fishType.isMutant )
			setDebugMessage(Regions.finned(grid, bases, blues.keySet()));
		final String filename =
			fishType.isMutant ? "MutantFishHint.html"
			: fishType.isFranken ? "FrankenFishHint.html"
			: fishType.isFinned||fishType.isSashimi ? "FinnedFishHint.html"
			: "BasicFishHint.html";
		final String nn; if(bases.length<2) nn=EMPTY_STRING; else nn=NUMBER_NAMES[bases.length-2];
		// add some color to toString() to make it easier to see whats what.
		final String coloredHint = Html.colorIn(toString()
				.replaceFirst(": ", ": <b1>")
				.replaceFirst(AND, "</b1> and <b2>")
				.replaceFirst(ON, "</b2> on "));
		final String bluesString = blues==null ? "" : " "+blues.toString();
		final String bluesPlural = blues==null||blues.isEmpty() ? "has no" : (blues.size()==1?"has":"have");
		return Html.produce(this, filename
			, getHintTypeName()				//{0}
			, Integer.toString(candidate)	// 1
			, Regions.typeNames(bases)		// 2
			, Regions.typeNames(covers)		// 3
			, nn							// 4 number name
			, debugMessage					// 5 used in FinnedFishHint.html
			, reds.toString()				// 6 eliminations
			, bluesString					// 7 fins
			, coloredHint					// 8 hint (elims)
			, bluesPlural					// 9
		);
	}

}
