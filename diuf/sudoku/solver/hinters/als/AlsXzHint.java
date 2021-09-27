/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collection;


/**
 * ALS-XZ Hint holds all the data of an ALS-XZ hint for display in the GUI,
 * and later apply-cation.
 *
 * @author Keith Corlett 2020 Apr 17
 */
public class AlsXzHint extends AHint  {

	private final Als a;
	private final Als b;
	private final int zsZapped; // bitset of z-values removed by single-link
	private final boolean anyDoubleLinked;
	private final String debugMessage, rccsString;

	public AlsXzHint(AHinter hinter, Als a, Als b, int zsZapped, Pots reds,
			boolean anyDoubleLinked, String rccsString, String debugMessage) {
		// nb: what are normally greens are oranges here
		super(hinter, reds, null, null, null, Regions.list(a.region)
				, Regions.list(b.region));
		this.a = a;
		this.b = b;
		this.zsZapped = zsZapped;
		this.anyDoubleLinked = anyDoubleLinked;
		this.rccsString = rccsString;
		this.debugMessage = debugMessage;
	}

	@Override
	public Collection<Als> getAlss() {
		return Als.list(a, b);
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " in "+a.region.id+" and "+b.region.id
			  +" on "+rccsString;
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName()).append(":")
		  .append(" in ").append(a.region.id).append(" and ").append(b.region.id)
		  .append(" on ").append(rccsString);
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		final String filename = anyDoubleLinked
				? "AlsXzHintBig.html"
				: "AlsXzHint.html";
		// Calculate ONCE at HTML-time: we need "" (not "-") if no z-values
		// removed by single-link, else we see a stray - in the html.
		final String zsString = zsZapped==0 ? ""
				: Values.andS(zsZapped);
		final String zBlurb = zsZapped==0 ? ""
				: "<br>and another (non-restricted) common candidate z = "
				  +"<b>"+zsString+"</b>";
		return Html.produce(this, filename
			, a.toString()			//{0}
			, b.toString()			// 1
			, rccsString			// 2 x's
			, zsString				// 3 z's
			, redPots.toString()	// 4
			, debugMessage			// 5
			, zBlurb				// 6
		);
	}

}
