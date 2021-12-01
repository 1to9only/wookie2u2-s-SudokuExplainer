/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.Validator;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.CSP;

/**
 * XColoringHint is the Extended Coloring hint DTO.
 *
 * @author Keith Corlett 2021-03-03
 */
public class GEMHint extends AHint  {

	private final int v;
	private final String greenCells; // ids of the green cells
	private final String blueCells;
	private final String steps;
	private final Collection<Link> links;
	private final Idx[][] ons;
	private final Idx[][] offs;
	public GEMHint(AHinter hinter, int v, Pots reds
			, Pots greens, Pots blues, Idx[] colorSet, String steps
			, Collection<Link> links, Idx[][] ons, Idx[][] offs
	) {
		super(hinter, reds, greens, null, blues, null, null);
		this.v = v;
		if ( colorSet != null ) {
			this.greenCells = colorSet[0].ids();
			this.blueCells = colorSet[1].ids();
		} else {
			this.greenCells = greens.cells();
			this.blueCells = blues.cells();
		}
		this.steps = steps;
		this.links = links;
		this.ons = ons;
		this.offs = offs;
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
	public Collection<Link> getLinks(int viewNum) {
		return links;
	}

	@Override
	public Idx[][] getOns() {
		return ons;
	}

	@Override
	public Idx[][] getOffs() {
		return offs;
	}

	/**
	 * @return Tech.GEM.difficulty plus an elimination bonus of number of 
	 * eliminations * 0.01.
	 */
	@Override
	public double getDifficultyTotal() {
		return getDifficulty() + (reds.totalSize() * 0.01);
	}

	@Override
	protected String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName()).append(COLON_SP)
		  .append(greenCells).append(CSP).append(blueCells)
		  .append(ON).append(v)
		  .toString();
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+v+"</b>";
		return s;
	}

	private String htmlHintTypeName() {
		return getHintTypeName().replaceFirst("GEM", "sudopedia.org GEM (Graded Equivalence Marks)");
	}

	@Override
	protected String toHtmlImpl() {
		StringBuilder sb = new StringBuilder(10*1024);
		sb.append("<html><body>").append(NL);
		if ( isInvalid )
			sb.append("<h2>").append("<r>INVALID</r> ").append(getHintTypeName()).append("</h2>").append(NL)
			  .append("<r><b>").append(Validator.prevMessage).append("</b></r><p>").append(NL);
		else
			sb.append("<h2>").append(htmlHintTypeName()).append("</h2>").append(NL);
	    sb.append("There are two extended coloring sets:<pre>").append(NL)
		  .append("<g>GREEN: ").append(greenCells).append("</g>").append(NL)
		  .append("<b1>BLUE : ").append(blueCells).append("</b1>").append(NL)
		  .append("</pre>").append(NL)
		  .append("Either the <g>green values</g> or the <b1>blue values</b1> are true.").append(NL);
		if ( steps != null ) {
			sb.append("<p>").append(NL)
			  .append("<u>Painting Steps</u>").append(NL)
			  .append("<pre>").append(NL)
			  .append(steps.trim()).append(NL)
			  .append("</pre>").append(NL);
		}
		sb.append("<p>").append(NL)
		  .append("Therefore <r>we can remove <b>").append(reds.toString()).append("</b></r>.").append(NL);
	    // append the pre-made html-snippet in GEMHintExplanation.html
		sb.append("<p>").append(NL)
		  .append(Html.load(this, "GEMHintExplanation.html"));
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

}
