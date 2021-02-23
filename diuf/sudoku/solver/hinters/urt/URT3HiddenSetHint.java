/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import java.util.List;


public final class URT3HiddenSetHint extends AURTHint {

	private final Cell c1;
	private final Cell c2;
	private final Values otherValues;
	private final ARegion region;
	private final Values hdnSetVals;
	private final Indexes hdnSetIdxs; // indexes of the hidden set
	private final int[] hdnSetIdxsArray; // indexes of the hidden set

	// NB: Hide AHint's degree field so I can set it to the size of the hidden
	// set. Note that degree is known at Hinter-creation-time pretty-much-
	// everywhere-else, it's only URT's and BUG's which want to create hidden &
	// naked sets of "arbitrary" size (IIRC), and this is the best way I can
	// think of to do so. If you've got better ideas then go for it.
	private final int degree;

	public URT3HiddenSetHint(UniqueRectangle hinter, List<Cell> loop
			, int v1, int v2, Pots redPots, Cell c1, Cell c2
			, Values otherValues, Values hdnSetVals, ARegion region
			, Indexes hdnSetIdxs) {
		super(3, hinter, loop, v1, v2, redPots);
		this.c1 = c1;
		this.c2 = c2;
		this.otherValues = otherValues;
		this.hdnSetVals = hdnSetVals;
		this.region = region;
		this.hdnSetIdxs = hdnSetIdxs;
		this.hdnSetIdxsArray = hdnSetIdxs.toArray();
		this.degree = hdnSetIdxs.size;
	}

	@Override
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		double d = super.getDifficulty();
		// Pair+=0.1, Triple+=0.2 Quad+=0.3
		d += (degree-1) * 0.1;
		// return the result
		return d;
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots();
			for ( int i=0; i<degree; ++i )
				pots.put(region.cells[hdnSetIdxsArray[i]], hdnSetVals);
			// Add the two cells of the loop
			pots.upsert(c1, hdnSetVals);
			pots.upsert(c2, hdnSetVals);
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public String getHintTypeNameImpl() {
		return super.getHintTypeNameImpl() + " Hidden Set";
	}

	/** Overridden to differentiate hints with different naked sets.
	 * <p>NB: there is no need to override <tt>hashCode()</tt>. */
	@Override
	public boolean equals(Object o) {
		if ( !(o instanceof URT3HiddenSetHint)
		  && !super.equals(o) )
			return false;
		URT3HiddenSetHint other = (URT3HiddenSetHint)o;
		return this.region == other.region
			&& this.degree == other.degree
			&& this.hdnSetVals.bits == other.hdnSetVals.bits
			&& this.hdnSetIdxs.bits == other.hdnSetIdxs.bits;
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT3HiddenSetHint.html"
				, getTypeName()			// {0}
				, v1					//  1
				, v2					//  2
				, Frmt.csv(loop)		//  3
				, c1.id					//  4
				, c2.id					//  5
				, Frmt.or(otherValues)	//  6
				, GROUP_NAMES[hdnSetVals.size-2] // 7
				, Frmt.and(region.atNew(hdnSetIdxsArray)) // 8
				, Frmt.and(hdnSetVals)	//  9
				, region.id				// 10
		);
	}
}