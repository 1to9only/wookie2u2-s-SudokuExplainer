/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
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
	protected String getNamePrefix() { return "Unary"; }
	@Override
	protected String getNameMiddle() {
		if ( isXChain ) {
			if ( isYChain )
				return "XY";
			return "X";
		}
		if ( isYChain )
			return "Y";
		return "#"; // should NEVER happen
	}
	@Override
	protected String getNameSuffix() { return "Chain"; }

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in <b>" + resultAss.cell.id;
		s += "</b> on <b>" + resultAss.value + "</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(resultAss)
		  .toString();
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