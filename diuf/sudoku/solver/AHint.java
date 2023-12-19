/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Cells;
import diuf.sudoku.Constants;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Grid.Single;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.SET_BIT;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.solver.Print.printGrid;
import static diuf.sudoku.solver.Print.printHtml;
import diuf.sudoku.solver.hinters.IHinter;
import diuf.sudoku.solver.hinters.Validator;
import diuf.sudoku.solver.hinters.als.Als;
import diuf.sudoku.solver.hinters.als.AlsChainHint;
import diuf.sudoku.utils.Debug;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static diuf.sudoku.utils.MyMath.powerOf2;

/**
 * Abstract Hint is the reference type for ALL types of hints in SE.
 * <p>
 * An actual (real) hint is capable of advancing a Sudoku solution by a step,
 * where as {@link IPretendHint}s such as Warnings do nothing.
 *
 * @see {@link WarningHint} subclass.
 * @see {@link DirectHint}
 * @see {@link IndirectHint}
 */
public abstract class AHint implements Comparable<AHint> {

	/** hints by numElims descending, then toString */
	public static final Comparator<AHint> BY_SCORE_DESC = (AHint a, AHint b) -> {
		// short circuit
		if ( a == b )
			return 0;
		// order by score descending
		int ret;
		if ( (ret=b.getNumElims() - a.getNumElims()) != 0 )
			return ret;
		// order by difficulty ascending
		if ( (ret=a.getDifficulty() - b.getDifficulty()) != 0 )
			return ret;
		// order by toString
		return a.toString().compareTo(b.toString()); // random
	};

	/** ORDER BY score DESCENDING, AlsChainHint.BY_LENGTH, indice, toString */
	public static final Comparator<AHint> BY_ORDER = (AHint a, AHint b) -> {
		// short circuit
		if ( a == b )
			return 0;
		int ret;
		// by score DESCENDING
		if ( (ret=b.getScore() - a.getScore()) != 0 )
			return ret;
		// by difficulty ASCENDING
		if ( (ret=a.getDifficulty() - b.getDifficulty()) != 0 )
			return ret;
		// AlsChainHints by length ASCENDING
		if ( a instanceof AlsChainHint && b instanceof AlsChainHint
		  && (ret=AlsChainHint.BY_LENGTH.compare((AlsChainHint) a, (AlsChainHint) b)) != 0 )
			return ret;
		// by indice of the first cell eliminated from ASCENDING
		if ( (ret=a.getIndice() - b.getIndice()) != 0 )
			return ret;
		// finally by toString ASCENDING
		return a.toString().compareTo(b.toString());
	};

	/**
	 * A String array of CELL_IDs of indices.
	 *
	 * @param indices to get
	 * @return An array of CELL_ID
	 */
	public static String[] ids(Collection<Integer> indices) {
		return Cells.ids(indices);
	}

	/**
	 * Returns a new StringBuilder of the given capacity.
	 *
	 * @param initialCapacity the starting size. It can grow from there
	 * @return a new StringBuilder(initialCapacity)
	 */
	protected static StringBuilder SB(final int initialCapacity) {
		return Constants.SB(initialCapacity); // a single definition
	}

	protected static final boolean T = Constants.T;
	protected static final boolean F = Constants.F;
	protected static final boolean DUMMY = Constants.DUMMY;

	/** QEMPTY is returned by IntQueue.poll when the queue is empty. */
	protected static final int QEMPTY = diuf.sudoku.utils.IntQueue.QEMPTY;

	/**
	 * Should {@link #applyNoisily} also print the hint HTML. This is a static
	 * field to avoid adding a parameter to apply, which is painful.
	 * <p>
	 * This "feature" is ONLY used by LogicalSolverTester, and currently only
	 * when one puzzle is solved, in order to find test-cases in the details.
	 */
	public static boolean printHintHtml;

	/**
	 * Returns a real <b>java.util.</b>ArrayList of the given hint.
	 * @param hint to add
	 * @return {@code java.util.ArrayList<AHint>} of 1 element = hint
	 */
	public static List<AHint> list(AHint hint) {
		List<AHint> list = new ArrayList<AHint>(1) {
			private static final long serialVersionUID = 145908235892L;
			@Override
			public boolean add(AHint e) {
				e.toString(); // trying to avert JTree description docking
				return super.add(e);
			}
		};
		list.add(hint);
		return list;
	}

