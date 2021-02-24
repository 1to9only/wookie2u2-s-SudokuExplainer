/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * DeathBlossomHint holds all the data of a DeathBlossom hint.
 *
 * @author Keith Corlett 2021 Jan
 */
public class DeathBlossomHint extends AHint implements IActualHint {

	private final Cell stem;
	private final int[] values;
	private final Als[] alssByValue;
	private final List<ARegion> regions;
	private final Pots yellows;
	public DeathBlossomHint(AHinter hinter, Pots redPots, List<Pots> alsPots
			, Cell stem, Als[] alssByValue, List<ARegion> regions, Grid grid) {
		// nb: what are normally greens are oranges here
		super(hinter, redPots
				, alsPots.get(0)
				, null
				, alsPots.get(1)
				, regions, null);
		this.stem = stem;
		this.values = VALUESES[stem.maybes.bits];
		this.alssByValue = alssByValue.clone();
		this.regions = regions;
		// the Pots in the 3rd stem.maybe/ALS, if exists (rare)
		if ( stem.maybes.size > 2 )
			this.yellows = alsPots.get(2);
		else
			this.yellows = null;
	}

	@Override
	public Set<Cell> getAquaCells(int viewNumUnused) {
		return null;
	}

	@Override
	public Set<Cell> getPinkCells(int viewNumUnused) {
		return Grid.cellSet(stem);
	}

	@Override
	public Set<Cell> getGreenCells(int viewNumUnused) {
		return Grid.cellSet(alssByValue[values[0]].cells);
	}

	@Override
	public Set<Cell> getBlueCells(int viewNumUnused) {
		return Grid.cellSet(alssByValue[values[1]].cells);
	}

	@Override
	public Set<Cell> getYellowCells(int viewNumUnused) {
		if ( stem.maybes.size > 2 )
			return Grid.cellSet(alssByValue[values[2]].cells);
		return null;
	}

	@Override
	public Pots getYellows() {
		return yellows; // 3rd alsPots, if exists
	}

	// orange and brown are too close, so you can't see the link-arrows, so I
	// have demoted orange to the fourth ALS, which I never use. top1465 has 0
	// Death Blossoms on stem-cells with 4+ maybes. I'm not saying they can't
	// exist, only that I don't have any of them.
	@Override
	public Set<Cell> getOrangeCells(int viewNumUnused) {
		if ( stem.maybes.size > 3 )
			return Grid.cellSet(alssByValue[values[3]].cells);
		return null;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		Collection<Link> links = new ArrayList<>(12);
		for ( int v : values )
			for ( Cell c : alssByValue[v].cells )
				if ( c.maybe(v) )
					links.add(new Link(c, v, stem, v));
		return links;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " stem "+stem.id+" in "+Frmt.ssv(regions);
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = new StringBuilder(64);
		if ( isInvalid )
			sb.append('@');
		sb.append(getHintTypeName()).append(":")
//		  .append(' ').append(stem.maybes.size)
		  .append(" stem ").append(stem.id)
		  .append(" in ").append(Frmt.ssv(regions));
		return sb.toString();
	}

	// there can only be 4 values/alss in a Death Blossom
	// green, orange, blue, yellow
	private static final String[] COLORS = {"g", "o", "b1", "y"};
	private String alssToString() {
		StringBuilder sb = new StringBuilder(64*stem.maybes.size);
		// get each als by it's value
		for ( int i=0; i<values.length; ++i  ) {
			int v = values[i];
			if ( i > 0 )
				sb.append(NL);
			sb.append('<').append(COLORS[i]).append('>');
			sb.append(v).append(" in ").append(alssByValue[v]);
			sb.append("</").append(COLORS[i]).append('>');
		}
		return Html.colorIn(sb.toString());
	}

	@Override
	public String toHtmlImpl() {
		final String invalid; if(isInvalid) invalid="INVALID "; else invalid="";
		return Html.produce(this, "DeathBlossomHint.html"
			, stem.toFullString()			//{0}
			, redPots.toString()			// 1
			, alssToString()				// 2
			, invalid						// 3
		);
	}

}
