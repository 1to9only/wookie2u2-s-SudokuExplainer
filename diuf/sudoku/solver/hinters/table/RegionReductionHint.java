/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.CELL_IDXS;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Pots.INDICE;
import static diuf.sudoku.Pots.VALUES;
import diuf.sudoku.Regions;
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
 * RegionReductionHint DTO.
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
public class RegionReductionHint extends AHint {

	// @return a Pots of every place in region for valueSought
	private static Pots greens(final ARegion region, final int valueSought) {
		final Pots result = new Pots();
		for ( int place : INDEXES[region.places[valueSought]] )
			result.put(region.indices[place], VSHFT[valueSought]);
		return result;
	}

	private final ARegion region;
	private final int valueSought;
	private final long vcM0;
	private final int vcM1;
	private final int valueToEliminate;
	private final int viewCount;

	public RegionReductionHint(
		  final Grid grid, final IHinter hinter
		, final ARegion region, final int valueSought
		, final long vcM0, final int vcM1
		, final int valueToEliminate
		, final Pots reds
	) {
		super(grid, hinter
			, reds
			, greens(region, valueSought) // greens
			, null // oranges
			, null // blues
			, Regions.array(region) // bases
			, null // covers
		);
		this.region = region;
		this.valueSought = valueSought;
		this.vcM0 = vcM0;
		this.vcM1 = vcM1;
		this.valueToEliminate = valueToEliminate;
		this.viewCount = greens.size();
		// do this ONCE, before the grid gets modified
		assert this.viewCount == region.numPlaces[valueSought];
	}

	@Override
	protected String getHintTypeNameImpl() {
		return "Region Reduction";
	}

	/**
	 * one view for each place for value in this region
	 *
	 * @return
	 */
	@Override
	public int getViewCount() {
		return viewCount;
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(valueSought).append(IN).append(region.label)
		.append(SO).append(Idx.toString(vcM0,vcM1))
		.append(MINUS).append(valueToEliminate);
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNum) {
		if ( aquaBgs == null ) {
			aquaBgs = new Idx[viewCount];
			int count = 0;
			for ( int[] g : greensArray() )
				aquaBgs[count++] = CELL_IDXS[g[INDICE]];
		}
		return viewNum>-1 && viewNum<viewCount ? aquaBgs[viewNum] : null;
	}
	private Idx[] aquaBgs;

	private int[][] greensArray() {
		if ( greensA == null )
			greensA = greens.toArray();
		return greensA;
	}
	private int[][] greensA;

	private int[][] redsArray() {
		if ( redsA == null )
			redsA = reds.toArray();
		return redsA;
	}
	private int[][] redsA;

	/**
	 * Builds potsCache[viewNum][0=greens, 1=reds] for all viewNum.
	 *
	 * @return the new potsCache
	 */
	private Pots[][] potsCache() {
		IAssSet effects;
		Ass a;
		final Pots[][] result = new Pots[viewCount][];
		final int[] victims = Idx.toArrayNew(vcM0,vcM1);
		final int[] indices = region.indices;
		final int[] places = INDEXES[region.places[valueSought]];
		for ( int i=0; i<viewCount; ++i ) {
			result[i] = new Pots[]{new Pots(), new Pots()}; // greens, reds
			effects = getEffects(grid, indices[places[i]], valueSought);
			for ( int indice : victims )
				for ( a=effects.getAss(indice, valueToEliminate); a!=null; a=a.firstParent )
					result[i][a.isOn ? 0 : 1].upsert(a.indice, VSHFT[a.value], DUMMY);
		}
		return result;
	}
	private Pots[][] potsCache;

	/**
	 * Get the cell-values to highlight in green for this viewNum.
	 * <p>
	 * In a RegionReductionHint the viewNum is the index of a place in this
	 * region which maybe valueSought: 0..region.size-1 inclusive, ergo one
	 * view for each possible place for this value in this region. Note that
	 * getRedPots, getGreenPots and getLinks all use the same viewNum; thus
	 * together they render a consistent picture of each forcing chain.
	 *
	 * @param viewNum
	 * @return
	 */
	@Override
	public Pots getGreenPots(final int viewNum) {
		if ( potsCache == null )
			potsCache = potsCache();
		return potsCache[viewNum][0]; // 0's is greens
	}

