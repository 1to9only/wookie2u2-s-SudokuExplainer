/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import java.util.Collection;


/**
 * CellSet is just an alias to allow us to swap-out LinkedMatrixCellSet.
 * LinkedMatrixCellSet has O(1) contains & remove, + fast iterator;
 * at the expense of more memory and therefore slow construction, ergo:
 * <tt>2,432,277,297 ns / 771,672 = 3,151 each for top1465</tt>.
 * <p>KRC 2019-09-04 exhumed from AlignedTripleExlusion for use in
 * AlignedPairExlusiion, AlignedSetExclusion, and  AlignedPentExclusion
 * (currently deceased).
 * <p>KRC 2019-09-10 had to make CellSet public to use it in the Debug class.
 * @author Keith Corlett
 */
public class CellSet extends LinkedMatrixCellSet {

	/** Constructs a new empty CellSet. */
	CellSet() {
		super();
	}

	/** Constructs new CellSet populated with the given collection.
	 * <p>I do the addAll manually, which is faster for sets of less than 32
	 * elements, which is pretty normal in Sudoku land.
	 * @param c {@code Collection<Cell>} to addAll to this Set. */
	public CellSet(Collection<Cell> c) {
		super();
		for ( Cell cell : c )
			add(cell);
	}

	/** Constructs a new CellSet containing those cells which are common to both
	 * Sets 'a' and 'b'. Use this rather than set.addAll(a) only to immediately
	 * remove most of them with set.retaimAll(b). This is faster.
	 * @param a the cells to add (if also in 'b') whose iterator we shall use.
	 * @param b the set whose contains method we shall hammer. */
	CellSet(CellSet a, CellSet b) {
		for ( Cell cell : a )
			if ( b.contains(cell) )
				add(cell);
	}

}
