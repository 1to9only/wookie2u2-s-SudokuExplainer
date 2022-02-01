/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import java.util.Collection;
import java.util.LinkedList;

/**
 * TwoStringKiteHint is the DTO for TwoStringKite hints. It's presented via
 * toString (fast) in LST and GUI, and via toHtml (slow verbose) only in GUI.
 *
 * @author Bernhard Hobiger (HoDoKu) hacked into SE by Keith Corlett
 */
public class TwoStringKiteHint extends AHint  {

	private final int v; // the TwoStringKite value
	private final Cell[] rowPair;
	private final Cell[] colPair;
	public TwoStringKiteHint(final AHinter hinter, final int v
			, final ARegion[] bases, final ARegion[] covers
			, final Pots greens, final Pots blues, final Pots reds
			, final Cell[] rowPair, final Cell[] colPair) {
		super(hinter, reds, greens, null, blues, bases, covers);
		this.v = v;
		this.rowPair = rowPair;
		this.colPair = colPair;
	}

	@Override
	public Collection<Link> getLinks(int viewNumUnused) {
		final Collection<Link> links = new LinkedList<>();
		final Cell victim = reds.firstKey();
		links.add(new Link(rowPair[0], v, rowPair[1], v));
		links.add(new Link(rowPair[1], v, victim, v));
		links.add(new Link(colPair[0], v, colPair[1], v));
		links.add(new Link(colPair[1], v, victim, v));
		return links;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += ON+Integer.toString(v);
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(greens.keySet()))
		  .append(ON).append(Integer.toString(v))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		final Cell victim = reds.firstKey();
		return Html.produce(this, "TwoStringKiteHint.html"
			, Integer.toString(v)	//{0}
			, covers[0].id			// 1
			, covers[1].id			// 2
			, bases[0].id			// 3
			, victim.id				// 4
			, rowPair[0].id			// 5
			, rowPair[1].id			// 6
			, colPair[0].id			// 7
			, colPair[1].id			// 8
			, reds.toString()		// 9
		);
	}

}
