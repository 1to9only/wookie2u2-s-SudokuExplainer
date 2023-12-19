/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2023 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import static diuf.sudoku.Constants.BLUE;
import static diuf.sudoku.Constants.GREEN;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.EFFECTED_BOXS;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Grid.BOX_OF;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.COL_OF;
import static diuf.sudoku.Grid.MAYBES;
import static diuf.sudoku.Grid.ROW_OF;
import static diuf.sudoku.Grid.VALUE_CEILING;
import diuf.sudoku.IdxC;
import static diuf.sudoku.Indexes.IFIRST;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.hinters.AHinter;
import static diuf.sudoku.solver.hinters.color.Words.*;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import diuf.sudoku.solver.hinters.IPrepare;
import diuf.sudoku.utils.IntQueue;

/**
 * Medusa3D implements the {@link Tech#Medusa3D} Sudoku solving technique, as
 * described in the https://www.sudopedia.org/wiki/3D_Medusa specification.
 * Medusa3D is extended extended coloring.
 * <p>
 * See ./#Medusa3D.txt for annoted specification.
 * <p>
 * This implementation also includes one extension of the above specification.
 * It finds a naked single in an effected box, which leaves a naked single in
 * the source box... which I call the "shoot back" rule. Search for that.
 * <p>
 * Note that Medusa3D hijacks XColorings hint types.
 * <p>
 * Note that I do not want Medusa3D, because it finds many of the same hints as
 * {@link GEM GEM (Graded Equivalence Marks)} but sets less cells therefrom, so
 * the Medusa3D is counter-productive with GEM. You <u>can</u> use them
 * together, but I use GEM only. Eventually, I will break Medusa3D, and it will
 * go in the bin. Expect Medusa3D to be dropped at some unknowable future date.
 * Think of it as demi-deprecated; its on my do-not-worry-about list.
 * <pre>
 * 2023-11-25 update Idx iteration techniques, for speed.
 * </pre>
 *
 * @author Keith Corlett 2021-03-04
 */
public final class Medusa3D extends AHinter implements IPrepare {

	// NOTE: BLUE and GREEN are now defined in Constants, for use elsewhere
	// (outside of coloring package, in the GUI, so are shared with Coloring,
	// XColoring, me, GEM, etc)

	/** The opposite color. */
	private static final int[] OPPOSITE = {BLUE, GREEN};

	/** names of the colors, in an array to select from. */
	private static final String[] COLORS = {green, blue};

	/** colors ON HTML tags. */
	private static final String[] CON = {GON, BON};

	/** colors OFF HTML tags. */
	private static final String[] COFF = {GOFF, BOFF};

	/** colored color names, for convenience. */
	private static final String[] CCOLORS = {GON+green+GOFF, BON+blue+BOFF};

	// The starting size of the steps StringBuilder. It will grow if necessary.
	private static final int STEPS_SIZE = 4096;

	/** thrown ONLY when a Type 2 hint has been found and added to accu. */
	private static final Type2Found TYPE_2_FOUND = new Type2Found();

	/** order ValueScores by score descending. */
	private static final Comparator<ValueScore> VALUE_SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

	// ============================ instance land =============================

	// shall we build the steps justifying this hint?
	// Building steps is expensive, and it remains unread in the batch, but
	// it's still required in the GUI and test-cases. Hence the batch skips
	// building the steps-string.
	private final boolean wantExplanation;

	// A conjugate pair is the ONLY two cells in a region which maybe value,
	// hence one of those two cells will be that value in the solution.
	private Cell[] pair;

	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// the indices of GREEN 7s, or BLUE 2s, or whatever.
	private IdxC[][] colors;

	// all values of each color; array to swap between colors.
	private Idx[] all;

	// temporary Idxs: set, read, and forget!
	private Idx tmp1, tmp2, tmp3, tmp4;

	// a bitset of those values which have been painted either color
	private int paintedValues;

	// a bitset of the GREEN and BLUE values; array to swap between colors.
	private int[] colorValues;

	// the Cell=>Values to eliminate. A field to not create for 99% failure.
	private Pots redPots;

	// Multi Type 3 only: brown arrows (from src-value to dest-value) in hint.
	private Collection<Link> links;

	// the (presumably) "good" color to display "results" in the hint.
	private int setPotsColor;
	// Contradictions only: the cell/s which caused this hint.
	private Idx cause;
	// Type 2 only: the region containing the cells which caused this hint,
	// to be bordered in pink, to help the user find the bastard.
	private ARegion bigHintRegion;

	// a StringBuilder of the steps required to building the coloring-chains;
	// and now also the conclusions: Contradictions or Eliminations.
	private StringBuilder steps, why;

