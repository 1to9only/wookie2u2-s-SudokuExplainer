/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.align2;

import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.Collection;


/**
 * CellSet facilitates an easy swap between LinkedMatrixCellSet and any other
 * implementation of {@code Set<Cell>}, such as LinkedHashCellSet, which is
 * slower (built for correctness, not speed, as an alternative for testing).
 * <p>
 * I currently use LinkedMatrixCellSet for it's Idx methods, which you will
 * need to add to alternatives to make them work here.
 * <p>
 * I implement advanced constructors for A*E.
 * <p>
 * I implement toFullString for debugging.
 *
 * @author Keith Corlett
 */
public class CellSet extends LinkedMatrixCellSet {

	/**
	 * Constructs a new empty CellSet.
	 */
	CellSet() {
	}

	/**
	 * Constructs new CellSet populated with the given collection.
	 * <p>I do the addAll manually, which is faster for sets of less than 32
	 * elements, which is pretty normal in Sudoku land.
	 *
	 * @param c {@code Collection<Cell>} to addAll to this Set.
	 */
	public CellSet(Collection<Cell> c) {
		for ( Cell cell : c )
			add(cell);
	}

	/**
	 * Constructs a new CellSet containing those cells which are common to both
	 * Sets 'a' and 'b'. Use this rather than set.addAll(a) only to immediately
	 * remove most of them with set.retaimAll(b). This is faster.
	 *
	 * @param a the cells to add (if also in 'b') whose iterator we shall use.
	 * @param b the set whose contains method we shall hammer.
	 */
	CellSet(CellSet a, CellSet b) {
		for ( Cell cell : a )
			if ( b.contains(cell) )
				add(cell);
	}

	/**
	 * Constructs a new CellSet of the cells in the Idx.
	 *
	 * @param cells the cells to be added (grid.cells)
	 * @param idx the indices of cells to add
	 */
	CellSet(Cell[] cells, Idx idx) {
		idx.forEach(cells, (c)->add(c));
	}

	/**
	 * Returns the id's, separated by sep, of cells in this CellSet.
	 *
	 * @param sep
	 * @return
	 */
	String toFullString(String sep) {
		Node n = head;
		if ( n == null )
			return EMPTY_STRING;
		StringBuilder sb = new StringBuilder((sep.length()+2)*size);
		sb.append(n.cell.id);
		for ( n=n.next; n!=null; n=n.next )
			sb.append(sep).append(n.cell.id);
		return sb.toString();
	}

}
