/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedHashSet;
import diuf.sudoku.utils.MyLinkedList;
import diuf.sudoku.utils.Frmu;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;


/**
 * Hidden Set (Pair, Triple, Quad) Hint.
 *
 * Hidden means that these are the only two (for example) locations of these
 * two values in this Row. The same logic works for 2, 3, 4, or 5 locations
 * and values.
 */
public final class HiddenSetHint extends AHint implements IChildHint {

	private final Cell[] cells;
	private final int hdnSetRegionIdxBits;
	private final int[] hdnSetValuesArray;
	private final ARegion region;

	/** Used by Locking */
	public final Values hdnSetValues;
	/** Used by Locking */
	public final Idx hdnSetIdx;

	private String ts;

	public HiddenSetHint(AHinter hinter, Cell[] cells, Values hdnSetValues
			, Pots greenPots, Pots redPots, ARegion region
			, int hdnSetRegionIdxBits) {
		super(hinter, redPots, greenPots, null, null, Regions.list(region)
				, null);
		this.cells = cells;
		this.hdnSetRegionIdxBits = hdnSetRegionIdxBits;
		this.hdnSetValues = hdnSetValues;
		this.hdnSetValuesArray = hdnSetValues.toArrayNew();
		assert hdnSetValuesArray.length == super.degree
			: "hdnSetValuesArray "+java.util.Arrays.toString(hdnSetValuesArray)
			+" length "+hdnSetValuesArray.length+" != degree "+degree;
		this.region = region;
		this.hdnSetIdx = Idx.of(cells);
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(cells);
	}

	/**
	 * Find the parentOffs which eliminate any hidden-set value from any other
	 * cell in the region, and so forced these values into only the hidden-set
	 * cells.
	 *
	 * @param initGrid the initial Grid (without erasures when isDynamic)
	 * @param currGrid the current Grid (with erasures when isDynamic)
	 * @param prntOffs a complete Set of the parent Off assumptions
	 * @return a List of the Off assumptions which must be true before this
	 *  hint becomes applicable; ie the s__t that caused me
	 */
	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet prntOffs) {
		final MyLinkedList<Ass> result = new MyLinkedList<>(); // the result
		final Cell[] ic = initGrid.cells; // initialCells
		final Cell[] rc = this.region.cells; // regionCells (in current grid)
		final int[] vs = this.hdnSetValuesArray;
		Cell cc; // currentCell ie currGrid.cell
		Ass p; // parent = the complete parent, with it's own parents, if any
		int j // index
		  , J // index ceiling
		  , initBits // initGrid.cells[indice].maybes.bits
		  , v; // the candidate value
		// foreach cell in my region EXCEPT the hidden-set-cells
		for ( int i : INDEXES[Indexes.ALL_BITS & ~hdnSetRegionIdxBits] )
			// if this initGrid.cell has any maybes
			if ( (initBits=ic[(cc=rc[i]).i].maybes.bits) != 0 )
				// foreach hidden-set-value
				for ( j=0,J=degree; j<J; ++j )
					// if this initGrid.cell maybe this hidden-set-value
					if ( (initBits & VSHFT[v=vs[j]]) != 0
					  // and a parent-off exists for this cell-value
					  && (p=prntOffs.getAss(cc, v)) != null )
						// this parent must be true before I am applicable
						result.add(p);
		return result;
	}

	@Override
	public String toStringImpl() {
		return Frmt.getSB(64).append(getHintTypeName())
		  .append(COLON_SP).append(Frmu.csv(cells))
		  .append(COLON_SP).append(Frmt.csv(hdnSetValuesArray))
		  .append(IN).append(region.id)
		  .toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "HiddenSetHint.html"
				, NUMBER_NAMES[degree-2]		// {0}
				, Frmu.csv(cells)				//  1
				, Frmt.and(hdnSetValuesArray)	//  2
				, region.id						//  3
				, getHintTypeName()				//  4
				, redPots.toString()			//  5
		);
	}

}
