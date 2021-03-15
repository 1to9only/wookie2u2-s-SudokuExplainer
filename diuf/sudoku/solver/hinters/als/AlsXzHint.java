/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
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
public class AlsXzHint extends AHint implements IActualHint {

	private final Als a;
	private final Als b;
	private final int pinkBits;
	private final boolean anyDoubleLinked;
	private final String debugMessage, rccMaybes, aCells, bCells;

	public AlsXzHint(AHinter hinter, Als a, Als b, int pinkBits
			, Pots orangePots, Pots bluePots, Pots redPots
			, boolean anyDoubleLinked
			, String rccMaybes // pass this in coz we need the rcc to get it
			, String aCells, String bCells // pass these in coz we need the grid to get them
			, String debugMessage
	) {
		// nb: what are normally greens are oranges here
		super(hinter, redPots, null, orangePots, bluePots
				, Regions.list(a.region), Regions.list(b.region));
		this.a = a;
		this.b = b;
		this.pinkBits = pinkBits;
		this.anyDoubleLinked = anyDoubleLinked;
		this.rccMaybes = rccMaybes;
		this.aCells = aCells;
		this.bCells = bCells;
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
			  +" on "+rccMaybes;
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName()).append(":")
		  .append(" in ").append(a.region.id).append(" and ").append(b.region.id)
		  .append(" on ").append(rccMaybes);
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		final String filename = anyDoubleLinked
				? "AlsXzDblLnkdHint.html"
				: "AlsXzHint.html";
		String zBlurb = pinkBits == 0 ? ""
				: "<br>and another (non-restricted) common candidate z = "
				  +"<b>"+Values.andS(pinkBits)+"</b>";
		return Html.produce(this, filename
			, a.toString()			//{0}
			, b.toString()			// 1
			, rccMaybes				// 2 x's
			, Values.andS(pinkBits)	// 3 z's
			, redPots.toString()	// 4
			, debugMessage			// 5
			, zBlurb				// 6
		);
	}

}
