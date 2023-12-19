/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import diuf.sudoku.utils.Formatter;
import diuf.sudoku.utils.Formatter.FormatString;
import diuf.sudoku.utils.Log;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Static helper methods to format and print AHint, Grid, whatever.
 * <p>
 * Packaging: Not sure where I belong. I am not model, nor interface; more of
 * an indel: a bridge between an interface and the model; a models interface; a
 * thing that presents stuff. I will never really understand MVC. Many parts
 * don't really fit, so end-up in the controller, simply because it knows about
 * both the model and interface. I want an M-MV-V-VC-VM-C pattern. Too complex
 * to contemplate. Excessive abstraction is unhealthy. Learn to wank instead.
 * <p>
 * I am called by:<ul>
 * <li> {@link diuf.sudoku.gui.SudokuExplainer#applySelectedHints}
 * <li> {@link diuf.sudoku.solver.hinters.color.GEMHintMulti#applyImpl}
 * </ul>
 *
 * @author Keith Corlett 2021-03-30
 */
public class Print {

	public static final Formatter FORMATTER = new Formatter();
	public static final Locale LOCALE = Locale.getDefault();

	public static final String PUZZLE_SUMMARY_HEADERS = FORMATTER.format(
		  "\n%5s\t%17s\t%17s\t%5s\t%5s\t%5s\t%5s\t%7s\t%s\n"
		, "", "time(ns)", "average (ns)", "calls"
		, "hints", "elims", "maxD", "ttlDiff", "hinter").toString();

	private static final FormatString[] GRID_FSA = FORMATTER.parse(
		"\n%d/%s\n%s\n");

	public static void printGrid(final PrintStream out, final Grid grid) {
		out.print(FORMATTER.format(LOCALE, GRID_FSA, grid.hintNumber
				, grid.sourceShort(), grid.toString()));
	}

	/**
	 * Print the hint details; Usually print prev when the next hint is sought,
	 * then this is called when the last hint is applied, coz there is NO next.
	 * This is a by-product of pinning printed output to user action.
	 * Note: times slow in GUI coz it is time between hints being apply'd, which
	 * includes the time the user takes to peruse hint before apply'ing it. But
	 * it is fine in batch, where times and timings matter, so all good.
	 * <p>
	 * I am now public static in my own helper class coz I am also called by
	 * {@link diuf.sudoku.solver.hinters.color.GEMHintMulti#applyImpl}.
	 *
	 * @param out
	 * @param hint the hint to print
	 */
	public static void printHint(final PrintStream out, final AHint hint) {
		final long now = System.nanoTime();
		out.format("%,15d\t%s\n", now-Run.time, hint.toFullString());
		Run.time = now;
	}

	private static final FormatString[] HINT_FULL_FSA = FORMATTER.parse(
		"%-5d\t%,15d\t%2d\t%4d\t%3d\t%-30s\t%s\n");

	public static void printHintFull(final PrintStream out, final AHint hint
			, final Grid grid, final int numElims, long took) {
		// nb: I use my Format, which parses the format String once then
		// reuses the FormatString[] to print each hint, coz its faster.
		out.print(FORMATTER.format(LOCALE, HINT_FULL_FSA, grid.hintNumber
			, took, grid.numSet, grid.numMaybes, numElims, hint.hinter
			, hint.toFullString()));
	}

	public static void printGridFull(final PrintStream out, final AHint hint
			, final Grid grid, final int numElims, final long took) {
		printGrid(out, grid);
		if ( Log.LOG_MODE >= Log.VERBOSE_3_MODE )
			printHintFull(out, hint, grid, numElims, took);
	}

	public static void printHtml(final PrintStream out, final AHint hint) {
		if ( Log.LOG_MODE >= Log.VERBOSE_2_MODE )
			printHint(out, hint);
		out.println(hint.toFullString());
		out.println();
		out.println(hint.toHtml());
		out.println();
		out.println("--------------------------------------------------------------------------------");
		out.println();
	}

	private Print() { } // Never called

}
