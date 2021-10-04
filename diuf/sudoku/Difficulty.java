/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku;

import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.COLON_SP;
import static diuf.sudoku.utils.Frmt.COMMA_SP;
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
 *  <li>{@link diuf.sudoku.solver.LogicalSolver#LogicalSolver} to keep the
 *   order of wantedHinters near to that of Tech difficulty; so hinters run in
 *   increasing difficulty, to produce the simplest solution to each puzzle.
 *  <li>{@link diuf.sudoku.Tech} for the actual difficulties
 *  <li>{@link diuf.sudoku.Difficulty} double check the ranges
 * </ul>
 * @author Keith Corlett 2018 Mar - was an inner class of GenerateDialog
 */
public enum Difficulty {

	//Name		(index, minDifficulty, maxDifficulty)
	  Easy		(0.0,  1.5, "Testing 123")
	, Medium	(1.5,  2.5, "L Plates")
	, Hard		(2.5,  3.0, "P Plates")
	, Fiendish	(3.0,  4.0, "Car Licence")
	, Nightmare	(4.0,  6.0, "Art Licence")
	, Diabolical(6.0,  9.0, "Bus Licence")
	, IDKFA		(9.0,100.0, "FTL Licence")
	;

	// Weird: Difficulty.values() reversed.
	public static Difficulty[] reverseValues() {
		final Difficulty[] values = values();
		final int n = values.length;
		final Difficulty[] result = new Difficulty[n];
		for ( int i=0,m=n-1; i<n; ++i )
			result[i] = values[m-i];
		return result;
	}

	// get a list of the Techs of this difficulty
	public Tech[] techs() {
		return Tech.array(techs(min, max));
	}

	// get a list of Tech's from min (inclusive) to less than max (exclusive)
	public static List<Tech> techs(double min, double max) {
		final List<Tech> results = new LinkedList<>();
		for ( Tech tech : Tech.values() ) {
			if ( tech.difficulty >= min
			  && tech.difficulty < max
			  && !tech.name().startsWith("Direct")
			) {
				results.add(tech);
			}
		}
		return results;
	}

	// return a String listing the names of all the given Techs
	public static String names(List<Tech> techs) {
		final StringBuilder sb = new StringBuilder(techs.size()*22);
		for ( Tech tech : techs ) {
			if ( sb.length() > 0 )
				sb.append(COMMA_SP);
			sb.append(tech.name());
		}
		return sb.toString();
	}

	public final double min; // minimum difficulty
	public final double max; // maximum difficulty
	public final String html; // html describing this Difficulty in the GUI
	private Difficulty(double min, double max, String licence) {
		this.min = min;
		this.max = max;
		this.html = html(names(techs(min, max)), licence);
	}
	public String html(String description, String licence) {
		final String NL = System.lineSeparator();
		return "<html><body>"+NL
			+"<b>"+ordinal()+COLON_SP+name()+"</b> "+description+NL
			+"<p><b>Rating</b>: "+Frmt.frmtDbl(min)+" - "+Frmt.frmtDbl(max)+" ["+licence+"]"+NL
			+"</body></html>"+NL;
	}

}
