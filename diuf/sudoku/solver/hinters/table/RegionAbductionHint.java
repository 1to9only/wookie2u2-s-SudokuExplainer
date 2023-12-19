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
import diuf.sudoku.solver.hinters.table.ATableChainer.Eff;
import static diuf.sudoku.solver.hinters.table.ATableChainer.getEffects;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * RegionAbductionHint DTO.
 * <p>
 * The tables hints are a bit more complex than normal because they complete
 * the work of the hinter, which calculates that the hint exists, but unlike
 * other hinters does not pack-up everything about the hint in a "dumb" DTO,
 * instead table hints use methods to pad-it-out, using the key details, to
 * produce a hint that's meaningful to the user. This is faster in the batch
 * which need not ever go past the key-details.
 *
 * @author Keith Corlett 2023-11-04
 */
public class RegionAbductionHint extends AHint {

	private static Pots greenPots(final ARegion region, final int valueSought) {
		Pots result = new Pots();
		final int[] indices = region.indices;
		final int candSought = VSHFT[valueSought];
		for ( int place : INDEXES[region.places[valueSought]] )
			result.put(indices[place], candSought);
		return result;
	}

	private final ARegion region;
	private final int valueSought;
	private final Set<Eff> effects;
	private final int[] places;
	private final int viewCount;
	private final int[] indices;

	RegionAbductionHint(final Grid grid, final IHinter hinter
			, final ARegion region, final int valueSought
			, final Set<Eff> effects, final Pots reds
	) {
		super(
		  grid
		, hinter
		, reds // reds
		, greenPots(region, valueSought) // greens
		, null // oranges
		, null // blues
		, Regions.array(region)
		, null // covers
		);
		this.region = region;
		this.valueSought = valueSought;
		this.effects = effects;
		this.places = INDEXES[region.places[valueSought]];
		this.viewCount = region.numPlaces[valueSought];
		this.indices = new int[viewCount];
		for ( int i=0; i<viewCount; ++i )
			indices[i] = region.indices[places[i]];
	}

	@Override
	public int getViewCount() {
		return viewCount;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNum) {
		return Grid.CELL_IDXS[indices[viewNum]];
	}

