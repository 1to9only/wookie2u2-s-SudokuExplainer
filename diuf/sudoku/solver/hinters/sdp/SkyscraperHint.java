/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;

/**
 * SkyscraperHint contains the hint data for display in the GUI, and also
 * produces the HTML and the String representation of a Skyscraper hint.
 * @author Keith Corlett 2020 Mar 26
 */
public class SkyscraperHint extends AHint implements IActualHint {

	private final Pots orangePots;
	private final List<ARegion> bases;
	private final List<ARegion> covers;
	private final int valueToRemove;
	public SkyscraperHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots redPots, Pots orangePots) {
		super(hinter, redPots);
		this.orangePots = orangePots;
		this.bases = bases;
		this.covers = covers;
		this.valueToRemove = value;
	}

	@Override
	public Set<Cell> getAquaCells(int unusedViewNum) {
		return orangePots.keySet();
	}

	@Override
	public Pots getOranges(int unusedViewNum) {
		return orangePots;
	}

	@Override
	public Pots getReds(int unusedViewNum) {
		return redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return bases;
	}

	@Override
	public List<ARegion> getCovers() {
		return covers;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		return "Look for a " + getHintTypeName()
			+ (isBig ? " on "+Frmt.and(new Values(orangePots.values())) : "");
	}

	@Override
	public String toStringImpl() {
		StringBuilder sb = Frmt.getSB();
		sb.append(getHintTypeName())
		  .append(": ").append(Frmt.csv(orangePots.keySet()))
		  .append(" on ").append(Integer.toString(valueToRemove));
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "SkyscraperHint.html"
				, Integer.toString(valueToRemove)	// {0}
				, bases.get(0).id			//  1
				, bases.get(1).id			//  2
				, covers.get(0).typeName	//  3
		);
	}
	
}
