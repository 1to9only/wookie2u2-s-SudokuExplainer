/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * This means "Your Grid is buggered".
 * @author Keith Corlett 2017 IIRC
 */
public final class UnsolvableException extends RuntimeException {
	private static final long serialVersionUID = 1538294061L;
	public UnsolvableException() { super(); }
	public UnsolvableException(String message) { super(message); }
	public UnsolvableException(String message, Throwable cause) { super(message, cause); }
}
