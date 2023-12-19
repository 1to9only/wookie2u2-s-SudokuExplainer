/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters;

import diuf.sudoku.Constants;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Box;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.Col;
import diuf.sudoku.Grid.Row;
import diuf.sudoku.Idx;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.HinterruptException;
import diuf.sudoku.solver.accu.ExplodingHintsAccumulator;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.accu.ValidatingHintsAccumulator;

/**
 * AHinter (the abstract Hinter) is the base-type for all classes whose
 * implementation of the getHints method produces AHints (the abstract hint).
 * I supply default implementations of many ancillary methods, leaving getHints
 * upto my sub-classes. Each hinter extends AHinter (eventually) to implement
 * one-or-more Sudoku Solving Techniques, which is called a {@link Tech}.
 *
 * @author Keith Corlett 2018 Jan - previously it was an ugly interface tree.
 */
public abstract class AHinter implements IHinter {

	/** System.lineSeparator() by any other name is a rose. */
	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	/** QEMPTY is returned by IntQueue.poll when the queue is empty. */
	protected static final int QEMPTY = diuf.sudoku.utils.IntQueue.QEMPTY;

	/**
	 * Returns a new StringBuilder of the given capacity.
	 *
	 * @param initialCapacity the starting size. It can grow from there
	 * @return a new StringBuilder(initialCapacity)
	 */
	protected static StringBuilder SB(final int initialCapacity) {
		return Constants.SB(initialCapacity); // a single definition
	}

	/**
	 * The Sudoku Solving Technique that this {@code AHinter} implements.
	 * <p>
	 * Note that a hinter may implement several techniques, but it is presumed
	 * that each <u>instance</u> of that hinter implements a single Tech.
	 */
	public final Tech tech;

	/**
	 * The degree is the magnitude of this hinter.
	 * <p>
	 * <pre>
	 * That magnitude could be:
	 * * number of cells in {@link diuf.sudoku.solver.hinters.naked.NakedSet} and {@link diuf.sudoku.solver.hinters.hidden.HiddenSet};
	 * * or number of bases/covers in various {@link diuf.sudoku.solver.hinters.fish.ComplexFisherman:fishermen};
	 * * or the "insanity level", as in {@link diuf.sudoku.solver.hinters.chain.ChainerMulti};
	 * * or number of links, as in {@link diuf.sudoku.solver.hinters.als.AlsChainFast}.
	 * Techs that are implemented in there own hinter have a degree of 0;
	 * ie hinters that implement one-only Tech (usually) have a degree of 0.
	 * </pre>
	 */
	public final int degree;

	/** The degree + 1 */
	public final int degreePlus1;

	/** The degree - 1 */
	public final int degreeMinus1;

	/** The degree&lt;&lt;1 */
	public final int degreeBy2;

	/** The isEnabled field is negated because I am tight-ass */
	private volatile boolean isDisabled;

	/**
	 * The index of this hinter in the wantedHinters array.
	 * <p>
	 * This rather weird field is used to order hinter-summary-lines in the
	 * LogicalSolverTester .log by numCalls and then by the order of hinters
	 * in the wantedHinters array, which is the order in which solve executes
	 * each hinter. Without this field we need to create an external map of
	 * the IHinter back to it is index, and a HashMap look-up is a bit slow
	 * Redge (It is under the bonnet son).
	 */
	public int arrayIndex;

	/**
	 * Constructor.
	 * @param tech that this hinter implements.
	 */
	public AHinter(Tech tech) {
		assert tech != null;
		this.tech = tech;
		this.degree = tech.degree;
		this.degreePlus1 = degree + 1;
		this.degreeMinus1 = degree - 1;
		this.degreeBy2 = degree<<1;
	}

	@Override
	public final Tech getTech() {
		return tech;
	}

	@Override
	public String getTechName() {
		return tech.name();
	}

	@Override
	public boolean isNesting() {
		return tech.isNesting;
	}

	/**
	 * Is this hinter enabled?
	 * <pre>{@code
	 * if ( hinter.isEnabled() )
	 *     hinter.getHints(...)
	 * }</pre>
	 * @return is this hint shop open for business?
	 */
	@Override
	public final boolean isEnabled() {
		return !isDisabled;
	}
	@Override
	public final void setIsEnabled(final boolean isEnabled) {
		this.isDisabled = !isEnabled;
	}

	@Override
	public final int getArrayIndex() {
		return arrayIndex;
	}
	@Override
	public final void setArrayIndex(final int i) {
		arrayIndex = i;
	}

