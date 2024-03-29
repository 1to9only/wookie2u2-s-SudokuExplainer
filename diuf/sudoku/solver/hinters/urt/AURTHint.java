/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyArrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * AURTHint (Abstract Unique Rectangle Hint) contains shared implementation of
 * the various types of Unique Rectangle and Loop Hints.
 */
public abstract class AURTHint extends AHint  {

	// Sorts the hints by difficulty, type ascending
	public static final Comparator<AURTHint> BY_ORDER = (AURTHint h1, AURTHint h2) -> {
		final int d1 = h1.getDifficulty();
		final int d2 = h2.getDifficulty();
		if (d1 < d2)
			return -1;
		if (d1 > d2)
			return 1;
		return h1.typeIndex - h2.typeIndex;
	};

	protected final Cell[] loop;
	protected final int loopSize;
	protected final int v1;
	protected final int v2;
	protected final int typeIndex;

	public AURTHint(final Grid grid, final int typeIndex, final IHinter hinter
			, final Cell[] loop, final int loopSize, final int v1, final int v2
			, final Pots reds) {
		super(grid, hinter, reds);
		this.typeIndex = typeIndex;
		this.loop = loop;
		this.loopSize = loopSize;
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return new Idx(loop);
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		if ( greenPots == null ) {
			Pots pots = new Pots(loopSize, 1F);
			for ( int i=0; i<loopSize; ++i )
				pots.put(loop[i].indice, VSHFT[v1]|VSHFT[v2]); // orange
			pots.removeAll(reds);
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Collection<Link> getLinks(int viewNum) {
		try {
			final int n = this.loopSize;
			final Collection<Link> links = new ArrayList<>(n); // the result
			for ( int i=0; i<n; ++i ) {
				Cell curr = loop[i];
				Cell next = loop[(i+1) % n];
				links.add(new Link(curr, 0, next, 0));
			}
			return links;
		} catch (Exception ex) {
			// I am only ever called in the GUI, so just log it.
			Log.println("AURTHint.getLinks: "+ ex);
			return null;
		}
	}

	@Override
	public int getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		// a unique rectangle is 5.0
		int d = super.getDifficulty();
		// loops are extra 0.1 per cell.
		if ( loopSize > 4 )
			d += (loopSize-4) * 0.1;
		// truncate (round) d to .01
		d = (int)(d * 100.0 / 100); // integer division!
		// return difficulty
		return d;
	}

	protected String getTypeName() {
		return loopSize==4 ? "Rectangle" : "Loop";
	}

	@Override
	public String getHintTypeNameImpl() {
		// Type 7 is a "Unique Rectangle Hidden" (rect only, no loops).
		if ( typeIndex == 7 ) {
			return "Unique Rectangle Hidden";
		}
		return "Unique "
				+ (loopSize==4 ? "Rectangle" : "Loop")
				+ (loopSize>6 ? SP+loopSize : EMPTY_STRING)
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
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(Frmu.csv(loopSize, loop))
		.append(ON).append(v1).append(AND).append(v2);
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
		return MyArrays.containsAll(this.loop, this.loopSize, other.loop, other.loopSize);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : loop )
			result = result<<4 ^ c.indice;
		return result;
	}
}