	public static LinkedList<AHint> linkedList(AHint hint) {
		LinkedList<AHint> list = new LinkedList<>();
		list.add(hint);
		return list;
	}

	/**
	 * Returns a real <b>java.util.</b>ArrayList of the given array.
	 * @param array a smallish array of hints to be copied, ideally of under 32
	 * elements; but if it is a couple of hundred then do not worry about it. If
	 * it is a thousand then you will want something more efficient.
	 * @return {@code java.util.ArrayList<AHint>}
	 */
	public static List<AHint> list(AHint[] array) {
		List<AHint> list = new ArrayList<>(array.length);
		for ( AHint e : array ) {
			if ( e == null )
				break; // it is a null terminated array
			list.add(e);
		}
		return list;
	}

	public static String toString(final Collection<AHint> hints, final int maxHints) {
		assert hints != null;
		assert maxHints > 1;
		final StringBuilder sb = SB(Math.min(hints.size(), maxHints)<<5);
		boolean first = true;
		int i = 1;
		for ( AHint hint : hints ) {
			if(first) first=false; else sb.append(NL);
			sb.append(hint.toFullString());
			if ( ++i > maxHints ) {
				sb.append(NL).append("...");
				break;
			}
		}
		return sb.toString();
	}

	protected static final boolean IS_CACHING_HTML = true; // @check true

	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	public static final int DIRECT=0, WARNING=1, INDIRECT=2, AGGREGATE=3, MULTI=4;

	protected static final String[] GROUP_NAMES = new String[] {
		"Pair", "Triple", "Quad", "Pent", "Hex", "Sept", "Oct", "Nona", "Dec"
	};

	protected static final String[] NUMBER_NAMES = new String[] {
		"two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
	};

	// this method recovers from NUMBER_NAMES is used all over the shop so
	// I do NOT want to modify it is values, but BigWings, being ALS based,
	// can also find XYZ_Wings and does so occassionally, especially when
	// you Shft-F5, so I need a way of going under the bar, and this is it.
	protected String numberName(int i) {
		if ( i>-1 && i<NUMBER_NAMES.length )
			return NUMBER_NAMES[i];
		switch (i) {
		case -1: return "one";
		case -2: return "zero";
		case -3: return "minus one";
		case 9: return "eleven";
		case 10: return "twelve";
		default: return i<0 ? "minus shazbut" : "shazbut";
		}
	}

	public static final int CHARS_PER_HINT = 80; // per toString (approximate)

	protected final Grid grid; // The grid to which this hint applies
	public final IHinter hinter; // The hinter that produced this hint
	public final int degree;	 // The degree of my hinter
	public final int type; // DIRECT, WARNING, INDIRECT, AGGREGATE
	public final Cell cell; // The cell to be set
	public final int value; // The value to set the cell to
	public final Pots reds; // The Cell=>Values to be removed are painted red in the GUI
	public final Pots greens; // Cell=>Values to be painted green in the GUI
	public final Pots oranges; // Cell=>values to be painted orange in the GUI
	public final Pots blues; // Cell=>values to be painted blue in the GUI
	public final ARegion[] bases; // regions to paint blue
	public final ARegion[] covers; // regions to paint green

	// various hint-HTMLs display the debugMessage
	public String debugMessage = "";
	public void setDebugMessage(final String msg) {
		debugMessage = msg;
	}

	/** set true when {@link diuf.sudoku.solver.hinters.Validator#isValid}
	 * finds elimination of this cells solution value, before toString. */
	public boolean isInvalid;
	public void setIsInvalid(final boolean isInvalid) {
		this.isInvalid = isInvalid;
	}

	/** {@link diuf.sudoku.solver.hinters.Validator#isValid} sets invalidity
	 * to the WHOLE Logged line. */
	public String invalidity;
	public void invalidate() {
		this.isInvalid = true;
		this.invalidity = Validator.invalidity;
	}

