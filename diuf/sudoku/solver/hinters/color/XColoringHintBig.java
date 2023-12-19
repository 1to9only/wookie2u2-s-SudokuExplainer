/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.CSP;

/**
 * XColoringHintBig is the DTO for Type 2 and Type 3 Extended Coloring hints.
 * The "big" alludes to the fact that it sets multiple cells. This is the only
 * hint-type in the whole of Sudoku Explainer which does so. Note that links
 * are always empty for Type 2 hints, but there is really no need for two hint
 * types, so long as we accept that links are always empty for Type 2.
 *
 * @author Keith Corlett 2021-03-03
 */
public class XColoringHintBig extends AHint  {

	private final int v;
	private final int subtype;
	private final String greenCellIds;
	private final String blueCellIds;
	private final Pots setPots;
	private final Set<Integer> cause;
	private final int setPotsColor;
	private final String steps;
	private final Collection<Link> links;
	private final ARegion region;

	public XColoringHintBig(Grid grid, IHinter hinter, int v, int subtype
			, Idx[] colorSet, Set<Integer> cause, int setPotsColor, String steps
			, Pots setPots, Pots greens, Pots blues, Collection<Link> links
			, Pots oranges, ARegion region) {
		super(grid, hinter, AHint.MULTI, null, 0
		   , null, greens, oranges, blues // nb reds are null (not empty)
		   , null, null);
		this.v = v;
		this.subtype = subtype;
		if ( colorSet != null ) { // XColoring
			this.greenCellIds = colorSet[0].ids();
			this.blueCellIds = colorSet[1].ids();
		} else { // MedusaColoring
			this.greenCellIds = greens.ids();
			this.blueCellIds = blues.ids();
		}
		this.cause = cause;
		this.setPotsColor = setPotsColor;
		this.steps = steps;
		this.setPots = setPots;
		this.links = links;
		this.region = region;
	}

	@Override
	public ARegion[] getPinkRegions() {
		return Regions.array(region);
	}

	// super does both blue and green, so override again for none.
	// I now paint "the problem" pink, so it stands out a bit more.
	@Override
	public Set<Integer> getAquaBgIndices(int viewNum) {
		return null;
	}

	// The cells which caused this hint. So either:
	// * Type 3: all cells in region, which see cells of this color; or
	// * Type 2: the two-or-more cells in a region which are this color
	@Override
	public Set<Integer> getPinkBgIndices(int viewNum) {
		return cause;
	}

	@Override
	public Set<Integer> getBlueBgIndices(int viewNum) {
		return blues.keySet();
	}

	@Override
	public Set<Integer> getGreenBgIndices(int viewNum) {
		return greens.keySet();
	}

	// These are painted as larger green/blue digits in the SudokuGridPanel
	@Override
	public Pots getSetPots() {
		return setPots;
	}

	/**
	 * Paint setPots in green or blue
	 *
	 * @return Constants.GREEN, Constants.BLUE, or -1 meaning NONE
	 */
	@Override
	public int getSetPotsColor() {
		return setPotsColor;
	}

	// The GUI sorts hints by Score then Indice, and this is the only hint in
	// Sudoku Explainer which sets multiple cells, hence I override getScore
	// to return 10 * the number of cells set, so any Multi hints are listed
	// BEFORE any "normal" elimination hints.
	@Override
	protected int getScore() {
		return setPots.size() * 10;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		return links;
	}

	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		return setPots.setCells(isAutosolving, grid) * 10;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hinter.getTechName()+" Type " + subtype;
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(greenCellIds).append(CSP).append(blueCellIds).append(ON).append(v);
	}

	@Override
	public String toFullString() {
		return toStringImpl()+" ("+setPots.toString().replaceAll(MINUS, PLUS)+")";
	}

	@Override
	protected String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+v+"</b>";
		return s;
	}

	private String explanation() {
		switch ( subtype ) {
			case 1: return "Eliminations: Some cell values were eliminated by smartypantsness.";
			case 2: return "Contradiction 1: Multiple cells in a region are the same color, so the OTHER color is true.";
			case 3: return "Contradiction 2: All cells in a region see cells of the same color, so the OTHER color is true.";
			default: return "Subtype "+subtype+": Alien Ranga in parliament, so <k>run away</k>, or something like that.";
		}
	}

	private String htmlHintTypeName() {
		// handles both XColoring and Medusa3D hint-types
		return "sudopedia.org "+getHintTypeName();
	}

	@Override
	protected String toHtmlImpl() {
		StringBuilder sb = SB(1024);
		sb.append("<html><body>").append(NL)
		  .append("<h2>").append(htmlHintTypeName()).append("</h2>").append(NL);
		if ( subtype != 1 ) // not wanted for "normal" elimination hints
			sb.append(explanation()).append(NL).append("<p>").append(NL);
		sb.append("There are two extended coloring sets:<pre>").append(NL)
		  .append("<g>GREEN: ").append(greenCellIds).append("</g>").append(NL)
		  .append("<b1>BLUE : ").append(blueCellIds).append("</b1>").append(NL)
		  .append("</pre>").append(NL);
		sb.append("Either the <g>green values</g> or the <b1>blue values</b1> are true.").append(NL);
		if ( steps != null ) {
			sb.append("<p>").append(NL)
			  .append("<u>Steps</u>").append(NL)
		      .append("<pre>").append(NL)
		      .append(steps.trim()).append(NL)
		      .append("</pre>").append(NL);
		}
		String results = setPots.toString().replaceAll(MINUS, PLUS);
		sb.append("<p>").append(NL)
		  .append("Therefore <g>we can set <b>").append(results).append("</b></g>");
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

}
