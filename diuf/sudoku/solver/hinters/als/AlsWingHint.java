/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;


/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsWingHint extends AHint  {

	private final Als a;
	private final Als b;
	private final Als c;
	private final int x;
	private final int y;
	private final String zString;

	public AlsWingHint(AHinter hinter, Pots redPots, Als a, Als b, Als c
			, int x, int y, String zString) {
		// greens, oranges, and blues are now all null because I do all my own
		// presentation, via the getAlss method, new in SudokuGridPanel.
		super(hinter, redPots, null, null, null, Regions.list(a.region)
				, Regions.list(b.region));
		this.a = a;
		this.b = b;
		this.c = c;
		this.x = x;
		this.y = y;
		this.zString = zString;
	}

	@Override
	public Collection<Als> getAlss() {
		return Als.list(a, b, c);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += IN+Frmu.csv(Als.regionsList(getAlss()));
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(Als.regionsList(getAlss())))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "AlsWingHint.html"
			, a.format()			//{0}
			, b.format()			// 1
			, c.format()			// 2
			, redPots.toString()	// 3
			, Integer.toString(x)	// 4
			, Integer.toString(y)	// 5
			, zString				// 6
		);
	}
}
