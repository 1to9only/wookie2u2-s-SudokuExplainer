/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Result;
import diuf.sudoku.Values;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
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

	public static final boolean IS_NOISY = false; // @check false

	// set true by generate to suppress logging by AHint and LogicalSolver.
	public static boolean SHUT_THE______UP = false;

	public static boolean setShutThe____Up(boolean stfu) {
		boolean formerSTFU = SHUT_THE______UP;
		SHUT_THE______UP = stfu;
		return formerSTFU;
	}

	public static void noiseln(String msg) {
		if(AHint.IS_NOISY) System.out.println(msg);
	}

	protected static final boolean IS_CACHING_HTML = true; // @check true

	protected static final String NL = diuf.sudoku.utils.Frmt.NL;

	public static final int DIRECT=0, WARNING=1, INDIRECT=2, AGGREGATE=3;

	protected static final String[] GROUP_NAMES = new String[] {
		"Pair", "Triple", "Quad", "Pent", "Hex", "Sept", "Oct", "Nona", "Dec"
	};

	protected static final String[] NUMBER_NAMES = new String[] {
		"two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
	};

	public static final int AVG_CHARS_PER = 80; // per toString (approximate)

	public final AHinter hinter; // The hinter that produced this hint
	public final int degree;	 // The degree of my hinter
	public final int type; // DIRECT, WARNING, INDIRECT, AGGREGATE
	public final Cell cell; // The cell to be set
	public final int value; // The value to set the cell to
	public final Pots redPots; // The cells=>values to be removed are painted red in the GUI

	/** sb is a weird variable specifically for the HintsApplicumulator and
	 * AHint.apply->Cell.set. It's ugly but it's nicest way I can think of. */
	public StringBuilder sb;

	public AHint(AHinter hinter, Cell cell, int value) {
		this(hinter, AHint.DIRECT, cell, value, null);
	}

	public AHint(AHinter hinter, Pots redPots) {
		this(hinter, AHint.INDIRECT, null, 0, redPots);
	}

	public AHint(AHinter hinter, int type, Cell cell, int value, Pots redPots) {
//		if ( hinter==null && !Debug.isClassNameInTheCallStack(5, "AppliedHintsSummaryHint") )
//			Debug.breakpoint();

		this.hinter = hinter;

		// AHinter.degree is a mild hack: it's allmost allways just shorthand
		// for the AHinter.tech.degree, except in URT's where it's set to the
		// size of the hidden/naked set.
		this.degree = hinter==null ? 0 : hinter.degree;

		this.type = type;
		this.cell = cell;
		this.value = value;
		this.redPots = redPots;
	}

	void logHint() {
		// This is too verbose when generating!
		// nb: SHUT_THE______UP also suppresses LogicalSolver's logging.
		if ( SHUT_THE______UP )
			return;
		//TRICK: only SudokuExplainer constructor sets GrabBag.logicalSolver,
		//	and it's only used in the GUI, so GrabBag.logicalSolver!=null
		//	translates to "We are running in the GUI."
		//WARN: RecursiveAnalyser sets GrabBag.logicalSolver=null and restores
		//  it upon completion, thus avoiding the need for the smelly code:
		//if ( !Debug.isClassNameInTheCallStack(10, "RecursiveAnalyser") )
		if ( GrabBag.logicalSolver != null ) { // We are running in the GUI.
			System.out.println();
			if ( GrabBag.grid != null ) {
				System.out.format("%d/%s\n", hintNumber, GrabBag.grid.source);
				System.out.println(GrabBag.grid);
			}
			long now = System.nanoTime();
			System.out.format("%,15d\t%s\n", now-GrabBag.time, toFullString());
			GrabBag.time = now;
		}
	}
	public static int hintNumber = 1;

	/**
	 * Apply this hint to the grid.
	 * <p><b>NOTE</b>: This method is overridden by: ADirectHint
	 * , AggregatedChainingHint, AppliedHintsSummaryHint, AWarningHint
	 * , MultipleSolutionsHint, SolutionHint, and St Marks.
	 * @param isAutosolving true (Grid.AUTOSOLVE) makes Cell.set find and set
	 * subsequent NakedSingles and HiddenSingles; and also makes me find and
	 * set any NakedSingles caused by removing the redPots. I haven't tackled
	 * subsequent hidden singles, and I don't think I will, coz I dunno HOW.
	 * @return numElims = 10*numCellsSet + numMaybesEliminated.
	 */
	public int apply(boolean isAutosolving) { // throws UnsolvableException on the odd occassion
		assert cell!=null || redPots!=null : "Possum Giblets: apply both null!";
		if ( Log.MODE >= Log.VERBOSE_5_MODE && !GrabBag.isGenerating ) // note the new Log.MODE
			logHint();
		// =====================================================================
		// BEWARE: change both methods, or remove IS_NOISY completely.
		// I got damn sick of the mess of debug code which I'm no longer using.
		// I retained apply_NOISILY in case s__t goes all funky again, and I
		// need to debug down to that level. I haven't needed to for kenages.
		// BEWARE: IS_NOISY is too verbose for generate.
		// =====================================================================
		if ( IS_NOISY )
			return apply_NOISILY(isAutosolving);

		int myNumElims = 0;
		try {
			if ( cell != null ) {
				// returns number of cells set (may be >1 when isAutosolving)
				// nb: occassionally throws UnsolvableException
				myNumElims += 10 * cell.set(value, 0, isAutosolving, sb);
			}
			if ( redPots != null ) {
				int pinkBits; // Paul Hogan said "No comment".
				final Deque<Cell> nakedSingles = isAutosolving ? SINGLES_QUEUE : null;
				for ( Cell pc : redPots.keySet() ) // pinkCell
					// if pinkCell still has any of these red values?
					if ( (pinkBits=pc.maybes.bits & redPots.get(pc).bits) != 0 )
						// nb: occassionally throws UnsolvableException
						// nb: populates GrabBag.NAKED_SINGLES if isAutosolving
						myNumElims += pc.canNotBeBits(pinkBits, nakedSingles);
				if ( isAutosolving ) try {
					// set any naked singles that were found by canNotBeBits.
					assert nakedSingles != null;
					Cell ns; // nakedSingle
					while ( (ns=nakedSingles.poll()) != null )
						if ( ns.maybes.size == 1 ) // ignore any already set
							// nb: might add to GrabBag.NAKED_SINGLES
							// nb: occassionally throws UnsolvableException
							myNumElims += 10 * ns.set(ns.maybes.first(), 0
									, isAutosolving, sb);
				} catch (Exception ex) { // especially UnsolvableException
					// nb: SINGLE_QUEUE allways left empty even if not in use
					SINGLES_QUEUE.clear();
					throw ex;
				}
			}
		} catch (UnsolvableException ex) { // from cell.set or rc.canNotBeBits
			// error messages are superflous in recursiveSolve and generate, coz
			// we're guessing cell values, so we routinely break the puzzle, but
			// they're important elsewhere. I really don't like using "slow"
			// reflection to filter them out, but what else?.
			// NB: there should be an error if depth==0 (top level): How to?
			if ( !Debug.isMethodNameInTheCallStack(10, "recursiveSolve", "generate") )
				StdErr.whinge("Error applying: "+this.toFullString(), ex);
			throw ex;
		}
		return myNumElims;
	}
	private static final Deque<Cell> SINGLES_QUEUE = new LinkedList<>();

	/**
	 * I separated out this debug version of apply because it's logically more
	 * complex, so my debugging was causing bugs.
	 * @param isAuto ie isAutosolving
	 * @return
	 */
	private int apply_NOISILY(boolean isAuto) {
		assert IS_NOISY;
		int myNumElims = 0;
		try {
			System.out.format("%,13d %s", took(), toString());
			if ( cell != null ) {
				System.out.print(" ("+cell.id+"="+value+")");
				// returns number of cells set (may be >1 when isAutosolving)
				// nb: occassionally throws UnsolvableException
				myNumElims += 10 * cell.set(value, 0, isAuto, sb);
			}
			if ( redPots != null ) {
				int pinkBits; // Paul's already covered this one, thanks Cecil.
				final Deque<Cell> nakedSingles = isAuto ? SINGLES_QUEUE : null;
				System.out.print(" [");
				String sep = ""; // no leading space for the first one
				for ( Cell pc : redPots.keySet() ) { // pinkCell
					// does pinkCell still have any of these red values?
					if ( (pinkBits=pc.maybes.bits & redPots.get(pc).bits) == 0 )
						// Yeah, nah, I'm not pokin that one.
						System.out.print(sep+pc+" empty pinkBits");
					else {
						System.out.print(sep+pc+"-"+Values.toString(pinkBits));
						// ree-mooove the pinkBits? Wha'about a simple kiss boy?
						// nb: occassionally throws UnsolvableException
						myNumElims += pc.canNotBeBits(pinkBits, nakedSingles);
					}
					sep = " "; // leading space for each subsequent one
				}
				System.out.print("]");
				// set any naked singles that were stripped by canNotBeBits.
				// nb: necessary now coz set(AUTO) does a small-fast search.
				if ( isAuto ) try {
					// set any naked singles that were found by canNotBeBits.
					assert nakedSingles != null;
					Cell ns; // nakedSingle
					while ( (ns=nakedSingles.poll()) != null )
						if ( ns.maybes.size == 1 ) { // ignore any already set
							// NB: this MAY add subsequent singles to queue,
							// but there's no ConModEx from poll, so we poll
							assert ns.value == 0; // ie it's a naked single
							int v = ns.maybes.first();
							System.out.print(" => ("+ns.id+"="+v+")");
							// nb: occassionally throws UnsolvableException
							myNumElims += 10 * ns.set(v, 0, isAuto, sb);
						}
				} catch (Exception ex) { // especially UnsolvableException
					// nb: SINGLE_QUEUE allways left empty even if not in use
					SINGLES_QUEUE.clear();
					throw ex;
				}
			}
			System.out.println();
		} catch (UnsolvableException ex) { // from cell.set or rc.canNotBeBits
			// these error messages are superflous in recursiveSolve, coz we're
			// just guessing cell values, so we routinely break the puzzle, but
			// they're important elsewhere. I really don't like using "slow"
			// reflection to filter them out, but what else?.
			// NB: there should be an error if depth=0 (top level), but how?
			if ( !Debug.isMethodNameInTheCallStack(10, "recursiveSolve") )
				StdErr.whinge("Error applying: "+this.toFullString(), ex);
			throw ex;
		}
		return myNumElims;
	}

	/** Implementors override this one to return a subtype of AHinter.
	 * @return the Hinter which discovered this hint. */
	public AHinter getHinter() {
		return hinter;
	}

	/** Implementors override this one, the result of which is cached by
	 * getHintTypeName.
	 * @return the name of the Hinter that discovered this hint. */
	protected String getHintTypeNameImpl() {
		return hinter.toString();
	}
	/** @return the name of the Hinter which discovered this hint.<p>
	 * EG: "Naked Single", "Hidden Single", "Naked Pair", "Swampfish",
	 * "Ultra-Right-Wing Square Dance Dissociation (with twin aardvarks)". */
	public final String getHintTypeName() {
		return hintTypeName!=null ? hintTypeName
				: (hintTypeName=getHintTypeNameImpl());
	}
	private String hintTypeName; // hint type name

	/** @return the base (blue) regions of this hint. */
	public List<ARegion> getBases() {
		return null;
	}
	/** @return array of bases coz jUnit !compare List. */
	public ARegion[] getBasesArray() {
		List<ARegion> list = getBases();
		if ( list == null)
			return null;
		return list.toArray(new ARegion[list.size()]);
	}

	/** @return the cover (green) regions of this hint. */
	public List<ARegion> getCovers() {
		return null;
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
		return rs==null || rs.size()<1 ? null : rs.get(0).id;
	}

	/** Subtypes override this one to implement getClueHtml - gets the HTML
	 * for {@code (Shift)F6} or {@code Menu: Options ~ Get a (Big) Clue}.<p>
	 * Just remember that a hint is pointer towards a hint, not a hint itself.
	 * @param isBig <tt>true</tt> gets more information.
	 * @return a clue, in HTML. */
	protected String getClueHtmlImpl(boolean isBig) {
		String regionID = isBig ? getFirstRegionId() : null;
		return "Look for a " + getHintTypeName()
			 + (regionID==null ? "" : " in <b1>" + regionID + "</b1>");
	}
	/** Callers get a clue (directions towards a hint) as a HTML string.<p>
	 * I am final, I just cache the result of {@link #getClueHtmlImpl(boolean)}.
	 * @param isBig <tt>true</tt> gets more information.
	 * @return a clue, in HTML. */
	public final String getClueHtml(boolean isBig) {
		return clueHtml!=null ? clueHtml : (clueHtml=getClueHtmlImpl(isBig));
	}
	private String clueHtml; // get clue html

	/**
	 * Get this hints base degree of difficulty rating as a double, which is
	 * usually (but not always) a multiple of one tenth.
	 *
	 * @see diuf.sudoku.Difficulty for the ranges (used in Sudoku generator)
	 * @see diuf.sudoku.Tech for the actual base difficulties, chainers make
	 * themselves harder. Sigh.
	 * @return hinter.tech.difficulty: the base difficulty rating of this hint.
	 */
	public double getDifficulty() {
		return hinter.tech.difficulty;
	}

	public int getNumElims() {
		if(numElims != 0) return numElims;
		if(cell != null) numElims = 10;
		if(redPots != null) numElims += redPots.totalSize();
		return numElims;
	}
	private int numElims;

