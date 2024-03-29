/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.als;

import diuf.sudoku.Grid;
import diuf.sudoku.Link;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IntQueue;
import java.util.Collection;
import java.util.LinkedList;

/**
 * RccHint displays each RCC (Restricted Common Candidate) as if it where a
 * hint, just too see that they all look OK.
 *
 * @author Keith Corlett 2021-10-05
 */
class RccHint extends AHint {

	private final Rcc rcc;
	private final Als[] alss;
	public RccHint(Grid grid, IHinter hinter, Rcc rcc, Als[] alss) {
		super(grid, hinter, AHint.WARNING, null, 0, null, null, null, null, null, null);
		this.rcc = rcc;
		this.alss = alss;
	}

	@Override
	public Als[] getAlss() {
		return alss;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		int da[], s; // array of destination indices, source indice
		final Collection<Link> result = new LinkedList<>();
		for ( int v : VALUESES[rcc.cands] ) {
			da = alss[1].vs[v].toArrayNew();
			for ( final IntQueue q=alss[0].vs[v].indices(); (s=q.poll())>QEMPTY; )
				for ( int d : da )
					result.add(new Link(s, v, d, v));
		}
		return result;
	}

	@Override
	protected String toHtmlImpl() {
		String v2 = rcc.v2!=0 ? "/"+Integer.toString(rcc.v2) : "";
		return Html.colorIn("<html><body><pre>"+NL
			+"<b1>"+alss[0]+"</b1>    "+rcc.v1+v2+NL
			+"<b2>"+alss[1]+"</b2>"+NL
			+"</pre></body></html>");
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append("Rcc: ").append(rcc);
	}

}
