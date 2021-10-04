/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Copyright (C) 2013-2021 Keith Corlett
 * Available under the terms of the Lesser General Public License (LGPL)
 *
 * This class is based on HoDoKu's SimpleColoring class, by Bernhard Hobiger,
 * published under GNU's GPL licence. Kudos to hobiwan. Mistakes are mine. KRC.
 *
 * Here's hobiwans standard licence statement:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file was NOT part of HoDuKo, but the ideas where.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package diuf.sudoku.solver.hinters.color;

import diuf.sudoku.Cells;
import diuf.sudoku.Grid;
import diuf.sudoku.Grid.ARegion;
import static diuf.sudoku.Grid.BUDDIES;
import static diuf.sudoku.Grid.CELL_IDS;
import static diuf.sudoku.Grid.CELLS_REGIONS;
import diuf.sudoku.Grid.Cell;
import diuf.sudoku.Idx;
import diuf.sudoku.Indexes;
import static diuf.sudoku.Indexes.INDEXES;
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import static diuf.sudoku.Values.VSHFT;
import diuf.sudoku.io.StdErr;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import diuf.sudoku.utils.Frmt;
import static diuf.sudoku.utils.Frmt.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import static diuf.sudoku.utils.Frmt.IN;
import static diuf.sudoku.utils.Frmt.ON;
import static diuf.sudoku.utils.Frmt.SPACE;
import diuf.sudoku.utils.Log;

/**
 * XColoring implements the Extended Coloring Sudoku solving technique.
 * <p>
 * Extended Coloring involves chains of bi-location candidates: Cells in
 * regions with 2 places for v are "colored", so that either colorA or colorB
 * contains v. The variable v is the Coloring candidate value, BTW.
 * <p>
 * Then X-forcing chains are located: if we presume this colored cell contains
 * v then some other cell must also contain v because it's the only location
 * remaining in a region.
 * <p>
 * Then cells which see both colors are eliminated, because either colorA
 * contains v or colorB contains v. If any cell sees two-or-more cells of the
 * same color, then the OTHER color must contain v. Similarly if all cells in
 * a region see cells of the same color then the OTHER color must contain v.
 * <pre>
 * KRC 2021-03-03 I was getting invalid hints from the version of this class
 * based on hobiwan's SimpleColoring, so I rewrote it from scratch; but it's
 * still rooted, and the code looks to me like it follows the algorithm. Either
 * my code is wrong in some way that I'm too stupid to understand (likely), or
 * the actual published algorithm is just plain wrong (unlikely).
 *
 * I tried alterations (Step K:) to get it to work, without any joy. So now I
 * find myself at a loss. It finds all the hints in the test-cases, but it also
 * finds many false-positives: invalid hints. So reluctantly declare that the
 * actual published algorithm is wrong, which I do NOT think. sigh.
 *
 * Surely anyone who published an algorithm would go to the trouble of testing
 * it over many many puzzles BEFORE they publish, to check that it doesn't do
 * unexpected things, like find false positives; so my actual conclusion is I'm
 * too stupid to understand the published algorithm, and also too stupid to
 * understand the authors intent, and so make it work despite his publication.
 *
 * KRC 2021-03-03 09:35 Having failed to sort-out above invalid hint issues, I
 * have decided to password protect XColoring, so that only coders can use it.
 * My thinking is: I've tried everything I can think of to avoid/avert invalid
 * hints and either I'm misunderstanding the algorithm or it's WRONG! I think
 * it's just plain WRONG! Allthough one should never underestimate one's own
 * capacity for sheer unadulterated stupidity.
 *
 * KRC 2021-05-11 At some time in the past I fixed the above invalid hint issue
 * but forgot to comment on it. XColoring is no longer password protected. The
 * problem with coloring is that if any of it is wrong then it's all wrong.
 *
 * KRC 2021-05-17 Investigated XColoring-hints found after GEM has run (moved
 * GEM up in LogicalSolver.configureHinters). I can confirm that the XColoring
 * algorithm finds hints that GEM misses, because GEM requires "and conversely"
 * to paint a cell-value, so XColoring keeps painting where GEM stops. So we
 * use Coloring (for multi and fast basics), XColoring (for misses) and GEM.
 * </pre>
 *
 * @author Keith Corlett 2021-02-27
 */
public final class XColoring extends AHinter {

	/** The first and second colors. */
	private static final int C1 = 0, C2 = 1;

	// the iceQ size and mask
	private static final int Q_SIZE = 32;			// must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;	// for this trick to work