	/**
	 * The constructor.
	 */
	public Medusa3D() {
		super(Tech.Medusa3D);
		// The batch does NOT want explanations, coz they are slow.
		this.wantExplanation = !Run.isBatch();
	}

	/**
	 * Prepare is called after puzzles loaded, but BEFORE the first findHints.
	 * @param grid
	 * @param logicalSolver
	 */
	@Override
	public void prepare(final Grid grid, final LogicalSolver logicalSolver) {
		// re-enable me, in case I went dead-cat in the last puzzle
		setIsEnabled(true); // use the setter!
	}

	/**
	 * Set-up all of Medusa3D's fields in findHints (then we clear them all
	 * again on the way out, to reduce my heap foot-print between calls).
	 * Note that everything large MUST go in here. Smaller ones, maybe not.
	 */
	private void setMyFields() {
		pair = new Cell[2];
		colors = new IdxC[2][VALUE_CEILING];
		int v, c = 0;
		do {
			v = 1;
			do
				colors[c][v] = new IdxC(); // PlusBuds
			while (++v < VALUE_CEILING);
		} while (++c < 2);
		all = new Idx[]{new Idx(), new Idx()};
		tmp1 = new Idx();
		tmp2 = new Idx();
		tmp3 = new Idx();
		tmp4 = new Idx();
		colorValues = new int[2];
		redPots = new Pots();
		if ( wantExplanation ) {
			steps = SB(STEPS_SIZE);
			why = SB(128);
			sb = SB(32);
		} else
			steps = why = sb = null;
	}

	/**
	 * Clear all fields that are set in setMyFields, to avoid any hangovers.
	 */
	private void clearMyFields() {
		pair = null;
		colors = null;
		all = null;
		tmp1 = tmp2 = tmp3 = tmp4 = null;
		colorValues = null;
		cause = null;
		bigHintRegion = null;
		redPots = null;
		steps = why = sb = null;
	}

