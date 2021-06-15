/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.utils.Frmt;
import java.util.LinkedList;
import java.util.List;


/**
 * The Difficulty enum is used only to generate Sudokus.
 * <p>
 * The Difficulty of a Sudoku Puzzle is closely aligned with the difficulty
 * field of the Tech(nique) enum, and there's a smattering of "custom"
 * {@code getDifficulty()} methods in various {@code *Hint} classes.
 * <p>
 * Each Difficulty category (ie this enum) is used to select/reject a puzzle
 * when generating in the {@link diuf.sudoku.gui.GenerateDialog}. The rest of
 * the code-base seems to be getting along fine without it, and I think it'd be
 * good if things stayed that way, considering how much of a pain-in-the-ass
 * maintaining the Difficulty boundaries has turned out to be.
 * <p>
 * The only exception to the rule is {@code AggregatedChainingHint} uses
 * {@code Difficulty.IDKFA.min} to stop looking for a harder hint. Hence
 * Difficulty was exhumed to {@code diuf.sudoku} for public visibility.
 * Formerly Difficulty was an inner-class of GenerateDialog.
 * <p>
 * <b>WARNING:</b> If you change any hints difficulty then also look at:<ul>
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#configureHinters} to keep the
 *   order of wantedHinters same as (or near) the Tech difficulties; so that
 *   hinters are run in order of increasing difficulty, to produce the simplest
 *   possible solution to each Sudoku puzzle.
 *  <li>{@link diuf.sudoku.Tech} for the actual difficulties
 *  <li>{@link diuf.sudoku.Difficulty} double check the ranges
 * </ul>
 * @author Keith Corlett 2018 Mar - was an inner class of GenerateDialog
 */
public enum Difficulty {

	//Name		(index, minDifficulty, maxDifficulty)
	  Easy		(0, 0.0,  1.3, "Learners Test")
	, Medium	(1, 1.3,  2.7, "L Plates")
	, Hard		(2, 2.7,  3.6, "P Plates")
	, Fiendish	(3, 3.6,  4.3, "Open Licence")
	, Nightmare	(4, 4.3,  5.0, "Truck Licence")
	, Diabolical(5, 5.0,  9.0, "Bus licence")
	, IDKFA		(6, 9.0,100.0, "FTL Licence")
	;

	// get a list of the Techs of this difficulty
	public List<Tech> techs() {
		return techs(min, max);
	}

	public static List<Tech> techs(double min, double max) {
		List<Tech> results = new LinkedList<>();
		if ( min == 0.0 )
			min = 0.1;
		for ( Tech tech : Tech.values() ) {
			if ( tech.difficulty >= min
			  && tech.difficulty < max
			  && !tech.name().startsWith("Direct") ) {
				results.add(tech);
			}
		}
		return results;
	}

	// return a String listing the names of all the given Techs
	public static String names(List<Tech> techs) {
		final StringBuilder sb = new StringBuilder(512);
		for ( Tech tech : techs ) {
			if ( sb.length() > 0 )
				sb.append(", ");
			sb.append(tech.name());
		}
		return sb.toString();
	}

	public final int index; // a plain ordinal
	public final double min; // minimum difficulty
	public final double max; // maximum difficulty
	public final String html; // html describing this Difficulty in the GUI
	private Difficulty(int index, double min, double max, String licence) {
		this.index = index;
		this.min = min;
		this.max = max;
		this.html = html(names(techs(min, max)), licence);
	}
	public String html(String description, String licence) {
		final String NL = System.lineSeparator();
		return "<html><body>"+NL
			+"<b>"+index+" "+name()+"</b>: "+description+NL
			+"<p><b>Rating</b>: "+Frmt.dbl(min)+" - "+Frmt.dbl(max)+" ["+licence+"]"+NL
			+"</body></html>"+NL;
	}

}
