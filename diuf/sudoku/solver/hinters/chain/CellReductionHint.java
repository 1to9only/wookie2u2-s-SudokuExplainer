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
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.SO;


/**
 * A static or dynamic Cell Reduction Hint, produced when all potential values
 * of a cell cause the same other-cell-value; or more commonly: the same other-
 * cell-IS-NOT-value.
 * <p>Reduction hints are sought by the getMultipleOrDynamicChains method only
 * when {@code ( !isNishio && (card==2 || (isMultiple && card>2)) )}
 */
public final class CellReductionHint extends AChainingHint {

	private final Cell srcCell;
	// The causal chains of this Hint, theres a chain in the map for each
	// potential value of srcCell
	private final LinkedHashMap<Integer, Ass> chains;
	private final String typeID;

	public CellReductionHint(AHinter hinter, Pots redPots, Cell srcCell
			, LinkedHashMap<Integer, Ass> chains, String typeID) {
		super(hinter, redPots // Darth-AHinter, the removable cells=>maybes
				, chains.size() // flatViewCount = srcCell.maybesSize
				, true, true // isYChain, isXChain
				, chains.values().iterator().next()); // resultAss
		this.srcCell = srcCell;
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
		return new MyLinkedHashSet<>(new Cell[]{srcCell, resultAss.cell});
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		return super.getChainTargetFromArray(chains.values(), viewNum);
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return new ArrayList<>(chains.values());
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(getChainsTargets());
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in <b>" + srcCell.id + "</b>";
		return s;
	}

	@Override
	protected Ass getSource(Ass target) {
		assert target != null; // instead of an eventual NPE
		// this works for 2 out of 3 targets, even in a Nesting hint.
		Ass orig = super.getSource(target);
		// Fix "mad" assertions in Nesting (master) CellReductionHints
		Ass found = null;
		// if no ancestor found || orig isn't even on the right bloody cell!
		if ( orig==null || (orig.cell.i != srcCell.i) )
			found = recurseSource(target, srcCell);
		// We'll settle for orig if it's the best we can do, even if it's null!
		if ( found != null )
			return found;
		return orig;
	}

	private StringBuilder getAssertionsHtml() {
		StringBuilder sb = new StringBuilder(flatViewCount*128);
		Ass source;
		for ( Ass target : getChainsTargets() ) {
			source = getSource(target);
			if ( source == null )
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
		StringBuilder sb = new StringBuilder(flatViewCount*128);
		int index=1;  Ass source;
		for ( Ass target : getChainsTargets() ) {
			source = getSource(target);
			if(source==null) throw new IrrelevantHintException();
			sb.append("Chain ").append(index).append(": <b>If ")
			  .append(source.weak()).append(" then ").append(target.strong())
			  .append("</b> (View ").append(index).append("):<br>").append(NL)
			  .append(getChainHtml(target)).append("<br>").append(NL);
			++index;
		}
		return sb;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "Cell Reduction "+chainType()+" Chain"+typeID;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName())
		  .append(COLON_SP).append(srcCell.id)
		  .append(SO).append(resultAss)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		String filename = ((ChainerBase)hinter).isDynamic
				? "CellReductionHintDynamic.html"
				: "CellReductionHintStatic.html";
		String html = Html.load(this, filename);
		try {
			html = Html.format( html
					, getAssertionsHtml()			// {0}
					, srcCell.id					//  1
					, Html.strongCyanOrRed(resultAss)	//  2
					, getChainsHtml()				//  3
					, getHinterName()				//  4
					, getPlusHtml()					//  5
					, getNestedHtml()				//  6
			);
			if ( hinter.tech.isNested )
				html = super.appendNestedChainsHtml(html);
		} catch (IrrelevantHintException ex) { // probably from getAssertionsHtml()
			// see IrrelevantHintException declaration for discussion
			html = Html.load(ex, "IrrelevantHintException.html");
		}
		return html;
	}
}