	/**
	 * Called by solve to find first/all 3D Medusa hints, if any, in this grid.
	 *
	 * @return were any hint/s found?
	 */
	@Override
	public boolean findHints() {
		// presume that no hint will be found
		boolean result = false;
		try {
			setMyFields();
			// Step 1: foreach value in 2-or-more conjugate relationships,
			//         order by num conjugates + num bivalues DESCENDING.
			// WARN: startingValues filters to score >= 7 coz that is the
			// lowest score that hints in top1465. THIS MAY BE WRONG!!!
			for ( int value : startingValues() )
				for ( ARegion region : regions )
					if ( region.numPlaces[value] == 2
					  && (result|=search(region, value)) && oneHintOnly )
						return result;
		} catch ( Type2Found eaten ) {
			result = true; // a Type 2 hint has already been added to accu
		} finally {
			clearMyFields();
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
		final ValueScore[] scores = new ValueScore[VALUE_CEILING]; // score of values 1..9
		int i = 0;
		do // include 0 to not upset sort
			scores[i] = new ValueScore(i);
		while (++i < VALUE_CEILING);
		// count conjugate pairs for each value
		// build-up a bitset of those values that are in 2+ conjugate pairs
		int cands = 0;
		for ( ARegion r : regions )
			for ( int v : VALUESES[r.unsetCands] )
				if ( r.numPlaces[v] == 2
				  && ++scores[v].score > 1 )
					cands |= VSHFT[v];
		// foreach bivalue cell
		// increment score of each value with 2+ conjugate pairs
		for ( Cell c : cells )
			if ( c.size == 2 )
				for ( int v : VALUESES[c.maybes & cands] )
					++scores[v].score;
		// order by score descending
		Arrays.sort(scores, VALUE_SCORE_DESCENDING);
		// count values with a score of atleast 7. The least-possible minimum
		// score is 3, but I use 7 coz it is top1465s smallestThatHints.
		// I have no theoretical basis for 7, it is just what works for ME!
		// I presume it works for ALL Sudoku puzzles, but it may not!
		int n = 0;
		do
			if ( scores[n].score < 7 )
				break;
		while (++n < VALUE_CEILING);
		// then read-off the values, by score descending
		final int[] result = new int[n];
		for ( i=0; i<n; ++i ) // nb: MUST pretest (n may be 0)
			result[i] = scores[i].value;
		return result;
	}

	/**
	 * Search the conjugate pair in region on value for Medusa3D hints.
	 *
	 * @param region the region to search
	 * @param value the value to search for
	 * @return were any hint/s found; noting only one Type 2 is ever found.
	 */
	private boolean search(final ARegion region, final int value) {
		AHint hint;
		int subtype;
		// presume that no hint will be found
		boolean result = false;
		// clear all my fields
		clearColors();
		colorValues[GREEN]=colorValues[BLUE]=paintedValues = 0;
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
		region.at(region.places[value], pair);
		try {
			// Paint the cell, and all of it is ramifications (as above).
			if ( wantExplanation ) {
				steps.setLength(0); // painting steps
				why.setLength(0); // this painting step
				why.append("Assume that ").append(pair[0].id).append(MINUS)
				.append(value).append(IS).append(CCOLORS[GREEN])
				.append(NL);
			}
			paint(GREEN, pair[0], value, true);
			// Paint any "strong" hidden singles.
			paintMonoBoxs();
		} catch ( OverpaintException ex ) {
			// a cell was painted both colors, ergo my implementation is wrong.
			// Do not get your knickers in a twist. Itd be nice if it did not
			// happen, but it did, so it is handled, ie "prudently ignored".
			Log.teeln("WARN: Meduda3D: "+ex);
			return false;
		}
		// Step 5: Analyze the cluster.
		// clear the hint details
		links = null;
		subtype = 0;
		// squash all green values into one Idx
		// squash all blue values into one Idx
		// and sum the total number of cell-values colored
		final int n = squash(GREEN, all[GREEN])
				    + squash(BLUE, all[BLUE]);
		// 5.1 If either color contains a contradiction
		//     then the opposite color MUST be true.
		// Medusa3D: minContradiction=13, minElimination=6 top1465
		// NOTE: These minimum cell-values colored are top1465 specific. There
		// is no theoretical justification. These values may be WRONG for some
		// Sudokus; they are just right for MY Sudokus, ie top1465.
		if ( n > 12 ) {
			int c = 0;
			do {
				if ( (subtype=contradictions(c)) > 0
				  // presuming opposite color is not rooted too
				  && (hint=hintBig(value, subtype)) != null ) {
					if ( hint != null ) {
						result = true;
						if ( accu.add(hint) )
							return result;
						hint = null;
						// Same Type 2 from many conjugate-pairs, so throw it
						if ( subtype == 2 )
							throw TYPE_2_FOUND;
					}
				}
			} while (++c < 2);
		}
		// 5.2 If any uncolored cell-value sees both colors
		//     then it can be eliminated.
		if ( n > 5
		  && subtype==0
		  && (subtype=eliminations()) > 0
		  && (hint=hint(value, subtype)) != null ) {
			result = true;
			if ( accu.add(hint) )
				return result;
		}
		return result;
	}

	/**
	 * Clear all the Idxs in the colors array (both colors, all values).
	 */
	private void clearColors() {
		int c = 0, v;
		do {
			v = 1;
			do
				colors[c][v].clear();
			while (++v < VALUE_CEILING);
		} while (++c < 2);
	}

	/**
	 * Paint this cell this color, and recursively paint any consequences.
	 * <p>
	 * Note that this method calls itself (ie uses recursion) to paint not just
	 * the cell itself, but also all of the chaining-effects thereof; so if the
	 * cell has two values the other-value gets the other color, and if each of
	 * the cells three regions has two places for value then the other place
	 * is painted the other color; and so on, and so on... so this paint method
	 * paints a whole net of effects with a single initial call, recursively.
	 * <p>
	 * WARN: You must check that the cell-value is not already painted this
	 * color BEFORE you call paint. It is a performance thing. The call costs
	 * you even if it does nothing because the cell is already painted, so it is
	 * a bit faster to verify that it is actually required BEFORE you call me.
	 * <p>
	 * Paint implements steps 2 to 4 of ./Coloring.txt recursively. This is
	 * DFS, where order of results would make more sense BFS, but FFS how?
	 * <pre>
	 * Pseudocode for paint
	 * 1. Paint the given cellValue the given color.
	 * 2. If any region containing this cell except the given region (nullable)
	 *    has two places for this value then paint the otherCell the opposite
	 *    color; and then recursively paint all the consequences thereof.
	 * 3. If the biCheck parameter is true and this cell is bivalue then paint
	 *    the otherValue the opposite color; and then recursively paint all the
	 *    consequences thereof.
	 * </pre>
	 * Paint populates the two colors: green and blue, by value;
	 * and also populates the paintedValues bitset.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY in the GUI + testcases
	 * when we might want to use it as part of the HTML to display to the user.
	 * In the batch (LogicalSolverTester) and in generate its just a waste of
	 * time. MedusaColoringTest compares the full HTML.
	 * <p>
	 * IMPROVED: The why String is now constructed ONLY when used: so we always
	 * pretest "is this value already painted this color" BEFORE building the
	 * why String, even in the GUI, because it is slow: a MINUTE per top1465.
	 * IMPROVED: Unsure if the slow part is building each why String, or append
	 * to steps, so Steps is now a StringBuilder, clear with steps.setLength(0)
	 * instead of growing a new buffer EACH AND EVERY time with steps += why.
	 * IMPROVED: The wantWhy field is set ONCE in the constructor.
	 *
	 * @param c the color to paint this cellValue (GREEN or BLUE)
	 * @param cell the cell to paint
	 * @param value the value to paint
	 * @param biCheck if true then I also paint the otherValue of each bivalue
	 *  cell (cell.size==2) the opposite color, recursively. If false then skip
	 *  this step, because we already know it is a waste of time
	 * @param why a one-liner saying why this cell-value is painted this color
	 * @throws OverpaintException when a cell-value is painted in both colors
	 */
	private void paint(final int c, final Cell cell, final int value
			, final boolean biCheck) throws OverpaintException {
		int otherIndice; // the other cell
		int otherValue; // the other value
		// throw if cell-value is already painted the opposite color.
		final int o = OPPOSITE[c]; // the opposite color
		if ( colors[o][value].has(cell.indice) )
			throw new OverpaintException("Cannot paint "+cell.id+MINUS+value+SP
					+COLORS[c]+" when it is already "+COLORS[o]+PERIOD);
		// 1. Paint the given cell-value this color
		colors[c][value].add(cell.indice);
		paintedValues |= colorValues[c] |= VSHFT[value];
		// remember the steps in this coloring-chain (steps is a StringBuilder)
		if ( wantExplanation ) {
			steps.append(why);
			why.setLength(0);
		}
		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r : cell.regions ) {
			if ( r.numPlaces[value] == 2
			  // and otherCell-value is not already painted the opposite color
			  // nb: pre-check is faster than building useless why strings
			  && !colors[o][value].has(otherIndice=r.indices[IFIRST[r.places[value] & ~cell.placeIn[r.rti]]]) ) {
				// we want explanation in GUI and testcases
				// NOTE: batch is a MINUTE faster for this!
				if ( wantExplanation )
					why.append(colorCellId(c, cell, value))
					.append(CONJUGATE_IN).append(r.label).append(IS)
					.append(colorCellId(o, cells[otherIndice], value))
					.append(NL);
				// paint otherCell-value the opposite color, recursively.
				paint(o, cells[otherIndice], value, true);
			}
		}
		// 3. Paint the other value of this bivalue cell the opposite color
		if ( cell.size == 2
		 // skip this bi-check when we are painting "the other value"
		 && biCheck
		 // and get the other value (tautology)
		 && (otherValue=VFIRST[cell.maybes & ~VSHFT[value]]) > 0 // 9bits
		 // and cell-otherValue is not already paited the opposite color
		 // nb: pre-check is faster than building useless why strings
		 && !colors[o][otherValue].has(cell.indice)
		) {
			// we want explanation in GUI and testcases
			// NOTE: batch is a MINUTE faster for this!
			// NOTE: terniaries are slow!
			if ( wantExplanation )
				why.append(colorCellId(c, cell, value))
				.append(ONLY_OTHER_VALUE_IS)
				.append(colorCellId(o, cell, otherValue))
				.append(NL);
			// paint cell-otherValue the opposite color, recursively,
			// but skip the bi-check.
			paint(o, cell, otherValue, false);
		}
	}

