/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.bug;

import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.ON;

/**
 * Bug3Hint holds the data for a "BUG type 3:" hint.
 * <p>
 * Bug3Hint is untested as at 2020-08-26 because I don't have a Sudoku puzzle
 * that contains one.
 * <p>
 * There are 29 "BUG type 1:"s and just 1 "BUG type 2:"s in top1465 if you
 * turn-off all the HoDoKu hint types. If you turn on all the HoDoKu hint-types
 * (especially Coloring) then no BUG's are found so you may as well deselect me.
 *
 * @author Nicolas Juillerat
 */
public final class Bug3Hint extends ABugHint  {

	private final Set<Cell> bugCells;
	private final Cell[] nkdSetCells;
	private final Map<Cell, Integer> extraValues;
	private final Integer allExtraValues;
	private final Integer nkdSetCands;
	private final ARegion region;

	public Bug3Hint(AHinter hinter, Pots redPots, Cell[] nakedSetCells
			, Map<Cell, Integer> extraValues, Integer allExtraValues
			, Integer nakedSetCands, ARegion region) {
		super(hinter, redPots);
		this.bugCells = extraValues.keySet();
		this.extraValues = extraValues;
		this.allExtraValues = allExtraValues;
		this.nkdSetCells = nakedSetCells;
		this.nkdSetCands = nakedSetCands;
		assert super.degree == diuf.sudoku.Values.VSIZE[nakedSetCands];
		this.region = region;
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return bugCells;
	}

	@Override
	public Pots getGreens(int viewNum) {
		if ( greenPots == null ) {
			Pots pots = new Pots(bugCells.size()+nkdSetCells.length, 1F);
			for ( Cell c : bugCells )
				pots.put(c, nkdSetCands & extraValues.get(c)); // green
			greenPots = pots;
		}
		return greenPots;
	}
	private Pots greenPots;

	@Override
	public Pots getReds(int viewNum) {
		if ( myRedPots == null ) {
			Pots pots = new Pots(super.reds.size()+nkdSetCells.length, 1F);
			pots.putAll2(reds);
			for ( Cell c : nkdSetCells )
				pots.put(c, nkdSetCands); // orange
		}
		return myRedPots;
	}
	private Pots myRedPots;

	@Override
	public Pots getOranges(int viewNum) {
		if ( orangePots == null ) {
			Pots pots = new Pots(bugCells.size()+nkdSetCells.length, 1F);
			for ( Cell c : nkdSetCells )
				pots.put(c, nkdSetCands); // orange
			orangePots = pots;
		}
		return orangePots;
	}
	private Pots orangePots;

	@Override
	public List<ARegion> getBases() {
		return Regions.list(region);
	}

	@Override
	public double getDifficulty() {
		// hinter.tech.difficulty + 0.1 for each cell in the naked/hidden set
		return super.getDifficulty() + degree*0.1;
	}

	@Override
	public String getHintTypeNameImpl() {
		return "BUG type 3";
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(bugCells))
		  .append(ON).append(Frmu.csv(nkdSetCands))
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "Bug3Hint.html"
				, Values.andString(allExtraValues)	//{0}
				, Frmu.and(bugCells)				// 1
				, Frmu.or(bugCells)					// 2
				, Values.orString(allExtraValues)	// 3
				, GROUP_NAMES[degree-2]				// 4
				, Frmu.and(nkdSetCells)				// 5
				, Values.andString(nkdSetCands)		// 6
				, region.id							// 7
		);
	}
}