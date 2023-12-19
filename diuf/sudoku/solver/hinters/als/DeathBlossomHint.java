/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.solver.hinters.als.AlsHintHelper.link;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.COLON;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IntQueue;
import diuf.sudoku.utils.MyLinkedHashSet;
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

	// NOTE: There is max 4 alss in my Death Blossoms, but I suspect the max
	// possible might be 5, so the above COLORS code has overflow prevention.
	// If the max has grown then add an extra color/s. b5 exists already.
	private static final String[] COLORS = {"b1", "b2", "b3", "b4"};

	private final Cell stem;
	private final Als[] alssByValue;
	public DeathBlossomHint(Grid grid, IHinter hinter, Pots redPots, Cell stem
			, Als[] alssByValue) {
		super(grid, hinter, redPots);
		this.stem = stem;
		this.alssByValue = alssByValue;
	}

	@Override
	public Set<Integer> getPinkBgIndices(int viewNumUnused) {
		return MyLinkedHashSet.of(stem.indice);
	}

	@Override
	public Als[] getAlss() {
		if ( alss == null ) {
			// pack alssByValue into a "normal" array
			final Als[] array = new Als[stem.size];
			int cnt = 0;
			for ( int v : VALUESES[stem.maybes] )
				array[cnt++] = alssByValue[v];
			assert cnt == array.length;
			alss = array;
		}
		return alss;
	}
	private Als[] alss;

	@Override
	public Collection<Link> getLinks(int viewNum) {
		Idx dst; // destinations Idx
		int d; // destination indice
		final Collection<Link> result = new LinkedList<>();
		// stem -maybes-> alss
		final int s = stem.indice; // the source cell indice
		if ( alssByValue != null )
			for ( int v : VALUESES[stem.maybes] )
				if ( alssByValue[v] != null
				  && (dst=alssByValue[v].vs[v]) != null ) // NEVER null
					for ( final IntQueue q=dst.indices(); (d=q.poll())>QEMPTY; )
						result.add(new Link(s, v, d, v));
		// alss -zs-> elims
		// backwards: foreach dst, foreach src to link ONLY to elimed values
		reds.entrySet().forEach((e) -> {
			final int dd = e.getKey();
			for ( int z : VALUESES[e.getValue()] )
				for ( Als src : getAlss() )
					link(src.vs[z], dd, z, result);
		});
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		final StringBuilder sb = SB(64).append("Look for a ").append(getHintTypeName());
		if ( isBig )
			sb.append(" stem ").append(stem.id).append(IN)
			.append(Als.regionLabels(getAlss(), CSP));
		return sb.toString();
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(128).append(getHintTypeName()).append(COLON)
		.append(" stem ").append(stem.id).append(IN)
		.append(Als.regionLabels(getAlss(), CSP));
	}

	// produce a line per ALS in this DB, that is colored to match the grid.
	private String coloredAlss() {
		int v;
		final int n = stem.size;
		final StringBuilder sb = SB(n<<6); // * 64
		// get each als by it is value
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

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "DeathBlossomHint.html"
			, stem.toFullString()	//{0}
			, coloredAlss()			// 1
			, stem.id				// 2
			, reds.toString()		// 3
			, ""					// 4 was isInvalid, now done generically
		);
	}

}
