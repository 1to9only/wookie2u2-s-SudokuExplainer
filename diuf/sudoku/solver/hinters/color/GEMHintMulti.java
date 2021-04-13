/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;

/**
 * GEMHintMulti is GEM (Graded Equivalence Marks) Type 2+ (Big) hints.
 * <p>
 * Based on XColoringHintMulti to display SetPots, redPots, and the markers
 * (colored +'s and -'s).
 *
 * @author Keith Corlett 2021-03-24
 */
public class GEMHintMulti extends AHint {

	private final int v;
	private final int subtype;
	private final String greenCellIds;
	private final String blueCellIds;
	private final Pots setPots;
	private final Set<Cell> cause;
	private final int resultColor;
	private final String steps;
	private final ARegion region;
	private final Idx[][] supers;
	private final Idx[][] subs;

	public GEMHintMulti(AHinter hinter, int v, Pots redPots, int subtype
			, Set<Cell> cause, int resultColor, String steps, Pots setPots
			, Pots greens, Pots blues, ARegion region, Idx[][] ons
			, Idx[][] offs
	) {
		super(hinter, AHint.MULTI, null, 0
		   , redPots, greens, null, blues
		   , null, null);
		this.v = v;
		this.subtype = subtype;
		this.greenCellIds = greens.cells();
		this.blueCellIds = blues.cells();
		this.cause = cause;
		this.resultColor = resultColor;
		this.steps = steps;
		this.setPots = setPots;
		this.region = region;
		this.supers = ons;
		this.subs = offs;
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
	public List<ARegion> getPinkRegions() {
		return Regions.list(region);
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
	public Idx[][] getSupers() {
		return supers;
	}

	@Override
	public Idx[][] getSubs() {
		return subs;
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
	public int apply(boolean isAutosolving, boolean isNoisy) {
		int numElims = 0;
		for ( Cell c : setPots.keySet() ) {
			Values toSet = setPots.get(c);
			assert toSet.size == 1; // ONE value per cell in a setPots!
			numElims += c.set(toSet.first(), 0, isAutosolving, null) * 10;
		}
		return numElims;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return hinter.tech.nom+" Type " + subtype;
	}

	@Override
	protected String toStringImpl() {
		return getHintTypeName()+": "+greenCellIds+", "+blueCellIds
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

	private String htmlHintTypeName() {
		return getHintTypeName().replaceFirst("GEM", "GEM (Graded Equivalence Marks)");
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
		String results = setPots.toString().replaceAll("-", "+");
		sb.append("<p>").append(NL)
		  .append("Therefore <g>we can set <b>").append(results).append("</b></g>").append(NL);
		sb.append("<p>").append(NL)
		  .append("<pre>").append(NL)
		  .append(Html.load(this, "GEMHintExplanation.txt")).append(NL)
		  .append("</pre>").append(NL);
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}
	
	@Override
	public int hashCode() {
		if ( hc == 0 ) {
			if ( setPots != null )
				hc = setPots.hashCode();
			else if ( redPots != null )
				hc = redPots.hashCode();
			else
				hc = toString().hashCode();
		}
		return hc;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj
			|| ( obj != null
			  && obj instanceof GEMHintMulti
			  && this.hashCode() == ((GEMHintMulti) obj).hashCode() );
	}
	private int hc;

}
