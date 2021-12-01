/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.COLON;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

/**
 * DeathBlossomHint holds all the data of a DeathBlossom hint, which is
 * summarised by toStringImpl, and explained by toHtmlImpl.
 *
 * @author Keith Corlett 2021 Jan
 */
public class DeathBlossomHint extends AHint  {

	private final Cell stem;
	private final Als[] alss;
	private final Als[] alssByValue;
	public DeathBlossomHint(AHinter hinter, Pots redPots, Cell stem
			, Als[] alss, Als[] alssByValue) {
		super(hinter, redPots);
		this.stem = stem;
		this.alss = alss;
		this.alssByValue = alssByValue;
	}

	@Override
	public Set<Cell> getPinkCells(int viewNumUnused) {
		return Cells.set(stem);
	}

	@Override
	public Als[] getAlss() {
		return alss;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		final Collection<Link> result = new LinkedList<>();
		// stem -maybes-> alss
		final int s = stem.i; // the source cell indice
		for ( int v : VALUESES[stem.maybes] ) {
			final Idx dst = alssByValue[v].vs[v];
			if ( dst != null ) {
				dst.forEach((d)->result.add(new Link(s, v, d, v)));
			}
		}
		// alss -zs-> elims
		// backwards: foreach dst, foreach src to link ONLY to elim'd values
		reds.entrySet().forEach((e) -> {
			final int d = e.getKey().i;
			for ( int z : VALUESES[e.getValue()] ) {
				for ( Als src : alss ) {
					link(src.vs[z], d, z, result);
				}
			}
		});
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " stem "+stem.id+IN+Frmu.ssv(Als.regionsList(alss));
		return s;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB().append(getHintTypeName()).append(COLON)
		  .append(" stem ").append(stem.id)
		  .append(IN).append(Frmu.ssv(Als.regionsList(alss)))
		  .toString();
	}

	// produce a line per ALS in this DB, that's colored to match the grid.
	private String coloredAlss() {
		int v;
		final int n = stem.size;
		final StringBuilder sb = new StringBuilder(64*n);
		// get each als by it's value
		final int[] values = VALUESES[stem.maybes];
		for ( int i=0; i<n; ++i ) {
			v = values[i];
			if ( i > 0 )
				sb.append(NL).append("       "); // 7 spaces
			sb.append('<').append(COLORS[i % COLORS.length]).append('>')
			  .append(v).append(IN).append(alssByValue[v])
			  .append("</").append(COLORS[i % COLORS.length]).append('>');
		}
		return Html.colorIn(sb.toString());
	}
	// NOTE: There's max 4 alss in my Death Blossoms, but I suspect the max
	// possible might be 5, so the above COLORS code has overflow prevention.
	// If the max has grown then add an extra color/s. b5 exists already.
	private static final String[] COLORS = {"b1", "b2", "b3", "b4"};

	@Override
	public String toHtmlImpl() {
		final String s; if(isInvalid) s="INVALID "; else s=EMPTY_STRING;
		return Html.produce(this, "DeathBlossomHint.html"
			, stem.toFullString()	//{0}
			, coloredAlss()			// 1
			, stem.id				// 2
			, reds.toString()		// 3
			, s						// 4
		);
	}

}
