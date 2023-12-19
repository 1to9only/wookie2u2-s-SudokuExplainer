/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSHIFTED;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IrrelevantHintException;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.Frmu;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * AlignedExclusionHints are created by AlignedPairExclusion,
 * AlignedTripleExclusion, and AlignedSetExclusion (4 or more).
 * @author Keith Corlett
 */
public final class AlignedExclusionHint extends AHint  {

	private final Cell[] cells;
	private final Cell[] cmnExcluders;
	private final Idx alignedSet;
	private final LinkedHashMap<HashA, Cell> excludedCombosMap;

	AlignedExclusionHint(Grid grid, IHinter hinter, Pots reds, Cell[] cells
			, Cell[] excluders, LinkedHashMap<HashA, Cell> excludedCombosMap) {
		super(grid, hinter, reds);
		this.cells = cells.clone();
		this.cmnExcluders = excluders;
		this.alignedSet = new Idx(cells);
		this.excludedCombosMap = excludedCombosMap;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return alignedSet;
	}

	static boolean isRelevent(Cell[] cells, Pots reds, int[] combo) {
		Integer vals;
		for ( int i=0,n=cells.length; i<n; ++i )
			// nb: many combos are mostly 0's, so check > 0 first, for speed
			if ( combo[i] > 0 // 9bits
			  && (vals=reds.get(cells[i].indice)) != null
			  && (vals & VSHFT[combo[i]]) > 0 ) // 9bits
				return true;
		return false;
	}

	private int getReleventComboValues() {
		int result = 0;
		for ( HashA combo : excludedCombosMap.keySet() )
			if ( isRelevent(cells, reds, combo.array) )
				for ( int i : combo.array )
					result |= VSHFT[i];
		return result;
	}

