/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.CSP;
import static diuf.sudoku.utils.Frmt.IN;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.LinkedHashSet;

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
	private final String zs;
	private final Rcc[] rcs; // for DEBUG_MODE only (normally null)
	String debugMessage; // set by AlsWing directly AFTER AlsWingHint created

	public AlsWingHint(final AHinter hinter, final Pots reds, final Als a
			, final Als b, final Als c, final int x, final int y
			, final Rcc[] rcs) {
		// greens, oranges, and blues are now all null because I do all my own
		// presentation, via the getAlss method, new in SudokuGridPanel.
		super(hinter, reds, null, null, null, Regions.array(a.region), Regions.array(b.region));
		this.a = a;
		this.b = b;
		this.c = c;
		this.x = x;
		this.y = y;
		this.zs = Values.toString(reds.valuesOf(), CSP, AND);
		this.rcs = rcs;
	}

	@Override
	public Als[] getAlss() {
		return new Als[]{a, b, c};
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		// Use a Set for uniqueness!
		final LinkedHashSet<Link> result = new LinkedHashSet<>();
		// a -x-> b
		a.vs[x].forEach((s)->b.vs[x].forEach(
			(d)->result.add(new Link(s, x, d, x))));
		// b -y-> c
		b.vs[y].forEach((s)->c.vs[y].forEach(
			(d)->result.add(new Link(s, y, d, y))));
		// elims are backwards: foreach dst, foreach src to link ONLY elim vals
		// a -z-> elims
		// c -z-> elims
		reds.entrySet().forEach((e) -> {
			final int d = e.getKey().i;
			for ( int z : VALUESES[e.getValue()] ) {
				link(a.vs[z], d, z, result);
				link(c.vs[z], d, z, result);
			}
		});
		return result;
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

	private String format(Als als) {
		if ( AlsWing.DEBUG_MODE ) {
			// prepend the ALS index
			return als.index+": "+als.format();
		} else {
			return als.format();
		}
	}
	
	@Override
	public String toHtmlImpl() {
		if ( AlsWing.DEBUG_MODE ) {
			debugMessage = Html.colorIn("<p><b2>"+java.util.Arrays.toString(rcs)+"</b2>");
		} else {
			debugMessage = "";
		}
		return Html.produce(this, "AlsWingHint.html"
			, format(a)+" on x="+Integer.toString(x)	//{0}
			, format(b)+" on y="+Integer.toString(y)	// 1
			, format(c)+" removing z="+zs+" that see all z's in both (a) and (c)" // 2
			, reds.toString()							// 3
			, debugMessage								// 4
		);
	}

}
