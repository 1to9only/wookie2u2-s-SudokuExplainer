/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.hidden;

import diuf.sudoku.Ass;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Regions;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.utils.Frmt;
import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.IAssSet;
import diuf.sudoku.utils.MyLinkedList;
import diuf.sudoku.utils.Frmu;
import java.util.Set;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.Values.VALUESES;
import diuf.sudoku.solver.hinters.IFoxyHint;

/**
 * The DTO for a Hidden Set (Pair, Triple, Quad) Hint.
 * <p>
 * HiddenPair: the only two places for these two values in this region.
 * The same logic works for 3, 4, or even 5 locations and values.
 * Note that HiddenPent is not implemented: slow and VERY rare.
 */
public final class HiddenSetHint extends AHint implements IFoxyHint {

	private final Cell[] cells;
	private final int hdnSetRegionIdxBits;
	private final ARegion region;

	/** Used by Locking */
	public final int hdnSetCands;

	/**
	 * Constructor.
	 *
	 * @param hinter that found this hint
	 * @param region which contains this hidden set
	 * @param cands a bitset of the $degree values of the hidden set. <br>
	 *  note that degree is a public final field of AHint, my superclass
	 * @param indexes region indexes of the $degree cells which maybe cands
	 * @param reds cell=&gt;values to be eliminated
	 */
	public HiddenSetHint(final IHinter hinter, final ARegion region
			, final int cands, final int indexes, final Pots reds) {
		super(region.getGrid(), hinter, reds
			, new Pots(region.cells, indexes, cands, F, F) // greens
			, null, null, Regions.array(region), null);
		this.cells = region.atNew(indexes);
		this.hdnSetRegionIdxBits = indexes;
		this.hdnSetCands = cands;
		this.region = region;
	}

	@Override
	public Set<Integer> getAquaBgIndices(int notUsed) {
		return new Idx(cells);
	}

	/**
	 * Find the parentOffs which eliminate any hidden-set value from any other
	 * cell in the region, and so forced these values into only the hidden-set
	 * cells.
	 *
	 * @param initGrid the initial Grid (without erasures when isDynamic)
	 * @param currGrid the current Grid (with erasures when isDynamic)
	 * @param rents a complete Set of the parent Off assumptions
	 * @return a List of the Off assumptions which must be true before this
	 *  hint becomes applicable; ie the s__t that caused me
	 */
	@Override
	public MyLinkedList<Ass> getParents(final Grid initGrid, final Grid currGrid
			, final IAssSet rents) {
		Ass parent;
		final MyLinkedList<Ass> results = new MyLinkedList<>();
		final int[] initPlaces = initGrid.regions[region.index].places;
		for ( int v : VALUESES[hdnSetCands] )
			for ( int i : INDEXES[initPlaces[v] & ~hdnSetRegionIdxBits] )
				if ( (parent=rents.getAss(region.indices[i], v)) != null )
					results.add(parent);
		return results;
	}

	@Override
	public StringBuilder toStringImpl() {
		return SB(64).append(getHintTypeName()).append(COLON_SP)
		.append(Frmu.csv(cells)).append(COLON_SP).append(Frmt.csv(VALUESES[hdnSetCands]))
		.append(IN).append(region.label);
	}

	@Override
	public String toHtmlImpl() {
		return Html.produce(this, "HiddenSetHint.html"
			, NUMBER_NAMES[degree-2]			// {0}
			, Frmu.csv(cells)					//  1
			, Frmt.and(VALUESES[hdnSetCands])	//  2
			, region.label						//  3
			, getHintTypeName()					//  4
			, reds.toString()					//  5
		);
	}

}
