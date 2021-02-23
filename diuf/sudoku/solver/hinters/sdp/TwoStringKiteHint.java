/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.sdp;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Set;

/**
 *
 * @author User
 */
public class TwoStringKiteHint extends AHint {

	private final int redValue;
	private final List<ARegion> bases;
	private final List<ARegion> covers;
	private final Pots orangePots;
	private final Pots bluePots;
	private final Cell[] rowPair;
	private final Cell[] colPair;
	public TwoStringKiteHint(AHinter hinter, int value, List<ARegion> bases
			, List<ARegion> covers, Pots orangePots, Pots bluePots, Pots redPots
			, Cell[] rowPair, Cell[] colPair) {
		super(hinter, redPots);
		this.redValue = value;
		this.bases = bases;
		this.covers = covers;
		this.orangePots = orangePots;
		this.bluePots = bluePots;
		this.rowPair = rowPair;
		this.colPair = colPair;
	}

	@Override
	public Set<Grid.Cell> getAquaCells(int unusedViewNum) {
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
	public Pots getBlues(Grid gridUnused, int viewNum) {
		return bluePots;
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
		  .append(" on ").append(Integer.toString(redValue));
		return sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		Cell redCell = redPots.firstKey();
		return Html.produce(this, "TwoStringKiteHint.html"
				, Integer.toString(redValue)	// {0}
				, covers.get(0).id			//  1
				, covers.get(1).id			//  2
				, bases.get(0).id			//  3
				, redCell.id				//  4
				, rowPair[0].id				//  5
				, rowPair[1].id				//  6
				, colPair[0].id				//  7
				, colPair[1].id				//  8
		);
	}

}
