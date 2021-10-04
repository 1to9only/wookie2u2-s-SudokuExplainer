/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * InterruptException is thrown by AHinter.interrupt() when the user presses
 * the Stop button all the way back-up to
 * {@link diuf.sudoku.gen.Generator#generate},
 * which pulls the pin.
 *
 * @author Keith Corlett 2021 July
 */
public class InterruptException extends RuntimeException {
	private static final long serialVersionUID = 10395171735L;
	public InterruptException() { }
}
