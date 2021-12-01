/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.solver.InterruptException;
import diuf.sudoku.Grid;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.accu.IAccumulator;


/**
 * AHinter (the abstract Hinter) is the base-type for all classes whose
 * implementation of the getHints method produces AHint's (the abstract hint).
 * I supply default implementations of many ancillary methods, leaving getHints
 * upto my sub-classes. Each hinter extends AHinter (eventually) to implement
 * one-or-more Sudoku Solving Technique's, which is called a {@link Tech}.
 *
 * @author Keith Corlett 2018 Jan - previously it was an ugly interface tree.
 */
public abstract class AHinter implements IHinter {

	/** System.lineSeparator() by any other name is a rose. */
	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	/**
	 * The Sudoku Solving Technique that this {@code AHinter} implements.
	 * <p>
	 * Note that a hinter may implement several techniques, but it's presumed
	 * that each <u>instance</u> of that hinter implements a single Tech.
	 */
	public final Tech tech;

	/** Are we solving KRC's Sudoku puzzles: top1465.d5.mt. */
	public static boolean hackTop1465 = false;

	/**
	 * The degree is the "magnitude" of this hinter. That "magnitude" could be
	 * the number of cells in the set, like HiddenPair/Triple/Quad/Pent, or it
	 * could be the "insanity level" as in ChainerMulti. Tech's that are
	 * implemented in there own hinter have a degree of 0; hence hinters that
	 * implement one-only Tech end-up with a degree of 0.
	 * <p>
	 * <b>WARN:</b> Hackily overwritten in {@code UniqueRectangle}.
	 */
	public final int degree;

	/** The degree + 1 */
	public final int degreePlus1;

	/** The degree - 1 */
	public final int degreeMinus1;

	/**
	 * Is this hinter enabled?
	 * <pre>
	 * If {@code hinter.isEnabled()} then
	 *   {@link diuf.sudoku.solver.LogicalSolver#solve} (et al) calls {@code hinter.getHints}
	 * else
	 *   skip this <u>putz</u>!
	 * endif
	 * </pre>
	 */
	public boolean isEnabled = true;

	/**
	 * The index of this hinter in the wantedHinters array.
	 * <p>
	 * This rather weird field is used to order hinter-summary-lines in the
	 * LogicalSolverTester .log by numCalls and then by the order of hinters
	 * in the wantedHinters array, which is the order in which solve executes
	 * each hinter. Without this field we need to create an external map of
	 * the IHinter back to it's index, and a HashMap look-up is a bit slow
	 * Redge (It's under the bonnet son).
	 */
	public int index;

	/**
	 * Constructor.
	 * @param tech that this hinter implements.
	 */
	public AHinter(Tech tech) {
		this.tech = tech;
		this.degree = tech.degree;
		this.degreePlus1 = degree + 1;
		this.degreeMinus1 = degree - 1;
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

	@Override
	public int getIndex() {
		return index;
	}
	@Override
	public void setIndex(int i) {
		index = i;
	}

	protected final void interrupt() {
		if ( Run.stopGenerate ) {
			throw new InterruptException();
		}
	}

	/**
	 * {@inheritDoc}
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
	 * This implementation just returns the name() of the Tech passed into my
	 * constructor; to tell you which Sudoku Solving Technique I implement.
	 * <p>
	 * <b>WARNING:</b> Performance! most implementations use this toString(),
	 * which just returns tech.name() which is a compile time constant, and is
	 * almost always sufficient. Some may override me, which is fine, but be
	 * warned <b>NEVER</b> build a String in an override of my toString, it
	 * should return a compile-time constant String; or the bloody comparator
	 * bogs like a dead dogs bone. It may even crash the JVM.
	 *
	 * @return a compile-time constant String that says which Sudoku Solving
	 * Technique this AHinter implements.
	 */
	@Override
	public String toString() {
		return tech.name();
	}

	@Override
	public int getDegree() {
		return degree;
	}

}