	/**
	 * A "mono-box" is a box with only one place remaining for v (taking colors
	 * into account), which also leaves "causal box" with one v remaining,
	 * constituting a "strong" bidirectional link. The shoot-back rule is from
	 * XColoring. Its common enough, increasing Medusa3D eliminations from
	 * 20724 to 28601, ie nearly 1.5 times as many.
	 * <p>
	 * In XColoring if painting a cell-value green leaves any of its effected
	 * boxs with one-and-only-one remaining place for value then it is green.
	 * But note well that this is ONLY a "weak" link: if green c1 then c2 is
	 * green, which does NOT imply the converse: if c2 is green then c1 still
	 * may not be green.
	 * <p>
	 * Medusa3D Coloring requires "strong" (bidirectional) links, with an "and
	 * conversely" implication, so we must establish that a strong link exists,
	 * or we must just forget it. To do this we look at the vs in the box that
	 * contains the source cell, ie {@code cell.box.idxs[v]}.
	 * <pre>
	 * IF we remove the buddies of the cell to paint
	 * from the vs in the-source-cells-box
	 * and that leaves one-only v (No need to check it is the source cell,
	 *                             it just HAS to be)
	 * THEN it is a "strong" link (a reciprocal causal relationship),
	 * SO paint cell-value the same color as the source cell-value.
	 * </pre>
	 * <p>
	 * The <b>"shoot back"</b> rule, stated completely and concisely:
	 * <pre>
	 * IF cell1-v is green and that leaves one-only place in an effected box,
	 * which in turn leaves one-only place in the source box,
	 * THEN paint cell2-v green because:
	 *     (a) if cell1-value is green then cell2-value is green
	 * <b>AND CONVERSELY</b>
	 *     (b) if cell2-value is green then cell1-value is green.
	 * IE: It is a "strong" bidirectional link.
	 * </pre>
	 * NOTE: My implementation is eager: it tests if ALL existing green values
	 * AND the source cell-value reduce the effected box to a single.
	 * Occasionally this produces a coloring whose logic makes little sense,
	 * but it only does this BECAUSE existing colorings have eliminated other
	 * occurrences of v, so the logic stands-up, despite prima facea looking
	 * screwy; ergo it works, so dont ____ with it.
	 * <p>
	 * NOTE: Some paintings are missed because paintMonoBoxs is not repeated
	 * until it finds nothing to paint. The paint method recursively finds and
	 * paints all consequences, and paintMonoBoxs works-it-out on the fly (esp
	 * buds) so there is a small chance of missing a cell-value, hence we miss
	 * hints, rarely, but I dont think the last 0.1% of eliminations are worth
	 * the expense, but I am a lazy bastard.
	 * <p>
	 * ASIDE: Getting it right/fast took a WEEK. What you do not see is all the
	 * madCrap I tried while I was trying to work-out whatThe____AmIDoing()?
	 * Turns out its MUCH simpler than I first thought. Sigh. The breakthrough
	 * was the EFFECTED_BOXS static array field in Grid. That's smart.
	 */
	private void paintMonoBoxs() {
		Cell cell; // the source cell
		Cell cell2; // the cell to paint, if any
		ARegion box; // shorthand for the box containing the cell to paint
		// foreach color
		int c = 0;
		do { // GREEN, BLUE
			// foreach value which has a cell painted thisColor
			for ( int value : VALUESES[colorValues[c]] )
				// foreach cell painted thisColor (the source indice)
				for ( int source : colors[c][value].indicesCached() )
					// foreach of the 4 boxes effected by this source cell.
					// ebi: effectedBoxIndex: grid.boxs index of effected box.
					for ( int ebi : INDEXES[EFFECTED_BOXS[BOX_OF[source]]] )
						// if there is one v remaining in the effected box,
						// excluding this colors cells + there buddies.
						// NOTE: plusBuds has an internal cache.
						if ( tmp2.setAndNotAny(boxs[ebi].idxs[value], colors[c][value].plusBuds())
						  && tmp2.size() == 1
						  // and get the cell to paint (NEVER null)
						  && (cell2=cells[tmp2.peek()]) != null
						  // the "shoot back" rule (see method comments).
						  && tmp3.setAndNotAny(boxs[BOX_OF[source]].idxs[value], BUDDIES[cell2.indice])
						  && tmp3.size() == 1
						  // NB: no alreadyPainted check required because this
						  // this colors cells are already excluded.
						) {
							cell = cells[source]; // the source cell
							box = regions[ebi]; // the effected box
							if ( wantExplanation ) {
								why.append(colorCellId(c, cell, value))
								.append(LEAVES).append(cell2.id).append(ONLY)
								.append(value).append(IN).append(box.label)
								.append(COMMA_WHICH_LEAVES).append(cell.id)
								.append(ONLY).append(value).append(IN)
								.append(cell.box.label).append(COMMA_SO)
								.append(colorCellId(c, cell2, value))
								.append(NL);
							}
							paint(c, cell2, value, false);
						}
		} while (++c < 2);
	}

