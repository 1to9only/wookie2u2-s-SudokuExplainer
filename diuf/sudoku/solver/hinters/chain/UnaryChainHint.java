/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Set;

/**
 * Unary Forcing Chain hint.
 */
public final class UnaryChainHint extends AChainingHint {

	public UnaryChainHint(final Grid grid, final IHinter hinter
			, final Ass result, final Pots redPots
			, final boolean isYChain, final boolean isXChain) {
		super(grid, hinter, redPots, 1, isYChain, isXChain, result);
	}

	@Override
	public Set<Integer> getFlatAquaIndices() {
		return Idx.of(resultAss.indice);
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
	public int getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		int d = super.getBaseDifficulty();
		// All Chains are an extra 10 cents over a Bidirectional XY Cycle
		d += 1;
		// XYChains are an extra 10 cents.
		if(isYChain && isXChain) d += 1;
		// and 5+ inches costs ya too. It is only a small establishment.
		return d + getLengthDifficulty();
	}

	@Override
	protected String getNamePrefix() { return "Unary "; }
	@Override
	protected String getNameMiddle() {
		return isXChain ? (isYChain ? "XY" : "X") : "Y";
	}
	@Override
	protected String getNameSuffix() { return " Chain"; }

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in <b>"+CELL_IDS[resultAss.indice];
		s += "</b> on <b>"+resultAss.value+"</b>";
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP).append(resultAss);
	}

	@Override
	protected String toHtmlImpl() {
		final String filename = isYChain
			? "UnaryChainHintXY.html"
			: "UnaryChainHintX.html";
		String html = Html.produce(this, filename
			, colorAss(resultAss, resultAss.flip().weak())	// {0}
			, colorAss(resultAss, resultAss.strong())		//  1
			, Html.weakCyanOrRed(resultAss)			//  2
			, getChainHtml(resultAss)				//  3
		);
		if ( hinter.isNesting() )
			html = super.appendNestedChainsHtml(html);
		return html;
	}

}
