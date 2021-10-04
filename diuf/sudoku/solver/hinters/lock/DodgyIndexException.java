/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.lock;

/**
 * Thrown when the indexes are ____ed.
 *
 * @author Keith Corlett 2012-07-16
 */
class DodgyIndexException extends RuntimeException {
	private static final long serialVersionUID = 1201102102122L;
	public DodgyIndexException() { }
}