	// the opposite color
	private static final int[] OPPOSITE = {1, 0};

	// ============================== debug stuff =============================

	// DEBUG=true prints the "coloring journey" to stdout.
	private static final boolean DEBUG = false;
	private static void debug(String s) {
		if(DEBUG)System.out.println(s);
	}

	// ============================ instance stuff ============================

	/** The grid to search. */
	private Grid grid;

	/** The indices of cells in grid which maybe each value 1..9. */
	private Idx[] candidates;

	/** The IAccumulator to which any hints found are added. */
	private IAccumulator accu;

	// Indice Color Element Queue:
	// first index is the queue element index: 0..Q_SIZE-1
	// second index is 0=indice(0..80), 1=color(0..1)
	private final int[][] iceQ = new int[Q_SIZE][];
	// we reuse this ONE array to read the cells in an Idx
	private final Cell[] cells = new Cell[81];
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots redPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots setPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots oranges = new Pots();
	// we reuse this List rather than create one every time when we miss 99%.
	private final Collection<Link> links = new LinkedList<>();

	// The steps StringBuilder contains an explanation of why this hint exists.
	// It's part of the hint-HTML.
	private final StringBuilder steps = new StringBuilder(1024);

	private static final String green = "green";
	private static final String blue = "blue";
	private static final String[] cc = {green, blue};
	private static final String[] oc = {blue, green};

	public XColoring() {
		super(Tech.XColoring);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// each hinter must enabled/disable itself to do it on the fly.
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.candidates = grid.idxs;
		this.accu = accu;
		boolean result;
		try {
			// find all/first X-Color hints
			result = search();
		} finally {
			this.grid = null;
			this.candidates = null;
			this.accu = null;
			// each cell holds the whole grid, so forget all cells!
			Arrays.fill(cells, null);
			Cells.cleanCasA();
		}
		return result;
	}

