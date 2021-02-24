/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.Set;


/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsXyWingHint extends AHint implements IActualHint {

	private final Als a;
	private final Als b;
	private final Als c;
	private final int x;
	private final int y;
	private final String z;

	public AlsXyWingHint(
			  AHinter hinter
			, Pots redPots, Pots orangePots, Pots bluePots
			, Als a, Als b, Als c
			, int x, int y, String z
	) {
		// nb: what are normally greens are oranges here.
		super(hinter, redPots, null, orangePots, bluePots
				, Regions.list(a.region), Regions.list(b.region));
		this.a = a;
		this.b = b;
		this.c = c;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Collection<Als> getAlss() {
		return Als.list(a, b, c);
	}

//	@Override
//	public Set<Cell> getAquaCells(int viewNumUnused) {
//		// nb: what are normally greens are oranges here.
//		return oranges.keySet();
//	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in "+Frmt.csv(Als.regions(getAlss()));
		return s;
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()+": "+Frmt.csv(Als.regions(getAlss()));
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "AlsXyWingHint.html"
			, a.format()			//{0}
			, b.format()			// 1
			, c.format()			// 2
			, redPots.toString()	// 3
			, Integer.toString(x)	// 4
			, Integer.toString(y)	// 5
			, z						// 6
		);
	}
}
