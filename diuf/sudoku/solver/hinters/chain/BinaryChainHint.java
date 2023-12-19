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
import diuf.sudoku.Tech;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.SO;

/**
 * A Binary Nishio Chain,
 * Binary Contradiction Chain, or
 * Binary Reduction Chain hint.
 */
public final class BinaryChainHint extends AChainingHint {

	private final Ass source, dstOn, dstOff;
	private final boolean isNishio, isContradiction;
	public final String typeId;

	public BinaryChainHint(final Grid grid, final IHinter hinter
			, final Pots redPots, final Ass source, final Ass dstOn
			, final Ass dstOff, final boolean isNishio
			, final boolean isContradiction, final int typeId
			, final Ass result) {
		super(grid, hinter, redPots, 2, true, true, result); // resultAss
		this.source = source;
		this.dstOn = dstOn;
		this.dstOff = dstOff;
		this.isNishio = isNishio;
		this.isContradiction = isContradiction;
		this.typeId = WANT_TYPE_ID ? " ("+typeId+")" : "";
	}

	@Override
	public Set<Integer> getFlatAquaIndices() {
		return Idx.of(source.indice, dstOn.indice);
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return viewNum==0 ? dstOn : dstOff;
	}

	@Override
	public Pots getFlatReds(int viewNum) {
		return super.getFlatReds(viewNum).upsertAll(reds); //Orangonate
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return Ass.arrayList(dstOn, dstOff);
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(dstOn) + countAncestors(dstOff);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig ) {
			s += " at <b>"+CELL_IDS[source.indice]+"</b>"
			  + " on <b>"+source.value+"</b>";
		}
		return s;
	}

	@Override
	protected String getNameMiddle() {
		final AChainerBase base = (AChainerBase)hinter;
		return base.isNishio ? "Nishio"
			 : base.isDynamic ? (base.degree > 1 ? "Nested" : "Dynamic")
			 : "Multiple";
	}

	@Override
	protected String getNameSuffix() {
		switch ( hinter.getTech() ) {
			case DynamicChain	: return " Chain";
			case DynamicPlus		: return " Plus";
			case NestedUnary	: return " "+Tech.UnaryChain.name();
			case NestedStatic	: return " "+Tech.StaticChain.name();
			case NestedDynamic	: return " "+Tech.DynamicChain.name();
			case NestedPlus		: return " "+Tech.DynamicPlus.name();
			default				: return " Forcing Chain";
		}
	}

	@Override
	public StringBuilder toStringImpl() {
		if ( isNishio )
			return SB(64).append("Nishio Forcing Chain").append(typeId)
			.append(COLON_SP).append(source).append(SO).append(dstOff.contra());
		if ( isContradiction )
			return SB(64).append("Binary Contradiction Chain").append(typeId)
			.append(COLON_SP).append(source).append(SO).append(dstOff.contra());
		return SB(64).append("Binary Reduction Chain").append(typeId)
		.append(COLON_SP).append(source.contra()).append(SO).append(dstOn);
	}

	@Override
	public String toHtmlImpl() {
		final String filename =
				  isNishio ? "BinaryChainHintNishio.html"
				: isContradiction ? "BinaryChainHintContradiction.html"
				: "BinaryChainHintReduction.html";
		String html = Html.load(this, filename);
		try {
		if ( isContradiction ) // NB: isNishio implies isContradiction
			html = Html.format(
				  html
				, colorAss(source, source.weak())		// if {0}
				, colorAss(dstOn, dstOn.strong())		// if 0 then {1}
				, colorAss(dstOff, dstOff.strong())		// if 0 then {2}
				, Html.strongCyanOrRed(source.flip())	// 3
				, getChainHtml(dstOn)					// dstOn  4
				, getChainHtml(dstOff)					// dstOff 5
				, getHinterName()						// hinter 6 // not used in Nishio
				, getPlusHtml()							// plus   7
				, getNestedHtml()						// nested 8
			);
		else // reduction
			html = Html.format(
				  html
				, colorAss(source, source.weak(true)	)	// if {0} then 2
				, colorAss(source, source.weak(false))	// if {1} then 2
				, colorAss(dstOn, dstOn.strong())		// then   2
				, Html.strongCyanOrRed(dstOn)			// color2 3
				, getChainHtml(dstOn)					// dstOn  4
				, getChainHtml(dstOff)					// dstOff 5
				, getHinterName()						// hinter 6
				, getPlusHtml()							// plus   7
				, getNestedHtml()						// nested 8
			);
		if ( hinter.isNesting() )
			html = super.appendNestedChainsHtml(html);
		} catch (IrrelevantHintException ex) {
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}

}
