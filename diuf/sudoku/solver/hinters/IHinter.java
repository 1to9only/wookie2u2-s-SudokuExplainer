/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Comparator;


/**
 * I've abstracted the essentials of AHinter out to an interface IHinter, to
 * remove the dependency on AHinter, so that I no-longer know-or-care exactly
 * what the implementation is: as long as it implements IHinter it's a hinter.
 * <p>
 * All hinters directly or indirectly implement IHinter, which primarily
 * specifies the getHints method, plus some ancillary methods.
 * <p>
 * AHinter (the abstract hinter) now implements IHinter directly. As far as I
 * know, ALL hinters eventually extend AHinter, but this is NOT mandatory.
 * <p>
 * All LogicalSolver's collections and arrays are now of IHinter.
 *
 * @author Keith Corlett 2019 OCT
 */
public interface IHinter {

	/**
	 * Order by index in the
	 * {@link diuf.sudoku.solver.LogicalSolver#wantedHinters}
	 * array, ergo execution order.
	 * <p>
	 * Used by the UsageMap: a TreeMap.
	 */
	public static final Comparator<IHinter> BY_EXECUTION_ORDER
			= (IHinter a, IHinter b) -> a.getIndex() - b.getIndex(); // ASC

	/** just shorthand to make code fit on one line. */
	public final boolean T=true, F=false, DUMMY=false;

	/**
	 * findHints searches the given Grid for hints which it adds to the given
	 * IAccumulator. Each implementation of IHinter implements a specific
	 * Sudoku solving technique (a Tech). All current hinters (eventually)
	 * extend AHinter, but this is not mandatory. Any class that implements
	 * IHinter is a hinter, regardless of what (if anything) it extends.
	 * <p>
	 * So findHints searches the passed grid using a Sudoku Solving Technique.
	 * Each IHinter-instance implements exactly ONE Tech. All hinters add any
	 * hints to the passed IAccumulator. All hinters always return "were any
	 * hints found".
	 * <p>
	 * NOTE: {@link diuf.sudoku.solver.LogicalSolver#wantedHinters} is ordered
	 * (more or less) by complexity ASCENDING, so the simplest available hint
	 * is always found and applied at each step. Some hinters run BEFORE there
	 * Difficulty rating demands, because there implementation is actually
	 * faster than the existing implementation of "easier" hinters (not ideal).
	 * <p>
	 * NOTE: {@link IAccumulator#add} returns true only if hinters should stop
	 * searching immediately and return, so that this hint can be applied to
	 * the grid. LogicalSolver then starts-over from the first wantedHinter, to
	 * find the simplest possible hint at each step, and thereby find the
	 * simplest possible solution to the whole Sudoku puzzle. This probably
	 * won't be the shortest-simplest solution, just the simplest. Finding the
	 * simplest and shortest possible solution is a whole can of worms that I
	 * have thus-far resolutely refused to open. <br>
	 * Alternately if the given {@link IAccumulator#add} returns false then the
	 * AHinter continues searching the grid, adding any subsequent hints to the
	 * accumulator; eventually returning "Were any hints found?" The GUI goes
	 * this way when you Shift-F5 to find MORE hints.
	 * <p>
	 * NOTE: A hinter can declare itself to be an IPrepare, in which case it's
	 * prepare method is called after the puzzle is loaded into the Grid, but
	 * BEFORE we attempt to solve the puzzle, to give each hinter a chance to
	 * set-up for each puzzle.
	 * <p>
	 * NOTE: A hinter can also independently declare itself to be an ICleanUp,
	 * in which case it's cleanUp method is called AFTER each puzzle is solved
	 * (actually cleanUp is now called only before the GC is forced, but GC may
	 * never be forced: differing substantially from my initial design), to
	 * give each hinter a chance to clean-up it's own crap.
	 *
	 * @param grid containing the Sudoku puzzle to search for hints.
	 * @param accu the {@code IAccumulator} to which hinters add hints. <br>
	 *  If the add method returns true then hinters should immediately return
	 *  true. <br>
	 *  If the add methods returns false then hinters should keep searching to
	 *  find all hints available in the current grid
	 * @return were any hint/s found.
	 */
	public boolean findHints(Grid grid, IAccumulator accu);

	public Tech getTech(); // the Tech[nique] that I implement

	public double getDifficulty(); // the min-difficulty of hints produced

	public boolean isEnabled(); // this this hinter enabled
	public void setIsEnabled(boolean isEnabled);

	public int getDegree(); // the size of the fish, et al

	public int getIndex(); // my index in the wantedHinters array
	public void setIndex(int i);

	/**
	 * Each IHinter MUST override toString() to produce the hinter-name.
	 * @return the name of this hinter, which is usually the tech.name(), but
	 *  Aligned*Exclusion return class-name so user sees "hacked" v "correct".
	 *  Other hinters might also get-creative in future. sigh.
	 */
	@Override
	public String toString();

}
