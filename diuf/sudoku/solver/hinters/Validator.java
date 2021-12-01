/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Pots;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.hinters.als.Als;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import static diuf.sudoku.utils.Frmt.MINUS;
import static diuf.sudoku.utils.Frmt.NOT_EQUALS;
import static diuf.sudoku.utils.Frmt.PLUS;
import static diuf.sudoku.utils.Frmt.SP;
import diuf.sudoku.utils.Log;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Validator exposes static methods that validate a reds against the Sudoku
 * solution that is calculated ONCE using brute force. It's a way to debug a
 * hinter, by logging all invalid hints, to work-out what's causing them, to
 * make them go away. Validator should NOT be part of a production system, so
 * the release procedure checks that *_VALIDATES are all false.
 * <p>
 * Validator is a nasty hack. Using the solution to find the solution is crazy.
 * But thanks to Donald Knuth we can solve quickly, so we do so, to validate
 * stuff that troubles us at the moment; but ONLY those that trouble us, and
 * only at the moment. And then we check they are all false BEFORE we release!
 * Did I mention that you should check false when you release? You check that
 * *_VALIDATES are all false in a release, because relying on the solution to
 * find the solution is crazy! If you tell c__ts stuff three times they MIGHT
 * actually listen to you, or not. sigh.
 *
 * @author Keith Corlett 2020-07-14
 */
public class Validator {

	// My callers use the below switches to determine whether or not they call
	// the isValid method. Each user has a public static final (compile-time)
	// switch. It's done this way to make it easy to rip me out; and easy to
	// find any usages before each release, and switch them off.
	//
	// Validator is not kosha so should NOT be used in a release! It would be
	// best if Validator did not exist, but it does, but I am still determined
	// not to rely upon this crutch in production. Using brute-force (solution)
	// is poor form when calculating the solution. I would rather not solve a
	// puzzle than rely on Validator to solve it.
	//
	// What Validator is useful for is telling me where I stuffed-up; it logs
	// invalid hints, and suppresses apply, to reveal the pattern of bad hints,
	// so that hopefully I can fix them.

	/**
	 * Does XColoring use Validator?
	 */
	public static final boolean XCOLORING_VALIDATES = false; // @check false

	/**
	 * Does MedusaColoring use Validator?
	 */
	public static final boolean MEDUSA_COLORING_VALIDATES = false; // @check false

	/**
	 * Does GEM (GradedEquivalenceMarks) use Validator?
	 */
	public static final boolean GEM_VALIDATES = false; // @check false

	/**
	 * Does DeathBlossom use Validator?
	 */
	public static final boolean DEATH_BLOSSOM_VALIDATES = false; // @check false

	/**
	 * Do AlsWing and AlsChain use Validator?
	 */
	public static final boolean ALS_VALIDATES = false; // @check false

	/**
	 * Does ComplexFisherman (for Franken and Mutant only) use Validator?
	 */
	public static final boolean COMPLEX_FISHERMAN_VALIDATES = false; // @check false

	/**
	 * Does KrakenFisherman use Validator?
	 */
	public static final boolean KRAKEN_FISHERMAN_VALIDATES = false; // @check false

	/**
	 * Does AlignedExclusion use Validator?
	 */
	public static final boolean ALIGNED_EXCLUSION_VALIDATES = false; // @check false

	/**
	 * Does ChainerBase (for Nested hints only) use Validator?
	 */
	public static final boolean CHAINER_VALIDATES = false; // @check false

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// these fields are input for the valid method.
	/**
	 * report uses invalidities to output each distinct invalidity ONCE, and not
	 * every time time this hinter runs there-after. nb: reference-type is
	 * HashMap to use the putIfAbsent method which is not defined in the Map
	 * interface, because they hadn't thought of it yet.
	 */
	public static final HashMap<String, Object> INVALIDITIES = new HashMap<>(16);

	// THE values in the above HashMap which is used as a Set (as per HashSet)
	private static final Object PRESENT = new Object();

	/**
	 * The invalidity field is a "sneaky second output" of the isValid method
	 * which is printed by the report method. It contains a short summary of the
	 * invalid eliminations. I'm also public so that you can do whatever you
	 * like with me, if the report method does not suit your requirements.
	 */
	public static String invalidity;

	/**
	 * Returns: Are all the eliminations in redPots (ie in the hint) valid? If
	 * so return true, if something is wrong then return false. I'm using this
	 * method to find all instances of dodgy bloody hints, so that I can try to
	 * pin-down the commonality between them, so that I can work-out what's
	 * wrong with my bloody code, and bloody-well fix it. This whole class is
	 * NOT intended to ever be used in a production system. It's only a stop-
	 * gap measure for debugging during development.
	 * <p>
	 * NOTE: It's up to each of my callers to check it's *_VALIDATES setting
	 * BEFORE calling isValid. To use isValid in a new class just add a new
	 * static final boolean WHATEVER_VALIDATES setting to the Validator class.
	 * This just to keeps all my s__t together, so that it's easier to throw
	 * away the Validator if/when we're ever done with all these dodgy hints.
	 *
	 * @param grid that we're solving
	 * @param redPots the hints eliminations to be validated
	 * @return true if all eliminations are valid, else false (invalid hint).
	 */
	public static boolean isValid(Grid grid, Pots redPots) {
		// presume that the hint (ie redPots) is valid;
		final int[] solution = grid.getSolution();
		invalidity = EMPTY_STRING;
		Cell cell;
		for ( java.util.Map.Entry<Cell,Integer> e : redPots.entrySet() ) {
			// if red-cell-values contains the value of this cell in solution
			if ( (e.getValue() & VSHFT[solution[(cell=e.getKey()).i]]) != 0 ) {
				// note the leading space before first: odd but works ok
				invalidity += SP+cell.id+MINUS+solution[cell.i];
			}
		}
		return invalidity.isEmpty();
	}

	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean report(String reporterName, Grid grid, Iterable<Als> alss) {
		return report(reporterName, grid, diuf.sudoku.utils.Frmt.csvIt(alss));
	}

	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean report(String reporterName, Grid grid, String badness) {
		if ( INVALIDITIES.putIfAbsent(invalidity, PRESENT) == null ) {
			Log.teef("%s\n", grid);
			// NOTE: invalidity contains a leading space
			prevMessage = String.format("WARN: %s invalidity%s in %s\n", reporterName, invalidity, badness);
			Log.teeln(prevMessage);
			return true;
		}
		return false;
	}
	public static String prevMessage;

	public static void clear() {
		INVALIDITIES.clear();
	}

	public static boolean isValidSetPots(Grid grid, Pots setPots) {
		invalidity = EMPTY_STRING;
		final int[] solution = grid.getSolution();
		// note the leading space before the first invalidity
		for ( Entry<Cell,Integer> e : setPots.entrySet()) {
			final Cell cell = e.getKey();
			final Integer values = e.getValue();
			if (VSIZE[values] != 1) {
				invalidity += SP+cell.id+PLUS+Values.toString(values)+" is not one value!";
			} else if ( (values & VSHFT[solution[cell.i]]) == 0 ) {
				invalidity += SP+cell.id+PLUS+values.toString()+NOT_EQUALS+solution[cell.i];
			}
		}
		return invalidity.isEmpty();
	}

	public static boolean reportSetPots(String reporterName, Grid grid, String invalidity, String badness) {
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
