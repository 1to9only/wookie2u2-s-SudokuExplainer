/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.ExplodingHintsAccumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.ValidatingHintsAccumulator;
import java.util.Comparator;

/**
 * I have abstracted the essentials of AHinter out to an interface IHinter, to
 * remove the dependency on AHinter, so that I no-longer know-or-care exactly
 * what the implementation is: as long as it implements IHinter its a hinter;
 * however in reality s__t is still glommed-onto AHinter, da original reference
 * type of hinters. Sigh.
 * <p>
 * All hinters directly or indirectly implement IHinter, which primarily
 * specifies the getHints method, plus some ancillary methods.
 * <p>
 * AHinter (the abstract hinter) now implements IHinter directly. As far as I
 * know, ALL hinters eventually extend AHinter, but this is NOT mandatory.
 * <p>
 * All LogicalSolvers collections and arrays are now of IHinter.
 *
 * @author Keith Corlett 2019 OCT
 */
public interface IHinter {

	/**
	 * Order by {@link diuf.sudoku.solver.LogicalSolver#wantedHinters} List,
	 * ergo the order in which hinters are executed, which is determined by
	 * the order that hinters are added to wantedHinters List in the
	 * LogicalSolverBuilder#build method.
	 * <p>
	 * Used by the {@link diuf.sudoku.solver.UsageMap} TreeMap.
	 */
	public static final Comparator<IHinter> BY_EXECUTION_ORDER
			= (IHinter a, IHinter b) -> a.getArrayIndex() - b.getArrayIndex(); // ASC

	/** just shorthand to make code fit on one line. */
	public final boolean T=true, F=false, DUMMY=false;

	/**
	 * Implementations of findHints search the given Grid for hints that are
	 * added to the given IAccumulator.
	 * <p>
	 * Each implementation of IHinter implements a specific Sudoku solving
	 * technique (a Tech). All current hinters (eventually) extend AHinter, but
	 * this is not mandatory (in theory). Any class that implements IHinter is
	 * a hinter, regardless of what (if anything) it extends. In practice parts
	 * of SEs code still presume that every IHinter is an AHinter, so trying it
	 * without extending AHinter is "brave".
	 * <p>
	 * So findHints searches the passed grid using a Sudoku Solving Technique.
	 * Each implementation class implements one or more Tech. All hinters add
	 * any hints (eventually) to the passed IAccumulator. The return value from
	 * {@link IAccumulator#add} indicates if the hinter should stop searching
	 * immediately, and return its positive result. Every findHints method MUST
	 * return "were any hint/s found".
	 * <p>
	 * NOTE: {@link diuf.sudoku.solver.LogicalSolver#wantedHinters} is ordered
	 * (more or less) by complexity ASCENDING, so the simplest available hint
	 * is always found and applied at each step. Some hinters run BEFORE there
	 * Difficulty rating demands, because there implementation is actually
	 * faster than the existing implementation of "easier" hinters (not ideal).
	 * Many of these hinters had there {@link IHinter#getDifficulty()} tweaked
	 * to match the speed of my implementation thereof, so the run order of
	 * hinters is actually determined by eliminations/nanosecond INCREASING.
	 * Difficulty is entirely notional!
	 * <p>
	 * NOTE: {@link IAccumulator#add} returns true if hinters should return
	 * immediately, so that this hint can be applied to the grid. LogicalSolver
	 * then starts again at the top of the wantedHinters list, to find the
	 * simplest possible hint at every step, thereby producing the simplest
	 * possible solution to the Sudoku puzzle.
	 * <p>
	 * This solution probably is NOT the shortest-simplest solution, just the
	 * simplest. Finding the simplest and shortest possible solution is a whole
	 * can of worms that I have thus-far refused to open.
	 * <p>
	 * Alternately if the given {@link IAccumulator#add} returns false then the
	 * AHinter continues searching the grid, adding any subsequent hints to the
	 * accumulator; eventually returning "Were any hints found?" The GUI goes
	 * this way when you Shift-F5 to find MORE hints.
	 * <p>
	 * NOTE: A hinter can declare itself to be IPrepare, in which case its
	 * prepare method is called after the puzzle is loaded into the Grid, and
	 * BEFORE we attempt to solve the puzzle, giving this hinter a chance to
	 * set-itself-up to solve this puzzle, or clear caches, or whatever.
	 * <p>
	 * NOTE: A hinter can also independently declare itself to be an ICleanUp,
	 * in which case its cleanUp method is called AFTER each puzzle is solved
	 * Actually cleanUp is now called only before the GC is forced, but GC may
	 * never be forced: differing substantially from my initial design, to give
	 * each hinter a chance to clean-up its crap.
	 *
	 * @param grid the Sudoku puzzle to search for hints.
	 * @param accu the {@code IAccumulator} to which hints are added. <br>
	 *  If the add method returns true then hinters immediately return true. <br>
	 *  If the add methods returns false then hinters keep searching to find
	 *  all hints available in the current grid, adding each in turn to the
	 *  IAccumulator
	 * @return were any hint/s found.
	 */
	public boolean findHints(Grid grid, IAccumulator accu);

