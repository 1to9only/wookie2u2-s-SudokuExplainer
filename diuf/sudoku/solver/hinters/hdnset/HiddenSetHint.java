/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hdnset;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IActualHint;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.Ass;
import diuf.sudoku.Regions;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.MyLinkedHashSet;
import java.util.Set;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.solver.hinters.IChildHint;
import diuf.sudoku.utils.MyLinkedList;
import java.util.List;


/**
 * Hidden Set (Pair, Triple, Quad) Hint.
 *
 * Hidden means that these are the only two (for example) locations of these
 * two values in this Row. The same logic works for 2, 3, 4, or 5 locations
 * and values.
 */
public final class HiddenSetHint extends AHint implements IActualHint, IChildHint {

	private final Cell[] cells;
	private final int hdnSetRegionIdxBits;
	private final int[] hdnSetValuesArray;
	private final Pots greenPots;
	private final Grid.ARegion region;

	/** Used by Locking */
	public final Values hdnSetValues;
	/** Used by Locking */
	public final Idx hdnSetIdx;

	private String ts;

	public HiddenSetHint(AHinter hinter, Cell[] cells, Values hdnSetValues
			, Pots greenPots, Pots redPots, Grid.ARegion region
			, int hdnSetRegionIdxBits) {
		super(hinter, redPots);
		this.cells = cells;
		this.hdnSetRegionIdxBits = hdnSetRegionIdxBits;
		this.hdnSetValues = hdnSetValues;
		this.hdnSetValuesArray = hdnSetValues.toArrayNew();
		assert super.degree == hdnSetValuesArray.length
				: " degree "+degree+" != length "+hdnSetValuesArray.length
				+ " " + diuf.sudoku.utils.Frmt.csv(hdnSetValuesArray);
		this.greenPots = greenPots;
		this.region = region;
		this.hdnSetIdx = Idx.of(cells);
	}

	@Override
	public Set<Cell> getAquaCells(int notUsed) {
		return new MyLinkedHashSet<>(cells);
	}

	@Override
	public Pots getGreens(int viewNum) {
		return greenPots;
	}

	@Override
	public Pots getReds(int viewNum) {
		return redPots;
	}

	@Override
	public List<ARegion> getBases() {
		return Regions.list(this.region);
	}

	@Override
	public MyLinkedList<Ass> getParents(Grid initGrid, Grid currGrid
			, IAssSet prntOffs) {
		final MyLinkedList<Ass> result = new MyLinkedList<>(); // the result
		final Cell[] ic = initGrid.cells; // initialCells
		final Cell[] rc = this.region.cells; // regionCells
		final int[] ISHFT = Indexes.SHFT;
		final int[] SHFT = Values.SHFT;
		final int[] hdnSetVals = this.hdnSetValuesArray;
		final int bits = this.hdnSetRegionIdxBits;
		Cell cc; // currentCell ie currGrid.cell
		Ass p; // completeParent
		int j,J, initBits, v; // index, second index, initGrid cells maybes bits
		// foreach cell in my region EXCEPT the hidden-set-cells
		for ( int i=0; i<9; ++i )
			if ( (bits & ISHFT[i]) == 0 ) // EXCEPT hidden-set-cells
				// for each hidden-set-value of this cell in the initGrid
				if ( (initBits=ic[(cc=rc[i]).i].maybes.bits) != 0 )
					for ( j=0,J=degree; j<J; ++j )
						if ( (initBits & SHFT[v=hdnSetVals[j]]) != 0
						  && (p=prntOffs.getAss(cc, v)) != null )
							// This assumption must be true before I am applicable
							result.add(p);
		return result;
	}

	@Override
	public String getClueHtmlImpl(boolean isBig) {
		String regionID = isBig ? getFirstRegionId() : null;
		return "Look for a " + getHintTypeName()
			+ (regionID!=null ? " in <b1>" + regionID + "</b1>" : "");
	}

	@Override
	public String toStringImpl() {
		if ( ts != null )
			return ts;
		StringBuilder sb = Frmt.getSB();
		sb.append(Frmt.enspace(hinter.tech.name())) // Direct or not.
		  .append(": ").append(Frmt.csv(cells))
		  .append(": ").append(Frmt.csv(hdnSetValuesArray))
		  .append(" in ").append(region.id);
		return ts = sb.toString();
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "HiddenSetHint.html"
				, NUMBER_NAMES[degree-2]	// {0}
				, Frmt.csv(cells)			//  1
				, Frmt.and(hdnSetValuesArray)	//  2
				, region.id					//  3
				, getHintTypeName()			//  4
		);
	}
}
