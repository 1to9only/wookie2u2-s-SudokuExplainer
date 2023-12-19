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
import diuf.sudoku.Pots;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.SO;
import diuf.sudoku.utils.MyLinkedHashSet;

/**
 * A Cell Reduction Hint is produced when all potential values of a cell cause
 * the same cell-value; or more commonly the same cell-IS-NOT-value.
 */
public final class CellReductionHint extends AChainingHint {

	private final int srcIndice;
	// a chain for each potential value of srcCell (in maybes order)
	private final ArrayList<Ass> chains;
	private final String typeId;

	public CellReductionHint(final Grid grid, final IHinter hinter
			, final Pots redPots, final int srcIndice
			, final ArrayList<Ass> chains, final int typeId
			, final Ass result) {
		super(grid, hinter, redPots, chains.size(), true, true, result);
		this.srcIndice = srcIndice;
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
	ArrayList<Ass> ancestors(final Ass target) {
		return ancestorsImpl(target);
	}

	@Override
	public Set<Integer> getFlatAquaIndices() {
		return MyLinkedHashSet.of(new int[]{srcIndice, resultAss.indice});
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return super.getChainTargetFromArray(chains, viewNum);
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return chains;
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(getChainsTargets());
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in <b>"+CELL_IDS[srcIndice]+"</b>";
		return s;
	}

	/**
	 * This override of getInitialAss tries a bit harder to get the initial
	 * assumption that caused the given target Ass.
	 * <p>
	 * Note this ONLY fixes mad logic in Nesting (master) CellReductionHints!
	 * I start by calling super.getInitialAss, which follows the firstParent
	 * links back-to the first assumption that does not have a firstParent. If
	 * that Ass is null or its indice != srcIndice then try again recursively,
	 * searching targets whole ancestry, looking for an On of srcIndice (value
	 * is unknown so is not considered). If thats also null then I return the
	 * original super.getInitialAss result, even if its null. This is the best
	 * I can do. It may still occasionally fail to find the initial assumption!
	 *
	 * @param target to get the initial assumption of
	 * @return the initial assumption that caused target
	 */
	@Override
	protected Ass getInitialAss(Ass target) {
		assert target != null; // instead of an eventual NPE
		// this works for ~66% of targets in a Nesting hint.
		final Ass orig = super.getInitialAss(target);
		// Fix mad logic in Nesting (master) CellReductionHints
		// if none or not even the right frickin cell
		Ass result = null;
		if ( orig==null || orig.indice != srcIndice ) {
			result = super.getInitialAssRecursively(target, srcIndice);
		}
		// settle for orig if its the best we can do, even if its null!
		return result!=null ? result : orig;
	}

	private StringBuilder getAssertionsHtml() {
		Ass source;
		final StringBuilder sb = SB(flatViewCount*128);
		for ( Ass target : getChainsTargets() ) {
			if ( (source=getInitialAss(target)) == null )
				throw new IrrelevantHintException();
			sb.append("<li>If ").append(source.weak())
			.append(" then ").append(target.strong())
			.append(NL);
		}
		return sb;
	}

	/**
	 * Appends the HTML for each of this hints chain-targets
	 * @return the populated StringBuilder
	 */
	private StringBuilder getChainsHtml() {
		StringBuilder sb = SB(flatViewCount*128);
		int index=1;  Ass source;
		for ( Ass target : getChainsTargets() ) {
			if ( (source=getInitialAss(target)) == null )
				throw new IrrelevantHintException();
			sb.append("Chain ").append(index).append(": <b>If ")
			.append(source.weak()).append(" then ").append(target.strong())
			.append("</b> (View ").append(index).append("):<br>").append(NL)
			.append(getChainHtml(target)).append("<br>").append(NL);
			++index;
		}
		return sb;
	}

	@Override
	public String getNameMiddle() {
		return "Cell Reduction "+(isXChain ? (isYChain ? "XY" : "X") : "Y")+" Chain"+typeId;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(CELL_IDS[srcIndice]).append(SO).append(resultAss);
	}

	@Override
	public String toHtmlImpl() {
		final String filename = ((AChainerBase)hinter).isDynamic
				? "CellReductionHintDynamic.html"
				: "CellReductionHintStatic.html";
		String html;
		try {
			html = Html.format( Html.load(this, filename)
				, getAssertionsHtml()				// {0}
				, CELL_IDS[srcIndice]				//  1
				, Html.strongCyanOrRed(resultAss)	//  2
				, getChainsHtml()					//  3
				, getHinterName()					//  4
				, getPlusHtml()						//  5
				, getNestedHtml()					//  6
			);
			if ( hinter.isNesting() ) {
				html = super.appendNestedChainsHtml(html);
			}
		} catch (IrrelevantHintException ex) { // from getAssertionsHtml()
			// see IrrelevantHintException declaration for discussion
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}

}
