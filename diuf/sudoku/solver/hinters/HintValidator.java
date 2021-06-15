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
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.LogicalSolverFactory;
import diuf.sudoku.solver.hinters.als.Als;
import diuf.sudoku.utils.Log;
import java.util.HashMap;

/**
 * HintValidator exposes static helper methods to validate a hint against the
 * Sudoku solution calculated (once) with brute force. It's a way to debug a
 * hinter, by logging all invalid hints, to work-out what's causing them, to
 * make them go away. HintValidator should NOT be part of a production system,
 * so make sure you've turned me off in each release. It's part of the release
 * procedure: Check they're all false!
 * <p>
 * All of this class is a nasty hack. It makes no sense to rely on the solution
 * to the puzzle in order to solve the puzzle. BUT because we can solve quickly
 * (thanks to Donald Knuth), we do so, to validate stuff that troubles us at
 * the moment; but ONLY those that trouble us at the moment. And then we check
 * they're all false BEFORE we release! Did I mention that you should check
 * false when you release? Well, you should check false when you release.
 *
 * @author Keith Corlett 2020-07-14
 */
public class HintValidator {

	// My callers use the below switches to determine whether or not they call
	// the isValid method. Each user has a public static final (compile-time)
	// switch. It's done this way to make it easy to rip me out; and easy to
	// find any usages before each release, and switch them off.
	//
	// HintValidator is not kosha so should NOT be used in a release! It would
	// be best if HintValidator did not exist, but it does, but I am still
	// determined not to rely upon this crutch in a release. It is poor form
	// to use brute-force (the solution) to calculate the solution. I would
	// rather not solve the puzzle than rely on HintValidator to solve it.
	//
	// What HintValidator is useful for is telling me where I stuffed-up; it
	// can log all invalid hints, and not apply them, to reveal the pattern of
	// buggered hints, so that hopefully I can see it, and disembuggernate it.

	/**
	 * Does HdkColoring use HintValidator.isValid?
	 */
	public static final boolean COLORING_USES = false; // @check false

	/**
	 * Do AlsXyWing and AlsXyChain use HintValidator?
	 */
	public static final boolean ALS_USES = false; // @check false

	/**
	 * Does DeathBlossom use HintValidator?
	 */
	public static final boolean DEATH_BLOSSOM_USES = false; // @check false

	/**
	 * Does AlignedExclusion use HintValidator?
	 */
	public static final boolean ALIGNED_EXCLUSION_USES = false; // @check false

	/**
	 * Does ComplexFisherman (for Franken and Mutant only) use HintValidator?
	 */
	public static final boolean COMPLEX_FISHERMAN_USES = false; // @check false

	/**
	 * Does KrakenFisherman use HintValidator?
	 */
	public static final boolean KRAKEN_FISHERMAN_USES = false; // @check false

	/**
	 * Does AChainer (for Nested hints only) use HintValidator?
	 */
	public static final boolean CHAINER_USES = false; // @check false

	/**
	 * Does XColoring use HintValidator?
	 */
	public static final boolean XCOLORING_USES = false; // @check false

	/**
	 * Does MedusaColoring use HintValidator?
	 */
	public static final boolean MEDUSA_COLORING_USES = false; // @check false

	/**
	 * Does GEM (GradedEquivalenceMarks) use HintValidator?
	 */
	public static final boolean GEM_USES = false; // @check false

	/**
	 * Does any class use HintValidator?
	 */
	public static final boolean ANY_USES = false
			| COLORING_USES
			| ALS_USES
			| DEATH_BLOSSOM_USES
			| ALIGNED_EXCLUSION_USES
			| COMPLEX_FISHERMAN_USES
			| KRAKEN_FISHERMAN_USES
			| CHAINER_USES
			| XCOLORING_USES
			| MEDUSA_COLORING_USES
			| GEM_USES
			;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// these fields are input for the valid method.

	/**
	 * YOU need to set the solution values once for each grid. logicalSolvers
	 * prepare method is the perfect place, and that is the were I do it.
	 * <p>
	 * Used by isValid and possibly report methods.
	 */
	public static int[] solutionValues;

	/**
	 * YOU need to set the solutionValues OR the logicalSolver (or both) so
	 * that I can get the solutionValues for myself; if I need solution values
	 * and the logicalSolver field is null then I throw NullPointerException.
	 */
	public static LogicalSolver logicalSolver;

	/**
	 * report uses invalidities to output each distinct invalidity
	 * ONCE, and not every time time this hinter runs there-after.
	 * nb: reference-type is HashMap to use the putIfAbsent method which is
	 * not defined in the Map interface, because they hadn't thought of it yet.
	 */
	public static final HashMap<String, Object> invalidities = new HashMap<>(16);

	// THE values in the above HashMap which is used as a Set (as per HashSet)
	private static final Object PRESENT = new Object();

	/**
	 * The invalidity field is a "sneaky second output" of the isValid method
	 * which is printed by the report method. It contains a short
	 * summary of the invalid eliminations. I'm also public so that you can
	 * do whatever you like with me, if the report method does not
	 * suit your requirements.
	 */
	public static String invalidity;

