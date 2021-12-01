/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.HashSet;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.CSP;
import java.util.Collection;
import java.util.LinkedHashSet;


/**
 * ColoringHint is the DTO for coloring hints.
 *
 * @author Keith Corlett 2020 Sept 22
 */
public class ColoringHint extends AHint  {

	// calculate a HashSet of the cells to highlight with an aqua background,
	// ie all the cells with a highlighted candidate.
	private static HashSet<Cell> calculateAquaCells(Pots[] pots) {
		int totalSize = 0;
		for ( Pots pot : pots ) // currently there's 5 colors
			totalSize += pot.size();
		HashSet<Cell> cells = new HashSet<>(totalSize, 1F);
		for ( Pots pot : pots ) // currently there's 5 colors
			cells.addAll(pot.keySet());
		return cells;
	}

	// remove the redPots from all other pots, so they show-up red
	private static Pots[] clean(Pots[] pots, Pots redPots) {
		for ( Pots p : pots )
			p.removeAll(redPots);
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
	private final Pots[] pots; // an array of Pots to be highlighted
	private final int valueToRemove; // the value to be eliminated

	// cells to highlight with an aqua background, ie ALL cells in pots.
	private final Set<Cell> aquaCells;

	/**
	 *
	 * @param hinter
	 * @param subtype
	 * @param pots must be at-least length 2
	 * @param valueToRemove
	 * @param redPots
	 */
	public ColoringHint(AHinter hinter, Subtype subtype, Pots[] pots
			, int valueToRemove, Pots redPots) {
		// nb: Coloring uses ONE instance of Pots, so when we create a hint we
		// clone them, and then clear the ONE instance. Simples!
		super(hinter, redPots.copyAndClear(), pots[0], null, pots[1], null, null);
		this.subtype = subtype;
		this.pots = clean(pots, this.reds); // must pass the field!
		this.valueToRemove = valueToRemove;
		this.aquaCells = calculateAquaCells(pots);
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return aquaCells;
	}

	/*
	 * NOTE: I added three new candidate highlight colors: Yellow, Purple, and
	 * Brown to the SudokuGridPanel specifically to support the display of
	 * Coloring hints, but they may also be useful elsewhere, like ALS's.
	 */

	@Override
	public Pots getYellows() {
		if ( pots.length > 2 )
			return pots[2];
		return null;
	}

	@Override
	public Pots getPurples() {
		if ( pots.length > 3 )
			return pots[3];
		return null;
	}

	@Override
	public Pots getBrowns() {
		if ( pots.length > 4 )
			return pots[4];
		return null;
	}

	@Override
	public Collection<Link> getLinks(int viewNum) {
		final Collection<Link> result = new LinkedHashSet<>();
		reds.entrySet().forEach((e) -> {
			final int d = e.getKey().i;
			aquaCells.stream().filter((src)->src.sees[d]).forEach((src) -> {
				for ( int v : VALUESES[e.getValue()] ) {
					result.add(new Link(src.i, v, d, v));
				}
			});
		});
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( !(o instanceof ColoringHint) )
			return false;
		ColoringHint other = (ColoringHint)o;
		if ( this.valueToRemove != other.valueToRemove )
			return false;
		if ( this.aquaCells.size() != other.aquaCells.size() )
			return false;
		return this.aquaCells.containsAll(other.aquaCells);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Cell c : aquaCells )
			result = result<<4 ^ c.hashCode;
		result = result<<4 ^ valueToRemove;
		return result;
	}

	@Override
	protected String getHintTypeNameImpl() {
		return subtype.name;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for a " + getHintTypeName();
		if ( isBig )
			s += " on <b>"+valueToRemove+"</b>";
		return s;
	}

	@Override
	protected String toStringImpl() {
		final StringBuilder sb = Frmt.getSB(128);
		sb.append(getHintTypeName()).append(COLON_SP);
		boolean first = true;
		for ( Pots p : pots ) {
			if(first) first=false; else sb.append(CSP);
			sb.append(p.cells());
		}
		return sb.append(ON).append(valueToRemove).toString();
	}

	@Override
	protected String toHtmlImpl() {
		return Html.produce(this, "ColoringHint.html"
			, getHintTypeName()					// {0}
			, Integer.toString(valueToRemove)	//  1
			, reds.toString()				//  2
			, toString()						//  3
		);
	}

}
