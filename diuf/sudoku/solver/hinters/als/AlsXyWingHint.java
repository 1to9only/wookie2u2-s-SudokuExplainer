/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;


/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsXyWingHint extends AHint {

	private final Pots orangePots;
	private final Pots bluePots; // common candidates
	private final List<ARegion> bases;
	private final List<ARegion> covers;
	private final Als a;
	private final Als b;
	private final Als c;
	private final int x;
	private final int y;
	private final String z;

	public AlsXyWingHint(
			  AHinter hinter
			, Pots redPots, Pots orangePots, Pots bluePots
			, List<ARegion> bases, List<ARegion> covers
			, Als a, Als b, Als c
			, int x, int y, String z
	) {
		super(hinter, redPots);
		this.orangePots = orangePots;
		this.bluePots = bluePots;
		this.bases = bases;
		this.covers = covers;
		this.a = a;
		this.b = b;
		this.c = c;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNumUnused) {
		return orangePots.keySet();
	}

	@Override
	public List<ARegion> getBases() {
		return bases;
	}

	@Override
	public List<ARegion> getCovers() {
		return covers;
	}

	@Override
	public Pots getOranges(int viewNumUnused) {
		return orangePots; // note that our only greens are the oranges
	}

	@Override
	public Pots getReds(int viewNumUnused) {
		return redPots;
	}

	@Override
	public Pots getBlues(Grid gridUnused, int viewNumUnused) {
		return bluePots; // common candidates
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + getHintTypeName()
			// nb: cast to Object[] for nonvargs call and suppress warning
			+ (isBig ? " in "+Frmt.interleave(bases, covers) : "");
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.interleave(bases, covers);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "AlsXyWingHint.html"
			, a.format()			//{0}
			, b.format()			// 1
			, c.format()			// 2
			, redPots.format()		// 3
			, Integer.toString(x)	// 4
			, Integer.toString(y)	// 5
			, z						// 6
		);
	}
}