	/**
	 * LogicalSolver.prepare sets solutionValues and solutionValuesGridId;
	 * and then "Plan B" is isValid calls checkSolutionValues which sets the
	 * solutionValues only if they're missing (which they should not be) or
	 * or the solutionValuesPuzzleId has changed (which it should not have),
	 * so when isValid calls checkSolutionValues it should never DO anything,
	 * it is really only there as a keep-sake, a "Plan B" if you will.
	 * <p>
	 * The GUIs degrelease mode also sets solutionValues.
	 *
	 * @param solutionValues
	 * @param puzzleID
	 */
	public static void setSolutionValues(int[] solutionValues, long puzzleID) {
		HintValidator.solutionValues = solutionValues;
		HintValidator.solutionValuesPuzzleId = puzzleID;
	}

	/**
	 * Returns: Are all the eliminations in redPots (ie in the hint) valid? If
	 * so return true, if something is wrong then return false. I'm using this
	 * method to find all instances of dodgy bloody hints, so that I can try to
	 * pin-down the commonality between them, so that I can work-out what's
	 * wrong with my bloody code, and bloody-well fix it. This whole class is
	 * NOT intended to ever be used in a production system. It's only a stop-
	 * gap measure for debugging during development.
	 * <p>
	 * NOTE: It's up to each of my callers to check it's *_USES setting BEFORE
	 * calling isValid. To use isValid in a new class just add a new *_USES
	 * static final boolean setting to this HintValidator class, and add that
	 * to the ANY_USES calculation. This just to keeps all my s__t together,
	 * so it's easier to throw me away if/when we're ever finished with all
	 * these dodgy bloody hints.
	 *
	 * @param grid that we're solving
	 * @param redPots the hints eliminations to be validated
	 * @return true if all eliminations are valid, else false (invalid hint).
	 */
	public static boolean isValid(Grid grid, Pots redPots) {
		// presume that the hint (ie redPots) is valid;
		checkSolutionValues(grid);
		invalidity = "";
		for ( Cell cell : redPots.keySet() ) {
			if ( redPots.get(cell).contains(solutionValues[cell.i]) )
				// note the leading space on the first
				invalidity += " "+cell.id+"-"+solutionValues[cell.i];
		}
		return invalidity.isEmpty();
	}

	/**
	 * Checks that solutionValues are set and are for the given Grid, if not
	 * then the logicalSolver field must be preset because I use it to solve
	 * the given grid and set the solutionValues and solutionValuesGridId.
	 *
	 * @param grid
	 */
	public static void checkSolutionValues(Grid grid) {
		if ( grid.puzzleID == 0L ) // WTF: How did that happen?
			grid.puzzleID = Grid.RANDOM.nextLong();
		// If solutionValues is unset || this is a different puzzle
		if ( solutionValues==null || grid.puzzleID!=solutionValuesPuzzleId ) {
			if ( logicalSolver == null ) // WTF: How did that happen?
				logicalSolver = LogicalSolverFactory.get();
			logicalSolver.solveQuicklyAndQuietly(grid); // sets grid.solutionValues
			setSolutionValues(grid.solutionValues, grid.puzzleID);
		}
	}
	// the ID of the puzzle which provided my solutionValues.
	private static long solutionValuesPuzzleId;

	public static int[] getSolutionValues(Grid grid) {
		checkSolutionValues(grid);
		return solutionValues;
	}

	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean report(String reporterName, Grid grid, Iterable<Als> alss) {
		return report(reporterName, grid, diuf.sudoku.utils.Frmt.csvIt(alss));
	}
	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean report(String reporterName, Grid grid, String badness) {
		if ( invalidities.putIfAbsent(invalidity, PRESENT) == null ) {
			Log.teef("%s\n", grid);
			// NOTE: invalidity contains a leading space
			Log.teef(lastMessage=String.format("WARN: %s invalidity%s in %s\n", reporterName, invalidity, badness));
			return true;
		}
		return false;
	}
	public static String lastMessage;

	public static void clear() {
		invalidities.clear();
	}

	public static boolean isValidSetPots(Grid grid, Pots setPots) {
		checkSolutionValues(grid);
		invalidity = "";
		// note the leading space before the first invalidity
		for ( Cell cell : setPots.keySet() ) {
			Values values = setPots.get(cell);
			if ( values.size != 1 )
				invalidity += " "+cell.id+"+"+values.toString()+" is not one value!";
			else if ( !values.contains(solutionValues[cell.i]) )
				invalidity += " "+cell.id+"+"+values.toString()+"!="+solutionValues[cell.i];
		}
		return invalidity.isEmpty();
	}

	public static boolean reportSetPots(String reporterName, Grid grid, String invalidity, String badness) {
		// always set the lastMessage
		// NOTE: the invalidity String contains a leading space
		lastMessage = String.format("WARN: %s invalidity%s in %s\n", reporterName, invalidity, badness);
		// supress duplicate messages, ie report each invalidity once only
		if ( invalidities.putIfAbsent(invalidity, PRESENT) == null ) {
			Log.teef("%s\n", grid);
			Log.teef(lastMessage);
			return true;
		}
		return false;
	}

}