	/** funnySB is weird. See HintsApplicumulator and AHint.apply->Cell.set. */
	public StringBuilder funnySB;

	// for direct hints
	public AHint(final Grid grid, final IHinter hinter, final Cell cell, final int value) {
		this(grid, hinter, AHint.DIRECT, cell, value, null, null, null, null, null, null);
	}

	// basic indirect hints
	public AHint(final Grid grid, final IHinter hinter, final Pots redPots) {
		this(grid, hinter, AHint.INDIRECT, null, 0, redPots, null, null, null, null, null);
	}

	// indirect hints with pots-to-highlight and bases/covers
	public AHint(final Grid grid, final IHinter hinter, final Pots redPots
			, final Pots greens, final Pots oranges, final Pots blues
			, final ARegion[] bases, final ARegion[] covers) {
		this(grid, hinter, AHint.INDIRECT, null, 0, redPots, greens, oranges, blues
				, bases, covers);
	}

	// the actual constructor
	public AHint(final Grid grid, final IHinter hinter, final int type
			, final Cell cell, final int value, final Pots reds
			, final Pots greens, final Pots oranges, final Pots blues
			, final ARegion[] bases, final ARegion[] covers) {
		this.grid = grid;
		this.hinter = hinter;
		// IHinter.degree is a mild hack: it is allmost allways just shorthand
		// for the IHinter.tech.degree, except in URTs where it is set to the
		// size of the hidden/naked set.
		if ( hinter == null )
			this.degree = 0;
		else
			this.degree = hinter.getDegree();
		this.type = type;
		this.cell = cell;
		this.value = value;
		this.reds = reds;
		this.greens = greens;
		this.oranges = oranges;
		this.blues = blues;
		this.bases = bases;
		this.covers = covers;
	}

	public final int applyNoisily(final boolean isAutosolving, final Grid grid) {
		if ( Log.LOG_MODE >= Log.VERBOSE_2_MODE )
			printGrid(Log.out, grid); // the grid this hint was found in
		final int result = applyImpl(isAutosolving, grid);
		if ( printHintHtml )
			printHtml(Log.out, this);
		return result;
	}

	public final int applyQuitely(final boolean isAutosolving, final Grid grid) {
		return applyImpl(isAutosolving, grid);
	}

	/**
	 * Actually apply this hint to the grid.
	 * <p>
	 * This default implementation is protected, to be overridden by:<br>
	 *   ADirectHint
	 * , AWarningHint
	 * , SummaryHint
	 * , MultipleSolutionsHint
	 * , SolutionHint
	 * , XColoringHintBig
	 * , GemHintMulti
	 * <p>
	 * I am <b>ALWAYS</b> called via {@link #apply(boolean, boolean)} so I am
	 * protected, ergo only accessible to the solver package, which never calls
	 * me, except through the above public apply method. Clear?
	 *
	 * @param isAutosolving set subsequent Naked/Hidden Singles.
	 * @param grid the grid to apply this hint to. Note that it is this grid,
	 *  which <b>MAY BE A COPY</b>, that the hint is applied to, not the grid
	 *  in the hint, or its cells, or regions, or any other. Sigh.
	 * @return the score: numCellsSet*10 + numMaybesRemoved
	 * @throws UnsolvableException on the odd occasion
	 */
	protected int applyImpl(final boolean isAutosolving, final Grid grid) {
		if ( isInvalid || grid==null )
			return 0; // invalid hints are dead cats!
		int myNumElims = 0;
		try {
			final Cell[] cells = grid.cells;
			if ( cell != null ) {
				if ( !isAutosolving && funnySB==null )
					myNumElims += 10 * cells[cell.indice].set(value);
				else
					myNumElims += 10 * cells[cell.indice].set(value, 0, isAutosolving, funnySB);
			}
			if ( reds != null ) {
				if ( reds.isEmpty() ) // empty means trouble (nullable)
					throw new UnsolvableException("applyImpl: reds.isEmpty!");
				int indice, values, pinkos;
				final int[] maybes = grid.maybes;
				if ( isAutosolving ) {
					final Deque<Single> singles = grid.getSinglesQueue();
					for ( Map.Entry<Integer,Integer> e : reds.entrySet() ) {
						indice = e.getKey();
						values = e.getValue();
						if ( (pinkos=maybes[indice] & values) > 0 )
							myNumElims += cells[indice].canNotBeBits(pinkos, singles);
					}
					for ( Single s; (s=singles.poll())!=null; ) {
						if ( cells[s.indice].value == 0 ) // skip already set
							myNumElims += 10 * cells[s.indice].set(s.value, 0, true, funnySB);
					}
				} else {
					for ( Map.Entry<Integer,Integer> e : reds.entrySet() ) {
						indice = e.getKey();
						values = e.getValue();
						if ( (pinkos=maybes[indice] & values) > 0 )
							myNumElims += cells[indice].canNotBeBits(pinkos);
					}
				}
			}
			if ( cell==null && reds==null ) {
				throw new UnsolvableException("applyImpl: cell==null && reds==null!");
			}
		} catch (UnsolvableException ex) { // from cell.set or rc.canNotBeBits
			// Whinges unwanted in recursiveSolve and generate.
			// We guess cell values so break puzzle, but they are important
			// elsewhere. Do not like slow reflection filter, but what else?
			// NB: should whinge if depth==0 (top level) but how to?
			if ( !Debug.isMethodNameInTheCallStack(64, new String[]{"recursiveSolve", "generate"}) )
				Log.teeTrace("WARN: "+Log.me()+" "+this.toFullString()+" caught ", ex);
			throw ex;
		}
		return myNumElims;
	}