	public Tech getTech(); // the Tech[nique] that I implement

	public int getDifficulty(); // the min-difficulty of hints produced

	public boolean isEnabled(); // is this hinter enabled
	public void setIsEnabled(boolean isEnabled);

	public int getDegree(); // the size of the fish, et al

	public int getArrayIndex(); // my index in the wantedHinters array
	public void setArrayIndex(int i);

	/**
	 * Each IHinter MUST override toString() to produce the hinter-name.
	 *
	 * @return the name of this hinter. Some hinters might get creative.
	 */
	@Override
	public String toString();

	/**
	 * LogicalSolver suppresses krakHeads in Generate when Tech.degree>=3
	 * because Krakens are too ____ing slow!
	 * <p>
	 * KrakenFisherman, and Superfischerman override.
	 *
	 * @return true if you produce KrakenFishHint (slowly)
	 */
	public default boolean isKrakHead() {
		return false;
	}

	public boolean isNesting();

	public String getTechName();

	/**
	 * ComplexFisherman and KrakenFisherman isAVeryVeryNaughtyBoy using
	 * ExplodingHintsAccumulator to stop the batch upon first invalid hint.
	 * <p>
	 * The ExplodingHintsAccumulator wraps the IAccumulator in LogicalSolver.
	 * <p>
	 * Any hinter requiring BATCH validation overrides isAVeryVeryNaughtyBoy to
	 * return true.
	 * <p>
	 * NOTE: AHint now implements getGrid, which is passed into every hint.
	 *
	 * @return false, unless you require stricter discipline.
	 */
	public default boolean isAVeryVeryNaughtyBoy() {
		return false;
	}

	/**
	 * Various fishermen isAVeryNaughtyBoy using ValdidatingHintsAccumulator.
	 * If hinter.isAVeryNaughtyBoy() then the LogicalSolver wraps the actual
	 * IAccumulator in a ValidatingHintsAccumulator, to mark and report invalid
	 * hints when they are added to the IAccumulator.
	 * <p>
	 * Any hinter requiring validation overrides isAVeryNaughtyBoy to return
	 * true.
	 * <p>
	 * NOTE: AHint now implements getGrid, which is passed into every hint.
	 *
	 * @return false, unless you require a spanking.
	 */
	public default boolean isAVeryNaughtyBoy() {
		return false;
	}

	/**
	 * Various hinters have internal caches, and I want a way to turn them all
	 * off in logFollow, to minimise lost-hints.
	 *
	 * @param useCache true to use the internal cache, false to suppress it
	 * @return the previous setting, to allow for reinstatement
	 */
	public default boolean setCaching(boolean useCache) {
		return false;
	}

	/**
	 * Reset whatever state this hinter holds, to bring it back to as if new.
	 */
	public default void reset() {
		// a no-op
	}

	/**
	 * Get the accumulator for this hinter.
	 *
	 * @return the accumulator for this hinter
	 */
	public IAccumulator getAccumulator(IAccumulator accu);

	/**
	 * Set the accumulator for this hinter.
	 *
	 * @param accu the normal accumulator
	 * @param eha ExplodingHintsAccumulator
	 * @param vha ValidatingHintsAccumulator
	 */
	public void setAccumulator(IAccumulator accu, ExplodingHintsAccumulator eha, ValidatingHintsAccumulator vha);

	/**
	 * Set the accumulator to null.
	 */
	public void clearAccumulator();

}
