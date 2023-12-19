/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

/**
 * HinterruptException is thrown when the user stops Generate.
 * <p>
 * When the user presses the Stop button in the Generate dialog, the private
 * static volatile boolean {@link diuf.sudoku.Run#isHinterupted} is set true.
 * Slow hinters invoke {@link diuf.sudoku.solver.hinters.AHinter#hinterrupt()}
 * in there main loop (every 100..333 milliseconds is best). hinterrupt throws
 * a HinterruptException if {@link {@link diuf.sudoku.Run#isHinterupted} is
 * true. The HinterruptException propagates back-up the call-stack until
 * somebody catches it, <u>or its super-classes</u>. Warning Will Robinson!
 * <p>
 * HinterruptException is caught by
 * {@link diuf.sudoku.gen.Generator#generate}
 * which terminates it's loop, sans Sudoku.
 * <p>
 * <b>NOBODY</b> else catches HinterruptException or its super-classes:
 * RuntimeException, Exception, or Throwable. Are we clear?!?!
 * <p>
 * My definition of "slow" is the fastest chainer. Currently, this includes
 * AlignedExclusion (all sizes) and all of the complex fish: FinnedFisherman,
 * FrankenFisherman, KrakenFisherman, and MutantFisherman. It is time per call
 * that matters here (absolute), NOT time per elimination (effectiveness).
 * Generally, the less often you call hinterrupt the better.
 * <p>
 * I once put a timer in hinterrupt (VERY slow with a high-precision timer).
 * It's called FAR more often than the ideal 100..333 ms. It is what it is.
 * The fact that I needed a HIGH-PRECISION timer tells you everything. Sigh.
 * <p>
 * I am called HinterruptException because I treat the word interrupt as a
 * reserved word, because thou shalt avoid impinging thread-interruption,
 * because its dodgier than a priest doing push-ups in the long grass.
 * That and I just like the word play (Adams D.).
 *
 * @author Keith Corlett 2021 July
 */
public class HinterruptException extends RuntimeException {

	private static final long serialVersionUID = 10395171736L;

	public HinterruptException() { }

}
