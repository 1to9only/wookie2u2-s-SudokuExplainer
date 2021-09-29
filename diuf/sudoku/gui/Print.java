/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.utils.Log;
import diuf.sudoku.utils.MyStrings;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Static helper methods to format and print AHints to System.out by default.
 * <p>
 * Packaging: Not sure where I belong. I'm not model, nor interface; more of an
 * iodel: a bridge between an interface and the model; a models interface; a
 * thing that presents stuff. I'll never really understand MVC. Too many things
 * that don't really fit in any category, so usually end-up in the controller,
 * simply because it knows about both the model and interface. I want an
 * M-MV-V-VC-VM-C pattern, which is too complex to contemplate, so cannot be
 * defined precisely; but IS realistic. Excessive abstraction is unhealthy.
 * <p>
 * I'm called by:<ul>
 * <li> {@link diuf.sudoku.gui.SudokuExplainer#applySelectedHints}
 * <li> {@link diuf.sudoku.solver.hinters.color.GEMHintMulti#applyImpl}
 * </ul>
 *
 * @author Keith Corlett 2021-03-30
 */
public class Print {

	public static String PUZZLE_SUMMARY_HEADERS;

	public static void initialise() {
		//nb: use StringWriter coz PrintWriter.toString is Object.toString
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		pw.format("\n%5s\t%17s\t%17s\t%5s\t%5s\t%5s\t%4s\t%7s\t%s\n"
				, "", "time (ns)", "average (ns)", "calls"
				, "hints", "elims", "maxD", "ttlDiff", "hinter");
		PUZZLE_SUMMARY_HEADERS = sw.toString();
	}

	public static void grid(final PrintStream out, final Grid grid) {
		if (Log.MODE < Log.VERBOSE_3_MODE)
			return;
		if ( out==null || grid==null )
			return;
		out.format("\n%d/%s\n", grid.hintNumber, grid.source());
		out.println(grid.toString());
	}

	/**
	 * Print the hint details; Usually print prev when the next hint is sought,
	 * then this is called when the last hint is applied, coz there's NO next.
	 * This is a by-product of pinning printed output to user action.
	 * Note: times slow in GUI coz it's time between hints being apply'd, which
	 * includes the time the user takes to peruse hint before apply'ing it. But
	 * it's fine in batch, where times and timings matter, so all good.
	 * <p>
	 * I'm now public static in my own helper class coz I'm also called by
	 * {@link diuf.sudoku.solver.hinters.color.GEMHintMulti#applyImpl}.
	 *
	 * @param out
	 * @param hint the hint to print
	 */
	public static void hint(final PrintStream out, final AHint hint) {
		if (Log.MODE < Log.VERBOSE_2_MODE)
			return;
		if ( out==null || hint==null )
			return;
		final long now = System.nanoTime();
		out.format("%,15d\t%s\n", now-Run.time, hint.toFullString());
		Run.time = now;
	}

	public static void hintFull(final PrintStream out, final AHint hint
			, final Grid grid, final int numElims, long took) {
		if (Log.MODE < Log.VERBOSE_2_MODE)
			return;
		if ( out==null || hint==null || grid==null )
			return;
		// left justified to differentiate from puzzleNumber in the logFile
		out.format("%-5d", grid.hintNumber);
		// time between hints includes activate time et al
		out.format("\t%,15d", took);
		out.format("\t%2d", grid.countFilledCells());
		out.format("\t%4d", grid.countMaybes());
		out.format("\t%3d", numElims);
		out.format("\t%-30s", hint.hinter);
		// Kraken: squeeze hobiwans multiline format back onto one line
		if ( hint.isKraken() )
			out.format("\t%s", MyStrings.squeeze(hint.toFullString()));
		else
			out.format("\t%s", hint.toFullString());
		out.println();
	}

	public static void gridFull(final PrintStream s, final AHint hint
			, final Grid grid, final int numElims, final long took) {
		grid(s, grid);
		hintFull(s, hint, grid, numElims, took);
	}

	public static void html(final PrintStream out, final AHint hint) {
		hint(out, hint);
		out.println(hint.toFullString());
		out.println();
		out.println(hint.toHtml());
		out.println();
		out.println("--------------------------------------------------------------------------------");
		out.println();
	}

	private Print() { } // Never called

}
