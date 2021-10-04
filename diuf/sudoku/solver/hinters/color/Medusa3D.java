/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Cells;
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
import diuf.sudoku.Values;
import static diuf.sudoku.Grid.BOX_OF;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VFIRST;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.hinters.IPreparer;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import static diuf.sudoku.solver.hinters.color.Words.*;
import static diuf.sudoku.utils.Frmt.*;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Medusa3D implements the 3D Medusa Coloring Sudoku solving technique as
 * described in the https://www.sudopedia.org/wiki/3D_Medusa specification.
 * <p>
 * See ./#Medusa3D.txt for annoted specification.
 * <p>
 * This implementation also includes one extension of the above specification.
 * It finds a naked single in an effected box, which leaves a naked single in
 * the source box... which I call the "10 paces" rule. Search for that.
 * <p>
 * Note that Medusa3D hijacks XColoring's hint types.
 *
 * @author Keith Corlett 2021-03-04
 */
public class Medusa3D extends AHinter implements IPreparer
//		, diuf.sudoku.solver.IReporter
{

	/** The first color: green. */
	private static final int GREEN = 0;
	/** The second color: blue. */
	private static final int BLUE = 1;
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
	// The starting size of the steps StringBuilder. It'll grow if necessary.
	private static final int STEPS_SIZE = 4096;

	/** A distinctive subtype of RuntimeException. */
	private static final class Type2Exception extends RuntimeException {
		private static final long serialVersionUID = 68198734001267L;
	}
	/** thrown ONLY when a Type 2 hint has been found and added to accu. */
	private static final Type2Exception TYPE_2_EXCEPTION = new Type2Exception();
	/** A distinctive subtype of RuntimeException. */
	private static final class OverpaintException extends IllegalStateException {
		private static final long serialVersionUID = 720396129058L;
		public OverpaintException(String msg) {
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
		// debug only, not used in actual code
		@Override
		public String toString() {
			return ""+value+COLON_SP+score;
		}
	}

	/**
	 * Used to sort ValueScore's by score descending.
	 */
	private static final Comparator<ValueScore> VALUE_SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

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
	// do we need to build the steps String to go in the hint?
	private final boolean wantWhy;
	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// So colors holds the indices of GREEN 7's, or BLUE 2's, or whatever.
	private final Idx[][] colors = new Idx[2][10];
	// all values of each color; array to swap between colors.
	private final Idx[] all = {new Idx(), new Idx()};
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
	// Multi Type 3 only: brown arrows (from src-value to dest-value) in hint.
	private Collection<Link> links;

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
	public Medusa3D() {
		super(Tech.Medusa3D);
		// create the colors array.
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				colors[c][v] = new Idx();
		// Do we need to build the steps string to go in the hint? The steps
		// Strings take ages to build (a minute per top1465 run) and we don't
		// use it in the batch, only in the GUI and testcases. See the paint
		// method for more.
		this.wantWhy = Run.type != Run.Type.Batch;
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
//		Log.teef("\n%s: minContradiction=%d, minElimination=%d\n", tech.name()
//				, minContradiction, minElimination);
//	}
//	private int minContradiction = Integer.MAX_VALUE;
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
		this.candidates = grid.idxs;
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
		} catch ( Type2Exception eaten ) {
			// ONLY thrown after a Type 2 hint is added to accu
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
			if ( c.size == 2 )
				for ( int v : VALUESES[c.maybes & cands] )
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
		// presume that no hint will be found
		boolean result = false;
		// clear my fields
		clearColors();
		colorValues[GREEN]=colorValues[BLUE]=paintedValues = 0;
		steps.setLength(0); // the painting steps, and conclusions if any
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
		r.at(r.indexesOf[v].bits, conjugatePair);
		try {
			// Paint the cell, and all of it's ramifications (as above).
			paint(GREEN, v, conjugatePair[0], true, "Let us assume that "
				+conjugatePair[0].id+MINUS+v+IS+CCOLORS[GREEN]+NL);
			// Paint any "strong" hidden singles.
			paintMonoBoxs();
//			if ( steps.length() > STEPS_SIZE )
//				System.out.println("WARN: Meduda3dColoring: OVERSIZE steps="+steps.length());
		} catch ( OverpaintException ex ) {
			// a cell was painted both colors, ergo my implementation is wrong.
			// Don't get your knickers in a twist. It'd be nice if it didn't
			// happen, but it did, so it's handled, ie "prudently ignored".
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
		// MedusaColoring: minContradiction=13, minElimination=6 top1465
		// NOTE: These minimum cell-values colored are top1465 specific. There
		// is no theoretical justification. These values may be WRONG for you!
		if ( n > 12 ) {
			for ( int c=0; c<2; ++c ) {
				if ( (subtype=contradictions(c)) != 0
				  // presuming opposite color isn't rooted too
				  && (hint=createHintMulti(v, subtype)) != null ) {
					if ( HintValidator.MEDUSA_COLORING_USES ) {
						if ( !HintValidator.isValidSetPots(grid, hint.getResults()) ) {
							hint.isInvalid = true;
							HintValidator.reportSetPots(tech.name()+"Multi", grid
								, HintValidator.invalidity, hint.toFullString());
							if ( Run.type != Run.Type.GUI )
								hint = null;
						}
					}
					if ( hint != null ) {
						result = true;
//						if ( n < minContradiction )
//							minContradiction = n;
						if ( accu.add(hint) )
							return result;
						hint = null;
						// Same Type 2 from many conjugate-pairs
						// simplest way to handle this is throw an exception
						if ( subtype == 2 )
							throw TYPE_2_EXCEPTION;
					}
				}
			}
		}
		if ( n > 5 ) {
			// 5.2 If any uncolored cell-value sees both colors
			//     then it can be eliminated.
			if ( subtype==0 && (subtype=eliminations())!=0 ) {
				hint = createHint(v, subtype);
				if ( HintValidator.MEDUSA_COLORING_USES ) {
					if ( !HintValidator.isValid(grid, hint.getReds(0)) ) {
						hint.isInvalid = true;
						HintValidator.report(tech.name(), grid, hint.toFullString());
						if ( Run.type != Run.Type.GUI )
							hint = null;
					}
				}
				if ( hint != null ) {
					result = true;
//					if ( n < minElimination )
//						minElimination = n;
					if ( accu.add(hint) )
						return result;
					hint = null;
				}
			}
		}
		return result;
	}

	/**
	 * Clear all the Idx's in the colors array (both colors, all values).
	 */
	private void clearColors() {
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				colors[c][v].clear();
	}

	/**
	 * Paint this cell this color, and recursively paint any consequences.
	 * <p>
	 * WARNING: You must check that the cell-value is not already painted this
	 * color BEFORE you call paint. It's a performance issue with why String.
	 * <p>
	 * The paint method implements Steps 2 to 4 of ./Coloring.txt using a
	 * recursive DFS, where the instructions specify a BFS, without saying so
	 * explicitly, which is a BIG warning sign. Experienced programmers prefer
	 * DFS for speed, but newbs think BFS for simplicity. Being an experienced
	 * arrogant prick, I have simply countermanded him.
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any region containing this cell except the given region (nullable)
	 *    has two places for this value then paint the otherCell the opposite
	 *    color; and recursively paint the consequences thereof.
	 * 3. If biCheck and cell is bivalue then paint the otherValue the opposite
	 *    color; and recursively paint the consequences thereof.
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
	 * @param c the color to paint this cellValue (GREEN or BLUE)
	 * @param v the value to paint
	 * @param cell the cell to paint, recursively
	 * @param biCheck if true then I also paint the otherValue of each bivalue
	 *  cell (cell.maybesSize==2) the opposite color, recursively. If false
	 *  then skip step 3, because we already know it's a waste of time.
	 * @param why a one-liner on why this cell-value is painted this color.
	 * @throws OverpaintException when a cell-value is painted in both colors.
	 */
	private void paint(int c, int v, Cell cell, boolean biCheck, String why)
			throws OverpaintException {
		// If cell-value is already painted the opposite color then throw!
		final int o = OPPOSITE[c]; // the opposite color
		if ( colors[o][v].contains(cell.i) )
			throw new OverpaintException("Cannot paint "+cell.id+MINUS+v+SPACE
					+COLORS[c]+" when it's already "+COLORS[o]+PERIOD);
		int otherValue; // the other value
		Cell otherCell; // the other cell
		// 1. Paint the given cell-value this color
		colors[c][v].add(cell.i);
		paintedValues |= colorValues[c] |= VSHFT[v];
		// remember the steps in this coloring-chain (steps is a StringBuilder)
		if ( why != null ) {
			steps.append(why);
			why = null;
		}
		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r2 : cell.regions ) {
			if ( r2.indexesOf[v].size == 2
			  // nb: pre-check faster than needlessly building the why string!
			  && !colors[o][v].contains((otherCell=r2.otherThan(cell, v)).i)
			) {
				// we want explanation in GUI and testcases
				// NOTE: batch is a MINUTE faster for this!
				if ( wantWhy )
					why = CON[c]+cell.id+MINUS+v+COFF[c]+CONJUGATE_IN+r2.id
						+IS+CON[o]+otherCell.id+MINUS+v+COFF[o]+NL;
				// paint otherCell-v the opposite color, recursively.
				paint(o, v, otherCell, true, why);
			}
		}
		// 3. Paint the other value of this bivalue cell the opposite color
		if ( cell.size == 2
		 // skip this bi-check when we're painting "the other value"
		 && biCheck
		 // nb: pre-check faster than needlessly building the why string!
		 && !colors[o][otherValue=VFIRST[cell.maybes & ~VSHFT[v]]].contains(cell.i)
		) {
			// we want explanation in GUI and testcases
			// NOTE: batch is a MINUTE faster for this!
			// NOTE: terniaries are slow!
			if ( wantWhy )
				why = CON[c]+cell.id+MINUS+v+COFF[c]+ONLY_OTHER_VALUE_IS
					+CON[o]+cell.id+MINUS+otherValue+COFF[o]+NL;
			// paint cell-otherValue the opposite color, recursively,
			// but skip the bi-check.
			paint(o, otherValue, cell, false, why);
		}
	}

	/**
	 * A "mono-box" is a box with only one place remaining for v, which also
	 * leaves the "causal box" with one place remaining for v, constituting a
	 * "strong" bidirectional link. This is an additional rule borrowed from
	 * XColoring, adapted to 3D Medusa's needs. It's a common-enough case
	 * producing "some" extra eliminations: without was 20724, with is 28601.
	 * So this relatively straight-forward addition makes 3D Medusa nearly 1.5
	 * times as effective. Something to write home about, but only 50 words.
	 * <p>
	 * In XColoring if painting a cell-value green leaves any of it's effected
	 * boxs with one-and-only-one remaining place for value then it's green.
	 * But note carefully that this is ONLY a "weak" link: if green c1 then c2
	 * is green DOES NOT imply the converse: if c2 is green then c1 is green.
	 * <p>
	 * Medusa 3D Coloring requires "strong" (bidirectional) links, with an "and
	 * conversely" implication, so we must establish that a strong link exists,
	 * or forget it. To do this we look at the v's in the box that contains the
	 * source cell, ie {@code cell.box.idxs[v]}.
	 * <pre>
	 * IF we remove the buddies of the cell to paint
	 * from the v's in the-source-cells-box
	 * and that leaves one-only v (No need to check it's the source cell,
	 *                             it just HAS to be)
	 * THEN it's a "strong" link (a reciprocal relationship),
	 * SO paint cell-value-to-paint the same as the source cell-value.
	 * </pre>
	 * <p>
	 * <b>The "10 paces" rule (shoot back)</b> stated completely:<pre>
	 * IF cell-v is green and that leaves one-only place in an effected box,
	 * which in turn leaves one-only place in the source box,
	 * THEN we can paint cell2-v green because:
	 *     (a) if cell-value is green then cell2-value is green
	 *     <b>AND CONVERSELY
	 *     (b) if cell2-value is green then cell-value is green.</b>
	 * IE: It's a "strong" bidirectional link.
	 * NOTE: The implementation is more eager: it tests if ALL existing green
	 * values AND the source cell-value reduce the effected box to a single,
	 * occasionally this produces a coloring whose logic makes little sense,
	 * but it only does this BECAUSE existing colorings have eliminated other
	 * occurrences of v, so the logic stands-up, despite prima facea looking
	 * screwy; ergo it works, so don't ____ with it.
	 * </pre>
	 * <p>
	 * Note that some paintings are missed because paintMonoBoxs is not run
	 * repeatedly until it finds nothing to paint. paint recursively finds and
	 * paints all consequences, and paintMonoBoxs works-it-out on the fly,
	 * especially buds(), so there's small chance of missing a cell-value, so a
	 * missed hint is rarer than rocking horse s__t, so I don't think the last
	 * 0.1% of eliminations are worth the extra clock-ticks. Be a lazy bastard.
	 * <p>
	 * ASIDE: Getting it right/fast took a WEEK. What you don't see is all the
	 * madCrap I tried while I was trying to workOutWhatTheF___AmIDoing? Turns
	 * out it's MUCH simpler than I first thought. Sigh. The breakthrough was
	 * the EFFECTED_BOXS field.
	 */
	private void paintMonoBoxs() {
		Cell cell; // the source cell
		Cell cell2; // the cell to paint, if any
		ARegion box; // shorthand for the box containing the cell to paint
		String why = null; // the reason we paint this cell-value this color
		// foreach color
		for ( int c=0; c<2; ++c ) { // GREEN, BLUE
			final Idx[] thisColor = colors[c];
			// foreach value which has a cell painted thisColor
			for ( int v : VALUESES[colorValues[c]] ) {
				// foreach cell painted thisColor
				for ( int ci : thisColor[v].toArrayA() ) { //source cell indice
					// foreach of the four boxes effected by this source cell.
					// See EFFECTED_BOXS for a definition there-of.
					// i is the index in grid.regions of the effected box
					for ( int i : INDEXES[EFFECTED_BOXS[BOX_OF[ci]]] ) {
						// if there's only one v remaining in the effected box,
						// excluding this colors cells + there buddies.
					    // NOTE: the buds method has an internal cache.
						if ( tmp2.setAndNot(grid.regions[i].idxs[v]
										  , thisColor[v].plusBuds()).any()
						  && tmp2.size() == 1
						  // and get the cell to paint (NEVER null)
						  && (cell2=grid.cells[tmp2.peek()]) != null
						  // the "10 paces" rule (see method comment block).
						  && tmp3.setAndNot(grid.regions[BOX_OF[ci]].idxs[v]
										  , BUDDIES[cell2.i]).size() == 1
						  // NB: no pre-check coz buds incl this colors cells
						) {
							cell = grid.cells[ci]; // the source cell
							box = grid.regions[i]; // the effected box
//// F1-3 leaves B3 only 3 in box 1, which leaves F1 only 3 in box 2, so B3-3
//if ( v==3 && "F1".equals(cell.id) && "B3".equals(cell2.id) )
//	Debug.breakpoint();
							// we want explanation in GUI and testcases
							if ( wantWhy )
								why = CON[c]+cell.id+MINUS+v+COFF[c]+LEAVES
								+cell2.id+ONLY+v+IN+box.id
								+", which leaves "
								+cell.id+ONLY+v+IN+cell.box.id
								+COMMA_SO+CON[c]+cell2.id+MINUS+v+COFF[c]+NL;
							paint(c, v, cell2, false, why);
						}
					}
				}
			}
		}
	}

	/**
	 * Does colors[c] contain a contradiction.
	 * <pre>
	 * See if colors[c] contains a contradiction:
	 * * If two v's in a region are green then green invalid.
	 * * If two values of a cell are green then green is invalid.
	 * These two both set multiple cells via a XColoringHintMulti.
	 * </pre>
	 *
	 * @param c color index, GREEN or BLUE
	 * @return The subtype of hint to create:<br>
	 * 2 meaning Type 2: two same color v's in a region.<br>
	 * 3 meaning Type 3: two values in cell are same color.<br>
	 * 0 meaning none.
	 */
	private int contradictions(int c) {
		Cell cell; // cell with contradictory values
		final Idx[] thisColor = colors[c];
		final int o = OPPOSITE[c]; // the opposite color

		// If two+ green v's in a region then green is invalid.
		// NB: Type 2 is as rare as rocking horse s__t.
		// NB: for now we just ASSUME that colors[goodColor] are "good", but
		//     we double-check that when we're building the setPots for the
		//     XColoringHintMulti, otherwise could get (rare) invalid hints.
		for ( ARegion r : grid.regions )
			for ( int v : VALUESES[colorValues[c]] )
				if ( tmp1.setAndMany(thisColor[v], r.idxs[v]) ) {
					region = r;
					cause = tmp1.toCellSet(grid);
					goodColor = o;
					// we want explanation in GUI and testcases
					if ( wantWhy )
						steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
						.append(KON).append(r.id).append(KOFF).append(HAS).append(MULTIPLE)
						.append(CON[c]).append(COLORS[c]).append(SPACE).append(v)
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
					cell = grid.cells[tmp1.peek()];
					cause = Cells.set(cell);
					// we want explanation in GUI and testcases
					if ( wantWhy )
						steps.append(NL).append(CONTRADICTION_LABEL).append(NL)
						  .append(KON).append(cell.id).append(KOFF).append(HAS)
						  .append(CON[c]).append(COLORS[c]).append(SPACE)
						  .append(v1).append(AND).append(v2).append(COFF[c])
						  .append(", which is invalid, so ").append(CCOLORS[o])
						  .append(MUST_BE_TRUE).append(NL);
					return 3;
				}
		return 0;
	}

	/**
	 * Create a new XColoringHintMulti.
	 *
	 * @param value the potential value to create hint for.
	 * @param subtype the subtype of the hint to create.
	 * @return a new XColoringHintMulti
	 */
	private AHint createHintMulti(int value, int subtype) {
		final Pots setPots; // the cell-values to be set
		try {
			setPots = squishSetPots(goodColor);
		} catch ( Pots.IToldHimWeveAlreadyGotOneException ex ) {
			return null; // attempted to set 1 cell to 2 values.
		}
		AHint hint = new XColoringHintBig(this, value, subtype, null
			, cause, goodColor, steps.toString(), setPots, squish(GREEN)
			, squish(BLUE), links, null, region);
		cause = null;
		region = null;
		return hint;
	}

	/**
	 * Squish ONE value into each cell.
	 *
	 * @param c the color
	 * @return a new Pots of all values in this color. There must be 1 value
	 *  per cell (per indice) in colors[c], else I throw IllegalStateException
	 *  meaning that colors[c] is not a valid setPots.
	 * @throws Pots.IToldHimWeveAlreadyGotOneException when second value added
	 *  to a cell.
	 */
	private Pots squishSetPots(int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] )
			thisColor[v].forEach(grid.cells, (cc) ->
				result.insert(cc, VSHFT[v])
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
		Cell cc;	// currentCell: the cell to eliminate from
		int g		// green value/s bitset
		  , b		// blue value/s bitset
		  , pinks	// bitset of "all other values" to eliminate
		  , c		// color: the current color: GREEN or BLUE
		  , o;		// opposite: the other color: BLUE or GREEN
		int subtype = 0; // presume none
		boolean first = true; // is this the first elimination

		// (a) Both colors in a cell eliminate all other values.
		if ( tmp1.setAndAny(all[GREEN], all[BLUE]) ) {
			for ( int i : tmp1.toArrayA() ) {
				if ( grid.cells[i].size > 2
				  // get a bitset of all values of cells[i] that're green and blue.
				  // There may be none, never multiple (contradictions pre-tested)
				  // We need 1 green value and 1 blue value, to strip from pinks.
				  // NOTE: VSIZE[0] == 0.
				  && VSIZE[g=values(GREEN, i)] == 1
				  && VSIZE[b=values(BLUE, i)] == 1
				  // ensure that g and b are not equal (should NEVER be equal)
				  && g != b
				  // pinks is a bitset of "all other values" to be eliminated
				  && (pinks=(cc=grid.cells[i]).maybes & ~g & ~b) != 0
				  // ignore already-justified eliminations (shouldn't happen here)
				  && redPots.upsert(cc, pinks, false)
				) {
					// we want explanation in GUI and testcases
					if ( wantWhy ) {
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(cc.id).append(HAS_BOTH)
						  .append(GON).append(Values.toString(g)).append(GOFF).append(AND)
						  .append(BON).append(Values.toString(b)).append(BOFF).append(COMMA).append(ELIMINATING)
						  .append(RON).append(ALL_OTHER_VALUES).append(ROFF).append(PERIOD)
						  .append(NL);
					}
					subtype |= 1;
				}
			}
		}

		// (b) An uncolored v sees both colored v's.
		for ( int v : VALUESES[paintedValues] ) {
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
				if ( tmp3.setAndAny(BUDDIES[ii], colors[GREEN][v])
				  && tmp4.setAndAny(BUDDIES[ii], colors[BLUE][v])
				  // ignore already-justified eliminations
				  && redPots.upsert(grid.cells[ii], v)
				) {
					// we want explanation in GUI and testcases
					if ( wantWhy ) {
						cc = grid.cells[ii];
						Cell gc = closest(tmp3, cc);
						Cell bc = closest(tmp4, cc);
						if ( first ) {
							first = false;
							steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
						}
						steps.append(cc.id).append(MINUS).append(v).append(SEES_BOTH)
						  .append(GON).append(gc.id).append(MINUS).append(v).append(GOFF).append(AND)
						  .append(BON).append(bc.id).append(MINUS).append(v).append(BOFF)
						  .append(COMMA_SO).append(cc.id).append(CANT_BE)
						  .append(RON).append(v).append(ROFF).append(PERIOD).append(NL);
						if ( links == null )
							links = new LinkedList<>();
						links.add(new Link(cc, v, gc, v));
						links.add(new Link(cc, v, bc, v));
					}
					subtype |= 2;
				}
			}
		}

		// (c) An uncolored v sees a green, and has some other blue value.
		for ( int v : VALUESES[paintedValues] ) {
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
				for ( c=0; c<2; ++c ) {
					// if cells[ii] sees a this-color value
					if ( tmp3.setAndAny(BUDDIES[ii], colors[c][v])
					  // and the opposite color (any value) contains ii
					  && all[OPPOSITE[c]].contains(ii)
					  // ignore already-justified eliminations
					  && redPots.upsert(grid.cells[ii], v)
					) {
						// we want explanation in GUI and testcases
						if ( wantWhy ) {
							cc = grid.cells[ii];
							o = OPPOSITE[c];
							Cell sibling = closest(tmp3, cc);
							int otherV = firstValue(o, ii);
							if ( first ) {
								first = false;
								steps.append(NL).append(ELIMINATIONS_LABEL).append(NL);
							}
							steps.append(cc.id).append(SPACE).append(SEES).append(CON[c])
							  .append(sibling.id).append(MINUS).append(v)
							  .append(COFF[c]).append(AND_HAS)
							  .append(CON[o]).append(otherV).append(COFF[o])
							  .append(", so it can't be ").append(RON).append(v).append(ROFF)
							  .append(PERIOD).append(NL);
							if ( links == null )
								links = new LinkedList<>();
							links.add(new Link(cc, v, sibling, v));
						}
						subtype |= 4;
					}
				}
			}
		}
		return subtype;
	}

	/**
	 * Squash the indices of all values of this color into result.
	 *
	 * @param c the color you seek: GREEN or BLUE
	 */
	private int squash(final int c, final Idx result) {
		result.clear();
		for ( int v : VALUESES[colorValues[c]] )
			result.or(colors[c][v]);
		return result.size();
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
	 * Create a new XColoringHint.
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
		final Idx[] colorSet = null;
		// finks: links from cells which found this hint, if any. sigh.
		final LinkedList<Link> finks = links==null ? null : new LinkedList<>(links);
		AHint hint = new XColoringHint(this, value, new Pots(redPots)
			, squish(GREEN), squish(BLUE), colorSet, steps.toString(), finks);
		// don't hold cell references past there use-by date; so we copy-off
		// the fields when we create the hint (above) then clear the bastards.
		redPots.clear();
		links = null;
		return hint;
	}

	/**
	 * Squish all values of the given color into one new Idx.
	 *
	 * @param color the color to squish
	 * @return a new Pots of Cell=>value's in all values of colors[color].
	 */
	private Pots squish(int color) {
		Pots result = new Pots();
		for ( int v : VALUESES[colorValues[color]] )
			colors[color][v].forEach(grid.cells, (cc) ->
				result.upsert(cc, v)
			);
		return result;
	}

}
