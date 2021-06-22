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
import diuf.sudoku.Link;
import diuf.sudoku.Pots;
import diuf.sudoku.Run;
import diuf.sudoku.Tech;
import diuf.sudoku.Values;
import diuf.sudoku.solver.AHint;
import diuf.sudoku.solver.accu.IAccumulator;
import diuf.sudoku.solver.hinters.AHinter;
import diuf.sudoku.solver.hinters.HintValidator;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

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

	// the iceQueue size and mask
	private static final int Q_SIZE = 32;			// must be a power of 2
	private static final int Q_MASK = Q_SIZE - 1;	// for this trick to work

	// the opposite color
	private static final int[] OPPOSITE = {1, 0};

	// ============================ debugger stuff ============================

	// DEBUG=true prints the "coloring journey" to stdout.
	private static final boolean DEBUG = false;
	private static void debug(String s) {
		if(DEBUG)System.out.println(s);
	}
	private void step(String s) {
		debug(s);
		steps.append(s);
		steps.append(NL);
	}
	private StringBuilder steps = new StringBuilder(1024);

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
	private final int[][] iceQueue = new int[Q_SIZE][];
	// the two indices of a conjugate pair in a region
	private final int[] conjugates = new int[2];
	// we reuse this ONE array to read the cells in an Idx
