/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Html;
import java.util.Set;

/**
 * GEMHintBig is GEM (Graded Equivalence Marks) Type 2+ (Multi) hints. It's
 * unusual in that it has both cell values to set and eliminations, so the
 * applyImpl method is overridden to perform both operations.
 * <p>
 * Based on XColoringHintMulti to display SetPots, redPots, and the
 * markers (colored +'s and -'s).
 *
 * @author Keith Corlett 2021-03-24
 */
public class GEMHintBig extends AHint  {

	private final int v;
	private final int subtype;
	private final Pots setPots;
	private final Set<Cell> cause;
	private final int resultColor;
	private final String steps;
	private final ARegion region;
	private final Idx[][] ons;
	private final Idx[][] offs;

	public GEMHintBig(AHinter hinter, int v, Pots reds, int subtype
			, Set<Cell> cause, int resultColor, String steps, Pots setPots
			, Pots greens, Pots blues, ARegion region, Idx[][] ons
			, Idx[][] offs
	) {
		super(hinter, AHint.MULTI, null, 0, reds, greens, null, blues, null, null);
		this.v = v;
		this.subtype = subtype;
		this.cause = cause;
		this.resultColor = resultColor;
		this.steps = steps;
		this.setPots = setPots;
		this.region = region;
		this.ons = ons;
		this.offs = offs;
	}

	// super does both blue and green, so override again for none.
	// I now paint "the problem" pink, so it stands out a bit more.
	@Override
	public Set<Cell> getAquaCells(int viewNum) {
		return null;
	}

	// The cells which caused this hint. So either:
	// * Type 3: all cells in region, which see cells of this color; or
	// * Type 2: the two-or-more cells in a region which are this color
	@Override
	public Set<Cell> getPinkCells(int viewNum) {
		return cause;
	}

	@Override
	public ARegion[] getPinkos() {
		return Regions.array(region);
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
	public Idx[][] getOns() {
		return ons;
	}

	@Override
	public Idx[][] getOffs() {
		return offs;
	}

	/**
	 * Return Tech.GEM.difficulty plus two bonuses: <ul>
	 *  <li>The number of set-cells * Tech.NakedSingle.difficulty <br>
	 *  because a GEMHintBig can set many cells it's total difficulty includes
	 *  the total difficulty of setting all of those cells individually,
	 *  otherwise switching-off GEM makes SE report that a puzzle is harder
	 *  than it was with GEM, which it, pretty obviously, is not.
	 *  <li>The number of eliminations * 0.01 for consistency with GEMHint.
	 * </ul>
	 * @return Tech.GEM.difficulty plus two bonuses (as described).
	 */
	@Override
	public double getDifficultyTotal() {
		return getDifficulty()
			 + setPots.size() * Tech.NakedSingle.difficulty // cell-set bonus
			 + reds.totalSize() * 0.01; // elimination bonus
	}

	// The GUI sorts hints by Score then Indice, and this is the only hint in
	// Sudoku Explainer which sets multiple cells, hence I override getScore
	// to return 10 * the number of cells set, so any Multi hints are listed
	// BEFORE any "normal" elimination hints.
	@Override
	protected int getScore() {
		return setPots.size() * 10;
	}

	// @return numElims = 10*numCellsSet + numMaybesEliminated.
	@Override
	public int applyImpl(boolean isAutosolving, Grid grid) {
		if ( isInvalid )
			return 0; // invalid hints are not applicable
		// set the setPots, and then eliminate the redPots
		return setPots.setCells(isAutosolving) * 10
			 + super.applyImpl(isAutosolving, grid);
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hinter.tech.name()+" Type " + subtype;
	}

	@Override
	protected String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(greens.ids())
		  .append(CSP).append(blues.ids())
		  .append(ON).append(v)
		  .toString();
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

	private String htmlHintTypeName() {
		return getHintTypeName().replaceFirst("GEM", "sudopedia.org GEM (Graded Equivalence Marks)");
	}

	@Override
	protected String toHtmlImpl() {
		StringBuilder sb = new StringBuilder(20*1024);
		sb.append("<html><body>").append(NL)
		  .append("<h2>").append(htmlHintTypeName()).append("</h2>").append(NL);
		sb.append("Either the <g>green values</g> or the <b1>blue values</b1> are true.").append(NL);
		if ( steps != null ) {
			sb.append("<p>").append(NL)
			  .append("<u>Painting Steps</u>").append(NL)
		      .append("<pre>").append(NL)
		      .append(steps.trim()).append(NL)
		      .append("</pre>").append(NL);
		}
		String results = setPots.toString().replaceAll(MINUS, PLUS);
		sb.append("<p>").append(NL)
		  .append("Therefore <g>we can set <b>").append(results).append("</b></g>").append(NL);
	    // append the pre-made html-snippet in GEMHintExplanation.html
		sb.append("<p>").append(NL)
		  .append(Html.load(this, "GEMHintExplanation.html"));
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

	@Override
	public int hashCode() {
		if ( hc == 0 ) {
			if ( setPots != null )
				hc = setPots.hashCode();
			else if ( reds != null )
				hc = reds.hashCode();
			else
				hc = toString().hashCode();
		}
		return hc;
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			|| ( o != null
			  && o instanceof GEMHintBig
			  && this.hashCode() == ((GEMHintBig)o).hashCode() );
	}
	private int hc;

}