	/**
	 * Does colors[c] contain a contradiction.
	 * <pre>
	 * See if colors[c] contains a contradiction:
	 * * If two vs in a region are green then green is invalid.
	 * * If two values of a cell are green then green is invalid.
	 * These two both set multiple cells via a XColoringHintBig.
	 * </pre>
	 *
	 * @param c color index, GREEN or BLUE
	 * @return The subtype of hint to create:<br>
	 * 2 meaning Type 2: two same color vs in a region.<br>
	 * 3 meaning Type 3: two values in cell are same color.<br>
	 * 0 meaning none.
	 */
	private int contradictions(final int c) {
		Cell cell; // cell with contradictory values
		final Idx[] thisColor = colors[c];
		final int o = OPPOSITE[c]; // the opposite color
		// If two+ green vs in a region then green is invalid.
		// NB: Type 2 is as rare as rocking horse s__t.
		// NB: for now we just ASSUME that colors[goodColor] are "good", but
		//     we double-check that when we are building the setPots for the
		//     XColoringHintBig, otherwise could get (rare) invalid hints.
		for ( ARegion r : regions )
			for ( int v : VALUESES[colorValues[c]] )
				if ( tmp1.setAndMany(thisColor[v], r.idx) ) {
					this.bigHintRegion = r;
					this.cause = new Idx(tmp1);
					this.setPotsColor = o;
					// we want explanation in GUI and testcases
					if ( wantExplanation )
						steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
						.append(KON).append(r.label).append(KOFF).append(HAS).append(MULTIPLE)
						.append(CON[c]).append(COLORS[c]).append(SP).append(v)
						.append(APOSTROPHE_S).append(COFF[c])
						.append(", which is invalid, so ").append(CCOLORS[o])
						.append(MUST_BE_TRUE).append(NL);
					return 2;
				}
		// If two+ values in cell are green then green is invalid.
		for ( int v1 : VALUESES[colorValues[c]] )
			// foreach colorValue EXCEPT v1
			for ( int v2 : VALUESES[colorValues[c] & ~VSHFT[v1]] )
				if ( tmp1.setAndAny(thisColor[v1], thisColor[v2]) ) {
					cell = cells[tmp1.peek()];
					this.cause = Idx.of(cell.indice);
					// we want explanation in GUI and testcases
					if ( wantExplanation )
						steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
						.append(KON).append(cell.id).append(KOFF).append(HAS)
						.append(CON[c]).append(COLORS[c]).append(SP)
						.append(v1).append(AND).append(v2).append(COFF[c])
						.append(", which is invalid, so ").append(CCOLORS[o])
						.append(MUST_BE_TRUE).append(NL);
					return 3;
				}
		return 0;
	}

