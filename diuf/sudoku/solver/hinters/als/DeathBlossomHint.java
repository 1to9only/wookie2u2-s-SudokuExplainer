/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.LinkedList;
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
	private final List<Als> alss;
	private final Als[] alssByValue;
	public DeathBlossomHint(AHinter hinter, Pots redPots, Cell stem
			, List<Als> alss, Als[] alssByValue) {
		// nb: what are normally greens are oranges here
		super(hinter, redPots);
		this.stem = stem;
		this.values = VALUESES[stem.maybes.bits];
		this.alss = alss;
		this.alssByValue = alssByValue;
	}

	@Override
	public Set<Cell> getPinkCells(int viewNumUnused) {
		return Grid.cellSet(stem);
	}

	@Override
	public Collection<Als> getAlss() {
		return alss;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		Collection<Link> result = new LinkedList<>();
		for ( int v : values ) {
			Als a = alssByValue[v];
			for ( Cell c : a.cells )
				if ( c.maybe(v) )
					result.add(new Link(stem, v, c, v));
		}
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " stem "+stem.id+" in "+Frmt.ssv(Als.regions(alss));
		return s;
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(getHintTypeName()).append(":")
		  .append(" stem ").append(stem.id)
		  .append(" in ").append(Frmt.ssv(Als.regions(alss)));
		return sb.toString();
	}

	// there's max 4 alss in a Death Blossom
	// I'm not say you can't have 5, but I am saying that I don't have 5.
	private static final String[] COLORS = {"b1", "b2", "b3", "b4"};
	private String alssString() {
		StringBuilder sb = new StringBuilder(64*stem.maybes.size);
		// get each als by it's value
		for ( int i=0; i<values.length; ++i ) {
			int v = values[i];
			if ( i > 0 )
				sb.append(NL).append("       "); // 7 spaces
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
			, stem.toFullString()	//{0}
			, redPots.toString()	// 1
			, alssString()			// 2
			, invalid				// 3
		);
	}

}
