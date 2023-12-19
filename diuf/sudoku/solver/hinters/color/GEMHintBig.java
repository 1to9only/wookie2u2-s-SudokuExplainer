/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Constants;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Html;
import java.util.Set;

/**
 * GEMHintBig is GEM (Graded Equivalence Marks) Type 2+ (Multi) hints. It is
 * unusual in that it has both cell values to set and eliminations, so the
 * applyImpl method is overridden to perform both operations.
 * <p>
 * Based on XColoringHintBig to display SetPots, redPots, and the
 * markers (colored +s and -s).
 *
 * @author Keith Corlett 2021-03-24
 */
public class GEMHintBig extends AHint  {

	private final int v;
	private final int subtype;
	private final Pots setPots;
	private final Idx cause;
	private final int goodColor;
	private final String steps;
	private final ARegion region;
	private final Idx[][] ons;
	private final Idx[][] offs;

	public GEMHintBig(Grid grid, IHinter hinter, int v, Pots reds, int subtype
			, Idx cause, int goodColor, String steps, Pots setPots
			, Pots greens, Pots blues, ARegion region, Idx[][] ons
			, Idx[][] offs
	) {
		super(grid, hinter, AHint.MULTI, null, 0, reds, greens, null, blues, null, null);
		this.v = v;
		this.subtype = subtype;
		this.cause = cause;
		this.goodColor = goodColor;
		this.steps = steps;
		this.setPots = setPots;
		this.region = region;
		this.ons = ons;
		this.offs = offs;
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

	@Override
	public Idx[][] getSupers() {
		return ons;
	}

	@Override
	public Idx[][] getSubs() {
		return offs;
	}

	// for GEMHintBig (et al) so that results (bigger maybes) are painted in
	// the same color they where colored-in.
	@Override
	public int getSetPotsColor() {
		return goodColor;
	}

	/**
	 * Return Tech.GEM.difficulty plus two bonuses: <ul>
	 *  <li>The number of set-cells * Tech.NakedSingle.difficulty <br>
	 *  because a GEMHintBig can set many cells it is total difficulty includes
	 *  the total difficulty of setting all of those cells individually,
	 *  otherwise switching-off GEM makes SE report that a puzzle is harder
	 *  than it was with GEM, which it, pretty obviously, is not.
	 *  <li>The number of eliminations * 0.01 for consistency with GEMHint.
	 * </ul>
	 * @return Tech.GEM.difficulty plus two bonuses (as described).
	 */
	@Override
	public int getDifficultyTotal() {
		return getDifficulty()
			 + setPots.size() * Tech.NakedSingle.difficulty // cell-set bonus
			 + reds.totalSize(); // elimination bonus
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
		int numElims = 0; // my result
		// set the setPots (AFAIK, setPots is always non-null and not-empty in
		// a BigHint, but safety first demands I not crash: mischief managed.)
		if ( setPots!=null && !setPots.isEmpty() )
			numElims += 10 * setPots.setCells(isAutosolving, grid);
		// and then eliminate the redPots
		// nb: applyImpl enforces "reds never empty" (null=ok, empty=error) so
		// do not send him empty redPots for BIG BigHints that ONLY set cells
		if ( reds!=null && !reds.isEmpty() )
			 numElims += super.applyImpl(isAutosolving, grid);
		return numElims;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hinter.getTechName()+" Type " + subtype;
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(greens.ids()).append(CSP).append(blues.ids()).append(ON).append(v);
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

	private String htmlVerboseHintTypeName() {
		return getHintTypeName().replaceFirst("GEM", "sudopedia.org GEM (Graded Equivalence Marks)");
	}

	@Override
	protected String toHtmlImpl() {
		final StringBuilder sb = SB(20*1024);
		sb.append("<html><body>").append(NL)
		  .append("<h2>").append(htmlVerboseHintTypeName()).append("</h2>").append(NL);
		sb.append("Either the <g>green values</g> or the <b1>blue values</b1> are true.").append(NL);
		if ( steps != null ) {
			sb.append("<p>").append(NL)
			  .append("<u>Painting Steps</u>").append(NL)
		      .append("<pre>").append(NL)
		      .append(steps.trim()).append(NL)
		      .append("</pre>").append(NL);
		}
		final String results = setPots.toString().replaceAll(MINUS, PLUS);
		final String gc = goodColor==Constants.BLUE ? "b1" : "g"; // b1=BLUE, g=GREEN
		sb.append("<p>").append(NL).append("Therefore <").append(gc)
		  .append(">we can set <b>").append(results).append("</b></").append(gc)
		  .append(">").append(NL);
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
		return o instanceof GEMHintBig
			  && this.hashCode() == ((GEMHintBig)o).hashCode();
	}
	private int hc;

}
