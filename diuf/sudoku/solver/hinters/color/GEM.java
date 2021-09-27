/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import static diuf.sudoku.Grid.*;
import static diuf.sudoku.Indexes.*;
import static diuf.sudoku.Values.*;

import diuf.sudoku.*;
import diuf.sudoku.Grid.*;
import diuf.sudoku.solver.*;
import diuf.sudoku.solver.accu.*;
import diuf.sudoku.solver.hinters.*;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * GEM (Graded Equivalence Marks) partially implements the Graded Equivalence
 * Marks specification at https://www.sudopedia.org/wiki (mine ./GEM.txt).
 * <p>
 * See the text version of above article in ./GEM.txt but please note that I
 * can't make heads-nor-tails of it. My mental picture seems at odds with the
 * authors, such that I can't make his explanation make any bloody sense what
 * so ever; which is an admission of a mental weakness on my part, rather than
 * a criticism of the publication. So in the end I gave-up trying to implement
 * the full GradedEquivalenceMarks and just did what makes sense to me, which
 * is basically MedusaColoring with cheese.
 * <p>
 * It's a lot like building a shopping mall as an extension to your pool dunny,
 * and then going to K-Mart anyway. sigh.
 * <p>
 * Developers please note that somebody who understands the spec may well get
 * many more eliminations by totally implementing the spec. I'm too stoopid!
 * <p>
 * This implementation of GEM uses a different mark-up to the specification:
 * GREEN represents Parity 1, and BLUE represents Parity 2.
 * <p>
 * Table of parity markers as per the specification:
 * <pre>
 *          Parity 1    Parity 2    Explanation
 * Par      '           "           bidirectional links
 * Super    -           =           true when pars are true
 * Sub      .           :           false when pars are true
 * </pre>
 * <p>
 * Table of colored markers as per my implementation:
 * <pre>
 *          GREEN       BLUE        Explanation
 * colors   value       value       bidirectional links
 * ons      +           +           set when color is true
 * offs     -           -           eliminated by this color
 * * My markers are painted in the appropriate color to differentiate them!
 *
 * NOTES:
 * 1. The Par mark is just the value in GREEN/BLUE, using existing colors.
 * 2. A Super/on mark is a grey value, then a "+" in GREEN/BLUE
 * 3. A Sub/off mark is a grey value, then a "-" in GREEN/BLUE or RED=both
 * 4. So parity1 is GREEN, parity2 is BLUE. + is On, - is Off. Simples!
 * 5. This is best way I can think of to translate the manual GEM markers into
 *    computereese that fits in with my existing coloring set-up.
 *
 * KRC 2021-03-20 08:25 When I try the full Graded Equivalence Marks spec it
 * goes to hell in a hand-basket. So either I'm too stoopid to understand the
 * spec (probable) or the spec is wrong (improbable). I'll hear argument either
 * way, but I'm pretty-pissed-off right now, so I declare the spec to be CRAP!
 * My humble attempt is enabled by USE_PROMOTIONS = true; but be warned that it
 * produces and handles MANY invalid hints (see batch log-file when true).
 * Nutting this crap is unfun!
 *
 * KRC 2021-03-21 07:59 I think I found my mistake. I painted the last value of
 * a cell or the last place in region; now I only "On" them, and leave it up to
 * the later "sibling promoters" whether or not they get full color, which work
 * reliably. I bet that's what the original spec says. I just didn't listen.
 *
 * KRC 2021-03-22 09:58 I've found and fixed the last of my many mistakes, so
 * I must eat my words: the specification is NOT crap, I'm just stupid!
 * 1. We need to mark ONLY valid offs, not just mark all offs and then clean-up
 *    afterwards, because the clean-up removes more than it should. So never
 *    mark an off that's colors/ons the opposite color.
 * 2. When we paint a cell-value we remove any off of the opposite color.
 * 3. Also I pushed Type 3's into Type 4's to get a Type 4 to test, but Type 3
 *    is simpler, so I don't force it any-longer, so Type 4's are now rare.
 *
 * KRC 2021-03-24 08:45 I found "a few" extra hints from invalid ons which mean
 * the other color must be true. Also changed some formatting.
 *
 * KRC 2021-03-31 11:40 I built Mark 5 last night, and have spent this morning
 * testing it, and tweaking a few things. I'll release it as Mark 6 tomorrow
 * and that's it for GEM from me; I know not how to improve it any further.
 *
 * KRC 2021-06-07 Got the s__ts with gemSolve so ripped-out consequentSingles.
 * </pre>
 *
 * @author Keith Corlett 2021-03-17
 */
