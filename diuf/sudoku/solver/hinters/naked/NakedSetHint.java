/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.naked;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Frmu;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import diuf.sudoku.solver.hinters.IFoxyHint;
import diuf.sudoku.utils.Debug;

/**
 * A Naked Set Hint is produced when, for example 2 cells in a box maybe just
 * 2 values: 8 and 9: hence one of those cells will be 8 and the other 9;
 * therefore no other cell in the box may be 8 or 9; so we strip 8,9 from the
 * maybes of the other 7 cells in the box.
 */
public final class NakedSetHint extends AHint implements IFoxyHint {

	// the cells in the naked set
	private final Cell[] nkdSetCells;
	// a bitset of the naked set candidate values
	private final int nkdSetCands;

	public NakedSetHint(final Grid grid, final IHinter hinter
			, final Cell[] nkdSetCells, final int nkdSetCands
			, final Pots greenPots, final Pots redPots
			, final ARegion[] bases) {
		super(grid, hinter, redPots, greenPots, null, null, bases, null);
		this.nkdSetCells = nkdSetCells;
		assert super.degree == nkdSetCells.length;
		this.nkdSetCands = nkdSetCands;
		assert super.degree == Values.VSIZE[nkdSetCands];
	}

	@Override
	public Set<Integer> getAquaBgIndices(final int notUsed) {
		return new Idx(nkdSetCells); // 16, 0.75F
	}

	@Override
	public MyLinkedList<Ass> getParents(final Grid initialGrid, final Grid currentGrid, final IAssSet rents) {
		final int[] initMaybes = initialGrid.maybes; // initialCells
		final MyLinkedList<Ass> results = new MyLinkedList<>();
		int pinkBits;
		for ( Cell cell : nkdSetCells )
			if ( (pinkBits=initMaybes[cell.indice] & ~nkdSetCands) > 0 ) // 9bits
				for ( int value : VALUESES[pinkBits] )
					results.add(rents.getAss(cell.indice, value));
		return results;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		  .append(Frmu.csv(nkdSetCells)).append(COLON_SP)
		  .append(Values.csv(nkdSetCands)).append(IN).append(Frmt.and(bases));
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "NakedSetHint.html"
			, NUMBER_NAMES[degree-2]		// {0}
			, Frmu.and(nkdSetCells)			//  1
			, Values.andString(nkdSetCands)	//  2
			, Frmt.and(bases)				//  3
			, getHintTypeName()				//  4
			, reds.toString()				//  5
		);
	}

}