//  Visual representation

	/** @return the number of views required to explain this hint. */
	public int getViewCount() {
		return 1;
	}

	/** @return the result cell, causing the maybe to be displayed in a slightly
	 * larger bold font, to distinguish it from a plethora of also-rans. */
	public Result getResult() {
		return cell != null
				? new Result(cell, value, true)
				: null;
	}

	/**
	 * Get the cells to be highlighted with an aqua background.
	 * @param viewNum 1..128
	 * @return A Set of the selected Cells.
	 */
	public Set<Cell> getAquaCells(int viewNum) {
		return null;
	}

	/** Get the green (to set) cells=&gt;values.
	 * <p>NOTE: formerly green+red=orange. Now oranges are separate!
	 * @param viewNum the view number, zero-based.
	 * @return the green Pots, else <tt>null</tt> */
	public Pots getGreens(int viewNum) {
		return null;
	}

	/** Get the red (eliminated) cells=&gt;values.
	 * <p>NOTE: formerly green+red=orange. Now oranges are separate!
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt> */
	public Pots getReds(int viewNum) {
		return null;
	}

	/** Get the orange (highlighted) cells=&gt;values.
	 * <p>NOTE: formerly green+red=orange. Now oranges are separate!
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt> */
	public Pots getOranges(int viewNum) {
		return null;
	}

	/** Get the blue (chains=pre-eliminated, ALS=eliminated-value, fish=fin)
	 * cells=&gt;values.
	 * @param grid the current Grid.
	 * @param viewNum the view number, zero-based
	 * @return the red Pots, else <tt>null</tt> */
	public Pots getBlues(Grid grid, int viewNum) {
		return null;
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

	/** Get the links - the brown arrow from a potential value to his effected
	 * potential value in a chain.
	 * @param viewNum the view number, zero-based.
	 * @return the links to draw, or <tt>null</tt> if none. */
	public Collection<Link> getLinks(int viewNum) {
		return null;
	}

	/**
	 * Subtypes override this one and {@code toHtml()} caches the result.
	 * Any subsequent views reuse the existing HTML, because HTML generation
	 * can be slow for Nested hints, especially for NestedPlus (like 10 secs,
	 * then swings HTML parser takes like 15 secs to parse the result).
	 * <p>Use the {@link diuf.sudoku.utils.Html} class to load and format your
	 *  HTML document, and the {@link diuf.sudoku.utils.Frmt} class to format
	 *  data-types into human readable lists, numbers, etc. Don't start from
	 *  scratch, copy/paste toHtmlImpl from a likely (close) IHinter.
	 * <p>For a simple example see
	 *  {@link diuf.sudoku.solver.hinters.single.HiddenSingleHint}.
	 * <p>For the full-monty see
	 *  {@link diuf.sudoku.solver.hinters.chain.BinaryChainHint}
	 *  which includes the call to {@code super.appendNestedChainsHtml(...)}.
	 *
	 * @return A HTML explanation of this hint.
	 */
	protected abstract String toHtmlImpl();

	/** Caches the result of toHtmlImpl.
	 * @return an HTML explanation of this hint */
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

	/** @return A string representation of this hint to display in the GUI's
	 * hint tree (ie to the user).
	 * This method exists to cache the result of the toStringImpl() method.
	 * Implementors should override the abstract toStringImpl() method, not
	 * the toString method (as is "traditional"), unless of course your
	 * toString result is dynamic and therefore really can't be cached,
	 * in which case you override toString(). */
	@Override
//	public final String toString() { // you SHOULD override toStringImpl!
	public String toString() {
		return toString!=null ? toString : (toString=toStringImpl());
		//return toStringImpl();
	}
	private String toString; // toStrings cache

	/**
	 * The {@code toFullString()} method is called only by logging/debugging
	 * code, to display the hint and it's effects on the grid.
	 * @return String of hint type: hint description, the cell to set (if any),
	 * and removable potentials (if any).
	 */
	public String toFullString() {
		if(fullString!=null) return fullString;
		String cv = cell==null ? "" : cell.id+"+"+value; // cell value
		String rp = redPots==null ? "" : redPots.toString(); // removable potentials
		return fullString = toString()+" ("+cv+(cv.length()>0&&rp.length()>0?" ":"")+rp+")";
	}
	private String fullString; // toFullStrings cache

	/**
	 * Overridden to prevent a huge number of equivalent chains.
	 * <p>
 Two chaining hints are equal as soon as they are from the same hinter
 and the removable potential values are the same, despite any/all
 differences on how we came to those conclusions.
 <p>
	 * This is not dramatic, because we naturally create the hints in order of
	 * increasing complexity; so only more complex hints will be filtered out.
	 * @param o Object other
	 * @return is this hint equivalent the given Object (presumably a hint).
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof AHint))
			return false;
		if (!o.getClass().equals(this.getClass())) // not same subtype of AHint
			return false;
		AHint other = (AHint)o;
		if ( this instanceof IActualHint )
			return hinter==other.hinter
				&& redPots==null
					? (cell==other.cell && value==other.value) // direct hint
					: redPots.equals(other.redPots); // indirect hint
		else // messages, warnings, and analysis
			return hashCode() == other.hashCode();
	}

//RANDOM hashCode defeats DEAD_CAT detector!
//	/**
//	 * Get an integer which identifies this hint, which is used as an index into
//	 * HashMaps internal array. Objects that are equal must produce the same
//	 * hashCode, but objects produce the same hashCode may not be equal.
//	 * <br><b>WARN:</b> many subtypes override this "crap" implementation.
//	 * @return the bitwise-or of cell, hinter, and value hashCodes.
//	 */
//	@Override
//	public int hashCode() {
//		if(hashCode!=0) return hashCode;
//		if ( this instanceof IActualHint ) {
//			// all subtype that constitute "actual" puzzle solving hints;
//			// except the SolutionHint, which I solves the puzzle. Sigh.
//			if ( hinter == null )
//				return hashCode = redPots==null
//						? RANDOM.nextInt()+1
//						: redPots.hashCode();
//			else
//				return hashCode = hinter.hashCode()<<4 |
//					(redPots==null
//						? RANDOM.nextInt()+1
//						: redPots.hashCode());
//		} else
//			// messages, warnings, and analysis: Hinter and redPots are both
//			// null, so we revert to the random integer trick.
//			return hashCode = RANDOM.nextInt()+1;
//	}
//	private int hashCode; // hashCode

	/**
	 * Get an integer which identifies this hint, which is used as an index into
	 * HashMaps internal array. Objects that are equal must produce the same
	 * hashCode, but objects produce the same hashCode may not be equal.
	 * <br><b>WARN:</b> many subtypes override this "crap" implementation.
	 * @return a hash of cell, value, and redPots hashCodes.
	 * @throws IllegalStateException rather than set a 0 hashCode.
	 */
	@Override
	public int hashCode() {
		// cached
		if ( hashCode != 0 )
			return hashCode;
		// direct hint
		int hc;
		if ( redPots == null ) {
			if ( cell==null && value==0 ) // not a "real" hint
				hc = toString().hashCode(); // slow, but it's only thing I can think of. Random numbers are OUT!
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

	/** Formerly Used by getHints, see BY_SCORE_DESC_AND_INDICE. */
	public static final Comparator<AHint> BY_SCORE_DESC = (AHint h1, AHint h2) -> {
		// short circuit
		if ( h1 == h2 )
			return 0;
		// order by score descending (NumElims can take a wee while to workout)
		return h2.getNumElims() - h1.getNumElims();
	};

	// this does not add elims to a set cell, so that the comparator then 
	// orders two hints which both set a cell value by there toStrings
	private int getScore() {
		if(score != 0) return score;
		if(cell != null) score = 10;
		else if(redPots != null) score = redPots.totalSize();
		return score;
	}
	private int score;

	// getIndice returns the indice of the first cell (topest/leftest)
	private int getIndice() {
		if(indice != -1) return indice;
		if(cell != null) indice = cell.i;
		else if(redPots != null) indice = redPots.firstCellIndice();
		return indice;
	}
	private int indice = -1;

	/** Used by getHints. */
	public static final Comparator<AHint> BY_SCORE_DESC_AND_INDICE = (AHint h1, AHint h2) -> {
		// short circuit
		if ( h1 == h2 )
			return 0;
		int ret;
		if ( (ret=h2.getScore() - h1.getScore()) != 0 ) // DESC
			return ret;
		// getIndice returns the indice of the first cell encountered,
		// which is PROBABLY the topest-leftest because that is the order in
		// which cells are normally added to redPots; some hinters may differ.
		if ( (ret=h1.getIndice() - h2.getIndice()) != 0 ) // ASC
			return ret;
		return h1.toString().compareTo(h2.toString());
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

}