/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import static diuf.sudoku.Config.CFG;
import static diuf.sudoku.Constants.BLUE;
import static diuf.sudoku.Constants.GREEN;
import diuf.sudoku.Grid;
import static diuf.sudoku.Grid.*;
import diuf.sudoku.Grid.ARegion;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.Idx;
import static diuf.sudoku.Idx.offCounts;
import static diuf.sudoku.Idx.only;
import diuf.sudoku.IdxC;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.HintsApplicumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.IPrepare;
import static diuf.sudoku.solver.hinters.color.Words.*;
import diuf.sudoku.solver.hinters.single.HiddenSingle;
import diuf.sudoku.solver.hinters.single.NakedSingle;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.IntQueue;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * GEM implements the {@link Tech#GEM} Sudoku solving technique. GEM is an
 * acronym for Graded Equivalence Marks as defined at
 * https://www.sudopedia.org/wiki (see ./#GEM.txt for annoted version),
 * but I cannot understand the specification, so this implementation MAY still
 * be incomplete, but I really dont think so any-longer.
 * <p>
 * GEM is extended extended extended coloring, ergo "ultimate" coloring.
 * <p>
 * It appears that I have a persistent premoulded picture of what colouring and
 * therefore GEM is, which is at odds with the authors view of what GEM is,
 * such that I cannot make his explanation make any bloody sense what-so-ever;
 * which should be perceived as an admission of a mental frailty on my part,
 * rather than a criticism of the specification. So in the end I gave-up trying
 * to implement the full spec and just did what makes sense to me, which is
 * pretty much 3DMedusa SimpleColoring with cheese and pickles, so I added all
 * trimmings that makes sense to me, piece by piece, but the net-result is can
 * not be unsure that I have implemented the whole GEM spec, so there may still
 * be unresolved condiments; its a bit like upgrading your pool dunny into a
 * shopping mall, then shopping at K-Mart anyway. Sigh. If you understand the
 * spec then you may find more eliminations by actually implementing the WHOLE
 * specification. I'm simply too stupid!
 * <p>
 * This implementation of GEM uses a different mark-up to the specification:
 * GREEN represents Parity 1, and BLUE represents Parity 2.
 * <p>
 * Table of (manual) parity markers as per the specification:
 * <pre>
 *          Parity 1    Parity 2    Explanation
 * Par      '           "           bidirectional links
 * Super    -           =           true when pars are true
 * Sub      .           :           false when pars are true
 * </pre>
 * <p>
 * Table of color markers as per my implementation:
 * <pre>
          GREEN       BLUE        Explanation
 colors   value       value       bidirectional links
 ons      +           +           set when color is true
 offs     -           -           eliminated by this color
 My markers are painted in the appropriate color to differentiate them, so
   if you are blue/green color-blind then change green to orange (or whatever)
   and republish, coz what works for you will probably work for everybody.
   * I believe red is the most common color deficet, so red looks green.

 NOTES:
 1. The Par mark is just the value in GREEN/BLUE, using existing colors.
 2. A Super/on mark is a grey value, then a "+" in GREEN/BLUE
 3. A Sub/off mark is a grey value, then a "-" in GREEN/BLUE or RED=both
 So, parity1 is GREEN, parity2 is BLUE. + is On, - is Off. Simples!

 KRC 2021-03-20 08:25 When I try the full Graded Equivalence Marks spec it
 goes to hell in a hand-basket. So either I am too stoopid to understand the
 spec (probable) or the spec is wrong (improbable). I will hear argument either
 way, but I am pretty-pissed-off right now, so I declare the spec to be CRAP!
 Be warned that USE_PROMOTIONS=true produces and handles MANY invalid hints
 (see batch log-file when true). This s__t is unfun!

 KRC 2021-03-21 07:59 I think I found my mistake. I painted the last value of
 a cell or the last place in region; now I only "On" them, and leave it up to
 the later "sibling promoters" whether or not they get full color, which is
 what the original spec says. I just did not listen.

 KRC 2021-03-22 09:58 I have found and fixed the last of my many mistakes, so I
 must eat my words: the specification is NOT crap, I am just stupid!
 1. We need to mark ONLY valid offs, not just mark all offs and then clean-up
    afterwards, because the clean-up removes more than it should. So never
    mark an off that is colors/ons the opposite color.
 2. When we paint a cell-value we remove any off of the opposite color.
 3. Also I put Type 4s above Type 3s to get a Type 4 to test, but Type 3
    is simpler, so I reverted, so Type 4s are now non-existant, but can, I
    believe, exist, if there is just no type 3 found first, which does not happen
	in top1465. My testing is deficient, leaving the proposition unchallenged.

 KRC 2021-03-24 08:45 I found "a few" extra hints from invalid ons which mean
 the other color must be true.

 KRC 2021-03-31 11:40 I built Mark 5 last night, and have spent this morning
 testing it, and tweaking a few things. I will release it as Mark 6 tomorrow
 and that is it for GEM from me; I know not how to improve it any further.

 KRC 2021-06-07 Got the s__ts with gemSolve so ripped-out consequentSingles.
 </pre>
 *
 * @author Keith Corlett 2021-03-17
 */
public final class GEM extends AHinter
implements IPrepare
//		, diuf.sudoku.solver.hinters.IReporter
{
//	@Override
//	public void report() {
//		Log.teef("\n%s: minContradiction=%d, minConfirmation=%d, minElimination=%d\n"
//			, getClass().getSimpleName(), minContradiction, minConfirmation, minElimination);
//		Log.teef("\n%s: minScore=%d\n", getClass().getSimpleName(), minScore);
//		Log.teeln(tech.name()+": TIMES=" + Arrays.toString(TIMES));
//	}
//	private int minContradiction = Integer.MAX_VALUE;
//	private int minConfirmation = Integer.MAX_VALUE;
//	private int minElimination = Integer.MAX_VALUE;
//	private int minScore = Integer.MAX_VALUE;
//	private long[] TIMES = new long[10];
	//             0            1            2            3           4          5            6            7          8
	// 1,952,645,655, 166,440,244, 161,057,914, 551,059,499, 22,045,385, 7,177,886, 718,068,277, 197,793,402, 1,740,095, 0

	/** Should paintPromotions use the "shoot back" rule. */
	protected static final boolean PROMOTIONS_SHOOTS_BACK = true;

	/**
	 * Should {@link #confirmations} look for Hidden Singles?
	 * Rare as rocking horse s__t: 0 in top1465.
	 */
	protected static final boolean DO_CONFIRMATIONS_HIDDEN_SINGLES = false;

	/**
	 * Should {@link #contradictions} look for Type 5?
	 * Rare as rocking horse s__t: 0 in top1465.
	 */
	protected static final boolean DO_CONTRADICTION_TYPE_5 = false;

	// NOTE: BLUE and GREEN are now defined in Constants, for use elsewhere
	// (outside of coloring package, in the GUI) so are shared with Coloring,
	// XColoring, Medusa3D, etc)

	/** names of the colors, in an array to select from. */
	private static final String[] COLORS = {green, blue};
	/** The opposite color. */
	private static final int[] OPPOSITE = {BLUE, GREEN};
	/** colors ON HTML tags. */
	private static final String[] CON = {GON, BON};
	/** colors OFF HTML tags. */
	private static final String[] COFF = {GOFF, BOFF};
	/** colored color names. */
	private static final String[] COLOR_NAMES = {GON+green+GOFF, BON+blue+BOFF};
	/** Initial size of steps StringBuilder. It will grow if necessary. */
	private static final int STEPS_SIZE = 4096;
	/** Initial size of why StringBuilder. It will grow if necessary. */
	private static final int WHY_SIZE = 128;

	// contradiction retvals:
	private static final int SUBTYPE_NONE = 0; // no contradiction was found
	private static final int SUBTYPE_2 = 2; // 2+ green v/ons in cell
	private static final int SUBTYPE_3 = 3; // 2+ green v/ons in region
	private static final int SUBTYPE_4 = 4; // All vs in region see a green v
	private static final int SUBTYPE_5 = 5; // All vs in cell are off
	// confirmation retvals:
	private static final int SUBTYPE_6 = 6; // Naked Single in colors
	private static final int SUBTYPE_7 = 7; // Hidden Single in colors

	/**
	 * Thrown ONLY when a Type 2 hint has been found and added to accu.
	 * NOTE WELL: This exception is eaten. StackTrace does not matter.
	 */
	private static final StopException STOP_EXCEPTION = new StopException();

	/** I use a Map as a Set, so PRESENT is each value in {@link #hashes}. */
	private static final Object PRESENT = new Object();

	/**
	 * Create the array of arrays which is a colors, or supers, subs.
	 * @param a
	 */
	private static void buildColorsArray(final Idx[][] a) {
		int v, c = 0;
		do
			for ( v=1; v<VALUE_CEILING; ++v )
				a[c][v] = new Idx();
		while (++c < 2);
	}

	/**
	 * Clear all the Idxs in the given array (both colors, all values).
	 */
	private static void clearColorsArray(final Idx[][] a) {
		int c = 0;
		do
			clear(a[c]);
		while (++c < 2);
	}

	/**
	 * Deep copy all the Idxs in the given array (both colors, all values).
	 */
	private static Idx[][] deepCopyColorsArray(final Idx[][] a) {
		final Idx[][] result = new Idx[2][VALUE_CEILING];
		int c = 0, v;
		do
			for ( v=1; v<VALUE_CEILING; ++v )
				result[c][v] = new Idx(a[c][v]);
		while (++c < 2);
		return result;
	}

	/**
	 * Create an array of arrays of IdxPlusBuds.
	 */
	private static IdxC[][] buildPlusBudsArray() {
		final IdxC[][] result = new IdxC[2][VALUE_CEILING];
		int v;
		v=1; do result[0][v]=new IdxC(); while(++v<VALUE_CEILING);
		v=1; do result[1][v]=new IdxC(); while(++v<VALUE_CEILING);
		return result;
	}

	/**
	 * Clear all the Idxs in the given array (all values).
	 */
	private static void clear(final Idx[] a) {
		int v = 1;
		do
			a[v].clear();
		while (++v < VALUE_CEILING);
	}

	/**
	 * Used to sort ValueScores by score descending.
	 */
	private static final Comparator<ValueScore> SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

	/**
	 * Used to sort ValueScores by value ascending (ie put them back in order).
	 */
	private static final Comparator<ValueScore> VALUE_ASCENDING =
			(ValueScore a, ValueScore b) -> a.value - b.value;

	// ============================ instance stuff ============================

	// almost everything is a field, to save creating instances once per call,
	// and also to to save passing everything around. For some reason creating
	// an Idx is pretty slow: BFIIK, no arrays here.

	// do we steps+=why (an explanation) to go in the hint?
	private final boolean wantWhy;

	// There is 3 mark-ups in GEM:
	// 1. Par markers implemented as "colors",
	// 2. Super markers implemented as "ons", and
	// 3. Sub markers implemented as "offs".
	//
	// A color is a "parity": a set of "strong" bidirectional links, ie if A1
	// is 3 then D2 is 3, and conversely if D2 is 3 then A1 is 3.
	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// So colors holds the indices of GREEN 7s, or BLUE 2s, or whatever.
	private IdxC[][] colors; // [color][value]
	// An "On" is a "weak" unidirectional link that sets a cell value in this
	// color. Eg: if C7 is 3 then A5 is also 3, which does NOT imply if A5 is 3
	// then C7 is also 3. Supers are used to set cells if this parity turns-out
	// to be true, but are (mostly) excluded from calculations.
	private IdxC[][] ons; // [color][value]
	// An "Off" is a "weak" unidirectional link that removes a potential value
	// in this color. Eg: if C7 is 3 then I7 is not 3, which does NOT imply if
	// I7 is not 3 then C7 is 3, so ONE super is the end of the line: you cannot
	// "build on it" as if it was an actual color. If both parities off a
	// candidate then that candidate is eliminated.
	private Idx[][] offs; // [color][value]
	// colorsOns are colors or ons (ie both) for use in contradictions.
	private Idx[][] colorsPlusOns; // [color][value]
	// all values of each color; array to swap between colors.
	private Idx[] painted; // [color]
	// temporary Idxs: set, read, and forget!
	private Idx tmp1, tmp2, tmp3, tmp4;
	// done is an Idx of the "starting cells" tried for each value, so when an
	// initial cellValue thats conjugate in multiple regions is painted we can
	// avoid repeatedly checking the whole mess.
	private Idx[] done; // [value]
	// for confirmations: an Idx of redPots (eliminations) by value 1..9.
	private Idx[] redIdxs; // [value]

	// A bitset of those values which have an on
	private int[] onsValues; // [color]
	// the number of values offed from each cell in the grid.
	private int[] offCounts; // [color][indice]
	// values that have been offed from each cell in either color.
	private int[][] offedValues; // [color][indice]
	// a bitset of the GREEN and BLUE values; array to swap between colors.
	private int[] colorValues; // [color]

	// a bitset of those values which have been painted either color
	private int paintedValues;
	// the Cell=>Values to eliminate. A field to not build for 99% failure.
	private Pots redPots;

	// A hashCode of each painted cell-set, all values, both colors.
	private HashMap<Long,Object> hashes;

	// the (presumably) "good" color to display "results" in the hint.
	private int hintColor;

	// Contradictions only: the cell/s which caused this hint.
	private Idx hintCause;

	// Type 2 only: the region containing the cells which caused this hint,
	// to be bordered in pink, to help the user find the bastard.
	private ARegion hintRegion;

	// a StringBuilder of the steps required to building the coloring-chains;
	// and also any conclusions of contradictions/eliminations/confirmations.
	// This is a persistant buffer for speed. Faster to re-use than grow new.
	private StringBuilder steps;

	// score of values 1..9 (a field only to support reporting minScore, but it
	// is also faster (I think) to not recreate the array for every call)
	private ValueScore[] scores;

	// why: explanation of why each cell-value is painted this color.
	// sb: contributes bits and pieces to the why-string.
	private StringBuilder why, sb;

	/**
	 * The constructor.
	 */
	public GEM() {
		super(Tech.GEM);
		// We want explanation in GUI and testcases, just not in the batch.
		// nb: the "why" strings slow batch down a MINUTE, which is too much!
		this.wantWhy = !Run.isBatch();
	}

	/**
	 * Prepare is called after puzzles loaded, but BEFORE the first findHints.
	 * @param unused
	 * @param unused2
	 */
	@Override
	public void prepare(final Grid unused, final LogicalSolver unused2) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
	}

	@Override
	public void setFields(Grid grid) {
		this.grid = grid;
		cells = grid.cells;
		sizes = grid.sizes;
		maybes = grid.maybes;
		regions = grid.regions;
		boxs = grid.boxs;
		idxs = grid.idxs;
	}

	@Override
	public void clearFields() {
		grid = null;
		cells = null;
		sizes = maybes = null;
		regions = null;
		boxs = null;
		idxs = null;
	}

	// create my fields. Everything in here should also be in clearMyFields,
	// no-biggy if you miss one, just pointless heap held between calls.
	private void setMyFields() {
		int v;
		// All fields are created and cleared by findHints.
		// BACKSTORY: These where all final, constructed inline or in the
		// constructor, but I now want to minimise the permanent footprint
		// of hinters, coz we are close to OOME -> GC running on overtime.
		// So I pick-on heaviest hinters first: XColoring, Medusa3D, GEM.
		// This WILL slow these three hinters, but hope faster overall.
		this.overpaintCount = 0; // reset from last time
		if ( wantWhy ) {
			steps = SB(STEPS_SIZE);
			why = SB(WHY_SIZE);
			sb = SB(128);
		} else
			steps = why = sb = null;
		// [colors GREEN=0 or BLUE=1][values 1..9]
		colors = buildPlusBudsArray();
		ons = buildPlusBudsArray();
		buildColorsArray(offs=new Idx[2][VALUE_CEILING]);
		buildColorsArray(colorsPlusOns=new Idx[2][VALUE_CEILING]);
		painted = new Idx[]{new Idx(), new Idx()};
		done = new Idx[VALUE_CEILING];
		v=1; do done[v]=new Idx(); while(++v<VALUE_CEILING);
		tmp1 = new Idx();
		tmp2 = new Idx();
		tmp3 = new Idx();
		tmp4 = new Idx();
		onsValues = new int[2];
		offCounts = new int[GRID_SIZE];
		offedValues = new int[2][GRID_SIZE];
		colorValues = new int[2];
		redPots = new Pots();
		scores = new ValueScore[VALUE_CEILING];
		// NOTE WELL: sort requires notNull 0
		v=0; do scores[v]=new ValueScore(v); while(++v<VALUE_CEILING);
		redIdxs = new Idx[VALUE_CEILING];
		v=1; do redIdxs[v]=new Idx(); while(++v<VALUE_CEILING);
		hashes = new HashMap<>(128, 0.75F);
	}

	private void clearMyFields() {
		steps = why = sb = null;
		colors = ons = null;
		offs = colorsPlusOns = null;
		painted = done = redIdxs = null;
		tmp1 = tmp2 = tmp3 = tmp4 = null;
		onsValues = colorValues = offCounts = null;
		offedValues = null;
		redPots = null;
		scores = null;
		hashes = null;
		hintRegion = null;
		hintCause = null;
	}

	/**
	 * Find 3DMedusa hints in the grid, and add them to accu.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints() {
		int indice;
		// presume that no hint will be found
		boolean result = false;
		try {
			int n, i, v; // StartingValues: numberOf, indexOf, value
			setMyFields();
			// Step 1: foreach value in 2-or-more conjugate relationships,
			//         order by num conjugates + num bivalues DESCENDING.
			// WARN: startingValues filters to score >= 4 because that is
			// the lowest score that hints in top1465. THIS MAY BE WRONG!
			// The higher this value the faster GEM is per elimination;
			// upto ~7, where it starts missing too many eliminations.
			for ( n=startingValues(),i=0; i<n; ++i ) { // repopulate scores
				v = scores[i].value;
				// foreach region in the grid
				for ( ARegion r : regions ) {
					// if v has two places in this region
					if ( r.numPlaces[v] == 2
					  // and we have not already painted v in this region
					  && !done[v].has(indice=r.indices[IFIRST[r.places[v]]])
					  // and the search finds something
					  // nb: search may disable me!
					  && ( result |= search(indice, v) )
					  // and we want onlyOne hint
					  && oneHintOnly )
						return result; // all done
				}
			}
		} catch ( UncleanException ex ) {
			// This indicates an invalid grid so throw UnsolvableException.
			// To debug it change both throws to UncleanException instead!
			// USE_PROMOTIONS -> create(Big)?Hint: bad elim or cell-value.
			Log.teeln("WARN: "+Log.me()+": "+ex.getClass().getSimpleName()+NL
					+ex.getMessage());
			// disable me for this puzzle (re-enabled by prepare)
			setIsEnabled(false); // use the setter!
		} catch ( StopException meh ) {
			// thrown after a contradiction/confirmation hint is added to accu
			result = true;
		} finally {
			clearMyFields();
		}
		return result;
	}

	/**
	 * To select the best starting value, take the value that (is in 2 or more
	 * conjugate pairs, which) has the most bivalue cells. These bivalue cells
	 * expand the coloring clusters beyond the single digit boundary.
	 * <p>
	 * From the spec: "Seed candidates should be chosen which are hoped will
	 * give the maximum return, and time spent considering the best choice is
	 * worthwhile, as a complete mark-up is time consuming."
	 * <p>
	 * The spec implies this can be done in simple coloring, but I am ignoring
	 * it to keep my hinters independent of each other. This is "fast enough".
	 *
	 * @return the number of values with a score of 4 or more.
	 */
	private int startingValues() {
		int cands, m, n, i;
		// make indice align with value
		Arrays.sort(scores, VALUE_ASCENDING);
		// count conjugate pairs for each value
		// and build cands: values in 2+ conjugate pairs
		// nb: a "conjugate pair" is two places for a value in a region. These
		// two cells are conjoined (a strong link): either cellA or cellB is v,
		// and are a complete set, hence if cellA is NOT v then cellB must be.
		cands = 0;
		int value = 1;
		do {
			scores[value].score = 0;
			for ( ARegion r : regions )
				if ( r.numPlaces[value]==2 && ++scores[value].score>1 )
					cands |= VSHFT[value];
		} while (++value < VALUE_CEILING);
		// foreach bivalue cell value in 2+ conjugate pairs, ++value.score
		i = 0;
		do
			if ( sizes[i]==2 && (m=maybes[i] & cands)>0 )
				for ( int x : VALUESES[m] )
					++scores[x].score;
		while (++i < GRID_SIZE);
		// order by score descending
		Arrays.sort(scores, SCORE_DESCENDING);
		// count scores >= 4. Min min score is 3, but I use 4 coz its top1465s
		// min that hints. Theres no theory for 4, its just what works for me,
		// so I presume it will work for all Sudoku puzzles, but it may not!
		// If its wrong it cannot be very wrong: it cannot skip many hints.
		n = 0;
		do
			if ( scores[n].score < 4 // comments above
			  || scores[n].value == 0 ) // stop at value 0 (null terminator)
				break;
		while (++n < VALUE_CEILING);
		return n;
	}

	/**
	 * Search this Cell and value (which is in a conjugate pair) for GEM hints.
	 *
	 * @param indice of cell, with three regions to search: box, row, and col.
	 *  Atleast one of these regions contains two instances of value, ie this
	 *  cell is in atleast one conjugate pair on the given value, not that it
	 *  really matters, we just avoid wasting time searching cells that cannot
	 *  contribute to a GEM hint coz they have no conjugate/s. Sigh.
	 * @param v the value to search for
	 * @return were any hint/s found. Note that only ONE Type 2 is sought.
	 */
	private boolean search(final int indice, final int v) {
		AHint hint;
		Long hash;
		int subtype // sub-type of this hint
		  , mst; // multi-hint subtype
		final int n;
		// presume that no hint will be found
		boolean result = false;
		// clear the paint artifacts so each search is independant of previous.
		// done and hashes are the ONLY hangovers, by design.
		clearColorsArray(colors);
		clearColorsArray(ons);
		clearColorsArray(offs);
		Arrays.fill(offedValues[GREEN], 0);
		Arrays.fill(offedValues[BLUE], 0);
		colorValues[GREEN]=colorValues[BLUE]=paintedValues = 0;
		if ( wantWhy )
			steps.setLength(0); // the painting steps, and conclusions if any
		redPots.clear();
		// Step 2: paint cells in the conjugate pair opposite colors.
		//
		// Step 3: Mark opposite parities in bivalue cells.
		// Check your cluster to see if there are bivalue cells
		// where only one of the candidates is colored. Foreach
		// color the second candidate with the opposite color.
		//
		// Step 4: Expand the cluster for newly added values.
		// Foreach candidate added in step 3, check remaining
		// candidates of that value.
		// * If the candidate you are checking is part of one or
		//   more conjugate pairs containing any recently added
		//   candidates, expand your cluster by coloring the
		//   candidate you are checking with the opposite color
		//   from that of the recently added candidate.
		// * Continue coloring for each value until no more
		//   candidates can be added.
		//
		// paintMonoBoxs: colors/ons any Hidden Singles left in boxs.
		//
		// paintPromotions: last cell-value or place-in-region is an ons;
		// and then any ons with colors siblings/cell-mates are painted.
		try {
			// paint this cell-value, and it is consequences, ...
			if ( wantWhy ) {
				why.setLength(0); // clear the StringBuilder
				why.append("Let us assume that ").append(CELL_IDS[indice])
				.append(MINUS).append(v).append(IS).append(COLOR_NAMES[GREEN])
				.append(NL);
			} else
				why = null;
			paint(indice, v, GREEN, true);
			// paint/on consequences, and there consequences, and there cons...
			while ( paintMonoBoxs() | paintPromotions() ); // intentional null statement
			// we search each region/value ONCE only.
			for ( int v1 : VALUESES[paintedValues] )
				done[v1].or(colors[GREEN][v1], colors[BLUE][v1]);
			// we search each distinct both-color-sets ONCE only.
			// nb: these hashes are 64 bits, to reduce collisions.
			// nb: there is no distinction between green and blue in this hash
			hash = hash(colors[GREEN], VALUESES[colorValues[GREEN]])
				 | hash(colors[BLUE], VALUESES[colorValues[BLUE]]);
			if ( hashes.putIfAbsent(hash, PRESENT) != null ) // addOnly
				return false;
			// my minimum colored cells is 3.
			// WARN: This limit is top1465 specific. It is probably too high for
			// some puzzles. There is no theoretical basis for this, it is just
			// what works for me. Remove this limit for correctness, or come up
			// with a theory to justify it. I am just a stupid programmer.
			if ( (n = countAll(colors[GREEN], VALUESES[colorValues[GREEN]], painted[GREEN])
				    + countAll(colors[BLUE], VALUESES[colorValues[BLUE]], painted[BLUE])) < 3 )
				return false; // too few cells painted
			// Step 5: Analyse the cluster.
			// 5.1 If either color contains a contradiction
			//     then the opposite color MUST be true.
			// nb: minContradiction=3 so there is no filter on it
			if ( (subtype=contradictions()) != SUBTYPE_NONE
			  // presuming that the opposite color is not rooted too (sigh)
			  && (hint=createBigHint(v, subtype)) != null
			) {
				result = true;
				accu.add(hint);
				// big hint is always the last hint I produce.
				throw STOP_EXCEPTION;
			}
			// nb: minElimination=3 so there is no filter on it
		    // do not look if contradictions bombed (it should not, in prod)
			if ( subtype == 0
			  // 5.2 If any uncolored cell-value sees both colors
			  //     then it can be eliminated.
			  && (subtype=eliminations()) > 0
			) {
				// search eliminations for confirmation that color is true.
				hint = null;
				// minConfirmation=11
				if ( n > 10
				  // 5.3 promote the "little" elimination hint to a big hint
				  && (mst=confirmations()) > SUBTYPE_NONE
				  && (hint=createBigHint(v, mst)) != null
				) {
					result = true;
					accu.add(hint);
					hint = null;
					// big hint is always the last hint I produce.
					throw STOP_EXCEPTION;
				} else
					hint = createHint(v, subtype);
				if ( hint != null ) {
					result = true;
					if ( accu.add(hint) )
						return result;
					// big hint is always the last hint I produce.
					if ( hint instanceof GEMHintBig )
						throw STOP_EXCEPTION;
				}
			}
		} catch ( OverpaintException ex ) {
			// from paint, paintMonoBoxs, or promotions.
			// overpaintCount to prevent spamming the ____ out of the log
			if ( ++overpaintCount < 7 )
				Log.teeln("WARN: GEM: Overpaint: "+ex.getMessage());
			return false;
		}
		return result;
	}
	private int overpaintCount;

	/**
	 * Paint this cell this color, and all of it is consequences, recursively.
	 * <pre>
	 * "Recursively" means this paint method is applied to every painted cell,
	 * hence a request to paint a cell actually paints it and all consequences.
	 * Those "all consequences" are two-fold:
	 * (1) if this value has two places in any of this cells regions, then the
	 *     other place is painted the opposite color; and
	 * (2) if the cell has two potential values, then the other value is
	 *     painted the other color.
	 * If this smells a lot like static XY Forcing Chaining thats coz it is.
	 * Coloring is faster, but less capable. Dynamic Coloring anybody? Sigh.
	 * </pre>
	 * WARN: You check that the cell-value is not already painted this color
	 * before calling paint, because building the why parameter is slow.
	 * <p>
	 * Paint implements Steps 2 to 4 of ./#XColoring.txt with a recursive DFS,
	 * where the instructions imply a BFS, without saying so explicitly, which
	 * is a warning. DFS is faster, but newbs tend towards BFS, so being an old
	 * prick (an experienced programmer), I "fixed" it using my secret decoder
	 * ring (an IQ that IS enough to blow my own hat off, thanks for that Bob).
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any of the three regions containing this cell except the "source"
	 *    region (nullable) has two places for value then paint the otherCell
	 *    the opposite color; and recursively paint any consequences.
	 * 3. If biCheck is true and cell is bivalue then paint the otherValue the
	 *    opposite color; and recursively paint any consequences.
	 * </pre>
	 * Paint populates the two colors: GREEN and BLUE, by value;
	 * and also populates the paintedValues bitset.
	 * <p>
	 * When a cell-value is painted (1) all other places for this value in the
	 * cells box, row, and col are "off"ed (displayed with a '-' to the right);
	 * and (2) all other potential values of this cell "off"ed. These offs are
	 * used in eliminations: if a cell-value is "off"ed by both colors then it
	 * can be eliminated.
	 * <p>
	 * IMPROVED: why String is now constructed ONLY in the GUI when we its used
	 * in the HTML displayed to the user, or verify why in a test-case. GEMTest
	 * compares full HTML. In the batch (LogicalSolverTester), and in Generate,
	 * building the why String is just a waste of quite-a-lot-of time.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY when used: so we always
	 * pretest "is this value already painted this color" BEFORE building the
	 * why String, even in the GUI, because it is slow: a MINUTE per top1465.
	 * IMPROVED: Unsure if the slow part is building each why String, or append
	 * to steps, so Steps is now a StringBuilder, clear with steps.setLength(0)
	 * instead of growing a new buffer EACH AND EVERY time with steps += why.
	 * IMPROVED: The wantWhy field is set ONCE in the constructor.
	 * <p>
	 * SUGGEST: Problem is it takes ages building steps-SB, which is then used
	 * one time in like 500; so to avoid building unused whys. Keep fast DFS
	 * and write a new presentation BFS: If the DFS hints and wantWhy, then run
	 * the new presentation BFS to produce a hint with an explanation. The DFS
	 * results will be sufficient for the batch, where wantWhy is always false.
	 *
	 * @param cell cell to paint, recursively
	 * @param v value to paint
	 * @param c color to paint this cell-value: GREEN or BLUE
	 * @param biCheck if true then I also paint the otherValue of each bivalue
	 *  cell (cell.size==2) the opposite color, recursively. If false then skip
	 *  step 3 as a waste of time.
	 * @param why a one-liner defining why paint this cell-value?
	 * @return any cells painted (always true, else throws)
	 * @throws OverpaintException when a cell-value is painted in both colors.
	 */
	private boolean paint(final int indice, final int v, final int c
			, final boolean biCheck) throws OverpaintException {
//System.out.println("paint "+CELL_IDS[indice]+"-"+v+" "+COLORS[c]);
		int other, i; // the other indice/value
		// constants for understandability and speed (sort of).
		final int sv = VSHFT[v]; // shiftedValue: value as a Values bitset
		final int o = OPPOSITE[c]; // lookup the other color index ONCE
		final Idx[] otherColor = colors[o]; // lookup the other color ONCE
		// If cell-value is already painted this color then you have broken the
		// pre-test requirement, which is intended to reduce the time wasted
		// building why-strings that are never used. Note that asserts effect
		// developers only, who run with java -ea, so bad code NEVER makes it
		// through to prod. Well, almost never, which is not never. Sigh.
		assert !colors[c][v].has(indice);
		// If cell-value is already painted the opposite color then throw.
		// Intersecting colors-sets renders both invalid, so neither is usable.
		//
		// If unsure then ignore it and chase down any log WARN:s but you MUST
		// be sure that it is valid to avert an overpaint, do NOT just lazily
		// make the warning message go away; build a logical justification (or
		// not) otherwise GEM produces invalid hints. The logic MUST stand-up
		// as a whole. It's one out, all out logic. That's why GEM is difficult
		// to implement. Until its all right then none of its right. A misstep
		// breaks everything. So development is ONLY incremental, step by step;
		// verifying each move fully before moving on. Ergo, GEM is hard!
		if ( otherColor[v].has(indice) )
			throw new OverpaintException(CELL_IDS[indice]+MINUS+v+" is already "+COLORS[o]);
		// 1. Paint the given cell-value this color.
		colors[c][v].add(indice);
		paintedValues |= colorValues[c] |= sv;
		// painting full-colors usurps the "on", if any
		ons[c][v].remove(indice);
		// remember the steps in these two coloring-set (a StringBuilder)
		if ( wantWhy ) {
			steps.append(why);
			why.setLength(0);
		}
		// Now we examine the consequences of painting $cell color $c.
		// 2. Paint the other cell in each conjugate pair the opposite color.
		final Cell cell = cells[indice]; // dereference the cell indice ONCE
		for ( ARegion r : cell.regions ) { // box, row, col
			if ( r.numPlaces[v] == 2
			  // pre-check faster than building why string pointlessly
			  && !otherColor[v].has(other=r.indices[IFIRST[r.places[v] & ~cell.placeIn[r.rti]]])
			) {
				if ( wantWhy ) {
					why.append(colorMinus(c, indice, v))
					.append(CONJUGATE_IN).append(r.label).append(IS)
					.append(colorMinus(o, other, v))
					.append(NL);
				}
				// paint otherCell-v the opposite color, recursively.
				paint(other, v, o, true);
			}
			// off all other places for v in r
			offs[c][v].or(tmp1.setAndNot(r.idxs[v], CELL_IDXS[indice]));
			// It tested faster when IntQueue is final. BFIIK!
			for ( final IntQueue q=tmp1.indices(); (i=q.poll())>QEMPTY; )
				offedValues[c][i] |= sv;
		}
		// 3. Paint the other value of this bivalue cell the opposite color
		// skip this bi-check when we are painting "the other value"
		if ( biCheck
		  // if this cell has just two potential values
		  && sizes[indice] == 2
		  // pre-check faster than building why string pointlessly
		  && !otherColor[other=VFIRST[maybes[indice] & ~sv]].has(indice)
		) {
			if ( wantWhy ) {
				why.append(colorMinus(c, indice, v))
				.append(ONLY_OTHER_VALUE_IS)
				.append(colorMinus(o, indice, other))
				.append(NL);
			}
			// paint cell-otherValue the opposite color, skipping biCheck
			paint(indice, other, o, false);
		}
		// off all other values of this cell
		for ( int ov : VALUESES[other=maybes[indice] & ~sv] )
			offs[c][ov].add(indice);
		offedValues[c][indice] |= other;
		return true;
	}

	/**
	 * paintMonoBoxs is a KRC original. There is no other explanation, so I have
	 * done my best to explain it here.
	 * <p>
	 * A "mono-box" is a box with only one place for v (ie a hidden single)
	 * which may leave the "causal box" with one place for v, constituting a
	 * strong bidirectional link, so fully "paint" the v in the "causal box".
	 * However, if the link cannot be proven bidirectional then create an "on",
	 * which allows the cell value to be set in the event that this color is
	 * proven ALL true, but is NOT used in any further logic (with just one
	 * exception: "ons" are used in contradictions).
	 * <p>
	 * paintMonoBoxs differs from paintPromotions in its ability to discern the
	 * strong link between two cells. Promotions just "ons" it, leaving it is
	 * promotion to chance, where-as paintMonoBoxs "field promotes" it.
	 * <p>
	 * The "shoot back" rule: We start with a cell in the grid that is painted
	 * this color, called the source cell. We examine each of the four boxs
	 * effected by the source cell (two left/right, and two above/below).
	 * If the effected box, minus buddies of all cells painted dis color leaves
	 * ONE cell (a Hidden Single, called the suspect) in the effected box then
	 * treat it, but how: colors or ons? To determine this we "shoot back".
	 * <p>
	 * Shoot back: if the box containing the source cell, minus buddies of the
	 * cell to paint (Note *1), leaves ONE cell in da box containing the source
	 * cell then paint the suspect because there is a bidirectional relationship
	 * (a "strong" link) between da suspect and da source cell, succinctly:<br>
	 * {@code if A is v then B is v AND CONVERSELY if B is v then A is v}<br>
	 * so we can paint B this color, else we just "on" the bastard.
	 * <p>
	 * Note (*1) when we shoot back we cannot minus the buddies of (cells of
	 * this color minus the source cell). I tried, and it does not work, but I
	 * do not understand why it does not work. Mine is not to reason why.
	 * <p>
	 * Possibly we could "shoot back" faster just in paintPromotions using:
	 * if an ons buds leave ONE v in any box that is this color den paint da on.
	 * So I tried that and it works (paints some) but finds less elims, so I
	 * did not chase it down, I just use BOTH, finding more than either alone.
	 * It is nice to turn a ____up into a win, but I worry about paintPromotions
	 * not doing the SAME as me. It should, AFAIK, but still have not chased it.
	 * Even finding an example would be long tedious pernickety work.
	 *
	 * @return any
	 */
	private boolean paintMonoBoxs() {
		int indice; // identifies the cell to paint
		ARegion srcBox; // the box that contains the source cell
		boolean any = false; // any painted
		boolean first = true; // is this the first painted or onned
		// foreach color
		int c = GREEN;
		do { // GREEN, BLUE
			// foreach value which has a cell painted thisColor
			for ( int value : VALUESES[colorValues[c]] ) {
				// foreach sourceCell whose value is painted thisColor
				for ( int source : colors[c][value].indicesCached() ) {
					// foreach of four boxes effected by this source cell.
					// ebi effectedBoxIndex is an index in grid.regions
					for ( int ebi : INDEXES[EFFECTED_BOXS[BOX_OF[source]]] ) {
						// if effected box vs less this colors cells+buds
						// is one only. Repeatedly call plusBuds() because
						// paint adds colors[c][v] beneath me; so plusBuds
						// has a cache (updated upon change), for speed.
						if ( tmp1.setAndNotAny(boxs[ebi].idxs[value], colors[c][value].plusBuds())
						  && tmp1.size() == 1
						) {
							srcBox = boxs[BOX_OF[source]]; // contains ci.
							indice = tmp1.peek(); // cell to paint
							// cell to paint needs a buddy in the box that
							// contains source cell, else forget it: c2 is
							// a Hidden Single sans paints, making the cell
							// to paint nondeterministic, ie NOT my problem
							if ( srcBox.idxs[value].intersects(BUDDIES[indice])
							  // and the "shoot back" rule
							  && tmp2.setAndNot(srcBox.idxs[value], BUDDIES[indice]).size() == 1
							) {
								// a "strong" bidirectional link
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										why.append(MONOBOXS_LABEL);
									}
									// nb: REGION_LABELS is coincident with boxs
									why.append(colorMinus(c, source, value))
									.append(LEAVES).append(CELL_IDS[indice]).append(ONLY).append(value).append(IN).append(REGION_LABELS[ebi])
									.append(COMMA_WHICH_LEAVES).append(CELL_IDS[source]).append(ONLY).append(value).append(IN).append(srcBox.label)
									.append(PRODUCES).append(colorMinus(c, indice, value))
									.append(NL);
								}
								// nb: paint removes any pre-existing On
								any |= paint(indice, value, c, false);
							} else if ( !ons[c][value].has(indice) ) {
								// a "weak" mono-directional link.
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										steps.append(MONOBOXS_LABEL);
									}
									steps.append(colorMinus(c, source, value))
									.append(LEAVES).append(CELL_IDS[indice]).append(ONLY).append(value).append(IN).append(REGION_LABELS[ebi])
									.append(PRODUCES).append(colorPlus(c, indice, value))
									.append(NL);
								}
								ons[c][value].add(indice);
								onsValues[c] |= VSHFT[value];
							}
						}
					}
				}
			}
		} while (++c < 2);
		return any;
	}

	/**
	 * Calculate an uncached hash64 of a color (of the upto 9 Idxs).
	 * <p>
	 * Note that these hashes are now 64 bits, reducing collisions to 17. I use
	 * the new hash64 method of Idx, which does NOT cache. Each hash64 contains
	 * the indices of cells whose v is painted this color, so the net result is
	 * indices of all cells with any value painted this color, which is "mostly
	 * distinct enough" to identify a painted-set. The low-17 of a2 and the
	 * high-17 of a1 collide (both sets of cells use the same bits), so where a
	 * collision occurs we wrongly skip the search, about one in 500 of which
	 * produces a hint, so we are missing ? hints here. A few but insignificant.
	 * <p>
	 * Run the batch with/without hash-suppression to find out how many: <pre>
	 *     with HS: 1,874,477,500  13546  138,378  130711  14,340  GEM
	 *       no HS: 1,790,473,600  13546  132,177  130711  13,697  GEM
	 *     fast HS: 1,788,628,200  13546  132,041  130711  13,683  GEM
	 * LongHashSet: 1,804,973,600  13546  133,247  130711  13,808  GEM
	 *    reverted: 1,815,852,100  13546  134,050  130711  13,892  GEM
	 *  wrap count: 1,802,735,900  13546  133,082  130711  13,791  GEM
	 *  inline 6.2: 1,768,956,182  13546  130,588  130711  13,533  GEM
	 * </pre>
	 * So it is faster WITHOUT hash-suppression, ie hash-suppression costs more
	 * than it saves, so I comment-it-out for now, for future removal. The good
	 * news is that hash-suppression misses 0 hints, despite collision problem.
	 * So this whole cluster____ of complexity turned out to be a waste of time
	 * but you never know if you never try. Maybe hash-suppression can be done
	 * faster, but I know not how.
	 * <pre>
	 * 2021-11-30 09:44 "fast HS" does it all inline instead of call Idx.hash64
	 * repeatedly, even though it is ludicrously complex, and saves just 114ms.
	 * 2021-11-30 11:37 LongHashSet is slower, so revert to HashMap.
	 * </pre>
	 *
	 * @param c the color: GREEN or BLUE
	 * @return hash64 of colors[c][*]
	 */
	private static long hash(final Idx[] thisColor, final int[] colorValues) {
		Idx t; // temp Idx pointer to thisColor[v]
		long result = 0L; // the hashCode
		for ( int v : colorValues ) {
			// This must be done in one line, for speed. Converting each int
			// into a long makes it hard to follow. Suck it up princess. The
			// technique is taken from Idx.hash64, which is now commented-out.
			t = thisColor[v];
			// 37 is 64-27, so b occupies high-order bits of result
			result |= ( ((long)t.m1<<37) ^ t.m0 );
		}
		return result;
	}

	/**
	 * Accumulate (or) the indices of all $values of this $color into $result.
	 *
	 * @param c the color: GREEN or BLUE
	 * @param values the values in this color
	 * @param result the idx to build
	 * @return total number of indices in painted[c]
	 */
	private static int countAll(final Idx[] color, final int[] values, final Idx result) {
		result.clear();
		for ( int v : values )
			result.or(color[v]);
		return result.size();
	}

	// append PROMOTIONS_LABEL to steps; and return false, for first.
	private boolean promotionsLabel() {
		steps.append(NL).append(PROMOTIONS_LABEL).append(NL);
		return false;
	}

	/**
	 * Promote all possible ons (Supers) to full colors (Pars).
	 * <pre>
	 * Promotions
	 * 1. When all but one cell mates or siblings of a value are "off"ed
	 *    then the survivor is an "on" of that color.
	 *    a) The only un-off-ed value in a cell is an "on".
	 *    b) The only un-off-ed place for a value in a region is an "on".
	 *    Both of these rules are ALL in one color.
	 * 2. An "on" seeing an opposite color sibling or cell mate gets painted.
	 *    a) If an "on" sees opposite color (same value) then paint it.
	 *    b) If an "on" has an opposite colored cell-mate (any value) paint it.
	 * KRC Shoot-back rule
	 * 3. If an "on" leaves any effected box (2 left/right, 2 above/below) with
	 *    ONE v that is this color, then paint it.
	 * </pre>
	 * @return any
	 */
	private boolean paintPromotions() {
		long m0; int m1, o, i, v;
		boolean any = false // presume that no cell-values will be promoted
			  , first = true; // is this the first promotion
		// 1a. The only un-off-ed value in a cell is an "on".
		int c = GREEN;
		do {
			i = 0;
			do {
				if ( sizes[i]-1 == VSIZE[offedValues[c][i]] ) {
					v = VFIRST[maybes[i] & ~offedValues[c][i]];
					// pre-check it is not already painted for speed
					if ( !colors[c][v].has(i)
					  // and it is not already an On
					  && !ons[c][v].has(i)
					) {
						if ( wantWhy ) {
							if(first) first = promotionsLabel();
							steps.append("last potential value").append(PRODUCES)
							.append(colorPlus(c, i, v))
							.append(NL);
						}
						ons[c][v].add(i);
						onsValues[c] |= VSHFT[v];
					}
				}
			} while (++i < GRID_SIZE);
		} while (++c < 2);
		// 1b. The only un-off-ed place for a value in a region is an "on".
		c = GREEN;
		do {
			for ( int v1 : VALUESES[colorValues[c]] ) {
				for ( ARegion r : regions ) {
//					if ( tmp1.setAndAny(offs[c][v1], r.idx)
//					  && tmp2.setAndNot(r.idxs[v1], tmp1).size() == 1
					if ( ( (m0=offs[c][v1].m0 & r.idx.m0)
						 | (m1=offs[c][v1].m1 & r.idx.m1) ) > 0L
					  && (i=only(r.idxs[v1].m0 & ~m0
								,r.idxs[v1].m1 & ~m1)) > -1
					  // and not already painted for speed
					  && !colors[c][v1].has(i)
					  // and not already an on for one why
					  && !ons[c][v1].has(i)
					) {
						if ( wantWhy ) {
							if(first) first = promotionsLabel();
							steps.append("last place in ").append(r.label)
							.append(PRODUCES).append(colorPlus(c, i, v1))
							.append(NL);
						}
						ons[c][v1].add(i);
						onsValues[c] |= VSHFT[v1];
					}
				}
			}
		} while (++c < 2);
		// 2a. If an on sees (same v, OPPOSITE color) a colors then paint it.
		c = GREEN;
		do {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[onsValues[c]] ) {
				// nb: Idx's Iterator<Integer> is slow, but every time I fix it
				// ChainerDynamic goes slow. I know not why. So just leave it!
				// Now I use toArrayCached: ~85% hit rate, so GEM ~20% faster.
				// This has not caused slow ChainerDynamic, yet. So I build!
				for ( int ii : ons[c][v1].indicesCached() ) {
					if ( tmp1.setAndAny(BUDDIES[ii], colors[o][v1])
					  // pre-check it is not already painted for speed
					  && !colors[c][v1].has(ii) ) {
						if ( wantWhy ) {
							if(first) first = promotionsLabel();
							why.append(SEES_SP + A).append(COLOR_NAMES[o])
							.append(PRODUCES).append(colorMinus(c, ii, v1))
							.append(NL);
						}
						any |= paint(ii, v1, c, T);
					}
				}
			}
		} while (++c < 2);
		// 2b. If an on has an OPPOSITE colored cell-mate (any other value)
		//     then paint it.
		countAll(colors[GREEN], VALUESES[colorValues[GREEN]], painted[GREEN]);
		countAll(colors[BLUE], VALUESES[colorValues[BLUE]], painted[BLUE]);
		c = GREEN;
		do {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[colorValues[c]] ) {
				if ( tmp2.setAndAny(ons[c][v1], painted[o]) ) {
					for ( int ii : tmp2 ) {
						// kinky way to do other color has another value:
						// if cell-in-other-color (above) and
						// not other-color-this-value.has(cell)
						if ( !colors[o][v1].has(ii)
						  // pre-check it is not already painted for speed
						  && !colors[c][v1].has(ii) ) {
							if ( wantWhy ) {
								assert why != null;
								if(first) first = promotionsLabel();
								why.append(HAS + A).append(COLOR_NAMES[o])
								.append(PRODUCES).append(colorMinus(c, ii, v1))
								.append(NL);
							}
							any |= paint(ii, v1, c, T);
						}
					}
				}
			}
		} while (++c < 2);
		// ShootBack!
		// if on.buds leave ONE v in any box that is dis color den paint da on.
		// WTF: paintMonoBoxs finds Ons that this does not! Why? So use both.
		// WEIRD: Start from the suspect On, and just shoot back.
		if ( PROMOTIONS_SHOOTS_BACK ) {
			// foreach color: GREEN, BLUE
			c = GREEN;
			do {
				// foreach value which has an On of this color
				for ( int v1 : VALUESES[onsValues[c]] ) {
					// foreach indice of the "suspect" On (to be painted)
					for ( int ii : ons[c][v1].indicesCached() ) {
						// foreach index of the possible source box
						for ( int ri : INDEXES[EFFECTED_BOXS[BOX_OF[ii]]] ) {
							// if ons buddies leave ONE v in the source box
							if ( tmp1.setAndNotAny(regions[ri].idxs[v1], BUDDIES[ii])
							  && tmp1.size() == 1
							  // and that source box v is this color
							  && tmp1.intersects(colors[c][v1])
							  // pre-check it is not already painted for speed.
							  // We need to check in here (not pre-check) incase
							  // it is painted in this loop, otherwise we hit the
							  // assert in paint in 63#top1465.d5.mt. sigh.
							  && !colors[c][v1].has(ii)
							) {
								if ( wantWhy ) { //shootBack at source Cell
									assert why != null;
									if(first) first = promotionsLabel();
									why.append("shoot back at ").append(CELL_IDS[tmp1.poll()])
									.append(PRODUCES).append(colorMinus(c, ii, v1))
									.append(NL);
								}
								any |= paint(ii, v1, c, T);
								break; // first box only
							}
						}
					}
				}
			} while (++c < 2);
		}
		// clean-up any hangover ons that have been painted (should be a no-op)
		c = GREEN;
		do {
			for ( int v1 : VALUESES[onsValues[c]] ) {
				// If you are seeing this you need to work-out if:
				// (a) an On was created when it is already colored; or
				// (b) a cell was painted without removing the existing On?
				// This is no biggy in prod (its handled) but it is unclean!
				// To be clear:
				// (a) if cell-value is already painted DO NOT create an "On".
				//     that is: if colors[c][v].has(indice) DO NOT ons...
				// (b) paint removes any existing "On", so that colors and ons
				//     remain disjunct sets, for simplicity.
				// nb: J1.8 bug in "assert clause : msg" causes msg to be
				// evalutated when clause is false (costly), so I roll my own.
				if ( Run.ASSERTS_ENABLED && Idx.overlaps(ons[c][v1], colors[c][v1]) )
					throw new AssertionError("unclean: "+Idx.ofAnd(ons[c][v1], colors[c][v1]));
				ons[c][v1].andNot(colors[c][v1]);
			}
		} while (++c < 2);
		return any;
	}

	/**
	 * Does either color contain a contradiction. First of:<pre>
	 * * If two of any combo of greens and green-ons in a cell then blue true.
	 * * If any combo of greens and green-ons (same value) in region then blue.
	 * * RARE: If green offs all of a cells values then blue is true.
	 * * RARE: If all vs in a region see a green v then blue is true.
	 * </pre>
	 * The use of ons in contradictions is worthy of some explanation. The ons
	 * are true if the color is true, right? Well that means that if any of the
	 * ons are invalid then the whole color is false; so we can safely use the
	 * ons in contradictions (but NOT in confirmations).
	 * <p>
	 * Did not work: I tried if cell-value in both colors/ons and offs then the
	 * other color is true. Finds 0 top1465 after first two rules above, and
	 * finds less than half of the above two rules combined, so forget it.
	 *
	 * @return The subtype of hint to be created:<pre>
	 * 0: meaning none.
	 * 2: 2+ green values/ons in cell (261)
	 * 3: 2+ green vs/ons in region (168)
	 * 4: green offs all of a cells values (9)
	 * 5: All vs in region see a green v (2)
	 * NOTE: 6 and 7 are confirmations (not mine)
	 * </pre>
	 */
	private int contradictions() {
		Idx t1, t2, color[], on[], colorPlusOns[];
		int indice, c, cands, i;
		long idxM0; int idxM1; // an exploded Idx (of what I know not)
		// presume neither color is good (an invalid value)
		hintColor = -1;
		// foreach color in GREEN, BLUE
		c = 0;
		do {
			color = colors[c];
			on = ons[c];
			colorPlusOns = colorsPlusOns[c];
			// repopulate colorsOns[c]
			cands = colorValues[c] | onsValues[c];
			for ( int v : VALUESES[cands] )
				colorPlusOns[v].setOr(color[v], on[v]);
			// If any combo of greens and ons (different values) in cell
			// then blue is true. (265 in top1465)
			for ( int v1 : VALUESES[cands] ) {
				// foreach color/on value EXCEPT v1
				for ( int v2 : VALUESES[cands & ~VSHFT[v1]] ) {
					if ( tmp1.setAndAny(colorPlusOns[v1], colorPlusOns[v2]) ) {
						// report first only (confusing if we show all)
						indice = tmp1.peek();
						hintCause = Idx.of(indice);
						hintColor = OPPOSITE[c];
						if ( wantWhy ) {
							steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
							.append(KON).append(CELL_IDS[indice]).append(KOFF).append(HAS)
							.append(CON[c]).append(COLORS[c]).append(SP).append(v1).append(AND).append(v2).append(COFF[c])
							.append(COMMA_SO).append(COLOR_NAMES[hintColor]).append(IS_TRUE)
							.append(NL);
						}
						return SUBTYPE_2;
					}
				}
			}
			// If any combo of greens and ons (same value) in region
			// then blue is true. (170 in top1465)
			for ( int v : VALUESES[cands] ) {
				t1 = colorPlusOns[v];
				for ( ARegion r : regions ) {
					if ( r.numPlaces[v] > 1
					  && ( (idxM0 = t1.m0 & (t2=r.idx).m0)
						 | (idxM1 = t1.m1 & t2.m1) ) > 0L
					  && Long.bitCount(idxM0) + Integer.bitCount(idxM1) > 1
					) {
						hintCause = new Idx(idxM0,idxM1);
						hintColor = OPPOSITE[c];
						if ( wantWhy ) {
							steps.append(NL)
							.append(CONTRADICTION_LABEL).append(NL)
							.append(KON).append(r.label).append(KOFF)
							.append(HAS).append(MULTIPLE)
							.append(CON[c]).append(v).append("/+s").append(COFF[c])
							.append(COMMA_SO).append(COLOR_NAMES[hintColor]).append(IS_TRUE)
							.append(NL);
						}
						return SUBTYPE_3;
					}
				}
			}
			// If green offs all of a cells values then blue is true.
			// RARE: 10 in top1465
			offCounts(offs[c], offCounts);
			for ( i=0; i<GRID_SIZE; ++i ) {
				if ( sizes[i]>0 && sizes[i]==offCounts[i] ) {
					hintCause = Idx.of(i);
					hintColor = OPPOSITE[c];
					if ( wantWhy ) {
						steps.append(NL)
						.append(CONTRADICTION_LABEL).append(NL)
						.append(KON).append(CELL_IDS[i]).append(KOFF).append(" all values are ")
						.append(CON[c]).append(COLORS[c]).append(SP).append(MINUS).append(COFF[c])
						.append(COMMA_SO).append(COLOR_NAMES[hintColor]).append(IS_TRUE)
						.append(NL);
					}
					return SUBTYPE_4;
				}
			}
			// If all vs in a region see a green v then blue is true.
			// RARE: 0 in top1465 (was 3 prior to 2023-10-24, hence disabled)
			// SLOW thanks to "excessive" buds calls.
			if ( DO_CONTRADICTION_TYPE_5 ) {
				for ( int v : VALUESES[colorValues[c]] ) {
					tmp2.clear();
					for ( final IntQueue q=color[v].indices(); (i=q.poll())>QEMPTY; )
						tmp2.or(BUDDIES[i]);
					tmp2.andNot(color[v]);
					for ( ARegion r : regions ) {
						if ( r.numPlaces[v] > 1
						  && tmp1.setAnd(tmp2, r.idx).equals(r.idxs[v])
						) {
							hintRegion = r;
							hintCause = new Idx(tmp1);
							hintColor = OPPOSITE[c];
							if ( wantWhy ) {
								steps.append(NL)
								.append(CONTRADICTION_LABEL).append(NL)
								.append(KON).append(r.label).append(KOFF)
								.append(ALL).append(v).append(APOSTROPHE_S).append(SEE)
								.append(CON[c]).append(v).append(APOSTROPHE_S).append(COFF[c])
								.append(COMMA_SO).append(COLOR_NAMES[hintColor]).append(IS_TRUE)
								.append(NL);
							}
							return SUBTYPE_5;
						}
					}
				}
			}
		} while (++c < 2);
		return SUBTYPE_NONE; // 0 meaning that no contradiction was found
	}

	/**
	 * Do a post-eliminations confirmation (opposite of contradiction) search
	 * to promote an elimination hint to a "big" hint: setting all cell values
	 * in a color (and the ons).
	 * <pre>
	 * a) If all bar one value is eliminated from a cell, and the remaining
	 *    cell-value is colored then that whole color is true.
	 * b) when all but one place is eliminated from a region and the surviving
	 *    cell-value is colored then that whole color is true.
	 * </pre>
	 * NOTE: Do not use ons here! This is why ons are separate from colors.
	 * If a color is true then all ons are also true, but NOT conversely!
	 * <p>
	 * I query the redPots from eliminations, and HashMap look-ups are slow, so
	 * I am slow. The saving grace is I run only when an eliminations hint is
	 * found, ie not often enough to worry about really. The new
	 * {@code redPots.toIdxs(REDS)} eliminates the repeated HashMap gets.
	 * <p>
	 * Any contradiction or confirmation hint is now the last hint GEM finds,
	 * regardless of what the passed accu says. A confirmation was usually the
	 * last hint found, but now that is always: after we add a confirmation to
	 * accu we throw a STOP_EXCEPTION to stop search. Smelly, but works.
	 * <p>
	 * These are as rare as rocking horse s__t: 2 'a' and 0 'b' in top1465.
	 *
	 * @return 0: meaning none<br>
	 * 4: meaning Type A<br>
	 * 5: meaning Type B<br>
	 * This value is up-shifted to differentiate it from existing subtypes.
	 */
	private int confirmations() {
		Integer values;
		// a) all bar one value is actually eliminated from a cell and that
		//    cell-value is colored then ALL of this color is true
		// RARE: 2 in top165
		int c = GREEN;
		do
			for ( int v : VALUESES[colorValues[c]] )
				for ( int i : colors[c][v] )
					if ( (values=redPots.get(i)) != null
					  && VSIZE[values] == sizes[i] - 1
					) {
						if ( wantWhy ) {
							steps.append(NL)
							.append(CONFIRMATION_LABEL).append(NL)
							.append(colorMinus(c, i, v))
							.append(" and all other values are eliminated")
							.append(COMMA_SO).append(COLOR_NAMES[c])
							.append(IS_ALL_TRUE).append(NL);
						}
						hintColor = c;
						return SUBTYPE_6; // continue on from existing subtypes
					}
		while (++c < 2);
		// b) if all bar one cell is actually eliminated from a region and that
		//    cell-value is colored then ALL of this color is true
		// RARE: 1 in top1465
		if ( DO_CONFIRMATIONS_HIDDEN_SINGLES ) {
			redPots.toIdxs(redIdxs); // repopulate REDS[1..9] for efficiency
			c = GREEN;
			do
				for ( int v : VALUESES[colorValues[c]] )
					// foreach indice of colored cell to examine
					for ( int i : colors[c][v] )
						// foreach of the 3 regions containing this cell
						for ( ARegion r : cells[i].regions )
							// if this region has reds
							if ( tmp2.setAndAny(redIdxs[v], r.idx)
							  // and the only non-red is cells[i]
							  && tmp3.setAndNot(r.idxs[v], tmp2).has(i)
							  && tmp3.size() == 1
							) {
								if ( wantWhy ) {
									steps.append(NL)
									.append(CONFIRMATION_LABEL).append(NL)
									.append(colorMinus(c, i, v))
									.append(" and all other ").append(v).append(APOSTROPHE_S).append(IN).append(r.label)
									.append(" are eliminated, so ").append(COLOR_NAMES[c])
									.append(IS_ALL_TRUE).append(NL);
								}
								this.hintColor = c;
								this.hintRegion = r;
								return SUBTYPE_7;
							}
			while (++c < 2);
		}
		return 0;
	}

	/**
	 * Create a new "big" GEMHintMulti to set cell values of a whole color.
	 *
	 * @param value the potential value to create hint for.
	 * @param subtype the subtype of the hint to create.
	 * @return a new GEMHintMulti, or null meaning the hint is rooted.
	 */
	private AHint createBigHint(final int value, final int subtype) {
		try {
			// nb: the reference is final, its contents is mutated.
			Pots sets = squishSetPots(hintColor); // cell-values to set
			addOns(sets);
			// bolt-on a turbo to solve faster
			if ( CFG.getBoolean("turbo", false) )
				sets = solveWithSingles(sets); // adds to sets
			final AHint hint = new GEMHintBig(grid, this, value
				, new Pots(redPots), subtype, hintCause, hintColor
				, wantWhy ? steps.toString() : null
				, sets, pots(GREEN), pots(BLUE), hintRegion
				, deepCopyColorsArray(ons), deepCopyColorsArray(offs));
			hintCause = null;
			hintRegion = null;
			return hint;
		} catch ( Pots.IToldHimWeveAlreadyGotOneException ex ) {
			// tried to set a cell twice, to two different values.
			// This should NEVER happen. Never say never.
			Log.teeln("WARN: "+Log.me()+": "+ex+" hence null hint!");
			return null;
		}
	}



	/**
	 * Attempt to solve the grid using NakedSingle and HiddenSingle,
	 * being seriously greedy.
	 *
	 * @param sets a pots of setCells to which I add cells that I set.
	 * @return result is a copy of sets with new set-cells added to it, because
	 *  you cant add to sets while you are iterating it.
	 */
	public Pots solveWithSingles(final Pots sets) {
		int i, v;
		// HintsApplicumulator.add apply's each hint when it is added.
		// @param isStringy=true for hint.toString's in the SummaryHint
		final HintsApplicumulator apcu = new HintsApplicumulator(false);
		// make the apcu remember cells it sets in a Pots
		apcu.setSetPots(new Pots());
		// we solve a copy of the given grid
		final Grid copy = new Grid(grid);
		// first apply sets to copy, and see if it solves
		final Pots result = new Pots(sets);
		for ( Map.Entry<Integer, Integer> e : sets.entrySet() ) {
			// nb: the AUTOSOLVE parameter is hardcoded true!
			copy.cells[e.getKey()].set(VFIRST[e.getValue()], 0, true, null);
			for ( i=0; i<GRID_SIZE; ++i ) {
				if ( (v=copy.cells[i].value)>0 && grid.cells[i].value==0 )
					result.put(i, VSHFT[v]);
			}
			if(copy.numSet > 80) return result; // solved!
		}
		// it didn't solve automatically so search again
		final NakedSingle nakedSingle = new NakedSingle();
		final HiddenSingle hiddenSingle = new HiddenSingle();
		apcu.grid = copy; // to tell Cell.apply which grid
		int ttlElims = 0;
		do {
			apcu.numElims = 0;
			if ( nakedSingle.findHints(copy, apcu)
			   | hiddenSingle.findHints(copy, apcu) ) {
				ttlElims += apcu.numElims;
			}
		} while ( apcu.numElims > 0 );
		if ( ttlElims > 0 )
			result.addAll(apcu.getSetPots());
		return result;
	}

	/**
	 * Add any ons to the given setPots, which is populated from colors.
	 */
	private void addOns(final Pots setPots) {
		Integer values;
		for ( int value=1; value<VALUE_CEILING; ++value ) {
			if ( ons[hintColor][value].any() ) {
				for ( int indice : ons[hintColor][value] ) {
					if ( (values=setPots.get(indice)) == null ) {
						setPots.put(indice, VSHFT[value]);
					} else {
						// colors MUST contain only one value
						assert VSIZE[values] == 1;
						// if colors-value != on-value then throw
						if ( (values & VSHFT[value]) > 0 )
							throw new Pots.IToldHimWeveAlreadyGotOneException("Bad on: "+CELL_IDS[indice]+" colors:"+MAYBES_STR[values]+" != ons:"+value);
						// if on-value==colors-value then just ignore on.
						// This should never happen but if it does, meh!
					}
				}
			}
		}
	}

	/**
	 * Create a new Pots from the given colors by placing only one value into
	 * each cell.
	 *
	 * @param c index of the good color (GREEN=0 or BLUE=1)
	 * @return a new Pots of all values in this color. There must be one value
	 *  per cell in colors[c], else I throw IllegalStateException meaning that
	 *  colors[c] is not a valid setPots, so this hint "has gone bad".
	 * @throws Pots.IToldHimWeveAlreadyGotOneException when second value added
	 *  to a cell.
	 */
	private Pots squishSetPots(final int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		int i;
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int value : VALUESES[colorValues[c]] )
			for ( final IntQueue q=thisColor[value].indices(); (i=q.poll())>QEMPTY; )
				result.insertOnly(i, VSHFT[value]); // throws IToldHimWeveAlreadyGotOneException
		return result;
	}

	/**
	 * Do the eliminations (mostly as per Medusa3D).
	 * <pre>
	 * Step 5.2: Eliminate if v sees both colors:
	 * AS PER Extended Coloring (XColoring)
	 * (a) Both colors appear in a single tri+valued cell.
	 * (b) An uncolored candidate X can see two differently colored X.
	 * AND NEW IN Medusa3D
	 * (c) An uncolored candidate sees a colored cell (eg green), but this
	 *     cell has another candidate with the opposite color (eg blue).
	 * AND NEW IN GEM (Graded Equivalence Marks):
	 * (d) Both colors have Sub marked the same cell-value so eliminate it.
	 *     Bring out the Gimp!
	 *
	 * There can be MORE THAN ONE type of elimination, so the subtype return
	 * value is now a bitset:
	 * * 1: Both colors in cell
	 * * 2: X sees both colors
	 * * 4: has green and sees blue
	 * * 8: Both colors Sub marked
	 * + The "steps" field provides the user with paint steps, and conclusions.
	 * </pre>
	 */
	private int eliminations() {
		int values[] // VALUESES[paintedValues]
		, green // green value/s bitset
		, blue // blue value/s bitset
		, pinkos // bitset of "all other values" to eliminate
		, c // color: the current color: GREEN or BLUE
		, o // opposite: the other color: BLUE or GREEN
		;
		int subtype = 0; // presume none
		boolean first = true; // is this the first elimination
		// (a) Both colors in one cell eliminates all other values.
		for ( int indice : tmp1.setAnd(painted[GREEN], painted[BLUE]) ) {
			if ( sizes[indice] > 2
			  // get a bitset of all values of cells[i] that are green and blue.
			  // There may be none, never multiple (contradictions pre-tested)
			  // We need 1 green value and 1 blue value, to strip from pinks.
			  // NOTE: VSIZE[0] == 0.
			  && VSIZE[green=values(GREEN, indice)] == 1
			  && VSIZE[blue=values(BLUE, indice)] == 1
			  // ensure that g and b are not equal (should NEVER be equal)
			  && green != blue
			  // pinkos is a bitset of "all other values" to be eliminated.
			  && (pinkos=maybes[indice] & ~green & ~blue) > 0
			  // ignore already-justified eliminations (should not happen here)
			  && redPots.upsert(indice, pinkos, DUMMY)
			) {
				if ( wantWhy ) {
					if ( first ) {
						first = false;
						steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
					}
					steps.append(CELL_IDS[indice])
					.append(HAS_BOTH).append(GON).append(MAYBES[green]).append(GOFF)
					.append(AND).append(BON).append(MAYBES[blue]).append(BOFF)
					.append(COMMA).append(ELIMINATING)
					.append(RON).append(ALL_OTHER_VALUES).append(ROFF).append(PERIOD)
					.append(NL);
				}
				subtype |= 1;
			}
		}
		// cache unpainted indices (read twice)
		int[][] unpainted = new int[VALUE_CEILING][];
		for ( int value : values=VALUESES[paintedValues] )
			if ( tmp2.setAndNotAny(idxs[value], tmp1.setOr(colors[GREEN][value], colors[BLUE][value])) )
				unpainted[value] = tmp2.toArrayNew();
		// (b) An uncolored v sees both colored vs.
		for ( int value : values ) {
			if ( unpainted[value] != null ) {
				for ( int indice : unpainted[value] ) {
					// if indice sees both green and blue of this value
					if ( tmp3.setAndAny(BUDDIES[indice], colors[GREEN][value])
					  && tmp4.setAndAny(BUDDIES[indice], colors[BLUE][value])
					  // and this elimination is new to us
					  && redPots.upsert(indice, VSHFT[value], DUMMY)
					) {
						if ( wantWhy ) {
							final int gc = closest(tmp3, COL_OF[indice], ROW_OF[indice]); // green cell seen
							final int bc = closest(tmp4, COL_OF[indice], ROW_OF[indice]); // blue cell seen
							if ( first ) {
								first = false;
								steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
							}
							steps.append(CELL_IDS[indice]).append(MINUS).append(value)
							.append(SEES_BOTH).append(colorMinus(GREEN, gc, value))
							.append(AND).append(colorMinus(BLUE, bc, value))
							.append(COMMA_SO).append(CELL_IDS[indice]).append(CANT_BE)
							.append(RON).append(value).append(ROFF).append(PERIOD)
							.append(NL);
						}
						subtype |= 2;
					}
				}
			}
		}
		// (c) v sees green, and has some other blue value.
		for ( int value : values ) {
			if ( unpainted[value] != null ) {
				for ( int indice : unpainted[value] ) {
					c = 0;
					do {
						// if cells[indice] sees a this-color value
						if ( tmp3.setAndAny(BUDDIES[indice], colors[c][value])
						  // and opposite color (any value) contains indice
						  && painted[OPPOSITE[c]].has(indice)
						  // and this elimination is new to us
						  && redPots.upsert(indice, VSHFT[value], DUMMY)
						) {
							if ( wantWhy ) {
								o = OPPOSITE[c];
								final int sib = closest(tmp3, COL_OF[indice], ROW_OF[indice]); // the seen cell
								final int otherValue = firstValue(o, indice);
								if ( first ) {
									first = false;
									steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
								}
								steps.append(CELL_IDS[indice])
								.append(HAS).append(color(o, otherValue))
								.append(" and sees ").append(colorMinus(c, sib, value))
								.append(COMMA_SO).append(CELL_IDS[indice]).append(CANT_BE)
								.append(RON).append(value).append(ROFF).append(PERIOD)
								.append(NL);
							}
							subtype |= 4;
						}
					} while (++c < 2);
				}
			}
		}
		unpainted = null;
		// (d) offed by both colors.
		for ( int value=1; value<VALUE_CEILING; ++value ) {
			if ( tmp1.setAndAny(offs[GREEN][value], offs[BLUE][value]) ) {
				for ( int indice : tmp1 ) {
					// ignore already-justified eliminations
					if ( redPots.upsert(indice, VSHFT[value], DUMMY) ) {
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
							}
							steps.append(CELL_IDS[indice]).append(MINUS).append(value)
							.append(" is eliminated in both ")
							.append(COLOR_NAMES[GREEN]).append(AND).append(COLOR_NAMES[BLUE]).append(COMMA_SO)
							.append(CELL_IDS[indice]).append(CANT_BE).append(RON).append(value).append(ROFF)
							.append(PERIOD)
							.append(NL);
						}
						subtype |= 8;
					}
				}
			}
		}
		return subtype;
	}

	/**
	 * Get a bitset of the values that are painted color-c at indice-i.
	 *
	 * @param c the color index: GREEN, or BLUE
	 * @param indice the cell indice to check
	 * @return a bitset of painted values
	 */
	private int values(final int c, final int indice) {
		final Idx[] thisColor = colors[c];
		int result = 0;  // NOTE: VSIZE[0] == 0
		for ( int v : VALUESES[colorValues[c]] )
			if ( thisColor[v].has(indice) )
				result |= VSHFT[v];
		return result;
	}

	/**
	 * Return the first value in colors[o][*] which contains(ii).
	 * <p>
	 * NOTE: We cannot use FIRST_VALUE because not-found must return 0.
	 *
	 * @param o the opposite color: BLUE or GREEN
	 * @param ii the cell index
	 * @return the first value of ii that is painted the opposite color
	 */
	private int firstValue(final int o, final int ii) {
		for ( int v : VALUESES[colorValues[o]] )
			if ( colors[o][v].has(ii) )
				return v;
		return 0; // NOTE: VSIZE[0] == 0
	}

	/**
	 * Return the cell in idx that is physically closest to the target cell.
	 * <p>
	 * NOTE: This method is just a "nice to have". If the target cell sees ANY
	 * cell-of-this-color then the logic holds up; it is just nice to show the
	 * user the closest-cell-of-this-color; that's all. closest is called ONLY
	 * when eliminations is producing a hint, so efficiency is NOT a problem!
	 *
	 * @param idx {@code tmp3.setAndAny(BUDDIES[ii], colors[c][v])} the buddies
	 * of the target cell which are painted this color.
	 * @param x {@code cc.x} the col of target cell
	 * @param y {@code cc.y} the row of target cell
	 * @return the closest cell (or null if I am rooted)
	 */
	private int closest(final Idx idx, final int x, final int y) {
		int dx, dy, distance;
		int closest = -1; // the indice of the closest cell = NOT_FOUND
		int minDistance = Integer.MAX_VALUE; // distance from closest cell
		for ( int indice : idx ) { // Iterator fast enough here (hinting)
			if ( COL_OF[indice] == x ) {
				distance = ROW_OF[indice] - y;
				if ( distance < 0 )
					distance = -distance;
			} else if ( ROW_OF[indice] == y ) {
				distance = COL_OF[indice] - x;
				if ( distance < 0 )
					distance = -distance;
			} else { // pythagoras
				dx = COL_OF[indice] - x;
				if ( dx < 0 )
					dx = -dx;
				dy = ROW_OF[indice] - y;
				if ( dy < 0 )
					dy = -dy;
				distance = (int)Math.sqrt((double)(dx*dx + dy*dy));
			}
			if ( distance < minDistance ) {
				minDistance = distance;
				closest = indice;
			}
		}
		return closest;
	}

	/**
	 * Create a new "normal" GEMHint containing eliminations.
	 * <ul>
	 * <li>the subtype is now a bitset! 1=Type1, 2=Type2, 4=Type3. sigh.
	 * <li>just keep the subtype param, even though it is no longer used because
	 * the steps String is now populated up in eliminate, which is MUCH better.
	 * </ul>
	 *
	 * @param value the value
	 * @param subtype no longer used!
	 * @return a new GEMHint (for eliminations), or possibly a "big" hint
	 *  (upgraded) if CONSEQUENT_SINGLES_TOO and the eliminations leave a cell
	 *  with one potential value, or a region with one place for a value.
	 */
	private AHint createHint(final int value, final int subtype) {
		final Idx[] colorSet = null;
		final Pots reds = new Pots(redPots);
		final AHint hint = new GEMHint(grid, this, value, reds, pots(GREEN)
				, pots(BLUE), colorSet
				, wantWhy ? steps.toString() : null
				, null, deepCopyColorsArray(ons), deepCopyColorsArray(offs));
		// copy-off to reds (above) then clear the redPots field.
		redPots.clear();
		return hint;
	}

	/**
	 * Build a new Pots of all cells, all values in the given color.
	 *
	 * @param c the color: GREEN or BLUE
	 * @return a new Pots of Cell=>values in all values of colors[color].
	 */
	private Pots pots(final int c) {
		final Idx[] thisColor = colors[c];
		final Pots result = new Pots();
		for ( int v : VALUESES[colorValues[c]] ) {
			result.upsertAll(thisColor[v], maybes, v);
		}
		return result;
	}

	private StringBuilder sb() {
		sb.setLength(0);
		return sb;
	}

	/**
	 * Helper method averts AIOOBE when coloring a string, for use when we are
	 * not sure that the goodColor field has been set. Note that I am used only
	 * when goodColor could be -1 (not found) coz creating a temp-buffer for my
	 * $s argument is MUCH slower than appending to an existing one; so use me
	 * only when creating a hint, not in the why-string of the paint routines.
	 */
	private StringBuilder color(final int c, final Object s) {
		if ( c>-1 && c<2 ) // ignore -1 (goodColor not found)
			return sb().append(CON[c]).append(s).append(COFF[c]);
		return sb();
	}

	// c is the color index: GREEN or BLUE
	private StringBuilder colorMinus(final int c, final int indice, final int value) {
		// 3 + 2 + 1 + 1 + 5 = 12 + 1 extra just in case
		return sb().append(CON[c]).append(CELL_IDS[indice]).append(MINUS).append(value).append(COFF[c]);
	}

	// c is the color index: GREEN or BLUE
	private StringBuilder colorPlus(final int c, final int indice, final int value) {
		// 1 + 3 + 2 + 1 + 1 + 5 = 11 + 1 extra just in case
		return sb().append(ON_MARKER).append(CON[c]).append(CELL_IDS[indice]).append(PLUS).append(value).append(COFF[c]);
	}

	/**
	 * ValueScore is the score for each value; only used in the startingValues
	 * method, to keep the value WITH it is score, so that we can sort the array
	 * by score descending and keep the value. The array is unsorted back to
	 * value order (including 0) at the start of each startingValues run.
	 */
	private static class ValueScore {
		public final int value;
		public int score;
		ValueScore(final int value) {
			this.value = value;
		}
		// debug only, not used in actual code
		@Override
		public String toString() {
			return ""+value+": "+score;
		}
	}

	/** A distinctive subtype of RuntimeException. */
	private static final class OverpaintException extends IllegalStateException {
		private static final long serialVersionUID = 720396129058L;
		public OverpaintException(final String msg) {
			super(msg);
		}
	}

