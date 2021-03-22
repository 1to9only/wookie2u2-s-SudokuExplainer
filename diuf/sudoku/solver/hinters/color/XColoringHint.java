/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.Set;

/**
 * XColoringHint is the Extended Coloring hint DTO.
 *
 * @author Keith Corlett 2021-03-03
 */
public class XColoringHint extends AHint {

	private final int v;
	private final Idx[] colorSet;
	private final String steps;
	public XColoringHint(AHinter hinter, int v
			, Pots reds, Pots greens, Pots blues
			, Idx[] colorSet, StringBuilder steps
	) {
		super(hinter, reds, greens, null, blues, null, null);
		this.v = v;
		this.colorSet = new Idx[]{new Idx(colorSet[0]), new Idx(colorSet[1])};
		this.steps = steps.toString();
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return null;
	}

	@Override
	public Set<Cell> getBlueCells(int viewNum) {
		return blues.keySet();
	}

	@Override
	public Set<Cell> getGreenCells(int viewNum) {
		return greens.keySet();
	}

	@Override
	protected String toStringImpl() {
		return getHintTypeName()+": "+colorSet[0].ids()+", "+colorSet[1].ids()
			 + " on " + v;
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+v+"</b>";
		return s;
	}

	@Override
	protected String toHtmlImpl() {
		StringBuilder sb = new StringBuilder(512);
		sb.append("<html><body>").append(NL);
		if ( isInvalid ) {
			sb.append("<h2>").append("<r>INVALID</r> ").append(getHintTypeName()).append("</h2>").append(NL)
			  .append(invalidity).append("<p>").append(NL);
		} else
			sb.append("<h2>").append(getHintTypeName()).append("</h2>").append(NL);
	    sb.append("There are two extended coloring sets on the value <b>").append(v).append("</b>:<pre>").append(NL)
		  .append("<g>GREEN: ").append(colorSet[0].ids()).append("</g>").append(NL)
		  .append("<b1>BLUE : ").append(colorSet[1].ids()).append("</b1>").append(NL)
		  .append("<u>Steps</u>").append(NL)
		  .append(steps.trim()).append(NL)
		  .append("</pre>").append(NL)
		  .append("Because either the <b1>blue</b1> cells or the <g>green</g> cells must contain the value <b>")
		  .append(v).append("</b>, any cell seeing members of <b>both</b> sets can be eliminated.").append(NL)
		  .append("<p>").append(NL)
		  .append("Therefore <r>we can remove <b>").append(redPots.toString()).append("</b></r>.").append(NL);
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

}
