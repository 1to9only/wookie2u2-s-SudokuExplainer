/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


/**
 * AURTHint (Abstract Unique Rectangle Hint) contains shared implementation of
 * the various types of Unique Rectangle and Loop Hints.
 */
public abstract class AURTHint extends AHint implements IActualHint {

	// Sorts the hints by difficulty, type ascending
	public static final Comparator<AURTHint> BY_DIFFICULTY_DESC_TYPE_INDEX_ASC
			= new Comparator<AURTHint>() {
		@Override
		public int compare(AURTHint h1, AURTHint h2) {
			final double d1 = h1.getDifficulty();
			final double d2 = h2.getDifficulty();
			if (d1 < d2)
				return -1;
			if (d1 > d2)
				return 1;
			return h1.typeIndex - h2.typeIndex;
		}
	};

	protected final List<Cell> loop;
	protected final int loopSize;
	protected final int v1;
	protected final int v2;
	protected final int typeIndex;

	public AURTHint(int typeIndex, UniqueRectangle hinter, List<Cell> loop
			, int v1, int v2, Pots redPots) {
		super(hinter, redPots);
		this.typeIndex = typeIndex;
		this.loop = loop;
		this.loopSize = loop.size();
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(loop);
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null ) {
			Pots pots = new Pots(loop.size(), 1F);
			for ( Cell c : loop )
				pots.put(c, new Values(v1, v2)); // orange
			pots.removeAll(redPots);
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final List<Cell> loop = this.loop;
			final int n = this.loopSize;
			final Collection<Link> links = new ArrayList<>(n); // the result
			for ( int i=0; i<n; ++i ) {
				Cell curr = loop.get(i);
				Cell next = loop.get((i+1) % n);
				links.add(new Link(curr, 0, next, 0));
			}
			return links;
		} catch (Throwable ex) {
			// I'm only ever called in the GUI, so just log it.
			Log.println("AURTHint.getLinks: "+ ex);
			return null;
		}
	}

	@Override
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		// a unique rectangle is 5.0
		double d = super.getDifficulty();
		// loops are extra 0.1 per cell.
		if ( loopSize > 4 )
			d += (loopSize-4) * 0.1;
		// truncate (round) d to .01
		d = d * 100.0 / 100; // integer division!
		// return difficulty
		return d;
	}

	protected String getTypeName() {
		return loopSize==4 ? "Rectangle" : "Loop";
	}

	@Override
	public String getHintTypeNameImpl() {
		return "Unique "
				+ (loopSize==4 ? "Rectangle" : "Loop")
				+ (loopSize>6 ? " "+loopSize : "")
				+ " type " + typeIndex;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on <b>"+v1+"</b> and <b>"+v2+"</b>";
		return s;
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.csv(loop)+" on "+v1+" and "+v2;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!o.getClass().equals(this.getClass()))
			return false;
		AURTHint other = (AURTHint)o;
		if (this.loopSize != other.loopSize)
			return false;
		return this.loop.containsAll(other.loop);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : loop )
			result = result <<4 ^ c.hashCode();
		return result;
	}
}