	/**
	 * Create a new XColoringHintBig.
	 *
	 * @param value the potential value to create hint for.
	 * @param subtype the subtype of the hint to create.
	 * @return a new XColoringHintBig
	 */
	private AHint hintBig(final int value, final int subtype) {
		final Pots toSet; // the cell-values to be set
		try {
			toSet = squishSetPots(setPotsColor);
		} catch ( Pots.IToldHimWeveAlreadyGotOneException ex ) {
			return null; // attempted to set 1 cell to 2 values.
		}
		AHint hint = new XColoringHintBig(grid, this, value, subtype, null
			, cause, setPotsColor
			, wantExplanation ? steps.toString() : null
			, toSet, squish(GREEN), squish(BLUE), links, null, bigHintRegion);
		cause = null;
		bigHintRegion = null;
		return hint;
	}

	/**
	 * Squish ONE value into each cell.
	 *
	 * @param c the color to squish
	 * @return a new Pots of all values in this color. There must be 1 value
	 *  per cell (per indice) in colors[c], else I throw IllegalStateException
	 *  meaning that colors[c] is not a valid setPots.
	 * @throws Pots.IToldHimWeveAlreadyGotOneException when second value added
	 *  to a cell.
	 */
	private Pots squishSetPots(final int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		IntQueue q;
		int i;
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] ) {
			// nb: Pots.IToldHimWeveAlreadyGotOneException
			// means we set a cell to two different values.
			q = thisColor[v].indices();
			while ( (i=q.poll()) > QEMPTY )
				result.insertOnly(i, VSHFT[v]);
		}
		return result;
	}

	/**
	 * Do the 3D Medusa eliminations.
	 * <pre>
	 * Step 5.2: Eliminate if v sees both colors.
	 * (a) Both colors appear in a single tri+ valued cell.
	 * (b) An uncolored candidate v sees two differently colored vs.
	 * (c) An uncolored candidate sees a colored cell (eg green), but this
	 *     cell has another candidate with the opposite color (eg blue).
	 * NOTE: (c) is the only additional elimination over Extended Coloring.
	 *
	 * There can be MORE THAN ONE type of elimination, so the subtype return
	 * value is now a bitset:
	 * * 1: Both colors in cell
	 * * 2: X sees both colors
	 * * 4: has green and sees blue
	 * + The "steps" field boosted to provide user with conclusions.
	 * + The "links" field now provides the user with "why" indicators.
	 * </pre>
	 */
	private int eliminations() {
		IntQueue q; // is marginally faster than an Iterator<Integer>
		int g		// green value/s bitset
		  , b		// blue value/s bitset
		  , pinks	// bitset of "all other values" to eliminate
		  , c		// color: the current color: GREEN or BLUE
		  , o		// opposite: the other color: BLUE or GREEN
		  , i;		// indice of current cell
		// presume none
		int subtype = 0;
		// is this the first elimination
		boolean first = true;
	    // (a) Both colors appear in a cell, removing all other maybes,
		//     because one of the two colors MUST be true.
		if ( tmp1.setAndAny(all[GREEN], all[BLUE]) )
			for ( q=tmp1.indices(); (i=q.poll())>QEMPTY; )
				if ( cells[i].size > 2
				  // get a bitset of all values of cells[i] that are green and blue.
				  // There may be none, never multiple (contradictions pre-tested)
				  // We need 1 green value and 1 blue value, to strip from pinks.
				  // NOTE: VSIZE[0] == 0.
				  && VSIZE[g=cands(GREEN, i)] == 1
				  && VSIZE[b=cands(BLUE, i)] == 1
				  // ensure that g and b are not equal (should NEVER be equal)
				  && g != b
				  // pinks is a bitset of "all other values" to be eliminated
				  && (pinks=maybes[i] & ~g & ~b) > 0 // 9bits
				  // ignore already-justified eliminations (should not happen here)
				  && redPots.upsert(i, pinks, false)
				) {
					// we want explanation in GUI and testcases
					if ( wantExplanation ) {
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(CELL_IDS[i]).append(HAS_BOTH)
						.append(GON).append(MAYBES[g]).append(GOFF).append(AND)
						.append(BON).append(MAYBES[b]).append(BOFF).append(COMMA).append(ELIMINATING)
						.append(RON).append(ALL_OTHER_VALUES).append(ROFF).append(PERIOD)
						.append(NL);
					}
					subtype |= 1;
				}
		// (b) An uncolored v sees both colored vs, removing v from this cell,
		//     because one of the two colors MUST be true.
		uncolored(); // sets uncolored[value] array
		for ( int value : VALUESES[paintedValues] )
			for ( int ii : uncolored[value].indicesCached() )
				if ( tmp3.setAndAny(BUDDIES[ii], colors[GREEN][value])
				  && tmp4.setAndAny(BUDDIES[ii], colors[BLUE][value])
				  // ignore already-justified eliminations
				  && redPots.upsert(ii, VSHFT[value], DUMMY)
				) {
					// we want explanation in GUI and testcases
					if ( wantExplanation ) {
						int gc = closest(tmp3, COL_OF[ii], ROW_OF[ii]);
						int bc = closest(tmp4, COL_OF[ii], ROW_OF[ii]);
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(CELL_IDS[ii]).append(MINUS).append(value).append(SEES_BOTH)
						.append(GON).append(CELL_IDS[gc]).append(MINUS).append(value).append(GOFF).append(AND)
						.append(BON).append(CELL_IDS[bc]).append(MINUS).append(value).append(BOFF)
						.append(COMMA_SO).append(CELL_IDS[ii]).append(CANT_BE)
						.append(RON).append(value).append(ROFF).append(PERIOD).append(NL);
						if ( links == null )
							links = new LinkedList<>();
						links.add(new Link(ii, value, gc, value));
						links.add(new Link(ii, value, bc, value));
					}
					subtype |= 2;
				}
		// (c) An uncolored v sees a green, and has some other blue value.
		for ( int value : VALUESES[paintedValues] )
			for ( int ii : uncolored[value].indicesCached() ) {
				c = 0;
				do
					// if cells[ii] sees a this-color value
					if ( tmp3.setAndAny(BUDDIES[ii], colors[c][value])
					  // and the opposite color (any value) contains ii
					  && all[OPPOSITE[c]].has(ii)
					  // ignore already-justified eliminations
					  && redPots.upsert(ii, VSHFT[value], DUMMY)
					) {
						// we want explanation in GUI and testcases
						if ( wantExplanation ) {
							o = OPPOSITE[c];
							int sibling = closest(tmp3, COL_OF[ii], ROW_OF[ii]);
							int otherValue = firstValue(o, ii);
							if ( first ) {
								first = false;
								steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
							}
							steps.append(CELL_IDS[ii]).append(SP).append(SEES_SP).append(CON[c])
							.append(CELL_IDS[sibling]).append(MINUS).append(value)
							.append(COFF[c]).append(AND_HAS)
							.append(CON[o]).append(otherValue).append(COFF[o])
							.append(", so it cannot be ").append(RON).append(value).append(ROFF)
							.append(PERIOD).append(NL);
							if ( links == null )
								links = new LinkedList<>();
							links.add(new Link(ii, value, sibling, value));
						}
						subtype |= 4;
					}
				while (++c < 2);
			}
		uncolored = null;
		return subtype;
	}

	/**
	 * Indices of uncolored cells, by value.
	 */
	private void uncolored() {
		if ( uncolored == null ) {
			uncolored = new IdxC[VALUE_CEILING];
			for ( int v=1; v<VALUE_CEILING; ++v )
				uncolored[v] = new IdxC();
		} else {
			for ( int v=1; v<VALUE_CEILING; ++v )
				uncolored[v].clear();
		}
		final Idx tmp = new Idx();
		for ( int v : VALUESES[paintedValues] )
			uncolored[v].setAndNot(idxs[v], tmp.setOr(colors[GREEN][v], colors[BLUE][v]));
	}
	private IdxC[] uncolored;

	/**
	 * Squash the indices of all values of this color into result.
	 *
	 * @param c the color you seek: GREEN or BLUE
	 */
	private int squash(final int c, final Idx result) {
		result.clear();
		for ( int value : VALUESES[colorValues[c]] )
			result.or(colors[c][value]);
		return result.size();
	}

	/**
	 * Get a bitset of the values that are painted color-c at indice-i.
	 *
	 * @param c the color index: GREEN, or BLUE
	 * @param indice the cell indice to check
	 * @return a bitset of painted values
	 */
	private int cands(final int c, final int indice) {
		int result = 0;  // NOTE: VSIZE[0] == 0
		for ( int value : VALUESES[colorValues[c]] )
			if ( colors[c][value].has(indice) )
				result |= VSHFT[value];
		return result;
	}

	/**
	 * Return the first value in colors[c][*] which has(indice).
	 * <p>
	 * NOTE: We cannot use FIRST_VALUE because not-found must return 0.
	 *
	 * @param c the color: BLUE or GREEN
	 * @param indice the cell index
	 * @return the first value of ii that is painted the opposite color
	 */
	private int firstValue(final int c, final int indice) {
		for ( int value : VALUESES[colorValues[c]] )
			if ( colors[c][value].has(indice) )
				return value;
		return 0; // NOTE: VSIZE[0] == 0
	}

	/**
	 * Return the cell in idx that is physically closest to the target cell.
	 * <p>
	 * NOTE: This method is just a "nice to have". If the target cell sees ANY
	 * cell-of-this-color then logic holds up; it is just nice to show the user
	 * the closest-cell-of-this-color; that is all.
	 *
	 * @param idx {@code tmp3.setAndAny(BUDDIES[ii], colors[c][v])} the buddies
	 * of the target cell which are painted this color.
	 * @param target {@code cc = cells[ii]} the target bloody cell.
	 * @return the indice of the closest cell (or -1 if I am rooted)
	 */
	private int closest(final Idx idx, final int cx, final int cy) {
		int y, x, distance, closest = -1;
		int minD = Integer.MAX_VALUE; // the distance to the closest cell
		for ( int indice : idx ) {
			y = Math.abs(ROW_OF[indice] - cy);
			x = Math.abs(COL_OF[indice] - cx);
			if ( y==0 )
				distance = x;
			else if (x==0)
				distance = y;
			else
				distance = (int)Math.sqrt((double)(x*x + y*y));
			if ( distance < minD ) {
				minD = distance;
				closest = indice;
			}
		}
		return closest;
	}

	/**
	 * Create a new XColoringHint.
	 * <ul>
	 * <li>the subtype is now a bitset! 1=Type1, 2=Type2, 4=Type3. sigh.
	 * <li>just keep the subtype param, even though it is no longer used because
	 * the steps String is now populated up in eliminate, which is MUCH better.
	 * </ul>
	 *
	 * @param value the value
	 * @param subtype no longer used!
	 * @return a new XColoringHint (for eliminations)
	 */
	private AHint hint(final int value, final int subtype) {
		final Idx[] colorSet = null;
		// cinks: links from cells which found this hint, if any. sigh.
		final LinkedList<Link> linksCopy = links==null ? null : new LinkedList<>(links);
		final AHint hint = new XColoringHint(grid, this, value, new Pots(redPots)
			, squish(GREEN), squish(BLUE), colorSet
			, wantExplanation ? steps.toString() : null
			, linksCopy);
		// do not hold cell references past there use-by date; so we copy-off
		// the fields when we create the hint (above) then clear the bastards.
		redPots.clear();
		links = null;
		return hint;
	}

	/**
	 * Squish all values of the given color into one new Idx.
	 *
	 * @param color the color to squish
	 * @return a new Pots of Cell=>values in all values of colors[color].
	 */
	private Pots squish(final int color) {
		final Pots result = new Pots();
		for ( int v : VALUESES[colorValues[color]] )
			result.upsertAll(colors[color][v], maybes, v);
		return result;
	}

	private StringBuilder sb;
	private StringBuilder sb() {
		sb.setLength(0);
		return sb;
	}

	private StringBuilder colorCellId(final int color, final Cell cell, final int value) {
		return sb().append(CON[color]).append(cell.id).append(MINUS).append(value).append(COFF[color]);
	}

	/**
	 * ValueScore is the score for each value; only used in the startingValues
	 * method, to keep the value WITH it is score, so that we can sort the array
	 * by score descending and keep the value.
	 */
	private static class ValueScore {
		public final int value;
		public int score;
		ValueScore(int value) {
			this.value = value;
		}
		// debug only, not used in actual code
		@Override
		public String toString() {
			return ""+value+COLON_SP+score;
		}
	}

	/** A distinctive subtype of RuntimeException. */
	private static final class Type2Found extends RuntimeException {
		private static final long serialVersionUID = 68198734001267L;
	}

	/** A distinctive subtype of RuntimeException. */
	private static final class OverpaintException extends IllegalStateException {
		private static final long serialVersionUID = 720396129058L;
		public OverpaintException(String msg) {
			super(msg);
		}
	}

}
