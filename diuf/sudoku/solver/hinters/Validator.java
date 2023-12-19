/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.MAYBES_STR;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.NOT_EQUALS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.Log;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Validator exposes static methods that validate a redPots against the Sudoku
 * solution that is calculated ONCE using BruteForce. It is a way to debug a
 * hinter, by logging all invalid hints, to work-out what is causing them, to
 * make them go away. Validator should NOT be part of a production system. The
 * release process checks that the ValidatingHintsAccumulator is disabled.
 * <p>
 * Validator is called via the ValidatingHintsAccumulator. To use me in a
 * hinter class, override isAVeryNaughtyBoy to return true.
 * <p>
 * Validator is a nasty hack. Using the solution to find the solution is crazy.
 * But thanks to Donald Knuth we can solve quickly, so we do so, to validate
 * stuff that troubles us at the moment; but ONLY those that trouble us, and
 * only at the moment. And then we check that the ValidatingHintsAccumulator
 * is disabled BEFORE we release! This is part of the normal release process.
 *
 * @author Keith Corlett 2020-07-14
 */
public class Validator {

	/**
	 * report uses invalidities to output each distinct invalidity ONCE, and not
	 * every time time this hinter runs there-after. nb: reference-type is
	 * HashMap to use the putIfAbsent method which is not defined in the Map
	 * interface, because they had not thought of it yet.
	 */
	public static final HashMap<String, Object> INVALIDITIES = new HashMap<>(16);

	// THE values in the above HashMap which is used as a Set (as per HashSet)
	private static final Object PRESENT = new Object();

	/**
	 * The invalidity field is a "sneaky second output" of the isValid method
	 * which is printed by the report method. It contains a short summary of the
	 * invalid eliminations. I am also public so that you can do whatever you
	 * like with me, if the report method does not suit your requirements.
	 */
	public static String invalidity;

	/**
	 * Returns: Are all redPots NOT the solution values. I use this method to
	 * find dodgy hints, to pin-down the commonality between them, to work-out
	 * what is wrong with my code, and fix it. This class is NOT intended for
	 * use in prod, only for debugging during development.
	 * <p>
	 * I am called through the ValidatingHintsAccumulator. To use me in a new
	 * class, you override isAVeryNaughtyBoy to return true.
	 *
	 * @param grid that we are solving
	 * @param redPots the hints eliminations to be validated
	 * @return true if all eliminations are valid, else false (invalid hint).
	 */
	public static boolean validOffs(final Grid grid, final Pots redPots) {
		// presume that the hint (ie redPots) is valid;
		final int[] solution = grid.solution();
		invalidity = EMPTY_STRING;
		redPots.entrySet().stream()
			// if red-values contains the value of this cell in solution
			.filter((e) -> (e.getValue() & VSHFT[solution[e.getKey()]]) > 0) // 9bits
			// note the leading space before first: odd but works ok
			// Q: how is it OK to += a String in a lambda expression? It is a
			// static reference, but it is NOT final or effectively final!
			// Apparently I STILL do not understand lambdas rules. Sigh.
			.forEachOrdered((e) -> invalidity+=SP+CELL_IDS[e.getKey()]+MINUS+solution[e.getKey()]);
		return invalidity.isEmpty();
	}

	// for use when hint is invalid: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean reportInvalidHint(final String reporterName, final Grid grid, final String badness) {
		if ( INVALIDITIES.putIfAbsent(invalidity, PRESENT) == null ) {
			Log.teef("%s\n", grid.hintSource()); // hintNumber/[lineNumber#]filePath
			Log.teef("%s\n", grid);
			// NOTE: invalidity contains a leading space
			prevMessage = String.format("WARN: %s invalidity%s in %s\n", reporterName, invalidity, badness);
			Log.teeln(prevMessage);
			return true;
		}
		return false;
	}
	public static String prevMessage;

	public static void clearValidator() {
		INVALIDITIES.clear();
	}

	// does setPots contain only solution values.
	public static boolean validOns(final Grid grid, final Pots setPots) {
		invalidity = EMPTY_STRING;
		final int[] sol = grid.solution();
		// note the leading space before the first invalidity
		for ( Entry<Integer,Integer> e : setPots.entrySet()) {
			final int indice = e.getKey();
			final Integer values = e.getValue();
			if (VSIZE[values] != 1)
				invalidity += SP+CELL_IDS[indice]+PLUS+MAYBES_STR[values]+" is not one value!";
			else if ( (values & ~VSHFT[sol[indice]]) > 0 )
				invalidity += SP+CELL_IDS[indice]+PLUS+values.toString()+NOT_EQUALS+sol[indice];
		}
		return invalidity.isEmpty();
	}

	public static boolean reportSetPots(final String reporterName, final Grid grid, final String invalidity, final String badness) {
		// always set the lastMessage
		// NOTE: the invalidity String contains a leading space
		prevMessage = String.format("WARN: %s invalidity%s in %s\n", reporterName, invalidity, badness);
		// supress duplicate messages, ie report each invalidity once only
		if (INVALIDITIES.putIfAbsent(invalidity, PRESENT) == null) {
			Log.teef("%s\n", grid);
			Log.teef(prevMessage);
			return true;
		}
		return false;
	}

}