	/**
	 * Implementors override this one to return a subtype of IHinter.
	 *
	 * @return the Hinter which discovered this hint.
	 */
	public IHinter getHinter() {
		return hinter;
	}

	public Tech getTech() {
		return hinter.getTech();
	}

	/**
	 * Implementors override this one, the result of which is cached by
	 * getHintTypeName.
	 *
	 * @return the name of the Hinter that discovered this hint.
	 */
	protected String getHintTypeNameImpl() {
		return hinter.toString();
	}

	/**
	 * @return the name of the Hinter which discovered this hint.
	 * <p>
	 * EG: "Naked Single", "Hidden Single", "Naked Pair", "Swampfish",
	 * "Ultra-Right-Wing Square Dance Dissociation (with twin aardvarks)".
	 */
	public final String getHintTypeName() {
		if ( hintTypeName == null )
			hintTypeName = getHintTypeNameImpl();
		return hintTypeName;
	}
	private String hintTypeName; // hint type name

	/** @return the base (blue) regions of this hint. */
	public ARegion[] getBases() {
		return bases;
	}

	/** @return the cover (green) regions of this hint. */
	public ARegion[] getCovers() {
		return covers;
	}

	/** @return the id of the first Region, else null. */
	protected String getFirstRegionId() {
		ARegion[] bs = getBases();
		if ( bs==null || bs.length<1 )
			return null;
		return bs[0].label;
	}

	/**
	 * Subtypes override this one to implement getClueHtml - gets the HTML
	 * for {@code (Shift)F6} or {@code Menu: Options ~ Get a (Big) Clue}.<p>
	 * Just remember that a hint is pointer towards a hint, not a hint itself.
	 *
	 * @param isBig <tt>true</tt> gets more information.
	 * @return a clue, in HTML.
	 */
	protected String getClueHtmlImpl(boolean isBig) {
		final String rid; // regionID
		if ( isBig )
			rid = getFirstRegionId(); // may return null
		else
			rid = null;
		String s = "Look for a "+getHintTypeName();
		if ( rid != null )
			s += " in <b1>"+rid+"</b1>";
		return s;
	}

	/**
	 * Callers get a clue (directions towards a hint) as a HTML string.<p>
	 * I am final, I just cache the result of {@link #getClueHtmlImpl(boolean)}.
	 *
	 * @param isBig <tt>true</tt> gets more information.
	 * @return a clue, in HTML.
	 */
	public final String getClueHtml(boolean isBig) {
		if ( clueHtml == null )
			clueHtml = getClueHtmlImpl(isBig);
		return clueHtml;
	}
	private String clueHtml; // get clue html

