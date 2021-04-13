/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2020 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BOXS;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.EFFECTED_BOXS;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.FIRST_VALUE;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IPreparer;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.utils.Debug;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * GEM implements a subset of the Graded Equivalence Marks Sudoku solving
 * technique, based on https://www.sudopedia.org/wiki specification.
 * <p>
 * See the text version of above article in ./GEM.txt but please note that I
 * can't make heads-nor-tails of it. My mental picture seems at odds with the
 * authors, such that I can't make his explanation make any bloody sense what
 * so ever; which should be perceived as an admission of a mental weakness on
 * my part, rather than a criticism of the publication. So in the end I gave-up
 * trying to implement the full GradedEquivalenceMarks and just did what makes
 * sense to me, which is a basically MedusaColoring.
 * It's a lot like building a shopping mall as an extension to your pool dunny,
 * and then going to K-Mart anyway. sigh.
 * Developers please note that somebody who understands the spec may well get
 * many more eliminations by totally implementing the spec. I'm too stoopid!
 * <p>
 * My implementation of GEM uses a different mark-up to the specification:
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
 * colors                           bidirectional links
 * ons      +           +           set when color is true
 * offs     -           -           eliminated for this color
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
 * @stretch: I'm still using the old XColoringHintMulti, but should show marks,
 * eliminations, and SetPots to the user, especially for +contradictions.
 * </pre>
 *
 * @author Keith Corlett 2021-03-17
 */
