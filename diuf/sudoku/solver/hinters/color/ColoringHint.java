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
import static diuf.sudoku.Idx.FIFTY_FOUR;
import static diuf.sudoku.Idx.MASKED;
import static diuf.sudoku.Idx.MASKED81;
import diuf.sudoku.IdxI;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ColoringHint is the DTO for coloring hints.
 *
 * @author Keith Corlett 2020 Sept 22
 */
public class ColoringHint extends AHint  {

	// remove the redPots from all other pots, so they show-up red
	private static Pots[] clean(Pots[] pots, Pots reds) {
		for ( Pots p : pots )
			p.removeAll(reds);
		return pots;
	}

	/** The four subtypes of coloring hints. */
	public enum Subtype {
		  SimpleColorWrap("Simple Color Wrap")
		, SimpleColorTrap("Simple Color Trap")
		, MultiColor1("Multi Color 1")
		, MultiColor2("Multi Color 2")
		;
		public final String name; // the hint-type name for this subtype
		private Subtype(String name) {
			this.name = name;
		}
	}

	private final Subtype subtype; // hint type description
	private final Pots[] potss; // an array of Pots to be highlighted
	private final int valueToRemove; // the value to be eliminated

	// cells to highlight with an aqua background, that is indices of all cells
	// in all Coloring sets (the pots array).
	private Idx aquaIndices;
	private int[] aquaIndicesArray;
	private int hc; // hashCode is cached

	/**
	 * Constructor.
	 *
	 * @param grid to search
	 * @param hinter that created this hint
	 * @param subtype which type of ColoringHint is this
	 * @param pots an array of two Pots, one green and one blue; or possibly
	 *  more, apparently
	 * @param valueToRemove the plain value to remove
	 * @param reds eliminations
	 */
	public ColoringHint(final Grid grid, final IHinter hinter
			, final Subtype subtype, final Pots[] pots
			, final int valueToRemove, final Pots reds) {
		super(grid, hinter, reds, pots[0], null, pots[1], null, null);
		this.subtype = subtype;
		this.potss = clean(pots, this.reds); // pots sans this.reds (the field)
		this.valueToRemove = valueToRemove;
	}

	// fetch an Idx of cells to highlight with an aqua background, that is
	// indices of all cells in all of the Coloring sets (the pots array).
	private Idx getAquaIndices() {
		if ( aquaIndices == null ) {
			long m0=0L; int m1=0; // an exploded Idx
			for ( Pots pots : potss ) { // currently there are 5 colors max
				for ( int indice : pots.keySet() ) {
					if ( indice < FIFTY_FOUR )
						m0 |= MASKED81[indice];
					else
						m1 |= MASKED[indice];
				}
			}
			aquaIndices = new IdxI(m0, m1);
			aquaIndicesArray = aquaIndices.toArrayNew();
		}
		return aquaIndices;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int viewNumberNotUsed) {
		return getAquaIndices();
	}

	/*
	 * NOTE: I added three new candidate highlight colors: Yellow, Purple, and
	 * Brown to the SudokuGridPanel specifically to support the display of
	 * Coloring hints, but they may also be useful elsewhere, like ALSs.
	 */

	@Override
	public Pots getYellowPots() {
		if ( potss.length > 2 )
			return potss[2];
		return null;
	}

	@Override
	public Pots getPurplePots() {
		if ( potss.length > 3 )
			return potss[3];
		return null;
	}

	@Override
	public Pots getBrownPots() {
		if ( potss.length > 4 )
			return potss[4];
		return null;
	}

	@Override
	public Collection<Link> getLinks(final int viewNum) {
		int d, values[];
		boolean[] sees;
		final Collection<Link> result = new LinkedHashSet<>();
		getAquaIndices(); // initialise aquaIndicesArray
		for ( Map.Entry<Integer, Integer> e : reds.entrySet() ) {
			d = e.getKey(); // destination
			sees = SEES[d];
			values = VALUESES[e.getValue()];
			for ( int s : aquaIndicesArray ) // source
				if ( sees[s] )
					for ( int v : values )
						result.add(new Link(s, v, d, v));
		}
		return result;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof ColoringHint
			&& equals((ColoringHint)o);
	}

	public boolean equals(final ColoringHint other) {
		return valueToRemove == other.valueToRemove
			&& getAquaIndices().equals(other.getAquaIndices());
	}

	@Override
	public int hashCode() {
		if ( hc == 0 ) {
			getAquaIndices(); // initialise aquaIndicesArray
			int x = 0;
			for ( int indice : aquaIndicesArray )
				x = x<<4 ^ indice;
			hc = x<<4 ^ valueToRemove;
		}
		return hc;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return subtype.name;
	}

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on <b>"+valueToRemove+"</b>";
		return s;
	}

	@Override
	protected StringBuilder toStringImpl() {
		final StringBuilder sb = SB(128);
		sb.append(getHintTypeName()).append(COLON_SP);
		boolean first = true;
		for ( Pots p : potss ) {
			if ( p!=null && p.any() ) {
				if(first) first=false; else sb.append(CSP);
				sb.append(p.ids());
			}
		}
		return sb.append(ON).append(valueToRemove);
	}

	@Override
	protected String toHtmlImpl() {
		return Html.produce(this, "ColoringHint.html"
			, getHintTypeName()					//{0}
			, Integer.toString(valueToRemove)	// 1
			, reds.toString()					// 2
			, toString()						// 3
		);
	}

}
