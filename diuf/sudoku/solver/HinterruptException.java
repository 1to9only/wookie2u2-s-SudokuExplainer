/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * HinterruptException is thrown by
 * {@link diuf.sudoku.solver.hinters.AHinter#interrupt()}
 * when the user presses the Stop button. It is caught by
 * {@link diuf.sudoku.gen.Generator#generate}
 * which pulls the pin.
 * <p>
 * I'm called HinterruptException because I enjoy wordplay, and I reckon
 * that InterruptException should be a reserved word, because thou shalt
 * avoid impinging on interrupting threads in any way, because it is
 * already dodgier than a priest doing push-ups in the long grass.
 *
 * @author Keith Corlett 2021 July
 */
public class HinterruptException extends RuntimeException {
	private static final long serialVersionUID = 10395171736L;
	public HinterruptException() { }
}
