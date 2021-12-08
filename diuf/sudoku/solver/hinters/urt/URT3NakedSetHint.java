/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.urt;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The hint DTO for: Unique Rectangle Type 3 with Naked Set.
 */
public final class URT3NakedSetHint extends AURTHint {

	private final Cell c1;
	private final Cell c2;
	// values other than v1 and v2, the two values common to all cells in a URT
	private final int extraVals;
	private final ARegion region;
	// other cells of the naked set (not c1 or c2)
	// nb: equals requires a Set!
	private final Set<Cell> otherCellsSet;
	// values in the naked set
	private final int nkdSetVals;

	// local degree overridding AHint.degree is required because it's too damn
	// difficult to set the inherited one to anything other than the default
	// value, which is hardcoded in Tech. I probably should have implemented it
	// as a method, which I could have overridden. Sigh.
	private final int degree;

	/**
	 * Construct a new URT3NakedSetHint.
	 * @param hinter
	 * @param loop
	 * @param loopSize
	 * @param v1
	 * @param v2
	 * @param redPots
	 * @param c1
	 * @param c2
	 * @param extraVals
	 * @param region
	 * @param otherCells WARNING: DO NOT STORE otherCells, IT IS CACHED!
	 * @param nkdSetVals
	 */
	public URT3NakedSetHint(UniqueRectangle hinter, Cell[] loop, int loopSize
			, int v1, int v2, Pots redPots, Cell c1, Cell c2, int extraVals
			, ARegion region, Cell[] otherCells, int nkdSetVals) {
		super(3, hinter, loop, loopSize, v1, v2, redPots);
		this.c1 = c1;
		this.c2 = c2;
		this.extraVals = extraVals;
		this.region = region;
		this.otherCellsSet = new LinkedHashSet<>(Arrays.asList(otherCells));
		this.nkdSetVals = nkdSetVals;
		this.degree = VSIZE[nkdSetVals];
	}

	@Override
	public double getDifficulty() {
		// get my base difficulty := hinter.tech.difficulty
		double d = super.getDifficulty();
		// Pair+=0.1, Triple+=0.2, Quad+=0.3
		d += (degree-1) * 0.1;
		// return the result
		return d;
	}

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots();
			pots.put(c1, extraVals);
			pots.put(c2, extraVals);
			for ( Cell c : otherCellsSet )
				pots.upsert(c, nkdSetVals, false);
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public ARegion[] getBases() {
		return Regions.array(region);
	}

	@Override
	public String getHintTypeNameImpl() {
		return super.getHintTypeNameImpl() + " Naked Set";
	}

	/**
	 * Overridden to differentiate hints with different naked sets.
	 * <p>
	 * <b>NB: there is no need to override {@code hashCode()}!</b>
	 * @return does this s__t equal this other s__t?
	 */
	@Override
	public boolean equals(Object o) {
		if ( !(o instanceof URT3NakedSetHint)
		  || !super.equals(o) )
			return false;
		URT3NakedSetHint other = (URT3NakedSetHint)o;
		if ( this.region != other.region
		  || this.degree != other.degree )
			return false;
		return this.nkdSetVals == other.nkdSetVals
			&& this.otherCellsSet.equals(other.otherCellsSet);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "URT3NakedSetHint.html"
			, getTypeName()					//{0}
			, v1							// 1
			, v2							// 2
			, Frmu.csv(loopSize, loop)		// 3
			, c1.id							// 4
			, c2.id							// 5
			, Values.orString(extraVals)	// 6
			, GROUP_NAMES[degree-2]			// 7
			, Frmu.and(otherCellsSet)		// 8
			, Values.andString(nkdSetVals)	// 9
			, region.id						//10
			, reds.toString()				//11
		);
	}
}