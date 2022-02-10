/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
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
	// USE STATIC PREDICATES: Put the VALIDATE_* flag in his own if-statement.
	// Don't trust the compiler to demangle a mixed-predicate. I've confirmed
	// all static predicate is faster, so I think he's evaluated at compile
	// time, where a mixed predicate is evaluated at runtime, even though it's
	// STATICALLY an oxymoron. For reasons I do NOT understand this seems to
	// make a big difference to runtime. I suspect it's one out all out, that
	// is, ALL compile-time switches are evalutated at runtime. Weird!
	//
	// Evalutated: Yep, I can't spell worth s__t! Precluding academia, unless
	// I roll my own slepp-checker. Sigh. Just shoot me.
	//
	// Validator is not kosha so should NOT be used in a release! It would be
	// best if Validator did not exist, but it does, but I am still determined
	// not to rely upon this crutch in production. Using brute-force (solution)
	// is poor form when calculating the solution. I would rather not solve a
	// puzzle than rely on Validator to solve it.
	//
	// What Validator is for is to reveal a pattern in bad hints, to find the
	// bug in the hinter (using the debugger on a dodgy case), and fix it.

	/**
	 * Does XColoring use Validator?
	 */
	public static final boolean VALIDATE_XCOLORING = false; // @check false

	/**
	 * Does Medusa3D use Validator?
	 */
	public static final boolean VALIDATE_MEDUSA = false; // @check false

	/**
	 * Does GEM (GradedEquivalenceMarks) use Validator?
	 */
	public static final boolean VALIDATE_GEM = false; // @check false

	/**
	 * Does DeathBlossom use Validator?
	 */
	public static final boolean VALIDATE_DEATH_BLOSSOM = false; // @check false

	/**
	 * Do AlsXz, AlsWing and AlsChain use Validator?
	 */
	public static final boolean VALIDATE_ALS = false; // @check false

	/**
	 * Does ComplexFisherman (for Franken and Mutant only) use Validator?
	 */
	public static final boolean VALIDATE_COMPLEX_FISHERMAN = false; // @check false

	/**
	 * Does KrakenFisherman use Validator?
	 */
	public static final boolean VALIDATE_KRAKEN_FISHERMAN = false; // @check false

	/**
	 * Does AlignedExclusion use Validator?
	 */
	public static final boolean VALIDATE_ALIGNED_EXCLUSION = false; // @check false

	/**
	 * Does ChainerBase (for Nested hints only) use Validator?
	 */
	public static final boolean VALIDATE_CHAINER = false; // @check false

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
	 * Returns: Are all redPots NOT the solution values. I use this method to
	 * find dodgy hints, to pin-down the commonality between them, to work-out
	 * what's wrong with my code, and fix it. This class is NOT intended for
	 * use in prod, only for debugging during development.
	 * <p>
	 * It's up to each of my callers to check it's VALIDATE_* setting BEFORE
	 * calling me. To use me in a new class, you add to the Validator:
	 * static final boolean VALIDATE_${CLASS_NAME} = true;
	 * To keep my s__t together, to prevent Validator use in production.
	 * See usages of any of the existing flags to see how it's done.
	 *
	 * @param grid that we're solving
	 * @param redPots the hints eliminations to be validated
	 * @return true if all eliminations are valid, else false (invalid hint).
	 */
	public static boolean validOffs(final Grid grid, final Pots redPots) {
		// presume that the hint (ie redPots) is valid;
		final int[] solution = grid.getSolution();
		invalidity = EMPTY_STRING;
		redPots.entrySet().stream()
			// if red-values contains the value of this cell in solution
			.filter((e) -> (e.getValue() & VSHFT[solution[e.getKey().i]]) != 0)
			// note the leading space before first: odd but works ok
			// Q: how is it OK to += a String in a lambda expression? It's a
			// static reference, but it's NOT final or effectively final!
			// Apparently I STILL don't understand lambda's rules. Sigh.
			.forEachOrdered((e) -> invalidity+=SP+e.getKey().id+MINUS+solution[e.getKey().i]);
		return invalidity.isEmpty();
	}

	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean reportRedPots(final String reporterName, final Grid grid, final Iterable<Als> alss) {
		return reportRedPots(reporterName, grid, diuf.sudoku.utils.Frmt.csvIt(alss));
	}

	// for use when invalid returns true: log s__t in a standard way.
	// return was it reported, or swallowed as a repeat?
	public static boolean reportRedPots(final String reporterName, final Grid grid, final String badness) {
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

	public static void clearValidator() {
		INVALIDITIES.clear();
	}

	// does setPots contain only solution values.
	public static boolean validOns(final Grid grid, final Pots setPots) {
		invalidity = EMPTY_STRING;
		final int[] solution = grid.getSolution();
		// note the leading space before the first invalidity
		for ( Entry<Cell,Integer> e : setPots.entrySet()) {
			final Cell cell = e.getKey();
			final Integer values = e.getValue();
			if (VSIZE[values] != 1)
				invalidity += SP+cell.id+PLUS+Values.toString(values)+" is not one value!";
			else if ( (values & VSHFT[solution[cell.i]]) == 0 )
				invalidity += SP+cell.id+PLUS+values.toString()+NOT_EQUALS+solution[cell.i];
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
