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
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Set;


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
	public Set<Cell> getAquaCells(int viewNumUnused) {
		// nb: what are normally greens are oranges here
		return oranges.keySet();
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
		final String filename, gonners;
		if ( anyDoubleLinked ) {
			filename = "AlsXzDblLnkdHint.html";
			gonners = redPots.toString(); // "cell-values ..."
		} else {
			filename = "AlsXzHint.html";
			gonners = Frmt.and(redPots.keySet()); // cell.id's only
		}
		return Html.produce(this, filename
			, a.region.id+": "+aCells	//{0}
			, b.region.id+": "+bCells	// 1
			, rccMaybes					// 2
			, Values.andS(pinkBits)		// 3
			, gonners					// 4
			, debugMessage				// 5
		);
	}

}
