/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * AlignedExclusionHint's are created by AlignedPairExclusion,
 * AlignedTripleExclusion, and AlignedSetExclusion (4 or more).
 * @author Keith Corlett
 */
public final class AlignedExclusionHint extends AHint implements IActualHint {

	private final Cell[] cells;
	private final String cmnExcluders;
	// iteration order is significant to user understanding explanation
	private final MyLinkedHashSet<Cell> selectedCellsSet;
	// iteration order is significant to user understanding explanation
	private final LinkedHashMap<HashA, Cell> excludedCombosMap;

	AlignedExclusionHint(AHinter hinter, Pots redPots, Cell[] cells
			, String cmnExcluders
			, LinkedHashMap<HashA, Cell> excludedCombosMap) {
		super(hinter, redPots);
		this.cells = cells.clone();
		this.cmnExcluders = cmnExcluders;
		// NB: iteration order is significant to user understanding explanation
		this.selectedCellsSet = new MyLinkedHashSet<>(cells);
		this.excludedCombosMap = excludedCombosMap;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return selectedCellsSet;
	}

	/** Does this combo include one of the removable potentials? */
	private boolean isRelevent(int[] combo) {
		return isRelevant(cells, redPots, combo);
	}

	static boolean isRelevant(Cell[] cells, Pots reds, int[] combo) {
		Values redVals;
		for ( int i=0,n=cells.length; i<n; ++i )
			if ( combo[i] != 0 // most combos are mostly 0's
			  && (redVals=reds.get(cells[i])) != null
			  && (redVals.bits & VSHFT[combo[i]]) != 0 )
				return true;
		return false;
	}

	private Values getReleventComboValues() {
		Values result = new Values();
		for ( HashA combo : excludedCombosMap.keySet() )
			if ( isRelevent(combo.array) )
				result.add(combo.array);
		return result;
	}

