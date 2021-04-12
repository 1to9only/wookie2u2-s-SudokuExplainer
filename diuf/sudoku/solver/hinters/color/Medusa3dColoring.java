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
import static diuf.sudoku.Grid.CELL_IDS;
import diuf.sudoku.Grid.Cell;
import static diuf.sudoku.Grid.EFFECTED_BOXS;
import diuf.sudoku.Idx;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import static diuf.sudoku.Values.VALUESES;
import static diuf.sudoku.Values.VSHFT;
import static diuf.sudoku.Values.VSIZE;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.IPreparer;
import diuf.sudoku.solver.LogicalSolver;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.utils.Log;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Medusa3dColoring implements the 3D Medusa Coloring Sudoku solving technique
 * as described in the https://www.sudopedia.org/wiki/3D_Medusa article.
 * <p>
 * See the text version of above article in ./Coloring.txt
 *
 * @author Keith Corlett 2021-03-04
 */
public class Medusa3dColoring extends AHinter implements IPreparer {

	/** The first color: green. */
	private static final int GREEN = 0;
	/** The second color: blue. */
	private static final int BLUE = 1;
	/** The opposite color. */
	private static final int[] OPPOSITE = {BLUE, GREEN};

	/** An distinctive subtype of exception. */
	private static final class Type2Exception extends RuntimeException {
		private static final long serialVersionUID = 68198734001267L;
	}
	/** thrown ONLY when a Type 2 hint has been found and added to accu. */
	private static final Type2Exception TYPE_2_EXCEPTION = new Type2Exception();
	/** An distinctive subtype of exception. */
	private static final class BicolorException extends IllegalStateException {
		private static final long serialVersionUID = 720396129058L;
		public BicolorException(String msg) {
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
	 * Used to sort ValueScore's by score descending.
	 */
	private static final Comparator<ValueScore> VALUE_SCORE_DESCENDING =
			(ValueScore a, ValueScore b) -> b.score - a.score;

	/**
	 * The constructor.
	 */
	public Medusa3dColoring() {
		super(Tech.Medusa3dColoring);
		// create the colors array.
		for ( int c=0; c<2; ++c )
			for ( int v=1; v<10; ++v )
				colors[c][v] = new Idx();
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
	 * Sometimes I'm over efficophilic. But ____ it, everybody has a vice.
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
		// presume that no hint will be found
		boolean result = false;
		this.grid = grid;
		this.accu = accu;
		this.onlyOne = accu.isSingle();
		this.candidates = grid.getIdxs();
		try {
			// Step 1: foreach value in 2-or-more conjugate relationships,
			//         order by num conjugates + num bivalues DESCENDING.
			for ( int v : startingValues() )
				for ( ARegion region : grid.regions )
					if ( region.indexesOf[v].size == 2
					  && (result|=search(region, v)) && onlyOne )
						return result;
		} catch ( Type2Exception eaten ) {
			// ONLY thrown after a Type 2 hint is added to accu
			result = true;
		} finally {
			this.grid = null;
			this.accu = null;
			this.candidates = null;
			this.cause = null;
		}
		return result;
	}
	// The grid we're processing is a field so there's no need to pass around.
	private Grid grid;
	// the accumulator
	private IAccumulator accu;
	// accu.isSingle()
	private boolean onlyOne;
	// indices of cells in grid which maybe each value 1..9
	private Idx[] candidates;

	/**
	 * search this region and value for Medusa 3D Coloring hints.
	 * @param region the region to search
	 * @param v the value to search for
	 * @return were any hint/s found; noting only one Type 2 is ever found.
	 */
	private boolean search(ARegion region, int v) {
		AHint hint;
		int subtype;
		// presume that no hint will be found
		boolean result = false;
		// clear my fields
		clearColors();
		colorValues[GREEN]=colorValues[BLUE]=paintedValues = 0;
		steps = ""; // the painting steps, and conclusions if any
		// Step 2: paint cells in the conjugate pair opposite colors,
		// also add any additional conjugate pairs and bivalue cells.
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
		region.at(region.indexesOf[v].bits, conjugatePair);
		try {
			// Paint the cell, and all of it's ramifications (as above).
			paint(GREEN, v, region, conjugatePair[0], true
				, "Let us assume that "+conjugatePair[0].id+"-"+v
				  +" is "+CCOLORS[GREEN]
				  +NL
			);
			// A "mono-box" is a box with only 1 place remaining for v, which
			// also leaves the "causal box" with 1 place remaining for v, which
			// constitutes a "strong" link.
			paintMonoBoxs();
		} catch ( BicolorException ex ) {
			// a cell was painted both colors, ergo my implementation is wrong.
			// Don't get your knickers in a twist. It'd be nice if it didn't
			// happen, but it did, so it's handled, ie "prudently ignored".
			Log.teeln("WARN: "+tech.name()+": "+ex);
			return false;
		}
		// Step 5: Analyze the cluster.
		// clear the hint details
		links = null;
		subtype = 0;
		// 5.1 If a color contains a contradiction then
		//     the opposite color MUST be true.
		for ( int c=0; c<2; ++c ) {
//			steps = "";
			if ( (subtype=contradictions(c)) != 0 ) {
				// presuming opposite color isn't rooted too
				hint = createHintMulti(v, subtype);
				boolean ok = hint!=null;
				if ( HintValidator.MEDUSA_3D_COLORING_USES ) {
					if ( hint!=null && !HintValidator.isValidSetPots(grid, hint.getResults()) ) {
						hint.isInvalid = true;
						HintValidator.reportSetPots(tech.name()+"Multi", grid, HintValidator.invalidity, hint.toFullString());
						if ( Run.type != Run.Type.GUI )
							ok = false;
					}
				}
				if ( ok ) {
					result = true;
					if ( accu.add(hint) )
						return result;
					// Same Type 2 from many conjugate-pairs
					// simplest way to handle this is throw an exception
					if ( subtype == 2 )
						throw TYPE_2_EXCEPTION;
				}
			}
		}
		// 5.2 If any v sees both colors then
		//     the uncolored candidates can be eliminated.
		if ( subtype == 0
		  && (subtype=eliminations()) != 0 ) {
			hint = createHint(v, subtype);
			boolean ok = true;
			if ( HintValidator.MEDUSA_3D_COLORING_USES ) {
				if ( !HintValidator.isValid(grid, hint.getReds(0)) ) {
					hint.isInvalid = true;
					HintValidator.report(tech.name(), grid, hint.toFullString());
					if ( Run.type != Run.Type.GUI )
						ok = false;
				}
			}
			if ( ok ) {
				result = true;
				if ( accu.add(hint) )
					return result;
			}
		}
		return result;
	}
	// A conjugatePair is the ONLY two cells in a region which maybe value
	private final Cell[] conjugatePair = new Cell[2];
	// colors[color][value] indices
	// the first index is GREEN=0, or BLUE=1
	// the second index is value 1..9
	// So colors holds the indices of GREEN 7's, or BLUE 2's or whatever.
	// I have no idea whether or not this is the best way to do it! It's
	// just what I came up with, and it seems to work(ish).
	private final Idx[][] colors = new Idx[2][10];
	// a bitset of those values which have been painted either color
	private int paintedValues;
	// a bitset of the GREEN and BLUE values; in an array to swap between
	// color and OPPOSITE[color].
	private final int[] colorValues = new int[2];
	// a temporary Idx: set, read, and forget!
	private final Idx tmp1 = new Idx();

	/**
	 * To select the best starting value, pick a value that has 2 or more
	 * conjugate pairs, and as many candidates as possible in bivalue cells.
	 * These bivalue cells allow us to expand the coloring clusters beyond
	 * the single digit boundary.
	 */
	private int[] startingValues() {
		ValueScore[] scores = new ValueScore[10]; // score of values 1..9
		for ( int v=0; v<10; ++v ) // include zero to not upset sort
			scores[v] = new ValueScore(v);
		// count conjugate pairs for each value
		// build a bitset of those values that're in two+ conjugate pairs
		int values = 0;
		for ( int v=1; v<10; ++v )
			for ( ARegion r : grid.regions )
				if ( r.indexesOf[v].size == 2
				  && ++scores[v].score > 1 )
					values |= VSHFT[v];
		// foreach bivalue cell
		// increment score of each value with two+ conjugate pairs
		for ( Cell c : grid.cells )
			if ( c.maybes.size == 2 )
				for ( int v : VALUESES[c.maybes.bits & values] )
					++scores[v].score;
		// order by score descending
		Arrays.sort(scores, VALUE_SCORE_DESCENDING);
		// count scores with a score of atleast 3 (useless otherwise)
		int n;
		for ( n=0; n<10; ++n )
			if ( scores[n].score < 3 )
				break;
		// then read-off the values, by score descending
		int[] array = new int[n];
		for ( int i=0; i<n; ++i )
			array[i] = scores[i].value;
		return array;
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
	 * <pre>
	 * 1. Paint the given cellValue the given color.
	 * 2. If any region containing this cell except the given region (nullable)
	 *    have two places for this value then paint the otherCell the opposite
	 *    color; and recursively paint the consequences thereof.
	 * 3. If bi and cell is bivalue then paint the otherValue the opposite
	 *    color; and recursively paint the consequences thereof.
	 * </pre>
	 * Paint populates the two colors: green and blue, by value;
	 * and also populates the paintedValues bitset.
	 * <p>
	 * PERFORMANCE: The why String is now constructed ONLY in the GUI when we
	 * might want to use it as part of the HTML to display to the user. In the
	 * batch (LogicalSolverTester) and generate it's just a waste of time; but
	 * the Medusa3dColoringTest has to set Run.type = Run.Type.GUI in order to
	 * compare the full HTML.
	 *
	 * @param c the color to paint this cellValue (GREEN or BLUE)
	 * @param v the value to paint
	 * @param r the region this cell was found in, which is excluded from
	 *  the search for SUBSEQUENT conjugate pairs
	 * @param cell the cell to paint, recursively
	 * @param bi if true then I also paint the otherValue of each bivalue cell
	 *  (cell.maybes.size==2) the opposite color, recursively. If false then
	 *  skip step 3, coz we already know it's a waste of time.
	 * @param mono if true then I also paint the last unpainted cell in a
	 *  region. Currently mono is ALWAYS true, but you can turn this additional
	 *  KRC feature off, just by flicking the switch. Note that mono STILL
	 *  produces invalid hints, and I'm trying to work-out why. sigh.
	 * @param why a one-liner on WHY this cell-value is painted this color.
	 * @throws BicolorException when one cell-value is painted both colors.
	 */
	private void paint(int c, int v, ARegion r, Cell cell, boolean bi
			, String why) throws BicolorException {
		final Idx[] thisColor = colors[c];
		// NEVER paint the same cell-value the same color twice!
		if ( thisColor[v].contains(cell.i) )
			return;
		final int o = OPPOSITE[c]; // the opposite color
		// If this cell-value is already the opposite color then we're rooted!
		if ( colors[o][v].contains(cell.i) )
			throw new BicolorException("Cannot paint "+cell.id+"-"+v+" "
					+COLORS[c]+" when it's already "+COLORS[o]+".");
		// 1. Paint the given cell-value this color
		steps += why;
		thisColor[v].add(cell.i);
		colorValues[c] |= VSHFT[v];
		paintedValues |= VSHFT[v]; // both GREEN and BLUE values
		// 2. Paint the other cell in each conjugate pair the opposite color
		for ( ARegion r2 : cell.regions )
			if ( r2.indexesOf[v].size==2 && r2!=r ) {
				Cell otherCell = r2.otherThan(cell, v);
				// we want explanation only in the GUI
				// NOTE: batch is a MINUTE faster for this!
				if ( Run.type != Run.Type.Batch )
					why = cell.id+"-"+v+" is "+CCOLORS[c]+", so it's conjugate "
						+otherCell.id+"-"+v+" in "+r2.id+" is "+CCOLORS[o]
						+NL;
				else
					why = "";
				paint(o, v, r2, otherCell, true, why);
			}
		// 3. Paint the other value of a bivalue cell the opposite color
		if ( bi && cell.maybes.size==2 ) {
			int otherValue = cell.maybes.otherThan(v);
			// we want explanation only in the GUI
			if ( Run.type != Run.Type.Batch )
				why = cell.id+"-"+v+" is "+CCOLORS[c]+", so it's only other"
					+" value "+cell.id+"-"+otherValue+" is "+CCOLORS[o]+NL;
			else
				why = "";
			paint(o, otherValue, null, cell, false, why);
		}
	}

	/**
	 * mono-boxs: one place remaining for value in effected box. This is an
	 * addition to Medusa3dColoring inherited from XColoring, with changes.
	 * <p>
	 * In XColoring if painting a cell-value green leaves any of it's effected
	 * boxs with one-and-only-one remaining place for value then it too must be
	 * green. But note carefully that this is ONLY a "weak" link: if green c1
	 * then c2 is green DOES NOT imply the converse: if c2 is green then c1 is
	 * green.
	 * <p>
	 * Medusa 3D Coloring requires "strong" links, with that "and conversely"
	 * implication, so we must establish that such a link exists, or leave it
	 * out. To do this I look at cell.box.idxs[v] (the v's in the box that
	 * contains the source cell) if I remove the buddies (cells in same box,
	 * row, or col) of the cell to paint from the source-cells-box and that
	 * leaves one-and-only-one cell (the source cell) then we have a reciprocal
	 * relationship, ie a "strong" link, so we can go ahead and paint this
	 * cell-value the same color as the source-cell-value.
	 * <p>
	 * Note that some eliminations may be missed because mono is not currently
	 * run repeatedly until it finds nothing to paint. paint recursively finds
	 * and paints all consequences, and mono works everything out on the fly
	 * (especially the buds method) so there's a small chance of missing some
	 * cell-values, but missing a hint there-from should be rarer than rocking
	 * horse s__t, so I don't think it's worth the extra clock-ticks, or the
	 * code-changes required to support it (paint returns any, paintMonos
	 * returns any painted, while (paintMonos());
	 */
	private void paintMonoBoxs() {
		Cell cell; // the source cell
		Cell cell2; // the cell to paint, if any
		ARegion box; // shorthand for the box containing the cell to paint
		String cc; // shorthand for CCOLORS[c]
		String why; // the reason we paint this cell-value this color
		// foreach color
		for ( int c=0; c<2; ++c ) { // GREEN, BLUE
			final Idx[] thisColor = colors[c];
			// foreach value which has a cell painted thisColor
			for ( int v : VALUESES[colorValues[c]] ) {
				// foreach cell whose v is painted thisColor
				for ( int cellIndex : thisColor[v].toArrayA() ) {
					// foreach of the four boxes effected by this cell.
					// See EFFECTED_BOXS for a definition there-of.
					// i is the index in grid.regions of the effected box
					for ( int i : INDEXES[EFFECTED_BOXS[BOXS[cellIndex]]] ) {
						// if there's only one v remaining in the effected box,
						// excluding dis colors cells and all of there buddies.
						// NOTE: the buds method uses tmp1.
						if ( tmp2.setAndNot(grid.regions[i].idxs[v], buds(thisColor[v])).any()
						  && tmp2.size() == 1
						  // and get the cell to paint (NEVER null)
						  && (cell2=grid.cells[tmp2.peek()]) != null
						  // and the "and conversely" rule: If c2-v is green
						  // and that leaves one-and-only-one v in cell.box,
						  // then we can safely paint c2-v green because:
						  //   1. if cell-v is green then c2-v is green
						  //   AND CONVERSELY
						  //   2. if c2-v is green then cell-v is green.
						  // This is rare, but it does happen, producing a few
						  // extra hints, mainly eliminations, and setting the
						  // odd extra cell here and there.
						  //
						  // ASIDE: Getting this right took a WEEK. What you
						  // don't see is all the mad crap I tried while I was
						  // trying to work-out what the ____ am I doing? Turns
						  // out it's MUCH simpler than I first thought. sigh.
						  // The breakthrough was the EFFECTED_BOXS field.
						  // Note that we don't even need to check da left-over
						  // cell is the cell, because it must be.
						  && tmp3.setAndNot(grid.regions[BOXS[cellIndex]].idxs[v], BUDDIES[cell2.i]).size() == 1
						) {
							// cell2 is already set to the cell to paint!
							cell = grid.cells[cellIndex]; // the source cell
							cc = CCOLORS[c]; // coloured name of this color
							box = grid.regions[i]; // the effected box
							// we want explanation only in the GUI
							if ( Run.type != Run.Type.Batch )
								why = cell.id+"-"+v+" is "+cc+", so "+cell2.id
								+" is the only place for "+v+" in "+box.id
								+", which leaves only one "+v+" in "+cell.box.id
								+"; so if "+cell.id+" is "+cc+" then "+cell2.id
								+" is "+cc+" and conversely if "+cell2.id+" is "
								+cc+" then "+cell.id+" is "+cc+"; hence "
								+cell2.id+"-"+v+" is "+cc+NL;
							else
								why = "";
							paint(c, v, null, cell2, false, why);
						}
					}
				}
			}
		}
	}

	// tmp1 = this colors cells and all of there buddies
	// colorValues is colors[c][v]: indices of cells whose v's are this color.
	private Idx buds(Idx colorValues) {
		tmp1.set(colorValues);
		colorValues.forEach((i)->tmp1.or(BUDDIES[i]));
		return tmp1;
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
		//     XColoringHintMulti, otherwise we get (a few) invalid hints.
		for ( ARegion region : grid.regions )
			for ( int v : VALUESES[colorValues[c]] )
				if ( tmp1.setAndMultiple(thisColor[v], region.idxs[v]) ) {
					cause = tmp1.toCellSet(grid);
					goodColor = o;
					// we want explanation only in the GUI
					if ( Run.type != Run.Type.Batch )
						steps += "Contradiction: "+region.id+" has multiple "
							+CON[c]+COLORS[c]+" "+v+"'s"+COFF[c]
							+", which is invalid, so "+CCOLORS[o]
							+" must be true."+NL;
					return 2;
				}
		// If two+ values in cell are green then green is invalid.
		for ( int v1 : VALUESES[colorValues[c]] )
			// foreach colorValue EXCEPT v1
			for ( int v2 : VALUESES[colorValues[c] & ~VSHFT[v1]] )
				if ( tmp1.setAndAny(thisColor[v1], thisColor[v2]) ) {
					cause = null;
					cell = grid.cells[tmp1.peek()];
					// we want explanation only in the GUI
					if ( Run.type != Run.Type.Batch )
						steps += "Contradiction: "+cell.id+" has "+CON[c]
							+COLORS[c]+" "+v1+" and "+v2+COFF[c]
							+", which is invalid, so "+CCOLORS[o]
							+" must be true."+NL;
					return 3;
				}
		return 0;
	}
	// the (presumably) "good" color to display "results" in the hint.
	private int goodColor;
	// Type 2 only: the cells which caused this hint.
	private Set<Cell> cause;

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
		AHint hint = new XColoringHintMulti(this, value, subtype, null
			, cause, goodColor, steps, setPots, squish(GREEN), squish(BLUE)
			, links, null);
		cause = null;
		return hint;
	}

	/**
	 * Squish 1 value into each cell.
	 *
	 * @param c the color
	 * @return a new Pots of all values in this color. There must be 1 value
	 *  per cell (per indice) in colors[c], else I throw IllegalStateException
	 *  meaning that colors[c] is not a valid setPots.
	 * @throws Pots.IToldHimWeveAlreadyGotOneException on attempt to add 2
	 *  values to 1 cell.
	 */
	private Pots squishSetPots(int c) throws Pots.IToldHimWeveAlreadyGotOneException {
		final Pots result = new Pots();
		final Idx[] thisColor = colors[c];
		for ( int v : VALUESES[colorValues[c]] )
			thisColor[v].forEach(grid.cells, (cc) ->
				result.insert(cc, new Values(v))
			);
		return result;
	}

	/**
	 * Do the 3D Medusa eliminations.
	 * <pre>
	 * Step 5.2: If any v sees both colors.
	 * (a) Both colors appear in a single tri+valued cell.
	 * (b) An uncolored candidate X can see two differently colored X.
	 * (c) An uncolored candidate sees a colored cell (eg green), but this
	 *     cell has another candidate with the opposite color (eg blue).
	 *
	 * NOTE: (c) is the ONLY additional elimination over Extended Coloring!
	 * So 3D Medusa is a ____ of a lot of code for very little gain, except
	 * that it produces some MASSIVE hints; setting 90% of remaining cells.
	 * But it's still all good because this s__t is challenging!
	 *
	 * There can be MORE THAN ONE TYPE of hint in a grid. The subtype return
	 * value is now a bitset:
	 * * 1: Both colors in cell
	 * * 2: X sees both colors
	 * * 4: has green and sees blue
	 * + The "steps" field has been hijacked to provide the user with the logic
	 *   behind how this hint came to be, ergo specifically why it was raised.
	 * + The "links" field now provides the user with "why" indicators.
	 *
	 * Why Everest? Because it was there.
	 * </pre>
	 */
	private int eliminations() {
		Cell cc;
		int g, b, i, pinks, c, o;
		redPots.clear();
		int subtype = 0;
		// (a) Both colors appear in a single cell (with size > 2).
		for ( i=0; i<81; ++i ) {
			if ( (cc=grid.cells[i]).maybes.size > 2 ) {
				g = b = 0;
				for ( int v : VALUESES[paintedValues] ) {
					if(colors[GREEN][v].contains(i)) g |= VSHFT[v];
					if(colors[BLUE][v].contains(i))  b |= VSHFT[v];
				}
				// If cell has ONE-ONLY value of each color then Type 1.
				// But If either g or b is 0 then there's no hint here.
				// But If either g or b is multiple then there's a Multi Type 2
				// around here somewhere, so I suppress this hint to wait for
				// the Multi Type 2 conjugate-pair, if any, to come up. sigh.
				if ( VSIZE[b]==1 && VSIZE[g]==1
				  // pinks is a bitset of the values to be removed.
				  // if it's 0 then Medusa3dColoring goes DEAD_CAT, so we avoid
				  // the situation, even though it can never happen.
				  && (pinks=cc.maybes.bits & ~g & ~b) != 0
				  // eliminations that're already justified are ignored.
				  // Two rules and/or two cells cause an elimination, so we
				  // take only the first cause we encounter, and ignore any
				  // subsequent causes; to keep it simple for the user.
				  && redPots.upsert(cc, new Values(pinks, false))
				) {
					// we want explanation only in the GUI
					if ( Run.type != Run.Type.Batch )
						steps += CELL_IDS[i]+" has both"
							+" <g>"+Values.toString(g)+"</g>"
							+" and <b1>"+Values.toString(b)+"</b1>"
							+", so <r>all other values</r> are history."
							+NL;
					subtype |= 1;
				}
			}
		}
		// (b) An uncolored candidate X can see two differently colored X.
		for ( int v : VALUESES[paintedValues] ) {
			if ( tmp1.setOr(colors[GREEN][v], colors[BLUE][v]).any() ) {
				for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
					if ( tmp1.setAndAny(BUDDIES[ii], colors[GREEN][v])
					  && tmp3.setAndAny(BUDDIES[ii], colors[BLUE][v])
					  // eliminations that're already justified are ignored
					  && redPots.upsert(grid.cells[ii], new Values(v))
					) {
						// we want explanation only in the GUI
						if ( Run.type != Run.Type.Batch ) {
							cc = grid.cells[ii];
							Cell gc = grid.cells[tmp1.peek()];
							Cell bc = grid.cells[tmp3.peek()];
							steps += CELL_IDS[ii]+" sees"
								+" both <g>"+gc.id+"</g>"
								+" and <b1>"+bc.id+"</b1>"
								+", so this cell can't be <r>"+v+"</r>."
								+NL;
							if ( links == null )
								links = new LinkedList<>();
							links.add(new Link(cc, v, gc, v));
							links.add(new Link(cc, v, bc, v));
						}
						subtype |= 2;
					}
				}
			}
		}
		// (c) An uncolored candidate sees a colored cell (eg green), and this
		//     cell has another candidate with the opposite color (eg blue).
		all[GREEN] = squash(GREEN, allGreens);
		all[BLUE] = squash(BLUE, allBlues);
		for ( int v : VALUESES[paintedValues] ) {
			tmp1.setOr(colors[GREEN][v], colors[BLUE][v]);
			for ( int ii : tmp2.setAndNot(candidates[v], tmp1).toArrayA() ) {
				for ( c=0; c<2; ++c ) {
					o = OPPOSITE[c];
					// if cells[ii] sees a v of this color
					if ( tmp1.setAndAny(BUDDIES[ii], colors[c][v])
					  // and the opposite color (any value) contains ii
					  && all[o].contains(ii)
					  // eliminations that're already justified are ignored
					  && redPots.upsert(cc=grid.cells[ii], v)
					) {
						// we want explanation only in the GUI
						if ( Run.type != Run.Type.Batch ) {
							// v2: the first value colored thisColor which
							// contains the cell indice ii
							int v2 = -1;
							int[] cvs = VALUESES[colorValues[c]];
							for ( int i2=0,I2=cvs.length; i2<I2; ++i2 )
								if ( colors[o][v2=cvs[i2]].contains(ii) )
									break;
							Cell seen = grid.cells[tmp1.peek()];
							steps += cc.id+" sees a "
								+CON[c]+COLORS[c]+" "+v+COFF[c]+" at "+seen.id
								+" and it's "+CON[o]+v2+" is "+COLORS[o]+COFF[o]
								+", so "+seen.id+" can't be <r>"+v+"</r>."+NL;
							if ( links == null )
								links = new LinkedList<>();
							links.add(new Link(cc, v, seen, v));
						}
						subtype |= 4;
					}
				}
			}
		}
		return subtype;
	}
	// names of the colors in an array to select appropriate
	private final String[] COLORS = {"green", "blue"};
	// colorOn html tags in an array to select appropriate
	private final String[] CON = {"<g>", "<b1>"};
	// colorOff html tags in an array to select appropriate
	private final String[] COFF = {"</g>", "</b1>"};
	// colored color names, for convenience.
	private final String[] CCOLORS = {"<g>green</g>", "<b1>blue</b1>"};
	// temporary Idx: set, read, forget!
	private final Idx tmp2 = new Idx();
	// temporary Idx: set, read, forget!
	private final Idx tmp3 = new Idx();
	// the Cell=>Values to eliminate. A field to not create for 99% failure.
	private final Pots redPots = new Pots();
	// the union of all indices for all values of each color.
	private final Idx[] all = new Idx[2];
	// Multi Type 3 only: brown arrows (from src-value to dest-value) in hint.
	private Collection<Link> links;

	/**
	 * Squash the indices of all values of this color into result.
	 *
	 * @param color the color you seek
	 * @return an Idx(all values of this color)
	 */
	private Idx squash(final int color, final Idx result) {
		result.clear();
		final Idx[] thisColor = colors[color];
		for ( int v : VALUESES[colorValues[color]] )
			result.or(thisColor[v]);
		return result;
	}
	// The indices of all green values.
	private final Idx allGreens = new Idx();
	// The indices of all blue values.
	private final Idx allBlues = new Idx();

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
			, squish(GREEN), squish(BLUE), colorSet, steps, finks);
		// don't hold cell references past there use-by date; so we copy-off
		// the fields when we create the hint (above) then clear the bastards.
		redPots.clear();
		links = null;
		return hint;
	}
	private String steps = "";

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
				result.upsert(cc, new Values(v))
			);
		return result;
	}

}