	/**
	 * Get this hints base degree of difficulty rating as a double, which is
	 * usually* a multiple of one hundredth, to format as 0.00.
	 * <p>
	 * usually*: floating-point numbers are vage, even doubles.
	 *
	 * @see diuf.sudoku.Difficulty for the ranges (used in Sudoku generator)
	 * @see diuf.sudoku.Tech for the base difficulties. The Chainers make
	 * themselves harder. Sigh.
	 * @return hinter.tech.difficulty: the base difficulty rating of this hint.
	 */
	public int getDifficulty() {
		return hinter.getDifficulty();
	}

	/**
	 * GEM requires a separate difficulty-rating specifically for the total
	 * difficulty of a puzzle, because it sets lots of cell-values that must
	 * be included, else GEM reports a puzzle as "much easier", which it is not,
	 * it is the <u>same</u> bloody puzzle, so the total bloody difficulty must
	 * be reported consistently.
	 * <p>
	 * The downside is that no-upper-bound makes GEM report itself to be harder
	 * than NestedPlus chaining. GEM is pretty tricky, but it is not even in the
	 * same galaxy as Nested-bloody-anything, and you cannot have it both ways!
	 * Oh wait, yes you can: you just need a difficulty-rating specific to the
	 * total difficulty, ergo this method.
	 *
	 * @return the difficulty of this hint specifically for the calculation of
	 *  the total difficulty of a puzzle, including a bonus for each cell-value
	 *  set.
	 */
	public int getDifficultyTotal() {
		return getDifficulty();
	}

	public int getNumElims() {
		if ( numElims == 0 ) {
			if(cell != null) numElims = 10;
			if(reds != null) numElims += reds.totalSize();
		}
		return numElims;
	}
	private int numElims;

//  Visual representation

	/** @return the number of views required to explain this hint. */
	public int getViewCount() {
		return 1;
	}

	/**
	 * Results are Pots that are displayed in a larger font, to differentiate
	 * the effects of this hint from the rest.
	 * <p>
	 * Any On Results are differentiated by adding SET_BIT to there values.
	 * Ons inherently have one value per cell, to which the cell is set when
	 * the hint is applied. Note that Results are display-only, not setPots!
	 *
	 * @return a Pots to display in a larger font.
	 */
	public Pots getResults() {
		if ( cell != null )
			return new Pots(cell.indice, SET_BIT ^ VSHFT[value], DUMMY);
		if ( reds != null )
			return reds;
		return null;
	}

	/**
	 * Get the regions to be highlighted with a pink background,
	 * used to display errors (dont overuse this, please).
	 *
	 * @return
	 */
	public ARegion[] getPinkRegions() {
		return null;
	}

	/**
	 * Get the cells to be highlighted with an aqua background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the selected Cells.
	 */
	public Set<Integer> getAquaBgIndices(int viewNum) {
		if ( greens==null || greens.isEmpty() )
			return null;
		return greens.keySet();
	}

	/**
	 * Get the cells to be highlighted with a pink background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the pink (highlighted) Cells.
	 */
	public Set<Integer> getPinkBgIndices(int viewNum) {
		return null;
	}

	/**
	 * Get indices to be highlighted with a green background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the green (highlighted) indices.
	 */
	public Set<Integer> getGreenBgIndices(int viewNum) {
		return null;
	}

	/**
	 * Get indices to be highlighted with an orange background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the orange (highlighted) indices.
	 */
	public Set<Integer> getOrangeBgIndices(int viewNum) {
		return null;
	}

	/**
	 * Get indices to be highlighted with a blue background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the blue (highlighted) indices.
	 */
	public Set<Integer> getBlueBgIndices(int viewNum) {
		return null;
	}

	/**
	 * Get indices to be highlighted with a yellow background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the yellow (highlighted) indices.
	 */
	public Set<Integer> getYellowBgIndices(int viewNum) {
		return null;
	}

