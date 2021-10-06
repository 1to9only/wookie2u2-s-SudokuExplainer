/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Grid.Single;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.als.Als;
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

/**
 * Abstract Hint is a base for ALL types of hints in Sudoku Explainer.
 * <p>
 * An actual (real) hint is capable of advancing a Sudoku solution by a step.
 * Warnings and info messages are also hints; they just don't DO anything.
 *
 * @see {@link WarningHint} subclass.
 * @see {@link DirectHint}
 * @see {@link IndirectHint}
 */
public abstract class AHint implements Comparable<AHint> {

	/** Formerly Used by getHints, see BY_SCORE_DESC_AND_INDICE. */
	public static final Comparator<AHint> BY_SCORE_DESC
			= (AHint h1, AHint h2) -> {
		// short circuit
		if ( h1 == h2 )
			return 0;
		// order by score descending (NumElims can take a wee while to workout)
		int ret;
		if ( (ret=h2.getNumElims() - h1.getNumElims()) != 0 )
			return ret;
		// then by toString
		return h1.toString().compareTo(h2.toString()); // random
	};

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
		List<AHint> list = new ArrayList<>(1);
		list.add(hint);
		return list;
	}

	/**
	 * Returns a real <b>java.util.</b>ArrayList of the given array.
	 * @param array a smallish array of hints to be copied, ideally of under 32
	 * elements; but if it's a couple of hundred then don't worry about it. If
	 * it's a thousand then you'll want something more efficient.
	 * @return {@code java.util.ArrayList<AHint>}
	 */
	public static List<AHint> list(AHint[] array) {
		List<AHint> list = new ArrayList<>(array.length);
		for ( AHint e : array ) {
			if ( e == null )
				break; // it's a null terminated array
			list.add(e);
		}
		return list;
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
	// I do NOT want to modify it's values, but BigWings, being ALS based,
	// can also find XYZ_Wing's and does so occassionally, especially when
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

	public final AHinter hinter; // The hinter that produced this hint
	public final int degree;	 // The degree of my hinter
	public final int type; // DIRECT, WARNING, INDIRECT, AGGREGATE
	public final Cell cell; // The cell to be set
	public final int value; // The value to set the cell to
	public final Pots redPots; // The Cell=>Values to be removed are painted red in the GUI
	public final Pots greens; // Cell=>Values to be painted green in the GUI
	public final Pots oranges; // Cell=>values to be painted orange in the GUI
	public final Pots blues; // Cell=>values to be painted blue in the GUI
	public final List<ARegion> bases; // regions to paint blue
	public final List<ARegion> covers; // regions to paint green

	/** set true when HintValidator#isValid finds elimination of this cells
	 * solution value, before toString is called. */
	public boolean isInvalid;
	/** HintValidator.report sets hint.invalidity = the WHOLE Logged line. */
	public String invalidity;

	/** SB is a weird variable specifically for the HintsApplicumulator and
	 * AHint.apply->Cell.set. It's ugly but the cleanest way I can think of. */
	public StringBuilder SB;

	// for direct hints
	public AHint(AHinter hinter, Cell cell, int value) {
		this(hinter, AHint.DIRECT, cell, value, null, null, null, null, null, null);
	}

	// basic indirect hints
	public AHint(AHinter hinter, Pots redPots) {
		this(hinter, AHint.INDIRECT, null, 0, redPots, null, null, null, null, null);
	}

	// indirect hints with pots-to-highlight and bases/covers
	public AHint(AHinter hinter, Pots redPots, Pots greens, Pots oranges
			, Pots blues, List<ARegion> bases, List<ARegion> covers) {
		this(hinter, AHint.INDIRECT, null, 0, redPots, greens, oranges, blues
				, bases, covers);
	}

	// the actual constructor
	public AHint(AHinter hinter, int type, Cell cell, int value
			, Pots redPots, Pots greens, Pots oranges, Pots blues
			, List<ARegion> bases, List<ARegion> covers) {
		this.hinter = hinter;
		// AHinter.degree is a mild hack: it's allmost allways just shorthand
		// for the AHinter.tech.degree, except in URT's where it's set to the
		// size of the hidden/naked set.
		if ( hinter == null )
			this.degree = 0;
		else
			this.degree = hinter.degree;
		this.type = type;
		this.cell = cell;
		this.value = value;
		this.redPots = redPots;
		this.greens = greens;
		this.oranges = oranges;
		this.blues = blues;
		this.bases = bases;
		this.covers = covers;
	}

	public final int applyNoisily(final boolean isAutosolving, final Grid grid) {
		Print.grid(Log.out, grid); // the grid this hint was found in
		final int result = applyImpl(isAutosolving, grid);
		if ( printHintHtml )
			Print.html(Log.out, this);
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
	 * , AppliedHintsSummaryHint
	 * , MultipleSolutionsHint
	 * , SolutionHint
	 * , XColoringHintMulti
	 * , GemHintMulti
	 * <p>
	 * I'm <b>ALWAYS</b> called via {@link #apply(boolean, boolean)} so I'm
	 * protected, ergo only accessible to the solver package, which never calls
	 * me, except through the above public apply method. Clear?
	 *
	 * @param isAutosolving set subsequent Naked/Hidden Singles.
	 * @param grid the grid to apply this hint to
	 * @return the score: numCellsSet*10 + numMaybesRemoved
	 * @throws UnsolvableException on the odd occasion
	 */
	protected int applyImpl(final boolean isAutosolving, final Grid grid) {
		if ( isInvalid )
			return 0; // invalid hints are dead cats!
		int myNumElims = 0;
		try {
			if ( cell != null ) {
				if ( !isAutosolving && SB==null ) {
					myNumElims += 10 * cell.set(value);
				} else {
					myNumElims += 10 * cell.set(value, 0, isAutosolving, SB);
				}
			}
			if ( redPots!=null && !redPots.isEmpty() ) {
				Cell c; int pinkos;
				if ( isAutosolving ) {
					final Deque<Single> singles;
					if ( grid != null ) {
						singles = grid.getSinglesQueue();
					} else {
						singles = new LinkedList<>(); // slower
					}
					for ( Map.Entry<Cell,Integer> e : redPots.entrySet() ) {
						if ( (pinkos=(c=e.getKey()).maybes & e.getValue()) != 0 ) {
							myNumElims += c.canNotBeBits(pinkos, singles);
						}
					}
					for ( Single s; (s=singles.poll()) != null; ) {
						if ( s.cell.value == 0 ) { // skip already set
							myNumElims += 10 * s.cell.set(s.value, 0, true, SB);
						}
					}
				} else {
					for ( Map.Entry<Cell,Integer> e : redPots.entrySet() ) {
						if ( (pinkos=(c=e.getKey()).maybes & e.getValue()) != 0 ) {
							myNumElims += c.canNotBeBits(pinkos);
						}
					}
				}
			}
		} catch (UnsolvableException ex) { // from cell.set or rc.canNotBeBits
			// Whinges unwanted in recursiveSolve and generate.
			// We guess cell values so break puzzle, but they are important
			// elsewhere. Don't like slow reflection filter, but what else?
			// NB: should whinge if depth==0 (top level) but how to?
			if ( !Debug.isMethodNameInTheCallStack(64, new String[]{"recursiveSolve", "generate"}) )
				Log.teeTrace("Error applying: "+this.toFullString(), ex);
			throw ex;
		}
		return myNumElims;
	}

	/**
	 * Implementors override this one to return a subtype of AHinter.
	 *
	 * @return the Hinter which discovered this hint.
	 */
	public AHinter getHinter() {
		return hinter;
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
	public List<ARegion> getBases() {
		return bases;
	}

	/** @return array of bases coz jUnit !compare List. */
	public final ARegion[] getBasesArray() {
		List<ARegion> list = getBases();
		if ( list == null)
			return null;
		return list.toArray(new ARegion[list.size()]);
	}

	/** @return the cover (green) regions of this hint. */
	public List<ARegion> getCovers() {
		return covers;
	}

	/** @return array of bases coz jUnit !compare List. */
	public ARegion[] getCoversArray() {
		List<ARegion> list = getCovers();
		if ( list == null)
			return null;
		return list.toArray(new ARegion[list.size()]);
	}

	/** @return the id of the first Region, else null. */
	protected String getFirstRegionId() {
		List<ARegion> rs = getBases();
		if ( rs==null || rs.size()<1 )
			return null;
		return rs.get(0).id;
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
	public double getDifficulty() {
		return hinter.tech.difficulty;
	}

	/**
	 * GEM requires a separate difficulty-rating specifically for the total
	 * difficulty of a puzzle, because it set's lots of cell-values that must
	 * be included, else GEM reports a puzzle as "much easier", which it isn't,
	 * it's the <u>same</u> bloody puzzle, so the total bloody difficulty must
	 * be reported consistently.
	 * <p>
	 * The downside is that no-upper-bound makes GEM report itself to be harder
	 * than NestedPlus chaining. GEM is pretty tricky, but it's not even in the
	 * same galaxy as Nested-bloody-anything, and you can't have it both ways!
	 * Oh wait, yes you can: you just need a difficulty-rating specific to the
	 * total difficulty, ergo this method.
	 *
	 * @return the difficulty of this hint specifically for the calculation of
	 *  the total difficulty of a puzzle, including a bonus for each cell-value
	 *  set.
	 */
	public double getDifficultyTotal() {
		return getDifficulty();
	}

	public int getNumElims() {
		if ( numElims == 0 ) {
			if(cell != null) numElims = 10;
			if(redPots != null) numElims += redPots.totalSize();
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
	 * @return the result cell, causing the maybe to be displayed in a slightly
	 * larger bold font, to distinguish it from a plethora of also-rans.
	 */
	public Result getResult() {
		if ( cell == null )
			return null;
		return new Result(cell, value, true);
	}

	/**
	 * Get the cells to be highlighted with an aqua background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the selected Cells.
	 */
	public Set<Cell> getAquaCells(int viewNum) {
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
	public Set<Cell> getPinkCells(int viewNum) {
		return null;
	}

	/**
	 * Get the cell to be highlighted with a green background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the green (highlighted) Cells.
	 */
	public Set<Cell> getGreenCells(int viewNum) {
		return null;
	}

	/**
	 * Get the cell to be highlighted with an orange background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the orange (highlighted) Cells.
	 */
	public Set<Cell> getOrangeCells(int viewNum) {
		return null;
	}

	/**
	 * Get the cell to be highlighted with a blue background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the blue (highlighted) Cells.
	 */
	public Set<Cell> getBlueCells(int viewNum) {
		return null;
	}

	/**
	 * Get the cell to be highlighted with a yellow background.
	 *
	 * @param viewNum 1..128
	 * @return A Set of the yellow (highlighted) Cells.
	 */
	public Set<Cell> getYellowCells(int viewNum) {
		return null;
	}

	/**
	 * Get the green (to set) Cell=&gt;Values.
	 * <p>
	 * NOTE: formerly green+red=orange. Now oranges are separate!
	 *
	 * @param viewNum the view number, zero-based.
	 * @return the green Pots, else <tt>null</tt> */
	public Pots getGreens(int viewNum) {
		return greens;
	}

	/**
	 * Get the red (eliminated) Cell=&gt;Values.
	 *
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getReds(int viewNum) {
		return redPots;
	}

	/**
	 * Get the orange (highlighted) Cell=&gt;Values.
	 *
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getOranges(int viewNum) {
		return oranges;
	}

	/**
	 * Get the blue (fins, et al) Cell=&gt;Values.
	 *
	 * @param grid the current Grid.
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt>
	 */
	public Pots getBlues(Grid grid, int viewNum) {
		return blues;
	}

	public Pots getYellows() {
		return null;
	}

	public Pots getPurples() {
		return null;
	}

	public Pots getBrowns() {
		return null;
	}

	/**
	 * Get the regions to be highlighted with a pink border.
	 *
	 * @return
	 */
	public List<ARegion> getPinkRegions() {
		return null;
	}

	/**
	 * Get the ALS's which contain both regions and cells to highlight in
	 * blue, green, aqua, ...tba...
	 *
	 * @return {@code Collection<Als>}
	 */
	public Collection<Als> getAlss() {
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
	public Idx[][] getOns() {
		return null;
	}

	/**
	 * The offs for GEMHint only.
	 * @return Idx[color][value]
	 */
	public Idx[][] getOffs() {
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
	 * data-types into human readable lists, numbers, etc. Don't start from
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
	 * NB: toString is called by AHinter's BY_DIFFICULTY_ASC_TOSTRING_ASC which
	 * is the primary Comparator in the UsageMap, a TreeMap, so it's hammered,
	 * so make them all as fast as possible, please.
	 *
	 * @return A String which represents this hint.
	 */
	protected abstract String toStringImpl();

	/**
	 * This method exists to cache the result of the toStringImpl() method.
	 * Implementors should override the abstract toStringImpl() method, not
	 * this final toString method (against tradition), unless of course your
	 * toString result is dynamic and therefore really can't be cached, in
	 * which case you make me not final and you override me. sigh.
	 * <p>
	 * All hints are immutable DTO's (except the isInvalid attribute), so it
	 * just makes sense to build stuff like toString ONCE, and cache it.
	 *
	 * @return A string representation of this hint to display in the GUI's
	 * hint tree (ie to the user).
	 */
	@Override
	public final String toString() {
		if ( toString == null )
			if ( isInvalid )
				toString = "@" + toStringImpl();
			else
				toString = toStringImpl();
		return toString;
	}
	private String toString; // toStrings cache

	/**
	 * The {@code toFullString()} method is called only by logging/debugging
	 * code, to display the hint and it's effects on the grid.
	 *
	 * @return String of hint type: hint description, the cell to set (if any),
	 * and removable potentials (if any).
	 */
	public String toFullString() {
		if(fullString!=null) return fullString;
		final String cv; if(cell==null) cv=EMPTY_STRING; else cv=cell.id+PLUS+value;
		final String rp; if(redPots==null) rp=EMPTY_STRING; else rp=redPots.toString();
		final String sep; if(cv==EMPTY_STRING||rp==EMPTY_STRING) sep=EMPTY_STRING; else sep=SPACE;
		return fullString = toString()+" ("+cv+sep+rp+")";
	}
	private String fullString; // toFullStrings cache

	public static String toFullString(AHint hint) {
		if ( hint == null )
			return NULL_ST;
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
			// slow, but it's all I can think of. Random numbers are OUT!
			return toString().equals(o.toString());
		} else {
			// an "actual" hint
			if ( hinter != o.hinter )
				return false;
			if ( cell != null )
				return cell==o.cell && value==o.value;
			else if ( redPots != null )
				return redPots.equals(o.redPots); // indirect hint
			else
				return false; // end of tether
		}
	}

	/**
	 * Get an integer which identifies this hint, which is my index in HashMaps
	 * internal array. Objects that are equal must produce the same hashCode,
	 * but objects produce the same hashCode may not be equal.
	 * <p>
	 * <b>WARN:</b> Soveral subtypes override this "crap" implementation.
	 *
	 * @return a hash of cell, value, and redPots hashCodes.
	 * @throws IllegalStateException rather than set a 0 hashCode.
	 */
	@Override
	public int hashCode() {
		// cached
		if ( hashCode != 0 )
			return hashCode;
		int hc;
		if ( redPots == null ) {
			// a direct hint, or a warning
			if ( cell==null && value==0 ) // a warning
				// slow, but it's all I can think of. Random numbers are OUT!
				hc = toString().hashCode();
			else // a direct hint
				hc = (value<<8) ^ cell.hashCode;
		} else {
			// indirect or "mixed" hint
			hc = value; // may be 0
			if ( cell != null )
				hc = (hc<<8) ^ cell.hashCode;
			// nb: hc is still 0 for "normal" indirect hints,
			// no matter coz redPots are deterministic (I think).
			hc = (hc<<13) ^ redPots.hashCode();
		}
		if ( hc == 0 )
			throw new IllegalStateException("hashCode 0 for "+toString());
		return hashCode = hc;
	}
	private int hashCode; // hashCode

	/**
	 * @return the score (number of eliminations) for this hint
	 */
	protected int getScore() {
		if(score != 0) return score;
		if(cell != null) score = 10;
		else if(redPots != null) score = redPots.totalSize();
		return score;
	}
	private int score;

	/**
	 * @return the indice of the first cell (topest/leftest)
	 */
	private int getIndice() {
		if(indice != -1) return indice;
		if(cell != null) indice = cell.i;
		else if(redPots != null) indice = redPots.firstCellIndice();
		return indice;
	}
	private int indice = -1;

	/** Used by getHints: order by score DESCENDING, indice */
	public static final Comparator<AHint> BY_ORDER = (AHint a, AHint b) -> {
		// short circuit
		if ( a == b )
			return 0;
		int ret;
		if ( (ret=b.getScore() - a.getScore()) != 0 ) // DESCENDING
			return ret;
		// getIndice returns the indice of the first cell encountered,
		// which is PROBABLY the topest-leftest because that is the order in
		// which cells are normally added to redPots; some hinters may differ.
		if ( (ret=a.getIndice() - b.getIndice()) != 0 ) // ASCENDING
			return ret;
		return a.toString().compareTo(b.toString());
	};

	@Override
	public int compareTo(AHint o) {
		return getNumElims() - o.getNumElims();
	}

	// returns the number of nanoseconds between now and the last time you
	// called me. The first period starts at static init time, so you'll
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
	 * This odd little method is overridden only by XColoringHintMulti which is
	 * (to date) the only hint-type in the system which sets multiple cells in
	 * a single step; every other hint type either sets a single cell, or just
	 * removes some maybes, or even sometimes both, but this prick has to go
	 * and set multiple cells, stuffing-up all the existing plumbing.
	 *
	 * @return
	 */
	public Pots getResults() {
		return null;
	}
	public int getResultColor() {
		return -1; // 0 for green, 1 for blue
	}

	/**
	 * Is this a Kraken hint?
	 *
	 * @return is this a bloody Kraken hint?
	 */
	public boolean isKraken() {
		return false;
	}

}
