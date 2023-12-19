/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.Ass;
import static diuf.sudoku.Ass.OFF;
import static diuf.sudoku.Ass.ON;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.MAYBES_STR;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Pots.INDICE;
import static diuf.sudoku.Pots.VALUES;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.solver.hinters.table.ATableChainer.getEffects;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * CellReductionHint Immutable DTO.
 * <p>
 * The tables hints are a bit more complex than normal because they complete
 * the work of the hinter, which calculates that the hint exists, but unlike
 * other hinters does not pack-up everything about the hint in a "dumb" DTO,
 * instead table hints use methods to pad-it-out, using the key details, to
 * produce a hint that's meaningful to the user. This is faster in the batch
 * which need not ever go past the key-details.
 *
 * @author Keith Corlett 2023-10-31
 */
public class CellReductionHint extends AHint {

	private final int indice; // source cell, whose every maybe makes these same elimination/s
	private final int[] cellsValues; // VALUESES[grid.maybes[indice]]
	private final long vcM0; // victims cell.ids
	private final int vcM1; // number of victims
	private final int valueToEliminate; // the value eliminated from victims
	private final int viewCount; // grid.sizes[indice]

	// FYI, there are many cached attributes, coz I'm an immutable DTO.

	/**
	 * Constructor.
	 *
	 * @param grid we search
	 * @param hinter that found me
	 * @param indice of the source cell
	 * @param vcM0 victims mask0
	 * @param vcM1 victims mask1
	 * @param valueToEliminate the bloody value to bloody eliminate, dopey
	 * @param reds the cell-values to eliminate
	 */
	public CellReductionHint(Grid grid, IHinter hinter, int indice
			, long vcM0, int vcM1, int valueToEliminate, Pots reds) {
		super(grid, hinter
		, reds
		, new Pots(indice, grid.maybes[indice], DUMMY) // greens
		, null // oranges
		, null // blues
		, null // bases
		, null // covers
		);
		this.indice = indice;
		this.cellsValues = VALUESES[grid.maybes[indice]];
		this.vcM0 = vcM0;
		this.vcM1 = vcM1;
		this.valueToEliminate = valueToEliminate;
		this.viewCount = grid.sizes[indice];
	}

	/**
	 * Returns the number of views in this hint. In a CellReductionHint that
	 * is the number of maybes in the source cell.
	 *
	 * @return number of views in this hint
	 */
	@Override
	public int getViewCount() {
		return viewCount;
	}

	/**
	 * Get a String representing the victims, cells from which valueToEliminate
	 * may be eliminated.
	 *
	 * @return space separated cell-ids of the victim cells
	 */
	private String victims() {
		if ( victimsS == null )
			victimsS = Idx.toString(vcM0,vcM1);
		return victimsS;
	}
	private String victimsS;

	/**
	 * Get the name of this type of hint.
	 *
	 * @return the name of this type of hint
	 */
	@Override
	protected String getHintTypeNameImpl() {
		return "Cell Reduction";
	}

