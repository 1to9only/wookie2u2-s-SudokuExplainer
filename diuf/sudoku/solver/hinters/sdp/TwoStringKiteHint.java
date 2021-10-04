/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.List;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;

/**
 * TwoStringKiteHint is the DTO for TwoStringKite hints. It's presented via
 * toString (fast) in LST and GUI, and via toHtml (slow verbose) only in GUI.
 *
 * @author Bernhard Hobiger (HoDoKu) hacked into SE by Keith Corlett
 */
public class TwoStringKiteHint extends AHint  {

	private final int redValue; // the value to remove
	private final Cell[] rowPair;
	private final Cell[] colPair;
	public TwoStringKiteHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots greens, Pots blues, Pots reds
			, Cell[] rowPair, Cell[] colPair) {
		super(hinter, reds, greens, null, blues, bases, covers);
		this.redValue = value;
		this.rowPair = rowPair;
		this.colPair = colPair;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+Integer.toString(redValue);
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(greens.keySet()))
		  .append(ON).append(Integer.toString(redValue))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		Cell redCell = redPots.firstKey();
		return Html.produce(this, "TwoStringKiteHint.html"
			, Integer.toString(redValue)	//{0}
			, covers.get(0).id				// 1
			, covers.get(1).id				// 2
			, bases.get(0).id				// 3
			, redCell.id					// 4
			, rowPair[0].id					// 5
			, rowPair[1].id					// 6
			, colPair[0].id					// 7
			, colPair[1].id					// 8
			, redPots.toString()			// 9
		);
	}

}