//	/** A distinctive subtype of RuntimeException. */
//	private static final class BadPaintException extends IllegalStateException {
//		private static final long serialVersionUID = 11107670454L;
//		public BadPaintException(String msg) {
//			super(msg);
//		}
//	}

	/** A distinctive subtype of RuntimeException used by search stops find. */
	private static final class StopException extends RuntimeException {
		private static final long serialVersionUID = 68198734001267L;
	}

	/**
	 * A distinctive subtype of RuntimeException thrown by create[Big]Hint when
	 * a bad Elimination [or SetPot] is detected.
	 *
	 * This the more extreme way to handle it, for when you think you are on top
	 * of invalid hints. If you are getting invalid hints then comment out my
	 * throw an uncomment the "programmer friendly" handlers, but do not forget
	 * to put me back. Users should NEVER see "Invalid Eliminations" in a hint
	 * explanation, because it makes them distrust ALL of your software, not
	 * just the small part they should not trust, mainly coz they have not got a
	 * clue, and really do not want one; that is WHY they are using software to
	 * get nice fat hints. Do not scare the chooks! Gobble, gobble, gobble.
	 *
	 * NOTE that GEM is disabled (for this puzzle only) whenever this exception
	 * is thrown.
	 */
	private static class UncleanException extends RuntimeException {
		private static final long serialVersionUID = 64099692026011L;
		public UncleanException(final String msg) {
			super(msg);
		}
	}

}
