/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IntQueue;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * ALS-XY-Wing hint holds the data of an Almost Locked Set XY-Wing hint for
 * display in the GUI, and possible later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 21
 */
public class AlsWingHint extends AHint  {

	// NOTE: alss order differs from AlsWing (ergo a,c,b is MENTAL).
	private final Als a; // first
	private final Als b; // middle
	private final Als c; // last
	private final int x;
	private final int y;

	public AlsWingHint(Grid grid, final IHinter hinter, final Pots reds
			, final Als first, final Als middle, final Als last
			, final int x, final int y) {
		// greens, oranges, and blues are now all null because I do all my own
		// presentation, via the getAlss method, new in SudokuGridPanel.
		super(grid, hinter, reds, null, null, null
			, Regions.array(grid.regions[first.regionIndex])
			, Regions.array(grid.regions[middle.regionIndex])
		);
		this.a = first;
		this.b = middle;
		this.c = last;
		this.x = x;
		this.y = y;
	}

	@Override
	public Als[] getAlss() {
		return new Als[]{a, b, c};
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		int s; // the source indice
		// a Set to enforce uniqueness
		final LinkedHashSet<Link> result = new LinkedHashSet<>();
		// a -x-> b
		int[] da = b.vs[x].toArrayNew();
		for ( final IntQueue sq=a.vs[x].indices(); (s=sq.poll())>QEMPTY; )
			for ( int d : da )
				result.add(new Link(s, x, d, x));
		// b -y-> c
		da = c.vs[y].toArrayNew();
		for ( final IntQueue sq=b.vs[y].indices(); (s=sq.poll())>QEMPTY; )
			for ( int d : da )
				result.add(new Link(s, y, d, y));
		// elims are backwards: foreach dst, foreach src to link ONLY elim vals
		// a -z-> elims
		// c -z-> elims
		reds.entrySet().forEach((e) -> {
			final int d = e.getKey();
			for ( int z : VALUESES[e.getValue()] ) {
				link(a.vs[z], d, z, result);
				link(c.vs[z], d, z, result);
			}
		});
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		final StringBuilder sb = SB(64).append("Look for a ").append(getHintTypeName());
		if ( isBig )
			sb.append(IN).append(Als.regionLabels(getAlss(), CSP));
		return sb.toString();
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(Als.regionLabels(getAlss(), CSP));
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "AlsWingHint.html"
			, a.format().append(" on x=").append(x)			 //{0}
			, b.format().append(" on y=").append(y)			 // 1
			, Values.append(c.format().append(" removing z="), reds.candsOf(), CSP, AND)
			  .append(" that see all z in both (a) and (c)") // 2
			, reds.toString()								 // 3
			, debugMessage									 // 4
		);
	}

}
