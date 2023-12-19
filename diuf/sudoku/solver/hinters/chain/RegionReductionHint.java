/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.SO;

/**
 * Region Chain hint.
 */
public final class RegionReductionHint extends AChainingHint {

	private final ARegion region;
	private final int theValue;
	private final ArrayList<Ass> chains;
	private final String typeId;

	public RegionReductionHint(final Grid grid, final IHinter hinter
		, final Pots redPots, final ARegion region, final int theValue
		, final ArrayList<Ass> chains, final int typeId, final Ass result) {
		super(grid, hinter, redPots, chains.size(), true, true, result);
		this.region = region;
		this.theValue = theValue;
		this.chains = chains;
		this.typeId = WANT_TYPE_ID ? " ("+typeId+")" : "";
	}

	/**
	 * KRC 2020-09-12 Do not cache in RegionReductionHint or CellReductionHint
	 * because the target is the same for each possible initial assumption.
	 * @param target
	 * @return
	 */
	@Override
	ArrayList<Ass> ancestors(Ass target) {
		return ancestorsImpl(target);
	}

	@Override
	public Set<Integer> getFlatAquaIndices() {
		return Idx.of(resultAss.indice);
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return super.getChainTargetFromArray(chains, viewNum);
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(chains);
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return chains;
	}

	@Override
	public ARegion[] getBases() {
		return Regions.array(region);
	}

	ARegion getRegion() {
		return this.region;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += ON+theValue+IN+"<b1>"+region.label+"</b1>";
		return s;
	}

	@Override
	public String getNameMiddle() {
		return "Region Reduction Chain"+typeId;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP).append(theValue)
		.append(IN).append(region.label).append(SO).append(resultAss);
	}

	private String getAssertionsHtml() {
		final StringBuilder sb = SB(flatViewCount<<6); // * 64
		chains.forEach((target) -> {
			final Ass source = getInitialAss(target);
			if ( source != null ) {
				sb.append("<li>If ").append(source.weak())
				.append(" then ").append(colorAss(target, target.strong()))
				.append("</li>").append(NL);
			}
		});
		return sb.toString();
	}

	private StringBuilder getChainsHtml() {
		final StringBuilder sb = SB(flatViewCount<<7); // * 128
		int i = 1;
		for ( Ass target : chains ) {
			final Ass source = getInitialAss(target);
			if ( source != null ) {
				sb.append("Chain ").append(i)
				.append(": <b>If ").append(source.weak())
				.append(" then ").append(target.strong())
				.append("</b> (View ").append(i).append("):<br>").append(NL)
				.append(getChainHtml(target)).append("<br>").append(NL); // current chain
				++i;
			}
		}
		return sb;
	}

	@Override
	public String toHtmlImpl() {
		String filename = ((AChainerBase)hinter).isDynamic
				? "RegionReductionHintDynamic.html"
				: "RegionReductionHintStatic.html";
		String html = Html.load(this, filename);
		try {
		html = Html.format(html
			, getAssertionsHtml()				// 0
			, Integer.toString(theValue)		// 1
			, region.label						// 2
			, Html.strongCyanOrRed(resultAss)	// 3
			, getChainsHtml()					// 4
			, getHinterName()					// 5
			, getPlusHtml()						// 6
			, getNestedHtml()					// 7
		);
		if ( hinter.isNesting() )
			html = super.appendNestedChainsHtml(html);
		} catch (IrrelevantHintException ex) {
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}

}
