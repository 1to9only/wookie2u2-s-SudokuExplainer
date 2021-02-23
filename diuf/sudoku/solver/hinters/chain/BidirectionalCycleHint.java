/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.chain;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;


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
		return viewNum==0 ? this.dstOn : this.dstOff;
	}

	@Override
	public Pots getGreens(int viewNum) {
		return super.getFlatGreens(viewNum);
	}

	@Override
	public Pots getReds(int viewNum) {
		return super.getFlatReds(viewNum).putAll2(redPots); // Orangonate
	}

	// To the best of my knowledge THERE ARE NO ORANGES in BidirectionCycleHints
	// but I think it's safer to override getOranges (minimallu) just in case
	// there are, now OR in the future. Note that oranges are NOT removed from
	// reds and greens, which is currently required to make them appear orange!
	@Override
	public Pots getOranges(int viewNum) {
		return getReds(viewNum).intersection(getGreens(viewNum));
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		return super.getFlatLinks(getChainTarget(viewNum));
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
		return "Look for a " + getHintTypeName()
			+ ( !isBig ? "" : isXChain && !isYChain
				? " on <b>" + dstOn.value + "</b>"
				: " at <b>" + dstOn.cell.id + "</b>" );
	}

	private boolean isSwampfish() {
		return isXChain && !isYChain
			// NB: We're not calling the nasty getNestedAquaCells()
			&& getAquaCells(0).size() == 4;
	}

	@Override
	protected String getNamePrefix() { return "Bidirectional "; }
	@Override
	protected String getNameMiddle() {
		if ( isYChain )
			return isXChain ? "XY Cycle" : "Y Cycle";
		assert isXChain;
		return getAquaCells(0).size()==4 ? "Swamp Cycle" : "X Cycle";
	}
	@Override
	protected String getNameSuffix() { return ""; }

	@Override
	public String toStringImpl() {
		return getHintTypeName() + ": " + Frmt.csv(getAquaCells(0));
	}

	@Override
	public String toHtmlImpl() {
		final String fileName
				= isXChain && isYChain ? "BidirectionalCycleHintXY.html"
				: isXChain ? "BidirectionalCycleHintX.html"
				: "BidirectionalCycleHintY.html";
		return Html.produce(this, fileName
				, Frmt.and(getAquaCells(0))		//{0}
				, Integer.toString(dstOn.value)	// 1
				, isSwampfish() ? " (Generalized Swampfish)" : "" // 2
				, getChainHtml(dstOn)			// 3
				, getChainHtml(dstOff)			// 4
		);
	}
}