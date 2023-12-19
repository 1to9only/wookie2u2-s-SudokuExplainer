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
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bidirectional Cycle Hint DTO. ChainerUnary finds cycles, and unary chains.
 */
public final class CycleHint extends AChainingHint {

	private final Ass dstOn;
	private final Ass dstOff;

	public CycleHint(final Grid grid, final IHinter hinter, final Pots redPots
			, final boolean isYChain, final boolean isXChain
			, final Ass dstOn, final Ass dstOff) {
		super(grid, hinter, redPots, 2, isYChain, isXChain, null);
		this.dstOn = dstOn;
		this.dstOff = dstOff;
	}

	/**
	 * Get the cells to paint with an aqua background.
	 *
	 * @return a new LinkedHashSet of dstOff parents
	 */
	@Override
	public Set<Integer> getFlatAquaIndices() {
		final Set<Integer> set = new LinkedHashSet<>(16, 0.75F);
		for ( Ass a=dstOff; a.firstParent!=null; )
			set.add((a=a.firstParent).indice);
		return set;
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		if ( viewNum == 0 )
			return dstOn;
		return dstOff;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		return super.getFlatGreens(viewNum);
	}

	@Override
	public Pots getRedPots(int viewNum) {
		return super.getFlatReds(viewNum).putAll2(reds); // Orangonate
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			return super.getFlatLinks(getChainTarget(viewNum));
		} catch (Exception ex) {
			Log.teeln("WARN: "+Log.me()+": "+ex);
			return null;
		}
	}

	@Override
	protected int getFlatComplexity() {
		return countAncestors(dstOn);
	}

	@Override
	protected ArrayList<Ass> getChainsTargetsImpl() {
		return Ass.arrayList(dstOn, dstOff);
	}

	@Override
	public int getDifficulty() {
		int d = super.getBaseDifficulty(); // for a Unary Chain
		d -= 1; // for Unary Cycle
		if(isYChain && isXChain) d += 1;
		return d + getLengthDifficulty();
	}

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			if ( isXChain && !isYChain )
				s += " on <b>"+dstOn.value+"</b>";
			else
				s += " at <b>" +CELL_IDS[dstOn.indice]+"</b>";
		return s;
	}

	private boolean isSwampfish() {
		return isXChain && !isYChain
			&& getAquaBgIndices(0).size() == 4;
	}

	@Override
	protected String getNamePrefix() { return "Bidirectional "; }
	@Override
	protected String getNameMiddle() {
		// nb: X Cycles of 4 cells tend towards fishyness
		return isYChain ? (isXChain ? "XY" : "Y")
			: (getAquaBgIndices(0).size()==4 ? "Swampfish" : "X");
	}
	@Override
	protected String getNameSuffix() { return " Cycle"; }

	@Override
	public StringBuilder toStringImpl() {
		return Frmt.ids(SB(64).append(getHintTypeName()).append(COLON_SP)
		, getAquaBgIndices(0), CSP, CSP);
	}

	@Override
	public String toHtmlImpl() {
		final String fileName
				= isXChain && isYChain ? "CycleHintXY.html"
				: isXChain ? "CycleHintX.html"
				: "CycleHintY.html";
		final String swamp;
		if ( isSwampfish() )
			swamp = " (Generalized Swampfish)";
		else
			swamp = "";
		return Html.produce(this, fileName
			, Frmt.and(ids(getAquaBgIndices(0)))	 //{0}
			, Integer.toString(dstOn.value)		 // 1
			, swamp								 // 2
			, getChainHtml(dstOn)				 // 3
			, getChainHtml(dstOff)				 // 4
			, reds.toString()					 // 5
		);
	}

}
