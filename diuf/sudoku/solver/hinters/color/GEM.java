/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2022 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

// Rule: .* your own s__t. Never .* import anything you can't modify, but you
// can .* import anything that you can modify to resolve any name-conflicts.
import diuf.sudoku.*;
import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;
import diuf.sudoku.Grid.*;
import diuf.sudoku.IntArrays.IALease;
import diuf.sudoku.solver.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.hinters.*;
import static diuf.sudoku.solver.hinters.Validator.*;
import static diuf.sudoku.solver.hinters.color.Words.*;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Log;
import static java.lang.Integer.bitCount;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * GEM (Graded Equivalence Marks) partially implements the Graded Equivalence
 * Marks specification at https://www.sudopedia.org/wiki (online).
 * <p>
 * See the text version of above article in ./#GEM.txt, but please note that I
 * can't make heads-nor-tails of it. It appears that I have a preformed picture
 * of what GEM should be that is at odds with the authors view of what GEM is,
 * such that I can't make his explanation make any bloody sense what-so-ever;
 * which should be perceived as an admission of a mental weakness on my part,
 * rather than a criticism of the publication. So in the end I gave-up trying
 * to implement the full GradedEquivalenceMarks and just did what makes sense
 * to me, which is pretty much 3DMedusa++, ergo 3DMedusa SimpleColoring with
 * cheese and pickles, so I add each trimming that makes sense to me, piece by
 * piece, but the net-result is that I'm unsure if I have implemented the whole
 * GEM spec, or there may be unresolved condiment/s remaining; like upgrading
 * your pool dunny into a shopping mall, and then going to K-Mart anyway. sigh.
 * Somebody who understands the spec may well get more eliminations by actually
 * implementing the spec. I'm too stoopid!
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
   if you're blue/green color-blind then change green to orange (or whatever)
   and republish, coz what works for you will probably work for everybody.

 NOTES:
 1. The Par mark is just the value in GREEN/BLUE, using existing colors.
 2. A Super/on mark is a grey value, then a "+" in GREEN/BLUE
 3. A Sub/off mark is a grey value, then a "-" in GREEN/BLUE or RED=both
 So, parity1 is GREEN, parity2 is BLUE. + is On, - is Off. Simples!

 KRC 2021-03-20 08:25 When I try the full Graded Equivalence Marks spec it
 goes to hell in a hand-basket. So either I'm too stoopid to understand the
 spec (probable) or the spec is wrong (improbable). I'll hear argument either
 way, but I'm pretty-pissed-off right now, so I declare the spec to be CRAP!
 Be warned that USE_PROMOTIONS=true produces and handles MANY invalid hints
 (see batch log-file when true). This s__t is unfun!

 KRC 2021-03-21 07:59 I think I found my mistake. I painted the last value of
 a cell or the last place in region; now I only "On" them, and leave it up to
 the later "sibling promoters" whether or not they get full color, which is
 what the original spec says. I just didn't listen.

 KRC 2021-03-22 09:58 I've found and fixed the last of my many mistakes, so I
 must eat my words: the specification is NOT crap, I'm just stupid!
 1. We need to mark ONLY valid offs, not just mark all offs and then clean-up
    afterwards, because the clean-up removes more than it should. So never
    mark an off that's colors/ons the opposite color.
 2. When we paint a cell-value we remove any off of the opposite color.
 3. Also I put Type 4's above Type 3's to get a Type 4 to test, but Type 3
    is simpler, so I reverted, so Type 4's are now non-existant, but can,
    I believe, exist, even when there's no type 3 in top1465.

 KRC 2021-03-24 08:45 I found "a few" extra hints from invalid ons which mean
 the other color must be true.

 KRC 2021-03-31 11:40 I built Mark 5 last night, and have spent this morning
 testing it, and tweaking a few things. I'll release it as Mark 6 tomorrow
 and that's it for GEM from me; I know not how to improve it any further.

 KRC 2021-06-07 Got the s__ts with gemSolve so ripped-out consequentSingles.
 </pre>
 *
 * @author Keith Corlett 2021-03-17
 */
public class GEM extends AHinter
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

	// Should paintPromotions use the "shoot back" rule.
	private static final boolean PROMOTIONS_SHOOTS_BACK = true;

	// Should confirmations look for Hidden Singles?
	// RARE as rocking horse s__t: 0 in top1465
	private static final boolean CONFIRMATIONS_HIDDEN_SINGLES = false;

	// Should hints be validated internally. Set me true when you ____ with the
	// promotions method, to check for invalid hints. I'm more forgiving than
	// the standard Validator approach.
	// NOTE: YOU WILL ALSO NEED TO UNCOMMENT MY CODE!!!
	// Call: cleanRedPots to throw/remove invalid eliminations, and
	// Call: cleanSetPots to throw/remove invalid cell-values-to-set.
	//
	// NOTE this validation is besides the "normal" Validator, which can't
	// just remove the little bastards to keep things going, to show you what's
	// ____ed-up, so that you can fix it. The problem with error handlers is
	// they PREVENT you from seeing what the problem is. So you disable them
	// and there's a car crash. Then you try a smash-up derby, and discover how
	// much you like rubber bumpers; so it's all good.
	private static final boolean CHECK_HINTS = false;
	// should we Log s__t when we CHECK_HINTS?
	private static final boolean CHECK_NOISE = true;

	/** GREEN and BLUE. */
	private static final int G=0, B=1;
	/** The second color: blue. */
	/** names of the colors, in an array to select from. */
	private static final String[] COLORS = {green, blue};
	/** The opposite color. */
	private static final int[] OPPOSITE = {B, G};
	/** colors ON HTML tags. */
	private static final String[] CON = {GON, BON};
	/** colors OFF HTML tags. */
	private static final String[] COFF = {GOFF, BOFF};
	/** colored color names. */
	private static final String[] CCOLORS = {GON+green+GOFF, BON+blue+BOFF};