	@Override
	public Pots getGreens(int viewNumUnused) {
		if ( greenPots == null ) {
			Pots pots = new Pots();
			Values releventValues = getReleventComboValues();
			for ( Cell c : excludedCombosMap.values() )
				if (c!=null && releventValues.containsAll(c.maybes) )
					pots.put(c, new Values(c.maybes));
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getOranges(int viewNumUnused) {
		if ( orangePots == null ) {
			Pots pots = new Pots();
			for ( Cell cell : cells )
				pots.put(cell, new Values(cell.maybes));
			pots.removeAll(redPots); // remove any collisions with redPots!
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String s = "Look for an " + getHintTypeName();
		if ( isBig )
			s += " at " + Frmt.csv(selectedCellsSet);
		return s;
	}

	/** Gets the color of the given 'cell' and 'value', as a single char, which
	 * will be output as "{$c}" in our local html language, which is translated
	 * into an actual html-font-tag-pair by the HTML.colorIn method.
	 * <p>Returns 'r' for Red if given 'cell'.'value' is a redPot,<br>
	 * or 'o' for orange if cell is in the selectedCellsSet,<br>
	 * or (char)0 meaning default to black.
	 * @param cell to workout the color of
	 * @param value to workout the color of
	 * @return 'r', 'o', or (char)0 */
	private char getColorOf(Cell cell, int value) {
		Values redVals = redPots.get(cell);
		if ( redVals != null )
			if ( redVals.contains(value) )
				return 'r';
			else
				return (char)0; // Red or default black
		if ( selectedCellsSet.contains(cell) )
			return 'o'; // Orange
		return (char)0; // meaning default to black.
	}

	/** Append the HTML of the potential values combo (with optional lockCell)
	 * to the given StringBuilder. A null lockCell just means that "the value
	 *  can occur only once in the region".
	 * @maybe I'm still struggling to understand Aligned*Exclusion, so maybe
	 *  finding a better explanation from an "expert" will help a bit?
	 * @param sb to append to
	 * @param combo An int array of the potential values of the $degree cells
	 *  in this combo. Simultaneous with my cells array.
	 * @param lockCell the cell which disallows this combo because it's maybes
	 *  are a subset of this combos values. */
	private void appendTo(StringBuilder sb, int[] combo, Cell lockCell) {
		// <o><b>2</b></o>, <o><b>6</b></o>, <o><b>5</b></o> and <r><b>3</b></r>
		final int n = Math.min(cells.length, combo.length); // in case the combo is too long (has happened)
		for ( int i=0,m=n-1; i<n; ++i ) {
			// "1, 2 and 3"
			if ( i > 0 ) // not the first
				if ( i < m )
					sb.append(", ");
				else
					sb.append(" and ");
			char c = getColorOf(cells[i], combo[i]);
			if(c!=0) sb.append('<').append(c).append('>');
			sb.append("<b>").append(combo[i]).append("</b>");
			if(c!=0) sb.append("</").append(c).append('>');
		}
		sb.append(" because ");
		if (lockCell == null)
			sb.append("the value can occur only once in the region");
		else
			sb.append("the cell <b>").append(lockCell.id)
			  .append("</b> must contain <g><b>")
			  .append(Frmt.or(lockCell.maybes)).append("</b></g>");
	}

	/** Append the HTML representing the given combosMap to the StringBuilder.
	 * @param sb to append to
	 * @param excludedCombosMap a Map of HashA (the potential values combo) to
	 *  Cell (the lockCell, if any; null is a value collision between sibling
	 *  cells)
	 * @return a StringBuilder which is full of HTML. Yummy!
	 * @throws {@link diuf.sudoku.solver.IrrelevantHintException} */
	private StringBuilder appendTo(StringBuilder sb, Map<HashA, Cell> excludedCombosMap)
			throws IrrelevantHintException {
		// first we get an "index" (keys) of the combos sorted by there values.
		Set<HashA> keySet = excludedCombosMap.keySet();
		HashA[] keyArray = keySet.toArray(new HashA[keySet.size()]);
		Arrays.sort(keyArray, HashA.BY_VALUES_ASC);
		// now we'll append the HTML for each combo to the StringBuilder
		int relevantCount = 0;
		for ( HashA key : keyArray )
			if ( isRelevent(key.array) ) {
				++relevantCount;
				appendTo(sb, key.array, excludedCombosMap.get(key)); // get lockCell, maybe null
				sb.append("<br>").append(NL);
			}
		if ( relevantCount == 0 )
			throw new IrrelevantHintException(); // see declaration for more
		return sb;
	}

	private Values getRemovableValues() {
		int bits = 0;
		for ( Values vs : redPots.values() )
			bits |= vs.bits;
		return new Values(bits, true);
	}

	@Override
	public boolean equals(Object o) {
		if ( o==null || !(o instanceof AlignedExclusionHint) )
			return false;
		AlignedExclusionHint other = (AlignedExclusionHint)o;
		if ( !this.selectedCellsSet.equals(other.selectedCellsSet) )
			return false;
		return this.redPots.equals(other.redPots);
	}

	@Override
	public int hashCode() {
		int result = redPots.hashCode();
		for ( Cell c : selectedCellsSet )
			result = result<<4 ^ c.hashCode;
		return result;
	}

	@Override
	public String toStringImpl() {
		return getHintTypeName()
				+ ": " + Frmt.csv(selectedCellsSet)
				+ " on " + cmnExcluders;
	}

	@Override
	public String toHtmlImpl() {
		final String excludedCombos;
		try {
			StringBuilder sb = new StringBuilder(degree*256);
			excludedCombos = Html.colorIn(appendTo(sb, excludedCombosMap).toString());
		} catch (IrrelevantHintException ex) { // from appendTo
			// see IrrelevantHintException declaration for discussion
			return Html.load(ex, "IrrelevantHintException.html");
		}
		final String filename = degree==2
				? "AlignedExclusionHintPair.html"
				: "AlignedExclusionHint.html";
		return Html.produce(this, filename
			, GROUP_NAMES[degree-2]				//{0}
			, Frmt.and(selectedCellsSet)		// 1
			, excludedCombos					// 2
			, Frmt.and(redPots.keySet())		// 3
			, Frmt.csv(getRemovableValues())	// 4
			, redPots.toString()				// 5
			, cmnExcluders						// 6
		);
	}
}