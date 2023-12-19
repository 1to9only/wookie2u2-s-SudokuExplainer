/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.SEES;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.solver.hinters.Validator.prevMessage;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.CSP;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * XColoringHint is the Extended Coloring hint DTO.
 *
 * @author Keith Corlett 2021-03-03
 */
public class XColoringHint extends AHint {

	private final int v;
	private final String greenIds; // ids of the green cells
	private final String blueIds;
	private final String steps;
	private Collection<Link> links; // optional, a keeper
	public XColoringHint(final Grid grid, final IHinter hinter, final int v
			, final Pots reds, final Pots greens, final Pots blues
			, final Idx[] colorSet, final String steps
			, final Collection<Link> links
	) {
		super(grid, hinter, reds, greens, null, blues, null, null);
		this.v = v;
		if ( colorSet != null ) {
			// multi-coloring
			this.greenIds = colorSet[0].ids();
			this.blueIds = colorSet[1].ids();
		} else {
			// simple-coloring
			this.greenIds = greens.ids();
			this.blueIds = blues.ids();
		}
		this.steps = steps;
		this.links = links;
	}

	@Override
	public Set<Integer> getAquaBgIndices(final int viewNum) {
		return null;
	}

	@Override
	public Set<Integer> getBlueBgIndices(final int viewNum) {
		return blues.keySet();
	}

	@Override
	public Set<Integer> getGreenBgIndices(final int viewNum) {
		return greens.keySet();
	}

	@Override
	public Collection<Link> getLinks(final int viewNum) {
		// links is not null if it was set by Medusa3D, or if XColoring has
		// already called getLinks; to save us from generating them twice.
		if ( links != null )
			return links;
		// find the XColoring links (never Medusa3D)
		// nb: functional operations drops links. I do NOT understand.
		// I just avoid the problem by not using functional operations.
		// Insert new programmer and press any key to continue...
		final Collection<Link> result = new LinkedHashSet<>();
		final Pots[] potss = new Pots[] {greens, blues};
		reds.entrySet().forEach((e) -> {
			final int d = e.getKey(); // destination
			final boolean[] sees = SEES[d];
			final int cands = e.getValue();
			for ( Pots pots : potss )
				for ( int v : VALUESES[cands] )
					for ( int s : pots.keySet() ) // source
						if ( sees[s] )
							result.add(new Link(s, v, d, v));
		});
		return links = result;
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(greenIds).append(CSP).append(blueIds).append(ON).append(v);
	}

	@Override
	protected String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for a "+getHintTypeName();
		if ( isBig )
			s += " on <b>"+v+"</b>";
		return s;
	}

	private String htmlHintTypeName() {
		// handles both XColoring and Medusa3D hint-types
		return "sudopedia.org "+getHintTypeName();
	}

	@Override
	protected String toHtmlImpl() {
		final StringBuilder sb = SB(512); // observed max 330 in top1465
		sb.append("<html><body>").append(NL);
		if ( isInvalid )
			sb.append("<h2>").append("<r>INVALID</r> ").append(htmlHintTypeName()).append("</h2>").append(NL)
			  .append("<k><b>").append(prevMessage).append("</b></k><p>").append(NL);
		else
			sb.append("<h2>").append(htmlHintTypeName()).append("</h2>").append(NL);
	    sb.append("There are two extended coloring sets on ").append(v).append(":<pre>").append(NL)
		  .append("<g>GREEN: ").append(greenIds).append("</g>").append(NL)
		  .append("<b1>BLUE : ").append(blueIds).append("</b1>").append(NL)
		  .append("</pre>").append(NL)
		  .append("Either the <g>green values</g> or the <b1>blue values</b1> are true.").append(NL);
		if ( steps != null ) {
			sb.append("<p>").append(NL)
			  .append("<u>Steps</u>").append(NL)
			  .append("<pre>").append(NL)
			  .append(steps.trim()).append(NL)
			  .append("</pre>").append(NL);
		}
		sb.append("<p>").append(NL)
		  .append("Therefore <r>we can remove <b>").append(reds.toString()).append("</b></r>.").append(NL);
		sb.append("</body></html>").append(NL);
		return Html.colorIn(sb.toString());
	}

}