	/**
	 * Get the green Cell=&gt;Values, highlighting ons or maybes to set.
	 * <p>
	 * NOTE: formerly green+red=orange. Now oranges are separate!
	 *
	 * @param viewNum the view number, zero-based.
	 * @return the green Pots, else <tt>null</tt> */
	public Pots getGreenPots(int viewNum) {
		return greens;
	}

	/**
	 * Get the red Cell=&gt;Values, highlighting offs or eliminations.
	 * <p>
	 * NOTE: formerly green+red=orange. Now oranges are separate!
	 *
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getRedPots(int viewNum) {
		return reds;
	}

	/**
	 * Get the orange Cell=&gt;Values, highlighting on+offs or relevant maybes.
	 * <p>
	 * NOTE: formerly green+red=orange. Now oranges are separate!
	 *
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getOrangePots(int viewNum) {
		return oranges;
	}

	/**
	 * Get the blue Cell=&gt;Values, highlighting fins or interesting maybes.
	 *
	 * @param grid the current Grid.
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getBluePots(Grid grid, int viewNum) {
		return blues;
	}

	/**
	 * Get the yellow Cell=&gt;Values, highlighting ALS maybes.
	 *
	 * @return the yellow Pots, else <tt>null</tt>
	 */
	public Pots getYellowPots() {
		return null;
	}

	/**
	 * Get the purple Cell=&gt;Values, highlighting ALS maybes.
	 *
	 * @return the purple Pots, else <tt>null</tt>
	 */
	public Pots getPurplePots() {
		return null;
	}

	/**
	 * Get the brown Cell=&gt;Values, highlighting ALS maybes.
	 *
	 * @return the brown Pots, else <tt>null</tt>
	 */
	public Pots getBrownPots() {
		return null;
	}

	/**
	 * Get the ALSs which contain both regions and cells to highlight in
	 * blue, green, aqua, ...tba...
	 *
	 * @return {@code Collection<Als>}
	 */
	public Als[] getAlss() {
		return null;
	}

	/**
	 * Get the links - the brown arrow from a potential value to his effected
	 * potential value in a chain.
	 *
	 * @param viewNum the view number, zero-based.
	 * @return the links to draw, or <tt>null</tt> if none.
	 */
	public Collection<Link> getLinks(int viewNum) {
		return null;
	}

	/**
	 * the ons for GEMHint only.
	 * @return Idx[color][value]
	 */
	public Idx[][] getSupers() {
		return null;
	}

	/**
	 * The offs for GEMHint only.
	 * @return Idx[color][value]
	 */
	public Idx[][] getSubs() {
		return null;
	}

	/**
	 * Subtypes override this one and {@code toHtml()} caches the result.
	 * Any subsequent views reuse the existing HTML, because HTML generation
	 * can be slow for Nested hints, especially for NestedPlus (like 10 secs,
	 * then swings HTML parser takes like 15 secs to parse the result).
	 * <p>
	 * Use the {@link diuf.sudoku.utils.Html} class to load and format your
	 * HTML document, and the {@link diuf.sudoku.utils.Frmt} class to format
	 * data-types into human readable lists, numbers, etc. Do not start from
	 * scratch, copy/paste toHtmlImpl from a likely (close) IHinter.
	 * <p>
	 * For a simple example see
	 * {@link diuf.sudoku.solver.hinters.single.HiddenSingleHint}.
	 * <p>
	 * For the full-monty see
	 * {@link diuf.sudoku.solver.hinters.chain.BinaryChainHint}
	 * which includes the call to {@code super.appendNestedChainsHtml(...)}.
	 *
	 * @return A HTML explanation of this hint.
	 */
	protected abstract String toHtmlImpl();

	/**
	 * Caches the result of toHtmlImpl.
	 *
	 * @return an HTML explanation of this hint
	 */
	public final String toHtml() {
		if ( html==null || !IS_CACHING_HTML )
			html = toHtmlImpl();
		return html;
	}
	private String html; // toHtmls cache

	/**
	 * Subtypes override toStringImpl. The toHtml() method caches the result.
	 *
	 * NB: toString is called by IHinters BY_DIFFICULTY_ASC_TOSTRING_ASC which
	 * is the primary Comparator in the UsageMap, a TreeMap, so it is hammered,
	 * so make them all as fast as possible, please.
	 *
	 * @return A String which represents this hint.
	 */
	protected abstract StringBuilder toStringImpl();