	/**
	 * Get the cell-values to highlight in red for this viewNum.
	 * <p>
	 * In a RegionReductionHint the viewNum is the index of a place in this
	 * region which maybe valueSought: 0..region.size-1 inclusive, ergo one
	 * view for each possible place for this value in this region. Note that
	 * getRedPots, getGreenPots and getLinks all use the same viewNum; thus
	 * together they render a consistent picture of each forcing chain.
	 *
	 * @param viewNum
	 * @return
	 */
	@Override
	public Pots getRedPots(final int viewNum) {
		if ( potsCache == null )
			potsCache = potsCache();
		return potsCache[viewNum][1]; // 1's is reds
	}

	// cache target asses in array coz they don't change per viewNum.
	// "target" means the consequent Ass (the other end of the chain from the
	// initial assumption), whose parents link-back to the initial assumption.
	private Ass[] targets() {
		if ( targetsA == null ) {
			IAssSet effects;
			final int[][] greensArray = greensArray();
			final int[][] redsArray = redsArray();
			targetsA = new Ass[greensArray.length * redsArray.length];
			assert targetsA.length > 0;
			int count = 0;
			for ( int[] green : greensArray ) {
				effects = getEffects(grid, green[INDICE], VFIRST[green[VALUES]]);
				for ( int[] red : redsArray ) {
					for ( int rv : VALUESES[red[VALUES]] ) {
						targetsA[count] = effects.getAss(red[INDICE], rv);
						if ( ++count == targetsA.length )
							return targetsA; // AIOOBE averted
					}
				}
			}
		}
		return targetsA;
	}
	private Ass[] targetsA;

	// links from and to highligted cell-values are painted. Color matters not.
	private Pots highlighted(final int viewNum) {
		final Pots result = new Pots(getGreenPots(viewNum));
		result.upsertAll(getRedPots(viewNum));
		return result;
	}

	/**
	 * Get the links between cell-values to render this viewNum.
	 * <p>
	 * In a RegionReductionHint the viewNum is the index of a place in this
	 * region which maybe valueSought: 0..region.size-1 inclusive, ergo one
	 * view for each possible place for this value in this region. Note that
	 * getRedPots, getGreenPots and getLinks all use the same viewNum; thus
	 * together they render a consistent picture of each forcing chain.
	 * <p>
	 * Every On causes offs, typically multiple. We show only relevant ones.
	 * About 30% of Ons cause an Off. Again, we show only relevant ones.
	 * Relevance is determined by getGreenPots and getRedPots. All I do is look
	 * if both cell-values are highlighted (color matters not) and if so then I
	 * render the link between those two cell values.
	 * <p>
	 * Note also that I produce a Set, of DISTINCT links, otherwise the grid
	 * disappears under a plethora of extraneous links.
	 *
	 * @param viewNum
	 * @return
	 */
	@Override
	public Collection<Link> getLinks(final int viewNum) {
		final Set<Link> result = new HashSet<>();
		// only links from and to highligted cell-values are painted
		final Pots wanted = highlighted(viewNum);
		for ( Ass target : targets() )
			if ( target != null )
				for ( Ass p,a=target; (p=a.firstParent)!=null; a=p )
					if ( wanted.has(p) && wanted.has(a) )
						result.add(new Link(p, a));
		return result;
	}

	private String chainsHtml() {
		final StringBuilder html = SB(128);
		for ( int[] r : redsArray() ) {
			html.append(CELL_IDS[r[INDICE]]).append(MINUS)
			.append(VFIRST[r[VALUES]]).append(COLON).append(NL);
			for ( int[] g : greensArray() ) {
				html.append(Ass.toStringChain(getEffects(grid, g[INDICE]
				, VFIRST[g[VALUES]]).getAss(r[INDICE], VFIRST[r[VALUES]])
				, " &lt;- ")).append(NL);
			}
		}
		return html.toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.colorIn("<html><body>" + NL
		+ "<h2>"+getHintTypeName()+"</h2>" + NL
		+ "Every possible place for the value <b><g>"+valueSought+"</g></b>"
			+ " in <b><b1>"+region.label+"</b1></b>"
			+ " eliminates the value <r>"+valueToEliminate+"</r>"
			+ " from "+Idx.toString(vcM0,vcM1)
			+ ", and each region contains one instance of each value"
			+ ", hence one of those places must be true." + NL
		+ "<p>" + NL
		+ "Therefore we can eliminate <b><r>"+reds+"</r></b>." + NL
		+ "<p>" + NL
		+ "The details of the chains are<pre>" + NL
		+ chainsHtml()
		+ "</pre>" + NL
		+ "</body></html>" + NL);
	}

}
