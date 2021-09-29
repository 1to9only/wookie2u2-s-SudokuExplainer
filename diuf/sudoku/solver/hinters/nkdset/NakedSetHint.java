/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.nkdset;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.List;
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

	private final List<Cell> nkdSetCellList;
	private final String regionIds;

	/* Used by Locking. */
	public final Values nkdSetValues;
	/* Used by Locking. */
	public final Idx nkdSetIdx;

	public NakedSetHint(AHinter hinter
			, List<Cell> nkdSetCellList
			, Values nkdSetValues
			, Pots greenPots
			, Pots redPots
			, List<ARegion> bases
	) {
		super(hinter, redPots, greenPots, null, null, bases, null);
		this.nkdSetCellList = nkdSetCellList;
		this.nkdSetValues = nkdSetValues;
		assert super.degree == nkdSetValues.size;
		this.regionIds = Frmu.and(bases);
		this.nkdSetIdx = Idx.of(nkdSetCellList);
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(nkdSetCellList); // 16, 0.75F
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet parentOffs) {
		final int bits = nkdSetValues.bits;
		final Cell[] initGridCells = initGrid.cells;
		MyLinkedList<Ass> result = new MyLinkedList<>(); // the result
		int rmvdBits;
		for ( Cell c : nkdSetCellList )
			// removed := initial cell maybes minus nakedSetValues (may be 0)
			if ( (rmvdBits=initGridCells[c.i].maybes.bits & ~bits) != 0 )
				for ( int v : VALUESES[rmvdBits] )
					// my parent Ass must be applied before I am applicable
					result.add(parentOffs.getAss(c, v));
		return result;
	}

	@Override
	public String toStringImpl() {
		return Frmu.getSB(64).append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(nkdSetCellList)).append(COLON_SP)
		  .append(Frmu.csv(nkdSetValues)).append(IN).append(regionIds)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetHint.html"
				, NUMBER_NAMES[degree-2]	// {0}
				, Frmu.and(nkdSetCellList)//  1
				, Frmu.and(nkdSetValues)	//  2
				, regionIds					//  3
				, getHintTypeName()			//  4
				, redPots.toString()		//  5
		);
	}
}