//	/** opposite colored color names. */
//	private static final String[] OCOLORS = {BON+blue+BOFF, GON+green+GOFF};
	// The starting size of the steps StringBuilder. It'll grow if necessary.
	private static final int STEPS_SIZE = 4096;

	// contradiction retvals:
	private static final int SUBTYPE_2 = 2; // 2+ green v/ons's in cell
	private static final int SUBTYPE_3 = 3; // 2+ green v/ons's in region
	private static final int SUBTYPE_4 = 4; // All v's in region see a green v
	private static final int SUBTYPE_5 = 5; // All v's in cell are off'd
	// confirmation retvals:
	private static final int SUBTYPE_6 = 6; // Naked Single in colors
	private static final int SUBTYPE_7 = 7; // Hidden Single in colors

	/** thrown ONLY when a Type 2 hint has been found and added to accu. */
	private static final StopException STOP_EXCEPTION = new StopException();

	// this Object is every value in my hashes map, to use a Map as a Set.
	private static final Object PRESENT = new Object();

	/**
	 * Create the array of arrays which is a colors, or supers, subs.
	 * @param a
	 */
	private static void buildArray(final Idx[][] a) {
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<VALUE_CEILING; ++v )
				a[c][v] = new Idx();
	}

	/**
	 * Clear all the Idx's in the given array (both colors, all values).
	 */
	private static void clear(final Idx[][] a) {
		for ( int c=0; c<2; ++c )
			clear(a[c]);
	}

	/**
	 * Clear all the Idx's in the given array (all values).
	 */
	private static void clear(final Idx[] a) {
		for ( int v=1; v<VALUE_CEILING; ++v )
			a[v].clear();
	}

	/**
	 * Deep copy all the Idx's in the given array (both colors, all values).
	 */
	private static Idx[][] copy(final Idx[][] a) {
		final Idx[][] result = new Idx[2][VALUE_CEILING];
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<VALUE_CEILING; ++v )
				result[c][v] = new Idx(a[c][v]);
		return result;
	}

	/**
	 * Used to sort ValueScore's by score descending.
	 */
	private static final Comparator<ValueScore> SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

	/**
	 * Used to sort ValueScore's by value ascending (ie put them back in order).
	 */
	private static final Comparator<ValueScore> VALUE_ASCENDING =
			(ValueScore a, ValueScore b) -> a.value - b.value;

	/**
	 * promotions needs to know the values that have been eliminated from each
	 * cell in this color.
	 */
	private static final int[] OFF_VALS = new int[GRID_SIZE];

	/**
	 * for confirmations: an Idx of redPots (eliminations) by value 1..9
	 */
	private static final Idx[] REDS = new Idx[VALUE_CEILING];
	static {
		for ( int v=1; v<VALUE_CEILING; ++v )
			REDS[v] = new Idx();
	}

	/**
	 * Helper method to avert AIOOBE when coloring a string, for use when we're
	 * not sure that the goodColor field has been set. Note that I'm used only
	 * when goodColor could be -1 (not found) coz creating a temp-buffer for my
	 * 's' argument is MUCH slower than appending to an existing one; so use me
	 * only when creating a hint, not in the why-string of the paint routines.
	 */
	private static String color(final int c, final String s) {
		if ( c<0 || c>1 ) // especially -1 (goodColor not found)
			return s;
		return CON[c] + s + COFF[c];
	}

	/**
	 * Clear the given arrayOfArrays by nullifying each array, so that the GC
	 * can clean-up any arrays there-in that were created on-the-fly.
	 *
	 * @param as
	 */
	private static void clear(final int[][] as) {
		for ( int i=0,n=as.length; i<n; ++i )
			as[i] = null;
	}

	// ============================ instance stuff ============================

	// almost everything is a field, to save creating instances once per call,
	// and also to to save passing everything around. For some reason creating
	// an Idx is pretty slow: BFIIK, no arrays here.

	// The grid to process (a field so there's no need to pass around).
	private Grid grid;
	// The cells in grid to process
	private Cell[] gridCells;
	// The regions in grid to process
	private ARegion[] gridRegions;
	// The indices of each value in grid to process
	private Idx[] gridIdxs;
	// the accumulator
	private IAccumulator accu;
	// accu.isSingle()
	private boolean onlyOne;

	// do we steps+=why (an explanation) to go in the hint?
	private final boolean wantWhy;

	// There's 3 mark-ups in GEM:
	// 1. Par markers implemented as "colors",
	// 2. Super markers implemented as "ons", and
	// 3. Sub markers implemented as "offs".
	//
	// A color is a "parity": a set of "strong" bidirectional links, ie if A1
	// is 3 then D2 is 3, and conversely if D2 is 3 then A1 is 3.
	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// So colors holds the indices of GREEN 7's, or BLUE 2's, or whatever.
	private final Idx[][] colors = new Idx[2][VALUE_CEILING];
	// An "On" is a "weak" unidirectional link that sets a cell value in this
	// color. Eg: if C7 is 3 then A5 is also 3, which does NOT imply if A5 is 3
	// then C7 is also 3. Supers are used to set cells if this parity turns-out
	// to be true, but are (mostly) excluded from calculations.
	private final Idx[][] ons = new Idx[2][VALUE_CEILING];
	// colorsOns are colors or ons (ie both) for use in contradictions.
	private final Idx[][] colorsOns = new Idx[2][VALUE_CEILING];
	// An "Off" is a "weak" unidirectional link that removes a potential value
	// in this color. Eg: if C7 is 3 then I7 is not 3, which does NOT imply if
	// I7 is not 3 then C7 is 3, so ONE super is the end of the line: you can't
	// "build on it" as if it was an actual color. If both parities off a
	// candidate then that candidate is eliminated.
	private final Idx[][] offs = new Idx[2][VALUE_CEILING];
	// A bitset of those values which have an on
	private final int[] onsValues = new int[2];
	// Count the number of values off'd of each cell in the grid.
	private final int[] countOffs = new int[GRID_SIZE];

	// all values of each color; array to swap between colors.
	private final Idx[] painted = {new Idx(), new Idx()};
	// temporary Idxs: set, read, and forget!
	private final Idx tmp1 = new Idx()
					, tmp2 = new Idx()
					, tmp3 = new Idx()
					, tmp4 = new Idx();
	// other places for value in region, set ONLY in paint.
	private final Idx otherPlaces = new Idx();
	// a bitset of those values which have been painted either color
	private int paintedValues;
	// a bitset of the GREEN and BLUE values; array to swap between colors.
	private final int[] colorValues = new int[2];
	// the Cell=>Values to eliminate. A field to not build for 99% failure.
	private final Pots redPots = new Pots();
	// kickOffs is an Idx of the "starting cells" tried for each value, so that
	// when a cell-value that's conjugate in multiple regions is the initial
	// cell-value painted we can avoid repeat-checking the whole mess.
	private final Idx[] done = new Idx[VALUE_CEILING];
	{
		for ( int v=1; v<VALUE_CEILING; ++v )
			done[v] = new Idx();
	}

	/** A hashCode of each painted cell-set, all values, both colors. */
	private final HashMap<Long,Object> hashes = new HashMap<>(128, 0.75F);