	@Override
	protected String getHintTypeNameImpl() {
		return "Region Abduction";
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeNameImpl()).append(COLON_SP)
		.append(valueSought).append(IN).append(region.label)
		.append(SO).append(effects);
	}

	// links from and to highligted cell-values are painted. Color matters not.
	private Pots highlighted(final int viewNum) {
		final Pots result = new Pots(getGreenPots(viewNum));
		result.addAll(getRedPots(viewNum));
		return result;
	}

	/**
	 * Cache the target Asses (those are the Asses at the other ends of the
	 * chains from the initial assumptions).
	 * <p>
	 * NOTE WELL: That I record ONLY the first value eliminated from each cell.
	 * It cannot say that its not possible for a CellReductionHint to eliminate
	 * multiple values from a single cell, but I can tell you that it does not
	 * happen often. So, very rarely, if ever. This is necessary to make the
	 * targetCache line-up on viewNum. If broken Map viewNum to targets, and
	 * foreach VALUESES[r[VALUE]], using LinkedList as array factory.
	 *
	 * @return
	 */
	private LinkedList<Ass> targets() {
		if ( targetsL == null ) {
			targetsL = new LinkedList<>();
			final int[][] redsArray = redsArray();
			for ( int viewNum=0; viewNum<viewCount; ++viewNum ) {
				final IAssSet effects = effects(viewNum);
				for ( int[] red : redsArray ) {
					// nb: red[VALUES] is a bitset of all values
					for ( int v : VALUESES[red[VALUES]] ) {
						targetsL.add(effects.getAss(red[INDICE], v));
					}
				}
			}
		}
		return targetsL;
	}
	private LinkedList<Ass> targetsL;

	@Override
	public Collection<Link> getLinks(final int viewNum) {
		final Set<Link> result = new HashSet<>();
		// only links from and to highligted cell-values are painted
		final Pots wanted = highlighted(viewNum);
		boolean any = false;
		for ( Ass target : targets() ) {
			Ass a = target;
			if ( a != null ) {
				for ( Ass p; (p=a.firstParent)!=null; a=p ) {
					if ( wanted.has(p) && wanted.has(a) ) {
						any |= result.add(new Link(p, a));
					}
				}
			}
		}
		// fall-back: if none are wanted then just add all targets
		if ( !any ) {
			for ( Ass target : targets() ) {
				if ( target != null ) {
					for ( Ass p,a=target; (p=a.firstParent)!=null; a=p ) {
						result.add(new Link(p, a));
					}
				}
			}
		}
		return result;
	}

	/**
	 * I implement getGreenPots(viewNum) and getRedPots(viewNum) because there
	 * code was VERY similar. I build a new Pots, initially containing (EMPTY
	 * for greens, reds for reds); add each ON or OFF (depending on $isOn) in
	 * the forcing chain from each elimination back to the initial assumption
	 * for viewNum. Simple enough. Sigh.
	 *
	 * @param viewNum index of current value in maybes[indice]
	 * @param isOn true for greens, false for reds
	 * @return
	 */
	private Pots getPots(final int viewNum, final boolean isOn) {
		final Pots result = new Pots();
		// add the actual reds, in-case a "target" elim gets lost somehow.
		if ( !isOn ) {
			result.addAll(reds);
		}
		// get the effects of the initialOn assumption
		final IAssSet effects = effects(viewNum);
		// foreach elimination
		for ( int[] red : redsArray() ) {
			// foreach value eliminated
			for ( int v : VALUESES[red[VALUES]] ) {
				// get the "target" elimination Ass, which is an effect of the
				// current initialOn assumption.
				// foreach Ass in the forcing chain from this elimination
				// back-to the initial assumption for viewNum
				for ( Ass a=effects.getAss(red[INDICE], v); a!=null; a=a.firstParent ) {
					// if this effect is On/Off
					if ( a.isOn == isOn ) {
						// add the ____er
						result.put(a.indice, VSHFT[a.value]);
					}
				}
			}
		}
		return result;
	}

	@Override
	public Pots getGreenPots(int viewNum) {
		if ( greenPots == null ) {
			greenPots = new Pots[viewCount];
		}
		if ( greenPots[viewNum] == null ) {
			greenPots[viewNum] = getPots(viewNum, true);
		}
		return greenPots[viewNum];
	}
	private Pots[] greenPots;

	@Override
	public Pots getRedPots(int viewNum) {
		if ( redPots == null ) {
			redPots = new Pots[viewCount];
		}
		if ( redPots[viewNum] == null ) {
			redPots[viewNum] = getPots(viewNum, false);
		}
		return redPots[viewNum];
	}
	private Pots[] redPots;

	private int[][] redsArray() {
		if ( redsA == null ) {
			redsA = reds.toArray();
		}
		return redsA;
	}
	private int[][] redsA;

	/**
	 * The effects of each initial assumption are cached so that we calculate
	 * each once. In a RegionAbductionHint the value of the initial assumption
	 * is invariant, so I cache the effects of each place by viewNum.
	 *
	 * @param value
	 * @return
	 */
	private IAssSet effects(final int viewNum) {
		if ( effectsA == null ) {
			effectsA = new IAssSet[viewCount];
		}
		if ( effectsA[viewNum] == null ) {
			effectsA[viewNum] = getEffects(grid, indices[viewNum], valueSought);
		}
		return effectsA[viewNum];
	}
	private IAssSet[] effectsA;

	/**
	 * Returns a String containing each of the forcing chains which together
	 * justify this hint; one line per chain.
	 *
	 * @return a String representing the forcing chains that justify this hint
	 *  to the pedants.
	 */
	private String chainsHtml() {
		final StringBuilder html = SB(128);
		// foreach elimination
		for ( int[] red : redsArray() ) {
			// identify each target (elimination) if multiple
			if ( reds.size() > 1 ) {
				html.append(CELL_IDS[red[INDICE]]).append(MINUS)
				.append(VFIRST[red[VALUES]]).append(COLON).append(NL);
			}
			// foreach place in this region for the valueSought
			for ( int viewNum=0; viewNum<viewCount; ++viewNum )
				html.append(Ass.toStringChain(effects(viewNum).getAss(red[INDICE], VFIRST[red[VALUES]]), " &lt;- ")).append(NL);
		}
		return html.toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.colorIn("<html><body>" + NL
		+ "<h2>"+getHintTypeName()+"</h2>" + NL
		+ "Every place for the value <b><c>"+valueSought+"</c></b>"
			+ " in <b><b1>"+region.label+"</b1></b>"
			+ " causes the common consequences: "+effects
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
