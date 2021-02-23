/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;


/**
 * Unary Forcing Chain hint.
 */
public final class UnaryChainHint extends AChainingHint {

	public UnaryChainHint(AHinter hinter, Ass result, Pots redPots
			, boolean isYChain, boolean isXChain) {
		super(hinter, redPots, 1, isYChain, isXChain, result);
//		if ( "Unary XY Chain: F8-4".equals(this.toString()) )
//			Debug.breakpoint();
	}

	@Override
	public Set<Cell> getFlatAquaCells() {
		return Collections.singleton(resultAss.cell);
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(resultAss);
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return Ass.arrayList(resultAss);
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return resultAss;
	}

	@Override
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		double d = super.getBaseDifficulty();
		// All Chains are an extra 10 cents over a Bidirectional XY Cycle
		d += 0.1;
		// XYChains are an extra 10 cents.
		if(isYChain && isXChain) d += 0.1;
		// and 5+ inches costs ya too. It's only a small establishment.
		return d + getLengthDifficulty();
	}

	@Override
	protected String getNamePrefix() { return ""; }
	@Override
	protected String getNameMiddle() {
		if (isXChain && isYChain) return "Unary XY Chain";
		if (isYChain) return "Unary Y Chain";
		return "Unary X Chain";
	}
	@Override
	protected String getNameSuffix() { return ""; }

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + getHintTypeName()
				+ ( isBig ? " in <b>" + resultAss.cell.id
				+ "</b> on the value <b>" + resultAss.value + "</b>" : "" );
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName() + ": " + resultAss;
	}

	@Override
	protected String toHtmlImpl() {
		final String filename = isYChain
				? "UnaryChainHintXY.html"
				: "UnaryChainHintX.html";
		String html = Html.produce(this, filename
				, resultAss.flip().weak()	// {0}
				, resultAss.strong()			//  1
				, Html.weakCyanOrRed(resultAss)	//  2
				, getChainHtml(resultAss)		//  3
		);
		if ( hinter.tech.isNested )
			html = super.appendNestedChainsHtml(html);
		return html;
	}
}