/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.Set;

/**
 * XColoringHintMulti is the DTO for Type 2 and Type 3 Extended Coloring hints.
 * The "multi" alludes to the fact that it sets multiple cells. This is the
 * only hint-type in the whole of Sudoku Explainer which does so. Note that
 * links are always empty for Type 2 hints, but there's really no need for two
 * hint types, so long as we accept that links are always empty for Type 2.
 *
 * @author Keith Corlett 2021-03-03
 */
public class XColoringHintMulti extends AHint {

	private final int v;
	private final int subtype;
	private final Idx[] colorSet;
	private final Pots setPots;
	private final Set<Cell> cause;
	private final int resultColor;
	private final String steps;
	private final Collection<Link> links;
	public XColoringHintMulti(AHinter hinter, int v, int subtype, Idx[] colorSet
			, Set<Cell> cause, int resultColor, String steps
			, Pots setPots, Pots greens, Pots blues, Collection<Link> links) {
		// AHinter hinter, int type, Cell cell, int value
		// , Pots redPots, Pots greens, Pots oranges, Pots blues
		// , List<ARegion> bases, List<ARegion> covers
		super(hinter, AHint.MULTI, null, 0
		   , null, greens, null, blues
		   , null, null);
		this.v = v;
		this.subtype = subtype;
		this.colorSet = colorSet;
		this.cause = cause;
		this.resultColor = resultColor;
		this.steps = steps;
		this.setPots = setPots;
		this.links = links;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		// the cells which caused this hint. So either:
		// * Type 3: all cells in region, which see cells of this color; or
		// * Type 2: the two-or-more cells in a region which are this color
		return cause;
	}

	@Override
	public Set<Cell> getBlueCells(int viewNum) {
		return blues.keySet();
	}

	@Override
	public Set<Cell> getGreenCells(int viewNum) {
		return greens.keySet();
	}

	// These are painted as larger green/blue digits in the SudokuGridPanel
	@Override
	public Pots getResults() {
		return setPots;
	}

	// Paint as green/blue digits
	@Override
	public int getResultColor() {
		return resultColor;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		return links;
	}

	// @return numElims = 10*numCellsSet + numMaybesEliminated.
	@Override
	public int apply(boolean isAutosolving, boolean isNoisy) {
		int numElims = 0;
		for ( Cell c : setPots.keySet() )
			numElims += c.set(v, 0, isAutosolving, null) * 10;
		return numElims;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return "XColoring Type " + subtype;
	}

	@Override
	protected String toStringImpl() {
		return getHintTypeName()+": "+colorSet[0].ids()+", "+colorSet[1].ids()
			 + " on " + v;
	}

	@Override
	public String toFullString() {
		return toStringImpl()+" ("+setPots.toString().replaceAll("-", "+")+")";
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
		StringBuilder sb = new StringBuilder(1024);
		sb.append("<html><body>").append(NL)
		  .append("<h2>").append(getHintTypeName()).append("</h2>").append(NL);
		switch ( subtype ) {
			case 2: sb.append("Type 2: More than one cell in a region is the same color,").append(NL); break;
			case 3: sb.append("Type 3: All cells in a region see cells of the same color,").append(NL); break;
			default: sb.append("Type ").append(subtype).append(": Alien ranga's have invaded parliament,").append(NL); break;
		}
		sb.append(" which is invalid, so the OTHER color is true.").append(NL)
		  .append("<p>").append(NL)
		  .append("There are two extended coloring sets:<pre>").append(NL)
		  .append("<g>GREEN: ").append(colorSet[0].ids()).append("</g>").append(NL)
		  .append("<b1>BLUE : ").append(colorSet[1].ids()).append("</b1>").append(NL)
		  .append("</pre><u>Steps</u><pre>").append(NL)
		  .append(steps.trim()).append(NL)
		  .append("</pre>").append(NL)
		  .append("<p>").append(NL)
		  .append("Therefore <g>we can set <b>").append(setPots.cells()).append("</b>")
			.append(" to <b>").append(v).append("</b></g>.").append(NL)
		  .append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

}