public class GEM extends AHinter implements IPreparer
//		, diuf.sudoku.solver.IReporter
{
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
	private static final boolean CHECK_PROMOS = false;
//	// should we Log s__t when we CHECK_PROMOS?
//	private static final boolean PROMO_NOISE = true;

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
	// An idx containing each cells indice.
	private static final Idx[] CELL_IDX = new Idx[81];
	static {
		for ( int i=0; i<81; ++i )
			CELL_IDX[i] = new Idx().add2(i);
	}

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
	private class UncleanException extends RuntimeException {
		private static final long serialVersionUID = 64099692026011L;
		public UncleanException(String msg) {
			super(msg);
		}
	}

	/**
	 * ValueScore is the score for each value; only used in the startingValues
	 * method, to keep the value WITH it's score, so that we can sort the array
	 * by score descending and keep the value.
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
	}

	/**
	 * Create the array of arrays which is a colors, or supers, subs.
	 * @param array
	 */
	private static void createArray(Idx[][] array) {
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				array[c][v] = new Idx();
	}

	/**
	 * Clear all the Idx's in the given array (both colors, all values).
	 */
	private static void clear(Idx[][] array) {
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				array[c][v].clear();
	}

	/**
	 * Clear all the Idx's in the given array (both colors, all values).
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
	private static final Comparator<ValueScore> VALUE_SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

	/**
	 * promotions needs to know the values that have been eliminated from each
	 * cell in this color.
	 */
	private static final int[] OFF_VALUES = new int[81];

	/**
	 * for confirmations: an Idx of redPots (eliminations) by value 1..9
	 */
	private static final Idx[] REDS = new Idx[10];
	static {
		for ( int v=1; v<10; ++v )
			REDS[v] = new Idx();
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
	private Idx[] candidates;

	// A conjugatePair is the ONLY two cells in a region which maybe value
	private final Cell[] conjugatePair = new Cell[2];
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

	// all values of each color; array to swap between colors.
	private final Idx[] coloredCells = {new Idx(), new Idx()};
	// temporary Idxs used ONLY in paint: set, read, and forget!
	private final Idx tmpP1 = new Idx();
	// temporary Idxs: set, read, and forget!
	private final Idx tmp1 = new Idx()
					, tmp2 = new Idx()
					, tmp3 = new Idx()
					, tmp4 = new Idx();
	// a bitset of those values which have been painted either color
	private int paintedValues;
	// a bitset of the GREEN and BLUE values; array to swap between colors.
	private final int[] colorValues = new int[2];
	// the Cell=>Values to eliminate. A field to not create for 99% failure.
	private final Pots redPots = new Pots();

	// the (presumably) "good" color to display "results" in the hint.
	private int goodColor;
	// Contradictions only: the cell/s which caused this hint.
	private Set<Cell> cause;
	// Type 2 only: the region containing the cells which caused this hint,
	// to be bordered in pink, to help the user find the bastard.
	private ARegion region;

	// a StringBuilder of the steps required to building the coloring-chains;
	// and now also the conclusions: Contradictions or Eliminations.
	private final StringBuilder steps = new StringBuilder(STEPS_SIZE);

	/**
	 * The constructor.
	 */
	public GEM() {
		super(Tech.GEM);
		// create the colors, supers, and subs array-of-arrays of indices.
		createArray(colors);
		createArray(ons);
		createArray(colorsOns);
		createArray(offs);
		// We want explanation in GUI and testcases, just not in the batch.
		// nb: building all the "why" strings slows batch down by a MINUTE!
		this.wantWhy = Run.type != Run.Type.Batch;
	}

	/**
	 * Prepare is called after puzzles loaded, but BEFORE the first findHints.
	 * @param gridUnused
	 * @param logicalSolverUnused
	 */
	@Override
	public void prepare(Grid gridUnused, LogicalSolver logicalSolverUnused) {
		isEnabled = true; // re-enable me after I've gone DEAD_CAT
	}

//	@Override
//	public void report() {
//		Log.teef("\n%s: minContradiction=%d, minConfirmation=%d, minElimination=%d\n"
//			, getClass().getSimpleName(), minContradiction, minConfirmation, minElimination);
//	}
//	private int minContradiction = Integer.MAX_VALUE;
//	private int minConfirmation = Integer.MAX_VALUE;
//	private int minElimination = Integer.MAX_VALUE;

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
		// DEAD_CAT disables a hinter DURING solve (which only does disabled
		// BEFORE kick-off) so each hinter that's ever gone DEAD_CAT checks
		// isEnabled itself. I'm re-enabled by the prepare method, so I'm down
		// for this puzzle only. I'll get you next time Batman.
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		this.candidates = grid.getIdxs();
		// presume that no hint will be found
		boolean result = false;
		try {
			// Step 1: foreach value in 2-or-more conjugate relationships,
			//         order by num conjugates + num bivalues DESCENDING.
			// WARNING: startingValues filters to score >= 7 coz that's the
			// lowest score that hints in top1465. THIS MAY BE WRONG!!!
			for ( int v : startingValues() )
				for ( ARegion r : grid.regions )
					if ( r.indexesOf[v].size == 2
					  && (result|=search(r, v)) && onlyOne )
						return result;
		} catch ( UncleanException ex ) {
			// USE_PROMOTIONS -> create[Big]Hint: bad elim or cell-value.
			Log.teeln("WARN: GEM: "+ex.getMessage());
			isEnabled = false; // disable me for this puzzle only
		} catch ( StopException meh ) {
			// thrown after a contradiction/confirmation hint is added to accu
			result = true;
		} finally {
			this.grid = null;
			this.accu = null;
			this.candidates = null;
			this.cause = null;
			this.region = null;
		}
		return result;
	}

	/**
	 * To select the best starting value, pick a value that has 2 or more
	 * conjugate pairs, and as many candidates as possible in bivalue cells.
	 * These bivalue cells allow us to expand the coloring clusters beyond
	 * the single digit boundary.
	 */
	private int[] startingValues() {
		// reset scores
		final ValueScore[] scores = new ValueScore[10]; // score of values 1..9
		for ( int i=0; i<10; ++i ) // include zero to not upset sort
			scores[i] = new ValueScore(i);
		// count conjugate pairs for each value
		// build-up a bitset of those values that're in 2+ conjugate pairs
		int cands = 0;
		for ( int v=1; v<10; ++v )
			for ( ARegion r : grid.regions )
				if ( r.indexesOf[v].size == 2
				  && ++scores[v].score > 1 )
					cands |= VSHFT[v];
		// foreach bivalue cell
		// increment score of each value with 2+ conjugate pairs
		for ( Cell c : grid.cells )
			if ( c.maybes.size == 2 )
				for ( int v : VALUESES[c.maybes.bits & cands] )
					++scores[v].score;
		// order by score descending
		Arrays.sort(scores, VALUE_SCORE_DESCENDING);
		// count scores with a score of atleast 7. The minimum-possible minimum
		// score is 3, but I use 7 coz it's top1465's minimum-that-hints.
		// I have no theoretical basis for 7, it's just what works FOR ME!
		// I'm presuming it'll work for ALL Sudoku puzzles, but it may not!
		int n;
		for ( n=0; n<10; ++n )
			if ( scores[n].score < 7 )
				break;
		// then read-off the values, by score descending
		final int[] array = new int[n];
		for ( int i=0; i<n; ++i )
			array[i] = scores[i].value;
		return array;
	}

	/**
	 * search this region and value (a conjugate pair) for Medusa 3D Coloring
	 * hints.
	 *
	 * @param r the region to search
	 * @param v the value to search for
	 * @return were any hint/s found; noting only one Type 2 is ever found.
	 */
	private boolean search(ARegion r, int v) {
		AHint hint;
		int subtype;
		final int n;
		// presume that no hint will be found
		boolean result = false;
		// clear my fields
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
		r.at(r.indexesOf[v].bits, conjugatePair);
		try {
			// Paint the cell, and it's consequences.
			paint(conjugatePair[0], v, GREEN, true, "Let us assume that "
				+conjugatePair[0].id+"-"+v+" is "+CCOLORS[GREEN]+NL);
			// on/paint any more consequences (and there consequences, ...)
			while ( paintMonoBoxs() | paintPromotions() ) {
				// Do nothing, just go again.
			}
			// clear the hint details
			subtype = 0;
			// Step 5: Analyze the cluster.
			// 5.1 If either color contains a contradiction
			//     then the opposite color MUST be true.
			// sum the total number of cell-values colored
			n = squash(GREEN, coloredCells[GREEN])
			  + squash(BLUE, coloredCells[BLUE]);
			// GEM: minContradiction=2, minConfirmation=11, minElimination=5
			// NOTE: Minimum painted limits are top1465 specific. There is no
			// justification for them. They may be too high for other puzzles.
			// You can tune them to your puzzles, or take them out completely.
// minContradiction=2
// No point testing when n==2 coz it's always 2+ here.
// Yes, painting just 2 cells produces a contradiction! It's all in the +'s.
// If you press delete on the GEM hint you'll find it's a Finned Swampfish.
//			if ( n > 1 ) {
				for ( int c=0; c<2; ++c ) {
					if ( (subtype=contradictions(c)) != 0
					  // presuming opposite color isn't rooted too
					  && (hint=createBigHint(v, subtype)) != null
					  // validate the hint if HintValidator is enabled
					  && validSetPots("Contradiction", hint) ) {
						result = true;
//						if ( n < minContradiction )
//							minContradiction = n;
						accu.add(hint);
						// contradiction is always the last hint I produce.
						throw STOP_EXCEPTION;
					}
				}
//			}
			if ( n > 4 ) { // minElimination=5
				// 5.2 If any uncolored cell-value sees both colors
				//     then it can be eliminated.
				if ( subtype==0 && (subtype=eliminations())!=0 ) {
					// search eliminations for confirmation that color is true.
					int mst; // multi-hint subtype
					hint = null;
					// minConfirmation=11
					if ( n > 10 && (mst=confirmations()) != 0
					  && (hint=createBigHint(v, mst)) != null ) {
						if ( validSetPots("Confirmation", hint) ) {
							result = true;
//							if ( n < minConfirmation )
//								minConfirmation = n;
							accu.add(hint);
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
//						if ( n < minElimination )
//							minElimination = n;
						if ( accu.add(hint) )
							return result;
					}
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
	 * Paint this cell this color, and paint it's consequences recursively.
	 * <p>
	 * WARNING: You must check that the cell-value is not already painted this
	 * color BEFORE you call paint. It's a performance thing with why String.
	 * <p>
	 * The paint method implements Steps 2 to 4 of ./Coloring.txt using a
	 * recursive DFS, where the instructions specify a BFS, without saying so
	 * explicitly, which is a BIG warning sign. Experienced programmers prefer
	 * DFS for speed, but newbs think BFS for simplicity. Being an experienced
	 * programmer (ergo an arrogant prick), I have countermanded the spec.
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any region containing this cell except the given region (nullable)
	 *    has two places for this value then paint the otherCell the opposite
	 *    color; and recursively paint the consequences thereof.
	 * 3. If biCheck and cell is bivalue then paint the otherValue the opposite
	 *    color; and recursively paint the consequences thereof.
	 * </pre>
	 * Paint populates the two colors: GREEN and VLUE, by value;
	 * and also populates the paintedValues bitset.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY in the GUI + testcases
	 * when we might want to use it as part of the HTML to display to the user.
	 * In the batch (LogicalSolverTester) and in generate its just a waste of
	 * time. MedusaColoringTest compares the full HTML.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY when used: so we always
	 * pretest "is this value already painted this color" BEFORE building the
	 * why String, even in the GUI, because it's slow: a MINUTE per top1465.
	 * IMPROVED: Unsure if the slow part is building each why String, or append
	 * to steps, so Steps is now a StringBuilder, clear with steps.setLength(0)
	 * instead of growing a new buffer EACH AND EVERY time with steps += why.
	 * IMPROVED: The wantWhy field is set ONCE in the constructor.
	 * <p>
	 * THINK AGAIN: run paint once silently and if hint found and we're in the
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
	 * @param why a one-liner on why this cell-value is painted this color.
	 * @return any
	 * @throws OverpaintException when a cell-value is painted in both colors.
	 */
	private boolean paint(Cell cell, int v, int c, boolean biCheck, String why)
			throws OverpaintException {
		// If cell-value is already painted the opposite color then throw!
		if ( colors[OPPOSITE[c]][v].contains(cell.i) )
			throw new OverpaintException(cell.id+"-"+v+" is already "+COLORS[OPPOSITE[c]]);
		int otherValue; // the other value
		Cell otherCell; // the other cell
		final int o = OPPOSITE[c]; // the other color
		final int i = cell.i; // indice (to not dereference it repeatedly)

		// 1. Paint the given cell-value this color
		colors[c][v].add(i);
		paintedValues |= colorValues[c] |= VSHFT[v];
		// colors overwrites ons
		ons[c][v].remove(i);
		// colors overwrites the opposite offs
		offs[o][v].remove(i);
		// remember the steps in this coloring-chain (steps is a StringBuilder)
		if ( why != null ) {
			steps.append(why);
			why = null;
		}

		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r2 : cell.regions ) {
			// nb: pre-check faster than needlessly building the why string!
			if ( r2.indexesOf[v].size == 2 ) {
				if ( !colors[o][v].contains((otherCell=r2.otherThan(cell, v)).i) ) {
					if ( wantWhy )
						why = CON[c]+cell.id+"-"+v+COFF[c]
							+" conjugate in "+r2.id+" is "
							+CON[o]+otherCell.id+"-"+v+COFF[o]+NL;
					// paint otherCell-v the opposite color, recursively.
					paint(otherCell, v, o, true, why);
				}
			} else {
				// off other places for value in region (except colors/ons).
				offs[c][v].or(tmpP1.setAndNot(r2.idxs[v], CELL_IDX[i])
				  .andNot(colors[o][v]).andNot(ons[o][v]));
			}
		}

		// 3. Paint the other value of this bivalue cell the opposite color
		// skip this bi-check when we're painting "the other value"
		if ( biCheck ) {
			if ( cell.maybes.size == 2 ) {
				// nb: pre-check faster than needlessly building the why string!
				if ( !colors[o][otherValue=FIRST_VALUE[cell.maybes.bits & ~VSHFT[v]]].contains(i) ) {
					if ( wantWhy )
						why = CON[c]+cell.id+"-"+v+COFF[c]
							+" only other value is "
							+CON[o]+cell.id+"-"+otherValue+COFF[o]+NL;
					// paint cell-otherValue the opposite color, recursively,
					// but skip the bi-check.
					paint(cell, otherValue, o, false, why);
				}
			} else {
				// off all other values of this cell.
				for ( int ov : VALUESES[cell.maybes.bits & ~VSHFT[v]] )
					if ( !colors[o][ov].contains(i)
					  && !ons[o][ov].contains(i) )
						offs[c][ov].add(i);
			}
		}
		return true;
	}

	/**
	 * A "mono-box" is a box with only one place for v (a hidden single), which
	 * also leaves the "causal box" with one place for v, constituting a strong
	 * bidirectional link. If the link cannot be proven bidirectional then an
	 * "on" is created, which allows the cell value to be set in the event that
	 * this color is ALL true, but takes no part in any further logic.
	 * <p>
	 * paintMonoBoxs differs from promotions in it's ability to discern the
	 * strong link between these two cells. Promotions just "ons" it, leaving
	 * it's promotion to chance, where I can field promote it.
	 * <p>
	 * The "10 paces" rule (shoot back): We start with a cell in the grid that
	 * is painted this color, called the source cell. We examine each of the 4
	 * boxs effected by the source cell (the 2 left/right, and 2 above/below).
	 * If the effected box, minus buddies of all cells painted dis color leaves
	 * ONE cell (a Hidden Single) in the effected box then treat it, but how:
	 * colors or ons?
	 * <p>
	 * We "shoot back": if the box containing the source cell, minus buddies of
	 * the cell to paint, leaves ONE cell in the box containing the source cell
	 * then paint the suspect because there's a bidirectional relationship
	 * (a "strong" link) between the suspect and the source cell:<br>
	 * {@code if A is v then B is v AND CONVERSELY if B is v then A is v}<br>
	 * so we can paint B this color, else we just "on" the little bastard.
	 * <p>
	 * Note that when shooting back we cannot minus the buddies of (cells of
	 * this color minus the source cell). I tried, and it doesn't work, but I
	 * don't understand why it doesn't work. Mine is not to reason why, mine
	 * is just to do or die. But why? Or maybe I just ____ed up?
	 *
	 * @return any
	 */
	private boolean paintMonoBoxs() {
		Cell c1; // the source cell
		Cell c2; // the cell to paint, if any
		ARegion b1; // the box containing the source cell
		ARegion b2; // the box containing the cell to paint
		String why = null; // the reason we paint this cell-value this color
		boolean any = false; // where any painted
		boolean first = true; // is this the first painted or onned
		// foreach color
		for ( int c=0; c<2; ++c ) { // GREEN, BLUE
			final Idx[] thisColor = colors[c];
			// foreach value which has a cell painted thisColor
			for ( int v : VALUESES[colorValues[c]] ) {
				// foreach cell painted thisColor (the source cell)
				// i1 is the indice of the source cell
				for ( int i1 : thisColor[v].toArrayA() ) {
					// foreach of the four boxes effected by this source cell.
					// r2 is the index in grid.regions of the effected box.
					for ( int r2 : INDEXES[EFFECTED_BOXS[BOXS[i1]]] ) {
						// if there's only one v remaining in the effected box,
						// excluding this colors cells + there buddies.
						// Get buds() for each i coz paint mods thisColor[v],
						// and plusBuds has cache, so its fast anyways.
						if ( tmp1.setAndNot(grid.regions[r2].idxs[v], thisColor[v].plusBuds()).any()
						  && tmp1.size() == 1
						) {
							c2 = grid.cells[tmp1.peek()]; // the cell to paint
							b2 = c2.box; // the effected box
							c1 = grid.cells[i1]; // the source cell
							b1 = c1.box; // the box containing the source cell
						    // the cell to paint needs a buddy in the box that
						    // contains the source cell, else forget it (c2 is
							// a Hidden Single from buddies of colors, with no
							// help from the cell to paint. NOT what we want).
							if ( tmp2.setAndAny(b1.idxs[v], c2.buds)
							  // and the "10 paces" rule (shoot back).
							  && tmp2.setAndNot(b1.idxs[v], c2.buds).size() == 1
							) {
								// a "strong" bidirectional link.
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										why = NL+"<u>Mono Boxs</u>"+NL;
									} else
										why = "";
									why += CON[c]+c1.id+"-"+v+COFF[c]
									+" leaves "+c2.id+" only "+v+" in "+b2.id
									+", which leaves "+c1.id
									+" only "+v+" in "+c1.box.id
									+", so "+CON[c]+c2.id+"-"+v+COFF[c]+NL;
								}
								any |= paint(c2, v, c, false, why);
							} else if ( !ons[c][v].contains(c2.i) ) {
								// a "weak" mono-directional link.
								if ( wantWhy ) {
									if ( first ) {
										first = false;
										steps.append(NL).append("<u>Mono Boxs</u>").append(NL);
									}
									steps.append(CON[c]).append(c1.id)
									  .append("-").append(v).append(COFF[c])
									  .append(" leaves ").append(c2.id)
									  .append(" only ").append(v)
									  .append(" in ").append(b2.id)
									  .append(", so $").append(CON[c])
									  .append(c2.id).append("+").append(v)
									  .append(COFF[c]).append(NL);
								}
								ons[c][v].add(c2.i);
							}
						}
					}
				}
			}
		}
		return any;
	}

	/**
	 * Squash all values of this color into result.
	 *
	 * @param c the color: GREEN or BLUE
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
	 *    a) The only un-off'ed value in a cell (all this color) is an "on".
	 *    b) The only un-off'ed occurrence of a value in a region (all this
	 *       color) is an "on".
	 * 2. An "on" seeing an opposite color sibling or cell mate gets painted.
	 *    a) If an "on" sees opposite color (same value) then paint it.
	 *    b) If an "on" has an opposite colored cell-mate (any value) paint it.
	 * </pre>
	 * @return any
	 */
	private boolean paintPromotions() {
		Cell cell;
		int c, o;
		String why = null;
		// presume that no cell-values will be promoted
		boolean any = false;
		boolean first = true;
		// 1a. If all bar one v is eliminated from a cell (all one color)
		//     then it's an On of that color.
		for ( c=0; c<2; ++c ) {
			rebuildOffValues(c);
			for ( int i=0; i<81; ++i ) {
				if ( (cell=grid.cells[i]).maybes.size == VSIZE[OFF_VALUES[i]] + 1 ) {
					int v = FIRST_VALUE[cell.maybes.bits & ~OFF_VALUES[i]];
					if ( !colors[c][v].contains(i)
					  && !ons[c][v].contains(i) ) {
						if ( wantWhy ) {
							if ( first ) {
								first = false;
								steps.append(NL).append("<u>Promotions</u>").append(NL);
							}
							steps.append("last value $").append(CON[c])
							  .append(cell.id).append("+").append(v)
							  .append(COFF[c]).append(NL);
						}
						ons[c][v].add(cell.i);
					}
				}
			}
		}
		// 1b. If all bar one v is eliminated from a region (all one color)
		//     then it's an On of that color.
		{
			int i;
			for ( c=0; c<2; ++c ) {
				for ( int v : VALUESES[colorValues[c]] ) {
					for ( ARegion r : grid.regions ) {
						if ( tmp1.setAndAny(offs[c][v], r.idx)
						  && tmp2.setAndNot(r.idxs[v], tmp1).size() == 1
						  && !colors[c][v].contains(i=tmp2.peek())
						  && !ons[c][v].contains(i)
						) {
							cell = grid.cells[i];
							if ( wantWhy ) {
								if ( first ) {
									first = false;
									steps.append(NL).append("<u>Promotions</u>").append(NL);
								}
								steps.append("last place in ").append(r.id)
								  .append(" $").append(CON[c]).append(cell.id)
								  .append("+").append(v).append(COFF[c])
								  .append(NL);
							}
							ons[c][v].add(cell.i);
						}
					}
				}
			}
		}
		// 2a. If an on sees (same v, OPPOSITE color) a colors then paint it.
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v=1; v<10; ++v ) {
				if ( ons[c][v].any() ) {
					for ( int i : ons[c][v].toArrayA() ) {
						if ( tmp1.setAndAny(BUDDIES[i], colors[o][v])
						  && !colors[c][v].contains(i) ) {
							cell = grid.cells[i];
							if ( wantWhy )
								why = "sees a "+CCOLORS[o]+" "
									+ CON[c]+cell.id+"-"+v+COFF[c]+ NL;
							any |= paint(cell, v, c, T, why);
						}
					}
				}
			}
		}
		// 2b. If an on has an OPPOSITE colored cell-mate (of another value)
		//     then paint it.
		squash(GREEN, coloredCells[GREEN]);
		squash(BLUE, coloredCells[BLUE]);
		for ( c=0; c<2; ++c ) {
			o = OPPOSITE[c];
			for ( int v : VALUESES[colorValues[c]] ) {
				if ( tmp2.setAndAny(ons[c][v], coloredCells[o]) ) {
					for ( int i : tmp2.toArrayA() ) {
						// cheeky way to do other color has another value:
						// if cell-in-other-color (already done above)
						// and not other-color-this-value.contains(cell)
						if ( !colors[o][v].contains(i)
						  // and pre-check that it's not already painted
						  && !colors[c][v].contains(i) ) {
							cell = grid.cells[i];
							if ( wantWhy )
								why = "has a "+CCOLORS[o]+" "
									+ CON[c]+cell.id+"-"+v+COFF[c]+NL;
							any |= paint(cell, v, c, T, why);
						}
					}
				}
			}
		}
		return any;
	}

	/**
	 * Rebuild OFF_VALUES: the values eliminated by the given color from each
	 * cell in the grid, as a bitset.
	 *
	 * @param c the color: GREEN or BLUE
	 */
	private void rebuildOffValues(int c) {
		Arrays.fill(OFF_VALUES, 0);
		for ( int v : VALUESES[colorValues[c]] ) {
			final int sv = VSHFT[v];
			offs[c][v].forEach((i)->OFF_VALUES[i] |= sv);
		}
	}

	/**
	 * Does colors[c] contain a contradiction. First of:<pre>
	 * * If two of any combo of greens and green-ons in a cell then blue true.
	 * * If any combo of greens and green-ons (same value) in region then blue.
	 * * RARE: If green off's all of a cells values then blue is true.
	 * * RARE: If all v's in a region see a green v then blue is true.
	 * </pre>
	 * The use of ons in contradictions is worthy of some explanation. The ons
	 * are true if the color is true, right? Well that means that if any of the
	 * ons are invalid then the whole color is false; so we can safely use the
	 * ons in contradictions (but NOT in confirmations).
	 *
	 * @param c color index, GREEN or BLUE
	 * @return The subtype of hint to be created:<pre>
	 * 0: meaning none.
	 * 2: 2+ green values/ons in cell (261)
	 * 3: 2+ green v's/ons in region (168)
	 * 4: green off's all of a cells values (9)
	 * 5: All v's in region see a green v (2)
	 * NOTE: 6 and 7 are confirmations (not mine)
	 * </pre>
	 */
	private int contradictions(final int c) {
		Cell cell; // cell to hint on
		Idx buds; // buddies of thisColor[v]
		final Idx[] thisColor = colors[c];
		final Idx[] thisOns = ons[c];
		final Idx[] thisColorOns = colorsOns[c];
		final Idx[] thisOffs = offs[c];
		final int[] thisOffCnt = count(thisOffs);
		final int o = OPPOSITE[c];
		// presume neither color is good (an invalid value)
		goodColor = -1;

		// repopulate colorsOns[c]
		for ( int v=1; v<10; ++v )
			thisColorOns[v].setOr(thisColor[v], thisOns[v]);
		final int thisColorOnsValues = values(thisColorOns);

		// If any combo of greens and ons (different values) in cell
		// then blue is true.
		for ( int v1 : VALUESES[thisColorOnsValues] ) {
			// foreach color/on value EXCEPT v1
			for ( int v2 : VALUESES[thisColorOnsValues & ~VSHFT[v1]] ) {
				if ( tmp1.setAndAny(thisColorOns[v1], thisColorOns[v2]) ) {
					cell = grid.cells[tmp1.peek()];
					cause = Grid.cellSet(cell);
					goodColor = o;
					if ( wantWhy )
						steps.append(NL).append("<u>Contradiction</u>").append(NL)
						  .append("<k>").append(cell.id).append("</k>")
						  .append(" has ").append(CON[c]).append(COLORS[c]).append(" ")
						  .append(v1).append(" and ").append(v2).append(COFF[c])
						  .append(", so ").append(CCOLORS[o]).append(" is true.").append(NL);
					return SUBTYPE_2;
				}
			}
		}

		// If any combo of greens and ons (same value) in region
		// then blue is true.
		for ( int v : VALUESES[thisColorOnsValues] ) {
			for ( ARegion r : grid.regions ) {
				if ( r.indexesOf[v].size > 1
				  && tmp2.setAndMultiple(thisColorOns[v], r.idx) ) {
					cause = tmp2.toCellSet(grid);
					goodColor = o;
					if ( wantWhy )
						steps.append(NL)
						  .append("<u>Contradiction</u>").append(NL)
						  .append("<k>").append(r.id).append("</k>")
						  .append(" has multiple ").append(CON[c]).append(v).append("'s/+'s").append(COFF[c])
						  .append(", so ").append(CCOLORS[o]).append(" is true.")
						  .append(NL);
					return SUBTYPE_3;
				}
			}
		}

		// RARE: If green off's all of a cells values then blue is true.
		for ( int i=0; i<81; ++i )
			if ( thisOffCnt[i] > 0
			  && thisOffCnt[i] == grid.cells[i].maybes.size ) {
				cell = grid.cells[i];
				cause = Grid.cellSet(cell);
				goodColor = o;
				if ( wantWhy )
					steps.append(NL)
					  .append("<u>Contradiction</u>").append(NL)
					  .append("<k>").append(cell.id).append("</k>")
					  .append(" all values are ").append(CON[c]).append("-").append(COFF[c])
					  .append(", so ").append(CCOLORS[o]).append(" is true.")
					  .append(NL);
				return SUBTYPE_4;
			}

		// RARE: If all v's in a region see a green v then blue is true.
		for ( int v : VALUESES[colorValues[c]] ) {
			buds = thisColor[v].buds();
			for ( ARegion r : grid.regions ) {
				if ( r.indexesOf[v].size > 1
				  && tmp1.setAnd(buds, r.idx).equals(r.idxs[v]) ) {
					region = r;
					cause = tmp1.toCellSet(grid);
					goodColor = o;
					if ( wantWhy )
						steps.append(NL)
						  .append("<u>Contradiction</u>").append(NL)
						  .append("<k>").append(r.id).append("</k>")
						  .append(" all ").append(v).append("'s see ")
						  .append(CON[c]).append(v).append("'s").append(COFF[c])
						  .append(", so ").append(CCOLORS[o]).append(" is true.")
						  .append(NL);
					return SUBTYPE_5;
				}
			}
		}

		return 0;
	}

	/**
	 * Get a bitset of all of the values in these idxs.
	 * @param idxs ons[GREEN] or ons[BLUE]; or colors, offs, ...
	 * @return a bitset of ons values
	 */
	private int values(final Idx[] idxs) {
		int cands = 0;
		for ( int v=1; v<10; ++v )
			if ( idxs[v].any() )
				cands |= VSHFT[v];
		return cands;
	}

	/**
	 * Count the number of values which include each cell in the grid.
	 */
	private int[] count(final Idx[] idxs) {
		Arrays.fill(counts, 0);
		for ( int v=1; v<10; ++v )
			if ( idxs[v].any() )
				idxs[v].forEach((i)->++counts[i]);
		return counts;
	}
	private final int[] counts = new int[81];

	/**
	 * Do a post-eliminations confirmation (opposite of contradiction) search
	 * to promote an elimination hint to a multi hint: setting all cell values
	 * in a color (and the ons).
	 * <pre>
	 * (a) If all bar one value is eliminated from a cell, and the remaining
	 *     cell-value is colored then that whole color is true. (2)
	 * (b) when all but one value is eliminated from a region and the remaining
	 *     cell-value is colored then that whole color is true. (0)
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
	 *
	 * @return 0=none, 4="Type (a)", 5="Type (b)". This value is up-shifted to
	 *  differentiate it from existing XColoringHintMulti subtypes.
	 */
	private int confirmations() {
		Cell cell;
		Values vals;
		// a) all bar one value is actually eliminated from a cell and that
		//    cell-value is colored then ALL of this color is true
		for ( int c=0; c<2; ++c ) {
			for ( int v : VALUESES[colorValues[c]] ) {
				for ( int i : colors[c][v].toArrayA() ) {
					if ( (vals=redPots.get(cell=grid.cells[i])) != null
					  && vals.size == cell.maybes.size - 1
					) {
						if ( wantWhy )
							steps.append(NL)
							  .append("<u>Confirmation</u>").append(NL)
							  .append(CON[c]).append(cell.id).append("-").append(v).append(COFF[c])
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
// top1465 contains 0 occurrences, so either my code is wrong or there really
// are none of these on top1465. So, can you spot any mistakes in this code?
		if ( false ) {
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
									  .append(CON[c]).append(grid.cells[i].id).append("-").append(v).append(COFF[c])
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
	 * Create a new "big" XColoringHintMulti to set a whole colors cell values.
	 * <p>
	 * NOTE: I'm still using the XColoringHintMulti coz all that's required is
	 * to add the ons to setPots and the rest of this mess remains unchanged;
	 * so I ignore the offs justifying the additional cells set by ons rather
	 * than writing a new GEMHintMulti which shows the ons/offs because I am
	 * a lazy bastard. It'd be better done well, but it's OK with just this.
	 *
	 * @param value the potential value to create hint for.
	 * @param subtype the subtype of the hint to create.
	 * @return a new XColoringHintMulti
	 */
	private AHint createBigHint(int value, int subtype) {
		final Pots setPots; // the cell-values to be set
		try {
			setPots = squishSetPots(goodColor);
			addOns(setPots);
			if ( CHECK_PROMOS ) // deal with consequent dodgy hints
				if ( !cleanSetPots(setPots) )
					return null; // none remain when invalid sets removed
		} catch ( Pots.IToldHimWeveAlreadyGotOneException ex ) {
			return null; // attempted to set 1 cell to 2 values.
		}
		AHint hint = new GEMHintMulti(this, value, redPots, subtype, cause
			, goodColor, steps.toString(), setPots, pots(GREEN), pots(BLUE)
			, region, ons, offs);
		cause = null;
		region = null;
		return hint;
	}

	/**
	 * Add any ons to the given setPots, as populated from colors.
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
		Cell cc;	// currentCell: the cell to eliminate from
		int g		// green value/s bitset
		  , b		// blue value/s bitset
		  , pinks	// bitset of "all other values" to eliminate
		  , c		// color: the current color: GREEN or BLUE
		  , o;		// opposite: the other color: BLUE or GREEN
		int subtype = 0; // presume none
		boolean first = true; // is this the first elimination

		// (a) Both colors in one cell eliminates all other values.
		// nb: all[GREEN] and all[BLUE] pre-prepared in search (above)
		for ( int i : tmp1.setAnd(coloredCells[GREEN]
				                , coloredCells[BLUE]).toArrayA() ) {
			if ( grid.cells[i].maybes.size > 2
			  // get a bitset of all values of cells[i] that're green and blue.
		      // There may be none, never multiple (contradictions pre-tested)
			  // We need 1 green value and 1 blue value, to strip from pinks.
			  // NOTE: VSIZE[0] == 0.
			  && VSIZE[g=values(GREEN, i)] == 1
			  && VSIZE[b=values(BLUE, i)] == 1
			  // ensure that g and b are not equal (should NEVER be equal)
			  && g != b
			  // pinks is a bitset of "all other values" to be eliminated
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
		for ( int v : VALUESES[paintedValues] ) {
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
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
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
				for ( c=0; c<2; ++c ) {
					// if cells[ii] sees a this-color v
					if ( tmp3.setAndAny(BUDDIES[ii], colors[c][v])
					  // and the opposite color (any value) contains ii
					  && coloredCells[OPPOSITE[c]].contains(ii)
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
							  .append(" has ").append(CON[o]).append(otherV).append(COFF[o])
							  .append(" and sees ").append(CON[c]).append(sib.id).append("-").append(v).append(COFF[c])
							  .append(", so ").append(cc.id).append(" can't be <r>").append(v).append("</r>.")
							  .append(NL);
						}
						subtype |= 4;
					}
				}
			}
		}

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
	 * cell-of-this-color then logic holds up; it's just nice to show the user
	 * the closest-cell-of-this-color; that's all.
	 *
	 * @param idx {@code tmp3.setAndAny(BUDDIES[ii], colors[c][v])} the buddies
	 * of the target cell which are painted this color.
	 * @param target {@code cc = grid.cells[ii]} the target bloody cell.
	 * @return the closest cell (or null if I'm rooted)
	 */
	private Cell closest(Idx idx, Cell target) {
		Cell closest = null;
		int minD = Integer.MAX_VALUE; // the distance to the closest cell
		for ( int i : idx.toArrayB() ) {
			final Cell c = grid.cells[i];
			final int y = Math.abs(c.y - target.y);
			final int x = Math.abs(c.x - target.x);
			final int distance;
			if ( y==0 )
				distance = x;
			else if (x==0)
				distance = y;
			else // Pythons theorum: da square of ye hippopotamus = da sum of
				// da squares of yon two sides, except in ye gannetorium where
				// it remains indeterminate; mainly because of da bloody smell.
				distance = (int)Math.sqrt((double)(x*x + y*y));
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
	 * @return a new XColoringHint (for eliminations)
	 */
	private AHint createHint(int value, int subtype) {
		if ( CHECK_PROMOS ) // deal with consequent dodgy hints
			if ( !cleanRedPots() )
				return null;
		final Idx[] colorSet = null;
		AHint hint = new GEMHint(this, value, new Pots(redPots)
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
		final int[] solution = HintValidator.getSolutionValues(grid);
		Cell cell;
		Values values;
//		boolean first = true;
		for ( Iterator<Cell> it=setPots.keySet().iterator(); it.hasNext(); ) {
			if ( (values=setPots.get(cell=it.next())).first() != solution[cell.i] ) {
// unforgiving
				throw new UncleanException("Invalid SetPot: "+cell.id+"+"+values.first()+"!="+solution[cell.i]);
// forgiving
//				if ( PROMO_NOISE )
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
		final int[] solution = HintValidator.getSolutionValues(grid);
		Cell cell;
		Values values;
//		// is this the first invalid elimination?
//		boolean first = true;
		// use an iterator to delete from it
		for ( Iterator<Cell> it=redPots.keySet().iterator(); it.hasNext(); ) {
			values = redPots.get(cell = it.next());
			for ( int v : VALUESES[values.bits] )
				if ( solution[cell.i] == v ) {
// unforgiving
					throw new UncleanException("Invalid Elimination: "+cell.id+"-"+v+" is this cells value!");
// forgiving
//					if ( PROMO_NOISE )
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
