/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;


/**
 * Region Chain hint.
 */
public final class RegionReductionHint extends AChainingHint {

	private final ARegion region;
	private final int theValue;
	private final LinkedHashMap<Integer, Ass> chains;
	private final String typeID;

	public RegionReductionHint(AHinter hinter, Pots redPots, ARegion region
			, int theValue, LinkedHashMap<Integer, Ass> chains, String typeID) {
		super(hinter, redPots, chains.size(), true, true
				, chains.values().iterator().next()); // result, hacky but works
		this.region = region;
		this.theValue = theValue;
		this.chains = chains;
		this.typeID = typeID;
	}

	/**
	 * KRC 2020-09-12 Do not cache in RegionReductionHint or CellReductionHint
	 * because the target is the same for each possible initial assumption.
	 * @param target
	 * @return
	 */
	@Override
	ArrayList<Ass> getAncestorsList(Ass target) {
		return getAncestorsListImpl(target);
	}

	@Override
	public Set<Cell> getFlatAquaCells() {
		return Collections.singleton(resultAss.cell);
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return super.getChainTargetFromArray(chains.values(), viewNum);
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(chains.values());
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return new ArrayList<>(chains.values());
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	ARegion getRegion() {
		return this.region;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a "+getHintTypeName()
		+(isBig ? " on "+theValue+" in <b1>"+region.id+"</b1>" : "");
	}

	@Override
	public String getHintTypeNameImpl() {
		return "Region Reduction Chain"+typeID;
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+theValue+" in "+region.id+" so "+resultAss;
	}

	private String getAssertionsHtml() {
		StringBuilder bfr = new StringBuilder(64 * flatViewCount);
		for ( Ass target : chains.values() )
			bfr.append("<li>If ").append(getSource(target).weak())
			  .append(" then ").append(target.strong())
			  .append("</li>").append(NL);
		return bfr.toString();
	}

	private StringBuilder getChainsHtml() {
		StringBuilder bs = new StringBuilder(128 * flatViewCount);
		int i = 1;
		for ( Ass target : chains.values() ) {
			bs.append("Chain ").append(i)
			  .append(": <b>If ").append(getSource(target).weak())
			  .append(" then ").append(target.strong())
			  .append("</b> (View ").append(i).append("):<br>").append(NL);
			bs.append(getChainHtml(target)).append("<br>").append(NL); // current chain
			++i;
		}
		return bs;
	}

	@Override
	public String toHtmlImpl() {
		String filename = ((AChainer)hinter).isDynamic
				? "RegionReductionHintDynamic.html"
				: "RegionReductionHintStatic.html";
		String html = Html.load(this, filename);
		try {
		html = Html.format(
				  html
				, getAssertionsHtml()			// 0
				, Integer.toString(theValue)	// 1
				, region.id						// 2
				, Html.strongCyanOrRed(resultAss)	// 3
				, getChainsHtml()				// 4
				, getHinterName()				// 5
				, getPlusHtml()					// 6
				, getNestedHtml()				// 7
		);
		if ( hinter.tech.isNested )
			html = super.appendNestedChainsHtml(html);
		} catch (IrrelevantHintException ex) {
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}
}