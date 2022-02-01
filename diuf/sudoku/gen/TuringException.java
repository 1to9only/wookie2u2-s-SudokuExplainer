/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gen;

/**
 * The Turing Stopping Problem: One interesting thing that Alan Turing
 * posited was the proposition that it is impossible to prove that any
 * computer program will come to a conclusion, hence every program should
 * monitor itself, to see if it is in an endless loop, and if so throw a
 * wobbly. TuringException is my preferred wobbly.
 *
 * @author Keith Corlett 2021-07-12
 */
public class TuringException extends RuntimeException {
	private static final long serialVersionUID = 2908888493509L;
	public TuringException(String msg) { super(msg); }
}