	/**
	 * Return a String summarising this hint.
	 *
	 * @return a String summarising this hint
	 */
	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(CELL_IDS[indice]).append("{").append(MAYBES_STR[grid.maybes[indice]])
		.append("} so ").append(victims()).append(MINUS).append(valueToEliminate);
	}

	/**
	 * I turn the reds into an array once, using an Iterator once, for speed.
	 * It's faster to iterate an array, or array of arrays, than an Iterator.
	 * Note well that I am biased against Iterator. Your opinion won't change
	 * mine, but neither should mine change yours.
	 * <p>
	 * All the reds are on the valueToEliminate. Only the indice varies.
	 *
	 * @return [indice][values]
	 */
	private int[][] redsArray() {
		if ( redsA == null )
			redsA = reds.toArray();
		return redsA;
	}
	private int[][] redsA;

	/**
	 * Cache the target Asses (those are the Asses at the other ends of the
	 * chains from the initial assumptions) by viewNum.
	 *
	 * @param viewNum
	 * @return an array of the targets for viewNum
	 */
	private Ass[] targets(final int viewNum) {
		if ( targetsA == null )
			targetsA = new Ass[viewCount][];
		if ( targetsA[viewNum] == null ) {
			final IAssSet effects = getEffects(grid, indice, cellsValues[viewNum]);
			final int[][] redsArray = redsArray();
			final Ass[] result = new Ass[redsArray.length];
			int count = 0;
			for ( int[] red : redsArray )
				// nb: I record ONLY the first value eliminated
				result[count++] = effects.getAss(red[INDICE], VFIRST[red[VALUES]]);
			targetsA[viewNum] = result;
		}
		return targetsA[viewNum];
	}
	private Ass[][] targetsA;

	/**
	 * Build a Pots of the ons/offs in the chain for the given viewNum,
	 * including the given "actuals".
	 *
	 * @param actuals
	 * @param viewNum
	 * @param on
	 * @return
	 */
	private Pots potsFor(final Pots actuals, final int viewNum, final boolean on) {
		final Pots result = new Pots(actuals);
		for ( Ass target : targets(viewNum) )
			for ( Ass a=target; a!=null; a=a.firstParent )
				if ( a.isOn == on )
					result.put(a.indice, VSHFT[a.value]);
		return result;
	}

	/**
	 * Get the cell-values to paint green. These are the Ons.
	 *
	 * @param viewNum index of the current maybe of the source cell
	 * @return a Pots
	 */
	@Override
	public Pots getGreenPots(final int viewNum) {
		if ( greenPots == null )
			greenPots = new Pots[viewCount];
		if ( greenPots[viewNum] == null )
			greenPots[viewNum] = potsFor(greens, viewNum, ON);
		return greenPots[viewNum];
	}
	private Pots[] greenPots;

	/**
	 * Get the cell-values to paint red. These are the Offs.
	 *
	 * @param viewNum index of the current maybe of the source cell
	 * @return a Pots
	 */
	@Override
	public Pots getRedPots(final int viewNum) {
		if ( redPots == null )
			redPots = new Pots[viewCount];
		if ( redPots[viewNum] == null )
			redPots[viewNum] = potsFor(reds, viewNum, OFF);
		return redPots[viewNum];
	}
	private Pots[] redPots;

	/**
	 * Only links from and to highlighted cell-values are painted.
	 * Color matters not.
	 *
	 * @param viewNum index of the current maybe of the source cell
	 * @return a Pots of highlighted cell-values
	 */
	private Pots highlighted(final int viewNum) {
		final Pots result = new Pots(getGreenPots(viewNum));
		result.addAll(getRedPots(viewNum));
		return result;
	}

	/**
	 * Build the links for viewNum, denoting which maybe of the source cell.
	 *
	 * @param viewNum index of the current maybe of the source cell
	 * @return a new {@code HashSet<Link>} of the links for viewNum
	 */
	private Collection<Link> linksFor(final int viewNum) {
		final Set<Link> result = new HashSet<>();
		// only links from and to highligted cell-values are painted
		final Pots wanted = highlighted(viewNum);
		for ( Ass target : targets(viewNum) )
			for ( Ass p,a=target; (p=a.firstParent)!=null; a=p )
				if ( wanted.has(p) && wanted.has(a) )
					result.add(new Link(p, a));
		return result;
	}

	/**
	 * Get the links (brown arrows) for viewNum, which denotes the current
	 * value of the source cell.
	 *
	 * @param viewNum index of the current maybe of the source cell
	 * @return A {@code HashSet<Link>} of the links for viewNum
	 */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Collection<Link> getLinks(final int viewNum) {
		if ( links == null )
			links = new Collection[viewCount];
		if ( links[viewNum] == null )
			links[viewNum] = linksFor(viewNum);
		return links[viewNum];
	}
	Collection<Link>[] links;

	/**
	 * Returns a String containing each of the forcing chains which together
	 * justify this hint; one line per chain.
	 *
	 * @return a String representing the forcing chains that justify this hint
	 *  to the pedants.
	 */
	private String chainsHtml() {
		final StringBuilder html = SB(128);
		for ( int[] r : redsArray() ) {
			if ( reds.size() > 1 )
				html.append(CELL_IDS[r[INDICE]]).append(MINUS)
				.append(VFIRST[r[VALUES]]).append(COLON).append(NL);
			for ( int v : cellsValues )
				html.append(Ass.toStringChain(getEffects(grid, indice, v)
				.getAss(r[INDICE], VFIRST[r[VALUES]]), " &lt;- "))
				.append(NL);
		}
		return html.toString();
	}

	/**
	 * Get HTML identifying and explaining/justifying this hint.
	 *
	 * @return HTML to display in the hintDetailArea (JEditorPane) in the GUI.
	 */
	@Override
	protected String toHtmlImpl() {
		return Html.colorIn("<html><body>" + NL
		+ "<h2>"+getHintTypeName()+"</h2>" + NL
		+ "Every potential value of cell <b><c>"+CELL_IDS[indice]+"</c></b>"
			+ " eliminates the value "+valueToEliminate+" from "+victims()
			+ ", and each cell has one value, hence one of those values must"
			+ " be true." + NL
		+ "<p>" + NL
		+ "Therefore we can eliminate <b><r>"+reds+"</r></b>." + NL
		+ "<p>" + NL
		+ "The details of the chains are<pre>" + NL
		+ chainsHtml()
		+ "</pre>" + NL
		+ "</body></html>" + NL);
	}

}
