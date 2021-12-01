/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.ON;


/**
 * Bidirectional Cycle hint. See the 3 BinaryChainingHint*.html files for a
 * description of what is a Bidirectional Cycle.
 * <p>
 * Note that cycles are sought only in "vanilla" unary chaining mode, that is
 * when !isMultiple && !isDynamic (which implies degree==0 && !isNishio)
 */
public final class BidirectionalCycleHint extends AChainingHint {

	private final Ass dstOn;
	private final Ass dstOff;

	public BidirectionalCycleHint(AHinter hinter, Pots redPots
			, boolean isYChain, boolean isXChain, Ass dstOn, Ass dstOff) {
		super(hinter, redPots, 2, isYChain, isXChain, null);
		this.dstOn = dstOn;
		this.dstOff = dstOff;
	}

	@Override
	public Set<Cell> getFlatAquaCells() {
		// NB: the result is cached by getAquaCells for each viewNum
		// nb: 1,2,4,8,16,32,64 => cols 0&7 in same bucket
		//     1 2 3 4 5  6  7
		Set<Cell> set = new LinkedHashSet<>(64, 1F);
		for ( Ass a=this.dstOff; a.parents!=null && a.parents.size>0; )
			set.add( (a=a.parents.first.item).cell );
//		if ( set.size() > 32 )
//			Debug.breakpoint();
		return set;
	}

	@Override
	protected Ass getChainTarget(int viewNum) {
		if ( viewNum == 0 )
			return dstOn;
		return dstOff;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return super.getFlatGreens(viewNum);
	}

	@Override
	public Pots getReds(int viewNum) {
		return super.getFlatReds(viewNum).putAll2(reds); // Orangonate
	}

//	// AFAIK there are no oranges in BidirectionCycleHints but I it's safer to
//	// override getOranges (minimally) just in case (now OR in the future).
//	// Note that oranges NOT removed from reds and greens, which is required to
//	// make them appear orange!
//	@Override
//	public Pots getOranges(int viewNum) {
//		return getReds(viewNum).intersection(getGreens(viewNum));
//	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			return super.getFlatLinks(getChainTarget(viewNum));
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("BidirectionalCycleHint.getLinks: "+ ex);
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
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		double d = super.getBaseDifficulty(); // for a Unary Chain
		// this is a BidirectionalCycle but we get the base difficulty of a
		// UnaryChain, so we must discount it, which is a bit odd coz all the
		// other base difficulties are specific to there Tech. I didn't think
		// we'd need to differentiate UnaryChains from BidirectionalCycles; but
		// it turns out I was wrong, and it's too late now (too hard) to fix it
		// so discount UnaryChains-base to get BidirectionalCycles-base.
		// NB: I've never seen a rectangular cycle (length==4) so difficulty
		// will collide with Unary Chain, but that's OK coz they're Tasmanian.
		// (much better than colliding with Aligned Dec down at 6.9) and they're
		// now sorted by impact (numElims) within category anyways. Less badness
		// is goodness, right?
		d -= 0.1;
		// XY-Cycles are an extra 10 cents
		if(isYChain && isXChain) d += 0.1;
		// see above: we almost always score with 5+ inches. Though crowd.
		return d + getLengthDifficulty();
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			if ( isXChain && !isYChain )
				s += " on <b>" + dstOn.value + "</b>";
			else
				s += " at <b>" + dstOn.cell.id + "</b>";
		return s;
	}

	private boolean isSwampfish() {
		return isXChain && !isYChain
			// NB: We're NOT calling the nasty getNestedAquaCells()
			&& getAquaCells(0).size() == 4;
	}

	@Override
	protected String getNamePrefix() { return "Bidirectional "; }
	@Override
	protected String getNameMiddle() {
		if ( isYChain ) {
			if ( isXChain )
				return "XY";
			return "Y";
		}
		assert isXChain;
		// NB: We're NOT calling the nasty getNestedAquaCells()
		if ( getAquaCells(0).size() == 4 )
			return "Swamp";
		return "X";
	}
	@Override
	protected String getNameSuffix() { return "Cycle"; }

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(getAquaCells(0)))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		final String fileName
				= isXChain && isYChain ? "BidirectionalCycleHintXY.html"
				: isXChain ? "BidirectionalCycleHintX.html"
				: "BidirectionalCycleHintY.html";
		final String swamp;
		if ( isSwampfish() )
			swamp = " (Generalized Swampfish)";
		else
			swamp = "";
		return Html.produce(this, fileName
			, Frmu.and(getAquaCells(0))		//{0}
			, Integer.toString(dstOn.value)	// 1
			, swamp							// 2
			, getChainHtml(dstOn)			// 3
			, getChainHtml(dstOff)			// 4
			, reds.toString()			// 5
		);
	}

}
