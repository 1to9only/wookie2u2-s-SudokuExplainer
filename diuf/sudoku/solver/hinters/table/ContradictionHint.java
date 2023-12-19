/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.ASHFT;
import static diuf.sudoku.Ass.ON_BIT;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.CELL_IDXS;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.table.ATableChainer.Eff;
import static diuf.sudoku.solver.hinters.table.ATableChainer.getEffects;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import static diuf.sudoku.utils.Frmt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ContradictionHint DTO.
 * <p>
 * The tables hints are a bit more complex than normal because they complete
 * the work of the hinter, which calculates that the hint exists, but unlike
 * other hinters does not pack-up everything about the hint in a "dumb" DTO,
 * instead table hints use methods to pad-it-out, using the key details, to
 * produce a hint that's meaningful to the user. This is faster in the batch
 * which need not ever go past the key-details.
 *
 * @author Keith Corlett 2023-11-06
 */
public class ContradictionHint extends AHint {

	public final Eff initOn;
	public final int conIndice;
	public final int conValue;

	public ContradictionHint(final Grid grid, final IHinter hinter
			, final Eff initOn, final int indice, final int value) {
		super(grid, hinter, new Pots(initOn.indice, VSHFT[initOn.value], DUMMY)); // reds
		this.initOn = initOn;
		this.conIndice = indice;
		this.conValue = value;
	}

	@Override
	public Pots getOrangePots(final int viewNum) {
		if ( orangePots == null )
			orangePots = new Pots(conIndice, conValue);
		return orangePots;
	}
	private Pots orangePots;

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(initOn).append(CAUSES).append(CELL_IDS[conIndice])
		.append(PLUS_OR_MINUS).append(conValue);
	}

	/**
	 * Binary Contradiction inherently has 2 views: an On and an Off, which
	 * contradict each other, hence the initial assumption is ____ed.
	 *
	 * @return 2
	 */
	@Override
	public int getViewCount() {
		return 2;
	}

	@Override
	public Set<Integer> getGreenBgIndices(final int viewNum) {
		return CELL_IDXS[initOn.indice];
	}

	// ========================== getGreen/RedPots ==========================

	// populate $pot with $isOn Asses in $target's chain back-to initOn.
	private static Pots potsFor(final Ass target, final boolean isOn) {
		final Pots result = new Pots();
		for ( Ass a=target; a!=null; a=a.firstParent )
			if ( a.isOn == isOn )
				result.put(a.indice, VSHFT[a.value]);
		return result;
	}

	@Override
	public Pots getGreenPots(final int viewNum) {
		if ( greenPots == null )
			greenPots = new Pots[2];
		if ( greenPots[viewNum] == null )
			greenPots[viewNum] = potsFor(target(viewNum), true);
		return greenPots[viewNum];
	}
	private Pots[] greenPots;

	@Override
	public Pots getRedPots(final int viewNum) {
		if ( redPots == null )
			redPots = new Pots[2];
		if ( redPots[viewNum] == null )
			redPots[viewNum] = potsFor(target(viewNum), false);
		return redPots[viewNum];
	}
	private Pots[] redPots;

	// ========================== getLinks ==========================

	/**
	 * Build highlighted cell-values to paint. Color matters not.
	 *
	 * @param viewNum to find highlighted cell values for
	 * @return A Pots of all highlighted cell-values
	 */
	private Pots highlighted(final int viewNum) {
		final Pots result = new Pots(getGreenPots(viewNum));
		result.addAll(getRedPots(viewNum));
		// the initOn cell needs ALL of its maybes, so that the target acts as
		// the filter for this cell
		result.put(initOn.indice, grid.maybes[initOn.indice]);
		return result;
	}

	private Collection<Link> linksFor(final int viewNum) {
		// nb: I use a Set to enforce uniqueness, otherwise the Links tend to
		// over-paint so much of the grid that you can't see the grid, and the
		// whole thing looks like just like totes hodgepodgenated kludgiphile.
		final Collection<Link> result = new HashSet<>();
		// only links from and to highligted cell-values are painted
		final Pots wanted = highlighted(viewNum);
		Ass a = target(viewNum);
		if ( a != null )
			for ( Ass p; (p=a.firstParent)!=null; a=p )
				if ( wanted.has(p) && wanted.has(a) )
					result.add(new Link(p, a));
		return result;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"}) //Array of generic Collection
	public Collection<Link> getLinks(final int viewNum) {
		if ( links == null )
			links = new Collection[2];
		if ( links[viewNum] == null )
			links[viewNum] = linksFor(viewNum);
		return links[viewNum];
	}
	private Collection<Link>[] links;

	// ========================== toHtmlImpl ==========================

	private Ass target(final int viewNum) {
		if ( targets == null ) {
			final IAssSet effects = getEffects(grid, initOn.indice, initOn.value);
			targets = new Ass[] {
				// FYI getAss(hashCode) was reinstated especially for me
				  effects.getAss(ON_BIT ^ ASHFT[conValue] ^ conIndice)
				, effects.getAss(ASHFT[conValue] ^ conIndice)
			};
		}
		return targets[viewNum];
	}
	private Ass[] targets;

	private String chainsHtml() {
		final StringBuilder html = SB(128);
		html.append("ON:&nbsp; ").append(Ass.toStringChain(target(0), " &lt;- ")).append(NL);
		html.append("OFF: ").append(Ass.toStringChain(target(1), " &lt;- ")).append(NL);
		return html.toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.colorIn("<html><body>" + NL
		+ "<h2>"+getHintTypeName()+"</h2>" + NL
		+ "If we assume that <b><g>"+initOn+"</g></b>, it follows, through two"
			+ " contradictory XY Forcing chains that"
			+ " <b><o>"+CELL_IDS[conIndice]+PLUS_OR_MINUS+conValue+"</o></b>"
			+ " is both On and Off (ie both is value, and is not value), which"
			+ " is absurd, hence the initial assumption ("+initOn+") is proven"
			+ " false."
		+ "<p>" + NL
		+ "Therefore we can eliminate <b><r>"+reds+"</r></b>." + NL
		+ "<p>" + NL
		+ "The details of the chains are<pre>" + NL
		+ chainsHtml()
		+ "</pre>" + NL
		+ "</body></html>" + NL);
	}

}