	/**
	 * Append 's' to the steps StringBuilder, followed by a newline.
	 *
	 * @param s to append
	 */
	private void step(String s) {
		debug(s);
		steps.append(s).append(NL);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~ SIMPLE COLORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Finds all X-Color Trap/Wrap hints in the grid and adds them to accu.
	 *
	 * @return were any hint/s found?
	 */
	private boolean search() {
		Cell cell; // If you need this explaining you're in the wrong codebase.
		AHint hint; // Similarly.
		ARegion dr; // a dirty region, to be re-processed.
		Indexes riv; // region.indexesOf[v]
		Idx colorSet // current color (C1 or C2)
		  , otherSet; // OPPOSITE color (C2 or C1)
		int[] ice // indice color element: 0=indice, 1=color
		    , rivs; // VALUESES[region.indexesOf[v].bits]
		int r, w // iceQ read/write index
		  , i // the uniquitious general purpose index
		  , n // the number of whatevers added to the array
		  , a, b // the indices of the cells in this conjugate pair
		  , conjugate // the indice of the conjugate cell
		  , indice, color // grid.cells indice is colored C0 or C1
		  , subtype // the hint type, if any
		  , c // the indice into colorSet
		  , o // the OPPOSITE_COLOR to c
		  , oppositeColor // the other color
		  , dri // dirty region index
		  , resultColor; // the color to paint results in XColoringHintMulti
		boolean any; // did we find any cells?
		// presume that no hint will be found
		boolean result = false;
		// foreach value
		VALUES: for ( int v=1; v<10; ++v ) {
			final int fsv = VSHFT[v]; // for lambda expressions. sigh.
			debug(EMPTY_STRING);
			debug(EMPTY_STRING);
			debug("XColoring Value="+v);
			debug("=================");
			// foreach region in the grid
			for ( ARegion region : grid.regions ) {
				// with 2 places for v (ie a conjugate pair in region on v)
				if ( (riv=region.indexesOf[v]).size == 2 ) {
					// --------------------------------------------------------
					// Step 1: Select a conjugate pair (the only two places for
					// v in a region). Color the first C1, and second C2.
					bothColors.set(region.idxs[v]);
					// region.indexesOf[v] contains 2 cells: 'a' and 'b'
					// which we set in the 2 colorSets: green and blue
					rivs = INDEXES[riv.bits];
					colorSets[C1].clear().add(a = region.cells[rivs[0]].i);
					colorSets[C2].clear().add(b = region.cells[rivs[1]].i);
					debug(EMPTY_STRING);
					debug("a="+a+", b="+b);
					steps.setLength(0); // clear the steps string
					step(Frmt.getSB(64).append("Step 1: Color conjugate pair: green=")
					  .append(colorSets[C1].ids())
					  .append(" and blue=").append(colorSets[C2].ids())
					  .append(IN).append(region.id)
					  .append(ON).append(v).toString());
					// --------------------------------------------------------
					// Step 2: Until no more new cells are colored, do:
					// Find an uncolored conjugate of a colored cell
					// and color it the OPPOSITE color.
					any = false;
					// initialise the Indice Color Element Queue
					iceQ[0] = new int[]{a, C1};
					iceQ[1] = new int[]{b, C2};
					w = 2;
					r = 0;
					// nb: this is just iceQ.poll(), without a method call,
					// which is the main reason why this mess is faster.
					for ( ice=iceQ[r], iceQ[r]=null, r=(r+1)&Q_MASK;
						  ice!=null;
						  ice=iceQ[r], iceQ[r]=null, r=(r+1)&Q_MASK
					) {
						// foreach region containing this cell
						for ( ARegion rr : grid.cells[ice[0]].regions ) {
							if ( rr.indexesOf[v].size == 2 ) {
								// XColoring is first use of region.idxs
								if ( rr.idxs[v].size() != 2 )
									recover(rr, v);
								// and my conjugate is not colored
								if ( !bothColors.contains(conjugate=rr.idxs[v].otherThan(ice[0])) ) {
									indice = ice[0];
									color = ice[1];
									step(Frmt.getSB(64).append("    Step 2: ")
									  .append(CELL_IDS[indice]).append(" (")
									  .append(cc[color]).append(") =conjugate=> ")
									  .append(CELL_IDS[conjugate]).append(" (")
									  .append(oc[color]).append(") in ")
									  .append(rr.id).toString());
									// color it the OPPOSITE color
									oppositeColor = OPPOSITE[color];
									colorSets[oppositeColor].add(conjugate);
									bothColors.add(conjugate);
									// add this newly colored cell to the queue
									iceQ[w]=new int[]{conjugate, oppositeColor};
									w = (w+1) & Q_MASK;
									assert w != r; // queue undersized
									any = true;
								}
							}
						}
					}
					// --------------------------------------------------------
					// Step 3: Until no more new cells are colored, do: If all
					// bar one (called X) v's in a region see cells of the same
					// color then paint X with the SAME color.
					// NOTE: Step 3 is the extension in Extended Colors, which
					// is an extension of Weak Coloring, so we CAN'T paint the
					// conjugate/s of X the opposite color, because that's not
					// allways the case: if src(green)=>X(green) is always true
					// but src(green)=>X(green)=conjugate=>blue is NOT ALWAYS
					// TRUE (it's just logical, which I find REALLY confusing).
					// If you conjugate then you get many more hints, but many
					// of them (~40%) are invalid, and there commonality is
					// relying on the X=>conjugate, so just don't try it, OK?
					// I did and it doesn't! } finally { comment it }
					if ( any ) {
						// foreach color
						for ( c=0; c<2; ++c ) {
							// uncolored cells= colorSet.BUDDIES | colorSet
							bothBuds.set(colorSet=colorSets[c]);
							colorSet.forEach((j)->bothBuds.or(BUDDIES[j]));
							dirtyRegions = 0;
							// foreach region in the grid
							for ( ARegion rgn : grid.regions ) {
								if ( xSet.setAndNot(rgn.idxs[v], bothBuds).size() == 1 ) {
									indice = xSet.peek(); // second peek returned -1; suspicious: peek should NOT be destructive, that's poll ya dummy!
									step(Frmt.getSB(64).append("    Step 3: ")
									  .append(CELL_IDS[indice])
									  .append(" is the only place for ")
									  .append(v).append(IN).append(rgn.id)
									  .append(", so it's ").append(cc[c])
									  .toString());
									colorSet.or(xSet);
									bothColors.or(xSet);
									bothBuds.or(xSet).or(BUDDIES[indice]);
									// enqueue cell.regions for reprocessing
									dirtyRegions |= CELLS_REGIONS[indice];
								}
								// No need to reprocess this region
								dirtyRegions &= ~Idx.SHFT[rgn.index];
							}
							// now reprocess the dirty regions, adding any new
							// ones to the queue, until the queue is empty.
							// 32 means dirtyRegions==0, ie the queue is empty.
							while ( (dri=pollDirtyRegions()) != 32 ) {
								if ( xSet.setAndNot((dr=grid.regions[dri]).idxs[v], bothBuds).size() == 1 ) {
									step("    Step 3: "+CELL_IDS[xSet.peek()]
										+" is the only place for "+v+" in DIRTY "+dr.id
										+", so it's "+(c==0?"green":"blue"));
									colorSet.or(xSet);
									bothColors.or(xSet);
									indice = xSet.peek();
									bothBuds.or(xSet).or(BUDDIES[indice]);
									// reprocess my regions, except this one
									dirtyRegions |= CELLS_REGIONS[indice] & ~Idx.SHFT[dr.index];
								}
							}
						}
						// the type of hint created
						subtype = 0;
						// clear EVERY time. sigh.
						redPots.clear(); // Type 1 only
						setPots.clear(); // Type 2 and Type 3
						resultColor = 0; // color to paint the setPots
						links.clear(); // Type 3 only
						oranges.clear();
						// ------------------------------------------------
						// Step 4: Once all the coloring is done:
						// 4.2 If multiple cells in a region are the same
						//     color, then the OTHER color is true.
						// NOTE: Type 2 is as rare as rocking horse s__t.
						COLOR: for ( c=0; c<2; ++c ) {
							o = OPPOSITE[c];
							colorSet = colorSets[c];
							otherSet = colorSets[o];
							for ( ARegion rgn : grid.regions )
								// nb: I'm hijacking xSet
								if ( xSet.setAndMany(colorSet, rgn.idx) ) {
									step(Frmt.getSB(64)
									  .append("    Step 4.2: Multiple cells {")
									  .append(xSet.ids()).append("} in ")
									  .append(rgn.id).append(" are ").append(cc[c])
									  .append(", which is invalid, so the ")
									  .append(oc[c]).append(" set {")
									  .append(otherSet.ids())
									  .append("} must be true, ie ").append(v)
									  .toString());
									otherSet.forEach(grid.cells, (cc) ->
										setPots.put(cc, fsv)
									);
									resultColor = o;
									subtype = 2; // Type 2
									break COLOR; // first only
								}
						}
						if ( subtype == 0 ) {
							// 4.3 If ALL cells in region which maybe v see
							//     cells of the same color, then the OTHER
							//     color is true.
							COLOR: for ( c=0; c<2; ++c ) { // GREEN, BLUE
								o = OPPOSITE[c];
								colorSet = colorSets[c];
								otherSet = colorSets[o];
								// buddies of THIS color
								colorBuds.clear();
								colorSet.forEach((j)->colorBuds.or(BUDDIES[j]));
								for ( ARegion rgn : grid.regions )
									// hijack xSet
									if ( xSet.setAnd(colorBuds, rgn.idxs[v]).any()
									  && xSet.size() == rgn.indexesOf[v].size ) {
										step(Frmt.getSB(64)
										  .append("    Step 4.3: ALL cells in ")
										  .append(rgn.id).append(" which maybe ")
										  .append(v).append(" {")
										  .append(rgn.idxs[v].ids())
										  .append("} see a ").append(cc[c])
										  .append(SPACE).append(v)
										  .append(", which is invalid, so the ")
										  .append(oc[c]).append(" set {")
										  .append(otherSet.ids())
										  .append("} must be true, ie ")
										  .append(v).toString());
										otherSet.forEach(grid.cells, (cc) ->
											setPots.put(cc, fsv)
										);
										for ( int x : xSet.toArrayA() ) {
											Cell src = grid.cells[x];
											Cell dest = grid.cells[tmp.setAnd(colorSet, BUDDIES[x]).poll()];
											oranges.put(src, fsv);
											links.add(new Link(src, v, dest, v));
										}
										resultColor = o;
										subtype = 3;
										break COLOR; // first only
									}
							}
							if ( subtype == 0 ) {
								// 4.1 If a cell sees both GREEN and BLUE,
								//     then exclude v from this cell.
								// get cells = all uncolored v's in grid.
								n = xSet.setAndNot(candidates[v], bothColors)
										.cellsN(grid, cells);
								// foreach uncolored v in grid
								for ( i=0; i<n; ++i ) {
									cell = cells[i];
									// if cell sees both GREEN and BLUE
									if ( cell.buds.andAny(colorSets[C1])
									  && cell.buds.andAny(colorSets[C2]) ) {
										step("    Step 4.1: cell "+cell.id+" sees"
											+" BOTH green={"+tmp.setAnd(cell.buds, colorSets[C1]).ids()+"}"
											+" and blue={"+tmp.setAnd(cell.buds, colorSets[C2]).ids()+"}");
										redPots.put(cell, fsv);
										subtype = 1; // Type 1 (type not reported)
									}
								}
							}
						}
						// create the hint, if any
						if ( subtype != 0 ) {
							boolean ok = true;
							// Type 2 or Type 3: Set multiple cells (rare).
							if ( subtype==2 || subtype==3 ) {
								hint = new XColoringHintBig(this, v
									, subtype
									// deep-copy-off the Idx[] array, for reuse
									, new Idx[]{new Idx(colorSets[0]), new Idx(colorSets[1])}
									, xSet.toCellSet(grid)
									, resultColor
									, steps.toString() // coloring steps which built this hint
									, new Pots(setPots) // copy-off for reuse
									, new Pots(colorSets[0].cellsA(grid), v)
									, new Pots(colorSets[1].cellsA(grid), v)
									, new LinkedList<>(links) // copy-off for reuse
									, new Pots(oranges) // Type 3 causal cell-values
									, null // region
								);
								// clear fields for next time
								steps.setLength(0);
								setPots.clear();
								links.clear();
								oranges.clear();
								if ( HintValidator.XCOLORING_USES ) {
									if ( !HintValidator.isValidSetPots(grid, setPots) ) {
										hint.isInvalid = true;
										HintValidator.reportSetPots(tech.name()+"Multi", grid, HintValidator.invalidity, hint.toFullString());
										hint.invalidity = HintValidator.prevMessage;
										if ( Run.type != Run.Type.GUI )
											ok = false;
									}
								}
							// Type 1: Eliminations are common.
							} else {
								hint = new XColoringHint(this, v
									, new Pots(redPots)
									, new Pots(colorSets[0].cellsA(grid), v)
									, new Pots(colorSets[1].cellsA(grid), v)
									, new Idx[]{new Idx(colorSets[0]), new Idx(colorSets[1])}
									, steps.toString() // coloring steps which built this hint
									, null // links
								);
								// clear fields
								redPots.clear();
								steps.setLength(0);
								if ( HintValidator.XCOLORING_USES ) {
									if ( !HintValidator.isValid(grid, redPots) ) {
										hint.isInvalid = true;
										HintValidator.report(tech.name(), grid, hint.toFullString());
										hint.invalidity = HintValidator.prevMessage;
										// Show in GUI, hide in batch/testcases
										if ( Run.type != Run.Type.GUI )
											ok = false;
									}
								}
							}
							// add the hint to this accumulator
							if ( ok ) {
								debug("    ADDING hint: "+hint.toFullString());
								result = true;
								if ( accu.add(hint) )
									return result;
							}
							// Skip rest of v coz all hints are same, AFAIK
							continue VALUES; // even if not OK
						}
					}
				}
			}
		}
		return result;
	}
	// it's faster to construct Idx's ONCE
	// indices of cells which are this color
	// * C1 is 0 is colorA is green
	// * C2 is 1 is colorB is blue
	private final Idx[] colorSets = {new Idx(), new Idx()};
	// both colors = colorSets[C1] | colorSets[C2]
	private final Idx bothColors = new Idx();
	// The Exception Cell, or X for short
	private final Idx xSet = new Idx();
	// buddies of this color | bothColors, and then it's reused for other stuff
	private final Idx colorBuds = new Idx();
	// the buddies of both colors
	private final Idx bothBuds = new Idx();
	// a temporary Idx: set, read, forget.
	private final Idx tmp = new Idx();

	// a bitset of indexes of regions that are "dirty", ergo a queue of regions
	// to be reprocessed, used by findXColorHints (above) and poll (below).
	// NOTE: we use just a 32 bit int because there's only 27 regions.
	private int dirtyRegions;

	// poll the dirtyRegions bitset: remove and return it's first set (1) bit.
	private int pollDirtyRegions() {
		int x = dirtyRegions & -dirtyRegions; // lowestOneBit
		dirtyRegions &= ~x; // remove x
		return Integer.numberOfTrailingZeros(x);
	}

	// trouble in paradise: the indexesOf[v].size==2 but idxs[v].size()!=2
	// so for a start reindex this region, if that doesn't fix it reindex
	// the whole bloody grid, and if that still doesn't fix it then give up.
	private void recover(ARegion rr, int v) {
		rr.rebuildAllS__t();
		if ( rr.indexesOf[v].size == 2
		  && rr.idxs[v].size() != 2 ) // TROUBLE
			grid.rebuildBloodyEverything();
		if ( rr.indexesOf[v].size == 2
		  && rr.idxs[v].size() != 2 ) // FMS.
			StdErr.exit(Log.me()+": Unrecoverable!");
	}

}