public class GEM extends AHinter implements IPreparer
//		, diuf.sudoku.solver.IReporter
{
	// Should paintPromotions shoot back (use the "10 paces" rule).
	private static final boolean PROMOTIONS_SHOOTS_BACK = true;

	// Should confirmations look for Hidden Singles?
	// RARE as rocking horse s__t: 0 in top1465
	private static final boolean CONFIRMATIONS_HIDDEN_SINGLES = false;

	// Should hints be validated internally. Set me true when you ____ with the
	// promotions method, to check for invalid hints. I'm more forgiving than
	// the traditional HintValidator approach.
	// NOTE: YOU WILL ALSO NEED TO UNCOMMENT MY CODE!!!
	// Call: cleanRedPots to throw/remove invalid eliminations, and
	// Call: cleanSetPots to throw/remove invalid cell-values-to-set.
	//
	// NOTE this validation is besides the "normal" HintValidator, which can't
	// just remove the little bastards to keep things going, to show you what's
	// ____ed-up, so that you can fix it. The problem with error handlers is
	// they PREVENT you from seeing what the problem is. So you disable them
	// and there's a car crash. Then you try a smash-up derby, and discover how
	// much you like rubber bumpers; so it's all good.
	private static final boolean CHECK_HINTS = true;
//	// should we Log s__t when we CHECK_HINTS?
//	private static final boolean CHECK_NOISE = true;

	/** The first color: green. */
	private static final int GREEN = 0;
	/** The second color: blue. */
	private static final int BLUE = 1;
	/** The opposite color. */
	private static final int[] OPPOSITE = {BLUE, GREEN};
	/** names of the colors, in an array to select from. */
	private static final String[] COLORS = {"green", "blue"};
	/** colors ON HTML tags. */
	private static final String[] CON = {"<g>", "<b1>"};
	/** colors OFF HTML tags. */
	private static final String[] COFF = {"</g>", "</b1>"};
	/** colored color names, for convenience. */
	private static final String[] CCOLORS = {"<g>green</g>", "<b1>blue</b1>"};
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

	/** A distinctive subtype of RuntimeException. */
	private static final class StopException extends RuntimeException {
		private static final long serialVersionUID = 68198734001267L;
	}
	/** thrown ONLY when a Type 2 hint has been found and added to accu. */
	private static final StopException STOP_EXCEPTION = new StopException();

	/** A distinctive subtype of RuntimeException. */
	private static final class OverpaintException extends IllegalStateException {
		private static final long serialVersionUID = 720396129058L;
		public OverpaintException(String msg) {
			super(msg);
		}
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
		public UncleanException(String msg) {
			super(msg);
		}
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
		ValueScore(int value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return ""+value+": "+score;
		}
		/** just reset the score to 0 (value is final). */
		private void clear() {
			score = 0;
		}
	}

	/**
	 * Create the array of arrays which is a colors, or supers, subs.
	 * @param array
	 */
	private static void buildArray(Idx[][] array) {
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				array[c][v] = new Idx();
	}

	/**
	 * Clear all the Idx's in the given array (both colors, all values).
	 */
	private static void clear(Idx[][] array) {
		for ( int c=0; c<2; ++c )
			clear(array[c]);
	}

	/**
	 * Clear all the Idx's in the given array (all values).
	 */
	private static void clear(Idx[] array) {
		for ( int v=1; v<10; ++v )
			array[v].clear();
	}

	/**
	 * Deep copy all the Idx's in the given array (both colors, all values).
	 */
	private static Idx[][] copy(Idx[][] array) {
		Idx[][] copy = new Idx[2][10];
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				copy[c][v] = new Idx(array[c][v]);
		return copy;
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
	private static final int[] OFF_VALS = new int[81];

	/**
	 * for confirmations: an Idx of redPots (eliminations) by value 1..9
	 */
	private static final Idx[] REDS = new Idx[10];
	static {
		for ( int v=1; v<10; ++v )
			REDS[v] = new Idx();
	}

	/**
	 * Helper method to avert AIOOBE when coloring a string, for use when we're
	 * not sure that the goodColor field has been set. Note that I'm used only
	 * when goodColor could be -1 (not found) coz creating a temp-buffer for my
	 * 's' argument is MUCH slower than appending to an existing one; so use me
	 * only when creating a hint, not in the why-string of the paint routines.
	 */
	private static String color(int c, String s) {
		if ( c<0 || c>1 ) // especially -1 (goodColor not found)
			return s;
		return CON[c] + s + COFF[c];
	}

	/**
	 * Clear the given arrayOfArrays by nullifying each array, so that the GC
	 * can clean-up any arrays there-in that were created on-the-fly.
	 *
	 * @param arrayOfArrays
	 */
	private static void clear(int[][] arrayOfArrays) {
		for ( int i=0,n=arrayOfArrays.length; i<n; ++i )
			arrayOfArrays[i] = null;
	}

	// ============================ instance stuff ============================

	// almost everything is a field, to save creating instances once per call,
	// and also to to save passing everything around. For some reason creating
	// an Idx is pretty slow: BFIIK, no arrays here.

	// The grid we're processing is a field so there's no need to pass around.
	private Grid grid;
	// the accumulator
	private IAccumulator accu;
	// accu.isSingle()
	private boolean onlyOne;
	// indices of cells in grid which maybe each value 1..9
	private Idx[] vs;

	// do we steps+=why (an explanation) to go in the hint?
	private final boolean wantWhy;

	// There's 3 types (actually there's 4) of links in GEM:
	// 1. Par markers implemented as "colors",
	// 2. Super markers implemented as "ons", and
	//    (4th type: onOnly means this cannot be "upgraded" to a color)
	// 3. Sub markers implemented as "offs".
	//
	// A color is a "parity": a set of "strong" bidirectional links, ie if A1
	// is 3 then D2 is 3, and conversely if D2 is 3 then A1 is 3.
	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// So colors holds the indices of GREEN 7's, or BLUE 2's, or whatever.
	private final Idx[][] colors = new Idx[2][10];
	// An "On" is a "weak" unidirectional link that sets a cell value in this
	// color. Eg: if C7 is 3 then A5 is also 3, which does NOT imply if A5 is 3
	// then C7 is also 3. Supers are used to set cells if this parity turns-out
	// to be true, but are (mostly) excluded from calculations.
	private final Idx[][] ons = new Idx[2][10];
	// colorsOns are colors or ons (ie both) for use in contradictions.
	private final Idx[][] colorsOns = new Idx[2][10];
	// An "Off" is a "weak" unidirectional link that removes a potential value
	// in this color. Eg: if C7 is 3 then I7 is not 3, which does NOT imply if
	// I7 is not 3 then C7 is 3, so ONE super is the end of the line: you can't
	// "build on it" as if it was an actual color. If both parities off a
	// candidate then that candidate is eliminated.
	private final Idx[][] offs = new Idx[2][10];
	// A bitset of those values which have an on
	private final int[] onsValues = new int[2];

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
	private final Idx[] done = new Idx[10];
	{
		for ( int v=1; v<10; ++v )
			done[v] = new Idx();
	}

	/** The hashCode of each color, independent of it's color. */
	private final Map<Long,Object> hashes = new HashMap<>(128, 0.75F);
	private static final Object PRESENT = new Object();

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
		for ( int i=0; i<10; ++i ) // include zero to not upset sort
			scores[i] = new ValueScore(i);
	}

	/**
	 * Prepare is called after puzzles loaded, but BEFORE the first findHints.
	 * @param gridUnused
	 * @param logicalSolverUnused
	 */
	@Override
	public void prepare(Grid gridUnused, LogicalSolver logicalSolverUnused) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
	}

