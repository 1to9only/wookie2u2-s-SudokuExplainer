/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.table;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Pots.INDICE;
import static diuf.sudoku.Pots.VALUES;
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
import java.util.Map;
import java.util.Set;

/**
 * CellAbductionHint DTO.
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
public class CellAbductionHint extends AHint {

	// indice of the reduced cell
	private final int indice;
	// consequences common to every possible value of the reduced cell
	private final Set<Eff> consequences;
	// a view per potential value of the reduced cell
	private final int viewCount;

	// caches
	private int[][] greensA;
	private int[][] redsA;
	private Pots[] greenPots;
	private Pots[] redPots;
	private LinkedList<Ass> targetsL;

	public CellAbductionHint(final Grid grid, final IHinter hinter
			, final int indice, final Set<Eff> consequences, Pots reds) {
		super(
		  grid
		, hinter
		, reds
		, new Pots(indice, grid.maybes[indice], DUMMY) // greens
		, null // oranges
		, null // blue
		, null // bases
		, null // covers
		);
		this.indice = indice;
		this.consequences = consequences;
		this.viewCount = grid.sizes[indice];
	}

	/**
	 * I override apply to cater for ON consequence/s, if any.
	 *
	 * @param isAutosolving IGNORED
	 * @param grid to solve
	 * @return score (10 * number of cells set)
	 */
	@Override
	protected int applyImpl(final boolean isAutosolving, final Grid grid) {
		int count = 0;
		for ( Eff e : consequences ) {
			if ( e.isOn ) {
				count += grid.cells[e.indice].set(e.value);
			}
		}
		if ( count > 0 ) {
			return 10 * count;
		}
		return super.applyImpl(isAutosolving, grid);
	}

	@Override
	protected String getHintTypeNameImpl() {
		return "Cell Abduction";
	}

	@Override
	protected StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(CELL_IDS[indice]).append(SO).append(consequences);
	}

	@Override
	public int getViewCount() {
		return viewCount;
	}

	/**
	 * A "special" toArray coz each potential value needs it's own element,
	 * to show one potential value per view.
	 *
	 * @param pots the greens
	 * @return legs eleven gameet octopi.
	 */
	private int[][] greensToArray() {
		final LinkedList<int[]> list = new LinkedList<>();
		for ( Map.Entry<Integer,Integer> e : greens.entrySet() ) {
			for ( int v : VALUESES[e.getValue()] ) {
				list.add(new int[] {e.getKey(), v});
			}
		}
		return list.toArray(new int[list.size()][]);
	}

	private int[][] greensArray() {
		if ( greensA == null ) {
			greensA = greensToArray();
		}
		return greensA;
	}

	private int[][] redsArray() {
		if ( redsA == null ) {
			redsA = reds.toArray();
		}
		return redsA;
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
		// get the initialOn assumption for viewNum
		// nb: green "values" is already a plain value,
		// whereas reds "values" is a bitset of all values
		final int[] green = greensArray()[viewNum];
		// get the effects of the initialOn assumption
		final IAssSet effects = getEffects(grid, green[INDICE], green[VALUES]);
		// foreach elimination
		for ( int[] red : redsArray() ) {
			// foreach value eliminated
			for ( int v : VALUESES[red[VALUES]] ) {
				// get the "target" elimination Ass, which is an effect of the
				// current initialOn assumption.
				final Ass target = effects.getAss(red[INDICE], v);
				// foreach Ass in the forcing chain from this elimination
				// back-to the initial assumption for viewNum
				for ( Ass a=target; a!=null; a=a.firstParent ) {
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
			targetsL = new FunkyAssList();
			final int[][] redsArray = redsArray();
			for ( int[] green : greensArray() ) {
				// nb: greens[VALUES] is a plain value already
				final IAssSet effects = getEffects(grid, green[INDICE], green[VALUES]);
				for ( int[] red : redsArray ) {
					// nb: reds[VALUES] is a bitset of all values
					for ( int v : VALUESES[red[VALUES]] ) {
						targetsL.add(effects.getAss(red[INDICE], v));
					}
				}
			}
		}
		return targetsL;
	}

	@Override
	public Collection<Link> getLinks(final int viewNum) {
		final Set<Link> result = new HashSet<>();
		// only links from and to highligted cell-values are painted
		final Pots wanted = highlighted(viewNum);
		boolean any = false;
		for ( Ass a : targets() ) {
			for ( Ass p; (p=a.firstParent)!=null; a=p ) {
				if ( wanted.has(p) && wanted.has(a) ) {
					any |= result.add(new Link(p, a));
				}
			}
		}
		// fall-back: if none are wanted then just add all targets
		if ( !any ) {
			for ( Ass a : targets() ) {
				for ( Ass p; (p=a.firstParent)!=null; a=p ) {
					result.add(new Link(p, a));
				}
			}
		}
		return result;
	}

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
			// foreach potential value of the starting cell
			for ( int v : VALUESES[grid.maybes[indice]] )
				html.append(Ass.toStringChain(getEffects(grid, indice, v).getAss(red[INDICE], VFIRST[red[VALUES]]), " &lt;- ")).append(NL);
		}
		return html.toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.colorIn("<html><body>" + NL
		+ "<h2>"+getHintTypeName()+"</h2>" + NL
		+ "Every potential value of cell <b><c>"+CELL_IDS[indice]+"</c></b>"
			+ " causes the same conequences: "+consequences
			+ ", and each cell has one value"
			+ ", hence one of those values must be true." + NL
		+ "<p>" + NL
		+ "Therefore we can eliminate <b><r>"+reds+"</r></b>." + NL
		+ "<p>" + NL
		+ "The details of the chains are<pre>" + NL
		+ chainsHtml()
		+ "</pre>" + NL
		+ "</body></html>" + NL);
	}

	// Funky: add ignores null
	private static class FunkyAssList extends LinkedList<Ass> {
		private static final long serialVersionUID = 4509845894320120L;
		@Override
		public boolean add(final Ass e) {
			return e!=null && super.add(e);
		}
	}

}
