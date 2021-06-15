/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.gui;

import diuf.sudoku.Grid;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.GrabBag;
import diuf.sudoku.utils.MyStrings;
import java.io.PrintStream;

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

	public static PrintStream out = System.out;

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
	 * @param hint the hint to print
	 * @param source a String of the source grids "lineNumber#filepath"
	 * @param before a String of the grid BEFORE this hint is applied to it
	 */
	public static void hint(AHint hint, String source, String before) {
		final long now = System.nanoTime();
		if ( source==null || source.isEmpty() )
			source = "IGNOTO";
		out.format("\n%d/%s\n", AHint.hintNumber, source);
		if ( before != null )
			out.format("%s\n", before);
		out.format("%,15d\t%s\n", now-GrabBag.time, hint.toFullString());
		GrabBag.time = now;
	}

	/**
	 * Print the hint details, taking source from the grid.
	 *
	 * @param hint
	 * @param grid
	 * @param before 
	 */
	public static void hint(AHint hint, Grid grid, String before) {
		hint(hint, source(grid), before);
	}

	/**
	 * Get the grid.source.toString(); else null on any Exception.
	 * @param grid
	 * @return
	 */
	public static String source(Grid grid) {
		if ( grid!=null && grid.source!=null )
			try {
				return grid.source.toString();
			} catch (Exception ex) {
				// do nothing
			}
		return null;
	}

	public static void hint(PrintStream out, int hintCount, long took
			, Grid grid, int numElims, AHint hint, boolean wantBlankLine) {
		out.format("%-5d", hintCount); // left justified to differentiate from puzzleNumber in the logFile.
		out.format("\t%,15d", took); // time between hints includes activate time and rebuilding empty cell counts
		out.format("\t%2d", grid.countFilledCells());
		out.format("\t%4d", grid.countMaybes());
		out.format("\t%3d", numElims);
		out.format("\t%-30s", hint.hinter);
		// squeeze hobiwans multiline Kraken format back onto one line
		if ( hint.hinter.tech.name().startsWith("Kraken") )
			out.format("\t%s", MyStrings.squeeze(hint.toFullString()));
		else
			out.format("\t%s", hint.toFullString());
		out.println();
		if ( wantBlankLine )
			out.println();
	}
	
	private Print() { } // Never called

}
