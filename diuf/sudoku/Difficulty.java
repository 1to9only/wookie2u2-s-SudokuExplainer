/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.utils.Html;
import diuf.sudoku.utils.ResourceException;


/**
 * The Difficulty enum.
 * <p>
 * The Difficulty of a Sudoku Puzzle is closely aligned with the difficulty
 * field of the Tech(nique) enum, but there's also a smattering of "custom"
 * {@code getDifficulty()} methods in various {@code *Hint} classes.
 * <p>
 * The concept of Difficulty categories (ie this enum) is only used when
 * generating new Sudoku puzzles in the {@link diuf.sudoku.gui.GenerateDialog}.
 * The rest of the code-base seems to be getting along fine without it, and I
 * for one think it'd be a good idea if it stayed that way, considering how
 * much of a pain-in-the-ass maintaining it's turned out to be.
 * <p>
 * The only exception to this rule is in {@code AggregatedChainingHint}, which
 * references {@code Difficulty.IDKFA.min} to give up looking for a harder hint.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then please look at:
 * <ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#configureHinters} to keep the
 *   order of wantedHinters same as (or near) that of Tech difficulty; so that
 *   hinters are run in increasing difficulty, to produce the simplest solution
 *   for each puzzle.
 *  <li>{@link diuf.sudoku.Tech} // for the actual difficulties
 *  <li>{@link diuf.sudoku.Difficulty} // double check the ranges
 * </ul>
 * @author Keith Corlett 2018 Mar - was an inner class of GenerateDialog
 */
public enum Difficulty {

	//Name		(index, minDifficulty, maxDifficulty)
	  Easy		(0, 0.000000001, 1.2)
	, Medium	(1, 1.3, 2.6)
	, Hard		(2, 2.7, 3.5)
	, Fiendish	(3, 3.6, 4.2)
	, Nightmare	(4, 4.3, 5.9)
	, Diabolical(5, 6.0, 8.9)
	, IDKFA		(6, 9.0, 6969.42) // (a BIG value, actually it's unbounded)
	;

	public final int index; // a plain ordinal
	public final double min; // minimum difficulty
	public final double max; // maximum difficulty
	private String html; // html loaded from file: "Difficulty.$index.html"
	private Difficulty(int index, double min, double max) {
		this.index = index;
		this.min = min;
		this.max = max;
	}
	public String html() {
		  try {
			  if ( html == null ) {
				  String filename = "Difficulty."+this.index+".html";
				  html = Html.actuallyLoad(this, filename, null);
			  }
			  return html;
		  } catch (ResourceException ex) {
			  return ex.toString(); // should never happen.
		  }
	}

}