//	@Override
//	public void report() {
//		Log.teef("\n%s: minContradiction=%d, minConfirmation=%d, minElimination=%d\n"
//			, getClass().getSimpleName(), minContradiction, minConfirmation, minElimination);
//		Log.teef("\n%s: minScore=%d\n", getClass().getSimpleName(), minScore);
//	}
//	private int minContradiction = Integer.MAX_VALUE;
//	private int minConfirmation = Integer.MAX_VALUE;
//	private int minElimination = Integer.MAX_VALUE;
//	private int minScore = Integer.MAX_VALUE;

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
	public boolean findHints(Grid grid, IAccumulator accu) {
		// DEAD_CAT disables a hinter DURING solve (which does disabled only
		// BEFORE kick-off) so each hinter that's ever gone DEAD_CAT checks
		// isEnabled itself. I'm re-enabled by the prepare method, so I'm down
		// for this puzzle only. I'll get you next time Batman. Unfortunately
		// when you reload the current puzzle prepare is not called. I should
		// change the GUI to call it.
		if ( !isEnabled )
			return false;
		Cell cell;
		this.grid = grid;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		this.vs = grid.getIdxs();
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
				for ( ARegion r : grid.regions ) {
//if ( v==7 && r.id.equals("box 4") )
//	Debug.breakpoint();
					// if v has 2 possible positions in this region
					if ( r.indexesOf[v].size == 2
					  // and we haven't already painted this cell-value.
					  && !done[v].contains((cell=r.first(v)).i)
					  // and the search finds something
					  && ( result |= search(cell, v) )
					  // and we want onlyOne hint
					  && onlyOne )
						return result; // all done
				}
			}
		} catch ( UncleanException ex ) {
			// USE_PROMOTIONS -> create(Big)?Hint: bad elim or cell-value.
			Log.teeln("WARN: GEM: "+ex.getMessage());
			// disable me for this puzzle (re-enabled by prepare)
			setIsEnabled(false); // use the setter!
		} catch ( StopException meh ) {
			// thrown after a contradiction/confirmation hint is added to accu
			result = true;
		} finally {
			this.grid = null;
			this.accu = null;
			this.vs = null;
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
		for ( v=1; v<10; ++v )
			scores[v].clear();
		// count conjugate pairs for each value
		// also build-up a bitset of the values in 2+ conjugate pairs
		cands = 0;
		for ( v=1; v<10; ++v )
			for ( ARegion r : grid.regions )
				if ( r.indexesOf[v].size == 2
				  && ++scores[v].score > 1 )
					cands |= VSHFT[v];
		// foreach bivalue cell
		// increment the score of each value with 2+ conjugate pairs
		for ( Cell c : grid.cells )
			if ( c.maybes.size == 2 ) {
				values = VALUESES[c.maybes.bits & cands];
				for ( i=0,n=values.length; i<n; ++i )
					++scores[values[i]].score;
			}
		// order by score descending
		Arrays.sort(scores, SCORE_DESCENDING);
		// count the number of scores with a score of atleast 4. The minimum
		// possible minimum score is 3, but I use 4 coz it's top1465's minimum
		// that hints. There's no theoretical basis for 4, it's just what works
		// for me. I presume it'll work for all Sudoku puzzles but it may not!
		for ( n=0; n<10; ++n )
			if ( scores[n].score < 4 // see above comment
			  || scores[n].value == 0 ) // stop at value 0 (null terminator)
				break;
		return n;
	}
	// score of values 1..9 (a field only to support reporting minScore, but it
	// is also faster (I think) to not recreate the array for every call)
	private final ValueScore[] scores = new ValueScore[10];