	@Override
	public Pots getGreenPots(int viewNumUnused) {
		if ( greenPots == null ) {
			Pots pots = new Pots();
			int releventValues = getReleventComboValues();
			for ( Cell c : excludedCombosMap.values() )
				if ( c!=null && (releventValues&c.maybes)!=0 )
					pots.put(c.indice, c.maybes);
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots; // WORM

	@Override
	public Pots getOrangePots(int viewNumUnused) {
		if ( orangePots == null ) {
			final Pots pots = new Pots(cells);
			pots.removeAll(reds); // remove any collisions with redPots!
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots; // WORM

	@Override
	public String getClueHtmlImpl(final boolean isBig) {
		String s = "Look for an " + getHintTypeName();
		if ( isBig )
			s += " at " + Frmt.csv(ids(alignedSet));
		return s;
	}

	/** Gets the color of the given $cell and $value, as a single char, which
	 * will be output as "{$c}" in our local html language, which is translated
	 * into an actual html-font-tag-pair by the HTML.colorIn method.
	 * <p>Returns 'r' for Red if given $cell.$value is a redPot,<br>
	 * or 'o' for orange if cell is in the selectedCellsSet,<br>
	 * or (char)0 meaning default to black.
	 * @param cell to workout the color of
	 * @param value to workout the color of
	 * @return 'r', 'o', or (char)0 */
	private char getColorOf(Cell cell, int value) {
		Integer redVals = reds.get(cell.indice);
		if ( redVals != null )
			if ( (redVals & VSHFT[value]) > 0 ) // 9bits
				return 'r';
			else
				return (char)0; // Red or default black
		if ( alignedSet.contains(cell.indice) )
			return 'o'; // Orange
		return (char)0; // meaning default to black.
	}

	/** Append the HTML of the potential values combo (with optional lockCell)
	 * to the given StringBuilder. A null lockCell just means that "the value
	 *  can occur only once in the region".
	 * @maybe I am still struggling to understand Aligned*Exclusion, so maybe
	 *  finding a better explanation from an "expert" will help a bit?
	 * @param sb to append to
	 * @param combo An int array of the potential values of the $degree cells
	 *  in this combo. Simultaneous with my cells array.
	 * @param lockCell the cell which disallows this combo because it is maybes
	 *  are a subset of this combos values. */
	private void append(StringBuilder sb, int[] combo, Cell lockCell) {
		// <o><b>2</b></o>, <o><b>6</b></o>, <o><b>5</b></o> and <r><b>3</b></r>
		final int n = Math.min(cells.length, combo.length); // in case the combo is too long (has happened)
		for ( int i=0,m=n-1; i<n; ++i ) {
			// "1, 2 and 3"
			if ( i > 0 ) // not the first
				if ( i < m )
					sb.append(CSP);
				else
					sb.append(AND);
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
			  .append(Values.orString(lockCell.maybes)).append("</b></g>");
	}

	/** Append the HTML representing the given combosMap to the StringBuilder.
	 * @param sb to append to
	 * @param map a Map of HashA (the potential values combo) to Cell (the
	 *  lockCell, if any; null is a value collision between sibling cells)
	 * @return a StringBuilder which is full of HTML. Yummy!
	 * @throws {@link diuf.sudoku.solver.IrrelevantHintException} */
	private StringBuilder appendTo(StringBuilder sb, Map<HashA, Cell> map)
			throws IrrelevantHintException {
		// first we get an "index" (keys) of the combos sorted by there values.
		Set<HashA> keySet = map.keySet();
		HashA[] keyArray = keySet.toArray(new HashA[keySet.size()]);
		Arrays.sort(keyArray, HashA.BY_VALUES_ASC);
		// append the HTML for each combo to the StringBuilder
		int relevantCount = 0;
		for ( HashA key : keyArray )
			if ( isRelevent(cells, reds, key.array) ) {
				++relevantCount;
				append(sb, key.array, map.get(key)); // get lockCell, nullable
				sb.append("<br>").append(NL);
			}
		if ( relevantCount == 0 )
			throw new IrrelevantHintException(); // see declaration for more
		return sb;
	}

	private int getRemovableValues() {
		int result = 0;
		for ( Integer values : reds.values() )
			for ( int sv : VSHIFTED[values] )
				result |= sv;
		return result;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof AlignedExclusionHint
			&& equals((AlignedExclusionHint)o);
	}

	public boolean equals(final AlignedExclusionHint other) {
		return alignedSet.equals(other.alignedSet)
			&& reds.equals(other.reds);
	}

	@Override
	public int hashCode() {
		return (reds.hashCode()<<4) ^ alignedSet.hashCode();
	}

	@Override
	public StringBuilder toStringImpl() {
		return Frmt.ids(SB(128).append(getHintTypeName()).append(COLON_SP)
		, alignedSet, CSP, CSP).append(ON).append(Frmu.ssv(cmnExcluders));
	}

	// returns html defining the dropped excluders and why they were dropped.
	// If there are none I just return a full stop, to end the sentence.
	private String droppedExcluders() {
		int allCands = 0;
		final int[] gridMaybes = grid.maybes;
		for ( int indice : alignedSet ) {
			allCands |= gridMaybes[indice];
		}
		int cnt = 0, cands;
		Pots dropped = null;
		for ( Cell c : cmnExcluders ) {
			if ( (cands=c.maybes & ~allCands) > 0 ) { // 9bits
				if ( dropped == null )
					dropped = new Pots();
				dropped.put(c.indice, cands);
				++cnt;
			}
		}
		switch ( cnt ) {
		case 0: return ".";
		case 1: {
			assert dropped != null;
			final int indice = dropped.firstKey();
			cands = dropped.get(indice);
			final int n = VSIZE[cands];
			final String candS = Values.andString(cands);
			final String these = Frmt.plural(n, "this", "these");
			final String values = Frmt.plural(n, "value");
			String s = ", but <g><b>"+CELL_IDS[indice]+"</b></g> maybe <b>"+candS+"</b>"
			+" and none of the cells in the <g>aligned set</g> maybe "+these
			+" "+values+", so this cell is useless as an excluder cell"
			+", and is therefore disregarded.";
			return Html.colorIn(s);
		}
		default: {
			// size * 32 + 170 is approx trailing length
			assert dropped != null;
			final StringBuilder sb = SB((dropped.size()<<6) + 170);
			sb.append(", but ");
			final Integer[] indices = dropped.keySet().toArray(new Integer[dropped.size()]);
			for ( int i=0,m=cnt-1; i<cnt; ++i ) {
				final int indice = indices[i];
				final String candS = Values.andString(dropped.get(indice));
				if(i>0) if(i<m) sb.append(", "); else sb.append(" and ");
				sb.append("<g><b>").append(CELL_IDS[indice]).append("</b></g> maybe <b>")
				  .append(candS).append("</b>");
			}
			sb.append(" and none of the cells in the <g>aligned set</g> maybe"
			+" these values, so these cells are useless as excluder cells"
			+", and are therefore disregarded.");
			return Html.colorIn(sb.toString());
		}
		}
	}

	@Override
	public String toHtmlImpl() {
		final String excludedCombos;
		try {
			// 256 is just a guess, but observed to be big enough
			excludedCombos = Html.colorIn(appendTo(SB(256), excludedCombosMap).toString());
		} catch (IrrelevantHintException ex) { // from appendTo
			// see IrrelevantHintException declaration for discussion
			return Html.load(ex, "IrrelevantHintException.html");
		}
		final String filename = degree==2
				? "AlignedExclusionHintPair.html"
				: "AlignedExclusionHint.html";
		return Html.produce(this, filename
			, GROUP_NAMES[degree-2]				//{0}
			, Frmt.and(ids(alignedSet))			// 1
			, excludedCombos					// 2
			, Frmt.and(ids(reds.keySet()))		// 3
			, Values.csv(getRemovableValues())	// 4
			, reds.toString()					// 5
			, Frmu.ssv(cmnExcluders)			// 6
			, droppedExcluders()				// 7
		);
	}

}