//	private final Cell[] cells = new Cell[81];
	private final Cell[] cells = Cells.array(81);
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots redPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots setPots = new Pots();
	// we reuse this Pots rather than create one every time when we miss 99%.
	private final Pots oranges = new Pots();
	// we reuse this List rather than create one every time when we miss 99%.
	private final Collection<Link> links = new LinkedList<>();

	public XColoring() {
		super(Tech.XColoring);
	}

	@Override
	public boolean findHints(Grid grid, IAccumulator accu) {
		// each hinter must enabled/disable itself to do it on the fly.
		if ( !isEnabled )
			return false;
		this.grid = grid;
		this.candidates = grid.getIdxs();
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
		}
		return result;
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
		int subtype; // the hint type, if any
		int i; // the uniquitious general purpose index
		int n; // the number of whatevers added to the array
		int conjugate; // the indice of the conjugate cell
		int read, write; // iceQueue read/write index
		int[] ice; // indice color element: 0=indice, 1=color
		int indice, color; // grid.cells indice is colored C0 or C1
		int c; // the indice into colorSet
		int o; // the OPPOSITE_COLOR to c
		int oppositeColor; // the other color
		int dri; // dirty region index
		int resultColor; // the color to paint results in XColoringHintMulti
		boolean any; // did we find any cells?
		Idx colorSet; // current color (C1 or C2)
		Idx otherSet; // OPPOSITE color (C2 or C1)
		// presume that no hint will be found
		boolean result = false;
		// foreach value
		VALUES: for ( int v=1; v<10; ++v ) {
			final int fv = v; // for lambda expressions. sigh.
			debug("");
			debug("");
			debug("XColoring Value="+v);
			debug("=================");
			// foreach region
			for ( ARegion region : grid.regions ) {
				// with 2 places for v
				if ( region.indexesOf[v].size == 2 ) {
					// --------------------------------------------------------
					// Step 1: Select a conjugate pair (the only two places for
					// v in a region). Color the first C1, and second C2.
					bothColors.set(region.idxs[v]);
					n = region.idxs[v].toArrayN(conjugates);
					assert n == 2 : "Oops: n = "+n;
					colorSets[C1].clear().add(conjugates[0]);
					colorSets[C2].clear().add(conjugates[1]);
					debug("");
					steps.setLength(0); // clear the steps string
					step("Step 1: Color conjugate pair:"
						+" green="+colorSets[C1].ids()
						+" and blue="+colorSets[C2].ids()
						+" in "+region.id
						+" on "+v);
					// --------------------------------------------------------
					// Step 2: Until no more new cells are colored, do:
					// Find an uncolored conjugate of a colored cell
					// and color it the OPPOSITE color.
					any = false;
					iceQueue[0] = new int[]{conjugates[0], C1};
					iceQueue[1] = new int[]{conjugates[1], C2};
					write = 2;
					read = 0;
					// nb: this is just iceQueue.poll(), without a method call,
					// which is the main reason why this mess is faster.
					for ( ice=iceQueue[read], iceQueue[read]=null, read=(read+1)&Q_MASK;
						  ice!=null;
						  ice=iceQueue[read], iceQueue[read]=null, read=(read+1)&Q_MASK
					) {
						for ( ARegion r : grid.cells[ice[0]].regions ) {
							if ( r.indexesOf[v].size == 2
							  // and my conjugate is not colored
							  && !bothColors.contains(conjugate=r.idxs[v].otherThan(ice[0]))
							) {
								indice = ice[0];
								color = ice[1];
								step("    Step 2: "+CELL_IDS[indice]+" ("+(color==0?"green":"blue")+")"
									+" =conjugate=> "+CELL_IDS[conjugate]+" ("+(color==0?"blue":"green")+")"
									+" in "+r.id);
								// color it the OPPOSITE color
								oppositeColor = OPPOSITE[color];
								colorSets[oppositeColor].add(conjugate);
								bothColors.add(conjugate);
								// add this newly colored cell to the queue
								iceQueue[write]=new int[]{conjugate, oppositeColor};
								write = (write+1) & Q_MASK;
								assert write != read; // queue undersized
								any = true;
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
							for ( ARegion r : grid.regions ) {
								if ( xSet.setAndNot(r.idxs[v], bothBuds).size() == 1 ) {
									step("    Step 3: "+CELL_IDS[xSet.peek()]
										+" is the only place for "+v+" in "+r.id
										+", so it's "+(c==0?"green":"blue"));
									colorSet.or(xSet);
									bothColors.or(xSet);
									indice = xSet.peek();
									bothBuds.or(xSet).or(BUDDIES[indice]);
									// enqueue cell.regions for reprocessing
									dirtyRegions |= CELLS_REGIONS[indice];
								}
								// No need to reprocess this region
								dirtyRegions &= ~Idx.SHFT[r.index];
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
							for ( ARegion r : grid.regions )
								// nb: I'm hijacking xSet
								if ( xSet.setAndMany(colorSet, r.idx) ) {
									step("    Step 4.2: Multiple cells {"+xSet.ids()+"}"
										+" in "+r.id+" are "+(c==0?"green":"blue")
										+", which is invalid, so the "+(c==0?"blue":"green")
										+" set {"+otherSet.ids()+"}"
										+" must be true, ie "+v);
									otherSet.forEach(grid.cells, (cc) ->
										setPots.put(cc, new Values(fv))
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
								for ( ARegion r : grid.regions )
									// hijack xSet
									if ( xSet.setAnd(colorBuds, r.idxs[v]).any()
									  && xSet.size() == r.indexesOf[v].size ) {
										step("    Step 4.3: ALL cells in "+r.id
											+" which maybe "+v+" {"+r.idxs[v].ids()+"}"
											+" see a "+(c==0?"green":"blue")+" "+v
											+", which is invalid"
											+", so the "+(c==0?"blue":"green")
											+" set {"+otherSet.ids()+"}"
											+" must be true, ie "+v);
										otherSet.forEach(grid.cells, (cc) ->
											setPots.put(cc, new Values(fv))
										);
										for ( int x : xSet.toArrayA() ) {
											Cell src = grid.cells[x];
											Cell dest = grid.cells[tmp.setAnd(colorSet, BUDDIES[x]).poll()];
											oranges.put(src, new Values(v));
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
										redPots.put(cell, new Values(v));
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
									, new Pots(colorSets[0].cells(grid), new Values(v))
									, new Pots(colorSets[1].cells(grid), new Values(v))
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
									, new Pots(colorSets[0].cells(grid), new Values(v))
									, new Pots(colorSets[1].cells(grid), new Values(v))
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

}
