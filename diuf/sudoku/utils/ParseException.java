/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.utils;

/** 
 * ParseException is thrown when we failed to parse a line.
 */
public final class ParseException extends RuntimeException {
	private static final long serialVersionUID = 67857884L;
	public final int lineNumber;
	public ParseException(String message, int lineNumber) { 
		super(message);
		this.lineNumber=lineNumber;
	}
}