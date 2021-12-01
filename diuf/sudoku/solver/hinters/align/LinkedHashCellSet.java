/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.utils.MyLinkedHashSet;

/**
 * LinkedHashCellSet is a drop-in replacement for LinkedMatrixCellSet.
 * <p>
 * I extend MyLinkedHashSet to pick-up it's poll method, and I implement {idx,
 * idx1, and idx2} myself. These are slower than those in LinkedMatrixCellSet.
 * My only concern is correctness, not speed.
 *
 * @author Keith Corlett 2021-09-04
 */
public class LinkedHashCellSet
		extends MyLinkedHashSet<Cell>
		implements ILinkedCellSet
{
	private static final long serialVersionUID = 4569248921L;

	private static final int[] ARRAY = new int[3];

	private Idx idx;
	
	/**
	 * Get an index of this CellSet.
	 *
	 * @return a cached Idx containing the indices of cells in this CellSet.
	 */
	@Override
	public Idx idx() {
		if ( idx == null ) { // cache until this set changes, which nulls idx
			ARRAY[0]=ARRAY[1]=ARRAY[2]=0; // zero THE static array
			// using an Iterator is slower than manual-iterating a linked-list
			super.forEach((cell)->ARRAY[cell.idxdex] |= cell.idxshft);
			idx = new Idx(ARRAY);
		}
		return idx;
	}

	/**
	 * Call {@link #idx()} and set 'result' to the intersection of this Set
	 * and the given 'other' Idx; returning: Is result empty.
	 *
	 * @param result to be repopulated
	 * @param other to bitwise-and with me
	 * @return is result empty
	 */
	@Override
	public boolean idx1(final Idx result, final Idx other) {
		final Idx x = idx();
		// return result isEmpty
		return (result.a0 = x.a0 & other.a0) == 0
			 & (result.a1 = x.a1 & other.a1) == 0
			 & (result.a2 = x.a2 & other.a2) == 0;
	}

	/**
	 * Call {@code idx()} and set 'result' to to the intersection of this Set
	 * and the given 'other' Idx; returning: Does result contain LESS THAN two?
	 * 
	 * @param result to be repopulated
	 * @param other to bitwise-and with me
	 * @return does result contain LESS THAN two indices?
	 */
	@Override
	public boolean idx2(final Idx result, final Idx other) {
		int cnt = 0 // the number of elements that're non-zero
		  , only = 0; // the only element that's non-zero
		final Idx x = idx(); // get my idx (cached)
		if ( (result.a0=x.a0 & other.a0)>0 && ++cnt==1 )
			only = result.a0;
		if ( (result.a1=x.a1 & other.a1)>0 && ++cnt==1 )
			only = result.a1;
		if ( (result.a2=x.a2 & other.a2)>0 && ++cnt==1 )
			only = result.a2;
		// return does the result index contain LESS THAN 2 set (1) bits?
		return cnt==0 || (cnt==1 && VSIZE[ only       & 511]
				                  + VSIZE[(only>>>9)  & 511]
				                  + VSIZE[(only>>>18) & 511] == 1);
	}

}