	/**
	 * This method exists to cache the result of the toStringImpl() method.
	 * Implementors should override the abstract toStringImpl() method, not
	 * this final toString method (against tradition), unless of course your
	 * toString result is dynamic and therefore really cannot be cached, in
	 * which case you make me not final and you override me. Sigh.
	 * <p>
	 * NOTE: All hints are immutable DTOs except for the isInvalid attribute,
	 * so it makes sense to build toString ONCE and cache it, because building
	 * a String is a bit slow, and hint toStrings can (especially in the case
	 * of embedded chainers, requiring recursion) be really slow.
	 *
	 * @return A string representation of this hint to display in the GUIs
	 * hint tree (ie to the user).
	 */
	@Override
	public final String toString() {
		if ( toString == null )
			if ( isInvalid )
				toString = toStringImpl().insert(0, '@').toString();
			else
				toString = toStringImpl().toString();
		return toString;
	}
	private String toString; // toStrings cache

	/**
	 * {@code toFullString()} is logged: hint (effects).
	 * <p>
	 * Note that a leading @ means this hint is invalid.
	 *
	 * @return [@]hint.toString[ ([cellToSet+valueToSet][ ][reds])]
	 */
	public String toFullString() {
		if ( fullString == null ) {
			final StringBuilder sb = SB(128);
			if ( isInvalid )
				sb.append("@");
			sb.append(toStringImpl());
			if ( cell != null )
				if ( reds != null )
					fullString = sb.append(" (").append(cell.id).append(PLUS).append(value).append(" ").append(reds).append(")").toString();
				else
					fullString = sb.append(" (").append(cell.id).append(PLUS).append(value).append(")").toString();
			else if ( reds != null )
				fullString = sb.append(" (").append(reds).append(")").toString();
			else
				fullString = sb.toString();
		}
		return fullString;
	}
	private String fullString; // toFullStrings cache

	public static String toFullString(AHint hint) {
		if ( hint == null )
			return NULL_STRING;
		return hint.toFullString();
	}

	/**
	 * Overridden to prevent a huge number of equivalent chains.
	 * <p>
	 * Two chaining hints are equal as soon as they are from the same hinter
	 * and the removable potential values are the same, despite any/all
	 * differences on how we came to those conclusions.
	 * <p>
	 * This is not dramatic, because we naturally create the hints in order of
	 * increasing complexity; so only more complex hints will be filtered out.
	 *
	 * @param o Object other
	 * @return is this hint equivalent the given Object (presumably a hint).
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof AHint
			&& o.getClass().equals(this.getClass())
			&& equals((AHint) o);
	}

	public boolean equals(AHint o) {
		if ( o == null )
			return false; // sigh.
		if ( o == this )
			return true;
		if ( this instanceof IPretendHint ) {
			// messages, warnings, and analysis
			// slow, but it is all I can think of. Random numbers are OUT!
			return toString().equals(o.toString());
		} else {
			// an "actual" hint
			if ( hinter != o.hinter )
				return false;
			if ( cell != null )
				return cell==o.cell && value==o.value;
			else if ( reds != null )
				return reds.equals(o.reds); // indirect hint
			else
				return false; // end of tether
		}
	}

	/**
	 * Get an integer which identifies this hint, which is the index of my
	 * bucket in HashMaps internal table (and array). Objects that are equal
	 * must produce the same hashCode, but anti-conversely, objects with the
	 * same hashCode may be NOT equal. Ergo hashCodes tend towards lossyness.
	 * <p>
	 * <b>NOTE:</b> Many subtypes override this haphazard implementation.
	 *
	 * @return a hash of cell, value, and redPots hashCodes.
	 * @throws IllegalStateException rather than set a 0 hashCode.
	 */
	@Override
	public int hashCode() {
		// check the cache
		if ( hashCode != 0 )
			return hashCode; // cached
		// cache miss, so calculate my hashCode
		int hc;
		if ( reds==null || reds.isEmpty() ) {
			// a direct hint, or a warning
			if ( cell==null && value==0 ) // a warning
				// slow, but it is all I can think of. Random numbers are OUT!
				hc = toString().hashCode();
			else // a direct hint
				hc = (value<<7) ^ cell.indice;
		} else {
			// indirect or "mixed" hint
			hc = value; // may be 0
			if ( cell != null )
				hc = (hc<<7) ^ cell.indice;
			// nb: hc is still 0 for "normal" indirect hints,
			// no matter coz redPots are deterministic (I think).
			hc = (hc<<13) ^ reds.hashCode();
		}
		// hashCode is NEVER 0!
		if ( hc == 0 )
			return hashCode = 1;
		return hashCode = hc;
	}
	private int hashCode; // hashCode

