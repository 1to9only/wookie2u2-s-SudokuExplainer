/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

/**
 * The Turing Stopping Problem: Alan Turing muted the proposition that its
 * impossible to prove that any computer program will come to a conclusion;
 * hence a program should monitor itself, to see if its in an endless loop,
 * and if so throw a TuringException.
 *
 * @author Keith Corlett 2021-07-12
 */
class TuringException extends RuntimeException {

	private static final long serialVersionUID = 2908888493509L;

	TuringException(String msg) {
		super(msg);
	}

}