//	private final LongHashSet hashes = new LongHashSet(); // 512 capacity: no growth!

	// the (presumably) "good" color to display "results" in the hint.
	private int goodColor;
	// Contradictions only: the cell/s which caused this hint.
	private Set<Cell> cause;
	// Type 2 only: the region containing the cells which caused this hint,
	// to be bordered in pink, to help the user find the bastard.
	private ARegion region;

	// a StringBuilder of the steps required to building the coloring-chains;
	// and also any conclusions of contradictions/eliminations/confirmations.
	// This is a persistant buffer for speed. Faster to re-use than grow new.
	private final StringBuilder steps = new StringBuilder(STEPS_SIZE);

	// score of values 1..9 (a field only to support reporting minScore, but it
	// is also faster (I think) to not recreate the array for every call)
	private final ValueScore[] scores = new ValueScore[VALUE_CEILING];

	/**
	 * The constructor.
	 */
	public GEM() {
		super(Tech.GEM);
		// build the colors, supers, and subs array-of-arrays of indices.
		buildArray(colors);
		buildArray(ons);
		buildArray(colorsOns);
		buildArray(offs);
		// We want explanation in GUI and testcases, just not in the batch.
		// nb: building all the "why" strings slows batch down by a MINUTE!
		this.wantWhy = Run.type != Run.Type.Batch;
		// populate the scores array ONCE (cleared thereafter)
		for ( int i=0; i<VALUE_CEILING; ++i ) // include zero to not upset sort
			scores[i] = new ValueScore(i);
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

	/**
	 * Called by solve to find first/all 3D Medusa hints, if any, in this grid.
	 *
	 * @param grid the Grid to search, containing a Sudoku puzzle
	 * @param accu the implementation of IAccumulator to add hint/s to. If the
	 * passed IAccumulator isSingle() then it's add method returns true to tell
	 * me (and all other hinters) to stop searching at the first hint; else the
	 * search continues for more hints.<br>
	 * <br>
	 * An ODD exception to this pattern is "3D Medusa Coloring Type 2:" hints.
	 * If/when ONE of them is found it typically means EVERY conjugate pair in
	 * the grid finds the SAME hint, so we ALWAYS exit-early whenever we find a
	 * Type 2, rather than waste time on duplicates. This is NOT correct, just
	 * expedient. Any non-duplicate hints are missed, even if they're "bigger".
	 * Sometimes I'm overly efficophilic. But ____ it, everybody has a vice.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints(final Grid grid, final IAccumulator accu) {
		// deadCat disables a hinter DURING solve (which does disabled only
		// BEFORE kick-off) so each hinter that's ever gone deadCat checks
		// isEnabled itself. I'm re-enabled by the prepare method, so I'm down
		// for this puzzle only. I'll get you next time Batman. Unfortunately
		// when you reload the current puzzle prepare is not called. I should
		// change the GUI to call it.
		if ( !isEnabled )
			return false;
		Cell cell;
		this.grid = grid;
		this.gridCells = grid.cells;
		this.gridRegions = grid.regions;
		this.gridIdxs = grid.idxs;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		// presume that no hint will be found
		boolean result = false;
		try {
			clear(done); // supress repeat search keys
			hashes.clear(); // supress repeat searches (colors)
			// Step 1: foreach value in 2-or-more conjugate relationships,
			//         order by num conjugates + num bivalues DESCENDING.
			// WARNING: startingValues filters to score >= 4 because that's
			// the lowest score that hints in top1465. THIS MAY BE WRONG!
			// The higher this value the faster GEM is per elimination;
			// upto ~7, where it starts missing too many eliminations.
			for ( int i=0,n=startingValues(); i<n; ++i ) { // repopulate scores
				final int v = scores[i].value;
				for ( ARegion r : gridRegions ) {
					// if v has two places in this region
					if ( ( r.ridx[v].size == 2
						// and we haven't already painted this cell-value.
						&& !done[v].has((cell=r.first(v)).i)
						// and the search finds something
						// nb: search may disable me!
						&& ( result |= search(cell, v) )
						// and we want onlyOne hint
						&& onlyOne )
					  || !isEnabled // or I'm now disabled!
					) {
						return result; // all done
					}
				}
			}
		// This indicates an invalid grid so now throws UnsolvableException.
		// To debug it change both throws to UncleanException instead!
		} catch ( UncleanException ex ) {
			// USE_PROMOTIONS -> create(Big)?Hint: bad elim or cell-value.
			Log.teeln("WARN: "+Log.me()+": "+ex.getClass().getSimpleName()+NL
					+ex.getMessage());
			// disable me for this puzzle (re-enabled by prepare)
			setIsEnabled(false); // use the setter!
		} catch ( StopException meh ) {
			// thrown after a contradiction/confirmation hint is added to accu
			result = true;
		} finally {
			this.grid = null;
			this.gridCells = null;
			this.gridRegions = null;
			this.accu = null;
			this.gridIdxs = null;
			this.cause = null;
			this.region = null;
		}
		return result;
	}

	/**
	 * To select the best starting value, pick a value that has 2 or more
	 * conjugate pairs, and is present in the most bivalue cells. These bivalue
	 * cells expand the coloring clusters beyond the single digit boundary.
	 * <p>
	 * From the spec: "Seed candidates should be chosen which are hoped will
	 * give the maximum return, and time spent considering the best choice is
	 * worthwhile, as a complete mark-up is time consuming."
	 * <p>
	 * The spec implies this can be done in simple coloring, but I'm ignoring
	 * it to keep my hinters independent of each other. This is "fast enough".
	 *
	 * @return the number of value with a score of 4 or more.
	 */
	private int startingValues() {
		int[] values; // cell maybes with 2+ conjugate pairs
		int v, cands, i, n;
		// reset scores: ordered by value ascending
		Arrays.sort(scores, VALUE_ASCENDING);
		for ( v=1; v<VALUE_CEILING; ++v )
			scores[v].clear();
		// count conjugate pairs for each value
		// also build-up a bitset of the values in 2+ conjugate pairs
		cands = 0;
		for ( v=1; v<VALUE_CEILING; ++v )
			for ( ARegion r : gridRegions )
				if ( r.ridx[v].size == 2
				  && ++scores[v].score > 1 )
					cands |= VSHFT[v];
		// foreach bivalue cell
		// increment the score of each value with 2+ conjugate pairs
		for ( Cell c : gridCells )
			if ( c.size == 2 ) {
				values = VALUESES[c.maybes & cands];
				for ( i=0,n=values.length; i<n; ++i )
					++scores[values[i]].score;
			}
		// order by score descending
		Arrays.sort(scores, SCORE_DESCENDING);
		// count the number of scores with a score of atleast 4. The minimum
		// possible minimum score is 3, but I use 4 coz it's top1465's minimum
		// that hints. There's no theoretical basis for 4, it's just what works
		// for me. I presume it'll work for all Sudoku puzzles but it may not!
		for ( n=0; n<VALUE_CEILING; ++n )
			if ( scores[n].score < 4 // see above comment
			  || scores[n].value == 0 ) // stop at value 0 (null terminator)
				break;
		return n;
	}

//KEEP: used to get minScore to report and set the minimum score (above)
//	// get the score for the given value
//	private int getScore(final int value) {
//		for ( int i=0; i<VALUE_CEILING; ++i )
//			if ( scores[i].value == value )
//				return scores[i].score;
//		return -1;
//	}

	/**
	 * search this region and value (a conjugate pair) for Medusa 3D Coloring
	 * hints.
	 *
	 * @param r the region to search
	 * @param v the value to search for
	 * @return were any hint/s found; noting only one Type 2 is ever found.
	 */
	private boolean search(final Cell cell, final int v) {
		AHint hint;
		Long hash;
		int subtype // sub-type of this hint
		  , mst; // multi-hint subtype
		final int n;
		// presume that no hint will be found
		boolean result = false;
		// clear the paint artifacts so each search is independant of previous.
		// done and hashes are the ONLY hangovers, by design.
		clear(colors);
		clear(ons);
		clear(offs);
		colorValues[G]=colorValues[B]=paintedValues = 0;
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
		// * If the candidate you're checking is part of one or
		//   more conjugate pairs containing any recently added
		//   candidates, expand your cluster by coloring the
		//   candidate you're checking with the opposite color
		//   from that of the recently added candidate.
		// * Continue coloring for each value until no more
		//   candidates can be added.
		//
		// paintMonoBoxs: colors/ons any Hidden Singles left in boxs.
		//
		// paintPromotions: last cell-value or place-in-region is an ons;
		// and then any ons with colors siblings/cell-mates are painted.
		try {
			// paint this cell-value, and it's consequences, ...
			paint(cell, v, G, true, "Let's assume that "+cell.id+MINUS+v
					+IS+CCOLORS[G]+NL);
			// on/paint consequences, and there consequences, ...
			while ( paintMonoBoxs() | paintPromotions() ) {
				// do nothing, just go again
			}

			// we search each value that was painted ONCE.
			for ( int v1 : VALUESES[paintedValues] )
				done[v1].or(colors[G][v1]).or(colors[B][v1]);

			// we search each distinct both-color-sets ONCE.
			// nb: these hashes are 64 bits, to reduce collisions.
			// nb: there's no distinction between green and blue in this hash
			hash = hash(colors[G], VALUESES[colorValues[G]])
				 | hash(colors[B], VALUESES[colorValues[B]]);
			// use addOnly!
			if ( hashes.putIfAbsent(hash, PRESENT) != null )
//			if ( hashes.add(hash) )
				return false;

			// my minimum colored cells is 3.
			// WARN: This limit is top1465 specific. It's probably too high for
			// some puzzles. There is no theoretical basis for this, it's just
			// what works for me. Remove this limit for correctness.
			if ( (n = countAll(colors[G], VALUESES[colorValues[G]], painted[G])
				    + countAll(colors[B], VALUESES[colorValues[B]], painted[B])) < 3 )
				return false; // too few cells painted

			// Step 5: Analyse the cluster.
			// 5.1 If either color contains a contradiction
			//     then the opposite color MUST be true.
			// nb: minContradiction=3 so there's no filter on it
			if ( (subtype=contradictions()) != 0
			  // presuming that the opposite color isn't rooted too (sigh)
			  && (hint=createBigHint(v, subtype)) != null
			  // validate the hint if GEM_VALIDATES
			  && validSetPots("Contradiction", hint)
			) {
				result = true;
//				if ( n < minContradiction )
//					minContradiction = n;
				accu.add(hint);
				hint = null;
				// contradiction is always the last hint I produce.
				throw STOP_EXCEPTION;
			}
			// nb: minElimination=3 so there's no filter on it
		    // don't look if contradictions bombed (it shouldn't, in prod)
			if ( subtype == 0
			  // 5.2 If any uncolored cell-value sees both colors
			  //     then it can be eliminated.
			  && (subtype=eliminations()) != 0
			) {
				// search eliminations for confirmation that color is true.
				hint = null;
				// minConfirmation=11
				if ( n > 10
				  // 5.3 promote "little" elimination hint to a big hint
				  && (mst=confirmations()) != 0
				  && (hint=createBigHint(v, mst)) != null
				) {
					if ( validSetPots("Confirmation", hint) ) {
						result = true;
//						if ( n < minConfirmation )
//							minConfirmation = n;
						accu.add(hint);
						hint = null;
						// confirmation is always the last hint I produce.
						throw STOP_EXCEPTION;
					} else {
						hint = null;
					}
				} else {
					hint = createHint(v, subtype);
					if ( hint!=null && !validEliminations(hint) )
						hint = null;
				}
				if ( hint != null ) {
					result = true;
//					if ( n < minElimination )
//						minElimination = n;
					if ( accu.add(hint) )
						return result;
					// upgraded is always the last hint I produce.
					if ( hint instanceof GEMHintBig )
						throw STOP_EXCEPTION;
				}
			}
		} catch ( OverpaintException ex ) {
			// from paint, paintMonoBoxs, or promotions.
			// A cell was painted both colors, ergo this code is wrong.
			// Don't get your knickers in a twist. It'd be nice if it didn't
			// happen, but it did, so it's handled, sort of. The thing to do
			// is ensure that cells are not overpainted. Care is required.
			Log.println("WARN: GEM: Overpaint: "+ex.getMessage());
			return false;
// AlsChain is fixed, apparently, though I still don't understand the problem.
//		} catch ( BadPaintException ex ) {
//			// disable AlsChain which is da root cause of overpaints (I think).
//			LogicalSolverFactory.get().disable(Tech.ALS_CHAIN_TECHS);
//			Log.teeln("WARN: GEM Overpaint: Disabled ALS_Chain: "+ex.getMessage());
//			return false;
		}
		return result;
	}

	/**
	 * Paint this cell this color, and all of it's consequences, recursively.
	 * <pre>
	 * "Recursively" means these checks are applied to every painted cell, so a
	 * request to paint a cell actually paints it and all of it's consequences.
	 * Those "all consequences" are two-fold:
	 * (1) if this value has two places in any of this cells regions, then the
	 *     other place is painted the opposite color; and
	 * (2) if the cell has two potential values, then the other value is
	 *     painted the other color.
	 * </pre>
	 * WARNING: You check that the cell-value is not already painted this color
	 * before calling paint, because building the why parameter is slow.
	 * <p>
	 * Paint implements Steps 2 to 4 of ./#XColoring.txt with a recursive DFS,
	 * where the instructions imply a BFS, without saying so explicitly, which
	 * is a warning. DFS is faster, but newbs tend towards BFS, so being an old
	 * prick (an experienced programmer), I "fixed" it, with my secret decoder
	 * ring (an IQ that is actually enough to blow my own hat off, thanks Bob).
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any of the three regions containing this cell except the given
	 *    region (nullable) has two places for this value then paint the
	 *    otherCell the opposite color; and recursively paint any consequences.
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
	 * IMPROVED: The why String is now constructed ONLY in the GUI + testcases
	 * when we might want to use it as part of the HTML to display to the user.
	 * In the batch (LogicalSolverTester) and in generate its just a waste of
	 * time. GEMTest compares the full HTML.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY when used: so we always
	 * pretest "is this value already painted this color" BEFORE building the
	 * why String, even in the GUI, because it's slow: a MINUTE per top1465.
	 * IMPROVED: Unsure if the slow part is building each why String, or append
	 * to steps, so Steps is now a StringBuilder, clear with steps.setLength(0)
	 * instead of growing a new buffer EACH AND EVERY time with steps += why.
	 * IMPROVED: The wantWhy field is set ONCE in the constructor.
	 * <p>
	 * SUGGEST: Problem is it takes ages building steps-SB, which is then used
	 * one time in like 500; so to avoid building unused whys. Keep fast DFS
	 * and write a new presentation BFS: If the DFS hints and wantWhy, then run
	 * the new presentation BFS to produce a hint with an explanation. The DFS
	 * results are sufficient for the batch, where wantWhy is false.
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
	private boolean paint(final Cell cell, final int v, final int c,
			final boolean biCheck, String why)
			throws OverpaintException
//			, BadPaintException
	{
		Cell otherCell; // the only other cell in this region which maybe v
		int otherValue; // the only other value of this bivalue cell
		// constants for understandability and speed (sort of).
		final int i = cell.i; // dereference the cell indice ONCE
		final int o = OPPOSITE[c]; // lookup the other color index ONCE
		final Idx[] otherColor = colors[o]; // lookup the other color ONCE

		// If cell-value is already painted this color then you've broken the
		// pre-test requirement, which is intended to reduce the time wasted
		// building why-strings that are never used. Note that asserts effect
		// developers only, who run with java -ea, so bad code NEVER makes it
		// through to prod. Well, almost never, which is not never. Sigh.
		assert !colors[c][v].has(i);

		// If cell-value is already painted the opposite color then throw.
		// Intersecting colors-sets renders both invalid, so neither is usable.
		//
		// NEVER happens in prod, only in dev: you avert overpaint ONLY when
		// that's valid. Where overpaint shouldn't happen ignore it so I throw.
		//
		// If unsure then ignore it and chase down any log WARN:s but you MUST
		// be sure that it is valid to avert an overpaint, do NOT just lazily
		// make the warning message go away; build a logical justification (or
		// not) otherwise GEM produces invalid hints. The logic MUST stand-up
		// as a whole. It's one out, all out. That's why GEM is difficult to
		// implement. Until it's all right then none of it's right. One misstep
		// breaks everything. So development is ONLY incremental, step by slow
		// step; double-checking each fully before moving on.
		if ( otherColor[v].has(i) ) {
			throw new OverpaintException(cell.id+MINUS+v+" is already "+COLORS[o]);
//			throw new BadPaintException(cell.id+MINUS+v+" is already "+COLORS[o]);
		}

		// 1. Paint the given cell-value this color.
		colors[c][v].add(i);
		paintedValues |= colorValues[c] |= VSHFT[v];
		// painting full-colors replaces the "on", if any
		ons[c][v].remove(i);
		// remember the steps in these two coloring-set (a StringBuilder)
		if ( why != null ) {
			steps.append(why);
			why = null;
		}

		// Now we examine the consequences of painting 'cell' color 'c'.
		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r : cell.regions ) { // cell's box, row, col
			if ( r.ridx[v].size == 2
			  // pre-check faster than building why string pointlessly
			  && !otherColor[v].has((otherCell=r.otherThan(cell, v)).i)
			) {
				if ( wantWhy )
					why = CON[c]+cell.id+MINUS+v+COFF[c]
						+CONJUGATE_IN+r.id+IS
						+CON[o]+otherCell.id+MINUS+v+COFF[o]+NL;
				// paint otherCell-v the opposite color, recursively.
				paint(otherCell, v, o, true, why);
			}
			// off all other places for v in r
			offs[c][v].or(otherPlaces.setAndNot(r.idxs[v], CELL_IDXS[i]));
		}

		// 3. Paint the other value of this bivalue cell the opposite color
		// skip this bi-check when we're painting "the other value"
		if ( biCheck
		  // if this cell has just two potential values
		  && cell.size == 2
		  // pre-check faster than building why string pointlessly
		  && !otherColor[otherValue=VFIRST[cell.maybes & ~VSHFT[v]]].has(i)
		) {
			if ( wantWhy )
				why = CON[c]+cell.id+MINUS+v+COFF[c]+ONLY_OTHER_VALUE_IS
					+CON[o]+cell.id+MINUS+otherValue+COFF[o]+NL;
			// paint cell-otherValue the opposite color, skipping biCheck
			paint(cell, otherValue, o, false, why);
		}
		// off all other values of this cell
		for ( int ov : VALUESES[cell.maybes & ~VSHFT[v]] )
			offs[c][ov].add(i);

		return true;
	}

	/**
	 * paintMonoBoxs is a KRC original. There is no other explanation, so I've
	 * done my best.
	 * <p>
	 * A "mono-box" is a box with only one place for v (a hidden single), which
	 * also leaves the "causal box" with one place for v, constituting a strong
	 * bidirectional link. If the link cannot be proven bidirectional then an
	 * "on" is created, which allows the cell value to be set in the event that
	 * this color is proven ALL true, but is NOT used in any further logic,
	 * with just one exception: "ons" ARE used in contradictions.
	 * <p>
	 * paintMonoBoxs differs from paintPromotions in its ability to discern the
	 * strong link between these two cells. Promotions just "ons" it, leaving
	 * it's promotion to chance, where-as paintMonoBoxs "field promotes" it.
	 * <p>
	 * The "shoot back" rule: We start with a cell in the grid that's painted
	 * this color, called the source cell. We examine each of the four boxs
	 * effected by the source cell (two left/right, and two above/below).
	 * If the effected box, minus buddies of all cells painted dis color leaves
	 * ONE cell (a Hidden Single, called the suspect) in the effected box then
	 * treat it, but how: colors or ons? To determine this we "shoot back".
	 * <p>
	 * Shoot back: if the box containing the source cell, minus buddies of the
	 * cell to paint (*1), leaves ONE cell in the box containing the source
	 * cell then paint the suspect because there's a bidirectional relationship
	 * (a "strong" link) between da suspect and da source cell, succinctly:<br>
	 * {@code if A is v then B is v AND CONVERSELY if B is v then A is v}<br>
	 * so we can paint B this color, else we just "on" the little bastard.
	 * <p>
	 * Note (*1) when we shoot back we cannot minus the buddies of (cells of
	 * this color minus the source cell). I tried, and it doesn't work, but I
	 * don't understand why it doesn't work. Mine is not to reason why, mine
	 * is just to do or die. But why? Or maybe I just ____ed up? sigh.
	 * <p>
	 * Possibly we could "shoot back" faster just in paintPromotions using:
	 * if an ons buds leave ONE v in any box that's this color den paint da on.
	 * So I tried that and it works (paints some) but finds less elims, so I
	 * didn't chase it down, I just use BOTH, finding more than either alone.
	 * It's nice to turn a ____up into a win, but I worry about paintPromotions
	 * not doing the SAME as me. It should, AFAIK, but still haven't chased it.
	 * Even finding an example would be long tedious pernickety work.
	 *
	 * @return any
	 */
	private boolean paintMonoBoxs() {
		Cell c1; // the source cell
		Cell c2; // the cell to paint, if any
		ARegion sb; // sourceBox: the box that contains the source cell
		String why = null; // the reason we paint this cell-value this color
		boolean any = false; // any painted
		boolean first = true; // is this the first painted or onned
		// foreach color
		for ( int c=0; c<2; ++c ) { // GREEN, BLUE
			final Idx[] thisColor = colors[c];
			// foreach value which has a cell painted thisColor
			for ( int v : VALUESES[colorValues[c]] ) {
				// foreach sourceCell whose v is painted thisColor
				try ( final IALease ciLease = thisColor[v].toArrayLease() ) {
					for ( int ci : ciLease.array ) { // ci is cell-indice
						// foreach of the four boxes effected by this source cell.
						// ebi effectedBoxIndex grid.regions index of effected box.
						for ( int ebi : INDEXES[EFFECTED_BOXS[BOX_OF[ci]]] ) {
							// if there's only one v remaining in the effected box,
							// excluding this colors cells and there buddies.
							// Call plusBuds repeatedly coz paint adds thisColor[v]
							// underneath us; plusBuds caches for speed.
							if ( tmp1.setAndNotAny(gridRegions[ebi].idxs[v], thisColor[v].plusBuds())
							  && tmp1.size() == 1
							) {
								c2 = gridCells[tmp1.peek()]; // the cell to paint
								c1 = gridCells[ci]; // the source cell
								sb = c1.box; // the box containing the source cell
								// the cell to paint needs a buddy in the box that
								// contains the source cell, else forget it (c2 is
								// a Hidden Single from buddies of colors, ergo the
								// cell to paint is nondeterministic, which is NOT
								// what we want)
								if ( sb.idxs[v].intersects(c2.buds)
								  // and the "shoot back" rule
								  && tmp2.setAndNot(sb.idxs[v], c2.buds).size() == 1
								) {
									// a "strong" bidirectional link.
									if ( wantWhy ) {
										if ( first ) {
											first = false;
											why = MONOBOXS_LABEL;
										} else {
											why = "";
										}
										// nb: REGION_IDS are coincident with boxs
										why += CON[c]+c1.id+MINUS+v+COFF[c]
										+LEAVES+c2.id+ONLY+v+IN+REGION_IDS[ebi]
										+", which leaves "+c1.id+ONLY+v+IN+sb.id
										+PRODUCES+CON[c]+c2.id+MINUS+v+COFF[c]+NL;
									}
									// note that paint removes any pre-existing On
									any |= paint(c2, v, c, false, why);
								} else if ( !ons[c][v].has(c2.i) ) {
									// a "weak" mono-directional link.
									if ( wantWhy ) {
										if ( first ) {
											first = false;
											steps.append(MONOBOXS_LABEL);
										}
										// nb: REGION_IDS are coincident with boxs
										steps.append(CON[c]).append(c1.id).append(MINUS).append(v).append(COFF[c])
										  .append(LEAVES).append(c2.id).append(ONLY).append(v).append(IN).append(REGION_IDS[ebi])
										  .append(PRODUCES).append(ON_MARKER).append(CON[c]).append(c2.id).append(PLUS).append(v).append(COFF[c])
										  .append(NL);
									}
									ons[c][v].add(c2.i);
									onsValues[c] |= VSHFT[v];
								}
							}
						}
					}
				}
			}
		}
		return any;
	}

	/**
	 * Calculate an uncached hash64 of a color (of the upto 9 Idx's).
	 * <p>
	 * Note that these hashes are now 64 bits, reducing collisions to 17. I use
	 * the new hash64 method of Idx, which does NOT cache. Each hash64 contains
	 * the indices of cells whose v is painted this color, so the net result is
	 * indices of all cells with any value painted this color, which is "mostly
	 * distinct enough" to identify a painted-set. The low-17 of a2 and the
	 * high-17 of a1 collide (both sets of cells use the same bits), so where a
	 * collision occurs we wrongly skip the search, about one in 500 of which
	 * produces a hint, so we're missing ? hints here. A few but insignificant.
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
	 * So it's faster WITHOUT hash-suppression, ie hash-suppression costs more
	 * than it saves, so I comment-it-out for now, for future removal. The good
	 * news is that hash-suppression misses 0 hints, despite collision problem.
	 * So this whole cluster____ of complexity turned out to be a waste of time
	 * but you never know if you never try. Maybe hash-suppression can be done
	 * faster, but I know not how.
	 * <pre>
	 * 2021-11-30 09:44 "fast HS" does it all inline instead of call Idx.hash64
	 * repeatedly, even though it's ludicrously complex, and saves just 114ms.
	 * 2021-11-30 11:37 LongHashSet is slower, so revert to HashMap.
	 * </pre>
	 *
	 * @param c the color: GREEN or BLUE
	 * @return hash64 of colors[c][*]
	 */
	private static long hash(final Idx[] thisColor, final int[] colorValues) {
		Idx t; // temp Idx pointer to thisColor[v]
		long result = 0L; // the hashCode
		for ( int v : colorValues )
			// This must be done in one line, for speed. Converting each int
			// into a long makes it hard to follow. Suck it up princess. The
			// technique is taken from Idx.hash64, which is now commented-out.
			result |= (((long)(t=thisColor[v]).a2)<<37L) ^ (((long)t.a1)<<27L) ^ ((long)t.a0);
		return result;
	}

	/**
	 * Accumulate (or) the indices of all values of this color into result.
	 *
	 * @param c the color: GREEN or BLUE
	 * @return total number of indices in painted[c]
	 */
	private static int countAll(final Idx[] color, final int[] vs, final Idx idx) {
		idx.clear();
		for ( int v : vs )
			idx.or(color[v]);
		return idx.size();
	}

	/**
	 * promote all possible ons (Supers) to full colors (Pars).
	 * <pre>
	 * Promotions
	 *	 * 1. When all but one cell mates or siblings of a value are "off"ed then
	 *	 *    the survivor is an "on" of that color.
	 *	 *    a) The only un-off'ed value in a cell is an "on".
	 *	 *    b) The only un-off'ed place for a value in a region is an "on".
	 *	 *    Both of these rules are ALL in one color.
	 *	 * 2. An "on" seeing an opposite color sibling or cell mate gets painted.
	 *	 *    a) If an "on" sees opposite color (same value) then paint it.
	 *	 *    b) If an "on" has an opposite colored cell-mate (any value) paint it.
	 *	 * 3. If An "on" leaves any of it's effected boxs (2 lft/rght, 2 abv/blw)
	 *	 *    with ONE v, that's this color, then paint it.
	 *	 * </pre>
	 * @return any
	 */
	private boolean paintPromotions() {
		Cell cell;
		String why = null;
		int c, o, i, v;
		// presume that no cell-values will be promoted
		boolean any = false, first = true;

		// 1a. The only un-off'ed value in a cell is an "on".
		for ( c=0; c<2; ++c ) {
			getValuesOffedFromEachCell(c); // repopulate OFF_VALS
			for ( i=0; i<GRID_SIZE; ++i )
				if ( gridCells[i].size == VSIZE[OFF_VALS[i]] + 1 ) {
					v = VFIRST[gridCells[i].maybes & ~OFF_VALS[i]];
					// pre-check it's not already painted for speed
					if ( !colors[c][v].has(i)
					  // and it's not already an On
					  && !ons[c][v].has(i) ) {
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append(PROMOTIONS_LABEL).append(NL);
							}
							steps.append("last potential value").append(PRODUCES).append(ON_MARKER)
							  .append(CON[c]).append(CELL_IDS[i]).append(PLUS).append(v).append(COFF[c])
							  .append(NL);
						}
						ons[c][v].add(i);
						onsValues[c] |= VSHFT[v];
					}
				}
		}

		// 1b. The only un-off'ed place for a value in a region is an "on".
		for ( c=0; c<2; ++c )
			for ( int v1 : VALUESES[colorValues[c]] )
				for ( ARegion r : gridRegions )
					if ( tmp1.setAndAny(offs[c][v1], r.idx)
					  && tmp2.setAndNot(r.idxs[v1], tmp1).size() == 1
					  && !colors[c][v1].has(i=tmp2.peek())
					  // pre-check it's not already painted for speed
					  && !ons[c][v1].has(i)
					) {
						cell = gridCells[i];
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append(PROMOTIONS_LABEL).append(NL);
							}
							steps.append("last place in ").append(r.id).append(PRODUCES).append(ON_MARKER)
							  .append(CON[c]).append(cell.id).append(PLUS).append(v1).append(COFF[c])
							  .append(NL);
						}
						ons[c][v1].add(cell.i);
						onsValues[c] |= VSHFT[v1];
					}

		// 2a. If an on sees (same v, OPPOSITE color) a colors then paint it.
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[onsValues[c]] ) {
				try ( final IALease lease = ons[c][v1].toArrayLease() ) {
					for ( int ii : lease.array ) {
						if ( tmp1.setAndAny(BUDDIES[ii], colors[o][v1])
						  // pre-check it's not already painted for speed
						  && !colors[c][v1].has(ii) ) {
							cell = gridCells[ii];
							if ( wantWhy ) {
								if ( first ) {
									first = false;
									steps.append(NL).append(PROMOTIONS_LABEL).append(NL);
								}
								why = SEES+A+CCOLORS[o]+PRODUCES
									+CON[c]+cell.id+MINUS+v1+COFF[c]+NL;
							}
							any |= paint(cell, v1, c, T, why);
						}
					}
				}
			}
		}

		// 2b. If an on has an OPPOSITE colored cell-mate (of another value)
		//     then paint it.
		countAll(colors[G], VALUESES[colorValues[G]], painted[G]);
		countAll(colors[B], VALUESES[colorValues[B]], painted[B]);
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[colorValues[c]] ) {
				if ( tmp2.setAndAny(ons[c][v1], painted[o]) ) {
					try ( final IALease lease = tmp2.toArrayLease() ) {
						for ( int ii : lease.array ) {
							// cheeky way to do other color has another value:
							// if cell-in-other-color (already done above)
							// and not other-color-this-value.contains(cell)
							if ( !colors[o][v1].has(ii)
							  // pre-check it's not already painted for speed
							  && !colors[c][v1].has(ii) ) {
								cell = gridCells[ii];
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										steps.append(NL).append(PROMOTIONS_LABEL).append(NL);
									}
									why = HAS+A+CCOLORS[o]+PRODUCES
										+ CON[c]+cell.id+MINUS+v1+COFF[c]+NL;
								}
								any |= paint(cell, v1, c, T, why);
							}
						}
					}
				}
			}
		}

		// if on.buds leave ONE v in any box that's this color den paint da on.
		// WTF: paintMonoBoxs finds Ons that this doesn't! Why? So use both.
		// WEIRD: Start from the suspect On, and just shoot back.
		if ( PROMOTIONS_SHOOTS_BACK ) {
			// foreach color: GREEN, BLUE
			for ( c=0; c<2; ++c ) {
				// foreach value which has an On of this color
				for ( int v1 : VALUESES[onsValues[c]] ) {
					// foreach indice of the "suspect" On (to be painted)
					try ( final IALease lease = ons[c][v1].toArrayLease() ) {
						for ( int ii : lease.array ) {
							// foreach index of the possible source box
							for ( int ri : INDEXES[EFFECTED_BOXS[BOX_OF[ii]]] ) {
								// if ons buddies leave ONE v in the source box
								if ( tmp1.setAndNotAny(gridRegions[ri].idxs[v1], BUDDIES[ii])
								  && tmp1.size() == 1
								  // and that source box v is this color
								  && tmp1.intersects(colors[c][v1])
								  // pre-check it's not already painted for speed
								  // We need to check in here (not pre-check) incase
								  // it's painted in this loop, otherwise we hit the
								  // assert in paint in 63#top1465.d5.mt. sigh.
								  && !colors[c][v1].has(ii)
								) {
									cell = gridCells[ii];
									if ( wantWhy ) //      the source Cell id
										why = "shoot back at "+CELL_IDS[tmp1.poll()]+PRODUCES
											+ CON[c]+cell.id+MINUS+v1+COFF[c]+NL;
									any |= paint(cell, v1, c, T, why);
									break; // first box only
								}
							}
						}
					}
				}
			}
		}

		// clean-up any hangover on's that've been painted (should be a no-op)
		for ( c=0; c<2; ++c )
			for ( int v1 : VALUESES[onsValues[c]] ) {
				// If you're seeing this you need to work-out how the ____ did:
				// (a) an On get created when it's already colored; or
				// (b) a cell get painted without removing the existing On?
				// This is no biggy in prod (it's handled) but it's unclean!
				// To be clear:
				// (a) if color exists for cell-value DO NOT create an On; and
				// (b) the paint method removes any existing On
				// so that colors and ons remain disjunct sets, for simplicity.
				assert Idx.disjunct(ons[c][v1], colors[c][v1])
						: "unclean: "+Idx.ofAnd(ons[c][v1], colors[c][v1]);
				ons[c][v1].andNot(colors[c][v1]);
			}
		return any;
	}

	/**
	 * Rebuild OFF_VALS: a bitset of the values eliminated by the given color
	 * from each cell in the grid.
	 *
	 * @param c the color: GREEN or BLUE
	 */
	private void getValuesOffedFromEachCell(final int c) {
		final Idx[] thisOffs = offs[c]; // offs of this color
		Arrays.fill(OFF_VALS, 0);
		for ( int v : VALUESES[colorValues[c]] ) {
			final int sv = VSHFT[v];
			thisOffs[v].forEach((i)->OFF_VALS[i] |= sv);
		}
	}

	/**
	 * Does either color contain a contradiction. First of:<pre>
	 * * If two of any combo of greens and green-ons in a cell then blue true.
	 * * If any combo of greens and green-ons (same value) in region then blue.
	 * * RARE: If green off's all of a cells values then blue is true.
	 * * RARE: If all v's in a region see a green v then blue is true.
	 * </pre>
	 * The use of ons in contradictions is worthy of some explanation. The ons
	 * are true if the color is true, right? Well that means that if any of the
	 * ons are invalid then the whole color is false; so we can safely use the
	 * ons in contradictions (but NOT in confirmations).
	 * <p>
	 * Didn't work: I tried if cell-value in both colors/ons and offs then the
	 * other color is true. Finds 0 top1465 after first two rules above, and
	 * finds less than half of the above two rules combined, so forget it.
	 *
	 * @return The subtype of hint to be created:<pre>
	 * 0: meaning none.
	 * 2: 2+ green values/ons in cell (261)
	 * 3: 2+ green v's/ons in region (168)
	 * 4: green off's all of a cells values (9)
	 * 5: All v's in region see a green v (2)
	 * NOTE: 6 and 7 are confirmations (not mine)
	 * </pre>
	 */
	private int contradictions() {
		Cell cell; // cell to hint on
		Idx t1, t2;
		Idx buds; // buddies of thisColor[v]
		Idx[] thisColor, thisOns, thisColorOns;
		int c, cands, i, a0,a1,a2;
		// presume neither color is good (an invalid value)
		goodColor = -1;

		for ( c=0; c<2; ++c ) {

			thisColor = colors[c];
			thisOns = ons[c];
			thisColorOns = colorsOns[c];

			// repopulate colorsOns[c]
			cands = colorValues[c] | onsValues[c];
			for ( int v : VALUESES[cands] )
				thisColorOns[v].setOr(thisColor[v], thisOns[v]);

			// If any combo of greens and ons (different values) in cell
			// then blue is true. (265 in top1465)
			for ( int v1 : VALUESES[cands] )
				// foreach color/on value EXCEPT v1
				for ( int v2 : VALUESES[cands & ~VSHFT[v1]] )
					if ( tmp1.setAndAny(thisColorOns[v1], thisColorOns[v2]) ) {
						// report first only (confusing if we show all)
						cell = gridCells[tmp1.peek()];
						cause = Cells.set(cell);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
							  .append(KON).append(cell.id).append(KOFF)
							  .append(HAS).append(CON[c]).append(COLORS[c]).append(SP).append(v1)
							  .append(AND).append(v2).append(COFF[c])
							  .append(COMMA_SO).append(CCOLORS[goodColor])
							  .append(IS_TRUE).append(PERIOD).append(NL);
						return SUBTYPE_2;
					}

			// If any combo of greens and ons (same value) in region
			// then blue is true. (170 in top1465)
			for ( int v : VALUESES[cands] ) {
				t1 = thisColorOns[v];
				for ( ARegion r : gridRegions ) {
					if ( r.ridx[v].size > 1
// inlined for speed (this is the slow section, which is now just a bit faster)
//					  && tmp2.setAndMany(thisColorOns[v], r.idx)
					  && ( (a0 = t1.a0 & (t2=r.idx).a0)
						 | (a1 = t1.a1 & t2.a1)
						 | (a2 = t1.a2 & t2.a2) ) != 0
					  && bitCount(a0)+bitCount(a1)+bitCount(a2) > 1
					) {
						cause = tmp2.set(a0,a1,a2).toCellSet(gridCells);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL)
							  .append(CONTRADICTION_LABEL).append(NL)
							  .append(KON).append(r.id).append(KOFF)
							  .append(HAS).append(MULTIPLE).append(CON[c]).append(v).append("/+'s").append(COFF[c])
							  .append(COMMA_SO).append(CCOLORS[goodColor])
							  .append(IS_TRUE).append(PERIOD).append(NL);
						return SUBTYPE_3;
					}
				}
			}

			// If green off's all of a cells values then blue is true.
			// RARE: 10 in top1465
			countOffsInEachCell(c); // repopulates countOffs from offs
			for ( i=0; i<GRID_SIZE; ++i )
				if ( countOffs[i]>0 && countOffs[i]==gridCells[i].size ) {
					cell = gridCells[i];
					cause = Cells.set(cell);
					goodColor = OPPOSITE[c];
					if ( wantWhy )
						steps.append(NL)
						  .append(CONTRADICTION_LABEL).append(NL)
						  .append(KON).append(cell.id).append(KOFF).append(" all values are ")
						  .append(CON[c]).append(COLORS[c]).append(SP).append(MINUS).append(COFF[c])
						  .append(COMMA_SO).append(CCOLORS[goodColor]).append(IS_TRUE)
						  .append(PERIOD).append(NL);
					return SUBTYPE_4;
				}

			// If all v's in a region see a green v then blue is true.
			// RARE: 3 in top1465
			for ( int v : VALUESES[colorValues[c]] ) {
				buds = thisColor[v].buds(true);
				for ( ARegion r : gridRegions ) {
					if ( r.ridx[v].size > 1
					  && tmp1.setAnd(buds, r.idx).equals(r.idxs[v])
					) {
						region = r;
						cause = tmp1.toCellSet(gridCells);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL)
							  .append(CONTRADICTION_LABEL).append(NL)
							  .append(KON).append(r.id).append(KOFF)
							  .append(ALL).append(v).append(APOSTROPHE_S)
							  .append(SEE).append(CON[c]).append(v).append(APOSTROPHE_S).append(COFF[c])
							  .append(COMMA_SO).append(CCOLORS[goodColor]).append(IS_TRUE)
							  .append(PERIOD).append(NL);
						return SUBTYPE_5;
					}
				}
			}
		}

		return 0;
	}

	/**
	 * Count the number of values off'd of each cell in the grid.
	 */
	private void countOffsInEachCell(final int c) {
		final Idx[] thisColorsOffs = offs[c];
		Arrays.fill(countOffs, 0);
		for ( int v=1; v<VALUE_CEILING; ++v )
			if ( thisColorsOffs[v].any() )
				thisColorsOffs[v].forEach((i)->++countOffs[i]);
	}

	/**
	 * Do a post-eliminations confirmation (opposite of contradiction) search
	 * to promote an elimination hint to a multi hint: setting all cell values
	 * in a color (and the ons).
	 * <pre>
	 * a) If all bar one value is eliminated from a cell, and the remaining
	 *    cell-value is colored then that whole color is true.
	 * b) when all but one place is eliminated from a region and the surviving
	 *    cell-value is colored then that whole color is true.
	 * </pre>
	 * NOTE: Don't use ons here! This is why ons are separate from colors.
	 * If a color is true then all ons are also true, but NOT conversely!
	 * <p>
	 * I query the redPots from eliminations, and HashMap look-ups are slow, so
	 * I am slow. The saving grace is I run only when eliminations hint found,
	 * ie not often enough to worry about.
	 * Tardiness reduced with redPots.toIdxs(REDS).
	 * <p>
	 * Any contradiction or confirmation hint is now the last hint GEM finds,
	 * regardless of what the passed accu says. A confirmation was usually the
	 * last hint found, but now that's always: after we add a confirmation to
	 * accu we throw a STOP_EXCEPTION to stop search. Smelly, but works.
	 * <p>
	 * These are as rare as rocking horse s__t: 2 a's and 0 b's in top1465.
	 *
	 * @return 0: meaning none<br>
	 * 4: meaning Type A<br>
	 * 5: meaning Type B<br>
	 * This value is up-shifted to differentiate it from existing subtypes.
	 */
	private int confirmations() {
		Cell cell;
		Integer values;

		// a) all bar one value is actually eliminated from a cell and that
		//    cell-value is colored then ALL of this color is true
		// RARE: 2 in top165
		for ( int c=0; c<2; ++c ) {
			for ( int v : VALUESES[colorValues[c]] ) {
				try ( final IALease lease = colors[c][v].toArrayLease() ) {
					for ( int i : lease.array ) {
						if ( (values=redPots.get(cell=gridCells[i])) != null
						  && VSIZE[values] == cell.size - 1
						) {
							if ( wantWhy )
								steps.append(NL)
								  .append(CONFIRMATION_LABEL).append(NL)
								  .append(color(c, cell.id+MINUS+v))
								  .append(" and all other values are eliminated")
								  .append(COMMA_SO).append(CCOLORS[c])
								  .append(IS_ALL_TRUE).append(NL);
							goodColor = c;
							return SUBTYPE_6; // continue on from existing subtypes
						}
					}
				}
			}
		}

		// b) if all bar one cell is actually eliminated from a region
		//    and that cell-value is colored
		//    then ALL of this color is true
		// RARE: 0 in top1465, so either my code is wrong or I really have none
		// of these; So, can you see a mistake in this code?
		if ( CONFIRMATIONS_HIDDEN_SINGLES ) {
			redPots.toIdxs(REDS);
			for ( int c=0; c<2; ++c ) {
				for ( int v : VALUESES[colorValues[c]] ) {
					// foreach indice of cell to examine
					try ( final IALease lease = colors[c][v].toArrayLease() ) {
						for ( int i : lease.array ) {
							// foreach of the 3 regions which contains this cell
							for ( ARegion r : gridCells[i].regions ) {
								// if all bar one v in this region is a red
								if ( tmp2.setAndAny(REDS[v], r.idx)
								  && tmp2.size() == r.ridx[v].size - 1
								  // and the only survivor is this cell
								  && tmp3.setAndNot(r.idxs[v], tmp2).peek() == i
								) {
									if ( wantWhy )
										steps.append(NL)
										  .append(CONFIRMATION_LABEL).append(NL)
										  .append(color(c, gridCells[i].id+MINUS+v))
										  .append(" and all other ").append(v).append(APOSTROPHE_S).append(IN).append(r.id)
										  .append(" are eliminated, so ").append(CCOLORS[c])
										  .append(IS_ALL_TRUE).append(NL);
									goodColor = c;
									region = r;
									return SUBTYPE_7;
								}
							}
						}
					}
				}
			}
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
			final Pots sets = squishSetPots(goodColor); // cell-values to set
			addOns(sets);
			final AHint hint = new GEMHintBig(this, value, new Pots(redPots)
					, subtype, cause, goodColor, steps.toString(), sets
					, pots(G), pots(B) , region, copy(ons), copy(offs));
			if ( CHECK_HINTS ) { // deal with any dodgy hints minimally
				if ( !cleanSetPots(sets, hint) )
					return null; // none remain once invalid removed
			}
			cause = null;
			region = null;
//			int s = getScore(value);
//			if ( s < minScore )
//				minScore = s;
			return hint;
		} catch ( Pots.IToldHimWeveAlreadyGotOneException ex ) {
			return null; // attempted to set 1 cell to 2 values.
		}
	}

	/**
	 * Add any ons to the given setPots, which is populated from colors.
	 */
	private void addOns(final Pots setPots) {
		Cell cell;
		Integer values;
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			if ( ons[goodColor][v].any() ) {
				try ( final IALease lease = ons[goodColor][v].toArrayLease() ) {
					for ( int i : lease.array ) {
						if ( (values=setPots.get(cell=gridCells[i])) == null )
							setPots.put(cell, VSHFT[v]);
						else {
							// colors MUST contain only one value
							assert VSIZE[values] == 1;
							// if colors-value != on-value then throw
							if ( (values & VSHFT[v]) != 0 )
								throw new Pots.IToldHimWeveAlreadyGotOneException(
									"Off on: "+cell.id+" colors:"+Values.toString(values)+" != ons:"+v);
							// if on-value == colors-value then just ignore the on.
							// This should never happen but if it does, meh!
						}
					}
				}
			}
		}
	}

	/**
	 * Create a new Pots from the given colors by placing only one value into
	 * each cell.
	 *
	 * @param c the color
	 * @return a new Pots of all values in this color. There must be one value
	 *  per cell in colors[c], else I throw IllegalStateException meaning that
	 *  colors[c] is not a valid setPots, so this hint "has gone bad".
	 * @throws Pots.IToldHimWeveAlreadyGotOneException when second value added
	 *  to a cell.
	 */
	private Pots squishSetPots(final int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] )
			thisColor[v].forEach(gridCells, (cc) ->
				result.insert(cc, VSHFT[v]) // throws
			);
		return result;
	}

	/**
	 * Do the 3D Medusa eliminations.
	 * <pre>
	 * Step 5.2: Eliminate if v sees both colors.
	 * (a) Both colors appear in a single tri+valued cell.
	 * (b) An uncolored candidate X can see two differently colored X.
	 * (c) An uncolored candidate sees a colored cell (eg green), but this
	 *     cell has another candidate with the opposite color (eg blue).
	 * // NEW IN GradedEquivalenceMarks
	 * (d) Both colors have Sub marked the same cell-value so eliminate it.
	 * NOTE: (c) is the only additional elimination over Extended Coloring.
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
		Cell cc; // currentCell: the cell to eliminate from
		int g // green value/s bitset
		  , b // blue value/s bitset
		  , pinks // bitset of "all other values" to eliminate
		  , c // color: the current color: GREEN or BLUE
		  , o; // opposite: the other color: BLUE or GREEN
		int subtype = 0; // presume none
		boolean first = true; // is this the first elimination

		// (a) Both colors in one cell eliminates all other values.
		try ( final IALease lease = tmp1.setAnd(painted[G], painted[B]).toArrayLease() ) {
			for ( int i : lease.array ) {
				if ( gridCells[i].size > 2
				  // get a bitset of all values of cells[i] that're green and blue.
				  // There may be none, never multiple (contradictions pre-tested)
				  // We need 1 green value and 1 blue value, to strip from pinks.
				  // NOTE: VSIZE[0] == 0.
				  && VSIZE[g=values(G, i)] == 1
				  && VSIZE[b=values(B, i)] == 1
				  // ensure that g and b are not equal (should NEVER be equal)
				  && g != b
				  // pinkos is a bitset of "all other values" to be eliminated.
				  && (pinks=(cc=gridCells[i]).maybes & ~g & ~b) != 0
				  // ignore already-justified eliminations (shouldn't happen here)
				  && redPots.upsert(cc, pinks, false)
				) {
					if ( wantWhy ) {
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(cc.id).append(HAS_BOTH)
						  .append(GON).append(Values.toString(g)).append(GOFF)
						  .append(AND).append(BON).append(Values.toString(b)).append(BOFF)
						  .append(COMMA).append(ELIMINATING)
						  .append(RON).append(ALL_OTHER_VALUES).append(ROFF).append(PERIOD)
						  .append(NL);
					}
					subtype |= 1;
				}
			}
		}

		// (b) An uncolored v sees both colored v's.
		// Weird: retain the leases, and there arrays, for the next loop.
		final IALease[] leases = new IALease[VALUE_CEILING];
		for ( int v : VALUESES[paintedValues] ) {
			// weird: remember uncoloredCandidates for re-use in next loop
			tmp2.setAndNot(gridIdxs[v], tmp1.setOr(colors[G][v], colors[B][v]));
			leases[v] = tmp2.toArrayLease();
			for ( int ii : leases[v].array ) {
				if ( tmp3.setAndAny(BUDDIES[ii], colors[G][v])
				  && tmp4.setAndAny(BUDDIES[ii], colors[B][v])
				  // ignore already-justified eliminations
				  && redPots.upsert(gridCells[ii], v)
				) {
					if ( wantWhy ) {
						cc = gridCells[ii]; // current cell
						Cell gc = closest(tmp3, cc.x, cc.y); // green cell seen
						Cell bc = closest(tmp4, cc.x, cc.y); // blue cell seen
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(cc.id).append(MINUS).append(v)
						  .append(SEES_BOTH).append(GON).append(gc.id).append(MINUS).append(v).append(GOFF)
						  .append(AND).append(BON).append(bc.id).append(MINUS).append(v).append(BOFF)
						  .append(COMMA_SO).append(cc.id).append(CANT_BE).append(RON).append(v).append(ROFF)
						  .append(PERIOD).append(NL);
					}
					subtype |= 2;
				}
			}
		}

		// (c) v sees green, and has some other blue value.
		for ( int v : VALUESES[paintedValues] ) {
			// weird: leases are set-up in the previous loop
			for ( int ii : leases[v].array ) {
				for ( c=0; c<2; ++c ) {
					// if cells[ii] sees a this-color v
					if ( tmp3.setAndAny(BUDDIES[ii], colors[c][v])
					  // and the opposite color (any value) contains ii
					  && painted[OPPOSITE[c]].has(ii)
					  // ignore already-justified eliminations
					  && redPots.upsert(gridCells[ii], v)
					) {
						if ( wantWhy ) {
							cc = gridCells[ii];
							o = OPPOSITE[c];
							Cell sib = closest(tmp3, cc.x, cc.y); // the seen cell
							int otherV = firstValue(o, ii);
							if ( first ) {
								first = false;
								steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
							}
							steps.append(cc.id)
							  .append(HAS).append(color(o, ""+otherV))
							  .append(" and sees ").append(color(c, sib.id+MINUS+v))
							  .append(COMMA_SO).append(cc.id).append(CANT_BE).append(RON).append(v).append(ROFF)
							  .append(PERIOD).append(NL);
						}
						subtype |= 4;
					}
				}
			}
		}
		// weird: release the leases
		for ( IALease lease : leases)
			if ( lease != null )
				lease.close();
		Arrays.fill(leases, null);

		// (d) off'ed by both colors.
		for ( int v=1; v<VALUE_CEILING; ++v ) {
			if ( tmp1.setAndAny(offs[G][v], offs[B][v]) ) {
				try ( final IALease lease = tmp1.toArrayLease() ) {
					for ( int ii : lease.array ) {
						// ignore already-justified eliminations
						if ( redPots.upsert(gridCells[ii], v) ) {
							if ( wantWhy ) {
								cc = gridCells[ii];
								if ( first ) {
									first = false;
									steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
								}
								steps.append(cc.id).append(MINUS).append(v)
								  .append(" is eliminated in both ").append(CCOLORS[G]).append(AND).append(CCOLORS[B])
								  .append(COMMA_SO).append(cc.id).append(CANT_BE).append(RON).append(v).append(ROFF)
								  .append(PERIOD).append(NL);
							}
							subtype |= 8;
						}
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
	 * @param i the cell indice to check
	 * @return a bitset of painted values
	 */
	private int values(final int c, final int i) {
		final Idx[] thisColor = colors[c];
		int result = 0;  // NOTE: VSIZE[0] == 0
		for ( int v : VALUESES[colorValues[c]] )
			if ( thisColor[v].has(i) )
				result |= VSHFT[v];
		return result;
	}

	/**
	 * Return the first value in colors[o][*] which contains(ii).
	 * <p>
	 * NOTE: We can't use FIRST_VALUE because not-found must return 0.
	 *
	 * @param o the opposite color: BLUE or GREEN
	 * @param ii the cell index
	 * @return the first value of ii that's painted the opposite color
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
	 * cell-of-this-color then the logic holds up; it's just nice to show the
	 * user the closest-cell-of-this-color; that's all.
	 *
	 * @param idx {@code tmp3.setAndAny(BUDDIES[ii], colors[c][v])} the buddies
	 * of the target cell which are painted this color.
	 * @param x {@code cc.x} the col of target cell
	 * @param y {@code cc.y} the row of target cell
	 * @return the closest cell (or null if I'm rooted)
	 */
	private Cell closest(final Idx idx, final int x, final int y) {
		int closest = -1; // the indice of the closest cell
		int minD = Integer.MAX_VALUE; // distance to the closest cell
		try ( final IALease lease = idx.toArrayLease() ) {
			for ( int i : lease.array ) {
				final int distance;
				if ( COL_OF[i] == x )
					distance = Math.abs(ROW_OF[i] - y);
				else if ( ROW_OF[i] == y )
					distance = Math.abs(COL_OF[i] - x);
				else { // pythagoras
					final int dx = Math.abs(COL_OF[i] - x);
					final int dy = Math.abs(ROW_OF[i] - y);
					distance = (int)Math.sqrt((double)(dx*dx + dy*dy));
				}
				if ( distance < minD ) {
					minD = distance;
					closest = i;
				}
			}
		}
		if ( closest == -1 )
			return null;
		return gridCells[closest];
	}

	/**
	 * Create a new "normal" GEMHint.
	 * <ul>
	 * <li>the subtype is now a bitset! 1=Type1, 2=Type2, 4=Type3. sigh.
	 * <li>just keep the subtype param, even though it's no longer used because
	 * the steps String is now populated up in eliminate, which is MUCH better.
	 * </ul>
	 *
	 * @param value the value
	 * @param subtype no longer used!
	 * @return a new GEMHint (for eliminations), or possibly a GEMHintMulti
	 *  (upgraded) if CONSEQUENT_SINGLES_TOO and the eliminations leave a cell
	 *  with one potential value, or a region with one place for a value.
	 */
	private AHint createHint(final int value, final int subtype) {
//		int s = getScore(value);
//		if ( s < minScore )
//			minScore = s;
		final Idx[] colorSet = null;
		final Pots reds = new Pots(redPots);
		final AHint hint = new GEMHint(this, value, reds, pots(G)
				, pots(B), colorSet, steps.toString(), null, copy(ons)
				, copy(offs));
		if ( CHECK_HINTS ) { // deal with any dodgy hints minimally
			if ( !cleanRedPots(reds, hint) )
				return null;
		}
		// don't hold cell references past there use-by date; so we copy-off
		// the fields when we create the hint (above) then clear the bastards.
		redPots.clear();
		return hint;
	}

	/**
	 * Build a new Pots of all cells, all values in the given color.
	 *
	 * @param c the color: GREEN or BLUE
	 * @return a new Pots of Cell=>value's in all values of colors[color].
	 */
	private Pots pots(final int c) {
		final Idx[] thisColor = colors[c];
		final Pots result = new Pots();
		for ( int v : VALUESES[colorValues[c]] )
			result.upsertAll(thisColor[v], grid, v);
		return result;
	}

	/**
	 * Is the given "normal" elimination hint valid?
	 * <p>
	 * This is the "normal" hint validation routine using the Validator.
	 * CHECK_HINTS is preferable because it's more forgiving. It drops bad
	 * eliminations/sets, instead of dumping the whole hint, optionally Log
	 * CHECK_NOISE to identify the problem/s, so you can address it/them.
	 * <p>
	 * Sudoku Explainer is a comedy of errors.
	 *
	 * @param hint to validate
	 * @return true is OK, false means do NOT add this hint.
	 */
	private boolean validEliminations(final AHint hint) {
		if ( VALIDATE_GEM ) {
			if ( !validOffs(grid, hint.getReds(0)) ) {
				hint.setIsInvalid(true);
				reportRedPots(tech.name(), grid, hint.toFullString());
				if ( Run.type != Run.Type.GUI )
					return false;
			}
		}
		return true;
	}

	/**
	 * Is this given "multi" hint (a setPots) valid?
	 * <p>
	 * This is the "normal" hint validation routine using the Validator.
	 * CHECK_HINTS is preferable because it's more forgiving. It drops bad
	 * eliminations/sets, instead of dumping the whole hint, optionally Log
	 * CHECK_NOISE to identify the problem/s, so you can address it/them.
	 * <p>
	 * Sudoku Explainer is a comedy of errors.
	 *
	 * @param identifier String to identify from whence I am called.
	 * @param hint to validate
	 * @return true is OK, false means do NOT add this hint.
	 */
	private boolean validSetPots(final String identifier, final AHint hint) {
		if ( VALIDATE_GEM ) {
			if ( !validOns(grid, hint.getResults()) ) {
				hint.setIsInvalid(true);
				reportSetPots(tech.name()+identifier, grid, invalidity, hint.toFullString());
				if ( Run.type != Run.Type.GUI )
					return false;
			}
		}
		return true;
	}

	// HACK: clean non-solution values out of setPots
	private boolean cleanSetPots(final Pots sets, final AHint hint) {
		final int[] solution = grid.getSolution();
		sets.entrySet().forEach((e) -> {
			final Cell c = e.getKey();
			final int v = VFIRST[e.getValue()]; // hintValue
			final int expect = solution[c.i];
			if ( v != expect ) {
//				throw new UncleanException(
				throw new UnsolvableException(
					"Invalid Set: "+c.id+PLUS+v+NOT_EQUALS+expect+NL
					+grid+NL
					+hint.toFullString()+NL
				);
			}
		});
		return !sets.isEmpty(); // return anything remaining?
	}

	// HACK: clean solution values out of redPots
	private boolean cleanRedPots(final Pots reds, final AHint hint) {
		final int[] solution = grid.getSolution();
		Iterator<Map.Entry<Cell,Integer>> it = reds.entrySet().iterator();
		while ( it.hasNext() ) {
			Map.Entry<Cell,Integer> e = it.next();
			Cell cell = e.getKey();
			Integer cands = e.getValue();
			if ( cell==null || cands==null )
				it.remove(); // BFIIK!
			else {
				final int expect = solution[cell.i];
				for ( int v : VALUESES[cands] )
					if ( v == expect )
						throw new UncleanException(
//						throw new UnsolvableException(
							"Invalid Elim: "+cell.id+MINUS+v+" is solution value!"+NL
							+grid+NL
							+hint.toFullString()+NL
						);
			}
		}
		return !reds.isEmpty(); // return anything remaining?
	}

	/**
	 * ValueScore is the score for each value; only used in the startingValues
	 * method, to keep the value WITH it's score, so that we can sort the array
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
		/** just reset the score to 0 (value is final). */
		private void clear() {
			score = 0;
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
	 * This the more extreme way to handle it, for when you think you're on top
	 * of invalid hints. If you're getting invalid hints then comment out my
	 * throw an uncomment the "programmer friendly" handlers, but don't forget
	 * to put me back. Users should NEVER see "Invalid Eliminations" in a hint
	 * explanation, because it makes them distrust ALL of your software, not
	 * just the small part they shouldn't trust, mainly coz they haven't got a
	 * clue, and really don't want one; that's WHY they're using software to
	 * get nice fat hints. Don't scare the chooks! Gobble, gobble, gobble.
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
