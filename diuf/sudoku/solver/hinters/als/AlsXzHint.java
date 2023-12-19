/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.REGION_LABELS;
import static diuf.sudoku.Grid.SEES;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.AND;
import static diuf.sudoku.utils.Frmt.COLON;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IntQueue;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * ALS-XZ Hint holds all the data of an ALS-XZ hint for display in the GUI,
 * and later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 17
 */
public class AlsXzHint extends AHint  {

	private final Als a;
	private final int v1;
	private final int v2; // usually 0
	private final Als b;
	private final int zsZapped; // bitset of z-values removed by single-link
	private final boolean doubleLinked;
	private final String rcValuesString;

	public AlsXzHint(final Grid grid, final IHinter hinter, final Als a
			, final Als b, final int v1, final int v2, final int zsZapped
			, final Pots reds, final boolean doubleLinked) {
		// nb: what are normally greens are oranges here
		super(grid, hinter, reds, null, null, null
			, Regions.array(grid.regions[a.regionIndex])
			, Regions.array(grid.regions[b.regionIndex])
		);
		this.a = a;
		this.v1 = v1;
		this.v2 = v2;
		this.b = b;
		this.zsZapped = zsZapped;
		this.doubleLinked = doubleLinked;
		// build a string of the RCC-value/s
		this.rcValuesString = v2==0 ? Integer.toString(v1)
				: Integer.toString(v1)+AND+Integer.toString(v2);
	}

	@Override
	public Als[] getAlss() {
		return new Als[]{a, b};
	}

	/**
	 * AlsXz needs its own linkD method that handles doubleLinked elims,
	 * which are unique in that they are associated with one ALS only.
	 *
	 * @param srcAls is the source ALS
	 * @param dst destination Cell, not just it is index, because I read it is
	 *  sees array in order to handle doubleLinked elims from one ALS only
	 * @param z the z-value
	 * @param result to add links to
	 */
	private static void linkD(final Als srcAls, final int dst, final int z, final Collection<Link> result) {
		final Idx vs = srcAls.vs[z];
		if ( vs!=null && srcAls.vBudsB[z][dst] ) {
			int s; // source indice
			for ( final IntQueue q=vs.indices(); (s=q.poll())>QEMPTY; )
				if ( SEES[dst][s] )
					result.add(new Link(s, z, dst, z));
		}
	}

	@Override
	public Collection<Link> getLinks(final int viewNum) {
		// use a Set for uniqueness (which should not now be a problem, sigh)
		final LinkedHashSet<Link> result = new LinkedHashSet<>(32, 0.75F);
		// a -x-> b
		link(a.vs[v1], b.vs[v1], v1, result);
		// a -other x-> b for doubleLinked hints
		if ( v2 > 0 ) // NEVER negative
			link(a.vs[v2], b.vs[v2], v2, result);
		// a -z-> elims
		// b -z-> elims
		// backwards: foreach dst, foreach src to link only elimed values.
		// Only cells seeing each elim to handle doubleLinked elims.
		reds.entrySet().forEach((e) -> {
			final int dst = e.getKey();
			// foreach value that is eliminated from this cell
			// and this cell sees all zs in this ALS only.
			// BUG: May link from irrelevant ALS to elims, occassionally.
			for ( final int z : VALUESES[e.getValue()] ) {
				linkD(a, dst, z, result);
				linkD(b, dst, z, result);
			}
		});
		return result;
	}

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += IN+REGION_LABELS[a.regionIndex]
			  +AND+REGION_LABELS[b.regionIndex]+ON+rcValuesString;
		return s;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON)
		.append(IN).append(REGION_LABELS[a.regionIndex])
		.append(AND).append(REGION_LABELS[b.regionIndex])
		.append(ON).append(rcValuesString);
	}

	@Override
	public String toHtmlImpl() {
		final String filename = doubleLinked
				? "AlsXzHintBig.html"
				: "AlsXzHint.html";
		// Calculate ONCE at HTML-time: we need "" (not "-") if no z-values
		// removed by single-link, else we see a stray - in the html.
		final String zsString = zsZapped==0 ? "" : Values.andString(zsZapped);
		final String zBlurb = zsZapped==0 ? ""
				: "<br>and another (non-restricted) common candidate z = "
				  +"<b>"+zsString+"</b>";
		return Html.produce(this, filename
			, a.toString()		//{0}
			, b.toString()		// 1
			, rcValuesString	// 2 x's
			, zsString			// 3 z's
			, reds.toString()	// 4
			, debugMessage		// 5
			, zBlurb			// 6
		);
	}

}