	/**
	 * @return the score (the number of eliminations) of this hint
	 */
	protected int getScore() {
		if ( score > 0 )
			return score; // cached
		if ( cell != null )
			score = 10;
		else if ( reds != null )
			score = reds.totalSize();
		return score;
	}
	private int score;

	/**
	 * Returns the lowest indice that is present in this hint, for sort order.
	 * It is more efficient (and just nicer) to solve part of the grid first,
	 * and it seems natural to prefer the top-left.
	 *
	 * @return the indice of the first (topest/leftest) cell/elim in this hint
	 */
	private int getIndice() {
		if ( gi != -1 )
			return gi; // cached
		if ( cell != null )
			gi = cell.indice;
		else if ( reds != null )
			gi = reds.firstCellIndice();
		return gi;
	}
	private int gi = -1;

	@Override
	public int compareTo(AHint o) {
		return getNumElims() - o.getNumElims();
	}

	// returns the number of nanoseconds between now and the last time you
	// called me. The first period starts at static init time, so you will
	// want to priming-call me once before your loop for accurate timings.
	protected long took() {
		long t1 = System.nanoTime();
		long took = t1 - t0;
		t0 = t1;
		return took;
//		return MyStrings.format("%,d", took);
	}
	private long t0 = System.nanoTime();

	/**
	 * The complexity method is overridden by types of AHint which are added
	 * to a AggregatedHint, to allow us to add only the simplest hint which
	 * produces these eliminations.
	 *
	 * @return the number of cells, or something else which indicates the
	 * complexity of this hint relative to other hints of this type.
	 */
	public int complexity() {
		return 0;
	}

	/**
	 * If you overwrite IHinter.isAVeryNaughtyBoy to be validated, and you
	 * set any cells, then you also need to implement getSetPots in your hint
	 * types. Yes GEM I am looking at you!
	 * <p>
	 * Overridden by XColoringHintBig, and GEMHintBig which are (so far) the
	 * only hint-types that set multiple cells in a single hint; other hintType
	 * either set a single cell (so do NOT need validation), or removes maybes,
	 * sometimes both; so these pricks had to go and set multiple cells, which
	 * stuffed-up the existing plumbing. Sigh.
	 *
	 * @return
	 */
	public Pots getSetPots() {
		return null;
	}

	/**
	 * Get an integer denoting which Color to paint the setPots. My value is
	 * {@link Constants#GREEN}, {@link Constants#BLUE}, or -1 for NONE.
	 *
	 * @return
	 */
	public int getSetPotsColor() {
		return -1; // -1=none, 0=green, 1=blue
	}

	/**
	 * Is this a Kraken hint?
	 *
	 * @return is this a bloody Kraken hint?
	 */
	public boolean isKraken() {
		return false;
	}

	/**
	 * Every real hint has the grid that contains it, even if it means making
	 * a copy of that grid. WarningHints do not.
	 *
	 * @return
	 */
	public Grid getGrid() {
		if ( this instanceof AWarningHint )
			return null; // even if it has a grid!
		if ( grid != null )
			return grid;
		if ( cell != null )
			return cell.getGrid();
		if ( bases != null && bases.length > 0 )
			return bases[0].getGrid();
		if ( covers != null && covers.length > 0 )
			return covers[0].getGrid();
		return null;
	}

}