//KEEP: used to get minScore to report and set the minimum score (above)
//	// get the score for the given value
//	private int getScore(int value) {
//		for ( int i=0; i<10; ++i )
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
	private boolean search(Cell cell, int v) {
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
		colorValues[GREEN]=colorValues[BLUE]=paintedValues = 0;
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
			paint(cell, v, GREEN, true, "Let's assume that "+cell.id+"-"+v
					+" is "+CCOLORS[GREEN]+NL);
			// on/paint consequences, and there consequences, ...
			while ( paintMonoBoxs() | paintPromotions() ) {
				// do nothing, just go again
			}

			// we search each value that was painted ONCE.
			for ( int v1 : VALUESES[paintedValues] )
				done[v1].or(colors[GREEN][v1]).or(colors[BLUE][v1]);

			// we search each distinct both-color-sets ONCE.
			// nb: these hashes are 64 bits, to reduce collisions.
			// nb: there's no distinction between green and blue in this hash
			hash = hash(GREEN) | hash(BLUE);
			if ( hashes.get(hash) != null )
				return false;
			hashes.put(hash, PRESENT);

			// sum the total number of cell-values colored.
			// WARN: n limits are top1465 specific. They're probably too high
			// for some puzzles. There is no theoretical basis for them, they
			// are just what works for me. Remove this limit for correctness.
			if ( (n = squash(GREEN, painted[GREEN])
			        + squash(BLUE, painted[BLUE])) < 3 )
				return false; // too few cells painted

			// Step 5: Analyse the cluster.
			// 5.1 If either color contains a contradiction
			//     then the opposite color MUST be true.
			// nb: minContradiction=3 so there's no filter on it
			if ( (subtype=contradictions()) != 0
			  // presuming that the opposite color isn't rooted too (sigh)
			  && (hint=createBigHint(v, subtype)) != null
			  // validate the hint if HintValidator.GEM_USES
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
				if ( n > 10 &&
				  // 5.3 promote "little" elimination hint to a big hint
				  (mst=confirmations()) != 0
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
		}
		return result;
	}

	/**
	 * Paint this cell this color, and all of it's consequences, recursively.
	 * Where "all consequences" are two fold: (1) if this value has two places
	 * in any of this cells box, row, or col, then the other place is painted
	 * the opposite color; and (2) if the cell has just two potential values,
	 * then other value is painted the other color. "Recursively" means this
	 * search is applied to each painted cell; so that painting just one cell
	 * has the effect of painting it and it's whole consequence tree.
	 * <p>
	 * WARNING: You check that the cell-value is not already painted this color
	 * before calling paint, because building the why parameter is slow.
	 * <p>
	 * Paint implements Steps 2 to 4 of ./Coloring.txt with a recursive DFS,
	 * where the instructions imply a BFS, without saying so explicitly, which
	 * is a warning. DFS is faster, but newbs tend to think in BFS. So being an
	 * experienced programmer (a mad old prick), I "fixed" it.
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any region containing this cell except the given region (nullable)
	 *    has two places for this value then paint the otherCell the opposite
	 *    color; and recursively paint the consequences thereof.
	 * 3. If biCheck and cell is bivalue then paint the otherValue the opposite
	 *    color; and recursively paint the consequences thereof.
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
	 * SUGGESTION: run paint once silently and if hint found and we're in the
	 * GUI (or testcases) then run it again with wantWhy true; we could even
	 * keep our DFS for speed and use a new BFS for presentation. Not sure.
	 * Not implemented coz I'm a lazy bastard.
	 *
	 * @param cell the cell to paint, recursively
	 * @param v the value to paint
	 * @param c the color to paint this cell-value: GREEN or BLUE
	 * @param biCheck if true then I also paint the otherValue of each bivalue
	 *  cell (cell.maybes.size==2) the opposite color, recursively. If false
	 *  then skip step 3, because we already know it's a waste of time.
	 * @param why a one-liner defining why this cell-value is painted this
	 *  color.
	 * @return any cells painted (always true, else throws)
	 * @throws OverpaintException when a cell-value is painted in both colors.
	 */
	private boolean paint(Cell cell, int v, int c, boolean biCheck, String why)
			throws OverpaintException {
		Cell otherCell; // the only other cell in this region which maybe v
		int otherValue; // the only other value of this bivalue cell
		// constants for understandability and speed (sort of).
		final int i = cell.i; // dereference the cell indice ONCE
		final int o = OPPOSITE[c]; // lookup the other color index ONCE
		final Idx[] otherColor = colors[o]; // lookup the other color ONCE

		// If cell-value is already painted this color then you've broken the
		// pre-test requirement, which is intended to reduce the time wasted
		// building why-strings that are never used. Note that asserts effect
		// developers only, who run with java -ea (enable assertions), so this
		// situation never makes it through to test, let alone prod.
		assert !colors[c][v].contains(i);

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
		if ( otherColor[v].contains(i) )
			throw new OverpaintException(cell.id+"-"+v+" is already "+COLORS[o]);

		// 1. Paint the given cell-value this color
		colors[c][v].add(i);
		paintedValues |= colorValues[c] |= VSHFT[v];
		// painting full-colors replaces the "on", if any
		ons[c][v].remove(i);
		// remember the steps in these two coloring-set (a StringBuilder)
		if ( why != null ) {
			steps.append(why);
			why = null;
		}

		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r : cell.regions ) { // cell's box, row, col
			if ( r.indexesOf[v].size == 2
			  // pre-check faster than building why string pointlessly
			  && !otherColor[v].contains((otherCell=r.otherThan(cell, v)).i) ) {
				if ( wantWhy )
					why = CON[c]+cell.id+"-"+v+COFF[c]
						+" conjugate in "+r.id
						+" is "+CON[o]+otherCell.id+"-"+v+COFF[o]+NL;
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
		  && cell.maybes.size == 2
		  // pre-check faster than building why string pointlessly
		  && !otherColor[otherValue=FIRST_VALUE[cell.maybes.bits & ~VSHFT[v]]].contains(i) ) {
			if ( wantWhy )
				why = CON[c]+cell.id+"-"+v+COFF[c]
					+" only other value is "
					+CON[o]+cell.id+"-"+otherValue+COFF[o]+NL;
			// paint cell-otherValue the opposite color, skipping biCheck
			paint(cell, otherValue, o, false, why);
		}
		// off all other values of this cell
		for ( int ov : VALUESES[cell.maybes.bits & ~VSHFT[v]] )
			offs[c][ov].add(i);

		return true;
	}

	/**
	 * A "mono-box" is a box with only one place for v (a hidden single), which
	 * also leaves the "causal box" with one place for v, constituting a strong
	 * bidirectional link. If the link cannot be proven bidirectional then an
	 * "on" is created, which allows the cell value to be set in the event that
	 * this color is ALL true, but isn't engaged in further logic (exception:
	 * "ons" ARE used in contradictions logic).
	 * <p>
	 * paintMonoBoxs differs from paintPromotions in its ability to discern the
	 * strong link between these two cells. Promotions just "ons" it, leaving
	 * it's promotion to chance, where-as I can "field promote" it.
	 * <p>
	 * The "10 paces" rule (shoot back): We start with a cell in the grid that
	 * is painted this color, called the source cell. We examine each of the 4
	 * boxs effected by the source cell (the 2 left/right, and 2 above/below).
	 * If the effected box, minus buddies of all cells painted dis color leaves
	 * ONE cell (a Hidden Single, called the suspect) in the effected box then
	 * treat it, but how: colors or ons?
	 * <p>
	 * We "shoot back": if the box containing the source cell, minus buddies of
	 * the cell to paint (*1), leaves ONE cell in the box containing the source
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
	 * <p>
	 * paintMonoBoxs is a KRC original. There is no other explanation, so I've
	 * done my best.
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
				// foreach cell painted thisColor (the source cell)
				// ci is the indice of the source cell
				for ( int ci : thisColor[v].toArrayA() ) {
					// foreach of the four boxes effected by this source cell.
					// ri is the index in grid.regions of the effected box.
					for ( int ri : INDEXES[EFFECTED_BOXS[BOX_OF[ci]]] ) {
						// if there's only one v remaining in the effected box,
						// excluding this colors cells and there buddies.
						// Call plusBuds repeatedly coz paint adds thisColor[v]
						// underneath us; plusBuds caches for speed.
						if ( tmp1.setAndNotAny(grid.regions[ri].idxs[v], thisColor[v].plusBuds())
						  && tmp1.size() == 1
						) {
							c2 = grid.cells[tmp1.peek()]; // the cell to paint
							c1 = grid.cells[ci]; // the source cell
							sb = c1.box; // the box containing the source cell
						    // the cell to paint needs a buddy in the box that
						    // contains the source cell, else forget it (c2 is
							// a Hidden Single from buddies of colors, ergo the
							// cell to paint is nondeterministic, which is NOT
							// what we want).
							if ( sb.idxs[v].andAny(c2.buds)
							  // and the "10 paces" rule (shoot back).
							  && tmp2.setAndNot(sb.idxs[v], c2.buds).size() == 1
							) {
								// a "strong" bidirectional link.
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										why = NL+"<u>Mono Boxs</u>"+NL;
									} else
										why = "";
									why += CON[c]+c1.id+"-"+v+COFF[c]
									+" leaves "+c2.id+" only "+v+" in "+Grid.REGION_IDS[ri]
									+", which leaves "+c1.id+" only "+v+" in "+sb.id
									+" => "+CON[c]+c2.id+"-"+v+COFF[c]+NL;
								}
								// note that paint removes any pre-existing On
								any |= paint(c2, v, c, false, why);
							} else if ( !ons[c][v].contains(c2.i) ) {
								// a "weak" mono-directional link.
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										steps.append(NL).append("<u>Mono Boxs</u>").append(NL);
									}
									steps.append(CON[c]).append(c1.id).append("-").append(v).append(COFF[c])
									  .append(" leaves ").append(c2.id).append(" only ").append(v).append(" in ").append(Grid.REGION_IDS[ri])
									  .append(" => $").append(CON[c]).append(c2.id).append("+").append(v).append(COFF[c])
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
		return any;
	}

	/**
	 * Calculate an uncached hash64 of a color (of the upto 9 Idx's).
	 * <p>
	 * Note that these hashes are now 64 bits, reducing collisions to 17.
	 * I use the new hash64 method of Idx, which does NOT cache.
	 *
	 * @param c the color: GREEN or BLUE
	 * @return hash64 of colors[c][*]
	 */
	private long hash(int c) {
		long hc = 0L;
		Idx idx;
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] )
			hc |= Idx.hash64((idx=thisColor[v]).a0, idx.a1, idx.a2);
		return hc;
	}

	/**
	 * Accumulate (or) the indices of all values of this color into result.
	 *
	 * @param c the color: GREEN or BLUE
	 * @return result.cardinality (the number of indices in the result)
	 */
	private int squash(final int c, final Idx result) {
		result.clear();
		for ( int v : VALUESES[colorValues[c]] )
			result.or(colors[c][v]);
		return result.size();
	}

	/**
	 * promote all possible ons (Supers) to full colors (Pars).
	 * <pre>
	 * Promotions
	 * 1. When all but one cell mates or siblings of a value are "off"ed then
	 *    the survivor is an "on" of that color.
	 *    a) The only un-off'ed value in a cell is an "on".
	 *    b) The only un-off'ed place for a value in a region is an "on".
	 *    Both of these rules are ALL in one color.
	 * 2. An "on" seeing an opposite color sibling or cell mate gets painted.
	 *    a) If an "on" sees opposite color (same value) then paint it.
	 *    b) If an "on" has an opposite colored cell-mate (any value) paint it.
	 * 3. If An "on" leaves any of it's effected boxs (2 lft/rght, 2 abv/blw)
	 *    with ONE v, that's this color, then paint it.
	 * </pre>
	 * @return any
	 */
	private boolean paintPromotions() {
		Cell cell;
		int c, o, i, v;
		final Cell[] cells = grid.cells;
		final ARegion[] regions = grid.regions;
		String why = null;
		// presume that no cell-values will be promoted
		boolean any = false, first = true;

		// 1a. The only un-off'ed value in a cell is an "on".
		for ( c=0; c<2; ++c ) {
			getValuesOffedFromEachCell(c); // repopulate OFF_VALS
			for ( i=0; i<81; ++i )
				if ( cells[i].maybes.size == VSIZE[OFF_VALS[i]] + 1 ) {
					v = FIRST_VALUE[cells[i].maybes.bits & ~OFF_VALS[i]];
					// pre-check it's not already painted for speed
					if ( !colors[c][v].contains(i)
					  // and it's not already an On
					  && !ons[c][v].contains(i) ) {
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Promotions</u>").append(NL);
							}
							steps.append("last potential value => $")
							  .append(CON[c]).append(CELL_IDS[i]).append("+")
							  .append(v).append(COFF[c]).append(NL);
						}
						ons[c][v].add(i);
						onsValues[c] |= VSHFT[v];
					}
				}
		}

		// 1b. The only un-off'ed place for a value in a region is an "on".
		for ( c=0; c<2; ++c )
			for ( int v1 : VALUESES[colorValues[c]] )
				for ( ARegion r : regions )
					if ( tmp1.setAndAny(offs[c][v1], r.idx)
					  && tmp2.setAndNot(r.idxs[v1], tmp1).size() == 1
					  && !colors[c][v1].contains(i=tmp2.peek())
					  // pre-check it's not already painted for speed
					  && !ons[c][v1].contains(i)
					) {
						cell = cells[i];
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Promotions</u>").append(NL);
							}
							steps.append("last place in ").append(r.id)
							  .append(" => $").append(CON[c]).append(cell.id)
							  .append("+").append(v1).append(COFF[c])
							  .append(NL);
						}
						ons[c][v1].add(cell.i);
						onsValues[c] |= VSHFT[v1];
					}

		// 2a. If an on sees (same v, OPPOSITE color) a colors then paint it.
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[onsValues[c]] )
				for ( int ii : ons[c][v1].toArrayA() )
					if ( tmp1.setAndAny(BUDDIES[ii], colors[o][v1])
					  // pre-check it's not already painted for speed
					  && !colors[c][v1].contains(ii) ) {
						cell = cells[ii];
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Promotions</u>").append(NL);
							}
							why = "sees a "+CCOLORS[o]+" => "
								+CON[c]+cell.id+"-"+v1+COFF[c]+NL;
						}
						any |= paint(cell, v1, c, T, why);
					}
		}

		// 2b. If an on has an OPPOSITE colored cell-mate (of another value)
		//     then paint it.
		squash(GREEN, painted[GREEN]); // all-values painted green
		squash(BLUE, painted[BLUE]); // all-values painted blue
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v1 : VALUESES[colorValues[c]] )
				if ( tmp2.setAndAny(ons[c][v1], painted[o]) )
					for ( int ii : tmp2.toArrayA() )
						// cheeky way to do other color has another value:
						// if cell-in-other-color (already done above)
						// and not other-color-this-value.contains(cell)
						if ( !colors[o][v1].contains(ii)
						  // pre-check it's not already painted for speed
						  && !colors[c][v1].contains(ii) ) {
							cell = cells[ii];
							if ( wantWhy ) {
								if ( first ) {
									first = false;
									steps.append(NL).append("<u>Promotions</u>").append(NL);
								}
								why = "has a "+CCOLORS[o]+" => "
									+ CON[c]+cell.id+"-"+v1+COFF[c]+NL;
							}
							any |= paint(cell, v1, c, T, why);
						}
		}

		// if on.buds leave ONE v in any box that's this color den paint da on.
		// WTF: paintMonoBoxs finds Ons that this doesn't! Why? So use both.
		// WEIRD: Start from the suspect On, and just shoot back.
		if ( PROMOTIONS_SHOOTS_BACK )
			// foreach color: GREEN, BLUE
			for ( c=0; c<2; ++c )
				// foreach value which has an On of this color
				for ( int v1 : VALUESES[onsValues[c]] )
					// foreach indice of the "suspect" On (to be painted)
					for ( int ii : ons[c][v1].toArrayA() )
						// foreach index of the possible source box
						for ( int ri : INDEXES[EFFECTED_BOXS[BOX_OF[ii]]] )
							// if ons buddies leave ONE v in the source box
							if ( tmp1.setAndNotAny(grid.regions[ri].idxs[v1], BUDDIES[ii])
							  && tmp1.size() == 1
							  // and that source box v is this color
							  && tmp1.andAny(colors[c][v1])
							  // pre-check it's not already painted for speed
							  // We need to check in here (not pre-check) incase
							  // it's painted in this loop, otherwise we hit the
							  // assert in paint in 63#top1465.d5.mt. sigh.
							  && !colors[c][v1].contains(ii)
							) {
								cell = cells[ii];
								if ( wantWhy ) //      the source Cell id
									why = "shoot back at "+Grid.CELL_IDS[tmp1.poll()]+" => "
										+ CON[c]+cell.id+"-"+v1+COFF[c]+NL;
								any |= paint(cell, v1, c, T, why);
								break; // first box only
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
						: "unclean: "+Idx.newAnd(ons[c][v1], colors[c][v1]);
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
	private void getValuesOffedFromEachCell(int c) {
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
		Idx buds; // buddies of thisColor[v]
		Idx[] thisColor, thisOns, thisColorOns;
		int c, cands, i;
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
						cell = grid.cells[tmp1.peek()];
						cause = Cells.set(cell);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL).append("<u>Contradiction</u>").append(NL)
							  .append("<k>").append(cell.id).append("</k>")
							  .append(" has ").append(CON[c]).append(COLORS[c]).append(" ").append(v1)
							  .append(" and ").append(v2).append(COFF[c])
							  .append(", so ").append(CCOLORS[goodColor]).append(" is true.").append(NL);
						return SUBTYPE_2;
					}

			// If any combo of greens and ons (same value) in region
			// then blue is true. (170 in top1465)
			for ( int v : VALUESES[cands] )
				for ( ARegion r : grid.regions )
					if ( r.indexesOf[v].size > 1
					  && tmp2.setAndMany(thisColorOns[v], r.idx)
					) {
						cause = tmp2.toCellSet(grid);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL)
							  .append("<u>Contradiction</u>").append(NL)
							  .append("<k>").append(r.id).append("</k>")
							  .append(" has multiple ").append(CON[c]).append(v).append("/+'s").append(COFF[c])
							  .append(", so ").append(CCOLORS[goodColor]).append(" is true.")
							  .append(NL);
						return SUBTYPE_3;
					}

			// If green off's all of a cells values then blue is true.
			// RARE: 10 in top1465
			countOffsInEachCell(c); // repopulates counts from offs
			for ( i=0; i<81; ++i )
				if ( counts[i]>0 && counts[i]==grid.cells[i].maybes.size ) {
					cell = grid.cells[i];
					cause = Cells.set(cell);
					goodColor = OPPOSITE[c];
					if ( wantWhy )
						steps.append(NL)
						  .append("<u>Contradiction</u>").append(NL)
						  .append("<k>").append(cell.id).append("</k>")
						  .append(" all values are ").append(CON[c]).append(COLORS[c]).append(" -").append(COFF[c])
						  .append(", so ").append(CCOLORS[goodColor]).append(" is true.")
						  .append(NL);
					return SUBTYPE_4;
				}

			// If all v's in a region see a green v then blue is true.
			// RARE: 3 in top1465
			for ( int v : VALUESES[colorValues[c]] ) {
				buds = thisColor[v].buds(true);
				for ( ARegion r : grid.regions ) {
					if ( r.indexesOf[v].size > 1
					  && tmp1.setAnd(buds, r.idx).equals(r.idxs[v])
					) {
						region = r;
						cause = tmp1.toCellSet(grid);
						goodColor = OPPOSITE[c];
						if ( wantWhy )
							steps.append(NL)
							  .append("<u>Contradiction</u>").append(NL)
							  .append("<k>").append(r.id).append("</k>")
							  .append(" all ").append(v).append("'s")
							  .append(" see ").append(CON[c]).append(v).append("'s").append(COFF[c])
							  .append(", so ").append(CCOLORS[goodColor]).append(" is true.")
							  .append(NL);
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
	private void countOffsInEachCell(int c) {
		final Idx[] thisOffs = offs[c];
		Arrays.fill(counts, 0);
		for ( int v=1; v<10; ++v )
			thisOffs[v].forEach((i)->++counts[i]);
	}
	private final int[] counts = new int[81];

	/**
	 * Do a post-eliminations confirmation (opposite of contradiction) search
	 * to promote an elimination hint to a multi hint: setting all cell values
	 * in a color (and the ons).
	 * <pre>
	 * a) If all bar one value is eliminated from a cell, and the remaining
	 *    cell-value is colored then that whole color is true.
	 * b) when all but one value is eliminated from a region and the remaining
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
		Values vals;

		// a) all bar one value is actually eliminated from a cell and that
		//    cell-value is colored then ALL of this color is true
		// RARE: 2 in top165
		for ( int c=0; c<2; ++c ) {
			for ( int v : VALUESES[colorValues[c]] ) {
				for ( int i : colors[c][v].toArrayA() ) {
					if ( (vals=redPots.get(cell=grid.cells[i])) != null
					  && vals.size == cell.maybes.size - 1
					) {
						if ( wantWhy )
							steps.append(NL)
							  .append("<u>Confirmation</u>").append(NL)
							  .append(color(c, cell.id+"-"+v))
							  .append(" and all other values are eliminated")
							  .append(", so ").append(CCOLORS[c])
							  .append(" is ALL true.").append(NL);
						goodColor = c;
						return SUBTYPE_6; // continue on from existing subtypes
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
					for ( int i : colors[c][v].toArrayA() ) { // indice of the cell to examine
						// foreach of the 3 regions which contains this cell
						for ( ARegion r : grid.cells[i].regions ) {
							// if all bar one v in this region is a red
							if ( tmp2.setAndAny(REDS[v], r.idx)
							  && tmp2.size() == r.indexesOf[v].size - 1
							  // and the only survivor is this cell
							  && tmp3.setAndNot(r.idxs[v], tmp2).peek() == i
							) {
								if ( wantWhy )
									steps.append(NL)
									  .append("<u>Confirmation</u>").append(NL)
									  .append(color(c, grid.cells[i].id+"-"+v))
									  .append(" and all other ").append(v).append("'s in ").append(r.id)
									  .append(" are eliminated, so ").append(CCOLORS[c])
									  .append(" is ALL true.").append(NL);
								goodColor = c;
								region = r;
								return SUBTYPE_7;
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
	private AHint createBigHint(int value, int subtype) {
		try {
			final Pots setPots = squishSetPots(goodColor); //cell-values to set
			addOns(setPots);
			if ( CHECK_HINTS ) { // deal with any dodgy hints minimally
				if ( !cleanSetPots(setPots) )
					return null; // none remain once invalid removed
			}
			final AHint hint = new GEMHintBig(this, value, new Pots(redPots)
					, subtype, cause, goodColor, steps.toString(), setPots
					, pots(GREEN), pots(BLUE) , region, copy(ons), copy(offs));
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
	private void addOns(Pots setPots) {
		Cell cell;
		Values vals;
		for ( int v=1; v<10; ++v ) {
			if ( ons[goodColor][v].any() ) {
				for ( int i : ons[goodColor][v].toArrayA() ) {
					if ( (vals=setPots.get(cell=grid.cells[i])) == null )
						setPots.put(cell, new Values(v));
					else {
						// colors MUST contain only one value
						assert vals.size == 1;
						// if colors-value != on-value then throw
						if ( !vals.contains(v) )
							throw new Pots.IToldHimWeveAlreadyGotOneException(
								"Off on: "+cell.id+" colors:"+vals+" != ons:"+v);
						// if on-value == colors-value then just ignore the on.
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
	 * @param c the color
	 * @return a new Pots of all values in this color. There must be one value
	 *  per cell in colors[c], else I throw IllegalStateException meaning that
	 *  colors[c] is not a valid setPots, so this hint "has gone bad".
	 * @throws Pots.IToldHimWeveAlreadyGotOneException when second value added
	 *  to a cell.
	 */
	private Pots squishSetPots(int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] )
			thisColor[v].forEach(grid.cells, (cc) ->
				result.insert(cc, new Values(v)) // throws
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
		// per value 1..9: indices of v's that aren't painted either color
		final int[][] unco = this.uncoloredCandidates;
		int subtype = 0; // presume none
		boolean first = true; // is this the first elimination

		// (a) Both colors in one cell eliminates all other values.
		for ( int i : tmp1.setAnd(painted[GREEN], painted[BLUE]).toArrayA() ) {
			if ( grid.cells[i].maybes.size > 2
			  // get a bitset of all values of cells[i] that're green and blue.
		      // There may be none, never multiple (contradictions pre-tested)
			  // We need 1 green value and 1 blue value, to strip from pinks.
			  // NOTE: VSIZE[0] == 0.
			  && VSIZE[g=values(GREEN, i)] == 1
			  && VSIZE[b=values(BLUE, i)] == 1
			  // ensure that g and b are not equal (should NEVER be equal)
			  && g != b
			  // pinkos is a bitset of "all other values" to be eliminated.
			  && (pinks=(cc=grid.cells[i]).maybes.bits & ~g & ~b) != 0
			  // ignore already-justified eliminations (shouldn't happen here)
			  && redPots.upsert(cc, new Values(pinks, false))
			) {
				if ( wantWhy ) {
					if ( first ) {
						first = false;
						steps.append(NL).append("<u>Eliminations</u>").append(NL);
					}
					steps.append(cc.id).append(" has")
					  .append(" both <g>").append(Values.toString(g)).append("</g>")
					  .append(" and <b1>").append(Values.toString(b)).append("</b1>")
					  .append(", eliminating <r>all other values</r>.")
					  .append(NL);
				}
				subtype |= 1;
			}
		}

		// (b) An uncolored v sees both colored v's.
		//<TO_ARRAY_SMART comment="weird">
		tmp2.toArraySmartReset(); // weird: prepare to use toArraySmart
		for ( int v : VALUESES[paintedValues] ) {
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			// weird: remember uncoloredCandidates for re-use in next loop
			for ( int ii : (unco[v]=tmp2.setAndNot(vs[v], tmp1).toArraySmart()) ) {
				if ( tmp3.setAndAny(BUDDIES[ii], colors[GREEN][v])
				  && tmp4.setAndAny(BUDDIES[ii], colors[BLUE][v])
				  // ignore already-justified eliminations
				  && redPots.upsert(grid.cells[ii], new Values(v))
				) {
					if ( wantWhy ) {
						cc = grid.cells[ii]; // current cell
						Cell gc = closest(tmp3, cc); // green cell seen
						Cell bc = closest(tmp4, cc); // blue cell seen
						if ( first ) {
							first = false;
							steps.append(NL).append("<u>Eliminations</u>").append(NL);
						}
						steps.append(cc.id).append("-").append(v)
						  .append(" sees both <g>").append(gc.id).append("-").append(v).append("</g>")
						  .append(" and <b1>").append(bc.id).append("-").append(v).append("</b1>")
						  .append(", so ").append(cc.id).append(" can't be <r>").append(v).append("</r>.")
						  .append(NL);
					}
					subtype |= 2;
				}
			}
		}

		// (c) v sees green, and has some other blue value.
		for ( int v : VALUESES[paintedValues] ) {
			// weird: uncoloredCandidates are set in the above loop
			for ( int ii : unco[v] ) {
				for ( c=0; c<2; ++c ) {
					// if cells[ii] sees a this-color v
					if ( tmp3.setAndAny(BUDDIES[ii], colors[c][v])
					  // and the opposite color (any value) contains ii
					  && painted[OPPOSITE[c]].contains(ii)
					  // ignore already-justified eliminations
					  && redPots.upsert(grid.cells[ii], v)
					) {
						if ( wantWhy ) {
							cc = grid.cells[ii];
							o = OPPOSITE[c];
							Cell sib = closest(tmp3, cc); // the seen cell
							int otherV = firstValue(o, ii);
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Eliminations</u>").append(NL);
							}
							steps.append(cc.id)
							  .append(" has ").append(color(o, ""+otherV))
							  .append(" and sees ").append(color(c, sib.id+"-"+v))
							  .append(", so ").append(cc.id).append(" can't be <r>").append(v).append("</r>.")
							  .append(NL);
						}
						subtype |= 4;
					}
				}
			}
		}
		// weird: release memory in any unco arrays created on the fly
		if ( tmp2.usedNew )
			clear(unco);
		//</TO_ARRAY_SMART>

		// (d) off'ed by both colors.
		for ( int v=1; v<10; ++v ) {
			if ( tmp1.setAndAny(offs[GREEN][v], offs[BLUE][v]) ) {
				for ( int ii : tmp1.toArrayA() ) {
					// ignore already-justified eliminations
					if ( redPots.upsert(grid.cells[ii], v) ) {
						if ( wantWhy ) {
							cc = grid.cells[ii];
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Eliminations</u>").append(NL);
							}
							steps.append(cc.id).append("-").append(v)
							  .append(" is eliminated in both ").append(CCOLORS[GREEN]).append(" and ").append(CCOLORS[BLUE])
							  .append(", so ").append(cc.id).append(" can't be <r>").append(v).append("</r>.")
							  .append(NL);
						}
						subtype |= 8;
					}
				}
			}
		}

		return subtype;
	}
	// A field to build the array once per instance, not during eliminations.
	// They're only ints (not Cells) so cleanup not required, and any hangovers
	// from the previous invocation don't effect the logic (they're skipped).
	// Note that the actual arrays referenced by this array-of-arrays actually
	// belong to the Idx's IAS (Integer ArrayS) cache, so toArray (et el) will
	// interfere arbitrarily with uncoloredCandidates, so do not call them
	// while uncoloredCandidates is in use.
	private final int[][] uncoloredCandidates = new int[10][];

	/**
	 * Get a bitset of the values that are painted color-c at indice-i.
	 *
	 * @param c the color index: GREEN, or BLUE
	 * @param i the cell indice to check
	 * @return a bitset of painted values
	 */
	private int values(final int c, final int i) {
		int result = 0;  // NOTE: VSIZE[0] == 0
		for ( int v : VALUESES[colorValues[c]] )
			if ( colors[c][v].contains(i) )
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
	private int firstValue(int o, int ii) {
		for ( int v : VALUESES[colorValues[o]] )
			if ( colors[o][v].contains(ii) )
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
	 * @param target {@code cc = grid.cells[ii]} the target bloody cell.
	 * @return the closest cell (or null if I'm rooted)
	 */
	private Cell closest(Idx idx, Cell target) {
		Cell closest = null;
		int minD = Integer.MAX_VALUE; // the distance to the closest cell
		// nb: caller uses toArraySmart, so I must avoid toArrayA/B. I'm only
		// called when we're hinting (rarely) so my performance isn't an issue,
		// whereas search performance is, so I just use a new array each time.
		for ( int i : idx.toArrayNew() ) {
			final Cell c = grid.cells[i];
			final int dy = Math.abs(c.y - target.y);
			final int dx = Math.abs(c.x - target.x);
			final int distance;
			if ( dy == 0 )
				distance = dx;
			else if ( dx == 0 )
				distance = dy;
			else // Pythons theorum: the square of ye hippopotamus = da sum of
				// da squares of yon two sides; except in ye gannetorium where
				// it remains indeterminate because of the bloody smell.
				distance = (int)Math.sqrt((double)(dx*dx + dy*dy));
			if ( distance < minD ) {
				minD = distance;
				closest = grid.cells[i];
			}
		}
		return closest;
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
	private AHint createHint(int value, int subtype) {
//		int s = getScore(value);
//		if ( s < minScore )
//			minScore = s;
		if ( CHECK_HINTS ) { // deal with any dodgy hints minimally
			if ( !cleanRedPots() )
				return null;
		}
		final Idx[] colorSet = null;
		final AHint hint = new GEMHint(this, value, new Pots(redPots)
			, pots(GREEN), pots(BLUE), colorSet, steps.toString(), null
			, copy(ons), copy(offs));
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
	private Pots pots(int c) {
		final Pots result = new Pots();
		for ( int v : VALUESES[colorValues[c]] )
			colors[c][v].forEach(grid.cells, (cc) -> {
					result.upsert(cc, new Values(v));
			});
		return result;
	}

	/**
	 * Is the given "normal" elimination hint valid?
	 * <p>
	 * This is the "normal" hint validation routine using the HintValidator.
	 * CHECK_HINTS is preferable because it's more forgiving. It drops bad
	 * eliminations/sets, instead of dumping the whole hint, optionally Log
	 * CHECK_NOISE to identify the problem/s, so you can address it/them.
	 * <p>
	 * Sudoku Explainer is a comedy of errors.
	 *
	 * @param hint to validate
	 * @return true is OK, false means do NOT add this hint.
	 */
	private boolean validEliminations(AHint hint) {
		if ( HintValidator.GEM_USES ) {
			if ( !HintValidator.isValid(grid, hint.getReds(0)) ) {
				hint.isInvalid = true;
				HintValidator.report(tech.name(), grid, hint.toFullString());
				if ( Run.type != Run.Type.GUI )
					return false;
			}
		}
		return true;
	}

	/**
	 * Is this given "multi" hint (a setPots) valid?
	 * <p>
	 * This is the "normal" hint validation routine using the HintValidator.
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
	private boolean validSetPots(String identifier, AHint hint) {
		if ( HintValidator.GEM_USES ) {
			if ( !HintValidator.isValidSetPots(grid, hint.getResults()) ) {
				hint.isInvalid = true;
				HintValidator.reportSetPots(tech.name()+identifier, grid
					, HintValidator.invalidity, hint.toFullString());
				if ( Run.type != Run.Type.GUI )
					return false;
			}
		}
		return true;
	}

	// HACK: clean non-solution values out of setPots
	private boolean cleanSetPots(Pots setPots) {
		Cell cell;
		Values values;
		final int[] svs = grid.getSolutionValues();
//		boolean first = true;
		for ( Iterator<Cell> it=setPots.keySet().iterator(); it.hasNext(); ) {
			if ( (values=setPots.get(cell=it.next())).first() != svs[cell.i] ) {
// unforgiving
				throw new UncleanException("Invalid SetPot: "+cell.id+"+"+values.first()+"!="+svs[cell.i]);
// forgiving
//				if ( CHECK_NOISE )
//					Log.println("WARN: GEM: Promo: Invalid SetPot: "+cell.id+"+"+values.first()+"!="+solution[cell.i]);
//				if ( wantWhy ) {
//					if ( first ) {
//						first = false;
//						steps.append(NL).append("<u>Invalid SetPots</u>").append(NL);
//					}
//					steps.append("<k><b>").append(cell.id).append("+").append(values.first())
//					  .append("!=").append(solution[cell.i]).append("</b></k>").append(NL);
//				}
//				it.remove();
			}
		}
		return !setPots.isEmpty(); // return any remaining cell-values to set?
	}

	// HACK: clean solution values out of redPots
	private boolean cleanRedPots() {
		Cell cell;
		Values values;
		final int[] svs = grid.getSolutionValues();
//		// is this the first invalid elimination?
//		boolean first = true;
		// use an iterator to delete from it
		for ( Iterator<Cell> it=redPots.keySet().iterator(); it.hasNext(); ) {
			values = redPots.get(cell = it.next());
			for ( int v : VALUESES[values.bits] )
				if ( svs[cell.i] == v ) {
// unforgiving
					throw new UncleanException("Invalid Elimination: "+cell.id+"-"+v+" is this cells value!");
// forgiving
//					if ( CHECK_NOISE )
//						Log.println("WARN: GEM: Promo: Invalid Elimination: "+cell.id+"-"+v+" is this cells value!");
//					if ( wantWhy ) {
//						if ( first ) {
//							first = false;
//							steps.append(NL).append("<u>Invalid Eliminations</u>").append(NL);
//						}
//						steps.append("<k><b>").append(cell.id).append("+").append(v)
//						  .append("</b></k> is this cells value!").append(NL);
//					}
//					if ( values.remove(v) == 0 )
//						it.remove();
				}

		}
		return !redPots.isEmpty(); // return any eliminations remaining
	}

}
