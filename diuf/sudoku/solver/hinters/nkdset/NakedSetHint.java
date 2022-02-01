/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;

/**
 * A Naked Set Hint is produced when, for example 2 cells in a box maybe just
 * 2 values: 8 and 9: hence one of those cells will be 8 and the other 9;
 * therefore no other cell in the box may be 8 or 9; so we strip 8,9 from the
 * maybes of the other 7 cells in the box.
 */
public final class NakedSetHint extends AHint implements IChildHint {

	// the cells in the naked set
	private final Cell[] nkdSetCells;
	// a bitset of the naked set candidate values
	private final int nkdSetCands;
	// a string of the region.id's of the bases (the regions we searched)
	private final String regionIds;

	public NakedSetHint(AHinter hinter, Cell[] nkdSetCells, int nkdSetCands
			, Pots greenPots, Pots redPots, ARegion[] bases) {
		super(hinter, redPots, greenPots, null, null, bases, null);
		this.nkdSetCells = nkdSetCells;
		assert super.degree == nkdSetCells.length;
		this.nkdSetCands = nkdSetCands;
		assert super.degree == Values.VSIZE[nkdSetCands];
		this.regionIds = Frmt.and(bases);
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(nkdSetCells); // 16, 0.75F
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid ig, Grid cg, IAssSet rents) {
		final Cell[] igcs = ig.cells;
		final MyLinkedList<Ass> result = new MyLinkedList<>();
		int goners;
		for ( Cell c : nkdSetCells )
			if ( (goners=igcs[c.i].maybes & ~nkdSetCands) != 0 )
				for ( int v : VALUESES[goners] )
					result.add(rents.getAss(c, v));
		return result;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB(64).append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(nkdSetCells)).append(COLON_SP)
		  .append(Values.csv(nkdSetCands)).append(IN).append(regionIds)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetHint.html"
			, NUMBER_NAMES[degree-2]		// {0}
			, Frmu.and(nkdSetCells)			//  1
			, Values.andString(nkdSetCands)	//  2
			, regionIds						//  3
			, getHintTypeName()				//  4
			, reds.toString()				//  5
		);
	}
}