/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Grid;
import diuf.sudoku.Tech;
import java.util.Comparator;
import diuf.sudoku.solver.accu.IAccumulator;
import java.util.Objects;


/**
 * AHinter (Abstract Hinter) is the base-type for all classes which produce
 * AHints (Abstract Hints) through the getHints method.
 *
 * @author Keith Corlett 2018 Jan - previously it was an ugly interface tree.
 */
public abstract class AHinter implements IHinter {

	/** System.lineSeparator() by any other name is a rose. */
	public static final String NL = diuf.sudoku.utils.Frmt.NL;

	/**
	 * Compares two IHinters by difficulty ASCENDING,
	 * and then by toString() ASCENDING.
	 * Used by the UsageMap: a TreeMap.
	 * You could also use me to sort the wantedHinters list!
	 */
	public static final Comparator<IHinter> BY_DIFFICULTY_ASC_TOSTRING_ASC
			= new Comparator<IHinter>() {
		@Override
		public int compare(IHinter a, IHinter b) {
			if ( Objects.equals(a, b) ) // Currently a==b. May be overridden in future
				return 0;
			double da = a.getDifficulty();
			double db = b.getDifficulty();
			if ( da < db )
				return -1;
			if ( da > db )
				return 1;
			// WARNING: toString can be "a bit slow Redge"
			// See: toString
			return a.toString().compareTo(b.toString());
		}
	};

	/** The Solving Technique which this {@code AHinter} implements. */
	public final Tech tech;

	/** Are we solving KRC's Sudoku puzzles: top1465.d5.mt. */
	public static boolean hackTop1465 = false;

	/** The degree (the number of cells, or Chainer level) of this AHinter.
	 * <p>For example: HiddenPairs=2, NakedTriples=3.
	 * <p><b>WARN:</b> Hackily overwritten in {@code UniqueRectangle}. */
	public final int degree;

	/** The degree + 1 */
	public final int degreePlus1;

	/** Is this hinter enabled? If {@code !getIsEnabled()} then getHints
	 * won't call this hinter. */
	public boolean isEnabled = true;

	/** Is this hinter active? If {@code !isActive()} then getHints
	 * won't call this hinter. Note that active is much finer grained (much more
	 * on/off) than enabled.
	 * <p>In order to actually be used a hinter must first be wanted by the
	 * user, then in -REDO mode only those that contributed to solving this
	 * puzzle in the "keeper" run are enabled ONCE at the start of the solve
	 * process. Only enabled hinters are considered for activation. Some
	 * hinters are de/activated by hintNumber. So the user does wanted, then
	 * we do enabled once at the top of each solve, and then we de/activate
	 * on the fly. Clear as bloody mud, right? */
	protected boolean isActive = true;

	/**
	 * Constructor.
	 * @param tech The {@code Tech} (SudokuSolvingTechnique) that you're
	 * implementing.
	 */
	public AHinter(Tech tech) {
		this.tech = tech;
		this.degree = tech.degree;
		this.degreePlus1 = degree + 1;
	}

	@Override
	public Tech getTech() {
		return tech;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	@Override
	public void setIsEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * All implementations are expected to be active by default. Note that the
	 * isActive field is protected, so that my subtypes can define there own
	 * setIsActive methodology, using whatever mechanism/logic suits them.
	 * <p>
	 * Note that implementors are not required to "action" isActive in there
	 * getHints method... that's done for you in the LogicalSolver, because I
	 * don't trust you/me/us/anyone to consistently get s__t like that right.
	 *
	 * @return is this hinter active, or are we having a little lie down?
	 */
	@Override
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Find hints in the grid using a Sudoku Solving Technique (ie each AHinter
	 * implements a Tech), and add them to the given IAccumulator.
	 * <p>
	 * Note that {@link IAccumulator#add} returns true only if we should stop
	 * the search immediately and return, so this hint can be applied to the
	 * grid; then the LogicalSolver starts-over from the top of the
	 * wantedHinters list to find the simplest possible hint at each stage, and
	 * thereby the simplest possible solution to the Sudoku. Note that this
	 * probably won't be the shortest possible solution, just the simplest
	 * possible. Finding the simplest and shortest possible solution is a whole
	 * can of worms that I have thus-far resolutely refused to open.
	 * <p>
	 * Note also that the LogicalSolvers wantedHinters list is sorted by
	 * complexity ASCENDING, so the simplest available hint is found and
	 * applied at each step.
	 * <p>
	 * Alternately if the given {@link IAccumulator#add} returns false then the
	 * AHinter continues searching the grid, adding any subsequent hints to the
	 * accumulator; eventually returning "Were any hints found?" The GUI goes
	 * this way when you Shift-F5 to find MORE hints.
	 *
	 * @param grid the Sudoku {@code Grid} to search.
	 * @param accu the {@code IAccumulator} to which I will add hints.
	 * @return true if this hinter found a/any hint/s.
	 */
	@Override
	abstract public boolean findHints(Grid grid, IAccumulator accu);

	/**
	 * @return the base difficulty of hints produced by this AHinter.
	 * <p>Only used by the {@code LogicalSolver.HinterComparator} to provide
	 * the order of the {@code UsagesMap} returned by the {@code solve} method.
	 * Good luck resolving the collisions inherent there-in.
	 *
	 * If your RFC says
	 *    "Can you put them in a reasonable order"
	 * then the reply is
	 *    "Yes, it's so easy why don't you do it?".
	 *
	 * The real answer is "Yes, but we'd have to blow difficulty up to 100-based
	 * to have a chance of doing so without ANY possible collisions; and we'd
	 * need to be much more miserly with the gaps on the down-low.
	 */
	@Override
	public double getDifficulty() {
		return tech.difficulty;
	}

	/**
	 * Exists to be overridden - called before the start of each processing
	 * of each puzzle.
	 */
	public void reset() {
	}

	/**
	 * This implementation just returns tech.nom (it's French for name, which
	 * is reserved by the EnumSet) of the Tech passed into my constructor; to
	 * tell you which Sudoku Solving Technique I implement.
	 * <p>
	 * <b>WARNING:</b> Performance! most implementations use this toString(),
	 * which just returns tech.nom, which is a compile time constant, and is
	 * almost always sufficient. A few override me, which is fine, but bewarned
	 * <b>NEVER</b> build a String in an override of my toString, it should
	 * return a compile-time constant String; or the bloody comparator bogs
	 * like a dead dogs bone. I've seen this crash a JVM, repeatably.
	 *
	 * @return a compile-time constant String that says which Sudoku Solving
	 * Technique this AHinter implements.
	 */
	@Override
	public String toString() {
		return tech.nom;
	}

	@Override
	public int getDegree() {
		return degree;
	}

}
