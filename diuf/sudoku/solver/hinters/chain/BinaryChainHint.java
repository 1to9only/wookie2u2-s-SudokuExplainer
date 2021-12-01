/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.ArrayList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.SO;


/**
 * A Nishio Forcing Chain, Binary Forcing Chain, or Contradiction Forcing Chain
 * hint.
 */
public final class BinaryChainHint extends AChainingHint {

	private final Ass source, dstOn, dstOff;
	private final boolean isNishio, isContradiction;
	public final String typeID;

	public BinaryChainHint(AHinter hinter, Pots redPots, Ass source
			, Ass dstOn, Ass dstOff, boolean isNishio, boolean isContradiction
			, String typeID) {
		super(hinter, redPots	// hinter, redPots
			, 2				// flatViewCount = 2, ie a BINARY Chain.
			, true, true	// isXChain, isYChain
			// mashed in here rather than leave it up to caller
			, isContradiction||isNishio ? source.flip() : dstOn); // resultAss
		this.source = source;
		this.dstOn = dstOn;
		this.dstOff = dstOff;
		this.isNishio = isNishio;
		this.isContradiction = isContradiction;
		this.typeID = typeID;
	}

	@Override
	public Set<Cell> getFlatAquaCells() {
		return new MyLinkedHashSet<>(source.cell, dstOn.cell);
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		if ( viewNum == 0 )
			return dstOn;
		return dstOff;
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
		if ( isBig )
			s += " at <b>"+source.cell.id+"</b>"
			  + " on <b>"+source.value+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		if ( isNishio )
			return Frmu.getSB(64).append("Binary Nishio Chain").append(typeID)
			  .append(COLON_SP).append(source).append(SO).append(dstOff.contra())
			  .toString();
		if ( isContradiction )
			return Frmu.getSB(64).append("Binary Contradiction Chain").append(typeID)
			  .append(COLON_SP).append(source).append(SO).append(dstOff.contra())
			  .toString();
		return Frmu.getSB(64).append("Binary Reduction Chain").append(typeID)
		  .append(COLON_SP).append(source.contra()).append(SO).append(dstOn)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
//if ( hinter.tech==Tech.NestedPlus && resultAss.cell.id.equals("B6") )
//	Debug.breakpoint();
		final String filename =
				  isNishio ? "BinaryChainHintNishio.html"
				: isContradiction ? "BinaryChainHintContradiction.html"
				: "BinaryChainHintReduction.html";
		String html = Html.load(this, filename);
		try {
		if ( isContradiction ) // NB: isNishio implies isContradiction
			html = Html.format(
				  html
				, source.weak()				// if {0}
				, dstOn.strong()			// if 0 then {1}
				, dstOff.strong()			// if 0 then {2}
				, Html.strongCyanOrRed(source.flip()) // 3
				, getChainHtml(dstOn)		// dstOn  4
				, getChainHtml(dstOff)		// dstOff 5
				, getHinterName()			// hinter 6 // not used in Nishio
				, getPlusHtml()				// plus   7
				, getNestedHtml()			// nested 8
			);
		else // reduction
			html = Html.format(
				  html
				, source.weak(true)				// if {0} then 2
				, source.weak(false)			// if {1} then 2
				, dstOn.strong()				// then   2
				, Html.strongCyanOrRed(dstOn)	// color2 3
				, getChainHtml(dstOn)			// dstOn  4
				, getChainHtml(dstOff)			// dstOff 5
				, getHinterName()				// hinter 6
				, getPlusHtml()					// plus   7
				, getNestedHtml()				// nested 8
			);
		if ( hinter.tech.isNested )
			html = super.appendNestedChainsHtml(html);
		} catch (IrrelevantHintException ex) {
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}
}