	/**
	 * hinterrupt is called in hinters that run "for a while" to break-out-of
	 * the search, in order to stop the Generator fast enough to avoid giving
	 * the user the s__ts enough to press the bloody "Stop" button again, which
	 * will not work, in fact it just starts another one.
	 * <p>
	 * Why the ____ do we have a "Start/Stop" button? ____ing mental. A Start
	 * and a separate Stop button wont ____up when things go bad. Stop cant be
	 * accidentally double-pressed. We can just disable the Start button until
	 * it stops. Simples.
	 */
	protected final void hinterrupt() {
		if ( Run.isHinterrupted() )
			throw new HinterruptException();
	}

	protected Grid grid;
	protected Cell[] cells;
	protected int[] maybes;
	protected int[] sizes;
	protected ARegion[] regions;
	protected Box[] boxs;
	protected Row[] rows;
	protected Col[] cols;
	protected Idx[] idxs;

	protected IAccumulator accu;
	protected boolean oneHintOnly;

	public void setFields(final Grid grid) {
//System.out.println(grid.getClass().getName() + "@" + Integer.toHexString(grid.hashCode()));
		this.grid = grid;
		this.cells = grid.cells;
		this.maybes = grid.maybes;
		this.sizes = grid.sizes;
		this.regions = grid.regions;
		this.boxs = grid.boxs;
		this.rows = grid.rows;
		this.cols = grid.cols;
		this.idxs = grid.idxs;
	}

	public void clearFields() {
		this.grid = null;
		this.cells = null;
		this.maybes = null;
		this.sizes = null;
		this.regions = null;
		this.boxs = null;
		this.rows = null;
		this.cols = null;
		this.idxs = null;
	}

	/**
	 * All consumers call this findHints(Grid, IAccumulator) method, but my
	 * subclasses may override either this findHints(Grid, IAccumulator) or
	 * the findHints() implementation method to get all the protected fields:
	 * grid, cells, maybes, etc.
	 * <p>
	 * Fast/simple hinters override this findHints(Grid, IAccumulator) method.
	 * The slower/heavier hinters override findHints(), where the extra time
	 * spent setting all of these fields is an acceptably small proportion of
	 * the overall time. Ergo, fast hinters DIY, but the slow ones are lazy.
	 * <p>
	 * Fields go in here only if atleast three hinters want them.
	 *
	 * @param grid the Sudoku puzzle to search
	 * @param accu to which I add hints
	 * @return any hint/s found
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
//if ( this instanceof diuf.sudoku.solver.hinters.table.TableChains )
//	Debug.breakpoint();
		final boolean result;
		try {
			this.accu = accu;
			this.oneHintOnly = accu.isSingle();
			setFields(grid);
			result = findHints();
		} finally {
			clearFields();
			this.accu = null;
		}
		return result;
	}

	/**
	 * My many subclasses override this empty implementation to find any hints
	 * of there specific type in the grid field, adding them to accu.
	 *
	 * @return any hint/s found
	 */
	protected boolean findHints() {
		return false;
	}

	/**
	 * @return the base difficulty of hints produced by this AHinter.
	 * <p>Only used by the {@code LogicalSolver.HinterComparator} to provide
	 * the order of the {@code UsagesMap} returned by the {@code solve} method.
	 * Good luck resolving the collisions inherent there-in.
	 *
	 * If your RFC says
	 *    "Can you put them in a reasonable order"
	 * then the reply is
	 *    "Yes, it is so easy why do not you do it?".
	 *
	 * The real answer is "Yes, but we would have to blow difficulty up to 100-based
	 * to have a chance of doing so without ANY possible collisions; and we would
	 * need to be much more miserly with the gaps on the down-low.
	 */
	@Override
	public final int getDifficulty() {
		return tech.difficulty;
	}

	/**
	 * I need this in order to disable Krakens, only.
	 * @return this hinters degree.
	 */
	@Override
	public final int getDegree() {
		return degree;
	}

	/**
	 * Exists to be overridden - called before the start of each processing
	 * of each puzzle.
	 */
	public void reset() {
	}

	@Override
	public IAccumulator getAccumulator(IAccumulator accu) {
		if ( this.accu == null )
			return accu;
		return this.accu;
	}

	@Override
	public void setAccumulator(IAccumulator accu, ExplodingHintsAccumulator eha, ValidatingHintsAccumulator vha) {
		if ( isAVeryVeryNaughtyBoy() )
			this.accu = eha; // Exploding
		else if ( isAVeryNaughtyBoy() )
			this.accu = vha; // Validating
		else
			this.accu = accu; // normal
	}

	@Override
	public void clearAccumulator() {
		this.accu = null;
	}

	/**
	 * This implementation just returns the name() of the Tech passed into my
	 * constructor; to tell you which Sudoku Solving Technique I implement.
	 * <p>
	 * <b>WARN:</b> Performance! most implementations use this toString(),
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

}
